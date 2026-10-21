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
| RESGATE-LR003 | 64 | 64 | 10 | — |
| RESCUBIC-LR001 | 64 | 63 | 19 | 32/32 |
| RESCUBIC-LR003 | 64 | 63 | 15 | — |
| BRE-CONTROL-LR001 | 64 | 64 | 26 | 31/32 |
| BRE-CONTROL-LR003 | 64 | 64 | 23 | — |

Best63 contains31 additional generations,4063232 training positions and253952
additional updates, while Latest64 contains32. The equal-exposure comparison uses
Latest64; the Best comparison evaluates the GUI's practical selection rule. Do not
describe a Best63-versus-BRE64 match as exactly matched training exposure.

The higher rate reduces final training loss but worsens held-out loss. Mean matched
.001-minus-.003 validation differences are −.002206(GATE),−.004416(CUBIC),−.002815(BRE).
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
Full GATE-LR001 Best63 improves CP cross entropy .60082→.58054 from its initial32;
BRE-LR001 improves .61443→.59332. Cubic trajectories are more variable and Latest64
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
| GATE-LR001 Best63 | 15/5/12 | 54.69% | 36.67–72.70% |
| GATE-LR001 Latest64 | 12/7/13 | 48.44% | 34.67–62.20% |
| GATE-LR00364 | 15/6/11 | 56.25% | 41.76–70.74% |
| CUBIC-LR001 Best63 | 19/6/7 | 68.75% | 53.58–83.92% |
| CUBIC-LR001 Latest64 | 20/2/10 | 65.63% | 50.21–81.04% |
| CUBIC-LR003 Best63 | 18/1/13 | 57.81% | 41.24–74.38% |
| CUBIC-LR003 Latest64 | 19/6/7 | 68.75% | 54.26–83.24% |
| BRE-LR00164 | 10/6/16 | 40.63% | 25.21–56.04% |
| BRE-LR00364 | 20/4/8 | 68.75% | 52.94–84.56% |

Cubic's encouraging equal-node point estimates do not separate it from the additive
LR003 control. The better validation rate does not select the stronger BRE in this
small chess screen. Within the same roots, BRE001 minus BRE003 is −28.125 percentage
points (unadjusted paired interval−49.05..−7.20); selection/multiplicity/opening
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
BRE001 CP worsens (.59332→.59649); BRE003 CP improves (.60370→.59627).
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
runtime and balanced transition timings. Rank4 saves17-19%, rank8 about9%; rank12
is unchanged/slower and rank16 is20-23% slower. Whole-search benefits would be
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

The standard-chess validity sensitivity was investigated further without changing
any data. Nineteen of21 flagged CP positions pass python-chess status in Chess960
mode; only two excess-pawn/piece diagrams remain invalid in both modes. This
supports a variant/notation explanation for most flags, not proven source history
or legal reachability. Static features ignore castling rights. The original
standard-status stratification and all predictions remain preserved. Exact rows and
flags: `wave4-cp-position-status.json`.

A fixed-window maturity diagnostic is also admitted: W3 generations25..32 versus
W4 generations57..64, reducing endpoint noise at both ages. It reuses the frozen
averaging implementation and consumed prediction panels; no training or source
writes. Exact24 historical ingredients and scope are in the maturity plan/admission.

Maturity diagnostic completed with actual generations25..32 verified from manifests
and consecutive ancestry/steps. Independent Python CE agrees within1.9e-15; the
previous ablation-schema report reproduces exactly after extending the analyzer to
recognize explicitly named averaging predictors. `wave4-maturity-average-results.json`
records all comparisons. GATE and BRE full/half means improve both panels at both
rates. CUBIC full CP worsens from.58664 to.59950(.001)/.59592(.003), while half CP
improves from.58793 to.58565/.58687. CUBIC LR003 BT4 also worsens slightly. Thus
more exposure is not uniformly beneficial after removing endpoint noise; calibration,
optimization and source-distribution effects remain credible. Do not use generation
count alone to prescribe another maturity run.

A packed active-component prototype tests the indirect-layout hypothesis while
leaving all engine code untouched. Eight masked model oracles pass again. Within-run
packed/dense ratios are.70-.75(rank4),.85(rank8),.91-.92(rank12),about1(rank16).
However dense baselines vary between runs and rank4 absolute cost resembles the
first sparse prototype; no causal cross-run packing gain is established. Packed
copies add about24-96KiB beside the retained model. See the packed plan/results and
runtime. A same-run three-layout test is needed before selecting an implementation;
that work is deferred until pruned quality justifies integration. No optimizer,
serialized model, core search path or GUI distribution was changed.

## Primary equal-time screen closed

`wave4-primary-clock-results.json` closes the fixed192 games with26798 independently
replayed legal moves. No failures, ply caps, incomplete/missing pairs or legal
fallbacks. All16 bounded process receipts verify the same frozen original runtime,
one worker/game, no ponder and BelowNormal priority; maximum sampled working set
581MiB under the768MiB heap. No protocol extension or favorable-score stopping.

All candidates below use half-change around frozen Gen32, with preselected GUI Best.

| Candidate | W/D/L | Score | Descriptive paired95% interval | Median same-root NPS / Gen32 |
| --- | --- | --- | --- | --- |
| W4-RESGATE-LR001-g63 | 10/8/14 | 43.75% | 26.14-61.36% | 0.609 |
| W4-RESGATE-LR003-g64 | 12/7/13 | 48.44% | 34.67-62.20% | 0.617 |
| W4-RESCUBIC-LR001-g63 | 15/5/12 | 54.69% | 43.53-65.84% | 0.746 |
| W4-RESCUBIC-LR003-g63 | 15/6/11 | 56.25% | 40.44-72.06% | 0.706 |
| W4-BRE-CONTROL-LR001-g64 | 12/6/14 | 46.88% | 29.62-64.13% | 0.988 |
| W4-BRE-CONTROL-LR003-g64 | 17/4/11 | 59.38% | 44.62-74.13% | 0.974 |

Cubic retains encouraging point estimates under time, with substantial uncertainty;
the faster BRE003 control is also encouraging and higher in this screen. Every
interval includes50%. No architecture separation or superiority is established.
The warmed same-opening measurements support a continuing throughput disadvantage,
not the stale cold-node timing ratios. Evaluator-dependent trees still differ, so
these are practical search observations, not a pure instruction-latency experiment.
Do not pool node and clock scores or reuse these roots as independent confirmation.

The admitted direct448-game programme is now active, starting with equal-node
comparisons against BRE001. Both trained endpoint controls remain mandatory.
A separate64-game rank4-versus-identical-rank16 function test is admitted at full
and half scale on development roots17..32; exact plans and dependencies are in
`wave4-pruned-admission.json`. This selection follows the fit-only mask and cost
hypothesis, not pruning chess results. Runtime/loading support preserves all
originals, binds retained components and effective hashes, and uses the existing
dense cache. Thirteen targeted tests pass; both actual masked models pass6333-position
and48 paired-search oracles with zero integer/search differences. Full masked
payload hash exactly matches the earlier prediction diagnostic. No speed claim is
attached to these games, and no production path has changed. The frozen mask runtime
is distinct from the unchanged averaging runtime used by the448 direct games.

The new compact summarizer verifies analysis/receipt/plan/model/runtime hashes and
all bounded process records before closure. Its regression reproduced the completed
288-game node accounting (35656 moves,24 process receipts). It does not infer
selection-adjusted significance or acceptance. Original reports and raw evidence
remain preserved. The earlier accidental UTF-8 display conversion in this Markdown
was restored from the preceding committed bytes; research numbers were unchanged.

## Direct half-change node screens closed

Both fixed96-game control screens complete with no failures/caps/incomplete pairs;
12992 moves against BRE001 and13259 against BRE003 were independently replayed.
All candidates here are fixed-window means at half change, except frozen Gen32.
Each entry has only eight colour-reversed opening pairs; these are diagnostic scores.

| Candidate | Score vs BRE001 endpoint | Score vs BRE003 endpoint |
| --- | --- | --- |
| W4-RESGATE-LR001-average8-half | 62.50% | 65.62% |
| W4-RESGATE-LR003-average8-half | 59.38% | 43.75% |
| W4-RESCUBIC-LR001-average8-half | 75.00% | 56.25% |
| W4-RESCUBIC-LR003-average8-half | 40.62% | 43.75% |
| Same-rate BRE mean | 68.75% | 34.375% |
| Frozen Gen32 | 18.75% | 46.875% |

Averaging is not universally beneficial: the low-rate additive mean improves its
endpoint comparison, while the high-rate mean loses to its endpoint here. Both
residual low-rate means have encouraging node point estimates, but there is no
credible expressive separation. Normal intervals are descriptive; the compact
summaries also provide conservative bounded-pair Hoeffding intervals and a within-
plan union bound. These require independent opening-pair units, still unverified;
they do not account for all prior candidate selection or protocol repetitions.
The old primary reports are preserved. No final significance claim follows.

The change in BRE001 versus Gen32 (40.625% on the primary16 roots,81.25% on the
new8 roots) prompted a focused integrity check before further interpretation.
Effective model hashes match. Four original games, with reference/candidate roles
reversed under the averaging loader, reproduce all595 moves, scores, depths, nodes,
FENs and terminations exactly; only wall timing and labels differ. The orientation
replay plan/results preserve this check and are excluded from new strength counts.
No orientation/loading defect was found; opening selection remains a material
source of small-screen variation. Do not pool the screens into a confirmation claim.

To protect restartability during long bounded programmes, the existing PowerShell
launcher now holds an exclusive worktree lease and refuses a live BRN match process,
including one orphaned by a shell interruption. A safe nonexistent-plan check was
rejected before plan loading while the same sole runner stayed alive. A later normal
batch completed under the guard with its launcher hash recorded. Exact evidence:
`wave4-runner-guard-validation.json`. Engine jars, arguments and frozen protocols
are unchanged. Older process receipts without this new field remain unchanged.

Next: finish the64 full-scale node comparisons, the64 pruned function comparisons,
and both96-game direct clock plans. Interpret full/half, smoothing and component
value jointly with both additive controls and measured costs before selecting the
next implementation/training question. No further training gate is yet admitted.


## Full-scale and pruning controls; hybrid feasibility

The64 full-scale direct node games against BRE003 half are closed, with8837 legal
moves independently replayed and no failure/cap. GATE001 scores62.5%,GATE00359.375%,
CUBIC00143.75%,CUBIC00365.625%. On the same eight opening pairs, full-minus-half
changes are -3.125,+15.625,-12.5,+21.875 percentage points respectively. These
selected small comparisons do not establish superiority, but reject treating the
half-scale LR003 ranking as calibration-independent architectural evidence.
`wave4-direct-scale-contrasts.json` records paired uncertainty. Before any direct
clock outcomes were examined, a fixed32-game full-clock screen of GATE003 and
CUBIC003 against BRE003 half was admitted in `wave4-direct-full-clock-admission.json`.
It follows the original two96-game half-clock plans under the same frozen averaging
runtime. Selection is explicit; it is not independent confirmation.

The64 rank4-versus-rank16 node games are also closed: full scale9/7/16 (39.0625%,
paired normal95%27.25-50.87), half scale13/7/12 (51.5625%,39.34-63.79). All8250 moves
replay legally, without failures/caps. Full prediction improvement did not preserve
the full-scale chess point score. Half pruning remains uncertain, not proven
noninferior. These games use the dense verified cache and establish no speed gain.
Sparse/packed integration is deferred; any further cost choice requires a balanced
same-run layout comparison and must retain the function mask identity.

`wave4-persisted-training-cost.json` extracts the existing counters, not new training.
Across32 generations, recorded training-time medians are8.95s for both GATE arms,
7.22/7.33s for CUBIC001/003 and10.16/10.61s for BRE001/003. Summed generation elapsed
counters are302.0/300.4,244.8/244.7 and350.7/357.9s respectively. Host conditions and
acquisition/shuffling differ; these are not controlled CPU costs, and exclude
unpersisted failed-attempt time and pauses. Inference speed and training speed must
not be conflated.

The next discriminating hypothesis preserves continued additive learning alongside
the existing correction: Gen32 +.5*(BRE64-Gen32) + beta*(residual mean-Gen32).
The fixed beta grid0/.25/.5/1 and both additive-only mean controls were recorded
before calculation in `wave4-hybrid-feasibility-plan.json`. Each source's first1024
rows select beta; the remaining1024 report prediction separately. All panels are
already consumed development data. All eight residual/base combinations select.5.
All four on BRE001 improve both sources versus both the base alone and the
standalone half residual. GATE001/003 on BRE003 also improve both; CUBIC on BRE003
trades improved BT4 for worse CP. Additive-only mean mixtures do not give the same
consistent cross-source benefit. This supports complementarity, not chess strength.

The initial algebra report is preserved; the separately extended
`wave4-hybrid-feasibility-verified.json` reproduces Java CE to1.22e-15 and adds
standalone comparisons without changing selection. No model or optimizer is exported.
An explicit one-base-plus-correction research evaluator is being implemented;
ordinary codecs, training and production defaults remain unchanged. Nineteen core
tests pass, including six new hybrid composition, transition, rounding-boundary,
zero-correction and search cases. Actual-component cache/search/cost and prediction
oracles, loader tests and strength admission remain pending. Do not treat a
correction codec hash as the identity of the combined function: bind both component
payloads, scales and mapping. No new human training gate is justified yet.


One remaining control gap is now explicitly admitted: full-versus-half continuation
for both actual BRE Gen64 endpoints,16 games/rate at nodes and16 at time (64 total).
The observed residual calibration reversals make it unsafe to assume the earlier
W3 preference or W4 prediction loss identifies the strongest BRE calibration.
`wave4-bre-scale-admission.json` fixes these small development comparisons before
any full-scale W4 BRE results. They follow the original clock protocols and use the
unchanged averaging runtime; no new architecture or training is involved. They
must inform later hybrid comparisons, so a weakly calibrated additive control is
not mistaken for evidence of expressive value.


The direct LR001 clock plan is CLOSED:96 games,13113 independently replayed legal
moves, no failures/caps/fallbacks. Scores versus the BRE001 half endpoint are
GATE00121.875%,GATE00343.75%,CUBIC00146.875%,CUBIC00331.25%,BRE001 mean53.125%,
Gen3225%. The corresponding low-rate residual node scores were62.5/75%.
This strengthens the inference-cost concern without proving the expressive signal
is useless. Only eight pairs per comparison, opening sensitivity and selection
remain material. Finish the other fixed protocols; no favourable stopping or
post-hoc confirmation follows. Exact evidence: wave4-direct-clock-LR001-results.json.


Hybrid inference implementation is now unit-verified (35 distinct targeted cases;
three loader cases rechecked after making the base scale mandatory). It composes
one immutable Pair2 table and an unchanged correction layout, through canonical
and guarded delta paths. Ordinary loading/codecs/optimizers/default dispatch are
unchanged. Research receipts bind base and correction hashes to a distinct function
identity. The launcher rejects a hybrid plan with an older runtime lacking that
capability, before starting an engine; the negative test created no output/process.
Exact evidence and the initial PowerShell stderr-handling limitation are recorded
in wave4-hybrid-implementation-validation.json. This core addition warrants build52;
actual trained-model oracles/cost and legal-play readiness still precede any hybrid
strength admission. No GUI availability or native package verification is inferred.


## Actual hybrid readiness

Frozen build52 runtime583310e reproduces all eight hybrid predictions and two
zero-correction controls on4096 rows/model. Maximum discrepancy from the separately
frozen component algebra is3.56e-15; independent CE agrees within1.23e-15.
Every composition passes6333-position full/delta and fresh-cache integer oracles,
with maximum raw discrepancy5.33e-15, plus48 paired32768-node searches. The two
zero-correction controls additionally reproduce their actual continued BRE base
searches exactly. No source checkpoint, optimizer or codec is changed.

Within-run transition ratios for hybrid versus the corresponding Gen32-based
residual range0.978..1.055. This avoids a second evaluator call; it does not prove
zero overhead, and it retains the residual's substantial cost relative to ordinary
BRE. Candidate primitive memory remains852080 bytes for GATE and904400 for CUBIC,
with6341/4329-byte workspaces, excluding headers/loading temporaries and the shared
resident Gen32 construction resource. Model-component hashes and function identity
are explicit in wave4-hybrid-readiness.json and its frozen runtime manifest.

The first1024-node legal smoke stopped without a completed iteration on a tactical
position in its third game; two prior intentional8-ply caps and the failed attempt
remain preserved. A separate plan uses the already verified32768-node budget:
all16 eight-ply games replay independently (128 legal plies), with matching hybrid
bindings and16 intentional administrative caps. No fallback or engine correction
was introduced. All smoke receipts, including the failed setup, are excluded from
strength statistics. This is readiness for development games, not strength or GUI
adoption. A new strength admission must remain distinct from the unchanged
build51 clocks and BRE calibration protocols still being completed.


The large node/time gap has a second confound:100ms searches do more absolute work
than32768 nodes. A fixed32-game sensitivity test now compares the same low-rate
GATE/CUBIC half means against the same BRE001 half control at131072 nodes, with
unchanged common search and the original eight pairs. It follows the existing
clock/calibration screens. This can distinguish a retained deeper equal-node
advantage from deterioration with search depth; it cannot identify the exact
search mechanism or establish confirmation. See wave4-depth-admission.json.
The per-launch240-second, one-worker resource envelope is unchanged.


Hybrid interpretation must also account for training history. A continued BRE base
plus a separately trained correction combines two post-reference optimization
streams, while a single BRE endpoint represents one. Each component's full ancestry
remains bound; their examples are not proven disjoint, and the BRE versus residual
data partitions differ. Existing additive-only mean mixtures provide a useful
control but do not fully match this cross-family data diversity. Any hybrid gain
would therefore be practical evidence for the composed function, not a clean causal
estimate of architecture alone. Conditional-gate/cubic-term ablations and total
training cost are necessary; a matched further-training BRE control may be needed
before an architectural success claim. No extra training is inferred or requested
at this stage. Investigate the prepared compositions first.


## Hybrid nonlinear components

The explicit ablation generator prepares full, unary-only and conditional/cubic-term
removal variants for all eight compositions. GATE constants are mean probabilities
from the first1024 positions/source, without labels; the same correction has exactly
the same gate vector on either base. All eight full functions reproduce their prior
predictions exactly. Independent CE checks agree within1.23e-15; the same21 CP
standard-status flags remain preserved, with no data filtering.

Removing conditional gating worsens held-out CE by.00429-.00492 on BT4 and
.00996-.01345 on CP. Removing cubic terms worsens it by.000397-.000749 on BT4 and
.000588-.001099 on CP. The nonlinear terms retain predictive value after replacing
the base, although cubic's contribution is smaller. Unary removal comparisons
are mostly favourable to the full model; CUBIC001 on BRE003 is a CP exception,
where unary-only is slightly better. Thus the whole correction's value and the
cubic term's marginal value are different questions. These are inference ablations
without retraining, on consumed development data, not chess evidence.

Exact plans, fit constants and metrics are in wave4-hybrid-ablation-model-plan.json
and wave4-hybrid-ablation-results.json. The diagnostic runtime adds only the plan
generator class; every existing inference/search/research class byte is unchanged
from the original hybrid runtime (the generated version property records its later
source revision). Core/application behaviour remains build52. No hybrid or ablated
strength plan is yet admitted; both trained additive controls and aggregate training
history remain necessary comparisons.


## Original direct programme closed; packed-layout decision

All448 games in the original direct admission are now closed, with61709 legal
moves independently replayed. The LR003 clock screen contributes96 games/13508
moves, without failures/caps/fallbacks: GATE00131.25%,GATE00331.25%,CUBIC00153.125%,
CUBIC00359.375%,BRE003 mean50%,Gen3234.375%, against BRE003 half. The corresponding
LR001 clock screen was21.875/43.75/46.875/31.25/53.125/25%. These eight-pair screens
retain wide uncertainty and opening sensitivity; no expressive superiority or
independent confirmation is established. Both trained controls remain necessary.
The selected32 full-clock games are now active, followed by64 BRE full/half controls
and32 deeper equal-node sensitivity games, all under the unchanged averaging runtime.

The completed same-JVM layout diagnostic resolves the earlier cross-run ambiguity.
All dense/sparse/packed implementations match canonical integer scores across
25143 checks/model, for both rates and all four retained widths at half correction.
At rank4, packed median score-transition costs are215.5/226.7ns versus dense
307.0/303.4ns (29.8/25.3% lower) and sparse237.6/245.0ns (9.3/7.5% lower).
Rank8 packed costs255.9/257.2ns; rank12 approximately286ns; rank16 approximately
303-307ns, close to dense. Nine rotated passes per implementation are retained.
These are microcosts, not whole-search gains. Packed copies add memory beside the
retained source model; all per-layout primitive counts are recorded.

This supports preparing an explicit packed research cache and actual search
oracles, while keeping ordinary model loading unchanged. It does not resolve
pruned chess quality: full masking was adverse and half masking inconclusive.
Any packed match must bind the same mask/function and a separate verified runtime.
Evidence: wave4-cubic-layout-plan/runtime/results.json. No core integration has yet
been performed at this record boundary.

The LR003 completion summarizer initially rejected nearby diagnostic process
receipts because they lack a tournament plan. It now explicitly classifies known
diagnostic entrypoints, records their hashes separately, and still rejects malformed
or unclassified records. Two targeted tests pass; a prior96-game clock summary's
results and all match-process bindings reproduce exactly. No timed match overlapped
any of the16 newly recorded diagnostic processes. Initial failed summary/regression
setup attempts remain preserved in wave4-process-summary-validation.json; no engine
or match result changed.


Packed cache implementation and remaining controls

The selected full-correction direct clock screen is closed:32 games,4528 legal moves; GATE003 and CUBIC003 each6/2/8 (43.75%) against BRE003 half. Both pair intervals include50%; no consistent recovery of the full-node gain, no conclusive rejection. See wave4-direct-full-clock-results.json. The admitted64 BRE scale and32 deeper-node controls remain next.

The same-JVM layout finding now has an explicit core research implementation, preserving the immutable model and ordinary evaluator defaults. Zero-head components are packed per worker; nonfinite error bounds retain all components. Same periodic rebuild and canonical rounding guard, explicit hybrid base, exclusive loader flag and fail-closed runtime capability binding.21 targeted tests pass (6 packed,4 dense,6 hybrid,2 blend,3 binding), including zero/partial/full widths, huge finite overflow, special moves, rounding boundaries, independent owners and search parity. Two summary classifier tests pass; an incompatible old runtime is rejected before engine launch. Actual-model/search oracles remain pending at this implementation boundary. See wave4-packed-implementation-validation.json and wave4-packed-model-plan.json. This core addition qualifies build53; no native GUI packaging/adoption or strength claim.


Actual packed search verification

Build53/c396945 frozen in wave4-packed-core-runtime.json passes all four actual rank4/rank16/zero and original/continued-base oracles:4x6333 positions, zero integer/fresh differences,48 root/pass comparisons each across canonical/packed/dense (plus actual base for zero). Same-root packed/dense median search cost ratios0.885/0.890 for rank4,1.009 for rank16; roughly11% search saving, smaller than the transition microbenchmark gain. Rank4 workspace28409 primitive bytes vs4329 dense; original payload retained. All64 smoke plies independently replayed,8 intentional caps excluded from strength. See wave4-packed-core-readiness.json.

An initial freeze-name collision left the earlier prototype runtime intact; a shell failed to stop after that Python error and ran four old-runtime probes. They ignored unsupported fields and are excluded in wave4-packed-probe-runtime-incident.json. No models/runtime were overwritten. The separate replacement verifies explicit capabilities and all jar hashes before launch; the match launcher already fails closed on incompatible capabilities. Preserve both sets as evidence.


Prospective hybrid screen

The next96-game mechanism screen is frozen before its outcomes: three LR001 corrections on the identical BRE001 half base (full GATE, full CUBIC, packed rank4 CUBIC),48 games at131072 nodes and48 at100ms, eight colour-reversed pairs each. It activates only after the existing calibration/depth controls close or a material contradiction warrants amendment. Selection follows lower-rate validation, complementary prediction and measured cache cost, not hybrid chess results. Both BRE rates and Gen32 remain required subsequent targets for any promising composition; nonlinear ablation and aggregate-training control remain essential. See wave4-hybrid-direct-admission.json.

Packed rank4 hybrid prediction algebra preserves BT4 improvement (.625507 vs base .636918) but CP is slightly worse than base (.588801 vs .588289) and worse than full cubic hybrid (.582806). This is a real quality/cost tradeoff, not a universally improved predictor. Consumed panels, no new fit or independent validation claim. See wave4-packed-hybrid-prediction.json.


Calibration controls and ablation readiness

All64 BRE full-versus-half games are closed,8970 legal moves, no failure/cap/fallback. Full BRE scores50% at32768 nodes for both rates, but37.5% at100ms forLR001 and34.375% forLR003. Broad pair uncertainty and reused openings preclude optimal-calibration claims. Half-change BRE remains an appropriate strong control; the depth interaction reinforces the admitted131072-node sensitivity test, now active.

All three selected hybrid ablations pass6333-position and48 root/pass canonical/cache search oracles (packed adds same-base dense). Maximum raw errors below2.7e-15. A dedicated read-only probe wrapper now fails closed on runtime hash/capability mismatch, shares the tournament lease, rejects orphan diagnostic/match processes, and caps a probe at300seconds. Active-lease and incompatible-runtime negative checks plus three normal probes pass; forced timeout was not exercised. The batch wrapper also recognizes orphan diagnostic processes. No search/model/optimizer change, no application bump. See wave4-selected-hybrid-ablation-readiness.json.

STATE has been consolidated to remove superseded current-action prose; historical findings remain here, in the manifests and Git history.


Phase sensitivity: informative negative refinement

A fixed piece-count stratification of the existing panels shows the largest CUBIC full CP maturity deterioration in17-24-piece positions: CE rises about.033 at either rate,306/1024 evaluation rows. It is not simply an endgame failure; low-rate1-8-piece CP improves by.029. Half correction largely suppresses the midgame deterioration. All76 weighted group totals reproduce the original Java CE within1e-12. See wave4-phase-sensitivity.json and its verification.

A bounded fit-only phase grid0/.25/.5/1 on the low-rate means selects GATE full everywhere, CUBIC full except25-32 pieces half, and BRE half except17-24 pieces full. The selective CUBIC loses.01420 CE on the CP evaluation half against uniform half; the additive selective control loses on both sources. No new phase-routing implementation or games are justified from this diagnostic. Preserve the negative result in wave4-phase-grid-diagnostic.json; existing hybrid protocols remain unchanged. Consumed data and repeated choices remain limitations.


Depth control and hybrid activation

The32-game131072-node depth control closes with4555 legal moves, no failure/cap/fallback. GATE0015/4/7=43.75%,CUBIC0017/3/6=53.125% against BRE001 half. Compared with32768 nodes, paired score changes are-18.75pp (descriptive95% -55.5..18.0) and-21.875pp (-51.79..8.04); small dependent development samples, no causal certainty. Absolute depth matters; do not attribute the earlier time/node discrepancy entirely to inference cost. The unchanged hybrid mechanism screen is activated in wave4-hybrid-direct-activation.json.

The first hybrid node launch was correctly rejected during plan parsing: nodes131072 and moveMillis2000 were both positive. The latter was mistakenly used to express safety, which the runner already supplies internally. No model loading/output directory/game occurred. Preserve the invalid plan and failed process. A separate node-v2 plan sets moveMillis0, with all models/openings/budgets/resources unchanged, under wave4-hybrid-direct-budget-amendment.json. The clock plan is unchanged. The wrapper now checks exclusive budgets before engine launch; the invalid original is a preserved passing negative check. Node-v2/clock execution is active.


Additive-control implementation frontier (static finding, not runtime evidence)

Before an eventual practical expressive-value claim, optimize the additive ablation where feasible. Source inspection of BrnPair2Geometry/Model and BrnNextModel plus an independent geometry enumeration gives294 compiled BRE physical relations and210 nearest-neighbour GATE relations. Four GATE physical index pairs are absent from BRE: (1,8),(6,15),(48,57),(55,62); all64 squares occur in BRE relations. Consequently an arbitrary unary correction can mathematically be folded into one existing incident BRE relation per square, but the whole constant-gate ablation cannot be folded into the unchanged294-relation payload: four extra pair tables are needed. Do not silently omit them or claim exact equivalence. This is an implementation hypothesis for a fast additive control if hybrids show value, not a new trained architecture, measured cost or explanation of observed strength. Existing canonical ablation oracles remain valid.


Compiled unary control implementation

The research-only compiler assigns each learned unary term to one verified incident BRE relation, retaining ordinary Pair2 payload/evaluation cost. It rejects non-unary/unbound corrections and nonfinite compilation, validates through the unchanged codec, and binds a distinct compiled payload hash plus both source components.18 targeted tests pass (compiler3,binding4,variants11), including every square/category/STM, special-board scores, source preservation and codec reload. Two process-summary tests pass. Actual trained score/search/prediction and smoke checks remain pending before any game admission. Binary64 summation order changes; no universal bit-equality claim. No core/GUI dependency change, so build53 remains.

Hybrid node-v2 paused deliberately after36 games at a bounded batch boundary for this build; no game was interrupted. Preserve the owned pause sentinel until ready, then remove it and resume remaining12 node games and48 clock games on their original frozen packed-core runtime. Do not substitute the new unary runtime into those protocols.


Compiled unary actual readiness and hybrid node closure

Both actual compiled unary functions pass6333 placement/fresh-score checks and48 canonical/compiled/dense search root passes, with zero integer/search differences; max raw error2.23e-14. All4096 prediction rows reproduce the prior canonical ablation within2e-14 and independent CE within1.23e-15. Their32 smoke plies independently replay,4 intentional caps excluded. Ordinary BRE payload795032bytes,immutable primitives799688bytes;35%/26% paired search savings versus the slow residual-cache unary implementation. Incompatible runtime rejection passes. See wave4-unary-compiled-readiness.json. They remain inference ablations, not separately retrained additive models.

Hybrid node-v2 is closed48 games/7366 legal moves: GATE8/7/1=71.875%,CUBIC8/3/5=59.375%,packed rank4 CUBIC5/6/5=50%, versus the identical BRE001 half base. No failures/caps/fallbacks. Gate is encouraging at equal nodes, but selection,8 pairs and repeated development opening use preclude confirmation even though its descriptive normal interval exceeds50%. The admitted clock48 remains active; no follow-up candidate selection yet. The temporary build pause is preserved and removed; all protocols retain their originally frozen runtimes.


Fast constant-gate additive control implementation

The GATE node signal justifies preparing an efficient mechanism ablation before interpreting practical nonlinear value. The constant-gate compiler folds unary and206 adjacent relations into a BRE base and retains all four missing relations as an explicit298-pair research layout. Core validates/copies the bounded extension; the ordinary Pair2 codec rejects it before any write, so it cannot masquerade as an ordinary checkpoint/optimizer. Component/function hashes and runtime capabilities bind the alternative. Targeted core boundary, Pair2 equivalence, unary, compiler and binding tests pass; see wave4-constant-compiled-implementation-validation.json for counts. Actual trained oracles remain pending. The core change qualifies build54; no native GUI/default adoption.

Hybrid clocks deliberately paused after36 completed games at a batch boundary for this build, with no interrupted game. Resume the remaining12 games on the original packed-core runtime after verification. The old unary smoke binding used an overly broad nativeResidualCache source tag; the executed compiled Pair2 function is independently established by its explicit hash/code/oracles. Preserve those receipts; new compiled bindings correctly say sourceFrozenResidual.


Complete additive-control readiness and selected follow-up

The298-relation constant-gate control passes6333 positions and48 canonical/compiled/dense search root passes with zero integer/search differences (max raw1.45e-13). Its4096 predictions match the prior canonical ablation within1.33e-13; independent CE error below9e-16.16 legal smoke plies pass,2 intentional caps excluded. Immutable primitives810568bytes,workspace2952bytes, transition130.3ns versus actual base126.2ns; paired search cost0.633 of the slow same-function gated cache. All four missing relations remain. See wave4-constant-compiled-readiness.json.

The original hybrid96-game screen is closed: clock48/6326 legal moves, no failures/caps/fallbacks. GATE5/1/10=34.375%,CUBIC6/6/4=56.25%,packed rank4 CUBIC4/6/6=43.75%. Same-root warmed NPS ratios.624/.727/.803; all confidence intervals remain broad. GATE quality at nodes has not translated into practical speed-limited gains; it remains a mechanism/cost question, not a discarded expressive family.

The fixed128-game follow-up is admitted in wave4-hybrid-followup-admission.json. Advance full CUBIC to Gen32 and BRE003 on16 development pairs, retain GATE for optimized conditional-gating ablation at nodes/time, and compare CUBIC with quadratic at nodes and compiled unary at time. This separates extra additive learning, cubic/conditional value and cost. No quadratic clock comparison is admitted until its computation is appropriately optimized. No final selection-adjusted confirmation or new human training gate is claimed.

The two node mechanism controls are closed: GATE versus compiled constant gates
5/6/5=50% over16 games/2071 legal moves; full CUBIC versus quadratic4/3/9=34.375%
over16 games/2399 legal moves. Both have8 complete reversed-colour pairs, no failures,
caps or fallbacks. Descriptive normal intervals are31.48-68.52% and9.98-58.77%.
Conditional/cubic chess value remains unestablished; this is neither proof of no
benefit nor evidence that a separately trained ablation would behave identically.
See the two `wave4-hybrid-followup-*-node-results.json` records.

`wave4-source-exposure-bounds.json` sharpens the previously documented W3 data
overlap limitation using frozen audited ledgers only. Gen32 scanned raw BT4 ordinals
0..17166401 and CP0..16392586. Both BRE continuation streams through64 remain
entirely inside those intervals; the residual streams extend to19418349/18383761.
Of8388608 post-reference residual training samples,908694..983040 have ordinals
outside Gen32's scanned ranges;5530795..5636096 lie outside the BRE64 continuation
stream's scanned ranges. These are rigorous interval/count bounds, not exact
selected-record intersections or FEN/game-disjoint claims. Inside an old scanned
range does not prove prior training on the record. The frozen base may have seen
correction holdout, as already documented in WAVE3. No source cursor was altered.
Matching summed hybrid updates alone would leave this coverage/diversity confound.

The internal follow-up clocks are also closed: full GATE versus fast constant gates
3/4/9=31.25% over16 games/2149 legal moves; full CUBIC versus compiled unary
4/8/4=50% over16 games/2424 moves. Both8-pair intervals include50%, with no failures,
caps or fallbacks. Same-root NPS ratios are.606 and.689. Neither mechanism has yet
earned an established practical advantage over its additive ablation. The two
external comparator clocks retain their fixed32-game budgets and remain active.

Operational evidence: the first clock batch completed12 games with process exit0,
but its outer shell incorrectly checked stale `$LASTEXITCODE` and stopped the loop.
No game failed or was replayed. The corrected finite loop checks the recorded child
exit code and resumed with a new log prefix. Four subsequently stalled read-only
PowerShell shells were identified by exact commands and stopped; unrelated shells,
the existing match loop and Java processes were preserved. Diagnostics are local
`raw/wave4/process-stall-01.json`; new reads/analysis use CMD or filesystem tools.
This is an environment/tooling limitation, not evidence of evaluator failure.
