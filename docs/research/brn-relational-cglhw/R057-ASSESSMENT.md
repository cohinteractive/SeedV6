# R057 completed bounded ONE assessment

29 October 2026. Workstream `seedv6-brn-relational-20261029`.
**PAUSED — AWAITING HUMAN RESEARCH DECISION.** Both authorized questions were
investigated, including the complete principal training allocation. The frozen
development screen failed its continuation threshold; independent confirmation
was therefore not run. No follow-on experiment or production promotion is queued.
This is a research finding and recommendation, not owner acceptance.

**A. Did incremental ONE achieve CP64-like efficiency?** Yes for the small 8/4
configuration in the measured lifecycle and common-search workloads. No for the
selected, fully trained 16/4 model under the prospective joint criterion: its
lifecycle cost was 1.404 times CP64, exceeding the 1.25 limit, while its NPS ratio
was 0.802, just above the 0.8 limit. Thus low-rank relational inference is
computationally plausible, but the chosen capacity did not meet the whole criterion.

**B. Did ONE learn useful evaluation at CP64 scale?** Yes. The principal model
completed 33,554,432 samples times eight passes, or **268,435,456 exposures**.
Development sigmoid MSE fell from 0.0321507 to 0.0231604. Masking its relational
path worsened final held-out loss by about 24%; equivalently, the full model's
loss was about 19–20% lower than that ablation. This demonstrates a useful learned
relational contribution, not equivalence to a separately trained unary control.

**C. How close to CP64?** Final ONE loss was 35.6% higher on development and
42.1% higher on the fresh panel. In the complete 64-game development screen,
ONE scored **14 wins, 15 draws, 35 losses: 33.59375%**. The paired bootstrap 95%
interval was 23.4375–43.75%; the conservative Hoeffding interval was much wider,
9.586–57.602%. These results fail the frozen 40% screen and do not establish
competitiveness. They are not an independent confirmation or equal-time result.

**D. What explains the remaining differences?** The measured computational
bottleneck is recipient readout. The learning gap is observed but not causally
isolated: ONE has 31,489 parameters versus CP64's 1,970,440 inference parameters,
a different nonlinear representation, optimizer semantics, batch size, update
count, regularization schedule, precision and input order. Equal exposure removes
the previous gross exposure disparity; it does not control these differences.
One run cannot distinguish capacity limits from optimization or distribution effects.

**E. Is there a credible production path?** The incremental structure and small
configuration's cost are credible foundations. A production-competitive strength
and cost combination has not been demonstrated. This selected model is slower
and weaker in the measured comparisons. Quantization/vectorization or optimizer
changes might help, but neither outcome is established by this experiment.

**F. Further bounded work or suspension?** Keep the broader programme suspended.
Another full-scale run is not justified merely by increasing exposure again.
A specifically authorized small control isolating optimization/regularization
from capacity could be informative before any further substantial investment.
That is a possible human research choice, not an automatic next experiment or a
claim that all low-rank relational architectures are inferior.

The principal evidence is [training progress](r057-main-progress.json),
[inference measurements](r057-main-efficiency-results.json),
[prediction results](r057-main-prediction-results.json), and
[playing results](r057-main-playing-results.json). The prospective methods remain
in [R057](R057.md), [capacity protocol](R057-CAPACITY.md), and
[principal freeze](R057-MAIN.md). Earlier negative and failed attempts are retained.

**Mathematics, implementation and correctness.** For each board orientation,
the pool is `P[r,d] = sum_j K[j,r]*E[j,d]`. Recipient activation is
`(sum_r T[i,r]*P[r,d] - (sum_r T[i,r]*K[i,r])*E[i,d])/sqrt(R*(n-1))`.
Softsign is applied per recipient/channel, followed by its learned readout.
The material, unary and relational terms retain the original R056 scale and
self-edge exclusion. This is a genuinely relational, one-pass low-rank model.

Immutable piece-square products and self-products are precomputed. Each stack
frame maintains both orientations, unary totals, exact material tenths and piece
count. Changed-square deltas cover quiet moves, captures, promotions, castling,
en passant and null moves. Child frames preserve parents and siblings; explicit
copy ownership handles other search states. No dense two-pass architecture was
introduced. R056's original implementation and independent quadratic oracle remain.

Tests cover all those moves, colour/perspective consistency, 10,000 reversible
history transitions, transpositions, reset, state ownership, qsearch copies,
half-centipawn boundaries, repeatability and real search. Generalized-capacity
gradient, tiny-fit, checkpoint/optimizer restore, packed decoding, replacement
ordinals and complete-epoch permutation tests pass. Final runtime validation used
13 focused tests; across this RESUME root, 21 distinct focused tests passed
([validation inventory](r057-resume-validation.json)). Prior-root core/CP64 tests
remain separately recorded; they were not represented as newly rerun here.

Tolerance was fixed at `1e-8 cp + 1e-12*abs(reference cp)`. On the final trace,
39,566 individual readouts had maximum absolute error **1.819e-12 cp**, with zero
raw rounded or actual search-score differences. Full and incremental ONE agreed
on repeated root moves, scores, depths and node counts. The 16,384 prediction
positions had maximum error 1.507e-12 cp. Earlier randomized histories reached
4.093e-12 cp. Reassociation is mathematically equivalent, not bitwise identical.
A guard rebuilds in reference order within 1e-6 cp of rounding discontinuities;
targeted tests cover it. The finite/clipped supported domain and tested histories
do not constitute a universal floating-point proof or production qualification.

**Complete inference lifecycle.** Final model, Ryzen 5 5500, Java 21,
one logical CPU, SerialGC, 512 MiB heap, BelowNormal. Frozen CP64 uses its normal
`vector-256-long` head. Thirty phase/special roots, nine rotated timing rounds,
three fixed-node search rounds and 90 requests per evaluator. The shared trace
has 175 initializations, 45,000 child updates and 39,566 readouts. This flat search
shares main/qsearch storage and made zero explicit cross-state copies; separate
tests establish copy correctness, not timed copy frequency.

| Median cost, microseconds | Full ONE 16/4 | Incremental ONE 16/4 | CP64 |
|---|---:|---:|---:|
| Full evaluation / rebuild plus readout | 2.528 | 2.369 | 3.199 |
| Incremental update | — | 0.464 | 1.288 |
| Prepared-state readout | — | 1.470 | 0.595 |
| Update plus readout | — | 1.661 | 1.395 |
| Trace cost per readout, all state work included | 2.439 | 1.414 | 1.007 |
| Whole-search nodes/second | 352,476 | 549,640 | 685,699 |

The optimized trace is about 42% faster than full ONE. Component medians need not
add because they are separate measurements. All measured hot-loop allocation
deltas were zero. The immutable ONE primitive payload is 620,816 bytes; a frame
is 1,081 bytes, excluding headers/alignment. Model initialization allocated
656,016 bytes and took 9.43 ms in this invocation. Creating all trace replay
states allocated 9,011,680 bytes for ONE versus 16,886,712 for CP64 and took
9.76 versus 4.57 ms. These include JVM/class-loading effects, not just steady
state loading. CPU/runtime hashes, all rounds and initialization evidence are saved.

The 8/4 trained pilot used 19,201 parameters and achieved lifecycle ratio 0.763
and NPS ratio 1.105; the 32/8 pilot used 62,209 parameters and ratios 3.955 and
0.348. The original untrained 8/4 audit also passed, but trained-model results
are more relevant. Its historical 25,409 stored fields included inactive TWO
weights. The selected model stores only its 31,489 active parameters.

Pool updates cost O(changed squares × rank × width); recipient-dependent nonlinear
readout remains O(pieces × rank × width). Increasing width is consequently not
free. Timings come from one JVM/machine, and the trace is ONE-generated. Within-JVM
rounds are correlated; min/max are descriptive rather than confidence intervals.
Different evaluators produce different whole-search trees. No cost result alone
implies learned quality or strength. Earlier scalar-head/oracle timing attempts
remain historical and are not used as the principal CP64 comparison.

**Data, capacity and training opportunity.** Original CP64 E006 data, source
descriptors, prepared BT4 units, targets and manifests were available. The verified
CP64 reference means 64 superbatches × 1,024 batches × 4,096 examples, including
the resumed first four superbatches: eight corpus-equivalent passes, not 268
million unique positions. Full hashes, all stored labels/provenance links and
66,560 upstream packing proof rows were checked. See [data audit](R057-DATA.md).

The original target method was retained: invert publisher BT4 Q, clamp probability
to [0.0001,0.9999], store rounded `400*logit(p)` with side-to-move normalization,
then train sigmoid squared loss with WDL mixture zero. These are link values,
not original engine centipawns. There was no CP64-teacher distillation. Every new
label and packed record was independently checked against the upstream converter.

| Quantity | Original CP64 | Principal ONE |
|---|---:|---:|
| Samples per equivalent pass | 33,554,432 | 33,554,432 |
| Canonical unique placements | 33,164,045 | 33,180,466 |
| Additional canonical duplicate samples | 390,387 | 373,966 |
| Completed principal exposures | 268,435,456 | 268,435,456 |
| Inference parameters | 1,970,440 | 31,489 |
| Training parameters | 2,167,048 | 31,489 |

ONE retained 33,515,036 original rows and replaced 39,396 (0.1174%) excluded
rows with same-source samples. Canonical keys normalize side to move and fold
file reflection; counts therefore group aliases, not all distinct complete FENs.
An exact sorted merge visited all 33M rows; an independent Python encoder checked
a frozen sample plus full counts. All replacements avoid original training,
validation and known reservations. Original duplicate visits are retained.
The full validation corpus and earlier diagnostic reservations are excluded.

Original selection used quiet, noncheck positions at ply ≥16 from shuffled units
and bounded prefixes. The final corpus has 32.78% of samples below 12 pieces,
40.71% with 12–23 pieces and 26.51% with 24–32 pieces. These are phase proxies,
not formal opening/middlegame/endgame labels. Detailed material and original-ply
counts are in [distribution evidence](r057-main-distribution.json). Very early
openings, checks and tactical captures were filtered by the original methodology.
The fresh 8,192-row panel comes from two unused units and is more midgame-heavy.
Source-game IDs are unavailable: unit disjointness and exact alias exclusions
do not prove game/trajectory independence. This limits uncertainty and leakage claims.

The bounded capacity investigation used three configurations and four total arms:
8/4, 16/4 and 32/8 at seed 71; 16/4 also at seed 97. Each saw 1,048,576 samples
twice. The frozen 1%-of-best-loss rule selected 16/4 by cost; 8/4 missed the band
narrowly. This threshold-sensitive selection is not a strong architecture ranking.
The principal seed 20261029 was fresh, with no pilot warm start or seed selection.
The four pilots' 8,388,608 exposures are separate from the principal allocation;
total exposure across all five models is 276,824,064.

The principal run used batch 128 and rates [0.01,0.01,0.007,0.005,0.003,0.002,
0.001,0.0005]. It completed 2,097,152 optimizer updates and 1,024 immutable
262,144-exposure checkpoints. Every epoch is a verified bijection over all sample
rows; exact cursor, moments, steps, config and chained receipts are preserved.
The 8/4 pilot also explicitly resumed after its first checkpoint, without
repeating counted exposures. Main execution used 32 checkpointed invocations.

| Pass | Development MSE | Fixed training panel MSE |
|---|---:|---:|
| Initial | 0.0321507 | 0.0311980 |
| 1 | 0.0253361 | 0.0243944 |
| 2 | 0.0252382 | 0.0240877 |
| 3 | 0.0251532 | 0.0240997 |
| 4 | 0.0241823 | 0.0231890 |
| 5 | 0.0236688 | 0.0227959 |
| 6 | 0.0234637 | 0.0225365 |
| 7 | 0.0233157 | 0.0222854 |
| 8 | 0.0231604 | 0.0221535 |

No nonfinite or irrecoverable principal failure occurred. At the endpoint, 491
parameters were at the clipping boundary. Loss improved; one full run does not
characterize training-seed variability. No early stop or validation-selected epoch
was used. [Method comparison](R057-METHOD-COMPARISON.md) details the consequential
optimizer differences: CPU double touched-coordinate moments versus GPU float32
dense AdamW; 32 times as many updates; different rates, clipping and cumulative
decay. CP64 training includes a factorizer absent from its exported parameter count.

**Prediction and play.** Both panels have 8,192 positions with original BT4
targets. The fresh panel was first consumed after configuration and endpoint freeze.

| Panel | ONE MSE | CP64 MSE | ONE with relational path masked |
|---|---:|---:|---:|
| Original-validation development | 0.0231604 | 0.0170790 | 0.0289412 |
| Fresh reserved units | 0.0309569 | 0.0217784 | 0.0384317 |

Paired loss differences were 0.0060814 and 0.0091785, with descriptive iid-position
standard errors 0.0003762 and 0.0004809. Unit/game correlations make those unsuitable
as independent-sample confidence guarantees. CP64 had lower loss in every recorded
fresh material bucket. Development histories differ; these are not fully controlled
architecture comparisons. Per-position predictions and phase sums are retained.

The game screen used 32 pre-reserved roots, both colours, 20,000 whole-request
nodes including qsearch, one active game, one worker, TT16 MiB and common
flags262143/ADAPTIVE16. No clocks, ponder, book or tablebases. Four special-position
UCI probes per engine repeated deterministically before play. All 5,424 game
requests passed node accounting; 106,006,148 actual nodes were searched, with
early terminal requests explaining less than the per-request maximum.

All 64 games, JSON receipts and PGNs passed independent legal replay, with zero
caps or failures. ONE match throughput was 532,766 NPS versus CP64 661,360 NPS
(ratio 0.806). Match wall time was 268.22 seconds including overhead. The score
and both uncertainty intervals above use all 32 pairs; bootstrap used the frozen
20,000 resamples/seed20261029061. Related opening families limit independence.
The reusable runner's historical generic scope string says “reused openings”;
R057 actually used the newly reserved roots pinned by r057-openings-selection.json.

The 512-game independent panel remains unused because the complete screen failed
the predeclared continuation rule. This is a completed negative screen, not missing
evidence being represented as confirmation. Neither noninferiority nor universal
inferiority is established. Production CP64 additionally uses NOMINAL_FUTILITY
and VERIFIED_NULL (flags3407871); this production composition remains untested here.
Practical equal-time strength is unverified and no clock test is authorized.

**Resources, reproducibility and stop.** Main supervised wall time summed to
2,289.40 seconds (38.16 minutes); training/checkpoint segment time was 2,233.12
seconds. There was one logical CPU, BelowNormal priority, 512 MiB heap and the
mapped ~1 GiB corpus, with no GPU training. Across 445 main-run resource samples,
minimum free RAM was 16.90 GiB and maximum observed system CPU was 61.46%
(below the active stop threshold). Game samples had at least 17.11 GiB free and
at most 46.67% total CPU. These are sampled system values, not proof of zero impact
or measured process peak RSS. No resource gate occurred during this RESUME root.

Main state files occupy 773,918,720 bytes, including optimizer state; the R057 raw
tree is approximately 3.03 GB including indices, runtime snapshots, failures and
all models. Original CP64 input data was reused without a second 1 GiB corpus copy.
Training source/config freeze is commit19159d93, final audit code commit91e3d25c;
runtime-training-v2 and runtime-evaluation-v2 have exact bindings. Final model SHA256
is `7f081bab949139d768147ea736d68d26b16a8f33593823e209e34e20cc63c1c2`.
Its path, receipt and optimizer cursor are in r057-main-progress.json. Frozen CP64
SHA256 is `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.

Raw artifacts are local-only and ignored, with tracked manifests and hashes.
A fresh checkout must restore matching raw inputs before reproduction; never
silently regenerate or overwrite completed evidence. Full application/GUI tests,
GPU qualification, production packaging and clock tournaments were not run.
CP64, production defaults and application build79 are unchanged by this RESUME.
Final process-release and evidence checks are recorded in r057-completion-evidence.json.

Human actions required after this prompt: **None**. Retaining this pause needs
no action. New experimental work requires a new explicit bounded research decision.
The existing RESUME instruction still starts at STATE.md; because R057 is complete,
bare RESUME does not authorize another run, confirmation match or old research queue.
