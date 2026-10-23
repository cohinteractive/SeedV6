"""Freeze admitted finalists and disjoint independent inputs before any games."""
import json,subprocess
from pathlib import Path
from run_observation_v1 import HERE,ROOT,digest,write_new
from run_clock_v1 import module

def main():
    plan=json.loads((HERE/'confirmation-v1.json').read_text());d=plan['domain']
    checked=json.loads((HERE/'confirmation-statistics-validation-v1.json').read_text());assert checked['status']=='passed'
    for name,sha in checked['files'].items():assert digest(HERE/name)==sha,name
    removal=json.loads((HERE/'removal-clock-v2-summary.json').read_text())
    witness=json.loads((HERE/'root-witness-v1-assessment.json').read_text())
    null_eligible=witness['decision']=='eligible_for_confirmation' and not witness['correctnessBlocker']
    for name in ['null-clock-primary-v2','null-clock-increment-v2']:
        result=json.loads((HERE/(name+'-summary.json')).read_text())
        assert result['status']=='passed' and result['results']['statistics']['score']>=.55
    admitted=['null'] if null_eligible else []
    if removal['status']=='passed' and removal['results']['statistics']['score']>=.55:admitted.append('removal')
    assert admitted,'No eligible finalist; do not create a confirmation study'
    if 'null' in admitted:
        for name in ['flat-null-validation-v3-summary.json','flat-null-composition-v2-summary.json']:
            validation=json.loads((HERE/name).read_text())
            assert validation['exitCode']==0 and validation['totals']['failures']==validation['totals']['errors']==0
    b=module('baseline',ROOT/'tools/search-baseline.py')
    book=Path('C:/projects/seed/java/seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn')
    assert digest(book)=='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
    files={};external={str(book):digest(book)};stages={}
    def add(path):files[str(Path(path).relative_to(ROOT))]=digest(path)
    for name in ['confirmation-v1.json','prepare_confirmation_v1.py','run_confirmation_block_v1.py','analyze_confirmation_v1.py',
                 'validate_confirmation_v1.py','confirmation-statistics-validation-v1.json',
                 'run_observation_v1.py','run_clock_v1.py','root-witness-v1-assessment.json','root-witness-v1-summary.json',
                 'root-witness-v1.json','root-witness-v1-binding.json','root-witness-d7-v1-execution.json','root-witness-d9-v1-execution.json',
                 'flat-null-validation-v3-summary.json','flat-null-composition-v2-summary.json',
                 'removal-clock-v2-summary.json','null-clock-primary-v2-summary.json','null-clock-increment-v2-summary.json']:
        add(HERE/name)
    for name in ['p3-selectivity-null-match.py','p3-selectivity-match.py','p3-tt-session.py','tournament-process.py',
                 'search-baseline.py','sr024-process-cost.py','sr036-process-cost.py']:add(ROOT/'tools'/name)
    for hypothesis in admitted:
        spec=plan['hypotheses'][hypothesis];source=HERE/spec['sourceBinding'];prior=json.loads(source.read_text())
        add(source)
        for p,sha in prior['files'].items():
            assert digest(ROOT/p)==sha,p
            if p in files:assert files[p]==sha,p
            files[p]=sha
        for p,sha in prior['externalFiles'].items():assert digest(p)==sha,p;external[p]=sha
        tracked=subprocess.check_output(['git','ls-files','docs/research/search'],cwd=ROOT,text=True).splitlines()
        possible={ROOT/p for p in tracked if p.endswith('.selection.json')}|set(HERE.glob('*.selection.json'))
        excluded=[]
        for path in sorted(possible):
            value=json.loads(path.read_text(encoding='utf-8-sig'))
            if 'positionKeys' in value and len(value['positionKeys'])==value.get('count'):excluded.append(path)
        excluded += [HERE/'clock-development-exclusions-v1.json',ROOT/'docs/research/search/p3-tt-effectiveness-2026-10-23/clock-development-exclusions.json']
        prefix=hypothesis+'-confirmation-v1';opening=HERE/(prefix+'-openings.pgn')
        b.select_openings(book,opening,128,spec['seed'],1,excluded)
        selection=json.loads(opening.with_suffix('.selection.json').read_text());games=[]
        with opening.open(encoding='utf8') as stream:
            while (game:=b.chess.pgn.read_game(stream)) is not None:assert not game.errors;games.append(game)
        assert len(games)==128;add(opening);add(opening.with_suffix('.selection.json'));blocks=[]
        for block in range(1,5):
            name=f'{prefix}-b{block:02}';target=HERE/(name+'-openings.pgn');chosen=games[(block-1)*32:block*32]
            with target.open('x',encoding='utf8',newline='\n') as stream:stream.write('\n\n'.join(str(game) for game in chosen)+'\n')
            keys=[' '.join(game.end().board().fen().split()[:4]) for game in chosen]
            assert keys==selection['positionKeys'][(block-1)*32:block*32]
            receipt=target.with_suffix('.selection.json')
            write_new(receipt,{'count':32,'positionKeys':keys,'parentSelectionSha256':digest(opening.with_suffix('.selection.json')),
                'sourceSha256':selection['sourceSha256'],'outputSha256':digest(target),'partition':1,'seed':spec['seed'],
                'selection':f'Frozen128 selection indices{(block-1)*32}..{block*32-1}; no reselection'})
            add(target);add(receipt);blocks.append(name)
        stages[hypothesis]={'prefix':prefix,'blocks':blocks,'runtime':spec['runtime'],'candidate':spec['candidate'],
            'flags':spec['flags'],'resourceModule':spec['resourceModule'],'seed':spec['seed']}
    for p,sha in files.items():assert digest(ROOT/p)==sha,p
    write_new(HERE/'confirmation-v1-binding.json',{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'files':files,'externalFiles':external,'admitted':admitted,'stages':stages,
        'unusedAllocations':[h+': admission not met; alpha not transferred' for h in plan['hypotheses'] if h not in admitted]})
    print(json.dumps({'status':'prepared','admitted':admitted,'pairsEach':128,'blocksEach':4}))

if __name__=='__main__':main()
