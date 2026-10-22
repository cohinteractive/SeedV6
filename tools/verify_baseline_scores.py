"""Bounded CP64 score/transition check; writes fresh local evidence and compact summary.

Uses the independent frozen CP64 reference, color/rank mirrors, curated legal positions,
and the same compiled core in scalar and Vector modes. No training or score rescaling.
"""
import argparse
import csv
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import chess

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools/conventional-nnue'))
import compute_gate

CASES = {
    'balanced-start': chess.STARTING_FEN,
    'balanced-castling': 'r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1',
    'queen-advantage': 'rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    'rook-advantage': '1nbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQk - 0 1',
    'queen-versus-rook': '6k1/5ppp/8/8/8/8/5PPP/3Q2Kr w - - 0 1',
    'passed-pawn': '8/4P1k1/4K3/8/8/8/8/8 w - - 0 1',
    'pawn-ending': '4k3/8/8/8/4P3/8/8/4K3 w - - 0 1',
    'promotion': '7k/P7/8/8/8/8/8/7K w - - 0 1',
    'en-passant': '4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1',
    'mate-in-one': '7k/5Q2/6K1/8/8/8/8/8 w - - 0 1',
    'checkmated': '7k/6Q1/6K1/8/8/8/8/8 b - - 0 1',
    'stalemate': '7k/5Q2/6K1/8/8/8/8/8 b - - 0 1',
}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--summary', type=Path, required=True)
    a = parser.parse_args(); compute_gate.require()
    a.output.mkdir(parents=True, exist_ok=False)
    if a.summary.exists(): raise ValueError('Preserve completed summary')
    reference = ROOT / 'app-uci/src/test/resources/conventional-cp64-parity.tsv'
    inputs = []
    for index, line in enumerate(reference.read_text().splitlines()):
        if not line or line.startswith('#'): continue
        expected, fen = line.split('\t'); board = chess.Board(fen)
        inputs += [f'reference-{index}\t{fen}\t{expected}',
                   f'mirror-{index}\t{board.mirror().fen()}\t{expected}']
    for name, fen in CASES.items():
        if not chess.Board(fen).is_valid(): raise ValueError('Invalid curated position: ' + name)
        inputs.append(f'case-{name}\t{fen}')
    source = a.output / 'positions.tsv'; source.write_text('\n'.join(inputs)+'\n')
    jars = ROOT / 'app-uci/build/install/seedv6-uci/lib'
    classes = a.output / 'classes'; classes.mkdir()
    suffix = '.exe' if os.name == 'nt' else ''
    subprocess.run([str(a.java_home / ('bin/javac'+suffix)), '-cp', str(jars/'*'), '-d', str(classes),
                    str(ROOT/'tools/BaselineCalibrationProbe.java')], check=True)
    outputs = []
    for mode, options in [('scalar', []), ('vector', ['--add-modules=jdk.incubator.vector'])]:
        compute_gate.require()
        result = subprocess.run([str(a.java_home/('bin/java'+suffix)), *options, '-XX:ActiveProcessorCount=1',
            '-Xmx768m', '-cp', str(classes)+os.pathsep+str(jars/'*'), 'BaselineCalibrationProbe', str(source)],
            check=True, text=True, capture_output=True)
        (a.output/(mode+'.tsv')).write_text(result.stdout)
        (a.output/(mode+'.stderr')).write_text(result.stderr)
        outputs.append(result.stdout)
    if outputs[0] != outputs[1]: raise AssertionError('Scalar/Vector score or search mismatch')
    rows = list(csv.DictReader(io.StringIO(outputs[0]), delimiter='\t'))
    heads = {}
    for head in range(8):
        scores = [int(row['nnueCp']) for row in rows if int(row['head']) == head]
        heads[str(head)] = dict(positions=len(scores), minimum=min(scores), maximum=max(scores),
                               mean=sum(scores)/len(scores))
    result = dict(status='PASS', positions=len(rows), children_checked=sum(int(r['childrenChecked']) for r in rows),
        head_crossings=sum(int(r['headCrossings']) for r in rows),
        king_context_crossings=sum(int(r['kingContextCrossings']) for r in rows),
        maximum_head_transition_delta=max(int(r['maximumHeadDelta']) for r in rows),
        independent_reference_positions=259, color_rank_mirrors=259, heads=heads,
        curated=[r for r in rows if r['name'].startswith('case')],
        scalar_vector_full_rebuild_incremental='exact agreement', evidence=str(a.output),
        reference_sha256=hashlib.sha256(reference.read_bytes()).hexdigest(),
        source_revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        limits='Static distribution and bounded depth-4 removal control; not empirical WDL calibration or isolated heuristic strength proof.')
    a.summary.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))

if __name__ == '__main__': main()
