"""Small legal-game fixtures for independent match evidence validation."""
import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest

import chess

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location(
    "successor_match", Path(__file__).with_name("analyze-brn-successor-match.py"))
match = importlib.util.module_from_spec(spec)
spec.loader.exec_module(match)


class MatchEvidenceTest(unittest.TestCase):
    def setUp(self):
        workspace = Path(__file__).resolve().parents[1]
        owned = workspace / "build/research/analyzer-tests"
        owned.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=owned)
        self.root = Path(self.temp.name).resolve()
        assert self.root.is_relative_to(owned.resolve())
        self.addCleanup(self.temp.cleanup)
        self.plan = self.root / "plan.json"
        self.output = self.root / "analysis.json"
        self.plan.write_text(json.dumps(dict(
            pairs=1, models=[dict(name="reference", checkpoint="base"),
                            dict(name="candidate", checkpoint="new")],
            openingMoves=[[]])), encoding="utf-8")
        binding = hashlib.sha256(self.plan.read_bytes()).hexdigest()
        self.records = []
        for colour in (0, 1):
            board, moves = chess.Board(), []
            for uci in ("f2f3", "e7e5", "g2g4", "d8h4"):
                move = chess.Move.from_uci(uci)
                player = 0 if board.turn else 1
                board.push(move)
                moves.append(dict(
                    move=move.from_square | (move.to_square << 6), player=player,
                    fenAfter=board.fen(en_passant="fen"), depth=4 if colour == 0 else 3,
                    nodes=200 if colour == 0 else 100, elapsedNanos=1000, fallback=False))
            record = dict(planHash=binding, pair=0, colour=colour, candidate="new",
                          candidateName="candidate", reference="base",
                          openingFen=chess.Board().fen(),
                          result=dict(termination="BLACK_CHECKMATES_WHITE", plies=4), moves=moves)
            self.records.append(record)
        self.save()

    def save(self):
        for colour, record in enumerate(self.records):
            (self.root / f"p000-m1-c{colour}.json").write_text(json.dumps(record), encoding="utf-8")

    def analyze(self):
        with contextlib.redirect_stdout(io.StringIO()):
            match.analyze(self.plan, self.root, self.output)
        return json.loads(self.output.read_text())

    def test_replays_colour_reversal_and_same_root_cost(self):
        report = self.analyze()
        self.assertEqual(8, report["validatedMoves"])
        self.assertEqual([1, 0, 1], [report["models"][0][k] for k in ("wins", "draws", "losses")])
        self.assertEqual(.5, report["models"][0]["score"])
        self.assertEqual(2, len(report["receiptHashes"]))
        root = report["sameRootSearch"][0]
        self.assertEqual(1, root["meanDepthDifference"])
        self.assertEqual(2, root["medianNodeRatio"])
        self.assertEqual(2, root["medianNpsRatio"])
        with self.assertRaises(ValueError):
            self.analyze()

    def test_rejects_wrong_checkpoint_even_with_matching_plan_hash(self):
        self.records[0]["reference"] = "wrong-base"
        self.save()
        with self.assertRaises(AssertionError):
            self.analyze()

    def test_rejects_illegal_move_trace(self):
        self.records[0]["moves"][0]["move"] = chess.E1 | (chess.E8 << 6)
        self.save()
        with self.assertRaises(AssertionError):
            self.analyze()

    def test_rejects_receipt_filename_mismatch(self):
        (self.root / "p000-m1-c0.json").rename(self.root / "p001-m1-c0.json")
        with self.assertRaises(AssertionError):
            self.analyze()

    def test_caps_are_not_scored_and_failed_attempts_remain_visible(self):
        for record in self.records:
            record["result"]["termination"] = "PLY_CAP"
        self.save()
        (self.root / "prior.failed").write_text("retained failure")
        report = self.analyze()
        self.assertEqual(2, len(report["failures"]))
        self.assertEqual(["prior.failed"], report["failedAttemptFiles"])
        self.assertIsNone(report["models"][0]["score"])
        self.assertEqual(1, report["models"][0]["incompletePairs"])

    def add_control(self, complete=True):
        plan = json.loads(self.plan.read_text())
        plan["models"].append(dict(name="control", checkpoint="continued"))
        self.plan.write_text(json.dumps(plan), encoding="utf-8")
        binding = hashlib.sha256(self.plan.read_bytes()).hexdigest()
        for record in self.records:
            record["planHash"] = binding
        self.save()
        controls = json.loads(json.dumps(self.records))
        for record in controls:
            record.update(candidateName="control", candidate="continued")
        board, moves = chess.Board(), []
        for uci in ("e2e4", "f7f6", "d2d4", "g7g5", "d1h5"):
            move = chess.Move.from_uci(uci)
            player = 0 if board.turn else 1
            board.push(move)
            moves.append(dict(move=move.from_square | (move.to_square << 6), player=player,
                              fenAfter=board.fen(en_passant="fen"), depth=4, nodes=200,
                              elapsedNanos=1000, fallback=False))
        controls[0].update(moves=moves, result=dict(termination="WHITE_CHECKMATES_BLACK", plies=5))
        for colour in range(2 if complete else 1):
            (self.root / f"p000-m2-c{colour}.json").write_text(json.dumps(controls[colour]), encoding="utf-8")

    def test_common_opponent_contrast_has_direction_and_shared_pair(self):
        self.add_control()
        contrast = self.analyze()["commonOpponentContrasts"][0]
        self.assertEqual(("candidate", "control"), (contrast["left"], contrast["right"]))
        self.assertEqual(1, contrast["sharedCompletePairs"])
        self.assertEqual(-.5, contrast["scoreDifference"])
        self.assertIsNone(contrast["descriptiveNormal95"])

    def test_common_opponent_contrast_excludes_missing_colour(self):
        self.add_control(complete=False)
        contrast = self.analyze()["commonOpponentContrasts"][0]
        self.assertEqual(0, contrast["sharedCompletePairs"])
        self.assertIsNone(contrast["scoreDifference"])


if __name__ == "__main__":
    unittest.main()
