# Wave4 trained assessment

21 October2026. Continuation of the original CONTRACT, WAVE4 and STATE. No new
workstream or objective. G4 is verified complete; no further human action is
currently required. No strength improvement has yet been established.

## Verified training

`wave4-trained-checkpoints.json` binds the read-only codec audit, all six origins,
initial/latest/Best IDs and payload hashes, settings, generation metrics and source
cursor accounting. `tools/audit-brn-wave4-training.py` reproduces the structural
audit. All199 checkpoint/optimizer pairs (six times33 plus the explicit frozen
reference) pass checksum/codec checks and24-board optimizer/runtime integer parity.
All192 new history records are present, warnings empty, parent generations33..64
continuous,8192 updates each. The32-generation sessions are COMPLETED. No partial
artifact or unfinished/abandoned source reservation is present. The formerly failed
BRE-LR001 session completed its original ID/budget, without extra durable updates.
Initial32 payloads match the preserved W3 source network and Adam-state hashes.
The prior verified continuation implementation preserves Adam moments when applying
the explicit rate; this audit checks payload ancestry/counters/optimizer settings,
not a replay of all substantive training.

Each arm used4194304 training positions,1048576 held-out positions,33554432 visits,
262144 updates; batch128,8epochs,seed1,one worker,half BT4/half CP. Final steps are
524288 for residuals and2277376 for BRE. LR001 used.001, LR003 used.003; all other
Adam coefficients remain .9/.999/1e-8. The initial bootstrap's stored.003 is expected.
All64 inherited/new source reservations match exactly between rate arms within
each family, including ranges and both data hashes. Cross-family split/cursor
confounds remain; equal exposure does not establish identical architectural trials.

| Arm | Latest | Best | New promotions | .001 lower validation loss than .003 |
| --- | --- | --- | --- | --- |
| RESGATE-LR001 | 64 | 63 | 15 | 32/32 matched generations |
| RESGATE-LR003 | 64 | 64 | 10 | â€” |
| RESCUBIC-LR001 | 64 | 63 | 19 | 32/32 |
| RESCUBIC-LR003 | 64 | 63 | 15 | â€” |
| BRE-CONTROL-LR001 | 64 | 64 | 26 | 31/32 |
| BRE-CONTROL-LR003 | 64 | 64 | 23 | â€” |

Best63 contains31 additional generations,4063232 training positions and253952
additional updates, while Latest64 contains32. The equal-exposure comparison uses
Latest64; the Best comparison evaluates the GUI's practical selection rule. Do not
describe a Best63-versus-BRE64 match as exactly matched training exposure.

The higher rate reduces final training loss but worsens held-out loss. Mean matched
.001-minus-.003 validation differences are âˆ’.002206(GATE),âˆ’.004416(CUBIC),âˆ’.002815(BRE).
This supports lower-rate optimization for all families, not a residual-only effect.
The GUI WDL Loss is half-squared error on bounded outcome predictions, not cross
entropy. Best is sequential incumbent promotion on each generation's fresh holdout,
not the global minimum on a fixed validation set. Changing examples confound raw
generation-to-generation averages; they are not proof of monotonic convergence.
The trainer's minibatch gradient objective is WDL cross entropy; the displayed
final training loss and promotion loss are half-squared outcome error. Both BRE
and residual code use this distinction. BRE updates moments only at touched sparse
addresses; the residual trainer updates dense parameters. Identical Adam coefficients
therefore do not imply identical effective optimization.

## Fixed-panel diagnostics and correctness

`wave4-trained-model-plan.json` and `wave4-frozen-checkpoints.json` freeze separate
read-only copies of32/40/48/56/64 and distinct Best63 for each family/rate, plus the
explicit Gen32 reference. The first freeze also copied32 reference ancestors into
the isolated root; they remain preserved and are excluded from every model plan.
No live checkpoint or pointer was changed. The pre-audit inventory is
`raw/wave4/trained-before-01.json` (1405 live files).

`raw/wave4/learning-curve-02` assesses all34 selected models on the same consumed
2048 BT4+2048 CP positions. All residual raw trainer/runtime differences are zero.
Independent Python WDL cross entropy agrees within1.6e-15;21 CP positions have
invalid python-chess status, so valid-position sensitivity is retained separately.
These old development panels are not independent generalization or strength proof.
Full GATE-LR001 Best63 improves CP cross entropy .60082â†’.58054 from its initial32;
BRE-LR001 improves .61443â†’.59332. Cubic trajectories are more variable and Latest64
can deteriorate materially from Best63. BT4 and CP do not always rank checkpoints
alike. Additional exposure improved some predictions, without uniform convergence.

`wave4-calibration-diagnostics.json` reports unchanged-base shrinkage scales
.25/.5/.75/1. Half-change remains a sensible common development control for both
additive continuation and residual correction. It preserves Gen32, costs no extra
BRE inference, and avoids giving only expressive models a calibration intervention.
It is an inference-only transform; stored training payloads remain unchanged.

All seven distinct residual Latest/Best endpoints pass actual-model guarded-cache
checks on6333 preserved transitions and48 paired32768-node searches each: zero
integer/search differences, maximum raw discrepancy below9e-15. Raw reports are
`raw/wave4/cache-W4-*.json`. Some early timing samples overlapped the bounded Python
calibration calculation; use them for correctness, not a clean absolute cost claim.
Tournament same-root telemetry will remeasure practical cost without competing jobs.

Failed preparatory diagnostics are preserved: learning-curve-01 stopped before
prediction on a mistyped input filename; the corrected02 run succeeded. The first
structural audit asserted RETAIN instead of the actual RETAIN_INCUMBENT enum and
was corrected before producing its report. Neither was a training/model defect.

The additional W3/frozen preservation audit checks710 historical files against
the pre-repair inventory: all byte-identical. All1405 current W4 files also remain
unchanged by Codex. `wave4-preservation-check.json` records the scope. No W1 store
was written or involved in this assessment.

The prediction-ablation tool now selects frozen-base schemas instead of hardcoded
W3 display names. This is research-only instrumentation; no evaluator/trainer/search
semantics change and no application build bump. `:research:installResearch` and
actual W4 Best assessment pass. Seven existing independent match-analyzer tests pass.
`wave4-best-ablation-prediction.json` binds the resulting jar hashes and metrics;
original matches continue using the previously frozen build51 runtime.

On the second-half development panels, GATE-LR001 Best63 CE is .62917(BT4)/.58054(CP),
versus .63693/.60639 with label-independent constant gates. CUBIC-LR001 Best63 is
.62998/.61443, versus .63045/.62699 without the cubic term. LR003 shows the same
direction for these ablations. Nonlinear components contribute to these trained
functions; removing jointly trained components is not a retrained additive baseline
and does not establish expressive chess value. Original CP validity caveats remain.

Orchestration notes: a wrapper after node batch03 tested PowerShell's unset
LASTEXITCODE despite Start-Process's successful exit receipt. Admission paused;
the verified successful batch was preserved and ordinary resume continued at04,
using the explicit process receipt. No game was repeated/discarded. Git's default
whitespace check flags intentional Windows CRLF in newly frozen JSON/PGN inputs;
the explicit `core.whitespace=cr-at-eol` check passes. Preserve frozen input bytes
and their hashes rather than normalizing an already admitted protocol.

## Prospective development assessment admission

Admitted before observing W4 game outcomes:

1. Frozen Gen32 versus all nine distinct Latest/Best endpoints at32768 nodes,
   sixteen paired openings each:288 games. Uniform half-change calibration;
   explicit guarded GATE/CUBIC delta caches; exact common QXRAY/completed-hints
   search. Compare rate arms and Latest/Best on the same openings. No deletion of
   adverse, failed or capped games. Caps/errors stop affected admission for diagnosis.
2. Frozen Gen32 versus the six pre-existing GUI Best endpoints at100ms/move,
   the same sixteen paired openings each:192 games. This selection uses the GUI
   promotion record, not primary-match winners. Both continued BRE rates included.
   Legal first-iteration fallback is explicit and counted. Equal-time is essential;
   short time controls are development diagnostics, not final practical acceptance.

The32 predetermined fresh development openings are drawn from partition0 with
seed2026102104, excluding both prior W3 selections. The first16 serve this screen;
the second16 remain available for subsequent development diagnostics. Partition1
is untouched. Final positions are unique but shared early prefixes can correlate
opening pairs. Report paired scores/uncertainty descriptively, no multiplicity-free
significance, success claim or optional favorable-result stop. Close each admitted
fixed budget irrespective of direction unless correctness/resource failure intervenes.

After this screen, select a focused common-control comparison against both continued
BRE arms, with full/half calibration and nonlinear ablation where evidence warrants.
Do not infer architectural benefit from beating Gen32 when additive continuation
achieves comparable gains. Do not reject expression from an initial equal-time
loss alone. Any confirmation requires a separate prospective independent protocol.

All jobs use frozen build51 jars, one game,one worker/engine,no ponder,64MiB TT,
768MiB JVM,ActiveProcessorCount1,BelowNormal, batches at most12 games/240seconds
(finish current game). No substantive autonomous training. Inspect each batch before
admitting its successor. Exact model/plans/runtime hashes and receipts are retained.

## Primary fixed-node results (closed)

All288 admitted games completed in24 bounded batches, no caps/failures/missing pairs.
Independent python-chess replay verified35656 moves and every termination.
`wave4-primary-node-results.json` binds full local analysis and paired contrasts.
All candidates here use half-change; sixteen colour-reversed opening pairs each.

| Candidate | W/D/L | Score | Descriptive paired95% interval |
| --- | --- | --- | --- |
| GATE-LR001 Best63 | 15/5/12 | 54.69% | 36.67â€“72.70% |
| GATE-LR001 Latest64 | 12/7/13 | 48.44% | 34.67â€“62.20% |
| GATE-LR00364 | 15/6/11 | 56.25% | 41.76â€“70.74% |
| CUBIC-LR001 Best63 | 19/6/7 | 68.75% | 53.58â€“83.92% |
| CUBIC-LR001 Latest64 | 20/2/10 | 65.63% | 50.21â€“81.04% |
| CUBIC-LR003 Best63 | 18/1/13 | 57.81% | 41.24â€“74.38% |
| CUBIC-LR003 Latest64 | 19/6/7 | 68.75% | 54.26â€“83.24% |
| BRE-LR00164 | 10/6/16 | 40.63% | 25.21â€“56.04% |
| BRE-LR00364 | 20/4/8 | 68.75% | 52.94â€“84.56% |

Cubic's encouraging equal-node point estimates do not separate it from the additive
LR003 control. The better validation rate does not select the stronger BRE in this
small chess screen. Within the same roots, BRE001 minus BRE003 is âˆ’28.125 percentage
points (unadjusted paired intervalâˆ’49.05..âˆ’7.20); selection/multiplicity/opening
dependence prevent a definitive rate-strength claim. All residual-minus-BRE003
contrast intervals include zero. The report also includes illustrative simultaneous
Bonferroni-normal intervals; none is an independent programme acceptance test.

Fixed-node launches do not perform the timed runner's JIT warmup. Batch-start model
positions exhibit severely depressed apparent NPS (for example GATE001 Best63.182
of baseline versus its Latest64.640). These are cold-launch timing artifacts, not
valid warmed model-cost differences. Node budgets/legality remain intact; do not
use this screen's latency ratios as practical throughput. The timed protocol warms
all models before its budget and will provide separately qualified cost evidence.

## Endpoint-stability diagnostic and inference-only averaging

Nonmonotonic learning curves and distinct Best/latest motivated a predetermined
uniform last-eight window57..64 for **all six arms**, before inspecting primary
match outcomes. No window/rate was fitted to chess results. The plan is
`wave4-average-diagnostic-plan.json`;48 source checkpoint pairs were copied and
hash-verified into `raw/wave4/average-inputs-01`. No owner files were changed.

`BrnCheckpointAverageProbe` compares latest, parameter average and functional
ensemble on the same consumed development panels, both full and half change.
`wave4-average-diagnostic-results.json` binds provenance, parameter/function mean
differences, independent CE reproduction and validity sensitivity. Parameter means
retain one ordinary model's size/runtime; the eight-model functional ensemble is
only a diagnostic oracle. No optimizer is averaged, reset or exported, and these
inference variants are not new trainable checkpoints or continuation parents.

All four residual parameter means improve BT4 and CP full-prediction CE versus
their Latest64. Full GATE001 mean gives .62752/.57893 and GATE003 .62871/.57601;
CUBIC001 .62750/.59950 and CUBIC003 .63030/.59592. BRE means improve BT4, while
BRE001 CP worsens (.59332â†’.59649); BRE003 CP improves (.60370â†’.59627).
Thus smoothing is useful enough to test, not an expressive-only improvement.
For BRE, compiled-table averaging agrees with the independent functional ensemble
within1.8e-15. Nonlinear parameter means are different functions: maximum raw
discrepancy reaches.625 pawns for CUBIC003; their actual measured losses, not ensemble
equivalence, govern. Independent Python CE agrees within1.4e-15;21 invalid CP rows
remain explicitly stratified. No new generalization/strength claim follows.

Research match loading now supports an explicit eight-checkpoint mean with exact
ingredient hashes, consecutive parent/generation/update checks and latest-anchor
identity. Effective network hash and every ingredient are retained in bindings.
Existing plans without averaging retain their semantics. Tests verify additive
function equivalence, unchanged runtime storage, frozen-base preservation, schema
and identity failures;12 targeted JUnit cases pass. All six actual averaged models
loaded and played96 independently verified legal smoke plies. Twelve deliberate
eight-ply caps are excluded from strength and preserved, never converted to draws.
The first diagnostic build used an int for the long generation field; compilation
rejected it before execution and the corrected build/tests passed. Reports/logs
are retained. No application distribution changes or build increment.

Implementation33535b4 and `wave4-average-runtime.json` now freeze all averaging
runtime jars. The first diagnostic's jar was not retained before a later build;
its identity was explicitly left unknown. A second run under the frozen runtime
reproduces every structured metric and binding exactly (`wave4-average-reproduction.json`).
No historical runtime identity was reconstructed. Eight actual residual means,
both rates/full-and-half, then pass6333-position and48 paired-search oracles each:
zero integer/search differences, maximum raw error1.1e-14. This resolves averaging
readiness for subsequent development games, not chess superiority. Full results
and clean balanced timing are in `wave4-average-validation.json`.

## Bounded cubic-rank investigation

Cubic's equal-node results and persistent inference cost motivate reducing active
polynomial components before any new substantial training. The fixed diagnostic
`wave4-cubic-rank-diagnostic-plan.json` ranks each of16 components by its removal's
full-prediction CE effect on the first1024 positions/source, then evaluates retained
4/8/12/16 sets on the second halves, without reranking. Both rate arms' eight-model
means are included. This is reused development selection, not independent evidence.

`wave4-cubic-rank-results.json` and `wave4-rank-runtime.json` bind actual outputs,
the source delta and runtime jars before execution. Full16 exactly reproduces the
averaged input's payload hash and loss. Independent Python means agree within1.5e-15;
original CP invalid-position sensitivity remains. LR001 full rank4 CE is
.62457/.59877 versus rank16 .62750/.59950. LR003 full rank12 is .62924/.58756 versus
rank16 .63030/.59592. At half scale, rank4 improves BT4 but degrades CP for both rates.
This supports a measured cost/quality tradeoff, not blanket harmless pruning.

Zeroing heads alone does not make the current evaluator cheaper. The isolated
research sparse-rank prototype preserves the period64 rebuild and rounding guard,
and retains all ranks if its numerical bound is nonfinite. All eight actual masked
models pass forward/reverse/forward checks on6333 positions: zero integer
mismatches, maximum raw error below5e-15. Reconstructed masked payload hashes match
the prior rank diagnostic. `wave4-sparse-cubic-results.json` binds the frozen
runtime and balanced transition timings. Rank4 saves17?19%, rank8 about9%; rank12
is unchanged/slower and rank16 is20?23% slower. Whole-search benefits would be
smaller. This limited result does not justify production integration without
pruned chess quality evidence; defer it behind direct architecture/control tests.
No core evaluator, codec, optimizer or ordinary application path changed.

Core jar hashes across research builds differ because generated application-version
properties record the source identity; an entry comparison found no changed core
class bytes. Exact jar hashes remain separately bound; no runtime substitution is
allowed within an active protocol.

## Direct controls and stabilization: prospective admission

Before analyzing the primary-clock scores, `wave4-direct-admission.json` admits a
fixed448-game development follow-up:192 half-change equal-node,192 half-change
100ms, and64 full-correction equal-node calibration games. Four residual means
(all architecture/rate arms) face **both actual continued BRE Gen64 endpoints**,
with each control's own mean and frozen Gen32 included as candidates. Thus an
expressive candidate cannot obtain an apparent advantage just by replacing the
control with a weaker averaged control. Four full residual means also face the
same LR003 half-change endpoint on the same roots, allowing full/half comparison.

Eight previously unused frozen development roots17..24 are paired and shared across
these tests. This is a diagnostic selection screen with wide uncertainty, not final
confirmation. Root reuse, early-prefix dependence and multiple comparisons remain
explicit. No candidate was removed for an initial time defeat or picked solely for
validation loss. No new substantive training or source checkpoint writes occur.
The frozen averaging runtime is required; complete fixed budgets unless correctness
or resource problems stop them. Primary-clock must complete and be validated first.
Plans, exact models/ingredients and limits are recorded in the admission manifest.
Remaining development roots25..32 and confirmation partition1 are still unused.
