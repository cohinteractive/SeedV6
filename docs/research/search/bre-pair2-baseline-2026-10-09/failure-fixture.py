"""Deliberately broken UCI engine for isolated Fastchess failure-detection checks."""
import sys
import time
import chess

board = chess.Board()

for line in sys.stdin:
    line = line.strip()
    if line == 'uci':
        print('id name failure-fixture\nuciok', flush=True)
    elif line == 'isready':
        print('readyok', flush=True)
    elif line.startswith('position '):
        tokens = line.split()
        board = chess.Board(' '.join(tokens[2:8])) if tokens[1] == 'fen' else chess.Board()
        if 'moves' in tokens:
            for move in tokens[tokens.index('moves')+1:]: board.push_uci(move)
    elif line.startswith('go '):
        if sys.argv[1] == 'crash':
            sys.exit(7)
        if sys.argv[1] == 'late':
            time.sleep(.4)
        print('bestmove ' + ('a1a1' if sys.argv[1] == 'illegal' else next(iter(board.legal_moves)).uci()), flush=True)
    elif line == 'quit':
        break
