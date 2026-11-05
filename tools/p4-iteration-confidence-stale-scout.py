"""Read completed C4 observations for alpha advances before full root retries.

A failed original-window scout is a lower bound, never an exact alternative score.
This replay measures existing full retries; it cannot predict a fresh scout's result
or savings, since another search would change TT/order/scheduling trajectories.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-iteration-confidence'
RUNS = ('root-baseline-v1', 'root-baseline-v2', 'root-c2-control-v1', 'root-c2-control-v2')


def analyze(name):
    path = OUT / 'raw' / name / 'observations.jsonl'
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    receipt = json.loads((OUT / (name + '-execution.json')).read_text())
    assert receipt['status'] == 'passed' and receipt['inputsStable']
    assert receipt['drain']['ownedProcessesRemaining'] == 0
    assert receipt['files'][str(path.relative_to(ROOT))] == digest
    counts = Counter(); nodes = Counter(); requests = total = maximum_advance = 0
    for row in map(json.loads, path.read_text().splitlines()):
        if row['type'] != 'request':
            continue
        requests += 1
        total += row['measurement']['outcome']['nodes']
        for attempt in row['attempts']:
            first = {j['job']: j for j in attempt['rootJobs'] if j['invocation'] == 0}
            for retry in (j for j in attempt['rootJobs'] if j['invocation'] == 1):
                initial = first[retry['job']]
                assert initial['completed'] and initial['scout'] and not retry['scout']
                assert initial['score'] > initial['alpha']
                assert initial['move'] == retry['move'] and initial['worker'] == retry['worker']
                advance = retry['alpha'] - initial['alpha']
                assert advance >= 0
                maximum_advance = max(maximum_advance, advance)
                counts['allFullRetries'] += 1; nodes['allFullRetries'] += retry['nodes']
                if advance == 0:
                    continue
                counts['alphaAdvancedRetries'] += 1
                nodes['alphaAdvancedRetries'] += retry['nodes']
                category = ('advancedIncomplete' if not retry['completed'] else
                            'advancedCompletedAtOrBelowAlpha' if retry['score'] <= retry['alpha'] else
                            'advancedCompletedAboveAlpha')
                counts[category] += 1; nodes[category] += retry['nodes']
    assert total == receipt['observedNodes']
    return dict(run=name, inputSha256=digest, requests=requests, totalNodes=total,
                counts=dict(counts), nodes=dict(nodes), maximumAlphaAdvance=maximum_advance,
                requestNodeFractions={key: value / total for key, value in nodes.items()})


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    result = dict(status='Completed C4-only descriptive replay; no new Search or current match scores',
                  analyzerSha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  runs=[analyze(name) for name in RUNS],
                  interpretation='Counts include all observed passes and incomplete retry costs. A completed full '
                                 'retry at or below its newer alpha is an observed selective bound, not proof that '
                                 'a fresh scout would reject it cheaply. Old scout scores cannot skip retries.',
                  limitations='Repeated selected roots, correlated SMP, changing shared TT/order and no alternative '
                              'trajectory. Cost fractions are exposure, not recoverable work or strength evidence.')
    with Path(args.output).open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(result, stream, indent=2); stream.write('\n')
    print(json.dumps(result))
