"""Outcome-blind SR-006 diagnostic sampling from already exploratory game trajectories."""
import hashlib
import json
from pathlib import Path
import random
import sys
import chess.pgn

source, target = map(Path, sys.argv[1:3])
rng = random.Random(20261061)
rows, selection = [], []
with source.open(encoding='utf-8') as stream:
    while (game := chess.pgn.read_game(stream)) is not None:
        board = game.board()
        prefix, eligible = [], []
        for ply, move in enumerate(game.mainline_moves(), 1):
            board.push(move)
            prefix.append(move.uci())
            if 20 <= ply <= 160 and not board.is_game_over(claim_draw=True):
                eligible.append((ply, ' '.join(prefix)))
        if not eligible:
            continue
        # Alternate early and late halves of the available trajectory. No score/result filter.
        midpoint = (len(eligible)+1)//2
        subset = eligible[:midpoint] if len(rows)%2 == 0 else eligible[midpoint:] or eligible
        ply, moves = rng.choice(subset)
        rows.append(f'played-{len(rows):02d}\t{game.board().fen()}\t{moves}')
        selection.append({'game': len(selection)+1, 'ply': ply, 'availablePositions': len(eligible)})
        if len(rows) == 64:
            break
target.write_text('\n'.join(rows)+'\n', encoding='utf-8', newline='\n')
target.with_suffix('.selection.json').write_text(json.dumps({
    'source': str(source), 'sourceSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
    'seed': 20261061, 'rows': selection,
    'outputSha256': hashlib.sha256(target.read_bytes()).hexdigest(),
    'method': 'First 64 usable exploratory games; alternating early/late trajectory halves; no outcome or evaluation filter.'
}, indent=2)+'\n', encoding='utf-8', newline='\n')
print('Selected', len(rows), 'full-prefix positions')
