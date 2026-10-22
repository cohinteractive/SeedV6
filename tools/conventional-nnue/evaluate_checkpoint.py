"""Bounded independent export/parity and diagnostic validation of a frozen checkpoint.

Run through nnue-control.cmd run with the local chess-enabled Python environment.
Preserves commands and hashes; new output directory required, no candidate selection.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time
import compute_gate
from compute_control import verify_checkpoint


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('checkpoint',type=Path);p.add_argument('probes',type=Path)
    p.add_argument('validation',type=Path);p.add_argument('output',type=Path)
    p.add_argument('--material',type=Path)
    a=p.parse_args();compute_gate.require();receipt,_=verify_checkpoint(a.checkpoint)
    a.output.mkdir(parents=True,exist_ok=False)
    root=Path(__file__).resolve().parents[2];tools=root/'tools/conventional-nnue'
    network=a.output/'network.cnnue'
    commands=[];start=time.monotonic()
    def run(command,log):
        compute_gate.require();commands.append(dict(command=command,output=str(log)))
        with log.open('xb') as stream:subprocess.run(command,stdout=stream,stderr=subprocess.STDOUT,check=True)
    python=[sys.executable,'-B']
    java=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=1','-Xmx512m',
          '-cp',str(root/'research/build/install/research/lib/*'),'com.ohinteractive.seedv6.tools.eval.ConventionalNnueProbe',str(network)]
    run(python+[str(tools/'export_network.py'),str(a.checkpoint/'quantised.bin'),str(network),
        '--fens',str(a.probes),'--reference',str(a.output/'python.jsonl')],a.output/'export.log')
    run([str(root/'build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe'),
         'features',str(a.probes)],a.output/'bullet.jsonl')
    run(java+[str(a.probes)],a.output/'java.jsonl')
    run(python+[str(tools/'verify_parity.py'),'--python',str(a.output/'python.jsonl'),
        '--java',str(a.output/'java.jsonl'),'--bullet',str(a.output/'bullet.jsonl'),
        '--gpu',str(a.checkpoint.parent/'gpu-probes.jsonl'),'--checkpoint',str(a.checkpoint),
        '--output',str(a.output/'parity.json')],a.output/'parity.log')
    run(java+['--validation',str(a.validation)],a.output/'validation.json')
    if a.material:run(java+[str(a.material)],a.output/'material.jsonl')
    manifest=dict(checkpoint=receipt,commands=commands,elapsed_seconds=time.monotonic()-start,
        files={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in a.output.iterdir() if p.is_file()})
    (a.output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print((a.output/'parity.json').read_text());print((a.output/'validation.json').read_text())


if __name__=='__main__':main()
