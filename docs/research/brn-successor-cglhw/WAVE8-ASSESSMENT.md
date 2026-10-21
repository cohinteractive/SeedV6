# Wave 8 fixed-head assessment

22 October 2026. Original CGLHW, contract revision 2. **G8 is verified complete
through Gen168 and all 176 prescribed development games are closed. No independent
strength superiority, useful cubic strength advantage or programme success is
established.** The next admitted experiment is WAVE9; G8 requires no retraining.

## Training and checkpoint verification

All three sessions completed eight generations161..168 from their exact prepared
Gen160 bootstraps. Nine checkpoints per arm, expected parent chains and 8,192-step
increments pass. Latest is168 for all; fixed cubic and fixed quadratic Best are167,
BRE Best is168. Full checkpoint IDs, network/optimizer hashes, ancestry, histories,
recipes and validation records are in [wave8-trained-checkpoints.json](wave8-trained-checkpoints.json).
Selection was prospective Latest168, without retrospective Best substitution.

All 28 payload/state pairs, including the explicit frozen Gen32 reference, pass
checksum and 24-board runtime/optimizer score parity. Every frozen weight and
excluded zero Adam moment is exact through every generation. Heads end at65,536
steps; BRE at3,129,344. Sessions are COMPLETED, final attempts settled, with no
partial/staging state or recorded anomaly. Uninterrupted wall-clock execution is
not independently known. Source reservations, recipes and inherited CP seek
prefixes pass. Read-only frozen copies and admission are retained locally under
raw/wave8/trained-frozen-01 and bound by wave8-trained-admission.json.

Both head arms begin at the exact W7 Q4 Gen160 function with new Adam step0.
Cubic freezes all3,845 Q4 weights and trains four new cubic coefficients;
quadratic freezes3,841 values and trains four existing quadratic heads. Their
source reservations match through Gen168. BRE retains its full optimizer and distinct
historical data/split. Each adds1,048,576 training examples,262,144 validation
examples,8,388,608 visits and65,536 updates. Equal nominal learning rate does not
imply equal functional conditioning.

All losses are finite. Cubic beats quadratic validation in3/8 generations, with
mean cubic-minus-quadratic +0.00000078368. First-four/last-four means are cubic
.10573650/.10463036, quadratic .10570503/.10466027 and BRE .10670902/.10491812.
Panels change each generation, so these are not fixed-test learning curves.
Training optimizes cross-entropy; displayed training/validation loss is
half-squared outcome error. Best promotions compare incumbent/candidate on that
generation's panel. No strong convergence, overfitting or capacity conclusion is
warranted from eight changing panels.

On four consumed, status-valid evaluation panels, Latest cubic improves the
established half-change/search-mapped cross-entropy modestly; quadratic/BRE are
mixed. Best cubic is worse than Latest on those panels. Java metrics independently
reproduce within3.34e-15 (wave8-fitting-old/fresh5.json). Coefficient signs and
magnitudes vary considerably between167/168. This supports optimization/calibration
investigation, not a claim of corruption or inherent cubic unsuitability.

## Fixed development strength programme

Protocol: wave8-assessment-protocol.json; complete results and evidence hashes:
[wave8-assessment-results.json](wave8-assessment-results.json), with individual
wave8-*-results.json reports. Frozen build58 and exact Latest168 payloads were
used throughout. Common Policy4/QXRAY, material-plus-quarter mapping, half learned
change from Gen32, reversed colors and fixed node/time budgets. Thirty-two new
development roots exclude prior exact roots; training and opening-family overlap
remain unknown. Roots are reused across related comparisons. Partition1 remains
untouched for separately admitted confirmation.

NPS below is the median ratio at paired identical initial search roots, not a
universal engine speed ratio. W/D/L and score refer to the first column.

| Candidate | Opponent | Budget | W/D/L | Score | Descriptive paired95% | Bounded paired95% | NPS ratio |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Fixed cubic | Trained fixed quadratic | 262,144 nodes | 12/8/12 | 50.0000% | 33.87–66.13% | 16.05–83.95% | 0.945 |
| Fixed cubic | Trained fixed quadratic | 200 ms/move | 10/9/13 | 45.3125% | 34.16–56.47% | 11.36–79.27% | 0.911 |
| Fixed cubic | Unchanged Q4 anchor | 262,144 nodes | 7/4/5 | 56.2500% | 36.07–76.43% | 8.23–100.00% | 0.952 |
| Fixed cubic | Unchanged Q4 anchor | 200 ms/move | 8/5/3 | 65.6250% | 47.25–84.00% | 17.61–100.00% | 0.939 |
| Fixed quadratic | Continued BRE | 200 ms/move | 5/5/6 | 46.8750% | 23.38–70.37% | 0.00–94.89% | 0.867 |
| Fixed cubic | Continued BRE | 200 ms/move | 5/3/8 | 40.6250% | 20.05–61.20% | 0.00–88.64% | 0.849 |
| Fixed quadratic | Frozen Gen32 | 200 ms/move | 4/1/11 | 28.1250% | 2.87–53.38% | 0.00–76.14% | 0.893 |
| Fixed cubic | Frozen Gen32 | 200 ms/move | 7/3/6 | 53.1250% | 21.81–84.44% | 5.11–100.00% | 0.825 |
| Continued BRE | Frozen Gen32 | 200 ms/move | 11/2/3 | 75.0000% | 58.96–91.04% | 26.98–100.00% | 1.086 |

All 23,873 recorded moves replay independently as legal; no game
failure, ply cap or fallback. Every resource receipt verifies one game, one search
worker, no pondering,64MiB TT,768MiB heap,ActiveProcessorCount1 and BelowNormal.
Each launch was bounded at12 games/240 seconds, finishing the current game.
Maximum sampled working set was583.7MiB;
recorded engine CPU totaled4555.8seconds. No training ran
with tournaments. The owned idle build daemon was released after packaging;
pre-existing user Java processes were preserved.

Normal intervals are descriptive, not selection-adjusted confirmation. Bounded
intervals assume independent opening pairs, which family dependence may violate;
within-plan simultaneous bounds are in the individual reports and do not correct
all prior selection or cross-protocol comparisons. Do not pool different budgets
or opponents. All fixed budgets completed without favorable stopping or extension.
One premature BRE batch launch was rejected at the exclusive lease before any
engine/receipt creation, then resumed only after the preceding batch exited0;
raw/wave8/bre-clock-launch-observation-01.json preserves the event.

The direct degree comparisons do not establish a cubic advantage. Positive exact-
anchor point estimates remain uncertain and do not establish benefit beyond an
adapted additive control. Both candidates' continued-BRE point estimates are
below50%, also uncertain. No candidate is admitted to independent success testing.
The evidence neither proves useful cubic strength nor rules out nonlinear
expressiveness generally. Training maturity, low-dimensional fixed features,
calibration and throughput remain plausible contributors.

## Inference cost and bounded diagnostics

Actual frozen G8 cache probes each pass6,333 positions and48 full/cache search
pairs, with zero integer/fresh/search differences and maximum raw error2.22e-15.
Cached medians are cubic 255.552ns and quadratic 204.704ns; corresponding separate
Gen32 medians151.404ns and137.040ns. Full evaluation medians are639.664ns and
555.986ns. Sequential host/JIT variation limits direct ratios. Parameter counts
are3,849/3,845 plus the frozen base; combined serialized model sizes825,896/825,864B;
workspaces3,753/3,689 primitive bytes, excluding object headers. Evidence:
wave8-trained-readiness.json and raw/wave8/trained-cache-*-half-01.json.

Cubic costs about25% more than quadratic in these isolated cached measurements;
the direct equal-time search ratio is about.911. Against continued BRE, quadratic
and cubic ratios are.867/.849 on another root set. These differences demonstrate
why isolated latency cannot substitute for measured search or strength.

Uniform161..168 head averaging equals functional averaging to roundoff but reduces
the already small cubic signal and worsens all four mapped panels versus Latest;
no averaged-cubic match was admitted. Quadratic/BRE averages improve fitting but
do not demonstrate extra expressiveness. Latest cubic changes about23–30% of
mapped integer scores, mean absolute0.23–0.30 score units, RMS.0033–.0039 pawns.
See wave8-head-impact.json. Its initial BRE comparison mixed trainer and compiled
representations; the corrected analysis separates the known2.89e-9 metric delta
from same-representation agreement1.12e-15. Raw failed output is preserved; no
model, data, training or match repair was required.

Bounded128-update diagnostics use2,043 consumed fit rows, identical nominal Adam
.001 and seed, no owner writes or model export. Material-phase-conditioned
quadratic features lack consistent benefit and were not selected. Search-mapped
fitting helps mapped loss more than raw fitting, but a four-coefficient ordinary
affine control improves more than interaction-only pilots on all four panels.
Thus calibration is a material confound, not proof of expressive information.
Joint affine+quadratic and affine+cubic improve further; cubic wins only fresh
BT4. Full configurations and negative results: wave8-phase-head-admission/results,
wave8-affine-head-admission/results and wave8-calibrated-degree-admission/results.
These are diagnostics, not substantive training or independent generalization.

## Decision and continuity

Admit W9's matched affine-only, affine+quadratic and affine+cubic head models,
plus exact BRE continuation, to test degree value after shared calibration and
search-aligned fitting. This follows the completed G8 evidence and concrete
diagnostics, not a favorable selected match. The underlying Q4 basis remains fixed;
all new head coefficients/Adam begin at zero in distinct identities. See WAVE9
and wave9-gate-admission.json for architecture, validation, exact human settings
and restart conditions. No unchanged G8 extension is requested.

All existing model artifacts are preserved; final audited inventory results are
in wave9-final-preservation.json. W1 and source data were untouched, without a new
complete checksum claim. Raw evidence is local-only and absent from a fresh Git
checkout; manifests bind the recorded local paths. Application implementation is
build59/e804482, while the closed G8 games remain frozen build58 evidence.

Human actions required after this prompt: the consolidated G9 GUI training in
WAVE9, blocking trained G9 assessment. G8 itself requires no further human action.
