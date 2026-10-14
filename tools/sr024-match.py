"""SR-024 paired clock adapter: compare SEE policies within the same worker domain.

Uses the established immutable model, paired openings and independent legal/history
PGN auditor. Every fixed exploratory batch finishes or is invalid. OFF is current
accepted cumulative production, including ROOT_REUSE for multiple workers.
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
spec = importlib.util.spec_from_file_location('process_cost', Path(__file__).with_name('sr024-process-cost.py'))
c = importlib.util.module_from_spec(spec); spec.loader.exec_module(c)


def command(args, side):
    policy = getattr(args, side + '_policy')
    cmd = b.command(args.java, getattr(args, side), args.store, 'production', 'production',
                    'off', 'default', 'off', 'default')
    workers = getattr(args, side + '_workers')
    cmd[3] = '-XX:ActiveProcessorCount=' + str(1 if workers == 1 else 6)
    cmd[4:4] = ['-Dseedv6.uci.seeResearch=' + policy,
                '-Dseedv6.uci.warmupWorkers=' + str(workers),
                '-Dseedv6.research.actor=' + side]
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
        assert fields['mapping'] == 'BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1'
        for key, expected in [('leaves', 'see_selective' if policy in ('qsafe','qdeep','qscout','qpawn','qxray') else 'unpruned'),
                              ('lmr','quiet'),('lmp','window'),('reuse','effort')]:
            assert fields[key] == expected, (key, identity)
        assert fields.get('concurrency','off') == ('root_reuse' if workers > 1 else 'off')
        assert fields.get('see','off') == policy
        assert fields['mode'] == ('production' if policy == 'off' else 'research')
        if policy != 'off': assert fields['experimental'] == 'true'
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
    assert args.baseline_workers == args.candidate_workers, 'Isolated SEE comparisons require equal workers'
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    report = {'status': 'running', 'arguments': vars(args), 'os': platform.platform(),
              'logicalCpus': os.cpu_count(), 'model': b.NETWORK_SHA256,
              'resourceNote': 'Ryzen 5500 six physical/twelve logical cores; both engines use the same workers, heaps and JVM ergonomic CPU count. CPU count is not affinity. Existing comparable game concurrency is reused.'}
    started = time.monotonic()
    frozen = {str(Path(p).resolve()): b.digest(p) for p in (args.java, args.runner, args.openings, __file__, b.__file__)}
    frozen.update({str(Path(p).resolve()):b.digest(p) for p in (c.__file__,c.n.__file__)})
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
            cohort = c.Cohort(process.pid,args.concurrency,out/'process-cost.json')
            try:
                cohort.attach()
                code = process.wait(timeout=args.timeout)
            except BaseException:
                if process.poll() is None:
                    subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
                    process.wait()
                raise
            finally:
                cohort.close()
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
        p.add_argument('--' + side + '-policy', choices=('off','qsafe','qdeep','qscout','qpawn','qxray','main','quiet','order'), default='off')
        p.add_argument('--' + side + '-workers', type=int, choices=range(1,17), default=1)
    p.add_argument('--concurrency', type=int, choices=(1,2,3,4), default=1)
    p.add_argument('--pairs', type=int, required=True); p.add_argument('--tc', default='5+0.05')
    p.add_argument('--seed', type=int, default=20261014); p.add_argument('--timeout', type=int, default=7200)
    run(p.parse_args())
