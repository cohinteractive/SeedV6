"""Small Win32 job wrapper: atomic job assignment during process creation.

No process-name/PID-tree killing, no breakaway, no service. Closing the supervisor's
last job handle kills its owned tree, including after a supervisor crash.
"""
import ctypes as C
from ctypes import wintypes as W
import os
from pathlib import Path
import shutil
import subprocess

if os.name != 'nt':
    raise RuntimeError('NNUE compute supervision currently requires Windows')

k = C.WinDLL('kernel32', use_last_error=True)
SIZE = C.c_size_t


class BasicLimit(C.Structure):
    _fields_ = [('per_process', C.c_int64), ('per_job', C.c_int64), ('flags', W.DWORD),
                ('min_ws', SIZE), ('max_ws', SIZE), ('active_limit', W.DWORD),
                ('affinity', SIZE), ('priority', W.DWORD), ('scheduling', W.DWORD)]


class IO(C.Structure):
    _fields_ = [(name, C.c_uint64) for name in ('read_ops', 'write_ops', 'other_ops', 'read', 'write', 'other')]


class ExtendedLimit(C.Structure):
    _fields_ = [('basic', BasicLimit), ('io', IO), ('process_memory', SIZE),
                ('job_memory', SIZE), ('peak_process', SIZE), ('peak_job', SIZE)]


class Accounting(C.Structure):
    _fields_ = [(name, C.c_int64) for name in ('user', 'kernel', 'period_user', 'period_kernel')] + [
        (name, W.DWORD) for name in ('faults', 'total', 'active', 'terminated')]


class Startup(C.Structure):
    _fields_ = [('cb', W.DWORD), ('reserved', W.LPWSTR), ('desktop', W.LPWSTR), ('title', W.LPWSTR)] + [
        (name, W.DWORD) for name in ('x', 'y', 'xsize', 'ysize', 'xchars', 'ychars', 'fill', 'flags')] + [
        ('show', W.WORD), ('reserved_size', W.WORD), ('reserved_bytes', C.POINTER(W.BYTE)),
        ('stdin', W.HANDLE), ('stdout', W.HANDLE), ('stderr', W.HANDLE)]


class ProcessInfo(C.Structure):
    _fields_ = [('process', W.HANDLE), ('thread', W.HANDLE), ('pid', W.DWORD), ('tid', W.DWORD)]


class StartupEx(C.Structure):
    _fields_ = [('startup', Startup), ('attributes', C.c_void_p)]


def signature(name, args, result=W.BOOL):
    fn = getattr(k, name); fn.argtypes = args; fn.restype = result
    return fn


close = signature('CloseHandle', [W.HANDLE])
create_job = signature('CreateJobObjectW', [C.c_void_p, W.LPCWSTR], W.HANDLE)
open_job = signature('OpenJobObjectW', [W.DWORD, W.BOOL, W.LPCWSTR], W.HANDLE)
set_job = signature('SetInformationJobObject', [W.HANDLE, C.c_int, C.c_void_p, W.DWORD])
query_job = signature('QueryInformationJobObject', [W.HANDLE, C.c_int, C.c_void_p, W.DWORD, C.c_void_p])
terminate_job = signature('TerminateJobObject', [W.HANDLE, W.UINT])
initialize_attributes = signature('InitializeProcThreadAttributeList', [C.c_void_p, W.DWORD, W.DWORD, C.POINTER(SIZE)])
update_attribute = signature('UpdateProcThreadAttribute', [C.c_void_p, W.DWORD, SIZE, C.c_void_p, SIZE, C.c_void_p, C.c_void_p])
delete_attributes = signature('DeleteProcThreadAttributeList', [C.c_void_p], None)
create_process = signature('CreateProcessW', [W.LPCWSTR, W.LPWSTR, C.c_void_p, C.c_void_p,
    W.BOOL, W.DWORD, C.c_void_p, W.LPCWSTR, C.POINTER(Startup), C.POINTER(ProcessInfo)])
resume_thread = signature('ResumeThread', [W.HANDLE], W.DWORD)
terminate_process = signature('TerminateProcess', [W.HANDLE, W.UINT])
wait = signature('WaitForSingleObject', [W.HANDLE, W.DWORD], W.DWORD)
exit_code = signature('GetExitCodeProcess', [W.HANDLE, C.POINTER(W.DWORD)])
open_process = signature('OpenProcess', [W.DWORD, W.BOOL, W.DWORD], W.HANDLE)
process_times = signature('GetProcessTimes', [W.HANDLE] + [C.POINTER(W.FILETIME)] * 4)


def check(value):
    if not value: raise C.WinError(C.get_last_error())
    return value


def identity(pid):
    handle = open_process(0x1000 | 0x100000, False, pid)  # query-limited + synchronize
    if not handle:
        error = C.get_last_error()
        if error == 87: return None  # no such process
        raise C.WinError(error)
    try:
        if wait(handle, 0) == 0: return None
        times = [W.FILETIME() for _ in range(4)]
        check(process_times(handle, *(C.byref(t) for t in times)))
        return {'pid': pid, 'created': (times[0].dwHighDateTime << 32) | times[0].dwLowDateTime}
    finally: close(handle)


def active_count(handle):
    info = Accounting()
    check(query_job(handle, 1, C.byref(info), C.sizeof(info), None))
    return info.active


def inspect(name, stop=False):
    handle = open_job(4 | (8 if stop else 0), False, name)  # query / terminate
    if not handle:
        if C.get_last_error() == 2: return 0
        raise C.WinError(C.get_last_error())
    try:
        if stop: check(terminate_job(handle, 75))
        return active_count(handle)
    finally: close(handle)


class Job:
    def __init__(self, name):
        self.handle = check(create_job(None, name))
        self.process = None
        try:
            limits = ExtendedLimit()
            limits.basic.flags = 0x2000 | 0x20  # KILL_ON_JOB_CLOSE + PRIORITY_CLASS
            limits.basic.priority = 0x4000  # BELOW_NORMAL_PRIORITY_CLASS
            check(set_job(self.handle, 9, C.byref(limits), C.sizeof(limits)))
        except BaseException:
            close(self.handle); self.handle = None; raise

    def start(self, command, log, cwd, env):
        import msvcrt
        if self.process: raise RuntimeError('Previous child handle not closed')
        executable = shutil.which(command[0])
        if not executable: raise FileNotFoundError(command[0])
        if Path(executable).suffix.lower() in ('.bat', '.cmd'):
            raise ValueError('Use an explicit cmd.exe or powershell.exe for a batch script')
        line = C.create_unicode_buffer(subprocess.list2cmdline([executable, *command[1:]]))
        environment = C.create_unicode_buffer('\0'.join(f'{key}={value}' for key, value in
            sorted(env.items(), key=lambda item: item[0].upper())) + '\0\0')
        pi = ProcessInfo(); extended = StartupEx(); si = extended.startup
        si.cb = C.sizeof(extended); si.flags = 0x100
        Path(log).parent.mkdir(parents=True, exist_ok=True)
        with open(log, 'xb', buffering=0) as output, open(os.devnull, 'rb') as empty:
            handles = [msvcrt.get_osfhandle(f.fileno()) for f in (empty, output)]
            for handle in handles: os.set_handle_inheritable(handle, True)
            si.stdin, si.stdout, si.stderr = handles[0], handles[1], handles[1]
            size = SIZE()
            initialize_attributes(None, 2, 0, C.byref(size))  # expected size-query failure
            attributes = C.create_string_buffer(size.value)
            initialized = False
            try:
                check(initialize_attributes(attributes, 2, 0, C.byref(size))); initialized = True
                extended.attributes = C.cast(attributes, C.c_void_p)
                jobs = (W.HANDLE * 1)(self.handle)
                inherited = (W.HANDLE * 2)(*handles)
                # Windows 10+ JOB_LIST makes ownership atomic even if the supervisor
                # dies immediately inside/after CreateProcessW, before ResumeThread.
                check(update_attribute(attributes, 0, 0x2000D, jobs, C.sizeof(jobs), None, None))
                check(update_attribute(attributes, 0, 0x20002, inherited, C.sizeof(inherited), None, None))
                check(create_process(executable, line, None, None, True,
                    0x4 | 0x400 | 0x08000000 | 0x4000 | 0x80000, environment, str(cwd), C.byref(si), C.byref(pi)))
            finally:
                if initialized: delete_attributes(attributes)
                for handle in handles: os.set_handle_inheritable(handle, False)
        try:
            if resume_thread(pi.thread) == 0xFFFFFFFF: raise C.WinError(C.get_last_error())
            self.process = pi.process
            return pi.pid
        except BaseException:
            terminate_process(pi.process, 75); wait(pi.process, 5000); close(pi.process)
            raise
        finally: close(pi.thread)

    def poll(self):
        if wait(self.process, 0) == 0x102: return None
        code = W.DWORD(); check(exit_code(self.process, C.byref(code)))
        close(self.process); self.process = None
        return code.value

    def stop(self): check(terminate_job(self.handle, 75))
    def count(self): return active_count(self.handle)

    def close(self):
        if self.handle:
            close(self.handle); self.handle = None
        if self.process:
            close(self.process); self.process = None
