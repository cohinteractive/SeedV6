"""Audit explicit clean SR-018 cost inputs, separating equal-work throughput from tree savings."""
import argparse
import hashlib
import json
from pathlib import Path


def analyze(paths):
    result = {'status': 'passed', 'scope': 'diagnostic cost, not strength', 'comparisons': []}
    for path in paths:
        path = Path(path)
        rows = [json.loads(line) for line in path.read_text().splitlines()]
        assert rows[-1] == {'status': 'passed', 'observations': len(rows)-1}
        rows = rows[:-1]
        candidates = {r['policy'] for r in rows} - {'effort'}
        assert len(candidates) == 1
        candidate = candidates.pop()
        item = {'candidate': candidate, 'file': str(path),
                'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'budgets': {}}
        for budget in (0, 100_000, 1_000_000):
            groups = {}
            for r in rows:
                if r['budget'] == budget:
                    key = (r['id'], r['round'])
                    assert r['policy'] not in groups.setdefault(key, {})
                    groups[key][r['policy']] = r
            assert groups and all(set(g) == {'effort', candidate} for g in groups.values())
            for g in groups.values():
                for r in g.values():
                    assert r['termination'] != 'TIME_LIMIT'
                    assert r['outcome']['lastCompletedResult']['completed']
                    if budget: assert r['outcome']['nodes'] <= budget
                    else: assert r['outcome']['targetDepthCompleted']
            summary = {}
            for name in ('effort', candidate):
                selected = [g[name] for g in groups.values()]
                summary[name] = {k: sum(r[k] for r in selected) for k in
                                 ('elapsedNanos', 'threadAllocatedBytes', 'processCompilationMillis', 'processGcCollections')}
                summary[name]['nodes'] = sum(r['outcome']['nodes'] for r in selected)
                summary[name]['meanDepth'] = sum(r['outcome']['lastCompletedResult']['depth'] for r in selected)/len(selected)
            summary['requestPairs'] = len(groups)
            summary['elapsedRatio'] = summary[candidate]['elapsedNanos']/summary['effort']['elapsedNanos']
            summary['nodesRatio'] = summary[candidate]['nodes']/summary['effort']['nodes']
            if budget:
                full = [g for g in groups.values() if all(r['termination'] == 'NODE_LIMIT'
                        and r['outcome']['nodes'] == budget for r in g.values())]
                a = sum(g[candidate]['elapsedNanos'] for g in full)
                b = sum(g['effort']['elapsedNanos'] for g in full)
                summary['equalWork'] = {'requestPairs': len(full), 'elapsedRatio': a/b if b else None,
                                        'npsRatio': b/a if a else None}
            item['budgets'][str(budget)] = summary
        result['comparisons'].append(item)
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', action='append', required=True)
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    output = Path(args.out)
    if output.exists(): raise ValueError('New output required')
    result = analyze(args.input)
    output.write_text(json.dumps(result, indent=2)+'\n')
    for item in result['comparisons']:
        print(item['candidate'], {k: round(v['elapsedRatio'], 4) for k,v in item['budgets'].items()})
