# ExactSearch material ordering within SEE classes - 2026-09-27

Starting revision: `58f8b99f2aaf9785597eda459273f9befc455f26`, clean worktree. This bounded SR-015 experiment compares immediate material evidence and an optional LVA tie-break against stable SEE_TACTICAL. CONTROL remains the production/default ordering. Previous modes, aliases, evidence, Search contract and frontier are preserved.

## Policies and mechanics

- **A / SEE_TACTICAL**: legal hash, stable SEE-good tacticals, stable SEE-bad tacticals, stable quiets.
- **B / SEE_MATERIAL**: same classes, with descending immediate material value inside each tactical class; equal values remain stable.
- **C / SEE_MATERIAL_LVA**: same primary ordering as B, then lower moving-piece value, then stable ties.

New selectors: `--ordering=see-material`, `--ordering=see-material-lva`, and `--ordering=see-tactical,see-material,see-material-lva` for the three-way comparison. `both` still selects CONTROL/SEE_TIERED; `control,see-tactical` still selects its original pair. Existing constructors select CONTROL.

`ExactSearch.tacticalMaterialValue` uses only the phase-independent `Eval.exchangeValue` constants already used by SEE: pawn 100, knight 320, bishop 330, rook 500, queen 975, king 20000. These are fixed exchange-accounting values, not HCE/NNUE evaluation output.

Immediate value is captured value plus promotion gain. EP substitutes pawn value for the empty packed victim. Promotion gain is promoted-piece value minus pawn value: queen 875, rook 400, bishop 230, knight 220. Capture-promotions add their victim; quiet promotions use only the gain. Underpromotions use the actual promoted type. C uses the pre-move piece for its attacker tie-break, so every promotion's attacker value is the pawn's 100.

Both material modes share `rankTacticals`, a small stable insertion pass over each already-produced tactical class. B uses the immediate integer value. C uses `immediate * (exchangeValue(KING) + 1) - movingValue`. The current scale is 20001, so primary evidence dominates every attacker difference; the maximum primary value is 1850 and the composite fits in an int. Strict comparison during shifting retains ties. No exact numeric SEE magnitude contributes to ranking.

Good moves compact into the generated list; bad tacticals use existing scratch. The legal hash is identified once during classification, bypasses SEE, is promoted outside the ranked ranges, and receives no material score. The existing final hash promotion sees it already at index zero. Quiets remain stable and receive no score.

One owner-held `int[512]` (2 KiB payload) is allocated only for B/C and reused for both tactical classes, alongside existing SEE scratch. No per-node allocation, boxing, collections, streams, comparator object or strategy dispatch is introduced. Source and compiled bytecode were inspected. This insertion pass is an experimental mechanism, not an SR-017 sorting architecture decision or a claim about eventual optimized handcrafted sorting cost.

Full `Gen.genAll` remains unchanged. Classification/ranking occurs after terminal/draw resolution, the static boundary and applicable TT cutoffs. No Search semantics, TT policy, evaluator, cancellation, production lifecycle, histories, pruning, qsearch, PVS or staged generation changes.

Changed files/symbols: ExactSearch mode constants/constructor/`orderSeeClassified`/`tacticalMaterialValue`/`rankTacticals`; ExactSearchHarness selection/rotated multi-mode comparisons/`orderingName`; ExactSearchOrderingTest; ExactSearchHarnessTest; this evidence file; required VERSION_STATE finalization.

## Validation

```powershell
.\gradlew.bat :app:test --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' -Pheadless=true --console=plain
git diff --check
```

**70 tests in 9 suites passed, zero failures/errors/skips; BUILD SUCCESSFUL in 6 s.** Main/test compilation and module packaging succeeded. Final staged whitespace checks passed.

- 420 exhaustive-oracle comparisons: 14 positions, depths 0-2, five modes, TT off/on. Exact scores, optimal legal best moves, consistent PVs and input-board preservation passed.
- 33 generated tactical moves were compared with independent material balance before/after a full Board move. Nineteen explicit value assertions cover a normal capture, EP in both colours and all four capture/quiet promotion choices in both colours.
- Root visitation checks prove complete unique move sets, exact class precedence, stable ties, primary-before-LVA precedence, actual LVA tie changes, both SEE classes, evasions, promotions and EP. A separate test-only stable library sort uses independent full-board material accounting.
- Good/bad/quiet legal hash hints, invalid hints, depth mismatch and older generations are exercised in every SEE mode. Hash remains first exactly once.
- Scratch sentinels and call-site inspection cover static leaves, terminal/draw nodes, TT cutoffs, and a sole-tactical-hash position whose remaining quiets must not be scored.
- Repetition, cancellation/incomplete results, last-leaf cancellation, absent aborted root TT publication and owner reuse pass in all five modes.
- Recorded depth-5 Start visitation counts for CONTROL, SEE_TIERED and SEE_TACTICAL remain exact. Existing deterministic visitation checks were extended, not weakened.
- Exact TT/oracle, SearchDriver/production-boundary, harness and independent threshold SEE tests pass.

No correctness failure, move-set failure or material-semantics ambiguity was found.

## Benchmark methodology

Same six-position corpus/FENs as [the preceding experiment](SEARCH_SEE_TACTICAL_EXPERIMENT_2026-09-27.md), from unchanged `ExactSearchHarness.orderingPositions()`: Start, Kiwipete, Tactical, Evasion, Middlegame and Transposition-pawns.

Windows 11 amd64, AMD Ryzen 5 5500 (6 cores/12 logical processors), OpenJDK 21 build `21+35-2513`, `-Xms256m -Xmx256m -Xbatch`, HCE, one Search thread. Five warmups and seven measured repetitions per position/mode. Three owners rotate execution order each round; separate evaluators and, for TT-on, separate cold 4 MiB tables cleared outside timing. Runs were sequential.

Median of seven Search elapsed samples, including existing request setup and excluding owner construction, FEN parsing and table clear. NPS is from the same median sample. Every round checks equal scores and deterministic per-mode nodes/best/PV. Untimed legal PV replay and TT-off CONTROL best-child/PV-endpoint re-searches verify realized root values.

Primary commands:

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-tactical,see-material,see-material-lva --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-tactical,see-material,see-material-lva --tt=on' --console=plain
```

Both passed (15 s and 16 s Gradle elapsed). Depth 5 comprises 252 measured searches and 180 warmups. All scores, best moves and PVs match across modes and TT settings. A's node counts exactly match the prior SEE_TACTICAL evidence.

Material-only's depth-5 aggregate benefit justified a bounded depth-6 confirmation on contrasting Kiwipete, Middlegame and Evasion fixtures. A feasibility pilot completed in 13 s; its timings are excluded from reported results:

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=1 --repetitions=1 --ordering=see-tactical,see-material,see-material-lva --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=5 --repetitions=7 --ordering=see-tactical,see-material,see-material-lva --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=5 --repetitions=7 --ordering=see-tactical,see-material,see-material-lva --tt=on' --console=plain
```

Confirmation runs passed (44 s and 41 s), with the same five/seven protocol: 126 measured searches and 90 warmups. Scores, best moves and PVs match across modes and TT settings at depth 6 too. Depths are reported and aggregated separately.

## Depth 5, TT off

| Position | Mode | Depth | Score | Best | Nodes | Median ms | NPS |
| --- | --- | ---: | ---: | --- | ---: | ---: | ---: |
| start | A | 5 | 197 | e2e3 | 48,287 | 30.460 | 1,585,254 |
| start | B | 5 | 197 | e2e3 | 48,224 | 31.166 | 1,547,347 |
| start | C | 5 | 197 | e2e3 | 48,283 | 30.583 | 1,578,742 |
| kiwipete | A | 5 | 403 | d5e6 | 92,891 | 80.871 | 1,148,638 |
| kiwipete | B | 5 | 403 | d5e6 | 83,934 | 69.865 | 1,201,380 |
| kiwipete | C | 5 | 403 | d5e6 | 83,395 | 74.867 | 1,113,902 |
| tactical | A | 5 | 1177 | e4d5 | 5,277 | 1.652 | 3,194,890 |
| tactical | B | 5 | 1177 | e4d5 | 5,233 | 1.951 | 2,682,076 |
| tactical | C | 5 | 1177 | e4d5 | 5,143 | 1.537 | 3,345,475 |
| evasion | A | 5 | -81 | c4c5 | 20,428 | 18.885 | 1,081,687 |
| evasion | B | 5 | -81 | c4c5 | 20,741 | 20.145 | 1,029,565 |
| evasion | C | 5 | -81 | c4c5 | 20,752 | 19.037 | 1,090,087 |
| middlegame | A | 5 | 250 | c3d5 | 207,383 | 176.237 | 1,176,728 |
| middlegame | B | 5 | 250 | c3d5 | 192,147 | 153.648 | 1,250,570 |
| middlegame | C | 5 | 250 | c3d5 | 199,325 | 184.078 | 1,082,831 |
| transposition-pawns | A | 5 | 6 | e1d2 | 2,867 | 0.826 | 3,471,364 |
| transposition-pawns | B | 5 | 6 | e1d2 | 2,867 | 0.656 | 4,367,763 |
| transposition-pawns | C | 5 | 6 | e1d2 | 2,867 | 0.821 | 3,490,807 |

## Depth 5, TT on

| Position | Mode | Depth | Score | Best | Nodes | Median ms | NPS |
| --- | --- | ---: | ---: | --- | ---: | ---: | ---: |
| start | A | 5 | 197 | e2e3 | 43,800 | 28.069 | 1,560,468 |
| start | B | 5 | 197 | e2e3 | 43,737 | 28.370 | 1,541,658 |
| start | C | 5 | 197 | e2e3 | 43,788 | 29.248 | 1,497,117 |
| kiwipete | A | 5 | 403 | d5e6 | 74,360 | 60.676 | 1,225,531 |
| kiwipete | B | 5 | 403 | d5e6 | 65,515 | 53.817 | 1,217,366 |
| kiwipete | C | 5 | 403 | d5e6 | 63,609 | 52.594 | 1,209,427 |
| tactical | A | 5 | 1177 | e4d5 | 4,594 | 2.039 | 2,252,623 |
| tactical | B | 5 | 1177 | e4d5 | 4,550 | 1.761 | 2,583,465 |
| tactical | C | 5 | 1177 | e4d5 | 4,461 | 1.704 | 2,618,418 |
| evasion | A | 5 | -81 | c4c5 | 18,504 | 23.809 | 777,172 |
| evasion | B | 5 | -81 | c4c5 | 19,834 | 24.617 | 805,709 |
| evasion | C | 5 | -81 | c4c5 | 19,839 | 26.258 | 755,541 |
| middlegame | A | 5 | 250 | c3d5 | 180,188 | 220.640 | 816,659 |
| middlegame | B | 5 | 250 | c3d5 | 167,784 | 217.012 | 773,154 |
| middlegame | C | 5 | 250 | c3d5 | 173,053 | 243.104 | 711,847 |
| transposition-pawns | A | 5 | 6 | e1d2 | 2,788 | 1.244 | 2,242,058 |
| transposition-pawns | B | 5 | 6 | e1d2 | 2,788 | 1.396 | 1,996,848 |
| transposition-pawns | C | 5 | 6 | e1d2 | 2,788 | 1.334 | 2,089,955 |

## Depth 6 confirmation, TT off

| Position | Mode | Depth | Score | Best | Nodes | Median ms | NPS |
| --- | --- | ---: | ---: | --- | ---: | ---: | ---: |
| kiwipete | A | 6 | -220 | e2a6 | 234,801 | 224.034 | 1,048,061 |
| kiwipete | B | 6 | -220 | e2a6 | 219,362 | 203.968 | 1,075,472 |
| kiwipete | C | 6 | -220 | e2a6 | 228,538 | 213.780 | 1,069,031 |
| middlegame | A | 6 | -126 | e2e1 | 1,074,158 | 814.819 | 1,318,278 |
| middlegame | B | 6 | -126 | e2e1 | 844,853 | 654.058 | 1,291,710 |
| middlegame | C | 6 | -126 | e2e1 | 887,461 | 700.535 | 1,266,834 |
| evasion | A | 6 | -787 | d2d4 | 158,136 | 139.817 | 1,131,025 |
| evasion | B | 6 | -787 | d2d4 | 160,027 | 143.917 | 1,111,937 |
| evasion | C | 6 | -787 | d2d4 | 164,562 | 148.871 | 1,105,401 |

## Depth 6 confirmation, TT on

| Position | Mode | Depth | Score | Best | Nodes | Median ms | NPS |
| --- | --- | ---: | ---: | --- | ---: | ---: | ---: |
| kiwipete | A | 6 | -220 | e2a6 | 184,896 | 195.485 | 945,831 |
| kiwipete | B | 6 | -220 | e2a6 | 167,833 | 182.580 | 919,227 |
| kiwipete | C | 6 | -220 | e2a6 | 168,138 | 173.350 | 969,936 |
| middlegame | A | 6 | -126 | e2e1 | 912,241 | 776.063 | 1,175,472 |
| middlegame | B | 6 | -126 | e2e1 | 719,485 | 626.244 | 1,148,889 |
| middlegame | C | 6 | -126 | e2e1 | 759,428 | 679.971 | 1,116,854 |
| evasion | A | 6 | -787 | d2d4 | 102,435 | 97.815 | 1,047,235 |
| evasion | B | 6 | -787 | d2d4 | 101,518 | 101.435 | 1,000,817 |
| evasion | C | 6 | -787 | d2d4 | 106,492 | 112.225 | 948,911 |

## Aggregates versus stable SEE_TACTICAL

Totals sum nodes and per-position medians at each fixed depth. Aggregate throughput is total nodes divided by summed median time, not an average of NPS. Displayed times sum rounded milliseconds; percentages come from unrounded harness nanoseconds.

| Depth / positions / TT | Mode | Total nodes | Summed median ms | Nodes vs A | Wall vs A | Throughput vs A |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| 5 / 6 / off | A | 377,133 | 308.931 | baseline | baseline | baseline |
| 5 / 6 / off | B | 353,146 | 277.431 | -6.360% | -10.196% | 4.272% |
| 5 / 6 / off | C | 359,765 | 310.923 | -4.605% | 0.645% | -5.217% |
| 5 / 6 / on | A | 324,234 | 336.477 | baseline | baseline | baseline |
| 5 / 6 / on | B | 304,208 | 326.973 | -6.176% | -2.824% | -3.449% |
| 5 / 6 / on | C | 307,538 | 354.242 | -5.149% | 5.280% | -9.906% |
| 6 / 3 / off | A | 1,467,095 | 1178.670 | baseline | baseline | baseline |
| 6 / 3 / off | B | 1,224,242 | 1001.943 | -16.553% | -14.994% | -1.835% |
| 6 / 3 / off | C | 1,280,561 | 1063.186 | -12.715% | -9.798% | -3.234% |
| 6 / 3 / on | A | 1,199,572 | 1069.363 | baseline | baseline | baseline |
| 6 / 3 / on | B | 988,836 | 910.259 | -17.568% | -14.878% | -3.159% |
| 6 / 3 / on | C | 1,034,058 | 965.546 | -13.798% | -9.708% | -4.529% |

## Interpretation

**Immediate material ordering is the stronger candidate here: favourable aggregate evidence, mixed per-position effects.** At depth 5, B reduces nodes 6.360%/6.176% and wall time 10.196%/2.824% (TT off/on). Depth 6 confirms reductions of 16.553%/17.568% in nodes and 14.994%/14.878% in wall time on the three-position subset.

Kiwipete and Middlegame generally benefit. Evasion regresses in wall time in both TT configurations at both depths; at depth 6 with TT on, B even searches fewer nodes (-0.895%) but takes longer (+3.701%). Start at depth 5 has tiny node savings but higher time. The short Tactical and Transposition-pawns timings vary and should not drive a general conclusion. These regressions remain visible rather than hidden by the aggregate.

**Attacker/LVA tie-breaking adds no useful aggregate benefit over B in this evidence.** At depth 5, C searches 1.874%/1.095% more nodes than B and takes approximately 12.072%/8.340% longer (TT off/on). At depth 6, C searches 4.600%/4.573% more nodes than B and takes approximately 6.112%/6.074% longer. C has individual wins, notably Kiwipete TT-on timing, but does not establish a stronger overall policy. Material-only should remain the stronger research candidate; attacker preference should not be adopted on conventional MVV-LVA grounds. C remains selectable solely as an experimental comparison.

Policy/tree shape and mechanical throughput remain separate. B's depth-5 aggregate throughput changes by +4.272% with TT off and -3.449% with TT on; C's changes by -5.217%/-9.906%. At depth 6, B throughput drops 1.835%/3.159% but its larger tree savings still improve wall time. C reduces nodes against A at depth 5 yet increases total time. These NPS effects include scoring/ranking, the changed visited-node mix and timing variability; they do not isolate ranking unit cost or establish an optimal sorting mechanism.

No production adoption or canon/frontier disposition is made. CONTROL remains default. The rejected policy placing bad tacticals after quiets is not revived.

## Limits and skipped work

One host, HCE static leaves, six positions at depth 5 and three at depth 6, cold 4 MiB TT, no confidence intervals or playing-strength result. Absolute timing varies between runs; comparisons use contemporaneous rotated modes, not earlier experiment timings. Small time differences, especially millisecond/sub-millisecond samples, may be noise. Other evaluators, depths, warm/larger TT and future ordering combinations remain unknown.

No full repository suite, long perft/exhaustive campaign, GUI checks, allocation/JIT/branch profiler, standalone ranking microbenchmark or playing-strength test was run. No capture history, quiet history or sorting-architecture research was undertaken. The experimental insertion mechanism's measured cost is not a forecast for a future optimized handcrafted Sort implementation.
