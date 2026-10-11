"""Frozen SR-022 UCI equivalence bridge; never a strength experiment."""
import argparse
import importlib.util
from pathlib import Path
import re

spec = importlib.util.spec_from_file_location('baseline', Path(__file__).with_name('search-baseline.py'))
b = importlib.util.module_from_spec(spec)
spec.loader.exec_module(b)


def stable(result):
    info = result['finalInfo']
    if info:
        info = re.sub(r'\b(?:time|nps) \d+\s*', '', info)
    return {key: result.get(key) for key in ('nodes', 'bestmove', 'termination')} | {'info': info}


def corpus_requests(cmd, corpora, transcript):
    engine = b.Uci(cmd, transcript)
    results = []
    try:
        engine.send('uci'); engine.until('uciok')
        engine.send('setoption name OwnBook value false')
        engine.send('setoption name Threads value 1')
        engine.send('isready'); engine.until('readyok')
        for corpus in corpora:
            for row in Path(corpus).read_text(encoding='utf-8').splitlines():
                if not row or row.startswith('#'):
                    continue
                label, fen, *prefix = row.split('\t')
                moves = prefix[0].split() if prefix else []
                board = b.chess.Board(fen)
                for move in moves:
                    board.push_uci(move)
                position = 'position fen ' + fen + (' moves ' + ' '.join(moves) if moves else '')
                for budget in ('depth 6', 'nodes 100000'):
                    engine.send('ucinewgame'); engine.send(position); engine.send('go ' + budget)
                    result = b.inspect_result(board, engine.until('bestmove ', timeout=60))
                    results.append({'corpus': str(corpus), 'label': label, 'position': position,
                                    'budget': budget, **result})
        return results
    finally:
        engine.close()


def main(args):
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    report = {'status': 'running', 'arguments': vars(args), 'preflights': {}, 'corpusResults': {}}
    frozen = {str(Path(p).resolve()): b.digest(p) for p in
              (__file__, b.__file__, args.java, *args.corpus)}
    for install in (args.control, args.candidate):
        frozen.update(b.installation_hashes(install))
    network = Path(args.store) / 'checkpoints' / b.CHECKPOINT / 'network.brn-pair2'
    assert b.digest(network) == b.NETWORK_SHA256
    frozen.update({str(p.resolve()): b.digest(p) for p in network.parent.iterdir() if p.is_file()})
    report['filesBefore'] = frozen
    try:
        for name, install in (('control', args.control), ('candidate', args.candidate)):
            lmp = getattr(args, name + '_lmp') or args.lmp
            cmd = b.command(args.java, install, args.store, 'production', 'production', 'off', lmp)
            report['preflights'][name] = b.preflight(cmd, out / (name + '-preflight.txt'), 1)
            report['corpusResults'][name] = corpus_requests(cmd, args.corpus, out / (name + '-corpus.txt'))
        comparisons = 0
        for a, c in zip(report['preflights']['control']['requests'],
                        report['preflights']['candidate']['requests'], strict=True):
            if a['budget'] in ('depth 4', 'nodes 20000'):
                assert stable(a) == stable(c), ('preflight', a, c)
                comparisons += 1
        for a, c in zip(report['corpusResults']['control'], report['corpusResults']['candidate'], strict=True):
            assert (a['corpus'], a['label'], a['budget']) == (c['corpus'], c['label'], c['budget'])
            assert stable(a) == stable(c), ('corpus', a, c)
            comparisons += 1
        assert all(b.digest(path) == value for path, value in frozen.items())
        report.update(status='passed', inputsStable=True, deterministicComparisons=comparisons)
    except BaseException as error:
        report.update(status='failed', failure=repr(error))
        raise
    finally:
        b.save(out / 'bridge.json', report)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('control', 'candidate', 'store', 'java', 'out'):
        parser.add_argument('--' + name, required=True)
    parser.add_argument('--lmp', default='off', choices=('off','shallow','lmr','combined','guarded','d2','window'))
    for side in ('control', 'candidate'):
        parser.add_argument('--' + side + '-lmp', choices=('off','production','default','shallow','lmr','combined','guarded','d2','window'),
                            help='Side-specific policy; default omits the property to test actual production startup')
    parser.add_argument('--corpus', action='append', required=True)
    main(parser.parse_args())
