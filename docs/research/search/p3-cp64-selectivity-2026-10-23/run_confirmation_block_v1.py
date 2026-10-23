"""One immutable confirmation block; expose integrity/resources, never interim scores."""
import ctypes,json,sys,time
from datetime import datetime,timezone
from types import SimpleNamespace
from run_observation_v1 import HERE,ROOT,digest,write_new
from run_clock_v1 import module

def main():
    hypothesis=sys.argv[1];block=int(sys.argv[2]);assert 1<=block<=4
    binding=json.loads((HERE/'confirmation-v1-binding.json').read_text());assert hypothesis in binding['admitted']
    stage=binding['stages'][hypothesis];name=stage['blocks'][block-1]
    resource=module('resource',HERE/(stage['resourceModule']+'.py'))
    if len(sys.argv)>3 and sys.argv[3]=='worker':
        assert sys.stdin.readline().strip()=='GO'
        adapter='p3-selectivity-null-match.py' if hypothesis=='null' else 'p3-selectivity-match.py'
        m=module('match',ROOT/'tools'/adapter);runtime=str(HERE/stage['runtime'])
        m.run(SimpleNamespace(baseline=runtime,candidate=runtime,network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
            runner=str(ROOT/'build/research/tools/fastchess.exe'),openings=str(HERE/(name+'-openings.pgn')),out=str(HERE/'raw'/name),
            java='C:/java/jdk-21/bin/java.exe',baseline_policy='baseline',candidate_policy=stage['candidate'],
            baseline_workers=1,candidate_workers=1,hash_mib=64,background=True,operational=False,concurrency=1,pairs=32,
            tc='10+0.1',seed=stage['seed']+block,timeout=5400));return
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.GetCurrentProcess.restype=ctypes.c_void_p
    k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong];assert k.SetPriorityClass(k.GetCurrentProcess(),0x4000)
    def stable():
        for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
        for p,sha in binding['externalFiles'].items():assert digest(p)==sha,p
    stable();assert not (HERE/'raw'/name).exists() and not (HERE/(name+'-summary.json')).exists(),'Never replay a started block'
    for other in binding['stages'].values():
        for other_name in other['blocks']:
            assert not (HERE/'raw'/other_name).exists() or (HERE/(other_name+'-summary.json')).exists(),'Unclosed block: '+other_name
    previous=[]
    for earlier in stage['blocks'][:block-1]:
        receipt=json.loads((HERE/(earlier+'-summary.json')).read_text());assert receipt['status']=='passed',earlier
        for p,sha in receipt['evidence'].items():assert digest(HERE/p)==sha,p
        previous.append(receipt)
    prior=sorted(HERE.glob(name+'-admission-*.json'));attempt=len(prior)+1
    if prior:
        last=json.loads(prior[-1].read_text());assert not last['passed']
        assert (datetime.now(timezone.utc)-datetime.fromisoformat(last['end']['utc'])).total_seconds()>=900
    pre=None
    if previous:
        candidate=previous[-1]['postDelivery'];path=HERE/candidate['path'];assert digest(path)==candidate['sha256']
        value=json.loads(path.read_text());assert value['status']=='passed' and value['criteriaPassed']
        if (datetime.now(timezone.utc)-datetime.fromisoformat(value['hostInterval']['end']['utc'])).total_seconds()<=900:pre=candidate
    if pre is None:pre=resource.current_delivery(f'{name}-a{attempt:02}')
    host=module('host',ROOT/'tools/p3-tt-session.py');owned=module('owned',ROOT/'tools/tournament-process.py');b=module('baseline',ROOT/'tools/search-baseline.py')
    start=host.host_sample();time.sleep(30);admission=resource.host_interval(start,host.host_sample())
    admissionFile=HERE/f'{name}-admission-{attempt:02}.json';write_new(admissionFile,admission)
    if not admission['passed']:raise SystemExit('Prelaunch resource gate failed; block unplayed')
    start=host.host_sample();receipt={'status':'running','hypothesis':hypothesis,'block':block,'preDelivery':pre,
        'bindingSha256':digest(HERE/'confirmation-v1-binding.json'),'admissionSha256':digest(admissionFile)}
    write_new(HERE/(name+'-launch.json'),receipt|{'hostStart':start})
    try:
        with (HERE/'raw'/(name+'-worker.log')).open('xb') as log:
            receipt['execution']=owned.run_owned([sys.executable,'-B',str(HERE/'run_confirmation_block_v1.py'),hypothesis,str(block),'worker'],log,5500,ROOT,b.clean_environment())
        receipt['resourceQualification']=resource.host_interval(start,host.host_sample(),receipt['execution']['totalOwnedCpuSeconds'])
        assert receipt['execution']['exitCode']==0
        post=resource.delivery(name+'-post-delivery');receipt['postDelivery']={'path':name+'-post-delivery-summary.json','sha256':digest(HERE/(name+'-post-delivery-summary.json'))}
        result=json.loads((HERE/'raw'/name/'run.json').read_text());assert result['status']=='passed';stable()
        cost=json.loads((HERE/'raw'/name/'process-cost.json').read_text());receipt['actorDelivery']=resource.actor_delivery(result['results'],cost)
        receipt['matchWallSeconds']=result['matchWallSeconds']
        receipt['integrity']={'terminations':result['results']['terminations'],'wholeRequestResources':result['results']['wholeRequestResources'],
            'inputsStable':result['inputsStable'],'scoreInspection':'Deferred until all four blocks complete; no score field copied into this receipt'}
        receipt['status']='passed' if receipt['resourceQualification']['passed'] and post['criteriaPassed'] and receipt['actorDelivery']['passed'] else 'resource-unqualified'
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in sorted((HERE/'raw'/name).iterdir()) if p.is_file()}
        receipt['evidence'][str((HERE/'raw'/(name+'-worker.log')).relative_to(HERE))]=digest(HERE/'raw'/(name+'-worker.log'))
    except BaseException as error:receipt.update(status='failed',failure=repr(error));raise
    finally:write_new(HERE/(name+'-summary.json'),receipt)
    print(json.dumps({'status':receipt['status'],'hypothesis':hypothesis,'block':block,
        'resourceQualification':receipt['resourceQualification'],'actorDelivery':receipt['actorDelivery'],
        'postDeliveryPassed':post['criteriaPassed'],'scores':'Unopened'},indent=2))

if __name__=='__main__':main()
