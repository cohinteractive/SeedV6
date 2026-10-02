# BRN-2 NNUE same-square replacement calibration: route closed

**Outcome 2 â€” internal calibration route closed. No defensible NNUE->CP mapping is supported for this teacher from BASIC_V1 material anchors. Do not use current SeedV6 NNUE raw output as a CP teacher for BASIC_V1.** Controlled replacements preserve occupancy but still produce incompatible class responses, reversed bishop/rook sensitivity in ordinary openings, phase and magnitude dependence, and held-out errors comparable to the material differences themselves. No nonlinear model was fitted; the eligibility conditions were not met.

Research completed on 2 October 2026, Pacific/Auckland, in authoritative `C:\projects\seed\java\seedv6`, starting HEAD `0e02fe3b0e957e9c0b9b3a81e1ac32e571aee97d`. This completes the bounded experiment, not GPT/user acceptance or production integration. Production NNUE semantics, BRN-2/BASIC_V1, training objectives, optimizers, GUI, search scoring and checkpoint formats were untouched. No external engine was introduced.

## Question and prior evidence

Can changing only a same-side non-king piece's type on its occupied square recover a universal raw-to-CP scale, reducing deletion's opened-line and occupancy confounds? Same-square replacement still changes mobility, attacks, coordination and tactical/king safety relationships; these are measured residual variation, not assumed absent.

The [removal study](BRN2_NNUE_CP_CALIBRATION_2026-10-02.md) and its [machine evidence](evidence/nnue-cp-calibration-2026-10-02/results.json) established no mapping. Its rejected quiet candidate was `CP=749.811891*raw`, with held-out MAE/RMSE 174.48/230.56 CP; piece slopes were pawn/knight/bishop/rook/queen 205.67/919.16/740.47/981.77/1168.20, and opening/middle/end slopes 922.33/564.84/430.00. An odd cubic improved RMSE only 1.1%. Those are comparison evidence, not authoritative conversions.

## Exact teacher binding

| Field | Verified value |
|---|---|
| Store | `E:\SeedV6-Networks\NNUE\training` |
| Accepted Best generation / optimizer step | 121 / 11,724 |
| Checkpoint | `g000121-s000011724-604847b03dc1d1866a5f95a3fa9d1aba51fd86d012cfae2db5ef041518993dc1` |
| Network SHA-256 | `712b905928b5235bc5618621a39de2abcc4b3ef70eef41ee6166b2ea4bc28cab` |
| Network file / size | `checkpoints/<checkpoint>/network.nnue` / 12,599,889 bytes |
| Training payload SHA-256 | `23716ffee49b167e54f6ea753f75988cbf4cd3eaacf9efbfdb0c6051fa970dd1` |
| Architecture / feature schema / network codec | `seedv6.nnue.halfkp.v1` / 1 / 1 |
| Dimensions | 49,152 features; 64 x 2 accumulators; hidden 32; output 1 |
| Manifest training depth / parent | 4 / `g000120-s000011673-8fac499531b6dbfc3cb9bd05a936ffeb4fcdbfeca65409ff2f8cd798d28809f4` |
| Lineage | `172b1e2a-62f7-4ce1-8486-146456426e6e`, `training`, created `2026-10-02T03:28:04.883779600Z` |

Both measurement runs used integrity-checked `CheckpointStore.readBestSnapshot`, asserted the complete requested identity and independently hashed the file **before positions were evaluated**, then repeated this check after measurement. Best and payload matched throughout. No writer, promotion, pruning or training was invoked. The mutable saved lineage configuration differs from the prior receipt; the immutable checkpoint manifest, payload and lineage identity match. Saved configuration is not an inference input and was neither applied nor altered. These findings bind the payload, not a moving Best reference or an active BRN teacher selection.

`NnueEvaluator.evaluate()` is verified pre-tanh STM raw, equal to `raw()`. Neither integer search mapping nor bounded output supplies the calibration variable. Labels use locked BASIC_V1 P/N/B/R/Q/K = 100/320/330/500/900/0 CP and its public one-unit-equals-one-CP semantics.

## Positions, generation and filtering

Reused unchanged sources:

- Diagnostic `evidence/brn2-basic-v1-2026-10-02/samples.bin`: 4,096 positions, 128 deterministic handcrafted games x 32 samples; seed `7921502845091313457`, depth 4. This is the diagnostic reproduction corpus, not the unavailable original failed training run's boards. SHA-256 `09aa882d43d2b2f4b198dbe6937e694e80a901e2ecc6515a3f948d3e0c4ebd50`.
- All 3,182 retained `EvaluationCorpus` entries, including support positions; decoded SHA-256 `dad39174f075e77941e6e4e5d579e0c46a294b6652e22d15d614b9bd0a07f697`, original baseline `9981efef51dd5d7be3ea82d97a49add2653d4caf`. No new search or self-play generated positions.

Every existing piece participating in an anchor class is replaced in each applicable direction. Same square, color, occupancy, all other squares, STM, clocks and unrelated status are asserted unchanged. FEN reconstruction rebuilds the key. Kings are never replaced. Replacing back must restore the exact six-long board, including its key; restored raw is also sampled. Invalid castling rights are rejected rather than repaired. Pawn destinations on ranks 1/8 are rejected. Pawn replacements are optional sensitivity evidence because they can resemble unusual promotions/demotions and may be outside training support.

Reuse of the prior harness preserves its static legality rules: valid encodings and king counts, neither king checked, no EP, valid castling pieces, STM has a legal move, halfmove clock below 100, conservative dead-material rejection. This is not proof of historical reachability or exhaustive draw adjudication. Deduplicate globally by placement plus STM; clocks/rights do not create separate NNUE examples. All examples from one source game/root stay in the same calibration split.

Primary quiet pairs have no profitable legal SEE capture worth >=100, no promotion and no legal mate in one for either side at either endpoint. SEE uses repository exchange values only for filtering; labels use BASIC_V1. Tactical/mating pairs remain in the valid broad cohort, separately labeled. Deeper tactics, repetition and forced draws remain unverified. No raw extremes are discarded in primary analysis; both endpoints |raw| <=1 is a separate sensitivity check.

| Accounting | Count |
|---|---:|
| Input bases | 7,278 |
| Base rejection: check / EP / no legal move | 506 / 281 / 1 |
| Duplicate valid bases | 684 |
| Unique valid bases: handcrafted / corpus | 2,770 / 3,036 |
| Replacement attempts | 427,957 |
| Pair rejection: castling / pawn rank | 31,140 / 24,388 |
| Pair rejection: check / dead material / no legal move | 9,104 / 102 / 20 |
| Valid pairs / bases yielding pairs | 363,203 / 5,791 |
| Tactical/nonquiet pairs | 301,743 |
| Base tactical / replacement tactical flags (overlap) | 280,794 / 296,681 |
| Mate-in-one flags (overlap with nonquiet) | 10,516 |
| Quiet pairs / bases | 61,460 / 1,545 |
| Quiet pairs: handcrafted / corpus | 51,379 / 10,081 |
| Primary quiet N-R, B-R, R-Q pairs / bases | 11,026 / 1,438 |

Other rejection reasons and exact tanh saturation had zero observations. Primary quiet pairs include 9,616 handcrafted and 1,410 corpus pairs, from 107 games and seven roots. The full quiet set includes ten roots. The corpus is clustered and not a representative human-game distribution. One bounded pass over existing data was sufficient; rerunning identical inputs verifies determinism without adding samples. No further sampling followed the falsification.

## Candidate, uncertainty and held-out evaluation

For every pair, `x=raw(replacement)-raw(base)` and `y=(new value-old value)*(+1 if replaced side is STM else -1)`. Fit `K=sum(x*y)/sum(x*x)`. Zero intercept is appropriate **for differences**: identical endpoints imply zero difference, and an absolute affine offset cancels. These data cannot identify an absolute CP intercept or prove positional CP meaning.

The primary fit includes only N-R, B-R, R-Q. N-B's 10 CP probe is excluded from the candidate, and optional pawn anchors have a separate pooled sensitivity fit. All subgroup coefficients describe all quiet observations; they are not used to tune the held-out candidate. The unchanged removal-study split is SHA-256 of `nnue-cp-v1:0:<source>:<cluster>`, first eight bytes big endian modulo five, bucket zero held out. Train/test games and roots are disjoint. Primary split is 9,238/1,788 pairs, 89/25 clusters. This holds out calibration clusters, not necessarily the NNUE's own training data.

Bootstrap intervals use 1,000 whole-cluster resamples within source with `random.Random(20261002)`. They measure conditional sampling uncertainty, not residual perturbation bias. Per-pair signed ratio summaries exclude only |x| <=1e-8; zeros and adverse directions remain in all fitting/errors. Linear interpolation defines quantiles.

| Cohort | Train K, CP/raw | 95% cluster CI | Held-out MAE / RMSE, CP |
|---|---:|---:|---:|
| Broad primary classes, N=55,815 | 339.223319 | 314.95-363.46 | 187.51 / 213.94 |
| Quiet primary, N=11,026 | **362.159017** | **327.72-393.85** | **200.92 / 236.47** |
| Quiet primary, both endpoints abs(raw) <=1, N=7,436 | 353.669607 | 315.79-388.26 | 197.33 / 234.84 |

The quiet candidate is **rejected**, not a CP contract or validity range. Training MAE/RMSE is 196.36/228.60 CP. Held-out anchor-relative MAE/RMSE is **83.35%/92.33%**. Huber/L1 K = 387.22/447.66; equal total class weighting K = 391.58 still has held-out RMSE 236.58. Adding optional pawn anchors, still excluding N-B, changes K to 670.72 and held-out MAE/RMSE to 284.24/344.93. Weighting choices cannot fix inconsistent chess meaning.

## Replacement-class results

Held-out predictions below use the single primary K and are oriented toward a positive material gain. IQR is the 25th-to-75th percentile, not a coefficient interval. Counts retain both directions/colors/STM.

| Class | Anchor CP | Valid / quiet | Quiet OLS K (95% CI) | Held-out N | Median delta / IQR, CP | MAE / RMSE, CP | Wrong/zero, quiet |
|---|---:|---:|---|---:|---|---|---:|
| N-B, probe | 10 | 25,733 / 4,982 | 21.13 (17.09-24.85) | 723 | 38.53 / -3.30 to 87.75 | 69.72 / 93.27 | 21.06% |
| N-R | 180 | 19,621 / 3,966 | **320.43 (301.75-338.89)** | 616 | **55.74 / 9.10 to 124.73** | 126.58 / 147.24 | 21.56% |
| B-R | 170 | 21,788 / 4,087 | **42.50 (-30.77-108.05)** | 653 | **6.23 /-29.65 to 52.74** | 160.21 / 175.54 | **50.11%** |
| R-Q | 400 | 14,406 / 2,973 | **580.08 (513.61-662.13)** | 519 | **47.01 /-10.30 to 131.98** | 340.39 / 357.98 | 23.11% |
| P-N | 220 | 73,888 / 14,180 | 48.21 (19.39-77.59) | 2,384 | 2.71 /-47.15 to 50.73 | 217.76 / 234.96 | 46.25% |
| P-B | 230 | 74,381 / 12,758 | 233.25 (185.22-283.83) | 2,140 | 11.62 /-29.74 to 69.43 | 213.49 / 232.12 | 37.54% |
| P-R | 400 | 65,824 / 9,515 | 710.19 (617.67-814.89) | 1,443 | 47.61 / 0.81 to 109.77 | 345.01 / 358.77 | 19.40% |
| P-Q | 800 | 67,562 / 8,999 | 1073.34 (1001.02-1136.94) | 1,311 | 138.00 / 48.13 to 227.41 | 656.22 / 669.72 | 9.59% |

N-B noise/error dwarfs its 10 CP anchor; it cannot identify a useful universal scale and is not decisive against calibration. The three larger core anchors already falsify it. Their coefficient intervals are incompatible. B-R median oriented raw is exactly zero; its mean-response scale is 12,697.82 versus 927.10 for N-R and 2010.83 for R-Q. Thus OLS attenuation alone does not explain away the mismatch. Signed ratio medians are 165.44/557.36/1079.01 for B-R/N-R/R-Q; 10%-tail trimmed means 67.10/616.88/1227.34 also disagree. Full distributions, zero counts and all broad/optional subgroups are in JSON. In the |raw| <=1 subset, B-R/N-R/R-Q K is approximately **-0.02/313.04/588.70**: extreme outputs are not the sole problem.

## Phase, raw magnitude and interactions

Use the unchanged material phase `clamp(24-(4Q+2R+B+N),0,24)` on the base: opening 0-7, middle 8-16, end 17-24. Replacement changes piece identity and hence endpoint material phase; this is a base-phase stratification, not chronological stage or a phase-conditioned calibrator.

| Primary quiet phase | N | K (95% CI) | Wrong/zero | N-R / B-R / R-Q K |
|---|---:|---|---:|---|
| Opening | 5,537 | 452.11 (410.47-488.66) | 31.55% | **366.02 /-206.66 / 776.74** |
| Middlegame | 4,177 | 260.51 (225.49-296.62) | 33.83% | 284.81 / 152.91 / 320.23 |
| Endgame | 1,312 | 329.66 (291.52-373.64) | 32.77% | 224.58 / 204.97 / 485.71 |

Within-phase class counts N-R/B-R/R-Q are 2048/2086/1403 opening,1454/1588/1135 middle,464/413/435 end. In ordinary opening positions, **60.55% of B-R pairs have wrong/zero direction**, with a negative fitted slope. Phase composition cannot explain away the contradiction: rook/queen and knight/rook imply different positive scales while bishop/rook points the wrong way in the same phase. No interaction model was fitted.

| Base abs(raw), primary quiet | N | K (95% CI) |
|---|---:|---|
| <.25, near equal | 2,911 | 363.66 (325.92-398.18) |
| .25 to <.75 | 3,811 | 353.86 (313.32-393.95) |
| .75 to <1.5 | 3,432 | 367.68 (330.51-405.86) |
| >=1.5, larger nonterminal advantage | 872 | **843.50 (614.27-1196.56)** |

High-magnitude response compression persists within classes: N-R K increases from 321.18 near equality to 714.10 at >=1.5; R-Q from 628.10 to 1594.11. B-R is already negative (-81.64) near equality. Observed primary endpoint raw spans -1.598099470 to 1.640444875; none of the 1,112,408 raw evaluations exactly saturates tanh. The known clip01 network bounds remain relevant, but neither atanh recovery nor a restricted range resolves class inconsistency.

## Direction and perspective

Exact restoration reverses the endpoint delta algebraically and numerically; restored boards are not counted as independent observations. Natural replacements in opposite directions originate from different positions and reveal distributional asymmetry:

| Primary quiet grouping | N | K |
|---|---:|---:|
| Piece-value increase / decrease | 6,577 / 4,449 | **272.72 / 461.90** |
| Positive /negative STM material delta | 5,500 / 5,526 | 372.06 / 353.91 |
| White /Black replaced piece | 5,502 / 5,524 | 369.90 / 355.75 |
| White /Black STM | 5,225 / 5,801 | 366.83 / 359.69 |

Increase/decrease slopes by class are N-R 347.98/282.04, B-R**-159.72/212.52**, R-Q 392.85/764.00. B->R wrong/zero frequency is 62.10%, versus32.53% for R->B. Color and STM pooling does not hide these directional defects.

Changing STM alone changes material sign exactly; the learned raw function is not mathematically antisymmetric. Reversed-STM all-primary slope 365.65 is close to original all-primary, with held-out MAE/RMSE 200.81/236.38 and wrong/zero 32.87%. Pair-delta STM sum has mean .00672, SD .04806; base raw STM sum has mean -.07416, SD .17096. Chess-equivalent rank reflection with color and STM reversal preserves material exactly and raw within **8.3447e-7**, validating canonical coordinates. No artificial antisymmetrization was applied.

## Stability and direct removal comparison

Four deterministic split salts give primary K 362.16/382.40/361.47/366.54; held-out RMSE 236.47/240.18/235.80/234.55. Ordered training cluster fractions 25/50/75/100% give K 377.68/357.97/364.37/362.16 on 1964/4938/7051/9238 pairs. This is ample convergence for falsification; more samples would refine an inadequate pooled coefficient. Handcrafted/corpus primary slopes 346.71/465.11 show additional source dependence.

| Question | Removal -> replacement evidence | Assessment |
|---|---|---|
| Variance reduced? | Held-out oriented raw SD: removal N/B/R/Q=.163/.230/.221/.338; replacement N-R/B-R/R-Q=.259/.227/.313 | No uniform substantial reduction; occupancy preservation does not suppress relative sensitivity noise |
| Piece consistency improved? | Removal five slopes206-1168; larger-piece slopes740-1168. Replacement core43/320/580; B-R CI includes zero, opening slope negative | No coherent shared K; optional pawn slopes48-1073 also contradict |
| Phase dependence reduced? | Pooled922/565/430 ->452/261/330; max/min2.14 ->1.74 | Some pooled reduction, but incompatible within-phase responses and reversed opening B-R remain |
| Magnitude dependence reduced? | Removal high-magnitude K1558 versus704-790 below1.5; replacement843 versus354-368 | No; roughly2.38x compression at upper magnitude remains and occurs within classes |
| K more stable? | Removal split-salt range745-758; replacement361-382 | Both pooled numbers converge; replacement is not more stable and convergence does not imply calibration |
| Held-out errors substantially smaller? | MAE/RMSE174.48/230.56 ->200.92/236.47 CP | No. Anchor-relative RMSE improves125.62% ->92.33%, still nearly an entire ordinary material distinction |
| Remaining errors suitable for BRN? | Replacement medians56/6/47 CP against180/170/400; B-R50.11% wrong/zero | No |

Different intervention labels, class weighting and quiet populations make these aggregate comparisons descriptive, not matched causal variance estimates. Smaller calibrated SD by itself would be misleading because the replacement K is approximately half the deletion K. Raw SD and anchor-relative errors provide a scale-aware comparison. The reduced pooled phase spread and relative error are real partial improvements; neither repairs incompatible major anchors or sign reversals. Deletion disruption therefore cannot reasonably serve as a sufficient explanation for the previous failure.

The simple odd cubic was **not run**: it was permitted only after good class and phase agreement with residual curvature. Those prerequisites failed. No high-degree fit, spline, phase/piece table, neural calibration or ensemble was attempted. A positive monotonic scalar cannot correct a wrong-sign paired response. There is no supported coefficient, usable CP range or evidence-based clipping rule from this work.

## Closure, limitations and reproduction

Close the cheap internal NNUE material-anchor calibration route for this exact teacher. Future BRN work must choose another supervision design, such as external CP-capable teacher supervision or a properly calibrated CP->WDL/outcome training model, in a separate authorized work unit. Neither is implemented or selected here. No further elaborate internal calibration experiment is recommended; no specific cheap methodological flaw was discovered that explains the core contradictions.

This does not prove NNUE lacks positional knowledge or playing strength. Replacement endpoints can be unusual, static quietness misses deeper tactics, corpus/root dependence limits generalization, and the NNUE's training-data overlap is unknown. Marginal material differences do not fully establish absolute positional CP, draw/mate policy or supervision quality even if calibration had passed. Findings do not transfer to a different payload.

New non-shipped harness, fixtures and analysis:

- `app/src/verification/java/com/ohinteractive/seedv6/tools/nnue/NnueCpReplacementCalibration.java`
- `app/src/test/java/com/ohinteractive/seedv6/tools/nnue/NnueCpReplacementCalibrationTest.java`
- `tools/analyze-nnue-cp-replacement-calibration.py`

The old harness/statistics are reused unchanged. [Evidence](evidence/nnue-cp-replacement-calibration-2026-10-02) includes results, measurement, 24 held-out percentile examples, verification, preservation and file hashes. The 82,629,864-byte pair TSV stays in external scratch; SHA-256 `e3ac69dd6b3c2ad5b1b99cd6c7a7334738a6a6c4c1d42fdf1f4dee38a04f3763`. No network is copied into Git. Reproduction requires the retained exact accepted Best and both unchanged corpora; the harness fails closed if identities move. Choose fresh directories:

```powershell
./gradlew.bat :app:compileVerificationJava :app:processVerificationResources
$replacementCp = 'app/build/classes/java/verification;app/build/classes/java/main;app/build/resources/verification'
java -Xmx1g '-Djava.awt.headless=true' -cp $replacementCp com.ohinteractive.seedv6.tools.nnue.NnueCpReplacementCalibration E:/SeedV6-Networks/NNUE/training docs/research/brn/evidence/brn2-basic-v1-2026-10-02/samples.bin C:/Temp/seedv6-replacement-new-run
python -B tools/analyze-nnue-cp-replacement-calibration.py C:/Temp/seedv6-replacement-new-run/pairs.tsv C:/Temp/seedv6-replacement-new-analysis
./gradlew.bat :app:test -Pheadless --tests '*NnueCpReplacementCalibrationTest' --tests '*NnueCpCalibrationTest' --tests '*NnueEvaluatorTest' --tests '*NnueExactInferenceTest'
```

Validation actually performed:

- Production was up to date; verification/test compilation and resource processing succeeded. **17 JUnit tests passed, zero failures/errors/skips**: evaluator 7, exact inference 3, reused calibration 3, replacement 4. Fixtures check eight anchors in both directions/colors/STM, exact restoration, occupancy/status and piece identity, king/empty/same-type rejection, castling/pawn-rank/EP/check/stalemate screens. Existing fixtures cover quiet capture and mate-in-one detection.
- Two measurement runs produced byte-identical pairs and measurement; two analyses produced byte-identical JSON/examples. Self-tests cover synthetic linear recovery, differences, independent hash splitting, error metrics and quantiles; inherited statistical self-tests also passed.
- Every evaluation asserted pre-tanh extraction; scalar oracle and repeat checks every 97th evaluation had zero discrepancy. Bounded/raw round-trip error <=4.4409e-16; canonical reflection <=8.3447e-7; sampled restored raw exact. Every valid pair asserted occupancy, identity, status, restoration, BASIC_V1 delta and canonical material transforms.
- Independent endpoint audit recomputed literal FEN piece/color/STM/material and directed anchors for all 363,203 rows, quiet classifications, uniqueness, filtering counts and pair hash. It independently reproduced all three cohort train/test counts, K and held-out MAE/RMSE to 1e-10, with zero cluster overlap. Whole-cluster resampling and output hashes reproduced exactly.
- Inherited-file preservation, text checks, Git whitespace and evidence hashes are recorded in the receipts. All 48 inherited dirty/untracked files remain byte-for-byte unchanged, including earlier diagnostic/removal evidence. HEAD/index were not changed.

No broad/full or slow suite, GUI/browser verification, long training, self-play campaign, tournament, Elo test, performance benchmark or external-engine check was run. They do not validate this bounded material-anchor question. Absolute CP accuracy, deeper tactics, historical reachability and playing strength remain unmeasured.

No commit or push was created. Git retains 17 inherited tracked modifications, six unrelated inherited untracked files, 15 prior BASIC_V1 diagnostic files and 10 prior removal-study files; this study adds only its three research code/test files, this report and six evidence files. Root `CODEXLOG_CURRENT.md` is absent and was not created. Research-only version decision is `no_bump`; build 32 bytes remained unchanged through validation. Required version finalization is reported separately at turn completion.

Human actions required after this prompt: None.
