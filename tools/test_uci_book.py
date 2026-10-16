"""Exercise the real standalone UCI profile and unavailable packaged-book fallback.

Uses the existing verified local production model; never modifies the installation.
Native packaging runs the same positive protocol suite through its actual launcher.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import zipfile

from native_package_checks import Engine, check, verify_uci_book


RESOURCE = 'com/ohinteractive/seedv6/book/opening-book.txt'


def main(args):
    root = Path(__file__).resolve().parents[1]
    output = Path(args.out).resolve()
    check(output.is_relative_to(root / 'build'), 'Evidence must stay inside this repository/build')
    output.mkdir(parents=True, exist_ok=False)
    cwd = output / 'unrelated client directory'; cwd.mkdir()
    install = Path(args.install).resolve()
    model = Path(args.model).resolve()
    env = os.environ.copy()
    for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH'):
        env.pop(key, None)
    env['TEMP'] = env['TMP'] = str(cwd)
    report = {'status': 'running', 'sessions': [], 'modelSha256': hashlib.sha256(model.read_bytes()).hexdigest()}

    def command(lib):
        return [str(Path(args.java).resolve()), '-Xmx192m', '-Djava.awt.headless=true',
                '--limit-modules', 'java.base', '-cp', str(lib / '*'),
                'com.ohinteractive.seedv6.uci.UciMain', '--model', str(model)]

    try:
        engine = Engine(command(install / 'lib'), cwd, env)
        report['sessions'].append({'kind': 'bundled', 'sent': engine.sent,
                                   'stdout': engine.lines, 'stderr': engine.errors})
        try:
            engine.send('uci\nisready')
            handshake = engine.until('uciok')
            for option in ('UseBook', 'OwnBook'):
                check(handshake.count(f'option name {option} type check default false') == 1,
                      'Missing/duplicate option: ' + option)
            check(engine.until('readyok') == ['readyok'], 'Invalid readiness output')
            count = verify_uci_book(engine)
            engine.close()
            check(sum(line.startswith('bestmove ') for line in engine.lines) == count, 'Duplicate/missing bestmove')
            report['sessions'][-1]['searches'] = count
        finally:
            engine.kill()

        # Fresh, isolated class loaders/processes cover both lazy-loader failure modes.
        # Preserve every other JAR entry and the original resource bytes in the source installation.
        for kind, replacement in (('missing', None), ('invalid', b'invalid opening repertoire\n')):
            lib = output / kind; lib.mkdir()
            shutil.copyfile(install / 'lib/app-uci.jar', lib / 'app-uci.jar')
            with zipfile.ZipFile(install / 'lib/engine-core.jar') as source, \
                    zipfile.ZipFile(lib / 'engine-core.jar', 'x') as target:
                check(RESOURCE in source.namelist(), 'Normal distribution is missing its book')
                for entry in source.infolist():
                    if entry.filename != RESOURCE:
                        target.writestr(entry, source.read(entry))
                    elif replacement is not None:
                        target.writestr(entry, replacement)
            engine = Engine(command(lib), cwd, env)
            report['sessions'].append({'kind': kind, 'sent': engine.sent,
                                       'stdout': engine.lines, 'stderr': engine.errors})
            try:
                engine.send('uci'); engine.until('uciok')
                for option in ('UseBook', 'OwnBook'):
                    engine.send(f'setoption name {option} value true\nsetoption name Hash value 8')
                    engine.send('position startpos\ngo depth 2')
                    lines = engine.until('bestmove ')
                    check(any(line.startswith('info depth 2 ') for line in lines), 'Unavailable book did not search')
                    check('info string opening book' not in lines, 'Unavailable book claimed a hit')
                    legal = {f'{f}2{f}{r}' for f in 'abcdefgh' for r in '34'} | {'b1a3', 'b1c3', 'g1f3', 'g1h3'}
                    check(lines[-1] in {'bestmove ' + move for move in legal}, 'Illegal fallback move')
                    engine.send('stop\nisready')
                    check(engine.until('readyok') == ['readyok'], 'Duplicate unavailable-book bestmove')
                engine.send('quit'); engine.process.wait(timeout=5)
                for reader in engine.readers:
                    reader.join(timeout=2)
                    check(not reader.is_alive(), 'Unavailable-book process retained streams')
                check(engine.process.returncode == 0, 'Unavailable book crashed the engine')
                check(len(engine.errors) == 1 and engine.errors[0].startswith(
                    'SeedV6 UCI opening book unavailable; using search:'), 'Missing/duplicate/unexpected diagnostic')
                check(sum(line.startswith('bestmove ') for line in engine.lines) == 2, 'Duplicate/missing bestmove')
                report['sessions'][-1]['searches'] = 2
            finally:
                engine.kill()
        report['status'] = 'passed'
        print('PASS: bundled-book protocol and missing/invalid resource fallback; '
              + str(sum(session['searches'] for session in report['sessions'])) + ' requests')
    finally:
        (output / 'results.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java', required=True)
    parser.add_argument('--install', required=True)
    parser.add_argument('--model', required=True)
    parser.add_argument('--out', required=True)
    main(parser.parse_args())
