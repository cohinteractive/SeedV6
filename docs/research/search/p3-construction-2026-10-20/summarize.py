"""Rebuild compact summaries from preserved, completed local JSONL evidence."""
import collections,hashlib,json,pathlib,statistics
base=pathlib.Path(__file__).resolve().parent;raw=base/'raw'
def read(name):
    path=raw/(name+'.jsonl');rows=[json.loads(x) for x in path.read_text().splitlines()]
    assert rows[-1]=={'status':'passed'},name
    return rows
def save(name,value):
    (base/name).write_bytes((json.dumps(value,indent=2)+'\n').encode())
cost={}
for suffix in ['-v1', '-v2']:
    for mode in ['depth','nodes']:
        for workers in [1,4]:
            name=f'{mode}-w{workers}'+suffix;rows=read(name)
            receipt=json.loads((raw/(name+'.execution.json')).read_text())
            assert receipt['exitCode']==0
            allrequests=[x for x in rows if x.get('type')=='request']
            requests=[x for x in allrequests if x['round']>=0]
            profiles={}
            for profile in ['flat-policy4','rich-full','rich-aspiration']:
                sample=[x for x in requests if x['profile']==profile]
                iterations=[x['outcome']['diagnostics']['iteration'] for x in sample]
                profiles[profile]={'requests':len(sample),
                    'targetDepthCompleted':sum(x['outcome']['targetDepthCompleted'] for x in sample),
                    'nodes':sum(x['outcome']['nodes'] for x in sample),
                    'elapsedMillis':sum(x['elapsedNanos'] for x in sample)/1e6,
                    'processCpuMillis':sum(x['processCpuNanos'] for x in sample)/1e6,
                    'liveThreadAllocatedMiB':sum(x['liveThreadAllocatedBytes'] for x in sample)/2**20,
                    'resetMillis':sum(x['resetNanos'] for x in sample)/1e6,
                    'gcCollections':sum(x['gcCollections'] for x in sample),
                    'compilationMillis':sum(x['compilationMillis'] for x in sample),
                    'heapUsedMiBRange':[min(x['heapUsed'] for x in sample)/2**20,max(x['heapUsed'] for x in sample)/2**20],
                    'deepestCompletedRange':[min(x['deepestCompletedDepth'] for x in iterations),max(x['deepestCompletedDepth'] for x in iterations)],
                    'aspirationAttempts':sum(x['aspirationAttempts'] for x in iterations),
                    'failLowResearches':sum(x['failLowResearches'] for x in iterations),
                    'failHighResearches':sum(x['failHighResearches'] for x in iterations),
                    'fullWindowFallbacks':sum(x['fullWindowFallbacks'] for x in iterations)}
            ratios={}
            for profile in ['rich-full','rich-aspiration']:
                ratios[profile]={k:profiles[profile][k]/profiles['flat-policy4'][k]
                    for k in ['nodes','elapsedMillis','processCpuMillis','liveThreadAllocatedMiB']}
                perroot=[]
                for root in sorted({x['id'] for x in requests}):
                    numerator=sum(x['elapsedNanos'] for x in requests if x['id']==root and x['profile']==profile)
                    denominator=sum(x['elapsedNanos'] for x in requests if x['id']==root and x['profile']=='flat-policy4')
                    perroot.append(numerator/denominator)
                ratios[profile]['rootMeanElapsedRatioMinMedianMax']=[min(perroot),statistics.median(perroot),max(perroot)]
            cost[name]={'inputSha256':hashlib.sha256((raw/(name+'.jsonl')).read_bytes()).hexdigest(),
                'warmupRequests':len(allrequests)-len(requests),'measuredRequests':len(requests),
                'profiles':profiles,'ratiosToFlatPolicy4':ratios,'runnerElapsedSeconds':receipt['runnerElapsedSeconds']}
save('cost-summary.json',{'limitations':'Same reused 12 roots, diagnostic budgets, one host; JIT/GC/OS effects retained. Allocation counts only threads live at request start. Three drivers coexist; heap is not isolated per-engine memory. No strength evidence. Baseline is flat Policy 4 in the shared current runtime, not the recursive production binary.','runs':cost})
rows=read('observe-v1');mechanisms=[x for x in rows if x.get('type')=='mechanisms'];audits=[x for x in rows if x.get('type')=='predictionAudit'];shallow=[x for x in rows if x.get('type')=='shallowReference']
counts=collections.Counter()
for x in mechanisms:counts.update(x['counters'])
witness=[json.loads(x) for x in (raw/'shallow-witness-v1.jsonl').read_text().splitlines()]
save('diagnostic-summary.json',{'roots':len(mechanisms),'completedDepth6':sum(x['outcome']['completed'] for x in mechanisms),
    'mechanismCounters':dict(counts),'counterInterpretation':'protectedMoves counts positive-history count-pruning guard bypasses before all eligibility checks, not proven omissions prevented; it does not count futility protection. Other event counters include repeated work.',
    'conditionalAudits':len(audits),'completedAudits':sum(x['reference']['completed'] for x in audits),
    'falseThresholds':sum(x['falseThreshold'] for x in audits),
    'shallowDisagreements':[{'id':x['id'],'reference':x['reference']['score'],'candidate':x['candidate']['score']} for x in shallow if x['reference']['score']!=x['candidate']['score']],
    'witness':[{k:x[k] for k in ['depth','type','name'] if k in x}|{'score':x['outcome']['score'],'completed':x['outcome']['completed']} for x in witness if 'outcome' in x],
    'ordinaryProofStores':sum(x['tt']['proofStores'] for x in mechanisms),
    'limitations':'Conditional event samples are not random or a calibrated population error rate. Different selective horizons are diagnosed, not forced to equal the unpruned reference. No strength inference.'})
for name,run in cost.items():
    print(name,[(profile,round(x['elapsedMillis'],2),round(x['processCpuMillis'],2),x['nodes'],x['compilationMillis'],x['gcCollections'],round(x['liveThreadAllocatedMiB'],3)) for profile,x in run['profiles'].items()])
