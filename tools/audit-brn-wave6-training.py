"""Read-only W6/W7/W8 history/ancestry audit after BrnLineageAudit's codec checks.

Usage: python -B tools/audit-brn-wave6-training.py AUDIT_DIRECTORY NEW_REPORT
The report binds live evidence, but never changes a training store.
"""
import hashlib
import json
import math
import statistics
import sys
from pathlib import Path


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    audit, output = map(Path, sys.argv[1:])
    assert not output.exists(), "Preserve prior evidence"
    stores = json.loads((audit / 'stores.json').read_text())
    wave = json.loads((Path(stores[1]['root']) / 'degree-continuation-origin.json').read_text())['wave']
    assert wave in ('brn-successor-2026-10-21-wave6', 'brn-successor-2026-10-21-wave7', 'brn-successor-2026-10-22-wave8')
    fixed = wave.endswith('wave8')
    compact = wave.endswith('wave7')
    number, start = (8, 160) if fixed else (7, 128) if compact else (6, 96)
    additional = 8 if fixed else 32
    end = start + additional
    assert len(stores) == (4 if compact or fixed else 5)
    results = []
    for store in stores[1:]:
        name, root = store['name'], Path(store['root'])
        rate = .001
        origin = json.loads((root / 'degree-continuation-origin.json').read_text())
        assert origin['wave'] == wave and name.startswith(f'W{number}-')
        chain = store['chain']
        assert [x['generation'] for x in chain] == list(range(start, end + 1))
        initial = chain[0]
        expected_architecture = 'BRN_GEN32_QUADRATIC' if 'RESQUAD' in name else 'BRN_GEN32_CUBIC_R4' if '-R4-' in name else 'BRN_GEN32_CUBIC' if '-R16-' in name else 'BRN_PAIR2'
        if compact and 'RESQUAD' in name:
            expected_architecture = 'BRN_GEN32_QUADRATIC_R4'
        if fixed:
            expected_architecture = 'BRN_GEN32_FIXED_CUBIC_R4' if 'FIXED-CUBIC' in name else 'BRN_GEN32_FIXED_QUADRATIC_R4' if 'FIXED-QUAD' in name else 'BRN_PAIR2'
        assert all(c['architecture'] == expected_architecture for c in chain)
        projection = ('fixed-cubic-heads' if 'FIXED-CUBIC' in name else 'fixed-quadratic-heads' if 'FIXED-QUAD' in name else 'exact') if fixed else ('quadratic' if 'RESQUAD' in name else 'exact')
        assert origin['arm']['projection'] == projection
        assert origin['bootstrap'] == initial['id'] == store['initializer']['initialCheckpoint']
        assert initial['parentId'] == ''
        if fixed and projection != 'exact':
            assert origin['inheritedStep'] == 1310720 and initial['optimizerStep'] == 0
        else:
            assert origin['inheritedStep'] == initial['optimizerStep']
        assert origin['arm']['rate'] == rate
        for field, pattern in [('networkSha256', 'network.*'), ('trainingSha256', 'training.state')]:
            assert initial[field] == origin[field]
            source = Path(origin['arm']['source']['root']) / 'checkpoints' / origin['arm']['source']['checkpoint']
            files = list(source.glob(pattern))
            assert len(files) == 1 and sha(files[0]) == origin['arm']['source'][field]
        for previous, current in zip(chain, chain[1:]):
            assert current['parentId'] == previous['id']
            assert current['optimizerStep'] == previous['optimizerStep'] + 8192
        assert store['latestTraining'] == chain[-1]['id']
        assert len(store['checkpoints']) == additional + 1
        if fixed and projection != 'exact':
            import importlib.util
            spec = importlib.util.spec_from_file_location('degree', Path(__file__).with_name('audit-brn-degree-projection.py'))
            codec = importlib.util.module_from_spec(spec); spec.loader.exec_module(codec)
            cubic = projection == 'fixed-cubic-heads'
            wire, frozen_values = (10, 3845) if cubic else (9, 3841)
            baseline = codec.decode(codec.payload(root, initial['id'], 'network.*'), False, wire)[0]
            source = origin['arm']['source']
            source_weights = codec.decode(codec.payload(source['root'], source['checkpoint'], 'network.*'), False, 8)[0]
            assert baseline == source_weights + (bytes(32) if cubic else b'')
            for c in chain:
                network = codec.decode(codec.payload(root, c['id'], 'network.*'), False, wire)[0]
                state = codec.decode(codec.payload(root, c['id'], 'training.state'), True, wire)
                assert state[0] == network
                assert network[:frozen_values*8] == baseline[:frozen_values*8]
                assert all(a[:frozen_values*8] == bytes(frozen_values*8) for a in state[1:])
                if c['id'] == initial['id']:
                    assert all(a == bytes(len(a)) for a in state[1:])
        for checkpoint in store['checkpoints']:
            gen = checkpoint['manifest']['generation']
            assert checkpoint['optimizerStep'] == checkpoint['manifest']['optimizerStep']
            assert checkpoint['optimizerRuntimeScoreParity'] is True
            assert checkpoint['optimizer'] == dict(learningRate=rate,
                                                   beta1=.9, beta2=.999, epsilon=1e-8)
        session = store['session']
        assert (session['baseline'], session['target'], session['completed'], session['state']) == (start, additional, additional, 'COMPLETED')
        history = store['history']
        assert not history['warnings']
        rows = history['records']
        assert [x['generation'] for x in rows] == list(range(start + 1, end + 1))
        ledger = store['ledger']
        reservations = ledger['reservations']
        assert len(reservations) == end
        assert [r['generation'] for r in reservations] == list(range(1, end + 1))
        assert all(r['status'] == 'COMPLETED' for r in reservations)
        cursors = {}
        for reservation in reservations:
            for r in reservation['ranges']:
                assert r['start'] == cursors.get(r['source'], 0)
                assert r['end'] - r['start'] == r['training'] + r['validation'] + r['skipped']
                assert r['training'] == 65536 and r['validation'] == 16384
                cursors[r['source']] = r['end']
        assert cursors == ledger['next']
        best = initial['id']
        for row, checkpoint, reservation in zip(rows, chain[1:], reservations[start:]):
            assert row['candidate'] == checkpoint['id'] and row['incumbent'] == best
            assert row['decision'] in ('PROMOTE', 'RETAIN_INCUMBENT')
            if row['decision'] == 'PROMOTE':
                best = row['candidate']
            assert row['resultingBest'] == best
            boot = row['bootstrap']; corpus = boot['corpus']; comparison = boot['comparison']
            assert row['samples'] == 163840 and boot['trainingSamples'] == 131072
            assert corpus['usable'] == 131072 and corpus['heldOut'] == comparison['samples'] == 32768
            assert corpus['seed'] == boot['splitSeed'] == 1
            assert corpus['generation'] == row['generation']
            assert corpus['trainingHash'] == boot['dataHash'] == reservation['trainingHash']
            assert corpus['heldOutHash'] == reservation['validationHash']
            assert all(math.isfinite(v) for v in [row['loss'], comparison['candidateLoss'], comparison['bestLoss']])
            assert row['wins'] == row['draws'] == row['losses'] == 0
            settings = row['regime']['effectiveSettings']
            for part in ['threads=1', 'epochs=8', 'minibatchSize=128', 'shuffle=true',
                         'learningRate=' + str(rate), 'validation=WDL Loss', 'masterSeed=1',
                         'BRN3_SOURCE_OUTCOME_V1', 'finalLossObjective=half-squared target error']:
                assert part in settings, (name, part)
        assert best == store['best']
        assert not list(root.rglob('*partial*')), 'Inspect unexpected partial artifact'
        by_id = {x['id']: x for x in chain}
        def means(subset):
            return dict(trainingLoss=statistics.mean(r['loss'] for r in subset),
                        validationLoss=statistics.mean(r['bootstrap']['comparison']['candidateLoss'] for r in subset),
                        candidateMinusIncumbent=statistics.mean(r['bootstrap']['comparison']['candidateLoss']-r['bootstrap']['comparison']['bestLoss'] for r in subset))
        results.append(dict(name=name, root=str(root), origin=origin, initial=initial,
                            latest=chain[-1], best=by_id[best], session=session,
                            addedOptimizerSteps=chain[-1]['optimizerStep']-initial['optimizerStep'],
                            generations=additional, trainingSamples=131072*additional, validationSamples=32768*additional,
                            trainingVisits=1048576*additional, promotions=sum(r['decision']=='PROMOTE' for r in rows),
                            first8=means(rows[:8]), last8=means(rows[-8:]),
                            trainingSeconds=sum(r['trainingNanos'] for r in rows)/1e9,
                            finalComparison=rows[-1]['bootstrap']['comparison'],
                            finalCursors=cursors, originSha256=sha(root/'degree-continuation-origin.json'),
                            generationMetrics=[dict(generation=r['generation'], trainingLoss=r['loss'],
                                validationLoss=r['bootstrap']['comparison']['candidateLoss'],
                                incumbentLoss=r['bootstrap']['comparison']['bestLoss'], decision=r['decision']) for r in rows]))
        if fixed:
            result = results[-1]
            result.pop('first8'); result.pop('last8')
            result.update(first4=means(rows[:4]), last4=means(rows[-4:]),
                          optimizerSemantics='New zero head-only Adam' if projection != 'exact' else 'Exact inherited full Adam')
    comparisons = []
    for left, right in ([(stores[1],stores[2])] if compact or fixed else [(stores[1],stores[2]), (stores[3],stores[2])]):
        lrows, rrows = left['history']['records'], right['history']['records']
        for l, r in zip(left['ledger']['reservations'], right['ledger']['reservations']):
            for key in ['generation','mix','ranges','trainingHash','validationHash','status']:
                assert l[key] == r[key], (left['name'], key)
        delta = [l['bootstrap']['comparison']['candidateLoss']-r['bootstrap']['comparison']['candidateLoss'] for l,r in zip(lrows,rrows)]
        comparisons.append(dict(left=left['name'],right=right['name'],identicalDataThroughGeneration=end,
                                leftValidationWins=sum(d<0 for d in delta),
                                meanLeftMinusRight=statistics.mean(delta),last8LeftMinusRight=statistics.mean(delta[-8:])))
    report = dict(id=f'wave{number}-trained-evidence-v1', audit=str(audit),
                  auditFiles={p.name:sha(p) for p in audit.iterdir() if p.is_file()},
                  reference=stores[0]['checkpoints'][0]['manifest'], lineages=results, pairedComparisons=comparisons,
                  limitations=['Codec/runtime parity is verified separately by BrnLineageAudit.',
                    'Sequential generations use different validation examples; raw temporal means are descriptive.',
                    'Best is a sequential held-out promotion decision, not the global minimum on a fixed panel.',
                    'Data matches between residual arms; BRE raw ranges/splits and historical exposure differ. Quadratic inherits cubic-trained embeddings/Adam.',
                    'WDL Loss GUI label does not change the recorded half-squared target-error objective.',
                    'No chess strength conclusion follows from these losses.'])
    if fixed:
        report['limitations'][3] = 'Residual arms share data and exact initial Q4 function; only four heads train with new Adam. BRE retains different historical data/split/full optimizer.'
    output.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(dict(lineages=len(results), verifiedCheckpointPairs=1+(additional+1)*len(results), pairedComparisons=comparisons)))


if __name__ == '__main__':
    main()
