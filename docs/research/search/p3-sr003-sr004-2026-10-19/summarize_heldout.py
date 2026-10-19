"""Fixed held-out cost protocol: descriptive root-cluster uncertainty, separate worker domains."""
import collections,hashlib,json,math,pathlib,random,statistics
base=pathlib.Path(__file__).resolve().parent;result={}
for workers in [1,4]:
    rows=[];inputs={}
    for fork in [1,2]:
        p=base/'raw'/f'heldout-w{workers}-fork{fork}.jsonl'
        data=[json.loads(s) for s in p.read_text().splitlines()]
        assert data[-1].get('status')=='passed'
        inputs[str(p.relative_to(base))]=hashlib.sha256(p.read_bytes()).hexdigest()
        for r in data:
            if r.get('type')=='request' and r['round']>=0:r['fork']=fork;rows.append(r)
    ref={(r['fork'],r['round'],r['id']):r for r in rows if r['policy']=='PVS'}
    summary={'inputs':inputs,'measuredRequests':len(rows),'arms':{}}
    for policy in ['PVS','TWO_PASS','GUARDED_TWO']:
        selected=[r for r in rows if r['policy']==policy];clusters=collections.defaultdict(list)
        for r in selected:
            b=ref[(r['fork'],r['round'],r['id'])]
            clusters[r['id']].append(math.log(r['elapsedNanos']/b['elapsedNanos']))
        means=[statistics.mean(v) for v in clusters.values()];rng=random.Random(4030419)
        boot=sorted(math.exp(statistics.mean(rng.choices(means,k=len(means)))) for _ in range(10000))
        s={'requests':len(selected),'completed':sum(r['outcome']['targetDepthCompleted'] for r in selected),
           'rootGeometricWallRatio':math.exp(statistics.mean(means)),
           'rootBootstrap95WallRatio':[boot[249],boot[9749]],'forkWallRatios':{},
           'totalNodes':sum(r['outcome']['nodes'] for r in selected),
           'passes':sum(r['passes'] for r in selected),
           'meanCompletedDepth':statistics.mean(r['outcome']['lastCompletedResult']['depth'] for r in selected)}
        for metric in ['elapsedNanos','processCpuNanos','allocatedBytes','compilationMillis','gcCollections']:
            s[metric]=sum(r[metric] for r in selected)
            denominator=sum(b[metric] for b in ref.values())
            if metric in ['elapsedNanos','processCpuNanos','allocatedBytes']:s[metric+'Ratio']=s[metric]/denominator
        s['nodeRatio']=s['totalNodes']/sum(b['outcome']['nodes'] for b in ref.values())
        for fork in [1,2]:s['forkWallRatios'][fork]=sum(r['elapsedNanos'] for r in selected if r['fork']==fork)/sum(r['elapsedNanos'] for r in ref.values() if r['fork']==fork)
        summary['arms'][policy]=s
    result[workers]=summary
result['limitations']='Fixed frozen cost replication, not playing-strength confirmation. Root-cluster percentile intervals do not cover arbitrary JVM/host/platform variability. CPU uses aggregate counters to avoid coarse per-request tick division. All capped/incomplete work retained.'
(base/'heldout-cost-v1-summary.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
