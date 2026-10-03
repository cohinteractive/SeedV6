# BRN learning-strength V1 result

**V1 criteria satisfied, 2026-10-03.** BRN-3 retains its architecture, fixed
material values and corpus training. Ordinary play now uses
`material + 0.25 * learnedResidual`. The change is confined to BRN-3 evaluation
calibration and its configuration/history identity. NNUE and shared search
semantics are unchanged. User acceptance and release finalization remain separate.

[Contract](../../brn/BRN_LEARNING_CONTRACT.md) ·
[Frontier](../../brn/BRN_LEARNING_FRONTIER.md) ·
[Chronological evidence](BRN_LEARNING_RESEARCH.md) ·
[Reproduction](BRN_LEARNING_REPRODUCTION.md) ·
[Evidence index](evidence/learning-2026-10-03/evidence-index.json)

## Where the chain failed

The original Windows Gen0..10 lineage was cumulative, with completed Gen1..9
history confirming poor scores against Gen0. Best staying at Gen0 did not reset
training. Generation losses used changing source populations, so their apparent
progress was not one comparable longitudinal generalization curve. On a fixed
reference population, raw half-MSE worsened from .04970 at Gen1 to .10153 at Gen9.
That population overlaps historical training and is not presented as new held-out
evidence. The optimized objective is expected-score cross entropy; the framework
reports half-MSE. Both were measured explicitly.

The learned residual can cancel the material prior almost completely on positions
reached during search and play. One losing Gen9 trajectory evaluates +.45 pawns
while down 18.4 material pawns, later +1.21 while down 19.4. On 2,243 early-game
states the raw prediction/material slope is -.0271. Among 912 states with at least
five pawns of material imbalance, raw predictions disagree with material sign in
630 cases; quarter-residual predictions disagree in none of those same states.
Material is a diagnostic reference here, not an oracle declaring every sacrifice
wrong. Legal-capture response changes in the predicted direction, but tempo and
tactical anticipation confound that proxy. Selected critical-root PV material
endpoints were mixed; the favorable examples alone would not justify adoption.

The controlled intervention changes only the residual's influence on the same
frozen weights. It preserves useful learned preferences while preventing the
observed near-total material cancellation in that sample. Gen9 reference CE gets
worse, .68086 -> 1.13528, while play improves and then replicates across models,
new openings, further optimization and production search. This locates the
practical failure at the translation from raw supervised evaluation into search
preferences. It is not a global unit conversion, nor evidence that a lower raw
loss necessarily improves play.

This is the strongest supported causal account, not a unique identification of
which upstream combination of target/link, sampling and representation created
the cancellation. Architecture replacement and optimizer reset were unnecessary
to recover useful learning. Scalar/cache/codec error was not found: 7,746 game
states agree within 5.53e-7 pawn. One-versus-12-worker scores agree on all 96
tested roots. Production qsearch is disabled, so its behavior cannot directly
explain the original production games.

## Playing strength and continued training

Every row below passes its predeclared point-score, confidence and completion
gates against material-only Gen0. Score is wins plus half draws divided by games.

| Frozen model / endpoint | Depth / workers | Color pairs | W / D / L | Score | Conservative 95% lower |
|---|---|---:|---|---:|---:|
| Original Gen1 | 4 / 1 reference | 64 | 102 / 18 / 8 | 86.72% | 71.42% |
| Original Gen5 | 4 / 1 reference | 64 | 101 / 16 / 11 | 85.16% | 69.86% |
| Original Gen9 | 4 / 1 reference | 64 | 102 / 17 / 9 | 86.33% | 71.03% |
| Independent seed97 / source range | 4 / 1 reference | 64 | 96 / 26 / 6 | 85.16% | 69.86% |
| Prospective +1 from Gen9 | 4 / 1 reference | 64 | 104 / 18 / 6 | 88.28% | 72.98% |
| Prospective +2 from Gen9 | 4 / 1 reference | 64 | 100 / 21 / 7 | 86.33% | 71.03% |
| Gen9, new-opening integrated confirmation | 6 / 12 production | 32 | 44 / 18 / 2 | 82.81% | 61.18% |

These are 832 completed games. The six depth-four runs deliberately share 64
openings; they are not 768 independent observations. The depth-six run uses 32
new openings and ordinary production construction. Its pair-bootstrap95%
interval is [76.56%,88.28%]. Results are conditional on the declared legal-uniform
opening population and fixed-depth budgets, not general Elo or equal-time strength.

Prospective optimization resumes the copied Gen9 model and Adam state at step
73,728, then reaches 81,920 and 90,112 using two new 131,072-example slices and
eight epochs each. A codec save/reload separates the endpoints; moments are not
reset. Paired differences from frozen Gen9 are +1.95 percentage points
(bootstrap95% [-2.34,+6.25]) and zero ([-5.86,+6.25]). The evidence establishes
retained utility over Gen0, not monotonic improvement or an unlimited trajectory.

Negative evidence remains visible. The initial seed97 run failed its cap1024
completion gate. A deterministic replay completed the long loss at ply1211;
the first1024 recorded plies match exactly. Cap2048 was then declared and the
whole 64-pair test rerun, preserving the failed report. No cap was scored a draw.
The initial full/quarter contrast is exploratory and small; its worst-case paired
bootstrap includes zero. Adoption rests on the complete confirmation programme,
not replacement of unfavorable outcomes or that contrast alone.

## Retained implementation and compatibility

The runtime policy is `BRN3_MATERIAL_RESIDUAL_QUARTER_V1` in
`Brn3SearchCalibration`, applied through `SearchEvaluation.brn3` and worker-local
`Brn3Workspace`. No new evaluator inputs, trainable material values or handcrafted
chess features were introduced. Raw model predictions, encoded weights, checkpoint
formats, gradients, masked Adam .003, batch128 and eight-epoch training remain
unchanged. An explicit gain1 research control reproduces historical evaluation.

The GUI explains raw corpus loss versus calibrated play. BRN-3 history and game
attempt settings include the calibration identity. A later start of an old
unfinished game-pair attempt follows the existing Restart Generation/archive
path, preventing mixed validation results; that unfinished attempt is not resumed
exactly. Completed payloads and held-out-only pending optimizer work remain
compatible. No original training store was started, restarted, promoted or edited.
Existing Best pointers therefore remain as they were. The Mac control was never
accessed and is not a dependency.

## Verification, limits and next work

- All 128 production static-leaf diagnostic roots finish for material, full and
  quarter controls. All 96 worker-count comparisons agree in score.
- In separate opt-in unpruned qsearch, 31/32 quarter roots finish within five
  seconds under concurrent load; the recorded deadline failure is retained.
  The isolated repeat of the slow root finishes in 4.102 seconds, 654,180 nodes
  and maximum qply29, versus 263,103 nodes for full residual. Its roughly 2.5x
  node tail is a real cost limitation before any future production-qsearch work.
- 39 distinct targeted JUnit tests pass: BRN training/resume/codecs, calibration,
  information boundary, search parity, restart/archive, GUI and neighboring NNUE
  evaluation/search/training. The headless Swing render was visually inspected.
  The evidence package contains exact cases and source XML hashes.
- Original/copied hashes still match for all 15 archived payloads; original seek
  metadata is unchanged. Source inspection and Git comparison show no NNUE or
  recursive/shared search changes. Whitespace verification uses `cr-at-eol` for
  the repository's existing CRLF source convention.

The full suite, expensive NNUE performance/training programme, application
packaging, manual desktop operation, mature-NNUE competition and long-time games
were not rerun: the relevant boundary tests and controlled experiments cover this
change, while those broader claims are outside this milestone. Historical data
overlap and source-order/game correlations limit generalization. No full 22GB
source checksum or universal tactical guarantee is claimed. The independent
Mac trajectory may later strengthen or revise the interpretation.

Only deferred research remains: distinguish upstream causes; test training-time
regularization versus runtime calibration; broaden sources/openings/time controls;
study much longer continuation and mature NNUE; investigate qsearch tails before
any separate shared-search change. None is used to disguise a failed V1 gate.

The working unit began at clean `b14b804`; its code, tests, tools and canon are
attributable to this programme. Large generated datasets/checkpoints stay ignored.
The pre-existing version operation `9e5cb01570284814a3a7f45a498ae785` remains
blocked with uncertain historical mutation and no verified bump. VERSION_STATE.txt
remains byte-preserved at its observed build33; this runtime change warrants a
future finalized bump but no new token could begin. No recovery, manual version
compensation, push or deployment was performed. CODEXLOG_CURRENT.md is absent
and was not created. This is completed research under DEFERRED_FINALIZATION,
not a finalized build or a release.

Human actions required after this prompt: None for the completed research work.
A future release requiring finalized identity remains blocked until the prior
version reservation is reconciled under the authorized recovery protocol.
