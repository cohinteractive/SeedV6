# NNUE / BRN-3 CGLHW contract

Established 2026-10-05 (Pacific/Auckland), baseline commit
`245a057`. Source of authority: the owner's pasted CGLHW request in this session.
Amended 2026-10-05 by the owner's explicit bootstrap-parity fairness decision.
Read this file and STATE.md before resuming. Do not treat historical BRN research
contracts as permission to weaken this programme's fairness rules.

## Objective and boundaries

Develop a substantially improved, from-scratch NNUE-family evaluator comparable
to current BRN-3 at Gen 0 and after controlled training. Explain the initial gap
before optimizing. Preserve the original NNUE and BRN-3 as reproducible references.
Do not alter BRN-3 semantics to ease the target. A separate, explicitly labelled
diagnostic ablation is evidence, not a replacement reference or a parity claim.

NNUE identity means a learned board representation maintained by cheap small-state
updates across moves, suitable for CPU search, with a meaningful incremental cost
advantage. Full unrestricted relational evaluation relabelled NNUE is excluded.

Architecture may encode board facts, geometric relationships, symmetry, shared
parameters, raw piece counts, and learnable features. Fixed handcrafted chess
evaluation is forbidden **except where necessary to reproduce, on equal terms,
fixed bootstrap knowledge demonstrably present in the BRN-3 reference evaluator**.
This exception permits only its existing material values and perspective, verified
from source (see BOOTSTRAP-PARITY.md). It does not permit stronger values, PSQT,
mobility, king safety, pawn structure, tactical or positional rules. No pretrained
weights, hidden pretraining/distillation, privileged data, lucky-seed selection,
extra updates disguised as initialization, or asymmetric search advantages.
Keep independent full-refresh and scalar correctness oracles.

Two separate research tracks remain active:

- **A — knowledge-free architectural control:** original NNUE without fixed
  material. All baseline and E001–E007 evidence belongs here. Those observations
  remain valid controls; they cannot reject the corrected practical comparison.
- **B — bootstrap-parity practical track (primary):** real unchanged BRN-3 versus
  NNUE with equivalent automatic fixed material. Retain each architecture's
  legitimate learned representation. Material stays separate from learned output,
  is maintained incrementally where sensible, and remains present after training
  on both sides. Removal is permitted only as an explicitly labelled control.

Preserve a material-off configuration, original mappings and initialization for
replay. Record fixed contribution, learned contribution, units, calibration and
clipping separately. Freeze values and material scale from BRN-3 source before
matches; do not optimize them against NNUE match outcomes. Baseline-reset matches
must isolate the prior addition before further architectural selection.

## Evidence and experimental rules

1. Audit fresh initialization, persistence, score mapping and shared search first.
   Reproduce the supplied strength observation on this Mac, retaining discrepancies.
2. Establish multi-seed, paired-opening/colour-reversed baselines. Seeds and openings
   are fixed before observing their outcomes. Preserve raw results, failures and
   administrative terminations; a ply cap is not a draw. Cluster uncertainty by
   initialization and paired opening, not by independent-game assumptions.
3. Record hypothesis, mechanism, implementation, configuration, seeds, parameter
   count, tests, results, interpretation and next decision for meaningful experiments.
   Do not retry rejected mechanisms without new evidence.
4. Funnel effort: diagnostics/correctness/cost -> small multi-seed matches -> stronger
   independent-seed matches -> small training -> serious equal-data training
   (approximately one million positions when justified and feasible) -> final checks.
5. Distinguish evaluator cost, search NPS/node counts, and playing strength. Warm up
   the JVM and use repeated robust timing summaries. Record actual search, depth,
   nodes/time limits, TT, threads, opening policy and evaluator mappings.
6. Training strict-control and architecture-tuned tracks remain separate. Same
   records, split, exposures, batch/epochs/update opportunity and shuffle control;
   same optimizer where technically meaningful. Disclose loss/optimizer differences.
   Tuned experiments receive comparable tuning opportunities and no final-test fishing.
7. Validate affected compilation, targeted tests, determinism and incremental/full
   equivalence. Do not run long suites by ritual. Record omitted checks and why.

## Acceptance thresholds (retained prospectively for the corrected track)

Historical Track A evidence, before the fairness amendment: six initialization seeds crossed with eight paired openings gave
NNUE score 0/96 at depth 3, with no caps. BRN-3 Gen 0 is seed-invariant material;
NNUE has nonzero evaluation variance but all six seed match means were zero.
Depth 2 gave 0 wins / 2 draws / 69 losses / 25 caps. This establishes a large gap,
not precise population variance: zero sample variance at a boundary must not
produce a zero-width confidence interval. See BASELINE.md and raw artifacts.

The original practical margin was chosen as one tenth of the Track A depth-3 gap:
`delta = (.5 - 0)/10 = .05` score units. This is a stated research judgment to
require closing at least 90% of the observed deficit, not a statistically inferred
universal Elo tolerance. Accept Gen-0 non-inferiority when the **one-sided 95%
lower confidence bound is at least .45**, over independently initialized networks
and paired openings. Outperformance is allowed. Confirm with at least 12 new
initialization seeds and new openings, using crossed seed/opening resampling or
another justified clustered inference; do not treat all games as independent.
Check between-seed heterogeneity and a second search budget/depth. Caps/failures
cannot be silently omitted to pass; use conservative missing-outcome bounds or
complete the affected games. Baseline seeds/openings are screening data only.
If small samples or degenerate outcomes make uncertainty unreliable, gather
additional evidence instead of declaring acceptance. No candidate results had
been observed when this rule was selected. The owner's fairness amendment retains
this .45 practical target and the cost gates prospectively, before any Track B
results. The old deficit motivates the historical choice; it does not estimate
Track B variance or prove that any Track B architecture is weak.

CPU guardrail: on common traces, warmed median incremental transition + inference
must be **at most 2x unchanged NNUE and at most 2/3 unchanged BRN-3**. Thus retain
at least a 1.5x advantage over BRN-3; current NNUE is roughly 3x faster. Two
baseline JVM runs gave NNUE 1.152/1.163 us and BRN-3 3.562/3.491 us. Re-measure
controls in the candidate JVM and independent repeats to control thermal/JIT
drift. Measure allocations, full refresh, king/capture updates, parameter footprint
and practical search throughput separately. Candidate hot inference/updates must
allocate zero, as both references do. Keep a real update-vs-refresh advantage;
an evaluator whose expensive head removes that advantage fails identity even if
one isolated timing passes. Do not tune these thresholds after candidate results.

Gen-0 parity alone does not complete the programme; controlled trained parity
and the serious approximately million-position comparison remain required when
feasible. Gen-0 equality from two zero heads plus identical material is an
accounting result, not evidence of comparable learned capacity. Distinguish such
identity controls from random-residual and trained comparisons.

## Authority and stop conditions

Routine reversible research, code, tests, local matches/training, durable records
and coherent local milestone commits are authorized. Preserve unrelated work;
do not push, deploy, destructively manipulate Git, or change shared/global tools.
Use Mac-local mechanisms. The later pasted request explicitly excludes **all
Finalizer workflows/tools** for this workstream, superseding their earlier local
invocation instruction. Do not create a root journal.

Continue autonomously across failed experiments and context boundaries. Human
input is warranted only for unavailable required dependencies, credentials,
external hardware/actions, consequential unresolved ambiguity or contradiction,
out-of-scope high-risk actions, or relaxation of fairness/identity rules.

Terminal outcomes, requiring evidence:

- **1 Success:** Gen-0 acceptance and cost guardrail satisfied across seeds, with
  controlled trained comparability including the serious million-position trial
  when feasible; do not stop at Gen-0 parity if trained gap remains.
- **2 Architectural limit / in-scope exhaustion:** substantial negative evidence
  across distinct justified families and understood mechanisms; difficulty alone
  is not evidence of exhaustion.
- **3 Fairness boundary reached:** evidence supports that remaining plausible
  routes require forbidden knowledge or advantages; do not assume impossibility
  merely because the current baseline has a material prior.
- **4 Genuine external blocker:** no meaningful work remains possible without a
  human-controlled dependency, including a consequential contract interpretation.

Final reporting must state classification, architecture, baseline/multi-seed/
training/million-position/cost evidence (or non-performance), negative findings,
fairness conclusions, changed files, validation and omissions, Git state, risks,
and required human actions (explicitly None if none).
