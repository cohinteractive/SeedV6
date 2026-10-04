#!/usr/bin/env python3
"""Summarize immutable CGLHW bootstrap artifacts; caps never become draws.

Only standard-library dependencies. Run from the repository root. Match scores
are descriptive: common openings and repeated seeds invalidate a naive 96-game
binomial confidence interval. Zero empirical variance is not zero uncertainty.
"""
import collections
import json
import math
import random
from pathlib import Path
import statistics
import sys


def rows(path):
    return [json.loads(line) for line in (path / "results.jsonl").read_text().splitlines()]


def matches(path, records=None):
    records = rows(path) if records is None else records
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
    result["uncertainty"] = f"{len(by_seed)} initialization seeds crossed with {len({r['index'] for r in records})} opening indices; no independent-game CI. Zero sample variance is not proof of zero population variance. Timings include JIT/GC and possible concurrent jobs; matches are strength/node evidence, not controlled NPS benchmarks."
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


def confirmation(records, metadata):
    """E005's predeclared block bound, only after a complete independent design."""
    variants = metadata["args"][2].split(",")
    seeds = metadata["seeds"]
    pair_count = int(metadata["args"][4])
    complete = len(records) == len(variants)*len(seeds)*pair_count
    expected = {(v, s, ordinal*pair_count+i) for v in variants
                for ordinal, s in enumerate(seeds) for i in range(pair_count)}
    actual = [(r["variant"], r["seed"], r["index"]) for r in records]
    complete = complete and len(set(actual)) == len(actual) and set(actual) == expected
    identities = {}
    for r in records:
        key = r["seed"], r["index"]
        if key in identities and identities[key] != r["openingHash"]:
            raise ValueError("Architectures did not receive the same opening/history")
        identities[key] = r["openingHash"]
    out = {"completeIndependentDesign": complete, "plannedBlocks": len(seeds),
           "familySize": len(variants), "alpha": .05,
           "assumption": "Independent randomly initialized model/opening blocks; paired colours stay in their block. This is a bound for tested architectures and the declared opening/search distribution, not all possible NNUEs."}
    if not complete:
        out["status"] = "Incomplete or nonconforming design; no confirmatory confidence claim"
        return out
    radius = math.sqrt(math.log(len(variants)/.05)/(2*len(seeds)))
    bounds = {}
    for variant in variants:
        scores = []
        for seed in seeds:
            games = [r[side] for r in records if r["variant"] == variant and r["seed"] == seed
                     for side in ("first", "second")]
            values = [1 if g["nnueScore"] is None else g["nnueScore"] for g in games]
            if any(v not in (0, .5, 1) for v in values):
                raise ValueError("Invalid chess result score")
            scores.append(statistics.mean(values))
        upper = min(1, statistics.mean(scores)+radius)
        bounds[variant] = {"blockScoresCapsAsWins": scores, "meanCapsAsWins": statistics.mean(scores),
                           "simultaneous95Upper": upper, "below045": upper < .45}
    out["hoeffdingRadius"] = radius
    out["variants"] = bounds
    out["allTestedArmsBelow045"] = all(v["below045"] for v in bounds.values())
    return out


def parity_comparison(control, corrected, expected_seeds):
    """Paired before/after prior ablation; resample whole initialization/opening blocks."""
    def index(records):
        result = {}
        for r in records:
            for side in ("first", "second"):
                g = r[side]
                key = (r["seed"], r["index"], g["nnueColor"])
                if key in result:
                    raise ValueError("Duplicate paired game")
                result[key] = (r["openingHash"], g["nnueScore"])
        return result
    a, b = index(control), index(corrected)
    if set(a) != set(b) or {k[0] for k in a} != set(expected_seeds):
        return {"completePairedDesign": False}
    blocks = collections.defaultdict(list)
    for k in a:
        if a[k][0] != b[k][0]:
            raise ValueError("Prior ablation changed opening/history")
        old, new = a[k][1], b[k][1]
        # Missing corrected outcomes lose; missing old outcomes win (and vice versa).
        blocks[k[0]].append(((0 if new is None else new)-(1 if old is None else old),
                             (1 if new is None else new)-(0 if old is None else old)))
    means = [(statistics.mean(x[0] for x in blocks[s]), statistics.mean(x[1] for x in blocks[s]))
             for s in expected_seeds]
    lower, upper = [m[0] for m in means], [m[1] for m in means]
    radius = math.sqrt(2*math.log(1/.05)/len(means))  # differences in [-1,1]
    rng = random.Random(2026100509)
    boot = sorted(statistics.mean(lower[rng.randrange(len(lower))] for _ in lower) for _ in range(20000))
    return {"completePairedDesign": True, "blocks": len(means), "gamesPerArm": len(a),
            "scoreImprovementBounds": [statistics.mean(lower), statistics.mean(upper)],
            "conservativeOneSided95ImprovementLower": max(-1, statistics.mean(lower)-radius),
            "approximateClusterBootstrap95ImprovementLower": boot[1000],
            "bootstrapSeed": 2026100509, "bootstrapResamples": 20000,
            "perSeedImprovementBounds": dict(zip(expected_seeds, means)),
            "scope": "Before/after fixed-material correction; same learned network and mapping. Resampling preserves all games/openings per seed. Approximate percentile bootstrap; no architecture acceptance or trained-capacity claim."}


if __name__ == "__main__":
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("docs/research/nnue-cglhw")
    result = {}
    for name in ("baseline-audit", "baseline-topology"):
        if (root / name / "results.jsonl").exists():
            result[name] = audit(root / name)
    for name in ("baseline-depth2", "baseline-depth3"):
        if (root / name / "results.jsonl").exists():
            result[name] = matches(root / name)
    for path in sorted(root.glob("e*-matches")):
        if not (path/"results.jsonl").exists():
            result[path.name] = {"status": "No match records; experiment did not reach matches"}
            if (path/"failure.json").exists():
                result[path.name]["failure"] = json.loads((path/"failure.json").read_text())
            continue
        records = rows(path)
        result[path.name] = {
            name: matches(None, [r for r in records if r["variant"] == name])
            for name in sorted({r["variant"] for r in records})
        }
        metadata = json.loads((path/"metadata.json").read_text())
        if metadata["args"][1] == "confirm":
            result[path.name]["confirmation"] = confirmation(records, metadata)
        result[path.name]["track"] = metadata.get("track", "B bootstrap-parity" if any(r["variant"].endswith("-material") for r in records) else "A knowledge-free architectural control")
        result[path.name]["accountingScope"] = "WDL and score bounds cover reported games; incomplete designs are not complete scheduled experiments. Inspect completion metadata before inference."
    parity = root/"e008-confirmation-matches"
    if (parity/"results.jsonl").exists():
        old = [r for r in rows(root/"e005-confirmation-matches") if r["variant"] == "baseline"]
        new = [r for r in rows(parity) if r["variant"] == "baseline-material"]
        seeds = json.loads((parity/"metadata.json").read_text())["seeds"]
        result["bootstrapParityCorrection"] = parity_comparison(old, new, seeds)
    print(json.dumps(result, indent=2))
