# SeedV6 Search Contract — Phase 2

Internal revision: **P2-R011**

Status: **Active Phase 2 canon; accepted SR-001/SR-029/SR-022 production retained. SR-023 owner-accepted DEFERRED. SR-030 technical closeout is DEFERRED with no production adoption; GPT/owner reconciliation and both final mirrors remain pending. No feature is ACTIVE.**

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
SR-029 added confirmed quiet LMR, and SR-022 adds protected shallow quiet omission
as documented below. Current cumulative Search policies are:

- Recursive nominal-depth TT-enabled PVS; full-window successive-depth driver;
  unpruned qsearch leaves plus protected quiet LMR and WINDOW LMP for single-worker Pair-2, static leaves for other
  evaluators and multiworker Search; legal hash first, SEE-good then SEE-bad tacticals by immediate
  material, then main quiet history; staged/lazy generation and exact legal
  existence at non-check static leaves (SR-003/014/015/016/017/037).
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

## Experimental efficiency and resource allocation

Apply this policy prospectively to all remaining Phase 2 feature research,
including separately authorized resumptions. Maximize reliable, useful strength
decisions per unit of computation and wall-clock time without weakening feature
investigation, correctness, integrity or independent confirmation. Completed
protocols, frozen experiments and their original conclusions remain unchanged;
these requirements do not retrospectively regrade prior evidence or dispositions.

### Tournament concurrency

Prefer concurrent independent games when they improve completed, valid,
comparable games per unit of wall-clock time. Tournament concurrency is distinct
from engine-level parallel Search: current single-worker BRE-Pair 2 comparisons
must retain one Search worker per engine and the evaluated Search policies.
Choose concurrency from measured host/workload throughput and comparison
integrity, not logical-core count alone. Proportionate calibration considers
physical/logical cores, CPU contention, memory pressure, JVM warm-up/GC,
whole-match elapsed throughput, actual Search work, time losses, stability and
possible candidate-dependent effects. A/A validation checks infrastructure but
alone cannot establish that concurrency leaves candidate-versus-baseline strength
unaffected; investigate plausible asymmetric resource effects with relevant
comparisons or diagnostics and retain the limitations.

Reuse an established calibration when its evidence applies to the current host
and workload; do not benchmark concurrency anew for every feature. Reassess after
material hardware, evaluator, workload, time-control, JVM or other measurement
changes. Avoid calibration campaigns whose expected benefit is negligible;
record why the retained setting and available evidence are suitable.

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

All predecessor acceptance and synchronization gates, including SR-023 DEFERRED
and BOTH P2-R008/P2-F012 mirrors, were owner-confirmed by the SR-030 launch.
SR-030 now has a **DEFERRED technical closeout** after six formulations, 704
development games and two finished inconclusive independent confirmations.
Accepted full-window single-worker Pair-2 remains SR-001+SR-029+SR-022; no
production check extension is adopted and no feature is ACTIVE. The
[SR-030 report](../docs/research/search/SR030_CHECK_EXTENSIONS_PHASE2_2026-10-12.md)
retains scope, identities, evidence, precision limits and future resumption
conditions. GPT/owner reconciliation remains pending. BOTH current P2-R011/P2-F015
mirror replacements with explicit synchronization remain required before dependent
planning; these targets supersede the unfulfilled P2-R010/P2-F014 refresh request.
This efficiency-policy amendment completes no acceptance or synchronization gate
and selects or activates no feature.
Build 35 is unchanged; no pending version operation or cleanup is required.

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
