"""C001 single frozen512-game independent endpoint and final audit; no interim selection."""
import argparse
import json
from pathlib import Path
import random
import statistics
import subprocess
import sys
import uuid
import chess
from bootstrap import ROOT, BASE, PRIOR, write, sha
from run_screens import charged


def key(board):return ' '.join(board.fen(en_passant='legal').split()[:4])


def opening_keys(openings):
    keys=[]
    for opening in openings:
        board=chess.Board()
        for move in opening['moves'].split():board.push_uci(move)
        assert board.is_valid() and not board.is_game_over(claim_draw=True)
        keys.append(key(board))
    return keys


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--prepare-only',action='store_true');a=p.parse_args()
    endpoints={};paired={};plans={}
    for name in ('screen-plan.json','compact128-screen-plan.json','duration128-screen-plan.json'):
        path=BASE/name;plan=json.loads(path.read_text());plans[name]=sha(path)
        for candidate in plan['candidates']:
            identifier=candidate['id'];report=BASE/f'screen-{identifier}-results.json';out=BASE/f'raw/screen-{identifier}'
            result=json.loads(report.read_text());protocol=json.loads((out/'protocol.json').read_text())
            assert result['games']==result['completed']==64 and result['censored']==0
            assert result['legal_replay_and_outcome_audit']==result['frozen_artifact_recheck']=='PASS'
            assert result['protocol_sha256']==sha(out/'protocol.json') and result['games_sha256']==sha(out/'games.jsonl')
            assert protocol['network_sha256']==candidate['sha256'] and protocol['openings_sha256']==plan['openings_sha256']
            games=[json.loads(line) for line in (out/'games.jsonl').read_text().splitlines()]
            assert len(games)==64 and {(g['opening_index'],g['candidate_white']) for g in games}=={(i,c) for i in range(32) for c in (True,False)}
            paired[identifier]=[sum(g['score'] for g in games if g['opening_index']==i)/2 for i in range(32)]
            endpoints[identifier]={f:result[f] for f in ('wins','draws','losses','score','elo','paired_bootstrap_95_elo')}
            endpoints[identifier].update(report_sha256=sha(report),observed_match_seconds=charged(out))
    assert len(endpoints)==15 and max(endpoints,key=lambda n:endpoints[n]['score'])=='compact128-base'
    contrasts={};rng=random.Random(2026111508)
    for label,old,new in [('direct128','compact128-base','duration128-base'),('deep128','compact128-deep16','duration128-deep16'),
                          ('pawn128','compact128-pawnpairs','duration128-pawnpairs'),('standard256','replica','duration128-replica256')]:
        values=[later-earlier for earlier,later in zip(paired[old],paired[new])]
        samples=sorted(statistics.mean(rng.choices(values,k=32)) for _ in range(10000))
        contrasts[label]=dict(earlier=old,later=new,score_difference=statistics.mean(values),paired_bootstrap_95=[samples[249],samples[9749]])
    screening=dict(endpoints=endpoints,plans_sha256=plans,total_games=960,total_censored=0,duration_contrasts=contrasts,
        observed_match_seconds=sum(e['observed_match_seconds'] for e in endpoints.values()),
        limits='All development endpoints use reused32 paired roots; contrasts are post-hoc/descriptive, not multiplicity-adjusted acceptance or direct head-to-head Elo. No pooled strength estimate.')
    summary_path=BASE/'screening-summary.json'
    if summary_path.exists():assert json.loads(summary_path.read_text())==screening
    else:write(summary_path,screening)
    runtime_path=BASE/'frozen-runtime-v4.json';runtime=json.loads(runtime_path.read_text())
    for name,digest in runtime['files'].items():assert sha(Path(runtime['path'])/name)==digest
    assessment_path=BASE/'compact128-base-64-assessment.json';assessment=json.loads(assessment_path.read_text());meta=assessment['metadata']
    assert assessment['parity']['status']==assessment['uci']['status']=='PASS' and assessment['benchmark']['allocated_bytes_100000']==0
    assert sha(meta['network'])==meta['network_sha256']=='defaf21da465d6f63fc13a2299299e927029d82558d663c7bbc801e7c35e9d1c'
    assert sha(runtime['incumbent'])==runtime['incumbent_sha256']=='a25e76fcdfe1b854452685bec87bc759bed2ed166312579e024410f0903517ab'
    pool=json.loads((BASE/'opening-pool.json').read_text());pool_meta=json.loads((BASE/'opening-pool.selection.json').read_text())
    assert sha(BASE/'opening-pool.json')==pool_meta['output_sha256']
    openings_path=BASE/'confirmation-openings.json';openings=json.loads(openings_path.read_text());selection=json.loads((BASE/'confirmation-openings.selection.json').read_text())
    assert openings==pool[32:288] and len(openings)==256 and selection['range']==[32,288]
    keys=opening_keys(openings);assert keys==selection['positionKeys'] and len(set(keys))==256
    development=json.loads((BASE/'screen-openings.json').read_text());assert development==pool[:32]
    used=set(opening_keys(development))
    for path in (PRIOR/'cost-roots.fens',ROOT/'docs/research/conventional-nnue-cglhw/raw/e007-probes/probes.fens'):
        used.update(key(chess.Board(fen)) for fen in path.read_text().splitlines())
    assert not set(keys)&used,'Confirmation roots overlap development/cost/parity probes'
    output=BASE/'raw/c001-direct128-64'
    for path in (BASE/'raw').glob('*/protocol.json'):
        if path.parent!=output:assert json.loads(path.read_text()).get('openings_sha256')!=sha(openings_path),'Reserved confirmation pool already used'
    plan=dict(schema='seed-phase2-c001-v1',plan_revision=2,candidate='direct128 checkpoint64',network=meta['network'],network_sha256=meta['network_sha256'],
        candidate_assessment_sha256=sha(assessment_path),runtime=runtime['path'],runtime_manifest_sha256=sha(runtime_path),
        incumbent=runtime['incumbent'],incumbent_sha256=runtime['incumbent_sha256'],
        openings=str(openings_path),openings_sha256=sha(openings_path),opening_selection_sha256=sha(BASE/'confirmation-openings.selection.json'),
        pairs=256,games=512,root_independence='No exact root overlap with development/cost/parity probes; reserved pool not previously played in Phase2; related-family dependence can remain.',
        decision_sha256=sha(BASE/'C001.md'),screening_summary_sha256=sha(summary_path),movetime_ms=200,workers=1,hash_mib=32,
        search='CLV_C1',max_plies=800,max_wall_seconds=14400,confirmation_family_size=1,minimum_point_elo=20,
        gates='All512 complete, no caps, legal/artifact audits; paired normal95 and bootstrap95 lower score bounds>0.5; point Elo>=20.',
        warmup=str(PRIOR/'cost-roots.fens'),warmup_sha256=sha(PRIOR/'cost-roots.fens'),
        tool_hashes={n:sha(ROOT/'tools/conventional-nnue'/n) for n in ('paired_match.py','analyze_confirmation.py','uci_smoke.py')},driver_sha256=sha(Path(__file__)))
    frozen=BASE/'c001-plan-v2.json'
    if frozen.exists():assert json.loads(frozen.read_text())==plan,'Changed frozen confirmation plan'
    else:write(frozen,plan)
    if a.prepare_only:print('C001 frozen: direct128 checkpoint64,512 games,one attempt; all primary gates prospective.');return
    python=[sys.executable,'-B'];control=python+[str(ROOT/'tools/conventional-nnue/compute_control.py')]
    def execute(command,label,timeout):
        nonce=uuid.uuid4().hex[:12];command=command.copy()
        if '--log' in command:
            at=command.index('--log')+1;old=Path(command[at]);command[at]=str(old.with_name(old.stem+'-'+nonce+old.suffix))
        with (BASE/f'raw/{label}-driver-{nonce}.log').open('xb') as log:subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=timeout)
    for name,network in [('candidate',plan['network']),('incumbent',plan['incumbent'])]:
        out=BASE/f'raw/c001-preflight-{name}'
        if (out/'summary.json').exists():assert json.loads((out/'summary.json').read_text())['status']=='PASS';continue
        execute(control+['run','--name','phase2-c001-preflight-'+name,'--resume-note','Verify frozen candidate/runtime before primary confirmation',
            '--log',str(out)+'.log','--']+python+[str(ROOT/'tools/conventional-nnue/uci_smoke.py'),'--network',network,
            '--strength-research','--vector','--runtime-lib',str(Path(plan['runtime'])/'lib'),'--output',str(out)],'c001-preflight-'+name,180)
    report=BASE/'c001-results.json'
    if not report.exists():
        command=python+[str(ROOT/'tools/conventional-nnue/paired_match.py'),'--strength-research','--strength-runtime',plan['runtime'],
            '--network',plan['network'],'--opponent-network',plan['incumbent'],'--openings',plan['openings'],'--output',str(output),
            '--movetime-ms','200','--max-plies','800','--max-wall-seconds','14400','--vector','--jvm-batch',
            '--warmup-fens',plan['warmup'],'--warmup-nodes','20000','--confirmation','--confirmation-family-size','1']
        if output.exists():command.append('--resume')
        execute(control+['run','--name','phase2-c001-confirmation','--resume-note','One frozen512-game endpoint; no interim efficacy; preserve every completed/interrupted game',
            '--log',str(BASE/'raw/c001-match.log'),'--']+command,'c001-match',14700)
        execute(control+['run','--name','phase2-c001-audit','--resume-note','Final independent legal replay/artifact/paired statistical audit',
            '--log',str(BASE/'raw/c001-audit.log'),'--']+python+[str(ROOT/'tools/conventional-nnue/analyze_confirmation.py'),
            str(output),plan['openings'],str(report),'--jars',str(Path(plan['runtime'])/'lib')],'c001-audit',300)
    result=json.loads(report.read_text());assert result['protocol_sha256']==sha(output/'protocol.json')
    technical=result['games']==result['completed']==512 and result['censored']==0 and result['legal_replay_and_outcome_audit']==result['frozen_artifact_recheck']=='PASS'
    statistical=bool(result['competitive_statistical_gate']);point_elo=result.get('elo')
    practical=point_elo is not None and point_elo>=plan['minimum_point_elo']
    decision=dict(plan_sha256=sha(frozen),result_sha256=sha(report),technical_gate=technical,statistical_gate=statistical,practical_gate=practical,
        advances_to_robustness=technical and statistical and practical,production_promotion='NOT AUTHORIZED',owner_acceptance='NOT CLAIMED',
        limits='One selected primary endpoint; no pooled development result or automatic alternate candidate. Untested clocks/workers/platforms are not established.')
    target=BASE/'c001-decision.json'
    if target.exists():assert json.loads(target.read_text())==decision
    else:write(target,decision)
    print(json.dumps(dict(result={k:result.get(k) for k in ('wins','draws','losses','censored','elo','paired_bootstrap_95_elo')},decision=decision),indent=2),flush=True)


if __name__=='__main__':main()
