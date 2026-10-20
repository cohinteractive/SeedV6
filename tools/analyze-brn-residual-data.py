"""Independent position-validity sensitivity on preserved development predictions.

Usage: venv-python tools/analyze-brn-residual-data.py ASSESSMENT_DIRECTORY NEW_OUTPUT
No corpus filtering/mutation or new sample selection. Mirrors the frozen WDL link,
checks it against Java's recorded second-half means, then reports all/valid strata.
python-chess status is a necessary validity check, not proof of legal reachability.
"""
import collections
import json
import math
from pathlib import Path
import statistics
import sys
import chess


def cross_entropy(pawns, material, target):
    m = max(17, min(78, material)) / 58
    a = ((-13.50030198 * m + 40.92780883) * m - 36.82753545) * m + 386.83004070
    b = ((96.53354896 * m - 165.79058388) * m + 90.89679019) * m + 49.29561889
    z1, z2 = a / b * (pawns - 1), a / b * (pawns + 1)

    def log_sigmoid(z):
        return -max(0, -z) - math.log1p(math.exp(-abs(z)))

    def log_add(x, y):
        top = max(x, y)
        return top + math.log(math.exp(x - top) + math.exp(y - top))

    p = log_add(log_sigmoid(z1), log_sigmoid(z2)) - math.log(2)
    q = log_add(log_sigmoid(-z1), log_sigmoid(-z2)) - math.log(2)
    return -.5 * ((1 + target) * p + (1 - target) * q)


def main(directory, output):
    directory, output = Path(directory), Path(output)
    if output.exists():
        raise ValueError("Preserve prior evidence")
    java = json.loads((directory / "summary.json").read_text(encoding="utf-8"))
    results = []
    for source, java_source in zip(("bt4", "cp"), java["sources"]):
        rows = [json.loads(s) for s in (directory / f"{source}-predictions.jsonl").read_text(encoding="utf-8").splitlines()]
        statuses = []
        metrics = collections.defaultdict(list)
        for index, row in enumerate(rows):
            board = chess.Board(row["fen"])
            status = int(board.status())
            statuses.append(status)
            material = sum(len(board.pieces(piece, colour)) * value
                           for colour in chess.COLORS
                           for piece, value in ((chess.PAWN, 1), (chess.KNIGHT, 3), (chess.BISHOP, 3), (chess.ROOK, 5), (chess.QUEEN, 9)))
            for name, raw in row["predictions"].items():
                ce = cross_entropy(raw, material, row["target"])
                for subset in ("all", "valid" if status == 0 else "invalid"):
                    metrics[(name, subset, "all")].append(ce)
                    if index >= 1024:
                        metrics[(name, subset, "evaluation")].append(ce)
        maximum_error = 0
        for name in rows[0]["predictions"]:
            actual = statistics.mean(metrics[(name, "all", "evaluation")])
            expected = java_source["metrics"][name + "@1.0:evaluation"]["crossEntropy"]
            maximum_error = max(maximum_error, abs(actual - expected))
        assert maximum_error < 1e-12, maximum_error
        results.append(dict(source=source, positions=len(rows), invalid=sum(s != 0 for s in statuses),
                            statusCounts=dict(collections.Counter(statuses)), maximumJavaMeanError=maximum_error,
                            crossEntropy=[dict(name=k[0], validity=k[1], partition=k[2], count=len(v), value=statistics.mean(v))
                                          for k, v in metrics.items() if v]))
    report = dict(input=str(directory), pythonChess=chess.__version__, sources=results,
                  limitation="Development sensitivity only. No retroactive filtering of frozen evidence. Valid status does not prove legal reachability.")
    output.write_bytes((json.dumps(report, indent=2) + "\n").encode("utf-8"))
    print(json.dumps([dict(source=s["source"], invalid=s["invalid"], maximumJavaMeanError=s["maximumJavaMeanError"]) for s in results]))


if __name__ == "__main__":
    main(*sys.argv[1:])
