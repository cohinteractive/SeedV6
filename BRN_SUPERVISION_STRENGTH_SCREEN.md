# BRN-2 supervision strength screen

Experimental four-evaluator screen in the authoritative SeedV6 repository,
starting from `be07ff561ce8491bc629f87b53f79e1d56e96023` on 2026-09-23 UTC.
This follows the [three-arm ablation](BRN_SUPERVISION_ABLATION.md),
[five-weight sweep](BRN_SUPERVISION_WEIGHT_SWEEP.md) and
[canonical campaign diagnostics](BRN_CANONICAL_G128_DIAGNOSTICS.md).

**All 768 scored games completed. The 75% blend has the highest observed
scores, but every BRN-vs-BRN pairing remains inconclusive under the existing
95% confidence method. The 50% and 75% blends each show a clear advantage over
the pinned NNUE g74 within this short, bounded screen; teacher-only does not
clear that statistical threshold.** No model was promoted and no supervision
regime was made canonical.

## Immutable competitors

All paths were discovered from the accepted reports and cross-checked against
the actual stores before games. BRN `best.txt`, `experiment.json`, its matching
`replay.jsonl` milestone, model bytes and training-state bytes agree. These are
the final published experimental Best exports at boundary 128, not the
latest-training generation-128 Candidates. The exports have **no production
manifest or refs/best**; their experiment metadata is not presented as one.

### 50% teacher, Best g125 (`brn50`)

```text
E:\SeedV6-Networks\BRN\BRN-2\experimental-supervision-ablation-20260923\blended\boundary-128\network.brn2
model SHA-256: 570e78b8e64369ca2fa9ec102c8f5fbbb7a0f39ef205062a4cd44cc12e726935
training.state SHA-256: 36b77a6aaefcf84616ef7aa0a0f3f040ffeb46955d6790634e75057d319136c9
experiment.json SHA-256: e504d5639c5a834dcab853ed91991579f942563d3a7d760ad9632823e709b93e
```

### 75% teacher, Best g125 (`brn75`)

```text
E:\SeedV6-Networks\BRN\BRN-2\experimental-supervision-ablation-weight-sweep-20260923\weight0p75\boundary-128\network.brn2
model SHA-256: 6124771b5f83e86964aad8fa6f441dde378aca58b0aabe8541bbd9602b456e37
training.state SHA-256: 557863160233cfbeec56123dc8a5b14382b7830965039667f6c8055122a1fb74
experiment.json SHA-256: 56ae30fecf6bbbd6a12d64daf063ebaae90f436042b231a112578cb8dcdd57db
```

### 100% teacher, Best g127 (`brn100`)

```text
E:\SeedV6-Networks\BRN\BRN-2\experimental-supervision-ablation-20260923\teacher\boundary-128\network.brn2
model SHA-256: 286dd8151abe7e935c1fd8b26834171a7c08565357a4416e42582ceffb2ed1d8
training.state SHA-256: 7d92e70fdb343b9e4d78df1cf8fb89acf818431a353cf04469edfdbee8363872
experiment.json SHA-256: 105000faaa1aee914e98b368c110f96c668a0570076abba53f2766593e178cc6
```

All three are canonical BRN-2 architecture `seedv6.brn.2`, feature schema 2,
width 32. Their sibling `training.state` and `experiment.json` files are in
the same directory as the model above.

### Trained NNUE reference, Best g74 (`nnue74`)

```text
E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6\network.nnue
model SHA-256: 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training.state SHA-256: 453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
manifest.bin SHA-256: efa51cecebb088283cfd13abd610c0d3c7b10f1c7d5829addcb4f0d3a51060d5
generation: 74; optimizer step: 8942
```

The exact directory is passed to the existing `Brn2Diagnostics.load` path,
which uses `CheckpointInspection.manifest` and `CheckpointStore.inspect`.
Production integrity/schema/model/optimizer checks apply. BRN standalone
exports use the existing model reader. All four decoded model serialization
hashes must match their pins before any search. The NNUE decoded manifest
identity is checked separately before the smoke or screen starts. No store
writer, recovery, trainer or publication operation is invoked.

## Search control and game framework

`ValidationArena` and the existing research `StrengthArena` use fixed depth;
the production Candidate-vs-Best entry point also rejects architecture mixing.
The experiment therefore uses a narrow tool under `tools/search`, with no
change to those arena semantics. It calls the existing
`SearchLifecycleService` and its native timed `SearchLimits`, retaining the
production iterative-deepening, completed-result selection and legal fallback.
The board, move legality, repetition history and adjudication are owned by
the existing `HeadlessGame`; no chess or search algorithm is duplicated.

Settings were fixed before the smoke and all scored games:

| Setting | Value |
|---|---|
| Competitors | Exactly brn50, brn75, brn100, nnue74 |
| Pairings | All six unordered combinations |
| Games per pairing | 128, from 64 color-swapped opening pairs |
| Per-move budget | **50 ms wall clock**, native monotonic time control |
| Search limits | `depth=0` (no requested depth cap), `nodes=-1`, `timeMillis=50`, `infinite=false` |
| Search worker count | 1; only one game/search active at a time |
| TT | Private 1,048,576 entries per color; fresh each game, retained within that game |
| Evaluator mapping | Existing BRN fixed mapping and NNUE V1; no calibration |
| Search policy | Existing neural policy: mate-distance bounds, full iterative windows, unchanged quiescence |
| Diagnostics | Existing counters enabled equally; no qshadow |
| Maximum game length | 1,024 played plies **after** the opening |
| Move delivery watchdog | 5,000 ms for the native service result |
| External process watchdog | 45,000 seconds total; 120 seconds without JSONL progress |
| JVM | `java -Xms256m -Xmx1536m` |

Observed host: Windows 11, AMD Ryzen 5 5500 (6 cores / 12 logical processors),
Java `21+35-2513`. The `ValidationConfig` used only as the opening/statistics
carrier retains its required depth field of 4; **that field never controls a
search in this experiment**. Actual search requests use the timed limits above.

The finite 50-ms control is a short practical screen: evaluator/update cost
counts against the same clock for every competitor. This is not a fixed-depth
quality-isolation experiment. It was not tuned after looking at game results.
Normal `TIME_LIMIT` is expected, and is distinct from a failed search or hard
watchdog timeout. The native lifecycle uses the deepest completed iteration;
if none completed, it returns its established first-legal-move fallback. Every
such fallback, completed depth, total/main/q nodes, evaluation count, elapsed
time, search termination and failure detail is recorded. A failure remains
invalid even if the native result also contains a legal fallback move.

Measured move latency includes request/dispatch/result delivery, excluding
model loading, TT/player construction, game move application and JSON output.
Cooperative limits can overshoot due to scheduling, GC or checkpoint spacing;
observed tails are reported rather than treated as exact hard deadlines.
Five 50-ms starting-position warmups per actor precede each process's games,
using fresh private search objects. They are separately labeled and excluded
from game statistics. No task-owned test or second search process runs alongside
the screen. Host/JIT effects remain a limitation of timed results.

`HeadlessGame` gives mate/stalemate precedence, then applies its existing
fifty-move, formal threefold and insufficient-material rules. There is no
evaluation-score resignation/adjudication. A ply cap is an incomplete game,
**not a draw**. `ValidationResult` excludes both games of an incomplete pair
from W/D/L. The runner records and stops on an invalid pair rather than silently
replacing games, expanding the sample or promoting a model.

## Opening and scheduling method

The common initial board is `Board.startingPosition()`, with its actual initial
history. `ValidationArena.opening` selects authoritative legal moves uniformly
through `SplittableRandom`, for **exactly eight plies** (`min=max=8`). This uses
the established validation-opening mechanism; there is no external book and
no training-data generation. Fixing the opening length avoids repeated empty
openings from the ordinary 0–8 default. The seed is **20260923**.

For opening index `i=0..63`, `SelfPlayRunner.gameSeed` returns the first
`SplittableRandom(seed + 0x9E3779B97F4A7C15L * i).nextLong()`, with Java long
wraparound. The opening generator initializes its own RNG with that result
and retains its ordinary opening-length draw even though both endpoints are
eight. Legal move array order and selection are the existing arena's order.
Opening generation is independent of actors, game length and game results.

Each opening is generated twice before use and its full-state identity must
match. All 64 hashes must be unique and all openings nonterminal. The exact
same opening board **and ordered repetition history**, including rights,
en-passant and clocks, are reused for all six pairings and both colors.
`ValidationArena.stateHash` hashes six big-endian board longs, history size
as a long, then all ordered history keys as longs. Those exact board/history
arrays, indexed seeds and hashes are retained in JSONL.

Pairings are indexed (50,75), (50,100), (50,NNUE), (75,100), (75,NNUE),
(100,NNUE). For opening `i`, their execution order rotates by `i mod 6`.
Even opening indices play A as White first; odd indices play A as Black first.
This distributes ordering/warmup/load effects without changing the statistical
A/White and A/Black labels. Timed search paths need not repeat bit-for-bit;
opening identity and every outcome's pairing/color attribution remain exact.

## Statistical definitions

All W/D/L is from the first named evaluator A's perspective. Game score is
`(W + 0.5*D) / games`. The unit for uncertainty is an opening **pair**, with
pair score `(A-white game score + A-black game score) / 2`, in [0,1]. No
independence of the two color-swapped games is assumed.

The unchanged `StrengthArena.assess` uses minimum 64 valid pairs and alpha .05:
`radius = sqrt(log(2/alpha)/(2*n))`; bounds are
`[max(0, mean-radius), min(1, mean+radius)]`. At n=64 the radius is about
0.169763. It reports `CLEAR_A_ADVANTAGE` if lower > .5,
`CLEAR_B_ADVANTAGE` if upper < .5, otherwise `NO_CLEAR_ADVANTAGE`.
Insufficient pairs or any incomplete pair gives `INSUFFICIENT_EVIDENCE`.
These are the existing two-sided Hoeffding score bounds under the framework's
opening-sample assumptions, not a new fitted rating system.

The unchanged `PromotionPolicy.DEFAULT` is also reported **informationally**:
minimum 64 pairs, alpha .05, required margin zero;
`radius = sqrt(-log(alpha)/(2*n))`, un-clipped lower `mean-radius`, threshold
.5, strict lower > threshold for `PROMOTE`; otherwise `RETAIN_INCUMBENT`
once minimum n is reached. At n=64 its radius is about 0.152984.
A retain status is not evidence of equality or reverse superiority. No
formula status can promote anything in this runner, and no model is promoted.

Intervals are per pairing, without a multiple-comparison correction. Reusing
the same opening set improves comparability but correlates results across
pairings. The screen is conditional on this opening generator, seed, host,
short resource control and single accepted checkpoint per supervision regime;
it is not a population estimate across independent training campaigns.

## Complete six-pairing results

Every row has 64 valid opening pairs, 128 games, and zero incomplete pairs.
A is the first named evaluator. Intervals and conclusions are the unchanged
per-pairing `StrengthArena.assess` output at alpha .05. Decimal values below
are rounded to six places; full-precision outputs remain in `analysis.json`.
The final column applies `PromotionPolicy.DEFAULT` **informationally only**.
Its statuses caused no publication, promotion or reference mutation.

| A vs B | Games | W/D/L | A score | 95% interval | Existing conclusion | Promotion lower / informational status |
| --- | --- | --- | --- | --- | --- | --- |
| 50% vs 75% | 128 | 50/8/70 | 0.421875 | 0.252112–0.591638 | NO_CLEAR_ADVANTAGE | 0.268891 / RETAIN_INCUMBENT |
| 50% vs 100% | 128 | 70/8/50 | 0.578125 | 0.408362–0.747888 | NO_CLEAR_ADVANTAGE | 0.425141 / RETAIN_INCUMBENT |
| 50% vs NNUE | 128 | 84/7/37 | 0.683594 | 0.513831–0.853356 | CLEAR_A_ADVANTAGE | 0.530610 / PROMOTE |
| 75% vs 100% | 128 | 81/5/42 | 0.652344 | 0.482581–0.822106 | NO_CLEAR_ADVANTAGE | 0.499360 / RETAIN_INCUMBENT |
| 75% vs NNUE | 128 | 92/3/33 | 0.730469 | 0.560706–0.900231 | CLEAR_A_ADVANTAGE | 0.577485 / PROMOTE |
| 100% vs NNUE | 128 | 75/10/43 | 0.625000 | 0.455237–0.794763 | NO_CLEAR_ADVANTAGE | 0.472016 / RETAIN_INCUMBENT |

### Colors and paired-opening evidence

Each color column contains 64 games, scored from A's perspective. The five
histogram bins count pairs with A score 0, .25, .5, .75 and 1, summing to 64.
Every underlying pair, both terminations and its opening hash are retained.

| A vs B | A White W/D/L | A Black W/D/L | Pair scores 0 / .25 / .5 / .75 / 1 |
| --- | --- | --- | --- |
| 50% vs 75% | 25/4/35 | 25/4/35 | 20 / 2 / 28 / 6 / 8 |
| 50% vs 100% | 36/6/22 | 34/2/28 | 11 / 4 / 24 / 4 / 21 |
| 50% vs NNUE | 45/3/16 | 39/4/21 | 5 / 2 / 26 / 3 / 28 |
| 75% vs 100% | 42/2/20 | 39/3/22 | 7 / 0 / 28 / 5 / 24 |
| 75% vs NNUE | 50/0/14 | 42/3/19 | 3 / 0 / 27 / 3 / 31 |
| 100% vs NNUE | 40/3/21 | 35/7/22 | 6 / 2 / 29 / 8 / 19 |

### Game lengths and search cost

Lengths count played plies after the eight-ply opening; add eight for total
plies from the ordinary initial position. Search costs include both actors
and unfinished work in the final timed iteration. Fallback games count games
with at least one native no-completed-iteration fallback, not invalid games.

| A vs B | Mean plies | Median plies | Max plies | Total nodes | Search seconds | Fallback moves / games |
| --- | --- | --- | --- | --- | --- | --- |
| 50% vs 75% | 91.9766 | 79.5 | 405 | 164091469 | 584.051 | 1 / 1 |
| 50% vs 100% | 93.8594 | 78.5 | 332 | 164852802 | 595.903 | 8 / 3 |
| 50% vs NNUE | 85.8125 | 73.0 | 430 | 254806320 | 541.398 | 0 / 0 |
| 75% vs 100% | 90.7734 | 77.5 | 414 | 162092459 | 576.179 | 1 / 1 |
| 75% vs NNUE | 88.7500 | 74.0 | 435 | 264322453 | 560.396 | 1 / 1 |
| 100% vs NNUE | 93.0391 | 76.0 | 462 | 280656040 | 587.592 | 10 / 1 |

Physical-color terminations are below. Abnormal means ply cap, cancellation,
search failure or infrastructure failure; all are zero. No game was replaced
or converted from an administrative stop to a draw.

| A vs B | White mate | Black mate | Threefold | Fifty move | Insufficient | Stalemate | Abnormal |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 50% vs 75% | 60 | 60 | 6 | 1 | 1 | 0 | 0 |
| 50% vs 100% | 64 | 56 | 4 | 2 | 2 | 0 | 0 |
| 50% vs NNUE | 66 | 55 | 2 | 3 | 2 | 0 | 0 |
| 75% vs 100% | 64 | 59 | 2 | 3 | 0 | 0 | 0 |
| 75% vs NNUE | 69 | 56 | 1 | 2 | 0 | 0 | 0 |
| 100% vs NNUE | 62 | 56 | 7 | 2 | 1 | 0 | 0 |

### Search cost by evaluator

Each actor played 384 scored games (192 as each color). Searched positions and
game lengths differ, so node/depth comparisons describe these played trees,
not equal-position inference speed. NNUE's higher median completed depth and
node throughput did not by themselves predict the observed score ordering.

| Actor | Searches | Main nodes | Qnodes | Total nodes | Median nodes | Median depth | Seconds | Median / max ms | Time limit | Fallbacks |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 50% | 17397 | 172082158 | 67372510 | 239454668 | 10058 | 4 | 862.867 | 50.0292 / 85.2073 | 17086 | 1 |
| 75% | 17405 | 168531858 | 74022948 | 242554806 | 10088 | 4 | 863.361 | 50.0292 / 60.6718 | 17084 | 2 |
| 100% | 17764 | 171619340 | 77198806 | 248818146 | 10206.5 | 4.0 | 880.768 | 50.0292 / 57.5621 | 17439 | 16 |
| NNUE | 17093 | 373063899 | 186930024 | 559993923 | 31092 | 5 | 838.523 | 50.0276 / 51.2431 | 16706 | 2 |

## BRN and NNUE-anchor interpretation

Internal totals combine each BRN's two BRN opponents, with equal 128-game
weight. They are descriptive standings, not an additional independent
confidence test. NNUE columns use the same pinned anchor and opening set.

| BRN | Internal W/D/L (256 games) | Internal score | NNUE W/D/L (128 games) | NNUE score |
| --- | --- | --- | --- | --- |
| 50% | 120/16/120 | 0.500000 | 84/7/37 | 0.683594 |
| 75% | 151/13/92 | 0.615234 | 92/3/33 | 0.730469 |
| 100% | 92/13/151 | 0.384766 | 75/10/43 | 0.625000 |

1. **BRN head-to-head ordering is suggestive, not established.** The 75% model
   scores 57.8125% against 50% and 65.2344% against 100%; 50% scores 57.8125%
   against 100%. Thus the observed internal order is 75% > 50% > 100%, but
   all three two-sided intervals include 50%. The 75%-versus-100% difference
   is the largest internal signal, not a statistically confirmed win under
   the accepted method. Its informational one-sided lower bound is
   0.4993595731, just below the strict .5 threshold; the formula is not changed
   to turn that near miss into a promotion.
2. **NNUE-anchor comparisons support two bounded advantages.** The 50% blend
   scores 68.3594% (95% interval 51.3831%-85.3356%), and 75% scores 73.0469%
   (56.0706%-90.0231%). Both satisfy the existing two-sided advantage criterion.
   Teacher-only scores 62.5%, but its interval 45.5237%-79.4763% includes parity.
   The higher 75% anchor score is descriptive; these anchor comparisons do
   not establish a significant 75%-versus-50% difference.
3. **Poor 50% fixed-corpus volatility did not identify the worst player here.**
   It finishes ahead of teacher-only by observed score and clears the NNUE
   anchor criterion. It trails 75% by observed score, but that pairing is
   inconclusive. These results do not prove that local volatility is harmless
   or isolate its causal effect; they show that the static volatility ranking
   was insufficient to rank these particular models' game performance.
4. **The smoother 75% blend is the point-estimate leader.** It leads both BRN
   head-to-heads and has the highest NNUE-anchor score, but superiority over
   either BRN is not established by the existing intervals. It is a candidate
   for further discriminating evidence, not a canonical policy selection.
5. **Teacher-only's closer NNUE agreement and lower static volatility did not
   predict better play than the blends.** It has the lowest internal score
   and lowest observed BRN score against NNUE. Those BRN differences remain
   inconclusive; neither stronger teacher agreement nor better WDL prediction
   alone produces a proven total ordering. The static smoothness/agreement
   order (100%, 75%, 50%) differs from the observed game order (75%, 50%, 100%).
   The static bounded-search cost advantage of 50% likewise did not make it
   the point-estimate leader in games.
6. **No pairing was invalidated by search/game failure.** There were no stalls,
   hard watchdog expirations, failed searches, illegal moves or ply-cap games.
   Native time-limited fallbacks were uncommon and are fully reported below.
   Ordinary computational cost under the common clock is an intended part of
   this practical comparison; equal-depth quality cannot be inferred from it.
7. **Decision-critical uncertainty remains.** All three BRN-vs-BRN pairings are
   inconclusive. The smaller observed gaps are 50%-versus-75% and
   50%-versus-100%; 75%-versus-100% is a larger but still inconclusive signal.
   Choosing between the blends particularly needs stronger direct
   50%-versus-75% evidence. A targeted extension would be a separately
   authorized work unit. No extension or additional games were run.
8. **Scope of the conclusion.** Per-pair intervals are unadjusted for the six
   comparisons; they are not a simultaneous family-wise superiority claim.
   These are single-seed, short-clock games from randomized legal openings,
   with instrumented search and one checkpoint per regime from one training
   campaign. Opening distribution, host scheduling/JIT/GC, longer time controls,
   independent campaigns and other opponents can change the ordering. Beating
   this NNUE anchor here does **not** establish general engine superiority.
   No canonical supervision-policy or model-promotion decision follows.

### Runtime bounds and fallback evidence

The scored JVM ran from `2026-09-23T01:22:26.303096100Z` to
`2026-09-23T02:20:02.560010700Z`; its external process receipt measured
3,457.470078 seconds (about 57m37s), exit zero. All three verify/smoke/screen
processes exited zero, with no hard watchdog event.

The screen played **69,659 post-opening plies**, averaging 90.7018 per game
(median 76, maximum 462). It searched **1,290,821,543 nodes**: 885,297,255 main
and 405,524,288 qnodes, with 920,256,006 recorded evaluator calls. Summed measured
search-response time was **3,445.518726 seconds**. There were **68,315 normal
TIME_LIMIT results** and **1,344 COMPLETED results**, with zero search failures,
5-second delivery-watchdog timeouts or process-watchdog expirations. Maximum
response time was **85.2073 ms** and no response exceeded 100 ms; cooperative
overshoot is visible and no exact hard 50-ms deadline is claimed.

There were **21 native fallback moves in seven games**, 0.0301% of all moves:

| Pairing / opening / A color | Fallback actor and count | Actual winner |
|---|---|---|
| 75%-NNUE / 11 / Black | 75%: 1 | 75% |
| 50%-100% / 12 / White | 50%: 1 | 50% |
| 100%-NNUE / 17 / White | 100%: 8; NNUE: 2 | 100% |
| 50%-100% / 26 / Black | 100%: 5 | 50% |
| 50%-100% / 38 / Black | 100%: 2 | 100% |
| 75%-100% / 46 / White | 100%: 1 | 100% |
| 50%-75% / 57 / Black | 75%: 1 | 75% |

Indices are zero-based. These returned legal native fallbacks after the clock
expired before depth 1 completed; they were not silently counted as completed
iterations. All seven games subsequently ended in checkmate. Their move ordinals,
nodes, qnodes and outcomes are retained in `analysis.json` and `screen.jsonl`.
Teacher-only has the most such events, but the sample is too small and localized
to infer a general stall rate or a causal explanation for its lower score.

A conservative sensitivity check permits **any outcome change in each of those
seven games**, bounding each pairing's score change by affected-games/128.
Applying the unchanged two-sided formula to both ends of that envelope leaves
all six classifications unchanged. The 50%-NNUE pairing had no fallback game;
75%-NNUE had only one, insufficient to erase its observed confidence margin.
This check does not replay counterfactual games, excuse a failure, or show what
longer thinking time would do across all positions. Actual scores retain all
original games. Normal short-clock truncation and timing noise remain limitations.

## Smoke gate and targeted validation

Before the scored screen, a separate verify process successfully loaded all
four accepted models and checked their decoded hashes and NNUE manifest identity.
The smoke process again loaded all four and ran five warmup searches per actor,
then played one BRN50/NNUE74 color-swapped opening pair using the exact screen
settings. Its common opening hash was
`0eeb45e90377e54bd3219de51df4747f92b8b6f3c181053b07da7a9ca85315fa`.

Both smoke games completed by checkmate, in 127 and 94 played plies (221 total).
BRN50 won both; these **two smoke games are excluded** from the scored screen.
There were zero search failures, fallback moves, ply caps and watchdog events;
maximum observed response time was 50.2021 ms. Both colors and the persisted
opening/game/pair records were inspected. All 1,441 protected files matched
their before-smoke SHA-256, size and modification-time inventory afterward.

The focused validation command passed **56 tests in six suites**, zero
failures, errors or skips:

```powershell
.\gradlew.bat :app:test --tests '*Brn2StrengthScreenTest' --tests '*StrengthArenaTest' --tests '*ValidationArenaTest' --tests '*PromotionPolicyTest' --tests '*SearchLifecycleServiceTest' --tests '*HeadlessGameTest' --console=plain
```

The six new tests cover read-only BRN/NNUE model loading with hash/architecture
rejection; reproducible unique openings and preserved full history; color swaps
and recorded outcomes; cap exclusion rather than draw conversion; real mixed
timed searches and mate; rule draws, explicit failure recording and native
time-fallback acceptance while rejecting a failure that also supplies a move.
Existing suites cover lifecycle/time controls, board/game rules, paired arena
semantics and both statistical formulas. The wrapper's Python syntax check
also passed. Raw JUnit XML and counts are retained with the local evidence.

## Reproduction and evidence location

Run from `C:\projects\seed\java\seedv6`, after the focused build/tests above.
Use a fresh output name on repetition:

```powershell
python -B tools/brn2-strength-screen.py smoke app/build/brn2-strength-screen-NEW
python -B tools/brn2-strength-screen.py screen app/build/brn2-strength-screen-NEW
python -B tools/brn2-strength-screen.py analyze app/build/brn2-strength-screen-NEW
```

The first command pins/verifies the accepted stores and writes a checksummed
configuration, before-inventory, decoded-identity verification and smoke evidence.
The second refuses to run without a successful smoke receipt, unchanged
configuration, identical identities and unchanged protected inventories. It runs
exactly the six 128-game pairings once, with no automatic extensions. Existing
JSONL outputs are rejected. Output must be under this repository's ignored
`app/build/` tree and cannot overlap an input store. The default network root
is `E:\SeedV6-Networks`; `--networks` permits relocation only with the same pins.

This run's evidence directory is
`app/build/brn2-strength-screen-20260923/`. It contains:

- `identities.json`, `config.properties`, its checksum and the starting Git state.
- `verify.jsonl`, `smoke.jsonl`, `screen.jsonl`, process logs and watchdog receipts.
- Exact opening board/history arrays, seeds, hashes and indexed pairing/color IDs.
- A flushed pre-search record, move/result/cost record, final game state hash and
  termination, and pair record for every scored game/search.
- Original complete SHA-256/size/mtime inventories and post-process comparisons
  for canonical BRN training001, NNUE training and both accepted experiment roots.
- Targeted JUnit XML/counts and the independently checked `analysis.json`.
- An artifact SHA-256 inventory for the generated root evidence files.

The final analyzer independently recounts W/D/L and colors, requires all 768
unique games and 64 unique opening identities, verifies each opening's binary
hash, every pair's exact color swap and score, ordered move/actor attribution,
game lengths, native node accounting and the existing confidence radius.
Missing, failed or incomplete games cannot produce a successful complete report.
Raw generated logs and all model checkpoints are excluded from Git; the report
preserves the result tables if build outputs are later cleaned.

## Scope and deliberately skipped checks

No training, generation-129 Resume, training-data generation, target/calibration
change, architecture/NNUE/search change, inference optimization, BRN-3 work,
GUI change, canonical model replacement or promotion is part of this unit.
No normal Candidate-vs-Best validation or promotion formula was modified.

`fullCheck`, broad GUI/browser tests and unrelated subsystem suites were
deliberately skipped as instructed. The changed boundary is the offline runner;
the focused tests, mixed smoke and scored games exercise it directly. No browser
verification is required for this CLI-only work. No adaptive extension or
additional strength campaign is authorized or automatically launched.

Applicable supplied governance, `source/CHESS_SEARCH_CONTRACT.md`, the accepted
reports and existing match/search/validation implementation were read before
mutation. The Search canon was not revised. Exact repository-root
`CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent; neither
was created or activated.

## Final validation and provenance

The final independent analyzer passed all 768 game, 384 pair, 64 opening,
color, move-sequence and node-accounting checks, including independently
recomputed W/D/L, pair scores and the existing confidence formula. The fallback
sensitivity check passed as described above. `git diff --check` and the final
staged whitespace check passed. No required validation was skipped.

Before/after full file inventories, SHA-256, sizes and modification times match
for **948 canonical BRN training001 files, 401 NNUE training files, 55 accepted
ablation files and 37 accepted sweep files**: 1,441 unchanged files total. This
covers every selected model, training state, metadata/reference file and the
stopped generation-129 attempt/plan. No training or canonical/accepted checkpoint
was mutated, and generation 129 remains unresumed. The initial canonical,
NNUE and accepted-ablation inventories also exactly match the saved
post-weight-sweep inventories from the preceding accepted experiment.

Scored game evidence `screen.jsonl` SHA-256:
`240206d2de34246830f03f9ddc67d52190a8b2aa58f5373dfbf0e31bc0449afa`.

The focused changed files are:

- `BRN_SUPERVISION_STRENGTH_SCREEN.md`.
- `BRN_DIAGNOSTICS.md` (index link only).
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/Brn2StrengthScreen.java`.
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/Brn2StrengthScreenTest.java`.
- `tools/brn2-strength-screen.py`.

Starting Git HEAD was the accepted weight-sweep commit. Its only inherited
untracked item was `app/bin/`, which is preserved and excluded from the focused
commit. Generated evidence remains ignored. The final worktree retains only
that inherited untracked directory; the focused commit hash is supplied in
the completion response. No push, deployment, reset, rebase, amend or unrelated
cleanup occurred. This records a completed experimental work unit, not user
acceptance or production adoption.

Human actions required after this prompt: **None**.
