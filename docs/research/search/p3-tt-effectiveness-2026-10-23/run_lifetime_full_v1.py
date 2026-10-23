"""Execute the frozen full-trajectory retention protocol after both clock domains."""
import ctypes
from ctypes import wintypes as w
import importlib.util
import json
import subprocess
import time
from datetime import datetime, timezone
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

for workers in (1,4):
    assert json.loads((HERE/f'development-clock-w{workers}-v1-execution.json').read_text())['status']=='passed'
protocol=json.loads((HERE/'lifetime-full-v1.json').read_text())
prior=json.loads((HERE/protocol['runtimeBinding']).read_text())
assert all(digest(ROOT/p)==sha for p,sha in prior['files'].items())
runtime=HERE/protocol['runtime'];output=HERE/'raw/lifetime-full-v1.jsonl';log=HERE/'raw/lifetime-full-v1.log'
assert not output.exists() and not log.exists()
binding={'protocolSha256':digest(HERE/'lifetime-full-v1.json'),'runnerSha256':digest(__file__),
         'runtimeBindingSha256':digest(HERE/protocol['runtimeBinding']),'files':prior['files'],
         'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()}
write_new(HERE/'lifetime-full-v1-binding.json',binding)
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
         '-Xms256m','-Xmx768m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+';'+str(runtime/'lib/*'),
         protocol['main'],protocol['model'],protocol['lines'],str(output),
         *[str(protocol[k]) for k in ('depth','nodeBudget','startPly','endPly')]]
spec=importlib.util.spec_from_file_location('native',ROOT/'tools/sr036-process-cost.py')
native=importlib.util.module_from_spec(spec);spec.loader.exec_module(native)
class Counters(ctypes.Structure):
    _fields_=[('cb',w.DWORD),('PageFaultCount',w.DWORD)]+[(n,ctypes.c_size_t) for n in (
        'PeakWorkingSetSize','WorkingSetSize','QuotaPeakPagedPoolUsage','QuotaPagedPoolUsage',
        'QuotaPeakNonPagedPoolUsage','QuotaNonPagedPoolUsage','PagefileUsage','PeakPagefileUsage')]
api=ctypes.WinDLL('psapi',use_last_error=True).GetProcessMemoryInfo
api.argtypes=[w.HANDLE,ctypes.POINTER(Counters),w.DWORD];api.restype=w.BOOL
receipt={'status':'running','command':command,'startedUtc':datetime.now(timezone.utc).isoformat(),
         'bindingSha256':digest(HERE/'lifetime-full-v1-binding.json'),'priority':'BelowNormal','nativeAccountingSha256':digest(native.__file__)}
start=time.monotonic()
try:
    with log.open('x',encoding='utf8') as stream:
        process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        try:receipt['exitCode']=process.wait(timeout=1200)
        except BaseException:
            process.kill();process.wait();raise
        finally:
            receipt['nativeLifetime']=native.snapshot(int(process._handle))
            memory=Counters();memory.cb=ctypes.sizeof(memory)
            if not api(int(process._handle),ctypes.byref(memory),ctypes.sizeof(memory)):raise ctypes.WinError(ctypes.get_last_error())
            receipt['peakWorkingSetBytes']=memory.PeakWorkingSetSize
    assert receipt['exitCode']==0 and json.loads(output.read_text().splitlines()[-1])['status']=='passed'
    assert all(digest(ROOT/p)==sha for p,sha in prior['files'].items())
    receipt['status']='passed'
except BaseException as error:
    receipt.update(status='failed',failure=repr(error));raise
finally:
    receipt['wallSeconds']=time.monotonic()-start
    receipt['finishedUtc']=datetime.now(timezone.utc).isoformat()
    receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
    write_new(HERE/'lifetime-full-v1-execution.json',receipt)
print('lifetime-full-v1',receipt['status'],receipt['wallSeconds'],flush=True)
