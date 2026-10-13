# SeedV6 Search Contract — Phase 2

Internal revision: **P2-R023**

Status: **SR-036 Phase 2 concurrency extension ACTIVE; inherited same-depth coordinator remains BASELINE. SR-007 DEFERRED is owner-accepted and BOTH P2-R022/P2-F026 mirrors are confirmed synchronized. Accepted EFFORT production unchanged.**

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
workstreams. Continue useful investigation of credible, materially distinct
approaches under the [experimental-efficiency policy](#experimental-efficiency-and-resource-allocation).
One failed prototype, an unexpected result, or a completed experiment alone is
not grounds to stop. Historical witnesses guide investigation without prescribing
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
the retained baseline; SR-001 changed the single-worker Pair-2 leaf composition,
SR-029 added confirmed quiet LMR, SR-022 adds protected shallow quiet omission,
and SR-005 adds independently confirmed completed-iteration EFFORT ordering
as documented below. Current cumulative Search policies are:

- Recursive nominal-depth TT-enabled PVS; full-window successive-depth driver;
  unpruned qsearch leaves plus protected quiet LMR and WINDOW LMP for single-worker Pair-2, static leaves for other
  evaluators and multiworker Search. Legal hash stays first. Single-worker Pair-2
  then prioritizes legal completed-PV prefixes and root alternatives ranked by
  previous inclusive effort (SR-005); ordinary SEE-good/SEE-bad tactical material
  and main quiet-history ordering follow. Other evaluators/workers retain their
  ordering. Staged/lazy generation and exact legal existence remain, with eligible
  reuse forcing pending quiet generation (SR-003/014/015/016/017/037).
- SR-010 repetition mechanics, SR-011 core mate-domain restriction and SR-012
  TTable policies: primitive direct-mapped storage, coherent striped locking,
  64 MiB requested default, NO_LEAF_TT, cutoff-only ordinary bounds and current
  replacement/hash-move behavior. Qsearch has no qTT; q-dependent nominal
  ancestor/root results do not enter ordinary mathematical TT proof. Reduced-work
  and omitted-work dependencies also conservatively suppress nominal proof,
  including after reduction verification; no dependency discharge is inferred. The separate
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
passed bounded A/A validation. That prerequisite used pair-based fixed-sample
uncertainty. Frozen inputs, complete failure accounting and independent finalist
confirmation remain required; subsequent designs follow the
[experimental-efficiency policy](#experimental-efficiency-and-resource-allocation).
Tiny pilot Elo/error bars cannot establish strength. Single-worker fixed-depth
repetition, real-network reference/1-/2-worker integrity, node/time/stop/terminal
protocols and targeted regressions passed. Cross-platform performance, long-time
controls, multiworker strength, universal numerical determinism and superiority
of this checkpoint to other evaluators remain unproved. These limitations do not
invalidate the supported fixed-evaluator, single-worker comparison procedure.

### SR-001 implemented leaf composition (2026-10-10 local)

Disposition: **IMPLEMENTED**, technically validated within the authorized feature.
The 2026-10-10 SR-029 launch confirms GPT/owner acceptance and synchronization of
the P2-R003/P2-F004 mirrors; those predecessor gates are satisfied. Deployment
remains a separate action.
The [research report and reproducible evidence](../docs/research/search/SR001_QUIESCENCE_PHASE2_2026-10-09.md)
record eight distinct policy screens, two longer exploratory comparisons, the
prospective confirmation design, adverse tactical witnesses and retained controls.

SR-001 established the canonical single-worker `BRN_PAIR2` confirmed unpruned
v2 PVS composition: terminal/required-draw handling before stand-pat, stand-pat
only out of check, all legal captures/EP/promotions at non-check qnodes and all
legal evasions in check. It adds no quiet checks, nominal qdepth cap, SEE/delta/
futility pruning or qTT. The absolute supported-ply guard aborts unresolved work
honestly. Nominal depth, real mate ply, completed iterations and whole main/q/
aborted work remain distinct. Depth-one mate bounds no longer assume static
leaves on this path; q-dependent ordinary nominal proof is conservatively suppressed.
No TTable storage, locking, identity or applicability rule was relaxed. SR-029
adds reductions to the main search below without changing these leaf rules.

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

### SR-029 implemented reduction composition (2026-10-10 local)

Disposition: **IMPLEMENTED** within the authorized SR-029 workstream. The SR-006
launch explicitly confirmed owner/GPT acceptance and P2-R004/P2-F006 mirror
synchronization; those predecessor gates are satisfied. The
[research report and raw evidence](../docs/research/search/SR029_LMR_PHASE2_2026-10-10.md)
retain historical tactical failures, distinct fixed/scaled/scout alternatives,
replay-derived quiet protection, costs, prospectively frozen comparisons and
integration identities. These are the selected production policy and measured
conclusions, not immutable thresholds for every future formulation.

Single-worker Pair-2 production uses scaled quiet reductions with the first
three **searched quiet moves** protected, independently of global move ordinal.
Eligibility requires parent depth at least three, non-root parent, global and
quiet ordinal at least four, and a non-checking quiet child from an unchecked
parent. Hash/first moves, captures/EP/promotions, synthetic-null subtrees,
mate-band windows and a nominal horizon reaching the clock-100 boundary are
protected. Reduction is one ply, two at parent depth >=5/global ordinal >=8,
three at depth >=8/global ordinal >=16, capped to retain one main child ply.
Existing move ordering and per-invocation quiet-history lifecycle are unchanged.

The reduced null-window observation is selective. Every alpha-raising or
mate-band return receives a nominal-child-depth null-window verification;
strict nominal improvement inside beta receives the normal full PVS re-search.
An accepted reduced fail-low predicts the captured alpha and does not fabricate
a nominal fail-soft value or PV suffix. A monotone selective dependency marker
conservatively suppresses affected ordinary TT stores even after verification;
unaffected actual-depth proof retains all existing identity/depth/generation,
history, mate-normalization and original-window requirements. Q-dependent proof
suppression remains intact. No TTable storage/locking redesign or HCE pruning
transfer accompanies LMR.

The child board/evaluator/history preparation is shared across probes and retries;
every descendant admission and aborted iteration remains budgeted. Main/q domain
accounting uses the actual invocation domain, not the original root-depth cutoff.
The one-main-ply minimum preserves that prepared child's domain across retries;
future zero-depth reductions would require revisiting domain assignment.
Final cancellation is checked before publishing the completed root result.
Independent exact/static/unpruned controls, real-ply mates, rule draws, ownership,
legal PV prefixes and supported-horizon abort semantics remain available.

Independent held-out **256-pair/512-game**, 30+0.3, generation-32 confirmation
against accepted SR-001 scored **231/119/162 W/D/L**, .5673828125, **+47.11 Elo**.
The predeclared approximate paired-normal two-sided 95% Elo interval
**[+21.06,+73.69]** and required conservative 2.0 x pair-SE sensitivity
**[+20.53,+74.24]** both pass the positive-lower-bound criterion. Bounded-pair
Hoeffding sensitivity **[-12.16,+109.27]** includes zero: this is an approximate
paired inference, not a Hoeffding-confirmed finite-sample claim. All games,
legality/history/pairing/accounting and stable-input audits passed. No exploratory
or 128-game finalist result is pooled into confirmation. Four independent games
ran concurrently, each engine using one Search worker, on the same Windows host.

Integration source `8940a5f9bf35e3dbb4012b88fdcce4f7294f108e` passed **462**
broad regressions (11 existing historical disabled), nine Python tests and
**36** frozen deterministic production/off/static comparisons, with actual
default/worker switching, budget/stop/terminal/invalid-model checks. All 22
recursive Search/TT classes match confirmed v2 bytes. The report binds exact
source/JAR/checkpoint identities and earlier proof/oracle/replay evidence.
`ProductionSearch` serves Play, self-play, validation and the convenience driver.
Explicit UCI `lmrResearch=off` retains SR-001 through production-leaf control;
absent/default `production` resolves actual quiet/off policy by worker/evaluator.
Static and other qsearch controls do not silently enable LMR. Other evaluators
and multiworker production are unchanged; ordinary startup remains HCE.

At depth six over the diagnostic corpus, quiet LMR saves about 62.5% of nodes;
at a 1M admitted-node budget, mean completed depth is 7.1 versus 6.0 on the 20
budget-exhausting roots. Clean measured request cost is similar, with JIT and
allocation limits recorded; these economics alone do not establish strength.
The gain is scoped to the frozen checkpoint, opening distribution, host and
fast clock/resource condition. Long-clock, other-weight, cross-platform and
multiworker strength, universal tactical safety and global optimality remain
unproved. Ordering/history and reduced-learning implications inform later
SR-007/022/023/033 selection without activating them.

### SR-006 aspiration investigation closeout (2026-10-11 local)

The SR-022 launch explicitly confirms owner/GPT acceptance of this scoped rejection
and synchronization of both P2-R005/P2-F008 mirrors; those predecessor gates are satisfied.

Disposition: **REJECTED** for the investigated single-worker generation-32
Pair-2 conditions after six formulations, 640 development
games and a fresh independent 2,048-game confirmation. This is scoped
non-adoption, not proof of universal inferiority or zero benefit. The
[report and raw evidence](../docs/research/search/SR006_ASPIRATION_PHASE2_2026-10-10.md)
retain fixed narrow/wide fallback, directional and volatility-adaptive widening,
one-sided lower windows, parity centering, adverse witnesses, costs and failures.

Frozen ALPHA16 versus accepted cumulative SR-001+SR-029 scored **809/453/786
W/D/L** over **1,024 unique held-out pairs**, 30+0.3, one worker, generation 32,
four independent concurrent games. Estimated gain is **+3.90 Elo**. The
prospectively three-look-adjusted paired-normal interval **[-12.30,+20.12]**,
required 2.5-SE sensitivity **[-13.02,+20.84]**, and adjusted bounded-pair
Hoeffding interval **[-29.77,+37.64]** all include zero. Both earlier prescribed
looks required continuation; the final result is **inconclusive**, not adoption.
All stages and independent legal/history/pairing/resource/input audits passed.
No development pooling, within-stage stopping, tuning or post-hoc extension
occurred. Approximate inference and ordinary-desktop contention limits remain.

The full-window cumulative production composition is retained. Experimental
aspiration is explicitly selectable only for single-worker Pair-2 cumulative
qsearch+quiet-LMR; it is off by default. Other evaluators, ordinary HCE startup,
multiworker, exact/reference, unpruned-only/static controls and managed lifecycle
composition remain unchanged. No TT/history, nominal-depth or pruning redesign
was made. Research state resets across requests; same-depth retries retain the
invocation's original-window proof, history and generation. Failed bounds or
aborted retries cannot publish completed depth/PV; all attempts count toward
whole-request work. Newly returned mates receive full-window verification.

WIDEN16/ALPHA16 reduce measured fixed-depth tree/time costs, but increase request
allocation and show nearly unchanged fixed-node throughput. Selective root
scores/moves can differ; qsearch/LMR proof suppression and per-invocation history
reset make retries expensive. These observations did not establish strength.
The final retained-code suite passes **670 tests**, with 11 pre-existing disabled
and no failures/errors; all 702 rebuilt main classes match frozen v3. Independent
real-model qsearch oracles, budget/stop/history/PV and frozen runtime bridges
also pass. No aspiration production integration or deployment is claimed.

No distinct, evidence-supported beneficial formulation remains identified by
this investigation. Untried constants/gates are not proven inferior; future
reconsideration may follow newly evidenced retry/quality mechanisms or changed
Search/evaluator conditions under a new authorized feature selection. No other
Frontier subject is activated by those implications.

### SR-022 implemented omission composition (2026-10-12 local)

Disposition: **IMPLEMENTED**, technically complete under the authorized SR-022
workstream. The SR-023 launch confirms owner/GPT acceptance and replacement/
synchronization of both P2-R006/P2-F010 mirrors; those gates are satisfied.
Deployment remains separate. The
[research report and raw evidence](../docs/research/search/SR022_LMP_PHASE2_2026-10-11.md)
preserve historical failures, six distinct formulations, adverse replays, complete
request costs, 448 development games, four separate A/A games, the frozen
confirmation protocol, all three complete looks and production integration.

Single-worker Pair-2 production adds **WINDOW** to full-window iterative PVS,
unpruned qsearch and protected quiet LMR. At non-root parent depth one/two, omit
eligible quiets only after **six/ten actually searched quiet moves**. The parent
must contain more than six pieces including kings and be unchecked, with at least
one searched child. Eligibility requires an actual scout or an invocation whose
**original** window is one score unit; an originally wide invocation that narrows
during traversal does not qualify. Root, hash/first moves, captures/EP/promotions,
castling, giving check, synthetic-null paths, mate-band windows and a child's
nominal horizon reaching clock 100 remain protected. These are the confirmed
policy's scoped parameters, not universal optimal constants.

An omitted legal move has no searched score, evaluator-child transition, history
push, admitted descendant or invented PV. Omission predicts at least the current
alpha floor, avoiding a fabricated fail-soft losing mate when legal alternatives
remain. A monotone selective dependency suppresses affected ordinary TT proof
through ancestors; there is no new dependency discharge or TT architecture.
Terminal resolution, original-window classification, actual-depth proof and
qsearch/LMR suppression retain their existing rules. Removing omissions preserves
the compact actually searched prefix, quiet/global searched counts and lazy tie
order; omitted moves receive no searched-prefix history malus. Whole request
time includes eligibility/board preparation, and work accounting includes all
actual main/q/probe/re-search/aborted admissions. No per-node objects were added.

The independent predeclared three-look confirmation completed **1,024 fresh
color-reversed pairs / 2,048 games**, all normal, against accepted SR-001+SR-029:
**876/485/687 W/D/L**, score .546142578125, **+32.15 Elo**. Generation 32,
30+0.3, one worker, four concurrent games, seed 20261124 and opening partition 1
were frozen. Primary three-look-adjusted paired-normal **[+16.09,+48.36]** and
mandatory 2.5-SE **[+15.38,+49.08]** both pass the declared positive criterion.
Adjusted bounded-pair Hoeffding **[-1.53,+66.46]** includes zero: the supported
inference is approximate, not distribution-free confirmation. Both earlier looks
required continuation; no within-stage stopping, retuning, development pooling
or post-stop extension occurred. All legality/full-history/pairing, exclusions,
runtime/resource and frozen-input audits pass. The requested pause after stage
two and subsequent RESUME changed no experimental conditions.

Clean integration source `2f08516d0f24c9a08946bff122dcf5f3a0c8667f`, frozen
production v5, app.jar SHA-256
`0ca1af7b730040aa26828133539f5c2977ae69fb8cfbcdf233c16db969e6d2c8`,
retains the measured v4 recursive class bytes. All **703** rebuilt main classes
match v5; only four integration units and their three bytecode-equivalent inner
classes differ from v4. **690 tests pass**, 11 pre-existing disabled, zero
failures/errors; 12 live-adapter Python tests pass. Two real-model runtime bridges
provide **116** deterministic score/PV/depth/node/bestmove comparisons: actual
default production equals frozen WINDOW, explicit LMP-off equals accepted SR-006
v3. Budget/time/stop/terminal preflights and actual all-policy-default 1→2→1 worker
switching pass, restoring identical single-worker behavior with stable inputs.

`ProductionSearch` serves Play, self-play, validation and convenience drivers;
tests exercise actual omissions through those consumers. Default UCI reports
WINDOW only for the selected composition; `lmpResearch=off` preserves cumulative
quiet-LMR control, and `lmrResearch=off` preserves SR-001. Explicit research
policies remain marked experimental. Static/exact/reference, other qsearch/LMR
and aspiration controls do not inherit LMP. Production and explicit controls
retain configured book/tablebase services. HCE startup, other evaluators and
multiworker algorithms remain unchanged; no HCE pruning transfers to Pair-2.

Depth-six corpus nodes fall **46.93%** versus the cumulative control. Clean v3
whole-request elapsed ratios are **.5891 / 1.1118 / 1.1474** at depth six / 100k /
1M nodes: fixed-depth savings coexist with worse fixed-node throughput. V4's
count-first implementation modestly improves or preserves v3 cost with identical
trees; v4/v3 elapsed ratios **.9705 / .9799 / .9953** do not establish new strength.
Main/q composition, allocation/JIT and desktop contention limit causal attribution.
The clock match, not node savings or nominal depth, justifies adoption. Sparse
king/pawn and early quiet resources explain protection choices; local omission
replay rates are correlated diagnostics, not calibrated error probabilities.

Long-clock, other-weight, other-platform and multiworker strength, universal
tactical safety and global policy optimality remain unproved. Future ordering
or history changes require count-policy reassessment; omitted moves supply no
negative search labels. TT-domain/extension/learning implications inform
SR-004/005/007/023/030/033 without selecting or implementing them. No visual GUI
change or GUI QA, unrelated full training suite, release or deployment is claimed.

### SR-023 history investigation closeout (2026-10-12 local)

**Owner-accepted DEFERRED; no production adoption.** The SR-030 launch confirms
completed GPT/owner reconciliation and BOTH P2-R008/P2-F012 mirror replacements
with explicit synchronization. Historical pending statements are satisfied. The
[feature report](../docs/research/search/SR023_HISTORY_PHASE2_2026-10-12.md) and
[raw evidence](../docs/research/search/sr023-phase2-2026-10-12/) retain six
history-conditioned protection/additional-omission/replacement formulations,
448 development games and 2,048 independent confirmation games. The unchanged
REPLACE finalist scored **810 wins, 460 draws, 778 losses**, +5.43 Elo; adjusted
paired-normal interval **[-10.56,+21.44]**, required 2.5-SE **[-11.26,+22.15]**,
adjusted Hoeffding **[-28.23,+39.19]**. The frozen 128/384/1024-pair procedure
finished **inconclusive**. Its sample cannot be extended after that decision.
The meaningful possibility of a small benefit remains unresolved; insufficient
evidence is neither an established gain nor grounds to declare all formulations
inferior. Resumption requires a separately justified, fresh independent design
with appropriate precision and error accounting, under a new GPT/owner selection.
No such successor work is activated by this record.

Accepted single-worker Pair-2 remains full-window SR-001+SR-029+WINDOW. Frozen
accepted v5 and candidate v2 remain independently reproducible; the six history
policies are explicit research controls only. Other evaluators, multiworker
production, exact/static/qsearch, LMR-off/LMP-off and aspiration controls remain.
All 703 final compiled main classes match the frozen v2 research JAR; default
and control UCI equivalence and 714 broad tests pass (11 existing skips).

Main quiet history is conditional evidence from completed quiet cutoffs, reset
per invocation and privately owned. Ordering already protects most positive
history; zero mixes unobserved and balanced evidence. Negative history can omit
useful resources. Existing reduced fail-low maluses do not imply nominal-depth
verification; omitted moves receive no searched score, PV continuation or malus.
All candidates preserve root/check/tactical/checking/castling/hash/early-quiet,
sparse/mate/clock protections, actual searched prefixes and conservative nominal
TT-proof suppression. No history lifecycle, ordering or TT architecture changes.
These findings inform SR-014/SR-016/SR-022/SR-029/SR-033 without activating them.

Evidence is single-worker, one frozen generation-32 checkpoint, Java 21/Windows,
15+0.15 confirmation. No longer-clock, multiworker or other-evaluator strength is
claimed. Broad changes in history architecture remain untested; they are not
invented prerequisites. Both final active masters require owner replacement and
explicit synchronization, plus GPT/owner handback reconciliation, before dependent
planning. Codex technical completion does not establish those external actions.

### SR-030 check-extension technical closeout (2026-10-13 local)

**DEFERRED; no production adoption.** Six materially distinct bounded check
mechanisms received semantic, tactical, cost and clock investigation. The
[feature report](../docs/research/search/SR030_CHECK_EXTENSIONS_PHASE2_2026-10-12.md)
and [raw evidence](../docs/research/search/sr030-phase2-2026-10-12/) retain
704 development games and two separate prospectively frozen 2,048-game
confirmations. Development and candidates are never pooled for inference.

FORCED (exactly one legal evasion at a checked main node, at most two added
plies/path) scored **787/444/817 W/D/L**, **-5.09 Elo**, adjusted paired-normal
**[-21.56,+11.36]**, required 2.5-SE **[-22.29,+12.08]**, adjusted Hoeffding
**[-38.85,+28.57]**. Its positive development signal did not replicate.
BOUNDARY (giving check at parent depth one, at most one added ply/path) scored
**830/445/773**, **+9.67 Elo**, adjusted **[-6.61,+26.00]**, 2.5-SE
**[-7.34,+26.73]**, Hoeffding **[-23.96,+43.49]**. Both finished inconclusive;
every input/legal/history/pairing/termination/resource audit passed. Each used
three prescribed 128/384/1024-pair looks and .025 approximate positive-tail
allocation, together at most .05 for the feature. Neither completed sample may
be extended, retuned or reinterpreted as an established gain or loss.

BOUNDARY retains a plausible low-cost modest benefit, but its uncertainty
prevents adoption and a reliable blanket rejection. Broader checking/evasion
policies cost substantially more without compensating development evidence;
LEAF's early favorable screen failed fresh replication; REPEAT supplied no
positive screen. This deferral records a specific remaining precision deficit,
not unfinished ordinary validation or proof that finite sampling excludes all
check-extension benefit. Resumption needs fresh justified independent evidence,
adequate opening coverage and precision, explicit error accounting and a new
GPT/owner selection; no arbitrary sweep or successor is authorized here.

Unpruned checked qsearch already searches all evasions. A last-ply checking-edge
extension need not introduce a new unpruned leaf domain, although main/q ordering,
PVS/history and selective work differ. Extending an evasion can introduce a quiet
reply otherwise beyond the main horizon. Local mate improvements and node/time
changes do not establish clock strength. Path-local credit changes value
applicability: incompatible ordinary TT reads are excluded while credit remains,
and extension-dependent ancestor proof stays selective. Scouts and full retries
reuse the same edge depth/credit; admitted work, nominal depth, actual added
plies and real-ply mate/limits remain distinct. At unsupported unresolved depth,
completion is honestly aborted after terminal/draw handling. No TT architecture,
qsearch redesign, evaluator mapping or neighboring extension family is adopted.

All six policies remain explicit research controls, OFF by default. Accepted
single-worker Pair-2 stays full-window SR-001 + SR-029 + WINDOW, without aspiration,
history pruning or check extensions. HCE, other evaluators, supported multiworker
Search and prior exact/static/qsearch/LMR/LMP/aspiration/history controls remain.
Final runtime source `41587d2a80995cffe60acdf5859f5aa9b9846e54` matches all 704
compiled main classes in frozen v2; accepted comparator remains source 2f08516d0f24.
Broad validation passes **726 tests**, with 11 opt-in instrumentation skips;
focused/oracle/TT/history/mate/cancellation/accounting and real-UCI bridges pass.
Full identities, resources, tests and retained negative evidence are in the report.

Evidence is one frozen generation-32 Pair-2 checkpoint, shared Windows/Java21,
15+0.15, one worker and four concurrent games. Normal inference is approximate;
small effects, longer clocks, other weights/platforms and multiworker strength
remain unresolved. No new production integration is justified. Build 35 is
unchanged with the disclosed non-blocking initialization warning; finalizer status
is idle, with no pending token or required cleanup/repair. GPT/owner technical
reconciliation and owner replacement of BOTH current Project Source mirrors
(revision targets below), with explicit synchronization, block dependent GPT
planning. No acceptance, mirror synchronization, deployment or successor selection
is claimed.

### SR-005 implemented iteration reuse (2026-10-13 local)

Disposition: **IMPLEMENTED**, technically integrated and validated. Owner/GPT
acceptance, BOTH Project Source mirror replacement/confirmation and deployment
remain separate. The [report and evidence](../docs/research/search/SR005_ITERATION_REUSE_PHASE2_2026-10-13.md)
retain the bootstrap, six formulations, 624 diagnostic observations, 1,872 clean
cost requests, 192 development games, eight A/A games, the prospectively frozen
confirmation and all integration checks. No successor is active or selected.

Single-worker Pair-2 production now uses **EFFORT**: legal prior-PV hints only
along the identical real move prefix, plus stable descending root-alternative
ordering by the previous iteration's inclusive admitted node effort. Legal TT
moves retain precedence, then the previous root/PV move, then ranked alternatives
at root, then ordinary ordering. Reuse is heuristic only: it supplies neither
scores/bounds/cutoffs nor fabricated PV, completion, nominal depth or TT proof.
Root/PV/rank state is private and bounded; no per-node allocation is introduced.
Only completed adjacent full-window iterations of the same full board/status,
real history and fixed evaluator/policy/owner/request qualify. Narrow/skipped,
changed-root/history, failed or incomplete work invalidates it; begin/end/newGame
prevent cross-request retention. Production quiet history still resets each depth.

PVS re-search, actual searched global/quiet ordinals, protected quiet LMR and
WINDOW omission schedules are unchanged; ordering changes their encountered
work and can change selective root decisions. Qsearch consumes no hint. Ordinary
TT depth/generation/identity/mate/original-window rules and qsearch/LMR/LMP proof
suppression remain. A nonzero prior-PV hint, including a tactical one, or eligible
root ranking can force earlier quiet generation/history sampling; this is part
of the measured formulation, not an unmeasured optimization.

Against accepted cumulative source `2f08516d0f24c9a08946bff122dcf5f3a0c8667f`,
frozen candidate `4ecaf783b57d42e82b07da109ce4d264f74c55df` scored **130/58/68 W/D/L**
in **128 fresh reversed-color pairs / 256 normal games** at **15+0.15**. Generation
32, the fixed quarter-residual mapping, one Search worker and four concurrent
games were held fixed. Point gain **+85.85 Elo**, prospectively adjusted paired-normal
interval **[+36.87,+138.45]**, required 2.5-SE **[+39.38,+135.56]**. Adjusted
Hoeffding **[-17.54,+207.38]** includes zero: inference is approximate, not a
bounded-distribution proof. First-confirmation alpha .025 is split over three
planned looks, within prospective family spending .05/2^confirmation-index.
All frozen input, legal/history/pair/resource audits pass. The positive rule
closes at the first look; later stages are unplayed, with no extension, retuning,
development pooling or adverse-game exclusion.

Phase 1's TT redundancy does not transfer to this selective composition: 1,292
of 1,441 observed prior-PV candidate nodes lack a TT move; 144 match and five differ.
These repeated diagnostics are not independent population rates. EFFORT has .765x
D6 nodes and .773x elapsed in the clean corpus. On matched requests that both
reach the full 100k/1M caps, elapsed is 1.011/1.010x: useful savings come from
less work, with roughly one percent lower local throughput, not a universal NPS
increase. Allocation/JIT, early completion, selective quality and shared-host
limits remain in the report. No strength transfer to longer clocks, other models,
other evaluators or multiworker is claimed.

Integrated source **`450988455af31383d17d126c6f51d277451f5a18`**, frozen
`app/build/sr005-p2-production-v2`, app.jar SHA-256
`dfc9d9cb3e5d372eb40015fcc5b53093c5841d09ddad2960cc7c54f21bb4abf8`,
retains the measured core/adapter class bytes; all 706 rebuilt classes match its
JAR. Broad validation: **741 passed, 11 pre-existing disabled, zero failures/errors**.
Real-model runtime checks pass **64 actual-default comparisons against EFFORT,
64 explicit-OFF comparisons against accepted production, and 291 preservation/
worker checks**, including 1->2->1 transitions, HCE, historical research controls,
invalid model rejection, legal PVs and whole-request budgets/stops. Unit coverage
includes book-enabled default/OFF facilities, managed owners, self-play/validation
routing, failure recovery and independent exact/rule/proof references. GUI, native
Syzygy execution and expensive training/long-clock/cross-platform strength suites
were not run; no UI or native tablebase implementation changed.

Omitted `seedv6.uci.reuseResearch` (or `production`) selects the actual default;
explicit `off` and the named factory/lifecycle controls retain pre-SR-005 WINDOW.
Older research controls retain their measured compositions with reuse OFF;
explicit root/pv/score/history/pvhistory remain research-only, OBSERVE diagnostic.
They are not rejected or proven worse; the exploratory data do not establish
relative superiority or additivity. Independent static/unpruned/LMR controls,
other evaluators, multiworker, optional book/tablebase and lifecycle semantics
remain available. No aspiration, history pruning or check extension becomes
production. Build 35 is unchanged after the non-blocking initialization warning;
final version status is idle with no token, unfinished operation or repair.

### SR-018 Pair-2 activation (2026-10-13 local)

The owner explicitly confirms SR-005 IMPLEMENTED acceptance and replacement/
synchronization of BOTH P2-R013/P2-F017 Project Source mirrors after that handoff.
All earlier SR-005 pending-acceptance/synchronization statements below are historical
and satisfied. Only the bounded single-worker BRE-Pair 2 SR-018 extension was activated;
the inherited HCE fixed-R null-move functionality remains separately BASELINE.

Clean starting HEAD `901842282eb2` and all 315 main-source hashes match accepted
SR-005 production source `450988455af31383d17d126c6f51d277451f5a18`. Frozen
`app/build/sr005-p2-production-v2`, app.jar
`dfc9d9cb3e5d372eb40015fcc5b53093c5841d09ddad2960cc7c54f21bb4abf8`, is the primary
comparator: full-window PVS, unpruned qsearch, protected quiet LMR, WINDOW and EFFORT.
Explicit reuse OFF is the older comparator, not this feature's accepted baseline.
Checkpoint and frozen archives match their recorded hashes. The
[SR-018 report](../docs/research/search/SR018_NULL_MOVE_PHASE2_2026-10-13.md)
records bootstrap challenges, historical witnesses, prospective experiments and
continuation. No new pruning is adopted at activation. Existing proof/history,
terminal, evaluator, ownership and lifecycle requirements remain binding.

The activation authorized autonomous SR-018 investigation and selected no successor.
It superseded historical predecessor handback language. The technical closeout
below now governs current status; owner/GPT reconciliation and mirror handback
remain external actions.

### SR-018 Pair-2 technical closeout (2026-10-13 local)

**DEFERRED; no production adoption.** Historical HCE SR-018 remains separately
BASELINE. The [report](../docs/research/search/SR018_NULL_MOVE_PHASE2_2026-10-13.md)
and [evidence](../docs/research/search/sr018-phase2-2026-10-13/) retain five pruning
formulations, OFF/OBSERVE controls, 256 development games, 256 independent
confirmation games, eight A/A games and semantic/resource validation.

SHALLOW's frozen fresh confirmation closes at the first 128-pair look for
prespecified **computational futility**: 99/60/97 W/D/L, +2.71 point Elo; adjusted
paired-normal interval [-49.71,+55.26], required 2.5-SE [-46.93,+52.47], adjusted
Hoeffding [-101.77,+107.71]. Family alpha .05 spends .025 for this candidate over
128/384/1024 looks; normal-eligible plug-in probability below .10 closes early.
Observed probability .00809 triggers that rule. This is inconclusive, not
inferiority or equivalence. Do not extend or pool the closed sample.

Unchanged ADAPTIVE receives a separate fresh 64-pair development follow-up at
15+0.15: 49/33/46, +8.14 Elo, exploratory normal interval [-41.45,+58.07]. It is
not confirmation. Both stronger formulations save substantial diagnostic work
(SHALLOW/ADAPTIVE D6 elapsed ratios .7494/.6569) without establishing stronger
clock play. FIXED/VERIFIED initial clock signals are adverse; verification filters
no observed D6 cuts; GUARDED100 has negligible opportunity. All 520 game endings
are normal and complete identity/PGN/history/pairing/accounting audits pass.

Modest benefit remains unresolved. Another ADAPTIVE confirmation would cost
about 45 minutes to six hours; weak observed means imply low plug-in power,
while development uncertainty still permits useful effects. The report preserves
both forecasts and the allocation judgment. No universal budget, minimum Elo or
sample quota establishes this disposition. Resume under a new authorized feature
selection when a credible error-quality mechanism, fresh stronger development
signal, more economical adequate-precision measurement, or changed Search context
warrants it. These are useful grounds, not new prohibitions on authorized research.
Any new confirmation needs disjoint data and prospective multiplicity/stopping
rules; the next family confirmation allocation is index2, .0125.

Production remains full-window PVS, unpruned qsearch, protected quiet LMR, WINDOW
and EFFORT. The explicit NMP controls retain accepted cumulative composition;
reuse OFF remains the older pre-SR-005 control. Other evaluators, HCE startup,
multiworker and historical research/reference paths do not acquire Pair-2 NMP.
Frozen accepted source `450988455af31` and research source `ac7891e14ff7` remain
reproducible. The report records full source/JAR/network identities and commands.

Synthetic probes preserve the real clock, expire EP and isolate history without
creating a legal pass occurrence. They exclude ordinary TT participation,
recursive nulls, LMR/LMP, prior-PV hints and history writes. Accepted cutoffs return
only the ordinary threshold, with no invented PV/mate/proof; reduced verification
cannot discharge nominal selective provenance. Nominal-reference audits and
full-window replays preserve two reduction/horizon errors despite unpruned qsearch.
These are conditional witnesses, not calibrated population error rates.

Validation includes 747 broad Search/rules/UCI passes (11 existing gated skips),
focused semantic/gating tests, 17 adapter and eight assessor tests, 546 real-model
observations, 428 completed conditional oracle checks, clean whole-request costs,
64 default/control comparisons, 156 frozen-binary parity/cost comparisons and
291 historical/HCE/multiworker control comparisons. Final main sources and frozen
artifacts match their recorded identities; the production factory is unchanged.
Long-clock, other-weight/platform and multiworker strength, universal tactical
safety and globally optimal policy remain unproved. GUI/full training/release
packaging was not relevant and was not run. Build 35 remains unchanged with the
disclosed non-blocking initialization warning; no version repair is required.

Qsearch does not eliminate reduction/horizon error; EFFORT observation requires
neutral ordering weights but truthful work; neural margins are not HCE/WDL
calibration; probe/verification work is not nominal proof; node savings are not
strength. Record these implications for related subjects without activating them.
No feature is ACTIVE, and no successor is selected. Owner/GPT SR-018 reconciliation
and BOTH updated-mirror replacement/confirmation remain external handback gates.

## Experimental efficiency and resource allocation

Apply this policy prospectively to all remaining Phase 2 feature research,
including separately authorized resumptions. Maximize reliable, useful strength
decisions per unit of computation and wall-clock time without weakening feature
investigation, correctness, integrity or independent confirmation. Completed
protocols, frozen experiments and their original conclusions remain unchanged;
these requirements do not retrospectively regrade prior evidence or dispositions.

### Tournament concurrency

For lengthy tournaments and playing-strength comparisons, prefer concurrent
independent games when they improve completed, valid, comparable games per unit
of wall-clock time without compromising experimental integrity. Tournament
concurrency is distinct from engine-level multithreaded Search: measurements of
the established single-worker BRE-Pair 2 composition must retain exactly one
Search worker per engine instance and the evaluated Search policies. Multiple
independent games may run simultaneously, each with appropriate isolated mutable
engine/process state and its frozen experimental configuration.

Choose concurrency from measured host/workload throughput and comparison
integrity, not logical-core count or maximum launchable process count alone;
maximizing concurrency despite contention is not the objective. Proportionate
calibration considers physical/logical cores, CPU contention, memory pressure,
JVM warm-up/GC, whole-match elapsed throughput, actual Search work, timing
variability, time losses, game termination, stability and possible
candidate-dependent resource effects. Concurrent clock-based games need not
produce identical outcomes to sequential execution: preserve independent game
semantics and comparability,
measure relevant contention and report its limitations. A/A validation checks
infrastructure but alone cannot establish that concurrency leaves
candidate-versus-baseline strength unaffected; investigate plausible asymmetric
resource effects with relevant comparisons or diagnostics and retain the
limitations.

Four simultaneous independent games were previously measured as suitable on the
Windows research host. Reuse that calibration when the host, workload, time
controls and resource conditions remain sufficiently comparable. Four is not a
universal fixed requirement: four, six, eight or another defensible count may be
appropriate for actual hardware, workload and observed valid comparison throughput.
Do not benchmark concurrency anew for every feature when suitable measured
evidence already exists. Reassess after material hardware, evaluator, workload,
JVM, time-control or resource-condition changes. Avoid calibration campaigns whose
expected benefit is negligible; record why the retained setting and available
evidence are suitable. Preserve comparable time controls, evaluator identity,
Search configuration, opening/color pairing, seeds and other frozen parameters
under [comparability and evidence](#comparability-and-evidence). Record actual
game concurrency, per-engine Search workers, relevant resource conditions and
material limitations with the experimental evidence.

### Long-running experiment monitoring and reporting

For long-running tournaments, benchmarks and other non-interactive experiments,
prefer reliable blocking, completion-driven or event-driven waits when supported
by the execution environment. Avoid repeated short-interval status polling whose
only expected result is another routine progress count; in particular, do not
poll every one or two minutes merely to report incremental game completions.
When periodic polling is genuinely necessary, prefer approximately 15–30-minute
intervals for lengthy stable jobs where operationally appropriate. This is
guidance, not a mandatory delay overriding reliable failure detection or timely
intervention. If tool limitations make polling unavoidable, use a reasonable
interval compatible with actual monitoring capabilities and record material
limitations where necessary.

Monitoring must preserve stall and process-failure detection, cancellation safety,
resource integrity, durable experimental identity and reproducibility. Choose
wait/check mechanisms that retain these safeguards. Do not interrupt a correctly
running experiment merely to reduce polling or status messages.

Codex-controlled user-facing progress reports should be event-driven and
information-bearing: a meaningful experiment or validation stage completes; a
process fails or stalls; an unexpected integrity or resource problem appears; a
genuine blocker requires external intervention; a material finding changes the
research decision; or the experiment finishes and its results can be analyzed.
Suppress repetitive count-only messages such as "54 of 128 games completed, no
errors" followed shortly by "58 of 128 games completed, no errors." A stable
experiment may run to completion without intermediate user-facing commentary
when reliable monitoring and the execution environment permit it, subject to
applicable host/tool requirements. Never suppress meaningful warnings, exceptions,
required owner decisions or evidence of process failure. This policy governs
Codex-controlled polling and commentary; unavoidable host-generated process
output is outside that control and cannot be suppressed by repository policy.

### Progressive candidate evaluation

Start with inexpensive exploratory screens and targeted diagnostics to separate
weak from promising formulations. Increase investment as evidence quality,
plausible benefit, computational cost and the expected value of further testing
justify increasingly informative experiments. Sufficient negative evidence and
diagnostics may justify abandoning a strongly unpromising formulation early.
That decision does not close the feature-level CGLHW while credible, materially
distinct alternatives warrant useful investigation. Progress suitable frozen
finalists to controlled, fresh independent confirmation before strength-supported
adoption; screening or tuning success alone is insufficient.

### Prospective statistical design and stopping

Prefer suitable prospective sequential or staged designs when they save resources
without compromising inference. SPRT, paired/pentanomial modeling, fixed-sample
confirmation and other defensible methods are options, not universal mandates.
Before substantial confirmatory testing and before observing its results, specify
the comparison and hypotheses, sampling/pairing model, stopping rules (including
success, negative/futility and inconclusive outcomes as applicable), uncertainty
treatment and relevant acceptance criteria. Account appropriately for repeated
looks, candidate selection, multiplicity across candidates/tests and optional
stopping; state model assumptions, approximations and limitations.

Keep exploratory/tuning results out of the independent confirmation sample.
Do not retune a frozen finalist, change its inferential rules after seeing
outcomes, or extend a completed frozen confirmation post hoc to seek significance.
A justified later confirmation requires fresh independent evidence and a
prospective design with appropriate error accounting, within the feature's
current authority or separately authorized resumption. Preserve every completed
protocol and its original conclusion.

### Marginal improvements and proportionate budgets

There is no universal minimum Elo improvement, mandatory fixed game count or
arbitrary maximum number of pairs. Prior designs such as 1,024 or 1,280 pairs
are examples, not programme-wide targets or limits. Before expensive additional
testing, estimate likely information value and computational cost: use available
variance/precision or power evidence and applicable throughput to assess the
additional pairs and elapsed/resource time, assumptions, and whether reduced
uncertainty could change the research decision enough to justify that cost.

Do not automatically spend thousands more games resolving an apparent tiny gain
when expected decision value is low. Equally, do not terminate potentially
important research merely because measurement is difficult or expensive.
Allocate effort proportionately to the feature, plausible strength impact,
credible alternatives, available resources and evidence quality. Revisit that
allocation as evidence develops without altering a frozen confirmation's rules;
a resource-driven interruption must be reported as such, not as statistical
success or inferiority.

### Adoption and disposition

A small but credible positive Elo gain may justify implementation under the
existing acceptance requirements. A positive point estimate or an interval
crossing zero does not, by itself, establish improvement. Efficiency cannot
relax correctness, integrity, independent confirmation or integration requirements.
Use the Frontier's existing
[status meanings](CHESS_SEARCH_RESEARCH_FRONTIER.md#phase-2-status-meanings):
IMPLEMENTED and ACCEPTED retain their distinct technical meanings and do not
establish owner acceptance. A meaningful unresolved small-gain hypothesis may
justify DEFERRED when further testing has insufficient expected decision value;
record the uncertainty and conditions for resumption. REJECTED still requires
proportionate, substantial investigation finding no credible beneficial
formulation under the stated conditions. Exhausting a chosen testing budget
alone establishes neither inferiority nor REJECTED.

### Comparability and evidence

Freeze evaluator/checkpoint and executable identities, relevant Search policies,
openings and color pairing, resource limits, time controls and measurement
conditions as appropriate to the comparison. Do not silently mix materially
different concurrency or resource configurations within a comparison; separate
such evidence or account for planned differences explicitly in the design.
Clock-based playing-strength measurement remains the primary strength evidence.
Fixed-depth and fixed-node tests are complementary correctness, mechanism and
cost diagnostics, not automatic substitutes for clock-based confirmation.

Retain reproducible evidence in the existing research locations: completed
games/pairs and outcomes, failures/exclusions, elapsed time, resource conditions
(including tournament concurrency and per-engine workers), actual Search work
and costs, statistical uncertainty, stopping rationale and limitations. Distinguish
exploration from confirmation, negative evidence from unresolved uncertainty,
and resource-driven deferral from a supported strength conclusion.

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
foundation. SR-001 and SR-029 were separately launched after confirmed predecessor
acceptance and mirror synchronization. Technical closeout does not waive
owner/GPT reconciliation or synchronization before a separately authorized feature.

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

SR-036 Phase 2 concurrency extension is the sole ACTIVE feature, selected by the
owner on 2026-10-14 local. The inherited SR-036 same-depth cooperative implementation
remains BASELINE. Owner/GPT acceptance of SR-007 DEFERRED and synchronization of
BOTH P2-R022/P2-F026 Project Source mirrors are confirmed complete by this launch.
The [SR-036 report](../docs/research/search/SR036_CONCURRENCY_PHASE2_2026-10-14.md)
records clean HEAD 986837d, verified frozen production/evaluator/archive identities,
actual composition, historical evidence, initial challenge and next research action.
Only single-worker Pair-2 currently has qsearch/quiet LMR/WINDOW/EFFORT; inherited
multiworker Pair-2 remains static. Production is unchanged at activation. The
extension may investigate composition, cooperation, scheduling and resource
tradeoffs while preserving proof/owner/lifecycle requirements. Substantial TT
storage/locking changes remain owner-gated. No successor is selected.

### Owner-accepted SR-007 handoff (historical evidence)


SR-007 has reached **DEFERRED technical closeout**, without production adoption.
The owner-confirmed SR-018 acceptance and BOTH P2-R015/P2-F019 and P2-R016/P2-F020
mirror synchronizations remain satisfied history; the SR-036 launch now also
confirms the SR-007 handback and P2-R022/P2-F026 synchronization.

Clean feature-start HEAD was `1a63a7177f99ce0ce5b62367f216c77cd6c7e622`; accepted
EFFORT production, fixed generation32 and Phase1 archive identities remain intact.
The [SR-007 report](../docs/research/search/SR007_INTERNAL_ITERATIVE_PHASE2_2026-10-13.md)
and linked frozen evidence preserve all seven formulations, 416 development games,
256 independent-confirmation games, eight A/A infrastructure games, correctness,
whole-request costs and explicit remaining uncertainty. All clock games ended normally.

IID_WIDE's 128-pair independent confirmation remains CLOSED for original-rule
computational futility: 86/59/111, -34.04 Elo, adjusted interval [-83.47,+14.05].
It did not establish inferiority and is never extended, restarted or pooled.
After explicit owner RESUME and saved-evidence verification, fresh follow-ups
returned IIR_ALL 46/31/51 (-13.58 Elo, 64 pairs), IID_SHALLOW 25/16/23 (+10.86,
32 pairs), and IID_NEAR 26/12/26 (0.00, 32 pairs); all exploratory intervals
cross zero. No formulation established an independent positive lower bound.

Meaningful modest-gain and better-targeted missing-guidance hypotheses remain
unresolved. Further similar small screens offer limited precision, while an
illustrative fresh test with 80% power for +10 Elo costs about 32-42 hours at
observed variance/throughput. This supports DEFERRED, not universal rejection,
a gain floor or closure from an arbitrary game quota. The report explains
mechanism findings, alternative coverage, limitations and conditions for a
separately authorized future resumption with fresh data and family index2.
All explicit research controls remain OFF by default; accepted EFFORT is unchanged.

Current activation masters are **P2-R023/P2-F027**. SR-036 research is ACTIVE;
accepted production and build35 are unchanged. Prior SR-007 evidence remains
closed and is not pooled with this feature. The current research report supplies
continuation state and validation limitations.

`PROJECT_SOURCE_REFRESH_REQUIRED: YES` at the next external handback: the owner
must replace BOTH active mirrors and confirm synchronization before dependent
programme planning. No mid-feature refresh blocks Codex's authorized repository
work. Technical state is not owner acceptance, release or deployment.

| Revision | Change |
| --- | --- |
| P2-R001 | Initialize the owner-authorized strength programme from byte-preserved R045/F052; distinguish retained technical foundations from reopened historical dispositions; establish BRE-Pair 2 baseline dependency, strength acceptance, feature-level CGLHW and confirmed mirror handback. |
| P2-R002 | Record the verified generation-32 BRE-Pair 2 production measurement baseline, explicit hash-checked UCI path, unchanged HCE-only pruning boundary, paired comparison evidence/limitations and new synchronization handback; no Search feature started. |
| P2-R003 | Record strength-supported unpruned qsearch for single-worker Pair-2 production, preserved static/reference/other-evaluator/multiworker paths and proof boundaries, completed confirmation/integration evidence, limits and owner synchronization handback. |
| P2-R004 | Reconcile accepted SR-001 predecessor and record independently confirmed quiet LMR for single-worker Pair-2, conservative proof/accounting semantics, preserved controls and evaluator/worker scope, production integration evidence, statistical/resource limits and SR-029 owner/GPT mirror handback; activate no successor. |
| P2-R005 | Confirm accepted SR-029 predecessor; record scoped SR-006 non-adoption after six formulations and inconclusive independent 2,048-game confirmation, retained full-window cumulative production, retry/proof/cost findings, validation and statistical limits, and owner/GPT mirror handback; activate no successor. |
| P2-R006 | Confirm accepted SR-006 predecessor; record strength-supported WINDOW LMP for single-worker Pair-2, conservative omission/proof/history semantics, complete independent confirmation and uncertainty, production/control integration, whole-request economics and preserved boundaries; hand back SR-022 for owner/GPT acceptance and both-mirror synchronization, with no successor. |
| P2-R007 | Confirm owner/GPT SR-022 acceptance and both predecessor mirror synchronization; record only SR-023 activation, verified cumulative comparator and history/proof boundaries without changing production. |
| P2-R008 | Record SR-023 DEFERRED after six formulations, 448 development and inconclusive 2,048-game independent confirmation; retain WINDOW production, explicit research controls, conditional-history/proof findings, complete validation and precision limits, and owner/GPT both-mirror handback without successor activation. |
| P2-R009 | Confirm accepted SR-023 DEFERRED and both predecessor mirrors; activate only SR-030 against verified cumulative production, preserving extension/proof/real-ply boundaries and linking prospective research. |
| P2-R010 | Close SR-030 as DEFERRED after six formulations, 704 development and two independent 2,048-game confirmations; retain unchanged production, explicit controls, check/qsearch/credit/proof/resource findings, validation, unresolved modest BOUNDARY benefit and fresh-evidence conditions; hand back both mirrors and acceptance without successor activation. |
| P2-R011 | Establish prospective programme-wide experimental efficiency: reusable measured tournament concurrency, progressive candidate investment, defensible statistical stopping, decision-value budgets, preserved adoption/disposition standards and comparable clock-strength evidence; clarify the baseline fixed-sample design's scope and current mirror targets without changing frozen conclusions, production or pending owner gates. |
| P2-R012 | Confirm owner-accepted SR-030 DEFERRED and synchronized P2-R011/P2-F015 predecessor mirrors; activate only SR-005, link verified comparator and challenged iteration-information hypotheses, preserve production/proof/lifecycle scope. |
| P2-R013 | Close SR-005 as IMPLEMENTED after six formulations and prospectively positive 256-game EFFORT confirmation; record completed-iteration ordering/proof/lifecycle boundaries, costs and uncertainty, preserved OFF/research paths, production identity and full integration validation; hand back owner/GPT acceptance and BOTH mirrors without successor activation. |
| P2-R014 | Confirm owner-accepted SR-005 and synchronized predecessor mirrors; activate only the scoped Pair-2 SR-018 extension against verified EFFORT cumulative production, preserving HCE BASELINE and binding proof/history boundaries. |
| P2-R015 | Close Pair-2 SR-018 as DEFERRED after five pruning formulations, 256 development games and closed 256-game SHALLOW confirmation futility; retain EFFORT production and HCE BASELINE, research controls, synthetic proof/history/cost findings, complete validation and unresolved modest benefit; hand back owner/GPT reconciliation and BOTH mirrors without successor activation. |
| P2-R016 | Strengthen measured independent-game concurrency and add low-frequency, event-driven experiment monitoring; reconcile owner-accepted SR-018 DEFERRED and synchronized P2-R015/P2-F019 mirrors, preserve production/evidence/dispositions, and require new BOTH-mirror refresh plus starter revision correction without feature activation. |
| P2-R017 | Confirm owner-accepted SR-018 and synchronized starting mirrors; verify cumulative EFFORT/model/archive identities and activate only SR-007 IID/IIR with challenged mechanisms, binding proof/lifecycle boundaries and durable research continuation. |
| P2-R018 | Reconcile the Frontier activation-header correction and validated SR-007 research state; retain binding proof/lifecycle requirements, accepted EFFORT production and independent clock-confirmation requirement. |
| P2-R019 | Record completed five-policy SR-007 development, refreshed all-policy integrity and selection of unchanged IID_WIDE for fresh prospective confirmation with explicit information-value, stopping and identity controls; retain ACTIVE status and unchanged EFFORT production. |
| P2-R020 | Record closed 128-pair IID_WIDE confirmation futility, complete integrity/results/process shutdown and owner-requested temporary pause; preserve SR007 ACTIVE/incomplete, unchanged EFFORT, frozen protocol, explicit RESUME gate and remaining research without successor activation. |
| P2-R021 | Record explicit owner RESUME, clean provenance and verified frozen/pause evidence; keep the completed IID_WIDE confirmation closed while continuing scoped IIR_ALL and distinct IID coverage/probe-quality research over unchanged EFFORT. |
| P2-R022 | Close SR-007 as DEFERRED after seven formulations, 416 development games and closed 256-game IID_WIDE confirmation futility; retain EFFORT production, explicit controls, verified evidence, quantified modest-gain uncertainty and fresh resumption conditions; hand back owner/GPT reconciliation and BOTH mirrors without successor activation. |
| P2-R023 | Confirm owner-accepted SR-007 DEFERRED and BOTH P2-R022/P2-F026 mirror synchronization; activate only the SR-036 Phase2 concurrency extension over the retained SR-036 BASELINE, recording verified comparators, composition confounding, lifecycle/proof boundaries and scoped research continuation. |
