"""Reconcile the closed diagnostic-v1 outputs; no strength inference."""
from collections import Counter
import hashlib
import json
import math
from pathlib import Path
import statistics

HERE=Path(__file__).resolve().parent


def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def geom(values): return math.exp(statistics.mean(math.log(x) for x in values))


def main():
    protocol=json.loads((HERE/'diagnostic-v1.json').read_text())
    report={'protocolSha256':digest(HERE/'diagnostic-v1.json'), 'status':'passed', 'experiments':{},
            'interpretation':'Descriptive development evidence; no Elo, equivalence or universal speed claim. Repeated rounds are not independent roots.',
            'limitations':['Historical development roots; 12 opening positions only.',
                           'Desktop workload uncontrolled; process CPU includes JVM background work.',
                           'Multiworker scheduling changes trees; one-worker evidence does not certify every worker count.',
                           'Observation timings are ordered plain then observed without warmup: no observer-overhead estimate.',
                           'Aborted invocations reset Construction counters; attempt mechanism maps omit their pre-abort work. Whole-request nodes/CPU and table counters still include aborted work.',
                           'Collision misses do not prove the missing key was previously produced.']}
    for experiment in protocol['experiments']:
        name=experiment['name']; path=HERE/'raw'/f'{name}.jsonl'
        rows=[json.loads(x) for x in path.read_text().splitlines()]
        receipt=json.loads((HERE/f'{name}-execution.json').read_text())
        assert receipt['status']=='passed' and rows[-1]['status']=='passed'
        result={'rawPath':str(path.relative_to(HERE)), 'rawSha256':digest(path),
                'receipt':f'{name}-execution.json', 'lifetimeSeconds':receipt['elapsedSeconds'],
                'lifetimeCpuSeconds':receipt['nativeLifetime']['totalCpuSeconds'],
                'peakWorkingSetBytes':receipt['peakWorkingSetBytes'], 'profiles':{}}
        if experiment['mode']=='observe':
            for profile in experiment['profiles'].split(','):
                selected=[r for r in rows if r.get('profile')==profile]
                assert len(selected)==experiment['roots'] and all(r['neutral'] for r in selected)
                tables=[r['table'] for r in selected]; totals={}
                for key in tables[0]:
                    if isinstance(tables[0][key],int): totals[key]=sum(t[key] for t in tables)
                    else: totals[key]=[sum(t[key][i] for t in tables) for i in range(256)]
                assert totals['proofStores']==totals['proofHits']==totals['occupiedProof']==0
                attempts=[a for r in selected for a in r['attempts']]
                failed=[a for a in attempts if a['outcome']['completed'] and
                        (a['outcome']['score']<=a['alpha'] or a['outcome']['score']>=a['beta'])]
                result['profiles'][profile]={'requests':len(selected), 'allNeutral':True, 'tableTotals':totals,
                    'meanOccupancyFraction':statistics.mean(t['occupiedHints']/t['entries'] for t in tables),
                    'occupancyRange':[min(t['occupiedHints']/t['entries'] for t in tables),max(t['occupiedHints']/t['entries'] for t in tables)],
                    'hintHitFraction':totals['hintHits']/totals['hintProbes'],
                    'completedDepths':dict(Counter(r['plain']['outcome']['lastCompletedResult']['depth'] for r in selected)),
                    'attempts':len(attempts), 'failedBounds':len(failed),
                    'abortedAttempts':sum(not a['outcome']['completed'] for a in attempts),
                    'totalNodes':sum(a['outcome']['nodes'] for a in attempts),
                    'failedBoundNodes':sum(a['outcome']['nodes'] for a in failed),
                    'failedBoundNodeFraction':sum(a['outcome']['nodes'] for a in failed)/sum(a['outcome']['nodes'] for a in attempts)}
        else:
            measured=[r for r in rows if r.get('type')=='request' and r['round']>=0]
            by={(r['round'],r['id'],r['profile']):r for r in measured}
            for profile in experiment['profiles'].split(','):
                selected=[r for r in measured if r['profile']==profile]
                assert len(selected)==experiment['roots']*experiment['rounds']
                base=[by[r['round'],r['id'],'baseline'] for r in selected]
                totals={key:sum(r[key] for r in selected) for key in ('elapsedNanos','processCpuNanos','liveThreadAllocatedBytes','resetNanos','gcCollections','compilationMillis')}
                ratios={key:sum(r[key] for r in selected)/sum(r[key] for r in base) for key in ('elapsedNanos','processCpuNanos','liveThreadAllocatedBytes')}
                elapsed=[r['elapsedNanos']/b['elapsedNanos'] for r,b in zip(selected,base)]
                nodes=[r['outcome']['nodes']/b['outcome']['nodes'] for r,b in zip(selected,base)]
                result['profiles'][profile]={'requests':len(selected), 'totals':totals, 'ratioOfTotals':ratios,
                    'pairedElapsedGeometricRatio':geom(elapsed), 'pairedElapsedRatioRange':[min(elapsed),max(elapsed)],
                    'elapsedPlusResetRatio':sum(r['elapsedNanos']+r['resetNanos'] for r in selected)/sum(r['elapsedNanos']+r['resetNanos'] for r in base),
                    'totalNodes':sum(r['outcome']['nodes'] for r in selected), 'pairedNodesGeometricRatio':geom(nodes),
                    'completedDepths':dict(Counter(r['outcome']['lastCompletedResult']['depth'] for r in selected)),
                    'cappedRequests':sum(not r['outcome']['targetDepthCompleted'] for r in selected),
                    'differentMovesToBaseline':sum(r['outcome']['lastCompletedResult']['bestMove']!=b['outcome']['lastCompletedResult']['bestMove'] for r,b in zip(selected,base)),
                    'aspirationRetries':sum(r['outcome']['diagnostics']['iteration']['failLowResearches']+r['outcome']['diagnostics']['iteration']['failHighResearches'] for r in selected),
                    'fullWindowFallbacks':sum(r['outcome']['diagnostics']['iteration']['fullWindowFallbacks'] for r in selected),
                    'heapUsedRange':[min(r['heapUsed'] for r in selected),max(r['heapUsed'] for r in selected)]}
        report['experiments'][name]=result
    with (HERE/'diagnostic-v1-summary.json').open('x',encoding='utf8',newline='\n') as out:
        json.dump(report,out,indent=2);out.write('\n')
    for name,r in report['experiments'].items():
        print(name,'lifetime seconds',round(r['lifetimeSeconds'],2),'CPU',round(r['lifetimeCpuSeconds'],2))
        for p,v in r['profiles'].items():
            print(p,{k:v[k] for k in ('hintHitFraction','pairedElapsedGeometricRatio','pairedNodesGeometricRatio','ratioOfTotals','completedDepths','cappedRequests') if k in v})


if __name__=='__main__': main()
