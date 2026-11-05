"""Freeze CLV development inputs only; no confirmation sample or score inspection."""
import importlib.util
import argparse
import json
from pathlib import Path
import random
import subprocess
import chess

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-critical-line'
spec = importlib.util.spec_from_file_location('clv_opening_support', ROOT / 'tools/p4-tactical-openings.py')
support = importlib.util.module_from_spec(spec); spec.loader.exec_module(support)


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--version',type=int,default=1)
    args=parser.parse_args()
    if args.version<1:raise ValueError('Positive input version required')
    target = OUT / f'development-v{args.version}-openings.pgn'
    receipt = target.with_suffix('.selection.json')
    if target.exists() or receipt.exists(): raise FileExistsError('Frozen inputs: use a new version')
    if support.digest(support.OLD) != support.OLD_SHA: raise ValueError('Changed opening source')
    excluded=set(); inputs={}; unhandled=[]
    for name in subprocess.check_output(['git','ls-files','docs/research/search'],cwd=ROOT,text=True).splitlines():
        if 'boundary2' in name.lower() or not name.endswith('selection.json'): continue
        p=ROOT/name; prior=json.loads(p.read_text(encoding='utf-8-sig')); keys=prior.get('positionKeys')
        if not isinstance(keys,list): unhandled.append(name);continue
        if any(not isinstance(k,str) or len(k.split())!=4 for k in keys): raise ValueError(name)
        excluded.update(keys);inputs[name]=support.digest(p)
    corpus=ROOT/'docs/research/search/p4-correction/mixedphase-v1-roots.tsv';inputs[str(corpus.relative_to(ROOT))]=support.digest(corpus)
    diagnostic=[]
    for line in corpus.read_text().splitlines():
        if not line or line.startswith('#'):continue
        name,fen,moves=line.split('\t');board=chess.Board(fen)
        for move in moves.split():board.push_uci(move)
        key=' '.join(board.fen().split()[:4]);excluded.add(key);diagnostic.append(key)
    population={}
    for game,board,key in support.games(support.OLD):
        if key not in excluded and not board.is_game_over(claim_draw=True):population.setdefault(key,str(game))
    keys=random.Random(2026110601).sample(sorted(population),34)
    with target.open('x',encoding='utf8',newline='\n') as f:f.write('\n\n'.join(population[k] for k in keys)+'\n')
    result=dict(count=34,seed=2026110601,positionKeys=keys,outputSha256=support.digest(target),
        source=str(support.OLD),sourceSha256=support.OLD_SHA,excludedSelections=inputs,
        diagnosticPositionKeys=diagnostic,availableDistinctPositions=len(population),unhandledNonKeyReceipts=unhandled,
        selectorSha256=support.digest(Path(__file__)),selection='No scores read. 2 operational pilot pairs then32 separate development pairs. Excludes recorded non-BOUNDARY2 selection keys and current diagnostic roots. Development-only, not globally held-out confirmation; future independent input-only audit remains mandatory.')
    with receipt.open('x',encoding='utf8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps(dict(selected=34,available=len(population),unhandledReceipts=len(unhandled))))
