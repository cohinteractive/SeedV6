# Verified bootstrap semantics and parity implementation

Authority: owner's 2026-10-05 fairness amendment. No BRN-3 code is changed.

## Reference audit

`core/brn3/Brn3Features.pieceMaterial` assigns P=1, N=3.2, B=3.3, R=5,
Q=9, K=0 **pawn units** to every occupied square. Promoted pieces receive their
current identity's value. There is no phase, square, count-dependent bonus,
bishop-pair term, king value, or endgame scaling. White adds; Black subtracts.
`material` negates that sum for Black to move. STM changes perspective, not a
tempo bonus. Rights, EP, clocks and hash are not evaluator inputs.

`Brn3Trainer.predictPawns` adds this scalar directly to the neural readout.
Fresh unary/pair/shared/dense parameters are random; HEAD and OUTPUT_BIAS are
exactly zero. Thus real Gen0 is material-only, irrespective of seed.
`Brn3Workspace.evaluatePawns` similarly sums White material while rebuilding its
piece list, caches it for identical placements, and adds its STM-relative value
directly to `compactReadout`. It is neither a trainable input nor passed through
trainable layers. Production computes `M + g * ((M + R) - M)` with `g=.25`, from
`Brn3SearchCalibration.BRN3_MATERIAL_RESIDUAL_QUARTER_V1`. Material is unattenuated.
The subtraction can introduce binary64 roundoff; there is no material-dependent
clipping or nonlinear material transformation in this stage.

`Brn3Model.score` rejects nonfinite totals and returns symmetric nearest integer
(half away from zero), **100 score units/pawn**, clamped to ±32511 below the mate
band. No nonzero minimum. The tiny floating error from decimal3.2/3.3 accumulation
disappears at ordinary integer Gen0 scoring; tests compare actual reference scores.

After training, material remains unchanged. `Brn3Trainer.trainBatch` learns a
residual on `M+R` using `Brn3Objective.crossEntropy`; search still uses `M+.25R`.
The corpus WDL link depends on total material as a supervised loss/output link,
not an additional runtime handcrafted position value. Disclose and match this
where appropriate in strict training controls. Training-only relative-table
factors are learned and folded into inference; they are not fixed desired values.
No PSQT, mobility, king-safety, pawn, tactical or positional rule is present in
this audited evaluator path. Relation and pool normalization depend on piece
counts (1/sqrt(max(1,n-1)) and1/sqrt(max(1,n))); they scale learned features,
not fixed material. At Gen0 the zero head removes their contribution to the score.
Shared search SEE/material move ordering and chess terminal rules remain common
to both sides; they are not newly added NNUE evaluation knowledge.

## First controlled NNUE addition

`NnueMaterialBootstrap` stores integer hundredths:100/320/330/500/900/0. One
White-relative integer per search ply is initialized from occupied squares;
transitions subtract/add only changed-square identities. Null/quiet/king/castling
moves preserve totals; captures, EP and promotions update them. STM sign is
applied at readout. No learned parameter or board mutation is added.

Explicit `SearchEvaluation.incrementalWithMaterial` and
`fullRecomputeWithMaterial` factories enable it. Existing factories/codecs remain
material-off for historical replay. Research `-material` variant suffix composes
the same mechanism with any incremental architecture; old names remain Track A.

First-baseline neural score is exactly the previous mapping:
`N = V1.map(tanh(raw))`, scale32511, including old rounding, nonzero minimum and
clipping. Return `clamp(M_engine + N, ±32511)`. No .25 gain is applied to this
legacy residual, which is not a pawn-unit BRN residual. Material is exactly the
reference contribution; learned calibration remains explicitly different and
unchanged. Neural clipping precedes addition and final clipping follows it.
This isolates the prior; it does not certify calibrated trained centipawns.
Diagnostics expose learned raw values separately from final integer scores.

Old trained outcome-predicting NNUE checkpoints cannot simply receive this term
and be called controlled residual training: that could double-count learned
material. Establish a new residual-aware training recipe and explicit identity
before trained Track B comparisons. Retain material through training and search;
never silently reinterpret legacy checkpoints. No material value/gain tuning.

## Validation

Known synthetic positions check each identity/colour/STM and square independence;
full-reference comparisons, long legal chains, branches, reverse transitions,
nulls, castling, EP and all legal promotions check incremental accounting.
Production incremental/full-refresh, wrapper and material-off scores agree.
Zero-head material matches actual BRN-3 Gen0 exactly on the test corpus.
Saturation tests protect the mate band and sum overflow. All36 targeted Java
tests passed before parity matches (2026-10-05).

Hot-cost measurement includes final score, not only raw learned output. The prior
adds four payload bytes/ply (1028 for257 slots), no per-call allocation and
O(changed squares) updates. Measured cost/match outcomes belong in E008.md.
