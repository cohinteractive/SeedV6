"""Compact provenance-bound completion summary, after independent legal replay.

Usage: python summarize-brn-match.py PLAN RECEIPTS ANALYSIS RUNTIME NEW_OUTPUT
Does not run engines, alter protocols, select candidates or claim confirmation.
"""
import hashlib
import json
from pathlib import Path
import sys


def summarize(plan_file, receipts, analysis_file, runtime_file, output):
    plan_file, receipts, analysis_file, runtime_file, output = map(
        Path, (plan_file, receipts, analysis_file, runtime_file, output))
    if output.exists():
        raise ValueError("Preserve completed summary")
    read = lambda p: json.loads(p.read_text(encoding="utf-8-sig"))
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    plan, analysis, runtime = map(read, (plan_file, analysis_file, runtime_file))
    bindings_file = receipts / "model-bindings.json"
    bindings = read(bindings_file)
    assert analysis["planHash"] == bindings["planHash"] == digest(plan_file)
    assert not analysis["failures"] and not analysis["failedAttemptFiles"]
    assert not list(receipts.glob("*.failed"))
    assert set(analysis["receiptHashes"]) == {p.name for p in receipts.glob("p*-m*-c*.json")}
    for name, expected in analysis["receiptHashes"].items():
        assert digest(receipts / name) == expected
    assert len(analysis["receiptHashes"]) == 2 * plan["pairs"] * (len(plan["models"]) - 1)
    assert [m["name"] for m in analysis["models"]] == [m["name"] for m in plan["models"][1:]]
    assert len(bindings["models"]) == len(plan["models"])
    for spec, bound in zip(plan["models"], bindings["models"]):
        assert spec["name"] == bound["name"]
        assert spec["checkpoint"] == bound["originalManifest"]["id"]
        assert spec["networkSha256"] == bound["originalManifest"]["networkSha256"]
    for model in analysis["models"]:
        assert model["validPairs"] == plan["pairs"]
        assert model["missingPairs"] == model["incompletePairs"] == 0
    for name, expected in runtime["jars"].items():
        assert digest(Path(runtime["runtime"]) / name) == expected
    processes = []
    for path in sorted(receipts.parent.glob("*.process.json")):
        record = read(path)
        if Path(record["plan"]).resolve() != plan_file.resolve():
            continue
        assert record["exitCode"] == 0
        assert Path(record["runtimeManifest"]).resolve() == runtime_file.resolve()
        assert record["concurrentGames"] == record["workersPerEngine"] == record["activeProcessors"] == 1
        assert not record["pondering"] and record["priority"] == "BelowNormal"
        assert record["heapMiB"] == 768
        processes.append(dict(path=str(path), sha256=digest(path), **record))
    assert processes, "Bounded process receipts required"
    report = dict(
        id=plan["id"] + "-completed", status="Closed fixed-budget development experiment",
        plan=str(plan_file), receipts=str(receipts), analysis=str(analysis_file), runtime=str(runtime_file),
        evidenceSha256={str(p): digest(p) for p in (plan_file, analysis_file, runtime_file, bindings_file)},
        games=len(analysis["receiptHashes"]), validatedMoves=analysis["validatedMoves"],
        models=analysis["models"], modelBindings=bindings["models"],
        sameRootSearch=analysis["sameRootSearch"], telemetry=analysis["telemetry"],
        commonOpponentContrasts=analysis["commonOpponentContrasts"],
        openingPrefixSensitivity=analysis.get("openingPrefixSensitivity"),
        processReceipts=processes,
        maximumSampledWorkingSetMiB=max(p["sampledPeakWorkingSetBytes"] for p in processes) / 1048576,
        totalEngineCpuSeconds=sum(p["cpuSeconds"] for p in processes),
        limitations=[analysis["statisticalLimit"], analysis["contrastLimit"], analysis["sameRootLimit"],
                     analysis.get("openingPrefixLimit"),
                     "No final strength claim, selection-adjusted confirmation, or architectural causation inferred."])
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps(dict(games=report["games"], validatedMoves=report["validatedMoves"],
                          models=report["models"], sameRootSearch=report["sameRootSearch"]), indent=2))


if __name__ == "__main__":
    summarize(*sys.argv[1:])
