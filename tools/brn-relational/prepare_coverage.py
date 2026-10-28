"""Prospective nested 131k training subset; retain the same consumed diagnostic panel."""
import argparse
import json
from pathlib import Path
import random
import time
import chess
from baseline import ROOT,admit,sha
from prepare_objective import alias,unpack


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('output',type=Path);a=p.parse_args()
    resource=admit(initial=True);start=time.monotonic();a.output.mkdir(parents=True,exist_ok=False)
    prior=ROOT/'docs/research/brn-relational-cglhw/raw/r002-data';manifest=json.loads((prior/'manifest.json').read_text())
    for name in ('train.tsv','validation.tsv','selected-indices.json'):
        if sha(prior/name)!=manifest['files'][name]:raise ValueError('Prior subset identity')
    source=Path(manifest['source']);binary=source/'train.data'
    if sha(binary)!=manifest['source_hashes']['train.data']:raise ValueError('Source identity')
    train=(prior/'train.tsv').read_text().splitlines();validation=(prior/'validation.tsv').read_text()
    seen={alias(chess.Board(line.split('\t')[0])) for line in train+validation.splitlines()}
    ids=json.loads((prior/'selected-indices.json').read_text())['train'];used=set(ids)
    rng=random.Random(2026102902);candidates=sorted(rng.sample(range(binary.stat().st_size//32),262144));samples=[resource]
    with binary.open('rb') as stream:
        for ordinal,index in enumerate(candidates):
            if len(train)>=131072:break
            if ordinal%4096==0:
                samples.append(admit())
                if time.monotonic()-start>240:raise RuntimeError('Bounded preparation allocation; partial evidence preserved')
            if index in used:continue
            stream.seek(index*32);board,target=unpack(stream.read(32));key=alias(board)
            if key in seen:continue
            seen.add(key);ids.append(index);train.append(f'{board.fen()}\t{target:.17g}')
    if len(train)!=131072:raise ValueError('Insufficient unique records')
    (a.output/'train.tsv').write_text('\n'.join(train)+'\n');(a.output/'validation.tsv').write_text(validation)
    (a.output/'selected-indices.json').write_text(json.dumps(ids)+'\n')
    result=dict(schema='brn-relational-nested-coverage-v1',train=131072,validation=4096,
        original_train_prefix=16384,source=str(binary),source_sha256=sha(binary),prior_manifest_sha256=sha(prior/'manifest.json'),
        seed=2026102902,files={n:sha(a.output/n) for n in ('train.tsv','validation.tsv','selected-indices.json')},
        seconds=time.monotonic()-start,resources=samples,
        limitations='Consumed development validation; nested training prefix; unknown game identity. Exact color/rank/file aliases purged. No final test.')
    (a.output/'manifest.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))


if __name__=='__main__':main()
