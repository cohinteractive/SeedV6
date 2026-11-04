"""MW-R001 bounded daytime diagnostics, durable admission and owned process drain.

No scheduler/service and no timed-match authority. Existing Windows Job and PC-use
sampling are reused; all outputs are new, and unresolved admissions block restart.
"""
import argparse
import ctypes as c
from ctypes import wintypes as w
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


class ResourcePressure(RuntimeError):
    """Temporary admission/continuation condition, not experiment success or closeout."""


class PcUse(support.PcUse):
    """Read only resource/foreground identity; no UI automation or responsiveness claim."""
    def sample(self):
        sample = super().sample()
        user = c.WinDLL('user32', use_last_error=True)
        user.GetForegroundWindow.restype = w.HWND
        user.GetWindowThreadProcessId.argtypes = [w.HWND, c.POINTER(w.DWORD)]
        pid = w.DWORD()
        user.GetWindowThreadProcessId(user.GetForegroundWindow(), c.byref(pid))
        self.k.OpenProcess.argtypes = [w.DWORD, w.BOOL, w.DWORD]
        self.k.OpenProcess.restype = w.HANDLE
        self.k.QueryFullProcessImageNameW.argtypes = [w.HANDLE, w.DWORD, w.LPWSTR, c.POINTER(w.DWORD)]
        self.k.QueryFullProcessImageNameW.restype = w.BOOL
        self.k.CloseHandle.argtypes = [w.HANDLE]
        name = None
        handle = self.k.OpenProcess(0x1000, False, pid.value) if pid.value else None
        if handle:
            try:
                size = w.DWORD(32768); text = c.create_unicode_buffer(size.value)
                if self.k.QueryFullProcessImageNameW(handle, 0, text, c.byref(size)):
                    name = Path(text.value).name
            finally:
                self.k.CloseHandle(handle)
        sample.update(logicalProcessors=os.cpu_count(), foregroundApplication=name,
                      demandingForeground=(name or '').lower() == 'overwatch.exe')
        return sample


def resource_decision(sample, workers, resources, *, running=False, idle=False):
    """One bounded observation, never a wait/retry policy. Estimates are explicit."""
    peak, reserve = resources['estimatedPeakGiB'], resources['personalReserveGiB']
    if not .75 <= peak <= 2 or not 2 <= reserve <= 8:
        raise ValueError('Unqualified diagnostic memory envelope')
    logical = sample.get('logicalProcessors') or 0
    cpu = sample.get('totalCpuPercent')
    needed = reserve if running else reserve + peak
    reasons = []
    if sample['freeMemoryGiB'] < needed:
        reasons.append(f"available memory {sample['freeMemoryGiB']:.2f} GiB below {needed:.2f} GiB envelope")
    if sample.get('demandingForeground'):
        reasons.append('Overwatch is the foreground workload; interactive latency takes priority')
    elif sample.get('overwatch') and sample.get('foregroundApplication') is None:
        reasons.append('Overwatch present and foreground identity unavailable')
    if logical < workers or logical < 1:
        reasons.append('insufficient verified logical processors')
    if cpu is None:
        reasons.append('CPU interval unavailable')
    elif not running and logical > 0 and cpu + 100 * workers / logical > (90 if idle else 70):
        reasons.append('existing CPU load plus requested workers exceeds planning envelope')
    elif running and cpu >= (99 if idle else 95):
        reasons.append('critical total CPU contention')
    return {'admissible': not reasons, 'reasons': reasons, 'requiredAvailableGiB': needed,
            'estimatedPeakGiB': peak, 'personalReserveGiB': reserve,
            'projectedCpuPercent': None if cpu is None or not logical else cpu + (0 if running else 100 * workers / logical),
            'scope': 'Resource/foreground guard, not measured input latency or thermal qualification'}


def continuation_action(decision, alternative_work=()):
    if decision['admissible']:
        return 'proceed'
    return 'continue_low_impact_work' if alternative_work else 'await_user_input'


def peak_memory(job):
    limits = support.owned.Limits()
    if not job.k.QueryInformationJobObject(job.handle, 9, c.byref(limits), c.sizeof(limits), None):
        raise c.WinError(c.get_last_error())
    return limits.peakJobMemory / 2**30


def save(path, value):
    """Single supervised writer; refuse a stale pending temp instead of guessing."""
    path = Path(path)
    temp = path.with_name(path.name + '.pending-write')
    with temp.open('xb') as stream:
        stream.write((json.dumps(value, indent=2) + '\n').encode('utf-8'))
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temp, path)


def run(path, alternative_work=()):
    path = Path(path)
    p = json.loads(path.read_text(encoding='utf-8'))
    if p['mode'] != 'A' or p['kind'] != 'diagnostic' or p['competitiveGames'] != 0:
        raise ValueError('Only noncompetitive daytime diagnostics are admitted here')
    if p['workers'] not in (2, 4) or not 0 < p['timeoutSeconds'] <= 300:
        raise ValueError('Diagnostic operating bounds')
    if type(p['maximumNodes']) is not int or p['maximumNodes'] <= 0 or type(p['requests']) is not int or p['requests'] <= 0:
        raise ValueError('Invalid diagnostic work reservation')
    resources = p['resources']
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
    pc = PcUse()
    start = time.monotonic()
    error = None
    pressure = None
    peak = 0
    try:
        pc.sample()
        time.sleep(.5)  # One CPU interval, not an availability-poll/retry loop.
        initial = pc.sample()
        receipt['preflightEnvironment'] = initial
        decision = resource_decision(initial, p['workers'], resources)
        if (OUT / 'STOP').exists():
            decision['admissible'] = False
            decision['reasons'].append('Owner STOP file is present')
        receipt['resourceDecision'] = decision
        save(receipt_path, receipt)
        if not decision['admissible']:
            pressure = decision
            raise ResourcePressure('; '.join(decision['reasons']))
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
                if (OUT / 'STOP').exists():
                    pressure = {'admissible': False, 'reasons': ['Owner STOP file is present']}
                    raise ResourcePressure('Owner STOP')
                if time.monotonic() - start >= p['timeoutSeconds']:
                    raise RuntimeError('Diagnostic hard deadline')
                try:
                    code = process.wait(timeout=min(2, max(.01, p['timeoutSeconds'] - (time.monotonic() - start))))
                    break
                except subprocess.TimeoutExpired:
                    sample = pc.sample()
                    peak = max(peak, peak_memory(job))
                    sample['peakOwnedCommittedGiB'] = peak
                    environment.write(json.dumps(sample) + '\n'); environment.flush()
                    hot = hot + 1 if (sample['totalCpuPercent'] or 0) >= 80 else 0
                    decision = resource_decision(sample, p['workers'], resources, running=True)
                    if hot >= 3:
                        decision['reasons'].append('total CPU >=80% for three observations')
                    if peak > resources['estimatedPeakGiB']:
                        decision['reasons'].append('owned committed memory exceeded planned peak')
                    decision['admissible'] = not decision['reasons']
                    if not decision['admissible']:
                        pressure = decision
                        raise ResourcePressure('; '.join(decision['reasons']))
            if code != 0:
                raise RuntimeError('Diagnostic exit code ' + str(code))
    except BaseException as problem:
        error = repr(problem)
    finally:
        try:
            peak = max(peak, peak_memory(job))
            receipt['drain'] = support.drain(job, process)
        finally:
            job.close()
            if process is not None and process.poll() is None:
                process.kill(); process.wait(timeout=10)
    receipt['wallSeconds'] = time.monotonic() - start
    receipt['endedUtc'] = support.utc().isoformat()
    receipt['error'] = error
    receipt['peakOwnedCommittedGiB'] = peak
    receipt['temporaryResourceBlocker'] = pressure
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
    next_action = continuation_action(pressure, alternative_work) if pressure else ('continue_research' if receipt['status'] == 'passed' else 'investigate_failure')
    save(OUT / 'resource-state.json', {
        'status': next_action, 'receipt': str(receipt_path.relative_to(ROOT)),
        'blocker': pressure, 'ownedProcessesRemaining': 0,
        'alternativeWork': list(alternative_work), 'resumeProtocolTemplate': str(path),
        'resumeRequiresNewRunId': True, 'resumeState': 'docs/research/search/p4-multiworker/STATE.md',
        'pendingHumanAction': ('Confirm the stated PC condition has cleared, or state what must change; then recheck once.' if next_action == 'await_user_input' else None),
        'clientBoundary': 'Repository code records disposition only. Agent must ask via available client input tool when needed. No interactive pause, future execution, or client liveness is created here.'})
    print(json.dumps({k: receipt[k] for k in ('id', 'status', 'chargedNodes', 'wallSeconds', 'error', 'drain')}))
    if receipt['status'] != 'passed':
        raise SystemExit(75 if pressure else 1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', required=True)
    parser.add_argument('--alternative-work', action='append', default=[])
    args = parser.parse_args()
    run(args.protocol, args.alternative_work)
