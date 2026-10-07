"""Bounded local BRN architecture experiment runner; preserves exact commands and failures."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import subprocess
import time
from brn_process_resource import execute


def digest(path):
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--timeout', type=int, default=300)
    parser.add_argument('--receipt', type=Path, required=True)
    parser.add_argument('mode', choices=['prepare', 'fresh', 'train', 'energy', 'cubic', 'edge', 'context', 'logic', 'tuple', 'tuple-compile', 'tuple-paired', 'qsearch', 'timed', 'subset', 'quality', 'opening-data-audit', 'measure', 'transitions', 'openings', 'match', 'audit', 'native'])
    parser.add_argument('arguments', nargs='+')
    args = parser.parse_args()
    if not 1 <= args.timeout <= 1800:
        parser.error('Timeout must be 1..1800 seconds, justified by the contract gate')
    if args.receipt.exists():
        parser.error('Use a fresh receipt')
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    cp = os.pathsep.join(['app/build/classes/java/verification', 'app/build/classes/java/main',
                         'app/build/resources/main', 'app/build/install/seedv6/lib/*'])
    classes = {'prepare': 'BrnResearchMain', 'energy': 'BrnEnergyExperiment', 'tuple': 'BrnTupleExperiment',
               'tuple-compile': 'BrnTupleCompilation', 'tuple-paired': 'BrnTuplePairedRuntime',
               'qsearch': 'BrnArchitectureQsearch',
               'cubic': 'BrnCubicExperiment',
               'edge': 'BrnSplineExperiment',
               'context': 'BrnContextExperiment',
               'logic': 'BrnLogicExperiment',
               'timed': 'BrnArchitectureTimed', 'subset': 'BrnArchitectureSubset',
               'quality': 'BrnArchitectureQuality', 'opening-data-audit': 'BrnArchitectureOpeningDataAudit',
               'transitions': 'BrnArchitectureTransitions', 'openings': 'BrnArchitectureOpeningAudit',
               'audit': 'BrnArchitectureDataAudit', 'native': 'BrnArchitectureNativeReference',
               'fresh': 'BrnArchitectureFreshData'}
    main_class = 'com.ohinteractive.seedv6.training.service.' + classes.get(args.mode, 'BrnArchitectureControls')
    java_args = ['prepare-uniform'] if args.mode == 'prepare' else [] if args.mode in classes else [args.mode]
    command = ['java', '--add-modules=jdk.incubator.vector', '-Xms256m', '-Xmx2g',
               '-Djava.awt.headless=true', '-cp', cp, main_class] + java_args + args.arguments
    if args.mode in ['measure', 'transitions', 'tuple-paired']:
        command.insert(1, '-Xbatch')
    git = ['git', '-c', 'safe.directory=' + Path.cwd().as_posix()]
    sources = [p for root in ['app/src/main/java', 'app/src/verification/java']
               for p in Path(root).rglob('*.java')]
    compiled = [p for root in ['app/build/classes/java/main', 'app/build/classes/java/verification']
                for p in Path(root).rglob('*.class')]
    receipt = {'schema': 'brn-architecture-execution-v1', 'command': command,
               'head': subprocess.check_output(git + ['rev-parse', 'HEAD'], text=True).strip(),
               'gitStatus': subprocess.check_output(git + ['status', '--porcelain=v1'], text=True),
               'sourceSha256': {p.as_posix(): digest(p) for p in sources},
               'compiledSha256': {p.as_posix(): digest(p) for p in compiled},
               'runnerSha256': {p.as_posix(): digest(p) for p in
                                [Path(__file__), Path(__file__).with_name('brn_process_resource.py')]},
               'java': subprocess.run(['java', '-version'], capture_output=True, text=True).stderr,
               'platform': platform.platform(), 'timeoutSeconds': args.timeout,
               'startedUtc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
               'status': 'running'}
    args.receipt.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    started = time.monotonic()
    code = 1
    try:
        with args.receipt.with_suffix('.stdout.txt').open('xb') as output:
            receipt.update(execute(command, output, args.timeout))
        code = receipt['exitCode'] if receipt['status'] != 'timeout' else 1
        if receipt['status'] == 'timeout':
            receipt['note'] = 'Only this owned Java child was terminated; retained partial files are not completed results.'
    except BaseException as failure:
        receipt['status'] = 'failed'
        receipt['error'] = type(failure).__name__ + ': ' + str(failure)
        raise
    finally:
        receipt['elapsedSeconds'] = time.monotonic() - started
        receipt['endedUtc'] = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())
        args.receipt.write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
        print(json.dumps({k: receipt[k] for k in ['status', 'elapsedSeconds']}, indent=2))
    raise SystemExit(code)


if __name__ == '__main__':
    main()
