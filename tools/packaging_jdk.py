"""Installed native JDK selection, independent of Gradle's daemon and toolchains.

Discovery is bounded to conventional installation roots; never downloads a JDK
or changes the machine environment. Explicit selections fail without fallback.
"""
import os
from pathlib import Path
import re
import shutil
import subprocess


def home(path):
    path = Path(path).expanduser().resolve()
    if (path / 'Contents/Home').is_dir():
        path /= 'Contents/Home'
    return path


def version(identity):
    match = re.fullmatch(r'"?(21(?:\.\d+)*)(?:\+[^"\s]+)?"?', identity.get('JAVA_VERSION', ''))
    if not match:
        raise RuntimeError('Packaging requires a stable Java 21 JDK (21.0.6 or newer); other majors and early access are unsupported.')
    numbers = tuple(int(n) for n in match[1].split('.'))
    if (numbers + (0, 0))[:3] < (21, 0, 6):
        raise RuntimeError('Packaging requires JDK 21.0.6 or newer 21.x; Java 21 GA is incompatible.')
    return numbers + (0,) * max(0, 4 - len(numbers))


def validate(jdk, target):
    release = jdk / 'release'
    if not release.is_file():
        raise RuntimeError('Missing JDK release file: ' + str(release))
    identity = dict(line.split('=', 1) for line in release.read_text().splitlines()
                    if '=' in line and not line.startswith('#'))
    version(identity)
    aliases = {'amd64': 'x64', 'x86_64': 'x64', 'x64': 'x64', 'aarch64': 'arm64', 'arm64': 'arm64'}
    expected = 'x64' if target == 'windows' else 'arm64'
    if aliases.get(identity.get('OS_ARCH', '').strip('"').lower()) != expected:
        raise RuntimeError('JDK architecture does not match target ' + expected + '.')
    if identity.get('OS_NAME') != ('"Windows"' if target == 'windows' else '"Darwin"'):
        raise RuntimeError('JDK OS does not match target ' + target + '.')
    for name in ('java', 'javac', 'jpackage', 'jlink', 'jdeps'):
        if not (jdk / 'bin' / (name + ('.exe' if target == 'windows' else ''))).is_file():
            raise RuntimeError('Missing JDK tool: ' + name)
    for name in ('java.base', 'jdk.jpackage'):
        if not (jdk / 'jmods' / (name + '.jmod')).is_file():
            raise RuntimeError('Incomplete JDK; missing jmods/' + name + '.jmod')
    if target == 'macos':
        for library in (jdk / 'lib').rglob('*.dylib'):
            result = subprocess.run(['/usr/bin/otool', '-L', str(library)], capture_output=True,
                                    text=True, timeout=30, check=True)
            for line in result.stdout.splitlines()[2:]:
                dependency = line.strip().split(' (compatibility version')[0]
                if dependency.startswith('/') and not dependency.startswith(('/usr/lib/', '/System/Library/')):
                    raise RuntimeError(f'JDK requires external library {dependency}; use a self-contained arm64 JDK 21.')
    return identity


def registry_homes():
    if os.name != 'nt':
        return []
    import winreg
    paths = []
    for hive in (winreg.HKEY_LOCAL_MACHINE, winreg.HKEY_CURRENT_USER):
        for view in (winreg.KEY_WOW64_64KEY, winreg.KEY_WOW64_32KEY):
            for key in ('SOFTWARE\\JavaSoft\\JDK', 'SOFTWARE\\JavaSoft\\Java Development Kit'):
                try:
                    with winreg.OpenKey(hive, key, 0, winreg.KEY_READ | view) as parent:
                        for index in range(winreg.QueryInfoKey(parent)[0]):
                            with winreg.OpenKey(parent, winreg.EnumKey(parent, index)) as child:
                                try:
                                    paths.append(winreg.QueryValueEx(child, 'JavaHome')[0])
                                except OSError:
                                    pass
                except OSError:
                    pass
    return paths


def candidates(target, gradle_jdk='', installation_paths=''):
    paths = [p for p in (gradle_jdk, os.environ.get('JAVA_HOME'), os.environ.get('JDK_HOME')) if p]
    paths += [p.strip() for p in installation_paths.split(',') if p.strip()]
    java = shutil.which('java')
    if java:
        paths.append(Path(java).resolve().parent.parent)
    roots = [Path.home() / '.jdks', Path.home() / '.gradle/jdks']
    if os.environ.get('GRADLE_USER_HOME'):
        roots.append(Path(os.environ['GRADLE_USER_HOME']) / 'jdks')
    if target == 'windows':
        paths += registry_homes()
        roots.append(Path(os.environ.get('SystemDrive', 'C:') + '/java'))
        for base in {os.environ.get('ProgramFiles', 'C:/Program Files'), os.environ.get('ProgramW6432', 'C:/Program Files')}:
            roots += [Path(base) / vendor for vendor in
                      ('Java', 'Eclipse Adoptium', 'Microsoft', 'Amazon Corretto', 'BellSoft', 'Zulu', 'Semeru')]
    else:
        roots += [Path('/Library/Java/JavaVirtualMachines'), Path.home() / 'Library/Java/JavaVirtualMachines',
                  Path('/opt/homebrew/opt/openjdk@21/libexec'), Path('/usr/local/opt/openjdk@21/libexec'),
                  Path.home() / '.sdkman/candidates/java', Path('/opt/java')]
        try:
            result = subprocess.run(['/usr/libexec/java_home', '-V'], capture_output=True, text=True, timeout=15)
            for line in (result.stdout + '\n' + result.stderr).splitlines():
                match = re.search(r'(/[^\n]+/Contents/Home)\s*$', line)
                if match:
                    paths.append(match[1])
        except (OSError, subprocess.SubprocessError):
            pass  # Conventional roots still work without java_home.
    for root in roots:
        if root.is_dir():
            paths.append(root)
            # Gradle-managed installations may add one vendor directory level.
            for child in sorted(root.iterdir()):
                if child.is_dir():
                    paths.append(child)
                    if root.name == 'jdks':
                        paths.extend(p for p in child.iterdir() if p.is_dir())
    return sorted({home(p) for p in paths}, key=lambda p: str(p).casefold())


def select(target, explicit='', gradle_jdk='', installation_paths=''):
    if explicit:
        selected = home(explicit)
        try:
            return selected, validate(selected, target)
        except (OSError, RuntimeError, subprocess.SubprocessError) as error:
            raise RuntimeError(f'Explicit packaging JDK is incompatible: {selected}: {error}') from error
    accepted, rejected = [], []
    for candidate in candidates(target, gradle_jdk, installation_paths):
        try:
            identity = validate(candidate, target)
            accepted.append((version(identity), str(candidate).casefold(), candidate, identity))
        except (OSError, RuntimeError, subprocess.SubprocessError) as error:
            rejected.append(f'  {candidate}: {error}')
    if not accepted:
        setting = 'windowsPackagingJdk' if target == 'windows' else 'macPackagingJdk'
        raise RuntimeError('No compatible installed packaging JDK found. Install a complete native Java 21 JDK '
                           '(21.0.6 or newer) with jpackage, or select it using -P' + setting
                           + '=<JDK home> / optional build-assets/packaging.properties. No JDK is downloaded.\n'
                           + '\n'.join(rejected))
    _, _, selected, identity = max(accepted)
    return selected, identity
