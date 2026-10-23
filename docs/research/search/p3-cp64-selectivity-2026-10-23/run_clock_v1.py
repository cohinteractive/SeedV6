"""Run one frozen development match with completion-driven native ownership."""
import ctypes
import importlib.util
import json
import subprocess
import sys
import time
from run_observation_v1 import HERE, ROOT, digest, write_new

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def worker():
    if sys.stdin.readline().strip()!='GO':raise RuntimeError('Missing owned launch gate')
    m=module('selectivity_match',ROOT/'tools/p3-selectivity-match.py')
    from argparse import Namespace
    runtime=str(HERE/'raw/runtime-v2')
    m.run(Namespace(baseline=runtime,candidate=runtime,network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
        runner=str(ROOT/'build/research/tools/fastchess.exe'),openings=str(HERE/'clock-development-v1-openings.pgn'),
        out=str(HERE/'raw/clock-development-v1'),java='C:/java/jdk-21/bin/java.exe',baseline_policy='baseline',
        candidate_policy='no-improving',baseline_workers=1,candidate_workers=1,hash_mib=64,background=True,
        operational=False,concurrency=1,pairs=32,tc='10+0.1',seed=2026102401,timeout=5400))

def main():
    ctypes.windll.kernel32.SetPriorityClass(ctypes.windll.kernel32.GetCurrentProcess(),0x4000)
    plan=json.loads((HERE/'clock-development-v1-binding.json').read_text())
    def stable():
        for p,sha in plan['files'].items():assert digest(ROOT/p)==sha,p
        for p,sha in plan['externalFiles'].items():assert digest(p)==sha,p
    stable()
    host=module('host',ROOT/'tools/p3-tt-session.py');owned=module('owned',ROOT/'tools/tournament-process.py')
    baseline=module('baseline',ROOT/'tools/search-baseline.py')
    start=host.host_sample();time.sleep(30);end=host.host_sample()
    admission=host.host_interval(start,end);write_new(HERE/'clock-development-v1-admission.json',admission)
    if not admission['passed']:raise RuntimeError('Resource admission failed; no games launched')
    start=host.host_sample();receipt={'status':'running','bindingSha256':digest(HERE/'clock-development-v1-binding.json')}
    write_new(HERE/'clock-development-v1-launch.json',receipt|{'hostStart':start})
    try:
        with (HERE/'raw/clock-development-v1-worker.log').open('xb') as log:
            receipt['execution']=owned.run_owned([sys.executable,'-B',str(Path(__file__).resolve()),'worker'],
                log,5500,ROOT,baseline.clean_environment())
        end=host.host_sample();receipt['resourceQualification']=host.host_interval(start,end,receipt['execution']['totalOwnedCpuSeconds'])
        assert receipt['execution']['exitCode']==0
        result=json.loads((HERE/'raw/clock-development-v1/run.json').read_text())
        assert result['status']=='passed';stable()
        receipt['results']=result['results'];receipt['matchWallSeconds']=result['matchWallSeconds']
        receipt['status']='passed' if receipt['resourceQualification']['passed'] else 'resource-unqualified'
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in sorted((HERE/'raw/clock-development-v1').iterdir()) if p.is_file()}
    except BaseException as e:receipt.update(status='failed',failure=repr(e));raise
    finally:write_new(HERE/'clock-development-v1-summary.json',receipt)
    print(json.dumps({'status':receipt['status'],'results':receipt['results']},indent=2))

if __name__=='__main__':
    from pathlib import Path
    if len(sys.argv)>1 and sys.argv[1]=='worker':worker()
    else:main()
