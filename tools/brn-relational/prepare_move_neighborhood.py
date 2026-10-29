"""R049 checkpointed known-parent/unseen-child diagnostic selection, no fitting."""
import json
from pathlib import Path
import random
import sqlite3
import time
import chess
from baseline import ROOT, admit, sha
from compute_control import atomic
from prepare_objective import alias

SEED = 2026102949
TARGET = 4096
CANDIDATES = 8192
TRAIN_ROWS = 1048576
FOLDER = ROOT / 'docs/research/brn-relational-cglhw'
OUT = FOLDER / 'raw/r049-data'
MATCHES = [('r001', 16), ('r001b', 192), ('r002-games', 64),
           ('r007-bt4-games', 64), ('r025', 32), ('r030', 32),
           ('r035-71', 24), ('r035-97', 24), ('r043', 32),
           ('r047', 32), ('r048', 32)]


def main():
    started = time.monotonic()
    samples = [admit(initial=True)]
    sampled = started

    def check():
        nonlocal sampled
        if time.monotonic() - sampled >= 5:
            samples.append(admit()); sampled = time.monotonic()
        if time.monotonic() - started >= 180:
            raise TimeoutError('Selection tranche exhausted; durable cursor retained')

    train = FOLDER / 'raw/r007-teacher-data/train/data.tsv'
    dbpath = FOLDER / 'raw/r010-data/selection.sqlite'
    old = FOLDER / 'raw/r002-scale-data/validation.tsv'
    newer = FOLDER / 'raw/r016-data/selection.json'
    structural = FOLDER / 'r008-cases.json'
    expected = {train: 'b9e01e578c2765a77a906b818d8ba5595d7c3f64156674c4dccd2f80a5cde63a',
                dbpath: '0a5c7afc24ed463146a0dfa8964e00eb24c208512d7b200d8f4ea48f61bb4972',
                old: '9e3ba6430ca575c00780bec1de5ef65971b59d3e5c9a30a4ed9d2edc65a59826',
                newer: 'ef3c9a40f55e432d1012dcc3c1af1e703fd2d49cdd0d5ba5307e2b051ed7b37d',
                structural: '271b3cba2b8e7803c8009ca338f7335584e0dba89e7f624a2014dfc8b389b6d4'}
    inputs = {}
    for path, digest in expected.items():
        assert sha(path) == digest, path
        inputs[str(path.relative_to(ROOT))] = digest; check()
    games = []
    for name, count in MATCHES:
        planpath = FOLDER / (name + '-plan.json')
        plan = json.loads(planpath.read_text()); openings = ROOT / plan['openings']
        inputs[str(planpath.relative_to(ROOT))] = sha(planpath)
        inputs[str(openings.relative_to(ROOT))] = sha(openings)
        if 'openings_sha256' in plan:
            assert sha(openings) == plan['openings_sha256']
        roots = json.loads(openings.read_text())
        paths = sorted((ROOT / plan['output']).glob('**/game-*.json'))
        assert len(paths) == count, (name, len(paths))
        for path in paths:
            inputs[str(path.relative_to(ROOT))] = sha(path)
            games.append((path, roots))
    assert len(games) == 544
    binding = dict(seed=SEED, target=TARGET, candidates=CANDIDATES, inputs=inputs,
                   selector_sha256=sha(Path(__file__)),
                   alias_sha256=sha(Path(__file__).with_name('prepare_objective.py')),
                   protocol_sha256=sha(FOLDER / 'R049.md'))
    OUT.mkdir(exist_ok=True)
    bound = OUT / 'binding.json'
    if bound.exists():
        assert json.loads(bound.read_text()) == binding, 'Selection binding changed'
    else:
        atomic(bound, binding)
    done = FOLDER / 'r049-selection.json'
    if done.exists():
        manifest = json.loads(done.read_text())
        assert manifest['binding_sha256'] == sha(bound)
        for path, digest in manifest['files'].items():
            assert sha(ROOT / path) == digest
        print('Verified completed R049 selection'); return
    error = None
    try:
        reservation_path = OUT / 'reservations.json'
        if reservation_path.exists():
            state = json.loads(reservation_path.read_text())
            reserved = set(state['aliases']); cursor = state['next_game']
        else:
            reserved = {alias(chess.Board(line.split('\t')[0])) for line in old.read_text().splitlines()}
            reserved.update(row['alias'] for row in json.loads(newer.read_text())['state']['rows'])
            for quartet in json.loads(structural.read_text()):
                reserved.update(alias(chess.Board(fen)) for fen in quartet['fens'])
            cursor = 0
            atomic(reservation_path, dict(next_game=0, aliases=sorted(reserved)))
        for i in range(cursor, len(games)):
            path, roots = games[i]; game = json.loads(path.read_text()); board = chess.Board()
            for uci in roots[game['opening']]['moves'].split() + game['moves']:
                reserved.add(alias(board)); move = chess.Move.from_uci(uci)
                assert board.is_legal(move), path
                board.push(move)
            reserved.add(alias(board))
            if (i + 1) % 32 == 0 or i + 1 == len(games):
                atomic(reservation_path, dict(next_game=i + 1, aliases=sorted(reserved)))
            check()

        candidates_path = OUT / 'candidates.json'
        if candidates_path.exists():
            candidates = json.loads(candidates_path.read_text())
        else:
            order = random.Random(SEED).sample(range(TRAIN_ROWS), CANDIDATES)
            wanted = set(order); selected = {}
            with train.open() as stream:
                for index, line in enumerate(stream):
                    if index in wanted:
                        fen, target = line.rstrip().split('\t')
                        selected[index] = dict(ordinal=index, fen=fen, teacher_target=float(target))
                    if index % 65536 == 0: check()
            assert index + 1 == TRAIN_ROWS and len(selected) == CANDIDATES
            candidates = [selected[index] for index in order]
            atomic(candidates_path, candidates)
        selection_path = OUT / 'selection.json'
        if selection_path.exists():
            selection = json.loads(selection_path.read_text())
        else:
            selection = dict(cursor=0, pairs=[], dropped={})
        pairs = selection['pairs']; used = {p['child_alias'] for p in pairs}
        dropped = selection['dropped']
        connection = sqlite3.connect(dbpath.resolve().as_uri() + '?mode=ro', uri=True)
        connection.execute('PRAGMA cache_size=-8192')
        try:
            assert int(connection.execute("SELECT value FROM meta WHERE key='count'").fetchone()[0]) == 1574296
            for i in range(selection['cursor'], len(candidates)):
                if len(pairs) >= TARGET: break
                candidate = candidates[i]; board = chess.Board(candidate['fen'])
                key = alias(board)
                inherited = connection.execute('SELECT alias,line FROM rows WHERE ordinal=?', (candidate['ordinal'],)).fetchone()
                assert inherited and inherited[0] == key and inherited[1].split('\t')[0] == candidate['fen']
                reason = None
                if not board.is_valid() or board.is_check() or board.is_game_over(claim_draw=False):
                    reason = 'parent_check_terminal_invalid'
                elif key in reserved:
                    reason = 'parent_reserved'
                else:
                    moves = sorted(board.legal_moves, key=lambda m: m.uci())
                    move = random.Random(SEED + candidate['ordinal']).choice(moves)
                    flags = dict(capture=board.is_capture(move), promotion=bool(move.promotion),
                                 castling=board.is_castling(move), en_passant=board.is_en_passant(move))
                    child = board.copy(stack=False); child.push(move); childkey = alias(child)
                    if child.is_check() or child.is_game_over(claim_draw=False): reason = 'child_check_terminal'
                    elif childkey in reserved: reason = 'child_reserved'
                    elif childkey in used: reason = 'child_duplicate'
                    elif connection.execute('SELECT 1 FROM rows WHERE alias=?', (childkey,)).fetchone(): reason = 'child_preexisting'
                    else:
                        assert child.is_valid()
                        pairs.append(candidate | dict(parent_alias=key, child_alias=childkey,
                                     child_fen=child.fen(), move=move.uci(), pieces=len(board.piece_map()), **flags))
                        used.add(childkey)
                if reason: dropped[reason] = dropped.get(reason, 0) + 1
                selection['cursor'] = i + 1
                if (i + 1) % 128 == 0 or len(pairs) == TARGET: atomic(selection_path, selection)
                check()
            atomic(selection_path, selection)
        finally:
            connection.close()
        assert len(pairs) == TARGET, ('Insufficient fixed candidate coverage', len(pairs))
        reference = json.loads((FOLDER / 'r046-played-plan.json').read_text())
        reference['models'] = [m for m in reference['models'] if m['kind'] != 'MIX_TIED_MASK']
        files = {}
        for path in (bound, reservation_path, candidates_path, selection_path):
            files[str(path.relative_to(ROOT))] = sha(path)
        plans = []
        for shard in range(8):
            positions = []
            for pair in pairs[shard * 512:(shard + 1) * 512]:
                for fen in (pair['fen'], pair['child_fen']):
                    board = chess.Board(fen)
                    positions.append(dict(fen=fen, pieces=len(board.piece_map()), in_check=board.is_check()))
            cases = OUT / f'cases-{shard:02d}.json'
            planpath = FOLDER / f'r049-{shard:02d}-plan.json'
            plan = reference | dict(cases=str(cases.relative_to(ROOT)), rows=len(positions),
                                    output=str((OUT / f'prediction-{shard:02d}.json').relative_to(ROOT)))
            value = dict(positions=positions)
            if cases.exists(): assert json.loads(cases.read_text()) == value
            else: atomic(cases, value)
            plan['cases_sha256'] = sha(cases)
            if planpath.exists(): assert json.loads(planpath.read_text()) == plan
            else: atomic(planpath, plan)
            for path in (cases, planpath): files[str(path.relative_to(ROOT))] = sha(path)
            plans.append(str(planpath.relative_to(ROOT)))
        result = dict(binding_sha256=sha(bound), files=files, plans=plans, pairs=len(pairs),
                      consumed_candidates=selection['cursor'], dropped=dropped, reserved_aliases=len(reserved),
                      prior_games=len(games), captures=sum(p['capture'] for p in pairs),
                      seconds=time.monotonic()-started, resources=samples,
                      scope='Known training parents;unseen legal children;development diagnostic only;no fitting.')
        atomic(done, result)
        print(json.dumps({k:v for k,v in result.items() if k not in ('files','plans','resources')}, indent=2))
    except Exception as exc:
        error = repr(exc); raise
    finally:
        atomic(OUT / ('selection-execution-' + str(time.time_ns()) + '.json'),
               dict(seconds=time.monotonic()-started, error=error, resources=samples))


if __name__ == '__main__':
    main()
