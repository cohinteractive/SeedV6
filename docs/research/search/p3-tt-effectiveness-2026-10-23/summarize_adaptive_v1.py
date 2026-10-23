"""Reconcile the one frozen adaptive formulation and its prospective cost gate."""
from collections import Counter
import json
import math
import statistics
from run_diagnostic_v1 import digest, write_new, HERE

protocol=json.loads((HERE/'adaptive-v1.json').read_text())
report={'status':'passed','protocolSha256':digest(HERE/'adaptive-v1.json'),'experiments':{},'domainDecisions':{},
        'limits':['Development only; no strength inference from nodes/depth/time.','Diagnostic counters OFF; repeated rounds are not independent roots.','Caps and selective move differences retained. Heap snapshots are not native process peak working set.']}
for experiment in protocol['experiments']:
    name=experiment['name'];path=HERE/'raw'/f'{name}.jsonl'
    rows=[json.loads(line) for line in path.read_text().splitlines()];assert rows[-1]['status']=='passed'
    receipt=json.loads((HERE/f'{name}-execution.json').read_text());assert receipt['status']=='passed'
    measured=[r for r in rows if r.get('type')=='request' and r['round']>=0]
    by={(r['id'],r['round'],r['profile']):r for r in measured}
    result={'rawPath':str(path.relative_to(HERE)),'rawSha256':digest(path),'wallSeconds':receipt['wallSeconds'],
            'nativeCpuSeconds':receipt['totalOwnedCpuSeconds'],'profiles':{}}
    for profile in ('baseline','adaptive'):
        selected=[r for r in measured if r['profile']==profile];assert len(selected)==24
        base=[by[r['id'],r['round'],'baseline'] for r in selected]
        value={key:sum(r[key] for r in selected) for key in ('elapsedNanos','processCpuNanos','resetNanos','gcCollections','compilationMillis')}
        value.update(requests=len(selected),totalNodes=sum(r['outcome']['nodes'] for r in selected),
            completedDepths=dict(Counter(r['outcome']['lastCompletedResult']['depth'] for r in selected)),
            capped=sum(not r['outcome']['targetDepthCompleted'] for r in selected),
            differentMoves=sum(r['outcome']['lastCompletedResult']['bestMove']!=b['outcome']['lastCompletedResult']['bestMove'] for r,b in zip(selected,base)),
            elapsedTotalRatio=sum(r['elapsedNanos'] for r in selected)/sum(b['elapsedNanos'] for b in base),
            cpuTotalRatio=sum(r['processCpuNanos'] for r in selected)/sum(b['processCpuNanos'] for b in base),
            pairedElapsedGeometricRatio=math.exp(statistics.mean(math.log(r['elapsedNanos']/b['elapsedNanos']) for r,b in zip(selected,base))),
            heapUsedRange=[min(r['heapUsed'] for r in selected),max(r['heapUsed'] for r in selected)])
        result['profiles'][profile]=value
    candidate=[r for r in measured if r['profile']=='adaptive'];base=[by[r['id'],r['round'],'baseline'] for r in candidate]
    result['depthPairCounts']={name:sum(test(r['outcome']['lastCompletedResult']['depth'],b['outcome']['lastCompletedResult']['depth']) for r,b in zip(candidate,base))
        for name,test in [('lower',lambda a,b:a<b),('equal',lambda a,b:a==b),('higher',lambda a,b:a>b)]}
    report['experiments'][name]=result
for workers in (1,4):
    depth=report['experiments'][f'adaptive-d8-w{workers}-v1'];nodes=report['experiments'][f'adaptive-n250k-w{workers}-v1']
    a=depth['profiles']['adaptive'];b=depth['profiles']['baseline']
    checks={'elapsedAtMost0.90':a['elapsedTotalRatio']<=.90,'cpuAtMost0.90':a['cpuTotalRatio']<=.90,
            'targetCompletionsNoLower':a['capped']<=b['capped'],
            'nodeBudgetDepthNotUniformlyWorse':nodes['depthPairCounts']['higher']>0 or nodes['depthPairCounts']['lower']==0}
    report['domainDecisions'][str(workers)]={'checks':checks,'clockScreenAdmitted':all(checks.values()),
        'note':'A purely operational cost/depth selection gate. No strength acceptance; no width sweep on failure.'}
write_new(HERE/'adaptive-v1-summary.json',report)
print(json.dumps(report,indent=2))
