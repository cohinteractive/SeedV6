"""Freeze one selected complete profile and disjoint independent inputs after development."""
import json
from pathlib import Path
import subprocess
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module


def main():
    assert not (HERE/'confirmation-v1-binding.json').exists(), 'Never replace an existing confirmation binding'
    assert not any(HERE.glob('sr034-confirmation-v1*')) and not any((HERE/'raw').glob('sr034-confirmation-v1*')), 'Inspect partial preparation; do not overwrite it'
    plan = json.loads((HERE/'confirmation-v1.json').read_text()); d = plan['domain']
    selected = plan['selectedProfile']; assert selected in ('unstable2','boundary2')
    decision = json.loads((HERE/'confirmation-selection-v1.json').read_text()); assert decision['selectedProfile'] == selected
    for name, sha in decision['evidence'].items(): assert digest(HERE/name) == sha, name
    checked = json.loads((HERE/'confirmation-statistics-validation-v1.json').read_text()); assert checked['status'] == 'passed'
    for name, sha in checked['files'].items(): assert digest(HERE/name) == sha, name
    deliveries = []
    for profile in ('unstable2','boundary2'):
        name = 'spending-development-'+profile+'-v1'
        result = json.loads((HERE/(name+'-summary.json')).read_text())
        assert result['status'] in ('passed','resource-unqualified'), 'Unresolved correctness/clock result'
        if profile == selected: assert result['status'] == 'passed' and result['results']['statistics']['score'] >= .55
        path = HERE/(name+'-post-delivery-summary.json'); delivery = json.loads(path.read_text())
        if delivery['status'] == 'passed' and delivery['criteriaPassed']:
            deliveries.append((delivery['hostInterval']['end']['utc'], {'path':path.name,'sha256':digest(path)}))
    assert deliveries
    prior = json.loads((HERE/'spending-development-v1-binding.json').read_text())
    files = prior['files'].copy(); external = prior['externalFiles'].copy()
    def add(path):
        key = str(Path(path).relative_to(ROOT)); sha = digest(path)
        if key in files: assert files[key] == sha, key
        files[key] = sha
    for p, sha in files.items(): assert digest(ROOT/p) == sha, p
    for p, sha in external.items(): assert digest(Path(p)) == sha, p
    for name in ('confirmation-v1.json','confirmation-selection-v1.json','prepare_confirmation_v1.py','run_confirmation_block_v1.py',
                 'audit_confirmation_clock_v1.py','analyze_confirmation_v1.py','validate_confirmation_v1.py',
                 'confirmation-statistics-validation-v1.json','spending-development-v1-binding.json','spending-frozen-preflight-v1-summary.json',
                 'clock-development-exclusions-v1.json','clock-development-half-v1-compact.json','clock-development-stable3-half-v1-compact.json'):
        add(HERE/name)
    for profile in ('unstable2','boundary2'):
        for suffix in ('-summary.json','-clock-analysis-v1.json','-post-delivery-summary.json'):
            add(HERE/('spending-development-'+profile+'-v1'+suffix))
    book = HERE/'raw/source-openings-v1.pgn'
    assert digest(book) == 'eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'; add(book)
    tracked = subprocess.check_output(['git','ls-files','docs/research/search'],cwd=ROOT,text=True).splitlines()
    possible = {ROOT/p for p in tracked if p.endswith('.selection.json')} | set(HERE.glob('*.selection.json'))
    excluded = []
    for path in sorted(possible):
        value = json.loads(path.read_text(encoding='utf-8-sig'))
        if 'positionKeys' in value and len(value['positionKeys']) == value.get('count'): excluded.append(path)
    excluded += [HERE/'clock-development-exclusions-v1.json',
        ROOT/'docs/research/search/p3-cp64-selectivity-2026-10-23/clock-development-exclusions-v1.json',
        ROOT/'docs/research/search/p3-tt-effectiveness-2026-10-23/clock-development-exclusions.json']
    for path in excluded: add(path)
    b = module('baseline', ROOT/'tools/search-baseline.py')
    prefix = 'sr034-confirmation-v1'; opening = HERE/(prefix+'-openings.pgn')
    b.select_openings(book, opening, d['pairs'], d['seed'], 1, excluded)
    selection = json.loads(opening.with_suffix('.selection.json').read_text()); games = []
    with opening.open(encoding='utf-8') as stream:
        while (game := b.chess.pgn.read_game(stream)) is not None:
            assert not game.errors; games.append(game)
    assert len(games) == 128; add(opening); add(opening.with_suffix('.selection.json')); blocks = []
    for block in range(1,5):
        name = f'{prefix}-b{block:02}'; target = HERE/(name+'-openings.pgn'); chosen = games[(block-1)*32:block*32]
        with target.open('x',encoding='utf-8',newline='\n') as stream: stream.write('\n\n'.join(str(game) for game in chosen)+'\n')
        keys = [' '.join(game.end().board().fen().split()[:4]) for game in chosen]
        assert keys == selection['positionKeys'][(block-1)*32:block*32]
        receipt = target.with_suffix('.selection.json')
        write_new(receipt, {'count':32,'positionKeys':keys,'parentSelectionSha256':digest(opening.with_suffix('.selection.json')),
            'sourceSha256':selection['sourceSha256'],'outputSha256':digest(target),'partition':1,'seed':d['seed'],
            'selection':f'Frozen128 selection indices{(block-1)*32}..{block*32-1}; no reselection'})
        add(target); add(receipt); blocks.append(name)
    for p, sha in files.items(): assert digest(ROOT/p) == sha, p
    write_new(HERE/'confirmation-v1-binding.json', {'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'files':files,'externalFiles':external,'admitted':['finalist'],'stages':{'finalist':{
            'prefix':prefix,'blocks':blocks,'candidate':selected,'flags':[3407871,3407871],
            'runtime':'raw/runtime-candidate-v2','baselineRuntime':'raw/runtime-production-v2','seed':d['seed'],
            'firstPreDelivery':max(deliveries,key=lambda x:x[0])[1]}},
        'baseline':'Exact accepted native build74 core+UCI; same launcher, warmup, CP64 and fixed resources',
        'sampling':'One finalist, fresh128 paired openings, no development reuse, one final look'})
    print(json.dumps({'status':'prepared','candidate':selected,'pairs':128,'blocks':4}))


if __name__ == '__main__': main()
