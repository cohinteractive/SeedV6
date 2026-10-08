# Completed BRN research: findings and evidence map

Consolidated 2026-10-08 (Pacific/Auckland), from checkout `791814d`. The research
programme is complete. This document preserves the conclusions needed after local
build-tree evidence is retired; [the retention guide](BRN_ARTIFACT_RETENTION.md)
identifies the few proposed binary exceptions. No new experiment or cleanup was
performed. Results below are historical measurements, not new runtime validation.

**Final research choice: C02 compiled order-2 relational tables.** The subsequent
application integration exposes this family as **BRE-Pair 2**, identity `BRN_PAIR2`.
Native BRN-3 remains supported. Neither the research recommendation nor integration
automatically selected a research checkpoint as a user's Best or changed the
default evaluator. The earlier recommendation's “integration not yet performed”
language describes its own boundary; the later [Pair-2 guide](../../brn/BRN_PAIR2.md)
records the implemented capability and its limits.

## Durable knowledge already present

Keep the existing tracked records; do not replace their detailed tables with this
overview. Before this consolidation, `docs/brn/`, `docs/research/brn/` and
`docs/research/brn-architecture-cglhw/` held 67 tracked Markdown files (1,887,896
bytes), and 385 tracked files including compact evidence (33,114,529 bytes).
The large experimental tree is not needed just to read those findings.

| Research question | Retained record and what it establishes |
| --- | --- |
| Early BRN training, source/teacher separation, lifecycle and symmetry | [Bootstrap](BRN_BOOTSTRAP.md), [remediation](BRN_REMEDIATION.md), [diagnostic index](BRN_DIAGNOSTICS.md), [color symmetry](BRN_COLOR_SYMMETRY.md), [handcrafted generation](BRN_HANDCRAFTED_POSITION_GENERATION.md) |
| BRN-2 supervision and capture consistency | [Canonical g128](BRN_CANONICAL_G128_DIAGNOSTICS.md), [ablation](BRN_SUPERVISION_ABLATION.md), [weight sweep](BRN_SUPERVISION_WEIGHT_SWEEP.md), [strength screen](BRN_SUPERVISION_STRENGTH_SCREEN.md), [capture integration and preceding studies](BRN_CAPTURE_CONSISTENCY_PRODUCTION_INTEGRATION_2026-09-24.md), [multi-generation acceptance](BRN_CAPTURE_CONSISTENCY_MULTI_GENERATION_ACCEPTANCE_2026-09-24.md) |
| Scale/objective failures and causal limits | [BASIC_V1 regression](BRN2_BASIC_V1_TRAINING_REGRESSION_DIAGNOSTIC_2026-10-02.md), [NNUE replacement calibration](BRN2_NNUE_CP_REPLACEMENT_CALIBRATION_2026-10-02.md), [g134/G4 attribution limits](BRN_G134_STRENGTH_AND_G4_REVERSAL_DIAGNOSTIC_2026-09-24.md) |
| BRN-3 architecture, learning and calibration | [Programme ledger](BRN_PROGRAMME_LEDGER.md), [V1 result](BRN_V1_RESULT.md), [learning result](BRN_LEARNING_RESULT.md), [learning reproduction](BRN_LEARNING_REPRODUCTION.md) |
| Training speed and successor experiments | [Throughput research](BRN_THROUGHPUT_RESEARCH.md), [successor research](BRN_SUCCESSOR_RESEARCH.md), [successor state](../../brn/BRN_SUCCESSOR_STATE.md) |
| Final architecture comparison and methodology | [Recommendation](../brn-architecture-cglhw/RECOMMENDATION.md), [scorecard](../brn-architecture-cglhw/SCORECARD.md), [final results](../brn-architecture-cglhw/Z01-FINAL-RESULTS.md), [completion audit](../brn-architecture-cglhw/COMPLETION-AUDIT.md), [retained orchestration](../brn-architecture-cglhw/reproduction/README.md) |

The contracts preserve prospective gates; ledgers preserve intermediate hypotheses,
negative runs and changes of direction. Read dated results in order: an early
family file's provisional disposition does not override the final results. Old
version-reservation warnings are historical; [the later remediation](../../version-finalizer.md)
resolved that reservation. None of these records implies universal playing strength.

## Architectures and mathematical approaches

| Family | Mechanism and supported disposition |
| --- | --- |
| BRN-0 / BRN-1 / BRN-2 | Progression from a sparse scalar primitive-feature evaluator to width-32 pooled embeddings/ReLU/tanh (BRN-1), then width-32 local endpoint composition, ReLU and board pooling/value head (BRN-2). Current definitions remain in `core/brn`, `core/brn1`, `core/brn2`. Bootstrap/lifecycle success was not proof of stable scores or strong play. |
| R0 / R1 / R2 / R3 | Additive unary/typed-displacement potentials; factorized all-pair product pooling; directed local messages; king-anchored absolute geometry, respectively. The [ledger](BRN_PROGRAMME_LEDGER.md) preserves objectives, gradients, timing and failed quality gates. The old R0 recipe is not a rejection of every modern learned unary/PST model. |
| R4 / native BRN-3 | Absolute unordered all-pair embeddings, eight channels, node-local ReLU, 12 owner/type pooling groups, dual mover/opponent perspectives, 32-unit ReLU readout. Training-only relative displacement sharing folds into the inference table. This was the successful native BRN architecture. |
| Successor A/I/F experiments | Absolute versus relative/hash/factorized sharing, width 16, max/square pooling, directed messages, context/SELF depth and rule-state probes. No BRN-4 promoted: the tested context successor failed final play; exact-semantic R01 cache updates survived. |
| B01 / D01 | Linear and low-rank energy/product readouts; nested cubic interactions. Learned useful functions, but the tested cost/strength trade-offs did not earn the final recommendation. |
| C01 / C02 | Discrete two-/three-square categorical potentials with translation-sharing during training. Order 2 ignores the third cell of the same templates. C02 compiles the selected order-2 function into direct physical-pair lookup tables without changing its fitted capacity or weights. Final practical recommendation. |
| E01 / F01 | Learned piecewise-linear edge functions and one-hop CONTEXT/SELF. Edge functions are a predictive-quality alternative, without an established playing lead. The later causal context comparison scored 40.625%, both fresh runs below 50%. |
| G01 | Spatial bitboard logic: learned softmax over 16 Boolean truth functions during training; hard bitwise gates, shifts and popcounts at inference. One-/two-layer models have 1,537/3,073 trainable parameters. Raw gate-learning benefit did not establish a direct playing benefit over fixed gates. |

The final family specifications and unsuccessful descendants remain in
[B01](../brn-architecture-cglhw/B01.md), [C01](../brn-architecture-cglhw/C01.md),
[C02](../brn-architecture-cglhw/C02.md), [D01](../brn-architecture-cglhw/D01.md),
[E01](../brn-architecture-cglhw/E01.md), [F01](../brn-architecture-cglhw/F01.md) and
[G01](../brn-architecture-cglhw/G01.md). These dispositions concern tested recipes;
they are not impossibility results or direct pair-versus-every-family tournaments.

### Retained evaluator and training specifications

Native BRN-3 and Pair-2 use fixed material values P/N/B/R/Q/K =
1/3.2/3.3/5/9/0 pawns plus a learned residual, initially exactly zero. Placement,
piece ownership/type, geometry and side to move enter the learned function;
attacks, castling/en-passant rights, clocks and history do not. Color/rank/ownership
equivalence is distinct from changing only side to move: a tempo effect is allowed.
Ordinary calibrated search uses `material + 0.25 * residual`, then 100 units/pawn
with symmetric rounding and static-score limits. Historical gain-1 experiments
and the final architecture campaign's separately selected gains must be explicit.

BRN-3 has 2,368,577 inference and 2,627,777 training parameters; payloads are
9,474,352-byte models and 63,066,732-byte optimizer states. Its successful recipe
was batch 128, eight epochs, masked Adam lr .003, betas .9/.999, epsilon 1e-8,
pure expected-score cross entropy through the frozen Stockfish WDL link. Absent
coordinates' moments stay unchanged. Reported half-MSE is a diagnostic/selection
metric, not the optimized CE objective. Relative sharing, stable CE gradients and
typed pooling helped; wider embeddings, forced STM antisymmetry and dense-history
Adam did not justify their tested costs. [V1 specification](BRN_V1_RESULT.md).

Pair-2 uses 327 anchored templates over eight shapes, 13 square categories
(empty plus 12 owner/type states), two canonical roles and normalization
`1/sqrt(654)`. Its residual is bias plus the normalized active absolute/shared
entries. Training has 113,231 binary64 parameters and masked Adam; shared entries
fold into 110,527 binary32 inference parameters. C02 merges duplicate/reversed
terms into **294 physical pair tables**, with binary64 compiled values per STM.
Changes update incident pairs; worker caches are private and immutable tables shared.
The tuned research rate is .01, batch 128, the same CE link and Adam constants.
The two selected research payloads are 442,124 bytes each. Compiled primitive
storage is 799,688 bytes plus 2,920 bytes/worker, excluding JVM overhead.

Application Pair-2 uses a distinct 795,032-byte `network.brn-pair2` codec and a
2,731,424-byte `training.state`; research optimizer states are 2,731,396 bytes.
Research `.model` files are not ordinary application checkpoints. Compiled runtime
tables cannot reconstruct unfused weights/moments or resume training. The
[Pair-2 guide](../../brn/BRN_PAIR2.md) preserves loading, CRC/header, source-cursor,
Best/latest-training and lifecycle rules. Fresh application training and research
within-time checkpoint selection are different workflows.

## What the measurements established

### Early BRN-2 and native BRN-3

- Canonical BRN-2 schema 2 restored exact structural color symmetry, but its g121
  Best after 128 generations still capped Kiwipete at one million nodes after depth
  3; the pinned NNUE completed depth 4 in 41,435 nodes. Symmetry did not remove
  color-independent volatility. See [g128](BRN_CANONICAL_G128_DIAGNOSTICS.md).
- The supervision replay used 208,104 training and 52,974 held-out samples from
  128 saved generation batches, online Adam .001. For teacher weight `w`, target
  was `(1-w)*terminalWDL + w*boundedNNUE`, with half squared error. Teacher loss
  improved across sampled weights 0/.25/.5/.75/1 while stability/search cost did
  not improve monotonically. The 768-game [strength screen](BRN_SUPERVISION_STRENGTH_SCREEN.md)
  favored 75% by point score, but BRN-versus-BRN intervals were inconclusive.
  Exact replay of the same generated batches was not independent generalization.
- BRN-2 BASIC_V1's WDL target/full-range output/material-scale combination could
  overwhelm material despite valid gradients and codecs. The [regression diagnosis](BRN2_BASIC_V1_TRAINING_REGRESSION_DIAGNOSTIC_2026-10-02.md)
  did not identify serialization or search-sign errors. The later
  [NNUE-to-CP route](BRN2_NNUE_CP_REPLACEMENT_CALIBRATION_2026-10-02.md) closed:
  a removal-based candidate `CP=749.811891*raw` had held-out MAE/RMSE
  174.48/230.56 CP; replacement responses remained incompatible across piece
  classes and contexts. This is a rejected conversion, not a usable calibration.
- Capture-consistency work tested a Huber penalty on parent/child score changes
  relative to a pinned teacher, alongside the actual campaign target. It became
  an experimental default-off capability. Fixed-checkpoint diagnostic improvement
  did not establish durable strength improvement; [multi-generation](BRN_CAPTURE_CONSISTENCY_MULTI_GENERATION_ACCEPTANCE_2026-09-24.md)
  and [g134/G4](BRN_G134_STRENGTH_AND_G4_REVERSAL_DIAGNOSTIC_2026-09-24.md)
  retain reversals, mixed results and the unresolved concurrent-store integrity incident.

Native BRN-3's frozen V1 comparison used two source ranges/seeds 71/97, each with
131,072 training, 16,384 validation and 16,384 test positions, and 1,048,576 training
exposures. Selected BRN epochs were 8/6 versus NNUE 8/8. Pooled sealed half-MSE
was **.069986193 versus .064947225**, ratio **1.077586**, geometry-group bootstrap
95% **[1.046175, 1.107427]**. Both seeds scored 198/0/2 W/D/L in 200 games at
25 ms/move against their fresh NNUE controls. Shared openings across seeds and
short time controls preclude broad Elo or mature-NNUE claims. These are the
original V1/full-residual results, not measurements of the later calibration.
[Sealed evidence](evidence/programme-2026-10-03/brn3-final-sealed-test.json),
[V1 result](BRN_V1_RESULT.md).

The later learning study found raw residuals nearly cancelling material along
actual losing trajectories; a Gen9 example scored +.45 pawns while down 18.4
material pawns. Quarter-residual calibration on frozen weights improved practical
play even as reference CE worsened from .68086 to 1.13528. Gen9 scored
**102/17/9 (86.328125%)** over 64 depth-4 color pairs against material-only Gen0;
the separate depth-6/12-worker confirmation scored **44/18/2 (82.8125%)** over
32 new pairs, bootstrap 95% [76.5625%, 88.28125%]. All seven confirmation endpoints
totaled 832 completed games, with shared openings explicitly clustered. Exact
continuation from Adam step 73,728 to 81,920 and 90,112 retained utility but did
not prove monotonic improvement. Scalar/cache error was not found; upstream
target/sampling/representation causality remains unresolved.
[Learning result and limitations](BRN_LEARNING_RESULT.md),
[depth-4 evidence](evidence/learning-2026-10-03/confirm-g9-d4-quarter-64.json),
[production confirmation](evidence/learning-2026-10-03/confirm-g9-d6-production12-32.json).

The successor programme retained BRN-3 plus R01 count-changing relation-cache
updates. Context scored **44.53125%**, cluster 95% **[40.04%, 49.02%]**, at 25 ms
in its 256-game final comparison; no BRN-4 promotion. R01 preserved search identities
and improved bounded capture/search cost, with quiet/sibling microbenchmark
regressions disclosed. [Successor result](../../brn/BRN_SUCCESSOR_STATE.md),
[confirmation](evidence/successor-2026-10-04/f01-primary-confirmation.json).

### Final C02 comparison

Two fresh adjacent source/order ranges A/B (seeds 211/337) each contain
262,144 training, 32,768 validation and 32,768 sealed positions. Raw ordinal ranges
are `[2351172,2735463)` and `[2735463,3122388)`. Geometry-group assignment,
deduplication and uniform held-out reservoirs address exact/geometry leakage;
missing game IDs leave game/source correlations unresolved. The selected pair
models are the **300-second endpoints** chosen within the frozen **600 optimizer
second** opportunity, not the last checkpoints. Selected exposure counts are
26,161,792 / 24,971,392; the `.model` hashes are in the retention guide and
[selection evidence](../brn-architecture-cglhw/z01-engine-scaling-models.json).

The final campaign used unchanged production exact/PVS static-leaf search,
one worker, private 4 MiB TT, 100 ms/move, 128 common fresh reversed-color opening
pairs per run. All 768 batches / 3,072 games completed without caps or failures;
each direct comparison has 512 games. Pair gain was .25; fresh BRN/current
NNUE/strict NNUE gains were .125; captured native BRN used .5.

| Opponent | Pair W / D / L | Score | Approximate two-way bootstrap 95% |
| --- | ---: | ---: | --- |
| Fresh BRN-3 | 384 / 53 / 75 | 80.18% | 75.39–84.57% |
| Current material NNUE v2 | 349 / 57 / 106 | 73.73% | 69.53–77.73% |
| Strict research NNUE | 351 / 65 / 96 | 74.90% | 70.12–79.49% |
| Cheap material | 277 / 104 / 131 | 64.26% | 57.23–70.70% |
| Captured historical native BRN | 241 / 87 / 184 | 55.57% | 50.98–60.16% |
| Compiled own Gen0 | 335 / 87 / 90 | 73.93% | 66.99–80.47% |

Score = `(wins + draws/2)/games`, not win rate. The primary intervals pass, but
the simultaneous bounded-pair lower bound is only 50.58% against material and
41.89% against native BRN; native's single-comparison bound is 44.75%. Its apparent
advantage is not robust under that conservative sensitivity analysis. Two training
runs are insufficient for population-wide certainty.
[Final results](../brn-architecture-cglhw/Z01-FINAL-RESULTS.md),
[machine results](../brn-architecture-cglhw/z01-final-match-results.json).

The cheaper realization is supported by same-function evidence: C02 reduced
paired depth-4 root time by **24.19%** (recomputed from retained aggregate times),
with matching scores/moves/depths/nodes.
Final pair game throughput was about **2.50M nodes/s**, versus .385M BRN and .780M
current NNUE in their direct comparisons. Different evaluators visit different
trees, so cross-family NPS is not an isolated evaluator speed ratio. At common
eight-epoch exposure, pair training took 21.84–22.22 optimizer seconds versus
103.16–109.22 BRN and 657.63–669.14 current NNUE; this is not equal-quality time.
[C02 cost evidence](../brn-architecture-cglhw/c02-combined-cost-results.json),
[recommendation](../brn-architecture-cglhw/RECOMMENDATION.md).

Pair sealed raw half-MSE **.128970/.125921** was worse than BRN
**.073677/.069346** and current NNUE **.105277/.112585**, despite stronger fixed-time
play. All 22 profiles completed 720,896 visits over 65,536 distinct positions;
original/compiled pair metrics matched exactly in the sample, with zero saturated
search scores. Pair loss plateaued/slightly regressed after 300 seconds while
NNUE continued improving. Compute-change confidence intervals included zero;
all eight data-size playing intervals included .5. No long-run scaling winner
was established. [Sealed quality](../brn-architecture-cglhw/z01-final-quality-results.json),
[scaling discussion](../brn-architecture-cglhw/Z01-ENGINE-SCALING.md).

## Implementation discoveries and limits

- Training throughput improvements retained parse-once FEN/direct planes, cached
  patterns/input slices, row stamps, contiguous Adam, exact lane-wise SIMD and a
  dense-head transpose. The 64k × 2 pipeline median fell **12.9644564 → 6.963636 s**
  (1.86174×), preserving state/selection. Row sorting, selective JSON, extra dense
  backward SIMD and 2/4/6-worker variants did not justify production complexity.
  RTX4060 partial FP64 optimizer offload lost its advantage after transfers;
  full-device training and actual 8M runs were not demonstrated.
  [Measured comparisons](evidence/throughput-2026-10-04/comparisons.json),
  [throughput record](BRN_THROUGHPUT_RESEARCH.md).
- Diagnostic qsearch and production search must not be conflated. Original V1's
  worst complete BRN/NNUE qsearch root used 838,203/46,717 nodes (17.94×); later
  quarter-residual Gen9 had a 654,180-node, 4.102-second isolated tail. The final
  architecture qsearch seam completed 703/704 roots, with one linear node-cap
  outcome. These are bounded diagnostic observations, not universal tactical
  safety or current production-qsearch guarantees.
- Search calibration can improve play while worsening teacher loss. Fixed-depth,
  fixed-time, matched-exposure and matched-maximum-time results answer different
  questions. Best versus latest-training, selected versus last endpoint, and
  original versus copied lineage are separate identities.
- The production Pair-2 integration checked 131,072 positions / 655,360 board/gain
  comparisons with zero observed error across both retained research models.
  Its historical record lists 153 passing tests and seven native-window skips;
  that is prior validation, not a suite rerun for this consolidation.
  [Integration evidence](../../brn/BRN_PAIR2.md).

### Build-only BRN-2 performance findings now preserved here

The following measurements were found in local `build/brn-diagnostics/`, not a
tracked result report. Their compact findings are preserved here before any
potential retirement. They describe commit `c04d747a3d174d83d647bc114003baa701ac67f7`
on Windows 11 / Oracle Java 21 in September 2026, not current engine throughput.

The headless `cross-host-pc/training-1x256` run used seed 20260926, depth 2,
256 HCE games and 64 validation pairs, six configured/one effective worker,
production ExactSearch with no qsearch. Whole run/generation were
103.273648/101.259661 seconds; 8,123 training samples; 115,514 root searches;
14,183,273 ordinary nodes; 75.516499 root seconds; maximum root 29.3489 ms.
Candidate was not promoted. The 32-game smoke used 44,428 roots / 5,645,966 nodes,
45.235 seconds overall. Production replay with configured one/six workers gave
the same 1,429 nodes across 15 roots; separate legacy replay used qsearch and
different work. None establishes a Mac/PC cause.

Three actual GUI Start Training runs (`gui-bootstrap-t3/run-1`, `run-2`, `run-3`)
used seeds 1/1, HCE depth 2, 128 units, 32 samples/unit maximum, WDL, Adam .001,
one online pass, capture off, 64 validation pairs, cap 1024. All trained 4,053
samples and promoted the identical candidate after 123/5/0 validation games.
GAME_PAIRS trained the whole sampled batch; there was no held-out split.

| Historical GUI measurement | Run 1 | Run 2 | Run 3 |
| --- | ---: | ---: | ---: |
| Whole generation seconds (phase analysis) | 59.472 | 58.090 | 56.314 |
| Validation seconds | 43.431 | 43.711 | 43.122 |
| Validation roots / ordinary nodes | 22,260 / 3,726,550 | 22,260 / 3,726,550 | 22,260 / 3,726,550 |
| p99 / maximum root ms | 7.320 / 24.187 | 7.208 / 30.401 | 7.140 / 48.164 |
| Maximum between-root gap ms | 67.284 | 73.833 | 74.183 |

All ordered search semantics matched. There were no 100-ms roots, 250-ms gaps,
one-second live-search watchdog events or multi-second stalls. Sixty standalone
replays of five selected events matched score/move/nodes; fresh versus restored
repetition history made no difference on that sample, and original TT contents
were not reconstructed. Timing order/JIT and telemetry overhead limit timing
causality. The earlier manual 124/206/140-second observations remain unexplained.
Raw capture counts were defective because production moves lacked the experimental
type bits; corrected legal captures came from board regeneration and all 22,260
legal-move counts reconciled. Zero raw counts must not be read as zero captures.

Source identifiers (historical files may later be absent):

- `build/brn-diagnostics/cross-host-pc/training-1x256/analysis.json`, SHA-256
  `5e5d1725b79d0d84eeace9a7f07b4dd1c0571e285c90954b543e2c3581424041`.
- `build/brn-diagnostics/gui-bootstrap-t3/analysis.json`, SHA-256
  `d7084bc8d4b03548da392be27287bc7e1b99b059510dc0b88635812e1c0a4136`;
  `analysis.md`, SHA-256 `25fe0102b0b0e5e3917a6a22d058b6f4f7e50d90b1ea8bd29cf0a8c311a2cb11`.

## Remaining scientific questions and preservation boundary

Longer/mature-control play, more independent sources/platforms, long continuation,
modern unary/PST controls, upstream material-cancellation causes and qsearch tails
remain unresolved. These are limitations and possible future questions, not a
newly authorized workstream. Opened test sets cannot become fresh tuning evidence.

Retained Markdown, source, plans and compact JSON preserve methods, measurements,
identities and interpretations. They do **not** preserve deleted neural weights,
optimizer moments, raw samples, per-ply traces, profiles or exact reproducibility.
The proposed minimal model set preserves the final selected functions; the optional
archive buys specific further analyses/continuations. It does not restore the whole
research programme. See [retention choices and losses](BRN_ARTIFACT_RETENTION.md).
