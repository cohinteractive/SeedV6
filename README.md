# SeedV6

SeedV6 is a high-performance Java chess engine and desktop application under active development.

The project is focused on building a strong chess engine around a fast, compact core designed specifically for the JVM. SeedV6 uses bitboards, packed primitive state, direct legal move generation, PEXT-based sliding attacks, reusable buffers, caller-owned move destinations, staged move picking, and allocation-conscious search structures.

The goal is not merely to produce a working engine. SeedV6 is also an experiment in how far a carefully designed Java implementation can push chess-engine performance while remaining compatible with progressively more sophisticated search.

SeedV6 runs as a standalone UCI engine or through a desktop application with Play, Network Training and Arena workspaces. The production engine combines selective search, cooperative root parallelism and the bundled WDL25 NNUE evaluator. The GUI and UCI applications share the same engine core.

## Highlights

Current SeedV6 features include:

- Direct legal-only move generation.
- Separate legal generation stages for evasions, tactical moves, and quiet moves.
- PEXT-based sliding-piece attacks in the legal generator.
- Compact bitboard and packed board-state representation.
- Reusable board and move buffers in hot paths.
- JVM/JIT-conscious hot-path design.
- FEN parsing and Zobrist hashing.
- Extensive perft validation infrastructure.
- UCI position handling and legal move replay.
- Asynchronous search lifecycle with cancellation and search limits.
- Position history and draw adjudication.
- Iterative deepening, PVS, aspiration windows and check-aware quiescence.
- Incremental NNUE evaluation, with alternative neural and handcrafted evaluators.
- Static exchange evaluation (SEE), transposition tables and staged move ordering.
- Killer moves, countermoves, capture history and continuation history.
- Selective reductions, pruning and bounded extensions.
- Cooperative root search with 1–16 workers.
- Desktop play, model lineages, checkpoint continuation and paired Arena matches.
- Exact full-width search baselines retained as correctness oracles.

Historical single-worker perft measurements exceeded **120 million nodes per second** on some positions, including more than **6.9 billion nodes at approximately 126.9 million NPS**. These figures measure board mechanics, not playing strength or actual chess-search throughput; their context is recorded below.

## Project Goals

SeedV6 is built around several priorities:

- Correct chess rules and reliable legal move generation.
- High single-threaded mechanical performance.
- Minimal allocation in search hot paths.
- Compact, cache-friendly primitive data structures.
- Code shapes that give the JVM and JIT compiler useful optimization opportunities.
- Strong correctness baselines that remain available while search becomes increasingly selective.
- Measurable performance improvements rather than assumed optimizations.
- A search architecture that can make effective use of multiple CPU cores.

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

Search code works with reusable board storage and writes child positions into caller-owned buffers rather than allocating a new board object at every node. The same philosophy is used for move storage, move ordering, search stacks, history information, and other frequently accessed search data. This is an allocation-conscious design, not a claim that every application or search operation is allocation-free.

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

SeedV6 uses PEXT-based sliding attack lookup in its direct legal move generator.

Earlier Seed engines used magic-bitboard techniques, but SeedV6 moved its main move-generation path to PEXT. In this Java implementation the PEXT approach produces smaller and simpler hot methods and has proved faster in the project's historical measurements.

Rook, bishop, and queen move generation uses the SeedV6 PEXT infrastructure. Magic-bitboard tables and attack/check helpers are still present; they have not been completely eliminated from the codebase.

This is representative of the wider SeedV6 design philosophy: implementation choices are selected according to measured behaviour in this engine rather than simply because a technique is conventional in other chess engines.

## Move Application

Move application is designed around reusable destination boards rather than allocation.

Production `Board.makeMoveInto()` reads the encoded move and writes every field of a caller-owned destination board. It handles captures, switches on the moving piece type, and applies the necessary castling, en-passant, promotion, move-counter and Zobrist updates within that method.

Specialized move-type transition implementations remain useful comparison and perft tools, but they are not the production move-application dispatch. The production path uses the unified `makeMoveInto()` implementation.

The JVM optimization principle remains the same: mechanically simple code, compact primitive state and reusable storage give the JIT compiler useful opportunities to optimize common paths. Method size and inlining are matters to measure rather than assumptions about which implementation must be fastest.

## Perft and Correctness

Perft is one of the principal correctness tools used during SeedV6 development.

The perft suite contains positions involving:

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

The suite records expected node counts for 20 positions. The historical README reported a complete passing run; that record is not a claim that the full suite has been rerun against the current engine.

Large perft runs are particularly useful because they simultaneously exercise legal generation and board transitions billions of times. A wrong move, corrupted position, incorrect special-move transition, or legality error normally causes the known node counts to diverge.

## Perft Performance

SeedV6's optimized core has produced high single-threaded perft throughput for a Java chess engine.

The following historical measurements were recorded in the earlier README. They are retained as engineering evidence, not presented as current benchmark results:

| Position | Depth | Nodes | Time | Throughput |
| --- | ---: | ---: | ---: | ---: |
| Initial position | 6 | 119,060,324 | 1.250 s | 95.2M NPS |
| Kiwipete | 6 | 8,031,647,685 | 69.239 s | 116.0M NPS |
| Promotion and castling tactics | 6 | 706,045,033 | 6.225 s | 113.4M NPS |
| Complex middlegame | 6 | 6,923,051,137 | 54.561 s | **126.9M NPS** |

Recorded benchmark environment:

- AMD Ryzen 5 5500
- 32 GB RAM
- Windows 11 Pro
- Java 21
- Single perft worker thread

The most significant figures are the long-running multi-billion-node tests rather than very short positions where timer resolution and JVM effects can dominate the reported NPS.

These numbers are **perft throughput, not chess-search NPS**.

Perft does not perform evaluation, transposition-table probing, search reductions, pruning, or other work performed by a real chess search. The results instead measure the throughput of the underlying legal move-generation and board-transition machinery.

They provide useful historical evidence that the optimization work in SeedV6's board representation, PEXT attack generation, direct legal generation, move encoding, and move-application paths produced practical results.

Performance will vary with position, hardware, JVM behaviour, system load, and warm-up state.

## Search Architecture

SeedV6 search is a V6-native implementation built around the engine's own board representation and reusable storage. The production main search and quiescence loops use explicit per-ply frames, with independent simpler implementations retained for comparison.

The following sections explain the architecture. The detailed [Search and Evaluation feature inventory](docs/SEARCH_AND_EVALUATION_FEATURES.md) describes the active production feature set.

### Iterative Deepening, PVS and Quiescence

Iterative deepening searches successive depths, retaining the last completed result and principal variation. Each iteration can reuse the previous principal variation and the effort spent on root alternatives. Adaptive aspiration windows centre the next search on the previous score, widen after a failed window and fall back to a full window when needed.

The main search uses fail-soft alpha-beta negamax with Principal Variation Search (PVS). It searches the first move with a full window and later moves with narrow scout windows, re-searching improving alternatives as needed. Good ordering therefore saves work by finding useful bounds early.

At the nominal horizon, quiescence continues through captures and promotions instead of evaluating a tactically unfinished position immediately. Outside check it can use a stand-pat evaluation; in check it searches all legal evasions. SEE-based filtering skips selected losing nonchecking captures while protecting promotions, en passant and captures that uncover additional friendly slider attacks.

### Exact Search Baseline

SeedV6 retains a deterministic full-width negamax traversal that can be used as a correctness baseline.

Maintaining a simple exact search is intentional. Alpha-beta traversal, move ordering and mechanical optimizations can be checked against independently simpler implementations under matching search conditions. Selective pruning changes which lines are examined, so arbitrary production scores are not expected to equal a full-width search. Recursive search and full-recompute evaluation references also remain available to check optimized paths.

### Position History and Draws

Search supports position-history tracking and rule-aware draw handling, including formal threefold repetition, the 50-move rule and supported insufficient-material draws. Checkmate and stalemate are resolved from legal-move availability.

Game history and search-line history are kept separate so search can explore and restore lines without corrupting the real game history.

### Evaluation

The production default is **WDL25**, a bundled, quantized NNUE network. It supplies the complete static evaluation; the normal NNUE score does not add a separate handcrafted material or positional term.

The network uses 7,680 sparse piece-square inputs, including both kings, organised into ten king-location buckets. Each side has a normalized perspective, with rank reversal for Black and file mirroring based on its king. A shared 256-unit feature transformer produces each perspective's accumulator. The combined 512 activations feed one of eight scalar output heads selected by the total piece count.

Incremental evaluation reuses the parent accumulators and adds or subtracts only changed piece features. When a king changes bucket or mirroring orientation, the affected perspective is rebuilt. Each worker and search ply owns its accumulator state, so sibling moves and retries preserve their parents.

Inference uses quantized integer parameters, 32-bit accumulators, squared clipped-ReLU activations and 64-bit output sums. The output calculation has a Java Vector API implementation and a scalar fallback. This keeps the frequently executed evaluation path compact and performance-conscious without requiring floating-point training machinery during play.

UCI reports these scores in centipawn format, but they express the network's learned expected-score scale. They are not literal material pawn values or calibrated winning probabilities, and scores from different evaluator families should not be treated as interchangeable measurements.

**CP64** remains bundled as an alternative and rollback network with the same inference architecture. Selecting `--network cp64` in the standalone UCI application changes the network while retaining the normal production search configuration. WDL25 and CP64 are also available in desktop Play and fixed-evaluator Arena matches.

SeedV6 retains a phase-aware **handcrafted evaluator (HCE)** adapted from useful SeedV3 concepts, with material and positional terms operating on SeedV6's board representation and PEXT infrastructure. **BREP2-32** is a frozen generation-32 BRE-Pair 2 evaluator using compiled pair tables with a material prior and calibrated learned residual. These alternatives share the engine infrastructure and resource controls, but retain their own search configuration rather than all of the conventional NNUE production refinements.

The frozen WDL25 and CP64 playing networks are distinct from the trainable NNUE, BRN-3 and BRE-Pair 2 lineages available in Network Training. Research architectures and checkpoints do not become production evaluators merely by being present in the repository.

Rule-based draws remain separate from static evaluation.

### Static Exchange Evaluation

SeedV6 includes a legal static exchange evaluator used to determine the material outcome of tactical exchanges.

The implementation handles difficult cases including:

- x-ray attackers,
- pinned pieces,
- king recaptures,
- en passant,
- promotions,
- and underpromotions.

### Transposition Table

A transposition table lets different move sequences reuse information about the same position. SeedV6's table provides:

- full position-key verification,
- depth-qualified entries,
- exact, lower, and upper search bounds,
- complete move preservation,
- mate-score normalization,
- replacement policy,
- search generations,
- and concurrency-safe publication.

Production keys also account for rule state and relevant repetition history. Completed move hints guide ordering, while selective score estimates have stricter reuse conditions: interior null-window cutoffs require sufficient depth, a compatible bound, a legal associated move and a non-mate estimate from the current move search. A bound from selective search is useful evidence, not proof of the full-width minimax value.

### Staged Move Ordering

Search move selection builds directly on SeedV6's staged legal generator.

The move picker can prioritize:

1. a validated transposition-table move,
2. non-losing tactical moves, ranked using material and capture history,
3. quiet killer moves and countermoves,
4. quiet moves ranked by history and immediate continuation history,
5. losing tactical moves.

Moves remain complete opaque move identities while scores and ordering metadata are stored separately. The picker is designed to emit every legal move exactly once; selective search may then reduce or omit eligible moves.

History heuristics learn which moves have caused useful cutoffs. Capture history refines tactical ordering; quiet history tracks successful quiet moves; continuation history relates a move to the preceding move. Killers remember useful quiet cutoffs at the same ply, and countermoves remember replies to earlier moves.

Each worker retains this learned information across depths, aspiration retries and root jobs within one move search. Rewards and penalties exclude results obtained only at reduced depth. Previous principal variations and root effort provide additional ordering information between completed iterations.

### Search Diagnostics

Search can publish cumulative diagnostic snapshots covering main-search and quiescence nodes, transposition-table use, ordering, cutoffs and aspiration retries. Workers accumulate primitive counters locally, and snapshots are produced at reporting boundaries.

These measurements help explain where a change saves or adds work. Formatting and diagnostic reporting are kept outside the per-node hot path; elapsed time and NPS remain measurements affected by the JVM and host environment.

### Selective Search Policy

Production search spends more effort on promising or tactically important lines and less on moves unlikely to change the result.

- **Late-move reductions** search later quiet moves at reduced depth, then verify promising results at unreduced depth. History, improving evaluations, refuted reduced probes and suitable transposition evidence can make reductions more cautious.
- **Late-move and futility pruning** omit selected late quiet moves at shallow nodes when move order and static evaluation suggest little prospect of improving the bound. Protected moves and sparse endgames receive more conservative treatment.
- **Reverse futility pruning** can cut off eligible shallow nodes when static evaluation comfortably exceeds beta, subject to check, material and rule-clock safeguards.
- **Verified null-move pruning** tests a reduced search after a synthetic pass, then requires a separate reduced search with legal moves before accepting the cutoff.
- **Tactical verification (ProbCut)** probes selected non-losing captures or promotions with quiescence and a reduced legal search before accepting a cutoff above an elevated threshold.
- **Bounded extensions** add a ply for a single forced check evasion near the horizon or a qualifying same-square recapture, capped at one extra ply along a line.
- **Mate-distance bounds** tighten windows to scores achievable at the current ply.

These mechanisms are selective heuristics with eligibility and verification safeguards. Correctness references establish chess mechanics and comparable search behaviour; controlled measurements and games are needed to assess the practical value of selectivity.

### Deterministic Search Benchmark

The single-worker search benchmark uses a named position corpus, repeatable limits and explicit cold or deterministically primed warm transposition-table conditions. It checks repeatability of results, principal variations, node counts and enabled diagnostics, while treating elapsed time and NPS separately.

This makes it useful for detecting mechanical regressions and comparing search work. Neither perft speed nor a reduction in benchmark nodes alone establishes stronger play.

## UCI and Search Lifecycle

SeedV6 includes a standalone UCI application for standard-stream chess clients. Its normal launcher starts the bundled WDL25 production engine without requiring a training store or external model path.

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

This lifecycle is separate from the number of chess-search workers. A single-worker search remains asynchronous, while cooperative multi-worker search uses the same cancellation and result-publication boundaries.

UCI exposes configurable worker count and transposition-table memory through `Threads` and `Hash`. The bundled opening repertoire is disabled by default in UCI and can be enabled with `setoption name UseBook value true` (or the `OwnBook` alias). No external book file is required.

See the [UCI operating instructions](docs/guide.md#uci-and-search-lifecycle) for configuration and client integration.

## Desktop Applications

The desktop application brings the same engine together with three workspaces:

- **Play** supports human and engine play, board interaction and analysis display, with evaluator selection, opening-book controls and configurable engine resources.
- **Network Training** manages named model lineages and their generations. A lineage keeps its architecture, weights, recipe and optimizer state together, with checkpoint continuation that preserves training progress. A shared data library lets Training and Arena use registered sources without merging their independent lineage cursors.
- **Arena** runs fixed-evaluator matches or learning campaigns. Match-only mode compares HCE, BREP2-32, WDL25 or CP64 over colour-reversed game pairs without training. Learning campaigns start from fresh models or copied checkpoints, train independent competitors on the same frozen records and targets, and compare them before training and after each round. Saved campaigns retain their configuration and completed results for continuation.

Frozen conventional NNUE networks are playing choices, not trainable lineages in the learning-campaign selector. Training and research models retain their own architecture and score semantics.

GUI Play, fixed-evaluator Arena and standalone UCI use `engine-core`'s shared `BaselineEngine`. The interface does not maintain a second chess engine, and a bundled playing model does not require a local training dataset.

See [desktop Play](docs/guide.md#desktop-play-workspace), [Network Training](docs/guide.md#network-training-lineages-and-sessions), the [Training Data guide](docs/corpus-training.md) and the [Arena guide](docs/learning-arena.md) for detailed operation.

## SeedV3 and the Engine's History

SeedV6 follows several earlier Seed chess-engine projects.

**SeedV3** provided useful higher-level engine experience during SeedV6's development.

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

SeedV3 served as a source of algorithms, lessons, evaluation ideas, search techniques, and working-engine experience.

Features were adapted to SeedV6 rather than copied mechanically. SeedV3 compatibility is not a requirement to preserve known correctness or performance weaknesses, nor is recreating its remaining features a standing development objective.

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
- efficient move-transition code,
- local primitive state,
- low allocation rates,
- bounded heuristic structures,
- reduced branching,
- compact data layouts,
- and methods shaped with JVM inlining in mind.

Performance work is treated experimentally.

An implementation is not assumed to be faster simply because it appears more sophisticated. Where possible, optimizations are validated using perft, deterministic search comparisons, focused benchmarks, profiling, or controlled game-strength testing.

Correctness baselines are deliberately preserved so mechanical or search optimizations can be tested against independently simpler implementations.

## Concurrency

SeedV6 supports **1–16 search workers**, with one worker as the default. With multiple workers, production search cooperatively distributes root moves from depth three; shallower iterations and roots with at most one legal move use the owner alone.

Each worker has private board storage, search stacks, position history, learned ordering state and NNUE accumulators. Workers share synchronized transposition-table resources and the search's time, node and cancellation limits.

Finite aspiration-window passes can start root jobs concurrently. Full-window passes search the first root move before distributing alternatives. The coordinator gathers the results and drains helper work before publication or reuse, preserving the boundary around a completed iteration.

SeedV6 retains the lifecycle and state-ownership separation that allows searches to execute and cancel independently of the UCI command thread. The architecture remains grounded in comparison with the established single-worker engine, with explicit ownership of mutable search state.

Support for a worker count does not imply demonstrated playing-strength scaling at that count. Scheduling overhead, position, time control, JVM behaviour and hardware all affect the result; scaling across every worker count and platform is not established.

## Project Status

SeedV6 is a functioning chess engine and desktop application under active development. It supports UCI play, selective search, NNUE evaluation, cooperative root search, desktop play, training and Arena comparisons.

Development continues through measured optimization, correctness work and controlled experiments. Research implementations and historical results remain separate from the normal production choices.

A locally built application is a development build, not an official release or a universal performance or strength qualification.

## Repository Structure

The repository is organised around shared engine responsibilities and separate application entry points:

- `engine-core` — board state, legal move generation, rules, search, evaluation, model inference and opening-book resources.
- `engine-training` — trainers, model workspaces, checkpoint continuation, corpus readers and campaign services.
- `app-gui` — desktop Play, Network Training, Arena, preferences and GUI resources.
- `app-uci` — the standalone UCI entry point, protocol/session implementation and bundled model selection.
- `research` — experiments, independent correctness references, perft, diagnostics and empirical tools, excluded from production distributions.
- `app` — retained compatibility launchers and legacy checkpoint configuration. Its legacy UCI default differs from standalone UCI; use `app-uci` for the normal production engine.
- `src/integrationTest` — tests spanning application, training and research boundaries.
- `build-assets` — bundled production model inputs and packaging assets.
- `docs` — operating instructions, architecture, the production feature inventory and research evidence.
- `source` — search research contracts and their supporting frontier documentation.
- `tools` / `gradle` — development utilities and shared build/packaging conventions.
- `archive` — historical instrumentation outside the active source sets.

Within the engine, responsibilities remain separated into core mechanics and utilities, search, transposition storage, move ordering and search lifecycle management. See the [architecture documentation](docs/architecture.md) for source boundaries.

## Requirements

SeedV6 is written in Java and uses a multi-project Gradle build.

Development requires an installed **Java 21 JDK** and Git. Use the checked-in **Gradle wrapper**; a separate Gradle installation is not required. The build uses installed JDK toolchains rather than downloading a JDK for you.

Native packaging additionally requires **Python 3.9+** and a complete, stable **Java 21 JDK, version 21.0.6 or newer within Java 21**, including `jpackage`, `jlink` and `jdeps`. Packaging targets **Windows x64** on Windows and **macOS arm64** on macOS; these are native-host builds rather than cross-compilation targets.

## Building

Run these commands from the repository root. On Windows, use PowerShell:

```powershell
# Build the project.
.\gradlew.bat build

# Launch the desktop application directly.
.\gradlew.bat :app-gui:runGui

# Install and run the standalone UCI development distribution.
.\gradlew.bat :app-uci:installDist
.\app-uci\build\install\seedv6-uci\bin\seedv6-uci.bat
```

On macOS or other compatible Java development hosts:

```sh
./gradlew build
./gradlew :app-gui:runGui
./gradlew :app-uci:installDist
./app-uci/build/install/seedv6-uci/bin/seedv6-uci
```

The UCI launcher reads protocol commands from standard input. Omit arguments for WDL25, or append `--network cp64` to select the retained rollback network. The `installDist` distribution uses an installed Java runtime.

### Native Packaging

To package both applications with their own bundled Java runtimes:

```powershell
# On Windows x64:
.\gradlew.bat packageWindows
```

```sh
# On a native macOS arm64 host:
./gradlew packageMac
```

The same tasks are available independently as `:app-gui:packageWindows`, `:app-uci:packageWindows`, `:app-gui:packageMac` and `:app-uci:packageMac`.

Windows launchers are created at:

- `dist/windows/current/SeedV6-GUI/SeedV6-GUI.exe`
- `releases/current/windows-x64/SeedV6-UCI/SeedV6-UCI.exe`

macOS outputs are created under `dist/mac/current/` for the GUI and `releases/current/macos-arm64/` for UCI. Keep each complete application image or extracted archive together: its launcher depends on the bundled runtime and resources. End users of these native packages do not need a separate Java installation.

Ordinary native packaging replaces the previous successful output for that application; close the application before rebuilding it. Packaging performs integrity checks, but its output is a local development distribution rather than an official signed or published release.

Ordinary Git clones include the WDL25, CP64 and BREP2-32 model assets. They are embedded in the shared engine JAR used by the applications; no training store or manual model transfer is needed for normal play. Selected bundled models are checked for identity and integrity, and invalid assets fail explicitly rather than silently choosing a different evaluator. Packaging uses installed JDK tools and does not download JDKs or replacement models.

See the [build and test guide](docs/guide.md#building) and [native packaging instructions](docs/unified-architecture/WU7-PACKAGING.md#current-portable-build-commands-and-tracked-production-input) for operating details and packaging options.

## Notes for Readers

SeedV6 is a work-in-progress chess engine and an engineering project.

Some code is intentionally low-level or unconventional by normal application-Java standards because it exists in paths that may execute millions or billions of times.

The repository is public so that the architecture, experiments, correctness work, performance work, and evolution of the search engine can be followed as the project develops.

Performance results in this README are observations from the stated development environment rather than universal performance guarantees.

## License

No reuse or redistribution rights should be assumed unless and until an explicit project license is added. The repository does not currently include a project-wide license.

Bundled third-party components retain their own licenses and notices, including [Fathom](engine-core/src/main/native/syzygy/fathom/LICENSE), the [BINP decoder reference](engine-training/src/main/resources/com/ohinteractive/seedv6/training/data/binpack-NOTICE.txt) and the [Bullet training components](tools/conventional-nnue/trainer/UPSTREAM-NOTICE.txt). Those component licenses do not establish a license for SeedV6 as a whole.
