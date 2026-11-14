"""Execute the prospective B003 compact128 trio after build/runtime qualification."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import re
import statistics
import time
import uuid
from bootstrap import ROOT, BASE, write, sha
from codec import verify_checkpoint, tensors, KINDS


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--runtime-version',type=int,required=True)
    p.add_argument('--assessment-revision',type=int,default=1);a=p.parse_args();os.chdir(ROOT);started=time.monotonic()
    runtime_path=BASE/f'frozen-runtime-v{a.runtime_version}.json';runtime=json.loads(runtime_path.read_text())
    for name,digest in runtime['files'].items():assert sha(Path(runtime['path'])/name)==digest
    binary=ROOT/'build/conventional-nnue/cargo-target/release/seed-phase2-compact.exe';source=ROOT/'tools/conventional-nnue/trainer/src/phase2_compact.rs'
    frozen=BASE/'raw'/('trainer-compact128-'+sha(binary)+'.exe');record=BASE/'compact-trainer.json'
    if not frozen.exists():shutil.copyfile(binary,frozen)
    assert sha(frozen)==sha(binary)
    provenance=dict(path=str(frozen),sha256=sha(frozen),source=str(source),source_sha256=sha(source),runtime_manifest_sha256=sha(runtime_path))
    if record.exists():assert json.loads(record.read_text())==provenance
    else:write(record,provenance)
    python=[sys.executable,'-B'];control=python+[str(ROOT/'tools/conventional-nnue/compute_control.py')]
    template=json.loads((BASE/'a001-replica-pilot.json').read_text());reference=None;spent=0.
    def run(command,label):
        remaining=7200-(time.monotonic()-started)
        if remaining<=0:raise RuntimeError('B003 supervisor budget reached; inspect preserved evidence')
        nonce=uuid.uuid4().hex[:12]
        if '--log' in command:
            command=command.copy();at=command.index('--log')+1;old=Path(command[at]);command[at]=str(old.with_name(old.stem+'-'+nonce+old.suffix))
        with (BASE/f'raw/{label}-driver-{nonce}.log').open('xb') as log:subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=remaining)
    for name in ['base','deep16','pawnpairs']:
        pilot={k:template[k] for k in ('data','probes','seed','batch_size','batches_per_superbatch','learning_rate','wdl','data_sha256','probes_sha256','lr_drop_every','lr_gamma')}
        pilot.update(architecture=name,hidden=128,output=str(BASE/f'raw/compact128-{name}-pilot'),start_superbatch=1,end_superbatch=4,resume=None)
        cp=None
        for stage in ['pilot','64']:
            config=pilot if stage=='pilot' else dict(pilot,output=str(BASE/f'raw/compact128-{name}-64'),start_superbatch=5,end_superbatch=64,resume=str(cp))
            plan=BASE/f'compact128-{name}-{stage}.json'
            if plan.exists():assert json.loads(plan.read_text())==config
            else:write(plan,config)
            out=Path(config['output'])
            if not (out/'completed.json').exists():
                if spent>=3600:raise RuntimeError('B003 native budget reached; reassess before further training')
                run(control+['run','--name',f'phase2-compact128-{name}-{stage}','--resume-note','Resume only exact compact recipe and verified checkpoint chain',
                    '--log',str(BASE/f'raw/compact128-{name}-{stage}.log'),'--']+python+['tools/nnue-phase2/train_architecture.py',str(plan),
                    '--trainer',str(frozen),'--trainer-source',str(source),'--max-native-seconds',str(3600-spent)],f'compact128-{name}-{stage}')
            completed=json.loads((out/'completed.json').read_text());cp=Path(completed['checkpoint']);verify_checkpoint(cp);spent+=completed['native_wall_seconds']
            if stage=='pilot':
                first=list(out.glob('segments/000001-*/run/initial/weights.bin'));assert len(first)==1
                values=tensors(first[0],KINDS[name],128)
                if reference is None:reference={k:v for k,v in values.items()}
                equal={k:values[k].tobytes()==v.tobytes() for k,v in reference.items()}
                if not all(equal.values()):raise ValueError('Compact base initializers differ')
                initials=BASE/f'compact128-{name}-initializer.json'
                if not initials.exists():write(initials,dict(path=str(first[0]),sha256=sha(first[0]),base_tensors_exact=equal))
            assessment=BASE/f'raw/compact128-{name}-{stage}-assessment-r{a.assessment_revision}'
            if not (assessment/'summary.json').exists():
                run(control+['run','--name',f'phase2-compact128-{name}-{stage}-assessment','--resume-note','Preserve all diagnostics; parity and cost gate games',
                    '--log',str(assessment)+'.log','--']+python+['tools/nnue-phase2/assess_architecture.py',str(cp),str(assessment),
                    '--runtime',runtime['path'],'--trainer',str(frozen)]+(['--pilot'] if stage=='pilot' else []),f'compact128-{name}-{stage}-assessment')
            result=json.loads((assessment/'summary.json').read_text())
            assert result['parity']['status']=='PASS' and result['parity']['quantization_rms_link_error']<=30
            report=BASE/f'compact128-{name}-{stage}-assessment.json'
            if not report.exists():write(report,result)
        print('Compact128 endpoint and assessment complete:',name,flush=True)
    curves={}
    for name in ('base','deep16','pawnpairs'):
        values=[];seconds=0.
        for stage in ('pilot','64'):
            for verified in sorted((BASE/f'raw/compact128-{name}-{stage}').glob('segments/*/verified.json')):
                config=json.loads((verified.parent/'config.json').read_text());step=config['end_superbatch']
                transcript=re.sub(r'\x1b\[[0-9;]*[A-Za-z]','',(verified.parent/'console.log').read_text(encoding='utf-8'))
                losses=re.findall(r'running loss\s+([0-9.]+)',transcript)
                if not losses:raise ValueError('Missing native training loss')
                seconds+=json.loads(verified.read_text())['wall_seconds']
                values.append(dict(superbatch=step,loss=float(losses[-1]),config_sha256=sha(verified.parent/'config.json')))
        values.sort(key=lambda row:row['superbatch']);assert [row['superbatch'] for row in values]==list(range(1,65))
        curves[name]=dict(superbatches=values,native_wall_seconds=seconds,
                         reported_16_superbatch_means=[statistics.mean(v['loss'] for v in values[i:i+16]) for i in range(0,64,16)])
    curve_path=BASE/'compact128-curves.json'
    if curve_path.exists():assert json.loads(curve_path.read_text())==curves
    else:write(curve_path,curves)
    if not (BASE/'compact128-cost.json').exists():
        run(control+['run','--name','phase2-compact128-cost','--resume-note','Warmed fixed-root cost and unchanged-payload search parity; no games or strength claims',
            '--log',str(BASE/'raw/compact128-cost.log'),'--']+python+['tools/nnue-phase2/compare_compact_costs.py','--runtime-version',str(a.runtime_version)],'compact128-cost')
    print('Compact128 training, qualification and comparative cost complete; strength selection remains separate.',flush=True)


if __name__=='__main__':main()
