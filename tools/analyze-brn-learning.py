"""Summarize isolated BRN learning probes without changing their raw evidence."""
import json
import statistics
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



if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--first-loss", action="store_true")
    args = parser.parse_args()
    if not args.root.is_dir():
        parser.error("Research source directory is unavailable: " + str(args.root))
    if args.first_loss:
        if not (args.root / "match.json").is_file():
            parser.error("Required match.json is unavailable")
    elif not any(args.root.glob("*/match.json")):
        parser.error("No match.json inputs found; historical inputs may have been removed")
    (first_loss if args.first_loss else matches)(args.root)
