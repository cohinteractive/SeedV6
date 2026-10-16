"""WU2 accepted-source bridge. Read frozen inputs; write only under build/migration.

Uses the immutable UE source probe as an independent verification fixture, never
as an engine implementation. Requires an already built core JAR and JDK 21.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess

UE_REVISION = '58511f45bf1bf0a45ca25f31da402a9e0ba56c50'
MODEL_SHA = '5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7'
JAR_SHA = '2341955302900ae1a15879d0cff6b14a3744498db0dd862b8cd2aabe7482ed0f'
FIXTURES = {
    'sr024-vectors.tsv': '0a10bc7de11506fca7945e236caa8badffab4abbcee975fd1b72583e47dfa3e7',
    'sr024-tactical.tsv': 'bb839d60e62dc2f72d69ad45a421905fdb59f61676e70c937c8861a70320524e',
}


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main(a):
    root = Path(__file__).resolve().parents[1]
    out = Path(a.out).resolve()
    if not out.is_relative_to(root / 'build/migration'):
        raise ValueError('Output must stay inside this worktree/build/migration')
    out.mkdir(parents=True, exist_ok=False)
    model, core = Path(a.model).resolve(), Path(a.core).resolve()
    accepted = Path(a.accepted_install).resolve() / 'lib/app.jar'
    assert digest(model) == MODEL_SHA
    assert digest(accepted) == JAR_SHA
    report = {'status': 'running', 'ueRevision': UE_REVISION,
              'modelSha256': digest(model), 'acceptedJarSha256': digest(accepted),
              'coreJarSha256': digest(core), 'commands': [], 'fixtures': {}}
    env = os.environ.copy()
    env['GIT_OPTIONAL_LOCKS'] = '0'
    for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH'):
        env.pop(key, None)
    try:
        for source in ['tools/Sr024SourceProbe.java', *[
                'src/test/resources/' + name for name in
                ('positions.fen', 'sr005-corpus.tsv', 'sr024-tactical.tsv', 'sr024-vectors.tsv')]]:
            data = subprocess.check_output(['git', '-C', a.ue_repo, 'show', UE_REVISION + ':' + source], env=env)
            target = out / Path(source).name
            target.write_bytes(data)
            report['fixtures'][source] = digest(target)
            if target.name in FIXTURES:
                assert digest(target) == FIXTURES[target.name]
        classes = out / 'classes'
        classes.mkdir()
        jdk = Path(a.jdk)
        suffix = '.exe' if os.name == 'nt' else ''

        def run(command, log):
            report['commands'].append([str(x) for x in command])
            with (out / log).open('w', encoding='utf-8') as stream:
                subprocess.run(command, cwd=root, env=env, stdout=stream,
                               stderr=subprocess.STDOUT, check=True, timeout=600)

        run([jdk/('bin/javac'+suffix), '-cp', core, '-d', classes, out/'Sr024SourceProbe.java'], 'compile.txt')
        for label, classpath in [('accepted', str(accepted.parent/'*')), ('core', str(core))]:
            run([jdk/('bin/java'+suffix), '-Xms256m', '-Xmx512m', '-XX:ActiveProcessorCount=1',
                 '-cp', str(classes)+os.pathsep+classpath, 'Sr024SourceProbe', model,
                 out/'positions.fen', out/'sr005-corpus.tsv', out/'sr024-tactical.tsv',
                 out/(label+'.tsv')], label+'.txt')
        expected = (out/'sr024-vectors.tsv').read_text().splitlines()
        for label in ('accepted', 'core'):
            actual = (out/(label+'.tsv')).read_text().splitlines()
            differences = [i+1 for i, (x, y) in enumerate(zip(expected, actual)) if x != y]
            assert len(actual) == len(expected) and not differences, (label, len(actual), len(expected), differences)
        assert digest(model) == MODEL_SHA and digest(accepted) == JAR_SHA
        report.update(status='passed', vectors=len(expected), exactFields='all 17 TSV fields',
                      acceptedAndCoreMatchFrozenVectors=True, inputsUnchanged=True)
    finally:
        (out/'result.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k: report[k] for k in ('status', 'vectors', 'exactFields')}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('ue-repo', 'accepted-install', 'model', 'core', 'jdk', 'out'):
        parser.add_argument('--'+key, required=True)
    main(parser.parse_args())
