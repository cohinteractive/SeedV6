"""Headless WU5 acceptance using immutable UE protocol fixtures and independent legal-PV checks.

Requires a built Java distribution, the explicit pinned local model, python-chess,
and read-only access to UE Git objects. Writes only to a new build/migration folder.
Native packages, real Arena/BotLi clients and live games are deliberately outside this test.
"""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import queue
import shutil
import subprocess
import time

import chess

REV = '58511f45bf1bf0a45ca25f31da402a9e0ba56c50'
SHA = '5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7'
FIXTURE_SHA = '0a10bc7de11506fca7945e236caa8badffab4abbcee975fd1b72583e47dfa3e7'


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main(a):
    root = Path(__file__).resolve().parents[1]
    out = Path(a.out).resolve()
    if not out.is_relative_to(root / 'build/migration'):
        raise ValueError('Evidence must stay under this worktree/build/migration')
    out.mkdir(parents=True, exist_ok=False)
    model = Path(a.model).resolve()
    assert digest(model) == SHA
    env = os.environ.copy()
    env['GIT_OPTIONAL_LOCKS'] = '0'
    for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH'):
        env.pop(key, None)
    fixture = out / 'historical'
    report = {'status': 'running', 'arguments': vars(a), 'ueRevision': REV, 'modelSha256': SHA,
              'source': {}, 'sessions': [], 'checks': []}
    for name in ['tools/test_uci.py', 'src/test/resources/sr024-vectors.tsv']:
        target = fixture / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(subprocess.check_output(['git', '-C', a.ue_repo, 'show', REV + ':' + name], env=env))
        report['source'][name] = digest(target)
    assert digest(fixture/'src/test/resources/sr024-vectors.tsv') == FIXTURE_SHA
    source = (fixture/'tools/test_uci.py').read_text()
    # Only product identity and diagnostic prefix change. All historical assertions remain.
    adapted = source.replace('id name SeedV6-UE v{version}', 'id name SeedV6 UCI 0.0.0 (build 35)')
    adapted = adapted.replace('id name SeedV6-UE', 'id name SeedV6 UCI')
    adapted = adapted.replace('SeedV6-UE rejected command:', 'SeedV6 UCI rejected command:')
    path = fixture/'tools/adapted_test_uci.py'; path.write_text(adapted)
    spec = importlib.util.spec_from_file_location('historical_uci', path)
    ue = importlib.util.module_from_spec(spec); spec.loader.exec_module(ue)
    original_engine = ue.Engine

    class RecordedEngine(original_engine):
        def __init__(self, command, cwd, env=None):
            super().__init__(command, cwd, env)
            self.record = {'command': list(map(str, command)), 'cwd': str(cwd), 'sent': [],
                           'stdout': self.observed, 'stderr': self.errors}
            report['sessions'].append(self.record)

        def send(self, command):
            self.record['sent'].append(command)
            super().send(command)

        def finish(self, eof=False):
            super().finish(eof)
            self.record['exitCode'] = self.process.returncode

    ue.Engine = RecordedEngine
    cwd = out/'unrelated client directory'; cwd.mkdir()
    install = Path(a.install).resolve()
    jars = sorted((install/'lib').glob('*.jar'))
    assert {p.name for p in jars} == {'app-uci.jar', 'engine-core.jar'}, jars
    base = [a.java, '-Xmx192m', '-Djava.awt.headless=true', '--limit-modules', 'java.base',
            '-cp', str(install/'lib/*'), 'com.ohinteractive.seedv6.uci.UciMain']
    command = base + ['--model', str(model)]
    try:
        ue.exchange(command, cwd, env, release_version='6')
        report['checks'].append('Unweakened historical SR-024 UCI exchange: vectors, clocks, reset, stop, EOF, invalid input')
        engine = RecordedEngine(command, cwd, env)
        try:
            engine.send('uci'); handshake = engine.until('uciok')
            assert any('sha256='+SHA in line and 'mode=production' in line for line in handshake)
            assert not any('OwnBook' in line or 'Ponder' in line for line in handshake)
            # Small requests across every supported worker count; legal move and every PV verified independently.
            for workers in range(1, 17):
                engine.send(f'setoption name Threads value {workers}\nsetoption name Hash value 8')
                engine.ready()
                engine.send('uci'); identity = '\n'.join(engine.until('uciok'))
                assert 'workers='+str(workers)+' ' in identity
                assert 'see=qxray' in identity and 'lmr=quiet' in identity and 'lmp=window' in identity
                assert 'concurrency='+('off' if workers == 1 else 'root_reuse') in identity
                engine.send('ucinewgame\nposition startpos\ngo nodes 10000 depth 16')
                lines = engine.until('bestmove', 8)
                board = chess.Board()
                assert chess.Move.from_uci(lines[-1].split()[1]) in board.legal_moves
                assert any('request nodes=10000 ' in line for line in lines), lines
                for line in lines:
                    if ' pv ' in line:
                        replay = board.copy()
                        for token in line.split(' pv ')[1].split():
                            move = chess.Move.from_uci(token)
                            assert move in replay.legal_moves, line
                            replay.push(move)
            report['checks'].append('All workers 1..16: accepted identity, exact request budget, independently legal bestmove/PV')
            engine.send('setoption name Threads value 1\nsetoption name Hash value 64')
            engine.ready()
            # Deterministic resource failure under a small heap; retain the working service after OOM.
            engine.send('setoption name Hash value 1024'); engine.ready()
            engine.send('uci'); lines = engine.until('uciok')
            assert any('ttMiB=64' in line for line in lines), lines
            engine.send('position startpos\ngo depth 2'); engine.until('bestmove')
            deadline = time.monotonic()+2
            while not engine.errors and time.monotonic()<deadline: time.sleep(.01)
            assert any('could not allocate Hash/Threads; retained previous settings' in x for x in engine.errors)
            report['checks'].append('Hash=1024 allocation failure retains Hash=64 and successful search under -Xmx192m')
            for hash_mib in (1, 3, 64):
                engine.send(f'setoption name Hash value {hash_mib}\nsetoption name Clear Hash\nposition startpos\ngo depth 2')
                lines = engine.until('bestmove')
                assert chess.Move.from_uci(lines[-1].split()[1]) in chess.Board().legal_moves
            report['checks'].append('Hash minimum, non-power-of-two request and restored default with Clear Hash')
            # Zero budget must publish a legal move without fabricated completed-depth/score/PV.
            for go in ('go nodes 0', 'go movetime 0'):
                engine.send('ucinewgame\nposition startpos\n'+go)
                lines = engine.until('bestmove')
                assert not any(x.startswith('info depth') for x in lines), lines
                assert chess.Move.from_uci(lines[-1].split()[1]) in chess.Board().legal_moves
            # Strict FEN validation must retain the last accepted position atomically.
            fen = '4k3/8/8/8/8/8/4P3/4K3 w - - 0 1'
            for invalid in ('8/8/8/8/8/8/8/8 w - - 0 1', '4k3/8/8/8/8/8/8/P3K3 w - - 0 1',
                            '4k3/8/8/8/8/8/4Q3/4K3 w - - 0 1', '4k3/8/8/8/8/8/8/4K3 w K - 0 1',
                            '4k3/8/8/4P3/8/8/8/4K3 w - d6 0 1'):
                engine.send('position fen '+fen+'\nposition fen '+invalid+'\ngo depth 1')
                lines = engine.until('bestmove')
                assert chess.Move.from_uci(lines[-1].split()[1]) in chess.Board(fen).legal_moves
            # Truthful mate-score domain in both directions; no translated centipawn mate.
            for fen, depth, score in [('7k/8/5KQ1/8/8/8/8/8 w - - 0 1', 1, 'mate 1'),
                                     ('7k/p7/5KQ1/8/8/8/8/8 b - - 0 1', 2, 'mate -1')]:
                engine.send('ucinewgame\nposition fen '+fen+f'\ngo depth {depth}')
                lines = engine.until('bestmove')
                assert any(f'info depth {depth} score {score} ' in x for x in lines), lines
                game = chess.Board(fen)
                move = chess.Move.from_uci(lines[-1].split()[1]); assert move in game.legal_moves
                game.push(move)
                if score == 'mate 1':
                    assert game.is_checkmate()
                    engine.send('position fen '+game.fen()+'\ngo depth 1')
                    assert engine.until('bestmove')[-1] == 'bestmove 0000'
            report['checks'].append('Zero-budget honesty, strict FEN atomicity, winning/losing mate distances')
            engine.finish()
        finally: engine.kill()
        # A local copy is validation input only; neither source nor model is published.
        relocated = out/'relocated Java application'; shutil.copytree(install, relocated)
        (relocated/'models').mkdir(); shutil.copyfile(model, relocated/'models/baseline.brn-pair2')
        launch_env = env.copy(); launch_env['DEBUG'] = '1'; launch_env['JAVA_HOME'] = str(Path(a.java).resolve().parent.parent)
        launcher = relocated/'bin'/('seedv6-uci.bat' if os.name == 'nt' else 'seedv6-uci')
        engine = RecordedEngine([str(launcher)], cwd, launch_env)
        try:
            engine.send('uci'); engine.until('uciok'); engine.ready()
            engine.send('position startpos\ngo depth 2'); engine.until('bestmove'); engine.finish()
            assert not engine.errors
        finally: engine.kill()
        report['checks'].append('Relocated standard Java launcher, spaces, unrelated cwd, DEBUG=1, adjacent verified model')
        # Fail closed before any protocol stdout for missing/corrupt/incompatible models and unsupported selection.
        bad = out/'corrupt.brn-pair2'; data=bytearray(model.read_bytes()); data[120]^=1; bad.write_bytes(data)
        short = out/'truncated.brn-pair2'; short.write_bytes(data[:-1])
        for selected in (out/'missing', bad, short):
            result = subprocess.run(base+['--model',str(selected)], cwd=cwd,env=env,input='uci\n',capture_output=True,text=True,timeout=5)
            assert result.returncode != 0 and result.stdout == '' and 'startup/runtime error:' in result.stderr, result
            report['checks'].append({'modelFailure':selected.name,'exitCode':result.returncode,'stderr':result.stderr.strip()})
        result = subprocess.run(base,cwd=cwd,env=env,input='uci\n',capture_output=True,text=True,timeout=5)
        assert result.returncode != 0 and not result.stdout and 'Required BRE-Pair 2 model missing' in result.stderr
        report['checks'].append('Missing default model fails without HCE fallback')
        result = subprocess.run([base[0], '-Dseedv6.uci.futilityResearch=off', *base[1:], '--model', str(model)],
                                cwd=cwd, env=env, input='uci\n', capture_output=True, text=True, timeout=5)
        assert result.returncode != 0 and not result.stdout and 'Legacy/research property' in result.stderr
        report['checks'].append('Standalone rejects legacy/research selection properties before startup')
        assert digest(model)==SHA
        report.update(status='passed', searches=sum(sum(x.startswith('bestmove ') for x in s['stdout']) for s in report['sessions']),
                      runtimeJars={p.name:digest(p) for p in jars}, modelUnchanged=True)
    finally:
        (out/'result.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ('status','searches','modelUnchanged')}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('java','install','model','ue-repo','out'): p.add_argument('--'+key,required=True)
    main(p.parse_args())
