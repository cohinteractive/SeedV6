"""Close the fixed W5 development protocols without pooling unlike comparisons.

Usage: python -B tools/summarize-brn-wave5-assessment.py RESEARCH_DIR NEW_REPORT
Requires all 544 games, their independently replayed analyses and bound receipts.
This is a provenance summary, not independent statistical confirmation.
"""
import hashlib
import json
from pathlib import Path
import sys


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def summarize(directory, output):
    directory, output = Path(directory), Path(output)
    if output.exists():
        raise ValueError("Preserve completed report")
    protocols = [("primary-node", 128), ("primary-clock", 128)]
    protocols += [(f"direct-{family}-{mode}", 32 if family == "bre001" else 16)
                  for mode in ("node", "clock")
                  for family in ("r4-r16", "bre001", "r4-unary", "r16-unary")]
    protocols += [(f"cubic-only-{control}-{mode}", 16)
                  for mode in ("node", "clock") for control in ("full", "unary")]
    protocols += [("depth-bre001-node", 32), ("degree3-r4-node", 16), ("degree3-r16-node", 16)]
    rows = []
    for name, games in protocols:
        path = directory / f"wave5-{name}-results.json"
        result = json.loads(path.read_text(encoding="utf-8-sig"))
        assert result["games"] == games
        assert result["status"] == "Closed fixed-budget development experiment"
        for filename, expected in result["evidenceSha256"].items():
            assert digest(Path(filename)) == expected, filename
        analysis = json.loads(Path(result["analysis"]).read_text(encoding="utf-8-sig"))
        assert not analysis["failures"] and not analysis["failedAttemptFiles"]
        assert len(analysis["receiptHashes"]) == games
        for filename, expected in analysis["receiptHashes"].items():
            assert digest(Path(result["receipts"]) / filename) == expected, filename
        for process in result["processReceipts"]:
            assert digest(Path(process["path"])) == process["sha256"]
            assert process["exitCode"] == 0
            assert process["concurrentGames"] == process["workersPerEngine"] == process["activeProcessors"] == 1
            assert process["pondering"] is False and process["heapMiB"] == 768
            assert process["priority"] == "BelowNormal"
        rows.append(dict(protocol=name, report=str(path), sha256=digest(path),
                         games=games, validatedMoves=result["validatedMoves"],
                         models=result["models"], sameRootSearch=result["sameRootSearch"],
                         peakWorkingSetMiB=result["maximumSampledWorkingSetMiB"],
                         engineCpuSeconds=result["totalEngineCpuSeconds"]))
    report = dict(id="wave5-trained-development-assessment-v1", protocols=rows,
                  totalGames=sum(x["games"] for x in rows),
                  validatedMoves=sum(x["validatedMoves"] for x in rows),
                  maximumSampledWorkingSetMiB=max(x["peakWorkingSetMiB"] for x in rows),
                  totalEngineCpuSeconds=sum(x["engineCpuSeconds"] for x in rows),
                  limitations=["Development evidence only; no pooled strength score or success claim.",
                               "Eight reversed-colour opening pairs per candidate comparison; roots reused within protocols and prior adaptive candidate selection.",
                               "Normal intervals are descriptive. Bounded intervals assume independent opening pairs; game-family independence is not verified.",
                               "Different depths, clocks, opponents and ablations answer distinct questions. Do not pool them or select a favourable stopping point.",
                               "Inference ablations are not separately optimized trained controls. BRE and residual data histories remain different."])
    assert report["totalGames"] == 544
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps({k: report[k] for k in ("totalGames", "validatedMoves", "maximumSampledWorkingSetMiB")}))


if __name__ == "__main__":
    summarize(*sys.argv[1:])
