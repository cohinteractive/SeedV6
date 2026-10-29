"""R035 root-grouped, symmetry-purged neural search corpus; no score filter."""
import json
from pathlib import Path
import sqlite3
import time
import chess
from baseline import ROOT, admit, sha
from compare_neural import replay
from prepare_objective import alias


def main():
    started = time.monotonic(); samples = [admit(initial=True)]; sampled = started
    def check():
        nonlocal sampled
        if time.monotonic()-sampled >= 5:
            samples.append(admit());sampled = time.monotonic()
        if time.monotonic()-started > 180:raise RuntimeError('Bounded extraction allocation exhausted')
    folder = ROOT/'docs/research/brn-relational-cglhw'
    out = folder/'raw/r035-data'
    assert not out.exists(), 'Preserve and inspect prior extraction publication'
    exclusions = {}; reserved = set()
    def pin(path):
        exclusions[str(path.relative_to(ROOT))] = sha(path)
    old = folder/'raw/r002-scale-data/validation.tsv';pin(old)
    assert sha(old) == '9e3ba6430ca575c00780bec1de5ef65971b59d3e5c9a30a4ed9d2edc65a59826'
    for line in old.read_text().splitlines():
        reserved.add(alias(chess.Board(line.split('\t')[0])));check()
    newer = folder/'raw/r016-data/selection.json';pin(newer)
    assert sha(newer) == 'ef3c9a40f55e432d1012dcc3c1af1e703fd2d49cdd0d5ba5307e2b051ed7b37d'
    reserved.update(row['alias'] for row in json.loads(newer.read_text())['state']['rows'])
    structural=folder/'r008-cases.json';pin(structural)
    assert sha(structural)=='271b3cba2b8e7803c8009ca338f7335584e0dba89e7f624a2014dfc8b389b6d4'
    for quartet in json.loads(structural.read_text()):
        reserved.update(alias(chess.Board(fen)) for fen in quartet['fens'])
    prior_games = 0
    for experiment,count in (('r001',16),('r001b',192),('r002-games',64),('r007-bt4-games',64),('r025',32),('r030',32)):
        path = folder/(experiment+'-plan.json');pin(path);plan=json.loads(path.read_text())
        opening_path=ROOT/plan['openings'];pin(opening_path)
        openings=json.loads(opening_path.read_text())
        if 'openings_sha256' in plan:assert sha(opening_path)==plan['openings_sha256']
        paths=sorted((ROOT/plan['output']).glob('**/game-*.json'));assert len(paths)==count,(experiment,len(paths),count)
        for path in paths:
            pin(path);game=json.loads(path.read_text());board=chess.Board()
            for move in openings[game['opening']]['moves'].split()+game['moves']:
                reserved.add(alias(board));board.push_uci(move);check()
            reserved.add(alias(board));prior_games+=1
    database=folder/'raw/r010-data/selection.sqlite';pin(database)
    assert sha(database)=='0a5c7afc24ed463146a0dfa8964e00eb24c208512d7b200d8f4ea48f61bb4972'
    connection=sqlite3.connect(database.resolve().as_uri()+'?mode=ro',uri=True)
    partitions={'train':{},'validation':{}};sources=[];dropped=dict(in_check=0,prior_alias=0,r010_alias=0,cross_partition_alias=0)
    try:
        for seed in (71,97):
            plan_path=folder/f'r035-{seed}-plan.json';plan=json.loads(plan_path.read_text())
            match=ROOT/plan['output'];binding=json.loads((match/'binding.json').read_text())
            assert binding['plan_sha256']==sha(plan_path)
            assert binding['runner_sha256']==sha(ROOT/'tools/brn-relational/compare_neural.py')
            for name,digest in binding['runtime'].items():assert sha(ROOT/plan['runtime']/name)==digest
            assert sha(ROOT/plan['openings'])==plan['openings_sha256']
            openings=json.loads((ROOT/plan['openings']).read_text());assert len(openings)==12
            records=json.loads((match/'results.json').read_text());assert len(records)==1
            record=records[0];directory=match/record['id']
            releases=list(directory.glob('release-*.json'));assert releases
            for release in releases:
                r=json.loads(release.read_text());assert r['remaining']==0 and not r['errors']
            for i,opening in enumerate(openings):
                partition='train' if i<8 else 'validation'
                for color in (True,False):
                    path=directory/f'game-{i:03d}-{int(color)}.json'
                    assert sha(path)==record['games'][path.name]
                    game=json.loads(path.read_text());assert game['opening']==i and game['left_color']==color
                    replay(game,opening,plan)
                    board=chess.Board()
                    for move in opening['moves'].split():board.push_uci(move)
                    source=dict(path=str(path.relative_to(ROOT)),sha256=sha(path),seed=seed,opening=i,partition=partition,available_roots=len(game['moves']))
                    sources.append(source)
                    for ply,move in enumerate(game['moves']):
                        check()
                        if board.is_check():dropped['in_check']+=1
                        else:
                            key=alias(board)
                            if key in reserved:dropped['prior_alias']+=1
                            elif connection.execute('SELECT 1 FROM rows WHERE alias=?',(key,)).fetchone():dropped['r010_alias']+=1
                            else:
                                origin=dict(game=str(path.relative_to(ROOT)),seed=seed,opening=i,ply=ply)
                                positions=partitions[partition]
                                if key in positions:positions[key]['sources'].append(origin)
                                else:positions[key]=dict(alias=key,fen=board.fen(),pieces=len(board.piece_map()),sources=[origin])
                        board.push_uci(move)
    finally:connection.close()
    for key in set(partitions['train'])&set(partitions['validation']):
        partitions['train'].pop(key);dropped['cross_partition_alias']+=1
    assert not (set(partitions['train'])&set(partitions['validation']))
    check();out.mkdir()
    datasets={}
    for name,positions in partitions.items():
        ordered=[positions[key] for key in sorted(positions)]
        path=out/(name+'-positions.json')
        with path.open('x') as stream:json.dump(ordered,stream,indent=2);stream.write('\n')
        data=out/(name+'-unlabeled.tsv')
        with data.open('x') as stream:
            for position in ordered:stream.write(position['fen']+'\t0.5\n')
        roots={(s['seed'],s['opening']) for position in ordered for s in position['sources']}
        datasets[name]=dict(rows=len(ordered),roots=len(roots),positions=str(path.relative_to(ROOT)),positions_sha256=sha(path),
                            unlabeled=str(data.relative_to(ROOT)),unlabeled_sha256=sha(data),placeholder_target=.5)
    result=dict(selector_sha256=sha(Path(__file__)),alias_helper_sha256=sha(ROOT/'tools/brn-relational/prepare_objective.py'),
                sources=sources,exclusion_files=exclusions,prior_games=prior_games,reserved_aliases=len(reserved),dropped=dropped,datasets=datasets,
                seconds=time.monotonic()-started,resources=samples,
                training_allocation_justified=datasets['train']['rows']>=1000 and datasets['validation']['rows']>=500 and datasets['train']['roots']>=12 and datasets['validation']['roots']>=6,
                scope='Fresh root-grouped corpus;all old heldouts remain excluded;unlabeled placeholders are not training targets;no score/outcome filtering or strength claim.')
    with (folder/'r035-corpus-selection.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ('sources','exclusion_files')},indent=2))


if __name__ == '__main__':
    main()
