# SeedV6 Search Contract - Phase 3

Internal revision: **P3-R001**

Status: **Initialized; no ACTIVE Phase 3 research workstream. The first objective requires separate GPT/owner selection and authorization.**

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

## Initial champion and supported scope

The initial accepted production champion is the cumulative Phase 2 engine:
**SR-001 + SR-029 + SR-022 + SR-005 + SR-036 + SR-024**. Its accepted Search
source anchor is `35294146bb7e44975b516005b2cc196439666e09`; the
[unified architecture contract](../docs/unified-architecture/CONTRACT.md) governs
shared-core ownership and migration compatibility. Initialization inspected clean
HEAD `a549cf970647dc6d4220119a61535d4b33ba2f23`, after final Phase 2 technical
HEAD `cbee909da837deaf11ac8c65964d4f9ee3f5a1f0`; the intervening training/GUI
commit did not change the Search masters or `engine-core`. Initialization changes
no engine, evaluator, default, configuration or production acceptance.

| Scope | Retained configuration |
| --- | --- |
| Pair-2, one worker (default count) | Recursive nominal-depth PVS, full-window successive-depth driver, QXRAY selective qsearch, protected quiet LMR, WINDOW LMP, EFFORT completed-PV/inclusive-root-effort ordering. |
| Pair-2, 2-16 workers | ROOT_REUSE with the same cumulative selective mechanisms; eligible depth >=3 roots use eldest-first full work, parallel scouts and required original-window full retries. Completed-root guidance and private history seeding; smaller/terminal/draw roots retain the owner path. Restricted root jobs neither consume nor publish ordinary root proof. |
| Other supported evaluators and controls | Preserve HCE, NNUE and supported BRN-family configurations, exact/static/unpruned and explicit feature-off controls, and the inherited same-depth coordinator. Bounded HCE NMP (fixed R=2, depth 4-6, surplus 512) and depth-two static-null (margin 960) remain HCE-only; Pair-2 does not inherit them. |
| Shared mechanics | Legal hash priority, SEE-good then SEE-bad tacticals by immediate material, quiet history, staged/lazy generation and exact legal-existence checks; accepted completed-PV/root-effort guidance composes with these. Independent owners remain isolated. |
| TT, outcomes and clocks | Current primitive direct-mapped TTable, coherent striped locking, requested default 64 MiB, NO_LEAF_TT, equal-depth/current-generation cutoff-only ordinary proof, existing replacement/hash-move rules and separate evaluation cache. No production qTT. Guarded optional root tablebase outcomes and bounded clock management remain separate from nominal Search. |

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

Keep four identities distinct: accepted production champion, experimental
integrated candidate, historical comparison anchor, and feature/ablation control.
A convincingly stronger complete candidate may become champion only after credible
playing-strength evidence, appropriate fresh independent confirmation, correctness,
lifecycle and supported-configuration validation, and explicit GPT/owner acceptance.
Implementation alone authorizes neither promotion nor ordinary production adoption.
There is no arbitrary minimum Elo gain or universal game count: decide whether
the configuration is credibly stronger at the evaluated resources, with costs,
uncertainty and compatibility stated.

Then optimize the accepted champion through removals, substitutions, interactions
and configuration mutations; future research normally compares with that champion.
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

- Validate semantic/proof/history/mate/terminal/PV integrity, independent oracles,
  cancellation and worker/evaluator isolation incrementally and after composition.
  Separate fixed-depth/fixed-node mechanism diagnostics, whole-request elapsed/CPU
  and JVM costs, and playing strength. Include all retries and aborted work.
- Clock games are primary strength evidence: paired/color-reversed openings,
  controlled sources/binaries, evaluator/checkpoint/mapping, configuration, seeds,
  workers, TT/resources, host/JVM/warm-up and time controls. Keep single-worker and
  multiworker domains distinct. Default-OFF controls must identify actual behavior;
  a familiar option name or old binary path is not sufficient provenance.
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
- Respect workstation availability: current preferences are roughly 4-6 disruptive
  daytime hours or 6-8 overnight; a disruptive continuous 12-hour run needs an
  alternative or explicit approval. High-value architectural hypotheses may justify
  extensive computation and validated multiple sessions. Sessions are not statistical
  looks; freeze restart/block rules independently of outcomes, preserve failures and
  incomplete games, and use only demonstrated auditable continuation. The
  [tournament procedure](../docs/research/search/TOURNAMENT_EXECUTION_2026-10-17.md)
  records the pair-block method and limits. Changing the confirmation-clock standard
  requires evidence and owner authority; shorter clocks define another domain.

Prefer reliable completion/event-driven monitoring with failure, stall and
cancellation detection. Avoid count-only polling; communicate material decisions,
results and blockers while satisfying host/tool reporting requirements. Do not
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
Phase 3 introduces a new two-mirror refresh obligation. No research is ACTIVE,
no integrated research engine is constructed and no experiment is launched here.

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
