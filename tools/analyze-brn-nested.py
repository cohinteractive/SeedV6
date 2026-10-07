"""Audit a frozen nested-data study without decoding held-out labels."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import struct


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


def sha(path):
    with Path(path).open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def require(value, message):
    if not value:
        raise ValueError(message)


def code_delta(before, after):
    result = {}
    for key in ['sourceSha256','compiledSha256','runnerSha256']:
        a, b = before[key], after[key]
        result[key] = {'added': {p:b[p] for p in sorted(b.keys()-a.keys())},
                       'removed': {p:a[p] for p in sorted(a.keys()-b.keys())},
                       'changed': {p:{'before':a[p],'after':b[p]} for p in sorted(a.keys()&b.keys()) if a[p]!=b[p]}}
    return result


def reconciled_code_delta(before, after, baseline, expected, allowed):
    """Allow only exact extra historical source drafts with no executable classes."""
    require(code_delta(baseline, after) == expected, 'Unaccounted current code difference')
    variation = code_delta(baseline, before)
    for field, delta in variation.items():
        require(not delta['removed'] and not delta['changed'], 'Historical shared code changed')
        if field != 'sourceSha256':
            require(not delta['added'], 'Historical executable inventory changed')
    for path, digest in variation['sourceSha256']['added'].items():
        require(allowed.get(path) == digest, 'Unaccounted historical source draft')
        prefix = path.replace('app/src/verification/java/', 'app/build/classes/java/verification/').removesuffix('.java')
        require(not any(p == prefix+'.class' or p.startswith(prefix+'$') for p in before['compiledSha256']),
                'Historical draft was compiled')
    return code_delta(before, after)


def partitions(root):
    """Opaque 57-byte records; only the partition byte and file header are decoded."""
    root = Path(root)
    payload = (root / 'positions.bin').read_bytes()
    require(len(payload) >= 16 and (len(payload)-16) % 57 == 0, 'Payload length')
    magic, training, validation = struct.unpack('>Qii', payload[:16])
    require(magic == 0x533642524e443031 and training > 0 and validation > 0, 'Payload header')
    rows = [[], [], []]
    for offset in range(16, len(payload), 57):
        record = payload[offset:offset+57]
        require(record[56] in (0, 1, 2), 'Partition byte')
        rows[record[56]].append(record)
    require([len(rows[0]), len(rows[1])] == [training, validation], 'Header counts')
    require(hashlib.sha256(payload).hexdigest() == read(root / 'manifest.json')['payloadSha256'], 'Payload hash')
    return rows


def audit_subset(spec):
    parent, subset = Path(spec['parent']), Path(spec['subset'])
    require(sha(parent/'positions.bin') == spec['parentPayloadSha256'] and
            sha(subset/'positions.bin') == spec['subsetPayloadSha256'], 'Frozen data changed')
    require(sha(subset/'manifest.json') == spec['subsetManifestSha256'], 'Subset manifest changed')
    manifest = read(subset/'manifest.json')
    require(manifest['parentManifestSha256'] == sha(parent/'manifest.json') and
            manifest['parent']['payloadSha256'] == spec['parentPayloadSha256'], 'Parent binding')
    require(Path(manifest['parent']['path']).resolve() == parent.resolve(), 'Parent path')
    require(manifest['selectionSeed'] == spec['subsetSeed'], 'Subset seed')
    large, small = partitions(parent), partitions(subset)
    require([len(x) for x in large] == spec['parentCounts'] and
            [len(x) for x in small] == spec['subsetCounts'] == manifest['counts'], 'Dataset counts')
    require(large[1:] == small[1:], 'Held-out bytes differ')
    hashes = [hashlib.sha256(b''.join(part)).hexdigest() for part in small]
    require(hashes == [manifest[k+'Sha256'] for k in ['training','validation','test']], 'Partition hashes')
    cursor, ordinal_hash = 0, hashlib.sha256()
    for index, record in enumerate(large[0]):
        if cursor < len(small[0]) and record == small[0][cursor]:
            ordinal_hash.update(struct.pack('>i', index))
            cursor += 1
    require(cursor == len(small[0]), 'Subset is not an ordered parent training subsequence')
    require(ordinal_hash.hexdigest() == manifest['selectedTrainingOrdinalsSha256'], 'Selected ordinal hash')
    return {'seed': spec['seed'], 'parent': str(parent), 'subset': str(subset),
            'parentCounts': spec['parentCounts'], 'subsetCounts': spec['subsetCounts'],
            'subsetPayloadSha256': spec['subsetPayloadSha256'], 'partitionSha256': hashes,
            'heldOutBytesIdentical': True, 'selectedOrdinalsVerified': True,
            'scope': 'Opaque byte audit, not a second implementation of seeded sampling or a label metric'}


def audit_training(spec, count, epochs, eligible):
    run = Path(spec['run'])
    report, execution = read(run/'result.json'), read(str(run)+'.execution.json')
    for key,path in [('resultSha256',run/'result.json'),('receiptSha256',Path(str(run)+'.execution.json'))]:
        if key in spec:
            require(sha(path) == spec[key], 'Frozen reference evidence changed')
    require(execution['status'] == 'completed' and execution['exitCode'] == 0 and
            report.get('complete') and report.get('exactNextBatchResume'), 'Incomplete training')
    expected_arguments = (['train'] if spec['mode'] == 'train' else []) + spec['arguments']
    require(report['arguments'] == expected_arguments, 'Frozen training arguments changed')
    require(execution['command'][-len(expected_arguments):] == expected_arguments, 'Executed command mismatch')
    require(report['datasetManifest']['payloadSha256'] == spec['dataPayloadSha256'], 'Training dataset changed')
    events = report['events']
    require([e['epoch'] for e in events] == list(range(epochs+1)) and report['completedEpochs'] == epochs, 'Epochs incomplete')
    require(eligible[0] == 0 and eligible[-1] == epochs and sorted(set(eligible)) == eligible, 'Eligible checkpoint grid')
    curve = []
    for event in events:
        epoch = event['epoch']
        require(event['samples'] == count*epoch and event['updates'] == count*epoch//128, 'Exposure/update arithmetic')
        require(event['validation']['positions'] == 4096, 'Selection population')
        require(math.isfinite(event['validation']['outcomeHalfMse']) and 0 <= event['validation']['outcomeHalfMse'] <= 2 and
                math.isfinite(event['trainingSeconds']) and event['trainingSeconds'] >= 0, 'Invalid metric/time')
        endpoint = run/'gen0' if epoch == 0 else run/'epochs'/('epoch-%03d' % epoch)
        metadata = read(endpoint/'result.json')
        digest = sha(endpoint/'selected.model')
        require(metadata.get('complete') and metadata['kind'] == report['kind'] and metadata['selectedEpoch'] == epoch and
                metadata['selectedModelSha256'] == digest, 'Endpoint identity mismatch')
        if epoch:
            require(metadata['endpoint'] == event and Path(metadata['sourceRun']).resolve() == run.resolve(), 'Endpoint evidence differs')
        if epoch in eligible:
            curve.append({'epoch': epoch, 'samples': event['samples'], 'updates': event['updates'],
                          'trainingSeconds': event['trainingSeconds'], 'validation': event['validation'],
                          'model': str(endpoint), 'modelSha256': digest,
                          'metadataSha256': sha(endpoint/'result.json')})
    chosen = min(curve, key=lambda e: e['validation']['outcomeHalfMse'])
    require(sha(run/'selected.model') == report['selectedModelSha256'] and
            sha(run/'selected.state') == report['selectedStateSha256'] and
            sha(run/'last.state') == report['lastStateSha256'], 'Parent checkpoint identity')
    return {'run': str(run), 'kind': report['kind'], 'curve': curve, 'selected': chosen,
            'parentSelectedEpoch': report['selectedEpoch'],
            'selectedEpochIsParentSelection': chosen['epoch'] == report['selectedEpoch'],
            'optimizationSeconds': events[-1]['trainingSeconds'], 'processSeconds': execution['elapsedSeconds'],
            'processMemory': execution.get('processMemory'), 'resultSha256': sha(run/'result.json'),
            'receiptSha256': sha(str(run)+'.execution.json'), 'exactNextBatchResume': True,
            'gen0ModelSha256': sha(run/'gen0'/'selected.model'),
            'gen0StateSha256': sha(run/'gen0'/'selected.state')}, execution


def analyze(plan):
    require(plan['schema'] == 'brn-architecture-nested-plan-v1', 'Plan schema')
    require(len({(s['family'],s['seed']) for s in plan['runs']}) == len(plan['runs']) and plan['runs'], 'Duplicate/empty training pairs')
    require(plan['smallCount']*plan['smallEpochs'] == plan['largeCount']*plan['largeEpochs'], 'Unequal exposure budgets')
    require(len(plan['smallEligibleEpochs']) == len(plan['largeEligibleEpochs']), 'Unequal selection opportunities')
    bridge = plan['referenceBridge']
    require(sha(bridge['plan']) == bridge['planSha256'] and sha(bridge['result']) == bridge['resultSha256'], 'Reference bridge changed')
    bridge_plan, bridge_result = read(bridge['plan']), read(bridge['result'])
    require(bridge_result.get('allChecksPassed') and bridge_result['planSha256'] == bridge['planSha256'], 'Unverified reference bridge')
    bridged = {(r['family'],r['seed']) for r in bridge_result['runs'] if r.get('exactModelsAndMetrics')}
    require(bridged == {(r['family'],r['seed']) for r in plan['runs']}, 'Missing family/run reproduction')
    reconciliation, baseline, reconciled = None, None, {}
    if 'historicalCodeReconciliation' in plan:
        amendment = plan['historicalCodeReconciliation']
        require(sha(amendment['path']) == amendment['sha256'], 'Code reconciliation changed')
        reconciliation = read(amendment['path'])
        require(reconciliation['schema'] == 'brn-architecture-nested-code-reconciliation-v1'
                and reconciliation.get('allChecksPassed'), 'Unverified code reconciliation')
        require(sha(reconciliation['originalPlan']) == reconciliation['originalPlanSha256'], 'Original training plan changed')
        original = read(reconciliation['originalPlan'])
        require({k:v for k,v in plan.items() if k not in ['historicalCodeReconciliation','analyzerValidation']}
                == {k:v for k,v in original.items() if k != 'analyzerValidation'}, 'Analysis amendment changed study design')
        baseline_path = Path(plan['runs'][0]['large']['run']+'.execution.json')
        require(Path(reconciliation['baselineReceipt']).resolve() == baseline_path.resolve()
                and sha(baseline_path) == reconciliation['baselineReceiptSha256'], 'Historical baseline changed')
        baseline = read(baseline_path)
        reconciled = {(r['family'],r['seed']):r for r in reconciliation['runs']}
        require(len(reconciled) == len(reconciliation['runs']) == len(plan['runs'])
                and set(reconciled) == {(r['family'],r['seed']) for r in plan['runs']}, 'Reconciliation coverage')
    require(all(s['parentCounts'][0] == plan['largeCount'] and s['subsetCounts'][0] == plan['smallCount'] and
                s['parentCounts'][1] >= 4096 and s['subsetCounts'][1] >= 4096 for s in plan['datasets']), 'Plan/data populations differ')
    by_seed = {s['seed']:s for s in plan['datasets']}
    require(len(by_seed) == len(plan['datasets']), 'Duplicate dataset seed')
    data = [audit_subset(s) for s in plan['datasets']]
    paired, manifests = [], None
    for spec in plan['runs']:
        dataset = by_seed[spec['seed']]
        require(spec['small']['dataPayloadSha256'] == dataset['subsetPayloadSha256'] and
                spec['large']['dataPayloadSha256'] == dataset['parentPayloadSha256'], 'Run/data association differs')
        small, se = audit_training(spec['small'], plan['smallCount'], plan['smallEpochs'], plan['smallEligibleEpochs'])
        large, le = audit_training(spec['large'], plan['largeCount'], plan['largeEpochs'], plan['largeEligibleEpochs'])
        current = {k: se[k] for k in ['sourceSha256', 'compiledSha256', 'runnerSha256']}
        require(manifests is None or manifests == current, 'Code changed within nested training queue')
        manifests = current
        if reconciliation is None:
            require(code_delta(le,se) == bridge_plan['historicalCodeDelta'], 'Unaccounted historical code difference')
        else:
            evidence = reconciled[(spec['family'],spec['seed'])]
            require(Path(evidence['oldReceipt']).resolve() == Path(spec['large']['run']+'.execution.json').resolve()
                    and Path(evidence['newReceipt']).resolve() == Path(spec['small']['run']+'.execution.json').resolve()
                    and evidence['oldReceiptSha256'] == large['receiptSha256']
                    and evidence['newReceiptSha256'] == small['receiptSha256'], 'Reconciled execution identity changed')
            actual = reconciled_code_delta(le,se,baseline,bridge_plan['historicalCodeDelta'],
                                           reconciliation['historicalUncompiledSources'])
            require(actual == evidence['actualHistoricalCodeDelta'], 'Reconciled delta changed')
        require(small['kind'] == large['kind'] and small['gen0ModelSha256'] == large['gen0ModelSha256'] and
                small['gen0StateSha256'] == large['gen0StateSha256'], 'Initializer differs')
        require([e['samples'] for e in small['curve']] == [e['samples'] for e in large['curve']], 'Exposure grid mismatch')
        require(small['curve'][0]['validation'] == large['curve'][0]['validation'], 'Gen0 held-out metrics differ')
        paired.append({'family': spec['family'], 'seed': spec['seed'], 'small': small, 'large': large,
                       'selectedRawLossSmallOverLarge': small['selected']['validation']['outcomeHalfMse']/large['selected']['validation']['outcomeHalfMse']})
    return {'schema': 'brn-architecture-nested-results-v1', 'allChecksPassed': True,
            'datasets': data, 'pairs': paired,
            'newOptimizationSeconds': sum(r['small']['optimizationSeconds'] for r in paired),
            'newProcessSeconds': sum(r['small']['processSeconds'] for r in paired),
            'referenceBridge': bridge,
            'historicalCodeReconciliation': plan.get('historicalCodeReconciliation'),
            'codeManifestSha256': {k: hashlib.sha256(json.dumps(v, sort_keys=True, separators=(',',':')).encode()).hexdigest() for k,v in manifests.items()},
            'scope': 'Same held-out population and total exposures, different distinct data/repetition; prediction curves are not engine strength. Epoch checkpoints are inference-only; no optimizer state is attached to the eligible selection.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('plan_sha256')
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    require(not args.output.exists() and sha(args.plan) == args.plan_sha256, 'Output exists or frozen plan changed')
    result = analyze(read(args.plan))
    result.update(planSha256=args.plan_sha256, analyzerSha256=sha(Path(__file__)))
    args.output.write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({'allChecksPassed': True, 'pairs': len(result['pairs']), 'newProcessSeconds': result['newProcessSeconds']}))


if __name__ == '__main__':
    main()
