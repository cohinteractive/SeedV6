"""Bound raw-ordinal overlap from audited ledgers; never infer selected-record identity.

Usage: python -B tools/analyze-brn-source-exposure.py AUDITED_STORES NEW_REPORT
Reads the already frozen audit only; no corpus scan, training or owner-store access.
"""
import hashlib
import json
from pathlib import Path
import sys


def merged(intervals):
    result = []
    for start, end in sorted(intervals):
        assert 0 <= start < end
        if result and start <= result[-1][1]:
            result[-1][1] = max(end, result[-1][1])
        else:
            result.append([start, end])
    return result


def ranges(store, first=1, last=64):
    rows = [r for r in store['ledger']['reservations'] if first <= r['generation'] <= last]
    assert all(r['status'] == 'COMPLETED' for r in rows)
    return [r for reservation in rows for r in reservation['ranges']]


def coverage(rows):
    return {source: merged((r['start'], r['end']) for r in rows if r['source'] == source)
            for source in sorted({r['source'] for r in rows})}


def bounds(rows, scanned):
    result = {}
    for kind in ('training', 'validation'):
        lower = upper = count = 0
        for r in rows:
            size = r['end'] - r['start']
            overlap = sum(max(0, min(r['end'], b) - max(r['start'], a))
                          for a, b in scanned.get(r['source'], []))
            assert 0 <= overlap <= size
            n = r[kind]
            count += n
            lower += max(0, n - (size - overlap))
            upper += min(n, overlap)
        result[kind] = dict(selected=count, selectedOrdinalsInsideOtherScannedRanges=[lower, upper],
                            selectedOrdinalsOutsideOtherScannedRanges=[count-upper, count-lower])
    return result


def main():
    source, target = map(Path, sys.argv[1:])
    assert not target.exists(), 'Preserve previous evidence'
    stores = json.loads(source.read_text(encoding='utf-8'))
    assert stores[0]['name'] == 'Gen32' and len(stores) == 7
    historical = ranges(stores[0], last=32)
    assert len(historical) == 64
    baseline = coverage(historical)
    bre = next(x for x in stores if x['name'] == 'W4-BRE-CONTROL-LR001')
    residual = next(x for x in stores if x['name'] == 'W4-RESCUBIC-LR001')
    comparisons = []
    for store in stores[1:]:
        rows = ranges(store)
        assert len(rows) == 128
        comparisons.append(dict(name=store['name'], scannedRanges=coverage(rows),
                                againstFrozenGen32ScannedRanges=bounds(rows, baseline)))
    result = dict(id='wave4-source-exposure-bounds-v1', source=str(source),
                  sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                  frozenGen32ScannedRanges=baseline, lineages=comparisons,
                  residualAgainstContinuedBreScannedRanges=bounds(ranges(residual), coverage(ranges(bre))),
                  continuedBreAgainstResidualScannedRanges=bounds(ranges(bre), coverage(ranges(residual))),
                  limitations=[
                      'Raw ordinal intervals are not exact selected training-position intersections.',
                      'Inside a previously scanned interval does not prove that the earlier model trained on that record.',
                      'Outside a scanned interval proves different source ordinals, not different FENs, geometries or games.',
                      'All post-reference streams restarted at zero in W3 by design; W4 inherited and continued them.',
                      'Whole-model validation independence was never established: the frozen base can have seen correction holdout.',
                      'An inference hybrid combines two optimization streams; matching summed updates alone does not match data diversity or sparse/dense optimization.'
                  ])
    target.write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
