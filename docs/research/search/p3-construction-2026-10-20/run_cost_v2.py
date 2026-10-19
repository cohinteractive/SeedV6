"""Frozen low-impact construction diagnostics. Run from repository root."""
import argparse, hashlib, json, pathlib, subprocess, time

p = argparse.ArgumentParser()
p.add_argument('name')
p.add_argument('--mode', choices=['depth', 'nodes'], required=True)
p.add_argument('--workers', type=int, default=1)
a = p.parse_args()
base = pathlib.Path(__file__).resolve().parent
raw = base / 'raw'
paths = {x: raw / (a.name + x) for x in ['.jsonl', '.log', '.execution.json']}
if any(x.exists() for x in paths.values()):
    raise SystemExit('Refusing to overwrite retained evidence')
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()
runtime = pathlib.Path('research/build/p3-construction-v1/lib')
if not runtime.exists():
    raise SystemExit('Freeze the validated research runtime first')
observe = a.mode == 'observe'
cmd = ['C:/java/jdk-21/bin/java.exe', '--add-modules=jdk.incubator.vector',
       '-Djava.awt.headless=true', '-Xms256m', '-Xmx768m', '-XX:ActiveProcessorCount=2',
       '-cp', str(runtime / '*'), 'com.ohinteractive.seedv6.search.exact.ConstructionStudy',
       'build-assets/production/bre-pair2-g32.brn-pair2', str(base / 'diagnostic-corpus.tsv'),
       str(paths['.jsonl']), str(a.workers), '4', '4',
       '6' if a.mode == 'depth' else '20',
       '2000000' if a.mode == 'depth' else '100000',
       '12', 'observe' if observe else 'cost']
receipt = {'command': cmd, 'parameters': vars(a),
           'runtime': {str(f): digest(f) for f in sorted(runtime.glob('*.jar'))},
           'protocolSha256': digest(base / 'construction-cost-v2.json'),
           'corpusSha256': digest(base / 'diagnostic-corpus.tsv'),
           'modelSha256': digest(pathlib.Path('build-assets/production/bre-pair2-g32.brn-pair2')),
           'sourceHead': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
           'dirty': subprocess.check_output(['git', 'status', '--porcelain'], text=True),
           'priority': 'BELOW_NORMAL_PRIORITY_CLASS',
           'startedUtc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())}
paths['.execution.json'].write_bytes((json.dumps(receipt, indent=2) + '\n').encode())
started = time.monotonic()
with paths['.log'].open('xb') as log:
    process = subprocess.Popen(cmd, stdout=log, stderr=subprocess.STDOUT,
                              creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS | subprocess.CREATE_NO_WINDOW)
    try:
        code = process.wait(timeout=900)
    except subprocess.TimeoutExpired:
        process.kill()
        code = process.wait()
        receipt['timeout'] = True
receipt.update(exitCode=code, runnerElapsedSeconds=time.monotonic()-started,
               endedUtc=time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()))
receipt['outputHashes'] = {str(f): digest(f) for f in paths.values() if f.exists() and f != paths['.execution.json']}
paths['.execution.json'].write_bytes((json.dumps(receipt, indent=2) + '\n').encode())
print(json.dumps({'name': a.name, 'exitCode': code, 'elapsedSeconds': receipt['runnerElapsedSeconds']}))
raise SystemExit(code)
