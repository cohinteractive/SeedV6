#!/usr/bin/env python3
"""Summarize immutable CGLHW bootstrap artifacts; caps never become draws.

Only standard-library dependencies. Run from the repository root. Match scores
are descriptive: common openings and repeated seeds invalidate a naive 96-game
binomial confidence interval. Zero empirical variance is not zero uncertainty.
"""
import collections
import json
from pathlib import Path
import statistics
import sys


def rows(path):
    return [json.loads(line) for line in (path / "results.jsonl").read_text().splitlines()]


def matches(path):
    records = rows(path)
    by_seed = collections.defaultdict(list)
    all_games = []
    complete_pairs = []
    for row in records:
        games = [row["first"], row["second"]]
        by_seed[row["seed"]].extend(games)
        all_games.extend(games)
        if all(g["nnueScore"] is not None for g in games):
            complete_pairs.append(sum(g["nnueScore"] for g in games) / 2)

    def summary(games):
        known = [g["nnueScore"] for g in games if g["nnueScore"] is not None]
        missing = len(games) - len(known)
        return {
            "games": len(games), "wins": known.count(1), "draws": known.count(.5),
            "losses": known.count(0), "unscored": missing,
            "scoreOnCompletedGamesOnly": statistics.mean(known) if known else None,
            "allScheduledScoreBounds": [sum(known)/len(games), (sum(known)+missing)/len(games)],
            "terminations": dict(collections.Counter(g["termination"] for g in games)),
            "meanPlies": statistics.mean(g["plies"] for g in games),
        }

    result = summary(all_games)
    result["bySeed"] = {k: summary(v) for k, v in by_seed.items()}
    result["completePairs"] = len(complete_pairs)
    result["incompletePairs"] = len(records) - len(complete_pairs)
    result["completePairMean"] = statistics.mean(complete_pairs) if complete_pairs else None
    result["totalNodesNnueBrn"] = [sum(g["nodesNnueBrn"][i] for g in all_games) for i in range(2)]
    result["totalSearchSecondsNnueBrn"] = [sum(g["nanosNnueBrn"][i] for g in all_games)/1e9 for i in range(2)]
    result["uncertainty"] = "Six initialization seeds crossed with eight shared openings; no independent-game CI. Zero sample variance at depth3 is not proof of zero population variance. Timings include JIT/GC; matches are strength/node evidence, not controlled NPS benchmarks."
    return result


def audit(path):
    data = rows(path)
    audits = [r["data"] for r in data if r["type"] == "audit"]
    cost = [r["data"] for r in data if r["type"] == "performance"]
    report = {
        "seeds": [r["seed"] for r in audits],
        "positionsPerSeed": [r["positions"] for r in audits],
        "brnReadoutMaxAbs": max(r["brnReadoutMaxAbs"] for r in audits),
        "brnMaterialMaxErrorPawns": max(r["brnMaterialMaxErrorPawns"] for r in audits),
        "nnueIncrementalMaxRawError": max(r["nnueIncrementalMaxRawError"] for r in audits),
        "nnueParameters": audits[0]["nnueParameters"],
        "brnInferenceParameters": audits[0]["brnInferenceParameters"],
        "brnTrainingParameters": audits[0]["brnTrainingParameters"],
        "nnueMappedScoreRange": [min(min(r["nnueMappedScores"]) for r in audits), max(max(r["nnueMappedScores"]) for r in audits)],
        "medianCostNsAcrossSeedMedians": {
            k: statistics.median(r[k]["medianNs"] for r in cost)
            for k in cost[0] if isinstance(cost[0][k], dict)
        } if cost else {},
        "activationRanges": {
            k: [min(r[k] for r in audits), max(r[k] for r in audits)]
            for k in audits[0] if "Fraction" in k
        },
        "topology": {
            key: {"perSeed": [r["topology"][key] for r in audits]}
            for key in audits[0]["topology"]
        },
    }
    return report


if __name__ == "__main__":
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("docs/research/nnue-cglhw")
    result = {}
    for name in ("baseline-audit", "baseline-topology"):
        if (root / name / "results.jsonl").exists():
            result[name] = audit(root / name)
    for name in ("baseline-depth2", "baseline-depth3"):
        if (root / name / "results.jsonl").exists():
            result[name] = matches(root / name)
    print(json.dumps(result, indent=2))
