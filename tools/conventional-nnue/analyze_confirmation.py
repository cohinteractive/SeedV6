"""Audit a completed fixed-sample match and report paired final-endpoint uncertainty.

Replays every PGN and checks frozen artifacts, histories, outcomes and move records.
No interim acceptance or censored-game imputation. Run through owner compute control.
"""
import argparse
import hashlib
import json
import math
import os
import random
from pathlib import Path
import statistics
import chess
import chess.pgn
import compute_gate
from paired_match import summarize


def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()


def elo(p):return 400*math.log10(p/(1-p)) if 0<p<1 else None


def confirmation_family_bounds(values, family):
    if family not in (1,2) or len(values)<2 or any(not math.isfinite(x) or not 0<=x<=1 for x in values):
        raise ValueError('Invalid paired confirmation observations/family')
    alpha=.05/family;mean=statistics.mean(values);se=statistics.stdev(values)/math.sqrt(len(values))
    z=statistics.NormalDist().inv_cdf(1-alpha/2)
    adjusted=[max(0.,mean-z*se),min(1.,mean+z*se)]
    rng=random.Random(20261022)
    samples=sorted(sum(rng.choice(values) for _ in values)/len(values) for _ in range(10000))
    boot=[samples[math.ceil(10000*alpha/2)-1],samples[math.ceil(10000*(1-alpha/2))-1]]
    return dict(max_attempts=family,per_attempt_confidence=1-alpha,
        paired_normal_score=adjusted,paired_normal_elo=[elo(x) for x in adjusted],
        paired_bootstrap_score=boot,paired_bootstrap_elo=[elo(x) for x in boot])


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('match',type=Path);p.add_argument('openings',type=Path);p.add_argument('output',type=Path)
    p.add_argument('--jars',type=Path,help='Optional preserved runtime directory; bytes must match the frozen protocol exactly')
    p.add_argument('--strength-screen',action='store_true',help='Audit a completed evaluator development screen; never a competitive acceptance gate')
    a=p.parse_args();compute_gate.require()
    if a.output.exists():raise ValueError('Preserve completed analysis')
    root=Path(__file__).resolve().parents[2]
    protocol=json.loads((a.match/'protocol.json').read_text())
    if a.strength_screen:
        if protocol.get('search_profile')!='CLV_C1' or not protocol['purpose'].startswith('development screen; evaluator-only'):
            raise ValueError('Not a declared strength development screen')
    elif 'fixed-sample paired confirmation' not in protocol['purpose']:raise ValueError('Not a declared confirmation')
    openings=json.loads(a.openings.read_text())
    assert sha(a.openings)==protocol['openings_sha256'] and openings==protocol['openings']
    assert sha(Path(protocol['network']))==protocol['network_sha256']
    jars=a.jars or root/'app-uci/build/install/seedv6-uci/lib'
    assert {f.name:sha(f) for f in jars.glob('*.jar')}==protocol['jar_sha256']
    if 'launcher_sha256' in protocol:
        command=protocol['commands'][0];index=command.index('-cp')
        assert command[index+2]=='com.ohinteractive.seedv6.integration.NnueStrengthUciMain'
        directories=[Path(s) for s in command[index+1].split(os.pathsep)
                     if (Path(s)/'com/ohinteractive/seedv6/integration/NnueStrengthUciMain.class').is_file()]
        assert len(directories)==1,'Ambiguous strength launcher classpath'
        assert {str(f.relative_to(directories[0])):sha(f) for f in directories[0].rglob('NnueStrengthUciMain*.class')}==protocol['launcher_sha256']
    if 'selective_research_classes_sha256' in protocol:
        command=protocol['commands'][0];index=command.index('-cp')
        assert command[index+2]=='SeedNnueSelectiveResearch'
        directories=[Path(s) for s in command[index+1].split(os.pathsep)
                     if (Path(s)/'SeedNnueSelectiveResearch.class').is_file()]
        assert len(directories)==1,'Ambiguous research launcher classpath'
        assert {f.name:sha(f) for f in directories[0].glob('*.class')}==protocol['selective_research_classes_sha256']
    if 'opponent_network' in protocol:assert sha(Path(protocol['opponent_network']))==protocol['opponent_network_sha256']
    else:assert sha(root/'build-assets/production/bre-pair2-g32.brn-pair2')==protocol['baseline_sha256']
    if 'warmup' in protocol:
        warmup=protocol['warmup'];assert sha(Path(warmup['path']))==warmup['sha256']
        assert Path(warmup['path']).read_text().splitlines()==warmup['fens']
        assert warmup['passes']==2 and 1000<=warmup['nodes']<=100000
        complete=0
        for file in a.match.glob('warmup-*.json'):
            record=json.loads(file.read_text());observations=record['observations']
            expected=[(r,i,e) for r in range(2) for i in range(len(warmup['fens'])) for e in range(2)]
            assert [(x['round'],x['root'],x['engine']) for x in observations]==expected[:len(observations)]
            for row in observations:
                assert chess.Move.from_uci(row['move']) in chess.Board(warmup['fens'][row['root']]).legal_moves
                assert row['wall_seconds']>=0
            if record['complete']:
                assert len(observations)==len(expected);complete+=1
        assert complete>0,'Missing complete frozen warmup'
    games=[json.loads(line) for line in (a.match/'games.jsonl').read_text().splitlines()]
    assert len(games)==2*len(openings),'Incomplete fixed sample'
    seen=set();pairs={};files={};terminations={}
    resources={True:dict(moves=0,nodes=0,seconds=0.,depth_sum=0),False:dict(moves=0,nodes=0,seconds=0.,depth_sum=0)}
    for record in games:
        compute_gate.require();index=record['opening_index'];white=record['candidate_white'];identifier=(index,white)
        assert type(white) is bool and 0<=index<len(openings) and identifier not in seen
        seen.add(identifier);assert record['opening']==openings[index]['name']
        stem=f'{index:03d}-'+('white' if white else 'black')
        pg=a.match/(stem+'.pgn');detail=a.match/(stem+'.json')
        with pg.open() as stream:
            game=chess.pgn.read_game(stream);assert game is not None and not game.errors
            assert chess.pgn.read_game(stream) is None
        board=game.board();assert board.fen()==chess.STARTING_FEN
        opponent=protocol.get('opponent_name',('NNUE-'+protocol['opponent_network_sha256'][:12]) if 'opponent_network_sha256' in protocol else 'Gen32-Policy4')
        candidate=protocol.get('candidate_name','NNUE')
        assert game.headers['White']==(candidate if white else opponent)
        assert game.headers['Black']==(opponent if white else candidate)
        moves=list(game.mainline_moves());history=[m.uci() for m in moves]
        prefix=openings[index]['moves'].split();assert history[:len(prefix)]==prefix
        details=json.loads(detail.read_text());assert details['result']==record
        assert [m['move'] for m in details['moves']]==history[len(prefix):]
        assert len(details['moves'])==record['plies']
        for move_info in details['moves']:
            resource=resources[move_info['candidate']];resource['moves']+=1
            resource['nodes']+=move_info.get('nodes') or 0;resource['seconds']+=move_info.get('time') or 0
            resource['depth_sum']+=move_info.get('depth') or 0
        for i,move in enumerate(moves):
            assert move in board.legal_moves
            if i>=len(prefix):assert details['moves'][i-len(prefix)]['candidate']==(board.turn==white)
            board.push(move)
        outcome=board.outcome(claim_draw=True)
        expected=None if outcome is None else .5 if outcome.winner is None else float(outcome.winner==white)
        assert record['score']==expected
        assert record['termination']==('CAP' if outcome is None else outcome.termination.name)
        assert game.headers['Result']==('*' if outcome is None else outcome.result())
        if outcome is None:assert record['plies']==protocol['max_plies']
        pairs.setdefault(index,[]).append(expected);terminations[record['termination']]=terminations.get(record['termination'],0)+1
        files[pg.name]=sha(pg);files[detail.name]=sha(detail)
    assert seen=={(i,w) for i in range(len(openings)) for w in [True,False]}
    result=summarize(games);assert all(len(v)==2 for v in pairs.values())
    result.update(protocol_sha256=sha(a.match/'protocol.json'),games_sha256=sha(a.match/'games.jsonl'),
        audited_game_files=len(files),legal_replay_and_outcome_audit='PASS',frozen_artifact_recheck='PASS',terminations=terminations,
        endpoint='One predeclared final fixed-sample endpoint; no independent-game binomial interval',
        limits='Hardware/clock/worker/opening-distribution specific; related opening-family dependence may remain. UCI resource totals use the last available info, which may omit unfinished iteration work. No automatic default promotion or owner acceptance.')
    if result['censored']==0:
        values=[sum(pairs[i])/2 for i in range(len(openings))];mean=statistics.mean(values)
        se=statistics.stdev(values)/math.sqrt(len(values));interval=[max(0.,mean-1.96*se),min(1.,mean+1.96*se)]
        result.update(paired_normal_95_score=interval,paired_normal_95_elo=[elo(x) for x in interval],
            paired_sample_sd=statistics.stdev(values),competitive_statistical_gate=mean>.5 and interval[0]>.5 and result['paired_bootstrap_95_score'][0]>.5)
    else:result['competitive_statistical_gate']=False
    family=protocol.get('confirmation_family_size',1)
    if type(family) is not int or family not in (1,2):raise ValueError('Invalid declared confirmation family')
    if family==2 and result['censored']==0:
        # Bonferroni allows at most two independent confirmation attempts in the
        # campaign. Keep the ordinary 95% intervals as descriptive estimates;
        # use 97.5% per-attempt intervals for the positive decision.
        bounds=confirmation_family_bounds(values,family)
        result['confirmation_family']=bounds
        result['competitive_statistical_gate']=mean>.5 and bounds['paired_normal_score'][0]>.5 and bounds['paired_bootstrap_score'][0]>.5
    if a.strength_screen:
        result['exploratory_positive_difference']=result['competitive_statistical_gate']
        result['competitive_statistical_gate']=False
        result['endpoint']='Predeclared development endpoint; selection evidence only, not independent confirmation'
    if 'interpretation' in protocol:
        result['positive_difference_detected'] = result.pop('competitive_statistical_gate')
        result['interpretation'] = protocol['interpretation']
    result['resources']={('candidate' if side else 'opponent'):dict(**v,
        nodes_per_second=v['nodes']/v['seconds'] if v['seconds'] else None,
        mean_depth=v['depth_sum']/v['moves'] if v['moves'] else None) for side,v in resources.items()}
    result['local_evidence']=str(a.match);result['game_files_sha256']=files
    a.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:v for k,v in result.items() if k!='game_files_sha256'},indent=2))


if __name__=='__main__':main()
