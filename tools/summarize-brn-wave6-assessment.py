"""Verify and close the fixed G6 development admission without pooling strength.

Usage: python -B tools/summarize-brn-wave6-assessment.py RESEARCH_DIR NEW_REPORT [--followup]
Requires all admitted games and independently replayed evidence to remain local.
"""
import hashlib
import json
from pathlib import Path
import sys


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def load(path):
    return json.loads(Path(path).read_text(encoding="utf-8-sig"))


def summarize(directory, output, mode=None):
    assert mode in (None, "--followup")
    followup = mode == "--followup"
    directory, output = Path(directory), Path(output)
    if output.exists():
        raise ValueError("Preserve completed report")
    admission_path = directory / ("wave6-followup-admission.json" if followup else "wave6-assessment-protocol.json")
    admission = load(admission_path)
    assert admission["id"] == ("wave6-cost-and-compact-degree-followup-v1" if followup else "wave6-development-assessment-v1")
    rows = []
    for protocol in admission["protocols"]:
        name, games = protocol["label"], protocol["games"]
        assert digest(protocol["plan"]) == protocol["sha256"]
        path = directory / f"wave6-{name}-results.json"
        result = load(path)
        assert result["games"] == games
        assert result["status"] == "Closed fixed-budget development experiment"
        for filename, expected in result["evidenceSha256"].items():
            assert digest(filename) == expected, filename
        analysis = load(result["analysis"])
        assert not analysis["failures"] and not analysis["failedAttemptFiles"]
        assert len(analysis["receiptHashes"]) == games
        for filename, expected in analysis["receiptHashes"].items():
            assert digest(Path(result["receipts"]) / filename) == expected, filename
        for process in result["processReceipts"]:
            assert digest(process["path"]) == process["sha256"]
            assert process["exitCode"] == 0
            assert process["concurrentGames"] == process["workersPerEngine"] == process["activeProcessors"] == 1
            assert process["pondering"] is False and process["heapMiB"] == 768
            assert process["priority"] == "BelowNormal"
        rows.append(dict(protocol=name, report=str(path), sha256=digest(path),
                         games=games, validatedMoves=result["validatedMoves"],
                         models=result["models"], sameRootSearch=result["sameRootSearch"],
                         peakWorkingSetMiB=result["maximumSampledWorkingSetMiB"],
                         engineCpuSeconds=result["totalEngineCpuSeconds"]))
    report = dict(id="wave6-trained-development-followup-v1" if followup else "wave6-trained-development-assessment-v1",
                  admission=str(admission_path), admissionSha256=digest(admission_path),
                  protocols=rows, totalGames=sum(x["games"] for x in rows),
                  validatedMoves=sum(x["validatedMoves"] for x in rows),
                  maximumSampledWorkingSetMiB=max(x["peakWorkingSetMiB"] for x in rows),
                  totalEngineCpuSeconds=sum(x["engineCpuSeconds"] for x in rows),
                  limitations=[
                      "Development only; no pooled strength score, independent confirmation or success claim.",
                      "Eight or sixteen reversed-colour opening pairs per comparison; roots reused across related protocols, unknown opening-family dependence and prior candidate selection.",
                      "Normal intervals are descriptive; bounded intervals still assume independent opening pairs.",
                      "Distinct budgets/opponents answer distinct questions; fixed budgets completed without favorable optional stopping.",
                      "Q16 is adaptation-trained on identical G6 residual reservations; BRE retains different historical data, split and optimizer semantics."])
    assert report["totalGames"] == admission["totalGames"] == (80 if followup else 208)
    if followup:
        report["limitations"].append("Compact degree mask is unadapted and retains cubic cache cost; no adapted Q4 strength conclusion.")
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps({key: report[key] for key in
                     ("totalGames", "validatedMoves", "maximumSampledWorkingSetMiB")}))


if __name__ == "__main__":
    summarize(*sys.argv[1:])
