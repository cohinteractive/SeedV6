# BRN capture-consistency training diagnostic — 2026-09-24

**Classification: mixed; negative on the qsearch integration gate.** Target-grounded auxiliary training learns better local capture deltas, including on frozen actual-search edges. It does **not** reliably restore stand-pat effectiveness or reduce the primary qsearch tree. The held-out-selected λ=2 model improves median delta error **0.242500 → 0.200964 (17.13%)** and ordinary endpoint loss **0.137350 → 0.126709 (7.75%)** relative to the equally trained λ=0 control. However, Kiwipete changes from **672,779 qnodes at completed depth 4** to **988,123 qnodes in a million-node capped prefix, completing only depth 3**. Fianchetto worsens **38,689 → 56,566 qnodes**. The conservative λ=0.5 helps Fianchetto and Opening, but worsens Kiwipete.

**Recommendation: do not integrate this loss into the production BRN trainer for a longer campaign on this evidence.** Preserve the isolated research result. First establish a representative, original-supervision-preserving base replay whose λ=0 arm does not itself regress the reproducer, then repeat a bounded auxiliary ablation. This result supports learnability of teacher deltas, not sufficiency of that objective for search compatibility.

## Scope, identity and accepted-report relationship

- Date: 2026-09-24, UTC and New Zealand local date. First successful UTC clock observation: `2026-09-24T01:32:07Z`; the initial unsupported PowerShell `Get-Date -AsUTC` attempt failed. No earlier timestamp is inferred.
- Authoritative repository: `C:\projects\seed\java\seedv6`; inspected HEAD **`b1d224f3a51e5bb720a15e6339db6023291b6ff2`**.
- Read both accepted reports, including their methods, source appendices, common-perspective algebra and limitations: [stand-pat diagnostic](BRN_QSEARCH_STAND_PAT_DIAGNOSTIC_2026-09-24.md), commit `8ea5cb99b32330c1b85ef8a918dfb24ae5336109`; [calibration diagnostic](BRN_QSEARCH_CALIBRATION_DIAGNOSTIC_2026-09-24.md), commit `b1d224f3a51e5bb720a15e6339db6023291b6ff2`. This third unit tests their proposed target-grounded training intervention; it does not replace their accepted observations.
- No ancestor/repository on-disk `AGENTS.md` was found. User-supplied governance applies. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent; neither was created and no finalizer/journal operation was activated.
- Initial index empty; **41 modified tracked files and 46 individually enumerated untracked files**, inherited GUI/training/docs/build outputs. They remain unattributed and preserved. Initial SHA-256 inventory covers **540 existing tracked/non-ignored files**.
- Isolated environment: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-capture-consistency-20260924`. `git archive HEAD` produced a clean disposable copy, excluding every inherited edit. No temporary source/checkpoint/class/data file was placed in the authoritative worktree.
- Exact temporary mutations: diagnostic capture-loss support in copied `Brn2Trainer.java`, and the prior report's two BRN-only calibration return expressions in copied `SearchEvaluation.java`. Complete archive comparison confirms all other existing files unchanged. Qsearch, NNUE, move ordering, pruning, extensions, requested depth, checkpoint codecs/schemas and GUI/configuration were unchanged.
- This report is the sole task-created authoritative file. No normal self-play generation, promotion, arena or store writer was invoked. Report completion/commit is not user acceptance, production acceptance or deployment.

## Pinned model and target source

Direct canonical payload codecs verified headers, schema/dimensions, finite values and CRC. g134 model bytes equal the snapshot decoded from its exact saved training state. All three arms restore the same weights, Adam moments, configuration and step; optimizer equality is exact rather than approximate.

| Role | Exact path | SHA-256 |
| --- | --- | --- |
| brn | `E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c\network.brn2` | `21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924` |
| training | `E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c\training.state` | `ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9` |
| nnue | `E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6\network.nnue` | `3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9` |

BRN is canonical BRN-2/schema 2, width 32, with 1,645,665 binary64 parameters. Saved optimizer: step **218,158**, learning rate **0.001**, β1 **0.9**, β2 **0.999**, ε **1e−8**. Experimental model files use the unchanged `Brn2Codec` inference format, with no production store identities, refs, promotion records or schema additions.

The independently grounded per-position target is the **pinned g74 NNUE's native side-to-move bounded static value**, `StrictMath.tanh(raw)`, obtained afresh for each exact board. This is the established committed mechanism in `training/service/BrnSupervision.teacherValue`, already exercised by `tools/search/Brn2SupervisionAblation` and supported at teacher weight 1. No integer search-score or centipawn conversion supplies a training target. The NNUE network is immutable; byte-identical serialization before/after and stable input-file hashes verify that training never changes it.

**No terminal/final-game WDL labels were used, either as a shared zero-delta surrogate or as an arbitrary label for a hypothetical capture child.** Each parent and child gets its own NNUE value. The targets are independent of BRN and valid per position under the repository's teacher convention; they are a search-compatible reference, not a proof of optimal chess value or calibrated game-winning probability. No teacher search was necessary.

### Meaning of the preserved base objective

The ordinary BRN **half-squared-error loss, tanh output, full network backpropagation and sparse online Adam conventions remain active** at every update. The experiment uses the existing **teacher-only supervision endpoint** for absolute parent/child targets. It does not train with delta loss alone, or invent final outcomes for synthetic children.

This is an important scope limit: the store's checksummed root `brn-supervision.bin` says `NNUE_BLENDED`, weight **0.5**. Thus the diagnostic preserves the normal loss form but is **not an exact continuation of the campaign's 50:50 WDL/NNUE target mixture**. The equally trained λ=0 arm controls for the teacher-only targets, corpus and updates when estimating the auxiliary effect. Comparing any trained arm directly with untouched g134 also includes this base-target/data shift. Preservation of the original campaign WDL objective was not established by the teacher-relative held-out loss.

The optional high-level HEAD store inspection could not validate the complete live store because its bootstrap-plan reader rejected a later record kind. No recovery, writer or schema change was attempted. Canonical pinned model/training payloads loaded normally and matched exactly; direct root-record SHA/framing inspection established the observation above. Full store/lineage acceptance remains outside this diagnostic.

## Fixed corpus and strict split

Use the accepted diagnostic's legal sorted-move generator and `Board.makeMoveInto`, with Java `SplittableRandom(2026092403 + pathIndex)`. Generate **64 bounded random legal-move paths**, independent of either evaluator, from two existing roots:

1. `Brn2DiagnosticCorpus` / `BrnRemediationBenchmark` Ruy Lopez: `r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3`.
2. `DefaultPerftPositionLibrary` knight-check-and-castling: `rnbqkb1r/pp1p1ppp/2p5/4P3/2B5/8/PPP1NnPP/RNBQK2R w KQkq - 0 6`.

Root family is `(pathIndex / 4) % 2`. Retain path plies **16, 17, 32, 33, 48, 49, 64, 65** when the position is active: **508 broad roots** in total. At each retained root enumerate every legal capture, then at most two coordinate-first legal capture replies per child. These include captures beyond the initial sampled board; they do not depend on teacher scores. Exclude terminal/rule-drawn endpoints (**4 exclusions**) and deduplicate parent-key/move edges (**50 exclusions**). Retain partial paths; no evaluator-based path selection occurs. These are bounded synthetic position walks, not engine self-play or completed outcome-labeled games.

Reserve entire paths where `pathIndex % 4 == 0`. The resulting train and held-out **endpoint board-key sets are disjoint**, including broad roots. Benchmark roots are explicitly forbidden. Every actual BRN static search evaluation is also checked against all corpus keys: **zero overlap**, including the capped prefix. No benchmark position or qnode result selected λ, epochs, optimizer or corpus. The two shared root families mean this is path-held-out generalization, not opening-family-held-out generalization.

| Split | Paths | Pairs | Direct / reply edges | Unique pair parents | Unique pair endpoints | Broad roots |
| --- | --- | --- | --- | --- | --- | --- |
| train | 48 | 4884 | 1730 / 3154 | 2004 | 5232 | 380 |
| held | 16 | 1854 | 645 / 1209 | 740 | 1968 | 128 |

Total **6,738 pairs = 4,884 train + 1,854 held out**. There are **80 sampled parents with one/two retained immediate captures** and **331 with four or more**; parent occupancy ranges **14–32 pieces**. This supplies relatively quiet capture opportunities and active positions, but random legal play is often materially imbalanced and is not representative of a real BRN search distribution. Reply selection is lexicographic and bounded, another sampling limitation. Endpoints repeat across edges and observations are correlated.

The held-out split is also the model-selection validation set; the final independent test is the untouched search-fixture set. No claim of an additional unseen statistical test partition is made. Exact boards (all six longs), move encoding/coordinate, path/ply/branch, split, both targets and common-perspective target delta were frozen in `pairs.bin`/`pairs.jsonl` before training. The source appendix reproduces them deterministically.

## Loss, online updates and fixed budget

Let `y_p,y_c,t_p,t_c` be native **STM** bounded outputs/targets. In the parent perspective:

```text
predicted_delta = -y_c - y_p
target_delta    = -t_c - t_p
e               = predicted_delta - target_delta
Huber_h(e)      = 0.5*e^2                    if |e| <= h
                  h*(|e| - 0.5*h)           otherwise
h               = 0.25
L_step          = 0.5*(y_anchor-t_anchor)^2 + lambda*Huber_h(e)
```

This is the requested child-minus-parent formula after aligning perspective. It targets the **error in change**, not the change's magnitude. With STM variables the auxiliary derivatives are `−λ*clip(e,−h,h)` for **both** endpoint outputs. In a common fixed orientation, the two output derivatives have opposite signs. The tanh derivative and each endpoint's own two ReLU masks then propagate them through the full network.

Every pair receives **two ordinary online base steps**, parent then child. Each step computes both endpoint auxiliary Jacobians using its own single pre-update weight state, aggregates shared sparse rows and dense parameters once, then performs one Adam update. The second step recomputes both predictions after the first. This implements the average base objective across endpoints plus the auxiliary term; it is not a simultaneous two-base-example minibatch. λ=0 dispatches to the unchanged `train(board,target)` method, so neighbour-only sparse rows are not updated in the control. Sparse row activation/moment behavior for nonzero λ follows the added endpoint Jacobian; absent rows still freeze. Candidate weights/moments are finite-checked before publication.

Initial **training** endpoint base loss is **0.1608279065**, auxiliary Huber loss **0.0529546631**. The weights were frozen before any training or final search result:

| λ | Initial λ·delta / base | Maximum auxiliary output derivative per endpoint | Role |
| --- | --- | --- | --- |
| 0 | 0% | 0 | ordinary trainer control |
| 0.5 | 16.46% | 0.125 | conservative |
| 2 | 65.85% | 0.5 | stronger bounded |

A single **fixed epoch**, **4,884 pairs**, **9,768 base examples/Adam steps per arm**, identical deterministic Fisher–Yates pair order with seed `2026092403`, no early stopping/checkpoint selection. Final step **227,926**. All source weights/moments/configuration are identical; only λ varies. Total measured training-loop time across the three arms is **17.79 seconds**. Time is descriptive, excludes corpus generation/held-out evaluation/serialization, and is not a throughput benchmark.

Selection rule, frozen before training: choose the nonzero arm with lowest held-out median absolute delta error, provided its endpoint and broad-root ordinary base losses are each at most 10% above λ=0. Both qualify; **λ=2 was selected at `2026-09-24T01:41:15.352896+00:00`, before the first search benchmark**. λ=0.5's later Fianchetto result did not change selection. There was no λ sweep, retuning, extra epoch or search-driven model choice.

| λ | Training seconds | Online base / delta | Final train base / delta | Model SHA-256 |
| --- | --- | --- | --- | --- |
| 0 | 4.071 | 0.039345 / 0.030120 | 0.014406 / 0.017319 | `20af96932e9aa1db7bdfe5df61599fac02ab3f6d314b07e0a07337d60516e366` |
| 0.5 | 6.836 | 0.040373 / 0.032444 | 0.017256 / 0.021522 | `9ef79c98ce9c0650e81b56c6f0938013d33be59cfbf0326e67a20b6cfab22223` |
| 2 | 6.879 | 0.047374 / 0.034296 | 0.022922 / 0.023278 | `f8a38965218f51d895e928bd5a562f548f65747f0464807de799109f5402fc01` |

Online losses are pre-update averages; final train/held-out losses evaluate the completed frozen model. Training loss alone did not select a model: λ=2 actually has higher final training loss than λ=0 while improving held-out error.

## Held-out absolute and capture-delta results

All training-domain values below are unrounded tanh values in `[-1,+1]`; a delta can span `[-2,+2]`. Ordinary loss is mean half-squared endpoint target error, equally weighting two endpoints per pair. MAE is endpoint target error. Quantiles interpolate at `(n−1)*p`.

| Model | Base loss | Target MAE | Broad-root loss (128) | Delta Huber | Delta error p50 / p90 / p95 |
| --- | --- | --- | --- | --- | --- |
| g134 untouched | 0.194762 | 0.502987 | 0.219588 | 0.053573 | 0.257130 / 0.662675 / 0.788650 |
| λ=0 | 0.137350 | 0.408644 | 0.149827 | 0.050931 | 0.242500 / 0.670305 / 0.784130 |
| λ=0.5 | 0.131102 | 0.404887 | 0.140853 | 0.046936 | 0.222219 / 0.616428 / 0.764664 |
| λ=2 | 0.126709 | 0.406332 | 0.134929 | 0.039843 | 0.200964 / 0.541625 / 0.689705 |

| Model | Delta-sign agreement % | Error >0.5 % | Unsupported value reversals % | Raw abs delta p50 / p95 | Mean STM |
| --- | --- | --- | --- | --- | --- |
| g134 untouched | 53.40 | 21.04 | 20.66 | 0.201050 / 0.614300 | -0.027321 |
| λ=0 | 57.28 | 20.17 | 18.39 | 0.181702 / 0.609493 | 0.001458 |
| λ=0.5 | 57.87 | 17.48 | 16.99 | 0.173107 / 0.608493 | 0.022459 |
| λ=2 | 61.11 | 12.46 | 14.29 | 0.156253 / 0.487977 | 0.000667 |

The held-out target's absolute delta median/p95 is **0.116504 / 0.679348**. Raw means uncalibrated, not pre-tanh logits. Multiplication by 32,511 approximately expresses these in search units; real search uses the unchanged rounding/minimum-nonzero mapper. The report's principal error measurements avoid that quantization.

“Unsupported value reversal” means BRN's parent and negated child values have opposite signs while the corresponding teacher values do not. It is distinct from disagreement about the **direction of delta**. Error >0.5 is a fixed descriptive large-error threshold, twice the Huber transition, not a centipawn claim or a rejection of legitimate large target deltas. λ=2 improves sign agreement **57.28%→61.11%**, unsupported reversals **18.39%→14.29%**, and large-error rate **20.17%→12.46%** against λ=0.

The median-error gain occurs in both root families: Ruy Lopez **0.267481→0.210776**, knight-check/castling **0.219895→0.192527**. A descriptive 2,000-resample whole-path paired bootstrap (16 held-out paths, Python seed 20260924) gives a 95% percentile interval **[−0.074189, −0.012805]** for λ=2 minus λ=0 median delta error. This is conditional on this synthetic generator and post-selection; it is not a broad strength/generalization confidence claim.

The improvement is not solely re-centering: held-out signed delta-error means are **−0.031455 (λ=0)** and **−0.029872 (λ=2)**, while error SD falls **0.395811→0.336854** and mean-centered median absolute error falls **0.243634→0.204745**. The STM means are also almost identical near zero. There is learned relative-error improvement beyond the small mean shift.

### Legitimate volatile targets remain difficult

For the **217 held-out pairs with |target delta| >0.5**, target median absolute change is **0.642406**. λ=0→2 median target-delta error improves only **0.579791→0.542132 (6.50%)**, and direction agreement **68.20%→71.89%**. Predicted absolute delta medians are **0.193866→0.189463**, much smaller than the teacher's required changes. The smaller raw delta is **not** counted as success; substantial tactical underprediction remains. On **871 pairs with |target delta|≤0.1**, median error is **0.152064→0.146673**, with large errors **8.15%→3.21%**. The robust auxiliary term helps aggregate error/tails without adequately learning all large tactical transitions.

Ordinary teacher-relative held-out loss does not degrade and broad-root loss improves **0.149827→0.134929**. Absolute quality nevertheless remains weak: endpoint MAE is about **0.406**, sign agreement only **61%**, and final λ=2 train/held losses differ substantially (**0.022922/0.126709**). Neither retention of the original WDL mixture nor chess strength follows from these numbers.

## Real qsearch results

Same six accepted fixtures/FENs, **requested depth 4**, one worker, ordinary iterative deepening, fresh **262,144-entry TT**, singleton root history, **1,000,000 entered-node / 60-second** caps. Neural search retains mate-distance-only selectivity; qsearch soft limit 16, checked-node evasions, ordering and all pruning/extension rules remain original. Every returned search preserved its root board and matched node accounting. No run reached the time cap.

`QsearchDecisionTrace` is the unchanged observer: NNUE shadow at every static, detached position stride 101, maximum 12,000 samples (not reached). Qnodes count entered qsearch children; qsearch roots remain main leaves, as in both accepted reports. Q% is q/total entered nodes. SP attempts count eligible stand-pat opportunities; SP% is cutoffs/attempts. Timing includes tracing/JIT and is secondary.

The untouched g134 and NNUE replay reproduce all accepted primary counts. The table labels **CAP3** for a depth-4 request capped after only depth 3 completed. Its nodes include a partial depth-4 iteration; its score/move belong to depth 3. It is not a completed depth-4 comparison or a measured speedup.

| Fixture | Raw model | Depth/status | Total | Qnodes | Q% | SP attempts | SP cuts | SP% | Score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kiwipete | g134 | 4/OK | 218,790 | 209,297 | 95.66 | 180,919 | 80,896 | 44.71 | 10036 / e1g1 | 2.608 |
| Kiwipete | NNUE | 4/OK | 41,435 | 32,300 | 77.95 | 36,191 | 29,281 | 80.91 | 20697 / e1g1 | 0.243 |
| Kiwipete | lambda0 | 4/OK | 683,012 | 672,779 | 98.50 | 567,023 | 286,440 | 50.52 | 3943 / e5f7 | 6.848 |
| Kiwipete | lambda0.5 | 4/OK | 761,654 | 745,112 | 97.83 | 653,543 | 346,857 | 53.07 | 3620 / e5g6 | 7.971 |
| Kiwipete | lambda2 | CAP3 | 1,000,000 | 988,123 | 98.81 | 860,306 | 441,322 | 51.30 | 3247 / d5e6 | 10.292 |
| Exchanges | g134 | 4/OK | 2,019 | 749 | 37.10 | 1,347 | 925 | 68.67 | 11541 / e4d5 | 0.011 |
| Exchanges | NNUE | 4/OK | 3,197 | 1,419 | 44.39 | 2,362 | 1,441 | 61.01 | 21844 / e4d5 | 0.010 |
| Exchanges | lambda0 | 4/OK | 1,935 | 690 | 35.66 | 1,270 | 910 | 71.65 | 12150 / e4d5 | 0.012 |
| Exchanges | lambda0.5 | 4/OK | 2,050 | 752 | 36.68 | 1,380 | 980 | 71.01 | 11630 / e4d5 | 0.012 |
| Exchanges | lambda2 | 4/OK | 1,970 | 695 | 35.28 | 1,288 | 899 | 69.80 | 10157 / e4d5 | 0.018 |
| Tactical queen | g134 | 4/OK | 189 | 8 | 4.23 | 115 | 72 | 62.61 | 6397 / e4d5 | 0.002 |
| Tactical queen | NNUE | 4/OK | 229 | 10 | 4.37 | 154 | 109 | 70.78 | 7057 / e4d5 | 0.002 |
| Tactical queen | lambda0 | 4/OK | 199 | 8 | 4.02 | 125 | 79 | 63.20 | 9851 / e4d5 | 0.002 |
| Tactical queen | lambda0.5 | 4/OK | 188 | 8 | 4.26 | 125 | 87 | 69.60 | 7382 / e4d5 | 0.002 |
| Tactical queen | lambda2 | 4/OK | 205 | 8 | 3.90 | 141 | 102 | 72.34 | 4072 / e4d5 | 0.003 |
| En passant | g134 | 4/OK | 358 | 18 | 5.03 | 235 | 151 | 64.26 | 0 / e5d6 | 0.005 |
| En passant | NNUE | 4/OK | 638 | 40 | 6.27 | 427 | 264 | 61.83 | 0 / e5d6 | 0.004 |
| En passant | lambda0 | 4/OK | 248 | 10 | 4.03 | 142 | 77 | 54.23 | 0 / e5d6 | 0.002 |
| En passant | lambda0.5 | 4/OK | 245 | 10 | 4.08 | 139 | 74 | 53.24 | 0 / e5d6 | 0.003 |
| En passant | lambda2 | 4/OK | 281 | 6 | 2.14 | 169 | 106 | 62.72 | 0 / e5d6 | 0.002 |
| Fianchetto | g134 | 4/OK | 18,327 | 13,007 | 70.97 | 16,509 | 8,896 | 53.89 | -18683 / f3e5 | 0.253 |
| Fianchetto | NNUE | 4/OK | 8,793 | 3,928 | 44.67 | 7,437 | 5,744 | 77.24 | -22502 / g2h3 | 0.041 |
| Fianchetto | lambda0 | 4/OK | 50,165 | 38,689 | 77.12 | 46,862 | 22,156 | 47.28 | -21400 / e2e3 | 0.606 |
| Fianchetto | lambda0.5 | 4/OK | 20,329 | 12,362 | 60.81 | 18,437 | 11,512 | 62.44 | -20796 / e2e3 | 0.272 |
| Fianchetto | lambda2 | 4/OK | 66,869 | 56,566 | 84.59 | 62,623 | 28,044 | 44.78 | -17005 / b1c3 | 0.810 |
| Opening | g134 | 4/OK | 3,795 | 220 | 5.80 | 2,913 | 2,158 | 74.08 | -872 / b1c3 | 0.047 |
| Opening | NNUE | 4/OK | 2,343 | 126 | 5.38 | 1,684 | 1,136 | 67.46 | -887 / h2h3 | 0.011 |
| Opening | lambda0 | 4/OK | 2,843 | 188 | 6.61 | 2,097 | 1,434 | 68.38 | -267 / b1c3 | 0.034 |
| Opening | lambda0.5 | 4/OK | 3,298 | 155 | 4.70 | 2,482 | 1,838 | 74.05 | -1184 / e2e4 | 0.042 |
| Opening | lambda2 | 4/OK | 3,860 | 324 | 8.39 | 3,039 | 2,241 | 73.74 | -4030 / f2f3 | 0.048 |

The auxiliary effect must be compared with λ=0, and the original checkpoint must remain visible:

- **Kiwipete:** ordinary teacher-only replay alone raises qnodes **209,297→672,779**, changes `e1g1→e5f7`, and still improves aggregate SP **44.71%→50.52%**. Thus higher SP% already fails as a sufficient indicator of smaller search. λ=0.5 then increases qnodes **10.75%** to **745,112**, despite SP **53.07%**. Selected λ=2 reaches the cap (**988,123 qnodes**, SP **51.30%**) and fails to finish depth 4. Its capped prefix exceeds both completed controls; the full count is unknown.
- **Fianchetto:** λ=0.5 reduces qnodes **38,689→12,362 (68.05%)**, SP **47.28%→62.44%**. It is also **4.96% below untouched g134**. λ=2 instead raises qnodes **46.21%** to **56,566**, SP falls to **44.78%**. Better held-out delta loss does not identify the better search model.
- **Opening:** λ=0.5 reduces **188→155 qnodes (17.55%)**; λ=2 increases them to **324 (+72.34%)**. Counts are small, but move/score changes are substantial.
- **Sparse tactics:** all three trained models retain the obvious `e4d5` captures; qtrees stay tiny. En-passant raw retains `e5d6` and exact returned draw score zero. These are limited controls, not a tactical strength suite.

Root changes are material: the raw Kiwipete moves are `e1g1` (g134), `e5f7` (λ=0), `e5g6` (λ=0.5), and last-completed-depth-3 `d5e6` (λ=2). Fianchetto changes `f3e5→e2e3→b1c3` across g134/control/selected; Opening changes `b1c3→f2f3` for selected λ=2. Scores are native mapped search units, not centipawns; no oracle establishes whether a changed root move is objectively a blunder. There is no numerical saturation/collapse assertion here, but the primary node-cap regression is an observed operational pathology.

## Calibration control, frozen rather than retuned

Use exactly the prior g134 **AFFINE_SAFE_MEAN** coefficients for both λ=0 and held-out-selected λ=2, and replay original g134 as an anchor:

`T(x) = Math.round(0.9681958511436085*x + 1033.984683470143)`.

`x` is the original integer STM score. This is the previous mean-centered, full-band-safe affine method, **with its existing coefficients frozen**; no candidate-specific fit or qnode optimization occurred. It preserves the normal score band for all possible original normal scores, leaves terminal/mate returns untouched, and throws rather than clipping an inadmissible static. No range failure occurred. λ=0.5 was not calibrated because it was not selected before search results.

| Fixture | Model / mapping | Depth/status | Total | Qnodes | Q% | SP attempts | SP cuts | SP% | Score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kiwipete | g134 / CAL | 4/OK | 169,311 | 160,327 | 94.69 | 137,611 | 64,579 | 46.93 | 9660 / e1g1 | 2.507 |
| Kiwipete | lambda0 / CAL | 4/OK | 345,857 | 336,010 | 97.15 | 288,745 | 155,155 | 53.73 | 4852 / e5f7 | 3.913 |
| Kiwipete | lambda2 / CAL | 4/OK | 559,621 | 544,050 | 97.22 | 492,088 | 284,744 | 57.86 | 2444 / d2c1 | 5.965 |
| Exchanges | g134 / CAL | 4/OK | 1,937 | 693 | 35.78 | 1,272 | 902 | 70.91 | 11194 / e4d5 | 0.013 |
| Exchanges | lambda0 / CAL | 4/OK | 1,915 | 670 | 34.99 | 1,254 | 921 | 73.44 | 12798 / e4d5 | 0.016 |
| Exchanges | lambda2 / CAL | 4/OK | 2,006 | 726 | 36.19 | 1,345 | 945 | 70.26 | 10868 / e4d5 | 0.016 |
| Tactical queen | g134 / CAL | 4/OK | 189 | 8 | 4.23 | 115 | 72 | 62.61 | 7228 / e4d5 | 0.003 |
| Tactical queen | lambda0 / CAL | 4/OK | 199 | 8 | 4.02 | 125 | 79 | 63.20 | 10572 / e4d5 | 0.002 |
| Tactical queen | lambda2 / CAL | 4/OK | 205 | 8 | 3.90 | 141 | 102 | 72.34 | 4976 / e4d5 | 0.002 |
| En passant | g134 / CAL | 4/OK | 547 | 17 | 3.11 | 378 | 293 | 77.51 | 870 / e1e2 | 0.005 |
| En passant | lambda0 / CAL | 4/OK | 374 | 17 | 4.55 | 242 | 176 | 72.73 | 584 / e1d1 | 0.003 |
| En passant | lambda2 / CAL | 4/OK | 314 | 7 | 2.23 | 192 | 121 | 63.02 | 0 / e5d6 | 0.003 |
| Fianchetto | g134 / CAL | 4/OK | 14,107 | 7,346 | 52.07 | 12,462 | 8,542 | 68.54 | -17141 / d1d2 | 0.265 |
| Fianchetto | lambda0 / CAL | 4/OK | 24,305 | 16,454 | 67.70 | 22,287 | 12,598 | 56.53 | -19685 / e2e3 | 0.294 |
| Fianchetto | lambda2 / CAL | 4/OK | 36,157 | 27,154 | 75.10 | 33,117 | 18,367 | 55.46 | -16468 / f3e5 | 0.454 |
| Opening | g134 / CAL | 4/OK | 3,800 | 196 | 5.16 | 2,921 | 2,196 | 75.18 | 190 / b1c3 | 0.066 |
| Opening | lambda0 / CAL | 4/OK | 2,844 | 191 | 6.72 | 2,098 | 1,449 | 69.07 | 775 / b1c3 | 0.048 |
| Opening | lambda2 / CAL | 4/OK | 3,700 | 258 | 6.97 | 2,903 | 2,210 | 76.13 | -2868 / f2f3 | 0.049 |

The four-way comparison is unfavorable to the selected training intervention. Kiwipete λ=0 raw/calibrated is **672,779 / 336,010 qnodes**; λ=2 raw/calibrated is **988,123 capped / 544,050 completed**. Calibration helps λ=2 finish, but it still needs **61.91% more qnodes than calibrated λ=0**, and **3.39×** original calibrated g134's 160,327. Its calibrated SP **57.86%** exceeds control **53.73%**, again without a smaller tree. Calibration changes λ=2's returned Kiwipete move to `d2c1` at completed depth 4; the raw comparison has a different completed horizon.

Fianchetto λ=0 raw/calibrated is **38,689 / 16,454**; λ=2 is **56,566 / 27,154**. Calibration remains useful, but selected λ=2 is **65.03% worse than calibrated control**. Opening is **188/191** versus **324/258**. There is no raw qsearch improvement of selected λ=2 that calibration could merely explain away: its local-error benefit survives raw, while its primary search benefit is absent.

For an offline same-held-out-pair distribution control, apply the same integer mapper and affine transform and divide back by 32,511; this is diagnostic evaluation only, never a training-domain target or gradient:

| Model | Mapping | Held median delta error | Held p95 error | Held base loss |
| --- | --- | --- | --- | --- |
| lambda0 | RAW | 0.242503 | 0.784118 | 0.137350 |
| lambda0 | CAL | 0.242740 | 0.801980 | 0.136873 |
| lambda2 | RAW | 0.200973 | 0.689699 | 0.126709 |
| lambda2 | CAL | 0.197153 | 0.684851 | 0.127624 |

Training provides a local-error improvement that the frozen calibration alone does not: control median **0.242503→0.242740** with calibration, compared with raw auxiliary **0.200973**. This supports a real local learning effect. It does not establish causal sufficiency for stand-pat pruning.

## Same frozen actual-search edges

After model selection/search, freeze eligible capture-only detached samples from **untouched g134's raw tree** and **λ=0's raw tree**, using the accepted stride-101 capture/static-parent/eligible-child criteria. Re-evaluate all four models and the same independent NNUE teacher on these identical boards. All **5,972 retained edges across six fixtures/two populations** are diagnostic test data and were never used for training or selection. Main populations below have 1,405/4,101 Kiwipete and 117/342 Fianchetto edges. These are correlated topology-selected samples, not IID positions.

| Frozen population | Model | N | Delta-error p50 / p95 | Error >0.5 % | Unsupported reversals % | Delta-sign agreement % | Absolute endpoint loss |
| --- | --- | --- | --- | --- | --- | --- | --- |
| g134-tree/middlegame-kiwipete | g134 | 1405 | 0.324817 / 0.920749 | 29.96 | 38.29 | 62.21 | 0.138760 |
| g134-tree/middlegame-kiwipete | lambda0 | 1405 | 0.332871 / 0.898116 | 31.25 | 33.45 | 59.64 | 0.141703 |
| g134-tree/middlegame-kiwipete | lambda0.5 | 1405 | 0.280418 / 0.811161 | 23.56 | 28.33 | 60.93 | 0.134312 |
| g134-tree/middlegame-kiwipete | lambda2 | 1405 | 0.208655 / 0.703984 | 13.74 | 18.15 | 63.56 | 0.139665 |
| g134-tree/quiet-fianchetto | g134 | 117 | 0.207623 / 0.667555 | 10.26 | 4.27 | 52.99 | 0.057777 |
| g134-tree/quiet-fianchetto | lambda0 | 117 | 0.227514 / 0.678199 | 10.26 | 4.27 | 59.83 | 0.048832 |
| g134-tree/quiet-fianchetto | lambda0.5 | 117 | 0.122065 / 0.478587 | 3.42 | 2.56 | 66.67 | 0.042642 |
| g134-tree/quiet-fianchetto | lambda2 | 117 | 0.201446 / 0.479270 | 4.27 | 2.56 | 61.54 | 0.068681 |
| lambda0-tree/middlegame-kiwipete | g134 | 4101 | 0.288465 / 0.854753 | 23.12 | 39.04 | 69.03 | 0.132142 |
| lambda0-tree/middlegame-kiwipete | lambda0 | 4101 | 0.280420 / 0.862524 | 23.02 | 38.09 | 68.20 | 0.134315 |
| lambda0-tree/middlegame-kiwipete | lambda0.5 | 4101 | 0.247516 / 0.773018 | 19.39 | 35.31 | 68.42 | 0.127745 |
| lambda0-tree/middlegame-kiwipete | lambda2 | 4101 | 0.220889 / 0.681456 | 14.51 | 33.43 | 69.13 | 0.136090 |
| lambda0-tree/quiet-fianchetto | g134 | 342 | 0.195746 / 0.651449 | 10.82 | 4.68 | 52.05 | 0.069831 |
| lambda0-tree/quiet-fianchetto | lambda0 | 342 | 0.266590 / 0.712868 | 20.18 | 5.26 | 55.85 | 0.064705 |
| lambda0-tree/quiet-fianchetto | lambda0.5 | 342 | 0.168028 / 0.550218 | 9.06 | 2.92 | 59.65 | 0.051026 |
| lambda0-tree/quiet-fianchetto | lambda2 | 342 | 0.220819 / 0.597642 | 9.94 | 4.97 | 57.02 | 0.079857 |

On the original g134 Kiwipete edges, λ=0→2 median error improves **0.332871→0.208655 (37.32%)**, large errors **31.25%→13.74%**, and unsupported reversals **33.45%→18.15%**. Even relative to untouched g134's **0.324817**, the selected model learns better teacher deltas. On λ=0's tree it improves **0.280420→0.220889 (21.23%)**, with large errors **23.02%→14.51%**. The effect is therefore not confined to the artificial held-out corpus.

However, residual error and absolute quality matter. On original Fianchetto edges, selected λ=2 endpoint loss **0.068681** is worse than λ=0's **0.048832**, even while delta error improves modestly **0.227514→0.201446**. On λ=0's Fianchetto edges it is **0.079857 versus 0.064705**. The conservative model is much better on that local population. On original Kiwipete edges the target absolute delta median/p95 is **0.156473/0.763314**; λ=2 predicts **0.190203/0.503858**, leaving tactical tail underprediction. Lower raw delta magnitude is not evidence of correctness by itself.

The combination of improved identical-edge target agreement and worse realized qsearch demonstrates that this degree/type of local repair is **insufficient**. It does not disprove all capture-consistency objectives or prove that static NNUE targets are wrong. Main-search preferences, absolute errors, windows, newly exposed descendants, population mismatch and optimizer/target-regime interaction remain possible contributors. No qsearch mechanism was changed to hide the regression.

## Explicit answers

| Question | Answer |
| --- | --- |
| 1. Can held-out local-delta error be reduced? | Yes. Selected λ=2: median 0.242500→0.200964, Huber 0.050931→0.039843; both root families improve. |
| 2. Does unsupported capture-neighbour behavior reduce? | Yes in held-out and identical frozen actual-search edges. It remains substantial and does not guarantee a better realized tree. |
| 3. Is ordinary held-out quality acceptable? | No major teacher-relative loss degradation; endpoint and broad-root losses improve. Absolute error remains high and original WDL-mixture preservation is untested. |
| 4. Does SP cutoff rate improve? | Kiwipete yes descriptively (50.52%→51.30% for selected capped prefix; λ=0.5 53.07%). Fianchetto selected worsens (47.28%→44.78%). Rates alone are insufficient. |
| 5. Do Kiwipete qnodes fall materially? | No. Both nonzero raw arms worsen control; λ=2 fails depth 4 at the node cap. |
| 6. Is benefit present independently? | λ=0.5 improves Fianchetto and Opening. Selected λ=2 worsens both. There is no consistent multi-fixture primary benefit. |
| 7. Does the effect remain without calibration? | The target-delta learning effect does. A selected-model qsearch improvement does not exist in the raw results. |
| 8. Is calibration still additive/useful? | Yes as a search control: selected model finishes Kiwipete with 544,050 qnodes and Fianchetto drops to 27,154. Both remain worse than equally calibrated λ=0. |
| 9. Do root moves/scores change? | Yes, materially on Kiwipete, Fianchetto and Opening; sparse obvious captures mostly stable. λ=2 raw Kiwipete has a different completed horizon. |
| 10. Integrate into real trainer for longer training? | No on this unit. Local learnability warrants further isolated diagnosis, while the search gate fails. |
| 11. What failed? | Not absence of delta learning, nor aggregate teacher-held-out base-loss degradation. Failure is inconsistent/adverse qsearch transfer, coefficient/fixture sensitivity, residual tactical underprediction and a base-only replay that already regresses search. Synthetic population and changed target mixture are measured design limitations; teacher truth/strength remains unvalidated. |
| 12. Smallest supported next intervention? | Authenticate and replay a small frozen g134-like original 50:50 base-data/target batch, retain those base examples, and attach teacher-labeled legal capture pairs from its independent training positions. First require a non-regressing base-only control; then reuse a bounded auxiliary comparison and separate qsearch fixtures. No production integration or long campaign yet. |

## Validation performed and limits

**Passed:**

1. Clean Java 21 compilation from archived HEAD plus isolated additions. Exact g134 inference/training payload equality; original codec round-trip for every experimental checkpoint. Java runtime is OpenJDK `21+35-2513`.
2. **6,738 pair board/target checks:** legal generated captures, occupancy decreases by one (including special capture handling inherited from the accepted generator), STM alternation, exact `HeadlessGame.play` board equality, stable parent, active endpoints, deterministic parent/child teacher re-evaluation. Train/held endpoint disjointness and exclusion of benchmark roots passed. NNUE serialized bytes and pinned input files remained unchanged.
3. λ=0 **256-update exact loss and full training-byte comparison**, including every weight, moment and optimizer step, against the unchanged ordinary `train` method. Independent **full 9,768-update ordinary-method replay** produced byte-identical λ=0 model output and final step 227,926.
4. **138 finite-difference parameter checks**, spanning node, both relation endpoints, status, local/board biases, head weights/bias, plus parent-exclusive/child-exclusive parameters. Maximum discrepancy **7.18e−10** (tolerance 2e−6). **Eight scalar derivative checks** verify both endpoints and quadratic/linear Huber regions.
5. **30/30 existing focused tests passed**, zero skips/aborts/failures: `Brn2CoreTest`, `Brn2CodecTest`, `QsearchDecisionTraceTest`, `Brn2SearchIntegrationTest`, `NnueScoreMappingTest`. Cached JUnit Jupiter 5.10.3 / Platform 1.10.3; isolated test classes/output. Focused run duration 10.90 seconds.
6. Untouched g134/NNUE **12 traced-versus-untraced fixture equalities**, all nontiming result/counter fields. Counts reproduce both accepted reports, including g134 calibration 160,327 Kiwipete qnodes. Every main trace checks exact SP/cutoff partition and qchild/node accounting; zero corpus/search overlaps.
7. Selected λ=2 checkpoint loaded/evaluated through **freshly compiled pristine authoritative engine classes**, without experimental trainer/calibration source. Its untraced capped Kiwipete search exactly equals experimental RAW on move, score, completed depth, termination and all checked nontiming counters. This establishes ordinary inference compatibility and unchanged search behavior, beyond static source inspection.
8. Complete disposable existing-file comparison: only trainer and two BRN mapping expressions changed. All qsearch/NNUE/ordering/pruning/extension/codec source bytes unchanged. Final authoritative SHA/status verification and report-only diff checks are performed before commit; results are recorded below.

**Limitations:** one starting checkpoint, one seed/order, one epoch, two artificial root families, 16 held-out path clusters, endpoint/path correlations, lexicographic reply sampling, static teacher rather than exact tactical truth, Huber downweighting large residuals, inherited optimizer moments from another target mixture, and a single capped primary result. No broad strength or original WDL-loss acceptance. The high-level live-store lineage read was unavailable under archived HEAD's older plan reader; direct immutable payload validation succeeded. Finite tests are not a proof for every board/model. Timing is not an independently warmed performance claim.

**Deliberately skipped:** full/long suites; GUI/browser tests (no UI work); normal/multi-generation self-play; new completed-game corpus; promotion arenas, tournaments or strength screens; long campaigns; full store recovery/lineage validation; release/package/build/deployment acceptance. These do not belong to this bounded isolated loss experiment. No architecture, NNUE, qsearch or production configuration change was attempted.

## Provenance and reproduction

The authoritative tree receives only this report. The report is committed alone if its isolated index remains safe; the final commit SHA is supplied in the completion response because a document cannot contain its own final commit hash. No push or deployment is authorized/performed. Disposable source/model/data outputs stay outside the repository and are not candidates for merging or promotion.

Recreate the disposable HEAD archive; extract `CaptureDiagnostic`, `CalibrationScore`, and `CalibrationSearch` from the accepted prior report appendices. Apply Appendix A below. `CalibrationSearch` additionally emits sampled `board` and `parentBoard` long arrays for frozen-edge evaluation; this changes output only. Compile external harnesses with `javac -d classes -sourcepath repo/app/src/main/java ...`. With the exact pinned paths above:

```text
CaptureTraining prepare TRAINING_STATE BRN_MODEL NNUE_MODEL OUTPUT_DIRECTORY
CaptureLossChecks TRAINING_STATE OUTPUT_DIRECTORY/pairs.bin
CaptureTraining 0   TRAINING_STATE BRN_MODEL NNUE_MODEL OUTPUT_DIRECTORY
CaptureTraining 0.5 TRAINING_STATE BRN_MODEL NNUE_MODEL OUTPUT_DIRECTORY
CaptureTraining 2   TRAINING_STATE BRN_MODEL NNUE_MODEL OUTPUT_DIRECTORY
```

Run from the disposable directory. `CaptureTraining`/`CalibrationSearch`/`ControlReplay`/`FrozenEdges` use package `com.ohinteractive.seedv6.tools.search`; `CaptureLossChecks` uses `com.ohinteractive.seedv6.core.brn2`. Training uses `java -Xmx1536m`; search uses `-Xmx1024m`. Search each model with prior `CalibrationSearch` mode `all`, RAW defaults or the frozen `-Dbrn.diag.a`/`-Dbrn.diag.b` coefficients, and `corpus-keys.txt`. Raw g134 mode `rawcheck` reproduces all NNUE/tracing controls. A pristine selected-model mode `replay` verifies normal inference. No prior compiled classes are required.

The exact implementation/test source below makes the key experiment reproducible without retaining disposable binaries. The analysis uses the formulas and populations above; all full search rows are retained here. Raw output hashes identify this execution while those files remain available, not an additional publication requirement.

| Disposable evidence | Bytes | SHA-256 |
| --- | --- | --- |
| experiment-plan.json | 1840 | `296077fd8a819cb2a8ea78e1df0171e8a090be7015a967d3298230b3cd5c4d80` |
| selection.json | 212 | `dc8028f6984d033dc96476c752e1900c84b674a8aac64fd57cd79e0f8263397b` |
| pairs.bin | 921936 | `29d63a2a4310ca91e7a2955438e694354a9deb23bb49ef1e61d43b0a670290a7` |
| pairs.jsonl | 2750137 | `2677f131f59d117bd443cab12e7f9416f0e5d6502501641f8dc2b89eafe5cb59` |
| initial.jsonl | 1666 | `e6be8256eae1ec5fb769775e67dcd1e463a8294204a853970c06f732505da328` |
| lambda0.jsonl | 1745 | `f24473ec2df17a9bd87b7c7fd51a68e616bc266033e003a05ff202c8984aa61b` |
| lambda0.5.jsonl | 1743 | `02c69be05dfc9fc697ac4d540338df8428ed6aca214bda3ecca22abdcd814f5f` |
| lambda2.jsonl | 1745 | `40aeb6bfbdd700ea2f855a8f0ee61fc01d540775521c7bd512bba35465b7252c` |
| analysis.json | 102020 | `c778a5e8ae60d578a043019e2f7f6a414211fc3f93ee72a20fcae04d5a4b9e34` |
| focused-tests.txt | 1160 | `cb1280de69d13a2806736bc6492f4b07ed322d4b5d89e1ea9d6b39c05290b61d` |
| loss-checks.txt | 372 | `77b251c7e834eae3c3fdf0c6d51fd5ca3b4f5600c9096e5367c2c80f89b2017b` |
| control-replay.txt | 188 | `3355caf4f2e154e76ff92c8bc9a0be628161df235ea790a42b2b6c60be5b1d1f` |
| pristine-replay-verification.txt | 167 | `c8c8f919b413986a97d1230f3eefb1e526333c4fd219de40d6e6f38d62679664` |
| g134-rawcheck.jsonl | 1489805 | `c765e5c179b6602cda87edc27bd5481f66ce9e3814fc108d53a293c9948179ba` |
| lambda0-RAW.jsonl | 3270982 | `1671ecd85d7eec2f6224c030032c5865aa265c76304d49698249313db9f80b86` |
| lambda0.5-RAW.jsonl | 3530561 | `1f7f2b26085090684870c416341c17012ffac91f22ee281a69d42dfc9bb79fa2` |
| lambda2-RAW.jsonl | 4692743 | `c6b765aeab2d99fb93fb636f148a36bfb1199e3c68dcb0deae1ec41a6c6aea47` |
| lambda0-CAL.jsonl | 1732266 | `2c3e05c54bb3fa50c21bb2738de89543d76c9a8d003043b9ae2770c7f5ebe67e` |
| lambda2-CAL.jsonl | 2705889 | `27b48142640cd48bb8c687f02cbd3190a37b62ab88fe42d9252d18214c6875c7` |
| g134-CAL.jsonl | 895835 | `48b386f93cfd3cfa8a1664350c89239ec7ef92dbd52f612ef8154fd4daa2f985` |
| frozen-tree-edges.bin | 776348 | `0a24c5f0ddd4362c3c8193bc1631dce40e4b894fda8e86c0b482bfbaba70bb81` |

**Observed pre-report-write verification:** all **540 initial tracked/non-ignored authoritative files** retained their SHA-256 values; full porcelain status exactly matched the initial captured status. All three pinned model/training/teacher input hashes remained unchanged. Root journal/version files remained absent. The post-creation check also verified all 540 inherited hashes, the report as the only new tracked/non-ignored file, an empty index before report staging, absent root journal/version files, and balanced Markdown fences. The report-only staged diff is checked before its isolated commit.

**Human actions required after this prompt: None.** The proposed next isolated experiment is a future decision, not a blocker to completion of this diagnostic. Production integration, playing-strength acceptance and deployment remain unperformed.

## Appendix A — exact isolated implementation and checks

These sources are evidence inside the report, not installed product source. Apply/run only in a fresh disposable copy. The helper classes extracted from the accepted reports supply unchanged legal move generation, search fixtures and JSON output.

### patch_experiment.py

```python
from pathlib import Path
d=Path(__file__).parent
p=d/'repo/app/src/main/java/com/ohinteractive/seedv6/core/brn2/Brn2Trainer.java'
s=p.read_text()
s=s.replace('64 + 2 * (64 * 63 / 2) + 64;', '2 * (64 + 2 * (64 * 63 / 2) + 64);')
addition='''
    /** DIAGNOSTIC ONLY. One ordinary online base example plus a capture-pair Huber term.
     * Both endpoint Jacobians use the same pre-update weights. Values/targets are STM;
     * the common-parent-perspective residual is -neighbour - anchor + targetNeighbour + target.
     * Two calls per pair alternate the base endpoint, preserving normal online base training.
     */
    public double[] trainCapture(long[] board, double target, long[] neighbour, double neighbourTarget,
                                 double lambda, double huber) {
        if (!Double.isFinite(lambda) || lambda < 0 || !Double.isFinite(huber) || huber <= 0
            || !Double.isFinite(target) || Math.abs(target)>1
            || !Double.isFinite(neighbourTarget) || Math.abs(neighbourTarget)>1)
            throw new IllegalArgumentException("Invalid capture loss arguments");
        double y=predict(board), n=predict(neighbour), e=-n-y+target+neighbourTarget;
        double delta=Math.abs(e)<=huber ? .5*e*e : huber*(Math.abs(e)-.5*huber);
        if (lambda==0) return new double[]{train(board,target),delta};
        if (optimizer.step==Long.MAX_VALUE) throw new ArithmeticException("Adam step exhausted");
        for (int k=0;k<touchedCount;k++) slots[touched[k]]=0;
        touchedCount=0;
        Arrays.fill(outputGradient,0); Arrays.fill(boardGradient,0); Arrays.fill(contextGradient,0);
        double aux=-lambda*Math.max(-huber,Math.min(huber,e));
        double derivative=captureContribution(board,y-target+aux)+captureContribution(neighbour,aux);
        long next=optimizer.step+1;
        double c1=1-StrictMath.pow(config.beta1(),next), c2=1-StrictMath.pow(config.beta2(),next);
        update(derivative,c1,c2,false); update(derivative,c1,c2,true); optimizer.step=next;
        return new double[]{.5*(y-target)*(y-target),delta};
    }
    private final double[] captureBoard=new double[HIDDEN_WIDTH],captureContext=new double[HIDDEN_WIDTH];
    private double captureContribution(long[] board,double valueDerivative) {
        double prediction=predict(board),derivative=valueDerivative*(1-prediction*prediction);
        for(int h=0;h<HIDDEN_WIDTH;h++) {
            outputGradient[h]+=derivative*Math.max(0,scratch.boardPre[h]);
            captureBoard[h]=scratch.boardPre[h]>0?derivative*weights[OUTPUT_WEIGHT_OFFSET+h]:0;
            boardGradient[h]+=captureBoard[h];
        }
        Arrays.fill(captureContext,0);
        int nodes=scratch.features.nodeCount();
        for(int i=0;i<nodes;i++) for(int h=0;h<HIDDEN_WIDTH;h++) {
            double g=scratch.localPre[i*HIDDEN_WIDTH+h]>0?captureBoard[h]:0;
            localGradient[i*HIDDEN_WIDTH+h]=g;captureContext[h]+=g;
        }
        for(int h=0;h<HIDDEN_WIDTH;h++) contextGradient[h]+=captureContext[h];
        for(int i=0;i<nodes;i++) add(scratch.features.indexAt(1+i)-BrnFeatureSchema.NODE_OFFSET,localGradient,i*HIDDEN_WIDTH);
        int pair=1+nodes;
        for(int a=0;a<nodes;a++) for(int b=a+1;b<nodes;b++) {
            int relation=scratch.features.indexAt(pair++)-BrnFeatureSchema.RELATION_OFFSET;
            add(RELATION_A_ROW+relation,localGradient,a*HIDDEN_WIDTH);
            add(RELATION_B_ROW+relation,localGradient,b*HIDDEN_WIDTH);
        }
        for(int k=pair;k<scratch.features.size();k++)
            add(STATUS_ROW+scratch.features.indexAt(k)-BrnFeatureSchema.STATUS_OFFSET,captureContext,0);
        return derivative;
    }
'''
assert 'public double[] trainCapture' not in s
s=s.replace('    private void add(',addition+'\n    private void add(')
p.write_text(s)
# The same two-return diagnostic calibration seam used by the accepted report.
p=d/'repo/app/src/main/java/com/ohinteractive/seedv6/search/evaluation/SearchEvaluation.java'
s=p.read_text()
for expression in ['BrnScoreMapping.map(stack[ply].evaluate(board))','BrnScoreMapping.map(definition.brn2.evaluateReference(board, scratch))']:
 assert s.count(expression)==1
 s=s.replace(expression,'com.ohinteractive.seedv6.tools.search.CalibrationScore.map('+expression+', board)')
p.write_text(s)
print('Patched only disposable trainer and two BRN mapping returns')
```

### CaptureTraining.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;

public class CaptureTraining {
 static final long SEED=2026092403L;
 static final double HUBER=.25;
 static final String[] ROOTS={
  "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
  "rnbqkb1r/pp1p1ppp/2p5/4P3/2B5/8/PPP1NnPP/RNBQK2R w KQkq - 0 6"};
 public record Pair(int path,int ply,int branch,long move,long[] parent,long[] child,double tp,double tc) {
  boolean held(){return path%4==0;}
 }
 record Root(int path,int ply,long[] board,double target) { boolean held(){return path%4==0;} }
 static final List<Pair> pairs=new ArrayList<>(); static final List<Root> roots=new ArrayList<>();
 static NnueEvaluator teacher;static long validationCount;static int terminalExcluded,duplicateExcluded;
 static final Set<String> edges=new HashSet<>(); static final Set<Long> forbidden=new HashSet<>();
 static double target(long[] b){teacher.evaluate(b);return teacher.boundedValue();}
 static void require(boolean test,String msg){if(!test)throw new AssertionError(msg);}
 static int occupied(long[] b){return Long.bitCount(b[0]|b[1]|b[2]);}
 static boolean add(int path,int ply,int branch,long[] parent,long move) {
  long[] before=parent.clone(),child=CaptureDiagnostic.play(parent,move);
  require(occupied(parent)==occupied(child)+1,"not capture");
  require(Board.player((int)parent[4])!=Board.player((int)child[4]),"STM didn't change");
  if(forbidden.contains(parent[5])||forbidden.contains(child[5]))throw new AssertionError("Benchmark root contamination");
  var game=new HeadlessGame(parent,4);
  if(!game.active()){terminalExcluded++;return false;}
  game.play(move);require(Arrays.equals(child,game.boardSnapshot()),"Headless transition mismatch");
  if(!game.active()){terminalExcluded++;return false;}
  String id=Long.toHexString(parent[5])+"/"+Move.coordinate(move);
  if(!edges.add(id)){duplicateExcluded++;return false;}
  double tp=target(parent),tc=target(child);
  require(tp==target(parent)&&tc==target(child),"Teacher endpoint mismatch");
  require(Arrays.equals(before,parent),"Parent mutation");
  pairs.add(new Pair(path,ply,branch,move,parent.clone(),child,tp,tc));validationCount++;
  return true;
 }
 static void generate(){
  for(var f:CaptureDiagnostic.POSITIONS)forbidden.add(Board.fromFen(f[1])[5]);
  for(int path=0;path<64;path++){
   var rng=new SplittableRandom(SEED+path);long[] board=Board.fromFen(ROOTS[(path/4)%2]);
   for(int ply=1;ply<=65;ply++){
    var legal=CaptureDiagnostic.legal(board);if(legal.length==0)break;
    board=CaptureDiagnostic.play(board,legal[rng.nextInt(legal.length)]);
    if(!Set.of(16,17,32,33,48,49,64,65).contains(ply))continue;
    if(!new HeadlessGame(board,2).active())continue;
    roots.add(new Root(path,ply,board.clone(),target(board)));
    for(long move:CaptureDiagnostic.legal(board))if(CaptureDiagnostic.capture(board,move)){
     if(!add(path,ply,1,board,move))continue;
     long[] child=CaptureDiagnostic.play(board,move);int replies=0;
     // Bounded continuation: at most two coordinate-first legal capture replies.
     for(long reply:CaptureDiagnostic.legal(child))if(CaptureDiagnostic.capture(child,reply)){
      add(path,ply,2,child,reply);if(++replies==2)break;
     }
    }
   }
  }
  Set<Long> held=new HashSet<>(),train=new HashSet<>();
  for(var p:pairs)for(long key:new long[]{p.parent[5],p.child[5]})(p.held()?held:train).add(key);
  for(var r:roots)(r.held()?held:train).add(r.board[5]);
  require(Collections.disjoint(held,train),"Train/held board-key overlap");
 }
 static double huber(double e){return Math.abs(e)<=HUBER?.5*e*e:HUBER*(Math.abs(e)-.5*HUBER);}
 static double quant(double[] a,double p){a=a.clone();Arrays.sort(a);double x=(a.length-1)*p;int i=(int)x;return a[i]+(a[Math.min(i+1,a.length-1)]-a[i])*(x-i);}
 static Map<String,Object> metrics(Brn2Model m,boolean held,PrintWriter predictions){
  var ws=new Brn2Workspace();var subset=pairs.stream().filter(p->p.held()==held).toList();
  double[] err=new double[subset.size()],deltas=new double[subset.size()],td=new double[subset.size()];
  double base=0,mae=0,dl=0,mean=0;int sign=0,unsupported=0,falseReversal=0,reversals=0,targetReversals=0;
  for(int i=0;i<subset.size();i++){
   var p=subset.get(i);double yp=m.evaluate(p.parent,ws),yc=m.evaluate(p.child,ws);
   double delta=-yc-yp,truth=-p.tc-p.tp,e=delta-truth;
   err[i]=Math.abs(e);deltas[i]=Math.abs(delta);td[i]=Math.abs(truth);
   base+=.25*((yp-p.tp)*(yp-p.tp)+(yc-p.tc)*(yc-p.tc));mae+=(Math.abs(yp-p.tp)+Math.abs(yc-p.tc))/2;dl+=huber(e);mean+=(yp+yc)/2;
   if(Math.signum(delta)==Math.signum(truth))sign++;
   if(Math.abs(e)>.5)unsupported++;
   boolean pr=yp*(-yc)<0,tr=p.tp*(-p.tc)<0;
   if(pr)reversals++;if(tr)targetReversals++;if(pr&&!tr)falseReversal++;
   if(predictions!=null)write(predictions,"path",p.path,"ply",p.ply,"branch",p.branch,"parentKey",Long.toHexString(p.parent[5]),"childKey",Long.toHexString(p.child[5]),"yp",yp,"yc",yc,"tp",p.tp,"tc",p.tc,"delta",delta,"targetDelta",truth,"error",e);
  }
  int n=err.length;double rootBase=0,rootMAE=0;int rn=0;
  for(var r:roots)if(r.held()==held){double e=m.evaluate(r.board,ws)-r.target;rootBase+=.5*e*e;rootMAE+=Math.abs(e);rn++;}
  return fields("pairs",n,"baseLoss",base/n,"targetMAE",mae/n,"deltaLoss",dl/n,"deltaMedianError",quant(err,.5),"deltaP90Error",quant(err,.9),"deltaP95Error",quant(err,.95),"deltaSignAgreementPct",100.*sign/n,"unsupportedErrorOverPoint5Pct",100.*unsupported/n,"falseReversalPct",100.*falseReversal/n,"predictedReversalPct",100.*reversals/n,"targetReversalPct",100.*targetReversals/n,"predictedAbsDeltaMedian",quant(deltas,.5),"predictedAbsDeltaP95",quant(deltas,.95),"targetAbsDeltaMedian",quant(td,.5),"targetAbsDeltaP95",quant(td,.95),"meanSTM",mean/n,"broadRoots",rn,"broadRootBaseLoss",rootBase/rn,"broadRootMAE",rootMAE/rn);
 }
 static void saveCorpus(Path d)throws Exception{
  try(var o=new DataOutputStream(Files.newOutputStream(d.resolve("pairs.bin"),StandardOpenOption.CREATE_NEW));var j=new PrintWriter(Files.newBufferedWriter(d.resolve("pairs.jsonl"),StandardOpenOption.CREATE_NEW))){
   o.writeInt(pairs.size());for(var p:pairs){o.writeInt(p.path);o.writeInt(p.ply);o.writeInt(p.branch);o.writeLong(p.move);for(long x:p.parent)o.writeLong(x);for(long x:p.child)o.writeLong(x);o.writeDouble(p.tp);o.writeDouble(p.tc);
    write(j,"path",p.path,"ply",p.ply,"branch",p.branch,"split",p.held()?"held":"train","move",Move.coordinate(p.move),"parent",Arrays.stream(p.parent).boxed().toList(),"child",Arrays.stream(p.child).boxed().toList(),"targetParentSTM",p.tp,"targetChildSTM",p.tc,"targetDelta",-p.tc-p.tp);}
   o.writeInt(roots.size());for(var r:roots){o.writeInt(r.path);o.writeInt(r.ply);for(long x:r.board)o.writeLong(x);o.writeDouble(r.target);}
  }
  Set<Long> keys=new TreeSet<>();for(var p:pairs){keys.add(p.parent[5]);keys.add(p.child[5]);}for(var r:roots)keys.add(r.board[5]);
  Files.write(d.resolve("corpus-keys.txt"),keys.stream().map(Long::toHexString).toList(),StandardOpenOption.CREATE_NEW);
 }
 static void readCorpus(Path d)throws Exception{
  try(var in=new DataInputStream(Files.newInputStream(d.resolve("pairs.bin")))){
   int n=in.readInt();for(int i=0;i<n;i++){int path=in.readInt(),ply=in.readInt(),branch=in.readInt();long move=in.readLong();long[] p=new long[6],c=new long[6];for(int k=0;k<6;k++)p[k]=in.readLong();for(int k=0;k<6;k++)c[k]=in.readLong();pairs.add(new Pair(path,ply,branch,move,p,c,in.readDouble(),in.readDouble()));}
   n=in.readInt();for(int i=0;i<n;i++){int path=in.readInt(),ply=in.readInt();long[] b=new long[6];for(int k=0;k<6;k++)b[k]=in.readLong();roots.add(new Root(path,ply,b,in.readDouble()));}require(in.read()==-1,"Trailing corpus bytes");
  }
 }
 public static void main(String[] args)throws Exception{
  Path training=Path.of(args[1]),model=Path.of(args[2]),teacherPath=Path.of(args[3]),dir=Path.of(args[4]);
  byte[] state=Files.readAllBytes(training),modelBytes=Files.readAllBytes(model),nnueBytes=Files.readAllBytes(teacherPath);
  var trainer=Brn2Codec.decodeTraining(state);require(Arrays.equals(modelBytes,Brn2Codec.encodeModel(trainer.snapshot())),"g134 training/model mismatch");
  var net=NnueNetworkCodec.decode(nnueBytes);teacher=new NnueEvaluator(net);
  if(args[0].equals("prepare")){
   generate();saveCorpus(dir);
   try(var out=new PrintWriter(Files.newBufferedWriter(dir.resolve("initial.jsonl"),StandardOpenOption.CREATE_NEW))){
    write(out,"type","initial","step",trainer.optimizer().step(),"optimizer",trainer.config().toString(),"pairs",pairs.size(),"roots",roots.size(),"boardChecks",validationCount,"terminalExcluded",terminalExcluded,"duplicateExcluded",duplicateExcluded,"train",metrics(trainer.snapshot(),false,null),"held",metrics(trainer.snapshot(),true,null));
   }
  }else{
   readCorpus(dir);double lambda=Double.parseDouble(args[0]);String name="lambda"+args[0];
   var order=new ArrayList<>(pairs.stream().filter(p->!p.held()).toList());var rng=new SplittableRandom(SEED);
   for(int i=order.size()-1;i>0;i--)Collections.swap(order,i,rng.nextInt(i+1));
   long start=System.nanoTime();double base=0,delta=0;long examples=0;
   // Fixed one epoch. Alternating endpoints keeps the existing online base objective.
   for(var p:order){
    var a=trainer.trainCapture(p.parent,p.tp,p.child,p.tc,lambda,HUBER);base+=a[0];delta+=a[1];examples++;
    var b=trainer.trainCapture(p.child,p.tc,p.parent,p.tp,lambda,HUBER);base+=b[0];delta+=b[1];examples++;
   }
   long elapsed=System.nanoTime()-start;var result=trainer.snapshot();
   byte[] saved=Brn2Codec.encodeModel(result);Files.write(dir.resolve(name+".brn2"),saved,StandardOpenOption.CREATE_NEW);
   require(Arrays.equals(saved,Brn2Codec.encodeModel(Brn2Codec.decodeModel(saved))),"Checkpoint codec roundtrip");
   try(var out=new PrintWriter(Files.newBufferedWriter(dir.resolve(name+".jsonl"),StandardOpenOption.CREATE_NEW));var pred=new PrintWriter(Files.newBufferedWriter(dir.resolve(name+"-held.jsonl"),StandardOpenOption.CREATE_NEW))){
    write(out,"type","trained","lambda",lambda,"huber",HUBER,"epochs",1,"examples",examples,"steps",examples,"finalStep",trainer.optimizer().step(),"trainingNs",elapsed,"onlineBaseLoss",base/examples,"onlineDeltaLoss",delta/examples,"train",metrics(result,false,null),"held",metrics(result,true,pred),"modelSha256",CaptureDiagnostic.sha(dir.resolve(name+".brn2")));
   }
  }
  require(Arrays.equals(nnueBytes,NnueNetworkCodec.encode(net)),"Teacher network mutated");
  require(Arrays.equals(state,Files.readAllBytes(training))&&Arrays.equals(modelBytes,Files.readAllBytes(model))&&Arrays.equals(nnueBytes,Files.readAllBytes(teacherPath)),"Input payload mutation");
  System.out.println("PASS "+args[0]+" pairs="+pairs.size()+" teacher/source payloads unchanged");
 }
}
```

### CaptureLossChecks.java

```java
package com.ohinteractive.seedv6.core.brn2;
import java.nio.file.*;import java.io.*;import java.util.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;
public class CaptureLossChecks {
 record Pair(long[] p,long[] c,double tp,double tc){}
 static void require(boolean c,String s){if(!c)throw new AssertionError(s);}
 static double huber(double e){return Math.abs(e)<=.25?.5*e*e:.25*(Math.abs(e)-.125);}
 static double loss(double[] w,Pair p,double lambda){var s=new Brn2Workspace();double y=s.evaluate(p.p,w),n=s.evaluate(p.c,w);return .5*(y-p.tp)*(y-p.tp)+lambda*huber(-n-y+p.tp+p.tc);}
 public static void main(String[] args)throws Exception{
  byte[] state=Files.readAllBytes(Path.of(args[0]));var pairs=new ArrayList<Pair>();
  try(var in=new DataInputStream(Files.newInputStream(Path.of(args[1])))){
   int n=in.readInt();for(int i=0;i<n;i++){in.readInt();in.readInt();in.readInt();in.readLong();long[] p=new long[6],c=new long[6];for(int k=0;k<6;k++)p[k]=in.readLong();for(int k=0;k<6;k++)c[k]=in.readLong();pairs.add(new Pair(p,c,in.readDouble(),in.readDouble()));}
  }
  var original=Brn2Codec.decodeTraining(state);var zero=Brn2Codec.decodeTraining(state);
  for(var p:pairs.subList(0,128)){
   require(original.train(p.p,p.tp)==zero.trainCapture(p.p,p.tp,p.c,p.tc,0,.25)[0],"lambda0 parent loss");
   require(original.train(p.c,p.tc)==zero.trainCapture(p.c,p.tc,p.p,p.tp,0,.25)[0],"lambda0 child loss");
  }
  require(Arrays.equals(Brn2Codec.encodeTraining(original),Brn2Codec.encodeTraining(zero)),"lambda0 complete weights/moments/step mismatch");
  System.out.println("PASS lambda=0 exact training bytes after 256 existing online updates");
  int checks=0,childOnly=0,parentOnly=0;double maxError=0;
  var model=Brn2Codec.decodeTraining(state).snapshot();
  for(var pair:pairs.subList(0,3))for(double lambda:new double[]{.5,2}){
   double[] w=model.copyWeights();var trained=new Brn2Trainer(model,new BrnAdamConfig(.001));
   trained.trainCapture(pair.p,pair.tp,pair.c,pair.tc,lambda,.25);
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

### ControlReplay.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.brn2.*;
public class ControlReplay {
 public static void main(String[] args)throws Exception{
  CaptureTraining.readCorpus(Path.of("."));
  var order=new ArrayList<>(CaptureTraining.pairs.stream().filter(p->!p.held()).toList());
  var rng=new SplittableRandom(CaptureTraining.SEED);
  for(int i=order.size()-1;i>0;i--)Collections.swap(order,i,rng.nextInt(i+1));
  var trainer=Brn2Codec.decodeTraining(Files.readAllBytes(Path.of(args[0])));
  for(var p:order){trainer.train(p.parent(),p.tp());trainer.train(p.child(),p.tc());}
  byte[] actual=Brn2Codec.encodeModel(trainer.snapshot()), expected=Files.readAllBytes(Path.of("lambda0.brn2"));
  if(!Arrays.equals(actual,expected)) throw new AssertionError("Full control replay differs");
  System.out.println("PASS full ordinary trainer replay: 9768 steps, exact lambda0 model bytes, final step="+trainer.optimizer().step());
 }
}
```

### FrozenEdges.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.brn2.*;import com.ohinteractive.seedv6.core.nnue.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class FrozenEdges {
 public static void main(String[] args)throws Exception{
  var model=Brn2Codec.decodeModel(Files.readAllBytes(Path.of(args[0])));
  var teacher=new NnueEvaluator(NnueNetworkCodec.decode(Files.readAllBytes(Path.of(args[1]))));var ws=new Brn2Workspace();
  try(var in=new DataInputStream(Files.newInputStream(Path.of(args[2])));var out=new PrintWriter(Files.newBufferedWriter(Path.of(args[3]),StandardOpenOption.CREATE_NEW))){
   int count=in.readInt();for(int i=0;i<count;i++){
    String population=in.readUTF(),fixture=in.readUTF();long[] p=new long[6],c=new long[6];for(int k=0;k<6;k++)p[k]=in.readLong();for(int k=0;k<6;k++)c[k]=in.readLong();
    double yp=model.evaluate(p,ws),yc=model.evaluate(c,ws);teacher.evaluate(p);double tp=teacher.boundedValue();teacher.evaluate(c);double tc=teacher.boundedValue();
    write(out,"population",population,"fixture",fixture,"yp",yp,"yc",yc,"tp",tp,"tc",tc,"delta",-yc-yp,"targetDelta",-tc-tp,"error",-yc-yp+tc+tp);
   }
   if(in.read()!=-1)throw new AssertionError("Trailing bytes");
  }
 }
}
```
