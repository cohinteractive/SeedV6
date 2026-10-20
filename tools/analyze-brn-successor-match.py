"""Independent legal replay and paired summaries; use build/research/venv Python (chess1.11.2).

Usage: python tools/analyze-brn-successor-match.py PLAN RECEIPTS NEW_OUTPUT
No engine execution, training, model mutation or acceptance decision.
"""
import collections
import hashlib
import json
import math
from pathlib import Path
import sys

import chess


def analyze(plan_path, directory, output):
    plan_path, directory, output = map(Path, (plan_path, directory, output))
    if output.exists():
        raise ValueError("Preserve previous analysis")
    plan = json.loads(plan_path.read_text())
    binding = hashlib.sha256(plan_path.read_bytes()).hexdigest()
    groups = collections.defaultdict(dict)
    terminations = collections.Counter()
    telemetry = collections.defaultdict(lambda: collections.Counter())
    failures = []
    total_moves = 0
    for path in sorted(directory.glob("p*-m*-c*.json")):
        record = json.loads(path.read_text())
        assert record["planHash"] == binding, path
        pair, colour = record["pair"], record["colour"]
        if plan.get("openingMoves") is not None:
            board = chess.Board()
            for uci in plan["openingMoves"][pair]:
                move = chess.Move.from_uci(uci)
                assert move in board.legal_moves
                board.push(move)
            assert board.fen(en_passant="fen").split()[:5] == record["openingFen"].split()[:5], path
        else:
            board = chess.Board(record["openingFen"])  # Earlier prefix history unavailable in this receipt.
        for entry in record["moves"]:
            packed = entry["move"]
            promotion = {0: None, 2: chess.QUEEN, 3: chess.ROOK, 4: chess.BISHOP, 5: chess.KNIGHT}[(packed >> 12) & 7]
            move = chess.Move(packed & 63, (packed >> 6) & 63, promotion)
            assert move in board.legal_moves, (path, board.fen(), move)
            assert entry["player"] == (0 if board.turn else 1)
            board.push(move)
            assert board.fen(en_passant="fen").split()[:5] == entry["fenAfter"].split()[:5], (path, move)
            model = (record.get("candidateName") or record["candidate"]) if entry["player"] == colour else record["reference"]
            t = telemetry[model]
            t["moves"] += 1
            t["nodes"] += entry["nodes"]
            t["elapsedNanos"] += entry["elapsedNanos"]
            t["sumDepth"] += entry["depth"]
            t["fallbacks"] += bool(entry.get("fallback"))
            t["maximumMoveNanos"] = max(t["maximumMoveNanos"], entry["elapsedNanos"])
            total_moves += 1
        termination = record["result"]["termination"]
        terminations[termination] += 1
        assert len(record["moves"]) == record["result"]["plies"]
        if termination.endswith("CHECKMATES_BLACK"):
            assert board.is_checkmate() and board.turn == chess.BLACK
            white_score = 1
        elif termination.endswith("CHECKMATES_WHITE"):
            assert board.is_checkmate() and board.turn == chess.WHITE
            white_score = 0
        elif termination == "STALEMATE":
            assert board.is_stalemate()
            white_score = .5
        elif termination == "THREEFOLD_REPETITION":
            assert board.is_repetition(3)
            white_score = .5
        elif termination == "FIFTY_MOVE_RULE":
            assert board.is_fifty_moves()
            white_score = .5
        elif termination == "INSUFFICIENT_MATERIAL":
            assert board.is_insufficient_material()
            white_score = .5
        else:
            failures.append({"receipt": path.name, "termination": termination})
            white_score = None
        score = white_score if colour == 0 or white_score is None else 1 - white_score
        candidate_name = record.get("candidateName")
        if candidate_name is None:
            matching = [m["name"] for m in plan["models"][1:] if m["checkpoint"] == record["candidate"]]
            assert len(matching) == 1
            candidate_name = matching[0]
        key = (candidate_name, pair)
        assert colour not in groups[key]
        groups[key][colour] = score
    results = []
    for model in plan["models"][1:]:
        pairs, wdl, missing, incomplete = [], [0, 0, 0], 0, 0
        for pair in range(plan["pairs"]):
            game = groups[(model["name"], pair)]
            if len(game) != 2:
                missing += 1
            elif any(v is None for v in game.values()):
                incomplete += 1
            else:
                pairs.append((game[0] + game[1]) / 2)
                for value in game.values():
                    wdl[{1: 0, .5: 1, 0: 2}[value]] += 1
        mean = sum(pairs) / len(pairs) if pairs else None
        se = math.sqrt(sum((p - mean) ** 2 for p in pairs) / (len(pairs) - 1) / len(pairs)) if len(pairs) > 1 else None
        # Descriptive normal interval only: not a selection/confirmation rule.
        interval = [max(0, mean - 1.96 * se), min(1, mean + 1.96 * se)] if se is not None else None
        results.append(dict(name=model["name"], wins=wdl[0], draws=wdl[1], losses=wdl[2],
                            validPairs=len(pairs), missingPairs=missing, incompletePairs=incomplete,
                            score=mean, pairStandardError=se, descriptiveNormal95=interval, pairScores=pairs))
    report = dict(planHash=binding, pythonChess=chess.__version__, validatedMoves=total_moves,
                  terminations=terminations, failures=failures, models=results, telemetry=telemetry,
                  statisticalLimit="Paired descriptive normal intervals; small samples, candidate selection, reused development openings and repeated testing preclude confirmation claims.")
    with output.open("x", encoding="utf-8") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps(dict(validatedMoves=total_moves, models=results, failures=failures), indent=2))


if __name__ == "__main__":
    analyze(*sys.argv[1:])
