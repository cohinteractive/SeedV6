"""Summarize frozen HL reduced/nominal reference samples without population claims."""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    rows = [json.loads(line) for line in args.input.read_text().splitlines()]
    samples = [r for r in rows if r.get('type') == 'sample']
    assert rows[-1] == {'status': 'passed', 'samples': len(samples)}
    counts, strata, witnesses = Counter(), defaultdict(Counter), []
    for row in samples:
        sample, reduced, nominal = (row[k] for k in ('sample', 'reduced', 'nominal'))
        if not (reduced['result']['completed'] and nominal['result']['completed']):
            label = 'incomplete'
        else:
            r, n = (x['parentRelativeScore'] > sample['alpha'] for x in (reduced, nominal))
            label = ('reduced-high' if r else 'reduced-low') + '/' + ('nominal-high' if n else 'nominal-low')
            if not r and n:
                witnesses.append({'root': row['root'], 'sample': sample,
                                  'reduced': reduced['parentRelativeScore'],
                                  'nominal': nominal['parentRelativeScore']})
        counts[label] += 1
        strata[sample['kind']][label] += 1
    result = {'status': 'passed', 'input': str(args.input),
              'inputSha256': hashlib.sha256(args.input.read_bytes()).hexdigest(),
              'samples': len(samples), 'counts': counts, 'byBand': strata,
              'depths': dict(Counter(str(r['sample']['depth']) for r in samples)),
              'witnesses': witnesses,
              'interpretation': 'Prospective first-event stratified samples, not random population frequencies. '
              'Threshold disagreement between independent reduced and nominal TT-off qtrees is not a correctness defect '
              'or actual flat-kernel false-low count. Real history replay and legal PV checks passed; '
              'selective extension/order/TT context and original mate distance are not reproduced.'}
    with args.output.open('x', newline='\n') as stream:
        json.dump(result, stream, indent=2)
        stream.write('\n')
    print(json.dumps({k: v for k, v in result.items() if k != 'witnesses'}))


if __name__ == '__main__':
    main()
