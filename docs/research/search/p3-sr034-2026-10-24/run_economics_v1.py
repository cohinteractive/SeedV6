"""Bounded immutable clock economics / pre-post CPU delivery observation."""
import ctypes
from ctypes import wintypes
import json
import math
from pathlib import Path
import shutil
import statistics
import subprocess
import sys
import time
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module

def host_interval(start,end,cpu=0):
    host=module('host',ROOT/'tools/p3-tt-session.py')
    r=host.host_interval(start,end,cpu)
    r['legacyOneCoreGatePassed']=r.pop('passed')
    r['passed']=r['otherAverageLogicalCores']<=2 and r['memoryEndpointsPass']
    r['protocol']='SR034 economics-v1 current serial-W1 host plus direct delivery qualification'
    return r

def execute(name,mode):
    assert mode in ('clock','delivery') and all(c.isalnum() or c=='-' for c in name)
    protocol=json.loads((HERE/'economics-v1.json').read_text())
    runtime=HERE/'raw/runtime-economics-v1';binding_path=HERE/'economics-v1-binding.json'
    if not binding_path.exists():
        assert json.loads((HERE/'economics-build-v1-summary.json').read_text())['exitCode']==0
        shutil.copytree(ROOT/'build/research/runtime',runtime)
        files=sorted(p for p in runtime.rglob('*') if p.is_file())
        files += [HERE/'economics-v1.json',Path(__file__),ROOT/protocol['corpus'],
            ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue',ROOT/'tools/sr036-process-cost.py',ROOT/'tools/p3-tt-session.py']
        write_new(binding_path,{'files':{str(p.relative_to(ROOT)):digest(p) for p in files},
            'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()})
    binding=json.loads(binding_path.read_text())
    def stable():
        for path,sha in binding['files'].items():assert digest(ROOT/path)==sha,path
    stable();native=module('native',ROOT/'tools/sr036-process-cost.py');host=module('host',ROOT/'tools/p3-tt-session.py')
    output=HERE/'raw'/(name+'.jsonl');log=HERE/'raw'/(name+'.log');summary=HERE/(name+'-summary.json')
    assert not any(p.exists() for p in (output,log,summary))
    cmd=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Xms256m','-Xmx512m',
        '-XX:ActiveProcessorCount=4','-Djava.awt.headless=true','-cp',str(runtime/'classes')+';'+str(runtime/'lib/*'),
        'com.ohinteractive.seedv6.search.exact.Cp64ClockEconomics',str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
        str(ROOT/protocol['corpus']),str(output),mode]
    start=host.host_sample();started=time.monotonic();record={'status':'running','command':cmd,'bindingSha256':digest(binding_path)}
    try:
        with log.open('xb') as stream:
            process=subprocess.Popen(cmd,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
            try:record['exitCode']=process.wait(timeout=1800)
            except BaseException:process.kill();process.wait();raise
            finally:
                record['nativeLifetime']=native.snapshot(int(process._handle))
                class Memory(ctypes.Structure):
                    _fields_=[('cb',wintypes.DWORD),('PageFaultCount',wintypes.DWORD)]+[(n,ctypes.c_size_t) for n in
                        ('PeakWorkingSetSize','WorkingSetSize','QuotaPeakPagedPoolUsage','QuotaPagedPoolUsage',
                         'QuotaPeakNonPagedPoolUsage','QuotaNonPagedPoolUsage','PagefileUsage','PeakPagefileUsage')]
                api=ctypes.WinDLL('psapi',use_last_error=True).GetProcessMemoryInfo
                api.argtypes=[wintypes.HANDLE,ctypes.POINTER(Memory),wintypes.DWORD];api.restype=wintypes.BOOL
                memory=Memory();memory.cb=ctypes.sizeof(memory)
                if not api(int(process._handle),ctypes.byref(memory),ctypes.sizeof(memory)):raise ctypes.WinError(ctypes.get_last_error())
                record['peakWorkingSetBytes']=memory.PeakWorkingSetSize
        record['hostInterval']=host_interval(start,host.host_sample(),record['nativeLifetime']['totalCpuSeconds'])
        assert record['exitCode']==0
        rows=[json.loads(s) for s in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
        measured=[x for x in rows if x.get('type')=='request' and x['round']>=0]
        expected=216 if mode=='clock' else 72;assert len(measured)==expected
        summaries=[]
        for budget in ((50,100,400) if mode=='clock' else (-1,)):
            for profile in ('baseline','half','stable3-half'):
                selected=[x for x in measured if x['profile']==profile and x['budgetMillis']==budget];assert len(selected)==24
                ratios=sorted(x['ownerCpuToElapsed'] for x in selected)
                completed=[x['outcome'].get('lastCompletedResult') for x in selected]
                value={'profile':profile,'budgetMillis':budget,'requests':len(selected),'medianCpuDelivery':statistics.median(ratios),
                    'fifthPercentileCpuDelivery':ratios[math.ceil(.05*len(ratios))-1],
                    'elapsedNanos':sum(x['elapsedNanos'] for x in selected),'ownerCpuNanos':sum(x['ownerThreadCpuNanos'] for x in selected),
                    'processCpuNanos':sum(x['processCpuNanos'] for x in selected),'nodes':sum(x['outcome']['nodes'] for x in selected),
                    'softStops':sum(x['termination']=='TIME_ALLOCATION' for x in selected),
                    'incompleteRequests':sum(x['outcome']['iterationIncomplete'] for x in selected),
                    'completedDepthMean':statistics.mean(r['depth'] if r else 0 for r in completed),
                    'gcCollections':sum(x['gcCollections'] for x in selected),'compilationMillis':sum(x['compilationMillis'] for x in selected),
                    'maxHeapUsed':max(x['heapUsed'] for x in selected)}
                if mode=='clock':
                    value['maxOverrunMillis']=max(x['elapsedNanos']/1e6-budget for x in selected)
                    pairs=[(x['outcome'].get('lastCompletedResult'),next(y for y in measured
                        if y['id']==x['id'] and y['round']==x['round'] and y['budgetMillis']==budget and y['profile']=='baseline')['outcome'].get('lastCompletedResult'))
                        for x in selected]
                    value['unmatchedCompletedMoves']=sum(a is None or b is None for a,b in pairs)
                    value['matchedBaselineDifferentMove']=sum(a['bestMove']!=b['bestMove'] for a,b in pairs if a is not None and b is not None)
                summaries.append(value)
        record['profiles']=summaries
        if mode=='delivery':
            spread=max(x['medianCpuDelivery'] for x in summaries)-min(x['medianCpuDelivery'] for x in summaries)
            record['betweenProfileMedianRange']=spread
            record['criteriaPassed']=all(x['medianCpuDelivery']>=.90 and x['fifthPercentileCpuDelivery']>=.80 for x in summaries) and spread<=.05 and record['hostInterval']['passed']
        else:record['timingQualified']=record['hostInterval']['passed']
        stable();record['status']='passed'
    except BaseException as error:record.update(status='failed',failure=repr(error));raise
    finally:
        record['wallSeconds']=time.monotonic()-started
        record['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
        write_new(summary,record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('command','evidence','hostInterval','profiles')},indent=2),flush=True)
    return record

if __name__=='__main__':execute(sys.argv[1],sys.argv[2])
