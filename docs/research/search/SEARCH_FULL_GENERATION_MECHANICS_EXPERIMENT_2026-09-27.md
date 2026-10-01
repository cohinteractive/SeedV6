# SR-017 full-generation mechanics experiment - 2026-09-27

Starting revision: `eeba4e908b65b4a3166a2b50156ceb4872f74e77`, clean worktree.
Verified authoritative Search Contract **R010** and Research Frontier **F005**.
No applicable repository-local AGENTS.md was present in the root, parents or
repository subtree. The supplied governance applies. Root VERSION_STATE.txt
activates the verified maintained finalizer; CODEXLOG_CURRENT.md is absent and
is not created. No pre-existing working changes were absorbed.

## Research outcome and next boundary

**Recommend outcome 3: LAZY_SELECTION FAVOURED**, provisionally, for the accepted
SR-015/SR-016 policy with full legal generation. It essentially ties current
insertion at depth 5, then improves aggregate depth-6 time by 4.897% / 6.865%
(TT off / on). A fresh-JVM repeat improves by 6.176% / 5.939%. All mechanics have
identical scores, best moves, PVs and nodes within each configuration.

This is sufficient to use lazy selection as the provisional full-generation
baseline for the next comparison against staged tactical/quiet generation and
staged/hybrid generation with handcrafted sorting inside classes. Keep current
insertion as the independent deterministic reference. No size-dependent rule is
proposed: the evidence does not establish a crossover for choosing mechanics at
individual nodes. The small depth-5 aggregate differences are not decisive alone.

The tested handcrafted total-key hybrid does not earn its added complexity.
None of 8/16/24/32 beats insertion-only full-key sorting consistently across the
two depth-5 TT settings; even that insertion-only variant loses to current
partitioned insertion in aggregate. This does not reject every possible
class-local handcrafted sorter, especially under a later staged generator.

This is a completed research unit, not programme acceptance or production
adoption. **SR-017 stays PENDING; Contract/Frontier remain R010/F005 unchanged.**
Full `Gen.genAll` remains in every node. No staged generation, deferred legality,
Board/Gen redesign, self-play, strength inference, PVS or other Search feature
is introduced. No staged-generation performance is inferred.

## Implementation and selector

- `search/exact/ExactSearch.java`: integer mechanics selector, experimental
  constructor overloads, immutable per-node ordering keys and lazy consumption.
- `search/exact/Sort.java`: dedicated primitive full sort and next-best selection.
- `tools/search/ExactSearchHarness.java`: orthogonal mechanics/crossover options,
  rotated comparisons and strict cross-mechanic semantic checks outside timing.
- `search/exact/SortTest.java` and `ExactSearchMechanicsTest.java`: primitive,
  complete sequence, Search-tree, lifecycle, draw, TT and cancellation coverage.
- `search/exact/MechanicsDiagnostics.java` in test sources: explicitly untimed
  callback-based diagnostics, with no production instrumentation.
- `tools/search/ExactSearchHarnessTest.java`: selector, comparison and crossover
  coverage, preserving existing mode/alias tests.
- This evidence file and required version finalization.

Paths above are relative to `app/src/main/java/com/ohinteractive/seedv6/` or the
corresponding `app/src/test/java/` tree as appropriate.

The three-argument experimental constructor still selects CURRENT_INSERTION.
All previously established ordering modes/aliases retain their meaning; ordinary
constructors still select CONTROL. The accepted history policy is experimental.
The new four/five-argument constructors accept integer mechanics and optional
crossover; alternate mechanics reject policies other than
SEE_MATERIAL_QUIET_HISTORY. No strategy objects/interface dispatch are added.

```text
--ordering=see-material-quiet-history --mechanics=all
--mechanics=current-insertion|handcrafted-sort|lazy-selection
--mechanics=current-insertion,handcrafted-sort --sort-crossovers=8,16,24,32,512
```

Comma-separated mechanics are supported. `all` means A/B/C, with B's experimental
default crossover 24. That value is retained as a reproducible test setting,
not claimed as an empirical winner. Numeric constructor bounds are 2..512.
The harness labels mechanics in its existing `ordering=` output column for
these explicitly selected runs. Ordinary ordering comparisons are unchanged.

## Mechanics, evidence and storage

**A CURRENT_INSERTION:** unchanged stable classification, separate tactical
insertion passes, quiet insertion, range copies and legal hash promotion.
`orderTacticalFirst`, `orderSeeClassified`, `promoteHashMove`, `rankTacticals`
and `rankQuiets` method bodies were compared with starting HEAD and are unchanged.
Pre-edit depth-5 runs were recorded before implementation; saved score, best
move, full PV and node results match every later candidate and crossover run.
No shared refactor of insertion was needed.

**B HANDCRAFTED_FULL_SORT:** one primitive total-key snapshot over the generated
list, then in-place descending median-of-three quicksort with insertion leaves.
Recurse only on the smaller partition and iterate the larger partition, bounding
stack depth logarithmically. Moves and keys swap together. This uses the complete
list rather than separately sorting class ranges, avoiding additional partition
buffers and sharing evidence capture with C. Physical stability is unnecessary.

**C LAZY_SELECTION:** snapshot every key before the first child; each requested
next move scans the remaining keys and swaps the maximum into the next prefix
slot. Never reread history between siblings. The searched prefix remains intact
for the unchanged history reward/malus logic. Hash is selected once through its
dominant key; it is not separately generated or duplicated. An unconsumed tail
is neither fully sorted nor visited, although its generation and scoring still
occur. SEE/material/history classification is not deferred.

For a non-hash move the positive integer key is:

```text
(class << 25) | ((evidence + 16384) << 9) | (511 - generationOrdinal)
class: good tactical=2, bad tactical=1, quiet=0
evidence: immediate tactical material, or snapshotted quiet history
legal hash: Integer.MAX_VALUE - generationOrdinal
```

Ordinal is 0..511, matching the existing capacity. Quiet history is
[-16384,+16384]; tactical material is [0,1850]. Fields do not overlap or overflow.
Class dominates evidence; evidence dominates ordinal. Higher keys lead. Thus
hash, SEE-good material descending, SEE-bad material descending, quiet history
descending; equal evidence preserves original generation order. Captures, EP,
capture/quiet promotions and actual underpromotion values use the existing
accepted evaluator-independent evidence routines. Hash skips SEE/material scoring
as in A. Invalid hints do not match a generated legal move and have no effect.

A and B/C evaluate the same evidence before recursion. A takes quiet evidence
during its insertion pass; B/C take it during the key pass. History cannot change
between those points, so the evidence is identical without rewriting A.

Payload footprints, excluding array/object headers and unchanged board/move/PV
storage: A retains two `long[512]` partition arrays (8 KiB total), one
`int[512]` score array (2 KiB), generator scratch and the 256 KiB main history.
B reuses the existing 2 KiB score array and adds **zero array payload**; existing
partition arrays remain allocated but unused by B. C adds one owner-allocated
`int[MAX_DEPTH * 512]` = **524,288 bytes / 512 KiB**, one 512-key slice for each
possible positive-depth ply. Existing scratch remains allocated. Unlike B, C
must preserve unconsumed evidence across recursion, so a single shared score
array would be unsafe. No per-node allocations are introduced.

Source and OpenJDK 21 `javap -p -c` inspection of `snapshotOrdering`, `Sort.full`
and `Sort.next` found primitive array operations/direct calls, no allocation,
boxing, collections, streams, Comparator or object dispatch. New array creation
is confined to owner construction. Existing request/result allocation and
exceptional error paths are unchanged. This is source/bytecode evidence, not a
runtime allocation profile or proof about cache misses, branch prediction or JIT
code quality; no such claims are made.

## Validation

**89 targeted tests passed, zero failures/errors/skips** on the final test run.
The initial new learned-state fixture (Kiwipete depth 4) produced no learned
history; its precondition assertion caught that. It was replaced with verified
starting-position depth-4 learning before timing. No mechanics sequence/node
mismatch occurred. The unchanged insertion implementation was not altered to
make a comparison pass.

```powershell
.\gradlew.bat :app:test --tests '*SortTest' --tests '*ExactSearchMechanicsTest' --tests '*ExactSearchOrderingTest' --tests '*QuietHistoryTest' --tests '*ExactSearchTest' --tests '*ExactSearchTTableTest' --tests '*TTableTest' --tests '*ExactSearchHarnessTest' -Pheadless --console=plain
```

The wildcard scope also exercised the related SearchDriverTTableTest,
OrderedExactSearchTest and TtExactSearchTest. New coverage includes:

- 7,440 full-sort sequence comparisons (five crossover values) plus 1,488 lazy
  comparisons; every legal hash plus absent/invalid hints, across zero, signed
  boundary/tied and real learned histories. Independent stable policy oracle
  uses numeric SEE and before/after board material accounting. Check exact set,
  uniqueness, sequence and original-generation tie order.
- Ordinary/high-branch positions; exact 99/110 fixtures; both SEE classes,
  equal-material ties, equal-history ties, quiets, EP for both colours,
  capture/quiet promotions and underpromotions, both-colour castling and evasions.
- Real root Search for every legal hash in representative tactical/special-move
  fixtures, including deliberate history mutation after each sibling, proves
  snapshot rather than dynamic-history semantics.
- Every primitive length 0..512, all tested thresholds, nonzero subrange/key
  offsets, sorted/reverse/alternating inputs, key/move association and untouched
  outer boundaries. Lazy selection preserves every already-emitted prefix.
- All six depth-5 positions, both TT configurations: recorded node counts,
  exact scores/best moves/PVs, deterministic child-visitation checksums and
  identical final learned histories. No equal-valued-best-move allowance.
- Exhaustive depth-0..2 oracle, terminal mate/stalemate, rule-50, insufficient
  material, repetition, TT EXACT/LOWER/UPPER cutoffs, warm same-request reuse,
  later requests, cancellation at several checkpoints and on the final child,
  cleared aborted history, no aborted root TT publication, owner reuse and
  preserved interruption semantics.
- All benchmark repetitions and crossovers require identical score, best move,
  PV and nodes across mechanics; repeatability and independent TT-off CONTROL
  best-child/PV value verification run outside timed regions.

## Benchmark methodology

AMD Ryzen 5 5500, Windows 11 amd64, OpenJDK **21+35-2513**, HCE, one Search thread,
**-Xms256m -Xmx256m -Xbatch** via the established Gradle JavaExec task. Five
warmups and seven measurements, rotated A/B/C order each round, fourth elapsed
sample as median. TT off; or independent cold 4 MiB TT cleared before each
invocation. Quiet history resets at every fixed-depth invocation, inside timing.
Worker construction, FEN parsing, TT clear and semantic checks are outside timing.
Diagnostic code is test-only and never active in timed runs.

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history --mechanics=all --tt=off' --console=plain
```

Repeat with `--tt=on`. Depth-6 confirmation uses
`--position=kiwipete,middlegame,evasion --depth=6` with both TT settings, then one
fresh-JVM repeat of each. Crossover screen uses
`--mechanics=current-insertion,handcrafted-sort --sort-crossovers=8,16,24,32,512`
at depth 5 for both TT settings, rotating all six variants. 512 is full-key
insertion-only, distinct from A's unchanged partitioned insertion.

Tables below use A=current insertion, B24=handcrafted hybrid at 24, C=lazy
selection. Scores, best moves and nodes are shared by all three within each row.
NPS is per-position nodes/median time. Aggregates use total nodes divided by
summed position medians, not averaged NPS; percentages use unrounded harness
nanoseconds. Printed total milliseconds/NPS may differ at the last digit through
rounding. These are end-to-end Search timings, not isolated ordering timings.

### Depth 5, TT off

| Position | Score / best | Nodes (all) | A ms / NPS | B24 ms / NPS | C ms / NPS |
| --- | --- | ---: | ---: | ---: | ---: |
| start | 197 / e2e3 | 31,418 | 18.996 / 1,653,909 | 19.827 / 1,584,638 | 18.886 / 1,663,604 |
| kiwipete | 403 / d5e6 | 83,934 | 58.749 / 1,428,690 | 61.008 / 1,375,789 | 59.782 / 1,404,008 |
| tactical | 1177 / e4d5 | 4,861 | 1.437 / 3,383,683 | 1.422 / 3,418,424 | 1.452 / 3,347,335 |
| evasion | -81 / c4c5 | 20,741 | 15.102 / 1,373,403 | 16.222 / 1,278,556 | 14.156 / 1,465,163 |
| middlegame | 250 / c3d5 | 141,150 | 96.645 / 1,460,498 | 99.995 / 1,411,567 | 96.511 / 1,462,533 |
| transposition-pawns | 6 / e1d2 | 1,563 | 0.378 / 4,138,204 | 0.361 / 4,335,644 | 0.378 / 4,130,549 |

Summed medians A/B/C: 191.307 / 198.835 / 191.165 ms. Aggregate NPS: 1,482,784 / 1,426,645 / 1,483,886.

### Depth 5, TT on

| Position | Score / best | Nodes (all) | A ms / NPS | B24 ms / NPS | C ms / NPS |
| --- | --- | ---: | ---: | ---: | ---: |
| start | 197 / e2e3 | 27,911 | 16.905 / 1,651,030 | 17.013 / 1,640,568 | 16.837 / 1,657,718 |
| kiwipete | 403 / d5e6 | 65,515 | 45.659 / 1,434,866 | 48.375 / 1,354,306 | 46.798 / 1,399,952 |
| tactical | 1177 / e4d5 | 4,280 | 1.364 / 3,137,139 | 1.369 / 3,126,141 | 1.392 / 3,075,154 |
| evasion | -81 / c4c5 | 19,834 | 17.574 / 1,128,586 | 17.468 / 1,135,473 | 16.319 / 1,215,370 |
| middlegame | 250 / c3d5 | 120,175 | 91.095 / 1,319,231 | 91.499 / 1,313,396 | 89.939 / 1,336,183 |
| transposition-pawns | 6 / e1d2 | 1,532 | 0.838 / 1,827,944 | 0.822 / 1,862,840 | 0.827 / 1,852,478 |

Summed medians A/B/C: 173.435 / 176.546 / 172.112 ms. Aggregate NPS: 1,379,462 / 1,355,154 / 1,390,066.

### Depth 6, TT off

| Position | Score / best | Nodes (all) | A ms / NPS | B24 ms / NPS | C ms / NPS |
| --- | --- | ---: | ---: | ---: | ---: |
| kiwipete | -220 / e2a6 | 219,294 | 168.657 / 1,300,233 | 186.741 / 1,174,324 | 153.290 / 1,430,586 |
| middlegame | -126 / e2e1 | 564,514 | 412.978 / 1,366,934 | 428.041 / 1,318,832 | 395.355 / 1,427,866 |
| evasion | -787 / d2d4 | 157,868 | 110.443 / 1,429,412 | 112.719 / 1,400,550 | 109.544 / 1,441,135 |

Summed medians A/B/C: 692.078 / 727.501 / 658.189 ms. Aggregate NPS: 1,360,650 / 1,294,398 / 1,430,708.

### Depth 6, TT on

| Position | Score / best | Nodes (all) | A ms / NPS | B24 ms / NPS | C ms / NPS |
| --- | --- | ---: | ---: | ---: | ---: |
| kiwipete | -220 / e2a6 | 167,760 | 134.617 / 1,246,201 | 158.100 / 1,061,099 | 126.879 / 1,322,208 |
| middlegame | -126 / e2e1 | 482,640 | 380.577 / 1,268,178 | 385.773 / 1,251,098 | 347.436 / 1,389,146 |
| evasion | -787 / d2d4 | 99,849 | 73.148 / 1,365,032 | 82.338 / 1,212,675 | 73.640 / 1,355,910 |

Summed medians A/B/C: 588.342 / 626.211 / 547.955 ms. Aggregate NPS: 1,275,192 / 1,198,077 / 1,369,180.

### Depth 6 fresh-JVM repeat, TT off

| Position | Score / best | Nodes (all) | A ms / NPS | B24 ms / NPS | C ms / NPS |
| --- | --- | ---: | ---: | ---: | ---: |
| kiwipete | -220 / e2a6 | 219,294 | 171.270 / 1,280,399 | 191.887 / 1,142,828 | 158.361 / 1,384,775 |
| middlegame | -126 / e2e1 | 564,514 | 407.534 / 1,385,193 | 420.538 / 1,342,362 | 379.711 / 1,486,693 |
| evasion | -787 / d2d4 | 157,868 | 109.666 / 1,439,531 | 113.796 / 1,387,289 | 107.879 / 1,463,380 |

Summed medians A/B/C: 688.470 / 726.221 / 645.951 ms. Aggregate NPS: 1,367,781 / 1,296,680 / 1,457,813.

### Depth 6 fresh-JVM repeat, TT on

| Position | Score / best | Nodes (all) | A ms / NPS | B24 ms / NPS | C ms / NPS |
| --- | --- | ---: | ---: | ---: | ---: |
| kiwipete | -220 / e2a6 | 167,760 | 149.807 / 1,119,840 | 162.122 / 1,034,777 | 144.196 / 1,163,420 |
| middlegame | -126 / e2e1 | 482,640 | 368.923 / 1,308,239 | 378.796 / 1,274,141 | 341.647 / 1,412,685 |
| evasion | -787 / d2d4 | 99,849 | 83.248 / 1,199,411 | 78.066 / 1,279,031 | 80.387 / 1,242,110 |

Summed medians A/B/C: 601.978 / 618.984 / 566.230 ms. Aggregate NPS: 1,246,306 / 1,212,065 / 1,324,990.

### Aggregate changes relative to CURRENT_INSERTION

| Run | Identical nodes per mechanic | B24 wall / throughput change | C wall / throughput change |
| --- | ---: | ---: | ---: |
| Depth 5, TT off | 283,667 | 3.935% / -3.786% | -0.074% / 0.074% |
| Depth 5, TT on | 239,247 | 1.794% / -1.762% | -0.763% / 0.769% |
| Depth 6, TT off | 941,676 | 5.118% / -4.869% | -4.897% / 5.149% |
| Depth 6, TT on | 750,249 | 6.437% / -6.047% | -6.865% / 7.371% |
| Depth 6 fresh-JVM repeat, TT off | 941,676 | 5.483% / -5.198% | -6.176% / 6.582% |
| Depth 6 fresh-JVM repeat, TT on | 750,249 | 2.825% / -2.747% | -5.939% / 6.314% |

### Shared deterministic PVs

TT off/on and all mechanics share the following PVs in these measured runs.

| Depth / position | PV |
| --- | --- |
| 5 / start | e2e3 e7e5 d1f3 g8e7 f3f7 |
| 5 / kiwipete | d5e6 e7e6 e2a6 h3g2 f3f6 |
| 5 / tactical | e4d5 e6d5 d3g6 e8d8 g6c6 |
| 5 / evasion | c4c5 a3b4 c5b6 b2a1q d1a1 |
| 5 / middlegame | c3d5 e7d7 g5f6 g7f6 f3e5 |
| 5 / transposition-pawns | e1d2 e8d7 d2c3 d7d6 c3d4 |
| 6 / kiwipete | e2a6 b4c3 d2c3 e6d5 e5f7 h3g2 |
| 6 / middlegame | e2e1 h7h6 c3d5 c5f2 e1f2 f6d5 |
| 6 / evasion | d2d4 a3b4 a1b1 b6a7 h6f7 e8f7 |

### Bounded crossover screen

Eight to 32 spans the observed typical lists/quiet ranges; tactical ranges are
mostly much smaller. 512 tests the same total-key implementation without any
quicksort, rather than assuming a historical threshold is optimal.

TT off: median milliseconds; all nodes/score/best/PV identical.

| Position | A | B8 | B16 | B24 | B32 | B512 (insertion-only) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 17.377 | 18.781 | 17.974 | 17.483 | 17.360 | 17.159 |
| kiwipete | 59.366 | 61.363 | 60.854 | 60.742 | 61.183 | 60.340 |
| tactical | 1.538 | 1.528 | 1.509 | 1.510 | 1.477 | 1.485 |
| evasion | 15.885 | 17.124 | 16.726 | 17.406 | 17.024 | 16.675 |
| middlegame | 98.012 | 101.838 | 99.679 | 99.971 | 100.511 | 98.673 |
| transposition-pawns | 0.403 | 0.382 | 0.362 | 0.351 | 0.348 | 0.353 |

| Crossover | Wall change vs A | Throughput change vs A |
| --- | ---: | ---: |
| 8 | 4.380% | -4.196% |
| 16 | 2.349% | -2.295% |
| 24 | 2.534% | -2.472% |
| 32 | 2.763% | -2.689% |
| 512 | 1.092% | -1.080% |

TT on: median milliseconds; all nodes/score/best/PV identical.

| Position | A | B8 | B16 | B24 | B32 | B512 (insertion-only) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 16.464 | 16.970 | 16.838 | 16.433 | 16.514 | 16.037 |
| kiwipete | 49.407 | 51.176 | 50.292 | 49.993 | 49.909 | 49.962 |
| tactical | 1.422 | 1.436 | 1.414 | 1.372 | 1.370 | 1.401 |
| evasion | 15.881 | 17.416 | 17.542 | 17.314 | 17.316 | 16.321 |
| middlegame | 87.784 | 91.987 | 91.293 | 91.699 | 91.234 | 88.975 |
| transposition-pawns | 0.422 | 0.445 | 0.422 | 0.402 | 0.390 | 0.405 |

| Crossover | Wall change vs A | Throughput change vs A |
| --- | ---: | ---: |
| 8 | 4.695% | -4.485% |
| 16 | 3.745% | -3.610% |
| 24 | 3.403% | -3.291% |
| 32 | 3.123% | -3.028% |
| 512 | 1.003% | -0.993% |

B16 has the smallest hybrid aggregate cost with TT off; B32 with TT on.
Neither beats B512 across the aggregate screen, and B512 still costs about 1%
against A. No threshold merits adoption. The B24 depth-6 confirmation is worse
than A in both runs/settings. Further threshold tuning or a depth-6 grid was
not justified by this bounded result; no optimum is asserted.

## Untimed list/range and consumption diagnostics

Diagnostics run separately through existing evaluator callbacks with independent
re-generation/classification and inspection of the current history/TT hint.
Each diagnostic result is checked against an uninstrumented Search. No timed
measurement uses this evaluator. Only expanded nodes reaching move consumption
are counted: static/terminal/draw and TT-resolved nodes do no sorting. Generated
class distributions include a possible hash in its ordinary class; actual A
ranking excludes the legal hash (at most one move). Insertion comparisons/shifts
are simulated exactly on each original class and snapshotted history, excluding
hash. Selection comparisons are the exact full-list scan count implied by the
observed consumed prefix. These counts are work metrics, not hardware costs.

| Depth / TT | Ordered nodes | Legal moves in ordered lists | Consumed | Unconsumed | Tail % | Early cutoffs |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 5 / off | 37,015 | 1,455,794 | 283,661 | 1,172,133 | 80.515 | 29,026 |
| 5 / on | 34,909 | 1,387,023 | 239,241 | 1,147,782 | 82.751 | 28,293 |
| 6 / off | 230,421 | 10,217,155 | 941,673 | 9,275,482 | 90.783 | 208,997 |
| 6 / on | 185,490 | 8,225,038 | 750,246 | 7,474,792 | 90.879 | 168,542 |

| Depth / TT | A key comparisons | A shifts | Quiet shifts | C scan comparisons | Nodes with zero quiet shifts |
| --- | ---: | ---: | ---: | ---: | ---: |
| 5 / off | 7,995,145 | 6,690,756 | 6,642,991 | 5,593,864 | 18.714% |
| 5 / on | 7,629,700 | 6,385,350 | 6,339,166 | 4,833,038 | 19.442% |
| 6 / off | 46,579,270 | 37,430,561 | 36,864,568 | 22,403,046 | 31.805% |
| 6 / on | 38,510,902 | 31,156,341 | 30,701,483 | 18,037,342 | 30.532% |

| Depth / TT / range | Mean | P50 | P90 | P99 | Max | Above 24 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 5 / off / list | 39.330 | 42 | 51 | 56 | 63 | 91.190% |
| 5 / off / good | 2.166 | 2 | 5 | 10 | 17 | 0.000% |
| 5 / off / bad | 2.704 | 3 | 5 | 6 | 10 | 0.000% |
| 5 / off / quiet | 34.460 | 36 | 45 | 49 | 53 | 88.499% |
| 5 / off / consumed | 7.663 | 1 | 35 | 44 | 55 | 17.696% |
| 5 / on / list | 39.733 | 42 | 51 | 56 | 63 | 91.326% |
| 5 / on / good | 2.174 | 2 | 5 | 10 | 17 | 0.000% |
| 5 / on / bad | 2.749 | 3 | 5 | 6 | 10 | 0.000% |
| 5 / on / quiet | 34.810 | 37 | 45 | 49 | 53 | 88.923% |
| 5 / on / consumed | 6.853 | 1 | 34 | 44 | 55 | 15.323% |
| 6 / off / list | 44.341 | 46 | 51 | 56 | 65 | 98.060% |
| 6 / off / good | 3.551 | 3 | 8 | 12 | 20 | 0.000% |
| 6 / off / bad | 3.184 | 3 | 5 | 8 | 11 | 0.000% |
| 6 / off / quiet | 37.606 | 39 | 44 | 50 | 58 | 96.747% |
| 6 / off / consumed | 4.087 | 1 | 2 | 42 | 61 | 8.692% |
| 6 / on / list | 44.342 | 46 | 51 | 57 | 65 | 98.030% |
| 6 / on / good | 3.445 | 3 | 7 | 12 | 20 | 0.000% |
| 6 / on / bad | 3.243 | 3 | 5 | 8 | 11 | 0.000% |
| 6 / on / quiet | 37.654 | 39 | 44 | 50 | 58 | 96.979% |
| 6 / on / consumed | 4.045 | 1 | 1 | 43 | 61 | 8.458% |

Tactical ranges are naturally small: good P99 is 10 at depth 5 and 12 at
depth 6; bad P99 is 6 and 8. Quiet ranges are not generally below the tested
crossovers. Full-sort quicksort activates for 91.190% / 91.326% of ordered lists
at depth 5 and 98.060% / 98.030% at depth 6 with threshold 24 (TT off/on).
Thus the lack of hybrid benefit is not explained by never reaching its large
range algorithm. No quicksort partition-level comparison counter was added.

At depth 6 the median consumed prefix is one move, with roughly 91% of generated
candidates left unused. C reduces key comparisons substantially relative to A
and avoids its millions of array shifts on the unused tail. At depth 5 C scans
about 70% / 63% as many keys as A compares, but end-to-end time is almost tied;
at depth 6 that fraction falls to about 48% / 47%, and elapsed gains repeat.
Classification/SEE, generation and child Search remain shared costs. These
observations support the lazy result without claiming a specific cache/JIT
cause or equating one comparison with a fixed amount of time.

## Limits and deliberately skipped work

No full/long-running suite, slow NNUE tests, self-play, strength testing, staged
generation, browser/UI checks (no UI changed), JFR/allocation profiling, hardware
performance counters or extensive sorting-algorithm/threshold survey. Targeted
tests and source/bytecode inspection satisfy the bounded validation requested.
No required check is unavailable. No depth-6 crossover grid: the bounded depth-5
screen already favours insertion-only over every hybrid in aggregate, and B24
is worse in both depth-6 runs. The fresh-JVM repeat covers the primary A/B/C
comparison, not the full crossover grid.

Timings remain host/JVM/corpus dependent and individual positions vary; TT-on
absolute times show run-to-run variation. There is no claim of statistically
universal superiority or playing strength. The extra 512 KiB lazy key payload
is a real ownership cost. Future staged generation changes the work available
to avoid, so it must be measured independently against this provisional baseline.

No canon/frontier maintenance or mirror refresh is required by this unit because
those files were not changed. Production ordering and history lifecycle remain
as before. Required human actions after this prompt: **None**.

