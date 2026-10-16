"""Unified local candidates: Gradle inputs, JDK native tools, fail-closed verification.

Reuses the GUI jpackage/Info.plist/icon workflow and immutable UE SR-024
87b8b5b50acf3d2fe19793bfb0e8b24a45be114b console/jlink/RENAME_EXCL techniques.
No release counter, publication, download, source-engine copy or model registry.
"""
import argparse
import ctypes
import datetime
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import stat
import subprocess
import sys
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[1]
APPS = {'gui': ('SeedV6-GUI', 'com.ohinteractive.seedv6.gui.SwingLauncher'),
        'uci': ('SeedV6-UCI', 'com.ohinteractive.seedv6.uci.UciMain')}


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def encode(value):
    return (json.dumps(value, indent=2, sort_keys=True) + '\n').encode()


def write_json(path, value):
    Path(path).write_bytes(encode(value))


def properties(text):
    return dict(line.split('=', 1) for line in text.splitlines() if '=' in line and not line.startswith('#'))


def environment(empty_path=False):
    env = os.environ.copy()
    for key in ('JAVA_HOME', 'JDK_HOME', 'CLASSPATH', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS'):
        env.pop(key, None)
    env['GIT_OPTIONAL_LOCKS'] = '0'
    if empty_path:
        env['PATH'] = ''
    return env


def run(command, cwd=ROOT, env=None, timeout=300):
    result = subprocess.run(list(map(str, command)), cwd=cwd, env=env or environment(),
                            capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=timeout)
    require(result.returncode == 0, f'Command failed ({result.returncode}): {command}\n{result.stdout}\n{result.stderr}')
    return result.stdout.strip()


def source_state():
    names = subprocess.check_output(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'],
                                    cwd=ROOT, env=environment()).decode('utf-8').split('\0')
    return {'revision': run(['git', 'rev-parse', 'HEAD']),
            'status': run(['git', 'status', '--porcelain=v1', '--untracked-files=all']).splitlines(),
            'files': {n: sha(ROOT / n) for n in sorted(set(names) - {''}) if (ROOT / n).is_file()}}


def contained(path, parent):
    path = Path(path).resolve()
    require(path.is_relative_to(parent.resolve()) and path != parent.resolve(), f'Unsafe staging path: {path}')
    return path


def host_check(target, jdk):
    actual = 'windows' if sys.platform == 'win32' else 'macos' if sys.platform == 'darwin' else sys.platform
    require(actual == target, f'Native {target} packaging requires a {target} host; current host is {actual}.')
    arch = platform.machine().lower()
    if target == 'macos' and arch == 'x86_64':
        translated = subprocess.run(['/usr/sbin/sysctl', '-in', 'sysctl.proc_translated'], capture_output=True, text=True)
        if translated.stdout.strip() == '1':
            arch = 'arm64'
    expected = 'x64' if target == 'windows' else 'arm64'
    aliases = {'amd64': 'x64', 'x86_64': 'x64', 'aarch64': 'arm64', 'arm64': 'arm64'}
    require(aliases.get(arch) == expected, f'Only native {target} {expected} packaging is supported; got {arch}.')
    identity = properties((jdk / 'release').read_text())
    require(re.match(r'"21(?:\.|"|\+)', identity.get('JAVA_VERSION', '')), 'Packaging requires Java 21.')
    if target == 'windows':
        numbers = [int(n) for n in re.match(r'"([0-9.]+)', identity['JAVA_VERSION'])[1].split('.')]
        require(tuple((numbers + [0, 0])[:3]) >= (21, 0, 6),
                'Windows packaging requires JDK 21.0.6 or later 21.x with jpackage child-process cleanup; Java 21 GA fails termination tests.')
    require(aliases.get(identity.get('OS_ARCH', '').strip('"')) == expected, 'JDK architecture does not match target.')
    require(identity.get('OS_NAME') == ('"Windows"' if target == 'windows' else '"Darwin"'), 'JDK OS does not match target.')
    for tool in ('java', 'javac', 'jpackage', 'jlink', 'jdeps'):
        require((jdk / 'bin' / (tool + ('.exe' if target == 'windows' else ''))).is_file(), f'Missing JDK tool: {tool}')
    if target == 'macos':
        for library in (jdk / 'lib').rglob('*.dylib'):
            for line in run(['/usr/bin/otool', '-L', library]).splitlines()[2:]:
                dep = line.strip().split(' (compatibility version')[0]
                require(not dep.startswith('/') or dep.startswith(('/usr/lib/', '/System/Library/')),
                        f'JDK requires external library {dep}; use a self-contained arm64 JDK 21.')
    return identity


def destination(target, application, candidate):
    base = ROOT / ('dist/windows/candidates' if target == 'windows' else 'dist/mac/candidates') if application == 'gui' else (
        ROOT / 'releases/candidates' / ('windows-x64' if target == 'windows' else 'macos-arm64'))
    return base / candidate


def prepare(a):
    jdk = Path(a.jdk).resolve()
    identity = host_check(a.platform, jdk)
    model = Path(a.model).resolve()
    require(model.is_file(), 'Required private production model is missing: ' + str(model)
            + '. Supply -PproductionModel or SEEDV6_PRODUCTION_MODEL; no asset is downloaded or substituted.')
    # The runtime loader is authoritative; this early size check bounds accidental bulk input.
    require(model.stat().st_size < 2_000_000, 'Unexpected production model size.')
    candidate = a.candidate
    if candidate == 'auto':
        candidate = 'candidate-' + datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S') + '-' + uuid.uuid4().hex[:8]
    require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,95}', candidate) and candidate not in ('.', '..'), 'Invalid candidate ID.')
    context = contained(a.context, ROOT / 'build/packaging')
    context.parent.mkdir(parents=True, exist_ok=True)
    write_json(context, {'candidate': candidate, 'platform': a.platform,
                        'architecture': 'x64' if a.platform == 'windows' else 'arm64',
                        'jdk': str(jdk), 'jdkIdentity': identity, 'model': str(model), 'modelSha256': sha(model),
                        'source': source_state(), 'version': json.loads((ROOT / 'VERSION_STATE.txt').read_text(encoding='utf-8-sig')),
                        'versionSha256': sha(ROOT / 'VERSION_STATE.txt')})
    print('Native preflight passed: ' + candidate)


def tool(context, name):
    return Path(context['jdk']) / 'bin' / (name + ('.exe' if context['platform'] == 'windows' else ''))


def model_probe(context, stage, jars, model, kind='model', java=None):
    classes = stage / 'probe-classes'
    if not classes.exists():
        classes.mkdir()
        run([tool(context, 'javac'), '--release', '21', '-encoding', 'UTF-8', '-cp', os.pathsep.join(map(str, jars)),
             '-d', classes, ROOT / 'tools/PackagingProbe.java'])
    work = stage / 'probe working directory'
    work.mkdir(exist_ok=True)
    command = [java or tool(context, 'java'), '-Djava.awt.headless=true', '-Duser.home=' + str(work),
               '-Djava.io.tmpdir=' + str(work), '-Djna.tmpdir=' + str(work)]
    if kind == 'gui':
        command += ['--add-modules=jdk.incubator.vector']
    output = run(command + ['-cp', os.pathsep.join(map(str, [classes, *jars])), 'PackagingProbe', kind, model],
                 cwd=work, env=environment(empty_path=True))
    return properties(output)


def launcher_text():
    return '''#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(/usr/bin/dirname -- "$0")" && pwd)
unset JAVA_HOME JDK_HOME CLASSPATH JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
exec "$APP_HOME/runtime/bin/java" -Xmx768m -Djava.awt.headless=true \\
    -cp "$APP_HOME/app/*" com.ohinteractive.seedv6.uci.UciMain "$@"
'''


def mac_resources(context, stage):
    resources = stage / 'resources'; resources.mkdir()
    with zipfile.ZipFile(Path(context['jdk']) / 'jmods/jdk.jpackage.jmod') as archive:
        template = archive.read('classes/jdk/jpackage/internal/resources/Info-lite.plist.template').decode()
    require('DEPLOY_BUNDLE_SHORT_VERSION' in template, 'Unrecognized JDK Info.plist template')
    version = '.'.join(str(context['version'][n]) for n in ('major', 'minor', 'patch'))
    (resources / 'Info.plist').write_text(template.replace('DEPLOY_BUNDLE_SHORT_VERSION', version), encoding='utf-8')
    iconset = stage / 'seedv6.iconset'; iconset.mkdir()
    for size in (16, 32, 128, 256, 512):
        for scale in (1, 2):
            name = f'icon_{size}x{size}' + ('@2x' if scale == 2 else '') + '.png'
            run(['/usr/bin/sips', '-z', size * scale, size * scale, ROOT / 'crowned_seed_emblem.png', '--out', iconset / name])
    icon = stage / 'seedv6.icns'
    run(['/usr/bin/iconutil', '-c', 'icns', iconset, '-o', icon])
    return resources, icon


def layouts(image, application, target):
    if target == 'macos' and application == 'gui':
        return image / 'Contents/app', image / 'Contents/runtime/Contents/Home', image / 'Contents/MacOS/SeedV6-GUI'
    return image / 'app', image / 'runtime', image / ('SeedV6-' + application.upper() + ('.exe' if target == 'windows' else ''))


def build_image(context, application, stage, jars):
    target = context['platform']; name, main = APPS[application]
    input_dir = stage / 'input'; input_dir.mkdir()
    for jar in jars:
        shutil.copy2(jar, input_dir / jar.name)
    model = stage / 'model-input.brn-pair2'
    shutil.copy2(context['model'], model)
    require(sha(model) == context['modelSha256'], 'Model changed since preflight.')
    identity = model_probe(context, stage, list(input_dir.glob('*.jar')), model)
    require(identity['model.sha256'] == sha(model), 'Model differs from canonical engine-core identity.')
    require(identity['version'] == '.'.join(str(context['version'][n]) for n in ('major', 'minor', 'patch'))
            and identity['build'] == str(context['version']['build']), 'Embedded application version differs from authority.')
    # Keep stamps in a separate tiny resource JAR; never rewrite shared engine-core.jar.
    if application == 'gui':
        with zipfile.ZipFile(input_dir / 'desktop-package.jar', 'x', zipfile.ZIP_DEFLATED) as archive:
            archive.writestr('com/ohinteractive/seedv6/desktop-build.properties',
                             'builtAt=' + datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z')
                             + '\nplatform=' + target + ' / ' + context['architecture'] + '\n')
    if target == 'macos' and application == 'uci':
        image = stage / name; image.mkdir()
        shutil.copytree(input_dir, image / 'app')
        modules = run([tool(context, 'jdeps'), '--multi-release', '21', '--print-module-deps',
                       '--class-path', str(input_dir / '*'), input_dir / 'app-uci.jar', input_dir / 'engine-core.jar'])
        require(modules == 'java.base', 'Standalone UCI must require java.base only: ' + modules)
        run([tool(context, 'jlink'), '--add-modules', 'java.base', '--strip-debug', '--no-header-files', '--no-man-pages',
             '--compress=zip-6', '--output', image / 'runtime'])
        (image / name).write_text(launcher_text(), encoding='utf-8', newline='\n')
        (image / name).chmod(0o755)
    else:
        command = [tool(context, 'jpackage'), '--type', 'app-image', '--name', name,
                   '--vendor', 'OH Interactive', '--input', input_dir, '--main-jar', f'app-{application}.jar',
                   '--main-class', main, '--dest', stage,
                   '--temp', stage / 'jpackage-temp',
                   '--app-version', identity['version'] if target == 'windows' else identity['build'],
                   '--jlink-options', '--strip-debug --no-man-pages --no-header-files']
        if application == 'gui':
            command += ['--add-modules', 'jdk.incubator.vector,ALL-DEFAULT', '--java-options', '--add-modules=jdk.incubator.vector']
            if target == 'windows':
                command += ['--icon', ROOT / 'tools/icons/seedv6.ico']
            else:
                require(int(identity['build']) > 0, 'macOS jpackage requires a positive shared build number.')
                resources, icon = mac_resources(context, stage)
                command += ['--resource-dir', resources, '--icon', icon, '--mac-package-identifier', 'com.ohinteractive.seedv6.gui']
        else:
            command += ['--win-console', '--add-modules', 'java.base', '--java-options', '-Xmx768m',
                        '--java-options', '-Djava.awt.headless=true']
        run(command)
        image = stage / (name + ('.app' if target == 'macos' else ''))
    app, runtime, launcher = layouts(image, application, target)
    # UCI resolves a model relative to the JAR's parent directory's parent.
    model_target = app.parent / 'models/baseline.brn-pair2'
    model_target.parent.mkdir(); shutil.copy2(model, model_target)
    require(sha(model_target) == identity['model.sha256'], 'Packaged model mismatch.')
    require(launcher.is_file() and (runtime / 'legal/java.base/LICENSE').is_file(), 'Incomplete launcher/runtime/license.')
    # Every third-party JAR remains intact, including its bundled licenses/native payloads.
    (image / 'CANDIDATE.txt').write_text('SeedV6 local packaging candidate. Not an official release.\n'
        'Private model input: publication requires owner approval.\n'
        'GUI interactive acceptance and external-client acceptance remain separate gates.\n', encoding='utf-8')
    return image, identity


def file_inventory(folder):
    return {p.relative_to(folder).as_posix(): {'sha256': sha(p), 'bytes': p.stat().st_size,
            'mode': stat.S_IMODE(p.stat().st_mode)} for p in sorted(folder.rglob('*')) if p.is_file()}


def core_content(jar):
    with zipfile.ZipFile(jar) as archive:
        entries = {name: hashlib.sha256(archive.read(name)).hexdigest()
                   for name in sorted(archive.namelist()) if not name.endswith('/')}
    native = {name: value for name, value in entries.items() if name.startswith(('native/', 'META-INF/licenses/Fathom'))}
    portable = {name: value for name, value in entries.items() if name not in native}
    return {'portableEntriesSha256': hashlib.sha256(encode(portable)).hexdigest(), 'nativeEntries': native}


def make_zip(image, target):
    with zipfile.ZipFile(target, 'x', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in [image, *sorted(image.rglob('*'))]:
            # Dereference jlink's internal license links; the archive is self-contained.
            archive.write(path, path.relative_to(image.parent).as_posix())


def extract_verified(archive_path, destination_dir):
    destination_dir.mkdir()
    with zipfile.ZipFile(archive_path) as archive:
        for member in archive.infolist():
            target = contained(destination_dir / member.filename, destination_dir)
            require(not stat.S_ISLNK(member.external_attr >> 16), 'Unexpected archive symlink')
            archive.extract(member, destination_dir)
            target.chmod(stat.S_IMODE(member.external_attr >> 16))


def verify_integrity(image, archive=None):
    manifest = json.loads((image / 'build-manifest.json').read_text())
    actual = file_inventory(image); actual.pop('build-manifest.json')
    require(actual == manifest['files'], 'Image file inventory differs from manifest.')
    if 'engine' in manifest:
        app, _, _ = layouts(image, manifest['application'], manifest['platform'])
        require(sha(app / 'engine-core.jar') == manifest['engine']['coreJarSha256'], 'Recorded core identity differs from actual JAR.')
        require(sha(app.parent / 'models/baseline.brn-pair2') == manifest['engine']['model']['model.sha256'],
                'Recorded model identity differs from actual payload.')
        if 'coreContent' in manifest['engine']:
            require(core_content(app / 'engine-core.jar') == manifest['engine']['coreContent'], 'Core content record differs.')
    if archive:
        with zipfile.ZipFile(archive) as zipped:
            require(all(i.filename.startswith(image.name + '/') and '..' not in Path(i.filename).parts
                        for i in zipped.infolist()), 'Unsafe or unexpected archive root.')
            files = {info.filename.split('/', 1)[1]: {'sha256': hashlib.sha256(zipped.read(info)).hexdigest(),
                     'bytes': info.file_size} for info in zipped.infolist() if not info.is_dir()}
            expected = {n: {k: v[k] for k in ('sha256', 'bytes')} for n, v in file_inventory(image).items()}
            require(len(files) == len([i for i in zipped.infolist() if not i.is_dir()]) and files == expected,
                    'Archive differs from application image.')
            if os.name != 'nt':
                for info in zipped.infolist():
                    if not info.is_dir():
                        require(stat.S_IMODE(info.external_attr >> 16) == stat.S_IMODE((image / info.filename.split('/', 1)[1]).stat().st_mode),
                                'Archive executable permissions differ.')
    return manifest


def rename_exclusive(source, destination_path):
    if sys.platform == 'darwin':
        libc = ctypes.CDLL(None, use_errno=True)
        rename = libc.renamex_np
        rename.argtypes = [ctypes.c_char_p, ctypes.c_char_p, ctypes.c_uint]; rename.restype = ctypes.c_int
        if rename(os.fsencode(source), os.fsencode(destination_path), 0x00000004):
            error = ctypes.get_errno(); raise OSError(error, os.strerror(error), str(destination_path))
    else:
        require(os.name == 'nt', 'Exclusive candidate promotion is supported on Windows/macOS only.')
        os.rename(source, destination_path)  # Windows rename never replaces an existing destination.


def verify_runtime(context, application, image, stage):
    from native_package_checks import verify_uci, verify_dependencies
    app, runtime, launcher = layouts(image, application, context['platform'])
    java = runtime / 'bin' / ('java.exe' if context['platform'] == 'windows' else 'java')
    modules = run([java, '--list-modules'], env=environment(empty_path=True)).splitlines()
    verify_dependencies(application, app, modules, context['platform'])
    if context['platform'] == 'macos':
        for binary in (java, runtime / 'lib/server/libjvm.dylib'):
            require(run(['/usr/bin/lipo', '-archs', binary]) == 'arm64', 'Wrong Mac binary architecture: ' + str(binary))
        require(os.access(launcher, os.X_OK) and os.access(java, os.X_OK), 'Missing executable permissions.')
    if application == 'gui':
        result = model_probe(context, stage, sorted(app.glob('*.jar')), app.parent / 'models/baseline.brn-pair2', 'gui', java)
        require(result.get('gui.headlessResourcesAndJni') == 'passed', 'GUI headless native verification failed.')
        return {'status': 'headless-resources-jni-passed', 'interactiveGui': 'NOT RUN; owner gate',
                'optionalSyzygy': result['gui.optionalSyzygy'], 'modules': modules}
    return verify_uci(launcher, stage, environment(empty_path=True), app.parent / 'models/baseline.brn-pair2')


def package(a):
    context = json.loads(Path(a.context).read_text())
    require(context['platform'] == a.platform, 'Context platform mismatch.')
    require(source_state() == context['source'], 'Source changed after packaging preflight; retry with fresh candidate ID.')
    host_check(context['platform'], Path(context['jdk']))
    jars = [Path(j).resolve() for j in a.jar]
    require(len({j.name for j in jars}) == len(jars), 'Duplicate runtime JAR names.')
    require(all(j.is_file() for j in jars), 'Missing runtime artifact.')
    output = destination(a.platform, a.application, context['candidate'])
    output.parent.mkdir(parents=True, exist_ok=True)
    require(not os.path.lexists(output), 'Occupied candidate destination: ' + str(output))
    stage = output.parent / ('.staging-' + context['candidate'] + '-' + uuid.uuid4().hex)
    stage.mkdir()
    write_json(stage / 'attempt.json', {'status': 'UNVERIFIED', 'candidate': context['candidate']})
    try:
        image, identity = build_image(context, a.application, stage, jars)
        app, _, _ = layouts(image, a.application, a.platform)
        for jar in jars:
            require(sha(jar) == sha(app / jar.name), 'Packaged JAR differs from Gradle artifact: ' + jar.name)
        native = verify_runtime(context, a.application, image, stage)
        engine = {'model': identity, 'coreJarSha256': sha(app / 'engine-core.jar'),
                  'coreContent': core_content(app / 'engine-core.jar'),
                  'productionSearch': {'accepted': 'SR-001/SR-029/SR-022/SR-005/SR-036/SR-024',
                     'see': 'qxray', 'lmr': 'quiet', 'lmp': 'window', 'oneWorker': 'effort',
                     'multiworker': 'root_reuse (2..16)', 'sr021': 'deferred; disabled'},
                  'coreInputs': {n: v for n, v in context['source']['files'].items()
                                  if n.startswith('engine-core/src/main/') or n in (
                                      'VERSION_STATE.txt', 'engine-core/build.gradle', 'gradle/java-conventions.gradle',
                                      'gradle/application-version.gradle', 'gradle/syzygy.gradle')}}
        manifest = {'format': 1, 'kind': 'local-candidate', 'officialRelease': False,
                    'candidate': context['candidate'], 'application': a.application,
                    'platform': a.platform, 'architecture': context['architecture'],
                    'version': context['version'], 'source': context['source'],
                    'engine': engine, 'jdk': context['jdkIdentity'],
                    'compilerJdk': properties((Path(a.compiler_jdk) / 'release').read_text()),
                    'verification': native, 'files': file_inventory(image)}
        write_json(image / 'build-manifest.json', manifest)
        archive = stage / f'{APPS[a.application][0]}-{a.platform}-{context["architecture"]}.zip'
        make_zip(image, archive)
        verify_integrity(image, archive)
        extracted = stage / 'extracted package with spaces'
        extract_verified(archive, extracted)
        relocated = extracted / image.name
        # Extraction path has no source layout; child PATH is empty and cwd unrelated.
        verify_integrity(relocated)
        extracted_check = verify_runtime(context, a.application, relocated, stage)
        require(source_state() == context['source'], 'Source changed during packaging.')
        require(sha(ROOT / 'VERSION_STATE.txt') == context['versionSha256'], 'Packaging changed version authority.')
        require(sha(context['model']) == context['modelSha256'], 'Model input changed during packaging.')
        result = {'status': 'VERIFIED_LOCAL_CANDIDATE', 'officialRelease': False,
                  'candidate': context['candidate'], 'application': a.application,
                  'image': image.name, 'archive': archive.name, 'archiveSha256': sha(archive),
                  'manifestSha256': sha(image / 'build-manifest.json'), 'coreJarSha256': engine['coreJarSha256'],
                  'modelSha256': identity['model.sha256'], 'imageChecks': native, 'extractedChecks': extracted_check,
                  'interactiveGuiAcceptance': 'outstanding' if a.application == 'gui' else 'not applicable'}
        # Only the deliverables enter the final directory. Diagnostics/extractions stay
        # in unpublished staging; an atomic exclusive rename promotes the whole set.
        deliverables = stage / 'candidate'; deliverables.mkdir()
        shutil.move(str(image), deliverables / image.name)
        shutil.move(str(archive), deliverables / archive.name)
        write_json(deliverables / 'candidate.json', result)
        write_json(stage / 'attempt.json', {'status': 'VERIFIED_STAGING', 'intendedOutput': str(output)})
        rename_exclusive(deliverables, output)
        print(json.dumps({'candidate': str(output), 'archive': str(output / archive.name),
                          'coreJarSha256': engine['coreJarSha256'], 'status': result['status']}))
    except Exception as error:
        write_json(stage / 'attempt.json', {'status': 'FAILED_UNVERIFIED', 'error': str(error)})
        raise


def compare(a):
    context = json.loads(Path(a.context).read_text())
    manifests = []
    for app in APPS:
        output = destination(a.platform, app, context['candidate'])
        receipt = json.loads((output / 'candidate.json').read_text())
        require(receipt['status'] == 'VERIFIED_LOCAL_CANDIDATE', 'Unverified candidate')
        require(sha(output / receipt['archive']) == receipt['archiveSha256'], 'Archive hash mismatch')
        manifests.append(verify_integrity(output / receipt['image'], output / receipt['archive']))
    require(manifests[0]['engine'] == manifests[1]['engine'], 'GUI/UCI actual engine identity differs.')
    require(manifests[0]['source'] == manifests[1]['source'] == context['source'], 'GUI/UCI source identity differs.')
    require(source_state() == context['source'], 'Source changed before coordinated verification.')
    print('BOTH local candidates verified; identical engine-core bytes and production model/configuration. ' + context['candidate'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('prepare', 'package', 'compare'))
    parser.add_argument('--platform', choices=('windows', 'macos'), required=True)
    parser.add_argument('--context', required=True)
    parser.add_argument('--jdk'); parser.add_argument('--model'); parser.add_argument('--candidate', default='auto')
    parser.add_argument('--compiler-jdk')
    parser.add_argument('--application', choices=APPS); parser.add_argument('--jar', action='append', default=[])
    args = parser.parse_args()
    try:
        globals()[args.action](args)
    except Exception as error:
        print('Native candidate packaging FAILED: ' + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
