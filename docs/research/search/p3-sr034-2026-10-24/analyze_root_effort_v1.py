"""Exploratory root-effort discrimination; no quality labels, policy or p-values."""
import json
import statistics as st
from pathlib import Path
from run_trajectory_v1 import HERE, ROOT, digest, write_new


def distribution(values):
    if not values: return {'n': 0}
    v=sorted(values)
    return {'n':len(v),'min':v[0],'q25':v[int(.25*(len(v)-1))],
            'median':st.median(v),'q75':v[int(.75*(len(v)-1))],'max':v[-1]}


def auc(rows, score):
    yes=[score(x) for x in rows if x['nextChanged']]
    no=[score(x) for x in rows if not x['nextChanged']]
    if not yes or not no:return None
    return sum((a>b)+.5*(a==b) for a in yes for b in no)/(len(yes)*len(no))


def main():
    plan=json.loads((HERE/'root-effort-v1.json').read_text())
    binding=json.loads((HERE/'root-effort-v1-binding.json').read_text())
    for rel,sha in binding['files'].items():assert digest(ROOT/rel)==sha,rel
    inputs={};samples=[];coverage=[];roots=0;comparisons=0
    for experiment in plan['experiments']:
        name=experiment['name'];path=HERE/'raw'/(name+'.jsonl')
        receipt=json.loads((HERE/(name+'-execution.json')).read_text());assert receipt['status']=='passed'
        inputs[str(path.relative_to(HERE))]=digest(path)
        rows=[json.loads(line) for line in path.read_text().splitlines()];assert rows[-1]['status']=='passed'
        for row in rows:
            if row.get('type')!='effort-trajectory':continue
            assert row['neutral'];comparisons+=1;roots+=row['round']==0
            seq=[x['iteration']['result'] for x in row['observed']['iterations']]
            entries={x['depth']:x for x in row['rootEffort']}
            assert set(entries)=={x['depth'] for x in seq}
            for i,r in enumerate(seq):
                observation=entries[r['depth']];effort=observation['finalAttemptEffort']
                total=sum(x['nodes'] for x in effort)
                selected=[x for x in effort if x['move']==r['bestMove']]
                available=total>0 and len(selected)==1 and observation['cachedDepth']==r['depth']
                coverage.append({'available':available,'entries':len(effort),'legal':observation['legalRootMoves'],
                    'rootEffortToAllAttemptNodes':None if observation['allAttemptNodes']==0 else total/observation['allAttemptNodes']})
                if not available or i<2 or i+1>=len(seq):continue
                if any(abs(x['score'])>=32512 for x in seq[i-2:i+2]):continue
                shares=[x['nodes']/total for x in effort]
                samples.append({'source':name,'root':row['id'],'depth':r['depth'],'selective':r['selective'],
                    'stable3':len({x['bestMove'] for x in seq[i-2:i+1]})==1,
                    'changedNow':r['bestMove']!=seq[i-1]['bestMove'],
                    'nextChanged':r['bestMove']!=seq[i+1]['bestMove'],
                    'selectedShare':selected[0]['nodes']/total,'largestShare':max(shares),
                    'concentration':sum(x*x for x in shares),
                    'rootEffortToAllAttemptNodes':total/max(1,observation['allAttemptNodes']),
                    'fullLegalRootCoverage':len(effort)==observation['legalRootMoves']})
    tables=[]
    for low,high in ((3,5),(6,8),(9,12)):
        for condition,predicate in (('all',lambda x:True),('stable3',lambda x:x['stable3']),
                ('changedNow',lambda x:x['changedNow'])):
            group=[x for x in samples if low<=x['depth']<=high and predicate(x)]
            tables.append({'depthRange':[low,high],'condition':condition,'n':len(group),
                'distinctRoots':len({(x['source'],x['root']) for x in group}),
                'nextChanged':sum(x['nextChanged'] for x in group),
                'selectedShare':distribution([x['selectedShare'] for x in group]),
                'selectedShareBeforeChanges':distribution([x['selectedShare'] for x in group if x['nextChanged']]),
                'selectedShareBeforeUnchanged':distribution([x['selectedShare'] for x in group if not x['nextChanged']]),
                'lowSelectedShareAuc':auc(group,lambda x:-x['selectedShare']),
                'lowConcentrationAuc':auc(group,lambda x:-x['concentration']),
                'moveChangedNowAuc':auc(group,lambda x:x['changedNow'])})
    result={'status':'passed','roots':roots,'neutralComparisons':comparisons,'inputs':inputs,
        'analysisScriptSha256':digest(Path(__file__)),'observedBoundaries':len(coverage),
        'availableBoundaries':sum(x['available'] for x in coverage),
        'fullLegalRootCoverage':sum(x['entries']==x['legal'] for x in coverage),
        'rootEffortToAllAttemptNodes':distribution([x['rootEffortToAllAttemptNodes'] for x in coverage if x['rootEffortToAllAttemptNodes'] is not None]),
        'ordinarySamples':len(samples),'selectiveSamples':sum(x['selective'] for x in samples),'tables':tables,
        'highSelectedShareThenChangedWitnesses':sorted((x for x in samples if x['nextChanged'] and x['depth']>=6),
            key=lambda x:x['selectedShare'],reverse=True)[:8],
        'limitations':['Development roots/adjacent depths are dependent and node-censored; descriptive AUC has no independent uncertainty claim.',
            'A later different selective move is not a demonstrated better move.',
            'Final-attempt inclusive root effort excludes earlier aspiration attempts; its ratio to all-attempt nodes is not an exact retry fraction.',
            'Ordering and selective pruning affect effort/coverage; root fail-soft scores are not exact alternative scores.',
            'No time-allocation policy or confidence threshold is selected, no whole-game or multiworker strength is established.']}
    write_new(HERE/'root-effort-v1-summary.json',result)
    print(json.dumps({k:result[k] for k in ('status','roots','neutralComparisons','observedBoundaries','availableBoundaries',
        'fullLegalRootCoverage','rootEffortToAllAttemptNodes','tables')},indent=2))


if __name__=='__main__':main()
