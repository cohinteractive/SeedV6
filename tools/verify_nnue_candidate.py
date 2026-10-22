"""Offline release gate. Requires chess==1.11.2 and psutil; never reads BotLi config or connects online."""
import argparse
import asyncio
import hashlib
import json
import logging
import os
from pathlib import Path
import platform
import queue
import random
import re
import shutil
import subprocess
import threading
import time
import zipfile

import chess
import chess.engine
import psutil

SHA = '8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b'
PROFILE = 'E008_QXRAY_LMR_WINDOW_EFFORT_HINTS'
MAIN = 'com.ohinteractive.seedv6.uci.NnueCandidateUciMain'
WARNING = 'WARNING: Using incubator modules: jdk.incubator.vector'


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


class Engine:
    def __init__(self, command, cwd, env, report):
        self.start = time.monotonic()
        self.p = subprocess.Popen(list(map(str, command)), cwd=cwd, env=env, stdin=subprocess.PIPE,
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1)
        self.record = {'command': list(map(str, command)), 'sent': [], 'stdout': [], 'stderr': [], 'peak_tree_rss_bytes': 0}
        report['sessions'].append(self.record)
        self.events = queue.Queue()
        self.done = threading.Event()
        self.owned = {self.p.pid}
        def read(stream, channel):
            for line in stream:
                line = line.rstrip('\r\n')
                self.record[channel].append(line)
                if channel == 'stdout':
                    self.events.put(line)
        def sample():
            while not self.done.wait(.02):
                try:
                    processes = [psutil.Process(self.p.pid)]
                    processes += processes[0].children(recursive=True)
                    self.owned.update(p.pid for p in processes)
                    rss = sum(p.memory_info().rss for p in processes)
                    self.record['peak_tree_rss_bytes'] = max(self.record['peak_tree_rss_bytes'], rss)
                except psutil.Error:
                    pass
        self.readers = [threading.Thread(target=read, args=(self.p.stdout, 'stdout'), daemon=True),
                        threading.Thread(target=read, args=(self.p.stderr, 'stderr'), daemon=True)]
        for reader in self.readers:
            reader.start()
        self.sampler = threading.Thread(target=sample, daemon=True)
        self.sampler.start()

    def send(self, text):
        self.record['sent'].append(text)
        self.p.stdin.write(text + '\n'); self.p.stdin.flush()

    def until(self, prefix, timeout=20):
        deadline = time.monotonic() + timeout
        lines = []
        while time.monotonic() < deadline:
            try:
                line = self.events.get(timeout=max(.001, deadline - time.monotonic()))
            except queue.Empty:
                break
            check(line.startswith(('id ', 'option ', 'info ', 'bestmove ')) or line in ('uciok', 'readyok'),
                  'Non-UCI stdout: ' + repr(line))
            lines.append(line)
            if line.startswith(prefix):
                return lines
        raise AssertionError('Timeout: ' + prefix + '; stderr=' + repr(self.record['stderr']))

    def ready(self):
        self.send('isready'); return self.until('readyok')

    def intro(self, scalar=False, hash_mib=64):
        self.send('uci'); lines = self.until('uciok')
        identity = next(s for s in lines if s.startswith('info string candidate='))
        for expected in (SHA, PROFILE, 'evaluator=CONVENTIONAL_NNUE', 'checkpoint=64', 'schema=S6CNU002',
                         'heads=8', 'workers=1', 'hashMiB=' + str(hash_mib)):
            check(expected in identity, 'Missing identity ' + expected)
        check('option name Threads type spin default 1 min 1 max 1' in lines, 'Wrong worker advertisement')
        if scalar:
            check('head=scalar-long' in identity, 'Scalar fallback did not activate')
        self.record['identity'] = identity
        self.record['startup_handshake_seconds'] = time.monotonic() - self.start
        self.ready()
        return identity

    def search(self, position, board, go, timeout=20, maximum_seconds=None):
        self.send('position ' + position)
        start = time.monotonic()
        self.send('go ' + go)
        lines = self.until('bestmove ', timeout)
        elapsed = time.monotonic() - start
        move = lines[-1].split()[1]
        check((move == '0000' and not any(board.legal_moves)) or
              (move != '0000' and chess.Move.from_uci(move) in board.legal_moves), 'Illegal move ' + move + ' for ' + board.fen())
        for line in lines:
            if ' pv ' in line:
                pv_board = board.copy()
                for text in line.split(' pv ', 1)[1].split():
                    m = chess.Move.from_uci(text)
                    check(m in pv_board.legal_moves, 'Illegal PV ' + line)
                    pv_board.push(m)
        check(not any('opening book' in line for line in lines), 'Unexpected book route')
        if maximum_seconds is not None:
            check(elapsed < maximum_seconds, 'Time management overrun: ' + str(elapsed))
        info = [line for line in lines if line.startswith('info depth ')]
        observation = {'fen': board.fen(), 'go': go, 'move': move, 'wall_seconds': elapsed,
                       'last_iteration': info[-1] if info else None}
        self.record.setdefault('searches', []).append(observation)
        return observation

    def close(self, eof=False, rejection=False):
        if eof:
            self.p.stdin.close()
        else:
            self.send('quit')
        self.p.wait(timeout=5)
        for reader in self.readers:
            reader.join(timeout=3)
            check(not reader.is_alive(), 'Descendant retained UCI pipes after exit')
        self.done.set(); self.sampler.join(timeout=1)
        check(self.p.returncode == 0, 'Nonzero engine exit')
        for line in self.record['stderr']:
            check(line == WARNING or (rejection and line.startswith('SeedV6 UCI rejected command:')),
                  'Unexpected engine error: ' + line)
        check(not any(psutil.pid_exists(pid) for pid in self.owned), 'Owned process survived shutdown')
        self.record['exit_code'] = self.p.returncode

    def cleanup(self):
        if self.p.poll() is None:
            try:
                if not self.p.stdin.closed:
                    self.p.stdin.close()
                self.p.wait(timeout=5)
            except (OSError, subprocess.TimeoutExpired):
                for child in psutil.Process(self.p.pid).children(recursive=True):
                    child.kill()
                self.p.kill(); self.p.wait(timeout=5)
        self.done.set()


def signature(observation):
    line = observation['last_iteration'] or ''
    # Time and NPS are machine dependent; scores, completed depth, nodes and PV are exact.
    return re.sub(r' (?:time|nps) \d+', '', line), observation['move']


async def botli_compatible(command, cwd, env):
    # Same popen_uci/configure/play/quit boundary as Torom/BotLi engine.py; no client startup.
    messages = []
    class Capture(logging.Handler):
        def emit(self, record):
            messages.append(record.getMessage())
    handler = Capture()
    logger = logging.getLogger('chess.engine')
    logger.addHandler(handler)
    transport, engine = await chess.engine.popen_uci(command, cwd=cwd, env=env)
    try:
        check(engine.options['Threads'].max == 1, 'python-chess saw wrong Threads option')
        await engine.configure({'Threads': 1, 'Hash': 256, 'UseBook': False})
        await engine.ping()
        board = chess.Board()
        moves = []
        for game in (object(), object()):
            board.reset()
            for ply in range(8):
                limit = chess.engine.Limit(time=.1) if ply < 2 else chess.engine.Limit(
                    white_clock=3, black_clock=3, white_inc=.05, black_inc=.05)
                result = await engine.play(board, limit, game=game, ponder=False, info=chess.engine.INFO_ALL)
                check(result.move in board.legal_moves, 'BotLi-compatible illegal move')
                moves.append(result.move.uci()); board.push(result.move)
        check(not any('Unexpected engine output' in m for m in messages), 'Unsolicited protocol output during BotLi configuration')
        return {'status': 'passed', 'adapter': 'python-chess ' + chess.__version__, 'moves': moves,
                'options': list(engine.options), 'id': engine.id, 'ponder': False, 'hash_mib': 256}
    finally:
        await asyncio.wait_for(engine.quit(), 5)
        transport.close()
        logger.removeHandler(handler)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True, help='Fresh evidence directory')
    parser.add_argument('--frozen-lib', type=Path, help='Optional frozen E008 UCI jars (Windows reproduction)')
    parser.add_argument('--frozen-classes', type=Path)
    parser.add_argument('--network', type=Path, help='Pinned network for frozen launcher only')
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=False)
    install = out / 'relocated engine with spaces'
    shutil.copytree(args.install.resolve(), install)
    cwd = out / 'unrelated BotLi working directory'; cwd.mkdir()
    executable = install / 'bin' / ('seedv6-nnue-cp64.bat' if os.name == 'nt' else 'seedv6-nnue-cp64')
    command = [str(executable)]
    env = os.environ.copy()
    for key in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'JAVA_OPTS', 'SEED_NNUE_OPERATION'):
        env.pop(key, None)
    env['DEBUG'] = 'true'
    report = {'status': 'running', 'platform': platform.platform(), 'machine': platform.machine(),
              'native_mac': platform.system() == 'Darwin', 'sessions': [], 'network_sha256': SHA,
              'jar_sha256': {p.name: sha(p) for p in sorted((install / 'lib').glob('*.jar'))}}
    active = []
    try:
        check(set(report['jar_sha256']) == {'app-uci.jar', 'engine-core.jar'}, 'Unexpected runtime dependencies')
        with zipfile.ZipFile(install / 'lib/app-uci.jar') as jar:
            network = jar.read('models/conventional-cp64.cnnue')
        check(hashlib.sha256(network).hexdigest() == SHA, 'Wrong packaged network')
        def open_engine(extra=(), cmd=None, environment=None):
            e = Engine(cmd or command + list(extra), cwd, environment or env, report)
            active.append(e)
            return e

        roots = [chess.Board(), chess.Board('r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1'),
                 chess.Board('4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1'),
                 chess.Board('7k/P7/8/8/8/8/8/7K w - - 0 1')]
        comparisons = []
        for extra in ([], ['--scalar']):
            e = open_engine(extra); e.intro(scalar=bool(extra))
            e.send('setoption name Hash value 32'); e.ready()
            results = []
            for board in roots:
                e.send('ucinewgame')
                results.append(e.search('fen ' + board.fen(), board, 'depth 5'))
            comparisons.append(results)
            e.close()
        check([signature(x) for x in comparisons[0]] == [signature(x) for x in comparisons[1]],
              'Scalar/vector search mismatch')
        report['scalar_vector_depth5_equal'] = True

        e = open_engine(); e.intro()
        e.search('startpos', chess.Board(), 'depth 4')
        e.send('setoption name Threads value 1\nsetoption name Hash value 256\nsetoption name UseBook value false\nsetoption name OwnBook value false')
        e.ready(); e.intro(hash_mib=256)
        # Negative option must retain the qualified single-worker service.
        e.send('setoption name Threads value 2'); e.ready(); e.intro(hash_mib=256)
        for i in range(24):
            if i % 6 == 0:
                e.send('ucinewgame\nsetoption name Clear Hash')
            board = roots[i % len(roots)]
            e.search('fen ' + board.fen(), board, 'movetime 150', maximum_seconds=.75)
        for board in roots[:2]:
            e.send('ucinewgame')
            e.search('fen ' + board.fen(), board, 'movetime 1000', maximum_seconds=1.5)
        for board in (chess.Board(), chess.Board('rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1')):
            e.search('fen ' + board.fen(), board, 'wtime 3000 btime 3000 winc 100 binc 100', maximum_seconds=.75)
            e.search('fen ' + board.fen(), board, 'wtime 50 btime 50 winc 0 binc 0', maximum_seconds=.5)
        rng = random.Random(20261023)
        board = chess.Board(); moves = []
        for _ in range(160):
            options = list(board.legal_moves); rng.shuffle(options)
            for move in options:
                trial = board.copy(); trial.push(move)
                if not trial.is_game_over(claim_draw=True):
                    board = trial; moves.append(move.uci()); break
            else:
                break
        check(len(moves) >= 100, 'Long sequence fixture unexpectedly short')
        e.search('startpos moves ' + ' '.join(moves), board, 'depth 4')
        report['long_sequence_plies'] = len(moves)
        for fen in ('7k/6Q1/5K2/8/8/8/8/8 b - - 0 1', '7k/5K2/6Q1/8/8/8/8/8 b - - 0 1'):
            e.search('fen ' + fen, chess.Board(fen), 'depth 4')
        e.send('position startpos\ngo infinite'); e.ready()
        time.sleep(.1)
        start = time.monotonic(); e.send('stop')
        lines = e.until('bestmove ', 2)
        check(chess.Move.from_uci(lines[-1].split()[1]) in chess.Board().legal_moves, 'Illegal stopped move')
        report['stop_seconds'] = time.monotonic() - start
        e.send('position startpos\ngo infinite\nucinewgame'); lines = e.ready()
        check(not any(s.startswith('bestmove ') for s in lines), 'Stale publication after reset')
        e.search('startpos', chess.Board(), 'depth 3')
        e.send('position startpos\ngo infinite'); e.close(rejection=True)
        e = open_engine(['--scalar']); e.intro(scalar=True)
        e.send('position startpos\ngo infinite'); e.close(eof=True)

        # Exact launch API used by BotLi, with its chess dependency version.
        report['botli_compatible'] = asyncio.run(botli_compatible(command, cwd, env))

        # java.base-only runtime proves the module-absent scalar path in actual search.
        java = str(Path(env['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if env.get('JAVA_HOME') else 'java'
        base_command = [java, '--limit-modules', 'java.base', '-Xmx768m', '-XX:ActiveProcessorCount=2',
                        '-cp', str(install / 'lib/*'), MAIN]
        e = open_engine(cmd=base_command); e.intro(scalar=True)
        e.search('startpos', chess.Board(), 'depth 4'); e.close()

        # Fail closed for every required-asset failure, using copies of the delivered JAR.
        bad = out / 'invalid network.cnnue'; bad.write_bytes(network[:-1])
        for extra in (['--network', str(out / 'missing.cnnue')], ['--network', str(bad)], ['--nnue', str(bad)]):
            result = subprocess.run(command + extra, cwd=cwd, env=env, capture_output=True, text=True, timeout=20)
            check(result.returncode != 0 and not result.stdout and 'startup/runtime error:' in result.stderr,
                  'Invalid external asset/options did not fail closed')
        for case in ('missing', 'corrupt', 'incompatible', 'different-valid-checksum'):
            bad_install = out / ('asset-' + case); shutil.copytree(install, bad_install)
            jar_path = bad_install / 'lib/app-uci.jar'
            with zipfile.ZipFile(jar_path) as jar:
                entries = [(i, jar.read(i)) for i in jar.infolist() if i.filename != 'models/conventional-cp64.cnnue']
            if case != 'missing':
                altered = bytearray(network)
                altered[7 if case == 'incompatible' else -1] ^= 1
                if case == 'different-valid-checksum':
                    altered[40:72] = hashlib.sha256(altered[72:]).digest()
                entries.append(('models/conventional-cp64.cnnue', bytes(altered)))
            with zipfile.ZipFile(jar_path, 'w', zipfile.ZIP_DEFLATED) as jar:
                for info, data in entries:
                    jar.writestr(info, data)
            result = subprocess.run([str(bad_install / 'bin' / executable.name)], input='uci\nisready\nquit\n',
                                    cwd=cwd, env=env, capture_output=True, text=True, timeout=20)
            check(result.returncode != 0 and not result.stdout and 'startup/runtime error:' in result.stderr,
                  'Packaged ' + case + ' asset did not fail closed')
        report['asset_rejections'] = ['missing', 'truncated', 'corrupt', 'incompatible', 'different-valid-checksum']
        check(not list(cwd.iterdir()), 'Engine wrote files into the client working directory')
        check(report['jar_sha256'] == {p.name: sha(p) for p in sorted((install / 'lib').glob('*.jar'))},
              'Runtime modified its own jars')
        report['no_client_files_or_runtime_mutation'] = True

        if args.frozen_lib:
            check(args.frozen_classes and args.network, 'Frozen comparison needs classes and network')
            frozen = [java, '--add-modules=jdk.incubator.vector', '-Xmx768m', '-XX:ActiveProcessorCount=2',
                      '-cp', str(args.frozen_lib.resolve() / '*') + os.pathsep + str(args.frozen_classes.resolve()),
                      'SeedNnueSelectiveResearch', str(args.network.resolve())]
            frozen_env = env.copy()
            frozen_env['SEED_NNUE_OPERATION'] = os.environ['SEED_NNUE_OPERATION']
            e = open_engine(cmd=frozen, environment=frozen_env)
            e.send('uci'); e.until('uciok'); e.ready()
            e.send('setoption name Hash value 32'); e.ready()
            results = []
            for board in roots:
                e.send('ucinewgame'); results.append(e.search('fen ' + board.fen(), board, 'depth 5'))
            e.close()
            check([signature(x) for x in results] == [signature(x) for x in comparisons[0]], 'Frozen E008 runtime mismatch')
            report['frozen_e008_depth5_equal'] = True
        report['status'] = 'passed'
        print('PASS: pinned NNUE/search, scalar/vector, UCI lifecycle, offline BotLi API, integrity and shutdown gates')
    finally:
        for engine in active:
            engine.cleanup()
        (out / 'result.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
