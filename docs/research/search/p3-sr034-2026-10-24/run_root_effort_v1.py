"""Exclusive, frozen current-baseline trajectory execution with native accounting."""
import ctypes
from ctypes import wintypes
from datetime import datetime, timezone
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time
import zipfile
from verify_baseline_v1 import ROOT, HERE, digest

def write_new(path, value):
    with path.open('x',encoding='utf8',newline='\n') as out:
        json.dump(value,out,indent=2);out.write('\n')

def module(name, path):
    spec=importlib.util.spec_from_file_location(name,path)
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result

def main():
    protocol=json.loads((HERE/'root-effort-v1.json').read_text())
    assert json.loads((HERE/'baseline-native-v2-summary.json').read_text())['status']=='passed'
    assert json.loads((HERE/'baseline-validation-v1-summary.json').read_text())['exitCode']==0
    assert json.loads((HERE/'spending-validation-v1-summary.json').read_text())['exitCode']==0
    runtime=HERE/'raw/runtime-root-effort-v1';binding_path=HERE/'root-effort-v1-binding.json'
    if not binding_path.exists():
        shutil.copytree(ROOT/'build/research/runtime',runtime)
        native=ROOT/'releases/candidates/windows-x64/candidate-20261023T195552-c00e3315/SeedV6-UCI/app/engine-core.jar'
        assert digest(native)=='facac0db65c7f97a120083174ea4de20ef210daf3252a79a3fd5eb2cac99e6f7'
        with zipfile.ZipFile(native) as a,zipfile.ZipFile(runtime/'lib/engine-core.jar') as b:
            an={x for x in a.namelist() if not x.endswith('/')};bn={x for x in b.namelist() if not x.endswith('/')}
            assert an==bn,'Unexpected core entry set change'
            differences=[x for x in sorted(an) if a.read(x)!=b.read(x)]
            assert set(differences)<={'com/ohinteractive/seedv6/application-version.properties',
                'com/ohinteractive/seedv6/search/common/SearchControl.class',
                'com/ohinteractive/seedv6/search/common/SingleDepthSearch.class',
                'com/ohinteractive/seedv6/search/driver/SearchDriver.class'},differences
        files=[HERE/'root-effort-v1.json',Path(__file__),HERE/'verify_baseline_v1.py',native,
               ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64TimeTrajectory.java',
               ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64RootEffortTrajectory.java',
               ROOT/protocol['model'],ROOT/'tools/sr036-process-cost.py',ROOT/'tools/p3-tt-session.py']
        files += [ROOT/e['corpus'] for e in protocol['experiments']]
        files += sorted(p for p in runtime.rglob('*') if p.is_file())
        write_new(binding_path,{'frozenUtc':datetime.now(timezone.utc).isoformat(),
            'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
            'worktree':subprocess.check_output(['git','status','--short'],cwd=ROOT,text=True),
            'nativeCoreParity':{'passed':True,'onlyDifferingEntries':differences},
            'files':{str(p.relative_to(ROOT)):digest(p) for p in files}})
    binding=json.loads(binding_path.read_text())
    def stable():
        for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
    stable()
    native=module('native_cost',ROOT/'tools/sr036-process-cost.py')
    host=module('host',ROOT/'tools/p3-tt-session.py')
    class Memory(ctypes.Structure):
        _fields_=[('cb',wintypes.DWORD),('PageFaultCount',wintypes.DWORD)]+[(n,ctypes.c_size_t) for n in
            ('PeakWorkingSetSize','WorkingSetSize','QuotaPeakPagedPoolUsage','QuotaPagedPoolUsage',
             'QuotaPeakNonPagedPoolUsage','QuotaNonPagedPoolUsage','PagefileUsage','PeakPagefileUsage')]
    api=ctypes.WinDLL('psapi',use_last_error=True).GetProcessMemoryInfo
    api.argtypes=[wintypes.HANDLE,ctypes.POINTER(Memory),wintypes.DWORD];api.restype=wintypes.BOOL
    for e in protocol['experiments']:
        name=e['name'];output=HERE/'raw'/f'{name}.jsonl';log=HERE/'raw'/f'{name}.log';receipt=HERE/f'{name}-execution.json'
        assert not any(p.exists() for p in (output,log,receipt)),name
        command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
                 '-Xms256m','-Xmx768m','-XX:ActiveProcessorCount=2','-cp',str(runtime/'classes')+';'+str(runtime/'lib/*'),
                 'com.ohinteractive.seedv6.search.exact.Cp64RootEffortTrajectory',str(ROOT/protocol['model']),
                 str(ROOT/e['corpus']),str(output),str(protocol['depth']),str(protocol['nodes']),str(protocol['rounds']),str(e['roots'])]
        record={'status':'running','command':command,'bindingSha256':digest(binding_path),'priority':'BelowNormal',
                'startedUtc':datetime.now(timezone.utc).isoformat()}
        started=time.monotonic();host_start=host.host_sample()
        try:
            with log.open('xb') as stream:
                process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
                    creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
                try:record['exitCode']=process.wait(timeout=1800)
                except BaseException:process.kill();process.wait();raise
                finally:
                    record['nativeLifetime']=native.snapshot(int(process._handle))
                    memory=Memory();memory.cb=ctypes.sizeof(memory)
                    if not api(int(process._handle),ctypes.byref(memory),ctypes.sizeof(memory)):raise ctypes.WinError(ctypes.get_last_error())
                    record['peakWorkingSetBytes']=memory.PeakWorkingSetSize
            assert record['exitCode']==0,'Java failed'
            assert json.loads(output.read_text().splitlines()[-1])['status']=='passed','Incomplete output'
            stable();record['status']='passed'
        except BaseException as error:record.update(status='failed',failure=repr(error));raise
        finally:
            record['hostStart']=host_start;record['hostEnd']=host.host_sample()
            record['wallSeconds']=time.monotonic()-started
            record['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
            write_new(receipt,record)
        print(name,record['status'],round(record['wallSeconds'],2),flush=True)

if __name__=='__main__':main()
