"""Verify retained SR-023 history controls across frozen binaries after SR-030.

Run outside clock matches. Deterministic depth/node requests must preserve score,
PV, nodes and termination; timed requests are audited but not compared for speed.
"""
import argparse
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location('bridge', Path(__file__).with_name('verify-sr022-bridge.py'))
bridge = importlib.util.module_from_spec(spec); spec.loader.exec_module(bridge)
b = bridge.b
POLICIES = ('history-protect', 'history-extra', 'history-replace',
            'history-deep', 'history-gate', 'history-leaf')
CONTROL_SOURCE = '3c6f5ed5a36c615b2b88361f2e2a2cb830c217b5'
CONTROL_JAR = '3f9320fbcd526b3903f31b48241c7c8436e4f50f506a5eb275d174f4ea5895e7'


def main(args):
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    frozen = {str(Path(f).resolve()): b.digest(f) for f in (__file__, bridge.__file__, b.__file__, args.java)}
    for install in (args.control, args.candidate): frozen.update(b.installation_hashes(install))
    assert b.digest(Path(args.control) / 'lib/app.jar') == CONTROL_JAR
    assert b.digest(Path(args.candidate) / 'lib/app.jar') == args.candidate_sha256
    checkpoint = Path(args.store) / 'checkpoints' / b.CHECKPOINT
    assert b.digest(checkpoint / 'network.brn-pair2') == b.NETWORK_SHA256
    frozen.update({str(f.resolve()): b.digest(f) for f in checkpoint.iterdir() if f.is_file()})
    report = {'status': 'running', 'arguments': vars(args), 'filesBefore': frozen, 'policies': {}}
    try:
        comparisons = 0
        for policy in POLICIES:
            records = {}
            for side in ('control', 'candidate'):
                cmd = b.command(args.java, getattr(args, side), args.store,
                                'production', 'production', 'off', policy, 'off')
                records[side] = b.preflight(cmd, out / (policy + '-' + side + '.txt'), 1)
                source = CONTROL_SOURCE if side == 'control' else args.candidate_source
                assert 'source=' + source[:12] in records[side]['identity']
            for left, right in zip(records['control']['requests'], records['candidate']['requests'], strict=True):
                assert (left['fen'], left['budget']) == (right['fen'], right['budget'])
                if left['budget'] in ('depth 4', 'nodes 20000'):
                    assert bridge.stable(left) == bridge.stable(right), (policy, left, right)
                    comparisons += 1
            report['policies'][policy] = records
        assert comparisons == 72
        assert all(b.digest(f) == value for f, value in frozen.items())
        report.update(status='passed', inputsStable=True, deterministicComparisons=comparisons)
    except BaseException as error:
        report.update(status='failed', failure=repr(error))
        raise
    finally:
        b.save(out / 'controls.json', report)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('control', 'candidate', 'store', 'java', 'out'):
        parser.add_argument('--' + name, required=True)
    parser.add_argument('--candidate-source', required=True)
    parser.add_argument('--candidate-sha256', required=True)
    main(parser.parse_args())
