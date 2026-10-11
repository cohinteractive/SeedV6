"""Summarize complete SR-023 diagnostic JSONL; never infer strength from proxies."""
import argparse
import collections
import hashlib
import json
from pathlib import Path
import statistics


def summarize(path, mode):
    path = Path(path)
    records = [json.loads(line) for line in path.read_text(encoding='utf-8').splitlines()]
    if records[-1].get('status') != 'passed':
        raise ValueError('Study lacks a successful completion record')
    final, rows = records[-1], records[:-1]
    result = {'input': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
              'completion': final, 'mode': mode}
    if mode == 'diagnostics':
        groups = collections.defaultdict(list)
        for row in rows:
            groups[row['mode'], row['policy']].append(row)
        result['groups'] = {}
        for (budget, policy), group in sorted(groups.items()):
            result['groups'][f'{budget}:{policy}'] = {
                'roots': len(group), 'nodes': sum(r['outcome']['nodes'] for r in group),
                'qnodes': sum(r['outcome']['diagnostics']['worker']['nodes']['qNodes'] for r in group),
                'meanCompletedDepth': statistics.mean(r['outcome']['lastCompletedResult']['depth'] for r in group),
                'terminations': dict(collections.Counter(r['termination'] for r in group)),
                'changedBestMovesVersusWindow': sum(
                    r['outcome']['lastCompletedResult']['bestMove'] != next(
                        x for x in rows if x['mode'] == budget and x['id'] == r['id'] and x['policy'] == 6
                    )['outcome']['lastCompletedResult']['bestMove'] for r in group)}
    elif mode in ('history', 'replay'):
        captures = [r for r in rows if r.get('kind') == 'capture']
        replays = [r for r in rows if r.get('kind') == 'replay']
        if not all(r['reference']['completed'] for r in replays):
            raise ValueError('Incomplete independent replay')
        result['captureIdentities'] = len(captures)
        result['replays'] = len(replays)
        strata = collections.defaultdict(lambda: {'samples': 0, 'aboveAlpha': 0, 'maxExcess': 0})
        for r in replays:
            key = str(r.get('policy', 6)) + ':' + r['stratum']
            g = strata[key]; g['samples'] += 1; g['aboveAlpha'] += r['aboveAlpha']
            g['maxExcess'] = max(g['maxExcess'], r['nominalParentValue'] - r['sample']['alpha'])
        result['strata'] = dict(strata)
        result['adverse'] = sorted([r for r in replays if r['aboveAlpha']],
                                  key=lambda r: r['nominalParentValue']-r['sample']['alpha'], reverse=True)[:20]
        result['interpretation'] = 'Correlated outcome-blind stratified samples, not population probabilities or strength.'
        if mode == 'history':
            histogram = collections.Counter(); population = collections.Counter()
            for c in captures:
                histogram.update({int(k): v for k, v in c['historyHistogram'].items()})
                population.update(c['population'])
            result['opportunitiesChangedSignChangedOmitted'] = [sum(c['opportunities'][i] for c in captures) for i in range(4)]
            result['signs'] = {str(sign): sum(v for k,v in histogram.items() if (k>0)-(k<0) == sign) for sign in (-1,0,1)}
            result['range'] = [min(histogram), max(histogram)]
            result['population'] = dict(population)
    elif mode == 'cost':
        groups = collections.defaultdict(list)
        for r in rows:
            groups[r['budget'], r['policy'], r['id']].append(r)
        sums = collections.defaultdict(collections.Counter)
        for (budget, policy, root), group in groups.items():
            if len(group) != 2 or {r['round'] for r in group} != {0,1}:
                raise ValueError('Missing rotated round')
            for name in ('elapsedNanos', 'threadAllocatedBytes'):
                sums[budget,policy][name] += statistics.median(r[name] for r in group)
            for name in ('nodes',):
                sums[budget,policy][name] += statistics.median(r['outcome'][name] for r in group)
            sums[budget,policy]['completedDepthSum'] += statistics.mean(r['outcome']['lastCompletedResult']['depth'] for r in group)
            sums[budget,policy]['roots'] += 1
        result['groups'] = {f'{b}:{p}':dict(v) for (b,p),v in sorted(sums.items())}
        result['ratiosVsWindow'] = {f'{b}:{p}':{k:v[k]/sums[b,'window'][k] for k in v if k != 'roots'}
                                    for (b,p),v in sorted(sums.items()) if p != 'window'}
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('diagnostics','history','replay','cost'))
    parser.add_argument('input'); parser.add_argument('output')
    args = parser.parse_args()
    output = Path(args.output)
    if output.exists(): raise ValueError('Do not overwrite an assessment')
    result = summarize(args.input,args.mode)
    output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ('adverse','strata','population')},indent=2))
