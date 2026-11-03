"""Bounded TC development/risk match ownership and PC coexistence; no service."""
import argparse
import ctypes as c
from ctypes import wintypes as w
import datetime as dt
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import time

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-tactical'


def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(filename))
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


owned = module('tc_window_owned', 'tournament-process.py')
screen = module('tc_window_screen', 'p4-tactical.py')
utc = lambda: dt.datetime.now(dt.timezone.utc)
digest = lambda path: hashlib.sha256(Path(path).read_bytes()).hexdigest()


def save(path, value):
    Path(path).write_bytes((json.dumps(value, indent=2) + '\n').encode())


class Memory(c.Structure):
    _fields_ = [('length', w.DWORD), ('load', w.DWORD)] + [
        (key, c.c_ulonglong) for key in ('total', 'available', 'pageTotal',
        'pageAvailable', 'virtualTotal', 'virtualAvailable', 'extendedAvailable')]


class PcUse:
    def __init__(self):
        self.k = c.WinDLL('kernel32', use_last_error=True)
        self.k.GetSystemTimes.argtypes = [c.POINTER(c.c_ulonglong)] * 3
        self.k.GetSystemTimes.restype = w.BOOL
        self.k.GlobalMemoryStatusEx.argtypes = [c.POINTER(Memory)]
        self.k.GlobalMemoryStatusEx.restype = w.BOOL
        self.previous = None
        self.next_game_check = 0
        self.gaming = False

    def sample(self):
        idle, kernel, user = c.c_ulonglong(), c.c_ulonglong(), c.c_ulonglong()
        if not self.k.GetSystemTimes(c.byref(idle), c.byref(kernel), c.byref(user)):
            raise c.WinError(c.get_last_error())
        current = (idle.value, kernel.value + user.value)
        cpu = None
        if self.previous and current[1] > self.previous[1]:
            cpu = 100 * (1 - (current[0] - self.previous[0]) / (current[1] - self.previous[1]))
        self.previous = current
        memory = Memory(); memory.length = c.sizeof(memory)
        if not self.k.GlobalMemoryStatusEx(c.byref(memory)):
            raise c.WinError(c.get_last_error())
        if time.monotonic() >= self.next_game_check:
            result = subprocess.run(['tasklist', '/FI', 'IMAGENAME eq Overwatch.exe', '/FO', 'CSV', '/NH'],
                capture_output=True, text=True, check=True, timeout=5,
                creationflags=subprocess.CREATE_NO_WINDOW)
            self.gaming = 'overwatch.exe' in result.stdout.lower()
            self.next_game_check = time.monotonic() + 10
        return {'utc': utc().isoformat(), 'totalCpuPercent': cpu,
                'freeMemoryGiB': memory.available / 2**30, 'overwatch': self.gaming}


def terminate(job):
    job.k.TerminateJobObject.argtypes = [w.HANDLE, w.UINT]
    job.k.TerminateJobObject.restype = w.BOOL
    if not job.k.TerminateJobObject(job.handle, 1):
        raise c.WinError(c.get_last_error())


def drain(job, process):
    if job.accounting().activeProcesses:
        terminate(job)
    if process is not None:
        process.wait(timeout=10)
    deadline = time.monotonic() + 5
    while job.accounting().activeProcesses and time.monotonic() < deadline:
        time.sleep(.05)
    a = job.accounting()
    if a.activeProcesses:
        raise RuntimeError('Owned descendants did not drain')
    return {'ownedProcessesRemaining': 0, 'processDrained': True,
            'ownedCpuSeconds': (a.user + a.kernel) / 10000000,
            'totalOwnedProcesses': a.totalProcesses}


def self_check(name):
    output = OUT / name
    if output.exists():
        raise FileExistsError('Preserve existing qualification; choose a new --qualification name')
    start = time.monotonic(); pc = PcUse(); first = pc.sample(); time.sleep(.2); sample = pc.sample()
    assert sample['totalCpuPercent'] is not None and 0 <= sample['totalCpuPercent'] <= 100
    assert sample['freeMemoryGiB'] > 0
    job = owned.OwnedJob(); process = None
    try:
        process = subprocess.Popen([sys.executable, '-B', '-c', 'import time; time.sleep(60)'],
            creationflags=0x4 | 0x4000 | subprocess.CREATE_NO_WINDOW)
        job.assign(process); job.resume_initial_thread(process)
        result = drain(job, process)
        assert process.returncode != 0
    finally:
        job.close()
        if process is not None and process.poll() is None:
            process.kill(); process.wait(timeout=10)
    result.update(status='passed', utc=utc().isoformat(), wallSeconds=time.monotonic()-start,
                  toolSha256=digest(__file__), sample=sample,
                  scope='Actual native CPU/memory sampling and owned suspended dummy process termination/drain. No engine or match, no window authorization inferred.')
    save(output, result)
    print(json.dumps(result))


def validate_admission(p, window, ledger, now):
    deadline = dt.datetime.fromisoformat(window['deadlineUtc'])
    assert p['id'] in window['allowedProtocols'] and window['ownerReply']
    assert deadline.tzinfo is not None and (deadline - now).total_seconds() >= p['timeoutSeconds'] + 60
    assert p['supervisorSha256'] == digest(__file__)
    if p['workers'] == [1]:
        assert p['category'] == 'W1 development'
        budget, limit = 'developmentGames', 384
    else:
        assert p['workers'] == [4] and p['category'] == 'W4 competitive risk'
        budget, limit = 'riskGames', 96
    games = p['pairs'] * 2
    assert type(p['pairs']) is int and p['pairs'] > 0
    assert ledger[budget] + games <= limit
    assert ledger['initiatedGames'] + games + 640 <= 1120
    assert ledger['remainingWallSeconds'] >= p['timeoutSeconds'] + 60 + 24000
    return deadline, games, p['workers'][0]


def run(protocol_path, window_path):
    p = json.loads(protocol_path.read_text(encoding='utf-8'))
    window = json.loads(window_path.read_text(encoding='utf-8'))
    ledger = json.loads((OUT / 'resources.json').read_text(encoding='utf-8'))
    deadline, games, workers = validate_admission(p, window, ledger, utc())
    name = p['namePrefix']; receipt = OUT / (name + '-execution.json')
    assert not receipt.exists() and not (OUT / 'raw' / (name + '-w' + str(workers))).exists()
    command = [str(ROOT/'build/research/venv/Scripts/python.exe'), '-B',
               'tools/p4-tactical.py', '--protocol', protocol_path.name, '--workers', str(workers)]
    r = {'status':'running', 'startedUtc':utc().isoformat(), 'protocol':protocol_path.name,
         'protocolSha256':digest(protocol_path), 'window':str(window_path),
         'windowSha256':digest(window_path), 'command':command, 'reservedGamesPending':games,
         'category':'charged controlled-window ' + p['category'] + ' and audit', 'supervisorSha256':digest(__file__)}
    start = time.monotonic(); job = owned.OwnedJob(); process = None; save(receipt, r)
    stop_file = OUT / 'raw' / (name + '.stop')
    try:
        if stop_file.exists():
            raise RuntimeError('Owner stop present before admission')
        pc = PcUse(); high = 0
        with (OUT/'raw'/(name+'-pc-use.jsonl')).open('x', encoding='utf-8', newline='\n') as samples:
            def sample():
                value = pc.sample(); samples.write(json.dumps(value)+'\n'); samples.flush(); return value
            sample()
            for _ in range(10):
                time.sleep(2); value = sample()
                if value['overwatch'] or value['freeMemoryGiB'] < 12 or value['totalCpuPercent'] > 35:
                    raise RuntimeError('PC admission failed before any match game')
            with (OUT/'raw'/(name+'-supervisor.log')).open('x', encoding='utf-8') as log:
                process = subprocess.Popen(command, cwd=ROOT, env=screen.b.clean_environment(),
                    stdout=log, stderr=subprocess.STDOUT,
                    creationflags=0x4 | 0x4000 | subprocess.CREATE_NO_WINDOW)
                job.assign(process); r['pid'] = process.pid; save(receipt, r); job.resume_initial_thread(process)
                while True:
                    remaining = min((deadline-utc()).total_seconds(), p['timeoutSeconds']+45-(time.monotonic()-start))
                    if stop_file.exists() or remaining <= 0:
                        raise RuntimeError('Owner stop, agreed window deadline, or wall cap')
                    try: code = process.wait(timeout=min(2, remaining)); break
                    except subprocess.TimeoutExpired: pass
                    value = sample(); high = high + 1 if value['totalCpuPercent'] >= 75 else 0
                    if value['overwatch'] or value['freeMemoryGiB'] < 8 or high >= 3:
                        raise RuntimeError('PC interference guard: '+json.dumps(value))
                    if stop_file.exists() or utc() >= deadline or time.monotonic()-start > p['timeoutSeconds']+45:
                        raise RuntimeError('Owner stop, agreed window deadline, or wall cap')
                r['exitCode'] = code
                # Observe natural draining before classifying successful exit.
                until = time.monotonic()+5
                while job.accounting().activeProcesses and time.monotonic() < until: time.sleep(.05)
                if code != 0 or job.accounting().activeProcesses:
                    raise RuntimeError('Match failed or descendants did not naturally drain')
                result = json.loads((OUT/'raw'/(name+'-w'+str(workers))/'run.json').read_text(encoding='utf-8'))
                assert result['status']=='passed' and result['engineHandlesDrained']
                r.update(status='passed', gamesInitiated=games)
    except BaseException as error:
        r.update(status='failed', failure=repr(error),
                 gamesInitiated=0 if process is None else 'Reconcile actual starts before further admission')
        raise
    finally:
        try: r.update(drain(job, process))
        except BaseException as error:
            r.update(status='failed', drainFailure=repr(error)); raise
        finally:
            job.close()
            if process is not None and process.poll() is None: process.kill(); process.wait(timeout=10)
            r.update(endedUtc=utc().isoformat(), wallSeconds=time.monotonic()-start)
            save(receipt, r); print(json.dumps(r), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-check', action='store_true')
    parser.add_argument('--qualification', default='window-supervisor-v1-qualification.json')
    parser.add_argument('--protocol'); parser.add_argument('--window', type=Path)
    args = parser.parse_args()
    if args.self_check: self_check(args.qualification)
    else:
        if not args.protocol or not args.window: parser.error('--protocol and --window required')
        run(OUT/args.protocol, args.window)
