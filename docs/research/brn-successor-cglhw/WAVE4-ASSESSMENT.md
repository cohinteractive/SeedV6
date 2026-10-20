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
