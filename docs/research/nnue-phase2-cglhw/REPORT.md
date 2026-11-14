# SeedV6 NNUE strength research — Phase 2

**Interim synthesis: research remains active.** All development experiments below
are complete. C001 passed independent confirmation; bounded robustness is next.
No Phase2 evaluator has been accepted or promoted. Production WDL25/build101 and historical CP64 are
unchanged. [STATE.md](STATE.md) records the current operation and restart boundary.

## Current finding and confirmation choice

The best development point estimate belongs to a direct128-unit network trained
for64 superbatches:25W/23D/16L against WDL25, +49.18 Elo, paired bootstrap95%
-10.86..+112.33. Its interval includes a loss. The standard256 training controls
are close enough that the development data do not establish a reliable ranking.

The smaller direct model combines a credible screen result with a substantial
measured cost reduction. It is the single candidate selected for independent
confirmation. Its network SHA256 is
`defaf21da465d6f63fc13a2299299e927029d82558d663c7bbc801e7c35e9d1c`;
the local artifact and complete qualification are identified in
[compact128-base-64-assessment.json](compact128-base-64-assessment.json).

The added nonlinear layer and pawn-pair representations improve predictive
diagnostics in several comparisons, but none of their tested formulations has
established an equal-time strength gain. Doubling training duration improves
diagnostics across the matched controls without establishing a strength benefit.
These are conditional findings about the tested recipes, not limits on all deeper
or relational NNUE designs.

## Completed development evidence

Each row is64 games on the same32 paired development openings,200ms/move,W1,
32MiB hash, CLV_C1 and frozen Java21 research runtimes. All960 games were legally
replayed and artifact-audited, with no censored outcomes. The repeated opening
pool makes these exploratory selection results. Intervals below are descriptive
paired bootstrap95%, not multiplicity-adjusted acceptance. No scores are pooled
across candidates or with confirmation.

| Formulation | Width | Superbatches | W/D/L | Elo estimate [paired95%] |
|---|---:|---:|---:|---:|
| WDL25 reproduction | 256 | 64 | 22/26/16 | +32.7 [-32.7, +100.4] |
| Teacher-only | 256 | 64 | 16/25/23 | -38.2 [-106.3, +27.2] |
| 50% outcome | 256 | 64 | 14/21/29 | -83.0 [-143.3, -21.7] |
| Narrow data | 256 | 64 | 14/23/27 | -71.6 [-137.0, -10.9] |
| Late decay | 256 | 64 | 16/29/19 | -16.3 [-77.2, +43.7] |
| Deep residual | 256+16 | 64 | 8/33/23 | -83.0 [-149.8, -27.2] |
| Pawn pairs | 256 | 64 | 12/28/24 | -65.9 [-130.7, -5.4] |
| WDL25 exact-weight kernel | 256 | unchanged | 19/20/25 | -32.7 [-77.2, +10.9] |
| Direct; C001 selected | 128 | 64 | 25/23/16 | +49.2 [-10.9, +112.3] |
| Deep residual | 128+16 | 64 | 17/26/21 | -21.7 [-83.0, +38.2] |
| Pawn pairs | 128 | 64 | 14/34/16 | -10.9 [-77.2, +60.3] |
| Direct | 128 | 128 | 21/26/17 | +21.7 [-43.7, +94.6] |
| Deep residual | 128+16 | 128 | 16/29/19 | -16.3 [-77.2, +43.7] |
| Pawn pairs | 128 | 128 | 19/28/17 | +10.9 [-60.3, +77.2] |
| Standard reproduction | 256 | 128 | 24/23/17 | +38.2 [-21.7, +106.3] |

Full endpoint identities, audits, uncertainty and observed match costs are in
[screening-summary.json](screening-summary.json) and its referenced result files.
The completed screens consumed20,371.17 observed match seconds (5.659h), including
their recorded warmup. This is experimental match accounting, not exact host
session duration.

## Training and data conclusions

A001 uses one independent initialization seed and matched base tensors to compare
the WDL25 recipe with teacher-only supervision,50% outcome supervision, narrower
data and later decay. The broad mixture and early decay improve the common
diagnostics in these conditional comparisons.50% outcome supervision is worse
both diagnostically and in its screen. Teacher-only supervision has lower
prediction loss than the25% reproduction but a worse screen score.

The post-hoc paired score contrasts favor the broad mixture and25% over50%
supervision in this sample. The teacher-only and decay contrasts remain wide.
This is not a full factorial experiment, a multi-seed effect estimate, an optimal
outcome ratio, or a decomposition of Phase1's historical+63.8 Elo. In particular,
the narrow/broad contrast changes coverage, weighting and passes per record:
the33M source receives eight passes by64 superbatches, the67M mixture four.
See [A001.md](A001.md), [a001-diagnostic-summary.json](a001-diagnostic-summary.json)
and [s001-summary.json](s001-summary.json).

B004 continues all three compact models from64 to128 with exact weights,
momentum, velocity, data order and global learning-rate continuation. A002 adds
the corresponding standard256 control. All reach eight mixture passes. The
additional native wall costs are988.99s for the compact trio and320.77s for
standard256. These are recorded native/segment wall measurements with applicable
checkpoint overhead, not GPU-active time. All64 standard optimizer transitions were independently checked
against their immediate parents. The compact last16-superbatch loss improvements
have diminished to roughly0.3–0.4%; this is not proof of convergence.

All four descriptive paired64-to128 score-difference intervals include zero.
There is consequently no established causal strength benefit from the duration
extension here. More training is not selected solely because loss still decreases.
The protocols and exact continuation evidence are [B004.md](B004.md),
[A002.md](A002.md), [duration128-summary.json](duration128-summary.json) and
[a002-summary.json](a002-summary.json).

D001 establishes separation of prepared training/validation chunks and the two
extractions, but not source games. The decoder lacks a global game identifier;
self-contained chains can still originate from repeated games. Both source
months appear in training. Older teacher labels also have the publisher's
documented FEN-only history limitation. The6753-position diagnostic holdout filters
exact/symmetric and one-piece-neighbor contamination, but remains **not verified
source-game independent**.

D002 hashes the exact67,108,864-record mixture and audits canonical STM-relative,
file-mirrored placement fingerprints. It finds65,642,886 fingerprints overall,
686,069 shared between sources, and32,478,841 additional-only fingerprints. Only
951,243 of the added33,554,432 records (2.83%) match original-source inputs. Total
duplicate records are2.18%. This materially strengthens the coverage assessment;
it does not identify independent games.63-bit fingerprints are probabilistic,
with the documented approximate collision bound, not an exact uniqueness proof.
See [d001-data-audit.json](d001-data-audit.json) and
[d002-input-overlap.json](d002-input-overlap.json).

## Architectures and inference economics

The nonlinear formulation adds a shared16-unit SCReLU residual layer and eight
material-selected residual outputs to the direct path. The relational formulation
adds4560 unordered pawn-pair inputs, activating same/adjacent-file relationships
with explicit incremental maintenance. Both were tested with dual256 and dual128
accumulators. The compact trio also includes a direct128 control. Score scale400,
QA255/QB64, eight material heads and the search configuration are preserved.
The explicit experimental formats are rejected by the production loader.

The width256 additions are expensive and lose their initial screens. Reducing
width makes depth substantially cheaper and pawn pairs closer to production's
whole-search throughput, but neither establishes a playing advantage. Stable
pawn relationships still impose update and refresh work. The separate instrumented
fixed-root runs show evaluation on about90% of nodes, so downstream work is paid
frequently. These invocation counts are workload-specific.

For the selected direct128 checkpoint64 candidate, the repeated warmed study
measures923086 mean NPS versus645837 for WDL25; the fixed legal-walk update+head
cost is about386ns with zero measured hot allocation. Its file is1,970,524 bytes.
Primitive model-array payload is1,974,544 bytes and a257-state accumulator stack
has287,840 bytes of array payload, excluding Java headers/alignment and process
RSS. Different learned evaluations change trees: the NPS ratio is **not an
intrinsic kernel speedup or an Elo estimate**.

The final matched128-superbatch cost study measures the following mean NPS:

| Evaluator | Mean NPS |
|---|---:|
| WDL25 production | 653885 |
| WDL25, exact-weight experimental kernel | 684325 |
| Direct128 | 955203 |
| Deep128+16 | 734965 |
| Pawn128 | 620493 |
| Standard256 reproduction | 655453 |

Separate JVMs retain a complete warmup pass, measure a second pass, and reverse
model order on the second repeat. All48 same-payload requests match exact nodes,
scores, depths, completion and configuration. A separate read-only comparison of
existing v3/v4 WDL25 observations matches144 requests across the runtime epochs;
this is bounded evidence, not an exhaustive or cross-platform proof. Hardware
cache/branch counters were not collected. Sources:
[compact128-cost.json](compact128-cost.json),
[compact128-128-controlled-cost.json](compact128-128-controlled-cost.json),
[runtime-v3-v4-equivalence.json](runtime-v3-v4-equivalence.json).

## Correctness, quantization and retained failures

Validation covers independent Python/Rust/Java feature extraction, native export,
GPU float tolerances, arbitrary-precision integer references, Java scalar/vector
agreement, full refresh versus incremental sums, special moves, king mirroring,
siblings/undo, null/qsearch state and worker-state transfer. Every selected model
passes259 fixture probes and a real UCI lifecycle check. Trained legal-walk
checks and measured evaluator allocation gates also pass.

Initial Java validation ran22 targeted tests, followed by focused reruns after
kernel corrections. Compact integration ran four Compact and four existing
Phase2 tests, all passing; these counts include reruns. Focused Python feature,
quantization, warmup, wall-budget and durable-output checks also passed. The full
engine suite, GUI/package release tests, Mac execution and BotLi deployment were
deliberately outside this isolated research validation.

Two material implementation findings were corrected before scored use: the
independent quantizer initially rounded an intermediate to float32 where Bullet
uses double multiplication, and JDK21's vector conversion allocated a shuffle
in the hot path. Native rounding parity was restored, and a cached shuffle removed
the allocation. A tested activation-cache revision reduced the deep head's cost.
Failed/export/allocation and slower optimization attempts remain in local raw
evidence; they are not represented as successful models.

D003 evaluates float and quantized forms of all three compact128 checkpoint128
models on all6753 diagnostic positions and matches Java quantized aggregates.
Teacher-MSE changes from quantization are+0.00001239 (direct),+0.00003673 (deep),
and-0.00001634 (pawn); ranking is unchanged. Deep's change is about0.23%, and
its lower absolute-score calibration is already present in floating inference.
This does not prove quantization irrelevant to games, but does not identify a
major predictive defect warranting quantization-aware retraining here. Full
per-position output stays under raw/; [quantization-128.json](quantization-128.json)
contains the compact analysis and limitations.

A PowerShell process-exit handoff failed after successful S001 completion; actual
driver exit0 and all eight audits were verified before recovering. No games were
repeated. An unexecuted C001 preparation driver was corrected to handle missing
Elo fields for censored summaries; both preparation versions are preserved and
all scientific parameters compare equal. The active plan is v2.

## Independent confirmation and remaining work

**C001 passed all three predeclared gates.** The selected checkpoint64 direct128
model finished 179W/204D/129L: 54.8828% score, +34.04 Elo, paired bootstrap95%
+10.86..+56.82 and normal95% +11.15..+57.22. All 512 games are uncensored;
independent legal replay and frozen-artifact audits pass. See
[c001-results.json](c001-results.json) and [c001-decision.json](c001-decision.json).
It played WDL25 on256 reserved opening pairs, with no development-score
pooling. Roots were verified legal, unique, previously unused in Phase2 matches and disjoint from
development/cost/parity fixtures. Related opening-family dependence can remain.

The frozen settings are runtime v4,200ms,W1,Hash32,CLV_C1,-Xbatch, the same fixed
warmup and an800-ply censoring ceiling. The match budget is14400 observed seconds.
There is exactly one primary confirmation attempt. Advancement requires all512
uncensored and audited games, both paired normal95% and bootstrap95% lower bounds
above50% score, and a point estimate of at least+20 Elo. No alternate candidate
or unlucky-result retest is selected. See [C001.md](C001.md) and
[c001-plan-v2.json](c001-plan-v2.json).

R001 selects bounded longer-clock and two-worker checks on separate robustness
roots. Isolated launcher/harness enablement is qualified: 82,474 actual W2
evaluations match full rebuild, including 29,784 helper-thread evaluations;
both models pass W1/W2 UCI lifecycle checks, and 16 bounded W1 requests match
exactly across v4/v5. Eight targeted Java and five Python checks pass. The only
other runtime-byte change is sourceRevision metadata inside engine-core.jar;
all its other entries match. Preparation failures remain preserved and described
in STATE.md. The two fixed follow-ups are 64 games at400ms/W1 and64 at200ms/W2,
with separate32-pair roots and a combined7200-second allowance. Results remain
pending. See [R001.md](R001.md), [r001-plan.json](r001-plan.json) and
[r001-qualification.json](r001-qualification.json).

Further architecture-specific supervision tuning, conditional subnetworks,
additional features, more seeds and longer training remain untested possibilities.
None is selected merely to pursue a favorable result. Source-game independence,
untested clocks/workers/platforms and sustained live-rating effect remain limits.
Research completion and the final recommendation await R001 and final synthesis;
completion is not owner acceptance or deployment.

## Preservation and delivery boundary

Tracked source, protocols, configurations, bounded analyses and evidence manifests
live on research/nnue-phase2. Full datasets, weights, optimizer states, profiles,
game records and per-position diagnostics remain at their recorded ignored raw
paths. A fresh checkout alone cannot reproduce an analysis without those assets;
verify local availability and hashes first. Prior Phase1 findings and all failed,
negative and frozen experiments remain preserved.

Production WDL25 and CP64 hashes and VERSION_STATE build101 remain unchanged.
There is no push, deployment or promotion. A compact-model promotion would need
separate production integration/authorization; the normal loader currently rejects
its experimental format. Threadline candidates are separately schema-validated
file publications, not observed application or acceptance; their paths/IDs and
remaining manual actions are recorded in STATE.md and publication manifests.
