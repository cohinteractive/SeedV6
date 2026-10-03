"""Bounded, synchronous BRN throughput runner. Run after Gradle compilation.

Example: python tools/brn-throughput.py train DATA NEW_OUTPUT --positions 65536
No subprocess survives the five-minute timeout. Do not run benchmarks concurrently.
"""
import argparse
import collections
import json
import os
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("train", "load", "pipeline", "profile", "baseline"))
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--positions", type=int, default=16384)
    parser.add_argument("--epochs", type=int, default=2)
    parser.add_argument("--repeats", type=int, default=3)
    parser.add_argument("--overlay", type=Path)
    parser.add_argument("--scalar", action="store_true")
    parser.add_argument("--no-superword", action="store_true")
    parser.add_argument("--record", action="store_true")
    parser.add_argument("--workers", type=int, default=1, choices=range(1, 13))
    args = parser.parse_args()
    if args.output.exists():
        parser.error("Output must be new")
    if args.mode == "baseline":
        args.output.mkdir(parents=True)
        sources = []
        for relative in ("core/brn3/Brn3Trainer.java", "core/util/Fen.java", "corpus/CorpusPosition.java",
                         "corpus/lichess/LichessDecoder.java", "training/data/SourceReaders.java"):
            name = "com/ohinteractive/seedv6/" + relative
            content = subprocess.run(["git", "show", f"{args.input}:app/src/main/java/{name}"],
                                     capture_output=True, check=True, timeout=30).stdout
            path = args.output / "src" / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
            sources.append(str(path))
        subprocess.run(["javac", "-cp", os.pathsep.join(["app/build/classes/java/main", "app/build/install/seedv6/lib/*"]),
                        "-d", str(args.output / "classes"), *sources], check=True, timeout=60)
        (args.output / "revision.txt").write_text(str(args.input) + "\n", encoding="utf-8")
        return
    if args.mode == "profile":
        result = subprocess.run(["jfr", "print", "--json", "--events", "jdk.ExecutionSample",
                                 str(args.input)], capture_output=True, check=True, timeout=60)
        samples = json.loads(result.stdout)["recording"]["events"]
        counts = collections.Counter()
        for event in samples:
            frame = event["values"]["stackTrace"]["frames"][0]
            method = frame["method"]
            counts[(method["type"]["name"], method["name"], frame["lineNumber"])] += 1
        summary = {"totalSamples": len(samples), "topFrames": [
            {"class": key[0], "method": key[1], "line": key[2], "samples": n}
            for key, n in counts.most_common()]}
        args.output.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(summary, indent=2))
        return
    paths = ["app/build/classes/java/verification", "app/build/classes/java/main",
             "app/build/resources/main", "app/build/install/seedv6/lib/*"]
    if args.overlay:
        paths.insert(0, str(args.overlay))
    command = ["java", "--add-modules=jdk.incubator.vector", "-Xms512m", "-Xmx2g"]
    command.append(f"-Dbrn.workers={args.workers}")
    if args.scalar:
        command.append("-Dseedv6.brn3.scalar=true")
    if args.no_superword:
        command.append("-XX:-UseSuperWord")
    if args.record:
        command.append("-Dbrn.profile=true")
    command += ["-cp", os.pathsep.join(paths),
                "com.ohinteractive.seedv6.training.service.BrnThroughput", args.mode,
                str(args.input), str(args.output), str(args.positions), str(args.epochs), str(args.repeats)]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.with_suffix(".stdout.txt").open("xb") as stdout:
        subprocess.run(command, stdout=stdout, stderr=subprocess.STDOUT, check=True, timeout=300)
    report = json.loads((args.output / "report.json").read_text(encoding="utf-8"))
    print(json.dumps({"output": str(args.output), "medianSeconds": report["medianSeconds"],
                      "seconds": [t["seconds"] for t in report["trials"]],
                      "stateHashes": [t.get("stateSha256") for t in report["trials"]]}, indent=2))


if __name__ == "__main__":
    main()
