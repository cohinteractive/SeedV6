"""Freeze the prospective G7 development protocol; no games or owner writes."""
import importlib.util
import json
from pathlib import Path

BASE = Path('docs/research/brn-successor-cglhw')
def load(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def save(p, value):
    with p.open('x', encoding='utf-8', newline='\n') as f:
        json.dump(value, f, indent=2); f.write('\n')

def main():
    spec = importlib.util.spec_from_file_location('baseline', Path(__file__).with_name('search-baseline.py'))
    b = importlib.util.module_from_spec(spec); spec.loader.exec_module(b)
    prior = load(BASE/'wave6-development-openings.selection.json')
    assert b.digest(prior['source']) == prior['sourceSha256']
    book = BASE/'wave7-development-openings.pgn'
    exclusions = ['wave3-used-openings.selection.json', 'wave3-fair-control-openings.selection.json']
    exclusions += [f'wave{w}-development-openings.selection.json' for w in (4, 5, 6)]
    b.select_openings(prior['source'], book, 32, 2026102107, 0, [BASE/n for n in exclusions])
    openings = b.opening_prefixes(book, 32)
    models = load(BASE/'wave7-half-model-plan.json')['models']
    for m in models[1:]: m['name'] += '-g160-half'
    reference, q4, c4, bre = models
    protocols = []
    for label, selected, roots, nodes, millis in [
        ('degree-node', [q4, c4], openings[:16], 262144, 0),
        ('degree-clock', [q4, c4], openings[:16], 0, 200),
        ('bre-node', [bre, q4, c4], openings[16:24], 262144, 0),
        ('bre-clock', [bre, q4, c4], openings[16:24], 0, 200),
        ('reference-clock', [reference, q4, c4, bre], openings[24:32], 0, 200)]:
        plan = dict(id='wave7-'+label+'-v1', pairs=len(roots), seed=2026102107,
            maximumPlies=512, batchGames=12, batchSeconds=240, legalFallback=False,
            models=selected, nodes=nodes, moveMillis=millis, openingMoves=roots)
        path = BASE/('wave7-'+label+'-plan.json'); save(path, plan)
        protocols.append(dict(label=label, plan=path.as_posix(), sha256=b.digest(path),
                              games=2*len(roots)*(len(selected)-1)))
    save(BASE/'wave7-assessment-protocol.json', dict(id='wave7-development-assessment-v1',
        protocols=protocols, totalGames=sum(p['games'] for p in protocols), runtime='wave7-runtime.json',
        selection='Latest160 selected before G7 training; Q4 Best160, C4/BRE Best158 audited separately. Fixed consumed-panel results do not justify substituting Best. No Best match selected.',
        mapping='Common Policy4/QXRAY, material plus quarter residual, half learned change from Gen32 in all trained arms; unchanged from G6.',
        purpose='Adapted rank4 degree comparison, continued BRE at practical-depth equal nodes and time, and frozen reference at time.',
        stopping='Complete fixed budgets, except correctness/resource failure. No favorable-score stopping or budget extension. Any further protocol needs a separately recorded discriminating question.',
        statistics='Opening-pair descriptive and bounded intervals; roots reused between conditions, opening-family dependence unknown. Development only, no independent superiority claim.',
        resource='One game, one worker per engine, no ponder,64MiB TT,768MiB heap,ActiveProcessorCount1,BelowNormal;12games/240sec per launch.',
        confirmation='Partition1 remains reserved. No confirmation candidate selected.'))
    print('Frozen', sum(p['games'] for p in protocols), 'development games')

if __name__ == '__main__': main()
