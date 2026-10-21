# Wave9 calibration-aware fixed-basis investigation

22 October2026. Existing CGLHW, contract revision2. G8 is complete; its frozen
strength assessment is still running. This is autonomous experimental preparation,
not an active human gate, a trained model, or a strength claim. Gate admission
requires the completed G8 interpretation and all implementation/readiness checks.

## Question and evidence

Does cubic information add practical value after fitting an equally calibrated
additive baseline? G8's tiny mapped cubic signal, unstable endpoints and raw versus
mapped fitting disagreement motivate this control. A four-parameter affine pilot
beats interaction-only pilots on all four consumed mapped panels. Joint affine
plus cubic and affine plus quadratic improve further; cubic beats quadratic on
only one panel. These are128-update diagnostics on2043 consumed status-valid fit
rows, not substantive training or independent generalization. Preserve all negative
results: wave8-phase-head-results, wave8-affine-head-results and
wave8-calibrated-degree-results. Phase conditioning is deferred, not implemented.

## Proposed controlled implementation

Keep the exact Q4 anchor retained inside W8 fixed-cubic Latest168. Its3845 values
are unchanged from W7 Q4 Latest160. In new identities only, omit the trained W8
cubic coefficients, initialize new heads/Adam to zero, and retain the source's
Gen168 data cursors and seek indexes. This is an explicit projection/restart of
head optimization, never inherited Adam or an overwritten W8 model.

Let G be frozen Gen32 raw pawns, M material, R the original Q4 correction,
q/c the fixed rank-four degree2/3 features. Define four affine features
[1,M/8,(G-M)/4,R]. New model raw pawns are
G + .5*(R + affineHeads dot affineFeatures + optional degreeHeads dot q/c).
Search uses the unchanged material-plus-quarter mapping once. Training and
validation use that same floating-point search pawn function before integer
rounding. This changes the training objective explicitly; existing wire1..10
models, objectives, weights and search behavior remain unchanged.

Proposed new wire11/12/13 layouts: calibrated affine-only (four trainable
coefficients), affine+quadratic (eight), affine+cubic (eight). All retain the
same3845 anchor values. The first two are additive pairwise controls, not
successor expressiveness claims. The cubic variant adds genuine degree3 terms.
Canonical evaluation remains an independent oracle; runtime may fold constant
head coefficients into existing guarded polynomial caches, preserving rounding
safety. No search-policy tuning or unrelated optimization is admitted.

The eventual consolidated gate, only if admitted after G8 closure, is three
calibrated head arms and exact continued BRE, all at168 with eight additional
generations through176, LR.001, batch128,eight epochs,131072 positions,seed1,
one worker,held-out WDL and inherited sources. Prospective Latest176 primary,
Best separately audited. Head Adam starts0; BRE continues its exact state.
No unchanged automatic extension. No new owner lineages have been created yet.

## Admission and restart

Finish all176 G8 games without result-dependent budget changes. Implement and
verify analytic gradients, immutable anchor/state masks, serialization/resume,
canonical/cache/search parity, source/seek inheritance and packaged native GUI.
Reassess expected value using the closed games. Only then create verified separate
lineages and present exact human instructions, or record why the proposal changed.
Current next action remains in STATE. Human actions required after this prompt:
None during this autonomous preparation; no G9 training is requested yet.

## Implementation validation in progress

241 targeted tests passed, none skipped: explicit-combination forward oracle,
trainable analytic/finite-difference gradients of the calibrated objective,
frozen weights/moments, exact resumed Adam and partial corpus generations,
source cursor ownership, codecs, colour symmetry, native evaluator selection,
rounding guards and full/cache search parity. No full-suite claim. Evidence:
wave9-implementation-validation.json. Earlier failed runs are preserved: a
pawn-channel fixture error and exhaustive integration enum fixtures were fixed.

Isolated actual-source preparation succeeded with zero training. Independent
byte/CRC audit verifies all3845 retained anchor weights, four/eight zero new
heads, zero moments/step, exact BRE optimizer continuation, full inherited
source configuration/cursors and CP seek indexes (wave9-isolated-projection).
The first Java audit plan incorrectly used a payload-only Gen32 copy as a full
store; it failed on absent refs before auditing any candidate. Its corrected
v2 plan uses the already verified owner reference store; no store repair is
needed. Complete its runtime checks and actual-weight cost probes before gate
admission. No owner G9 lineage exists yet.

A subsequent implementation review caught a new-path integration mismatch before
admission: checkpoint-based HeldOutLoss still evaluated raw pawns while the new
trainer used mapped search pawns. The new architectures now explicitly route
checkpoint validation through canonical pre-rounding searchPawns. Direct
trainer/immutable-checkpoint outcome parity assertions cover all13 BRN-next wire
families. Existing wire1..10 objectives remain unchanged. A new full targeted
run06 is required; the earlier241 passes are not presented as verification of
this subsequently corrected code. No owner G9 training occurred.

Run06 completed all241 targeted tests with zero failures/skips after the
validation correction. Five actual payload/state pairs pass24-board codec/runtime
parity. Each actual-head cache passes4340 played positions and48 full/cache search
pairs; all three zero-head formulations also agree on every search result.
Maximum raw difference2.09e-15; integer/fresh-state differences zero.
Measured cached medians affine204.122ns,quad202.720ns,cubic215.632ns, versus
separate Gen32 measurements132.904/132.316/141.220ns. These are initial functions,
not learned-model speed/strength claims. Workspaces8205/8205/8269 primitive bytes,
plus a per-worker derived polynomial model of about30KiB; headers excluded.
See wave9-implementation-validation-v2 and wave9-isolated-readiness.
The development runtime retains inherited build58 and explicitly binds uncommitted
source hashes; its copied timestamp is corrected in the separate runtime erratum.

Application implementation is ready for the build59 commit and native packaging.
This does not activate G9: finish G8, interpret its full evidence, then validate
the packaged owner lineages before requesting any training.
