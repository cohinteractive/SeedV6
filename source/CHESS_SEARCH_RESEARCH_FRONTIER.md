# SeedV6 Search Research Frontier - Phase 4

Internal frontier revision: **P4-F002**

Status: **CONSOLIDATED - 30 October 2026. Inventory closed and accounting correction technically accepted; no active Phase 4 research objective, experimental candidate or champion advancement.**

Repository master: `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` in `C:/projects/seed/java/seedv6-unified`.
Governing [Contract P4-R001](CHESS_SEARCH_CONTRACT.md). This Frontier records
current state, opportunities and dependencies; it supplies no independent launch
or promotion authority.

## Verified starting point

Initial production baseline **P4-BASELINE-001** and current research champion
are identical: commit `753367614b8aa7e32dee337fd76b542737141ac1`, version
**0.0.0/build79**, clean `main` at initialization. The Contract's
[baseline identity](CHESS_SEARCH_CONTRACT.md#initial-production-baseline-and-current-champion)
pins the configuration, evaluator hashes and qualifications.

Shared BaselineEngine retains flattened PVS/ADAPTIVE16, frozen Conventional NNUE
CP64/checkpoint64/S6CNU002/eight heads/BT4_Q_LOGIT400_V1, original18 plus
NOMINAL_FUTILITY and VERIFIED_NULL (**flags3407871**), requested Hash64,
workers1-16/default1/bounded Max and existing ROOT_REUSE. Existing production TT
restrictions and completed legal hints remain. Managed clocks use BOUNDARY2 with
explicit FACTUAL fallback; other evaluators and historical/research controls keep
their separate configurations.

Initialization on 29 October 2026 used focused static source/accepted-record
inspection and CP64 file hashing. The subsequent inventory and correction below
were separately authorized; neither redefines the immutable baseline. Build77
BOUNDARY2 integration receipts do not requalify build79 after the research
evaluator seams. Both independent BOUNDARY2 studies remain **clock-unqualified,
scores unopened**, with unresolved timing risk; do not reopen, execute, inspect
scores or reinterpret those studies. CP64 primary W1 evidence and sensitivity
limits, inconclusive W4 screening and unconfirmed broader multiworker, platform
and clock strength remain as recorded. The accounting correction does not resolve
these separate qualifications.

## Accepted inventory and accounting correction

The fresh implementation inventory at
**`94565ce67cd59d936ec6b504d669d4d3369b7a4b`** is **complete and closed**, accepted
by GPT as complete at source level. This independently authorized read-only unit
examined production composition and consumer routing, Search mechanisms, TT,
concurrency, clocks, accounting, lifecycle, chess safeguards and independent
research controls. It changed no code and ran no tests, benchmarks, tournaments
or experiments. Its full handback was not committed as a report; the material
findings are preserved here:

- **TT information effectiveness.** Both production extension features are enabled.
  Their configuration globally suppresses ordinary TT score stores and cutoffs,
  even where no extension occurs. Completed legal move hints still affect ordering,
  hash-move protection from some pruning, reduction eligibility and cross-worker
  information exchange. The production TT therefore primarily carries completed
  move information. Other observed properties are history-rich SearchKey identity,
  equal-depth/current-generation ordinary score reuse in eligible alternative
  configurations, direct-mapped replacement, no production qTT or leaf TT, and no
  separate ordinary TT selective-provenance field. See the
  [kernel](../engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/ExactSearch.java)
  and [table](../engine-core/src/main/java/com/ohinteractive/seedv6/search/tt/TTable.java).
  These are implementation findings, not evidence that relaxing restrictions gains
  strength or that TT effectiveness is an established strength bottleneck.
- **Consumer identity matters.** GUI Play and built-in Arena primarily use depth or
  movetime limits; UCI game-clock requests activate managed-clock allocation.
  Explicit UCI evaluator/model routes can differ from competitive CP64. Future
  experiments must preserve actual consumer, evaluator, configuration, worker,
  clock and resource identities.
- **Research foundations exist.** Independent recursive fixed-depth Search, TT-off
  ordered alpha-beta, unpruned qsearch controls, exhaustive oracles, TT diagnostics,
  alternative aspiration drivers and historical MTD facilities were located by
  source inspection. They were not freshly executed or validated by the inventory.

**Verified-null accounting.** A separately authorized bounded read-only
investigation confirmed the inventory's W1/multiworker discrepancy as an
implementation/accounting defect. The flattened kernel explicitly signals a
same-position legal-verifier admission through `ExactEvaluator.reenter(ply)`.
W1 charged shared and private accounting; the parallel wrapper inherited the empty
interface default, omitting admission and delegation. The callback contract and
retained implementation documentation supported charging it; no compensating
parallel admission was identified. The invariant is: **charge each successful
explicit work admission once**, including prepared legal/synthetic child transitions
and separately signalled same-position searches. This counts admission events,
not unique positions or every repeated kernel visit.

The correction is commit **`0bfb4ea8422ef78443e309930166ad46df5cc454`**,
`fix(search): account for verified-null worker reentry`, **0.0.0/build80**.
Its seven-line [ParallelSearch override](../engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/ParallelSearch.java)
restores shared admission, worker-private accounting, maximum-ply update and
callback delegation. Refusal still counts and delegates nothing; there is no
evaluator transition or intentional Search-policy/heuristic change. The commit
also adds five [focused regression tests](../research/src/test/java/com/ohinteractive/seedv6/search/exact/VerifiedNullAccountingTest.java)
and increments only the build from 79 to 80.

The accepted correction handback reports five focused tests passed, a broader
combined execution of **198 tests with zero failures or skips**, and nine
additional selected build80 qualification executions passed. Removing the fix
made three targeted admission tests fail in the negative control. Focused coverage
included production W1/W2/W8/W16 accounting, cancellation, restoration, reporting
and lifecycle behavior. GPT reconciled and accepted technical completion;
**no implementation remediation remains**. These are prior correction results,
not tests rerun for this documentation consolidation.

Build80 changes production source; it establishes neither a stronger configuration
nor champion advancement or research-driven production promotion. GPT technical
acceptance, owner acceptance, champion advancement, production promotion and
operational deployment remain distinct. No push, deployment or release occurred;
the refreshed local development install was an existing validation dependency.
Broader ROOT_REUSE ordering effects, runtime frequency, timing impact and playing
strength remain unmeasured. Full-repository, native-platform, performance and
strength qualification were not performed for the correction.

## Decisions, priorities and hypotheses

| Kind | Current state and implications |
| --- | --- |
| Adopted programme decision | Strength-first, externally informed, dependency-led and configuration-oriented research under P4-R001. This is a research reset, not an engine reset. |
| Baseline and correction | Immutable build79 baseline and research champion retained. The technically accepted build80 accounting correction is separate; no champion advancement or research-driven production promotion. |
| Completed foundation | Fresh source-level inventory closed and accepted; bounded accounting investigation and correction technically complete. |
| Planned foundation | Next intended activity: develop a fresh externally informed Search research map under separate authorization. It has not begun; do not recreate the historical 38-subject programme. |
| Early research priority | TT information effectiveness: assess how stored information supports useful Search decisions and integrated playing strength. This priority is not an active objective or a proven bottleneck. |
| Unvalidated hypothesis | Restrictive ordinary score reuse may limit useful information and driver effectiveness. The inventory establishes suppression, not a strength benefit from relaxing it. |
| Active research | **None.** No experimental candidate, external-engine investigation, tournament or CGLHW is launched. |

Potential dependencies include TT cutoff eligibility, depth/generation reuse,
selective-score provenance, bound use, replacement/aging, capacity/layout and
identity/history safeguards. Their interactions may involve **MTD(f), aspiration,
PVS, selective pruning, reductions, extensions, move ordering and parallel Search**.
For example, a driver investigation may need to understand what TT scores it can
reuse before judging the driver. These are possible dependencies for future
mapping, not validated improvements, adopted alternatives or a predetermined
research sequence.

The planned map should be strength-first and configuration-oriented, grounded in
the actual SeedV6 implementation and informed by Stockfish, Bagatur and other
relevant strong engines. Attend to mechanism interactions, supporting conditions,
safeguards and empirical evidence, with relevant source revisions. Consider
dependencies, combinations, substitutions and reverse ablations. This direction
does not select experiments or validate hypotheses. Within a later authorized
coherent objective, follow consequential dependencies and revise priorities as
evidence changes. Escalate material new objectives, safety/authority conflicts and
disproportionate expansion under the Contract; do not erect subject-by-subject
permission gates for routine work.

## Lightweight progression and handback

Reconcile this consolidation with GPT/owner; completion of the documentation
commit is not acceptance. Research-map development, new experiments, research
objectives and implementation changes require separate authorization. This unit
is not a CGLHW and launches none. In later authorized research, use economical
diagnostics, controlled strength comparisons and independent confirmation where
appropriate, preserving exact references and meaningful controls. Champion
advancement requires credible integrated-strength evidence and explicit owner
acceptance; production promotion requires separate authorization and acceptance
under the Contract.

Keep current priorities, hypotheses, adopted decisions and active work distinct.
Record material discoveries and dependencies here; focused reports/manifests hold
detail. Do not create an exhaustive taxonomy, experiment diary or automatic queue.
Each future CGLHW requires the complete current authoritative premade GPT prompt
generator from the owner before prompt preparation, and a dedicated GPT
conversation for the objective's complete coordination and closeout.

The [Phase 3 archive](../docs/search-phase3/README.md) contains the final
P3-R035/P3-F035 historical authority and links to retained Phase 1/2 history and
supporting reports. Historical restrictions and failed formulations do not
become Phase 4 policy by reference; frozen evidence remains unchanged.
The existing Phase 3 Project retains its owner-confirmed final mirrors, with no
refresh required. The current repository master pair is **P4-R001 / P4-F002**;
the Contract remains unchanged. After repository/GPT/owner reconciliation, the
owner must refresh the separate Phase 4 Project's Frontier mirror to **P4-F002**
and confirm both master identities before mirror-dependent Project planning.
That human action is pending; repository edits synchronize neither Project.
Consolidation ends here, with no active research objective, experimental candidate
or champion advancement.
