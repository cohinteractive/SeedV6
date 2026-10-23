"""Actual H1 profile CPU delivery using its unchanged frozen runtime."""
import json,math,statistics,subprocess
from datetime import datetime,timezone
from run_observation_v1 import HERE,ROOT,digest,write_new
from run_clock_v1 import module
from clock_resource_v2 import host_interval,actor_delivery

def delivery(name):
    binding=json.loads((HERE/'removal-cpu-v1-binding.json').read_text())
    for p,sha in binding['files'].items():assert digest(p)==sha,p
    output=HERE/'raw'/(name+'.jsonl');log=HERE/'raw'/(name+'.log');command=binding['commandBase']+[str(output)]
    host=module('host',ROOT/'tools/p3-tt-session.py');native=module('native',ROOT/'tools/sr036-process-cost.py')
    start=host.host_sample();receipt={'command':command,'bindingSha256':digest(HERE/'removal-cpu-v1-binding.json'),'status':'running'}
    try:
        with log.open('xb') as stream:
            process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
                creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS|subprocess.CREATE_NO_WINDOW)
            try:receipt['exitCode']=process.wait(timeout=1800)
            except BaseException:process.kill();process.wait();raise
            finally:receipt['nativeLifetime']=native.snapshot(int(process._handle))
        receipt['hostInterval']=host_interval(start,host.host_sample(),receipt['nativeLifetime']['totalCpuSeconds'])
        assert receipt['exitCode']==0
        rows=[json.loads(x) for x in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
        measured=[x for x in rows if x.get('type')=='request' and x['round']>=0];assert len(measured)==48
        profiles={}
        for profile in ['baseline','no-improving']:
            selected=[x for x in measured if x['profile']==profile];assert len(selected)==24
            ratios=sorted(x['ownerCpuToElapsed'] for x in selected)
            profiles[profile]={'requests':24,'median':statistics.median(ratios),'fifthPercentileNearestRank':ratios[math.ceil(.05*24)-1],
                'minimum':min(ratios),'maximum':max(ratios),'aggregate':sum(x['ownerThreadCpuNanos'] for x in selected)/sum(x['elapsedNanos'] for x in selected),
                'processCpuToElapsed':sum(x['processCpuNanos'] for x in selected)/sum(x['elapsedNanos'] for x in selected),
                'gcCollections':sum(x['gcCollections'] for x in selected),'compilationMillis':sum(x['compilationMillis'] for x in selected)}
        receipt['profiles']=profiles;spread=max(x['median'] for x in profiles.values())-min(x['median'] for x in profiles.values())
        receipt['betweenProfileMedianRange']=spread
        receipt['criteriaPassed']=all(x['median']>=.90 and x['fifthPercentileNearestRank']>=.80 for x in profiles.values()) and spread<=.05 and receipt['hostInterval']['passed']
        for p,sha in binding['files'].items():assert digest(p)==sha,p
        receipt['status']='passed'
    except BaseException as error:receipt.update(status='failed',failure=repr(error));raise
    finally:
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
        write_new(HERE/(name+'-summary.json'),receipt)
    return receipt

def current_delivery(stage):
    candidates=[]
    for path in [HERE/'removal-cpu-v1-summary.json',*HERE.glob('removal-clock-*-delivery-summary.json')]:
        record=json.loads(path.read_text())
        if record.get('status')=='passed' and record.get('criteriaPassed'):
            candidates.append((datetime.fromisoformat(record['hostInterval']['end']['utc']),path))
    end,path=max(candidates)
    if (datetime.now(timezone.utc)-end).total_seconds()>900:
        path=HERE/(stage+'-pre-delivery-summary.json');record=delivery(stage+'-pre-delivery')
    else:record=json.loads(path.read_text())
    assert record['status']=='passed' and record['criteriaPassed']
    return {'path':str(path.relative_to(HERE)),'sha256':digest(path)}
