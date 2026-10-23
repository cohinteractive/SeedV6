"""Fresh identity/lifecycle smoke of the accepted native image; no packaging changes."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[4]
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / 'tools'))
from native_package_checks import Engine, verify_uci_identity, verify_baseline_identity

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    raw = HERE / 'raw'
    raw.mkdir(exist_ok=True)
    output = raw / 'baseline-native-v1.json'
    summary = HERE / 'baseline-native-v1-summary.json'
    assert not output.exists() and not summary.exists(), 'Evidence already exists'
    image = ROOT / 'releases/candidates/windows-x64/candidate-20261023T195552-c00e3315/SeedV6-UCI'
    launcher = image / 'SeedV6-UCI.exe'
    paths = [launcher, image/'app/engine-core.jar', image/'app/app-uci.jar',
             ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue']
    hashes = {str(p.relative_to(ROOT)): digest(p) for p in paths}
    assert hashes[str(paths[-1].relative_to(ROOT))] == '8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b'
    record = {'startedUtc': datetime.now(timezone.utc).isoformat(), 'files': hashes,
              'head': subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
              'status': 'running', 'searches': []}
    env = os.environ.copy()
    for key in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS'):
        env.pop(key, None)
    env['PATH'] = ''
    engine = Engine([launcher], ROOT, env, ('WARNING: Using incubator modules: jdk.incubator.vector',))
    try:
        engine.send('uci')
        handshake = engine.until('uciok')
        verify_uci_identity(handshake, 1, 64)
        record['identity'] = next(x for x in handshake if x.startswith('info string baseline='))
        assert ' build=74 ' in record['identity']
        legal = {f'{f}2{f}{r}' for f in 'abcdefgh' for r in '34'} | {'b1a3','b1c3','g1f3','g1h3'}
        for workers in (1, 4, 16):
            engine.send(f'setoption name Threads value {workers}\nsetoption name Hash value 64\nsetoption name Clear Hash\nisready')
            lines = engine.until('readyok')
            identity = [x.removeprefix('info string ') for x in lines if x.startswith('info string baseline=')][-1]
            verify_baseline_identity(identity, workers, 64)
            for mode in ('depth 4', 'wtime 1000 btime 1000 winc 100 binc 100', 'infinite'):
                engine.send('ucinewgame\nposition startpos\ngo ' + mode)
                if mode == 'infinite':
                    engine.until('info depth 2 ')
                    engine.send('stop')
                lines = engine.until('bestmove ')
                assert lines[-1].split()[1] in legal, lines
                assert any(x.startswith('info depth ') for x in lines) or mode == 'infinite'
                engine.send('stop\nisready')
                assert engine.until('readyok') == ['readyok'], 'Late/stale publication'
                record['searches'].append({'workers':workers, 'mode':mode, 'lines':lines})
        engine.close()
        record['status'] = 'passed'
    except BaseException as error:
        record.update(status='failed', error=repr(error))
        engine.kill()
        raise
    finally:
        record['transcript'] = engine.lines
        record['commands'] = engine.sent
        record['stderr'] = engine.errors
        with output.open('x',encoding='utf8',newline='\n') as out:
            json.dump(record,out,indent=2);out.write('\n')
        compact = {k:v for k,v in record.items() if k not in ('transcript','commands','stderr','searches')}
        compact.update(searches=len(record['searches']), evidence=str(output.relative_to(ROOT)),
                       evidenceSha256=digest(output), limitation='Headless W1/W4/W16 smoke only; not fresh strength, GUI or macOS verification.')
        with summary.open('x',encoding='utf8',newline='\n') as out:
            json.dump(compact,out,indent=2);out.write('\n')
    print(json.dumps(compact))

if __name__ == '__main__':
    main()
