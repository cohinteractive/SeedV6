"""Execute only this frozen development protocol, serially, with durable receipts.

Uses existing Windows native lifetime accounting. No polling, tournament logic,
policy selection or automatic replay. Failed output is preserved and blocks retry
under the same name. All Java processes are directly owned by this subprocess.
"""
import ctypes
from ctypes import wintypes
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[4]
HERE = Path(__file__).resolve().parent


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def write_new(path, value):
    with Path(path).open('x', encoding='utf-8', newline='\n') as out:
        json.dump(value, out, indent=2)
        out.write('\n')


def main():
    protocol = json.loads((HERE / 'diagnostic-v1.json').read_text())
    runtime = HERE / 'raw/runtime-v1'
    binding_path = HERE / 'diagnostic-v1-binding.json'
    if not binding_path.exists():
        shutil.copytree(ROOT / 'build/research/runtime', runtime)
        inputs = [HERE/'diagnostic-v1.json', Path(__file__),
                  ROOT/protocol['model'], ROOT/protocol['corpus'],
                  ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/ConstructionTtStudy.java']
        inputs += sorted(p for p in runtime.rglob('*') if p.is_file())
        binding = {'frozenAtUtc': datetime.now(timezone.utc).isoformat(),
                   'sourceHead': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                   'worktree': subprocess.check_output(['git', 'status', '--short'], cwd=ROOT, text=True),
                   'files': {str(p.relative_to(ROOT)): digest(p) for p in inputs},
                   'runtimeNote': 'build69 application classes unchanged; research instrumentation and metadata explicitly bound'}
        write_new(binding_path, binding)
    binding = json.loads(binding_path.read_text())
    for name, expected in binding['files'].items():
        if digest(ROOT/name) != expected:
            raise ValueError('Frozen input changed: '+name)
    spec = importlib.util.spec_from_file_location('native_cost', ROOT/'tools/sr036-process-cost.py')
    native = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(native)
    class MemoryCounters(ctypes.Structure):
        _fields_ = [('cb', wintypes.DWORD), ('PageFaultCount', wintypes.DWORD)] + [
            (n, ctypes.c_size_t) for n in ('PeakWorkingSetSize', 'WorkingSetSize',
                                         'QuotaPeakPagedPoolUsage', 'QuotaPagedPoolUsage',
                                         'QuotaPeakNonPagedPoolUsage', 'QuotaNonPagedPoolUsage',
                                         'PagefileUsage', 'PeakPagefileUsage')]
    memory_api = ctypes.WinDLL('psapi', use_last_error=True).GetProcessMemoryInfo
    memory_api.argtypes = [wintypes.HANDLE, ctypes.POINTER(MemoryCounters), wintypes.DWORD]
    memory_api.restype = wintypes.BOOL
    selected = set(sys.argv[1:])
    for experiment in protocol['experiments']:
        name = experiment['name']
        if selected and name not in selected:
            continue
        output = HERE/'raw'/f'{name}.jsonl'
        receipt = HERE/f'{name}-execution.json'
        log_path = HERE/'raw'/f'{name}.log'
        if any(p.exists() for p in (output, receipt, log_path)):
            raise ValueError('Refusing replay/overwrite: '+name)
        args = [str(ROOT/protocol['model']), str(ROOT/protocol['corpus']), str(output)]
        args += [str(experiment[k]) for k in ('workers','warmups','rounds','depth','nodeBudget','roots','mode','profiles')]
        command = ['C:/java/jdk-21/bin/java.exe', *protocol['jvm'], '-cp',
                   str(runtime/'classes')+';'+str(runtime/'lib/*'),
                   'com.ohinteractive.seedv6.search.exact.ConstructionTtStudy', *args]
        record = {'experiment':name, 'bindingSha256':digest(binding_path), 'command':command,
                  'startedUtc':datetime.now(timezone.utc).isoformat(), 'priority':'BelowNormal',
                  'nativeAccountingSource':str(ROOT/'tools/sr036-process-cost.py'),
                  'nativeAccountingSha256':digest(ROOT/'tools/sr036-process-cost.py')}
        start = time.monotonic()
        try:
            with log_path.open('x', encoding='utf-8') as log:
                process = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
                                           creationflags=subprocess.CREATE_NO_WINDOW | subprocess.BELOW_NORMAL_PRIORITY_CLASS)
                try:
                    record['exitCode'] = process.wait(timeout=1200)
                except BaseException:
                    process.kill()
                    process.wait()
                    raise
                finally:
                    record['nativeLifetime'] = native.snapshot(int(process._handle))
                    memory = MemoryCounters(); memory.cb = ctypes.sizeof(memory)
                    if not memory_api(int(process._handle), ctypes.byref(memory), ctypes.sizeof(memory)):
                        raise ctypes.WinError(ctypes.get_last_error())
                    record['peakWorkingSetBytes'] = memory.PeakWorkingSetSize
                    record['peakPagefileBytes'] = memory.PeakPagefileUsage
            if record['exitCode']:
                raise RuntimeError('Experiment failed; inspect preserved log: '+name)
            if not output.exists() or json.loads(output.read_text().splitlines()[-1]).get('status') != 'passed':
                raise ValueError('Missing successful Java completion receipt: '+name)
            record['status'] = 'passed'
        except BaseException as error:
            record.update(status='failed', failure=repr(error))
            raise
        finally:
            record['elapsedSeconds'] = time.monotonic()-start
            record['finishedUtc'] = datetime.now(timezone.utc).isoformat()
            record['evidence'] = {str(p.relative_to(ROOT)):digest(p) for p in (output,log_path) if p.exists()}
            write_new(receipt, record)
        print(name, record['status'], round(record['elapsedSeconds'],3), flush=True)


if __name__ == '__main__':
    main()
