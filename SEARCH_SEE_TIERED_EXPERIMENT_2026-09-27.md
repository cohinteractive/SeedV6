# ExactSearch SEE tier experiment — 2026-09-27

This is bounded experimental evidence for SR-014/SR-015, not a research disposition or production adoption. Starting revision: `15223c8f9568e973e50786c378c9bddcdedcc857` (clean worktree). CONTROL remains the default. The Search contract and frontier are unchanged.

## Implementation and selection

`ExactSearch.CONTROL` retains stable tactical-before-quiet ordering followed by legal hash promotion. `ExactSearch.SEE_TIERED` orders legal hash, remaining SEE-good tactical moves, quiets, then SEE-bad tactical moves. Each remaining class preserves generator order. Tactical means capture, en passant or promotion, using the existing classification. The threshold is exactly zero via `See.atLeastGeneratedLegal`.

The three-argument `ExactSearch(ExactEvaluator, TTable, int)` selects the mode once per owner. Established constructors use CONTROL. The headless harness accepts `--ordering=control|see-tiered|both`; the default is control. No production coordinator selects the candidate.

Full `Gen.genAll` generation remains unchanged. Classification occurs only after terminal/draw adjudication, the static boundary and applicable TT score cutoffs. Exact legal-list membership identifies the hash hint, bypasses its unnecessary SEE call and retains it once; existing stable promotion places it first. Other good moves compact into the list; quiets use existing reusable scratch; bad tacticals use one additional owner-held `long[512]` (4 KiB payload), allocated only for SEE_TIERED. Two array copies finish the partition before recursion. One mode branch per searched positive-depth node; no strategy dispatch, per-node ordering allocation, boxing or sorting. Existing request/result allocation is unchanged. Source and compiled-bytecode inspection confirmed no allocation instructions in the partition; no allocation profiler was run.

No tactical scoring, history, sorting, staged generation, pruning, reductions, PVS, qsearch, SEE implementation or TT-policy changes were made.

## Method

- Windows 11 amd64; AMD Ryzen 5 5500, 6 cores/12 logical processors.
- OpenJDK 21, build `21+35-2513`, 64-Bit Server VM.
- Existing Gradle `:app:exactSearch` task, HCE evaluator, one Search thread, `-Xms256m -Xmx256m -Xbatch`.
- All reported positions use depth **5**, five warmups and seven measured repetitions **per mode per position**. Depth 5 was retained after a one-warmup/one-repetition TT-off pilot already exposed substantial effects. Pilot timings are excluded below.
- One JVM per TT configuration; runs were sequential. Both modes reuse separate Search/evaluator owners and alternate execution order each round. TT-on uses a separate explicit 4 MiB table per mode, cleared before every request, outside Search timing. No warm-table carryover.
- Statistic: median of seven Search elapsed times, including existing request setup but excluding owner construction, FEN parsing and table clear. NPS is nodes divided by that sample's elapsed time. No instrumentation counters were added.
- Every warmup/measured round checks equal scores across modes. Each mode must repeat its nodes, score, best move and PV exactly. After timing, legal PV replay and TT-off CONTROL re-searches of each best child/PV endpoint check realized root value, including mate-distance adjustment.
- Across the two configurations: 168 measured searches and 120 warmup searches. All completed, with identical scores, best moves and PVs across modes and TT settings. No equal-valued best-move differences occurred.

Commands (PowerShell, repository root):

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=1 --repetitions=1 --ordering=both --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=both --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=both --tt=on' --console=plain
```

All three commands completed successfully (Gradle reported 9 s, 41 s and 41 s respectively).

## Fixed position set

`ExactSearchHarness.orderingPositions()` supplies these fixtures. The original six-position harness/oracle set is preserved.

| Name / purpose | Repository source | FEN |
| --- | --- | --- |
| start / quiet opening | DefaultPerftPositionLibrary initial-position | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` |
| kiwipete / high branching | DefaultPerftPositionLibrary kiwipete | `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1` |
| tactical / material exchanges | retained SearchBenchmark fixture | `4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1` |
| evasion / checked root, promotions | DefaultPerftPositionLibrary promotion-and-castling-tactics | `r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1` |
| middlegame / ordinary complex middlegame | DefaultPerftPositionLibrary complex-middlegame | `r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10` |
| transposition-pawns / reversible paths and pawn resets | ExactSearchTTableTest fixture | `4k3/pp6/8/8/8/8/PP6/4K3 w - - 0 1` |

## TT off

All rows: depth 5. C = CONTROL; S = SEE_TIERED. Score/best columns apply to both modes.

| Position | Score | Best | C nodes | S nodes | C median ms | S median ms | C NPS | S NPS | Nodes change | Wall change |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 197 | e2e3 | 48,266 | 103,653 | 24.051 | 52.288 | 2,006,827 | 1,982,359 | +114.754% | +117.404% |
| kiwipete | 403 | d5e6 | 147,864 | 95,333 | 91.389 | 63.137 | 1,617,961 | 1,509,950 | -35.527% | -30.915% |
| tactical | 1177 | e4d5 | 6,103 | 6,096 | 1.568 | 1.678 | 3,891,474 | 3,632,246 | -0.115% | +7.014% |
| evasion | -81 | c4c5 | 43,223 | 22,255 | 27.920 | 14.764 | 1,548,123 | 1,507,362 | -48.511% | -47.119% |
| middlegame | 250 | c3d5 | 227,299 | 4,418,180 | 134.183 | 2,759.925 | 1,693,954 | 1,600,833 | +1843.774% | +1956.844% |
| transposition-pawns | 6 | e1d2 | 2,867 | 2,867 | 0.603 | 0.585 | 4,757,716 | 4,901,692 | 0% | -2.937% |

## TT on

All rows: depth 5, cold 4 MiB table. Score/best columns apply to both modes.

| Position | Score | Best | C nodes | S nodes | C median ms | S median ms | C NPS | S NPS | Nodes change | Wall change |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 197 | e2e3 | 43,779 | 95,495 | 25.069 | 53.433 | 1,746,326 | 1,787,191 | +118.130% | +113.142% |
| kiwipete | 403 | d5e6 | 128,832 | 76,685 | 84.528 | 53.991 | 1,524,132 | 1,420,337 | -40.477% | -36.127% |
| tactical | 1177 | e4d5 | 4,648 | 5,621 | 1.291 | 1.638 | 3,601,146 | 3,430,995 | +20.934% | +26.931% |
| evasion | -81 | c4c5 | 40,524 | 19,948 | 27.665 | 14.056 | 1,464,811 | 1,419,160 | -50.775% | -49.191% |
| middlegame | 250 | c3d5 | 189,509 | 4,266,197 | 120.185 | 2,818.682 | 1,576,808 | 1,513,543 | +2151.184% | +2245.282% |
| transposition-pawns | 6 | e1d2 | 2,788 | 2,788 | 0.610 | 0.612 | 4,572,740 | 4,554,811 | 0% | +0.394% |

## Aggregate and interpretation

Aggregates sum node counts and position medians at the same depth. Throughput is total nodes divided by summed median time; it is not an average of per-position NPS. Millisecond totals below sum the displayed rounded medians; percentage changes are the harness's unrounded calculations.

| TT | C total nodes | S total nodes | C summed ms | S summed ms | Nodes change | Wall change | Throughput change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| off | 475,622 | 4,648,384 | 279.714 | 2,892.377 | +877.327% | +934.051% | -5.486% |
| on | 410,080 | 4,466,734 | 259.348 | 2,942.412 | +989.235% | +1034.542% | -3.993% |

Evidence is **mixed by position and strongly unfavourable overall** for this isolated precedence policy. Kiwipete and evasion improve in both TT configurations. Start and especially middlegame regress in both. The tactical fixture with TT off saves seven nodes but loses wall time; TT on increases both nodes and time. Transposition-pawns has identical node counts between modes and sub-millisecond timings, so its small timing differences should not drive a decision.

Most per-position NPS values decrease, and aggregate throughput decreases in both configurations. These measurements include both SEE classification cost and a changed mix of visited nodes; they do not isolate SEE's unit cost. Lower nodes alone do not establish faster Search. The large middlegame regression dominates the aggregate, explicitly visible above. This experiment does not justify adopting SEE_TIERED as the simple tactical-ordering baseline. CONTROL remains default; no SR-014/SR-015 acceptance or implementation disposition is made.

Limits: one host, one fixed depth, six positions, HCE static leaves without qsearch, cold 4 MiB TT and no playing-strength test. No statistical confidence interval, allocation/branch/JIT profiling or cross-machine replication was attempted. These results do not settle other depths, evaluators, TT sizes or future combinations with other ordering mechanisms.

## Validation

```powershell
.\gradlew.bat :app:test --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' -Pheadless=true --console=plain
git diff --check
```

Final targeted run: **64 tests in 9 suites, zero failures/errors/skips; BUILD SUCCESSFUL in 4 s**. Main/test compilation and module packaging tasks succeeded. The first run had one test-helper failure: a constant evaluator was incorrectly assumed to force a zero root score on a mate-in-one fixture. The visitation helper now checks completion; independent score tests remain intact. No Search score defect or move-set failure was found.

New `ExactSearchOrderingTest` covers:

- Explicit CONTROL versus established defaults, including exact node counts, scores, best moves and PVs, with TT off/on.
- 168 comparisons against the existing exhaustive oracle: 14 fixtures, depths 0–2, both orderings and both TT modes; legal optimal best moves, consistent PVs and input-board preservation.
- Actual root visitation order, complete move-set identity and uniqueness, repeatability, captures, losing captures, quiets, en passant, capture/quiet promotions and underpromotions, checked evasions and a high-mobility fixture. Visitation checks remain separate from Search-value comparisons.
- Every legal root hash hint in Kiwipete, en-passant and promotion fixtures, covering good/bad/quiet hints, depth mismatch, old generations and invalid hints.
- Partition bypass at static leaves, draw/terminal nodes and exact/lower/upper TT score cutoffs, using scratch sentinels plus inspection of the sole SEE call site.
- Repetition, cancellation during search/at the last leaf, no completed result or root TT publication on abort, and reusable owner state afterward.

Existing exact TT/oracle, SearchDriver/production-boundary and independent threshold-SEE oracle tests passed. Harness tests exercise both TT modes and paired selection. Full repository suites, long perft/generated/exhaustive campaigns, GUI tests and further benchmark programmes were deliberately not run: this unit changes only the selectable ordering partition and its headless experimental surface.
