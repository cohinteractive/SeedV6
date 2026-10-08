# SeedV6

SeedV6 is a high-performance Java chess engine under active development.

The project is focused on building a strong chess engine around a fast, compact core designed specifically for the JVM. SeedV6 uses bitboards, packed primitive state, direct legal move generation, PEXT-based sliding attacks, reusable buffers, specialized move application, staged move picking, and allocation-conscious search structures.

The goal is not merely to produce a working engine. SeedV6 is also an experiment in how far a carefully designed Java implementation can push chess-engine performance while remaining compatible with progressively more sophisticated search.

SeedV6 is already capable of running as a UCI engine. The completed search programme now supplies the application's production search baseline.

## Highlights

Current SeedV6 features include:

- Direct legal-only move generation.
- Separate legal generation stages for evasions, tactical moves, and quiet moves.
- PEXT-based sliding-piece attacks instead of magic bitboards.
- Compact bitboard and packed board-state representation.
- Reusable board and move buffers in hot paths.
- Move-type encoding used to specialize move application.
- JVM/JIT-conscious hot-path design.
- FEN parsing and Zobrist hashing.
- Extensive perft validation.
- UCI position handling and legal move replay.
- Asynchronous search lifecycle with cancellation and search limits.
- Position history and draw adjudication.
- Rich phase-aware static evaluation.
- Static exchange evaluation (SEE).
- Transposition table support.
- Staged search move ordering.
- Killer moves and history heuristics.
- Exact full-width search baselines retained as correctness oracles.

On the development machine, single-threaded perft has sustained more than **120 million nodes per second** on some positions, including a run of more than **6.9 billion nodes at approximately 126.9 million NPS**.

## Project Goals

SeedV6 is built around several priorities:

- Correct chess rules and reliable legal move generation.
- High single-threaded mechanical performance.
- Minimal allocation in search hot paths.
- Compact, cache-friendly primitive data structures.
- Code shapes that give the JVM and JIT compiler useful optimization opportunities.
- Strong correctness baselines that remain available while search becomes increasingly selective.
- Measurable performance improvements rather than assumed optimizations.
- A search architecture that can eventually make effective use of multiple CPU cores.

The project intentionally prioritizes correctness and mechanical efficiency over conventional application-code readability where that trade-off is justified.

## Engine Core

SeedV6 uses a compact bitboard-based representation built primarily from primitive `long` values.

Piece and colour information is represented through bitplanes, while other position information is packed into primitive state containing data such as:

- side to move,
- castling rights,
- en-passant state,
- move counters,
- and Zobrist position identity.

The engine avoids object-heavy board representations in its hot paths.

Search code works with reusable board storage and writes child positions into caller-owned buffers rather than allocating a new board object at every node. The same philosophy is used for move storage, move ordering, search stacks, history information, and other frequently accessed search data.

## Move Generation

Move generation is one of the most heavily optimized parts of SeedV6.

Unlike engines built around generating pseudo-legal moves and subsequently rejecting moves that leave the king in check, SeedV6 directly generates legal moves.

The generator handles information such as:

- checks,
- double checks,
- pinned pieces,
- legal king movement,
- check response masks,
- castling legality,
- en-passant legality,
- promotions,
- and discovered attacks.

Legal generation is also staged.

Depending on the search situation, SeedV6 can generate:

- evasions when the side to move is in check,
- tactical moves,
- quiet moves,
- or the complete legal move set.

This is useful for search because a chess engine frequently does not need to generate, score, and sort every possible move before discovering a cutoff.

## PEXT Sliding Attacks

SeedV6 uses PEXT-based sliding attack lookup rather than magic bitboards.

Earlier Seed engines used magic-bitboard techniques, but SeedV6 deliberately moved to PEXT. In this Java implementation the PEXT approach produces smaller and simpler hot methods and has proved faster in practice.

Rook, bishop, and queen attacks therefore use the SeedV6 PEXT infrastructure rather than carrying forward the older magic-bitboard implementation.

This is representative of the wider SeedV6 design philosophy: implementation choices are selected according to measured behaviour in this engine rather than simply because a technique is conventional in other chess engines.

## Move Application

Move application is designed around reusable destination boards rather than allocation.

Moves include move-type information that allows `makeMoveInto()` processing to be divided into specialized paths for different classes of move.

Instead of forcing one very large method to handle every possible transition, move application can use smaller methods for cases such as ordinary moves and special move types.

This is partly a JVM optimization.

Smaller, mechanically simple hot methods give the JIT compiler better opportunities to inline and optimize common paths than a single large method containing every possible move transition.

This design also allows common moves to avoid unnecessary special-move work.

## Perft and Correctness

Perft is one of the principal correctness tools used during SeedV6 development.

The current suite exercises positions involving:

- ordinary legal movement,
- castling,
- castling through attacked squares,
- en passant,
- en-passant discovered checks,
- promotions,
- underpromotions,
- checks,
- discovered checks,
- double checks,
- pins,
- checkmate,
- stalemate,
- and complex high-mobility positions.

The current 20-position full run passes every expected node count.

Large perft runs are particularly useful because they simultaneously exercise legal generation and board transitions billions of times. A wrong move, corrupted position, incorrect special-move transition, or legality error normally causes the known node counts to diverge.

## Perft Performance

SeedV6's optimized core has produced high single-threaded perft throughput for a Java chess engine.

Representative results from the current development machine include:

| Position | Depth | Nodes | Time | Throughput |
| --- | ---: | ---: | ---: | ---: |
| Initial position | 6 | 119,060,324 | 1.250 s | 95.2M NPS |
| Kiwipete | 6 | 8,031,647,685 | 69.239 s | 116.0M NPS |
| Complex middlegame position | 6 | 706,045,033 | 6.225 s | 113.4M NPS |
| Complex middlegame position | 6 | 6,923,051,137 | 54.561 s | **126.9M NPS** |

Benchmark environment:

- AMD Ryzen 5 5500
- 32 GB RAM
- Windows 11 Pro
- Java 21
- Single perft worker thread

The most significant figures are the long-running multi-billion-node tests rather than very short positions where timer resolution and JVM effects can dominate the reported NPS.

These numbers are **perft throughput, not chess-search NPS**.

Perft does not perform evaluation, transposition-table probing, search reductions, pruning, or other work performed by a real chess search. The results instead measure the throughput of the underlying legal move-generation and board-transition machinery.

They provide useful evidence that the optimization work in SeedV6's board representation, PEXT attack generation, direct legal generation, move encoding, and move-application paths is producing practical results.

Performance will vary with position, hardware, JVM behaviour, system load, and warm-up state.

## Search Architecture

The completed search programme established `SearchDriver -> ExactSearchAdapter -> ExactSearch` as the sole production search lineage. Play and UCI own asynchronous jobs through `SearchLifecycleService`; managed Threads > 1 uses same-depth `ParallelSearch` workers. Network Training self-play and validation own synchronous drivers and use the same per-search thread setting.

Each search owner has its own `TTable`, board/PV state and evaluator stack. Current TT-enabled search uses nominal-depth PVS, SEE/material/main-history ordering, staged lazy legal generation and static leaves. HCE alone enables the accepted calibrated static-null/null-move factories; neural evaluators remain exact. Full windows, repetition, mate-domain and tablebase boundaries are described in the [search contract](../source/CHESS_SEARCH_CONTRACT.md).

See [architecture and source classification](architecture.md). The older alpha-beta/qsearch, flat-search and alternative-table comparisons remain in the verification source set. Their [historical architecture](history/legacy-search.md) does not describe production behavior.

## Search Research

Phase 2 is governed by the active [Contract](../source/CHESS_SEARCH_CONTRACT.md)
and [Research Frontier](../source/CHESS_SEARCH_RESEARCH_FRONTIER.md). It seeks
credible playing-strength gains while preserving the established correctness
foundations. The complete Phase 1 [Contract R045](search-phase1/CHESS_SEARCH_CONTRACT.md),
[Frontier F052](search-phase1/CHESS_SEARCH_RESEARCH_FRONTIER.md) and
[reports/evidence](research/search/) remain historical references. Phase 2 admits
the historical rejections for reconsideration and preserves pending pondering
and evaluator-assisted guidance without starting them. BRE-Pair 2 baseline
verification and all feature research belong to subsequent authorized work.

## UCI and Search Lifecycle

SeedV6 already contains a working UCI engine path.

Normal Play and UCI use the bundled opening repertoire before search. A book hit
returns a weighted legal move with no searched score or depth; misses search as
usual. UCI `setoption name OwnBook value false` disables it (default `true`), and
`go infinite` always searches. Training, validation and data generation remain
book-free. The portable source is `app/src/main/resources/com/ohinteractive/seedv6/book/opening-book.txt`:
four FEN fields, legal en passant identity, and UCI moves with positive weights.
Its header records historical provenance and the rank-support weighting rule.
Seed validates the complete graph once and derives its own runtime keys/moves.

The engine supports legal position reconstruction from both starting positions and FEN positions. Supplied UCI moves are resolved against the engine's generated legal move set before being applied.

Search execution is managed independently of command input so the UCI engine can remain responsive while a search is running.

The search lifecycle includes support for concepts such as:

- depth limits,
- node limits,
- time limits,
- clock-based limits,
- infinite search,
- cancellation,
- replacement searches,
- stale-result suppression,
- and clean shutdown.

This lifecycle is separate from future multi-threaded chess search. The engine can execute search asynchronously without yet using multiple workers to search the chess tree.

## SeedV3 and the Feature Transplant

SeedV6 follows several earlier Seed chess-engine projects.

The current development programme is bringing useful higher-level engine capabilities from **SeedV3** into SeedV6.

SeedV3 is already a working and playable chess engine with features including evaluation, alpha-beta/PVS search, quiescence, transposition storage, move ordering, history and killer heuristics, iterative search, and root parallelism.

SeedV6 is not intended to reproduce SeedV3's implementation.

The two engines have substantially different low-level architectures.

SeedV6 retains its own:

- board representation,
- move encoding,
- direct legal move generator,
- PEXT attack system,
- move-application model,
- perft infrastructure,
- and search-oriented reusable storage.

SeedV3 is therefore used as a source of algorithms, lessons, evaluation ideas, search techniques, and working-engine experience.

Features are adapted to SeedV6 rather than copied mechanically. Where SeedV3 behaviour has known correctness or performance weaknesses, SeedV6 is intended to correct them rather than preserve them for compatibility.

This approach allows SeedV6 to combine the working higher-level knowledge developed in previous Seed engines with a substantially more optimized low-level foundation.

## Performance Philosophy

SeedV6 is deliberately performance-oriented.

Hot-path code commonly favours:

- primitive arrays,
- packed integers and longs,
- bitboards,
- precomputed tables,
- PEXT attack lookup,
- reusable buffers,
- caller-owned destination storage,
- specialized move-transition methods,
- local primitive state,
- low allocation rates,
- bounded heuristic structures,
- reduced branching,
- compact data layouts,
- and methods shaped with JVM inlining in mind.

Performance work is treated experimentally.

An implementation is not assumed to be faster simply because it appears more sophisticated. Where possible, optimizations are validated using perft, deterministic search comparisons, focused benchmarks, profiling, or later game-strength testing.

Correctness baselines are deliberately preserved so mechanical or search optimizations can be tested against independently simpler implementations.

## Concurrency

Managed Play/UCI may use cooperative same-depth `ParallelSearch` with private evaluator/history state, shared coherent `TTable` evidence, and drained helpers before publication. The default remains one worker. Training parallelizes independent self-play games with separate search owners.

## Project Status

The current application includes desktop Play and Network Training, UCI, the completed programme's search, model persistence and validation, and optional guarded small-table root outcomes. Historical experiments are retained separately from the shipped application.

## Repository Structure

- `app/src/main`: current engine, rules, search, GUI/UCI and training application.
- `app/src/verification`: active developer tools, old search comparisons and correctness baselines.
- `app/src/test`: JUnit tests, independent oracles and test-only research harnesses.
- `docs`: architecture, operating guide, search masters, reports and evidence.
- `archive/search-programme`: historical instrumentation requiring its original source revision.
- `gradle` and `tools`: build/packaging and supported developer scripts.

## Requirements

SeedV6 is written in Java.

The primary development environment currently uses:

- Java 21
- Gradle
- Windows 11

Other platforms should be usable where compatible Java and Gradle environments are available, although development and performance measurements are primarily performed on the author's local Windows system.

## Building

The project uses Gradle.

Typical build usage is:

```bash
./gradlew build
```

On Windows:

```text
.\gradlew.bat build
```

Perft, engine, and development tools have their own entry points within the project and may evolve as development continues.

### Test suites

Routine validation, including fast NNUE correctness tests:

```powershell
.\gradlew.bat :app:test
```

Ordinary `check` and `build` also use this routine suite and do not invoke
expensive NNUE integration tests. Run the tests tagged `slow-nnue` explicitly:

```powershell
.\gradlew.bat :app:nnueSlowTest
```

Complete validation runs ordinary checks and the slow NNUE suite:

```powershell
.\gradlew.bat :app:fullCheck
```

The slow suite preserves test failures and fails if no matching tests are
found. Its native Swing smoke tests require a graphical desktop; existing
headless assumptions skip those tests.

Performance and training experiments remain separate explicit tasks:
`:app:nnuePerformanceBenchmark` and `:app:nnueResearch`. Use
`.\gradlew.bat :app:nnueResearch -PresearchArgs="help"` for research options.

### Exact Search foundation (R002)

The independent `search.exact.ExactSearch` is a recursive, single-thread fixed-depth
reference, also invoked by the separate production driver described below.
ExactSearch constructors without a table remain TT-off. R005 permits an optional,
exclusively owned handcrafted `TTable`. Ordinary constructors remain exact with static leaves;
HCE production explicitly composes the accepted static-null/null-move factories. Experimental
selectors support active verification and are never production configuration choices.
TT-off retains deterministic captures/promotions-first ordered alpha-beta as the
independent oracle. Normal TT-enabled constructors use PVS at every positive-depth node:
first move full window, later moves scout, and only interior improvements receive
a full-window re-search, always at the same remaining depth. Ordering is legal
hash once, SEE-good tacticals, SEE-bad tacticals (both by immediate material), then
main quiet history, with generated-order ties. Non-check nodes stage tacticals
before quiets and select moves lazily; checked nodes use complete evasions.
Deferred quiet history is sampled when quiets materialize and fixed for that phase.
History resets for every fixed-depth invocation, including driver iterations;
This fixed-depth history lifecycle is the current baseline. HCE is the default;
`ExactEvaluator` adapts the existing
`SearchEvaluation.State` for HCE, NNUE and BRN without importing search policies.

Run the dedicated headless harness (separate from perft and `searchBenchmark`):

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=start,kiwipete,endgame --depth=4 --warmups=3 --repetitions=5'
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=start,kiwipete,endgame,mate --depth=4 --warmups=5 --repetitions=5 --tt=on'
.\gradlew.bat :app:exactSearch '-PsearchFen=7k/8/5KQ1/8/8/8/8/8 w - - 0 1' '-PsearchArgs=--depth=3'
.\gradlew.bat :app:test --tests '*search.exact.*' --tests '*ExactSearchHarnessTest' --tests '*rules.*'
```

Named positions are `start`, `kiwipete`, `endgame`, `mate`, `checkmate` and
`stalemate`; `--position=all` selects all six. Depth defaults to 3 and accepts
0 through 256. FEN input has no prior repetition history. API callers should
supply `GameHistory` when previous moves are known.

`--tt=off` (default) constructs no table; `--tt=on` uses an explicit 4 MiB
validation table, cleared before every warm-up and measured request. These are
cold-content comparisons; clearing and allocation are outside search timing.
Production retains its table between ordinary requests.
Without ordering/mechanics/traversal overrides, the harness uses the normal
constructors: TT-off oracle or TT-on production baseline. Explicit
`--ordering=control` retains the earlier TT-enabled alpha-beta comparison;
existing research ordering, mechanics and traversal options remain available.

Each result reports requested/completed depth, coordinate best move/PV, score,
nodes, elapsed wall time and NPS. Nodes include the root, terminal positions and
static leaves. The harness checks repeatability of scores, moves, PVs and node
counts, then reports the upper median timing sample after warm-up. JVM worker
construction and FEN parsing are excluded; per-call setup is included. Timings
measure this static-leaf tree, not playing strength or perft throughput.

Scores are side-to-move relative. The existing SeedV6 numeric convention
(`TranspositionScores`) reserves +/-32768 for mate, adjusted by
root ply through 256; static scores must stay within +/-32511. HCE enforces
its own +/-30000 limit, and NNUE/BRN mappings stay within +/-32511. New evaluators
are range-checked. Depth-zero nodes still resolve mate, stalemate and rule draws
before evaluation. Draws use `DrawAdjudicator` and real-position history.

Cancellation uses a caller-supplied `BooleanSupplier` plus thread interruption;
`SearchControl.checkpoint()` can be adapted without adding time policy. Aborted
results publish no best move/PV, use `Value.INVALID` for score, and report
completed depth -1. Completed narrow-window calls can return fail-soft bounds;
only a full-window call promises an exact minimax score. Instances and evaluator
state are worker-confined and reusable, not concurrently callable.

Its tests retain an independent unpruned shallow oracle and controlled positions;
deep search and playing-strength validation remain separate work.

R005 score reuse requires the same remaining depth and request generation.
Terminals/draws precede probing; EXACT returns directly, LOWER/UPPER only cut
when they already prove the caller's bound. Non-cutting bounds do not tighten
windows. Completed scores are classified against the original window and mate
scores normalized with `TranspositionScores`. Depth 256 remains supported but
is not stored in the table's eight-bit depth field. Cancelled nodes do not store
completed evidence; the root store follows the final completion checkpoint.

`SearchKey` combines the board Zobrist key, complete status long (BRN includes
fullmove bits), and an ordered fingerprint of canonical repetition identities
within the reversible history window. The fingerprint includes supplied pre-root
history and extends in a primitive per-ply stack. Pawn moves/captures reset it;
other differing histories are conservatively kept separate. This may miss safe
transpositions. Like the established Zobrist key, it is a 64-bit fingerprint.
The evaluator definition is fixed within each request. No evaluation cache is
mixed into the Search table; current evaluator paths have no such cache to migrate.

A matching legal hash move moves to the front with all other moves kept in order,
even when its depth or generation cannot prove a score. Cutoffs return only a
legally checked PV prefix. `TTable`'s existing replacement mechanics are unchanged,
including retention of some older same-key entries; their scores are rejected.
There are no new replacement, sizing, statistics or selective-Search policies.

### Production Search driver (R003)

`search.driver.SearchDriver` searches complete full-window depths 1, 2, 3, ...
through the caller's maximum depth. It publishes one stable `IterationSnapshot`
per completed iteration. An interrupted iteration supplies no score, move or PV;
the outcome retains only the latest completed iteration, separately identifying
the attempted depth and incomplete attempt. Before any completion, that result
is null. Managed Play leaves the board unchanged; UCI publishes `bestmove 0000`.
There is no arbitrary legal-move fallback. Self-play and validation still require
their requested depth to complete before accepting a move/sample.

`ExactSearchAdapter` reuses one private evaluator state and the established
evaluator child-transition hook to admit each child through `SearchControl`.
Rejected admission stops ExactSearch at its next cancellation checkpoint. The
budget is cumulative across depths, excludes roots, and lets the last admitted
node unwind normally. This preserves the existing consumer node convention:
prepared child transitions. A PVS full re-search reuses its prepared child;
the independent harness counts every negamax entry, including roots and re-searches.
Final lifecycle statistics include interrupted work; completed snapshots contain cumulative work at their
publication point. Diagnostics report admitted main nodes, evaluator calls,
maximum ply and completed iterations; legacy-only mechanism counters stay zero.

Each production adapter also owns a separate `TTable`, using its pre-existing
64 MiB requested constructor default (192 MiB packed payload). `SearchDriver` brackets every top-level request with
`beginRequest`/`endRequest`; all its depths share one table and generation. Normal
requests advance generation once without clearing; eight-bit wrap and `newGame`
invalidate Search data. A direct ExactSearch call establishes its own generation
unless explicitly bracketed as part of one request. Table injection transfers a
fresh, generation-zero table to one owner; callers must not share or independently
advance it. `new ExactSearchAdapter(evaluation, null)` provides TT-off driver
comparisons. Play and Training retain separate tables, generations, cancellation,
board/evaluator stacks and PV state.

External cancellation and hard limits retain precedence. Clock-managed requests
may stop on a completed forced move or mate-in-one under the accepted SR-034
allocation policy. Managed Play/UCI Threads>1 uses drained same-depth
`ParallelSearch`; ordinary self-play and validation remain synchronous per game.
Training threads belong to each search; games remain sequential. Play and Training owners can run
concurrently, with existing score mappings and private evaluator state.

`ProductionSearchBoundaryTest` rejects production dependencies on verification
classes and inspects the application JAR for excluded classes/resources.
Legacy flat/alpha-beta/qsearch comparisons and diagnostic tools live exclusively
in `app/src/verification`. Production consumers have no fallback to them.
Developer strength and mapping tools invoke the canonical driver.

### Desktop Play workspace

Launch the Swing desktop with `.\gradlew.bat :app:run --args=gui` (ordinary
startup remains UCI). Phase 1 uses `docs/images/UI_PLAY.png` as the Play reference
and Phase 2 uses `docs/images/UI_TRAINING.png` for the Training dashboard. FlatLaf supplies the dark
controls/window chrome; `SeedTheme` and its packaged properties centralize the
palette, spacing, typography and cards. The existing rounded piece resources
are used on the board. The workspace divider is resizable; at small heights,
the right-hand cards scroll without collapsing their controls or PV.

Human vs Engine still starts engine turns automatically. Engine vs Engine opens
setup and waits for **Start Game**. New Game, Load FEN and Stop Search remain available. Its read-only scoresheet retains coordinate
notation, aligns Black-first FEN games correctly, and follows new moves only
when already scrolled to the bottom. No clocks, captured-piece ledger, Undo,
manual Move action, SAN conversion or historical position navigation were added.

The Engine card consumes existing completed-iteration publications on the EDT.
Depth, NPS and elapsed time describe the last completed iteration (time is not a
live chess clock). Scores and the evaluation bar use White's perspective; NNUE
V1 values remain explicitly uncalibrated units, not centipawns or win probabilities.
The bar uses a bounded presentation mapping and does no additional evaluation.
There are no new engine callbacks, search polling loops or engine dependencies
on GUI code. Training retains its independent lifecycle and 500 ms polling.

Play, Training and Arena offer **Max**, resolving on the current machine to the
lesser of its logical processor count and the engine's supported worker limit (16).
Normal explicit values are bounded to that range. Max persists as a semantic choice,
so another machine resolves it afresh. Legacy larger positive choices remain readable
and are bounded at execution. These are maximum workers, including the main worker;
small searches can use fewer. Play changes while idle govern the next search and
remain disabled during active search/evaluator changes.

`PlayPresentationTest` checks score mapping and scoresheet behavior;
`PlayWorkspaceSmokeTest` exercises a real window, board input and resizing and
writes renderings to `app/build/gui-smoke/`. The slow `NnueGuiSmokeTest` also
exercises evaluator changes, simultaneous Play/Training and safe shutdown.

In Engine vs Engine, **White Engine** and **Black Engine** independently select
**HCE (Handcrafted)** or **Network**. HCE needs no checkpoint; its network controls
are hidden and disabled. HCE vs HCE and either orientation against any supported
network architecture are available. Network uses the common
**Lineage -> Generation** browser (architecture is derived from the lineage). Register an external store
once without moving it, or choose a managed named lineage. **Swap Sides** transfers
both complete bindings and freezes a Best alias to its displayed concrete snapshot.
Start Game pins independent evaluators and search state even for identical weights.
Changing selections affects the next game. A missing/stale payload fails visibly
and preserves the prior board and participants, without fallback.

Human vs Engine offers **Handcrafted** or **Network**, with the same browser under
**Engine Opponent** and an independent remembered selection. New Game resolves the
snapshot for either human colour. Active labels identify the loaded architecture,
lineage and generation, which subsequent promotion or pruning cannot replace.

Browsing reads bounded advisory metadata off the EDT; actual loading verifies payload
identity. Valid historical manifests remain browsable and annotatable after pruning,
but unavailable snapshots cannot load. Best and Latest are separate. A publish-only
Arena store can expose Latest without Best; it never receives invented promotion
evidence. Paths are available in details and registration, not everyday selection.

`PerSideNnuePlayTest` checks participant identity across alternating searches and
failed game creation; `AvailableCheckpointsTest` checks materialization, pruning
and payload coordination. `PerSideNnueSmokeTest` exercises native Swing selection,
presentation and resizing, with captures in `app/build/gui-smoke/per-side/`.
`HumanEngineSelectionTest` checks architecture parity, lineage selection, New Game
pinning and failure semantics; `HumanEngineSelectionSmokeTest` checks native Swing
wiring, active identities and the opponent controls' layout.

### Network Training: lineages and sessions

One compact status area identifies the selected lineage/architecture, generation,
training state, model losses, phase progress and validation result. Candidate/current
training, latest completed generation, and Best have separate identities: Best is
not necessarily the latest trained model. Position-generation progress appears only
during active generated-game work, never corpus acquisition, frozen replay or stopped
work. **Evidence** expands validation components, identities and prior-generation
timing. **Effective configuration** below Recent History expands diagnostic settings.
The comparison and duration graphs have a 230 logical-pixel preferred plot height
(formerly 120), take spare vertical space, and retain vertical scrolling on smaller
windows. The transient Training Activity card beneath the board is removed; the
board's evaluation tooltip, accessible description and Diagnostics button remain.

Loss is a numerical objective with six decimal places (scientific notation for very
small or large values), not a percentage or playing-strength score. The dashboard
distinguishes the mean over optimizer sample visits, final training-set fit, and
held-out validation loss. Game-pair scores remain percentages. Objectives differ:
legacy NNUE/BRN paths report half-squared error, while calibrated NNUE reports
cross-entropy. BRE-Pair 2 reports cross-entropy for its running optimizer mean but
half-squared outcome error for final fit; those two metrics are not the same objective.
BRN-3 reports half-squared outcome error despite its cross-entropy gradient.
Held-out comparisons use half-squared error on their recorded
targets. Do not compare loss magnitudes across architectures, recipes or datasets.
Corpus validation labels distinguish historical CP targets from outcome adapters.

Checkpoint manifests do not store final training
loss. `GenerationRecord.loss` stores measured final training fit, and
`BootstrapEvidence` stores held-out Candidate/incumbent evidence with separate IDs.
`TrainerSnapshot.meanTrainingLoss` is a running mean, not final fit. A restored
checkpoint may have validation evidence but no historical training loss; it remains
**Unavailable**, without loading models or reconstructing measurements. The GUI
caches history by publication identity and binds loss to exact checkpoint IDs.
Retained validation publications may refer to the preceding generation. These
dashboard measurements keep their original mathematical and persistence semantics.

The workspace has **Session**, **Lineage Settings**, **Training Settings**,
**Validation Settings**, **Dataset Library**, **History**, and **Diagnostics**.
The primary selector lists lineages across architectures. Lineage Settings shows
identity, architecture, recorded completed generations, latest completed loss, Best,
checkpoint availability and recorded exposure. Exposure here means cumulative sampled
positions in the recorded generation history, including held-out samples where
recorded; it is neither unique positions nor optimizer presentations. Missing
historical exposure is explicitly unavailable. Catalog summaries are read off the
EDT and cached; repaint never loads checkpoints or reconstructs history.

In **Lineage Settings**, **New Lineage...** creates a named UUID-backed store under
the configured library. Architecture is fixed at creation. **Rename...** changes
only the display name; ID, storage location, model references and history remain.
**Import...** adopts an existing lineage in place, detecting its actual architecture.
Names need not be unique. New lineages can use NNUE material parity, BRE-Pair 2, or
BRN-3. The Architecture Library retains legacy NNUE, BRN/BRN-0, BRN-1 and BRN-2 for
existing stores; BRN-4 is explicitly research-only, with no executable implementation.
`ArchitectureLibrary` defines creation eligibility and delegates defaults to
`TrainingRecipe`; persisted schema IDs, folder names and historical aliases stay fixed.

**Training Settings** contains Learning Rate, Batch Size, Training Epochs, Training
Positions per Generation, weighted Training Sources, and Seed. BRE-Pair 2 starts at
0.01 with Masked Adam, batch 128 and 8 epochs; BRN-3 uses 0.003 / 128 / 8; NNUE
material parity uses Adam at 0.001 / 32 / 1. Dataset acquisition defaults to 131072
training positions per generation. Existing saved settings and absent optimizer-rate
overrides retain their historical meaning. Legacy BRN-0/1/2 use one online pass.
Explicit rate changes preserve Adam moments and step. Settings are locked while
training and apply prospectively: an incompatible partial generation restarts from
its settled parent under the existing restart transaction, preserving prior evidence.
Saved configurations remain revisioned, separately from immutable model payloads.

Seed defaults to 1 for new lineages. It controls the established generation-indexed
run/shuffle/validation streams, plus fresh NNUE/BRN-3 initialization. BRN-1/2 retain
fixed initialization seeds, and BRN-2 retains its immutable recorded run-seed contract.
Other architectures permit seed edits while stopped for subsequent generations.
BRE-Pair 2 uses its fixed material prior and zero pair residual. Resume restores model
and optimizer state; changing a seed does not reinitialize them. Sequential datasets
advance by durable source cursors, not random selection. No initialization facts are
invented for old stores. Optional live generation has separate depth/thread/random
opening/game-cap controls, independent of validation settings.

**Dataset Library** registers supported files/folders, displays source format,
identity, label profile, location/provenance and readiness, and offers reusable BINP
preparation. The restored Lichess `.jsonl.zst` import panel creates or extends a Seed
corpus and registers its resulting dataset; source files remain unchanged. A lineage's
**Training Sources** select library datasets with positive integer relative weights
(default 1): 1:1 gives equal contributions and 3:1 gives 75%:25%, rounded deterministically
by largest remainder. These counts drive real accepted-example acquisition, separately
for training and held-out data. Weights are saved in that lineage's selection, not the
library descriptor. Version and label metadata remain intact. Source exhaustion,
changed identities, missing data and incompatible label profiles stop with a diagnostic;
there is no wraparound or fallback source.

Live self-play remains available only for architectures whose existing trainer can
supply it. It generates positions per generation and is distinct from stored datasets.
Live generation cannot be mixed by weight with imported datasets: the backend has no
combined live/dataset scheduler. Native saved self-play trajectories are resume data,
not an importable dataset format. Only data in a supported dataset format and label
profile belongs in the library. BRN-3 and BRE-Pair 2 are corpus-only trainers.

**Validation Settings** selects **Game Pair** or **WDL Loss**. The latter shows no
Game Pair controls and retains the existing held-out objective (including CP or outcome
adapters, rather than relabeling their numerical values as percentages). Game Pair
keeps the proven default of 64 pairs and supports 128. One pair is two colour-reversed
games from the same seeded random-legal opening. Opening Min/Max Plies default to
0/8 and are not opening-book lengths. The 1024-ply cap counts moves after that opening.
Only chess-complete pairs contribute to scoring and the unchanged promotion policy.

Search Limit is **Depth** or **Time**, showing only its applicable field. Time means
the same per-move millisecond budget for both sides, using the existing cancellable
iterative search driver and its last completed iteration. No usable iteration is a
search failure, never a depth fallback. Time-limited search is not bitwise deterministic
across machines or scheduling. **Search Threads** controls actual same-depth parallel
search workers within each move (1 through the supported processor/cap limit, or Max);
validation games remain sequential. Search resources close at game boundaries and Stop
cancels active search. Validation configuration is frozen per attempt; old depth-only
fingerprints and records retain their encoding, and timed evidence has a distinct policy
identity. Candidate/incumbent IDs and W/D/L evidence remain bound to that experiment.

**Session** contains Training Duration and the existing dashboard/lifecycle controls.
Choose **Unlimited / Until Stopped** or **Fixed Number of Generations**. A target of 16
from 10 completed generations runs 16 additional settled cycles, through generation 26.
Rejected and inconclusive completed cycles count; partial, interrupted and failed cycles
do not. A session ends at the settled generation boundary without changing promotion,
retention or the next training parent. Historical time-budget modes remain supported.

The additive `training-session.json` records session ID, lineage ID, starting boundary,
target, completed count and lifecycle state under the existing store lock. Reading it
never auto-starts training. After an unclean exit/failure, explicit Start recovers the
remaining original budget, reconciling settled checkpoint evidence before admitting
more work. A changed duration draft does not replace an unfinished recovery budget.
**Stop Now**, **Stop after Generation**, and application shutdown end the current
session; the next Start creates a new additional-generation budget and can resume its
saved partial generation. Switching workspaces or leaving training in the background
keeps the session running. Completed-session records are displayed without execution.

Example: create `l1` with BRE-Pair 2, keep 0.01 / 128 / 8 and seed 1, set 65536
positions, and select just an eligible Stockfish dataset at weight 1. Choose Game Pair,
128 pairs, Depth 4, and four search threads where supported. In Session choose Fixed
Number of Generations, 16, then Start Training. The fixture tests exercise this GUI
configuration and real budget accounting without a production 16-generation campaign.

Generation zero establishes the existing bootstrap Best/Latest lifecycle. Architecture
schemas and checkpoint/optimizer formats remain distinct and unchanged. Wrong-architecture
stores fail clearly without conversion. New metadata lives in separate sidecars.

By default, generated-game trainers use terminal W/D/L targets (`-1`, `0`, `+1`) from each
sampled position's side-to-move perspective. Search scores are not training targets;
no centipawn conversion or clamping is applied. BRN inference is separately mapped
to uncalibrated search units: zero maps to zero, otherwise
`sign(v) * max(1, round(32511 * abs(v)))`. This monotone, sign-preserving mapping
uses the full normal score range while reserving the mate band, matching the
established bounded-value policy without changing NNUE's mapping. BRN search uses
full windows and mate-distance-only selectivity through the existing evaluator
interface; search algorithms and handcrafted evaluation are unchanged.

Safe Stop now saves completed self-play games and their ordered samples, or the
exact model/Adam state and optimizer-batch cursor. Resume restores the indexed game
seeds and deterministic shuffled workload; completed games and updates are not
replayed. An interrupted game restarts at its original indexed opening. Game-pair
validation retains each finished colour game; a paused match has no final decision
or history row. The bounded held-out prediction passes and any already committed
decision drain safely. Candidate publication remains atomic.

`partial-generation.bin` atomically selects a checksummed immutable snapshot under
`partial-generations/`. Partial state never advances Latest Training or Best and
is not a completed generation. Model state uses the existing architecture codecs;
metadata binds its checksum, generation attempt, samples, cursor, validation games
and accumulated active durations. Snapshots are written at normal safe stop, not
per sample or search node. Wait for STOPPED before closing the process. An abrupt
failure resumes the last valid durable boundary, not unsaved in-flight work.
Superseded partial snapshots remain preserved; repeated stops consume additional
disk space. Existing stores without partial snapshots retain their available
checkpoint/bootstrap-data boundaries; missing historical work is not reconstructed.
Legacy Candidates without recorded generation settings are labelled accordingly;
current editable settings are not presented as their original self-play configuration.
Engine vs Engine can load NNUE and BRN evaluators independently from their checkpoint stores.

BRN-0 core (`6983f0e`), its 766-test integration gate, and its human GUI training /
Stop / Resume test are accepted. BRN-1 automated validation passed `:app:fullCheck`
on 2026-09-22: 728 routine and 81 slow tests, 809/809 total, with no failures or
skips. BRN-0 and BRN-1, including human GUI/runtime training, validation, Stop,
restart and Resume tests, are accepted (user confirmation 2026-09-22).

BRN-1 reuses the exact 26,224 non-bias BRN features (960 nodes, 25,200 canonical
pair relations, 64 raw status bits). Each has 32 learned double embeddings, pooled
by active occurrence count with 32 hidden biases, then ReLU, 32 output weights,
one output bias and tanh: **839,233 parameters**. Raw status can gate board-pattern
activations without special chess logic. Initialization uses `java.util.Random`
seed `0x533642524e310001`, embeddings uniform +/-0.01, zero biases and output
weights uniform +/-sqrt(6/33). Width and initialization are fixed, not GUI controls.
Dense parameters participate in every Adam step; inactive embedding rows and their
moments freeze. The same BRN-1 family supplies self-play, Candidate, validation and
promoted evaluation. No separate Player/Generator model is implemented.

`core.brn1` documents the independent, versioned CRC32 payload: model 6,713,896 bytes,
training 20,141,664 bytes, including all parameters, moments, optimizer settings and
step. BRN-0 payloads retain their original interpretation and are never migrated.
`Brn1CoreTest` checks finite-difference gradients and learns a board/status XOR
that additive BRN-0 cannot classify; this demonstrates expressiveness, not chess
strength. BRN-1 codec, checkpoint, service, search, validation and GUI tests cover
continuation, recovery, pinning and locks. Bounded real service smoke uses a legal
mate fixture for two generations; GUI fixtures also exercise real training with
bounded legal positions. Human testing remains the gate for subsequent model work.

A temporary warmed single-thread Java 21 measurement (2026-09-22, starting
position, a middlegame and a sparse endgame equally weighted; five alternating
300,000-evaluation rounds) measured median BRN-0 900 ns/evaluation versus BRN-1
4,773 ns/evaluation: about 1.11M versus 210K evaluations/second, or 5.3x evaluation
time. Thread allocation counters measured zero bytes per evaluation for both.
This is an evaluator-only observation, not a performance gate or chess-strength
claim; end-to-end search/training costs and larger campaigns remain unmeasured.

BRN-2 is the final currently planned experimental BRN architecture. It composes
relations locally before global pooling: each occupied node receives its player-relative
node embedding, the same summed canonical rule-state context, local bias, and its
A/B endpoint vectors from every incident unordered canonical pair. Local ReLU is
followed by sum pooling plus board bias, a second ReLU, and a linear/tanh value head.
Width is fixed at 32. Feature schema **2** makes color symmetry structural: side to
move is us, the opponent is them, and Black perspective uses rank reflection (`^56`).
Endpoint routing and accumulation order use canonical squares. Castling is us/them,
EP is oriented consistently, and the halfmove clock is retained. Redundant STM,
fullmove numbering and unused status bits are excluded. Both player perspectives
are maintained incrementally; evaluation selects one, without averaging outputs.
There are no additional message-passing rounds. NNUE, BRN-0 and BRN-1 remain
independent baselines; search and promotion policy are unchanged.

The exact binary64 layout is 960x32 nodes, two 25,200x32 relation endpoint tables,
64x32 status, 32 local biases, 32 board biases, 32 output weights and one output
bias: **1,645,665 parameters**. Initialization uses `java.util.Random` seed
`0x533642524e320001`, nodes uniform +/-0.01, endpoint/status embeddings +/-0.005,
zero biases and output weights uniform +/-sqrt(6/33). Deterministic initialization
checks required no scale adjustment. Each touched embedding row receives one Adam
update using the summed vector gradient from all occurrences, including different
endpoint ReLU masks. Absent rows and their moments freeze. Dense biases and the
head update each step. All gradients use pre-update weights.

[`core.brn2` package documentation](../app/src/main/java/com/ohinteractive/seedv6/core/brn2/package-info.java)
describes the independent format-1 CRC32 payload. `network.brn2` is **13,165,364
bytes**; `training.state` is **39,496,044 bytes**, including every weight, both
moments, global step and all four optimizer settings. The 40-byte header validates
schema, row counts, endpoint count, width and parameter count. Checkpoint manifests
retain SHA-256 protection and the existing atomic publication/recovery protocol.
The architecture remains `seedv6.brn.2`; the payload and manifest schema fields are
now **2**. Absolute-color schema-1 weights (including g315) are incompatible and
fail with an instruction to choose a separate empty store. Start a fresh NNUE-bootstrap
BRN-2 lineage; do not migrate or overwrite the old store. See
[canonical representation evidence](research/brn/BRN_COLOR_SYMMETRY.md#canonical-brn-2-remediation).
Self-play pins BRN-2 Latest Training; validation loads persisted BRN-2 Candidate
and Best independently. Incompatible stores/payloads fail without migration.

BRN-2 validation on 2026-09-22 passed the forced repository gate
`:app:fullCheck --rerun-tasks`: **776 routine + 82 slow = 858 tests**, with no
failures, errors or skips. This includes 49 BRN-2, 43 BRN-1 and 49 BRN-0 tests,
plus NNUE and shared architecture/GUI/checkpoint/recovery/promotion regressions.
BRN-2 tests cover numerical gradients for every parameter family, asymmetric
endpoint routing, both ReLUs, all raw status bits, repeated-row sparse Adam and a
48-example translated local-convergence learning problem. Codec and service tests
compare all state bytes across continuation, changed initial-rate settings, and
Stop after an actual optimizer update followed by deterministic replay.

The bounded real BRN-2 service smoke used the same legal mate fixture as BRN-1:
**two generations, four completed self-play games, four samples/updates, four valid
validation pairs, two retained Candidates**, and normal durable history/decisions.
Native Swing tests also ran real training, Stop, controller restart and Resume;
the configuration and lifecycle captures were visually inspected. These bounded
fixtures establish integration, not general playing strength or long-run health.

A warmed single-thread Java 21 measurement on Windows 11 (2026-09-22) used the
same equally weighted starting/middlegame/sparse-endgame positions as the BRN-1
measurement above, prebuilt boards and reusable workspaces. After 600,000 warmup
evaluations per architecture, five rotating-order rounds of 300,000 evaluations
measured median BRN-0 **1,089,052 eval/s** (918 ns), BRN-1 **172,376 eval/s**
(5,801 ns), and BRN-2 **83,600 eval/s** (11,962 ns). BRN-2 took **2.06x BRN-1**
and **13.03x BRN-0** evaluation time. Thread allocation counters recorded **zero
bytes per evaluation** in every measured round for all three. The JVM used
`-Xms512m -Xmx512m -XX:+AlwaysPreTouch -Xbatch`; timing excludes model creation,
board construction, persistence and training. This is an evaluator observation,
not a performance threshold or playing-strength result.

Human testing accepted per-architecture folder persistence, Resume for all four
architectures, recognition of the originally failing BRN-2 store, and incremental
BRN-2 inference gains. Untrained BRN-2 still caused severe search stalls. The
historical remediation is preserved in [BRN_REMEDIATION.md](research/brn/BRN_REMEDIATION.md)
and commit `fc21935`.

BRN-0/1/2 support **Bootstrap with NNUE**: a separate NNUE generator store's
Best drives position generation, while the selected BRN learns terminal W/D/L by default.
New BRN-0/1 GUI lineages default to this source. New BRN-2 lineages default to
Handcrafted generation and WDL supervision. **Position generation** and **Candidate
validation** are independent campaign selections. BRN-2 offers network self-play,
Handcrafted and NNUE generation with either Candidate-vs-Best game pairs or WDL /
held-out loss. NNUE training uses its own network self-play and can select either
validator. BRN-0/1 retain their supported self-play and NNUE generation sources.
NNUE generation selects a compatible accepted Best lineage in the common browser,
separately from the student. Existing preferences retain their chosen source and
historical validation default. Resume restores optimizer state; absent explicit
recipe overrides retain its actual learning rate.

NNUE and BRN-2 support original sources through **Network Training > Data & exposure**.
Register Lichess JSONL/PZstandard files or a legacy Seed data directory, configure
source weights once, and set training positions per generation in Data & exposure.
Source cursors and exact active-generation ranges belong to each lineage. Startup
checks source identities; acquisition decodes only the required ranges with bounded
seek overhead. There is no normal import or whole-source preparation step.
See the [Training Data guide](corpus-training.md) for allocation, migration, source
identity, target policies and durable stop/resume semantics.

Held-out validation reserves about 20% of completed sampled games (13 of 64), with
at least two games in each partition and at least four completed sampled games.
No held-out sample enters that generation's updates. Candidate and Best are compared
on exactly the same held-out games using the configured target and mean half-squared
error. Strictly lower loss promotes; ties retain Best. This measures prediction loss,
not playing strength. Game-pair validation independently plays Candidate against Best
under the existing promotion/confidence policy. WDL training with game-pair validation
uses the full generated batch, including when positions come from an external source.

BRN-2 additionally offers **Supervision: NNUE blended** in **BRN-2 Configuration**.
Set **NNUE teacher weight (%)** to the desired percentage; its complementary WDL
contribution is shown beside it. The exact sampled-position normalized static
NNUE value is blended with terminal WDL. **NNUE Teacher Store** selects the accepted Best to pin independently for each new
generation, including with handcrafted generation. Generator and teacher identities
are persisted separately; unchanged Resume loads the exact pins, never current Best.
Historical blended plans retain their original shared NNUE checkpoint semantics. WDL
remains the default, and existing WDL stores need no migration. Supervision, weight and teacher selection can change at a stopped boundary.
They remain fixed within a running generation.
When held-out validation is selected, blended history records configured, WDL and
teacher component losses; only the configured held-out loss decides promotion.
Game-pair validation uses its existing independent promotion policy. Current metadata, Resume and bounded preflight evidence are in
[BRN_HANDCRAFTED_POSITION_GENERATION.md](research/brn/BRN_HANDCRAFTED_POSITION_GENERATION.md).
Handcrafted scores never enter BRN targets. The next campaign's supervision regime
requires a separate decision; training003 remains historical evidence.

Position source and validator persist independently. Configuration locks during work
and becomes editable after the lifecycle settles. Unchanged Resume preserves pinned
generators/teachers, samples and exact optimizer continuation. Changing source,
validator or effective generation settings while stopped archives unfinished work and
restarts that generation from its settled training parent with the same generation
number. Already durable decisions finish under their recorded policy. No new store
is required for a methodology change, and settled generations retain their recorded
regime. Legacy history without those fields remains readable and reports no invented
configuration. Network architecture, payload/feature schemas and optimizer compatibility
remain checked by the existing loaders. Persisted run seeds retain their reproducibility
semantics; frozen replay retains its separate corpus-bound workflow.

Each new generation pins current NNUE Best for any required external roles. There is
no automatic source or validator transition. The existing depth confirmation remains;
no restart-confirmation modal is added. Explicit recipe rate changes follow the same
safe generation-boundary/restart rules; unchanged or inherited rates retain exact
optimizer continuation. Historical bootstrap details are in
[BRN_BOOTSTRAP.md](research/brn/BRN_BOOTSTRAP.md#stopped-reconfiguration).

Checkpoint folders now persist independently under `checkpointRoot.nnue`,
`checkpointRoot.brn0`, `checkpointRoot.brn1` and `checkpointRoot.brn2`. Switching
architecture retains the outgoing field and restores the incoming selection;
an unselected architecture has an empty field. Legacy `root` migrates once only to
the old selected architecture (NNUE if the selection was absent), without deleting
the old preference. Existing manifests identify stores before unrelated root names
are considered. New bootstrap attempts have a checksummed architecture identity;
only that identity with exact empty Seed scaffolding can restart initialization.
Unidentified non-empty folders, including bare training/history directories, remain
rejected and preserved. History reads and catalogue browsing create no files.

Training opens on Dashboard, with generation/state, self-play game accounting,
optimizer updates/samples/loss, validation progress, Candidate versus the actual
incumbent Best, and the current independent depth/game/pair/thread settings.
The retained previous validation is labelled as the latest match and never
counted as the next generation's validation progress. Loss is a training-fit
metric; Candidate scores are valid-pair results against that match's incumbent,
not Elo or absolute strength. Promotion publication remains distinct from a
passing assessment. Campaign elapsed is runtime of the current Start/Resume
invocation. Generation elapsed uses the same monotonic active-time measurement
as history: it includes saved active time on Resume, the full generation lifecycle
and settlement, and excludes downtime and the history append. Unmeasured legacy
recovery duration remains unavailable. The centered thin divider fills by the
displayed Run ordinal / configured generation count, without phase fractions;
continuous runs retain an unfilled divider. Candidate/Draws/Best counters retain
valid-pair accounting. Candidate and Best increases pulse for 420 ms on the EDT
without changing layout; first display, reopening and replay of saved pairs do
not pulse. Previous generation shows only the measured duration of N-1, with an
unavailable state for missing history or timing.

Validation & run owns run/match controls; Recipe & lineage owns trainer settings;
Data & exposure owns provider/source setup. Apply settings or Start/Resume saves
edits, and settings remain locked until the worker terminates. The existing depth
confirmation is preserved. Diagnostics adds shared optimizer metrics and retains
both bounded textual snapshots with scroll-position preservation and bottom following.

Phase 4 publishes genuine active self-play and validation positions. `HeadlessGame`
remains owned by the trainer/arena caller; search workers and Swing never share its
mutable board. An optional `ActiveGameFeed` copies the existing six-long board at
game start and after each completed legal move into an immutable latest-value
publication. There are no per-node callbacks, GUI locks, queues, extra evaluations,
filesystem operations or worker-to-EDT calls. Headless callers without a feed incur
no snapshot allocations. The service joins the latest position to `TrainerSnapshot`
on polling, guarded by generation and phase. The existing 500 ms EDT timer copies
and repaints a board only when its publication changes; intermediate moves may be
coalesced. This is presentation state, never checkpoint/history/promotion evidence.

Each position carries generation, phase, monotonically increasing game/version
identities, one-based game ordinal/pair slot, played plies, last move, the complete
board (including side to move), actual participant roles/checkpoint IDs and monotonic
timestamps. Self-play uses the frozen Latest Training actor for both colours, not
Best and not the as-yet-unpublished Candidate. Validation uses persisted Candidate
and incumbent Best, with their actual reversed colour assignments in game two.

The Training bar is explicitly **last move search, White perspective**: the existing
final completed root search that selected the displayed last move, before applying
that move. Its primitive score is sign-normalized using the searching side, with
source-position key, searching side and depth retained. NNUE V1 values are
uncalibrated mapping units, not centipawns or probability; mate bands retain the
searched root's mate distance. This is not a newly evaluated score for the resulting
position and not validation aggregate score. The caption identifies the actual
searching participant. Game starts and random opening moves have no evaluation;
no score is carried across an unsearched move or between games.

Terminal/capped games clear immediately; all published-game exit paths clear in `finally`.
Optimizer, publication, settlement, cancellation, failure and shutdown display an
unavailable board. Cancellation permanently closes the feed, so late publications
cannot revive it. Resume uses the existing fresh service lifecycle (there is no
in-place pause); the next game/generation starts with new identity and no score.
The concurrent Play board and its pinned evaluators stay independent.

`ActiveGameFeedTest`, `SelfPlayPositionTest`, `ValidationPositionTest` and
`TrainingBoardPresentationTest` cover ownership, moves, perspectives, side swaps and
stale-state handling. Real NNUE games with telemetry enabled/disabled retain identical
trajectories/validation evidence. `LiveTrainingBoardSmokeTest` runs a production
trainer from the standard starting position alongside Play, observes changing live
boards through the normal timer, captures `app/build/gui-smoke/phase4/<scale>/`, and
checks stop/shutdown. It is distinct from scripted layout fixtures.
`ActiveGamePublicationBenchmarkTest` reports warmed capture/publication nanoseconds
and allocation over prebuilt boards; it does not measure total NNUE training
throughput. Run it with `:app:test --tests '*ActiveGamePublicationBenchmarkTest'`.
No dependency, packaging resource, engine search or learning policy was changed.

### Durable generation history (Phase 3)

Completed generations now append to `history/generations-v1.tsv` under the selected
checkpoint folder. The default Windows path is
`%LOCALAPPDATA%\SeedV6-NNUE\training\history\generations-v1.tsv`; the existing
fallback is `%USERPROFILE%\.seedv6-nnue\training\history\generations-v1.tsv`.
The saved checkpoint-folder preference also selects the history. Nothing is written
to the source checkout or packaged application directory by default. Test fixtures
use isolated temporary stores. There is no database or additional dependency.

Schema 1 is a UTF-8 line-oriented, tab-separated `key=value` format with a SHA-256
checksum and a terminating newline. Fields are: `schema`, `generation`, `candidate`,
`incumbent`, `best`, `outcome`, `decision`, `wins`, `draws`, `losses`, `validPairs`,
`incompletePairs`, `score`, `lower`, `threshold`, `depth`, `games`, `pairs`, `threads`,
`completedGames`, `abortedGames`, `samples`, `loss`, `started`, `completed`,
`selfPlayNs`, `trainingNs`, `validationNs`, `totalNs`, and `sha256`.
Network identities are the existing immutable checkpoint identities. `incumbent`
is captured at validation start, never inferred from the later Best. W-D-L and
score count valid pairs only; no valid pairs means absent score/lower bound.
Samples are sampled self-play positions, and loss is the final full-dataset loss,
when training returned one. `-` denotes unmeasured optional values. Timestamps are
absolute ISO-8601 instants; elapsed nanoseconds use the monotonic clock.
Unknown additional fields are tolerated within schema 1; incompatible schemas
require a new version and are reported instead of being interpreted as schema 1.
New game-pair rows optionally include `rawPromotionThreshold`, the strict raw-score
boundary computed by the actual promotion policy at the settled valid-pair count.
It is absent when promotion is unattainable at that sample size. Legacy rows lack
the complete confidence policy and retain a threshold gap; no history is rewritten.

The authoritative write boundary is in `TrainerService.execute`, immediately
**after** `resolveCandidate` returns from durable validation recording, any
required promotion, and reference recovery. The immutable record is appended and
`FileChannel.force(true)` completes before the worker increments its completed
counter and proceeds. Actual resulting Best determines the outcome; a passing
assessment alone is never called promotion. Self-play/optimizer cancellation and
infrastructure/publication failures emit no ordinary completion. A settled
cancelled validation is `CANCELLED_VALIDATION`; a capped/insufficient experiment is
`INCONCLUSIVE`. The original policy decision is retained separately.

History is analytics, independent of checkpoint validity. An append error does
not undo promotion or stop the trainer: its generation, Candidate, path and error
remain visible in History/Diagnostics and are written to stderr. Success is not
claimed when forcing the write fails. There is no automatic retry loop. Readers
validate checksums, structure and measurement relationships, preserve all original
bytes, warn about malformed/duplicate rows, and keep the first valid unique
Candidate/generation. An unterminated last line is ignored until a subsequent
append adds a separator; that append never truncates/replaces historical bytes.
Unsupported schemas prevent this version from appending. History errors do not
make the checkpoint store corrupt. Missing/empty history is normal startup.

History begins with this feature. Existing checkpoints do not contain authoritative
original generation timing and complete training configuration, so they are not
backfilled. Restart reuses existing rows without duplicating the prior Candidate.
A process crash between settled checkpoint completion and a completed history
append can leave a history gap, as can an analytics write failure. Recovery of a
previously pending Candidate does not invent that generation's original settings
or timing. Checkpoint recovery remains unchanged; there is no distributed
transaction between analytics and checkpoint publication, and no universal
power-loss guarantee for Windows storage hardware.

History offers Last 25/50/100, Today and All. Today uses completion instants in the
system's local timezone, including a refresh across local midnight. The same range
drives metrics, charts, latest-first table, consecutive configuration regimes and
Best lineage. Selecting a row exposes full identities, W-D-L, valid/incomplete pairs,
exact score/lower/threshold, configuration, samples, final loss and phase durations.
Metric definitions (all scoped to the selected records) are:

- Generations: recorded completed lifecycles, including explicitly inconclusive or
  cancelled-validation completions; promotions: rows whose resulting Best is Candidate.
- Promotion frequency: promotions / recorded completed generations.
- Average duration: summed total duration / generations with recorded durations.
- Generations/hour: timed generations * 3600 / recorded active seconds.
- Generations/promotion: all recorded completed generations / promotions.
- Time/promotion: summed recorded total duration / promotions, with duration coverage
  shown explicitly when only part of the selected range has timing.

Empty denominators display unavailable, never infinity or fabricated zero. Total
duration starts before loading the generation parent and ends after the settled
decision; it includes checkpoint/decision I/O but excludes the analytics append and
between-run downtime. Phase durations measure the respective existing operations.
Wall-clock adjustments do not alter recorded elapsed durations.

Candidate comparison plots show the latest contiguous validation objective in the
selected range, explicitly labelled, so different loss objectives/scales are never
interpolated. Game pairs show score and lower bound (higher is better), with a 50%
reference. Held-out objectives show authoritative configured candidate minus Best
loss (lower is better), with a zero reference. Blended targets use the recorded
configured loss, never a GUI combination of the descriptive component losses.
Green diamonds mark promotions and vertical dashed lines mark configuration changes.
The Dashboard adds an unconnected hollow live score point with its percentage.
Green horizontal dashed segments show the raw promotion boundary from
`PromotionPolicy`: `0.5 + requiredMargin + sqrt(-log(alpha) / (2 * validPairs))`,
with its minimum-pair gate and strict greater-than rule preserved. The live
reference assumes the expected final valid pairs (configured pairs minus known
incomplete pairs), not an early promotion decision. Settled references use the
actual final valid pairs. Changes are separate horizontal segments, never slopes;
unknown or unattainable thresholds and mixed large-history buckets leave gaps.
Duration plots show actual total active seconds (axes use readable time units).
Missing measurements are marked unavailable and are never interpolated. Large
series use at most 600 min/max buckets, preserving promotion and regime markers;
the full table and statistics remain exact. Tooltips identify individual values or
aggregated generation spans. Regimes are consecutive equal depth/games/pairs/thread
settings; returning to earlier settings starts another regime. Their context does
not establish causation. Best lineage lists which generation/network became Best
and when, without inventing an absolute-strength or Elo curve. No fixed-anchor
matches or training-policy selection are introduced.

Dashboard shows absolute and invocation generation progress, continuous-run status,
elapsed/time budget, effective position generation and validation methods. Detailed
configuration is expandable below the charts/history. Held-out
progress counts actual sample comparisons in chunks of 256, including the configured,
WDL and NNUE component passes when applicable. Optimizer snapshot publication is
coalesced to at most once per 50 ms, with final phase publications retained.
Recent comparison/duration previews cover the last 25 rows and the table the last
five. Both history tables expose final training loss and candidate/incumbent validation metrics separately
from concise outcomes; INCONCLUSIVE and historical CANCELLED remain distinct from
BEST RETAINED. Full identities and secondary diagnostics remain available.
The History view provides deeper analysis and keeps the accepted Configuration,
Diagnostics, independent Training board, lifecycle and Play workspaces intact.
The GUI never writes authoritative history. Its existing serial I/O executor reads
and caches immutable snapshots; metadata is checked at most every five seconds or
on a changed completion counter. The 500 ms EDT refresh does not read files or
recompute unchanged charts/tables. Rendering visits at most 600 buckets; table rows
are virtual. Full-range preparation is linear and occurs only on data/range/day
changes. The trainer appends once per generation outside its start/stop monitor
and all search/optimizer hot paths. The existing OS checkpoint ownership remains
held for the run, so a forced append adds its measured latency to that run; no new
checkpoint lock, GUI monitor coupling, background writer or shutdown protocol is
introduced.

`HistoryRepositoryTest` and `HistoryAnalyticsTest` exercise storage/recovery and
range math. Trainer service/control/history tests verify actual settlement,
original incumbents, promotion, retained/inconclusive/cancelled results, failures,
restart and real bounded two-generation runs. `TrainingHistorySmokeTest` uses
explicit deterministic persisted fixtures for multiple regimes and captures
Dashboard/History/navigation/resizing under `app/build/gui-smoke/<scale>/`.
`TrainingWorkspaceSmokeTest`, Play tests and slow NNUE GUI tests retain native
runtime and simultaneous Play/Training coverage. The existing `flatlaf.uiScale`
JVM property exercises 150% scaling.

### Native file and folder pickers

All desktop pickers use the shared GUI picker service. Windows uses the shell's
Common Item Dialog through JNA, with folder-picking mode for store/root choices.
**Add source** and **Change location** accept either a file with Open or the
currently displayed directory with **Select this folder**. Double-clicking folders
continues to navigate normally.

macOS uses AWT FileDialog backed by the native AppKit panel. For a Training Data
source, first choose File or Folder, then use the native panel. Other operating
systems use the isolated Swing fallback. Windows/macOS native failures are reported
instead of silently switching to a Swing chooser.

Picker history lives under the existing Java Preferences training node's
`pickers` child. Add/relocate source, training storage, adopted checkpoint,
generator, teacher, corpus archive/root, and White/Black/Opponent stores have
separate stable keys. Approval flushes the selected directory (or a file's parent)
to preferences. Cancel leaves history unchanged. Invalid history falls back to
the current configured path, then Documents/home; existing corpus archive and
store preferences seed the configured-path fallback. Editable application settings
retain their existing meaning. The archive picker retains its actual `.zst` filter.

JNA and jna-platform are runtime JAR dependencies copied by both existing
jpackage tasks. Native COM objects and event callbacks stay on a dedicated
Windows STA, with a Swing modal owner keeping the EDT responsive. Only the chosen
platform backend initializes its native bindings. No helper executable is used.

### Application version and About

The source-controlled root `VERSION_STATE.txt` is the sole numeric application
version authority: exactly the non-negative JSON integers `major`, `minor`,
`patch`, and `build`. Only the user changes major/minor/patch. Automation changes
only build, by one, and never resets it when another number changes.
SeedV6 previously had no application build counter; old Windows images inherited
jpackage's default `1.0`. The initial `0.0.0`, build `0` was explicitly selected
by the user; it is a new counter, not a recovered historical number.

The existing shared Codex completion workflow applies through root-file presence.
Use the maintained `C:/projects/codebase-entropy-control/scripts/codex-versioning/finalizer.py`
with `--repo <repository-root> begin` before implementation, retaining its token.
After implementation and required validation, use the same token with
`finish <token> --decision bump` for qualifying built/shipped application changes,
or `--decision no_bump` for non-qualifying work. The existing semantic distinction
is unchanged: read-only analysis, diagnostics, testing-only, documentation-only,
and non-runtime tooling work do not acquire a bump just by running commands.
The finalizer verifies build-only mutation; same-token retries cannot bump twice.
Follow the maintained binding and safety checks in the shared Codex instructions.
There is no Gradle bump task, local completion hook, or second version writer.

Normal resource processing generates `application-version.properties` inside the
application JAR from the canonical file. Every build reinspects Git metadata;
the resource is only rewritten if its contents change. About reads these embedded
resources, never a checkout or external version file. It shows the version, build,
source revision, version-update date, desktop build identity, and build platform.
The revision is the short HEAD SHA, labelled when the working tree has uncommitted
changes. The version-update date is the most recent Git commit date affecting
`VERSION_STATE.txt`. Uncommitted version changes say that their date is unavailable;
source archives/missing Git history say Unknown. Filesystem times and build times
are never substituted for the version-update date.

`gradlew run` retains its UCI entry point; `gradlew run --args=gui` opens the desktop
GUI. Development resources contain no desktop stamp and About says
`Desktop build: Development run`. File > Exit invokes the same cooperative cleanup
as closing the main window. Help > About is a small modal using the existing theme
and icon.

Native packaging copies the development distribution into an isolated staging
directory, embeds `desktop-build.properties` with a UTC `builtAt` instant in that
copy of the JAR. Windows passes the embedded major.minor.patch to jpackage's native
application version. macOS uses major.minor.patch for `CFBundleShortVersionString`
and the same shared build number for `CFBundleVersion` (macOS jpackage requires
a positive build number). Both retain the complete embedded application identity.
An old image keeps its original version/revision/timestamp even as the checkout
changes. Packaging does not stamp the development JAR or modify `VERSION_STATE.txt`.
Builds, tests, run, and repeated packaging never increment the counter.
The Java reader and Gradle resources are platform independent; both packagers
stamp the same desktop resource in their own packaging copy.

Focused headless checks: `gradlew :app:test -Pheadless --tests '*ApplicationVersionTest'
--tests '*ApplicationMenuTest' --tests '*VersionResourceBuildTest'`. The maintained
finalizer's disposable-repository regression suite covers build-only, no-bump and
exactly-once completion semantics; do not mutation-test version bumps in this checkout.

### Standalone Windows and macOS NNUE application

Run the matching native command on each platform:

```bash
# Windows:
./gradlew packageWindows

# macOS:
./gradlew packageMac
```

On macOS, set `JAVA_HOME` to a self-contained macOS JDK 21 or newer containing
`jpackage`, such as Eclipse Temurin. Package-manager JDKs that link to external
libraries (such as Homebrew's font libraries) are rejected because copying their
runtime alone would not produce a standalone application.
The task builds `:app:installDist` without tests and creates
`dist/mac/<UTC timestamp>/SeedV6-NNUE.app`. Open that `.app`
to launch the existing Play / Network Training GUI. Its dependencies, resources
and private Java runtime are included; no separately installed Java is needed.
The launcher/runtime architecture follows the JDK executing Gradle (arm64 on
an Apple Silicon JDK); this is a native build, with no cross-architecture step.
The task only packages on macOS and creates no DMG or PKG. It requests no
Developer ID signing or notarization; jpackage handles its normal local image
creation without Apple credentials.

The canonical application artwork is repository-root `crowned_seed_emblem.png`.
Regenerate the checked-in runtime PNGs and Windows ICO with
`python tools/generate-app-icons.py` (requires Pillow). The PNGs in
`app/src/main/resources/com/ohinteractive/seedv6/gui/icons/` and
`tools/icons/seedv6.ico` contain 16, 20, 24, 32, 40, 48, 64, 96, 128 and 256 pixel
representations of the complete artwork. Runtime window icons, application
identity, engine badges, Training header and About use these PNG resources.

The macOS icon is generated on each packaging run from the same canonical PNG,
using macOS `sips` and `iconutil` for the
standard 16–1024 pixel representations. Generated icon/staging files remain in
`app/build/desktop-input/`; no artwork is changed. Like Windows, every invocation
creates a new ignored snapshot outside `build/`, preserved by Gradle clean.
Keep the complete `.app` together and training data outside the application image.

Windows packaging continues as follows.

From the repository root, build a snapshot of the current working tree using a
Windows JDK 21 or newer with `jpackage` (`JAVA_HOME`):

```bash
./gradlew packageWindows
```

In PowerShell, use `.\gradlew.bat packageWindows`. This is the root task
`:packageWindows`; it runs `:app:installDist`, then packages its output without
running tests. All paths are anchored to the repository root. Discover it under
Distribution tasks or inspect its help:

```bash
./gradlew tasks
./gradlew help --task packageWindows
```

Allow any other Gradle build/test run in this checkout to finish before packaging;
they share development build outputs. An already-packaged trainer can keep running.

The standalone script remains available, including its optional `-JdkHome`:

```powershell
powershell.exe -NoProfile -File .\tools\package-windows.ps1
```

Both entry points build the application without running tests before creating
`dist/windows/<UTC timestamp>/SeedV6-NNUE/SeedV6-NNUE.exe`. Double-click this
executable to open the existing Play / Network Training GUI without a console.
Keep the entire `SeedV6-NNUE` folder together: its JARs, resources and private
Java runtime are included. No separately installed Java, Gradle, IDE or terminal
is needed to run it. Each packaging run creates a new folder and leaves earlier
snapshots alone; ordinary Gradle clean/build operations do not remove them.

Network Training uses a machine-local **Base Training Root**, chosen under **File > Training storage settings**.
New named lineages live at `<base>/<architecture>/<UUID>/`; their display names and
editable settings are stored in additive `training-lineage.bin` metadata. Select
an architecture and Training Lineage above the Dashboard, or use **New Lineage...**
for that architecture's defaults. The Dashboard identifies the selected lineage.
**Import...** adopts an existing store in place and remembers its location; it
never copies or moves checkpoint payloads. Existing architecture folder preferences
are retained as catalog entries. Naturally nested stores establish the initial base.
Legacy stores without complete saved editable settings load architecture defaults
plus durable source/seed settings, with a visible Recipe & lineage notice. Historical
generation evidence remains unchanged; incompatible edits retain the existing
unfinished-generation restart semantics. **Apply settings** and Start save settings
to the selected lineage, independently of other lineages and machine preferences.
Keep them outside `build/` and the application image. Each completed
checkpoint contains `network.nnue` (NNUE), `network.brn` (BRN-0), `network.brn1` (BRN-1), or `network.brn2` (BRN-2),
`training.state` (model and Adam state), and
`manifest.bin`; `refs/best`, `refs/latest-training`, `validations`, `promotions`
and publication staging remain in that same store. Resume continues Latest
Training; Best changes only through the existing bootstrap/promotion rules.

At a clean generation boundary the control reads **Start Training**; durable
unfinished work reads **Resume Generation N** (or **Restart Generation N** when
incompatible settings require the existing archive/restart flow). Switching lineages
loads configuration, checkpoint identity, progress, history and this action together.
While running, **Stop after Generation N** finishes generation, validation,
promotion/retention, publication and history handling before any next-generation
work begins. **Cancel Scheduled Stop** restores continuation; **Stop Now** remains
available and uses the existing cooperative interruption and partial-save behavior.
A failed validation remains a failure, and any history persistence warning stays
visible. Architecture, lineage and configuration editing are locked during training.

**Validation & run > Stop policy** offers Unlimited, a generation count, or a time
budget; legacy combined limits remain explicit. A compatible resumed partial generation
counts toward that invocation's generation limit. From completed Gen 10, ten generations
ends after Gen 20 settlement/history, without Gen 21 initialization. Historical Candidate
reconciliation remains separately counted.

Each Start/Resume gets a fresh monotonic time budget. Expiry closes admission of the
next generation and finishes the current generation, including validation and history.
**Stop Now** separately requests immediate cooperative cancellation and exact partial
saving. Limits do not invalidate partial work. Incompatible recipe/generation edits
retain the existing archive/restart flow. BRN-2's immutable run-seed contract remains;
changing it requires a new lineage.

Use Stop Now (or Stop after Generation) and wait for the safe stop before switching application
versions. Only one process can own a store: its OS file lock rejects another
trainer. Play can run concurrently with Training, using its own search workers,
TT, cancellation and evaluator state. The Play and Training thread controls are
independent; choose each limit to suit the CPU resources you want to assign.
Both engine Play modes load validated immutable network snapshots at game creation,
without acquiring the trainer's writer lock. Human vs Engine resolves its opponent
choice at New Game; Engine vs Engine resolves its White and Black choices at Start Game.
Promotions become available to subsequent games; the current game retains its
pinned networks. Stop/reset in
either tab affects only that tab, and closing the window drains both runtimes.
Development can continue alongside the packaged trainer; use a separate store
if a development instance also needs to train. GUI preferences are shared by
instances running under the same Windows account. Replacing an image does not
replace training data; keep backups of the external store and do not delete a
running image. The existing protocol detects corruption and uses atomic
publication, but does not promise universal power-loss durability on Windows.

## Optional small endgame tablebases

Managed UCI and Play searches can use local Syzygy tables to choose a proven
winning root move. This optional capability supports the verified three- and
four-piece set. It does not replace ordinary Search scores, report DTZ as mate
distance, change the exact/reference or training drivers, or probe inside the
recursive search. Unavailable evidence, repeated active history, castling,
non-winning outcomes and positions too close to the fifty-move boundary use
ordinary Search. Default builds and searches do not require tablebases.

Build the optional native provider on the target host:

```text
gradlew :app:installDist -PwithSyzygy
```

Windows requires the Visual Studio C++ build tools; macOS/Linux use the host
C++ compiler and Java 21 headers. Windows x64 is runtime-validated; the other
native builds still require platform validation. A standard Java build remains
available without a C++ compiler. The optional JAR contains the native provider
and its third-party license, but no tablebase data. Use a separate directory
containing the 70 canonical three/four-piece WDL and DTZ files; their required
SHA-256 hashes are in
`app/src/main/resources/com/ohinteractive/seedv6/search/tablebase/small-tables.sha256`.
The canonical data directories are
[WDL](https://tablebase.lichess.ovh/tables/standard/3-4-5-wdl/) and
[DTZ](https://tablebase.lichess.ovh/tables/standard/3-4-5-dtz/).
Only filenames in the manifest are needed. No data is downloaded automatically.
All 70 hashes are checked before native initialization; leave the files unchanged
while the process runs.

Set `-Dseedv6.syzygy.config=C:/chess/syzygy.properties` in the JVM launch options.
The UTF-8 properties file contains:

```properties
path=C:/chess/tables-3-4
```

Use forward slashes in properties paths. For a locally compiled provider outside
an optional JAR, also set `library=C:/path/to/seedv6_syzygy.dll` (use the host's
library filename). `seedv6.syzygy.path` and `seedv6.syzygy.library` JVM properties
can override these entries. Configuration is fixed for the process lifetime.
The config-file route supports non-ASCII data paths; on the tested Windows JDK,
the native library itself must reside on a path representable by the host's
native encoding. A bundled library uses a hash-verified cache under the JVM's
temporary directory, shared by launches using that same binary. A failed
checksum, missing native library or unsupported setup
prints a diagnostic on stderr and falls back to ordinary Search.

A completed tablebase decision publishes `info string tablebase win dtz N` and
`bestmove` in UCI, with no fabricated completed depth or numeric Search score.
Play displays the winning side. Infinite analysis still waits for `stop` before
publishing its final move. Probes are serialized and cooperative cancellation is
checked before and after native work; a native/file-system call is not preemptible.

## Notes for Readers

SeedV6 is a work-in-progress chess engine and an engineering project.

Some code is intentionally low-level or unconventional by normal application-Java standards because it exists in paths that may execute millions or billions of times.

The repository is public so that the architecture, experiments, correctness work, performance work, and evolution of the search engine can be followed as the project develops.

Performance results in this README are observations from the stated development environment rather than universal performance guarantees.

## License

No reuse or redistribution rights should be assumed unless and until an explicit project license is added.

The vendored Fathom source has its own MIT license in
`app/src/main/native/syzygy/fathom/LICENSE`; that license applies to those files.
