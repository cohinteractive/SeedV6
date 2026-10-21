"""Compact provenance-bound completion summary, after independent legal replay.

Usage: python summarize-brn-match.py PLAN RECEIPTS ANALYSIS RUNTIME NEW_OUTPUT
Does not run engines, alter protocols, select candidates or claim confirmation.
"""
import hashlib
import json
import math
from pathlib import Path
import sys


def diagnostic_entrypoint(record):
    """Recognize explicitly separate diagnostic receipts; malformed match records fail closed."""
    if "args" in record and "runtimeSha256" in record:
        # Read-only lineage/prediction audits have a distinct subprocess receipt.
        # Classify only their exact bounded launch shape; never treat them as games.
        from datetime import datetime
        args = record["args"]
        assert isinstance(args, list) and all(isinstance(x, str) for x in args)
        assert args[1:5] == ["-Xmx768m", "-XX:ActiveProcessorCount=1",
                             "--add-modules=jdk.incubator.vector", "-cp"]
        entry = args[6]
        counts = {"com.ohinteractive.seedv6.tools.eval.BrnLineageAudit": 9,
                  "com.ohinteractive.seedv6.tools.eval.BrnResidualAssessment": 11}
        assert entry in counts and len(args) == counts[entry]
        assert record["priority"] == "BelowNormal" and record["heapMiB"] == 768
        assert record["activeProcessors"] == 1 and isinstance(record["exitCode"], int)
        assert len(record["runtimeSha256"]) == 64
        assert datetime.fromisoformat(record["completedUtc"]) >= datetime.fromisoformat(record["startedUtc"])
        return entry
    if set(record) == {"started", "ended", "exitCode", "args"}:
        # Historical bounded Gradle QA shares the evidence directory with games.
        # Require its explicit launcher and finite, recognized task set; do not
        # treat a missing match plan or arbitrary process as a successful match.
        from datetime import datetime
        arguments = record["args"]
        assert isinstance(arguments, list) and all(isinstance(x, str) for x in arguments)
        assert arguments[:4] == ["cmd.exe", "/d", "/c", "gradlew.bat"]
        tasks = [x for x in arguments[4:] if x.startswith(":")]
        assert tasks and set(tasks) <= {":engine-core:test", ":engine-training:test",
                                       ":app-gui:test", ":research:installResearch"}
        assert not any("com.ohinteractive." in x for x in arguments)
        assert "--max-workers=1" in arguments and "--offline" in arguments
        assert datetime.fromisoformat(record["ended"]) >= datetime.fromisoformat(record["started"])
        assert isinstance(record["exitCode"], int)
        return "separate-bounded-gradle-qa"
    arguments = record.get("arguments")
    assert isinstance(arguments, list) and all(isinstance(x, str) for x in arguments)
    entries = [x for x in arguments if x.startswith("com.ohinteractive.seedv6.tools.eval.")]
    admitted = {"BrnHybridProbe", "BrnHybridAblationPlan", "BrnGatedCacheProbe", "BrnCubicLayoutProbe"}
    assert len(entries) == 1 and entries[0].rsplit(".", 1)[-1] in admitted, "Unclassified process record without a plan"
    assert isinstance(record.get("exitCode"), int)
    return entries[0]


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
        if spec.get("packedCubicCache"):
            assert "brn-cubic-packed-v1" in runtime["modelFeatures"]
            assert bound["packedCubicCache"] == "packed-polynomial-period64-canonical-rounding-guard-v1"
        if spec.get("compiledUnary"):
            assert "brn-unary-compiled-v1" in runtime["modelFeatures"]
            assert spec["correctionAblation"] == "unary"
            assert bound["compiledUnary"]["kind"] == "brn-unary-compiled-v1"
            assert len(bound["compiledUnary"]["compiledNetworkSha256"]) == 64
        if spec.get("compiledConstantGates"):
            assert "brn-constant-gates-compiled-v1" in runtime["modelFeatures"]
            assert spec["correctionAblation"] == "constant-gates"
            compiled = bound["compiledConstantGates"]
            assert compiled["kind"] == "brn-constant-gates-compiled-v1" and compiled["relations"] == 298
            assert compiled["extraFirst"] == [1, 6, 48, 55] and compiled["extraSecond"] == [8, 15, 57, 62]
            assert compiled["ordinaryCheckpointExport"] is False
            identity = "brn-constant-gates-compiled-v1\n" + compiled["foldedBaseSha256"] + "\n" + compiled["extraTablesSha256"] + "\n"
            assert hashlib.sha256(identity.encode("utf-8")).hexdigest() == compiled["functionSha256"]
        if spec.get("hybridBase") is not None:
            assert "brn-hybrid-raw-v1" in runtime["modelFeatures"]
            hybrid = bound["hybrid"]
            assert hybrid["input"] == spec["hybridBase"]
            assert hybrid["originalManifest"]["id"] == spec["hybridBase"]["checkpoint"]
            assert hybrid["originalManifest"]["networkSha256"] == spec["hybridBase"]["networkSha256"]
            assert hybrid["effectiveCorrectionSha256"] == bound["effectiveNetworkSha256"]
            identity = "brn-hybrid-raw-v1\n" + hybrid["effectiveBaseSha256"] + "\n" + hybrid["effectiveCorrectionSha256"] + "\n"
            assert identity == hybrid["identityEncoding"]
            assert hashlib.sha256(identity.encode("utf-8")).hexdigest() == hybrid["functionSha256"]
    for model in analysis["models"]:
        assert model["validPairs"] == plan["pairs"]
        assert model["missingPairs"] == model["incompletePairs"] == 0
        # Pair scores are bounded in [0,1]. Unlike the descriptive normal
        # interval, this does not collapse when a tiny sample has zero variance.
        # Coverage still requires independent opening-pair units; the book's
        # shared prefixes do not establish that assumption.
        for label, comparisons in (("boundedPair95", 1),
                                   ("withinPlanSimultaneousBoundedPair95", len(analysis["models"]))):
            radius = math.sqrt(math.log(2 * comparisons / .05) / (2 * model["validPairs"]))
            model[label] = [max(0, model["score"] - radius), min(1, model["score"] + radius)]
    for name, expected in runtime["jars"].items():
        assert digest(Path(runtime["runtime"]) / name) == expected
    processes = []
    separate_diagnostics = []
    for path in sorted(receipts.parent.glob("*.process.json")):
        record = read(path)
        if "plan" not in record:
            entry = diagnostic_entrypoint(record)
            separate_diagnostics.append(dict(path=str(path), sha256=digest(path), entrypoint=entry,
                                             exitCode=record["exitCode"]))
            continue
        assert isinstance(record["plan"], str) and record["plan"], "Missing match process plan"
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
        separateDiagnosticProcessReceipts=separate_diagnostics,
        maximumSampledWorkingSetMiB=max(p["sampledPeakWorkingSetBytes"] for p in processes) / 1048576,
        totalEngineCpuSeconds=sum(p["cpuSeconds"] for p in processes),
        limitations=[analysis["statisticalLimit"], analysis["contrastLimit"], analysis["sameRootLimit"],
                     analysis.get("openingPrefixLimit"),
                     "Hoeffding intervals assume independent bounded opening-pair scores; within-plan union bound covers only this plan, not prior candidate selection or repeated protocols. Independence remains unverified.",
                     "No final strength claim, selection-adjusted confirmation, or architectural causation inferred."])
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps(dict(games=report["games"], validatedMoves=report["validatedMoves"],
                          models=report["models"], sameRootSearch=report["sameRootSearch"]), indent=2))


if __name__ == "__main__":
    summarize(*sys.argv[1:])
