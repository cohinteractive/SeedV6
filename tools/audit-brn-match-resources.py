"""Audit frozen match execution and actor costs without computing playing scores."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import statistics


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def actor_moves(plies, starting_color, candidate_color):
    require(type(plies) is int and plies >= 0, 'Invalid plies')
    require(starting_color in ['w', 'b'] and candidate_color in ['w', 'b'],
            'Invalid side to move / candidate color')
    first = (plies + 1) // 2
    candidate = first if starting_color == candidate_color else plies // 2
    return {'candidate': candidate, 'opponent': plies - candidate}


def empty_actor():
    return {'nodesAllRecordedGames': 0, 'secondsAllRecordedGames': 0,
            'movesInGamesWithUnambiguousMoveAccounting': 0,
            'secondsInGamesWithUnambiguousMoveAccounting': 0,
            'gameMeanMilliseconds': []}


def analyze(plan):
    require(plan['schema'] == 'brn-architecture-match-plan-v1', 'Plan schema')
    declared = {entry['run']: (name, seed)
                for name, seeds in plan['comparisons'].items()
                for seed, entries in seeds.items() for entry in entries}
    require(len(declared) == sum(len(entries) for seeds in plan['comparisons'].values()
                                for entries in seeds.values()), 'Duplicate declared run')
    runs = {spec['run']: spec for spec in plan['runs']}
    require(len(runs) == len(plan['runs']) and set(runs) == set(declared),
            'Run manifest and comparison grid differ')
    require(2 * sum(int(spec['arguments'][3]) for spec in runs.values()) == plan['plannedGames'],
            'Planned game count differs from batch manifest')
    manifests, groups, provenance = {}, {}, []
    for name, spec in runs.items():
        comparison, seed = declared[name]
        require(spec['comparison'] == comparison and str(spec['seed']) == seed,
                'Run comparison / seed differs')
        run = Path(name)
        receipt_path, config_path = Path(name + '.execution.json'), run / 'config.json'
        execution, config = read(receipt_path), read(config_path)
        require(execution['status'] != 'running', 'Run still active: ' + name)
        require(execution['status'] == 'completed' and execution['exitCode'] == 0,
                'Noncompleted execution needs explicit continuation accounting: ' + name)
        expected_args = [spec['mode']] + spec['arguments']
        require(spec['mode'] == 'match' and config['arguments'] == expected_args
                and execution['command'][-len(expected_args):] == expected_args,
                'Executed arguments differ from frozen plan: ' + name)
        command_prefix = execution['command'][:-len(expected_args)]
        require('commandPrefix' not in manifests or manifests['commandPrefix'] == command_prefix,
                'JVM command prefix changed')
        manifests['commandPrefix'] = command_prefix
        require(execution['timeoutSeconds'] == plan['timeoutSeconds'], 'Timeout differs')
        require(int(expected_args[5]) == plan['depth']
                and int(expected_args[6]) == plan['millis'], 'Search control differs')
        for field in ['sourceSha256', 'compiledSha256', 'runnerSha256', 'java', 'platform']:
            require(field not in manifests or manifests[field] == execution[field],
                    'Code/runtime manifest changed: ' + field)
            manifests[field] = execution[field]
        key = comparison + '/' + seed
        group = groups.setdefault(key, {'comparison': comparison, 'seed': seed,
            'recordedGames': 0, 'ambiguousGames': [], 'missingPairIndices': [],
            'processSeconds': 0, 'actors': {a: empty_actor() for a in ['candidate', 'opponent']}})
        elapsed = execution['elapsedSeconds']
        require(math.isfinite(elapsed) and elapsed >= 0, 'Invalid process duration')
        group['processSeconds'] += elapsed
        expected_indices = set(range(int(expected_args[7]),
                                    int(expected_args[7]) + int(expected_args[4])))
        indices, pair_hashes, batch_search_seconds = set(), {}, 0
        for path in sorted(run.glob('pair-*.json')):
            pair = read(path)
            require(pair['index'] in expected_indices and pair['index'] not in indices,
                    'Unexpected / duplicate pair: ' + str(path))
            indices.add(pair['index'])
            pair_hashes[path.name] = sha(path)
            start_color = pair['fen'].split()[1]
            for color in ['white', 'black']:
                game = pair[color]
                group['recordedGames'] += 1
                counts = actor_moves(game['plies'], start_color, color[0])
                unambiguous = not game.get('failure') and len(game['moves']) == game['plies']
                if not unambiguous:
                    group['ambiguousGames'].append({'pair': str(path), 'candidateColor': color,
                        'termination': game['termination'], 'failure': game.get('failure'),
                        'plies': game['plies'], 'recordedMoves': len(game['moves'])})
                for actor, values in group['actors'].items():
                    seconds, nodes = game[actor + 'Seconds'], game[actor + 'Nodes']
                    require(math.isfinite(seconds) and seconds >= 0 and
                            type(nodes) is int and nodes >= 0, 'Invalid actor resources')
                    values['secondsAllRecordedGames'] += seconds
                    values['nodesAllRecordedGames'] += nodes
                    batch_search_seconds += seconds
                    if unambiguous:
                        moves = counts[actor]
                        require(moves > 0 or (seconds == 0 and nodes == 0),
                                'Resources without a played move require failure accounting')
                        values['movesInGamesWithUnambiguousMoveAccounting'] += moves
                        values['secondsInGamesWithUnambiguousMoveAccounting'] += seconds
                        if moves:
                            values['gameMeanMilliseconds'].append(1000 * seconds / moves)
        require(batch_search_seconds <= elapsed + .05,
                'Actor search time exceeds whole process time')
        group['missingPairIndices'].extend(sorted(expected_indices - indices))
        provenance.append({'run': name, 'receiptSha256': sha(receipt_path),
            'configSha256': sha(config_path), 'pairSha256': pair_hashes,
            'processSeconds': elapsed, 'actorSearchSeconds': batch_search_seconds,
            'otherProcessSeconds': elapsed - batch_search_seconds,
            'processMemory': execution['processMemory']})
    for group in groups.values():
        for values in group['actors'].values():
            seconds, nodes = values['secondsAllRecordedGames'], values['nodesAllRecordedGames']
            values['aggregateNps'] = nodes / seconds if seconds else None
            moves = values['movesInGamesWithUnambiguousMoveAccounting']
            values['millisecondsPerPlayedMove'] = (
                1000 * values['secondsInGamesWithUnambiguousMoveAccounting'] / moves if moves else None)
            means = sorted(values.pop('gameMeanMilliseconds'))
            values['gamesWithPositiveMoveCount'] = len(means)
            values['distributionOfGameMeansMilliseconds'] = ({
                'minimum': means[0], 'median': statistics.median(means),
                'p95': means[math.ceil(.95 * len(means)) - 1], 'maximum': means[-1]}
                if means else None)
    return {'schema': 'brn-architecture-match-resources-v1', 'allBindingChecksPassed': True,
        'plannedGames': plan['plannedGames'],
        'recordedGames': sum(group['recordedGames'] for group in groups.values()),
        'allPairFilesPresent': all(not group['missingPairIndices'] for group in groups.values()),
        'groups': groups, 'provenance': provenance,
        'codeAndRuntimeManifestSha256': {k: hashlib.sha256(json.dumps(v, sort_keys=True,
            separators=(',', ':')).encode()).hexdigest() for k, v in manifests.items()},
        'scope': 'No playing scores computed. Search timing includes the measured search call, '
                 'excludes explicit warmup and other process overhead, and permits early search '
                 'completion. Game-mean percentiles are not per-move percentiles. Failed or '
                 'inconsistent games retain all recorded resources but are excluded from per-move '
                 'means because an unplayed search attempt can contribute time. Missing pairs '
                 'remain explicit. WDL, model/gain/opening and adverse-bound audit is separate.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('expected_sha256')
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    require(not args.output.exists(), 'Use a new immutable output')
    require(sha(args.plan) == args.expected_sha256, 'Frozen plan hash mismatch')
    result = analyze(read(args.plan))
    result.update(planSha256=args.expected_sha256, analysisSourceSha256=sha(Path(__file__)))
    with args.output.open('x', encoding='utf-8') as target:
        json.dump(result, target, indent=2)
        target.write('\n')
    print('Recorded games:', result['recordedGames'], 'all pairs:', result['allPairFilesPresent'])


if __name__ == '__main__':
    main()
