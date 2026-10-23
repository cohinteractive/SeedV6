"""Headless checks against actual bundled launchers; Python standard library only."""
import os
from pathlib import Path
import queue
import re
import subprocess
import threading
import time
import zipfile

UCI_MODULES = ('java.base', 'jdk.incubator.vector')
NNUE_LAUNCHER = 'SeedV6-NNUE-CP64'
NNUE_MAIN = 'com.ohinteractive.seedv6.uci.NnueCandidateUciMain'
VECTOR_WARNING = 'WARNING: Using incubator modules: jdk.incubator.vector'


def check(ok, message):
    if not ok:
        raise RuntimeError(message)


def verify_dependencies(application, app, modules, target):
    jars = sorted(app.glob('*.jar'))
    names = {j.name for j in jars}
    if target == 'windows' or application == 'gui':
        config = (app / ('SeedV6-' + application.upper() + '.cfg')).read_text()
        classpath = [line.split('=', 1)[1].replace('\\', '/') for line in config.splitlines()
                     if line.startswith('app.classpath=')]
        check(set(classpath) == {'$APPDIR/' + name for name in names}, 'Launcher classpath differs from verified runtime JARs')
        main = 'com.ohinteractive.seedv6.' + ('gui.SwingLauncher' if application == 'gui' else 'uci.UciMain')
        check('app.mainclass=' + main in config, 'Wrong packaged entry point')
        check(('java-options=--add-modules=jdk.incubator.vector' if application == 'gui'
               else 'java-options=-Djava.awt.headless=true') in config, 'Missing packaged JVM options')
    if application == 'uci':
        check(names == {'engine-core.jar', 'app-uci.jar'}, 'UCI has forbidden/missing runtime JARs: ' + repr(names))
        check({m.split('@', 1)[0] for m in modules} == set(UCI_MODULES),
              'UCI runtime must contain exactly ' + ','.join(UCI_MODULES))
        if target == 'windows':
            config = (app / (NNUE_LAUNCHER + '.cfg')).read_text()
            check('app.mainclass=' + NNUE_MAIN in config, 'Wrong NNUE packaged entry point')
            for option in ('--add-modules=jdk.incubator.vector', '-XX:ActiveProcessorCount=2', '-Xmx768m', '-Djava.awt.headless=true'):
                check('java-options=' + option in config, 'Missing NNUE JVM option ' + option)
            classpath = [line.split('=', 1)[1].replace('\\', '/') for line in config.splitlines()
                         if line.startswith('app.classpath=')]
            check(set(classpath) == {'$APPDIR/' + name for name in names}, 'Wrong NNUE launcher classpath')
    else:
        required = {'engine-core.jar', 'engine-training.jar', 'app-gui.jar', 'desktop-package.jar',
                    'flatlaf-3.7.jar', 'jna-5.17.0.jar', 'jna-platform-5.17.0.jar', 'gson-2.11.0.jar',
                    'zstd-jni-1.5.7-20.jar', 'sqlite-jdbc-3.53.4.0.jar', 'error_prone_annotations-2.27.0.jar'}
        check(names == required, 'Unexpected GUI runtime closure: ' + repr(names ^ required))
        for required_module in ('java.desktop', 'java.sql', 'jdk.unsupported', 'jdk.incubator.vector'):
            check(any(m.startswith(required_module + '@') for m in modules), 'Missing runtime module ' + required_module)
        native_payloads = {
            'flatlaf-3.7.jar': ('com/formdev/flatlaf/natives/flatlaf-windows-x86_64.dll',
                              'com/formdev/flatlaf/natives/libflatlaf-macos-arm64.dylib'),
            'jna-5.17.0.jar': ('com/sun/jna/win32-x86-64/jnidispatch.dll', 'com/sun/jna/darwin-aarch64/libjnidispatch.jnilib'),
            'sqlite-jdbc-3.53.4.0.jar': ('org/sqlite/native/Windows/x86_64/sqlitejdbc.dll', 'org/sqlite/native/Mac/aarch64/libsqlitejdbc.dylib'),
            'zstd-jni-1.5.7-20.jar': ('win/amd64/libzstd-jni-1.5.7-20.dll', 'darwin/aarch64/libzstd-jni-1.5.7-20.dylib')}
        for jar, payloads in native_payloads.items():
            with zipfile.ZipFile(app / jar) as archive:
                check(payloads[0 if target == 'windows' else 1] in archive.namelist(), 'Missing target native library: ' + jar)
    classes = set()
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            entries = {e for e in archive.namelist() if not e.endswith('/')}
            for name in entries:
                if name.endswith('.class'):
                    check(name not in classes or name.endswith('module-info.class'), 'Duplicate class: ' + name)
                    classes.add(name)
            if jar.name in ('engine-core.jar', 'app-gui.jar', 'app-uci.jar', 'engine-training.jar'):
                check(not any('/verification/' in e or '/research/' in e or '/seedv6/tools/' in e
                              or '/seedv6/integration/' in e or e == 'com/ohinteractive/seedv6/Main.class'
                              for e in entries), 'Research/legacy implementation leaked into ' + jar.name)
            if application == 'uci':
                check(not any(e.startswith('com/ohinteractive/seedv6/' + owner + '/')
                              for e in entries for owner in ('gui', 'training', 'corpus')), 'UCI owner leakage')
    if target == 'windows':
        # Inspect PE subsystem without starting a window; GUI=2, console=3.
        launchers = ['SeedV6-' + application.upper()] + ([NNUE_LAUNCHER] if application == 'uci' else [])
        for name in launchers:
            data = (app.parent / (name + '.exe')).read_bytes()
            pe = int.from_bytes(data[0x3c:0x40], 'little')
            check(data[pe:pe + 4] == b'PE\0\0', 'Missing PE launcher')
            check(int.from_bytes(data[pe + 4:pe + 6], 'little') == 0x8664, 'Launcher is not x64')
            subsystem = int.from_bytes(data[pe + 24 + 68:pe + 24 + 70], 'little')
            check(subsystem == (2 if application == 'gui' else 3), 'Wrong Windows launcher subsystem')


class Engine:
    def __init__(self, command, cwd, env, allowed_stderr=()):
        self.allowed_stderr = allowed_stderr
        self.process = subprocess.Popen(list(map(str, command)), cwd=cwd, env=env,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
            encoding='utf-8', errors='replace', bufsize=1)
        self.lines = []; self.errors = []; self.events = queue.Queue(); self.sent = []
        def read(stream, destination, events=None):
            for line in stream:
                line = line.rstrip('\r\n'); destination.append(line)
                if events is not None:
                    events.put(line)
        self.readers = [threading.Thread(target=read, args=(self.process.stdout, self.lines, self.events), daemon=True),
                        threading.Thread(target=read, args=(self.process.stderr, self.errors), daemon=True)]
        for reader in self.readers:
            reader.start()

    def send(self, text):
        self.sent.append(text); self.process.stdin.write(text + '\n'); self.process.stdin.flush()

    def until(self, prefix, timeout=12):
        end = time.monotonic() + timeout; seen = []
        while time.monotonic() < end:
            try:
                line = self.events.get(timeout=max(.01, end - time.monotonic()))
            except queue.Empty:
                break
            seen.append(line)
            if line.startswith(prefix):
                return seen
        raise RuntimeError(f'UCI timeout waiting for {prefix}: {seen}; stderr={self.errors}')

    def close(self, eof=False):
        if eof:
            self.process.stdin.close()
        else:
            self.send('quit')
        self.process.wait(timeout=5)
        for reader in self.readers:
            reader.join(timeout=2)
            check(not reader.is_alive(), 'UCI child retained protocol streams after termination')
        check(self.process.returncode == 0, 'UCI nonzero exit: ' + repr(self.errors))
        check(all(line in self.allowed_stderr for line in self.errors), 'Unexpected UCI stderr: ' + repr(self.errors))

    def kill(self):
        # Closing stdin also drains a restarted JDK launcher child on failing tests.
        if not self.process.stdin.closed:
            self.process.stdin.close()
        if self.process.poll() is None:
            self.process.kill(); self.process.wait(timeout=5)


def verify_uci_book(engine, workers=1):
    """Real protocol checks shared by Java-distribution and native-package validation."""
    start = {f'{f}2{f}{r}' for f in 'abcdefgh' for r in '34'} | {'b1a3', 'b1c3', 'g1f3', 'g1h3'}
    black = {f'{f}7{f}{r}' for f in 'abcdefgh' for r in '65'} | {'b8a6', 'b8c6', 'g8f6', 'g8h6'}
    opening = {'c2c4', 'd2d4', 'e2e4', 'g1f3'}
    searches = 0

    def search(position='startpos', choices=start, book=False, go='depth 2'):
        nonlocal searches
        engine.send('position ' + position + '\ngo ' + go)
        lines = engine.until('bestmove ')
        check(lines[-1] in {'bestmove ' + move for move in choices}, 'Unexpected/illegal book-test move: ' + repr(lines))
        check(all(line.startswith('info ') for line in lines[:-1]), 'Non-UCI search output: ' + repr(lines))
        check(('info string opening book' in lines) == book, 'Wrong book/search route: ' + repr(lines))
        if book:
            check(not any(line.startswith('info depth ') for line in lines), 'Book invented a searched score/depth')
            check(any('request nodes=0 ' in line and 'termination=BOOK' in line for line in lines),
                  'Book hit started ordinary search')
        else:
            check(any(line.startswith('info depth ') for line in lines), 'Book miss/disable did not search')
        # A stop after completion must not republish a book result; readiness fences output.
        engine.send('stop\nisready')
        check(engine.until('readyok') == ['readyok'], 'Duplicate bestmove or late search output')
        searches += 1

    search()  # Default is off.
    engine.send('setoption name UseBook value true')
    search(choices=opening, book=True, go='wtime 5000 btime 5000 winc 20 binc 20')
    engine.send('setoption name OwnBook value false')
    search()
    engine.send('setoption name OwnBook value true')
    search(choices=opening, book=True)
    engine.send('setoption name UseBook value false')
    search()
    engine.send('  setoption   name   uSeBoOk   value   TRUE  ')
    # Both aliases share state, survive service replacement and new games, and reject bad values.
    engine.send(f'setoption name Threads value {2 if workers == 1 else 1}\nsetoption name Hash value 9')
    engine.send('setoption name OwnBook value invalid\nucinewgame')
    search(choices=opening, book=True)
    engine.send(f'setoption name Threads value {workers}\nsetoption name Hash value 8\nsetoption name Clear Hash')
    # Clear Hash/Threads announce identity; drain it before the next search.
    engine.send('isready')
    check(all(line.startswith('info string ') or line == 'readyok' for line in engine.until('readyok')),
          'Unexpected option output')
    search('startpos moves e2e4', black, True)
    search('startpos moves a2a3', black)
    search('fen 4k3/8/8/8/8/8/4P3/4K3 w - - 0 1',
           {'e1d1', 'e1f1', 'e1d2', 'e1f2', 'e2e3', 'e2e4'})
    search('fen r1b1k2r/pp1pppbp/2n2np1/q7/2BNP3/2N1B3/PPP2PPP/R2QK2R w KQkq - 0 1', {'e1g1'}, True)
    search('fen r1b1k2r/1pq1bppp/p1nppn2/8/3NPP2/2N1B3/PPP1B1PP/R2Q1R1K b kq - 0 1', {'e8g8'}, True)
    search('fen 7k/P7/8/8/8/8/8/7K w - - 0 1',
           {'h1g1', 'h1g2', 'h1h2', 'a7a8q', 'a7a8r', 'a7a8b', 'a7a8n'})
    # History and rule draws must bypass even a covered position.
    search('startpos moves g1f3 g8f6 f3g1 f6g8 g1f3 g8f6 f3g1 f6g8')
    search('fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 100 80')
    engine.send('position startpos\ngo infinite')
    analysis = engine.until('info depth 2 ')
    check(not any(line.startswith('bestmove ') or line == 'info string opening book' for line in analysis),
          'Infinite analysis returned a book move')
    engine.send('stop')
    check(engine.until('bestmove ')[-1] in {'bestmove ' + move for move in start}, 'Illegal stopped analysis move')
    searches += 1
    engine.send('setoption name OwnBook value FALSE\nucinewgame')
    search()
    return searches


def verify_uci(launcher, stage, env, model):
    cwd = stage / 'unrelated client working directory'; cwd.mkdir(exist_ok=True)
    legal_start = {f'{file}2{file}{rank}' for file in 'abcdefgh' for rank in '34'} | {'b1a3', 'b1c3', 'g1f3', 'g1h3'}
    checks = []; transcripts = []; searches = 0
    for workers in (1, 4, 16):
        engine = Engine([launcher], cwd, env, allowed_stderr=('WARNING: Using incubator modules: jdk.incubator.vector',))
        try:
            engine.send('uci'); handshake = engine.until('uciok')
            check(any(line.startswith('id name SeedV6 Construction V1 NNUE CP64 ') for line in handshake), 'Wrong UCI identity')
            for option in ('Threads', 'Hash', 'Clear Hash'):
                check(any('option name ' + option + ' ' in line for line in handshake), 'Missing option ' + option)
            for option in ('UseBook', 'OwnBook'):
                check(f'option name {option} type check default false' in handshake, 'Missing book alias ' + option)
            engine.send(f'setoption name Threads value {workers}\nsetoption name Hash value 8\nsetoption name Clear Hash\nisready')
            engine.until('readyok')
            engine.send('uci'); identity = '\n'.join(engine.until('uciok'))
            check(f'workers={workers};hashMiB=8' in identity and 'search=construction-v1' in identity
                  and 'checkpoint=64 schema=S6CNU002 heads=8' in identity and 'flags=262143' in identity
                  and 'aspiration=ADAPTIVE16' in identity, 'Wrong baseline search/evaluator identity')
            check('parallel=' + ('OWNER' if workers == 1 else 'ROOT_REUSE') in identity, 'Worker policy differs')
            for go in ('depth 3', 'movetime 100', 'wtime 5000 btime 5000 winc 20 binc 20 movestogo 20'):
                engine.send('ucinewgame\nposition startpos\ngo ' + go)
                lines = engine.until('bestmove ')
                check(lines[-1].split()[1] in legal_start, 'Illegal starting move: ' + lines[-1])
                searches += 1
            engine.send('position startpos\ngo infinite'); engine.until('info depth 2')
            engine.send('stop'); check(engine.until('bestmove ')[-1].split()[1] in legal_start, 'Illegal stopped move')
            searches += 1
            engine.send('position startpos moves e2e4 e7e5 g1f3\ngo depth 2')
            check(engine.until('bestmove ')[-1].split()[1] != '0000', 'Position move setup failed')
            searches += 1
            searches += verify_uci_book(engine, workers)
            engine.close()
            checks.append(f'Threads={workers}: handshake/ready/hash/clear/depth/timed/clocks/stop/position/quit/book aliases and fallback')
            transcripts.append({'workers': workers, 'sent': engine.sent, 'stdout': engine.lines, 'stderr': engine.errors})
        finally:
            engine.kill()
    for termination in ('EOF', 'terminate'):
        engine = Engine([launcher], cwd, env, allowed_stderr=('WARNING: Using incubator modules: jdk.incubator.vector',))
        try:
            engine.send('setoption name Threads value 4\nposition startpos\ngo infinite')
            engine.until('info depth 2')
            if termination == 'EOF':
                engine.close(eof=True)
            else:
                engine.process.terminate(); engine.process.wait(timeout=5)
                for reader in engine.readers:
                    reader.join(timeout=2)
                    check(not reader.is_alive(), 'Process termination retained pipes')
            checks.append(termination + ' during active multiworker search exits and closes pipes')
        finally:
            engine.kill()
    # Negative inputs are private copies, never mutations of a completed candidate.
    for name, contents in [('missing-model', None), ('corrupt-model', b'not a model'),
                           ('wrong-hash-model', bytes([model.read_bytes()[0] ^ 1]) + model.read_bytes()[1:])]:
        bad = stage / name
        if contents is not None and not bad.exists():
            bad.write_bytes(contents)
        result = subprocess.run([str(launcher), '--model', str(bad)], cwd=cwd, env=env,
            input='uci\n', capture_output=True, text=True, timeout=5)
        check(result.returncode != 0 and not result.stdout and 'SeedV6 UCI startup/runtime error:' in result.stderr,
              'Invalid model did not fail cleanly: ' + name)
    checks.append('Missing/corrupt/incompatible model fails nonzero on stderr without protocol/fallback')
    return {'status': 'passed', 'searches': searches, 'checks': checks, 'transcripts': transcripts,
            'externalJava': 'PATH empty; JAVA_HOME/JDK_HOME removed', 'guiTraining': 'absent'}


def verify_baseline_assets(stage, env, java, app):
    """The ordinary default fails closed with damaged embedded CP64, even with valid BREP2 present."""
    entry = 'models/conventional-cp64.cnnue'
    for kind in ('missing', 'truncated', 'corrupt', 'incompatible'):
        import tempfile
        directory = Path(tempfile.mkdtemp(prefix='baseline-network-'+kind+'-',dir=stage))
        for source in (app / 'engine-core.jar', app / 'app-uci.jar'):
            with zipfile.ZipFile(source) as original, zipfile.ZipFile(directory/source.name, 'w') as changed:
                for info in original.infolist():
                    data = original.read(info.filename)
                    if info.filename == entry:
                        if kind == 'missing': continue
                        if kind == 'truncated': data = data[:-1]
                        else:
                            data = bytearray(data); data[7 if kind == 'incompatible' else -1] ^= 1
                    changed.writestr(info, data)
        result = subprocess.run([str(java), '-cp', str(directory/'*'), 'com.ohinteractive.seedv6.uci.UciMain'],
            cwd=directory, env=env, input='uci\n', capture_output=True, text=True, timeout=10)
        check(result.returncode != 0 and not result.stdout and 'checkpoint-64 NNUE' in result.stderr,
              'Default did not reject embedded network: ' + kind + ': ' + result.stderr)
    return 'Missing, truncated, corrupt and incompatible embedded CP64 fail closed; no BREP2 fallback'


def verify_nnue(launcher, stage, env, java, app):
    """Short native-image gate for the pinned E008 entry point and exact fallback."""
    cwd = stage / 'unrelated NNUE client working directory'; cwd.mkdir(exist_ok=True)
    roots = (
        ('startpos', {f'{f}2{f}{r}' for f in 'abcdefgh' for r in '34'} | {'b1a3', 'b1c3', 'g1f3', 'g1h3'}),
        ('fen 4k3/8/8/8/8/8/4P3/4K3 w - - 0 1', {'e1d1', 'e1f1', 'e1d2', 'e1f2', 'e2e3', 'e2e4'}),
        ('fen 7k/P7/8/8/8/8/8/7K w - - 0 1', {'h1g1', 'h1g2', 'h1h2', 'a7a8q', 'a7a8r', 'a7a8b', 'a7a8n'}))
    transcripts = []; signatures = []
    base = [java, '--limit-modules=java.base', '-XX:ActiveProcessorCount=2', '-Xmx768m',
            '-Djava.awt.headless=true', '-cp', str(app / '*'), NNUE_MAIN]
    for mode, command in (('normal', [launcher]), ('scalar', [launcher, '--scalar']), ('module-absent', base)):
        engine = Engine(command, cwd, env, allowed_stderr=(VECTOR_WARNING,))
        try:
            engine.send('uci'); handshake = engine.until('uciok')
            identity = next((line for line in handshake if line.startswith('info string candidate=')), '')
            for field in ('candidate=nnue-cp64-e008-rc1', 'evaluator=CONVENTIONAL_NNUE', 'checkpoint=64',
                          'schema=S6CNU002', 'heads=8',
                          'networkSha256=8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b',
                          'payload=5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6',
                          'search=E008_QXRAY_LMR_WINDOW_EFFORT_HINTS', 'execution=recursive-pvs',
                          'qsearch=QXRAY lmr=QUIET lmp=WINDOW reuse=EFFORT',
                          'hints=completed_same_key history=invocation learning=all_searched',
                          'aspiration=OFF futility=OFF reverseFutility=OFF nullMove=OFF extensions=OFF', 'workers=1'):
                check(field in identity, 'Wrong native NNUE identity: missing ' + field)
            check('option name Threads type spin default 1 min 1 max 1' in handshake, 'Wrong NNUE worker limit')
            if mode != 'normal':
                check('head=scalar-long' in identity, 'NNUE scalar fallback did not activate')
            engine.send('isready'); engine.until('readyok')
            results = []
            for position, legal in roots:
                engine.send('ucinewgame\nposition ' + position + '\ngo depth 3')
                lines = engine.until('bestmove ')
                check(lines[-1] in {'bestmove ' + move for move in legal}, 'Illegal NNUE bestmove: ' + repr(lines))
                depth = [line for line in lines if line.startswith('info depth 3 ')]
                check(len(depth) == 1, 'NNUE search did not complete depth 3')
                results.append((re.sub(r' (?:time|nps) \d+', '', depth[0]), lines[-1]))
            engine.close(eof=mode == 'module-absent')
            if mode != 'module-absent':
                check(engine.errors == [VECTOR_WARNING], 'NNUE launcher did not resolve Vector module')
            else:
                check(not engine.errors, 'Module-absent fallback emitted stderr')
            signatures.append(results)
            transcripts.append({'mode': mode, 'identity': identity, 'sent': engine.sent,
                                'stdout': engine.lines, 'stderr': engine.errors, 'exitCode': engine.process.returncode})
        finally:
            engine.kill()
    check(signatures[0] == signatures[1] == signatures[2], 'Native NNUE vector/scalar search mismatch')
    return {'status': 'passed', 'searches': len(roots) * 3, 'scalarVectorEqual': True, 'transcripts': transcripts}
