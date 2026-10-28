"""One bounded CPU teacher relabeling job; resume only verified atomic chunks."""
import argparse
import json
from pathlib import Path
import subprocess
import time
from baseline import ROOT,admit,sha
from compute_control import atomic


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('plan',type=Path);args=p.parse_args()
    plan=json.loads(args.plan.read_text());config_path=ROOT/plan['config'];config=json.loads(config_path.read_text());runtime=ROOT/plan['runtime'];out=ROOT/config['output']
    resources=[admit(initial=True)];out.mkdir(parents=True,exist_ok=True)
    binding=dict(config_sha256=sha(config_path),runtime={f.name:sha(f) for f in runtime.glob('*.jar')},runner_sha256=sha(Path(__file__)))
    if not binding['runtime']:raise ValueError('No frozen teacher runtime')
    if (out/'binding.json').exists():
        if json.loads((out/'binding.json').read_text())!=binding:raise ValueError('Frozen teacher binding changed')
    else:atomic(out/'binding.json',binding)
    command=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=1','-XX:+UseSerialGC','-Xmx256m','-cp',str(runtime/'*'),'com.ohinteractive.seedv6.core.brnpair2.TeacherDataset',str(config_path)]
    started=time.monotonic();process=None;error=None
    try:
        with (out/('attempt-'+str(time.time_ns())+'.log')).open('x') as log:
            process=subprocess.Popen(command,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
            while True:
                try:
                    code=process.wait(timeout=5)
                    if code:raise RuntimeError('Teacher JVM exit '+str(code))
                    break
                except subprocess.TimeoutExpired:
                    resources.append(admit())
                    if time.monotonic()-started>plan['max_seconds']:raise RuntimeError('Teacher allocation exhausted; completed chunks retained')
    except Exception as exc:error=repr(exc);raise
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            try:process.wait(timeout=5)
            except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
        receipt=dict(status='COMPLETE' if error is None else 'STOPPED',error=error,seconds=time.monotonic()-started,resources=resources,process_absent=process is None or process.poll() is not None)
        atomic(out/('execution-'+str(time.time_ns())+'.json'),receipt);print(json.dumps(receipt))


if __name__=='__main__':main()
