"""Audit and aggregate a prospectively fixed match plan, including documented continuations."""
import argparse
from collections import Counter
import hashlib
import json
import math
from pathlib import Path
import random


def digest(path):
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def score(pair):
    values = [pair[color].get('score') for color in ['white', 'black']]
    if any(value is not None and value not in [0, .5, 1] for value in values):
        raise ValueError('Invalid game score')
    if any(value is None for value in values):
        return None
    return sum(values) / 2


def summarize(rows, expected, replicates=20000, seed=90167):
    seeds = sorted(rows)
    planned = len(seeds) * len(expected)
    complete = [score(row) for by_index in rows.values() for row in by_index.values() if score(row) is not None]
    total = sum(complete)
    clusters = [index for index in expected if all(index in rows[s] and score(rows[s][index]) is not None for s in seeds)]
    result = {'plannedPairs': planned, 'recordedPairs': sum(map(len, rows.values())),
              'completePairs': len(complete), 'missingOrIncompletePairs': planned-len(complete),
              'allPlannedPairsScoreBounds': [total/planned, (total+planned-len(complete))/planned],
              'completeOpeningClusters': len(clusters), 'initializations': seeds,
              'complete': len(complete) == planned}
    games = [pair[color] for by_index in rows.values() for pair in by_index.values() for color in ['white', 'black']]
    result['terminations'] = dict(Counter(game['termination'] for game in games))
    scored = [pair[color]['score'] for by_index in rows.values() for pair in by_index.values()
              if score(pair) is not None for color in ['white', 'black']]
    result['winsDrawsLossesCompletePairs'] = [scored.count(1), scored.count(.5), scored.count(0)]
    result['maximumObservedPlies'] = max((game['plies'] for game in games), default=0)
    for actor in ['candidate', 'opponent']:
        nodes = sum(game[actor+'Nodes'] for game in games)
        seconds = sum(game[actor+'Seconds'] for game in games)
        result[actor+'Resources'] = {'nodes': nodes, 'seconds': seconds, 'nps': nodes/seconds if seconds else None}
    if complete:
        result['completedPairPointScore'] = total/len(complete)
    if clusters:
        matrix = [[score(rows[s][index]) for index in clusters] for s in seeds]
        means = [sum(column)/len(seeds) for column in zip(*matrix)]
        result['completeClusterPointScore'] = sum(means)/len(means)
        rng = random.Random(seed)
        opening_boot, two_way_boot = [], []
        for _ in range(replicates):
            indices = [rng.randrange(len(clusters)) for _ in clusters]
            opening_boot.append(sum(means[i] for i in indices)/len(indices))
            sampled_seeds = [rng.randrange(len(seeds)) for _ in seeds]
            two_way_boot.append(sum(matrix[s][i] for s in sampled_seeds for i in indices)/(len(indices)*len(sampled_seeds)))
        def interval(values):
            values.sort()
            return [values[int(.025*len(values))], values[min(len(values)-1, int(.975*len(values)))]]
        result['openingClusterBootstrap95'] = interval(opening_boot)
        result['initializationAndOpeningBootstrap95'] = interval(two_way_boot)
        result['hoeffdingOneSided95LowerConditionalOnModels'] = max(0, sum(means)/len(means)-math.sqrt(math.log(20)/(2*len(means))))
    result['uncertaintyScope'] = ('Approximate bootstrap; common openings are clustered across initializations. '
                                  'Two-way resampling also resamples the few observed initializations and is unstable with only two. '
                                  'Missing pairs are adverse in bounds, never draws; incomplete-cluster intervals cannot establish acceptance.')
    return result


def analyze(plan):
    for path, frozen_digest in plan.get('frozenFiles', {}).items():
        if digest(Path(path)) != frozen_digest:
            raise ValueError('Frozen match input changed: '+path)
    expected = list(range(plan['firstOpeningIndex'], plan['firstOpeningIndex']+plan['pairsPerInitialization']))
    output = {'schema': 'brn-architecture-match-analysis-v1', 'comparisons': {}}
    frozen_openings = None
    if 'openingAudit' in plan:
        audit_path = Path(plan['openingAudit']['result'])
        if digest(audit_path) != plan['openingAudit']['sha256']:
            raise ValueError('Frozen opening audit changed')
        audit = json.loads(audit_path.read_text(encoding='utf-8'))
        if (not audit.get('fresh') or audit['seed'] != plan['openingSeed']
                or audit['firstIndex'] != plan['firstOpeningIndex'] or audit['count'] != plan['pairsPerInitialization']):
            raise ValueError('Frozen opening audit protocol mismatch')
        frozen_openings = audit['openings']
        if set(frozen_openings) != set(map(str, expected)):
            raise ValueError('Frozen opening audit indices mismatch')
        output['openingAudit'] = plan['openingAudit']
    all_openings = {}
    for name, initializations in plan['comparisons'].items():
        rows, provenance, model_hashes, gains_by_initialization, metadata_by_initialization = {}, [], {}, {}, {}
        for initialization, runs in initializations.items():
            rows[initialization] = {}
            for entry in runs:
                run = Path(entry['run'])
                receipt = run.with_name(run.name+'.execution.json')
                execution = json.loads(receipt.read_text(encoding='utf-8'))
                if execution['status'] != 'completed' and not entry.get('allowAdministrativePartial'):
                    raise ValueError(f'Noncompleted run not authorized as a partial: {run}')
                if execution['status'] == 'running':
                    raise ValueError(f'Run still active: {run}')
                config_path = run/'config.json'
                config = json.loads(config_path.read_text(encoding='utf-8'))
                args = config['arguments']
                if [int(args[5]), int(args[6]), int(args[8])] != [plan['depth'], plan['millis'], plan['openingSeed']]:
                    raise ValueError(f'Search/opening protocol mismatch: {run}')
                gains = [config.get('candidateGain', 'native-default'), config.get('opponentGain', 'native-default')]
                declared_gains = [float(args[9]), float(args[10])] if len(args) == 11 else ['native-default'] * 2
                if gains != declared_gains:
                    raise ValueError(f'Gain arguments/config mismatch: {run}')
                frozen_gains = plan.get('gains', {}).get(name, {}).get(initialization, ['native-default'] * 2)
                if gains != frozen_gains:
                    raise ValueError(f'Gain differs from frozen plan: {run}')
                if initialization in gains_by_initialization and gains != gains_by_initialization[initialization]:
                    raise ValueError(f'Gain changed within continuation: {run}')
                gains_by_initialization[initialization] = gains
                hashes = [config['candidateModelSha256'], config['opponentModelSha256']]
                frozen = plan.get('modelHashes', {}).get(name, {}).get(initialization)
                if frozen is not None and hashes != frozen:
                    raise ValueError(f'Model differs from frozen plan: {run}')
                if initialization in model_hashes and hashes != model_hashes[initialization]:
                    raise ValueError(f'Model changed within continuation: {run}')
                model_hashes[initialization] = hashes
                metadata = [config.get('candidateMetadataIdentity'), config.get('opponentMetadataIdentity')]
                frozen_metadata = plan.get('modelMetadataIdentities', {}).get(name, {}).get(initialization)
                if plan.get('requireMetadataIdentity') or frozen_metadata is not None:
                    if not isinstance(frozen_metadata, list) or len(frozen_metadata) != 2 or any(not isinstance(x,str) for x in frozen_metadata) or metadata != frozen_metadata:
                        raise ValueError(f'Evaluator metadata differs from frozen plan: {run}')
                    final_result = json.loads((run/'result.json').read_text(encoding='utf-8'))
                    if final_result.get('modelIdentitiesStable') is not True:
                        raise ValueError(f'Evaluator identity not verified stable: {run}')
                    if initialization in metadata_by_initialization and metadata_by_initialization[initialization] != metadata:
                        raise ValueError(f'Evaluator metadata changed within continuation: {run}')
                    metadata_by_initialization[initialization] = metadata
                legacy = entry.get('legacyCapErratum', False)
                if not legacy and (config.get('maximumPlies') != plan['maximumPlies']
                                   or config.get('protocolVersion') != 'architecture-match-v2-explicit-cap'):
                    raise ValueError(f'Unverified ply cap: {run}')
                pair_digests = {}
                for path in sorted(run.glob('pair-*.json')):
                    pair = json.loads(path.read_text(encoding='utf-8'))
                    index = pair['index']
                    if index not in expected or not int(args[7]) <= index < int(args[7])+int(args[4]):
                        raise ValueError(f'Unexpected opening index: {path}')
                    if index in rows[initialization]:
                        raise ValueError(f'Duplicate continuation pair: {path}')
                    if frozen_openings is not None:
                        declared = frozen_openings[str(index)]
                        if pair['identity'] != declared['identity'] or pair.get('fen') != declared['fen']:
                            raise ValueError(f'Opening differs from frozen geometry audit: {path}')
                    if index in all_openings and all_openings[index] != pair['identity']:
                        raise ValueError(f'Common opening identity mismatch: {path}')
                    all_openings[index] = pair['identity']
                    if legacy and any(pair[color]['plies'] >= plan['maximumPlies'] for color in ['white', 'black']):
                        raise ValueError(f'Legacy cap could have affected a recorded outcome: {path}')
                    score(pair)  # Validate outcome domain; absent values remain unscored.
                    rows[initialization][index] = pair
                    pair_digests[path.name] = digest(path)
                provenance.append({'run': str(run), 'status': execution['status'], 'receiptSha256': digest(receipt),
                                   'configSha256': digest(config_path), 'pairSha256': pair_digests, 'legacyCapErratum': legacy})
        result = summarize(rows, expected, plan.get('bootstrapReplicates', 20000), plan.get('bootstrapSeed', 90167))
        result['perInitialization'] = {s: summarize({s: values}, expected, plan.get('bootstrapReplicates', 20000))
                                       for s, values in rows.items()}
        result['modelHashesByInitialization'] = model_hashes
        result['gainsByInitialization'] = gains_by_initialization
        result['metadataIdentitiesByInitialization'] = metadata_by_initialization
        result['provenance'] = provenance
        output['comparisons'][name] = result
    if len(set(all_openings.values())) != len(all_openings):
        raise ValueError('Distinct planned indices repeat an opening; cluster accounting needs an explicit revision')
    output['openingIdentities'] = all_openings
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    if args.output.exists():
        parser.error('Use a new immutable analysis output')
    plan = json.loads(args.plan.read_text(encoding='utf-8'))
    result = analyze(plan)
    result['planSha256'] = digest(args.plan)
    result['analysisSourceSha256'] = digest(Path(__file__))
    args.output.write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    for name, row in result['comparisons'].items():
        print(name, 'complete', row['complete'], 'WDL', row['winsDrawsLossesCompletePairs'],
              'score', row.get('completedPairPointScore'), 'two-way95', row.get('initializationAndOpeningBootstrap95'))


if __name__ == '__main__':
    main()
