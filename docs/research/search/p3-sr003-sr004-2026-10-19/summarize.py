"""Descriptive development costs; all requests retained, no Elo inference."""
import argparse, collections, hashlib, json, pathlib, statistics
p=argparse.ArgumentParser();p.add_argument('name');a=p.parse_args()
base=pathlib.Path(__file__).resolve().parent;path=base/'raw'/(a.name+'.jsonl')
data=[json.loads(line) for line in path.read_text().splitlines()]
if data[-1].get('status')!='passed': raise SystemExit('Incomplete evidence')
rows=[x for x in data if x.get('type')=='request' and x['round']>=0]
groups=collections.defaultdict(list)
for x in rows:groups[x['policy']].append(x)
summary={'input':str(path.relative_to(base)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
         'identity':data[0],'rows':len(rows),'policies':{}}
for policy,items in groups.items():
    stats={k:sum(x[k] for x in items) for k in ['elapsedNanos','processCpuNanos','allocatedBytes','passes','compilationMillis','gcCollections']}
    stats['requests']=len(items);stats['nodes']=sum(x['outcome']['nodes'] for x in items)
    stats['completed']=sum(x['outcome']['targetDepthCompleted'] for x in items)
    stats['meanCompletedDepth']=statistics.mean(x['outcome'].get('lastCompletedResult',{}).get('depth',0) for x in items)
    stats['maxHeapUsed']=max(x['heapUsed'] for x in items)
    stats['terminations']=dict(collections.Counter(x['termination'] for x in items))
    stats['mainNodes']=sum(x['outcome']['diagnostics']['worker']['nodes']['mainNodes'] for x in items)
    stats['qNodes']=sum(x['outcome']['diagnostics']['worker']['nodes']['qNodes'] for x in items)
    if 'attempts' in items[0]:
        phases=collections.defaultdict(lambda:{'calls':0,'nodes':0,'selective':0})
        for x in items:
            for t in x['attempts']:
                s=phases[t['phase']];s['calls']+=1;s['nodes']+=t['result']['nodes'];s['selective']+=t['result']['selective']
        stats['phases']=dict(phases)
        stats['tt']={k:sum(x['tt'][k] for x in items) for k in items[0]['tt']}
    summary['policies'][policy]=stats
control=summary['policies']['PVS']
for policy,s in summary['policies'].items():
    s['ratiosToPvs']={k:s[k]/control[k] if control[k] else None for k in ['elapsedNanos','processCpuNanos','allocatedBytes','nodes','passes']}
    reference={(x['id'],x['round']):x for x in groups['PVS']}
    pairs=[(x,reference[(x['id'],x['round'])]) for x in groups[policy]]
    s['scoreDifferences']=sum(x['outcome'].get('lastCompletedResult',{}).get('score')!=b['outcome'].get('lastCompletedResult',{}).get('score') for x,b in pairs)
    s['moveDifferences']=sum(x['outcome'].get('lastCompletedResult',{}).get('bestMove')!=b['outcome'].get('lastCompletedResult',{}).get('bestMove') for x,b in pairs)
out=base/(a.name+'-summary.json');out.write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary['policies'],indent=2))
