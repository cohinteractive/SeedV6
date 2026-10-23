"""Freeze exact accepted baseline, current experimental binary and fresh development openings."""
import json
from pathlib import Path
import shutil
import subprocess
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module

def main():
    assert json.loads((HERE/'clock-qualification-v2.json').read_text())['status']=='passed'
    assert json.loads((HERE/'clock-cost-v1-summary.json').read_text())['status']=='passed'
    assert json.loads((HERE/'build75-runtime-summary.json').read_text())['exitCode']==0
    assert not subprocess.check_output(['git','status','--porcelain','--untracked-files=no'],cwd=ROOT,text=True).strip(), 'Commit runtime changes before freezing'
    protocol=json.loads((HERE/'clock-development-v1.json').read_text());domain=protocol['domain']
    candidate=HERE/'raw/runtime-candidate-v1';baseline=HERE/'raw/runtime-production-v1'
    shutil.copytree(ROOT/'build/research/runtime',candidate)
    shutil.copytree(candidate,baseline,ignore=shutil.ignore_patterns('engine-core.jar'))
    native=ROOT/'releases/candidates/windows-x64/candidate-20261023T195552-c00e3315/SeedV6-UCI/app/engine-core.jar'
    assert digest(native)=='facac0db65c7f97a120083174ea4de20ef210daf3252a79a3fd5eb2cac99e6f7'
    shutil.copy2(native,baseline/'lib/engine-core.jar')
    m=module('match',ROOT/'tools/p3-time-match.py');b=m.b
    source=Path('C:/projects/seed/java/seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn')
    assert digest(source)=='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
    book=HERE/'raw/source-openings-v1.pgn';shutil.copy2(source,book)
    exclusions=[]
    for rel in subprocess.check_output(['git','ls-files','docs/research/search'],cwd=ROOT,text=True).splitlines():
        if rel.endswith('.selection.json'):
            path=ROOT/rel;v=json.loads(path.read_text(encoding='utf-8-sig'))
            if 'positionKeys' in v and len(v['positionKeys'])==v.get('count'):exclusions.append(path)
    keys=set();corpora={}
    for rel in ('docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv',
                'docs/research/search/p3-sr002-execution-2026-10-19/confirmation-corpus.tsv'):
        path=ROOT/rel;corpora[rel]=digest(path)
        for line in path.read_text().splitlines():
            if not line or line.startswith('#'):continue
            identity,fen,moves=line.split('\t');board=b.chess.Board(fen)
            for ply,uci in enumerate(moves.split(),1):
                move=b.chess.Move.from_uci(uci);assert move in board.legal_moves;board.push(move)
                if ply>=16:keys.add(' '.join(board.fen().split()[:4]))
    extra=HERE/'clock-development-exclusions-v1.json'
    write_new(extra,{'count':len(keys),'positionKeys':sorted(keys),'corpora':corpora,'rule':'All32 trajectory histories, every prefix ply>=16'})
    exclusions.append(extra)
    opening=HERE/'clock-development-v1-openings.pgn'
    b.select_openings(book,opening,domain['pairsPerComparison'],domain['seed'],domain['partition'],exclusions)
    files=[HERE/'clock-development-v1.json',Path(__file__),HERE/'run_clock_development_v1.py',HERE/'run_economics_v1.py',
        HERE/'economics-v1-binding.json',opening,opening.with_suffix('.selection.json'),extra,book,
        ROOT/'tools/p3-time-match.py',ROOT/'tools/p3-selectivity-match.py',ROOT/'tools/search-baseline.py',ROOT/'tools/p3-tt-session.py',
        ROOT/'tools/tournament-process.py',ROOT/'tools/sr024-process-cost.py',ROOT/'tools/sr036-process-cost.py',
        ROOT/'build/research/tools/fastchess.exe',ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue']
    for runtime in (baseline,candidate):files+=sorted(p for p in runtime.rglob('*') if p.is_file())
    write_new(HERE/'clock-development-v1-binding.json',{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'files':{str(p.relative_to(ROOT)):digest(p) for p in files},
        'externalFiles':{'C:/java/jdk-21/bin/java.exe':digest(Path('C:/java/jdk-21/bin/java.exe'))},
        'baselineSource':'Accepted native image core, preserved build74 provenance; same current launcher/warmup',
        'candidateSource':'Current committed experimental source; production defaults unchanged',
        'openingSourceOriginal':str(source),'openingSourceSha256':digest(source)})
    print('Frozen accepted native baseline, experimental candidate and32 fresh development opening pairs.')

if __name__=='__main__':main()
