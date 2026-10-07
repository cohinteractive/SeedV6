"""Paired compute-budget score changes on common openings after match auditing."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import random


def digest(path):
    with Path(path).open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def paired_difference(earlier, later, openings, replicates=20000, seed=90312):
    """Rows map initialization -> opening -> pair score or None; never drop missing silently."""
    if not earlier or set(earlier) != set(later) or not openings or len(set(openings)) != len(openings):
        raise ValueError('Mismatched initializations or empty/duplicate opening sample')
    if not isinstance(replicates, int) or replicates < 1:
        raise ValueError('Positive integer replicate count required')
    seeds = sorted(earlier)
    expected = set(openings)
    for rows in [earlier, later]:
        for values in rows.values():
            if not set(values).issubset(expected):
                raise ValueError('Unexpected opening index')
            if any(v is not None and (not isinstance(v, (int, float)) or not math.isfinite(v) or not 0 <= v <= 1) for v in values.values()):
                raise ValueError('Invalid pair score')
    planned = len(seeds) * len(openings)
    lower = upper = 0.0
    differences = []
    per_seed = {}
    for s in seeds:
        current = []
        for i in openings:
            a, b = earlier[s].get(i), later[s].get(i)
            lower += (0 if b is None else b) - (1 if a is None else a)
            upper += (1 if b is None else b) - (0 if a is None else a)
            if a is not None and b is not None:
                current.append(b-a)
        differences.extend(current)
        per_seed[s] = {'completeMatchedPairs': len(current),
                       'completeMatchedPointDifference': sum(current)/len(current) if current else None}
    clusters = [i for i in openings if all(earlier[s].get(i) is not None and later[s].get(i) is not None for s in seeds)]
    result = {'plannedMatchedPairs': planned, 'completeMatchedPairs': len(differences),
              'allPlannedDifferenceBounds': [lower/planned, upper/planned],
              'completeOpeningClusters': len(clusters), 'complete': len(differences) == planned,
              'perInitialization': per_seed,
              'uncertaintyScope': 'Descriptive score change versus the same material control, not direct later-versus-earlier strength or transitive Elo. Approximate unadjusted intervals; only two source/order replications. Missing outcomes retain worst-case bounds; incomplete clusters cannot establish acceptance.'}
    if differences:
        result['completeMatchedPointDifference'] = sum(differences)/len(differences)
    if clusters:
        matrix = [[later[s][i]-earlier[s][i] for i in clusters] for s in seeds]
        means = [sum(column)/len(seeds) for column in zip(*matrix)]
        result['completeClusterPointDifference'] = sum(means)/len(means)
        rng = random.Random(seed)
        opening_samples, two_way_samples = [], []
        for _ in range(replicates):
            indices = [rng.randrange(len(clusters)) for _ in clusters]
            sampled_seeds = [rng.randrange(len(seeds)) for _ in seeds]
            opening_samples.append(sum(means[i] for i in indices)/len(indices))
            two_way_samples.append(sum(matrix[s][i] for s in sampled_seeds for i in indices)/(len(indices)*len(sampled_seeds)))
        for name, samples in [('openingClusterBootstrap95', opening_samples), ('initializationAndOpeningBootstrap95', two_way_samples)]:
            samples.sort()
            result[name] = [samples[int(.025*len(samples))], samples[min(len(samples)-1, int(.975*len(samples)))]]
    return result


def audited_pair_scores(comparison, planned_runs):
    rows = {s: {} for s in comparison['initializations']}
    run_seeds = {}
    for s, entries in planned_runs.items():
        if s not in rows:
            raise ValueError('Unexpected planned initialization')
        for entry in entries:
            key = Path(entry['run']).as_posix()
            if key in run_seeds:
                raise ValueError('Duplicate planned run')
            run_seeds[key] = s
    for entry in comparison['provenance']:
        run = Path(entry['run'])
        config = json.loads((run/'config.json').read_text(encoding='utf-8'))
        if digest(run/'config.json') != entry['configSha256']:
            raise ValueError('Config changed after match audit')
        if run.as_posix() not in run_seeds:
            raise ValueError('Unplanned audited run')
        s = run_seeds[run.as_posix()]
        if ([config['candidateModelSha256'], config['opponentModelSha256']] != comparison['modelHashesByInitialization'][s]
                or [config['candidateMetadataIdentity'], config['opponentMetadataIdentity']] != comparison['metadataIdentitiesByInitialization'][s]):
            raise ValueError('Run identity differs from audited initialization')
        for filename, expected in entry['pairSha256'].items():
            path = run/filename
            if digest(path) != expected:
                raise ValueError('Pair changed after match audit')
            pair = json.loads(path.read_text(encoding='utf-8')); index = pair['index']
            if index in rows[s]:
                raise ValueError('Duplicate pair')
            scores = [pair[color].get('score') for color in ['white', 'black']]
            if any(v is not None and v not in [0, .5, 1] for v in scores):
                raise ValueError('Invalid game score')
            rows[s][index] = None if None in scores else sum(scores)/2
    return rows


def analyze(plan_path, expected_plan, match_path, crosscheck_path):
    if digest(plan_path) != expected_plan:
        raise ValueError('Plan identity mismatch')
    plan = json.loads(Path(plan_path).read_text(encoding='utf-8'))
    matches = json.loads(Path(match_path).read_text(encoding='utf-8'))
    crosscheck = json.loads(Path(crosscheck_path).read_text(encoding='utf-8'))
    if (not crosscheck['allChecksPassed'] or matches['planSha256'] != expected_plan
            or crosscheck['planSha256'] != expected_plan or crosscheck['scoresSha256'] != digest(match_path)):
        raise ValueError('Complete match/cross-check binding required')
    for path, expected in plan['frozenFiles'].items():
        if digest(path) != expected:
            raise ValueError('Frozen input changed')
    openings = list(range(plan['firstOpeningIndex'], plan['firstOpeningIndex']+plan['pairsPerInitialization']))
    families = sorted({s['family'] for s in plan['comparisonSpecifications'].values() if s['axis'] == 'compute'})
    output = {'schema': 'brn-architecture-scaling-differences-v1', 'planSha256': expected_plan,
              'matchResultsSha256': digest(match_path), 'crosscheckSha256': digest(crosscheck_path),
              'analysisSourceSha256': digest(__file__), 'families': {}}
    for family in families:
        names = [f'compute-{family}-{budget}-vs-material' for budget in [120, 600]]
        comparisons = [matches['comparisons'][name] for name in names]
        for s in comparisons[0]['initializations']:
            if (comparisons[0]['modelHashesByInitialization'][s][1] != comparisons[1]['modelHashesByInitialization'][s][1]
                    or comparisons[0]['metadataIdentitiesByInitialization'][s][1] != comparisons[1]['metadataIdentitiesByInitialization'][s][1]
                    or comparisons[0]['gainsByInitialization'][s] != comparisons[1]['gainsByInitialization'][s]):
                raise ValueError('Opponent or gain changed across budgets')
        rows = [audited_pair_scores(comparison, plan['comparisons'][name]) for name, comparison in zip(names, comparisons)]
        output['families'][family] = paired_difference(*rows, openings, plan['bootstrapReplicates'], 90312)
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['plan', 'expected_plan', 'matches', 'crosscheck', 'output']:
        parser.add_argument(name)
    args = parser.parse_args()
    result = analyze(args.plan, args.expected_plan, args.matches, args.crosscheck)
    with Path(args.output).open('x', encoding='utf-8') as out:
        json.dump(result, out, indent=2);out.write('\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
