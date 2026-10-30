"""Descriptive analysis of the frozen clean H1/H2 successive-request diagnostic."""
import hashlib
import json
from pathlib import Path
import statistics

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-tt-memory'
RAW = OUT / 'raw/h2-cost-v1'


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    target = OUT / 'h2-cost-v1-summary.json'
    if target.exists():
        raise ValueError('Preserve completed analyses')
    run = json.loads((RAW / 'run.json').read_text())
    assert run['status'] == 'passed' and run['inputsStable']
    manifest = json.loads((OUT / 'h2-cost-v1-manifest.json').read_text())
    for path, expected in manifest['files'].items():
        assert digest(path) == expected, path
    summary = {
        'status': 'passed', 'protocol': 'h2-cost-v1-protocol.json',
        'manifest': 'h2-cost-v1-manifest.json', 'source': 'b0be4fe1ce5bfa8ac714108a3122b7615f25967d',
        'build': 82, 'analysisToolSha256': digest(__file__), 'domains': {},
        'interpretation': 'Descriptive integrated fixed-node cost, not playing strength. '
            'H1/H2 use the same frozen82 runtime; distinct selective trees are allowed. '
            'Warmup round-1 is retained and excluded. Root ratios average two measured '
            'repeats before taking the geometric mean across104 roots. Process CPU '
            'includes concurrent JVM activity. No scheduling neutrality or fine-grained '
            'cost significance is asserted. Legality/accounting/drain assertions ran in '
            'the harness on every request.'}
    for workers in (1, 4):
        path = RAW / f'w{workers}.jsonl'
        data = [json.loads(line) for line in path.read_text().splitlines()]
        identity, finish = data[0], data[-1]
        assert identity['source'] == 'b0be4fe1ce5b' and identity['build'] == 82
        assert identity['workers'] == workers and identity['head'] == 'vector-256-long'
        assert finish == {'status': 'passed', 'requests': 624}
        rows = data[1:-1]
        assert len(rows) == 624
        keyed = {(r['root'], r['round'], r['profile']): r for r in rows}
        roots = sorted({r['root'] for r in rows})
        assert len(roots) == 104 and len(keyed) == 624
        for root in roots:
            for repeat in (-1, 0, 1):
                for profile in ('h1', 'h2'):
                    row = keyed[root, repeat, profile]
                    assert row['outcome']['nodes'] == 100000 and row['workers'] == workers
                    assert row['elapsedNanos'] > 0 and row['processCpuNanos'] > 0
                    assert not row['outcome']['diagnostics']['enabled']
        measured = [r for r in rows if r['round'] >= 0]
        result = {'requests': 624, 'measuredRequests': len(measured), 'roots': 104,
                  'raw': str(path.relative_to(ROOT)), 'rawSha256': digest(path), 'profiles': {}}
        for profile in ('h1', 'h2'):
            selected = [r for r in measured if r['profile'] == profile]
            result['profiles'][profile] = {
                'requests': len(selected),
                'wallSeconds': sum(r['elapsedNanos'] for r in selected) / 1e9,
                'processCpuSeconds': sum(r['processCpuNanos'] for r in selected) / 1e9}
        for metric in ('elapsedNanos', 'processCpuNanos'):
            ratios = {
                root: sum(keyed[root, repeat, 'h2'][metric] for repeat in (0, 1)) /
                      sum(keyed[root, repeat, 'h1'][metric] for repeat in (0, 1))
                for root in roots}
            result[metric + 'H2OverH1'] = {
                'geometricMean': statistics.geometric_mean(ratios.values()),
                'median': statistics.median(ratios.values()), 'perRootRatios': ratios}
        summary['domains'][str(workers)] = result
    summary['evidenceSha256'] = {str(p.relative_to(ROOT)): digest(p) for p in
        (RAW / 'run.json', RAW / 'w1-console.log', RAW / 'w4-console.log')}
    with target.open('x', encoding='utf-8', newline='\n') as stream:
        stream.write(json.dumps(summary, indent=2) + '\n')
    for workers, result in summary['domains'].items():
        print(workers, {k: v['geometricMean'] for k, v in result.items() if k.endswith('H2OverH1')})


if __name__ == '__main__':
    main()
