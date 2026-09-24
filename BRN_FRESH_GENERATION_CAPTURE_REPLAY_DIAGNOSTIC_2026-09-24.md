# BRN fresh-generation capture replay diagnostic — 2026-09-24

**Classification: B — MIXED GENERALIZATION. Do not integrate the auxiliary loss into the production trainer yet.** The local learning benefit survives genuinely new generation-scale data, and raw qsearch improves on several previously unseen positions. The evidence does not meet the broader positive-generalization acceptance conditions: aggregate savings are dominated by one position, one entire source-game group regresses, the fresh ordinary control is unstable on continuity fixtures, and the strictly novel held-out population is small and entirely drawn-game material.

On the strictly novel holdout, λ=0→2 median capture-delta error improves **0.236691→0.145794 (38.40%)**, p90 **0.464349→0.413258 (11.00%)**, and ordinary 50:50 campaign loss **0.050592→0.043920 (13.19%)**. But p95 error worsens **0.545364→0.564570 (3.52%)**, and error >0.5 rises **4/56→5/56**.

Across eight frozen new search positions, all at completed depth 4, qnodes fall **332,925→89,838 (73.02%)**, median per-position ratio is **0.821696**, and **5 improve / 3 regress**. Aggregate stand-pat cutoff rate rises **41.27%→58.23%**. However, fresh-03 alone saves **247,107 qnodes**, exceeding the complete suite's **243,087** net saving. Removing that position gives **67,269→71,289 (+5.98%)**, despite 4/7 positions still improving. This is useful transfer, not broad near-resolution.

Kiwipete's λ=0 control hits the million-node cap after depth 3 (**992,510 qnodes**); λ=2 completes depth 4 with **951,273 qnodes**, still **4.54× untouched g134's 209,297**. Those two arm counters have different completed horizons and do not establish a 4.15% full-depth speedup. Fianchetto improves **92,037→18,404 qnodes (80.00%)**, but remains above untouched g134's 13,007. No playing-strength claim is made.

## Scope, governance and relationship to accepted evidence

- Date: 2026-09-24 UTC and New Zealand local date. Authoritative repository: `C:\projects\seed\java\seedv6`; inspected HEAD **`6eb0df1a1a48c646b1850ceaab5a397c5110eba1`**.
- Read the four accepted reports: [stand-pat](BRN_QSEARCH_STAND_PAT_DIAGNOSTIC_2026-09-24.md), commit `8ea5cb99b32330c1b85ef8a918dfb24ae5336109`; [calibration](BRN_QSEARCH_CALIBRATION_DIAGNOSTIC_2026-09-24.md), commit `b1d224f3a51e5bb720a15e6339db6023291b6ff2`; [initial capture training](BRN_CAPTURE_CONSISTENCY_TRAINING_DIAGNOSTIC_2026-09-24.md), commit `339b0d763e12141b24918190fd5b9adc00450c5c`; and [campaign-faithful replay](BRN_CAMPAIGN_TARGET_CAPTURE_REPLAY_DIAGNOSTIC_2026-09-24.md), commit `6eb0df1a1a48c646b1850ceaab5a397c5110eba1`. This fifth unit tests the fourth unit's λ=2 formulation on a newly generated batch; it does not replace its bounded historical positive result.
- No on-disk ancestor/repository `AGENTS.md` was found. User-supplied governance and `source/CHESS_SEARCH_CONTRACT.md` apply. No conflict requires a contract change. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` are independently absent; no capability was activated or file created. First successful machine-clock observation was `2026-09-24T03:26:32Z`; the initial unsupported `Get-Date -AsUTC` failed, with no earlier timestamp inferred.
- Initial index empty; **41 modified tracked files and 46 individually enumerated untracked files**, including GUI/training changes and `app/bin/`, were inherited and remain unattributed. SHA-256 inventory covers **542** initially present tracked/non-ignored files.
- All experimental source, data, payloads, classes and raw output stayed in `C:\Users\Central\AppData\Local\Temp\seedv6-brn-fresh-generation-20260924`. A clean `git archive HEAD` copy excluded inherited edits. This report is the only task-created authoritative file.
- The only modified existing file in the isolated archive is `core/brn2/Brn2Trainer.java`. It is **byte-identical to the fourth experiment's isolated auxiliary trainer**. Authoritative production trainer, NNUE, all search/qsearch rules, move ordering, pruning, extensions/reductions, depth/node limits, checkpoint schema, defaults and GUI are untouched. No calibration seam was applied.

## Authenticated starting checkpoint and teacher

Exact starting directory: `E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c`. Generation **134**, optimizer step **218,158**, training depth **4**; canonical BRN-2/schema 2, width 32, 1,645,665 binary64 parameters. `network.brn2` exactly equals the inference snapshot decoded from `training.state`. Both arms restore identical full state bytes, including Adam first/second moments, configuration and step; no optimizer reset or inferred reconstruction.

| Payload | SHA-256 |
| --- | --- |
| g134 network.brn2 | 21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924 |
| g134 training.state (complete weights/optimizer identity) | ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9 |
| NNUE g74 network.nnue | 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9 |
| λ=0 final inference | 0bb06bc6db1bd1841a5996f3d07b97c721bf88d915af6ae26a072422b25294a8 |
| λ=0 final complete training state | 1a8ac26828e38b7affe62823251764047ebf256ea031c7c29642ebae88ae01d3 |
| λ=2 final inference | a1113b551a494d7f0243bce49fc07260371161191c30c02faa4891090347288c |
| λ=2 final complete training state | 4015208f0a222a82d31e89f04cf0ad4aa3308a6447673a73112d6436ec6707ca |

Exact immutable teacher path: `E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6\network.nnue`. Read-only checksummed manifest/plan parsing verifies the same HANDCRAFTED source, NNUE_BLENDED weight **0.5**, and teacher pin as the accepted fourth report. Generation-134 plan hash: `c0a61a2583acd182478d4afe4ceaa521d05170f67ce50287fadb5ed159973a78`. Saved source/teacher roles are distinct: handcrafted search generates terminal games; pinned NNUE supplies static teacher values. There is no BRN-generated self-play distribution in this lineage. The archived high-level reader predates the store’s v4 plan field; direct framed/hash-checked inspection was used without a schema edit or store recovery. Full live-store operational acceptance is not claimed.

## One fresh production-equivalent data generation

Call the **unmodified** production `SelfPlayBatch.generateHandcrafted` / `SelfPlayRunner` on the standard starting position. Use the exact relevant campaign settings: **64 games, search depth 4, six root-search workers, random opening length 0–8 plies, maximum 32 samples per completed game, maximum 1,024 game plies, no per-move node/time cap**. Each game has private TT/order state, reused within that game. The batch is sequential across games. No candidate publication, validation, promotion or second generation follows it.

Use the lineage's next indexed seed domain, nominal generation **135**, master/data seeds **1/1**. This is an isolated generation-135 *data seed*, not a published g135 checkpoint or resumed production campaign:

```text
generation seed = -8204741532265061904
whole-game split seed = -6965945134292404706
training shuffle seed = 6164425025604425063
capture-child seed = 2026092405
seed(g, domain) = SplittableRandom(1 XOR domainSalt XOR (g * 0x9E3779B97F4A7C15)).nextLong()
domain salts: SELF_PLAY 0x6A09E667F3BCC909; HOLDOUT 0xA54FF53A5F1D36F1; SHUFFLE 0xBB67AE8584CAA73B
gameSeed(i) = SelfPlayRunner.gameSeed(generationSeed, i)
```

The formula reproduces all three authenticated generation-134 seeds before generation. All **64 fresh game seeds are distinct from all indexed game seeds under the 134 stored generation plans**; the generation seed is also new. Source identifiers are `(isolated nominal generation 135, gameIndex, gameSeed, sampled pre-move ply)`; exact six-long boards, WDL labels and keys are frozen. A new RNG seed does not guarantee a new chess position: the overlap audit below measures this explicitly.

All **64 games completed**, with **28 White wins / 8 draws / 28 Black wins**, no aborted, capped or failed games. **8,903** played/pre-move positions, game lengths **65–357**, mean **139.109375** plies, produce **2,048 ordinary samples**. Runtime was **38.576 seconds**, secondary evidence. Production sampling is result-independent: `floor(i*(positions-1)/(count-1))`, including both ends; every game here supplies its full 32 samples. WDL is taken from actual terminal result and flipped for Black STM; every persisted row was checked against its source-game termination.

The native `BootstrapPartition.split` reserves 13 games and trains on **51 games / 1,632 samples**. This is one normal generation's actual update count, not an arbitrary 1,585-step match to the previous batch (which had shorter games). Both arms make exactly one seeded shuffled pass: **epochs=1, minibatch=1, 1,632 Adam updates**, final step **219,790**. Restored learning rate **0.001**, β1 **0.9**, β2 **0.999**, ε **1e−8**. Equal base sample weighting, no rebalancing, new schedule, clipping or weight decay. Train WDL counts (-1,0,+1): **705/96/831**.

## Freshness and TRAIN / HELD-OUT / SEARCH-TEST separation

This corpus was **generated afresh**, not loaded from the historical g133 replay or synthetic training corpus. Historical files were read only for exclusion/provenance. It is **not wholly position-novel**: repeated short random openings and deterministic handcrafted continuations reproduce substantial historical material. Keeping ordinary TRAIN intact preserves production sample weighting and update count; deleting its natural repetitions would change the experiment.

| Source split | Games | Observations / unique keys | Overlap with g133 replay base observations | Overlap with any historical training observations | Overlap with any historical train/held observations |
| --- | --- | --- | --- | --- | --- |
| TRAIN | 51 | 1632 / 1363 | 329 | 747 | 781 |
| HELD | 7 | 224 / 216 | 7 | 131 | 131 |
| SEARCH | 6 | 192 / 187 | 37 | 101 | 101 |

The historical audit covers **134 saved sample files, 144,051 distinct historical TRAIN keys and 176,859 train-or-held keys**. Previous replay/synthetic diagnostic parent/child keys are also excluded from novel evaluation. Of new TRAIN's 1,632 observations, **329 (20.16%)** match a g133 replay base key (267 match its TRAIN keys), **747 (45.77%)** match any historical TRAIN key, and **781 (47.86%)** match any saved historical train/held key. **851 observations from 28 training source games** are absent from that broader history. Fresh source generation is established; complete training-position independence is not claimed. Key equality ignores move counters and catches ordinary transpositions; no claim is made about symmetry-equivalent positions, every historical unsampled trajectory/search node, or unsaved training exposure.

Keep native TRAIN game assignment unchanged. Split the thirteen reserved source games *before updates*: first six ascending IDs become SEARCH, the remaining seven HELD. Remove overlapping keys only when forming novel evaluation subsets. The held-out ordinary set excludes any parent present in historical train/held data, previous diagnostic endpoints, or fresh TRAIN parent/selected auxiliary-child keys. Held capture pairs additionally require both endpoints absent. SEARCH roots exclude that entire union and **all** HELD parent/selected-child keys. No source game belongs to more than one logical partition.

| Role | Source game IDs | Retained evaluation |
| --- | --- | --- |
| TRAIN | 0, 1, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 17, 19, 20, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 39, 40, 43, 44, 45, 47, 48, 49, 50, 51, 53, 54, 55, 56, 58, 59, 60, 61, 62, 63 | 1,632 unchanged base rows; 990 selected auxiliary pairs |
| HELD source | 27, 38, 41, 42, 46, 52, 57 | 224 native rows / 136 native pairs |
| HELD novel | 38, 42, 46 | 93 ordinary rows (31/game); 56 endpoint-disjoint pairs (15/21/20) |
| SEARCH source | 2, 3, 7, 16, 18, 21 | 192 source rows; 91 eligible novel roots in games 2,7,21 |
| SEARCH frozen suite | 2, 7, 21 | 8 unique roots: 3 / 3 / 2 per source game |

**Material holdout limitation:** all three historically novel HELD games are draws: game 38 fifty-move rule, games 42/46 insufficient material. The 93-row primary holdout therefore has WDL=0 throughout. The 224-row native held population contains (-1,0,+1) counts **35/128/61**, but 131 observations are historically familiar. Neither population establishes novel decisive-game WDL retention. Novel evaluation is independent of current gradient updates and saved historical samples, but it is not an IID set of 93 independent games.

Corpus, targets, child choices, partitions and suite were frozen by **2026-09-24T03:35:36.701763Z**, before control training completed at 03:35:40Z and auxiliary training at 03:36:29Z. Held metrics were saved at **03:37:07Z**, before comparative searches. No model selection, λ selection, retraining or dataset revision followed benchmark results.

| Frozen artifact | SHA-256 |
| --- | --- |
| generation-progress.jsonl | 1ba4e7933a24d1f9a4fd64ebbeffe5d1f502386030f8c188c1e7d222042557e2 |
| fresh-base.bin | c560e77ada7f79a9c8a27b607bf7ad1ba83451f001070531e69525a74316c856 |
| fresh-rows.bin | 5f805f70d4e0d701d607e8d9d50c52ca644ac4e7c0bfce79f4e112726d5e38d9 |
| search-suite.tsv | e308ca5f766ebf8ba58ef0f90e26a3b0a7056896071a729391c5cdcacc5b14dd |
| history-all-keys.txt | 66655c6be78cdf3aa340e86ef3decddd2acfd0af71305aa2a8568ec676bacfb8 |

## Exact ordinary and auxiliary objectives

```text
w = actual terminal WDL in parent STM orientation
n_p, n_c = pinned NNUE boundedValue() = tanh(raw), each native STM
y_p, y_c = continuous BRN tanh values, each native STM
t_parent = 0.5*w + 0.5*n_p
L_base = 0.5*(y_p-t_parent)^2

target_delta = 0.5*(-n_c-n_p)
predicted_delta = -y_c-y_p
e = predicted_delta-target_delta
Huber_0.25(e) = 0.5*e^2 if |e|<=0.25; otherwise 0.25*(|e|-0.125)
L_step = L_base + lambda*I(has_eligible_capture)*Huber_0.25(e)
lambda = 0 or 2 only
```

The shared terminal anchor cancels because the derivation-only child target is `-0.5*w+0.5*n_c`. No synthetic child gets an invented completed-game result or a base update. Parent targets remain legitimate unchanged campaign targets. The auxiliary target is **half the common-parent-perspective NNUE delta**, exactly as in the accepted fourth report; no teacher-only substitution or zero-delta smoothing.

For each base row, enumerate coordinate-sorted legal captures, exclude terminal/rule-drawn endpoints, and choose one uniformly with the frozen capture RNG. Process the frozen corpus in source-game/sample order; child selection is independent of either BRN arm and of teacher score. Ten terminal candidate children were excluded. A base row without a capture still gets its ordinary update. There are **990 paired TRAIN rows / 1,632**. Every selected transition matches `HeadlessGame.play`, occupancy decreases by one, STM flips, and shared-anchor cancellation holds to binary64 arithmetic tolerance.

The accepted robust implementation aggregates both endpoint Jacobians at the same pre-update weights and makes **one** sparse Adam update including the unchanged parent base gradient. Both native-STM auxiliary output derivatives are `-lambda*clip(e,-0.25,0.25)`. The λ=0 branch returns directly to ordinary `train` **before child evaluation**. Every arm sees identical initial state, frozen rows, Fisher–Yates order, split, teacher, optimizer configuration and update budget. The final scheduled checkpoint is used without search-based checkpoint choice.

The pristine production λ=0 path, independent frozen-target ordinary loop, and auxiliary-API zero branch produce **byte-identical complete final training state over all 1,632 updates**. Another 128-update exact loss/state check passes. λ=2 uses the previous auxiliary trainer byte-for-byte. Its 72 finite-difference parameter checks (λ=2 only) cover all parameter families and parent-/child-exclusive parameters; maximum error **9.62e−10**, tolerance 2e−6. Eight scalar derivative checks cover both Huber regions. No λ=0.5 arm or sweep ran.

| Arm | Updates / final step | Online base loss | Online auxiliary per base step |
| --- | --- | --- | --- |
| λ=0 | 1,632 / 219,790 | 0.060567 | 0 |
| λ=2 | 1,632 / 219,790 | 0.063162 | 0.016169 |

## Held-out evaluation, completed before search

Losses are equally weighted mean half-squared errors. The campaign uses one scalar blended target, not a sum of independently trained WDL and NNUE heads. Descriptive component losses are `0.5*(y-w)^2` and `0.5*(y-n)^2`; `L_base=0.5*L_WDL+0.5*L_NNUE-0.125*(w-n)^2`. Continuous unrounded values are used. Quantiles interpolate at `(n-1)*p`.

| Model | Train base | Native HELD base (224) | Novel HELD base (93) | Novel WDL component | Novel NNUE component |
| --- | --- | --- | --- | --- | --- |
| g134 | 0.080643 | 0.037979 | 0.049474 | 0.049852 | 0.119410 |
| lambda0 | 0.032118 | 0.033475 | 0.050592 | 0.043788 | 0.127710 |
| lambda2 | 0.039098 | 0.032686 | 0.043920 | 0.031432 | 0.126721 |

| Model | Novel pairs | Delta-error median / p90 / p95 | Huber | Error >0.5 | Delta-sign agreement |
| --- | --- | --- | --- | --- | --- |
| g134 | 56 | 0.183483 / 0.574967 / 0.680703 | 0.034953 | 14.29% | 60.71% |
| lambda0 | 56 | 0.236691 / 0.464349 / 0.545364 | 0.036746 | 7.14% | 58.93% |
| lambda2 | 56 | 0.145794 / 0.413258 / 0.564570 | 0.026077 | 8.93% | 60.71% |

| Model | Native pair median / p90 / p95 (136) | Native >0.5 | Native sign agreement |
| --- | --- | --- | --- |
| g134 | 0.187424 / 0.528099 / 0.674129 | 12.50% | 58.82% |
| lambda0 | 0.222051 / 0.470550 / 0.514780 | 5.88% | 59.56% |
| lambda2 | 0.155691 / 0.380517 / 0.491347 | 5.15% | 63.24% |

The **primary comparison is λ=2 versus fresh λ=0**. Novel median and p90 improve, Huber falls 29.03%, and sign agreement rises 58.93%→60.71%. The novel p95 and large-error count regress; the familiar/native holdout instead improves both tails. The stricter median improvement also beats untouched g134 (0.183483), while λ=2 ordinary novel loss is below g134 (0.049474). The native ordinary loss improves only 2.36% against λ=0; the larger novel gain is conditional on the three drawn-game sources.

| Novel source game | N ordinary / pairs | λ=0→2 base loss | λ=0→2 median delta error |
| --- | --- | --- | --- |
| 38 | 31 / 15 | 0.045241 → 0.038114 | 0.257021 → 0.214270 |
| 42 | 31 / 21 | 0.064230 → 0.049928 | 0.182676 → 0.109424 |
| 46 | 31 / 20 | 0.042306 → 0.043719 | 0.257654 → 0.167432 |

Median delta error improves in all three held source games. Ordinary loss improves in two and slightly worsens in game 46. Three clusters are too few for a persuasive generalization confidence interval; no pseudo-IID significance claim is made.

| Model | Raw absolute delta median / p95 | Raw signed delta p05 / p50 / p95 | Novel STM mean / SD | Novel STM p05 / p50 / p95 |
| --- | --- | --- | --- | --- |
| g134 | 0.224432 / 0.644752 | -0.501557 / 0.003361 / 0.383662 | 0.014026 / 0.315448 | -0.524148 / 0.025521 / 0.513072 |
| lambda0 | 0.233324 / 0.586086 | -0.515673 / -0.073186 / 0.367899 | 0.030664 / 0.294338 | -0.481216 / 0.072949 / 0.457943 |
| lambda2 | 0.174359 / 0.421015 | -0.397871 / 0.026066 / 0.333471 | 0.022446 / 0.249719 | -0.411733 / 0.023001 / 0.383646 |

The same 56-pair target delta distribution has absolute median/p95 **0.066483 / 0.310069**, signed p05/p50/p95 **-0.303874 / -0.000576 / 0.131998**. Raw means uncalibrated bounded BRN output, not pre-tanh logits. Error SD falls **0.298261→0.252535**, and mean-centered absolute-error median **0.235236→0.127483**. A simple output offset alone does not explain the local learning result. Reduced raw delta magnitude is never counted as correctness without comparison to target delta.

## Search-suite selection and exact frozen positions

Initial plan aimed for ten roots, five capture-rich (≥3 legal captures) and five quieter (≤1), maximum two per source game. Historical/cross-partition exclusion left **91 eligible roots from only three of the six SEARCH games**. That cap could not yield 8–12 positions. **Before either arm trained or any benchmark ran**, record a mechanical availability amendment: four capture-rich roots then four quieter roots; iterate ascending game IDs round-robin, choosing each game's smallest unsigned board key not already selected, maximum three roots per game. This produces **eight** roots with the same exclusions, without regenerating data, inspecting trained scores or consulting qnodes. The suite never changed after freezing. Selection observes only legality, capture count, source IDs and board keys.

There are three independent fresh source trajectories, not eight independent game clusters. The SEARCH source games include a White win (2), repetition draw (7), and Black win (21). Every selected root is active, FEN round-trips to its complete board, and has zero key overlap with TRAIN endpoints, HELD endpoints, saved historical train/held positions or previous diagnostic endpoints. Search *descendants* are not claimed globally unseen; singleton search histories intentionally match prior benchmark settings.

| ID | Game / sample / pre-move ply | Legal captures | Position key | FEN |
| --- | --- | --- | --- | --- |
| fresh-01 | 2 / 11 / 22 | 5 | 45c689d391c03d9 | `r2q1rk1/1p3ppp/2p1bn2/p1Pp2B1/2nQ3P/P1N5/1P2PPP1/2KR1B1R w - - 0 12` |
| fresh-02 | 7 / 4 / 22 | 5 | 1da931e2fe2aca1 | `r1b2rk1/pppp2pp/2n5/4pp2/B1P1Pn2/2b2N2/PP1P1PPP/R1B2RK1 w - - 0 12` |
| fresh-03 | 21 / 7 / 18 | 3 | 8e1794e35456d2dd | `r2qkbnr/pp1b2p1/8/1Bp1pp1p/P2nP3/1PN5/2PPQ1PP/1RB1K1NR w Kkq - 1 10` |
| fresh-04 | 2 / 16 / 33 | 4 | 2c1b60a8f2665e41 | `2r2rk1/1p3ppp/2p5/p1Pp3P/3QP1b1/P1N2P2/6P1/2Kn1B1R b - - 1 17` |
| fresh-05 | 2 / 30 / 61 | 0 | 5e8df6370ae376 | `7R/1p1r1pk1/2p5/p1P1Q3/2B3P1/PK6/6P1/3r4 b - - 8 31` |
| fresh-06 | 7 / 22 / 124 | 1 | 428f819a1b5ab33 | `8/8/5K2/2k4r/7p/8/7R/8 w - - 4 63` |
| fresh-07 | 21 / 5 / 13 | 1 | 20c30e35daa3942f | `rn1qkbnr/pp1b1pp1/8/1Bp1p2p/P3p3/1PN5/2PPQPPP/1RB1K1NR b Kkq - 3 7` |
| fresh-08 | 7 / 14 / 79 | 0 | e7ec86e1a5f3f4a | `8/5k2/p5pp/8/r1PK4/5R1P/7P/8 b - - 7 40` |

| Source game | Seed | Termination |
| --- | --- | --- |
| 2 | -1953140147931528288 | White checkmates Black |
| 7 | 7409407437269207440 | Threefold repetition |
| 21 | 5203947342537787807 | Black checkmates White |
| 38 | 4440243895490559442 | Fifty-move rule |
| 42 | 2157949755712825365 | Insufficient material |
| 46 | 2714235854480135142 | Insufficient material |

## Control audit and identical search conditions

Use raw unchanged search for g134, λ=0 and λ=2: **requested depth 4, one worker, iterative deepening, fresh 262,144-entry TT, singleton root history, cumulative 1,000,000 entered-node and 60-second caps**. Neural mate-distance-only selectivity and existing qsearch soft limit 16/check evasions are unchanged. Existing `QsearchDecisionTrace` uses pinned NNUE shadow stride 1 and detached sample stride 101, limit 12,000. It supplies no decisions. Qnodes count entered qsearch children, excluding main leaves reused as qsearch roots. SP rate is cutoffs/eligible attempts; Q% is q/total. Scores are native mapped search units, not centipawns. Timing includes JIT/trace and is secondary.

The control is **not a clean reproduction of the historical control gate**. g134→fresh λ=0:

- NEW suite: **201,102→332,925 qnodes (+65.55%)**, all eight complete depth 4; four positions improve and four regress, median ratio **1.15034**. The largest deterioration is fresh-03 **37,444→265,656 (7.09×)**, offset by substantial improvements elsewhere.
- Kiwipete: **209,297 completed depth 4→992,510 in a million-node prefix completing depth 3**. Fianchetto: **13,007→92,037 (7.08×)** at depth 4. Opening: 220→320; En-passant: 18→24.
- Audit found no unexpected source change, target mismatch, optimizer reset or update-count discrepancy. The generator/settings match the authenticated lineage; all targets equal production arithmetic; complete initial state restoration and 1,632-update production/manual/zero-aux equality pass. The evidence therefore supports **ordinary continuation instability on these continuity positions**, conditional on this one fresh batch/order, rather than a diagnosed experimental mismatch.

The preregistered stop check was systemic deterioration on the primary new suite: multiple newly capped/depth-failing positions, or >3× aggregate qnodes with a majority >2× worse. It did not trigger. The control audit was recorded at **03:38:38Z before λ=2 search**. Continuing was judged meaningful because the new-suite comparisons retain matched completed depths and the control is not uniformly broken; the severe continuity failures remain visible and prevent claiming a clean acceptance gate. This is a narrower basis than the previous historical gate, not a silent assertion that the control is healthy. A copied narrative ratio was corrected to the measured 1.6555 before interpretation; thresholds/results were unchanged.

All raw benchmark payloads load through separately compiled **pristine engine classes**, which contain no auxiliary trainer method (`javap` verified). Only λ=0 Kiwipete is node-capped. No time cap occurs. Four targeted trace/no-trace replays for λ=0/2 on fresh-01/Kiwipete exactly match all nontiming counters, moves, scores and termination, including the control's cap.

## Full fresh-suite search results

| Position | Model | Completed depth / status | Total nodes | Qnodes | Q% | SP attempts | SP cutoffs | SP% | Root score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| fresh-01 | g134 | 4 / OK | 146,623 | 132,071 | 90.08 | 135,659 | 62,133 | 45.80 | -678 / e2e4 | 2.360 |
| fresh-01 | lambda0 | 4 / OK | 39,141 | 32,624 | 83.35 | 35,181 | 19,746 | 56.13 | 3579 / e2e4 | 0.899 |
| fresh-01 | lambda2 | 4 / OK | 55,254 | 44,695 | 80.89 | 50,611 | 30,575 | 60.41 | 3575 / e2e4 | 1.044 |
| fresh-02 | g134 | 4 / OK | 12,514 | 9,957 | 79.57 | 11,347 | 3,614 | 31.85 | -13055 / d2c3 | 0.169 |
| fresh-02 | lambda0 | 4 / OK | 15,920 | 13,686 | 85.97 | 14,587 | 4,100 | 28.11 | -10651 / d2c3 | 0.211 |
| fresh-02 | lambda2 | 4 / OK | 5,674 | 3,442 | 60.66 | 4,981 | 2,723 | 54.67 | -3482 / d2c3 | 0.092 |
| fresh-03 | g134 | 4 / OK | 44,137 | 37,444 | 84.84 | 36,778 | 18,532 | 50.39 | -2148 / e2d1 | 0.535 |
| fresh-03 | lambda0 | 4 / OK | 275,480 | 265,656 | 96.43 | 240,828 | 90,062 | 37.40 | -7581 / d2d3 | 3.055 |
| fresh-03 | lambda2 | 4 / OK | 23,295 | 18,549 | 79.63 | 19,441 | 11,274 | 57.99 | -3206 / b5d7 | 0.396 |
| fresh-04 | g134 | 4 / OK | 16,738 | 12,917 | 77.17 | 14,677 | 6,363 | 43.35 | -16000 / d1c3 | 0.201 |
| fresh-04 | lambda0 | 4 / OK | 8,538 | 4,845 | 56.75 | 7,207 | 4,401 | 61.07 | -16685 / d1c3 | 0.081 |
| fresh-04 | lambda2 | 4 / OK | 13,262 | 9,515 | 71.75 | 11,384 | 5,368 | 47.15 | -14926 / d1c3 | 0.141 |
| fresh-05 | g134 | 4 / OK | 4,844 | 1,282 | 26.47 | 3,977 | 3,348 | 84.18 | -32764 / f7f6 | 0.046 |
| fresh-05 | lambda0 | 4 / OK | 4,614 | 1,218 | 26.40 | 3,791 | 3,232 | 85.25 | -32764 / f7f6 | 0.039 |
| fresh-05 | lambda2 | 4 / OK | 4,745 | 1,280 | 26.98 | 3,893 | 3,283 | 84.33 | -32764 / f7f6 | 0.040 |
| fresh-06 | g134 | 4 / OK | 2,827 | 762 | 26.95 | 2,188 | 1,454 | 66.45 | -5500 / h2h3 | 0.017 |
| fresh-06 | lambda0 | 4 / OK | 2,044 | 531 | 25.98 | 1,539 | 1,029 | 66.86 | -3239 / h2c2 | 0.013 |
| fresh-06 | lambda2 | 4 / OK | 1,781 | 421 | 23.64 | 1,318 | 749 | 56.83 | -3815 / h2c2 | 0.010 |
| fresh-07 | g134 | 4 / OK | 9,823 | 6,090 | 62.00 | 7,919 | 4,657 | 58.81 | 2372 / b8c6 | 0.149 |
| fresh-07 | lambda0 | 4 / OK | 19,286 | 13,583 | 70.43 | 15,493 | 7,970 | 51.44 | 1141 / g8f6 | 0.249 |
| fresh-07 | lambda2 | 4 / OK | 16,088 | 11,553 | 71.81 | 13,208 | 7,005 | 53.04 | 2913 / b8c6 | 0.210 |
| fresh-08 | g134 | 4 / OK | 2,495 | 579 | 23.21 | 1,955 | 1,322 | 67.62 | 4297 / f7e7 | 0.015 |
| fresh-08 | lambda0 | 4 / OK | 3,452 | 782 | 22.65 | 2,699 | 2,071 | 76.73 | 4387 / f7e7 | 0.027 |
| fresh-08 | lambda2 | 4 / OK | 1,475 | 383 | 25.97 | 1,180 | 755 | 63.98 | 4927 / f7e8 | 0.010 |

## Aggregate new-suite results and outlier sensitivity

| Model | Total nodes | Total qnodes | SP attempts / cutoffs | Aggregate SP% | Capped positions |
| --- | --- | --- | --- | --- | --- |
| g134 | 240,001 | 201,102 | 214,500 / 101,423 | 47.28 | 0 |
| lambda0 | 368,475 | 332,925 | 321,325 / 132,611 | 41.27 | 0 |
| lambda2 | 121,574 | 89,838 | 106,016 / 61,732 | 58.23 | 0 |

| Position | λ=2 / λ=0 qnode ratio | Qnode change | SP percentage-point change | Move comparison |
| --- | --- | --- | --- | --- |
| fresh-01 | 1.370004 | 12,071 | +4.28 | same |
| fresh-02 | 0.251498 | -10,244 | +26.56 | same |
| fresh-03 | 0.069823 | -247,107 | +20.59 | d2d3 → b5d7 |
| fresh-04 | 1.963880 | 4,670 | -13.91 | same |
| fresh-05 | 1.050903 | 62 | -0.92 | same |
| fresh-06 | 0.792844 | -110 | -10.03 | same |
| fresh-07 | 0.850548 | -2,030 | +1.59 | g8f6 → b8c6 |
| fresh-08 | 0.489770 | -399 | -12.75 | f7e7 → f7e8 |

- Total qnodes **−73.02%**, median per-position ratio **0.821696** (17.83% median reduction), **5 better / 3 worse / 0 tied**. All fresh results complete depth 4, so these are matched-horizon counts.
- Largest improvement, both absolute and relative: **fresh-03, 265,656→18,549**, saving **247,107 (93.02%)**. It accounts for **101.65% of net suite saving** and 79.79% of control qnodes. Its control was already 7.09× untouched g134; the auxiliary repairs a particularly adverse ordinary continuation here.
- Largest absolute regression: **fresh-01, 32,624→44,695**, **+12,071 (+37.00%)**. Largest relative regression: **fresh-04, 4,845→9,515**, **+4,670 (+96.39%)**, SP **61.07%→47.15%**. Neither hits a cap. The third regression is small: fresh-05 **1,218→1,280 (+62)**.
- Remove fresh-03: aggregate **67,269→71,289 (+5.98%)**, median ratio **0.850548**, **4 improve / 3 regress**. Excluding the two largest absolute savings (fresh-03 and fresh-02) gives **53,583→67,847 (+26.62%)**. Thus the 73% headline is not broadly distributed. There are still multiple real individual gains, especially fresh-02 and fresh-08; the result is not absence of transfer.
- Aggregate SP rises **16.96 points**, but per-position SP improves only **4/8**. On the seven-position subset without fresh-03 it rises **52.86%→58.28%** while total qnodes worsen. SP rate alone does not establish a smaller tree.

| Independent SEARCH game | Positions | λ=0→2 qnodes | Change | Improved / regressed |
| --- | --- | --- | --- | --- |
| 2 | fresh-01, fresh-04, fresh-05 | 38,687 → 55,490 | +43.43% | 0 / 3 |
| 7 | fresh-02, fresh-06, fresh-08 | 14,999 → 4,246 | -71.69% | 3 / 0 |
| 21 | fresh-03, fresh-07 | 279,239 → 30,102 | -89.22% | 2 / 0 |

All three sampled positions from game 2 regress; all three from game 7 and both from game 21 improve. Effective independent-source evidence is **two favorable clusters versus one adverse cluster**, with only three clusters total. λ=2 is also 55.33% below untouched g134 in aggregate new-suite qnodes; that reference result does not remove the within-arm outlier dependence.

## Continuity fixtures and root choices

| Position | Model | Completed depth / status | Total nodes | Qnodes | Q% | SP attempts | SP cutoffs | SP% | Root score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| middlegame-kiwipete | g134 | 4 / OK | 218,790 | 209,297 | 95.66 | 180,919 | 80,896 | 44.71 | 10036 / e1g1 | 2.332 |
| middlegame-kiwipete | lambda0 | 3 / CAP | 1,000,000 | 992,510 | 99.25 | 827,396 | 347,800 | 42.04 | 4051 / e1g1 | 9.885 |
| middlegame-kiwipete | lambda2 | 4 / OK | 960,880 | 951,273 | 99.00 | 803,494 | 351,635 | 43.76 | 311 / d5e6 | 11.145 |
| quiet-fianchetto | g134 | 4 / OK | 18,327 | 13,007 | 70.97 | 16,509 | 8,896 | 53.89 | -18683 / f3e5 | 0.279 |
| quiet-fianchetto | lambda0 | 4 / OK | 101,776 | 92,037 | 90.43 | 95,610 | 28,072 | 29.36 | -10253 / d1d2 | 1.253 |
| quiet-fianchetto | lambda2 | 4 / OK | 24,487 | 18,404 | 75.16 | 21,813 | 11,377 | 52.16 | -7528 / f3e5 | 0.393 |
| opening-start | g134 | 4 / OK | 3,795 | 220 | 5.80 | 2,913 | 2,158 | 74.08 | -872 / b1c3 | 0.048 |
| opening-start | lambda0 | 4 / OK | 3,627 | 320 | 8.82 | 2,670 | 1,759 | 65.88 | -3916 / b1c3 | 0.042 |
| opening-start | lambda2 | 4 / OK | 2,691 | 206 | 7.66 | 1,997 | 1,346 | 67.40 | -3402 / b1c3 | 0.040 |
| en-passant | g134 | 4 / OK | 358 | 18 | 5.03 | 235 | 151 | 64.26 | 0 / e5d6 | 0.002 |
| en-passant | lambda0 | 4 / OK | 497 | 24 | 4.83 | 327 | 218 | 66.67 | 16 / e1e2 | 0.002 |
| en-passant | lambda2 | 4 / OK | 362 | 15 | 4.14 | 240 | 154 | 64.17 | 0 / e5d6 | 0.002 |

| Fixture | FEN |
| --- | --- |
| Kiwipete | `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1` |
| Fianchetto | `r1bq1rk1/ppp1bppp/2n2n2/3pp3/8/1P1P1NP1/PBP1PPBP/RN1Q1RK1 w - - 0 8` |
| Opening | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` |
| En-passant | `4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1` |

Fianchetto preserves the auxiliary's favorable direction versus control, **80.00% fewer qnodes** and SP **29.36%→52.16%**. Its λ=2 count is still 41.49% above g134. Kiwipete provides only qualified directional progress: λ=2 finishes where λ=0 caps, with SP **42.04%→43.76%**, but the control's required full depth-4 work is unknown. The previous **205,023→111,437** completed-depth historical improvement does **not** reproduce at a similar operational level. λ=2 Kiwipete uses 960,880 total nodes, close to the million-node cap. Opening improves **320→206**, En-passant **24→15**; both remain continuity checks rather than primary generalization evidence.

New-suite λ=0/2 moves agree on **5/8** roots. Changes are fresh-03 `d2d3→b5d7`, fresh-07 `g8f6→b8c6`, and fresh-08 `f7e7→f7e8`. The fresh-05 forced-loss score **−32764** and move `f7f6` match across all models; that is a search mate-band return, not static-output saturation. Continuity Fianchetto returns from control `d1d2` to original g134 `f3e5`; Opening retains `b1c3`; En-passant returns from control `e1e2`/16 to `e5d6`/0. Kiwipete's displayed control `e1g1`/4051 is depth 3 versus λ=2 `d5e6`/311 at depth 4, so it is not a matched-depth root-choice comparison. These are descriptive changes, not verified blunders or strength gains.

**Calibration was deliberately skipped.** The reusable class named `CalibrationSearch` ran only RAW with a=1,b=0 and no source mapping seam. The principal uncertainty is fresh-data transfer/control stability, and a secondary calibration check would not resolve source diversity or outlier dependence. No fitting or retuning occurred.

## Explicit answers and recommendation

| Question | Answer |
| --- | --- |
| 1. Genuinely fresh relative to g133? | Yes as a newly executed next-seed generation, not reused files. No as a claim that every position is unseen: 329/1,632 TRAIN observations match g133 replay base keys. Novel evaluation excludes all saved historical and prior diagnostic keys. |
| 2. Exact size/source? | 64 production handcrafted-search games, 8,903 pre-move positions, 2,048 samples; native 51-game TRAIN gives 1,632 updates/arm. |
| 3. Better fresh held-out capture deltas? | Yes median −38.40%, p90 −11.00%, Huber −29.03%; all three novel game medians improve. p95 and >0.5 frequency worsen. |
| 4. Ordinary 50:50 campaign objective preserved? | Exactly, including WDL orientation, pinned bounded NNUE, one scalar target, base half-squared loss and saved Adam conventions. Novel loss improves 13.19%, but novel decisive WDL behavior remains untested. |
| 5. New-suite aggregate qnodes reduced? | Yes, 332,925→89,838 (−73.02%), all completed depth 4. |
| 6. Improved versus regressed? | 5 versus 3 positions; two favorable source-game clusters versus one adverse. |
| 7. Aggregate stand-pat improved? | Yes, 41.27%→58.23%; only 4/8 per-position rates improve. |
| 8. Kiwipete/Fianchetto reproduce prior direction? | Fianchetto clearly versus control. Kiwipete only finishes versus a capped control; no comparable full-depth reduction or prior low workload is established. |
| 9. Severe new regression? | Control Kiwipete newly caps; control Fianchetto grows 7.08×. Auxiliary fresh-04 nearly doubles qnodes and one source group regresses throughout. No λ=2 fresh-suite cap/systemic collapse, but λ=2 Kiwipete remains severely elevated versus g134. |
| 10. Outliers or broad distribution? | Large aggregate saving is outlier-dominated: remove fresh-03 and totals worsen 5.98%. Median/per-position gains show some transfer, insufficient breadth for C/D. |
| 11. Integrate into real trainer now? | No. Preserve isolated evidence; B—MIXED GENERALIZATION does not justify active/default integration or a controlled production-like campaign yet. |
| 12. Next acceptance experiment if later integration is justified? | First pass the fresh replication below. Then a separately authorized opt-in trainer integration with exact λ=0 parity and a bounded paired playing-strength/retention acceptance test before any default. No such match ran here. |
| 13. What remains unproven? | Robust gain across independent novel source groups/checkpoints/seeds, decisive-game ordinary-loss retention, acceptable unmodified-control continuation, tail reliability, and playing strength. |

**Recommended next work unit:** one further preregistered isolated fresh generation-sized λ=0/2 replay under a different independently fixed data/order seed, still starting from authenticated g134 and preserving the exact 50:50 objective and one native update schedule. Before training, inspect only source provenance/legality/outcome and freeze a source-group-aware evaluation allocation that includes novel decisive and drawn games and more independent search-game clusters; keep the native amount of TRAIN material and report any resulting partition difference. If the same 64-game production-equivalent budget cannot supply adequate independent evaluation coverage, return that feasibility limitation rather than generate until results look favorable. Predefine control acceptability, report per-source and leave-largest-position-out qnode statistics, and require the continuity cap problem to be resolved or explicitly understood. Do not tune λ or train longer. This is a new authorization proposal, not work performed here.

Only after that evidence supports broad, repeatable transfer should an opt-in production-trainer implementation and a bounded playing-strength acceptance experiment be considered. This unit does not establish that the auxiliary should become the default, that a long campaign is safe, or that smaller qtrees imply stronger play.

## Validation, limitations and skipped work

**Passed runtime/focused checks:**

1. Canonical checksummed payload loads; exact g134 model/training-state identity; full-state restoration and all output inference/training codec round trips. Saved moment/config identity is the complete training-state SHA above, not a weights-only approximation.
2. Unmodified production generation once; all 64 actual terminal outcomes/sampling indexes/source seeds verified; no fabricated outcomes for incomplete games. Fresh generation/game seeds do not overlap historical indexed seeds.
3. Every frozen parent equals production 50:50 target arithmetic; teacher endpoint determinism, legal capture transition, occupancy/STM checks and shared-anchor cancellation. Teacher serialization and input hashes unchanged throughout.
4. Exact 1,632-update complete-state equality among pristine production λ=0, independent ordinary frozen-target loop and auxiliary zero branch; 128 additional exact update comparisons; 72 λ=2 finite-difference checks plus eight scalar checks. Auxiliary trainer bytes match the accepted fourth implementation.
5. Native source-game partition separation; novel held parent and pair endpoint exclusions; eight unique legal/FEN-roundtripped search roots disjoint from train/held/history/previous diagnostic endpoints; frozen manifest unchanged after all training/search. Selection/freeze precedes training and comparisons.
6. **39/39 existing focused tests passed**, zero failures/aborts/skips, Java 21 / JUnit Jupiter 5.10.3 / Platform 1.10.3; 14.790 seconds. Classes: `Brn2CoreTest`, `Brn2CodecTest`, `Brn2TrainingTargetsTest`, `TrajectorySamplerTest`, `BootstrapPartitionTest`, `Brn2SearchIntegrationTest`, `QsearchDecisionTraceTest`, `NnueScoreMappingTest`. No full regression suite.
7. 36 primary real searches with exact node/restoration/trace accounting; **26,838 detached BRN static sample checks** against fresh evaluation (4,263 g134; 12,966 λ=0; 9,609 λ=2). Four trace-free equality replays, including capped control Kiwipete. Checkpoints search with the pristine engine and original normal score mapping. Static source equality is separately distinguished from these runtime checks.
8. Full archived-file byte comparison finds only the declared trainer change; no qsearch, NNUE, ordering, pruning, extension/reduction, codec/schema or search-evaluation-source change. All **542 initial authoritative files** retain their captured hashes; pre-report porcelain status exactly matched baseline. All 134 historical sample files, pinned model/state/teacher and 18 frozen input artifacts retain their hashes. Final report-only diff/index and post-commit preservation checks are recorded in the completion response.

Minor external harness corrections before relevant execution: `Board` has no FEN formatter, so generation records six-long boards and the external formatter was added with exact round-trip assertions; a diagnostic print had one excess parenthesis; the initially overconstrained suite quota was amended using availability only before training. No product fix, regenerated batch, changed targets or result-driven selection followed these corrections. PowerShell rendered Java stderr progress as `NativeCommandError` and reported shell status 1 on the first search wrappers, while every JSON file contains its successful end record and all requested results; subsequent Python `subprocess.run(check=True)` trace-free searches exit zero and reproduce the relevant counters. This was host stderr presentation, not an omitted failed benchmark.

**Limitations:** one checkpoint, one new seed/order and one generation; 47.86% natural TRAIN overlap with saved historical train/held keys; only three novel held source games (all draws) and three new SEARCH source games; correlated positions and repeated material; one selected capture per row; static NNUE is a reference rather than tactical truth; novel tail-error exceptions; unstable ordinary continuation on continuity fixtures; one censored control search; single-JVM timing rather than warmed performance estimates; no proof of absence from unsaved/unsampled historical positions or every search descendant. The three source clusters do not justify treating eight positions or 56 edges as independent trials. No confidence interval from independent campaign replicas is available.

**Deliberately skipped:** calibration, full/long regression suite, GUI/browser checks (no UI work), second generation or campaign, validation/promotion arena, engine tournament, tactical-oracle or playing-strength screen, full live-store recovery/operational acceptance, release/package/deployment checks. No generated payload/corpus is committed. No production trainer integration, GUI configuration, default change, checkpoint schema change, store writer/reference update, push or deployment occurred.

The diagnostic work unit is complete; report/turn completion does not establish user acceptance, production acceptance or deployment.

**Human actions required after this prompt: None.** The recommended follow-up is a future work-unit decision, not a blocker to completing this diagnostic.

## Reproduction and disposable evidence

Use Java `21+35-2513`, Python 3.13.4. Recreate an archive of the inspected HEAD outside the primary repository. Copy the pinned payloads to `g134.brn2`, `g134.state`, `teacher.nnue`; never point an output at the store. Extract `CaptureDiagnostic` from report one; `CalibrationSearch` and `CalibrationScore` from report two (RAW only, no seam); `inspect_store.py`, `patch_auxiliary.py`, `CampaignCapture`, `CampaignLossChecks`, `ReplayControl`, `ReplayData` and `run_focused.py` from report four. The previous report supplies the exact common auxiliary implementation and codec-reader bridge. `ReplayControl.main` and `CampaignCapture.main` are not invoked on historical data in this unit; their helpers support focused checks.

Before updates, scan all 134 `bootstrap/*.data` frames using the accepted `inspect_store.frame` format: verify frame SHA; read plan hash/generation duration; read each train/held count followed by six board longs and WDL double; union unsigned board keys into `history-train-keys.txt` / `history-all-keys.txt`. Add parent/child keys from the accepted third/fourth diagnostic corpora to `previous-diagnostic-keys.txt`. Those corpora are exclusion inputs only. Historical `.data` framing/layout and direct reader are retained in report four. Their hashes and the exact resulting key-set hash are recorded above.

Compile external `FreshGenerate.java` and run it once; it writes the new raw corpus and game provenance. Compile `FreshData.java`/`CaptureDiagnostic.java` into `pristine-classes`; run `FreshData DIR prepare` to freeze targets, pairs, held novelty and the final suite. Hash/freeze the input manifest. Compile/run `FreshTrain DIR control` against the pristine source. Run `FreshData DIR g134` and `FreshData DIR lambda0` for held predictions.

Only then apply the unchanged `patch_auxiliary.py` to the isolated trainer and compile separate `aux-classes`, including `CampaignCapture`, `ReplayControl`, `ReplayData`, `CampaignLossChecks`, and the Fresh helpers. Restrict `CampaignLossChecks` finite-difference λ array to `{2}`. Run `CampaignLossChecks g134.state check-pairs.bin`, `FreshTrain DIR 0check`, and `FreshTrain DIR 2`; run pristine `FreshData DIR lambda2`. Run `analyze_held.py` before search. No historical replay training or λ=0.5 execution is part of these steps.

Compile `FreshSearch`, `CalibrationSearch`, `CalibrationScore` into the preserved pristine classes. Run `FreshSearch DIR g134`, then `FreshSearch DIR lambda0`, record the control audit, then `FreshSearch DIR lambda2`. Mode `replay` runs only fresh-01 and Kiwipete without the trace for λ=0/2 equality checks. Run `analyze_search.py`. Search Java heap 1,024 MB; generation/training 1,536 MB. `run_focused.py` uses `aux-classes` instead of the previous `cal-classes`; no calibration source patch is needed. No Gradle output is written into the authoritative repository.

The final suite rule in the source below is the pretraining availability amendment, not post-search tuning. Output writers use CREATE_NEW to prevent replacing an earlier corpus/checkpoint/result. Keep all generated evidence outside the repository. Raw temporary data are disposable; this report retains the exact new source, selection rules, identities and complete search tables for downstream reconciliation.

| Disposable evidence | Bytes | SHA-256 |
| --- | --- | --- |
| experiment-plan.json | 1899 | c09eb8a3e3464bb714363884db5cbc3e041fe338abf34d4c13abf056b53f205d |
| selection-amendment.json | 535 | 94772e3324721a5a6b12917bec7c000d3de75aef087d86eba7fb5829a17d7974 |
| frozen-manifest.json | 1787 | 01759fbaf9392f3a5540b8ed26122a957f66b1882574c458c299a18081650578 |
| generation-plan.jsonl | 509 | 606e431b9709ed8f755a28af27e4ade1a1275fbe4636dcc58c8d1a9d1d3ece3f |
| generation-summary.jsonl | 684 | 126f11f87aefcd4d95d33d0fc5812f2ad7c1ea1544c658e2387f319e5fa2e4cc |
| fresh-base.bin | 153572 | c560e77ada7f79a9c8a27b607bf7ad1ba83451f001070531e69525a74316c856 |
| fresh-rows.bin | 286892 | 5f805f70d4e0d701d607e8d9d50c52ca644ac4e7c0bfce79f4e112726d5e38d9 |
| search-suite.tsv | 538 | e308ca5f766ebf8ba58ef0f90e26a3b0a7056896071a729391c5cdcacc5b14dd |
| held-results.json | 37677 | 2bba6597e426fe164cff76c4a1cdea54c5aa566378f6e7e21c28b10f48b2b8e6 |
| control-audit.json | 4588 | d5f37d6db165657ba92a47d3c35cc670e17c1d3abe989418d04e02f48ef842cc |
| search-results.json | 69518 | feeabb6f9c192d8b7eeb59492fd15a74fb9195d28d86116a434450ca0fbc03d4 |
| seed-check.json | 246 | 63dce5e7d63fbba56b5ec100d20fc8d136a971fe1383ff536b2da8c993faca85 |
| verification.json | 1380 | 0451383e2ec93d006fbb6e81a995242289626b771ffa6d0ae631fed974446614 |
| focused-tests.txt | 579 | b7881cd9481ae85d74b372914bc50bfb0e27d21144c7039e3d14ed3ed980bcf1 |
| trace-replay-validation.txt | 125 | b05531d4743fc1cabff42c7e94783eebe731a51b4b0d3d0fbd7dc2c1688b13fe |
| g134-search-all.jsonl | 1400314 | eaf29f79aae7466577509bd7899f34da27fe0784387b9e932863071833712293 |
| lambda0-search-all.jsonl | 3835873 | 140f30fc58b196ff0024bdaeaa1144069782b0639e4db20386b6e4c1a9360706 |
| lambda2-search-all.jsonl | 2890498 | b9b6b4dddba2ddad18e710aad051dbe3cc54bbe072c160e6461c247bedd32cbc |

## Appendix — exact new isolated harness and analysis source

All paths below refer to the disposable directory. Reused methods/codecs/search remain as pinned above. The same fresh rows and immutable teacher feed every arm; synthetic children never supply ordinary labels.

### FreshGenerate.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class FreshGenerate {
 static final long GENERATION=135, MASTER=1, DATA=1;
 static long seed(long g,long master,long salt){return new SplittableRandom(master^salt^(g*0x9E3779B97F4A7C15L)).nextLong();}
 static final long GEN_SEED=seed(GENERATION,DATA,0x6A09E667F3BCC909L), SPLIT_SEED=seed(GENERATION,MASTER,0xA54FF53A5F1D36F1L), SHUFFLE_SEED=seed(GENERATION,MASTER,0xBB67AE8584CAA73BL);
 public static void main(String[] args)throws Exception{
  Path d=Path.of(args[0]);
  if(seed(134,DATA,0x6A09E667F3BCC909L)!=-2860970018538427249L||seed(134,MASTER,0xA54FF53A5F1D36F1L)!=-5751002697157378807L||seed(134,MASTER,0xBB67AE8584CAA73BL)!=8583368780869221086L)throw new AssertionError("Historical seed schedule");
  var config=new SelfPlayConfig(64,4,6,GEN_SEED,0,8,32,1024,NnueScoreMapping.V1,-1,-1);
  try(var plan=new PrintWriter(Files.newBufferedWriter(d.resolve("generation-plan.jsonl"),StandardOpenOption.CREATE_NEW))){write(plan,"generation",GENERATION,"generationSeed",GEN_SEED,"splitSeed",SPLIT_SEED,"shuffleSeed",SHUFFLE_SEED,"config",config.toString(),"source","HANDCRAFTED","start",Board.FEN_STARTING_POSITION,"utc",java.time.Instant.now().toString());}
  long start=System.nanoTime();
  try(var log=new PrintWriter(Files.newBufferedWriter(d.resolve("generation-progress.jsonl"),StandardOpenOption.CREATE_NEW))){
   var batch=SelfPlayBatch.generateHandcrafted(config,Board.startingPosition(),new SelfPlayControl(),p->{var g=p.lastGame();write(log,"game",g.gameIndex(),"seed",g.seed(),"termination",g.termination().toString(),"plies",g.playedPlies(),"rawPositions",g.rawPositions(),"samples",g.sampledPositions(),"failure",g.failure(),"utc",java.time.Instant.now().toString());log.flush();System.out.println("game="+g.gameIndex()+" "+g.termination()+" plies="+g.playedPlies());if(g.failure()!=null)throw new IllegalStateException(g.failure());});
   if(batch.cancelled()||batch.games().size()!=64)throw new AssertionError("Incomplete generation");
   var part=BootstrapPartition.split(batch,SPLIT_SEED);var searchIds=new HashSet<>(part.heldOutGames().subList(0,6));
   int offset=0;try(var out=new DataOutputStream(Files.newOutputStream(d.resolve("fresh-base.bin"),StandardOpenOption.CREATE_NEW));var json=new PrintWriter(Files.newBufferedWriter(d.resolve("fresh-base.jsonl"),StandardOpenOption.CREATE_NEW))){
    out.writeInt(batch.samples().size());for(var g:batch.games()){var ix=TrajectorySampler.indexes(g.rawPositions(),32);String split=part.trainingGames().contains(g.gameIndex())?"TRAIN":searchIds.contains(g.gameIndex())?"SEARCH":"HELD";
     for(int j=0;j<g.sampledPositions();j++){var s=batch.samples().get(offset++);out.writeUTF(split);out.writeInt(g.gameIndex());out.writeInt(j);out.writeInt(ix[j]);for(long b:s.board())out.writeLong(b);out.writeDouble(s.target());write(json,"split",split,"game",g.gameIndex(),"gameSeed",g.seed(),"sample",j,"ply",ix[j],"key",Long.toUnsignedString(s.board()[5],16),"board",Arrays.stream(s.board()).boxed().toList(),"wdl",s.target());}
    }
   }
   try(var summary=new PrintWriter(Files.newBufferedWriter(d.resolve("generation-summary.jsonl"),StandardOpenOption.CREATE_NEW))){write(summary,"statistics",batch.statistics().toString(),"trainingGames",part.trainingGames(),"reservedGames",part.heldOutGames(),"searchGames",searchIds.stream().sorted().toList(),"heldGames",part.heldOutGames().stream().filter(i->!searchIds.contains(i)).toList(),"trainingSamples",part.training().size(),"totalSamples",batch.samples().size(),"elapsedNs",System.nanoTime()-start,"utc",java.time.Instant.now().toString());}
   System.out.println("FROZEN "+batch.statistics()+" train="+part.training().size());
  }
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
  byte[] tb=Files.readAllBytes(d.resolve("teacher.nnue"));var net=NnueNetworkCodec.decode(tb);var nn=new NnueEvaluator(net);var rng=new SplittableRandom(2026092405L);var rows=new ArrayList<Row>();int terminalExclusions=0;
  try(var in=new DataInputStream(Files.newInputStream(d.resolve("fresh-base.bin")))){int n=in.readInt();for(int i=0;i<n;i++){
   String split=in.readUTF();int g=in.readInt(),j=in.readInt(),ply=in.readInt();long[] p=new long[6];for(int k=0;k<6;k++)p[k]=in.readLong();double w=in.readDouble(),tp=teacher(nn,p),t=BrnSupervision.blended(.5).targets(nn).applyAsDouble(new TrajectorySampler.Sample(p,w));require(t==.5*w+.5*tp,"Campaign target");fen(p);
   var moves=new ArrayList<Long>();int caps=0;if(new HeadlessGame(p,2).active())for(long m:CaptureDiagnostic.legal(p))if(CaptureDiagnostic.capture(p,m)){caps++;long[] c=CaptureDiagnostic.play(p,m);if(new HeadlessGame(c,2).active())moves.add(m);else terminalExclusions++;}
   long move=0;long[] c=null;double tc=0;if(!moves.isEmpty()){move=moves.get(rng.nextInt(moves.size()));c=CaptureDiagnostic.play(p,move);var game=new HeadlessGame(p,2);game.play(move);require(Arrays.equals(c,game.boardSnapshot()),"Legal transition");require(Long.bitCount(p[0]|p[1]|p[2])==1+Long.bitCount(c[0]|c[1]|c[2]),"Capture occupancy");require(Board.player((int)p[4])!=Board.player((int)c[4]),"STM flip");tc=teacher(nn,c);require(tc==teacher(nn,c),"Teacher determinism");require(Math.abs(-(-.5*w+.5*tc)-t-.5*(-tc-tp))<3e-16,"Shared WDL cancellation");fen(c);}
   rows.add(new Row(split,g,j,ply,p,w,tp,t,move,c,tc,false,false,caps));
  }require(in.read()==-1,"Trailing raw rows");}
  var history=keys(d.resolve("history-all-keys.txt"));history.addAll(keys(d.resolve("previous-diagnostic-keys.txt")));var trainKeys=endpoints(rows.stream().filter(r->r.split.equals("TRAIN")).toList());var blocked=new HashSet<>(history);blocked.addAll(trainKeys);var marked=new ArrayList<Row>();
  for(var r:rows){boolean novel=!blocked.contains(r.p[5]);boolean np=novel&&r.pair()&&!blocked.contains(r.c[5]);marked.add(new Row(r.split,r.game,r.index,r.ply,r.p,r.w,r.tp,r.t,r.move,r.c,r.tc,novel,np,r.captures));}rows=marked;
  var heldKeys=endpoints(rows.stream().filter(r->r.split.equals("HELD")).toList());blocked.addAll(heldKeys);
  var search=rows.stream().filter(r->r.split.equals("SEARCH")&&!blocked.contains(r.p[5])&&new HeadlessGame(r.p,2).active()).sorted((a,b)->Long.compareUnsigned(a.p[5],b.p[5])).toList();
  System.out.println("Eligible unseen search roots="+search.size()+" perGame="+search.stream().collect(java.util.stream.Collectors.groupingBy(Row::game,java.util.stream.Collectors.counting()))+" captures="+search.stream().collect(java.util.stream.Collectors.groupingBy(Row::captures,java.util.stream.Collectors.counting())));System.out.println("Novel held="+rows.stream().filter(r->r.split.equals("HELD")&&r.novel).count()+" pairs="+rows.stream().filter(r->r.split.equals("HELD")&&r.novelPair).count());
  var ids=search.stream().map(Row::game).distinct().sorted().toList();var selected=new ArrayList<Row>();var counts=new HashMap<Integer,Integer>();var selectedKeys=new HashSet<Long>();
  for(boolean rich:new boolean[]{true,false}){int count=0;for(int round=0;round<3&&count<4;round++)for(int game:ids){if(count==4)break;if(counts.getOrDefault(game,0)>=3)continue;var found=search.stream().filter(r->r.game==game&&!selectedKeys.contains(r.p[5])&&(rich?r.captures>=3:r.captures<=1)).findFirst();if(found.isPresent()){var r=found.get();selected.add(r);selectedKeys.add(r.p[5]);counts.merge(game,1,Integer::sum);count++;}}require(count>=4,"Insufficient rich/quiet novel search positions: "+count);}
  require(selected.size()>=8&&selected.size()<=12,"Suite size");require(rows.stream().filter(r->r.split.equals("HELD")&&r.novel).count()>=64,"Insufficient novel holdout");require(rows.stream().filter(r->r.split.equals("HELD")&&r.novelPair).count()>=32,"Insufficient novel held pairs");
  try(var o=new DataOutputStream(Files.newOutputStream(d.resolve("fresh-rows.bin"),StandardOpenOption.CREATE_NEW));var json=new PrintWriter(Files.newBufferedWriter(d.resolve("fresh-rows.jsonl"),StandardOpenOption.CREATE_NEW))){o.writeInt(rows.size());for(var r:rows){writeRow(o,r);write(json,"split",r.split,"game",r.game,"sample",r.index,"ply",r.ply,"key",Long.toUnsignedString(r.p[5],16),"fen",fen(r.p),"wdl",r.w,"tp",r.tp,"target",r.t,"move",r.pair()?Move.coordinate(r.move):null,"childKey",r.pair()?Long.toUnsignedString(r.c[5],16):null,"tc",r.pair()?r.tc:null,"delta",r.pair()?r.delta():null,"novel",r.novel,"novelPair",r.novelPair,"captures",r.captures);}}
  try(var suite=new PrintWriter(Files.newBufferedWriter(d.resolve("search-suite.tsv"),StandardOpenOption.CREATE_NEW));var json=new PrintWriter(Files.newBufferedWriter(d.resolve("search-suite.jsonl"),StandardOpenOption.CREATE_NEW))){for(int i=0;i<selected.size();i++){var r=selected.get(i);String name=String.format("fresh-%02d",i+1);suite.println(name+"\t"+fen(r.p));write(json,"id",name,"game",r.game,"sample",r.index,"ply",r.ply,"key",Long.toUnsignedString(r.p[5],16),"captures",r.captures,"fen",fen(r.p),"utc",java.time.Instant.now().toString());}}
  // Prior checker's pair format, containing only TRAIN and strictly novel HELD pairs.
  var checkRows=rows.stream().filter(r->r.split.equals("TRAIN")||r.split.equals("HELD")&&r.novelPair).toList();try(var o=new DataOutputStream(Files.newOutputStream(d.resolve("check-pairs.bin"),StandardOpenOption.CREATE_NEW))){o.writeInt(checkRows.size());for(var r:checkRows){o.writeBoolean(!r.split.equals("TRAIN"));o.writeInt(r.index);for(long b:r.p)o.writeLong(b);o.writeDouble(r.w);o.writeDouble(r.tp);o.writeDouble(r.t);o.writeLong(r.move);o.writeBoolean(r.pair());if(r.pair()){for(long b:r.c)o.writeLong(b);o.writeDouble(r.tc);}}}
  require(Arrays.equals(tb,NnueNetworkCodec.encode(net)),"Teacher mutation");System.out.println("FROZEN rows="+rows.size()+" train="+rows.stream().filter(r->r.split.equals("TRAIN")).count()+" heldNovel="+rows.stream().filter(r->r.split.equals("HELD")&&r.novel).count()+" heldNovelPairs="+rows.stream().filter(r->r.split.equals("HELD")&&r.novelPair).count()+" suite="+selected.size()+" games="+counts+" excludedTerminalChildren="+terminalExclusions);
 }
 static void predictions(Path d,String name)throws Exception{var rows=read(d.resolve("fresh-rows.bin"));var model=Brn2Codec.decodeModel(Files.readAllBytes(d.resolve(name+".brn2")));var ws=new Brn2Workspace();try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve(name+"-predictions.jsonl"),StandardOpenOption.CREATE_NEW))){for(var r:rows)if(!r.split.equals("SEARCH"))write(out,"split",r.split,"game",r.game,"sample",r.index,"novel",r.novel,"novelPair",r.novelPair,"wdl",r.w,"tp",r.tp,"target",r.t,"yp",model.evaluate(r.p,ws),"yc",r.pair()?model.evaluate(r.c,ws):null,"delta",r.pair()?r.delta():null);}}
 public static void main(String[] a)throws Exception{if(a[1].equals("prepare"))prepare(Path.of(a[0]));else predictions(Path.of(a[0]),a[1]);}
}
```

### FreshTrain.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.brn2.*;import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;import com.ohinteractive.seedv6.training.service.BrnSupervision;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class FreshTrain {
 static void require(boolean b,String s){FreshData.require(b,s);}
 public static void main(String[] a)throws Exception{
  Path d=Path.of(a[0]);String mode=a[1];require(Set.of("control","0check","2").contains(mode),"Only lambda0,2 permitted");
  byte[] state=Files.readAllBytes(d.resolve("g134.state")),model=Files.readAllBytes(d.resolve("g134.brn2")),tb=Files.readAllBytes(d.resolve("teacher.nnue"));var net=NnueNetworkCodec.decode(tb);var nn=new NnueEvaluator(net);var trainer=Brn2Codec.decodeTraining(state);
  require(Arrays.equals(state,Brn2Codec.encodeTraining(trainer)),"Exact state restoration");require(Arrays.equals(model,Brn2Codec.encodeModel(trainer.snapshot())),"Initial model/state equality");require(trainer.optimizer().step()==218158,"Starting step");
  var rows=FreshData.read(d.resolve("fresh-rows.bin")).stream().filter(r->r.split().equals("TRAIN")).toList();var samples=rows.stream().map(FreshData.Row::sample).toList();
  for(var r:rows){require(r.tp()==FreshData.teacher(nn,r.p()),"Immutable parent teacher");require(r.t()==BrnSupervision.blended(.5).targets(nn).applyAsDouble(r.sample()),"Production target equality");if(r.pair())require(r.tc()==FreshData.teacher(nn,r.c())&&r.delta()==.5*(-r.tc()-r.tp()),"Aux target");}
  var order=new ArrayList<>(rows);var rng=new SplittableRandom(FreshGenerate.SHUFFLE_SEED);for(int i=order.size()-1;i>0;i--)Collections.swap(order,i,rng.nextInt(i+1));
  long start=System.nanoTime();double base=0,aux=0;
  if(mode.equals("control")){
   var stats=Brn2SelfPlayTraining.trainSamples(trainer,samples,new SelfPlayTraining.Config(1,1,true,FreshGenerate.SHUFFLE_SEED),new SelfPlayControl(),p->{},BrnSupervision.blended(.5).targets(nn)).orElseThrow();
   var manual=Brn2Codec.decodeTraining(state);for(var r:order)base+=manual.train(r.p(),r.t());require(Arrays.equals(Brn2Codec.encodeTraining(trainer),Brn2Codec.encodeTraining(manual)),"Production/manual final state equality");System.out.println(stats);
  }else{
   double lambda=mode.equals("2")?2:0;var method=Brn2Trainer.class.getMethod("trainCampaignCapture",long[].class,double.class,long[].class,double.class,double.class,double.class);
   for(var r:order){if(r.pair()){double[] losses=(double[])method.invoke(trainer,r.p(),r.t(),r.c(),r.delta(),lambda,.25);base+=losses[0];aux+=losses[1];}else base+=trainer.train(r.p(),r.t());}
  }
  long elapsed=System.nanoTime()-start;byte[] saved=Brn2Codec.encodeTraining(trainer),mb=Brn2Codec.encodeModel(trainer.snapshot());require(trainer.optimizer().step()==218158+rows.size(),"Exact schedule");require(Arrays.equals(saved,Brn2Codec.encodeTraining(Brn2Codec.decodeTraining(saved))),"Training codec roundtrip");require(Arrays.equals(mb,Brn2Codec.encodeModel(Brn2Codec.decodeModel(mb))),"Model codec roundtrip");
  if(mode.equals("0check")){require(Arrays.equals(saved,Files.readAllBytes(d.resolve("lambda0.state"))),"Lambda0 auxiliary branch must equal pristine production bytes");require(aux==0,"No lambda0 aux work");}
  else{String name=mode.equals("control")?"lambda0":"lambda2";Files.write(d.resolve(name+".state"),saved,StandardOpenOption.CREATE_NEW);Files.write(d.resolve(name+".brn2"),mb,StandardOpenOption.CREATE_NEW);}
  require(Arrays.equals(tb,NnueNetworkCodec.encode(net)),"Teacher unchanged");try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve(mode+"-training.jsonl"),StandardOpenOption.CREATE_NEW))){write(out,"mode",mode,"initialStateSha",CaptureDiagnostic.sha(d.resolve("g134.state")),"initialModelSha",CaptureDiagnostic.sha(d.resolve("g134.brn2")),"frozenRowsSha",CaptureDiagnostic.sha(d.resolve("fresh-rows.bin")),"shuffleSeed",FreshGenerate.SHUFFLE_SEED,"updates",rows.size(),"pairs",rows.stream().filter(FreshData.Row::pair).count(),"finalStep",trainer.optimizer().step(),"optimizer",trainer.config().toString(),"onlineBase",base/rows.size(),"onlineAuxPerBase",aux/rows.size(),"elapsedNs",elapsed,"utc",java.time.Instant.now().toString());}
  System.out.println("PASS "+mode+" exact initial weights/moments, immutable teacher, production targets, "+rows.size()+" updates; full codec validation");
 }
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
   else if(mode.equals("replay"))for(var p:List.of(suite.getFirst(),CaptureDiagnostic.POSITIONS[0])){CalibrationSearch.search(p,true,false,4);out.flush();}
   else throw new IllegalArgumentException(mode);
   write(out,"type","end","checks",CalibrationSearch.checks,"modelHashAfter",CaptureDiagnostic.sha(bp),"teacherHashAfter",CaptureDiagnostic.sha(np),"utc",java.time.Instant.now().toString());
  }
 }
}
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
 held=[r for r in rows if r['split']=='HELD'];novel=[r for r in held if r['novel']];pp=[r for r in held if r['novelPair']]
 allresults[model]={'train':metrics([r for r in rows if r['split']=='TRAIN']),'heldNative':metrics(held),'heldNovel':metrics(novel),'pairsNative':pairs([r for r in held if r['yc'] is not None]),'pairsNovel':pairs(pp),'perGame':{str(g):{'base':metrics([r for r in novel if r['game']==g]),'capture':pairs([r for r in pp if r['game']==g])} for g in sorted({r['game'] for r in novel})}}
(D/'held-results.json').write_text(json.dumps({'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'results':allresults},indent=2))
for m,r in allresults.items():print(m,json.dumps({'nativeBase':r['heldNative']['baseLoss'],'novel':r['heldNovel'],'pairs':r['pairsNovel']}))
```

### analyze_search.py

```python
from pathlib import Path
import json,statistics
D=Path(__file__).parent
models={}
for m in ['g134','lambda0','lambda2']:
 rows=[json.loads(l) for l in (D/f'{m}-search-all.jsonl').read_text().splitlines()];assert rows[-1]['type']=='end'
 rr={r['position']:r for r in rows if r['type']=='search'}
 for t in (r for r in rows if r['type']=='trace'):
  s=rr[t['position']];a=t['metrics']['all'];assert t['metrics']['observedCountedQChildren']==s['q'];assert a['driverStandPatBetaCutoffs']==s['standPatCutoffs'];assert a['standPatAllowed']==a['both']+a['driverOnly']+a['shadowOnly']+a['neither'];assert s['nodes']==s['main']+s['q'];s['attempts']=a['standPatAllowed'];s['spPct']=100*s['standPatCutoffs']/s['attempts'];s['qPct']=100*s['q']/s['nodes']
 models[m]=rr
suite=[json.loads(l) for l in (D/'search-suite.jsonl').read_text().splitlines()];names=[r['id'] for r in suite]
def aggregate(names):
 out={}
 for m,rows in models.items():
  rr=[rows[n] for n in names];q=sum(r['q'] for r in rr);attempts=sum(r['attempts'] for r in rr);cuts=sum(r['standPatCutoffs'] for r in rr)
  out[m]={'q':q,'nodes':sum(r['nodes'] for r in rr),'spAttempts':attempts,'spCuts':cuts,'spPct':100*cuts/attempts,'capped':sum(r['status']!='COMPLETED' for r in rr)}
 ratios=[models['lambda2'][n]['q']/models['lambda0'][n]['q'] for n in names]
 out['comparison']={'totalRatio':out['lambda2']['q']/out['lambda0']['q'],'medianRatio':statistics.median(ratios),'improved':sum(x<1 for x in ratios),'regressed':sum(x>1 for x in ratios),'unchanged':sum(x==1 for x in ratios),'standPatImproved':sum(models['lambda2'][n]['spPct']>models['lambda0'][n]['spPct'] for n in names)}
 return out
agg=aggregate(names);largest=max(names,key=lambda n:models['lambda0'][n]['q']-models['lambda2'][n]['q']);groups={str(g):aggregate([r['id'] for r in suite if r['game']==g]) for g in sorted({r['game'] for r in suite})}
result={'models':models,'aggregate':agg,'withoutLargestImprovement':{'removed':largest,'results':aggregate([n for n in names if n!=largest])},'groups':groups}
(D/'search-results.json').write_text(json.dumps(result,indent=2))
print(json.dumps({k:v for k,v in result.items() if k!='models'},indent=2))
for n in names+['middlegame-kiwipete','quiet-fianchetto','opening-start','en-passant']:
 a=models['lambda0'][n];b=models['lambda2'][n];print(n,'q',a['q'],b['q'],'ratio',b['q']/a['q'],'SP',a['spPct'],b['spPct'],'score/move',a['score'],a['move'],b['score'],b['move'],'status',a['status'],b['status'])
```
