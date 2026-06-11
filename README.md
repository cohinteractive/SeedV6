# SeedV6

SeedV6 is a Java chess engine project focused on building a fast, compact, and search-ready chess core from the ground up.

The main goal of this project is not just to produce a working chess engine, but to explore high-performance engine design in Java: bitboards, packed board state, low-allocation move generation, flattened search structures, staged move picking, transposition tables, and eventually concurrent search.

This repository is public so that the design, experiments, and performance work can be followed as the engine develops.

## Project Goals

SeedV6 is being built with a few clear priorities:

* Correct chess rules and reliable move generation.
* Fast single-threaded core operations.
* Minimal allocation in hot paths.
* Board and move representations that are simple for the JVM to optimize.
* A search architecture that can later support concurrent execution.
* Clear separation between core engine logic, search logic, UCI handling, and test/perft tooling.

The project currently prioritizes correctness and mechanical performance over readability. Some code is intentionally written in a compact or low-level style where it improves hot-path behaviour.

## Current Focus

The current development focus is the engine core:

* Board representation.
* Move encoding.
* Move generation.
* Make-move logic.
* FEN parsing.
* Zobrist hashing.
* Magic bitboard sliding attacks.
* Perft validation.
* Early evaluation and search preparation.

Perft is used heavily to validate correctness. Passing a broad set of perft positions is the main confidence check for the board and move generation code.

## Engine Design

SeedV6 uses a compact bitboard-based board representation.

The board is represented using primitive `long` values rather than object-heavy structures. Piece type and colour information are encoded across multiple bitplanes, with additional packed state used for side to move, castling rights, en-passant square, move counters, and Zobrist hash data.

This style is chosen to reduce allocations, improve cache behaviour, and keep move generation and search-friendly operations as close to primitive operations as possible.

The engine favours reusable buffers and preallocated per-ply storage rather than creating new objects at each node. This is especially important for future search work, where millions of nodes may be visited per second.

## Move Generation

Move generation is built around bitboards and precomputed attack tables.

Sliding piece attacks use magic bitboards. Knight, king, pawn, ray, file, rank, and other helper tables are generated or stored in utility classes.

The generator is being shaped to support staged move picking for search. Instead of treating move generation as only “generate every legal move”, the long-term direction is to support generation stages such as:

* tactical moves,
* quiet moves,
* evasions,
* ordered captures,
* search-specific move picking.

This is important because a strong search does not want to blindly generate and sort every move at every node when only a small number of moves may need to be searched before a cutoff.

## Perft

Perft is used to validate move generation correctness.

The project includes perft tooling for running known chess test positions at fixed depths and comparing the resulting node counts against expected values. This helps verify:

* normal legal moves,
* castling,
* en-passant,
* promotion,
* checks,
* pins,
* discovered checks,
* move legality,
* make-move correctness,
* board state restoration through generated positions.

Both single-threaded and concurrent perft experiments are part of the development process. Perft performance is also used as a rough benchmark for the speed of the core move generation and make-move pipeline.

## Search Direction

Search is planned as a custom implementation rather than a direct copy of an existing engine.

The intended direction includes:

* iterative deepening,
* alpha-beta / negamax style search,
* transposition table support,
* staged move ordering,
* quiescence search,
* null move pruning,
* late move reductions,
* history heuristics,
* killer or continuation move ideas,
* static exchange evaluation,
* time management,
* and eventually concurrent search.

The first search implementation is expected to be single-threaded, but structured in a way that does not block future concurrency work.

## Concurrency Direction

Concurrency is an important long-term goal for SeedV6.

The intended model is not just root-splitting or lazy SMP. The project is exploring how to structure search so that work can be split dynamically and idle worker threads can take useful work from active parts of the tree.

This requires careful design of:

* work units,
* per-thread state,
* split points,
* shared search state,
* transposition table access,
* stop conditions,
* and synchronization costs.

The core engine is being built with this future direction in mind.

## Performance Philosophy

SeedV6 is performance-oriented.

The code intentionally avoids unnecessary object allocation in hot paths. Where useful, it uses:

* primitive arrays,
* packed integers and longs,
* bit tricks,
* reusable buffers,
* precomputed tables,
* local primitive unloading,
* branch reduction,
* and low-level board transformations.

This means some implementation choices may look less conventional than typical application Java code. The aim is to give the JVM simple, predictable, optimizable code in the parts of the engine that run millions of times.

## Project Status

SeedV6 is under active development.

The engine is not yet a complete competitive chess engine. The core board and move-generation work is the current foundation. Search, evaluation, UCI behaviour, tuning, and engine-strength improvements are expected to evolve over time.

At this stage, the most important indicators of progress are:

* perft correctness,
* move generation speed,
* make-move speed,
* allocation reduction,
* and the ability to support increasingly search-oriented move generation.

## Repository Structure

The project is organised around a small number of engine areas:

* `core` — board, move generation, evaluation, and core engine logic.
* `core.util` — bitboards, magic tables, FEN, Zobrist, piece helpers, and supporting utilities.
* `tools` or `perft` — validation and benchmarking helpers.
* `search` — search implementations and related search structures.
* `uci` — eventual UCI protocol entry points.
* `concurrent` — future concurrency-related structures.

The exact structure may change as the engine develops.

## Requirements

SeedV6 is written in Java.

The current development environment uses:

* Java 21
* Gradle
* Windows development environment

Other platforms should work if the Java and Gradle versions are compatible, but the project is primarily developed and tested in the author’s local environment.

## Running

Build and test commands may vary as the project evolves, but typical Gradle usage is expected:

```
./gradlew build
```

On Windows:

```
.\gradlew.bat build
```

Perft and engine-specific tools may be run through their relevant main classes or Gradle tasks, depending on the current project setup.

## Notes For Readers

This repository is a work-in-progress engine project.

Some code may be experimental. Some classes may change shape as the engine moves from core validation into search, evaluation, and concurrency. Public visibility does not mean the project is currently packaged as a finished engine or library.

The code is shared mainly as a record of the design and implementation process behind a high-performance Java chess engine.

## License

License information should be added before relying on this project for reuse or redistribution.
