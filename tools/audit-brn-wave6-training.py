"""Read-only W6 history/ancestry audit after BrnLineageAudit's codec checks.

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
    assert len(stores) == 5
    results = []
    for store in stores[1:]:
        name, root = store['name'], Path(store['root'])
        rate = .001
        origin = json.loads((root / 'degree-continuation-origin.json').read_text())
        chain = store['chain']
        assert [x['generation'] for x in chain] == list(range(96, 129))
        initial = chain[0]
        expected_architecture = 'BRN_GEN32_QUADRATIC' if 'RESQUAD' in name else 'BRN_GEN32_CUBIC_R4' if '-R4-' in name else 'BRN_GEN32_CUBIC' if '-R16-' in name else 'BRN_PAIR2'
        assert all(c['architecture'] == expected_architecture for c in chain)
        assert origin['arm']['projection'] == ('quadratic' if 'RESQUAD' in name else 'exact')
        assert origin['bootstrap'] == initial['id'] == store['initializer']['initialCheckpoint']
        assert initial['parentId'] == ''
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
        assert len(store['checkpoints']) == 33
        for checkpoint in store['checkpoints']:
            gen = checkpoint['manifest']['generation']
            assert checkpoint['optimizerStep'] == checkpoint['manifest']['optimizerStep']
            assert checkpoint['optimizerRuntimeScoreParity'] is True
            assert checkpoint['optimizer'] == dict(learningRate=rate,
                                                   beta1=.9, beta2=.999, epsilon=1e-8)
        session = store['session']
        assert (session['baseline'], session['target'], session['completed'], session['state']) == (96, 32, 32, 'COMPLETED')
        history = store['history']
        assert not history['warnings']
        rows = history['records']
        assert [x['generation'] for x in rows] == list(range(97, 129))
        ledger = store['ledger']
        reservations = ledger['reservations']
        assert len(reservations) == 128
        assert [r['generation'] for r in reservations] == list(range(1, 129))
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
        for row, checkpoint, reservation in zip(rows, chain[1:], reservations[96:]):
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
                            generations=32, trainingSamples=4194304, validationSamples=1048576,
                            trainingVisits=33554432, promotions=sum(r['decision']=='PROMOTE' for r in rows),
                            first8=means(rows[:8]), last8=means(rows[-8:]),
                            trainingSeconds=sum(r['trainingNanos'] for r in rows)/1e9,
                            finalComparison=rows[-1]['bootstrap']['comparison'],
                            finalCursors=cursors, originSha256=sha(root/'degree-continuation-origin.json'),
                            generationMetrics=[dict(generation=r['generation'], trainingLoss=r['loss'],
                                validationLoss=r['bootstrap']['comparison']['candidateLoss'],
                                incumbentLoss=r['bootstrap']['comparison']['bestLoss'], decision=r['decision']) for r in rows]))
    comparisons = []
    for left, right in [(stores[1],stores[2]), (stores[3],stores[2])]:
        lrows, rrows = left['history']['records'], right['history']['records']
        for l, r in zip(left['ledger']['reservations'], right['ledger']['reservations']):
            for key in ['generation','mix','ranges','trainingHash','validationHash','status']:
                assert l[key] == r[key], (left['name'], key)
        delta = [l['bootstrap']['comparison']['candidateLoss']-r['bootstrap']['comparison']['candidateLoss'] for l,r in zip(lrows,rrows)]
        comparisons.append(dict(left=left['name'],right=right['name'],identicalDataAll128Generations=True,
                                leftValidationWins=sum(d<0 for d in delta),
                                meanLeftMinusRight=statistics.mean(delta),last8LeftMinusRight=statistics.mean(delta[-8:])))
    report = dict(id='wave6-trained-evidence-v1', audit=str(audit),
                  auditFiles={p.name:sha(p) for p in audit.iterdir() if p.is_file()},
                  reference=stores[0]['checkpoints'][0]['manifest'], lineages=results, pairedComparisons=comparisons,
                  limitations=['Codec/runtime parity is verified separately by BrnLineageAudit.',
                    'Sequential generations use different validation examples; raw temporal means are descriptive.',
                    'Best is a sequential held-out promotion decision, not the global minimum on a fixed panel.',
                    'Data matches between Q16/C16/C4; BRE raw ranges/splits and historical exposure differ. Q16 inherits cubic-trained embeddings/Adam.',
                    'WDL Loss GUI label does not change the recorded half-squared target-error objective.',
                    'No chess strength conclusion follows from these losses.'])
    output.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(dict(lineages=len(results), verifiedCheckpointPairs=133, pairedComparisons=comparisons)))


if __name__ == '__main__':
    main()
