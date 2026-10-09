"""Bounded UCI equivalence/integration bridge; no games or strength inference."""
import argparse
import importlib.util
from pathlib import Path
import re

spec = importlib.util.spec_from_file_location('baseline', Path(__file__).with_name('search-baseline.py'))
b = importlib.util.module_from_spec(spec)
spec.loader.exec_module(b)


def stable(request):
    info = request['finalInfo']
    if info:
        info = re.sub(r'\b(?:time|nps) \d+\s*', '', info)
    return {key: request.get(key) for key in ('fen', 'budget', 'nodes', 'bestmove', 'termination')} | {'info': info}


def main(args):
    out = Path(args.out).resolve()
    out.mkdir(parents=True, exist_ok=False)
    report = {'status': 'running', 'arguments': vars(args), 'preflights': {}, 'comparisons': []}
    frozen = {str(Path(__file__).resolve()): b.digest(__file__), str(Path(b.__file__).resolve()): b.digest(b.__file__)}
    for install in (args.baseline, args.research, args.production):
        frozen.update(b.installation_hashes(install))
    network = Path(args.store) / 'checkpoints' / b.CHECKPOINT / 'network.brn-pair2'
    assert b.digest(network) == b.NETWORK_SHA256
    frozen.update({str(p.resolve()): b.digest(p) for p in network.parent.iterdir() if p.is_file()})
    frozen[str(Path(args.java).resolve())] = b.digest(args.java)
    report['filesBefore'] = frozen
    try:
        for name, install, policy, workers in (
                ('old-static', args.baseline, 'static', 1),
                ('research-unpruned', args.research, 'unpruned', 1),
                ('production', args.production, 'production', 1),
                ('new-static', args.production, 'static', 1),
                ('production-two-workers', args.production, 'production', 2)):
            report['preflights'][name] = b.preflight(b.command(args.java, install, args.store, policy),
                                                   out / (name + '-uci.txt'), workers)
        for control, candidate in (('research-unpruned', 'production'), ('old-static', 'new-static')):
            left = report['preflights'][control]['requests']
            right = report['preflights'][candidate]['requests']
            assert len(left) == len(right)
            for a, c in zip(left, right):
                if a['budget'] not in ('depth 4', 'nodes 20000'):
                    continue  # Clock/stop latency is not a deterministic equivalence claim.
                assert stable(a) == stable(c), (control, candidate, stable(a), stable(c))
                report['comparisons'].append({'control': control, 'candidate': candidate, **stable(c)})
        cmd = b.command(args.java, args.production, args.store, 'production')
        cmd = [s for s in cmd if not s.startswith('-Dseedv6.uci.qsearchResearch=')]
        engine = b.Uci(cmd, out / 'default-and-worker-changes-uci.txt')
        try:
            engine.send('uci'); hello = engine.until('uciok')
            assert any(' leaves=unpruned mode=production' in s for s in hello)
            engine.send('setoption name OwnBook value false')
            transitions = []
            for workers in (2, 1):
                engine.send('setoption name Threads value ' + str(workers))
                engine.send('isready'); ready = engine.until('readyok')
                identity = [s for s in ready if s.startswith('info string evaluator=')]
                leaves = 'static' if workers == 2 else 'unpruned'
                assert len(identity) == 1 and ('leaves=' + leaves + ' mode=production') in identity[0]
                engine.send('ucinewgame'); engine.send('position startpos'); engine.send('go depth 4')
                result = b.inspect_result(b.chess.Board(), engine.until('bestmove '))
                if workers == 1:
                    expected = report['preflights']['production']['requests'][0]
                    assert stable({'fen': b.chess.STARTING_FEN, 'budget': 'depth 4', **result}) == stable(expected)
                transitions.append({'workers': workers, 'identity': identity[0], 'result': result})
            report['defaultAndWorkerChanges'] = transitions
        finally:
            engine.close()
        report['invalidConfigurationChecks'] = b.failure_probes(b.command(args.java, args.production, args.store, 'production'))
        assert all(b.digest(path) == digest for path, digest in frozen.items()), 'Frozen bridge input changed'
        report['inputsStable'] = True
        report['status'] = 'passed'
    except BaseException as error:
        report['status'] = 'failed'
        report['failure'] = repr(error)
        raise
    finally:
        b.save(out / 'bridge.json', report)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('baseline', 'research', 'production', 'store', 'java', 'out'):
        parser.add_argument('--' + name, required=True)
    main(parser.parse_args())
