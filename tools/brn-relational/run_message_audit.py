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
    if main_class not in ('MessageCapacityAudit','AnchorCapacityAudit','ContextualAttackCapacityAudit','ContextualPredictionAudit','AttackFieldCapacityAudit','HorizontalSymmetryAudit','FieldPredictionAudit','GamePositionPredictionAudit','NeuralScaleCalibrationAudit','ContextualPieceCapacityAudit','RoutingMechanismAudit'):raise ValueError('Unknown neural audit')
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
    process=None;samples=[resource];error=None;returncode=None
    try:
        with attempt.with_suffix('.stdout.log').open('x') as stdout,attempt.with_suffix('.stderr.log').open('x') as stderr:
            process=subprocess.Popen(command,stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
            while True:
                try:
                    returncode=process.wait(timeout=5)
                    if returncode:raise RuntimeError('Audit Java exit '+str(returncode))
                    break
                except subprocess.TimeoutExpired:
                    samples.append(admit())
                    if time.monotonic()-start>60:raise RuntimeError('Bounded audit allocation exhausted')
    except Exception as exc:
        error=repr(exc);raise
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            try:process.wait(timeout=5)
            except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
        atomic(attempt.with_suffix('.execution.json'),dict(command=command,seconds=time.monotonic()-start,returncode=returncode,error=error,resources=samples,process_absent=process is None or process.poll() is not None))
    atomic(receipt,dict(sha256=sha(out),seconds=time.monotonic()-start,resources=samples));print(attempt.with_suffix('.stdout.log').read_text())



if __name__=='__main__':main()
