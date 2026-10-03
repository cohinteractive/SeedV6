# BRN-3 V1 programme result

Outcome: **V1 success under the predeclared engineering criteria**, 2026-10-03.
The retained BRN-3 implementation is usable from scratch through normal Training
Data, checkpoints, search and GUI workflows. All required experimental gates and
targeted software checks pass. User acceptance and finalized build/release provenance
remain separate; the inherited version reservation is still blocked.

## Retained architecture and rationale

BRN-3 is an eight-channel absolute all-pair relational network with node-local ReLU,
typed pooling into 12 owner/type groups, dual mover/opponent perspectives and a
32-unit ReLU readout. A learned relative-displacement table shares statistical
strength during training and folds into the immutable inference table. Typed pooling
preserves entity identity that earlier small global pools discarded.

The fixed material prior is P 1, N 3.2, B 3.3, R 5, Q 9, K 0. Its independent pawn-unit residual
head starts exactly at zero. The engine receives 100 units/pawn with symmetric
rounding and normal-band protection. Color/ownership plus rank transformation
preserves mover-relative semantics; changing only the mover need not negate the
prediction because a learned tempo effect is allowed. Runtime inputs remain piece
identity, ownership, square geometry and STM. No attack, rule, history or hash feature
was added, and no search rule was changed to make BRN succeed.

The retained recipe is minibatch 128, eight epochs, masked Adam .003/.9/.999/1e-8,
pure expected-score cross entropy through the existing frozen WDL adapter. The
network still outputs pawns. All absent-coordinate moments remain unchanged.
Initializers, float folding, strict codecs, per-worker inference, ordinary training,
checkpoint selection/loading, architecture configuration and GUI are implemented.

Additive pair potentials learned too little; factorized global pooling, directed
message passing and king-anchored variants failed the representative quality gate.
Forced STM antisymmetry, wider embeddings and conventional dense-history Adam did
not justify their cost. Relative sharing, stable CE gradients and typed pooling
provided the successful progression. Rejected code is confined to verification
research helpers; only the retained BRN-3 family is added to normal production code.
The [ledger](BRN_PROGRAMME_LEDGER.md) preserves hypotheses, ablations and measurements.

## Matched fresh-NNUE evidence

The real framed Lichess source was usable before this programme began. The inherited
corpus migration was not reimplemented. Final runs use two source ranges and seeds
71/97, each 131072 distinct training,16384 validation and 16384 sealed-test positions.
Both architectures receive the same records, targets and 1048576 training exposures
per run. Eight epochs are opportunities; validation selects the checkpoint. BRN
selects epochs 8/6, NNUE 8/8. NNUE initialization, optimizer, feature kernels and score
mapping remain its existing implementation. Its test loss improves from .171468
fresh to .064947 trained, so the control is not an unlearned baseline.

Global geometry-group assignment prevents allowed-input equivalents crossing splits,
including across source ranges. The second range uses uniform held-out reservoirs.
The source has no game identifiers; related positions and order effects remain.
Identity is a sampled source fingerprint, not a full 22 GB checksum. Final tests were
opened once after the recipe froze and did not guide subsequent model tuning.

| Measurement | Seed 71 | Seed 97 | Declared gate |
|---|---:|---:|---|
| BRN sealed-test half-MSE | .062763 | .077210 | pooled ratio<=1.10 |
| NNUE sealed-test half-MSE | .061221 | .068674 | same examples |
| BRN/NNUE test ratio | 1.025190 | 1.124294 | pooled 1.077586 |
| Balanced-label ratio, absolute CP<=200 | 1.161686 | 1.185615 | report stratum |
| BRN training-only seconds | 108.084 | 129.117 | report compute |
| NNUE training-only seconds | 224.251 | 241.375 | matched exposure |
| BRN warmed evaluation microseconds | 9.380 | 9.810 | throughput>=.25 x |
| NNUE warmed evaluation microseconds | 2.714 | 2.660 | same boards |
| Paired median throughput ratio | .291319 | .269187 | pass |
| Production depth 4 completed roots | 128/128 | 128/128 | all complete |
| Production aggregate node ratio | .906428 | .973221 | <=2.0 |
| Production roots above 10 x control | 0 | 0 | <=5% |

Pooled half-MSE is .069986 versus .064947; the geometry-group bootstrap 95% ratio
interval is[1.046175,1.107427], below the 1.20 upper gate. Material-only loss is
.223076, a 68.6% reduction. CP MAE improves from 2.800555 to 2.248334 pawns. Residual
RMS is 1.988288 pawns. BRN makes 2163 versus 2027 sign mistakes among 17284 labels
of at least 50CP. These are engineering tolerance passes, not equality of predictors.
Balanced positions remain weaker. CP-tail RMSE is 11.61/13.07 pawns, reflecting large
label tails and the outcome-focused objective; it must not be hidden by the pooled
outcome score. Calibration bins, material/phase strata and error quantiles are in
[the sealed report](evidence/programme-2026-10-03/brn3-final-sealed-test.json).

## Practical search, play and compute

Both production-search replications pass without changing NNUE or search semantics.
BRN times total 3.676/3.993 s versusNNUE 1.305/1.520 s, including root setup/cold effects.
Local quiet p 99 deltas 2.295/1.907 pawns and maxima 3.774/4.200 remain below a queen.
Capture, recapture, promotion and geometry probes are finite; none of the sampled
local deltas exceeds nine pawns. Color equivalence is distinct from a legal move or
STM-only reversal. The final float/cache maximum error is 3.775e-7 pawn over 32768
validation positions, with zero integer-score differences or normal-band saturation.

Production has no qsearch. The separate unpruned, TT-free depth 4 probe initially
hits its 2 s deadline on BRN 1/4 and NNUE 2/1 roots. Every tail finishes under the
2 million-node/10 s follow-up guard. Complete aggregate qsearch node ratios are
.518/.853, but the second seed has two roots above 10 x control; the maximum is 17.94 x
(838203 versus 46717 nodes,5.357 versus .180 s). These bounded tactical-tree tails
remain a limitation before any future production-qsearch integration. Original
timeouts are preserved, not relabeled as successful 2 s runs.

The fixed game plan is 100 distinct reversed-color opening pairs per seed,25 ms/move,
512-ply administrative cap, legal-uniform 6..10-ply openings, seed 620391, start index 1000.
No evaluator filters openings; only active, roughly material-balanced starts qualify.
Games use actual production SearchDriver and normal rules. Capped/failed games
cannot be draws or satisfy sample completeness.

Both seeds finished 198 wins, 0 draws and 2 losses each: point score .99 and paired
one-sided 95% lower .98. All 400 games finished by checkmate, with no administrative
caps or failures. A supplemental conservative Hoeffding lower bound is .8676 per
seed and also clears the gate. The same 100 openings are reused across seeds;
these are not 400 independent observations. These short-time results concern the matched fresh controls
and this opening distribution; they are not mature-NNUE parity, general Elo, or
evidence about long time controls. The gate requires score>=.45 and lower>=.40.
Observed game NPS is BRN 276k/279k versus NNUE 698k/703k. Mean per-game average
completed depths are 9.27/9.27 versus 9.65/9.84. The full aggregate and raw match
records are linked from [final experimental evidence](evidence/programme-2026-10-03/brn3-final-experiments.json).

BRN has 2368577 inference/2627777 training parameters. Model payload 9,474,352 bytes
is smaller than NNUE 12,599,889; exact training state 63,066,732 bytes is larger than
NNUE 37,799,553 because BRN training uses double weights/moments. Large trainer
primitive arrays occupy 106,289,192 bytes including the shared relative-row lookup,
excluding JVM, object headers, corpus and transient copies. Worker primitive scratch
is about 12 KiB. Runtime reports include parent/quiet-child and capture/rebuild costs.

## Usable workflow, validation and provenance

[BRN_V1.md](../../brn/BRN_V1.md) documents architecture, GUI and headless scratch
commands. The real-source ordinary TrainerService smoke used 10000 positions,
two epochs and 158 optimizer updates, generated 0 games, and reduced held-out loss
from .207771 to .148061 on 2500 separate records. Its isolated Candidate promoted;
existing Best stores were untouched. Exact resume, stored optimizer restoration,
source-reservation replay and strict model/training codecs are tested.

Software validation includes independent finite differences, tiny-set overfit,
material initialization, forbidden-input and perspective invariants, model immutability,
2000-ply cached/reference comparison, folded float parity, corruption/truncation/trailing
payload rejection, exact continuation, ordinary corpus service, GUI controls/layout and
NNUE evaluation-state/search boundaries. Integration batch:26 passed,0 failed,1 gated
skip; boundary batch:15 passed,0 failed. The skipped source-acquisition test had already
passed with the real source configured during the 15/15 prerequisite run. Counts
overlap across batches. Final GUI/backend, lineage/folder switching, legacy architecture
selection and strengthened trained-invariance checks pass 26/26. This includes actual
fresh GUI-backend training and a resumed generation, plus nonzero learned residuals
under forbidden-field and color/rank transformations. The first final switching run
failed because its new BRN-3 fixture had no Training Data source; registering a valid
source fixed the fixture without weakening the application's no-fallback guard.
The diagnostic failure and final passing XML are preserved, along with the themed
Swing render. Documentation links and the headless command's PowerShell syntax were
also checked. Tests do not establish population-wide model strength.

The full repository suite, manual desktop session, browser QA, long-time games, mature
NNUE comparisons and population-wide strength tests were deliberately not run.
Browser work is not required for these Java/Swing checks. No push/deploy occurred.

Starting and final HEAD remain `ab562e62815f955efbaf95063a877372aa2d9955`; no commit
is made across the inherited uncommitted corpus migration. All 50 inherited paths
are accounted for:46 unchanged, four with attributable BRN integration additions.
Original bytes and a separate review diff are in the evidence directory. New core,
research, tests and canon remain uncommitted for review. No unrelated source migration
change was absorbed or discarded.

The remaining research is optional: improve balanced-position calibration on new
validation evidence, test broader/game-disjoint corpora and longer time controls,
compare with mature evaluators, reduce inference latency and investigate qsearch
tails before any future production-qsearch work. The opened tests cannot become
tuning data. No remaining implementation item is required by the current V1 gates.

Root CODEXLOG_CURRENT.md is absent and was not created. VERSION_STATE.txt remains
build 33. The inherited operation `9e5cb01570284814a3a7f45a498ae785` is still blocked
with uncertain mutation and no verified bump. No new begin token was issued; the
old token was not finished, recovered or reassigned. This programme proceeds under
DEFERRED_FINALIZATION independently of authoritative build/release provenance.

Human actions required after this prompt: separate resolution/authority for the
inherited version-finalization failure is blocking for finalized build/release
provenance, not for local BRN training or research. User reconciliation/acceptance
of this work remains separate from passing the declared engineering gates.
