"""R036 exactly half old labels, half root-uniform new labels; no development sampling."""
import json
from pathlib import Path
import random
import time
import chess
from baseline import ROOT, admit, sha
from prepare_objective import alias


def main():
    started=time.monotonic();samples=[admit(initial=True)];last=started
    def check():
        nonlocal last
        if time.monotonic()-last>=5:samples.append(admit());last=time.monotonic()
        if time.monotonic()-started>180:raise RuntimeError('Mixture preparation allocation exhausted')
    folder=ROOT/'docs/research/brn-relational-cglhw'
    evidence=json.loads((folder/'r035-results.json').read_text());assert evidence['training_allocation_justified']
    spec=evidence['datasets']['train'];dev=evidence['datasets']['validation']
    for s in (spec,dev):
        for path_key,hash_key in (('positions','positions_sha256'),('labeled','labeled_sha256')):assert sha(ROOT/s[path_key])==s[hash_key]
    positions=json.loads((ROOT/spec['positions']).read_text());lines=(ROOT/spec['labeled']).read_text().splitlines()
    development={p['alias'] for p in json.loads((ROOT/dev['positions']).read_text())}
    groups={};aliases=[]
    for i,(position,line) in enumerate(zip(positions,lines)):
        fen,target=line.split('\t');assert fen==position['fen'] and 0<=float(target)<=1
        key=alias(chess.Board(fen));assert key==position['alias'] and key not in development;aliases.append(key)
        roots=sorted({(source['seed'],source['opening']) for source in position['sources']})
        assert roots and all(root[1]<8 for root in roots)
        for root in roots:groups.setdefault(root,[]).append(i)
    assert len(aliases)==len(set(aliases))==len(lines)==2065 and len(groups)==16
    old=folder/'raw/r007-teacher-data/train/data.tsv';old_hash=sha(old)
    assert old_hash=='b9e01e578c2765a77a906b818d8ba5595d7c3f64156674c4dccd2f80a5cde63a'
    out=folder/'raw/r036-data';out.mkdir(exist_ok=False)
    mixture=out/'train.tsv';draws=out/'draws.json';rng=random.Random(2026102936);roots=sorted(groups)
    selected=[];root_counts={str(root):0 for root in roots};counts=[0]*len(lines)
    with old.open() as source,mixture.open('x') as target:
        for i,line in enumerate(source):
            if i%4096==0:check()
            if i%2==0:target.write(line)
            else:
                root=roots[rng.randrange(len(roots))];index=rng.choice(groups[root]);target.write(lines[index]+'\n')
                selected.append([root[0],root[1],index]);root_counts[str(root)]+=1;counts[index]+=1
    assert i+1==1048576 and len(selected)==524288
    with draws.open('x') as target:json.dump(selected,target);target.write('\n')
    # Independent streaming verification of old-row preservation and selected labels.
    with old.open() as source,mixture.open() as target:
        for i,(original,mixed) in enumerate(zip(source,target)):
            if i%4096==0:check()
            if i%2==0:assert original==mixed
            else:
                seed,opening,index=selected[i//2];assert index in groups[(seed,opening)]
                assert mixed.rstrip('\r\n')==lines[index]
        assert source.readline()=='' and target.readline()==''
    assert i+1==1048576 and all(counts)
    result=dict(schema='root-grouped-neural-mixture-v1',rows=1048576,old_rows=524288,new_rows=524288,seed=2026102936,
                source_evidence_sha256=sha(folder/'r035-results.json'),old_path=str(old.relative_to(ROOT)),old_sha256=old_hash,
                new_path=spec['labeled'],new_sha256=spec['labeled_sha256'],new_positions_sha256=spec['positions_sha256'],
                validation_sha256=dev['labeled_sha256'],output=str(mixture.relative_to(ROOT)),output_sha256=sha(mixture),
                draws=str(draws.relative_to(ROOT)),draws_sha256=sha(draws),root_counts=root_counts,
                unique_new_aliases=len(lines),minimum_alias_draws=min(counts),maximum_alias_draws=max(counts),
                selector_sha256=sha(Path(__file__)),seconds=time.monotonic()-started,resources=samples,
                verification='Exact parity of every retained old row and all selected new labels;disjoint aliases;16 train roots;no validation draws.')
    with (folder/'r036-mixture.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps({k:v for k,v in result.items() if k!='resources'},indent=2))


if __name__ == '__main__':
    main()
