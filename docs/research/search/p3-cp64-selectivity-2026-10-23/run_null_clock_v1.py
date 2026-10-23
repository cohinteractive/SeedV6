"""Idle resource admission and completion-driven execution of one frozen stage."""
import ctypes
from datetime import datetime
import json
import sys
import time
from types import SimpleNamespace
from run_observation_v1 import HERE, ROOT, digest, write_new
from run_clock_v1 import module

def main():
    which=sys.argv[1];assert which in ('primary','increment')
    plan=json.loads((HERE/'null-clock-development-v1.json').read_text());stage=plan['stages'][which=='increment'];name=stage['name']
    if len(sys.argv)>2 and sys.argv[2]=='worker':
        assert sys.stdin.readline().strip()=='GO'
        m=module('match',ROOT/'tools/p3-selectivity-null-match.py');runtime=str(HERE/'raw/runtime-v4')
        m.run(SimpleNamespace(baseline=runtime,candidate=runtime,network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
            runner=str(ROOT/'build/research/tools/fastchess.exe'),openings=str(HERE/(name+'-openings.pgn')),out=str(HERE/'raw'/name),
            java='C:/java/jdk-21/bin/java.exe',baseline_policy=stage['baseline'],candidate_policy=stage['candidate'],
            baseline_workers=1,candidate_workers=1,hash_mib=64,background=True,operational=False,concurrency=1,pairs=32,
            tc='10+0.1',seed=plan['domain']['seed']+(which=='increment'),timeout=5400));return
    if which=='increment':
        previous=json.loads((HERE/'null-clock-primary-v1-summary.json').read_text())
        assert previous['status']=='passed' and previous['results']['statistics']['score']>=.55,'Increment admission not met'
    assert not (HERE/'raw'/name).exists(),'Never replay a started match'
    assert not (HERE/(name+'-summary.json')).exists()
    for other in plan['stages']:
        if (HERE/'raw'/other['name']).exists():assert (HERE/(other['name']+'-summary.json')).exists(),'Another match is active or unclosed'
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.GetCurrentProcess.restype=ctypes.c_void_p
    k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong];assert k.SetPriorityClass(k.GetCurrentProcess(),0x4000)
    binding=json.loads((HERE/'null-clock-development-v1-binding.json').read_text())
    def stable():
        for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
        for p,sha in binding['externalFiles'].items():assert digest(p)==sha,p
    stable();host=module('host',ROOT/'tools/p3-tt-session.py');owned=module('owned',ROOT/'tools/tournament-process.py')
    baseline=module('baseline',ROOT/'tools/search-baseline.py')
    prior=sorted(HERE.glob(name+'-admission-*.json'));attempt=len(prior)+1
    if prior:
        last=json.loads(prior[-1].read_text());assert not last['passed']
        assert (datetime.now().astimezone()-datetime.fromisoformat(last['end']['utc'])).total_seconds()>=900,'Wait a later availability window'
    start=host.host_sample();time.sleep(30);end=host.host_sample();admission=host.host_interval(start,end)
    admissionFile=HERE/f'{name}-admission-{attempt:02}.json';write_new(admissionFile,admission)
    if not admission['passed']:raise SystemExit('Idle resource admission failed; no games launched')
    start=host.host_sample();receipt={'status':'running','stage':stage,'bindingSha256':digest(HERE/'null-clock-development-v1-binding.json'),
        'admissionSha256':digest(admissionFile)}
    write_new(HERE/(name+'-launch.json'),receipt|{'hostStart':start})
    try:
        with (HERE/'raw'/(name+'-worker.log')).open('xb') as log:
            receipt['execution']=owned.run_owned([sys.executable,'-B',str(HERE/'run_null_clock_v1.py'),which,'worker'],log,5500,ROOT,baseline.clean_environment())
        end=host.host_sample();receipt['resourceQualification']=host.host_interval(start,end,receipt['execution']['totalOwnedCpuSeconds'])
        assert receipt['execution']['exitCode']==0
        result=json.loads((HERE/'raw'/name/'run.json').read_text());assert result['status']=='passed';stable()
        receipt['results']=result['results'];receipt['matchWallSeconds']=result['matchWallSeconds']
        receipt['status']='passed' if receipt['resourceQualification']['passed'] else 'resource-unqualified'
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in sorted((HERE/'raw'/name).iterdir()) if p.is_file()}
    except BaseException as e:receipt.update(status='failed',failure=repr(e));raise
    finally:write_new(HERE/(name+'-summary.json'),receipt)
    print(json.dumps({'status':receipt['status'],'results':{k:v for k,v in receipt['results'].items() if k!='games'}},indent=2))

if __name__=='__main__':main()
