"""Freeze the dedicated Gradle UCI distribution into a new local, hash-inventoried candidate.

Portable Java 21 convention used by construction-v1; no native cross-compilation,
runtime download, publishing, live executable replacement or version mutation.
"""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import platform
import shutil
import subprocess
import tarfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NAME = 'seedv6-nnue-cp64-e008-rc1'
NETWORK_SHA = '8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b'


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install', type=Path, default=ROOT / 'app-uci/build/install' / NAME)
    args = parser.parse_args()
    def git(*values):
        return subprocess.check_output(['git', *values], cwd=ROOT, text=True).strip()
    if git('status', '--porcelain', '--untracked-files=normal'):
        raise RuntimeError('Commit the reviewed release inputs first; final candidate requires a clean source tree')
    revision = git('rev-parse', 'HEAD')
    version = json.loads((ROOT / 'VERSION_STATE.txt').read_text(encoding='utf-8-sig'))
    install = args.install.resolve()
    if {p.name for p in (install / 'lib').glob('*.jar')} != {'app-uci.jar', 'engine-core.jar'}:
        raise RuntimeError('Unexpected or incomplete runtime closure')
    with zipfile.ZipFile(install / 'lib/engine-core.jar') as jar:
        identity = jar.read('com/ohinteractive/seedv6/application-version.properties').decode()
    if 'sourceRevision=' + revision[:12] + '\n' not in identity or 'build=' + str(version['build']) + '\n' not in identity:
        raise RuntimeError('Rebuild the candidate from the clean commit before freezing it')
    with zipfile.ZipFile(install / 'lib/app-uci.jar') as jar:
        if hashlib.sha256(jar.read('models/conventional-cp64.cnnue')).hexdigest() != NETWORK_SHA:
            raise RuntimeError('Wrong network in packaged JAR')
    if b'\r' in (install / 'bin/seedv6-nnue-cp64').read_bytes():
        raise RuntimeError('POSIX launcher must have LF line endings')
    candidate = NAME + '-build' + str(version['build']) + '-' + revision[:12]
    destination = ROOT / 'releases/candidates/portable-java21' / candidate
    destination.mkdir(parents=True, exist_ok=False)
    image = destination / NAME
    shutil.copytree(install, image)
    (image / 'bin/seedv6-nnue-cp64').chmod(0o755)
    manifest = {
        'candidate': candidate, 'status': 'PENDING_NATIVE_MAC_ACCEPTANCE',
        'source_revision': revision, 'source_clean_at_build': True, 'version': version,
        'built_at_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'packaging_host': platform.platform(), 'network_sha256': NETWORK_SHA,
        'search': 'E008_QXRAY_LMR_WINDOW_EFFORT_HINTS', 'workers': 1,
        'requirements': 'Java 21; Apple Silicon uses arm64 JDK 21. No JNI/training/GUI dependency. Runtime not bundled.',
        'native_mac_tested': False, 'live_botli_modified': False,
        'files': {str(p.relative_to(image)).replace('\\', '/'): sha(p)
                  for p in sorted(image.rglob('*')) if p.is_file()}}
    (image / 'build-manifest.json').write_bytes((json.dumps(manifest, indent=2) + '\n').encode())
    (image / 'SHA256SUMS').write_bytes((''.join(h + '  ' + name + '\n' for name, h in manifest['files'].items())
                                      + sha(image / 'build-manifest.json') + '  build-manifest.json\n').encode())
    tar_path = destination / (candidate + '.tar.gz')
    def modes(info):
        info.uid = info.gid = 0; info.uname = info.gname = ''
        info.mode = 0o755 if info.isdir() or info.name.endswith('/bin/seedv6-nnue-cp64') else 0o644
        return info
    with tarfile.open(tar_path, 'w:gz') as archive:
        archive.add(image, arcname=NAME, filter=modes)
    with tarfile.open(tar_path) as archive:
        if archive.getmember(NAME + '/bin/seedv6-nnue-cp64').mode != 0o755:
            raise RuntimeError('Archive lost executable mode')
        for item in archive.getmembers():
            if item.isfile():
                source = image / Path(item.name).relative_to(NAME)
                if hashlib.sha256(archive.extractfile(item).read()).hexdigest() != sha(source):
                    raise RuntimeError('Archive content mismatch: ' + item.name)
    receipt = {'candidate': candidate, 'source_revision': revision, 'image': str(image.relative_to(ROOT)),
               'archive': str(tar_path.relative_to(ROOT)), 'archive_sha256': sha(tar_path),
               'manifest_sha256': sha(image / 'build-manifest.json'), 'status': manifest['status']}
    (destination / 'candidate.json').write_bytes((json.dumps(receipt, indent=2) + '\n').encode())
    print(json.dumps(receipt, indent=2))


if __name__ == '__main__':
    main()
