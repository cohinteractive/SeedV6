# BRN learning and playing-strength research contract

Revision 3, 2026-10-03 (Pacific/Auckland). Current programme authority under the
user's new research instruction; [frontier](BRN_LEARNING_FRONTIER.md) is its
execution plan. The completed [viability contract](BRN_CONTRACT.md), V1 result and
operator guide remain historical engineering evidence, not acceptance for this
new learning-strength question.

## Objective and established evidence

Explain where training objective -> static evaluation -> search -> playing
strength breaks, and validate the smallest justified intervention that makes
continued BRN learning useful relative to material-only Gen 0. Do not require
victory over NNUE. Do not presume defective architecture, excessive scale,
incorrect labels, or insufficient generations.

User-supplied evidence is established: a cumulative BRN-3 Gen 0..10 lineage,
approximately 131072 positions and eight epochs per generation, depth-six games
and eventually 64 opening pairs, retained Gen 0 as Best with almost all trained
candidates below 50%. Rejection did not reset parameters, Adam moments, step or
corpus consumption. The Windows lineage's exact Gen0..10 immutable artifacts and
Gen1..9 completed history were subsequently captured read-only and confirm this.
An independent fresh depth-four, eight-thread Mac lineage is an external control;
never inspect or alter its state/process/stores. Its completion is not a dependency.

The old viability programme succeeded. Its fresh-NNUE comparisons do not answer
whether BRN training improves on Gen 0 or remains useful through continuation.
Do not rerun old acceptance merely for ceremony.

## Invariants and authority

- Preserve NNUE behavior and ordinary shared alpha-beta/search semantics.
  Instrumentation and isolated existing search research seams are permitted.
  A shared correctness fix changing those semantics requires human authorization.
- Production BRN inputs are only piece identity, ownership, location, relational
  geometry, side to move and fixed material relationships. No handcrafted
  positional/tactical features, attack maps, mobility, pawn/king terms or tables.
  Offline chess classifications, legal moves, search traces and teachers are
  scientific observations/labels, never permission to add runtime inputs.
- Preserve fixed material P=1, N=3.2, B=3.3, R=5, Q=9, K=0 separately from the
  learned residual, zero residual initialization and coherent pawn-relative units.
- Corpus-backed training remains normal. Do not revive costly routine generation.
- Architecture/training/targets/loss/sampling/optimizer/residual mechanisms may
  change autonomously only when discriminating evidence justifies them. No
  speculative production redesign, unrelated refactors, pushes or deployment.
- Experiments use new isolated outputs; historical models and normal Best remain
  read-only. Record checkpoint, optimizer, source/split, exposure and search identity.
- CONTRACT may evolve within these boundaries; FRONTIER is dynamic. User acceptance
  and release/build finalization are distinct from scientific or turn completion.

## Evidence and acceptance

Use controlled interventions with predicted intermediate effects, then games and
continuation. Retain negative results, administrative failures and uncertainty.
Never count a ply cap or failed search as a draw. Use color pairs and report
uncertainty by opening pair; repeated seeds/openings are not independent games.
Separate exploratory selection from new confirmatory openings/source ranges.
Depth and equal-time controls answer different questions and must be labeled.
Use existing validation partitions for exploration; old opened sealed tests cannot
be reused as new final selection evidence. Choose proportionate quantitative gates
from observed variance/cost and freeze them before confirmation.

V1 requires: a supported causal explanation with explicit limits; prediction and
measurement of the mechanism's response to intervention; reliable production
search and acceptable separate qsearch diagnostics; actual trained-versus-Gen-0
playing improvement replicated beyond one favorable small sample; continued
optimization that credibly retains/adds utility over multiple later checkpoints;
correct training/resume/codecs/configuration and relevant GUI integration for any
adopted design; intact NNUE, information boundary and material anchor; durable
methodology, provenance, uncertainties and deferred work. Loss alone is insufficient.

The retained architecture and training remain BRN-3 with eight epochs, minibatch128,
masked-Adam .003 and expected-score cross entropy. Evidence supports retained
play calibration `material + .25 * learnedResidual`, separate from the raw
supervision output. All V1 gates are satisfied; the programme is closed at this
milestone. See the [result](../research/brn/BRN_LEARNING_RESULT.md) for scope and limits.

### Quantitative gates derived during research

Frozen-checkpoint confirmation: Gen1/5/9 of the historical Windows lineage and
an independent seed/source model each64color pairs at depth4; all games complete,
score>=.55, individual one-sided Hoeffding95% lower>.5. Prospective utility:
two further eight-epoch endpoints, each131072new examples, same64paired openings
for matched continuation assessment, same score/confidence/completion gates.
These gates have passed. They do not imply all generations must improve monotonically.

Final integrated depth gate:32new color pairs, depth6/12production workers,
score>=.65, one-sided Hoeffding95% lower>.5 and all games complete. Cap2048 is an
administrative limit, never a draw. Its revision from1024 followed an explicitly
retained failed seed97 test and a deterministic1211-ply conversion, not a favorable
outcome replacement. Source, budgets, seeds and original failures are in the record.

Correctness requires targeted BRN/NNUE integration and resume checks; old partial
game validations must not silently mix calibration versions. Search128root
completion,96root one/12worker score agreement and separately reported qsearch
tails ground the mechanism's runtime limits. Production qsearch remains disabled.
No general Elo, mature-NNUE parity or universal qsearch guarantee is required.

## Cost, continuity and stopping

Execute one coherent frontier question at a time, using the cheapest reliable test.
Before expensive runs, record question, insufficiency of cheaper tests, predicted
outcomes, budgets/stop conditions and disposition. Do not duplicate the Mac long
trajectory. Update canon progressively; reread contract, frontier and relevant
evidence after context loss, and inspect Git state.

Stop only on V1 acceptance, only explicitly non-V1 work remaining, or a genuine
human gate. Missing exact historical checkpoints is not automatically blocking
when isolated reproducible controls can discriminate the mechanism. If missing
evidence prevents a required conclusion, report that limitation rather than infer it.

## Bootstrap challenge

1. The old contract targets parity with fresh NNUE, whereas this contract tests
   learning against its own strong material control. Historical .99 versus NNUE
   is not evidence against the new failure observation.
2. Production neural search currently has static leaves and qsearch disabled.
   Qsearch pathology cannot directly explain production games on this code.
   The existing opt-in qsearch seam is a separate diagnostic, not a silent fix.
3. Production optimizes CE but reports half-MSE. Measure both rather than equating
   a framework loss with the differentiated objective.
4. Aggregate corpus loss can conceal behavior on search-selected leaves; inspect
   both populations, aligned perspectives and individual trajectories.
5. Prior repository canon describes inherited dirty files at an older HEAD. This
   root starts clean at b14b804; do not carry that stale ownership statement forward.
6. A blocked prior version reservation permits independent research under the
   supplied DEFERRED_FINALIZATION rule, but cannot confer finalized provenance.

Outcome: **V1 criteria satisfied**. The supported mechanism is
learned material cancellation that generalizes badly to search-selected positions;
residual-only calibration preserves useful learned information across historical
and prospective continuation. It is not established as the only upstream cause.
The final depth-six/12-worker gate scores .828125 (44W/18D/2L), all 32 pairs
complete, conservative 95% lower .611773. The isolated qsearch tail completes
in 4.102 seconds with identical node counts to the earlier longer-budget replay;
its increased cost remains explicit. All 39 distinct targeted software tests pass.
Only deferred research remains. User acceptance and build/release finalization
are separate; the pre-existing blocked version reservation is preserved.
