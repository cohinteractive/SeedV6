"""MW-R001 bounded daytime diagnostics, durable admission and owned process drain.

No scheduler/service and no timed-match authority. Existing Windows Job and PC-use
sampling are reused; all outputs are new, and unresolved admissions block restart.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import time

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-multiworker'
spec = importlib.util.spec_from_file_location('mw_native_support', Path(__file__).with_name('p4-tactical-supervise.py'))
support = importlib.util.module_from_spec(spec)
spec.loader.exec_module(support)


def save(path, value):
    """Single supervised writer; refuse a stale pending temp instead of guessing."""
    path = Path(path)
    temp = path.with_name(path.name + '.pending-write')
    with temp.open('xb') as stream:
        stream.write((json.dumps(value, indent=2) + '\n').encode('utf-8'))
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temp, path)


def run(path):
    path = Path(path)
    p = json.loads(path.read_text(encoding='utf-8'))
    if p['mode'] != 'A' or p['kind'] != 'diagnostic' or p['competitiveGames'] != 0:
        raise ValueError('Only noncompetitive daytime diagnostics are admitted here')
    if p['workers'] not in (2, 4) or not 0 < p['timeoutSeconds'] <= 300:
        raise ValueError('Diagnostic operating bounds')
    for name, digest in p['inputs'].items():
        if support.digest(ROOT / name) != digest:
            raise ValueError('Changed input ' + name)
    ledger_path = OUT / 'budget.json'
    ledger = json.loads(ledger_path.read_text())
    if ledger['pending'] or ledger['ownedProcesses']:
        raise ValueError('Reconcile previous ownership/admission before another run')
    if ledger['consumed']['fixedNodes'] + p['maximumNodes'] > ledger['ceilings']['fixedNodes']:
        raise ValueError('Node ceiling')
    run_dir = OUT / 'raw' / p['id']
    run_dir.mkdir(parents=True, exist_ok=False)
    receipt_path = OUT / (p['id'] + '-execution.json')
    if receipt_path.exists():
        raise FileExistsError(receipt_path)
    output = run_dir / 'observations.jsonl'
    command = [str(ROOT / value[5:]) if value.startswith('ROOT:') else value for value in p['command']]
    command = [str(output) if value == '{output}' else value for value in command]
    receipt = {'id': p['id'], 'status': 'admitted', 'protocol': str(path),
               'protocolSha256': support.digest(path), 'command': command,
               'startedUtc': support.utc().isoformat(), 'maximumNodes': p['maximumNodes'],
               'output': str(output), 'priority': 'below-normal', 'games': 0}
    save(receipt_path, receipt)
    ledger['pending'].append({'id': p['id'], 'maximumNodes': p['maximumNodes'], 'receipt': str(receipt_path)})
    save(ledger_path, ledger)
    admitted_bytes = ledger_path.read_bytes()
    job = support.owned.OwnedJob()
    process = None
    pc = support.PcUse()
    start = time.monotonic()
    error = None
    try:
        initial = pc.sample()
        receipt['preflightEnvironment'] = initial
        save(receipt_path, receipt)
        if initial['freeMemoryGiB'] < 6 or initial['overwatch']:
            raise RuntimeError('PC-use admission guard')
        with (run_dir / 'console.log').open('xb') as log, (run_dir / 'environment.jsonl').open('x') as environment:
            environment.write(json.dumps(initial) + '\n'); environment.flush()
            process = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
                    creationflags=0x4 | 0x4000 | subprocess.CREATE_NO_WINDOW)
            job.assign(process)
            receipt['processId'] = process.pid
            receipt['status'] = 'running'
            save(receipt_path, receipt)
            job.resume_initial_thread(process)
            hot = 0
            while True:
                if (OUT / 'STOP').exists() or time.monotonic() - start >= p['timeoutSeconds']:
                    raise RuntimeError('Owner STOP or diagnostic hard deadline')
                try:
                    code = process.wait(timeout=min(2, max(.01, p['timeoutSeconds'] - (time.monotonic() - start))))
                    break
                except subprocess.TimeoutExpired:
                    sample = pc.sample()
                    environment.write(json.dumps(sample) + '\n'); environment.flush()
                    hot = hot + 1 if (sample['totalCpuPercent'] or 0) >= 80 else 0
                    if sample['freeMemoryGiB'] < 6 or sample['overwatch'] or hot >= 3:
                        raise RuntimeError('PC-use pressure guard')
            if code != 0:
                raise RuntimeError('Diagnostic exit code ' + str(code))
    except BaseException as problem:
        error = repr(problem)
    finally:
        try:
            receipt['drain'] = support.drain(job, process)
        finally:
            job.close()
            if process is not None and process.poll() is None:
                process.kill(); process.wait(timeout=10)
    receipt['wallSeconds'] = time.monotonic() - start
    receipt['endedUtc'] = support.utc().isoformat()
    receipt['error'] = error
    # A verified refusal before process creation is zero work. Once launched,
    # incomplete/lost output conservatively charges the whole reserved node cap.
    # The raw partial file stays untouched and the receipt makes this explicit.
    try:
        rows = [json.loads(line) for line in output.read_text().splitlines()]
        summary = rows[-1]
        actual = sum(row['measurement']['outcome']['nodes'] for row in rows if row['type'] == 'request')
        assert not error and summary['type'] == 'summary' and summary['status'] == 'passed'
        assert summary['activeHelpersAfterClose'] == 0 and summary['nodes'] == actual
        assert summary['requests'] == p['requests'] and 0 <= actual <= p['maximumNodes']
        receipt.update(status='passed', chargedNodes=actual, nodeCharge='verified callback admissions', summary=summary)
    except (OSError, ValueError, KeyError, IndexError, AssertionError):
        if process is None and receipt['drain']['totalOwnedProcesses'] == 0:
            receipt.update(status='refused-before-launch', chargedNodes=0, nodeCharge='verified no process created')
        else:
            receipt.update(status='failed', chargedNodes=p['maximumNodes'], nodeCharge='conservative full reservation')
    receipt['files'] = {str(f): support.digest(f) for f in run_dir.iterdir() if f.is_file()}
    receipt['inputsStable'] = all(support.digest(ROOT / name) == digest for name, digest in p['inputs'].items())
    if not receipt['inputsStable']:
        receipt['status'] = 'failed'
    save(receipt_path, receipt)
    if ledger_path.read_bytes() != admitted_bytes:
        raise RuntimeError('Unexpected budget edit; retain pending admission for manual factual reconciliation')
    ledger['consumed']['fixedNodes'] += receipt['chargedNodes']
    ledger['consumed']['fixedWallSeconds'] += receipt['wallSeconds'] if process is not None else 0
    ledger['pending'] = []
    ledger['receipts'].append(str(receipt_path.relative_to(ROOT)))
    save(ledger_path, ledger)
    print(json.dumps({k: receipt[k] for k in ('id', 'status', 'chargedNodes', 'wallSeconds', 'error', 'drain')}))
    if receipt['status'] != 'passed':
        raise SystemExit(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', required=True)
    run(parser.parse_args().protocol)
