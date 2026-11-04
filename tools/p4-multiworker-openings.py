"""MW-R001 frozen development inputs and fresh unplayed confirmation reserve.
No BOUNDARY2 file, candidate evaluation, or game outcome is opened.
"""
import importlib.util
import json
from pathlib import Path
import random
import subprocess
import chess

spec = importlib.util.spec_from_file_location('mw_opening_support', Path(__file__).with_name('p4-tactical-openings.py'))
support = importlib.util.module_from_spec(spec); spec.loader.exec_module(support)
ROOT = support.ROOT
OUT = ROOT / 'docs/research/search/p4-multiworker'


def main():
    assert support.digest(support.OLD) == support.OLD_SHA
    assert support.digest(support.BOOK) == support.BOOK_SHA
    outputs = [OUT / (name + '-v1-openings.pgn') for name in ('development', 'confirmation-reserve')]
    if any(p.exists() or p.with_suffix('.selection.json').exists() for p in outputs):
        raise FileExistsError('Opening inputs are immutable; select a new version')
    excluded, receipts, unhandled = set(), {}, []
    for name in subprocess.check_output(['git', 'ls-files', 'docs/research/search'], cwd=ROOT, text=True).splitlines():
        if 'boundary2' in name.lower() or not name.endswith('selection.json'):
            continue
        f = ROOT / name; previous = json.loads(f.read_text(encoding='utf-8-sig'))
        keys = previous.get('positionKeys')
        if not isinstance(keys, list): unhandled.append(name); continue
        if any(not isinstance(k, str) or len(k.split()) != 4 for k in keys): raise ValueError(name)
        excluded.update(keys); receipts[name] = support.digest(f)
    for corpus in [ROOT/'docs/research/search/p4-tt-memory/h1-diagnostic-roots.tsv',
                   ROOT/'docs/research/search/p4-correction/mixedphase-v1-roots.tsv']:
        receipts[str(corpus.relative_to(ROOT))] = support.digest(corpus)
        for line in corpus.read_text().splitlines():
            name, fen, moves = line.split('\t'); moves = moves.split()
            for length in {len(moves), max(0, len(moves)-2), max(0, len(moves)-4)}:
                board = chess.Board(fen)
                for move in moves[:length]: board.push_uci(move)
                excluded.add(' '.join(board.fen().split()[:4]))
    development = {}
    for game, board, key in support.games(support.OLD):
        excluded.add(key)
        if not board.is_game_over(claim_draw=True): development.setdefault(key, str(game))
    reserve = {}
    for game, board, key in support.games(support.BOOK):
        if key in excluded or board.is_game_over(claim_draw=True): continue
        if int(support.hashlib.sha256(key.encode()).hexdigest(), 16) % 2 != 1: continue
        reserve.setdefault(key, str(game))
    for path, population, count, seed in [(outputs[0], development, 64, 2026110411),
                                          (outputs[1], reserve, 480, 2026110412)]:
        keys = random.Random(seed).sample(sorted(population), count)
        with path.open('x', encoding='utf-8', newline='\n') as out:
            out.write('\n\n'.join(population[k] for k in keys) + '\n')
        manifest = dict(count=count, seed=seed, positionKeys=keys, outputSha256=support.digest(path),
            source=str(support.OLD if path == outputs[0] else support.BOOK),
            sourceSha256=support.OLD_SHA if path == outputs[0] else support.BOOK_SHA,
            availableDistinctPositions=len(population), excludedSelections=receipts,
            excludedDistinctPositions=len(excluded), unhandledNonKeyReceipts=unhandled,
            selectorSha256=support.digest(Path(__file__)),
            selection='Seeded sample of unique legal 16-ply startpos histories. Development uses known legal16 pool. Reserve uses fresh odd hash bucket excluding entire legal16 pool, all tracked non-BOUNDARY2 positionKeys and MW diagnostic/predecessor roots.',
            status='Exploration input' if path == outputs[0] else 'Protected unplayed reserve; not a frozen confirmation sample/protocol and no claim attempt spent')
        path.with_suffix('.selection.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
    assert not set(json.loads(outputs[0].with_suffix('.selection.json').read_text())['positionKeys']) & set(
        json.loads(outputs[1].with_suffix('.selection.json').read_text())['positionKeys'])
    print(json.dumps({'developmentPairs':64, 'protectedFreshPairs':480, 'unhandledNonKeyReceipts':unhandled}))


if __name__ == '__main__': main()
