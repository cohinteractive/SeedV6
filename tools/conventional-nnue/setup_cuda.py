"""Provision official CUDA libraries locally; never install drivers or edit global settings.

Run without --install to inspect the exact download plan. Existing archives are
hash-verified and reused. Third-party license texts stay in the extracted trees.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request
import zipfile

BASE = 'https://developer.download.nvidia.com/compute/cuda/redist/'
RELEASE = '13.0.2'
COMPONENTS = ('cuda_cudart', 'cuda_nvrtc', 'libcublas')


def sha256(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path('build/conventional-nnue/cuda'))
    parser.add_argument('--install', action='store_true')
    args = parser.parse_args()
    with urllib.request.urlopen(BASE + 'redistrib_' + RELEASE + '.json', timeout=60) as response:
        manifest = json.load(response)
    plan = {name: manifest[name]['windows-x86_64'] for name in COMPONENTS}
    total = sum(int(item['size']) for item in plan.values())
    print(json.dumps({'release': RELEASE, 'downloadBytes': total, 'components': plan}, indent=2), flush=True)
    if not args.install:
        return
    if total > 800_000_000:
        raise RuntimeError('Unexpected download growth: review the plan before installing')
    root = args.root.resolve()
    root.mkdir(parents=True, exist_ok=True)
    if shutil.disk_usage(root).free < 4_000_000_000:
        raise RuntimeError('Need at least 4 GB free for archives, extraction and local libraries')
    for name, item in plan.items():
        archive = root / Path(item['relative_path']).name
        if not archive.exists():
            partial = archive.with_suffix('.partial')
            print('Downloading ' + name, flush=True)
            with urllib.request.urlopen(BASE + item['relative_path'], timeout=60) as response, partial.open('wb') as out:
                shutil.copyfileobj(response, out, 1024 * 1024)
            if partial.stat().st_size != int(item['size']) or sha256(partial) != item['sha256']:
                raise RuntimeError('Download integrity failure: ' + str(partial))
            partial.rename(archive)
        if sha256(archive) != item['sha256']:
            raise RuntimeError('Existing archive hash mismatch; preserved: ' + str(archive))
        print('Verified ' + name + '; extracting', flush=True)
        with zipfile.ZipFile(archive) as package:
            for member in package.infolist():
                target = (root / member.filename).resolve()
                if not target.is_relative_to(root):
                    raise RuntimeError('Unsafe archive path')
            package.extractall(root)
        source = root / archive.stem
        for folder in ('bin', 'lib', 'include'):
            if (source / folder).exists():
                shutil.copytree(source / folder, root / folder, dirs_exist_ok=True)
    (root / 'provisioning.json').write_text(json.dumps({
        'release': RELEASE, 'manifest': BASE + 'redistrib_' + RELEASE + '.json',
        'components': plan, 'scope': 'local extracted libraries; no driver/global installation'
    }, indent=2) + '\n')
    print('Ready: set process-local CUDA_PATH=' + str(root) + ' and add its bin to process PATH', flush=True)


if __name__ == '__main__':
    main()
