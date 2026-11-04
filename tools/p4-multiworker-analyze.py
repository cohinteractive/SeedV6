"""Read-only MW scheduling analysis; writes a new compact summary, never raw evidence."""
import argparse
import hashlib
import json
from pathlib import Path
import statistics


def analyze(receipt_path):
    receipt = json.loads(receipt_path.read_text())
    assert receipt['status'] == 'passed' and receipt['inputsStable']
    assert receipt['drain']['ownedProcessesRemaining'] == 0
    source = Path(receipt['output'])
    assert hashlib.sha256(source.read_bytes()).hexdigest() == receipt['files'][str(source)]
    rows = [json.loads(line) for line in source.read_text().splitlines()]
    # The original frozen probe predates the explicit selector and is ROOT_REUSE.
    if rows[0].get('architecture', 'ROOT_REUSE') != 'ROOT_REUSE':
        raise ValueError('Serial first-root analysis requires ROOT_REUSE; whole-root invocation work is different')
    requests = [row for row in rows if row['type'] == 'request']
    attempts = [a for row in requests for a in row['attempts']]
    for row in requests:
        assert sum(a['result']['nodes'] for a in row['attempts']) == row['measurement']['outcome']['nodes']
    for a in attempts:
        assert sum(w['nodes'] for w in a['workers']) == a['result']['nodes']
        assert 0 <= a['firstOwnerJobNodes'] <= a['workers'][0]['nodes']
    split = [a for a in attempts if a['depth'] >= 3]
    nodes = sum(a['result']['nodes'] for a in split)
    shares = [sum(a['workers'][i]['nodes'] for a in split) / nodes for i in range(len(split[0]['workers']))]
    delays = [a['firstHelperDelayNanos'] / 1e6 for a in split if a.get('firstHelperDelayNanos') is not None]
    helperless = [a for a in split if sum(w['nodes'] for w in a['workers'][1:]) == 0]
    repeats = {}
    for row in requests:
        result = row['measurement']['outcome']['lastCompletedResult']
        repeats.setdefault(row['root'], []).append((result['bestMove'], result['score'], result['depth']))
    return dict(id=receipt['id'], receipt=str(receipt_path), evidenceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
        requests=len(requests), nodes=receipt['chargedNodes'], attempts=len(attempts), splitDepthAttempts=len(split),
        splitDepthNodes=nodes, firstOwnerJobNodeFraction=sum(a['firstOwnerJobNodes'] for a in split)/nodes,
        workerNodeShares=shares, noHelperAdmissionAttempts=len(helperless),
        noHelperAdmissionNodes=sum(a['result']['nodes'] for a in helperless),
        firstHelperDelayMedianMs=statistics.median(delays), firstHelperDelayMaxMs=max(delays),
        repeatedRoots=len(repeats), repeatBestMoveDisagreements=sum(len({r[0] for r in rs})>1 for rs in repeats.values()),
        repeatScoreOrDepthDisagreements=sum(len(set(rs))>1 for rs in repeats.values()),
        completedDepthMean=statistics.mean(r['measurement']['outcome']['lastCompletedResult']['depth'] for r in requests),
        peakOwnedCommittedGiB=receipt['peakOwnedCommittedGiB'], wallSeconds=receipt['wallSeconds'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('receipts', nargs='+', type=Path)
    args = parser.parse_args()
    result = dict(status='passed', runs=[analyze(p) for p in args.receipts],
        interpretation='Exact TC-P1 instrumented scheduling diagnostics. Fractions count admitted work, not serial wall fraction or utilization. No timed strength, NPS scaling, tail-idle, cross-worker TT consumption or duplication claim. Repetitions expose scheduling variation; not an inferential competitive sample.')
    with args.output.open('x', encoding='utf-8', newline='\n') as out:
        json.dump(result, out, indent=2); out.write('\n')
