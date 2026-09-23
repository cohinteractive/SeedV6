"""Verify completed replay/diagnostic records and summarize the supervision experiment.

Arguments: experiment snapshots directory, measurements directory, canonical g128 measurements directory.
Generated analysis.json/tables.md stay with ignored measurements, never in a production store.
"""
from pathlib import Path
import collections
import hashlib
import json
import statistics as st
import sys


def read(path):
    return [json.loads(line) for line in path.read_text(encoding='utf-8').splitlines()]


def quantile(values, p):
    values = sorted(values)
    index = (len(values) - 1) * p
    lo = int(index)
    fraction = index - lo
    return values[lo] * (1 - fraction) + values[min(lo + 1, len(values) - 1)] * fraction


def ranks(values):
    result = [None] * len(values)
    indexes = sorted(range(len(values)), key=values.__getitem__)
    first = 0
    while first < len(values):
        last = first + 1
        while last < len(values) and values[indexes[last]] == values[indexes[first]]:
            last += 1
        for i in indexes[first:last]:
            result[i] = (first + last - 1) / 2
        first = last
    return result


def static(path):
    rows = read(path)
    assert rows[-1]['type'] == 'end'
    assert rows[0]['corpusSha256'] == '0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8'
    evaluations = {r['id']: r for r in rows if r['type'] == 'evaluation'}
    symmetric = {r['id']: r for r in rows if r['type'] == 'symmetry_position'}
    edges = [r for r in rows if r['type'] == 'delta']
    assert len(evaluations) == len(symmetric) == 248 and len(edges) == 233
    assert all(s['brn2'][key] == 0 for s in symmetric.values()
               for key in ('rawResidual', 'normalizedResidual', 'scoreResidual'))
    def metrics(architecture):
        values = {k: ({m: r[m] for m in ('rawPreTanh', 'normalized', 'searchScore')}
                      if architecture == 'brn2' else symmetric[k]['nnue']['original'])
                  for k, r in evaluations.items()}
        normalized = [v['normalized'] for v in values.values()]
        teacher = [symmetric[k]['nnue']['original']['normalized'] for k in values]
        iqr = quantile(normalized, .75) - quantile(normalized, .25)
        result = dict(rawSD=st.pstdev(v['rawPreTanh'] for v in values.values()),
                      normalizedSD=st.pstdev(normalized), normalizedIQR=iqr,
                      pearson=st.correlation(normalized, teacher), spearman=st.correlation(ranks(normalized), ranks(teacher)),
                      signAgreement=sum((a > 0) - (a < 0) == (b > 0) - (b < 0) for a, b in zip(normalized, teacher)) / 248)
        for category in ('quiet', 'tactical'):
            group = [e for e in edges if ('quiet' in e['categories']) == (category == 'quiet')]
            delta = st.median(abs(-values[e['id']]['normalized'] - values[e['parent']]['normalized']) for e in group)
            result[category + 'Delta'] = delta
            result[category + 'Ratio'] = delta / iqr
        return result
    return rows[0], metrics('brn2'), metrics('nnue'), evaluations


def search(path):
    rows = read(path)
    assert rows[-1]['type'] == 'end'
    searches = [r for r in rows if r['type'] == 'search' and not r['warmup']]
    assert len(searches) == 2
    for key in ('nodes', 'mainNodes', 'qNodes', 'completedDepth', 'status', 'score', 'move', 'pv', 'evaluationCalls'):
        assert searches[0][key] == searches[1][key], (path, key)
    for row in (r for r in rows if r['type'] == 'search'):
        assert row['nodes'] == row['mainNodes'] + row['qNodes']
    first = searches[0]
    result = {key: first[key] for key in ('mainNodes', 'qNodes', 'nodes', 'qRatio', 'completedDepth', 'status')}
    result.update(qPerMain=first['qNodes'] / first['mainNodes'] if first['mainNodes'] else None,
                  medianMs=st.median(r['elapsedNs'] / 1e6 for r in searches),
                  worstMs=max(r['elapsedNs'] / 1e6 for r in searches),
                  measuredMs=[r['elapsedNs'] / 1e6 for r in searches])
    return result


def table(headers, rows):
    def fmt(value):
        if value is None:
            return 'n/a'
        return f'{value:.9g}' if isinstance(value, float) else str(value)
    return '\n'.join(['| ' + ' | '.join(headers) + ' |', '|' + '|'.join(['---'] * len(headers)) + '|'] +
                     ['| ' + ' | '.join(map(fmt, row)) + ' |' for row in rows]) + '\n'


def main():
    experiment, output, canonical = map(Path, sys.argv[1:])
    canonical_hashes = json.loads((canonical / 'artifact-sha256.json').read_text())
    for name, expected in canonical_hashes.items():
        if name.startswith('search-') or name in ('static-final.jsonl', 'kiwipete-qshadow.jsonl'):
            assert hashlib.sha256((canonical / name).read_bytes()).hexdigest() == expected, name
    replay = read(experiment / 'replay.jsonl')
    assert replay[-1] == {'type': 'end', 'completed': True}
    gate = next(r for r in replay if r['type'] == 'gate')
    assert gate['passed'] and gate['tolerance'] == 0 and gate['generations'] == 128
    result = dict(gate=gate, finals=[r for r in replay if r['type'] == 'final'], milestones=[], blocks=[], search={}, qshadow={})
    for arm in ('wdl', 'blended', 'teacher'):
        generations = [r for r in replay if r['type'] == 'generation' and r['arm'].lower() == arm]
        assert [r['generation'] for r in generations] == list(range(1, 129))
        assert sum(r['trainingSamples'] for r in generations) == generations[-1]['step'] == 208104
        assert sum(r['heldOutSamples'] for r in generations) == 52974
        for filename, key in (('network.brn2', 'modelSha256'), ('training.state', 'trainingSha256')):
            assert hashlib.sha256((experiment / arm / 'latest-training-g128' / filename).read_bytes()).hexdigest() == generations[-1][key]
        for lo, hi in ((1, 32), (33, 64), (65, 96), (97, 128)):
            group = generations[lo-1:hi]
            result['blocks'].append(dict(arm=arm, first=lo, last=hi, promotions=sum(r['promoted'] for r in group),
                 **{k: st.mean(r[k] for r in group) for k in ('trainingLoss', 'candidateLoss', 'incumbentLoss')}))
        for boundary in (0, 32, 64, 96, 128):
            metadata = json.loads((experiment / arm / f'boundary-{boundary}' / 'experiment.json').read_text())
            for filename, key in (('network.brn2', 'modelSha256'), ('training.state', 'trainingSha256')):
                assert hashlib.sha256((experiment / arm / f'boundary-{boundary}' / filename).read_bytes()).hexdigest() == metadata[key]
            identity, metrics, teacher, evaluations = static(output / f'static-{arm}-{boundary}.jsonl')
            assert identity['brn2']['modelSha256'] == metadata['modelSha256']
            result['milestones'].append(dict(arm=arm, boundary=boundary, bestGeneration=metadata['bestGeneration'], **metrics))
            result['nnueStatic'] = teacher
            if arm == 'wdl' and boundary == 128:
                old_identity, _, _, old_evaluations = static(canonical / 'static-final.jsonl')
                assert identity['brn2']['modelSha256'] == old_identity['brn2']['modelSha256']
                assert evaluations == old_evaluations, 'Control final static records differ from canonical'
        pattern = 'search-brn2-*.jsonl' if arm == 'wdl' else f'search-{arm}-*.jsonl'
        folder = canonical if arm == 'wdl' else output
        result['search'][arm] = {p.stem.split('-', 2)[2]: search(p) for p in sorted(folder.glob(pattern))}
        assert len(result['search'][arm]) == 15
        shadow = canonical / 'kiwipete-qshadow.jsonl' if arm == 'wdl' else output / f'qshadow-{arm}-kiwipete.jsonl'
        rows = read(shadow)
        summary = next(r for r in rows if r['type'] == 'qtrace_summary')
        assert summary['completeAccounting']
        measured = next(r for r in rows if r['type'] == 'search')
        normal = result['search'][arm]['middlegame-kiwipete']
        for k in ('nodes', 'mainNodes', 'qNodes', 'completedDepth', 'status'):
            assert measured[k] == normal[k], (arm, k)
        result['qshadow'][arm] = summary['metrics']['adjacentStaticAbsoluteDelta']
    result['search']['nnue'] = {p.stem.split('-', 2)[2]: search(p) for p in sorted(canonical.glob('search-nnue-*.jsonl'))}
    result['searchAggregates'] = {}
    for arm, positions in result['search'].items():
        main = sum(r['mainNodes'] for r in positions.values())
        q = sum(r['qNodes'] for r in positions.values())
        result['searchAggregates'][arm] = dict(main=main, q=q, nodes=main+q, qPerTotal=q/(main+q), qPerMain=q/main,
             sumMedianMs=sum(r['medianMs'] for r in positions.values()), worstMs=max(r['worstMs'] for r in positions.values()),
             statuses=dict(collections.Counter(r['status'] for r in positions.values())))
    receipts = json.loads((output / 'processes.json').read_text())
    assert all(r['exit'] == 0 and not r['watchdog'] for r in receipts)
    assert json.loads((output / 'input-integrity.json').read_text())['unchanged']
    columns = ['rawSD', 'normalizedSD', 'quietDelta', 'quietRatio', 'tacticalDelta', 'tacticalRatio', 'pearson', 'spearman', 'signAgreement']
    sections = [table(['Arm', 'Boundary', 'Best'] + columns,
                     [[r['arm'], r['boundary'], r['bestGeneration']] + [r[k] for k in columns] for r in result['milestones']]),
                table(['Arm', 'Best', 'Promotions', 'WDL loss', 'Blend loss', 'Teacher loss'],
                     [[r[k] for k in ('arm', 'bestGeneration', 'promotions', 'wdlLoss', 'blendedLoss', 'teacherLoss')] for r in result['finals']]),
                table(['Arm', 'Block', 'Train', 'Candidate', 'Incumbent', 'Promotions'],
                     [[r['arm'], f"{r['first']}-{r['last']}"] + [r[k] for k in ('trainingLoss', 'candidateLoss', 'incumbentLoss', 'promotions')] for r in result['blocks']])]
    keys = ['mainNodes', 'qNodes', 'nodes', 'qRatio', 'qPerMain', 'medianMs', 'completedDepth', 'status']
    sections.append(table(['Arm', 'Position'] + keys,
                     [[arm, p] + [r[k] for k in keys] for arm, positions in result['search'].items() for p, r in positions.items()]))
    for name, data in [('analysis.json', json.dumps(result, indent=2)), ('tables.md', '\n'.join(sections))]:
        with (output / name).open('x', encoding='utf-8') as stream:
            stream.write(data)
    print(json.dumps({k: result[k] for k in ('finals', 'searchAggregates')}, indent=2))


if __name__ == '__main__':
    main()
