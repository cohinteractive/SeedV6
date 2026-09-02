# SeedV6

SeedV6 is a high-performance Java chess engine under active development.

The project is focused on building a strong chess engine around a fast, compact core designed specifically for the JVM. SeedV6 uses bitboards, packed primitive state, direct legal move generation, PEXT-based sliding attacks, reusable buffers, specialized move application, staged move picking, and allocation-conscious search structures.

The goal is not merely to produce a working engine. SeedV6 is also an experiment in how far a carefully designed Java implementation can push chess-engine performance while remaining compatible with progressively more sophisticated search.

SeedV6 is already capable of running as a UCI engine. Development is currently moving upward from the optimized board and move-generation core into the complete search architecture.

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

SeedV6 search is being developed as a V6-native implementation rather than by copying the structure of an existing engine.

Several important search foundations are already implemented.

### Exact Search Baseline

SeedV6 retains a deterministic full-width negamax traversal that can be used as a correctness baseline.

Maintaining a simple exact search is intentional. As alpha-beta pruning, transposition tables, ordering, quiescence, reductions, and other selective techniques are introduced, results can be compared against an independently simpler implementation.

### Position History and Draws

Search supports position-history tracking and rule-aware draw handling, including repetition and the 50-move rule.

Game history and search-line history are kept separate so search can explore and restore lines without corrupting the real game history.

### Evaluation

SeedV6 includes a phase-aware static evaluator adapted from useful concepts developed in SeedV3.

Evaluation includes material and positional terms while operating entirely on SeedV6's board representation and PEXT attack infrastructure.

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

A V6-native transposition table provides:

- full position-key verification,
- depth-qualified entries,
- exact, lower, and upper search bounds,
- complete move preservation,
- mate-score normalization,
- replacement policy,
- search generations,
- and concurrency-safe publication.

### Staged Move Ordering

Search move selection builds directly on SeedV6's staged legal generator.

The current move picker can prioritize:

1. a validated transposition-table move,
2. non-losing tactical moves,
3. quiet killer moves,
4. history-ordered quiet moves,
5. losing tactical moves.

Moves remain complete opaque move identities while scores and ordering metadata are stored separately.

The picker is designed to emit every legal move exactly once.

### Search Diagnostics

Production alpha-beta and quiescence search can optionally publish immutable,
cumulative diagnostics snapshots. One search worker owns a reusable primitive
accumulator; disabled search uses nullable hot-path checks and the shared empty
snapshot, with no atomics, event collections, formatting, or per-node objects.
Every standalone search resets the scope. One iterative search retains the same
scope across depths and aspiration retries, and each completed iteration freezes
the cumulative state at that publication point.

The metric definitions are intentionally narrow:

- `mainNodes` and `qNodes` classify successful `SearchControl.tryEnterNode()`
  child entries by the loop that owns them. Their sum equals authoritative
  search nodes; a qsearch leaf root already entered by main search is not counted
  again. Maximum absolute ply and qply are reached-depth maxima.
- TT probes count actual WS7 probes. Key matches, insufficient-depth matches,
  applied EXACT/LOWER/UPPER cutoffs, legally validated hash-move availability,
  and successful stores remain separate.
- searched moves count distinct legal main-search moves entered. Beta-cutoff rank
  is its one-based position in actual legal search order; a PVS re-search retains
  the original rank. Rank sums, maxima, fixed buckets, hash/tactical/quiet source,
  and precise current-ply killer or positive-history contribution are recorded.
- qsearch records entered checked qnodes, stand-pat cutoffs, searched tactical
  moves/evasions, soft-depth encounters, and qmate terminals. There are no
  fictional SEE/delta-pruning counters because production qsearch is unpruned.
- iterative counters define an aspiration attempt as one bounded narrow-window
  exact attempt. Every fail-low/high that causes widening is counted, while a
  full-window fallback is counted only after bounded attempts are exhausted.
  Completed iterations and deepest completed depth are controller-owned state.

Worker counter fields merge by addition and reached-depth fields by maximum.
Iteration state is deliberately not worker-mergeable; a future parallel
controller remains its sole owner.

### Selective Search Policy

WS13 adds three independently gated, single-threaded policies through the
immutable `SelectiveSearchPolicy`: mate-distance bounds, razoring, leaf
futility. `allOff()` preserves the committed WS12 search identity, `only(...)`
runs one heuristic in isolation, `with(...)` enables or disables one member of
a cumulative policy, and `production()` is the accepted bundle.

- Mate-distance bounds clamp only non-root windows to scores achievable at the
  current absolute ply. A collapsed impossible window returns without TT or PV
  fabrication.
- Razoring is limited to depth-one non-PV, non-check, normal-score nodes whose
  side-to-move static evaluation trails alpha by at least 250 centipawns. It
  launches the authoritative WS9 qsearch with the caller window and accepts
  only a completed result at or below alpha.
- Futility is limited to the same depth-one/non-PV/non-check/normal-score shape
  with a 180-centipawn margin. It always searches the first move and never
  skips captures, en passant, promotions, or moves that give check. Skipped
  moves never update history/killers, and a speculative upper result is not
  stored in the TT.
The donor check extension, reverse futility, null move, IID, and multi-prob-cut
are not in the accepted bundle. Reverse futility changed a shallow reference
score during WS13 isolation; verified null move increased aggregate benchmark
nodes/time; the other three lacked a defensible measured V6 contract. LMR is
deferred: it reduced cold-search work, but even after safe reduced-depth TT
storage it deterministically increased the warm-TT corpus from 22,588 to
39,722 nodes and reversed the timing result. It needs a separately justified,
TT-aware design.

WS13 diagnostics add only additive primitive counters for actual mate-distance
cutoffs, razor attempts/probes/accepted results, futility-eligible nodes and
quiet moves skipped.

### Deterministic Search Benchmark

`SearchBenchmark` runs the production iterative/alpha-beta path with one thread,
a named exact-FEN corpus, explicit diagnostics mode, and cold or deterministically
primed warm TT policy. It enforces result, PV, node, and enabled-counter equality
across repetitions while excluding elapsed time and NPS from deterministic
acceptance. A zero-duration sample reports NPS as unavailable.

On Windows, a representative correctness and overhead run is:

```text
.\gradlew.bat :app:searchBenchmark -PbenchmarkArgs="--depth=3 --warmup=2 --repetitions=5 --diagnostics=both --tt=cold"
```

Use `--tt=warm` for one identical excluded priming search per measured sample.
Use `--heuristics=all-off`, `--heuristics=production`, or a comma-separated
subset of `mate,razor,futility` to measure WS13 policy combinations.
Benchmark formatting and allocation are tool-only and never enter UCI stdout.

## Search Development Roadmap

The current search programme is progressively building the complete playing engine on top of these foundations.

The next major stages are:

- check-aware quiescence search,
- fail-soft alpha-beta / PVS,
- principal variation handling,
- iterative deepening,
- aspiration windows,
- richer UCI search reporting,
- search diagnostics and benchmarking,
- selective pruning and reductions,
- and multi-core search.

Selective techniques such as null-move pruning, futility pruning, late-move reductions, extensions, and related heuristics will be introduced only after the underlying search can be validated against simpler exact searches.

## UCI and Search Lifecycle

SeedV6 already contains a working UCI engine path.

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

Multi-core chess search remains an important future objective.

SeedV6 already has the lifecycle and state-ownership boundaries needed to execute and cancel searches independently of the UCI command thread.

The eventual parallel search architecture will be introduced only after the single-threaded search is sufficiently complete and measurable.

Initial parallel work is expected to favour a conservative architecture that can be compared directly against the established single-threaded engine before experimenting with more aggressive approaches such as work stealing, split points, or other forms of dynamic tree parallelism.

## Project Status

SeedV6 is under active development.

### Implemented

- Compact bitboard board representation
- Packed board state
- FEN parsing
- Zobrist hashing
- PEXT sliding attacks
- Direct legal move generation
- Staged legal generation
- Specialized move application
- Perft validation and benchmarking
- Position history and draw adjudication
- UCI engine shell and legal move replay
- Managed asynchronous search lifecycle
- Search limits and cancellation
- Exact full-width search baseline
- Rich phase-aware static evaluation
- Static exchange evaluation
- Transposition table
- Staged move ordering
- Killer heuristic
- History heuristic
- Immutable worker-local search diagnostics
- Deterministic single-thread search benchmark corpus
- Independently gated selective search heuristics

### Current Search Development

- Check-aware quiescence search
- Alpha-beta / PVS integration
- Principal variation search infrastructure
- Iterative deepening and root reporting

### Later Development

- Search tuning
- Multi-core search
- Further low-level optimization
- Playing-strength testing

SeedV6 should still be considered a development engine rather than a finished competitive release.

## Repository Structure

The repository is organised around a small number of engine responsibilities.

Major areas include:

- `core` — board state, move generation, evaluation, SEE, and fundamental chess mechanics.
- `core.util` — bitboard, PEXT, FEN, Zobrist, and supporting primitive utilities.
- `search` — search algorithms and search infrastructure.
- `search.tt` — transposition-table support.
- `search.order` — staged move ordering and search heuristics.
- `search.manage` — search lifecycle, limits, cancellation, and time management.
- `uci` — UCI protocol and engine-session handling.
- `tools` / `perft` — correctness validation and performance tooling.

The exact package structure may continue to evolve as the search engine develops.

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

## Notes for Readers

SeedV6 is a work-in-progress chess engine and an engineering project.

Some code is intentionally low-level or unconventional by normal application-Java standards because it exists in paths that may execute millions or billions of times.

The repository is public so that the architecture, experiments, correctness work, performance work, and evolution of the search engine can be followed as the project develops.

Performance results in this README are observations from the stated development environment rather than universal performance guarantees.

## License

No reuse or redistribution rights should be assumed unless and until an explicit project license is added.
