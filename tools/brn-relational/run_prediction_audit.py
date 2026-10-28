"""Short, hash-bound prediction and full/cache audit of frozen Pair-2 payloads."""
import argparse
import json
from pathlib import Path
import subprocess
import time
from baseline import ROOT,admit,sha
from compute_control import atomic


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('plan',type=Path);args=p.parse_args();plan=json.loads(args.plan.read_text())
    resource=admit(initial=True);runtime=ROOT/plan['runtime'];panel=ROOT/plan['validation'];out=ROOT/plan['output'];out.parent.mkdir(parents=True,exist_ok=True)
    if sha(panel)!=plan['validation_sha256']:raise ValueError('Prediction panel identity')
    for model in plan['models']:
        if sha(ROOT/model['path'])!=model['sha256']:raise ValueError('Prediction model identity')
    binding=dict(plan_sha256=sha(args.plan),runner_sha256=sha(Path(__file__)),runtime={f.name:sha(f) for f in runtime.glob('*.jar')})
    if not binding['runtime']:raise ValueError('No audit runtime')
    bound=out.with_suffix('.binding.json');receipt=out.with_suffix('.receipt.json')
    if bound.exists():
        if json.loads(bound.read_text())!=binding:raise ValueError('Frozen audit binding changed')
    else:atomic(bound,binding)
    if out.exists():
        if not receipt.exists() or sha(out)!=json.loads(receipt.read_text())['sha256']:raise ValueError('Incomplete/corrupt audit publication; preserve and inspect')
        print('Verified complete prediction audit');return
    command=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=1','-XX:+UseSerialGC','-Xmx384m','-cp',str(runtime/'*'),
             'com.ohinteractive.seedv6.core.brnpair2.PairPredictionDiagnostic',str(panel)]+[str(ROOT/m['path']) for m in plan['models']]
    attempt=out.parent/(out.stem+'-'+str(time.time_ns()));started=time.monotonic()
    try:
        result=subprocess.run(command,capture_output=True,text=True,timeout=60,creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        attempt.with_suffix('.stdout.log').write_text(result.stdout);attempt.with_suffix('.stderr.log').write_text(result.stderr)
        atomic(attempt.with_suffix('.execution.json'),dict(command=command,returncode=result.returncode,seconds=time.monotonic()-started,resources=resource))
        result.check_returncode();values=json.loads(result.stdout)
    except subprocess.TimeoutExpired as exc:
        atomic(attempt.with_suffix('.execution.json'),dict(command=command,error=repr(exc),seconds=time.monotonic()-started,resources=resource))
        raise
    if len(values)!=len(plan['models']) or any(v['positions']!=plan.get('positions',4096) for v in values):raise ValueError('Incomplete audit result')
    atomic(out,dict(binding=binding,resources=resource,results=values));atomic(receipt,dict(sha256=sha(out),scope='Read-only compiled-model prediction/full-cache check'))
    print(json.dumps(values))


if __name__=='__main__':main()
