# SR-017 staged-generation experiment - 2026-09-27

Starting commit: `5af439b`, clean worktree. Verified authoritative Contract **R010**
and Frontier **F005**, including SR-015/SR-016 ACCEPTED and SR-017 PENDING.
No applicable local AGENTS.md was found. Root VERSION_STATE.txt activates the
verified maintained finalizer; CODEXLOG_CURRENT.md is absent. Prior reports,
Contract and Frontier are unchanged.

## Outcome and closure assessment

**Recommend outcome 3: STAGED_LAZY FAVOURED.** Leaf-only staging independently
improves aggregate depth-5 time by 13.695% / 12.473% (TT off / on) with identical
trees. Full staging improves by 25.322% / 22.918%, including 7.681% / 6.920% fewer
nodes from explicitly deferred quiet-history sampling. At depth 6, full staging
improves by 21.040% / 22.072% while nodes change only -0.028% / +0.056%.
The fresh-JVM confirmation is tabulated below.

The preferred provisional architecture is complete evasions when checked;
tactical-first legal-existence detection otherwise; exact global hash validation;
deferred quiet generation when possible; accepted good/bad/material/history
precedence; primitive lazy selection; and quiet-history snapshot at generation
of that node's quiet phase. Checked nodes, no-tactical nodes and quiet-hash nodes
necessarily receive early/full generation. These are semantic requirements,
not a fitted size or position heuristic.

**The evidence is sufficient to close SR-017 research, subject to programme
acceptance.** The prior unit adequately considered current insertion, full
handcrafted hybrid sorting and lazy selection; this unit establishes leaf and
positive-depth staging separately. Staging wins despite existing repeated Gen
setup. No narrow aggregate loss or unresolved mechanical limitation justifies
another bounded experiment. A prepared-generation context is not required to
decide the architecture and was not implemented. This does not claim every
possible micro-optimization has been exhausted.

SR-017 remains **PENDING** in the unchanged Frontier. This report recommends
closure; it does not grant acceptance, update canon, or adopt experimental policy
into production. CONTROL remains the production/default ordering.

## Implementation and candidate boundary

Changed main sources: `search/exact/ExactSearch.java` and
`tools/search/ExactSearchHarness.java`. Added test sources:
`search/exact/StagedGenerationTest.java`, `StagedSearchTest.java`, and
`StagedGenerationDiagnostics.java`; extended the existing harness test and updated
the previous mechanics test's invalid-mode boundary. Added
`tools/search-staging-diagnostics.py`, this report and version finalization.
Java paths are under the existing `com/ohinteractive/seedv6` main/test trees.
Board, Gen, Sort, history-update rules and prior experiment reports are untouched.

```text
--ordering=see-material-quiet-history --mechanics=staging
--mechanics=full-lazy|leaf-staged-lazy|staged-lazy
```

`staging` rotates A/B/C. Individual selectors and comma lists work.
FULL_LAZY is an alias of the existing LAZY_SELECTION integer mode. The previous
`--mechanics=all` still means insertion/full-sort/lazy; no old ordering alias or
constructor default changes. New staged mechanics reject other ordering policies.

**A FULL_LAZY:** existing `Gen.genAll`, accepted ordering and immutable node-entry
key snapshot, then existing primitive next-best selection. The snapshot, SEE/
material routines, insertion routines and Sort implementation are unchanged.
Pre-edit depth-5 baseline runs were recorded, and later A/B scores, best moves,
PVs and nodes were checked against them. No baseline ranking optimization was made.

**B LEAF_STAGED_LAZY:** identical A behaviour at positive depth. At depth <= 0,
compute checkers. Checked nodes use complete `genEvasion`; non-check nodes use
`genTactical`. A nonempty tactical set proves nonterminal status. Only if that
set is empty, use `genQuiet` to distinguish a quiet-only position from stalemate.
After legal existence is established, retain existing draw and static-evaluation
precedence. No child is searched at these leaves. B is an identical-tree comparison.

**C STAGED_LAZY:** applies that entry procedure at every depth. A positive-depth
non-check node with tacticals can keep quiet generation pending. Resolve draw/
TT evidence as before. Lazily select the accepted good/bad tactical ordering;
if it does not cut off, append generated quiets and lazily select their history
order. An empty deferred quiet set safely ends the move loop. No-tactical nodes
generate quiets immediately because terminal adjudication requires them.

Checked nodes receive complete evasions before adjudication/TT/ordering. Existing
Gen APIs preserve the legal move identities and class-relative generated order.
No partial evasion scheme, prepared context, generator redesign or deferred
legality is introduced. Existing tactical and quiet calls each derive occupancy,
checkers and pins; Search's preliminary checker calculation is also a real cost.
The retained legacy picker was inspected as evidence, not used as architecture.

## Hash, history and terminal semantics

A tactical TT hint is validated by exact membership in the tactical set. A
quiet-shaped hint forces quiet generation before it can be used for Search or
a TT PV prefix. This includes invalid quiet-shaped hints; no shortcut assumes
legality. The total-key hash priority promotes only an exact generated match,
so hash remains globally first and unique. Missing/invalid root TT moves retain
the established fallback search behaviour. TT depth/generation applicability,
score/bound policy, storage and history-sensitive identity are unchanged.

The main history is a mutable flat owner table with no existing cheap versioned
read, rollback or persistent snapshot. The old reusable keys represent already
generated moves, not arbitrary future quiets. Preserving node-entry history while
genuinely deferring quiet generation would require extra machinery or early quiet
generation. Neither was added.

C therefore **deliberately snapshots quiet history when quiets are generated**.
For a deferred phase this is after tactical siblings and can change visitation.
When terminal detection, check/evasion handling or a quiet hash requires early
generation, history is still sampled before the first child. Once sampled, keys
remain immutable across remaining siblings. Good and bad tactical evidence,
material values, quiet history identity/gravity updates and stable equal-evidence
ties are unchanged. Subset ordinals preserve the original within-class relative
order; absolute inter-class ordinal gaps have no ranking meaning.

C is not claimed to be a pure identical-tree experiment. It preserves exact
fixed-depth values; any changed best move must be legal and realize the same
value. In the measured corpus best moves and full PVs happened to match A/B in
all runs, despite some changed node counts. Independent value/PV validation does
not rely on that observation.

Cancellation is checked before node admission. Mate/stalemate still precede
claimable rule draws and the nominal static horizon; nonterminal draws precede
evaluation/TT; static leaves resolve before Search-TT probing; positive-depth
TT evidence precedes child Search. An empty tactical subset alone never means
terminal. Aborted work keeps the existing incomplete result and history clearing.

## Storage and hot-path inspection

All three lazy candidates retain the same owner storage: existing per-ply
`long[512]` move arrays, **512 KiB** flat per-ply ordering-key payload and **256 KiB**
main quiet history, plus existing boards/PVs and shared scratch. C uses the existing
**4 KiB `quietScratch`** as the Gen quiet output buffer and copies into the current
ply before recursion. No additional candidate array payload or per-node heap
allocation is added; phase state is primitive local state. No collections,
streams, boxing, Comparator or strategy-object dispatch enter the new hot path.

Source and OpenJDK 21 `javap -p -c` inspection verified primitive checker/append
helpers and no new normal-path allocation in negamax. Existing evaluator/cancel
interfaces, request/result allocation and exceptional invalid-score allocation
remain. Production bytecode has no diagnostic references. No allocation profile,
hardware counters, cache advantage or JIT explanation is claimed.

## Correctness and diagnostics method

The initial targeted run passed **84 tests**, zero failures/errors/skips. The
staged-search class was rerun after adding an invalid quiet-shaped hash case.
The selected scope was staged generation/Search, existing mechanics/order,
ExactSearch semantic/oracle, TT and harness tests, including related driver/legacy
exact TT tests matched by the filters. No full suite was run.

```powershell
.\gradlew.bat :app:test --tests '*StagedGenerationTest' --tests '*StagedSearchTest' --tests '*ExactSearchMechanicsTest' --tests '*ExactSearchOrderingTest' --tests '*ExactSearchTest' --tests '*ExactSearchTTableTest' --tests '*TTableTest' --tests '*ExactSearchHarnessTest' -Pheadless --console=plain
```

- Before Search implementation, **982 positions / 60 checked positions** proved
  tactical + quiet is the exact disjoint legal `genAll` set and preserves each
  class's original generation order. Complete evasions preserve the same classes.
  Fixtures include ordinary/high branching, 99/110 moves, captures, quiets, EP,
  capture/quiet promotions, underpromotions, castling, single/double check, mate
  and stalemate, extended by deterministic legal walks.
- Fully consumed depth-1 Search matches across A/B/C with fixed nonzero history,
  for every legal hash in those fixtures, absent hints and invalid tactical/quiet
  hints. Exact complete move identity, uniqueness, order and hash precedence hold.
- A/B reproduce recorded depth-5 nodes and match score, best move, PV, visitation
  checksum and final learned history with TT off/on. C passes depth-0..2 exhaustive
  oracle tests and independent depth-5 best-move/PV value checks.
- A sibling-time mutation fixture proves C reads deferred quiet evidence later,
  while A/B retain node-entry order; a quiet hash forces C's early snapshot.
- Mate, stalemate, rule-50, insufficient material, repetition, TT EXACT/LOWER/UPPER,
  static-boundary TT exclusion, invalid hints, cancellation before/during/final
  child, root TT nonpublication, history clearing, reuse and interruption are covered.

UNTIMED instrumentation is generated under `build/` from the current ExactSearch
source, with checked substitutions for node entry/exit, evaluation, generator
calls and history updates. A separate classpath loads this diagnostic copy;
production source/classes and timed JVMs have no hooks/counter branches. The
checked-in script reproduces it after `:app:compileTestJava`:

```powershell
python tools/search-staging-diagnostics.py 5
python tools/search-staging-diagnostics.py 6
python tools/search-staging-diagnostics.py 5 --freeze-history
```

Instrumented static-leaf assertions prove tactical existence skips quiets,
no-tactical leaves generate quiets, checked leaves use evasions, and evaluation
never overrides mate/stalemate. Positive hash-cutoff fixtures prove tactical hash
cutoffs avoid quiets and quiet hash validation forces them early. Instrumented
node totals equal Search results; A/B traces match. Disabling history learning
only in the test copy makes **A/B/C trees, scores, best moves, PVs and visitation
checksums identical across the full depth-5 corpus with both TT settings**. This
controls for the only deliberately variable ordering evidence; it is neither a
new production policy nor a timed benchmark.

## Timing protocol

AMD Ryzen 5 5500, Windows 11 amd64, OpenJDK **21+35-2513**, HCE, one Search thread;
**-Xms256m -Xmx256m -Xbatch**, five warmups, seven measured repetitions, rotated
A/B/C order every round and median elapsed time. TT off, or separate cold **4 MiB**
tables cleared before each invocation. History resets inside every fixed-depth
invocation. Worker construction, parsing, TT clear and semantic verification are
outside timing. The instrumented diagnostic copy is never on the timed classpath.

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history --mechanics=staging --tt=off' --console=plain
```

Repeat with TT on. Depth-6 uses `--position=kiwipete,middlegame,evasion --depth=6`
in both TT settings, then one fresh-JVM repeat to check the smaller individual
leaf-only effects. Each repetition checks determinism; A/B require exact tree
identity, C requires equal score and independent legal/optimal best move/PV.
Aggregate throughput is total nodes divided by summed position medians. Percentage
changes use unrounded nanoseconds; printed sums/NPS use rounded medians.

A = FULL_LAZY; B = LEAF_STAGED_LAZY; C = STAGED_LAZY. Scores and best moves
are shared across all three candidates in these runs. A/B nodes are identical.
Each timing cell is **median ms / NPS**.

### Depth 5, TT off

| Position | Score / best | A/B nodes | C nodes | A ms / NPS | B ms / NPS | C ms / NPS |
|---|---|---:|---:|---:|---:|---:|
| start | 197 / e2e3 | 31,418 | 31,418 | 17.636 / 1,781,459 | 17.546 / 1,790,576 | 17.034 / 1,844,407 |
| kiwipete | 403 / d5e6 | 83,934 | 83,934 | 61.397 / 1,367,063 | 49.938 / 1,680,760 | 47.040 / 1,784,299 |
| tactical | 1177 / e4d5 | 4,861 | 4,861 | 1.921 / 2,531,111 | 2.151 / 2,259,458 | 1.982 / 2,452,325 |
| evasion | -81 / c4c5 | 20,741 | 20,832 | 14.678 / 1,413,105 | 12.946 / 1,602,079 | 11.497 / 1,811,919 |
| middlegame | 250 / c3d5 | 141,150 | 119,270 | 99.748 / 1,415,068 | 85.911 / 1,642,985 | 68.174 / 1,749,506 |
| transposition-pawns | 6 / e1d2 | 1,563 | 1,563 | 0.356 / 4,395,388 | 0.436 / 3,584,862 | 0.443 / 3,529,013 |
| **Sum** | | **283,667** | **261,878** | **195.736 / 1,449,233** | **168.928 / 1,679,218** | **146.170 / 1,791,599** |

### Depth 5, cold TT on

| Position | Score / best | A/B nodes | C nodes | A ms / NPS | B ms / NPS | C ms / NPS |
|---|---|---:|---:|---:|---:|---:|
| start | 197 / e2e3 | 27,911 | 27,911 | 16.830 / 1,658,447 | 16.767 / 1,664,638 | 16.704 / 1,670,917 |
| kiwipete | 403 / d5e6 | 65,515 | 65,481 | 49.555 / 1,322,055 | 41.793 / 1,567,625 | 38.681 / 1,692,829 |
| tactical | 1177 / e4d5 | 4,280 | 4,280 | 2.009 / 2,130,625 | 1.745 / 2,452,862 | 2.229 / 1,920,315 |
| evasion | -81 / c4c5 | 19,834 | 19,925 | 15.719 / 1,261,752 | 13.700 / 1,447,758 | 12.356 / 1,612,576 |
| middlegame | 250 / c3d5 | 120,175 | 103,563 | 89.020 / 1,349,976 | 77.448 / 1,551,696 | 63.316 / 1,635,658 |
| transposition-pawns | 6 / e1d2 | 1,532 | 1,532 | 0.402 / 3,807,157 | 0.439 / 3,487,366 | 0.479 / 3,201,002 |
| **Sum** | | **239,247** | **222,692** | **173.535 / 1,378,667** | **151.892 / 1,575,113** | **133.765 / 1,664,800** |

### Depth 6 confirmation, TT off

| Position | Score / best | A/B nodes | C nodes | A ms / NPS | B ms / NPS | C ms / NPS |
|---|---|---:|---:|---:|---:|---:|
| kiwipete | -220 / e2a6 | 219,294 | 219,294 | 157.732 / 1,390,294 | 142.452 / 1,539,420 | 115.776 / 1,894,129 |
| middlegame | -126 / e2e1 | 564,514 | 564,505 | 391.829 / 1,440,713 | 355.113 / 1,589,675 | 314.051 / 1,797,496 |
| evasion | -787 / d2d4 | 157,868 | 157,609 | 109.856 / 1,437,046 | 96.551 / 1,635,075 | 90.851 / 1,734,803 |
| **Sum** | | **941,676** | **941,408** | **659.417 / 1,428,043** | **594.116 / 1,585,004** | **520.678 / 1,808,043** |

### Depth 6 confirmation, cold TT on

| Position | Score / best | A/B nodes | C nodes | A ms / NPS | B ms / NPS | C ms / NPS |
|---|---|---:|---:|---:|---:|---:|
| kiwipete | -220 / e2a6 | 167,760 | 167,922 | 139.808 / 1,199,930 | 125.829 / 1,333,237 | 104.001 / 1,614,615 |
| middlegame | -126 / e2e1 | 482,640 | 483,069 | 368.142 / 1,311,017 | 327.714 / 1,472,748 | 294.203 / 1,641,956 |
| evasion | -787 / d2d4 | 99,849 | 99,681 | 88.453 / 1,128,841 | 85.343 / 1,169,970 | 66.558 / 1,497,653 |
| **Sum** | | **750,249** | **750,672** | **596.403 / 1,257,956** | **538.886 / 1,392,222** | **464.762 / 1,615,175** |

### Depth 6 fresh-JVM repeat, TT off

| Position | Score / best | A/B nodes | C nodes | A ms / NPS | B ms / NPS | C ms / NPS |
|---|---|---:|---:|---:|---:|---:|
| kiwipete | -220 / e2a6 | 219,294 | 219,294 | 159.742 / 1,372,799 | 144.640 / 1,516,140 | 117.167 / 1,871,631 |
| middlegame | -126 / e2e1 | 564,514 | 564,505 | 387.100 / 1,458,314 | 343.836 / 1,641,812 | 312.346 / 1,807,307 |
| evasion | -787 / d2d4 | 157,868 | 157,609 | 109.645 / 1,439,807 | 95.632 / 1,650,781 | 91.641 / 1,719,861 |
| **Sum** | | **941,676** | **941,408** | **656.487 / 1,434,417** | **584.108 / 1,612,161** | **521.154 / 1,806,391** |

### Depth 6 fresh-JVM repeat, cold TT on

| Position | Score / best | A/B nodes | C nodes | A ms / NPS | B ms / NPS | C ms / NPS |
|---|---|---:|---:|---:|---:|---:|
| kiwipete | -220 / e2a6 | 167,760 | 167,922 | 137.754 / 1,217,827 | 125.348 / 1,338,354 | 104.120 / 1,612,773 |
| middlegame | -126 / e2e1 | 482,640 | 483,069 | 495.270 / 974,499 | 440.312 / 1,096,131 | 396.639 / 1,217,905 |
| evasion | -787 / d2d4 | 99,849 | 99,681 | 76.296 / 1,308,712 | 69.446 / 1,437,799 | 60.999 / 1,634,138 |
| **Sum** | | **750,249** | **750,672** | **709.320 / 1,057,702** | **635.106 / 1,181,297** | **561.758 / 1,336,291** |

### Aggregate effects

Negative wall-time change is faster. A to B has identical nodes in every run.
C node change is the same versus either A or B.

| Corpus / TT | A to B wall | A to B throughput | C nodes | B to C wall | B to C throughput | A to C wall | A to C throughput |
|---|---:|---:|---:|---:|---:|---:|---:|
| Depth 5, TT off | -13.695% | +15.868% | -7.681% | -13.472% | +6.693% | -25.322% | +23.623% |
| Depth 5, cold TT on | -12.473% | +14.250% | -6.920% | -11.934% | +5.694% | -22.918% | +20.755% |
| Depth 6 confirmation, TT off | -9.903% | +10.991% | -0.028% | -12.361% | +14.072% | -21.040% | +26.610% |
| Depth 6 confirmation, cold TT on | -9.644% | +10.673% | +0.056% | -13.755% | +16.014% | -22.072% | +28.396% |
| Depth 6 fresh-JVM repeat, TT off | -11.025% | +12.392% | -0.028% | -10.778% | +12.048% | -20.615% | +25.932% |
| Depth 6 fresh-JVM repeat, cold TT on | -10.463% | +11.685% | +0.056% | -11.549% | +13.121% | -20.803% | +26.339% |

Absolute medians vary between JVM runs (especially Middlegame), so these are local
measurements, not portable performance guarantees. The rotated within-run
comparisons nevertheless repeat the same aggregate result. All three depth-6
positions favour C over B and A, with both TT settings in both JVM runs.

## Untimed generation evidence

Aggregate counters below are from separate instrumented executions, not timed
runs. `depth_zero_nodes` includes terminal/draw nodes; `static_evaluations` counts
only nonterminal evaluations. All generated leaf moves count as unsearched;
positive-depth unused moves are also shown separately. A checked-node count is
not a `genEvasion` call count for A, which still calls `genAll`. Zero staging
counters for A indicate that it has no staged path, not zero legal tacticals.

### Depth 5 diagnostics

| Metric | Off A | Off B | Off C | On A | On B | On C |
|---|---:|---:|---:|---:|---:|---:|
| Entered nodes | 283,667 | 283,667 | 261,878 | 239,247 | 239,247 | 222,692 |
| Depth <= 0 nodes | 246,650 | 246,650 | 225,173 | 202,454 | 202,454 | 186,213 |
| Static evaluations | 246,402 | 246,402 | 224,925 | 202,207 | 202,207 | 185,966 |
| Checked nodes | 7,539 | 7,539 | 7,628 | 6,745 | 6,745 | 6,977 |
| Positive-depth nodes | 37,017 | 37,017 | 36,705 | 36,793 | 36,793 | 36,479 |
| Checked positive-depth nodes | 943 | 943 | 1,401 | 939 | 939 | 1,397 |
| genAll calls | 283,667 | 37,017 | 0 | 239,247 | 36,793 | 0 |
| genTactical calls | 0 | 240,054 | 254,250 | 0 | 196,648 | 215,715 |
| genQuiet calls | 0 | 14,002 | 22,321 | 0 | 12,519 | 19,766 |
| genEvasion calls | 0 | 6,596 | 7,628 | 0 | 5,806 | 6,977 |
| Leaf quiet phase avoided | 0 | 226,052 | 204,950 | 0 | 184,129 | 168,120 |
| Positive hash/tactical cutoff avoids quiets | 0 | 0 | 26,979 | 0 | 0 | 26,325 |
| Quiet hash forces early quiet phase | 0 | 0 | 0 | 0 | 0 | 2 |
| No tacticals: quiets required for terminal status | 0 | 14,002 | 16,552 | 0 | 12,519 | 15,002 |
| Of those, positive-depth nodes | 0 | 0 | 2,556 | 0 | 0 | 2,489 |
| Both tactical and quiet generators | 0 | 14,002 | 22,321 | 0 | 12,519 | 19,766 |
| Of those, positive-depth nodes | 0 | 0 | 8,325 | 0 | 0 | 7,253 |
| Total legal moves generated | 11,577,973 | 3,232,017 | 2,066,681 | 9,679,671 | 2,913,747 | 1,755,519 |
| Generated but unsearched | 11,294,312 | 2,948,356 | 1,804,809 | 9,440,430 | 2,674,506 | 1,532,833 |
| Of those, at positive depth | 1,172,133 | 1,172,133 | 149,891 | 1,209,416 | 1,209,416 | 157,494 |
| Quiet history sampled after tactical children | 0 | 0 | 5,769 | 0 | 0 | 4,762 |
| Positive TT/draw return without quiets/children | 0 | 0 | 0 | 0 | 0 | 1,504 |

### Depth 6 diagnostics

| Metric | Off A | Off B | Off C | On A | On B | On C |
|---|---:|---:|---:|---:|---:|---:|
| Entered nodes | 941,676 | 941,676 | 941,408 | 750,249 | 750,249 | 750,672 |
| Depth <= 0 nodes | 711,060 | 711,060 | 710,833 | 552,368 | 552,368 | 552,335 |
| Static evaluations | 710,561 | 710,561 | 710,334 | 551,924 | 551,924 | 551,891 |
| Checked nodes | 29,541 | 29,541 | 29,536 | 24,198 | 24,198 | 24,177 |
| Positive-depth nodes | 230,616 | 230,616 | 230,575 | 197,881 | 197,881 | 198,337 |
| Checked positive-depth nodes | 4,568 | 4,568 | 4,568 | 4,091 | 4,091 | 4,112 |
| genAll calls | 941,676 | 230,616 | 0 | 750,249 | 197,881 | 0 |
| genTactical calls | 0 | 686,087 | 911,872 | 0 | 532,261 | 726,495 |
| genQuiet calls | 0 | 2,151 | 22,456 | 0 | 1,195 | 17,179 |
| genEvasion calls | 0 | 24,973 | 29,536 | 0 | 20,107 | 24,177 |
| Leaf quiet phase avoided | 0 | 683,936 | 683,719 | 0 | 531,066 | 531,077 |
| Positive hash/tactical cutoff avoids quiets | 0 | 0 | 205,697 | 0 | 0 | 165,994 |
| Quiet hash forces early quiet phase | 0 | 0 | 0 | 0 | 0 | 47 |
| No tacticals: quiets required for terminal status | 0 | 2,151 | 2,199 | 0 | 1,195 | 1,246 |
| Of those, positive-depth nodes | 0 | 0 | 53 | 0 | 0 | 53 |
| Both tactical and quiet generators | 0 | 2,151 | 22,456 | 0 | 1,195 | 17,179 |
| Of those, positive-depth nodes | 0 | 0 | 20,310 | 0 | 0 | 15,986 |
| Total legal moves generated | 38,412,108 | 13,851,364 | 5,811,231 | 30,731,720 | 11,580,330 | 4,681,183 |
| Generated but unsearched | 37,470,435 | 12,909,691 | 4,869,826 | 29,981,474 | 10,830,084 | 3,930,514 |
| Of those, at positive depth | 9,275,482 | 9,275,482 | 1,236,306 | 7,978,276 | 7,978,276 | 1,078,908 |
| Quiet history sampled after tactical children | 0 | 0 | 20,257 | 0 | 0 | 15,886 |
| Positive TT/draw return without quiets/children | 0 | 0 | 0 | 0 | 0 | 12,245 |

## Interpretation and limits

- **Pure leaf effect (A to B):** quiet generation is avoided at 91.65% / 90.95%
  of depth-5 horizon nodes and 96.19% / 96.14% at depth 6 (off / on). Total generated
  moves fall 72.08% / 69.90% at depth 5 without changing a single searched node.
  The measured wall-time benefit is 12-14% at depth 5 and roughly 10-11% at depth 6.
- **Positive-depth opportunity (B to C):** hash/tactical cutoffs avoid quiets at
  73.50% / 72.16% of positive-depth nodes at depth 5, rising to 89.21% / 83.69% at
  depth 6. Positive-depth generated-but-unsearched moves fall about 87% versus B.
  At depth 6 the tree is almost unchanged in size, yet C saves another 11-14%
  elapsed time versus B across the original and fresh-JVM runs.
- **Costs retained:** C uses complete evasions at about 3% of entered nodes;
  those nodes do not gain deferred quiet generation. Quiet hashes force an early
  quiet phase only 2 times at depth 5 and 47 at depth 6 with cold TT on. This is
  a cold fixed-depth observation, not a prediction for iterative or warm-TT use.
  Both subset generators run at 22.68% / 19.88% of C's positive-depth nodes at
  depth 5 and 8.81% / 8.06% at depth 6. At leaf nodes the second call is needed
  only when the tactical set is empty. These counts expose repeated setup; they
  do not isolate its elapsed cost. There is no evidence of a narrow overall loss
  requiring a prepared-generation context to resolve this experiment.
- **History/tree effect:** the depth-5 reduction comes principally from
  Middlegame; Evasion adds 91 nodes in both TT settings. Depth-6 changes have
  mixed signs and are negligible in aggregate. Later sampling is therefore not
  a universally better ordering policy. The observed C wall-time result combines
  generation savings, changed ordering work and changed visitation; its depth-5
  gain must not all be attributed to generation. Frozen-history diagnostic traces
  support that deferred history evidence is the source of the tree differences.
- **Work saved:** C generates about 82% fewer legal moves than A at depth 5 and
  85% fewer at depth 6. Avoiding unused quiets outweighs the new branches,
  preliminary check detection and repeated Gen setup on the representative
  aggregate. Small positions do not uniformly improve: the pawn fixture is
  slower by about 0.08 ms, and Tactical is mixed. These results do not justify
  inventing a size/position heuristic. No playing-strength inference follows.

The remaining limits are the six-position corpus, one HCE host/JVM configuration,
cold fixed-depth TT lifecycle and explicitly changed history timing. Absolute
timing varied on the fresh run; no hardware-counter or JIT causality claim is
made. Exhaustive edge-position oracles, independent best/PV checks and identical
frozen-history traces establish the tested semantic boundary, not a proof for
every possible chess position or future evaluator.

Deliberately skipped: full/long-running suite, self-play/strength matches,
GUI/neural campaigns, deeper/larger timing campaigns, allocation profiling,
handcrafted-sort retuning and Gen redesign/prepared contexts. No PVS, qsearch,
selectivity, production adoption or Contract/Frontier maintenance was performed.
No further experiment or human action is required to complete this unit; formal
research acceptance/closure remains a later programme decision.

Local raw outputs are retained under ignored `build/sr017-staged-*.txt`, including
the pre-edit baseline, targeted validation, six timing runs, normal depth-5/6
diagnostics, frozen-history diagnostic and bytecode inspection. The tables above
are the durable evidence; the checked-in harness and diagnostic script reproduce
the measurements without requiring those local output files.
