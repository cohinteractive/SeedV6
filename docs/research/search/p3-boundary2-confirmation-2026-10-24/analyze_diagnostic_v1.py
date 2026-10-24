"""Analyze only new outcome-free request captures, never historical game data."""
from collections import Counter
from datetime import datetime
import json
from pathlib import Path
import re
import statistics
import sys
from verify_inputs import HERE, digest, write_new


def signature(row):
    result = row['result']
    info = result['finalInfo']
    return (result['bestmove'], result['nodes'], result['termination'],
            tuple(re.search(pattern, info).groups() for pattern in
                  (r'depth (\d+)', r'score (cp|mate) (-?\d+)', r'pv (.*)$')) if info else None)


def safepoints(path):
    values = []
    for line in path.read_text().splitlines():
        if 'Safepoint "' not in line or 'Total: ' not in line:
            continue
        stamp = line.partition(']')[0][1:]
        end = int(datetime.fromisoformat(stamp).timestamp()*1e9)
        duration = int(re.search(r'Total: (\d+) ns', line)[1])
        reaching = int(re.search(r'Reaching safepoint: (\d+) ns', line)[1])
        values.append({'endEpochNanosApprox': end, 'startEpochNanosApprox': end-duration,
                       'durationMillis': duration/1e6, 'reachingMillis': reaching/1e6,
                       'operation': re.search(r'Safepoint "([^"]+)"', line)[1]})
    return values


def analyze(name):
    receipt_path = HERE/(name+'-summary.json')
    receipt = json.loads(receipt_path.read_text())
    assert receipt['status'] in ('passed', 'resource-unqualified')
    for path, sha in receipt['evidence'].items():
        assert digest(HERE/path) == sha, path
    raw = HERE/'raw'/name
    rows = [json.loads(line) for line in (raw/'requests.jsonl').read_text().splitlines()]
    assert len(rows) == 784
    fixed = [r for r in rows if r['clockMillis'] is None]
    timed = [r for r in rows if r['clockMillis'] is not None]
    roots = sorted({r['root'] for r in fixed})
    assert len(roots) == 14 and len(fixed) == 112 and len(timed) == 672
    parity = all(len({signature(r) for r in fixed if r['root'] == root}) == 1 for root in roots)
    assert parity, 'Deterministic control mismatch'
    costs = []
    for profile in ('baseline', 'boundary2'):
        root_ratios = []
        for root in roots:
            selected = [r for r in fixed if r['root'] == root and r['profile'] == profile]
            times = {on: statistics.mean(r['externalElapsedNanos'] for r in selected if r['instrumented'] == on)
                     for on in (False, True)}
            root_ratios.append(times[True]/times[False])
        costs.append({'profile': profile, 'pairedMedianInstrumentedElapsedRatio': statistics.median(root_ratios),
                      'rootRatios': root_ratios, 'descriptiveFivePercentScreenPassed': .95 <= statistics.median(root_ratios) <= 1.05})
    groups = []
    for profile in ('baseline', 'boundary2'):
        for on in (False, True):
            selected = [r for r in timed if r['profile'] == profile and r['instrumented'] == on]
            groups.append({'profile': profile, 'instrumented': on, 'requests': len(selected),
                           'maximumRequestOverrunMillis': max(r['overrunMillis'] for r in selected),
                           'maximumExternalOverrunMillis': max(r['externalOverrunMillis'] for r in selected),
                           'over25MillisCount': sum(r['overrunMillis'] > 25 for r in selected),
                           'terminations': dict(Counter(r['result']['termination'] for r in selected)),
                           'wholeRequestNanos': sum(r['result']['elapsedNanos'] for r in selected),
                           'nativeCpuSeconds': sum(r['nativeCpuSeconds'] for r in selected)})
    events = {}
    for run in sorted({r['run'] for r in rows if r['instrumented']}):
        events[run] = safepoints(raw/(run+'-gc.log'))
    adverse = []
    for row in timed:
        if row['overrunMillis'] <= 5:
            continue
        overlapping = [e for e in events.get(row['run'], [])
                       if e['startEpochNanosApprox'] <= row['endEpochNanos']+2e6
                       and e['endEpochNanosApprox'] >= row['startEpochNanos']-2e6]
        adverse.append({k:v for k,v in row.items() if k != 'result'} |
                       {'requestElapsedNanos':row['result']['elapsedNanos'], 'termination':row['result']['termination'],
                        'overlappingSafepointsApprox':overlapping})
    result = {'status':'analyzed', 'captureStatus':receipt['status'], 'requests':len(rows),
              'deterministicControls':len(fixed), 'clockRequests':len(timed), 'allControlSignaturesEqual':parity,
              'instrumentationCost':costs, 'timingGroups':groups,
              'safepointSummary':{run:{'count':len(es), 'maximumMillis':max((e['durationMillis'] for e in es), default=0),
                                       'operations':dict(Counter(e['operation'] for e in es))} for run,es in events.items()},
              'adverseOver5ms':adverse, 'receiptSha256':digest(receipt_path), 'analyzerSha256':digest(__file__),
              'limits':['No games or strength inference; these requests cannot reconstruct historical JVM state.',
                        'Process CPU includes all JVM threads and has OS counter granularity; no owner-preemption claim.',
                        'Unified-log UTC is millisecond resolution; overlap is approximate with2ms tolerance, not causal proof.',
                        'Instrumentation5% screen is descriptive; neither small sample equivalence nor worst-case bound.']}
    write_new(HERE/(name+'-analysis.json'), result)
    print(json.dumps({k:v for k,v in result.items() if k not in ('adverseOver5ms','limits')}, indent=2))


if __name__ == '__main__':
    analyze(sys.argv[1])
