"""Bounded, resumable one-superbatch architecture processes inside owner supervision."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
import uuid
from bootstrap import ROOT, BASE, write, sha
from codec import verify_checkpoint, tensors, KINDS
import compute_gate


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('config',type=Path)
    p.add_argument('--trainer',type=Path,default=ROOT/'build/conventional-nnue/cargo-target/release/seed-phase2-nnue.exe');a=p.parse_args()
    compute_gate.require();source=a.config.resolve();plan=json.loads(source.read_text());out=Path(plan['output'])
    binary=a.trainer.resolve()
    if out.exists():
        if (out/'requested-config.json').read_bytes()!=source.read_bytes():raise ValueError('Changed training plan')
        if json.loads((out/'execution.json').read_text())['trainer_sha256']!=sha(binary):raise ValueError('Changed trainer')
    else:
        out.mkdir(parents=True);(out/'requested-config.json').write_bytes(source.read_bytes())
        write(out/'execution.json',dict(trainer_sha256=sha(binary),config_sha256=sha(source),
              source_sha256=sha(ROOT/'tools/conventional-nnue/trainer/src/phase2.rs'),
              bullet='579b78b37a694cacd6e9ede4706386c15c34837c',control_operation=os.environ['SEED_NNUE_OPERATION']))
    previous=Path(plan['resume']) if plan['resume'] else None;elapsed=0.
    if previous:verify_checkpoint(previous)
    for step in range(plan['start_superbatch'],plan['end_superbatch']+1):
        compute_gate.require()
        complete=list(out.glob(f'segments/{step:06d}-*/run/seed-phase2-{step}/receipt.json'))
        if len(complete)>1:raise ValueError('Ambiguous completed segment')
        if complete:
            cp=complete[0].parent;config,_=verify_checkpoint(cp)
            if config.get('resume')!=(str(previous) if previous else None):raise ValueError('Broken continuation chain')
            previous=cp;elapsed+=json.loads((cp.parent.parent/'verified.json').read_text())['wall_seconds'];continue
        if elapsed>=3600:raise RuntimeError('Architecture native wall budget reached; reassess before extending')
        attempt=out/'segments'/f'{step:06d}-{uuid.uuid4().hex[:12]}';attempt.mkdir(parents=True)
        config=dict(plan,output=str(attempt/'run'),start_superbatch=step,end_superbatch=step,resume=str(previous) if previous else None)
        write(attempt/'config.json',config);start=time.monotonic()
        with (attempt/'console.log').open('xb') as log:
            subprocess.run([str(binary),'train',str(attempt/'config.json')],stdout=log,stderr=subprocess.STDOUT,check=True,timeout=300)
        elapsed+=time.monotonic()-start;cp=attempt/'run'/f'seed-phase2-{step}';_,receipt=verify_checkpoint(cp)
        if previous:
            for name in ('weights','momentum','velocity'):
                if sha(previous/f'optimiser_state/{name}.bin')!=sha(cp.parent/f'initial/{name}.bin'):
                    raise ValueError('Optimizer continuity mismatch')
        write(attempt/'verified.json',dict(wall_seconds=time.monotonic()-start,receipt_sha256=sha(cp/'receipt.json'),
              continuation='exact tensors verified' if previous else 'fresh initializer',checkpoint=str(cp)))
        previous=cp
    completed=dict(checkpoint=str(previous),receipt_sha256=sha(previous/'receipt.json'),native_wall_seconds=elapsed)
    if (out/'completed.json').exists():
        old=json.loads((out/'completed.json').read_text())
        if old['checkpoint']!=completed['checkpoint'] or old['receipt_sha256']!=completed['receipt_sha256']:
            raise ValueError('Unexpected completion record')
    else:write(out/'completed.json',completed)
    print(json.dumps(dict(completed=str(previous),native_wall_seconds=elapsed)),flush=True)


if __name__=='__main__':main()
