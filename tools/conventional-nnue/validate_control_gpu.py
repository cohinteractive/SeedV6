"""Short, opt-in real GPU interruption/resume validation under the owner supervisor.

Usage: nnue-control.cmd run ... -- python -B THIS_SCRIPT NEW_OUTPUT PILOT_CONFIG
Uses an isolated control for test mode changes; outer owner control remains effective.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import compute_control as cc
import compute_gate
import windows_job as windows


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output',type=Path);parser.add_argument('source_config',type=Path)
    parser.add_argument('--emergency',action='store_true')
    args=parser.parse_args();compute_gate.require()
    args.output.mkdir(parents=True,exist_ok=False)
    base=json.loads(args.source_config.read_text())
    base.update(output=str((args.output/'segmented').resolve()),start_superbatch=1,end_superbatch=4,
                batch_size=1024,batches_per_superbatch=128,save_rate=1,resume=None)
    config=args.output/'config.json';cc.atomic(config,base)
    control=cc.Control(args.output/'test-control');control.request('NORMAL')
    command=[sys.executable,'-B',str(Path(cc.__file__)),'--state-dir',str(control.directory),
             'train','--name','short GPU control validation',str(config.resolve())]
    stream=(args.output/'held-console.log').open('xb')
    process=subprocess.Popen(command,stdout=stream,stderr=subprocess.STDOUT)
    try:
        deadline=time.monotonic()+90; observed=None
        while time.monotonic()<deadline:
            if not compute_gate.permitted(): raise RuntimeError('Outer owner hold; stop validation')
            if process.poll() is not None:raise RuntimeError('Training ended before interruption witness')
            state=control.status();active=state['active']
            if active and active.get('checkpoint') and active.get('child_pid'):
                if active['checkpoint']['completed_superbatch']==1 and '000002-' in str(active['command']):
                    # Observe the actual CUDA process before requesting the hold.
                    query=subprocess.run(['nvidia-smi','--query-compute-apps=pid','--format=csv,noheader,nounits'],
                                         capture_output=True,text=True,timeout=10)
                    pids={int(line.strip()) for line in query.stdout.splitlines() if line.strip().isdigit()}
                    if active['child_pid'] in pids:
                        observed=active;break
            time.sleep(.05)
        if observed is None:raise RuntimeError('No owned live CUDA process observed in second segment')
        start=time.monotonic();control.request('COMPUTE_HOLD',now=args.emergency)
        process.wait(timeout=40);latency=time.monotonic()-start
        stream.close()
        state=control.status()
        if not state['hold_complete'] or state['owned_processes']!=0:raise RuntimeError('Hold cleanup incomplete')
        if windows.identity(observed['child_pid']) is not None:raise RuntimeError('Owned CUDA child still alive')
        latest=state['latest_checkpoint'];cc.verify_checkpoint(latest['path'])
        before=list(Path(base['output']).glob('segments/*/verified.json'))
        query=subprocess.run(['nvidia-smi','--query-compute-apps=pid','--format=csv,noheader,nounits'],
                             capture_output=True,text=True,timeout=10)
        after_pids={int(line.strip()) for line in query.stdout.splitlines() if line.strip().isdigit()}
        if observed['child_pid'] in after_pids:raise RuntimeError('CUDA PID still reported after hold')
        # A fresh process must deny launch; explicit normal then resumes the same plan.
        denied=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
        if denied.returncode==0:raise RuntimeError('Hold failed to prevent training launch')
        control.request('FULL_PAUSE')
        if control.status()['lightweight_allowed']:raise RuntimeError('Pause permission mismatch')
        compute_gate.require();control.request('NORMAL')
        with (args.output/'resume-console.log').open('xb') as output:
            resumed=subprocess.run(command,stdout=output,stderr=subprocess.STDOUT)
        if resumed.returncode!=0:raise RuntimeError('Resume failed')
        final=control.status()['latest_checkpoint'];cc.verify_checkpoint(final['path'])
        if final['completed_superbatch']!=4:raise RuntimeError('Wrong resumed endpoint')
        after=list(Path(base['output']).glob('segments/*/verified.json'))
        if len(after)!=4 or any(not p.exists() for p in before):raise RuntimeError('Completed segments repeated/removed')
        # Historical nonsegmented method, identical fixed recipe and endpoint.
        reference=dict(base,output=str((args.output/'reference').resolve()))
        cc.atomic(args.output/'reference-config.json',reference)
        trainer=str(cc.ROOT/'build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe')
        refcmd=[sys.executable,'-B',str(Path(cc.__file__)),'--state-dir',str(control.directory),'run',
                '--name','bounded uninterrupted GPU reference','--resume-note','Reference validation only',
                '--log',str(args.output/'reference-console.log'),'--',trainer,'train',str(args.output/'reference-config.json')]
        compute_gate.require();subprocess.run(refcmd,check=True)
        reference_path=Path(reference['output'])/'seed-cnnue-v1-4';cc.verify_checkpoint(reference_path)
        cc.atomic(args.output/'summary.json',dict(hold_seconds=latency,kind='emergency' if args.emergency else 'graceful',held_after_superbatch=latest['completed_superbatch'],
            observed_cuda_pid=observed['child_pid'],owned_processes_after_hold=0,cuda_pid_absent_after_hold=True,
            held_checkpoint=latest,final_checkpoint=final,reference_checkpoint=str(reference_path),
            completed_segments_before_resume=len(before),completed_segments_after_resume=len(after),
            denied_fresh_process_exit=denied.returncode,full_pause_checked=True,
            original_recipe='1024 batch,128 batches/superbatch,4 superbatches,constant LR/WDL',
            limitations='Single short local measurement; GPU continuation is not bitwise deterministic'))
        print(json.dumps(json.loads((args.output/'summary.json').read_text()),indent=2))
    finally:
        control.request('COMPUTE_HOLD',now=True)
        if process.poll() is None:process.wait(timeout=15)
        stream.close()


if __name__=='__main__':main()
