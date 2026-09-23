# BRN-2 normal 75% training: deterministic reproduction and g130 diagnostics

Measured 2026-09-23 UTC in the authoritative `C:\projects\seed\java\seedv6`,
starting at `c66dd20f89272069c141e4e3c4aee71c24f7d5b5`.
This follows the accepted [training integration](BRN_SUPERVISION_TRAINING.md),
[weight sweep](BRN_SUPERVISION_WEIGHT_SWEEP.md),
[ablation](BRN_SUPERVISION_ABLATION.md),
[canonical diagnostics](BRN_CANONICAL_G128_DIAGNOSTICS.md), and
[strength screen](BRN_SUPERVISION_STRENGTH_SCREEN.md).

**Normal blended training reproduced the accepted replay exactly through g128,
but this was not an independent-data campaign.** Every saved training and held-out
sample, partition game ID and game statistic in g1–g128 is identical to training001.
All 128 Candidate model and optimizer hashes, training losses, held-out objective
losses, decisions and Best progression match the accepted 75% replay. The new store
was freshly initialized and generated its data through the normal application;
fresh generation did not make those deterministic data independent.

The final published Best is g130; latest-training is g131. The lineage contains
**56 promotions and 75 retentions**, not 53 lifetime promotions. Generations 4–131
contain exactly **128 completions and 53 promotions**, following a 190.733-second
gap after g3. This strongly fits a resumed service run with limit 128. The mechanism
is verified and reproduced below; a historical Start/Stop event was not persisted,
so the precise user action is not asserted as fact.

The independent-generalization conclusion is **blocked by demonstrated data
overlap**. The requested safe evaluator/search comparisons were completed, with
their meaning restricted accordingly. No protected store was mutated, training
was not resumed, and production behavior was not changed.

## Exact campaign state and pinned models

Student store: `E:\SeedV6-Networks\BRN\BRN-2\training002`.
Architecture `seedv6.brn.2`, feature schema **2**, hidden width **32**. All 132
manifests, g0–g131, form the complete parent chain. There are 131 checksummed
schema-3 history rows, 131 validation records and 57 accepted publication records
(bootstrap plus 56 promotions). The authoritative `refs/best` acceptance chain,
validation evidence and final history agree on g130.

Exact checkpoint directories under that student's `checkpoints/`:

```text
g0:
g000000-s000000000-31a42b7e131558c59e7868e45b759f1e5a322eb2bef5ff26e7b9386e4f831507
network.brn2 SHA-256: 195d4300ce1b90a30cb888d6990a872f33165bfadd66cf1f0212ba3f0c2f653f
training.state SHA-256: dec6eccdf4d9e59d4ea9f9843831548a045e07d050032f3c4ab62767d8c595bc
optimizer step: 0

Published Best g130:
g000130-s000211342-a5b111b2dd3c08f66e63a1ddd2a86a3fb3087c2e6fb74622f370608dd57c0d88
network.brn2 SHA-256: 9d08b6ab1fd9f9c0f72cada60463b301f95fb506f83127385593c0d7b2410453
training.state SHA-256: 4a4fda22f38ccfe0d2378273bde4afda0665e197c8e8d7cada6b36cb20b075c2
optimizer step: 211342
refs/best evidence: p-1db5ff48eb5980ab8df3c858321abce9e1f8681d9ed58e0b3d02efa64012f6b4

Latest-training / final settled g131:
g000131-s000212942-5a420680dea0c86119e5f8a7b4a02417ad5e1dc38a5be094c56b18764883efc3
network.brn2 SHA-256: 69c33609c0d44e49beca1701ace0b52bd30cabe8c3bbcd88581020c6753167e9
training.state SHA-256: 1f400edd66300ed71e4daf66a43e87342861cac742623929042524ac92689a49
optimizer step: 212942
```

G131 was retained. Its `generation-attempt.bin` remains as the last settled attempt,
with parent and incumbent both g130, format `CURRENT`, and empty restart notice.
**No pending generation or g132 attempt/plan exists.** All 131 plans have data and
settled decisions. `staging/` is empty; no restarted-generation archive or recovery
transaction is present. This does not prove that no transient event ever occurred.
104 model/optimizer pairs survive retention (g0, g10, g20, g30, g32–g131); the other
28 generations retain their authenticated historical metadata/pruning evidence.
All 208 extant fresh model/optimizer files match manifest lengths and SHA-256.

Each BRN model is 13,165,364 bytes and training state 39,496,044 bytes. Adam remains
learning rate .001, beta1 .9, beta2 .999, epsilon 1e-8; one shuffled online pass,
minibatch 1. G0 is the same deterministic initialization as the accepted campaign,
not a new random model seed. BRN-2 initialization has its own fixed architecture seed.

Comparison identities, checked against accepted reports, manifests or experimental
metadata and actual payload hashes:

```text
Replay 75% published experimental Best g125, step 203209:
E:\SeedV6-Networks\BRN\BRN-2\experimental-supervision-ablation-weight-sweep-20260923\weight0p75\boundary-128\network.brn2
model SHA-256: 6124771b5f83e86964aad8fa6f441dde378aca58b0aabe8541bbd9602b456e37
training.state SHA-256: 557863160233cfbeec56123dc8a5b14382b7830965039667f6c8055122a1fb74

Canonical WDL published Best g121, step 196690:
E:\SeedV6-Networks\BRN\BRN-2\training001\checkpoints\g000121-s000196690-5cf3400fde8d0c61c024b0dac47a8db54e425f7899f62bd2f657f77cc5b6b315
network.brn2 SHA-256: cc2072fe57fcfbc8ca8f83111586e3bfff4fe0abb554998e8ca8b859d1786c47
training.state SHA-256: 5b29899b380937769ebf2f1a90153916797311c3bc95dd706fdf3a3280e50d4c

Pinned NNUE g74, step 8942:
E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6
network.nnue SHA-256: 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training.state SHA-256: 453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
```

The experimental export has `experiment.json`/`best.txt`, not a production manifest
or `refs/best`; boundary 128 contains Best g125. Its two payload hashes also equal
training002's g125 exactly. No latest-training payload was substituted for any Best.

## Supervision, teacher history and independence

`brn-supervision.bin`, **every g1–g131 plan**, every validation and every history
row agree on `NNUE_BLENDED`, teacher weight **.75**, WDL contribution **.25**.
Every generation pins the exact NNUE g74 path and model hash above, from
`E:\SeedV6-Networks\NNUE\training`. There are **no teacher changes**. The teacher
also generated all these games. All history regimes are depth 2, six threads,
64 requested games, zero game-pair validation; plan settings retain opening plies
0–8, maximum 32 samples/game, maximum 1,024 plies and the standard starting FEN.

The read-only production readers verify plan/data hashes, history/validation
component evidence, Candidate ancestry, cumulative optimizer steps, strict-loss
decisions, incumbent chain and accepted Best progression. Thus the durable objective
did not silently change across the observed g3/g4 boundary or elsewhere. First-128
exact replay equivalence supplies stronger evidence than configuration labels alone.

For every corresponding g1–g128, comparison of the decoded and serialized samples
establishes exact equality of all six board longs, WDL double targets, sample order,
training/held-out membership, game IDs and complete game statistics. Only envelope
information such as plan binding and measured generation time differs. There are
208,104 identical training samples and 52,974 identical held-out samples in that
prefix. All 128 Candidate model and optimizer hashes match the accepted replay's
full-precision `replay.jsonl`; all own-objective losses and decisions match too.
For pruned early payloads this comparison uses the authenticated historical
manifest hashes; surviving payload hashes were independently checked against the
actual files. It is not a claim to have reloaded deleted optimizer payloads.

This is explained by existing deterministic behavior, not an application defect:
`TrainerConfig.seed()` derives generation/domain streams from master seed and
generation, without time, store path or process identity. `generateBootstrap()`
uses the pinned NNUE actor, not the BRN student, to generate games. The current
preferences contain run seed **1**, and all persisted settings/seeds match the
canonical prefix. A fresh folder with the same generation seeds, NNUE actor and
settings regenerates the same examples. The prior fresh-campaign instructions
preserved seed 1; they did not establish independent data. This finding supersedes
the independent-data premise of that proposed experiment, while preserving its
valid normal-workflow reproducibility result.

## Why 131 rather than 128?

Verified durable timing (UTC):

| Boundary | Timestamp / observation |
|---|---|
| g1 started | 2026-09-23T03:05:49.417297Z |
| g3 settled | 2026-09-23T03:06:34.883364800Z |
| g4 started | 2026-09-23T03:09:45.616327500Z |
| g3→g4 gap | 190.732963 seconds |
| g131 settled | 2026-09-23T03:41:32.041915400Z |
| All other adjacent gaps | At most .120933 seconds |
| g1–g3 | Three generations, three promotions |
| g4–g131 | 128 generations, 53 promotions, 75 retentions |

The read-only Windows Java Preferences snapshot at
`HKCU\Software\JavaSoft\Prefs\com\ohinteractive\seedv6\gui\nnue-training`
contains `maximumGenerations=128`, the training002 selection, blended .75, source
NNUE training, depth 2, threads 6, games 64 and seed 1. Preferences are current
state, not a timestamped log of values at each Start.

`TrainerConfig.maximumGenerations` is explicitly a **per-service-run count of new
completed generations**, excluding startup reconciliation. `TrainerService.execute()`
initializes instance field `completed` at zero, restores durable generation identity,
and loops while `completed < maximumGenerations`. It increments once after Candidate
publication, validation, settlement and history. A resumed instance does not subtract
the lineage's existing generation count. The six search threads do not independently
schedule generations; the service has one sequential worker. No asynchronous GUI stop
timer implements this limit. `TrainingController` rejects concurrent active Starts
and creates a new single-use service on a later Resume. Dashboard promotions are
explicitly labeled **"this run"**.

The new focused test
`Brn2BlendedBootstrapTest.generationLimitCountsNewCompletionsPerServiceRunNotLineageTotal`
uses real generation/search/Adam/publication in a temporary BRN-2 .75 store and
temporary NNUE fixture: a limit-3 fresh run stops at g3 with three new completions;
a limit-2 Resume stops at g5 with two new completions, five total history rows,
zero recovered lifecycles, and no g6 attempt. It reproduces the additive boundary
without a long campaign. Existing startup-reconciliation code can settle a pending
Candidate before counting newly trained generations; that is a separate boundary,
not evidence of three parallel extra generations here.

**Strongest supported explanation:** three generations preceded a later 128-new-
generation service run. The g3/g4 gap and exact 53-promotion suffix independently fit
this explanation. Three prior generations were present if that later service started
at this boundary; the store still has an uninterrupted, genuine g0-origin lineage.
**Not proven from durable records:** the exact Start/Stop/Resume action, whether a
process restarted, the first session's entered limit, or the reason for the gap.
Session IDs, start/stop events, per-session limits and stop reasons are not persisted.
A gap alone cannot prove a restart rather than a pause or stall. Current preferences
cannot reconstruct historical edits. No runtime event is invented to close that gap.

A single uninterrupted fresh service run with maximum 128 cannot reach g131 under
the inspected counting loop. **No generation-limit implementation defect is
demonstrated**, and no off-by-three fix is justified. If a future product decision
wants an absolute lineage endpoint instead, it must explicitly define that endpoint
and startup-recovery counting and test fresh, resumed and pending-Candidate boundaries;
that is a separate behavior change. Persisted per-run start/limit/stop evidence would
remove the historical ambiguity. Neither change was made here.

## Training trajectory, games and timing

The block means below are arithmetic means of generation measurements, each using
that generation's own held-out batch. Train is the Candidate's final training-partition
configured loss. Candidate/incumbent columns are pre-decision held-out losses.
Configured loss is half-squared error against **the blended target**, not a weighted
average of WDL and teacher losses. Lower configured Candidate loss alone promotes.

| Generations | Promotions / retentions | Resulting Best | Train | Candidate held-out | Incumbent held-out | Mean lifecycle s |
|---|---|---|---|---|---|---|
| g1–32 | 15 / 17 | 27 | 0.013269948 | 0.0345503541 | 0.0410545547 | 13.3299556 |
| g33–64 | 16 / 16 | 61 | 0.0106000175 | 0.023949969 | 0.0237138597 | 15.4251301 |
| g65–96 | 15 / 17 | 95 | 0.0105784544 | 0.021878043 | 0.0219884591 | 15.8344687 |
| g97–128 | 9 / 23 | 125 | 0.0102674688 | 0.02188213 | 0.0208868149 | 14.9286805 |
| g129–131 | 1 / 2 | 130 | 0.0094030761 | 0.0214813591 | 0.021116562 | 15.4499811 |

Durable component losses on those same batches:

| Generations | Candidate WDL | Incumbent WDL | Candidate teacher | Incumbent teacher |
|---|---|---|---|---|
| g1–32 | 0.202988328 | 0.211810199 | 0.0354262389 | 0.0411578827 |
| g33–64 | 0.19566995 | 0.193967303 | 0.0254554462 | 0.0257081828 |
| g65–96 | 0.185669903 | 0.186248188 | 0.025015098 | 0.0249695578 |
| g97–128 | 0.183861862 | 0.180457474 | 0.0223557743 | 0.0221634836 |
| g129–131 | 0.173128524 | 0.175171242 | 0.0216741132 | 0.0205068111 |

This is a changing-batch history, not a monotone fixed-validation curve. The prefix
matches the accepted replay, including its strong early reduction and late flattening.
At g125 there had been 55 promotions; g126–129 retain it, g130 promotes, g131 retains
g130. G130's own batch favors it: configured loss **.025182655516 versus .027992937810**
(10.04% lower), WDL **.226568078411 versus .240002157864**, but teacher loss is worse:
**.021862300319 versus .021131316894**. G131 then favors g130 over its Candidate,
**.017172104208 versus .020648906614**.

These demonstrate continued parameter change and a local selection improvement.
They do not demonstrate sustained useful generalization: common A/B blended loss
slightly worsens from g125 to g130, and fixed-corpus stability changes unevenly.
The evidence is consistent with a plateau with noisy late promotions and objective
trade-offs; it does not prove that useful learning has ceased.

| Partition | Samples | W (+1) | D (0) | L (-1) |
|---|---|---|---|---|
| training | 212942 | 74263 | 70865 | 67814 |
| heldOut | 54222 | 18879 | 18204 | 17139 |

W/D/L sample composition is in each sampled board's side-to-move perspective.
Whole completed-game outcomes are White wins **2,985**, draws **2,785**, Black wins
**2,604**. Of **8,384 requested games**, **8,374 completed** and **10 were aborted,
all accounted for by the persisted capped-game count**. One cap occurs at each of
g7, g29, g39, g46, g50, g57, g83, g93, g119 and g131. Capped games provide no terminal
training samples. There are **1,398,592 played plies/raw trajectory positions** and
**267,164 retained samples** (212,942 training plus 54,222 held-out). No generation
has an empty or inadequate held-out partition. All held-out partitions have 13
games; the training partition has 50 or 51 completed games.

| Phase | Sum seconds | Median/generation s | Min s | Max s |
|---|---|---|---|---|
| Self-play | 1095.08269 | 8.3774491 | 6.336767 | 12.9087069 |
| Training | 46.2425958 | 0.3610656 | 0.2450706 | 0.4235105 |
| Validation | 50.8324165 | 0.3771631 | 0.342531 | 0.5183682 |
| Whole lifecycle | 1950.93346 | 14.733486 | 11.9541987 | 22.4699624 |

Summed measured lifecycle time is **1,950.933 seconds** (32m30.933s), versus
**2,142.625 seconds** wall span (35m42.625s), including the g3/g4 gap. Effective
completed-game throughput is **4.292 games/s** over lifecycle time or **7.647 games/s**
over self-play time; training performs about **4,605 samples/s** during measured
training. Lifecycle totals include load/publication/durable-I/O work beyond the three
named phases; the 758.776-second residual is not attributed to one unmeasured cause.
No completed-generation history warning, corrupt record, pending recovery, search
failure or infrastructure failure is evidenced. The gap and ten caps are reported
explicitly; no complete historical GUI/process error or stop-event log exists, so
an absence of persisted failures is not proof of no transient interruption.

## Common-population loss matrix

Dataset A is every saved held-out sample from training001 g1–g128: **52,974**.
Dataset B is every saved held-out sample from training002 g1–g131: **54,222**.
**A is an exact ordered prefix of B: 97.698% of B repeats A.** The extra 1,248
samples come from g129–g131 of the same deterministic stream, not another independent
campaign. No substitute data, regeneration, repartitioning or integer score target
was used.

Each model predicts native side-to-move normalized values. Each sample's teacher
comes from its generation-specific persisted NNUE pin; here all pins are g74.
`NnueEvaluator.evaluate(board)` is followed by native `boundedValue()`, and its integer
score is discarded. Loss is `.5 * (prediction - target)^2`, averaged equally over
sampled positions. Blended target is `.25 * WDL + .75 * teacher`. Production BRN
model/workspace and supervision primitives are reused by the diagnostic-only tool.

| Dataset | Model | Samples | WDL loss | NNUE-teacher loss | 75% blended loss |
|---|---|---|---|---|---|
| A | Normal 75% g130 | 52974 | 0.18815959 | 0.0225022503 | 0.0211645681 |
| A | Replay 75% g125 | 52974 | 0.185464462 | 0.0232238169 | 0.0210319612 |
| A | WDL g121 | 52974 | 0.16961258 | 0.0952010949 | 0.0710519491 |
| A | NNUE g74 | 52974 | 0.228010758 | 0 | 0.0142506724 |
| B | Normal 75% g130 | 54222 | 0.187827772 | 0.0224925292 | 0.0211824005 |
| B | Replay 75% g125 | 54222 | 0.185278508 | 0.0231778278 | 0.0210590584 |
| B | WDL g121 | 54222 | 0.170048606 | 0.095244973 | 0.0713019417 |
| B | NNUE g74 | 54222 | 0.227434344 | 0 | 0.0142146465 |

NNUE's zero teacher loss is an identity check, not evidence that it predicts truth
perfectly; its blended loss equals WDL loss divided by 16 by construction. On A,
g130 versus replay g125 has **.63% worse blended loss**, **1.45% worse WDL loss** and
**3.11% better teacher loss**. On B the corresponding differences are **.59% worse**,
**1.38% worse** and **2.96% better**. Both 75% models remain much closer to the teacher
than WDL g121, while WDL g121 retains lower WDL loss on these pooled populations.

To expose the overlap rather than let it hide in pooled results, the small appended
g129–g131 population alone is also summarized (1,248 samples):

| Model | WDL loss | NNUE-teacher loss | 75% blended loss |
|---|---|---|---|
| Normal 75% g130 | 0.17374307 | 0.0220798949 | 0.0219393317 |
| Replay 75% g125 | 0.177385291 | 0.0212257232 | 0.0222092582 |
| WDL g121 | 0.18855665 | 0.0971074705 | 0.0819134085 |
| NNUE g74 | 0.202967237 | 0 | 0.0126854523 |

Here g130 has about **1.22% lower blended loss** than g125 but higher teacher loss.
Those batches already participated in g130 selection (and g129–g130 training used
their separate training partitions); this is not an untouched test set. Both full
populations include within-lineage Best-selection data, and whole-game holdout within
a generation does not ensure global position disjointness across generations.
No campaign-specific over-specialization or independent cross-campaign generalization
can be established or excluded with these overlapping data.

## Fixed evaluator corpus

The unchanged `seedv6-brn2-diagnostics-v1` corpus has 15 roots, 233 legal children,
248 pooled outputs, 210 quiet edges and 23 tactical edges, SHA-256
`0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.
Delta is `abs(-f(child) - f(parent))` in normalized parent perspective. Quiet excludes
captures, promotions and delivered checks; tactical is their union. Delta/IQR divides
the category median by that model's pooled normalized IQR. SD is population SD;
quantiles interpolate at `(n-1)*p`; Spearman uses average tie ranks; zero has its own
sign. NNUE correlations are on actual side-to-move normalized values. Accepted
replay/WDL measurements are reused only after their saved artifact hashes and
checkpoint identities are verified.

| Model | Raw SD | Normalized SD | Quiet median Δ | Quiet Δ/IQR | Tactical median Δ | Tactical Δ/IQR | Pearson | Spearman | Sign agreement |
|---|---|---|---|---|---|---|---|---|---|
| Normal 75% g130 | 0.320738278 | 0.293196396 | 0.183392225 | 0.470172436 | 0.159857468 | 0.409835123 | 0.841913559 | 0.763691851 | 0.798387097 |
| Replay 75% g125 | 0.333614641 | 0.308482891 | 0.134513301 | 0.292391424 | 0.168879751 | 0.367093742 | 0.81752451 | 0.779397618 | 0.806451613 |
| WDL g121 | 0.342218767 | 0.302590821 | 0.299234632 | 0.720925084 | 0.155615699 | 0.374914027 | -0.0981314403 | -0.0116056517 | 0.560483871 |
| NNUE g74 | 0.653115316 | 0.475713259 | 0.103092323 | 0.155644784 | 0.128999362 | 0.194758225 | 1 | 1 | 1 |

G130 broadly preserves output scale (raw SD .3207 versus .3336) and strong NNUE
agreement (Pearson .8419 versus .8175). It does **not** reproduce every local-stability
advantage: quiet median rises 36.34%, and quiet median/IQR rises 60.80%. Tactical
median falls 5.34%, but tactical median/IQR rises 11.64% because the overall IQR
narrows. Spearman and sign agreement are slightly lower. Against WDL g121, g130
has much better NNUE agreement and quiet stability, but no uniformly better tactical
ratio. NNUE remains more locally stable in both categories. These are evaluator
diagnostics, not playing-strength measurements.

Raw, normalized and actual integer search-score distributions (248 values/model):

| Model | Output | Mean | SD | Min | p5 | Median | p95 | Max |
|---|---|---|---|---|---|---|---|---|
| Normal 75% g130 | rawPreTanh | 0.0290486949 | 0.320738278 | -0.658121684 | -0.392832411 | -0.0342908416 | 0.644418729 | 0.8536286 |
| Normal 75% g130 | normalized | 0.0210463698 | 0.293196396 | -0.577112044 | -0.373798345 | -0.0342767108 | 0.567899217 | 0.692960384 |
| Normal 75% g130 | searchScore | 684.245968 | 9532.09428 | -18762 | -12152.85 | -1114.5 | 18463.15 | 22529 |
| Replay 75% g125 | rawPreTanh | -0.0225345132 | 0.333614641 | -0.721486998 | -0.518481767 | -0.10401089 | 0.579124392 | 0.924608223 |
| Replay 75% g125 | normalized | -0.0244740083 | 0.308482891 | -0.617829539 | -0.476524953 | -0.10363718 | 0.522022051 | 0.728070157 |
| Replay 75% g125 | searchScore | -795.657258 | 10029.0772 | -20086 | -15492.4 | -3369.5 | 16971.2 | 23670 |
| WDL g121 | rawPreTanh | -0.0440163338 | 0.342218767 | -1.17014001 | -0.676967886 | -0.0949495551 | 0.478247815 | 1.17294449 |
| WDL g121 | normalized | -0.0380569148 | 0.302590821 | -0.824317048 | -0.5895379 | -0.094665188 | 0.444838819 | 0.825213813 |
| WDL g121 | searchScore | -1237.29032 | 9837.54928 | -26799 | -19166.55 | -3077.5 | 14462 | 26829 |
| NNUE g74 | rawPreTanh | 0.110319693 | 0.653115316 | -1.47473049 | -0.739172688 | -0.0289610866 | 1.39360318 | 1.42664778 |
| NNUE g74 | normalized | 0.055148621 | 0.475713259 | -0.900476014 | -0.628624622 | -0.0289527134 | 0.883957662 | 0.89097757 |
| NNUE g74 | searchScore | 1792.95968 | 15465.882 | -29275 | -20436.95 | -941 | 28737.95 | 28967 |

All four models have **zero** pooled values with `abs(normalized) >= .95`, .99,
.999 or exactly 1, and zero endpoint/clamped scores at ±32,511. Search scores are
the existing fixed mapping, not calibrated centipawns. All measured BRN color-reversal
raw/value/score residuals remain exactly zero. This corpus does not exhaust searched
positions or establish a universal saturation bound.

## Bounded search and Kiwipete

Normal runs use the accepted 15 roots, requested depth 4, one thread, private cold
262,144-entry TT per search, mate-distance-only selectivity, full windows, singleton
root history, diagnostics enabled, qshadow disabled, exact **1,000,000 nodes** and
cooperative **10,000 ms** per search. Each position has its own JVM with one warmup
and two measured searches, under a **40-second hard subprocess watchdog**. No bound
was increased to obtain a completion. All repeated measured non-timing fields match;
main plus q equals total. Terminal roots report depth 1 and zero nodes; ratios there
are undefined. Aggregates represent one corpus, not both repetitions combined.

| Model | Main | Qnodes | Total | Q/total | Q/main | Sum median ms | Worst ms | Completed / terminal / limited |
|---|---|---|---|---|---|---|---|---|
| Normal 75% g130 | 32386 | 57479 | 89865 | 0.639614978 | 1.7748101 | 766.7638 | 449.1439 | 13 / 2 / 0 |
| Replay 75% g125 | 38451 | 158077 | 196528 | 0.80434849 | 4.11112845 | 1524.96365 | 674.1993 | 13 / 2 / 0 |
| WDL g121 | 41285 | 1037239 | 1078524 | 0.961720833 | 25.1238707 | 7541.7084 | 6953.7868 | 12 / 2 / 1 |
| NNUE g74 | 34894 | 48723 | 83617 | 0.582692515 | 1.39631455 | 253.78815 | 85.3744 | 13 / 2 / 0 |

G130 completes all 13 nonterminal roots and both terminal roots in both repetitions
and warmups: **zero node-limit, engine-timeout, hard-watchdog, stall or search-failure
events** in 45 normal searches. The reused WDL Kiwipete results are node-limited
prefixes at completed depth 3; their depth-4 cost remains unknown. Replay and NNUE
also completed all nonterminal roots in the accepted runs.

G130 uses 54.27% fewer corpus nodes than replay g125 and 91.67% fewer than the bounded
WDL total. Its total is 7.47% above NNUE's, but its search time remains much higher;
this does not overturn the accepted inference-cost evidence. Reused timings were
recorded at different times on the same Windows 11 / Ryzen 5 5500 / Java 21 host;
fresh and replay use `-Xms256m -Xmx1536m`, canonical WDL/NNUE used a 1,024-MiB cap.
Latencies exclude JVM startup/loading/output, are descriptive, and are not strength
or clean cross-run speed rankings. Sum latency is the sum of position medians;
worst latency is the largest measured single search, Kiwipete for all four models.

Kiwipete normal search:

| Model | Main | Qnodes | Total | Q/total | Q/main | Median ms | Worst ms | Depth | Status |
|---|---|---|---|---|---|---|---|---|---|
| Normal 75% g130 | 9129 | 45587 | 54716 | 0.833156663 | 4.99364662 | 443.63815 | 449.1439 | 4 | COMPLETED |
| Replay 75% g125 | 9662 | 82243 | 91905 | 0.894869702 | 8.5120058 | 667.21865 | 674.1993 | 4 | COMPLETED |
| WDL g121 | 7595 | 992405 | 1000000 | 0.992405 | 130.665569 | 6859.76025 | 6953.7868 | 3 | NODE_LIMIT |
| NNUE g74 | 9135 | 32300 | 41435 | 0.77953421 | 3.53585112 | 82.50555 | 85.3744 | 4 | COMPLETED |

### Same-edge qshadow and tree observations

One fresh g130 Kiwipete run uses the existing separate instrumentation allowance:
depth 4, 1,000,000 nodes, cooperative **30 seconds**, hard **45-second** watchdog,
no warmup, one repetition, NNUE shadow stride 1, symmetry stride 101/limit 8,192.
This matches the accepted replay/canonical qshadow settings; the normal-search
10-second limit above was not relaxed. Shadow timing is excluded from normal latency.
Fresh qshadow completes depth 4 with 54,716 nodes, identical normal-search node counts,
score, move and PV; accounting is complete and neither time bound fires.

All adjacent-static deltas below are absolute parent-perspective **integer search
units**, on edges where both endpoints were evaluated. The NNUE shadow observes
the same edges as the BRN driver. Different BRN drivers produce different trees,
so rows are paired within each tree, not across an identical cross-model tree.

| Driving tree | Evaluator | Edges | Mean | Median | p95 | Max |
|---|---|---|---|---|---|---|
| Normal 75% g130 | BRN | 34872 | 7597.5735 | 5891 | 20721.8 | 42563 |
| Normal 75% g130 | NNUE shadow | 34872 | 7304.76078 | 4706 | 23369.95 | 53415 |
| Replay 75% g125 | BRN | 56465 | 8447.62892 | 6879 | 21635 | 48739 |
| Replay 75% g125 | NNUE shadow | 56465 | 7219.04258 | 4778 | 22540 | 54534 |
| WDL g121 | BRN | 659077 | 13264.0125 | 11516 | 31584.2 | 58541 |
| WDL g121 | NNUE shadow | 659077 | 6768.64141 | 3869 | 22942 | 56495 |

The fresh paired BRN/NNUE median ratio is **1.252**, versus replay **1.440** and
WDL **2.976**. The fresh p95 is below its shadow's p95, while its median remains
larger. This improves the measured Kiwipete tree behavior despite the worse quiet
fixed-corpus ratio, illustrating why a single static statistic cannot predict cost.

| Measurement | Normal 75% g130 | Replay 75% g125 | WDL g121 |
|---|---|---|---|
| Observed q positions | 51970 | 89165 | 998853 |
| Shadow evaluations | 48407 | 79667 | 876259 |
| Stand-pat eligible | 47565 | 76060 | 815521 |
| Both cutoff | 28495 | 41241 | 225104 |
| BRN only cutoff | 5176 | 7707 | 102813 |
| NNUE only cutoff | 4279 | 9564 | 214437 |
| Neither cutoff | 9615 | 17548 | 273167 |
| Shadow-only frontier nodes | 512 | 984 | 3562 |
| Descendants below frontier | 28915 | 63418 | 982608 |
| Maximum qply | 18 | 19 | 20 |
| Soft-qply-limit returns | 842 | 3607 | 60738 |
| Aborted returns | 0 | 0 | 7 |
| Exception returns | 0 | 0 | 0 |
| Instrumented elapsed ms | 791.3078 | 1080.5311 | 10537.4944 |

Fresh's 28,915 descendants below shadow-only frontiers locate actual work; they are
not a prediction of node savings if NNUE scores replaced BRN. Shadow substitutions
change windows, returns and visited boards. There are no fresh aborted/exception
qtrace returns. Its 515 sampled symmetry positions retain exact zero BRN reversal
residuals. WDL's seven aborted returns are expected node-cap unwind, not seven stalls.
The small corpus and evaluator-dependent tree populations limit extrapolation.

## Interpretation and next decision boundary

1. **Durability/correctness:** yes for the observed lineage. All 131 objectives and
   teachers are consistent; production readers authenticate the chain; the first
   128 models/optimizers and decisions reproduce the accepted replay exactly. This
   is strong normal-workflow integration/restart-consistency evidence.
2. **131 versus 128:** per-run counting is proven. The exact 3+128 / 3+53 split and
   isolated gap strongly support a later Resume. Historical actions remain unproven
   because session events/limits were not saved. No implementation defect is shown.
3. **Fresh versus replay evaluator/search behavior:** scale and teacher agreement
   broadly persist; quiet stability worsens, tactical measurements are mixed, and
   bounded search/Kiwipete become cheaper. G130 is a later checkpoint of the same
   deterministic learning trajectory through g128, not an independent replication.
4. **Two-dataset generalization / over-specialization:** not established. Both 75%
   models have similar A/B loss, but 97.698% overlap makes that expected. The extra
   three-generation sample gives limited descriptive evidence only. Neither campaign
   specialization nor independent robustness can be inferred from this design.
5. **Versus WDL:** substantially better NNUE agreement, quiet stability and bounded
   tactical-search cost, with worse pooled WDL prediction. These trade-offs agree
   with the supervision findings; they establish no playing-strength ordering.
6. **Late learning:** g130 wins its own batch and improves bounded search, but pooled
   blended loss slightly worsens and quiet stability regresses. This is compatible
   with a plateau/noisy promotions, not evidence that longer training necessarily helps.
7. **Next experiment decision:** prioritize an actually independent fresh 75% lineage
   using a different documented **run/data seed**, while keeping the intended teacher
   and other controls pinned. Verify the first generated sample hashes differ before
   committing to a long run, and define per-session versus absolute completion intent.
   Do not execute that experiment as part of this diagnostic. A focused strength screen
   containing g130 is a separate useful option if strength is the immediate decision,
   but it cannot repair the missing independent-data evidence. Generation-limit
   remediation is not supported without a different intended limit contract or new
   defect evidence; optional session telemetry/wording is a separate product decision.

75% remains provisional, not canonical/default. Candidate-versus-Best BRN game
validation remains absent. Nothing here justifies score calibration, architecture
changes or BRN-3. No strength games, additional sweep or longer training were run.

## Validation, evidence, protected state and Git boundary

Applicable supplied governance, repository README and active
`source/CHESS_SEARCH_CONTRACT.md` were inspected. Its mechanical-cost/tree-shape/
playing-strength distinction is preserved. No local/ancestor AGENTS.md was present.
Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent;
neither capability was activated or created. First successful clock observation was
03:49:04Z after an unsupported PowerShell `Get-Date -AsUTC` attempt; no earlier
journal timestamp is claimed and no journal is required here.

Focused validation: **four tests passed, zero failures/errors/skips**: three new
cross-campaign-tool tests and the new real temporary-store limit test. They check
native production prediction/loss agreement, use of per-sample teacher targets,
sample-weighted pooling, the distinction between blended-target loss and weighted
component losses, exact sample identity/order, and actual per-service completion
boundaries. The Gradle command compiled production/test sources, built the launcher
dependency and passed. Runtime evaluation authenticated all 259 A/B generation
plan/data/history/validation relationships and pinned teachers; all selected models
loaded. Cross-loss measurements cover all four models on both complete populations.
Four retained g129–g131 Candidate/incumbent models and final g0/g131 payloads were
additionally checked using production readers and component-loss recomputation.
The existing static and bounded-search tools were reused unchanged. New runtime
subprocesses all exited zero within their hard watchdogs; repeated search accounting,
qshadow accounting and preserved accepted artifact hashes were verified. The initial
staged whitespace check flagged CRLF on the new report; its new-file line endings
were normalized to LF and the final staged whitespace check passed.

Full before/after SHA-256, size and modification-time inventories are identical
for **all 2,400 protected files**: training001 948, training002 959, NNUE training
401, accepted ablation 55 and accepted sweep 37. The four pre-existing stores'
**1,441 files** also exactly match the accepted integration's pre-campaign
`protected-after.json`, establishing that this fresh campaign did not change
those source/experimental stores. This includes training001's historical pending
g129 evidence. Selected checkpoint hashes and fresh retained payload/manifest
bindings were separately verified. The final full inventory completed at
2026-09-23T04:08:23Z. No training application process was observed during the
initial process inspection, and no store writer or recovery service was invoked
against a protected store. All writes remained in this repository's task files,
ignored diagnostic/build output or isolated test temporary stores.

Initial Git state was accepted integration HEAD plus inherited untracked `app/bin/`
only. That directory remains excluded and preserved. One focused diagnostic commit
contains exactly these five task files:

- `BRN_FRESH_75_TRAINING_DIAGNOSTICS.md`.
- `BRN_DIAGNOSTICS.md` (link and independent-data caveat).
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/Brn2CrossCampaignDiagnostics.java`.
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/Brn2CrossCampaignDiagnosticsTest.java`.
- `app/src/test/java/com/ohinteractive/seedv6/training/service/Brn2BlendedBootstrapTest.java` (bounded limit reproduction).

No production evaluator, search, training, generation-limit or GUI semantics changed.
No push, deploy, reset, discard, amendment, rebase or absorption of inherited work
occurred. Final commit identity and observed worktree state appear in the completion
response; the expected sole remaining item is inherited untracked `app/bin/`.
This is diagnostic work-unit completion, not user acceptance or independent-campaign
acceptance.

Generated evidence remains ignored under `app/build/brn2-fresh-75-diagnostics/`:
complete production-reader JSONL, all 131 history/plan/sample summaries, exact replay
and dataset-equality evidence, preferences/process observations, before/after full-file
inventories, static/search/qshadow JSONL, accepted-artifact hash references,
`analysis.json`, full commands/watchdog receipts, focused-test log/XML, supplementary
component-loss checks and analysis scripts. `artifact-sha256.json` binds the generated
evidence. The committed tables preserve the key findings after build cleanup.

Reproduction (new output filenames; never invoke training against the selected stores):

```powershell
.\gradlew.bat :app:test --tests '*Brn2CrossCampaignDiagnosticsTest' --tests '*Brn2BlendedBootstrapTest.generationLimitCountsNewCompletionsPerServiceRunNotLineageTotal' --console=plain
java -Xms256m -Xmx1536m -cp 'app/build/classes/java/main;app/build/resources/main;app/build/install/seedv6/lib/*' com.ohinteractive.seedv6.tools.search.Brn2CrossCampaignDiagnostics '<training001 store>' '<training002 store>' '<exact replay network.brn2 above>' '<exact NNUE g74 checkpoint above>'
```

The second command writes JSONL to stdout. Use the exact pinned paths above and
preserve output in a new ignored diagnostic directory. The saved runner encloses
cross evaluation in a 120-second subprocess watchdog and uses the established
40/45-second normal/qshadow watchdogs. Search/static arguments and all exact commands
are retained in `processes.json`; `analyze.py` verifies the reused measurements.

Deliberately skipped: `fullCheck`, broad GUI/slow suites, playing-strength games,
training Resume or a new long campaign, another supervision sweep, score calibration,
architecture/BRN-3 work, inference optimization and unrelated tests. The full
inference-throughput/memory campaign was not rerun: `git diff be94af4 HEAD` shows
no changes under production `core/` or `search/`, so the accepted canonical figures
remain the implementation baseline. This native CLI diagnostic has no required
browser or GUI verification. Static/source inspection is not represented as runtime
verification of historical UI actions.

Human actions required after this prompt: **None** for this completed diagnostic.
Choosing and authorizing the next experiment is a separate decision; independent-data
claims remain unavailable until such an experiment is actually completed and audited.

## Complete fresh loss/decision history

Full precision remains in the authenticated source and generated evidence. This table
rounds to nine significant digits. P/R is promotion/retention; Best is the resulting
published generation. Every row uses its own held-out population and .75 objective.

| g | Train | Candidate blend | Incumbent blend | Candidate WDL | Incumbent WDL | Candidate teacher | Incumbent teacher | P/R | Best |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0.0217496696 | 0.0578574276 | 0.228280587 | 0.192016437 | 0.360187524 | 0.0648310266 | 0.236004876 | P | 1 |
| 2 | 0.0190969847 | 0.0600513185 | 0.073617872 | 0.223143862 | 0.253755575 | 0.0624325605 | 0.0703173942 | P | 2 |
| 3 | 0.0158358177 | 0.0376678661 | 0.063694975 | 0.193079738 | 0.246026362 | 0.0394473875 | 0.0565013248 | P | 3 |
| 4 | 0.0173837903 | 0.0355210815 | 0.0495889709 | 0.220850267 | 0.234730078 | 0.0334433135 | 0.0475738955 | P | 4 |
| 5 | 0.0174258724 | 0.0417204216 | 0.0343549756 | 0.194409355 | 0.187913873 | 0.0495231923 | 0.0418677585 | R | 4 |
| 6 | 0.0186380351 | 0.0542766128 | 0.0535348582 | 0.21779333 | 0.211967496 | 0.0502554011 | 0.0512083398 | R | 4 |
| 7 | 0.0111776845 | 0.0448315911 | 0.0553700106 | 0.279124594 | 0.289785393 | 0.0394489241 | 0.0499465503 | P | 7 |
| 8 | 0.0131434959 | 0.0332707778 | 0.0373975223 | 0.168346723 | 0.177026764 | 0.0310529785 | 0.0336619575 | P | 8 |
| 9 | 0.0142914121 | 0.0217094123 | 0.0234676465 | 0.117838662 | 0.125020476 | 0.0228695123 | 0.0228198867 | P | 9 |
| 10 | 0.0148249254 | 0.0535853733 | 0.0548575498 | 0.303500774 | 0.315891542 | 0.0426384989 | 0.0402044784 | P | 10 |
| 11 | 0.0113108159 | 0.0381631202 | 0.040649996 | 0.244005212 | 0.244605676 | 0.0359346595 | 0.0390503393 | P | 11 |
| 12 | 0.0124543821 | 0.0208727206 | 0.0207360111 | 0.152854051 | 0.149759319 | 0.0325653006 | 0.0334145984 | R | 11 |
| 13 | 0.0148157149 | 0.0260344969 | 0.0227729938 | 0.154823394 | 0.161042664 | 0.0299734043 | 0.0235516437 | R | 11 |
| 14 | 0.0131670878 | 0.0372586253 | 0.035611819 | 0.221067686 | 0.210095787 | 0.0311765886 | 0.0326381462 | R | 11 |
| 15 | 0.0139934611 | 0.042785162 | 0.0349481275 | 0.247068888 | 0.231495552 | 0.0361067188 | 0.0308484516 | R | 11 |
| 16 | 0.0118848733 | 0.0259995082 | 0.0238774923 | 0.207947985 | 0.187334779 | 0.0242883951 | 0.0283301095 | R | 11 |
| 17 | 0.0117660507 | 0.0292640562 | 0.023930529 | 0.192398939 | 0.179990214 | 0.0261836214 | 0.0232084932 | R | 11 |
| 18 | 0.0109591346 | 0.0321905358 | 0.0328909296 | 0.216437572 | 0.222257456 | 0.0374945079 | 0.036488405 | P | 18 |
| 19 | 0.00984638426 | 0.0284899191 | 0.0324520402 | 0.215490452 | 0.221865161 | 0.0283096105 | 0.0314675356 | P | 19 |
| 20 | 0.0135192376 | 0.021668139 | 0.0226171386 | 0.147362556 | 0.155636956 | 0.0236783829 | 0.0221855821 | P | 20 |
| 21 | 0.010412086 | 0.0237900358 | 0.0240083641 | 0.11447093 | 0.118088683 | 0.0254757903 | 0.0245609771 | P | 21 |
| 22 | 0.0123038714 | 0.0340667988 | 0.0322490045 | 0.25234275 | 0.239096488 | 0.0328573963 | 0.0348490914 | R | 21 |
| 23 | 0.0104593518 | 0.0258158898 | 0.0313408611 | 0.157985146 | 0.168092997 | 0.0255780773 | 0.029575422 | P | 23 |
| 24 | 0.00952528459 | 0.0354403725 | 0.0337869628 | 0.194306915 | 0.203849866 | 0.0389370754 | 0.0335515453 | R | 23 |
| 25 | 0.0080804539 | 0.0284615685 | 0.0280705537 | 0.279532807 | 0.278418397 | 0.0310673866 | 0.0309175034 | R | 23 |
| 26 | 0.0129895022 | 0.0321392042 | 0.029928377 | 0.159761768 | 0.170106794 | 0.0436750867 | 0.0372789755 | R | 23 |
| 27 | 0.00858748426 | 0.0304139861 | 0.0322056348 | 0.27542627 | 0.29690137 | 0.0319857432 | 0.0272162415 | P | 27 |
| 28 | 0.0113941706 | 0.0276122963 | 0.0274737041 | 0.183116073 | 0.188863912 | 0.0303585019 | 0.0282577662 | R | 27 |
| 29 | 0.0171747212 | 0.0330459262 | 0.0262231871 | 0.156535246 | 0.152522458 | 0.0348749078 | 0.0271155184 | R | 27 |
| 30 | 0.0128644636 | 0.0368847945 | 0.0321013828 | 0.198026277 | 0.189675529 | 0.0394843183 | 0.0358900187 | R | 27 |
| 31 | 0.0114928137 | 0.0267397334 | 0.0261198738 | 0.174535891 | 0.177274131 | 0.0310011666 | 0.0292619402 | R | 27 |
| 32 | 0.0120693031 | 0.0279825582 | 0.0255858008 | 0.240025946 | 0.228647099 | 0.0266902085 | 0.0272874812 | R | 27 |
| 33 | 0.0119068927 | 0.0293968556 | 0.0253915703 | 0.312808073 | 0.290553945 | 0.0277469152 | 0.0298245774 | R | 27 |
| 34 | 0.0100954512 | 0.019304126 | 0.0193020034 | 0.155922048 | 0.162999958 | 0.0226903226 | 0.0203281893 | R | 27 |
| 35 | 0.0103380662 | 0.0219778074 | 0.0240504125 | 0.178412351 | 0.172086191 | 0.0232950512 | 0.0281672444 | P | 35 |
| 36 | 0.00908999977 | 0.0175254842 | 0.0221387346 | 0.141606993 | 0.145229303 | 0.0208295032 | 0.0257730672 | P | 36 |
| 37 | 0.0101248491 | 0.0282276967 | 0.0199568535 | 0.240417853 | 0.224626652 | 0.0303300664 | 0.0245660093 | R | 36 |
| 38 | 0.014900001 | 0.0235478799 | 0.026035407 | 0.217726995 | 0.243620606 | 0.0284430251 | 0.0231285244 | P | 38 |
| 39 | 0.0110380476 | 0.0335797065 | 0.0314905299 | 0.304870732 | 0.296100458 | 0.0327800632 | 0.0329179194 | R | 38 |
| 40 | 0.00957406927 | 0.0197453816 | 0.0214009978 | 0.126578547 | 0.138705049 | 0.0194651992 | 0.0176305202 | P | 40 |
| 41 | 0.012569943 | 0.030158149 | 0.0317558458 | 0.230704479 | 0.22892325 | 0.0276636157 | 0.0303876211 | P | 41 |
| 42 | 0.0112506439 | 0.0260599939 | 0.0253449347 | 0.179023609 | 0.176958963 | 0.0218313331 | 0.0215661363 | R | 41 |
| 43 | 0.010867959 | 0.0324636939 | 0.0344851253 | 0.273407569 | 0.275753359 | 0.0291447952 | 0.031058107 | P | 43 |
| 44 | 0.0102412728 | 0.0182412142 | 0.0160923492 | 0.193676935 | 0.189115062 | 0.0231230733 | 0.0217785444 | R | 43 |
| 45 | 0.0101638725 | 0.020734533 | 0.022826089 | 0.141189433 | 0.14321589 | 0.0284093299 | 0.0305225856 | P | 45 |
| 46 | 0.0115998397 | 0.0193327081 | 0.0193942092 | 0.181568113 | 0.18730561 | 0.0228677863 | 0.0210372888 | P | 46 |
| 47 | 0.010526437 | 0.0220820005 | 0.0237741845 | 0.158213281 | 0.153783306 | 0.0240732899 | 0.0278061933 | P | 47 |
| 48 | 0.00893151292 | 0.0181719604 | 0.0187451084 | 0.127640417 | 0.128193629 | 0.0197568708 | 0.0203366641 | P | 48 |
| 49 | 0.0104269941 | 0.0337928804 | 0.0287751172 | 0.297156061 | 0.293631068 | 0.0389573956 | 0.0334420425 | R | 48 |
| 50 | 0.0100528267 | 0.0270462166 | 0.0259399479 | 0.121679287 | 0.120145943 | 0.0342356436 | 0.0332717333 | R | 48 |
| 51 | 0.0103351533 | 0.0322509104 | 0.0330295893 | 0.23116614 | 0.233012931 | 0.0342703548 | 0.0346929965 | P | 51 |
| 52 | 0.0104331505 | 0.0250240769 | 0.0257442952 | 0.161542398 | 0.158134906 | 0.030003282 | 0.0320994035 | P | 52 |
| 53 | 0.0111384116 | 0.0264294651 | 0.0262595741 | 0.183232075 | 0.18965941 | 0.0238918289 | 0.0215228624 | R | 52 |
| 54 | 0.00839132086 | 0.0269098859 | 0.0245462157 | 0.214305194 | 0.214692208 | 0.0258630721 | 0.0225825073 | R | 52 |
| 55 | 0.0116276477 | 0.0148136535 | 0.0153930079 | 0.159707141 | 0.161468524 | 0.0157853744 | 0.0159707193 | P | 55 |
| 56 | 0.00997185386 | 0.0206999039 | 0.0202133046 | 0.157859499 | 0.150133414 | 0.02309577 | 0.0250223324 | R | 55 |
| 57 | 0.0118475962 | 0.018449909 | 0.019634474 | 0.205593308 | 0.199925268 | 0.0218005919 | 0.0252693587 | P | 57 |
| 58 | 0.00980128872 | 0.0316935603 | 0.0278787426 | 0.259371521 | 0.239336666 | 0.0284114335 | 0.0300032948 | R | 57 |
| 59 | 0.00827890382 | 0.0245917473 | 0.022222209 | 0.216340138 | 0.201817187 | 0.023687852 | 0.0253694512 | R | 57 |
| 60 | 0.0128555019 | 0.0234185887 | 0.0278134477 | 0.196146642 | 0.202116913 | 0.0319978074 | 0.0358675292 | P | 60 |
| 61 | 0.0107697471 | 0.0147142748 | 0.0162312207 | 0.150413667 | 0.153096649 | 0.0158653668 | 0.0169936337 | P | 61 |
| 62 | 0.00927264137 | 0.0249168223 | 0.0243088999 | 0.203736074 | 0.199021924 | 0.0230918357 | 0.0238526559 | R | 61 |
| 63 | 0.0119360464 | 0.0201060977 | 0.019287963 | 0.168386946 | 0.165962487 | 0.0207845662 | 0.0205018731 | R | 61 |
| 64 | 0.00884261954 | 0.020991825 | 0.0193811459 | 0.17103489 | 0.167626977 | 0.0203818642 | 0.0193702631 | R | 61 |
| 65 | 0.0115755575 | 0.0281799899 | 0.0293885817 | 0.267266266 | 0.256453789 | 0.033883471 | 0.0390990857 | P | 65 |
| 66 | 0.010017517 | 0.0175436509 | 0.0174277328 | 0.174698206 | 0.17332815 | 0.0198283458 | 0.0201304736 | R | 65 |
| 67 | 0.0112211029 | 0.0178139992 | 0.0173453628 | 0.175512124 | 0.172280071 | 0.0212899836 | 0.0217424863 | R | 65 |
| 68 | 0.0120685571 | 0.0187055563 | 0.0171553891 | 0.145589573 | 0.150979675 | 0.0208515848 | 0.0169879944 | R | 65 |
| 69 | 0.0125387911 | 0.023403049 | 0.0209483973 | 0.177035059 | 0.17001715 | 0.0246963904 | 0.0237628243 | R | 65 |
| 70 | 0.0099349687 | 0.0251945282 | 0.0262269978 | 0.236813321 | 0.241253195 | 0.0214006033 | 0.0212972715 | P | 70 |
| 71 | 0.00971075313 | 0.0227486923 | 0.0244115148 | 0.111542916 | 0.114357462 | 0.0273163461 | 0.0285952607 | P | 71 |
| 72 | 0.0100424427 | 0.0131133967 | 0.0119959701 | 0.136434827 | 0.134236234 | 0.0159925842 | 0.0152355463 | R | 71 |
| 73 | 0.00792809316 | 0.0141572903 | 0.0173402548 | 0.0747007044 | 0.0839132383 | 0.0206850194 | 0.0218581274 | P | 73 |
| 74 | 0.0114956864 | 0.0188159885 | 0.0165055673 | 0.15769092 | 0.146065595 | 0.0192610809 | 0.0200556274 | R | 73 |
| 75 | 0.0091162091 | 0.0300487291 | 0.0329517179 | 0.217542531 | 0.225783741 | 0.0282805834 | 0.0294041649 | P | 75 |
| 76 | 0.00812614751 | 0.0235368059 | 0.026183089 | 0.126734013 | 0.131746031 | 0.0397681927 | 0.0416258974 | P | 76 |
| 77 | 0.0129378598 | 0.0248901314 | 0.0246244205 | 0.204862311 | 0.194331732 | 0.0269566771 | 0.030112589 | R | 76 |
| 78 | 0.0137717903 | 0.0290597213 | 0.0235359004 | 0.24048123 | 0.217793065 | 0.0314527856 | 0.031650413 | R | 76 |
| 79 | 0.00951506894 | 0.016258414 | 0.0236268262 | 0.17397457 | 0.20523176 | 0.0218970931 | 0.0213025794 | P | 79 |
| 80 | 0.0109537473 | 0.0143542843 | 0.0151289575 | 0.221176111 | 0.212835116 | 0.0174294802 | 0.0212427093 | P | 80 |
| 81 | 0.00847573446 | 0.026842306 | 0.0244675832 | 0.22999962 | 0.231165783 | 0.0295686931 | 0.026013675 | R | 80 |
| 82 | 0.00981166726 | 0.0234127444 | 0.0199168128 | 0.170911359 | 0.168707473 | 0.0212895964 | 0.0173629831 | R | 80 |
| 83 | 0.0111232869 | 0.0164432303 | 0.0198255367 | 0.236746816 | 0.256332285 | 0.0248515808 | 0.0228328329 | P | 83 |
| 84 | 0.0106399756 | 0.0244609291 | 0.0235336887 | 0.194844344 | 0.194033835 | 0.0274509732 | 0.0264848223 | R | 83 |
| 85 | 0.0116413737 | 0.0145157527 | 0.0155137065 | 0.179001377 | 0.179967301 | 0.0156357635 | 0.016644394 | P | 85 |
| 86 | 0.0118068914 | 0.0252568398 | 0.0289430261 | 0.247362094 | 0.249070116 | 0.0298618458 | 0.0342074204 | P | 86 |
| 87 | 0.00901118333 | 0.0242283704 | 0.0241166682 | 0.139950703 | 0.138547929 | 0.0254098382 | 0.0257284934 | R | 86 |
| 88 | 0.009776052 | 0.0244904202 | 0.0202360653 | 0.136556418 | 0.124300985 | 0.0286706205 | 0.0270832916 | R | 86 |
| 89 | 0.010092045 | 0.0198895635 | 0.0198197643 | 0.158192537 | 0.15374689 | 0.017970451 | 0.0193592675 | R | 86 |
| 90 | 0.0103869417 | 0.0215548599 | 0.0174191527 | 0.214559869 | 0.198518532 | 0.0217947196 | 0.0216275559 | R | 86 |
| 91 | 0.00989676274 | 0.0272890561 | 0.025260126 | 0.298538519 | 0.303922349 | 0.0282187448 | 0.0237188948 | R | 86 |
| 92 | 0.00991676457 | 0.0193345998 | 0.0206040731 | 0.128454755 | 0.127667069 | 0.0200860003 | 0.0220411936 | P | 92 |
| 93 | 0.0101410157 | 0.0250314044 | 0.0251480973 | 0.17273532 | 0.185848385 | 0.0370544017 | 0.0328389707 | P | 93 |
| 94 | 0.00976482519 | 0.0155138394 | 0.0195572149 | 0.194335323 | 0.205355948 | 0.0157984397 | 0.0175160653 | P | 94 |
| 95 | 0.0126048767 | 0.0269224745 | 0.0291674123 | 0.273550852 | 0.274241905 | 0.030437822 | 0.0332007217 | P | 95 |
| 96 | 0.0124668508 | 0.0270867571 | 0.0253050845 | 0.123642314 | 0.137909239 | 0.0353934229 | 0.0282622179 | R | 95 |
| 97 | 0.00867466179 | 0.013681378 | 0.0155739604 | 0.141281941 | 0.149089092 | 0.0181535906 | 0.0180746501 | P | 97 |
| 98 | 0.0131236625 | 0.0304787576 | 0.0294970977 | 0.235966426 | 0.239163129 | 0.0251241294 | 0.0227496818 | R | 97 |
| 99 | 0.0104900331 | 0.0206595212 | 0.0220734085 | 0.187440771 | 0.196111728 | 0.0214963701 | 0.0204912343 | P | 99 |
| 100 | 0.012702403 | 0.0214258249 | 0.0201993611 | 0.162785726 | 0.156990052 | 0.0249940761 | 0.0252906821 | R | 99 |
| 101 | 0.0091477537 | 0.0223861594 | 0.02096556 | 0.155659222 | 0.15365235 | 0.0169616881 | 0.0157365127 | R | 99 |
| 102 | 0.0100408992 | 0.0244682559 | 0.022891525 | 0.201965372 | 0.199351043 | 0.0214230346 | 0.0201921698 | R | 99 |
| 103 | 0.00932276304 | 0.0320296867 | 0.0315037932 | 0.22914722 | 0.223005875 | 0.0292169098 | 0.0305628334 | R | 99 |
| 104 | 0.0107601929 | 0.0186917555 | 0.0170690157 | 0.139416921 | 0.130378607 | 0.0176829386 | 0.0185320569 | R | 99 |
| 105 | 0.00991194727 | 0.011501962 | 0.00953264145 | 0.170903158 | 0.17175778 | 0.0209349196 | 0.0180242849 | R | 99 |
| 106 | 0.00962828429 | 0.0225311302 | 0.0225110564 | 0.174171048 | 0.179289249 | 0.0266452275 | 0.0249123954 | R | 99 |
| 107 | 0.0096426186 | 0.0176323922 | 0.0186638047 | 0.135707282 | 0.134462379 | 0.0204059569 | 0.0221961411 | P | 107 |
| 108 | 0.012321407 | 0.0270865369 | 0.0238551443 | 0.217631895 | 0.206368764 | 0.0213600472 | 0.0208059006 | R | 107 |
| 109 | 0.00936149841 | 0.0297681979 | 0.0277853894 | 0.223893481 | 0.215918641 | 0.027180349 | 0.0271948843 | R | 107 |
| 110 | 0.00873022619 | 0.0216405515 | 0.0194374365 | 0.203885548 | 0.183362466 | 0.0196086404 | 0.0235121809 | R | 107 |
| 111 | 0.00902146425 | 0.0107254038 | 0.0117827477 | 0.0826354414 | 0.0824031134 | 0.0133949522 | 0.0148821867 | P | 111 |
| 112 | 0.0102460275 | 0.0272108217 | 0.0261015982 | 0.200597968 | 0.194788585 | 0.0301340951 | 0.0305915914 | R | 111 |
| 113 | 0.0087820548 | 0.0258366597 | 0.0251804269 | 0.248831718 | 0.244467657 | 0.0233246718 | 0.0239043818 | R | 111 |
| 114 | 0.0106936029 | 0.0196322738 | 0.0226338608 | 0.182866821 | 0.182238538 | 0.0202223502 | 0.0244338939 | P | 114 |
| 115 | 0.0092100465 | 0.0229415891 | 0.0203741345 | 0.245190719 | 0.244304803 | 0.0222103223 | 0.0190823549 | R | 114 |
| 116 | 0.0122887734 | 0.0215897133 | 0.0184399531 | 0.176868697 | 0.167816312 | 0.0192077259 | 0.0180255073 | R | 114 |
| 117 | 0.01162869 | 0.016824799 | 0.0137187397 | 0.226976922 | 0.215888343 | 0.0250461027 | 0.024600883 | R | 114 |
| 118 | 0.0105938522 | 0.0290856712 | 0.0250321377 | 0.259302174 | 0.259630453 | 0.0275466013 | 0.0220324635 | R | 114 |
| 119 | 0.0122970519 | 0.0193452521 | 0.0146799302 | 0.164395874 | 0.152051841 | 0.017830788 | 0.0157250365 | R | 114 |
| 120 | 0.00991590457 | 0.0223582038 | 0.0254182465 | 0.181914971 | 0.189131989 | 0.0197658002 | 0.0214401846 | P | 120 |
| 121 | 0.0101937106 | 0.0168254458 | 0.0174436238 | 0.118983044 | 0.12183606 | 0.0237368793 | 0.0236101112 | P | 121 |
| 122 | 0.00968869968 | 0.0288220127 | 0.0266617562 | 0.193421692 | 0.183191869 | 0.0222967349 | 0.0228263339 | R | 121 |
| 123 | 0.0131922753 | 0.0260298883 | 0.0230500834 | 0.180592674 | 0.17308213 | 0.0269102559 | 0.0254406974 | R | 121 |
| 124 | 0.00921670558 | 0.0190634484 | 0.0209187965 | 0.187736755 | 0.18394905 | 0.0228724867 | 0.0266088522 | P | 124 |
| 125 | 0.00807957656 | 0.0159907458 | 0.0166969773 | 0.149453794 | 0.152542576 | 0.016255882 | 0.0161679303 | P | 125 |
| 126 | 0.00881133153 | 0.0164509456 | 0.01513511 | 0.149447908 | 0.146434185 | 0.0169344309 | 0.016184558 | R | 125 |
| 127 | 0.00936190628 | 0.022057131 | 0.020133646 | 0.116097852 | 0.115197775 | 0.0286687728 | 0.0264041518 | R | 125 |
| 128 | 0.0114789778 | 0.0254560458 | 0.0234171141 | 0.238408556 | 0.226782727 | 0.0278380486 | 0.0289947495 | R | 125 |
| 129 | 0.00941813929 | 0.0186125152 | 0.0181846441 | 0.109368737 | 0.10941031 | 0.0229055375 | 0.0223211851 | R | 125 |
| 130 | 0.00846046159 | 0.0251826555 | 0.0279929378 | 0.226568078 | 0.240002158 | 0.0218623003 | 0.0211313169 | P | 130 |
| 131 | 0.0103306274 | 0.0206489066 | 0.0171721042 | 0.183448757 | 0.176101259 | 0.0202545019 | 0.0180679312 | R | 130 |

## Full bounded-search comparison

All rows use the normal-search settings above. Medians/worst are milliseconds;
q/total and q/main are ratios. Reused accepted baselines retain original timing precision.

| Model | Position | Main | Qnodes | Total | Q/total | Q/main | Median ms | Worst ms | Depth | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| Normal 75% g130 | check-evasion | 28 | 2 | 30 | 0.0666666667 | 0.0714285714 | 1.9935 | 2.3091 | 4 | COMPLETED |
| Normal 75% g130 | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2187 | 0.22 | 1 | TERMINAL |
| Normal 75% g130 | en-passant | 389 | 3 | 392 | 0.00765306122 | 0.00771208226 | 9.27285 | 11.0179 | 4 | COMPLETED |
| Normal 75% g130 | middlegame-kiwipete | 9129 | 45587 | 54716 | 0.833156663 | 4.99364662 | 443.63815 | 449.1439 | 4 | COMPLETED |
| Normal 75% g130 | opening-ruy-lopez | 4507 | 2839 | 7346 | 0.386468827 | 0.62990903 | 73.86855 | 75.3542 | 4 | COMPLETED |
| Normal 75% g130 | opening-start | 4617 | 278 | 4895 | 0.0567926456 | 0.060212259 | 50.6492 | 56.6714 | 4 | COMPLETED |
| Normal 75% g130 | promotion-race | 413 | 393 | 806 | 0.487593052 | 0.95157385 | 7.85215 | 9.7606 | 4 | COMPLETED |
| Normal 75% g130 | qsearch-exchanges | 1361 | 870 | 2231 | 0.389959659 | 0.639235856 | 11.4666 | 11.994 | 4 | COMPLETED |
| Normal 75% g130 | queen-endgame | 1885 | 171 | 2056 | 0.0831712062 | 0.0907161804 | 10.6564 | 12.4396 | 4 | COMPLETED |
| Normal 75% g130 | quiet-endgame | 195 | 16 | 211 | 0.0758293839 | 0.0820512821 | 5.48895 | 6.1501 | 4 | COMPLETED |
| Normal 75% g130 | quiet-fianchetto | 8001 | 7283 | 15284 | 0.476511384 | 0.910261217 | 131.4705 | 138.638 | 4 | COMPLETED |
| Normal 75% g130 | quiet-pawn | 532 | 0 | 532 | 0 | 0 | 6.34585 | 8.2515 | 4 | COMPLETED |
| Normal 75% g130 | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2199 | 0.2385 | 1 | TERMINAL |
| Normal 75% g130 | tactical-queen | 183 | 8 | 191 | 0.0418848168 | 0.043715847 | 5.0079 | 5.9862 | 4 | COMPLETED |
| Normal 75% g130 | transposition-knights | 1146 | 29 | 1175 | 0.0246808511 | 0.0253054101 | 8.6146 | 10.5598 | 4 | COMPLETED |
| Replay 75% g125 | check-evasion | 28 | 2 | 30 | 0.0666666667 | 0.0714285714 | 1.77695 | 1.8703 | 4 | COMPLETED |
| Replay 75% g125 | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2007 | 0.2008 | 1 | TERMINAL |
| Replay 75% g125 | en-passant | 361 | 5 | 366 | 0.0136612022 | 0.0138504155 | 6.75855 | 8.6573 | 4 | COMPLETED |
| Replay 75% g125 | middlegame-kiwipete | 9662 | 82243 | 91905 | 0.894869702 | 8.5120058 | 667.21865 | 674.1993 | 4 | COMPLETED |
| Replay 75% g125 | opening-ruy-lopez | 9467 | 7694 | 17161 | 0.448342171 | 0.812717862 | 146.8958 | 148.0918 | 4 | COMPLETED |
| Replay 75% g125 | opening-start | 4104 | 202 | 4306 | 0.0469112866 | 0.0492202729 | 52.5325 | 60.12 | 4 | COMPLETED |
| Replay 75% g125 | promotion-race | 394 | 378 | 772 | 0.489637306 | 0.959390863 | 8.4051 | 9.2709 | 4 | COMPLETED |
| Replay 75% g125 | qsearch-exchanges | 1141 | 646 | 1787 | 0.36149972 | 0.566170026 | 9.26845 | 11.3719 | 4 | COMPLETED |
| Replay 75% g125 | queen-endgame | 1885 | 171 | 2056 | 0.0831712062 | 0.0907161804 | 9.4922 | 10.6567 | 4 | COMPLETED |
| Replay 75% g125 | quiet-endgame | 217 | 26 | 243 | 0.106995885 | 0.119815668 | 7.0547 | 7.5928 | 4 | COMPLETED |
| Replay 75% g125 | quiet-fianchetto | 9531 | 66700 | 76231 | 0.874972124 | 6.99821635 | 597.00755 | 612.9096 | 4 | COMPLETED |
| Replay 75% g125 | quiet-pawn | 417 | 0 | 417 | 0 | 0 | 6.3284 | 8.1158 | 4 | COMPLETED |
| Replay 75% g125 | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.23665 | 0.2485 | 1 | TERMINAL |
| Replay 75% g125 | tactical-queen | 179 | 8 | 187 | 0.0427807487 | 0.0446927374 | 5.55115 | 5.9914 | 4 | COMPLETED |
| Replay 75% g125 | transposition-knights | 1065 | 2 | 1067 | 0.00187441425 | 0.00187793427 | 6.2363 | 8.4429 | 4 | COMPLETED |
| WDL g121 | check-evasion | 28 | 2 | 30 | 0.0666666667 | 0.0714285714 | 2.31135 | 2.6493 | 4 | COMPLETED |
| WDL g121 | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2912 | 0.3627 | 1 | TERMINAL |
| WDL g121 | en-passant | 625 | 22 | 647 | 0.0340030912 | 0.0352 | 6.52275 | 8.4424 | 4 | COMPLETED |
| WDL g121 | middlegame-kiwipete | 7595 | 992405 | 1000000 | 0.992405 | 130.665569 | 6859.76025 | 6953.7868 | 3 | NODE_LIMIT |
| WDL g121 | opening-ruy-lopez | 9465 | 5510 | 14975 | 0.367946578 | 0.582144744 | 144.8592 | 161.1648 | 4 | COMPLETED |
| WDL g121 | opening-start | 5780 | 333 | 6113 | 0.0544740717 | 0.0576124567 | 59.3089 | 62.3416 | 4 | COMPLETED |
| WDL g121 | promotion-race | 464 | 373 | 837 | 0.445639188 | 0.80387931 | 8.18465 | 10.6263 | 4 | COMPLETED |
| WDL g121 | qsearch-exchanges | 1488 | 882 | 2370 | 0.372151899 | 0.592741935 | 12.8935 | 15.3519 | 4 | COMPLETED |
| WDL g121 | queen-endgame | 1887 | 170 | 2057 | 0.0826446281 | 0.0900900901 | 7.7086 | 8.973 | 4 | COMPLETED |
| WDL g121 | quiet-endgame | 413 | 45 | 458 | 0.0982532751 | 0.108958838 | 7.7913 | 9.1015 | 4 | COMPLETED |
| WDL g121 | quiet-fianchetto | 11565 | 37459 | 49024 | 0.764095137 | 3.23899697 | 410.27855 | 424.3041 | 4 | COMPLETED |
| WDL g121 | quiet-pawn | 410 | 0 | 410 | 0 | 0 | 7.059 | 9.433 | 4 | COMPLETED |
| WDL g121 | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.3363 | 0.3904 | 1 | TERMINAL |
| WDL g121 | tactical-queen | 217 | 8 | 225 | 0.0355555556 | 0.0368663594 | 6.4925 | 6.8369 | 4 | COMPLETED |
| WDL g121 | transposition-knights | 1348 | 30 | 1378 | 0.0217706821 | 0.0222551929 | 7.91035 | 10.8723 | 4 | COMPLETED |
| NNUE g74 | check-evasion | 28 | 2 | 30 | 0.0666666667 | 0.0714285714 | 2.4251 | 2.6127 | 4 | COMPLETED |
| NNUE g74 | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.23245 | 0.2524 | 1 | TERMINAL |
| NNUE g74 | en-passant | 598 | 40 | 638 | 0.0626959248 | 0.0668896321 | 13.01295 | 15.9252 | 4 | COMPLETED |
| NNUE g74 | middlegame-kiwipete | 9135 | 32300 | 41435 | 0.77953421 | 3.53585112 | 82.50555 | 85.3744 | 4 | COMPLETED |
| NNUE g74 | opening-ruy-lopez | 11517 | 10223 | 21740 | 0.47023919 | 0.887644352 | 56.1925 | 67.9074 | 4 | COMPLETED |
| NNUE g74 | opening-start | 2217 | 126 | 2343 | 0.0537772087 | 0.0568335589 | 8.36655 | 9.0932 | 4 | COMPLETED |
| NNUE g74 | promotion-race | 467 | 474 | 941 | 0.503719447 | 1.01498929 | 10.9465 | 14.9382 | 4 | COMPLETED |
| NNUE g74 | qsearch-exchanges | 1778 | 1419 | 3197 | 0.443853613 | 0.798087739 | 9.31615 | 10.64 | 4 | COMPLETED |
| NNUE g74 | queen-endgame | 1888 | 171 | 2059 | 0.0830500243 | 0.0905720339 | 6.27585 | 6.7992 | 4 | COMPLETED |
| NNUE g74 | quiet-endgame | 300 | 16 | 316 | 0.0506329114 | 0.0533333333 | 8.64205 | 10.2123 | 4 | COMPLETED |
| NNUE g74 | quiet-fianchetto | 4865 | 3928 | 8793 | 0.446718981 | 0.807399794 | 24.1588 | 25.2563 | 4 | COMPLETED |
| NNUE g74 | quiet-pawn | 576 | 0 | 576 | 0 | 0 | 8.21915 | 12.635 | 4 | COMPLETED |
| NNUE g74 | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.3862 | 0.4997 | 1 | TERMINAL |
| NNUE g74 | tactical-queen | 219 | 10 | 229 | 0.0436681223 | 0.0456621005 | 15.39515 | 16.2089 | 4 | COMPLETED |
| NNUE g74 | transposition-knights | 1306 | 14 | 1320 | 0.0106060606 | 0.010719755 | 7.7132 | 9.6015 | 4 | COMPLETED |
