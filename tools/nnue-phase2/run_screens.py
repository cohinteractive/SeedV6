"""Fixed Phase2 paired screens and independent completed-game audits; no interim selection.

The final runtime and candidate hashes are frozen before the first game. All long
waits occur inside supervised child processes. Resume only this exact plan.
"""
import argparse
import json
from pathlib import Path
import random
import subprocess
import sys
from bootstrap import ROOT, BASE, PRIOR, write, sha


def make_plan(version):
    runtime_path=BASE/f'frozen-runtime-v{version}.json';runtime=json.loads(runtime_path.read_text())
    for name,digest in runtime['files'].items():assert sha(Path(runtime['path'])/name)==digest
    assert sha(runtime['incumbent'])==runtime['incumbent_sha256']
    candidates=[]
    for name in ('replica','teacher','wdl50','narrow','late'):
        path=BASE/f'a001-{name}-assessment.json';d=json.loads(path.read_text())['results'][name]
        assert d['parity']['status']=='PASS' and d['uci']['status']=='PASS'
        assert d['benchmark']['incremental_allocated_bytes_20000']==0 and sha(d['network'])==d['network_sha256']
        candidates.append(dict(id=name,network=d['network'],sha256=d['network_sha256'],evidence=str(path),evidence_sha256=sha(path)))
    for name in ('deep16','pawnpairs'):
        path=BASE/f'b-{name}-64-assessment.json';d=json.loads(path.read_text())
        assert d['parity']['status']=='PASS' and d['uci']['status']=='PASS' and d['benchmark']['allocated_bytes_100000']==0
        assert sha(d['metadata']['network'])==d['metadata']['network_sha256']
        candidates.append(dict(id=name,network=d['metadata']['network'],sha256=d['metadata']['network_sha256'],evidence=str(path),evidence_sha256=sha(path)))
    kernel_path=BASE/'baseline-kernel-r5.json';kernel=json.loads(kernel_path.read_text())
    assert kernel['experimental_cost']['allocated_bytes_100000']==0
    candidates.append(dict(id='kernel',network=kernel['metadata']['network'],sha256=kernel['metadata']['network_sha256'],
                           evidence=str(kernel_path),evidence_sha256=sha(kernel_path)))
    random.Random(2026111503).shuffle(candidates)
    cost=json.loads((BASE/'comparative-cost.json').read_text());assert len(cost['equal_payload_search_equivalence'])==4
    return dict(schema='seed-phase2-screen-plan-v1',runtime=runtime['path'],runtime_manifest_sha256=sha(runtime_path),runtime_version=version,
        incumbent=runtime['incumbent'],incumbent_sha256=runtime['incumbent_sha256'],candidates=candidates,
        openings=str(BASE/'screen-openings.json'),openings_sha256=sha(BASE/'screen-openings.json'),
        warmup=str(PRIOR/'cost-roots.fens'),warmup_sha256=sha(PRIOR/'cost-roots.fens'),
        comparative_cost_sha256=sha(BASE/'comparative-cost.json'),movetime_ms=200,max_plies=600,per_match_wall_seconds=2400,
        aggregate_wall_seconds=14400,worker=1,hash_mib=32,search='CLV_C1',jvm_batch=True,
        tools_sha256={n:sha(ROOT/'tools/conventional-nnue'/n) for n in ('paired_match.py','analyze_confirmation.py','uci_smoke.py')},
        policy='64 fixed games/candidate; paired shared roots are exploratory; no efficacy stopping or extension. Any confirmation is independent and separately frozen.')


def charged(out):
    total=0.
    games=out/'games.jsonl'
    if games.exists():total+=sum(json.loads(line)['elapsed_seconds'] for line in games.read_text().splitlines())
    for pattern in ('interrupted-*.json','warmup-*.json'):
        total+=sum(json.loads(p.read_text()).get('elapsed_seconds',0) for p in out.glob(pattern))
    if (out/'summary.json').exists():total=max(total,json.loads((out/'summary.json').read_text())['elapsed_seconds'])
    return total


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--runtime-version',type=int,required=True)
    p.add_argument('--prepare-only',action='store_true');a=p.parse_args();plan=make_plan(a.runtime_version)
    path=BASE/'screen-plan.json'
    if path.exists():assert json.loads(path.read_text())==plan,'Changed frozen screen plan'
    else:write(path,plan)
    if a.prepare_only:
        print('Frozen candidate order:',', '.join(c['id'] for c in plan['candidates']));return
    python=[sys.executable,'-B'];control=python+[str(ROOT/'tools/conventional-nnue/compute_control.py')]
    def execute(command,name,timeout):
        # A distinct diagnostic transcript for each invocation; never truncate old evidence.
        import uuid
        nonce=uuid.uuid4().hex[:12];log=BASE/'raw'/f'{name}-driver-{nonce}.log'
        command=command.copy()
        if '--log' in command:
            at=command.index('--log')+1;old=Path(command[at]);command[at]=str(old.with_name(old.stem+'-'+nonce+old.suffix))
        with log.open('xb') as stream:subprocess.run(command,stdout=stream,stderr=subprocess.STDOUT,check=True,timeout=timeout)
    # Recheck actual final UCI runtime. This is correctness work, not timed strength.
    for candidate in [dict(id='incumbent',network=plan['incumbent'])]+plan['candidates']:
        output=BASE/f'raw/screen-preflight-{candidate["id"]}-v{a.runtime_version}'
        if (output/'summary.json').exists():
            assert json.loads((output/'summary.json').read_text())['status']=='PASS';continue
        execute(control+['run','--name','phase2-screen-preflight-'+candidate['id'],'--resume-note','Preserve final-runtime UCI evidence; inspect partial failures',
            '--log',str(output)+'.log','--']+python+[str(ROOT/'tools/conventional-nnue/uci_smoke.py'),
            '--network',candidate['network'],'--strength-research','--vector','--runtime-lib',str(Path(plan['runtime'])/'lib'),'--output',str(output)],
            'preflight-'+candidate['id'],180)
    for candidate in plan['candidates']:
        name=candidate['id'];output=BASE/f'raw/screen-{name}';report=BASE/f'screen-{name}-results.json'
        if report.exists():
            assert json.loads(report.read_text())['protocol_sha256']==sha(output/'protocol.json');continue
        used=sum(charged(BASE/f'raw/screen-{c["id"]}') for c in plan['candidates'])
        if plan['aggregate_wall_seconds']-used<plan['per_match_wall_seconds']-charged(output):
            raise RuntimeError('Insufficient remaining initial screen allowance; prospectively reassess economics')
        command=python+[str(ROOT/'tools/conventional-nnue/paired_match.py'),'--strength-research','--strength-runtime',plan['runtime'],
            '--network',candidate['network'],'--opponent-network',plan['incumbent'],'--openings',plan['openings'],'--output',str(output),
            '--movetime-ms',str(plan['movetime_ms']),'--max-plies',str(plan['max_plies']),'--max-wall-seconds',str(plan['per_match_wall_seconds']),
            '--vector','--jvm-batch','--warmup-fens',plan['warmup'],'--warmup-nodes','20000']
        if output.exists():command.append('--resume')
        execute(control+['run','--name','phase2-screen-'+name,'--resume-note','Resume exact fixed protocol; completed games skip, incomplete game restarts',
            '--log',str(BASE/f'raw/screen-{name}.log'),'--']+command,'screen-'+name,2700)
        execute(control+['run','--name','phase2-audit-'+name,'--resume-note','Independently replay all games and frozen artifacts at final endpoint',
            '--log',str(BASE/f'raw/screen-{name}-audit.log'),'--']+python+[str(ROOT/'tools/conventional-nnue/analyze_confirmation.py'),
            str(output),plan['openings'],str(report),'--strength-screen','--jars',str(Path(plan['runtime'])/'lib')],'audit-'+name,300)
        print('Completed and audited fixed screen:',name,flush=True)


if __name__=='__main__':main()
