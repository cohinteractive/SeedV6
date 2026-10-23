"""Full endpoint reconciliation; distinguish partial invocation counters from persistent traces."""
from collections import Counter
import json
import math
import statistics
from run_observation_v1 import HERE, digest, write_new

protocol=json.loads((HERE/'separation-v1.json').read_text())
report={'status':'passed','protocolSha256':digest(HERE/'separation-v1.json'),'experiments':{},
    'limits':['Development only, no strength inference.','Repeated rounds and related positions are not independent samples.',
        'Construction counters reset on abort and cover completed attempts; external opportunity trace includes aborted work.',
        'Null surplus and reduction-futility opportunities are not calibrated prediction quality.',
        'Multiworker scheduling prevents exact node/tree equivalence; heap/peak are cohort observations.']}
for e in protocol['experiments']:
    name=e['name'];path=HERE/'raw'/f'{name}.jsonl';receipt=json.loads((HERE/f'{name}-execution.json').read_text())
    assert receipt['status']=='passed' and digest(path)==receipt['evidence'][str(path.relative_to(HERE))]
    rows=[json.loads(x) for x in path.read_text().splitlines()];assert rows[-1]['status']=='passed'
    assert 'build=71' in rows[0]['productionIdentity'] and 'flags=262143' in rows[0]['productionIdentity']
    value={'rawPath':str(path.relative_to(HERE)),'rawSha256':digest(path),'wallSeconds':receipt['wallSeconds'],
        'nativeLifetime':receipt['nativeLifetime'],'peakWorkingSetBytes':receipt['peakWorkingSetBytes'],'profiles':{}}
    if name=='baseline-regression-v1':assert receipt['build70DefaultRequestParity'];value['build70DefaultRequestParity']=True
    if e['mode']=='observe':
        assert receipt['lazyTraversalParity'];value['lazyTraversalParity']=True
        for profile in e['profiles'].split(','):
            chosen=[r for r in rows if r.get('type')=='observation' and r['profile']==profile]
            assert len(chosen)==e['roots'] and all(r['neutral'] for r in chosen)
            counters=Counter();counts=[0]*9;surviving={};reductions={};samples=Counter();aborted=0
            for r in chosen:
                for a in r['attempts']:
                    counters.update(a['mechanisms'])
                    if not a['outcome']['completed']:aborted+=a['outcome']['nodes']
                o=r['opportunities'];counts=[a+b for a,b in zip(counts,o['counts'])]
                samples.update(s['kind'] for s in o['samples'])
                for d,row in enumerate(o['surviving']):
                    if sum(row):surviving[str(d)]=[a+b for a,b in zip(surviving.get(str(d),[0]*6),row)]
                for d,children in enumerate(o['reductions']):
                    for child,signs in enumerate(children):
                        if sum(signs):
                            key=f'{d}/{child}';reductions[key]=[a+b for a,b in zip(reductions.get(key,[0]*3),signs)]
            labels=['survivingMainNodes','guardedDeepScoutNodes','rawAtLeastBetaNodes','laterSearchedMoves',
                    'laterSearchedQuietMoves','negativeHistoryQuiets','zeroHistoryQuiets','baseReductionMoves','reductionFutilityOpportunities']
            value['profiles'][profile]={'completedAttemptCounters':dict(counters),'abortedNodesExcludedFromThoseCounters':aborted,
                'traceCountsIncludingAborted':dict(zip(labels,counts)),'deepScoutSurplusByDepth':surviving,
                'reductionDepthHistorySigns':reductions,'samples':dict(samples),
                'surplusColumns':['notEvaluated','belowBeta','0..99','100..299','300..599','600+'],
                'reductionKey':'nominalDepth/assignedChildDepth; negative,zero,positive main-history counts'}
    else:
        measured=[r for r in rows if r.get('type')=='request' and r['round']>=0]
        assert len(measured)==e['roots']*e['rounds']*len(e['profiles'].split(','))
        by={(r['id'],r['round'],r['profile']):r for r in measured};assert len(by)==len(measured)
        for profile in e['profiles'].split(','):
            chosen=[r for r in measured if r['profile']==profile];base=[by[r['id'],r['round'],'baseline'] for r in chosen]
            v={k:sum(r[k] for r in chosen) for k in ('elapsedNanos','processCpuNanos','resetNanos','gcCollections','compilationMillis')}
            v.update(requests=len(chosen),nodes=sum(r['outcome']['nodes'] for r in chosen),
                capped=sum(not r['outcome']['targetDepthCompleted'] for r in chosen),
                completedDepths=dict(Counter(r['outcome']['lastCompletedResult']['depth'] for r in chosen)),
                differentMoves=sum(r['outcome']['lastCompletedResult']['bestMove']!=b['outcome']['lastCompletedResult']['bestMove'] for r,b in zip(chosen,base)),
                elapsedRatio=sum(r['elapsedNanos'] for r in chosen)/sum(r['elapsedNanos'] for r in base),
                cpuRatio=sum(r['processCpuNanos'] for r in chosen)/sum(r['processCpuNanos'] for r in base),
                nodesRatio=sum(r['outcome']['nodes'] for r in chosen)/sum(r['outcome']['nodes'] for r in base),
                elapsedIncludingResetRatio=sum(r['elapsedNanos']+r['resetNanos'] for r in chosen)/sum(r['elapsedNanos']+r['resetNanos'] for r in base),
                geometricElapsedRatio=math.exp(statistics.mean(math.log(r['elapsedNanos']/b['elapsedNanos']) for r,b in zip(chosen,base))),
                depthPairCounts={label:sum(fn(r['outcome']['lastCompletedResult']['depth'],b['outcome']['lastCompletedResult']['depth']) for r,b in zip(chosen,base))
                    for label,fn in [('lower',lambda a,b:a<b),('equal',lambda a,b:a==b),('higher',lambda a,b:a>b)]})
            if e['workers']==1 and profile=='lazy-improving':
                assert all(r['outcome']==b['outcome'] for r,b in zip(chosen,base));v['exactTraversalParity']=True
            value['profiles'][profile]=v
    report['experiments'][name]=value
write_new(HERE/'separation-v1-summary.json',report)
for name,e in report['experiments'].items():
    print(name)
    for p,v in e['profiles'].items():print(p,json.dumps(v.get('traceCountsIncludingAborted') or {k:v[k] for k in ('elapsedRatio','cpuRatio','nodesRatio','capped','differentMoves','depthPairCounts')}))
