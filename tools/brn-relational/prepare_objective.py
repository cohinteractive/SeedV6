"""Small frozen, symmetry-grouped development sample from verified existing BT4 data."""
import argparse
import json
import math
from pathlib import Path
import random
import struct
import sys
import chess
from baseline import ROOT, admit, sha


def alias(board):
    variants=(board,board.mirror(),board.transform(chess.flip_horizontal),board.mirror().transform(chess.flip_horizontal))
    return min(b.board_fen()+(' w' if b.turn else ' b') for b in variants)


def unpack(record):
    if len(record)!=32: raise ValueError('Truncated Bullet record')
    occupancy=struct.unpack_from('<Q',record)[0];board=chess.Board(None);count=0
    for square in chess.scan_forward(occupancy):
        code=(record[8+count//2]>>(4*(count%2)))&15;count+=1
        if (code&7)>5:raise ValueError('Invalid piece code')
        board.set_piece_at(square,chess.Piece((code&7)+1,not bool(code&8)))
    board.turn=chess.WHITE
    if not board.is_valid():raise ValueError('Invalid decoded placement')
    if board.king(chess.WHITE)!=record[27] or board.king(chess.BLACK)!=(record[28]^56):raise ValueError('King metadata mismatch')
    cp=struct.unpack_from('<h',record,24)[0]
    return board,1/(1+math.exp(-cp/400))


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('output',type=Path);a=p.parse_args()
    resource=admit(initial=True);a.output.mkdir(parents=True,exist_ok=False)
    source=ROOT/'docs/research/conventional-nnue-cglhw/raw/e006-data'
    manifest=json.loads((source/'manifest.json').read_text());hashes={}
    for name in ('train.data','validation.txt'):
        hashes[name]=sha(source/name)
        if hashes[name]!=manifest['hashes'][name]:raise ValueError('Source hash mismatch '+name)
        admit()
    rng=random.Random(20261029);seen=set();selected={'train':[],'validation':[]};provenance={'train':[],'validation':[]}
    wanted=set(rng.sample(range(manifest['validation']),8192))
    with (source/'validation.txt').open() as stream:
        for index,line in enumerate(stream):
            if index not in wanted or len(selected['validation'])>=4096:continue
            fen,cp,_=line.strip().split(' | ');b=chess.Board(fen);key=alias(b)
            if key in seen:continue
            seen.add(key);q=1/(1+math.exp(-float(cp)*(1 if b.turn else -1)/400))
            selected['validation'].append((b.fen(),q));provenance['validation'].append(index)
    indices=sorted(rng.sample(range(manifest['train']),32768))
    with (source/'train.data').open('rb') as stream:
        for index in indices:
            if len(selected['train'])>=16384:break
            stream.seek(index*32);b,q=unpack(stream.read(32));key=alias(b)
            if key in seen:continue
            seen.add(key);selected['train'].append((b.fen(),q));provenance['train'].append(index)
    assert len(selected['train'])==16384 and len(selected['validation'])==4096
    for name,rows in selected.items():
        with (a.output/(name+'.tsv')).open('x') as stream:
            for fen,q in rows:stream.write(f'{fen}\t{q:.17g}\n')
    (a.output/'selected-indices.json').write_text(json.dumps(provenance)+'\n')
    report=dict(schema='brn-relational-objective-sample-v1',source=str(source),source_hashes=hashes,
        source_manifest_sha256=sha(source/'manifest.json'),seed=20261029,train=16384,validation=4096,
        files={name:sha(a.output/name) for name in ('train.tsv','validation.tsv','selected-indices.json')},resources=resource,
        grouping='placement+STM plus color/rank and horizontal equivalents; validation first; unique within/across subset',
        target='STM sigmoid(BT4 inverse-Q logit400 / 400); validation source CP was White-relative',
        limitations='consumed development data, no game IDs; not independent of CP64 training/selection; train normalization discards rule state not used by evaluators')
    (a.output/'manifest.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))


if __name__=='__main__':main()
