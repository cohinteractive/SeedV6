"""Freeze R057 development and confirmation roots before principal training."""
import json
import mmap
from pathlib import Path
import random
import re
import struct
import subprocess
import time
import chess
import chess.pgn
from baseline import ROOT,sha
from prepare_one_exclusions import compact
from verify_one_corpus import contains
from prepare_confirmation_openings import strings
import compute_gate


def main():
    compute_gate.require();started=time.monotonic();base=ROOT/'docs/research/brn-relational-cglhw';raw=base/'raw/r057'
    source=Path('C:/projects/seed/java/seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn')
    expected='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
    assert sha(source)==expected
    prior=set();identities={}
    tracked=subprocess.check_output(['git','ls-files','docs/research'],cwd=ROOT,text=True).splitlines()
    for name in tracked:
        if not name.endswith('.json') or not ('selection' in name or 'prior-opening-fens' in name or 'openings' in name):continue
        compute_gate.require();p=ROOT/name;data=json.loads(p.read_text(encoding='utf-8-sig'));identities[name]=sha(p)
        for text in strings(data):
            if re.match(r'^[prnbqkPRNBQK1-8/]+ [wb] ',text):
                fields=text.split()
                if len(fields)>=4:prior.add(compact(chess.Board(' '.join(fields[:4])+' 0 1')))
        if isinstance(data,list):
            for entry in data:
                if isinstance(entry,dict) and isinstance(entry.get('moves'),str):
                    b=chess.Board()
                    for move in entry['moves'].split():b.push_uci(move)
                    prior.add(compact(b))
    # All newly reserved supplement rows are kept independent of played roots too.
    from prepare_objective import unpack
    for name in ['topup.data','fresh-validation.data']:
        data=(raw/'supplement'/name).read_bytes()
        for offset in range(0,len(data),32):prior.add(compact(unpack(data[offset:offset+32])[0]))
    merge=json.loads((raw/'corpus-merge/result.json').read_text());available={};counts=dict(source_games=0,structural=0,prior=0,training_or_reserved=0,duplicate=0)
    with (raw/'corpus-merge/unique.keys').open('rb') as uf,(raw/'exclusions/reserved.keys').open('rb') as rf:
        with mmap.mmap(uf.fileno(),0,access=mmap.ACCESS_READ) as unique,mmap.mmap(rf.fileno(),0,access=mmap.ACCESS_READ) as reservations:
            reserved_count=struct.unpack_from('>I',reservations,8)[0]
            with source.open(encoding='utf-8-sig') as stream:
                while (game:=chess.pgn.read_game(stream)) is not None:
                    if counts['source_games']%128==0:compute_gate.require()
                    counts['source_games']+=1;b=game.board();moves=list(game.mainline_moves())
                    if game.errors or b.fen()!=chess.STARTING_FEN or len(moves)!=16:counts['structural']+=1;continue
                    for move in moves:
                        assert move in b.legal_moves;b.push(move)
                    if b.is_game_over(claim_draw=True):counts['structural']+=1;continue
                    k=compact(b)
                    if k in prior:counts['prior']+=1;continue
                    if contains(unique,k,merge['exact_canonical_unique'])[0] or contains(reservations,k,reserved_count,12)[0]:counts['training_or_reserved']+=1;continue
                    if k in available:counts['duplicate']+=1;continue
                    available[k]=dict(name='Book prefix '+str(counts['source_games']),moves=' '.join(m.uci() for m in moves),fen=b.fen())
    keys=sorted(available);seed=20261029060;random.Random(seed).shuffle(keys);assert len(keys)>=288
    selected=keys[:288];outputs={}
    for split,subset in [('development',selected[:32]),('confirmation',selected[32:])]:
        p=base/f'r057-{split}-openings.json'
        with p.open('x') as f:f.write(json.dumps([available[k] for k in subset],indent=2)+'\n')
        outputs[split]=dict(path=str(p.relative_to(ROOT)),sha256=sha(p),roots=len(subset),games=2*len(subset))
    result=dict(source=str(source),source_sha256=expected,seed=seed,counts=counts,prior_canonical_aliases=len(prior),available=len(keys),outputs=outputs,
                prior_manifests=identities,positionKeys=[available[k]['fen'] for k in selected],seconds=time.monotonic()-started,
                policy='Uniform seeded shuffle of distinct legal16-ply nonterminal book roots. Excludes inspected prior root aliases, all original training and validation/diagnostic aliases, and all supplemental data aliases. No evaluator/score/outcome filtering.',
                limitations='Related opening families and unknown source games can remain correlated. Exact root exclusion does not prove entire future trajectories are absent from training.')
    with (base/'r057-openings-selection.json').open('x') as f:f.write(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ('prior_manifests','positionKeys')},indent=2))


if __name__=='__main__':main()
