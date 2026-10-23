"""Require complete frozen outputs, then summarize every measured request without censoring."""
from collections import Counter
import json
import math
import statistics
from run_observation_v1 import HERE, digest, write_new

protocol=json.loads((HERE/'observation-v1.json').read_text())
report={'status':'passed','protocolSha256':digest(HERE/'observation-v1.json'),'experiments':{},
    'limits':['Development roots, not strength evidence.','Repeated rounds are not independent roots.',
              'Selective trees differ; elapsed/node savings alone are not gains.',
              'Guard-bypass protection counter overcounts actual protected eligible moves.',
              'Per-request CPU has OS resolution limits; report aggregate and paired wall measurements.']}
for e in protocol['experiments']:
    name=e['name'];path=HERE/'raw'/f'{name}.jsonl';receipt=json.loads((HERE/f'{name}-execution.json').read_text())
    assert receipt['status']=='passed' and digest(path)==receipt['evidence'][str(path.relative_to(HERE))]
    rows=[json.loads(s) for s in path.read_text().splitlines()];assert rows[-1]['status']=='passed'
    assert rows[0]['modelSha256']=='8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b'
    assert 'aspiration=ADAPTIVE16' in rows[0]['productionIdentity'] and 'build=70' in rows[0]['productionIdentity']
    result={'rawPath':str(path.relative_to(HERE)),'rawSha256':digest(path),'wallSeconds':receipt['wallSeconds'],
        'nativeLifetime':receipt['nativeLifetime'],'peakWorkingSetBytes':receipt['peakWorkingSetBytes']}
    if e['mode']=='observe':
        observed=[r for r in rows if r.get('type')=='observation'];assert len(observed)==e['roots']
        assert all(r['neutral'] for r in observed)
        counts=Counter();attempts=0;events=[0,0];samples=0;nodes=0
        for row in observed:
            nodes+=row['plain']['outcome']['nodes'];samples+=len(row['samples'])
            for i,n in enumerate(row['predictionEvents']):events[i]+=n
            for a in row['attempts']: counts.update(a['mechanisms']);attempts+=1
        result.update(roots=len(observed),neutral=True,counters=dict(counts),attempts=attempts,nodes=nodes,
            predictionEvents=events,samples=samples,
            capped=sum(not r['plain']['outcome']['targetDepthCompleted'] for r in observed),
            rootCounters=[{'id':r['id'],'counters':dict(sum((Counter(a['mechanisms']) for a in r['attempts']),Counter()))} for r in observed])
    else:
        measured=[r for r in rows if r.get('type')=='request' and r['round']>=0]
        profiles=e['profiles'].split(',');assert len(measured)==e['roots']*e['rounds']*len(profiles)
        by={(r['id'],r['round'],r['profile']):r for r in measured};assert len(by)==len(measured)
        result['profiles']={};result['rootComparisons']=[]
        for profile in profiles:
            selected=[r for r in measured if r['profile']==profile]
            base=[by[r['id'],r['round'],'baseline'] for r in selected]
            complete=[(r,b) for r,b in zip(selected,base) if r['outcome']['targetDepthCompleted'] and b['outcome']['targetDepthCompleted']]
            value={k:sum(r[k] for r in selected) for k in ('elapsedNanos','processCpuNanos','resetNanos','gcCollections','compilationMillis')}
            value.update(requests=len(selected),totalNodes=sum(r['outcome']['nodes'] for r in selected),
                completedDepths=dict(Counter(r['outcome']['lastCompletedResult']['depth'] for r in selected)),
                capped=sum(not r['outcome']['targetDepthCompleted'] for r in selected),
                differentMoves=sum(r['outcome']['lastCompletedResult']['bestMove']!=b['outcome']['lastCompletedResult']['bestMove'] for r,b in zip(selected,base)),
                differentScores=sum(r['outcome']['lastCompletedResult']['score']!=b['outcome']['lastCompletedResult']['score'] for r,b in zip(selected,base)),
                elapsedTotalRatio=sum(r['elapsedNanos'] for r in selected)/sum(r['elapsedNanos'] for r in base),
                cpuTotalRatio=sum(r['processCpuNanos'] for r in selected)/sum(r['processCpuNanos'] for r in base),
                nodeTotalRatio=sum(r['outcome']['nodes'] for r in selected)/sum(r['outcome']['nodes'] for r in base),
                elapsedIncludingResetRatio=sum(r['elapsedNanos']+r['resetNanos'] for r in selected)/sum(r['elapsedNanos']+r['resetNanos'] for r in base),
                pairedElapsedGeometricRatio=math.exp(statistics.mean(math.log(r['elapsedNanos']/b['elapsedNanos']) for r,b in zip(selected,base))),
                matchedTargetCompletedRequests=len(complete),
                matchedTargetElapsedRatio=sum(r['elapsedNanos'] for r,b in complete)/sum(b['elapsedNanos'] for r,b in complete) if complete else None,
                heapUsedRange=[min(r['heapUsed'] for r in selected),max(r['heapUsed'] for r in selected)])
            result['profiles'][profile]=value
            for r,b in zip(selected,base):
                a=r['outcome']['lastCompletedResult'];c=b['outcome']['lastCompletedResult']
                result['rootComparisons'].append({'id':r['id'],'round':r['round'],'profile':profile,
                    'baseline':{k:c[k] for k in ('depth','score','bestMove')},
                    'candidate':{k:a[k] for k in ('depth','score','bestMove')},
                    'elapsedRatio':r['elapsedNanos']/b['elapsedNanos'],
                    'nodesRatio':r['outcome']['nodes']/b['outcome']['nodes']})
    report['experiments'][name]=result
write_new(HERE/'observation-v1-summary.json',report)
for name,e in report['experiments'].items():
    print(name,json.dumps(e.get('counters',{})))
    for p,v in e.get('profiles',{}).items():print(p,json.dumps({k:v[k] for k in ('elapsedTotalRatio','cpuTotalRatio','nodeTotalRatio','capped','differentMoves','differentScores')}))
