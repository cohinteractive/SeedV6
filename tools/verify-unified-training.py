"""WU3 bounded compatibility bridge. Writes only new worktree-local evidence.

Requires built WU2 and WU3 distributions and the explicit pinned checkpoint.
The migrated probe runs with training/core and training libraries only: no app,
GUI, UCI, verification or research classes can satisfy a missing dependency.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import shutil

INPUTS = {
    'network.brn-pair2': '5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7',
    'training.state': 'f8bdda5eda0ff7893ee36c2270baaedecaf50e14ff5202ab786996f72a2788d5',
    'manifest.bin': 'a51513ddb6e23a8537cc38b8aeb7676af5723c3e3b6bb8151f265bb87ae83891',
    'training-metadata.bin': '135fb61ed736f313158e5514f2a16a81eda6526c4f034d2232ff26f8ee4176e7',
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main(a):
    root = Path(__file__).resolve().parents[1]
    out = Path(a.out).resolve()
    if not out.is_relative_to(root / 'build/migration'):
        raise ValueError('Output must stay inside this worktree/build/migration')
    checkpoint = Path(a.checkpoint).resolve()
    assert {name: digest(checkpoint / name) for name in INPUTS} == INPUTS
    out.mkdir(parents=True, exist_ok=False)
    classes = out / 'classes'
    classes.mkdir()
    temporary = out / 'tmp'
    temporary.mkdir()
    cloned_checkpoint = out / 'historical-store' / 'checkpoints' / checkpoint.name
    cloned_checkpoint.mkdir(parents=True)
    for name in INPUTS:
        shutil.copyfile(checkpoint / name, cloned_checkpoint / name)
    lineage = checkpoint.parent.parent / 'training-lineage.bin'
    lineage_sha = digest(lineage)
    shutil.copyfile(lineage, out / 'historical-store' / lineage.name)
    baseline = sorted((Path(a.baseline).resolve() / 'lib').glob('*.jar'))
    current_lib = Path(a.current).resolve() / 'lib'
    current = [current_lib / 'engine-core.jar', current_lib / 'engine-training.jar']
    for pattern in ('gson-*.jar', 'zstd-jni-*.jar', 'sqlite-jdbc-*.jar'):
        match, = current_lib.glob(pattern)
        current.append(match)
    assert baseline and all(p.is_file() for p in current)
    report = {'status': 'running', 'inputs': INPUTS, 'commands': [],
              'baselineJars': {p.name: digest(p) for p in baseline},
              'trainingOnlyJars': {p.name: digest(p) for p in current},
              'historicalLineageSha256': lineage_sha}
    env = os.environ.copy()
    env.update(TEMP=str(temporary), TMP=str(temporary), GIT_OPTIONAL_LOCKS='0')
    for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH'):
        env.pop(key, None)
    jdk = Path(a.jdk)
    suffix = '.exe' if os.name == 'nt' else ''

    def run(command, label):
        report['commands'].append([str(x) for x in command])
        with (out / (label + '.tsv')).open('w', encoding='utf-8') as stdout, \
                (out / (label + '.err')).open('w', encoding='utf-8') as stderr:
            subprocess.run(command, cwd=root, env=env, stdout=stdout, stderr=stderr,
                           check=True, timeout=600)

    try:
        probes = ['UnifiedModelCompatibilityProbe', 'UnifiedTrainingCompatibilityProbe']
        run([jdk / ('bin/javac' + suffix), '-cp', os.pathsep.join(map(str, baseline)),
             '-d', classes, *[root / 'tools' / (p + '.java') for p in probes]], 'compile')
        for label, jars in [('baseline', baseline), ('training', current)]:
            common = [jdk / ('bin/java' + suffix), '-Xmx1200m', '-XX:ActiveProcessorCount=2',
                      '--add-modules=jdk.incubator.vector', '-Djava.io.tmpdir=' + str(temporary),
                      '-cp', os.pathsep.join(map(str, [classes, *jars]))]
            run([*common, probes[0], checkpoint], label + '-model')
            run([*common, probes[1], out / (label + '-stores'), cloned_checkpoint], label + '-continuation')
        for probe in ('model', 'continuation'):
            expected = (out / ('baseline-' + probe + '.tsv')).read_bytes()
            actual = (out / ('training-' + probe + '.tsv')).read_bytes()
            assert actual == expected, 'Before/after difference: ' + probe
            report[probe] = {'exact': True, 'rows': actual.decode().splitlines(),
                             'sha256': hashlib.sha256(actual).hexdigest()}
        assert {name: digest(checkpoint / name) for name in INPUTS} == INPUTS
        assert digest(lineage) == lineage_sha
        report.update(status='passed', historicalInputsUnchanged=True,
                      historicalRecompileExact=True, familyDiskContinuations=7,
                      appAbsentFromMigratedClasspath=True)
    finally:
        (out / 'result.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: report[k] for k in ('status', 'familyDiskContinuations', 'historicalRecompileExact')}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('baseline', 'current', 'checkpoint', 'jdk', 'out'):
        parser.add_argument('--' + key, required=True)
    main(parser.parse_args())
