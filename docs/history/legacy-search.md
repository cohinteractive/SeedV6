# Historical WS search architecture

These descriptions document the superseded WS search, retained in `app/src/verification/java`. They are historical reference, not current engine specifications.
See [the current baseline](../architecture.md) and [search contract](../search/CHESS_SEARCH_CONTRACT.md).

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

`SearchBenchmark` runs the legacy iterative/alpha-beta reference path with one thread,
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

For BRN-2 checkpoint score distributions, perspective-correct move volatility and
bounded search stability (with optional NNUE reference), use
[`brn2Diagnostics`](../research/brn/BRN_DIAGNOSTICS.md). It emits reproducible JSON Lines without
opening a training writer or changing evaluator/search policy.
