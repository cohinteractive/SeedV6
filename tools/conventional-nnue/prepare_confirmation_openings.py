"""Freeze uniformly sampled legal 16-ply book prefixes, excluding recorded prior roots.

Read-only reuse of a checksum-pinned local opening input. No score/outcome filter.
Run through owner compute control; completed output and selection manifest are inputs.
"""
import argparse
import hashlib
import json
from pathlib import Path
import random
import re
import subprocess
import chess
import chess.pgn
import compute_gate


def sha(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()


def key(board):return ' '.join(board.fen(en_passant='legal').split()[:4])


def strings(value):
    if isinstance(value,str):yield value
    elif isinstance(value,list):
        for x in value:yield from strings(x)
    elif isinstance(value,dict):
        for x in value.values():yield from strings(x)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source',type=Path,required=True);p.add_argument('--sha256',required=True)
    p.add_argument('--output',type=Path,required=True);p.add_argument('--manifest',type=Path,required=True)
    p.add_argument('--count',type=int,default=64);p.add_argument('--seed',type=int,required=True)
    a=p.parse_args();compute_gate.require()
    if not 1<=a.count<=128:raise ValueError('Bounded opening count required')
    if a.output.exists() or a.manifest.exists():raise ValueError('Preserve existing frozen selection')
    if sha(a.source)!=a.sha256:raise ValueError('Source identity changed')
    root=Path(__file__).resolve().parents[2];excluded=set();receipts={}
    tracked=subprocess.check_output(['git','ls-files','docs/research'],cwd=root,text=True).splitlines()
    for name in tracked:
        if not name.endswith('.json') or not ('selection' in name or 'prior-opening-fens' in name):continue
        compute_gate.require();path=root/name;data=json.loads(path.read_text(encoding='utf-8-sig'));receipts[name]=sha(path)
        for text in strings(data):
            if re.match(r'^[prnbqkPRNBQK1-8/]+ [wb] ',text):
                fields=text.split()
                if len(fields)>=4:
                    excluded.add(key(chess.Board(' '.join(fields[:4])+' 0 1')))
    prior=root/'docs/research/conventional-nnue-cglhw/openings-screen.json';receipts[str(prior.relative_to(root))]=sha(prior)
    for opening in json.loads(prior.read_text()):
        board=chess.Board()
        for move in opening['moves'].split():board.push_uci(move)
        excluded.add(key(board))
    available={};counts=dict(source_games=0,structural_rejections=0,duplicates=0,prior_excluded=0)
    with a.source.open(encoding='utf-8-sig') as stream:
        while (game:=chess.pgn.read_game(stream)) is not None:
            if counts['source_games']%128==0:compute_gate.require()
            counts['source_games']+=1;board=game.board();moves=list(game.mainline_moves())
            if game.errors or board.fen()!=chess.STARTING_FEN or len(moves)!=16:
                counts['structural_rejections']+=1;continue
            for move in moves:
                if move not in board.legal_moves:raise ValueError('Illegal source move')
                board.push(move)
            if board.is_game_over(claim_draw=True):counts['structural_rejections']+=1;continue
            k=key(board)
            if k in excluded:counts['prior_excluded']+=1;continue
            if k in available:counts['duplicates']+=1;continue
            available[k]=dict(name=f'Book prefix {counts["source_games"]}',moves=' '.join(m.uci() for m in moves))
    if len(available)<a.count:raise ValueError(f'Only {len(available)} unused roots')
    keys=sorted(available);random.Random(a.seed).shuffle(keys);selected=keys[:a.count]
    output=[available[k] for k in selected];compute_gate.require()
    a.output.write_text(json.dumps(output,indent=2)+'\n',encoding='utf-8',newline='\n')
    result=dict(source=str(a.source.resolve()),source_sha256=a.sha256,seed=a.seed,count=a.count,
        counts=counts,recorded_prior_roots=len(excluded),available_distinct_roots=len(available),
        exclusion_manifest_sha256=receipts,positionKeys=selected,output=str(a.output),output_sha256=sha(a.output),
        policy='Fixed16-ply legal standard-start nonterminal prefixes, four-field root with legal-en-passant normalization, dedup/exclusion, seeded uniform shuffle; no outcome/evaluator filter',
        limits='Excludes inspected tracked root records, not unknown historic games or training positions. Related opening-family dependence remains possible.')
    a.manifest.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ('exclusion_manifest_sha256','positionKeys')}))


if __name__=='__main__':main()
