"""Freeze G8's fixed-head development assessment; no games or owner writes."""
import copy
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
    prior = load(BASE/'wave7-development-openings.selection.json')
    assert b.digest(prior['source']) == prior['sourceSha256']
    book = BASE/'wave8-development-openings.pgn'
    exclusions = ['wave3-used-openings.selection.json', 'wave3-fair-control-openings.selection.json']
    exclusions += [f'wave{w}-development-openings.selection.json' for w in (4, 5, 6, 7)]
    b.select_openings(prior['source'], book, 32, 2026102208, 0, [BASE/n for n in exclusions])
    openings = b.opening_prefixes(book, 32)
    models = load(BASE/'wave8-half-model-plan.json')['models']
    for m in models[1:]: m['name'] += '-g168-half'
    reference, cubic, quad, bre = models
    anchor = copy.deepcopy(load(BASE/'wave7-half-model-plan.json')['models'][1])
    anchor['name'] = 'W7-Q4-anchor-g160-half'
    protocols = []
    for label, selected, roots, nodes, millis in [
        ('degree-node', [quad, cubic], openings[:16], 262144, 0),
        ('degree-clock', [quad, cubic], openings[:16], 0, 200),
        ('anchor-node', [anchor, cubic], openings[16:24], 262144, 0),
        ('anchor-clock', [anchor, cubic], openings[16:24], 0, 200),
        ('bre-clock', [bre, quad, cubic], openings[16:24], 0, 200),
        ('reference-clock', [reference, quad, cubic, bre], openings[24:32], 0, 200)]:
        plan = dict(id='wave8-'+label+'-v1', pairs=len(roots), seed=2026102208,
            maximumPlies=512, batchGames=12, batchSeconds=240, legalFallback=False,
            models=selected, nodes=nodes, moveMillis=millis, openingMoves=roots)
        path = BASE/('wave8-'+label+'-plan.json'); save(path, plan)
        protocols.append(dict(label=label, plan=path.as_posix(), sha256=b.digest(path),
                              games=2*len(roots)*(len(selected)-1)))
    save(BASE/'wave8-assessment-protocol.json', dict(id='wave8-development-assessment-v1',
        protocols=protocols, totalGames=sum(p['games'] for p in protocols), runtime='wave8-runtime.json',
        selection='Latest168 prospectively specified before training. Head Best167 and BRE Best168 audited; no retrospective Best match substitution.',
        mapping='Common Policy4/QXRAY; material plus quarter residual, half learned change from Gen32. Raw/full/half fixed-panel diagnostics remain separate. No G8 calibration fit.',
        purpose='Fixed cubic versus fixed quadratic at nodes/time; exact zero-cubic Q4 anchor at nodes/time; continued BRE and frozen Gen32 at time.',
        stopping='Complete fixed budgets except correctness/resource failure. No favorable stopping or unchanged extensions.',
        statistics='Opening-pair descriptive and bounded intervals; roots reused across conditions, unknown family dependence and repeated candidate selection. Development only.',
        resource='One game, one worker per engine, no pondering,64MiB TT,768MiB heap,ActiveProcessorCount1,BelowNormal;12games/240sec per launch.',
        confirmation='Partition1 remains reserved. No confirmation candidate selected.',
        causalLimits='Exact anchor differs only by four learned cubic coefficients and cubic runtime cost. Fixed Q heads change the lower-degree function; neither control establishes shared-embedding interference. BRE has different historical data and optimizer.'))
    print('Frozen', sum(p['games'] for p in protocols), 'development games')

if __name__ == '__main__': main()
