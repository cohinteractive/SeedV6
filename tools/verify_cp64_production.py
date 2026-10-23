"""Bounded offline production-UCI adoption gate. No tournaments or installed-client changes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import chess
from native_package_checks import Engine, check, VECTOR_WARNING

SHA = '8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b'
REJECTED = 'SeedV6 UCI rejected command: Unsupported Threads/Hash setting (Threads 1..16, Hash 1..1024 MiB)'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve(); output.mkdir(parents=True, exist_ok=False)
    install = args.install.resolve()
    launcher = install / 'bin' / ('seedv6-uci.bat' if os.name == 'nt' else 'seedv6-uci')
    env = {k: v for k, v in os.environ.items() if k not in
           ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'JAVA_OPTS', 'SEEDV6_UCI_OPTS', 'CLASSPATH')}
    report = dict(status='FAIL', sessions=[], workers=[],
                  jars={p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (install/'lib').glob('*.jar')})

    def launch():
        engine = Engine([launcher], output, env, (VECTOR_WARNING, REJECTED))
        report['sessions'].append(dict(sent=engine.sent, stdout=engine.lines, stderr=engine.errors))
        return engine

    def identify(engine, workers, hash_mib):
        engine.send('uci'); lines = engine.until('uciok'); identity = '\n'.join(lines)
        for value in ('flags=3407871', 'NOMINAL_FUTILITY,VERIFIED_NULL', SHA, 'schema=S6CNU002 heads=8',
                      'scoreMapping=BT4_Q_LOGIT400_V1', f'workers={workers};hashMiB={hash_mib};aspiration=ADAPTIVE16',
                      'option name Threads type spin default 1 min 1 max 16',
                      'option name Hash type spin default 64 min 1 max 1024'):
            check(value in identity, 'Missing identity/option: ' + value)

    def search(engine, position, board, go):
        engine.send('position ' + position + '\ngo ' + go)
        lines = engine.until('bestmove ', timeout=45)
        move = lines[-1].split()[1]
        check((move == '0000' and not any(board.legal_moves)) or
              (move != '0000' and chess.Move.from_uci(move) in board.legal_moves), 'Illegal bestmove: ' + move)
        for line in lines:
            if ' pv ' in line:
                replay = board.copy()
                for text in line.split(' pv ', 1)[1].split():
                    move = chess.Move.from_uci(text)
                    check(move in replay.legal_moves, 'Illegal PV: ' + line); replay.push(move)
        return lines

    engine = None
    try:
        engine = launch(); identify(engine, 1, 64)
        for workers in range(1, 17):
            hash_mib = 64 if workers in (1, 4, 16) else 1
            engine.send(f'setoption name Threads value {workers}\nsetoption name Hash value {hash_mib}\nisready')
            engine.until('readyok'); identify(engine, workers, hash_mib)
            lines = search(engine, 'startpos', chess.Board(), 'depth 3')
            check(any(line.startswith('info depth 3 ') for line in lines), 'Incomplete depth 3')
            if workers in (1, 2, 4, 8, 16):
                lines = search(engine, 'startpos', chess.Board(), 'depth 40 nodes 25000')
                check(any('request nodes=25000 ' in line for line in lines), 'Whole-request budget mismatch')
                engine.send('position startpos\ngo infinite\nisready'); engine.until('readyok')
                engine.send('stop'); check(engine.until('bestmove ')[-1].split()[1] != '0000', 'Stop fallback missing')
                engine.send('ucinewgame\nsetoption name Clear Hash\nisready'); engine.until('readyok')
                search(engine, 'startpos', chess.Board(), 'movetime 50')
            report['workers'].append(dict(workers=workers, hashMiB=hash_mib))
        for name, value in (('Threads', 0), ('Threads', 17), ('Hash', 0), ('Hash', 1025)):
            engine.send(f'setoption name {name} value {value}\nisready'); engine.until('readyok')
            identify(engine, 16, 64)
        for fen, score in [('7k/6Q1/5K2/8/8/8/8/8 b - - 100 1', 'mate 0'),
                           ('7k/5Q2/6K1/8/8/8/8/8 b - - 100 1', 'cp 0'),
                           ('7k/5Q2/6K1/8/8/8/8/8 w - - 0 1', 'mate 1')]:
            lines = search(engine, 'fen ' + fen, chess.Board(fen), 'depth 3')
            check(any('score ' + score in line for line in lines), 'Terminal/mate score mismatch')
        moves = 'g1f3 g8f6 f3g1 f6g8 g1f3 g8f6 f3g1 f6g8'
        board = chess.Board()
        for move in moves.split(): board.push_uci(move)
        lines = search(engine, 'startpos moves ' + moves, board, 'depth 3')
        check(any('score cp 0' in line for line in lines), 'Formal repetition not drawn')
        engine.send('position startpos\ngo infinite\nucinewgame\nisready')
        check(not any(line.startswith('bestmove ') for line in engine.until('readyok')), 'Stale result after reset')
        search(engine, 'startpos', chess.Board(), 'depth 3')
        engine.send('go infinite'); engine.close(); engine.kill(); engine = None
        engine = launch(); identify(engine, 1, 64)
        engine.send('position startpos\ngo infinite'); engine.close(eof=True)
        report['status'] = 'PASS'
    finally:
        if engine is not None: engine.kill()
        (output/'result.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print('PASS: standard UCI defaults, all 1..16 workers, Hash options, legal PVs, terminals, history, stop/reset/quit/EOF')


if __name__ == '__main__':
    main()
