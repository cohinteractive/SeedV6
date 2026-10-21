# Wave 5 trained assessment

21 October 2026. Existing CGLHW, contract revision2. G5 is **complete and verified**;
no programme success or production adoption is established. This root began clean
at b43b761/build55 at observed02:31:57 UTC. No root journal exists; no finalizer used.

## Verified training

All four sessions completed32 additional generations64->96 with no history warnings,
unsettled reservation or partial state. All133 retained checkpoint/optimizer pairs
(four times33 plus frozenGen32) pass checksum/codec checks and24-board integer
runtime/state parity. Exact source bootstrap payloads, compact parameter/moment
projection and full-control payload equality remain intact. Every new parent link
and8192-step increment agrees. Final steps786432(cubic),2539520(BRE). Recipes are
the prepared .001/.003, batch128,8epochs,seed1,one worker,131072 train/32768 validation.
Each adds4194304 train examples,1048576 validation examples and33554432 visits.

| Lineage | Latest | Best | Promotions |
| --- | --- | --- | --- |
| W5-RESCUBIC-R4-LR001 |96|93|17|
| W5-RESCUBIC-R16-LR001 |96|95|15|
| W5-BRE-CONTROL-LR001 |96|95|18|
| W5-BRE-CONTROL-LR003 |96|94|11|

Full IDs/hashes, ancestry, rates, steps, histories and cursor endpoints are in
[trained checkpoints](wave5-trained-checkpoints.json). Read-only Java audit evidence:
`raw/wave5/trained-audit-01`; pre-task owner inventory:`raw/wave5/trained-before-01.json`.
All four stores were copied to distinct `raw/wave5/trained-frozen-01` roots before
evaluation. Original owner models are never training or match outputs.

R4/R16 have identical inherited96 reservation ranges and hashes; the two BRE rates
also match each other. BRE versus cubic data/partition/optimizer differences remain.
R4 validation loss exceeds R16 in all32 matched generations (mean difference.00267273,
last-eight.00245233). BRE.001 beats.003 in all32 (mean difference-.00355518).
Temporal validation means rise for all four arms, but generations use different
examples; this alone cannot establish overtraining. Best is sequential comparison
against an incumbent on fresh validation, not a global minimum on a common panel.
GUI WDL loss remains half-squared target error; optimizer uses its established WDL
gradient objective. Nominal loss values must not be called playing strength.

Consumed fixed panels (2048 BT4+2048 CP) support partial R4 fitting recovery:
with half correction and normal quarter-residual mapping, second-half BT4 CE
.781416->.779777 and CP1.338620->1.331401 from64->96. R16 moves.780619->.779753
and1.330306->1.330501 respectively. Thus R4 closes most of this panel's gap, while
the fresh per-generation validation still favours R16. No independent generalization
claim: these panels have already influenced research, and historical base overlap
is unresolved. Independent Python CE reproduction error<2e-15;21 CP records have
standard-board status flags, preserved without retrospective filtering. Details:
`raw/wave5/trained-predictions-02`, `raw/wave5/prediction-reproduction-01.json`.
The first prediction invocation used a nonexistent CP filename and stopped before
prediction output; its log/directory are preserved, with a separate corrected run.

## Prospective primary strength screen

[Admission](wave5-primary-admission.json):256 fixed development games,128 each at
32768 nodes and200ms/move, one game/one worker, no ponder,64MiB TT,768MiB Java,
one active processor, BelowNormal, max12games/240seconds per resumable batch.
All eight actual Latest/Best endpoints face frozenGen32,16 games each per condition.
Calibration is the prior-established .5 learned change for every cubic/BRE candidate;
no averaging or per-candidate loss tuning. Common QXRAY search, warmed clock starts,
512-ply safety caps counted as failures, no legal fallback. Actual cache parity is
required before games. Resource/receipt wrappers preserve every completed game.

The first8 pairs of32 newly frozen development roots are used. Partition0 sampling
seed2026102105 excludes all prior W3/W4 selected positions; partition1 confirmation
remains unused. Fresh final positions do not establish independent opening families.
The remaining24 pairs are available for separately admitted focused follow-up.
The narrow `.gitignore` exception is only this frozen opening INPUT, not game output.

Complete the fixed budgets without optional favourable-score stopping. Report paired
descriptive uncertainty and failures; no final success follows from this screen.
Use equal-node and equal-time results jointly to select discriminating direct
R4/R16, continued-BRE and additive/quadratic controls. Prefer Latest for equal-exposure
controls when endpoint differences are inconclusive. No next-wave architecture or
training gate is predetermined. G5's pause sentinel is preserved as
`raw/wave5/g5-completed-pause.txt`; no additional human action is currently required.

## Trained cost, fitting controls and closed node screen

The 128-game primary node budget is complete: 16501 independently replayed legal
moves, zero failures, caps or fallback. Results: wave5-primary-node-results.json.
Latest R4/R16/BRE001 score78.125/81.25/75 percent against Gen32; their broad paired
intervals overlap. Latest BRE003 scores53.125 percent. These eight opening pairs
and eight selected endpoints establish neither superiority nor expressive value.
Best is not a consistently better chess selector. Complete the fixed clock budget.

Actual trained cubic cache/search oracles each pass6333 positions and48 search
root passes without integer or search differences. R4 cached evaluation208–222ns,
R16 290–303ns; compiled Gen32 baselines136–147ns across these runs. Workspace3753
versus4329 bytes; total immutable payload825896 versus899816 bytes. These isolated
costs are not cross-model end-to-end strength. See wave5-trained-runtime-validation.json.

On consumed panels, both latest full corrections have higher quarter-mapped CE than
their unary-only ablations at fixed half correction. Cubic degree-three helps versus
quadratic-only, but combined interactions do not beat unary fitting here. Detailed
fixed maturity/calibration results: wave5-fitting-diagnostics.json. Algebra reproduces
Java means within1e-12 using the actual compiled frozen base. The old assessment
BRE predictor uses binary64 trainer parameters before float snapshot folding; maximum
baseline discrepancy is4.52e-7 pawn. This is recorded rather than claimed exact parity.

A new bounded source diagnostic is prospectively admitted in wave5-fresh-data-plan.json.
It begins at verified cubic G5 cursor endpoints, uses reserved geometry, and reads at
most131072 raw records per source for2048 positions. Owner stores/cursors remain read-only.
The data probe now supports arbitrary model aliases and an explicit raw-record bound;
a tiny source/checkpoint integration test exercises successful collection and preserved
partial evidence on budget exhaustion. This is research tooling only, no application bump.

The match summarizer now recognizes the exact historical standalone bounded Gradle
QA receipt schema rather than misclassifying it as a match. Three targeted classifier
tests pass, including rejection of unknown/match arguments and missing worker limits.
Original receipts are unchanged. No failed game was excluded or replayed.

The primary clock runner paused at a batch boundary after22 games for the bounded
source probe/build. Its original plan/runtime and every game remain intact; resume
to128 games with new log prefixes after diagnostics, without score-dependent changes.

## Fresh reserved-source diagnostic and direct admission

The bounded probe acquired2048 rows each from BT4 ordinals29121292..29146791
and CP27458246..27478103, examining25500/19858 records. All source navigation used
verified copied indexes/prepared chunks; no owner cursor was modified and no training
occurred. New runtime and execution hashes: wave5-data-runtime.json and local
raw/wave5/fresh-probe-execution-01. One targeted Java integration test passes; no
application change/build bump. Matches retain their original frozen runtime.

Fresh evaluation-half CE at half correction: R4 BT4 .768612->.767441, CP .979018->.973753
from64->96; R16 .768296->.767723 and .969053->.970422. R4 recovered much of the gap,
but R16 remains better on CP. R4 unary CE .767286/.973325 improves both marginally;
R16 unary .766353/.976145 improves BT4 but worsens CP. Thus the consumed-panel
interaction disadvantage is not universal. Cubic degree-three improves over quadratic
on both new sources. New source ordinals/reserved geometry do not establish game/FEN
independence or exclude historical base overlap. See wave5-fresh-fitting-diagnostics.json.

Both latest compiled unary controls pass6333 placements and48 search root passes,
zero integer/search differences. Their ordinary BRE-size payload makes a practical
ablation possible without charging it for unused cubic arithmetic. These are inference
interventions, not separately optimized unary training controls.

The160-game direct protocol is now frozen in wave5-direct-admission.json: R4 versus
R16, both versus BRE001, and each versus its own compiled unary at32768nodes and200ms.
Latest96 is selected for equal exposure before primary clock closure; all scales remain
.5. Previously unused development roots9..16 are colour reversed. Finish primary clock
first, then these fixed budgets; do not extend a favourable result to seek significance.

## Degree-specific refinement admitted

Posthoc decomposition shows that R4 unary+degree3, dropping degree2, improves CE over
both full and unary on all four old/fresh source panels. For fresh evaluation halves:
BT4 .766137 versus .767441 full/.767286 unary; CP .970327 versus .973753/.973325.
R16 degree3-only also beats unary everywhere, but its full correction is better on
fresh CP (.970422 versus .973095). Therefore the all-interaction ablation can mask
opposing contributions. This is an exploratory prediction finding, not chess evidence.
See wave5-degree-diagnostics.json and tools/analyze-brn-wave5-degrees.py.

A small inference-only cubic-only variant zeroes quadratic heads while retaining
unary, embeddings and third-degree heads. It does not change owner checkpoints or
training. The64-game follow-up is frozen in wave5-cubic-only-admission.json, contingent
on targeted tests and actual score/search parity, and starts only after the existing
primary/direct budgets finish. R4 cubic-only faces full R4 and its compiled unary
control at equal nodes/time on development roots17..24. No new human training gate
is yet justified or requested. A bounded build pause preserves ongoing primary games.

The unary readiness summary was reduced to compact identity/cost/parity aggregates;
all detailed search rows remain intact in its hash-bound local raw files. No protocol
or research result changed.
