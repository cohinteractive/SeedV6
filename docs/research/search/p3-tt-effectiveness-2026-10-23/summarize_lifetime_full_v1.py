"""Full-game retention with explicit initial-window replay and later strata."""
from collections import Counter
import json
from run_diagnostic_v1 import digest, write_new, HERE

path=HERE/'raw/lifetime-full-v1.jsonl';rows=[json.loads(line) for line in path.read_text().splitlines()]
assert rows[-1]['status']=='passed'
requests=[r for r in rows if r.get('type')=='lifetime']
expected=sum(len(line.split('\t')[2].split())-16 for line in (HERE/'lifetime-lines.tsv').read_text().splitlines())
assert len(requests)==expected and all(r['neutral'] for r in requests)
receipt=json.loads((HERE/'lifetime-full-v1-execution.json').read_text());assert receipt['status']=='passed'
report={'status':'passed','protocolSha256':digest(HERE/'lifetime-full-v1.json'),
        'rawPath':str(path.relative_to(HERE)),'rawSha256':digest(path),'requests':expected,'trajectories':4,
        'wallSeconds':receipt['wallSeconds'],'cpuSeconds':receipt['nativeLifetime']['totalCpuSeconds'],
        'peakWorkingSetBytesSixOwnerDiagnosticJvm':receipt['peakWorkingSetBytes'],'strata':{},
        'limits':['Initial ply16..47 repeats prior data to establish retained state, not independent evidence.',
                  'Four trajectories; no strength inference, no warmup timing claim, no generalization to long clocks.',
                  'Collision writes do not themselves establish useful information lost. Occupancy is observed after requests.']}
for label,selected in (('all',requests),('initial16to47',[r for r in requests if r['ply']<48]),
                       ('later48to79',[r for r in requests if 48<=r['ply']<80]),('late80plus',[r for r in requests if r['ply']>=80])):
    tables=[r['table'] for r in selected];total={k:sum(t[k] for t in tables) for k in (
        'proofProbes','proofHits','proofStores','hintProbes','hintHits','writes','emptyWrites','refreshWrites','sameMoveRefresh','collisionWrites','deeperBlocked')}
    assert total['proofHits']==total['proofStores']==0
    value={'requests':len(selected),'tableTotals':total,
           'hitAge':{str(i):sum(t['hitAge'][i] for t in tables) for i in range(256) if sum(t['hitAge'][i] for t in tables)},
           'maxOccupancyFraction':max(t['occupiedHints']/t['entries'] for t in tables),'profiles':{}}
    for profile in ('retained','cleared'):
        value['profiles'][profile]={'nodes':sum(r[profile]['outcome']['nodes'] for r in selected),
            'capped':sum(not r[profile]['outcome']['targetDepthCompleted'] for r in selected),
            'completedDepths':dict(Counter(r[profile]['outcome']['lastCompletedResult']['depth'] for r in selected)),
            'elapsedNanos':sum(r[profile]['elapsedNanos'] for r in selected),'processCpuNanos':sum(r[profile]['processCpuNanos'] for r in selected)}
    both=[r for r in selected if all(r[p]['outcome']['targetDepthCompleted'] for p in ('retained','cleared'))]
    value['jointlyCompletedCount']=len(both)
    value['retainedToClearedNodesJointlyCompleted']=sum(r['retained']['outcome']['nodes'] for r in both)/sum(r['cleared']['outcome']['nodes'] for r in both) if both else None
    value['differentMoves']=sum(r['retained']['outcome']['lastCompletedResult']['bestMove']!=r['cleared']['outcome']['lastCompletedResult']['bestMove'] for r in selected)
    report['strata'][label]=value
final={}
for r in requests:final[(r['id'].split('-ply')[0],r['side'])]=r
report['lastRequestPerOwner']=[{'id':r['id'],'side':r['side'],'occupancy':r['table']['occupiedHints']/r['table']['entries']} for r in final.values()]
write_new(HERE/'lifetime-full-v1-summary.json',report)
print(json.dumps(report,indent=2))
