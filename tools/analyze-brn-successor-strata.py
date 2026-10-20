"""Descriptive source/phase/target sensitivity on already consumed W3 predictions.

Usage: venv-python tools/analyze-brn-successor-strata.py INPUT_DIRECTORY NEW_OUTPUT
The frozen second half is unchanged; no fitting, new selection or strength claim.
"""
import collections
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import statistics
import sys

import chess


sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location(
    "residual_data", Path(__file__).with_name("analyze-brn-residual-data.py"))
residual_data = importlib.util.module_from_spec(spec)
spec.loader.exec_module(residual_data)
FAMILIES = ("W3-RESGATE", "W3-RESCUBIC", "W3-BRE-CONTROL")


def correlation(a, b):
    ma, mb = statistics.mean(a), statistics.mean(b)
    va = sum((x - ma) ** 2 for x in a)
    vb = sum((x - mb) ** 2 for x in b)
    return (sum((x - ma) * (y - mb) for x, y in zip(a, b)) /
            math.sqrt(va * vb)) if va and vb else None


def main(directory, output):
    directory, output = Path(directory), Path(output)
    if output.exists():
        raise ValueError("Preserve prior evidence")
    sources = []
    java = json.loads((directory / "summary.json").read_text(encoding="utf-8"))
    for source, expected in zip(("bt4", "cp"), java["sources"]):
        path = directory / f"{source}-predictions.jsonl"
        rows = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]
        assert len(rows) == 2048
        buckets = collections.defaultdict(list)
        deltas = collections.defaultdict(list)
        for row in rows[1024:]:
            board = chess.Board(row["fen"])
            material = sum(len(board.pieces(piece, colour)) * value
                           for colour in chess.COLORS
                           for piece, value in ((chess.PAWN, 1), (chess.KNIGHT, 3),
                                                (chess.BISHOP, 3), (chess.ROOK, 5),
                                                (chess.QUEEN, 9)))
            phase = ("material00_16" if material < 17 else
                     "material17_39" if material < 40 else
                     "material40_59" if material < 60 else "material60plus")
            target = row["target"]
            target_band = ("targetAbsBelow025" if abs(target) < .25 else
                           "targetAbs025_075" if abs(target) < .75 else "targetAbs075plus")
            strata = ("all", "valid" if board.is_valid() else "invalid", phase,
                      target_band, "whiteToMove" if board.turn else "blackToMove")
            raw = row["predictions"]
            base = raw["Gen32"]
            baseline = residual_data.cross_entropy(base, material, target)
            for family in FAMILIES:
                delta = raw[family] - base
                deltas[family].append(delta)
                for scale in (.5, 1.0):
                    prediction = base + scale * delta
                    loss = residual_data.cross_entropy(prediction, material, target)
                    values = (loss, loss - baseline, scale * delta, baseline)
                    for stratum in strata:
                        buckets[(family, scale, stratum)].append(values)
        metrics = []
        for (family, scale, stratum), values in buckets.items():
            n = len(values)
            metrics.append(dict(
                family=family, scale=scale, stratum=stratum, count=n,
                crossEntropy=statistics.mean(v[0] for v in values),
                baselineCrossEntropy=statistics.mean(v[3] for v in values),
                lossDelta=statistics.mean(v[1] for v in values),
                fractionLowerLoss=sum(v[1] < 0 for v in values) / n,
                meanRawChangePawns=statistics.mean(v[2] for v in values),
                rmsRawChangePawns=math.sqrt(statistics.mean(v[2] ** 2 for v in values))))
        max_error = max(abs(m["crossEntropy"] - expected["metrics"][
            m["family"] + "@1.0:evaluation"]["crossEntropy"])
                        for m in metrics if m["scale"] == 1 and m["stratum"] == "all")
        assert max_error < 1e-12, max_error
        sources.append(dict(
            source=source, input=str(path),
            sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
            evaluationRows=[1024, 2048], maximumJavaMeanError=max_error, metrics=metrics,
            correctionCorrelations=[dict(a=a, b=b, pearson=correlation(deltas[a], deltas[b]))
                                   for i, a in enumerate(FAMILIES) for b in FAMILIES[i + 1:]]))
    report = dict(
        id="brn-wave3-strata-sensitivity-v1", pythonChess=chess.__version__, sources=sources,
        limitations=[
            "Previously consumed development predictions; descriptive post-hoc mechanism diagnosis only.",
            "All original evaluation rows retained. Validity is a separate sensitivity stratum.",
            "No training, calibration fitting, candidate selection or independent playing-strength evidence.",
            "Source game identities unavailable; positions and strata are not assumed independent.",
            "Half-change interpolates recorded raw predictions; compiled BRE float folding may differ slightly."])
    with output.open("x", encoding="utf-8") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps([dict(source=s["source"], maximumJavaMeanError=s["maximumJavaMeanError"],
                           correctionCorrelations=s["correctionCorrelations"]) for s in sources], indent=2))


if __name__ == "__main__":
    main(*sys.argv[1:])
