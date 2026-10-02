# BRN-2 BASIC_V1 training regression diagnosis

The actual failed Gen-1 checkpoint reproduces the material giveaways headlessly. The primary mechanism is a retained **WDL objective on an uncalibrated full-range output**, combined with a material prior expressed in centipawns. Ordinary successful optimization learns residuals on the WDL scale, overwhelming the much smaller material signal. Online Adam and the sum-pooled architecture amplify early updates; tanh saturation then further attenuates the material prior. This is a failure of the combined training/score design, with early generalization errors made costly by that design, rather than a missing prior, incorrect derivative, serialization failure, or search sign reversal.

This is a diagnostic work unit. No production semantics, learning rate, initialization, score mapping, material values, checkpoints, or lineage references were changed. Correction proposals below require a separate reconciled production work unit.

## Actual artifact and reproduction

Authoritative repository: `C:\projects\seed\java\seedv6`, inspected HEAD `0e02fe3b0e957e9c0b9b3a81e1ac32e571aee97d`. Investigation date: 2 October 2026, Pacific/Auckland.

The GUI's Java Preferences selection identified the existing store:

`E:\SeedV6-Networks\BRN-2\8a5ad5dd-6646-4741-8ab7-8b188069b73e`

Its checksummed references and manifests identify:

| Artifact | Generation | Updates | Model SHA-256 |
|---|---:|---:|---|
| Best | 0 | 0 | `24e4c86c548a78830ac339ebf97e9d62dad298a6b84145cea3c2f419ce4f1c59` |
| Latest training candidate | 1 | 4096 | `e6a8d151a97014e2fa681ac8fd1dd4da17b8a04a438c93425d18b29f3c9fd5f7` |

Gen-1's checkpoint ID is `g000001-s000004096-98c067258cf3567e8f24ae675d358a5f79b0848cd7b3918d2cc2886e0441f390`; its parent is the referenced Gen 0. Its training-state SHA-256 is `7990809300e41d2783052411dd79ab873d35025ece008bd15b428e9d78710f39`. Binary CRC checks passed. Both model and training payloads have **format 2, feature schema 2, BASIC_V1**. Model weights exactly equal the respective decoded trainer snapshots.

The actual source is **HANDCRAFTED**, supervision **WDL, teacher weight 0**, capture consistency **OFF**. Recorded settings: 128 games, depth 4, requested 12 search workers, random opening 0-8 plies, 32 samples/game, maximum 1024 plies; one shuffled online training pass; Adam `lr=.001, beta1=.9, beta2=.999, epsilon=1e-8`. Recorded generator seed `7921502845091313457`, shuffle seed `-6540313355536843707`.

The retained partial-validation record contains 160 candidate losses, one candidate win, and one draw among individual completed chess-result games; 31 games hit the ply cap, and 63 entries are cancelled/unstarted. These individual counts include incomplete pairs and are **not a tournament acceptance statistic**. Training generation statistics record 56 White wins, 22 draws, 50 Black wins, 4096 samples, and 15624 played plies.

The training boards were released after candidate publication; the partial record retains no samples. Consequently the original optimizer trajectory cannot be replayed exactly from retained artifacts. A separate deterministic **single-worker** production handcrafted batch was generated with the recorded seed and other bounds: 128 games, 4096 samples, 16591 plies, 67 White wins / 20 draws / 41 Black wins, in 27.88 seconds. This is a controlled reproduction dataset, **not the original data**. Its manually instrumented update sequence and the unmodified production `Brn2SelfPlayTraining.trainSamples` produce identical weights. Neither matches the failed checkpoint byte-for-byte, as expected from different generated games. All ablations reuse exactly this frozen dataset and shuffle order.

Actual Gen 1 versus material-only Gen 0 from the standard start:

| Diagnostic fragment | Horizon | Outcome at bounded endpoint |
|---|---|---|
| Actual Gen 1, normal residual | depth 2, 64 plies | White material -3800 cp; only king and two pawns remain |
| Same actual weights, residual head multiplied by .01 | depth 2, 64 plies | White material +100 cp; many pieces retained |
| Actual Gen 1, normal residual | depth 4, 48 plies | White material -3280 cp; only king and four pawns remain |

These are bounded causal fragments, not Elo estimates. The original lineage was only read, never resumed, trained, repaired, promoted, or deleted.

## Verified numerical training pipeline

1. `TrainerService.generateTrainPublish` selects production handcrafted search for this source. The generator's search score determines moves, **not training targets**.
2. `HeadlessGame` records pre-move boards. `GameResult` stores terminal result in White orientation: +1 / 0 / -1.
3. `TrajectorySampler` retains evenly spaced positions and calls `result.target(position.sideToMove())`. A win for the sampled side to move is +1, loss -1, draw 0. There is **no CP source score and no division by 32511**: normalization is the identity on these already bounded WDL labels.
4. `Brn2SelfPlayTraining` freezes targets before updates and performs one deterministic shuffled pass, minibatch 1. The production WDL path calls `trainer.train(board, sample.target())`.
5. Both trainer and model use `Brn2Workspace`. Canonical features reflect ranks for Black and change absolute colour identities into US/THEM; material uses exactly the same STM orientation.
6. Physical material `M` is US minus THEM with 100/320/330/500/900/0 cp values. Its raw prior is `P=atanh(M/32511)` implemented with `log1p`.
7. Residual `R=b_out + sum_h(w_out[h] * ReLU(boardPre[h]))`. Board features are the sum of local ReLU activations followed by a second ReLU; there is no pooling normalization.
8. Combined raw output `z=P+R`; normalized output `y=tanh(z)`; public static score `sign(y)*max(1,round(32511*abs(y)))`, except exact zero maps to zero.
9. Loss `L=.5*(y-target)^2`. Backpropagation correctly uses `dL/dz=(y-target)*(1-y*y)`. The fixed prior has no trainable parameters. Output gradients are this derivative times pooled activation; upstream gradients use pre-update output weights and the correct ReLU masks. The intended implicit residual target is `atanh(target)-P`, **not** a second copy of the full target added at inference.
10. Adam aggregates each touched sparse row per sample and updates dense head/biases. Global-step bias correction is `1-beta^step`; update is `w-=lr*(m/c1)/(sqrt(v/c2)+epsilon)`. Candidates are finite-checked before publication.
11. Snapshots retain the prior enum. `Brn2Codec` reads/writes prior identity through payload format. `NetworkTrainingState.Brn2` and `NetworkModel.Brn2` retain it; checkpoint model/trainer weights agree. Validation constructs the pinned model's `SearchEvaluation.brn2`, with separate worker state, no fallback.

Representative samples from the frozen reproduction, all in the sampled board's STM perspective:

| Sample | Source terminal WDL = target | Public mapping equivalent | Material cp | Material raw | Implicit raw residual target |
|---|---:|---:|---:|---:|---|
| 3507, Black to move | -1 | -32511 | -530 | -.016303619 | negative infinity |
| 1386, White to move | +1 | +32511 | +430 | .013227064 | positive infinity |
| 1223, Black to move | 0 | 0 | -120 | -.003691075 | +.003691075 |
| 2418, White to move | +1 | +32511 | +1520 | .046787517 | positive infinity |

“Public mapping equivalent” is a numerical mapping, **not an independently meaningful centipawn label**. A terminal win label does not establish a +32511 cp static position. The actual retained training loss was initially .4056782311; mean online loss was .2517298686. Correctly reducing this loss can still destroy CP-calibrated move preferences.

NNUE is not involved in this failed run. The alternate blended path nevertheless has the same semantic concern: `BrnSupervision.teacherValue` uses `NnueEvaluator.boundedValue()` = `tanh(raw)`, with no CP conversion or calibration. `NnueEvaluator` explicitly defines raw as a side-to-move scalar with no CP interpretation; `NnueScoreMapping.V1` explicitly defines uncalibrated full-range search units. Sharing the number 32511 establishes no calibration between these outputs and BASIC_V1 centipawns.

## Early updates and Adam amplification

The following rows measure the deterministic production-equivalent shuffled reproduction. At steps 0-1000 the row is the **next sample before its update**; 4096 is the last sample after the completed pass. Different rows therefore need not have the same target or material. Loss is the displayed sample's loss, not the dataset average.

| Updates completed | Target | Material cp | Prior raw | Residual raw | tanh output | Public static score | Sample loss |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 0 | -1 | -530 | -.0163036 | 0 | -.0163022 | -530 | .483831 |
| 1 | +1 | +430 | .0132271 | -.00487555 | .00835132 | +272 | .491684 |
| 2 | 0 | -120 | -.00369108 | -.00256432 | -.00625531 | -203 | .0000195645 |
| 10 | +1 | +650 | .0199959 | -.000763300 | .0192302 | +625 | .480955 |
| 100 | 0 | +330 | .0101508 | .0255927 | .0357283 | +1162 | .000638255 |
| 1000 | +1 | +1520 | .0467875 | 1.167132 | .837851 | +27239 | .0131461 |
| 4096 | -1 | 0 | 0 | -.808834 | -.668947 | -21748 | .0547981 |

At update 1000, that sample's residual is 25 times its own material prior, 76 times a rook's raw contribution, and 42 times a queen's. A fixed +rook control at that same state has residual -.106582 and score **-2957**; after 4096 updates its residual is -.241099 and score **-7216**. At initialization it scores exactly +500.

Output-head and Adam summaries over the 32 head weights; bias moments are separately retained in `progress.tsv`:

| Updates | Head weight RMS | Output bias | First-moment RMS | Second-moment RMS |
|---:|---:|---:|---:|---:|
| 0 | 0 | 0 | 0 | 0 |
| 1 | .000999999 | -.001000000 | .00363967 | .00000211917 |
| 2 | .000496242 | -.000943246 | .0103104 | .0000210059 |
| 10 | .00122078 | -.00149540 | .00650158 | .0000462107 |
| 100 | .00530365 | .00263256 | .0132721 | .000971596 |
| 1000 | .0637584 | -.0561206 | .0802035 | .277326 |
| 4096 | .0890387 | -.0885014 | .284931 | .720535 |

The first actual production-equivalent update has `dL/dz=.9834364`. All 32 head gradients are positive, .00173162 to .0913903; Adam changes every weight by approximately **-.001**, regardless of its gradient magnitude. Its measured residual change on that sample is -.00200633515; the analytical first-step prediction `-lr*(1+sum pooled activations)` is -.00200633548. Sparse upstream gradient is exactly zero on update 1 and becomes nonzero on update 2 (6273 nonzero entries, max .00297474). This verifies the reported delayed upstream learning through the production path.

An independent two-sample production-wrapper experiment exposes the architecture amplification on an equal-material opening, with two target +1 labels:

| Completed updates | Head weights approximately | Sum of pooled activations | Residual raw | Public score |
|---:|---:|---:|---:|---:|
| 0 | 0 | 7.68341 | 0 | 0 |
| 1 | +.001 | 7.68341 | .00868341 | +282 |
| 2 | +.00199976 | 23.18182 | .04835786 | +1571 |

Holding upstream weights at their update-1 values and applying only the second head update gives raw .01736474 (+564). Holding the head at update 1 and applying only the second upstream update gives .02418182 (+786). Both together give .04835786. Thus sum pooling and simultaneous upstream/head growth causally amplify a normal optimizer step.

Reducing those targets to .01 still gives +282 on step 1 and +1377 on step 2. A .001 target still gives +282 on step 1, then +17 on step 2 when the error reverses. Adam approximately normalizes the initial gradient to a learning-rate-sized parameter step; merely multiplying the loss/target scale cannot be assumed to multiply the initial update by that factor.

Repeated +1 training on the legal +queen control also isolates objective pressure without dataset complexity: residual 0 -> .00127975 -> .00288460 -> .03391955 -> 1.668617 -> 2.658668 at 0/1/2/10/100/1000 updates. Scores go 900 -> 942 -> 994 -> 2000 -> 30396 -> 32211 while loss drops .472700 -> .000042690. Large residuals here are the objective's intended direction, not a numerical fault.

## Controlled material positions with the actual models

FEN family: `7k/<THEM on rank 7>/8/8/8/8/<US on rank 2>/7K w - - 0 1`; pieces are on file a, and two rooks use a2/b2. These are legal static-evaluation controls. Rule adjudication can correctly declare some minor-piece controls drawn; their static values below are not search/game results.

Every Gen-0 residual is exactly zero and every Gen-0 public score equals the material column. Black-to-move controls use identical placement and reverse the material sign. “Reverse residual” is separately measured, not assumed to be the negative of the White residual.

| Control | Material cp | Prior raw | Gen-1 residual raw | Gen-1 score | Reverse residual raw | Reverse Gen-1 score |
|---|---:|---:|---:|---:|---:|---:|
| Equal pawns | 0 | 0 | .161720 | +5212 | .161720 | +5212 |
| +pawn | 100 | .003075892 | .182017 | +5950 | .0840818 | +2628 |
| -pawn | -100 | -.003075892 | .0840818 | +2628 | .182017 | +5950 |
| +knight | 320 | .009843140 | .0397901 | +1612 | .0539638 | +1433 |
| +bishop | 330 | .010150759 | .0308482 | +1332 | .0670223 | +1847 |
| +rook | 500 | .015380623 | .0378133 | +1728 | .124829 | +3544 |
| +queen | 900 | .027690013 | .0521900 | +2591 | .0485133 | +677 |
| Rook versus knight | 180 | .005536644 | .102495 | +3499 | .0414299 | +1166 |
| Queen versus rook | 400 | .012304149 | .0700603 | +2672 | .00493555 | -240 |
| Two rooks versus queen | 100 | .003075892 | -.0168379 | -447 | .166554 | +5268 |

Actual Gen 1 at the standard start: material 0, residual .47780906, score +14451. All head weights are finite: min -.1411574, max .1384095, RMS .08004573; output bias .06070621. All parameter absolute values are at most .2245606. Head first moments have RMS .08434296, second moments .64496049; no corrupt/nonfinite state was found.

The learned STM function is not constrained to be odd under changing only STM, and the large optimism on both sides is a learned calibration/generalization failure. It is **not evidence of a boundary sign bug**. Chess-equivalent rank reflection plus colour reversal preserves canonical material and model output exactly in the boundary experiment.

## Numerically explained material giveaways

Before the first bishop giveaway, the actual root is:

`r1bqkbnr/pppppppp/n7/8/P6P/3P4/1PP1PPP1/RNBQKBNR w KQk - 1 4`

Forced-root-move evaluations use the production child search and qsearch, with scores reoriented to White. The two response leaves are quiet and their static scores agree with the searched score:

| Model and line | Material cp | Prior raw | Residual raw | Total score |
|---|---:|---:|---:|---:|
| Gen 0: Nc3, ...Rb8 | 0 | 0 | 0 | 0 |
| Gen 0: Bh6, ...Nxh6 | -330 | -.01015076 | 0 | -330 |
| Actual Gen 1: Nc3, ...Nb4 | 0 | 0 | 1.78716504 | +30738 |
| Actual Gen 1: Bh6, ...Nxh6 | -330 | -.01015076 | 1.84668199 | +30900 |
| Same Gen 1, residual .01: Nc3, ...Nb4 | 0 | 0 | .01787165 | +581 |
| Same Gen 1, residual .01: Bh6, ...Nxh6 | -330 | -.01015076 | .01846682 | +270 |

Gen 1's residual increase **.05951695** is 5.86 times the raw bishop cost **.01015076**. This makes the giveaway's total raw value larger. Tanh preserves that ordering. Full depth-2 root search chooses Bh6 (+30900), with **both one and twelve workers**. The scaled copy chooses Nc3 (+581). This intervention changes only the residual contribution of the same failed weights and reverses the bad preference.

The same fragment later supplies independent queen and rook failures:

| Actual Gen-1 line | Material cp after response | Prior raw | Residual raw | Total score | Same leaf with residual .01 |
|---|---:|---:|---:|---:|---:|
| Qe2-d2, ...Qxh4 | -950 | -.0292292 | .0779850 | +1584 | -925 |
| Qe2-e5, ...Kxe5 | -1750 | -.0538800 | 1.3441803 | +27934 | -1314 |
| Rh3-h1, ...Qxh4 | -2280 | -.0702454 | .4336288 | +11320 | -2140 |
| Rh3-e3, ...Kxe3 | -2680 | -.0826211 | .9874161 | +23363 | -2361 |

The preserving alternatives lose an exposed pawn; the other lines give away an additional queen or rook without compensating capture. Actual Gen 1 chooses the queen and rook giveaways. Gen 0 ranks the preserving alternatives higher. Scaling the same residual also reverses both pairwise preferences. Exact roots and leaves are retained in `move-choices.tsv`.

Saturation compounds the problem. With residual held at the safe bishop leaf's 1.78716504, changing only material 0 -> -330 changes the continuous public score **30737.87 -> 30702.51**, a cost of only **35.35**. In general,

`d public_score / d material_cp = (1 - tanh(P+R)^2) / (1 - (M/32511)^2)`

before integer quantization. Thus the formula guarantees literal material values at zero residual, but does **not** guarantee a 330-unit marginal material penalty once the residual is large. At residuals near saturation, the fixed prior offers weak practical protection.

Changing only the quiet halfmove clock also moves actual Gen-1 residuals by large amounts. However, the bishop-losing leaf still outranks the safe leaf when both clocks are set to the same tested value (0,1,2,3,4,8,16). Clock differences are not the sole explanation for the giveaway.

## Causal ablations

All trained arms use the same 4096 frozen positions, targets except where explicitly changed, shuffle order, one production online pass, and fresh optimizer state. RMS is raw residual over those fixed training positions, not CP error. Playing endpoints are one depth-2 fragment against material Gen 0; they do not establish strength.

| Arm | Final sample-set loss | Residual RMS | Bounded fragment result |
|---|---:|---:|---|
| BASIC_V1, zero head, WDL | .0911603 | 1.83108 | -1250 cp at 64 plies |
| Prior OFF, same zero head, WDL | .0883411 | 1.86466 | -3300 cp at 64 plies; lone king |
| BASIC_V1, former random head, WDL | .0933068 | 2.05644 | -1600 cp at 64 plies |
| Legacy NONE and former random head, WDL | .0915796 | 2.05754 | Checkmates opponent at ply 31, while down 900 cp |
| BASIC_V1, lr .00001, WDL | .404754 | .0394007 | +220 cp at 64 plies |
| BASIC_V1, targets WDL times .01, lr .001 | .0000299805 | .0119857 | Checkmates opponent at ply 33, while down 100 cp |
| BASIC_V1, literal M/32511 targets | .000000288775 | .000760115 | Threefold at ply 12, equal material |
| BASIC_V1, exact initial predictions as targets | 0 | 0 | Threefold at ply 12, equal material; all weights unchanged |

Interpretation:

- Prior omission is not needed for large residuals; disabling the prior does not remove them.
- Zero head is not needed for large residuals or a severe material regression in this controlled BASIC_V1 fragment. Former initialization alone is not a supported fix. Initialization affects the learned function and tactics; it is not universally irrelevant, as the legacy-NONE arm's mating result demonstrates.
- Target-scale intervention reduces residual RMS about **153-fold** while retaining the optimizer and architecture. This distinguishes objective pressure from a broken prior implementation.
- A smaller learning rate suppresses early magnitude but largely stops learning the WDL objective; it does not resolve its semantics.
- The literal M/32511 arm has tiny floating-point differences from `tanh(atanh(M/32511))`. Adam amplifies these enough to create small drift. The exact-initial-prediction arm has exactly zero loss, gradient, moments, and residual after 4096 successful optimizer boundaries. There is no spontaneous state corruption.
- Post-training .01 scaling is especially direct evidence on the **actual failed checkpoint**, but it is not an approved production calibration or learning objective.

The reproduced WDL set has 1630 negative / 640 draw / 1826 positive targets and 3315 unique canonical feature positions. Three repeated feature groups contain conflicting outcome labels, covering 132 samples. Finite-game WDL supervision includes early equal-material positions with eventual decisive results. Generalization and outcome noise contribute; no single hidden feature has been claimed as the exclusive cause.

## Boundaries and hypotheses tested

The actual Gen-1 model was checked on 133 sampled positions, their legal children, and their chess-equivalent colour/rank reversals. Trainer prediction, model workspace, reference workspace, rebuilt/delta accumulator, main-search state, and separately initialized qsearch state had **zero public-score mismatches**. Maximum observed raw difference was `4.440892098500626e-16`.

Finite-difference checks of output bias, head weight, and an upstream sparse parameter at the first two production-equivalent updates agreed with analytical gradients, maximum absolute error `3.36e-11`. Upstream gradient is exactly zero on update 1 and nonzero on update 2. There is no incorrect tanh derivative or double-learning of material.

Actual Gen-1 training state was serialized/reloaded, then both original and restored diagnostic copies received ten identical updates: **complete training payload bytes matched**, BASIC_V1 retained, step 4096 -> 4106. Relevant existing checkpoint/material tests also cover format-1 compatibility, initialization, snapshots, continuation and trainer wiring. Frozen-replay routing was traced to the same BRN-2 trainer; a separate whole frozen-replay campaign was not run. Capture-gradient tests passed, but capture is OFF in the actual run.

| Hypothesis | Finding |
|---|---|
| A: zero head + Adam | Early amplification is real and contributes; a unique zero-head numerical explosion is ruled out by the former-head and zero-gradient arms. |
| B: target semantics incompatible with CP prior | Confirmed. Actual targets are terminal WDL, not CP; desired winning raw residual is unbounded. Target scaling causally suppresses magnitude. |
| C: training omits prior or learns full target twice | Ruled out by shared workspace, numerical equality, loss/gradient checks and exact-initial-target control. |
| D: wrong combined derivative | Ruled out by code trace and finite differences. |
| E: perspective/sign mismatch | Ruled out at traced boundaries and numerical colour/rank reversals. Learned non-odd/optimistic values remain a generalization issue. |
| F: prior identity lost | Ruled out for actual model, optimizer, snapshots, save/reload and continuation. |
| G: game inference differs from tested inference | Ruled out on sampled transitions and representative searches, including 12 workers. |
| H: search independently corrupts scale/sign | No evidence. Search correctly prefers the larger bad static value. Draw/mate adjudication remains separate. Saturating evaluator math is a contributing interaction. |
| I: interacting factors | Confirmed: WDL/CP scale mismatch, optimizer/feature amplification, early generalization errors, and saturation. |

## Meaning of the GUI score

`ActiveGameFeed.moved` publishes the completed root `SearchResult.score()` that selected the last move, **before that move was applied**, reoriented to White. `TrainingBoard.score` displays that number. The board displayed beside it is the resulting position, so the number is not a fresh static evaluation of that displayed board and may come from either participant's model.

The mate threshold is 32512, with mate base 32768. **+29609 is an ordinary completed search score, not a mate-distance code and not the clipping limit.** Its exact original source actor/FEN/PV was not retained, so that screenshot number alone does not prove a saturated static BRN output. The headless reproduction separately does prove actual static saturation on identified search leaves, including +30900 after losing the bishop.

## Production correction options for reconciliation

**Preferred: define a CP-compatible full-position objective before changing optimizer/search.** Use a teacher or analytically defined source with established centipawn semantics, convert a bounded non-mate CP target `C` into `target=C/32511`, and train the existing combined output. Its implicit residual target is then `atanh(C/32511)-atanh(M/32511)`. A NNUE raw/bounded value is not such a teacher until calibrated. Alternatively retain a WDL head separately and derive a justified WDL-to-CP calibration; using ±1 as ±32511 CP is not that calibration. Target .01 WDL was only an experimental intervention, not a principled recommendation for a fixed constant.

This fixes the primary mismatch and preserves one public score unit per centipawn. It changes the objective, teacher/data requirements, loss magnitudes, and potentially terminal/mate-label handling. Existing format-2 files remain mechanically readable under unchanged inference, but this failed generation's learned weights and Adam state should not be treated as corrected by continuing under a new objective: start a separate fresh lineage and preserve this one as evidence. Restrict changed training semantics to BASIC_V1 if legacy format-1 objectives must remain unchanged.

**Companion: set optimizer/architecture update scales in CP terms.** After target calibration, choose verified head/upstream learning rates, warmup, or feature/pooling normalization so an ordinary initial step does not move a balanced position hundreds of CP and the second step over a queen. Consider an explicit CP residual parameterization with consistent derivatives and regularization if appropriate. This addresses the measured Adam and pooled-feature amplification. Side effects include slower learning, changed parameter semantics for normalization/parameterization, new optimizer compatibility requirements, and possible suppression of legitimate tactical compensation. It preserves the public CP contract if inference conversion is coherent. Changing only optimizer settings leaves format-2 inference bytes readable, but restarting optimizer/lineage is preferable to reusing the failed state; changing feature/output parameter semantics may require a new payload identity or an explicit proven conversion. Apply by prior/version so format 1 remains unaffected. **Do not choose a learning rate solely from the .00001 diagnostic arm.**

**Optional structural alternative: add material linearly in public CP space.** A coherent model such as `score_cp=M+residual_cp`, with an explicit bounded normal-score policy and corresponding target/loss/derivative, would avoid attenuating a material penalty when residual confidence is large. This changes the specified pre-tanh combination and must be deliberately approved. It does not alone make WDL labels into CP or cure residual overfitting. Side effects include altered saturation, normal/mate-band handling and serialization semantics. Public units remain CP, but existing format-2 weights cannot silently be reinterpreted; keep their existing path or introduce a new format/migration. Legacy format 1 can remain unchanged through versioned routing.

Restoring the former head, dropping the prior, clipping, or post-hoc .01 scaling is not supported as a complete production fix. In particular, scaling only the head while retaining WDL targets allows optimization to grow it again.

## Validation, reproducibility and preserved work

The targeted Gradle test selection passed **30 tests, zero failures/errors/skips**: `Brn2MaterialPriorTest` (5), `Brn2TrainingTargetsTest` (3), `Brn2CheckpointTest` (11), `Brn2SearchIntegrationTest` (2), `Brn2AccumulatorTest` (5), `Brn2CaptureGradientTest` (4). Production and verification compilation succeeded. The standalone diagnostic ran headlessly, without Swing or browser interaction.

Additional experiments actually run: read-only binary/header/CRC/hash inspection; eight-game timing probe; the bounded 128-game serial frozen data generation; 4096-update instrumented replay and production-wrapper equivalence; eight same-data ablations; 1000-update constant-win experiment; two-update target-scale/hybrid-weight experiments; 133-position boundary comparisons; finite differences; actual-state save/reload continuation; controlled material/perspective evaluation; depth-2/depth-4 tactical searches and bounded fragments; twelve-worker bishop search; halfmove-clock interventions.

No normal lineage training, replacement promotion, full tournament, Elo test, long benchmark, broad/slow suite, GUI launch, or browser verification was performed. They are unnecessary to distinguish the proven mechanism and would exceed the diagnostic expense boundary. Long-run calibrated-objective strength and optimal optimizer settings remain unmeasured; they belong to a later approved work unit. Exact reproduction of the original training trajectory remains unavailable because its boards were not retained.

Retained diagnostic-only Java entrypoint:

`app/src/verification/java/com/ohinteractive/seedv6/core/brn2/Brn2MaterialTrainingDiagnostic.java`

It is excluded from shipped source output. It never creates a store writer. `generate` writes only the explicitly supplied sample output; all training/weight interventions are isolated in-memory copies. The checked-in investigation uses this named Gen-0/Gen-1 case and its recorded seeds; it is not a generic lineage trainer.

Evidence directory: [brn2-basic-v1-2026-10-02](evidence/brn2-basic-v1-2026-10-02). It contains UTF-8 TSV output, the exact 4096-sample diagnostic binary, and preservation hashes. Sample wire format: big-endian int count, then six signed longs and one double per sample. No failed-lineage payload was copied into Git.

Reproduce from PowerShell after `./gradlew.bat :app:compileVerificationJava`:

```powershell
$diagCp = 'app/build/classes/java/verification;app/build/classes/java/main'
$failedRoot = 'E:/SeedV6-Networks/BRN-2/8a5ad5dd-6646-4741-8ab7-8b188069b73e'
$sampleFile = 'docs/research/brn/evidence/brn2-basic-v1-2026-10-02/samples.bin'
java -Xmx2g '-Djava.awt.headless=true' -cp $diagCp com.ohinteractive.seedv6.core.brn2.Brn2MaterialTrainingDiagnostic $failedRoot progress $sampleFile
java -Xmx2g '-Djava.awt.headless=true' -cp $diagCp com.ohinteractive.seedv6.core.brn2.Brn2MaterialTrainingDiagnostic $failedRoot boundaries $sampleFile
java -Xmx2g '-Djava.awt.headless=true' -cp $diagCp com.ohinteractive.seedv6.core.brn2.Brn2MaterialTrainingDiagnostic $failedRoot failures
java -Xmx2g '-Djava.awt.headless=true' -cp $diagCp com.ohinteractive.seedv6.core.brn2.Brn2MaterialTrainingDiagnostic $failedRoot ablations $sampleFile
```

Only this report, diagnostic entrypoint, and its evidence files were added. No disposable production instrumentation remains. External scratch outputs under `C:\Temp\seedv6-brn2-20261002` contain compiled diagnostic classes and intermediate output; they do not affect application behavior.

All 23 pre-existing modified/untracked repository files and all 22 original lineage files were hash-checked before and after the investigation, unchanged byte-for-byte. The original 17 tracked modifications and six untracked files remain separate from these diagnostic additions. No commit or push was made. `CODEXLOG_CURRENT.md` was absent, so no journal was created. Version finalization uses **no_bump**, retaining build 32 for this non-runtime work unit.

Human actions required after this prompt: None. Production correction and any new training lineage require subsequent GPT/user reconciliation and authorization; neither is part of diagnostic completion.
