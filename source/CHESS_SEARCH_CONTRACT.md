# SeedV6 Search Contract - Phase 3

Internal revision: **P3-R007**

Status: **ACTIVE Phase 3: owner-accepted Policy 4 integrated, validated and promoted as the production Search champion/default for Pair-2 at 1-16 workers. Cumulative Phase 2 retained as a comparison control; no successor objective activated.**

Repository master: `source/CHESS_SEARCH_CONTRACT.md` in `C:/projects/seed/java/seedv6-unified`.

## Authority and objective

Maximize the playing strength of the complete SeedV6 engine through foundational
Search improvements, technically sound integrated candidates, controlled
combinations, reverse ablation and champion-based optimization. Correctness,
lifecycle integrity and computational economy remain binding. Nodes, NPS,
nominal depth and isolated tactical wins are diagnostics, not strength proof.

This Contract governs objectives, requirements, methodology, architecture and
acceptance. The [Frontier](CHESS_SEARCH_RESEARCH_FRONTIER.md) governs subjects,
dispositions, dependencies, opportunities and programme progression. Repository
masters are authoritative; ChatGPT Project Sources are owner-maintained mirrors.
A listing, implementation or document edit alone does not activate research or
accept a candidate. Material changes to binding decisions require owner authority
before dependent implementation/adoption; a launch may explicitly supply it.

Phase 3 supersedes the earlier research methodology, feature-by-feature execution
boundary and historical reopening restrictions. Negative, rejected and deferred
formulations remain evidence and may be reconsidered, including under changed
baselines. Only the technical requirements and current baseline explicitly retained
here carry authority from the archives; adjacent old workflow, activation and
non-adoption prose does not create a Phase 3 prohibition. Surface material
code/canon discrepancies: static source establishes implementation facts, not
runtime verification, and neither silently rewrites intended requirements.

## Policy 4 production champion and retained controls

The owner's 18 October 2026 launch authorized O1 and O2 as one coherent objective:
useful Search-information production, retention, quality and reuse, including
direct consumers, coherent combinations and reverse ablation. Autonomous research,
experimental implementation and controlled validation were authorized within this
Contract. O3 calibration/training, unrelated objectives, production promotion,
owner-reserved binding TT changes and external release were outside that grant.
The [Frontier](CHESS_SEARCH_RESEARCH_FRONTIER.md#completed-first-objective)
records disposition;
the [focused research record](../docs/research/search/P3_INFORMATION_REUSE_2026-10-18.md)
holds exact experimental identities, protocols, measurements and limitations.

The first CGLHW is technically complete, and the owner has explicitly accepted
**Policy 4**: the previous cumulative Phase 2 configuration plus completed legal TT
hint production and reuse, retaining the accepted main-history lifetime and
ROOT_REUSE private seeding. Separate fresh confirmations at **one and four Search
workers, 10+0.1 clocks**, support this decision in those recorded domains. The
Frontier summarizes their positive approximate paired-normal results and resource
costs; conservative bounded intervals still include zero. This is not proof of
improvement at every worker count, clock, memory pressure, platform or resource
configuration. Inconclusive O2 ablations and unconfirmed Policy 7 remain research
alternatives, not accepted components of Policy 4.

The owner's 19 October 2026 production-integration launch separately authorized
adoption and promotion after correctness, configuration-equivalence, architecture,
lifecycle and supported-configuration validation. Those gates passed: **Policy 4
is now the production Search champion/default for Pair-2 at 1-16 workers** and
the baseline for future research. The [integration record](../docs/research/search/P3_POLICY4_PRODUCTION_2026-10-19.md)
binds frozen-candidate equivalence, production entry points, tests and limitations.
The integrated mechanism is unchanged; only normal activation and identity reporting
change. Retained-history, nominal-learning-filter and Policy 7 alternatives remain off.

`ProductionSearch.create` selects Policy 4 for Pair-2; standalone pinned-model UCI,
legacy explicitly selected Pair-2 and GUI Pair-2 Search use that shared factory.
`ProductionSearch.createInformationControl` retains the cumulative Phase 2 engine
with completed hints off, including explicit Hash selection. Existing research
information policy 0 and historical cumulative study controls are pinned to that
control; policy 4 remains independently constructible for equivalence checks.
Other evaluator families preserve their previous behavior, with completed hints off.
Functional compatibility at all supported workers and smaller Hash settings is
not new strength evidence. Confirmed gains remain limited to the recorded generation-32,
one-/four-worker, 10+0.1 domains; neither other Pair-2 checkpoints nor HCE, NNUE or
other BRN families inherit an Elo claim. No successor objective/CGLHW is activated.
Promotion establishes neither version finalization nor external deployment/release;
the inherited finalizer issue remains separately owner-handled.

The preserved previous champion/control is the cumulative Phase 2 engine:
**SR-001 + SR-029 + SR-022 + SR-005 + SR-036 + SR-024**. Its accepted Search
source anchor is `35294146bb7e44975b516005b2cc196439666e09`; the
[unified architecture contract](../docs/unified-architecture/CONTRACT.md) governs
shared-core ownership and migration compatibility. Initialization inspected clean
HEAD `a549cf970647dc6d4220119a61535d4b33ba2f23`, after final Phase 2 technical
HEAD `cbee909da837deaf11ac8c65964d4f9ee3f5a1f0`; the intervening training/GUI
commit did not change the Search masters or `engine-core`. Initialization changes
no engine, evaluator, default, configuration or production acceptance.

| Scope | Production configuration |
| --- | --- |
| Pair-2, one worker (default count) | Recursive nominal-depth PVS, full-window successive-depth driver, QXRAY selective qsearch, protected quiet LMR, WINDOW LMP, EFFORT completed-PV/inclusive-root-effort ordering, completed legal TT hints. |
| Pair-2, 2-16 workers | ROOT_REUSE with the same cumulative selective mechanisms; eligible depth >=3 roots use eldest-first full work, parallel scouts and required original-window full retries. Completed-root guidance, private history seeding and completed legal TT hints; smaller/terminal/draw roots retain the owner path. Restricted root jobs neither consume nor publish ordinary root proof. |
| Other supported evaluators and controls | Preserve HCE, NNUE and supported BRN-family configurations with completed hints off, exact/static/unpruned and explicit feature-off controls, and the inherited same-depth coordinator. Bounded HCE NMP (fixed R=2, depth 4-6, surplus 512) and depth-two static-null (margin 960) remain HCE-only; Pair-2 does not inherit them. |
| Shared mechanics | Legal hash priority, SEE-good then SEE-bad tacticals by immediate material, quiet history, staged/lazy generation and exact legal-existence checks; accepted completed-PV/root-effort guidance composes with these. Independent owners remain isolated. |
| TT, outcomes and clocks | Current primitive direct-mapped TTable, coherent striped locking, requested default 64 MiB, NO_LEAF_TT, equal-depth/current-generation cutoff-only ordinary proof, existing replacement/hash-move rules and separate evaluation cache. Pair-2 completed-node hints occupy the existing hint-only representation, never ordinary mathematical proof. No production qTT. Guarded optional root tablebase outcomes and bounded clock management remain separate from nominal Search. |

QXRAY omits SEE-negative ordinary nonchecking qcaptures outside check while
protecting promotions, en passant and the accepted off-square friendly-slider
resource. LMR protects its early searched quiets and tactical/rule/mate domains;
WINDOW uses actual searched-quiet counts with shallow original-scout/window
eligibility. Their precise guards, ordering, provenance and controls remain in
the [SR-024](../docs/research/search/SR024_SEE_PRUNING_PHASE2_2026-10-14.md),
[SR-029](../docs/research/search/SR029_LMR_PHASE2_2026-10-10.md),
[SR-022](../docs/research/search/SR022_LMP_PHASE2_2026-10-11.md),
[SR-005](../docs/research/search/SR005_ITERATION_REUSE_PHASE2_2026-10-13.md) and
[SR-036](../docs/research/search/SR036_CONCURRENCY_PHASE2_2026-10-14.md) reports.
Broader retained history is not already adopted: ordinary main history resets
per fixed-depth invocation, with controlled private seeding for ROOT_REUSE.

BRE-Pair 2 remains the principal fixed evaluator for strength research: generation
32, lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
network SHA-256 `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
The tracked [model manifest](../build-assets/production/manifest.json) and
[measurement baseline](../docs/research/search/BRE_PAIR2_MEASUREMENT_BASELINE_2026-10-09.md)
bind payload, schema/compiler, commands and controls. `BRN_PAIR2` v1 C02 order-2
runtime uses private rolling workspaces and shared immutable tables. Preserve
`BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1`: STM `material + .25*(raw-material)`,
100 units/pawn, half-away-from-zero rounding, saturation at +/-32511. UCI cp
labels do not prove empirical centipawn/WDL calibration or evaluator superiority.
No latest/Best/other checkpoint substitution or evaluator change is implicit.
Standalone production UCI uses the pinned model; the legacy launcher defaults
to HCE; GUI evaluator selection remains supported.

Functional availability at 1-16 workers is not strength confirmation for every
count. The retained cumulative QXRAY evidence has separate one- and four-worker
confirmations under its recorded host, clocks and inferential assumptions;
ROOT_REUSE evidence also distinguishes equal clocks from equal CPU. Do not pool
worker domains or extrapolate universal platform, clock, weight or equal-compute
gains. Use the actual selected production/control factory and report all options.

## Binding technical requirements

Detailed specifications are in the frozen [R045 Contract](../docs/search-phase1/CHESS_SEARCH_CONTRACT.md)
and [P2-R049 Contract](../docs/search-phase2/CHESS_SEARCH_CONTRACT.md), limited to
the technical topics mapped below and the later accepted baseline above.

| Requirement | Rule and archived detail |
| --- | --- |
| Independent references | Keep independently invocable recursive fixed-depth ExactSearch, TT-off ordered alpha-beta, useful independent oracles, static controls, unpruned SR-001A qsearch and headless validation. Never modify an oracle merely to agree with selectivity. R045 section G/section I and "TT-off oracle and exact TT acceptance"; P2 continuing foundations. |
| Negamax/PVS | Side-to-move scores, negated/reversed child windows, fail-soft exact returns; nominal remaining depth is distinct from real ply. Ordinary legal children consume one nominal ply. First searched legal move gets a full window; later scouts require full re-search for strict alpha < score < beta improvement. Each invocation uses its own original window. R045 section I/exact traversal. |
| Rules and history | Legal exhaustion/checkmate/stalemate precede formal threefold/clock >=100 draws and leaf/TT resolution; preserve separately owned insufficient-material composition. Real prefix includes root once, followed by legal path; unknown history is not invented and twofold is nonterminal. Repetition uses legal EP, side, pieces and rights; Search identity also retains evaluator-relevant full status and future draw context. Push/pop restores exactly through retries and cancellation. R045 SR-010. |
| Mate and ordinary domains | Real ply determines mate distance; ordinary scores cannot enter mate bands. Re-establish the full envelope if horizons/limits/mapping change; static depth-one bounds cannot be transplanted to qsearch. Domain-only bounds create neither a searched PV witness nor a TT store. Mate normalization is caller-owned: positive store +ply/probe -ply, negative store -ply/probe +ply. R045 SR-011/section J; P2 SR-001. |
| Mathematical TT proof | Require complete value-equivalent board/status, evaluator, relevant history/path/policy identity, **equal remaining depth** and **current request generation**. Classify S <= original alpha as UPPER, S >= original beta as LOWER, otherwise EXACT. Current bounds cut only at decoded UPPER <= alpha or LOWER >= beta; no non-cutting tightening. A deeper, older or selective score is not ordinary proof. One request shares its generation across iterations; wrap invalidates old entries. R045 section J. |
| Proof versus guidance | Invocation facts, mathematical proof, provenance/path context and heuristic prediction are distinct. Raw eval, adjusted estimates, history, rank, node-role prediction and legal TT move hints do not certify a nominal score/PV. Current hints retain conservative SearchKey identity and legality checks even when depth/generation differ. Eligibility and aggression are separate; no universal confidence scalar is presumed. R045 sections B-F/section J, SR-008/009. |
| Selective dependencies and verification | Reductions, omissions, extensions, qsearch, synthetic passes and restricted move sets cannot contaminate ordinary nominal proof. Retain current conservative ancestor suppression, including after LMR verification, unless a separately authorized discharge is proved. Every reduced alpha-raising or mate-band return receives nominal-child-depth scout verification, then required full PVS re-search. Reduced fail-low predictions cannot fabricate nominal fail-soft values or PV suffixes. Track actual/assigned/nominal depths and restricted-domain applicability. P2 SR-029/022; R045 SR-018/021/029-033. |
| Legal moves, learning and completion | Search legal moves at most once; publish valid searched PV prefixes only. Omitted/unsearched moves supply no searched score, history label, admitted descendant or fabricated continuation. Distinguish reduced/unverified outcomes from nominally verified labels. Incomplete/cancelled work cannot publish completed results, normal history effects or mathematical TT stores. Keep only actually completed iterations with truthful depth/provenance. R045 section I/section K, SR-016; P2 SR-022/029. |
| Qsearch and synthetic domains | Unpruned SR-001A resolves terminals/draws before stand-pat, permits stand-pat only outside check, searches legal captures/EP/all promotions and all checked evasions, with real-ply mates and honest ply-limit aborts. Production selectivity remains explicit. Null passes are not real repetitions; isolate synthetic history, restore state on unwind and retain ordinary-TT exclusion. R045 SR-001/018; P2 foundations. |
| Ownership, cancellation and accounting | Independent owners isolate mutable board/PV/history/TT/evaluator state. Workers within one request own private mutable state and share only coherent permitted TT/generation and immutable definitions; seeding has explicit ownership boundaries. Drain helpers before publication/reset/reuse/close; stale work cannot publish. Hard stops dominate soft allocation; account for all main/q/probe/retry/coordination and aborted-helper work and full-request cost. R045 section J/section K/SR-036; P2 SR-036. |
| Evaluators and special outcomes | Keep exact core semantics evaluator-independent; select calibrated policies at the adapter/factory boundary. HCE margins are not neural authority. Root tablebase WDL/DTZ outcomes require legal/history/rule guards and native ownership; they cannot fabricate nominal score, depth or TT proof. Preserve ordinary Search, fallback and special-outcome distinctions. R045 evaluator boundary/section K/SR-013/018/019/034. |

Exact-mode disagreement requires diagnosis. Selective disagreement with a
finite-horizon oracle is an explicit research outcome, not automatically a bug
or universal rejection. False proof, illegal moves, corrupted state, unsafe
shared ownership and dishonest completion cannot be exchanged for Elo.

## Architecture and integrated candidates

Research may change foundations, add/remove/replace mechanisms, compare competing
drivers and construct a comprehensive research-capable engine within a selected
objective. Available capabilities and a selected executable configuration are
different. The 38 SR subjects are not 38 independent switches: some specify
architecture, evidence, resources or mutually exclusive algorithms. Record each
candidate's concrete composition, dependencies, exclusions, defaults and identity.
Experimental availability does not make it the production default or champion.

Preserve the unified ownership boundary: accepted shared runtime belongs in
`engine-core`; research tools/oracles and experimental activation remain under
research ownership. Production must not acquire a dependency on research or a
second independently maintained engine. Use existing extension seams or obtain
a scoped architecture decision when necessary.

Select compact primitive policies at construction where practical; prefer packed
state, reusable scratch and direct control flow. Avoid unnecessary per-node
allocation, heavyweight abstractions and hot-path dispatch. Measure branches,
cache/indexing/locking, reset/seed cost, JVM/JIT/allocation and full-request CPU
and elapsed cost. Research flexibility must not impose needless production cost.

TT research may examine information content, producer eligibility, retention,
replacement, reuse and representation. Do not presume TTable is defective or
that a new table is necessary: better use of current storage may suffice.
Mathematical proof admission remains independent of heuristic move guidance;
storing a selective estimate never makes it nominal proof. Current binding TT
identity, layout, locking, replacement and proof decisions require applicable
owner authorization to change. The owner permits consideration of a modest
requested-budget increase, e.g. 64 to approximately 72 MiB, if useful additional
information needs bytes while retaining comparable entry capacity. This is
design flexibility, not an allocation or redesign instruction; measure actual
capacity, memory overhead, information value and concurrency cost.

The [independent Bagatur analysis](../../BAGATUR_SEARCH_FRONTIER_ARCHITECTURE_ANALYSIS.md)
compared all 38 subjects with 31 direct behavioural matches at pinned snapshots.
It supplies detailed reference-engine evidence and testable hypotheses, not an
implementation specification, accepted SeedV6 architecture or isolated-feature
Elo proof. Its historical status/line references belong to that snapshot. Its
proposed sequence is advisory, not a new standalone-feature acceptance gate.
Never relax SeedV6 guarantees to copy more permissive reference behaviour.

## Combinations, ablation and promotion

Two complementary approaches are authorized by a suitably scoped future objective:

- **Evidence-guided combinations:** test plausible dependencies, including TT
  guidance with aspiration, retained history with history-conditioned pruning,
  evaluation evidence with depth adjustment, or TT retention with repeated
  zero-window drivers. Compare foundations alone and combined when informative;
  pair/triplet and larger coherent combinations are legitimate.
- **Integrated-candidate optimization:** assemble a technically valid feature-rich
  configuration and compare the complete candidate with the accepted champion.
  Constituents need not each demonstrate independently positive Elo. Neutral or
  apparently negative mechanisms may be retained experimentally for interactions.
  Validate semantics continuously as mechanisms enter; do not postpone validation
  until dozens of interacting selective features have accumulated.

Keep the accepted research configuration, accepted production champion,
experimental integrated candidate, historical comparison anchor, and
feature/ablation control distinct.
A convincingly stronger complete candidate may become champion only after credible
playing-strength evidence, appropriate fresh independent confirmation, correctness,
lifecycle and supported-configuration validation, and explicit GPT/owner acceptance.
Implementation alone authorizes neither promotion nor ordinary production adoption.
There is no arbitrary minimum Elo gain or universal game count: decide whether
the configuration is credibly stronger at the evaluated resources, with costs,
uncertainty and compatibility stated.

After production promotion, optimize the champion through removals, substitutions,
interactions and configuration mutations; future research normally compares with
that champion.
Preserve older identities and reproducible evidence as historical controls.
Reverse ablation must distinguish harmful, redundant, interaction-beneficial and
unclassifiable components. A removal loss establishes conditional value in that
configuration, not universal component value. Diagnose changed dependencies and
resource costs before attributing a causal contribution.

Evidence-guided or economical stochastic/evolutionary exploration is permitted.
Avoid uncontrolled exhaustive enumeration. Candidate selection and promotion must
account for noisy games, repeated looks, multiple comparisons, selection bias and
overfitting; screen progressively and independently confirm frozen finalists.

## Measurement, validation and resources

Use the retained Phase 2 infrastructure and focused reports; closed confirmations
remain closed. Fresh candidate configurations require new appropriate validation.

Default to background-friendly research on the owner's Windows PC so normal
interactive use can continue. Optimize useful evidence with minimal disruptive
resource consumption, not maximum tournament throughput or shortest completion
time. Where scientifically sufficient, prefer few concurrent games, modest Search
worker counts, background-friendly process priority, controlled memory and
conservative parallelism. Prefer substantially longer experiments over unnecessary
CPU saturation or desktop disruption. Increase resources only for experimental
validity, information value or the worker domain under study, subject to the
availability and disruptive-workload approval rules below.

- Codex autonomously chooses scientifically appropriate fixed-depth, fixed-node,
  clock-based or other methods for the mechanisms and research questions involved.
  Fixed-depth and fixed-node games are legitimate development screens where they
  provide meaningful evidence with less resource interference. No universal depth,
  node budget, Search worker count or concurrent game count is prescribed; depth 4
  is illustrative, not a testing standard.
- Validate semantic/proof/history/mate/terminal/PV integrity, independent oracles,
  cancellation and worker/evaluator isolation incrementally and after composition.
  Separate fixed-depth/fixed-node mechanism diagnostics, whole-request elapsed/CPU
  and JVM costs, and playing strength. Include all retries and aborted work.
- Clock games are primary strength evidence: paired/color-reversed openings,
  controlled sources/binaries, evaluator/checkpoint/mapping, configuration, seeds,
  workers, TT/resources, host/JVM/warm-up and time controls. Keep single-worker and
  multiworker domains distinct; single-worker research cannot substitute for required
  multiworker validation. Default-OFF controls must identify actual behavior; a
  familiar option name or old binary path is not sufficient provenance.
- Before substantial confirmation, freeze hypotheses/comparison, candidates,
  sampling/pair model, prospective looks, success/futility/inconclusive stopping,
  uncertainty and acceptance rules. Suitable sequential, staged or fixed-sample
  methods are allowed; disclose approximation/model limits and relevant sensitivity.
  Exclude development/tuning data from independent confirmation. Do not retune a
  finalist, extend a closed protocol post hoc, or selectively replay/exclude games.
- Progress from cheap discriminating observations to informative screens and fresh
  confirmation as expected decision value warrants. Positive point estimates or
  intervals crossing zero alone do not establish gain. Resource interruption is
  not statistical futility or inferiority. No budget exhaustion automatically
  rejects a subject; retain unresolved hypotheses and uncertainty.
- Calibrate concurrent independent games separately from engine workers using
  valid complete-pair throughput and asymmetric contention risk, not logical-core
  occupancy. Reuse suitable calibration; reassess material workload/host changes.
  A/A checks alone do not prove candidate/baseline immunity to contention. Equal
  clocks do not imply equal compute. Record full runner time, CPU/memory, failures,
  game endings, actual work, precision/value and forecast duration before costly runs.
  Account for CPU contention and scheduling: fixed-depth/fixed-node games generally
  remain useful under variable desktop load, but elapsed-time measurements and clock
  games can be distorted. Uncontrolled contention is not reliable strength evidence.
- Respect workstation availability: current preferences are roughly 4-6 disruptive
  daytime hours or 6-8 overnight; a disruptive continuous 12-hour run needs an
  alternative or explicit approval. High-value architectural hypotheses may justify
  extensive computation and validated multiple sessions. Sessions are not statistical
  looks; freeze restart/block rules independently of outcomes, preserve failures and
  incomplete games, and use only demonstrated auditable continuation. The
  [tournament procedure](../docs/research/search/TOURNAMENT_EXECUTION_2026-10-17.md)
  records the pair-block method and limits. Changing the confirmation-clock standard
  requires evidence and owner authority; shorter clocks define another domain.

When valid confirmation would require disruptive resources that cannot currently
run, retain promising candidates with confirmation outstanding; lower-impact
development screening does not satisfy champion confirmation. Continue other useful
in-scope work where dependencies permit.

Across all Phase 3 research workstreams, including future CGLHWs, monitor
long-running paired-game tournaments, benchmarks and comparable unattended
research processes using reliable blocking waits, event-driven notifications or
completion-driven monitoring in preference to frequent polling. If periodic
polling is technically necessary, use infrequent checks, ordinarily approximately
15-30 minutes apart; a shorter interval requires a specific failure-detection,
safety, operational or tool constraint.

Monitoring frequency and user-facing reporting frequency are distinct: an internal
check does not itself warrant commentary. Do not issue routine progress commentary
every minute or two. Game-count-only updates, unchanged status and repeated
statements that no errors have occurred are not independently report-worthy.
Report material events when useful or necessary: important experimental or
statistical milestones; completed scheduled assessments; failures, stalls and
integrity problems; material decisions, results or changes to evidence or
conclusions; genuine blockers or required human intervention; and tournament or
research-unit completion. Impose no mandatory periodic reporting schedule;
completion-only reporting is acceptable if no intervening material event warrants
communication.

Preserve reliable failure, stall, timeout, cancellation and completion detection,
operational safety and evidence integrity. Keep existing logs, durable research
state and required final evidence-bearing completion reports. Honor mandatory
host/tool reporting requirements while minimizing unnecessary discretionary
commentary and tool calls. Do not
optimize for rapid SR closure or use unlimited experiments in place of meaningful
hypotheses and controlled comparisons.

## Autonomy, evidence and handback

A coherent, independently reviewable selected objective is the unit of Codex
autonomy. It may span several SR identifiers and many internal workstreams:
investigation, prototypes, diagnosis, experiments, combinations, ablations,
validation and iteration toward an evidence-supported conclusion. One failed
formulation or finished experiment is not automatic closeout. Continue useful
work within scope; do not autonomously start another unrelated objective.
Escalate genuine external blockers, unresolved owner decisions, binding changes
and work outside the authorized boundary.

Record material conclusions, dependencies, candidate/configuration identities,
accepted changes, uncertainty and unresolved hypotheses in the two masters or
focused reports/manifests in existing research locations. Keep canon forward-looking;
do not append an experimental diary or repeated acceptance narratives. Preserve
negative, failed, interrupted and frozen evidence and original protocols. Under
[repository evidence policy](../AGENTS.md#local-research-evidence), bulk output is
ignored local evidence (new formats use existing experiment `raw/` directories);
reports, compact summaries, configurations, manifests and intentional fixtures
remain tracked. Verify availability before analysis; a fresh checkout lacks local
raw evidence. No extra programme-management documents, ledgers or orchestration
records are required or authorized merely to run this programme.

At closeout reconcile evidence, validation, temporary artifacts, unrelated work
and applicable Git boundaries. Report technical outcome, limits, changes, actual
checks, Git/version state and blocking/non-blocking human actions; return control
to GPT/owner. Technical completion, acceptance and deployment are distinct.
Increment affected master revisions once per content-changing update using
`P3-Rnnn` / `P3-Fnnn`; keep only concise current-state/decision information.
When canon changes, identify **both** files and exact headers and report
`PROJECT_SOURCE_REFRESH_REQUIRED: YES`. The owner replaces both Project Source
mirrors and confirms synchronization before GPT uses them for dependent planning
or prepares the next launch. Codex uses repository masters within authorized work
and never claims external synchronization from a refresh request.

## Historical evidence and initialization boundary

The owner confirms final Phase 2 mirror synchronization and GPT acceptance of
SR-032 as DEFERRED without adoption. Those predecessor actions are satisfied;
The owner confirmed synchronization of P3-R002 / P3-F001 at the O1 + O2 launch
and of P3-R006 / P3-F004 before this production-integration launch. The first
objective is complete; acceptance and validated production promotion are recorded
above. This P3-R007 / P3-F005 promotion update requires a new two-mirror refresh
and synchronization confirmation before dependent GPT planning or a subsequent
launch, without blocking this authorized repository work. The new refresh is
outstanding; Codex does not claim external synchronization.

| Frozen file | Revision | Verified SHA-256 (complete original bytes) |
| --- | --- | --- |
| [Phase 1 Contract](../docs/search-phase1/CHESS_SEARCH_CONTRACT.md) | R045 | `9f820c2e9efd366b179902d3fc14bdd8f88e6ae793356aded6b6119f73e8401b` |
| [Phase 1 Frontier](../docs/search-phase1/CHESS_SEARCH_RESEARCH_FRONTIER.md) | F052 | `ead3743400b1dc513e02ad1c776b39db1486e8800c947cece9e20ba199e8fc1a` |
| [Phase 2 Contract](../docs/search-phase2/CHESS_SEARCH_CONTRACT.md) | P2-R049 | `013a09a15425d1f99388816b6726a9c9b0665d7b7b741a9d93287a702ea2dab6` |
| [Phase 2 Frontier](../docs/search-phase2/CHESS_SEARCH_RESEARCH_FRONTIER.md) | P2-F053 | `f371ae1e3ddd259f51ae5df17b856b9bf9b5c54dcec39cdafc9231d008ff8927` |

Archives are immutable historical evidence, including old ACTIVE, pending-handback,
mirror and implementation statements. Do not rewrite or execute them as current
instructions. Phase 2 links retain their original `source/` context; resolve them
there when relocation affects a relative path. Individual [research reports](../docs/research/search/),
retained evidence, source/Git history and pinned Bagatur analysis supply detail;
initialization does not recertify old experiments or close unrelated migration gates.
