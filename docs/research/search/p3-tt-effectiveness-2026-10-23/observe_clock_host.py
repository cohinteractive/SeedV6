"""Read-only host/load window ending on validated game-engine process exits.

No outcomes, scheduling changes, periodic workload probes or game decisions.
This is local experiment evidence, not execution-history/journal infrastructure.
"""
import ctypes
from ctypes import wintypes as w
from datetime import datetime, timezone
import importlib.util
import json
import os
import sys
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

name=sys.argv[1];cohortPath=HERE/'raw'/name/'process-cost.json'
cohort=json.loads(cohortPath.read_text());assert cohort['status']=='attached'
startPath=HERE/'raw'/f'{name}-host-start.json';endPath=HERE/'raw'/f'{name}-host-end.json'
assert not startPath.exists() and not endPath.exists()
spec=importlib.util.spec_from_file_location('native',ROOT/'tools/sr036-process-cost.py')
n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n);k=n.kernel
k.GetSystemTimes.argtypes=[ctypes.POINTER(w.FILETIME)]*3
k.GetPriorityClass.argtypes=[w.HANDLE];k.GetPriorityClass.restype=w.DWORD
class Memory(ctypes.Structure):
    _fields_=[('length',w.DWORD),('load',w.DWORD)]+[(x,ctypes.c_ulonglong) for x in (
        'totalPhys','availPhys','totalPagefile','availPagefile','totalVirtual','availVirtual','availExtendedVirtual')]
k.GlobalMemoryStatusEx.argtypes=[ctypes.POINTER(Memory)];k.GlobalMemoryStatusEx.restype=w.BOOL
def host():
    t=[w.FILETIME() for _ in range(3)];assert k.GetSystemTimes(*[ctypes.byref(x) for x in t])
    memory=Memory();memory.length=ctypes.sizeof(memory);assert k.GlobalMemoryStatusEx(ctypes.byref(memory))
    return {'utc':datetime.now(timezone.utc).isoformat(),'ticks100ns':dict(zip(('idle','kernelIncludingIdle','user'),
        [(x.dwHighDateTime<<32)+x.dwLowDateTime for x in t])),
        'physicalMemoryLoadPercent':memory.load,'availablePhysicalBytes':memory.availPhys}
handles=[]
try:
    engines={};priorities={}
    for row in cohort['processes']:
        h=k.OpenProcess(0x1000|0x100000,False,row['pid']);assert h;handles.append(h)
        observed=n.snapshot(h)
        assert observed['creationTicks']==row['atAttachment']['creationTicks'] and observed['exitCode']==259
        engines[row['actor']]=observed;priorities[row['actor']]=k.GetPriorityClass(h)
    start={'host':host(),'engines':engines,'priorityClass':priorities,'logicalCpus':os.cpu_count(),
           'observerSha256':digest(__file__),'cohortAttachmentSha256':digest(cohortPath)}
    assert all(value==0x4000 for value in priorities.values()),'Engines not BelowNormal'
    write_new(startPath,start)
    array=(w.HANDLE*len(handles))(*handles)
    while True:
        result=k.WaitForMultipleObjects(len(handles),array,True,60000)
        if result==0:break
        assert result==258
    end=host();final={row['actor']:n.snapshot(h) for row,h in zip(cohort['processes'],handles)}
    assert all(value['exitCode']==0 for value in final.values())
    delta={key:end['ticks100ns'][key]-start['host']['ticks100ns'][key] for key in end['ticks100ns']}
    total=delta['kernelIncludingIdle']+delta['user'];busy=(total-delta['idle'])/1e7
    engine=sum(final[a]['totalCpuSeconds']-engines[a]['totalCpuSeconds'] for a in final)
    report={'status':'passed','startPath':str(startPath.relative_to(HERE)),'startSha256':digest(startPath),
        'host':end,'engines':final,'aggregateNonIdleFraction':1-delta['idle']/total,
        'globalBusyCpuSeconds':busy,'engineCpuSecondsInWindow':engine,'otherBusyCpuSecondsInWindow':busy-engine,
        'limits':'Observed interval starts after process attachment and ends at engine exits. Aggregate idle/memory capacity and BelowNormal class do not establish isolation, affinity or immunity to local contention. No game results inspected.'}
    write_new(endPath,report);print(name,'host window passed',report['aggregateNonIdleFraction'],flush=True)
finally:
    for h in handles:k.CloseHandle(h)
