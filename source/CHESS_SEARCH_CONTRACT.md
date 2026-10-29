# SeedV6 Search Contract - Phase 4

Internal revision: **P4-R001**

Status: **INITIALIZED - 29 October 2026. Active Search research programme; no active Phase 4 research objective or experimental candidate. Production unchanged.**

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
Initialization authorizes documentation only: no implementation inventory,
external-engine investigation, experiment, tournament, CGLHW or production change.

## Strength-first, externally informed research

The principal objective is **maximizing practical playing strength under defined
computational resources**. Optimize the complete Search configuration. Depth,
nodes, NPS, computational efficiency and agreement with a finite-horizon reference
are useful measurements, but none substitutes for playing strength.

In authorized research, GPT and Codex should actively learn from established
strong engines, including **Bagatur, Stockfish and other relevant implementations**.
Inspect actual source, supporting mechanisms, eligibility, safeguards,
interactions, development history and empirical evidence where useful; record
relevant source revisions. Use observations to form SeedV6 hypotheses rather than
remaining confined to historical SeedV6 assumptions. External use makes a mechanism
worth investigating, not proof of its isolated strength contribution. Do not
blindly copy implementations or transplant parameters without SeedV6 evidence.

Follow consequential dependencies across subject boundaries within the authorized
coherent objective. For example, MTD(f) may lead to TT score reuse, then selective
provenance, replacement, aspiration or ordering. Subjects organize knowledge;
they do not constrain understanding of interacting mechanisms. Keep expansion
tied to the original objective and evidence. Escalate material new objectives,
safety concerns, authority conflicts and disproportionate scope changes.

Evaluate combinations, substitutions, reverse ablations and interactions. A weak
standalone mechanism may help another configuration; individually promising
features may interfere. One failed formulation under one baseline does not reject
a mechanism universally. Preserve negative and inconclusive evidence with its
actual scope, without reopening frozen studies or rewriting their conclusions.

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

Maintain three distinct identities:

| Role | Meaning and advancement |
| --- | --- |
| Initial production baseline | Immutable accepted Phase 3 configuration and repository identity recorded below. |
| Current research champion | Initially identical to that baseline. Advance only on credible integrated-strength evidence and explicit owner acceptance, with qualifications and supported domains recorded. |
| Experimental candidate | Reproducible alternative under investigation; implementation or a promising screen alone grants no acceptance. None exists at initialization. |

Exploration is not production adoption. Champion acceptance does not itself deploy
or change production. Promotion requires evidence, appropriate technical and
consumer/worker/lifecycle qualification, and explicit owner acceptance of the
specified production change and remaining risks. Technical completion, GPT/owner
reconciliation, acceptance and operational deployment are distinct.

Keep governance lightweight. Preserve material discoveries, dependencies,
configuration identities, adopted decisions and limitations in the two masters or
focused reports/manifests in existing research locations. Do not recreate the
38-subject programme, an exhaustive diary, process ledgers or competing canons.
Under [local evidence policy](../AGENTS.md#local-research-evidence), retain raw
output locally at recorded ignored paths, including negative/failed/interrupted
work; keep compact summaries, configurations, manifests and intentional fixtures
in Git. Check local evidence availability before analysis. Git exclusion does
not authorize deletion, protocol changes or revised historical conclusions.

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

There is no active research objective. After initialization and owner/GPT
reconciliation, the intended next work is a separately authorized fresh
implementation inventory, then an externally informed research map. The Frontier
is a small set of priorities and hypotheses, not a predetermined exhaustive queue.
For any future CGLHW objective, the owner must supply the **complete current
authoritative premade CGLHW GPT prompt generator before GPT prepares its execution
prompt**. Each objective requires a dedicated GPT conversation for its complete
coordination and closeout. This initialization is not a CGLHW and launches none.

Repository masters are authoritative; Project Sources are owner-maintained mirrors.
The owner has confirmed the final **P3-R035/P3-F035** mirrors: retain them in the
existing Phase 3 ChatGPT Project; no further Phase 3 refresh is required.
After initialization and owner/GPT reconciliation, supply **P4-R001/P4-F001** to a
**separate Phase 4 ChatGPT Project**, verify both identities and confirm setup
before planning that depends on those mirrors. Repository edits synchronize
neither Project. On later canon changes, increment each affected internal revision
once (P4-Rnnn/P4-Fnnn), identify the current pair and the corresponding Phase 4
mirror update needed before dependent Project planning. No new objective starts
merely because a document, mirror or Codex turn is complete.
