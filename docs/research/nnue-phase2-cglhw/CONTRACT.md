# SeedV6 NNUE Strength Research — Phase 2

Owner-authorized CGLHW, initiated 2026-11-14. Revision 1. This contract governs
this research only. Repository instructions and the owner's Phase 2 assignment
remain authoritative. Operational findings and selection belong in STATE.md and
prospective experiment protocols, not amendments to historical experiments.

## Objective and boundaries

Develop and evaluate substantially stronger, computationally efficient NNUE
evaluators, maximizing practical equal-time strength under unchanged SeedV6
search. Architecture-led research combines compact depth and chess relationships
with controlled training/data studies. No Elo gain is guaranteed or required for
an honest research conclusion.

WDL25/build101/source08877838 is the protected production incumbent and primary
comparator; CP64 is a historical control/rollback asset. Never modify production
weights/defaults, deploy, change BotLi, or access/change the running Mac workload.
Preserve chess mechanics, search, CLV_C1 flags3329490943, BOUNDARY2, logit400
scaling and all prior evidence. Experimental evaluators, codecs, accumulators and
training tools may change in isolated research sources. No shared governance,
CGLHW generator, unrelated infrastructure or Search canon maintenance is authorized.

## Evidence and research principles

Freeze each experiment's hypothesis, controls, identities, endpoints, resource
budget and decision before observing its results. Distinguish local observations,
external claims and hypotheses. Reuse completed evidence with its limitations;
never rewrite frozen protocols or treat exploratory selection as confirmation.
Seeds, exposure counts, data hashes, model/runtime hashes and actual commands must
permit reconstruction. Retain failures, negative results and partial work. Raw
output remains ignored under raw/; compact reports, recipes and manifests belong
in Git. Fresh sessions must check local evidence availability before analysis.

Audit data provenance, exact/symmetric duplicates and near-neighbor contamination.
Prefer verifiable game/source-separated holdouts. State what cannot be established;
position separation alone never establishes game independence. Training loss and
held-out prediction loss are diagnostics, never substitutes for playing strength.
Architecture rejection requires adequate training or an independently demonstrated
implementation/cost limitation; failed individual recipes do not reject a family.

Cross-check feature extraction, merged/quantized export and integer arithmetic
independently. Incremental inference must agree with full recomputation through
king-context changes, special/null moves, siblings/undo, qsearch and worker transfer.
Incorrect inference invalidates downstream strength testing. Targeted checks are
normal; a full regression suite requires a concrete risk-based justification.

Measure warmed JVM update and downstream costs separately where needed, allocation,
memory, refresh frequency and whole-search throughput. Record warmup/runtime and
hardware conditions. NPS changes caused by a changed search tree are not intrinsic
kernel speedups; equal-node play cannot replace equal-time evidence. No hidden
search retuning to compensate for evaluation differences.

## Autonomy, economics and decisions

Use the existing owner compute controller and local Windows resources only, one
expensive workload at a time. Respect HOLD/PAUSE and actual resource contention.
Long work uses deterministic process supervision, persistent logs and resumable
checkpoints. No routine conversational counter updates. Before expensive work,
establish a bounded information-bearing pilot and its escalation conditions.
Budgets can be revised prospectively for a concrete research reason, never to
continue sampling until a favorable result or to consume remaining allowance.

CONTINUE when credible strength or efficient-capacity evidence warrants it. REVISE
when a specific economical correction can discriminate the remaining hypothesis.
DEFER/ABANDON when adequate evidence, impractical cost or poor information value
removes a credible useful next step. Ordinary experimental choices are autonomous.

Screen at fixed endpoints on paired openings primarily against WDL25; report paired
uncertainty and censor caps. Reserve independent unused openings for confirmation.
Before any confirmation freeze the candidate, sample, settings and family-adjusted
decision gates. No interim efficacy stopping, pooled development/confirmation
claim or repeated retesting of an unlucky result. Longer clocks/multiple workers
are required evidence for corresponding suitability claims, not every pilot.

## Completion and human boundaries

Completion requires a defensible synthesis of WDL25 reproduction and important
training factors (or evidenced isolation limits), improved data-independence
knowledge, meaningful compact-depth and relational experiments (or evidence that
pursuit is unjustified), correctness/efficiency evidence for implemented candidates,
equal-time strength for competitive candidates, and explicit disposition of all
material residuals. One early winner, elapsed time, experiment count, tokens or a
session boundary cannot establish completion. Negative findings may support a
completed research programme; unknown strength must remain unknown.

Stop affected work for unsafe ownership, contradictory material prerequisites,
unavailable required validation or a decision changing authorized scope. Continue
independent permitted work. Resource problems should first receive an economical
within-scope correction. Production promotion/deployment, shared-policy changes and
otherwise unauthorized external actions require separate owner authorization.
Research completion is neither owner acceptance nor production promotion.

Threadline CREATE at a verified bootstrap milestone and UPDATE at material
resumable milestones use the pinned Bundle V1 workflow, if available. Repository
continuity is primary. Never auto-Apply or infer CLOSE from a session ending.
