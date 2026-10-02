"""Material-anchored paired NNUE calibration research; Python standard library only.

Usage: python tools/analyze-nnue-cp-calibration.py PAIRS.tsv NEW_OUTPUT_DIRECTORY
No production configuration or model is written. Cluster splits keep games/roots intact.
"""
import collections
import csv
import hashlib
import json
import math
import pathlib
import random
import statistics
import sys

PIECES = ('pawn', 'knight', 'bishop', 'rook', 'queen')


def quantile(values, p):
    values = sorted(values)
    if not values:
        return None
    pos = (len(values) - 1) * p
    lo = int(pos)
    return values[lo] + (values[min(lo + 1, len(values) - 1)] - values[lo]) * (pos - lo)


def distribution(values):
    values = list(values)
    return dict(n=len(values), mean=statistics.fmean(values),
                sd=statistics.pstdev(values),
                **{name: quantile(values, p) for name, p in
                   [('min', 0), ('p05', .05), ('q25', .25), ('median', .5), ('q75', .75), ('p95', .95), ('max', 1)]})


def slope(rows, weights=None):
    weights = weights or [1.0] * len(rows)
    xy = math.fsum(w * r['x'] * r['y'] for w, r in zip(weights, rows))
    xx = math.fsum(w * r['x'] ** 2 for w, r in zip(weights, rows))
    return xy / xx if xx else 0.0


def prediction(row, coefficients):
    a1, a3 = coefficients
    return a1 * row['x'] + a3 * row['z']


def metrics(rows, coefficients):
    error = [prediction(r, coefficients) - r['y'] for r in rows]
    return dict(n=len(rows), mae=statistics.fmean(abs(e) for e in error),
                rmse=math.sqrt(statistics.fmean(e * e for e in error)),
                residual=distribution(error))


def cubic(rows, domain):
    xx = math.fsum(r['x'] ** 2 for r in rows)
    xz = math.fsum(r['x'] * r['z'] for r in rows)
    zz = math.fsum(r['z'] ** 2 for r in rows)
    xy = math.fsum(r['x'] * r['y'] for r in rows)
    zy = math.fsum(r['z'] * r['y'] for r in rows)
    det = xx * zz - xz * xz
    unconstrained = ((xy * zz - zy * xz) / det, (zy * xx - xy * xz) / det)
    candidates = [(max(0, xy / xx), 0.0), (0.0, max(0, zy / zz))]
    # Convex cone boundary: derivative at the validity-domain endpoint is zero.
    factor = 3 * domain * domain
    t = [r['x'] - r['z'] / factor for r in rows]
    a1 = max(0.0, math.fsum(v * r['y'] for v, r in zip(t, rows)) / math.fsum(v * v for v in t))
    candidates.append((a1, -a1 / factor))
    if min(unconstrained[0], unconstrained[0] + factor * unconstrained[1]) >= 0:
        candidates.append(unconstrained)
    selected = min(candidates, key=lambda c: math.fsum((prediction(r, c) - r['y']) ** 2 for r in rows))
    return dict(coefficients=selected, unconstrained=unconstrained, domain=domain,
                derivative_min=min(selected[0], selected[0] + factor * selected[1]))


def huber(rows):
    k = slope(rows)
    for _ in range(30):
        errors = [k * r['x'] - r['y'] for r in rows]
        median = statistics.median(errors)
        scale = max(1e-12, 1.4826 * statistics.median(abs(e - median) for e in errors))
        weights = [min(1.0, 1.345 * scale / max(abs(e), 1e-12)) for e in errors]
        new = slope(rows, weights)
        if abs(new - k) < 1e-9:
            break
        k = new
    return k


def l1_slope(rows):
    ratios = sorted((r['y'] / r['x'], abs(r['x'])) for r in rows if abs(r['x']) > 1e-10)
    midpoint = math.fsum(w for _, w in ratios) / 2
    cumulative = 0
    for ratio, weight in ratios:
        cumulative += weight
        if cumulative >= midpoint:
            return max(0.0, ratio)
    return 0.0


def bootstrap(rows, repeats=1000):
    # Resample whole independent game/root clusters within each source.
    sums = collections.defaultdict(lambda: [0.0, 0.0])
    for r in rows:
        s = sums[(r['source'], r['cluster'])]
        s[0] += r['x'] * r['y']
        s[1] += r['x'] ** 2
    strata = collections.defaultdict(list)
    for (source, _), value in sorted(sums.items()):
        strata[source].append(value)
    rng = random.Random(20261002)
    estimates = []
    for _ in range(repeats):
        xy = xx = 0.0
        for values in strata.values():
            for _ in values:
                a, b = values[rng.randrange(len(values))]
                xy += a
                xx += b
        estimates.append(xy / xx if xx else 0.0)
    return dict(repeats=repeats, clusters=len(sums), lower=quantile(estimates, .025), upper=quantile(estimates, .975))


def describe(rows, coefficients):
    ratios = [r['y'] / r['x'] for r in rows if abs(r['x']) > 1e-8]
    positive = [v for v in ratios if v > 0]
    trimmed = sorted(ratios)[len(ratios) // 10:len(ratios) - len(ratios) // 10]
    calibrated = [math.copysign(1, r['y']) * prediction(r, coefficients) for r in rows]
    normalized = [math.copysign(1, r['y']) * r['x'] for r in rows]
    constant_target = len({abs(r['y']) for r in rows}) == 1
    target = abs(rows[0]['y'])
    return dict(n=len(rows), clusters=len({(r['source'], r['cluster']) for r in rows}),
                ls_k=slope(rows), implied_k_median=statistics.median(ratios),
                implied_k_trimmed_mean=statistics.fmean(trimmed),
                correct_sign_implied_k_median=statistics.median(positive) if positive else None,
                nearzero_delta_excluded=len(rows) - len(ratios),
                zero_delta_count=sum(r['x'] == 0 for r in rows),
                wrong_or_zero_fraction=sum(r['x'] * r['y'] <= 0 for r in rows) / len(rows),
                mean_response_k=target / statistics.fmean(normalized) if constant_target and statistics.fmean(normalized) else None,
                median_response_k=target / statistics.median(normalized) if constant_target and statistics.median(normalized) else None,
                signed_delta_raw=distribution(normalized), calibrated_delta_cp=distribution(calibrated),
                metrics=metrics(rows, coefficients))


def fold(row, salt=0):
    identity = f"nnue-cp-v1:{salt}:{row['source']}:{row['cluster']}"
    return int.from_bytes(hashlib.sha256(identity.encode()).digest()[:8], 'big') % 5


def analyze(rows, label):
    train = [r for r in rows if fold(r) != 0]
    test = [r for r in rows if fold(r) == 0]
    domain = max(max(abs(r['r0']), abs(r['r1'])) for r in train)
    k = slope(train)
    candidate = cubic(train, domain)
    # Evaluation may extend beyond the train domain; record rather than hide that limit.
    candidate['test_outside_domain'] = sum(max(abs(r['r0']), abs(r['r1'])) > domain for r in test)
    candidate['train'] = metrics(train, candidate['coefficients'])
    candidate['test'] = metrics(test, candidate['coefficients'])
    result = dict(label=label, pairs=len(rows), train_pairs=len(train), test_pairs=len(test),
                  base_positions=len({(r['source'], r['index']) for r in rows}),
                  all_ls_k=slope(rows), train_k=k, bootstrap=bootstrap(train),
                  robust_huber_k=huber(train), robust_l1_k=l1_slope(train),
                  train=metrics(train, (k, 0)), test=metrics(test, (k, 0)), cubic=candidate,
                  raw_endpoints=distribution(v for r in rows for v in (r['r0'], r['r1'])))
    balance = collections.Counter(r['piece'] for r in train)
    balanced_k = slope(train, [1 / balance[r['piece']] for r in train])
    result['piece_balanced'] = dict(k=balanced_k, test=metrics(test, (balanced_k, 0)))
    groupings = {
        'piece': lambda r: r['piece'],
        'phase': lambda r: 'opening' if r['phase'] <= 7 else 'middlegame' if r['phase'] <= 16 else 'endgame',
        'sign': lambda r: 'positive' if r['y'] > 0 else 'negative',
        'stm': lambda r: 'white' if r['stm'] == 0 else 'black',
        'removed_side': lambda r: 'white' if r['removed_side'] == 0 else 'black',
        'source': lambda r: r['source'],
        'magnitude': lambda r: '<0.25' if abs(r['r0']) < .25 else '0.25-0.75' if abs(r['r0']) < .75 else '0.75-1.5' if abs(r['r0']) < 1.5 else '>=1.5',
        'material_balance': lambda r: '<=100' if abs(r['material']) <= 100 else '101-500' if abs(r['material']) <= 500 else '>500',
        'base_sign': lambda r: 'positive' if r['r0'] > 0 else 'negative' if r['r0'] < 0 else 'zero',
        'piece_phase': lambda r: r['piece'] + ':' + ('opening' if r['phase'] <= 7 else 'middlegame' if r['phase'] <= 16 else 'endgame'),
    }
    result['groups'] = {}
    result['heldout_groups'] = {}
    for name, getter in groupings.items():
        groups = collections.defaultdict(list)
        heldout = collections.defaultdict(list)
        for r in rows:
            groups[getter(r)].append(r)
        for r in test:
            heldout[getter(r)].append(r)
        result['groups'][name] = {g: describe(q, (k, 0)) for g, q in sorted(groups.items())}
        result['heldout_groups'][name] = {g: describe(q, (k, 0)) for g, q in sorted(heldout.items())}
        if name in ('piece', 'phase'):
            for g, q in sorted(groups.items()):
                result['groups'][name][g]['bootstrap'] = bootstrap(q)
    result['cubic_heldout_piece'] = {
        p: describe([r for r in test if r['piece'] == p], candidate['coefficients']) for p in PIECES
    }
    result['robust_test'] = {
        'huber': metrics(test, (result['robust_huber_k'], 0)),
        'l1': metrics(test, (result['robust_l1_k'], 0)),
    }
    result['split_stability'] = []
    for salt in range(4):
        tr = [r for r in rows if fold(r, salt) != 0]
        te = [r for r in rows if fold(r, salt) == 0]
        value = slope(tr)
        result['split_stability'].append(dict(salt=salt, k=value, train_pairs=len(tr), test=metrics(te, (value, 0))))
    identities = sorted({(r['source'], r['cluster']) for r in train}, key=lambda v: hashlib.sha256(str(v).encode()).digest())
    result['convergence'] = []
    for fraction in (.25, .5, .75, 1):
        selected = set(identities[:max(1, int(len(identities) * fraction))])
        subset = [r for r in train if (r['source'], r['cluster']) in selected]
        result['convergence'].append(dict(fraction=fraction, clusters=len(selected), pairs=len(subset), k=slope(subset)))
    reverse = [dict(r, x=r['reverse_r1'] - r['reverse_r0'], y=-r['y'],
                    z=r['reverse_r1'] ** 3 - r['reverse_r0'] ** 3) for r in rows]
    result['symmetry'] = dict(reverse_ls_k=slope(reverse),
                             raw_stm_sum=distribution(r['r0'] + r['reverse_r0'] for r in rows),
                             paired_delta_sum=distribution(r['x'] + q['x'] for r, q in zip(rows, reverse)),
                             reversed_wrong_fraction=sum(r['x'] * r['y'] <= 0 for r in reverse) / len(reverse),
                             reverse_test=metrics([r for r in reverse if fold(r) == 0], (k, 0)))
    return result


def self_test():
    # Independent hand-computable identities: perfect K=1000, then exact odd cubic.
    synthetic = [dict(x=x, z=z, y=1000 * x) for x, z in [(1, 1), (2, 8), (-1, -1)]]
    assert slope(synthetic) == 1000
    assert metrics(synthetic, (1000, 0))['rmse'] == 0
    assert abs(cubic(synthetic, 2)['coefficients'][0] - 1000) < 1e-10
    curved = [dict(r, y=500 * r['x'] + 20 * r['z']) for r in synthetic]
    c = cubic(curved, 2)
    assert abs(c['coefficients'][0] - 500) < 1e-10 and abs(c['coefficients'][1] - 20) < 1e-10
    e = metrics(synthetic, (900, 0))
    assert abs(e['mae'] - 400 / 3) < 1e-10 and abs(e['rmse'] - math.sqrt(20000)) < 1e-10
    assert abs(quantile([0, 10], .25) - 2.5) < 1e-12


def main():
    self_test()
    pair_file, output = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
    output.mkdir() # Fail if a prior result exists.
    rows = []
    with pair_file.open(encoding='utf-8') as stream:
        for row in csv.DictReader(stream, delimiter='\t'):
            for key in ('r0', 'r1', 'reverse_r0', 'reverse_r1'):
                row[key] = float(row[key])
            for key in ('phase', 'stm', 'removed_side', 'cp', 'material', 'index', 'square', 'max_see0', 'max_see1'):
                row[key] = int(row[key])
            row['quiet'] = row['quiet'] == 'true'
            row['x'], row['z'], row['y'] = row['r1'] - row['r0'], row['r1'] ** 3 - row['r0'] ** 3, row['cp']
            rows.append(row)
    result = dict(schema='seedv6.nnue-cp-calibration.research.v1',
                  pairs_sha256=hashlib.sha256(pair_file.read_bytes()).hexdigest(),
                  split='SHA256(nnue-cp-v1:{salt}:{source}:{cluster}) first eight bytes big endian mod 5; 0 held out',
                  bootstrap='1000 within-source whole-game/root cluster resamples, random.Random(20261002)',
                  self_tests='passed')
    for label, subset in [('broad', rows), ('quiet', [r for r in rows if r['quiet']]),
                          ('quiet_abs_raw_le_1', [r for r in rows if r['quiet'] and max(abs(r['r0']), abs(r['r1'])) <= 1])]:
        result[label] = analyze(subset, label)
        q = result[label]
        print(label, 'N', q['pairs'], 'K', q['train_k'], 'CI', q['bootstrap'],
              'test MAE/RMSE', q['test']['mae'], q['test']['rmse'],
              'cubic', q['cubic']['coefficients'], 'test MAE/RMSE', q['cubic']['test']['mae'], q['cubic']['test']['rmse'])
    # Deterministic representative heldout examples at each piece's median signed predicted delta.
    k = result['quiet']['train_k']
    examples = []
    for piece in PIECES:
        subset = [r for r in rows if r['quiet'] and fold(r) == 0 and r['piece'] == piece]
        subset.sort(key=lambda r: math.copysign(1, r['y']) * k * r['x'])
        for percentile in (.1, .5, .9):
            r = subset[int((len(subset) - 1) * percentile)]
            examples.append(dict(r, percentile=percentile, calibrated_delta=k * r['x']))
    with (output / 'heldout-examples.tsv').open('w', encoding='utf-8', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(examples[0]), delimiter='\t')
        writer.writeheader()
        writer.writerows(examples)
    (output / 'results.json').write_text(json.dumps(result, indent=2, allow_nan=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
