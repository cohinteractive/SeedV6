"""Bounded isolated BRN successor measurements; compile verification classes first."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('mode', choices=['baseline', 'runtime', 'coverage', 'train', 'cache', 'evaluate', 'match'])
    p.add_argument('input', help='Git revision for baseline; dataset otherwise')
    p.add_argument('output', type=Path)
    p.add_argument('--model', type=Path)
    p.add_argument('--overlay', type=Path)
    p.add_argument('--relations', choices=['ABSOLUTE', 'RELATIVE', 'HASH', 'FACTORIZED', 'DIRECTED_RELATIVE'], default='ABSOLUTE')
    p.add_argument('--pool', choices=['SUM', 'SUM_MAX', 'SUM_SQUARE', 'SUM_STATE', 'SUM_CONTEXT', 'SUM_SELF'], default='SUM')
    p.add_argument('--width', type=int, choices=[8, 16, 32], default=8)
    p.add_argument('--positions', type=int, default=16384)
    p.add_argument('--epochs', type=int, default=8)
    p.add_argument('--seed', type=int, default=71)
    p.add_argument('--opponent', type=Path)
    p.add_argument('--pairs', type=int, default=8)
    p.add_argument('--depth', type=int, default=4)
    p.add_argument('--millis', type=int, default=10)
    p.add_argument('--first', type=int, default=0)
    p.add_argument('--opening-seed', type=int, default=714091)
    p.add_argument('--gain', type=float, default=.25)
    p.add_argument('--opponent-gain', type=float, default=.25)
    a = p.parse_args()
    if a.output.exists():
        p.error('Output must be new')
    cp = ['app/build/classes/java/verification', 'app/build/classes/java/main',
          'app/build/resources/main', 'app/build/install/seedv6/lib/*']
    if a.mode == 'baseline':
        name = 'com/ohinteractive/seedv6/core/brn3/Brn3Workspace.java'
        content = subprocess.run(['git', 'show', f'{a.input}:app/src/main/java/{name}'],
                                 check=True, capture_output=True, timeout=30).stdout
        source = a.output / 'src' / name
        source.parent.mkdir(parents=True)
        source.write_bytes(content)
        subprocess.run(['javac', '-cp', os.pathsep.join(cp), '-d', str(a.output / 'classes'), str(source)],
                       check=True, timeout=60)
        (a.output / 'provenance.json').write_text(json.dumps({'revision': a.input,
            'workspaceSourceSha256': hashlib.sha256(content).hexdigest()}, indent=2) + '\n')
        return
    if a.mode in ('runtime', 'cache', 'evaluate') and not a.model:
        p.error('--model required')
    if a.mode == 'match' and not a.opponent:
        p.error('--opponent required')
    if a.overlay:
        cp.insert(0, str(a.overlay))
    a.output.parent.mkdir(parents=True, exist_ok=True)
    cmd = ['java', '-Xms512m', '-Xmx2g', '-Djava.awt.headless=true', '-cp', os.pathsep.join(cp),
           'com.ohinteractive.seedv6.training.service.BrnSuccessorProbe', a.mode, a.input,
           str(a.model or '-'), str(a.output)]
    if a.mode == 'train':
        cmd = cmd[:6] + ['com.ohinteractive.seedv6.training.service.BrnSuccessorTrain',
                        a.input, str(a.output), a.relations, str(a.width), a.pool,
                        str(a.positions), str(a.epochs), str(a.seed)]
    if a.mode == 'cache':
        cmd = cmd[:6] + ['com.ohinteractive.seedv6.training.service.BrnSuccessorCache',
                        a.input, str(a.model), str(a.output)]
    if a.mode == 'evaluate':
        cmd = cmd[:6] + ['com.ohinteractive.seedv6.training.service.BrnSuccessorEvaluate',
                        a.input, str(a.model), str(a.output)]
    if a.mode == 'match':
        cmd = cmd[:6] + ['com.ohinteractive.seedv6.training.service.BrnSuccessorMatch',
                        a.input, str(a.opponent), str(a.output), str(a.pairs), str(a.depth),
                        str(a.millis), str(a.first), str(a.opening_seed), str(a.gain), str(a.opponent_gain)]
    compiled = list(Path('app/build/classes/java/verification/com/ohinteractive/seedv6/training/service').glob('BrnSuccessor*.class'))
    workspace = (a.overlay if a.overlay else Path('app/build/classes/java/main')) / 'com/ohinteractive/seedv6/core/brn3/Brn3Workspace.class'
    compiled.append(workspace)
    execution = {'command': cmd, 'timeoutSeconds': 300,
                 'head': subprocess.run(['git', 'rev-parse', 'HEAD'], check=True, capture_output=True, text=True).stdout.strip(),
                 'compiledSha256': {str(f): hashlib.sha256(f.read_bytes()).hexdigest() for f in compiled}}
    receipt = a.output.with_suffix('.execution.json')
    with receipt.open('x', encoding='utf-8') as f:
        json.dump(execution, f, indent=2)
    started = time.monotonic()
    try:
        with a.output.with_suffix('.stdout.txt').open('xb') as log:
            subprocess.run(cmd, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=300)
        execution['status'] = 'completed'
    except (subprocess.SubprocessError, OSError) as error:
        execution['status'] = 'failed'
        execution['error'] = str(error)
        raise
    finally:
        execution['elapsedSeconds'] = time.monotonic() - started
        receipt.write_text(json.dumps(execution, indent=2) + '\n', encoding='utf-8')
    report = json.loads((a.output / 'result.json' if a.mode == 'train' else a.output / 'match.json' if a.mode == 'match' else a.output).read_text())
    if a.mode == 'match':
        print(json.dumps(report['summary'], indent=2))
        return
    if a.mode == 'evaluate':
        print(json.dumps({k: report[k] for k in ('rawValidation', 'quarterValidation', 'maximumFloatCacheErrorPawns',
                                               'rawIntegerDifferencesVsDouble', 'foldedUnrelatedInferenceNs')}, indent=2))
        return
    if a.mode == 'cache':
        print(json.dumps([{k: v for k, v in r.items() if k != 'roots'} for r in report['trials']], indent=2))
        return
    if a.mode == 'train':
        print(json.dumps({k: report[k] for k in ('arguments', 'trainingParameters', 'selectedEpoch',
                                               'selectedValidation', 'selectedQuarterValidation',
                                               'uncachedDoubleInferenceNs')}, indent=2))
        return
    if a.mode == 'coverage':
        print(json.dumps(report['coverage'], indent=2))
    else:
        print(json.dumps({k: {n: v for n, v in r.items() if n != 'nanoseconds'}
                          for k, r in report['runtime'].items() if k != 'searchDepth4'}, indent=2))


if __name__ == '__main__':
    main()
