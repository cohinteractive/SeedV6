"""Headless checks against actual bundled launchers; Python standard library only."""
import os
from pathlib import Path
import queue
import subprocess
import threading
import time
import zipfile


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
        check(len(modules) == 1 and modules[0].startswith('java.base@'), 'UCI runtime must contain java.base only')
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
        launcher = app.parent / ('SeedV6-' + application.upper() + '.exe')
        data = launcher.read_bytes()
        pe = int.from_bytes(data[0x3c:0x40], 'little')
        check(data[pe:pe + 4] == b'PE\0\0', 'Missing PE launcher')
        check(int.from_bytes(data[pe + 4:pe + 6], 'little') == 0x8664, 'Launcher is not x64')
        subsystem = int.from_bytes(data[pe + 24 + 68:pe + 24 + 70], 'little')
        check(subsystem == (2 if application == 'gui' else 3), 'Wrong Windows launcher subsystem')


class Engine:
    def __init__(self, command, cwd, env):
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
        check(not self.errors, 'Unexpected UCI stderr: ' + repr(self.errors))

    def kill(self):
        # Closing stdin also drains a restarted JDK launcher child on failing tests.
        if not self.process.stdin.closed:
            self.process.stdin.close()
        if self.process.poll() is None:
            self.process.kill(); self.process.wait(timeout=5)


def verify_uci(launcher, stage, env, model):
    cwd = stage / 'unrelated client working directory'; cwd.mkdir(exist_ok=True)
    legal_start = {f'{file}2{file}{rank}' for file in 'abcdefgh' for rank in '34'} | {'b1a3', 'b1c3', 'g1f3', 'g1h3'}
    checks = []; transcripts = []; searches = 0
    for workers in (1, 4, 16):
        engine = Engine([launcher], cwd, env)
        try:
            engine.send('uci'); handshake = engine.until('uciok')
            check(any(line.startswith('id name SeedV6 UCI ') for line in handshake), 'Wrong UCI identity')
            for option in ('Threads', 'Hash', 'Clear Hash'):
                check(any('option name ' + option + ' ' in line for line in handshake), 'Missing option ' + option)
            engine.send(f'setoption name Threads value {workers}\nsetoption name Hash value 8\nsetoption name Clear Hash\nisready')
            engine.until('readyok')
            engine.send('uci'); identity = '\n'.join(engine.until('uciok'))
            check(f'workers={workers} ' in identity and 'futility=off' in identity and 'see=qxray' in identity,
                  'Wrong accepted search identity')
            check('concurrency=' + ('off' if workers == 1 else 'root_reuse') in identity, 'Worker policy differs')
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
            engine.close()
            checks.append(f'Threads={workers}: handshake/ready/hash/clear/depth/timed/clocks/stop/position/quit')
            transcripts.append({'workers': workers, 'sent': engine.sent, 'stdout': engine.lines, 'stderr': engine.errors})
        finally:
            engine.kill()
    for termination in ('EOF', 'terminate'):
        engine = Engine([launcher], cwd, env)
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
            'externalJava': 'PATH empty; JAVA_HOME/JDK_HOME removed', 'guiTraining': 'absent; java.base-only runtime'}
