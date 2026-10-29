"""Independent Python reconciliation of R057 compact keys, exact counts and exclusions."""
import json
import mmap
from pathlib import Path
import random
import struct
import time
from baseline import ROOT, sha
from prepare_objective import unpack
from prepare_one_exclusions import compact
import compute_gate


def contains(data, key, count, start=0, stride=24):
    lo, hi = 0, count
    while lo < hi:
        mid = (lo+hi)//2
        actual = struct.unpack_from('>QQQ', data, start+stride*mid)
        if actual < key:
            lo = mid+1
        else:
            hi = mid
    return lo < count and struct.unpack_from('>QQQ', data, start+stride*lo) == key, lo


def main():
    compute_gate.require()
    started=time.monotonic()
    base=ROOT/'docs/research/brn-relational-cglhw'
    folder=base/'raw/r057'
    result=json.loads((folder/'corpus-merge/result.json').read_text())
    for name, field in [('unique.keys','unique_keys_sha256'),('excluded.bitset','excluded_bitset_sha256')]:
        if sha(folder/'corpus-merge'/name)!=result[field]:raise ValueError('Merge output changed')
    header=(folder/'corpus-merge/excluded.bitset').read_bytes()
    magic,n,size=struct.unpack_from('>QII',header)
    if magic!=0x5230353745584231 or n!=33554432 or len(header)!=16+size:raise ValueError('Exclusion schema')
    mask=header[16:];excluded=[]
    for byte,value in enumerate(mask):
        while value:
            bit=(value&-value).bit_length()-1;excluded.append(byte*8+bit);value&=value-1
    if len(excluded)!=result['excluded_samples']:raise ValueError('Mask count')
    histogram={int(k):v for k,v in result['alias_multiplicities'].items()}
    if sum(histogram.values())!=result['exact_canonical_unique'] or sum(k*v for k,v in histogram.items())!=n:raise ValueError('Multiplicity accounting')
    original=json.loads((ROOT/'docs/research/conventional-nnue-cglhw/e006-data-manifest.json').read_text())
    for bucket in range(0,33,4):
        if sum(result['source_piece_counts'][bucket:bucket+4])!=original['counts']['train_pieces_'+str(bucket)]:raise ValueError('Original phase distribution')
    rng=random.Random(2026102957);ids=sorted(set(rng.sample(range(n),2048)+rng.sample(excluded,min(512,len(excluded)))))
    with (ROOT/'docs/research/conventional-nnue-cglhw/raw/e006-data/train.data').open('rb') as source, (folder/'exclusions/reserved.keys').open('rb') as rf, (folder/'corpus-merge/unique.keys').open('rb') as uf:
        with mmap.mmap(rf.fileno(),0,access=mmap.ACCESS_READ) as reservations,mmap.mmap(uf.fileno(),0,access=mmap.ACCESS_READ) as unique:
            count=struct.unpack_from('>I',reservations,8)[0];checked=0
            for shard in range(32):
                subset=[i for i in ids if i//1048576==shard]
                path=folder/'corpus-index'/f'keys-{shard:03d}.bin'
                with path.open('rb') as sf,mmap.mmap(sf.fileno(),0,access=mmap.ACCESS_READ) as indexed:
                    for index in subset:
                        source.seek(index*32);board,target=unpack(source.read(32));key=compact(board)
                        found,offset=contains(indexed,key,1048576,12,28)
                        if not found:raise ValueError('Java/Python canonical key mismatch')
                        members=[]
                        while offset<1048576 and struct.unpack_from('>QQQ',indexed,12+28*offset)==key:
                            members.append(struct.unpack_from('>I',indexed,12+28*offset+24)[0]);offset+=1
                        if index not in members:raise ValueError('Key/source identity mismatch')
                        if not contains(unique,key,result['exact_canonical_unique'])[0]:raise ValueError('Unique merge key missing')
                        marked=index//8<len(mask) and bool(mask[index//8]&(1<<(index%8)))
                        if contains(reservations,key,count,12)[0]!=marked:raise ValueError('Wrong reservation mask')
                        checked+=1
                compute_gate.require()
    report=dict(status='PASS',sampled_rows=checked,forced_excluded_sample=min(512,len(excluded)),seed=2026102957,
        original_records=n,exact_canonical_unique=result['exact_canonical_unique'],canonical_duplicate_samples=result['canonical_duplicate_samples'],
        excluded_samples=result['excluded_samples'],remaining_samples=result['remaining_samples'],remaining_unique=result['remaining_unique'],
        mask_all_bits_counted=True,multiplicity_totals_exact=True,original_phase_totals_exact=True,
        merge_result_sha256=sha(folder/'corpus-merge/result.json'),seconds=time.monotonic()-started,
        limitations='Java merge checks every source ID and sort order; Python independently checks canonical encoding/reservation membership on the frozen sample, not a second full 33M-row decode.')
    target=base/'r057-corpus-audit.json'
    with target.open('x') as out:out.write(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))


if __name__=='__main__':main()
