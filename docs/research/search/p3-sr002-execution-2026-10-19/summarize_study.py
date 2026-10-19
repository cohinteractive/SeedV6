"""Compact, reproducible mechanical-cost summary; no Elo estimate.

Usage: summarize_study.py NEW-SUMMARY.json RAW.jsonl [RAW.jsonl ...]
Paired uncertainty resamples position clusters, keeping all rounds/forks together.
"""
import collections
import hashlib
import json
import math
import pathlib
import random
import statistics
import sys

target=pathlib.Path(sys.argv[1])
if target.exists():
    raise SystemExit("Refusing to replace summary")
groups=collections.defaultdict(dict)
rows=[]
sources=[]
for name in sys.argv[2:]:
    path=pathlib.Path(name)
    data=[json.loads(line) for line in path.read_text(encoding="utf-8-sig").splitlines()]
    if data[-1].get("status")!="passed":
        raise SystemExit("Incomplete evidence: "+name)
    sources.append({"path":name,"sha256":hashlib.sha256(path.read_bytes()).hexdigest(),"identity":data[0]})
    for row in data:
        if row.get("type")=="request" and row["round"]>=0:
            rows.append(row)
            groups[(name,row["round"],row["id"])][row.get("mode",0)]=row
result={"sources":sources,"measuredRequests":len(rows),"modes":{}}
for mode in sorted({r.get("mode",0) for r in rows}):
    subset=[r for r in rows if r.get("mode",0)==mode]
    nodes=sum(r["outcome"]["nodes"] for r in subset)
    result["modes"][mode]={
        "requests":len(subset),"nodes":nodes,
        "qFraction":sum(r["outcome"]["diagnostics"]["worker"]["nodes"]["qNodes"] for r in subset)/nodes,
        "wallSeconds":sum(r["elapsedNanos"] for r in subset)/1e9,
        "processCpuSeconds":sum(r["processCpuNanos"] for r in subset)/1e9,
        "ownerCpuSeconds":sum(r["ownerCpuNanos"] for r in subset)/1e9,
        "medianAllocatedBytesPerRequest":statistics.median(r["liveThreadAllocatedBytes"] for r in subset),
        "allocatedBytesPerNode":sum(r["liveThreadAllocatedBytes"] for r in subset)/nodes,
        "gcCollections":sum(r["gcCollections"] for r in subset),
        "compilationMillis":sum(r["compilationMillis"] for r in subset),
        "maximumObservedHeapUsed":max(r["heapUsed"] for r in subset),
        "maximumObservedPly":max(r["outcome"]["diagnostics"]["worker"]["nodes"]["maximumAbsolutePly"] for r in subset),
        "maximumObservedQply":max(r["outcome"]["diagnostics"]["worker"]["nodes"]["maximumQply"] for r in subset)}
candidate=2 if any(2 in v for v in groups.values()) else 1
pairs=[{0:v[0],1:v[candidate]} for v in groups.values() if 0 in v and candidate in v]
result["candidateMode"]=candidate
if pairs:
    result["pairedRequests"]=len(pairs)
    for metric in ["elapsedNanos","processCpuNanos","ownerCpuNanos","liveThreadAllocatedBytes"]:
        clusters=collections.defaultdict(list)
        for pair in pairs:
            if pair[0][metric]>0 and pair[1][metric]>0:
                clusters[pair[0]["id"]].append(math.log(pair[1][metric]/pair[0][metric]))
        values=[statistics.mean(v) for v in clusters.values()]
        rng=random.Random(200219)
        boot=sorted(math.exp(statistics.mean(rng.choices(values,k=len(values)))) for _ in range(10000))
        result[metric]={"geometricFlatOverRecursive":math.exp(statistics.mean(values)),
                        "positionClusterPercentile95":[boot[249],boot[9749]],
                        "positionCount":len(values),
                        "perPosition":{k:math.exp(statistics.mean(v)) for k,v in clusters.items()},
                        "aggregateFlatOverRecursive":sum(p[1][metric] for p in pairs)/sum(p[0][metric] for p in pairs)}
    result["identicalOutcomes"]=all(p[0]["outcome"]==p[1]["outcome"] for p in pairs)
result["limits"]="Position-cluster percentile bootstrap is approximate, conditional on fixed host/corpus/JVM; no platform generalization. Shared-JVM carryover, low-priority contention, coarse process CPU and live-thread allocation limits remain. Profiling runs are not clean timing evidence."
target.write_text(json.dumps(result,indent=2)+"\n")
print(json.dumps({k:v for k,v in result.items() if k not in ["sources"] and not isinstance(v,dict)},indent=2))
for metric in ["elapsedNanos","processCpuNanos"]:
    if metric in result:
        print(metric,json.dumps({k:v for k,v in result[metric].items() if k!="perPosition"}))
