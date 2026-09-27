# SR-016 immediate continuation-history experiment - 2026-09-27

Starting revision: `79670d45185b3e0a16e5f6b278035e92dd65a216`, clean worktree. Reverified authoritative Search Contract **R009**, Research Frontier **F004**, SR-015 ACCEPTED and SR-016 PENDING. Their contents are unchanged from the sources read in the preceding main-history unit. No repository-local AGENTS.md is present at the root, in the checked parent directories or under the repository; the supplied shared governance and Search sources apply. VERSION_STATE.txt activates maintained version finalization. Root CODEXLOG_CURRENT.md is absent and is not created.

## Research outcome

**Recommend outcome 2: CONTINUATION HISTORY NOT JUSTIFIED.** Main quiet history alone remains the provisional SR-016 baseline. The immediate continuation candidate remains independently selectable as experimental evidence. This recommendation does not mark SR-016 ACCEPTED, IMPLEMENTED or REJECTED; Contract/Frontier remain R009/F004.

At depth 5 the continuation candidate reduces total nodes by only 0.619% TT-off and 0.338% TT-on, with mixed wall time: 1.161% slower off, 2.275% faster on. The modest and mixed differences warranted the prescribed depth-6 confirmation and one fresh-JVM repeat of that set. Both confirmation batches favour main history alone on aggregate wall time: continuation is 3.761% / 7.418% slower TT-off and 6.602% / 4.586% slower TT-on. The deterministic depth-6 node reductions are only 0.595% off and 0.211% on.

Kiwipete gains no node reduction at either depth and is slower in every depth-6 comparison. Evasion has unchanged nodes at depth 5 and slightly more nodes at depth 6. Its depth-6 wall time regresses in three comparisons and is effectively tied in the fourth (-0.161% TT-on repeat). There is no reliable contextual rescue of the positions where main history previously incurred costs.

This bounded result does not require parameter tuning or another continuation experiment before considering independent SR-016 candidates. It does not establish that continuation history is universally unhelpful, nor does it imply playing strength.

## Implementation and files

- `app/src/main/java/com/ohinteractive/seedv6/search/exact/ContinuationHistory.java`: compact piece mapping, resulting-piece context, dense primitive index, searched-prefix updates and short storage.
- `app/src/main/java/com/ohinteractive/seedv6/search/exact/ExactSearch.java`: distinct mode, owner table, reset/abort clearing, immediate previous-move argument, equal-weight quiet-score addition and continuation cutoff updates.
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/ExactSearchHarness.java`: single and paired mode selectors, help/output name; existing methodology unchanged.
- `app/src/test/java/com/ohinteractive/seedv6/search/exact/ContinuationHistoryTest.java`: ten focused primitive, path, update, ordering, lifecycle and semantic tests.
- `app/src/test/java/com/ohinteractive/seedv6/search/exact/ExactSearchOrderingTest.java`: candidate in the existing all-mode matrix; additional recorded six-position depth-5 main-history visitation checks.
- `app/src/test/java/com/ohinteractive/seedv6/search/exact/QuietHistoryTest.java` and `CaptureHistoryTest.java`: reflection helpers pass the explicit absent-context argument; all assertions retained.
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/ExactSearchHarnessTest.java`: independent selection and paired repeatability.
- This evidence file and required root VERSION_STATE.txt finalization.

New mode: `see-material-continuation-history`, Java constant `ExactSearch.SEE_MATERIAL_CONTINUATION_HISTORY`. It includes both main and immediate continuation history. Pair: `--ordering=see-material-quiet-history,see-material-continuation-history`.

CONTROL remains production/default. Existing mode constants and aliases retain their meanings, including `see-material-quiet-history`, `see-material-history` and `both`. The candidate has no capture-history table. Main-history arithmetic, lifecycle and policy remain unchanged; its six-position depth-5 node counts exactly match commit 79670d4 under both TT settings. Older deterministic visitation tests remain intact.

## Representation, context and footprint

Inspected actual `Piece`, `Board`, `Move`, `Gen.addPromotions` and recursion mechanics. Moving-piece bits are 16..19; promoted-piece bits are 12..15; destination bits are 6..11; source bits are 0..5. Both moving and promoted fields include side. Verified valid codes are white 1..6 and black 9..14, with side shift 3.

The branch-free compact mapping is:

```text
compact(piece) = (piece & 7) - 1 + 6 * (piece >>> 3)
white 1..6 -> 0..5; black 9..14 -> 6..11
```

It uses a mask, shift, multiply by six, subtraction and addition; no mapping array. The previous resulting piece is the nonzero promoted field, otherwise the moving field. Board-after-move tests independently verify this for both colours, captures, EP, castling and all promotion choices.

The exact flattened index is:

```text
(((compact(previousResultingPiece) * 64 + previousTo) * 12
    + compact(currentMovingPiece)) * 64 + currentTo)
```

There are **12 * 64 * 12 * 64 = 589,824 entries** in one owner-local **short[]**, payload **1,179,648 bytes = 1,152 KiB = 1.125 MiB**, plus JVM array overhead. No slots are wasted on unused encoded piece values. Short storage halves payload and reset bytes relative to the equivalent int array; its separate performance benefit was not benchmarked. Both side dimensions remain explicit. Some combinations are unreachable in ordinary alternating legal play.

With the unchanged 256 KiB main table, the two history payloads total **1,441,792 bytes / 1.375 MiB**. Every older mode has a null continuation table.

The recursive negamax call carries one extra primitive `long previousMove`. Root passes zero, even if GameHistory contains earlier game moves. Each child receives the actual move just searched, never a grandparent move, TT hint or PV entry. This avoids new path arrays or context objects. Root's derived context is -1; no table sentinel row is allocated.

For the candidate, an unresolved positive-depth node computes the previous-piece/destination row base once, after terminal/draw/static/TT resolution and before ordering. Current-piece/destination mapping occurs for quiet lookups/updates. Previous captures, EP, promotions and quiets all supply valid context. No context is derived from evaluator output, score, TT state or ply.

## Lifecycle, updates and ranking

The continuation table is confined to one ExactSearch owner and shared by nodes only within that fixed-depth invocation. Both main and continuation tables clear at the start of every valid invocation, including successive calls inside one beginRequest lifetime, static searches and immediate cancellation. Both clear after Aborted/incomplete invocations. No cross-owner, cross-invocation or iterative-deepening history persistence is introduced.

At a searched quiet beta cutoff, the existing completion/cancellation checkpoint precedes both updates. Main history keeps its existing reward and searched-quiet-prefix maluses. With a valid context, continuation applies the same reward and maluses to the same prefix. Its helper returns immediately at root or for a tactical winner. Tactical moves, including quiet promotions, never learn in either quiet history; generated but unsearched moves do not update. A successful quiet hash gets the ordinary reward; an earlier failed quiet hash gets the ordinary malus. TT score cutoffs do not count as searched quiet evidence.

The previous context is deliberately compact: different source squares may share a resulting-piece/destination identity, and different current source squares share a current-piece/destination identity. Updates to an aliased continuation bucket apply sequentially, winner first then failed quiet prefix, as specified. Main history still distinguishes current source.

For remaining depth d:

```text
magnitude = min(d, 64)^2
bonus = +magnitude for winner; -magnitude for earlier searched quiets
bonus = clamp(bonus, -16384, 16384)
h = h + bonus - h * abs(bonus) / 16384
```

The short value sign-extends to int; gravity is computed in int before narrowing to short. Search bonuses are 1..4096; the full clamped product is at most 268,435,456 and cannot overflow int. Results stay within +/-16384, representable in short. Java division truncates toward zero. Bounds, cap and equal weighting were not tuned.

Quiet score is `mainHistory + continuationHistory`, or main history alone at root. The sum is computed in int and lies within [-32768, 32768]. Strict less-than insertion retains original deterministic relative order for equal sums, including ties formed from different opposing components. The existing primitive quiet insertion pass and score scratch are reused.

Legal hash precedence remains unconditional and independent of history. SEE >= 0 good tacticals and SEE < 0 bad tacticals remain ahead of quiets, each ranked by unchanged descending immediate material. Full Gen.genAll remains. No numeric SEE magnitude, new tactical score, pruning, reduction, PVS, previous PV, staged generation or SR-017 sorting decision is introduced.

Source and compiled-bytecode inspection (`javap -c -p`) show direct primitive loads/stores, sign extension and bounded integer arithmetic; no added per-node allocation, boxing, collections, streams, comparators or strategy dispatch. Table allocation occurs only at owner construction. Existing root/result allocations remain. This is static/bytecode evidence, not allocation profiling or JIT assembly verification.

## Validation

Final targeted run: **139 tests in 15 suites; zero failures, errors or skips; BUILD SUCCESSFUL in 6 seconds**.

```powershell
.\gradlew.bat :app:test --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.search.tt.*' --tests 'com.ohinteractive.seedv6.search.order.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' -Pheadless=true --console=plain
```

Initial attempt stopped at test compilation: the new fixture called nonexistent Board.toFen. It was corrected to use the established starting-position FEN from ExactSearchHarness. The subsequent complete targeted run passed. There was no failed exact-score or move-uniqueness assertion, and no production correction after measurement began.

Coverage:

- All 589,824 combinations of both compact piece/side and destination dimensions, omitted source identity, exact footprint and absent root context.
- Independent post-move board verification of resulting-piece context, including sixteen promotion variants across both colours, ordinary captures/quiets, EP and castling.
- Initial zero state, positive and negative gravity, reversal, extreme bonus clamping, repeated bounds/saturation and depth cap.
- Root learns main history only, including a root supplied with prior GameHistory.
- Actual child cutoffs after quiet moves, captures and white/black promotions, with TT off/on and quiet hash reward/malus. At the first learned context, the test verifies the complete main and continuation tables against only the actually searched quiet prefix, then aborts and verifies both clear.
- Primitive tactical exclusion, including quiet/capture promotions and EP; unsearched quiet exclusion; castling remains quiet.
- Equal-weight score ordering, opposing-component equal-sum stability, no-context main-only order, all legal hash hints and an illegal hint, complete unique move visitation and unchanged tactical order, including evasion fixtures.
- Independent owners, poisoned-table reset, identical final history tables/node counts/best/PV across repeated invocations within one request, static/immediate-cancellation resets and thread-interruption preservation.
- Cancellation raised by a child is observed before either table updates; abort results retain invalid score/no move/no PV and do not publish root TT evidence.
- **672 exhaustive score/optimal-best/PV comparisons** in the established 14-fixture, depths 0..2, eight-mode, TT-off/on matrix. This includes mate/stalemate, rule-50, insufficient material, EP and promotions; all-mode repetition and final-leaf cancellation tests remain.
- **Eight additional depth-3 exhaustive value/optimal-best/PV comparisons** for white/black promotion paths, castling and evasions.
- Recorded depth-5 main-history nodes across all six benchmark positions and both TT settings; historical CONTROL/SEE_TIERED/SEE_TACTICAL/SEE_MATERIAL visitation assertions unchanged.
- Existing exact/TT evidence, score normalization, generation/depth qualification and history-sensitive identity tests; material accounting retains 33 generated tactical comparisons and 19 explicit special values.
- All six benchmark runs below pass per-round exact-score equality and within-mode deterministic score/best/PV/nodes, plus untimed legal optimal-best/PV realization checks through TT-off CONTROL.

Static review includes complete source diffs, bytecode allocation/call sites, whitespace checks and protected-source comparison. QuietHistory.java, CaptureHistory.java, See.java, Contract, Frontier and earlier research reports remain unchanged.

Deliberately skipped: full/long-running suites, standalone SEE tests (SEE and tactical classification are unchanged), production driver/GUI/browser/UCI lifecycle suites, NNUE/BRN performance campaigns, playing-strength games, larger/deeper corpus, separate int-versus-short timing, allocation profiling/JIT assembly, coefficient/key tuning, history persistence, deeper continuation contexts and other SR-016/SR-017 mechanisms. Existing exact TT tests retain their evaluator-equivalence fixtures. This headless Search task requires no browser verification.

## Benchmark methodology

Unchanged six-position depth-5 ordering corpus: Start, Kiwipete, Tactical, Evasion, Middlegame and Transposition-pawns. Depth 6 uses exactly Kiwipete, Middlegame and Evasion. The mixed/modest depth-5 outcome warranted depth 6; modest timing differences warranted one fresh-JVM repeat of the depth-6 confirmation batch.

Windows 11 amd64; AMD Ryzen 5 5500; OpenJDK 21 build 21+35-2513; HCE; one Search thread; `-Xms256m -Xmx256m -Xbatch`. Five warmups and seven measured repetitions per position/mode. The two mode owners alternate execution order each round. Each main/continuation table begins empty for every warmup and measured invocation. TT-on uses separate cold 4 MiB requested tables, cleared before every Search outside timing.

Median is the fourth of seven sorted elapsed samples. Timing includes fixed-depth setup and both history resets; excludes owner construction, FEN parsing, TT clearing and semantic verification. Each reported NPS belongs to the same median sample. Aggregate throughput is total nodes divided by summed position median times, not an average of per-position NPS.

Runs were sequential, with no other validation/benchmark work launched by this agent concurrently. All six runs succeeded: Gradle elapsed 8s, 8s, 22s, 22s, 23s and 20s. Total **336 measured searches and 240 warmups**, plus untimed semantic verification. No code or parameter changed between runs. Node counts repeat exactly. Host/JVM timing varies, visibly including Kiwipete TT-on between batches; paired comparisons and the fresh-JVM repeat support the conclusion without assuming isolated instruction costs.

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history,see-material-continuation-history --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history,see-material-continuation-history --tt=on' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history,see-material-continuation-history --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history,see-material-continuation-history --tt=on' --console=plain
# Repeat the two depth-6 commands once in fresh JVMs.
```

M / C below means main history / main plus immediate continuation. Scores and best moves are identical across the two modes. At depth 5 Transposition-pawns has different legal equal-valued PVs; both are preserved in the raw evidence and passed the untimed verification.

## Depth 5, TT off

| Position | Depth | Score | Best | Nodes M / C | Median ms M / C | NPS M / C |
|---|---:|---:|---|---:|---:|---:|
| start | 5 | 197 | e2e3 | 31,418 / 29,756 | 18.200 / 18.678 | 1,726,311 / 1,593,146 |
| kiwipete | 5 | 403 | d5e6 | 83,934 / 83,934 | 60.641 / 61.313 | 1,384,113 / 1,368,949 |
| tactical | 5 | 1177 | e4d5 | 4,861 / 4,861 | 1.435 / 1.489 | 3,387,692 / 3,264,826 |
| evasion | 5 | -81 | c4c5 | 20,741 / 20,741 | 16.502 / 16.734 | 1,256,900 / 1,239,445 |
| middlegame | 5 | 250 | c3d5 | 141,150 / 141,150 | 103.906 / 104.793 | 1,358,440 / 1,346,944 |
| transposition-pawns | 5 | 6 | e1d2 | 1,563 / 1,470 | 0.380 / 0.390 | 4,117,492 / 3,765,368 |

## Depth 5, cold 4 MiB TT on

| Position | Depth | Score | Best | Nodes M / C | Median ms M / C | NPS M / C |
|---|---:|---:|---|---:|---:|---:|
| start | 5 | 197 | e2e3 | 27,911 / 27,196 | 19.064 / 17.838 | 1,464,037 / 1,524,601 |
| kiwipete | 5 | 403 | d5e6 | 65,515 / 65,515 | 53.756 / 52.317 | 1,218,756 / 1,252,276 |
| tactical | 5 | 1177 | e4d5 | 4,280 / 4,280 | 1.340 / 1.450 | 3,194,506 / 2,951,317 |
| evasion | 5 | -81 | c4c5 | 19,834 / 19,834 | 17.980 / 17.752 | 1,103,102 / 1,117,301 |
| middlegame | 5 | 250 | c3d5 | 120,175 / 120,175 | 99.576 / 97.967 | 1,206,870 / 1,226,686 |
| transposition-pawns | 5 | 6 | e1d2 | 1,532 / 1,438 | 0.383 / 0.403 | 4,005,228 / 3,564,700 |

## Depth 6 confirmation, TT off

| Position | Depth | Score | Best | Nodes M / C | Median ms M / C | NPS M / C |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 219,294 / 219,294 | 192.731 / 199.378 | 1,137,824 / 1,099,892 |
| middlegame | 6 | -126 | e2e1 | 564,514 / 558,849 | 452.642 / 470.448 | 1,247,152 / 1,187,908 |
| evasion | 6 | -787 | d2d4 | 157,868 / 157,931 | 113.615 / 117.709 | 1,389,495 / 1,341,707 |

## Depth 6 confirmation, cold 4 MiB TT on

| Position | Depth | Score | Best | Nodes M / C | Median ms M / C | NPS M / C |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 167,760 / 167,760 | 212.370 / 233.256 | 789,940 / 719,210 |
| middlegame | 6 | -126 | e2e1 | 482,640 / 480,993 | 410.242 / 429.746 | 1,176,476 / 1,119,248 |
| evasion | 6 | -787 | d2d4 | 99,849 / 99,914 | 79.463 / 85.427 | 1,256,550 / 1,169,580 |

## Depth 6 fresh-JVM repeat, TT off

| Position | Depth | Score | Best | Nodes M / C | Median ms M / C | NPS M / C |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 219,294 / 219,294 | 189.832 / 197.939 | 1,155,199 / 1,107,889 |
| middlegame | 6 | -126 | e2e1 | 564,514 / 558,849 | 449.661 / 490.104 | 1,255,420 / 1,140,267 |
| evasion | 6 | -787 | d2d4 | 157,868 / 157,931 | 135.360 / 144.294 | 1,166,281 / 1,094,512 |

## Depth 6 fresh-JVM repeat, cold 4 MiB TT on

| Position | Depth | Score | Best | Nodes M / C | Median ms M / C | NPS M / C |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 167,760 / 167,760 | 153.067 / 167.249 | 1,095,992 / 1,003,057 |
| middlegame | 6 | -126 | e2e1 | 482,640 / 480,993 | 413.412 / 429.183 | 1,167,456 / 1,120,719 |
| evasion | 6 | -787 | d2d4 | 99,849 / 99,914 | 83.676 / 83.541 | 1,193,285 / 1,195,990 |

## Aggregate comparison

Summed milliseconds use the displayed position medians (rounding uncertainty <=0.003 ms at depth 5 and <=0.0015 ms at depth 6). Approximate aggregate NPS rounds to the nearest 1,000. Percentage changes use the harness's unrounded calculations. Positive wall change is slower; negative throughput is fewer nodes per second.

| Set | Total nodes M / C | Node change | Summed median ms M / C | Wall change | Approx aggregate NPS M / C | Throughput change |
|---|---:|---:|---:|---:|---:|---:|
| 5 / TT off | 283,667 / 281,912 | -0.619% | 201.064 / 203.397 | 1.161% | 1,411,000 / 1,386,000 | -1.759% |
| 5 / TT on | 239,247 / 238,438 | -0.338% | 192.099 / 187.727 | -2.275% | 1,245,000 / 1,270,000 | 1.982% |
| 6 / TT off | 941,676 / 936,074 | -0.595% | 758.988 / 787.535 | 3.761% | 1,241,000 / 1,189,000 | -4.198% |
| 6 / TT on | 750,249 / 748,667 | -0.211% | 702.075 / 748.429 | 6.602% | 1,069,000 / 1,000,000 | -6.391% |
| 6 repeat / TT off | 941,676 / 936,074 | -0.595% | 774.853 / 832.337 | 7.418% | 1,215,000 / 1,125,000 | -7.460% |
| 6 repeat / TT on | 750,249 / 748,667 | -0.211% | 650.155 / 679.973 | 4.586% | 1,154,000 / 1,101,000 | -4.587% |

## Interpretation and boundaries

1. **Tree effect:** deterministic node reductions are below 0.7% for every aggregate. At depth 5 only Start and Transposition-pawns improve nodes; Kiwipete, Tactical, Evasion and Middlegame are identical. At depth 6 Kiwipete stays identical, Middlegame saves 1.004% TT-off / 0.341% TT-on, and Evasion grows 0.040% / 0.065%. The extra context supplies little marginal ordering value on these fixed-depth trees.
2. **Throughput:** continuation adds a 1.125 MiB table/reset, context derivation, quiet lookups and a second searched-prefix update pass. Depth-6 aggregate throughput falls 4.198-7.460% in the four comparisons. Throughput also includes node-mix and JVM/cache effects; these figures are not an isolated measurement of a specific operation. The favourable depth-5 TT-on throughput sample is retained, not discarded.
3. **Wall time:** small node savings do not pay the observed confirmation cost. Both TT settings regress in both confirmation batches. The depth-5 TT-on gain alone is insufficient evidence to replace the simpler main-history baseline.

For Kiwipete, depth-6 wall regressions are 3.449% / 4.270% off and 9.834% / 9.265% on, with no node savings. For Evasion they are 3.603% / 6.600% off and 7.506% / -0.161% on, with slightly more nodes in every case. Continuation does not establish a repeatable improvement for either priority position.

No coefficient tuning, deeper contexts or extra research sweep is performed in response to the weak result. Killers, countermoves, relative history and other contextual histories remain independent future decisions. No SR-017 sorting architecture is chosen; experimental insertion costs do not predict a future handcrafted Sort's costs.

Remaining unknowns: larger corpora and other depths, neural evaluators, other hosts/JVMs, persistent or iterative-deepening reuse, and actual playing strength. Fixed-depth wall time is not a direct playing-strength measure. This recommendation is bounded to the specified main-plus-immediate-continuation experiment.

The starting tree was clean. All implementation, tests and this report are attributable to this work unit. Version-finalizer results and the final local commit/status are reported in the completion response after observation. No push is performed. No programme acceptance or canon/frontier maintenance is inferred from completion.

Human actions required after this prompt: None. GPT/user may separately choose the next SR-016 comparison; no setup or Project Source refresh is required by this unchanged-canon experiment.

## Captured harness evidence

### Depth 5, TT off

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=5 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=off table=none
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=start requested=5 completed=5 best=e2e3 score=197 pv=[e2e3 e7e5 d1f3 g8e7 f3f7] nodes=31418 median_ms=18.200 nps=1726311 ordering=SEE_MATERIAL_QUIET_HISTORY
position=start requested=5 completed=5 best=e2e3 score=197 pv=[e2e3 e7e5 d1f3 g8e7 f3f7] nodes=29756 median_ms=18.678 nps=1593146 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=start depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-5.290 wall_pct=2.626 throughput_pct=-7.714
position=kiwipete requested=5 completed=5 best=d5e6 score=403 pv=[d5e6 e7e6 e2a6 h3g2 f3f6] nodes=83934 median_ms=60.641 nps=1384113 ordering=SEE_MATERIAL_QUIET_HISTORY
position=kiwipete requested=5 completed=5 best=d5e6 score=403 pv=[d5e6 e7e6 e2a6 h3g2 f3f6] nodes=83934 median_ms=61.313 nps=1368949 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=kiwipete depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=1.108 throughput_pct=-1.096
position=tactical requested=5 completed=5 best=e4d5 score=1177 pv=[e4d5 e6d5 d3g6 e8d8 g6c6] nodes=4861 median_ms=1.435 nps=3387692 ordering=SEE_MATERIAL_QUIET_HISTORY
position=tactical requested=5 completed=5 best=e4d5 score=1177 pv=[e4d5 e6d5 d3g6 e8d8 g6c6] nodes=4861 median_ms=1.489 nps=3264826 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=tactical depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=3.763 throughput_pct=-3.627
position=evasion requested=5 completed=5 best=c4c5 score=-81 pv=[c4c5 a3b4 c5b6 b2a1q d1a1] nodes=20741 median_ms=16.502 nps=1256900 ordering=SEE_MATERIAL_QUIET_HISTORY
position=evasion requested=5 completed=5 best=c4c5 score=-81 pv=[c4c5 a3b4 c5b6 b2a1q d1a1] nodes=20741 median_ms=16.734 nps=1239445 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=evasion depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=1.408 throughput_pct=-1.389
position=middlegame requested=5 completed=5 best=c3d5 score=250 pv=[c3d5 e7d7 g5f6 g7f6 f3e5] nodes=141150 median_ms=103.906 nps=1358440 ordering=SEE_MATERIAL_QUIET_HISTORY
position=middlegame requested=5 completed=5 best=c3d5 score=250 pv=[c3d5 e7d7 g5f6 g7f6 f3e5] nodes=141150 median_ms=104.793 nps=1346944 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=middlegame depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=0.853 throughput_pct=-0.846
position=transposition-pawns requested=5 completed=5 best=e1d2 score=6 pv=[e1d2 e8d7 d2c3 d7d6 c3d4] nodes=1563 median_ms=0.380 nps=4117492 ordering=SEE_MATERIAL_QUIET_HISTORY
position=transposition-pawns requested=5 completed=5 best=e1d2 score=6 pv=[e1d2 e8d7 d2d3 d7d6 d3d4] nodes=1470 median_ms=0.390 nps=3765368 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=transposition-pawns depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-5.950 wall_pct=2.845 throughput_pct=-8.552
comparison aggregate depth=5 positions=6 statistic=sum-of-position-medians baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-0.619 wall_pct=1.161 throughput_pct=-1.759
```

### Depth 5, cold 4 MiB TT on

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=5 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=on table=cold/cleared before each request; explicit harness 4 MiB
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=start requested=5 completed=5 best=e2e3 score=197 pv=[e2e3 e7e5 d1f3 g8e7 f3f7] nodes=27911 median_ms=19.064 nps=1464037 ordering=SEE_MATERIAL_QUIET_HISTORY
position=start requested=5 completed=5 best=e2e3 score=197 pv=[e2e3 e7e5 d1f3 g8e7 f3f7] nodes=27196 median_ms=17.838 nps=1524601 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=start depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-2.562 wall_pct=-6.432 throughput_pct=4.137
position=kiwipete requested=5 completed=5 best=d5e6 score=403 pv=[d5e6 e7e6 e2a6 h3g2 f3f6] nodes=65515 median_ms=53.756 nps=1218756 ordering=SEE_MATERIAL_QUIET_HISTORY
position=kiwipete requested=5 completed=5 best=d5e6 score=403 pv=[d5e6 e7e6 e2a6 h3g2 f3f6] nodes=65515 median_ms=52.317 nps=1252276 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=kiwipete depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=-2.677 throughput_pct=2.750
position=tactical requested=5 completed=5 best=e4d5 score=1177 pv=[e4d5 e6d5 d3g6 e8d8 g6c6] nodes=4280 median_ms=1.340 nps=3194506 ordering=SEE_MATERIAL_QUIET_HISTORY
position=tactical requested=5 completed=5 best=e4d5 score=1177 pv=[e4d5 e6d5 d3g6 e8d8 g6c6] nodes=4280 median_ms=1.450 nps=2951317 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=tactical depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=8.240 throughput_pct=-7.613
position=evasion requested=5 completed=5 best=c4c5 score=-81 pv=[c4c5 a3b4 c5b6 b2a1q d1a1] nodes=19834 median_ms=17.980 nps=1103102 ordering=SEE_MATERIAL_QUIET_HISTORY
position=evasion requested=5 completed=5 best=c4c5 score=-81 pv=[c4c5 a3b4 c5b6 b2a1q d1a1] nodes=19834 median_ms=17.752 nps=1117301 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=evasion depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=-1.271 throughput_pct=1.287
position=middlegame requested=5 completed=5 best=c3d5 score=250 pv=[c3d5 e7d7 g5f6 g7f6 f3e5] nodes=120175 median_ms=99.576 nps=1206870 ordering=SEE_MATERIAL_QUIET_HISTORY
position=middlegame requested=5 completed=5 best=c3d5 score=250 pv=[c3d5 e7d7 g5f6 g7f6 f3e5] nodes=120175 median_ms=97.967 nps=1226686 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=middlegame depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=-1.615 throughput_pct=1.642
position=transposition-pawns requested=5 completed=5 best=e1d2 score=6 pv=[e1d2 e8d7 d2c3 d7d6 c3d4] nodes=1532 median_ms=0.383 nps=4005228 ordering=SEE_MATERIAL_QUIET_HISTORY
position=transposition-pawns requested=5 completed=5 best=e1d2 score=6 pv=[e1d2 e8d7 d2d3 d7d6 d3d4] nodes=1438 median_ms=0.403 nps=3564700 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=transposition-pawns depth=5 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-6.136 wall_pct=5.464 throughput_pct=-10.999
comparison aggregate depth=5 positions=6 statistic=sum-of-position-medians baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-0.338 wall_pct=-2.275 throughput_pct=1.982
```

### Depth 6 confirmation, TT off

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=6 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=off table=none
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=219294 median_ms=192.731 nps=1137824 ordering=SEE_MATERIAL_QUIET_HISTORY
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=219294 median_ms=199.378 nps=1099892 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=kiwipete depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=3.449 throughput_pct=-3.334
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=564514 median_ms=452.642 nps=1247152 ordering=SEE_MATERIAL_QUIET_HISTORY
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=558849 median_ms=470.448 nps=1187908 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=middlegame depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-1.004 wall_pct=3.934 throughput_pct=-4.750
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=157868 median_ms=113.615 nps=1389495 ordering=SEE_MATERIAL_QUIET_HISTORY
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=157931 median_ms=117.709 nps=1341707 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=evasion depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.040 wall_pct=3.603 throughput_pct=-3.439
comparison aggregate depth=6 positions=3 statistic=sum-of-position-medians baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-0.595 wall_pct=3.761 throughput_pct=-4.198
```

### Depth 6 confirmation, cold 4 MiB TT on

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=6 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=on table=cold/cleared before each request; explicit harness 4 MiB
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=167760 median_ms=212.370 nps=789940 ordering=SEE_MATERIAL_QUIET_HISTORY
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=167760 median_ms=233.256 nps=719210 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=kiwipete depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=9.834 throughput_pct=-8.954
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=482640 median_ms=410.242 nps=1176476 ordering=SEE_MATERIAL_QUIET_HISTORY
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=480993 median_ms=429.746 nps=1119248 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=middlegame depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-0.341 wall_pct=4.754 throughput_pct=-4.864
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=99849 median_ms=79.463 nps=1256550 ordering=SEE_MATERIAL_QUIET_HISTORY
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=99914 median_ms=85.427 nps=1169580 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=evasion depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.065 wall_pct=7.506 throughput_pct=-6.921
comparison aggregate depth=6 positions=3 statistic=sum-of-position-medians baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-0.211 wall_pct=6.602 throughput_pct=-6.391
```

### Depth 6 fresh-JVM repeat, TT off

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=6 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=off table=none
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=219294 median_ms=189.832 nps=1155199 ordering=SEE_MATERIAL_QUIET_HISTORY
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=219294 median_ms=197.939 nps=1107889 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=kiwipete depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=4.270 throughput_pct=-4.095
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=564514 median_ms=449.661 nps=1255420 ordering=SEE_MATERIAL_QUIET_HISTORY
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=558849 median_ms=490.104 nps=1140267 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=middlegame depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-1.004 wall_pct=8.994 throughput_pct=-9.172
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=157868 median_ms=135.360 nps=1166281 ordering=SEE_MATERIAL_QUIET_HISTORY
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=157931 median_ms=144.294 nps=1094512 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=evasion depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.040 wall_pct=6.600 throughput_pct=-6.154
comparison aggregate depth=6 positions=3 statistic=sum-of-position-medians baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-0.595 wall_pct=7.418 throughput_pct=-7.460
```

### Depth 6 fresh-JVM repeat, cold 4 MiB TT on

```text
exact-search evaluator=HCE threads=1 java=21 vm=OpenJDK 64-Bit Server VM os=Windows 11/amd64 depth=6 warmups=5 repetitions=7
Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.
tt=on table=cold/cleared before each request; explicit harness 4 MiB
Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=167760 median_ms=153.067 nps=1095992 ordering=SEE_MATERIAL_QUIET_HISTORY
position=kiwipete requested=6 completed=6 best=e2a6 score=-220 pv=[e2a6 b4c3 d2c3 e6d5 e5f7 h3g2] nodes=167760 median_ms=167.249 nps=1003057 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=kiwipete depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.000 wall_pct=9.265 throughput_pct=-8.480
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=482640 median_ms=413.412 nps=1167456 ordering=SEE_MATERIAL_QUIET_HISTORY
position=middlegame requested=6 completed=6 best=e2e1 score=-126 pv=[e2e1 h7h6 c3d5 c5f2 e1f2 f6d5] nodes=480993 median_ms=429.183 nps=1120719 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=middlegame depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-0.341 wall_pct=3.815 throughput_pct=-4.003
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=99849 median_ms=83.676 nps=1193285 ordering=SEE_MATERIAL_QUIET_HISTORY
position=evasion requested=6 completed=6 best=d2d4 score=-787 pv=[d2d4 a3b4 a1b1 b6a7 h6f7 e8f7] nodes=99914 median_ms=83.541 nps=1195990 ordering=SEE_MATERIAL_CONTINUATION_HISTORY
comparison position=evasion depth=6 baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=0.065 wall_pct=-0.161 throughput_pct=0.227
comparison aggregate depth=6 positions=3 statistic=sum-of-position-medians baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY nodes_pct=-0.211 wall_pct=4.586 throughput_pct=-4.587
```
