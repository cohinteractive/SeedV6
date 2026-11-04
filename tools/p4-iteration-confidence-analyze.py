"""Compact IC-R001 fixed-work observations. No strength or causal speed inference."""
import collections
import hashlib
import json
from pathlib import Path
import statistics
import sys


def analyze(path):
    path=Path(path);rows=[json.loads(x) for x in path.read_text().splitlines()]
    assert rows[-1]['type']=='summary' and rows[-1]['status']=='passed'
    requests=[x for x in rows if x['type']=='request']
    classes=collections.Counter();nodes=collections.Counter();depths=[];per_root=[]
    stable=changes_after_stable=changes=eligible=widened=widened_failed=failures=0
    for r in requests:
        resolved=[];memory=16
        for d in sorted({a['depth'] for a in r['attempts']}):
            attempts=[a for a in r['attempts'] if a['depth']==d]
            first=attempts[0];width=(first['beta']-first['alpha'])//2
            if width<1000:
                raised=memory//2>width;eligible+=1;widened+=raised
                failed=first['classification'] in ('lower','upper')
                failures+=failed;widened_failed+=raised and failed
                effective=max(width,memory//2)
            else:effective=16
            for a in attempts:
                classes[a['classification']]+=1;nodes[a['classification']]+=a['result']['nodes']
                if a['classification']=='resolved-selective':resolved.append(a);memory=effective
        for i in range(1,len(resolved)):
            different=resolved[i]['result']['bestMove']!=resolved[i-1]['result']['bestMove'];changes+=different
            was_stable=i>=2 and resolved[i-1]['result']['bestMove']==resolved[i-2]['result']['bestMove']
            stable+=was_stable;changes_after_stable+=was_stable and different
        out=r['measurement']['outcome'];last=out.get('lastCompletedResult')
        depth=last['depth'] if last else 0;depths.append(depth)
        per_root.append({'root':r['root'],'round':r['round'],'depth':depth,'move':last['bestMove'] if last else 0,
                         'score':last['score'] if last else None,'nodes':out['nodes'],
                         'failedPassNodes':sum(a['result']['nodes'] for a in r['attempts'] if a['classification'] in ('upper','lower'))})
    total=sum(nodes.values());assert total==rows[-1]['nodes']
    return {'status':'passed observational summary','input':str(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
            'identity':rows[0],'requests':len(requests),'nodes':total,'attemptClasses':dict(classes),
            'nodesByClass':dict(nodes),'completedBoundPassNodeFraction':(nodes['upper']+nodes['lower'])/total,
            'incompletePassNodeFraction':nodes['incomplete']/total,'meanCompletedDepth':statistics.mean(depths),
            'completedDepthCounts':dict(collections.Counter(depths)),'completedMoveChanges':changes,
            'stableTwoIterationOpportunities':stable,'subsequentChangesAfterStableTwo':changes_after_stable,
            'baselineOnlyCounterfactual':{'eligibleInitialWindows':eligible,'firstFailures':failures,
                                        'carryoverWouldWiden':widened,'widenedObservedFailures':widened_failed,
                                        'meaning':'Read-only replay of widths on observed trajectory; does not predict changed-search outcomes.'},
            'perRequest':per_root,
            'limitations':'Fresh TT, instrumented fixed-node requests, selected mixed-phase roots, SMP variance and repeated roots. Effort is admissions, not useful work. No move-truth, timed-strength, clean-speed or independent confidence conclusion.'}


if __name__=='__main__':
    result=analyze(sys.argv[1])
    with Path(sys.argv[2]).open('x',encoding='utf-8') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps({k:result[k] for k in ('requests','nodes','meanCompletedDepth','completedBoundPassNodeFraction','incompletePassNodeFraction')}))
