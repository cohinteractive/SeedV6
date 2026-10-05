"""Summarize E013's retained observations without selecting or dropping match results."""
import argparse
import json
from pathlib import Path


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def summary(rows):
    scores = [r.get("nnueScore") for r in rows]
    scored = [x for x in scores if x is not None]
    return dict(games=len(rows), wins=scores.count(1), draws=scores.count(.5),
                losses=scores.count(0), unscored=scores.count(None),
                score=sum(scored) / len(scored) if scored else None,
                terminalOpenings=sum(r["plies"] == 0 for r in rows),
                candidateNodes=sum(r["nodesNnueBrn"][0] for r in rows),
                opponentNodes=sum(r["nodesNnueBrn"][1] for r in rows))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    matches, raw = {}, {}
    for path in sorted(args.root.glob("e013-*/games.jsonl")):
        rows = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]
        config = read(path.with_name("config.json"))
        assert len(rows) == 2 * config["pairs"], path
        for pair in range(config["pairs"]):
            block = [r for r in rows if r["pair"] == pair]
            assert len(block) == 2 and len({r["openingHash"] for r in block}) == 1, path
            assert {r["nnueColor"] for r in block} == {0, 1}, path
        matches[path.parent.name] = dict(config=config, **summary(rows))
        raw[path.parent.name] = rows

    controls = ["e013-full-g1-depth4", "e013-full-g1-confirmation"]
    gains = ["e013-full-g1-gain-control", "e013-full-g1-gain-confirmation"]
    paired = []
    for control, gain in zip(controls, gains):
        if control not in raw or gain not in raw:
            continue
        a = {(r["openingHash"], r["nnueColor"]): r for r in raw[control]}
        b = {(r["openingHash"], r["nnueColor"]): r for r in raw[gain]}
        assert a.keys() == b.keys()
        for opening in sorted({key[0] for key in a}):
            change = sum(b[(opening, c)]["nnueScore"] - a[(opening, c)]["nnueScore"] for c in (0, 1)) / 2
            paired.append(dict(block=control, opening=opening, scoreChange=change))
    result = dict(matches=matches,
                  fullDepth4=summary(sum((raw.get(name, []) for name in controls), [])),
                  diagnosticQuarterGain=summary(sum((raw.get(name, []) for name in gains), [])),
                  pairedGainChanges=paired,
                  note="Candidate/opponent nodes are on different visited positions. No timing, NPS or Elo inference. Caps remain unscored; terminal opening pairs remain present.")
    with args.output.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(result, stream, indent=2)
        stream.write("\n")
    print(json.dumps({k: v for k, v in result.items() if k in ("fullDepth4", "diagnosticQuarterGain")}, indent=2))


if __name__ == "__main__":
    main()
