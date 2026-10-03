# BRN viability programme contract

Revision 1.1, 2026-10-03 (Pacific/Auckland). This repository master governs the
new BRN programme within the current user's instructions. It does not change
shared governance, other Seed repositories, or the existing search contract.
See [frontier](BRN_FRONTIER.md) and [ledger](../research/brn/BRN_PROGRAMME_LEDGER.md).

## Purpose and boundary

Determine whether a learned evaluator whose primary representation comprises
pieces, locations and relationships can provide approximately fresh-NNUE-level
usefulness inside SeedV6. Initial success is viability, not superiority. Existing
BRN-0/1/2 models are historical controls, not an architectural specification.
Failure is limited to the architectures, evidence and resource envelope tested.

The authoritative implementation boundary is this SeedV6 repository. Related
repositories are read-only evidence. Preserve NNUE inference, training kernels,
checkpoint compatibility and normal behavior. Generalize shared infrastructure
only when necessary, with explicit NNUE regression coverage. Search, move generation,
board representation and general GUI redesign are excluded except for a demonstrated
architecture-neutral defect or bounded evaluator integration requirement.

## Innate information and score semantics

Permitted inputs are piece type, ownership, location, board geometry and side to
move. Learned embeddings of these facts and their relationships are permitted.
No innate attacks, mobility, legal-move counts, defended state, tactical labels,
pawn structure, king safety, positional tables, development or other handcrafted
positional terms. No castling rights, en-passant, clocks, history, repetition,
Zobrist keys, search state, or another evaluator's output enters runtime features.
Teachers and tactical classifications may be used for labels or diagnostics only.
Tests must vary forbidden board fields and confirm identical BRN inference.

Material is a fixed, separately observable prior, with pawn-relative units:
P=1, N=3.2, B=3.3, R=5, Q=9, K=0. Evidence is the basic SeedV6 evaluator at
commit `c433256`, `app/src/main/java/com/ohinteractive/seedv6/core/Eval.java`,
whose five literal values are 100/320/330/500/900. This agrees with the later
`Brn2MaterialPrior.BASIC_V1`. The sibling `seedv1` contains move-generation
prototypes, not an evaluator; the later phase-aware evaluator's SEE values
(including queen 975 and king 20000) are a different purpose and are not substituted.
The old basic evaluator's bishop-pair bonus is excluded. Kings retain identities.

The new evaluator is `material(us)-material(them)+learnedResidual`, all in pawn
units. Initialize the residual to zero deterministically. Keep material outside
any saturating combination with the residual. At the engine boundary one pawn
unit maps to 100 integer score units; rounding must be symmetric and clamp only
to the existing normal-score band to protect mate scores. Learned positional
effects may outweigh material when supported by data. Do not cap compensation
at a pawn, piece or queen merely to enforce material dominance.

Use side-relative identities and rank-reflected Black coordinates. Test exact
color exchange plus rank reflection plus STM exchange invariance. STM reversal
alone is not chess equivalence: tempo may be learned, but perspective inconsistency
must not produce artificial alternating offsets. For architectures imposing odd
STM symmetry, document the lost tempo capacity and test the exact identity.
All parent/child deltas must first express both scores from the same side.

## Corpus and fair controls

The prerequisite is a usable, reproducible data-backed path for both NNUE and
BRN. Corpus migration belongs to a separate workstream. Never substitute costly
training-time game generation. The 2026-10-03 recheck passed both architectures'
production optimizer/resume tests and bounded reads from the local Lichess archive.
Current acquisition uses `SourceReaders` and `SequentialTraining`; it does not
require reconverting the archive or preparing a whole-source index.

Research must explicitly bind source identity, raw range, eligibility, ordered
examples, target policy and split hashes. Use the common CP-eligible population
for matched comparisons; NNUE ordinarily also accepts mates while BRN-2 excludes
them, so merely requesting equal counts from their default adapters is NOT a
matched comparison. A bounded research sample/manifest through existing readers
is in scope; implementing the missing source migration is not.

Group exact positions by permitted information (including equivalent color/rank
transforms) before assigning splits. Differing clocks/rights must not create
independent examples with identical BRN inputs. Use a stable hash-based train,
validation, sealed-test assignment and identical examples for the fresh controls.
No test-selected tuning; validation chooses checkpoints and hyperparameters.
Record duplicates, rejected labels, disagreements and source-order correlation.
The source lacks game identities; never claim game-disjoint or independent sampling.
Use separate source ranges for replication and report residual correlation risk.

Fresh NNUE uses its existing initializer, optimizer, feature kernels, output and
score mapping. Use at least two run seeds for final evidence. Mature NNUE Best is
only an optional external reference/teacher/opponent, never the parity control.
Report quality versus examples AND training time; count repeats as exposure.
Keep architecture-specific targets explicit: CP regression is meaningful for
BRN; NNUE expects an outcome target. Compare both in the existing frozen
STOCKFISH_WDL_V1 outcome space on identical CP labels, mapping BRN predicted CP
through the same evaluation-only adapter. Never reinterpret NNUE raw output as CP.
Direct CP errors remain BRN calibration diagnostics; do not invent NNUE CP errors.

## Predeclared V1 acceptance gates

These are engineering equivalence tolerances, not a claim of statistical equality.
They are fixed before candidate evaluation. Any revision requires new evidence,
a recorded contract decision and disclosure of affected prior comparisons.

1. **Correctness:** finite inference/gradients; independent finite-difference checks;
   deterministic material initialization; forbidden-input and perspective tests;
   tiny-set overfit; serialization and optimizer-resume agreement. No silent fallback.
2. **Learning and held-out quality:** final matched exposure at least 131072 distinct
   training positions, at least 16384 sealed-test positions, two fresh seeds. Across
   seeds BRN outcome half-MSE at most 1.10 times fresh NNUE, with a position-group
   bootstrap upper 95% ratio at most 1.20. Also report near-balanced (|label|<=200 CP)
   and material/phase strata. BRN must improve at least 5% over its own material-only
   baseline in outcome loss and improve CP MAE; parity with an unlearned control is
   insufficient. Both models must demonstrate nontrivial learning. Report error
   quantiles, sign mistakes, residual magnitude, calibration and exposure curves.
3. **Local stability:** report quiet, capture, recapture and single-square/piece
   perturbation deltas, in a common perspective. No nonfinite values or systematic
   color offset; report median/p90/p99/max and the fraction exceeding a queen.
   Large legitimate tactical changes are investigated, not automatically clipped.
4. **Production search:** at least 128 fixed held-out roots, depth 4, identical
   production settings and explicit node/time safeguards. No BRN-specific failures
   or incomplete searches where the control completes. Aggregate entered nodes
   <=2x NNUE; at most 5% of roots may exceed 10x NNUE nodes. Investigate
   tails and score saturation. Compare the production `SearchDriver`, not legacy
   diagnostic search implementations. NNUE search semantics remain unchanged.
   Current production has static leaves and no qsearch. Its zero qnodes cannot
   establish qsearch stability. Separately use the existing opt-in
   `ExactSearch.quiescenceResearch` seam on bounded roots and compare qnodes,
   completion and tails against NNUE; do not enable qsearch in production.
5. **Practical play and compute:** at least 100 paired openings at equal per-move
   compute/time, reversed colors, against the fresh NNUE control. BRN point score
   >=0.45 and paired uncertainty reported (one-sided 95% lower bound >=0.40).
   Report matched-depth as a separate diagnostic when useful. Measure warmed
   evaluation latency, NPS, parameter/payload size, memory, training cost and
   update cost. BRN throughput must be >=0.25x NNUE; equal-time games are the
   controlling practical test of the remaining speed/quality tradeoff.
6. **Usable V1:** fresh initialization and training through the ordinary data-backed
   `TrainerService`, headless reproduction, immutable checkpoint codecs/loading,
   architecture-specific configuration and sufficient existing GUI integration.
   Preserve accepted NNUE behavior with targeted tests. Durable operator instructions
   and evidence must reproduce the accepted architecture without a supplied trained BRN.

The loss tolerance allows modest seed/training variance; requiring material-baseline
improvement guards against a trivially weak fresh control. Node-tail gates address
the observed earlier BRN qsearch pathology. Game pairing and equal time keep lower
latency and chess utility visible rather than declaring success from label fit.

## Experimental funnel and stopping

Start with architecture sanity, then tiny overfit, micro-training (up to 16384
distinct positions), representative training, and only then serious comparison.
Use the cheapest test capable of rejecting a hypothesis. Compare normalized
pair potentials, nonlinear relation pooling and message-passing only as evidence
warrants. These are hypotheses, not a required architecture sequence. Concentrate
on a promising family. Do not run expensive experiments to complete a checklist.

For each unit record hypothesis, candidate/version, change, source/split/exposure,
parameters, software checks, actual measurements, interpretation, disposition and
next work. Update the frontier. Preserve rejected evidence and existing user assets;
use isolated outputs, never overwrite normal Best stores. One coherent unit at a time.

Stop only for V1 success, sufficiently supported narrow non-viability, or a genuine
blocker: missing acquisition prerequisite/access, unresolved material authority or
unsafe concurrent work, required validation unavailable, need to breach the information
boundary, unapproved destructive action, major scope change or unjustified resource
increase. Several failed candidates are not non-viability. Preserve resumable evidence.
No push/deploy authorization is implied. Acceptance remains with the user.

## Bootstrap challenge (completed before implementation)

- Removed BRN-2 status features entirely from candidate designs; its encoder is
  not a permissible base. Learned square embeddings have no hand-authored scores.
- Rejected CP/WDL conflation and post-hoc NNUE-to-CP fitting; prior calibration
  studies already falsified that route. Fixed material stays outside tanh.
- Equal corpus counts do not imply matching examples because mate policies differ.
  Record common eligibility and hashes; deduplicate by allowed input, not full FEN.
- Sequential neighboring slices are not adequate independent held-out evidence.
  Assign stable position groups before experiments and retain a sealed test split.
- Pooling normalization is a candidate variable: mean aggregation may erase useful
  cardinality while unnormalized relations can amplify steps. Measure both cheaply.
- Color equivalence differs from STM reversal. No arbitrary test of sign equality
  across an actual move; first align score perspectives.
- Existing `Brn2Diagnostics` uses legacy search. New acceptance uses production
  `SearchDriver`; diagnostics must not silently inherit the obsolete path.
- GUI and full integration are deferred until cheaper evidence supports a family.
  The final V1 still requires them. Existing source migration is inherited work.
- Version reservation is separate from model validity. Deferred finalization permits
  research but no claim of verified build provenance or a release dependent on it.

Revision 1.1 corrects a verified search-boundary fact before representative candidate
evaluation: production disables qsearch. It preserves the user's diagnostic requirement
through the existing research seam instead of silently claiming success from zero counts.
