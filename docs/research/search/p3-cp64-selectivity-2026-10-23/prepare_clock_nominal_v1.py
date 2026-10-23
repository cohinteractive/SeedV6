"""Freeze fresh H1 development openings and all clock inputs before execution."""
import importlib.util
import json
import subprocess
from pathlib import Path
from run_observation_v1 import HERE, ROOT, digest, write_new

spec=importlib.util.spec_from_file_location('baseline',ROOT/'tools/search-baseline.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
protocol=json.loads((HERE/'clock-nominal-v1.json').read_text());d=protocol['domain']
assert json.loads((HERE/'deeper-uci-qualification-v1.json').read_text())['status']=='passed'
binding=json.loads((HERE/'deeper-v1-binding.json').read_text())
runtime=HERE/'raw/runtime-v3'
for p,sha in binding['files'].items():
    if 'raw/runtime-v3/' in p.replace('\\','/'):assert digest(ROOT/p)==sha,p
book=Path('C:/projects/seed/java/seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn')
assert digest(book)=='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
tracked=subprocess.check_output(['git','ls-files','docs/research/search'],cwd=ROOT,text=True).splitlines()
exclusions=[]
for rel in tracked:
    if rel.endswith('.selection.json'):
        p=ROOT/rel;v=json.loads(p.read_text(encoding='utf-8-sig'))
        if 'positionKeys' in v and len(v['positionKeys'])==v.get('count'):
            exclusions.append(p)
prior=ROOT/'docs/research/search/p3-tt-effectiveness-2026-10-23/clock-development-exclusions.json'
exclusions.append(prior)
extra=HERE/'clock-development-exclusions-v1.json'
exclusions.append(extra)
opening=HERE/'clock-nominal-v1-openings.pgn'
b.select_openings(book,opening,d['pairs'],d['seed'],d['partition'],exclusions)
files=[HERE/'clock-nominal-v1.json',Path(__file__),HERE/'run_clock_nominal_v1.py',opening,
       opening.with_suffix('.selection.json'),extra,HERE/'deeper-uci-qualification-v1.json',
       ROOT/'tools/p3-selectivity-deeper-match.py',ROOT/'tools/search-baseline.py',ROOT/'tools/p3-tt-session.py',
       ROOT/'tools/tournament-process.py',ROOT/'tools/sr024-process-cost.py',ROOT/'tools/sr036-process-cost.py',
       ROOT/'build/research/tools/fastchess.exe',ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue']
files+=sorted(p for p in runtime.rglob('*') if p.is_file())
write_new(HERE/'clock-nominal-v1-binding.json',{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
    'files':{str(p.relative_to(ROOT)):digest(p) for p in files},
    'externalFiles':{str(book):digest(book),'C:/java/jdk-21/bin/java.exe':digest('C:/java/jdk-21/bin/java.exe')},
    'rawOutput':'raw/clock-nominal-v1','prospectiveProtocolSha256':digest(HERE/'clock-nominal-v1.json')})
print('Frozen32 fresh development pairs and exact inputs.')
