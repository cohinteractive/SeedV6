"""Serial CPU objective diagnostics with resource gates and per-epoch recovery."""
import argparse
import json
from pathlib import Path
import subprocess
import time
from baseline import ROOT, admit, sha
from compute_control import atomic


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('plan',type=Path);a=p.parse_args()
    plan=json.loads(a.plan.read_text());runtime=ROOT/plan['runtime'];receipts=[]
    main_class=plan.get('main_class','PairObjectiveDiagnostic')
    if main_class not in ('PairObjectiveDiagnostic','GraphEnergyDiagnostic','MessageDiagnostic','ContextPairDiagnostic','PairCoverageDiagnostic','AnchorDiagnostic','AnchorCoverageDiagnostic','ContextualAttackDiagnostic','AttackFieldDiagnostic','ContextualContinuationDiagnostic'):raise ValueError('Unknown diagnostic')
    hashes={str(x.relative_to(ROOT)):sha(x) for x in sorted(runtime.glob('*.jar'))}
    if not hashes:raise ValueError('No frozen runtime')
    for config_path in plan['configs']:
        resource=admit(initial=True);config_path=ROOT/config_path;config=json.loads(config_path.read_text());out=ROOT/config['output']
        binding=dict(config_sha256=sha(config_path),runtime_sha256=hashes,runner_sha256=sha(Path(__file__)))
        out.mkdir(parents=True,exist_ok=True);bound=out/'binding.json'
        if bound.exists():
            if json.loads(bound.read_text())!=binding:raise ValueError('Frozen run binding changed')
        else:atomic(bound,binding)
        # Completed arms are immutable; check every checkpoint before skipping.
        if (out/'result.json').exists():
            for epoch in range(config.get('initial_epoch',0)+1,config['epochs']+1):
                r=json.loads((out/f'epoch-{epoch}.json').read_text())
                formats=[('state','state_sha256')]
                if main_class in ('PairObjectiveDiagnostic','PairCoverageDiagnostic'):formats.append(('brn-pair2','model_sha256'))
                for ext,key in formats:
                    if sha(out/f'epoch-{epoch}.{ext}')!=r[key]:raise ValueError('Checkpoint corruption')
            receipts.append(dict(config=str(config_path),status='verified-complete',result_sha256=sha(out/'result.json')));continue
        command=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=1','-XX:+UseSerialGC','-Xmx512m',
            '-cp',str(runtime/'*'),'com.ohinteractive.seedv6.core.brnpair2.'+main_class,str(config_path)]
        start=time.monotonic();process=None;samples=[resource];status='FAILED';error=None
        try:
            with (out/('attempt-'+str(time.time_ns())+'.log')).open('x') as log:
                process=subprocess.Popen(command,stdout=log,stderr=subprocess.STDOUT,
                    creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
                while True:
                    try:
                        code=process.wait(timeout=5)
                        if code:raise RuntimeError('Java exit '+str(code))
                        break
                    except subprocess.TimeoutExpired:
                        samples.append(admit())
                        if time.monotonic()-start>plan['max_seconds_per_arm']:raise RuntimeError('Bounded arm allocation exhausted; retain epochs')
            status='COMPLETE'
        except Exception as exc:
            error=repr(exc);raise
        finally:
            if process is not None and process.poll() is None:
                process.terminate()
                try:process.wait(timeout=5)
                except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
            r=dict(config=str(config_path),status=status,error=error,elapsed_seconds=time.monotonic()-start,resources=samples,
                command=command,last_complete_epoch=max([int(f.stem.split('-')[1]) for f in out.glob('epoch-*.json')],default=0),
                process_absent=process is None or process.poll() is not None)
            atomic(out/('execution-'+str(time.time_ns())+'.json'),r);receipts.append(r)
            atomic(ROOT/plan['summary'],dict(arms=receipts))
    print(json.dumps(receipts,indent=2))


if __name__=='__main__':main()
