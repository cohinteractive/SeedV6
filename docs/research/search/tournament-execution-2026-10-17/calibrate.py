"""Fixed operational pilot order; invoke with the repository's research venv.

Plans must already exist. Does not prepare/restart missing or failed studies.
Raw transcripts are preserved; only completed-block timing is summarized here.
"""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
ORDER = ['w4-c2', 'w4-c1', 'w4-c3', 'w4-c3', 'w4-c1', 'w4-c2', 'w1-c4', 'w1-c4']


def main():
    receipt = HERE/'calibration-execution.json'
    if receipt.exists(): raise ValueError('Never replay the calibration campaign')
    report = {'purpose':'Operational throughput/integrity only; no strength inference',
              'startedUtc':datetime.now(timezone.utc).isoformat(), 'order':ORDER,
              'driverSha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(), 'sessions':[]}
    def save():receipt.write_text(json.dumps(report,indent=2)+'\n')
    save()
    try:
        for index,name in enumerate(ORDER):
            study = HERE/'raw'/name
            def sample():
                time.sleep(20)
                return subprocess.run(['powershell','-NoProfile','-NonInteractive','-File',
                    str(ROOT/'tools/sr024-host-sample.ps1'),'-Seconds','15','-Out',
                    str(HERE/'raw'/f'host-{index}.json')],capture_output=True,text=True,timeout=50)
            with ThreadPoolExecutor(max_workers=1) as pool:
                sampling=pool.submit(sample)
                with (HERE/'raw'/f'session-{index}.txt').open('w') as log:
                    start=time.monotonic()
                    command=[sys.executable,'-B',str(study/'inputs/tools/tournament-session.py'),
                        'run','--plan',str(study/'plan.json'),'--max-blocks','1','--stop-before',
                        (datetime.now(timezone.utc)+timedelta(minutes=20)).isoformat()]
                    run=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,cwd=ROOT,timeout=1100)
                sample_result=sampling.result()
            entry={'index':index,'study':name,'exitCode':run.returncode,
                   'sessionWallSeconds':time.monotonic()-start,'hostSampleExitCode':sample_result.returncode,
                   'command':command}
            report['sessions'].append(entry);save()
            print(json.dumps(entry),flush=True)
            if run.returncode:raise RuntimeError('Pilot failed; preserve output and stop')
        report['status']='passed'
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        report['endedUtc']=datetime.now(timezone.utc).isoformat();save()


if __name__=='__main__':main()
