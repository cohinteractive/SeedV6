"""Fixed-grid algebra on frozen predictions, not training or a strength test.

Usage: venv-python analyze-brn-hybrid-feasibility.py PLAN WORKSTREAM NEW_OUTPUT
"""
import importlib.util
import hashlib
import json
from pathlib import Path
import statistics
import sys

import chess


def main(plan_path, workstream, output):
    plan_path, workstream, output = map(Path, (plan_path, workstream, output))
    if output.exists():
        raise ValueError("Preserve prior result")
    plan = json.loads(plan_path.read_text(encoding="utf-8"))
    spec = importlib.util.spec_from_file_location("residual_data", Path(__file__).with_name("analyze-brn-residual-data.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    inputs = {str(plan_path): hashlib.sha256(plan_path.read_bytes()).hexdigest()}
    datasets = []
    java_sources = {}
    for relative in plan["inputs"]:
        path = workstream / relative
        inputs[str(path)] = hashlib.sha256(path.read_bytes()).hexdigest()
        java_path = path.parent / "summary.json"
        inputs[str(java_path)] = hashlib.sha256(java_path.read_bytes()).hexdigest()
        java_sources.update({s["source"]: s for s in json.loads(java_path.read_text(encoding="utf-8"))["sources"]})
        rows = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]
        assert len(rows) == 2048
        material = []
        for row in rows:
            board = chess.Board(row["fen"])
            material.append(sum(len(board.pieces(piece, colour)) * value
                                for colour in chess.COLORS
                                for piece, value in ((chess.PAWN, 1), (chess.KNIGHT, 3),
                                                     (chess.BISHOP, 3), (chess.ROOK, 5), (chess.QUEEN, 9))))
        datasets.append(("BT4" if "bt4" in path.name else "CP", rows, material))
    measurements = []
    selections = []
    maximum_java_error = 0
    for base in plan["bases"]:
        for correction in plan["corrections"]:
            grid = []
            for beta in plan["betaGrid"]:
                sources = []
                for source, rows, material in datasets:
                    losses = []
                    standalone_losses = []
                    raw_delta = []
                    for index, row in enumerate(rows):
                        values = row["predictions"]
                        baseline = values["Gen32"] + .5 * (values[base] - values["Gen32"])
                        combined = baseline + beta * (values[correction] - values["Gen32"])
                        losses.append(module.cross_entropy(combined, material[index], row["target"]))
                        standalone = values["Gen32"] + beta * (values[correction] - values["Gen32"])
                        standalone_losses.append(module.cross_entropy(standalone, material[index], row["target"]))
                        raw_delta.append((combined - baseline) ** 2)
                    if beta == 0:
                        for partition, measured in (("fit", losses[:1024]), ("evaluation", losses[1024:])):
                            expected = java_sources[source]["metrics"][base + "-half:" + partition]["crossEntropy"]
                            maximum_java_error = max(maximum_java_error, abs(statistics.mean(measured) - expected))
                    if beta in (.5, 1):
                        key = correction + ("-half" if beta == .5 else "") + ":evaluation"
                        expected = java_sources[source]["metrics"][key]["crossEntropy"]
                        maximum_java_error = max(maximum_java_error, abs(statistics.mean(standalone_losses[1024:]) - expected))
                    sources.append(dict(source=source, fitCE=statistics.mean(losses[:1024]),
                                        evaluationCE=statistics.mean(losses[1024:]),
                                        standaloneEvaluationCE=statistics.mean(standalone_losses[1024:]),
                                        evaluationDeltaRmsPawns=statistics.mean(raw_delta[1024:]) ** .5))
                row = dict(base=base, correction=correction, beta=beta, sources=sources,
                           meanFitCE=statistics.mean(s["fitCE"] for s in sources))
                grid.append(row)
                measurements.append(row)
            # Fixed tie rule favours less added correction. Evaluation rows never
            # enter this selection; all grid results remain visible.
            selected = min(grid, key=lambda row: (row["meanFitCE"], row["beta"]))
            baseline = next(row for row in grid if row["beta"] == 0)
            selections.append(dict(
                base=base, correction=correction, beta=selected["beta"],
                meanFitImprovement=baseline["meanFitCE"] - selected["meanFitCE"],
                evaluationImprovement={a["source"]: a["evaluationCE"] - b["evaluationCE"]
                                       for a, b in zip(baseline["sources"], selected["sources"])},
                improvementOverStandalone={s["source"]: s["standaloneEvaluationCE"] - s["evaluationCE"]
                                           for s in selected["sources"]},
                additiveOnly="BRE-CONTROL" in correction))
    assert maximum_java_error < 1e-12, maximum_java_error
    report = dict(id=plan["id"] + "-results", inputs=inputs, measurements=measurements, selections=selections,
                  maximumIndependentJavaCEError=maximum_java_error,
                  formula=plan["formula"],
                  limitations="Consumed development panels and multiple choices. Algebraic feasibility only; no trained model, efficient implementation, chess gain, or independent generalization claimed. Additive-only mixtures are essential controls.")
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps(selections, indent=2))


if __name__ == "__main__":
    main(*sys.argv[1:])
