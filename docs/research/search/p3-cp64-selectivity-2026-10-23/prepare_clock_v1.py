"""Freeze fresh H1 development openings and all clock inputs before execution."""
import importlib.util
import json
import subprocess
from pathlib import Path
from run_observation_v1 import HERE, ROOT, digest, write_new

spec=importlib.util.spec_from_file_location('baseline',ROOT/'tools/search-baseline.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
protocol=json.loads((HERE/'clock-development-v1.json').read_text());d=protocol['domain']
assert json.loads((HERE/'uci-qualification-v1.json').read_text())['status']=='passed'
binding=json.loads((HERE/'separation-v1-binding.json').read_text())
runtime=HERE/'raw/runtime-v2'
for p,sha in binding['files'].items():
    if 'raw/runtime-v2/' in p.replace('\\','/'):assert digest(ROOT/p)==sha,p
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
keys=set();corpora={}
for rel in ('docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv',
            'docs/research/search/p3-sr002-execution-2026-10-19/confirmation-corpus.tsv',
            'docs/research/search/sr006-phase2-2026-10-10/played-corpus.tsv'):
    p=ROOT/rel;corpora[rel]=digest(p)
    for line in p.read_text().splitlines()[:12]:
        identity,fen,moves=line.split('\t');board=b.chess.Board(fen)
        for ply,uci in enumerate(moves.split(),1):
            move=b.chess.Move.from_uci(uci);assert move in board.legal_moves;board.push(move)
            if ply>=16:keys.add(' '.join(board.fen().split()[:4]))
extra=HERE/'clock-development-exclusions-v1.json'
write_new(extra,{'count':len(keys),'positionKeys':sorted(keys),'corpora':corpora,
    'rule':'first12 roots per current corpus, every legal prefix at ply>=16'})
exclusions.append(extra)
opening=HERE/'clock-development-v1-openings.pgn'
b.select_openings(book,opening,d['pairs'],d['seed'],d['partition'],exclusions)
files=[HERE/'clock-development-v1.json',Path(__file__),HERE/'run_clock_v1.py',opening,
       opening.with_suffix('.selection.json'),extra,HERE/'uci-qualification-v1.json',
       ROOT/'tools/p3-selectivity-match.py',ROOT/'tools/search-baseline.py',ROOT/'tools/p3-tt-session.py',
       ROOT/'tools/tournament-process.py',ROOT/'tools/sr024-process-cost.py',ROOT/'tools/sr036-process-cost.py',
       ROOT/'build/research/tools/fastchess.exe',ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue']
files+=sorted(p for p in runtime.rglob('*') if p.is_file())
write_new(HERE/'clock-development-v1-binding.json',{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
    'files':{str(p.relative_to(ROOT)):digest(p) for p in files},
    'externalFiles':{str(book):digest(book),'C:/java/jdk-21/bin/java.exe':digest('C:/java/jdk-21/bin/java.exe')},
    'rawOutput':'raw/clock-development-v1','prospectiveProtocolSha256':digest(HERE/'clock-development-v1.json')})
print('Frozen32 fresh development pairs and exact inputs.')
