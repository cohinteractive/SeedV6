"""Summarize SR-004 raw evidence, without optional dependencies.

Usage: python tools/analyze-search-mtd.py build/sr004
Totals include every timed repetition and all iterative depths/passes. Node
counts are production admitted children; raw visited nodes are also retained.
"""
import csv
import io
import json
import statistics as st
import sys
import zipfile
from collections import Counter, defaultdict
from pathlib import Path


def distribution(values):
    values = sorted(values)
    return dict(count=len(values), minimum=min(values), q1=values[len(values) // 4],
                median=st.median(values), q3=values[3 * len(values) // 4], maximum=max(values), mean=st.mean(values))


def diagnose(archive):
    """Phase 1 reads the committed unit-one archive without rerunning Search.

    Distribution samples are distinct targets (repeat one). Costs aggregate all
    three repetitions. Quartiles use sorted index floor(n/4), floor(3*n/4).
    """
    with zipfile.ZipFile(archive) as source:
        def read(name):
            return list(csv.DictReader(io.StringIO(source.read(name).decode("utf-8"))))
        iterations, passes = read("iterations.csv"), read("passes.csv")
    targets = [r for r in iterations if r["policy"] == "MTD_PREV" and r["repeat"] == "1" and r["initial_guess"]]
    sequences = defaultdict(list)
    for r in passes:
        if r["policy"] == "MTD_PREV" and r["phase"] == "zero":
            sequences[r["repeat"], r["position"], r["depth"]].append(r)
    errors = [int(r["signed_guess_error"]) for r in targets]
    result = dict(source=str(archive), signed_errors=distribution(errors),
                  error_counts=dict(Counter("positive" if x > 0 else "negative" if x < 0 else "zero" for x in errors)),
                  absolute_buckets={"zero": errors.count(0), "one": sum(abs(x) == 1 for x in errors),
                                    **{"at_most_" + str(n): sum(abs(x) <= n for x in errors) for n in (4, 16, 64)},
                                    "over_64": sum(abs(x) > 64 for x in errors)})
    result["passes_by_error_sign"] = {name: distribution([int(r["zero_passes"]) for r in targets if predicate(int(r["signed_guess_error"]))])
                                     for name, predicate in [("positive", lambda x: x > 0), ("negative", lambda x: x < 0), ("zero", lambda x: x == 0)]}
    gaps, patterns, directions, details = [], Counter(), Counter(), []
    for r in targets:
        seq = sequences["1", r["position"], r["requested_depth"]]
        pattern = "".join("H" if a["classification"] == "fail-high" else "L" for a in seq)
        switches = sum(a != b for a, b in zip(pattern, pattern[1:]))
        patterns[pattern[0] + "_first_" + str(switches) + "_switches"] += 1
        directions.update(pattern)
        for a in seq:
            gaps.append(int(a["score"]) - int(a["beta"]) if a["classification"] == "fail-high" else int(a["alpha"]) - int(a["score"]))
        details.append(dict(position=r["position"], depth=int(r["requested_depth"]), error=int(r["signed_guess_error"]),
                            passes=len(seq), pattern=pattern, first_two_nodes=sum(int(a["nodes"]) for a in seq[:2])))
    exact_two = [r for r in targets if int(r["zero_passes"]) <= 2]
    non_two = {(r["position"], r["requested_depth"]) for r in targets if int(r["zero_passes"]) > 2}
    prefix = [a for key, seq in sequences.items() if key[1:] in non_two for a in seq[:2]]
    all_passes = [a for key, seq in sequences.items() if key[1:] in non_two for a in seq]
    controls = [r for r in iterations if r["policy"] == "CONTROL" and (r["position"], r["requested_depth"]) in non_two]
    result.update(patterns=dict(patterns), directions=dict(directions), exact_in_two=len(exact_two),
                  exact_in_two_zero_error=sum(int(r["signed_guess_error"]) == 0 for r in exact_two),
                  failsoft_gap=distribution(gaps), gap_at_most={n: sum(g <= n for g in gaps) for n in (0, 1, 4, 16, 64)},
                  non_two=dict(count=len(non_two), first_two_nodes=sum(int(a["nodes"]) for a in prefix),
                               first_two_seconds=sum(int(a["elapsed_ns"]) for a in prefix) / 1e9,
                               all_zero_nodes=sum(int(a["nodes"]) for a in all_passes),
                               matching_control_nodes=sum(int(a["nodes"]) for a in controls)), targets=details)
    print(json.dumps(result, indent=2))


def analyze(directory):
    if not (directory / "verified.txt").is_file():
        raise ValueError("The empirical run has not passed its completion checks")
    def read(name):
        with (directory / name).open(newline="", encoding="utf-8") as source:
            return list(csv.DictReader(source))

    requests = read("requests.csv")
    iterations = read("iterations.csv")
    passes = read("passes.csv")
    policies = list(dict.fromkeys(r["policy"] for r in iterations))
    output = {"policies": {}, "positions": {}, "repetitions": {}}
    total = lambda rows, field: sum(int(r[field]) for r in rows)
    for policy in policies:
        rs = [r for r in requests if r["policy"] == policy]
        its = [r for r in iterations if r["policy"] == policy]
        ps = [r for r in passes if r["policy"] == policy]
        zeros = [r for r in ps if r["phase"] in ("zero", "bracket", "bisect")]
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
        p["phases"] = {phase: dict(passes=len(rr), nodes=total(rr, "nodes"), seconds=total(rr, "elapsed_ns") / 1e9)
                       for phase in dict.fromkeys(a["phase"] for a in ps)
                       for rr in [[a for a in ps if a["phase"] == phase]]}
        p["directions"] = dict(Counter(a["classification"] for a in zeros if a["repeat"] == "1"))
        if policy == "MTD_TWO_PASS":
            p["completion_groups"] = {}
            for phase in ("materialize", "fallback"):
                keys = {(a["repeat"], a["position"], a["depth"]) for a in ps if a["phase"] == phase}
                group_iterations = [r for r in its if (r["repeat"], r["position"], r["requested_depth"]) in keys]
                controls = [r for r in iterations if r["policy"] == "CONTROL" and (r["repeat"], r["position"], r["requested_depth"]) in keys]
                group_passes = [a for a in ps if (a["repeat"], a["position"], a["depth"]) in keys]
                p["completion_groups"][phase] = dict(targets=sum(r["repeat"] == "1" for r in group_iterations),
                    nodes=total(group_iterations, "nodes"), seconds=total(group_iterations, "elapsed_ns") / 1e9,
                    control_nodes=total(controls, "nodes"), control_seconds=total(controls, "elapsed_ns") / 1e9,
                    probe_nodes=total([a for a in group_passes if a["phase"] == "zero"], "nodes"),
                    probe_seconds=total([a for a in group_passes if a["phase"] == "zero"], "elapsed_ns") / 1e9)
        if unique:
            errors = sorted(int(r["absolute_guess_error"]) for r in unique)
            counts = [int(r["zero_passes"]) for r in unique]
            p.update(guess_count=len(unique), mean_absolute_error=st.mean(errors), median_absolute_error=st.median(errors),
                     max_absolute_error=max(errors), mean_signed_error=st.mean(int(r["signed_guess_error"]) for r in unique),
                     zero_error_count=errors.count(0), mean_passes=st.mean(counts), median_passes=st.median(counts), max_passes=max(counts))
            p["passes_by_error_sign"] = {name: distribution([int(r["zero_passes"]) for r in unique if predicate(int(r["signed_guess_error"]))])
                                         for name, predicate in [("positive", lambda x: x > 0), ("negative", lambda x: x < 0), ("zero", lambda x: x == 0)]
                                         if any(predicate(int(r["signed_guess_error"])) for r in unique)}
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
    if sys.argv[1] == "--diagnose":
        diagnose(Path(sys.argv[2]))
    else:
        analyze(Path(sys.argv[1]))
