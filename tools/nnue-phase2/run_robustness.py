"""R001 fixed descriptive clock/worker checks; no interim decisions or pooled score."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import uuid
from bootstrap import ROOT, BASE, PRIOR, write, sha
from run_screens import charged
from run_confirmation import opening_keys, key
import chess


def freeze(path,value):
    if path.exists():assert json.loads(path.read_text())==value,'Frozen artifact changed: '+str(path)
    else:write(path,value)


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--prepare-only',action='store_true');args=parser.parse_args()
    primary=json.loads((BASE/'c001-plan-v2.json').read_text());decision=json.loads((BASE/'c001-decision.json').read_text())
    assert decision['advances_to_robustness'] and decision['result_sha256']==sha(BASE/'c001-results.json')
    runtime_path=BASE/'frozen-runtime-v5.json';runtime=json.loads(runtime_path.read_text())
    for name,digest in runtime['files'].items():assert sha(Path(runtime['path'])/name)==digest
    for field in ('network','incumbent'):assert sha(Path(primary[field]))==primary[field+'_sha256']
    qualification=json.loads((BASE/'r001-qualification.json').read_text());assert qualification['status']=='PASS'
    assert qualification['runtime_manifest_sha256']['5']==sha(runtime_path)
    assert qualification['driver_sha256']==sha(ROOT/'tools/nnue-phase2/qualify_robustness.py')
    assert qualification['helper_source_sha256']==sha(ROOT/'tools/nnue-phase2/ParallelCompactCheck.java')
    assert qualification['uci_tool_sha256']==sha(ROOT/'tools/conventional-nnue/uci_smoke.py')
    pool=json.loads((BASE/'robustness-openings.json').read_text());selection=json.loads((BASE/'robustness-openings.selection.json').read_text())
    original=json.loads((BASE/'opening-pool.json').read_text())
    assert selection['range']==[288,416] and selection['pool_sha256']==sha(BASE/'opening-pool.json') and pool==original[288:416]
    keys=opening_keys(pool);assert keys==selection['positionKeys'] and len(set(keys))==128
    used=set(opening_keys(original[:288]))
    for path in (PRIOR/'cost-roots.fens',ROOT/'docs/research/conventional-nnue-cglhw/raw/e007-probes/probes.fens'):
        used.update(key(chess.Board(fen)) for fen in path.read_text().splitlines())
    assert not set(keys[:64])&used
    conditions=[]
    for name,start,workers,movetime,budget in [('long',0,1,400,4800),('workers',32,2,200,2400)]:
        path=BASE/f'r001-{name}-openings.json';freeze(path,pool[start:start+32])
        output=BASE/f'raw/r001-{name}'
        for prior in (BASE/'raw').glob('*/protocol.json'):
            if prior.parent!=output:assert json.loads(prior.read_text()).get('openings_sha256')!=sha(path),'Pool already played elsewhere'
        conditions.append(dict(id=name,workers=workers,movetime_ms=movetime,max_wall_seconds=budget,games=64,
            openings=str(path),openings_sha256=sha(path),robustness_slice=[start,start+32],output=str(output)))
    plan=dict(schema='seed-phase2-r001-v1',primary_decision_sha256=sha(BASE/'c001-decision.json'),
        qualification_sha256=sha(BASE/'r001-qualification.json'),protocol_sha256=sha(BASE/'R001.md'),
        network=primary['network'],network_sha256=primary['network_sha256'],incumbent=primary['incumbent'],incumbent_sha256=primary['incumbent_sha256'],
        runtime=runtime['path'],runtime_manifest_sha256=sha(runtime_path),conditions=conditions,max_plies=800,hash_mib=32,search='CLV_C1',
        aggregate_match_seconds=7200,warmup=primary['warmup'],warmup_sha256=primary['warmup_sha256'],warmup_nodes=20000,jvm_batch=True,
        tools_sha256={name:sha(ROOT/'tools/conventional-nnue'/name) for name in ('paired_match.py','analyze_confirmation.py','uci_smoke.py')},
        driver_sha256=sha(Path(__file__)),
        interpretation='Two descriptive robustness endpoints; no pooled estimate, extension, efficacy stop or retest. Technical failures/caps unverified; negative point crossing even uncertain; interval wholly below even detected harm; nonnegative point limited support only.')
    plan_path=BASE/'r001-plan.json';freeze(plan_path,plan)
    if args.prepare_only:print('R001 frozen:64 games at400ms/W1,64 at200ms/W2; separate fresh32-pair slices.');return
    python=[sys.executable,'-B'];control=python+[str(ROOT/'tools/conventional-nnue/compute_control.py')]
    def execute(command,label,timeout):
        nonce=uuid.uuid4().hex[:12]
        with (BASE/f'raw/r001-{label}-driver-{nonce}.log').open('xb') as log:
            subprocess.run(control+['run','--name','phase2-r001-'+label,'--resume-note','Fixed descriptive endpoint; preserve completed games and interruptions',
                '--log',str(BASE/f'raw/r001-{label}-{nonce}.log'),'--']+command,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=timeout)
    outcomes={}
    for condition in conditions:
        name=condition['id'];output=Path(condition['output']);report=BASE/f'r001-{name}-results.json'
        if not report.exists():
            used=sum(charged(Path(c['output'])) for c in conditions)
            assert used+condition['max_wall_seconds']-charged(output)<=7200,'Remaining allowance insufficient'
            command=python+[str(ROOT/'tools/conventional-nnue/paired_match.py'),'--strength-research','--strength-runtime',plan['runtime'],
                '--network',plan['network'],'--opponent-network',plan['incumbent'],'--openings',condition['openings'],'--output',str(output),
                '--workers',str(condition['workers']),'--movetime-ms',str(condition['movetime_ms']),'--max-plies','800',
                '--max-wall-seconds',str(condition['max_wall_seconds']),'--vector','--jvm-batch','--warmup-fens',plan['warmup'],'--warmup-nodes','20000']
            if output.exists():command.append('--resume')
            execute(command,name,condition['max_wall_seconds']+300)
            execute(python+[str(ROOT/'tools/conventional-nnue/analyze_confirmation.py'),str(output),condition['openings'],str(report),
                '--strength-screen','--jars',str(Path(plan['runtime'])/'lib')],name+'-audit',180)
        result=json.loads(report.read_text());assert result['protocol_sha256']==sha(output/'protocol.json')
        assert result['games_sha256']==sha(output/'games.jsonl')
        technical=result['games']==result['completed']==64 and result['censored']==0 and result['legal_replay_and_outcome_audit']==result['frozen_artifact_recheck']=='PASS'
        interval=result.get('paired_bootstrap_95_score')
        disposition='unverified' if not technical else 'detected harm' if min(interval[1],result['paired_normal_95_score'][1])<.5 else 'transfer uncertain' if result['score']<.5 else 'limited supporting evidence'
        outcomes[name]=dict(result_sha256=sha(report),technical_gate=technical,disposition=disposition,
            wins=result['wins'],draws=result['draws'],losses=result['losses'],elo=result.get('elo'),paired_bootstrap_95_elo=result.get('paired_bootstrap_95_elo'),
            observed_match_seconds=charged(output))
        print('Completed and audited R001 condition: '+name,flush=True)
    summary=dict(plan_sha256=sha(plan_path),outcomes=outcomes,observed_match_seconds=sum(x['observed_match_seconds'] for x in outcomes.values()),
        limits=plan['interpretation'],production_promotion='NOT AUTHORIZED',owner_acceptance='NOT CLAIMED')
    freeze(BASE/'r001-summary.json',summary);print(json.dumps(summary,indent=2),flush=True)


if __name__=='__main__':main()
