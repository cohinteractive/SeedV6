"""Phase 3 CP64 selectivity paired clock adapter over existing audit/ownership tools.

Uses frozen CP64, explicit Construction profiles, paired openings and independent
legal/history PGN audit. Production factories and historical adapters stay unchanged.
"""
import argparse
import ctypes
from ctypes import wintypes
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
spec = importlib.util.spec_from_file_location('owned_process', Path(__file__).with_name('tournament-process.py'))
owned = importlib.util.module_from_spec(spec); spec.loader.exec_module(owned)


NETWORK_SHA256 = '8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b'


class MemoryCohort(c.Cohort):
    """Add read-only lifetime peak memory using the already retained handles."""
    def close(self):
        class Counters(ctypes.Structure):
            _fields_ = [('cb', wintypes.DWORD), ('PageFaultCount', wintypes.DWORD)] + [
                (name, ctypes.c_size_t) for name in ('PeakWorkingSetSize', 'WorkingSetSize',
                'QuotaPeakPagedPoolUsage', 'QuotaPagedPoolUsage', 'QuotaPeakNonPagedPoolUsage',
                'QuotaNonPagedPoolUsage', 'PagefileUsage', 'PeakPagefileUsage')]
        api = ctypes.WinDLL('psapi', use_last_error=True).GetProcessMemoryInfo
        api.argtypes = [wintypes.HANDLE, ctypes.POINTER(Counters), wintypes.DWORD]
        api.restype = wintypes.BOOL
        for row in self.report['processes']:
            memory = Counters(); memory.cb = ctypes.sizeof(memory)
            if api(self.handles[row['pid']], ctypes.byref(memory), ctypes.sizeof(memory)):
                row['peakWorkingSetBytes'] = memory.PeakWorkingSetSize
                row['peakPagefileBytes'] = memory.PeakPagefileUsage
            else:
                row['memoryUnavailableWindowsError'] = ctypes.get_last_error()
        super().close()


def command(args, side):
    profile = getattr(args, side + '_policy')
    if profile not in ('baseline', 'no-improving', 'lazy-improving', 'nominal-futility', 'reduction-futility', 'reduction-no-improving'):
        raise ValueError('Unknown selectivity research configuration')
    runtime = Path(getattr(args, side)).resolve()
    return [str(Path(args.java).resolve()), '--add-modules=jdk.incubator.vector',
            '-Xms256m', '-Xmx512m', '-XX:ActiveProcessorCount=4',
            '-Djava.awt.headless=true', '-Dseedv6.research.actor=' + side,
            '-Dseedv6.selectivity.warmupWorkers=' + str(getattr(args, side + '_workers')),
            '-cp', str(runtime/'classes') + os.pathsep + str(runtime/'lib/*'),
            'com.ohinteractive.seedv6.integration.Cp64SelectivityUciMain',
            '--network', str(Path(args.network).resolve()), '--profile', profile]


def preflight(cmd, path, workers, policy, hash_mib=64):
    engine = b.Uci(cmd, path)
    try:
        engine.send('uci'); lines = engine.until('uciok', 90)
        engine.send('setoption name OwnBook value false')
        engine.send('setoption name Hash value ' + str(hash_mib))
        engine.send('setoption name Threads value ' + str(workers))
        engine.send('isready'); lines += engine.until('readyok', 90)
        identity = [s for s in lines if 'profile='+policy+' ' in s][-1]
        for value in ('mode=research', 'experimental=true', 'checkpoint=64', 'schema=S6CNU002',
                      'heads=8', 'flags='+str({'baseline':262143,'no-improving':260095,'lazy-improving':524287,'nominal-futility':1310719,'reduction-futility':786431,'reduction-no-improving':784383}[policy]), 'networkSha256='+NETWORK_SHA256,
                      'warmupNodes=200000', 'warmupWorkers='+str(workers),
                      'workers='+str(workers)+';hashMiB='+str(hash_mib),
                      'aspiration=ADAPTIVE16'):
            if value not in identity: raise ValueError('Missing identity '+value+': '+identity)
        requests = []
        for _ in range(2):
            engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
            engine.send('position startpos'); engine.send('go nodes 100000')
            result = b.inspect_result(b.chess.Board(), engine.until('bestmove ', 60))
            if result['nodes'] != 100000: raise ValueError('Whole-request node accounting')
            requests.append(result)
        return {'identity': identity, 'requests': requests}
    finally:
        engine.close()


def run(args):
    if not args.background: raise ValueError('Background priority required')
    assert args.baseline_workers == args.candidate_workers, 'Search comparisons require equal workers'
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    report = {'status': 'running', 'arguments': vars(args), 'os': platform.platform(),
              'logicalCpus': os.cpu_count(), 'model': NETWORK_SHA256,
              'resourceNote': 'Ryzen 5500 six physical/twelve logical cores; both engines use the same workers, heaps and JVM ergonomic CPU count. CPU count is not affinity. Existing comparable game concurrency is reused.'}
    started = time.monotonic()
    frozen = {str(Path(p).resolve()): b.digest(p) for p in (args.java, args.runner, args.openings, __file__, b.__file__)}
    frozen.update({str(Path(p).resolve()):b.digest(p) for p in (c.__file__,c.n.__file__,owned.__file__)})
    model = Path(args.network).resolve()
    if b.digest(model) != NETWORK_SHA256: raise ValueError('Frozen CP64 mismatch')
    frozen[str(model)] = NETWORK_SHA256
    for side in ('baseline', 'candidate'): frozen.update(b.installation_hashes(getattr(args, side)))
    report['filesBefore'] = frozen
    report['javaVersion'] = subprocess.run([args.java, '-version'], capture_output=True, text=True, check=True).stderr
    commands = {side: command(args, side) for side in ('baseline', 'candidate')}
    report['commands'] = commands
    try:
        for side, cmd in commands.items():
            report[side + 'Preflight'] = preflight(cmd, out / (side + '-uci.txt'),
                                                 getattr(args, side + '_workers'), getattr(args, side + '_policy'), args.hash_mib)
        cmd = [str(Path(args.runner).resolve())]
        for side, engine in commands.items():
            cmd += ['-engine', 'name=' + side, 'cmd=' + engine[0],
                    'args=' + subprocess.list2cmdline(engine[1:]),
                    'option.Threads=' + str(getattr(args, side + '_workers'))]
        cmd += ['-each', 'proto=uci', 'tc=' + args.tc, 'timemargin=0', 'restart=off', 'option.OwnBook=false', 'option.Hash='+str(args.hash_mib),
                '-rounds', str(args.pairs), '-repeat', '-concurrency', str(args.concurrency), '-srand', str(args.seed),
                '-openings', 'file=' + str(Path(args.openings).resolve()), 'format=pgn', 'order=sequential',
                '-pgnout', 'file=' + str(out / 'games.pgn'), 'nodes=true', 'nps=true', 'pv=true', 'timeleft=true', 'latency=true',
                'match_line=^info string request ', '-report',
                'penta=false' if getattr(args, 'operational', False) else 'penta=true',
                '-ratinginterval', '0' if getattr(args, 'operational', False) else '2',
                '-autosaveinterval', '0', '-strict']
        report['runnerCommand'] = cmd; b.save(out / 'run.json', report)
        match_started = time.monotonic()
        with (out / 'runner.txt').open('w', encoding='utf-8') as log:
            job=owned.OwnedJob();process=None;cohort=None
            try:
                process = subprocess.Popen(cmd, cwd=out, env=b.clean_environment(), stdout=log,
                                           stderr=subprocess.STDOUT,creationflags=0x00000004 | (0x00004000 if getattr(args,'background',False) else 0))
                job.assign(process);job.resume_initial_thread(process)
                cohort = MemoryCohort(process.pid,args.concurrency,out/'process-cost.json')
                cohort.attach()
                code = process.wait(timeout=args.timeout)
            except BaseException:
                # Close only the suspended-before-assignment owned Job. Never
                # reconstruct cleanup ownership from recycled parent PID edges.
                job.close()
                if process is not None and process.poll() is None:
                    process.kill();process.wait(timeout=10)
                if cohort is not None:cohort.wait_for_exit()
                raise
            finally:
                try:
                    if cohort is not None:cohort.close()
                finally:job.close()
        report['matchWallSeconds'] = time.monotonic() - match_started
        report['runnerExitCode'] = code
        report['results'] = b.analyze(out / 'games.pgn', args.openings, args.pairs, require_accounting=True,
                                    unordered=args.concurrency > 1,
                                    statistics_enabled=not getattr(args, 'operational', False))
        assert code == 0 and report['results']['terminations'] == {'normal': args.pairs * 2}
        assert all(b.digest(p) == digest for p, digest in frozen.items()), 'Frozen inputs changed'
        report.update(status='passed', inputsStable=True)
    except BaseException as error:
        report.update(status='failed', failure=repr(error)); raise
    finally:
        report['wallSeconds'] = time.monotonic() - started; b.save(out / 'run.json', report)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('baseline', 'candidate', 'network', 'runner', 'openings', 'out'): p.add_argument('--' + name, required=True)
    p.add_argument('--java', default='C:/java/jdk-21/bin/java.exe')
    for side in ('baseline', 'candidate'):
        p.add_argument('--' + side + '-policy', choices=('baseline','no-improving','lazy-improving','nominal-futility','reduction-futility','reduction-no-improving'), default='baseline' if side=='baseline' else 'no-improving')
        p.add_argument('--' + side + '-workers', type=int, choices=range(1,17), default=1)
    p.add_argument('--hash-mib', type=int, choices=range(1,1025), default=64)
    p.add_argument('--background', action='store_true')
    p.add_argument('--operational', action='store_true')
    p.add_argument('--concurrency', type=int, choices=(1,2,3,4), default=1)
    p.add_argument('--pairs', type=int, required=True); p.add_argument('--tc', default='10+0.1')
    p.add_argument('--seed', type=int, default=20261014); p.add_argument('--timeout', type=int, default=7200)
    run(p.parse_args())
