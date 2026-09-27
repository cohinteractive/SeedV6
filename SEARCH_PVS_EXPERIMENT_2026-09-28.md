# SR-003 PVS / zero-window Search experiment — 2026-09-28

Starting commit: `ebe9e30`, clean worktree. Authoritative Contract **R012** and
Frontier **F007** verified before mutation. SR-014/015/016/017 are ACCEPTED;
production/default CONTROL is unchanged. No local AGENTS.md was found.
Root VERSION_STATE.txt was build 13; the verified maintained finalizer initialized
this unit before source mutation. CODEXLOG_CURRENT.md is absent, so no journal
file is created. Canon/frontier and SR-003 status remain unchanged.

## Recommendation

**Recommend exactly outcome 3: MIXED / CONDITIONAL.** Prefer the experimental PVS
traversal with the accepted staged/material/main-history ordering **when Search
TT is enabled in the tested cold 4 MiB fixed-depth HCE configuration**. Retain
ordered alpha-beta as the preferred TT-off traversal and independent reference.
This is an evidence boundary, not an automatic mode switch or a claim about
all TT sizes, warm iterative Search, other evaluators or every position.

TT-on aggregate wall time improves 11.482% at depth 5 and 7.504% at depth 6;
one fresh JVM per configuration confirms 12.054% and 8.083%. TT-off depth 5
improves 4.440% but depth 6 regresses 12.473% (fresh: -5.483% / +15.858%).
The repeat preserves all tree counts and the direction of every position's time
change. No bounded follow-up is needed to state this conditional conclusion.

This report recommends a research outcome; it does not accept the unit, update
SR-003, adopt PVS in production or authorize a tuning heuristic. Programme
reconciliation remains separate.

## Implementation and exact semantics

Main changes are `search/exact/ExactSearch.java`, `ExactSearchResult.java` and
`tools/search/ExactSearchHarness.java`, under the existing Java package root.
Test changes are `PvsSearchTest.java`, `PvsDiagnostics.java` and
`ExactSearchHarnessTest.java`. `tools/search-pvs-diagnostics.py` builds a temporary
instrumented copy under ignored build/. This report and version finalization
complete the unit. Board, Gen, SEE, ordering/history rules and TTable are unchanged.

The owner holds one new final primitive integer: `ORDERED_ALPHA_BETA=0`, `PVS=1`.
The six-argument constructor selects it; all previous constructors delegate to
ORDERED_ALPHA_BETA. The harness adds `--search=alpha-beta|pvs|both` (also
`alpha-beta,pvs`). All ordering/mechanics modes and old aliases remain available.
For isolated A/B traversal comparisons, select exactly one ordering/mechanics
combination. No production driver, UCI/GUI default or adoption path changes.

At both root and interior nodes, the first **actually searched** legal move gets
`[-beta,-alpha]`. Later PVS moves get `[-alpha-1,-alpha]`. Negate the child return:
`score <= alpha` continues; `score >= beta` cuts off directly; strictly between
alpha and beta triggers another call with `[-beta,-alpha]` before the value/PV is
accepted. Every call uses remaining depth minus one. Re-search reuses the same
child board, evaluator slot and single real-history push; it does not make the
move twice. Alpha-beta still uses its original full-window call on every move.
No shallow-depth, PV-node, rank or move-count suppression is added.

Windows are validated within [-32769,32769] with alpha < beta. Thus alpha is at
most 32768 and alpha+1 <= beta; neither addition nor negation approaches int
overflow. Mate score is 32768, maximum mate ply 256, mate threshold 32512, and
ordinary static scores stay within +/-32511. No clamping, mate-window tightening
or change to ply-based mate distance is introduced. Returns remain fail-soft.

TT identity, equal remaining depth, current generation, cutoff-only LOWER/UPPER
reuse, mate normalization, legal hash evidence, leaf exclusion and lifecycle
are unchanged. Each call captures its own original alpha/beta before searching.
Scout calls therefore store their own LOWER/UPPER bounds. A wider re-search
cannot use a non-cutting scout bound as EXACT. A completed scout entry may remain
when its later full re-search is cancelled; the incomplete re-search stores no
new completed evidence. Terminal/draw/cancellation precedence is unchanged.

PV replacement remains strict on improvement. A fail-low scout never replaces
the current discovered line, even when its upper bound raises the numeric
fail-low return. A completed full re-search supplies the improved principal
line. A scout beta cutoff exposes only its legal cutoff-move prefix, never an
allegedly exact continuation. Narrow-window results remain bounds and their
legal discovered prefixes do not promise an exact line realizing the numeric
bound. Full-window results are exact; existing applicable TT hits may truncate
their principal line. Every timed A/B pair here has identical best move and PV,
including depth 6; no equal-valued alternative required relaxing an assertion.

There is no new per-node allocation, move array, strategy object, interface
dispatch, TT metadata or production telemetry. Added hot-path state is primitive
control flow. Bytecode inspection of negamax found only its pre-existing
invalid-static-score exception allocation; no PvsDiagnostics reference exists
in the production class. This is source/bytecode evidence, not allocation or
hardware-counter profiling. Existing evaluator dispatch is unchanged.

Extra completed scout/re-search nodes learn history through exactly the existing
rules. This can change later ordering evidence. There is no history freeze,
rollback on successful re-search, double parent reward, or altered reset policy.
Deferred quiet history is still sampled when its phase materializes and held
fixed through that phase; checked nodes retain complete evasions.

## Correctness and window validation

The broad targeted run passed **143 tests** across ExactSearch, exact ordering,
staged generation/mechanics, history, TTable, TranspositionScores and the harness.
The final focused run passed **27 tests** (12 PVS, 15 harness), including two
later-added hash/interior-cancellation cases: **145 distinct tests** passed across
these runs. No full suite was requested or run.

- Twelve pre-edit depth-5 TT-off/on baseline tuples (score, best, PV, nodes) are
  asserted unchanged by the alpha-beta selector. PVS matches every score, best
  move and full PV, with independent TT-off exact verification of each PV prefix.
- 144 PVS/exhaustive-oracle combinations cover 24 fixture entries, depth 0–2,
  TT off/on: promotions and underpromotions, en passant, castling, complete
  evasions, mate/stalemate, quiet-only and tactical positions, and high branching.
  Best moves match alpha-beta; legal PV endpoints realize the oracle value.
- Board/history preservation, evaluator stack-slot reuse, owner reuse, exact
  history reset/reward, rule-50, repetition, insufficient material, terminal
  precedence, thread interruption and cancellation during scout/full re-search
  are checked. Incomplete roots and interrupted interior calls publish no
  completed score evidence; earlier completed scout UPPER evidence stays a bound.
- Legal hash moves across good/bad tactical and quiet classes are first and
  searched once. Inapplicable score depth still supplies legal ordering evidence.
- Extreme valid windows, unit windows, equality at alpha/beta, full root windows,
  both static-score limits and positive/negative mate-band fixtures are checked.
  Invalid int-extreme/inverted windows are rejected.
- Board-derived depth-two TT fixtures observe first-child EXACT, subsequent scout
  LOWER/UPPER, and UPPER followed by EXACT only after necessary wider re-search.
  Equal-depth/current-generation applicability, non-cutting bounds and zero-depth
  TT exclusion retain explicit tests.

The initial PVS test run had one fixture-assumption failure: Kiwipete at depth 3
did not produce a quiet cutoff. An explicit starting-position quiet-cutoff fixture
now asserts the reward/reset. No score/PV equivalence failure was observed.

An independently instrumented shadow class adds 12 interior-node fixtures,
six each with TT off/on. They use real legal moves and board-only leaf values.
The target is at ply 1, not merely a root wrapper with a narrow window:

| Interior window | Case | Full calls | Scouts | Fail lows | Scout cuts | Re-searches |
| --- | --- | --- | --- | --- | --- | --- |
| [-100,100] | later fail-low / tie | 1 | 19 | 19 | 0 | 0 |
| [5,100] | alpha equality | 1 | 19 | 19 | 0 | 0 |
| [-100,50] | beta equality | 1 | 1 | 0 | 1 | 0 |
| [-100,50] | fail-soft 83 overshoot | 1 | 1 | 0 | 1 | 0 |
| [-100,100] | three successive improvements | 4 | 19 | 16 | 0 | 3 |
| [0,1] | unit window | 1 | 1 | 0 | 1 | 0 |

Instrumentation asserts first-move caller windows, negamax transformations,
valid integer bounds and original-window TT classification at every observed
call/store. Every diagnostic run is compared with an uninstrumented run for
identical score, best move, PV and nodes. Counters are outside timing.

## Benchmark protocol

Windows 11 amd64; AMD Ryzen 5 5500 (6 cores / 12 logical processors); OpenJDK 21,
build 21+35-2513. HCE, one Search thread, `-Xms256m -Xmx256m -Xbatch`. Five warmups,
seven measured repetitions, alternating paired A/B and B/A rounds, median search
wall time. A is ORDERED_ALPHA_BETA; B is PVS. Both use
SEE_MATERIAL_QUIET_HISTORY plus STAGED_LAZY. Node counts include every admitted
root, terminal, static and repeated call.

Search timers include request setup/history reset and PV extraction but exclude
worker construction, FEN parsing, table clear and final result-record construction.
TT off means no Search TT;
TT on uses separate 4 MiB tables, cleared before every invocation. Each mode has
its own owner and empty initial history. Repeatability/value checks and independent
TT-off CONTROL searches of every returned PV prefix run outside the search timer.
Diagnostics do not participate in timed JVMs. Benchmarks run sequentially, without
concurrent tests or diagnostic campaigns.

The established six-position ordering corpus is unchanged. Its FEN definitions
remain in ExactSearchHarness.orderingPositions()/DefaultPerftPositionLibrary.
Favourable/ambiguous depth-5 evidence triggered the specified depth-6 Kiwipete,
Middlegame and Evasion confirmation. Modest aggregate effects triggered exactly
one fresh-JVM repeat of each depth/TT configuration. No deeper campaign was run.

## Depth-5 results

Times are milliseconds, NPS is nodes/second. Score/best are identical for A/B.
Scouts/re-searches/rates are B's untimed diagnostics; A has no scout/re-search calls.

### TT off

| Position | Score | Best | A nodes | B nodes | A ms | B ms | A NPS | B NPS | B scouts | B re-search | Rate % |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| start | 197 | e2e3 | 31418 | 32976 | 17.100 | 17.622 | 1837277 | 1871276 | 26003 | 75 | 0.2884 |
| kiwipete | 403 | d5e6 | 83934 | 86777 | 47.528 | 49.285 | 1765990 | 1760707 | 77211 | 24 | 0.0311 |
| tactical | 1177 | e4d5 | 4861 | 5941 | 1.449 | 1.743 | 3355190 | 3409469 | 4872 | 16 | 0.3284 |
| evasion | -81 | c4c5 | 20832 | 30597 | 11.742 | 16.672 | 1774204 | 1835210 | 21058 | 26 | 0.1235 |
| middlegame | 250 | c3d5 | 119270 | 94667 | 68.168 | 54.133 | 1749660 | 1748791 | 78247 | 30 | 0.0383 |
| transposition-pawns | 6 | e1d2 | 1563 | 1764 | 0.458 | 0.487 | 3413409 | 3625154 | 1218 | 7 | 0.5747 |

### TT on

| Position | Score | Best | A nodes | B nodes | A ms | B ms | A NPS | B NPS | B scouts | B re-search | Rate % |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| start | 197 | e2e3 | 27911 | 25818 | 16.039 | 14.644 | 1740228 | 1763103 | 21106 | 61 | 0.2890 |
| kiwipete | 403 | d5e6 | 65481 | 65433 | 39.091 | 38.776 | 1675078 | 1687474 | 58186 | 19 | 0.0327 |
| tactical | 1177 | e4d5 | 4280 | 4458 | 1.534 | 1.694 | 2790637 | 2631796 | 3690 | 15 | 0.4065 |
| evasion | -81 | c4c5 | 19925 | 19879 | 12.831 | 12.327 | 1552916 | 1612599 | 14169 | 23 | 0.1623 |
| middlegame | 250 | c3d5 | 103563 | 82396 | 63.202 | 50.006 | 1638595 | 1647709 | 68305 | 27 | 0.0395 |
| transposition-pawns | 6 | e1d2 | 1532 | 1501 | 0.469 | 0.428 | 3266524 | 3503734 | 1084 | 7 | 0.6458 |

### Preserved depth-5 principal variations

| Position | A = B, TT off = TT on |
| --- | --- |
| start | e2e3 e7e5 d1f3 g8e7 f3f7 |
| kiwipete | d5e6 e7e6 e2a6 h3g2 f3f6 |
| tactical | e4d5 e6d5 d3g6 e8d8 g6c6 |
| evasion | c4c5 a3b4 c5b6 b2a1q d1a1 |
| middlegame | c3d5 e7d7 g5f6 g7f6 f3e5 |
| transposition-pawns | e1d2 e8d7 d2c3 d7d6 c3d4 |

## Aggregate effects and fresh-JVM repeat

Percentages are B relative to A; negative wall time is faster. Throughput is total
nodes divided by summed position medians, not the mean of position NPS values.
Summed times below use the displayed millisecond precision; percentage calculations
come directly from the harness using unrounded elapsed nanoseconds.

| Depth | TT | Run | A nodes | B nodes | A sum ms | B sum ms | Nodes % | Wall % | Throughput % |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 5 | off | primary | 261878 | 252722 | 146.445 | 139.942 | -3.496 | -4.440 | 0.988 |
| 5 | off | fresh | 261878 | 252722 | 143.950 | 136.058 | -3.496 | -5.483 | 2.102 |
| 5 | on | primary | 222692 | 199485 | 133.166 | 117.875 | -10.421 | -11.482 | 1.199 |
| 5 | on | fresh | 222692 | 199485 | 129.549 | 113.933 | -10.421 | -12.054 | 1.857 |
| 6 | off | primary | 941408 | 1057632 | 512.290 | 576.186 | 12.346 | 12.473 | -0.113 |
| 6 | off | fresh | 941408 | 1057632 | 510.418 | 591.360 | 12.346 | 15.858 | -3.031 |
| 6 | on | primary | 750672 | 692146 | 446.113 | 412.638 | -7.796 | -7.504 | -0.317 |
| 6 | on | fresh | 750672 | 692146 | 441.833 | 406.120 | -7.796 | -8.083 | 0.312 |

## Depth-6 confirmation

Each row is the primary run. All best moves and entire PVs agree between A/B and
TT off/on; every prefix has an independently verified exact value.

| TT | Position | Score | Best | A nodes | B nodes | A ms | B ms | A NPS | B NPS | B scouts | B re-search | Rate % |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| off | kiwipete | -220 | e2a6 | 219294 | 198304 | 117.139 | 103.794 | 1872078 | 1910546 | 121359 | 15 | 0.0124 |
| off | middlegame | -126 | e2e1 | 564505 | 656137 | 308.744 | 360.749 | 1828390 | 1818820 | 540701 | 76 | 0.0141 |
| off | evasion | -787 | d2d4 | 157609 | 203191 | 86.407 | 111.643 | 1824038 | 1820001 | 181683 | 48 | 0.0264 |
| on | kiwipete | -220 | e2a6 | 167922 | 156417 | 96.655 | 91.288 | 1737326 | 1713450 | 96830 | 14 | 0.0145 |
| on | middlegame | -126 | e2e1 | 483069 | 438254 | 288.414 | 262.991 | 1674918 | 1666419 | 352649 | 57 | 0.0162 |
| on | evasion | -787 | d2d4 | 99681 | 97475 | 61.044 | 58.359 | 1632931 | 1670265 | 83051 | 33 | 0.0397 |

| Position | A = B depth-6 PV |
| --- | --- |
| kiwipete | e2a6 b4c3 d2c3 e6d5 e5f7 h3g2 |
| middlegame | e2e1 h7h6 c3d5 c5f2 e1f2 f6d5 |
| evasion | d2d4 a3b4 a1b1 b6a7 h6f7 e8f7 |

### Per-position fresh-JVM timing confirmation

Trees, scores, moves and PVs are identical to the primary run. No remeasurement
was used to select a preferred sample.

| Depth | TT | Position | A ms | B ms | A NPS | B NPS | Wall % |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 5 | off | start | 16.612 | 17.130 | 1891306 | 1925077 | 3.118 |
| 5 | off | kiwipete | 46.497 | 47.665 | 1805148 | 1820575 | 2.511 |
| 5 | off | tactical | 1.390 | 1.709 | 3496116 | 3475488 | 22.943 |
| 5 | off | evasion | 11.366 | 16.280 | 1832883 | 1879480 | 43.234 |
| 5 | off | middlegame | 67.652 | 52.791 | 1762998 | 1793254 | -21.967 |
| 5 | off | transposition-pawns | 0.433 | 0.483 | 3606368 | 3652173 | 11.444 |
| 5 | on | start | 16.033 | 14.104 | 1740857 | 1830544 | -12.031 |
| 5 | on | kiwipete | 37.415 | 37.377 | 1750145 | 1750631 | -0.101 |
| 5 | on | tactical | 1.318 | 1.382 | 3248330 | 3226226 | 4.872 |
| 5 | on | evasion | 12.068 | 11.783 | 1651046 | 1687062 | -2.361 |
| 5 | on | middlegame | 62.273 | 48.863 | 1663048 | 1686282 | -21.535 |
| 5 | on | transposition-pawns | 0.442 | 0.424 | 3469202 | 3544273 | -4.099 |
| 6 | off | kiwipete | 112.048 | 102.734 | 1957136 | 1930266 | -8.313 |
| 6 | off | middlegame | 303.273 | 358.545 | 1861375 | 1829996 | 18.225 |
| 6 | off | evasion | 95.097 | 130.081 | 1657349 | 1562032 | 36.788 |
| 6 | on | kiwipete | 96.346 | 92.025 | 1742911 | 1699732 | -4.485 |
| 6 | on | middlegame | 285.702 | 256.980 | 1690817 | 1705400 | -10.053 |
| 6 | on | evasion | 59.785 | 57.115 | 1667324 | 1706644 | -4.466 |

## Untimed diagnostic explanation

Full calls include the first move and necessary full re-searches, and may inherit
a unit caller window. Scouts count only the later-move PVS calls. Scout outcomes
partition exactly into fail-low, beta cutoff and re-search. Nodes equal
1 + full calls + scouts. First/later cutoffs count searched-move cutoffs, excluding
TT return-at-entry. Re-search nodes count entered nodes under at least one full
re-search, without double-counting nested re-searches; they are not all avoidable
overhead, since improved moves also require work in alpha-beta.

| Depth | TT | Position | Full calls | Scout lows | Scout cuts | First cuts | Later cuts | Multi-re-search nodes | Nodes in re-search | % nodes | Root re-search/scout |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 5 | off | start | 6972 | 25175 | 753 | 5061 | 753 | 17 | 15569 | 47.21 | 4/19 |
| 5 | off | kiwipete | 9565 | 77045 | 142 | 7082 | 142 | 3 | 12259 | 14.13 | 1/47 |
| 5 | off | tactical | 1068 | 4833 | 23 | 608 | 23 | 2 | 3189 | 53.68 | 2/23 |
| 5 | off | evasion | 9538 | 21012 | 20 | 8806 | 20 | 5 | 15157 | 49.54 | 2/5 |
| 5 | off | middlegame | 16419 | 77959 | 258 | 13361 | 258 | 7 | 19253 | 20.34 | 2/45 |
| 5 | off | transposition-pawns | 545 | 1204 | 7 | 394 | 7 | 2 | 388 | 22.00 | 1/8 |
| 5 | on | start | 4711 | 20371 | 674 | 3110 | 674 | 16 | 10274 | 39.79 | 4/19 |
| 5 | on | kiwipete | 7246 | 58033 | 134 | 5338 | 134 | 2 | 8231 | 12.58 | 1/47 |
| 5 | on | tactical | 767 | 3661 | 14 | 464 | 14 | 2 | 2227 | 49.96 | 2/23 |
| 5 | on | evasion | 5709 | 14129 | 17 | 5196 | 17 | 5 | 4809 | 24.19 | 2/5 |
| 5 | on | middlegame | 14090 | 68032 | 246 | 11490 | 246 | 7 | 12474 | 15.14 | 2/45 |
| 5 | on | transposition-pawns | 416 | 1070 | 7 | 282 | 7 | 2 | 156 | 10.39 | 1/8 |
| 6 | off | kiwipete | 76944 | 121062 | 282 | 72972 | 282 | 2 | 2106 | 1.06 | 0/47 |
| 6 | off | middlegame | 115435 | 539045 | 1580 | 97670 | 1580 | 14 | 228888 | 34.88 | 3/45 |
| 6 | off | evasion | 21507 | 181344 | 291 | 15393 | 291 | 16 | 73019 | 35.94 | 1/5 |
| 6 | on | kiwipete | 59586 | 96546 | 270 | 56359 | 270 | 2 | 874 | 0.56 | 0/47 |
| 6 | on | middlegame | 85604 | 351225 | 1367 | 73784 | 1367 | 12 | 72424 | 16.53 | 3/45 |
| 6 | on | evasion | 14423 | 82771 | 247 | 11569 | 247 | 10 | 18533 | 19.01 | 1/5 |

### Re-search rate by remaining depth (pooled positions, untimed)

| Nominal depth | TT | Remaining depth | Scouts | Re-searches | Rate % |
| --- | --- | --- | --- | --- | --- |
| 5 | off | 1 | 173109 | 78 | 0.0451 |
| 5 | off | 2 | 28503 | 52 | 0.1824 |
| 5 | off | 3 | 5922 | 22 | 0.3715 |
| 5 | off | 4 | 928 | 14 | 1.5086 |
| 5 | off | 5 | 147 | 12 | 8.1633 |
| 5 | on | 1 | 136036 | 65 | 0.0478 |
| 5 | on | 2 | 23459 | 42 | 0.1790 |
| 5 | on | 3 | 5971 | 18 | 0.3015 |
| 5 | on | 4 | 927 | 15 | 1.6181 |
| 5 | on | 5 | 147 | 12 | 8.1633 |
| 6 | off | 1 | 661052 | 54 | 0.0082 |
| 6 | off | 2 | 157694 | 40 | 0.0254 |
| 6 | off | 3 | 19563 | 26 | 0.1329 |
| 6 | off | 4 | 4797 | 9 | 0.1876 |
| 6 | off | 5 | 540 | 6 | 1.1111 |
| 6 | off | 6 | 97 | 4 | 4.1237 |
| 6 | on | 1 | 384334 | 41 | 0.0107 |
| 6 | on | 2 | 127326 | 28 | 0.0220 |
| 6 | on | 3 | 15475 | 19 | 0.1228 |
| 6 | on | 4 | 4758 | 6 | 0.1261 |
| 6 | on | 5 | 540 | 6 | 1.1111 |
| 6 | on | 6 | 97 | 4 | 4.1237 |

## Interpretation and future readiness

**Tree effect:** PVS is not an unconditional tree reduction. TT-off adds nodes in
five of six depth-5 positions; the aggregate win comes from Middlegame. At depth
6, Kiwipete improves but Middlegame and Evasion regress enough to reverse the
aggregate. TT-on saves 10.421% of nodes at depth 5 and 7.796% at depth 6. The
depth-5 Tactical fixture still adds 4.159% nodes and 0.160 ms in the primary run.

**Mechanical effect:** Aggregate NPS changes remain small compared with the
position-level tree changes. Primary throughput changes are +0.988%/+1.199% at
depth 5 (off/on), and -0.113%/-0.317% at depth 6. The fresh TT-off depth-6 result
is -3.031%, showing some timing variability, but its 12.346% extra nodes are
deterministic. These measurements do not isolate branch cost, TT cost or JIT
causality. PVS adds zero-window control and some repeated recursive calls; it
does not improve the underlying generator/evaluator mechanics.

**Final wall time:** TT-on's depth-5 and depth-6 aggregate wins survive the fresh
JVM and the depth-6 wins cover all three positions. TT-off's depth-6 regressions
also repeat. This supports the stated TT-enabled condition, not a universal PVS
replacement or a fitted position/depth suppression rule.

**Ordering dependence:** Global re-search rates are low, but they are dominated
by cheap near-leaf scouts and conceal expensive failures near the root. At
depth 6 Kiwipete needs no root or remaining-depth-5 re-search and saves work
with TT either off or on. Middlegame needs 3/45 root re-searches; Evasion 1/5.
Without TT, 228,888 Middlegame nodes (34.88%) and 73,019 Evasion nodes (35.94%)
lie within full re-searches. With TT, those counts are 72,424 (16.53%) and
18,533 (19.01%). At depth 5 Evasion needs 2/5 root re-searches and regresses
46.875% in nodes without TT, despite 99.9% of its scouts failing low globally.
Thus good average ordering is insufficient to guarantee TT-off benefit;
reuse under the unchanged exact TT rules materially supports these re-searches.
History effects and TT/hash ordering remain natural consequences of the changed
traversal. No independent causal ablation of those effects is claimed.

The tested zero-window transformations, bound classification, cancellation and
re-search/PV handling provide a correctness base for later **MTD(f)** research.
This does not validate an MTD(f) driver: repeated root passes, first guesses,
convergence, TT dependence and complete PV recovery remain separate questions.
For **LMR**, full-depth probe/re-search infrastructure is now exercised, but
reduced-depth evidence, eligibility, reduction size and selective correctness
are entirely untested. Aspiration and other narrow-window work can reuse these
fixtures, but widening policy and driver semantics remain unimplemented. No
qsearch, MTD(f), aspiration, LMR, ProbCut or other selectivity is introduced.
No playing-strength inference follows from these fixed-depth timings.

## Reproduction and limits

```text
gradlew.bat :app:test --tests "*search.exact.*" --tests "*search.tt.TTableTest" --tests "*search.tt.TranspositionScoresTest" --tests "*tools.search.ExactSearchHarnessTest" -Pheadless --console=plain
gradlew.bat :app:compileTestJava --console=plain
java -Xms256m -Xmx256m -Xbatch -cp app/build/classes/java/main com.ohinteractive.seedv6.tools.search.ExactSearchHarness --position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history --mechanics=staged-lazy --search=both --tt=off
```

Repeat with `--tt=on`; for depth 6 use
`--position=kiwipete,middlegame,evasion --depth=6`. One fresh invocation per
configuration reproduces the confirmation protocol.

```text
python -B tools/search-pvs-diagnostics.py
python -B tools/search-pvs-diagnostics.py --depth=6 --position=kiwipete,middlegame,evasion
```

Local raw outputs are under ignored `build/sr003-*.txt`: eight timing runs,
depth-5/depth-6 diagnostics, targeted/focused tests and bytecode inspection.
The tables here are the durable evidence; checked-in harness/tests/diagnostic
script reproduce it without requiring those local files.

Deliberately skipped: full/long-running suite, self-play/strength matches, GUI
or browser campaigns, NNUE/BRN campaigns, deeper timing campaigns, warm/iterative
TT studies, other TT sizes, allocation/JIT/hardware profiling, and all excluded
Search-policy changes. Existing small neural stack tests in the affected exact
suite ran, but this is not neural performance/adoption evidence.

Limits: one host/JDK, six depth-5 and three depth-6 positions, HCE, cold 4 MiB TT
and per-invocation history reset. Small sub-millisecond measurements and modest
throughput differences should not be generalized. Tests establish the exercised
exactness boundary, not a formal proof for all chess states or future evaluators.
No remaining correctness failure, mandatory validation gap or further experiment
blocks this research unit. Final verified version/Git state is reported in the
completion; no push or deployment is authorized or performed.

Human actions required after this prompt: None. Acceptance/reconciliation and
canon/frontier maintenance remain a separate future programme decision.
