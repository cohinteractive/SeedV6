"""Run the retained H1 screen once, under prospectively qualified resources."""
import ctypes,json,sys,time
from datetime import datetime
from types import SimpleNamespace
from run_observation_v1 import HERE,ROOT,digest,write_new
from run_clock_v1 import module
import removal_resource_v2 as resource

def main():
    name='removal-clock-v2';plan=json.loads((HERE/(name+'.json')).read_text())
    if len(sys.argv)>1 and sys.argv[1]=='worker':
        assert sys.stdin.readline().strip()=='GO'
        m=module('match',ROOT/'tools/p3-selectivity-match.py');runtime=str(HERE/'raw/runtime-v2')
        m.run(SimpleNamespace(baseline=runtime,candidate=runtime,network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
            runner=str(ROOT/'build/research/tools/fastchess.exe'),openings=str(HERE/'clock-development-v1-openings.pgn'),out=str(HERE/'raw'/name),
            java='C:/java/jdk-21/bin/java.exe',baseline_policy='baseline',candidate_policy='no-improving',baseline_workers=1,candidate_workers=1,
            hash_mib=64,background=True,operational=False,concurrency=1,pairs=32,tc='10+0.1',seed=plan['domain']['seed'],timeout=5400));return
    for other in ['null-clock-primary-v2','null-clock-increment-v2']:
        assert (HERE/(other+'-summary.json')).exists(),'Finish prior game and post-delivery work'
    assert not (HERE/'raw'/name).exists() and not (HERE/(name+'-summary.json')).exists(),'Never replay a started stage'
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.GetCurrentProcess.restype=ctypes.c_void_p
    k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong];assert k.SetPriorityClass(k.GetCurrentProcess(),0x4000)
    binding=json.loads((HERE/(name+'-binding.json')).read_text())
    def stable():
        for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
        for p,sha in binding['externalFiles'].items():assert digest(p)==sha,p
    stable();host=module('host',ROOT/'tools/p3-tt-session.py');owned=module('owned',ROOT/'tools/tournament-process.py')
    baseline=module('baseline',ROOT/'tools/search-baseline.py');pre=resource.current_delivery(name)
    prior=sorted(HERE.glob(name+'-admission-*.json'));attempt=len(prior)+1
    if prior:
        last=json.loads(prior[-1].read_text());assert not last['passed']
        assert (datetime.now().astimezone()-datetime.fromisoformat(last['end']['utc'])).total_seconds()>=900
    start=host.host_sample();time.sleep(30);admission=resource.host_interval(start,host.host_sample())
    admissionFile=HERE/f'{name}-admission-{attempt:02}.json';write_new(admissionFile,admission)
    if not admission['passed']:raise SystemExit('H1 resource admission failed; no games launched')
    start=host.host_sample();receipt={'status':'running','preDelivery':pre,'bindingSha256':digest(HERE/(name+'-binding.json')),'admissionSha256':digest(admissionFile)}
    write_new(HERE/(name+'-launch.json'),receipt|{'hostStart':start})
    try:
        with (HERE/'raw'/(name+'-worker.log')).open('xb') as log:
            receipt['execution']=owned.run_owned([sys.executable,'-B',str(HERE/'run_removal_clock_v2.py'),'worker'],log,5500,ROOT,baseline.clean_environment())
        receipt['resourceQualification']=resource.host_interval(start,host.host_sample(),receipt['execution']['totalOwnedCpuSeconds'])
        assert receipt['execution']['exitCode']==0
        post=resource.delivery(name+'-post-delivery');receipt['postDelivery']={'path':name+'-post-delivery-summary.json','sha256':digest(HERE/(name+'-post-delivery-summary.json'))}
        result=json.loads((HERE/'raw'/name/'run.json').read_text());assert result['status']=='passed';stable()
        cost=json.loads((HERE/'raw'/name/'process-cost.json').read_text());receipt['actorDelivery']=resource.actor_delivery(result['results'],cost)
        receipt['results']=result['results'];receipt['matchWallSeconds']=result['matchWallSeconds']
        receipt['status']='passed' if receipt['resourceQualification']['passed'] and post['criteriaPassed'] and receipt['actorDelivery']['passed'] else 'resource-unqualified'
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in sorted((HERE/'raw'/name).iterdir()) if p.is_file()}
    except BaseException as error:receipt.update(status='failed',failure=repr(error));raise
    finally:write_new(HERE/(name+'-summary.json'),receipt)
    print(json.dumps({'status':receipt['status'],'resourceQualification':receipt['resourceQualification'],'actorDelivery':receipt['actorDelivery'],
        'results':{k:v for k,v in receipt['results'].items() if k!='games'}},indent=2))

if __name__=='__main__':main()
