"""Independent BT4 link/packed-label validation and upstream packing comparison.

The proof sample is distributed across sampled units, not a complete board audit.
Every training target/result and provenance ordinal is checked. No strength claim.
"""
import argparse
import hashlib
import json
import math
import mmap
from pathlib import Path
import struct
import subprocess
import compute_gate


def sha(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('dataset',type=Path);p.add_argument('output',type=Path)
    p.add_argument('--trainer',type=Path,default=Path('build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe'))
    a=p.parse_args();compute_gate.require();m=json.loads((a.dataset/'manifest.json').read_text())
    if m['schema']!='seed-bt4-bullet-v1':raise ValueError('Wrong schema')
    for name,digest in m['hashes'].items():
        compute_gate.require()
        if Path(name).name!=name or sha(a.dataset/name)!=digest:raise ValueError('Dataset identity changed')
    # Independently invert the source formula, then quantize the link.
    links={};qs={};max_error=0
    for score in range(32001):
        lo,hi=0.,1.
        for _ in range(56):
            q=(lo+hi)/2
            if 660.6*q/(1-.9751875*q**10)<score:lo=q
            else:hi=q
        q=(lo+hi)/2 if score else 0.
        for sign in (-1,1):
            prob=max(.0001,min(.9999,(1+q*sign)/2))
            link=math.floor(400*math.log(prob/(1-prob))+.5)
            links[score*sign]=link;qs[score*sign]=q*sign
            max_error=max(max_error,abs(1/(1+math.exp(-link/400))-((1+q*sign)/2)))
    units={u['index']:u for u in m['units'] if u['split']=='train'}
    counts={};n=0
    with (a.dataset/'train.data').open('rb') as bf,(a.dataset/'train-provenance.bin').open('rb') as pf:
        if (a.dataset/'train.data').stat().st_size!=m['train']*32 or (a.dataset/'train-provenance.bin').stat().st_size!=m['train']*12:
            raise ValueError('Wrong lengths')
        with mmap.mmap(bf.fileno(),0,access=mmap.ACCESS_READ) as b,mmap.mmap(pf.fileno(),0,access=mmap.ACCESS_READ) as raw:
            for n,(unit,ordinal,encoded,result,pad) in enumerate(struct.iter_unpack('<IIhbB',raw),1):
                if n%65536==0:compute_gate.require()
                entry=units[unit]
                if not 0<=ordinal<entry['unit']['count'] or pad!=0 or result not in (-1,0,1):raise ValueError('Invalid provenance')
                score,actual_result=struct.unpack_from('<hB',b,(n-1)*32+24)
                if score!=links[encoded] or actual_result!=result+1:raise ValueError('Target/result mismatch')
                counts[unit]=counts.get(unit,0)+1
    if n!=m['train'] or any(counts.get(i,0)!=u['accepted'] for i,u in units.items()):raise ValueError('Count mismatch')
    proof=0
    with (a.dataset/'proof.txt').open() as texts,(a.dataset/'proof-labels.jsonl').open() as labels:
        for text,label in zip(texts,labels,strict=True):
            r=json.loads(label);fen,score,result=text.strip().split(' | ');sign=1 if fen.split()[1]=='w' else -1
            if int(score)*sign!=links[r['encoded']] or abs(qs[r['encoded']]-r['q'])>1e-12 or float(result)!=(sign*r['stm_result']+1)/2:
                raise ValueError('Proof target perspective mismatch')
            proof+=1
    a.output.mkdir(parents=True,exist_ok=False);compute_gate.require()
    with (a.output/'conversion.log').open('xb') as f:
        subprocess.run([str(a.trainer),'convert',str(a.dataset/'proof.txt'),str(a.output/'upstream.data')],stdout=f,stderr=subprocess.STDOUT,check=True)
    if sha(a.output/'upstream.data')!=sha(a.dataset/'proof.data') or proof!=m['proof_records']:raise ValueError('Upstream packing mismatch')
    result=dict(status='PASS',training_records=n,proof_records=proof,all_training_links_and_results_verified=True,
        all_frozen_file_hashes_verified=True,proof_packing_identical_to_upstream=True,
        maximum_probability_error_over_encoding_range=max_error,manifest_sha256=sha(a.dataset/'manifest.json'),
        trainer_sha256=sha(a.trainer),limitation='All stored labels/provenance checked; board packing compared with upstream on distributed proof sample. Reuses existing tested BINP decoder, without independent source redecoding.')
    (a.output/'result.json').write_text(json.dumps(result,indent=2)+'\n',newline='\n');print(json.dumps(result,indent=2))


if __name__=='__main__':main()
