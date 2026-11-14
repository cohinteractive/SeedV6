"""Bounded R001 worker qualification, UCI gates and one-worker runtime equivalence."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import re
import zipfile
import chess
import chess.engine
from bootstrap import ROOT, BASE, PRIOR, sha, write
sys.path.insert(0,str(ROOT/'tools/conventional-nnue'))
import compute_gate


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--attempt',required=True);args=parser.parse_args()
    if not re.fullmatch(r'r[1-9][0-9]*',args.attempt):raise ValueError('Use a fresh rN attempt directory')
    compute_gate.require();start=time.monotonic()
    assert not (BASE/'r001-qualification.json').exists(),'Preserve completed qualification'
    out=BASE/f'raw/r001-qualification-{args.attempt}';out.mkdir(exist_ok=False)
    manifests={v:json.loads((BASE/f'frozen-runtime-v{v}.json').read_text()) for v in (4,5)}
    for manifest in manifests.values():
        for name,digest in manifest['files'].items():assert sha(Path(manifest['path'])/name)==digest
    old,new=manifests[4],manifests[5]
    assert old['files'].keys()==new['files'].keys()
    changed=[name for name in new['files'] if old['files'][name]!=new['files'][name]]
    assert changed and all(name=='lib/engine-core.jar' or name.startswith('classes/com/ohinteractive/seedv6/integration/NnueStrengthUciMain') for name in changed)
    core_metadata={}
    with zipfile.ZipFile(Path(old['path'])/'lib/engine-core.jar') as before,zipfile.ZipFile(Path(new['path'])/'lib/engine-core.jar') as after:
        assert set(before.namelist())==set(after.namelist())
        for name in before.namelist():
            left,right=before.read(name),after.read(name)
            if left!=right:
                assert name=='com/ohinteractive/seedv6/application-version.properties'
                assert [x for x in left.splitlines() if not x.startswith(b'sourceRevision=')]==[x for x in right.splitlines() if not x.startswith(b'sourceRevision=')]
                core_metadata=dict(entry=name,before=left.decode(),after=right.decode())
    plan=json.loads((BASE/'c001-plan-v2.json').read_text());decision=json.loads((BASE/'c001-decision.json').read_text())
    assert decision['advances_to_robustness'] and decision['plan_sha256']==sha(BASE/'c001-plan-v2.json')
    models={'candidate':plan['network'],'incumbent':plan['incumbent']}
    for name,path in models.items():assert sha(Path(path))==plan['network_sha256' if name=='candidate' else 'incumbent_sha256']
    runtime=Path(new['path']);source=ROOT/'tools/nnue-phase2/ParallelCompactCheck.java';classes=out/'classes';classes.mkdir()
    with (out/'compile.log').open('xb') as log:
        subprocess.run(['C:/java/jdk-21/bin/javac.exe','--add-modules=jdk.incubator.vector','-cp',str(runtime/'lib/*'),
                        '-d',str(classes),str(source)],stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
    with (out/'parallel-check.jsonl').open('xb') as stdout,(out/'parallel-check.stderr').open('xb') as stderr:
        subprocess.run(['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=2','--add-modules=jdk.incubator.vector','-Xmx768m',
                        '-cp',str(runtime/'lib/*')+os.pathsep+str(classes),'ParallelCompactCheck',models['candidate'],
                        str(PRIOR/'cost-roots.fens')],stdout=stdout,stderr=stderr,check=True,timeout=300)
    parallel=json.loads((out/'parallel-check.jsonl').read_text());assert parallel['status']=='PASS'
    preflights={}
    for workers in (1,2):
        for name,network in models.items():
            compute_gate.require();target=out/f'uci-{name}-w{workers}'
            with (out/f'uci-{name}-w{workers}.log').open('xb') as log:
                subprocess.run([sys.executable,'-B',str(ROOT/'tools/conventional-nnue/uci_smoke.py'),'--network',network,
                    '--workers',str(workers),'--strength-research','--vector','--runtime-lib',str(runtime/'lib'),'--output',str(target)],
                    stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
            summary=json.loads((target/'summary.json').read_text());assert summary['status']=='PASS'
            preflights[f'{name}-w{workers}']={'summary':str(target/'summary.json'),'sha256':sha(target/'summary.json'),'status':'PASS'}
    roots=(PRIOR/'cost-roots.fens').read_text().splitlines()[:4];observations={}
    for version,manifest in manifests.items():
        workspace=Path(manifest['path'])
        for name,network in models.items():
            compute_gate.require()
            command=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=1','-Xbatch','--add-modules=jdk.incubator.vector',
                '-Xmx512m','-Djava.awt.headless=true','-cp',str(workspace/'lib/*')+os.pathsep+str(workspace/'classes'),
                'com.ohinteractive.seedv6.integration.NnueStrengthUciMain',network]
            rows=[]
            with (out/f'equivalence-v{version}-{name}.stderr').open('wb') as stderr:
                with chess.engine.SimpleEngine.popen_uci(command,stderr=stderr,timeout=30,
                        creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS) as engine:
                    engine.configure({'Threads':1,'Hash':32,'UseBook':False})
                    for repeat in range(2):
                        for index,fen in enumerate(roots):
                            board=chess.Board(fen);result=engine.play(board,chess.engine.Limit(nodes=10000),game=object(),info=chess.engine.INFO_ALL)
                            assert result.move in board.legal_moves
                            score=result.info['score'].pov(board.turn)
                            rows.append(dict(repeat=repeat,root=index,move=result.move.uci(),nodes=result.info.get('nodes'),
                                depth=result.info.get('depth'),score_cp=score.score(),score_mate=score.mate(),
                                pv=[m.uci() for m in result.info.get('pv',[])]))
            observations[f'v{version}-{name}']=rows
            write(out/f'equivalence-v{version}-{name}.json',dict(command=command,observations=rows))
    for name in models:assert observations['v4-'+name]==observations['v5-'+name],f'One-worker runtime difference: {name}'
    report=dict(status='PASS',elapsed_seconds=time.monotonic()-start,local_evidence=str(out),
        runtime_manifest_sha256={str(v):sha(BASE/f'frozen-runtime-v{v}.json') for v in manifests},changed_runtime_files=changed,core_metadata_difference=core_metadata,
        parallel=parallel,uci_preflights=preflights,one_worker_equivalent_requests=16,
        helper_source_sha256=sha(source),helper_class_sha256={p.name:sha(p) for p in classes.glob('*.class')},
        driver_sha256=sha(Path(__file__)),uci_tool_sha256=sha(ROOT/'tools/conventional-nnue/uci_smoke.py'),
        limits='Actual bounded W2 candidate search with full-rebuild oracle; 16 matching W1 requests across v4/v5, not exhaustive equivalence or performance/strength evidence.')
    write(BASE/'r001-qualification.json',report);print(json.dumps(report,indent=2),flush=True)


if __name__=='__main__':main()
