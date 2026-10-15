"""Fixed-binary futility availability and lifecycle at every supported worker count."""
import argparse
from pathlib import Path
from types import SimpleNamespace
import importlib.util

spec = importlib.util.spec_from_file_location('bridge', Path(__file__).with_name('sr021-verify.py'))
bridge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bridge)
m, b, v = bridge.m, bridge.b, bridge.v


def run(a):
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=False)
    report = dict(status='running', arguments=vars(a), preflights=[], transitions=[])
    frozen = b.installation_hashes(a.candidate)
    frozen.update({str(Path(p).resolve()): b.digest(p) for p in
                   (__file__, bridge.__file__, m.__file__, b.__file__, v.__file__, v.v.__file__, a.java)})
    model = Path(a.store)/'checkpoints'/b.CHECKPOINT
    frozen.update({str(p.resolve()): b.digest(p) for p in model.iterdir() if p.is_file()})
    engines = []
    try:
        for workers in range(1, 17):
            args = SimpleNamespace(java=a.java, store=a.store, baseline=a.candidate,
                                   baseline_policy=a.policy, baseline_workers=workers)
            report['preflights'].append(dict(workers=workers, **m.preflight(
                m.command(args, 'baseline'), out/f'workers-{workers}.txt', workers, a.policy)))
        args.baseline_workers = 1
        engine = v.start(m.command(args, 'baseline'), out/'transitions.txt')
        engines.append(engine)
        for workers in (1, 4, 16, 1):
            engine.send(f'setoption name Threads value {workers}')
            engine.send('isready'); lines = engine.until('readyok', 90)
            engine.send('uci'); lines += engine.until('uciok', 90)
            identity = [line for line in lines if line.startswith('info string evaluator=')][-1]
            fields = dict(x.split('=', 1) for x in identity.split() if '=' in x)
            assert fields['workers'] == str(workers) and fields['futility'] == a.policy
            requests = [v.v.request(engine, [], 'go nodes 100000'),
                        v.v.request(engine, [], 'go movetime 50')]
            assert requests[0]['nodes'] == 100000 and requests[1]['termination'] == 'TIME_LIMIT'
            engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok', 90)
            engine.send('position startpos'); engine.send('go infinite')
            lines = engine.until('info depth 1 ', 90)
            engine.send('stop'); lines += engine.until('bestmove ', 90)
            stopped = b.inspect_result(b.chess.Board(), lines)
            assert stopped['termination'] == 'STOPPED'
            requests.append(stopped)
            report['transitions'].append(dict(workers=workers, identity=identity, requests=requests))
        assert all(b.digest(p) == h for p, h in frozen.items())
        report.update(status='passed', inputsStable=True, requests=44)
    except BaseException as error:
        report.update(status='failed', failure=repr(error))
        raise
    finally:
        for engine in engines:
            engine.close()
        report['files'] = frozen
        b.save(out/'verification.json', report)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('candidate', 'store', 'policy', 'out'):
        parser.add_argument('--'+key, required=True)
    parser.add_argument('--java', default='C:/java/jdk-21/bin/java.exe')
    run(parser.parse_args())
