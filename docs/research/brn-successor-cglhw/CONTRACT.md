# BRN evaluator successor CGLHW

Revision 2, 20 October 2026 (Pacific/Auckland). Authority: the owner's launch
request to establish and execute a GUI-trained evaluator research programme against
BRE-Pair 2 Gen 32. Start at [STATE](STATE.md); initial decisions and challenged
assumptions are in [BOOTSTRAP](BOOTSTRAP.md). This is a new programme, not a rewrite
of the completed `brn-architecture-cglhw` experiments or the Search programme.

Continuation amendment: the owner has supplied substantial W1 training and
preliminary adverse Arena results. First-wave implementation is complete; do not
restart that programme. Investigate baseline provenance, implementation, effective
optimization, data, calibration and search confounds before major redesign.
Use the existing STATE frontier as a hypothesis-driven selection mechanism.
Negative W1 observations do not establish architectural inferiority; useful extra
expressiveness must ultimately contribute to independently credible strength over
frozen Gen32. Resource limits and the user-only substantive training boundary remain.

## Objective and authority

Find a compact, practical, distinctive evaluator with credible playing-strength
improvement over the frozen reference under controlled equal resources. Expressive
nonlinear, conditional and higher-order chess relationships are hypotheses, not
automatic improvements. Training throughput and inference cost are separate axes;
consumer-hardware practicality does not mean restricting substantive training to
short Codex runs. NPS, latency, size, depth, parameter count and loss diagnose
strength; none replaces it. A credible small gain can merit adoption consideration.

Follow root AGENTS, module ownership, checkpoint compatibility and Search correctness
contracts within their scope. The owner's current request expressly authorizes new
GUI candidates and new managed lineages, and reserves substantive training to the
user. Older completed research's no-GUI/no-user-store and autonomous serious-training
rules do not govern this new programme. No shared policy changes or Search phase
activation. Root AGENTS prohibits finalizers and bumps build only in a qualifying
application commit. Commit attributable coherent units; never push or deploy.

## Scope and research principles

Core owns inference, training owns optimizers/continuation/corpus services, GUI owns
presentation; research owns runners and evidence. Reuse existing capabilities.
Implement approximately three or four substantially distinct first-wave candidates,
with shared infrastructure where useful. Each has a hypothesis, versioned configuration,
lineage, independent checkpoints, inference representation and evidence. Distinguish
architecture from features, initialization, calibration, exposure and training objective.
Preserve failed and superseded approaches. Later waves may refine, replace, combine,
simplify, distill or retire mechanisms using evidence; do not stop at the first failure
or first winner. Consider piece-centric, attack/defence, polynomial, factorized,
conditional, specialized and hybrid models as justified. The nonlinear control is
explicitly a control, not a claim to have reinvented NNUE.

Record permitted information and expected inference/memory/incremental cost before
implementation. Attack geometry, blockers, pins, king safety, pawn structure and
coordination may motivate later explicit feature experiments; no requirement to add
all of them. Teacher outputs never enter inference. No unrelated GUI, packaging,
search, tournament policy or subsystem redesign. Narrow correctness/integration
repairs require direct evidence. No production/default evaluator replacement without
the owner's adoption decision; preparation for future Search research is in scope.

## Frozen reference and comparison

Reference: `BRN_PAIR2` v1, compiled C02 order-2 tables; lineage
`235d0688-e060-44e3-8f1e-119c39bc8017`; checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`;
network SHA-256 `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Tracked runtime: `build-assets/production/bre-pair2-g32.brn-pair2`, 795032 bytes.
Search mapping: side-to-move material + .25 * residual, times 100, half-away-from-zero
rounding, capped at +/-32511. Preserve real-ply mates, score perspective, numerical
safety and evaluator identity. CP labels alone do not prove empirical calibration.
Reverify bytes/semantics/integration before comparisons; never substitute Best/latest.
Unavailable or ambiguous reference blocks dependent comparison.

Use a recorded common search implementation/configuration on both sides. Current
Pair-2-only Policy 4 activation must not silently give different algorithms to the
competitors. Evaluator isolation and later production integration are distinct
experiments. Do not inherit HCE pruning margins or evaluation-sensitive policies
without justification. No unrelated search optimization to rescue a candidate.

## Training and human gate

Create/configure new lineages automatically through existing workspace mechanisms;
do not move, rewrite or train existing user lineages. Preserve all networks, pointers,
source cursors, histories and checkpoints. Use compatible existing target adapters
(CP/WDL and explicitly labelled BT4 Q), not mandatory imitation of Gen 32. Consider
outcomes, stronger teachers, move ranking or targeted relations if evidence warrants.
Record source/target/split identity and limitations; validation and independent match
assessment cannot become training data. No invented game-disjoint claim for sources
without game IDs. Freeze confirmation openings separately from development choices.

Only tiny bounded smoke training is authorized for Codex: initialize, check data,
forward/gradients/updates, finite deterministic outputs, tiny fit, persistence/reload,
exact optimizer continuation and service pause/resume. Each smoke job <=5 minutes,
<=2 GiB heap, one CPU process; no repeated generations seeking competitive strength.
Substantive training belongs to the user through Network Training. Validate GUI
selection/configuration and engine loading/Play/Arena, with native Swing evidence
where relevant; headless tests cannot be called visual acceptance.

At a consolidated wave gate, ready candidates must have actual usable GUI lineages,
defaults, validation evidence, recoverable implementation and durable resume state.
Provide names, distinctions, settings, limitations, validation indicators and maturity
criteria. No universal fixed generation count. User trains until meaningful learning
and reasonable play, then reports readiness; inspect actual exposures, validation,
lineages and checkpoints on resume. Training is blocking for dependent tournaments;
independently ready candidates may proceed while others remain pending.

Before games, freeze each selected checkpoint separately from live training, recording
full ID, architecture/configuration, network/state hashes, exposure and validation.
Do not reinterpret defective or immature training as architectural weakness.

## Empirical gates and resources

One simultaneous game; one search worker per engine; ponder off; below-normal
priority where supported; initially 64 MiB TT per engine; no competing heavy jobs.
Verify current hardware and startup/JIT/clock/search behavior. Starting calibration
options are 10+0.1 screening and 30+0.3 confirmation, not immutable rules. Use longer
elapsed time rather than extra concurrency; material increases require owner approval.
Pause/reduce load safely if ordinary PC use suffers. Preserve completed games and
resume evidence. Reuse Fastchess/tournament infrastructure where suitable.

A: correctness plus bounded legal-play smoke (load/selection, deterministic scores,
special moves, perspective/bounds, clock/termination/checkpoint checks).
B: prospective bounded screening, approximately 100-200 games when useful.
C: diagnose/remediate implementation, calibration, cost, training or architectural
weakness; substantial retraining returns to the human gate.
D: prospective confirmation of promising mechanisms with hypotheses, errors,
statistical rule, budgets and stopping fixed before outcomes. Use paired reversed
colours, varied predetermined openings, equal books/tablebases/resources, fresh
independent confirmation and longer clocks as needed. Account for opening clusters,
candidate selection and multiple comparisons. Sequential testing is optional with a
valid predefined rule; do not peek at fixed-N intervals as a stopping procedure.

Report W/D/L, failures/missing/capped games, uncertainty and economic resolution
limits. Allowed conclusions include improvement, regression, promising/inconclusive,
insufficiently trained, defective and uncompetitive under tested conditions. No
endless match to force significance, arbitrary minimum Elo gain or small-sample win.
Measure warmed full/incremental inference latency, model/working-set size, load cost,
search NPS/behavior and position strata to explain strength, not replace games.
Record hardware/JVM, build/source identity, exact search/model settings and commands.

Routine tournament reports no more often than every 15 minutes; meaningful events,
errors, completion, statistical stops and human gates may be reported immediately.
Detailed observations belong in local evidence. Host-generated progress is separate.

## Continuity, autonomy, stop and completion

STATE is the dynamic frontier and candidate registry; add focused experiment plans,
results/manifests only as needed. Preserve raw local evidence under ignored `raw/`
or module build paths; commit compact provenance, reports and reproducibility inputs.
Never force-add raw outputs or rewrite frozen experiments. Check local availability.
At each unit inspect, investigate, implement/execute, verify, record, select the next
admissible unit and continue. No repeated minor design approvals. Evidence must
distinguish observations, historical reports, hypotheses and skipped verification.

Stop affected work for the human training gate, unreliable benchmark/comparison,
nonfunctional required checkpoint/training support, unavailable required validation,
unsafe unexplained changes, material authority/scope contradiction, unavailable
external prerequisites, destructive/high-risk action or an actual owner decision.
Continue independent safe work when possible. State blocking action and evidence
needed to resume; silence and later prompts do not satisfy gates.

A turn, commit, handoff or wave is not programme acceptance or operational completion.
Success requires independent credible strength evidence, practical cost trade-offs,
limitations and readiness for later Search research. Continue justified research after
failure or success, including ablations/hybridization where mechanisms merit it.
Inconclusive closure requires an evidence-based account of exhausted admitted avenues,
remaining uncertainty and diminishing expected value or a genuine blocker, not elapsed
time, context length or an arbitrary unit count. Human acceptance remains with owner.
Every pause/completion states actual validation, skipped checks, Git/build state and
`Human actions required after this prompt:` with blocking effects, or `None`.
