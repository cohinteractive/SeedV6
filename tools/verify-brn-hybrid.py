"""Verify actual hybrid predictions against previously frozen component predictions.

Usage: venv-python verify-brn-hybrid.py MODEL_PLAN PROBE_DIRECTORY NEW_REPORT
Cache/search and strength receipts are separately checked, never inferred here.
"""
import hashlib
import importlib.util
import json
from pathlib import Path
import statistics
import sys

import chess


def verify(plan_path, directory, output):
    plan_path, directory, output = map(Path, (plan_path, directory, output))
    assert not output.exists(), "Preserve earlier verification"
    read = lambda p: json.loads(p.read_text(encoding="utf-8"))
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    plan, actual = read(plan_path), read(directory / "summary.json")
    assert {Path(k).resolve(): v for k, v in actual["inputs"].items()}[plan_path.resolve()] == digest(plan_path)
    assert len(actual["bindings"]) == len(plan["models"]) - 1
    for m, binding in zip(plan["models"][1:], actual["bindings"]):
        for key in ("name", "checkpoint", "networkSha256", "averagedInputs", "hybridBase", "correctionScale"):
            assert m[key] == binding["spec"][key]
        hybrid = binding["hybrid"]
        assert hybrid["input"] == m["hybridBase"]
        assert hashlib.sha256(hybrid["identityEncoding"].encode("utf-8")).hexdigest() == hybrid["functionSha256"]
    spec = importlib.util.spec_from_file_location("residual_data", Path(__file__).with_name("analyze-brn-residual-data.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    inputs = {str(p): digest(p) for p in (plan_path, directory / "summary.json")}
    prior_path = plan_path.parent / "wave4-hybrid-feasibility-verified.json"
    inputs[str(prior_path)] = digest(prior_path)
    prior_inputs = {Path(k).resolve(): v for k, v in read(prior_path)["inputs"].items()}
    max_raw, max_ce = 0, 0
    comparisons = []
    for source, label in (("bt4", "BT4"), ("cp", "CP")):
        original_path = plan_path.parent / "raw/wave4/average-diagnostic-02" / (source + "-predictions.jsonl")
        new_path = directory / (source + "-predictions.jsonl")
        assert digest(original_path) == prior_inputs[original_path.resolve()]
        inputs.update({str(p): digest(p) for p in (original_path, new_path)})
        original = [json.loads(line) for line in original_path.read_text(encoding="utf-8").splitlines()]
        new = [json.loads(line) for line in new_path.read_text(encoding="utf-8").splitlines()]
        assert len(original) == len(new) == 2048
        losses = {m["name"]: [] for m in plan["models"][1:]}
        for old, row in zip(original, new):
            assert old["fen"] == row["fen"] and old["target"] == row["target"]
            board = chess.Board(row["fen"])
            material = sum(len(board.pieces(piece, colour)) * value
                           for colour in chess.COLORS
                           for piece, value in ((chess.PAWN, 1), (chess.KNIGHT, 3),
                                                (chess.BISHOP, 3), (chess.ROOK, 5), (chess.QUEEN, 9)))
            values = old["predictions"]
            for m in plan["models"][1:]:
                mapping = plan["predictionMapping"][m["name"]]
                expected = values["Gen32"] + m["hybridBase"]["changeScale"] * (values[mapping["base"]] - values["Gen32"])
                expected += m["correctionScale"] * (values[mapping["correction"]] - values["Gen32"])
                measured = row["predictions"][m["name"]]
                max_raw = max(max_raw, abs(expected - measured))
                losses[m["name"]].append(module.cross_entropy(measured, material, row["target"]))
        java = next(s["metrics"] for s in actual["sources"] if s["source"] == label)
        for name, values in losses.items():
            for partition, part in (("fit", values[:1024]), ("evaluation", values[1024:])):
                mean = statistics.mean(part)
                max_ce = max(max_ce, abs(mean - java[name + ":" + partition]["crossEntropy"]))
                comparisons.append(dict(source=label, name=name, partition=partition, crossEntropy=mean))
    assert max_raw < 1e-9 and max_ce < 1e-12, (max_raw, max_ce)
    report = dict(id=plan["id"] + "-prediction-verification", inputs=inputs,
                  models=len(plan["models"]) - 1, positionsPerModel=4096,
                  maximumFrozenComponentAlgebraError=max_raw, maximumIndependentJavaCEError=max_ce,
                  metrics=comparisons, bindings=actual["bindings"],
                  scope="Actual immutable components reproduce the prior algebra. Consumed development data; no training, search or strength claim.")
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps({k: report[k] for k in ("models", "positionsPerModel", "maximumFrozenComponentAlgebraError", "maximumIndependentJavaCEError")}))


if __name__ == "__main__":
    verify(*sys.argv[1:])
