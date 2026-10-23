"""Reconcile the frozen deeper controls, retaining capped requests."""
import json
import math
import statistics
from collections import Counter
from pathlib import Path
from run_diagnostic_v1 import digest, write_new, HERE

report={'protocolSha256':digest(HERE/'deeper-v1.json'),'status':'passed','experiments':{},
        'limits':['Development roots, not independent playing-strength confirmation.',
                  'Capped requests retained; target-depth ratios are not equal completed-depth speedups.',
                  'Two rounds are repeated measurements, not independent positions.']}
for workers in (1,4):
    name=f'deeper-w{workers}-v1';path=HERE/'raw'/f'{name}.jsonl'
    rows=[json.loads(line) for line in path.read_text().splitlines()]
    assert rows[-1]['status']=='passed'
    receipt=json.loads((HERE/f'{name}-execution.json').read_text())
    assert receipt['status']=='passed'
    measured=[r for r in rows if r.get('type')=='request' and r['round']>=0]
    by={(r['id'],r['round'],r['profile']):r for r in measured}
    result={'rawPath':str(path.relative_to(HERE)),'rawSha256':digest(path),
            'receipt':f'{name}-execution.json','wallSeconds':receipt['wallSeconds'],'profiles':{}}
    for profile in ('baseline','full'):
        selected=[r for r in measured if r['profile']==profile];assert len(selected)==24
        base=[by[r['id'],r['round'],'baseline'] for r in selected]
        value={key:sum(r[key] for r in selected) for key in ('elapsedNanos','processCpuNanos','liveThreadAllocatedBytes','resetNanos','gcCollections','compilationMillis')}
        value.update(requests=len(selected),totalNodes=sum(r['outcome']['nodes'] for r in selected),
            completedDepths=dict(Counter(r['outcome']['lastCompletedResult']['depth'] for r in selected)),
            capped=sum(not r['outcome']['targetDepthCompleted'] for r in selected),
            heapUsedRange=[min(r['heapUsed'] for r in selected),max(r['heapUsed'] for r in selected)],
            differentMoves=sum(r['outcome']['lastCompletedResult']['bestMove']!=b['outcome']['lastCompletedResult']['bestMove'] for r,b in zip(selected,base)),
            pairedElapsedGeometricRatio=math.exp(statistics.mean(math.log(r['elapsedNanos']/b['elapsedNanos']) for r,b in zip(selected,base))),
            elapsedTotalRatio=sum(r['elapsedNanos'] for r in selected)/sum(b['elapsedNanos'] for b in base),
            cpuTotalRatio=sum(r['processCpuNanos'] for r in selected)/sum(b['processCpuNanos'] for b in base))
        result['profiles'][profile]=value
    report['experiments'][name]=result
write_new(HERE/'deeper-v1-summary.json',report)
print(json.dumps(report,indent=2))
