"""Owned, serial Gradle validation with fresh XML evidence; no experiment overlap."""
import json
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
        '*search.driver.SearchDriverTest', '*search.driver.ClockAllocationPolicyTest', '*search.driver.SpendingPolicyTest',
        '*search.manage.SearchLifecycleServiceTest', '*search.driver.ProductionSearchIntegrationTest',
        '*search.exact.Cp64ProductionTest', '*search.exact.Cp64FlatNullTest',
        '*search.exact.Cp64SelectivityFutilityTest', '*search.exact.ExactSearchTest',
        '*search.exact.ExactSearchMechanicsTest', '*search.exact.ExactSearchTTableTest', '*search.exact.FlatExactSearchTest',
        '*search.tt.TtExactSearchTest', '*search.tt.TranspositionTableConcurrencyTest',
        '*search.alphabeta.RootParallelSearchTest', '*search.exact.HistoryLearningTest']),
    (':app-uci:test', []),
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
    env['JAVA_TOOL_OPTIONS']='-XX:ActiveProcessorCount=4'
    result=subprocess.run(command(),cwd=ROOT,env=env,creationflags=subprocess.CREATE_NO_WINDOW)
    raise SystemExit(result.returncode)


def main(name):
    background()
    out=HERE/'raw'/name; out.mkdir(exist_ok=False)
    owned=module('owned',ROOT/'tools/tournament-process.py')
    baseline=module('baseline',ROOT/'tools/search-baseline.py')
    before_version=digest(ROOT/'VERSION_STATE.txt')
    begin=time.time()
    record={'command':command(),'scriptSha256':digest(__file__),'head':subprocess.check_output(
        ['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'suites':[],
        'scope':'Targeted Search correctness/clock/evaluator/TT/history/lifecycle and integrated UCI/GUI-adapter/Arena-worker routing; headless, not interactive GUI QA or all-worker strength.'}
    try:
        with (out/'gradle.log').open('xb') as stream:
            record['execution']=owned.run_owned([sys.executable,'-B',str(Path(__file__).resolve()),name,'worker'],
                stream,1800,ROOT,baseline.clean_environment())
        for project,task in [('engine-core','test'),('research','test'),('app-uci','test'),('','integrationTest')]:
            folder=ROOT/project/'build/test-results'/task
            files=list(folder.glob('TEST-*.xml')); assert files,folder
            for path in files:
                assert path.stat().st_mtime>=begin,('Stale validation XML',str(path))
                target=out/((project or 'root')+'-'+path.name); shutil.copyfile(path,target)
                record['suites'].append({'path':str(target.relative_to(HERE)),'sha256':digest(target),
                                         **ET.parse(target).getroot().attrib})
        record['totals']={k:sum(int(s[k]) for s in record['suites']) for k in ('tests','failures','errors','skipped')}
        assert record['execution']['exitCode']==0
        assert record['totals']['tests']>0 and record['totals']['failures']==record['totals']['errors']==0
        assert digest(ROOT/'VERSION_STATE.txt')==before_version
        record['status']='passed'
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        record['evidence']={str(p.relative_to(HERE)):digest(p) for p in out.rglob('*') if p.is_file()}
        write_new(HERE/(name+'-summary.json'),record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('suites','evidence')},indent=2))


if __name__=='__main__':
    if len(sys.argv)==3 and sys.argv[2]=='worker': worker(sys.argv[1])
    else: main(sys.argv[1])
