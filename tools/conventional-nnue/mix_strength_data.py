"""Bounded, deterministic 50:50 batch mixture of two existing BT4 extractions.

Never modifies sources. Reuses the first source's validation for a subsequent
global separation audit. Source chunks must be disjoint; cross-source placement
dedup is not claimed. Native records and their 12-byte provenance stay aligned.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import compute_gate


def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('first',type=Path);p.add_argument('second',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args();compute_gate.require();sources=[a.first,a.second]
    manifests=[json.loads((s/'manifest.json').read_text()) for s in sources]
    if manifests[0]['prepared_identity']!=manifests[1]['prepared_identity']:raise ValueError('Different prepared sources')
    units=[{u['index'] for u in m['units']} for m in manifests]
    if units[0]&units[1]:raise ValueError('Source chunk overlap')
    counts=[m['train'] for m in manifests]
    if counts[0]!=counts[1] or not 0<counts[0]<=33554432 or counts[0]%4096:raise ValueError('Unsupported mixture geometry')
    for s,m in zip(sources,manifests):
        for name in ('train.data','train-provenance.bin','validation.txt'):
            compute_gate.require()
            if sha(s/name)!=m['hashes'][name]:raise ValueError('Source identity changed')
    a.output.mkdir(parents=True,exist_ok=False)
    for name,size in [('train.data',32),('train-provenance.bin',12)]:
        with (sources[0]/name).open('rb') as left,(sources[1]/name).open('rb') as right,(a.output/name).open('xb') as output:
            for batch in range(counts[0]//4096):
                if batch%64==0:compute_gate.require()
                for stream in (left,right):
                    data=stream.read(4096*size)
                    if len(data)!=4096*size:raise ValueError('Truncated source')
                    output.write(data)
            if left.read(1) or right.read(1):raise ValueError('Unexpected trailing records')
    shutil.copyfile(sources[0]/'validation.txt',a.output/'validation.txt')
    for s,m in zip(sources,manifests):
        for name in ('train.data','train-provenance.bin','validation.txt'):
            compute_gate.require()
            if sha(s/name)!=m['hashes'][name]:raise ValueError('Source changed during mixture')
    hashes={name:sha(a.output/name) for name in ('train.data','train-provenance.bin','validation.txt')}
    manifest=dict(schema='seed-bt4-batch-mixture-v1',train=sum(counts),validation=manifests[0]['validation'],
                  prepared_identity=manifests[0]['prepared_identity'],hashes=hashes,
                  sources=[dict(path=str(s),manifest_sha256=sha(s/'manifest.json'),train=m['train'],hashes=m['hashes']) for s,m in zip(sources,manifests)],
                  selection='Alternate complete 4096-record minibatches from first then second; preserve each already shuffled source order; 50:50 fixed mixture.',
                  provenance='12-byte global prepared-unit/ordinal/label records, interleaved identically to train.data. Global unit IDs are shared and verified disjoint.',
                  limitations=['No cross-source placement dedup or source-game independence claim.','Validation must be audited against the complete mixture before diagnostic use.'])
    with (a.output/'manifest.json').open('x',encoding='utf-8') as out:json.dump(manifest,out,indent=2);out.write('\n')
    print(json.dumps(manifest,indent=2))


if __name__=='__main__':main()
