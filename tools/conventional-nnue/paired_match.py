"""One active game, one worker/engine; paired fixed-movetime diagnostic matches.

Openings/configuration are frozen before games. Caps remain censored, never draws.
Use fresh openings for confirmation; this screen is not conclusive strength proof.
"""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import random
import subprocess
import time
import chess
import chess.engine
import chess.pgn
import compute_gate


def sha(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()


def summarize(games):
    completed=[g for g in games if g['score'] is not None]
    score=sum(g['score'] for g in completed)
    bounds=[score/len(games),(score+len(games)-len(completed))/len(games)] if games else [0,1]
    result=dict(games=len(games),completed=len(completed),wins=sum(g['score']==1 for g in completed),
        draws=sum(g['score']==.5 for g in completed),losses=sum(g['score']==0 for g in completed),
        censored=len(games)-len(completed),all_game_score_bounds=bounds)
    pairs={}
    for g in games:pairs.setdefault(g['opening_index'],[]).append(g['score'])
    values=[sum(scores)/2 for scores in pairs.values() if len(scores)==2 and None not in scores]
    if len(completed)==len(games) and values and len(values)*2==len(games):
        mean=sum(values)/len(values)
        rng=random.Random(20261022)
        samples=sorted(sum(rng.choice(values) for _ in values)/len(values) for _ in range(10000))
        interval=[samples[249],samples[9749]]
        def elo(p):return 400*math.log10(p/(1-p)) if 0<p<1 else None
        result.update(score=mean,elo=elo(mean),paired_bootstrap_95_score=interval,
            paired_bootstrap_95_elo=[elo(p) for p in interval],pairs=len(values),
            uncertainty='percentile bootstrap by opening; exploratory, unreliable at tiny n/boundaries; no sequential significance claim')
    return result


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--network',type=Path,required=True);p.add_argument('--openings',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True);p.add_argument('--movetime-ms',type=int,default=50)
    p.add_argument('--max-plies',type=int,default=400)
    p.add_argument('--resume',action='store_true',help='Verify frozen protocol; skip completed games, replay any interrupted game')
    p.add_argument('--selective-research-classes',type=Path,help='Explicit unaccepted one-worker NNUE research launcher classes')
    p.add_argument('--opponent-network',type=Path,help='Frozen conventional NNUE opponent with the same unpruned search; evaluator comparison')
    p.add_argument('--vector',action='store_true',help='Resolve optional Vector API equally for both engines; commands freeze this choice')
    p.add_argument('--confirmation',action='store_true',help='Identify a separately predeclared fixed-sample confirmation protocol')
    p.add_argument('--policy-comparison',action='store_true',help='Explicit selective-versus-unpruned comparison with byte-identical NNUE files')
    a=p.parse_args()
    compute_gate.require()
    if a.policy_comparison:
        if not a.opponent_network or not a.selective_research_classes:
            raise ValueError('Policy comparison requires both a research launcher and the same NNUE opponent')
        if sha(a.network)!=sha(a.opponent_network):
            raise ValueError('Policy comparison must hold network bytes identical')
    elif a.opponent_network and a.selective_research_classes:
        raise ValueError('The NNUE comparison holds search fixed; omit selective research classes')
    if not 10<=a.movetime_ms<=1000 or not 100<=a.max_plies<=1000:raise ValueError('Unbounded match settings')
    openings=json.loads(a.openings.read_text());assert 1<=len(openings)<=128
    a.output.mkdir(parents=True,exist_ok=a.resume)
    root=Path(__file__).resolve().parents[2]
    jars=root/'app-uci/build/install/seedv6-uci/lib'
    base=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=1','-Xmx512m','-Djava.awt.headless=true',
        '-cp',str(jars/'*'),'com.ohinteractive.seedv6.uci.UciMain']
    commands=[base+['--nnue',str(a.network.resolve())],base+['--model',str(root/'build-assets/production/bre-pair2-g32.brn-pair2')]]
    if a.opponent_network:commands[1]=base+['--nnue',str(a.opponent_network.resolve())]
    if a.selective_research_classes:
        classes=a.selective_research_classes.resolve()
        if not (classes/'SeedNnueSelectiveResearch.class').is_file():raise ValueError('Missing research launcher')
        commands[0]=base[:5]+[str(jars/'*')+os.pathsep+str(classes),'SeedNnueSelectiveResearch',str(a.network.resolve())]
    if a.vector:
        for command in commands:command.insert(1,'--add-modules=jdk.incubator.vector')
    protocol=dict(network=str(a.network.resolve()),network_sha256=sha(a.network),openings_sha256=sha(a.openings),
        openings=openings,movetime_ms=a.movetime_ms,max_plies=a.max_plies,threads=1,hash_mib=32,
        use_book=False,ponder=False,concurrent_games=1,commands=commands,
        jar_sha256={f.name:sha(f) for f in jars.glob('*.jar')},
        baseline_sha256=sha(root/'build-assets/production/bre-pair2-g32.brn-pair2'),
        purpose='prospective development screen; different calibrated search policies; complete-engine comparison',
        outcome_policy='python-chess formal endings including claimable draws; cap censored, no score adjudication')
    if a.selective_research_classes:
        protocol['selective_research_classes_sha256']={f.name:sha(f) for f in classes.glob('*.class')}
        protocol['candidate_policy']='Unaccepted NNUE research composition; no inherited Policy4 strength claim'
    if a.opponent_network:
        protocol.pop('baseline_sha256')
        protocol.update(opponent_network=str(a.opponent_network.resolve()),opponent_network_sha256=sha(a.opponent_network),
            purpose='prospective evaluator development comparison; both conventional NNUE with identical unpruned search')
    if a.policy_comparison:
        protocol['purpose']='prospective search-policy development comparison; byte-identical NNUE; selective research versus unpruned'
        protocol['network_held_identical']=True
    if a.confirmation:
        protocol['purpose']='prospective fixed-sample paired confirmation; frozen candidates/settings/openings; final endpoint only'
    games=[]
    if a.resume:
        if json.loads((a.output/'protocol.json').read_text())!=protocol:
            raise ValueError('Resume protocol/artifact mismatch; preserve evidence and reconcile')
        if (a.output/'games.jsonl').exists():
            games=[json.loads(line) for line in (a.output/'games.jsonl').read_text().splitlines()]
    else:
        (a.output/'protocol.json').write_text(json.dumps(protocol,indent=2)+'\n')
    completed={(g['opening_index'],g['candidate_white']) for g in games}
    if len(completed)!=len(games):raise ValueError('Duplicate completed game records')
    if len(completed)==2*len(openings):
        print('All frozen games already completed; no engines launched',flush=True)
        return
    engines=[];start=time.monotonic()
    flags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS
    try:
        for cmd in commands:
            engine=chess.engine.SimpleEngine.popen_uci(cmd,timeout=30,creationflags=flags)
            engine.configure({'Threads':1,'Hash':32,'UseBook':False});engines.append(engine)
        for opening_index,opening in enumerate(openings):
            initial=chess.Board()
            for move in opening['moves'].split():initial.push_uci(move)
            for candidate_white in (True,False):
                if (opening_index,candidate_white) in completed:continue
                board=initial.copy();identifier=object();observations=[];game_start=time.monotonic()
                while not board.is_game_over(claim_draw=True) and len(observations)<a.max_plies:
                    if not compute_gate.permitted():
                        stem=f'interrupted-{time.time_ns()}'
                        (a.output/(stem+'.pgn')).write_text(str(chess.pgn.Game.from_board(board))+'\n')
                        (a.output/(stem+'.json')).write_text(json.dumps(dict(opening_index=opening_index,
                            candidate_white=candidate_white,moves=observations,
                            continuation='Resume same protocol with --resume; completed games skipped; this unfinished game restarts from opening.'),indent=2)+'\n')
                        raise SystemExit(75)
                    candidate_turn=board.turn==candidate_white
                    played=engines[0 if candidate_turn else 1].play(board,chess.engine.Limit(time=a.movetime_ms/1000),
                        game=identifier,info=chess.engine.INFO_BASIC|chess.engine.INFO_SCORE)
                    if played.move not in board.legal_moves:raise RuntimeError('Illegal engine move')
                    observations.append(dict(move=played.move.uci(),candidate=candidate_turn,nodes=played.info.get('nodes'),
                        time=played.info.get('time'),depth=played.info.get('depth')))
                    board.push(played.move)
                outcome=board.outcome(claim_draw=True)
                score=None if outcome is None else .5 if outcome.winner is None else float(outcome.winner==candidate_white)
                record=dict(opening_index=opening_index,opening=opening['name'],candidate_white=candidate_white,
                    score=score,termination='CAP' if outcome is None else outcome.termination.name,
                    elapsed_seconds=time.monotonic()-game_start,plies=len(observations))
                opponent_name=('NNUE-'+protocol['opponent_network_sha256'][:12]) if a.opponent_network else 'Gen32-Policy4'
                game=chess.pgn.Game.from_board(board);game.headers['White']='NNUE' if candidate_white else opponent_name
                game.headers['Black']=opponent_name if candidate_white else 'NNUE'
                game.headers['Result']='*' if outcome is None else outcome.result()
                stem=f'{opening_index:03d}-'+('white' if candidate_white else 'black')
                (a.output/(stem+'.pgn')).write_text(str(game)+'\n')
                (a.output/(stem+'.json')).write_text(json.dumps(dict(result=record,moves=observations),indent=2)+'\n')
                games.append(record)
                with (a.output/'games.jsonl').open('a') as out:out.write(json.dumps(record)+'\n')
                print(json.dumps(dict(progress=f'{len(games)}/{len(openings)*2}',last=record,summary=summarize(games))),flush=True)
        result=summarize(games);result['elapsed_seconds']=time.monotonic()-start
        (a.output/'summary.json').write_text(json.dumps(result,indent=2)+'\n')
    finally:
        for engine in engines:
            try:engine.quit()
            except Exception:engine.close()


if __name__=='__main__':main()
