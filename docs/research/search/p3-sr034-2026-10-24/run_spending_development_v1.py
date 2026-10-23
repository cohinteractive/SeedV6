"""One frozen whole-game comparison, with pre/post CPU delivery and owned descendants."""
import ctypes
import json
from pathlib import Path
import sys
import time
from types import SimpleNamespace
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module
from run_spending_economics_v1 import execute, host_interval

def main():
    profile=sys.argv[1];assert profile in ('unstable2','boundary2')
    stage='spending-development-'+profile+'-v1'
    binding=json.loads((HERE/'spending-development-v1-binding.json').read_text())
    protocol=json.loads((HERE/'spending-development-v1.json').read_text());d=protocol['domain']
    def stable():
        for path,sha in binding['files'].items():assert digest(ROOT/path)==sha,path
        for path,sha in binding['externalFiles'].items():assert digest(Path(path))==sha,path
    stable()
    assert json.loads((HERE/'spending-frozen-preflight-v1-summary.json').read_text())['status']=='passed'
    if len(sys.argv)>2 and sys.argv[2]=='worker':
        assert sys.stdin.readline().strip()=='GO'
        m=module('match',HERE/'spending_match_v1.py')
        m.run(SimpleNamespace(baseline=str(HERE/'raw/runtime-production-v2'),candidate=str(HERE/'raw/runtime-candidate-v2'),
            network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),runner=str(ROOT/'build/research/tools/fastchess.exe'),
            openings=str(HERE/'clock-development-v1-openings.pgn'),out=str(HERE/'raw'/stage),java='C:/java/jdk-21/bin/java.exe',
            baseline_policy='baseline',candidate_policy=profile,baseline_workers=1,candidate_workers=1,hash_mib=64,
            background=True,operational=False,concurrency=1,pairs=d['pairsPerComparison'],tc=d['clock'],seed=d['seed'],timeout=5400))
        return
    attempt=int(sys.argv[2]) if len(sys.argv)>2 else 1
    assert attempt>=1
    admission_name=stage+f'-a{attempt:02}'
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.GetCurrentProcess.restype=ctypes.c_void_p
    k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong];assert k.SetPriorityClass(k.GetCurrentProcess(),0x4000)
    assert not (HERE/(stage+'-launch.json')).exists(),'Do not replay launched games'
    assert json.loads((HERE/'clock-development-half-v1-summary.json').read_text())['status'] in ('passed','resource-unqualified'), 'Finish/diagnose prescribed HALF comparison first'
    if profile=='boundary2':
        assert json.loads((HERE/'spending-development-unstable2-v1-summary.json').read_text())['status'] in ('passed','resource-unqualified'), 'Finish/diagnose first spending comparison'
    pre=execute(admission_name+'-pre-delivery','delivery')
    assert pre['status']=='passed' and pre['criteriaPassed'],'CPU delivery admission failed before games'
    host=module('host',ROOT/'tools/p3-tt-session.py');owned=module('owned',ROOT/'tools/tournament-process.py')
    b=module('baseline',ROOT/'tools/search-baseline.py')
    start=host.host_sample();time.sleep(30);admission=host_interval(start,host.host_sample())
    write_new(HERE/(admission_name+'-admission.json'),admission)
    assert admission['passed'],'Host admission failed before games'
    stable();start=host.host_sample();receipt={'status':'running','bindingSha256':digest(HERE/'spending-development-v1-binding.json')}
    write_new(HERE/(stage+'-launch.json'),receipt|{'hostStart':start,'profile':profile,'admissionAttempt':attempt,
        'admissionSha256':digest(HERE/(admission_name+'-admission.json')),
        'preDeliverySha256':digest(HERE/(admission_name+'-pre-delivery-summary.json'))})
    try:
        with (HERE/'raw'/(stage+'-worker.log')).open('xb') as log:
            receipt['execution']=owned.run_owned([sys.executable,'-B',str(Path(__file__).resolve()),profile,'worker'],
                log,5500,ROOT,b.clean_environment())
        receipt['resourceQualification']=host_interval(start,host.host_sample(),receipt['execution']['totalOwnedCpuSeconds'])
        assert receipt['execution']['exitCode']==0
        post=execute(stage+'-post-delivery','delivery');receipt['postDeliveryPassed']=post['criteriaPassed']
        result=json.loads((HERE/'raw'/stage/'run.json').read_text());assert result['status']=='passed'
        cost=json.loads((HERE/'raw'/stage/'process-cost.json').read_text());assert cost['status']=='passed'
        ratios={actor:cost['cpuSeconds'][actor]/(result['results']['wholeRequestResources'][actor]['elapsedNanos']/1e9)
                for actor in ('baseline','candidate')}
        receipt['actorDelivery']={'nativeCpuToSearchElapsed':ratios,'absoluteDifference':abs(ratios['baseline']-ratios['candidate']),
            'passed':all(.90<=x<=1.25 for x in ratios.values()) and abs(ratios['baseline']-ratios['candidate'])<=.10}
        clock=module('clock_audit',HERE/'audit_spending_clock_v1.py').audit(stage)
        receipt['clockReliability']={k:clock[k] for k in ('clockReliabilityPassed','maximumRequestOverHardMillis')}
        receipt['clockAnalysisSha256']=digest(HERE/(stage+'-clock-analysis-v1.json'))
        receipt['results']={k:v for k,v in result['results'].items() if k!='games'}
        receipt['matchWallSeconds']=result['matchWallSeconds'];stable()
        receipt['status']='passed' if receipt['resourceQualification']['passed'] and post['criteriaPassed'] and receipt['actorDelivery']['passed'] else 'resource-unqualified'
        if not clock['clockReliabilityPassed']:receipt['status']='clock-unqualified'
    except BaseException as error:receipt.update(status='failed',failure=repr(error));raise
    finally:
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in sorted((HERE/'raw'/stage).glob('*')) if p.is_file()}
        write_new(HERE/(stage+'-summary.json'),receipt)
    print(json.dumps({'status':receipt['status'],'results':receipt.get('results'),'profile':profile}),flush=True)

if __name__=='__main__':main()
