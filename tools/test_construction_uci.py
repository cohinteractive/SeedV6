"""Bounded packaged construction-v1 protocol checks; no network, games or third-party dependencies."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile
from native_package_checks import Engine, check, verify_uci_book

SHA = '5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7'
START = {f'{f}2{f}{r}' for f in 'abcdefgh' for r in '34'} | {'b1a3', 'b1c3', 'g1f3', 'g1h3'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True, help='New evidence directory (must not exist)')
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=False)
    install = out / 'relocated engine with spaces'
    shutil.copytree(args.install.resolve(), install)
    client = out / 'unrelated client directory'; client.mkdir()
    launcher = install / 'bin' / ('seedv6-construction-v1.bat' if os.name == 'nt' else 'seedv6-construction-v1')
    jars = sorted((install / 'lib').glob('*.jar'))
    check({j.name for j in jars} == {'app-uci.jar', 'engine-core.jar'}, 'Unexpected runtime dependency')
    with zipfile.ZipFile(install / 'lib/engine-core.jar') as archive:
        check(hashlib.sha256(archive.read('models/bre-pair2-g32.brn-pair2')).hexdigest() == SHA, 'Wrong packaged model')
    env = os.environ.copy()
    for key in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'JAVA_OPTS', 'SEEDV6_CONSTRUCTION_V1_OPTS'):
        env.pop(key, None)
    env['DEBUG'] = 'true'  # Must not corrupt stdout even in a developer shell.
    report = {'sessions': [], 'jarSha256': {j.name: hashlib.sha256(j.read_bytes()).hexdigest() for j in jars},
              'host': os.name, 'nativeMac': os.name == 'posix' and __import__('sys').platform == 'darwin'}

    def open_engine(extra=()):
        engine = Engine([launcher, *extra], client, env)
        report['sessions'].append({'sent': engine.sent, 'stdout': engine.lines, 'stderr': engine.errors})
        return engine

    def identity(engine, workers, hash_mib, aspiration='OFF'):
        engine.send('uci'); lines = engine.until('uciok')
        text = '\n'.join(lines)
        for expected in ('id name SeedV6 construction-v1 experimental', 'flags=262143', SHA,
                         'futility=100d;reverse=200d;extensionCap=1',
                         f'workers={workers};hashMiB={hash_mib};aspiration={aspiration}'):
            check(expected in text, 'Missing identity: ' + expected)
        for option in ('option name Threads type spin default 1 min 1 max 16',
                       'option name Hash type spin default 64 min 1 max 1024', 'option name Clear Hash type button'):
            check(option in lines, 'Missing UCI option: ' + option)

    def search(engine, command):
        engine.send('position startpos\ngo ' + command)
        lines = engine.until('bestmove ', timeout=20)
        check(lines[-1].split()[1] in START, 'Illegal start-position move')
        check(all(line.startswith('info ') for line in lines[:-1]), 'Non-UCI output')
        return lines

    engine = open_engine()
    try:
        identity(engine, 1, 64)
        engine.send('isready'); engine.until('readyok')
        check(any(line.startswith('info depth 3 ') for line in search(engine, 'depth 3')), 'Default did not search')
        for workers in (1, 4, 16):
            engine.send(f'setoption name Threads value {workers}\nsetoption name Hash value 8\nisready')
            engine.until('readyok'); identity(engine, workers, 8)
            lines = search(engine, 'nodes 1234 depth 20')
            check(any('request nodes=1234 ' in line for line in lines), 'Wrong whole-request node budget')
            search(engine, 'wtime 5000 btime 5000 winc 20 binc 20 movestogo 20')
            engine.send('position startpos\ngo infinite\nisready'); engine.until('readyok')
            engine.send('stop'); check(engine.until('bestmove ')[-1].split()[1] in START, 'Stop failed')
            engine.send('ucinewgame\nsetoption name Clear Hash\nisready'); engine.until('readyok')
            search(engine, 'depth 2')
        engine.send('position startpos\ngo infinite\nucinewgame\nisready')
        check(not any(line.startswith('bestmove ') for line in engine.until('readyok')), 'Stale result after reset')
        search(engine, 'movetime 100')
        verify_uci_book(engine, workers=16)
        engine.send('position startpos\ngo infinite')
        engine.close()
    finally:
        engine.kill()
    engine = open_engine(['--aspiration', 'WIDEN16'])
    try:
        identity(engine, 1, 64, 'WIDEN16'); search(engine, 'depth 3')
        engine.send('position startpos\ngo infinite'); engine.close(eof=True)
    finally:
        engine.kill()
    for extra, expected in ((['--model', str(client / 'missing')], 'missing'),
                            (['--aspiration', 'typo'], 'OFF or WIDEN16')):
        failed = subprocess.run([launcher, *extra], cwd=client, env=env, capture_output=True, text=True, timeout=20)
        check(failed.returncode != 0 and expected in failed.stderr and not failed.stdout, 'Startup failure was not explicit')
    report['status'] = 'passed'
    (out / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    print('PASS: packaged construction-v1 handshake, pinned identity, workers 1/4/16, Hash, clocks, nodes, stop/reset/reuse, quit/EOF and startup rejection')


if __name__ == '__main__':
    main()
