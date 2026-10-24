"""Freeze fresh disjoint openings and all executable/prerequisite identities once."""
import json
from pathlib import Path
import subprocess
import sys
from verify_inputs import HERE, ROOT, OLD, audit, digest, write_new
from run_diagnostic_v1 import module, roots


def main():
    assert not (HERE/'confirmation-v1-binding.json').exists()
    assert not list(HERE.glob('boundary2-fresh-v1*')) and not list((HERE/'raw').glob('boundary2-fresh-v1*'))
    audit(); plan=json.loads((HERE/'confirmation-v1.json').read_text()); assert plan['domain']['pairs']==192
    for name in ('game-diagnostic-v1-a01-completion.json','game-diagnostic-v1-a01-event-audit.json',
                 'validation-v3-a01-summary.json','validation-v3-a01-provenance.json','confirmation-v1-math-validation.json'):
        assert json.loads((HERE/name).read_text())['status']=='passed',name
    delivery=HERE/'delivery-qualification-v1-a01-summary.json'; value=json.loads(delivery.read_text())
    assert value['status']=='passed' and value['criteriaPassed']
    check=json.loads((HERE/'game-diagnostic-v1-a01-fresh-clock-validation.json').read_text())
    assert check['clockReliabilityPassed'] and check['completeClockPassed']
    math_check=json.loads((HERE/'confirmation-v1-math-validation.json').read_text())
    assert math_check['analyzerSha256']==digest(HERE/'analyze_fresh_v1.py')
    tested=json.loads((HERE/'validation-v3-a01-provenance.json').read_text())
    assert digest(ROOT/tested['fixture'])==tested['fixtureSha256']
    for path,sha in value['evidence'].items(): assert digest(HERE/path)==sha
    book=OLD/'raw/source-openings-v1.pgn'
    assert digest(book)=='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
    b=module('baseline',ROOT/'tools/search-baseline.py')
    positions=set()
    for _,position,_ in roots(b.chess):
        tokens=position.split(); board=b.chess.Board() if tokens[0]=='startpos' else b.chess.Board(' '.join(tokens[1:7]))
        positions.add(' '.join(board.fen().split()[:4]))
        moves=tokens[tokens.index('moves')+1:] if 'moves' in tokens else []
        for move in moves:
            board.push_uci(move); positions.add(' '.join(board.fen().split()[:4]))
    extra=HERE/'diagnostic-history-exclusions-v1.json'
    write_new(extra,{'count':len(positions),'positionKeys':sorted(positions),
        'purpose':'All prefixes and roots of the14 diagnostic histories; no historical game results read.'})
    tracked=subprocess.check_output(['git','ls-files','docs/research/search'],cwd=ROOT,text=True).splitlines()
    possible={ROOT/p for p in tracked if p.endswith('.selection.json')}
    excluded=[]
    for path in sorted(possible):
        previous=json.loads(path.read_text(encoding='utf-8-sig'))
        if 'positionKeys' in previous and len(previous['positionKeys'])==previous.get('count'): excluded.append(path)
    excluded += [extra,OLD/'clock-development-exclusions-v1.json',
        ROOT/'docs/research/search/p3-cp64-selectivity-2026-10-23/clock-development-exclusions-v1.json',
        ROOT/'docs/research/search/p3-tt-effectiveness-2026-10-23/clock-development-exclusions.json']
    opening=HERE/'boundary2-fresh-v1-openings.pgn'
    b.select_openings(book,opening,192,plan['domain']['seed'],1,excluded)
    selection=json.loads(opening.with_suffix('.selection.json').read_text()); games=[]
    with opening.open(encoding='utf8') as stream:
        while (game:=b.chess.pgn.read_game(stream)) is not None:
            assert not game.errors; games.append(game)
    assert len(games)==192 and len(set(selection['positionKeys']))==192
    files=[book,opening,opening.with_suffix('.selection.json'),*excluded]
    blocks=[]
    for block in range(1,7):
        name=f'boundary2-fresh-v1-b{block:02}'; blocks.append(name); chosen=games[(block-1)*32:block*32]
        target=HERE/(name+'-openings.pgn')
        with target.open('x',encoding='utf8',newline='\n') as stream: stream.write('\n\n'.join(str(g) for g in chosen)+'\n')
        keys=[' '.join(g.end().board().fen().split()[:4]) for g in chosen]
        assert keys==selection['positionKeys'][(block-1)*32:block*32]
        receipt=target.with_suffix('.selection.json')
        write_new(receipt,{'count':32,'positionKeys':keys,'parentSelectionSha256':digest(opening.with_suffix('.selection.json')),
            'sourceSha256':selection['sourceSha256'],'outputSha256':digest(target),'partition':1,'seed':plan['domain']['seed'],
            'selection':f'Frozen192 indices{(block-1)*32}..{block*32-1}; no reselection'})
        files += [target,receipt]
    for name in ('confirmation-v1.json','prepare_fresh_v1.py','run_fresh_block_v1.py','study_common_v1.py','audit_fresh_clock_v1.py',
        'analyze_fresh_v1.py','run_delivery_v1.py','run_persistent_v2.py','run_persistent_v1.py','run_diagnostic_v1.py',
        'analyze_diagnostic_v1.py','verify_inputs.py','confirmation-v1-math-validation.json',
        'game-diagnostic-v1-a01-completion.json','game-diagnostic-v1-a01-event-audit.json',
        'game-diagnostic-v1-a01-fresh-clock-validation.json','validation-v3-a01-summary.json','validation-v3-a01-provenance.json',
        'run_validation_v3.py','delivery-qualification-v1-a01-summary.json'):
        files.append(HERE/name)
    files += [ROOT/tested['fixture'],OLD/'confirmation-v1-binding.json',OLD/'audit_confirmation_clock_v1.py',
              OLD/'run_trajectory_v1.py',OLD/'fastchess-clock-source-v1.json',Path(sys.executable),Path(sys._base_executable),
              Path(b.chess.__file__),Path(b.chess.pgn.__file__),Path('C:/java/jdk-21/bin/jcmd.exe')]
    historical=json.loads((OLD/'confirmation-v1-binding.json').read_text())
    bound={str((ROOT/p).resolve()):sha for p,sha in historical['files'].items()}
    bound.update({str(Path(p).resolve()):sha for p,sha in historical['externalFiles'].items()})
    diagnostic=json.loads((HERE/'game-diagnostic-v1-a01-binding.json').read_text())
    for path,sha in diagnostic['files'].items():
        if path.startswith('C:\\java\\') or path.startswith('C:/java/'): bound[str(Path(path).resolve())]=sha
    for path in files: bound[str(path.resolve())]=digest(path)
    for path,sha in bound.items(): assert digest(path)==sha,path
    inventories=diagnostic['runtimeInventory']
    for runtime,paths in inventories.items():
        assert sorted(str(p.resolve()) for p in (OLD/'raw'/runtime).rglob('*') if p.is_file())==paths
    write_new(HERE/'confirmation-v1-binding.json',{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'files':bound,'runtimeInventory':inventories,'blocks':blocks,
        'firstDelivery':{'path':delivery.name,'sha256':digest(delivery)},
        'scope':'New separate192-pair confirmation. Historical unqualified scores unopened. Exact frozen actors; no engine rebuild or JFR bridge.',
        'selection':{'availableDistinctPositions':selection['availableDistinctPositions'],'excludedDistinctPositions':selection['excludedDistinctPositions'],
            'count':192,'sourceSha256':selection['sourceSha256']}})
    print(json.dumps({'status':'frozen','pairs':192,'blocks':6,'boundFiles':len(bound),
        'availableDistinctPositions':selection['availableDistinctPositions'],'excludedDistinctPositions':selection['excludedDistinctPositions']}))


if __name__=='__main__': main()
