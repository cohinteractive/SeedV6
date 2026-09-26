# Headless BRN cross-host diagnostics

`brn-diagnostic` diagnoses the **current production training path**. It never opens Swing,
loads GUI preferences, discovers a normal training store, resumes a lineage, or writes
outside a new directory under repository `build/`. The production checkpoint lifecycle
still runs, including Candidate publication and Best promotion, inside that private store.
Supplemental `replay --engine legacy` exercises the retained parallel/qsearch engine.

## Finding in this checkout

At the implementation baseline (`8929e96`), `SelfPlayRunner` and `ValidationArena` construct
`SearchDriver`, which performs full-window `ExactSearch` iterations from depth 1 through the
requested depth. `ExactSearch` is single-threaded and ends at a static leaf: **no qsearch**.
`SelfPlayBatch` generates games in index order. `Brn2SelfPlayTraining` then performs a single
online pass, optionally shuffled using the generation's shuffle seed. Validation game pairs
also run sequentially. The configured training/validation thread count does not reach a
parallel search constructor. Thus a configured six-thread production run reports
`effective_workers: 1`, `worker_count_ignored: true`, and zero qnodes. This is a code-path
finding, not a diagnosis of the reported Mac/PC speed discrepancy.

The old `IterativeDeepeningSearch` → `RootParallelSearch` → `AlphaBetaPvsSearch` path remains
available for labeled legacy **replay only**. It retains its original shared TT, root work
assignment, reduction, qsearch and ordering. Production training is never redirected to it.

Normal BRN-2 initialization already uses `java.util.Random` with
`Brn2Model.INITIALIZATION_SEED`. The new explicit-seed constructor uses the identical
distributions and zero biases. The no-argument constructor still uses its original seed.
Model inference/Adam use the existing binary64/StrictMath implementation. Feature schema
is 2; codec format is 1. Diagnostic fingerprints hash a versioned architecture/schema/
dimensions preamble and **every parameter in ascending index order**, big-endian
`Double.doubleToLongBits`, using SHA-256. They exclude Adam state, paths and timestamps.
Native `.brn2` snapshots are saved for generation zero and every distinct Candidate/Best.
The isolated training store also retains ordinary model/optimizer payloads and evidence.

Existing generation/domain seeds (`TrainerConfig.seed`) and indexed game seeds
(`SelfPlayRunner.gameSeed`) are reused. Both master/data streams start from `--seed`.
Random opening length and legal move choice are game-local; shuffle and held-out partition
streams are generation/domain-local. No random input is selected by worker completion order.
Zobrist initialization also has a fixed seed. This harness does not reorder any work.

## Invocation

Run from the repository root with Java 21. Use `./gradlew` on macOS/Linux and `./gradlew.bat`
on Windows. The existing application entry point also accepts the command:

```text
./gradlew :app:brnDiagnostic -PdiagnosticArgs="--help"
./gradlew :app:run --args="brn-diagnostic replay --seed 20260926 --depth 2 --workers 1"
```

The dedicated task sets headless mode, uses a 256 MiB initial/1024 MiB maximum heap, and runs
from the repository root. Main's diagnostic branch sets headless mode too and returns before
the GUI/UCI branches. The ordinary application still defaults to UCI; `gui` remains its
explicit GUI argument. Installed `seedv6` / `seedv6.bat` launchers accept `brn-diagnostic` too
when invoked from within this repository.

Commands for the requested comparisons (same commands on both hosts, changing only wrapper
suffix). Output directories must not already exist; omit `diagnosticOutput` for a fresh
automatically named directory each time.

**A — strongest production replay baseline, virgin BRN-2, built-in 15-FEN corpus:**

```text
./gradlew :app:brnDiagnostic -PdiagnosticArgs="replay --architecture BRN-2 --seed 20260926 --depth 2 --workers 1" -PdiagnosticOutput=build/brn-diagnostics/A
```

**B — same production replay with six configured workers:**

```text
./gradlew :app:brnDiagnostic -PdiagnosticArgs="replay --architecture BRN-2 --seed 20260926 --depth 2 --workers 6" -PdiagnosticOutput=build/brn-diagnostics/B
```

B is an intentional ignored-worker control on this checkout, not a parallel benchmark.
For the supplemental true parallel/qsearch comparison, run **both** legacy variants:

```text
./gradlew :app:brnDiagnostic -PdiagnosticArgs="replay --engine legacy --seed 20260926 --depth 2 --workers 1" -PdiagnosticOutput=build/brn-diagnostics/legacy-A
./gradlew :app:brnDiagnostic -PdiagnosticArgs="replay --engine legacy --seed 20260926 --depth 2 --workers 6" -PdiagnosticOutput=build/brn-diagnostics/legacy-B
```

**C — real production training, 16 × 256 games, depth 2, six configured workers:**

```text
./gradlew :app:brnDiagnostic -PdiagnosticArgs="train --architecture BRN-2 --seed 20260926 --generations 16 --games 256 --depth 2 --workers 6 --source self-play --validation game-pairs" -PdiagnosticOutput=build/brn-diagnostics/C
```

C explicitly selects BRN-generated self-play and game-pair validation. Match these settings
to the observed workload before comparing; the GUI's fresh BRN-2 source default can instead
be Handcrafted. `--source handcrafted` and `--validation held-out` use the existing production
alternatives. Held-out validation requires at least four completed games with samples. This
diagnostic supports virgin WDL training with capture consistency OFF; NNUE teacher/bootstrap,
blended targets, capture supervision, frozen replay and existing lineages are deliberately
outside its from-scratch scope. The help lists all supported configuration, including opening
ranges, ply cap, sample cap, learning rate, shuffle, validation depth/pairs/openings/cap and
promotion policy. Defaults are recorded as structured effective settings in every run.

FEN input is UTF-8, one six-field FEN per line; blank lines and `#` comments are ignored.
Direct CLI `--fen` is repeatable. These Gradle properties preserve spaces in arguments:

```text
./gradlew :app:brnDiagnostic -PdiagnosticArgs="replay --seed 20260926 --depth 2 --workers 1" -PdiagnosticFenFile="path with spaces/captured.fen" -PdiagnosticModel="build/brn-diagnostics/C/models/FINGERPRINT.brn2"
```

`--model` is replay-only and reads an explicitly supplied raw `.brn2` file. It never resolves
Best/latest or a production training directory. Its actual fingerprint replaces the virgin
fingerprint in metadata; the seed remains recorded but does not reinitialize loaded weights.

## Output contract and interpretation

Each record has `schema: "seedv6-brn-diagnostic-v1"`, `record_type`, and a monotonically
increasing `sequence`. Output location and initial fingerprint are printed at startup;
the location and concise summary are printed again at completion. Files are `report.jsonl`,
`summary.txt`, `models/<fingerprint>.brn2`, and, for replay, `corpus.fen`. Training adds its
private `training-store/`. Existing output directories and paths outside build are rejected;
existing path aliases are checked before mutation. These artifacts are ignored by Git.

| Record | Meaning |
| --- | --- |
| `run_metadata` | Git commit/dirty/status (null + error if unavailable), OS/JVM/heap/CPU, seed, model/schema, effective settings, search path, worker behavior, node definition and reproducibility limitations |
| `generation_start` | Generation number, parent Candidate and current Best fingerprints, all domain seeds |
| `search_start` | Position/game/generation/phase, pre-search FEN, actor fingerprint/path and requested depth; flushed before timing starts, so a hung root is identifiable |
| `search` | Same identity, returned move/score, completion, ordinary nodes/qnodes/total, nanoseconds/milliseconds/NPS, existing diagnostics, repetition history keys, outlier reasons |
| `search_outlier` | Full duplicate of a flagged `search`, including FEN; do not count this as another search |
| `game_end` | Zero-based indexed seed, termination/plies/samples and cumulative production game statistics |
| `generation_end` | Settled Candidate/Best fingerprints, policy outcome, available training/validation losses, games/positions/samples, wall time and search aggregates; generation 0 is explicitly initialization-only |
| `run_end` | Completed/failed status, error when applicable, wall time, search totals, worst search and all generation fingerprints |

Position IDs are one-based and sequential within a run; game indexes and played ply counts
are zero-based. Validation game indexes count both colours of each opening pair. `phase`
separates replay, self-play and validation. Ordinary node counts exclude the root and include
admitted child entries across all depth iterations; `total_nodes = nodes + qnodes`.
Qnode percentage uses total nodes, while the configurable outlier ratio uses
`qnodes / max(1, ordinary nodes)`. Per-root NPS and aggregate search NPS use search-only time;
`wall_nps` includes lifecycle, allocation, checkpoint and reporting overhead. Root timing
excludes report writes and driver construction. Generation duration is the existing production
active-generation clock; per-generation aggregates include validation searches. Run aggregates
are cumulative. JSON null means unavailable, not zero: ExactSearch has no TT instrumentation;
stand-pat attempts and general qsearch beta cutoffs are not separately exposed by the existing
counters. Legacy replay reports existing maximum qply, stand-pat cutoffs and TT counters.

Defaults flag elapsed time >=1000 ms, total nodes >=1,000,000, or qnodes/ordinary nodes >=100.
Use `--outlier-ms`, `--outlier-nodes`, `--outlier-qratio` to change reporting thresholds.
**They never terminate or suppress a search.** There is no implicit time/node safety limit.
The ordinary configured game ply cap still applies and capped games retain production
sample/termination semantics. A final report says `failed` if the lifecycle fails; an external
process kill may leave only the flushed prefix ending in `search_start`.

`Fen.fromBoard` serializes all six FEN fields from the actual packed Board state, including
castling, EP and clocks. Board's existing clock packing limits remain unchanged. FEN alone
cannot preserve previous repetition positions or a game's reused TT. Therefore captured
FEN + model replay is a **fresh-state position experiment**, not a promise to reconstruct an
in-game tree exactly. Reports preserve full ordered repetition keys as unsigned decimal
strings for investigation. Re-running the entire seeded training campaign preserves the
ordinary history/TT lifetime. Single-worker replay is the strongest comparable isolated test.

Compare initial fingerprints, effective settings, engine, corpus and Git status first.
Equal fingerprints/moves/scores/node counts with different elapsed time point toward execution,
JIT/GC, allocation, I/O or host-load differences. Different tree counts in legacy parallel replay
can result from shared TT/worker scheduling; this remains deliberately unchanged. Current
production training has no scheduling-dependent optimizer update order, even when configured
with six workers. Subsequent generation fingerprints expose the first parameter divergence.
Timing is inherently host-dependent; no cross-host equality is claimed from one-host tests.

## Headless verification and short smoke runs

On PowerShell, force headless mode in the test JVM **and its subprocesses**:

```powershell
./gradlew.bat :app:test -Pheadless
```

On macOS/Linux:

```sh
./gradlew :app:test -Pheadless
```

UCI and diagnostic subprocess tests pass the headless property explicitly (the Windows
launcher test uses its existing `JAVA_OPTS` interface). Do not inject it through
`JAVA_TOOL_OPTIONS`: that JVM environment mechanism prints a banner which intentionally
fails the UCI tests' empty-stderr assertions.

`-Pheadless` gives test JVMs a 2 GiB maximum heap: production drivers each allocate a 192 MiB
TT, and lifecycle tests can hold several simultaneously. It changes neither application nor
training/search settings. The routine suite retains its normal `slow-nnue` exclusion. Most visible-window tests use
headless assumptions and skip; unguarded window-dependent tests can fail with HeadlessException.
No graphical verification is required for this facility. New
tests parse JSONL with an independent test-only Gson parser; verify seeds/codec/FEN round trips,
output isolation, observation equivalence and tiny real production/legacy runs; and run Main
in a separate headless JVM with preference access forbidden and class-loading evidence showing
no SeedV6 GUI classes loaded. The tiny training fixture actually generates samples and updates
parameters, rather than treating an empty generation as successful training.

Only a short smoke workload should be run during implementation. C and the full legacy corpus
are operator experiments, not acceptance-time workloads. Example bounded training smoke:

```text
./gradlew :app:brnDiagnostic -PdiagnosticArgs="train --seed 20260926 --generations 2 --games 2 --depth 2 --workers 6 --opening-max 0 --max-plies 4 --samples 2 --validation-pairs 1" -PdiagnosticStartFen="7k/5Q2/6K1/8/8/8/8/8 w - - 0 1"
```

## Implementation validation, 2026-09-26

Windows verification against baseline `8929e96fd8fe06f12c3876e090ec90adb59ce516`:

* All 9 new diagnostic tests passed. The final focused run passed all 26 tests, including
  the production/legacy boundary guard and 16 UCI subprocess/launcher tests.
* The complete routine suite ran 1,097 tests, with 18 skipped. It initially reported 41
  failures: 25 were reproduced on an untouched `git archive HEAD` baseline; 16 were the
  `JAVA_TOOL_OPTIONS` banner interfering with UCI empty-stderr assertions. Those 16 passed
  after headless propagation was changed to explicit subprocess arguments. The baseline
  failures remain unmodified; the suite is **not green**. The normal `slow-nnue` exclusion
  remained in effect. No GUI or graphical application was launched.
* The default 512 MiB test heap exhausted on both modified and baseline trees. The complete
  run used 2 GiB, now provided by `-Pheadless`. Initial/final logs, XML and a machine-readable
  reconciliation are under `build/`, including `brn-test-comparison.json`.
* Short smoke runs used seed **20260926**, with initial parameter fingerprint
  `a719589230d79d1c4add495804804e54902a1ce4147071193d7b5aa222b502ca` in every mode:

| Smoke | Searches | Ordinary nodes | Qnodes | Wall seconds |
| --- | ---: | ---: | ---: | ---: |
| Production replay, 15-FEN corpus, depth 2, worker 1 | 15 | 1429 | 0 | 0.671 |
| Production training, 2 generations × 2 mate-fixture games, depth 2, workers 6 configured / 1 effective | 8 including validation | 512 | 0 | 10.957 |
| Supplemental legacy replay, one mate fixture, depth 1, workers 6 effective | 1 | 26 | 2 | 0.433 |

The smoke artifacts are `build/brn-diagnostics/smoke-{replay,training,legacy}-20260926/`.
Both training generations produced samples and changed Candidate parameters; Best was
retained. JSONL parsing, search aggregate reconciliation, and fingerprints recomputed
independently from the serialized parameter bytes passed. These are harness demonstrations,
not cross-host speed measurements. The full 16 × 256 experiment was not run.
