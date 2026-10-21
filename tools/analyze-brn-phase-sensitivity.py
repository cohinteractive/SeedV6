"""Descriptive piece-count stratification of already consumed W3/W4 predictions.

No fitting, new data, engine execution, model mutation, or candidate selection.
Usage: venv-python analyze-brn-phase-sensitivity.py WORKSTREAM NEW_OUTPUT
"""
import collections
import hashlib
import importlib.util
import json
from pathlib import Path
import statistics
import sys

import chess


def main(workstream, output):
    root, output = Path(workstream), Path(output)
    if output.exists():
        raise ValueError("Preserve completed evidence")
    spec = importlib.util.spec_from_file_location("metrics", Path(__file__).with_name("analyze-brn-residual-data.py"))
    metric = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(metric)
    groups = collections.defaultdict(lambda: collections.defaultdict(list))
    inputs = {}
    for source in ("bt4", "cp"):
        datasets = []
        for directory in ("maturity-average-01", "average-diagnostic-02"):
            path = root / "raw/wave4" / directory / (source + "-predictions.jsonl")
            inputs[str(path)] = hashlib.sha256(path.read_bytes()).hexdigest()
            datasets.append([json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()])
        assert len(datasets[0]) == len(datasets[1]) == 2048
        for index, (old, new) in enumerate(zip(*datasets)):
            assert old["fen"] == new["fen"] and old["target"] == new["target"]
            assert old["predictions"]["Gen32"] == new["predictions"]["Gen32"]
            board = chess.Board(old["fen"])
            count = len(board.piece_map())
            assert 2 <= count <= 32
            bucket = (count - 1) // 8
            material = sum(len(board.pieces(piece, colour)) * value for colour in chess.COLORS
                           for piece, value in ((chess.PAWN, 1), (chess.KNIGHT, 3), (chess.BISHOP, 3),
                                                (chess.ROOK, 5), (chess.QUEEN, 9)))
            reference = new["predictions"]["Gen32"]
            predictions = {"Gen32": reference}
            for family in ("RESGATE", "RESCUBIC", "BRE-CONTROL"):
                for scale in ("", "-half"):
                    name = "W3-" + family + "-average8" + scale
                    predictions[name] = old["predictions"][name]
                    for rate in ("LR001", "LR003"):
                        name = "W4-" + family + "-" + rate + "-average8" + scale
                        predictions[name] = new["predictions"][name]
            key = (source, "fit" if index < 1024 else "evaluation", bucket)
            for name, value in predictions.items():
                groups[key][name].append((metric.cross_entropy(value, material, old["target"]),
                                         (value - reference) ** 2))
    rows = []
    for (source, partition, bucket), models in sorted(groups.items()):
        rows.append(dict(source=source, partition=partition, pieces=[bucket * 8 + 1, (bucket + 1) * 8],
                         count=len(models["Gen32"]), metrics={name: dict(
                             crossEntropy=statistics.mean(v[0] for v in values),
                             deltaRmsPawns=statistics.mean(v[1] for v in values) ** .5)
                             for name, values in models.items()}))
    report = dict(id="wave4-phase-sensitivity-v1", inputs=inputs, rows=rows,
                  protocol="Fixed piece-count bins1-8,9-16,17-24,25-32 including kings. Retain existing first1024 fit/last1024 evaluation partitions. All original positions retained.",
                  limitations="Consumed panels and post-result diagnostic. Small bin sizes, no independent generalization, no phase calibration fitted, no strength or causal claim.")
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")


if __name__ == "__main__":
    main(*sys.argv[1:])
