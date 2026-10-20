"""Development-only complementary-error diagnostic; no training or model writes.

Usage: python this_script.py BT4_JSONL CP_JSONL NEW_OUTPUT
Fixed grid .0/.25/.5/1 W1 weight, raw and quarter residual calibration.
Matches the source-defined smooth WDL objective; verify alpha0/1 against data-01.
"""
import hashlib
import json
import math
from pathlib import Path
import sys


def log_sigmoid(z):
    return -max(0, -z) - math.log1p(math.exp(-abs(z)))


def log_add(a, b):
    m = max(a, b)
    return m + math.log(math.exp(a - m) + math.exp(b - m))


def objective(pawns, material, target):
    m = max(17, min(78, material)) / 58
    a = ((-13.50030198 * m + 40.92780883) * m - 36.82753545) * m + 386.83004070
    b = ((96.53354896 * m - 165.79058388) * m + 90.89679019) * m + 49.29561889
    z1, z2 = a / b * (pawns - 1), a / b * (pawns + 1)
    lp = log_add(log_sigmoid(z1), log_sigmoid(z2)) - math.log(2)
    lq = log_add(log_sigmoid(-z1), log_sigmoid(-z2)) - math.log(2)
    return -.5 * ((1 + target) * lp + (1 - target) * lq), (2 * math.exp(lp) - 1 - target) ** 2


def main():
    output = Path(sys.argv[3])
    if output.exists():
        raise ValueError("Preserve previous evidence")
    reports = []
    for source in sys.argv[1:3]:
        data = Path(source).read_bytes()
        rows = [json.loads(line) for line in data.splitlines()]
        metrics = []
        for name in ("W1-GATE", "W1-CUBIC", "W1-POOL"):
            for alpha in (0, .25, .5, 1):
                for gain in (.25, 1):
                    ce = mse = 0
                    for row in rows:
                        p = row["predictions"]
                        raw = (1 - alpha) * p["Gen32"] + alpha * p[name]
                        score = p["material"] + gain * (raw - p["material"])
                        material = sum({"p": 1, "n": 3, "b": 3, "r": 5, "q": 9}.get(c.lower(), 0)
                                       for c in row["fen"].split()[0])
                        c, e = objective(score, material, row["target"])
                        ce += c
                        mse += e
                    metrics.append(dict(candidate=name, alpha=alpha, gain=gain,
                                        crossEntropy=ce / len(rows), outcomeMse=mse / len(rows)))
        reports.append(dict(source=source, sha256=hashlib.sha256(data).hexdigest(),
                            positions=len(rows), metrics=metrics))
    output.write_bytes((json.dumps(dict(
        purpose="Fixed-grid complementary-error diagnostic on previously inspected development data",
        limitation="No independent selection, training, inference-cost or playing-strength claim",
        sources=reports), indent=2) + "\n").encode())


if __name__ == "__main__":
    main()
