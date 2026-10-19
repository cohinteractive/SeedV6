"""One background-priority JVM; preserve commands, binary hashes and all exits.

Run from repository root. All raw paths are new and must not already exist.
"""
import argparse
import hashlib
import json
import pathlib
import subprocess
import time

p = argparse.ArgumentParser()
p.add_argument("name")
p.add_argument("--workers", type=int, default=1)
p.add_argument("--warmups", type=int, default=2)
p.add_argument("--rounds", type=int, default=3)
p.add_argument("--roots", type=int, default=8)
p.add_argument("--budget", type=int, default=500000)
p.add_argument("--depth", type=int, default=20)
p.add_argument("--mode", choices=["recursive", "flat", "both", "full", "full-both"], default="both")
p.add_argument("--corpus", default="docs/research/search/sr005-phase2-2026-10-13/diagnostic-corpus.tsv")
p.add_argument("--profile", action="store_true")
a = p.parse_args()
base = pathlib.Path(__file__).resolve().parent
raw = base / "raw"
raw.mkdir(exist_ok=True)
stem = raw / a.name
paths = {suffix: stem.with_suffix(suffix) for suffix in [".jsonl", ".log", ".execution.json", ".jfr", ".jit.xml"]}
if any(path.exists() for path in paths.values()):
    raise SystemExit("Refusing to overwrite an experiment")
cmd = ["C:/java/jdk-21/bin/java.exe", "--add-modules=jdk.incubator.vector",
       "-Djava.awt.headless=true", "-Xms256m", "-Xmx768m", "-XX:ActiveProcessorCount=2"]
if a.profile:
    cmd += ["-XX:StartFlightRecording=filename=" + str(paths[".jfr"]) + ",settings=profile,dumponexit=true",
            "-XX:NativeMemoryTracking=summary", "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+PrintNMTStatistics", "-XX:+LogCompilation", "-XX:LogFile=" + str(paths[".jit.xml"])]
cmd += ["-cp", "research/build/install/research/lib/*",
        "com.ohinteractive.seedv6.search.exact.Sr002ExecutionStudy",
        "build-assets/production/bre-pair2-g32.brn-pair2", a.corpus, str(paths[".jsonl"]),
        str(a.workers), str(a.warmups), str(a.rounds), str(a.depth), str(a.budget), str(a.roots), a.mode]
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()
receipt = {"command": cmd, "parameters": vars(a),
           "runtime": {str(f): digest(f) for f in sorted(pathlib.Path("research/build/install/research/lib").glob("*.jar"))},
           "corpusSha256": digest(pathlib.Path(a.corpus)),
           "sourceHead": subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip(),
           "dirty": subprocess.check_output(["git", "status", "--porcelain"], text=True),
           "priority": "BELOW_NORMAL_PRIORITY_CLASS", "startedUtc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}
paths[".execution.json"].write_text(json.dumps(receipt, indent=2) + "\n")
start = time.monotonic()
with paths[".log"].open("xb") as log:
    process = subprocess.Popen(cmd, stdout=log, stderr=subprocess.STDOUT,
                               creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS | subprocess.CREATE_NO_WINDOW)
    try:
        code = process.wait(timeout=1800)
    except subprocess.TimeoutExpired:
        process.kill()
        code = process.wait()
        receipt["timeout"] = True
receipt.update(exitCode=code, runnerElapsedSeconds=time.monotonic()-start,
               endedUtc=time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()))
receipt["outputHashes"] = {str(f): digest(f) for f in paths.values() if f.exists() and f != paths[".execution.json"]}
paths[".execution.json"].write_text(json.dumps(receipt, indent=2) + "\n")
print(json.dumps({"name": a.name, "exitCode": code, "elapsedSeconds": receipt["runnerElapsedSeconds"]}))
raise SystemExit(code)
