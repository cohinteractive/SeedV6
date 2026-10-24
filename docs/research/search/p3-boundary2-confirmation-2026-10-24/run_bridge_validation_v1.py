"""Calibrate JFR UCI-boundary observation against uninstrumented unchanged actors."""
import ctypes
from datetime import datetime, timezone
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
from types import SimpleNamespace
from verify_inputs import HERE, ROOT, OLD, audit, digest, write_new


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def roots(chess):
    cases = []
    for i, row in enumerate(json.loads((OLD/'confirmation-clock-tail-v1.json').read_text())['adverseRequests']):
        board = chess.Board()
        for move in row['historyUci']:
            board.push_uci(move)
        assert board.fen() == row['rootFen']
        cases.append((f'adverse-{i+1}', 'startpos moves '+' '.join(row['historyUci']), board))
    corpus = ROOT/'docs/research/search/p3-sr002-execution-2026-10-19/confirmation-corpus.tsv'
    for line in corpus.read_text().splitlines()[:12]:
        name, fen, history = line.split('\t')
        board = chess.Board(fen)
        for move in history.split():
            board.push_uci(move)
        cases.append((name, 'fen '+fen+' moves '+history, board))
    assert len(cases) == 14
    return cases


def background():
    k = ctypes.WinDLL('kernel32', use_last_error=True)
    k.GetCurrentProcess.restype = ctypes.c_void_p
    k.SetPriorityClass.argtypes = [ctypes.c_void_p, ctypes.c_ulong]
    assert k.SetPriorityClass(k.GetCurrentProcess(), 0x4000)


def worker(name):
    assert sys.stdin.readline().strip() == 'GO'
    background()
    m = module('spending', OLD/'spending_match_v1.py')
    native = module('native', ROOT/'tools/sr036-process-cost.py')
    out = HERE/'raw'/name
    protocol = json.loads((HERE/'bridge-validation-v1.json').read_text())
    cases = roots(m.b.chess)
    args = SimpleNamespace(baseline=str(OLD/'raw/runtime-production-v2'), candidate=str(OLD/'raw/runtime-candidate-v2'),
        baseline_policy='baseline', candidate_policy='boundary2', baseline_workers=1, candidate_workers=1,
        java='C:/java/jdk-21/bin/java.exe', network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'))
    record = {'status': 'running', 'runs': []}
    try:
        with (out/'requests.jsonl').open('x', encoding='utf8') as stream:
            for index, (profile, instrument) in enumerate(protocol['sequence']):
                label = f'{index+1:02}-{profile}-'+('on' if instrument else 'off')
                command = m.command(args, 'baseline' if profile == 'baseline' else 'candidate')
                if instrument:
                    cp = command.index('-cp')+1
                    command[cp] = str(HERE/'raw/timing-bridge-v1/classes')+os.pathsep+command[cp]
                    entry = command.index('com.ohinteractive.seedv6.integration.Cp64SpendingUciMain')
                    command[entry] = 'com.ohinteractive.seedv6.research.timing.Boundary2TimingMain'
                    # Relative paths avoid drive-colon ambiguity in the JVM unified logging syntax.
                    relative = (out/label).relative_to(ROOT).as_posix()
                    command[1:1] = [f'-Xlog:gc*,safepoint=info:file={relative}-gc.log:utctime,uptimenanos,level,tags:filecount=0',
                        f'-XX:StartFlightRecording=settings=default,filename={relative}.jfr,dumponexit=true,maxsize=64m']
                run = {'label': label, 'profile': profile, 'instrumented': instrument, 'command': command,
                       'startedUtc': datetime.now(timezone.utc).isoformat()}
                record['runs'].append(run)
                engine = m.b.Uci(command, out/(label+'-uci.txt'))
                try:
                    engine.send('uci')
                    run['identity'] = m.verify_identity(engine.until('uciok', 90), 1, profile, 64)
                    schedule = [(root, 'nodes 100000', None) for root in cases]
                    schedule += [(root, f'wtime {clock} btime {clock} winc 100 binc 100', clock)
                                 for clock in (9799, 2933, 500, 150, 51, 50) for root in cases]
                    for ordinal, ((rid, position, board), go, clock) in enumerate(schedule):
                        engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                        engine.send('position '+position); engine.send('isready'); engine.until('readyok')
                        cpu0 = native.snapshot(int(engine.process._handle))['totalCpuSeconds']
                        wall0 = time.time_ns(); tick0 = time.perf_counter_ns()
                        engine.send('go '+go)
                        lines = engine.until('bestmove ', 30)
                        tick1 = time.perf_counter_ns(); wall1 = time.time_ns()
                        cpu1 = native.snapshot(int(engine.process._handle))['totalCpuSeconds']
                        result = m.b.inspect_result(board, lines)
                        depths = [int(x.split()[2]) for x in lines if x.startswith('info depth ')]
                        assert depths == list(range(1, len(depths)+1)), 'Gapped completed depth'
                        assert result['termination'] in ('NODE_LIMIT','TIME_LIMIT','TIME_ALLOCATION','COMPLETED')
                        if clock is None:
                            assert result['nodes'] == 100000 and result['termination'] == 'NODE_LIMIT'
                        engine.send('stop'); engine.send('isready')
                        assert engine.until('readyok') == ['readyok'], 'Stale publication'
                        row = {'run': label, 'profile': profile, 'instrumented': instrument, 'ordinal': ordinal,
                               'root': rid, 'clockMillis': clock, 'go': go, 'startEpochNanos': wall0, 'endEpochNanos': wall1,
                               'externalElapsedNanos': tick1-tick0, 'nativeCpuSeconds': cpu1-cpu0, 'result': result}
                        if clock is not None:
                            base = min(max(0, clock-50), max(1, clock//30+65))
                            hard = base if profile == 'baseline' else min(max(0, clock-50), base*2)
                            row.update(baseMillis=base, hardMillis=hard,
                                       overrunMillis=result['elapsedNanos']/1e6-hard,
                                       externalOverrunMillis=(tick1-tick0)/1e6-hard)
                            if clock <= 50:
                                assert result['nodes'] == 0 and result['finalInfo'] is None
                        stream.write(json.dumps(row, allow_nan=False)+'\n'); stream.flush()
                    run['requests'] = len(schedule)
                finally:
                    engine.close()
                    run['nativeLifetime'] = native.snapshot(int(engine.process._handle))
                    run['exitCode'] = engine.process.returncode
                assert run['exitCode'] == 0
        record['status'] = 'passed'
    except BaseException as error:
        record.update(status='failed', failure=repr(error))
        raise
    finally:
        write_new(out/'execution.json', record)


def main(name):
    assert re.fullmatch(r'[a-z0-9-]+', name)
    background()
    host = module('host', ROOT/'tools/p3-tt-session.py')
    owned = module('owned', ROOT/'tools/tournament-process.py')
    out = HERE/'raw'/name
    assert not out.exists(), 'Never overwrite an attempt'
    out.mkdir(parents=True)
    checked = audit()
    files = [Path(__file__), HERE/'bridge-validation-v1.json', HERE/'verify_inputs.py', HERE/'Boundary2TimingMain.java',
             ROOT/'tools/tournament-process.py', ROOT/'tools/p3-tt-session.py',
             ROOT/'tools/sr036-process-cost.py', ROOT/'tools/search-baseline.py',
             OLD/'spending_match_v1.py', OLD/'confirmation-clock-tail-v1.json',
             ROOT/'docs/research/search/p3-sr002-execution-2026-10-19/confirmation-corpus.tsv',
             Path('C:/java/jdk-21/lib/jfr/default.jfc')]
    files += sorted(p for p in (HERE/'raw/timing-bridge-v1/classes').rglob('*') if p.is_file())
    binding = {str(p): digest(p) for p in files}
    write_new(HERE/(name+'-binding.json'), {'historicalInputAudit': checked, 'files': binding})
    start = host.host_sample(); time.sleep(30)
    admission = host.host_interval(start, host.host_sample())
    admission['passed'] = admission['otherAverageLogicalCores'] <= 2 and admission['memoryEndpointsPass']
    write_new(HERE/(name+'-admission.json'), admission)
    if not admission['passed']:
        print('Resource admission failed; no diagnostic requests started', flush=True)
        return
    start = host.host_sample()
    record = {'status': 'running', 'bindingSha256': digest(HERE/(name+'-binding.json'))}
    try:
        with (out/'worker.log').open('xb') as stream:
            record['execution'] = owned.run_owned([sys.executable, '-B', str(Path(__file__).resolve()), name, 'worker'],
                                                  stream, 900, ROOT, os.environ.copy())
        assert record['execution']['exitCode'] == 0
        record['resources'] = host.host_interval(start, host.host_sample(), record['execution']['totalOwnedCpuSeconds'])
        record['resources']['passed'] = record['resources']['otherAverageLogicalCores'] <= 2 and record['resources']['memoryEndpointsPass']
        for path, sha in binding.items():
            assert digest(path) == sha, path
        audit()
        record['status'] = 'passed' if record['resources']['passed'] else 'resource-unqualified'
    except BaseException as error:
        record.update(status='failed', failure=repr(error))
        raise
    finally:
        record['evidence'] = {str(p.relative_to(HERE)): digest(p) for p in out.iterdir() if p.is_file()}
        write_new(HERE/(name+'-summary.json'), record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('evidence','resources')}, indent=2))


if __name__ == '__main__':
    if len(sys.argv) == 3 and sys.argv[2] == 'worker':
        worker(sys.argv[1])
    else:
        main(sys.argv[1])
