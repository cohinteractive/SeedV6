"""Execute a separately selected, frozen B003 screen; never select by interim scores."""
import argparse
import json
from pathlib import Path
import random
import subprocess
import sys
import uuid
from bootstrap import ROOT, BASE, PRIOR, write, sha
from run_screens import charged


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--runtime-version',type=int,required=True)
    p.add_argument('--candidates',nargs='+',choices=['base','deep16','pawnpairs'],required=True)
    p.add_argument('--decision',type=Path,required=True,help='Prospective selection/economics document, frozen with the plan')
    p.add_argument('--prepare-only',action='store_true');a=p.parse_args()
    assert len(a.candidates)==len(set(a.candidates))
    runtime_path=BASE/f'frozen-runtime-v{a.runtime_version}.json';runtime=json.loads(runtime_path.read_text())
    for name,digest in runtime['files'].items():assert sha(Path(runtime['path'])/name)==digest
    assert sha(runtime['incumbent'])==runtime['incumbent_sha256']
    cost_path=BASE/'compact128-cost.json';cost=json.loads(cost_path.read_text())
    assert cost['runtime_manifest_sha256']==sha(runtime_path)
    candidates=[]
    for name in sorted(a.candidates):
        evidence=BASE/f'compact128-{name}-64-assessment.json';result=json.loads(evidence.read_text());meta=result['metadata']
        assert result['parity']['status']=='PASS' and result['uci']['status']=='PASS'
        assert result['benchmark']['allocated_bytes_100000']==0 and meta['hidden']==128
        assert sha(meta['network'])==meta['network_sha256']
        assert all(row['network_sha256']==meta['network_sha256'] for row in cost['reports'] if row['id']=='compact128-'+name)
        assert 'compact128-'+name in cost['summary']
        candidates.append(dict(id='compact128-'+name,network=meta['network'],sha256=meta['network_sha256'],
                               evidence=str(evidence),evidence_sha256=sha(evidence)))
    random.Random(2026111505).shuffle(candidates)
    plan=dict(schema='seed-phase2-compact128-screen-v1',runtime=runtime['path'],runtime_manifest_sha256=sha(runtime_path),
        runtime_version=a.runtime_version,incumbent=runtime['incumbent'],incumbent_sha256=runtime['incumbent_sha256'],
        candidates=candidates,decision=str(a.decision.resolve()),decision_sha256=sha(a.decision),
        openings=str(BASE/'screen-openings.json'),openings_sha256=sha(BASE/'screen-openings.json'),
        warmup=str(PRIOR/'cost-roots.fens'),warmup_sha256=sha(PRIOR/'cost-roots.fens'),
        cost_report_sha256=sha(cost_path),curves_sha256=sha(BASE/'compact128-curves.json'),
        movetime_ms=200,max_plies=600,per_match_wall_seconds=2400,aggregate_wall_seconds=7200,
        workers=1,hash_mib=32,search='CLV_C1',jvm_batch=True,
        tool_hashes={n:sha(ROOT/'tools/conventional-nnue'/n) for n in ('paired_match.py','analyze_confirmation.py','uci_smoke.py')},
        driver_sha256=sha(Path(__file__)),charge_helper_sha256=sha(ROOT/'tools/nnue-phase2/run_screens.py'),
        policy='64 fixed games/candidate on reused development roots. Selection dependence explicit; no efficacy stopping or resampling; confirmation remains separate.')
    path=BASE/'compact128-screen-plan.json'
    if path.exists():assert json.loads(path.read_text())==plan,'Changed frozen compact screen plan'
    else:write(path,plan)
    if a.prepare_only:
        print('Frozen compact candidate order:',', '.join(c['id'] for c in candidates));return
    python=[sys.executable,'-B'];control=python+[str(ROOT/'tools/conventional-nnue/compute_control.py')]
    def execute(command,label,timeout):
        nonce=uuid.uuid4().hex[:12];command=command.copy()
        if '--log' in command:
            at=command.index('--log')+1;old=Path(command[at]);command[at]=str(old.with_name(old.stem+'-'+nonce+old.suffix))
        with (BASE/f'raw/{label}-driver-{nonce}.log').open('xb') as log:
            subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=timeout)
    for candidate in [dict(id='incumbent',network=plan['incumbent'])]+candidates:
        out=BASE/f'raw/compact-screen-preflight-{candidate["id"]}-v{a.runtime_version}'
        if (out/'summary.json').exists():assert json.loads((out/'summary.json').read_text())['status']=='PASS';continue
        execute(control+['run','--name','phase2-compact-preflight-'+candidate['id'],'--resume-note','Final frozen-runtime UCI lifecycle before games',
            '--log',str(out)+'.log','--']+python+[str(ROOT/'tools/conventional-nnue/uci_smoke.py'),
            '--network',candidate['network'],'--strength-research','--vector','--runtime-lib',str(Path(plan['runtime'])/'lib'),'--output',str(out)],
            'compact-preflight-'+candidate['id'],180)
    for candidate in candidates:
        name=candidate['id'];out=BASE/f'raw/screen-{name}';report=BASE/f'screen-{name}-results.json'
        if report.exists():assert json.loads(report.read_text())['protocol_sha256']==sha(out/'protocol.json');continue
        used=sum(charged(BASE/f'raw/screen-{c["id"]}') for c in candidates)
        if plan['aggregate_wall_seconds']-used<plan['per_match_wall_seconds']-charged(out):
            raise RuntimeError('Compact screen allowance insufficient; reassess prospectively')
        command=python+[str(ROOT/'tools/conventional-nnue/paired_match.py'),'--strength-research','--strength-runtime',plan['runtime'],
            '--network',candidate['network'],'--opponent-network',plan['incumbent'],'--openings',plan['openings'],'--output',str(out),
            '--movetime-ms','200','--max-plies','600','--max-wall-seconds','2400','--vector','--jvm-batch',
            '--warmup-fens',plan['warmup'],'--warmup-nodes','20000']
        if out.exists():command.append('--resume')
        execute(control+['run','--name','phase2-screen-'+name,'--resume-note','Resume only this fixed model/runtime/opening protocol; preserve partial evidence',
            '--log',str(BASE/f'raw/screen-{name}.log'),'--']+command,'screen-'+name,2700)
        execute(control+['run','--name','phase2-audit-'+name,'--resume-note','Independent completed-match replay and artifact audit',
            '--log',str(BASE/f'raw/screen-{name}-audit.log'),'--']+python+[str(ROOT/'tools/conventional-nnue/analyze_confirmation.py'),
            str(out),plan['openings'],str(report),'--strength-screen','--jars',str(Path(plan['runtime'])/'lib')],'audit-'+name,300)
        print('Completed and audited fixed screen:',name,flush=True)


if __name__=='__main__':main()
