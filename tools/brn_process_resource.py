"""Bounded owned-child execution and observational process-memory counters for research."""
import ctypes
import os
import subprocess
import time


class WindowsMemory:
    """Peak counters are OS-maintained; final sampling coverage is reported explicitly."""

    def __init__(self):
        self.report = {'method': 'Windows GetProcessMemoryInfo PROCESS_MEMORY_COUNTERS_EX',
                       'samples': 0, 'readFailures': 0,
                       'coverage': 'Observed OS peak counters through the last successful sample; includes JVM/data/checkpoint allocations, not evaluator-only memory'}
        if os.name != 'nt':
            self.report['unavailable'] = 'Windows-specific counters; no estimate substituted'
            self.query = None
            return

        class Counters(ctypes.Structure):
            _fields_ = [('cb', ctypes.c_ulong), ('PageFaultCount', ctypes.c_ulong)] + [
                (name, ctypes.c_size_t) for name in [
                    'PeakWorkingSetSize', 'WorkingSetSize', 'QuotaPeakPagedPoolUsage',
                    'QuotaPagedPoolUsage', 'QuotaPeakNonPagedPoolUsage', 'QuotaNonPagedPoolUsage',
                    'PagefileUsage', 'PeakPagefileUsage', 'PrivateUsage']]

        self.counters = Counters()
        self.counters.cb = ctypes.sizeof(Counters)
        self.query = ctypes.WinDLL('psapi', use_last_error=True).GetProcessMemoryInfo
        self.query.argtypes = [ctypes.c_void_p, ctypes.POINTER(Counters), ctypes.c_ulong]
        self.query.restype = ctypes.c_int

    def sample(self, process, elapsed):
        if self.query is None:
            return False
        if not self.query(int(process._handle), ctypes.byref(self.counters), ctypes.sizeof(self.counters)):
            self.report['readFailures'] += 1
            self.report['lastWinError'] = ctypes.get_last_error()
            return False
        self.report['samples'] += 1
        self.report['lastSampleSeconds'] = elapsed
        for field, source in [('peakWorkingSetBytes', 'PeakWorkingSetSize'),
                              ('peakPrivateCommitBytes', 'PeakPagefileUsage'),
                              ('maximumObservedPrivateCommitBytes', 'PrivateUsage')]:
            self.report[field] = max(self.report.get(field, 0), getattr(self.counters, source))
        return True


def execute(command, output, timeout):
    """Only the child created here can be terminated; no process discovery or external handles."""
    memory = WindowsMemory()
    started = time.monotonic()
    with subprocess.Popen(command, stdout=output, stderr=subprocess.STDOUT) as process:
        timed_out = False
        try:
            while True:
                elapsed = time.monotonic() - started
                memory.sample(process, elapsed)
                if elapsed >= timeout:
                    timed_out = True
                    process.kill()
                    process.wait()
                    break
                try:
                    process.wait(timeout=min(1, timeout - elapsed))
                    break
                except subprocess.TimeoutExpired:
                    pass
        except BaseException:
            if process.poll() is None:
                process.kill()
            process.wait()
            raise
        memory.report['postExitSampleSucceeded'] = memory.sample(process, time.monotonic() - started)
        return {'status': 'timeout' if timed_out else 'completed' if process.returncode == 0 else 'failed',
                'exitCode': process.returncode, 'processMemory': memory.report}
