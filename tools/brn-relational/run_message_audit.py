"""Bounded exact symbolic neural-support audit, with immutable evidence bindings."""
import argparse
import json
from pathlib import Path
import subprocess
import time
from baseline import ROOT,admit,sha
from compute_control import atomic


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('plan',type=Path);args=parser.parse_args();plan=json.loads(args.plan.read_text())
    main_class=plan.get('main_class','MessageCapacityAudit')
    if main_class not in ('MessageCapacityAudit','AnchorCapacityAudit'):raise ValueError('Unknown neural audit')
    resource=admit(initial=True);runtime=ROOT/plan['runtime'];out=ROOT/plan['output']
    if sha(ROOT/plan['cases'])!=plan['cases_sha256']:raise ValueError('Fixture changed')
    binding=dict(plan_sha256=sha(args.plan),runner_sha256=sha(Path(__file__)),runtime={p.name:sha(p) for p in runtime.glob('*.jar')})
    if not binding['runtime']:raise ValueError('No runtime')
    bound=out.with_suffix('.binding.json');receipt=out.with_suffix('.receipt.json')
    if bound.exists():
        if json.loads(bound.read_text())!=binding:raise ValueError('Binding changed')
    else:atomic(bound,binding)
    if out.exists():
        if not receipt.exists() or sha(out)!=json.loads(receipt.read_text())['sha256']:raise ValueError('Incomplete/corrupt publication; preserve and inspect')
        print('Verified completed symbolic audit');return
    command=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=1','-XX:+UseSerialGC','-Xmx384m','-cp',str(runtime/'*'),
             'com.ohinteractive.seedv6.core.brnpair2.'+main_class,str(args.plan)]
    start=time.monotonic();attempt=out.parent/(out.stem+'-'+str(time.time_ns()))
    result=subprocess.run(command,capture_output=True,text=True,timeout=60,creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
    attempt.with_suffix('.stdout.log').write_text(result.stdout);attempt.with_suffix('.stderr.log').write_text(result.stderr)
    atomic(attempt.with_suffix('.execution.json'),dict(command=command,seconds=time.monotonic()-start,returncode=result.returncode,resources=resource))
    result.check_returncode();atomic(receipt,dict(sha256=sha(out),seconds=time.monotonic()-start,resources=resource));print(result.stdout)


if __name__=='__main__':main()
