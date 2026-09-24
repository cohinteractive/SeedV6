# BRN campaign-target capture replay diagnostic — 2026-09-24

**Classification: D — AUXILIARY POSITIVE on this bounded frozen replay. Control gate: PASS.** Restoring the original campaign objective, authentic persisted data, sample weighting and saved optimizer eliminates the prior large base-only qsearch regression. Relative to that sound λ=0 control, held-out-selected **λ=2** reduces raw Kiwipete qnodes **205,023→111,437 (45.65%)** and Fianchetto **14,625→4,460 (69.50%)**, with stand-pat rates **44.69%→54.54%** and **54.65%→75.40%**. Both complete depth 4 and preserve their root moves. The gain exists before calibration and survives identical frozen calibration.

On the stricter 239-pair endpoint-disjoint holdout, median absolute campaign-delta error improves **0.197517→0.184137 (6.77%)**, p95 **0.770752→0.559759 (27.37%)**, and error >0.5 **16.74%→7.53%**. Native 255-pair median improves **0.213629→0.181669 (14.96%)**. Ordinary mixed-target held-out loss improves **0.082273→0.079031 (3.94%)**. Direction agreement does not improve; the median gain is modest and tails carry much of the benefit.

**Recommendation:** retain this as positive isolated evidence. It supports considering an explicitly opt-in controlled integration experiment, but does **not** yet justify changing the active production trainer/defaults or starting a long campaign. The smallest next experiment is one fresh, bounded handcrafted generation-sized frozen replay with preregistered λ=0 versus λ=2, the same targets/optimizer conventions, a non-regressing control gate and independent held-out positions. No production integration was performed here.

## Scope, governance and accepted-report relationship

- Date: 2026-09-24 UTC and New Zealand local date. Inspected HEAD: **339b0d763e12141b24918190fd5b9adc00450c5c** in `C:\projects\seed\java\seedv6`.
- Read the accepted [stand-pat report](BRN_QSEARCH_STAND_PAT_DIAGNOSTIC_2026-09-24.md), commit `8ea5cb99b32330c1b85ef8a918dfb24ae5336109`; [calibration report](BRN_QSEARCH_CALIBRATION_DIAGNOSTIC_2026-09-24.md), commit `b1d224f3a51e5bb720a15e6339db6023291b6ff2`; and [capture-consistency report](BRN_CAPTURE_CONSISTENCY_TRAINING_DIAGNOSTIC_2026-09-24.md), commit `339b0d763e12141b24918190fd5b9adc00450c5c`. This fourth unit resolves that last report's explicitly confounded control; it does not replace its measured teacher-only results.
- No on-disk ancestor/repository `AGENTS.md` was found. User-supplied governance and the scoped search contract apply. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent; neither was created, and no journal/finalizer was activated. First successful machine-clock observation was `2026-09-24T02:33:01Z`; the preceding unsupported PowerShell `Get-Date -AsUTC` attempt failed. No earlier time is inferred.
- Initial index empty; 41 modified tracked files and 46 individually enumerated untracked files were inherited. All **541 initially present tracked/non-ignored files** were SHA-256 inventoried and preserved. Their GUI/training/document/build changes remain unattributed and outside this work.
- Disposable environment: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-campaign-replay-20260924`. A `git archive HEAD` copy excluded inherited edits. Experimental source, frozen corpora, model/training payloads, classes, test outputs and raw measurements stayed there. The authoritative worktree receives only this report.
- Only two archived existing files were eventually changed: isolated `Brn2Trainer.java` auxiliary support, and the accepted two-return BRN calibration seam in `SearchEvaluation.java`. Raw searches for **all four models** ran through classes compiled from the pristine archive **before either change**. No qsearch, NNUE, ordering, pruning, reduction, extension, search-depth or checkpoint-schema change occurred.

## Authenticated starting state and lineage

Starting checkpoint: `E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c`.

Canonical BRN-2/schema 2, width 32, 1,645,665 binary64 parameters; generation **134**, step **218,158**, training depth **4**. Its `network.brn2` bytes exactly equal the model snapshot decoded from `training.state`. Every arm restores this complete state, including both Adam moment arrays and optimizer configuration. No reset optimizer or learning-rate replacement is used.

The parent is `g000133-s000216573-903ffd2566c8084b438e51f8be49523cdf4ebf74fad647cb18e2606f87116d0d`, step **216,573**. Direct checksummed manifest/frame reads verify parent linkage, sizes and payload hashes. The generation-134 plan is named after this parent under `t2/bootstrap`, SHA-256 **c0a61a2583acd182478d4afe4ceaa521d05170f67ce50287fadb5ed159973a78**. Its data file SHA-256 is **c1a9f8be677d2755f126630f94a46d3dabc988e6cbcc94476a3084d65526bdd9**, with an exact embedded plan-hash match.

The plan explicitly says **HANDCRAFTED** position generation, **NNUE_BLENDED / teacherWeight=0.5**, separate teacher store `E:\SeedV6-Networks\NNUE\training`, and exact teacher checkpoint `g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6`. This is the same NNUE g74 payload as the accepted diagnostics. Source and teacher are distinct roles: handcrafted search generates the games; NNUE supplies per-position teacher values. Root `training-source.bin`, `brn-supervision.bin`, `brn-teacher.bin` and run-seed records agree. Generation plans 128–134 also carry this source, weight and teacher pin; no earlier-lineage objective uniformity is inferred.

The archived HEAD high-level reader understands plan versions only through v3; this store has v4 records. Current inherited source defines v4's appended validation-method field. Read-only direct parsing verified magic/version/kind, payload length, SHA-256 and complete payload consumption, without changing any schema or invoking store recovery/writers. Generation 134's v4 validation method is **GAME_PAIRS**; this does not change its frozen training targets. Full live-store/promotion/operational acceptance remains unclaimed.

**Stronger than a lineage inference:** restoring exact g133 state and calling the unmodified production `Brn2SelfPlayTraining.trainSamples` on the saved generation-134 training partition, target selector and shuffle seed reproduces **every byte of g134 `training.state`**. All weights, first/second moments, config and step match. Initial loss 0.078351506 becomes 0.030122899 in exactly 1,585 updates. This establishes the relevant historical target/data/optimizer path without repairing the older plan reader.

| Payload | SHA-256 |
| --- | --- |
| g134.brn2 | 21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924 |
| g134.state | ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9 |
| g133.brn2 | 9bb670424e548225bb305f7c03b2e0aa4d6f2d1577a6e448a2848f592d48318d |
| g133.state | a130d4bd689bfe24cc9cb5dcaef62c42fa0b270b4ce96b68ef81ea3f96d86a35 |
| teacher.nnue | 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9 |
| lambda0.brn2 | 8c92918156db4f63e6503344a4f1aacbf3b7a734f7f5b320dbfa2aba9bb68d50 |
| lambda0.state | 7a1dd16b9cc5246de199d4114d015a3354210ab9092beeef081396940572ab68 |
| lambda0.5.brn2 | 689a7adc0aae0b118abf639b158041733ec48ecb84275e4e1231ab4bec396030 |
| lambda0.5.state | f53bfc0ab5cca60e763b2f79b8ed9a72eb643be5737dd1d89cde78051de40818 |
| lambda2.brn2 | 573845085b794b8f4a8e6d04289671a6b01dd20a2ceff0ad38ecc52e19586f47 |
| lambda2.state | cd69f31d6df1cf5e43345559927fff7b44063c93a2182455921a03b0df5ffb05 |

Temporary names `g134.state`, `g133.state` and `teacher.nnue` are byte copies of the respective pinned `training.state` and NNUE `network.nnue`. Experimental payloads use unchanged canonical codecs; none was published to a production store or reference.

## Exact campaign base objective

Relevant production sources under `app/src/main/java/com/ohinteractive/seedv6/`: `training/service/BrnSupervision.java`; `training/selfplay/{GameResult,GameTrajectory,TrajectorySampler,BootstrapPartition,Brn2SelfPlayTraining}.java`; `core/brn2/Brn2Trainer.java`; and `training/service/TrainerService.java`. Current inherited changes to supervision affect allowed source/campaign handling, not the target arithmetic. The archived target/train code reproduces g134 exactly as above.

For sampled board `s`, in its **side-to-move (STM)** orientation:

```text
w(s) ∈ {-1, 0, +1} = actual completed-game loss, draw, win for that STM
n(s) = pinned NNUE boundedValue() = tanh(raw NNUE output), independently evaluated on s
t(s) = (1 - teacherWeight) * w(s) + teacherWeight * n(s)
     = 0.5*w(s) + 0.5*n(s)
y(s) = differentiable BRN tanh output
L_base(s) = 0.5 * (y(s) - t(s))²
```

This is **one blended scalar target followed by one loss**, not a WDL classification/vector head or two separately optimized losses. No search-score rounding, centipawn conversion, sigmoid reinterpretation or new clipping supplies the target. Both inputs and their convex blend are bounded in [-1,+1]. WDL comes from the completed game's fixed White result, sign-flipped for Black STM; draws remain zero. NNUE evaluates the exact sampled board in its own STM orientation.

Descriptive component losses below are `L_WDL=0.5*(y-w)²` and `L_NNUE=0.5*(y-n)²`. At weight 0.5, `L_base = 0.5*L_WDL + 0.5*L_NNUE - 0.125*(w-n)²`. A weighted sum of those two squared losses would have the same prediction gradient on fixed targets but a different reported loss by this data-dependent constant. The actual code uses the scalar blend.

Sampling is result-independent, evenly spaced over pre-move positions of **completed** trajectories: index `floor(i*(positions-1)/(count-1))`, including both ends; at most 32 per game. Incomplete/administratively capped games do not receive invented outcomes. Samples have equal weight; there is no class balancing, recency weighting or special capture weighting in the base loss. Short games supply fewer samples. Repeated board observations and different outcomes for the same opening board are retained.

Production training freezes all selected targets before updates, then performs one seeded Fisher–Yates shuffled pass: **epochs=1, minibatch=1**. Adam learning rate **0.001**, β1 **0.9**, β2 **0.999**, ε **1e-8**, restored from the saved state; no schedule change. The derivative through the tanh head is `(y-t)*(1-y²)`, propagated through each ReLU mask. Shared sparse rows aggregate once per example; untouched rows/moments freeze, while bias correction uses the global step. There is no added weight decay or gradient clipping.

The previous experiment was materially different: teacher-only `t=n`, random legal walks from two artificial roots, capture-selected duplicated endpoints, ordinary updates on both parent and synthetic child, and 9,768 updates. This experiment preserves actual WDL anchors and the original sample distribution/base weighting, supplies no synthetic child base example, and uses one native 1,585-update batch. These simultaneous corrections mean the improved control cannot be attributed to the target mixture alone.

## Frozen replay construction and limitations

No new games were generated. Reuse the **exact saved generation-134 batch**, once more from g134, rather than invent a historical sample reconstruction. This is a repeated historical batch, **not a claim to reproduce unseen generation 135**.

Original durable settings:

```text
SelfPlayConfig[games=64, depth=4, threads=6, seed=-2860970018538427249, minimumOpeningPlies=0, maximumOpeningPlies=8, maximumSamplesPerGame=32, maximumPlies=1024, scoreMapping=NnueScoreMapping[scale=32511.0], nodesPerMove=-1, millisPerMove=-1]|Config[epochs=1, minibatchSize=1, shuffle=true, shuffleSeed=8583368780869221086]|-5751002697157378807|rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1|brn-run-seeds-v1:1:1
```

Thus: 64 handcrafted-search games, depth 4, six generation threads, random opening length 0–8 from the standard start, at most 32 samples/game, 1,024-ply game cap, no per-move node/time cap. All 64 completed: 28 White wins, 6 draws, 30 Black wins; zero aborted/capped. 7,674 played/pre-move positions, 2,001 samples; game lengths 6–290 plies. The production whole-game split reserves 13 games / 416 samples and trains on 51 games / **1,585 samples**. Train WDL counts (-1,0,+1): **621/192/772**; held: **187/0/229**. Train/held teacher-WDL sign agreement is **51.61%/64.66%**, so the mixture is substantively different from teacher-only imitation.

The original partition, row order, boards and WDL labels are unchanged. Original shuffle seed **8583368780869221086** is reused; split seed **-5751002697157378807** identifies the already persisted partition. Base targets were independently recomputed using the pinned teacher and frozen in `targets.bin`/JSONL **before control training**. Every arm sees the same examples, order, 1,585 optimizer updates, initial state and online minibatch behavior. Final step is **219,743**. One native generation-sized batch is the evidence-supported smaller unit, rather than six repeated passes to imitate the previous synthetic budget.

There are 1,432 distinct native training board keys and 403 native held keys. Whole-game separation does not imply board separation: **35 shared keys / 47 held observations** leave **369 disjoint ordinary held observations**. All ordinary tables show both native and disjoint results. Adding auxiliary training-child keys excludes no additional ordinary held rows. A broader read-only scan of 134 persisted sample files finds 144,051 distinct training keys and only **211/416** current held observations absent from all those training partitions. This is a scan of persisted records, not a claim that every earlier training operation was separately audited. No claim of completely unseen pretraining holdout is made.

None of Kiwipete, Fianchetto, Exchanges, Tactical queen or En-passant root keys appears in base examples or auxiliary children. The **Opening start occurs naturally 51 times in training and 13 in held-out data**: removing it would alter the exact historical replay. Opening is a familiar control, not an independent generalization fixture. No qnode result selected, edited or filtered the corpus. Absence of the other five roots is checked; complete disjointness from every possible search descendant is not claimed.

After the control gate passed, freeze one uniformly seeded legal nonterminal capture child per base row that has an eligible capture, with `SplittableRandom(2026092404)`, train rows then held rows in original order, coordinate-sorted legal captures. No evaluator score selects the move. Rows without captures still receive their original base update. Ten terminal/rule-drawn candidate children are excluded; there are **960 train pairs / 255 held pairs**. The 239-pair stricter holdout excludes any parent or child whose key occurs in any training base or selected auxiliary endpoint. One pair per row preserves base sample weights rather than multiplying them by capture count. It does not cover whole capture trajectories or balance capture types. Repeated boards remain correlated.

## Lambda=0 control and gate

The first and only control arm calls the **unmodified production trainer and target selector**. An independent manual pass over frozen targets/order yields byte-identical complete final training state. After auxiliary implementation, its λ=0 branch was also checked over all 1,585 updates against that already saved state, plus a 128-update exact loss/state comparison. It dispatches directly to ordinary `train` before any child evaluation; auxiliary gradient and auxiliary work are exactly zero.

| Fixture | Untouched g134 qnodes / SP% | Campaign λ=0 qnodes / SP% | Qnode change | Depth / cap |
| --- | --- | --- | --- | --- |
| Kiwipete | 209,297 / 44.71 | 205,023 / 44.69 | -2.04% | both 4, no cap |
| Fianchetto | 13,007 / 53.89 | 14,625 / 54.65 | +12.44% | both 4, no cap |
| Opening | 220 / 74.08 | 214 / 73.26 | -2.73% | both 4, no cap |

**Gate PASS**, recorded before nonzero training. All six fixtures finish depth 4; no >50% qnode regression on two fixtures, primary node-cap failure or severe fixture-wide instability occurs. Kiwipete/Fianchetto/Opening best moves remain unchanged. En-passant changes from the drawn capture to a quiet king move; that deserves disclosure but does not by itself invalidate the control. Native held loss rises **3.98%**, disjoint held **3.98%**, while train loss falls **0.030123→0.018327**. This is modest repeated-batch overfitting, not contradictory target behavior. It is much smaller than the previous teacher-only control's Kiwipete **672,779** and Fianchetto **38,689** qnodes.

Original target formulation, exact sample path and optimizer continuity are now established. No evidence here requires blaming continuation from g134 itself. The previous regression remains jointly confounded by distribution, objective, endpoint weighting and update budget; this unit does not isolate their individual causal shares.

## Campaign-consistent auxiliary target and loss

Let the real parent WDL anchor be `w`, parent NNUE `n_p`, and child NNUE `n_c`, each NNUE output native STM. The child is a hypothetical local relation, with **no independent completed-game result**. Transporting the same anchor into child STM gives `-w` only for deriving the auxiliary relation:

```text
t_parent = 0.5*w + 0.5*n_p
t_child_shared_anchor = -0.5*w + 0.5*n_c   (derivation only, never a base example)
target_delta in parent orientation
    = -t_child_shared_anchor - t_parent
    = 0.5*(-n_c - n_p)
predicted_delta = -y_c - y_p
e = predicted_delta - target_delta
Huber_0.25(e) = 0.5*e²                    if |e| <= 0.25
                0.25*(|e|-0.125)         otherwise
L_step = unchanged L_base(parent) + lambda * I(has_pair) * Huber_0.25(e)
```

The auxiliary target therefore uses **half** the NNUE common-perspective delta, not the full teacher delta and not zero. It lies in [-1,+1]; predicted deltas can span [-2,+2]. The implementation passes this delta directly, not a synthetic WDL-labeled `Sample`. It never adds a child ordinary update. The WDL anchor is an explicitly shared local construction, not a claim that the hypothetical move's actual terminal result is known.

With native STM outputs, both endpoint auxiliary derivatives are `-lambda*clip(e,-0.25,0.25)`. In a common fixed orientation they have opposite signs. Both Jacobians use the same pre-update weights and their own ReLU masks, aggregate shared rows, then undergo one Adam update together with the unchanged parent base derivative. Differentiation uses continuous tanh output, never integer search scores.

Initial training base loss **0.030122899**; pair-conditional auxiliary Huber **0.031042281**, or **0.018801634 per base step** after multiplying by 960/1,585. Thus initial λ·aux/base is **31.21% (λ=0.5)** and **124.83% (λ=2)**, versus 16.46%/65.85% in the previous experiment. On paired parents the base-output derivative RMS is **0.233285**, unit-λ auxiliary derivative RMS per endpoint **0.182648**; weighted auxiliary RMS is **0.091324 / 0.365296**. These are output-level derivatives, not a full parameter-gradient norm. The requested coefficients are numerically meaningful and were not tuned or expanded. The magnitudes were recorded before nonzero training.

| Lambda | Updates | Final step | Online base | Online auxiliary per base step | Loop seconds |
| --- | --- | --- | --- | --- | --- |
| 0.0 | 1585 | 219743 | 0.028262 | 0.000000 | 0.517 |
| 0.5 | 1585 | 219743 | 0.028315 | 0.018843 | 0.768 |
| 2.0 | 1585 | 219743 | 0.032272 | 0.016508 | 0.769 |

The λ=0 auxiliary diagnostic replay above is an equality check of the already completed control, not a second selectable control arm. Reported zero online auxiliary loss there reflects its early zero-work branch; offline pair error is measured separately. Nonzero base and auxiliary online averages are pre-update values. All arms run one fixed pass, with no early stopping or model selection within an arm.

## Held-out evaluation before nonzero qsearch testing

Selection was frozen at **2026-09-24T02:43:07.723202Z**, before either nonzero search: lowest endpoint-disjoint held-out median delta error, subject to ordinary native held loss <=110% of λ=0. **λ=2** wins on both native and disjoint median definitions; both nonzero arms meet the base-loss constraint. There was no qnode-driven λ selection.

| Model | Train base | Native held base (416) | Held WDL loss | Held NNUE loss | Held MAE | Disjoint held base (369) |
| --- | --- | --- | --- | --- | --- | --- |
| g134 | 0.030123 | 0.079125 | 0.306699 | 0.093967 | 0.300151 | 0.084314 |
| lambda0 | 0.018327 | 0.082273 | 0.309135 | 0.097827 | 0.301501 | 0.087668 |
| lambda0.5 | 0.019175 | 0.080772 | 0.311585 | 0.092375 | 0.304182 | 0.086009 |
| lambda2 | 0.026018 | 0.079031 | 0.315936 | 0.084542 | 0.319203 | 0.083795 |

The selected arm preserves the actual ordinary objective: native held loss **-3.94%** and disjoint held **-4.42%** versus λ=0; it is also close to untouched g134 (native **0.079031 vs 0.079125**, disjoint **0.083795 vs 0.084314**). Components expose a tradeoff: selected WDL-only loss increases **2.20%** versus λ=0, while NNUE-only loss falls **13.58%**. Equal preservation of each component is not asserted. On the 211 held observations absent from all scanned persisted training keys, base losses are **0.118859 / 0.120584 / 0.117986 / 0.111760** for g134/0/0.5/2, respectively. This additional subset was descriptive, not model selection.

| Model | Native error p50 (255) | Disjoint error p50 / p90 / p95 (239) | Disjoint Huber | Sign agreement % | Error >0.5 % |
| --- | --- | --- | --- | --- | --- |
| g134 | 0.177458 | 0.173641 / 0.575202 / 0.692880 | 0.036764 | 55.65 | 12.97 |
| lambda0 | 0.213629 | 0.197517 / 0.617386 / 0.770752 | 0.043576 | 50.21 | 16.74 |
| lambda0.5 | 0.209658 | 0.208573 / 0.562875 / 0.732627 | 0.040318 | 48.95 | 12.97 |
| lambda2 | 0.181669 | 0.184137 / 0.457731 / 0.559759 | 0.033017 | 48.12 | 7.53 |

Relative to the sound control, λ=2 improves median, p90, p95, Huber and large-error frequency. Its direction agreement remains near chance and actually decreases **50.21%→48.12%** on the disjoint set. Many target changes are small, but no post hoc sign deadband is applied. λ=0.5 improves tails and native median slightly while worsening the stricter median **0.197517→0.208573**. The stronger arm's disjoint median **0.184137 remains worse than untouched g134's 0.173641**; the advantage is relative to ordinary continuation, with substantially better tails. Positive classification does not conceal that external-reference limitation.

The disjoint target's absolute delta p50/p95 is **0.064392 / 0.278378**, signed p05/p50/p95 **-0.189834 / 0.009840 / 0.232715**. Native target absolute p50/p95 is **0.066518 / 0.267177**. These are campaign-implied targets, so their errors cannot be compared numerically with the previous full-NNUE-target errors as if the geometry were unchanged.

| Model | Disjoint raw abs delta p50 / p95 | Disjoint signed delta p05 / p50 / p95 |
| --- | --- | --- |
| g134 | 0.187587 / 0.723140 | -0.293844 / 0.066238 / 0.723140 |
| lambda0 | 0.212886 / 0.770171 | -0.379118 / 0.022048 / 0.770171 |
| lambda0.5 | 0.199147 / 0.718075 | -0.359969 / 0.017228 / 0.716028 |
| lambda2 | 0.183809 / 0.550812 | -0.358955 / 0.000349 / 0.545295 |

Smaller raw changes are not the success criterion; agreement with independently varying campaign-implied changes is. Selected output still has excessive typical/tail changes relative to the targets. The improvement is not purely a constant shift: disjoint error SD falls **0.355609→0.289677**, and mean-centered median absolute error falls **0.224708→0.191325**, although error mean also changes **0.057404→0.018355**.

| Model | Held mean STM | Held signed p05 / p50 / p95 | Held absolute p50 / p95 |
| --- | --- | --- | --- |
| g134 | 0.044416 | -0.733108 / 0.010625 / 0.812155 | 0.388637 / 0.844067 |
| lambda0 | 0.061477 | -0.732917 / 0.033418 / 0.829270 | 0.403968 / 0.865892 |
| lambda0.5 | 0.065719 | -0.722488 / 0.026763 / 0.830567 | 0.397562 / 0.852212 |
| lambda2 | 0.065276 | -0.654010 / 0.043061 / 0.770687 | 0.350029 / 0.806425 |

## Real qsearch benchmarks

Exact six fixtures and FENs from the accepted reports: Kiwipete, Exchanges, Tactical queen, En-passant, Fianchetto and Opening. Requested **depth 4**, one worker, ordinary iterative deepening, fresh **262,144-entry TT**, singleton root history, cumulative **1,000,000 entered-node** and **60-second** caps. Neural mate-distance-only selectivity and qsearch soft limit 16/check-evasion rules are unchanged. Existing `QsearchDecisionTrace` observes NNUE shadows at stride 1 and detached samples every 101 qpositions (limit 12,000, never hit); no new qsearch instrumentation.

Qnodes count entered qsearch children, excluding main leaves reused as qsearch roots. Q%=q/total. SP%=cutoffs/eligible attempts. Root score/move belong to the completed iteration. **Every row completed depth 4, with no node-cap or time-cap termination.** Timing includes tracing/JIT and is secondary. Untouched g134 and NNUE nontiming counts reproduce the accepted reports.

| Fixture | Model | Depth/status | Nodes | Qnodes | Q% | SP attempts | SP cuts | SP% | Max q-ply | Score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| middlegame-kiwipete | g134 / RAW | 4/OK | 218,790 | 209,297 | 95.66 | 180,919 | 80,896 | 44.71 | 19 | 10036 / e1g1 | 3.042 |
| middlegame-kiwipete | NNUE / RAW | 4/OK | 41,435 | 32,300 | 77.95 | 36,191 | 29,281 | 80.91 | 17 | 20697 / e1g1 | 0.303 |
| middlegame-kiwipete | lambda0 / RAW | 4/OK | 213,857 | 205,023 | 95.87 | 173,952 | 77,738 | 44.69 | 20 | 12495 / e1g1 | 2.696 |
| middlegame-kiwipete | lambda0.5 / RAW | 4/OK | 177,891 | 169,053 | 95.03 | 146,533 | 70,601 | 48.18 | 19 | 10615 / e1g1 | 2.340 |
| middlegame-kiwipete | lambda2 / RAW | 4/OK | 120,134 | 111,437 | 92.76 | 99,547 | 54,288 | 54.54 | 19 | 9878 / e1g1 | 1.825 |
| qsearch-exchanges | g134 / RAW | 4/OK | 2,019 | 749 | 37.10 | 1,347 | 925 | 68.67 | 6 | 11541 / e4d5 | 0.014 |
| qsearch-exchanges | NNUE / RAW | 4/OK | 3,197 | 1,419 | 44.39 | 2,362 | 1,441 | 61.01 | 5 | 21844 / e4d5 | 0.014 |
| qsearch-exchanges | lambda0 / RAW | 4/OK | 2,011 | 741 | 36.85 | 1,339 | 934 | 69.75 | 6 | 12073 / e4d5 | 0.014 |
| qsearch-exchanges | lambda0.5 / RAW | 4/OK | 2,000 | 730 | 36.50 | 1,331 | 935 | 70.25 | 6 | 11575 / e4d5 | 0.017 |
| qsearch-exchanges | lambda2 / RAW | 4/OK | 1,950 | 707 | 36.26 | 1,289 | 908 | 70.44 | 5 | 10229 / e4d5 | 0.024 |
| tactical-queen | g134 / RAW | 4/OK | 189 | 8 | 4.23 | 115 | 72 | 62.61 | 1 | 6397 / e4d5 | 0.003 |
| tactical-queen | NNUE / RAW | 4/OK | 229 | 10 | 4.37 | 154 | 109 | 70.78 | 1 | 7057 / e4d5 | 0.002 |
| tactical-queen | lambda0 / RAW | 4/OK | 189 | 8 | 4.23 | 115 | 72 | 62.61 | 1 | 7553 / e4d5 | 0.004 |
| tactical-queen | lambda0.5 / RAW | 4/OK | 161 | 8 | 4.97 | 101 | 70 | 69.31 | 1 | 7580 / e4d5 | 0.003 |
| tactical-queen | lambda2 / RAW | 4/OK | 161 | 8 | 4.97 | 101 | 70 | 69.31 | 1 | 6426 / e4d5 | 0.003 |
| en-passant | g134 / RAW | 4/OK | 358 | 18 | 5.03 | 235 | 151 | 64.26 | 1 | 0 / e5d6 | 0.005 |
| en-passant | NNUE / RAW | 4/OK | 638 | 40 | 6.27 | 427 | 264 | 61.83 | 2 | 0 / e5d6 | 0.004 |
| en-passant | lambda0 / RAW | 4/OK | 569 | 18 | 3.16 | 392 | 302 | 77.04 | 1 | 1451 / e1e2 | 0.007 |
| en-passant | lambda0.5 / RAW | 4/OK | 558 | 17 | 3.05 | 385 | 296 | 76.88 | 1 | 796 / e1e2 | 0.007 |
| en-passant | lambda2 / RAW | 4/OK | 355 | 20 | 5.63 | 230 | 143 | 62.17 | 1 | 0 / e5d6 | 0.004 |
| quiet-fianchetto | g134 / RAW | 4/OK | 18,327 | 13,007 | 70.97 | 16,509 | 8,896 | 53.89 | 17 | -18683 / f3e5 | 0.269 |
| quiet-fianchetto | NNUE / RAW | 4/OK | 8,793 | 3,928 | 44.67 | 7,437 | 5,744 | 77.24 | 9 | -22502 / g2h3 | 0.045 |
| quiet-fianchetto | lambda0 / RAW | 4/OK | 20,019 | 14,625 | 73.06 | 17,695 | 9,671 | 54.65 | 17 | -18910 / f3e5 | 0.297 |
| quiet-fianchetto | lambda0.5 / RAW | 4/OK | 9,838 | 4,877 | 49.57 | 8,503 | 6,000 | 70.56 | 14 | -16594 / f3e5 | 0.168 |
| quiet-fianchetto | lambda2 / RAW | 4/OK | 9,576 | 4,460 | 46.57 | 8,141 | 6,138 | 75.40 | 14 | -14155 / f3e5 | 0.158 |
| opening-start | g134 / RAW | 4/OK | 3,795 | 220 | 5.80 | 2,913 | 2,158 | 74.08 | 4 | -872 / b1c3 | 0.053 |
| opening-start | NNUE / RAW | 4/OK | 2,343 | 126 | 5.38 | 1,684 | 1,136 | 67.46 | 2 | -887 / h2h3 | 0.009 |
| opening-start | lambda0 / RAW | 4/OK | 3,947 | 214 | 5.42 | 2,999 | 2,197 | 73.26 | 4 | 130 / b1c3 | 0.049 |
| opening-start | lambda0.5 / RAW | 4/OK | 3,919 | 227 | 5.79 | 2,980 | 2,190 | 73.49 | 4 | -1632 / b1c3 | 0.052 |
| opening-start | lambda2 / RAW | 4/OK | 3,293 | 192 | 5.83 | 2,495 | 1,871 | 74.99 | 4 | -3420 / b1c3 | 0.044 |

Against λ=0, selected λ=2 improves **two independent primary fixtures**, Kiwipete **45.65% fewer qnodes**, Fianchetto **69.50% fewer**; stand-pat improves by **9.85 / 20.74 percentage points**. Opening improves **10.28%** but is a training-familiar control. Exchanges improves **4.59%**, queen qnodes stay 8, and En-passant worsens **18→20**, a tiny absolute change. λ=0.5 independently improves Kiwipete **17.54%** and Fianchetto **66.65%**, but Opening grows **214→227**. Success is not inferred from a single fixture or cutoff percentage alone.

Raw selected best moves remain **e1g1 / f3e5 / b1c3** on Kiwipete/Fianchetto/Opening for every arm. Relative to control, selected scores change **12495→9878 / -18910→-14155 / 130→-3420**. Both obvious queen captures remain `e4d5`. En-passant changes control `e1e2`/1451 back to untouched g134's `e5d6`/0. Thus 5/6 raw root choices match control, with substantial score changes but no widespread move instability. These are native search units, not centipawns; no oracle proves optimal root choices or playing strength.

## Frozen calibration control

After raw results, apply precisely the accepted g134 **AFFINE_SAFE_MEAN** coefficients to λ=0 and held-out-selected λ=2: **a=0.9681958511436085, b=1033.984683470143**, `round(a*mapped_integer_score+b)`. No refit, qnode tuning or model-specific coefficients. The two-return diagnostic seam changes only BRN static mapping, preserves the normal band [-32511,+32511] for all raw scores and leaves terminal/mate returns untouched. All 65,023 integer inputs were checked; no clipping or range failure. An initial transcription differed only in final decimal bits; exhaustive rounding equality over the complete band was established, and final tables use reruns with the exact accepted decimals.

| Fixture | Model | Depth/status | Nodes | Qnodes | Q% | SP attempts | SP cuts | SP% | Max q-ply | Score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| middlegame-kiwipete | lambda0 / CAL | 4/OK | 123,510 | 114,585 | 92.77 | 102,355 | 52,536 | 51.33 | 19 | 13132 / e1g1 | 2.003 |
| middlegame-kiwipete | lambda2 / CAL | 4/OK | 94,189 | 84,765 | 89.99 | 78,465 | 47,450 | 60.47 | 19 | 10598 / e1g1 | 1.579 |
| qsearch-exchanges | lambda0 / CAL | 4/OK | 1,935 | 681 | 35.19 | 1,275 | 919 | 72.08 | 5 | 11470 / e4d5 | 0.017 |
| qsearch-exchanges | lambda2 / CAL | 4/OK | 1,952 | 693 | 35.50 | 1,294 | 940 | 72.64 | 5 | 9362 / e4d5 | 0.022 |
| tactical-queen | lambda0 / CAL | 4/OK | 189 | 8 | 4.23 | 115 | 72 | 62.61 | 1 | 8347 / e4d5 | 0.003 |
| tactical-queen | lambda2 / CAL | 4/OK | 161 | 8 | 4.97 | 101 | 70 | 69.31 | 1 | 7256 / e4d5 | 0.003 |
| en-passant | lambda0 / CAL | 4/OK | 739 | 42 | 5.68 | 522 | 414 | 79.31 | 1 | 2439 / e1e2 | 0.010 |
| en-passant | lambda2 / CAL | 4/OK | 646 | 19 | 2.94 | 450 | 358 | 79.56 | 1 | 408 / e1e2 | 0.006 |
| quiet-fianchetto | lambda0 / CAL | 4/OK | 13,380 | 7,579 | 56.64 | 11,861 | 7,897 | 66.58 | 16 | -18151 / f3e5 | 0.233 |
| quiet-fianchetto | lambda2 / CAL | 4/OK | 9,254 | 3,843 | 41.53 | 7,840 | 6,129 | 78.18 | 15 | -13873 / f3e5 | 0.153 |
| opening-start | lambda0 / CAL | 4/OK | 3,933 | 201 | 5.11 | 2,986 | 2,203 | 73.78 | 4 | 1160 / b1c3 | 0.063 |
| opening-start | lambda2 / CAL | 4/OK | 3,210 | 119 | 3.71 | 2,417 | 1,832 | 75.80 | 4 | -2277 / b1c3 | 0.058 |

Four-way Kiwipete: control **205,023 raw / 114,585 calibrated**, selected **111,437 raw / 84,765 calibrated**. The auxiliary gain survives calibration: **26.03% fewer calibrated qnodes** and SP **51.33%→60.47%**. Calibration itself further saves **23.93%** on selected raw Kiwipete. Fianchetto: control **14,625 / 7,579**, selected **4,460 / 3,843**; selected beats calibrated control by **49.29%**, while calibration improves selected raw by **13.83%**. Opening selected **192→119**, compared with calibrated control 201. These effects combine favorably on the primary fixtures; no claim of mathematically additive or population-independent effects is made. Calibrated Exchanges is a small exception: selected 693 versus control 681.

The main root moves remain `e1g1/f3e5/b1c3` after calibration. Selected En-passant becomes `e1e2`/408 rather than its raw drawn capture; this repeats the accepted calibration's terminal-versus-static preference sensitivity. Calibration is independently useful for search here, not an accepted production mapping.

Offline apply the same integer mapping/calibration to the identical 239 disjoint pairs, divide by 32,511, and compare with the unchanged continuous targets (never use this quantization for training):

| Model | Mapping | Disjoint median delta error | Disjoint p95 error | Disjoint base loss |
| --- | --- | --- | --- | --- |
| lambda0 | RAW | 0.197499 | 0.770743 | 0.087668 |
| lambda0 | CAL | 0.224067 | 0.727302 | 0.086850 |
| lambda2 | RAW | 0.184122 | 0.559767 | 0.083795 |
| lambda2 | CAL | 0.190692 | 0.571698 | 0.084119 |

Calibration alone worsens the control's median delta error **0.197499→0.224067** while reducing qnodes. The selected model's raw and calibrated errors remain better than the corresponding control; mean-centered error and error-SD reductions also show a learned local change beyond a global score offset. These controls do not prove that local-delta repair is the sole causal mediator of search improvement.

## Interpretation and explicit answers

| Outcome | Assessment |
| --- | --- |
| A. CONTROL FAILURE | Not observed: exact ordinary replay has an acceptable control. |
| B. AUXILIARY NEGATIVE | Not the aggregate result: actual base loss and both primary searches improve; modest WDL-only and sparse-fixture exceptions are disclosed. |
| C. AUXILIARY MIXED | Direction agreement and external g134 median remain limitations, but primary multi-fixture search transfer is consistently favorable here. |
| D. AUXILIARY POSITIVE | Supported **for this frozen, one-batch continuation** relative to its sound control, including raw/calibrated primary searches and ordinary objective retention. Not a broad strength or deployment verdict. |

| Question | Answer |
| --- | --- |
| 1. Exact original formulation? | Scalar `t=0.5*terminal_WDL_STM+0.5*NNUE_tanh_STM`, then `0.5*(BRN_tanh-t)^2`, equal sample weights, one shuffled online Adam pass. |
| 2. Prior experiment materially different? | Yes: teacher-only targets, synthetic walks, capture-endpoint base weighting and 9,768 updates instead of an authentic 1,585-sample unit. |
| 3. Can faithful λ=0 avoid the prior regression? | Yes on all six bounded fixtures; gate PASS. |
| 4. Why ordinary continuation damages qsearch? | That premise is not reproduced here. Historical exact replay excludes optimizer/target-path mismatch; prior causes cannot be separated across its simultaneous data/objective/budget changes. |
| 5. Does consistent-delta training still learn? | Yes, λ=2 improves held error/tails relative to λ=0; median gains are modest on the stricter subset and sign agreement does not improve. |
| 6. Search versus λ=0? | Selected raw Kiwipete -45.65%, Fianchetto -69.50%, both complete depth 4. |
| 7. More than one independent fixture? | Yes, Kiwipete and Fianchetto; Opening is only a familiar control. |
| 8. Stand-pat effectiveness? | Yes, +9.85/+20.74 points raw on those two fixtures, alongside reduced total qnodes. |
| 9. Ordinary WDL/NNUE loss acceptable? | Actual blended held loss improves 3.94%; WDL component +2.20%, teacher component -13.58%. Strength preservation is untested. |
| 10. Calibration independently useful? | Yes; selected qnodes fall further and auxiliary gain survives identical calibration. Not merely a calibrated score-distribution effect. |
| 11. Root choices destabilized? | Main fixture moves stable across all arms/calibration; En-passant is sensitive, and root scores change materially. |
| 12. Production integration justified? | Positive evidence supports a separately reviewed opt-in experiment, not immediate active/default integration. No integration is performed or accepted. |
| 13. Smallest next intervention? | Preregister λ=0 vs λ=2 on one fresh handcrafted generation-sized frozen replay with original targets, faithful state and disjoint held-out positions, requiring the control gate first. Reserve additional independent search fixtures and retain frozen calibration; no extra λ sweep. |

The strongest causal conclusion is narrow: changing only the auxiliary term, on this fixed authentic replay and optimizer state, improves primary qsearch relative to ordinary continuation. It does not establish that all ordinary campaign continuation will behave well, that this is the best λ, or that a longer campaign will improve strength. Reusing the last training batch, historical holdout exposure, one checkpoint/order, correlated samples, one selected capture per example, no capture trajectory context, near-chance delta signs and a modest stricter median gain remain material uncertainties. No statistical confidence interval from independent campaign repeats is available.

## Validation, skipped work and production boundary

**Performed/passed:**

1. Read-only framed plan/data/manifest hash and length verification; canonical model/training codec validation, exact g134 inference/state identity, pinned teacher identity. Exact historical **g133→g134 full training-state byte reproduction** under the original 1,585-example production pass.
2. Frozen original boards/WDL/teacher/blended-target checks; production `BrnSupervision.targets` versus literal mixture equality; correct STM orientation; one base update per original row; pair legality, occupancy decrease, `HeadlessGame` transition equality and independently repeatable teacher endpoints. Shared-anchor cancellation checked for every selected pair. No synthetic child `Sample` or ordinary WDL label is constructed.
3. Complete **1,585-update λ=0 equivalence** among production path, independent frozen-target ordinary loop and auxiliary API zero branch; additional **128-update** exact loss/full-state check. Auxiliary branch returns before child evaluation when zero.
4. **138 finite-difference parameter checks**, covering all parameter families and both parent-exclusive/child-exclusive parameters; max error **4.2133e-10**, tolerance 2e-6. Eight scalar derivative checks cover Huber regions and both STM endpoint signs. Both endpoint gradients are live for nonzero λ. The checker uses isolated single-endpoint target-1 probes only to identify active parameter rows; those probes are discarded, never corpus labels or updates to any experimental arm.
5. **39/39 focused existing tests passed**, zero skipped/aborted/failed: `Brn2CoreTest`, `Brn2CodecTest`, `Brn2TrainingTargetsTest`, `TrajectorySamplerTest`, `BootstrapPartitionTest`, `Brn2SearchIntegrationTest`, `QsearchDecisionTraceTest`, `NnueScoreMappingTest`. Fresh Java 21 compilation; JUnit Jupiter 5.10.3 / Platform 1.10.3, 13.247 seconds. An initial external harness compile hit package-private helper access; a small external reader bridge fixed it before any training. No production visibility change.
6. All inference/training output codecs round-trip normally. Every trained checkpoint searched through freshly compiled **pristine** engine classes, without requiring the experimental trainer or calibration source. All primary/correctly calibrated searches finish depth 4, no caps. **12 baseline trace/no-trace equalities** cover BRN and NNUE across six fixtures. Trace node/SP partitions and root restoration pass; **9,724** detached sample records were freshly inspected: **9,236 BRN** records have exact fresh mapped-score equality asserted; **488 NNUE** samples are recorded separately.
7. Immutable teacher serialization and original input-file hashes after training; all seven selected original plan/data/model/training/teacher inputs remain unchanged. No store writer/reference, schema or teacher-network mutation.
8. Complete disposable archive comparison confirms only the two declared isolated source changes. Qsearch, NNUE, ordering, pruning, extensions/reductions, codec and all other existing copied source files remain unchanged. All **541** initial authoritative file hashes and exact pre-report porcelain status were verified unchanged; index empty and root journal/version files still absent. Post-report preservation/diff/index verification is recorded in the completion response.

**Deliberately skipped:** full/long test suites; GUI/browser tests (no UI change); new ordinary self-play or multi-generation campaigns; promotion matches, arenas/tournaments and strength screens; full live-store recovery/acceptance; release/package/deployment checks. No arbitrary architecture, search, NNUE, GUI/default or checkpoint-schema change was made. No statistical independent-run replication was attempted; that is the next bounded question, not an implicit completion claim.

## Evidence and reproduction

Exact initial corpus and state paths/hashes above, executable source below, original seed/settings and immutable teacher pin make the experiment reproducible without relying on conversation history. Disposable files are research evidence, not production checkpoint candidates. Final report-only commit SHA is supplied in the completion response; no push/deployment is performed. Turn/report completion does not establish user acceptance or production acceptance.

Recreate a clean HEAD archive outside the authoritative worktree. Extract `CaptureDiagnostic`, `CalibrationSearch` and `CalibrationScore` from the accepted report appendices. The latter two are unchanged for RAW search; RAW classes are compiled before applying the calibration seam. Copy the exact g133/g134 payloads, pinned teacher, generation-134 `.plan` and `.data` to the temporary filenames in this report. Compile the appended `ReplayControl`/`ReplayData`, then:

```text
ReplayControl DIR prepare         # checks historical exact replay, freezes base targets
ReplayControl DIR control         # authentic ordinary λ=0 continuation
CalibrationSearch g134 ... RAW rawcheck EMPTY_KEYS
CalibrationSearch lambda0 ... RAW all EMPTY_KEYS
# Evaluate and record the gate. STOP if it fails.
python patch_auxiliary.py
CampaignCapture DIR prepare-pairs
CampaignLossChecks DIR/g134.state DIR/replay-pairs.bin
CampaignCapture DIR 0             # equality validation only
CampaignCapture DIR 0.5
CampaignCapture DIR 2
# Select from held-out results before nonzero search.
CalibrationSearch lambda0.5 ... RAW all EMPTY_KEYS
CalibrationSearch lambda2 ... RAW all EMPTY_KEYS
# Apply only the accepted two-return calibration seam and compile separate classes.
CalibrationSearch lambda0 ... CAL all EMPTY_KEYS  # exact frozen a,b JVM properties
CalibrationSearch lambda2 ... CAL all EMPTY_KEYS
```

Java package for these harnesses is `com.ohinteractive.seedv6.tools.search`, except `ReplayData` in `training.checkpoint` and `CampaignLossChecks` in `core.brn2`. Use `javac -d CLASSES -sourcepath ARCHIVE/app/src/main/java` and Java 21; training heap 1,536 MB, search 1,024 MB. `CalibrationSearch` full signature is `MODEL NNUE OUTPUT LABEL VARIANT MODE KEY_FILE`; empty key file avoids falsely asserting a new corpus/search-disjointness guarantee on authentic opening data. Existing read-only trace and separate root-overlap checks provide the declared evidence. Calibration JVM properties are `-Dbrn.diag.a=0.9681958511436085 -Dbrn.diag.b=1033.984683470143`.

**Human actions required after this prompt: None.** A fresh replay or integration proposal is a future authorized work unit, not a blocker to completion of this diagnostic.

| Temporary evidence | Bytes | SHA-256 |
| --- | --- | --- |
| experiment-plan.json | 680 | 22ad1d78e6cf23c2d3478bf7df88d4f37300c94525a848489232ad01c7b0ddab |
| lineage.json | 27669 | 72bf3495e5a6469974fca234d8bb210bf54fdb384e8e5a8a1bc1f6ee22875f20 |
| gate.json | 339 | 9d16e2c3ac854d79627e0a6af0ebceb67fb7f6b2bbdab4e047351b4f572a1830 |
| selection.json | 224 | fb279f3c67c44c6b47f39e8cb444588239ac99c1b408c4d1bfa06400d48c9157 |
| targets.bin | 144080 | 0a5c0b6ae6ec01b1174df97117d5a0d2e406c1dcea1314789508f144d9fd54a2 |
| replay-pairs.bin | 240130 | 07d0efb1ed9de945740c7f50ea6ce74db5b18691f00b7f87f106525a44d07a7b |
| prepare.jsonl | 2028 | 5f30f468be0df6d6c8e556fa5ef9efe0192570138317bb0be4885f0fd2702c7a |
| control.jsonl | 2092 | 69e556b15503d9d4ae499654ca4438fd35fcadc7d7a2da17969d7feda4547b6f |
| prepare-pairs-capture.jsonl | 7591 | 4b6fc6cd99d4954bf72452913154ae780031647e2a9f656902f2ea7d2691fb08 |
| 0-capture.jsonl | 3958 | 32f48a587afd8c6d8787605ae6c6beb216c0f556599a723debb6828170fabc83 |
| 0.5-capture.jsonl | 3975 | b70cf15c4200a9a06aaea3e254bf4280780334c6d4d5e28a8a239f704a73a080 |
| 2-capture.jsonl | 3998 | af4b0e2b78d010fb6cd7c556f8ea32337ff415eda0c003338deeccd262960ec6 |
| g134-RAW.jsonl | 931926 | d1e0c70b06f52ba92009fdadd3fdf06bc4db459ead31e5cac5c2d430fe3987c2 |
| lambda0-RAW.jsonl | 686468 | b2b01eaf2a98cf86e968758f899e8ffc70e6abb8373fb81b508621416acde539 |
| lambda0.5-RAW.jsonl | 568039 | 3cb92cf295d89fc3d1109d8a6000757d299007c462630992cf6be57ce560d902 |
| lambda2-RAW.jsonl | 430278 | 282e932bf530db8764a4558f0f9747521918d40cba0f5021cffa0ad03beeb1ca |
| lambda0-CAL-exact.jsonl | 451889 | 9069205faefcb6222998f8fc8d48379731b7f237b5de5481ee73418939e69785 |
| lambda2-CAL-exact.jsonl | 360587 | 8eb8cb6006bf6684cf53c3ce4aa240976dfe40d384f571f4c0bc5499c84cca43 |
| focused-tests.txt | 579 | 7cf7a2885555ab5e795cdc4576ebb4cc487a395fe7e79155280e44ea78b1338a |
| verification.json | 368 | d71f7149403e1525a9cb1d23a54053fdabede619803ee7a37b06f0c51da7134e |

## Appendix — exact isolated implementation and checks

These sources are report evidence only. They must be extracted outside the production tree. `patch_auxiliary.py` reuses the accepted third report's already documented two-endpoint backpropagation, changes the argument to the explicitly derived campaign delta, and moves the zero branch before child evaluation. The ordinary production `train` body is unchanged.

### inspect_store.py

```python
from pathlib import Path
import struct,hashlib,json
D=Path(__file__).parent
STORE=Path(r'E:\SeedV6-Networks\BRN\BRN-2\t2')
class Reader:
 def __init__(self,b): self.b=b;self.i=0
 def take(self,n): v=self.b[self.i:self.i+n];assert len(v)==n;self.i+=n;return v
 def fmt(self,f):return struct.unpack('>'+f,self.take(struct.calcsize('>'+f)))[0]
 def utf(self):return self.take(self.fmt('H')).decode('utf-8')
 def end(self):assert self.i==len(self.b),(self.i,len(self.b))
def frame(p):
 b=p.read_bytes();assert hashlib.sha256(b[:-32]).digest()==b[-32:];r=Reader(b[:-32]);assert r.fmt('I')==0x53364350 and r.fmt('i')==1;k=r.utf();n=r.fmt('i');payload=r.take(n);r.end();return k,Reader(payload),hashlib.sha256(b).hexdigest()
def plan(p):
 k,r,h=frame(p);v=int(k.rsplit('v',1)[1]);x={'file':str(p),'kind':k,'sha256':h,'parent':r.utf(),'incumbent':r.utf(),'generation':r.fmt('q')};x['source']=r.utf() if v>=3 else 'NNUE_BOOTSTRAP'
 for a in ['generatorStore','generatorId','generatorHash','settings']:x[a]=r.utf()
 x['splitSeed']=r.fmt('q');x['supervision']=r.utf() if v>=2 else 'WDL';x['teacherWeight']=r.fmt('d') if v>=2 else 0
 if v>=3:
  for a in ['teacherStore','teacherId','teacherHash']:x[a]=r.utf()
 if v>=4:x['validation']=r.utf()
 r.end();return x
def data(p):
 k,r,h=frame(p);assert k=='brn-bootstrap-data-v1';x={'file':str(p),'sha256':h,'planHash':r.utf(),'generationNanos':r.fmt('q')}
 for split in ['train','held']:
  rows=[]
  for _ in range(r.fmt('i')):rows.append(([r.fmt('q') for _ in range(6)],r.fmt('d')))
  x[split+'N']=len(rows);x[split+'WDL']={str(a):sum(t==a for b,t in rows) for a in [-1,0,1]};x[split+'Keys']=set(b[5] for b,t in rows)
 for split in ['train','held']:x[split+'Games']=[r.fmt('i') for _ in range(r.fmt('i'))]
 for a in ['requestedGames','completedGames','abortedGames','whiteWins','draws','blackWins','cappedGames']:x[a]=r.fmt('i')
 x['totalPlayedPlies']=r.fmt('q');x['minimumCompletedPlies']=r.fmt('i');x['maximumCompletedPlies']=r.fmt('i');x['meanCompletedPlies']=r.fmt('d');x['rawTrajectoryPositions']=r.fmt('q');x['sampledPositions']=r.fmt('i');r.end()
 x['keyOverlap']=len(x.pop('trainKeys')&x.pop('heldKeys'));return x
def manifest(p):
 k,r,h=frame(p);assert k=='manifest';x={'file':str(p),'sha256':h,'id':r.utf(),'schema':r.utf(),'schemaVersion':r.fmt('i'),'generation':r.fmt('q'),'step':r.fmt('q'),'depth':r.fmt('i'),'parent':r.utf()}
 for a in ['network','training']:
  f=r.utf();n=r.fmt('q');hash_=r.utf();fp=p.parent/f;assert fp.stat().st_size==n and hashlib.sha256(fp.read_bytes()).hexdigest()==hash_;x[a]={'path':str(fp),'bytes':n,'sha256':hash_}
 r.end();return x
if __name__=='__main__':
 plans=[]
 for p in sorted((STORE/'bootstrap').glob('*.plan')):
  x=plan(p)
  if x['generation']>=128:
   q=p.with_suffix('.data');x['data']=data(q) if q.exists() else None
   if x['data']:assert x['data']['planHash']==x['sha256']
   plans.append(x)
 manifests=[manifest(p) for p in sorted((STORE/'checkpoints').glob('g00013[34]*/manifest.bin'))]
 result={'plans':plans,'manifests':manifests};(D/'lineage.json').write_text(json.dumps(result,indent=2))
 print(json.dumps(result,indent=2))
 for p in STORE.glob('*.bin'):
  k,r,h=frame(p);print(p.name,k,h,repr(r.b))
```

### ReplayData.java

```java
package com.ohinteractive.seedv6.training.checkpoint;
public class ReplayData {
 public static BootstrapData load(java.nio.file.Path path)throws java.io.IOException{return BootstrapData.read(path);}
}
```

### ReplayControl.java

```java
package com.ohinteractive.seedv6.tools.search;
import com.ohinteractive.seedv6.training.checkpoint.*;
import java.nio.file.*;import java.io.*;import java.util.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.service.BrnSupervision;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class ReplayControl {
 static final long SEED=8583368780869221086L;
 static final BrnSupervision SUP=BrnSupervision.blended(.5);
 static final SelfPlayTraining.Config CONFIG=new SelfPlayTraining.Config(1,1,true,SEED);
 static NnueEvaluator teacher;static Set<Long> trainKeys=new HashSet<>();
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static double quant(double[] x,double p){x=x.clone();Arrays.sort(x);double j=(x.length-1)*p;int i=(int)j;return x[i]+(x[Math.min(i+1,x.length-1)]-x[i])*(j-i);}
 static Map<String,Object> metrics(Brn2Model m,List<TrajectorySampler.Sample> samples){
  var ws=new Brn2Workspace();double loss=0,wdl=0,nnue=0,mae=0,mean=0,targetMean=0,teacherMean=0,cov=0;int same=0;
  double[] y=new double[samples.size()],abs=new double[y.length];int i=0;
  for(var s:samples){double p=m.evaluate(s.board(),ws),w=s.target(),n=BrnSupervision.teacherValue(teacher,s),t=SUP.target(w,n);require(t==.5*w+.5*n,"Target mismatch");y[i]=p;abs[i++]=Math.abs(p);loss+=.5*(p-t)*(p-t);wdl+=.5*(p-w)*(p-w);nnue+=.5*(p-n)*(p-n);mae+=Math.abs(p-t);mean+=p;targetMean+=t;teacherMean+=n;cov+=w*n;if(Math.signum(w)==Math.signum(n))same++;}
  int n=y.length;return fields("N",n,"baseLoss",loss/n,"WdlComponent",wdl/n,"NnueComponent",nnue/n,"MAE",mae/n,"meanSTM",mean/n,"targetMean",targetMean/n,"teacherMean",teacherMean/n,"wdlTeacherProduct",cov/n,"wdlTeacherSignPct",100.*same/n,"predictionP05",quant(y,.05),"predictionP50",quant(y,.5),"predictionP95",quant(y,.95),"absPredictionP50",quant(abs,.5),"absPredictionP95",quant(abs,.95));
 }
 static void report(PrintWriter out,String label,Brn2Trainer trainer,BootstrapData data){
  var held=data.partition().heldOut();var strict=held.stream().filter(s->!trainKeys.contains(s.board()[5])).toList();
  write(out,"type","metrics","model",label,"step",trainer.optimizer().step(),"optimizer",trainer.config().toString(),"train",metrics(trainer.snapshot(),data.partition().training()),"held",metrics(trainer.snapshot(),held),"strictHeld",metrics(trainer.snapshot(),strict));
 }
 public static void main(String[] args)throws Exception{
  Path d=Path.of(args[0]);String mode=args[1];byte[] teacherBytes=Files.readAllBytes(d.resolve("teacher.nnue"));var net=NnueNetworkCodec.decode(teacherBytes);teacher=new NnueEvaluator(net);
  var data=ReplayData.load(d.resolve("campaign.data"));var samples=data.partition().training();for(var s:samples)trainKeys.add(s.board()[5]);
  byte[] state=Files.readAllBytes(d.resolve("g134.state")),model=Files.readAllBytes(d.resolve("g134.brn2"));var baseline=Brn2Codec.decodeTraining(state);require(Arrays.equals(model,Brn2Codec.encodeModel(baseline.snapshot())),"Inference/state weights mismatch");
  try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve(mode+".jsonl"),StandardOpenOption.CREATE_NEW))){
   if(mode.equals("prepare")){
    require(data.planHash().equals("c0a61a2583acd182478d4afe4ceaa521d05170f67ce50287fadb5ed159973a78"),"Plan pin");
    var parent=Brn2Codec.decodeTraining(Files.readAllBytes(d.resolve("g133.state")));
    var stats=Brn2SelfPlayTraining.trainSamples(parent,samples,CONFIG,new SelfPlayControl(),p->{},SUP.targets(teacher)).orElseThrow();
    require(Arrays.equals(state,Brn2Codec.encodeTraining(parent)),"Historical g133 -> g134 full training payload mismatch");
    write(out,"type","historicalExactReplay","statistics",stats.toString(),"fullTrainingBytesEqual",true);
    try(var frozen=new DataOutputStream(Files.newOutputStream(d.resolve("targets.bin"),StandardOpenOption.CREATE_NEW));var json=new PrintWriter(Files.newBufferedWriter(d.resolve("targets.jsonl"),StandardOpenOption.CREATE_NEW))){
     for(var split:List.of("train","held")){var rows=split.equals("train")?samples:data.partition().heldOut();frozen.writeInt(rows.size());for(int i=0;i<rows.size();i++){var s=rows.get(i);double w=s.target(),n=BrnSupervision.teacherValue(teacher,s),t=SUP.target(w,n);for(long v:s.board())frozen.writeLong(v);frozen.writeDouble(w);frozen.writeDouble(n);frozen.writeDouble(t);write(json,"split",split,"index",i,"board",Arrays.stream(s.board()).boxed().toList(),"wdlSTM",w,"nnueSTM",n,"targetSTM",t);}}
    }
    report(out,"g134",baseline,data);
   }else if(mode.equals("control")){
    var manual=Brn2Codec.decodeTraining(state);var order=new ArrayList<Integer>();for(int i=0;i<samples.size();i++)order.add(i);var random=new SplittableRandom(SEED);for(int i=order.size()-1;i>0;i--)Collections.swap(order,i,random.nextInt(i+1));
    double[] frozen=new double[samples.size()];try(var input=new DataInputStream(Files.newInputStream(d.resolve("targets.bin")))){require(input.readInt()==samples.size(),"Frozen size");for(int i=0;i<frozen.length;i++){for(long v:samples.get(i).board())require(input.readLong()==v,"Frozen board");require(input.readDouble()==samples.get(i).target(),"Frozen WDL");double n=input.readDouble();frozen[i]=input.readDouble();require(frozen[i]==SUP.target(samples.get(i).target(),n),"Frozen target");require(n==BrnSupervision.teacherValue(teacher,samples.get(i)),"Frozen teacher");}}
    long start=System.nanoTime();var stats=Brn2SelfPlayTraining.trainSamples(baseline,samples,CONFIG,new SelfPlayControl(),p->{},SUP.targets(teacher)).orElseThrow();long elapsed=System.nanoTime()-start;
    for(int i:order)manual.train(samples.get(i).board(),frozen[i]);
    byte[] result=Brn2Codec.encodeTraining(baseline);require(Arrays.equals(result,Brn2Codec.encodeTraining(manual)),"Control full state mismatch");
    Files.write(d.resolve("lambda0.state"),result,StandardOpenOption.CREATE_NEW);byte[] saved=Brn2Codec.encodeModel(baseline.snapshot());Files.write(d.resolve("lambda0.brn2"),saved,StandardOpenOption.CREATE_NEW);require(Arrays.equals(saved,Brn2Codec.encodeModel(Brn2Codec.decodeModel(saved))),"Codec roundtrip");
    write(out,"type","training","statistics",stats.toString(),"elapsedNs",elapsed,"fullOrdinaryUpdateEquality",true,"auxiliaryEvaluations",0,"auxiliaryGradient",0);report(out,"lambda0",baseline,data);
   }else throw new IllegalArgumentException(mode);
  }
  require(Arrays.equals(teacherBytes,NnueNetworkCodec.encode(net)),"Teacher mutated");System.out.println("PASS "+mode+" train="+samples.size()+" held="+data.partition().heldOut().size()+" exact ordinary state equality; teacher immutable");
 }
}
```

### patch_auxiliary.py

```python
from pathlib import Path
import ast,re
D=Path(__file__).parent
report=(D/'repo/BRN_CAPTURE_CONSISTENCY_TRAINING_DIAGNOSTIC_2026-09-24.md').read_text(encoding='utf-8')
script=re.search(r'### patch_experiment.py\s+```python\n(.*?)\n```',report,re.S).group(1)
tree=ast.parse(script)
addition=next(ast.literal_eval(n.value) for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='addition' for t in n.targets))
addition=addition[addition.index('    public double[] trainCapture'):]
addition=addition.replace('trainCapture','trainCampaignCapture').replace('double neighbourTarget','double targetDelta')
addition=addition.replace('!Double.isFinite(neighbourTarget) || Math.abs(neighbourTarget)>1','!Double.isFinite(targetDelta) || Math.abs(targetDelta)>1')
addition=addition.replace('double y=predict(board), n=predict(neighbour), e=-n-y+target+neighbourTarget;', 'if (lambda==0) return new double[]{train(board,target),0};\n        double y=predict(board), n=predict(neighbour), e=-n-y-targetDelta;')
addition=addition.replace('        if (lambda==0) return new double[]{train(board,target),delta};\n','')
addition='\n    /** Isolated diagnostic: one unchanged ordinary base example; synthetic child has auxiliary gradient only. */\n'+addition
p=D/'repo/app/src/main/java/com/ohinteractive/seedv6/core/brn2/Brn2Trainer.java';s=p.read_text();assert 'trainCampaignCapture' not in s
s=s.replace('64 + 2 * (64 * 63 / 2) + 64;', '2 * (64 + 2 * (64 * 63 / 2) + 64);').replace('    private void add(',addition+'\n    private void add(')
p.write_text(s)
print('Only isolated trainer changed; ordinary train method unchanged')
```

### CampaignCapture.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.*;import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.brn2.*;import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;import com.ohinteractive.seedv6.training.checkpoint.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class CampaignCapture {
 static final long PAIR_SEED=2026092404L;static final double H=.25;
 public record Row(boolean held,int index,long[] parent,double wdl,double tp,double target,long move,long[] child,double tc){public boolean pair(){return child!=null;}public double delta(){return .5*(-tc-tp);}}
 static void require(boolean c,String s){if(!c)throw new AssertionError(s);}
 static double huber(double e){return Math.abs(e)<=H?.5*e*e:H*(Math.abs(e)-.5*H);}
 static double target(NnueEvaluator t,long[] b){t.evaluate(b);return t.boundedValue();}
 static List<Row> prepare(Path d,NnueEvaluator teacher)throws Exception{
  var rng=new SplittableRandom(PAIR_SEED);var rows=new ArrayList<Row>();var data=ReplayData.load(d.resolve("campaign.data"));int excluded=0;
  try(var in=new DataInputStream(Files.newInputStream(d.resolve("targets.bin")))){
   for(boolean held:new boolean[]{false,true}){var samples=held?data.partition().heldOut():data.partition().training();require(in.readInt()==samples.size(),"count");for(int i=0;i<samples.size();i++){
    long[] p=new long[6];for(int k=0;k<6;k++)p[k]=in.readLong();double w=in.readDouble(),tp=in.readDouble(),base=in.readDouble();require(Arrays.equals(p,samples.get(i).board())&&w==samples.get(i).target(),"Persisted base sample changed");require(tp==target(teacher,p)&&base==.5*w+.5*tp,"Base target changed");
    var candidates=new ArrayList<Long>();if(new HeadlessGame(p,2).active())for(long m:CaptureDiagnostic.legal(p))if(CaptureDiagnostic.capture(p,m)){var c=CaptureDiagnostic.play(p,m);if(new HeadlessGame(c,2).active())candidates.add(m);else excluded++;}
    long move=0;long[] child=null;double tc=0;if(!candidates.isEmpty()){move=candidates.get(rng.nextInt(candidates.size()));child=CaptureDiagnostic.play(p,move);var game=new HeadlessGame(p,2);game.play(move);require(Arrays.equals(game.boardSnapshot(),child),"Legal child transition");require(Long.bitCount(p[0]|p[1]|p[2])==Long.bitCount(child[0]|child[1]|child[2])+1,"Capture occupancy");require(Board.player((int)p[4])!=Board.player((int)child[4]),"STM orientation");tc=target(teacher,child);require(tc==target(teacher,child),"Teacher determinism");double virtualChild=-.5*w+.5*tc;require(Math.abs((-virtualChild-base)-.5*(-tc-tp))<3e-16,"Shared-anchor cancellation");}
    rows.add(new Row(held,i,p,w,tp,base,move,child,tc));
   }}require(in.read()==-1,"trailing");
  }
  try(var out=new DataOutputStream(Files.newOutputStream(d.resolve("replay-pairs.bin"),StandardOpenOption.CREATE_NEW));var json=new PrintWriter(Files.newBufferedWriter(d.resolve("replay-pairs.jsonl"),StandardOpenOption.CREATE_NEW))){out.writeInt(rows.size());for(var r:rows){out.writeBoolean(r.held);out.writeInt(r.index);for(long x:r.parent)out.writeLong(x);out.writeDouble(r.wdl);out.writeDouble(r.tp);out.writeDouble(r.target);out.writeLong(r.move);out.writeBoolean(r.pair());if(r.pair()){for(long x:r.child)out.writeLong(x);out.writeDouble(r.tc);}write(json,"split",r.held?"held":"train","index",r.index,"baseTarget",r.target,"wdl",r.wdl,"parent",Arrays.stream(r.parent).boxed().toList(),"move",r.pair()?Move.coordinate(r.move):null,"child",r.pair()?Arrays.stream(r.child).boxed().toList():null,"tp",r.tp,"tc",r.pair()?r.tc:null,"targetDelta",r.pair()?r.delta():null);}}
  System.out.println("Frozen rows="+rows.size()+" pairs="+rows.stream().filter(Row::pair).count()+" terminal candidate exclusions="+excluded);return rows;
 }
 public static List<Row> readRows(Path path)throws Exception{
  var rows=new ArrayList<Row>();try(var in=new DataInputStream(Files.newInputStream(path))){int n=in.readInt();for(int i=0;i<n;i++){boolean h=in.readBoolean();int index=in.readInt();long[] p=new long[6];for(int k=0;k<6;k++)p[k]=in.readLong();double w=in.readDouble(),tp=in.readDouble(),t=in.readDouble();long m=in.readLong();long[] c=null;double tc=0;if(in.readBoolean()){c=new long[6];for(int k=0;k<6;k++)c[k]=in.readLong();tc=in.readDouble();}rows.add(new Row(h,index,p,w,tp,t,m,c,tc));}require(in.read()==-1,"trailing pairs");}return rows;
 }
 static Set<Long> trainingKeys(List<Row> rows){var keys=new HashSet<Long>();for(var r:rows)if(!r.held){keys.add(r.parent[5]);if(r.pair())keys.add(r.child[5]);}return keys;}
 static Map<String,Object> metrics(Brn2Model m,List<Row> rows,boolean held,boolean strict){
  var keys=trainingKeys(rows);var selected=rows.stream().filter(r->r.held==held&&r.pair()&&(!strict||(!keys.contains(r.parent[5])&&!keys.contains(r.child[5])))).toList();var ws=new Brn2Workspace();int n=selected.size(),sign=0,large=0;double loss=0,mean=0,baseGradSq=0,auxGradSq=0;double[] err=new double[n],pred=new double[n],truth=new double[n],signed=new double[n],targetSigned=new double[n];
  for(int i=0;i<n;i++){var r=selected.get(i);double p=m.evaluate(r.parent,ws),c=m.evaluate(r.child,ws),delta=-c-p,t=r.delta(),e=delta-t;err[i]=Math.abs(e);pred[i]=Math.abs(delta);truth[i]=Math.abs(t);signed[i]=delta;targetSigned[i]=t;loss+=huber(e);mean+=e;double bg=p-r.target,ag=Math.max(-H,Math.min(H,e));baseGradSq+=bg*bg;auxGradSq+=ag*ag;if(Math.signum(delta)==Math.signum(t))sign++;if(Math.abs(e)>.5)large++;}
  return fields("pairs",n,"huber",loss/n,"errorP50",ReplayControl.quant(err,.5),"errorP90",ReplayControl.quant(err,.9),"errorP95",ReplayControl.quant(err,.95),"signPct",100.*sign/n,"errorOverPoint5Pct",100.*large/n,"meanError",mean/n,"predAbsP50",ReplayControl.quant(pred,.5),"predAbsP95",ReplayControl.quant(pred,.95),"targetAbsP50",ReplayControl.quant(truth,.5),"targetAbsP95",ReplayControl.quant(truth,.95),"predSignedP05",ReplayControl.quant(signed,.05),"predSignedP50",ReplayControl.quant(signed,.5),"predSignedP95",ReplayControl.quant(signed,.95),"targetSignedP05",ReplayControl.quant(targetSigned,.05),"targetSignedP50",ReplayControl.quant(targetSigned,.5),"targetSignedP95",ReplayControl.quant(targetSigned,.95),"baseOutputGradientRmsPaired",Math.sqrt(baseGradSq/n),"unitAuxOutputGradientRmsPerEndpoint",Math.sqrt(auxGradSq/n));
 }
 static void report(PrintWriter out,String name,Brn2Trainer trainer,List<Row> rows,Path d)throws Exception{
  var data=ReplayData.load(d.resolve("campaign.data"));ReplayControl.trainKeys.clear();for(var r:rows)if(!r.held)ReplayControl.trainKeys.add(r.parent[5]);ReplayControl.report(out,name,trainer,data);
  write(out,"type","capture","model",name,"train",metrics(trainer.snapshot(),rows,false,false),"held",metrics(trainer.snapshot(),rows,true,false),"strictHeld",metrics(trainer.snapshot(),rows,true,true));
 }
 public static void main(String[] args)throws Exception{
  Path d=Path.of(args[0]);String mode=args[1];byte[] state=Files.readAllBytes(d.resolve("g134.state")),tb=Files.readAllBytes(d.resolve("teacher.nnue"));var net=NnueNetworkCodec.decode(tb);var teacher=new NnueEvaluator(net);ReplayControl.teacher=teacher;var trainer=Brn2Codec.decodeTraining(state);
  var rows=mode.equals("prepare-pairs")?prepare(d,teacher):readRows(d.resolve("replay-pairs.bin"));
  try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve(mode+"-capture.jsonl"),StandardOpenOption.CREATE_NEW))){
   if(mode.equals("prepare-pairs")){report(out,"g134",trainer,rows,d);report(out,"lambda0",Brn2Codec.decodeTraining(Files.readAllBytes(d.resolve("lambda0.state"))),rows,d);}
   else {double lambda=Double.parseDouble(mode);require(lambda==0||lambda==.5||lambda==2,"Unsupported lambda");var order=new ArrayList<>(rows.stream().filter(r->!r.held).toList());var rng=new SplittableRandom(ReplayControl.SEED);for(int i=order.size()-1;i>0;i--)Collections.swap(order,i,rng.nextInt(i+1));double base=0,delta=0;long start=System.nanoTime();for(var r:order){if(r.pair()){var a=trainer.trainCampaignCapture(r.parent,r.target,r.child,r.delta(),lambda,H);base+=a[0];delta+=a[1];}else base+=trainer.train(r.parent,r.target);}long elapsed=System.nanoTime()-start;
    byte[] saved=Brn2Codec.encodeTraining(trainer);if(lambda==0)require(Arrays.equals(saved,Files.readAllBytes(d.resolve("lambda0.state"))),"Lambda zero state differs from production");else{Files.write(d.resolve("lambda"+mode+".state"),saved,StandardOpenOption.CREATE_NEW);byte[] mb=Brn2Codec.encodeModel(trainer.snapshot());Files.write(d.resolve("lambda"+mode+".brn2"),mb,StandardOpenOption.CREATE_NEW);require(Arrays.equals(mb,Brn2Codec.encodeModel(Brn2Codec.decodeModel(mb))),"Model roundtrip");require(Arrays.equals(saved,Brn2Codec.encodeTraining(Brn2Codec.decodeTraining(saved))),"Training roundtrip");}
    write(out,"type","training","lambda",lambda,"updates",order.size(),"finalStep",trainer.optimizer().step(),"onlineBaseLoss",base/order.size(),"onlineAuxLossPerBaseStep",delta/order.size(),"elapsedNs",elapsed);report(out,"lambda"+mode,trainer,rows,d);
   }
  }require(Arrays.equals(tb,NnueNetworkCodec.encode(net)),"Teacher mutation");System.out.println("PASS "+mode+" immutable teacher; unchanged base rows; synthetic children auxiliary only");
 }
}
```

### CampaignLossChecks.java

```java
package com.ohinteractive.seedv6.core.brn2;
import java.nio.file.*;import java.io.*;import java.util.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;
public class CampaignLossChecks {
 record Pair(long[] p,long[] c,double tp,double td){}
 static void require(boolean c,String s){if(!c)throw new AssertionError(s);}
 static double huber(double e){return Math.abs(e)<=.25?.5*e*e:.25*(Math.abs(e)-.125);}
 static double loss(double[] w,Pair p,double lambda){var s=new Brn2Workspace();double y=s.evaluate(p.p,w),n=s.evaluate(p.c,w);return .5*(y-p.tp)*(y-p.tp)+lambda*huber(-n-y-p.td);}
 public static void main(String[] args)throws Exception{
  byte[] state=Files.readAllBytes(Path.of(args[0]));var pairs=new ArrayList<Pair>();
  for(var r:com.ohinteractive.seedv6.tools.search.CampaignCapture.readRows(Path.of(args[1])))if(r.pair())pairs.add(new Pair(r.parent(),r.child(),r.target(),r.delta()));
  var original=Brn2Codec.decodeTraining(state);var zero=Brn2Codec.decodeTraining(state);
  for(var p:pairs.subList(0,128)){
   require(original.train(p.p,p.tp)==zero.trainCampaignCapture(p.p,p.tp,p.c,p.td,0,.25)[0],"lambda0 parent loss");
  }
  require(Arrays.equals(Brn2Codec.encodeTraining(original),Brn2Codec.encodeTraining(zero)),"lambda0 complete weights/moments/step mismatch");
  System.out.println("PASS lambda=0 exact training bytes after 128 original mixed-target online updates");
  int checks=0,childOnly=0,parentOnly=0;double maxError=0;
  var model=Brn2Codec.decodeTraining(state).snapshot();
  for(var pair:pairs.subList(0,3))for(double lambda:new double[]{.5,2}){
   double[] w=model.copyWeights();var trained=new Brn2Trainer(model,new BrnAdamConfig(.001));
   trained.trainCampaignCapture(pair.p,pair.tp,pair.c,pair.td,lambda,.25);
   var singleP=new Brn2Trainer(model,new BrnAdamConfig(.001));var singleC=new Brn2Trainer(model,new BrnAdamConfig(.001));
   singleP.train(pair.p,1);singleC.train(pair.c,1);
   List<Integer> selected=new ArrayList<>();int[] family=new int[7];
   for(int i=0;i<PARAMETER_COUNT;i++)if(Math.abs(trained.optimizer().firstMoment(i))>1e-10){
    int type=i<NODE_ROWS*32?0:i<RELATION_B_ROW*32?1:i<STATUS_ROW*32?2:i<LOCAL_BIAS_OFFSET?3:i<BOARD_BIAS_OFFSET?4:i<OUTPUT_WEIGHT_OFFSET?5:6;
    if(family[type]++<3)selected.add(i);
    if(singleP.optimizer().firstMoment(i)==0&&singleC.optimizer().firstMoment(i)!=0&&childOnly<3){selected.add(i);childOnly++;}
    if(singleC.optimizer().firstMoment(i)==0&&singleP.optimizer().firstMoment(i)!=0&&parentOnly<3){selected.add(i);parentOnly++;}
   }
   selected.add(OUTPUT_BIAS);
   for(int i:selected){double old=w[i],eps=1e-7;w[i]=old+eps;double plus=loss(w,pair,lambda);w[i]=old-eps;double minus=loss(w,pair,lambda);w[i]=old;
    double expected=(plus-minus)/(2*eps),actual=trained.optimizer().firstMoment(i)/(1-.9),error=Math.abs(expected-actual);
    maxError=Math.max(maxError,error);require(error<2e-6,"Finite difference "+i+" expected="+expected+" actual="+actual);checks++;
   }
  }
  require(childOnly>0&&parentOnly>0,"Did not cover both exclusive endpoint parameter gradients");
  // Scalar derivatives explicitly test both robust regions and the common-perspective signs.
  for(double y:new double[]{-.7,.2})for(double c:new double[]{-.1,.5}){
   double tp=.3,tc=-.6,e=-c-y+tp+tc,analytic=-Math.max(-.25,Math.min(.25,e)),eps=1e-6;
   double dp=(huber(-c-(y+eps)+tp+tc)-huber(-c-(y-eps)+tp+tc))/(2*eps);
   double dc=(huber(-(c+eps)-y+tp+tc)-huber(-(c-eps)-y+tp+tc))/(2*eps);
   require(Math.abs(dp-analytic)<1e-8&&Math.abs(dc-analytic)<1e-8,"Endpoint scalar derivative");
  }
  System.out.println("PASS finite differences="+checks+" maxError="+maxError+" parentExclusive="+parentOnly+" childExclusive="+childOnly+" scalarDerivatives=8");
 }
}
```

### PredictionDump.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;import com.ohinteractive.seedv6.core.brn2.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class PredictionDump {
 public static void main(String[] args)throws Exception{
  Path d=Path.of(args[0]);var rows=CampaignCapture.readRows(d.resolve("replay-pairs.bin"));var keys=CampaignCapture.trainingKeys(rows);
  try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve("predictions.jsonl"),StandardOpenOption.CREATE_NEW))){
   for(String label:List.of("g134","lambda0","lambda0.5","lambda2")){var m=Brn2Codec.decodeModel(Files.readAllBytes(d.resolve(label+".brn2")));var ws=new Brn2Workspace();for(var r:rows)write(out,"model",label,"held",r.held(),"index",r.index(),"key",Long.toHexString(r.parent()[5]),"strictBase",!keys.contains(r.parent()[5]),"strictPair",r.pair()&&!keys.contains(r.parent()[5])&&!keys.contains(r.child()[5]),"wdl",r.wdl(),"tp",r.tp(),"target",r.target(),"yp",m.evaluate(r.parent(),ws),"yc",r.pair()?m.evaluate(r.child(),ws):null,"tc",r.pair()?r.tc():null,"targetDelta",r.pair()?r.delta():null);}
  }
 }
}
```

### CorpusFixtureCheck.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.nio.file.*;import java.util.*;import com.ohinteractive.seedv6.core.Board;
public class CorpusFixtureCheck {
 public static void main(String[] args)throws Exception{var rows=CampaignCapture.readRows(Path.of(args[0]));for(var p:CaptureDiagnostic.POSITIONS){long key=Board.fromFen(p[1])[5];System.out.println(p[0]+" baseTrain="+rows.stream().filter(r->!r.held()&&r.parent()[5]==key).count()+" baseHeld="+rows.stream().filter(r->r.held()&&r.parent()[5]==key).count()+" auxiliaryChildren="+rows.stream().filter(r->r.pair()&&r.child()[5]==key).count());}}
}
```

### run_focused.py

```python
from pathlib import Path
import subprocess
D=Path(__file__).parent
jars=Path(r'C:\Users\Central\AppData\Local\Temp\seedv6-brn-diagnostic-20260924/test-classpath.txt').read_text().strip().split(';')[1:]
assert all(Path(p).is_file() for p in jars)
classes=['com.ohinteractive.seedv6.core.brn2.Brn2CoreTest','com.ohinteractive.seedv6.core.brn2.Brn2CodecTest','com.ohinteractive.seedv6.training.selfplay.Brn2TrainingTargetsTest','com.ohinteractive.seedv6.training.selfplay.TrajectorySamplerTest','com.ohinteractive.seedv6.training.selfplay.BootstrapPartitionTest','com.ohinteractive.seedv6.search.evaluation.Brn2SearchIntegrationTest','com.ohinteractive.seedv6.tools.search.QsearchDecisionTraceTest','com.ohinteractive.seedv6.search.evaluation.NnueScoreMappingTest']
runner='''import java.io.PrintWriter;import org.junit.platform.launcher.core.*;import org.junit.platform.launcher.listeners.SummaryGeneratingListener;import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
public class FocusedTests {public static void main(String[] args){var l=new SummaryGeneratingListener();var r=LauncherDiscoveryRequestBuilder.request().selectors(SELECTORS).build();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(l);launcher.execute(r);l.getSummary().printTo(new PrintWriter(System.out));l.getSummary().printFailuresTo(new PrintWriter(System.out));if(l.getSummary().getTestsFailedCount()!=0||l.getSummary().getTestsFoundCount()==0)System.exit(1);}}
'''.replace('SELECTORS',','.join('selectClass("'+c+'")' for c in classes))
(D/'FocusedTests.java').write_text(runner)
src=D/'repo/app/src';cp=';'.join([str(D/'cal-classes')]+jars)
subprocess.run(['javac','-d',str(D/'test-classes'),'-cp',cp,'-sourcepath',str(src/'main/java')+';'+str(src/'test/java')]+[str(src/'test/java'/Path(c.replace('.','/')+'.java')) for c in classes]+[str(D/'FocusedTests.java'),str(D/'CalibrationScore.java')],check=True)
with (D/'focused-tests.txt').open('w') as out:subprocess.run(['java','-Xmx1536m','-cp',str(D/'test-classes')+';'+cp,'FocusedTests'],stdout=out,stderr=subprocess.STDOUT,check=True)
print((D/'focused-tests.txt').read_text())
```
