"""Freeze and measure owner-thread delivery before any clock outcome exists."""
import json, math, shutil, statistics, subprocess, time
from datetime import datetime, timezone
from run_observation_v1 import HERE, ROOT, digest, write_new
from run_clock_v1 import module

def main():
    name='cpu-delivery-v1'; runtime=HERE/'raw/runtime-cpu-delivery-v1'
    assert not runtime.exists();shutil.copytree(ROOT/'build/research/runtime',runtime)
    model=ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'
    corpus=ROOT/'docs/research/search/sr006-phase2-2026-10-10/played-corpus.tsv'
    files=[HERE/(name+'.json'),__file__,model,corpus,ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64CpuDelivery.java']
    files+=sorted(p for p in runtime.rglob('*') if p.is_file())
    binding={'frozenAtUtc':datetime.now(timezone.utc).isoformat(),'files':{str(p):digest(p) for p in files}}
    write_new(HERE/(name+'-binding.json'),binding)
    host=module('host',ROOT/'tools/p3-tt-session.py');native=module('native',ROOT/'tools/sr036-process-cost.py')
    output=HERE/'raw'/(name+'.jsonl');log=HERE/'raw'/(name+'.log')
    command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true','-Xms256m','-Xmx1024m',
        '-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+';'+str(runtime/'lib/*'),
        'com.ohinteractive.seedv6.search.exact.Cp64CpuDelivery',str(model),str(corpus),str(output)]
    start=host.host_sample();receipt={'command':command,'bindingSha256':digest(HERE/(name+'-binding.json')),'status':'running'}
    try:
        with log.open('xb') as stream:
            process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
                creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS|subprocess.CREATE_NO_WINDOW)
            try:receipt['exitCode']=process.wait(timeout=1800)
            except BaseException:process.kill();process.wait();raise
            finally:receipt['nativeLifetime']=native.snapshot(int(process._handle))
        end=host.host_sample();receipt['hostInterval']=host.host_interval(start,end,receipt['nativeLifetime']['totalCpuSeconds'])
        assert receipt['exitCode']==0
        rows=[json.loads(x) for x in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
        measured=[x for x in rows if x.get('type')=='request' and x['round']>=0];assert len(measured)==72
        summaries={}
        for profile in ['baseline','verified-null','nominal-verified-null']:
            selected=[x for x in measured if x['profile']==profile];assert len(selected)==24
            ratios=sorted(x['ownerCpuToElapsed'] for x in selected)
            summaries[profile]={'requests':24,'median':statistics.median(ratios),'fifthPercentileNearestRank':ratios[math.ceil(.05*24)-1],
                'minimum':min(ratios),'maximum':max(ratios),'aggregate':sum(x['ownerThreadCpuNanos'] for x in selected)/sum(x['elapsedNanos'] for x in selected),
                'processCpuToElapsed':sum(x['processCpuNanos'] for x in selected)/sum(x['elapsedNanos'] for x in selected),
                'gcCollections':sum(x['gcCollections'] for x in selected),'compilationMillis':sum(x['compilationMillis'] for x in selected)}
        receipt['profiles']=summaries;spread=max(x['median'] for x in summaries.values())-min(x['median'] for x in summaries.values())
        receipt['betweenProfileMedianRange']=spread
        criteria=json.loads((HERE/(name+'.json')).read_text())['criteria'];interval=receipt['hostInterval']
        receipt['criteriaPassed']=all(x['median']>=criteria['perProfileMedianOwnerCpuToElapsedMinimum'] and x['fifthPercentileNearestRank']>=criteria['perProfileNearestRankFifthPercentileMinimum'] for x in summaries.values()) and spread<=criteria['maximumBetweenProfileMedianRange'] and interval['otherAverageLogicalCores']<=criteria['otherAverageLogicalCoresMaximum'] and interval['memoryEndpointsPass']
        for p,sha in binding['files'].items():assert digest(p)==sha,p
        receipt['status']='passed'
    except BaseException as error:receipt.update(status='failed',failure=repr(error));raise
    finally:
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
        write_new(HERE/(name+'-summary.json'),receipt)
    print(json.dumps(receipt,indent=2))

if __name__=='__main__':main()
