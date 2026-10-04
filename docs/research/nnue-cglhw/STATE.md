# CGLHW state / experiment frontier

Updated 2026-10-05. Status: **bootstrap complete; architecture frontier active; no terminal outcome**.
Baseline: clean `main`, `245a057` (also origin/main when inspected).
No candidate exists; current NNUE and BRN-3 production code remain unchanged.

## Environment and mechanisms

MacBook Air arm64, macOS Darwin 25.4.0; Temurin Java 21.0.12.1+1.
`./gradlew :app:classes :app:verificationClasses --console=plain` passed.
Production source: `app/src/main`; non-shipped tools: `app/src/verification`.
Gradle JavaExec tasks include `nnueResearch`, `nnuePerformanceBenchmark`,
`brnDiagnostic`, `nnueCorpusTrain`, `brnCorpusTrain`, `developerTools`.
BRN-3 corpus entrypoint also exists in verification as `Brn3CorpusMain`.
Learning Arena (`training/service/LearningArena*`) supplies frozen shared tranches,
fresh initialization, round-zero matches and resumable equal-record campaigns.
Main search: SearchDriver -> ExactSearchAdapter -> ExactSearch; both neural
definitions use the exact policy; HCE-only calibrated pruning does not apply.
HeadlessGame supplies legal moves, history and genuine chess terminations.

## B001: initialization/fairness audit (complete)

**Hypothesis:** observed Gen-0 gap may be predetermined evaluation knowledge,
not superior random-network topology.

Source evidence:

- `core/brn3/Brn3Trainer`: fresh constructor randomizes relational/unary/dense
  weights, but leaves readout weights and output bias exactly zero; step=0.
- `Brn3Features`: fixed P=1, N=3.2, B=3.3, R=5, Q=9, K=0 pawn units.
- `Brn3Workspace`: adds fixed material to readout. Production uses
  material + .25 * residual, mapped to 100 score units/pawn.
- Therefore BRN-3 Gen 0 is material-only independently of random seed. Random
  representation smoothness cannot influence its Gen-0 scores through a zero head.
- `NnueNetwork.initialized`: HalfKP 49,152 rows x 64; concatenated 128 -> 32 -> 1;
  all weights independently uniform [-1/64,+1/64], zero biases. clip01 twice,
  tanh output, full-range sign-preserving integer mapping (scale 32,511).
- LearningArenaTraining.fresh invokes precisely these constructors; no load path.
- Arena defaults also differ: NNUE Adam .001 vs BRN-3 masked Adam .003 and different
  losses. Equal positions/epochs alone is not identical optimization treatment.

**Interpretation:** both can be untrained, but they are not equally free of chess
evaluation knowledge. This is an intentional existing BRN design, not a newly
discovered accidental implementation defect. Do not change BRN to make a target
easier. No conclusion of NNUE impossibility is justified from source audit alone.

**Optional clarification pending:** an asynchronous question offered unchanged
BRN-3 as a knowingly knowledge-advantaged practical target or a separately
labelled no-material research reference. The original instruction already clearly
authorizes retaining unchanged BRN-3. **Proceed with that target**; do not invent a
blocker while a different reference remains unauthorized. Neither production BRN-3
nor the reference is being changed. If the user later redirects, record that
decision before changing the target. There is no current human blocker.

## Selected next work

Completed B001/B002/B003: see BASELINE.md, summary.json and baseline-* artifacts.
192 baseline games plus 16 deterministic replay games; 7,680 sampled evaluations
per audit pass across six seeds; two separate warmed cost runs. Original code
hashes retained in provenance.json; the second audit metadata hashes the extended
tool. Twenty-three targeted tests pass. No application evaluator code changed.

Frozen comparability/cost gates now in CONTRACT.md, set before candidates.

Next experiment frontier, ordered by explanatory value:

1. **H001, perspective-consistent output head.** Actual White-relative quiet-move
   correlations range -.638 to .097; holding STM fixed gives .892 to .945.
   Shared HalfKP placement is already locally smooth. Test a learned antisymmetric
   readout to remove viewpoint inconsistency, with no fixed desired chess values.
   Measure king sensitivity, runtime cost and multi-seed strength separately.
2. **H002, shared/factorized representation.** King movement still reduces same-STM
   correlation to .146..603 and valid file reflection gives -.171..095. Test
   shared piece/square factors or geometry tying that preserve incremental sums.
   Do not confuse this with fixing the reference's material knowledge advantage.
3. **H003, score quantization / initialization scale.** Baseline mapped outputs
   span only -10..12; nearly half clipped units are zero but none saturate at one.
   Isolate quantization from architecture with explicit research-only mapping
   controls and scale-free/raw topology. No asymmetric search parameters.
4. **H004, learned global piece-count pathway.** Raw counts are admissible, fixed
   material values are not. Test parameter sharing with zero-centred random heads,
   preserving seed distributions rather than selecting desirable piece rankings.
5. Broaden families from evidence, then training only for a credible contender.

Deferred: trained runs, million-position experiment, external research, application
promotion. No corpus dependency is currently blocking research. No exhaustion,
fairness-boundary terminal condition, or success has been established.
