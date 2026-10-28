"""Deterministic, outcome-blind root sampling from completed R025 games."""
import hashlib
import json
from pathlib import Path
import chess
from baseline import ROOT, admit, sha
from compare_neural import replay


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    evidence = json.loads((folder/'r025-results.json').read_text())
    plan_path = folder/'r025-plan.json'
    assert sha(plan_path) == evidence['plan_sha256']
    plan = json.loads(plan_path.read_text())
    assert sha(ROOT/plan['openings']) == plan['openings_sha256']
    openings = json.loads((ROOT/plan['openings']).read_text())
    unique = {}; sources = []; selected = 0
    for comparison in evidence['comparisons']:
        for filename, digest in sorted(comparison['games'].items()):
            path = ROOT/plan['output']/comparison['id']/filename
            assert sha(path) == digest
            game = json.loads(path.read_text())
            opening = openings[game['opening']]
            replay(game, opening, plan)
            board = chess.Board()
            for move in opening['moves'].split():
                board.push_uci(move)
            candidates = []
            for ply, move in enumerate(game['moves']):
                rank = hashlib.sha256(f"r026:{comparison['id']}:{filename}:{ply}".encode()).hexdigest()
                origin = dict(comparison=comparison['id'], game=filename, ply=ply,
                              fen=board.fen(), neural_to_move=board.turn == game['left_color'],
                              played_move=move)
                candidates.append((rank, board.board_fen()+' '+('w' if board.turn else 'b'),
                                   dict(rank=rank, fen=board.fen(), in_check=board.is_check(),
                                        pieces=len(board.piece_map()), sources=[origin])))
                board.push_uci(move)
            chosen = sorted(candidates)[:32]
            selected += len(chosen)
            for rank, key, case in chosen:
                if key in unique:
                    unique[key]['sources'].extend(case['sources'])
                else:
                    unique[key] = case
            sources.append(dict(path=str(path.relative_to(ROOT)), sha256=digest,
                                available_roots=len(candidates), selected=len(chosen)))
    cases = sorted(unique.values(), key=lambda case: case['rank'])
    assert 0 < len(cases) <= selected <= 1024 and len(sources) == 32
    out = folder/'raw/r026-data'
    out.mkdir(exist_ok=False)
    with (out/'cases.json').open('x') as stream:
        json.dump(dict(positions=cases), stream, indent=2); stream.write('\n')
    manifest = dict(rows=len(cases), selected_before_dedup=selected,
                    cases=str((out/'cases.json').relative_to(ROOT)), cases_sha256=sha(out/'cases.json'),
                    source_evidence_sha256=sha(folder/'r025-results.json'), plan_sha256=sha(plan_path),
                    selector_sha256=sha(Path(__file__)), resources=resource, games=sources,
                    selection='Lowest SHA256 r026:comparison:game:ply,32 per game;exact placement/STM dedup;no outcome/evaluation filter;diagnostic only')
    with (folder/'r026-selection.json').open('x') as stream:
        json.dump(manifest, stream, indent=2); stream.write('\n')
    print(json.dumps({k:v for k,v in manifest.items() if k != 'games'}, indent=2))


if __name__ == '__main__':
    main()
