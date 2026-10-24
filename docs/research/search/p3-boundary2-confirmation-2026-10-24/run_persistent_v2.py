"""Persistent v1 capture with decimal/hexadecimal JDK native-thread-ID parsing only."""
import ctypes as c
from ctypes import wintypes as w
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
from types import SimpleNamespace
from verify_inputs import HERE, ROOT, OLD, audit, digest, write_new
from run_diagnostic_v1 import module, roots, background


class OwnerCpu:
    def __init__(self, pid, evidence):
        result = subprocess.run(['C:/java/jdk-21/bin/jcmd.exe', str(pid), 'Thread.print'], capture_output=True,
                                text=True, timeout=30,
                                creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        with evidence.open('x', encoding='utf8') as out:
            out.write(result.stdout+result.stderr)
        assert result.returncode == 0
        lines = [line for line in result.stdout.splitlines() if line.startswith('"seedv6-search-worker"')]
        assert len(lines) == 1, 'Unique owned lifecycle worker required'
        token = re.search(r'\bnid=(0x[0-9a-fA-F]+|[0-9]+)\b', lines[0])[1]
        self.tid = int(token, 16 if token.startswith('0x') else 10)
        self.k = c.WinDLL('kernel32', use_last_error=True)
        self.k.OpenThread.argtypes=[w.DWORD,w.BOOL,w.DWORD]; self.k.OpenThread.restype=w.HANDLE
        self.k.GetThreadTimes.argtypes=[w.HANDLE,*([c.POINTER(w.FILETIME)]*4)]; self.k.GetThreadTimes.restype=w.BOOL
        self.k.QueryThreadCycleTime.argtypes=[w.HANDLE,c.POINTER(c.c_ulonglong)]; self.k.QueryThreadCycleTime.restype=w.BOOL
        self.k.CloseHandle.argtypes=[w.HANDLE]; self.k.CloseHandle.restype=w.BOOL
        self.handle=self.k.OpenThread(0x0040,False,self.tid)
        assert self.handle, 'Cannot retain Search-worker handle'

    def sample(self):
        values=[w.FILETIME() for _ in range(4)]; cycles=c.c_ulonglong()
        if not self.k.GetThreadTimes(self.handle,*[c.byref(x) for x in values]): raise c.WinError(c.get_last_error())
        if not self.k.QueryThreadCycleTime(self.handle,c.byref(cycles)): raise c.WinError(c.get_last_error())
        ticks=[(x.dwHighDateTime<<32)|x.dwLowDateTime for x in values]
        return {'creationTicks':ticks[0], 'cpuSeconds':(ticks[2]+ticks[3])/1e7, 'cycles':cycles.value}

    def close(self):
        if self.handle:
            self.k.CloseHandle(self.handle); self.handle=None


def worker(name):
    assert sys.stdin.readline().strip() == 'GO'
    background()
    m=module('spending',OLD/'spending_match_v1.py')
    native=module('native',ROOT/'tools/sr036-process-cost.py')
    protocol=json.loads((HERE/'persistent-diagnostic-v1.json').read_text())
    out=HERE/'raw'/name; cases=roots(m.b.chess)
    args=SimpleNamespace(baseline=str(OLD/'raw/runtime-production-v2'), candidate=str(OLD/'raw/runtime-candidate-v2'),
        baseline_policy='baseline', candidate_policy='boundary2',baseline_workers=1,candidate_workers=1,
        java='C:/java/jdk-21/bin/java.exe',network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'))
    record={'status':'running','actors':{}}; engines={}; cpus={}
    try:
        for profile in ('baseline','boundary2'):
            command=m.command(args,'baseline' if profile=='baseline' else 'candidate')
            relative=(out/profile).relative_to(ROOT).as_posix()
            command[1:1]=[f'-Xlog:gc*,safepoint=info:file={relative}-gc.log:utctime,uptimenanos,level,tags:filecount=0',
                         f'-XX:StartFlightRecording=settings=default,filename={relative}.jfr,dumponexit=true,maxsize=128m']
            engine=m.b.Uci(command,out/(profile+'-uci.txt')); engines[profile]=engine
            engine.send('uci'); identity=m.verify_identity(engine.until('uciok',90),1,profile,64)
            for _ in range(10):
                engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                engine.send('position startpos'); engine.send('go nodes 100000')
                r=m.b.inspect_result(m.b.chess.Board(),engine.until('bestmove ',30))
                assert r['nodes']==100000 and r['termination']=='NODE_LIMIT'
            cpus[profile]=OwnerCpu(engine.process.pid,out/(profile+'-threads.txt'))
            record['actors'][profile]={'command':command,'identity':identity,'ownerThreadId':cpus[profile].tid}
        with (out/'requests.jsonl').open('x',encoding='utf8') as stream:
            for round_index in range(protocol['sample']['rounds']):
                for engine in engines.values():
                    engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                rotated=cases[round_index%14:]+cases[:round_index%14]
                for clock in protocol['sample']['remainingMillis']:
                    for ri,(rid,position,board) in enumerate(rotated):
                        profiles=('baseline','boundary2') if (round_index+ri)%2==0 else ('boundary2','baseline')
                        for profile in profiles:
                            engine=engines[profile]; owner=cpus[profile]
                            engine.send('position '+position); engine.send('isready'); engine.until('readyok')
                            process0=native.snapshot(int(engine.process._handle))['totalCpuSeconds']; owner0=owner.sample()
                            wall0=time.time_ns(); tick0=time.perf_counter_ns()
                            engine.send(f'go wtime {clock} btime {clock} winc 100 binc 100')
                            lines=engine.until('bestmove ',30)
                            tick1=time.perf_counter_ns(); wall1=time.time_ns()
                            owner1=owner.sample(); process1=native.snapshot(int(engine.process._handle))['totalCpuSeconds']
                            assert owner1['creationTicks']==owner0['creationTicks']
                            result=m.b.inspect_result(board,lines)
                            depths=[int(x.split()[2]) for x in lines if x.startswith('info depth ')]
                            assert depths==list(range(1,len(depths)+1))
                            assert result['termination'] in ('TIME_LIMIT','TIME_ALLOCATION','COMPLETED')
                            engine.send('isready'); assert engine.until('readyok')==['readyok']
                            base=min(max(0,clock-50),max(1,clock//30+65))
                            hard=base if profile=='baseline' else min(max(0,clock-50),2*base)
                            row={'round':round_index,'root':rid,'profile':profile,'clockMillis':clock,'hardMillis':hard,
                                 'startEpochNanos':wall0,'endEpochNanos':wall1,'externalElapsedNanos':tick1-tick0,
                                 'ownerCpuSeconds':owner1['cpuSeconds']-owner0['cpuSeconds'],
                                 'ownerCycles':owner1['cycles']-owner0['cycles'], 'nativeCpuSeconds':process1-process0,
                                 'overrunMillis':result['elapsedNanos']/1e6-hard,'result':result}
                            stream.write(json.dumps(row,allow_nan=False)+'\n'); stream.flush()
        record['status']='passed'
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        for owner in cpus.values(): owner.close()
        for profile,engine in engines.items():
            engine.close()
            record['actors'].setdefault(profile,{})['nativeLifetime']=native.snapshot(int(engine.process._handle))
        write_new(out/'execution.json',record)


def main(name):
    assert re.fullmatch(r'[a-z0-9-]+',name)
    background(); audit()
    prerequisite=json.loads((HERE/'diagnostic-v1-a01-analysis.json').read_text())
    assert prerequisite['captureStatus']=='passed' and prerequisite['allControlSignaturesEqual']
    assert all(r['descriptiveFivePercentScreenPassed'] for r in prerequisite['instrumentationCost'])
    out=HERE/'raw'/name; assert not out.exists(); out.mkdir(parents=True)
    files=[Path(__file__),HERE/'run_persistent_v1.py',HERE/'persistent-diagnostic-v1.json',HERE/'run_diagnostic_v1.py',HERE/'verify_inputs.py',
           HERE/'diagnostic-v1-a01-analysis.json',Path('C:/java/jdk-21/bin/jcmd.exe')]
    binding={str(p):digest(p) for p in files}
    write_new(HERE/(name+'-binding.json'),{'files':binding,'historicalInputAudit':audit()})
    host=module('host',ROOT/'tools/p3-tt-session.py'); owned=module('owned',ROOT/'tools/tournament-process.py')
    start=host.host_sample(); time.sleep(30); admission=host.host_interval(start,host.host_sample())
    admission['passed']=admission['otherAverageLogicalCores']<=2 and admission['memoryEndpointsPass']
    write_new(HERE/(name+'-admission.json'),admission)
    if not admission['passed']:
        print('Resource admission failed; persistent capture unstarted'); return
    record={'status':'running','bindingSha256':digest(HERE/(name+'-binding.json'))}; start=host.host_sample()
    try:
        with (out/'worker.log').open('xb') as stream:
            record['execution']=owned.run_owned([sys.executable,'-B',str(Path(__file__).resolve()),name,'worker'],stream,2400,ROOT,os.environ.copy())
        assert record['execution']['exitCode']==0
        record['resources']=host.host_interval(start,host.host_sample(),record['execution']['totalOwnedCpuSeconds'])
        record['resources']['passed']=record['resources']['otherAverageLogicalCores']<=2 and record['resources']['memoryEndpointsPass']
        for path,sha in binding.items(): assert digest(path)==sha,path
        audit(); record['status']='passed' if record['resources']['passed'] else 'resource-unqualified'
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        record['evidence']={str(p.relative_to(HERE)):digest(p) for p in out.iterdir() if p.is_file()}
        write_new(HERE/(name+'-summary.json'),record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('resources','evidence')},indent=2))


if __name__=='__main__':
    if len(sys.argv)==3 and sys.argv[2]=='worker': worker(sys.argv[1])
    else: main(sys.argv[1])
