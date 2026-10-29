# Build79/build80 comparison bridge

Completed exploratory bridge, 30 October 2026. Build79 remains the immutable
baseline and accepted champion; build80 is the accounting-corrected technical
control, **not an accepted champion**. Their competitive equivalence is unresolved.

## Identities and behavioral evidence

Champion `753367614b8aa7e32dee337fd76b542737141ac1` / build79 and correction
`0bfb4ea8422ef78443e309930166ad46df5cc454` / build80 are preserved. The frozen80
artifact was built at `9fec1d2af379827d105d523c32162fd771905cb9`, whose runtime
source is unchanged from correction80. The runtime79/runtime80 manifests bind
JARs, Java, evaluator and physical TT identity. [Artifact comparison](bridge-jar-difference-summary.json)
finds the expected ParallelSearch class changes, version metadata and three empty
package descriptors; shared UCI entries and other engine entries are identical.
This is artifact inspection, not behavioral-equivalence proof.

[136 diagnostic requests](bridge-diagnostics-v1-retry1-summary.json) passed legal
PV, request-limit and lifecycle checks. Nine W1 depth5 positions repeated
identically across controls. W4 results are scheduling-dependent; a stable witness
charged 14,580 nodes on79 versus14,637 on80. Equal reported node limits must not be
interpreted as equal work in affected multiworker comparisons.

The [launcher isolation check](launcher-control-v1-summary.json) then passed 39
requests on frozen80 ordinary UCI, frozen81 ordinary UCI, and frozen81 research
launcher with H1 disabled. All 11 W1 depth/node outcomes, scores and PVs matched;
W4 node accounting and legal PVs passed without asserting scheduling equality.
The candidate's shared UCI JAR is identical to80, and its core entry changes are
confined to the intended H1 classes/version ([comparison](h1-jar-difference-summary.json)).
The research launcher uses an empty book rather than a disabled bundled book and
different identity announcements; actual evaluator, Search and clock paths agree.

## Paired game evidence

Frozen V2: 32 opening pairs/color reversal per domain, 10+0.1, one active game,
ordinary UCI, CP64/mapping and flags3407871, ADAPTIVE16, existing BOUNDARY2,
Hash64 =2,097,152 slots/50,331,648 payload bytes, W1 OWNER or W4 ROOT_REUSE.
Windows/Ryzen5500/Java21, Xms256m/Xmx512m, ActiveProcessorCount4, no overlapping
heavy work. No book, tablebase or adjudication. Both domains completed 64 normal
games with strict PV/bestmove checks, independent PGN/history/request audit and
stable executable/model/opening/adapter inputs.

| Build80 versus79 | W/D/L | Score | Paired normal95 | Bounded Hoeffding95 |
| --- | ---: | ---: | ---: | ---: |
| [W1](bridge-w1-v2-summary.json) |27/12/25|51.56%|41.99–61.14%|27.55–75.57%|
| [W4](bridge-w4-v2-summary.json) |15/24/25|42.19%|33.83–50.55%|18.18–66.20%|

Neither domain establishes a difference or equivalence. W4's adverse point
estimate makes assuming correction neutrality especially inappropriate. These
are separate exploratory domains, not confirmatory discovery tests or a pooled
result. The bounded intervals emphasize the limited inference available from 32
pairs. No source-level accounting argument substitutes for these qualifications.

Process evidence is in each raw run's `process-cost.json`: W1 lifetime CPU79/80
928.70/926.20s; W4 2020.56/1978.28s. Whole-request elapsed totals were W1
922.06/922.84s and W4 962.73/962.16s. Peak working sets were about338/339MiB W1
and374/370MiB W4; these are total JVM/process footprints, not TT payload.
Different game trees, lengths and accounting prevent converting these totals
into isolated speed or strength effects.

## Failures, deviations and decision

V1 W1 at 3+0.03 stopped during game52 after 51 completed games on a strict missing-
score-info warning. Preserve `raw/bridge-w1-v1`; never treat its partial score as
a completed comparison. W4 V1 never started. Independent zero-work qualification
shows both controls truthfully return a legal fallback with no fabricated score
under a 40ms remaining clock; it does not establish the exact untraced V1 cause.

V2 W1 omitted the logger's required `engine=true` switch: runner lifecycle was
captured, not full UCI communications. Its complete strict/game/request audit is
valid with that limitation. The prospective W4 execution amendment added the
switch; full communications exist in `raw/bridge-w4-v2/uci-trace.log`. Its parent
V2 protocol hash was independently checked after completion. Logging differs
between domains, so cross-domain timing comparisons have this extra qualification.
No engine clock policy or historical BOUNDARY2 study was changed or reopened.

Decision: bridge sufficiently characterized to admit H1 exploratory comparisons
against corrected80. It does **not** license transferring a candidate's gain
against80 into a gain against champion79. Any promising candidate needs a direct
79 comparison, particularly W4, as well as separately frozen independent
confirmation appropriate to its claimed domain. The correction remains intact.
Raw completed evidence: `raw/bridge-w1-v2`, `raw/bridge-w4-v2`; protocols and
opening selection are tracked alongside this report. Research and acceptance
remain separate.
