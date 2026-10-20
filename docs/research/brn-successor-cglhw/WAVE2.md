# Wave 2: reconcile training and isolate evaluator effects

20 October 2026. Active investigation within the existing successor contract.
No research success, production adoption or new human-training gate is declared.
The continuation request makes diagnosis of the poor W1 Arena results the immediate
priority. WAVE1 and its initial lineage manifest remain historical records.

## Established provenance

`wave2-trained-checkpoints.json` freezes Gen32 and the three Gen200 endpoints,
including network/optimizer hashes, sizes, steps, initializer evidence and observed
Best/latest pointers. GATE Best is Gen198; the Gen200 endpoint is deliberately used
to reconcile the reported Gen200 comparison, not silently selected for performance.
`BrnSuccessorAudit` uses production checksum-validating readers without a store
writer. All four complete payloads and optimizer states load; parent chains have
33/201/201/201 entries. History readers report no warnings. Ledgers and saved Arena
campaigns validate. Original stores, refs, checkpoints, corpus and paused campaigns
have not been changed.

| Fact | Frozen BRE-Pair2 Gen32 | Each W1 Gen200 |
| --- | ---: | ---: |
| Retained training positions | 28,049,408 | 26,214,400 |
| Epochs per generation | 8 | 8 |
| Sample visits | 224,395,264 | 209,715,200 |
| Batch size | 128 | 128 |
| Optimizer steps | 1,753,088 | 1,638,400 |
| Learning rate | .01 | .003 |
| Adam beta1/beta2/epsilon | .9/.999/1e-8 | .9/.999/1e-8 |
| Seed and shuffle | master seed1, shuffle true | master/initialization seed1, shuffle true |
| Source mixture | 50% BT4, 50% Lichess CP | Lichess CP only |
| Held-out source positions | 0 | 6,553,600 |
| Selection validation | Candidate vs Best game pairs | Held-out WDL error |

Gen32 generations1–8 each retained1,048,576 positions;9–30 each131,072;
31–32 each8,388,608. Its immutable initializer record says **Zero-weight
initializer**, first-run seed1, 2026-10-07T05:06:05.646291200Z. There is no recorded
teacher-model bootstrap. The complete parent chain starts at that Gen0. Later
training through Gen281 belongs to descendants and is not Gen32 exposure.
Historical source selection identity
`1cfa6dd2a74bb8f6e1f421c3fa10824ec03291cde92f0dfe293f60438f9e65f5`
is archived in the baseline's `training-data/selections`. Current source settings
are not evidence of the historical mix.

Each source supplied14,024,704 retained Gen32 training positions. BT4 consumed raw
ordinals `[0,17166401)` with3,141,697 skips; Lichess `[0,16392586)` with2,367,882
skips. Gen32 used `BRN3_SOURCE_OUTCOME_V1`: BT4 Q decoding for its explicit BT4
profile, CP/mate-to-outcome conversion for Lichess. W1 used `BRN3_CP_WDL_V1` and
stable geometry partition `brn-next-geometry-sha256-8train-1validation-1sealed-v1`.
All three consumed identical raw CP endpoints112,604,708 and matching counts.
The geometry split groups placement aliases, not entire games.

Training uses expected-score cross entropy through the smooth material-dependent
WDL link. Reported final fit/held-out errors use half-squared outcome error,
including the established rounded outcome adapter. These are distinct metrics.
WDL validation describes validation methodology; it does not make the source
actual game-result labels. Per-generation source tranches change. Falling reported
loss alone therefore does not establish improvement on a fixed independent set.

Gen32 validation recipes specify128 pairs/256 games, depth4,4 workers, opening
plies0–8, cap1024, seed1, candidate vs incumbent, alpha .05 and no required margin.
The `games=64` field belongs to unused self-play settings, not that corpus
validation's game count. The stored recipe records quarter-residual calibration.
Exact executable/source identity for each historical generation is not established
by these receipts. Current implementation arithmetic is verified separately;
do not infer that every historical search feature was already active in October7.
Selection across generations means historical promotion is not independent evidence
for a new successor's strength.

## Historical Arena evidence and the search confound

The saved user campaigns substantiate poor preliminary results, but were not
evaluator-isolated experiments:

- Ordinary stored Pair2 models use cumulative Policy4 (quiescence, quiet LMR,
  WINDOW LMP, EFFORT, QXRAY and completed TT hints for one worker).
- Ordinary W1 models use static-leaf search without that quiescence policy.
- The saved frozen-Gen32 opponents were explicitly pinned **Construction V1**.
  Most reported time matches used6 workers,1000ms/move. New experiments use1 worker.
- Fresh Arena training used common BT4 tranches but still different search
  policies. A later user campaign also tested GATE at .01 and1,048,576 positions
  per round; it does not remove the search confound.

Authoritative pair accounting (candidate W/D/L):

| Campaign suffix | Candidate | Valid W/D/L | Incomplete pairs |
| --- | --- | --- | ---: |
| 132950-24d08581 | CUBIC Gen60 | 0/0/8 | 0 |
| 134836-0ce1bca0 | CUBIC Gen100, depth4 | 1/1/6 | 0 |
| 134919-772d38c2 | CUBIC Gen100, time | 0/0/8 | 0 |
| 153837-8441bec9 | CUBIC Gen200 | 0/1/7 | 0 |
| 160033-ea25583d | GATE Gen60, paused | 0/0/2 | 3 |
| 162733-ee21a6b3 | GATE Gen200 | 0/0/8 | 0 |
| 170550-223afebd | POOL Gen200, paused | 0/1/5 | 1 |

All directories have prefix `E:/SeedV6-Networks/learning-arena/20261020-`.
Cancelled games are not draws. Incomplete pairs do not contribute W/D/L.
Training campaigns171654/172718/173430 each have11 complete rounds includingGen0:
GATE11/24/53, CUBIC13/18/57, POOL14/17/57 over88 games each. These aggregate different
checkpoints and repeated openings; they are not estimates for one fixed evaluator.
The .01 GATE campaign183014 has9 completed rounds (14/11/47), then paused round9
before its A endpoint. Never resume its substantive training autonomously.

The new runtime audit finds identical material scores for all four Gen0 models on
24 legal random-walk positions. On8 roots, identical explicit Policy4 adapters give
identical depth3 scores, moves, PVs and nodes for all four. Family-dependent
production dispatch changes nodes on every root and scores on3/8 roots despite
identical evaluation. Thus the confound is verified, not hypothetical. Its exact
share of historical losses remains unknown: those matches also differ in openings,
workers, checkpoints and budgets from the new diagnostics.

## Executed diagnostics

Plans were written before the respective new outcomes. Raw evidence is local-only
under `raw/wave2`; fresh checkouts lack it.

`audit-01`: verified frozen Gen32 network SHA
`5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`
and optimizer SHA
`f8bdda5eda0ff7893ee36c2270baaedecaf50e14ff5202ab786996f72a2788d5`;
three trained candidates; histories, parent chains, ledgers, campaign accounting;
trained optimizer-snapshot/runtime integer score parity; Gen0 common-policy parity.
Raw stores/history reconciliation also remains in `training-reconciliation.json`.

`node-screen`: eight predetermined random legal openings, reversed colours,
8,192 nodes per move, common Policy4,64MiB TT each, one worker, no ponder.
GATE9/2/5; CUBIC10/0/4 on seven valid pairs and one failed pair; POOL5/4/7.
No significance or superiority claim. One reference-side search in the CUBIC
comparison had no completed iteration. Original `.failed` trace is preserved;
its explicit SEARCH_FAILURE receipt was reconciled from that trace, not replayed
as a replacement result. The remaining unaffected games were completed.
`failure-replay-01.json` replays all33 prior moves with matching searched moves and
full opening history, reproducing NODE_LIMIT at8192 with no completed result.
A fresh-table replay does complete depth2; prior TT move ordering matters.
This is a short-budget harness limitation, not evidence of defective W1 arithmetic.
Future clock screens explicitly use the application's first-legal-move fallback,
with no invented searched score/depth and separately counted fallbacks.

`data-01`:2,048 BT4 samples from raw ordinal20,000,000 onward (stride31),
and2,048 Lichess sealed-geometry samples from20,000,000 onward (stride31 then
partition filter). Both starts exceed Gen32's consumed raw ranges. Repeated
positions across offsets/sources remain possible; no game-disjoint claim.
The Lichess bucket used here is now development evidence and cannot be reused
as independent confirmation. Original corpus/seek indexes were not changed;
seek metadata was copied into this new raw directory.

| Full raw evaluator | BT4 cross entropy | Lichess cross entropy |
| --- | ---: | ---: |
| Material | .924267 | 1.672299 |
| Gen32 | .654154 | .597866 |
| GATE | .652826 | .586806 |
| CUBIC | .674755 | .623180 |
| POOL | .697121 | .713222 |

All W1 trainer/runtime raw predictions agree exactly on4,096 samples. Uniform
GATE mixing gives CE2.245348/2.229703. This changes both learned average mixture
and context dependence; the large loss increase does not isolate those effects.
A learned-mean constant-mixture control is still required before attributing the
benefit specifically to context. Removing CUBIC's cubic head increases CE to
.757521/.840136. Removing
all interaction terms worsens every W1 model. These are post-training ablations:
co-adaptation means they are not equivalent to separately trained restricted models,
and they do not establish that expressive machinery improves playing strength.
Raw versus quarter-residual loss generally favours raw on these labels, including
Gen32; this does not justify changing search calibration without games.

`cost-01.json`: warmed256-position BT4 transition workload, seven rotated passes,
50,000 evaluations/pass: Gen321294ns, GATE2152ns, CUBIC1059ns, POOL968ns median.
Production workspaces retain their cache behaviour; this is neither a pure forced
full-refresh comparison nor search NPS. The initial raw record's “full refresh”
wording is too broad and is corrected here. Gen32 gains from rolling local-change
updates in actual search; random-position transitions understate that advantage.
Model payloads are795032/52432/104752/104752 bytes. Working-memory/search throughput
and equal-time results remain important.

## Why BRE remains a strong baseline

Verified implementation properties: direct additive table weights with fixed
features; immediate gradients from zero initialization; absolute plus shared shape
weights; both canonical roles and empty-square states;113,231 training parameters;
sparse touched-weight Adam; compiled rolling tables. W1 has6,549/13,089 parameters,
jointly learned embeddings/heads and dense Adam. These differences plausibly affect
optimization, capacity and search speed. Their individual causal effects have not
been isolated. “Pairwise is inherently weaker” is not an accepted premise.

## Completed clock screen and active frontier

`wave2-clock-screen-plan.json`: GATE and CUBIC admitted to64 games each on32
predetermined eight-move book openings from existing Search development input.
50ms/move, identical common Policy4, one worker,64MiB each, no ponder, warmed JVM;
12 games or240 seconds per invocation (finish current game), immutable game
receipts and resumable batches. All128 games completed at the fixed endpoint:

| Candidate | W/D/L | Score | Descriptive paired normal95% interval |
| --- | --- | ---: | --- |
| GATE Gen200, full recompute | 14/17/33 | 35.16% | 25.35–44.96% |
| CUBIC Gen200, full recompute | 23/11/30 | 44.53% | 33.81–55.25% |

No failed/capped pairs and no legal-move fallbacks. Independent python-chess1.11.2
replayed16,152 moves and checked every termination (`raw/wave2/clock-analysis-01.json`).
GATE regressed in this screen; CUBIC is inconclusive. Neither establishes improvement.
These descriptive intervals do not correct for selection, shared opening families
or repeated development tests. The short clock limits extrapolation.
Mean search telemetry: GATE0.701M nodes/s and depth7.41; CUBIC1.146M and8.48;
reference1.748M and8.75. Different games reach different position distributions,
so these are workload observations, not isolated speed ratios. Maximum recorded
move elapsed time was83.5ms; ordinary OS/JVM scheduling and polling contribute
overrun. One worker, no ponder,64MiB TT each,768MiB heap, BelowNormal priority,
ActiveProcessorCount1. Host AMD Ryzen5 5500,6 cores/12 logical processors, Java21.

This is a short exploratory clock screen, not the contract's independent strength
confirmation. Later confirmation must use independent openings, a prospective error
rule accounting for candidate selection, appropriate clocks and cost/ablation evidence.

Priorities now: complete clock screen; independently audit legal trajectories and
termination; investigate calibration/incremental inference if speed hides useful
evaluator strength; provide explicit fair Arena comparison if justified. Preserve
W1 and avoid speculative replacement architectures before these results.
More user training is not currently necessary. If retraining becomes necessary,
prepare a consolidated, exact GUI gate with new identities and resume evidence.

### Incremental GATE implementation experiment

`wave2-gate-cache-plan.json` was frozen after observing the original clock screen's
large GATE throughput deficit. The original screen continues using immutable
copied jars in `raw/wave2/clock-runtime/lib`; its engine/training jars predate this
change. Batch02/03/04 execution inventories record jar hashes. Batch01 did not
capture a jar inventory before execution; subsequent research-only replay additions
and a helper visibility change preceded the frozen copy. Engine/training behaviour
did not change in that interval; do not claim an independently recorded Batch01
research-jar hash.

`BrnGatedAccumulator` caches canonical piece codes for both perspectives,
rank unary/embedding sums and shape-by-rank expert sums. Dirty blocks are recomputed
from their current inputs, then combined in fixed order. No floating delta history,
model conversion, codec change, new input feature, retraining or default activation.
`SearchEvaluation.brnGatedIncrementalResearch` is explicit opt-in; ordinary model
loading retains full recomputation. Model weights and checkpoint identities remain.

Three new tests pass:9,000 random-weight legal/arbitrary transitions, bit-identical
cached versus rebuilt values, original-forward error at most1.51e-14 pawns,
special moves/undo/null perspective/empty ownership, worker independence and
common-policy depth4 search parity. Existing three-family runtime tests also pass.

`raw/wave2/gate-cache-01.json` freezes input-receipt hashes and actual Gen200 model:
6,333 positions, maximum raw error5.33e-14 pawns, zero integer-score differences;
48 paired fixed-node search comparisons (16 roots,3 passes) have identical
score/move/PV/node results. The cache uses3,312 primitive bytes per worker, plus
JVM object overhead and the existing workspace retained by the integration.
Median transition latency: full GATE1429ns, cached950ns, rolling Gen32143ns.
Summed fixed-node search time fell from2.371s to1.821s (1.30x speedup).
These are bounded host measurements, not universal timings or strength evidence.
The small summation-order difference remains a numerical limitation; universal
bit equality with the original scalar summation is not claimed.

This added inference capability is a qualifying core implementation commit:
application build42→43, with major/minor/patch unchanged. No GUI default policy,
production evaluator replacement, checkpoint update or deployment is implied.
Next evaluate the opt-in cache under equal time, and investigate calibration if
the fixed clock screen warrants it. Preserve a full-recompute control.

### Prepared calibration study

`wave2-calibration-plan.json` fixes128 exploratory games:16 development openings
and colour reversal for cached GATE at gains.25/.5/1, plus CUBIC at.5. The first
16 openings are an explicitly reused development subset, not independent evidence.
The .25 cache arm controls implementation effects; higher gains test the unverified
inheritance of Pair2's quarter-residual calibration. Original frozen Gen32 always
retains its .25 mapping. No substantive training, checkpoint writes or conversion.
Execution has begun in `raw/wave2/calibration-screen`, using the separately frozen
`raw/wave2/calibration-runtime/lib/*` and its SHA256 inventory. Do not rebuild or
substitute those jars while resuming this study.

`BrnSuccessorVariants` scales only bias/unary and output-linear weights in an
immutable copied W1 runtime model; embeddings and hidden/gate biases stay fixed.
This realizes the requested residual gain under the unchanged quarter mapper.
Three architecture-parameterized tests verify the mathematical score mapping and
preservation of source weights. A malformed test FEN (black to move with white's
EP rank) was corrected before the tests passed.
The match runner records original and effective payload hashes and distinct variant
names, including when several variants share one original checkpoint. Resume compares
JSON bindings semantically so map key iteration order cannot change model identity.
Failure traces now receive unique immutable names instead of overwriting a retry.

`raw/wave2/variant-smoke-v2` completed four deliberately capped, unscored games across
four fresh JVMs; all16 moves independently replayed. The two GATE variants retain
separate pair accounting. Quarter-gain effective payload equals the original network
hash; half-gain hash is
`a28ce147971c14bb87b6b7fcf0ea5b2d938a87e471273d96a0704d71a96addbd`.
The original smoke attempt stopped at configuration validation because random-opening
bounds were irrelevant but incompatible with its four-ply cap. Book-opening runs now
use zero random-opening bounds; old failed outputs remain. A PowerShell wrapper also
mistook an unavailable process ExitCode for failure after the first successful v2
game; the game was preserved and resumed normally. These were harness/preflight
issues, not model-training failures.

## Implementation and validation record

Initial research-only runners: `BrnSuccessorAudit`, `BrnSuccessorMatch`,
`BrnSuccessorDataProbe`, `BrnSuccessorCostProbe`, `BrnSuccessorReplay`. That diagnostic
unit changed no evaluator, trainer, default search, GUI or checkpoint format.
The later opt-in GATE cache and calibration controls are documented above.
Build uses Java21, offline Gradle with one worker and the existing migration cache.
`:research:installResearch` succeeds. Selected existing
`BrnNextMathTest`, `BrnNextPartitionTest`, `BrnNextRuntimeTest` pass, including
independent forward/finite-difference gradient checks, tiny fit, exact continuation,
codec corruption rejection, colour symmetry and search runtime checks.
Only the existing512-update/three-position test is training performed by Codex.
No substantial training or full-suite claim. No browser/GUI verification has yet
been needed for these headless research-only changes.

Development compile errors (Pair2 trainer constructor/snapshot wrapper and
CorpusPosition.toBoard argument) were corrected before successful execution.
Keep failed experiment outputs, paused user campaigns and all original models.
Root journal absent; no finalizer invoked. Starting HEAD2334760, clean, build42;
research-only work does not warrant an application build increment. No push/deploy.
