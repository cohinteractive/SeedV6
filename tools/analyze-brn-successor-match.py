"""Independent legal replay and paired summaries; use build/research/venv Python (chess1.11.2).

Usage: python tools/analyze-brn-successor-match.py PLAN RECEIPTS NEW_OUTPUT
No engine execution, training, model mutation or acceptance decision.
"""
import collections
import hashlib
import json
import math
from pathlib import Path
import statistics
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
    roots = collections.defaultdict(dict)
    receipt_hashes = {}
    failures = []
    total_moves = 0
    for path in sorted(directory.glob("p*-m*-c*.json")):
        record = json.loads(path.read_text())
        assert record["planHash"] == binding, path
        pair, colour = record["pair"], record["colour"]
        assert 0 <= pair < plan["pairs"] and colour in (0, 1), path
        candidate_name = record.get("candidateName")
        if candidate_name is None:
            matching = [m["name"] for m in plan["models"][1:] if m["checkpoint"] == record["candidate"]]
            assert len(matching) == 1
            candidate_name = matching[0]
        model_index = next(i for i, m in enumerate(plan["models"]) if m["name"] == candidate_name)
        assert model_index > 0, path
        assert path.name == f"p{pair:03d}-m{model_index}-c{colour}.json", path
        assert record["candidate"] == plan["models"][model_index]["checkpoint"], path
        assert record["reference"] == plan["models"][0]["checkpoint"], path
        receipt_hashes[path.name] = hashlib.sha256(path.read_bytes()).hexdigest()
        if plan.get("openingMoves") is not None:
            board = chess.Board()
            for uci in plan["openingMoves"][pair]:
                move = chess.Move.from_uci(uci)
                assert move in board.legal_moves
                board.push(move)
            assert board.fen(en_passant="fen").split()[:5] == record["openingFen"].split()[:5], path
        else:
            board = chess.Board(record["openingFen"])  # Earlier prefix history unavailable in this receipt.
        if record["moves"]:
            roots[(candidate_name, pair)][colour] = dict(
                openingFen=record["openingFen"], first=record["moves"][0])
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
    root_summaries = []
    for model in plan["models"][1:]:
        observations = []
        for pair in range(plan["pairs"]):
            games = roots[(model["name"], pair)]
            if len(games) != 2:
                continue
            assert games[0]["openingFen"] == games[1]["openingFen"]
            # Candidate colour equals the common first player's colour in one game.
            player = games[0]["first"]["player"]
            assert player == games[1]["first"]["player"]
            candidate = games[player]["first"]
            reference = games[1 - player]["first"]
            if candidate.get("fallback") or reference.get("fallback"):
                continue
            if min(candidate["nodes"], reference["nodes"],
                   candidate["elapsedNanos"], reference["elapsedNanos"]) <= 0:
                continue
            observations.append(dict(
                pair=pair, sameFirstMove=candidate["move"] == reference["move"],
                depthDifference=candidate["depth"] - reference["depth"],
                nodeRatio=candidate["nodes"] / reference["nodes"],
                elapsedRatio=candidate["elapsedNanos"] / reference["elapsedNanos"],
                npsRatio=(candidate["nodes"] / candidate["elapsedNanos"]) /
                         (reference["nodes"] / reference["elapsedNanos"])))
        root_summaries.append(dict(
            name=model["name"], usablePairedRoots=len(observations),
            excludedOrMissingRoots=plan["pairs"] - len(observations),
            sameFirstMove=sum(o["sameFirstMove"] for o in observations),
            meanDepthDifference=statistics.mean(o["depthDifference"] for o in observations) if observations else None,
            lessEqualMoreDepth=[sum((o["depthDifference"] > 0) - (o["depthDifference"] < 0) == sign
                                    for o in observations) for sign in (-1, 0, 1)],
            medianNodeRatio=statistics.median(o["nodeRatio"] for o in observations) if observations else None,
            medianElapsedRatio=statistics.median(o["elapsedRatio"] for o in observations) if observations else None,
            medianNpsRatio=statistics.median(o["npsRatio"] for o in observations) if observations else None))
    report = dict(planHash=binding, pythonChess=chess.__version__, validatedMoves=total_moves,
                  terminations=terminations, failures=failures, models=results, telemetry=telemetry,
                  failedAttemptFiles=[p.name for p in sorted(directory.glob("*.failed"))],
                  receiptHashes=receipt_hashes, sameRootSearch=root_summaries,
                  sameRootLimit="Fresh search/TT at identical opening roots; different evaluator-dependent trees. "
                                "Clock protocols warm up; node protocols do not. Descriptive cost/behaviour, not causal latency or strength.",
                  statisticalLimit="Paired descriptive normal intervals; small samples, candidate selection, reused development openings and repeated testing preclude confirmation claims.")
    if plan.get("openingMoves") is not None:
        report["openingPrefixSensitivity"] = [
            dict(plies=n, uniquePrefixes=len(counts), largestGroup=max(counts.values()))
            for n in (4, 8, 12, 16)
            for counts in [collections.Counter(tuple(m[:n]) for m in plan["openingMoves"])]]
        report["openingPrefixLimit"] = (
            "Move-prefix grouping is a descriptive dependence warning, not ECO/source-game classification "
            "or proof of independent opening families. Pair intervals are unadjusted.")
    with output.open("x", encoding="utf-8") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps(dict(validatedMoves=total_moves, models=results, failures=failures), indent=2))


if __name__ == "__main__":
    analyze(*sys.argv[1:])
