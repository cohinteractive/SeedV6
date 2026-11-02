"""Freeze HL confirmation roots disjoint from prior receipts and the legal16 pool.

Never opens BOUNDARY2 study paths. No candidate evaluation or outcome filtering.
Run between matches: full legal PGN parsing is intentionally not concurrent work.
"""
import argparse
import hashlib
import json
from pathlib import Path
import random
import subprocess
import time
import chess
import chess.pgn

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-learning'
OLD = Path('C:/projects/seed/java/seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn')
OLD_SHA = 'eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
BOOK = OUT / 'raw/confirmation-book-v1/8moves_v3.pgn'
BOOK_SHA = '5835239f88cc2c7511b177c32392a69f3ede21819cf0616f80a7f907cd21d17e'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def games(path):
    with path.open(encoding='utf-8-sig') as stream:
        while (game := chess.pgn.read_game(stream)) is not None:
            if game.errors or game.board().fen() != chess.STARTING_FEN:
                raise ValueError('Invalid opening start/history: ' + str(path))
            moves = list(game.mainline_moves())
            if len(moves) != 16:
                raise ValueError('Expected exactly sixteen legal plies: ' + str(path))
            board = game.end().board()
            yield game, board, ' '.join(board.fen().split()[:4])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--count', type=int, default=160)
    parser.add_argument('--seed', type=int, default=2026110204)
    args = parser.parse_args()
    if args.count < 1 or args.output.exists() or args.output.with_suffix('.selection.json').exists():
        raise ValueError('Require positive count and new output paths')
    start = time.monotonic()
    assert digest(OLD) == OLD_SHA and digest(BOOK) == BOOK_SHA
    excluded, receipts, unhandled = set(), {}, []
    names = subprocess.check_output(['git', 'ls-files', 'docs/research/search'], cwd=ROOT, text=True).splitlines()
    for name in names:
        # This guard precedes opening, parsing or hashing any historical study file.
        if 'boundary2' in name.lower() or not name.endswith('selection.json'):
            continue
        path = ROOT / name
        previous = json.loads(path.read_text(encoding='utf-8-sig'))
        keys = previous.get('positionKeys')
        if not isinstance(keys, list):
            unhandled.append(name)
            continue
        if any(not isinstance(k, str) or len(k.split()) != 4 for k in keys):
            raise ValueError('Unexpected historical key representation: ' + name)
        excluded.update(keys)
        receipts[name] = digest(path)
    receipt_count = len(excluded)
    old_count = 0
    for _, _, key in games(OLD):
        excluded.add(key)
        old_count += 1
    admitted = {}
    source_count = excluded_count = terminal_count = 0
    for game, board, key in games(BOOK):
        source_count += 1
        if key in excluded:
            excluded_count += 1
            continue
        if board.is_game_over(claim_draw=True):
            terminal_count += 1
            continue
        # Preserve the established held-out hash bucket, then sample unique roots.
        if int(hashlib.sha256(key.encode()).hexdigest(), 16) % 2 != 1 or key in admitted:
            continue
        fresh = chess.pgn.Game()
        fresh.headers['Event'] = 'SeedV6 HL fresh independent confirmation'
        node = fresh
        for move in game.mainline_moves():
            node = node.add_main_variation(move)
        admitted[key] = str(fresh)
    keys = random.Random(args.seed).sample(sorted(admitted), args.count)
    assert not (set(keys) & excluded)
    with args.output.open('x', newline='\n', encoding='utf-8') as stream:
        stream.write('\n\n'.join(admitted[k] for k in keys) + '\n')
    result = {'source': str(BOOK), 'sourceSha256': BOOK_SHA,
              'excludedSource': str(OLD), 'excludedSourceSha256': OLD_SHA,
              'seed': args.seed, 'partition': 1, 'count': args.count,
              'sourceGames': source_count, 'oldSourceGames': old_count,
              'excludedSourceRows': excluded_count, 'terminalSourceRows': terminal_count,
              'availableDistinctPositions': len(admitted), 'positionKeys': keys,
              'excludedSelections': receipts, 'receiptDistinctPositions': receipt_count,
              'excludedDistinctPositions': len(excluded), 'unhandledNonKeyReceipts': unhandled,
              'outputSha256': digest(args.output), 'selectorSha256': digest(Path(__file__)),
              'wallSeconds': time.monotonic() - start,
              'selection': 'All final roots from the entire legal16 pool and every tracked non-BOUNDARY2 positionKeys receipt excluded. '
              'Legal nonterminal 16-ply startpos histories from pinned Stockfish 8moves_v3; '
              'sha256(final FEN first four fields) mod 2 == 1; sorted unique keys; seeded Python random.sample. '
              'No outcomes, candidate evaluations or BOUNDARY2 study files read. '
              'Non-key receipts are listed for explicit review before confirmation; source curators influence the book distribution.'}
    with args.output.with_suffix('.selection.json').open('x', newline='\n') as stream:
        json.dump(result, stream, indent=2)
        stream.write('\n')
    print(json.dumps({k: v for k, v in result.items() if k not in ('positionKeys', 'excludedSelections')}))


if __name__ == '__main__':
    main()
