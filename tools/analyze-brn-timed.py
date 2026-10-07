"""Audit frozen measured-compute curves and their retained checkpoint/cursor evidence."""
import argparse
import hashlib
import json
from pathlib import Path


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def cursor_check(cursor):
    require(cursor['population'] >= 128 and cursor['population'] % 128 == 0,
            'Unsupported cursor population')
    require(0 <= cursor['nextOffset'] < cursor['population'] and cursor['nextOffset'] % 128 == 0,
            'Cursor offset')
    require(cursor['samples'] == (cursor['nextEpoch'] - 1) * cursor['population'] + cursor['nextOffset'],
            'Cursor sample arithmetic')


def analyze(plan, references):
    require(plan['schema'] == 'brn-architecture-timed-plan-v1', 'Timed plan schema')
    refs = {(r['family'], r['seed']): r for r in references['profiles']}
    rows, manifests = [], {}
    for spec in plan['runs']:
        run = Path(spec['run'])
        file, receipt = run / 'result.json', Path(str(run) + '.execution.json')
        result, execution = read(file), read(receipt)
        require(execution['status'] == 'completed' and execution['exitCode'] == 0 and
                result.get('complete') and result.get('exactNextBatchResume'), 'Run incomplete')
        require(result['arguments'] == spec['arguments'] and result['family'] == spec['family'],
                'Frozen recipe mismatch')
        require(result['datasetManifest']['payloadSha256'] == spec['dataPayloadSha256'], 'Dataset mismatch')
        for field in ['sourceSha256', 'compiledSha256', 'runnerSha256']:
            require(field not in manifests or manifests[field] == execution[field], 'Code changed: ' + field)
            manifests[field] = execution[field]
        initial = run / 'gen0'
        reference = Path(refs[spec['family'], spec['seed']]['model']) / 'gen0'
        require(sha(initial / 'selected.model') == sha(reference / 'selected.model'), 'Frozen initializer changed')
        best_path = initial
        best_loss = read(initial / 'result.json')['selectedValidation']['outcomeHalfMse']
        events = result['events']
        require([e['targetSeconds'] for e in events] == plan['budgetsSeconds'], 'Budget grid mismatch')
        curves = []
        previous_prefix_samples = 0
        for index, event in enumerate(events, 1):
            require(event['index'] == index, 'Endpoint index')
            used, target = event['trainingSeconds'], event['targetSeconds']
            require(used >= target and 0 <= event['overshootSeconds'] <= event['maximumBatchSeconds'] + 1e-8
                    and abs(used - target - event['overshootSeconds']) < 1e-8, 'Budget/overshoot bounds')
            for key in ['cursor', 'prefixCursor']:
                cursor_check(event[key])
                require(event[key]['population'] == plan['trainingPopulation'] and
                        event[key]['shuffleSeed'] == spec['seed'], 'Cursor population/seed')
            prefix, deployed = event['prefixCursor']['samples'], event['cursor']['samples']
            require(prefix > previous_prefix_samples and deployed >= prefix and event['updates'] * 128 == deployed,
                    'Update/prefix arithmetic')
            previous_prefix_samples = prefix
            if spec['family'] in ['logic', 'logic-fixed']:
                require(event['gateDiagnostics']['hardened'] and deployed > prefix, 'Missing terminal logic phase')
            else:
                require(event['cursor'] == event['prefixCursor'] and event['prefixSeconds'] == used,
                        'Unexpected non-logic branch')
            endpoint = run / ('endpoint-%03d' % index)
            metadata = read(endpoint / 'result.json')
            require(metadata['complete'] and metadata['exactNextBatchResume'], 'Endpoint resume incomplete')
            require(sha(endpoint / 'selected.model') == event['modelSha256'] and
                    sha(endpoint / 'selected.state') == event['stateSha256'], 'Retained endpoint changed')
            loss = event['validation']['outcomeHalfMse']
            require(event['validation']['positions'] == 4096, 'Selection population count')
            if loss < best_loss:
                best_loss, best_path = loss, endpoint
            require(Path(event['bestWithinBudgetPath']).resolve() == best_path.resolve() and
                    sha(best_path / 'selected.model') == event['bestWithinBudgetModelSha256'],
                    'Declared budget selection differs from raw-loss rule')
            curves.append(dict(event, bestRawHalfMse=best_loss,
                               bestCheckpointSamples=0 if best_path == initial else
                               read(best_path / 'result.json')['endpoint']['cursor']['samples']))
        expected_seconds = events[-1]['prefixSeconds'] + sum(e['trainingSeconds'] - e['prefixSeconds'] for e in events)
        expected_samples = events[-1]['prefixCursor']['samples'] + sum(e['cursor']['samples'] - e['prefixCursor']['samples'] for e in events)
        require(abs(result['executedOptimizationSeconds'] - expected_seconds) < 1e-7 and
                result['executedOptimizationSamples'] == expected_samples, 'Branch cost ledger')
        require(result['unretainedVerificationSamples'] == 256 * len(events) and
                result['unretainedVerificationUpdates'] == 2 * len(events), 'Resume diagnostic ledger')
        require(Path(result['selectedSource']).resolve() == best_path.resolve() and
                sha(run / 'selected.model') == sha(best_path / 'selected.model') == result['selectedModelSha256'] and
                sha(run / 'selected.state') == sha(best_path / 'selected.state') == result['selectedStateSha256'],
                'Final selection copy mismatch')
        rows.append({'family': spec['family'], 'seed': spec['seed'], 'run': str(run),
                     'receiptSha256': sha(receipt), 'resultSha256': sha(file),
                     'dataPayloadSha256': spec['dataPayloadSha256'], 'events': curves,
                     'selectedSource': str(best_path), 'selectedModelSha256': result['selectedModelSha256'],
                     'selectedStateSha256': result['selectedStateSha256'], 'selectedRawHalfMse': best_loss,
                     'sameGen0AsExposureStudy': True, 'exactNextBatchResume': True,
                     'executedOptimizationSeconds': result['executedOptimizationSeconds'],
                     'executedOptimizationSamples': result['executedOptimizationSamples'],
                     'unretainedVerificationSamples': result['unretainedVerificationSamples'],
                     'processSeconds': execution['elapsedSeconds'], 'processMemory': execution['processMemory']})
    return {'schema': 'brn-architecture-timed-results-v1', 'allChecksPassed': True, 'runs': rows,
            'processSeconds': sum(r['processSeconds'] for r in rows),
            'executedOptimizationSeconds': sum(r['executedOptimizationSeconds'] for r in rows),
            'codeManifestSha256': {k: hashlib.sha256(json.dumps(v, sort_keys=True,
                separators=(',', ':')).encode()).hexdigest() for k, v in manifests.items()},
            'scope': plan['timingScope'], 'branchCostScope': plan['logicSchedule']['ledger']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('plan_sha256')
    parser.add_argument('reference_plan', type=Path)
    parser.add_argument('reference_sha256')
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    require(not args.output.exists(), 'Use a new output')
    require(sha(args.plan) == args.plan_sha256 and sha(args.reference_plan) == args.reference_sha256,
            'Frozen plan changed')
    result = analyze(read(args.plan), read(args.reference_plan))
    result.update(planSha256=args.plan_sha256, referencePlanSha256=args.reference_sha256,
                  analyzerSha256=sha(Path(__file__)))
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'allChecksPassed': True, 'runs': len(result['runs']),
                      'processSeconds': result['processSeconds']}))


if __name__ == '__main__':
    main()
