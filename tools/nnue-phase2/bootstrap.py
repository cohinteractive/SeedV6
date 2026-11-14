"""Read-only identity audit and exclusive creation of Phase 2 recipes/evidence."""
import base64
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'docs/research/nnue-phase2-cglhw'
PRIOR = ROOT / 'docs/research/conventional-nnue-cglhw/strength-20261114'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(value, stream, indent=2)
        stream.write('\n')


def main():
    protected = {
        'build-assets/production/conventional-wdl25.cnnue': 'a25e76fcdfe1b854452685bec87bc759bed2ed166312579e024410f0903517ab',
        'build-assets/nnue-candidate/conventional-cp64.cnnue': '8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b',
    }
    for name, expected in protected.items():
        assert sha(ROOT / name) == expected, name
    recipe = json.loads((PRIOR / 'wdl25-64.json').read_text())
    verified = dict(protected)
    for name, expected in [(recipe['data'], recipe['data_sha256']), (recipe['probes'], recipe['probes_sha256'])]:
        assert sha(ROOT / name) == expected, name
        verified[name] = expected
    manifest = json.loads((PRIOR / 'mixed-data-manifest.json').read_text())
    for name, expected in manifest['hashes'].items():
        path = PRIOR / 'raw/mixed-data' / name
        assert sha(path) == expected, str(path)
        verified[str(path.relative_to(ROOT))] = expected
    frozen = json.loads((PRIOR / 'frozen-runtime.json').read_text())
    for name, expected in frozen['files'].items():
        assert sha(ROOT / frozen['path'] / name) == expected, name
    holdout = PRIOR / 'raw/mixed-holdout/validation.txt'
    assert sha(holdout) == '2742eb3acdfcc0d6c45e040fd1183e48f9cd430174b2542078570fa7e3ea8271'
    verified[str(holdout.relative_to(ROOT))] = sha(holdout)
    binary = ROOT / 'build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe'
    assert sha(binary) == '57a9ee3cd70334a5c6a6bbe3ea42b58985179da4fa81f166f4e14b5540b2c8e3'
    version = (ROOT / 'VERSION_STATE.txt').read_bytes()
    assert json.loads(version) == dict(major=0, minor=0, patch=0, build=101)
    result = dict(schema='seed-nnue-phase2-baseline-v1',
                  head=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                  protected_sha256=protected, verified_inputs=verified,
                  trainer_sha256=sha(binary), frozen_runtime_verified_files=len(frozen['files']),
                  version_bytes_base64=base64.b64encode(version).decode(),
                  version_sha256=hashlib.sha256(version).hexdigest(),
                  no_root_journal=not (ROOT / 'CODEXLOG_CURRENT.md').exists())
    write(BASE / 'baseline.json', result)
    narrow = ROOT / 'docs/research/conventional-nnue-cglhw/raw/e006-data/train.data'
    narrow_hash = 'b9306efe0675a0e0c0c3bea9d64e4bccda856f35ebafe79ab5a87472ca5b9e2c'
    assert sha(narrow) == narrow_hash
    for name, wdl, decay, data, data_hash in [
        ('replica', .25, 32, recipe['data'], recipe['data_sha256']),
        ('teacher', 0., 32, recipe['data'], recipe['data_sha256']),
        ('wdl50', .50, 32, recipe['data'], recipe['data_sha256']),
        ('narrow', .25, 32, str(narrow.relative_to(ROOT)), narrow_hash),
        ('late', .25, 128, recipe['data'], recipe['data_sha256']),
    ]:
        config = dict(recipe, data=data, data_sha256=data_hash,
                      output=str((BASE / f'raw/a001-{name}-pilot').relative_to(ROOT)),
                      seed=2026111501, start_superbatch=1, end_superbatch=4,
                      resume=None, wdl=wdl, lr_drop_every=decay)
        write(BASE / f'a001-{name}-pilot.json', config)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
