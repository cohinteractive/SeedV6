"""Completion-driven serial W1 match with prospective pre/post resource gates."""
import ctypes,json,sys,time
from datetime import datetime
from types import SimpleNamespace
from run_observation_v1 import HERE, ROOT, digest, write_new
from run_clock_v1 import module
import clock_resource_v2 as resource

def main():
    which=sys.argv[1];assert which in ('primary','increment')
    plan=json.loads((HERE/'null-clock-development-v2.json').read_text());stage=plan['stages'][which=='increment'];name=stage['name']
    if len(sys.argv)>2 and sys.argv[2]=='worker':
        assert sys.stdin.readline().strip()=='GO'
        m=module('match',ROOT/'tools/p3-selectivity-null-match.py');runtime=str(HERE/'raw/runtime-v4')
        m.run(SimpleNamespace(baseline=runtime,candidate=runtime,network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
            runner=str(ROOT/'build/research/tools/fastchess.exe'),openings=str(HERE/(name+'-openings.pgn')),out=str(HERE/'raw'/name),
            java='C:/java/jdk-21/bin/java.exe',baseline_policy=stage['baseline'],candidate_policy=stage['candidate'],
            baseline_workers=1,candidate_workers=1,hash_mib=64,background=True,operational=False,concurrency=1,pairs=32,
            tc='10+0.1',seed=plan['domain']['seed']+(which=='increment'),timeout=5400));return
    if which=='increment':
        previous=json.loads((HERE/'null-clock-primary-v2-summary.json').read_text())
        assert previous['status']=='passed' and previous['results']['statistics']['score']>=.55,'Increment selection gate not met'
    assert not (HERE/'raw'/name).exists(),'Never replay a started match'
    assert not (HERE/(name+'-summary.json')).exists()
    for other in plan['stages']:
        if (HERE/'raw'/other['name']).exists():assert (HERE/(other['name']+'-summary.json')).exists(),'Unclosed match'
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.GetCurrentProcess.restype=ctypes.c_void_p
    k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong];assert k.SetPriorityClass(k.GetCurrentProcess(),0x4000)
    binding=json.loads((HERE/'null-clock-development-v2-binding.json').read_text())
    def stable():
        for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
        for p,sha in binding['externalFiles'].items():assert digest(p)==sha,p
    stable();host=module('host',ROOT/'tools/p3-tt-session.py');owned=module('owned',ROOT/'tools/tournament-process.py')
    baseline=module('baseline',ROOT/'tools/search-baseline.py')
    prior=sorted(HERE.glob(name+'-admission-*.json'));attempt=len(prior)+1
    if prior:
        last=json.loads(prior[-1].read_text());assert not last['passed']
        assert (datetime.now().astimezone()-datetime.fromisoformat(last['end']['utc'])).total_seconds()>=900
    pre=resource.current_delivery(name)
    start=host.host_sample();time.sleep(30);admission=resource.host_interval(start,host.host_sample())
    admissionFile=HERE/f'{name}-admission-{attempt:02}.json';write_new(admissionFile,admission)
    if not admission['passed']:raise SystemExit('New resource admission failed; no games launched')
    start=host.host_sample();receipt={'status':'running','stage':stage,'preDelivery':pre,
        'bindingSha256':digest(HERE/'null-clock-development-v2-binding.json'),'admissionSha256':digest(admissionFile)}
    write_new(HERE/(name+'-launch.json'),receipt|{'hostStart':start})
    try:
        with (HERE/'raw'/(name+'-worker.log')).open('xb') as log:
            receipt['execution']=owned.run_owned([sys.executable,'-B',str(HERE/'run_null_clock_v2.py'),which,'worker'],log,5500,ROOT,baseline.clean_environment())
        receipt['resourceQualification']=resource.host_interval(start,host.host_sample(),receipt['execution']['totalOwnedCpuSeconds'])
        assert receipt['execution']['exitCode']==0
        # Do not select or inspect outcomes until all integrity/resource observations are complete.
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
