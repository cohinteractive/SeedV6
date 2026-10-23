"""Freeze the distinct adaptive activation; preserve closed clock-v1 sources."""
import importlib.util
import json
import re
import shutil
import subprocess
from types import SimpleNamespace
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

spec=importlib.util.spec_from_file_location('match',ROOT/'tools/p3-tt-match.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
assert all(v['clockScreenAdmitted'] for v in json.loads((HERE/'adaptive-v1-summary.json').read_text())['domainDecisions'].values())
runtime=HERE/'raw/clock-runtime-v2';shutil.copytree(ROOT/'build/research/runtime',runtime)
binding={'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
    'worktree':subprocess.check_output(['git','status','--short'],cwd=ROOT,text=True),
    'files':m.b.installation_hashes(runtime),'launcherSha256':digest(ROOT/'src/researchLauncher/java/com/ohinteractive/seedv6/integration/P3TtUciMain.java'),
    'adapterSha256':digest(ROOT/'tools/p3-tt-match.py'),'qualificationSha256':digest(__file__),
    'historicalV1SourcePreservationSha256':digest(HERE/'clock-v1-source-preservation.json')}
write_new(HERE/'clock-runtime-v2-binding.json',binding)
args=SimpleNamespace(baseline=str(runtime),candidate=str(runtime),baseline_policy='baseline',candidate_policy='adaptive',
    baseline_workers=1,candidate_workers=1,java='C:/java/jdk-21/bin/java.exe',network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'))
report={'status':'running','runtimeBindingSha256':digest(HERE/'clock-runtime-v2-binding.json'),'checks':{}}
try:
    for workers in (1,4,16):
        args.baseline_workers=args.candidate_workers=workers
        for side in ('baseline','candidate'):
            key=f'{side}-w{workers}'
            report['checks'][key]=m.preflight(m.command(args,side),HERE/'raw'/f'adaptive-v1-{key}-uci.txt',workers,getattr(args,side+'_policy'))
    args.baseline_workers=1;command=m.command(args,'baseline');entry=command.index('com.ohinteractive.seedv6.integration.P3TtUciMain')
    command=command[:entry]+['com.ohinteractive.seedv6.uci.UciMain']
    engine=m.b.Uci(command,HERE/'raw/adaptive-v1-ordinary-uci.txt')
    try:
        engine.send('uci');identity=engine.until('uciok',90)
        assert any('baseline=construction-v1-cp64' in x and 'flags=262143' in x and 'aspiration=WIDEN16' in x for x in identity)
        engine.send('setoption name OwnBook value false');engine.send('setoption name Threads value 1')
        engine.send('setoption name Hash value 64');engine.send('isready');engine.until('readyok',90)
        results=[]
        for _ in range(2):
            engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
            engine.send('position startpos');engine.send('go nodes 100000')
            results.append(m.b.inspect_result(m.b.chess.Board(),engine.until('bestmove ',60)))
        report['ordinaryBaseline']={'identity':identity,'requests':results}
        for a,b in zip(results,report['checks']['baseline-w1']['requests']):
            for key in ('bestmove','nodes','termination'):assert a[key]==b[key],(key,a,b)
            for pattern in (r'depth (\d+)',r'score (cp|mate) (-?\d+)',r'pv (.*)$'):
                assert re.search(pattern,a['finalInfo']).groups()==re.search(pattern,b['finalInfo']).groups(),(a,b)
        report['baselineParity']=True
    finally:engine.close()
    report['status']='passed'
except BaseException as error:
    report.update(status='failed',failure=repr(error));raise
finally:write_new(HERE/'adaptive-uci-qualification-v1.json',report)
print(report['status'],flush=True)
