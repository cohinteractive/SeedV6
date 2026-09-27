# ExactSearch capture history after material ordering - 2026-09-27

Starting revision: `543cdecbb16b875995a55fa30d78f39ef0a5e4ba`, clean worktree. This bounded SR-015 experiment adds compact capture history only as secondary evidence within equal immediate-material groups. CONTROL remains the production/default mode. All previous modes and aliases retain their meanings. Search contract and research frontier are unchanged.

## Research result

**Mixed evidence; recommend outcome 1: SEE_MATERIAL remains the stronger simple baseline.** History consistently reduces summed nodes, but its wall-time benefit is small and not consistently reproducible across TT settings and depths. Depth-6 TT-on is a real positive result in both batches (3.859% and 2.267% faster); it is not hidden by this recommendation. Depth-5 TT-off and depth-6 TT-off each change the sign of their aggregate wall-time result between fresh JVM batches. Depth-5 TT-on's improvement shrinks from 2.048% to 0.261%.

This is a simplicity-based recommendation, not a claim that material-only is faster on every workload or that capture history carries no useful information. The bounded experiment does not establish a sufficiently consistent overall advantage to add adaptive state to the preferred SR-015 path. The historical candidate remains independently selectable as evidence. No further SR-015 experiment is required to retain the established simpler candidate; more keys, tuning or persistence would require a new decision. No frontier disposition or production adoption is made here.

## Policies, state and arithmetic

- **M / SEE_MATERIAL**: legal hash, SEE-good tacticals ranked by immediate material, SEE-bad tacticals ranked by immediate material, stable quiets.
- **H / SEE_MATERIAL_CAPTURE_HISTORY**: same classes and primary material evidence, then descending learned capture history, then stable original order. It does not use LVA or numeric SEE magnitude.

New selectors: `--ordering=see-material-history` and paired `--ordering=see-material,see-material-history`. The Java constant is `ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY`. Existing constructors continue selecting CONTROL; `both` still means CONTROL versus SEE_TIERED.

Each H owner alone allocates one `int[8192]` (32 KiB payload). Its flattened identity is `((movingPieceIncludingSide * 64 + destination) * 8) + victimType`: four packed moving-piece bits, six square bits, three victim-type bits. Victim colour is redundant for generated legal captures because the victim must be the opponent; this removes a redundant dimension without losing legal-move identity. Some packed piece/type slots are unused.

EP normalizes the empty packed victim to pawn. Quiet promotions use explicit bucket zero; capture-promotions use the actual captured type. All promotion choices share a history key at the same moving-piece/destination/victim identity; their different immediate promotion gains remain primary. The source square is intentionally absent.

The owner clears the table with `Arrays.fill` at the start of **every valid top-level fixed-depth invocation**, including invocations inside one `beginRequest` lifetime, static/terminal requests, and immediately cancelled requests. Clearing is inside Search timing. No cross-owner sharing or cross-invocation learning exists. Aborted searches may have partial learned entries internally; the next invocation resets them before any node work.

The bound is **+/-16384**. At a searched tactical beta cutoff, `b = min(remainingDepth, 64)^2`: reward the successful move with +b, then visit the already-searched move-list prefix and apply -b to each earlier tactical. Quiet entries are neither read for ranking nor updated. A quiet cutoff makes no history updates. No unsearched sibling is penalized. A tactical hash cutoff receives its reward; a prior unsuccessful tactical hash receives its malus. TT score cutoffs do not count as searched tactical cutoffs.

Signed gravity is `h' = h + b - h * abs(b) / 16384`, with b clamped to the signed bound and Java integer division toward zero. Search bonuses range from 1 to 4096. Intermediate multiplication fits in int even for the full clamped bound. Repeated evidence saturates, while opposite evidence adapts existing entries. There are no decay sweeps. The bound/depth cap/squared bonus take retained `MoveOrdering` conventions as evidence; the signed gravity arithmetic is specific to this experiment and no parameter tuning was performed. Moves sharing the deliberately compact key apply their updates successively in searched order after the winner reward.

The existing `rankTacticals` stable insertion pass consumes `immediateMaterial * 32769 + history` for H. Since the maximum history difference is 32768, even one material unit dominates it. The largest legal composite, `1850 * 32769 + 16384`, fits in int. Strict less-than shifting retains equal-evidence generator order. M's ranking key is unchanged.

Immediate material continues to use SEE's phase-independent `Eval.exchangeValue`: captured value plus actual promotion gain, with EP pawn normalization. No evaluator output or Search score enters either key.

The legal hash is excluded from SEE and ranking, then retained first exactly once. Existing primitive score scratch is reused; the current node's existing move-list prefix survives recursion and supplies failed tactical moves for maluses. No per-node history storage, allocation, boxing, collections, streams, comparator objects or strategy dispatch is added. Source and compiled bytecode inspection confirm the new primitive paths allocate no objects. Existing top-level Search result/history allocations remain. No allocation profiler or JIT machine-code study was run.

Full `Gen.genAll` and the existing insertion mechanism remain. SEE/ranking still occur only after terminal/draw resolution, static leaves and applicable TT cutoffs. This does not decide SR-017 mechanics. TT policy, exact values, evaluator, production lifecycle, cancellation/result rules, qsearch, PVS, reductions and pruning are unchanged. No quiet history or other refutation mechanism is added.

## Changed files and symbols

- `search/exact/CaptureHistory.java`: primitive index, tactical predicate, cutoff update and signed gravity helpers.
- `search/exact/ExactSearch.java`: distinct mode, optional owner table, per-invocation reset, tactical cutoff learning and secondary ranking.
- `tools/search/ExactSearchHarness.java`: singleton/paired selectors and output name.
- `search/exact/CaptureHistoryTest.java`: seven focused tests.
- `search/exact/ExactSearchOrderingTest.java`: six-mode semantic/ordering coverage and preserved material visitation baseline.
- `tools/search/ExactSearchHarnessTest.java`: independent selection and repeatability for the new pair.
- This evidence file and the required root version-state finalization. No canon/frontier or historical report edits.

## Validation

Executed twice:

```powershell
.\gradlew.bat :app:test --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' -Pheadless=true --console=plain
```

Initial run: 78 tests, one failed new fixture assertion, BUILD FAILED in 6 s. The reset test incorrectly assumed Kiwipete depth 3 would finish with a negative history entry; final history signs depend on its tree. The separate controlled real-cutoff test already proves negative maluses. Removed only that new, irrelevant final-sign assumption; no production correction or historical test weakening was needed.

**Final run: 78 tests in 10 suites, zero failures/errors/skips, BUILD SUCCESSFUL in 4 s.** Main/test compilation and module packaging succeeded.

Coverage includes:

- 5376 distinct packed moving-side/piece/destination/victim tuples, omitted source identity, EP in both colours, no-victim promotions, actual promotion victims and all four promotion choices in both colours.
- Exact signed-gravity arithmetic, positive/negative bounds, saturation, reversal, depth cap, reward and prefix maluses, quiet-cutoff non-updates, and unsearched sibling exclusion.
- Actual searched beta-cutoff learning with TT off, a quiet hash preceding failed/winning tacticals, and a successful tactical hash; an actual quiet hash cutoff leaves the table zero.
- Owner isolation; poisoned-table reset, identical learned state/node counts/best/PV across repeated fixed-depth calls in one driver request; immediate cancellation/static reset; terminal/draw/TT-cutoff non-learning.
- Seeded extreme history proves primary material dominance, real adaptive tie changes, stable equal evidence, all hash classes first exactly once, complete move sets, deterministic order, EP, promotions/underpromotions and evasions.
- 504 independent exhaustive score/optimal-best/PV comparisons: 14 positions, depths 0-2, six modes, TT off/on. Input boards are preserved.
- Existing root move-visitation and scratch-sentinel checks extended to H: unique complete lists, hash handling, no SEE/ranking at resolved nodes, static leaves, quiets or a sole tactical hash.
- Mate/stalemate/rule draws/repetition, cancellation and last-leaf incomplete results, absent aborted root TT publication, exact TT evidence and driver/production boundaries.
- Historical Start depth-5 node counts retained, including M's 48224 TT-off and 43737 TT-on.
- Existing material accounting: 33 generated tactical checks and 19 explicit special/capture values.
- Independent SEE oracle tests: 20 focused moves/560 thresholds; 24 promotion moves/672 thresholds; 2 optional/branching moves/56 thresholds; 935 generated positions/3765 tactical moves/105420 thresholds. SEE code is unchanged.

Static checks: `git diff --check`; protected-file diff; source inspection; `javap -c -p` for CaptureHistory and ExactSearch's allocation/call sites. Final staged whitespace checks are also required before commit.

No score equivalence, move uniqueness, history ownership or material-semantics defect was found.

## Benchmark methodology and commands

Unchanged six-position depth-5 corpus from `ExactSearchHarness.orderingPositions()`, with FENs preserved in [the earlier evidence](SEARCH_SEE_TACTICAL_EXPERIMENT_2026-09-27.md). Depth-6 confirmation uses exactly Kiwipete, Middlegame and Evasion.

Windows 11 amd64; AMD Ryzen 5 5500, 6 cores/12 logical processors; OpenJDK 21 build `21+35-2513`; `-Xms256m -Xmx256m -Xbatch`; HCE; one Search thread. Five warmups, seven measured repetitions per position/mode. Two owners alternate execution order each round. TT-off uses no table; TT-on uses separate cold 4 MiB tables cleared outside timing. History reset occurs inside every measured/warmup invocation, so all repetitions start identically.

Median Search elapsed time (fourth of seven sorted samples) includes fixed-depth setup and history reset, excludes owner construction, FEN parsing and TT clear. NPS comes from the same sample. Every round verifies score equality and deterministic per-mode nodes/best/PV. Untimed legal PV replay and TT-off CONTROL best-child/PV-endpoint re-searches verify realized exact root values.

Four commands, each run once as the primary batch and repeated once in a fresh JVM because the small timing gains warranted a reproducibility check:

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material,see-material-history --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material,see-material-history --tt=on' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=5 --repetitions=7 --ordering=see-material,see-material-history --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=kiwipete,middlegame,evasion --depth=6 --warmups=5 --repetitions=7 --ordering=see-material,see-material-history --tt=on' --console=plain
```

All eight runs passed. Primary Gradle elapsed: 8 s, 8 s, 23 s, 21 s. Repeat: 8 s, 8 s, 22 s, 21 s. Runs were sequential with no other agent testing during measurement. Total: 504 measured searches and 360 warmups, plus untimed semantic verification. No policy or parameter changed between batches. All displayed scores, best moves and PVs match across modes/TT/batches; node counts repeat exactly, and M matches the preceding experiment's recorded nodes.

M/H columns below are material baseline / capture-history candidate. Score and best are shared by both modes. Aggregate changes use the harness's unrounded samples; displayed summed milliseconds can differ by rounding.

## Primary depth 5, TT off

| Position | Depth | Score | Best move | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| start | 5 | 197 | e2e3 | 48,224 / 47,823 | 23.761 / 23.596 | 2,029,569 / 2,026,741 |
| kiwipete | 5 | 403 | d5e6 | 83,934 / 83,214 | 58.764 / 57.297 | 1,428,325 / 1,452,340 |
| tactical | 5 | 1177 | e4d5 | 5,233 / 4,830 | 1.455 / 1.321 | 3,595,575 / 3,657,705 |
| evasion | 5 | -81 | c4c5 | 20,741 / 20,704 | 14.514 / 14.663 | 1,429,043 / 1,411,998 |
| middlegame | 5 | 250 | c3d5 | 192,147 / 189,857 | 127.803 / 124.213 | 1,503,457 / 1,528,482 |
| transposition-pawns | 5 | 6 | e1d2 | 2,867 / 2,867 | 0.619 / 0.645 | 4,630,167 / 4,445,650 |

## Primary depth 5, cold TT on

| Position | Depth | Score | Best move | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| start | 5 | 197 | e2e3 | 43,737 / 43,358 | 23.597 / 23.375 | 1,853,521 / 1,854,863 |
| kiwipete | 5 | 403 | d5e6 | 65,515 / 62,853 | 47.124 / 45.854 | 1,390,265 / 1,370,714 |
| tactical | 5 | 1177 | e4d5 | 4,550 / 4,294 | 1.397 / 1.269 | 3,256,046 / 3,382,700 |
| evasion | 5 | -81 | c4c5 | 19,834 / 19,702 | 15.318 / 15.114 | 1,294,791 / 1,303,576 |
| middlegame | 5 | 250 | c3d5 | 167,784 / 165,989 | 116.828 / 114.446 | 1,436,168 / 1,450,369 |
| transposition-pawns | 5 | 6 | e1d2 | 2,788 / 2,788 | 0.660 / 0.668 | 4,222,323 / 4,174,277 |

## Primary depth 6, TT off

| Position | Depth | Score | Best move | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 219,362 / 228,612 | 153.588 / 167.214 | 1,428,251 / 1,367,180 |
| middlegame | 6 | -126 | e2e1 | 844,853 / 810,342 | 574.847 / 562.069 | 1,469,701 / 1,441,713 |
| evasion | 6 | -787 | d2d4 | 160,027 / 161,447 | 103.198 / 105.308 | 1,550,677 / 1,533,096 |

## Primary depth 6, cold TT on

| Position | Depth | Score | Best move | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 167,833 / 168,201 | 137.314 / 135.691 | 1,222,259 / 1,239,586 |
| middlegame | 6 | -126 | e2e1 | 719,485 / 688,571 | 538.697 / 512.924 | 1,335,603 / 1,342,443 |
| evasion | 6 | -787 | d2d4 | 101,518 / 104,051 | 79.642 / 77.876 | 1,274,682 / 1,336,112 |

## Aggregate comparisons

Percentages are H relative to M. Throughput is summed nodes divided by summed position medians; unlike depths are never combined.

| Batch | Depth / positions | TT | Summed nodes M / H | Summed median ms M / H | Nodes change | Wall change | Throughput change |
|---|---|---|---:|---:|---:|---:|---:|
| Primary | 5 / 6 | off | 353,146 / 349,295 | 226.916 / 221.735 | -1.090% | -2.284% | 1.222% |
| Primary | 5 / 6 | on | 304,208 / 298,984 | 204.924 / 200.726 | -1.717% | -2.048% | 0.338% |
| Primary | 6 / 3 | off | 1,224,242 / 1,200,401 | 831.633 / 834.591 | -1.947% | 0.356% | -2.295% |
| Primary | 6 / 3 | on | 988,836 / 960,823 | 755.653 / 726.491 | -2.833% | -3.859% | 1.067% |
| Repeat | 5 / 6 | off | 353,146 / 349,295 | 220.956 / 222.362 | -1.090% | 0.636% | -1.715% |
| Repeat | 5 / 6 | on | 304,208 / 298,984 | 203.273 / 202.742 | -1.717% | -0.261% | -1.460% |
| Repeat | 6 / 3 | off | 1,224,242 / 1,200,401 | 823.204 / 811.215 | -1.947% | -1.456% | -0.498% |
| Repeat | 6 / 3 | on | 988,836 / 960,823 | 752.411 / 735.357 | -2.833% | -2.267% | -0.579% |

## Interpretation and limits

History adds genuine, deterministic tree information: depth-5 summed nodes fall 1.090% off / 1.717% on, and depth-6 nodes fall 1.947% off / 2.833% on. Tactical depth 5 improves 7.701% / 5.626% in nodes. Middlegame depth 6 improves 4.085% / 4.297%, with wall-time gains in both batches.

There are also real tree regressions: depth-6 Kiwipete grows 4.217% off / 0.219% on; Evasion grows 0.887% off / 2.495% on. Kiwipete TT-off wall time regresses 8.872% and 7.935%. Start's depth-5 node saving is under 1%, with wall time changing from slight benefit to regression in the repeat. Transposition-pawns visits are identical and its tiny timings mostly expose added overhead/noise.

Throughput does not give a stable uniform improvement. Repeat aggregate throughput is lower in all four configurations (1.715%, 1.460%, 0.498%, 0.579%). Added table lookup, gravity updates, prefix scans and ranking/reset work are real costs; aggregate NPS also depends on changed node composition and cannot isolate their causal cost as a microbenchmark would. Primary throughput sometimes rises, emphasizing the effects of node composition, JIT/runtime state and timing variation.

Thus the candidate has modest useful node evidence, and reproducible benefit in the deeper TT-on subset, but no clear robust overall wall-time win sufficient to displace the simple current candidate. Retain **hash -> SEE-good material-ordered tacticals -> SEE-bad material-ordered tacticals -> quiets** as the recommended SR-015 candidate path. Capture history remains an experimental mode, not preferred architecture or production policy. This recommendation does not claim statistical significance or playing-strength improvement.

The six fixtures, shallow depths and one machine limit generality. No cross-request or iterative-deepening history persistence was tested; neither should be inferred from these results. Shared compact keys intentionally pool promotion choices and sources, and are not tuned. No allocation/JIT/branch-counter profiling was performed.

Deliberately not run: full application/long NNUE/perft suites, self-play/Elo, deeper or larger benchmark programmes, alternative keys/weights/bonus formulas, persistent history, quiet-history or sorting/generation experiments. They are outside this bounded unit; targeted ExactSearch/TT/oracle/driver/harness/SEE validation passed.

## Fresh-JVM repeat per-position results

### Depth 5, TT off

| Position | Depth | Score | Best move | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| start | 5 | 197 | e2e3 | 48,224 / 47,823 | 24.146 / 25.328 | 1,997,158 / 1,888,132 |
| kiwipete | 5 | 403 | d5e6 | 83,934 / 83,214 | 57.158 / 56.802 | 1,468,455 / 1,464,975 |
| tactical | 5 | 1177 | e4d5 | 5,233 / 4,830 | 1.454 / 1.333 | 3,598,294 / 3,623,134 |
| evasion | 5 | -81 | c4c5 | 20,741 / 20,704 | 14.263 / 14.960 | 1,454,151 / 1,384,003 |
| middlegame | 5 | 250 | c3d5 | 192,147 / 189,857 | 123.325 / 123.310 | 1,558,053 / 1,539,678 |
| transposition-pawns | 5 | 6 | e1d2 | 2,867 / 2,867 | 0.610 / 0.629 | 4,702,312 / 4,560,203 |

### Depth 5, cold TT on

| Position | Depth | Score | Best move | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| start | 5 | 197 | e2e3 | 43,737 / 43,358 | 23.877 / 24.541 | 1,831,762 / 1,766,786 |
| kiwipete | 5 | 403 | d5e6 | 65,515 / 62,853 | 48.240 / 45.902 | 1,358,108 / 1,369,301 |
| tactical | 5 | 1177 | e4d5 | 4,550 / 4,294 | 1.304 / 1.258 | 3,489,799 / 3,414,711 |
| evasion | 5 | -81 | c4c5 | 19,834 / 19,702 | 14.532 / 14.486 | 1,364,887 / 1,360,071 |
| middlegame | 5 | 250 | c3d5 | 167,784 / 165,989 | 114.561 / 115.795 | 1,464,588 / 1,433,477 |
| transposition-pawns | 5 | 6 | e1d2 | 2,788 / 2,788 | 0.759 / 0.760 | 3,675,191 / 3,667,938 |

### Depth 6, TT off

| Position | Depth | Score | Best move | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 219,362 / 228,612 | 151.895 / 163.948 | 1,444,171 / 1,394,421 |
| middlegame | 6 | -126 | e2e1 | 844,853 / 810,342 | 562.363 / 537.863 | 1,502,326 / 1,506,595 |
| evasion | 6 | -787 | d2d4 | 160,027 / 161,447 | 108.946 / 109.404 | 1,468,866 / 1,475,702 |

### Depth 6, cold TT on

| Position | Depth | Score | Best move | Nodes M / H | Median ms M / H | NPS M / H |
|---|---:|---:|---|---:|---:|---:|
| kiwipete | 6 | -220 | e2a6 | 167,833 / 168,201 | 130.462 / 132.038 | 1,286,456 / 1,273,880 |
| middlegame | 6 | -126 | e2e1 | 719,485 / 688,571 | 545.547 / 525.469 | 1,318,831 / 1,310,394 |
| evasion | 6 | -787 | d2d4 | 101,518 / 104,051 | 76.402 / 77.850 | 1,328,734 / 1,336,555 |

Human actions required after this prompt: None. GPT/user reconciliation of the recommended research conclusion remains separate from implementation completion; no later workstream is advanced here.
