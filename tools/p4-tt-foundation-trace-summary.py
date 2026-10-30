"""Post-screen descriptive diagnostics; no new games, tuning or strength inference."""
import argparse
import collections
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import statistics


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def summarize(path):
    spec = importlib.util.spec_from_file_location('trace', Path(__file__).with_name('p4-tt-trace.py'))
    trace = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(trace)
    state = {a: {} for a in ('baseline', 'candidate')}
    rows = {a: [] for a in state}
    with path.open(encoding='utf-8') as stream:
        for line in stream:
            match = trace.ENGINE.match(line.rstrip('\r\n'))
            if not match:
                continue
            actor, direction, text = match.groups()
            s = state[actor]
            if direction == '<---':
                if text.startswith('position '):
                    s['position'] = text
                elif text.startswith('go '):
                    assert not s.get('active')
                    words = text.split()
                    options = dict(zip(words[1::2], words[2::2]))
                    position = s['position'].split()
                    assert position[:2] == ['position', 'startpos']
                    moves = position[3:] if len(position) > 2 else []
                    s.update(active=True, last=None, metric=None,
                             availableMs=int(options['wtime' if len(moves) % 2 == 0 else 'btime']))
            elif s.get('active'):
                if text.startswith('info depth '):
                    s['last'] = {k: int(re.search(r'\b'+k+r' (\d+)', text)[1])
                                 for k in ('depth', 'nodes', 'time')}
                elif text.startswith('info string request '):
                    assert s['metric'] is None
                    metric = trace.TELEMETRY.fullmatch(text)
                    assert metric
                    s['metric'] = dict(nodes=int(metric[1]), elapsedNanos=int(metric[2]), termination=metric[3])
                elif text.startswith('bestmove '):
                    assert s['metric'] is not None and s['last'] is not None
                    assert s['last']['nodes'] <= s['metric']['nodes']
                    rows[actor].append(dict(**s['metric'], depth=s['last']['depth'],
                                           completedNodes=s['last']['nodes'], availableMs=s['availableMs']))
                    s['active'] = False
    assert not any(s.get('active') for s in state.values())
    result = {}
    for actor, values in rows.items():
        assert values
        fractions = [v['elapsedNanos']/1e6/max(1, v['availableMs']) for v in values]
        result[actor] = {
            'requests': len(values), 'nodes': sum(v['nodes'] for v in values),
            'elapsedNanos': sum(v['elapsedNanos'] for v in values),
            'terminations': dict(collections.Counter(v['termination'] for v in values)),
            'completedDepthHistogram': dict(sorted(collections.Counter(v['depth'] for v in values).items())),
            'medianCompletedDepth': statistics.median(v['depth'] for v in values),
            'minimumAvailableClockMs': min(v['availableMs'] for v in values),
            'maximumRequestToAvailableClockRatio': max(fractions),
            'requestsExceedingAvailableClock': sum(f > 1 for f in fractions),
            'zeroDepthCompletedRequests': sum(v['depth'] == 0 for v in values),
            'completedNodesToWholeRequestNodes': sum(v['completedNodes'] for v in values)/sum(v['nodes'] for v in values)}
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    root = Path(args.root)
    report = {'status': 'passed', 'scriptSha256': digest(__file__), 'domains': [],
              'scope': 'Post hoc descriptive investigation of completed screen traces. Actors encounter different positions; depth/node/time aggregates are not causal quality or efficiency comparisons. No new strength test, no game extension, no protocol alteration.'}
    for workers in (1, 4):
        path = root/'raw'/('screen-v1-w'+str(workers))/'uci-trace.log'
        observations = summarize(path)
        summary = json.loads((root/('screen-v1-w'+str(workers)+'-summary.json')).read_bytes())
        assert sum(v['requests'] for v in observations.values()) == summary['traceAudit']['completedRequests']
        for actor, values in observations.items():
            expected = summary['results']['wholeRequestResources'][actor]
            assert all(values[k] == expected[k] for k in ('requests', 'nodes', 'elapsedNanos'))
        report['domains'].append({'workers': workers, 'trace': str(path), 'sha256': digest(path), 'actors': observations})
    with Path(args.output).open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
