"""Audit bounded research qsearch without silently discarding aborted roots."""
import argparse
from collections import Counter
import hashlib
import json
import math
from pathlib import Path
import statistics


def digest(path):
    with Path(path).open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def validate_root(row):
    if type(row.get('index')) is not int or row['index'] < 0 or not isinstance(row.get('fen'), str) or not row['fen']:
        raise ValueError('Invalid root identity')
    for key in ['completed', 'nodeGuardReached', 'timeGuardReached', 'otherAbort']:
        if type(row.get(key)) is not bool:
            raise ValueError('Missing/invalid completion or guard flag')
    for key in ['nodes', 'qnodes', 'maximumQply']:
        if type(row.get(key)) is not int or row[key] < 0:
            raise ValueError('Invalid node/qply accounting')
    if row['qnodes'] > row['nodes']:
        raise ValueError('Qnodes exceed total visited nodes')
    seconds = row.get('seconds')
    if type(seconds) not in [int, float] or not math.isfinite(seconds) or seconds < 0:
        raise ValueError('Invalid root time')
    if row['completed']:
        if type(row.get('score')) is not int or not isinstance(row.get('move'), str) or row['otherAbort']:
            raise ValueError('Completed root has invalid score/move/abort state')
        try:
            move = int(row['move'])
        except ValueError as error:
            raise ValueError('Invalid unsigned move') from error
        if not 0 <= move < 2**64:
            raise ValueError('Move outside unsigned64 range')
    else:
        if 'score' in row or 'move' in row:
            raise ValueError('Aborted root must not carry a completed score/move')
        if row['otherAbort'] != (not row['nodeGuardReached'] and not row['timeGuardReached']):
            raise ValueError('Abort cause accounting inconsistent')


def distribution(values):
    ordered = sorted(values)
    return {'minimum': ordered[0], 'median': statistics.median(ordered),
            'p95': ordered[min(len(ordered)-1, math.ceil(.95*len(ordered))-1)], 'maximum': ordered[-1]}


def summarize(rows):
    if not rows:
        raise ValueError('No attempted roots')
    for row in rows:
        validate_root(row)
    if len({row['index'] for row in rows}) != len(rows):
        raise ValueError('Duplicate measured root')
    totals = {key: sum(row[key] for row in rows) for key in ['nodes', 'qnodes', 'seconds']}
    completed = sum(row['completed'] for row in rows)
    return {'attemptedRoots': len(rows), 'completedRoots': completed, 'allRootsCompleted': completed == len(rows),
            'completionFraction': completed/len(rows),
            'guardCounts': {key: sum(row[key] for row in rows) for key in ['nodeGuardReached', 'timeGuardReached', 'otherAbort']},
            'allAttemptTotals': totals,
            'aggregateQnodeFraction': totals['qnodes']/totals['nodes'] if totals['nodes'] else None,
            'allAttemptDistributions': {key: distribution([row[key] for row in rows]) for key in ['nodes', 'qnodes', 'maximumQply', 'seconds']},
            'incompleteRoots': [{key: row[key] for key in ['index', 'fen', 'nodes', 'qnodes', 'maximumQply', 'seconds', 'nodeGuardReached', 'timeGuardReached', 'otherAbort']}
                                for row in rows if not row['completed']]}


def analyze(plan_path, expected):
    plan_path = Path(plan_path)
    if digest(plan_path) != expected:
        raise ValueError('Plan identity mismatch')
    plan = json.loads(plan_path.read_text(encoding='utf-8'))
    for files in [plan['frozenFiles'], *plan['frozenCode'].values()]:
        for path, frozen in files.items():
            if digest(path) != frozen:
                raise ValueError('Frozen input/code changed: '+path)
    profiles = {p['profile']: p for p in plan['profiles']}
    if len(profiles) != len(plan['profiles']) or {s['profile'] for s in plan['runs']} != set(profiles) or len(plan['runs']) != len(profiles):
        raise ValueError('Run/profile allocation mismatch')
    output = {'schema': 'brn-architecture-qsearch-analysis-v1', 'allBindingChecksPassed': True,
              'planSha256': expected, 'analysisSourceSha256': digest(__file__), 'profiles': {},
              'scope': 'Existing bounded research quiescence search, not production-PVS strength. All attempted roots included; process completion is separate from root completion. Guard flags may overlap; caps are not successful searches.'}
    common_roots, common_warmup = {}, {}
    total_process = 0.0
    for spec in plan['runs']:
        profile = profiles[spec['profile']];run = Path(spec['run'])
        result_path = run/'result.json';receipt_path = Path(str(run)+'.execution.json')
        result = json.loads(result_path.read_text(encoding='utf-8'))
        receipt = json.loads(receipt_path.read_text(encoding='utf-8'))
        if (receipt['status'] != 'completed' or receipt['exitCode'] != 0 or receipt['timeoutSeconds'] != plan['timeoutSeconds']
                or receipt['command'][-len(spec['arguments']):] != spec['arguments'] or result['arguments'] != spec['arguments']):
            raise ValueError('Execution identity/status mismatch')
        for kind, files in plan['frozenCode'].items():
            if receipt[kind] != files:
                raise ValueError('Execution code manifest differs')
        if not result['complete'] or result['schema'] != 'brn-architecture-qsearch-v1':
            raise ValueError('Not all requested roots were attempted')
        for report_field, profile_field in [('modelSha256', 'modelIdentity'), ('modelMetadataIdentity', 'modelMetadataIdentity'), ('gain', 'gain')]:
            if result[report_field] != profile[profile_field]:
                raise ValueError('Model/metadata/gain differs')
        manifest = json.loads((Path(profile['data'])/'manifest.json').read_text(encoding='utf-8'))
        if result['datasetManifest'] != manifest or digest(Path(profile['data'])/'positions.bin') != profile['dataPayloadSha256']:
            raise ValueError('Dataset differs')
        for key in ['nodeLimit', 'millis', 'depth']:
            if result[key] != plan[key]:
                raise ValueError('Search protocol differs')
        rows, warm = result['roots'], result['warmup']
        if [r['index'] for r in rows] != list(range(plan['firstIndex'], plan['firstIndex']+plan['rootCount'])):
            raise ValueError('Measured root sample/order differs')
        if [r['index'] for r in warm] != list(range(plan['warmup']['firstIndex'], plan['warmup']['firstIndex']+plan['warmup']['count'])):
            raise ValueError('Warmup sample/order differs')
        summary, warm_summary = summarize(rows), summarize(warm)
        if summary['completedRoots'] != result['completedRoots'] or summary['allRootsCompleted'] != result['allRootsCompleted']:
            raise ValueError('Reported completion totals differ')
        for target, current in [(common_roots, rows), (common_warmup, warm)]:
            identities = {r['index']: r['fen'] for r in current}
            key = profile['dataPayloadSha256']
            if key in target and target[key] != identities:
                raise ValueError('Models did not use common board geometries')
            target[key] = identities
        seconds = receipt['elapsedSeconds']
        if not math.isfinite(seconds) or seconds < 0:
            raise ValueError('Invalid process elapsed time')
        total_process += seconds
        output['profiles'][spec['profile']] = dict(summary, family=profile['family'], seed=profile['seed'],
            modelSha256=profile['modelIdentity'], modelMetadataIdentity=profile['modelMetadataIdentity'], gain=profile['gain'],
            warmup=warm_summary, processSeconds=seconds, processMemory=receipt['processMemory'],
            provenance={'run': run.as_posix(), 'resultSha256': digest(result_path), 'receiptSha256': digest(receipt_path)})
    output['processSeconds'] = total_process
    output['attemptedRoots'] = sum(r['attemptedRoots'] for r in output['profiles'].values())
    output['completedRoots'] = sum(r['completedRoots'] for r in output['profiles'].values())
    output['commonRootDataRanges'] = len(common_roots)
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['plan', 'expected', 'output']:
        parser.add_argument(name)
    args = parser.parse_args()
    result = analyze(args.plan, args.expected)
    with Path(args.output).open('x', encoding='utf-8') as out:
        json.dump(result, out, indent=2);out.write('\n')
    print(json.dumps({k: result[k] for k in ['allBindingChecksPassed', 'attemptedRoots', 'completedRoots', 'processSeconds']}, indent=2))


if __name__ == '__main__':
    main()
