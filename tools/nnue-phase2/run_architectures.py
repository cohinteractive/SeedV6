"""Deterministic architecture pilot->parity->fixed endpoint, one owner job at a time."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
from bootstrap import ROOT, BASE, write, sha
from codec import verify_checkpoint, tensors, KINDS


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('names',nargs='+',choices=['deep16','pawnpairs'])
    p.add_argument('--assessment-revision',type=int,default=1);a=p.parse_args()
    if a.assessment_revision<1:raise ValueError('Invalid assessment revision')
    suffix='' if a.assessment_revision==1 else f'-r{a.assessment_revision}'
    os.chdir(ROOT);python=[sys.executable,'-B'];control=python+['tools/conventional-nnue/compute_control.py']
    template=json.loads((BASE/'a001-replica-pilot.json').read_text())
    original=list((BASE/'raw/a001-replica-pilot/segments').glob('000001-*/run/initial/weights.bin'))
    if len(original)!=1:raise ValueError('Missing exact initializer control')
    original_tensors=tensors(original[0],0)
    def run(command,label):
        driver_log=BASE/f'raw/{label}-driver.log'
        with driver_log.open('xb') as log:
            subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=7200)
    for name in a.names:
        pilot={k:template[k] for k in ('data','probes','seed','batch_size','batches_per_superbatch','learning_rate','wdl','data_sha256','probes_sha256','lr_drop_every','lr_gamma')}
        pilot.update(architecture=name,output=str(BASE/f'raw/b-{name}-pilot'),start_superbatch=1,end_superbatch=4,resume=None)
        plan=BASE/f'b-{name}-pilot.json'
        if plan.exists():assert json.loads(plan.read_text())==pilot
        else:write(plan,pilot)
        for stage in ['pilot','64']:
            if stage=='64':
                config=dict(pilot,output=str(BASE/f'raw/b-{name}-64'),start_superbatch=5,end_superbatch=64,resume=str(cp))
                plan=BASE/f'b-{name}-64.json'
                if plan.exists():assert json.loads(plan.read_text())==config
                else:write(plan,config)
            else:config=pilot
            out=Path(config['output'])
            if not (out/'completed.json').exists():
                run(control+['run','--name',f'phase2-{name}-{stage}','--resume-note','Preserve segments; resume only exact verified plan',
                    '--log',str(BASE/f'raw/b-{name}-{stage}-supervisor.log'),'--']+python+['tools/nnue-phase2/train_architecture.py',str(plan)],f'b-{name}-{stage}')
            completed=json.loads((out/'completed.json').read_text());cp=Path(completed['checkpoint']);verify_checkpoint(cp)
            if stage=='pilot':
                first=list(out.glob('segments/000001-*/run/initial/weights.bin'))
                if len(first)!=1:raise ValueError('Ambiguous first initializer')
                values=tensors(first[0],KINDS[name]);equal={k:values[k].tobytes()==v.tobytes() for k,v in original_tensors.items()}
                if not all(equal.values()):raise ValueError('Base initialization changed; investigate before interpretation')
                record=BASE/f'b-{name}-initializer.json'
                if not record.exists():write(record,dict(reference=str(original[0]),reference_sha256=sha(original[0]),
                    candidate=str(first[0]),candidate_sha256=sha(first[0]),base_tensors_exact=equal))
            assessment=BASE/f'raw/b-{name}-{stage}-assessment{suffix}'
            if not (assessment/'summary.json').exists():
                command=python+['tools/nnue-phase2/assess_architecture.py',str(cp),str(assessment)]+(['--pilot'] if stage=='pilot' else [])
                run(control+['run','--name',f'phase2-{name}-{stage}-assessment','--resume-note','Inspect preserved parity/cost evidence; never overwrite partial output',
                    '--log',str(BASE/f'raw/b-{name}-{stage}-assessment{suffix}.log'),'--']+command,f'b-{name}-{stage}-assessment{suffix}')
            summary=json.loads((assessment/'summary.json').read_text())
            if summary['parity']['status']!='PASS':raise ValueError('No parity pass')
            if summary['parity']['quantization_rms_link_error']>30:raise ValueError('Quantization fidelity needs investigation before continuation')
            record=BASE/f'b-{name}-{stage}-assessment.json'
            if not record.exists():write(record,summary)
        print(name+': endpoint and assessment complete',flush=True)


if __name__=='__main__':main()
