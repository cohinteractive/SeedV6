# BRN qsearch calibration diagnostic — 2026-09-24

**Verdict: calibration is causally useful in some settings, but does not reliably restore BRN stand-pat effectiveness. It is not ready for a production calibration layer.** A frozen, independent capture-mean fit, with a small linear gain to preserve the normal score band, reduces g134 Kiwipete depth-4 qnodes **209,297 → 160,327 (23.40%)**, with eligible stand-pat cutoffs **44.71% → 46.93%**. This is measurable but insufficient: **4.96× NNUE qnodes** remain, with a **33.98 percentage-point** cutoff deficit. Capture-median calibration instead raises g134 depth-4 qnodes to **273,667 (+30.76%)** and changes its best move.

For g127, capture-median calibration reduces the matched, completed depth-2 count **696,663 → 376,902 (45.90%)**, but depth 4 still reaches the million-node cap: **999,635 → 997,308 qnodes**, completing depth **2 → 3**. The cap masks the full required workload; its 0.23% prefix-count difference is not a depth-4 speedup. General legal-position centering worsens the reproducer. Gain-only scaling leaves qnode counts unchanged on all six fixtures for both checkpoints.

The appropriate classification is **substantial shallow/position-specific improvement, but insufficient and inconsistent primary-reproducer improvement; no near-resolution**. This does not establish that every possible independent calibration must fail, or that an architecture change is necessary. It does establish that a single global center is population-, checkpoint- and horizon-sensitive, that better score histograms are insufficient, and that substantial local capture-neighbour problems remain.

## Scope, provenance and relationship to the accepted unit

- Date: 2026-09-24 UTC. First machine-clock observation was `2026-09-24T01:01:45Z`.
- Authoritative repository: `C:\projects\seed\java\seedv6`.
- Inspected HEAD: `8ea5cb99b32330c1b85ef8a918dfb24ae5336109`, the user's accepted, preceding report-only commit.
- Read `BRN_QSEARCH_STAND_PAT_DIAGNOSTIC_2026-09-24.md`, including its method, offset algebra, limitations and executable Appendix A. This unit performs the causal evaluator-output intervention that report recommended. Its conclusions are extended, not silently replaced.
- No on-disk ancestor/repository `AGENTS.md` was found. Current user-supplied governance applies. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` are independently absent; no journal/version capability was activated and neither file was created.
- Initial worktree: 41 modified tracked files and 46 individually enumerated untracked files; the index was empty. These inherited GUI/training/document/build-output changes remain unattributed and excluded. SHA-256 baseline covers all **539 tracked/non-ignored files** present at the start.
- Current core, rules, search and diagnostic-helper source have no diff from HEAD. The disposable copy was made with `git archive HEAD`, outside the repository. It contains none of the inherited edits. Pristine classes were freshly compiled from current authoritative source; calibrated classes were freshly compiled from the copy.
- The persistent report is the only intended authoritative-repository change. No training, checkpoint write, promotion, GUI setting, qsearch rule, move-ordering rule, pruning rule or NNUE source/evaluation change was made. No production calibration was installed.

## Frozen checkpoint identities

Direct read-only codecs loaded the exact payloads from the accepted report, with format/schema/dimension/finite-value/CRC checks. BRN is schema 2, width 32, incremental bounded-intermediate implementation. These are inference snapshots, not a fresh claim about Best/Latest references or strength. Full store/optimizer/lineage acceptance was not repeated.

| Role | Exact payload path | SHA-256 |
| --- | --- | --- |
| g127 | `E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000127-s000206807-d42ee23437e9d3e1a9525b7425bd46df1ecfc36b114c20ccfe380621f1fbfb84\network.brn2` | `474a43ee06c8f86357d4d1d3afd0f68fbfcfd6086b3e976ba4458791bbb27b31` |
| g134 | `E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c\network.brn2` | `21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924` |
| nnue | `E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6\network.nnue` | `3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9` |

## Calibration corpus and separation from search testing

The initial existing `EvaluationCorpus` was inspected but not used for fitting: 2,703 of its 3,282 entries are from Kiwipete, and several other roots are the search-test fixtures. Using it wholesale would contaminate this experiment.

Before any search-result measurement in this unit, the external `CalibrationCorpus` generated and froze **512 legal positions from 64 independent random-move paths**, with seed `20260924 + pathIndex` in Java `SplittableRandom`. All paths start from the existing `opening-ruy-lopez` fixture:

`r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3`

At each step, all legal moves are sorted by coordinate and one is chosen uniformly, without consulting BRN, NNUE, a search, a teacher, a qnode count or a match result. Retain plies **16, 17, 32, 33, 48, 49, 64, 65** after that root. Whole paths only are retained, to give equal path weight and exactly **256 White / 256 Black STM** positions. No path was omitted; all 512 keys are unique and exclude the six search roots. This is bounded position sampling, not engine self-play, training-data creation or a self-play generation.

The broad positions showed a different distribution from the accepted report's capture-selected endpoints. Before search testing, the same 512 boards were therefore also expanded through **every legal immediate capture**: **2,556 edges / 5,112 equally weighted endpoint observations**. This second frozen population estimates a capture-context center; it was not substituted for or used to erase the broad result. Parents repeat per outgoing edge, and transpositions are retained. All original 512 observations are unchanged. Children remain legal; terminal children are retained for descriptive frozen evaluation, not fabricated stand-pat attempts.

`ROOT_*` centers come from the 512 broad observations. `CAPTURE_*` centers come from the 5,112 capture endpoints. The latter are descriptive central tendencies, **not a demonstrated correct chess zero point**: real captures can improve value, and NNUE itself has a nonzero endpoint center. The sample is evaluator-independent but artificial, generated from one opening family and often materially imbalanced; it is not representative evidence for all training/play positions.

All primary coefficient choices and both populations were frozen before the first test search. The only later extension was `AFFINE_SAFE_MEAN`, computed mechanically from the already frozen capture mean after pure-offset range failures. No coefficient was selected by qnode minimization, and neither Kiwipete positions, prior Kiwipete means nor search-test outcomes entered a fit. The best-performing variants identified below are retrospective descriptions, not newly tuned/shipped constants.

For stronger separation evidence, the disposable BRN mapping checks every actual main/qsearch static board key against **all 3,019 unique calibration root and capture-child keys**. Zero overlap occurred in any request, including prefixes that later failed the score-band guard. All detached samples and fixed-test edge keys also show zero overlap. This does not assert that the calibration population resembles the search population; it asserts separation.

| Checkpoint | Population | N observations | Mean STM | Median STM | IQR | Min / max |
| --- | --- | --- | --- | --- | --- | --- |
| g127 | roots | 512 | 851.132812500 | 1,490.00 | 25,032.00 | -30,576 / 31,537 |
| g127 | captures | 5112 | -1309.890845070 | -1,821.00 | 25,554.50 | -31,423 / 31,537 |
| g134 | roots | 512 | 771.074218750 | 1,135.50 | 25,149.25 | -28,212 / 31,156 |
| g134 | captures | 5112 | -1067.949921753 | -1,558.00 | 24,580.00 | -30,303 / 31,156 |
| nnue | roots | 512 | 1901.595703125 | 3,306.00 | 42,836.50 | -29,280 / 28,967 |
| nnue | captures | 5112 | -422.868935837 | -374.50 | 44,170.50 | -29,280 / 28,967 |

Both BRN broad centers are mildly positive; both capture centers are mildly negative. The g127 capture mean **−1,309.89** is far smaller in magnitude than the accepted report's Kiwipete-dominated fixed-capture mean **−7,594.73**. g134 is **−1,067.95 versus −3,044.61**. This is direct evidence of population dependence, not license to replace the independent fit with the larger reproducer-specific offset.

For descriptive sampling uncertainty, 1,000 whole-path bootstrap resamples (64 paths per resample; Python `Random(20260924)`) give the following 95% percentile intervals. These describe this random-path generator only; they exclude uncertainty about its relevance to real play and do not justify optimizing within the intervals.

| Model | Population | Mean center interval | Median center interval |
| --- | --- | --- | --- |
| g127 | roots | 71.8 to 1,593.4 | -580.5 to 3,248.0 |
| g127 | captures | -2,017.1 to -655.6 | -2,615.3 to -706.0 |
| g134 | roots | 15.3 to 1,472.5 | -205.3 to 2,803.0 |
| g134 | captures | -1,763.3 to -347.6 | -2,888.1 to -357.9 |

## Formulas, coefficients and score-band integrity

Let `x` be the production BRN mapped **integer STM search score**, not raw pre-tanh output and not centipawns. The temporary output is `T(x) = Math.round(a*x + b)` (Java rounding). The normal range is `[-32511,32511]`; mate/draw/terminal search returns are untouched.

- `RAW`: `a=1,b=0`, exactly the unmodified score.
- `ROOT_MEAN`, `ROOT_MEDIAN`: subtract the respective broad-position center.
- `CAPTURE_MEAN`, `CAPTURE_MEDIAN`: subtract the respective frozen capture-endpoint center.
- `AFFINE_SAFE_MEAN`, `AFFINE_SAFE` (median): `b=-a*c`, `a=min(IQR_NNUE/IQR_BRN, 32511/(32511+abs(c)))` on the capture population. This is robust spread matching subject to the **full raw score band's linear admissibility constraint**. It is a safety-constrained affine experiment, not successful NNUE spread matching.
- `GAIN_ONLY`: use `AFFINE_SAFE`'s positive gain with zero intercept, to isolate scaling.

Unconstrained robust spread matching asks to **increase** gain to **1.7284822634 (g127)** or **1.7970097640 (g134)**, not reduce an oversized BRN scale. It would drive actual calibration observations outside the normal band. The full-band constraint binds, giving a modest contraction instead. No clipping, nonlinear remapping, mate-band change or qsearch exception was introduced. A pure offset has no global normal-band guarantee; it was allowed to run only while every observed transformed static remained admissible. An out-of-band static aborts that diagnostic request and is not passed to search.

| Model | Variant | a | b |
| --- | --- | --- | --- |
| g127 | RAW | 1 | 0 |
| g127 | ROOT_MEAN | 1 | -851.1328125 |
| g127 | ROOT_MEDIAN | 1 | -1490 |
| g127 | CAPTURE_MEAN | 1 | 1309.89084507042 |
| g127 | CAPTURE_MEDIAN | 1 | 1821 |
| g127 | AFFINE_SAFE_MEAN | 0.961269771069281 | 1259.15847276659 |
| g127 | AFFINE_SAFE | 0.946959105207969 | 1724.41253058371 |
| g127 | GAIN_ONLY | 0.946959105207969 | 0 |
| g134 | RAW | 1 | 0 |
| g134 | ROOT_MEAN | 1 | -771.07421875 |
| g134 | ROOT_MEDIAN | 1 | -1135.5 |
| g134 | CAPTURE_MEAN | 1 | 1067.94992175274 |
| g134 | CAPTURE_MEDIAN | 1 | 1558 |
| g134 | AFFINE_SAFE_MEAN | 0.968195851143609 | 1033.98468347014 |
| g134 | AFFINE_SAFE | 0.954269277055388 | 1486.75153365229 |
| g134 | GAIN_ONLY | 0.954269277055388 | 0 |

For `a=1`, integer input makes Java rounding equivalent to a constant integer offset: g127 broad mean −851, broad median −1490, capture mean +1310, capture median +1821; g134 broad mean −771, broad median −1135, capture mean +1068, capture median +1558. The half-unit g134 broad median therefore has a rounding convention worth making explicit. The coefficient JSON in the reproduction appendix retains the full binary64 input decimals.

## Real-search mechanism and invariant conditions

`SearchEvaluation` is final and its `State` hierarchy is sealed; no BRN wrapper/injection configuration was available. An external wrapper alone cannot supply transformed scores to the real search. The permitted fallback was a disposable HEAD copy in:

`C:\Users\Central\AppData\Local\Temp\seedv6-brn-calibration-20260924`

Only **two BRN-2 return expressions** in copied `search/evaluation/SearchEvaluation.java` call the external `CalibrationScore.map(mappedScore, board)`: the incremental path and the full-recompute oracle path. The latter keeps focused equivalence checks meaningful. The external helper reads immutable JVM coefficients, rounds, rejects out-of-band values, counts calls/range, and checks calibration-board separation. It never modifies a board or model. The model codecs and accumulators remain original. The exact patch and harness are retained below.

Byte comparison against `git archive HEAD` confirms this is the copy's only changed existing file. Qsearch, alpha-beta, iterative deepening, move ordering, pruning, TT implementation, NNUE and all other copied files are byte-identical to HEAD. Authoritative source was never edited. Evaluation changes can change the realized tree, hash moves and history updates naturally; the move-ordering algorithm itself is unchanged.

Primary settings match the accepted report: **depth 4**, one worker, ordinary iterative deepening, fresh private **262,144-entry TT** per request, singleton root game history, **1,000,000 cumulative entered-node cap / 60-second cap**. Neural search uses mate-distance bounds only; aspiration, razoring and futility remain disabled. Qsearch's existing soft limit 16/check-evasion rules remain unchanged. No request terminated by time limit. `QsearchDecisionTrace` is the existing read-only observer with NNUE shadow stride 1, detached position stride 101, limit 12,000; the sample limit was not hit.

Qnodes are entered qsearch **children**, excluding main leaves reused as qsearch roots. Q% is `q/(main+q)`. Stand-pat rate is `cutoffs/eligible attempts`, not cutoffs/qnodes. Typical q-ply includes qsearch roots. Timings are secondary single observations with JIT, shadow tracing and incidental machine load; they are not warmed throughput comparisons.

Auxiliary depth-2 requests compare raw and calibrated evaluation at the **same** completed depth to expose work hidden by g127's cap. They are labeled separately and never substituted for depth-4 success.

### Search-test fixtures

The six fixtures are those in the preceding unit. Five come from `SearchBenchmark`; fianchetto comes from `Brn2DiagnosticCorpus`. Opening is the strict quiet control (no root captures); fianchetto is only relatively quiet. No fixture contributes calibration data.

| Fixture | FEN |
| --- | --- |
| Kiwipete | `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1` |
| Exchanges | `4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1` |
| Tactical queen | `4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1` |
| En passant | `4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1` |
| Fianchetto | `r1bq1rk1/ppp1bppp/2n2n2/3pp3/8/1P1P1NP1/PBP1PPBP/RN1Q1RK1 w - - 0 8` |
| Opening | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` |

## Primary Kiwipete results

`CAP` is node-cap termination, with counters from the entire prefix including its incomplete iteration. Other numeric rows completed depth 4. `BAND` has no accepted result: see the failure table. Completed depth and root score/move refer to the last fully completed iteration.

| Model | Variant | Depth / status | Total | Qnodes | Q% | SP attempts | SP cutoffs | SP% | Q-ply mean / median / max | Score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| g127 | RAW | 2 / CAP | 1,000,000 | 999,635 | 99.96 | 773,302 | 286,120 | 37.00 | 12.14 / 12 / 19 | -442 / e1g1 | 9.896 |
| g127 | ROOT_MEAN | 2 / CAP | 1,000,000 | 999,787 | 99.98 | 780,300 | 271,078 | 34.74 | 12.15 / 12 / 19 | -1293 / e1g1 | 10.682 |
| g127 | ROOT_MEDIAN | BAND | — | — | — | — | — | — | — | — | — |
| g127 | CAPTURE_MEAN | 2 / CAP | 1,000,000 | 999,589 | 99.96 | 746,998 | 279,555 | 37.42 | 12.51 / 13 / 19 | 868 / e1g1 | 10.223 |
| g127 | CAPTURE_MEDIAN | 3 / CAP | 1,000,000 | 997,308 | 99.73 | 770,550 | 307,941 | 39.96 | 11.95 / 12 / 19 | 3011 / e1g1 | 10.124 |
| g127 | AFFINE_SAFE_MEAN | 2 / CAP | 1,000,000 | 999,589 | 99.96 | 747,063 | 279,581 | 37.42 | 12.51 / 13 / 19 | 834 / e1g1 | 11.037 |
| g127 | AFFINE_SAFE | 3 / CAP | 1,000,000 | 997,308 | 99.73 | 770,550 | 307,941 | 39.96 | 11.95 / 12 / 19 | 2851 / e1g1 | 10.171 |
| g127 | GAIN_ONLY | 2 / CAP | 1,000,000 | 999,635 | 99.96 | 773,198 | 286,011 | 36.99 | 12.14 / 12 / 19 | -419 / e1g1 | 10.046 |
| g134 | RAW | 4 / OK | 218,790 | 209,297 | 95.66 | 180,919 | 80,896 | 44.71 | 9.25 / 10 / 19 | 10036 / e1g1 | 2.864 |
| g134 | ROOT_MEAN | 4 / OK | 223,954 | 215,265 | 96.12 | 182,316 | 80,278 | 44.03 | 9.70 / 10 / 19 | 10654 / e1g1 | 2.655 |
| g134 | ROOT_MEDIAN | 4 / OK | 271,980 | 263,291 | 96.81 | 221,710 | 94,082 | 42.43 | 9.86 / 11 / 19 | 10290 / e1g1 | 3.539 |
| g134 | CAPTURE_MEAN | BAND | — | — | — | — | — | — | — | — | — |
| g134 | CAPTURE_MEDIAN | BAND | — | — | — | — | — | — | — | — | — |
| g134 | AFFINE_SAFE_MEAN | 4 / OK | 169,311 | 160,327 | 94.69 | 137,611 | 64,579 | 46.93 | 9.41 / 10 / 19 | 9660 / e1g1 | 2.580 |
| g134 | AFFINE_SAFE | 4 / OK | 284,040 | 273,667 | 96.35 | 223,746 | 104,039 | 46.50 | 10.37 / 11 / 19 | 9960 / c3b5 | 3.274 |
| g134 | GAIN_ONLY | 4 / OK | 218,790 | 209,297 | 95.66 | 180,919 | 80,896 | 44.71 | 9.25 / 10 / 19 | 9577 / e1g1 | 2.743 |
| NNUE | RAW | 4 / OK | 41,435 | 32,300 | 77.95 | 36,191 | 29,281 | 80.91 | 3.43 / 1 / 17 | 20697 / e1g1 | 0.338 |

The corresponding primary tree-expansion counters provide another view. Captures include capture evasions; tactical moves may include promotions and evasions may be quiet. The capped g127 populations have different completed work and cannot be treated as matched full-depth trees.

| Model | Variant | Observed q-positions | Capture entries | Captures/q-position | Tactical moves | Evasions | Soft-limit encounters |
| --- | --- | --- | --- | --- | --- | --- | --- |
| g127 | RAW | 999,954 | 839,567 | 0.840 | 784,981 | 214,654 | 104,775 |
| g127 | AFFINE_SAFE | 999,813 | 855,014 | 0.855 | 786,041 | 211,267 | 104,552 |
| g134 | RAW | 216,180 | 183,267 | 0.848 | 170,385 | 38,912 | 10,750 |
| g134 | AFFINE_SAFE_MEAN | 166,847 | 139,890 | 0.838 | 129,712 | 30,615 | 10,268 |
| NNUE | RAW | 38,843 | 29,536 | 0.760 | 27,341 | 4,959 | 88 |

The strongest completed g134 depth-4 result among these fixed coefficients is **AFFINE_SAFE_MEAN**. It saves **48,970 qnodes (23.40%)** and improves cutoff rate by **2.21 percentage points**, but leaves **128,027 qnodes above NNUE**. Of the baseline excess over NNUE, **27.67%** is removed and **72.33%** remains; this is a descriptive benchmark gap, not a decomposition of true learned error. Its mean q-ply is **9.41**, versus raw **9.25** and NNUE **3.43**; the remaining tree is still deep. The maximum remains 19 versus NNUE 17.

For g127, median rather than mean produces the stronger progress: one more completed iterative-deepening depth before the cap and a **2.96-point** eligible-cutoff increase. It still does not finish depth 4, and the cap prevents quantifying its completed depth-4 saving or a full causal fraction. Mean calibration's capped cutoff improvement is just **0.42 points**. Both remain far below NNUE's 80.91%.

Mean/median are not interchangeable. On g134 a center difference of about **490 units** switches the result from a 23.40% qnode reduction to a 30.76% increase, despite both variants increasing the aggregate stand-pat cutoff rate. The median tree changes the root move to `c3b5`, while the mean retains `e1g1`. This shows that cutoff percentages alone cannot predict total expansion: changed windows and returned values select different descendants and main-search paths.

`GAIN_ONLY` produces **exactly the raw qnode count on every fixture for both checkpoints**. Minor integer-boundary cutoff differences are possible (g127 Kiwipete raw 286,120 versus gain-only 286,011), but there is no node benefit. Wherever pure capture centering and its safe affine counterpart both produced results, their qnode counts agree, including the completed depth-2 probes. The modest contraction's demonstrated benefit is preserving score-band integrity, not curing qsearch expansion.

### Explicit score-band failures

These are diagnostic non-results, not silently capped/clipped successes. The allowed linear safe branch already defined above enabled continued comparison without widening scope. The repeated g134 median failure in an untraced replay is the same offending value.

| Model | Variant | Fixture | Failure |
| --- | --- | --- | --- |
| g127 | CAPTURE_MEAN | quiet-fianchetto | Calibration outside normal score band: raw=31357 mapped=32667 |
| g127 | CAPTURE_MEDIAN | quiet-fianchetto | Calibration outside normal score band: raw=30969 mapped=32790 |
| g127 | ROOT_MEDIAN | middlegame-kiwipete | Calibration outside normal score band: raw=-31119 mapped=-32609 |
| g134 | CAPTURE_MEAN | middlegame-kiwipete | Calibration outside normal score band: raw=31489 mapped=32557 |
| g134 | CAPTURE_MEDIAN | middlegame-kiwipete | Calibration outside normal score band: raw=31125 mapped=32683 |
| g134 | CAPTURE_MEDIAN | quiet-fianchetto | Calibration outside normal score band: raw=30968 mapped=32526 |

### Matched completed depth-2 probes

These establish causal effects at a common completed horizon; they do not repair the primary depth-4 failure. NNUE's depth-2 baseline was repeated identically in all three g134 probe processes.

| Model | Variant | Total | Qnodes | SP attempts | SP cutoffs | SP% | Mean q-ply | Score / move |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| g127 | AFFINE_SAFE | 377,102 | 376,902 | 295,523 | 121,399 | 41.08 | 11.98 | 1306 / e1g1 |
| g127 | CAPTURE_MEDIAN | 377,102 | 376,902 | 295,523 | 121,399 | 41.08 | 11.98 | 1379 / e1g1 |
| g127 | RAW | 696,864 | 696,663 | 544,886 | 202,522 | 37.17 | 12.06 | -442 / e1g1 |
| g134 | AFFINE_SAFE | 8,264 | 8,076 | 6,980 | 3,576 | 51.23 | 9.37 | 15399 / e1g1 |
| NNUE | RAW | 4,082 | 3,895 | 3,521 | 2,356 | 66.91 | 7.21 | 21568 / e1g1 |
| g134 | CAPTURE_MEDIAN | 8,264 | 8,076 | 6,980 | 3,576 | 51.23 | 9.37 | 16137 / e1g1 |
| g134 | RAW | 31,097 | 30,909 | 25,295 | 10,591 | 41.87 | 10.94 | 14579 / e1g1 |

g127 raw→safe median: **45.90% fewer qnodes**, cutoff **37.17%→41.08% (+3.91 points)**, same `e1g1`. Yet its calibrated 376,902 qnodes are **96.77× NNUE's 3,895** at that horizon. g134 raw→safe median: **73.87% fewer qnodes**, cutoff **41.87%→51.23% (+9.36 points)**, same `e1g1`. The reversal of g134's benefit at depth 4 is a central limit on any proposed default calibration.

## Additional-position effects and root decisions

The table uses each checkpoint's stronger *primary Kiwipete* safe variant descriptively: g127 safe median; g134 safe mean. It does not fit those variants to additional positions. Full variant-by-position metrics are in Appendix B.

| Fixture | Model | Raw → calibrated q | Q change | SP% raw → calibrated | Move raw → calibrated | Score raw → calibrated | NNUE q / SP% |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Exchanges | g127 | 894 → 878 | -1.79% | 64.53 → 67.87 | e4d5 → e4d5 | 11837 → 10004 | 1,419 / 61.01% |
| Exchanges | g134 | 749 → 693 | -7.48% | 68.67 → 70.91 | e4d5 → e4d5 | 11541 → 11194 | 1,419 / 61.01% |
| Tactical queen | g127 | 8 → 8 | +0.00% | 67.16 → 67.16 | e4d5 → e4d5 | 7109 → 8456 | 10 / 70.78% |
| Tactical queen | g134 | 8 → 8 | +0.00% | 62.61 → 62.61 | e4d5 → e4d5 | 6397 → 7228 | 10 / 70.78% |
| En passant | g127 | 17 → 55 | +223.53% | 61.39 → 65.03 | e5d6 → e1e2 | 0 → 1270 | 40 / 61.83% |
| En passant | g134 | 18 → 17 | -5.56% | 64.26 → 77.51 | e5d6 → e1e2 | 0 → 870 | 40 / 61.83% |
| Fianchetto | g127 | 5,642 → 4,576 | -18.89% | 87.04 → 89.80 | g2h1 → g2h1 | -12205 → -9833 | 3,928 / 77.24% |
| Fianchetto | g134 | 13,007 → 7,346 | -43.52% | 53.89 → 68.54 | f3e5 → d1d2 | -18683 → -17141 | 3,928 / 77.24% |
| Opening | g127 | 667 → 657 | -1.50% | 73.08 → 73.60 | h2h3 → h2h3 | -4251 → -2301 | 126 / 67.46% |
| Opening | g134 | 220 → 196 | -10.91% | 74.08 → 75.18 | b1c3 → b1c3 | -872 → 190 | 126 / 67.46% |

Fianchetto improves materially: g127 **−18.89% qnodes**, g134 safe mean **−43.52%**, while g134 safe median gives **−52.72%** (13,007→6,150; SP 53.89%→69.17%). These remain above NNUE's 3,928, and root moves are not uniformly stable: g134 safe mean changes `f3e5→d1d2`, whereas safe median retains `f3e5`. Opening changes are small in absolute terms (g127 saves 10 qnodes; g134 safe mean saves 24). Sparse queen captures retain `e4d5`; already tiny qtrees have little room to improve.

En-passant exposes a qualitative decision change in both checkpoints: raw selects `e5d6` with returned score 0; the positive-offset variants select `e1e2` with nonzero score. g127 qnodes increase 17→55 with safe median. This is a diagnostic warning about changed search preferences, not a proven tactical blunder: no oracle value or strength match was evaluated. Search terminal draws remain exact zero, and static-offset transformations do not preserve negamax outcomes across alternating depths or terminal/nonterminal choices.

For the descriptive stronger safe variant, best moves stay the same on **5/6 g127 fixtures** (Kiwipete has different last-completed depths) and **4/6 g134 fixtures**. g134 safe median also retains 4/6, but changes Kiwipete rather than fianchetto. Stable simple capture choices and deterministic replay justify further research; this is insufficient stability or strength evidence for deployment.

## Capture-neighbour analysis: what changes and what cannot

### Perspective correction to an expectation in the prompt

For ordinary same-orientation score differences, a pure offset cancels: `T(y)-T(x)=a*(y-x)`. But the accepted diagnostic's capture delta uses **parent perspective**, with both original evaluations in STM orientation:

`d = -y - x`

Therefore `d_calibrated = -(a*y+b)-(a*x+b) = a*d - 2*b`. With centering `b=-a*c`, this is **`a*(d+2*c)`**, or **`d+2*c`** for a pure offset. Its absolute magnitude need not stay unchanged. The accepted report already states this algebra. Claiming invariant absolute capture delta after STM centering would be incorrect. No policy or task-scope change is needed to report the correct measurement.

Uniform offsets preserve differences/order among STM scores and the signed-delta standard deviation, but move the perspective-corrected delta distribution and can change sign reversals. A positive gain preserves real-valued order, scales differences/standard deviation, and does not change signs except through its intercept. Integer rounding can merge neighbouring scores; it never reverses order. We assert these identities on every retained edge, and report actual Java-rounded transformed statistics below (ideal versus rounded delta differs by at most one score unit).

### Frozen identical-edge comparisons

Repeated the preceding harness on current pristine classes for both checkpoints: **4,221 total edges each = 4,100 captures + 121 quiet root moves**, including 804 immediate same-target recaptures. All edge IDs, board keys/status, BRN raw/fresh/reference outputs and NNUE outputs reproduce the previous raw files exactly. Parent restoration succeeds. Most fixed captures are still Kiwipete-derived (4,055); these are **test data**, never calibration observations.

Also reuse, within this unit, the detached samples from each checkpoint's **RAW actual Kiwipete qsearch tree**: 5,992 eligible capture edges for g127, 1,405 for g134. Every variant and NNUE is compared on identical raw-selected edges. Sampling is every 101st observed q-position, not an independent IID sample. A different calibrated tree is separately measured below.

Offline edge transforms retain arithmetic outputs even when outside the normal band; these rows describe affine geometry, not admissible search results. The real-search guard and its failed requests remain separate. No offline statistic was used to revise a coefficient.

| Checkpoint/tree | Population | Evaluation transform | Edges | Median abs delta | p95 abs delta | Reversals % | Signed-delta SD |
| --- | --- | --- | --- | --- | --- | --- | --- |
| g127 | fixed | RAW | 4,100 | 15,168.00 | 27,250.80 | 65.59 | 7,457.86 |
| g127 | fixed | ROOT_MEAN | 4,100 | 16,855.00 | 28,952.80 | 72.78 | 7,457.86 |
| g127 | fixed | ROOT_MEDIAN | 4,100 | 18,118.00 | 30,230.80 | 75.61 | 7,457.86 |
| g127 | fixed | CAPTURE_MEAN | 4,100 | 12,556.00 | 24,646.50 | 53.29 | 7,457.86 |
| g127 | fixed | CAPTURE_MEDIAN | 4,100 | 11,541.50 | 23,634.05 | 51.17 | 7,457.86 |
| g127 | fixed | AFFINE_SAFE_MEAN | 4,100 | 12,070.00 | 23,692.45 | 53.29 | 7,169.04 |
| g127 | fixed | AFFINE_SAFE | 4,100 | 10,929.00 | 22,380.05 | 51.17 | 7,062.29 |
| g127 | fixed | GAIN_ONLY | 4,100 | 14,363.50 | 25,805.75 | 65.59 | 7,062.30 |
| g127 | fixed | NNUE | 4,100 | 7,700.50 | 24,939.80 | 23.56 | 11,048.80 |
| g127 | rawTree | RAW | 5,992 | 14,901.00 | 29,393.95 | 63.37 | 9,054.47 |
| g127 | rawTree | ROOT_MEAN | 5,992 | 16,578.50 | 31,095.95 | 67.42 | 9,054.47 |
| g127 | rawTree | ROOT_MEDIAN | 5,992 | 17,850.50 | 32,373.95 | 70.26 | 9,054.47 |
| g127 | rawTree | CAPTURE_MEAN | 5,992 | 12,358.00 | 26,773.95 | 56.73 | 9,054.47 |
| g127 | rawTree | CAPTURE_MEDIAN | 5,992 | 11,389.50 | 25,751.95 | 53.54 | 9,054.47 |
| g127 | rawTree | AFFINE_SAFE_MEAN | 5,992 | 11,880.00 | 25,737.50 | 56.73 | 8,703.80 |
| g127 | rawTree | AFFINE_SAFE | 5,992 | 10,785.50 | 24,385.50 | 53.54 | 8,574.21 |
| g127 | rawTree | GAIN_ONLY | 5,992 | 14,110.50 | 27,834.95 | 63.37 | 8,574.20 |
| g127 | rawTree | NNUE | 5,992 | 6,320.50 | 25,800.45 | 20.43 | 11,462.53 |
| g134 | fixed | RAW | 4,100 | 6,566.00 | 14,495.95 | 46.20 | 5,379.97 |
| g134 | fixed | ROOT_MEAN | 4,100 | 7,937.00 | 16,037.95 | 51.73 | 5,379.97 |
| g134 | fixed | ROOT_MEDIAN | 4,100 | 8,618.00 | 16,765.95 | 55.37 | 5,379.97 |
| g134 | fixed | CAPTURE_MEAN | 4,100 | 4,942.00 | 12,481.55 | 39.10 | 5,379.97 |
| g134 | fixed | CAPTURE_MEDIAN | 4,100 | 4,397.50 | 11,805.15 | 36.32 | 5,379.97 |
| g134 | fixed | AFFINE_SAFE_MEAN | 4,100 | 4,785.00 | 12,084.55 | 39.10 | 5,208.85 |
| g134 | fixed | AFFINE_SAFE | 4,100 | 4,196.50 | 11,266.10 | 36.32 | 5,133.93 |
| g134 | fixed | GAIN_ONLY | 4,100 | 6,266.00 | 13,833.90 | 46.20 | 5,133.95 |
| g134 | fixed | NNUE | 4,100 | 7,700.50 | 24,939.80 | 23.56 | 11,048.80 |
| g134 | rawTree | RAW | 1,405 | 11,959.00 | 27,380.20 | 48.61 | 8,861.26 |
| g134 | rawTree | ROOT_MEAN | 1,405 | 13,494.00 | 28,922.20 | 53.31 | 8,861.26 |
| g134 | rawTree | ROOT_MEDIAN | 1,405 | 14,222.00 | 29,650.20 | 55.16 | 8,861.26 |
| g134 | rawTree | CAPTURE_MEAN | 1,405 | 9,899.00 | 25,244.20 | 43.91 | 8,861.26 |
| g134 | rawTree | CAPTURE_MEDIAN | 1,405 | 9,040.00 | 24,264.20 | 40.93 | 8,861.26 |
| g134 | rawTree | AFFINE_SAFE_MEAN | 1,405 | 9,584.00 | 24,441.20 | 43.91 | 8,579.43 |
| g134 | rawTree | AFFINE_SAFE | 1,405 | 8,626.00 | 23,154.40 | 40.93 | 8,456.05 |
| g134 | rawTree | GAIN_ONLY | 1,405 | 11,412.00 | 26,128.20 | 48.61 | 8,456.03 |
| g134 | rawTree | NNUE | 1,405 | 5,087.00 | 24,816.20 | 17.58 | 11,011.43 |

On g127's RAW actual-tree edges, safe median leaves **10,785.5 median delta / 53.54% sign reversals**, versus NNUE **6,320.5 / 20.43%**. On g134's RAW actual-tree edges, the better depth-4 safe mean leaves **9,584 / 43.91%**, versus NNUE **5,087 / 17.58%**. Thus neither fitted offset resolves local directional inconsistency. As in the preceding unit, BRN's signed-delta SD is lower than NNUE's in these samples: the evidence does **not** support describing BRN as higher-variance random noise everywhere. The remaining issue involves zero point, direction and selected local geometry.

For pure offsets the signed-delta SD is unchanged exactly on these integer-score populations. Gain-only preserves the raw reversal rate. Positive affine transforms produce zero ordering inversions; rounding under safe median merges **14 of 2,247** distinct fixed g127 score levels and **12 of 2,238** fixed g134 levels; on raw-tree endpoints it merges **132 of 9,112** and **12 of 2,623**, respectively. This is nondecreasing order preservation, not a claim that every strict integer distinction survives scaling.

### Actual calibrated qsearch trees

Full adjacent-static histograms from the unchanged trace compare each calibrated driver with unmodified NNUE on exactly the same observed parent/child edges. These include tactical captures/promotions with actual statics at both ends; checked/terminal positions are not given fictional evaluations. Counts are not the sampled capture-only counts above.

| Driver | Variant | Paired edges | BRN median | NNUE median | BRN p95 | NNUE p95 |
| --- | --- | --- | --- | --- | --- | --- |
| g127 | RAW | 668,002 | 14,682.00 | 6,127.00 | 29,312.00 | 25,384.00 |
| g127 | AFFINE_SAFE_MEAN | 679,044 | 13,305.00 | 6,120.00 | 27,339.00 | 25,350.00 |
| g127 | AFFINE_SAFE | 665,545 | 11,929.00 | 5,991.00 | 25,345.00 | 25,497.00 |
| g134 | RAW | 146,387 | 12,121.00 | 5,002.00 | 26,987.00 | 24,662.00 |
| g134 | AFFINE_SAFE_MEAN | 111,218 | 10,600.00 | 4,863.00 | 25,035.00 | 24,960.60 |
| g134 | AFFINE_SAFE | 180,971 | 9,554.00 | 4,661.00 | 22,780.50 | 24,888.00 |

Even in the *new* safer-mean g134 tree, median BRN delta is **10,600 versus NNUE 4,863 (2.18×)**. g127 safe median is **11,929 versus 5,991 (1.99×)** in its capped prefix. Calibration changes the explored geometry and removes some directional bias; large local differences persist. The experiment therefore cannot be described as merely relocating windows while leaving all parent/child deltas numerically intact, nor as repairing the learned relative ordering/consistency function.

## Explicit answers to the twelve questions

1. **Does simple centering materially reduce qnodes?** Sometimes. Completed depth-2 reductions are 45.90% (g127 median) and 73.87% (g134 median); g134 safe-mean depth 4 saves 23.40%. g127 depth 4 remains capped, broad-position centering worsens Kiwipete, and g134 safe median depth 4 worsens it. No general restoration is established.
2. **Does it materially increase stand-pat cutoff rate?** Context-dependent: about +3.91/+9.36 points in matched depth-2 g127/g134 median probes; only +2.96 points in g127's capped depth-4 prefix and +2.21 in g134's better completed depth-4 result. Fianchetto improves more strongly. The primary cutoff deficit to NNUE remains large.
3. **Consistent across checkpoints?** No. g127 prefers the frozen median for progress; g134's depth-4 mean is beneficial while its median is harmful. Position and depth sensitivity are as important as checkpoint identity.
4. **Median versus mean materially different?** Yes. g127 median completes depth 3 before cap while mean only completes depth 2. g134 safe mean gives 160,327 qnodes versus safe median 273,667. There is no justified universal preference from this sample.
5. **Is scale adjustment beneficial in addition?** No independent node-reduction benefit is demonstrated. Gain-only qnodes exactly reproduce raw on all twelve model/fixture pairs. Safe contraction preserves the legal score band and reproduces pure-centering qnodes wherever both runs are valid. The fitted unconstrained spread gain would expand scores and is inadmissible without further changes.
6. **Meaningfully closer to NNUE without retraining/qsearch changes?** Modestly on g134's primary reproducer and substantially on selected shallow/control searches, but not close: g134 still 4.96× NNUE qnodes and SP 46.93% versus 80.91%; g127 still capped and its completed calibrated depth-2 tree is 96.77× NNUE's.
7. **Only symptom fixed, deltas intact?** The premise needs the STM correction above. Offsets shift perspective-corrected deltas by twice the center, so some absolute deltas shrink. They do not improve same-orientation relative score geometry or reduce signed-delta spread; substantial excess reversals and typical deltas remain.
8. **How much g127 pathology is distribution bias?** This specific independent median removes 45.90% of its matched depth-2 qnodes and permits one additional completed depth before the depth-4 cap. It cannot quantify the full depth-4 causal share. The much larger prior Kiwipete-specific center was deliberately not used; a claim that 'most' g127 pathology is globally correctable bias remains unsupported.
9. **How much g134 remains after the best tested defensible calibration?** 160,327 qnodes, 4.96× NNUE; 72.33% of the original excess-qnode gap remains. SP deficit remains 33.98 points. Mean q-ply remains 9.41 versus 3.43. Same-tree median capture-adjacent delta remains 2.18× NNUE.
10. **Production calibration layer next?** No default production layer or constants are justified. Population-dependent center, score-band failures, mean/median sensitivity, depth reversal and changed root choices prevent that recommendation. A research seam remains useful, but it must stay isolated until independent target/outcome validation supports a stable rule.
11. **Instead training/architecture local consistency?** The evidence supports making learned local capture-neighbour consistency the next research intervention, with calibration retained as a control. It does not prove an architectural change necessary or rule out a better representative/target-grounded calibration. A global center is insufficient in this bounded experiment.
12. **Stable enough to investigate?** Yes for further research, no for strength/deployment claims. Simple tactical capture choices survive; raw/traced/injected execution is deterministic. Meaningful root choices change in Kiwipete, fianchetto and en-passant depending on variant, and no tactical oracle/strength acceptance was run.

## Recommended next implementation experiment

In a separately authorized, isolated research unit, test a **target-grounded capture/recapture consistency training-loss ablation** alongside the unchanged baseline and these frozen calibration controls. Preserve ordinary value targets and legitimately large winning-capture changes: compare the predicted parent-to-negated-child *delta* against an independently justified teacher/return delta, rather than smoothing all captures or enforcing zero delta. Freeze representative calibration/holdout root families and seeds before training, distinguish quiet/capture/recapture/check transitions, and require held-out tactical value/move checks as well as qsearch improvements at matched completed depths. Establish the reference target's meaning before implementing the loss; NNUE here was a comparison, not an oracle.

If the next work is restricted to frozen models, the appropriate next diagnostic is target-grounded, phase/material-stratified bias measurement on disjoint real-play/validation positions, not another qnode-driven offset sweep. This is a limitation-driven alternative, not required work to finish this unit. No training or architecture implementation occurred here.

## Validation, limitations and skipped work

**Performed:** clean external Java 21 compilation of pristine and disposable paths; direct codec loads; before/after model SHA checks; complete disposable-tree byte comparison; raw injected/unmodified equality on all six fixtures for both checkpoints; trace/no-trace equality on six fixtures for both BRN snapshots and NNUE; calibrated Kiwipete replays; all successful main/q counters and eligible/cutoff/reason accounting; actual-static calibration-key exclusion; detached fresh BRN verification; frozen edge restoration/fresh/reference verification; positive-affine ordering/algebra/rounding assertions; exhaustive bounds/nondecreasing-order checks over all 65,023 integer inputs for RAW, both safe affine variants and gain-only for both checkpoints; final primary-file SHA preservation and report diff checks. Initial report staging exposed CRLF whitespace warnings; only the new report was rewritten with LF and rechecked.

**12/12 focused existing tests passed**, zero skips/aborts/failures: `QsearchDecisionTraceTest` (6), `Brn2SearchIntegrationTest` (2), `NnueScoreMappingTest` (4). These were compiled from the disposable HEAD source and run with the patched RAW mapping, outside the repository, using cached JUnit Jupiter 5.10.3 / Platform 1.10.3, OpenTest4J 1.3.0 and API Guardian 1.1.2. A clean final run used only those JARs and fresh current diagnostic classes, without previous-work-unit engine classes. The two patched BRN evaluation paths are both exercised by integration equivalence checks. NNUE mapping/source was not changed.

**Limits:** six search fixtures; one artificial opening family for fitting; correlated path/edge samples; unvalidated meaning of zero STM population center; no outcome labels or calibrated chess truth; g127 capped primary results; guard-aborted pure-offset requests; finite sampled sign/consistency checks; integer quantization and exact terminal/mate scores; horizon-sensitive search decisions; elapsed times include trace/JIT/load. No probability calibration or playing-strength preservation is established. Static source equality is not presented as runtime verification: real search and focused tests supply the runtime evidence.

**Deliberately skipped:** full/long test suites, training/retraining, self-play generations, candidate promotion, broad tournaments/benchmarks, GUI/browser checks, release/package/build acceptance, full optimizer/store recovery validation. They are outside this evaluator-output diagnostic and unnecessary to measure its controlled search effects. No browser task was needed. Nonlinear remapping, score clipping, checkpoint schema changes and production promotion logic changes were deliberately excluded.

The work unit produces diagnostic evidence and a recommendation. Completion/report commit does not establish user acceptance of this second unit, engine-fix acceptance, strength acceptance or deployment.

**Human actions required after this prompt: None.** The suggested next research unit is not a blocking completion action.

### Validation counts and execution provenance

| Check | Observed result |
| --- | --- |
| g127RawPatchedEquality | 6 |
| g127CAPTURE_MEDIANReplay | 1 |
| g127AFFINE_SAFEReplay | 1 |
| g134RawPatchedEquality | 6 |
| g134CAPTURE_MEDIANReplay | 0 |
| g134AFFINE_SAFEReplay | 1 |
| g134AFFINE_SAFE_MEANReplay | 1 |
| g127FixedEdges | 4221 |
| g134FixedEdges | 4221 |
| sampleChecks | 100538 |
| sampleFitOverlaps | 0 |

Of the detached static records above, **99,936 BRN** records were explicitly asserted equal to fresh reconstruction plus the frozen transform; 602 NNUE sample records were observed separately. Across successfully returned instrumented search requests, **11,136,750 BRN static calls** matched diagnostic evaluation-call counts and passed the all-key calibration exclusion check; aborted prefixes also enforced exclusion but are not included in that total. Fixed-root/child fresh/reference checks cover another **8,454** evaluations. These are repeated observations, not distinct positions. The zero-entry g134 pure-median depth-4 replay means repeated band failure, not passed search equality.

All 539 initial tracked/non-ignored authoritative files retained their captured SHA-256 values. Existing inherited changes remain outside the report-only commit. The report is staged and diff-checked alone; the final commit SHA belongs in the completion response because a report cannot contain its own commit hash. No push/deployment is authorized or performed.

## Appendix A — exact temporary patch, harness and frozen coefficients

The shared `CaptureDiagnostic.java` dependency is Appendix A of the accepted preceding report (extract its Java block outside the repository). Its `POSITIONS`, sorted `legal`, `capture`, `play` and `sha` methods are reused unchanged; no existing class/build output is required. `DiagnosticReport` is the unchanged repository JSON writer. The following code is report evidence, not installed product code.

```diff
--- HEAD/SearchEvaluation.java
+++ disposable/SearchEvaluation.java
@@ -179 +179 @@
-        @Override public int evaluate(long[] board, int ply) { return BrnScoreMapping.map(stack[ply].evaluate(board)); }
+        @Override public int evaluate(long[] board, int ply) { return com.ohinteractive.seedv6.tools.search.CalibrationScore.map(BrnScoreMapping.map(stack[ply].evaluate(board)), board); }
@@ -193 +193 @@
-            return BrnScoreMapping.map(definition.brn2.evaluateReference(board, scratch));
+            return com.ohinteractive.seedv6.tools.search.CalibrationScore.map(BrnScoreMapping.map(definition.brn2.evaluateReference(board, scratch)), board);
```

### CalibrationScore.java

```java
package com.ohinteractive.seedv6.tools.search;
public final class CalibrationScore {
 public static final double A=Double.parseDouble(System.getProperty("brn.diag.a","1"));
 public static final double B=Double.parseDouble(System.getProperty("brn.diag.b","0"));
 public static final java.util.Set<Long> fitKeys=new java.util.HashSet<>();
 public static long calls;public static int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
 public static void reset(){calls=0;min=Integer.MAX_VALUE;max=Integer.MIN_VALUE;}
 public static int map(int raw,long[] board){
  if(fitKeys.contains(board[com.ohinteractive.seedv6.core.Board.KEY]))throw new AssertionError("Calibration/test evaluation overlap");
  if(!(A>0)||!Double.isFinite(A)||!Double.isFinite(B))throw new IllegalArgumentException("Invalid affine coefficients");
  long mapped=Math.round(A*raw+B);
  if(Math.abs(mapped)>32511)throw new IllegalStateException("Calibration outside normal score band: raw="+raw+" mapped="+mapped);
  calls++;min=Math.min(min,(int)mapped);max=Math.max(max,(int)mapped);return (int)mapped;
 }
}
```

### CalibrationCorpus.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class CalibrationCorpus {
 public static void main(String[] args)throws Exception {
  var b1=SearchEvaluation.brn2(Brn2Codec.decodeModel(Files.readAllBytes(Path.of(args[0])))).newState(1);
  var b2=SearchEvaluation.brn2(Brn2Codec.decodeModel(Files.readAllBytes(Path.of(args[1])))).newState(1);
  SearchEvaluation.State n;try(var in=Files.newInputStream(Path.of(args[2]))){n=SearchEvaluation.incremental(NnueNetworkCodec.read(in)).newState(1);}
  Set<Long> seen=new HashSet<>(),test=new HashSet<>();for(var p:CaptureDiagnostic.POSITIONS)test.add(Board.fromFen(p[1])[Board.KEY]);
  var records=new ArrayList<Map<String,Object>>(); int failed=0;
  for(int path=0;path<64;path++) {
   var rng=new SplittableRandom(20260924L+path);
   long[] board=Board.fromFen("r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3");
   var staged=new ArrayList<Map<String,Object>>();var keys=new ArrayList<Long>();
   for(int ply=1;ply<=65;ply++) {
    var moves=CaptureDiagnostic.legal(board);if(moves.length==0)break;
    board=CaptureDiagnostic.play(board,moves[rng.nextInt(moves.length)]);
    if(ply==16||ply==17||ply==32||ply==33||ply==48||ply==49||ply==64||ply==65) {
     if(CaptureDiagnostic.legal(board).length==0||test.contains(board[Board.KEY])||seen.contains(board[Board.KEY]))break;
     b1.initialize(board,0);b2.initialize(board,0);n.initialize(board,0);
     staged.add(fields("path",path,"ply",ply,"board",Arrays.stream(board).boxed().toList(),"key",Long.toHexString(board[Board.KEY]),"white",Board.player((int)board[Board.STATUS])==0,"g127",b1.evaluate(board,0),"g134",b2.evaluate(board,0),"nnue",n.evaluate(board,0)));
     keys.add(board[Board.KEY]);
    }
   }
   // Retain whole paths only: identical path weight and exactly balanced STM.
   if(staged.size()==8){records.addAll(staged);seen.addAll(keys);}else failed++;
  }
  try(var out=new PrintWriter(Files.newBufferedWriter(Path.of(args[3]),StandardOpenOption.CREATE_NEW))){for(var r:records){write(out,"type","calibration","sample",r);
   @SuppressWarnings("unchecked") var longs=(List<Long>)r.get("board");long[] board=longs.stream().mapToLong(Long::longValue).toArray();
   for(long move:CaptureDiagnostic.legal(board))if(CaptureDiagnostic.capture(board,move)) {
    long[] child=CaptureDiagnostic.play(board,move);b1.initialize(child,0);b2.initialize(child,0);n.initialize(child,0);
    write(out,"type","calibrationEdge","path",r.get("path"),"parentKey",r.get("key"),"childKey",Long.toHexString(child[Board.KEY]),"parent",fields("g127",r.get("g127"),"g134",r.get("g134"),"nnue",r.get("nnue")),"child",fields("g127",b1.evaluate(child,0),"g134",b2.evaluate(child,0),"nnue",n.evaluate(child,0)));
   }
  }write(out,"type","end","boards",records.size(),"failedPaths",failed,"uniqueKeys",seen.size());}
  System.out.println("Frozen boards="+records.size()+" paths="+(records.size()/8)+" omittedIncompletePaths="+failed);
 }
}
```

### CalibrationSearch.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.iterative.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class CalibrationSearch {
 static Brn2Model model;static SearchEvaluation bd,nd;static PrintWriter out;static String label,variant;
 static Brn2Accumulator fresh;static SearchEvaluation.State ns;static long checks;static Set<Long> fitKeys=new HashSet<>();static long overlaps;
 static Map<String,Object> values(long[] board){fresh.rebuild(board);int raw=BrnScoreMapping.map(fresh.evaluate(board));ns.initialize(board,0);return fields("brn",raw,"nnue",ns.evaluate(board,0),"key",Long.toHexString(board[Board.KEY]));}
 static void search(String[] p,boolean brn,boolean traced,int depth){
  long[] board=Board.fromFen(p[1]),before=board.clone();String driver=brn?label:"NNUE";
  CalibrationScore.reset();
  var trace=traced?new QsearchDecisionTrace(nd,1,101,12000):null;
  var worker=trace==null?new AlphaBetaPvsSearch(brn?bd:nd,1<<18):new AlphaBetaPvsSearch(brn?bd:nd,1<<18,trace);
  try(var engine=new IterativeDeepeningSearch(worker)){
   long start=System.nanoTime();var control=SearchControl.controlled(1_000_000,start,60_000_000_000L,TimeSource.SYSTEM);
   var result=engine.search(new SearchRequest(board,GameHistory.initial(board),depth,SearchObserver.NONE,control,true));
   long elapsed=System.nanoTime()-start;var d=result.diagnostics();var n=d.worker().nodes();var q=d.worker().qsearch();var r=result.lastCompletedResult();
   if(!Arrays.equals(board,before)||control.nodes()!=d.totalEnteredNodes())throw new AssertionError("Accounting/restoration");
   write(out,"type","search","model",driver,"variant",brn?variant:"RAW","position",p[0],"trace",traced,"depth",depth,"completedDepth",r==null?0:r.depth(),"status",result.targetDepthCompleted()?"COMPLETED":control.termination().toString(),"nodes",control.nodes(),"main",n.mainNodes(),"q",n.qNodes(),"maxQply",n.maximumQply(),"evaluations",n.evaluationCalls(),"standPatCutoffs",q.standPatCutoffs(),"checkedQ",q.checkedQNodes(),"tacticalMoves",q.tacticalMovesSearched(),"evasionMoves",q.evasionMovesSearched(),"softLimit",q.softDepthLimitEncounters(),"score",r==null?null:r.score(),"move",r==null?null:Move.coordinate(r.bestMove()),"elapsedNs",elapsed,"transformCalls",CalibrationScore.calls,"minTransformed",CalibrationScore.calls==0?null:CalibrationScore.min,"maxTransformed",CalibrationScore.calls==0?null:CalibrationScore.max,"workerDiagnostics",d.worker().toString());
   System.err.println(driver+" "+variant+" "+p[0]+" trace="+traced+" d="+(r==null?0:r.depth())+" q="+n.qNodes());
   if(trace!=null){
    if(((Number)trace.summary().get("observedCountedQChildren")).longValue()!=n.qNodes())throw new AssertionError("Trace count");
    write(out,"type","trace","model",driver,"variant",brn?variant:"RAW","position",p[0],"depth",depth,"metrics",trace.summary());
    for(var s:trace.positionSamples())if(s.driver()!=null){
     var v=values(s.board());int expected=brn?(int)Math.round(CalibrationScore.A*(int)v.get("brn")+CalibrationScore.B):(int)v.get("nnue");
     if(brn&&s.driver()!=expected)throw new AssertionError("Incremental calibration/fresh mismatch");checks++;
     if(fitKeys.contains(s.board()[Board.KEY]))overlaps++;
     boolean cap=s.parentBoard()!=null&&Long.bitCount(s.parentBoard()[0]|s.parentBoard()[1]|s.parentBoard()[2])==Long.bitCount(s.board()[0]|s.board()[1]|s.board()[2])+1;
     write(out,"type","sample","model",driver,"variant",brn?variant:"RAW","position",p[0],"depth",depth,"ordinal",s.ordinal(),"qply",s.qply(),"capture",cap,"allowed",s.standPatAllowed(),"static",s.driver(),"values",v,"parent",s.parentBoard()==null?null:values(s.parentBoard()));
    }
   }
  }catch(IllegalStateException ex){if(!ex.getMessage().startsWith("Calibration outside"))throw ex;write(out,"type","bandFailure","model",driver,"variant",variant,"position",p[0],"trace",traced,"error",ex.getMessage());System.err.println(ex.getMessage());}
 }
 public static void main(String[] args)throws Exception{
  Path bp=Path.of(args[0]),np=Path.of(args[1]);label=args[3];variant=args[4];String mode=args[5];
  model=Brn2Codec.decodeModel(Files.readAllBytes(bp));bd=SearchEvaluation.brn2(model);try(var in=Files.newInputStream(np)){nd=SearchEvaluation.incremental(NnueNetworkCodec.read(in));}fresh=new Brn2Accumulator(model);ns=nd.newState(1);
  for(String line:Files.readAllLines(Path.of(args[6])))fitKeys.add(Long.parseUnsignedLong(line,16));CalibrationScore.fitKeys.addAll(fitKeys);
  try(var writer=new PrintWriter(Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW))){out=writer;
   write(out,"type","run","model",label,"variant",variant,"a",CalibrationScore.A,"b",CalibrationScore.B,"brnSha",CaptureDiagnostic.sha(bp),"nnueSha",CaptureDiagnostic.sha(np));
   if(mode.equals("all"))for(var p:CaptureDiagnostic.POSITIONS)search(p,true,true,4);
   if(mode.equals("rawcheck"))for(var p:CaptureDiagnostic.POSITIONS){search(p,true,true,4);search(p,true,false,4);if(label.equals("g134")){search(p,false,true,4);search(p,false,false,4);}}
   if(mode.equals("replay"))search(CaptureDiagnostic.POSITIONS[0],true,false,4);
   if(mode.equals("depth2")){search(CaptureDiagnostic.POSITIONS[0],true,true,2);if(label.equals("g134"))search(CaptureDiagnostic.POSITIONS[0],false,true,2);}
   write(out,"type","end","checks",checks,"sampleFitKeyOverlaps",overlaps,"brnShaAfter",CaptureDiagnostic.sha(bp),"nnueShaAfter",CaptureDiagnostic.sha(np));
  }
 }
}
```

### Frozen coefficient JSON

The first JSON was fixed before searches; the separate second JSON is the range-safe mean extension of the same already frozen center.

```json
{
  "g127": {
    "RAW": {
      "a": 1.0,
      "b": 0.0,
      "calibrationBandExceedances": 0
    },
    "ROOT_MEAN": {
      "a": 1.0,
      "b": -851.1328125,
      "center": 851.1328125,
      "calibrationBandExceedances": 0
    },
    "ROOT_MEDIAN": {
      "a": 1.0,
      "b": -1490.0,
      "center": 1490.0,
      "calibrationBandExceedances": 5
    },
    "CAPTURE_MEAN": {
      "a": 1.0,
      "b": 1309.8908450704225,
      "center": -1309.8908450704225,
      "calibrationBandExceedances": 5
    },
    "CAPTURE_MEDIAN": {
      "a": 1.0,
      "b": 1821.0,
      "center": -1821.0,
      "calibrationBandExceedances": 14
    },
    "AFFINE_SAFE": {
      "a": 0.9469591052079692,
      "b": 1724.412530583712,
      "center": -1821.0,
      "unconstrainedIqrGain": 1.728482263397836,
      "maximumFullBandSafeGain": 0.9469591052079692,
      "calibrationBandExceedances": 0
    },
    "GAIN_ONLY": {
      "a": 0.9469591052079692,
      "b": 0.0,
      "calibrationBandExceedances": 0
    }
  },
  "g134": {
    "RAW": {
      "a": 1.0,
      "b": 0.0,
      "calibrationBandExceedances": 0
    },
    "ROOT_MEAN": {
      "a": 1.0,
      "b": -771.07421875,
      "center": 771.07421875,
      "calibrationBandExceedances": 0
    },
    "ROOT_MEDIAN": {
      "a": 1.0,
      "b": -1135.5,
      "center": 1135.5,
      "calibrationBandExceedances": 0
    },
    "CAPTURE_MEAN": {
      "a": 1.0,
      "b": 1067.9499217527386,
      "center": -1067.9499217527386,
      "calibrationBandExceedances": 0
    },
    "CAPTURE_MEDIAN": {
      "a": 1.0,
      "b": 1558.0,
      "center": -1558.0,
      "calibrationBandExceedances": 13
    },
    "AFFINE_SAFE": {
      "a": 0.9542692770553876,
      "b": 1486.7515336522938,
      "center": -1558.0,
      "unconstrainedIqrGain": 1.7970097640358014,
      "maximumFullBandSafeGain": 0.9542692770553876,
      "calibrationBandExceedances": 0
    },
    "GAIN_ONLY": {
      "a": 0.9542692770553876,
      "b": 0.0,
      "calibrationBandExceedances": 0
    }
  }
}
```

```json
{
  "g127": {
    "AFFINE_SAFE_MEAN": {
      "a": 0.9612697710692814,
      "b": 1259.1584727665925,
      "center": -1309.8908450704225
    }
  },
  "g134": {
    "AFFINE_SAFE_MEAN": {
      "a": 0.9681958511436085,
      "b": 1033.984683470143,
      "center": -1067.9499217527386
    }
  }
}
```

### Reproduction sequence

Use the pinned model paths above, Java `21+35-2513`, and a new output directory because harness outputs use `CREATE_NEW`. From the repository root, compile the unmodified sources and external classes with:

```powershell
javac -d "$diag/pristine-classes" -sourcepath app/src/main/java "$diag/CaptureDiagnostic.java" "$diag/CalibrationCorpus.java" "$diag/CalibrationScore.java" "$diag/CalibrationSearch.java"
java -Xmx1024m -cp "$diag/pristine-classes" com.ohinteractive.seedv6.tools.search.CalibrationCorpus $g127 $g134 $nnue "$diag/calibration-edges.jsonl"
# Freeze roots/capture endpoint means, interpolated medians and IQRs as defined above.
# fit-keys.txt is the sorted unique hex root/childKey set, one key per line.
# Archive pinned HEAD outside the primary repository and apply only the two-line patch above.
javac -d "$diag/calibrated-classes" -sourcepath "$diag/copy/app/src/main/java" "$diag/CaptureDiagnostic.java" "$diag/CalibrationScore.java" "$diag/CalibrationSearch.java"
java -Xms256m -Xmx1024m "-Dbrn.diag.a=$a" "-Dbrn.diag.b=$b" -cp "$diag/calibrated-classes" com.ohinteractive.seedv6.tools.search.CalibrationSearch $brn $nnue $output $modelLabel $variant all "$diag/fit-keys.txt"
```

`all` runs all six BRN depth-4 fixtures; `rawcheck` adds trace-free runs and NNUE references when label is g134; `depth2` runs the labeled auxiliary Kiwipete probes; `replay` runs trace-free Kiwipete depth 4. The suite ran pristine `rawcheck` and patched RAW `all` for each checkpoint; `all` for each nonraw variant; depth2 for RAW/CAPTURE_MEDIAN/AFFINE_SAFE; replay for CAPTURE_MEDIAN/AFFINE_SAFE and g134 AFFINE_SAFE_MEAN. NNUE references are RAW even when the containing g134 probe process has a calibration variant label. No NNUE output is transformed.

Capture-only fixed-edge reruns use pristine `CaptureDiagnostic` with mode `edges`, as in the accepted report. Quantiles interpolate `(n-1)*p`. Same-raw-tree capture samples require incoming occupancy decrease of one, a real static parent and stand-pat eligibility at the child. The exact analysis script follows so the retained table numbers do not depend on prose interpretation.

```python
from pathlib import Path
import json,math,statistics,hashlib
p=Path(__file__).parent
fits=json.loads((p/'fits.json').read_text())
for m,v in json.loads((p/'fits-safe-mean.json').read_text()).items():fits[m].update(v)
def read(name):return [json.loads(x) for x in (p/name).read_text().splitlines()]
def q(x,f):
 a=sorted(x);r=(len(a)-1)*f;i=int(r);return a[i]+(a[min(i+1,len(a)-1)]-a[i])*(r-i) if a else None
def histq(groups,f):
 n=sum(v['nodes'] for v in groups.values());r=(n-1)*f
 def at(k):
  total=0
  for key,val in sorted(groups.items(),key=lambda t:int(t[0])):
   total+=val['nodes']
   if total>k:return int(key)
 return at(math.floor(r))*(1-r%1)+at(math.ceil(r))*(r%1)
def strip(row):return {k:v for k,v in row.items() if k not in ['elapsedNs','trace','transformCalls','minTransformed','maxTransformed']}
def searchmap(rows):return {(x['model'],x['position'],x['depth'],x['trace']):x for x in rows if x['type']=='search'}
verification={};main=[];depth2=[];allfiles={};failures=[]
for f in sorted(p.glob('g*-*.jsonl')):
 if 'fixed-edges' in f.name:continue
 try:
  rows=read(f.name)
  if rows[-1]['type']!='end':continue
 except Exception:continue
 allfiles[f.name]=rows
 failures.extend(x for x in rows if x['type']=='bandFailure')
 traces={(x['model'],x['position'],x['depth']):x['metrics'] for x in rows if x['type']=='trace'}
 for x in rows:
  if x['type']!='search' or not x['trace']:continue
  t=traces[x['model'],x['position'],x['depth']];a=t['all'];assert a['driverStandPatBetaCutoffs']==x['standPatCutoffs'];assert t['observedCountedQChildren']==x['q'];assert a['standPatAllowed']==a['both']+a['driverOnly']+a['shadowOnly']+a['neither']
  r=dict(x,attempts=a['standPatAllowed'],rate=100*x['standPatCutoffs']/a['standPatAllowed'],qPercent=100*x['q']/x['nodes'],meanQply=sum(int(k)*v['nodes'] for k,v in t['byQply'].items())/t['observed'],medianQply=histq(t['byQply'],.5),captureEntries=sum(v['nodes'] for k,v in t['byIncomingMove'].items() if '/capture' in k),observedQ=t['observed'],adjacent=t['adjacentStaticAbsoluteDelta'])
  if f.name.endswith('-depth2.jsonl'):depth2.append(r)
  elif f.name.endswith('-patched.jsonl') or f.name.endswith('-main.jsonl') or (x['model']=='NNUE' and f.name.endswith('-pristine.jsonl')):main.append(r)
for m in fits:
 pristine=searchmap(allfiles[f'{m}-RAW-pristine.jsonl']);patched=searchmap(allfiles[f'{m}-RAW-patched.jsonl'])
 for k,x in pristine.items():
  if k[-1]:assert strip(x)==strip(pristine[(*k[:-1],False)])
 for k,x in patched.items():assert strip(x)==strip(pristine[k])
 verification[m+'RawPatchedEquality']=len(patched)
 for v in ['CAPTURE_MEDIAN','AFFINE_SAFE','AFFINE_SAFE_MEAN']:
  name=f'{m}-{v}-replay.jsonl'
  if name not in allfiles:continue
  rp=searchmap(allfiles[name]);mr=searchmap(allfiles[f'{m}-{v}-main.jsonl'])
  for k,x in rp.items():assert strip(x)==strip(mr[(*k[:-1],True)])
  verification[m+v+'Replay']=len(rp)
for m in fits:
 raw=read(f'{m}-fixed-edges.jsonl');old=Path('C:/Users/Central/AppData/Local/Temp/seedv6-brn-diagnostic-20260924')/('t2-g127-edges.jsonl' if m=='g127' else 't2-g134-primary.jsonl')
 if old.exists():
  before=[json.loads(x) for x in old.read_text().splitlines() if json.loads(x)['type']=='edge'];now=[x for x in raw if x['type']=='edge'];assert before==now
 verification[m+'FixedEdges']=sum(x['type']=='edge' for x in raw)
verification['sampleChecks']=sum(r[-1]['checks'] for r in allfiles.values())
verification['sampleFitOverlaps']=sum(r[-1]['sampleFitKeyOverlaps'] for r in allfiles.values())
assert verification['sampleFitOverlaps']==0
def stats(pairs,a,b):
 rd=[-y-x for x,y,nx,ny in pairs];ideal=[-(a*y+b)-(a*x+b) for x,y,nx,ny in pairs]
 mappedPairs=[(math.floor(a*x+b+.5),math.floor(a*y+b+.5)) for x,y,nx,ny in pairs];d=[-y-x for x,y in mappedPairs]
 vals=[v for x,y,nx,ny in pairs for v in [x,y]];ordered=sorted(set(vals));mapped=[math.floor(a*v+b+.5) for v in ordered]
 assert all(u<=v for u,v in zip(mapped,mapped[1:]))
 assert all(abs(v-(a*r-2*b))<1e-9 for v,r in zip(ideal,rd))
 rounding=max(abs(x-y) for x,y in zip(d,ideal));assert rounding<=1.00000001
 return dict(edges=len(pairs),meanAbs=statistics.mean(abs(v) for v in d),medianAbs=q([abs(v) for v in d],.5),p95Abs=q([abs(v) for v in d],.95),meanSigned=statistics.mean(d),signedSd=statistics.pstdev(d),reversalPct=100*sum(x*(-y)<0 for x,y in mappedPairs)/len(pairs),stmDiffMaxError=max(abs(((a*y+b)-(a*x+b))-a*(y-x)) for x,y,nx,ny in pairs),uniqueRawScores=len(ordered),newRoundingTies=len(ordered)-len(set(mapped)),maxDeltaRoundingError=rounding)
capture={}
for m in fits:
 fixed=[x for x in read(f'{m}-fixed-edges.jsonl') if x['type']=='edge' and x['capture']]
 tree=[x for x in allfiles[f'{m}-RAW-patched.jsonl'] if x['type']=='sample' and x['position']=='middlegame-kiwipete' and x['capture'] and x['allowed'] and x['parent'] is not None]
 capture[m]={}
 for pop,rows in [('fixed',fixed),('rawTree',tree)]:
  pairs=[(x['parent']['brn'],x['child' if pop=='fixed' else 'values']['brn'],x['parent']['nnue'],x['child' if pop=='fixed' else 'values']['nnue']) for x in rows]
  capture[m][pop]={v:stats(pairs,f['a'],f['b']) for v,f in fits[m].items()}
  capture[m][pop]['NNUE']=stats([(nx,ny,0,0) for x,y,nx,ny in pairs],1,0)
result=dict(main=main,depth2=depth2,capture=capture,verification=verification,failures=failures)
(p/'analysis.json').write_text(json.dumps(result,indent=2))
print('VERIFICATION',verification,'band failures',failures)
for m in ['g127','g134','NNUE']:
 print('\n',m)
 for r in main:
  if r['model']==m and r['position']=='middlegame-kiwipete':print(r['variant'],r['completedDepth'],r['q'],round(r['rate'],3),r['score'],r['move'],round(r['meanQply'],2),r['maxQply'])
for m,pops in capture.items():
 print('\nCAPTURE',m)
 for pop,vv in pops.items():
  for v,s in vv.items():print(pop,v,s['edges'],round(s['medianAbs'],2),round(s['reversalPct'],2),round(s['signedSd'],2))
```

### Temporary evidence hashes

Raw output is disposable. The report retains coefficients, populations/method, exact temporary source and the complete search tables. The following hashes identify the measured files while they remain available; they are not an additional required publication artifact.

| External evidence file | Bytes | SHA-256 |
| --- | --- | --- |
| calibration.jsonl | 128261 | `3b3772258efc586e3246cb74af069e6af71064047bae2a16c8b1a4afb7c18352` |
| calibration-edges.jsonl | 636005 | `e788eda6bdac8aee821a00e11baaf2b5aa6f98586385632e4fee9564716db2d2` |
| fits.json | 2119 | `5f06ad18b66f67b58660983d4eb7c0985ac221b3c6e70770c6dadd46b0dae160` |
| fits-safe-mean.json | 310 | `2802a2bd661974303ec3a439e837286e51b91b88b7edf8652bdc62b0f385b066` |
| fit-keys.txt | 54128 | `cffc09c308c23bd8e735cefbf67890639419226f21c03f5ea1129021d5009fb7` |
| focused-tests-clean.txt | 964 | `115f380bbce465e21e1ad4330688129e94aad33d16cce6012c75f630dafd375f` |
| g127-AFFINE_SAFE-depth2.jsonl | 965524 | `5f37065a36ac8de05dfd4ff779a5d53de3020ec026f9ca85a584efb1b47eaca1` |
| g127-AFFINE_SAFE-main.jsonl | 2614216 | `9df3a0f7b1dedca5d22681ae2f977a7a6d8506c460c1677e6844e073bb4ca182` |
| g127-AFFINE_SAFE-replay.jsonl | 1933 | `ed1782139274cbd1f81a3c9c1a6af11dc8c6ee95c851c8b1e7d490fbe1b596ae` |
| g127-AFFINE_SAFE_MEAN-main.jsonl | 2666839 | `461c86c600c962b46001ad3906b6efb7ec1dbeb2bd3be272883c6ab0d71ca387` |
| g127-CAPTURE_MEAN-main.jsonl | 2573162 | `7997180a901122af47c072e38a48eb9f6f74502acf5f0ac15814e5cd581e4d06` |
| g127-CAPTURE_MEDIAN-depth2.jsonl | 975460 | `bc5f4a71d6eb670d470f7c59b3c2d59b3ec3387fd9eb5c1023d46ba71772effe` |
| g127-CAPTURE_MEDIAN-main.jsonl | 2587187 | `dc6b60c3e3162b791c01bbeab85d5405270114e5e2061f85489e58d501fc4b26` |
| g127-CAPTURE_MEDIAN-replay.jsonl | 1913 | `8d43f0a0459cc93579438c6ac33d527ecd4bb269ae923f8a5d5f991970953db8` |
| g127-fixed-edges.jsonl | 2537147 | `e5c0f2b9084b7d3e03a3f9f04a8c700cfc29f9f39b26789a540e8829c3c90f4b` |
| g127-GAIN_ONLY-main.jsonl | 2615845 | `0dddb06a6374c005e25b2547ac06852abb06db25f3e3e48dff1089be21deb1ed` |
| g127-RAW-depth2.jsonl | 1720091 | `4053efe9469873e45b99fe44c2a732ab8baa0f006ff76a225d6a9358ab37f369` |
| g127-RAW-patched.jsonl | 2571283 | `939647db533fc90f3ba4b257cc055f916b7f2f102a90075a12d7db654155d0d6` |
| g127-RAW-pristine.jsonl | 2579616 | `2d4e7e4cf432c722ad2d42d8dbb215982e2f78b3c861c815b29a28f11d998fe7` |
| g127-ROOT_MEAN-main.jsonl | 2640315 | `9e12acc25afee30deb9b3a85f201b845aa7c094ad91bcc5c314eb813e13f2b5c` |
| g127-ROOT_MEDIAN-main.jsonl | 136907 | `3abecd09ee67a5f191a7dfea31d7dde3ce9bfa8ce97f1870c311f6c51759c3e4` |
| g134-AFFINE_SAFE-depth2.jsonl | 84804 | `5b39e06b68343168dc44a1f8bd5c3dc330e0e90324c941eaa7c880ef8526612d` |
| g134-AFFINE_SAFE-main.jsonl | 832749 | `bb62fee259ee52cdc4dec24de3cad05dedeaaf724d6685d45f865912176bba26` |
| g134-AFFINE_SAFE-replay.jsonl | 1946 | `cbf230d435d7e60cbb33df664baa9ea73dfa1c0a4d4fd6e95fc4fa70177fb1e8` |
| g134-AFFINE_SAFE_MEAN-main.jsonl | 574008 | `5f9461e6491b55508f7653624d95e83bd468ac7ecfb29f7f837c2f01cd2a5c32` |
| g134-AFFINE_SAFE_MEAN-replay.jsonl | 1948 | `a34924c9d597fa75885dd09d6b94af6a7300cbd6fbec856e770fe88912faad0c` |
| g134-CAPTURE_MEAN-main.jsonl | 123504 | `1b8700840b5ed64423ad31ba05b346fdf53c421e778311fee98ff156e2bbd0ab` |
| g134-CAPTURE_MEDIAN-depth2.jsonl | 85013 | `cf06faefc5baee33752d157b47fa7133c1786cab001f8897676f77cb3d5c74f3` |
| g134-CAPTURE_MEDIAN-main.jsonl | 65161 | `e1ac96bb258eaa1221c95b0f53fd9aaef1f0d87b2478b17eb41f5a79387df6db` |
| g134-CAPTURE_MEDIAN-replay.jsonl | 630 | `eeffd1f1dffe60c0aeec88c0df01e1ba6be6f4a8eae11eefb35b1085a214c291` |
| g134-fixed-edges.jsonl | 2529859 | `e17ddc8f1b50a9ed7d6a52f41a6f411b3e052947c2b4e87f6b730b00e2b74087` |
| g134-GAIN_ONLY-main.jsonl | 709095 | `f3b347c87f91d2fb1bc737567b6b98b48160fc09a0b837bc8ea66784d8452b89` |
| g134-RAW-depth2.jsonl | 135438 | `f6e20747b9770ab2dce4c399640cdae252fa29860c2db3d21db1bdfdc0f8620b` |
| g134-RAW-patched.jsonl | 696339 | `c6346989b29ae4a7a91e86b6e178a571e60a10f5a74a049c035b4d31e877d1bc` |
| g134-RAW-pristine.jsonl | 931928 | `9f25157c09d167b7019db5c6eeafb9e00d1f44b65ad52c9ee59465c56e6addaa` |
| g134-ROOT_MEAN-main.jsonl | 724750 | `97b0d1e9c6c4d4f860bd9cc84efce0c0ef63096049591325b53920a82d063278` |
| g134-ROOT_MEDIAN-main.jsonl | 861190 | `a06cea84c77c552f13787e06eb490cd5b11910cbded7b339030a67cc3457ad72` |

## Appendix B — complete depth-4 search result matrix

All numeric rows below are actual completed or capped requests; band failures are listed above instead of being assigned invented node/cutoff results. `ΔQ%` and `ΔSP` compare the same checkpoint/fixture with its RAW baseline; `ΔSP` is percentage points. Capped `ΔQ%` is only a prefix comparison. `Qply` is mean/median/max. Scores use mapped search units. The main text's selected views do not omit any variant from this matrix.

### g127

| Fixture | Variant | Depth | Total | Q | Q% | ΔQ% | SP attempts | SP cuts | SP% | ΔSP | Qply | Soft limit | Score/move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kiwipete | RAW | 2/CAP | 1,000,000 | 999,635 | 99.96 | +0.00 | 773,302 | 286,120 | 37.00 | +0.00 | 12.14/12/19 | 104,775 | -442/e1g1 | 9.896 |
| Kiwipete | ROOT_MEAN | 2/CAP | 1,000,000 | 999,787 | 99.98 | +0.02 | 780,300 | 271,078 | 34.74 | -2.26 | 12.15/12/19 | 104,235 | -1293/e1g1 | 10.682 |
| Kiwipete | CAPTURE_MEAN | 2/CAP | 1,000,000 | 999,589 | 99.96 | -0.00 | 746,998 | 279,555 | 37.42 | +0.42 | 12.51/13/19 | 131,616 | 868/e1g1 | 10.223 |
| Kiwipete | CAPTURE_MEDIAN | 3/CAP | 1,000,000 | 997,308 | 99.73 | -0.23 | 770,550 | 307,941 | 39.96 | +2.96 | 11.95/12/19 | 104,552 | 3011/e1g1 | 10.124 |
| Kiwipete | AFFINE_SAFE_MEAN | 2/CAP | 1,000,000 | 999,589 | 99.96 | -0.00 | 747,063 | 279,581 | 37.42 | +0.42 | 12.51/13/19 | 131,579 | 834/e1g1 | 11.037 |
| Kiwipete | AFFINE_SAFE | 3/CAP | 1,000,000 | 997,308 | 99.73 | -0.23 | 770,550 | 307,941 | 39.96 | +2.96 | 11.95/12/19 | 104,552 | 2851/e1g1 | 10.171 |
| Kiwipete | GAIN_ONLY | 2/CAP | 1,000,000 | 999,635 | 99.96 | +0.00 | 773,198 | 286,011 | 36.99 | -0.01 | 12.14/12/19 | 104,944 | -419/e1g1 | 10.046 |
| Exchanges | RAW | 4 | 2,309 | 894 | 38.72 | +0.00 | 1,562 | 1,008 | 64.53 | +0.00 | 0.73/0/5 | 0 | 11837/e4d5 | 0.017 |
| Exchanges | ROOT_MEAN | 4 | 2,373 | 914 | 38.52 | +2.24 | 1,640 | 1,031 | 62.87 | -1.67 | 0.73/0/6 | 0 | 10986/e4d5 | 0.021 |
| Exchanges | ROOT_MEDIAN | 4 | 2,512 | 1,038 | 41.32 | +16.11 | 1,766 | 1,053 | 59.63 | -4.91 | 0.84/1/6 | 0 | 10347/e4d5 | 0.038 |
| Exchanges | CAPTURE_MEAN | 4 | 2,265 | 861 | 38.01 | -3.69 | 1,520 | 1,018 | 66.97 | +2.44 | 0.70/0/5 | 0 | 11075/e4d5 | 0.020 |
| Exchanges | CAPTURE_MEDIAN | 4 | 2,301 | 878 | 38.16 | -1.79 | 1,553 | 1,054 | 67.87 | +3.34 | 0.71/0/5 | 0 | 10564/e4d5 | 0.016 |
| Exchanges | AFFINE_SAFE_MEAN | 4 | 2,265 | 861 | 38.01 | -3.69 | 1,520 | 1,018 | 66.97 | +2.44 | 0.70/0/5 | 0 | 10646/e4d5 | 0.027 |
| Exchanges | AFFINE_SAFE | 4 | 2,301 | 878 | 38.16 | -1.79 | 1,553 | 1,054 | 67.87 | +3.34 | 0.71/0/5 | 0 | 10004/e4d5 | 0.017 |
| Exchanges | GAIN_ONLY | 4 | 2,309 | 894 | 38.72 | +0.00 | 1,562 | 1,008 | 64.53 | +0.00 | 0.73/0/5 | 0 | 11209/e4d5 | 0.023 |
| Tactical queen | RAW | 4 | 207 | 8 | 3.86 | +0.00 | 134 | 90 | 67.16 | +0.00 | 0.06/0/1 | 0 | 7109/e4d5 | 0.002 |
| Tactical queen | ROOT_MEAN | 4 | 207 | 8 | 3.86 | +0.00 | 134 | 90 | 67.16 | +0.00 | 0.06/0/1 | 0 | 6258/e4d5 | 0.004 |
| Tactical queen | ROOT_MEDIAN | 4 | 207 | 8 | 3.86 | +0.00 | 134 | 90 | 67.16 | +0.00 | 0.06/0/1 | 0 | 5619/e4d5 | 0.003 |
| Tactical queen | CAPTURE_MEAN | 4 | 207 | 8 | 3.86 | +0.00 | 134 | 90 | 67.16 | +0.00 | 0.06/0/1 | 0 | 8419/e4d5 | 0.002 |
| Tactical queen | CAPTURE_MEDIAN | 4 | 207 | 8 | 3.86 | +0.00 | 134 | 90 | 67.16 | +0.00 | 0.06/0/1 | 0 | 8930/e4d5 | 0.002 |
| Tactical queen | AFFINE_SAFE_MEAN | 4 | 207 | 8 | 3.86 | +0.00 | 134 | 90 | 67.16 | +0.00 | 0.06/0/1 | 0 | 8093/e4d5 | 0.003 |
| Tactical queen | AFFINE_SAFE | 4 | 207 | 8 | 3.86 | +0.00 | 134 | 90 | 67.16 | +0.00 | 0.06/0/1 | 0 | 8456/e4d5 | 0.002 |
| Tactical queen | GAIN_ONLY | 4 | 207 | 8 | 3.86 | +0.00 | 134 | 90 | 67.16 | +0.00 | 0.06/0/1 | 0 | 6732/e4d5 | 0.002 |
| En passant | RAW | 4 | 397 | 17 | 4.28 | +0.00 | 259 | 159 | 61.39 | +0.00 | 0.06/0/1 | 0 | 0/e5d6 | 0.003 |
| En passant | ROOT_MEAN | 4 | 337 | 19 | 5.64 | +11.76 | 218 | 130 | 59.63 | -1.76 | 0.08/0/1 | 0 | 0/e5d6 | 0.004 |
| En passant | ROOT_MEDIAN | 4 | 335 | 19 | 5.67 | +11.76 | 216 | 128 | 59.26 | -2.13 | 0.08/0/1 | 0 | 0/e5d6 | 0.005 |
| En passant | CAPTURE_MEAN | 4 | 507 | 41 | 8.09 | +141.18 | 355 | 237 | 66.76 | +5.37 | 0.11/0/1 | 0 | 830/e1e2 | 0.004 |
| En passant | CAPTURE_MEDIAN | 4 | 535 | 55 | 10.28 | +223.53 | 366 | 238 | 65.03 | +3.64 | 0.14/0/1 | 0 | 1341/e1e2 | 0.004 |
| En passant | AFFINE_SAFE_MEAN | 4 | 507 | 41 | 8.09 | +141.18 | 355 | 237 | 66.76 | +5.37 | 0.11/0/1 | 0 | 798/e1e2 | 0.005 |
| En passant | AFFINE_SAFE | 4 | 535 | 55 | 10.28 | +223.53 | 366 | 238 | 65.03 | +3.64 | 0.14/0/1 | 0 | 1270/e1e2 | 0.004 |
| En passant | GAIN_ONLY | 4 | 397 | 17 | 4.28 | +0.00 | 259 | 159 | 61.39 | +0.00 | 0.06/0/1 | 0 | 0/e5d6 | 0.005 |
| Fianchetto | RAW | 4 | 18,096 | 5,642 | 31.18 | +0.00 | 15,708 | 13,673 | 87.04 | +0.00 | 0.52/0/11 | 0 | -12205/g2h1 | 0.264 |
| Fianchetto | ROOT_MEAN | 4 | 18,601 | 6,375 | 34.27 | +12.99 | 16,154 | 13,702 | 84.82 | -2.22 | 0.75/0/14 | 0 | -13056/g2h1 | 0.281 |
| Fianchetto | ROOT_MEDIAN | 4 | 17,510 | 6,145 | 35.09 | +8.92 | 15,150 | 12,643 | 83.45 | -3.59 | 0.79/0/13 | 0 | -13695/g2h1 | 0.277 |
| Fianchetto | AFFINE_SAFE_MEAN | 4 | 17,081 | 4,832 | 28.29 | -14.36 | 14,751 | 13,144 | 89.11 | +2.06 | 0.37/0/7 | 0 | -10473/g2h1 | 0.337 |
| Fianchetto | AFFINE_SAFE | 4 | 16,808 | 4,576 | 27.23 | -18.89 | 14,481 | 13,004 | 89.80 | +2.76 | 0.33/0/6 | 0 | -9833/g2h1 | 0.216 |
| Fianchetto | GAIN_ONLY | 4 | 18,096 | 5,642 | 31.18 | +0.00 | 15,708 | 13,673 | 87.04 | +0.00 | 0.52/0/11 | 0 | -11558/g2h1 | 0.263 |
| Opening | RAW | 4 | 7,171 | 667 | 9.30 | +0.00 | 5,794 | 4,234 | 73.08 | +0.00 | 0.18/0/5 | 0 | -4251/h2h3 | 0.087 |
| Opening | ROOT_MEAN | 4 | 7,202 | 675 | 9.37 | +1.20 | 5,826 | 4,252 | 72.98 | -0.09 | 0.18/0/5 | 0 | -5102/h2h3 | 0.099 |
| Opening | ROOT_MEDIAN | 4 | 7,179 | 675 | 9.40 | +1.20 | 5,802 | 4,226 | 72.84 | -0.24 | 0.18/0/5 | 0 | -5741/h2h3 | 0.096 |
| Opening | CAPTURE_MEAN | 4 | 7,162 | 658 | 9.19 | -1.35 | 5,785 | 4,241 | 73.31 | +0.23 | 0.18/0/5 | 0 | -2941/h2h3 | 0.101 |
| Opening | CAPTURE_MEDIAN | 4 | 7,206 | 657 | 9.12 | -1.50 | 5,830 | 4,291 | 73.60 | +0.53 | 0.17/0/5 | 0 | -2430/h2h3 | 0.105 |
| Opening | AFFINE_SAFE_MEAN | 4 | 7,162 | 658 | 9.19 | -1.35 | 5,785 | 4,241 | 73.31 | +0.23 | 0.18/0/5 | 0 | -2827/h2h3 | 0.121 |
| Opening | AFFINE_SAFE | 4 | 7,206 | 657 | 9.12 | -1.50 | 5,830 | 4,291 | 73.60 | +0.53 | 0.17/0/5 | 0 | -2301/h2h3 | 0.090 |
| Opening | GAIN_ONLY | 4 | 7,171 | 667 | 9.30 | +0.00 | 5,794 | 4,234 | 73.08 | +0.00 | 0.18/0/5 | 0 | -4026/h2h3 | 0.109 |

### g134

| Fixture | Variant | Depth | Total | Q | Q% | ΔQ% | SP attempts | SP cuts | SP% | ΔSP | Qply | Soft limit | Score/move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kiwipete | RAW | 4 | 218,790 | 209,297 | 95.66 | +0.00 | 180,919 | 80,896 | 44.71 | +0.00 | 9.25/10/19 | 10,750 | 10036/e1g1 | 2.864 |
| Kiwipete | ROOT_MEAN | 4 | 223,954 | 215,265 | 96.12 | +2.85 | 182,316 | 80,278 | 44.03 | -0.68 | 9.70/10/19 | 14,681 | 10654/e1g1 | 2.655 |
| Kiwipete | ROOT_MEDIAN | 4 | 271,980 | 263,291 | 96.81 | +25.80 | 221,710 | 94,082 | 42.43 | -2.28 | 9.86/11/19 | 17,384 | 10290/e1g1 | 3.539 |
| Kiwipete | AFFINE_SAFE_MEAN | 4 | 169,311 | 160,327 | 94.69 | -23.40 | 137,611 | 64,579 | 46.93 | +2.21 | 9.41/10/19 | 10,268 | 9660/e1g1 | 2.580 |
| Kiwipete | AFFINE_SAFE | 4 | 284,040 | 273,667 | 96.35 | +30.76 | 223,746 | 104,039 | 46.50 | +1.78 | 10.37/11/19 | 22,231 | 9960/c3b5 | 3.274 |
| Kiwipete | GAIN_ONLY | 4 | 218,790 | 209,297 | 95.66 | +0.00 | 180,919 | 80,896 | 44.71 | +0.00 | 9.25/10/19 | 10,750 | 9577/e1g1 | 2.743 |
| Exchanges | RAW | 4 | 2,019 | 749 | 37.10 | +0.00 | 1,347 | 925 | 68.67 | +0.00 | 0.69/0/6 | 0 | 11541/e4d5 | 0.014 |
| Exchanges | ROOT_MEAN | 4 | 2,141 | 831 | 38.81 | +10.95 | 1,430 | 944 | 66.01 | -2.66 | 0.75/1/6 | 0 | 12138/e4d5 | 0.018 |
| Exchanges | ROOT_MEDIAN | 4 | 2,238 | 857 | 38.29 | +14.42 | 1,487 | 949 | 63.82 | -4.85 | 0.74/0/6 | 0 | 12215/e4d5 | 0.014 |
| Exchanges | CAPTURE_MEAN | 4 | 1,937 | 693 | 35.78 | -7.48 | 1,272 | 902 | 70.91 | +2.24 | 0.61/0/5 | 0 | 11562/e4d5 | 0.016 |
| Exchanges | CAPTURE_MEDIAN | 4 | 1,931 | 676 | 35.01 | -9.75 | 1,270 | 914 | 71.97 | +3.30 | 0.58/0/5 | 0 | 11237/e4d5 | 0.021 |
| Exchanges | AFFINE_SAFE_MEAN | 4 | 1,937 | 693 | 35.78 | -7.48 | 1,272 | 902 | 70.91 | +2.24 | 0.61/0/5 | 0 | 11194/e4d5 | 0.018 |
| Exchanges | AFFINE_SAFE | 4 | 1,931 | 676 | 35.01 | -9.75 | 1,270 | 914 | 71.97 | +3.30 | 0.58/0/5 | 0 | 10723/e4d5 | 0.013 |
| Exchanges | GAIN_ONLY | 4 | 2,019 | 749 | 37.10 | +0.00 | 1,347 | 925 | 68.67 | +0.00 | 0.69/0/6 | 0 | 11013/e4d5 | 0.012 |
| Tactical queen | RAW | 4 | 189 | 8 | 4.23 | +0.00 | 115 | 72 | 62.61 | +0.00 | 0.07/0/1 | 0 | 6397/e4d5 | 0.002 |
| Tactical queen | ROOT_MEAN | 4 | 189 | 8 | 4.23 | +0.00 | 115 | 72 | 62.61 | +0.00 | 0.07/0/1 | 0 | 5626/e4d5 | 0.002 |
| Tactical queen | ROOT_MEDIAN | 4 | 189 | 8 | 4.23 | +0.00 | 115 | 72 | 62.61 | +0.00 | 0.07/0/1 | 0 | 5262/e4d5 | 0.003 |
| Tactical queen | CAPTURE_MEAN | 4 | 189 | 8 | 4.23 | +0.00 | 115 | 72 | 62.61 | +0.00 | 0.07/0/1 | 0 | 7465/e4d5 | 0.003 |
| Tactical queen | CAPTURE_MEDIAN | 4 | 189 | 8 | 4.23 | +0.00 | 115 | 72 | 62.61 | +0.00 | 0.07/0/1 | 0 | 7955/e4d5 | 0.003 |
| Tactical queen | AFFINE_SAFE_MEAN | 4 | 189 | 8 | 4.23 | +0.00 | 115 | 72 | 62.61 | +0.00 | 0.07/0/1 | 0 | 7228/e4d5 | 0.005 |
| Tactical queen | AFFINE_SAFE | 4 | 189 | 8 | 4.23 | +0.00 | 115 | 72 | 62.61 | +0.00 | 0.07/0/1 | 0 | 7591/e4d5 | 0.002 |
| Tactical queen | GAIN_ONLY | 4 | 189 | 8 | 4.23 | +0.00 | 115 | 72 | 62.61 | +0.00 | 0.07/0/1 | 0 | 6104/e4d5 | 0.003 |
| En passant | RAW | 4 | 358 | 18 | 5.03 | +0.00 | 235 | 151 | 64.26 | +0.00 | 0.07/0/1 | 0 | 0/e5d6 | 0.006 |
| En passant | ROOT_MEAN | 4 | 385 | 12 | 3.12 | -33.33 | 253 | 161 | 63.64 | -0.62 | 0.04/0/1 | 0 | 0/e5d6 | 0.006 |
| En passant | ROOT_MEDIAN | 4 | 387 | 12 | 3.10 | -33.33 | 254 | 162 | 63.78 | -0.48 | 0.04/0/1 | 0 | 0/e5d6 | 0.007 |
| En passant | CAPTURE_MEAN | 4 | 547 | 17 | 3.11 | -5.56 | 378 | 293 | 77.51 | +13.26 | 0.04/0/1 | 0 | 899/e1e2 | 0.006 |
| En passant | CAPTURE_MEDIAN | 4 | 663 | 32 | 4.83 | +77.78 | 470 | 383 | 81.49 | +17.23 | 0.07/0/1 | 0 | 1389/e1e2 | 0.008 |
| En passant | AFFINE_SAFE_MEAN | 4 | 547 | 17 | 3.11 | -5.56 | 378 | 293 | 77.51 | +13.26 | 0.04/0/1 | 0 | 870/e1e2 | 0.007 |
| En passant | AFFINE_SAFE | 4 | 663 | 32 | 4.83 | +77.78 | 470 | 383 | 81.49 | +17.23 | 0.07/0/1 | 0 | 1325/e1e2 | 0.007 |
| En passant | GAIN_ONLY | 4 | 358 | 18 | 5.03 | +0.00 | 235 | 151 | 64.26 | +0.00 | 0.07/0/1 | 0 | 0/e5d6 | 0.006 |
| Fianchetto | RAW | 4 | 18,327 | 13,007 | 70.97 | +0.00 | 16,509 | 8,896 | 53.89 | +0.00 | 3.46/2/17 | 24 | -18683/f3e5 | 0.277 |
| Fianchetto | ROOT_MEAN | 4 | 23,141 | 17,626 | 76.17 | +35.51 | 21,113 | 10,264 | 48.61 | -5.27 | 4.08/3/17 | 17 | -19454/f3e5 | 0.316 |
| Fianchetto | ROOT_MEDIAN | 4 | 27,693 | 22,082 | 79.74 | +69.77 | 25,422 | 11,772 | 46.31 | -7.58 | 4.58/4/17 | 33 | -19818/f3e5 | 0.372 |
| Fianchetto | CAPTURE_MEAN | 4 | 14,107 | 7,346 | 52.07 | -43.52 | 12,462 | 8,542 | 68.54 | +14.66 | 2.00/1/15 | 0 | -17704/d1d2 | 0.232 |
| Fianchetto | AFFINE_SAFE_MEAN | 4 | 14,107 | 7,346 | 52.07 | -43.52 | 12,462 | 8,542 | 68.54 | +14.66 | 2.00/1/15 | 0 | -17141/d1d2 | 0.300 |
| Fianchetto | AFFINE_SAFE | 4 | 11,752 | 6,150 | 52.33 | -52.72 | 10,250 | 7,090 | 69.17 | +15.28 | 1.78/1/14 | 0 | -17022/f3e5 | 0.188 |
| Fianchetto | GAIN_ONLY | 4 | 18,327 | 13,007 | 70.97 | +0.00 | 16,509 | 8,896 | 53.89 | +0.00 | 3.46/2/17 | 24 | -17829/f3e5 | 0.245 |
| Opening | RAW | 4 | 3,795 | 220 | 5.80 | +0.00 | 2,913 | 2,158 | 74.08 | +0.00 | 0.10/0/4 | 0 | -872/b1c3 | 0.064 |
| Opening | ROOT_MEAN | 4 | 3,802 | 231 | 6.08 | +5.00 | 2,921 | 2,143 | 73.37 | -0.72 | 0.11/0/4 | 0 | -1643/b1c3 | 0.051 |
| Opening | ROOT_MEDIAN | 4 | 3,898 | 241 | 6.18 | +9.55 | 3,017 | 2,219 | 73.55 | -0.53 | 0.12/0/4 | 0 | -2007/b1c3 | 0.053 |
| Opening | CAPTURE_MEAN | 4 | 3,800 | 196 | 5.16 | -10.91 | 2,921 | 2,196 | 75.18 | +1.10 | 0.09/0/4 | 0 | 196/b1c3 | 0.057 |
| Opening | CAPTURE_MEDIAN | 4 | 3,798 | 193 | 5.08 | -12.27 | 2,919 | 2,199 | 75.33 | +1.25 | 0.09/0/4 | 0 | 686/b1c3 | 0.065 |
| Opening | AFFINE_SAFE_MEAN | 4 | 3,800 | 196 | 5.16 | -10.91 | 2,921 | 2,196 | 75.18 | +1.10 | 0.09/0/4 | 0 | 190/b1c3 | 0.067 |
| Opening | AFFINE_SAFE | 4 | 3,798 | 193 | 5.08 | -12.27 | 2,919 | 2,199 | 75.33 | +1.25 | 0.09/0/4 | 0 | 655/b1c3 | 0.048 |
| Opening | GAIN_ONLY | 4 | 3,795 | 220 | 5.80 | +0.00 | 2,913 | 2,158 | 74.08 | +0.00 | 0.10/0/4 | 0 | -832/b1c3 | 0.057 |

### NNUE

| Fixture | Variant | Depth | Total | Q | Q% | ΔQ% | SP attempts | SP cuts | SP% | ΔSP | Qply | Soft limit | Score/move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kiwipete | RAW | 4 | 41,435 | 32,300 | 77.95 | +0.00 | 36,191 | 29,281 | 80.91 | +0.00 | 3.43/1/17 | 88 | 20697/e1g1 | 0.338 |
| Exchanges | RAW | 4 | 3,197 | 1,419 | 44.39 | +0.00 | 2,362 | 1,441 | 61.01 | +0.00 | 0.90/1/5 | 0 | 21844/e4d5 | 0.013 |
| Tactical queen | RAW | 4 | 229 | 10 | 4.37 | +0.00 | 154 | 109 | 70.78 | +0.00 | 0.06/0/1 | 0 | 7057/e4d5 | 0.002 |
| En passant | RAW | 4 | 638 | 40 | 6.27 | +0.00 | 427 | 264 | 61.83 | +0.00 | 0.11/0/2 | 0 | 0/e5d6 | 0.005 |
| Fianchetto | RAW | 4 | 8,793 | 3,928 | 44.67 | +0.00 | 7,437 | 5,744 | 77.24 | +0.00 | 0.84/1/9 | 0 | -22502/g2h3 | 0.056 |
| Opening | RAW | 4 | 2,343 | 126 | 5.38 | +0.00 | 1,684 | 1,136 | 67.46 | +0.00 | 0.09/0/2 | 0 | -887/h2h3 | 0.011 |
