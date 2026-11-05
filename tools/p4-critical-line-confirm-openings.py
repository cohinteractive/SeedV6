"""CLV held-out input-only selection; never reads historical result/score files.

Exclude every tracked Search PGN fixture and root/corpus TSV, selection keys,
the entire development-source legal16 pool, and current CLV development roots.
Only explicitly named opening fixtures are read in frozen clock-study directories.
"""
import hashlib
import importlib.util
import json
from pathlib import Path
import random
import subprocess
import chess
import chess.pgn

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/research/search/p4-critical-line'
spec=importlib.util.spec_from_file_location('clv_input_support',ROOT/'tools/p4-tactical-openings.py')
s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)


def key(board):return ' '.join(board.fen().split()[:4])


if __name__=='__main__':
    target=OUT/'confirmation-v1-openings.pgn';selection=target.with_suffix('.selection.json')
    audit=OUT/'confirmation-freshness-v1.json'
    if any(p.exists() for p in (target,selection,audit)):raise FileExistsError('Frozen selection already exists')
    book_manifest=json.loads((OUT/'confirmation-book-v1-manifest.json').read_text())
    book=ROOT/book_manifest['path']
    assert s.digest(s.OLD)==s.OLD_SHA and s.digest(book)==book_manifest['sha256']
    names=subprocess.check_output(['git','ls-files','docs/research/search'],cwd=ROOT,text=True).splitlines()
    excluded=set();receipts={};counts={};resolved=[]
    # The tracked PGNs are intentional input exceptions, not ignored tournament output.
    for name in names:
        p=ROOT/name
        if p.suffix=='.pgn':
            if 'boundary2' in name.lower() and not p.name.endswith('-openings.pgn'):
                raise ValueError('Unexpected frozen clock input: '+name)
            n=0
            with p.open(encoding='utf-8-sig') as f:
                while (game:=chess.pgn.read_game(f)) is not None:
                    if game.errors:raise ValueError('Illegal PGN input fixture: '+name)
                    board=game.board();excluded.add(key(board))
                    for move in game.mainline_moves():board.push(move);excluded.add(key(board))
                    n+=1
            receipts[name]=s.digest(p);counts[name]=n
        elif p.suffix=='.tsv':
            n=0
            for line in p.read_text(encoding='utf8').splitlines():
                if not line or line.startswith('#'):continue
                parts=line.split('\t')
                if len(parts)!=3:raise ValueError('Unhandled root fixture: '+name)
                board=chess.Board(parts[1]);excluded.add(key(board))
                for move in parts[2].split():board.push_uci(move);excluded.add(key(board))
                n+=1
            receipts[name]=s.digest(p);counts[name]=n
        elif name.endswith('selection.json'):
            # These are input selection manifests, including frozen clock inputs.
            data=json.loads(p.read_text(encoding='utf-8-sig'));keys=data.get('positionKeys')
            if keys is not None:
                if not isinstance(keys,list) or any(len(k.split())!=4 for k in keys):raise ValueError(name)
                excluded.update(keys)
            else:
                fixture=data.get('output',data.get('fixture'))
                if fixture and str(fixture).endswith('.tsv'):
                    f=ROOT/fixture
                    if not f.exists():f=p.parent/fixture
                    expected=data.get('outputSha256',data.get('sha256'))
                    if expected!=s.digest(f):raise ValueError('Changed input-only replay fixture: '+str(f))
                    for line in f.read_text(encoding='utf8').splitlines():
                        if not line or line.startswith('#'):continue
                        parts=line.split('\t');assert len(parts)==3
                        board=chess.Board(parts[1]);excluded.add(key(board))
                        for move in parts[2].split():board.push_uci(move);excluded.add(key(board))
                    receipts[str(f.relative_to(ROOT))]=expected
                resolved.append(dict(path=name,reason='Underlying tracked PGN/TSV or hash-verified named TSV replay input included. Source result headers are not used.'))
            receipts[name]=s.digest(p)
    for game,board,k in s.games(s.OLD):excluded.add(k)
    receipts[str(s.OLD)]=s.OLD_SHA
    population={};source_counts=dict(records=0,invalid=0,otherLength=0,excluded=0,terminal=0,otherPartition=0)
    with book.open(encoding='utf-8-sig') as source:
        while (game:=chess.pgn.read_game(source)) is not None:
            source_counts['records']+=1
            if game.errors or game.board().fen()!=chess.STARTING_FEN:source_counts['invalid']+=1;continue
            moves=list(game.mainline_moves())
            if len(moves)!=14:source_counts['otherLength']+=1;continue
            board=game.end().board();k=key(board)
            if k in excluded:source_counts['excluded']+=1;continue
            if board.is_game_over(claim_draw=True):source_counts['terminal']+=1;continue
            if int(hashlib.sha256(k.encode()).hexdigest(),16)%2!=1:source_counts['otherPartition']+=1;continue
            clean=chess.pgn.Game();clean.headers['Event']='CLV-R001 frozen held-out opening';node=clean
            for move in moves:node=node.add_variation(move)
            population.setdefault(k,str(clean))
    keys=random.Random(2026110605).sample(sorted(population),320)
    with target.open('x',encoding='utf8',newline='\n') as f:f.write('\n\n'.join(population[k] for k in keys)+'\n')
    record=dict(count=320,seed=2026110605,positionKeys=keys,outputSha256=s.digest(target),
        source=str(book.relative_to(ROOT)),sourceSha256=book_manifest['sha256'],sourceCounts=source_counts,availableDistinctPositions=len(population),
        excludedDistinctPositions=len(excluded),selectorSha256=s.digest(Path(__file__)),
        selection='Fixed random sample from fresh odd-hash14-ply legal startpos histories; no candidate evaluation or outcome filtering. The earlier16-ply held-out pool could not supply320 fresh pairs. This same-source-family seven-move distribution is declared before confirmation. Frozen320-pair independent sample, no spare/replacement pairs.')
    with selection.open('x',encoding='utf8',newline='\n') as f:json.dump(record,f,indent=2);f.write('\n')
    result=dict(status='passed',overlaps=len(set(keys)&excluded),inputFiles=receipts,inputCounts=counts,
        selectionManifestsWithoutPositionKeys=resolved,selectedPairs=320,selectionSha256=s.digest(selection),
        scope='Input-only audit. Frozen clock opening fixtures/selection inputs read; no frozen clock result, execution, analysis, transcript or tournament PGN files read. Some input manifests/fixtures retain original source result headers; those fields are ignored. Historical campaigns not reopened.',
        limitations='Excludes recorded experiment starting/opening/history positions, not every position visited inside historical search trees. Freshness is against locally available recorded input evidence, not a claim about every external chess database.')
    with audit.open('x',encoding='utf8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps(dict(status=result['status'],selectedPairs=320,overlaps=result['overlaps'],available=len(population),inputFiles=len(receipts))))
