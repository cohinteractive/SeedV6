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
        for policy in ('off', 'quiet'):
            for name, install in (('control', args.control), ('candidate', args.candidate)):
                key = name + '-' + policy
                report['preflights'][key] = b.preflight(
                    b.command(args.java, install, args.store, 'production', policy), out / (key + '-uci.txt'), 1)
            left = report['preflights']['control-' + policy]['requests']
            right = report['preflights']['candidate-' + policy]['requests']
            assert len(left) == len(right)
            for a, c in zip(left, right):
                if a['budget'] not in ('depth 4', 'nodes 20000'): continue
                assert stable(a) == stable(c), (policy, stable(a), stable(c))
                report['comparisons'].append({'policy': policy, **stable(c)})
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
    main(parser.parse_args())
