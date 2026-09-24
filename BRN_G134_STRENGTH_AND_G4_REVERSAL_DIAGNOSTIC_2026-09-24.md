# BRN g134 strength and G4 reversal diagnostic — 2026-09-24

**Qualified attribution; production-store integrity BLOCKED/FAILED, and original-game divergence attribution BLOCKED. No fix or training was performed by this diagnostic.**

The evidence supports **BASE OBJECTIVE / STRENGTH MISALIGNMENT** and **SEARCH-SCORE GEOMETRY**, with measurable **DATA-DISTRIBUTION INSTABILITY** as a plausible contributor. It does not isolate a single causal explanation for the playing-strength deficit. The retained generation plans and data support the same handcrafted/50:50 training regime as g134, not a discovered source-generator substitution. Lambda=2 is not established as the primary source of the deficit: lambda=0 already loses strength, and lambda=2 wins the direct endpoint comparison.

Two premises need qualification. First, both arms were below g134 at G1 and G2, **above 50% against g134 at G3**, and below again at G4. All eight RETAIN decisions do not mean all eight candidates lost their matches. Second, the G4 sentinel reversal is relative: both arms improve markedly from G3; L0 improves more. sv-17 alone contributes 20,113 of the full-suite 27,174 extra G4 L2 qnodes (74.02%). Fianchetto has a separate, actual L2 G3→G4 regression.

**Integrity exception discovered at final validation:** another, unidentified writer changed the live BRN t2 store during this turn. The baseline g134 plan was replaced, the original checkpoint and data were archived, a replacement g134 appeared, and a store lock was unreadable. This task stopped further search/gradient execution on discovery and did not undo or reconcile those changes. All frozen acceptance inputs and outputs remained unchanged. Historical reconstruction below refers to the authenticated starting snapshot, not the newly replaced live plan. This report is a completed reporting unit with the stated limitations, not complete causal isolation, user acceptance, or authorization to train.

## Scope, baseline and evidence

Authoritative repository: `C:\projects\seed\java\seedv6`. Inspected HEAD: `df6c23398307c3023275fc68051abaa8d6f42e69`, exactly the cited acceptance-report commit. The inherited dirty tree, including the accepted integration, was used and preserved. Initial index empty. No on-disk ancestor/repository AGENTS file was found; the supplied governance and `source/CHESS_SEARCH_CONTRACT.md` applied. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent, so neither was created or activated. First successful UTC observation: `2026-09-24T11:21:10Z`; an earlier unsupported PowerShell date option failed and supplies no timestamp.

Read first: [production integration](BRN_CAPTURE_CONSISTENCY_PRODUCTION_INTEGRATION_2026-09-24.md) and [multi-generation acceptance](BRN_CAPTURE_CONSISTENCY_MULTI_GENERATION_ACCEPTANCE_2026-09-24.md). The earlier stand-pat, calibration, campaign-target replay and fixed-checkpoint search-validation reports supplied fixture definitions and interpretation limits. Their previously measured results were not substituted for this turn's measurements.

Existing acceptance root, abbreviated **A**: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-multigen-20260924`.

New external diagnostic root, abbreviated **D**: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-attribution-20260924`.

Production roots: `E:\SeedV6-Networks\BRN\BRN-2\t2` and `E:\SeedV6-Networks\NNUE\training`. A's L0/L2 stores, frozen payload copies, all generation JSONL/binary files, held corpus and game aggregates were read only. Temporary Java/Python sources, compiled current production classes, derivative arrays and diagnostic outputs stayed in D. No store-writing service or optimizer update method was called. Model/state decoding and analytic gradients are not new training generations.

D's complete diagnostic artifact manifest is `diagnostic-artifacts.json`, SHA-256 `64849203a058e3955feb32faa978503f54e0f93b600dfe9e1cf7fb377369ebb1`. It pins the source helpers and raw/derived outputs named below. D's starting full-tree/store inventory SHA-256 is `9aae603f39a771133425ee2b9a1b390dc326c3171a771fb442cc7ef10918e90c`. Local temporary evidence is retained but is not a portable repository dependency; substantive results and limitations are preserved here.

| Frozen acceptance artifact (under A) | SHA-256 |
| --- | --- |
| g134.brn2 | 21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924 |
| g134.state | ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9 |
| teacher.nnue | 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9 |
| held.bin | fc084a202de2a5cda88eb00149aff6c3dde1e5a4aef446324483a1222d1a86c2 |
| g135-data.bin | 3628f9826acd2ee405d3cbe4877d1424c7c19aae780925f328fb307a78004723 |
| g136-data.bin | bbecc8fa0484f39007f6ff0f10d6f3e50cf218f3eec39654ac666fca69641fc9 |
| g137-data.bin | 65ff84f0a8b2ac00626a4b19b96ceef38f4846cc5cb80e57bb198a7c2db811f0 |
| g138-data.bin | 793647c6393a50b56d305f28e245465f1dff65b055c3aab81ad2418dfabcd6bc |
| search-suite.tsv | 9aaad2d4cb46f4409ec3dfbe9d2af3732a5254a1ae18ea98482171197cf494b1 |
| sentinel.tsv | 8407ed4d9f4e0b5c2ddd8796664da51eea078e4dc2f5ae20011a6e82655f8b17 |
| arena-g134.jsonl | b6d982c71263c3ecfd217ab6499e17dd14ee9fd6d7d9785400f724041872b547 |
| arena-L0-G4.jsonl | 9fcc07fb157fc2081c3de5f2ff9658c678d609e36b0276d99d157402b93f337b |

All eight L0/L2 model/state hashes also match the acceptance report; full hashes are in the unchanged acceptance report and D/integrity-before.json. Forty-three hash-bearing report rows and all 25 preregistered frozen-input hashes matched. Each of the nine states passed codec round-trip and exact inference-model equality. Model identities are always hashes, never just a generation label.

## 1. How g134 was produced, and fidelity of the acceptance campaign

Read-only checksummed readers decoded **134 plans, 135 linked checkpoint manifests (g0–g134), 133 completed history rows, the pending generation attempt, and retained historical data for g131–g134**. All manifest parent links form the recorded consecutive lineage. No lineage fact was taken from memory.

At the starting snapshot, g134 was `g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c`. Its parent was g133 `g000133-s000216573-903ffd2566c8084b438e51f8be49523cdf4ebf74fad647cb18e2606f87116d0d`, step 216,573. Their step difference is exactly **1,585**, matching g134's retained TRAIN count. The g134 plan/data link matched the starting plan hash `c0a61a2583acd182478d4afe4ceaa521d05170f67ce50287fadb5ed159973a78`. Its full model, weights, Adam moments and config were retained.

Additional durable corroboration: the accepted [campaign-target replay report](BRN_CAMPAIGN_TARGET_CAPTURE_REPLAY_DIAGNOSTIC_2026-09-24.md), commit `6eb0df1a1a48c646b1850ceaab5a397c5110eba1`, records that restoring exact g133 state and running the saved generation-134 partition, original target selector and shuffle through unmodified Brn2SelfPlayTraining reproduced **every byte of g134 training.state**. Training loss .078351506→.030122899 in exactly 1,585 updates. This is previously accepted runtime evidence consulted here, not a replay newly executed in this read-only unit. It strengthens reconstruction of the actual update path beyond settings inference.

All 134 plans record HANDCRAFTED generation, 50% pinned NNUE g74 supervision, and the same basic generation/training configuration. g1–g128 used held-out-loss promotion; **g129–g134 used candidate-vs-Best game pairs**. g127 was the last promoted Best; g128–g133 retained it. g134 validation was unfinished. g134 is therefore a strong empirical reference in these later comparisons, not a historically completed/promoted Best.

Immediately preceding g134, candidate game scores against g127 were g129 44.92%, g130 42.58%, g131 21.09%, g132 31.64%, g133 37.50%. This establishes pre-existing continuation/promotion divergence and volatility, but the opponent differs from the acceptance campaign's g134; these scores must not be pooled as one strength scale.

| Dimension | g134 / historical evidence | Acceptance campaign | Classification |
| --- | --- | --- | --- |
| Source and generation mechanism | HANDCRAFTED; no neural generator; retained plans/data | Native SelfPlayBatch.generateHandcrafted, frozen once per generation | Identical recorded source; operationally equivalent execution |
| Games / start / opening | 64; standard start; uniform 0–8 random plies | Same | Identical |
| Search settings | Depth 4, six root workers; 1,024-ply cap; no node/time move cap | Same | Identical |
| Sample selection | Completed games only; ≤32 evenly spaced pre-move samples, endpoints included; equal row weight | Same sampler and whole-game partition | Identical |
| Source/outcome selection | Unconditioned batch; no class balancing recorded; fresh retained batches | Unconditioned fresh batches; no outcome quotas | Operationally equivalent, different realized samples |
| Base target | 0.5 terminal STM WDL + 0.5 bounded static g74 NNUE | Exact same teacher bytes and mixture | Identical |
| Training loop | One Fisher–Yates shuffled online epoch, minibatch 1; one update per real parent | Same; L2 adds synthetic-child auxiliary derivative only | Identical base; L2 intentionally materially different |
| Optimizer | Saved Adam: LR .001, β1 .9, β2 .999, ε 1e−8; global step; sparse moments freeze when untouched | Continued exact g134 state; no reset, decay or schedule | Identical initialization and semantics |
| RNG streams | Master/data seeds 1/1; generation-specific domains | 2026092404/2026092404; same domain mixing | Materially different realization, same stochastic regime |
| Validation | g129–g134: 64 pairs, depth 4, six workers, policy(64,.05,0) | Same current contract; isolated g134 Best | Identical method; materially different incumbent identity |
| Earlier promotion | g1–g128 held-out loss; g127 last Best | All eight use game-pair validation | Materially different from early history, same as immediate predecessor regime |
| Continuation after RETAIN | Recorded consecutive Latest parents; no rollback to Best | Latest advances after all RETAIN decisions | Identical semantics |
| Dataset installation | Normal service generates and persists plan/data | Harness pre-generates identical-format shared data and installs it before resume | Operationally equivalent for training on recorded rows |
| Exact old executable / six-thread game replay | Historical executable image and per-move generation traces not retained here | Current dirty source compiled externally | Unknown binary-level game-generation reproduction; accepted exact training replay exists |

**Answer to fidelity question:** yes at the evidenced data-generation, target and optimizer semantics level for g134's immediate regime. It is not a bit-identical continuation of seed 1's next batches or the original incumbent selection. No support was found for NNUE-generated positions, target-weight substitution, class-conditioned generation, teacher replacement or optimizer reset in the acceptance run. The earlier 75%-teacher reports concern other experiments, not this t2 lineage. A recorded HANDCRAFTED enum alone would be insufficient; the corroboration here includes settings, actual retained data, native sampler/source dispatch, teacher hashes, state config and parent/update accounting. Exact historical process provenance remains limited as stated.

The historical generator and current native generator both use the existing handcrafted evaluator through RootParallelSearch/IterativeDeepeningSearch. Search scores do not become targets. WDL is actual completed-game outcome in sample STM; NNUE is independently evaluated as its bounded static value. The child has no terminal WDL/base label. Ordinary loss is half squared error of the blended scalar, not a separately reported sum of two component losses.

## 2. Complete checkpoint trajectory

| Checkpoint | Fixed base | Native held | Δ median | Δ p90 | Δ p95 | \|error\|>.5 | Sign | Sentinel q | SP | W/D/L vs g134 | Score | Lifecycle s | Decision |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| g134 | 0.109080 | 0.079125 (H134) | 0.2329 | 0.5923 | 0.7378 | 17.76% | 66.36% | 111874 | 55.35% | reference | not measured | unfinished | original validation unfinished |
| L0-G1 | 0.082485 | 0.081260 | 0.2015 | 0.4931 | 0.6803 | 10.28% | 63.55% | 63927 | 69.61% | 41/5/82 | 33.98% | 1316.976 | RETAIN g134 |
| L2-G1 | 0.086078 | 0.078763 | 0.1488 | 0.3940 | 0.4904 | 4.67% | 62.62% | 47240 | 73.82% | 54/4/70 | 43.75% | 1495.414 | RETAIN g134 |
| L0-G2 | 0.107676 | 0.062546 | 0.2030 | 0.4424 | 0.5013 | 5.61% | 61.68% | 64240 | 69.74% | 29/4/95 | 24.22% | 1157.905 | RETAIN g134 |
| L2-G2 | 0.097493 | 0.065644 | 0.2013 | 0.4602 | 0.5193 | 6.54% | 55.14% | 42676 | 75.58% | 41/6/81 | 34.38% | 1219.363 | RETAIN g134 |
| L0-G3 | 0.110196 | 0.060129 | 0.2995 | 0.6952 | 0.7433 | 21.50% | 62.62% | 306406 | 44.04% | 72/7/49 | 58.98% | 1741.539 | RETAIN g134 |
| L2-G3 | 0.089595 | 0.057837 | 0.1502 | 0.3846 | 0.4307 | 4.67% | 64.49% | 156349 | 53.96% | 76/7/45 | 62.11% | 1590.149 | RETAIN g134 |
| L0-G4 | 0.088740 | 0.109358 | 0.2062 | 0.5956 | 0.6463 | 15.89% | 63.55% | 52296 | 72.73% | 54/3/71 | 43.36% | 1260.054 | RETAIN g134 |
| L2-G4 | 0.082795 | 0.102942 | 0.1642 | 0.4144 | 0.4748 | 3.74% | 62.62% | 67509 | 66.22% | 44/6/78 | 36.72% | 1358.176 | RETAIN g134 |

Fixed base/delta use the same 107 held pairs from five games at every checkpoint. Native held populations differ. Lifecycle seconds are stored production totalNs; they exclude the separately generated source charge and JVM/setup overhead. g134's own generation did not finish validation, so no invented total runtime, ordinary candidate score or promotion is supplied. For g134's generation-134 native held loss, the older campaign-target replay report records 0.079125; it is a different population from every acceptance native holdout and is not an extra recomputation here.

The eight binary validation records were decoded and their statistics matched their run JSONLs; every incumbent is the isolated g134. The promotion rule requires score strictly over roughly 65.30% at 64 pairs, so G3 scores 58.98% and 62.11% still RETAIN. Strength degradation is **immediate at G1, non-monotonic thereafter**, not four consecutive losses or a cumulative monotone decline. Different generation opening seeds and six-worker games limit cross-generation comparisons.

G1 L0 loss 0.109080→0.082485 improves about 24.4%, while its score is 33.98%; its qnodes also fall 42.9%. This is the cleanest demonstration that neither base loss nor cheap qsearch guarantees strength retention, independent of the auxiliary objective. G3→G4 gives the same directional conflict in both arms: loss and qnodes improve, game score declines. The endpoint arenas remain 78/3/47 (62.11%) for L2 over L0 and 30/8/90 (26.56%) for L2 against g134; they use a separate g139 opening set. There is no L0/g134 match on that final set.

## 3. Frozen dataset distributions

| Dataset | Games W/D/B | All/TRAIN/HELD rows | Game plies min/mean/max | TRAIN ply mean/median | White STM | Distinct openings | Legal capture | Eligible pair | ≥3 captures |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| H131 | 27/5/32 | 2048 / 1632 / 416 | 49 / 124.0 / 237 | 61.9 / 54.0 | 50.31% | 57 | 62.62% | 62.44% | 22.24% |
| H132 | 14/6/44 | 2048 / 1632 / 416 | 60 / 131.6 / 280 | 65.0 / 59.0 | 50.06% | 56 | 59.13% | 59.13% | 21.08% |
| H133 | 22/6/36 | 2048 / 1632 / 416 | 43 / 119.9 / 287 | 59.5 / 53.0 | 52.45% | 58 | 60.23% | 59.87% | 20.96% |
| H134 | 28/6/30 | 2001 / 1585 / 416 | 6 / 119.9 / 290 | 61.7 / 54.0 | 51.04% | 60 | 60.76% | 60.57% | 19.56% |
| G1 | 27/7/30 | 2043 / 1627 / 416 | 27 / 122.2 / 273 | 60.2 / 51.0 | 53.84% | 59 | 63.86% | 63.80% | 26.43% |
| G2 | 26/6/32 | 2048 / 1632 / 416 | 39 / 121.3 / 254 | 58.7 / 52.0 | 52.70% | 59 | 62.75% | 62.38% | 23.59% |
| G3 | 21/11/32 | 2025 / 1632 / 393 | 9 / 139.5 / 249 | 72.1 / 64.5 | 48.35% | 54 | 55.58% | 55.45% | 19.67% |
| G4 | 29/2/33 | 2042 / 1626 / 416 | 26 / 118.5 / 226 | 58.3 / 52.0 | 49.75% | 61 | 61.44% | 61.25% | 19.50% |

H131–H134 denote historical generations immediately preceding/including g134. Every batch has 64 completed games and zero aborted/capped games. TRAIN/HELD split is 51/13 whole games. Opening groups are exact board-key groups reconstructed using only the recorded indexed opening RNG and legal random moves; **no new game or search generated them**. These are random native openings, not named opening-book classes. Historical per-game lengths were not retained in .data, so their medians are unknown. Acceptance game-length medians, distributions and terminations are in A/g135..g138-source.jsonl and D/dataset-analysis.json. All sample-level distribution statistics use retained boards, not reconstructed game trajectories.

| Dataset | STM W/D/L rows | Mean WDL | NNUE mean/median/SD | Blend mean/median/SD | \|NNUE\|>.9 | \|blend\|>.9 | w·n<0 | Mean \|w−n\| |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| H131 | 791 / 128 / 713 | 0.0478 | 0.0466 / 0.0394 / 0.5526 | 0.0472 / 0.0731 / 0.6132 | 3.37% | 13.48% | 33.58% | 0.8108 |
| H132 | 837 / 64 / 731 | 0.0650 | 0.0495 / 0.0554 / 0.5660 | 0.0572 / 0.0765 / 0.6096 | 3.37% | 13.17% | 38.66% | 0.8709 |
| H133 | 761 / 160 / 711 | 0.0306 | 0.0309 / 0.0107 / 0.5512 | 0.0308 / 0.0497 / 0.5989 | 4.17% | 13.42% | 34.99% | 0.8249 |
| H134 | 772 / 192 / 621 | 0.0953 | 0.0542 / 0.0501 / 0.5600 | 0.0747 / 0.0993 / 0.5787 | 4.10% | 11.55% | 36.28% | 0.8467 |
| G1 | 760 / 224 / 643 | 0.0719 | 0.0717 / 0.0537 / 0.5800 | 0.0718 / 0.0958 / 0.6034 | 3.44% | 16.10% | 33.19% | 0.7890 |
| G2 | 799 / 96 / 737 | 0.0380 | 0.0257 / 0.0086 / 0.5649 | 0.0318 / 0.0544 / 0.6137 | 3.06% | 13.60% | 36.40% | 0.8425 |
| G3 | 766 / 320 / 546 | 0.1348 | 0.0326 / 0.0313 / 0.5135 | 0.0837 / 0.1250 / 0.5597 | 2.39% | 8.39% | 31.37% | 0.7713 |
| G4 | 809 / 64 / 753 | 0.0344 | 0.0395 / 0.0233 / 0.5585 | 0.0369 / 0.0611 / 0.6100 | 3.81% | 12.73% | 38.62% | 0.8686 |

Disagreement uses strict opposite nonzero signs; draws are not counted as disagreements. Thus draw proportion partly changes that rate. Reported |w−n| separately includes draws. WDL is exactly −1/0/+1; “extreme” thresholds above are descriptive, fixed at .9, not tuned training filters. D/dataset-analysis.json also contains the full ALL and HELD distributions, p05/p90/p95, extrema, sample-ply spread, piece counts, all eligible teacher deltas, and exact selected-pair deltas for acceptance data.

| Dataset | Selected pairs | Target Δ mean | Median | SD | p05 | p95 | Min | Max |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| G1 | 1038 | 0.0160 | 0.0054 | 0.1325 | -0.2059 | 0.2283 | -0.7054 | 0.4805 |
| G2 | 1018 | 0.0164 | 0.0079 | 0.1303 | -0.2152 | 0.2193 | -0.5184 | 0.4841 |
| G3 | 905 | 0.0166 | 0.0088 | 0.1479 | -0.2436 | 0.2490 | -0.6315 | 0.5241 |
| G4 | 996 | 0.0187 | 0.0053 | 0.1392 | -0.2070 | 0.2544 | -0.6321 | 0.5896 |

G3→G4 shifts substantially: source draws **17.19%→3.13%**; TRAIN draw rows **19.61%→3.94%**; mean sampled ply **72.10→58.29**; mean pieces **16.77→18.69**; legal-capture incidence **55.58%→61.44%**, and eligible pairs **55.45%→61.25%**. However, the stricter capture-rich category (≥3 legal captures) is essentially flat, **19.67%→19.50%**. “More capture-rich” should therefore not be interpreted as every tactical-intensity measure increasing.

The blended target becomes less positively centered (**.08369→.03695**) but wider (**SD .55974→.61000**); extreme blend targets rise **8.39%→12.73%**. Teacher/WDL sign disagreement rises **31.37%→38.62%**, and mean absolute disagreement rises **.77128→.86860**. Selected target-delta SD actually falls **.14787→.13918**, so there is no evidence of a G4 explosion in auxiliary target size. Opening repetition also differs: 54→61 distinct opening boards, with standard-start selections 11→4.

G4's low draw share is outside the immediately preceding historical 5–6/64 range, but historical outcome volatility is already substantial (g132 14/6/44). G3 is unusually late/endgame-heavy relative to H131–H134 and the other acceptance batches. These are measured shifts capable of changing gradients and learned score geometry. With one batch per generation and moving parent weights, they do **not** identify which source component caused the relative reversal or prove that draws should be reweighted.

## 4. Parameter and gradient drift

| Checkpoint | Step | Weight norm | Δ from parent | Relative parent | Δ from g134 | Adam m norm | Adam v norm |
| --- | --- | --- | --- | --- | --- | --- | --- |
| g134 | 218158 | 54.4256 | 0.0000 | 0.00% | 0.0000 | 2.3743 | 0.6037 |
| L0-G1 | 219785 | 54.5688 | 3.7943 | 6.97% | 3.7943 | 2.4520 | 0.6313 |
| L0-G2 | 221417 | 54.6963 | 3.6516 | 6.69% | 5.2715 | 2.5885 | 0.6660 |
| L0-G3 | 223049 | 54.8032 | 3.1573 | 5.77% | 6.2051 | 2.5297 | 0.5554 |
| L0-G4 | 224675 | 54.9049 | 3.4139 | 6.23% | 7.1418 | 2.4363 | 0.5846 |
| L2-G1 | 219785 | 54.6201 | 4.3218 | 7.94% | 4.3218 | 2.4617 | 2.0736 |
| L2-G2 | 221417 | 54.7981 | 3.9435 | 7.22% | 6.0099 | 2.5830 | 2.1308 |
| L2-G3 | 223049 | 54.9396 | 3.4129 | 6.23% | 7.0450 | 2.4435 | 1.8169 |
| L2-G4 | 224675 | 55.0814 | 3.6575 | 6.66% | 8.1051 | 2.4455 | 1.9764 |

| Checkpoint | node | relationA | relationB | status | localBias | boardBias | outputWeights | outputBias |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L0-G1 | 0.82310 | 2.56029 | 2.64410 | 0.28806 | 0.17550 | 0.15763 | 0.18474 | 0.00633 |
| L0-G2 | 0.78136 | 2.46387 | 2.55426 | 0.23022 | 0.14119 | 0.15710 | 0.17707 | 0.00593 |
| L0-G3 | 0.72384 | 2.16067 | 2.16483 | 0.20264 | 0.11217 | 0.10693 | 0.15741 | 0.00823 |
| L0-G4 | 0.74623 | 2.35055 | 2.32015 | 0.33648 | 0.16357 | 0.15700 | 0.15872 | 0.01187 |
| L2-G1 | 0.89416 | 2.97416 | 2.96933 | 0.32477 | 0.16627 | 0.17129 | 0.23107 | 0.00079 |
| L2-G2 | 0.86031 | 2.66739 | 2.75480 | 0.23483 | 0.13969 | 0.12143 | 0.13190 | 0.00654 |
| L2-G3 | 0.76650 | 2.30226 | 2.37831 | 0.22116 | 0.13972 | 0.13306 | 0.13265 | 0.00622 |
| L2-G4 | 0.75934 | 2.52404 | 2.51715 | 0.24049 | 0.11181 | 0.11715 | 0.09777 | 0.01520 |

Group entries are parent-to-child parameter L2 distances. Counts: node 30,720; relation A/B 806,400 each; status 2,048; local/board/output vectors 32 each; scalar output bias 1. Relative norms compare to each actual parent. All parameters/moments decode as finite; second moments are nonnegative; saved learning rate and Adam constants are unchanged. Sparse moments are not globally decayed on untouched rows, so their full-array norms are not a simple current-batch gradient statistic.

L2-G3→G4 changes 3.6575 in global L2, **6.657%** of parent norm, versus 3.4129/6.228% for its preceding update. L0 similarly rises 3.1573→3.4139. Neither G4 update is the largest in its arm. L2's G4 output-weight change **.09777** is its smallest generation change, while output bias moves **−.008457→+.006744**. Learned predictions cannot be explained by this scalar alone: local activations, many representation rows and output weights also change. L2's second-moment norm is larger throughout the campaign, not an isolated G4 discontinuity. Parameter distance detects substantial drift; it does not establish weakness or an optimizer defect.

A read-only analytic derivative check used **128 evenly indexed original TRAIN rows per dataset**, including unpaired rows, on **both fixed L2-G2 and L2-G3 weights**. It differentiates the accepted loss through each endpoint's own ReLU/tanh masks, includes lambda=2 in auxiliary gradients, and makes **zero optimizer calls**. Six finite-difference components checked the independent derivative calculation. These are fixed-state sample RMS norms, not an online update replay or dense-batch mean gradient; Adam would further transform them.

| Fixed model | Data | Pairs/128 | Base RMS | Aux RMS | Aux/base | Base head RMS | Aux head RMS | Huber linear rows |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-G2 | G3 | 70 | 1.5121 | 2.2123 | 1.4631 | 1.3188 | 2.0255 | 23 |
| L2-G2 | G4 | 80 | 1.8387 | 2.5248 | 1.3731 | 1.6029 | 2.3022 | 33 |
| L2-G3 | G3 | 70 | 1.1165 | 1.6414 | 1.4701 | 0.9970 | 1.5247 | 9 |
| L2-G3 | G4 | 80 | 1.7104 | 2.0178 | 1.1797 | 1.5219 | 1.8637 | 13 |

At the same L2-G3 weights, G4 raises base RMS by 53.2% and auxiliary RMS by 22.9%; the ratio falls 1.47→1.18. At L2-G2 weights the ratio also falls. This is evidence of harder/different G4 examples, **not** evidence of a sudden auxiliary-gradient takeover. The head accounts for much of squared gradient magnitude. There is no global gradient clipping in the inspected trainer; the auxiliary output derivative is Huber-bounded, and tanh derivatives can attenuate gradients. No new checkpoint was created. Full per-row norms/dot products are retained in D/gradients.jsonl.

## 5. Bounded search replay and concrete reversal mechanism

Six fixed fixtures were selected before new measurements: sv-17 (largest G4 regression), sv-18 (largest saving), sv-01 (next large absolute regression), sv-09 (modest/near-neutral), Kiwipete and Fianchetto. Outcome-based selection makes this a mechanism diagnostic, not an independent acceptance suite. Exact FENs are in D/probe-suite.tsv and the original suite/fixtures. Checkpoints: g134, L0-G3/G4, L2-G3/G4.

Settings exactly preserve accepted fixed-position search: depth 4, one worker, fresh 262,144-entry TT/order state, singleton history, 1,000,000 cumulative entered nodes, 60 seconds, normal search/qsearch/mapping. Each request ran trace-free then with the existing QsearchDecisionTrace. All **30** non-timing results matched exactly; 20 distinct already-recorded model/position results matched the acceptance artifacts (23 comparisons including duplicate sentinel/final records). The two additional cross-shadow requests also matched. No production instrumentation was added.

| Position | Model | Depth/status | Qnodes | SP attempts | SP rate | Max qply | Q roots | Root score/move |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| sv-01 | g134 | 4 / COMPLETED | 47050 | 48657 | 45.70% | 17 | 5964 | 146 / g4f2 |
| sv-01 | L0-G3 | 4 / COMPLETED | 31563 | 34413 | 49.69% | 17 | 5047 | -931 / h7h6 |
| sv-01 | L0-G4 | 4 / COMPLETED | 24610 | 26961 | 52.36% | 18 | 3989 | 396 / h7h6 |
| sv-01 | L2-G3 | 4 / COMPLETED | 74953 | 75477 | 49.80% | 18 | 5152 | -383 / h7h6 |
| sv-01 | L2-G4 | 4 / COMPLETED | 31044 | 33509 | 53.89% | 16 | 4321 | 688 / h7h6 |
| sv-09 | g134 | 4 / COMPLETED | 8073 | 11755 | 62.57% | 11 | 4360 | 2766 / e6c4 |
| sv-09 | L0-G3 | 4 / COMPLETED | 10688 | 15554 | 62.34% | 11 | 6039 | 5414 / d4c2 |
| sv-09 | L0-G4 | 4 / COMPLETED | 8619 | 12260 | 68.70% | 12 | 4562 | 7568 / e6c4 |
| sv-09 | L2-G3 | 4 / COMPLETED | 7738 | 12796 | 71.67% | 9 | 5892 | 6780 / d4c2 |
| sv-09 | L2-G4 | 4 / COMPLETED | 8532 | 12789 | 68.86% | 9 | 5150 | 6202 / e6c4 |
| sv-17 | g134 | 4 / COMPLETED | 55182 | 56716 | 51.89% | 18 | 7709 | 6066 / d8c7 |
| sv-17 | L0-G3 | 4 / COMPLETED | 250376 | 233220 | 40.21% | 18 | 11010 | 4683 / f8e7 |
| sv-17 | L0-G4 | 4 / COMPLETED | 18459 | 24273 | 72.29% | 17 | 6928 | 3815 / g7g6 |
| sv-17 | L2-G3 | 4 / COMPLETED | 115639 | 113571 | 48.71% | 18 | 9823 | 2805 / f7f6 |
| sv-17 | L2-G4 | 4 / COMPLETED | 38572 | 40222 | 55.71% | 17 | 5097 | 2392 / g7g6 |
| sv-18 | g134 | 4 / COMPLETED | 10221 | 15986 | 63.59% | 12 | 6529 | -10122 / g2g4 |
| sv-18 | L0-G3 | 4 / COMPLETED | 20137 | 23571 | 46.12% | 14 | 4756 | -7921 / g2g4 |
| sv-18 | L0-G4 | 4 / COMPLETED | 14267 | 20964 | 61.02% | 15 | 7643 | -10236 / g2g4 |
| sv-18 | L2-G3 | 4 / COMPLETED | 10144 | 15875 | 64.04% | 12 | 6466 | -6337 / g2g4 |
| sv-18 | L2-G4 | 4 / COMPLETED | 9641 | 13970 | 59.84% | 13 | 4893 | -7867 / g2g4 |
| middlegame-kiwipete | g134 | 4 / COMPLETED | 209297 | 180919 | 44.71% | 19 | 6883 | 10036 / e1g1 |
| middlegame-kiwipete | L0-G3 | 3 / NODE_LIMIT | 991481 | 777615 | 32.76% | 20 | 6170 | -1108 / e1g1 |
| middlegame-kiwipete | L0-G4 | 4 / COMPLETED | 686443 | 586332 | 46.01% | 19 | 9128 | -4470 / e5g6 |
| middlegame-kiwipete | L2-G3 | 3 / NODE_LIMIT | 997210 | 772371 | 33.41% | 20 | 2594 | -973 / e1g1 |
| middlegame-kiwipete | L2-G4 | 4 / COMPLETED | 531245 | 449786 | 49.18% | 19 | 6728 | -3248 / d5e6 |
| quiet-fianchetto | g134 | 4 / COMPLETED | 13007 | 16509 | 53.89% | 17 | 4014 | -18683 / f3e5 |
| quiet-fianchetto | L0-G3 | 4 / COMPLETED | 4533 | 11479 | 85.45% | 11 | 7291 | -11188 / b1a3 |
| quiet-fianchetto | L0-G4 | 4 / COMPLETED | 3964 | 15291 | 91.12% | 8 | 11519 | -3729 / d1d2 |
| quiet-fianchetto | L2-G3 | 4 / COMPLETED | 5602 | 11511 | 77.04% | 12 | 6133 | -11113 / d1d2 |
| quiet-fianchetto | L2-G4 | 4 / COMPLETED | 7171 | 15543 | 81.33% | 14 | 8765 | -7139 / d1d2 |

Kiwipete L0-G3 and L2-G3 reach the million-node cap at the recorded completed depth; their counts are prefixes, not completed depth-4 totals. All other main requests complete. Kiwipete G4 L2 costs less than L0, but both still cost much more than g134. No cap was increased and no failed request was silently treated as a completed result.

### sv-17: relative reversal, local windows and deeper continuations

L2 qnodes fall **115,639→38,572**; L0 falls **250,376→18,459**. Both G4 models choose **g7g6**, so the G4 arm difference cannot be attributed solely to a changed final root move. L2-G4 enters fewer qsearch roots (**5,097 vs 6,928**) and fewer main nodes (**6,953 vs 9,024**), yet its qply≥5 population is **22,426 vs 5,657**. The excess is therefore in surviving deeper continuations, not simply more qsearch starts.

At qply 1, L0 cuts **6,322/7,043 = 89.76%**, L2 **4,991/6,234 = 80.06%**. At qply 2 these become **788/1,636 = 48.17%** and **854/2,319 = 36.83%**. L2 explores 2,531 qply-2 entries versus L0's 1,736 despite fewer first-level entries. This local threshold behavior propagates into the deeper tree.

The stronger test evaluates **L0-G4 as a read-only shadow on every actual L2-G4 static node using L2's unchanged windows**. Of 40,222 stand-pat-eligible nodes: both cut 21,776; only L0 cuts **4,395**; only L2 cuts **632**; neither cuts 13,419. L0's same-window cut rate is **65.07%**, versus L2's **55.71%**. Actual descendants under at least one L0-only cutoff ancestor total **25,918/43,669 observed q positions**. This proves local score/window disagreement along the expanded tree; the descendant union is **not** a predicted node saving for a substituted search.

On the deterministic every-67th-node sample from that L2-G4 tree, L0/L2 mean STM values are **+.1204 / +.0336**, and their paired difference L2−L0 is mean **−.08679**, median **−.09098**, SD **.11124**. The difference is materially position-dependent, not one universal offset. In contrast, the small fixed 107-parent corpus has L2's mean slightly *higher* than L0's. A global holdout mean would miss the actual search-context shift.

Root capture edges also expose metric coverage: L2-G4's first-edge median error is **.0676 vs L0 .0983**, but second/third-edge medians are **.1192/.1801 vs .1052/.1520**. On sampled actual qsearch parent-child transitions, L2's median error is **.1533 vs L0 .1293**, reversing the global held-out ranking. These transitions include qsearch evasions/promotions as well as captures; the root capture-chain results below isolate legal captures. The supported mechanism is **context-specific score centering and local/deeper geometry interacting with beta windows**. A constant-offset defect, unique offending parameter, or exact counterfactual total cost has not been established.

### Fianchetto: a different mechanism from the sentinel aggregate

L2 G3→G4 qnodes **5,602→7,171 (+28.01%)**, while stand-pat effectiveness **77.04%→81.33%** actually rises. The final root move stays **d1d2**. Qsearch roots rise **6,133→8,765 (+42.92%)**, main nodes **7,732→11,009**, max qply **12→14**, and qply≥5 entries **481→828**. Qnodes per qsearch root fall **.913→.818**: more entry points account for the aggregate increase, even though a deeper tail also grows. Aggregate stand-pat percentage alone gives the wrong explanation.

Relative to L0-G4, L2-G4 has fewer qsearch roots (8,765 vs 11,519), but substantially more continuation after the first capture: qply 1 cutoffs **4,272/4,647 = 91.93%** versus **3,755/3,789 = 99.10%**, and qply-2 entries **729 vs 78**. At unchanged L2 windows the L0 shadow cuts **1,130** nodes where L2 continues; the reverse is **123**. Mean sampled STM values are **+.2820 L0 vs +.1312 L2**. Both G4 models still cost fewer qnodes than g134's 13,007, so “Fianchetto regresses” needs the comparator stated.

Main-search TT and ordering counters change, but no faulty ordering rule is established: first-move beta cutoffs rise from 1,393 total/1,221 first at L2-G3 to 1,816/1,652 at L2-G4. Neural aspiration, razoring and futility are disabled in these settings. Exact downstream contribution from PVS windows versus changed intermediate move ordering is not isolated by this observer. The final root/PV-first move staying fixed does not mean all internal PVs or ordering stayed fixed.

## 6. One-ply versus deeper capture geometry

Enumerated **all active legal capture continuations to length 3 from the six fixed roots**, in coordinate order, including capturing promotions/en passant and excluding quiet promotions or terminal/rule-drawn children. This produces 22 one-edge, 107 two-edge and 615 three-edge prefixes (744 total); no enumeration bound was reached. Every checkpoint sees identical boards. No chain was trained. The sample is correlated and Kiwipete dominates longer chains.

For native-STM values y_i and pinned teacher n_i, edge error is `e_i = -y_(i+1)-y_i - .5*(-n_(i+1)-n_i)`. Report endpoint cumulative error as `|(-1)^k*y_k-y_0 - .5*((-1)^k*n_k-n_0)|`, equivalently `|sum((-1)^i*e_i)|`; do **not** naively sum native-STM signed errors. D also records sum of absolute edge errors and same-square recapture subsets. Constant common-perspective WDL would cancel along these active edges, as in the campaign objective, but no actual WDL labels or chess-truth claims are assigned to synthetic children.

| Model | Edge1 median | Edge2 median | Edge3 median | 2-edge endpoint median | 3-edge endpoint median | Sign1 | Sign2 | Sign3 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| g134 | 0.1648 | 0.1740 | 0.1392 | 0.1286 | 0.1837 | 68.18% | 66.36% | 72.68% |
| L0-G3 | 0.3455 | 0.2116 | 0.2175 | 0.1505 | 0.3610 | 59.09% | 59.81% | 71.71% |
| L0-G4 | 0.2300 | 0.1458 | 0.1927 | 0.1549 | 0.3519 | 63.64% | 61.68% | 72.52% |
| L2-G3 | 0.2080 | 0.1497 | 0.1632 | 0.1351 | 0.2613 | 77.27% | 65.42% | 73.98% |
| L2-G4 | 0.1488 | 0.1099 | 0.1358 | 0.1411 | 0.2539 | 77.27% | 66.36% | 74.15% |

L2-G4 is better than L0-G4 in these pooled edge and cumulative medians. Against L2-G3, local medians improve at all three depths, but the two-edge endpoint median worsens **.1351→.1411**, while three-edge improves **.2613→.2539**. This illustrates cancellation/orientation effects; it is not a universal accumulation failure. Nor is L2-G4 better on every individual edge: pooled medians do not establish pointwise dominance.

Position-specific findings matter. sv-17 has better first-edge geometry yet worse second/third-edge median errors than L0-G4. Fianchetto has only **2/2/3 prefixes** at lengths 1/2/3: L2 G3→G4 last-edge medians **.1139→.1327**, **.0387→.0329**, **.1344→.2036**; three-edge endpoint error **.1677→.2558**. L2-G4's third-edge sign agreement is 0/3 there. These are concrete deeper-context defects on a tiny fixture, not a population estimate. Even there L2-G4 remains better than L0-G4 on the corresponding error medians, while searching more qnodes.

Thus the one-ply metric is insufficient as a whole-search/strength proxy. The evidence does **not** justify immediately replacing it with a multi-edge objective: multi-edge errors can also improve while search remains expensive, the teacher is not a tactical oracle, and no universal after-first-edge collapse is found.

## 7. Existing-game strength attribution: blocked divergence, bounded substitute

A's 1,024 candidate-validation and 256 final-arena outcomes were retained. However, `Campaign.ArenaLog` writes only game-boundary ValidationProgress plus accumulated node/time counters. `ValidationResult.Game` stores termination and plies; `ValidationRecord` explicitly omits raw games. No PGN/move stream, intermediate FEN sequence, PV or per-move evaluation series was found in the retained campaign tree. The last-move progress contains depth/nodes/time, not a recoverable board or move sequence.

Consequently **first persistent evaluation swing, first irreversible material change, and positions around actual loss divergence are unavailable**. That part stops under the user's stop condition. Six-thread games cannot be honestly reconstructed from seed/outcome alone, and no new game/arena was run. We cannot distinguish tactical blunders from broader strategic generalization on those original trajectories.

The explicitly permitted smallest substitute uses only retained **opening boards**, never invented divergence positions. Selection is deterministic: first decisive candidate loss by pair index then candidate-White/candidate-Black order, separately for L0-G4's normal g138 match and L2-G4's final g139 match. Both are zero-based pair index 1: L0 as Black, lost in 76 played plies; L2 as White, lost in 64. Their opening identities exactly match A/arena-openings.jsonl. D/selected-losses.json retains IDs, hashes, board arrays, results and selection. Each of g134/L0-G4/L2-G4 received two identical fixed-position depth-4 one-worker searches, with all six repeats exact. This is a new evaluation at an old opening, not replay of either six-worker game.

| Loss-selected opening | Model | Static STM | Teacher STM | Qnodes | Root score | Move |
| --- | --- | --- | --- | --- | --- | --- |
| L0-G4 | g134 | 0.0626 | -0.3662 | 3301 | 13094 | f6h4 |
| L0-G4 | L0-G4 | 0.1044 | -0.3662 | 2928 | 21886 | f6h4 |
| L0-G4 | L2-G4 | -0.0685 | -0.3662 | 3068 | 12306 | f6h4 |
| L2-G4 | g134 | 0.1094 | -0.0645 | 290 | 5337 | b1c3 |
| L2-G4 | L0-G4 | 0.0387 | -0.0645 | 459 | -626 | h2h3 |
| L2-G4 | L2-G4 | -0.0418 | -0.0645 | 747 | -4444 | d2d4 |

At the L0-loss opening all three choose f6h4, with large score differences but similar qnodes. At the L2-loss opening they choose b1c3/h2h3/d2d4 and disagree in score sign. L2 is closest to the teacher statically on both selected boards yet still belongs to a losing recorded endpoint. This establishes evaluation/root-choice sensitivity already at known openings; it does not identify which original move lost either game. Lower/higher qnodes at one opening cannot establish a search-efficiency cause of a complete-game loss.

## 8. Objective versus playing strength

Only eight candidates have normal measured scores versus the common g134 incumbent. g134 is the reference, not a measured 50% ninth data point. Across the eight, descriptive Spearman rank correlations of **higher metric value** with score are base loss **+0.167**, delta median **−0.214**, delta Huber **−0.190**, qnodes **+0.405**. No p-values or significance claims are appropriate: endpoints share parents/data, games are paired, and generation opening sets differ.

The ordinal counterexamples are more informative. L0-G1 has much better base/delta/qnode metrics than g134 but scores 33.98%; L0-G3 has the worst fixed base loss and highest qnodes yet scores 58.98%; L2-G4 improves base loss and qnodes over L2-G3 while normal score falls 62.11%→36.72%. G4 L2 improves base/delta metrics versus L0 but scores worse in that generation's validation, then wins their separate direct arena. These are bounded, opponent/opening-dependent results, not a reliable universal ordering.

The ordinary objective demonstrably optimizes a proxy that can move opposite to strength. Capture-delta error has some favourable matched-arm associations (G1–G3 and final direct arena), but fails as an acceptance gate by itself. Qnode reduction is a cost measure, not a strength measure; very cheap cutoffs may follow a shifted value distribution rather than more correct evaluation. The objective has no root-move, minimax-order or playing-outcome guarantee. The teacher is a fixed target component, not an adjudicator of correct play.

## 9. Ranked attribution and smallest next intervention

| Rank / category | Support | Boundary |
| --- | --- | --- |
| 1 — BASE OBJECTIVE / STRENGTH MISALIGNMENT (2) | Immediate L0-G1 loss improvement with 33.98% score; G3→G4 losses/cost improve while score declines; pre-existing historical volatility | Strong demonstration of proxy failure; specific losing decisions unavailable |
| 2 — SEARCH-SCORE GEOMETRY (7) | sv-17 same-window 4,395 vs 632 asymmetric cutoff disagreements and excess deep continuations; Fianchetto entry-count mechanism | Concrete observed mechanism for cost; no universal offset or substituted-tree causal effect proved |
| 3 — DATA-DISTRIBUTION INSTABILITY (5) | Draw/endgame/capture/target-width shifts; same-weight G4 gradient norms higher | Measured contributor candidate, not isolated causal treatment |
| 4 — ONE-PLY OBJECTIVE INSUFFICIENT (4) | sv-17 global-to-local ranking reversal; some recapture/third-edge defects; Fianchetto costs worse despite better deltas than L0 | Supports missing context/coverage; does not establish multi-edge training as a fix |
| Overall — INCONCLUSIVE / MULTIFACTOR (8) | Strength and relative qsearch reversal have different observed trajectories and mechanisms | Game divergence missing; live-store integrity failed |
| Not leading — OPTIMIZER / UPDATE INSTABILITY (6) | Substantial per-generation movement, particularly early head changes | No G4 norm/moment discontinuity, nonfinite values, reset or auxiliary takeover |
| Not supported — TRAINING REGIME MISMATCH (1) | Same evidenced immediate source/target/update regime | Seeds/incumbent changed intentionally; historical early promotion differs |
| Not established — AUXILIARY OVER-REGULARIZATION (3) | Local G4 regressions and larger Adam second moments exist | L0 also fails retention; direct L2/L0 arena favours L2; chain medians generally improve |

Lambda=2 is **not exonerated for every position**, but the evidence more strongly supports it mitigating parts of a general continuation problem than creating the entire g134 deficit. Its adverse G4 normal-validation contrast prevents claiming unqualified mitigation. The original four-generation result remains mixed.

**Smallest next scientific step: no additional training yet.** First restore an interpretable, quiescent store boundary and resolve the missing-game evidence gap. If original move records exist elsewhere, recover them and analyze the earliest indexed decisive losses. If they do not, separately authorize a small fixed-checkpoint diagnostic on the two already-selected opening pairs, with full move/board/static/search telemetry for g134 versus each G4 endpoint (two colour-reversed pairs, four games total). These would be newly logged diagnostic games, explicitly **not** reconstructed historical games or a new full arena. Fixed checkpoints and settings isolate decision behavior without adding another learning treatment. Examine teacher disagreement and static/root-move changes around a predeclared persistent swing/material event before choosing a training change.

For a later training intervention, these results favour investigating source/target stability and retention/early-stop policy before changing lambda or adding chain training. That is a hypothesis ordering, not authorization or an evidence-established fix. Do not launch another four-generation campaign. A single controlled data/target change would only be justified after the above decision-level evidence discriminates the cause.

Rejected or unsupported as sole explanations: corrupted frozen payloads; wrong pinned teacher; optimizer reset/LR change; synthetic-child base targets; uniform output saturation; a G4 auxiliary-gradient explosion; more qsearch root invocations as the sv-17 arm-difference explanation; a changed final root move as the sole sv-17/Fianchetto G4 explanation; monotonically worsening strength; and “fewer qnodes means stronger play.” The fixed probes show some saturation in selected contexts, but no broad saturation failure is established. No calibration, score shift, search change or objective alteration was applied.

## 10. Validation, concurrent mutation and completion boundary

Executed: external Java 21 compilation of current production source; 134 checksummed plan decodes; 135 linked manifests; four historical plus four frozen dataset reads; nine complete state/model round trips; all 25 frozen manifest hashes and 43 hash-bearing acceptance-report rows; eight binary candidate-validation/JSONL statistic equalities; all parameter-group/moment measurements; 512 fixed-state analytic gradient rows and six finite differences; 744 capture-chain prefixes and signed cumulative-error identities; **74 bounded search requests** (30 plain/30 trace, two cross-shadow, twelve loss-opening evaluations including six repeats). Outcomes: **70 completed**, **4 node-capped** (plain/trace at the two G3 Kiwipete checkpoints), zero time caps. Thirty main trace equalities, two additional shadow equalities and six repeat equalities passed. No full software suite was run.

Final full inventories compared **553 repository baseline files**, **1,544 acceptance-root files** and **742 NNUE-store files** with zero differences. Frozen datasets, held/search corpora, every frozen model/state and all existing arena/candidate outputs remained exact. Current production source is unchanged. This verifies read-only preservation of the scientific artifacts actually used.

The **production BRN-store requirement did not pass**. Starting inventory had 993 files; the final attempted inventory observed 1,001, with **23 differing path entries**, including an unreadable `store.lock`. The first final hash check found g134's plan changed from `c0a61a2583acd182478d4afe4ceaa521d05170f67ce50287fadb5ed159973a78` to `e54aa3e2614f6d24a8cb918ac67f44aee14ba957d389ed2d7880fbd8b06100c3`; observed modification time was `2026-09-24T11:25:48.746875Z`. The starting plan had already been decoded and hashed before this change. The active plan and data bytes both changed; their archived originals remain exact. An initial final inventory raised PermissionError while hashing a live file; a subsequent reporting inventory recorded the error explicitly, rather than dropping the file or passing the audit.

The original g134 checkpoint, plan/data, attempt and partial-generation metadata were moved under `restarted-generations/38cc24c1-8383-4a27-adcb-b957a6f1bfdc/`. All **eight archived baseline files** present there match starting size/SHA-256 exactly, including original g134 model/state. A replacement `g000134-s000218158-8bd4c2f8d37973f749cca86dc9f5fe673b568d9158af96d26e1f12d28f72f491` and changed Latest/attempt metadata were observed. These are different checkpoint identities despite equal generation/step labels. They were not used for analysis. The external frozen g134 copy remains the authoritative diagnostic model.

D/store-change-evidence.json preserves every changed path, old/new hashes, read errors and archive match. D/integrity-result.json records the **failed** aggregate check. A Java process starting during this turn was observed, but this diagnostic does not establish who initiated it or its precise operation. No command in this task starts/resumes training, writes any store, moves checkpoints, or kills/pauses another process. The user was asked to identify/pause the writer. No integrity pass is inferred from silence, and no missing source decision is waived.

Following discovery, only evidence reconciliation/reporting continued; no further searches, derivative probes, training or store repair ran. The historical g134 source distribution and plan reconstruction were already captured before replacement; historical game-opening reconstruction used retained settings/seeds. All experiment measurements use unchanged A checkpoint/data bytes, so the live-store incident does not alter those frozen results. It does block a claim that the entire production environment remained unchanged.

Deliberately skipped: new training/optimizer updates/checkpoints; source/self-play regeneration; full arena or candidate-validation reruns; new games; full 24-suite reruns at intermediate checkpoints; full software suites; GUI/browser QA; production source/search/qsearch/score-mapping edits; calibration; online gradient-update replay; restoration or reconciliation of the live store; push/deployment. Missing original game trajectories were not fabricated. The bounded opening substitute does not satisfy the unavailable divergence attribution.

Report is the only intended repository addition. The inherited dirty files remain byte-identical to the starting inventory and are excluded from any report-only commit. No Git reset, stash, clean, history rewrite or production promotion occurred. Report-only commit identity, if safely created, is supplied in the completion message; it is not an acceptance claim.

**Human actions required after this prompt:**

- **Blocking for a stable production-store integrity audit and any dependent training:** identify the concurrent BRN-store writer, stop/pause it, and explicitly reconcile the replacement g134/Latest lineage with the intended frozen baseline. Do not infer that the equal generation/step label denotes the original checkpoint. This diagnostic does not authorize rollback or deletion.
- **Blocking for original-game divergence attribution:** supply/recover the original move/position records if they exist; otherwise explicitly approve the separate small logged diagnostic described above. No new full arena or training is requested or authorized by this report.

These actions do not block reading the completed frozen-artifact findings. Acceptance and the next experiment decision remain with the user.
