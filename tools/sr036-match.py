"""SR-036 Fastchess adapter: independently specified engine workers/policy, audited paired games.

Uses the established immutable model, openings and independent legality/history/accounting
auditor. No outcome-based stopping: the requested fixed batch must finish or is invalid.
Use production for the integrated default and legacy for explicit static leaves.
The historical off choice verifies the pre-integration composition; frozen runners
retain its original meaning and must never be edited to reinterpret prior games.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import platform
import subprocess
import time

spec = importlib.util.spec_from_file_location('baseline', Path(__file__).with_name('search-baseline.py'))
b = importlib.util.module_from_spec(spec); spec.loader.exec_module(b)


def command(args, side):
    policy = getattr(args, side + '_policy')
    cmd = b.command(args.java, getattr(args, side), args.store, 'static' if policy == 'legacy' else 'production', 'production',
                    'off', 'default', 'off', 'default')
    cmd[3] = '-XX:ActiveProcessorCount=6'
    cmd[4:4] = ['-Dseedv6.uci.concurrencyResearch=' + ('off' if policy in ('production','legacy') else policy),
                '-Dseedv6.uci.warmupWorkers=' + str(getattr(args, side + '_workers'))]
    return cmd


def preflight(cmd, path, workers, policy):
    engine = b.Uci(cmd, path)
    try:
        engine.send('uci'); lines = engine.until('uciok', 90)
        engine.send('setoption name OwnBook value false')
        engine.send('setoption name Threads value ' + str(workers))
        engine.send('isready'); lines += engine.until('readyok', 90)
        identity = [s for s in lines if s.startswith('info string evaluator=')][-1]
        fields = dict(x.split('=', 1) for x in identity.split() if '=' in x)
        assert fields['sha256'] == b.NETWORK_SHA256 and fields['checkpoint'] == b.CHECKPOINT
        assert fields['workers'] == str(workers)
        # The accepted SR-005 binary predates internal-search research telemetry.
        assert fields.get('internal', 'off') == 'off' and fields['nmp'] == 'off'
        modern = policy != 'legacy' and (workers == 1 or policy != 'off')
        split = workers > 1 and policy in ('root_split', 'root_full')
        for key, expected in [('leaves', 'unpruned' if modern else 'static'),
                              ('lmr', 'quiet' if modern else 'off'),
                              ('lmp', 'window' if modern else 'off'),
                              ('reuse', 'root_effort' if split else 'effort' if modern else 'off')]:
            assert fields[key] == expected, (key, identity)
        expected = 'root_reuse' if policy == 'production' and workers > 1 else 'off' if policy in ('production','legacy') else policy
        assert fields.get('concurrency', 'off') == expected
        if policy in ('production','legacy'):
            assert fields['mode'] == ('production' if policy == 'production' else 'control')
            assert 'experimental' not in fields
        requests = []
        for _ in range(2):
            engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
            engine.send('position startpos'); engine.send('go nodes 100000')
            result = b.inspect_result(b.chess.Board(), engine.until('bestmove ', 60))
            assert result['nodes'] == 100000
            requests.append(result)
        return {'identity': identity, 'requests': requests}
    finally:
        engine.close()


def run(args):
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    report = {'status': 'running', 'arguments': vars(args), 'os': platform.platform(),
              'logicalCpus': os.cpu_count(), 'model': b.NETWORK_SHA256,
              'resourceNote': 'Six physical/twelve logical Ryzen 5500; ActiveProcessorCount=6 for both JVMs; engine workers differ only as specified.'}
    started = time.monotonic()
    frozen = {str(Path(p).resolve()): b.digest(p) for p in (args.java, args.runner, args.openings, __file__, b.__file__)}
    model = Path(args.store) / 'checkpoints' / b.CHECKPOINT
    assert b.digest(model / 'network.brn-pair2') == b.NETWORK_SHA256
    frozen.update({str(p.resolve()): b.digest(p) for p in model.iterdir() if p.is_file()})
    for side in ('baseline', 'candidate'): frozen.update(b.installation_hashes(getattr(args, side)))
    report['filesBefore'] = frozen
    report['javaVersion'] = subprocess.run([args.java, '-version'], capture_output=True, text=True, check=True).stderr
    commands = {side: command(args, side) for side in ('baseline', 'candidate')}
    report['commands'] = commands
    try:
        for side, cmd in commands.items():
            report[side + 'Preflight'] = preflight(cmd, out / (side + '-uci.txt'),
                                                 getattr(args, side + '_workers'), getattr(args, side + '_policy'))
        cmd = [str(Path(args.runner).resolve())]
        for side, engine in commands.items():
            cmd += ['-engine', 'name=' + side, 'cmd=' + engine[0],
                    'args=' + subprocess.list2cmdline(engine[1:]),
                    'option.Threads=' + str(getattr(args, side + '_workers'))]
        cmd += ['-each', 'proto=uci', 'tc=' + args.tc, 'timemargin=0', 'restart=off', 'option.OwnBook=false',
                '-rounds', str(args.pairs), '-repeat', '-concurrency', str(args.concurrency), '-srand', str(args.seed),
                '-openings', 'file=' + str(Path(args.openings).resolve()), 'format=pgn', 'order=sequential',
                '-pgnout', 'file=' + str(out / 'games.pgn'), 'nodes=true', 'nps=true', 'pv=true', 'timeleft=true', 'latency=true',
                'match_line=^info string request ', '-report', 'penta=true', '-ratinginterval', '2',
                '-autosaveinterval', '0', '-strict']
        report['runnerCommand'] = cmd; b.save(out / 'run.json', report)
        match_started = time.monotonic()
        with (out / 'runner.txt').open('w', encoding='utf-8') as log:
            process = subprocess.Popen(cmd, cwd=out, env=b.clean_environment(), stdout=log, stderr=subprocess.STDOUT)
            try:
                code = process.wait(timeout=args.timeout)
            except BaseException:
                if process.poll() is None:
                    subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
                    process.wait()
                raise
        report['matchWallSeconds'] = time.monotonic() - match_started
        report['runnerExitCode'] = code
        report['results'] = b.analyze(out / 'games.pgn', args.openings, args.pairs, require_accounting=True, unordered=args.concurrency > 1)
        assert code == 0 and report['results']['terminations'] == {'normal': args.pairs * 2}
        assert all(b.digest(p) == digest for p, digest in frozen.items()), 'Frozen inputs changed'
        report.update(status='passed', inputsStable=True)
    except BaseException as error:
        report.update(status='failed', failure=repr(error)); raise
    finally:
        report['wallSeconds'] = time.monotonic() - started; b.save(out / 'run.json', report)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('baseline', 'candidate', 'store', 'runner', 'openings', 'out'): p.add_argument('--' + name, required=True)
    p.add_argument('--java', default='C:/java/jdk-21/bin/java.exe')
    for side in ('baseline', 'candidate'):
        p.add_argument('--' + side + '-policy', choices=('production','legacy','off','private','cooperative','plain','owner','offset','mixed','root_split','root_full','root_reuse'), default='production')
        p.add_argument('--' + side + '-workers', type=int, choices=range(1,17), default=1)
    p.add_argument('--concurrency', type=int, choices=(1,2,3,4), default=1)
    p.add_argument('--pairs', type=int, required=True); p.add_argument('--tc', default='15+0.15')
    p.add_argument('--seed', type=int, default=20261014); p.add_argument('--timeout', type=int, default=7200)
    run(p.parse_args())
