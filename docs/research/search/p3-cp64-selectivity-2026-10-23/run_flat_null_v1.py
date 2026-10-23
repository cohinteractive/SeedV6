"""Execute frozen diagnostics serially; reuse validated native process accounting.

Every output is exclusive. No replay, tuning, outcome-dependent stopping or promotion.
"""
import ctypes
from ctypes import wintypes
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[4]
HERE = Path(__file__).resolve().parent
def digest(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def write_new(path, value):
    with Path(path).open('x', encoding='utf8', newline='\n') as out:
        json.dump(value, out, indent=2); out.write('\n')

def main():
    validation=json.loads((HERE/'flat-null-validation-v3-summary.json').read_text())
    assert validation['exitCode']==0 and validation['totals']['failures']==validation['totals']['errors']==0
    protocol=json.loads((HERE/'flat-null-v1.json').read_text())
    runtime=HERE/'raw/runtime-v4'; binding_path=HERE/'flat-null-v1-binding.json'
    if not binding_path.exists():
        shutil.copytree(ROOT/'build/research/runtime',runtime)
        files=[HERE/'flat-null-v1.json',Path(__file__),ROOT/protocol['model'],
               ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64SelectivityLab.java',
               ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/ConstructionTtAdaptiveStudy.java',
               ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/ConstructionTtStudy.java']
        files+=sorted({ROOT/e['corpus'] for e in protocol['experiments']})
        files+=sorted(p for p in runtime.rglob('*') if p.is_file())
        write_new(binding_path,{'frozenAtUtc':datetime.now(timezone.utc).isoformat(),
            'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
            'worktree':subprocess.check_output(['git','status','--short'],cwd=ROOT,text=True),
            'files':{str(p.relative_to(ROOT)):digest(p) for p in files}})
    binding=json.loads(binding_path.read_text())
    for p,sha in binding['files'].items(): assert digest(ROOT/p)==sha,p
    spec=importlib.util.spec_from_file_location('native_cost',ROOT/'tools/sr036-process-cost.py')
    native=importlib.util.module_from_spec(spec);spec.loader.exec_module(native)
    class Memory(ctypes.Structure):
        _fields_=[('cb',wintypes.DWORD),('PageFaultCount',wintypes.DWORD)]+[
            (n,ctypes.c_size_t) for n in ('PeakWorkingSetSize','WorkingSetSize','QuotaPeakPagedPoolUsage',
            'QuotaPagedPoolUsage','QuotaPeakNonPagedPoolUsage','QuotaNonPagedPoolUsage','PagefileUsage','PeakPagefileUsage')]
    memory_api=ctypes.WinDLL('psapi',use_last_error=True).GetProcessMemoryInfo
    memory_api.argtypes=[wintypes.HANDLE,ctypes.POINTER(Memory),wintypes.DWORD];memory_api.restype=wintypes.BOOL
    selected=set(sys.argv[1:])
    assert selected<={e['name'] for e in protocol['experiments']}
    for e in protocol['experiments']:
        name=e['name']
        if selected and name not in selected: continue
        output=HERE/'raw'/f'{name}.jsonl';log=HERE/'raw'/f'{name}.log';receipt=HERE/f'{name}-execution.json'
        assert not any(p.exists() for p in (output,log,receipt)),name
        command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
            '-Xms256m','-Xmx1024m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+';'+str(runtime/'lib/*'),
            'com.ohinteractive.seedv6.search.exact.Cp64SelectivityLab',str(ROOT/protocol['model']),str(ROOT/e['corpus']),str(output)]
        command += [str(e[k]) for k in ('workers','warmups','rounds','depth','nodeBudget','roots','mode','profiles')]
        record={'status':'running','command':command,'bindingSha256':digest(binding_path),'priority':'BelowNormal',
            'nativeAccountingSha256':digest(ROOT/'tools/sr036-process-cost.py'),'startedUtc':datetime.now(timezone.utc).isoformat()}
        started=time.monotonic()
        try:
            with log.open('xb') as stream:
                process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
                    creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
                try: record['exitCode']=process.wait(timeout=1800)
                except BaseException: process.kill();process.wait();raise
                finally:
                    record['nativeLifetime']=native.snapshot(int(process._handle))
                    memory=Memory();memory.cb=ctypes.sizeof(memory)
                    if not memory_api(int(process._handle),ctypes.byref(memory),ctypes.sizeof(memory)):raise ctypes.WinError(ctypes.get_last_error())
                    record['peakWorkingSetBytes']=memory.PeakWorkingSetSize
            assert record['exitCode']==0,'Java failed'
            assert json.loads(output.read_text().splitlines()[-1])['status']=='passed','Missing completion'
            for p,sha in binding['files'].items(): assert digest(ROOT/p)==sha,p
            if name=='flat-null-cost-d8-v1':
                previous=[json.loads(x) for x in (HERE/'raw/deeper-cost-d8-v1.jsonl').read_text().splitlines()]
                expected={(r['id'],r['round']):r['outcome'] for r in previous if r.get('type')=='request' and r['profile']=='baseline'}
                current=[json.loads(x) for x in output.read_text().splitlines()]
                checked=0
                for r in current:
                    if r.get('type')=='request' and r['profile']=='baseline':
                        assert r['outcome']==expected[r['id'],r['round']],r['id'];checked+=1
                assert checked==36
                record['defaultOutcomeParity']=True
            record['status']='passed'
        except BaseException as error: record.update(status='failed',failure=repr(error));raise
        finally:
            record['wallSeconds']=time.monotonic()-started
            record['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
            write_new(receipt,record)
        print(name,record['status'],round(record['wallSeconds'],2),flush=True)

if __name__=='__main__': main()
