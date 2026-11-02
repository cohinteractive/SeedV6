# SeedV6 Search Contract - Phase 4

Internal revision: **P4-R007**

Status: **ACTIVE - 3 November 2026. HL-G1 remains the normal experimental production policy across W1-W16, integrated as build85; current repository build88 adds opt-in research only. HL-R001 remains accepted and closed; HL-G1/build84 remains the W1-qualified research champion. TTF-R001 remains accepted for its limited foundational purpose; TT-R001 remains inconclusive with separate reconciliation pending. CH-R001 authorized execution is complete: independent W1 strength criterion failed; GPT/owner reconciliation pending. No promotion or deployment.**

Repository master: `source/CHESS_SEARCH_CONTRACT.md` in `C:/projects/seed/java/seedv6-unified`.
The [Frontier](CHESS_SEARCH_RESEARCH_FRONTIER.md) records current priorities,
dependencies and decisions under this Contract. These are the two Phase 4 masters.

## Founding authority and scope

The owner closes Phase 3 and establishes Phase 4 as a **research reset, not an
implementation reset**. Phase 4 inherits the accepted production engine, shared
architecture and genuine technical safeguards. It does not automatically inherit
Phase 1-3 methodology, rejection decisions, experimental prohibitions or
mathematical-proof restrictions as research policy. This charter explicitly
supersedes those phase-specific restrictions within authorized Phase 4 objectives,
including Phase 3's requirement for separate permission to experiment with its
binding TT policies. Historical failures retain their actual configuration and
evidence scope; they are not universal rejection decisions.

Applicable shared governance, [AGENTS.md](../AGENTS.md), repository safety,
application architecture ownership and requirements outside Search research keep
their legitimate scope. The [unified architecture contract](../docs/unified-architecture/CONTRACT.md)
governs ownership; its migration-era production descriptions are historical, and
its distinction between nominal proof and selective prediction is preserved.
This charter changes Search research authority, not shared behavioral policy or
registry activation. Escalate unresolved material authority conflicts before
affected work; do not silently override another domain's requirements.

Production stays in shared `engine-core`, consumed by GUI, Arena and UCI.
Research tools, independent references and experimental controls remain appropriately
separated under the established `research`/test ownership boundaries; production
must not depend on research or acquire a competing engine implementation.
Exclude unrelated GUI/application work, independent evaluator training or
architecture programmes, broad unrelated rewrites and external deployment.
This revision records closeout of the owner-authorized CH-R001 strength-development
CGLHW. Its [focused contract](../docs/research/search/p4-correction/CONTRACT.md)
retains the objective, finite resources, exact build84 comparator and independent
W1 criterion. [RESULTS](../docs/research/search/p4-correction/RESULTS.md) records
completed authorized execution with the strength criterion failed;
[STATE](../docs/research/search/p4-correction/STATE.md) owns terminal continuity.
Technical qualification is not a successful foundational relabelling. The sole
confirmation attempt is spent; further independent work needs renewed authority.
HL-G1 normal production policy remains unchanged across W1-W16. No promotion,
push, release, installation, deployment or successor is authorized.

## Strength-first, externally informed research

The overarching purpose is to **develop demonstrably stronger integrated Search
configurations under defined computational resources**. Strength-development
programmes must engineer that outcome, not merely test whether isolated changes
help the existing architecture. That architecture is the accepted starting point,
not a presumption of optimality. Depth, nodes, NPS, computational efficiency and
agreement with a finite-horizon reference are useful measurements, but none
substitutes for playing strength.

**Foundational engineering objectives** develop or adapt Search capabilities that
enable stronger integrated configurations through dependent mechanisms. Their
immediate success may be a technically qualified, demonstrably useful foundation
without statistically confirmed standalone Elo superiority. Explain its credible
relationship to practical Search strength: added complexity, storage, configurability
or theoretical capability alone is insufficient. Greater capability is not proven
playing strength.

These are related objective types, not necessarily separate workstreams. One
coherent authorized CGLHW may develop a foundation, investigate representative
dependent mechanisms and demonstrate integrated strength. Before execution, owner
and GPT coordination must establish the governing objective and acceptance
expectations. A material change to that objective or its completion criteria
requires owner authority; Codex must not relabel unsuccessful strength-development
work as completed foundational work to evade an unmet strength requirement.

In a normal authorized Search CGLHW, GPT and Codex must actively
learn from established strong engines, including **Bagatur, Stockfish and other
relevant implementations**. Inspect their actual source formulations, surrounding
Search mechanisms, eligibility, information structures, safeguards, available
empirical evidence and relevant development history; record relevant source
revisions. Compare their supporting conditions with SeedV6 to
identify incompatibilities, missing dependencies and architectural limitations.
Investigate adaptations needed to obtain comparable benefits, including whether
these differences explain unsuccessful or inconclusive SeedV6 results. An external
engine's overall strength does not prove the independent benefit of each mechanism.
Do not blindly copy implementations or transplant parameters without SeedV6 evidence.

Follow consequential dependencies across subject boundaries within the authorized
coherent objective. For example, MTD(f) may lead to TT score reuse, then selective
provenance, replacement, aspiration or ordering. Subjects organize knowledge;
they do not constrain understanding of interacting mechanisms. Keep expansion
tied to the original objective and evidence. Codex may investigate and change
relevant supporting Search mechanisms where evidence connects them to the intended
improvement; do not preserve a limiting architecture merely because it is the
baseline. Initial labels or subject boundaries must not block consequential
dependencies. Escalate material expansion into an independent objective,
disproportionate resource expenditure, safety/authority conflicts and changes
requiring owner authority. This is not permission for unrestricted architectural
rewrites.

Develop and test integrated configurations, including combinations, substitutions,
dependency changes, reverse ablations and interactions. A weak standalone
mechanism may help another configuration; individually promising
features may interfere. No independent gain is required from every mechanism.
Use experimental failures to guide subsequent investigation. One failed formulation
under one baseline does not reject a mechanism universally. Preserve negative and
inconclusive evidence with its actual scope, without reopening frozen studies or
rewriting their conclusions.

Evaluate foundations through their intended dependencies, not only isolated
subsystem substitutions. TT information availability, provenance, retention,
replacement and consumption may support selective pruning, reductions, extensions,
aspiration, move ordering, repeated-window searches and parallel cooperation.
A TT that performs similarly or slightly worse alone might enable more effective
singular extensions, selective verification or other integrated formulations; that
possibility needs evidence. Useful comparisons may combine existing/new foundations
with existing mechanisms and with representative dependent improvements. This is
an illustrative comparison structure, not a mandatory factorial experiment.
Use focused integration evidence and controlled strength tests where informative;
investigate and proportionately adapt surrounding mechanisms when compatibility
is necessary to establish the foundation's effectiveness.

## Correctness and deliberate approximation

Preserve genuine chess correctness: legal moves and terminal adjudication,
board/state integrity and exact restoration, reliable real history and repetition,
proper draw rules, and valid mate-distance/ordinary-score domains. Do not invent
unknown history or treat synthetic null passes as real game repetitions. Retain
legal PVs and truthful score, depth, provenance and completion reporting.

Preserve safe concurrency, worker-private mutable state, coherent shared access,
cancellation and full-request resource accounting. Hard stops dominate soft
allocation; drain owned work before reset, reuse or shutdown, and prevent stale
publication. An incomplete iteration cannot be reported as complete. Evaluator
selection, score mapping and special tablebase outcomes must retain honest identities
and their relevant rule, lifecycle and score-domain safeguards.

Distinguish implementation defects from intentional Search approximations.
Selective pruning, reductions, extensions and heuristic score reuse can disagree
with exhaustive finite-horizon references without being defective. Mathematical
equivalence is **not a universal requirement** for experimental heuristics.
Evaluate whether practical strength benefit justifies approximation and its risks;
illegal state, unsafe access, corrupt mate handling and dishonest reporting are
never acceptable trades for strength.

Retain independent exact references and historical comparison controls, including
independently invocable recursive fixed-depth Search, TT-off ordered alpha-beta
and unpruned qsearch controls. Preserve each exact control's declared semantics;
diagnose exact-mode disagreement. Never alter an oracle merely to agree with a
selective implementation. Use exact comparisons as diagnostic controls, and
characterize selective disagreement rather than automatically rejecting it or
mislabeling a heuristic as mathematical proof.

## Transposition-table experimental authority

Within a newly authorized Phase 4 research objective, routine experiments may
investigate the following without feature-by-feature owner permission:

- Score cutoff eligibility; equal-depth versus sufficient-depth reuse;
  cross-generation reuse; bound classification and utilization.
- Retention and reuse of selectively produced scores; heuristic score cutoffs;
  replacement and aging; capacity, representation and layout.
- Interactions with aspiration, PVS, MTD(f), extensions, reductions, move ordering
  and parallel Search; alternative identity representations and history policies.

The governing distinction is **mathematical proof versus deliberately approximate
Search heuristic**. A score may be investigated as a heuristic cutoff without
claiming it proves the exhaustive reference value. Bound labels and reported
provenance must state their actual meaning; an experimental label cannot manufacture
exact-proof authority. Phase 3's equal-depth/current-generation admission,
cutoff-only use and selective-score suppression are inherited production choices,
not blanket Phase 4 experimental vetoes. Historical proof-only restrictions do
not silently reimpose those vetoes through old reference documents.

Alternative identity/history policies must preserve chess-rule correctness and
establish adequate safeguards for any adopted heuristic. Experimental permission
does not allow invalid chess state, unsafe shared access, corrupt mate handling,
false completion or other genuine defects. No TT implementation change is
authorized by initialization. Production promotion remains separate and requires
evidence, technical qualification and explicit owner acceptance.

## Experimentation, evidence and acceptance

Permit credible speculative hypotheses and affordable exploratory experiments
without demanding advance evidence that they will gain strength. Progress from
economical diagnostics to meaningful strength comparisons and independent
confirmation where appropriate. Use reproducible source/configuration/evaluator
identities, meaningful controls and defined resource/worker/clock domains.
Account for computational cost, statistical uncertainty, selection bias and
multiple comparisons. Distinguish exploratory selection from confirmation;
preserve frozen protocols and prospective stopping rules. Avoid both premature
rejection and unnecessary exhaustive experimentation.

For a foundational objective, use evidence proportional to its intended purpose
and risk. Identify the existing capability or information-flow limitation, relevant
external formulations and the supporting conditions that make them effective.
Demonstrate the intended capability under realistic Search conditions and that
representative downstream mechanisms can consume or exploit it as intended;
include interaction/integration tests where needed for practical relevance.
Characterize memory, runtime, Search effort, concurrency and information-quality
trade-offs, retaining the correctness, state/history, score, lifecycle and reporting
safeguards above. Preserve reproducible source, configuration, evaluator, resource
and test-domain identities, remaining integration needs and unresolved strength
qualifications. These are proportionate evidence considerations, not an exhaustive
mandatory checklist or a new documentation layer.

Distinguish capabilities shown to work, demonstrated downstream-mechanism
improvements, plausible but unverified downstream benefits, measured operational
costs and demonstrated playing-strength improvements. Meaningful demonstration,
not speculative architectural superiority, supports qualification. Use controlled
direct-strength checks where appropriate to characterize immediate performance
and regression risk; an Elo gain is desirable but not universally required for
foundational completion. Unacceptable cost or adverse interaction without a
credible, justified route to downstream benefit precludes successful qualification,
even if the intended interface works.

Maintain distinct roles and evidence status, not a mandatory promotion hierarchy:

| Role | Meaning and advancement |
| --- | --- |
| Initial production baseline | Immutable accepted Phase 3 configuration and repository identity recorded below. |
| Current research champion | Initially identical to that baseline. Advance only on credible integrated-strength evidence and explicit owner acceptance, with qualifications and supported domains recorded. |
| Technical comparison control | Reproducible reference for a specified comparison, including qualified corrections; technical suitability does not establish champion status or competitive neutrality. |
| Experimental candidate | Reproducible alternative under investigation; implementation or a promising screen alone grants no acceptance. |
| Active experimental engine | Owner-selected playable configuration for controlled experimental use; may differ from the confirmed research champion. Its exact deployment and production-default status must be stated separately. |
| Technically qualified experimental research foundation | Enabling capabilities qualified for an agreed research purpose, with evidence and limitations; may be retained as a reproducible base without standalone Elo superiority, champion advancement or production adoption. |
| Production configuration | Explicitly owner-accepted configuration for production behavior; experimental qualification or champion status alone does not select it. |

A later owner-authorized objective may explicitly select a qualified foundation
as its experimental starting point. Identify its exact implementation/configuration,
qualification evidence, limitations and relevant dependencies; retain the accepted
champion and appropriate strength controls. Qualification establishes neither that
all downstream mechanisms benefit nor that derived configurations outperform the
champion. Technical qualification and competitive strength status are separate
properties; selection does not replace the champion or production default.

## Incremental experimental adoption

SeedV6 is an experimental chess engine operated by its developer, including through
a Lichess bot. Phase 4 investigates, adapts and integrates Search formulations
informed by much stronger external engines to improve the playable engine
incrementally. The active playable engine may evolve during Phase 4; useful research
must not accumulate indefinitely awaiting a programme-wide finish or an isolated,
statistically confirmed Elo gain from every component. Credibly stronger integrated
configurations should normally receive prompt consideration for experimental adoption.

Competitively neutral, slightly improving or individually inconclusive formulations
may also be eligible where a credible technical or empirical rationale supports
future synergy or enabling value, overhead is acceptable, operational safety is
sufficient, and downstream integration potential is useful. Distinguish that rationale
from demonstrated competitive superiority. Preserve the agreed distinction between
strength-development and foundational success: neither adoption nor a future enabling
objective retroactively relaxes completed acceptance criteria or converts an
unsuccessful strength objective into a successful foundation.

Experimental integration, research champion advancement, active Lichess deployment
and production-default changes are distinct decisions; their evidence and
authorization requirements may differ. A controlled experimental build may be
selected to investigate promising interactions even when it is not the statistically
strongest confirmed research champion. Champion advancement still requires credible
integrated-strength evidence and explicit owner acceptance in stated domains; the
Frontier records the current designation. Champion status or technical qualification
alone neither deploys a runtime nor changes production defaults.

Qualification for practical adoption must be proportionate to actual risk, without
automatically imposing commercial-release-scale validation. Genuine chess correctness,
state integrity, concurrency safety, truthful reporting and reliable lifecycle
operation remain mandatory. Carry known worker, clock, platform, consumer and
competitive qualifications with each adopted formulation; W1 evidence does not
qualify W4. Preserve reproducible source, artifact and configuration identities,
relevant testing evidence, known risks and a practical rollback path. The immutable
P4-BASELINE-001 remains historical evidence, not a configuration rewritten by adoption.

Configuration-identifiable Lichess games and ratings can provide longitudinal field
feedback. They do not replace controlled competitive experiments, and rating changes
must not automatically be attributed causally to a particular Search change. Retain
periodic integrated comparisons, reverse ablations and investigation of interactions
so uncertain features do not accumulate without evidence of their collective value.

Explicit owner authority remains necessary for champion advancement, actual
experimental deployment, production promotion, material scope changes and
disproportionate resources. Production promotion requires appropriate evidence and
technical/consumer/worker/lifecycle qualification, with owner acceptance of the
specified change and remaining risks. This policy grants Codex no automatic deployment
authority. Technical completion, GPT/owner reconciliation, acceptance and operational
deployment remain distinct.

Keep governance lightweight. Preserve material discoveries, dependencies,
configuration identities, adopted decisions and limitations in the two masters or
focused reports/manifests in existing research locations. Do not recreate the
38-subject programme, an exhaustive diary, process ledgers or competing canons.
Under [local evidence policy](../AGENTS.md#local-research-evidence), retain raw
output locally at recorded ignored paths, including negative/failed/interrupted
work; keep compact summaries, configurations, manifests and intentional fixtures
in Git. Check local evidence availability before analysis. Git exclusion does
not authorize deletion, protocol changes or revised historical conclusions.

## Research completion and escalation

Distinguish completion of an experiment, its positive/negative/inconclusive
finding, completion of a campaign's technical execution, foundational engineering
success, integrated playing-strength success, owner acceptance, champion advancement
and production promotion.
A valid, complete and scientifically useful experiment or technically completed
campaign need not achieve its governing objective. Exhausting a bounded
experiment queue with negative or inconclusive results is not successful
completion of a strength-development CGLHW's governing objective.

For a strength-development objective, the intended successful outcome is a
**credibly demonstrated practical playing-strength improvement in a reproducibly
identified integrated Search configuration**, under stated resource and evaluation
conditions. Preserve technical qualification, statistical confidence and appropriate
independent confirmation.
Even credible research success leaves a candidate experimental until separately
accepted; it neither advances the champion nor promotes production automatically.
Never manufacture a positive conclusion, retrospectively lower evidence standards
or claim improvement without sufficient evidence.

For a foundational objective, successful completion may establish the agreed
enabling capabilities, technical qualifications, relevant integration evidence,
operational costs and remaining limitations sufficiently for its intended research
purpose. It need not await every dependent strength-development objective or an
immediate standalone Elo gain. Implementation completion or passing unit tests
alone does not establish a successful foundation, and qualification must not be
reported as proven strength superiority. Owner acceptance remains separate.

Use unsuccessful results diagnostically. According to the evidence, investigate
whether the external formulation was understood and transferred correctly, its
supporting conditions and interactions are present, the implementation or
architecture limits it, and the intended configuration was actually exercised.
Distinguish statistical imprecision from a more valuable structural question:
an adapted or related integrated formulation may be more informative than simply
extending a test. These are proportionate diagnostic considerations, not a mandatory
checklist for each failure. Select the next investigation for its justified prospect
of practical strength improvement; avoid blind parameter searches, repeated
statistically uninformative experiments and increasingly costly confirmation while
a more useful structural question remains unanswered.

Codex should autonomously pursue justified affordable investigations within the
authorized scope. If the intended strength improvement or foundational capability
cannot be established within authorized resources and scope, state the unmet
objective, what was established and why success remains unproven. Explain credible
remaining technical explanations, alternatives investigated or ruled out within
their actual evidence domains, viable next investigations, and the expected value,
cost and uncertainty of the options.
Identify any material resource, scope or owner decision required. When no responsible
autonomous continuation remains, stop and escalate that decision. The owner may
continue research, change the objective, defer it or explicitly close it without
success. Such disposition is legitimate and distinct from achieving the objective;
neither every feature gaining strength nor infinite experimentation is required.
A foundation must not be accepted merely because further testing is expensive.
Neither objective type permits impossible improvement guarantees or retroactive
relaxation of its agreed evidence standards.

## Owner-facing formulation and comparison descriptions

Internal identifiers may remain compact, but must not substitute for understandable
technical descriptions in reports, handbacks or decision summaries. For each material
experimental formulation, preserve and communicate its stable identifier, concise
descriptive name, intended improvement, essential mechanism or architectural change,
difference from its relevant baseline or predecessor, important supporting conditions
or dependencies, and experimental status with what the evidence actually establishes.

At first substantive use, pair the identifier with its descriptive name and a brief
explanation. In subsequent comparisons, retain descriptive names alongside identifiers
where they materially improve comprehension. Keep descriptions proportional to
complexity; link existing detail rather than repeat it verbosely. No new documentation
database, redundant manifest or retrospective renaming of historical identifiers or
rewriting of experimental results is required.

Distinguish Search formulations/candidates from test-domain configurations, controls
or baselines, experiment protocols, and evidence or qualification status. Worker-count
variants are test domains, not different Search formulations unless their implementation
actually changes. Explain comparisons through the formulation, domain, control and
meaning of the result. Inspect the relevant experiment before describing unfamiliar
labels such as GATE or WIDE; never invent their definitions. The Frontier's completed
TT-memory example applies this convention without changing historical evidence.

## Experimental production selection - 2 November 2026

The owner explicitly promotes **HL-G1 - request-scoped retained Search learning
with bounded history-guided reductions** to the normal experimental production
CP64 Search default for **all supported workers W1-W16**, accepting unresolved
W4 competitive uncertainty. Shared `BaselineEngine` selects **flags104071167**:
previous flags3407871 plus REQUEST_LEARNING and HISTORY_EFFORT. The existing
qualified Search kernels are unchanged; GUI Play, built-in Arena and normal UCI
consume them through the same core. CP64, PVS/ADAPTIVE16, Hash64, BOUNDARY2,
default W1, bounded Max and OWNER/ROOT_REUSE routing remain unchanged. HCE,
BREP2-32, unrelated payloads, explicit profiles and independent controls retain
their separate identities.

Production integration is **0.0.0/build85**. Its formulation remains the exact
HL-G1 `guided`/flags104071167 formulation qualified at implementation
`b8ea8b2856eed3f6c774860ca540375054e4de30`, **build84**; adoption does not rewrite
that research identity or advance another champion. P4-BASELINE-001/build79 and
the separate corrected build80 control remain immutable. The [integration record](../docs/research/search/p4-learning/PRODUCTION.md)
contains production source/artifact identities, executed qualification and limits.

Rollback: **`-Dseedv6.searchProfile=LEGACY`** restores the previous CP64 Search
policy (**flags3407871**) with current correctness/accounting fixes. The shared
explicit API and `legacyConfiguration` bypass the default/property; explicit
`ConstructionSearch` controls remain independent. Engines snapshot the selected
profile; clock/hash/worker service reconstruction retains it. Normal launches need
no property; `HL_G1` explicitly selects the new default. Invalid selections fail
rather than silently falling back. Rebuilding an exact historical binary still
requires its pinned source/artifact, not just matching flags.

Qualification includes actual production factories and W1-W16 execution,
worker-private state, legal moves/PVs, restoration/history/repetition, score
semantics, complete/verified-null accounting, cancellation/time limits/draining,
reset/reuse, GUI depth/movetime and UCI managed clocks, rollback, and normal Windows
GUI/UCI packaging with matching core identity. Headless GUI execution is not
interactive presentation acceptance; native macOS and live-client deployment are
not claimed. The frozen BOUNDARY2 studies remain unopened and clock-unqualified.
W1 confirmation remains 56.40625% over160 fresh pairs; direct W4 versus79 remains
46.09% over32 pairs, inconclusive. **Neither W4 superiority nor neutrality is
established.** Production adoption is not competitive evidence or deployment.

## Initial production baseline and current champion

**P4-BASELINE-001** is the accepted production configuration at clean `main`
commit **`753367614b8aa7e32dee337fd76b542737141ac1`**, version **0.0.0/build79**.
The initial research champion is exactly P4-BASELINE-001. Initialization changes
only documentation and does not create a new runtime artifact or experimental
candidate. Pin this commit and the configuration below for reproduction; a build
number alone is not an artifact identity.

| Dimension | Initial selection |
| --- | --- |
| Engine/Search | Shared `BaselineEngine`; Construction V1 flattened PVS; ADAPTIVE16 aspiration. |
| Evaluator | Conventional NNUE CP64, checkpoint64, S6CNU002, eight material heads; BT4_Q_LOGIT400_V1 mapping. |
| Configuration | Original 18 Construction controls plus NOMINAL_FUTILITY and VERIFIED_NULL; **flags3407871**; futility=100d, reverse=200d, extensionCap=1. |
| Resources | Requested Hash64 MiB; workers 1-16, default 1; Max bounded by available processors and 16; OWNER at W1, existing ROOT_REUSE at W2-16. |
| TT | Existing identity/layout/locking/replacement, NO_LEAF_TT and no production qTT; ordinary equal-depth/current-generation cutoff-only score admission and conservative selective-proof suppression. Extension-enabled selection suppresses ordinary score stores/reuse; completed legal move hints remain active. These describe the baseline, not experimental prohibitions. |
| Managed clocks | BOUNDARY2 (`boundary2-d6-v1`) default; explicit FACTUAL fallback via UCI ClockPolicy, JVM `-Dseedv6.clockPolicy=FACTUAL` or core API. Other request-limit contracts unchanged. |
| Retained alternatives | HCE/BREP2-32 and unrelated conventional payloads retain flags262143; explicit evaluator routes, Policy4, E008 and research controls retain their distinct identities. |

Pinned CP64 file: `build-assets/nnue-candidate/conventional-cp64.cnnue`, 3,940,952
bytes, SHA-256 `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`;
payload SHA-256 `5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6`.
The [asset manifest](../build-assets/nnue-candidate/manifest.json) retains its
historical E008 candidate scope; it is not the current production Search selector.

Focused static verification at initialization checked the shared
[BaselineEngine](../engine-core/src/main/java/com/ohinteractive/seedv6/search/driver/BaselineEngine.java),
[configuration](../engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/FlatSearchConfig.java),
[construction](../engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/ConstructionSearch.java),
clock/worker/TT and consumer bindings against the accepted records, and rehashed
the CP64 file. This was not a fresh implementation inventory or runtime validation.
The [final Phase 3 closeout](../docs/search-phase3/CHESS_SEARCH_CONTRACT.md#formal-phase-3-closeout---29-october-2026)
retains the complete historical qualifications:

- BOUNDARY2 integration evidence belongs to build77 (`bafa9e7`): 287 recorded
  passing test executions and 230 UCI checks, with the latter repeated on build77
  artifacts. Builds78/79 added research evaluator seams in SearchEvaluation and
  ConstructionSearch. No equivalent build79 runtime requalification occurred at
  Phase 3 closeout or Phase 4 initialization.
- Both independent BOUNDARY2 studies remain **clock-unqualified, scores unopened**,
  with unresolved timing cause, frequency and comparative risk. Owner acceptance
  of the production default did not confirm strength or timing safety. Do not
  reopen, replay, inspect scores or reinterpret those studies.
- CP64 selectivity has positive primary W1/10+0.1 confirmation; conservative
  bounded sensitivity crosses equality. ADAPTIVE16 W4 screening is inconclusive;
  broader multiworker/clock/platform strength remains unconfirmed. Preserve adverse
  witnesses, bounded correctness evidence, prior failures/skips and native
  GUI/macOS/client gates. Configuration gains are not isolated component gains.

## History, progression and Project Sources

Phase 3 is **CLOSED**. Its final authoritative historical masters P3-R035/P3-F035
are frozen byte-for-byte in [docs/search-phase3](../docs/search-phase3/README.md),
with hashes and original-relative-link navigation. Phase 1/2 archives and all
historical evidence remain available for selective consultation, not automatic
Phase 4 instructions. Frozen dispositions, scores, protocols and qualifications
are not changed by this new authority.

The separately authorized implementation inventory, external research map and first
TT-memory campaign's technical execution are complete; the Frontier records their
dispositions and the still-unachieved TT strength-development aspiration. CH-R001
has completed authorized execution with its strength objective unmet; GPT/owner
reconciliation remains pending. This revision neither reopens completed campaigns
nor authorizes a successor. The Frontier records priorities and hypotheses, not
a predetermined exhaustive queue.
For any future CGLHW objective, the owner must supply the **complete current
authoritative premade CGLHW GPT prompt generator before GPT prepares its execution
prompt**. Each objective requires a dedicated GPT conversation for its complete
coordination and closeout. The prior production integration was not a CGLHW; the
owner supplied the dedicated CH-R001 execution request in a new Codex conversation.
After legitimate closeout and required reconciliation, GPT selects the next
appropriate research objective from the then-current authoritative Frontier.
Codex must not select or launch a new CGLHW on its own authority.

Repository masters are authoritative; Project Sources are owner-maintained mirrors.
The owner has confirmed the final **P3-R035/P3-F035** mirrors: retain them in the
existing Phase 3 ChatGPT Project; no further Phase 3 refresh is required.
The separate Phase 4 ChatGPT Project mirrors require owner refresh to the current
**P4-R007/P4-F023** pair before mirror-dependent Project planning; the Frontier
records the last confirmed mirror state. This does not block authorized repository
work. Repository edits synchronize neither Project. On later canon changes,
increment each affected internal revision once (P4-Rnnn/P4-Fnnn), identify the
current pair and the corresponding Phase 4 mirror update needed before dependent
Project planning. No new objective starts
merely because a document, mirror or Codex turn is complete.
