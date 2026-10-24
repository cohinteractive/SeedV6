"""Bounded production-adoption validation; no tournament or timing reproduction."""
import json
import ctypes as c
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from verify_inputs import HERE, ROOT, digest, write_new
from run_diagnostic_v1 import background, module

TASKS = [
    (':engine-core:test', [
        '*search.*', '*rules.GameHistoryTest', '*core.brnpair2.ProductionBaselineTest',
        '*core.nnue.conventional.ConventionalNetworkTest']),
    (':research:test', [
        '*search.driver.Boundary2EquivalenceTest', '*search.driver.SearchDriverTest', '*search.driver.ClockAllocationPolicyTest', '*search.driver.SpendingPolicyTest',
        '*search.manage.SearchLifecycleServiceTest', '*search.driver.ProductionSearchIntegrationTest',
        '*search.exact.Cp64ProductionTest', '*search.exact.Cp64FlatNullTest',
        '*search.exact.Cp64SelectivityFutilityTest', '*search.exact.ExactSearchTest',
        '*search.exact.ExactSearchMechanicsTest', '*search.exact.ExactSearchTTableTest', '*search.exact.FlatExactSearchTest',
        '*search.tt.TtExactSearchTest', '*search.tt.TranspositionTableConcurrencyTest',
        '*search.alphabeta.RootParallelSearchTest', '*search.exact.HistoryLearningTest']),
    (':app-uci:test', []),
    (':app-gui:test', ['*gui.ConventionalGameplayTest', '*gui.BaselineScoreTest']),
    (':engine-training:test', ['*service.BaselineArenaTest']),
    (':integrationTest', [
        '*uci.UciSessionTest', '*uci.UciReportingTest', '*uci.UciProcessTest',
        '*gui.EngineSearchAdapterTest', '*search.driver.ProductionSearchTest',
        '*training.validation.ValidationSearchWorkersTest'])
]


def command():
    result=['cmd.exe','/c','gradlew.bat','--gradle-user-home','build/migration/gradle-user-home',
            '--no-daemon','--max-workers=1','--offline','--console=plain','--rerun-tasks',
            '-PconventionalVectorTests','-Pheadless']
    for task,filters in TASKS:
        result.append(task)
        for pattern in filters: result.extend(['--tests',pattern])
    return result


def worker(name):
    assert sys.stdin.readline().strip()=='GO'
    background()
    env=os.environ.copy()
    env['JAVA_HOME']='C:/java/jdk-21'
    env.pop('JAVA_TOOL_OPTIONS',None)
    env.pop('JDK_JAVA_OPTIONS',None)
    env.pop('_JAVA_OPTIONS',None)
    result=subprocess.run(command(),cwd=ROOT,env=env,stdout=sys.stdout,stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW)
    write_new(HERE/'raw'/name/'command.json',{'exitCode':result.returncode})
    raise SystemExit(result.returncode)



def run_owned(command,log,timeout,cwd,env):
    owned=module('owned',ROOT/'tools/tournament-process.py')
    job=owned.OwnedJob(); process=None; started=time.monotonic()
    try:
        process=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,
            cwd=cwd,env=env,creationflags=0x00000004)
        job.assign(process); job.resume_initial_thread(process)
        process.stdin.write(b'GO\n'); process.stdin.close()
        code=process.wait(timeout=timeout)
        deadline=time.monotonic()+10; counts=[]
        while True:
            accounting=job.accounting(); counts.append(accounting.activeProcesses)
            if not accounting.activeProcesses: break
            if time.monotonic()>=deadline: raise RuntimeError('Owned Gradle tree did not drain within10s after wrapper exit')
            time.sleep(.05)
        return {'exitCode':code,'wallSeconds':time.monotonic()-started,'ownedProcessesRemaining':0,
            'totalOwnedProcesses':accounting.totalProcesses,'totalOwnedCpuSeconds':(accounting.user+accounting.kernel)/1e7,
            'postExitDrainCounts':counts}
    finally:
        job.close()
        if process is not None and process.poll() is None: process.kill(); process.wait(timeout=10)

def main(name):
    background()
    out=HERE/'raw'/name; out.mkdir(exist_ok=False)
    owned=module('owned',ROOT/'tools/tournament-process.py')
    baseline=module('baseline',ROOT/'tools/search-baseline.py')
    before_version=digest(ROOT/'VERSION_STATE.txt')
    begin=time.time()
    record={'command':command(),'scriptSha256':digest(__file__),'head':subprocess.check_output(
        ['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'suites':[],
        'scope':'BOUNDARY2 default adoption: exact reference equivalence, all evaluator/worker functional, clock/deadline, state/lifecycle, TT/history, UCI and headless GUI/Arena regressions; no Elo or timing-cause study.'}
    tracked=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard'],cwd=ROOT,text=True).splitlines()
    record['sourceInputs']={p:digest(ROOT/p) for p in tracked if p.endswith('.java') and (ROOT/p).is_file()}
    try:
        with (out/'gradle.log').open('xb') as stream:
            record['execution']=run_owned([sys.executable,'-B',str(Path(__file__).resolve()),name,'worker']+([flag for flag in ('--gui-diagnosis','--focused') if flag in sys.argv]),
                stream,1800,ROOT,baseline.clean_environment())
        for project,task in [('engine-core','test'),('research','test'),('app-uci','test'),('app-gui','test'),('engine-training','test'),('','integrationTest')]:
            if ((':'+project+':'+task) if project else ':'+task) not in [t for t,f in TASKS]: continue
            folder=ROOT/project/'build/test-results'/task
            files=list(folder.glob('TEST-*.xml')); assert files,folder
            for path in files:
                if path.stat().st_mtime<begin:
                    record.setdefault('unrunOrStaleSuites',[]).append(str(path)); continue
                target=out/((project or 'root')+'-'+path.name); shutil.copyfile(path,target)
                record['suites'].append({'path':str(target.relative_to(HERE)),'sha256':digest(target),
                                         **ET.parse(target).getroot().attrib})
        record['totals']={k:sum(int(s[k]) for s in record['suites']) for k in ('tests','failures','errors','skipped')}
        assert record['execution']['exitCode']==0
        assert not record.get('unrunOrStaleSuites')
        assert record['totals']['tests']>0 and record['totals']['failures']==record['totals']['errors']==0
        assert digest(ROOT/'VERSION_STATE.txt')==before_version
        for path,sha in record['sourceInputs'].items(): assert digest(ROOT/path)==sha,path
        record['status']='passed'
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        record['evidence']={str(p.relative_to(HERE)):digest(p) for p in out.rglob('*') if p.is_file()}
        write_new(HERE/(name+'-summary.json'),record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('suites','evidence','sourceInputs')},indent=2))


if '--focused' in sys.argv:
    TASKS[:]=[(':engine-core:test',['*Boundary2ProductionTest','*BaselineEngineTest','*TimeManagerTest']),(':research:test',['*Boundary2EquivalenceTest']),(':app-uci:test',[])]

if '--gui-diagnosis' in sys.argv:
    TASKS[:]=[(':integrationTest',['*gui.EngineSearchAdapterTest.productionReplacementUsesOneTwoAndFourTotalWorkersIncludingTheOwner'])]

if __name__=='__main__':
    if len(sys.argv)>=3 and sys.argv[2]=='worker': worker(sys.argv[1])
    else: main(sys.argv[1])
