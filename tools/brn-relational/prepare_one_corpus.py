"""Freeze the logical full-scale corpus and a development-only original validation sample."""
import json
import random
import struct
import subprocess
from baseline import ROOT, sha
import compute_gate


def main():
    compute_gate.require()
    base=ROOT/'docs/research/brn-relational-cglhw';raw=base/'raw/r057';original=ROOT/'docs/research/conventional-nnue-cglhw/raw/e006-data'
    old=json.loads((original/'manifest.json').read_text());merge=json.loads((raw/'corpus-merge/result.json').read_text())
    supplement=json.loads((raw/'supplement/manifest.json').read_text());audit=json.loads((base/'r057-supplement-audit.json').read_text())
    assert audit['status']=='PASS' and audit['manifest_sha256']==sha(raw/'supplement/manifest.json')
    for name in ['train.data','validation.txt']:
        assert sha(original/name)==old['hashes'][name];compute_gate.require()
    mask=(raw/'corpus-merge/excluded.bitset').read_bytes()
    assert sha(raw/'corpus-merge/excluded.bitset')==merge['excluded_bitset_sha256']
    magic,n,size=struct.unpack_from('>QII',mask)
    assert magic==0x5230353745584231 and n==33554432 and len(mask)==16+size
    assert sum(v.bit_count() for v in mask[16:])==supplement['training_replacements']==39396
    out=raw/'corpus';out.mkdir(exist_ok=False)
    seed=20261029058;ids=sorted(random.Random(seed).sample(range(524288),8192));wanted=set(ids)
    with (original/'validation.txt').open() as source,(out/'development.txt').open('x') as target:
        for i,line in enumerate(source):
            if i in wanted:target.write(line)
    trainer=ROOT/'build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe'
    with (out/'conversion.log').open('xb') as log:
        subprocess.run([str(trainer),'convert',str(out/'development.txt'),str(out/'development.data')],stdout=log,stderr=subprocess.STDOUT,check=True)
    assert (out/'development.data').stat().st_size==8192*32
    (out/'development-indices.json').write_text(json.dumps(ids)+'\n')
    paths=dict(original=original/'train.data',mask=raw/'corpus-merge/excluded.bitset',replacement=raw/'supplement/topup.data',development=out/'development.data',fresh_validation=raw/'supplement/fresh-validation.data')
    result=dict(schema='r057-logical-corpus-v1',samples=n,training_canonical_unique=merge['remaining_unique']+39396,
                canonical_duplicate_samples=n-merge['remaining_unique']-39396,retained_original=merge['remaining_samples'],replacements=39396,
                development_samples=8192,fresh_validation_samples=8192,development_seed=seed,
                paths={k:str(v.relative_to(ROOT)) for k,v in paths.items()},hashes={k:sha(v) for k,v in paths.items()},
                source_manifest_sha256=sha(original/'manifest.json'),merge_result_sha256=sha(raw/'corpus-merge/result.json'),supplement_manifest_sha256=sha(raw/'supplement/manifest.json'),
                development_indices_sha256=sha(out/'development-indices.json'),trainer_sha256=sha(trainer),
                mapping='Every original row index remains one logical sample. Set mask bits replace that row by the topup ordinal equal to the count of preceding set bits. Every original excluded row is replaced once; all other bytes unchanged.',
                sampling='Pilot: 16 of 512 shuffled 65536-row blocks, seed 20261029059. Main: all 512 blocks. Epochs permute selected blocks and every row within each block bijectively.',
                limitations='0.1174% replacement; original duplicates retained. No source game IDs. Development shares original CP64 validation history; fresh 8192 remains reserved until configuration freeze. No byte-identical corpus claim.')
    for p in [out/'manifest.json',base/'r057-training-corpus.json']:
        with p.open('x') as f:f.write(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ['samples','training_canonical_unique','canonical_duplicate_samples','retained_original','replacements']},indent=2))


if __name__=='__main__':main()
