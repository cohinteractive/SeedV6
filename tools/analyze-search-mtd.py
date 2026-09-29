"""Summarize SR-004 raw evidence, without optional dependencies.

Usage: python tools/analyze-search-mtd.py build/sr004
Totals include every timed repetition and all iterative depths/passes. Node
counts are production admitted children; raw visited nodes are also retained.
"""
import csv
import json
import statistics as st
import sys
from collections import Counter, defaultdict
from pathlib import Path


def analyze(directory):
    if not (directory / "verified.txt").is_file():
        raise ValueError("The empirical run has not passed its completion checks")
    def read(name):
        with (directory / name).open(newline="", encoding="utf-8") as source:
            return list(csv.DictReader(source))

    requests = read("requests.csv")
    iterations = read("iterations.csv")
    passes = read("passes.csv")
    policies = ["CONTROL", "MTD_PREV", "MTD_ORACLE"]
    output = {"policies": {}, "positions": {}, "repetitions": {}}
    total = lambda rows, field: sum(int(r[field]) for r in rows)
    for policy in policies:
        rs = [r for r in requests if r["policy"] == policy]
        its = [r for r in iterations if r["policy"] == policy]
        ps = [r for r in passes if r["policy"] == policy]
        zeros = [r for r in ps if r["phase"] == "zero"]
        # Pass/error distributions count each distinct position/depth once.
        unique = [r for r in its if r["repeat"] == "1" and r["initial_guess"]]
        nodes, ns = total(rs, "nodes"), total(rs, "elapsed_ns")
        p = output["policies"][policy] = {
            "requests": len(rs), "iterations": len(its), "nodes": nodes,
            "seconds": ns / 1e9, "nps": nodes * 1e9 / ns,
            "iteration_seconds": total(its, "elapsed_ns") / 1e9,
            "pass_seconds": total(ps, "elapsed_ns") / 1e9,
            "visited_nodes": total(ps, "visited_nodes"),
            "zero_nodes": total(zeros, "nodes"), "zero_seconds": total(zeros, "elapsed_ns") / 1e9,
            "materialize_nodes": total(its, "materialize_nodes"),
            "materialize_seconds": total(its, "materialize_ns") / 1e9,
            "pass_counts": dict(sorted(Counter(int(r["zero_passes"]) for r in unique).items())),
            "patterns": dict(Counter("".join("H" if a["classification"] == "fail-high" else "L"
                for a in zeros if a["repeat"] == "1" and a["position"] == r["position"]
                and a["depth"] == r["requested_depth"]) for r in unique)),
        }
        if unique:
            errors = sorted(int(r["absolute_guess_error"]) for r in unique)
            counts = [int(r["zero_passes"]) for r in unique]
            p.update(guess_count=len(unique), mean_absolute_error=st.mean(errors), median_absolute_error=st.median(errors),
                     max_absolute_error=max(errors), mean_signed_error=st.mean(int(r["signed_guess_error"]) for r in unique),
                     zero_error_count=errors.count(0), mean_passes=st.mean(counts), median_passes=st.median(counts), max_passes=max(counts))
            p["largest_errors"] = sorted(({k: r[k] for k in ("position", "requested_depth", "initial_guess", "final_score", "absolute_guess_error", "zero_passes")}
                                          for r in unique), key=lambda r: int(r["absolute_guess_error"]), reverse=True)[:6]
            later_cheaper = later_greater = equal = 0
            transitions = defaultdict(lambda: [0, 0, 0])
            for r in unique:
                seq = [a for a in zeros if a["repeat"] == "1" and a["position"] == r["position"] and a["depth"] == r["requested_depth"]]
                for a, b in zip(seq, seq[1:]):
                    x, y = int(a["nodes"]), int(b["nodes"])
                    later_cheaper += y < x; later_greater += y > x; equal += y == x
                    v = transitions[a["classification"] + " -> " + b["classification"]]
                    v[0] += 1; v[1] += x; v[2] += y
            p["pass_work_transitions"] = dict(cheaper=later_cheaper, dearer=later_greater, equal=equal, by_class=dict(transitions))
        for rep in sorted({r["repeat"] for r in rs}):
            rr = [r for r in rs if r["repeat"] == rep]
            output["repetitions"].setdefault(rep, {})[policy] = dict(nodes=total(rr, "nodes"), seconds=total(rr, "elapsed_ns") / 1e9)
        for position in dict.fromkeys(r["position"] for r in rs):
            rr = [r for r in rs if r["position"] == position]
            ii = [r for r in its if r["position"] == position]
            output["positions"].setdefault(position, {})[policy] = dict(nodes=total(rr, "nodes"), seconds=total(rr, "elapsed_ns") / 1e9,
                materialize_nodes=total(ii, "materialize_nodes"), zero_passes=total(ii, "zero_passes"))
    base = output["policies"]["CONTROL"]
    for name, p in output["policies"].items():
        p["node_change_percent"] = (p["nodes"] / base["nodes"] - 1) * 100
        p["time_change_percent"] = (p["seconds"] / base["seconds"] - 1) * 100
        p["materialize_node_percent"] = p["materialize_nodes"] / p["nodes"] * 100
        p["materialize_time_percent"] = p["materialize_seconds"] / p["seconds"] * 100
    for name, ps in output["positions"].items():
        for policy, p in ps.items():
            p["node_delta"] = p["nodes"] - ps["CONTROL"]["nodes"]
            p["node_change_percent"] = (p["nodes"] / ps["CONTROL"]["nodes"] - 1) * 100 if ps["CONTROL"]["nodes"] else 0
            p["time_change_percent"] = (p["seconds"] / ps["CONTROL"]["seconds"] - 1) * 100
    # Cross-check all aggregation layers rather than trusting summary output.
    for policy in policies:
        p = output["policies"][policy]
        assert p["nodes"] == total([r for r in iterations if r["policy"] == policy], "nodes")
        assert p["nodes"] == total([r for r in passes if r["policy"] == policy], "nodes")
    (directory / "summary.json").write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")
    with (directory / "summary.csv").open("w", newline="", encoding="utf-8") as target:
        writer = csv.writer(target)
        writer.writerow(["position", "policy", "nodes", "seconds", "node_change_percent", "time_change_percent", "materialize_nodes", "zero_passes"])
        for name, ps in output["positions"].items():
            for policy, p in ps.items():
                writer.writerow([name, policy] + [p[k] for k in ("nodes", "seconds", "node_change_percent", "time_change_percent", "materialize_nodes", "zero_passes")])
    # Pattern details remain in JSON, keeping console output bounded.
    print(json.dumps({name: {k: v for k, v in p.items() if k not in ("patterns", "pass_counts")}
                      for name, p in output["policies"].items()}, indent=2))


if __name__ == "__main__":
    analyze(Path(sys.argv[1]))
