# Wave6: trained degree control and matched continuation

21 October2026. This is the next experiment within the existing BRN successor
CGLHW, governed by CONTRACT revision2. G5 and all544 admitted W5 development
games are complete. No successor or independent strength superiority is established.
**G6 completed and verified21 October2026. All four Latest/Best endpoints are128.**
See [WAVE6-ASSESSMENT](WAVE6-ASSESSMENT.md), wave6-trained-checkpoints.json and
wave6-trained-admission.json. The preparation and GUI instructions below are preserved
as history; no repeat training is requested. Trained assessment is closed at288 development games; see WAVE6-ASSESSMENT.
The current blocking owner gate is G7 in WAVE7.md.

## Question and evidence for admission

Full R16 scores62.5% against its inference-only quadratic ablation at32768 nodes
(9/2/5, eight reversed-colour pairs; descriptive normal95 49.40-75.60%, bounded
interval14.48-100%). R4 scores46.875%. Full-versus-unary removes both quadratic
and cubic interactions; its favourable shallow results cannot isolate degree3.
Neither ablation lets the retained weights adapt. A separately optimized quadratic
control is therefore needed before attributing practical value to cubic terms.

At262144 nodes versus continued BRE001, R4/R16 score28.125/43.75%; at200ms they
score43.75/34.375%. Different shallow/deep opening sets prevent a causal depth
estimate. These results do not support throughput as the sole explanation, nor do
they prove an architecture inherently unsuitable. R4 improves Gen64-to96 prediction
on fixed old and fresh development panels, including standard-valid CP sensitivity,
while still trailing R16 on all32 same-generation validation comparisons. Compact
maturity/capacity remains a useful secondary question. Isolated R4 inference is
cheaper, but measured whole-search benefit over R16 varies substantially by roots.

Keep the same training objective and mapping for this degree intervention. The
quadratic contribution is useful under the raw training objective; selecting a
cubic-only training architecture from mapped prediction loss would be unjustified.
Mapping/objective alignment remains a later hypothesis, not a simultaneous change.

## Admitted arms and preserved initialization

Exact sources, checkpoint IDs and hashes are frozen in
[wave6-continuation-plan.json](wave6-continuation-plan.json).

| New lineage | Source Latest96 | Intervention |
| --- | --- | --- |
| W6-RESQUAD-R16-LR001 | W5-RESCUBIC-R16-LR001 | Wire7 degree2; retain every unary/embedding/quadratic weight and Adam moment, step and rate; omit only16 degree3 heads and their moments |
| W6-RESCUBIC-R16-LR001 | W5-RESCUBIC-R16-LR001 | Exact payload/state continuation; same starting data cursors as quadratic |
| W6-RESCUBIC-R4-LR001 | W5-RESCUBIC-R4-LR001 | Exact compact payload/state continuation; same residual data history |
| W6-BRE-CONTROL-LR001 | W5-BRE-CONTROL-LR001 | Exact ordinary BRE payload/state continuation |

All rates remain.001. BRE003 is preserved, not discarded: its G5 validation is
worse on all32 matched generations, its primary clock point score tiesBRE001,
and there is no development strength evidence requiring another rate fork now.
This is a bounded resource choice, not proof that.001 is globally optimal.

The new quadratic model is an additive pairwise control. It cannot itself satisfy
the programme's higher-order objective. Its dedicated guarded cache avoids unused
third-moment work; three research prototype replications saved7.5-12.3% isolated
R16 time, not a proven end-to-end speedup. No default evaluator is replaced.
The full C16 versus Q16 comparison is the primary mechanism control. R4 is a
secondary maturity/cost arm; a separately trained Q4 may be needed later if R4
becomes the leading candidate. No such success or fifth training arm is assumed.

## Dose, controls and decision rules

Each new arm receives32 additional generations,97 through128: batch128,8epochs,
131072 training and32768 validation positions/generation,seed1,one worker,
Held-out WDL, existing Training Data sources and equal BT4/CP mix. Preserve
source selection, cursors, CP seek indexes, partitions and optimizer continuation.
This adds262144 updates and33554432 training visits per arm. Expected ending
steps are1048576 for the three residual arms and2801664 for BRE. This bounded
dose matches the observed G5 adaptation interval; it is not a claim of convergence
or a universal stopping rule. No extra training is authorized to Codex.

C16/Q16 share exact pre-intervention training history, retained optimizer state,
objective and future data. The projected Q control still inherits a cubic-trained
initialization; inspect its adaptation before claiming architecture-level inferiority.
BRE retains its different historical cursors/splits and sparse optimizer semantics.
Do not claim all arms saw identical examples. W5 diagnostic panels are consumed
development evidence, with potential overlap in future validation; they are not
independent confirmation. Reserved confirmation openings remain untouched.

After G6, audit all actual Latest/Best identities and full trajectories. Use Latest128
as the primary equal-exposure endpoint, with Best as a recorded secondary choice,
not a silently selected winner. First verify actual cache/runtime/training integrity,
then compare C16/Q16 at practical depth and equal time, and compare promising cubic
arms against continued BRE and frozenGen32. Freeze bounded match protocols before
outcomes; do not reuse these development results as independent confirmation.

If C16 separates from a reasonably adapted Q16, investigate whether the quality
gain survives practical cost before confirmation. If Q16 matches or exceeds C16,
do not claim cubic value from full-versus-unary wins; prioritize objective alignment
or a justified new mechanism. If R4 remains materially weaker at practical depth,
do not repeat unchanged continuation indefinitely. Retain uncertainty about maturity,
data and calibration, but require a discriminating hypothesis for any later dose.

## Verified preparation

Implementation7e28740/build56 passes126 targeted tests (see degree-control-
implementation-validation.json). They cover independent forward/gradients,
projection/Adam persistence, cache/search parity, source-preserving preparation and
interrupted GUI-service continuation. No full-suite claim.

All readiness checks pass. Four distinct owner stores exist with Latest/Best both
at their new Gen96 bootstrap and zero new completed history rows. Independent
Python verifies exactly39219 retained weight/moment values (3x13073),
CRC/layout, step/rate and exact source/cursor/configuration/CP-index inheritance.
The three continuation controls retain exact source model and optimizer bytes.
The actual five-payload Java audit verifies checksums and runtime/state scores
on24 boards each. No original owner store was trained or rewritten.

Four actual cache probes (new Q16, C16, C4 and masked C16) each pass6333 positions
and48 paired canonical/cache search root passes, zero integer/search differences.
New Q16 and its masked C16 source agree on all96 search results. Q16 model/state
sizes are104656/313840 bytes, workspace4073 primitive bytes versus4329 for C16.
Its276.7ns transition median versus330.2ns for the identical masked C16 function
is a single sequential measurement; the warmed same-function search-cost ratio is
.95592 across32 root-pass observations. This modest end-to-end reduction does not
establish trained Q strength or a universal NPS gain.64 smoke plies replay legally;
eight intentional8-ply caps are loader checks, excluded from strength statistics.

Native build56, packaging source4faba8d5a79d, passes bundled model/runtime/JNI and
copied/extracted-path verification. The actual owner GUI test passes (141 distinct
targeted JUnit invocations in total, zero failures/skips). Fourteen explicit Play-catalog/participant-binding architecture cases also pass,
including Q16; selection remains pinned to the requested checkpoint. A separate repeat using
the packaged GUI JAR and bundled runtime loads all four lineages without starting
training; eight images are preserved, and lineage/recipe screens were inspected.
The first packaged-probe shell launcher stopped on a benign JVM stderr warning;
its evidence is preserved. A structured-process retry passed. No runtime defect or
owner training failure occurred. No full-suite or substantive training claim.

All710 historical W3/frozen,1405 W4,925 trained W5 and72 new W6 inventoried files
retain their hashes after all checks. W1 was untouched, not freshly rehashed.
See [readiness](wave6-owner-readiness.json), [projection](wave6-projection-audit.json),
[cache/search](wave6-cache-validation.json), and [preservation](wave6-preservation-check.json).
Raw evidence remains local under raw/wave6; compact manifests bind its paths/hashes.

## Human-training gate G6: exact actions

Launch the verified executable:
`C:/projects/seed/java/seedv6-unified/dist/windows/candidates/candidate-20261021T051743-83f14ec3/SeedV6-GUI/SeedV6-GUI.exe`.
Use **Network Training** and select each prepared lineage, one at a time:

| Name | Folder / lineage ID under E:/SeedV6-Networks |
| --- | --- |
| W6-RESQUAD-R16-LR001 | BRN-GEN32-QUADRATIC /23022fb7-f9fc-349d-ad69-60088a84d740 |
| W6-RESCUBIC-R16-LR001 | BRN-GEN32-CUBIC /ce533b4e-83a8-3ac5-b9e0-546f1ce414cd |
| W6-RESCUBIC-R4-LR001 | BRN-GEN32-CUBIC-R4 /dd1c701f-ff67-38da-97e3-40f41605ec98 |
| W6-BRE-CONTROL-LR001 | BRN-Pair2 /065d3f01-1bd6-3207-9c26-454dbfa683f3 |

All start from the bound W5 **Latest96**, not W5 Best. The new Q model's Gen96 label
is its inherited starting checkpoint, not96 completed Q generations. The GUI should
show Latest Gen96 (Starting checkpoint), Best Gen96, zero recorded completed
generations, idle Generation97 and **Start Training**. This is expected.

Keep **Fixed Number of Generations / Additional Generations32**, learning rate
**0.001 for all four**, batch128,8epochs,131072 training positions/generation,
seed1,one worker, **Held-out WDL** validation (32768 positions/generation),
**Training Data sources** and the inherited equal BT4/CP mix. The prepared
**Keep checkpoint learning rate on Resume** box is unchecked. The architecture's
generic default-rate description does not replace the selected0.001 recipe.
Do not edit sources/cursors/indexes, import another model, create replacement
lineages or train original W5 stores. Run one training job and no Arena concurrently.

Train97 through128, then stop. Do not start another32 after completion. Preserve
every bootstrap and checkpoint97..128, optimizer, Best/latest pointer, history,
source metadata and degree-continuation-origin.json; Best may precede128. The
new33-checkpoint lineages fit the existing100-generation retention. All older
models and experiments remain protected.

After interruption, select the **same W6 lineage** and use its offered **Resume
Generation** action with unchanged settings. An ACTIVE/crash/failed session retains
its target/remainder. An explicit safe Stop ends the session: before a new Start set
Additional Generations to `128 - latest settled completed generation`, preserving
any unfinished state. Do not reset, clear partials, import or restart from96.
Tiny fixture tests cover shuffled partial/epoch and optimizer continuation, including
the Q schema; actual substantive Gen97 completion remains an owner action. Preserve
and report errors or unusual loss before changing state.

**Human actions required after this prompt: blocking — train these four prepared
lineages through128, close the GUI and report completion, interruptions or setting
changes.** This blocks trained W6 assessment. No G5 retraining or manual Arena
matches are requested. No separate non-blocking action is required.

The existing runner pause is at build/research/brn-successor-batches.pause.
On resume, first verify persisted G6 histories/settings/Latest/Best and expected
steps, rather than treating a later invocation as completion. Preserve the pause
as raw/wave6/g6-completed-pause.txt only after the gate is evidenced. Then freeze
actual endpoints and select the next bounded protocol under the decision rules
above. Do not restart the closed544 W5 games or claim programme completion.
