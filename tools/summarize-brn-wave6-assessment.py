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
    assert mode in (None, "--followup", "--wave7", "--wave8", "--wave9", "--wave9-scale")
    followup = mode == "--followup"
    scale = mode == "--wave9-scale"
    wave = 9 if mode in ("--wave9", "--wave9-scale") else 8 if mode == "--wave8" else 7 if mode == "--wave7" else 6
    directory, output = Path(directory), Path(output)
    if output.exists():
        raise ValueError("Preserve completed report")
    admission_path = directory / ("wave9-scale-followup-admission.json" if scale else "wave6-followup-admission.json" if followup else f"wave{wave}-assessment-protocol.json")
    admission = load(admission_path)
    assert admission["id"] == ("wave9-calibration-scale-followup-v1" if scale else "wave6-cost-and-compact-degree-followup-v1" if followup else f"wave{wave}-development-assessment-v1")
    rows = []
    for protocol in admission["protocols"]:
        name, games = protocol["label"], protocol["games"]
        assert digest(protocol["plan"]) == protocol["sha256"]
        path = directory / f"wave{wave}-{name}-results.json"
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
    report = dict(id="wave9-calibration-scale-followup-results-v1" if scale else "wave6-trained-development-followup-v1" if followup else f"wave{wave}-trained-development-assessment-v1",
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
                      f"Quadratic is adaptation-trained on identical G{wave} residual reservations; BRE retains different historical data, split and optimizer semantics."])
    assert report["totalGames"] == admission["totalGames"] == (80 if followup or scale else 192 if wave == 9 else 176 if wave in (7, 8) else 208)
    if wave == 8:
        report['limitations'][-1] = 'Fixed heads have identical initial Q4 function/data and new head-only Adam; BRE retains distinct historical data and optimizer. Exact Q4 anchor isolates zero cubic terms plus lower runtime cost.'
    if wave == 9:
        report['limitations'][-1] = 'Calibrated heads share exact frozen Q4/data/fresh Adam and search-pawn objective. Quad/cubic eight-head spaces are not nested; affine is their common adapted control. BRE retains a distinct historical split and raw objective. No extra half scaling is applied.'
    if followup:
        report["limitations"].append("Compact degree mask is unadapted and retains cubic cache cost; no adapted Q4 strength conclusion.")
    if scale:
        report["limitations"].append("Separate prospectively fixed scale.25 sensitivity on reused G9 development roots; original192-game native protocol preserved. No scale grid or refitting. This changes magnitude/search interaction, not mathematical information.")
    with output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps({key: report[key] for key in
                     ("totalGames", "validatedMoves", "maximumSampledWorkingSetMiB")}))


if __name__ == "__main__":
    summarize(*sys.argv[1:])
