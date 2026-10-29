# SeedV6 Search Research Frontier - Phase 4

Internal frontier revision: **P4-F006**

Status: **ACTIVE - 30 October 2026. Useful TT score memory CGLHW: build79/build80 bridge characterized; opt-in H1/build81 qualified and screened in W1/W4 with promising but inconclusive results. Independent W1 confirmation interrupted by a truthful-fallback runner incompatibility; qualification selected. No champion advancement or production promotion.**

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
workers1-16/default1/bounded Max and ROOT_REUSE at W2-16. Existing production TT
restrictions and completed legal hints remain. Managed clocks use BOUNDARY2 with
explicit FACTUAL fallback; other evaluators and historical/research controls keep
their separate configurations.

Initialization on 29 October 2026 used focused static source/accepted-record
inspection and CP64 file hashing. The subsequent inventory, correction and map
below were separately authorized; none redefines the immutable baseline. Build77
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
  information exchange. A separate selective-subtree storage restriction remains:
  removing extension-related suppression alone does not establish useful general
  selective-score retention. The production TT primarily carries completed move
  information. Other properties are history-rich SearchKey identity,
  equal-depth/current-generation ordinary score admission in eligible configurations,
  direct-mapped replacement with restrictions on certain same-key replacements,
  coherent synchronized access, no production qTT or leaf TT, no separate ordinary
  selective-provenance field, and no combined ordinary record of raw static
  evaluation and searched score. See the
  [kernel](../engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/ExactSearch.java)
  and [table](../engine-core/src/main/java/com/ohinteractive/seedv6/search/tt/TTable.java).
  These are implementation findings, not evidence that relaxing restrictions gains
  strength or that TT effectiveness is an established strength bottleneck.
- **Configuration and storage dependencies.** Current
  [VERIFIED_NULL composition](../engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/FlatSearchConfig.java)
  requires all original 18 Construction controls. Reverse ablations must use valid
  configurations; silently disabling VERIFIED_NULL does not isolate removal of an
  extension or history component. Requested Hash64 produces **2,097,152 entries in
  three long arrays: 48 MiB of entry payload**, excluding JVM object, lock and other
  overhead. This is a source-derived calculation, not runtime memory measurement;
  future TT-layout comparisons must declare actual storage. These dependencies
  were identified by the accepted external research-map investigation.
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

Build80 is the qualified source-level accounting foundation for future technical
controls, not a renamed champion. Candidate/control comparisons must explicitly
identify the **build79/build80 bridge** and exact source/configuration used.

## Accepted externally informed research map

The separately authorized read-only investigation is **completed, reconciled and
accepted by GPT as complete**. It examined actual Search implementations,
information structures, TT/history/selectivity, drivers, parallel cooperation,
time management and selected development records, comparing relevant mechanisms
with current SeedV6 production source. The pinned external sources are:

| Engine | Tag | Source revision |
| --- | --- | --- |
| Stockfish 19 | `sf_19` | [`edb0d9db6731067ec50ce619ff372b463bc4dd5d`](https://github.com/official-stockfish/Stockfish/tree/edb0d9db6731067ec50ce619ff372b463bc4dd5d) |
| Bagatur 5.5c | `v5.5c` | [`56d1b3651f62bc770c7cde5f4c1b0360890b9bd5`](https://github.com/bagaturchess/Bagatur/tree/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5) |
| Ethereal 14.00 | `v14.00` | [`96cde8d2f82a3ab0536bf33572405fda96f6e236`](https://github.com/AndyGrant/Ethereal/tree/96cde8d2f82a3ab0536bf33572405fda96f6e236) |

No SeedV6 source changed, and no tests, runtime diagnostics, benchmarks, matches
or experiments ran in that investigation. Implementation observations are distinct
from author-reported empirical results and speculative SeedV6 transfer hypotheses.
Individual external testing records were not independently audited against complete
raw game evidence; inaccessible records do not become verified evidence. Stockfish's
overall release improvement is not attributed to individual mechanisms. None of
these findings establishes a SeedV6 playing-strength improvement.

- **Information effectiveness.** External TT scores, bounds, static evaluation,
  move hints, retention and histories interact in ordering, pruning, reductions,
  extensions, driver recovery and parallel cooperation, beyond repeated-position
  cutoffs. Investigate retention and consumption together: more storage or more
  aggressive reuse is not automatically better. Preserve useful completed-move
  guidance; race-tolerant external TT designs are not drop-in replacements for
  SeedV6's coherence guarantees.
- **Learning lifetime and effort.** SeedV6 already has capture and immediate
  continuation history, killers, countermoves and nominal-history filtering. Some
  learning resets at kernel invocation boundaries; ROOT_REUSE retains and seeds
  selected quiet history differently. External examples motivate investigating
  useful lifetime and signal quality together with graded history-informed LMR,
  improving signals and compatible selective learning/protection. Bounded
  correction history from search/static-evaluation differences is related but
  separate: distinguish raw evaluation, corrected evaluation and searched bounds.
- **Tactical coverage.** External singular extensions, ProbCut and selective
  verification motivate distinct TT-supported forced-line and capture-ordering/
  qsearch-supported tactical-cutoff packages. SeedV6 already has verified null,
  extensions, LMR, futility and QXRAY; addition and substitution both merit later
  consideration. Do not automatically combine the packages in a large first
  experiment. Historical mate-finding, maximum-ply and perpetual-check failures
  or reversions retain their specific scope and motivate qualification.
- **Drivers, cooperation and time.** Bagatur retains an implemented MTD-style
  alternative; Stockfish and Ethereal provide iteration-feedback, root-effort,
  worker-cooperation, history-sharing and time-management examples. MTD depends
  on supporting TT information and Search reset behavior. SeedV6 ADAPTIVE16 is
  already substantial, and ROOT_REUSE consumes completed root/ordering information.
  Sharing may trade NPS for better decisions; Search changes may alter BOUNDARY2
  spending without clock-policy code changes. Clock-policy research requires
  separate authorization; the frozen studies remain untouched and unqualified.

## Decisions, priorities and hypotheses

| Kind | Current state and implications |
| --- | --- |
| Adopted programme decision | Strength-first, externally informed, dependency-led and configuration-oriented research under P4-R001. This is a research reset, not an engine reset. |
| Baseline and correction | Immutable build79 baseline/champion retained; build80 is the separate accounting-corrected technical foundation. No champion advancement or research-driven production promotion. |
| Completed foundation | Inventory, accounting investigation/correction, P4-F002 consolidation and external research-map investigation are closed. The map is reconciled and accepted; Contract initialization statements describe historical sequencing, not unfinished work. |
| A. Useful TT memory for selective PVS | **Highest initial priority.** Hypothesis: retained heuristic scores with suitable admission, selective provenance, bound interpretation, retention/replacement and preserved completed-move guidance may strengthen existing CP64/PVS. Interactions include extensions, selective pruning, aspiration and repeated-window work. |
| B. Retained learning that allocates Search effort | **High priority; strongest competing first direction.** Hypothesis: useful history lifetime plus graded history-informed LMR and compatible learning/protection may improve strength. Build on existing history mechanisms. |
| C. Tactical coverage through verification and extensions | **Promising, dependent on reliable information and more expensive qualification.** Alternatives include TT-supported singular/forced-line verification and tactical ProbCut/qsearch cutoffs. Consider addition, substitution and reverse ablation through valid configurations. |
| D. Iteration confidence, re-search and resource allocation | **Credible follow-on.** Aspiration recovery, iteration feedback, root effort, MTD and managed-time expenditure; driver comparisons must control TT information and state lifetime. Clock-policy research needs separate authorization. |
| E. ROOT_REUSE cooperation and W2-16 information exchange | **Important, comparatively expensive and variable to qualify.** Shared useful TT information, selected history persistence/sharing, root-job ordering or overlapping Search structures; preserve concurrency, complete accounting, truthful completion, cancellation and draining. Stockfish-scale results do not transfer quantitatively by assumption. |
| Active research | **Useful TT score memory for selective PVS**, owner-authorized 30 October 2026. [Objective TT-R001 and resume state](../docs/research/search/p4-tt-memory/STATE.md). Build79 champion and build80 technical control preserved; H1/build81 passed technical qualification and W1/W4 exploratory screens. Promising point estimates remain inconclusive; the first W1 confirmation is operationally incomplete and runner qualification is selected. No champion advancement. |

These are connected relative priorities, **not an execution queue** or proven
SeedV6 improvements. Optimize integrated configurations for practical playing
strength without requiring every mechanism to win in isolation. Consider
dependencies, complements, substitutions and reverse ablations; preserve negative
evidence within its actual configuration/testing scope rather than making one
bounded failure a universal prohibition. Do not blindly copy external code or
tuning constants. Distinguish useful information from memory traffic or misleading
heuristic cutoffs, preserving chess correctness, state/history integrity,
mate/score domains, concurrency, lifecycle and truthful reporting.

Use reproducible source, evaluator, configuration, worker, actual memory, consumer
and resource identities. Node budgets, movetime, game clocks and depth are not
interchangeable; credible integrated playing-strength evidence takes priority
over standalone NPS, depth or node reductions. Within a later authorized coherent
objective, follow consequential dependencies and revise priorities with evidence;
escalate material new objectives, safety/authority conflicts and disproportionate
expansion under the Contract.

## Active first objective - owner authorized

> Determine whether bounded heuristic TT score reuse, with useful retention and
> preserved completed-move guidance, improves the practical strength of the
> existing CP64 flattened-PVS configuration.

The authorized initial formulation retains Conventional NNUE CP64, existing
extensions, flattened PVS, ADAPTIVE16, ROOT_REUSE, current clock-policy selection,
history-rich SearchKey identity initially and coherent TT access. It investigates
connected admission, provenance, retention and consumption policies, permitting
appropriate interacting mechanisms within this authorized coherent objective.
It does not automatically require clustered storage, qTT, correction history,
singular extensions or MTD. Possible first-stage diagnostics ask whether scores
are produced, admitted, retained, displaced and consumed while preserving useful
completed-move guidance. The strongest competing first objective is retained
Search history combined with graded history-informed reductions.

The owner has supplied the dedicated CGLHW execution authorization. Objective
[TT-R001](../docs/research/search/p4-tt-memory/CONTRACT.md) is subordinate to P4-R001;
[STATE](../docs/research/search/p4-tt-memory/STATE.md) is the sole objective resume
point. It holds protocol/runtime manifests, findings and next work without
creating a second programme canon. The initial source/authority challenge found
no blocking contradiction. It identified hint-retention coupling, the non-neutral
accounting bridge and the historical selectivity UCI adapter's absent current
clock wrapper as material comparison dependencies. Current experiments bind
BOUNDARY2 explicitly; frozen historical clock studies remain untouched.

The [comparison bridge](../docs/research/search/p4-tt-memory/BRIDGE.md) passed 136
behavioral requests. W1 depth5 identity matched on nine repeated positions; W4
showed scheduling variation and a stable counted-work difference. Complete
32-pair/domain 10+0.1 comparisons gave build80 versus79 scores of 51.56% W1
(paired normal95 41.99â€“61.14%) and 42.19% W4 (33.83â€“50.55%). Neither establishes
difference or equivalence; W4's adverse point estimate reinforces the need for
direct champion comparisons. The earlier 3+0.03 W1 attempt remains incomplete,
with all failed evidence retained. No historical BOUNDARY2 study was reopened.

[H1](../docs/research/search/p4-tt-memory/H1.md), source
`a72cc496fd39aeddefea4382044bd2bf2e9f6cbb` / build81, is explicitly selected by
flags7602175 and never the production default. It uses separate heuristic
provenance, ordinary scores, current-generation sufficient-depth interior
null-window eligibility and co-retained legal hints, with unchanged TT capacity
and coherent access. Its 297-test technical rerun, additional reached-null-verifier
witness and 39-request launcher isolation check passed. Failed raw-eval and
harness attempts remain preserved. Information-use and clean-cost diagnostics
support testability, not a strength conclusion.

H1's complete32-pair/domain10+0.1 screens versus corrected80 scored57.81% W1
(30W/14D/20L; paired normal95 48.63-67.00%) and57.03% W4 (25W/23D/16L;
47.13-66.93%). Both remain promising but inconclusive. The first independently
frozen128-pair W1 confirmation stopped during game113 after112 normal games:
strict Fastchess rejected baseline80's truthful legal fallback with no completed
score. The full trace records14 nodes/539316400ns/TIME_LIMIT. No completed
confirmation result, TT-strength conclusion or direct79 gate follows. Its nominal
error allowance remains spent and all partial evidence remains frozen; see the
[objective failure summary](../docs/research/search/p4-tt-memory/h1-confirm-w1-v1-failure-summary.json).
A separate process-accounting exit-time conversion failed during cleanup; actual
owned match processes drained. Selected work qualifies explicit handle draining
and a narrow, independently audited missing-score warning path before more games.
All other warning, legality/PV, terminal, accounting and provenance gates remain.
No engine/clock-policy change or retrospective protocol relaxation is selected.
W4 champion-relative improvement and independent confirmation remain unresolved.
No confirmed TT gain, research acceptance or promotion is claimed.

## Lightweight progression and handback

The inventory, correction, external map and F003 consolidation are accepted and
closed. The present owner authorization launches this specific CGLHW and permits
autonomous coherent research units under TT-R001. Completion of a Codex turn or
commit is not research closure or GPT/owner acceptance. Use economical
diagnostics, controlled strength comparisons and independent confirmation where
appropriate, preserving exact references and meaningful controls. Champion
advancement requires separate owner authorization, credible integrated-strength
evidence and explicit owner acceptance; production promotion requires separate
owner authorization and acceptance under the Contract.

Keep current priorities, hypotheses, adopted decisions and active work distinct.
Record material discoveries and dependencies here; focused reports/manifests hold
detail. Do not create an exhaustive taxonomy, experiment diary or automatic queue.
P4-R001 retains the GPT preparation requirements for future CGLHW objectives.
The current execution request is the owner's authorization for this objective;
no generator or unrelated governance is created or modified.

The [Phase 3 archive](../docs/search-phase3/README.md) contains the final
P3-R035/P3-F035 historical authority and links to retained Phase 1/2 history and
supporting reports. Historical restrictions and failed formulations do not
become Phase 4 policy by reference; frozen evidence remains unchanged.
The existing Phase 3 Project retains its owner-confirmed final mirrors, with no
refresh required. The Phase 4 Project's **P4-R001 / P4-F002** mirror pair was
already reconciled by GPT and the owner. The current repository master pair is
**P4-R001 / P4-F006**; the Contract remains unchanged. F003-F005 owner Frontier
mirror refreshes were pending and are not inferred complete. The owner must now
refresh the separate Phase 4 Project Frontier mirror to **P4-F006**, confirming
**P4-R001 / P4-F006** before mirror-dependent Project planning. This is non-blocking
for authorized repository research; repository edits synchronize neither Project.
The active objective remains open, with no champion advancement or promotion.
