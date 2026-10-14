"""Attach read-only Windows process handles to a fixed Fastchess engine cohort.

Each engine carries a harmless JVM actor property. Retained native handles give
full lifetime CPU after exit, including startup/JIT/GC and aborted Search work.
Discovery runs during startup only; this does not enforce CPU affinity.
"""
import importlib.util
import json
from pathlib import Path
import subprocess
import time

spec=importlib.util.spec_from_file_location('native_cost',Path(__file__).with_name('sr036-process-cost.py'))
n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n)


class Cohort:
    def __init__(self,pid,concurrency,output):
        self.pid=pid;self.expected=concurrency;self.output=Path(output);self.handles={}
        self.report={'status':'discovering','runnerPid':pid,'expectedPerActor':concurrency,
                     'observerSha256':n.digest(__file__),'nativeObserverSha256':n.digest(n.__file__),
                     'processes':[]}

    def attach(self):
        deadline=time.monotonic()+60
        while True:
            # PID is an integer from our own Popen. Never interpolate user text.
            cmd=f"Get-CimInstance Win32_Process -Filter 'ParentProcessId={int(self.pid)}' | Select-Object ProcessId,Name,CommandLine | ConvertTo-Json -Compress"
            result=subprocess.run(['powershell','-NoProfile','-NonInteractive','-Command',cmd],capture_output=True,text=True,check=True)
            rows=json.loads(result.stdout) if result.stdout.strip() else []
            if isinstance(rows,dict):rows=[rows]
            for row in rows:
                pid=row['ProcessId'];command=row['CommandLine'] or ''
                if pid in self.handles:continue
                assert row['Name'].lower()=='java.exe',row
                actor=next((a for a in ('baseline','candidate') if '-Dseedv6.research.actor='+a in command),None)
                assert actor is not None,row
                handle=n.kernel.OpenProcess(0x1000|0x100000,False,pid)
                if not handle:raise n.c.WinError(n.c.get_last_error())
                self.handles[pid]=handle
                observed=n.snapshot(handle)
                assert observed['exitCode']==259,'Engine exited before accounting attachment'
                self.report['processes'].append({'pid':pid,'actor':actor,'command':command,'atAttachment':observed})
            counts={a:sum(x['actor']==a for x in self.report['processes']) for a in ('baseline','candidate')}
            assert all(c<=self.expected for c in counts.values()),counts
            if all(c==self.expected for c in counts.values()):break
            if time.monotonic()>deadline:raise TimeoutError('Engine cohort discovery incomplete: '+str(counts))
            time.sleep(.5)
        self.report['status']='attached';self.save()

    def close(self):
        try:
            for row in self.report['processes']:
                row['lifetime']=n.snapshot(self.handles[row['pid']])
            assert len(self.handles)==2*self.expected
            assert all(x['lifetime']['exitCode']==0 for x in self.report['processes'])
            self.report['cpuSeconds']={a:sum(x['lifetime']['totalCpuSeconds'] for x in self.report['processes'] if x['actor']==a)
                                       for a in ('baseline','candidate')}
            self.report['candidateToBaselineCpu']=self.report['cpuSeconds']['candidate']/self.report['cpuSeconds']['baseline']
            self.report['status']='passed'
        except BaseException as error:
            self.report.update(status='failed',failure=repr(error));raise
        finally:
            for handle in self.handles.values():n.kernel.CloseHandle(handle)
            self.handles.clear();self.save()

    def save(self):
        self.output.write_text(json.dumps(self.report,indent=2)+'\n',encoding='utf-8',newline='\n')
