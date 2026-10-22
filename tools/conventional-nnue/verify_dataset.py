"""Check frozen extraction bytes and every Bullet score conversion independently."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import compute_gate


def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('dataset',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args();compute_gate.require()
    manifest=json.loads((a.dataset/'manifest.json').read_text())
    if manifest.get('stopped_for_compute_hold') or manifest.get('completed_requested_sample') is False:
        raise ValueError('Partial extraction cannot be accepted as the planned dataset')
    for name,digest in manifest['hashes'].items():
        compute_gate.require()
        if Path(name).name!=name:raise ValueError('Unexpected manifest path')
        if sha(a.dataset/name)!=digest:raise ValueError('Frozen extraction changed: '+name)
    binary=a.dataset/'train.data'
    if binary.stat().st_size!=manifest['train']*32:raise ValueError('Wrong binary record count')
    count=0
    with binary.open('rb') as packed,(a.dataset/'train.txt').open() as text:
        for line in text:
            if count%4096==0:compute_gate.require()
            fen,cp,_=line.strip().split(' | ')
            actual=struct.unpack_from('<h',packed.read(32),24)[0]
            expected=int(cp)*(1 if fen.split()[1]=='w' else -1)
            if actual!=expected:raise ValueError(f'Target perspective/score mismatch at record {count}')
            count+=1
        if packed.read() or count!=manifest['train']:raise ValueError('Truncated/misaligned conversion')
    result=dict(status='PASS',records=count,binary_sha256=sha(binary),
        extraction_manifest_sha256=sha(a.dataset/'manifest.json'),all_extraction_hashes_verified=True,
        all_binary_target_scores_verified=True,local_evidence=str(a.dataset),
        limitation='Checks recorded extraction identities and white-to-STM target conversion, not every packed board feature or source-label calibration')
    with a.output.open('x',encoding='utf-8',newline='\n') as out:json.dump(result,out,indent=2);out.write('\n')
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
