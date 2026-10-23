# Phase 3: TT Information Effectiveness and Reuse

Owner-authorized CGLHW, 23 October 2026 (Pacific/Auckland). **ACTIVE: bootstrap
and current-baseline measurement.** This is subordinate evidence and continuity
under [Contract P3-R017](../../../source/CHESS_SEARCH_CONTRACT.md) and
[Frontier P3-F015](../../../source/CHESS_SEARCH_RESEARCH_FRONTIER.md), not a new
contract or an acceptance/promotion authority.

## Resume and current work selection

Next: execute the frozen [diagnostic-v1 protocol](p3-tt-effectiveness-2026-10-23/diagnostic-v1.json)
using its research-owned whole-request probe, then analyze all results before
selecting a candidate. Baseline qualification and observer tests now pass.
Runtime baseline and TT policy remain unchanged. No owner decision currently
blocks this headless work. Inspect this record and its experiment summaries
before resuming; completed protocols must not be extended or overwritten.

## Verified bootstrap

- Clean starting main HEAD `43b690f` (full identity in bootstrap manifest to follow).
  Runtime source matches `18f3c4f12a01417a48fb4c044ff3bde2c5b44b2d`: Git reports
  no changes since that anchor in engine-core, app-uci, app-gui or engine-training.
  Root VERSION_STATE.txt is 0.0.0 build 69; root CODEXLOG_CURRENT.md is absent.
  Local AGENTS.md expressly removes finalizer dependencies. No finalizer invoked;
  no journal created. Commit-based build increments apply only to shipped changes.
- Canon headers were P3-R016/P3-F014 as the owner stated. Launch supersedes earlier
  no-successor wording, preserving historical evidence. Baseline source inspection
  confirms BaselineEngine -> ConstructionSearch.v1, flags 262143/WIDEN16,
  configurable 1-16 workers and requested default Hash64. Runtime QA remains next.
- Actual frozen CP64 file is 3,940,952 bytes, SHA-256
  `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
  Loader checks length/hash, S6CNU002 and eight heads. Its manifest describes the
  historical E008 export; it is not authority to replace the selected composition.
- Unified architecture preserves one shared core; research can consume core,
  production cannot depend on research. Use worktree-local Gradle home
  `build/migration/gradle-user-home`, constrained parallelism, existing runners.
- Existing incomplete final Windows GUI interaction and native macOS acceptance
  are unchanged qualifications, not blockers to this headless objective.

## Lifecycle model and programme challenge

Static inspection establishes the following implementation facts, not runtime
verification: TTable has three long arrays, direct mapping and 32 coherent striped
locks; Hash64 requests 2,097,152 slots / 48 MiB primitive payload. Hash72 would
have identical capacity. SearchKey preserves full status and ordered relevant
real history. Each request advances generation, retaining legal older hints;
eight-bit generation wrap clears data. Ordinary proof needs same depth/current
generation, cutoff-only admission and selective eligibility. Full Construction
enables extension policies and excludes ordinary proof globally, even in subtrees
which use no extension. Quiescence and restricted root jobs are not ordinary proof
producers. Completed legal hints remain separate, with legal membership checks.
Hints cannot displace proof; deeper current-generation hints survive, equal-depth
completions refresh, and old-generation hints may be replaced. Current consumers
probe ordinary proof first and then hints, so zero proof is not zero TT traffic.

Prior Policy 4 and MTD records were consulted for mechanisms and controls. Their
Pair-2 measurements are historical, not CP64 measurements. Existing Construction
records explicitly document zero ordinary proof under extension policy. This
launch does not reopen those experiments or their outcomes.

Challenge decisions before substantial execution:

1. Do not infer a defect from sparse proof. Establish actual hint usefulness by
   counterfactual complete-request controls, rather than reward higher hit rates.
2. Do not redesign retention before separating absent producers, identity misses,
   pressure losses and consumers. Observe current storage without modifying it.
3. Do not relax proof, identity, layout, locks or replacement experimentally.
   Selectivity and extension suppression are legitimate limits. Any alternative
   proof/retention policy needs a bounded owner decision with evidence and costs.
4. Include current CP64, aspiration retries, aborted work and worker drainage.
   The old flat Pair-2 or recursive E008 runner is not the selected comparator.
5. Start with development fixtures and deterministic observation parity; use
   fresh frozen openings for any independent confirmation. Small diagnostic
   depth/node savings alone cannot establish strength or production adoption.
6. Avoid a large new tracing/orchestration framework. Reuse existing TTable
   subclassing, optional diagnostics, SearchDriver, full-history fixtures and
   tournament tools. Instrumented costs remain separate from clean timing.

## Dynamic hypothesis and decision queue

| ID | Hypothesis / discriminator | Current disposition and next gate |
| --- | --- | --- |
| T1 | Completed hints materially reduce current CP64 request work despite no ordinary proof. | FIRST: observation parity, baseline versus existing hints-off control, D6 and capped iterative requests; one worker first, representative multiworker next. |
| T2 | Current capacity/replacement loses consequential guidance. | Inspect occupancy, collision/depth/age distributions, rejected stores and repeated probes; existing Hash1 pressure and Hash64 controls. A change in policy is owner-gated, resource controls are existing settings. |
| T3 | Aspiration/PVS/iteration or root guidance consumes hints ineffectively. | Inspect existing retry and effort semantics; compare bounded existing controls only when T1 exposes decision value. Prefer consumer changes over speculative storage changes. |
| T4 | TT access/coordination costs consume the information benefit. | Clean full-request elapsed/process CPU/allocation/memory and representative worker comparison; include retries/aborted work and observer overhead. No locking/layout variant without authority. |
| T5 | Producer depth/selective restrictions leave valuable proof or guidance unavailable. | Document current restrictions and measured consequences. Preserve mathematical correctness; propose owner decision only if economically supported. |
| T6 | A promising integrated candidate improves practical strength. | Conditional: correctness first, economical paired clock development, then freeze fresh confirmation under existing 10+0.1 standard with prospective analysis/stopping. No universal game quota or automatic promotion. |

Select the next question by expected decision value. Negative findings may close
formulations; incomplete/resource-limited evidence stays inconclusive or deferred.
No unrelated evaluator/GUI/training/hot-path programme, MTD redevelopment or
automatic successor. Closure requires a defensible lifecycle account and supported
dispositions of credible affordable opportunities, not an arbitrary positive Elo.

## Evidence and resource discipline

[Bootstrap validation](p3-tt-effectiveness-2026-10-23/bootstrap-validation.json)
binds the actual tests. The first run executed 84 checks: 83 passed, one stale
research assertion failed because it expected HCE rejection after build69 added
HCE support. Its original XML/log is preserved. Only that test expectation was
corrected; the second run passed 12 checks including the affected suite, new
CP64 observer neutrality/abort/history/reset and table-counter checks, and UCI
composition. Project dependency boundaries passed. Tests overlap across runs;
96 executions are not 96 distinct tests. No production source changed.

The first diagnostic protocol uses 12 historical development roots, existing
hints-off/Hash1 controls, one-/four-worker complete requests and separately
instrumented one-worker table observations. `run_diagnostic_v1.py` freezes the
runtime and hashes before execution, runs one BelowNormal JVM at a time with
blocking waits, and preserves native lifetime CPU/peak memory and failed output.
The host is a Ryzen 5 5500, 6 cores/12 threads, approximately 32 GiB RAM. Existing
editor/Gradle processes and a SeedV3 GUI process were observed and left untouched;
development timing is not an isolated-machine or clock-strength claim.

Use `p3-tt-effectiveness-2026-10-23/` for compact protocols, summaries, identities
and manifests, with bulk output in its ignored `raw/`. Check availability before
analysis; a fresh checkout lacks local raw evidence. Preserve failures and partial
runs. Keep source/configuration, model, workers/Hash, roots/history, JVM/host,
warm-up, allocation, request CPU/time, errors and cancellation auditable. Use
BelowNormal priority and modest process concurrency. Freeze costly protocols
before results, never retune closed finalists, and account for selection effects.
Existing tournament machinery provides pair blocks and process accounting.

No new playing-strength evidence yet. No candidate promoted. Both canon mirrors
must be refreshed to P3-R017/P3-F015 before dependent external GPT planning;
Codex has not synchronized them. Repository research is already authorized.
