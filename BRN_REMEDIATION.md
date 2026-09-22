**SeedV6 BRN-2 remediation — 2026-09-22. PERFORMANCE REMEDIATION INCOMPLETE; final automated gate passed 875/875.**

This unit starts from `f1026a8` (BRN-2), following accepted integration baseline
`f5e032c`. The inherited untracked `app/bin/` directory is excluded from the work.
Neither root `CODEXLOG_CURRENT.md` nor `VERSION_STATE.txt` exists. No user store was
modified. The inherited `ChessFrameSmokeTest` used production startup during the
first full gate, which ran the intended additive legacy preference migration;
the legacy value was retained. That test now uses temporary settings, as do the
other store/preferences tests. No BRN-3, strength tuning or Player/Generator work is included.

**Folder failures and fixes.** `TrainingSettings` stored one `root` preference.
The architecture selector changed only the configuration card. The next Apply or
Start combined the newly selected architecture with the previous field, then saved
that combination. `TrainingFolders` now retains each outgoing field and loads the
incoming architecture's own selection immediately. Stable keys are
`checkpointRoot.nnue`, `.brn0`, `.brn1`, and `.brn2`; unknown selections show an empty
field and cannot start. Browse, Apply/Start and switching retain the selected path.
Queued configuration saves cannot overwrite a newer folder/architecture selection.
Existing lifecycle editing locks remain in effect.

The legacy `root` migrates only to its old persisted `architecture`; an absent
architecture means NNUE. Existing architecture-specific keys win. The migration
marker makes this idempotent, and the legacy value is retained. An unrecognized
legacy architecture is preserved without guessing ownership or copying its path
to NNUE. Restart reloads each architecture independently.

The exact generic folder error came from a top-level filename whitelist in
`TrainingController.Backend.inspectStore`, before any checkpoint or architecture
inspection. The initial read-only inspection of the reported location found BRN-2 manifests/payloads and
an extra `New Folder` directory (created at 03:36:09 UTC). That entry triggers the
old predicate even though the root contains valid checkpoints. The temporary-store
regression reproduces this predicate after switching from BRN-0 to BRN-2, then
verifies successful recognition with the fix. There is no preserved event trace
proving which extra entry existed at the exact historical failure; current disk
state at that inspection and the deterministic reproduction establish the faulty recognition rule.
A later read-only check could no longer access `E:\SeedV6-BRN`; no operation in
this unit moved, removed or wrote that directory. No trained model was read for
the benchmark, and the historical failure is not claimed to be reconstructed
from a preserved runtime trace.

History did not pollute the tested fresh-root path: its constructor only stores
a path, refresh reads without creating files, and the GUI prints the history path
even when no file exists. Originally Browse changed the field; Apply/Start queued
preference persistence; Start inspected names; only a successful startup opened
the store, published its bootstrap checkpoint, ran a generation, settled its
decision, and appended history. The history directory is first created by append.
The new ordering is read-only root inspection, architecture validation, exclusive
store ownership, bootstrap identity, store scaffolding/checkpoint publication,
and only later generation history. A manifest's identity takes precedence over
incidental root names, and architecture mismatches are reported before mutation.

New architecture-aware stores write `bootstrap-identity.bin`, a versioned,
checksummed architecture record, before secondary directories. An interrupted
startup is recoverable only with that valid record and the exact empty Seed
directories/zero-length lock files. Corrupt identity, unknown files, nonempty
scaffolding and pre-bootstrap history are rejected and preserved. Older unmarked
partial folders cannot be conclusively attributed and are not auto-recovered.
Existing completed stores do not require this new record. Catalogue browsing now
uses an existing payload lock without creating one in legacy stores; absent-lock
catalogues are advisory, and actual model loading retains the existing ownership
and full validation protocol.

**Performance diagnosis and implementation.** A 20-second JFR capture of the
original real search contained 1,844 execution samples. BRN-2 evaluation was on
1,642 stacks (89.0%). Attribution within that evaluation was 828 samples in the
pair/endpoint loop, 597 in local activation/pooling, 116 in feature extraction,
60 in node initialization, 19 in context/checks, and 22 in score activation.
This is sampled CPU attribution, not an exact elapsed-time decomposition. Board
application, dispatch, copies, synchronization and thread-local access were not
the dominant costs. Exact evaluator call counts were not instrumented.

Full recomputation now separates contiguous endpoint additions while retaining
each channel's addition order. Immutable models whose weights are all bounded by
`1e100` have a conservative overflow proof for every intermediate; these models
avoid redundant inner-loop finite checks. This does not clip or change weights.
Larger finite models retain checked full inference, including overflow rejection.
The final output remains checked. The original full accumulation order is kept in
`Brn2Model.evaluateReference` and `SearchEvaluation.brn2FullRecompute`.

Incremental search inference uses the existing NNUE-style worker/ply companion
interface. Each occupied square owns its 32-channel incident-relation sum. A child
rebuilds changed piece endpoints and subtracts/adds only their incident relations
for unchanged endpoints. It decodes each occupied piece once, recomputes all raw
status context in canonical bit order, then applies the same node/local bias,
local ReLU, board pooling, board ReLU and value head. Position differences cover
captures, promotions, all castlings and en-passant without separate move rules.
Returning to the parent slot performs unmake; model identity and all five position
longs are checked, and Zobrist is ignored as an input. Main search and quiescence
own separate stacks. Rebuild after 32 placement transitions bounds drift; large
board replacements also rebuild. No search policy, trained architecture, H,
relation vocabulary, codec or optimizer math was changed.

The accumulator adds approximately 8.43 MiB of primitive payload per main+qsearch
context (257 slots each), or about 59 MiB for six workers plus the coordinator,
excluding object headers and other search storage. Validation retains a search
for each colour, so their combined additional accumulator payload is about
118 MiB at six threads. Self-play reuses its search within a game; validation
reuses each colour's search within a game. These buffers are not recreated at
every move. Allocation occurs at context construction, outside leaf inference.
Binary64 reassociation prevents general bit
identity. The deterministic test uses an absolute output tolerance of `2e-13`
for the bootstrap and a second perturbed model; 27,987 make/unmake/sibling
comparisons measured at most `1.124100812432971e-15`, with zero mapped-score
differences. Full optimized recomputation remained bit-identical on those boards.
Special-move, all 64 status bits, key independence, stale position, model isolation,
extreme finite weights, and single-/six-thread search equivalence are also tested.
These numeric observations are not a universal error bound for arbitrary weights.

**Measurement protocol.** Windows, AMD Ryzen 5 5500 (6 cores/12 logical processors),
JDK 21+35-2513, `-Xms512m -Xmx512m -XX:+AlwaysPreTouch -Xbatch`. Baseline engine
classes were preserved before evaluator/search changes; the same final benchmark
driver runs against both class sets. Neural architectures use their deterministic
bootstrap weights, identical mate-distance-only/full-window search policy, depth
4, cold private 262,144-entry TT, one warmup and three rotating-order measured
rounds, at one and six threads. The suite contains starting position, Kiwipete,
sparse pawn endgame and a legal Ruy Lopez opening. Each position has a 10-second
cap; all entered nodes, including incomplete iterations and quiescence, are
reported. Model-dependent node counts differ, and parallel scheduling also changes
node counts; per-position completion is reported separately from throughput.
Production defaults to a 1,048,576-entry table and reuses search contexts within
a game; this controlled benchmark uses the smaller capacity identically for all
four architectures. It does not measure end-to-end generation, construction or
checkpoint/optimizer costs.
The standalone benchmark uses the first three boards, 600,000 warmup evaluations
per model and five rotating rounds of 300,000 evaluations with thread allocation
counters. Setup, model construction and search-state construction are excluded.
No training campaign or 64-game/64-pair comparison was launched.

**Measured search results.** Values are independent medians of three measured
rounds. Nodes include incomplete work. `cap / 2` means the 10-second budget
expired with only depth 2 complete; it is a failure to finish the requested depth 4.
These are per-position searches, not measured generation durations.

| BRN-2 position | Threads | Before ms | After ms | Before nodes/s | After nodes/s | Before nodes | After nodes | Completed depth |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| Start | 1 | 164.843 | 58.197 | 57,770 | 163,633 | 9,523 | 9,523 | 4 |
| Ruy Lopez | 1 | 475.298 | 157.969 | 59,714 | 179,668 | 28,382 | 28,382 | 4 |
| Kiwipete | 1 | 10,000.026 | 10,000.042 | 130,294 | 260,636 | 1,302,948 | 2,606,371 | cap / 2 |
| Pawn endgame | 1 | 0.931 | 1.009 | 342,569 | 316,092 | 319 | 319 | 4 |
| Start | 6 | 140.336 | 39.281 | 212,682 | 747,292 | 29,847 | 29,132 | 4 |
| Ruy Lopez | 6 | 707.936 | 207.914 | 216,476 | 754,501 | 153,166 | 158,352 | 4 |
| Kiwipete | 6 | 10,000.079 | 10,000.057 | 266,406 | 905,596 | 2,664,132 | 9,055,993 | cap / 2 |
| Pawn endgame | 6 | 3.181 | 1.297 | 311,871 | 764,724 | 992 | 993 | 4 |

With six threads the start and Ruy Lopez throughputs improve 3.51x and 3.49x.
Kiwipete improves 3.40x in throughput but still cannot finish depth 4. The sparse
single-thread endgame regresses by 0.078 ms. The capped tactical result prevents
an operational-usability claim despite the material improvement elsewhere.

For the unchanged architectures, the same suite/settings produced:

| Architecture | Position | Threads | Before ms | After ms | Before nodes/s | After nodes/s | Before nodes | After nodes | Completed depth |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| NNUE | Start | 1 | 10.342 | 8.870 | 462,087 | 538,782 | 4,779 | 4,779 | 4 |
| NNUE | Ruy Lopez | 1 | 34.165 | 33.598 | 489,764 | 498,037 | 16,733 | 16,733 | 4 |
| NNUE | Kiwipete | 1 | 91.119 | 90.404 | 493,115 | 497,012 | 44,932 | 44,932 | 4 |
| NNUE | Pawn endgame | 1 | 0.738 | 0.745 | 543,360 | 538,617 | 401 | 401 | 4 |
| NNUE | Start | 6 | 10.327 | 10.794 | 2,018,089 | 1,986,511 | 21,254 | 21,443 | 4 |
| NNUE | Ruy Lopez | 6 | 36.549 | 38.078 | 2,276,272 | 2,157,045 | 83,195 | 82,126 | 4 |
| NNUE | Kiwipete | 6 | 595.195 | 613.037 | 1,574,458 | 1,504,966 | 937,109 | 905,842 | 4 |
| NNUE | Pawn endgame | 6 | 1.672 | 1.170 | 520,871 | 746,881 | 871 | 874 | 4 |
| BRN-0 | Start | 1 | 5.010 | 4.310 | 426,173 | 495,325 | 2,135 | 2,135 | 4 |
| BRN-0 | Ruy Lopez | 1 | 9.143 | 10.319 | 531,894 | 471,267 | 4,863 | 4,863 | 4 |
| BRN-0 | Kiwipete | 1 | 46.796 | 46.013 | 527,241 | 536,215 | 24,673 | 24,673 | 4 |
| BRN-0 | Pawn endgame | 1 | 0.233 | 0.205 | 699,271 | 795,122 | 163 | 163 | 4 |
| BRN-0 | Start | 6 | 7.015 | 6.546 | 1,773,159 | 1,900,426 | 12,421 | 12,440 | 4 |
| BRN-0 | Ruy Lopez | 6 | 19.267 | 20.342 | 2,066,777 | 1,956,357 | 39,798 | 39,797 | 4 |
| BRN-0 | Kiwipete | 6 | 54.991 | 56.739 | 2,266,529 | 2,182,252 | 123,995 | 124,175 | 4 |
| BRN-0 | Pawn endgame | 6 | 0.790 | 1.281 | 480,830 | 296,690 | 380 | 380 | 4 |
| BRN-1 | Start | 1 | 92.502 | 94.905 | 106,419 | 103,725 | 9,844 | 9,844 | 4 |
| BRN-1 | Ruy Lopez | 1 | 1,037.317 | 1,021.820 | 139,811 | 141,931 | 145,028 | 145,028 | 4 |
| BRN-1 | Kiwipete | 1 | 10,000.035 | 10,000.046 | 233,902 | 235,951 | 2,339,028 | 2,359,520 | cap / 0 |
| BRN-1 | Pawn endgame | 1 | 1.314 | 0.953 | 548,706 | 756,717 | 721 | 721 | 4 |
| BRN-1 | Start | 6 | 63.283 | 73.408 | 496,544 | 421,801 | 32,082 | 31,811 | 4 |
| BRN-1 | Ruy Lopez | 6 | 976.933 | 1,064.370 | 655,033 | 620,998 | 645,317 | 626,558 | 4 |
| BRN-1 | Kiwipete | 6 | 10,000.063 | 10,000.058 | 1,213,588 | 1,121,178 | 12,136,199 | 11,211,891 | cap / 0 |
| BRN-1 | Pawn endgame | 6 | 2.279 | 1.821 | 507,173 | 612,469 | 1,143 | 1,131 | 4 |

BRN-1 already fails to complete even its first iteration on Kiwipete in this
bounded fixture, before and after. That unrelated behavior is recorded rather
adjusted. Completed before/after pairs have identical scores and moves: 42 pairs
at one thread and 42 at six threads. All 42 single-thread pairs also have identical
node counts. Parallel counts differ with scheduling; timing variation in unchanged
models is not evidence of an architecture change.

**Standalone throughput and allocation.** These medians use the same three-board
corpus and current machine/JVM in both revisions. They are distinct from the
older reported BRN-0 1,089,052 / BRN-1 172,376 / BRN-2 83,600 eval/sec figures.

| Architecture | Before eval/sec | After eval/sec | Allocated bytes/eval, before and after |
|---|---:|---:|---:|
| NNUE | 439,566 | 423,972 | 0.000 |
| BRN-0 | 933,035 | 898,618 | 0.000 |
| BRN-1 | 154,507 | 153,905 | 0.000 |
| BRN-2 | 69,089 | 68,320 | 0.000 |

Full recomputation changes show no material standalone gain; the search gain
comes from incremental relation reuse. Search allocation includes all Java threads
during the timed search call, including its task/result allocation, but excludes
search-context construction and worker creation/prestart. Median allocation on
the Ruy Lopez fixture was:

| Architecture | Threads | Before bytes/search | After bytes/search |
|---|---:|---:|---:|
| NNUE | 1 | 13,832 | 13,832 |
| NNUE | 6 | 98,920 | 99,392 |
| BRN-0 | 1 | 13,832 | 13,832 |
| BRN-0 | 6 | 99,048 | 99,264 |
| BRN-1 | 1 | 14,888 | 14,888 |
| BRN-1 | 6 | 100,264 | 100,200 |
| BRN-2 | 1 | 14,888 | 14,384 |
| BRN-2 | 6 | 100,072 | 99,720 |

An independent after-change JFR run contains 1,701 execution samples, 1,677 in
search. Incremental `update` appears on 984 search stacks (58.7%; includes 386
rebuild-square and 172 decode samples); `evaluate` appears on 360 (21.5%). The
largest remaining sites include endpoint updates (420), clearing rebuilt local
rows (285), local composition/pooling (230), and board decoding (172). Quiescence
appears on 1,620 search stacks (96.6%). Sampling/inlining makes this approximate;
it is not a call counter or proof that every line took that fraction of elapsed
time. No exact evaluator-call instrumentation was added to production inference.

Existing diagnostics were also run on Kiwipete with one thread, depth 4, a cold
262,144-entry TT and a deterministic 1,000,000-node ceiling. This was a count-only
diagnostic during regression testing; its elapsed time is not benchmark evidence.

| Model | Main nodes | Qsearch nodes | Maximum q-ply | Soft q-depth limit encounters | Completed depth |
|---|---:|---:|---:|---:|---:|
| NNUE | 13,774 | 31,158 | 17 | 1,556 | 4 |
| BRN-0 | 8,333 | 16,340 | 7 | 0 | 4 |
| BRN-1 | 10 | 999,990 | 21 | 220,393 | 0, node cap |
| BRN-2 | 1,038 | 998,962 | 21 | 319,901 | 2, node cap |

BRN-2 has 193,267 stand-pat cutoffs and 132,359 checked qnodes in this bounded
prefix. Thus 99.8962% of its entered nodes are qsearch nodes. This directly
confirms excessive tactical-tree work in addition to evaluator cost; it does not
establish that shortening qsearch would preserve the accepted search semantics.
Running that same diagnostic against the preserved original engine produces
byte-identical diagnostic text for all four models: main/q nodes, reached plies,
cutoffs, ordering counters and completed iterations all match. No elapsed-time
comparison is drawn from these count-only runs.
Repeating the count-only diagnostic with production's 1,048,576-entry table gives
identical BRN-1 and BRN-2 diagnostics, including their capped depths. The smaller
benchmark table therefore does not explain the BRN-2 capped prefix. NNUE/BRN-0
table diagnostics differ; the timing tables continue to report the explicitly
fixed 262,144-entry benchmark configuration.

The remaining tactical tree requires more investigation. A proposed next unit is
the same bounded diagnostic on recorded slow positions and actual trained model
copies, followed by measurement of lazy accumulator
materialization or rigorously keyed exact result reuse. Such reuse must include
all raw BRN status bits and all search-history/bound semantics. No pruning change,
quantization, relation removal, width reduction or learned-architecture change is
authorized or implemented by this report. These are investigation directions,
not a proven fix or a claim that a search redesign is already necessary.

Raw measurements and JFR captures are in ignored `app/build/brn-remediation/`.
The durable tables above retain their conclusions; build cleanup can remove the
raw files. The reproducible entry point is `:app:brnRemediationBenchmark`, for
example `-PbenchmarkArgs="search 3 6"` or `-PbenchmarkArgs="micro 5"`. The driver
accepts a fifth positional argument for a standalone BRN-2 model file, read-only.
Baseline compiled classes were retained at `app/build/brn-remediation/baseline-classes`.

**Validation.** Final command:

```powershell
.\gradlew.bat :app:fullCheck --console=plain
```

**PASS: 875/875 tests, zero failures, errors or skips.** This is 793 routine tests
and 82 explicit slow NNUE tests; Gradle reports `BUILD SUCCESSFUL in 25m 36s`.
The full gate includes these disjoint package groups (all pass):

| Package group | Tests |
|---|---:|
| BRN-2 core/codec/incremental | 22 |
| BRN-1 core/codec | 14 |
| BRN-0 core/codec | 23 |
| NNUE core | 34 |
| Search, including evaluator integration | 221 |
| GUI/controller/preferences | 145 |
| Checkpoint, resume and recovery | 71 |
| Training service/lifecycle | 65 |
| Remaining rules, board, training, validation, tools and protocol tests | 280 |
| **Total** | **875** |

The new test classes contribute 17 tests: seven folder-preference tests, five
store-recognition/bootstrap tests and five incremental-equivalence tests.
Focused final storage verification also passed 27/27 using `:app:test` with
`--tests '*BrnStoreRemediationTest' --tests '*TrainingFoldersTest'
--tests '*TrainingStoreRecognitionTest' --tests '*Brn2CheckpointTest'`.
An earlier full gate passed 874/874 (792 routine + 82 slow); the final gate above
supersedes it and includes the last bootstrap safeguard and smoke-test isolation.
`git diff --check` passes; all seven new task files also pass explicit trailing
whitespace/final-newline checks. No required automated gate was skipped. Exact
historical GUI event tracing, human depth-4 training acceptance and full 64/64
workload timing remain unavailable/unperformed; no acceptance is inferred from
automated tests. Full-gate output is `app/build/brn-final-full-gate.log` and JUnit
XML is under `app/build/test-results/{test,nnueSlowTest}`.

The final routine suite passed 793/793 with no failures, errors or skips. Its
native Swing BRN-2 lifecycle screenshot was visually inspected: generation 2 is
stopped after 2/2 games, two optimizer updates and 2/2 validation pairs, with Resume
available. That automated legal mate fixture uses depth 1 / one thread and does
not substitute for the required human depth-4 / six-thread speed check.

**Final Git state.** `HEAD` remains `f1026a8`; no new commit, push or deployment was
made. There are 17 modified tracked files and seven new task files, all unstaged.
The inherited untracked `app/bin/` remains outside this work. The final diff and
file list are reviewable in the worktree; generated timing/profiling/build outputs
remain ignored. Both exact root journal/version capability files remain absent.
The code changes are implemented and automated validation is complete, while the
performance objective and human acceptance remain open.

**Files changed.** All paths are relative to the repository root.

| Area | Files |
|---|---|
| Inference and search | `app/src/main/java/com/ohinteractive/seedv6/core/brn2/Brn2Accumulator.java` (new); `Brn2Model.java`, `Brn2Workspace.java` in the same directory; `app/src/main/java/com/ohinteractive/seedv6/search/evaluation/SearchEvaluation.java` |
| GUI preferences and startup | `app/src/main/java/com/ohinteractive/seedv6/gui/TrainingFolders.java` (new), `TrainingSettings.java`, `TrainingPanel.java`, `TrainingController.java`, `ChessFrame.java` |
| Store identity and browsing | `app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointInspection.java`, `CheckpointStore.java`, `PayloadAccess.java`; `app/src/main/java/com/ohinteractive/seedv6/training/service/TrainerService.java` |
| Benchmark | `app/src/main/java/com/ohinteractive/seedv6/tools/search/BrnRemediationBenchmark.java` (new); `app/build.gradle` |
| New tests | `app/src/test/java/com/ohinteractive/seedv6/core/brn2/Brn2AccumulatorTest.java`; `app/src/test/java/com/ohinteractive/seedv6/gui/TrainingFoldersTest.java`, `BrnStoreRemediationTest.java` |
| Adjusted existing tests | `app/src/test/java/com/ohinteractive/seedv6/gui/ChessFrameSmokeTest.java`, `NetworkArchitectureTest.java`, `TrainingStoreRecognitionTest.java`; `app/src/test/java/com/ohinteractive/seedv6/search/evaluation/Brn2SearchIntegrationTest.java` |
| Documentation | `README.md`; `BRN_REMEDIATION.md` (new) |

**Human actions required after this prompt:**

1. **Blocking acceptance:** launch the current source build with
   `.\gradlew.bat :app:run --args=gui`; repeatedly switch among NNUE, BRN-0,
   BRN-1 and BRN-2 and verify each exact folder restores independently, including
   after restarting the application.
2. **Blocking acceptance:** select a separate fresh BRN-2 folder and confirm
   startup succeeds without a false non-empty-folder rejection.
3. **Blocking operational acceptance and further BRN architecture/strength work:**
   perform only a short depth-4 / six-thread BRN-2 speed pilot with small game/pair
   counts. The tactical benchmark remains unresolved even if easier positions are
   faster; review the pilot together with that evidence before accepting performance.
4. **Conditional, blocking lifecycle acceptance:** if the pilot establishes usable
   operation, exercise Stop, application restart and Resume against that same store.

Do not begin the full 64-game/64-pair comparison before the short speed pilot
passes. No BRN-3, strength tuning or Player/Generator separation follows this unit.
Passing automated tests or finishing this Codex turn does not establish operational
or human acceptance; the performance work unit remains incomplete.
