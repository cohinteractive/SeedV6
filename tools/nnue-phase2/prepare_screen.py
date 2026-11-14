"""Freeze fresh screen/confirmation partitions and the validated research runtime."""
import json
import argparse
from pathlib import Path
import shutil
import subprocess
import sys
from bootstrap import ROOT, BASE, PRIOR, write, sha
import codec
import compute_gate


def main():
    p=argparse.ArgumentParser();p.add_argument('--runtime-only',action='store_true');p.add_argument('--runtime-version',type=int,default=1);a=p.parse_args()
    if a.runtime_version<1:raise ValueError('Invalid runtime version')
    compute_gate.require();source=json.loads((PRIOR/'confirmation-openings.selection.json').read_text())
    pool=BASE/'opening-pool.json';selection=BASE/'opening-pool.selection.json'
    if not a.runtime_only:
        if pool.exists() or selection.exists():raise ValueError('Opening pool already exists; inspect/reuse instead of reselecting')
        subprocess.run([sys.executable,'-B',str(ROOT/'tools/conventional-nnue/prepare_confirmation_openings.py'),
            '--source',source['source'],'--sha256',source['source_sha256'],'--output',str(pool),'--manifest',str(selection),
            '--count','416','--seed','2026111502'],check=True,timeout=300)
    all_openings=json.loads(pool.read_text());meta=json.loads(selection.read_text());keys=meta['positionKeys']
    assert sha(pool)==meta['output_sha256']
    for name,start,end in [('screen',0,32),('confirmation',32,288),('robustness',288,416)]:
        if a.runtime_only:
            assert json.loads((BASE/f'{name}-openings.json').read_text())==all_openings[start:end]
            continue
        write(BASE/f'{name}-openings.json',all_openings[start:end])
        write(BASE/f'{name}-openings.selection.json',dict(pool_sha256=sha(pool),selection_sha256=sha(selection),
              range=[start,end],positionKeys=keys[start:end],purpose=name,
              policy='Preselected disjoint partitions; no outcome/score filter; confirmation and robustness unplayed until separate prospective selection'))
    assert len(set(keys))==416
    runtime=BASE/f'raw/frozen-runtime-v{a.runtime_version}'
    if runtime.exists():raise ValueError('Do not overwrite frozen runtime')
    shutil.copytree(ROOT/'build/research/runtime',runtime)
    files={str(p.relative_to(runtime)).replace('\\','/'):sha(p) for p in runtime.rglob('*') if p.is_file()}
    incumbent=BASE/'raw/frozen-wdl25.cnnue'
    if not incumbent.exists():shutil.copyfile(ROOT/'build-assets/production/conventional-wdl25.cnnue',incumbent)
    expected=json.loads((BASE/'baseline.json').read_text())['protected_sha256']['build-assets/production/conventional-wdl25.cnnue']
    assert sha(incumbent)==expected
    write(BASE/f'frozen-runtime-v{a.runtime_version}.json',dict(path=str(runtime),files=files,incumbent=str(incumbent),incumbent_sha256=sha(incumbent),
        source_revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        source_files={str(p.relative_to(ROOT)).replace('\\','/'):sha(p) for p in
          list((ROOT/'research/src/main/java/com/ohinteractive/seedv6/core/nnue/conventional').glob('Phase2*.java'))+
          [ROOT/'src/researchLauncher/java/com/ohinteractive/seedv6/integration/NnueStrengthUciMain.java']},
        limitation='Runtime/source hashes define tested uncommitted research bytes; HEAD alone is not their identity'))
    print(json.dumps(dict(openings=416,screen_pairs=32,reserved_confirmation_pairs=256,reserved_robustness_pairs=128,runtime_files=len(files))))


if __name__=='__main__':main()
