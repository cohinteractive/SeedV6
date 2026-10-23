"""Freeze new development selections and stage splits without reading game outcomes."""
import importlib.util
import json
import subprocess
from pathlib import Path
from run_observation_v1 import HERE, ROOT, digest, write_new

spec=importlib.util.spec_from_file_location('baseline',ROOT/'tools/search-baseline.py');b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
assert json.loads((HERE/'flat-null-uci-qualification-v1.json').read_text())['status']=='passed'
assert json.loads((HERE/'flat-null-cutoff-audit-v1-summary.json').read_text())['status']=='passed'
plan=json.loads((HERE/'null-clock-development-v1.json').read_text());d=plan['domain']
runtime=HERE/'raw/runtime-v4';prior=json.loads((HERE/'flat-null-v1-binding.json').read_text())
for p,sha in prior['files'].items():assert digest(ROOT/p)==sha,p
book=Path('C:/projects/seed/java/seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn')
assert digest(book)=='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
tracked=subprocess.check_output(['git','ls-files','docs/research/search'],cwd=ROOT,text=True).splitlines();excluded=[]
for rel in tracked:
    if rel.endswith('.selection.json'):
        p=ROOT/rel;v=json.loads(p.read_text(encoding='utf-8-sig'))
        if 'positionKeys' in v and len(v['positionKeys'])==v.get('count'):excluded.append(p)
excluded.extend([HERE/'clock-development-exclusions-v1.json',ROOT/'docs/research/search/p3-tt-effectiveness-2026-10-23/clock-development-exclusions.json'])
allopen=HERE/'null-clock-development-v1-openings.pgn'
b.select_openings(book,allopen,64,d['seed'],d['partition'],excluded)
selection=json.loads(allopen.with_suffix('.selection.json').read_text());games=[]
with allopen.open(encoding='utf8') as stream:
    while (g:=b.chess.pgn.read_game(stream)) is not None:assert not g.errors;games.append(g)
assert len(games)==64
stageFiles=[]
for i,stage in enumerate(plan['stages']):
    output=HERE/(stage['name']+'-openings.pgn');chosen=games[32*i:32*(i+1)]
    with output.open('x',encoding='utf8',newline='\n') as stream:stream.write('\n\n'.join(str(g) for g in chosen)+'\n')
    receipt=output.with_suffix('.selection.json');keys=[' '.join(g.end().board().fen().split()[:4]) for g in chosen]
    assert keys==selection['positionKeys'][32*i:32*(i+1)]
    write_new(receipt,{'count':32,'positionKeys':keys,'parentSelectionSha256':digest(allopen.with_suffix('.selection.json')),
        'sourceSha256':selection['sourceSha256'],'outputSha256':digest(output),'partition':d['partition'],
        'seed':d['seed'],'selection':f'Frozen64 selection indices{32*i}..{32*(i+1)-1}; no reselection'})
    stageFiles.extend([output,receipt])
files=[HERE/'null-clock-development-v1.json',Path(__file__),HERE/'run_null_clock_v1.py',allopen,allopen.with_suffix('.selection.json'),
    HERE/'flat-null-uci-qualification-v1.json',HERE/'flat-null-cutoff-audit-v1-summary.json',
    ROOT/'tools/p3-selectivity-null-match.py',ROOT/'tools/p3-tt-session.py',ROOT/'tools/tournament-process.py',
    ROOT/'tools/search-baseline.py',ROOT/'tools/sr024-process-cost.py',ROOT/'tools/sr036-process-cost.py',
    ROOT/'build/research/tools/fastchess.exe',ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue']+stageFiles
files+=sorted(p for p in runtime.rglob('*') if p.is_file())
write_new(HERE/'null-clock-development-v1-binding.json',{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
    'files':{str(p.relative_to(ROOT)):digest(p) for p in files},'externalFiles':{str(book):digest(book),'C:/java/jdk-21/bin/java.exe':digest('C:/java/jdk-21/bin/java.exe')}})
print('Frozen64 fresh openings into two disjoint32-pair stages.')
