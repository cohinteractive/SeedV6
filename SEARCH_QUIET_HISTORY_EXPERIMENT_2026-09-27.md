# SR-016 main quiet-history experiment - 2026-09-27

Starting revision: `5ff520ce9ba3a874290cae1226e7a736b310966c`, clean worktree. Verified authoritative Search Contract **R009** and Research Frontier **F004** before implementation. No repository-local AGENTS.md was present at the root, in its parents or under the repository; the supplied shared governance and Search sources apply. Root VERSION_STATE.txt activates the maintained finalizer; CODEXLOG_CURRENT.md is absent and is not created.

## Research outcome

**Recommend outcome 1: MAIN QUIET HISTORY FAVOURED.** Main quiet history merits being the provisional SR-016 baseline for later comparisons with continuation history, killers and countermoves. This is a research recommendation, not programme acceptance or production adoption. SR-016 is not marked ACCEPTED or IMPLEMENTED; Contract/Frontier stay R009/F004.

The candidate reduced aggregate nodes and summed median wall time in both TT configurations at depths 5 and 6. The wall-time gains are 14.187% / 16.295% at depth 5 and 10.775% / 12.156% at depth 6 (TT off / on). Throughput fell in all four comparisons: useful quiet ordering more than offset the measured cost on this corpus.

Benefits are uneven. Starting position and Middlegame gain substantially; the pawn position gains substantially but is tiny. Kiwipete and Evasion are slower: their node counts are unchanged at depth 5 and only slightly reduced at depth 6. The candidate is favoured on the specified aggregate methodology, not faster at every position. No additional main-history experiment or parameter tuning is needed to state this bounded result.

## Implementation and scope

- `app/src/main/java/com/ohinteractive/seedv6/search/exact/QuietHistory.java`: primitive key, searched-prefix update and gravity arithmetic.
- `app/src/main/java/com/ohinteractive/seedv6/search/exact/ExactSearch.java`: distinct experimental mode, owner table, invocation reset, guarded quiet-cutoff learning, abort clearing and quiet ranking.
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/ExactSearchHarness.java`: single/paired selectors and display name only; benchmark methodology unchanged.
- `app/src/test/java/com/ohinteractive/seedv6/search/exact/QuietHistoryTest.java`: ten focused primitive, integration, ordering and lifecycle tests.
- `app/src/test/java/com/ohinteractive/seedv6/search/exact/ExactSearchOrderingTest.java`: add the candidate to the existing semantic/ordering matrix without weakening historical visitation assertions.
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/ExactSearchHarnessTest.java`: independently selectable candidate and paired repeatability.
- This evidence report and required root VERSION_STATE.txt finalization.

New mode: `see-material-quiet-history`, Java constant `ExactSearch.SEE_MATERIAL_QUIET_HISTORY`. Paired selector: `--ordering=see-material,see-material-quiet-history`.

CONTROL remains the production/default mode. SEE_MATERIAL and all older modes/aliases retain their meanings, including `both` and `see-material-history` (capture history). The new candidate does not allocate or enable capture history. Existing constructors still select CONTROL.

A = SEE_MATERIAL: legal hash, SEE-good tacticals by descending immediate material, SEE-bad tacticals by descending immediate material, quiets in stable existing order.

B = SEE_MATERIAL_QUIET_HISTORY: same hash and tactical policy, quiets by descending learned main history with stable ties.

Full `Gen.genAll` remains. No killers, countermoves, continuation/relative/capture history, previous PV, staged generation, pruning, reductions, PVS or general Sort framework is added.

## Key, ownership and lifecycle

Inspected retained legacy `search/order/MoveOrdering.java`, `StagedMovePicker.java` and `alphabeta/AlphaBetaPvsSearch.java`. Legacy main history uses side/piece/from/to, a nonnegative saturating reward and accompanying killer state. Its identity is evidence; its killers, picker and additive update are not imported.

Verified `Piece.KING..PAWN = 1..6`, `Board.PLAYER_SHIFT = 3`, `Board.PIECE_BITS = 15` and source/destination square extraction in `Move`. Valid packed moving-piece codes are white 1..6 and black 9..14, not twelve contiguous codes.

One flat `int[16 * 64 * 64]` is allocated per candidate owner: **65,536 ints, 262,144 bytes / 256 KiB payload**, plus JVM array overhead. The direct index is:

```text
((movingPieceIncludingSide * 64 + fromSquare) * 64) + toSquare
```

All 49,152 valid side/piece/from/to tuples are distinct; codes 0, 7, 8 and 15 are unused. The spare slots preserve direct packed indexing without remapping. Neither evaluator output, Search score, TT state, ply nor parent context participates.

Each valid top-level fixed-depth invocation clears its table before any node work, including calls inside a single beginRequest lifetime, depth zero and immediately cancelled requests. All nodes in that invocation share the owner table. Separate owners never share it. No iterative-deepening cross-invocation reuse is introduced. Reset is inside timed Search; owner construction is outside timing.

Before updating at a beta cutoff the candidate checks cancellation, including cancellation raised by its final evaluated child with TT off. An Aborted invocation clears the candidate table and returns the established incomplete result (invalid score, no best move/PV); it publishes no root TT result. Completed internal cutoffs may learn while the invocation is running, but an eventual abort discards that table and the next invocation resets independently. TT-resolved nodes, terminals, rule draws and static leaves do not learn.

## Update and ordering mechanics

Only a searched quiet beta-cutoff move receives a reward. After that reward, each earlier actually searched quiet at the same node receives a malus. The existing per-ply move array supplies this prefix without extra per-node state. Generated but unsearched siblings do not update. Tactical cutoffs cause no quiet-history updates; captures, EP and every promotion are excluded. Castling is quiet.

A successful quiet hash receives the same reward; a previously searched failed quiet hash receives the ordinary malus when a later quiet cuts off. Hash priority never reads history. Applicable TT score cutoffs are not searched-move evidence.

At remaining depth d, `m = min(d, 64)^2`: successful move +m; failed quiet prefix -m. Bounds are +/-16384. The arithmetic matches the existing capture-history gravity arithmetic, reproduced in the separate quiet helper:

```text
b = clamp(bonus, -16384, 16384)
h = h + b - h * abs(b) / 16384
```

Java integer division truncates toward zero. Search bonuses are 1..4096. Valid history stays bounded; even the fully clamped product is at most 268,435,456, safely inside signed int. No parameters were tuned.

The unchanged tactical ranker finishes first. A direct stable insertion pass ranks the quiet scratch range, reusing the existing primitive material-score scratch. Strict less-than shifting preserves equal-history relative order. The legal hash is excluded from ranking and remains first exactly once. Quiets cannot cross either tactical class.

Source and `javap -c -p` inspection show no added per-node heap allocation, boxing, collections, streams, comparators or strategy-object dispatch. Constructor table allocation and the established top-level Search allocations remain. This is static/bytecode evidence, not allocation-profiler or JIT machine-code verification. Insertion is experimental only; its measured cost does not decide SR-017 or predict the cost of a future handcrafted Sort.

## Validation

Final targeted run: **127 tests, 14 suites, zero failures/errors/skips**, BUILD SUCCESSFUL in 5 seconds:

```powershell
.\gradlew.bat :app:test --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.search.tt.*' --tests 'com.ohinteractive.seedv6.search.order.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' -Pheadless=true --console=plain
```

The initial attempt failed test compilation because a new fixture referred to nonexistent START_SQUARE_SHIFT; source squares occupy the low six bits. The next run had two new fixture failures: Kiwipete depth 3 did not exercise quiet learning, so lifecycle tests requiring learned evidence switched to Starting position. A capture-underpromotion cutoff fixture also retains a black pawn so insufficient-material adjudication does not preempt the intended cutoff. These were test-fixture corrections; no production repair, exact-score failure or historical assertion weakening occurred.

Coverage:

- All 49,152 side/piece/from/to tuples, footprint, zero state, positive/negative updates, gravity reversal, bounds, repeated saturation and depth cap.
- Real searched cutoffs with quiet hash winner, failed quiet hash, tactical hash, ordinary failed quiets and unsearched quiets; tactical hash cutoffs including EP and quiet/capture promotions never learn.
- Both colours' special moves, all promotion choices, castling, check/evasion, seeded opposing history, stable equal history, every legal hash and an illegal hint, complete unique move sets, deterministic ranking and unchanged tactical order.
- Owner isolation, poisoned-state reset and identical final learned tables/nodes/best/PV across repeated fixed-depth searches in one request, TT off/on; static and immediate cancellation resets.
- Cancellation after the final cutoff child is observed before update, later abort after valid internal learning clears history, interruption stays flagged, incomplete result stays invalid, and aborted root TT publication is absent.
- **588 exhaustive score/optimal-best/legal-consistent-PV comparisons**: 14 fixtures, depths 0..2, seven modes, TT off/on. They include mate, stalemate, rule-50, insufficient material, promotions and EP. Repetition and final-leaf cancellation use the existing all-mode tests.
- Existing exact/TT tests cover exact-depth and generation qualification, bounds, mate normalization, history-sensitive identity and completed-result semantics. Historical deterministic depth-5 Start counts are unchanged: SEE_MATERIAL 48,224 TT-off / 43,737 TT-on; CONTROL, SEE_TIERED and SEE_TACTICAL recorded counts remain asserted.
- Existing material-accounting tests retain 33 generated tactical comparisons and 19 explicit special/capture values.
- All four benchmarks below pass per-round exact-score equality, deterministic per-mode score/best/PV/nodes and untimed legal optimal-best/PV realization checks.

Static review: `git diff --check`, complete source diff, protected-source comparison and `javap -c -p` allocation/call-site inspection. SEE implementation and threshold zero are unchanged. No exact-value, move-uniqueness or ownership defect was found.

Deliberately not run: full/long-running suites, standalone SEE suites (SEE and tactical classification/ranking unchanged), production driver/GUI/browser/UCI lifecycle suites, NNUE/BRN performance campaigns, playing-strength games, larger/deeper corpus, history parameter/key tuning, cross-invocation persistence experiments, allocation profiler/JIT assembly analysis or SR-017 mechanics comparisons. Existing exact TT tests do retain their evaluator-equivalence fixtures. No browser verification is required for this headless Search unit.

## Benchmark methodology

Unchanged six-position depth-5 corpus from `ExactSearchHarness.orderingPositions()`: Starting position, Kiwipete, Tactical, Evasion, Middlegame and Transposition-pawns. Depth-6 confirmation is exactly Kiwipete, Middlegame and Evasion, because depth-5 evidence was favourable and confirmation remained inexpensive.

Windows 11 amd64, AMD Ryzen 5 5500 (6 cores/12 logical processors), OpenJDK 21 build 21+35-2513. HCE, one Search thread, `-Xms256m -Xmx256m -Xbatch`. Five warmups and seven measured repetitions per position and mode. The two owners alternate execution order each warmup/measured round. Every candidate invocation starts empty. TT-off has no table; TT-on has separate cold 4 MiB requested tables, cleared outside Search timing.

Median = fourth of seven sorted elapsed samples. Search timing includes invocation setup and history reset; excludes FEN parsing, worker construction, TT clearing and semantic verification. NPS comes from that median sample. Aggregate throughput = total nodes / summed position median times, not the arithmetic mean of per-position NPS.

Four sequential fresh-JVM runs, with no concurrent validation workload: Gradle elapsed 8s, 8s, 22s and 20s. Total 252 measured searches and 180 warmups, plus untimed verification. No second timing batch was needed to classify these material aggregate effects; single-host/single-batch timing uncertainty remains.

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material,see-material-quiet-history --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material,see-material-quiet-history --tt=on' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=5 --repetitions=7 --ordering=see-material,see-material-quiet-history --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=5 --repetitions=7 --ordering=see-material,see-material-quiet-history --tt=on' --console=plain
```

M / H below means SEE_MATERIAL / SEE_MATERIAL_QUIET_HISTORY. Scores, best moves and PVs are identical for both modes in every measured fixture, TT mode and repetition. Equal-valued alternatives would have been allowed only after independent verification.

## Depth 5, TT off

| Position | Depth | Score | Best | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| start | 5 | 197 | e2e3 | 48,224 / 31,418 | 24.666 / 17.868 | 1,955,079 / 1,758,368 |
| kiwipete | 5 | 403 | d5e6 | 83,934 / 83,934 | 56.120 / 58.000 | 1,495,619 / 1,447,137 |
| tactical | 5 | 1177 | e4d5 | 5,233 / 4,861 | 1.448 / 1.444 | 3,613,950 / 3,367,276 |
| evasion | 5 | -81 | c4c5 | 20,741 / 20,741 | 14.634 / 15.931 | 1,417,315 / 1,301,910 |
| middlegame | 5 | 250 | c3d5 | 192,147 / 141,150 | 126.801 / 98.867 | 1,515,346 / 1,427,677 |
| transposition-pawns | 5 | 6 | e1d2 | 2,867 / 1,563 | 0.620 / 0.359 | 4,624,193 / 4,353,760 |

## Depth 5, cold 4 MiB TT on

| Position | Depth | Score | Best | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| start | 5 | 197 | e2e3 | 43,737 / 27,911 | 29.319 / 18.725 | 1,491,773 / 1,490,550 |
| kiwipete | 5 | 403 | d5e6 | 65,515 / 65,515 | 47.411 / 49.236 | 1,381,855 / 1,330,640 |
| tactical | 5 | 1177 | e4d5 | 4,550 / 4,280 | 1.424 / 1.483 | 3,195,898 / 2,885,458 |
| evasion | 5 | -81 | c4c5 | 19,834 / 19,834 | 15.173 / 16.732 | 1,307,224 / 1,185,386 |
| middlegame | 5 | 250 | c3d5 | 167,784 / 120,175 | 117.882 / 90.765 | 1,423,321 / 1,324,017 |
| transposition-pawns | 5 | 6 | e1d2 | 2,788 / 1,532 | 0.723 / 0.455 | 3,858,823 / 3,368,513 |

## Depth 6 confirmation, TT off

| Position | Depth | Score | Best | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 219,362 / 219,294 | 162.636 / 179.826 | 1,348,787 / 1,219,477 |
| middlegame | 6 | -126 | e2e1 | 844,853 / 564,514 | 564.269 / 452.746 | 1,497,251 / 1,246,867 |
| evasion | 6 | -787 | d2d4 | 160,027 / 157,868 | 107.758 / 112.156 | 1,485,059 / 1,407,571 |

## Depth 6 confirmation, cold 4 MiB TT on

| Position | Depth | Score | Best | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 167,833 / 167,760 | 134.612 / 148.342 | 1,246,795 / 1,130,903 |
| middlegame | 6 | -126 | e2e1 | 719,485 / 482,640 | 513.205 / 408.104 | 1,401,945 / 1,182,639 |
| evasion | 6 | -787 | d2d4 | 101,518 / 99,849 | 73.639 / 77.308 | 1,378,599 / 1,291,568 |

## Aggregate comparison

Summed milliseconds below use the displayed position medians (rounding uncertainty <=0.003 ms at depth 5 and <=0.0015 ms at depth 6). Aggregate NPS is therefore approximate, rounded to the nearest 1,000. Percentage changes are the harness's unrounded calculations. Negative wall time is faster; negative throughput is slower per node.

| Depth / TT | Total nodes M / H | Nodes change | Summed median ms M / H | Wall change | Approx aggregate NPS M / H | Throughput change |
|---|---:|---:|---:|---:|---:|---:|
| 5 / off | 353,146 / 283,667 | -19.674% | 224.289 / 192.469 | -14.187% | 1,575,000 / 1,474,000 | -6.394% |
| 5 / on | 304,208 / 239,247 | -21.354% | 211.932 / 177.396 | -16.295% | 1,435,000 / 1,349,000 | -6.044% |
| 6 / off | 1,224,242 / 941,676 | -23.081% | 834.663 / 744.728 | -10.775% | 1,467,000 / 1,264,000 | -13.792% |
| 6 / on | 988,836 / 750,249 | -24.128% | 721.456 / 633.754 | -12.156% | 1,371,000 / 1,184,000 | -13.629% |

## Interpretation and remaining limits

1. **Search-tree effect:** history reduces total nodes 19.7-24.1% across the four sets. Starting position and Middlegame contribute most of the useful reduction; Kiwipete/Evasion show little opportunity under this tactical-first fixed-depth policy.
2. **Mechanical cost:** aggregate throughput decreases about 6.0-6.4% at depth 5 and 13.6-13.8% at depth 6. This reflects the combined changed node mix and lookup/update/reset/insertion work; it is not an isolated microbenchmark of history overhead. Unchanged-node Kiwipete/Evasion depth-5 results give direct examples of added work without tree benefit. These costs cannot be assigned to a future SR-017 handcrafted sorter.
3. **Resulting wall time:** tree savings outweigh the measured costs in all four prescribed aggregate comparisons, while individual regressions remain visible. At depth 6 Middlegame improves about 20%; Kiwipete regresses about 10% and Evasion about 4-5%.

This is fixed-depth Search-speed evidence, not direct playing-strength evidence. Results are bounded to this corpus, HCE, one host, cold TT and reset-per-invocation history. Neural evaluators, larger samples, other depths, persistent/iterative history and real game strength remain unknown. No claim is made that the experimental sorter is the final architecture.

Retain this plain main-history candidate as the provisional comparison baseline for subsequent SR-016 work if the recommendation is adopted. Continuation history, killers, countermoves, relative history and richer context remain separate future experiments. No later mechanism, SR-017 decision, PVS or canon/frontier maintenance is part of this work unit.

The starting tree was clean; all six Java file changes and this report belong to this unit. Version finalization and the normal local work-unit commit are recorded in the completion response after their observed results. No push is authorized or performed. No build/version acceptance or programme acceptance is inferred from successful tests.

Human actions required after this prompt: None. Programme acceptance and any future canon/frontier maintenance remain separate decisions; this completed experiment requires no manual setup or mirror refresh.

## Captured benchmark evidence

The following retained harness records include PVs and unrounded-derived comparison percentages. Build success was observed for each run.

### Depth 5, TT off

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=5 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=off table=none
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=start requested=5 completed=5 best=e2e3 score=197 pv=[e2e3 e7e5 d1f3 g8e7 f3f7] nodes=48224 median_ms=24.666 nps=1955079 ordering=SEE_MATERIAL
position=start requested=5 completed=5 best=e2e3 score=197 pv=[e2e3 e7e5 d1f3 g8e7 f3f7] nodes=31418 median_ms=17.868 nps=1758368 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=start depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-34.850 wall_pct=-27.561 throughput_pct=-10.062
position=kiwipete requested=5 completed=5 best=d5e6 score=403 pv=[d5e6 e7e6 e2a6 h3g2 f3f6] nodes=83934 median_ms=56.120 nps=1495619 ordering=SEE_MATERIAL
position=kiwipete requested=5 completed=5 best=d5e6 score=403 pv=[d5e6 e7e6 e2a6 h3g2 f3f6] nodes=83934 median_ms=58.000 nps=1447137 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=kiwipete depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=0.000 wall_pct=3.350 throughput_pct=-3.242
position=tactical requested=5 completed=5 best=e4d5 score=1177 pv=[e4d5 e6d5 d3g6 e8d8 g6c6] nodes=5233 median_ms=1.448 nps=3613950 ordering=SEE_MATERIAL
position=tactical requested=5 completed=5 best=e4d5 score=1177 pv=[e4d5 e6d5 d3g6 e8d8 g6c6] nodes=4861 median_ms=1.444 nps=3367276 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=tactical depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-7.109 wall_pct=-0.304 throughput_pct=-6.826
position=evasion requested=5 completed=5 best=c4c5 score=-81 pv=[c4c5 a3b4 c5b6 b2a1q d1a1] nodes=20741 median_ms=14.634 nps=1417315 ordering=SEE_MATERIAL
position=evasion requested=5 completed=5 best=c4c5 score=-81 pv=[c4c5 a3b4 c5b6 b2a1q d1a1] nodes=20741 median_ms=15.931 nps=1301910 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=evasion depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=0.000 wall_pct=8.864 throughput_pct=-8.143
position=middlegame requested=5 completed=5 best=c3d5 score=250 pv=[c3d5 e7d7 g5f6 g7f6 f3e5] nodes=192147 median_ms=126.801 nps=1515346 ordering=SEE_MATERIAL
position=middlegame requested=5 completed=5 best=c3d5 score=250 pv=[c3d5 e7d7 g5f6 g7f6 f3e5] nodes=141150 median_ms=98.867 nps=1427677 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=middlegame depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-26.541 wall_pct=-22.030 throughput_pct=-5.785
position=transposition-pawns requested=5 completed=5 best=e1d2 score=6 pv=[e1d2 e8d7 d2c3 d7d6 c3d4] nodes=2867 median_ms=0.620 nps=4624193 ordering=SEE_MATERIAL
position=transposition-pawns requested=5 completed=5 best=e1d2 score=6 pv=[e1d2 e8d7 d2c3 d7d6 c3d4] nodes=1563 median_ms=0.359 nps=4353760 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=transposition-pawns depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-45.483 wall_pct=-42.097 throughput_pct=-5.848
comparison aggregate depth=5 positions=6 statistic=sum-of-position-medians baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-19.674 wall_pct=-14.187 throughput_pct=-6.394
```

### Depth 5, cold 4 MiB TT on

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=5 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=on table=cold/cleared before each request; explicit harness 4 MiB
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=start requested=5 completed=5 best=e2e3 score=197 pv=[e2e3 e7e5 d1f3 g8e7 f3f7] nodes=43737 median_ms=29.319 nps=1491773 ordering=SEE_MATERIAL
position=start requested=5 completed=5 best=e2e3 score=197 pv=[e2e3 e7e5 d1f3 g8e7 f3f7] nodes=27911 median_ms=18.725 nps=1490550 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=start depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-36.184 wall_pct=-36.132 throughput_pct=-0.082
position=kiwipete requested=5 completed=5 best=d5e6 score=403 pv=[d5e6 e7e6 e2a6 h3g2 f3f6] nodes=65515 median_ms=47.411 nps=1381855 ordering=SEE_MATERIAL
position=kiwipete requested=5 completed=5 best=d5e6 score=403 pv=[d5e6 e7e6 e2a6 h3g2 f3f6] nodes=65515 median_ms=49.236 nps=1330640 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=kiwipete depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=0.000 wall_pct=3.849 throughput_pct=-3.706
position=tactical requested=5 completed=5 best=e4d5 score=1177 pv=[e4d5 e6d5 d3g6 e8d8 g6c6] nodes=4550 median_ms=1.424 nps=3195898 ordering=SEE_MATERIAL
position=tactical requested=5 completed=5 best=e4d5 score=1177 pv=[e4d5 e6d5 d3g6 e8d8 g6c6] nodes=4280 median_ms=1.483 nps=2885458 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=tactical depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-5.934 wall_pct=4.186 throughput_pct=-9.714
position=evasion requested=5 completed=5 best=c4c5 score=-81 pv=[c4c5 a3b4 c5b6 b2a1q d1a1] nodes=19834 median_ms=15.173 nps=1307224 ordering=SEE_MATERIAL
position=evasion requested=5 completed=5 best=c4c5 score=-81 pv=[c4c5 a3b4 c5b6 b2a1q d1a1] nodes=19834 median_ms=16.732 nps=1185386 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=evasion depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=0.000 wall_pct=10.278 throughput_pct=-9.320
position=middlegame requested=5 completed=5 best=c3d5 score=250 pv=[c3d5 e7d7 g5f6 g7f6 f3e5] nodes=167784 median_ms=117.882 nps=1423321 ordering=SEE_MATERIAL
position=middlegame requested=5 completed=5 best=c3d5 score=250 pv=[c3d5 e7d7 g5f6 g7f6 f3e5] nodes=120175 median_ms=90.765 nps=1324017 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=middlegame depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-28.375 wall_pct=-23.003 throughput_pct=-6.977
position=transposition-pawns requested=5 completed=5 best=e1d2 score=6 pv=[e1d2 e8d7 d2c3 d7d6 c3d4] nodes=2788 median_ms=0.723 nps=3858823 ordering=SEE_MATERIAL
position=transposition-pawns requested=5 completed=5 best=e1d2 score=6 pv=[e1d2 e8d7 d2c3 d7d6 c3d4] nodes=1532 median_ms=0.455 nps=3368513 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=transposition-pawns depth=5 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-45.050 wall_pct=-37.052 throughput_pct=-12.706
comparison aggregate depth=5 positions=6 statistic=sum-of-position-medians baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-21.354 wall_pct=-16.295 throughput_pct=-6.044
```

### Depth 6 confirmation, TT off

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=6 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=off table=none
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=219362 median_ms=162.636 nps=1348787 ordering=SEE_MATERIAL
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=219294 median_ms=179.826 nps=1219477 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=kiwipete depth=6 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-0.031 wall_pct=10.569 throughput_pct=-9.587
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=844853 median_ms=564.269 nps=1497251 ordering=SEE_MATERIAL
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=564514 median_ms=452.746 nps=1246867 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=middlegame depth=6 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-33.182 wall_pct=-19.764 throughput_pct=-16.723
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=160027 median_ms=107.758 nps=1485059 ordering=SEE_MATERIAL
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=157868 median_ms=112.156 nps=1407571 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=evasion depth=6 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-1.349 wall_pct=4.082 throughput_pct=-5.218
comparison aggregate depth=6 positions=3 statistic=sum-of-position-medians baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-23.081 wall_pct=-10.775 throughput_pct=-13.792
```

### Depth 6 confirmation, cold 4 MiB TT on

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=6 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=on table=cold/cleared before each request; explicit harness 4 MiB
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=167833 median_ms=134.612 nps=1246795 ordering=SEE_MATERIAL
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=167760 median_ms=148.342 nps=1130903 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=kiwipete depth=6 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-0.043 wall_pct=10.200 throughput_pct=-9.295
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=719485 median_ms=513.205 nps=1401945 ordering=SEE_MATERIAL
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=482640 median_ms=408.104 nps=1182639 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=middlegame depth=6 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-32.919 wall_pct=-20.479 throughput_pct=-15.643
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=101518 median_ms=73.639 nps=1378599 ordering=SEE_MATERIAL
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=99849 median_ms=77.308 nps=1291568 ordering=SEE_MATERIAL_QUIET_HISTORY
comparison position=evasion depth=6 baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-1.644 wall_pct=4.984 throughput_pct=-6.313
comparison aggregate depth=6 positions=3 statistic=sum-of-position-medians baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY nodes_pct=-24.128 wall_pct=-12.156 throughput_pct=-13.629
```
