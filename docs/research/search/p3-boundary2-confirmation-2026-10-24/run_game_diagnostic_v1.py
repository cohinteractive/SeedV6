"""Actual outcome-blind Fastchess timing diagnosis with exact unchanged engine JARs."""
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
from types import SimpleNamespace
from verify_inputs import HERE, ROOT, OLD, audit, digest, write_new
from run_diagnostic_v1 import module, background


def worker(name, smoke):
    assert sys.stdin.readline().strip()=='GO'
    background()
    m=module('spending',OLD/'spending_match_v1.py')
    original=m.command; attempt=HERE/'raw'/name
    def instrument(args,side):
        command=original(args,side)
        cp=command.index('-cp')+1
        command[cp]=str(HERE/'raw/timing-bridge-v1/classes')+os.pathsep+command[cp]
        command[command.index('com.ohinteractive.seedv6.integration.Cp64SpendingUciMain')]='com.ohinteractive.seedv6.research.timing.Boundary2TimingMain'
        prefix=attempt/'events'/(side+'-%p')
        command[1:1]=[f'-XX:StartFlightRecording=settings=default,filename={prefix}.jfr,dumponexit=true,maxsize=128m',
                       f'-Xlog:gc*,safepoint=info:file="{prefix}-gc.log":utctime,uptimenanos,level,tags:filecount=0',
                       '-Xlog:jfr+startup=off']
        return command
    m.command=instrument
    m.run(SimpleNamespace(baseline=str(OLD/'raw/runtime-production-v2'),candidate=str(OLD/'raw/runtime-candidate-v2'),
        baseline_policy='baseline',candidate_policy='boundary2',baseline_workers=1,candidate_workers=1,
        java='C:/java/jdk-21/bin/java.exe',network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
        runner=str(ROOT/'build/research/tools/fastchess.exe'),openings=str(HERE/'game-smoke-v1-openings.pgn' if smoke else OLD/'clock-development-v1-openings.pgn'),
        out=str(attempt/'match'),hash_mib=64,background=True,operational=True,concurrency=1,pairs=1 if smoke else 32,
        tc='10+0.1',seed=2026102441,timeout=5400))


def main(name,smoke):
    assert re.fullmatch(r'[a-z0-9-]+',name)
    background(); checked=audit()
    assert json.loads((HERE/'bridge-validation-v1-a01-bridge-assessment.json').read_text())['status']=='passed'
    assert json.loads((HERE/'jvm-path-check-v1.json').read_text())['status']=='passed'
    if not smoke:
        assert json.loads((HERE/'game-smoke-v1-a01-event-audit.json').read_text())['status']=='passed'
    out=HERE/'raw'/name; assert not out.exists(); (out/'events').mkdir(parents=True)
    b=module('baseline',ROOT/'tools/search-baseline.py')
    opening=HERE/'game-smoke-v1-openings.pgn'
    if smoke and not opening.exists():
        with (OLD/'clock-development-v1-openings.pgn').open() as stream:
            first=b.chess.pgn.read_game(stream)
        assert first is not None and not first.errors
        with opening.open('x',encoding='utf8',newline='\n') as stream: stream.write(str(first)+'\n')
    files=[Path(__file__),HERE/'game-diagnostic-v1.json',HERE/'Boundary2TimingMain.java',HERE/'run_diagnostic_v1.py',HERE/'verify_inputs.py',
           HERE/'bridge-validation-v1-a01-bridge-assessment.json',HERE/'jvm-path-check-v1.json',
           OLD/'clock-development-v1-openings.pgn',OLD/'spending_match_v1.py',OLD/'audit_confirmation_clock_v1.py',
           ROOT/'tools/p3-selectivity-match.py',ROOT/'tools/search-baseline.py',ROOT/'tools/tournament-process.py',
           ROOT/'tools/sr024-process-cost.py',ROOT/'tools/sr036-process-cost.py',ROOT/'tools/p3-tt-session.py',
           Path('C:/java/jdk-21/bin/java.exe'),Path('C:/java/jdk-21/bin/server/jvm.dll'),
           Path('C:/java/jdk-21/lib/modules'),Path('C:/java/jdk-21/lib/jfr/default.jfc'),Path('C:/java/jdk-21/bin/jfr.exe')]
    if smoke: files.append(opening)
    files += sorted(p for p in (HERE/'raw/timing-bridge-v1/classes').rglob('*') if p.is_file())
    prior=json.loads((OLD/'confirmation-v1-binding.json').read_text())
    inventories={}
    for runtime in ('runtime-production-v2','runtime-candidate-v2'):
        folder=OLD/'raw'/runtime
        actual={str(p.resolve()) for p in folder.rglob('*') if p.is_file()}
        expected={str((ROOT/p).resolve()) for p in prior['files'] if str((ROOT/p).resolve()).startswith(str(folder.resolve())+os.sep)}
        assert actual==expected
        inventories[runtime]=sorted(actual)
    binding={str(p):digest(p) for p in files}
    write_new(HERE/(name+'-binding.json'),{'files':binding,'historicalInputAudit':checked,'runtimeInventory':inventories,'smoke':smoke})
    host=module('host',ROOT/'tools/p3-tt-session.py'); owned=module('owned',ROOT/'tools/tournament-process.py')
    start=host.host_sample(); time.sleep(30); admission=host.host_interval(start,host.host_sample())
    admission['passed']=admission['otherAverageLogicalCores']<=2 and admission['memoryEndpointsPass']
    write_new(HERE/(name+'-admission.json'),admission)
    if not admission['passed']:
        print('Host admission failed; game diagnostic unstarted'); return
    record={'status':'running','bindingSha256':digest(HERE/(name+'-binding.json')),'scores':'Unopened; timing diagnosis only'}
    start=host.host_sample()
    try:
        with (out/'worker.log').open('xb') as stream:
            record['execution']=owned.run_owned([sys.executable,'-B',str(Path(__file__).resolve()),name,'smoke' if smoke else 'full','worker'],stream,5500,ROOT,b.clean_environment())
        assert record['execution']['exitCode']==0
        result=json.loads((out/'match/run.json').read_text())
        assert result['status']=='passed' and result['results']['statistics'] is None
        assert result['results']['terminations']=={'normal':2 if smoke else 64}
        record['integrity']={k:result['results'][k] for k in ('terminations','wholeRequestResources')}
        record['resources']=host.host_interval(start,host.host_sample(),record['execution']['totalOwnedCpuSeconds'])
        record['resources']['passed']=record['resources']['otherAverageLogicalCores']<=2 and record['resources']['memoryEndpointsPass']
        record['matchWallSeconds']=result['matchWallSeconds']
        if not smoke:
            audit_module=module('clock',OLD/'audit_confirmation_clock_v1.py'); captured=[]
            audit_module.write_new=lambda path,value:captured.append(value)
            clock=audit_module.audit(str((out/'match').resolve())); assert len(captured)==1
            write_new(HERE/(name+'-clock-audit.json'),clock)
            record['clockReliability']={k:clock[k] for k in ('clockReliabilityPassed','maximumRequestOverHardMillis')}
        for path,sha in binding.items(): assert digest(path)==sha,path
        for name_,paths in inventories.items():
            assert sorted(str(p.resolve()) for p in (OLD/'raw'/name_).rglob('*') if p.is_file())==paths
        audit(); record['status']='passed' if record['resources']['passed'] else 'resource-unqualified'
        if record.get('clockReliability',{}).get('clockReliabilityPassed') is False: record['status']='clock-unqualified'
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        record['evidence']={str(p.relative_to(HERE)):digest(p) for p in out.rglob('*') if p.is_file()}
        write_new(HERE/(name+'-summary.json'),record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('resources','evidence','integrity')},indent=2))


if __name__=='__main__':
    if len(sys.argv)==4 and sys.argv[3]=='worker': worker(sys.argv[1],sys.argv[2]=='smoke')
    else: main(sys.argv[1],sys.argv[2]=='smoke')
