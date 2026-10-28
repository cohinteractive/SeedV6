"""Select value-free, legal counterfactual quartets with disjoint Pair-2 dependencies."""
import argparse
import json
from pathlib import Path
import random
import chess
from baseline import ROOT,admit,sha


SHAPES=((0,0,1,0,2,0),(0,0,0,1,0,2),(0,0,1,1,2,2),(0,0,-1,1,-2,2),
        (0,0,1,0,0,1),(0,0,-1,0,0,1),(0,0,2,0,0,2),(0,0,3,0,0,3))
BUCKETS=(0,1,2,3,4,4,5,5,6,6,6,6,7,7,7,7,8,8,8,8,8,8,8,8,9,9,9,9,9,9,9,9)


def physical_pairs():
    pairs=set()
    for shape in SHAPES:
        for y in range(8):
            for x in range(8):
                cells=[(x+shape[i],y+shape[i+1]) for i in (0,2,4)]
                if all(0<=a<8 and 0<=b<8 for a,b in cells):
                    for flip in (0,56):pairs.add(tuple(sorted((8*b+a)^flip for a,b in cells[:2])))
    if len(pairs)!=294:raise ValueError('Unexpected independent physical geometry')
    return pairs


def context(board):
    k=board.king(board.turn)^(0 if board.turn else 56);mirror=7 if k%8>3 else 0
    return BUCKETS[(k//8)*4+((k^mirror)%8)]*2+bool(mirror)


def relocate(board,move):
    if move not in board.legal_moves:return None
    out=board.copy(stack=False);out.push(move);out.turn=board.turn;out.ep_square=None
    return out if out.is_valid() and not out.is_check() else None


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('output',type=Path);args=parser.parse_args()
    resource=admit(initial=True);source=ROOT/'docs/research/brn-relational-cglhw/raw/r002-data/validation.tsv'
    manifest=json.loads((source.parent/'manifest.json').read_text())
    if sha(source)!=manifest['files']['validation.tsv']:raise ValueError('Development input identity')
    if args.output.exists() or args.output.with_suffix('.selection.json').exists():raise ValueError('Preserve frozen fixtures')
    rows=source.read_text().splitlines();indices=list(range(len(rows)));rng=random.Random(2026102906);rng.shuffle(indices)
    pairs=physical_pairs();selected=[];groups={False:0,True:0}
    for ordinal,index in enumerate(indices):
        if ordinal%128==0:admit()
        if len(selected)==64:break
        b=chess.Board(rows[index].split('\t')[0]);moves=[m for m in b.legal_moves if not b.is_capture(m) and not b.is_castling(m) and not m.promotion]
        kings=[m for m in moves if b.piece_type_at(m.from_square)==chess.KING];others=[m for m in moves if m not in kings];rng.shuffle(kings);rng.shuffle(others);accepted=False
        for king in kings:
            kb=relocate(b,king)
            if kb is None:continue
            changed=context(b)!=context(kb)
            if groups[changed]>=32:continue
            for piece in others:
                a={king.from_square,king.to_square};c={piece.from_square,piece.to_square}
                if a&c or any(tuple(sorted((s,t))) in pairs for s in a for t in c):continue
                pb=relocate(b,piece)
                if pb is None:continue
                both=relocate(kb,piece);reverse=relocate(pb,king)
                if both is None or reverse is None or both.board_fen()!=reverse.board_fen():continue
                selected.append(dict(source_row=index,king_move=king.uci(),piece_move=piece.uci(),context_changed=changed,
                                     fens=[x.fen() for x in (b,kb,pb,both)]));groups[changed]+=1;accepted=True;break
            if accepted:break
    if len(selected)!=64:raise ValueError('Insufficient admissible quartets: '+str(groups))
    args.output.write_text(json.dumps(selected,indent=2)+'\n')
    report=dict(source=str(source),source_sha256=sha(source),script_sha256=sha(Path(__file__)),output_sha256=sha(args.output),seed=2026102906,
                quartets=len(selected),context_changed=groups[True],context_unchanged=groups[False],physical_pairs=len(pairs),resources=resource,
                limits='Consumed development positions; hypothetical same-side relocations with fixed STM, cleared EP; all four boards valid/noncheck and both relocation orders legal. No evaluator/outcome selection; not a strength test.')
    args.output.with_suffix('.selection.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))


if __name__=='__main__':main()
