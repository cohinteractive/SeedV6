"""Summarize retained SR-006 request/attempt evidence; never infer strength from cost."""
import collections
import json
from pathlib import Path
import statistics
import sys


def quantiles(values):
    if not values:
        return None
    ordered = sorted(values)
    return {'count': len(values), 'median': statistics.median(values),
            'p90': ordered[int(.9 * (len(ordered)-1))],
            'p95': ordered[int(.95 * (len(ordered)-1))], 'max': max(values)}


def analyze(path):
    raw = [json.loads(line) for line in Path(path).read_text().splitlines()]
    complete = raw[-1].get('status') == 'passed'
    rows = [r for r in raw if 'outcome' in r]
    controls = {(r['id'], r['mode']): r for r in rows if r['policy'] == 'off'}
    summary = {'complete': complete, 'observations': len(rows), 'groups': {}, 'volatility': {}}
    for mode in sorted({r['mode'] for r in rows}):
        by_policy = {}
        for policy in sorted({r['policy'] for r in rows}):
            group = [r for r in rows if r['mode'] == mode and r['policy'] == policy
                     and (r['id'], mode) in controls]
            matched = [(r, controls[r['id'], mode]) for r in group]
            finished = [(r,c) for r,c in matched if r['outcome']['targetDepthCompleted']
                        and c['outcome']['targetDepthCompleted']]
            attempts = [a for r in group for a in r['attempts']]
            narrow = [a for a in attempts if a['alpha'] > -32769 or a['beta'] < 32769]
            failed = [a for a in narrow if a['completed'] and
                      (a['score'] <= a['alpha'] or a['score'] >= a['beta'])]
            # Sum independent invocation costs; no nested subtree double counting.
            retry_nodes = 0
            for r in group:
                for d in {a['depth'] for a in r['attempts']}:
                    seq = [a for a in r['attempts'] if a['depth'] == d]
                    retry_nodes += sum(a['nodes'] for a in seq[1:])
            nodes = sum(r['outcome']['nodes'] for r in group)
            both_nodes = sum(r['outcome']['nodes'] for r,c in finished)
            control_nodes = sum(c['outcome']['nodes'] for r,c in finished)
            finals = [r['outcome'].get('lastCompletedResult') for r in group]
            by_policy[policy] = {
                'requests': len(group), 'finishedTarget': sum(r['outcome']['targetDepthCompleted'] for r in group),
                'nodes': nodes, 'requestSeconds': sum(r['elapsedNanos'] for r in group)/1e9,
                'meanCompletedDepth': statistics.mean(r['depth'] if r else 0 for r in finals) if finals else None,
                'bothFinished': len(finished), 'bothFinishedNodeRatio': both_nodes/control_nodes if control_nodes else None,
                'sameDepthMoveChanges': sum(r['outcome']['lastCompletedResult']['bestMove'] != c['outcome']['lastCompletedResult']['bestMove'] for r,c in finished),
                'sameDepthScoreChanges': sum(r['outcome']['lastCompletedResult']['score'] != c['outcome']['lastCompletedResult']['score'] for r,c in finished),
                'narrowAttempts': len(narrow), 'failedCompletedAttempts': len(failed),
                'failLow': sum(a['score'] <= a['alpha'] for a in failed),
                'failHigh': sum(a['score'] >= a['beta'] for a in failed),
                'failedAttemptNodes': sum(a['nodes'] for a in failed), 'retryNodes': retry_nodes,
                'abortedAttemptNodes': sum(a['nodes'] for a in attempts if not a['completed']),
            }
        summary['groups'][mode] = by_policy
    # One depth-eight control trajectory per root, no duplicate depth-six/node trajectories.
    for policy in sorted({r['policy'] for r in rows}):
        by_depth = collections.defaultdict(list)
        for r in rows:
            if r['policy'] != policy or r['mode'] != 'depth8':
                continue
            for a,b in zip(r['iterations'], r['iterations'][1:]):
                if abs(a['score']) < 32512 and abs(b['score']) < 32512:
                    by_depth[b['depth']].append(abs(b['score'] - a['score']))
        summary['volatility'][policy] = {d: quantiles(v) for d,v in by_depth.items()}
    return summary


if __name__ == '__main__':
    result = analyze(sys.argv[1])
    payload = json.dumps(result, indent=2, allow_nan=False) + '\n'
    if len(sys.argv) > 2:
        Path(sys.argv[2]).write_text(payload, encoding='utf-8', newline='\n')
    else:
        print(payload)
