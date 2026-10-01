# BRN capture-consistency multi-generation acceptance — 2026-09-24

**B. ACCEPTANCE MIXED** Lambda=2 retains meaningful local-delta and ordinary-loss benefits and wins the primary matched-control arena, but its G4 qsearch advantage reverses, campaign runtime increases, and playing strength against untouched g134 is not retained. The conditions for positive acceptance and advancement beyond a default-off experimental option are unmet.

**The production default remains lambda=0.** No GUI/default, production source, search/qsearch, calibration or production-lineage change is authorized or performed. All experimental checkpoints, game data and harnesses are external.

## Baseline and isolation

Authoritative repository: `C:\projects\seed\java\seedv6`. Inspected HEAD: `43836daf4c55100357005477028b44e8e2c18bb0`. The **current accepted dirty working tree**, containing the accepted capture-consistency integration and inherited user work, is the execution baseline. No clean-HEAD replacement was used. The integration report was read first, followed by all seven named accepted diagnostic reports and the scoped search contract. Java 21 compiled the current production sources into the external evidence directory. Existing `app/bin` was not used or overwritten.

External evidence root: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-multigen-20260924`. `baseline.json` records all **552** initial tracked/non-ignored file hashes, HEAD and full porcelain status; `inherited.patch` preserves the starting tracked diff. `production-stores-before.json` hashes all files in the real BRN t2 and NNUE training stores. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent; neither capability was activated or created. No on-disk ancestor/repository AGENTS file was found.

Isolated arms: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-multigen-20260924\L0` and `C:\Users\Central\AppData\Local\Temp\seedv6-brn-multigen-20260924\L2`. The teacher is pinned in `C:\Users\Central\AppData\Local\Temp\seedv6-brn-multigen-20260924\teacher-store`. Both independent experimental lineages explicitly bootstrap the exact g134 model/optimizer at generation 134, with an empty experimental parent and the same initial Best/Latest. This changes the experimental manifest identity, **not payload bytes**. It does not copy the live g127 Best or reconcile the real store’s unfinished g134 validation. This initialization boundary was preregistered. Thereafter all candidates follow the production service’s GAME_PAIRS validation/promotion, checkpoint publication, history and resumable generation machinery. No isolated promotion is production promotion.

The current service always resumes training from durable Latest, including after RETAIN. Consequently normal validation does not cause either arm to roll back its optimizer parent; the four-generation sequence is also the fixed-sequence analysis. Incumbent Best may differ across arms if their actual isolated decisions differ.

## Exact starting identities

| Artifact | Identity / SHA-256 |
| --- | --- |
| Original g134 directory | E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c |
| Generation / optimizer step / depth | 134 / 218,158 / 4 |
| network.brn2 | 21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924 |
| training.state — full weights, moments and optimizer config | ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9 |
| Teacher checkpoint | g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6 |
| Teacher original path | E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6\network.nnue |
| Teacher model SHA-256 | 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9 |
| g134 generation plan SHA-256 | c0a61a2583acd182478d4afe4ceaa521d05170f67ce50287fadb5ed159973a78 |

Canonical BRN-2/schema 2, width 32, 1,645,665 binary64 parameters. Saved Adam is restored unchanged: learning rate 0.001, beta1 0.9, beta2 0.999, epsilon 1e-8, both moment arrays, global step 218,158. Direct codec round trips prove g134 `network.brn2` equals the inference snapshot from `training.state`; both arm initialization manifests have identical model/training hashes. The live NNUE Best is now g121; using it would change targets. The isolated teacher Best instead points to g74’s original accepted promotion evidence, with full copied payload/metadata dependencies. No live teacher reference was changed.

## Frozen protocol, data and seeds

Campaign root seed **2026092404**. Preregistration SHA-256 `1ff7c86fec7150ec35d2c966ab802b3014ea094420b50f5763327d73df9b59ca`, recorded before data generation or checkpoint evaluation. `frozen-manifest.json` was recorded at `2026-09-24T07:09:07.535637+00:00`, after all data and holdout exclusions and before the first g134 sentinel. Its inputs are rechecked between generations.

For generation g=135,136,137,138, each domain is `new SplittableRandom(root XOR salt XOR (g * 0x9E3779B97F4A7C15L)).nextLong()` with signed 64-bit overflow; both master and data seeds equal the campaign root. Domain salts are SELF_PLAY `0x6A09E667F3BCC909`, SHUFFLE `0xBB67AE8584CAA73B`, VALIDATION `0x3C6EF372FE94F82B`, HOLDOUT `0xA54FF53A5F1D36F1`, CAPTURE `0x510E527FADE682D1`. The native game/opening seed is `SplittableRandom(sourceSeed + 0x9E3779B97F4A7C15L * gameIndex).nextLong()`. Python independently reproduces all Java domain seeds. Final primary/secondary openings use the VALIDATION seed at index g139, **-4982725657129802987**. No seed is changed after results.

| Generation | Source | Partition | Shuffle | Capture | Validation |
| --- | --- | --- | --- | --- | --- |
| 135 | 3119407140618305967 | 1834838130905193016 | 3830395182778966310 | -6068338275065664667 | 6721616228235806581 |
| 136 | -5516991822479949012 | -8502361611866181555 | -505075224019552646 | -6515964810486797278 | -2893709098066606436 |
| 137 | 7530041731565897700 | -1461729739301225705 | 8065480552726592149 | 6661854323993036340 | 5484994828715069358 |
| 138 | -3376768107157059872 | -2475383883295127842 | -7266758173494923948 | -1041972056533135401 | -7111262491610933850 |

The authenticated g134 path is **HANDCRAFTED**, independent of BRN. Each generation uses one new unconditioned native `SelfPlayBatch.generateHandcrafted` batch: 64 games, depth 4, six root workers, standard chess start, 0–8 random opening plies, up to 32 evenly spaced samples per completed game, maximum 1,024 plies, no per-move node/time limit. Native `BootstrapPartition.split` reserves whole games. Both arms receive the exact same partition, rows, WDL labels, bounded teacher values, blended targets and Fisher–Yates order. The four batches are distinct; no historical or first-generation batch is replayed as later training data. All four are generated/frozen before any checkpoint evaluation.

The ordinary objective remains `t=0.5*w+0.5*n_parent`, `0.5*(y_parent-t)^2`. Lambda=2 adds exactly the accepted `2*I(pair)*Huber_0.25(-y_child-y_parent-0.5*(-n_child-n_parent))`. The child has no ordinary target/update. Eligible captures and the independent CAPTURE stream come directly from the integrated production relation builder. One epoch, online minibatch 1; only lambda differs between arm training settings. Source/partition data are installed into each normal checksummed BootstrapPlan/Data record before the production service runs; plan/frame hashes differ as required by arm identity/lambda, while actual examples and targets are identical.

| k / native generation | Source W/D/B | All samples | TRAIN / updates per arm | Native HELD | Data seconds |
| --- | --- | --- | --- | --- | --- |
| 1 / 135 | 27/7/30 | 2043 | 1627 | 416 | 32.956 |
| 2 / 136 | 26/6/32 | 2048 | 1632 | 416 | 30.166 |
| 3 / 137 | 21/11/32 | 2025 | 1632 | 393 | 29.386 |
| 4 / 138 | 29/2/33 | 2042 | 1626 | 416 | 27.339 |

Total: **256 fresh completed source games, 8,158 sampled observations, 6,517 updates per arm** over four generations; verified final optimizer step **224,675**. Short completed games retain their natural sample counts. No source game was capped, failed, relabeled or regenerated. Shared source execution took **119.847 seconds**, incurred once physically and charged identically to each logical campaign for cost comparisons.

Fresh executions do not imply every position is new. Unique TRAIN parent keys by generation are 1,500 / 1,419 / 1,326 / 1,478; pairwise cross-generation intersections range from 32 to 96 keys. All 256 indexed source-game seeds are distinct. These natural opening/transposition repetitions are retained to preserve ordinary campaign sampling; they are not replay of the same dataset. A new exhaustive historical exposure audit is not claimed.

## Frozen evaluation populations and bounds

The fixed capture corpus is the replication’s **107 genuinely novel HELD pairs from five source games**, including White wins, draws and Black wins. All 107 remain after excluding every parent/selected-child endpoint in all four current TRAIN datasets; no overlap removal was necessary. Corpus SHA-256: `fc084a202de2a5cda88eb00149aff6c3dde1e5a4aef446324483a1222d1a86c2`. It is unchanged for g134 and every checkpoint. Errors compare continuous predicted common-parent delta with **half** the bounded NNUE delta; quantiles interpolate at (n-1)*p, unsupported large error means abs(error)>0.5, sign agreement compares mathematical signs. Fixed ordinary loss is evaluated on these same real parents with their original WDL/teacher blend. Native per-generation holdouts are also evaluated; unlike the fixed holdout, their populations change between generations. Pair/source correlations and only five held games limit generalization.

The deterministic eight-position sentinel is **sv-05, sv-06, sv-10, sv-15, sv-16, sv-17, sv-20, sv-23**. Selection uses only SHA-256 of UTF-8 `2026092404|sentinel|ID|KEY`: take the smallest hash in each outcome × T/Q cell, then add T and Q from the first two distinct outcomes ordered by SHA-256 of `2026092404|sentinel-extra|OUTCOME`. No prior improvement/regression value is read by this selector. Coverage is 2 White-win / 3 draw / 3 Black-win and 4 tactical / 4 quieter positions.

Search uses the exact accepted settings: depth 4, one worker, fresh 262,144-entry TT/order state, singleton history, cumulative 1,000,000 entered-node and 60-second request caps, original search/qsearch/score mapping. Primary measurements are trace-free; a secondary sparse-shadow `QsearchDecisionTrace` replay supplies exact eligible stand-pat attempts, with all nontiming counters/moves/scores required to match. Qnodes count entered qsearch children; SP rate is cutoffs/eligible attempts. The full 24-position suite is unaltered, SHA-256 `9aaad2d4cb46f4409ec3dfbe9d2af3732a5254a1ae18ea98482171197cf494b1`, and has zero root overlap with current campaign TRAIN endpoints. Full suite and continuity fixtures run only at the final comparison; the eight-position sentinel runs at each checkpoint.

Normal candidate validation and final arenas use the **authenticated g134 generation-attempt contract: 64 opening pairs / 128 games, depth 4, six workers, 0–8 opening plies, maximum 1,024 played plies**, `PromotionPolicy(64,0.05,0)`. The original g134 validation was unfinished, so its generation-attempt settings—not a nonexistent completed validation record—supply this contract. Native paired openings are identical with candidate colours reversed. This uses 64 rather than 32 pairs to preserve the established minimum-pair promotion/comparison contract. Intermediate generations compare their candidate with isolated Best; the final primary compares L2-G4 directly with L0-G4.

Each normal generation and final arena has a preregistered one-hour outer runtime bound; no per-move setting is changed. Source batches were monitored against a planned ten-minute resource limit; each finished in 27–33 seconds, so no watchdog cancellation was needed. Poor finite results do not stop or extend the four-generation design. The separate unusable-control condition is at least four of eight sentinel depth failures for two consecutive control checkpoints. Arm execution order alternates (L0 first G1/G3, L2 first G2/G4), sequentially. Secondary L2-G4 versus g134 runs only if primary wall time is at most 30 minutes.

## Training and held-out trajectory

| Checkpoint | Updates | Auxiliary pairs trained | Online base | Unweighted aux / base row | Train loss before | Train loss after | Native held base | Fixed held base |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L0-G1 | 1627 | 0 | 0.071374 | 0.000000 | 0.094892 | 0.033572 | 0.081260 | 0.082485 |
| L2-G1 | 1627 | 1038 | 0.074770 | 0.016902 | 0.094892 | 0.047319 | 0.078763 | 0.086078 |
| L0-G2 | 1632 | 0 | 0.064101 | 0.000000 | 0.080739 | 0.033027 | 0.062546 | 0.107676 |
| L2-G2 | 1632 | 1018 | 0.066287 | 0.014323 | 0.080553 | 0.046299 | 0.065644 | 0.097493 |
| L0-G3 | 1632 | 0 | 0.054724 | 0.000000 | 0.069525 | 0.030343 | 0.060129 | 0.110196 |
| L2-G3 | 1632 | 905 | 0.057458 | 0.012534 | 0.066620 | 0.038993 | 0.057837 | 0.089595 |
| L0-G4 | 1626 | 0 | 0.058442 | 0.000000 | 0.076261 | 0.030268 | 0.109358 | 0.088740 |
| L2-G4 | 1626 | 996 | 0.061049 | 0.012486 | 0.072937 | 0.041080 | 0.102942 | 0.082795 |

Each real parent produces one optimizer update. A selected synthetic child contributes only to the joint auxiliary gradient for that update; it does not add an ordinary target or a separate optimizer step.

Auxiliary online loss is obtained by an independent diagnostic replay from each generation’s preceding exact state, using the frozen production relations/order. Replay must reproduce **all final training-state bytes**, not just predictions. Its diagnostic time is excluded from production-training cost. Weighted objective contribution is twice the listed auxiliary value for L2; L0 contributes zero.

| Checkpoint | Fixed base loss | Delta median | p90 | p95 | Error >0.5 | Sign agreement | Mean STM |
| --- | --- | --- | --- | --- | --- | --- | --- |
| g134 | 0.109080 | 0.232868 | 0.592255 | 0.737772 | 17.76% | 66.36% | -0.039926 |
| L0-G1 | 0.082485 | 0.201500 | 0.493080 | 0.680271 | 10.28% | 63.55% | 0.018202 |
| L2-G1 | 0.086078 | 0.148752 | 0.394018 | 0.490440 | 4.67% | 62.62% | 0.079282 |
| L0-G2 | 0.107676 | 0.202989 | 0.442409 | 0.501317 | 5.61% | 61.68% | 0.094048 |
| L2-G2 | 0.097493 | 0.201325 | 0.460217 | 0.519308 | 6.54% | 55.14% | 0.133541 |
| L0-G3 | 0.110196 | 0.299550 | 0.695224 | 0.743300 | 21.50% | 62.62% | -0.020457 |
| L2-G3 | 0.089595 | 0.150218 | 0.384592 | 0.430706 | 4.67% | 64.49% | 0.032809 |
| L0-G4 | 0.088740 | 0.206184 | 0.595569 | 0.646295 | 15.89% | 63.55% | 0.050805 |
| L2-G4 | 0.082795 | 0.164223 | 0.414442 | 0.474816 | 3.74% | 62.62% | 0.059631 |

## Sentinel search trajectory

All **28** untouched g134 final searches (the full 24 plus four fixtures) exactly reproduce the accepted earlier diagnostic results for the compared non-timing counters, scores and moves (`g134-final-reproduction.json`). The initial eight-position sentinel also reproduced them exactly.

| Checkpoint | Qnodes | Stand-pat cuts / attempts | SP rate | Caps | Trace-free seconds |
| --- | --- | --- | --- | --- | --- |
| g134 | 111874 | 72284/130605 | 55.35% | 0 | 1.211 |
| L0-G1 | 63927 | 64100/92080 | 69.61% | 0 | 1.114 |
| L2-G1 | 47240 | 54941/74424 | 73.82% | 0 | 0.893 |
| L0-G2 | 64240 | 63888/91603 | 69.74% | 0 | 1.038 |
| L2-G2 | 42676 | 53181/70360 | 75.58% | 0 | 0.913 |
| L0-G3 | 306406 | 134842/306152 | 44.04% | 0 | 2.814 |
| L2-G3 | 156349 | 91864/170259 | 53.96% | 0 | 1.514 |
| L0-G4 | 52296 | 58779/80819 | 72.73% | 0 | 1.026 |
| L2-G4 | 67509 | 59130/89289 | 66.22% | 0 | 1.085 |

### g134

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 16386 | 65.10% | 4 / COMPLETED | 5587 | g3g5 |
| sv-06 | 10165 | 57.83% | 4 / COMPLETED | 2521 | c3d5 |
| sv-10 | 779 | 57.85% | 4 / COMPLETED | 4818 | e6e7 |
| sv-15 | 5215 | 74.21% | 4 / COMPLETED | -7569 | f2f4 |
| sv-16 | 423 | 79.65% | 4 / COMPLETED | 2497 | f5g4 |
| sv-17 | 55182 | 51.89% | 4 / COMPLETED | 6066 | d8c7 |
| sv-20 | 1165 | 67.01% | 4 / COMPLETED | 17896 | f3g4 |
| sv-23 | 22559 | 44.73% | 4 / COMPLETED | 99 | b8c6 |

### L0-G1

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 19318 | 68.15% | 4 / COMPLETED | 2245 | c2c3 |
| sv-06 | 6022 | 71.17% | 4 / COMPLETED | 8548 | f3e4 |
| sv-10 | 787 | 52.53% | 4 / COMPLETED | 4667 | e6e7 |
| sv-15 | 2842 | 89.08% | 4 / COMPLETED | -3858 | f2f4 |
| sv-16 | 377 | 73.81% | 4 / COMPLETED | 942 | f5g4 |
| sv-17 | 24775 | 71.92% | 4 / COMPLETED | 10138 | f7f6 |
| sv-20 | 529 | 45.71% | 4 / COMPLETED | 17378 | f3g4 |
| sv-23 | 9277 | 61.31% | 4 / COMPLETED | 1509 | b8c6 |

### L2-G1

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 8824 | 81.02% | 4 / COMPLETED | 1814 | d6b8 |
| sv-06 | 5283 | 75.61% | 4 / COMPLETED | 10195 | f3e4 |
| sv-10 | 592 | 62.96% | 4 / COMPLETED | 7415 | e6e7 |
| sv-15 | 2320 | 91.08% | 4 / COMPLETED | -3021 | f2f4 |
| sv-16 | 358 | 80.40% | 4 / COMPLETED | 188 | f5g4 |
| sv-17 | 24082 | 68.46% | 4 / COMPLETED | 7090 | f7f6 |
| sv-20 | 344 | 46.93% | 4 / COMPLETED | 15823 | f3g4 |
| sv-23 | 5437 | 75.11% | 4 / COMPLETED | 1538 | b8c6 |

### L0-G2

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 17305 | 70.38% | 4 / COMPLETED | 7657 | a2a4 |
| sv-06 | 6305 | 75.39% | 4 / COMPLETED | 5458 | f1g1 |
| sv-10 | 419 | 55.21% | 4 / COMPLETED | 8845 | e6e7 |
| sv-15 | 2727 | 91.05% | 4 / COMPLETED | -7495 | f2f4 |
| sv-16 | 342 | 80.68% | 4 / COMPLETED | 24 | f5g4 |
| sv-17 | 19849 | 73.72% | 4 / COMPLETED | 9107 | g7g6 |
| sv-20 | 365 | 45.00% | 4 / COMPLETED | 20298 | f3g4 |
| sv-23 | 16928 | 55.60% | 4 / COMPLETED | 1581 | b8c6 |

### L2-G2

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 10763 | 72.31% | 4 / COMPLETED | 7118 | d3c4 |
| sv-06 | 5889 | 75.45% | 4 / COMPLETED | 4404 | f3e4 |
| sv-10 | 441 | 58.00% | 4 / COMPLETED | 5824 | e6e7 |
| sv-15 | 3514 | 90.85% | 4 / COMPLETED | -2650 | f2f4 |
| sv-16 | 349 | 78.35% | 4 / COMPLETED | 245 | f5g4 |
| sv-17 | 14123 | 78.99% | 4 / COMPLETED | 4734 | g7g6 |
| sv-20 | 322 | 46.47% | 4 / COMPLETED | 17757 | f3g4 |
| sv-23 | 7275 | 66.66% | 4 / COMPLETED | -66 | b8c6 |

### L0-G3

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 25537 | 55.30% | 4 / COMPLETED | 1317 | g3f4 |
| sv-06 | 6616 | 58.10% | 4 / COMPLETED | -235 | f1g1 |
| sv-10 | 951 | 50.17% | 4 / COMPLETED | 6307 | e6e7 |
| sv-15 | 4382 | 77.75% | 4 / COMPLETED | -5893 | f2f4 |
| sv-16 | 547 | 60.52% | 4 / COMPLETED | -278 | f5g4 |
| sv-17 | 250376 | 40.21% | 4 / COMPLETED | 4683 | f8e7 |
| sv-20 | 645 | 47.68% | 4 / COMPLETED | 16702 | f3g4 |
| sv-23 | 17352 | 49.92% | 4 / COMPLETED | -502 | b8c6 |

### L2-G3

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 18338 | 63.47% | 4 / COMPLETED | 2132 | g3f4 |
| sv-06 | 5181 | 74.98% | 4 / COMPLETED | 656 | f1g1 |
| sv-10 | 495 | 49.13% | 4 / COMPLETED | 3451 | e6e7 |
| sv-15 | 3774 | 76.90% | 4 / COMPLETED | -3826 | f2f4 |
| sv-16 | 491 | 72.58% | 4 / COMPLETED | 54 | f5g4 |
| sv-17 | 115639 | 48.71% | 4 / COMPLETED | 2805 | f7f6 |
| sv-20 | 534 | 43.07% | 4 / COMPLETED | 15416 | f3g4 |
| sv-23 | 11897 | 56.02% | 4 / COMPLETED | 2074 | b8c6 |

### L0-G4

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 13320 | 72.64% | 4 / COMPLETED | 1450 | g3f4 |
| sv-06 | 5184 | 68.71% | 4 / COMPLETED | -6662 | f1g1 |
| sv-10 | 442 | 55.82% | 4 / COMPLETED | 9599 | e6e7 |
| sv-15 | 3988 | 89.47% | 4 / COMPLETED | -5672 | f2f3 |
| sv-16 | 245 | 87.09% | 4 / COMPLETED | 894 | f5g4 |
| sv-17 | 18459 | 72.29% | 4 / COMPLETED | 3815 | g7g6 |
| sv-20 | 466 | 43.83% | 4 / COMPLETED | 17262 | f3g4 |
| sv-23 | 10192 | 68.95% | 4 / COMPLETED | 682 | b8c6 |

### L2-G4

| Position | Qnodes | SP rate | Depth / status | Score | Move |
| --- | --- | --- | --- | --- | --- |
| sv-05 | 11735 | 70.77% | 4 / COMPLETED | 1718 | d6b8 |
| sv-06 | 4366 | 73.22% | 4 / COMPLETED | -2822 | f3e4 |
| sv-10 | 483 | 55.18% | 4 / COMPLETED | 4729 | e6e7 |
| sv-15 | 5039 | 83.15% | 4 / COMPLETED | -4558 | g5e3 |
| sv-16 | 282 | 86.88% | 4 / COMPLETED | 896 | f5g4 |
| sv-17 | 38572 | 55.71% | 4 / COMPLETED | 2392 | g7g6 |
| sv-20 | 460 | 44.41% | 4 / COMPLETED | 15970 | f3g4 |
| sv-23 | 6572 | 78.69% | 4 / COMPLETED | 1932 | h7h6 |

## Final 24-position acceptance and continuity

L0-G4 → L2-G4: **160546 → 187720 qnodes (16.93% increase)**; median ratio 1.041333; 12 improved / 12 regressed / 0 tied. Aggregate SP 68.91% → 65.69%. Untouched g134: 296424 qnodes, SP 56.05%.

Largest saving: `sv-18`, 4626 qnodes. Largest absolute regression: `sv-17`, +20113. Largest relative regression: `sv-17`, 2.089604×. Removing the largest saving gives 146279 → 178079 (21.74% increase). >2× regressions: ['sv-17']; new depth/cap losses: [].

| Descriptive accepted gate | Pass |
| --- | --- |
| median | False |
| improvements | False |
| aggregate | False |
| outlierRemoved | False |
| severeGuard | True |

These search gates are descriptive evidence. They do not independently establish campaign acceptance or playing-strength retention.

| Stratum | g134 q | L0 q | L2 q | Reduction | Median ratio | I/R/T | SP L0→L2 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| WHITE_WIN | 136638 | 67529 | 69257 | -2.56% | 0.858561 | 6/2/0 | 66.51%→64.15% |
| DRAW | 44221 | 27682 | 32248 | -16.49% | 1.207281 | 1/7/0 | 74.02%→72.26% |
| BLACK_WIN | 115565 | 65335 | 86215 | -31.96% | 0.915818 | 5/3/0 | 68.59%→63.63% |
| T | 218483 | 121863 | 159325 | -30.74% | 1.192983 | 5/7/0 | 69.03%→64.42% |
| Q | 77941 | 38683 | 28395 | 26.60% | 0.914666 | 7/5/0 | 68.61%→70.37% |

Negative values in the stratum reduction column mean increased qnodes.

### g134 final suite and continuity

| Position | Qnodes | SP cuts / attempts | SP rate | Depth / status | Root score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- |
| sv-01 | 47050 | 22238/48657 | 45.70% | 4 / COMPLETED | 146 / g4f2 | 0.6631 |
| sv-02 | 2631 | 2599/4439 | 58.55% | 4 / COMPLETED | -13947 / d7b5 | 0.0376 |
| sv-03 | 4499 | 3534/6923 | 51.05% | 4 / COMPLETED | -5558 / b5b4 | 0.0541 |
| sv-04 | 39357 | 26899/50848 | 52.90% | 4 / COMPLETED | -10112 / h8h5 | 0.4960 |
| sv-05 | 16386 | 13189/20260 | 65.10% | 4 / COMPLETED | 5587 / g3g5 | 0.1377 |
| sv-06 | 10165 | 9249/15994 | 57.83% | 4 / COMPLETED | 2521 / c3d5 | 0.1624 |
| sv-07 | 12944 | 10363/15963 | 64.92% | 4 / COMPLETED | -1113 / b6c7 | 0.1727 |
| sv-08 | 3606 | 6866/8500 | 80.78% | 4 / COMPLETED | -8463 / g8f6 | 0.0602 |
| sv-09 | 8073 | 7355/11755 | 62.57% | 4 / COMPLETED | 2766 / e6c4 | 0.1257 |
| sv-10 | 779 | 1444/2496 | 57.85% | 4 / COMPLETED | 4818 / e6e7 | 0.0103 |
| sv-11 | 9911 | 8067/14618 | 55.19% | 4 / COMPLETED | -1202 / c2c3 | 0.1268 |
| sv-12 | 2263 | 3994/5297 | 75.40% | 4 / COMPLETED | 5568 / d4b4 | 0.0683 |
| sv-13 | 14093 | 9842/18500 | 53.20% | 4 / COMPLETED | 19534 / e2e4 | 0.1685 |
| sv-14 | 3464 | 2934/5136 | 57.13% | 4 / COMPLETED | 6266 / a4a1 | 0.0424 |
| sv-15 | 5215 | 6163/8305 | 74.21% | 4 / COMPLETED | -7569 / f2f4 | 0.0909 |
| sv-16 | 423 | 861/1081 | 79.65% | 4 / COMPLETED | 2497 / f5g4 | 0.0037 |
| sv-17 | 55182 | 29431/56716 | 51.89% | 4 / COMPLETED | 6066 / d8c7 | 0.4887 |
| sv-18 | 10221 | 10166/15986 | 63.59% | 4 / COMPLETED | -10122 / g2g4 | 0.1093 |
| sv-19 | 6085 | 6183/11145 | 55.48% | 4 / COMPLETED | 10095 / c1d1 | 0.0720 |
| sv-20 | 1165 | 1288/1922 | 67.01% | 4 / COMPLETED | 17896 / f3g4 | 0.0094 |
| sv-21 | 16486 | 13303/20883 | 63.70% | 4 / COMPLETED | -2513 / f3e5 | 0.2648 |
| sv-22 | 3297 | 3538/5989 | 59.07% | 4 / COMPLETED | 8412 / g2h3 | 0.0425 |
| sv-23 | 22559 | 10659/23831 | 44.73% | 4 / COMPLETED | 99 / b8c6 | 0.1846 |
| sv-24 | 570 | 1079/1670 | 64.61% | 4 / COMPLETED | 7904 / c7e5 | 0.0061 |
| middlegame-kiwipete | 209297 | 80896/180919 | 44.71% | 4 / COMPLETED | 10036 / e1g1 | 1.5263 |
| quiet-fianchetto | 13007 | 8896/16509 | 53.89% | 4 / COMPLETED | -18683 / f3e5 | 0.1601 |
| opening-start | 220 | 2158/2913 | 74.08% | 4 / COMPLETED | -872 / b1c3 | 0.0355 |
| en-passant | 18 | 151/235 | 64.26% | 4 / COMPLETED | 0 / e5d6 | 0.0007 |

### L0-G4 final suite and continuity

| Position | Qnodes | SP cuts / attempts | SP rate | Depth / status | Root score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- |
| sv-01 | 24610 | 14116/26961 | 52.36% | 4 / COMPLETED | 396 / h7h6 | 0.4476 |
| sv-02 | 2009 | 2511/3599 | 69.77% | 4 / COMPLETED | -8615 / c8e8 | 0.0297 |
| sv-03 | 2966 | 2296/4334 | 52.98% | 4 / COMPLETED | -5202 / b5b4 | 0.0351 |
| sv-04 | 6613 | 8791/12330 | 71.30% | 4 / COMPLETED | -9534 / c7c6 | 0.1405 |
| sv-05 | 13320 | 12950/17827 | 72.64% | 4 / COMPLETED | 1450 / g3f4 | 0.1543 |
| sv-06 | 5184 | 6596/9600 | 68.71% | 4 / COMPLETED | -6662 / f1g1 | 0.1282 |
| sv-07 | 10627 | 13048/16717 | 78.05% | 4 / COMPLETED | 8354 / b6c7 | 0.1766 |
| sv-08 | 2200 | 3203/4130 | 77.55% | 4 / COMPLETED | -3802 / f5g6 | 0.0300 |
| sv-09 | 8619 | 8423/12260 | 68.70% | 4 / COMPLETED | 7568 / e6c4 | 0.1219 |
| sv-10 | 442 | 667/1195 | 55.82% | 4 / COMPLETED | 9599 / e6e7 | 0.0071 |
| sv-11 | 5027 | 5935/8833 | 67.19% | 4 / COMPLETED | 3385 / e4d3 | 0.0738 |
| sv-12 | 1598 | 3603/4516 | 79.78% | 4 / COMPLETED | 839 / b1c3 | 0.0442 |
| sv-13 | 5803 | 7541/10262 | 73.48% | 4 / COMPLETED | 24449 / h2h3 | 0.0991 |
| sv-14 | 1960 | 2383/3631 | 65.63% | 4 / COMPLETED | 9001 / a4a1 | 0.0381 |
| sv-15 | 3988 | 8500/9500 | 89.47% | 4 / COMPLETED | -5672 / f2f3 | 0.1148 |
| sv-16 | 245 | 695/798 | 87.09% | 4 / COMPLETED | 894 / f5g4 | 0.0025 |
| sv-17 | 18459 | 17548/24273 | 72.29% | 4 / COMPLETED | 3815 / g7g6 | 0.2139 |
| sv-18 | 14267 | 12792/20964 | 61.02% | 4 / COMPLETED | -10236 / g2g4 | 0.1285 |
| sv-19 | 4026 | 5156/7802 | 66.09% | 4 / COMPLETED | 15898 / c1a1 | 0.0541 |
| sv-20 | 466 | 575/1312 | 43.83% | 4 / COMPLETED | 17262 / f3g4 | 0.0063 |
| sv-21 | 14226 | 13468/19084 | 70.57% | 4 / COMPLETED | 5311 / a2a3 | 0.2129 |
| sv-22 | 3208 | 6224/7934 | 78.45% | 4 / COMPLETED | 6850 / c5c7 | 0.0528 |
| sv-23 | 10192 | 11248/16314 | 68.95% | 4 / COMPLETED | 682 / b8c6 | 0.1340 |
| sv-24 | 491 | 1272/1866 | 68.17% | 4 / COMPLETED | 12798 / c7e5 | 0.0074 |
| middlegame-kiwipete | 686443 | 269756/586332 | 46.01% | 4 / COMPLETED | -4470 / e5g6 | 4.7884 |
| quiet-fianchetto | 3964 | 13933/15291 | 91.12% | 4 / COMPLETED | -3729 / d1d2 | 0.1585 |
| opening-start | 299 | 1885/2653 | 71.05% | 4 / COMPLETED | -5324 / h2h3 | 0.0309 |
| en-passant | 5 | 164/292 | 56.16% | 4 / COMPLETED | 3214 / e1f2 | 0.0010 |

### L2-G4 final suite and continuity

| Position | Qnodes | SP cuts / attempts | SP rate | Depth / status | Root score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- |
| sv-01 | 31044 | 18057/33509 | 53.89% | 4 / COMPLETED | 688 / h7h6 | 0.5044 |
| sv-02 | 1461 | 1803/2663 | 67.71% | 4 / COMPLETED | -9014 / c8e8 | 0.0186 |
| sv-03 | 2595 | 2761/4417 | 62.51% | 4 / COMPLETED | -4071 / b5b4 | 0.0310 |
| sv-04 | 2202 | 4125/5130 | 80.41% | 4 / COMPLETED | -9419 / h8h5 | 0.0753 |
| sv-05 | 11735 | 10258/14494 | 70.77% | 4 / COMPLETED | 1718 / d6b8 | 0.1188 |
| sv-06 | 4366 | 6062/8279 | 73.22% | 4 / COMPLETED | -2822 / f3e4 | 0.0906 |
| sv-07 | 14115 | 12013/18463 | 65.07% | 4 / COMPLETED | 3329 / b6c7 | 0.2046 |
| sv-08 | 1739 | 3250/3972 | 81.82% | 4 / COMPLETED | -4411 / f5g6 | 0.0308 |
| sv-09 | 8532 | 8806/12789 | 68.86% | 4 / COMPLETED | 6202 / e6c4 | 0.1281 |
| sv-10 | 483 | 725/1314 | 55.18% | 4 / COMPLETED | 4729 / e6e7 | 0.0065 |
| sv-11 | 5653 | 7047/10237 | 68.84% | 4 / COMPLETED | 2689 / c2c3 | 0.0827 |
| sv-12 | 2091 | 4157/5171 | 80.39% | 4 / COMPLETED | -1168 / c1d2 | 0.0526 |
| sv-13 | 7556 | 9026/12890 | 70.02% | 4 / COMPLETED | 20645 / h2h3 | 0.1243 |
| sv-14 | 2612 | 3017/4508 | 66.93% | 4 / COMPLETED | 5369 / a4a1 | 0.0358 |
| sv-15 | 5039 | 7656/9207 | 83.15% | 4 / COMPLETED | -4558 / g5e3 | 0.1076 |
| sv-16 | 282 | 702/808 | 86.88% | 4 / COMPLETED | 896 / f5g4 | 0.0035 |
| sv-17 | 38572 | 22408/40222 | 55.71% | 4 / COMPLETED | 2392 / g7g6 | 0.3564 |
| sv-18 | 9641 | 8359/13970 | 59.84% | 4 / COMPLETED | -7867 / g2g4 | 0.0927 |
| sv-19 | 3400 | 5446/7682 | 70.89% | 4 / COMPLETED | 15141 / c1a1 | 0.0527 |
| sv-20 | 460 | 592/1333 | 44.41% | 4 / COMPLETED | 15970 / f3g4 | 0.0063 |
| sv-21 | 24512 | 18604/28629 | 64.98% | 4 / COMPLETED | 4983 / a4d1 | 0.3108 |
| sv-22 | 2519 | 5374/7070 | 76.01% | 4 / COMPLETED | 6024 / h2h4 | 0.0507 |
| sv-23 | 6572 | 10727/13632 | 78.69% | 4 / COMPLETED | 1932 / h7h6 | 0.1242 |
| sv-24 | 539 | 1151/1650 | 69.76% | 4 / COMPLETED | 9115 / c7e5 | 0.0056 |
| middlegame-kiwipete | 531245 | 221212/449786 | 49.18% | 4 / COMPLETED | -3248 / d5e6 | 3.7835 |
| quiet-fianchetto | 7171 | 12641/15543 | 81.33% | 4 / COMPLETED | -7139 / d1d2 | 0.1665 |
| opening-start | 224 | 1122/1798 | 62.40% | 4 / COMPLETED | -5363 / b1c3 | 0.0232 |
| en-passant | 23 | 306/393 | 77.86% | 4 / COMPLETED | 2621 / e1e2 | 0.0016 |

**Fianchetto is explicitly retained as a separate sensitivity fixture.** The regression improves in magnitude but persists: L0-G4 3,964 versus L2-G4 7,171 qnodes, a 1.8090x ratio (+80.90%), compared with the previous diagnostic ratio 3.8648x. Stand-pat rate is 91.12% versus 81.33%; both choose d1d2. Both are below untouched g134's 13,007 qnodes, but the lambda=2 disadvantage relative to the matched control has not changed sign. Kiwipete favours lambda=2 by 22.61% (686,443 to 531,245), although both remain much more expensive than g134's 209,297. Opening is 299 to 224; en-passant is 5 to 23 qnodes. The en-passant root changes from g134's e5d6/score 0 to e1f2/3214 for L0-G4 and e1e2/2621 for L2-G4; these behavioural changes are retained as observations, not asserted move-quality verdicts.

## Candidate quality, arena and computational cost

| Arm / generation | Incumbent generation | Candidate W/D/L | Valid pairs | Score | Decision | Training sec | Validation sec | Lifecycle sec excluding shared data |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L0-G1 | 134 | 41/5/82 | 64 | 0.33984375 | RETAIN_INCUMBENT | 0.720 | 1310.166 | 1316.976 |
| L0-G2 | 134 | 29/4/95 | 64 | 0.2421875 | RETAIN_INCUMBENT | 0.649 | 1151.847 | 1157.905 |
| L0-G3 | 134 | 72/7/49 | 64 | 0.58984375 | RETAIN_INCUMBENT | 0.545 | 1735.456 | 1741.539 |
| L0-G4 | 134 | 54/3/71 | 64 | 0.43359375 | RETAIN_INCUMBENT | 0.632 | 1254.387 | 1260.054 |
| L2-G1 | 134 | 54/4/70 | 64 | 0.4375 | RETAIN_INCUMBENT | 1.095 | 1488.822 | 1495.414 |
| L2-G2 | 134 | 41/6/81 | 64 | 0.34375 | RETAIN_INCUMBENT | 1.013 | 1212.755 | 1219.363 |
| L2-G3 | 134 | 76/7/45 | 64 | 0.62109375 | RETAIN_INCUMBENT | 0.925 | 1584.311 | 1590.149 |
| L2-G4 | 134 | 44/6/78 | 64 | 0.3671875 | RETAIN_INCUMBENT | 0.953 | 1352.432 | 1358.176 |

| Arm | Completed generations | Training seconds | Validation seconds | Shared source charged seconds | Logical campaign total seconds | Sentinel seconds |
| --- | --- | --- | --- | --- | --- | --- |
| L0 | 4 | 2.546 | 5451.857 | 119.847 | 5596.320 | 5.992 |
| L2 | 4 | 3.987 | 5638.320 | 119.847 | 5782.949 | 4.405 |

| Arm | Measured generation-process seconds | End-to-end seconds including shared source charge |
| --- | --- | --- |
| L0 | 5501.397 | 5621.244 |
| L2 | 5687.693 | 5807.540 |

The end-to-end comparison also includes JVM launch and the external resume/data-frame setup before the native lifecycle timer starts. It excludes separately identified acceptance evaluations and one-time isolation/setup. `execution.jsonl` retains each measured command duration.

| Arm / generation | Validation nodes | Played plies after opening | Summed move-search seconds |
| --- | --- | --- | --- |
| L0-G1 | 702160910 | 11272 | 1298.824 |
| L0-G2 | 630442053 | 11150 | 1141.104 |
| L0-G3 | 946324738 | 12054 | 1724.127 |
| L0-G4 | 714188932 | 12524 | 1242.760 |
| L2-G1 | 789551543 | 11142 | 1477.850 |
| L2-G2 | 673263390 | 11060 | 1201.931 |
| L2-G3 | 838591619 | 12123 | 1570.425 |
| L2-G4 | 754673196 | 12069 | 1338.568 |

Validation node totals sum the existing cumulative per-move search counters, including earlier iterative depths and aspiration retries. Played plies and total node cost can change when game trajectories change, even at identical opening/depth settings. These costs are not a controlled per-position search-speed estimate.

Logical campaign total is normal service lifecycle time plus the same frozen-source generation cost. Shared datasets execute once physically. Training includes production target/relation preparation and its ordinary before/after metrics; teacher checkpoint loading and atomic lifecycle I/O sit outside that phase. Sentinel/final searches, secondary trace replays, diagnostic gradient replay, setup, full-store hashing and final arenas are separate acceptance overhead. Arena nodes are summed existing per-move telemetry; qnodes are not exposed by that normal telemetry. No equal-resource superiority is inferred from fixed-depth play. Timing includes JVM/JIT and host noise and is not an independently replicated speed benchmark.

### L2-G4 versus L0-G4

| Measure | Result |
| --- | --- |
| Pairs / games | 64 / 128 |
| W/D/L | 78/3/47 |
| Score | 62.11% |
| Paired score sum / pairs | 39.75 / 64 |
| Approximate paired 95% interval | [0.5320860250994331, 0.7101014749005669] |
| White split | {'wins': 40, 'draws': 1, 'losses': 23} |
| Black split | {'wins': 38, 'draws': 2, 'losses': 24} |
| Incomplete pairs | 0 |
| Terminations | {'BLACK_CHECKMATES_WHITE': 61, 'WHITE_CHECKMATES_BLACK': 64, 'THREEFOLD_REPETITION': 3} |
| Distinct opening states | 55 |
| Elapsed seconds | 1236.884 |
| Nodes | 692850544 |
| Framework assessment | {'validPairs': 64, 'mean': 0.62109375, 'radius': 0.15298417691755103, 'lowerBound': 0.46810957308244894, 'threshold': 0.5, 'decision': 'RETAIN_INCUMBENT'} |

The approximate interval uses the standard deviation of **opening-pair scores**, with a Student-t correction, rather than treating reversed-colour games as independent. It is descriptive under this fixed source distribution, not a non-inferiority proof, a permanent Elo estimate or proof of equality. The production one-sided Hoeffding assessment is separately retained exactly. These standalone final matches only assess the models; they do not publish or promote checkpoints into either experimental or production stores.

### L2-G4 versus g134

| Measure | Result |
| --- | --- |
| Pairs / games | 64 / 128 |
| W/D/L | 30/8/90 |
| Score | 26.56% |
| Paired score sum / pairs | 17.0 / 64 |
| Approximate paired 95% interval | [0.18097835990960748, 0.3502716400903925] |
| White split | {'wins': 17, 'draws': 5, 'losses': 42} |
| Black split | {'wins': 13, 'draws': 3, 'losses': 48} |
| Incomplete pairs | 0 |
| Terminations | {'WHITE_CHECKMATES_BLACK': 65, 'FIFTY_MOVE_RULE': 2, 'BLACK_CHECKMATES_WHITE': 55, 'INSUFFICIENT_MATERIAL': 4, 'THREEFOLD_REPETITION': 2} |
| Distinct opening states | 55 |
| Elapsed seconds | 1514.259 |
| Nodes | 869849681 |
| Framework assessment | {'validPairs': 64, 'mean': 0.265625, 'radius': 0.15298417691755103, 'lowerBound': 0.11264082308244897, 'threshold': 0.5, 'decision': 'RETAIN_INCUMBENT'} |

The approximate interval uses the standard deviation of **opening-pair scores**, with a Student-t correction, rather than treating reversed-colour games as independent. It is descriptive under this fixed source distribution, not a non-inferiority proof, a permanent Elo estimate or proof of equality. The production one-sided Hoeffding assessment is separately retained exactly. These standalone final matches only assess the models; they do not publish or promote checkpoints into either experimental or production stores.

## Interpretation and next work unit

Ordinary learning is preserved relative to the matched control: G4 native held-out base loss is 5.87% lower and fixed held-out base loss is 6.70% lower with lambda=2. Fixed capture-delta median/p90/p95 errors are 20.35%/30.41%/26.53% lower; unsupported-error rate falls from 15.89% to 3.74%. Sign agreement is slightly worse, 63.55% to 62.62%. The local benefit is non-monotonic: G2 median errors nearly tie and lambda=2's tails are slightly worse, before improvement returns at G3/G4.

The search-cost benefit does not persist through G4. Sentinel qnodes are 26.10%, 33.57% and 48.97% lower with lambda=2 at G1-G3, then 29.09% higher at G4. This is a relative reversal: both arms become cheaper from G3 to G4, but the control improves more. On the full final suite, lambda=2 uses 16.93% more qnodes, has median ratio 1.0413 and lower aggregate stand-pat effectiveness. Four of five descriptive gates fail; only the severe-regression guard passes. Removing the largest saving leaves 21.74% extra qnodes. Draw sources regress in seven of eight positions; tactical qnodes rise 30.74%, while quieter-position qnodes fall 26.60%. Better local capture geometry therefore does not establish sustained whole-search cost reduction.

Normal candidate quality is mixed. Lambda=2 scores better than lambda=0 against the common g134 incumbent in G1-G3, but worse in G4: 36.72% (44/6/78) versus 43.36% (54/3/71). All eight normal decisions retain g134 as isolated Best. Latest advances normally through all four generations, so the fixed sequence and actual training continuation coincide. No experimental candidate is promoted to production.

The primary fixed-depth comparison gives positive bounded evidence for lambda=2 relative to the matched lambda=0 endpoint: 78/3/47, 79.5/128 game points, or 39.75/64 paired points (62.11%). The approximate pair-based 95% interval is 53.21-71.01%; both colour splits favour lambda=2. The framework's more conservative one-sided Hoeffding lower bound is 46.81%, so its unchanged promotion assessment still retains the incumbent.

The secondary result is materially adverse for retention of the starting network's strength: L2-G4 versus untouched g134 is 30/8/90, 34/128 game points (26.56%), with an approximate paired 95% interval of 18.10-35.03%. This substantial observed deficit must not be hidden behind the favourable primary match. It does not by itself identify a lambda-specific cause: the primary contrast favours lambda=2, and there is no L0-G4/g134 match on the final g139 opening set. The normal G4 candidate matches use the separate preregistered g138 opening set.

These are 64-pair/128-game comparisons at common depth 4 and six workers, with 55 distinct native opening identities among the 64 pair slots. The evidence supports a comparative advantage over L0-G4 under these settings, not general retention against g134, equal-resource superiority, proof of equality, or a permanent Elo gain. No Elo estimate is made.

Production training phases total 2.546 s for lambda=0 and 3.987 s for lambda=2: +1.440 s (+56.58%). Candidate-validation phases total 5,451.857 s versus 5,638.320 s: +186.463 s. Including measured generation-process overhead and the same 119.847 s frozen-source charge, four-generation totals are 5,621.244 s (93m41s) versus 5,807.540 s (96m48s), a lambda=2 increase of 186.296 s (3m06s, +3.31%). Shared source generation physically ran once.

Validation nodes rise from 2,993,116,633 to 3,056,079,748 despite slightly fewer played plies (47,000 versus 46,394). Four-generation sentinel primary time saves 1.587 s, but G4's full 24-position primary time rises from 2.453 to 2.615 s. There is no net campaign runtime saving. The extra auxiliary update time is small in absolute terms and does not itself explain the longer validation; game trajectories differ. JVM/JIT, host noise and one execution per condition limit precise speed claims. Arena and diagnostic/replay costs are separately reported, not charged as training overhead.

Keep lambda=2 available only as the existing default-off experimental objective. Its local-geometry and matched-control playing benefits justify continued investigation, but this unit does not justify a production-default change or automatic advancement to a longer training campaign. The production and GUI defaults remain lambda=0. The next work unit should diagnose the frozen G3-to-G4 search-cost reversal and the g134 strength-retention failure, focusing on sv-17, the tactical/draw regressions and Fianchetto. Establish why lower local/base losses fail to transfer reliably before proposing training changes. Any subsequent independent-seed or equal-resource acceptance study needs a separate preregistered work unit; this four-generation campaign is not extended.

Unproven: repeatability across starting networks, root seeds and teacher versions; behaviour beyond four generations; wider-position and deeper-search generalization; the cause of the cost reversal and g134 playing deficit; and equal-time/equal-node strength. The fixed capture corpus has only 107 correlated pairs from five games, and the search suite has 24 positions. The final arenas use one fixed opening distribution and six-worker search. The observed primary gain is evidence within that bounded comparison, not proof of a lasting global strength gain. The report does not establish user acceptance or production readiness.

## Validation, execution limitations and Git boundary

Executed integrity checks: byte-identical starting network/full optimizer state; immutable exact teacher; config equivalence except lambda/root; independently derived seeds; fresh shared source counts/whole-game splits; active legal selected capture transitions and exact target arithmetic; frozen held/search corpus hashes; zero suite-root/current-training endpoint overlap. Per-generation independent manual update replays, trace-free/trace search equality, checkpoint serialization, native paired-opening identity checks and final store/source inventories are recorded as each stage completes. Runtime validation is distinguished from static source/hash inspection.

External harness setup corrections before campaign training: the initial sparse teacher copy omitted payloads required by historical acceptance-chain validation, so the full teacher store was copied externally; a JSON serializer treated Path as an Iterable and was corrected. L0 initialization had already persisted its exact starting checkpoint, which was verified and reused; no training update had occurred. An empty failed serializer output is retained. These are harness/setup issues, not repaired production defects or campaign restarts. The external evaluation harness also had a pre-compilation extra parenthesis corrected before execution.

Final integrity result: `{"utc":"2026-09-24T11:03:34.230777+00:00","baselineFilesPreserved":552,"productionSourceChanged":false,"productionStoreFilesUnchanged":{"brn":993,"teacher":742},"frozenInputsUnchanged":25,"pairedOpeningIdentitiesChecked":640,"fullStateReplayEqualities":8,"producedCheckpointCopiesVerified":8,"pairedConfigsAndDataHashesEqual":true,"onlyRepositoryAddition":"BRN_CAPTURE_CONSISTENCY_MULTI_GENERATION_ACCEPTANCE_2026-09-24.md","indexEmpty":true,"initialHeadUnchangedBeforeReportCommit":true,"journalVersionAbsent":true,"acceptedBaselineArchive":"C:\\Users\\Central\\AppData\\Local\\Temp\\seedv6-brn-multigen-20260924\\accepted-execution-baseline.zip","acceptedBaselineArchiveSha256":"a6b79497c7b1b8a802fed2769bb93406ec7f91b2c3c14680dfb6c824499d4905"}`.

All eight generations completed with exactly 6,517 updates per arm and final optimizer step 224,675. All 256 source games, 1,024 normal candidate-validation games and 256 final-arena games completed without a cap or failure. All 156 primary sentinel/final search requests completed depth 4 without caps; all 156 secondary observer replays matched their non-timing results. All eight independent training replays matched complete saved optimizer-state bytes. There were eight planned fresh-JVM resume starts, no unplanned generation restart or recovery, and no production defect requiring repair. Numerical outputs remained finite. Quality was not monotonic: G3 qnodes spiked and the relative advantage reversed at G4; substantial root-score/move drift is documented in the per-position and continuity tables. Operational completion is not evidence of stable playing quality. Final integrity checks preserve all 552 starting repository files, all 993 BRN and 742 NNUE production-store files, 25 frozen inputs, all produced checkpoint copies, and 640 executed paired-opening identities. Only this report is added to the repository. No production source/default/store mutation, push or deployment occurred.

Deliberately skipped: software regression-suite rerun, GUI/browser QA (headless empirical work), source/default repairs or tuning, other lambdas, calibration, altered search/depth/caps, additional training generations, full 24-position runs at intermediate checkpoints, tournament/round-robin, Elo claims, production promotion, push/deployment. The secondary match is conditional under the preregistered runtime rule, not required for primary acceptance.

The acceptance report is the only intended repository change. Generated networks/corpora/stores are not staged or committed. The final source/inherited-file hashes and Git status are checked before a report-only commit; the report’s own commit SHA is supplied in the completion response. No unrelated changes are reset, stashed, discarded, overwritten or absorbed. This completed empirical unit does not itself establish user acceptance or authorize a default change.

## Reproduction and retained evidence

Verified execution-source snapshot: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-multigen-20260924\accepted-execution-baseline.zip`, SHA-256 `a6b79497c7b1b8a802fed2769bb93406ec7f91b2c3c14680dfb6c824499d4905`. It was captured after measurements only after every one of the 552 original tracked/non-ignored files matched its recorded starting bytes. It includes accepted dirty and untracked integration files, and excludes this new report.

Execution uses Java `21+35-2513`, Python 3.13.4, `-Xmx1536m` and `-Djava.awt.headless=true`, with the externally compiled accepted dirty-tree source on the classpath. Each generation resumes in a fresh JVM; reported production-phase times therefore include that process's JIT conditions. This is a bounded resume-based campaign, not a steady-state JVM microbenchmark.

The external directory is retained. `preregistration.json`, `pins.json`, `baseline.json`, `production-stores-before.json`, `frozen-manifest.json`, `seeds.jsonl`, all `g135..g138-*` source/data/target files, `held.bin`, both normal stores, per-checkpoint model/state copies, raw JSONL searches/arenas, and `execution.jsonl` identify the run. `Campaign.java`, `DataBridge.java`, `RelationBridge.java`, `Evidence.java`, `Bench.java`, `build.py`, `freeze_campaign.py`, `execute.py`, `analyze.py`, and this report generator preserve the external harness. No clean HEAD archive is an execution substitute.

| Frozen artifact | SHA-256 |
| --- | --- |
| preregistration.json | 1ff7c86fec7150ec35d2c966ab802b3014ea094420b50f5763327d73df9b59ca |
| seeds.jsonl | eb301fc0f5951d5955b43cd3ea5d987847e72bf0805f2edb0e9d1e31e9d732a5 |
| g134.brn2 | 21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924 |
| g134.state | ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9 |
| teacher.nnue | 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9 |
| replication-rows.bin | fcfe1a52b4f90bf222357c1a7426b025d274587c232c986201f87c250a0ebd1d |
| held.bin | fc084a202de2a5cda88eb00149aff6c3dde1e5a4aef446324483a1222d1a86c2 |
| held-freeze.jsonl | c4d8ee4264a264887ce91c6cd1db783600545870bb9c5bb7e7cbc9010473c881 |
| campaign-train-keys.txt | 4c3d888c1b5b7d4aa075b7c51f10d13b1cadc37a573048702f4d7eaccaba79ca |
| search-suite.tsv | 9aaad2d4cb46f4409ec3dfbe9d2af3732a5254a1ae18ea98482171197cf494b1 |
| search-suite.jsonl | c7aa27bb205d2a66d0d8f6be265bbc1d3fabe6017526e1f0f1cf07cc8380f7ee |
| sentinel.tsv | 8407ed4d9f4e0b5c2ddd8796664da51eea078e4dc2f5ae20011a6e82655f8b17 |
| sentinel.json | 023eb652eab185e287d6004b96c8a5fba9859293f2349a3db2b3e413ebd2c5c6 |
| g135-data.bin | 3628f9826acd2ee405d3cbe4877d1424c7c19aae780925f328fb307a78004723 |
| g135-source.jsonl | d85703cceb51d09f6c7de210b2e36355804a93dd6a01a3c607e124a4fb599b6a |
| g135-train.jsonl | c510f7d6ffdac8521c972ce3bc3e45e6ab48b78ddb1f8039782980632d69272e |
| g136-data.bin | bbecc8fa0484f39007f6ff0f10d6f3e50cf218f3eec39654ac666fca69641fc9 |
| g136-source.jsonl | 0b8b4a56f198c10f22bd4a0923ee495415115dffe18f7a101faed98d96bd78f6 |
| g136-train.jsonl | 2f12891f28bb60ec4c312cc1de01e75c5e6a752097a3e8623b394b01341700e3 |
| g137-data.bin | 65ff84f0a8b2ac00626a4b19b96ceef38f4846cc5cb80e57bb198a7c2db811f0 |
| g137-source.jsonl | ae943cf85368c2249acae6150e11947ad5c48ef06e50e96bbda8ec96ec05ebb3 |
| g137-train.jsonl | a2ed4c970325b78684a85a1eeffa0b3b8b296813d49f15d9191a7ab013584bef |
| g138-data.bin | 793647c6393a50b56d305f28e245465f1dff65b055c3aab81ad2418dfabcd6bc |
| g138-source.jsonl | db3f5ea4c519eccd862bbedcffe56ef8a3076215430d0e45b4ec426bbbd1d1f5 |
| g138-train.jsonl | c645545a913043fffbcd98172c25b9685dbef0376b0e0fc9c6bfaf3c8157e1d5 |

| Checkpoint payload | SHA-256 |
| --- | --- |
| g134.brn2 | 21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924 |
| g134.state | ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9 |
| L0-G1.brn2 | 10924b42ba7f96bee0e2875408364b9c78227744ca14f247fba310e7034692b7 |
| L0-G1.state | a4d500885c71e8b3dc75ceeed1237035d4805000844e5e55fe201455d9845aa3 |
| L2-G1.brn2 | c4edb12d75cb4c70a7318faf1bfca3ec79b30d3fa6c19e8e2eb86baf87c295b3 |
| L2-G1.state | 13ad4a8bdc4a25557db6af7f7bcb014e3be12ee5714f95b376eeb2e2b84dd2dc |
| L0-G2.brn2 | c2238ff89965f3b3d52584d7dd02590bec613eea4fbf26b1fc9543660ceea8ba |
| L0-G2.state | 0652d47ff40b7c3549aa304831850dfefc3850efee8278a9ebb10a6ae167a92e |
| L2-G2.brn2 | db8074a25f74b1e07aa5496870bfafb76f692e74fb3fd9082ec012f636f39ea3 |
| L2-G2.state | 2aa309b849fa9f4879f9a069c2c4bb8a6b9715c2f4b6ac20e55d5cdb18e15a4d |
| L0-G3.brn2 | 1600bc614a168ed8c4646ffb688f11e6c6853c00c1fee329194ada4a741806bb |
| L0-G3.state | ab7429944ec5dc207b53ddcca3c9cd5b52806120d02a5d602fca90a63328accb |
| L2-G3.brn2 | 05d4c425ec6c28a1ba9c12534df9eee423e32089260b4a8609f52b02fe9fc7f4 |
| L2-G3.state | c1e6094c969a0c1b00bc548321871ba957efa6fa4898629794d175a6d78457a2 |
| L0-G4.brn2 | 7c1d6a99737d17a34e3daccea14e75023c74bb6d189f0a53e16e962991767876 |
| L0-G4.state | 21b0ee50acb5e0a9b0dcc9fff623feb4b9205973a0436b4bc5026192247ea786 |
| L2-G4.brn2 | cebde9e9d75609e44bf046c13674857aa6b26c8730e53bbba963a4464625c90c |
| L2-G4.state | 7ab04633013b525bb09e02722243dd4c6f050e1e3599eb96d77ca9b0b3d8637a |

**Human actions required after this prompt: None.** The recommended next empirical/default decision is a separate work unit, not a prerequisite for completion of this report.
