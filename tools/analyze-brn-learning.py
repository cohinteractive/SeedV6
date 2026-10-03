"""Summarize isolated BRN learning probes without changing their raw evidence."""
import json
import hashlib
import math
import random
import shutil
import statistics
import xml.etree.ElementTree as ET
from pathlib import Path


def matches(root):
    for path in sorted(root.glob("*/match.json")):
        data = json.loads(path.read_text())
        games = [p[c] for p in data["pairs"] for c in ("white", "black")]
        complete = [g for g in games if g.get("candidateScore") is not None]
        decided_score = sum(g["candidateScore"] for g in complete)
        totals = len(games)
        # Worst/best outcomes for administrative stops. Never assign them draws.
        bounds = [decided_score / totals, (decided_score + totals-len(complete))/totals]
        plies = [p for g in games for p in g["plies"]]
        candidate_plies = [p for p in plies if p["actor"] == 0]
        print(json.dumps({"run": path.parent.name, "summary": data["summary"],
                          "allGameScoreIdentificationBounds": bounds,
                          "meanPlies": statistics.mean(len(g["plies"]) for g in games),
                          "candidateMaterialMean": statistics.mean(p["material"] for p in candidate_plies),
                          "candidateRootScoreMean": statistics.mean(p["score"] for p in candidate_plies)}, indent=2))


def first_loss(root):
    data = json.loads((root / "match.json").read_text())
    for pair in data["pairs"]:
        for color in ("white", "black"):
            game = pair[color]
            if game.get("candidateScore") != 0:
                continue
            print("FIRST LOSS", pair["index"], color)
            for i, p in enumerate(game["plies"][:60]):
                print(i, p["actor"], p["score"], round(p["material"], 2), round(p["trained"], 2),
                      round(p["afterMaterial"], 2), round(p["afterTrained"], 2), p["fen"], p["move"])
            return


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def compact_match(path):
    data = json.loads(path.read_text())
    pairs = data.pop("pairs")
    complete = [p for p in pairs if all(p[c].get("candidateScore") is not None for c in ("white", "black"))]
    scores = [(p["white"]["candidateScore"] + p["black"]["candidateScore"])/2 for p in complete]
    assert len(complete) == data["summary"]["completedPairs"]
    if scores:
        assert math.isclose(statistics.mean(scores), data["summary"]["pointScore"])
    data["rawReportSha256"] = digest(path)
    data["requestedPairs"] = int(data["arguments"][3])
    data["recordedPairs"] = len(pairs)
    data["allRequestedPairsRecorded"] = len(pairs) == data["requestedPairs"]
    data["completePairWdl"] = {str(s): sum(p[c]["candidateScore"] == s for p in complete for c in ("white", "black")) for s in (1, .5, 0)}
    data["pairs"] = []
    for pair in pairs:
        row = {k: v for k, v in pair.items() if k not in ("white", "black")}
        for color in ("white", "black"):
            game = pair[color]
            row[color] = {k: v for k, v in game.items() if k != "plies"}
            row[color]["plies"] = len(game["plies"])
        data["pairs"].append(row)
    return data


def export_evidence(root, destination):
    """Keep outcomes/provenance without checking large corpora, weights or traces into Git."""
    destination.mkdir(parents=True, exist_ok=False)
    index = {"schema": "brn-learning-evidence-v1", "rawRoot": str(root), "files": []}
    runs = {}
    for path in sorted(root.glob("*/match.json")):
        name = path.parent.name
        runs[name] = compact_match(path)
        write_json(destination / (name + ".json"), runs[name])
        index["files"].append({"source": str(path), "sourceSha256": digest(path), "output": name + ".json", "transformation": "Per-ply traces omitted; complete pair score independently recomputed; raw summary retained"})
    reports = ["windows-lineage-v2/provenance.json", "windows-lineage-v2/generations-v1.tsv",
               "mechanism-g9/mechanism.json", "prospective-g9/continuation.json",
               "data-prospective-v2/manifest.json", "threads-g9/threads.json",
               "search128-g9/diagnostics.json", "preservation-final.json", "version-boundary.json"]
    reports += [str(p.relative_to(root)).replace("\\", "/") for p in sorted(root.glob("qsearch-*/qsearch.json"))]
    reports += [str(p.relative_to(root)).replace("\\", "/") for p in sorted(root.glob("diagnose*/diagnostics.json"))]
    for relative in reports:
        path = root / relative
        if not path.exists():
            raise FileNotFoundError(path)
        name = relative.replace("/", "--")
        shutil.copyfile(path, destination / name)
        index["files"].append({"source": str(path), "sourceSha256": digest(path), "output": name, "transformation": "Byte-for-byte copy"})

    base = runs["confirm-g9-d4-quarter-64"]["pairs"]
    comparisons = []
    for run in ("confirm-prospective1-d4-quarter-64", "confirm-prospective2-d4-quarter-64"):
        later = runs[run]["pairs"]
        assert len(base) == len(later) == 64
        assert [p["openingHash"] for p in base] == [p["openingHash"] for p in later]
        deltas = [(sum(p[c]["candidateScore"] for c in ("white", "black")) - sum(b[c]["candidateScore"] for c in ("white", "black")))/2 for b, p in zip(base, later)]
        rng = random.Random(831907)
        boots = sorted(statistics.mean(rng.choices(deltas, k=64)) for _ in range(10000))
        comparisons.append({"baseline": "confirm-g9-d4-quarter-64", "later": run,
                            "meanPairedScoreDifference": statistics.mean(deltas),
                            "pairedBootstrap95": [boots[249], boots[9749]],
                            "method": "10000 opening-pair bootstrap replicates; Python Random seed831907; descriptive retention comparison, not proof of monotonic gain"})
    write_json(destination / "paired-continuation.json", comparisons)

    full = runs["match-g9-d4-full-16"]["pairs"]
    quarter = runs["match-g9-d4-quarter-16"]["pairs"]
    assert [p["openingHash"] for p in full] == [p["openingHash"] for p in quarter]
    contrast = []
    for unknown_full_score in (1, 0):
        deltas = []
        for original, calibrated in zip(full, quarter):
            original_scores = [original[c].get("candidateScore") for c in ("white", "black")]
            deltas.append((sum(calibrated[c]["candidateScore"] for c in ("white", "black")) - sum(unknown_full_score if s is None else s for s in original_scores))/2)
        rng = random.Random(620391)
        boots = sorted(statistics.mean(rng.choices(deltas, k=len(deltas))) for _ in range(10000))
        contrast.append({"hypotheticalFullScoreForUnknownCaps": unknown_full_score,
                         "quarterMinusFullMean": statistics.mean(deltas),
                         "pairedBootstrap95": [boots[249], boots[9749]]})
    write_json(destination / "exploratory-paired-calibration.json", {
        "note": "Same16openings/frozenGen9; bounds, not assigned outcomes. Exploratory selection data. Worst-case bootstrap includes zero; use independent confirmation for retained gain over Gen0.",
        "contrasts": contrast})

    failed = json.loads((root / "confirm-v1-97-d4-quarter-64/match.json").read_text())
    replay = json.loads((root / "cap-followup-97-opening23/match.json").read_text())["pairs"][0]
    original = next(p for p in failed["pairs"] if p["index"] == 23)
    checks = {color: original[color]["plies"] == replay[color]["plies"][:len(original[color]["plies"])] for color in ("white", "black")}
    assert all(checks.values())
    write_json(destination / "cap-replay-check.json", {"first1024PliesExactlyMatch": checks,
               "failedAttemptPreserved": True, "replayBlackTermination": replay["black"]["termination"],
               "replayBlackScore": replay["black"]["candidateScore"], "replayBlackPlies": len(replay["black"]["plies"])})

    qa = []
    for path in sorted(root.glob("software-qa-*/*.xml")):
        suite = ET.parse(path).getroot()
        qa.append({"path": str(path), "sha256": digest(path), **suite.attrib,
                   "cases": [dict(case.attrib) for case in suite.findall("testcase")]})
        assert int(suite.get("failures", 0)) == int(suite.get("errors", 0)) == 0
    write_json(destination / "software-qa.json", qa)
    gui = root.parent / "brn-programme/brn3-gui.png"
    shutil.copyfile(gui, destination / "brn3-network-tab.png")
    index["files"].append({"source": str(gui), "sourceSha256": digest(gui),
                           "output": "brn3-network-tab.png", "transformation": "Headless Swing render copied byte-for-byte; visually inspected at 1000x850"})
    index["outputs"] = [{"path": p.name, "sha256": digest(p), "bytes": p.stat().st_size} for p in sorted(destination.iterdir())]
    write_json(destination / "evidence-index.json", index)
    print(json.dumps({"destination": str(destination), "matchRuns": len(runs), "files": len(index["outputs"])}))


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("--first-loss", action="store_true")
    parser.add_argument("--export", type=Path, help="Create a new compact evidence directory; refuses overwrite")
    args = parser.parse_args()
    if args.export:
        export_evidence(args.root, args.export)
    else:
        (first_loss if args.first_loss else matches)(args.root)
