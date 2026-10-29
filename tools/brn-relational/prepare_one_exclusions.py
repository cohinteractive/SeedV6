"""R057 preserve all existing diagnostic reservations and CP64 validation aliases."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import time
import chess
from baseline import ROOT, sha
import compute_gate


def compact(board):
    """Independent Python exact normalized/horizontal key matching OneCorpusAudit."""
    if not board.turn:
        board = board.mirror()
    def pack(b):
        occupied = lo = hi = 0
        for i, (square, piece) in enumerate(sorted(b.piece_map().items())):
            occupied |= 1 << square
            code = piece.piece_type - 1 + (0 if piece.color else 8)
            if i < 16:
                lo |= code << (4*i)
            else:
                hi |= code << (4*(i-16))
        return occupied, lo, hi
    return min(pack(board), pack(board.transform(chess.flip_horizontal)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    args.output = args.output.resolve()
    compute_gate.require()
    started = time.monotonic()
    base = ROOT / 'docs/research/brn-relational-cglhw'
    keys = set()
    sources = {}
    counts = {}
    def read(path):
        sources[str(path.relative_to(ROOT))] = sha(path)
        return json.loads(path.read_text())
    # Verify the frozen reservation records against the existing publication manifests.
    for name in ('r049-selection.json', 'r050-selection.json'):
        manifest = read(base/name)
        for path, digest in manifest['files'].items():
            if sha(ROOT/path) != digest:
                raise ValueError('Changed historical evidence: '+path)
    reserved = read(base/'raw/r049-data/reservations.json')
    if reserved['next_game'] != 544:
        raise ValueError('Incomplete historical game reservations')
    for index, alias in enumerate(reserved['aliases']):
        if index % 4096 == 0:
            compute_gate.require()
        keys.add(compact(chess.Board(alias+' - - 0 1')))
    counts['prior_544_games_old4096_r016_r008_reservations'] = len(keys)
    selected = read(base/'raw/r049-data/selection.json')
    for row in selected['pairs']:
        for field in ('fen', 'child_fen'):
            keys.add(compact(chess.Board(row[field])))
    counts['after_r049_parent_child'] = len(keys)
    for path in sorted((base/'raw/r050-data').glob('cases-*.json')):
        for row in read(path)['positions']:
            keys.add(compact(chess.Board(row['fen'])))
    counts['after_r050_opposite_turn'] = len(keys)
    # All original CP64 validation rows are held out, including unconsumed aliases.
    source = ROOT/'docs/research/conventional-nnue-cglhw/raw/e006-data/validation.txt'
    manifest = json.loads((source.parent/'manifest.json').read_text())
    sources[str(source.relative_to(ROOT))] = sha(source)
    if sources[str(source.relative_to(ROOT))] != manifest['hashes']['validation.txt']:
        raise ValueError('CP64 validation identity')
    rows = 0
    for rows, line in enumerate(source.open(), 1):
        if rows % 4096 == 0:
            compute_gate.require()
        keys.add(compact(chess.Board(line.split(' | ')[0])))
    if rows != 524288:
        raise ValueError('Validation count')
    counts['after_full_cp64_validation'] = len(keys)
    args.output.mkdir(parents=True, exist_ok=True)
    output = args.output/'reserved.keys'
    encoded = b'R057EX01' + struct.pack('>I', len(keys)) + b''.join(struct.pack('>QQQ', *key) for key in sorted(keys))
    reused = output.exists()
    if reused:
        if sha(output) != hashlib.sha256(encoded).hexdigest():
            raise ValueError('Existing exclusion bytes differ; preserve and investigate')
    else:
        with output.open('xb') as stream:
            stream.write(encoded)
    result = dict(experiment='R057', sources=sources, counts=counts, cp64_validation_rows=rows,
        path=str(output.relative_to(ROOT)), sha256=sha(output), seconds=time.monotonic()-started, reused_existing_keys=reused,
        scope='Exact placement/STM, colour/rank and horizontal grouping; rule metadata ignored as by evaluator. Includes all historical reservations; no training records selected yet.')
    with (args.output/'manifest.json').open('x') as stream:
        stream.write(json.dumps(result, indent=2)+'\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
