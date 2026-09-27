# ExactSearch SEE within tacticals experiment - 2026-09-27

Starting revision: `4d294d6bbc7bf313bfe684a3a953576016441b36`, clean worktree. The user accepted the preceding SEE_TIERED experiment as evidence and rejected its ordering policy. That outcome did not reject SEE itself. This experiment separates good/bad tactical classification from demoting bad tacticals behind quiets.

CONTROL remains the production/default ordering. No Search contract or frontier disposition is changed. The earlier [SEE_TIERED evidence](SEARCH_SEE_TIERED_EXPERIMENT_2026-09-27.md) is preserved byte-for-byte.

## Implementation

`ExactSearch.SEE_TACTICAL` selects exactly:

1. Legal applicable hash move, once, regardless of class.
2. Remaining SEE-good tactical moves, in their existing relative order.
3. Remaining SEE-bad tactical moves, in their existing relative order.
4. Remaining quiet moves, in their existing relative order.

The three-argument ExactSearch constructor accepts the new primitive mode; established constructors still select CONTROL. The harness accepts `--ordering=see-tactical` alone and `--ordering=control,see-tactical` for this pair. Existing `--ordering=both` still compares CONTROL with SEE_TIERED; `see-tiered` is not redefined.

`orderSeeClassified` shares the existing SEE partition pass. Good tacticals compact into the generated primitive list; bad tacticals and quiets use reusable owner-held arrays. The new mode copies bad tacticals before quiets; SEE_TIERED retains the opposite copy order. Existing stable legal hash promotion remains unchanged. Captures, en passant and all promotions use `See.atLeastGeneratedLegal(board, move, 0)`; quiets and the matched hash hint bypass SEE.

Full `Gen.genAll`, terminal/draw precedence, static leaves, TT score resolution, evaluator, Search recursion, cancellation and production lifecycle are unchanged. The partition is reached only at positive depth after applicable TT score cutoffs. There is no sorting, tactical scoring, history, pruning, PVS, qsearch, staged generation or strategy-object architecture.

The new mode reuses the SEE mode's one `long[512]` bad-tactical buffer (4 KiB payload per Search owner) and existing quiet scratch. No new per-node allocation, boxing, streams or collections. A primitive mode branch after classification selects the two copy destinations; no extra policy dispatch inside the classification loop. Source and `javap -c -p` inspection confirmed the partition contains no allocation instructions. No allocation profiler or standalone SEE microbenchmark was run.

Changed implementation/test symbols: ExactSearch constants/constructor/partition dispatch/`orderSeeClassified`; ExactSearchHarness argument parsing/help/`orderingName`; ExactSearchOrderingTest; ExactSearchHarnessTest. This evidence file and the required version-finalizer state are the other work-unit files.

## Validation

Command, from the repository root:

```powershell
.\gradlew.bat :app:test --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' -Pheadless=true --console=plain
git diff --check
```

Result: **66 tests in 9 suites, zero failures/errors/skips; BUILD SUCCESSFUL in 5 s**. Main/test compilation and module packaging tasks succeeded. Whitespace checks passed.

- Exhaustive-oracle comparisons expanded from 168 to **252**: 14 positions, depths 0-2, three ordering modes and TT off/on. This adds 84 comparisons for the new candidate, while retaining the 168 CONTROL/SEE_TIERED comparisons. Exact scores, legal optimal best moves, consistent PVs and board preservation passed.
- Root visitation checks still compare exact order, complete move-set identity and uniqueness separately from score tests. They now include all three modes and explicitly require every non-hash tactical to precede every quiet in SEE_TACTICAL.
- Fixtures cover good/bad captures, quiets, en passant, capture and quiet promotions/underpromotions, checked evasions and high mobility.
- Both SEE modes test good/bad/quiet legal hash hints, old-generation and depth-mismatched hints, invalid hints and stable remaining order.
- Both SEE modes retain static-leaf, terminal/draw and EXACT/LOWER/UPPER TT-cutoff partition-bypass checks. Inspection confirms the only SEE call is guarded by tactical classification.
- All three modes exercise repetition, mid-search and last-leaf cancellation, incomplete results, absent root TT publication on abort and owner reuse.
- A new deterministic visitation regression checks the accepted depth-5 Start node counts for CONTROL and SEE_TIERED with TT off/on. Existing deterministic tests were not weakened.
- ExactSearch, exact TT/oracle, SearchDriver/production-boundary, harness and independently oracle-validated threshold SEE tests passed.

No correctness failure, move-set failure or repository contradiction was found.

## Benchmark method

The six positions and FENs are unchanged in `ExactSearchHarness.orderingPositions()` and recorded in the preceding evidence file: Start (quiet opening), Kiwipete (high branching), Tactical (exchanges), Evasion (checked root/promotions), Middlegame (ordinary complex middlegame), and Transposition-pawns.

The accepted methodology is unchanged:

- Windows 11 amd64; AMD Ryzen 5 5500, 6 cores/12 logical processors.
- OpenJDK 21, build `21+35-2513`, 64-Bit Server VM; `-Xms256m -Xmx256m -Xbatch`.
- HCE, one Search thread, depth **5** for every position/mode.
- Five warmups and seven measured repetitions per position/mode. Paired execution order alternates every round; separate reusable Search/evaluator owners.
- TT off, then TT on, in sequential JVM runs. TT on uses a separate 4 MiB table per owner, cleared outside timing before every request.
- Median of seven Search elapsed samples, including existing request setup and excluding owner construction, FEN parsing and table clearing. NPS comes from that same median sample. Timing/statistical logic was not changed.
- Every round verifies equal scores and per-mode repeatability of nodes, best move and PV. Legal PV replay plus TT-off CONTROL best-child/PV-endpoint re-searches run outside timing.

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=control,see-tactical --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=control,see-tactical --tt=on' --console=plain
```

Both commands passed (Gradle reported 10 s and 9 s). Across both TT configurations there were 168 measured searches and 120 warmup searches. **All scores, best moves and PVs matched across modes and TT settings.** No different equal-valued best move occurred. Every CONTROL node count also matches the preceding six-position evidence.

## TT off

C = CONTROL; S = SEE_TACTICAL. Score and best move apply to both modes.

| Position | Depth | Score | Best | C nodes | S nodes | C median ms | S median ms | C NPS | S NPS |
| --- | ---: | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 5 | 197 | e2e3 | 48,266 | 48,287 | 24.531 | 24.218 | 1,967,583 | 1,993,888 |
| kiwipete | 5 | 403 | d5e6 | 147,864 | 92,891 | 101.802 | 65.327 | 1,452,473 | 1,421,936 |
| tactical | 5 | 1177 | e4d5 | 6,103 | 5,277 | 1.617 | 1.427 | 3,773,573 | 3,697,967 |
| evasion | 5 | -81 | c4c5 | 43,223 | 20,428 | 28.573 | 14.489 | 1,512,737 | 1,409,897 |
| middlegame | 5 | 250 | c3d5 | 227,299 | 207,383 | 153.510 | 146.073 | 1,480,674 | 1,419,723 |
| transposition-pawns | 5 | 6 | e1d2 | 2,867 | 2,867 | 0.617 | 0.634 | 4,649,691 | 4,524,936 |

## TT on

Same depth and evaluator, cold 4 MiB tables. Score and best move apply to both modes.

| Position | Depth | Score | Best | C nodes | S nodes | C median ms | S median ms | C NPS | S NPS |
| --- | ---: | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 5 | 197 | e2e3 | 43,779 | 43,800 | 23.219 | 23.171 | 1,885,489 | 1,890,261 |
| kiwipete | 5 | 403 | d5e6 | 128,832 | 74,360 | 89.783 | 62.093 | 1,434,921 | 1,197,560 |
| tactical | 5 | 1177 | e4d5 | 4,648 | 4,594 | 1.424 | 1.473 | 3,263,128 | 3,118,381 |
| evasion | 5 | -81 | c4c5 | 40,524 | 18,504 | 31.348 | 14.555 | 1,292,709 | 1,271,341 |
| middlegame | 5 | 250 | c3d5 | 189,509 | 180,188 | 130.015 | 130.640 | 1,457,592 | 1,379,272 |
| transposition-pawns | 5 | 6 | e1d2 | 2,788 | 2,788 | 0.620 | 0.821 | 4,500,403 | 3,396,272 |

## Aggregate

Sum node counts and position medians at the same depth; aggregate throughput is summed nodes divided by summed median time, not a mean of position NPS. Displayed time totals sum rounded milliseconds; percentage deltas come from the harness's unrounded nanoseconds.

| TT | C nodes | S nodes | C summed ms | S summed ms | Nodes change | Wall change | Throughput change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| off | 475,622 | 377,133 | 310.650 | 252.168 | -20.707% | -18.826% | -2.318% |
| on | 410,080 | 324,234 | 276.409 | 232.753 | -20.934% | -15.794% | -6.104% |

Approximate aggregate NPS: 1.531M to 1.496M with TT off; 1.484M to 1.393M with TT on.

## Comparison with rejected SEE_TIERED

The pathological tree growth observed previously is removed in these fixtures. Historical comparisons below use exact deterministic nodes. Current wall-time conclusions use the new paired CONTROL run, not historical timings from another JVM session.

| Position / TT | CONTROL nodes (unchanged) | Prior SEE_TIERED nodes | New SEE_TACTICAL nodes | Prior node change | New node change |
| --- | ---: | ---: | ---: | ---: | ---: |
| Start / off | 48,266 | 103,653 | 48,287 | +114.754% | +0.044% |
| Start / on | 43,779 | 95,495 | 43,800 | +118.130% | +0.048% |
| Middlegame / off | 227,299 | 4,418,180 | 207,383 | +1843.774% | -8.762% |
| Middlegame / on | 189,509 | 4,266,197 | 180,188 | +2151.184% | -4.918% |

Start adds just 21 nodes in either TT mode. Middlegame has useful node reductions instead of the prior explosions, but its TT-on wall time is essentially flat/slightly worse (+0.481%) because throughput falls 5.373%. Its TT-off wall time improves 4.845%.

The new tactical classification reduces Kiwipete nodes by 37.178%/42.281% (TT off/on) and wall time by 35.829%/30.841%. Evasion nodes fall 52.738%/54.338%, with wall improvements of 49.291%/53.571%.

The small Tactical fixture improves nodes/time with TT off (-13.534%/-11.767%); TT on reduces nodes 1.162% but increases time 3.426%. Transposition-pawns has identical nodes and measured time increases of 2.757%/32.510%; these sub-millisecond samples differ by only 0.017/0.201 ms and should not carry a broad performance conclusion.

## Evidence conclusion and limits

**Favourable aggregate evidence, with mixed per-position results.** Keeping every tactical ahead of quiets removes the observed large regressions while retaining useful good/bad classification gains in Kiwipete and Evasion. Overall node reduction is about 21% in both TT configurations; resulting wall-time improvements are smaller, about 19% and 16%.

Throughput costs remain: aggregate NPS falls 2.318% with TT off and 6.104% with TT on, and most individual positions have lower NPS. This measures the combined cost of classification and the changed mix of visited nodes, not pure SEE unit cost. Kiwipete TT-on NPS falls 16.542%, yet its larger node reduction still produces a wall-time improvement. No claim of improvement rests only on fewer nodes.

This is evidence about SEE good/bad classification within the tactical block under these conditions. It is not an automatic adoption decision or an SR-015 disposition. CONTROL remains default. The rejected SEE_TIERED policy and its evidence remain available, and its failure is not reinterpreted as a rejection of SEE.

Limits: one host, one fixed depth, six positions, HCE static leaves without qsearch, cold 4 MiB TT, no playing-strength result or confidence interval. Small timing differences may be noise. Effects at other depths, with neural evaluation, larger/warm TT or future ordering mechanisms remain unknown.

Deliberately skipped: full repository/long-running suites, long perft/exhaustive campaigns, GUI validation, standalone allocation/JIT/branch profiling, further SEE_TIERED timed runs and broader benchmarking. None was needed to isolate and validate this bounded ordering change.
