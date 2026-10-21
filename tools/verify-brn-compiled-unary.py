"""Compare compiled unary predictions with the previously evaluated canonical ablation.
Usage: venv-python verify-brn-compiled-unary.py PLAN ACTUAL_DIRECTORY PRIOR_DIRECTORY NEW_REPORT
"""
import hashlib
import importlib.util
import json
from pathlib import Path
import statistics
import sys
import chess


def verify(plan_path, actual_dir, prior_dir, output):
    plan_path, actual_dir, prior_dir, output = map(Path, (plan_path, actual_dir, prior_dir, output))
    if output.exists():
        raise ValueError("Preserve prior verification")
    read = lambda p: json.loads(p.read_text(encoding="utf-8"))
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    plan, summary = read(plan_path), read(actual_dir / "summary.json")
    inputs = {str(p): digest(p) for p in (plan_path, actual_dir / "summary.json", prior_dir / "summary.json")}
    assert {Path(k).resolve(): v for k, v in summary["inputs"].items()}[plan_path.resolve()] == digest(plan_path)
    models = plan["models"][1:]
    for model, binding in zip(models, summary["bindings"]):
        assert model["compiledUnary"] and model["correctionAblation"] == "unary"
        assert model["name"].endswith("-compiled")
        for key, value in model.items():
            assert binding["spec"][key] == value
        assert binding["compiledUnary"]["kind"] == "brn-unary-compiled-v1"
    assert len(summary["bindings"]) == len(models)
    spec = importlib.util.spec_from_file_location("metrics", Path(__file__).with_name("analyze-brn-residual-data.py"))
    metric = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(metric)
    maximum_raw = maximum_ce = 0
    rows = []
    for source in ("bt4", "cp"):
        paths = [d / (source + "-predictions.jsonl") for d in (actual_dir, prior_dir)]
        inputs.update({str(p): digest(p) for p in paths})
        actual, prior = [[json.loads(line) for line in p.read_text(encoding="utf-8").splitlines()] for p in paths]
        assert len(actual) == len(prior) == 2048
        losses = {m["name"]: [] for m in models}
        for a, b in zip(actual, prior):
            assert a["fen"] == b["fen"] and a["target"] == b["target"]
            board = chess.Board(a["fen"])
            material = sum(len(board.pieces(piece, colour)) * value for colour in chess.COLORS
                           for piece, value in ((chess.PAWN, 1), (chess.KNIGHT, 3), (chess.BISHOP, 3),
                                                (chess.ROOK, 5), (chess.QUEEN, 9)))
            for name in losses:
                value = a["predictions"][name]
                maximum_raw = max(maximum_raw, abs(value - b["predictions"][name.removesuffix("-compiled")]))
                losses[name].append(metric.cross_entropy(value, material, a["target"]))
        java = next(s["metrics"] for s in summary["sources"] if s["source"].lower() == source)
        for name, values in losses.items():
            for partition, part in (("fit", values[:1024]), ("evaluation", values[1024:])):
                measured = statistics.mean(part)
                maximum_ce = max(maximum_ce, abs(measured - java[name + ":" + partition]["crossEntropy"]))
                rows.append(dict(source=source, model=name, partition=partition, crossEntropy=measured))
    assert maximum_raw < 1e-9 and maximum_ce < 1e-12
    report = dict(id=plan["id"] + "-prediction-verification", inputs=inputs, models=len(models),
                  positionsPerModel=4096, maximumPriorCanonicalRawError=maximum_raw,
                  maximumIndependentJavaCEError=maximum_ce, metrics=rows, bindings=summary["bindings"],
                  limitations="Consumed panels; algebraic and empirical numerical checks, not universal bit identity, retrained additive optimization or playing strength.")
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")


if __name__ == "__main__":
    verify(*sys.argv[1:])
