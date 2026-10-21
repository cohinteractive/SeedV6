"""Independent python-chess feature and symmetry oracle for the read-only probe."""
from collections import Counter
import hashlib
import json
from pathlib import Path
import sys
import chess

TYPES = {chess.KING: 1, chess.QUEEN: 2, chess.ROOK: 3,
         chess.BISHOP: 4, chess.KNIGHT: 5, chess.PAWN: 6}
DIRECTIONS = [(1, 0), (-1, 0), (0, 1), (0, -1),
              (1, 1), (1, -1), (-1, 1), (-1, -1)]


def features(board, visible):
    board = board if board.turn else board.mirror()
    result = []
    for attacker_square, attacker in board.piece_map().items():
        if attacker.piece_type not in (chess.BISHOP, chess.ROOK, chess.QUEEN):
            continue
        for target_square, target in board.piece_map().items():
            if target_square == attacker_square:
                continue
            dx = chess.square_file(target_square) - chess.square_file(attacker_square)
            dy = chess.square_rank(target_square) - chess.square_rank(attacker_square)
            diagonal = abs(dx) == abs(dy)
            straight = dx == 0 or dy == 0
            if not (diagonal or straight):
                continue
            if attacker.piece_type == chess.BISHOP and not diagonal:
                continue
            if attacker.piece_type == chess.ROOK and not straight:
                continue
            if visible and chess.between(attacker_square, target_square) & board.occupied:
                continue
            direction = DIRECTIONS.index(((dx > 0) - (dx < 0), (dy > 0) - (dy < 0)))
            slot = direction - 4 if attacker.piece_type == chess.BISHOP else (
                4 + direction if attacker.piece_type == chess.ROOK else 8 + direction)
            relation = (0 if attacker.color else 16) + slot
            category = TYPES[target.piece_type] - 1 + (0 if target.color else 6)
            result.append(relation * 84 + category * 7 + max(abs(dx), abs(dy)) - 1)
    return sorted(result)


def main(report_path, features_path, output_path):
    report_path, features_path, output_path = map(Path, (report_path, features_path, output_path))
    if output_path.exists():
        raise ValueError('Preserve completed verification')
    report = json.loads(report_path.read_text(encoding='utf-8-sig'))
    rows = [json.loads(line) for line in features_path.read_text().splitlines()]
    assert len(rows) == report['positions']
    counts = Counter()
    for row in rows:
        board = chess.Board(row['fen'])
        counts['validPositions' if board.is_valid() else 'nonstandardPositions'] += 1
        for mode in ('visible', 'geometry'):
            expected = features(board, mode == 'visible')
            assert row[mode] == expected
            assert expected == features(board.mirror(), mode == 'visible')
            assert all(0 <= index < 2688 for index in expected)
            counts[mode + 'ActiveFeatures'] += len(expected)
    witness = {key: [] for key in ('visible', 'geometry')}
    selected = (4 + 2) * 84 + (4 - 1 + 6) * 7 + 2
    for mask in range(8):
        board = chess.Board(None)
        board.set_piece_at(chess.H1, chess.Piece(chess.KING, chess.WHITE))
        board.set_piece_at(chess.H8, chess.Piece(chess.KING, chess.BLACK))
        for bit, square, piece in [(1, chess.A1, chess.Piece(chess.ROOK, chess.WHITE)),
                                   (2, chess.A2, chess.Piece(chess.PAWN, chess.WHITE)),
                                   (4, chess.A4, chess.Piece(chess.BISHOP, chess.BLACK))]:
            if mask & bit:
                board.set_piece_at(square, piece)
        assert board.is_valid()
        for mode in witness:
            witness[mode].append(features(board, mode == 'visible').count(selected))
    for mode, values in witness.items():
        assert values == report[mode + 'Witness']
        difference = sum((-1) ** (3 - mask.bit_count()) * value for mask, value in enumerate(values))
        assert difference == (-1 if mode == 'visible' else 0)
    sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
    result = dict(id='wave6-visibility-independent-oracle-v1', positions=len(rows),
                  checks=dict(counts), thirdDifference=dict(visible=-1, geometry=0),
                  evidenceSha256={str(path): sha(path) for path in (report_path, features_path)},
                  limitations=['Feature identity and colour/rank symmetry only; no trained model or chess strength.',
                               'Third difference is nonzero on eight valid boards with fixed kings; geometric control is pairwise.'])
    with output_path.open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(result, stream, indent=2)
        stream.write('\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main(*sys.argv[1:])
