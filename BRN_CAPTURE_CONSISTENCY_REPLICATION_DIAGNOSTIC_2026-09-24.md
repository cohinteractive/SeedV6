# BRN capture-consistency replication diagnostic — 2026-09-24

**Primary classification: C — REPLICATION MIXED. Production-trainer experimental integration is not justified as the next work unit.** The preregistered ordinary-control and auxiliary-held-out gates pass. The new search suite improves in aggregate and across several source groups, but fails the fixed median-ratio and leave-largest-saving-out requirements. Thresholds were not relaxed.

The 12-position suite gives **113,666→91,097 qnodes (−19.86%)**, **8 improved / 3 regressed / 1 unchanged**, and median λ=2/λ=0 ratio **0.907658**, above the required ≤0.90. Removing **rep-05**, the largest saving, gives **95,947→87,559 (−8.74%)**, below the required ≥10% reduction. This is less outlier-dependent than the first fresh experiment, but still not a preregistered positive replication.

On 107 fully novel held-out capture pairs, median delta error improves **0.289197→0.175178 (39.43%)**, p90 **0.708692→0.396552**, and p95 **0.964771→0.482496 (49.99%)**. Ordinary campaign held-out loss improves **0.088542→0.080815 (8.73%)**. Novel decisive material is substantial within this bounded experiment: **122 ordinary samples / 82 capture pairs from four games**, containing both White and Black wins. Its ordinary loss and median delta error improve. Delta-sign agreement nevertheless worsens **62.62%→58.88%** on the novel holdout.

Every measured search completes depth 4 without a cap. Kiwipete improves **76,376→47,699 qnodes**, but Fianchetto regresses **4,881→18,864 (3.8648×)**, exceeding untouched g134's 13,007. These continuity fixtures do not determine the principal generalization classification. No calibration, playing-strength match or production integration occurred.

## Scope, governance and relationship to the five accepted reports

- Date: 2026-09-24 UTC / New Zealand local date. Authoritative repository: `C:\projects\seed\java\seedv6`. Inspected HEAD: **`eb46291fa350c9148218ec725bfb453870785979`**.
- The accepted [stand-pat diagnostic](BRN_QSEARCH_STAND_PAT_DIAGNOSTIC_2026-09-24.md), commit `8ea5cb99b32330c1b85ef8a918dfb24ae5336109`, established local capture geometry/score-distribution problems without an incremental BRN defect. The accepted [calibration diagnostic](BRN_QSEARCH_CALIBRATION_DIAGNOSTIC_2026-09-24.md), commit `b1d224f3a51e5bb720a15e6339db6023291b6ff2`, found population- and horizon-dependent calibration effects; calibration is excluded here.
- The accepted [initial capture-training diagnostic](BRN_CAPTURE_CONSISTENCY_TRAINING_DIAGNOSTIC_2026-09-24.md), commit `339b0d763e12141b24918190fd5b9adc00450c5c`, learned local geometry under a teacher-only/synthetic regime but failed search transfer. The [campaign-target replay](BRN_CAMPAIGN_TARGET_CAPTURE_REPLAY_DIAGNOSTIC_2026-09-24.md), commit `6eb0df1a1a48c646b1850ceaab5a397c5110eba1`, authenticated the exact 50:50 objective/full optimizer path and found bounded historical replay benefits.
- The [first fresh-generation replay](BRN_FRESH_GENERATION_CAPTURE_REPLAY_DIAGNOSTIC_2026-09-24.md), commit `eb46291fa350c9148218ec725bfb453870785979`, found mixed transfer: three novel search-game groups, draw-only novel holdout, an unstable control, and one dominant outlier. This sixth unit replicates its faithful λ=0/2 training formulation with a different fixed root seed, deliberate source stratification, decisive novel holdout and stricter preregistered gates. Earlier classifications belong to their own protocols; they are not retroactively relabeled.
- No on-disk ancestor/repository `AGENTS.md` was found. User-supplied instructions and `source/CHESS_SEARCH_CONTRACT.md` apply; no conflict or canon amendment is needed. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` are independently absent; neither was created. First successful machine-clock observation: `2026-09-24T04:30:24Z`; the earlier unsupported PowerShell `Get-Date -AsUTC` failed, so no earlier timestamp is inferred.
- Initial index empty, with **41 modified tracked files and 46 individually enumerated untracked files** inherited from earlier work. These GUI/training/document/build edits remain unattributed. All **543** initially present tracked/non-ignored files were SHA-256 inventoried and verified unchanged before report creation.
- Isolation: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-replication-20260924`. A clean `git archive HEAD` copy excluded inherited changes. The only changed existing archived source is `core/brn2/Brn2Trainer.java`, byte-identical to the accepted fourth/fifth unit's auxiliary implementation. Authoritative source remains unchanged. All generated corpora, checkpoints, scripts, classes and raw outputs remain external.

## Exact checkpoint, optimizer and campaign identity

Starting directory: `E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c`.

Generation **134**, optimizer step **218,158**, training depth **4**, canonical BRN-2/schema 2, width 32 and 1,645,665 binary64 parameters. Direct normal codecs load the model and complete training state, and their snapshots match exactly. Each arm starts from identical full bytes, including first/second Adam moments, configuration and step. Configuration: learning rate **0.001**, β1 **0.9**, β2 **0.999**, ε **1e−8**. No optimizer reset, learning-rate adjustment or checkpoint-schema change.

| Payload | SHA-256 |
| --- | --- |
| g134.brn2 | 21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924 |
| g134.state | ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9 |
| teacher.nnue | 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9 |
| lambda0.brn2 | 0e294b9d9f736b2070c1775935be1633d73b74287d71d86a2dc256435a1260de |
| lambda0.state | 6eed58a27d23e53fe8fd4a00d977d2cf68266aa19f26d1c4da4e3a1bd5e2a2de |
| lambda2.brn2 | e23a07e82fe3d3ceab6008f6a5ed362946c65f01e99936bf2adfb4cdebbc40d0 |
| lambda2.state | ad8e2b6631004391cccbaa188e8622436d57a16d60c4f9c1a297e1f9d8a4fff4 |

`g134.brn2` and `g134.state` are byte copies of `network.brn2` and `training.state` at the exact starting directory. Teacher path: `E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6\network.nnue`.

The checksummed g134 manifest and generation-134 plan independently establish HANDCRAFTED source, NNUE_BLENDED teacher weight **0.5**, and this exact g74 teacher. Plan SHA-256: `c0a61a2583acd182478d4afe4ceaa521d05170f67ce50287fadb5ed159973a78`. The generator and teacher have different roles: terminal games use handcrafted search; NNUE supplies bounded static targets. The archived high-level reader predates the live store's v4 validation-method field; read-only framed/hash-checked inspection uses the accepted reader bridge. No schema edit, store recovery, writer, candidate publication or reference update occurs. The previous exact g133→g134 reconstruction is accepted evidence, not claimed as rerun here.

## Preregistration and deterministic source construction

The immutable protocol was written at **2026-09-24T04:33:18.746493+00:00**, SHA-256 `10a6d32cd05f10ee21a68bafdbca6bc6224cefe579f6cda31267dc593d5bb731`, before source games, losses or comparative searches. Fixed root seed: **2026092402**. No seed was changed. Secondary seeds are exactly `new SplittableRandom(rootSeed XOR domainSalt).nextLong()` using Java signed 64-bit arithmetic:

| Stream | XOR salt | Derived signed long |
| --- | --- | --- |
| generation | 0x6A09E667F3BCC909 | -4638214466155141245 |
| partition | 0xA54FF53A5F1D36F1 | -2312974652461108083 |
| shuffle | 0xBB67AE8584CAA73B | -1669105197141226394 |
| capture | 0x3C6EF372FE94F82B | 355928815299128698 |

`gameSeed(i)=SelfPlayRunner.gameSeed(generationSeed,i)=SplittableRandom(generationSeed+0x9E3779B97F4A7C15*i).nextLong()`. The original candidate index, not its admitted ordinal, supplies this seed. All 64 selected seeds are distinct and absent from the 134 stored generation seed domains and first fresh generation. No other experimental RNG stream is used.

**Source-selection rule, fixed before games:** preview candidates 0–4,095 in order with the existing `ValidationArena.opening` helper, using the same standard starting board, indexed seed, legal-move order and 0–8-ply random-opening algorithm as `SelfPlayRunner`. Admit the first 64 active endpoints with **at least four randomized plies and distinct board keys**. Selection examines only opening length, legality and key; no BRN/NNUE evaluation, qnodes or terminal outcome participates. All 64 were available by candidate index 123. The complete source plan was frozen before game 0.

Run the **unchanged** `SelfPlayRunner.play(SearchEvaluation.handcrafted(), ...)` from the standard starting board for each admitted original index, and use unchanged `TrajectorySampler.sample`. Every actual game's board at the random-opening boundary is asserted equal to its preview. No validation match is run; only the native opening helper is reused. Native search retains depth 4, six root workers, fresh private TT/order state per game reused within that game, maximum 1,024 plies, maximum 32 samples, and no per-move node/time cap. Games run sequentially.

This deliberately **conditions source admission on native 4–8-ply opening diversity**; it does not claim an unconditioned random 64-game production draw. The per-game generation/search/target semantics are unchanged. This authorized source-distribution intervention is a material limitation when comparing different experiments. All games still share the ordinary chess starting position and can transpose later; distinct seeds/opening keys are not proof of IID observations.

There are **64 distinct random-opening groups and 64 distinct complete pre-move trajectory-key sequences**, one game per group. Opening lengths 4/5/6/7/8 occur **14/14/13/12/11** times. The final new suite spans **six** distinct source games, versus **three** previously; eight independent sources were reserved, but the fixed round-robin/capture quotas selected six. Both reserve games omitted by the deterministic suite rule remain unused for search. The novel holdout spans five groups, versus the prior three draw-only groups.

## One generation-sized corpus and outcome coverage

All **64 games complete: 30 White wins / 11 draws / 23 Black wins**, no cancelled, capped, failed or relabeled games. White/Black wins are actual checkmates. Draws retain their native rule termination. There are **8,584** pre-move positions, game lengths **27–359**, mean **134.125** plies. Native even-spacing is `floor(i*(positions−1)/(count−1))`, with `count=min(positions,32)`. The 27-ply source 25 supplies 27 samples; the other 63 supply 32. Total **2,043 samples**, rather than padding to 2,048. Every WDL label and sample index was independently verified against its game's real termination and STM.

The 51 TRAIN games therefore supply **1,627 updates per arm**, not an arbitrary forced 1,632: this is the same nominal generation configuration with production-faithful short-game sampling. No extra candidate pool was needed or generated; no second training generation ran.

| Partition before endpoint filtering | Games / groups | Samples | White / draw / Black games |
| --- | --- | --- | --- |
| TRAIN | 51 | 1627 | 25 / 8 / 18 |
| HELD | 5 | 160 | 2 / 1 / 2 |
| SEARCH | 8 | 256 | 3 / 2 / 3 |

## Frozen source-aware partitions and leakage audit

Fisher–Yates permute admitted source IDs with the partition seed. From that permutation, reserve the first qualifying two White-win, two Black-win and one drawn source for HELD; qualification uses ≥16 sampled positions absent from saved historical/prior-diagnostic keys. Reserve eight remaining sources for SEARCH, first ensuring one of each available outcome, then filling in permutation order. The remaining 51 whole sources are TRAIN. No source group belongs to multiple partitions. Outcome stratification is intentional evaluation construction, not post-loss model selection.

| Role | Source group IDs |
| --- | --- |
| TRAIN | 42, 56, 45, 55, 62, 58, 9, 57, 48, 11, 0, 14, 19, 35, 34, 13, 2, 30, 8, 20, 26, 7, 38, 40, 41, 47, 31, 6, 36, 3, 43, 39, 54, 15, 50, 44, 22, 59, 25, 24, 16, 5, 1, 63, 49, 52, 4, 33, 23, 51, 46 |
| HELD | 61, 60, 27, 10, 28 |
| SEARCH reserve | 32, 12, 17, 53, 18, 29, 21, 37 |

Keep every TRAIN observation and its ordinary weight, including naturally familiar opening positions. For HELD evaluation, remove any row whose parent or selected capture child touches TRAIN endpoints, and deduplicate held parent keys. Five rows are excluded, leaving **155** ordinary held observations. Of these, **153** are fully novel to scanned old data; two are historically overlapping non-pair observations. All **107** retained held capture pairs are endpoint-novel. The all-held population is therefore cross-TRAIN-disjoint even when historically familiar.

The old-data audit reads **134 SHA-checked saved data frames**, with **144,051 historical TRAIN keys / 176,859 historical train-or-held keys**. It also excludes prior synthetic/replay diagnostic endpoints, all first-fresh parent/selected-child keys, and 44 named old FEN fixtures found in diagnostic reports/tool sources. The g133 replay comparison uses the exact saved batch whose parent is g133 and whose training produced g134, matching the preceding reports' terminology. Old corpora are exclusion inputs only; none becomes current TRAIN data. The first-fresh manifest matches its accepted report hash and authenticates the reused rows/endpoint-key files; numeric historical key sets independently reproduce the previous audit. Leading-zero text formatting is normalized for that comparison; corpus bytes are unchanged.

| Raw partition | Observations / unique keys | g133 replay overlap | Any historical TRAIN | Any historical train/held | First-fresh base overlap |
| --- | --- | --- | --- | --- | --- |
| TRAIN | 1627 / 1569 | 52 (3.20%) | 86 (5.29%) | 86 (5.29%) | 52 (3.20%) |
| HELD | 160 / 156 | 5 (3.12%) | 7 (4.38%) | 7 (4.38%) | 5 (3.12%) |
| SEARCH | 256 / 247 | 8 (3.12%) | 9 (3.52%) | 16 (6.25%) | 8 (3.12%) |

Current TRAIN overlap is **52/1,627 (3.20%)** with g133 replay and **52/1,627 (3.20%)** with the first fresh corpus; **86/1,627 (5.29%)** overlap any saved historical TRAIN. These counts are separate memberships, not additive. The preceding fresh TRAIN had 20.16% g133 overlap and 45.77% historical TRAIN overlap. The lower measured overlap and doubled independent search-source count support materially broader coverage, not a claim of independence from all prior chess exposure.

Board keys ignore move counters and catch ordinary transpositions; no exclusion claim covers symmetry-equivalent positions, unsampled historical trajectories, unsaved data or every search descendant. Source groups share the standard start and early prefixes. Current TRAIN and retained HELD **endpoint-key intersection is zero**. All 12 SEARCH roots are unique and absent from TRAIN endpoints, all reserved HELD endpoints, saved history, first-fresh endpoints and named prior diagnostics. Selected roots are active, legal and exact FEN round trips.

| Held population | Ordinary samples | Capture pairs | Source provenance |
| --- | --- | --- | --- |
| All retained | 155 | 107 | 5 games; cross-TRAIN endpoint-disjoint |
| Fully novel | 153 | 107 | 5 games |
| Decisive retained | 124 | 82 | 2 White-win and 2 Black-win games |
| Fully novel decisive | 122 | 82 | 62 samples from White wins, 60 from Black wins |
| Drawn / novel drawn | 31 | 25 | 1 game |
| Historically overlapping | 2 | 0 | ordinary metrics only |

| HELD group | Candidate / seed | Actual result / termination | Plies | Novel ordinary / pairs |
| --- | --- | --- | --- | --- |
| 61 | 121 / -870498631292286354 | WHITE_WIN / WHITE_CHECKMATES_BLACK | 201 | 31 / 11 |
| 60 | 120 / 4261759917718071546 | WHITE_WIN / WHITE_CHECKMATES_BLACK | 101 | 31 / 23 |
| 27 | 50 / -7686137226003063494 | BLACK_WIN / BLACK_CHECKMATES_WHITE | 60 | 30 / 25 |
| 10 | 21 / -1696238650526761584 | BLACK_WIN / BLACK_CHECKMATES_WHITE | 76 | 30 / 23 |
| 28 | 52 / -8075803904750421179 | DRAW / THREEFOLD_REPETITION | 78 | 31 / 25 |

Source plan frozen **2026-09-24T04:35:04.711035200Z**; suite FENs fixed **2026-09-24T04:37:26.545870700Z**; full input manifest fixed **2026-09-24T04:38:26.184914+00:00**. Both final checkpoints fixed **2026-09-24T04:39:13.602902+00:00**. Held metrics saved **2026-09-24T04:40:00.047617+00:00**, before first search **2026-09-24T04:40:38.010693500Z**. Control gate recorded **2026-09-24T04:40:49.184104+00:00**, before λ=2 search **2026-09-24T04:41:53.694506300Z**. No corpus, suite, gate, stopping point or checkpoint changed after comparison.

| Frozen input | SHA-256 |
| --- | --- |
| source-plan.jsonl | d1b013e2db29d71f356e7499943d361ed71b7c7015f4d73cc7235dd966fa69f3 |
| fresh-base.bin | 905db157705db7af83d2608074b49037b29b8cbe41dada3e15d12eb5bc89c473 |
| fresh-rows.bin | fcfe1a52b4f90bf222357c1a7426b025d274587c232c986201f87c250a0ebd1d |
| partition.json | 72d5948d94d8b8e23b476c24d9438fe93c41bbb5d18d07fede91f774f9d7069d |
| search-suite.tsv | 9773214a0b9c18506a1b22373ca7198c3a4c0ffdde2d47290cae8fb6df13ba1e |
| frozen-manifest.json | 83b4743ba74c2ae99c015661447e33352e7cad0fcb1b52baae9961a8ae83e8e8 |

## Exact ordinary objective, auxiliary relation and schedule

```text
w = actual terminal WDL in parent STM orientation, in {-1,0,+1}
n_p,n_c = pinned NNUE tanh(raw), each native STM
y_p,y_c = continuous BRN tanh output, each native STM
t_parent = 0.5*w + 0.5*n_p
L_base = 0.5*(y_p-t_parent)^2

predicted_delta = -y_c-y_p                 # common parent perspective
target_delta = 0.5*(-n_c-n_p)
e = predicted_delta-target_delta
Huber_0.25(e) = 0.5*e^2 if |e|<=0.25; else 0.25*(|e|-0.125)
L_step = unchanged L_base + lambda*I(has_eligible_capture)*Huber_0.25(e)
lambda = 0 or 2 only
```

The shared terminal anchor cancels algebraically through the derivation-only child expression `-0.5*w+0.5*n_c`. Synthetic children receive **no invented terminal result and no ordinary update**. They supply only the local auxiliary relation. This is the exact accepted campaign geometry, not teacher-only replacement, zero-delta smoothing or calibration.

Enumerate coordinate-sorted legal captures for each base row in original source/sample order, exclude terminal/rule-drawn children, and choose one uniformly from the frozen capture stream. Fourteen terminal candidate children are excluded. Child selection uses no model/teacher score. There are **981 paired TRAIN rows / 1,627**; unpaired rows still receive their ordinary update. Each transition matches `HeadlessGame.play`, decreases occupancy by one, flips STM and satisfies shared-anchor cancellation. Parent targets equal production `BrnSupervision.blended(.5).targets(...)` exactly. Teacher bytes/serialization remain immutable.

Both endpoint Jacobians use the same pre-update weights and their own ReLU masks, aggregate shared parameters, then make **one** sparse Adam update with the unchanged base gradient. Both native-STM auxiliary output derivatives are `-lambda*clip(e,-0.25,0.25)`. λ=0 dispatches to ordinary `train` before child evaluation; its auxiliary loss contribution and work are exactly zero.

Schedule: **one epoch, minibatch 1, one fixed Fisher–Yates pass, 1,627 updates**, final optimizer step **219,785**. Same initial state, examples, order, teacher, target values, weighting, optimizer config and stopping point in both arms. No gradient clipping, weight decay, added child base examples, extra epoch, λ sweep or model selection. The final deterministic checkpoint is used. Experimental payloads load under pristine production codecs/search classes.

| Arm | Updates / final step | Online base loss | Online auxiliary per base step |
| --- | --- | --- | --- |
| λ=0 | 1627 / 219785 | 0.079910 | 0 |
| λ=2 | 1627 / 219785 | 0.083381 | 0.015116 |

The pristine production λ=0 pass, independent frozen-target ordinary loop and auxiliary zero branch yield **byte-identical complete final training states over all 1,627 updates**. A further 128-update loss/state equality check passes. The unchanged accepted auxiliary implementation passes **72 parameter finite differences**, maximum absolute error **8.89e−10** against tolerance 2e−6, covering parameter families and both endpoint-exclusive gradients, plus eight scalar derivative checks. Historical helper entrypoints containing other arms are not invoked; this experiment runs only λ=0 and λ=2.

## Held-out results, completed before final search

All losses are equally weighted mean half-squared errors on continuous bounded outputs. This is one scalar blended head. WDL/NNUE columns are descriptive `0.5*(y-w)^2` and `0.5*(y-n)^2`; `L_base=0.5*L_WDL+0.5*L_NNUE-0.125*(w-n)^2`. They are not separately optimized heads. Quantiles interpolate at `(N−1)*p`.

| Population | Model | N | Ordinary campaign loss | WDL component | NNUE component |
| --- | --- | --- | --- | --- | --- |
| All | g134 | 155 | 0.094247 | 0.361566 | 0.112159 |
| All | lambda0 | 155 | 0.088542 | 0.323782 | 0.138534 |
| All | lambda2 | 155 | 0.080815 | 0.331753 | 0.115109 |
| Novel | g134 | 153 | 0.093931 | 0.360640 | 0.113612 |
| Novel | lambda0 | 153 | 0.086927 | 0.320429 | 0.139815 |
| Novel | lambda2 | 153 | 0.079295 | 0.328888 | 0.116091 |
| Decisive | g134 | 124 | 0.107790 | 0.435324 | 0.118174 |
| Decisive | lambda0 | 124 | 0.098183 | 0.389498 | 0.144785 |
| Decisive | lambda2 | 124 | 0.093248 | 0.404884 | 0.119528 |
| Novel decisive | g134 | 122 | 0.107617 | 0.435371 | 0.120095 |
| Novel decisive | lambda0 | 122 | 0.096315 | 0.386370 | 0.146494 |
| Novel decisive | lambda2 | 122 | 0.091545 | 0.402490 | 0.120833 |
| Drawn | g134 | 31 | 0.040073 | 0.066535 | 0.088101 |
| Drawn | lambda0 | 31 | 0.049978 | 0.060919 | 0.113528 |
| Drawn | lambda2 | 31 | 0.031083 | 0.039228 | 0.097430 |
| Overlapping | g134 | 2 | 0.118388 | 0.432431 | 0.000980 |
| Overlapping | lambda0 | 2 | 0.212112 | 0.580319 | 0.040540 |
| Overlapping | lambda2 | 2 | 0.197136 | 0.550950 | 0.039956 |

| Population | Model | Pairs | Delta error median / p90 / p95 | Huber | Error >0.5 | Delta-sign agreement |
| --- | --- | --- | --- | --- | --- | --- |
| All/novel | g134 | 107 | 0.232868 / 0.592255 / 0.737772 | 0.047190 | 17.76% | 66.36% |
| All/novel | lambda0 | 107 | 0.289197 / 0.708692 / 0.964771 | 0.061656 | 27.10% | 62.62% |
| All/novel | lambda2 | 107 | 0.175178 / 0.396552 / 0.482496 | 0.027211 | 4.67% | 58.88% |
| Decisive/novel decisive | g134 | 82 | 0.230785 / 0.567673 / 0.721679 | 0.045368 | 14.63% | 64.63% |
| Decisive/novel decisive | lambda0 | 82 | 0.286513 / 0.811842 / 1.005179 | 0.062232 | 24.39% | 60.98% |
| Decisive/novel decisive | lambda2 | 82 | 0.152251 / 0.437573 / 0.509650 | 0.027190 | 6.10% | 57.32% |
| Drawn | g134 | 25 | 0.232868 / 0.637259 / 0.726460 | 0.053166 | 28.00% | 72.00% |
| Drawn | lambda0 | 25 | 0.345782 / 0.674569 / 0.717749 | 0.059769 | 36.00% | 68.00% |
| Drawn | lambda2 | 25 | 0.187189 / 0.375461 / 0.412837 | 0.027281 | 0.00% | 64.00% |

All/novel pair populations are identical; decisive/novel-decisive pair populations are identical. The two overlapping ordinary rows have no pairs. Large unsupported delta error is explicitly **|predicted delta−target delta|>0.5**, a fixed error threshold, not rejection of legitimately large target deltas. Counts are **29/107→5/107** for λ=0→2. Sign agreement has no post hoc deadband and worsens despite lower absolute error. That weakness is not hidden by shrinking raw deltas.

| Population | Model | Score mean / SD | Score p05 / median / p95 |
| --- | --- | --- | --- |
| All | g134 | -0.047450 / 0.388595 | -0.665207 / -0.045093 / 0.584551 |
| All | lambda0 | -0.065349 / 0.418320 | -0.673106 / -0.078776 / 0.660940 |
| All | lambda2 | 0.011283 / 0.333177 | -0.536067 / -0.020833 / 0.577707 |
| Novel | g134 | -0.044181 / 0.389829 | -0.668124 / -0.041761 / 0.585870 |
| Novel | lambda0 | -0.064831 / 0.420940 | -0.673326 / -0.078776 / 0.662465 |
| Novel | lambda2 | 0.012453 / 0.335147 | -0.540007 / -0.015868 / 0.583277 |
| Decisive | g134 | -0.035876 / 0.396254 | -0.646054 / -0.062851 / 0.602577 |
| Decisive | lambda0 | -0.048542 / 0.437336 | -0.674317 / -0.084631 / 0.671861 |
| Decisive | lambda2 | 0.030150 / 0.344085 | -0.523539 / -0.026110 / 0.625138 |
| Novel decisive | g134 | -0.031586 / 0.397767 | -0.646643 / -0.053256 / 0.603611 |
| Novel decisive | lambda0 | -0.047616 / 0.440749 | -0.674537 / -0.084631 / 0.671890 |
| Novel decisive | lambda2 | 0.031926 / 0.346560 | -0.524012 / -0.018351 / 0.626131 |
| Drawn | g134 | -0.093749 / 0.352535 | -0.723844 / 0.075753 / 0.386816 |
| Drawn | lambda0 | -0.132580 / 0.322893 | -0.648731 / -0.073386 / 0.322456 |
| Drawn | lambda2 | -0.064184 / 0.272646 | -0.527502 / -0.002558 / 0.358284 |

| Pair population | Model | Predicted delta mean / SD | Predicted signed p05 / median / p95 | Predicted abs median / p95 |
| --- | --- | --- | --- | --- |
| All/novel | g134 | 0.219495 / 0.316039 | -0.205107 / 0.174782 / 0.759368 | 0.215587 / 0.759368 |
| All/novel | lambda0 | 0.269279 / 0.368902 | -0.282425 / 0.227717 / 0.905670 | 0.334360 / 0.905670 |
| All/novel | lambda2 | 0.080357 / 0.249283 | -0.290479 / 0.068442 / 0.488998 | 0.176712 / 0.488998 |
| Decisive | g134 | 0.194821 / 0.296764 | -0.213840 / 0.156973 / 0.684133 | 0.196004 / 0.684133 |
| Decisive | lambda0 | 0.235553 / 0.386709 | -0.316288 / 0.196352 / 0.920699 | 0.296585 / 0.920699 |
| Decisive | lambda2 | 0.047236 / 0.245528 | -0.312048 / 0.016087 / 0.455369 | 0.179314 / 0.459621 |
| Drawn | g134 | 0.300426 / 0.360660 | -0.157315 / 0.256112 / 0.863624 | 0.319558 / 0.863624 |
| Drawn | lambda0 | 0.379899 / 0.275659 | -0.052202 / 0.443898 / 0.768921 | 0.443898 / 0.768921 |
| Drawn | lambda2 | 0.188993 / 0.229863 | -0.150856 / 0.174279 / 0.495868 | 0.176159 / 0.495868 |

| Pair population | Target delta mean / SD | Target signed p05 / median / p95 | Target abs median / p95 |
| --- | --- | --- | --- |
| All/novel | -0.001468 / 0.146627 | -0.269360 / 0.013629 / 0.215647 | 0.083321 / 0.306633 |
| Decisive | -0.019697 / 0.150633 | -0.276281 / 0.004423 / 0.198286 | 0.080720 / 0.347669 |
| Drawn | 0.058321 / 0.113706 | -0.089436 / 0.041117 / 0.267988 | 0.083321 / 0.267988 |

| Novel held source | Outcome | N / pairs | λ=0→2 ordinary loss | λ=0→2 median delta error |
| --- | --- | --- | --- | --- |
| 61 | WHITE_WIN | 31 / 11 | 0.119763 → 0.106631 | 0.185827 → 0.120656 |
| 60 | WHITE_WIN | 31 / 23 | 0.087466 → 0.109220 | 0.238008 → 0.121745 |
| 27 | BLACK_WIN | 30 / 25 | 0.119158 → 0.092507 | 0.634928 → 0.349996 |
| 10 | BLACK_WIN | 30 / 23 | 0.058387 → 0.056730 | 0.245820 → 0.137746 |
| 28 | DRAW | 31 / 25 | 0.049978 → 0.031083 | 0.345782 → 0.187189 |

Median delta error improves in **all five** novel held source groups. Ordinary loss improves in four; White-win source 60 worsens **0.087466→0.109220 (+24.87%)**, while its delta median improves. Both aggregate decisive and drawn populations improve ordinary/delta metrics. This passes the preregistered “no clear decisive reversal across both metrics” criterion, operationalized before training as >5% worsening in both ordinary loss and median delta error. It does not establish uniform ordinary-quality retention in every source. Five held groups remain a small correlated sample.

## Preregistered control-stability gate — PASS

The control audit below was saved before λ=2 search. Ordinary loss uses the same 155 retained held observations for all models; delta gates use fully novel endpoint-disjoint pairs. Clauses and numeric limits are those fixed before comparison.

| Clause | Requirement | Observed λ=0 versus g134 | Verdict |
| --- | --- | --- | --- |
| 1 | No new continuity cap / failure to reach depth 4 | 0; all four complete | PASS |
| 2 | Fewer than two continuity fixtures >3× qnodes | 1: Opening 5.8500×; Kiwipete 0.3649×, Fianchetto 0.3753×, EP 0.6111× | PASS |
| 3a | No more than one new-suite depth loss | 0; all 12 complete | PASS |
| 3b | New-suite total qnode ratio ≤2.0 | 113,666 / 129,100 = 0.880449 | PASS |
| 3c | New-suite median qnode ratio ≤1.5 | 0.949568 | PASS |
| 4 | Ordinary held loss ratio ≤1.10 | 0.088542 / 0.094247 = 0.939471 | PASS |

No control clause fails. Opening's **220→1,287 qnodes (+485%)** and score **−872→−13,826** are genuine exceptions, not erased by the pass. The fixed rule permits one >3× continuity fixture. Unlike the first fresh run, Kiwipete and Fianchetto improve substantially under ordinary continuation, and the new suite is stable. This is evidence against attributing all fresh continuation problems to an unavoidable g134 optimizer defect; source selection, data and order changed together, so their causal shares are not isolated.

## Preregistered auxiliary-held-out gate — PASS

| Clause | Requirement | Observed λ=2 versus λ=0 | Verdict |
| --- | --- | --- | --- |
| 1 | Novel median error improves ≥10% | 0.289197→0.175178; ratio 0.605740 (−39.43%) | PASS |
| 2 | Ordinary held loss worsens ≤5% | 0.088542→0.080815; ratio 0.912731 (−8.73%) | PASS |
| 3 | Novel p95 error worsens ≤5% | 0.964771→0.482496; ratio 0.500115 (−49.99%) | PASS |
| 4 | No clear decisive reversal across both ordinary and delta metrics | All decisive loss ratio 0.949736, novel decisive 0.950471; decisive median ratio 0.531392 | PASS |

## Frozen independent search suite and search conditions

From reserved SEARCH sources, exclude roots touching history, earlier diagnostics, TRAIN endpoints or all HELD endpoints. There are 240 eligible roots across eight sources. Select **six capture-rich roots (≥3 legal captures)**, then **six quieter roots (1–2 legal captures)**. For each category, iterate SEARCH groups in the frozen seeded permutation order, choose each group's smallest unsigned unused key, and allow at most two selected roots per group. The original rule succeeds unchanged: 12 roots from six groups, with one rich and one quieter position per selected group. All roots have legal captures. Outcomes: **6 positions from White-win games / 4 from draws / 2 from a Black-win game**. Two reserved source groups are not selected; no result-based replacement occurs.

Search all models identically with pristine engine classes: target **depth 4**, one worker, fresh **262,144-entry TT**, singleton root history, ordinary iterative deepening, cumulative **1,000,000 entered-node** and **60-second** caps. Neural mate-distance-only selectivity, qsearch soft limit 16, check evasions, ordering, pruning, extensions/reductions and depth semantics are unchanged. Existing `QsearchDecisionTrace` shadows NNUE at stride 1 and samples detached boards at stride 101, maximum 12,000. This observer supplies no search decisions. No calibration seam is present; the reusable helper named `CalibrationSearch` runs RAW only.

Qnodes count entered qsearch children, excluding main leaves reused as qsearch roots. Q%=q/total entered nodes; SP%=cutoffs/eligible attempts. Root scores use native mapped units, **not centipawns**. Every row below completes depth 4; node/time-cap state is **none** for all 48 primary requests. Timing includes tracing/JIT and is secondary, not a warmed performance claim.

| ID | Source / sample / pre-move ply | Outcome | Captures | Position key | Exact FEN |
| --- | --- | --- | --- | --- | --- |
| rep-01 | 32 / 8 / 41 | WHITE_WIN | 3 | 22bfa8af1fece46c | `r4knr/1p3pp1/8/2bNR3/P6p/6P1/1PP2P1P/R5K1 b - - 0 21` |
| rep-02 | 17 / 10 / 33 | DRAW | 3 | 21c69a2136d523ff | `3r3r/1kppnppp/pp6/2b1n3/2PN4/PP2P3/3B1P1P/R3K1NR b KQ - 0 17` |
| rep-03 | 53 / 11 / 51 | WHITE_WIN | 3 | 98c1f164a1766aea | `5r1k/ppp3p1/7p/1R5P/2B5/5PK1/P2r2P1/4R3 b - - 0 26` |
| rep-04 | 18 / 8 / 22 | DRAW | 6 | dfc6c356f55f563 | `2bqk1r1/r1pp1ppp/ppn4P/3N4/4p3/3PQN2/PbP1PPP1/R3KB1R w KQ - 0 12` |
| rep-05 | 29 / 4 / 32 | WHITE_WIN | 3 | b2364c84af5c8b49 | `r4k1r/3nbp1p/2pN3p/p3P2b/PpB2N2/8/1PP2PPP/R3K2R w - - 2 17` |
| rep-06 | 12 / 5 / 21 | BLACK_WIN | 4 | 96c8bdd213770d1 | `2kr1b1r/p2ppp2/n1p3pp/q4n2/3N1PQ1/2N1P3/PPPP2PP/R1B1K2R b KQ - 1 11` |
| rep-07 | 32 / 28 / 146 | WHITE_WIN | 1 | 1177b943a0e4513a | `4k3/R6r/8/5K2/4N3/8/8/8 w - - 2 74` |
| rep-08 | 17 / 23 / 77 | DRAW | 2 | 8db562ce955bdf6 | `8/7p/2k1np2/5R2/4PP2/2K5/3r3P/8 b - - 6 39` |
| rep-09 | 53 / 7 / 32 | WHITE_WIN | 1 | 171f712d79e9d710 | `3rk2r/ppp3pp/2b2p2/8/7P/N1b1BP2/P4KP1/4RB1R w k - 2 17` |
| rep-10 | 18 / 14 / 40 | DRAW | 2 | 292d4b4743e803ab | `2bq4/r1ppkp2/ppn2N1Q/8/8/b2P1N2/P1P1PPP1/1R2KB2 w - - 7 21` |
| rep-11 | 29 / 8 / 65 | WHITE_WIN | 1 | 23b24c87402eb7a7 | `6kb/3r3p/2p2N1R/4n3/Pp3P1P/6P1/1P2B3/2K5 b - - 1 33` |
| rep-12 | 12 / 31 / 133 | BLACK_WIN | 2 | 2b1957c02233957e | `8/8/p2p4/P7/3p4/1kq5/4p3/1KR5 b - - 15 67` |

## Complete fresh search results

| Position | Model | Depth / cap | Total nodes | Qnodes | Q% | SP attempts | SP cutoffs | SP% | Score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| rep-01 | g134 | 4 / none | 15,479 | 11,364 | 73.42 | 12,528 | 6,355 | 50.73 | 6563 / h4g3 | 0.368 |
| rep-01 | lambda0 | 4 / none | 13,322 | 7,845 | 58.89 | 10,773 | 6,916 | 64.20 | 4882 / h4g3 | 0.402 |
| rep-01 | lambda2 | 4 / none | 9,200 | 4,944 | 53.74 | 7,301 | 5,332 | 73.03 | 4251 / h4g3 | 0.295 |
| rep-02 | g134 | 4 / none | 14,787 | 8,987 | 60.78 | 12,528 | 7,174 | 57.26 | 5061 / e5d3 | 0.248 |
| rep-02 | lambda0 | 4 / none | 15,381 | 9,471 | 61.58 | 13,249 | 7,322 | 55.26 | 2974 / e5d3 | 0.244 |
| rep-02 | lambda2 | 4 / none | 12,116 | 6,553 | 54.09 | 10,026 | 7,187 | 71.68 | 5575 / e5d3 | 0.191 |
| rep-03 | g134 | 4 / none | 13,493 | 4,977 | 36.89 | 10,598 | 6,947 | 65.55 | -7409 / b7b6 | 0.152 |
| rep-03 | lambda0 | 4 / none | 14,482 | 4,712 | 32.54 | 11,368 | 8,036 | 70.69 | -6562 / c7c6 | 0.196 |
| rep-03 | lambda2 | 4 / none | 12,170 | 3,452 | 28.36 | 9,567 | 7,393 | 77.28 | -3178 / b7b6 | 0.129 |
| rep-04 | g134 | 4 / none | 41,155 | 35,500 | 86.26 | 33,125 | 24,211 | 73.09 | 4883 / e3e4 | 0.557 |
| rep-04 | lambda0 | 4 / none | 43,865 | 37,741 | 86.04 | 35,157 | 25,541 | 72.65 | 6949 / e3g5 | 0.571 |
| rep-04 | lambda2 | 4 / none | 41,113 | 35,596 | 86.58 | 32,842 | 24,263 | 73.88 | 6011 / e3e4 | 0.439 |
| rep-05 | g134 | 4 / none | 10,386 | 4,562 | 43.92 | 8,268 | 5,741 | 69.44 | 10105 / f2f3 | 0.113 |
| rep-05 | lambda0 | 4 / none | 27,146 | 17,719 | 65.27 | 24,008 | 10,091 | 42.03 | 3024 / f4h5 | 0.295 |
| rep-05 | lambda2 | 4 / none | 9,557 | 3,538 | 37.02 | 7,558 | 5,791 | 76.62 | 10513 / h1g1 | 0.097 |
| rep-06 | g134 | 4 / none | 52,042 | 38,905 | 74.76 | 45,058 | 21,203 | 47.06 | -15854 / d8e8 | 0.794 |
| rep-06 | lambda0 | 4 / none | 25,559 | 15,953 | 62.42 | 21,438 | 12,865 | 60.01 | -5208 / a6b4 | 0.304 |
| rep-06 | lambda2 | 4 / none | 29,923 | 19,611 | 65.54 | 25,493 | 15,039 | 58.99 | -7085 / a6b4 | 0.328 |
| rep-07 | g134 | 4 / none | 2,132 | 210 | 9.85 | 1,346 | 691 | 51.34 | 15136 / a7h7 | 0.020 |
| rep-07 | lambda0 | 4 / none | 2,289 | 200 | 8.74 | 1,433 | 727 | 50.73 | 15301 / a7h7 | 0.012 |
| rep-07 | lambda2 | 4 / none | 2,322 | 237 | 10.21 | 1,468 | 751 | 51.16 | 14196 / a7h7 | 0.013 |
| rep-08 | g134 | 4 / none | 6,117 | 2,342 | 38.29 | 4,641 | 3,386 | 72.96 | 2363 / d2d6 | 0.044 |
| rep-08 | lambda0 | 4 / none | 5,645 | 2,295 | 40.66 | 4,262 | 2,962 | 69.50 | 4749 / d2f2 | 0.035 |
| rep-08 | lambda2 | 4 / none | 5,174 | 2,060 | 39.81 | 3,890 | 2,940 | 75.58 | 2585 / d2f2 | 0.026 |
| rep-09 | g134 | 4 / none | 11,043 | 7,627 | 69.07 | 8,595 | 5,240 | 60.97 | -7163 / f1b5 | 0.121 |
| rep-09 | lambda0 | 4 / none | 14,078 | 5,498 | 39.05 | 11,561 | 9,099 | 78.70 | -3951 / f1b5 | 0.120 |
| rep-09 | lambda2 | 4 / none | 6,490 | 3,749 | 57.77 | 4,866 | 3,780 | 77.68 | -5095 / f1b5 | 0.054 |
| rep-10 | g134 | 4 / none | 22,421 | 13,525 | 60.32 | 17,277 | 10,573 | 61.20 | 7023 / e2e4 | 0.276 |
| rep-10 | lambda0 | 4 / none | 18,732 | 11,314 | 60.40 | 14,549 | 8,642 | 59.40 | 1864 / f6d5 | 0.185 |
| rep-10 | lambda2 | 4 / none | 17,808 | 10,383 | 58.31 | 13,628 | 9,200 | 67.51 | 3063 / f6d5 | 0.164 |
| rep-11 | g134 | 4 / none | 1,904 | 1,071 | 56.25 | 1,611 | 909 | 56.42 | -5032 / h8f6 | 0.018 |
| rep-11 | lambda0 | 4 / none | 1,679 | 888 | 52.89 | 1,395 | 812 | 58.21 | -5601 / h8f6 | 0.014 |
| rep-11 | lambda2 | 4 / none | 1,764 | 944 | 53.51 | 1,472 | 916 | 62.23 | -3833 / h8f6 | 0.014 |
| rep-12 | g134 | 4 / none | 142 | 30 | 21.13 | 46 | 35 | 76.09 | 32767 / c3b2 | 0.003 |
| rep-12 | lambda0 | 4 / none | 142 | 30 | 21.13 | 46 | 35 | 76.09 | 32767 / c3b2 | 0.003 |
| rep-12 | lambda2 | 4 / none | 142 | 30 | 21.13 | 46 | 35 | 76.09 | 32767 / c3b2 | 0.002 |

| New-suite model | Total nodes | Qnodes | Q% | SP attempts | SP cutoffs | Aggregate SP% | Median position SP% |
| --- | --- | --- | --- | --- | --- | --- | --- |
| g134 | 191,101 | 129,100 | 67.56 | 155,621 | 92,465 | 59.42 | 61.08 |
| lambda0 | 182,320 | 113,666 | 62.34 | 149,239 | 93,048 | 62.35 | 62.10 |
| lambda2 | 147,779 | 91,097 | 61.64 | 118,157 | 82,627 | 69.93 | 73.45 |

λ=2 versus λ=0: qnodes **−19.86%**, total nodes **182,320→147,779 (−18.94%)**, median per-position qnode ratio **0.907658**, **8 improve / 3 regress / 1 tie**. λ=2 is also below untouched g134 in aggregate (129,100→91,097), but the primary comparison is against the stable ordinary control.

- Largest absolute/relative saving: **rep-05, 17,719→3,538**, saving **14,181 (80.03%)**, 62.83% of the suite's 22,569 net saving. Its ordinary control was already **3.88×** untouched g134's 4,562; λ=2 reduces it below g134.
- Largest absolute/relative fresh regression: **rep-06, 15,953→19,611**, **+3,658 (+22.93%)**, still below g134's 38,905. The other regressions are rep-07 **200→237 (+18.50%)** and rep-11 **888→944 (+6.31%)**. rep-12 ties at 30.
- Remove rep-05: **95,947→87,559 (−8.74%)**, median ratio **0.917713**, **7 improve / 3 regress / 1 tie**. This remains beneficial, unlike the previous fresh-03 removal's +5.98%, but falls short of the preregistered 10% reduction.
- No fresh position exceeds 2× control qnodes, newly loses depth or hits a cap. The severe-regression guard passes on its intended NEW suite.

## Preregistered search / outlier gates — FAIL

| Clause | Requirement | Observed | Verdict |
| --- | --- | --- | --- |
| 1 | Median per-position λ2/λ0 qnode ratio ≤0.90 | 0.907658 | FAIL |
| 2 | At least 60% have lower qnodes | 8/12 = 66.67% | PASS |
| 3 | Aggregate qnodes at least 15% lower | 19.86% lower | PASS |
| 4 — outlier robustness | After largest saving removed, at least 10% lower | rep-05 removed: 8.74% lower; ratio 0.912577 | FAIL |
| 5a | No more than one position >2× qnodes | 0 | PASS |
| 5b | No new completed-depth loss / cap where control finishes | 0 | PASS |

The two misses are numerically close to the limits, but closeness does not authorize a positive classification. No threshold, search fixture or outcome was changed after observing the result.

## Source/outcome distribution and stand-pat evidence

| SEARCH source | Outcome | Positions | λ=0→2 qnodes | Change | Improved / regressed / tie |
| --- | --- | --- | --- | --- | --- |
| 12 | BLACK_WIN | rep-06, rep-12 | 15,983 → 19,641 | 22.89% | 0 / 1 / 1 |
| 17 | DRAW | rep-02, rep-08 | 11,766 → 8,613 | -26.80% | 2 / 0 / 0 |
| 18 | DRAW | rep-04, rep-10 | 49,055 → 45,979 | -6.27% | 2 / 0 / 0 |
| 29 | WHITE_WIN | rep-05, rep-11 | 18,607 → 4,482 | -75.91% | 1 / 1 / 0 |
| 32 | WHITE_WIN | rep-01, rep-07 | 8,045 → 5,181 | -35.60% | 1 / 1 / 0 |
| 53 | WHITE_WIN | rep-03, rep-09 | 10,210 → 7,201 | -29.47% | 2 / 0 / 0 |

| Source-game outcome | Positions / source groups | g134 / λ=0 / λ=2 qnodes | λ2/λ0 ratio | Improved / regressed / tie |
| --- | --- | --- | --- | --- |
| WHITE_WIN | 6 / 3 | 29,811 / 36,862 / 16,864 | 0.457490 | 4 / 2 / 0 |
| BLACK_WIN | 2 / 1 | 38,935 / 15,983 / 19,641 | 1.228868 | 0 / 1 / 1 |
| DRAW | 4 / 2 | 60,354 / 60,821 / 54,592 | 0.897585 | 4 / 0 / 0 |

Five of six SEARCH source groups improve aggregate qnodes. All four drawn-game roots improve; White-win material improves strongly in aggregate with two small regressions. The single selected Black-win group **worsens 22.89%** (one regression, one tie). Novel decisive **held-out learning** therefore replicates across both winning colours, but decisive **search transfer** is not uniform. A single Black-win source cannot support a general colour-conditioned conclusion.

Aggregate SP effectiveness improves **62.35%→69.93% (+7.58 percentage points)**. Median per-position SP% rises **62.10%→73.45%**; the median paired per-position change is **+5.05 points**. SP rate improves in **9/12** positions, worsens in rep-06 and rep-09, and ties in rep-12. rep-09 still reduces qnodes despite its small SP-rate decrease. Without rep-05, aggregate SP rises **66.24%→69.47%**, accompanying the smaller remaining qnode benefit. SP success is corroborating evidence, not a substitute for the failed distribution/outlier gates.

## Continuity fixtures and root choices

| Position | Model | Depth / cap | Total nodes | Qnodes | Q% | SP attempts | SP cutoffs | SP% | Score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| middlegame-kiwipete | g134 | 4 / none | 218,790 | 209,297 | 95.66 | 180,919 | 80,896 | 44.71 | 10036 / e1g1 | 2.601 |
| middlegame-kiwipete | lambda0 | 4 / none | 91,786 | 76,376 | 83.21 | 80,712 | 52,061 | 64.50 | 10670 / a2a3 | 1.041 |
| middlegame-kiwipete | lambda2 | 4 / none | 58,535 | 47,699 | 81.49 | 51,231 | 36,400 | 71.05 | 6669 / e1g1 | 0.679 |
| quiet-fianchetto | g134 | 4 / none | 18,327 | 13,007 | 70.97 | 16,509 | 8,896 | 53.89 | -18683 / f3e5 | 0.289 |
| quiet-fianchetto | lambda0 | 4 / none | 11,564 | 4,881 | 42.21 | 9,881 | 7,984 | 80.80 | -12156 / f3e5 | 0.160 |
| quiet-fianchetto | lambda2 | 4 / none | 24,816 | 18,864 | 76.02 | 22,705 | 10,524 | 46.35 | -12867 / f3e5 | 0.303 |
| opening-start | g134 | 4 / none | 3,795 | 220 | 5.80 | 2,913 | 2,158 | 74.08 | -872 / b1c3 | 0.051 |
| opening-start | lambda0 | 4 / none | 7,541 | 1,287 | 17.07 | 6,216 | 4,314 | 69.40 | -13826 / c2c4 | 0.095 |
| opening-start | lambda2 | 4 / none | 6,600 | 802 | 12.15 | 5,340 | 3,900 | 73.03 | -371 / b1c3 | 0.075 |
| en-passant | g134 | 4 / none | 358 | 18 | 5.03 | 235 | 151 | 64.26 | 0 / e5d6 | 0.002 |
| en-passant | lambda0 | 4 / none | 392 | 11 | 2.81 | 258 | 166 | 64.34 | 0 / e5d6 | 0.002 |
| en-passant | lambda2 | 4 / none | 341 | 19 | 5.57 | 221 | 139 | 62.90 | 0 / e5d6 | 0.002 |

| Fixture | FEN |
| --- | --- |
| Kiwipete | `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1` |
| Fianchetto | `r1bq1rk1/ppp1bppp/2n2n2/3pp3/8/1P1P1NP1/PBP1PPBP/RN1Q1RK1 w - - 0 8` |
| Opening | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` |
| En-passant | `4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1` |

Kiwipete continues the favorable auxiliary direction: **76,376→47,699 (−37.55%)**, SP **64.50%→71.05%**, both depth 4, and well below untouched g134. Fianchetto reverses the previous favorable direction: **4,881→18,864 (+286.48%)**, SP **80.80%→46.35%**; λ=2 is **45.03% above untouched g134**. This is a severe continuity regression despite no cap/depth failure and a stable `f3e5` move. Opening improves **1,287→802** but remains **3.65×** untouched g134; En-passant grows **11→19**, a small absolute increase with identical drawn capture.

New-suite best moves agree on **9/12** λ=0/2 roots. Changes: rep-03 `c7c6→b7b6`, rep-04 `e3g5→e3e4`, rep-05 `f4h5→h1g1`. Largest new-suite score change is rep-05 **3024→10513 (+7489)**. rep-12's **32767 / c3b2** is the identical mate-band search return for all three models, not static saturation. There is no new-suite cap or node-accounting pathology.

Continuity choices change on Kiwipete `a2a3→e1g1` and Opening `c2c4→b1c3`; both λ=2 choices match untouched g134. Opening's **−13826→−371 (+13455)** score swing is substantial and deserves explicit instability disclosure. Fianchetto's score changes **−12156→−12867** while its qtree almost quadruples. These are native units and matched completed horizons. Changed moves are not automatically errors; move agreement, qnodes and scores are not evidence of Elo or optimal chess choices.

## Explicit answers, classification and next work-unit boundary

| Question | Answer |
| --- | --- |
| 1. Materially broader sources? | Yes: 64 deliberately distinct native opening groups; six novel search source groups versus three previously; historical TRAIN overlap falls to 5.29%. This is a stratified source distribution, not an unconditioned replication. |
| 2. Outcome distribution? | 30 White wins / 11 draws / 23 Black wins, all real completed games. |
| 3. Novel decisive holdout? | Yes: 122 ordinary observations / 82 pairs, from two White-win and two Black-win sources. |
| 4. TRAIN overlap? | 52/1627 (3.20%) with g133 replay, 52/1627 (3.20%) with first fresh corpus; 86/1627 (5.29%) with any saved historical TRAIN. |
| 5–6. Control gate and failed clauses? | PASS; none fail. One Opening >3× exception is allowed and disclosed. |
| 7. Auxiliary-held-out gate? | PASS all four clauses. Sign agreement and one source ordinary loss still worsen. |
| 8–9. Search distribution / outlier gates? | FAIL median ratio 0.907658 >0.90 and leave-largest-saving-out ratio 0.912577 >0.90. Aggregate/fraction/severe guards pass. |
| 10. Improved/regressed? | 8 improved / 3 regressed / 1 unchanged on the new suite. |
| 11. Severe regression? | None on the new suite; Fianchetto continuity worsens 3.8648× versus control, with no depth loss/cap. |
| 12. Kiwipete/Fianchetto same direction as prior positives? | Kiwipete yes; Fianchetto no, it reverses. |
| 13. Consistent on decisive and draws? | Held-out ordinary/delta aggregates improve on both; all five held-source delta medians improve. Search is mixed: White-win/drawn aggregates improve, the one Black-win search group regresses. |
| 14. Genuine independent replication? | Independent fresh-data replication of local capture-geometry learning and partial distributed qsearch benefit, with a stable control. It does not meet the preregistered definition of a positive, outlier-robust search replication. |
| 15. Production experimental integration next? | No. Primary classification is C — REPLICATION MIXED; integration remains unauthorized and not yet evidence-supported. |
| 16. Playing strength? | Unproven: no matches, Elo estimate, tactical oracle, longer campaign or strength-retention evidence. Smaller qtrees do not imply stronger play. |

**Smallest evidence-supported next diagnostic:** preregister one bounded **search-only validation of these fixed final checkpoints** on additional independent source-game groups, with decisive/drawn coverage and the same distribution/outlier thresholds, while retaining Fianchetto as a separate regression probe. Do not retrain, tune λ, calibrate or change search to repair these results. That can test whether the near-threshold distributed saving and Fianchetto reversal persist without adding another optimizer/data-update confound. New evaluation sources must be independent of the current TRAIN/HELD/selected SEARCH roots and prior diagnostics. This is a proposed next authorization, not work performed here.

If a later diagnostic supports robust transfer, a separately authorized default-off production-trainer experiment preserving λ=0, followed by controlled paired multi-generation campaigns and playing-strength comparison, could become appropriate. This C result does not authorize or recommend that integration now.

## Validation actually performed and skipped checks

Passed:

1. Direct pinned manifest/plan framing and SHA checks, exact campaign identity, canonical g134/teacher loads, exact initial model-versus-training-snapshot equality, full optimizer-state round trip, and identical starting hashes/config/step for both arms.
2. Root **2026092402**, four exact XOR-domain derived streams, original indexed game seeds, 64 unique native opening previews verified against actual games, zero seed overlap with all 134 stored generation domains and the first fresh run. Native terminal WDL/STM labels and even sample indices independently checked for every base observation.
3. Exact production 50:50 targets, legal/nonterminal capture transitions, STM/occupancy checks and shared-anchor cancellation; immutable teacher checks at preparation/training and final input hashes. No ordinary synthetic-child targets/updates.
4. Full 1,627-update pristine/manual/zero-aux complete-state equality, 128-update extra equality, 72 finite differences and eight scalar checks. Both final model/training payloads load and round-trip normally; final step 219,785.
5. Frozen source partitions and exact 12 FENs precede either training arm; all histories/first-fresh exclusions and zero current endpoint intersections verified. Fully novel decisive provenance includes real wins of both colours. Frozen input/checkpoint manifests remain byte-identical after search.
6. **39/39 existing focused tests pass**, zero failures/aborts/skips: `Brn2CoreTest`, `Brn2CodecTest`, `Brn2TrainingTargetsTest`, `TrajectorySamplerTest`, `BootstrapPartitionTest`, `Brn2SearchIntegrationTest`, `QsearchDecisionTraceTest`, `NnueScoreMappingTest`. Java 21 / JUnit Jupiter 5.10.3 / Platform 1.10.3, 13.517 seconds. This is focused coverage, not a full regression claim.
7. **48 primary real searches** plus **six trace-free equalities** (rep-01, Kiwipete, Fianchetto for both trained arms), reproducing every compared nontiming node/depth/status/move/score counter. **8,082 detached BRN static checks** exactly match fresh evaluation: 3,653 g134 / 2,471 λ=0 / 1,958 λ=2. All root restoration and trace/node/SP accounting assertions pass. Pristine search classes contain no auxiliary method (`javap` verified).
8. Full archive byte comparison finds only the declared external trainer modification; all qsearch/search/evaluation, NNUE, ordering, pruning, extensions/reductions, codec/schema and other existing archived source bytes remain original. All 543 initial authoritative file hashes and the full pre-report porcelain status remain identical; initial index empty. All 134 saved historical data files and pinned original inputs remain unchanged. No authoritative product-source change, store publication or default change.

Static source/hash inspection is distinct from actual runtime searches, codec/gradient checks and tests. No browser verification is required for this headless experiment.

One external harness compile correction changed a local collection declaration from inferred `ArrayList<Row>` to `List<Row>` before preparation could execute. It changed no protocol, data generation, target or result. The final verification-only replay mode adds Fianchetto to the existing rep-01/Kiwipete trace-free checks; it does not alter the completed primary suite. No data regeneration, gate amendment or checkpoint selection occurred.

Deliberately skipped: calibration; full/long regression suites; GUI/browser checks; extra candidate pool; multiple training generations; promotion arenas, engine tournaments and self-play strength matches; playing-strength/Elo inference; tactical-oracle acceptance; full live-store recovery/operational acceptance; release/package/build/deployment checks. They are outside the authorized isolated diagnostic or explicitly prohibited. Historical g133→g134 reproduction was not repeated because accepted byte-replay evidence plus exact current pinned identities suffice for this continuation.

Remaining uncertainties: one starting checkpoint, one root seed/order and one update schedule; source admission intentionally changes the opening distribution; only five held and six searched source clusters, with one Black-win search cluster and one drawn held cluster; positions/pairs remain correlated; one selected auxiliary capture per row; NNUE is a reference rather than chess truth; worsened delta signs, a per-source ordinary-loss exception, the Fianchetto regression and large Opening score changes; no exclusion proof for unsaved/unsampled historical exposure or every search descendant; no multi-generation/playing-strength retention. No IID significance or Elo claim is made.

The only intended authoritative-repository change is this report. Final report-only staging/diff/commit and preservation checks are recorded in the completion response; a report cannot contain its own commit SHA. No push or deployment. Work-unit/report completion does not establish user acceptance, production acceptance or operational integration.

**Human actions required after this prompt: None.** The proposed future diagnostic is a separate work-unit decision, not a blocker to completing this one.

## Reproduction and external evidence

Use Java `21+35-2513` and Python 3.13.4. Recreate a clean archive of the inspected HEAD outside the authoritative worktree. The five accepted reports retain common code: `CaptureDiagnostic`, RAW `CalibrationSearch`/`CalibrationScore`, the exact auxiliary patch, `CampaignCapture` reader, `CampaignLossChecks`, `ReplayControl`/`ReplayData`, `inspect_store.py`, `run_focused.py`, and `FreshTrain`. Reuse them as shown; do not invoke historical training or calibration entrypoints. `CampaignLossChecks` is restricted to `{2}` for finite differences, as in the fifth unit. Copy only the exact pinned input bytes. Prior fresh/replay corpora are used solely to reconstruct exclusion keys.

The exact new/modified executable sources and source-plan manifest follow. Run `setup_audit.py`; execute `FreshGenerate` once; run `partition.py`; compile the final `FreshData` and run `prepare`; run `freeze.py`; run `run_training.py`; run `analyze_held.py`; run `run_search.py control` to persist the control verdict; then `run_search.py auxiliary`, the fifth report's unchanged `analyze_search.py`, and `gates.py`. Run `SeedCheck`, `run_focused.py` and `verify_final.py`. Search uses preserved pristine classes, not classes containing the auxiliary trainer. `FreshSearch` below includes the final verification-only Fianchetto replay extension; `verify_final.py`'s patch step is unnecessary if extracting that final form directly.

All output creation for corpus/checkpoints/search uses new filenames or CREATE_NEW; do not overwrite a completed run when reproducing. Temporary evidence is retained outside the repository, not published as a production candidate. The persistent report, full preregistration, source map, hashes and source appendices provide downstream continuity without depending on conversation history.

| External artifact | Bytes | SHA-256 |
| --- | --- | --- |
| preregistration.json | 5219 | 10a6d32cd05f10ee21a68bafdbca6bc6224cefe579f6cda31267dc593d5bb731 |
| source-plan.jsonl | 19499 | d1b013e2db29d71f356e7499943d361ed71b7c7015f4d73cc7235dd966fa69f3 |
| generation-progress.jsonl | 12853 | ce5fe6ffae8e4b718cbffda8aac17252964a21f479012f4119b9995a2e36a2d5 |
| unpartitioned-base.jsonl | 473938 | f1a10a69d0008332525c41f7ef4323324d2dff711e851896dcd7039e0549406e |
| trajectory-keys.jsonl | 163959 | 9f44004eb6b16923556dd5929c03a32da6d17b7f845b1e21e95e3fb587708c41 |
| partition.json | 2898 | 72d5948d94d8b8e23b476c24d9438fe93c41bbb5d18d07fede91f774f9d7069d |
| fresh-base.bin | 153325 | 905db157705db7af83d2608074b49037b29b8cbe41dada3e15d12eb5bc89c473 |
| fresh-rows.bin | 284397 | fcfe1a52b4f90bf222357c1a7426b025d274587c232c986201f87c250a0ebd1d |
| search-suite.tsv | 739 | 9773214a0b9c18506a1b22373ca7198c3a4c0ffdde2d47290cae8fb6df13ba1e |
| search-suite.jsonl | 2232 | 61e79bec43f518531429909ef5e39e019b93fc1bb2072b0ad94708eb44f053c5 |
| freshness-summary.json | 1871 | 7a6517cf07b814dd4c63dfaca0e2492a4ae937d0332ad6fda7c0d0ae7604cdb8 |
| frozen-manifest.json | 2168 | 83b4743ba74c2ae99c015661447e33352e7cad0fcb1b52baae9961a8ae83e8e8 |
| final-checkpoints.json | 429 | 7e57c2386b53bba7339152fbe7e0cc21b25e140104e9218d7d75ec98a4653a1e |
| held-results.json | 89445 | 91c819f7937f31f08ff8f7c8383bf4d8aa6f440bb585e97a1f52536da82c1211 |
| control-audit.json | 1330 | 19b194b05e4057ea7f13d42cf9c8945febdd23c944c6fd5cec30cfee0b7119ee |
| search-results.json | 93316 | 4c6b85966dc7247c6a2da59c9bbe6b20a85176bc216e0571c45f12c21a068540 |
| search-gates.json | 2594 | 85b5c5146aa03c0a72f28a70e00b7975dec1ba3632a5ef379c4830173336d0ed |
| reconciliation.json | 469 | c697f4bd7e5ba5692ee228fb8a29bc39c9f273af0495bc0f40f88ee25deb10b2 |
| loss-checks.txt | 197 | 5acd4fa9b42940fcf6e479f83da2d592fb3ecae1cb4cc64a37ce62ed5e8e6f75 |
| seed-check.txt | 161 | 5248dcbcf2079ff16e9992996a46b058785962ec253f8ded6b6d325c5bfc907d |
| focused-tests.txt | 579 | 96b9abe6ce0ced72caefb0931cd76759d905ed97d7db303e43afdad3c7cbf0ab |
| verification.json | 877 | 651b7fc263c6231f4547caa9f0fc1e0b5125754e2fa298cfa2745b1606133cd2 |
| g134-search-all.jsonl | 1293626 | f5d0de9b212d8a07397980e421f18d4de19ae1fa008dbfd1a14096c832b4517e |
| lambda0-search-all.jsonl | 962702 | 03032c4a56507450593496381822e86c179e680ff8d514ac1db52195dbd5e76e |
| lambda2-search-all.jsonl | 816363 | 36b172f09ce50b1aa3e7bc123a778f00375b68d0afd85a4246f83a0b78bb6024 |

## Appendix A — exact preregistration

```json
{
  "utc": "2026-09-24T04:33:18.746493+00:00",
  "head": "eb46291fa350c9148218ec725bfb453870785979",
  "rootSeed": 2026092402,
  "seeds": "secondary(domain)=new SplittableRandom(rootSeed XOR domainSalt).nextLong(); indexed gameSeed remains SelfPlayRunner.gameSeed(secondary, originalCandidateIndex)",
  "salts": {
    "generation": "6A09E667F3BCC909",
    "partition": "A54FF53A5F1D36F1",
    "shuffle": "BB67AE8584CAA73B",
    "capture": "3C6EF372FE94F82B"
  },
  "sources": "From at most 4096 indexed candidates, preview only native random 0-8-ply openings via ValidationArena.opening. Select first 64 with at least 4 randomized plies and distinct endpoint board keys. Freeze all sources before playing any game. Then unchanged SelfPlayRunner handcrafted loop from standard start, native original indexed seed. Source selection is deliberately stratified, conditioning native opening distribution on 4-8 plies and unique endpoint; no outcomes/evaluators inspected for admission.",
  "generation": {
    "games": 64,
    "depth": 4,
    "threads": 6,
    "minimumOpeningPlies": 0,
    "maximumOpeningPlies": 8,
    "maximumSamplesPerGame": 32,
    "maximumPlies": 1024,
    "nodesPerMove": -1,
    "millisPerMove": -1
  },
  "partitions": "Seeded source permutation. Reserve five HELD games with both decisive colours and draw where available, using outcome and saved-key novelty only. Prefer two White wins, two Black wins, one draw, then fill missing outcome slots in permutation order. Reserve eight SEARCH games from remaining, prefer colour/draw coverage; 51 remaining games TRAIN. Each source group is one unique native opening plus its completed trajectory. Remove cross-partition endpoint leakage in evaluation; keep production TRAIN weighting. If initial pool lacks meaningful decisive novel HELD or search diversity, at most 32 additional preselected independent source games allowed solely evaluation, never TRAIN. Stop if still infeasible.",
  "novelty": "Audit all 134 historical saved training/held files; g133 replay base/pairs, all first-fresh rows/endpoints, prior diagnostic endpoints and established benchmark roots. Fully novel held requires parent, and both endpoints for delta, absent from old data and TRAIN endpoints. SEARCH roots also absent from all HELD endpoints.",
  "minimumHoldout": "At least 64 novel ordinary samples, 32 novel pairs, 32 decisive novel ordinary samples and 16 decisive novel pairs, with both decisive colours where available. Exact source IDs frozen before training.",
  "suite": "12 unique roots, six capture-rich >=3 legal captures and six quieter with exactly 1-2 legal captures. Round-robin seeded SEARCH group order; within group unsigned key ascending, at most 2 roots per group, coverage at least 6 independent sources including both decisive colours where available. Fail feasibility rather than change rule after comparative evaluation.",
  "train": "One generation-equivalent pass, epochs=1 minibatch=1 native equal weighting and Adam restored; 51 TRAIN games (nominal 1632 samples); final predetermined state. Only lambda 0 and 2.",
  "target": "0.5*terminal_WDL_STM+0.5*bounded_NNUE_STM; base 0.5*(BRN-target)^2; auxiliary predicted=-yc-yp,target=0.5*(-nc-np),Huber h=0.25; no ordinary synthetic child update.",
  "held": "Evaluate g134/lambda0/lambda2 all, novel, decisive, novel decisive, drawn, novel drawn and by source before all search. Ordinary gate uses all cross-partition-disjoint HELD; delta gate median and p95 use endpoint-novel HELD. Clear decisive reversal means >5% worsening in BOTH ordinary loss and median delta error; report tails separately.",
  "controlGate": {
    "continuityNewDepthLossOrCap": 0,
    "continuityQRatioOver3CountMax": 1,
    "newSuiteDepthLossCountMax": 1,
    "newSuiteAggregateQRatioMax": 2.0,
    "newSuiteMedianQRatioMax": 1.5,
    "ordinaryHeldLossRatioMax": 1.1
  },
  "heldGate": {
    "novelMedianDeltaErrorRatioMax": 0.9,
    "ordinaryHeldLossRatioMax": 1.05,
    "novelP95ErrorRatioMax": 1.05,
    "decisiveClearReversalAllowed": false
  },
  "searchGate": {
    "medianQRatioMax": 0.9,
    "strictImprovedFractionMin": 0.6,
    "aggregateQRatioMax": 0.85,
    "largestSavingRemovedQRatioMax": 0.9,
    "qRatioOver2CountMax": 1,
    "newDepthLossOrCap": 0
  },
  "search": "Pristine depth4 single worker fresh TT262144 singleton history nodes1000000 time60s; unchanged tracing shadow stride1 samples101/max12000. g134 then lambda0, record control gate before lambda2 searches. Both arm checkpoints and held metrics fixed first.",
  "continuity": [
    "Kiwipete",
    "Fianchetto",
    "Opening",
    "En-passant"
  ],
  "classification": "A if control fails; otherwise B if no meaningful local/search benefit; C if benefit but any held/search/outlier guard fails; D if all pass; E only all pass with comfortably distributed source/outcome improvement. No strength inference or integration in this unit.",
  "prohibited": [
    "calibration",
    "lambda tuning",
    "production trainer integration",
    "multiple training generations",
    "arenas",
    "tournaments",
    "strength matches",
    "search/NNUE/schema changes"
  ]
}
```

## Appendix B — source-group definitions and all outcomes

| Group | Native candidate index | Game seed | Opening plies / key | Partition | Result / termination | Game plies / samples |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | 0 | 8795118756059498702 | 8 / d164733aa22be510 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 76 / 32 |
| 1 | 1 | 407592333800094012 | 5 / 54e2c73693b24d12 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 97 / 32 |
| 2 | 3 | -8147466476228221866 | 6 / 1184500b9d590ac | TRAIN | DRAW / THREEFOLD_REPETITION | 68 / 32 |
| 3 | 5 | -67491231719281132 | 8 / 8b0c297a3311805c | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 47 / 32 |
| 4 | 7 | 7309213553439806501 | 7 / c4467739458afcd6 | TRAIN | DRAW / INSUFFICIENT_MATERIAL | 117 / 32 |
| 5 | 9 | 1760290264650317462 | 7 / 9c48f6c921203ac0 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 141 / 32 |
| 6 | 10 | -6719918390367324913 | 4 / a398d12980f5c352 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 105 / 32 |
| 7 | 18 | 5251266177248600959 | 6 / 467c8aa8b4b22e4f | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 251 / 32 |
| 8 | 19 | 652321338207787235 | 8 / f9fe1ad8c69cbd42 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 83 / 32 |
| 9 | 20 | 8515904387236467565 | 4 / 1234dc6e3d85e1d0 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 129 / 32 |
| 10 | 21 | -1696238650526761584 | 5 / 2238e93d1171687b | HELD | BLACK_WIN / BLACK_CHECKMATES_WHITE | 76 / 32 |
| 11 | 23 | -8543410906596625857 | 4 / 2a2a950cb5081cc5 | TRAIN | DRAW / FIFTY_MOVE_RULE | 200 / 32 |
| 12 | 26 | 8202355944306587405 | 4 / 68235c71bac11450 | SEARCH | BLACK_WIN / BLACK_CHECKMATES_WHITE | 134 / 32 |
| 13 | 27 | 4123706835742240582 | 7 / a986aea5a74859fd | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 112 / 32 |
| 14 | 29 | 6055925113083204385 | 4 / e1b940bdd377059c | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 81 / 32 |
| 15 | 32 | -5631805836789058315 | 4 / ce8f9f1d9384f4c3 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 121 / 32 |
| 16 | 33 | 187287283796771632 | 5 / b71a6868e3087228 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 108 / 32 |
| 17 | 34 | 5703457521416965859 | 5 / daf44c21ae841592 | SEARCH | DRAW / THREEFOLD_REPETITION | 105 / 32 |
| 18 | 35 | -7424196965140754017 | 7 / 1aa720911006600a | SEARCH | DRAW / THREEFOLD_REPETITION | 90 / 32 |
| 19 | 36 | -5925791896664804184 | 6 / cad95491beb6580f | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 131 / 32 |
| 20 | 37 | -8882623004834889749 | 4 / 84b7b26e2973be93 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 143 / 32 |
| 21 | 38 | 7806109215978395447 | 8 / 2061f502cf434732 | SEARCH | BLACK_WIN / BLACK_CHECKMATES_WHITE | 70 / 32 |
| 22 | 40 | -6807838894749608851 | 7 / 2f54d7fcdb81acb5 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 173 / 32 |
| 23 | 44 | 2548902907723224751 | 8 / 427612aec153abb2 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 88 / 32 |
| 24 | 45 | 2680700752027843712 | 4 / 7a0518cf2f772631 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 152 / 32 |
| 25 | 47 | 5606973685991637529 | 6 / 64b08366b6d9f245 | TRAIN | DRAW / THREEFOLD_REPETITION | 27 / 27 |
| 26 | 49 | 4288931791571196446 | 7 / 354d5bf5f4708570 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 107 / 32 |
| 27 | 50 | -7686137226003063494 | 6 / 67a4fc81bd8b394e | HELD | BLACK_WIN / BLACK_CHECKMATES_WHITE | 60 / 32 |
| 28 | 52 | -8075803904750421179 | 5 / 465179f802303fa7 | HELD | DRAW / THREEFOLD_REPETITION | 78 / 32 |
| 29 | 53 | 2049363419104935689 | 8 / 2c2dc79fc0ee195b | SEARCH | WHITE_WIN / WHITE_CHECKMATES_BLACK | 255 / 32 |
| 30 | 55 | 8362117785776201119 | 4 / fc1fa6f98d07238a | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 236 / 32 |
| 31 | 56 | 6046909947802831631 | 5 / 9f108fb04afdf1e5 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 98 / 32 |
| 32 | 57 | 6637434222554559469 | 8 / 3fdf9c36369576c4 | SEARCH | WHITE_WIN / WHITE_CHECKMATES_BLACK | 163 / 32 |
| 33 | 58 | 5055986860355356039 | 7 / 31d16f01a9d16328 | TRAIN | DRAW / FIFTY_MOVE_RULE | 264 / 32 |
| 34 | 59 | 4799397683077705963 | 5 / 9ecef126c918cdce | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 113 / 32 |
| 35 | 68 | -4466931059656449646 | 7 / ff0fa56167d17d65 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 87 / 32 |
| 36 | 69 | 8513862616265655608 | 7 / 53c241dbd3ad857d | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 52 / 32 |
| 37 | 72 | -1755084910680808011 | 7 / 51938a7359ac9ad0 | SEARCH | BLACK_WIN / BLACK_CHECKMATES_WHITE | 190 / 32 |
| 38 | 76 | -4953613250432699608 | 5 / 58232d5db7b9e964 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 200 / 32 |
| 39 | 78 | -6269731270347736450 | 4 / bd3bd30678636393 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 88 / 32 |
| 40 | 80 | -1772215783544869286 | 4 / b3483fa713570688 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 105 / 32 |
| 41 | 81 | -5776989765149700704 | 6 / f2252bd92f92cec3 | TRAIN | DRAW / INSUFFICIENT_MATERIAL | 117 / 32 |
| 42 | 83 | 8068901034760128987 | 6 / 9b165e99c2815007 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 105 / 32 |
| 43 | 88 | 6914429184408732233 | 6 / 110e1fce06490572 | TRAIN | DRAW / THREEFOLD_REPETITION | 76 / 32 |
| 44 | 89 | -7843245647121329543 | 4 / 40c4d64517ab7bd9 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 69 / 32 |
| 45 | 91 | -8052922953217036137 | 8 / ebd9266fb82016ab | TRAIN | DRAW / FIFTY_MOVE_RULE | 220 / 32 |
| 46 | 94 | -2675503637961092460 | 5 / fe049e59f9c9570e | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 117 / 32 |
| 47 | 95 | -1890881821732858000 | 6 / 52efe1a58abd0fce | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 148 / 32 |
| 48 | 96 | -1652189085055794261 | 5 / da8097916e4b758e | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 291 / 32 |
| 49 | 100 | -6142991966096605976 | 7 / 29327c121d9deeac | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 97 / 32 |
| 50 | 101 | 607414287282188785 | 4 / 77f882d7ff761101 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 359 / 32 |
| 51 | 103 | -2821719289174570344 | 6 / 9f3a5490a6c49bba | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 154 / 32 |
| 52 | 104 | 8421406440920050 | 7 / b278b3f3b6dae8b2 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 145 / 32 |
| 53 | 105 | -3747772569721974456 | 5 / 326aa0dd295351dc | SEARCH | WHITE_WIN / WHITE_CHECKMATES_BLACK | 147 / 32 |
| 54 | 106 | 4484870480202040667 | 6 / 110b8ca0d93a4e0a | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 116 / 32 |
| 55 | 107 | -7857333182838773367 | 6 / 207a6e0111aff53a | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 109 / 32 |
| 56 | 108 | -4419798757211601127 | 4 / 68ef774355437e67 | TRAIN | WHITE_WIN / WHITE_CHECKMATES_BLACK | 177 / 32 |
| 57 | 110 | -928162017316245143 | 8 / 400a75ef563c6373 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 142 / 32 |
| 58 | 115 | 7074786920030400526 | 5 / c4d3234e71c92a69 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 130 / 32 |
| 59 | 117 | -3398579062279822052 | 8 / 8565b9803c585f17 | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 204 / 32 |
| 60 | 120 | 4261759917718071546 | 8 / f2f0d92ee852455 | HELD | WHITE_WIN / WHITE_CHECKMATES_BLACK | 101 / 32 |
| 61 | 121 | -870498631292286354 | 5 / ba7c2d3b0404127a | HELD | WHITE_WIN / WHITE_CHECKMATES_BLACK | 201 / 32 |
| 62 | 122 | 7628448293077571929 | 6 / 87cd31e23d33da2b | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 90 / 32 |
| 63 | 123 | -6180400086083081256 | 5 / a69730c65e30a1bb | TRAIN | BLACK_WIN / BLACK_CHECKMATES_WHITE | 248 / 32 |

Opening FENs and history identities are exactly reconstructed by `FreshGenerate` from the indexed seeds. Full frozen source manifest:

```jsonl
{"type":"plan","utc":"2026-09-24T04:35:04.711035200Z","rootSeed":2026092402,"generationSeed":-4638214466155141245,"splitSeed":-2312974652461108083,"shuffleSeed":-1669105197141226394,"captureSeed":355928815299128698,"config":"SelfPlayConfig[games=64, depth=4, threads=6, seed=-4638214466155141245, minimumOpeningPlies=0, maximumOpeningPlies=8, maximumSamplesPerGame=32, maximumPlies=1024, scoreMapping=NnueScoreMapping[scale=32511.0], nodesPerMove=-1, millisPerMove=-1]","permutation":[27,61,60,32,28,17,10,53,18,29,12,21,37,42,56,45,55,62,58,9,57,48,11,0,14,19,35,34,13,2,30,8,20,26,7,38,40,41,47,31,6,36,3,43,39,54,15,50,44,22,59,25,24,16,5,1,63,49,52,4,33,23,51,46]}
{"type":"source","group":0,"candidateIndex":0,"gameSeed":8795118756059498702,"openingPlies":8,"openingKey":"d164733aa22be510","openingFen":"rn1qkbnr/ppp1ppp1/4b2p/3p4/2B5/NP2P3/P1PP1PPP/R1BQK1NR w KQkq - 4 5","openingIdentity":"768961fa98dfd84b9ca784364448f99c628d4e50f3d88f2abfbfe585161c5018"}
{"type":"source","group":1,"candidateIndex":1,"gameSeed":407592333800094012,"openingPlies":5,"openingKey":"54e2c73693b24d12","openingFen":"rnbqkbnr/1ppppp1p/8/p5p1/7P/1P2P3/P1PP1PP1/RNBQKBNR b KQkq - 0 3","openingIdentity":"aeab3518db0753e0ad52734005f80871787e03c4b8d2c9017efd9bb126cb33c3"}
{"type":"source","group":2,"candidateIndex":3,"gameSeed":-8147466476228221866,"openingPlies":6,"openingKey":"1184500b9d590ac","openingFen":"r1bqkbnr/p1ppp1pp/n7/1p3p2/8/2N2N1P/PPPPPPP1/R1BQKB1R w KQkq - 2 4","openingIdentity":"3d7c0bf7e7427954454a09b8ac145f4f7825f618a7f9dce28f9423b46db415bd"}
{"type":"source","group":3,"candidateIndex":5,"gameSeed":-67491231719281132,"openingPlies":8,"openingKey":"8b0c297a3311805c","openingFen":"r1bqkbnr/ppppp3/n4pp1/7p/5N2/P2P4/1PP1PPPP/RNBQKB1R w KQkq h6 0 5","openingIdentity":"51e45c805c2a04d12c52a4292f7a46e4449055a9818999d854dcea7207deba9a"}
{"type":"source","group":4,"candidateIndex":7,"gameSeed":7309213553439806501,"openingPlies":7,"openingKey":"c4467739458afcd6","openingFen":"rnbqkbnr/1p1p1ppp/4p3/pNp5/4P3/5N2/PPPP1PPP/R1BQKB1R b KQkq - 1 4","openingIdentity":"e1d05e1535098c892ede65a67edd8da5fed5dbe04023df0ac58dee4da5bf0fe2"}
{"type":"source","group":5,"candidateIndex":9,"gameSeed":1760290264650317462,"openingPlies":7,"openingKey":"9c48f6c921203ac0","openingFen":"rnbqkbnr/pp1pp1p1/2p2p1p/8/2P2PP1/1P6/P2PP2P/RNBQKBNR b KQkq - 0 4","openingIdentity":"bcd65b38439db7ae50bea745b269a8d5a493eea17394ad9921b1a0d6b810c184"}
{"type":"source","group":6,"candidateIndex":10,"gameSeed":-6719918390367324913,"openingPlies":4,"openingKey":"a398d12980f5c352","openingFen":"rnbqkbnr/ppp1ppp1/8/3p3p/8/P3P3/1PPP1PPP/RNBQKBNR w KQkq h6 0 3","openingIdentity":"177df83774c29cf47401876944275c15fabd014300171d8abc194454386bdc4b"}
{"type":"source","group":7,"candidateIndex":18,"gameSeed":5251266177248600959,"openingPlies":6,"openingKey":"467c8aa8b4b22e4f","openingFen":"rnbqk2r/ppppnppp/4p3/8/Pb6/5P1P/1PPPP1P1/RNBQKBNR w KQkq - 1 4","openingIdentity":"9552a0b515574f8f6d2f0d5e20631cce3ac3ecea19e02200aa785aacd5df6a6c"}
{"type":"source","group":8,"candidateIndex":19,"gameSeed":652321338207787235,"openingPlies":8,"openingKey":"f9fe1ad8c69cbd42","openingFen":"rn1qkbnr/pppbppp1/3p4/8/3P3p/P2Q3P/1PP1PPP1/RNB1KBNR w KQkq - 0 5","openingIdentity":"d4b61ea0bf76dc9d3d2eb016a2e6b7c7db582655e9418176a972197815f6b2b4"}
{"type":"source","group":9,"candidateIndex":20,"gameSeed":8515904387236467565,"openingPlies":4,"openingKey":"1234dc6e3d85e1d0","openingFen":"rnbqkbnr/1ppppppp/8/8/p4P2/P7/1PPPP1PP/RNBQKBNR w KQkq - 0 3","openingIdentity":"8aae6d4ed4aaeb1e25d9541bbde4b3ccdaf29f7cc8b456d6838896b5d091bb66"}
{"type":"source","group":10,"candidateIndex":21,"gameSeed":-1696238650526761584,"openingPlies":5,"openingKey":"2238e93d1171687b","openingFen":"r1bqkbnr/1ppppppp/p1Q5/8/8/2P5/PP1PPPPP/RNB1KBNR b KQkq - 0 3","openingIdentity":"39ae24b50260027fec78ca32b00fba74339e9c5d3feebc3b2a53d4219b02eeaf"}
{"type":"source","group":11,"candidateIndex":23,"gameSeed":-8543410906596625857,"openingPlies":4,"openingKey":"2a2a950cb5081cc5","openingFen":"rnbqkbnr/pppp1pp1/4p3/7p/8/3P2P1/PPP1PP1P/RNBQKBNR w KQkq h6 0 3","openingIdentity":"4e12236fd85117129eded75be1301304e6344f224822feeacdc04cf96248b487"}
{"type":"source","group":12,"candidateIndex":26,"gameSeed":8202355944306587405,"openingPlies":4,"openingKey":"68235c71bac11450","openingFen":"rnbqkb1r/p1pppppp/7n/1p6/5P2/4P3/PPPP2PP/RNBQKBNR w KQkq b6 0 3","openingIdentity":"88568b463ba9d639a8d10584a8d5fcfdaafa1b7f88da73bddc6c05aff00799f0"}
{"type":"source","group":13,"candidateIndex":27,"gameSeed":4123706835742240582,"openingPlies":7,"openingKey":"a986aea5a74859fd","openingFen":"rnbqkb1r/p1pppppp/1p6/8/4PPn1/8/PPPPN1PP/RNBQKB1R b KQkq - 0 4","openingIdentity":"628d71c74407b75d55883f7a27a9ba84415d125139532235fa96ce4ca3f5fb84"}
{"type":"source","group":14,"candidateIndex":29,"gameSeed":6055925113083204385,"openingPlies":4,"openingKey":"e1b940bdd377059c","openingFen":"rnbqkb1r/p1pppppp/1p5n/8/8/5N1P/PPPPPPP1/RNBQKB1R w KQkq - 2 3","openingIdentity":"612702b8dcf42825aff0742d95342132b67276220e8c8f425ece1040f9d7c84b"}
{"type":"source","group":15,"candidateIndex":32,"gameSeed":-5631805836789058315,"openingPlies":4,"openingKey":"ce8f9f1d9384f4c3","openingFen":"rnbqkb1r/1ppppppp/7n/p7/4P3/7N/PPPP1PPP/RNBQKB1R w KQkq - 1 3","openingIdentity":"0ccf8d927c90a9606b1b91e6e6f91d811e396b9b274586391eb6adee33f134d1"}
{"type":"source","group":16,"candidateIndex":33,"gameSeed":187287283796771632,"openingPlies":5,"openingKey":"b71a6868e3087228","openingFen":"1nbqkbnr/rppppppp/p7/8/3P4/8/PPP1PPPP/RNBQKBNR b KQk d3 0 3","openingIdentity":"23b67668a6d3cbbebd0f73ccbeaf382d18928fc7096304efd48b71588fb77d19"}
{"type":"source","group":17,"candidateIndex":34,"gameSeed":5703457521416965859,"openingPlies":5,"openingKey":"daf44c21ae841592","openingFen":"r1bqkbnr/p1pppppp/1pn5/8/2P3P1/7B/PP1PPP1P/RNBQK1NR b KQkq - 2 3","openingIdentity":"8afc57315472c8a3217497cfc2a00bd6033f043a766766e0493f3113b5e176f4"}
{"type":"source","group":18,"candidateIndex":35,"gameSeed":-7424196965140754017,"openingPlies":7,"openingKey":"1aa720911006600a","openingFen":"1nbqkbnr/r1pppppp/pp6/7P/8/2NP4/PPP1PPP1/R1BQKBNR b KQk - 0 4","openingIdentity":"487c76c1540f04f6a13bfcb8f1160f8665aafd24204cccb07f1d8dcdce5eb53d"}
{"type":"source","group":19,"candidateIndex":36,"gameSeed":-5925791896664804184,"openingPlies":6,"openingKey":"cad95491beb6580f","openingFen":"r1bqkbnr/pp1pp1pp/n7/2p2p2/PP6/8/2PPPPPP/RNBQKBNR w KQkq c6 0 4","openingIdentity":"beb707a844da1b343dfa4d209c70e116794aaa672f908cda28efb83bf37f7c28"}
{"type":"source","group":20,"candidateIndex":37,"gameSeed":-8882623004834889749,"openingPlies":4,"openingKey":"84b7b26e2973be93","openingFen":"rnbqkbnr/pppp2pp/5p2/4p3/1PP5/8/P2PPPPP/RNBQKBNR w KQkq e6 0 3","openingIdentity":"a2634f87fd88e261c904c7cea9bffa411d481952731d6b208f68b14c5c96cd82"}
{"type":"source","group":21,"candidateIndex":38,"gameSeed":7806109215978395447,"openingPlies":8,"openingKey":"2061f502cf434732","openingFen":"rnbqkb1r/pp1pp1pp/2p2p2/5n2/8/PP4PN/2PPPP1P/RNBQKB1R w KQkq - 0 5","openingIdentity":"6c2234c4141f62455e5cf88d50eca613485e2ad2c426b63f376bb9db49372312"}
{"type":"source","group":22,"candidateIndex":40,"gameSeed":-6807838894749608851,"openingPlies":7,"openingKey":"2f54d7fcdb81acb5","openingFen":"rnb1kbnr/pppqppp1/7p/3p4/2P2P2/1P1P4/P3P1PP/RNBQKBNR b KQkq - 0 4","openingIdentity":"a2d5a2b54d44a7bb6bd063c2b7e403b88c697069b7bc47baf785d188093e47b8"}
{"type":"source","group":23,"candidateIndex":44,"gameSeed":2548902907723224751,"openingPlies":8,"openingKey":"427612aec153abb2","openingFen":"1nbqkbnr/rp1p1ppp/8/p1p1p3/P1P5/1PN5/3PPPPP/R1BQKBNR w KQk - 1 5","openingIdentity":"2d8dec75ec8af17ba556636d3759f8503381cc1d48122d7a8ac6c77a5d7babcd"}
{"type":"source","group":24,"candidateIndex":45,"gameSeed":2680700752027843712,"openingPlies":4,"openingKey":"7a0518cf2f772631","openingFen":"rnbqkbnr/1pppppp1/7p/p7/8/3P1N2/PPP1PPPP/RNBQKB1R w KQkq a6 0 3","openingIdentity":"394d29634692968e5eed7e4084f4784f74d0e1e2850ffe91b5213ec8132b9497"}
{"type":"source","group":25,"candidateIndex":47,"gameSeed":5606973685991637529,"openingPlies":6,"openingKey":"64b08366b6d9f245","openingFen":"rnb1kbnr/ppppqpp1/7p/4p3/6P1/5P1B/PPPPP2P/RNBQK1NR w KQkq - 1 4","openingIdentity":"837fc9a35a192e28d3ed719c8f94208f9a7f2df9f0f10974da52871cb24a9dde"}
{"type":"source","group":26,"candidateIndex":49,"gameSeed":4288931791571196446,"openingPlies":7,"openingKey":"354d5bf5f4708570","openingFen":"rnbqkb1r/pppppppp/3n4/8/1PP5/8/P2PPPPP/RNBQKBNR b KQkq - 2 4","openingIdentity":"7ea838f9eee049665327ff85a048808c5b07775a365acdd9564376fab388edf1"}
{"type":"source","group":27,"candidateIndex":50,"gameSeed":-7686137226003063494,"openingPlies":6,"openingKey":"67a4fc81bd8b394e","openingFen":"r1bqkbnr/ppppp1p1/2n2p1p/8/PP1P4/8/2P1PPPP/RNBQKBNR w KQkq - 1 4","openingIdentity":"10d084c59dcb32176ea2223b08bf79641b40d907955e20dbddfa9976f8c4ad8e"}
{"type":"source","group":28,"candidateIndex":52,"gameSeed":-8075803904750421179,"openingPlies":5,"openingKey":"465179f802303fa7","openingFen":"rnbqkbnr/ppp1p1pp/3p4/5p2/8/P4NP1/1PPPPP1P/RNBQKB1R b KQkq - 0 3","openingIdentity":"922bd22efd3fb2e8b97d14c73c5c279cef6f0364fa85b598629297802f65cd9b"}
{"type":"source","group":29,"candidateIndex":53,"gameSeed":2049363419104935689,"openingPlies":8,"openingKey":"2c2dc79fc0ee195b","openingFen":"rnbqkbnr/2p1pppp/8/pp1p4/3P1N2/P7/1PP1PPPP/RNBQKB1R w KQkq - 0 5","openingIdentity":"2f1f0796356378e63bf8f50369ec4dc254b86ae221772fc7f11259aaf3ce7418"}
{"type":"source","group":30,"candidateIndex":55,"gameSeed":8362117785776201119,"openingPlies":4,"openingKey":"fc1fa6f98d07238a","openingFen":"rnbqkbnr/p2ppppp/8/1pp5/2P5/4P3/PP1P1PPP/RNBQKBNR w KQkq c6 0 3","openingIdentity":"9d11ea121c95ec79f90a68403f1aa719d44111cd2dc38acf998c2d1b0c4f9ab2"}
{"type":"source","group":31,"candidateIndex":56,"gameSeed":6046909947802831631,"openingPlies":5,"openingKey":"9f108fb04afdf1e5","openingFen":"rnbqkbnr/pp1ppppp/8/8/2p5/2N4N/PPPPPPPP/R1BQKBR1 b Qkq - 1 3","openingIdentity":"0993df7d2769ccff85b904d482504b142fc4785b82c5480edd1fbdc01d9a9b58"}
{"type":"source","group":32,"candidateIndex":57,"gameSeed":6637434222554559469,"openingPlies":8,"openingKey":"3fdf9c36369576c4","openingFen":"rnb2bnr/pp1kppp1/1q6/2p4p/P7/4P3/1PPP1PPP/RNBQK1NR w KQ - 0 5","openingIdentity":"c1e98cd0c9172a3978a16d24ec9c84f6171948bc21a1ad2aae7bf33e2a657637"}
{"type":"source","group":33,"candidateIndex":58,"gameSeed":5055986860355356039,"openingPlies":7,"openingKey":"31d16f01a9d16328","openingFen":"rnbqkb1r/pppppppp/7n/5P2/8/P5P1/1PPPP2P/RNBQKBNR b KQkq - 0 4","openingIdentity":"4bd639bebfc50119fb6f495fc1e09e1d2ef52ecc43c8cb6c5717449e0a3f3581"}
{"type":"source","group":34,"candidateIndex":59,"gameSeed":4799397683077705963,"openingPlies":5,"openingKey":"9ecef126c918cdce","openingFen":"r1bqkbnr/pp1ppppp/n7/2p5/1P3P2/2N5/P1PPP1PP/R1BQKBNR b KQkq - 2 3","openingIdentity":"25c5476a0f8f329ac63869318308f61bfe1374c0070c35225f3eef0fc1a5e376"}
{"type":"source","group":35,"candidateIndex":68,"gameSeed":-4466931059656449646,"openingPlies":7,"openingKey":"ff0fa56167d17d65","openingFen":"1rbqkbnr/ppp1pppp/2np4/8/3P4/2P5/PP2PPPP/RNBQKBNR b KQk d3 0 4","openingIdentity":"b3460d3e7cf8085d2d9cb5ac0ed1d3be23e86fab1ac3cdd9ffc6c2154cd19f58"}
{"type":"source","group":36,"candidateIndex":69,"gameSeed":8513862616265655608,"openingPlies":7,"openingKey":"53c241dbd3ad857d","openingFen":"rnbqkb1r/ppp2ppp/4pn2/3p4/4P2P/8/PPPP1PP1/RNBQKBNR b KQkq - 1 4","openingIdentity":"dd07557f4a9c77417c4950b045146e7255cdf134aee5f5cdad5e6b0c10e112e4"}
{"type":"source","group":37,"candidateIndex":72,"gameSeed":-1755084910680808011,"openingPlies":7,"openingKey":"51938a7359ac9ad0","openingFen":"r1bqkbnr/1ppppppp/8/p1n5/6P1/1P5B/P1PPPP1P/RNBQK1NR b KQkq - 2 4","openingIdentity":"8f5d0baf778509b73f38757f810187091b69fdde4b9a6ecb0a07f9e8e1b95c99"}
{"type":"source","group":38,"candidateIndex":76,"gameSeed":-4953613250432699608,"openingPlies":5,"openingKey":"58232d5db7b9e964","openingFen":"rnbqkbnr/2pppppp/8/pp6/4P1P1/7N/PPPP1P1P/RNBQKB1R b KQkq - 1 3","openingIdentity":"e003c85c07d8805a74eb40aeb2b5c64dd6b288cfe2c5d73d91bdb8a54624a758"}
{"type":"source","group":39,"candidateIndex":78,"gameSeed":-6269731270347736450,"openingPlies":4,"openingKey":"bd3bd30678636393","openingFen":"rnbqkbnr/1ppppp1p/p5p1/8/1P3P2/8/P1PPP1PP/RNBQKBNR w KQkq - 0 3","openingIdentity":"40fc1073e5d4553c8d56c8efad83b3c62b27416b99089949623960292377ad0c"}
{"type":"source","group":40,"candidateIndex":80,"gameSeed":-1772215783544869286,"openingPlies":4,"openingKey":"b3483fa713570688","openingFen":"rnbqkbnr/1ppppp1p/8/p5p1/8/5NP1/PPPPPP1P/RNBQKB1R w KQkq a6 0 3","openingIdentity":"7d44f19341e751ce445fe8033abcfde1a86ca04cc993990eb8d4bd8cf6d93162"}
{"type":"source","group":41,"candidateIndex":81,"gameSeed":-5776989765149700704,"openingPlies":6,"openingKey":"f2252bd92f92cec3","openingFen":"rn1qkbnr/p1p1pppp/b2p4/1p6/P7/R4P2/1PPPP1PP/1NBQKBNR w Kkq - 2 4","openingIdentity":"c289f65d5b81b812c5b1ee042d1d558359cb8e3d878618ce836a0b36af7eae7d"}
{"type":"source","group":42,"candidateIndex":83,"gameSeed":8068901034760128987,"openingPlies":6,"openingKey":"9b165e99c2815007","openingFen":"rnbq1b1r/ppppkppp/4pn2/6P1/8/5P2/PPPPP2P/RNBQKBNR w KQ - 1 4","openingIdentity":"debf2708d7cc1660d5ae9b16b9f566279f6196a21b76c5f45d1f6c0615ca95fb"}
{"type":"source","group":43,"candidateIndex":88,"gameSeed":6914429184408732233,"openingPlies":6,"openingKey":"110e1fce06490572","openingFen":"rnbqkbnr/p2pppp1/2p5/1p5p/8/BP3P2/P1PPP1PP/RN1QKBNR w KQkq b6 0 4","openingIdentity":"4b2e6d29703154bb87b749531dc4415f2ff82ab072ec35753f9e4b3c3aecb4a8"}
{"type":"source","group":44,"candidateIndex":89,"gameSeed":-7843245647121329543,"openingPlies":4,"openingKey":"40c4d64517ab7bd9","openingFen":"rnbqkbnr/pppppppp/8/8/4P3/6P1/PPPP1P1P/RNBQKBNR w KQkq - 1 3","openingIdentity":"74d7303d561cf8b417a80880718f4540854ad00ccc807dbc5ff0a9458ee2d073"}
{"type":"source","group":45,"candidateIndex":91,"gameSeed":-8052922953217036137,"openingPlies":8,"openingKey":"ebd9266fb82016ab","openingFen":"r1bqkb1r/pppp1pp1/n3pn2/7p/3P1P2/2P5/PP1NP1PP/R1BQKBNR w KQkq - 0 5","openingIdentity":"8077670e1a4b7360b00e88aeed6db5ead386d2f7e4ffc7837df20545099401c8"}
{"type":"source","group":46,"candidateIndex":94,"gameSeed":-2675503637961092460,"openingPlies":5,"openingKey":"fe049e59f9c9570e","openingFen":"1nbqkbnr/rppppppp/p7/8/2PP4/1P6/P3PPPP/RNBQKBNR b KQk d3 0 3","openingIdentity":"375b81e4034caa9dfa882c3b4d9bb6d0d07af058e07502d3c247f8028d8e8ad8"}
{"type":"source","group":47,"candidateIndex":95,"gameSeed":-1890881821732858000,"openingPlies":6,"openingKey":"52efe1a58abd0fce","openingFen":"rn1qkbnr/p1pppp1p/bp6/6p1/1P6/3P1P2/P1P1P1PP/RNBQKBNR w KQkq - 1 4","openingIdentity":"67ec950077686dba76a3f5ceded7b7c96497d8363eca0bc0a0749628f8d9b690"}
{"type":"source","group":48,"candidateIndex":96,"gameSeed":-1652189085055794261,"openingPlies":5,"openingKey":"da8097916e4b758e","openingFen":"rnbqkbnr/p1pp1ppp/4p3/1p6/1P1P4/8/PBP1PPPP/RN1QKBNR b KQkq - 1 3","openingIdentity":"4a25aa4bb7f352e1ddcf3f82bf93de5633f281ac18dd5d6ae6ffc85d3e7e9b64"}
{"type":"source","group":49,"candidateIndex":100,"gameSeed":-6142991966096605976,"openingPlies":7,"openingKey":"29327c121d9deeac","openingFen":"rnbqkbnr/2p1pppp/8/pP1p4/4PP2/8/1PPP2PP/RNBQKBNR b KQkq e3 0 4","openingIdentity":"8dea7fe669cc751a11c5e7fc8fb91ddab576c4ef6327de5ef891c66ca6000221"}
{"type":"source","group":50,"candidateIndex":101,"gameSeed":607414287282188785,"openingPlies":4,"openingKey":"77f882d7ff761101","openingFen":"rnbqkb1r/1ppppppp/5n2/p7/3P3P/8/PPP1PPP1/RNBQKBNR w KQkq - 1 3","openingIdentity":"cc3c6ce6a5dbb040dff2d057cf9492371eb751511c85d506aa874b8cc8ac5742"}
{"type":"source","group":51,"candidateIndex":103,"gameSeed":-2821719289174570344,"openingPlies":6,"openingKey":"9f3a5490a6c49bba","openingFen":"rnbqkbnr/ppp2pp1/8/3pp2p/8/N2P4/PPP1PPPP/1RBQKBNR w Kkq h6 0 4","openingIdentity":"e26db6b3b90d6896c282650cf3cfdfa5279bce06988145770825a23d2300e138"}
{"type":"source","group":52,"candidateIndex":104,"gameSeed":8421406440920050,"openingPlies":7,"openingKey":"b278b3f3b6dae8b2","openingFen":"rnb1kbnr/pp1pppp1/1q6/2p4p/3P4/6PB/PPPKPP1P/RNBQ2NR b kq - 3 4","openingIdentity":"d35351ef09f724859e7611c2806c88535c6b0b533195a050907f52e60c68d00f"}
{"type":"source","group":53,"candidateIndex":105,"gameSeed":-3747772569721974456,"openingPlies":5,"openingKey":"326aa0dd295351dc","openingFen":"rn1qkbnr/pppbpppp/3p4/8/7P/N4P2/PPPPP1P1/R1BQKBNR b KQkq - 2 3","openingIdentity":"bebee550e6d122ceb971c2a4d05217a5a9f5f8b99ee794101bf23865eadb9985"}
{"type":"source","group":54,"candidateIndex":106,"gameSeed":4484870480202040667,"openingPlies":6,"openingKey":"110b8ca0d93a4e0a","openingFen":"rnbqkbnr/pp2pp1p/3p4/2p3p1/4N3/2P5/PP1PPPPP/R1BQKBNR w KQkq - 0 4","openingIdentity":"65af55e79c3022364317aaf0ef22cd8ba7b991d3559b0b7b31155a2319270dc5"}
{"type":"source","group":55,"candidateIndex":107,"gameSeed":-7857333182838773367,"openingPlies":6,"openingKey":"207a6e0111aff53a","openingFen":"rnb1kbnr/1pppqppp/p7/4p3/P1P4P/8/1P1PPPP1/RNBQKBNR w KQkq - 1 4","openingIdentity":"922ee1c3d3f51849958a31b24fe21c9b75e471e9b87aef156f40620feb2d2831"}
{"type":"source","group":56,"candidateIndex":108,"gameSeed":-4419798757211601127,"openingPlies":4,"openingKey":"68ef774355437e67","openingFen":"rnbq1bnr/pppkpppp/3p4/8/4P3/1P6/P1PP1PPP/RNBQKBNR w KQ - 1 3","openingIdentity":"88a168ceb668ddde3072533373a0f30f053e27860b4413581b5aaa7643edf117"}
{"type":"source","group":57,"candidateIndex":110,"gameSeed":-928162017316245143,"openingPlies":8,"openingKey":"400a75ef563c6373","openingFen":"rnbqk1nr/ppppp1b1/5p2/6pp/P7/2N4N/1PPPPPPP/R1BQKB1R w KQkq - 1 5","openingIdentity":"1e426f68df7631d9da34760eeac759acc5830cb0fd9285c6a2a1b3e67ccb7996"}
{"type":"source","group":58,"candidateIndex":115,"gameSeed":7074786920030400526,"openingPlies":5,"openingKey":"c4d3234e71c92a69","openingFen":"rnbqkbnr/pppp1pp1/4p3/7p/3P4/2N5/PPP1PPPP/1RBQKBNR b Kkq d3 0 3","openingIdentity":"6e203286049921c0b36cd4939754547e7f6b9177a5be2f391329852a8a83c3ae"}
{"type":"source","group":59,"candidateIndex":117,"gameSeed":-3398579062279822052,"openingPlies":8,"openingKey":"8565b9803c585f17","openingFen":"1nbqkbnr/rpppppp1/7p/1p6/2PP4/8/PP2PPPP/R1BQKBNR w KQk - 0 5","openingIdentity":"2c86e233aa1edb0863bf42598245b614a5927855d27922161fdbbf3a3a3a1e2f"}
{"type":"source","group":60,"candidateIndex":120,"gameSeed":4261759917718071546,"openingPlies":8,"openingKey":"f2f0d92ee852455","openingFen":"r1bqk1nr/pp1p1ppp/n1p5/4p3/P1P5/b5PP/1P1PPP2/RNBQKBNR w KQkq - 0 5","openingIdentity":"7181b3e35bd280e6d1541b84f9b25e2cd5c94f17eb00ea944d280cc35ad176f2"}
{"type":"source","group":61,"candidateIndex":121,"gameSeed":-870498631292286354,"openingPlies":5,"openingKey":"ba7c2d3b0404127a","openingFen":"rnbqkbnr/pp1ppp1p/2p3p1/8/5P2/1PP5/P2PP1PP/RNBQKBNR b KQkq f3 0 3","openingIdentity":"24325980a1bcbd85d5f6ff549512ab3e1ca4973787ff45327e935b40b78d2975"}
{"type":"source","group":62,"candidateIndex":122,"gameSeed":7628448293077571929,"openingPlies":6,"openingKey":"87cd31e23d33da2b","openingFen":"rnbqkbnr/ppp2ppp/4p3/8/3p1PPP/8/PPPPP3/RNBQKBNR w KQkq - 0 4","openingIdentity":"6551b74bf6cff57f17adc4598844559da076042f98bb0f8b1f7317a5194dabf2"}
{"type":"source","group":63,"candidateIndex":123,"gameSeed":-6180400086083081256,"openingPlies":5,"openingKey":"a69730c65e30a1bb","openingFen":"rnbqkbnr/1ppppp1p/p5p1/8/P6P/8/1PPPPPP1/RNBQKBNR b KQkq - 0 3","openingIdentity":"65f38837800aecebcca8126f9b98ff960f25b5e98a2a408bc0e1cc989bcf89da"}
```

## Appendix C — new/modified external executable sources

### FreshGenerate.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class FreshGenerate {
 static final long ROOT=2026092402L;
 static long seed(long salt){return new SplittableRandom(ROOT^salt).nextLong();}
 static final long GEN_SEED=seed(0x6A09E667F3BCC909L),SPLIT_SEED=seed(0xA54FF53A5F1D36F1L),SHUFFLE_SEED=seed(0xBB67AE8584CAA73BL),CAPTURE_SEED=seed(0x3C6EF372FE94F82BL);
 public static void main(String[] a)throws Exception {
  Path d=Path.of(a[0]);var config=new SelfPlayConfig(64,4,6,GEN_SEED,0,8,32,1024,NnueScoreMapping.V1,-1,-1);
  var openingConfig=new ValidationConfig(64,GEN_SEED,0,8,4,6,NnueScoreMapping.V1,1024);
  var ids=new ArrayList<Integer>();var roots=new ArrayList<ValidationArena.Opening>();var keys=new HashSet<Long>();long[] start=Board.startingPosition();
  for(int candidate=0;candidate<4096&&ids.size()<64;candidate++){
   var o=ValidationArena.opening(start,GameHistory.initial(start),openingConfig,candidate);
   if(o.randomizedPlies()>=4&&new HeadlessGame(o.board(),1024).active()&&keys.add(o.board()[5])){ids.add(candidate);roots.add(o);}
  }
  FreshData.require(ids.size()==64,"Cannot achieve 64 native opening groups");
  var permutation=new ArrayList<Integer>();for(int i=0;i<64;i++)permutation.add(i);var pr=new SplittableRandom(SPLIT_SEED);for(int i=63;i>0;i--)Collections.swap(permutation,i,pr.nextInt(i+1));
  try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve("source-plan.jsonl"),StandardOpenOption.CREATE_NEW))){
   write(out,"type","plan","utc",java.time.Instant.now().toString(),"rootSeed",ROOT,"generationSeed",GEN_SEED,"splitSeed",SPLIT_SEED,"shuffleSeed",SHUFFLE_SEED,"captureSeed",CAPTURE_SEED,"config",config.toString(),"permutation",permutation);
   for(int i=0;i<64;i++){var o=roots.get(i);write(out,"type","source","group",i,"candidateIndex",ids.get(i),"gameSeed",SelfPlayRunner.gameSeed(GEN_SEED,ids.get(i)),"openingPlies",o.randomizedPlies(),"openingKey",Long.toUnsignedString(o.board()[5],16),"openingFen",FreshData.fen(o.board()),"openingIdentity",o.identity());}
  }
  long began=System.nanoTime();int total=0;
  try(var progress=new PrintWriter(Files.newBufferedWriter(d.resolve("generation-progress.jsonl"),StandardOpenOption.CREATE_NEW));var rows=new PrintWriter(Files.newBufferedWriter(d.resolve("unpartitioned-base.jsonl"),StandardOpenOption.CREATE_NEW));var trajectories=new PrintWriter(Files.newBufferedWriter(d.resolve("trajectory-keys.jsonl"),StandardOpenOption.CREATE_NEW))){
   for(int i=0;i<64;i++){
    var game=SelfPlayRunner.play(SearchEvaluation.handcrafted(),config,ids.get(i),start,new SelfPlayControl());
    FreshData.require(game.termination().completed(),"Incomplete source "+i+" "+game.termination()+" "+game.failure());
    var samples=TrajectorySampler.sample(game,32);var ix=TrajectorySampler.indexes(game.positions().size(),32);
    FreshData.require(Arrays.equals(game.positions().get(roots.get(i).randomizedPlies()).board(),roots.get(i).board()),"Native opening preview mismatch");
    for(int j=0;j<samples.size();j++){var s=samples.get(j);write(rows,"game",i,"candidateIndex",ids.get(i),"gameSeed",SelfPlayRunner.gameSeed(GEN_SEED,ids.get(i)),"sample",j,"ply",ix[j],"key",Long.toUnsignedString(s.board()[5],16),"board",Arrays.stream(s.board()).boxed().toList(),"wdl",s.target());} rows.flush();total+=samples.size();
    write(trajectories,"game",i,"keys",game.positions().stream().map(p->Long.toUnsignedString(p.board()[5],16)).toList());trajectories.flush();
    write(progress,"game",i,"candidateIndex",ids.get(i),"seed",SelfPlayRunner.gameSeed(GEN_SEED,ids.get(i)),"termination",game.termination().toString(),"result",game.termination().result().orElseThrow().toString(),"plies",game.playedPlies(),"rawPositions",game.positions().size(),"samples",samples.size(),"utc",java.time.Instant.now().toString());progress.flush();
    System.out.println("source="+i+" candidate="+ids.get(i)+" "+game.termination()+" samples="+samples.size());
   }
  }
  System.out.println("COMPLETE 64 distinct source groups, samples="+total+" elapsedSeconds="+(System.nanoTime()-began)/1e9);
 }
}
```

### FreshData.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.*;import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.brn2.*;import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;import com.ohinteractive.seedv6.training.service.BrnSupervision;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class FreshData {
 public record Row(String split,int game,int index,int ply,long[] p,double w,double tp,double t,long move,long[] c,double tc,boolean novel,boolean novelPair,int captures){
  boolean pair(){return c!=null;}double delta(){return .5*(-tc-tp);}TrajectorySampler.Sample sample(){return new TrajectorySampler.Sample(p,w);}
 }
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 static double teacher(NnueEvaluator t,long[] b){t.evaluate(b);return t.boundedValue();}
 static Set<Long> keys(Path p)throws Exception{var out=new HashSet<Long>();for(String s:Files.readAllLines(p))if(!s.isBlank())out.add(Long.parseUnsignedLong(s,16));return out;}
 static Set<Long> endpoints(List<Row> rows){var out=new HashSet<Long>();for(var r:rows){out.add(r.p[5]);if(r.pair())out.add(r.c[5]);}return out;}
 static String fen(long[] b){StringBuilder s=new StringBuilder();String pieces=" KQRBNP  kqrbnp";for(int rank=7;rank>=0;rank--){int empty=0;for(int file=0;file<8;file++){int square=rank*8+file,p=0;for(int j=0;j<4;j++)p|=((b[j]>>>square)&1)<<j;if((p&7)==0){empty++;continue;}if(empty>0){s.append(empty);empty=0;}s.append(pieces.charAt(p));}if(empty>0)s.append(empty);if(rank>0)s.append('/');}int st=(int)b[4];s.append(Board.player(st)==0?" w ":" b ");int cast=(st>>>1)&15;if(cast==0)s.append('-');else for(int j=0;j<4;j++)if((cast&(1<<j))!=0)s.append("KQkq".charAt(j));int ep=Board.enPassantSquare(st);s.append(' ').append(ep<0?"-":"abcdefgh".charAt(ep%8)+Integer.toString(ep/8+1));s.append(' ').append(Board.halfMoveClock(st)).append(' ').append(Board.fullMoveNumber(st));String f=s.toString();require(Arrays.equals(b,Board.fromFen(f)),"FEN roundtrip");return f;}
 static void writeRow(DataOutputStream o,Row r)throws Exception{o.writeUTF(r.split);o.writeInt(r.game);o.writeInt(r.index);o.writeInt(r.ply);for(long b:r.p)o.writeLong(b);o.writeDouble(r.w);o.writeDouble(r.tp);o.writeDouble(r.t);o.writeLong(r.move);o.writeBoolean(r.pair());if(r.pair()){for(long b:r.c)o.writeLong(b);o.writeDouble(r.tc);}o.writeBoolean(r.novel);o.writeBoolean(r.novelPair);o.writeInt(r.captures);}
 static List<Row> read(Path p)throws Exception{var out=new ArrayList<Row>();try(var in=new DataInputStream(Files.newInputStream(p))){int n=in.readInt();for(int i=0;i<n;i++){String s=in.readUTF();int g=in.readInt(),j=in.readInt(),ply=in.readInt();long[] b=new long[6];for(int k=0;k<6;k++)b[k]=in.readLong();double w=in.readDouble(),tp=in.readDouble(),t=in.readDouble();long m=in.readLong();long[] c=null;double tc=0;if(in.readBoolean()){c=new long[6];for(int k=0;k<6;k++)c[k]=in.readLong();tc=in.readDouble();}out.add(new Row(s,g,j,ply,b,w,tp,t,m,c,tc,in.readBoolean(),in.readBoolean(),in.readInt()));}require(in.read()==-1,"Trailing rows");}return out;}
 static void prepare(Path d)throws Exception{
  byte[] tb=Files.readAllBytes(d.resolve("teacher.nnue"));var net=NnueNetworkCodec.decode(tb);var nn=new NnueEvaluator(net);var rng=new SplittableRandom(FreshGenerate.CAPTURE_SEED);List<Row> rows=new ArrayList<Row>();int terminalExclusions=0;
  try(var in=new DataInputStream(Files.newInputStream(d.resolve("fresh-base.bin")))){int n=in.readInt();for(int i=0;i<n;i++){
   String split=in.readUTF();int g=in.readInt(),j=in.readInt(),ply=in.readInt();long[] p=new long[6];for(int k=0;k<6;k++)p[k]=in.readLong();double w=in.readDouble(),tp=teacher(nn,p),t=BrnSupervision.blended(.5).targets(nn).applyAsDouble(new TrajectorySampler.Sample(p,w));require(t==.5*w+.5*tp,"Campaign target");fen(p);
   var moves=new ArrayList<Long>();int caps=0;if(new HeadlessGame(p,2).active())for(long m:CaptureDiagnostic.legal(p))if(CaptureDiagnostic.capture(p,m)){caps++;long[] c=CaptureDiagnostic.play(p,m);if(new HeadlessGame(c,2).active())moves.add(m);else terminalExclusions++;}
   long move=0;long[] c=null;double tc=0;if(!moves.isEmpty()){move=moves.get(rng.nextInt(moves.size()));c=CaptureDiagnostic.play(p,move);var game=new HeadlessGame(p,2);game.play(move);require(Arrays.equals(c,game.boardSnapshot()),"Legal transition");require(Long.bitCount(p[0]|p[1]|p[2])==1+Long.bitCount(c[0]|c[1]|c[2]),"Capture occupancy");require(Board.player((int)p[4])!=Board.player((int)c[4]),"STM flip");tc=teacher(nn,c);require(tc==teacher(nn,c),"Teacher determinism");require(Math.abs(-(-.5*w+.5*tc)-t-.5*(-tc-tp))<3e-16,"Shared WDL cancellation");fen(c);}
   rows.add(new Row(split,g,j,ply,p,w,tp,t,move,c,tc,false,false,caps));
  }require(in.read()==-1,"Trailing raw rows");}
  var history=keys(d.resolve("history-all-keys.txt"));history.addAll(keys(d.resolve("previous-diagnostic-keys.txt")));for(String f:Files.readAllLines(d.resolve("banned-fens.txt")))if(!f.isBlank())history.add(Board.fromFen(f)[5]);var trainKeys=endpoints(rows.stream().filter(r->r.split.equals("TRAIN")).toList());var blocked=new HashSet<>(history);blocked.addAll(trainKeys);var marked=new ArrayList<Row>();
  for(var r:rows){boolean novel=!blocked.contains(r.p[5]);boolean np=novel&&r.pair()&&!blocked.contains(r.c[5]);marked.add(new Row(r.split,r.game,r.index,r.ply,r.p,r.w,r.tp,r.t,r.move,r.c,r.tc,novel,np,r.captures));}rows=marked;
  var heldKeys=endpoints(rows.stream().filter(r->r.split.equals("HELD")).toList());blocked.addAll(heldKeys);
  var search=rows.stream().filter(r->r.split.equals("SEARCH")&&!blocked.contains(r.p[5])&&new HeadlessGame(r.p,2).active()).sorted((a,b)->Long.compareUnsigned(a.p[5],b.p[5])).toList();
  System.out.println("Eligible unseen search roots="+search.size()+" perGame="+search.stream().collect(java.util.stream.Collectors.groupingBy(Row::game,java.util.stream.Collectors.counting()))+" captures="+search.stream().collect(java.util.stream.Collectors.groupingBy(Row::captures,java.util.stream.Collectors.counting())));System.out.println("Novel held="+rows.stream().filter(r->r.split.equals("HELD")&&r.novel).count()+" pairs="+rows.stream().filter(r->r.split.equals("HELD")&&r.novelPair).count());
  var ids=Files.readAllLines(d.resolve("partition-search-order.txt")).stream().map(Integer::parseInt).toList();var selected=new ArrayList<Row>();var counts=new HashMap<Integer,Integer>();var selectedKeys=new HashSet<Long>();
  for(boolean rich:new boolean[]{true,false}){int count=0;for(int round=0;round<2&&count<6;round++)for(int game:ids){if(count==6)break;if(counts.getOrDefault(game,0)>=2)continue;var found=search.stream().filter(r->r.game==game&&!selectedKeys.contains(r.p[5])&&(rich?r.captures>=3:r.captures>=1&&r.captures<=2)).findFirst();if(found.isPresent()){var r=found.get();selected.add(r);selectedKeys.add(r.p[5]);counts.merge(game,1,Integer::sum);count++;}}require(count==6,"Insufficient rich/quiet novel search positions: "+count);}
  require(selected.size()==12&&counts.size()>=6,"Suite size/source coverage");require(rows.stream().filter(r->r.split.equals("HELD")&&r.novel).count()>=64,"Insufficient novel holdout");require(rows.stream().filter(r->r.split.equals("HELD")&&r.novelPair).count()>=32,"Insufficient novel held pairs");
  var heldSeen=new HashSet<Long>();int before=rows.size();rows=rows.stream().filter(r->!r.split.equals("HELD")||(!trainKeys.contains(r.p[5])&&(!r.pair()||!trainKeys.contains(r.c[5]))&&heldSeen.add(r.p[5]))).toList();
  require(rows.stream().filter(r->r.split.equals("HELD")&&r.novel&&r.w!=0).count()>=32,"Insufficient decisive novel held ordinary");
  require(rows.stream().filter(r->r.split.equals("HELD")&&r.novelPair&&r.w!=0).count()>=16,"Insufficient decisive novel held pairs");
  System.out.println("Removed cross-TRAIN/duplicate HELD rows="+(before-rows.size()));
  try(var o=new DataOutputStream(Files.newOutputStream(d.resolve("fresh-rows.bin"),StandardOpenOption.CREATE_NEW));var json=new PrintWriter(Files.newBufferedWriter(d.resolve("fresh-rows.jsonl"),StandardOpenOption.CREATE_NEW))){o.writeInt(rows.size());for(var r:rows){writeRow(o,r);write(json,"split",r.split,"game",r.game,"sample",r.index,"ply",r.ply,"key",Long.toUnsignedString(r.p[5],16),"fen",fen(r.p),"wdl",r.w,"tp",r.tp,"target",r.t,"move",r.pair()?Move.coordinate(r.move):null,"childKey",r.pair()?Long.toUnsignedString(r.c[5],16):null,"tc",r.pair()?r.tc:null,"delta",r.pair()?r.delta():null,"novel",r.novel,"novelPair",r.novelPair,"captures",r.captures);}}
  try(var suite=new PrintWriter(Files.newBufferedWriter(d.resolve("search-suite.tsv"),StandardOpenOption.CREATE_NEW));var json=new PrintWriter(Files.newBufferedWriter(d.resolve("search-suite.jsonl"),StandardOpenOption.CREATE_NEW))){for(int i=0;i<selected.size();i++){var r=selected.get(i);String name=String.format("rep-%02d",i+1);suite.println(name+"\t"+fen(r.p));write(json,"id",name,"game",r.game,"sample",r.index,"ply",r.ply,"key",Long.toUnsignedString(r.p[5],16),"captures",r.captures,"fen",fen(r.p),"utc",java.time.Instant.now().toString());}}
  // Prior checker's pair format, containing only TRAIN and strictly novel HELD pairs.
  var checkRows=rows.stream().filter(r->r.split.equals("TRAIN")||r.split.equals("HELD")&&r.novelPair).toList();try(var o=new DataOutputStream(Files.newOutputStream(d.resolve("check-pairs.bin"),StandardOpenOption.CREATE_NEW))){o.writeInt(checkRows.size());for(var r:checkRows){o.writeBoolean(!r.split.equals("TRAIN"));o.writeInt(r.index);for(long b:r.p)o.writeLong(b);o.writeDouble(r.w);o.writeDouble(r.tp);o.writeDouble(r.t);o.writeLong(r.move);o.writeBoolean(r.pair());if(r.pair()){for(long b:r.c)o.writeLong(b);o.writeDouble(r.tc);}}}
  require(Arrays.equals(tb,NnueNetworkCodec.encode(net)),"Teacher mutation");System.out.println("FROZEN rows="+rows.size()+" train="+rows.stream().filter(r->r.split.equals("TRAIN")).count()+" heldNovel="+rows.stream().filter(r->r.split.equals("HELD")&&r.novel).count()+" heldNovelPairs="+rows.stream().filter(r->r.split.equals("HELD")&&r.novelPair).count()+" suite="+selected.size()+" games="+counts+" excludedTerminalChildren="+terminalExclusions);
 }
 static void predictions(Path d,String name)throws Exception{var rows=read(d.resolve("fresh-rows.bin"));var model=Brn2Codec.decodeModel(Files.readAllBytes(d.resolve(name+".brn2")));var ws=new Brn2Workspace();try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve(name+"-predictions.jsonl"),StandardOpenOption.CREATE_NEW))){for(var r:rows)if(!r.split.equals("SEARCH"))write(out,"split",r.split,"game",r.game,"sample",r.index,"novel",r.novel,"novelPair",r.novelPair,"wdl",r.w,"tp",r.tp,"target",r.t,"yp",model.evaluate(r.p,ws),"yc",r.pair()?model.evaluate(r.c,ws):null,"delta",r.pair()?r.delta():null);}}
 public static void main(String[] a)throws Exception{if(a[1].equals("prepare"))prepare(Path.of(a[0]));else predictions(Path.of(a[0]),a[1]);}
}
```

### FreshSearch.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.brn2.*;import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class FreshSearch {
 public static void main(String[] a)throws Exception{
  Path d=Path.of(a[0]);String name=a[1],mode=a.length>2?a[2]:"all";var bp=d.resolve(name+".brn2");var np=d.resolve("teacher.nnue");
  CalibrationSearch.label=name;CalibrationSearch.variant="RAW";CalibrationSearch.model=Brn2Codec.decodeModel(Files.readAllBytes(bp));CalibrationSearch.bd=SearchEvaluation.brn2(CalibrationSearch.model);CalibrationSearch.nd=SearchEvaluation.incremental(NnueNetworkCodec.decode(Files.readAllBytes(np)));CalibrationSearch.fresh=new Brn2Accumulator(CalibrationSearch.model);CalibrationSearch.ns=CalibrationSearch.nd.newState(1);
  try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve(name+"-search-"+mode+".jsonl"),StandardOpenOption.CREATE_NEW))){CalibrationSearch.out=out;write(out,"type","run","model",name,"modelHash",CaptureDiagnostic.sha(bp),"suiteHash",CaptureDiagnostic.sha(d.resolve("search-suite.tsv")),"utc",java.time.Instant.now().toString());
   var suite=new ArrayList<String[]>();for(var line:Files.readAllLines(d.resolve("search-suite.tsv")))suite.add(line.split("\t"));for(int i:new int[]{0,4,5,3})suite.add(CaptureDiagnostic.POSITIONS[i]);
   if(mode.equals("all"))for(var p:suite){CalibrationSearch.search(p,true,true,4);out.flush();}
   else if(mode.equals("replay"))for(var p:List.of(suite.getFirst(),CaptureDiagnostic.POSITIONS[0],CaptureDiagnostic.POSITIONS[4])){CalibrationSearch.search(p,true,false,4);out.flush();}
   else throw new IllegalArgumentException(mode);
   write(out,"type","end","checks",CalibrationSearch.checks,"modelHashAfter",CaptureDiagnostic.sha(bp),"teacherHashAfter",CaptureDiagnostic.sha(np),"utc",java.time.Instant.now().toString());
  }
 }
}
```

### SeedCheck.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.nio.file.*;import java.util.*;import java.util.regex.*;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayRunner;
public class SeedCheck {
 public static void main(String[] a)throws Exception {
  Path d=Path.of(a[0]);if(FreshGenerate.ROOT!=2026092402L)throw new AssertionError("root");
  long[] salts={0x6A09E667F3BCC909L,0xA54FF53A5F1D36F1L,0xBB67AE8584CAA73BL,0x3C6EF372FE94F82BL};
  long[] expected={-4638214466155141245L,-2312974652461108083L,-1669105197141226394L,355928815299128698L};
  for(int i=0;i<4;i++)if(new SplittableRandom(2026092402L^salts[i]).nextLong()!=expected[i])throw new AssertionError("derived stream");
  var old=new HashSet<Long>();for(String line:Files.readAllLines(d.resolve("historical-generation-seeds.txt")))for(int i=0;i<64;i++)old.add(SelfPlayRunner.gameSeed(Long.parseLong(line),i));
  for(int i=0;i<64;i++)old.add(SelfPlayRunner.gameSeed(-8204741532265061904L,i));
  int n=0;var current=new HashSet<Long>();var pattern=Pattern.compile("\"candidateIndex\":(\\d+),\"gameSeed\":(-?\\d+)");
  for(String line:Files.readAllLines(d.resolve("source-plan.jsonl"))){var m=pattern.matcher(line);if(m.find()){int index=Integer.parseInt(m.group(1));long seed=Long.parseLong(m.group(2));if(seed!=SelfPlayRunner.gameSeed(FreshGenerate.GEN_SEED,index)||old.contains(seed)||!current.add(seed))throw new AssertionError("seed overlap");n++;}}
  if(n!=64)throw new AssertionError("sources");System.out.println("PASS root=2026092402; four deterministic XOR-domain streams; 64 unique indexed game seeds; zero overlap with 134 stored generations and first fresh generation.");
 }
}
```

### setup_audit.py

```python
from pathlib import Path
import sys,json,hashlib,subprocess,shutil
from inspect_store import frame,plan,manifest,STORE
D=Path(__file__).parent
P=D.parent/'seedv6-brn-fresh-generation-20260924'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
history=set();train=set();hashes={};g133=set();plans=[]
for p in sorted((STORE/'bootstrap').glob('*.data')):
 k,r,h=frame(p);assert k=='brn-bootstrap-data-v1';ph=r.utf();r.fmt('q')
 pl=plan(p.with_suffix('.plan'));assert ph==pl['sha256'];plans.append(pl)
 for split in ['train','held']:
  for _ in range(r.fmt('i')):
   b=[r.fmt('q') for _ in range(6)];w=r.fmt('d');assert w in (-1,0,1)
   key=format(b[5]&((1<<64)-1),'x');history.add(key)
   if split=='train':train.add(key)
   if pl['generation']==134:g133.add(key)
 hashes[str(p)]=h
assert len(hashes)==134
for fn,keys in [('history-all-keys.txt',history),('history-train-keys.txt',train),('g133-base-keys.txt',g133)]: (D/fn).write_text('\n'.join(sorted(keys))+'\n')
assert sha(P/'fresh-rows.bin')=='5f805f70d4e0d701d607e8d9d50c52ca644ac4e7c0bfce79f4e112726d5e38d9'
oldrows=[json.loads(l) for l in (P/'fresh-rows.jsonl').read_text().splitlines()]
oldkeys={r['key'] for r in oldrows};oldend=oldkeys|{r['childKey'] for r in oldrows if r['childKey']}
prior=set((P/'previous-diagnostic-keys.txt').read_text().split())
for fn,keys in [('previous-fresh-base-keys.txt',oldkeys),('previous-fresh-endpoint-keys.txt',oldend),('previous-diagnostic-keys.txt',prior|oldend)]: (D/fn).write_text('\n'.join(sorted(keys))+'\n')
shutil.copyfile(P/'historical-pairs.jsonl',D/'historical-pairs.jsonl')
(D/'history-data-hashes.json').write_text(json.dumps(hashes,indent=2))
manifest_path=next((STORE/'checkpoints').glob('g000134*/manifest.bin'));m=manifest(manifest_path)
pl=next(x for x in plans if x['generation']==134)
assert pl['source']=='HANDCRAFTED' and pl['supervision']=='NNUE_BLENDED' and pl['teacherWeight']==.5
(D/'lineage.json').write_text(json.dumps({'manifest':m,'plan':pl,'historyFiles':len(hashes),'historyKeys':len(history),'trainKeys':len(train),'g133Keys':len(g133),'previousFreshBaseKeys':len(oldkeys)},indent=2))
print('Verified 134 historical data frames/plans, pinned manifest and exact 50:50 campaign; prior fresh keys exclusion-only.')
src=D/'repo/app/src/main/java';cls=D/'pristine-classes';cls.mkdir(exist_ok=True)
names=['FreshGenerate','FreshData','FreshTrain','CaptureDiagnostic','FreshSearch','CalibrationSearch','CalibrationScore']
subprocess.run(['javac','-encoding','UTF-8','-d',str(cls),'-sourcepath',str(src)]+[str(D/(n+'.java')) for n in names],check=True)
print('Pristine diagnostic compilation PASS')
```

### partition.py

```python
from pathlib import Path
import json,struct,collections,datetime,re
D=Path(__file__).parent
def load(name):return [json.loads(l) for l in (D/name).read_text().splitlines()]
rows=load('unpartitioned-base.jsonl');games=load('generation-progress.jsonl');sources=load('source-plan.jsonl');order=sources[0]['permutation']
assert len(games)==64 and len({s['openingKey'] for s in sources[1:]})==64
old=set((D/'history-all-keys.txt').read_text().split())|set((D/'previous-diagnostic-keys.txt').read_text().split())
by={g:[r for r in rows if r['game']==g] for g in order};outcome={g['game']:g['result'] for g in games}
novel={g:sum(r['key'] not in old for r in by[g]) for g in order}
held=[]
for result in ['WHITE_WIN','WHITE_WIN','BLACK_WIN','BLACK_WIN','DRAW']:
 choices=[g for g in order if g not in held and outcome[g]==result and novel[g]>=16]
 if choices:held.append(choices[0])
for g in order:
 if len(held)==5:break
 if g not in held and novel[g]>=16:held.append(g)
search=[]
for result in ['WHITE_WIN','BLACK_WIN','DRAW']:
 choices=[g for g in order if g not in held+search and outcome[g]==result and novel[g]>=16]
 if choices:search.append(choices[0])
for g in order:
 if len(search)==8:break
 if g not in held+search and novel[g]>=16:search.append(g)
train=[g for g in order if g not in held+search]
assert len(held)==5 and len(search)==8 and len(train)==51
assert {'WHITE_WIN','BLACK_WIN'}<=set(outcome[g] for g in held)
with (D/'fresh-base.bin').open('xb') as f,(D/'fresh-base.jsonl').open('x') as j:
 f.write(struct.pack('>i',len(rows)))
 for r in rows:
  g=r['game'];split='HELD' if g in held else 'SEARCH' if g in search else 'TRAIN';b=r['board']
  # Independently verify terminal WDL orientation and production sampler.
  result=0 if outcome[g]=='DRAW' else 1 if outcome[g]=='WHITE_WIN' else -1
  assert r['wdl']==result*(-1 if b[4]&1 else 1)
  n=games[g]['rawPositions'];count=min(n,32);assert r['ply']==r['sample']*(n-1)//(count-1)
  sb=split.encode();f.write(struct.pack('>H',len(sb))+sb+struct.pack('>iii6qd',g,r['sample'],r['ply'],*b,r['wdl']))
  j.write(json.dumps(dict(r,split=split))+'\n')
summary=dict(utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),train=train,held=held,search=search,selectionOrder=order,novelSavedKeyCounts=novel,
 games=64,samples=len(rows),outcomes=dict(collections.Counter(outcome.values())),
 partitionCounts={s:{'games':len(ids),'samples':sum(len(by[g]) for g in ids),'outcomes':dict(collections.Counter(outcome[g] for g in ids))} for s,ids in [('TRAIN',train),('HELD',held),('SEARCH',search)]})
(D/'partition.json').write_text(json.dumps(summary,indent=2)+'\n')
# Exclude named old diagnostic roots, not only the immediately preceding eight.
fens=set();pattern=r'[rnbqkpRNBQKP1-8]+(?:/[rnbqkpRNBQKP1-8]+){7} [wb] [KQkq-]+ (?:[a-h][36]|-) \d+ \d+'
for p in list((D/'repo').glob('BRN*.md'))+list((D/'repo/app/src/main/java/com/ohinteractive/seedv6/tools').rglob('*.java')):
 fens.update(re.findall(pattern,p.read_text(encoding='utf-8')))
(D/'banned-fens.txt').write_text('\n'.join(sorted(fens))+'\n')
print(json.dumps({k:v for k,v in summary.items() if k not in ['selectionOrder','novelSavedKeyCounts']},indent=2))
```

### freeze.py

```python
from pathlib import Path
import json,hashlib,collections,datetime,re
from inspect_store import plan,STORE
D=Path(__file__).parent
def load(f):return [json.loads(l) for l in (D/f).read_text().splitlines()]
def keys(f):return set((D/f).read_text().split())
raw=load('fresh-base.jsonl');rows=load('fresh-rows.jsonl');suite=load('search-suite.jsonl');games=load('generation-progress.jsonl');sources=load('source-plan.jsonl')
parts={s:[r for r in rows if r['split']==s] for s in ['TRAIN','HELD','SEARCH']}
end=lambda rr:{r['key'] for r in rr}|{r['childKey'] for r in rr if r['childKey']}
te=end(parts['TRAIN']);he=end(parts['HELD']);sk={r['key'] for r in suite}
history=keys('history-all-keys.txt');old=keys('previous-diagnostic-keys.txt');assert not te&he and not sk&(te|he|history|old)
assert len(sk)==12 and len({r['game'] for r in suite})>=6
assert sum(r['captures']>=3 for r in suite)==6 and sum(1<=r['captures']<=2 for r in suite)==6
for r in parts['HELD']:
 assert r['novel']==(r['key'] not in history|old|te)
 assert r['novelPair']==(r['novel'] and r['childKey'] is not None and r['childKey'] not in history|old|te)
assert len({r['key'] for r in parts['HELD']})==len(parts['HELD'])
byoutcome={g['game']:g['result'] for g in games}
nh=[r for r in parts['HELD'] if r['novel']];np=[r for r in nh if r['novelPair']]
assert len(nh)>=64 and len(np)>=32 and sum(r['wdl']!=0 for r in nh)>=32 and sum(r['wdl']!=0 for r in np)>=16
assert {'WHITE_WIN','BLACK_WIN'}<=set(byoutcome[r['game']] for r in nh)
assert {'WHITE_WIN','BLACK_WIN'}<=set(byoutcome[r['game']] for r in suite)
references={n:keys(f) for n,f in [('g133','g133-base-keys.txt'),('historicalTraining','history-train-keys.txt'),('historicalAll','history-all-keys.txt'),('previousFresh','previous-fresh-base-keys.txt')]}
overlap={}
for s in parts:
 rr=[r for r in raw if r['split']==s];overlap[s]={'n':len(rr),'uniqueKeys':len({r['key'] for r in rr})}
 for label,kk in references.items():overlap[s][label]={'count':sum(r['key'] in kk for r in rr),'pct':100*sum(r['key'] in kk for r in rr)/len(rr)}
allhistSeeds=[]
for p in (STORE/'bootstrap').glob('*.plan'):
 pl=plan(p);allhistSeeds.append(int(re.search(r'SelfPlayConfig\[.*?seed=(-?\d+)',pl['settings']).group(1)))
# Java independently checks indexed source seeds in the focused SeedCheck.
(D/'historical-generation-seeds.txt').write_text('\n'.join(map(str,allhistSeeds))+'\n')
summary=dict(utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),overlap=overlap,held=dict(all=len(parts['HELD']),novel=len(nh),novelPairs=len(np),novelDecisive=sum(r['wdl']!=0 for r in nh),novelDecisivePairs=sum(r['wdl']!=0 for r in np),novelByOutcome=dict(collections.Counter(byoutcome[r['game']] for r in nh))),
 suiteGames=sorted({r['game'] for r in suite}),suiteOutcomes=dict(collections.Counter(byoutcome[r['game']] for r in suite)),crossTrainHeldEndpointIntersection=len(te&he),crossSearchBlockedIntersection=len(sk&(te|he|history|old)),sourceOpeningCounts=dict(collections.Counter(s['openingPlies'] for s in sources[1:])),
 trajectories=len({hashlib.sha256(json.dumps(r['keys']).encode()).hexdigest() for r in load('trajectory-keys.jsonl')}))
(D/'freshness-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
files=['preregistration.json','source-plan.jsonl','generation-progress.jsonl','unpartitioned-base.jsonl','trajectory-keys.jsonl','partition.json','fresh-base.bin','fresh-base.jsonl','fresh-rows.bin','fresh-rows.jsonl','search-suite.tsv','search-suite.jsonl','check-pairs.bin','banned-fens.txt','history-all-keys.txt','history-train-keys.txt','previous-diagnostic-keys.txt','historical-generation-seeds.txt','partition-search-order.txt','g134.brn2','g134.state','teacher.nnue']
manifest={'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sha256':{f:hashlib.sha256((D/f).read_bytes()).hexdigest() for f in files}}
(D/'frozen-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps(summary,indent=2))
```

### run_training.py

```python
from pathlib import Path
import subprocess,hashlib,json,datetime
D=Path(__file__).parent
def run(args,log):
 with (D/log).open('x') as out:subprocess.run(args,stdout=out,stderr=subprocess.STDOUT,check=True)
 print((D/log).read_text(),flush=True)
def java(cls,main,*args):return ['java','-Xmx1536m','-cp',str(D/cls),'com.ohinteractive.seedv6.'+main,*map(str,args)]
run(java('pristine-classes','tools.search.FreshTrain',D,'control'),'control-console.txt')
subprocess.run(['python',str(D/'patch_auxiliary.py')],check=True)
trainer=Path('app/src/main/java/com/ohinteractive/seedv6/core/brn2/Brn2Trainer.java')
prior=D.parent/'seedv6-brn-fresh-generation-20260924/repo'/trainer
assert (D/'repo'/trainer).read_bytes()==prior.read_bytes(), 'Exact accepted auxiliary trainer required'
aux=D/'aux-classes';aux.mkdir()
names=['FreshGenerate','FreshData','FreshTrain','CaptureDiagnostic','CampaignCapture','CampaignLossChecks','ReplayControl','ReplayData']
subprocess.run(['javac','-encoding','UTF-8','-d',str(aux),'-sourcepath',str(D/'repo/app/src/main/java')]+[str(D/(n+'.java')) for n in names],check=True)
run(java('aux-classes','core.brn2.CampaignLossChecks',D/'g134.state',D/'check-pairs.bin'),'loss-checks.txt')
run(java('aux-classes','tools.search.FreshTrain',D,'0check'),'zero-check-console.txt')
run(java('aux-classes','tools.search.FreshTrain',D,'2'),'lambda2-console.txt')
files=['lambda0.brn2','lambda0.state','lambda2.brn2','lambda2.state']
freeze={'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sha256':{f:hashlib.sha256((D/f).read_bytes()).hexdigest() for f in files}}
(D/'final-checkpoints.json').write_text(json.dumps(freeze,indent=2)+'\n')
for name in ['g134','lambda0','lambda2']:run(java('pristine-classes','tools.search.FreshData',D,name),name+'-held-console.txt')
print('Both final checkpoints fixed; held predictions complete; no search yet.',flush=True)
```

### analyze_held.py

```python
from pathlib import Path
import json,statistics,math,datetime
D=Path(__file__).parent
def quant(a,p):
 a=sorted(a);v=(len(a)-1)*p;i=int(v);return a[i]+(a[min(i+1,len(a)-1)]-a[i])*(v-i)
def dist(a):return {'mean':statistics.mean(a),'sd':statistics.pstdev(a),'p05':quant(a,.05),'p50':quant(a,.5),'p90':quant(a,.9),'p95':quant(a,.95),'absP50':quant(list(map(abs,a)),.5),'absP95':quant(list(map(abs,a)),.95)}
def metrics(rows):
 result={'n':len(rows),'games':sorted({r['game'] for r in rows})}
 for name,k in [('baseLoss','target'),('wdlLoss','wdl'),('nnueLoss','tp')]:result[name]=statistics.mean(.5*(r['yp']-r[k])**2 for r in rows)
 result['score']=dist([r['yp'] for r in rows]);return result
def pairs(rows):
 e=[-r['yc']-r['yp']-r['delta'] for r in rows];p=[-r['yc']-r['yp'] for r in rows];t=[r['delta'] for r in rows]
 return {'n':len(rows),'games':sorted({r['game'] for r in rows}),'errorP50':quant(list(map(abs,e)),.5),'errorP90':quant(list(map(abs,e)),.9),'errorP95':quant(list(map(abs,e)),.95),'huber':statistics.mean(.5*v*v if abs(v)<=.25 else .25*(abs(v)-.125) for v in e),'largeErrorPct':100*sum(abs(v)>.5 for v in e)/len(e),'signAgreementPct':100*sum((x>0)-(x<0)==(y>0)-(y<0) for x,y in zip(p,t))/len(p),'rawDelta':dist(p),'targetDelta':dist(t),'signedError':dist(e),'meanCenteredAbsP50':quant([abs(v-statistics.mean(e)) for v in e],.5)}
allresults={}
for model in ['g134','lambda0','lambda2']:
 rows=[json.loads(l) for l in (D/f'{model}-predictions.jsonl').read_text().splitlines()]
 held=[r for r in rows if r['split']=='HELD']
 def report(rr,novel=False):
  pp=[r for r in rr if r['yc'] is not None and (not novel or r['novelPair'])]
  return {'ordinary':metrics(rr) if rr else None,'capture':pairs(pp) if pp else None}
 subsets={'all':held,'novel':[r for r in held if r['novel']], 'decisive':[r for r in held if r['wdl']!=0], 'novelDecisive':[r for r in held if r['novel'] and r['wdl']!=0], 'drawn':[r for r in held if r['wdl']==0], 'novelDrawn':[r for r in held if r['novel'] and r['wdl']==0], 'overlapping':[r for r in held if not r['novel']]}
 allresults[model]={label:report(rr,label.startswith('novel')) for label,rr in subsets.items()}
 allresults[model]['train']=report([r for r in rows if r['split']=='TRAIN'])
 allresults[model]['perGame']={str(g):report([r for r in subsets['novel'] if r['game']==g],True) for g in sorted({r['game'] for r in held})}
a=allresults['lambda0'];b=allresults['lambda2']
gates=[]
def gate(clause,observed,limit,passed):gates.append(dict(clause=clause,observed=observed,limit=limit,pass_=passed))
x=b['novel']['capture']['errorP50']/a['novel']['capture']['errorP50'];gate('H1 novel median ratio',x,.9,x<=.9)
x=b['all']['ordinary']['baseLoss']/a['all']['ordinary']['baseLoss'];gate('H2 ordinary held loss ratio',x,1.05,x<=1.05)
x=b['novel']['capture']['errorP95']/a['novel']['capture']['errorP95'];gate('H3 novel p95 error ratio',x,1.05,x<=1.05)
x={s:{'ordinaryRatio':b[s]['ordinary']['baseLoss']/a[s]['ordinary']['baseLoss'],'medianRatio':b[s]['capture']['errorP50']/a[s]['capture']['errorP50']} for s in ['decisive','novelDecisive']}
gate('H4 decisive reversal',x,'No >5% worsening in both metrics',not any(v['ordinaryRatio']>1.05 and v['medianRatio']>1.05 for v in x.values()))
result={'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'results':allresults,'gates':gates,'passed':all(g['pass_'] for g in gates)}
(D/'held-results.json').write_text(json.dumps(result,indent=2)+'\n')
for m,r in allresults.items():
 print(m,{label:dict(n=r[label]['ordinary']['n'],loss=r[label]['ordinary']['baseLoss'],pairs=r[label]['capture']['n'],median=r[label]['capture']['errorP50'],p95=r[label]['capture']['errorP95']) for label in ['all','novel','decisive','novelDecisive','drawn']})
print('HELD GATES',json.dumps(gates))
```

### control_audit.py

```python
from pathlib import Path
import json,statistics,datetime
D=Path(__file__).parent
def load(name):
 rows=[json.loads(l) for l in (D/f'{name}-search-all.jsonl').read_text().splitlines()];assert rows[-1]['type']=='end'
 return {r['position']:r for r in rows if r['type']=='search'}
a=load('g134');b=load('lambda0');names=[json.loads(l)['id'] for l in (D/'search-suite.jsonl').read_text().splitlines()]
continuity=['middlegame-kiwipete','quiet-fianchetto','opening-start','en-passant'];held=json.loads((D/'held-results.json').read_text())['results'];g=[]
def gate(clause,value,limit,passed,detail=None):g.append(dict(clause=clause,observed=value,limit=limit,pass_=passed,detail=detail))
bad=[n for n in continuity if a[n]['completedDepth']==4 and a[n]['status']=='COMPLETED' and (b[n]['completedDepth']<4 or b[n]['status']!='COMPLETED')];gate('C1 continuity new cap/depth loss',len(bad),0,not bad,bad)
bad=[n for n in continuity if b[n]['q']>3*a[n]['q']];gate('C2 continuity >3x qnodes count',len(bad),1,len(bad)<2,{n:b[n]['q']/a[n]['q'] for n in continuity})
bad=[n for n in names if b[n]['completedDepth']<a[n]['completedDepth']];gate('C3a new-suite depth losses',len(bad),1,len(bad)<=1,bad)
x=sum(b[n]['q'] for n in names)/sum(a[n]['q'] for n in names);gate('C3b new-suite aggregate qnode ratio',x,2,x<=2)
x=statistics.median(b[n]['q']/a[n]['q'] for n in names);gate('C3c new-suite median qnode ratio',x,1.5,x<=1.5)
x=held['lambda0']['all']['ordinary']['baseLoss']/held['g134']['all']['ordinary']['baseLoss'];gate('C4 ordinary held-out loss ratio',x,1.1,x<=1.1)
result={'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'pass':all(x['pass_'] for x in g),'gates':g,'lambda2SearchExists':(D/'lambda2-search-all.jsonl').exists(),'suiteQ':{m:sum(r[n]['q'] for n in names) for m,r in [('g134',a),('lambda0',b)]}}
assert not result['lambda2SearchExists']
(D/'control-audit.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
```

### run_search.py

```python
from pathlib import Path
import subprocess,sys,json,hashlib
D=Path(__file__).parent
for manifest in ['frozen-manifest.json','final-checkpoints.json']:
 for name,sha in json.loads((D/manifest).read_text())['sha256'].items():assert hashlib.sha256((D/name).read_bytes()).hexdigest()==sha
assert (D/'held-results.json').exists()
models=['g134','lambda0'] if sys.argv[1]=='control' else ['lambda2']
if sys.argv[1]!='control':assert (D/'control-audit.json').exists()
for name in models:
 with (D/(name+'-search-console.txt')).open('x') as out:
  subprocess.run(['java','-Xmx1024m','-cp',str(D/'pristine-classes'),'com.ohinteractive.seedv6.tools.search.FreshSearch',str(D),name],stdout=out,stderr=subprocess.STDOUT,check=True)
 print((D/(name+'-search-console.txt')).read_text(),flush=True)
if sys.argv[1]=='control':subprocess.run(['python',str(D/'control_audit.py')],check=True)
```

### gates.py

```python
from pathlib import Path
import json,statistics,datetime
D=Path(__file__).parent
S=json.loads((D/'search-results.json').read_text());M=S['models'];suite=[json.loads(l) for l in (D/'search-suite.jsonl').read_text().splitlines()];names=[r['id'] for r in suite]
g=[]
def gate(clause,observed,limit,passed,details=None):g.append(dict(clause=clause,observed=observed,limit=limit,pass_=passed,details=details))
c=S['aggregate']['comparison'];a=M['lambda0'];b=M['lambda2']
gate('S1 median per-position qnode ratio',c['medianRatio'],.90,c['medianRatio']<=.90)
gate('S2 strictly improved fraction',c['improved']/len(names),.60,c['improved']/len(names)>=.60)
gate('S3 aggregate qnode ratio',c['totalRatio'],.85,c['totalRatio']<=.85)
x=S['withoutLargestImprovement']['results']['comparison']['totalRatio'];gate('S4 largest-saving removed ratio',x,.90,x<=.90,S['withoutLargestImprovement']['removed'])
bad=[n for n in names if b[n]['q']>2*a[n]['q']];gate('S5a >2x qnode regressions',len(bad),1,len(bad)<=1,bad)
bad=[n for n in names if a[n]['status']=='COMPLETED' and (b[n]['completedDepth']<a[n]['completedDepth'] or b[n]['status']!='COMPLETED')];gate('S5b new depth loss or cap',len(bad),0,not bad,bad)
control=json.loads((D/'control-audit.json').read_text());held=json.loads((D/'held-results.json').read_text())
classification='A â€” CONTROL FAILURE' if not control['pass'] else 'D â€” REPLICATION POSITIVE' if held['passed'] and all(x['pass_'] for x in g) else 'C â€” REPLICATION MIXED'
outcomes={r['game']:r['result'] for r in [json.loads(l) for l in (D/'generation-progress.jsonl').read_text().splitlines()]}
byoutcome={}
for outcome in ['WHITE_WIN','BLACK_WIN','DRAW']:
 nn=[r['id'] for r in suite if outcomes[r['game']]==outcome];qs={m:sum(M[m][n]['q'] for n in nn) for m in M}
 byoutcome[outcome]={'positions':nn,'q':qs,'ratio':qs['lambda2']/qs['lambda0'],'medianRatio':statistics.median(b[n]['q']/a[n]['q'] for n in nn),'improved':sum(b[n]['q']<a[n]['q'] for n in nn),'regressed':sum(b[n]['q']>a[n]['q'] for n in nn)}
summary={'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'classification':classification,'pass':all(x['pass_'] for x in g),'gates':g,'byOutcome':byoutcome,
 'medianSP':{m:statistics.median(M[m][n]['spPct'] for n in names) for m in M},'medianSPChange':statistics.median(b[n]['spPct']-a[n]['spPct'] for n in names),
 'largestSaving':max(names,key=lambda n:a[n]['q']-b[n]['q']),'largestRegression':max(names,key=lambda n:b[n]['q']-a[n]['q']),
 'largestRelativeRegression':max(names,key=lambda n:b[n]['q']/a[n]['q']),'moveChanges':[n for n in names if b[n]['move']!=a[n]['move']],
 'largestAbsoluteScoreChange':max(names,key=lambda n:abs(b[n]['score']-a[n]['score']))}
(D/'search-gates.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
```

### verify_final.py

```python
from pathlib import Path
import subprocess,json,hashlib,zipfile,datetime
D=Path(__file__).parent;R=Path(r'C:\projects\seed\java\seedv6')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
p=D/'FreshSearch.java';s=p.read_text();old='List.of(suite.getFirst(),CaptureDiagnostic.POSITIONS[0])';new='List.of(suite.getFirst(),CaptureDiagnostic.POSITIONS[0],CaptureDiagnostic.POSITIONS[4])';assert old in s;s=s.replace(old,new);p.write_text(s)
C=D/'pristine-classes';subprocess.run(['javac','-cp',str(C),'-d',str(C),str(p)],check=True)
comparisons=0
for name in ['lambda0','lambda2']:
 with (D/(name+'-replay-console.txt')).open('x') as out:subprocess.run(['java','-Xmx1024m','-cp',str(C),'com.ohinteractive.seedv6.tools.search.FreshSearch',str(D),name,'replay'],stdout=out,stderr=subprocess.STDOUT,check=True)
 original={r['position']:r for r in [json.loads(l) for l in (D/f'{name}-search-all.jsonl').read_text().splitlines()] if r['type']=='search'}
 rr=[json.loads(l) for l in (D/f'{name}-search-replay.jsonl').read_text().splitlines()];assert rr[-1]['type']=='end'
 for r in rr:
  if r['type']!='search':continue
  o=original[r['position']]
  for k in ['depth','completedDepth','status','nodes','main','q','maxQply','evaluations','standPatCutoffs','checkedQ','tacticalMoves','evasionMoves','softLimit','score','move','workerDiagnostics']:assert o[k]==r[k],(name,r['position'],k)
  comparisons+=1
for f in ['frozen-manifest.json','final-checkpoints.json']:
 for name,h in json.loads((D/f).read_text())['sha256'].items():assert sha(D/name)==h,(f,name)
for f in ['pinned-inputs.json','history-data-hashes.json']:
 for name,h in json.loads((D/f).read_text()).items():assert sha(Path(name))==h,(f,name)
initial=json.loads((D/'initial-hashes.json').read_text());assert all(sha(R/p)==h for p,h in initial.items())
assert subprocess.check_output(['git','status','--porcelain=v1','-uall'],cwd=R)==(D/'initial-status.txt').read_bytes()
assert subprocess.check_output(['git','diff','--cached','--name-only'],cwd=R)==b''
changed=[]
with zipfile.ZipFile(D/'head.zip') as z:
 for f in z.namelist():
  if not f.endswith('/') and z.read(f)!=(D/'repo'/f).read_bytes():changed.append(f)
assert changed==['app/src/main/java/com/ohinteractive/seedv6/core/brn2/Brn2Trainer.java'],changed
checks={};times={}
for name in ['g134','lambda0','lambda2']:
 rr=[json.loads(l) for l in (D/f'{name}-search-all.jsonl').read_text().splitlines()];checks[name]=rr[-1]['checks'];times[name]={'start':rr[0]['utc'],'end':rr[-1]['utc']};assert len([r for r in rr if r['type']=='search'])==16
assert json.loads((D/'control-audit.json').read_text())['utc']<times['lambda2']['start']
assert json.loads((D/'held-results.json').read_text())['utc']<times['g134']['start']
assert json.loads((D/'frozen-manifest.json').read_text())['utc']<json.loads((D/'control-training.jsonl').read_text())['utc']
result=dict(utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),primaryFilesUnchanged=len(initial),primaryStatusUnchanged=True,indexEmpty=True,rootJournalPresent=(R/'CODEXLOG_CURRENT.md').exists(),rootVersionPresent=(R/'VERSION_STATE.txt').exists(),archiveChanges=changed,traceFreeEqualities=comparisons,detachedStaticChecks=checks,searchTimes=times,focusedTests=39,searchRequests=48)
(D/'verification.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
```
