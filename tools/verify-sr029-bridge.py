"""Frozen LMR/off UCI equivalence bridge for non-algorithmic closeout changes."""
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
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    report = {'status': 'running', 'arguments': vars(args), 'preflights': {}, 'comparisons': []}
    frozen = {str(Path(p).resolve()): b.digest(p) for p in (__file__, b.__file__, args.java)}
    for install in (args.control, args.candidate): frozen.update(b.installation_hashes(install))
    network = Path(args.store) / 'checkpoints' / b.CHECKPOINT / 'network.brn-pair2'
    assert b.digest(network) == b.NETWORK_SHA256
    frozen.update({str(p.resolve()): b.digest(p) for p in network.parent.iterdir() if p.is_file()})
    report['filesBefore'] = frozen
    try:
        for policy in ('off', 'quiet', 'static') if args.production else ('off', 'quiet'):
            for name, install in (('control', args.control), ('candidate', args.candidate)):
                key = name + '-' + policy
                qpolicy = 'static' if policy == 'static' else 'production'
                selected = 'off' if policy == 'static' else (
                    'production' if args.production and name == 'candidate' and policy == 'quiet' else policy)
                report['preflights'][key] = b.preflight(
                    b.command(args.java, install, args.store, qpolicy, selected), out / (key + '-uci.txt'), 1)
            left = report['preflights']['control-' + policy]['requests']
            right = report['preflights']['candidate-' + policy]['requests']
            assert len(left) == len(right)
            for a, c in zip(left, right):
                if a['budget'] not in ('depth 4', 'nodes 20000'): continue
                assert stable(a) == stable(c), (policy, stable(a), stable(c))
                report['comparisons'].append({'policy': policy, **stable(c)})
        if args.production:
            cmd = b.command(args.java, args.candidate, args.store, 'production', 'production')
            report['preflights']['production-two-workers'] = b.preflight(cmd, out / 'two-workers-uci.txt', 2)
            # Omit both policy properties to test the real defaults, then drain/reconfigure workers.
            default_cmd = [s for s in cmd if not s.startswith(('-Dseedv6.uci.qsearchResearch=',
                                                             '-Dseedv6.uci.lmrResearch='))]
            engine = b.Uci(default_cmd, out / 'default-and-worker-changes-uci.txt')
            try:
                engine.send('uci'); hello = engine.until('uciok')
                identities = [s for s in hello if s.startswith('info string evaluator=')]
                assert len(identities) == 1 and 'leaves=unpruned mode=production' in identities[0]
                b.check_lmr_identity(identities[0], 'production', 'production', 1)
                engine.send('setoption name OwnBook value false')
                transitions = []
                for workers in (2, 1):
                    engine.send('setoption name Threads value ' + str(workers))
                    engine.send('isready'); ready = engine.until('readyok')
                    identity = [s for s in ready if s.startswith('info string evaluator=')]
                    leaves = 'static' if workers == 2 else 'unpruned'
                    assert len(identity) == 1 and ('leaves=' + leaves + ' mode=production') in identity[0]
                    b.check_lmr_identity(identity[0], 'production', 'production', workers)
                    engine.send('ucinewgame'); engine.send('position startpos'); engine.send('go depth 4')
                    result = b.inspect_result(b.chess.Board(), engine.until('bestmove '))
                    if workers == 1:
                        expected = report['preflights']['candidate-quiet']['requests'][0]
                        assert stable({'fen': b.chess.STARTING_FEN, 'budget': 'depth 4', **result}) == stable(expected)
                    transitions.append({'workers': workers, 'identity': identity[0], 'result': result})
                report['defaultAndWorkerChanges'] = transitions
            finally:
                engine.close()
            report['invalidConfigurationChecks'] = b.failure_probes(cmd)
        assert all(b.digest(path) == value for path, value in frozen.items())
        report['inputsStable'] = True; report['status'] = 'passed'
    except BaseException as error:
        report['status'] = 'failed'; report['failure'] = repr(error); raise
    finally:
        b.save(out / 'bridge.json', report)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('control', 'candidate', 'store', 'java', 'out'):
        parser.add_argument('--' + name, required=True)
    parser.add_argument('--production', action='store_true', help='Bridge canonical production, preserved controls and worker changes')
    main(parser.parse_args())
