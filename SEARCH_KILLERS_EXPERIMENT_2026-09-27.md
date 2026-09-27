# SR-016 two-killer quiet-ordering experiment — 2026-09-27

Recommendation: **KILLERS NOT JUSTIFIED** as the new provisional SR-016 baseline.
Main quiet history alone remains preferred. The candidate is retained as an independently
selectable research mode. This is a completed bounded experiment, not SR-016 acceptance
or production adoption.

Search Contract **R009** and Research Frontier **F004** were verified unchanged.
SR-015 remains ACCEPTED, SR-016 and SR-017 remain PENDING. Starting worktree was
clean at `567c671d41740eac0f8bc3b338990c74a8e4e44f`.
The main-history baseline established by `79670d4` is unchanged; continuation
history remains a separate prior experiment and is absent from this candidate.

## Implementation and boundaries

- `search/exact/ExactSearch.java`: mode 8, candidate-only primitive killer storage,
  reset and cutoff integration, current-ply quiet promotion.
- `search/exact/KillerMoves.java`: two-slot recent-first primitive replacement.
- `tools/search/ExactSearchHarness.java`: standalone and paired mode selection/help.
- `search/exact/KillerMovesTest.java`: 11 focused primitive, ordering, lifecycle,
  integration and oracle tests.
- `ExactSearchOrderingTest.java`: candidate added to existing semantic and visitation
  matrices; all recorded historical visitation assertions retained.
- `CaptureHistoryTest.java`, `QuietHistoryTest.java`, `ContinuationHistoryTest.java`:
  reflection calls supply the added primitive ply argument; assertions unchanged.
- `ExactSearchHarnessTest.java`: standalone/pair selection and repeated TT-off/on runs.
- This report and the root version file complete the work unit.

Paths above are relative to the existing main/test Java package
`com/ohinteractive/seedv6`. No Search canon/frontier, old research reports, production
driver, SEE, evaluator, TT implementation or main-history primitive was changed.

A: `see-material-quiet-history`.
B: `see-material-quiet-history-killers` (`SEE_MATERIAL_QUIET_HISTORY_KILLERS`).

Every existing mode and alias retains its meaning. All ordinary constructors still
select CONTROL. No new candidate enables capture or continuation history.

### Storage, identity and lifecycle

`long[(ExactSearch.MAX_DEPTH + 1) * 2]`: established MAX_DEPTH is 256, matching
the existing 257 per-ply board/move/PV slots. Index is `2 * ply + slot`, slot 0
primary, slot 1 secondary; **514 longs / 4,112 payload bytes**, excluding array
header/alignment. Main history remains its existing 262,144-byte payload.
No new arbitrary ply limit or per-ply object is introduced.

Each slot holds the entire generated long move, without normalization or coordinate
truncation. Verified `Gen.genAll` encodes source, destination, side-inclusive moving
piece, victim and promotion fields. Every legal move has a nonzero moving-piece
field, so zero is an unambiguous empty sentinel. This path does not substitute the
experimental move-type generator. Exact equality with the current generated quiet
list determines eligibility, including side/piece differences and castling.

Each ExactSearch owner allocates its own array once, only for B. All fixed-depth
invocations clear main history and killers before the first cancellation check,
including calls within one `beginRequest/endRequest` scope. Nodes at the same
absolute search ply share those slots during that invocation. Cancelled/interrupted
invocations clear both tables before returning the existing invalid/incomplete
result. No cross-invocation or cross-iteration persistence is introduced.

### Learning and ordering

After the existing post-child cancellation checkpoint, a searched quiet beta-cutoff
move receives the unchanged main-history reward, earlier searched quiets receive
the unchanged malus, and the winner is recorded at the current ply. Tactical
cutoffs (including EP and every promotion) never become killers. Generated-but-
unsearched moves and failed quiets are not recorded as killers. A quiet hash cutoff
is eligible; an earlier failed quiet hash receives the ordinary history malus.

If the winner equals primary, slots do not change. Otherwise old primary becomes
secondary and the winner becomes primary. An existing secondary therefore swaps
to primary, with no duplicate. This is equivalent to the recent-first legacy
`search/order/MoveOrdering.recordKiller` policy, without importing its picker or
Search architecture.

Main history remains `h += bonus - h * abs(bonus) / 16384`, bound +/-16384,
`abs(bonus) = min(depth, 64)^2`, positive reward/negative malus. Killer storage has
no history score, weighting or parameter tuning.

Global order remains legal hash, material-ranked SEE >= 0 tacticals,
material-ranked SEE < 0 tacticals, then quiets. Full `Gen.genAll` is retained.
After the existing stable main-history insertion pass, the generated quiet array
stably promotes an exactly matching secondary and then primary using the existing
primitive exact-match/arraycopy helper. Thus primary precedes secondary, both
precede ordinary quiets, and every remaining quiet retains history order and stable
equal-history ties. Hash is excluded from this quiet range and remains globally
first. No move is inserted into the move set; absent/illegal/tactical stored values
cannot match a quiet, and a hash/killer overlap is visited once.

Current recursive ply is passed directly to the ordering method. Negamax recursion,
previous-move handling for the separate continuation mode, TT applicability, draw/
terminal precedence and mate ply remain unchanged.

No per-node arrays, boxing, collections, streams, comparators or strategy objects
are added. Static source and compiled `javap -p -c` inspection found no allocation/
boxing/collection bytecodes in killer recording, quiet ranking, class ordering or
stable promotion. Negamax's existing invalid-static-score exception/string path is
unchanged. Owner construction and top-level result/history setup retain their
existing allocations. This is source/bytecode evidence, not an allocation-profiler
claim. The insertion/promotions remain experimental; no SR-017 sorting decision or
future handcrafted Sort cost inference is made.

## Validation

Command:

```powershell
.\gradlew.bat :app:test --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.search.tt.*' --tests 'com.ohinteractive.seedv6.search.order.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' -Pheadless=true --console=plain
```

**151 tests, 16 suites, zero failures/errors/skips** on the final targeted run.
This includes all 11 new killer tests, 10 main-history tests, 10 continuation tests,
7 capture-history tests, ordering, ExactSearch, TT and harness suites.

The ordering matrix made **756 exhaustive score/optimal-move/PV comparisons**
(14 fixtures x depths 0..2 x nine modes x TT off/on). Killer-specific depth-3
oracle checks add 10 candidate comparisons across ordinary pawn play, castling,
evasions and white/black promotion paths, also checked against main history.
The existing main-history six-position depth-5 recorded nodes/scores/best moves
and historical CONTROL/SEE visitation assertions passed unchanged.

Coverage includes independent slots through the established maximum ply; primary
insert/shift/repeat/secondary swap; owner isolation; poisoned-state reset inside
one request; actual same-ply sharing and absolute-ply updates; full identity;
captures, EP, all promotions and castling; no learning at resolved TT nodes,
static/terminal/draw nodes or full-window nodes without cutoffs; real quiet/hash
cutoff reward/malus and searched-prefix restrictions; cancellation before updates
(asserted before cleanup), later cancellation after valid learning, interruption,
invalid result/no PV/no root TT publication; stable classes/ties and unique complete
legal visitation. Existing tests also cover repetition, mate, stalemate, draw,
exact-depth/current-generation TT applicability and legal/consistent PVs.

The first test run exposed three new fixture assumptions, not engine semantic
failures: a black capture-underpromotion left insufficient material and therefore
correctly drew before evaluation; an overbroad FEN-prefix selector included a
position without both legal castles; and the sharing observer wrongly required
a changed primary rather than recognizing reuse across nodes. Fixtures/observation
were corrected. Exhaustive exact-value equivalence passed on both runs. No
historical assertions were weakened.

All six benchmark batches completed successfully. Every warmup/measured search
repeated its mode's score/best/nodes/PV deterministically. All A/B exact scores,
best moves and PVs agreed; equal-valued alternatives were allowed but unnecessary.
Legal/optimal best moves and PV consistency were verified outside timing through
the existing TT-off CONTROL re-search surface.

## Benchmark protocol

AMD Ryzen 5 5500, Windows 11 amd64, OpenJDK **21+35-2513**, HCE, one Search thread.
The unchanged Gradle JavaExec task supplies **-Xms256m -Xmx256m -Xbatch**.
Five warmups and seven measured repetitions for each position/mode, alternating
paired execution order each round. Each batch launches a fresh benchmark JVM;
one fresh-JVM repeat of both depth-6 batches checks the modest confirmation effect.

Elapsed time is the Search result's wall time, including per-invocation history/
killer resets and Search setup; owner construction and FEN parsing are excluded.
TT-off has no table; TT-on uses separate cold **4 MiB** tables cleared outside the
timed Search before every invocation. Both histories start empty on every measured
search. Semantic verification is outside timing. The fourth of seven elapsed
samples is the median. Aggregate time is the sum of position medians; aggregate
throughput is total nodes divided by that sum, not the mean of per-position NPS.

Reproduce depth 5 with TT off/on separately:

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history,see-material-quiet-history-killers --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history,see-material-quiet-history-killers --tt=on' --console=plain
```

For depth 6, replace `--position=ordering --depth=5` with
`--position=kiwipete,middlegame,evasion --depth=6`, run both TT modes, then repeat
those two commands once. No tuning or broader/deeper corpus was introduced.

Tables use A = main history and B = main history + killers. Scores are exact HCE
values at the stated depth. Summed milliseconds and approximate aggregate NPS below
are derived from the printed three-decimal position medians; comparison percentages
come directly from the harness's unrounded nanoseconds. Differences in last-digit
rounding are not additional precision.

### Depth 5, TT off

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 197 | e2e3 | 31,418 | 27,317 | 17.706 | 15.774 | 1,774,456 | 1,731,773 |
| kiwipete | 403 | d5e6 | 83,934 | 83,934 | 57.662 | 56.843 | 1,455,625 | 1,476,601 |
| tactical | 1177 | e4d5 | 4,861 | 4,885 | 1.384 | 1.432 | 3,512,537 | 3,412,027 |
| evasion | -81 | c4c5 | 20,741 | 20,741 | 15.258 | 15.349 | 1,359,397 | 1,351,319 |
| middlegame | 250 | c3d5 | 141,150 | 126,117 | 96.681 | 87.859 | 1,459,958 | 1,435,447 |
| transposition-pawns | 6 | e1d2 | 1,563 | 1,563 | 0.375 | 0.384 | 4,171,337 | 4,068,193 |

Totals: nodes 283,667 -> 264,557 (**-6.737%**); summed medians 189.066 -> 177.641 ms (**-6.042%**); aggregate throughput approximately 1,500,360 -> 1,489,279 NPS (**-0.739%**).

### Depth 5, cold 4 MiB TT on

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 197 | e2e3 | 27,911 | 25,475 | 16.301 | 14.843 | 1,712,184 | 1,716,355 |
| kiwipete | 403 | d5e6 | 65,515 | 65,515 | 46.265 | 45.987 | 1,416,069 | 1,424,654 |
| tactical | 1177 | e4d5 | 4,280 | 4,304 | 1.295 | 1.309 | 3,303,998 | 3,288,508 |
| evasion | -81 | c4c5 | 19,834 | 19,834 | 15.188 | 15.253 | 1,305,882 | 1,300,368 |
| middlegame | 250 | c3d5 | 120,175 | 109,553 | 84.913 | 78.101 | 1,415,265 | 1,402,712 |
| transposition-pawns | 6 | e1d2 | 1,532 | 1,532 | 0.387 | 0.400 | 3,963,777 | 3,834,793 |

Totals: nodes 239,247 -> 226,213 (**-5.448%**); summed medians 164.349 -> 155.893 ms (**-5.147%**); aggregate throughput approximately 1,455,725 -> 1,451,079 NPS (**-0.317%**).

### Depth 6 confirmation, TT off

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| kiwipete | -220 | e2a6 | 219,294 | 219,294 | 165.171 | 165.663 | 1,327,681 | 1,323,735 |
| middlegame | -126 | e2e1 | 564,514 | 560,499 | 401.627 | 405.924 | 1,405,566 | 1,380,797 |
| evasion | -787 | d2d4 | 157,868 | 157,706 | 105.947 | 106.595 | 1,490,070 | 1,479,489 |

Totals: nodes 941,676 -> 937,499 (**-0.444%**); summed medians 672.745 -> 678.182 ms (**0.808%**); aggregate throughput approximately 1,399,752 -> 1,382,371 NPS (**-1.242%**).

### Depth 6 confirmation, cold 4 MiB TT on

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| kiwipete | -220 | e2a6 | 167,760 | 167,760 | 130.072 | 130.785 | 1,289,747 | 1,282,720 |
| middlegame | -126 | e2e1 | 482,640 | 481,880 | 363.461 | 366.816 | 1,327,901 | 1,313,683 |
| evasion | -787 | d2d4 | 99,849 | 99,691 | 77.511 | 76.203 | 1,288,198 | 1,308,225 |

Totals: nodes 750,249 -> 749,331 (**-0.122%**); summed medians 571.044 -> 573.804 ms (**0.483%**); aggregate throughput approximately 1,313,820 -> 1,305,901 NPS (**-0.603%**).

### Depth 6 fresh-JVM repeat, TT off

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| kiwipete | -220 | e2a6 | 219,294 | 219,294 | 167.326 | 169.888 | 1,310,581 | 1,290,815 |
| middlegame | -126 | e2e1 | 564,514 | 560,499 | 424.262 | 418.310 | 1,330,579 | 1,339,914 |
| evasion | -787 | d2d4 | 157,868 | 157,706 | 106.534 | 106.913 | 1,481,858 | 1,475,088 |

Totals: nodes 941,676 -> 937,499 (**-0.444%**); summed medians 698.122 -> 695.111 ms (**-0.431%**); aggregate throughput approximately 1,348,870 -> 1,348,704 NPS (**-0.012%**).

### Depth 6 fresh-JVM repeat, cold 4 MiB TT on

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| kiwipete | -220 | e2a6 | 167,760 | 167,760 | 141.975 | 138.898 | 1,181,615 | 1,207,793 |
| middlegame | -126 | e2e1 | 482,640 | 481,880 | 373.865 | 375.951 | 1,290,947 | 1,281,763 |
| evasion | -787 | d2d4 | 99,849 | 99,691 | 74.941 | 75.056 | 1,332,369 | 1,328,214 |

Totals: nodes 750,249 -> 749,331 (**-0.122%**); summed medians 590.781 -> 589.905 ms (**-0.148%**); aggregate throughput approximately 1,269,927 -> 1,270,257 NPS (**0.026%**).

### Verified common principal variations

The following lines were identical across A/B, TT off/on, and depth-6 repeat.

| Depth | Position | Common PV |
| ---: | --- | --- |
| 5 | start | e2e3 e7e5 d1f3 g8e7 f3f7 |
| 5 | kiwipete | d5e6 e7e6 e2a6 h3g2 f3f6 |
| 5 | tactical | e4d5 e6d5 d3g6 e8d8 g6c6 |
| 5 | evasion | c4c5 a3b4 c5b6 b2a1q d1a1 |
| 5 | middlegame | c3d5 e7d7 g5f6 g7f6 f3e5 |
| 5 | transposition-pawns | e1d2 e8d7 d2c3 d7d6 c3d4 |
| 6 | kiwipete | e2a6 b4c3 d2c3 e6d5 e5f7 h3g2 |
| 6 | middlegame | e2e1 h7h6 c3d5 c5f2 e1f2 f6d5 |
| 6 | evasion | d2d4 a3b4 a1b1 b6a7 h6f7 e8f7 |

## Evidence interpretation and recommendation

**Tree effect:** At depth 5, total nodes fall 6.737% without TT and 5.448% with TT.
Start improves 13.053% / 8.728%, and Middlegame 10.650% / 8.839%. Tactical adds
24 nodes, while Kiwipete, Evasion and Transposition-pawns are unchanged. At depth 6,
the benefit collapses to 0.444% / 0.122%: Kiwipete is unchanged, Middlegame saves
0.711% / 0.157%, and Evasion saves 0.103% / 0.158%. These counts reproduce exactly.

**Mechanics/throughput:** Extra resets, killer lookup and primitive promotions cost
some throughput in the initial measurements (aggregate -0.739% / -0.317% at depth 5,
-1.242% / -0.603% at depth 6). The depth-6 repeat is approximately neutral
(-0.012% / +0.026%). These are observed end-to-end throughputs; changed node mixes
and timing variability prevent attributing an exact percentage solely to lookup/
update mechanics. Ranking is experimental, not the future SR-017 implementation.

**Wall time:** Depth 5 gives credible 6.042% / 5.147% aggregate reductions within
this protocol. Initial depth-6 aggregates regress 0.808% / 0.483%; the fresh-JVM
repeat instead improves 0.431% / 0.148%. The deeper result has no reproducible
wall-time direction or material gain. Kiwipete acquires no tree benefit at either
depth. Evasion's tiny depth-6 node reduction does not produce a stable wall-time
improvement (TT-off +0.612% then +0.356%; TT-on -1.687% then +0.154%).
Thus the targeted earlier weak positions are not reliably repaired.

Recommendation: **KILLERS NOT JUSTIFIED** for replacing main history alone as the
provisional SR-016 baseline. This is a conservative adoption judgment: the
depth-5 improvement is real evidence in favour, but it is concentrated in two
positions and is not sustained in the bounded deeper confirmation. It is not
a claim that killers can never help. The repeated confirmation is sufficient to
retain the simpler baseline without requiring another experiment or tuning.
Candidate B remains selectable to preserve reproducibility.

No playing-strength conclusion follows from these fixed-depth measurements.
Unknowns remain across a larger corpus, deeper Search, other evaluators and JVM/
hardware conditions, and future generation/sorting mechanics. Cross-invocation/
iterative history reuse and other quiet-ordering mechanisms were not researched.

## Deliberately skipped and completion boundaries

Not run: full/long-running test suite; standalone SEE/perft suites (SEE, generation
and move application unchanged); GUI/UCI/production-driver integration; NNUE/BRN;
self-play, Elo/playing-strength matches; allocation/hardware-counter profiling;
depth >6, another confirmation repeat, parameter/count/aging/replacement tuning.
The targeted tests, existing harness semantic verifier and bytecode inspection are
the actual evidence; none of these skipped checks is claimed complete.

No countermoves, relative history, new continuation mechanisms, pruning/reductions,
PVS, staged generation, production adoption, canon/frontier maintenance or SR-017
framework was implemented. SR-016 remains PENDING. Normal validated-unit commit and
version finalization follow the repository workflow; nothing is pushed.

Human actions required after this prompt: None.
