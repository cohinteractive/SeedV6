"""Aggregate SR-007 paired request diagnostics. No strength inference or independent-node claim."""
import argparse
import collections
import json
from pathlib import Path


def study(path):
    rows = [json.loads(line) for line in Path(path).read_text().splitlines()]
    assert rows[-1]['status'] == 'passed'
    assert rows[-1]['observations'] == len(rows) - 1
    rows = rows[:-1]
    control = {(r['id'], r['mode']): r for r in rows if r['policy'] == 0}
    result = {'observations': len(rows), 'interpretation': 'Correlated mechanism observations, not independent samples; callback timings are not clean economics.', 'groups': {}}
    for policy in sorted({r['policy'] for r in rows}):
        groups = {}
        for mode in sorted({r['mode'] for r in rows}):
            group = [r for r in rows if r['policy'] == policy and r['mode'] == mode]
            baselines = [control[(r['id'], mode)] for r in group]
            total = sum(r['outcome']['nodes'] for r in group)
            base_total = sum(r['outcome']['nodes'] for r in baselines)
            counters = collections.Counter()
            histograms = collections.defaultdict(collections.Counter)
            for r in group:
                for invocation in [i['internal'] for i in r['iterations']] + ([r['incompleteInternal']] if 'incompleteInternal' in r else []):
                    for key, value in invocation.items():
                        if isinstance(value, list):
                            for depth, count in enumerate(value): histograms[key][depth] += int(count)
                        else: counters[key] += int(value)
            changed = []
            for r, b in zip(group, baselines, strict=True):
                a, c = r['outcome']['lastCompletedResult'], b['outcome']['lastCompletedResult']
                if policy == 1:
                    assert r['outcome']['nodes'] == b['outcome']['nodes']
                    for key in ('score', 'bestMove', 'depth', 'principalVariation', 'selective'): assert a[key] == c[key]
                if a['bestMove'] != c['bestMove']:
                    changed.append({'id': r['id'], 'control': {k:c[k] for k in ('bestMove','score','depth')}, 'candidate': {k:a[k] for k in ('bestMove','score','depth')}})
            groups[mode] = {'requests':len(group),'nodes':total,'nodeRatio':total/base_total,
                            'changedBestMoves':changed,'counters':dict(counters),
                            'histograms':{k:{str(d):n for d,n in v.items() if n} for k,v in histograms.items()},
                            'terminations':dict(collections.Counter(r['termination'] for r in group))}
        result['groups'][str(policy)] = groups
    return result


def cost(paths):
    result = {'interpretation':'Rotated repeated whole-request observations; no strength or independent-position sampling inference.', 'policies':{}}
    for path in paths:
        rows=[json.loads(line) for line in Path(path).read_text().splitlines()]
        assert rows[-1]['status']=='passed' and rows[-1]['observations']==len(rows)-1
        rows=rows[:-1];baseline={(r['id'],r['round'],r['budget']):r for r in rows if r['policy']=='effort'}
        policy=next(r['policy'] for r in rows if r['policy']!='effort');groups={}
        for budget in sorted({r['budget'] for r in rows}):
            candidates=[r for r in rows if r['policy']==policy and r['budget']==budget]
            controls=[baseline[(r['id'],r['round'],budget)] for r in candidates]
            def metrics(cs,bs):
                totals={k:sum(r[k] for r in cs) for k in ('elapsedNanos','threadAllocatedBytes','processCompilationMillis','processGcCollections')}
                totals['requests']=len(cs)
                totals['elapsedRatio']=totals['elapsedNanos']/sum(r['elapsedNanos'] for r in bs) if cs else None
                totals['nodeRatio']=sum(r['outcome']['nodes'] for r in cs)/sum(r['outcome']['nodes'] for r in bs) if cs else None
                return totals
            entry=metrics(candidates,controls)
            if budget:
                pairs=[(c,b) for c,b in zip(candidates,controls,strict=True) if c['outcome']['nodes']==b['outcome']['nodes']==budget]
                entry['bothExhaustedCap']=metrics([x[0] for x in pairs],[x[1] for x in pairs])
            groups[str(budget)]=entry
        result['policies'][policy]=groups
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode',choices=('study','cost'));parser.add_argument('output');parser.add_argument('inputs',nargs='+')
    args=parser.parse_args();output=Path(args.output)
    if output.exists():raise ValueError('New output required')
    result=study(args.inputs[0]) if args.mode=='study' else cost(args.inputs)
    output.write_text(json.dumps(result,indent=2,allow_nan=False)+'\n',encoding='utf-8')
