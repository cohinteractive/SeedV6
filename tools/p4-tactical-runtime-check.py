"""Bounded TC-R001 UCI clocks, real history, terminals and process lifecycle."""
import argparse
import importlib.util
import json
from pathlib import Path
import time

spec = importlib.util.spec_from_file_location('tc_runtime', Path(__file__).with_name('p4-tactical.py'))
ch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ch)
m, b = ch.m, ch.b


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('runtime', 'manifest', 'name'):
        parser.add_argument('--'+key, required=True)
    parser.add_argument('--profile', default='probcut')
    parser.add_argument('--workers', type=int, choices=(1,4), nargs='+', default=[1])
    args = parser.parse_args()
    manifest = json.loads(Path(args.manifest).read_text())
    if any(b.digest(p) != h for p, h in manifest['files'].items()):
        raise ValueError('Frozen runtime mismatch')
    out = m.OUT / 'raw' / args.name
    out.mkdir(exist_ok=False)
    report = {'status': 'running', 'rows': [], 'identities': {}, 'runtimeManifest': args.manifest,
              'manifestSha256': b.digest(args.manifest), 'toolSha256': b.digest(__file__)}
    start = time.monotonic()
    repeated = 'startpos moves g1f3 g8f6 f3g1 f6g8 g1f3 g8f6 f3g1 f6g8'
    cases = [('startpos', 'movetime 100'), ('startpos', 'wtime 1000 btime 1000 winc 100 binc 100'),
             (repeated, 'depth 5'), ('fen 7k/6Q1/5K2/8/8/8/8/8 b - - 0 1', 'depth 4'),
             ('fen 7k/5K2/6Q1/8/8/8/8/8 b - - 0 1', 'depth 4'),
             ('fen 7k/8/8/8/8/8/4R3/K7 w - - 99 1', 'depth 4')]
    try:
        for workers in args.workers:
            engine = b.Uci(ch.command(args.runtime, args.profile, workers), out/f'w{workers}.txt')
            try:
                report['identities'][str(workers)] = ch.configure(engine, workers, profile=args.profile)
                for position, request in cases:
                    board = b.chess.Board(position[4:]) if position.startswith('fen ') else b.chess.Board()
                    if ' moves ' in position:
                        for move in position.split(' moves ')[1].split():
                            board.push_uci(move)
                    engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                    engine.send('position '+position); engine.send('go '+request)
                    result = b.inspect_result(board, engine.until('bestmove ', 20))
                    if result['elapsedNanos'] > 1_500_000_000:
                        raise ValueError('Bounded request grossly exceeded test time envelope')
                    if position == repeated and (result['nodes'] != 0 or 'score cp 0' not in (result['finalInfo'] or '')):
                        raise ValueError('Real threefold must adjudicate before searching')
                    report['rows'].append({'workers': workers, 'position': position, 'request': request, 'result': result})
                if args.profile == 'probcut':
                    reuse = []
                    for step in range(3):
                        if step == 0:
                            engine.send('ucinewgame')
                        elif step == 2:
                            engine.send('setoption name Clear Hash')
                        engine.send('isready'); engine.until('readyok')
                        engine.send('position startpos'); engine.send('go depth 6')
                        result = b.inspect_result(b.chess.Board(), engine.until('bestmove ', 20))
                        reuse.append(result)
                        report['rows'].append({'workers': workers, 'request': 'reuse/reset depth 6', 'step': step, 'result': result})
                    if workers == 1 and m.stable(reuse[0]) != m.stable(reuse[2]):
                        raise ValueError('Clear Hash did not reproduce fresh W1 state')
                    report.setdefault('reuseChecks', []).append({'workers': workers, 'freshResetW1Equal': workers == 1,
                        'retainedRequestDifferent': m.stable(reuse[0]) != m.stable(reuse[1])})
                engine.send('setoption name Clear Hash'); engine.send('isready'); engine.until('readyok')
                engine.send('position startpos'); engine.send('go infinite'); time.sleep(.1); engine.send('stop')
                b.inspect_result(b.chess.Board(), engine.until('bestmove ', 20))
                engine.send('ucinewgame'); engine.send('position startpos'); engine.send('go nodes 5000')
                result = b.inspect_result(b.chess.Board(), engine.until('bestmove ', 20))
                if result['nodes'] != 5000:
                    raise ValueError('Post-stop/reset work accounting')
                report['rows'].append({'workers': workers, 'request': 'post-stop nodes 5000', 'result': result})
            finally:
                engine.close()
        report.update(status='passed', enginesClosed=True)
    except BaseException as error:
        report.update(status='failed', failure=repr(error))
        raise
    finally:
        report['wallSeconds'] = time.monotonic()-start
        b.save(out/'observations.json', report)
    b.save(m.OUT/(args.name+'-summary.json'), {k: v for k, v in report.items() if k != 'rows'} |
           {'requests': len(report['rows']), 'stopRequests': len(args.workers), 'raw': str(out),
            'limits': 'Bounded W1/W4 UCI technical checks, not a clock-policy qualification or competitive evidence.'})
    print(json.dumps({'status': report['status'], 'requests': len(report['rows']), 'wallSeconds': report['wallSeconds']}))


if __name__ == '__main__':
    main()
