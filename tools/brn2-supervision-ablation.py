"""Run the offline replay and established diagnostics with hard subprocess bounds.

Build :app:classes first. All three paths are explicit. Existing outputs are rejected.
Snapshots go outside the source stores; measurements follow the app/build convention.
No GUI, store writer, recovery, self-play or production reference update is invoked.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import time


def fingerprint(root):
    result = {}
    for path in sorted(root.rglob('*')):
        if path.is_file():
            stat = path.stat()
            with path.open('rb') as stream:
                digest = hashlib.file_digest(stream, 'sha256').hexdigest()
            result[path.relative_to(root).as_posix()] = dict(bytes=stat.st_size, mtimeNs=stat.st_mtime_ns, sha256=digest)
    return result


def records(path):
    return [json.loads(line) for line in path.read_text(encoding='utf-8').splitlines()]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('teacher', type=Path)
    parser.add_argument('experiment', type=Path)
    parser.add_argument('measurements', type=Path)
    args = parser.parse_args()
    source, teacher, experiment, measurements = (p.resolve() for p in
                                                 (args.source, args.teacher, args.experiment, args.measurements))
    if experiment.exists() or measurements.exists():
        raise ValueError('Experiment and measurements must both be new paths')
    for output in (experiment, measurements):
        for protected in (source, teacher.parent.parent):
            if output.is_relative_to(protected) or protected.is_relative_to(output):
                raise ValueError('Output overlaps protected source store')
    measurements.mkdir(parents=True)
    inputs = {'source': source, 'teacher': teacher.parent.parent}
    print('Hashing complete protected stores before execution', flush=True)
    before = {name: fingerprint(path) for name, path in inputs.items()}
    (measurements / 'inputs-before.json').write_text(json.dumps(before, indent=2), encoding='utf-8')
    receipts = []
    classpath = os.pathsep.join(['app/build/classes/java/main', 'app/build/resources/main', 'app/build/install/seedv6/lib/*'])
    java = ['java', '-Xms256m', '-Xmx1536m', '-cp', classpath]

    def run(name, arguments, timeout):
        start = time.monotonic()
        receipt = dict(name=name, command=arguments, watchdogSeconds=timeout)
        try:
            with (measurements / (name + '.log')).open('w', encoding='utf-8') as log:
                process = subprocess.run(arguments, stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
            receipt.update(exit=process.returncode, watchdog=False)
        except subprocess.TimeoutExpired:
            receipt.update(exit=None, watchdog=True)
        receipt['elapsedSeconds'] = time.monotonic() - start
        receipts.append(receipt)
        (measurements / 'processes.json').write_text(json.dumps(receipts, indent=2), encoding='utf-8')
        print({k: v for k, v in receipt.items() if k != 'command'}, flush=True)
        if receipt['exit'] != 0:
            raise RuntimeError(f'{name} failed or reached watchdog; inspect preserved log/evidence')

    try:
        run('replay', java + ['com.ohinteractive.seedv6.tools.search.Brn2SupervisionAblation',
                             str(source), str(teacher), str(experiment)], 1200)
        replay = records(experiment / 'replay.jsonl')
        assert replay[-1] == {'type': 'end', 'completed': True}
        assert any(r['type'] == 'gate' and r['passed'] and r['tolerance'] == 0 for r in replay)
        diagnostic = java + ['com.ohinteractive.seedv6.tools.search.Brn2Diagnostics', f'--nnue={teacher}']
        for arm in ('wdl', 'blended', 'teacher'):
            for boundary in (0, 32, 64, 96, 128):
                name = f'static-{arm}-{boundary}'
                model = experiment / arm / f'boundary-{boundary}' / 'network.brn2'
                run(name, diagnostic + [f'--brn2={model}', '--depth=0', '--symmetry=true',
                                       f'--output={measurements / (name + ".jsonl")}', f'--label={name}'], 40)
        corpus = [r['id'] for r in records(measurements / 'static-wdl-128.jsonl') if r['type'] == 'position']
        assert len(corpus) == 15
        for arm in ('blended', 'teacher'):
            model = experiment / arm / 'boundary-128' / 'network.brn2'
            base = diagnostic + [f'--brn2={model}', '--drivers=brn2', '--depth=4', '--nodes=1000000']
            for position in corpus:
                name = f'search-{arm}-{position}'
                run(name, base + [f'--positions={position}', '--time-ms=10000', '--warmup=1', '--repetitions=2',
                                 '--qshadow=false', f'--output={measurements / (name + ".jsonl")}', f'--label={name}'], 40)
            name = f'qshadow-{arm}-kiwipete'
            # Same bounded instrumentation allowance as the canonical report, separate from normal search timings.
            run(name, base + ['--positions=middlegame-kiwipete', '--time-ms=30000', '--warmup=0', '--repetitions=1',
                             '--qshadow=true', '--symmetry=true', '--symmetry-stride=101', '--symmetry-limit=8192',
                             f'--output={measurements / (name + ".jsonl")}', f'--label={name}'], 45)
    finally:
        print('Rechecking complete protected stores', flush=True)
        after = {name: fingerprint(path) for name, path in inputs.items()}
        (measurements / 'inputs-after.json').write_text(json.dumps(after, indent=2), encoding='utf-8')
        if before != after:
            raise RuntimeError('Protected source inventory/content/mtime changed; stop and investigate')
        (measurements / 'input-integrity.json').write_text(json.dumps(dict(unchanged=True,
             files={name: len(files) for name, files in before.items()}), indent=2), encoding='utf-8')


if __name__ == '__main__':
    main()
