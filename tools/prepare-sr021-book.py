"""Derive a legal full-history eight-move book without engine-based filtering.

Use an explicitly pinned downloaded PGN source. Short/nonstandard prefixes are
outside the established measurement domain and are counted, never extended.
Selection and disjointness remain the existing search-baseline.py responsibility.
"""
import argparse
import hashlib
import json
from pathlib import Path
import chess
import chess.pgn


def prepare(source, output, manifest):
    source, output, manifest = map(Path, (source, output, manifest))
    if output.exists() or manifest.exists():
        raise ValueError('Never overwrite a derived book or its receipt')
    counts = dict(total=0, admitted=0, wrongLength=0, nonstandardRoot=0, terminal=0)
    with source.open(encoding='utf-8-sig') as stream, output.open('x', encoding='utf-8', newline='\n') as out:
        while (game := chess.pgn.read_game(stream)) is not None:
            counts['total'] += 1
            if game.errors:
                raise ValueError('Illegal input game ' + str(counts['total']))
            moves = list(game.mainline_moves())
            if game.board().fen() != chess.STARTING_FEN:
                counts['nonstandardRoot'] += 1
                continue
            if len(moves) != 16:
                counts['wrongLength'] += 1
                continue
            if game.end().board().is_game_over(claim_draw=True):
                counts['terminal'] += 1
                continue
            fresh = chess.pgn.Game()
            fresh.headers['Event'] = 'SeedV6 full-history source prefix'
            fresh.headers['Result'] = '*'
            node = fresh
            for move in moves:
                node = node.add_main_variation(move)
            out.write(str(fresh) + '\n\n')
            counts['admitted'] += 1
    digest = lambda p: hashlib.sha256(Path(p).read_bytes()).hexdigest()
    receipt = dict(source=str(source.resolve()), sourceSha256=digest(source),
                   output=str(output.resolve()), outputSha256=digest(output),
                   toolSha256=digest(__file__), counts=counts,
                   rule='Keep exactly16 legal plies from standard startpos and nonterminal endpoint, preserving every real move. No evaluator/score/outcome selection. Deduplication, partition, exclusions and seed sampling occur separately.',
                   limits='Opening-source distribution differs from8moves_v3; full-history validity does not establish training-source independence or universal strength.')
    manifest.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps(receipt), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('source', 'output', 'manifest'):
        parser.add_argument('--' + name, required=True)
    args = parser.parse_args()
    prepare(args.source, args.output, args.manifest)
