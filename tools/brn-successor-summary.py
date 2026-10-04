"""Frozen two-seed/opening-cluster accounting for successor confirmation."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import random


def read_seed(paths, expected):
    pairs, inputs, failures = {}, [], []
    signature = None
    for path in paths:
        raw = path.read_bytes()
        report = json.loads(raw)
        args = report['arguments']
        current = (report['candidateSha256'], report['opponentSha256'],
                   tuple(args[4:6]), tuple(args[7:]), report['searchPolicy'])
        if signature is not None and current != signature:
            failures.append(f'Inconsistent recipe/checkpoints: {path}')
        signature = current
        inputs.append({'path': str(path), 'sha256': hashlib.sha256(raw).hexdigest()})
        for pair in report['pairs']:
            identity = pair['identity']
            if identity in pairs:
                failures.append(f'Duplicate opening: {identity}')
            pairs[identity] = pair
            for color in ('white', 'black'):
                game = pair[color]
                if game.get('score') not in (0, .5, 1) or game.get('failure'):
                    failures.append(f'Incomplete game: {path}, {pair["index"]}, {color}')
    if len(pairs) != expected:
        failures.append(f'Expected {expected} unique pairs; found {len(pairs)}')
    return pairs, inputs, failures, signature


def analyze(first, second, expected):
    a, inputs_a, failures_a, recipe_a = read_seed(first, expected)
    b, inputs_b, failures_b, recipe_b = read_seed(second, expected)
    failures = failures_a + failures_b
    if set(a) != set(b):
        failures.append('Seed opening identities differ')
    if recipe_a is None or recipe_b is None or recipe_a[2:] != recipe_b[2:]:
        failures.append('Seed search/opening/calibration protocols differ')
    for identity in set(a) & set(b):
        if (a[identity]['fen'], a[identity]['index']) != (b[identity]['fen'], b[identity]['index']):
            failures.append(f'Opening content differs: {identity}')
    result = {'inputs71': inputs_a, 'inputs97': inputs_b, 'expectedPairsPerSeed': expected,
              'failures': failures, 'complete': not failures}
    if failures:
        return result  # Never silently exclude an incomplete planned game.
    clusters = []
    for identity, pair in sorted(a.items(), key=lambda item: item[1]['index']):
        scores = [(x['white']['score'] + x['black']['score']) / 2 for x in (pair, b[identity])]
        clusters.append({'identity': identity, 'index': pair['index'], 'seed71': scores[0],
                         'seed97': scores[1], 'score': sum(scores) / 2})
    values = [c['score'] for c in clusters]
    score = sum(values) / len(values)
    rng = random.Random(735611)
    samples = sorted(sum(rng.choice(values) for _ in values) / len(values) for _ in range(10000))
    seed_metrics = {}
    for name, rows in [('71', a), ('97', b)]:
        games = [p[color]['score'] for p in rows.values() for color in ('white', 'black')]
        seed_metrics[name] = {'wins': games.count(1), 'draws': games.count(.5), 'losses': games.count(0),
                              'score': sum(games) / len(games)}
    result.update({'clusters': clusters, 'seedMetrics': seed_metrics, 'score': score,
                   'clusterBootstrap95': [samples[249], samples[9749]],
                   'oneSidedBootstrap95Lower': samples[499],
                   'hoeffdingOneSided95Lower': score - math.sqrt(math.log(20) / (2 * len(values))),
                   'bootstrapSeed': 735611, 'bootstrapResamples': 10000,
                   'strengthGate': score >= .55 and samples[499] > .5
                   and all(m['score'] >= .5 for m in seed_metrics.values()),
                   'scope': 'Conditional on these two frozen model seeds and sampled opening distribution; '
                            'shared openings form one cluster across seeds; strength gate alone is not promotion.'})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path)
    parser.add_argument('--seed71', type=Path, nargs='+', required=True)
    parser.add_argument('--seed97', type=Path, nargs='+', required=True)
    parser.add_argument('--expected-pairs', type=int, required=True)
    args = parser.parse_args()
    if args.output.exists() or args.expected_pairs < 1:
        parser.error('Use a new output and positive expected pair count')
    result = analyze(args.seed71, args.seed97, args.expected_pairs)
    result['implementationSha256'] = hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(result, stream, indent=2)
        stream.write('\n')
    print(json.dumps({k: v for k, v in result.items() if k not in ('clusters', 'inputs71', 'inputs97')}, indent=2))
    if not result['complete']:
        raise SystemExit(2)


if __name__ == '__main__':
    main()
