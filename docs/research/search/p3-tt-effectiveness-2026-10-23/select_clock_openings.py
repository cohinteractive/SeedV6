"""Prospective legal-history input selection, never tournament-output filtering."""
import importlib.util
import json
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

spec=importlib.util.spec_from_file_location('baseline',ROOT/'tools/search-baseline.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
book=ROOT.parent/'seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn'
assert digest(book)=='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
keys=set();sources={}
for path,limit,lifetime in ((ROOT/'docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv',12,False),
                            (ROOT/'docs/research/search/p3-sr002-execution-2026-10-19/confirmation-corpus.tsv',12,False),
                            (HERE/'lifetime-lines.tsv',4,True)):
    sources[str(path)]=digest(path)
    for line in path.read_text().splitlines()[:limit]:
        _,fen,moves=line.split('\t');board=b.chess.Board(fen)
        for index,move in enumerate(moves.split()):
            board.push_uci(move)
            if lifetime and index>=15:keys.add(' '.join(board.fen().split()[:4]))
        if not lifetime:keys.add(' '.join(board.fen().split()[:4]))
receipt=HERE/'clock-development-exclusions.json'
write_new(receipt,{'count':len(keys),'positionKeys':sorted(keys),'sources':sources,
                   'meaning':'All measured diagnostic/deeper roots and all actual game prefixes at/after ply16 in lifetime fixtures. Historical CP64 comparison exclusion is separately supplied.'})
prior=ROOT/'docs/research/search/p3-construction-2026-10-20/baseline-cp64/openings.selection.json'
b.select_openings(book,HERE/'pilot-v1-openings.pgn',2,2026102301,0,exclude=[receipt,prior])
print('Selected pilot inputs',flush=True)
