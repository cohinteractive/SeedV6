# SeedV6 Search Contract — Phase 2

Internal revision: **P2-R003**

Status: **Active Phase 2 Search-strength programme canon; SR-001 IMPLEMENTED for single-worker BRE-Pair 2, owner/GPT handback pending.**

Repository master: `source/CHESS_SEARCH_CONTRACT.md`.

## Authority and the frozen Phase 1 record

This Contract governs Search purpose, technical requirements, architecture and
acceptance. The [active Frontier](CHESS_SEARCH_RESEARCH_FRONTIER.md) governs
admitted subjects, dispositions, dependencies and feature selection within this
Contract. The repository masters are authoritative; ChatGPT Project Sources
are manually maintained mirrors. Neither a frontier entry nor a document edit
alone authorizes implementation.

The owner authorized the Phase 1-to-Phase 2 transition on 2026-10-08. The verified
masters were R045/F052 at `docs/search/`, rather than the expected `source/`.
The two active files now use the owner's requested `source/` paths. No executable
path or revision parser was found to depend on the former location or lineage;
live documentation navigation is updated with this transition.

| Frozen Phase 1 record | Captured revision | SHA-256 of original and archive bytes |
| --- | --- | --- |
| [Contract](../docs/search-phase1/CHESS_SEARCH_CONTRACT.md) | R045 | `9f820c2e9efd366b179902d3fc14bdd8f88e6ae793356aded6b6119f73e8401b` |
| [Frontier](../docs/search-phase1/CHESS_SEARCH_RESEARCH_FRONTIER.md) | F052 | `ead3743400b1dc513e02ad1c776b39db1486e8800c947cece9e20ba199e8fc1a` |

These are complete, byte-preserved, frozen historical archives. Their original
“active”, master-path, workflow and revision statements describe Phase 1 only.
Keep both files unchanged and together; their mutual links and Contract's
historical-report link retain their original relative relationship. Historical
reports and external evidence references remain evidence, not new authorities.
External/local evidence availability must be checked when a feature needs it;
initialization does not recertify those experiments.

Phase 2 supersedes Phase 1's research objective, adoption criteria, rejection
and reopening restrictions, programme-long Codex selection loop, and revision
lineage. Every historically rejected frontier subject is admitted for Phase 2
reconsideration. Prior negative findings remain valuable evidence and witnesses;
they impose no prerequisite to prove a dependency change before investigating
a new formulation. No Phase 1 rejection becomes a Phase 2 rejection by inheritance.

Technical inheritance is limited to the explicit foundations below and the
retained production baseline. Consult the mapped R045 sections for their detailed
semantics and the relevant F052 row/reports before researching a feature.
Do not import old feature prohibitions from adjacent historical prose.
Observed code/tests establish implementation facts; canon establishes intended
requirements. Surface material discrepancies rather than silently treating either
a design statement or an old measurement as current runtime verification.

## Strength mandate and acceptance

Maximize SeedV6's practical chess playing strength, primarily through credible
Elo/playing-strength evidence, while preserving required correctness, runtime
integrity and computational efficiency.

**BRE-Pair 2 is the intended principal evaluator for new strength research.**
Its fixed operational identity, score mapping and production configuration are
now verified by the [measurement prerequisite](../docs/research/search/BRE_PAIR2_MEASUREMENT_BASELINE_2026-10-09.md).
That work establishes controlled comparison readiness with documented limits,
not empirical centipawn equivalence or proof of relative evaluator strength.
Do not assume superiority to HCE, NNUE, BRN or any other evaluator. Preserve
supported evaluator configurations and the evaluator-independent core boundary.

Research is adoption-seeking: seek beneficial formulations, investigate failures,
compare credible alternatives, and consider interactions with evolving Search and
evaluation. A feature CGLHW may involve substantial iteration and multiple internal
workstreams. Continue while a credible, materially distinct approach remains;
one failed prototype, an unexpected result, or a completed experiment is not
grounds to stop. Historical witnesses guide investigation without prescribing
the only acceptable implementation.

A small but credible positive Elo gain can justify adoption. There is **no
arbitrary minimum Elo gain**. Small apparent gains require adequate statistical
confidence; inconclusive evidence is not an established gain. Higher computation
cost does not automatically disqualify a feature if sufficient measured strength
compensates at the relevant resource limits. Fewer nodes, greater NPS, shorter
benchmarks or deeper nominal Search do not independently demonstrate strength.

Reject a feature only after proportionate, substantial investigation finds no
credible beneficial formulation under the stated conditions. Use **DEFERRED**
when meaningful hypotheses/dependencies remain unresolved or evidence cannot
support a reliable decision. Preserve negative evidence, scope, uncertainty and
known limitations without asserting universal inferiority. Exploration budgets
and stopping designs should be proportionate to the feature and resources;
exhausting a budget alone does not establish rejection.

## Continuing technical foundations

**Binding** requirements below cannot be overturned by an autonomous canon edit.
A selected feature may research architecture and baseline-policy changes within
its launch authority, preserving these requirements. If a proposal deliberately
overturns a binding contract or requires an owner-level architecture decision,
record the proposed change and escalate before dependent adoption. Researching
a separately identified hypothesis does not silently redefine the reference.

| Foundation | Continuing requirement and detailed Phase 1 reference |
| --- | --- |
| Independent reference | Keep independently invocable recursive fixed-depth ExactSearch, TT-off ordered alpha-beta, useful independent oracles and the headless validation path. Production selectivity or different mechanics must not remove them. R045 § G, § I, and “TT-off oracle and exact TT acceptance”. |
| Negamax and alpha-beta | Side-to-move scores; negated/reversed child windows; fail-soft exact returns; nominal depth distinct from real/root ply. An ordinary child consumes one nominal ply; changed production horizons must be explicit. R045 § I. |
| PVS and re-search | First searched legal move receives the full window; later exact scouts use the null window. Strict improvement between alpha and beta requires the full re-search before accepting the improved value/PV. Classify each invocation against its own original window. R045 “Exact traversal policy”. |
| TT proof and applicability | Preserve complete value-relevant identity (board/status, evaluator definition and relevant history/path/policy), equal remaining depth and current request generation for ordinary mathematical score reuse, original-window EXACT/LOWER/UPPER meanings, and correct mate normalization. Deeper or selective results are not nominal exact proof. R045 § J. |
| Terminal/history/mates | Resolve legal exhaustion, mate/stalemate and required rule draws before leaf evaluation or TT override. Preserve the product's formal threefold and clock-100 semantics, full real-prefix/legal-path history, nonterminal twofold, legal-EP repetition identity and full evaluator-relevant status. Unknown prior history is not invented. Real ply determines mate distance; ordinary/mate bands must remain separated over supported horizons. R045 § I, SR-010 and SR-011. |
| Completion and PV | Cancelled/incomplete work cannot publish completed results or mathematical TT stores. Retain only actually completed iterations, truthful depth/work/provenance, legal moves and valid searched PV prefixes; never invent a continuation for omitted work. R045 § I, § J and § K. |
| Evidence domains | Keep invocation facts, mathematical proof, provenance/path context and heuristic prediction distinct. Raw evaluation, ordering rank, node-role predictions and selective scores are not deeper mathematical evidence. Eligibility and aggression are separate questions; no universal confidence scalar is assumed. R045 § B–F and SR-008/SR-009. |
| Selective dependency | A reduction, omission, extension, synthetic pass or restricted move domain cannot contaminate ordinary nominal TT proof. Track surviving dependencies and validate any discharge/re-search claim. Actual-depth legal proof, restricted-domain proof and selective prediction stay distinguishable. R045 SR-018/SR-019 and the proof findings within SR-021/SR-029/SR-030/SR-031/SR-032. Experimental representations there are not mandatory production structures. |
| Ownership and concurrency | Independent Search owners isolate mutable TT/history/board/PV/evaluator state. Within one request, SR-036 workers have private mutable state and a coherently shared TT/generation; immutable evaluator definitions alone may be shared. Helpers drain before publication, reset, reuse or close; stale work cannot publish and work/budget accounting includes aborted helpers. R045 § J, § K and SR-036. |
| Evaluator boundary | Keep exact recursive semantics evaluator-independent and preserve supported configurations. Choose calibrated selective composition at the appropriate adapter/factory boundary. HCE empirical margins, probabilities and policies have no automatic authority for neural evaluators. R045 evaluator boundary and SR-008/SR-009/SR-018/SR-019. |
| Mechanical economy | Prefer primitive state/arrays, packed representations, reusable scratch, direct control flow and allocation-conscious JVM mechanics, especially per move/node. Added state, allocation and abstraction require concrete need or measured value; performance cannot excuse incorrect semantics. R045 § L and § J's selected TTable mechanics. |
| Separate outcome domains | Optional root tablebase outcomes retain legality, history/rule guards and native ownership; WDL/DTZ cannot fabricate nominal scores, depth or TT proof. Clock decisions retain hard-stop precedence and actual completed depth. R045 § K, SR-013 and SR-034. |

The established unpruned SR-001A qsearch reference also remains available:
terminal/draw handling before stand-pat; stand-pat only out of check; legal
captures, en passant and all promotions at non-check nodes; all legal evasions
in check; real-ply mate distance and honest cancellation. A new production
qsearch formulation may differ explicitly and selectively; its errors and costs
must be measured without rewriting the independent reference to agree with it.

Exact-mode disagreement is not a speed optimization. Selective differences from
an exact finite-horizon oracle are research outcomes requiring analysis, not
automatically implementation defects or universal disqualification. False proof,
illegal moves, corrupted state or dishonest lifecycle publication cannot be
traded for Elo.

## Retained production baseline and change boundaries

Initialization changed no engine code. The measurement prerequisite established
the retained baseline; SR-001 subsequently changed the single-worker Pair-2 leaf
composition as documented below. Current cumulative Search policies are:

- Recursive nominal-depth TT-enabled PVS; full-window successive-depth driver;
  unpruned qsearch leaves for single-worker Pair-2, static leaves for other
  evaluators and multiworker Search; legal hash first, SEE-good then SEE-bad tacticals by immediate
  material, then main quiet history; staged/lazy generation and exact legal
  existence at non-check static leaves (SR-003/014/015/016/017/037).
- SR-010 repetition mechanics, SR-011 core mate-domain restriction and SR-012
  TTable policies: primitive direct-mapped storage, coherent striped locking,
  64 MiB requested default, NO_LEAF_TT, cutoff-only ordinary bounds and current
  replacement/hash-move behavior. Qsearch has no qTT; q-dependent nominal
  ancestor/root results do not enter ordinary mathematical TT proof. The separate
  evaluation cache stays separate.
- The bounded HCE-only SR-018 NMP and SR-019 reverse-futility/static-null
  composition, including synthetic-history isolation, ordinary-TT exclusion in
  null subtrees and affected-ancestor proof suppression. HCE fixed R=2,
  depth-4..6/surplus-512 NMP and depth-two/margin-960 static-null policies are
  historical/current HCE calibration, **not BRE-Pair 2 settings**.
- Optional guarded root tablebase outcomes, bounded clock management and
  same-depth cooperative parallel Search (SR-013/034/036), with independent
  single-worker/reference execution preserved.

A feature launch can investigate changing relevant production policies, including
leaf horizons, ordering, reductions and architecture. Adoption requires evidence,
compatibility validation and an explicit active-canon account of the changed
baseline and preserved invariants. The owner transition is not blanket authority
to redesign TTable storage/locking, game adjudication, exact proof semantics or
other binding architectural choices. Escalate a required owner decision rather
than disguising it as feature maintenance. An archived “retain current policy”
or feature non-adoption is not, by itself, a permanent Phase 2 research ban.

Broader quiet-history lifecycle, static-evaluation correction, future TT domains
and other open dependencies are accounted for in the Frontier. No initialization
claim closes them or automatically launches unrelated work.

### Verified BRE-Pair 2 measurement configuration (2026-10-09)

Disposition: **READY WITH DOCUMENTED LIMITATIONS**, technically complete for
controlled single-worker comparisons; GPT/owner acceptance is separate.
Freeze managed Best generation 32 of lineage
`235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
network SHA-256 `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Latest-training and saved explicit Play selections differ and are not substituted.
The [baseline report and evidence](../docs/research/search/BRE_PAIR2_MEASUREMENT_BASELINE_2026-10-09.md)
bind executable/source identities, external checkpoint artifacts, full options,
commands, resource controls, opening selection and statistical procedure.

Runtime is the supported `BRN_PAIR2` v1 C02 compiled order-2 tables, with private
rolling workspaces and shared immutable tables. STM output maps through
`material + .25*(raw-material)`, rounds at 100 units per pawn half away from zero,
and saturates at +/-32511 below the mate band. UCI cp exposes these integers;
the label does not establish calibrated centipawn/WDL accuracy. No unvalidated
score-dependent neural threshold is authorized. Both single/parallel production
factories correctly disable HCE NMP and reverse-futility for Pair-2; no gating
repair was necessary. SR-001's later qsearch adoption does not enable those
HCE-calibrated pruning policies for Pair-2.

Explicit UCI process properties now select a verified checkpoint/hash, report
identity, optionally warm the JVM before clocks, and publish whole-request
accounting. Invalid configuration exits without becoming another evaluator.
Ordinary startup remains HCE. The prerequisite measured the real cumulative
production driver with static leaves, one worker, requested 64 MiB TT, OwnBook
off and root tablebases off. That configuration remains an explicit static
control after SR-001; current single-worker Pair-2 production uses the qsearch
composition below. Independent exact/reference paths remain separate. The older
direct exact harness's 4 MiB TT is not this production measurement configuration.

Fastchess paired/color-reversed clock games and independent PGN/statistical audits
passed bounded A/A validation. Pair-based fixed-sample uncertainty, frozen inputs,
complete failure accounting and independent finalist confirmation are required;
tiny pilot Elo/error bars cannot establish strength. Single-worker fixed-depth
repetition, real-network reference/1-/2-worker integrity, node/time/stop/terminal
protocols and targeted regressions passed. Cross-platform performance, long-time
controls, multiworker strength, universal numerical determinism and superiority
of this checkpoint to other evaluators remain unproved. These limitations do not
invalidate the supported fixed-evaluator, single-worker comparison procedure.

### SR-001 implemented leaf composition (2026-10-10 local)

Disposition: **IMPLEMENTED**, technically validated within the authorized feature;
GPT/owner acceptance, mirror synchronization and deployment remain separate.
The [research report and reproducible evidence](../docs/research/search/SR001_QUIESCENCE_PHASE2_2026-10-09.md)
record eight distinct policy screens, two longer exploratory comparisons, the
prospective confirmation design, adverse tactical witnesses and retained controls.

Canonical single-worker `BRN_PAIR2` construction uses the confirmed unpruned
v2 PVS composition: terminal/required-draw handling before stand-pat, stand-pat
only out of check, all legal captures/EP/promotions at non-check qnodes and all
legal evasions in check. It adds no quiet checks, nominal qdepth cap, SEE/delta/
futility pruning or qTT. The absolute supported-ply guard aborts unresolved work
honestly. Nominal depth, real mate ply, completed iterations and whole main/q/
aborted work remain distinct. Depth-one mate bounds no longer assume static
leaves on this path; q-dependent ordinary nominal proof is conservatively suppressed.
No TTable storage, locking, identity or applicability rule was relaxed.

`ProductionSearch` selects the policy from immutable evaluator identity outside
the recursive hot path. Play, self-play, validation and the convenience driver
share that composition. Other evaluators and multiworker production retain their
previous policies. Ordinary exact/adapter constructors and `createStaticControl`
keep independently invocable static controls; the independent SR-001A reference
and research policy families remain available. Explicit Pair-2 UCI defaults to
production and reports actual leaves after worker changes; `static` remains a
control. Measurement commands must state whether they request production or a
control, rather than silently inheriting a changed default.

The fixed, held-out 40-pair/80-game 30+0.3 confirmation with generation 32,
one worker, reversed colors and frozen seed 20261013 scored **59/13/8 W/D/L**
for qsearch versus the preceding cumulative static baseline. Score .81875,
score-equivalent Elo +261.95; primary two-sided 95% bounded-pair Hoeffding
score interval [.6040153,1], Elo [+73.35,+infinity], passed the predeclared
positive-lower-bound criterion. Secondary pair-normal Elo interval
[+187.56,+365.09] is not the acceptance criterion. All 80 endings were normal;
input, PGN, pairing, legality and accounting audits passed. An operator-interrupted
prior attempt is retained as invalid audit evidence and contributes no results.

Integration has 433 passing broad regressions (11 existing historical tests
disabled), 250 earlier real-model independent oracle comparisons, and a frozen
production UCI bridge with 24 exact fixed-depth/node comparisons against the
confirmed candidate and old static control. Production integration at
`b12a02b7b64a` retains byte-identical recursive Search/TTable classes from confirmed
v2; the subsequent static-control lifecycle compatibility fix preserves optional
book/tablebase facilities. See the report for the final frozen source/binary
identities, commands and validation boundaries.

The observed gain is specific to this evaluator/checkpoint, opening distribution,
single-worker host and fast clock. Fixed-depth qsearch work is greater and nominal
depth at equal node budgets can be lower; strength justifies that tradeoff here.
No globally optimal formulation, multiworker/long-clock gain, universal Elo,
numerical score calibration or superiority of this evaluator is established.
Alternative qsearch policies and broader horizon/TT/ordering interactions remain
research evidence or scoped future hypotheses, not newly authorized features.

## Validation for subsequent feature research

Each successive feature compares against the currently accepted **cumulative
production configuration**, with independent exact/reference controls retained.
Verify the actual baseline; a research prototype or unaccepted prior closeout
does not silently become it. Control evaluator/checkpoint hashes and mapping,
Search options, hardware, JVM/warm-up, time controls, openings, seeds and other
material experimental conditions.

Normally separate and report:

- Semantic correctness/regressions, legality/PV, history/rule/mate, proof domains,
  cancellation/lifecycle and evaluator/worker isolation.
- Fixed-depth or fixed-node behavior; Search-tree/horizon/depth and root-move
  changes; node and evaluation counts.
- Whole-request wall-clock cost including probes, re-search, iteration attempts,
  coordination and aborted work; NPS, memory/allocation and JVM effects.
- Actual playing strength, uncertainty, resource tradeoffs and interactions
  with previously accepted features.

Use appropriate paired openings, balanced colors, controlled matches, Elo
estimation and suitable sequential or fixed-sample statistical tests. State the
comparison, hypothesis/stopping design and relevant uncertainty. There is no
universal game count or minimum effect size. Avoid suite/parameter overfitting
and selection bias; promising frozen finalists need fresh independent confirmation
beyond exploratory matches or tuning data. Do not turn favorable node statistics,
isolated tactical wins or point Elo estimates into established strength.

The first strength feature depends on this fixed BRE-Pair 2 measurement baseline,
including reproducible usable evaluator/configuration identities and a credible
comparison procedure. The authorized prerequisite has supplied that technical
foundation. SR-001 was separately launched after confirmed predecessor acceptance
and mirror synchronization. Its technical closeout does not waive owner/GPT
reconciliation or synchronization before the next separately authorized feature.

## Feature-level CGLHW, closeout and synchronization

Use one GPT conversation and normally one new Codex conversation per selected
feature. GPT coordinates selection, owner decisions, launch preparation, external
escalations, closeout reconciliation and next-feature selection. Codex executes
autonomously within that selected feature. The Frontier defines the cycle and
statuses; **Codex must return to GPT and the owner at closeout and must not
select or begin the next feature automatically**.

Within an appropriately authorized feature launch, discovery, literature/reference
investigation, hypotheses, prototypes, experiments, alternative implementations,
failure analysis, remediation, economic/strength measurement, justified production
adoption, integration/correctness validation, canon maintenance, temporary cleanup
and Git closeout may form one coherent feature unit. Numerous internal workstreams
need no new prompt. Escalate genuine external authority/information/action blockers,
not ordinary experimental difficulty. A separate prerequisite feature needs
GPT coordination, not an autonomous expansion into another feature.

Keep research evidence, accepted technical conclusions, implementation completion,
playing-strength validation, owner acceptance and deployment/operational state
distinct. A Codex disposition or commit does not claim human acceptance or
deployment. Store material conclusions, limitations, evidence locations,
dependencies, acceptance state and required human actions durably in these two
active documents as appropriate; the final chat is not the programme record.
Detailed feature evidence may live in existing research locations when needed.
Create no extra orchestration log, governance document or tracking ledger merely
to operate CGLHW.

For each content-changing update, increment only the affected document once:
Contract `P2-R001 -> P2-R002 -> ...`; Frontier `P2-F001 -> P2-F002 -> ...`.
Keep filenames stable and append a concise revision-history entry. Do not resume
Phase 1 numbering or edit its archives. Report current revisions, applicable
`CANON_UPDATED` / `FRONTIER_UPDATED` transitions and
`PROJECT_SOURCE_REFRESH_REQUIRED: YES` whenever either active canon changes.

At each feature or other external handback with canon changes, identify the exact
two active repository files and current headers for owner replacement of both
ChatGPT Project Sources. The owner refreshes **both** mirrors and confirms
synchronization before GPT relies on them for subsequent planning or constructs
the next research launch. A refresh request is not evidence it happened. Codex
uses repository masters during the selected feature; Phase 1 archives require
no routine mirroring. Codex does not modify Project Sources.

## Current programme state and revision history

The owner confirmed initialization mirror synchronization and acceptance before
launching the measurement prerequisite. That earlier gate is satisfied.
The prerequisite's P2-R002/P2-F002 handback was accepted and both mirrors were
confirmed synchronized before SR-001. SR-001 is now IMPLEMENTED with the scoped
strength/integration evidence above; no further feature is ACTIVE. This
P2-R003/P2-F004 handback requires owner replacement of both Project Source mirrors,
explicit synchronization confirmation and GPT/owner reconciliation before the
next research launch. Codex has not synchronized those mirrors or claimed owner
acceptance/deployment. SR-029 remains only a possible successor selected by GPT
from current evidence and dependencies. Build 35 is retained under the recorded
non-blocking SeedV6 version warning; no verified bump or pending repair is claimed.

| Revision | Change |
| --- | --- |
| P2-R001 | Initialize the owner-authorized strength programme from byte-preserved R045/F052; distinguish retained technical foundations from reopened historical dispositions; establish BRE-Pair 2 baseline dependency, strength acceptance, feature-level CGLHW and confirmed mirror handback. |
| P2-R002 | Record the verified generation-32 BRE-Pair 2 production measurement baseline, explicit hash-checked UCI path, unchanged HCE-only pruning boundary, paired comparison evidence/limitations and new synchronization handback; no Search feature started. |
| P2-R003 | Record strength-supported unpruned qsearch for single-worker Pair-2 production, preserved static/reference/other-evaluator/multiworker paths and proof boundaries, completed confirmation/integration evidence, limits and owner synchronization handback. |
