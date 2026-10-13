"""Passive Windows lifetime CPU accounting for explicitly bound experiment processes.

Opens read/synchronize handles only; never signals, suspends or changes a process.
Native lifetime counters include startup, warm-up and JVM background threads.
This is experiment evidence, not repository/global execution bookkeeping.
"""
import argparse
import ctypes as c
from ctypes import wintypes as w
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path


kernel=c.WinDLL('kernel32',use_last_error=True)
kernel.OpenProcess.argtypes=[w.DWORD,w.BOOL,w.DWORD];kernel.OpenProcess.restype=w.HANDLE
kernel.GetProcessTimes.argtypes=[w.HANDLE,*([c.POINTER(w.FILETIME)]*4)];kernel.GetProcessTimes.restype=w.BOOL
kernel.GetExitCodeProcess.argtypes=[w.HANDLE,c.POINTER(w.DWORD)];kernel.GetExitCodeProcess.restype=w.BOOL
kernel.WaitForMultipleObjects.argtypes=[w.DWORD,c.POINTER(w.HANDLE),w.BOOL,w.DWORD];kernel.WaitForMultipleObjects.restype=w.DWORD
kernel.CloseHandle.argtypes=[w.HANDLE];kernel.CloseHandle.restype=w.BOOL


def digest(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def stamp(ticks):return datetime.fromtimestamp(ticks/10_000_000-11_644_473_600,timezone.utc).isoformat(timespec='microseconds')


def snapshot(handle):
    times=[w.FILETIME() for _ in range(4)];code=w.DWORD()
    if not kernel.GetProcessTimes(handle,*[c.byref(x) for x in times]):raise c.WinError(c.get_last_error())
    if not kernel.GetExitCodeProcess(handle,c.byref(code)):raise c.WinError(c.get_last_error())
    ticks=[(x.dwHighDateTime<<32)|x.dwLowDateTime for x in times]
    return {'creationTicks':ticks[0],'createdUtc':stamp(ticks[0]),'exitCode':code.value,
            'exitedUtc':stamp(ticks[1]) if code.value!=259 else None,
            'kernelCpuSeconds':ticks[2]/10_000_000,'userCpuSeconds':ticks[3]/10_000_000,
            'totalCpuSeconds':(ticks[2]+ticks[3])/10_000_000}


def main(a):
    output=Path(a.out)
    if output.exists():raise ValueError('Never overwrite an observation')
    binding=json.loads(Path(a.binding).read_text(encoding='utf-8-sig'))
    report={'status':'attached','observedAtUtc':datetime.now(timezone.utc).isoformat(),
            'binding':binding,'observerSha256':digest(__file__),'bindingSha256':digest(a.binding),
            'source':'https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-getprocesstimes',
            'limits':'Opened during the match. Native cumulative counters still cover process creation through exit, including startup/warmup/GC/JIT; they are not game-clock-only CPU. Only these bound game JVMs and Fastchess are measured; adapter/preflight/audit CPU is excluded. No strength or partial game outcomes are inspected.'}
    handles=[]
    try:
        assert len(binding['processes'])==3
        assert {p['role'] for p in binding['processes']}=={'baseline','candidate','fastchess'}
        report['atAttachment']={}
        for p in binding['processes']:
            handle=kernel.OpenProcess(0x1000|0x100000,False,p['pid'])
            if not handle:raise c.WinError(c.get_last_error())
            handles.append(handle);value=snapshot(handle)
            observed=datetime.fromisoformat(value['createdUtc']);expected=datetime.fromisoformat(p['createdUtc'].replace('Z','+00:00'))
            assert abs((observed-expected).total_seconds())<.01,'PID creation identity changed'
            assert value['exitCode']==259,'Process already exited before attachment'
            report['atAttachment'][p['role']]=value
        output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
        array=(w.HANDLE*len(handles))(*handles)
        while True:
            result=kernel.WaitForMultipleObjects(len(handles),array,True,60000)
            if result==0:break
            if result!=258:raise c.WinError(c.get_last_error())
        report['lifetime']={p['role']:snapshot(h) for p,h in zip(binding['processes'],handles)}
        assert all(x['exitCode']==0 for x in report['lifetime'].values())
        assert report['observerSha256']==digest(__file__) and report['bindingSha256']==digest(a.binding)
        report.update(status='passed',completedAtUtc=datetime.now(timezone.utc).isoformat(),inputsStable=True)
        print(json.dumps(report['lifetime'],indent=2))
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        for handle in handles:kernel.CloseHandle(handle)
        output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--binding',required=True);p.add_argument('--out',required=True)
    main(p.parse_args())
