# BRN-2 NNUE centipawn calibration study

**Outcome 2: no defensible production CP mapping was established for this teacher using material-removal pairs.** The pooled coefficient converges, but that hides inconsistent responses to the five BASIC_V1 material anchors and to game phase. A simple odd cubic provides only a 1.1% reduction in quiet held-out RMSE and leaves the material inconsistencies. Restricting both endpoints to |raw| <= 1 does not rescue calibration. Do not integrate the fitted coefficients into BRN training.

This is research completed on 2 October 2026, Pacific/Auckland, in authoritative `C:\projects\seed\java\seedv6`, starting HEAD `0e02fe3b0e957e9c0b9b3a81e1ac32e571aee97d`. GPT/user reconciliation remains separate from research completion. Production NNUE, BRN supervision/training, BASIC_V1 inference, optimizers, GUI, checkpoints, and search scoring were unchanged.

## Teacher identity and actual selection

The Java Preferences NNUE checkpoint selection resolves to `E:\SeedV6-Networks\NNUE\training`. The BRN-2 settings inspected use HANDCRAFTED/WDL, with no selected NNUE teacher. Thus there is **no active BRN teacher** to silently assume. This study tests the configured NNUE store's accepted Best, the appropriate first candidate for later explicit teacher selection. `TrainerService` selects the configured teacher store's accepted Best at a new blended generation and reloads the pinned checkpoint for unfinished work; `TrainingSource.loadBest` delegates to the integrity-checked snapshot API. Teacher and generator can now be selected independently; older research prose describing a single selector is historical.

| Identity | Value |
|---|---|
| Store | `E:\SeedV6-Networks\NNUE\training` |
| Lineage | `172b1e2a-62f7-4ce1-8486-146456426e6e`, name `training` |
| Lineage creation | `2026-10-02T03:28:04.883779600Z` |
| Checkpoint | `g000121-s000011724-604847b03dc1d1866a5f95a3fa9d1aba51fd86d012cfae2db5ef041518993dc1` |
| Generation / optimizer step / training depth | 121 / 11,724 / 4 |
| Parent | `g000120-s000011673-8fac499531b6dbfc3cb9bd05a936ffeb4fcdbfeca65409ff2f8cd798d28809f4` |
| Network file | `checkpoints/<checkpoint>/network.nnue`, 12,599,889 bytes |
| Network SHA-256 | `712b905928b5235bc5618621a39de2abcc4b3ef70eef41ee6166b2ea4bc28cab` |
| Training payload SHA-256 | `23716ffee49b167e54f6ea753f75988cbf4cd3eaacf9efbfdb0c6051fa970dd1` |
| Architecture | NNUE, `seedv6.nnue.halfkp.v1`, feature schema 1, network codec 1 |
| Dimensions | 49,152 HalfKP features; two 64-unit accumulators; concatenated 128; hidden 32; output 1; float parameters; clipped [0,1] transformer/hidden activations |

The first run used `CheckpointStore.readBestSnapshot`, validating acceptance evidence, manifest identity, payload sizes, hashes and codecs. Subsequent runs used the exact checkpoint ID through `readSnapshot`. An independently computed file SHA-256 matched the manifest. Best was read again after the final experiments and still identified the same checkpoint/hash. Latest training is not interchangeable with accepted Best. No store writer, training, promotion, or pruning was invoked.

## Source inspection and numerical boundary

The accepted [BASIC_V1 regression diagnostic](BRN2_BASIC_V1_TRAINING_REGRESSION_DIAGNOSTIC_2026-10-02.md) and current `NnueEvaluator`, `NnueNetwork`, `NnueScoreMapping`, `NnueTrainer`, `SelfPlayTraining`, `BrnSupervision`, `TrainerService`, `TrainingSource`, checkpoint loading, phase measure and existing diagnostic utilities were inspected.

`NnueEvaluator.evaluate(board)` returns a float **pre-tanh raw scalar in side-to-move perspective**. Its `raw()` returns the same float; `boundedValue()` uses `StrictMath.tanh(raw)`. We measured raw directly, without integer search quantization. `BrnSupervision.teacherValue` currently reads the bounded value without CP calibration. `NnueScoreMapping.V1` is explicitly uncalibrated: the approximately 32,511 multiplier protects the normal/mate boundary. `NnueTrainer` optimizes half-squared error against bounded STM eventual-outcome targets; normal sampled labels are terminal WDL, not CP. The existing NNUE research `calibrate` utility measures WDL prediction/loss, and `ScoreMappingStudy` examines integer quantization; neither establishes a material-anchored CP unit.

Canonical material was verified independently against `Brn2MaterialPrior.BASIC_V1.score`: US minus THEM, with pawn/knight/bishop/rook/queen = 100/320/330/500/900 CP and king = 0. Material supplies **paired marginal labels**, not full-position NNUE targets. The position's learned positional/tactical value remains in raw. No fit against total material balance was used.

## Dataset and perturbations

Sources were already local; no new self-play or search corpus was generated:

1. The accepted diagnostic's `evidence/brn2-basic-v1-2026-10-02/samples.bin`: 4,096 positions from 128 deterministic serial handcrafted games, 32 evenly spaced samples each, generator seed `7921502845091313457`, depth 4, opening 0-8 plies. The retained `generated-games.tsv`, sampler, and batch concatenation confirm game boundaries. This is the diagnostic's reproduction corpus, **not the original failed run's missing boards**. SHA-256: `09aa882d43d2b2f4b198dbe6937e694e80a901e2ecc6515a3f948d3e0c4ebd50`.
2. All 3,182 entries of `EvaluationCorpus`, including its support entries, from the previously retained depth-6 main-search/qsearch evaluation corpus. Decoded corpus SHA-256: `dad39174f075e77941e6e4e5d579e0c46a294b6652e22d15d614b9bd0a07f697`; baseline `9981efef51dd5d7be3ea82d97a49add2653d4caf`. Its existing sampling weights were not reinterpreted as a representative chess-population distribution. Kiwipete supplies most broad corpus pairs; separate source and quiet analyses prevent that composition from hiding the defect.

Every occupied non-king square was considered for single-piece removal. No king was removed and no piece was added or replaced. Side to move, castling flags, halfmove/fullmove counters and all other status fields were preserved exactly. FEN reconstruction rebuilds the Zobrist key. Rook removals that invalidate retained castling rights are rejected; flags are never silently repaired. Sources with EP are excluded to avoid invalidating EP history. Undefined encodings, pawns on ranks 1/8, incorrect king counts, either king in check, no legal STM move, fifty-move states, and basic dead-material states are screened. Removing a defender can create check, which rejects the pair. This is a static legal-board screen, not proof of a complete historical reachability sequence.

Duplicate bases are removed globally by piece placement plus STM (the NNUE inputs), retaining first provenance. Clocks/rights do not create separate NNUE examples. Calibration train/test splits keep all perturbations of a base within the source game/root; duplicate bases cannot cross the split. NNUE's own training-data independence is unknown: this is a held-out **calibration** study, not a teacher-generalization benchmark.

The broad retained cohort is reported transparently. The primary quiet cohort requires no profitable legal SEE capture worth >= 100 using the repository's exchange values, no promotion, and no legal mate in one, for **either side**, at **either endpoint**. Mate-in-one screening enumerates legal moves and, only for checking children, legal replies; it produces a boolean, never a search or mate score. These are cheap bounded legality/exchange probes. Deeper tactics, forced draws and repetition history remain unverified. Removing pieces also changes mobility, shielding and relationships; those are unavoidable perturbation noise, not assumed zero.

| Accounting | Count |
|---|---:|
| Input bases | 7,278 |
| Base rejection: either side in check | 506 |
| Base rejection: EP | 281 |
| Base rejection: no legal move | 1 |
| Duplicate valid bases | 684 |
| Valid unique bases | 5,806 (2,770 handcrafted; 3,036 search corpus) |
| Removal attempts | 116,860 |
| Pair rejection: invalidated castling | 10,380 |
| Pair rejection: check | 1,047 |
| Pair rejection: basic dead material | 226 |
| Pair rejection: no legal move | 3 |
| Valid broad pairs | 105,204, from 5,678 bases that yield a pair |
| Tactical/nonquiet pairs | 82,960 |
| Pairs flagged mate in one at either endpoint | 2,675, overlapping nonquiet classification |
| Quiet pairs | 22,244, from 1,443 bases |
| Quiet pairs, both endpoint abs(raw) <= 1 | 15,843 |

Other rejection categories and exact tanh saturation had zero observations. There are 107 retained handcrafted game clusters and seven search-root clusters in the broad paired dataset. Some games repeat earlier trajectories; deduplication prevents counting them as independent new evidence. Quiet pairs include 18,876 handcrafted and 3,368 corpus pairs. The quiet subset remains substantial and covers all five pieces and three phases; broad results are retained to expose selection effects.

## Fits, splitting and uncertainty

For each pair, `x=r1-r0`, `y=material1-material0`. The linear candidate minimizes `sum((K*x-y)^2)` through the origin: `K=sum(x*y)/sum(x*x)`. This is a noisy paired-response slope, not a proof that every marginal value is material alone. OLS can be attenuated when raw changes include positional noise; signed per-pair ratios, robust fits, population mean/median responses and equal-piece weighting are therefore also reported.

The deterministic cluster split hashes `nnue-cp-v1:0:<source>:<cluster>` with SHA-256, takes the first eight bytes big endian modulo 5, and holds out bucket 0. Games use `game-i`; corpus roots use `root-i`. Unequal clusters mean pair counts need not be 80/20: broad split = 39,379 train / 65,825 held out, principally because the dominant Kiwipete root is held out. Quiet split = **18,484 train / 3,760 held out**. Useful-range quiet split = 13,288 / 2,555. Do not tune the split to improve the reported result.

Uncertainty uses 1,000 whole-cluster bootstrap resamples, stratified by source, with `random.Random(20261002)`. These intervals estimate sampling variability conditional on these corpora, not systematic perturbation bias. Quantiles use linear interpolation. All per-pair ratio summaries include adverse signs; |delta_raw| <= 1e-8 is explicitly excluded only from ratio summaries. Correct-sign-only medians are separately named in JSON. Near-zero/zero counts are retained.

| Cohort | Linear training K (CP/raw) | 95% cluster bootstrap interval | Held-out MAE / RMSE (CP) |
|---|---:|---:|---:|
| Broad | 725.693863 | 668.18-793.24 | 156.21 / 212.62 |
| Quiet, primary | **749.811891** | **671.34-826.99** | **174.48 / 230.56** |
| Quiet, both abs(raw) <= 1 | 721.607862 | 625.82-812.53 | 166.31 / 221.02 |

Primary training MAE/RMSE = 165.84/223.31 CP. Held-out signed residual p05/p95 = -387.67/+386.74 CP. Primary Huber K = 754.242683; L1 through-origin K = 740.212724. Equal total weight per piece changes K to **1023.090416**, showing dependence on the weighting objective; it does not repair the universal anchor relationship. Exact robust held-out metrics are retained in JSON.

## Piece-type evidence

These are **all quiet-pair** subgroup estimates, rather than leaking held-out coefficients into the candidate. The subgroup bootstrap intervals use all quiet pairs to describe the research population. Signed implied K is `y/x`; its tails are intrinsically unstable when x is small. The mean-response K is locked piece value divided by the mean direction-oriented raw response; it exposes differences hidden by OLS noise attenuation.

| Piece | N | OLS K | 95% cluster interval | Median signed implied K | 10%-tail trimmed mean K | Mean-response K | Wrong/zero direction |
|---|---:|---:|---:|---:|---:|---:|---:|
| Pawn | 13,693 | 205.67 | 178.15-236.90 | 554.67 | 549.43 | 1241.95 | 28.25% |
| Knight | 2,732 | 919.16 | 812.18-1016.23 | 2058.35 | 2277.46 | 3043.06 | 21.12% |
| Bishop | 2,798 | 740.47 | 686.70-782.34 | 1085.53 | 1354.44 | 1239.16 | 8.15% |
| Rook | 2,001 | 981.77 | 928.03-1052.32 | 1774.33 | 2427.92 | 2282.71 | 15.19% |
| Queen | 1,020 | 1168.20 | 1073.46-1248.63 | 1372.86 | 1499.67 | 1413.00 | 1.08% |

Median-response scales are pawn 1594.25, knight **3911.59**, bishop **1277.37**, rook 2640.60, queen 1381.53 CP/raw. Knights and bishops have similar locked values, yet require radically different population response scales. This remains material even if one discounts OLS attenuation. Changing the common K cannot match both. A strictly increasing scalar transformation also cannot reverse a wrong-sign paired response.

Held-out quiet material changes below are oriented toward a **positive material gain** for comparison; actual STM removal deltas retain their negative/positive sign in the evidence. The same absolute interpretation applies to reversing endpoints as a gain/loss; reinsertion of a removed piece was not claimed as an independent experiment.

| Piece | Intended absolute delta CP | Held-out N | Linear median CP | IQR CP | Mean +/- SD CP | Cubic median CP |
|---|---:|---:|---:|---:|---:|---:|
| Pawn | 100 | 2,363 | 54.72 | -2.86 to 145.37 | 69.82 +/- 144.25 | 59.12 |
| Knight | 320 | 416 | 54.31 | 5.30 to 131.82 | 78.19 +/- 121.99 | 58.34 |
| Bishop | 330 | 459 | 154.11 | 43.45 to 270.49 | 162.52 +/- 172.66 | 160.13 |
| Rook | 500 | 365 | 130.54 | 46.88 to 246.88 | 156.61 +/- 165.98 | 141.56 |
| Queen | 900 | 157 | 466.61 | 333.71 to 570.24 | 475.24 +/- 253.13 | 467.49 |

The retained 15 representative examples identify FEN, removed square/side, raw endpoints, label and calibrated signed delta at each piece's approximately 10th/50th/90th held-out response percentiles. They illustrate distribution, not selected success cases.

## Phase, magnitude and perspective

Phase copies the repository's existing material measure: `clamp(24 - (4*queens + 2*rooks + bishops + knights), 0, 24)`. Broad bins: opening 0-7, middlegame 8-16, endgame 17-24; the last boundary matches `EvaluationCorpus.endgame`. These are material-phase bins, not chronological move-number labels.

| Quiet phase | N | OLS K | 95% cluster interval | Wrong/zero direction |
|---|---:|---:|---:|---:|
| Opening | 12,106 | 922.33 | 828.88-1006.21 | 18.97% |
| Middlegame | 6,974 | 564.84 | 522.21-609.91 | 25.64% |
| Endgame | 3,164 | 430.00 | 385.41-477.43 | 28.57% |

Piece composition contributes to phase slopes, but does not fully explain them. Quiet per-piece OLS K, opening/middlegame/endgame respectively: pawn **313/159/161**, knight **1061/770/695**, bishop **772/677/639**, rook **1143/953/904**, queen **1184/1072/1040**. Endgame queen N=48 is sparse; the other endgame strata have 208 bishops, 231 knights, 436 rooks and 2,241 pawns. A phase-specific scalar alone would still leave piece disagreement within each phase.

| Base abs(raw), quiet pairs | N | OLS K | Wrong/zero direction |
|---|---:|---:|---:|
| < .25 | 6,568 | 789.99 | 19.31% |
| .25 to < .75 | 7,781 | 703.68 | 21.76% |
| .75 to < 1.5 | 6,408 | 708.21 | 24.33% |
| >= 1.5 | 1,487 | 1558.39 | 31.47% |

Upper-magnitude response is compressed and unstable. Observed raw endpoints span **-1.598099470 to +1.640444875**. None of 227,826 evaluations had bounded output exactly +/-1; largest |tanh(raw)| is approximately .928. Recovery is numerically safe here. The output head and hidden clip01 imply arithmetic raw bounds approximately -1.633807239 to +1.650097270, apart from float accumulation rounding. Observed extremes approach those bounds; thus poor response at |raw| >= 1.5 is compatible with internal activation clipping, not lost atanh information. Broad pairs include 1,641 exactly unchanged raw responses. Restricting both endpoints to [-1,1] still gives pawn K approximately 208 versus queen approximately 1179 and large minor-piece inconsistencies; this is not solely an extreme-evaluation issue.

Quiet positive material deltas: N=11,130, K=754.80; negative: N=11,114, K=724.94. White STM: N=10,566, K=759.86; Black STM: N=11,678, K=722.97. Removing White versus Black pieces gives K=735.73 versus 744.38. All pairs are measured directly in original STM canonical perspective; no extra sign conversion is needed. Material changes reverse exactly when STM alone is flipped. Empirical reversed-STM raw pair slope = 743.26, close to original all-quiet pooled slope, and reversed held-out MAE/RMSE = 173.69/229.73 CP. Wrong/zero reversal fraction = 22.36%.

The NNUE is **not mathematically antisymmetric under changing only STM**: quiet `r(board,STM)+r(board,opposite STM)` has mean -.09399, median -.12509, SD .17000. This is a learned STM function, not evidence of a sign-conversion error. Pair-delta sum has mean .00393, SD .03930, p05/p95 -.05427/+.06617. Chess-equivalent rank reflection plus colour and STM reversal preserves canonical material exactly and raw within **4.9174e-7**, consistent with float summation order. That is the decisive canonical-coordinate check; imposing exact negation on the learned STM function would fabricate symmetry.

Other quiet subgroup scales: handcrafted 700.79 versus search corpus 934.38; base material |balance| <=100: 830.09, 101-500: 589.66, >500: 635.48; negative base raw: 776.60 versus positive: 696.09. JSON retains full subgroup residuals, calibrated distributions, and useful-range/broad equivalents. These systematic composition effects should not be averaged away.

## Nonlinear candidate and stability

The simplest nonlinear trial fits paired differences of the odd cubic, **not** a regression of y against `(r1-r0)^3`:

`delta_CP = a1*(r1-r0) + a3*(r1^3-r0^3)`.

The fit is constrained to nonnegative derivative over the observed training endpoint domain. The code compares unconstrained OLS when feasible with the two cone boundaries and the linear boundary. It introduces no intercept and preserves zero/sign/oddness. Useful-range coefficients allow a negative cubic term only when the derivative remains nonnegative on that explicitly bounded domain.

| Cohort | a1 | a3 | Held-out cubic MAE / RMSE CP | RMSE improvement |
|---|---:|---:|---:|---:|
| Broad | 624.537298 | 86.545836 | 145.21 / 196.86 | 7.4% |
| Quiet | **669.648365** | **76.219573** | **172.64 / 228.11** | **1.1% (2.46 CP)** |
| Quiet, endpoints abs(raw) <=1 | 736.571197 | -28.559728 | 166.25 / 220.92 | 0.04% (0.095 CP) |

The primary cubic derivative `669.648365 + 228.658719*r^2` is strictly positive, with minimum 669.648365, throughout the sampled domain and beyond. That mathematical property does **not** justify extrapolation. The useful-range derivative minimum is 650.91 over |r| <= .999892473. No held-out endpoints exceed their candidate's training domain. The broad improvement is mostly irrelevant to establishing quiet material consistency because broad held-out positions are dominated by one tactical root. Primary held-out piece medians remain far below their anchors. Extra complexity is not operationally justified.

Four deterministic quiet whole-cluster split salts give K = 749.81, 758.11, 745.15, 758.42. Increasing the same ordered training-cluster sample from 25/50/75/100% gives K = 724.45/762.84/753.26/749.81 using 3,775/10,276/14,166/18,484 pairs. The global number converges adequately; the problem is **systematic mismatch**, not insufficient N. Source/phase/piece responses remain different. More cheap repetitions of this identical population would tighten precision around a structurally inadequate mapping.

## Recommendation and limits

There is **no recommended production mapping, validity range, or evidence-based clipping threshold** from this study. `CP=749.811891*raw` and the cubic are rejected research candidates, not teacher contracts. Their linear numeric span would be roughly -1198 to +1230 CP on observed endpoints, but those numbers have failed the scale-anchor checks and must not be represented as calibrated CP. Neither 32511*tanh(raw) nor 32511*raw is justified.

The result establishes that the tested linear/simple-cubic material-removal method does not support a coherent CP teacher for this exact network. It does not prove that no conceivable monotonic function could be fitted, that NNUE lacks positional knowledge, or that all its chess decisions are poor. Deletion changes positional relationships and training-domain support; that bias, incomplete deeper-tactical screening, corpus composition, sparse queen endgames, calibration/teacher data dependence, and unconstrained learned STM asymmetry are material limitations. Marginal calibration alone would not fully validate absolute positional CP or draw/mate policy even if it had passed.

The cheapest next research step is a **bounded matched replacement study** on these same quiet bases (rook-to-knight, rook-to-bishop, queen-to-rook, both colours, reversible pairs), stratified by phase and restricted to supported raw ranges. This can distinguish whole-piece deletion/domain effects from inconsistent relative piece sensitivity, without new self-play or external engine authority. Approximately a few thousand cheap pairs should suffice to test that distinction; stop if the same minor-piece/phase mismatch remains. It is a subsequent research proposal, not implemented work or a required human action now. If replacement also fails, assess a better material-sensitive teacher or a deliberately approved CP-target design; do not disguise the defect with a high-degree fit.

Any future successful calibration must bind network SHA-256, architecture/schema, calibration-method version, data hashes, coefficients, validity bounds and validation evidence. **A different NNUE payload requires recalibration**, even with the same architecture or lineage. A moving Best reference alone is insufficient. Only a later authorized production unit can define `cp_target=calibrate(nnue_raw)`, divide by 32511, and specify normal-score clipping and terminal/WDL treatment. Existing BASIC_V1 normalization bounds are arithmetic safety, not empirical calibration bounds. No external engine cross-check was run; external CP would not resolve or replace the internal material anchor.

## Reproduction, validation and preservation

Non-shipped harness: `app/src/verification/java/com/ohinteractive/seedv6/tools/nnue/NnueCpCalibration.java`. Analysis: `tools/analyze-nnue-cp-calibration.py` (standard library only). Targeted fixtures: `app/src/test/java/com/ohinteractive/seedv6/tools/nnue/NnueCpCalibrationTest.java`. Evidence: [nnue-cp-calibration-2026-10-02](evidence/nnue-cp-calibration-2026-10-02), containing full subgroup `results.json`, `measurement.txt`, `heldout-examples.tsv`, verification/preservation receipts and file hashes. The approximately 23 MB pair TSV is kept in external scratch, **not added to Git**; its hash and deterministic regeneration are retained. No network payload was copied into Git. Reproduction requires the pinned external checkpoint remain available; pruning/replacing it would prevent exact reevaluation.

From PowerShell, choose a fresh output directory (the harness/analysis refuse to reuse one):

```powershell
./gradlew.bat :app:compileVerificationJava :app:processVerificationResources
$calCp = 'app/build/classes/java/verification;app/build/classes/java/main;app/build/resources/verification'
$calTeacher = 'g000121-s000011724-604847b03dc1d1866a5f95a3fa9d1aba51fd86d012cfae2db5ef041518993dc1'
java -Xmx1g '-Djava.awt.headless=true' -cp $calCp com.ohinteractive.seedv6.tools.nnue.NnueCpCalibration E:/SeedV6-Networks/NNUE/training $calTeacher docs/research/brn/evidence/brn2-basic-v1-2026-10-02/samples.bin C:/Temp/seedv6-calibration-new-run
python tools/analyze-nnue-cp-calibration.py C:/Temp/seedv6-calibration-new-run/pairs.tsv C:/Temp/seedv6-calibration-new-analysis
./gradlew.bat :app:test -Pheadless --tests '*NnueCpCalibrationTest' --tests '*NnueEvaluatorTest' --tests '*NnueExactInferenceTest' --tests '*NnueScoreMappingTest'
```

Validation actually run:

- Production/verification compilation and resource processing succeeded; the normal JAR/distribution tasks remained up to date. Verification classes are excluded from application packaging by the existing source-set boundary.
- **17 targeted JUnit tests passed, zero failures/errors/skips**: evaluator 7, exact inference 3, score mapping 4, calibration fixtures 3. Fixtures verify all five literal anchors, flip/reflection/phase, state-damage/check/dead-material rejection, capture quality, and mate-in-one detection for either side. After adding the bounded mate screen, the same selection was rerun successfully.
- Two final measurement runs produced byte-identical pair TSVs and measurement summaries; two final analysis runs produced byte-identical JSON and representative examples. Each run made 227,826 raw evaluations in about 6 seconds, with no training or full search.
- In every measured evaluation, raw return equalled `raw()`, bounded output equalled `StrictMath.tanh(raw)`, and board material matched an independent literal computation. Maximum bounded-to-raw round-trip error was **4.4409e-16**. Every 97th evaluation was compared to the scalar oracle and repeated: maximum difference **0**. Every valid base's canonical reflection was tested; material sign flip and every removal delta were asserted.
- Analysis self-tests verify perfect synthetic linear and odd-cubic coefficient recovery, hand-computed MAE/RMSE and quantiles. A separate endpoint-form audit recomputed the documented hash split, linear coefficient, held-out MAE/RMSE, and cubic held-out RMSE independently and verified train/test cluster disjointness.
- Baseline hashes cover **all 38 inherited dirty/untracked files**, including retained earlier diagnostic evidence. All remain byte-for-byte unchanged; the version bytes remain build 32. The original teacher hash was reread and matched. `git diff --check` was run, and new text files were checked for trailing whitespace/conflict markers.

No slow/full suite, long training, self-play, tournament, Elo test, broad search benchmark, GUI/browser QA or external engine experiment was run. They are unnecessary for this bounded evaluator/calibration conclusion. Deep tactical quietness, human-game representativeness, absolute CP accuracy, and playing strength remain unmeasured.

No commit or push was made. Final Git state preserves the original **17 tracked modifications and six unrelated untracked files**, plus **15 untracked files from the retained accepted BASIC_V1 diagnostic**. This unit adds only its harness, test, analysis, report and six evidence files; it does not absorb earlier work. Root `CODEXLOG_CURRENT.md` is absent and was not created. Version finalization is `no_bump`, retaining build 32 for non-shipped research. The preservation/evidence receipts distinguish this unit from both inherited categories.

Human actions required after this prompt: None. Choosing the next research study or authorizing later production changes remains a subsequent GPT/user decision; production integration is unsupported by this completed research result.
