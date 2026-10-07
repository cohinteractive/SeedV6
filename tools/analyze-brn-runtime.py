"""Verify a frozen BRN runtime plan and collect comparable costs without test labels."""
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


def analyze(plan):
    require(plan['schema'] == 'brn-architecture-runtime-plan-v1', 'Runtime plan schema')
    for path, digest in plan.get('frozenFiles', {}).items():
        require(sha(Path(path)) == digest, 'Frozen runtime input changed: '+path)
    profiles = {p['profile']: p for p in plan['profiles']}
    require(len(profiles) == len(plan['profiles']), 'Duplicate profile')
    rows = {key: dict(profile, measurements={}) for key, profile in profiles.items()}
    manifests, requests, counts = {}, {}, {}
    total_seconds = 0
    for spec in plan['runs']:
        key, mode = spec['profile'], spec['mode']
        require(key in rows and mode in ['measure', 'transitions'], 'Unknown profile/mode')
        row = rows[key]
        require(mode not in row['measurements'], 'Duplicate measurement')
        run = Path(spec['run'])
        receipt, result = Path(str(run) + '.execution.json'), run / 'result.json'
        execution, value = read(receipt), read(result)
        require(execution['status'] == 'completed' and execution['exitCode'] == 0,
                'Incomplete execution: ' + str(run))
        for field in ['sourceSha256', 'compiledSha256', 'runnerSha256']:
            require(field not in manifests or manifests[field] == execution[field],
                    'Code changed within runtime campaign: ' + field)
            manifests[field] = execution[field]
        expected_args = (['measure'] if mode == 'measure' else []) + spec['arguments']
        require(value['arguments'] == expected_args, 'Result arguments differ from plan')
        require(value['modelSha256'] == row['modelIdentity'] and value['gain'] == row['gain'],
                'Frozen model/gain mismatch')
        if plan.get('requireMetadataIdentity') or 'modelMetadataIdentity' in row:
            require(isinstance(row.get('modelMetadataIdentity'), str)
                    and value.get('modelMetadataIdentity') == row['modelMetadataIdentity']
                    and value.get('modelIdentityStable') is True, 'Frozen evaluator metadata mismatch or unstable identity')
        total_seconds += execution['elapsedSeconds']
        item = {'run': str(run), 'receiptSha256': sha(receipt), 'resultSha256': sha(result),
                'processSeconds': execution['elapsedSeconds'], 'processMemory': execution['processMemory']}
        if mode == 'measure':
            roots = value['roots']
            require(len(roots) == plan['rootCount'] and
                    all(r['complete'] and r['depth'] == plan['depth'] for r in roots),
                    'Incomplete depth search')
            require([r['index'] for r in roots] == list(range(plan['rootCount'])), 'Root indices')
            require(abs(statistics.median(value['fullRefreshScoreNs']) - value['medianNs']) < 1e-8,
                    'Latency median mismatch')
            seconds, nodes = sum(r['seconds'] for r in roots), sum(r['nodes'] for r in roots)
            times = sorted(r['seconds'] for r in roots)
            item.update(initializeAndScoreMedianNs=value['medianNs'],
                        initializeAndScorePassesNs=value['fullRefreshScoreNs'],
                        method=value['method'], roots=len(roots), searchSeconds=seconds,
                        searchNodes=nodes, searchNps=nodes / seconds,
                        searchMedianSeconds=statistics.median(times),
                        searchP95Seconds=times[math.ceil(.95 * len(times)) - 1],
                        searchMaximumSeconds=times[-1])
        else:
            require(value['datasetManifest']['payloadSha256'] == row['dataPayloadSha256'],
                    'Dataset identity mismatch')
            data = row['data']
            require(data not in requests or requests[data] == value['requestSha256'],
                    'Different transition requests on same dataset')
            requests[data] = value['requestSha256']
            group_counts = {k: g['requestsPerPass'] for k, g in value['groups'].items()}
            require(data not in counts or counts[data] == group_counts, 'Group counts differ')
            counts[data] = group_counts
            verification = value['verification']
            require(verification['maximumAbsoluteIntegerScoreDifference'] <= 1 and
                    verification['requests'] == group_counts['all'], 'Transition verification')
            require(all(group_counts.get(g, 0) > 0 for g in
                        ['all', 'nonking', 'king', 'capture', 'promotion', 'castle', 'enPassant']),
                    'Special transition coverage missing')
            for group in value['groups'].values():
                require(abs(statistics.median(group['nanosecondsPerRequestPasses']) - group['medianNs']) < 1e-8,
                        'Transition median mismatch')
            item.update(requestSha256=value['requestSha256'], verification=verification,
                        groups=value['groups'], method=value['method'])
        row['measurements'][mode] = item
    require(all(set(r['measurements']) == {'measure', 'transitions'} for r in rows.values()),
            'Missing profile measurement')
    for row in rows.values():
        model = Path(row['model']) / 'selected.model'
        row['modelBytes'] = 0 if row['model'] == 'material-fast' else model.stat().st_size
        if row['model'] != 'material-fast':
            require(sha(model) == row['modelIdentity'], 'Retained model changed')
        if plan.get('requireMetadataIdentity') or 'modelMetadataIdentity' in row:
            metadata = row['modelIdentity'] if row['model'] == 'material-fast' else sha(Path(row['model'])/'result.json')
            require(metadata == row['modelMetadataIdentity'], 'Retained evaluator metadata changed')
    return {'schema': 'brn-architecture-runtime-results-v1', 'allChecksPassed': True,
            'processSeconds': total_seconds, 'runs': len(plan['runs']),
            'profiles': list(rows.values()), 'transitionRequestSha256ByData': requests,
            'transitionGroupCountsByData': counts,
            'codeManifestSha256': {k: hashlib.sha256(json.dumps(v, sort_keys=True,
                separators=(',', ':')).encode()).hexdigest() for k, v in manifests.items()},
            'scope': 'Native cache semantics; initialize+score is not a forced BRN rebuild. '
                     'Transition completion also requires parent-score tolerance and unchanged inputs '
                     'in the Java verifier. JVM peak memory is not evaluator-only allocation. '
                     'Residual changes are descriptive, not automatically noise. No strength inference.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('expected_sha256')
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    require(not args.output.exists(), 'Use a new result path')
    require(sha(args.plan) == args.expected_sha256, 'Frozen plan changed')
    result = analyze(read(args.plan))
    result['plan'] = str(args.plan)
    result['planSha256'] = args.expected_sha256
    result['analyzerSha256'] = sha(Path(__file__))
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'allChecksPassed': True, 'runs': result['runs'],
                      'processSeconds': result['processSeconds']}))


if __name__ == '__main__':
    main()
