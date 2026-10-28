"""Bounded structural audit with frozen inputs/runtime and no game search."""
import argparse
import json
from pathlib import Path
import subprocess
import time
from baseline import ROOT,admit,sha
from compute_control import atomic


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('plan',type=Path);args=parser.parse_args()
    plan=json.loads(args.plan.read_text());runtime=ROOT/plan['runtime'];out=ROOT/plan['output']
    resource=admit(initial=True)
    binding=dict(plan_sha256=sha(args.plan),runner_sha256=sha(Path(__file__)),runtime={p.name:sha(p) for p in runtime.glob('*.jar')})
    if not binding['runtime']:raise ValueError('No runtime')
    for path,key in [('cases','cases_sha256'),('teacher','teacher_sha256')]:
        if sha(ROOT/plan[path])!=plan[key]:raise ValueError('Input identity')
    for model in plan['models']:
        if sha(ROOT/model['path'])!=model['sha256']:raise ValueError('Model identity')
    bound=out.with_suffix('.binding.json');receipt=out.with_suffix('.receipt.json')
    if bound.exists():
        if json.loads(bound.read_text())!=binding:raise ValueError('Audit binding changed')
    else:atomic(bound,binding)
    if out.exists():
        if not receipt.exists() or sha(out)!=json.loads(receipt.read_text())['sha256']:raise ValueError('Incomplete/corrupt publication; preserve and inspect')
        print('Verified complete structural audit');return
    command=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=1','-XX:+UseSerialGC','-Xmx384m','-cp',str(runtime/'*'),
             'com.ohinteractive.seedv6.core.brnpair2.InteractionAudit',str(args.plan)]
    start=time.monotonic();attempt=out.parent/(out.stem+'-'+str(time.time_ns()))
    try:
        result=subprocess.run(command,capture_output=True,text=True,timeout=60,creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        attempt.with_suffix('.stdout.log').write_text(result.stdout);attempt.with_suffix('.stderr.log').write_text(result.stderr)
        atomic(attempt.with_suffix('.execution.json'),dict(command=command,returncode=result.returncode,seconds=time.monotonic()-start,resources=resource))
        result.check_returncode()
    except subprocess.TimeoutExpired as exc:
        atomic(attempt.with_suffix('.execution.json'),dict(command=command,error=repr(exc),seconds=time.monotonic()-start,resources=resource));raise
    atomic(receipt,dict(sha256=sha(out),seconds=time.monotonic()-start,resources=resource));print(result.stdout)


if __name__=='__main__':main()
