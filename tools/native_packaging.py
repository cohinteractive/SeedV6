"""Reusable local distributions and explicitly frozen candidates, verified before publication.

Reuses the GUI jpackage/Info.plist/icon workflow and immutable UE SR-024
87b8b5b50acf3d2fe19793bfb0e8b24a45be114b console/jlink/RENAME_EXCL techniques.
No release counter, publication, download, source-engine copy or model registry.
"""
import argparse
from contextlib import contextmanager
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

import packaging_jdk
from native_package_checks import UCI_MODULES, NNUE_LAUNCHER, NNUE_MAIN, CP64_TC_P1_FLAGS, CP64_CLV_C1_FLAGS, verify_baseline_identity

ROOT = Path(__file__).resolve().parents[1]
APPS = {'gui': ('SeedV6-GUI', 'com.ohinteractive.seedv6.gui.SwingLauncher'),
        'uci': ('SeedV6-UCI', 'com.ohinteractive.seedv6.uci.UciMain')}


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def sha(path):
    # hashlib.file_digest requires Python 3.11; packaging also supports 3.9.
    digest = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


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


def host_check(target, jdk=None):
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
    return packaging_jdk.validate(jdk, target) if jdk is not None else None


def production_manifest():
    # This is an asset description, not another model selector/registry. The
    # compiled engine-core loader independently pins and decodes the payload.
    manifest = json.loads((ROOT / 'build-assets/production/manifest.json').read_text())
    require(manifest['format'] == 1, 'Unsupported production asset manifest.')
    return manifest


def verify_model(model):
    expected = production_manifest()['identity']
    require(model.is_file(), 'Required production model is missing: ' + str(model)
            + '. Restore tracked build-assets/production assets with Git, or supply -PproductionModel '
            + '/ SEEDV6_PRODUCTION_MODEL; no asset is downloaded or substituted.')
    require(model.stat().st_size == int(expected['model.bytes']), 'Incompatible production model length: ' + str(model))
    actual = sha(model)
    require(actual == expected['model.sha256'], 'Corrupt or different production model: ' + str(model)
            + '; expected SHA-256 ' + expected['model.sha256'] + ', got ' + actual)
    return actual


def destination(target, application, candidate):
    if candidate == 'current':
        return (ROOT / 'dist' / ('windows' if target == 'windows' else 'mac') / 'current'
                if application == 'gui' else ROOT / 'releases/current' /
                ('windows-x64' if target == 'windows' else 'macos-arm64'))
    base = ROOT / ('dist/windows/candidates' if target == 'windows' else 'dist/mac/candidates') if application == 'gui' else (
        ROOT / 'releases/candidates' / ('windows-x64' if target == 'windows' else 'macos-arm64'))
    return base / candidate


def safe_tree(path):
    """Never traverse links/junctions when replacing or cleaning generated output."""
    require(not path.is_symlink(), 'Refusing linked output: ' + str(path))
    for item in [path, *path.rglob('*')]:
        info = item.lstat()
        internal_file_link = (os.name != 'nt' and item.is_symlink() and item.is_file()
                              and item.resolve().is_relative_to(path.resolve()))
        require(internal_file_link or (not (getattr(info, 'st_file_attributes', 0) & 1024) and not item.is_symlink()),
                'Refusing linked output entry: ' + str(item))


@contextmanager
def output_lock(output):
    """OS lock, automatically released on process exit; never unlink a lock inode."""
    lock_path = output.parent / ('.' + output.name + '.packaging.lock')
    with lock_path.open('a+b') as stream:
        if stream.tell() == 0:
            stream.write(b'0'); stream.flush()
        stream.seek(0)
        try:
            if os.name == 'nt':
                import msvcrt
                msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError as error:
            raise RuntimeError('Another build owns ' + str(output) + '; wait and retry.') from error
        try:
            yield
        finally:
            if os.name == 'nt':
                stream.seek(0); msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(stream, fcntl.LOCK_UN)


def no_active_process(path):
    if os.name != 'nt':
        return
    # Also catches orphaned jpackage/java children after a hard interruption.
    env = environment()
    env['SEEDV6_INSPECT_PATH'] = str(path.resolve())
    env['SEEDV6_INSPECT_PID'] = str(os.getpid())
    command = r"""$ErrorActionPreference='Stop'
$target=$env:SEEDV6_INSPECT_PATH.Replace('/','\')
Get-CimInstance Win32_Process | Where-Object {
    $_.ProcessId -ne $PID -and $_.ProcessId -ne [int]$env:SEEDV6_INSPECT_PID -and
    (($_.ExecutablePath -and $_.ExecutablePath.Replace('/','\').IndexOf($target,[StringComparison]::OrdinalIgnoreCase) -ge 0) -or
     ($_.CommandLine -and $_.CommandLine.Replace('/','\').IndexOf($target,[StringComparison]::OrdinalIgnoreCase) -ge 0))
} | ForEach-Object { "$($_.Name) PID $($_.ProcessId)" }
"""
    active = run(['powershell.exe', '-NoProfile', '-NonInteractive', '-Command', command], env=env)
    require(not active, 'Close processes using ' + str(path) + ' and retry: ' + active)


@contextmanager
def idle_tree(path):
    """Reject running/locked files and hold off new readers while examining a tree."""
    if not path.exists():
        yield
        return
    safe_tree(path)
    no_active_process(path)
    handles = []
    if os.name == 'nt':
        kernel = ctypes.WinDLL('kernel32', use_last_error=True)
        create = kernel.CreateFileW
        create.argtypes = [ctypes.c_wchar_p, ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p,
                           ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p]
        create.restype = ctypes.c_void_p
        kernel.CloseHandle.argtypes = [ctypes.c_void_p]
    try:
        if os.name == 'nt':
            for file in sorted(path.rglob('*')):
                if not file.is_file():
                    continue
                # DELETE access catches mapped executables; omit WRITE because
                # jpackage deliberately marks some launchers read-only.
                handle = create(str(file), 0x80010000, 4, None, 3, 0, None)
                if handle == ctypes.c_void_p(-1).value:
                    raise RuntimeError('Output is locked or not writable: ' + str(file)
                                       + '; close the engine/GUI or file viewer and retry. Windows error '
                                       + str(ctypes.get_last_error()))
                handles.append(handle)
        yield
    finally:
        for handle in handles:
            kernel.CloseHandle(handle)


def remove_idle_tree(path):
    if path.exists():
        with idle_tree(path):
            pass
        def readonly_retry(function, name, error):
            item = Path(name)
            if not item.stat().st_mode & stat.S_IWRITE:
                item.chmod(item.stat().st_mode | stat.S_IWRITE)
                function(name)
            else:
                raise error[1]
        shutil.rmtree(path, onerror=readonly_retry)


def verify_published(output):
    safe_tree(output)
    receipt = json.loads((output / 'candidate.json').read_text())
    require(receipt['status'] == 'VERIFIED_LOCAL_CANDIDATE', 'Unverified previous output: ' + str(output))
    # Finder adds directory metadata when browsing a distribution. Tolerate only
    # that regular file; directories, links and other user content remain protected.
    entries = {item.name for item in output.iterdir()
               if item.name != '.DS_Store' or not stat.S_ISREG(item.lstat().st_mode)}
    require(entries == {'candidate.json', receipt['image'], receipt['archive']},
            'Unexpected files in previous output; preserve/move user work before retrying: ' + str(output))
    image = contained(output / receipt['image'], output)
    archive = contained(output / receipt['archive'], output)
    require(sha(archive) == receipt['archiveSha256'] and
            sha(image / 'build-manifest.json') == receipt['manifestSha256'], 'Previous output was modified: ' + str(output))
    verify_integrity(image, archive)


def recover_previous(output):
    previous = output.parent / ('.previous-' + output.name)
    if previous.exists():
        verify_published(previous)
        if not output.exists():
            with idle_tree(previous):
                pass
            rename_exclusive(previous, output)
            print('Restored previous successful distribution: ' + str(output), flush=True)
        else:
            verify_published(output)
            remove_idle_tree(previous)


def publish(deliverables, output, mutable):
    if not mutable:
        rename_exclusive(deliverables, output)
        return
    previous = output.parent / ('.previous-' + output.name)
    require(not previous.exists(), 'Unrecovered previous distribution: ' + str(previous))
    if output.exists():
        verify_published(output)
    with idle_tree(output):
        pass
    if output.exists():
        try:
            rename_exclusive(output, previous)
        except OSError as error:
            raise RuntimeError('Cannot retire current output; close its GUI/engine/file viewers and retry: ' + str(output)) from error
    try:
        # After retirement the canonical path cannot launch new old readers. Check
        # again for a consumer racing the first check and hold handles until publish.
        with idle_tree(previous):
            rename_exclusive(deliverables, output)
    except BaseException as publication_error:
        if previous.exists():
            try:
                rename_exclusive(previous, output)
            except Exception as restore_error:
                raise RuntimeError('Publication failed (' + str(publication_error)
                                   + '); the previous complete distribution remains at ' + str(previous)
                                   + '. Close its processes/file viewers and retry for recovery. Restore error: '
                                   + str(restore_error)) from publication_error
        raise
    # Published output is complete. If cleanup is locked, keep the backup and fail
    # clearly; the next invocation validates both before retrying cleanup.
    remove_idle_tree(previous)


def prepare(a):
    host_check(a.platform)
    jdk, identity = packaging_jdk.select(a.platform, a.jdk, a.gradle_jdk, a.jdk_installation_paths)
    print('Packaging JDK (' + ('explicit' if a.jdk else 'discovered') + '): ' + str(jdk)
          + '; Java ' + identity['JAVA_VERSION'] + '; Gradle JDK: ' + a.gradle_jdk, flush=True)
    model = Path(a.model).resolve()
    model_sha = verify_model(model)
    candidate = a.candidate
    if candidate == 'auto':
        candidate = 'candidate-' + datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S') + '-' + uuid.uuid4().hex[:8]
    require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,95}', candidate) and candidate not in ('.', '..'), 'Invalid candidate ID.')
    context = contained(a.context, ROOT / 'build/packaging')
    context.parent.mkdir(parents=True, exist_ok=True)
    write_json(context, {'candidate': candidate, 'platform': a.platform,
                        'architecture': 'x64' if a.platform == 'windows' else 'arm64',
                        'jdk': str(jdk), 'jdkIdentity': identity, 'model': str(model), 'modelSha256': model_sha,
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
    command += ['--add-modules=jdk.incubator.vector']
    output = run(command + ['-cp', os.pathsep.join(map(str, [classes, *jars])), 'PackagingProbe', kind, model],
                 cwd=work, env=environment(empty_path=True))
    return properties(output)


def launcher_text(nnue=False):
    options = '--add-modules=jdk.incubator.vector ' + ('-XX:ActiveProcessorCount=2 ' if nnue else '')
    main = NNUE_MAIN if nnue else APPS['uci'][1]
    return '''#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(/usr/bin/dirname -- "$0")" && pwd)
unset JAVA_HOME JDK_HOME CLASSPATH JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
exec "$APP_HOME/runtime/bin/java" ''' + options + '''-Xmx768m -Djava.awt.headless=true \\
    -cp "$APP_HOME/app/*" ''' + main + ''' "$@"
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
    require(all(identity.get(key) == value for key, value in production_manifest()['identity'].items()),
            'Production asset manifest differs from authoritative engine-core model identity.')
    verify_baseline_identity(identity.get('baseline.identity', ''), 1, 64)
    require(identity['version'] == '.'.join(str(context['version'][n]) for n in ('major', 'minor', 'patch'))
            and identity['build'] == str(context['version']['build']), 'Embedded application version differs from authority.')
    # Keep stamps in a separate tiny resource JAR; never rewrite shared engine-core.jar.
    if application == 'gui':
        with zipfile.ZipFile(input_dir / 'desktop-package.jar', 'x', zipfile.ZIP_DEFLATED) as archive:
            archive.writestr('com/ohinteractive/seedv6/desktop-build.properties',
                             'builtAt=' + datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z')
                             + '\nplatform=' + target + ' / ' + context['architecture'] + '\n')
    if application == 'uci':
        # The shared core now includes conventional NNUE's optional Vector kernel.
        # Keep the headless boundary strict: no desktop/training or unknown modules.
        modules = run([tool(context, 'jdeps'), '--multi-release', '21', '--print-module-deps',
                       '--class-path', str(input_dir / '*'), input_dir / 'app-uci.jar', input_dir / 'engine-core.jar'])
        require(set(modules.split(',')) == set(UCI_MODULES),
                'Standalone UCI must require exactly ' + ','.join(UCI_MODULES) + ': ' + modules)
    if target == 'macos' and application == 'uci':
        image = stage / name; image.mkdir()
        shutil.copytree(input_dir, image / 'app')
        run([tool(context, 'jlink'), '--add-modules', ','.join(UCI_MODULES), '--strip-debug', '--no-header-files', '--no-man-pages',
             '--compress=zip-6', '--output', image / 'runtime'])
        # Write exact UTF-8/LF bytes without Python 3.10's write_text(newline=).
        for executable, nnue in ((name, False), (NNUE_LAUNCHER, True)):
            (image / executable).write_bytes(launcher_text(nnue).encode('utf-8'))
            (image / executable).chmod(0o755)
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
            candidate = stage / 'nnue-launcher.properties'
            candidate.write_text('main-jar=app-uci.jar\nmain-class=' + NNUE_MAIN
                + '\njava-options=--add-modules=jdk.incubator.vector -XX:ActiveProcessorCount=2 -Xmx768m -Djava.awt.headless=true'
                + '\nwin-console=true\n', encoding='utf-8')
            command += ['--win-console', '--add-modules', ','.join(UCI_MODULES), '--java-options', '-Xmx768m',
                        '--java-options', '-Djava.awt.headless=true', '--java-options', '--add-modules=jdk.incubator.vector', '--add-launcher', NNUE_LAUNCHER + '=' + str(candidate)]
        run(command)
        image = stage / (name + ('.app' if target == 'macos' else ''))
    app, runtime, launcher = layouts(image, application, target)
    # UCI resolves a model relative to the JAR's parent directory's parent.
    model_target = app.parent / 'models/baseline.brn-pair2'
    model_target.parent.mkdir(); shutil.copy2(model, model_target)
    require(sha(model_target) == identity['model.sha256'], 'Packaged model mismatch.')
    require(launcher.is_file() and (runtime / 'legal/java.base/LICENSE').is_file(), 'Incomplete launcher/runtime/license.')
    if application == 'uci':
        # Carry the candidate's usage, attribution and frozen network provenance.
        shutil.copy2(ROOT / 'docs/nnue-botli-candidate.md', image / 'NNUE-CANDIDATE.md')
        shutil.copy2(ROOT / 'build-assets/nnue-candidate/manifest.json', image / 'network-manifest.json')
    # Every third-party JAR remains intact, including its bundled licenses/native payloads.
    (image / 'CANDIDATE.txt').write_text('SeedV6 local packaging candidate. Not an official release.\n'
        'Approved production model included; candidate publication is a separate owner action.\n'
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
    from native_package_checks import verify_uci, verify_dependencies, verify_nnue, verify_baseline_assets
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
    result = verify_uci(launcher, stage, environment(empty_path=True), app.parent / 'models/baseline.brn-pair2')
    result['baselineAssets'] = verify_baseline_assets(stage, environment(empty_path=True), java, app)
    result['modules'] = modules
    nnue = image / (NNUE_LAUNCHER + ('.exe' if context['platform'] == 'windows' else ''))
    result['nnueCandidate'] = verify_nnue(nnue, stage, environment(empty_path=True), java, app)
    return result


def package(a):
    context = json.loads(Path(a.context).read_text())
    require(context['platform'] == a.platform, 'Context platform mismatch.')
    require(source_state() == context['source'], 'Source changed after packaging preflight; retry with fresh candidate ID.')
    host_check(context['platform'], Path(context['jdk']))
    jars = [Path(j).resolve() for j in a.jar]
    require(len({j.name for j in jars}) == len(jars), 'Duplicate runtime JAR names.')
    require(all(j.is_file() for j in jars), 'Missing runtime artifact.')
    output = destination(a.platform, a.application, context['candidate'])
    require(output.resolve() == output.absolute(), 'Linked output parent: ' + str(output))
    output.parent.mkdir(parents=True, exist_ok=True)
    with output_lock(output):
        package_locked(a, context, jars, output)


def package_locked(a, context, jars, output):
    mutable = context['candidate'] == 'current'
    if mutable:
        recover_previous(output)
        # Fail before expensive assembly when a current executable is in use.
        with idle_tree(output):
            pass
    else:
        require(not os.path.lexists(output), 'Occupied candidate destination: ' + str(output))
    stage = output.parent / ('.staging-' + output.name)
    diagnostic = output.parent / ('.last-attempt-' + output.name + '.json')
    if stage.exists():
        safe_tree(stage)
        attempt = json.loads((stage / 'attempt.json').read_text())
        require(attempt.get('owner') == 'seedv6-native-packaging' and attempt.get('output') == str(output),
                'Unowned staging directory; inspect before removing: ' + str(stage))
        require(not attempt.get('retained'), 'Diagnostic staging retained by request; move/remove after inspection: ' + str(stage))
        # The per-output lock proves no packager owns it; process and file checks
        # additionally protect native children surviving a hard parent interruption.
        remove_idle_tree(stage)
    stage.mkdir()
    attempt = {'owner': 'seedv6-native-packaging', 'output': str(output), 'status': 'UNVERIFIED',
               'candidate': context['candidate'], 'retained': getattr(a, 'keep_staging', False)}
    write_json(stage / 'attempt.json', attempt)
    try:
        image, identity = build_image(context, a.application, stage, jars)
        app, _, _ = layouts(image, a.application, a.platform)
        for jar in jars:
            require(sha(jar) == sha(app / jar.name), 'Packaged JAR differs from Gradle artifact: ' + jar.name)
        native = verify_runtime(context, a.application, image, stage)
        engine = {'model': identity, 'coreJarSha256': sha(app / 'engine-core.jar'),
                  'coreContent': core_content(app / 'engine-core.jar'),
                  'productionSearch': {'profile': dict(field.split('=', 1) for field in identity['baseline.identity'].split()
                                                       if '=' in field)['searchProfile'], 'flags': CP64_CLV_C1_FLAGS,
                     'defaultWorkers': 1,
                     'workerRouting': {'W1': {'formulation': 'CLV-C1', 'flags': CP64_CLV_C1_FLAGS, 'parallel': 'OWNER'},
                                       'W2-W16': {'formulation': 'CLV-C1', 'flags': CP64_CLV_C1_FLAGS,
                                                   'parallel': 'EAGER_ROOT'}},
                     'evaluator': 'Conventional NNUE CP64', 'schema': 'S6CNU002', 'materialHeads': 8,
                     'configuration': identity['baseline.identity']},
                  'coreInputs': {n: v for n, v in context['source']['files'].items()
                                  if n.startswith('engine-core/src/main/') or n in (
                                      'VERSION_STATE.txt', 'engine-core/build.gradle', 'gradle/java-conventions.gradle',
                                      'gradle/application-version.gradle', 'gradle/syzygy.gradle')}}
        manifest = {'format': 1, 'kind': 'local-current' if mutable else 'local-candidate', 'officialRelease': False,
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
        # Only verified deliverables enter the final directory, as one complete tree.
        deliverables = stage / 'candidate'; deliverables.mkdir()
        shutil.move(str(image), deliverables / image.name)
        shutil.move(str(archive), deliverables / archive.name)
        write_json(deliverables / 'candidate.json', result)
        attempt['status'] = 'VERIFIED_STAGING'
        write_json(stage / 'attempt.json', attempt)
        publish(deliverables, output, mutable)
        attempt['status'] = 'PUBLISHED'
        print(json.dumps({'candidate': str(output), 'archive': str(output / archive.name),
                          'coreJarSha256': engine['coreJarSha256'], 'status': result['status']}))
    except BaseException as error:
        attempt.update(status='FAILED', error=str(error), errorType=type(error).__name__)
        raise
    finally:
        write_json(stage / 'attempt.json', attempt)
        write_json(diagnostic, attempt)
        if not attempt['retained']:
            try:
                remove_idle_tree(stage)
            except Exception as cleanup_error:
                # Retain only while genuinely in use; next build retries under lock.
                print('Staging cleanup deferred: ' + str(cleanup_error), file=sys.stderr)
                if attempt['status'] == 'PUBLISHED':
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
    parser.add_argument('--jdk', default=''); parser.add_argument('--gradle-jdk', default='')
    parser.add_argument('--jdk-installation-paths', default='')
    parser.add_argument('--model'); parser.add_argument('--candidate', default='current')
    parser.add_argument('--keep-staging', action='store_true', help='Retain staging for explicit diagnosis; next build requires its removal.')
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
