# SeedV6 Search Research Frontier — Phase 2

Internal frontier revision: **P2-F047**

Status: **SR-033 Adaptive/Hindsight Depth Feedback is ACTIVE under the owner-authorized feature workstream. Tournament maintenance is GPT/owner accepted and BOTH predecessor P2-R038/P2-F042 Project Source mirrors are confirmed synchronized. SR-021 remains owner-accepted DEFERRED without adoption. Accepted cumulative production is unchanged.**

Repository master: `source/CHESS_SEARCH_RESEARCH_FRONTIER.md`.

Current authoritative development repository: `C:/projects/seed/java/seedv6-unified`.
Old repository paths in frozen experimental records describe historical provenance.

## Authority and historical coverage

The [active Contract](CHESS_SEARCH_CONTRACT.md) governs purpose,
technical requirements, architecture and acceptance. This Frontier governs
admitted subjects, dispositions, dependencies and feature workflow within that
Contract; it cannot override the Contract or supply implementation authority
by listing an item. Repository masters are authoritative. Project Sources are
manual mirrors subject to the Contract's confirmed-synchronization gate.

The complete [Phase 1 Frontier, F052](../docs/search-phase1/CHESS_SEARCH_RESEARCH_FRONTIER.md)
and [Phase 1 Contract, R045](../docs/search-phase1/CHESS_SEARCH_CONTRACT.md) are
frozen historical archives. Their old “active” labels, workflow, master locations
and reopening restrictions do not govern Phase 2. Consult each historical SR row
and the corresponding R045 sections/reports for full scope, findings, witnesses,
limitations and evidence references; do not rewrite or selectively prune them.
The Contract identifies their preserved byte hashes and continuing technical scope.

All **38** F052 subjects are accounted for here: **20 REJECTED** subjects become
Phase 2 **PENDING** reconsideration candidates; **14 IMPLEMENTED** subjects remain
the inherited baseline; **2 ACCEPTED** evidence conclusions are retained without
inventing implementation; **2 PENDING** subjects remain unresolved. No F052 row
was ACTIVE or DEFERRED. Embedded deferrals, non-adoptions and OPEN questions are
accounted for separately below. No historical rejection is a Phase 2 conclusion.

Stable SR identifiers and historical subject scope are preserved. Numerical order
is not priority. Do not renumber or delete history. New subjects or material
scope changes require an explicit GPT/owner programme decision; newly discovered
dependencies may be recorded within the selected feature without starting them.

## Selection and expected dependencies

The programme is adoption-seeking and primarily strength-driven under the
Contract's correctness, efficiency and statistical standards. There is no minimum
Elo gain or universal game count. Cost increases can be justified by credible
compensating strength; speed, depth or node savings alone do not establish it.

The authorized **BRE-Pair 2 measurement baseline** prerequisite is technically
complete with documented limitations (below). Its verified identity, production
configuration, controls and reproducible comparison design support subsequent
single-worker research after handback reconciliation. Relative evaluator strength
and empirical centipawn calibration remain unresolved. Do not transfer HCE
NMP/reverse-futility settings to BRE-Pair 2 without independent evidence.

**SR-001 qsearch is now IMPLEMENTED for single-worker Pair-2**, with the scoped
evidence and handback below. **SR-029 quiet LMR is now IMPLEMENTED for the same
scope**, informed by the accepted qsearch/leaf horizon, ordering, evaluator and
proof model. The accepted scoped SR-006 closeout retained that cumulative baseline.
The separately authorized SR-022 investigation now adds technically IMPLEMENTED
protected WINDOW LMP for the same scope, now owner/GPT accepted. The owner has
accepted SR-023 as DEFERRED without production adoption and separately authorized
SR-030 check extensions. The owner accepted SR-030 as DEFERRED, confirmed BOTH P2-R011/P2-F015
mirrors synchronized, and selected SR-005. The SR-018 launch then confirmed
SR-005 IMPLEMENTED acceptance and BOTH P2-R013/P2-F017 mirrors synchronized.
SR-018's Pair-2 extension is now owner-accepted DEFERRED, with BOTH P2-R015/P2-F019
mirrors confirmed synchronized; EFFORT production and historical HCE SR-018
BASELINE remain unchanged. The owner subsequently selected only SR-007 and confirmed
BOTH P2-R016/P2-F020 mirrors synchronized. Its completed IID/IIR investigation and
satisfied historical gates are reconciled in the
[current maintenance state](#maintenance-and-initialization-state).
This expectation is not an immutable queue or a claim that qsearch must be
adopted before LMR can be researched.

After each feature closeout, GPT reconciles the result and owner actions, reads
the current Contract/Frontier and available evidence, reconsiders dependencies,
and chooses the next logical eligible feature. Consider enabling value, expected
strength, measurement quality, implementation maturity, cost and architectural
cohesion. In particular:

- Changed leaf horizons/evaluator economics can materially affect SR-020/021/024,
  probe pruning SR-025/026, extensions SR-030/031/032 and aspiration SR-006.
- SR-029 relates to ordering/history and SR-007/022/023; reduction-conditioned
  SR-033 needs a legitimate reduction producer. These relationships do not
  establish a mandatory order or authorize neighboring features.
- SR-004/005/006/007/027 interact with TT retention, PVS, iteration information
  and total request economics; exact proof obligations remain independent.
- SR-009 correction depends on useful consumers, unbiased-enough labels and
  evaluator-specific cost; SR-018/019 are existing HCE raw-eval consumers, not
  evidence that correction or neural calibration is already solved.
- SR-035 depends on lifecycle/time management and drained SR-036 ownership;
  SR-038 normally benefits from a mature conventional baseline, unless current
  evidence justifies earlier selection.

Codex does not autonomously choose the next feature. No research, evaluator
calibration, infrastructure construction or engine implementation is launched by
this document.

### BRE-Pair 2 measurement prerequisite — 2026-10-09

**READY WITH DOCUMENTED LIMITATIONS.** This is a prerequisite, not an SR entry or
SR-001 research/adoption. All 38 subject dispositions below remain unchanged.
The [baseline report](../docs/research/search/BRE_PAIR2_MEASUREMENT_BASELINE_2026-10-09.md)
and [raw evidence](../docs/research/search/bre-pair2-baseline-2026-10-09/) provide
commands, runtime/source hashes, score mapping, policy matrix, statistical design,
opening/seed controls, tests, pilots, failures and continuation information.

Selected identity is the managed playable Best, generation 32 of lineage
`235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
network SHA-256 `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Freeze that generation for comparisons, irrespective of future Best movement.
External model payloads are available locally and remain outside Git. Invalid
loads cannot fall back in the explicit measurement path.

At that prerequisite, production used static-leaf iterative PVS, established TT/history/ordering
and private evaluator state. Pair-2's quarter-residual mapping is fixed; HCE-only
NMP/reverse-futility are correctly off in both production factories. Added UCI
selection, warm-up and whole-request telemetry enable the real production path
to run under Fastchess without changing defaults. Independent exact and qsearch
oracles remain available; no qsearch/LMR experiment was run.

Verification includes real checkpoint load/score/reference/private-state checks,
141 targeted Java tests, six Python tests, repeatable depth-4 and bounded node/
time/stop/terminal requests, two-worker integrity, and three four-game A/A
functional pilots on the same two paired openings. The accounting parser's
omitted normal TIME_ALLOCATION termination was corrected and the preserved
games reanalyzed successfully; original failed evidence remains. These pilots
are not independent strength samples and demonstrate no Elo improvement.

Supported next use: frozen-evaluator single-worker feature comparisons with
independent opening confirmation. The documented paired fixed-sample protocol
is an established design; future designs follow the Contract's
[experimental-efficiency policy](CHESS_SEARCH_CONTRACT.md#experimental-efficiency-and-resource-allocation).
Unmeasured: longer-control/multiworker strength, other operating systems,
absolute score accuracy, and superiority over HCE/NNUE/other checkpoints.
Future multiworker claims require their own measurements; the SR-035 infinite
depth-ceiling concern remains unvalidated, not repaired by ordinary stop tests.
Version begin capture was stopped before reservation under the non-critical
SeedV6 override; status is idle, build 35 unchanged, no bump claimed or repair
required. No prerequisite technical blocker remains for the supported use.

**Predecessor handback satisfied:** the 2026-10-09 SR-001 launch explicitly
confirms GPT/owner acceptance as READY WITH DOCUMENTED LIMITATIONS and owner
replacement/confirmation of both P2-R002/P2-F002 Project Source mirrors.
That gate no longer blocks the authorized SR-001 workstream.

### SR-001 implemented feature — 2026-10-10 local

The actual clean starting commit is `075d342ec44301bf10642eea39936f64fe34c8f1`.
Frozen archive hashes and active starting revisions match. Rebuilt/frozen
production passed hash-checked real-model UCI preflight; initial targeted Search
regressions have 133 passes, 11 existing disabled tests and no failures/errors.
The [SR-001 report](../docs/research/search/SR001_QUIESCENCE_PHASE2_2026-10-09.md)
records bootstrap challenge, historical A-I evidence, candidate hypotheses,
prospective paired-match design, proof/mate boundaries and restart information.

Eight distinct short-screen policies and two longer exploratory comparisons
selected unpruned v2. Check-only lost substantially; recapture/q2 were less
promising and omit known resources. SEE/promotion, q4 and limited-check variants
showed favorable exploratory results but did not establish a ranking advantage;
SEE can omit tactical resources. They are not universally rejected. Numerical
delta/futility lacks Pair-2 calibration, and qTT still requires sound policy/
history/horizon proof plus measured benefit. No arbitrary parameter sweep or
HCE threshold transfer was used to justify adoption.

Independent fixed 40-pair/80-game 30+0.3 confirmation scored **59 wins, 13 draws,
8 losses** against the accepted cumulative static baseline, score .81875, Elo
+261.95. Primary 95% pair-Hoeffding score interval [.6040153,1] (Elo
[+73.35,+infinity]) passed the prospectively fixed lower-bound criterion.
All 80 games were normal; inputs, PGN/pairing, endings and request accounting
passed. The owner-interrupted attempt remains invalid and wholly excluded.
Raw restart evidence, seeds, openings, hashes and host receipts are in the report.

Single-worker Pair-2 production now uses all legal captures/EP/promotions,
stand-pat only out of check, all checked evasions, real-ply mates and honest
cancellation; no extra quiet checks, qcap, qTT or calibrated pruning. Ordinary
q-dependent nominal TT proof stays suppressed. Independent static/SR-001A paths,
other evaluators, multiworker behavior and TTable architecture remain preserved.
Production integration at `b12a02b7b64a` passed 433 broad tests (11 historical disabled),
with earlier 250 real-model oracle comparisons and a clean frozen UCI bridge:
24 deterministic comparisons, actual default/worker switching, budgets, stop,
terminal and invalid-model handling. The six recursive Search/TTable classes
are byte-identical to confirmed v2. A final static-control lifecycle compatibility
fix preserves optional book/tablebase facilities; final frozen source/binary
identities and its retest are in the report. Contract P2-R003 recorded that leaf
baseline; current P2-R004 adds the SR-029 composition described below.

This is IMPLEMENTED technical closeout, not owner acceptance or deployment.
Single-checkpoint/fast-clock/single-worker strength is established within the
recorded uncertainty. Longer clocks, multiworker strength, cross-platform
performance, evaluator superiority and optimality among all qsearch formulations
remain unproved. Greater qsearch work and sometimes lower nominal depth at a fixed
node budget are accepted costs under the measured clock-based gain. New leaf
semantics inform later ordering, pruning, horizon, TT and LMR decisions; no other
feature, including SR-029, is activated.

Version begin was stopped during prolonged full-tree capture before reservation;
observed status is idle with no unfinished operation. Build 35 is unchanged,
with a non-blocking version warning and no repair action. At eventual external
handback both Project Source mirrors require replacement and owner confirmation.
Next admissible programme action is GPT/owner reconciliation and successor
selection after synchronization, not another autonomous SR-001 experiment.

### SR-029 implemented feature - 2026-10-10 local

The launch explicitly confirms owner/GPT acceptance of SR-001 and owner
synchronization of both P2-R003/P2-F004 Project Source mirrors. Those predecessor
gates are satisfied; the preceding SR-001 handback statements are historical.
Only SR-029 is authorized. Actual clean HEAD is `d855141`, accepted source
`d2a6645`; frozen production JAR, all four generation-32 checkpoint hashes and
both Phase 1 archive hashes match their evidence. Use cumulative **production
unpruned qsearch** as the primary control, with static/reference paths preserved.

The [SR-029 research report](../docs/research/search/SR029_LMR_PHASE2_2026-10-10.md)
and [raw evidence](../docs/research/search/sr029-phase2-2026-10-10/) record the
bootstrap challenge, historical promotion/mate witnesses, proof/accounting
boundaries, hypotheses, prospective protocols, adverse findings and closeout.
Fixed-R1, scaled-depth/ordinal and scout-parent-only screens were inconclusive,
not universal rejections. Outcome-blind stratified replay of scaled/scout probes
found 27 false dismissals in 1,675 replays; 15 were among the first three quiet
moves despite later global ordinal. These are diagnostic samples, not population
error rates. The resulting quiet-class guard retains scaled depth headroom
without rewriting ordering/history or the independent reference.

The frozen quiet finalist's 64/25/39 over 128 games was promising; fresh
independent **256-pair/512-game 30+0.3** confirmation returned **231/119/162**,
score .5673828125, **+47.11 Elo** against accepted SR-001. Primary approximate
paired-normal 95% Elo **[+21.06,+73.69]** and required conservative 2.0 x SE
sensitivity **[+20.53,+74.24]** both satisfy the prospectively fixed criterion.
Hoeffding sensitivity **[-12.16,+109.27]** includes zero, a documented limitation
of statistical robustness rather than a changed acceptance method. No development
or finalist games were pooled. All 512 endings, legality/history/pair/color,
accounting and frozen-input audits passed. Four independent games ran together;
each engine retained one Search worker, fixed generation 32 and matched host/JVM.

**IMPLEMENTED:** canonical single-worker Pair-2 now uses protected quiet LMR over
the accepted unpruned qsearch leaves. Contract P2-R004 records the selected
eligibility/schedule and nominal verification/PVS rules. Surviving reduced/qsearch
dependencies conservatively suppress ordinary TT proof, even after a candidate
is verified. No TTable architecture or proof applicability requirement changed.
Main/q admission uses actual domains; probe/retry preparation is shared, aborted
work is counted and final cancellation cannot publish completion. A domain-aware
evaluator hook forwarding gap found during review was repaired and bridged without
changing measured Pair-2 transitions. Exact/static/SR-001 controls, other
evaluators, multiworker policies and ordinary HCE startup remain available.

Confirmed candidate source `74d7ddb84b532e02f883dbed518d49851b8e1062`;
production integration source `8940a5f9bf35e3dbb4012b88fdcce4f7294f108e`.
All 22 recursive Search/TT classes are byte-identical to the confirmed binary.
Post-integration verification passed **462 Java tests**, 11 existing disabled,
zero failures/errors, nine Python tests and **36 deterministic frozen UCI
comparisons**, including preserved unpruned/off and static controls. Default and
drained worker changes, node/time/stop/terminal and invalid-model behavior passed.
Play, self-play, validation and convenience drivers use the shared factory.
The report binds exact binary/checkpoint identities and reproducible commands.

Quiet retains about 62.5% node savings at depth six on the diagnostic roots;
on 20 budget-exhausting roots, mean depth is 7.1 versus 6.0 at 1M nodes. Clean
request timing is similar with recorded JIT/allocation qualifications; no cache
or fully stabilized-throughput claim is inferred. Strength, not these proxy
metrics, supports adoption. Long-clock, other-checkpoint, cross-platform and
multiworker strength and universal tactical safety remain unproved. Normal
intervals are approximate; the more conservative Hoeffding sensitivity disagrees.

Later SR-007/022/023 work must distinguish global and quiet searched ordinals.
Existing reduced subtrees update quiet history at actual depth; an omitted quiet
remains in the searched prefix for a later cutoff malus. These are heuristic
updates, not nominal disproof. SR-033 may investigate reduction-conditioned
learning only if separately selected; no such redesign is implemented here.
Reducing directly to zero would require revisiting prepared-child domain
accounting. Extensions/pruning must preserve the same surviving-dependency and
real-ply/horizon obligations. No neighboring feature is activated.

No required technical validation or manual cleanup remains. Retain frozen
installations and raw evidence for reproducibility. Root version begin capture
was stopped before reservation under the SeedV6 override; status idle, build 35
retained, non-blocking warning, no verified bump or repair. External release
identity requirements must be reconciled if a release is later authorized.

**Human actions required after this prompt:** owner replacement of **both**
`source/CHESS_SEARCH_CONTRACT.md` (**P2-R004**) and
`source/CHESS_SEARCH_RESEARCH_FRONTIER.md` (**P2-F006**) Project Source mirrors,
then explicit synchronization confirmation; GPT/owner closeout reconciliation
and acceptance. These actions block dependent programme planning and a successor
launch, not this completed technical implementation. Codex has not performed
them or claimed human acceptance/deployment. No successor is selected or begun.

### SR-006 rejected feature - 2026-10-11 local

The launch explicitly confirms GPT/owner acceptance of SR-029 and replacement
and synchronization of both P2-R004/P2-F006 Project Source mirrors. Earlier
pending-handback statements above are historical; those gates are satisfied.
Only SR-006 is authorized. Clean starting HEAD is
`a65a6ed42fe4a8cb7e08b12c0e714e0476eb78e6`; accepted production source
`8940a5f9bf35e3dbb4012b88fdcce4f7294f108e` and its frozen JAR match.
Both Phase 1 archives and all four generation-32 checkpoint hashes match.
Actual UCI preflight passes with cumulative unpruned qsearch plus quiet LMR,
single-worker Pair-2, ordinary HCE startup and separate controls preserved.

The [SR-006 report and raw evidence](../docs/research/search/SR006_ASPIRATION_PHASE2_2026-10-10.md)
record six formulations and 640 development games: fixed narrow/wide full
fallback, directional/volatility-adaptive widening, one-sided lower windows and
parity centering. WIDEN16's promising short-screen result did not replicate in
its longer comparison. ALPHA16 relieved concrete fail-high duplication and was
selected from longer development evidence for fresh independent confirmation.

The complete predeclared three-look confirmation scored **809/453/786 W/D/L**
in **2,048 games / 1,024 unique fresh pairs**, estimated **+3.90 Elo**. Adjusted
paired-normal interval **[-12.30,+20.12]**, required 2.5-SE sensitivity
**[-13.02,+20.84]**, and adjusted Hoeffding sensitivity **[-29.77,+37.64]** all
include zero. All frozen-input, game, full-history/legality/pairing and
whole-request audits pass. No development pooling or post-hoc extension occurred.
The final statistical result is **inconclusive**, so no adoption criterion passes.

**REJECTED** is the feature-level technical disposition after the complete
mechanism, correctness, economic and strength review, not an assertion of
statistically proven inferiority. Tree/time savings and deeper nominal searches
did not establish practical strength. All six formulations and important adverse
results are retained. No distinct supported beneficial mechanism remains
identified; arbitrary margin/gating sweeps or another sample merely seeking
significance are not a remaining research obligation. Small effects, untested
formulations and different weights/clocks/platforms remain uncertain. The unused
second positive-tail allocation does not launch another experiment. No elapsed
time, context budget or experiment-count rule determines this disposition.

Production remains **full-window cumulative SR-001+SR-029**. Explicit research
policies/instrumentation and independent controls remain available. Exact proof,
original windows, qsearch/LMR proof suppression, full history, mate verification,
truthful completion and whole-request accounting are preserved. No TT/history,
clock, nominal-depth or broader pruning change is made. HCE startup, other
evaluators, multiworker and supported consumer lifecycles remain unchanged.
The final Search/UCI/rules run passes **670 tests**, 11 pre-existing disabled,
zero failures/errors; all **702 main classes** match the frozen tested v3 bytes.
The report retains independent model-oracle, PV/budget/stop/runtime bridge,
allocation/JIT and approximate-statistical limitations. No visual GUI QA or
unrelated full suite is claimed; no aspiration production integration occurred.

Future newly evidenced retry/quality mechanisms or separately authorized
changes to evaluator, ordering/history or TT economics could motivate a new
selection. These are implications, not activated dependencies or successor work.
All important frozen installations and raw/negative evidence are intentionally
retained; no manual cleanup remains. Build 35 remains under the documented
non-blocking version warning, without a bump or pending repair. No push/deployment.

**Human actions required after this prompt:** **blocking dependent planning and
successor launch**, owner replaces BOTH Project Source mirrors from
`source/CHESS_SEARCH_CONTRACT.md` (**P2-R005**) and this Frontier (**P2-F008**),
then explicitly confirms synchronization. **Blocking acceptance and successor
selection**, GPT/owner reconciles and accepts or redirects this technical
closeout. Codex has not performed these actions or claimed human acceptance.
No successor is selected or begun.

### SR-005 implemented feature - 2026-10-13 local

The launch confirmed owner acceptance of SR-030 DEFERRED and BOTH P2-R011/P2-F015
mirrors synchronized. Historical predecessor gates are satisfied. Clean starting
HEAD `13004b1`, eligible PENDING SR-005, the accepted comparator and all 19 initial
checkpoint/installation/archive hashes were verified before activation.

**IMPLEMENTED:** single-worker Pair-2 adds completed-iteration **EFFORT** ordering
above ordinary move keys and below legal TT precedence. Reuse combines identical
legal PV-prefix hints with stable descending root-alternative inclusive effort.
Only completed adjacent full-window iterations of the same root/full history,
owner, request and fixed policy/evaluator may seed it. No score, TT proof, depth
or completed PV is inferred; failed/incomplete/narrow/skipped/changed-context work
invalidates retention. Quiet history still resets each production depth. PVS,
actual ordinals, qsearch/LMR/WINDOW proof restrictions and real-ply rules remain.

The [report and raw evidence](../docs/research/search/SR005_ITERATION_REUSE_PHASE2_2026-10-13.md)
cover ROOT, PV, SCORE, EFFORT, HISTORY and PVHISTORY; 624 diagnostics, 1,872 clean
cost requests, 192 exploratory games and eight A/A games. Current selective
TT-store suppression leaves 1,292 of 1,441 observed PV candidates without a TT
move, challenging the Phase 1 redundancy premise for this new composition.
EFFORT D6 nodes/time are .765/.773 of OFF; full equal-admission capped requests
have about one percent lower local throughput. Tree savings are not themselves
strength, and changed ordering changes actual selective decisions.

Independent frozen candidate `4ecaf783b57d42e82b07da109ce4d264f74c55df` versus
accepted cumulative `2f08516d0f24c9a08946bff122dcf5f3a0c8667f` passes the first
prospective look: **130/58/68 W/D/L**, **128 fresh pairs / 256 normal games**,
**+85.85 Elo**, adjusted paired-normal **[+36.87,+138.45]**, required 2.5-SE
**[+39.38,+135.56]**. Adjusted Hoeffding **[-17.54,+207.38]** includes zero;
inference remains approximate. Fixed generation 32/quarter mapping, **15+0.15**,
one worker, four independent concurrent games and disjoint full-history openings.
First-confirmation alpha .025 is split over three planned looks within geometric
family spending. Every input/legal/history/pair/resource audit passes. The
prespecified positive rule closes this confirmation; later stages are not played,
and no sample extension, development pooling, retuning or adverse exclusion occurs.

Integrated source **`450988455af31383d17d126c6f51d277451f5a18`**, frozen production
v2, app.jar **`dfc9d9cb3e5d372eb40015fcc5b53093c5841d09ddad2960cc7c54f21bb4abf8`**.
All 706 rebuilt main classes match; measured core/adapter bytes remain unchanged.
**741 tests pass**, 11 pre-existing disabled, zero failures/errors. Real-model
runtime checks pass **64 default/EFFORT**, **64 OFF/accepted-baseline**, and **291
preservation/worker comparisons**. Coverage includes independent references,
proof/rules/mates, lifecycle/failure/budgets, actual managed consumers, book-enabled
controls, HCE, historical research paths and 1->2->1 workers. Invalid model options
still fail closed. No GUI/native Syzygy, expensive training, long-clock or other
platform strength run is claimed; those unchanged/broader domains remain limits.

UCI omitted/default production selects EFFORT only in the intended composition;
explicit `reuseResearch=off` preserves accepted WINDOW. The pre-SR-005 factory/
lifecycle control retains optional book/tablebase facilities. Older research
controls retain their original composition with reuse OFF; other reuse variants
remain explicit research only, not independently accepted or shown inferior.
Static/unpruned/LMR controls, other evaluators and multiworker remain functional.
No production aspiration, history pruning, check extension or TT redesign.

Changed information availability may inform separately authorized SR-004/007;
ordering/selectivity/history interactions may inform SR-014/016/023, and completed
depth/aborted work may inform SR-034. These are implications, not activation or
ranked successor selection. Additional component combinations or constants are
unmeasured; a supported beneficial formulation does not establish optimality.
The positive independent result plus unchanged measured core and completed
integration support feature closure without another costly exploratory contest.

Build 35 remains unchanged after the non-blocking initial-capture warning; final
version status is idle, with no token, unfinished operation, blocker or repair.
Harmless retained test logs need no manual cleanup. No push/deployment occurred.

Human actions required after this prompt:
- **Blocking dependent planning/selection:** GPT/owner reconciles and accepts or
  redirects this SR-005 technical closeout.
- **Blocking Project Source reliance/successor launch:** owner replaces BOTH
  [Contract](CHESS_SEARCH_CONTRACT.md) **P2-R013** and this Frontier **P2-F017**
  mirrors, then confirms synchronization. Codex has not performed these actions.

No feature is ACTIVE and no successor is selected or begun.

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

## Phase 2 status meanings

| State | Meaning |
| --- | --- |
| PENDING | Admitted for selection; no Phase 2 include/exclude conclusion. This includes all 20 historical frontier rejections. |
| ACTIVE | The one selected feature is under an authorized feature-level CGLHW. |
| ACCEPTED | An evidence-supported technical conclusion favors incorporation, but implementation/adoption or required integration validation is incomplete. Record exactly what remains; this is not owner acceptance. |
| IMPLEMENTED | The accepted feature change is implemented and appropriately validated. Record strength evidence and uncertainty separately; this label does not claim human acceptance, deployment or unmeasured Elo. |
| REJECTED | Proportionate, substantial Phase 2 investigation found no credible beneficial formulation under the stated conditions. Preserve negative and positive evidence and limits; do not assert universal inferiority. |
| DEFERRED | A meaningful hypothesis, dependency or evidence deficit remains unresolved. State what permits resumption; insufficient evidence is not a gain or a rejection. |
| BASELINE | A Phase 1 implemented feature retained at initialization, not a duplicate Phase 2 research task or fresh strength validation. |
| EVIDENCE_RETAINED | A Phase 1 conceptual/evidence conclusion retained without claiming a production mechanism or a new Phase 2 acceptance. |

BASELINE/EVIDENCE_RETAINED distinguish inherited state from Phase 2 research
outcomes. When GPT explicitly selects a scoped extension or reconsideration of
one of these subjects, record both the retained implemented/evidence part and
the new scope/status; do not erase the inherited result.

### SR-022 implemented feature - 2026-10-12 local

The owner launch confirms acceptance of SR-006's scoped REJECTED disposition
and replacement/synchronization of both P2-R005/P2-F008 mirrors. Historical
pending SR-006 handback statements above no longer impose actions. Actual clean
HEAD `c8cdc603845f7128b47157093cc8942008678079`, both Phase 1 archive hashes,
frozen production installation and all four generation-32 checkpoint hashes
match. Fresh initial UCI preflight verified full-window cumulative SR-001+SR-029
with aspiration off. Only SR-022 was activated; no neighboring feature was selected.

The [SR-022 report](../docs/research/search/SR022_LMP_PHASE2_2026-10-11.md)
and its [raw evidence](../docs/research/search/sr022-phase2-2026-10-11/)
record six formulations: shallow actual-scout omission, deeper LMR-domain
omission, their combination, sparse-material protection, depth-two-only omission,
and protected originally narrow WINDOW omission. Historical HCE/static failures
were reconsidered with current Pair-2/qsearch/LMR. Replay found sparse king/pawn
resources and early quiet hazards despite late global rank; protection follows
quiet actually searched counts and material/context evidence. No history-based
pruning, threshold sweep or neighboring feature was activated.

The six short screens and longer WINDOW/COMBINED comparisons total **448
development games**, separate from four A/A games. Longer WINDOW scored
61/32/35 (+71.57 Elo), COMBINED 39/34/55 (-43.66 Elo), on the same paired opening
sample. Selection used mechanism, adverse-resource and complete-cost evidence;
these exploratory outcomes do not establish universal ranking or inferiority.

Independent confirmation completed **1,024 fresh pairs / 2,048 games**, all
normal, with **876/485/687 W/D/L**, **+32.15 Elo**. Three-look-adjusted
paired-normal **[+16.09,+48.36]** and mandatory 2.5-SE **[+15.38,+49.08]**
pass the frozen positive criterion. Adjusted Hoeffding **[-1.53,+66.46]** still
includes zero, so the inference retains its prespecified approximate-normal
limitation. The negative first look and smaller independent estimate are
preserved; no causal explanation for stage variation is established. All
legal/full-history/pairing/exclusion, resource and identity audits pass. No
development pooling, tuning, within-stage stopping or post-stop extension
occurred. The user-requested stage-two pause and later RESUME preserved inputs.

**IMPLEMENTED** is the technical disposition after production integration and
validation. Single-worker Pair-2 now uses full-window SR-001+SR-029+SR-022:
WINDOW omits eligible quiets at parent D1/D2 after six/ten searched quiets,
protects <=6-piece positions, and applies only to actual scouts or originally
one-unit windows. Root/checked/tactical/checking/castling/hash/first moves,
synthetic paths, mate windows and clock-100 horizons remain protected. Omission
is selective prediction, never nominal TT proof or an invented PV/malus; compact
searched prefixes and conservative ancestor dependency suppression remain intact.
The [Contract P2-R006](CHESS_SEARCH_CONTRACT.md) records the technical baseline.

Integration source `2f08516d0f24c9a08946bff122dcf5f3a0c8667f` freezes production
v5 with unchanged measured recursive class bytes. All **703** rebuilt main
classes match its JAR. **690 Search/UCI/rules tests pass**, 11 pre-existing
disabled, zero failures/errors; 12 live-adapter Python tests pass. **116** real
UCI deterministic comparisons bridge default production to frozen WINDOW and
explicit LMP-off to accepted SR-006 v3. Actual all-policy-default two-worker
preflight and 1→2→1 switching pass with exact single-worker restoration.
Managed Play, self-play and validation fixtures execute real omissions. Explicit
quiet-LMR/off, SR-001, static/reference, aspiration and other research controls,
HCE/other evaluators and multiworker behavior remain independently available.
Configured book/tablebase facilities remain on ordinary production/control paths.

At corpus D6, WINDOW saves **46.93% nodes**. Complete v3 elapsed ratios
**.5891 / 1.1118 / 1.1474** at D6 / 100k / 1M nodes demonstrate depth savings
but poorer fixed-node throughput. V4's count-first implementation retains exact
trees with modest/no-regression cost improvement. Main/q mixture, JIT/allocation
and ordinary-desktop contention preclude a single-cause throughput claim; the
clock confirmation supplies strength evidence. Long clocks, other weights or
platforms, multiworker strength, universal tactical safety and optimality remain
unproved. No visual GUI QA or unrelated full training suite is claimed.

Future ordering/history changes can shift searched-count calibration; omitted
moves are not negative training/history labels. Selective TT-domain, extension
and reduction-conditioned-learning implications inform SR-004/005/007/023/030/033
without changing their dispositions or launching successors. All useful failed,
negative and positive evidence, frozen inputs/installations and model payloads
are intentionally retained for reproducibility. No manual cleanup is required.
Build 35 remains under the documented non-blocking version warning; no verified
bump, pending repair, push or deployment is claimed.

The SR-023 launch confirms SR-022 owner/GPT acceptance and replacement of BOTH
P2-R006/P2-F010 Project Source mirrors with explicit synchronization. The former
handback actions are satisfied. Deployment remains separate.

### SR-023 deferred feature - 2026-10-12 local

**DEFERRED technical handback, no production adoption.** The
[feature report](../docs/research/search/SR023_HISTORY_PHASE2_2026-10-12.md) and
[evidence](../docs/research/search/sr023-phase2-2026-10-12/) preserve verified
baseline/model identities, six distinct formulations, 6,546 completed full-history
reference replays, four deeper all-alternative witnesses, 1,656 clean cost requests,
448 development games and 2,048 independent confirmation games. Diagnostic
samples are correlated, not calibrated probabilities or independent strength.

PROTECT preserved positive history at WINDOW counts; EXTRA added shallow negative
omissions; REPLACE substituted a negative-history condition after three searched
quiets; DEEP added stronger negative-history D3 omissions; GATE retained original
counts but protected zero/positive history; LEAF limited EXTRA to D1. All retained
the shared protected domain, searched-prefix accounting, omitted-move learning
boundary and selective TT-proof suppression. No capture omission or history
lifecycle/ordering redesign was introduced. Positive history was already mostly
protected by ordering, zero was not a learned failure, and negative-history
resource misses persisted. Fixed-depth savings did not establish strength.

Fresh larger development challenged REPLACE/GATE at 64 pairs each: REPLACE
55/20/53 (+5.43 Elo, wide exploratory interval), GATE 36/27/65 (-80.11 Elo).
REPLACE alone proceeded unchanged to the frozen 128/384/1024 cumulative-pair
confirmation at 15+0.15, four concurrent games, one worker, frozen generation 32.
Final **810/460/778**, score .5078125, **+5.43 Elo**; Bonferroni-adjusted paired-normal
**[-10.56,+21.44]**, required 2.5-SE **[-11.26,+22.15]**, adjusted Hoeffding
**[-28.23,+39.19]**. All games ended normally and input/PGN/pair/resource/protocol
audits pass. The predeclared final decision is **inconclusive**: no positive lower
bound and no supported inferiority conclusion. Development was never pooled.

The unresolved dependency is credible strength evidence for a possible small
REPLACE gain, not an implementation defect or an arbitrary minimum-Elo rule.
This evidence deficit warrants DEFERRED under the status definition, rather than
converting finite sample exhaustion into REJECTED. No post-hoc extension is
permitted. Resumption requires GPT/owner selection and a separately justified,
fresh independent design with adequate precision and full feature-level error
accounting; the unused .025 positive-tail allocation is not automatic authority
for another test. Current evidence remains immutable and no successor is active.

Production remains accepted full-window SR-001+SR-029+WINDOW. Research v2 source
`3c6f5ed5a36c615b2b88361f2e2a2cb830c217b5`, app.jar
`3f9320fbcd526b3903f31b48241c7c8436e4f50f506a5eb275d174f4ea5895e7` is preserved
alongside accepted v5. All 703 compiled main classes match; 714 Search/UCI/rules
tests pass with 11 existing skips. Actual-default bridges each pass 58 comparisons;
a final control audit passes 62 comparisons plus actual two-worker preflights.
The first control-checker receipt failed because ordinary HCE lacks Pair-2 request
telemetry; its corrected checker passes, with the valid engine response retained.
No unrelated slow training/GUI suite, release or deployment is claimed.

Cross-feature implications: SR-014/SR-022 ordering and searched quiet counts absorb
much of the apparent history signal; SR-016 history maturity/reset and SR-029/SR-033
selective learning affect label interpretation. These are evidence, not newly
activated work or mandatory prerequisites. Longer-clock, other-evaluator,
multiworker and redesigned-history strength remain unproved. Known resource
misses and statistical uncertainty remain visible; no universal safety margin.

Human actions required after this prompt:

- **Blocking for dependent GPT planning:** GPT/owner reconcile this DEFERRED
  technical handback and its unresolved strength hypothesis.
- **Blocking for dependent GPT planning:** owner replace BOTH active Project Source
  mirrors from `source/CHESS_SEARCH_CONTRACT.md` P2-R008 and this P2-F012 master,
  then explicitly confirm synchronization. Codex has not performed those actions.

No manual cleanup or version repair is required. Build 35 retains its non-blocking
warning; no verified bump, pending finalizer or manual compensation is claimed.

### SR-030 technical closeout - 2026-10-13 local

**DEFERRED; no production adoption.** The launch confirmed SR-023 owner/GPT
acceptance and BOTH P2-R008/P2-F012 predecessor mirrors; those historical gates
remain satisfied. SR-030 alone began from clean f4927c05d2d5 against accepted
single-worker Pair-2 SR-001+SR-029+WINDOW, source 2f08516d0f24, frozen generation 32.
The [report](../docs/research/search/SR030_CHECK_EXTENSIONS_PHASE2_2026-10-12.md)
and [evidence](../docs/research/search/sr030-phase2-2026-10-12/) preserve the
complete investigation, protocols, clocks, identities, validation and limitations.

Six materially distinct mechanisms were tested: last-ply giving-check BOUNDARY,
above-boundary GIVING, all checked-node EVASION, single-evasion FORCED, checked
main-leaf LEAF and accumulated-check REPEAT. There are **704 exploratory games**
and two separate **2,048-game independent confirmations**, all with required
normal-termination, legal/history/pairing, frozen-input and whole-request audits.

| Finalist | Independent W/D/L | Point Elo | Adjusted paired-normal | Required 2.5-SE | Adjusted Hoeffding |
| --- | --- | --- | --- | --- | --- |
| FORCED | 787/444/817 | -5.09 | [-21.56,+11.36] | [-22.29,+12.08] | [-38.85,+28.57] |
| BOUNDARY | 830/445/773 | +9.67 | [-6.61,+26.00] | [-7.34,+26.73] | [-23.96,+43.49] |

Both prospectively frozen 128/384/1024-pair procedures finished **inconclusive**.
Each uses .025 approximate positive-tail allocation, at most .05 together.
Normal inference is approximate; bounded-pair sensitivity is also retained.
Neither sample may be extended, tuned or pooled with development or the other
candidate. The initial favorable FORCED and LEAF results did not replicate as
adoption evidence. GIVING/EVASION impose substantial cost without a compensating
screen signal; REPEAT supplied no positive screen. Smaller screens cannot prove
those mechanisms universally inferior.

A modest low-cost BOUNDARY benefit remains plausible but unestablished. Checked
qsearch already covers evasions; the last-ply extension changes main/PVS/order/
history behavior without necessarily changing the unpruned leaf domain. Broader
evasions can add quiet continuations but cost substantially more. This is a
specific precision gap after completed ordinary work, not a generic finite-budget
rejection or a reason for arbitrary tuning. The remaining tiny/modest-effect
hypothesis requires a separately justified fresh independent design, sufficient
opening coverage and precision, explicit multiplicity accounting and a new
GPT/owner selection. Changed baseline evidence or a new causal mechanism could
also justify reconsideration. No new architecture dependency is invented.

All six options stay explicit research-only, default OFF. Current production is
unchanged full-window SR-001+SR-029+WINDOW; no check extensions, aspiration or
history pruning are adopted. HCE, other evaluators, multiworker Search and all
prior controls are preserved. Final source 41587d2a8099 matches all **704** main
class bytes in frozen v2. Broad Search/UCI/rules validation passes **726 tests**,
11 opt-in instrumentation skips, zero failures. Focused/reference, TT-credit,
PVS/history/mate/real-ply/cancellation/accounting, real-model integrity and UCI
bridges pass, including all six retained history controls (72 comparisons).
The report records complete costs, JIT/allocation limitations and skipped work.

Credit is value-relevant context; extension-dependent search cannot become
ordinary nominal TT proof. Reuse edge depth/credit on retries and account only
actual admitted work. Qsearch overlap, quiet-reply horizons and the actual/nominal/
real-ply distinction inform SR-001/SR-014/SR-016/SR-018/SR-029/SR-031/SR-032;
none is selected or implemented by this finding. Strength is limited to one
frozen Pair-2 checkpoint, shared Windows/Java21, 15+0.15, one worker and four games
in flight. Longer-clock, other-evaluator/platform and multiworker strength remain
untested; no universal Elo, timing equality or evaluator-superiority claim follows.

Human actions required after this prompt:

- **Blocking dependent GPT planning:** GPT/owner reconciliation of the technical
  DEFERRED disposition and acceptance or specified remediation.
- **Blocking dependent GPT planning:** owner replacement of BOTH Project Source
  mirrors from `source/CHESS_SEARCH_CONTRACT.md` and
  `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` at the current revisions stated below,
  followed by explicit synchronization confirmation. Codex has not updated them.

No manual cleanup or version repair is required. Frozen installations and raw
successful, adverse and inconclusive evidence are intentionally retained. Build
35 is unchanged with the disclosed non-blocking initialization warning; finalizer
status is idle, with no active token/unfinished/blocker. No push or deployment
occurred. Technical closeout is distinct from owner acceptance and mirror sync.
No feature is ACTIVE and no successor is selected.

## Reconsideration candidates from the 20 rejected F052 subjects

Every row was admitted anew as PENDING; recorded Phase 2 dispositions supersede
that initial state. Its full historical scope, findings and
evidence remain at the same SR identifier in F052 and the mapped R045 discussion.
The short dependency notes do not constrain research to the previous formulation.

| ID | Research subject | Phase 2 state | Principal dependencies / investigation context |
| --- | --- | --- | --- |
| SR-001 | Quiescence Search | IMPLEMENTED | Confirmed unpruned single-worker Pair-2 PVS leaf composition; 59/13/8 over 40 held-out pairs, primary 95% Hoeffding Elo lower bound +73.35, integrated and runtime-bridged. Preserve independent static/SR-001A controls, other evaluators, multiworker policies and proof boundaries. Scope, alternatives, costs, limitations and owner handback above. |
| SR-002 | Recursive vs flattened Search mechanics | PENDING | Current JVM/platform and Search costs; preserve independent recursive exact reference and equivalent semantics. |
| SR-004 | MTD(f) and memory-enhanced zero-window drivers | PENDING | First guesses, mature TT/PVS, repeated-pass and PV cost; include related memory-enhanced drivers where useful. |
| SR-005 | Iterative-deepening information reuse | IMPLEMENTED | Confirmed completed-PV/root-effort ordering for single-worker Pair-2: 130/58/68 over 128 fresh pairs, +85.85 Elo, adjusted normal lower +36.87 and required 2.5-SE lower +39.38; bounded sensitivity includes zero. Integrated/default and explicit-OFF bridges pass; preserve heuristic/proof/lifecycle boundaries, measured costs and scoped controls. Owner/GPT acceptance and BOTH P2-R013/P2-F017 mirror synchronization were confirmed by the SR-018 launch; closed SR-005 evidence remains unchanged. |
| SR-006 | Aspiration windows | REJECTED | Scoped six-formulation investigation found no credible strength-supported adoption over cumulative SR-001+SR-029. Independent ALPHA16: 809/453/786 over 1,024 pairs, +3.90 Elo, adjusted interval [-12.30,+20.12]; both required/wider sensitivities include zero. Retain full-window production, research controls, negative/cost evidence, uncertainty and owner/GPT handback above; no universal inferiority claim. |
| SR-007 | Internal iterative search: IID and IIR | DEFERRED | Seven formulations and 416 development games; IID_WIDE fresh 256-game confirmation closed for futility, later IIR_ALL/IID_SHALLOW/IID_NEAR follow-ups inconclusive. No adoption; EFFORT unchanged. Modest gains/targeted eligibility remain unresolved; report quantifies further evidence value and fresh resumption conditions. Owner/GPT acceptance and BOTH P2-R022/P2-F026 mirror synchronization were confirmed by the SR-036 launch; closed evidence remains separate. |
| SR-020 | Razoring | PENDING | Reduced/qsearch horizons, evaluator evidence, nominal resources and SR-018/019 composition. |
| SR-021 | Futility-pruning family | DEFERRED | Technical closeout by explicit owner resource choice; no adoption. SHALLOW1 closed for computational futility; SHALLOW4 owner-interrupted after two inconclusive scheduled looks, with partial final-stage games preserved as raw evidence. Larger DEEP screens also inconclusive; useful four-worker/small-gain hypotheses remain. New session study unlaunched, Mac access deferred, no automatic resume. Renewed authority and acceptable controlled prospective evidence are required for further investigation. GPT/owner reconciliation and BOTH current mirrors remain pending; no successor. |
| SR-022 | Late-move / move-count pruning | IMPLEMENTED | Confirmed protected D1/D2 WINDOW omission over single-worker Pair-2 SR-001+SR-029: 876/485/687 in 1,024 fresh pairs, +32.15 Elo, adjusted normal lower +16.09 and required 2.5-SE lower +15.38; Hoeffding includes zero. Production/default and explicit-off bridges pass. Preserve omission/proof/history protections, adverse alternatives, complete cost limits and scoped controls; owner/GPT acceptance and predecessor both-mirror synchronization confirmed by SR-023 launch. |
| SR-023 | History-based pruning | DEFERRED | Six formulations and 448 development games; unchanged REPLACE independent 2,048-game confirmation 810/460/778, +5.43 Elo with adjusted interval [-10.56,+21.44], wider and Hoeffding intervals also include zero. No production adoption; possible small benefit unresolved. Preserve frozen evidence, WINDOW baseline and history/proof protections; fresh justified independent design and GPT/owner selection required for resumption, with no post-hoc extension. |
| SR-024 | SEE-based move pruning | IMPLEMENTED | QXRAY adopted for Pair-2 EFFORT1/ROOT_REUSE2..16 after independent +24.128/+26.061 Elo confirmations with positive adjusted primary intervals; all-worker integration validated. Approximate inference and resource limits are documented; owner/GPT acceptance and BOTH P2-R028/P2-F032 mirror synchronization are confirmed by the SR-021 launch. See the unchanged SR-024 report and closeout below. |
| SR-025 | ProbCut and Multi-ProbCut | PENDING | Legal reduced probes, calibration, actual-depth proof and complete economics; distinct from SR-026. |
| SR-026 | Multi-Cut pruning | PENDING | Multiple-success/diversity evidence, correlated horizon errors and probe cost; distinct from SR-025. |
| SR-027 | Enhanced Transposition Cutoff and TT look-ahead | PENDING | Applicable child proofs, phase/TT traffic, PVS and evaluator-dependent saved work. |
| SR-028 | Alternative/historical forward-pruning screen | PENDING | Credible distinct historical/novel candidates, overlap with named families; RankCut relates to SR-038. |
| SR-029 | Late-move reductions and re-search | IMPLEMENTED | Confirmed quiet-class-protected scaled LMR over accepted single-worker Pair-2 SR-001. Independent 231/119/162 over 256 pairs, +47.11 Elo, primary approximate 95% lower bound +21.06 and required 2.0-SE lower bound +20.53; Hoeffding sensitivity includes zero. Integrated/bridged with conservative proof suppression, preserved off/static controls and actual-domain accounting. Full alternatives, limits, future implications and owner/GPT mirror handback above. |
| SR-030 | Check extensions | DEFERRED | Six formulations, 704 development games and two independent 2,048-game confirmations: FORCED -5.09 Elo, adjusted [-21.56,+11.36]; BOUNDARY +9.67, [-6.61,+26.00], all prescribed intervals cross zero. No adoption; plausible modest low-cost BOUNDARY benefit remains unresolved. Preserve unchanged WINDOW production and all controls; both confirmations are closed. Fresh justified evidence with suitable precision, opening coverage, error accounting and GPT/owner selection is required for resumption. |
| SR-031 | Singular-extension family | PENDING | TT anchors, exclusion-domain proof, policy identity, extension alternatives and probe cost. |
| SR-032 | Other tactical/forced-line extensions | PENDING | Forced replies, recapture/promotion/threat/history families, caps/fractional depth and complete root decisions. |
| SR-033 | Adaptive/hindsight depth feedback | ACTIVE | Owner-selected; accepted SR-029 supplies reduction producer. Investigate censored reduced/verified outcomes against cumulative QXRAY/EFFORT1 and ROOT_REUSE4; preserve workers 1..16. See SR-033 activation and research record. |

## Unresolved Phase 1 subjects and accepted evidence

| ID | Historical disposition | Phase 2 state | Treatment and remaining work |
| --- | --- | --- | --- |
| SR-008 | ACCEPTED: node/evidence model and path context | EVIDENCE_RETAINED | The orthogonal invocation/proof/provenance/prediction model is binding as mapped by the Contract. Closure required no new runtime mechanism. No generic predictor or carried context is silently commissioned; future consumers must justify state and information. |
| SR-009 | ACCEPTED: static-evaluation evidence and correction | EVIDENCE_RETAINED | Retain raw-eval versus proof and bounded HCE residual findings. Correction-history implementation remains DEFERRED/not adopted: consumers, labeling bias, evaluator cost, learning/update/aging/bounds and table lifecycle need research if selected. Existing SR-018/019 consumers are relevant dependencies, not automatic reopening or implementation. |
| SR-035 | PENDING: pondering and speculative opponent-turn Search | PENDING | Preserve predicted-move reuse, cancellation, protocol, TT/history and lifecycle scope. Carry SR-036's **unvalidated** nonterminal infinite-depth-ceiling publication concern as an inspection lead, not a confirmed bug or completed repair. |
| SR-038 | PENDING: learned or evaluator-assisted Search guidance | PENDING | Preserve neural/policy ordering, uncertainty/learned selectivity and evaluator-assisted evidence; retain core evaluator independence. SR-028's RankCut conditional-support idea is a lead, not adoption authority. |

F052's two ACCEPTED rows are explicitly evidence-model closures, despite its
general ACCEPTED definition suggesting incomplete adoption. They are not lost
implementation promises: SR-008 requires no new code; SR-009 retains a specific
deferred correction mechanism. No other ACCEPTED-but-unimplemented frontier row
was present at transition.

## Implemented baseline retained without duplicate tasks

The following 14 F052 IMPLEMENTED subjects retain their recorded production
functionality. “BASELINE” is not a fresh implementation or strength claim.
R045 supplies detailed semantics under the active Contract's inheritance boundary.

| ID | Retained subject | Phase 2 state |
| --- | --- | --- |
| SR-003 | PVS / zero-window Search with exact re-search rules | BASELINE |
| SR-010 | Repetition, cycles and graph-history interaction | BASELINE |
| SR-011 | Core mate-distance/domain restriction | BASELINE |
| SR-012 | Current advanced TT policy and selected TTable mechanics | BASELINE |
| SR-013 | Optional guarded small-table root WIN outcomes | BASELINE |
| SR-014 | Overall move-order architecture | BASELINE |
| SR-015 | Capture/tactical ordering and SEE | BASELINE |
| SR-016 | Main quiet-history ordering | BASELINE |
| SR-017 | Staged generation and lazy move selection | BASELINE |
| SR-018 | Bounded HCE-only fixed-R NMP composition | BASELINE |
| SR-019 | Bounded HCE-only reverse-futility/static-null policy | BASELINE |
| SR-034 | Bounded clock management | BASELINE |
| SR-036 | Optional same-depth cooperative parallel Search | BASELINE |
| SR-037 | Production hot-path/static-leaf legal-existence optimization | BASELINE |

### Embedded non-adoptions and OPEN dependencies

These remain visible for deliberate feature selection or necessary scoped work.
They do not reopen the 14 implemented subjects wholesale. Historical negative
formulations are eligible for Phase 2 reconsideration under their stable parent
IDs, without Phase 1's dependency-change admission prohibition. A GPT-selected
scope and any binding architecture decision are still required.

| Historical parent / location | Continuing unresolved or reconsiderable scope |
| --- | --- |
| SR-001, R045 current leaf policy | Preserve all rejected qsearch substudies, including SEE, qdepth, qTT, quiet checks/forcing limits, delta and post-static futility, as SR-001 evidence and available alternative formulations. |
| SR-008/009, R045 evidence sections | Mechanism-specific context/predictors, eval movement/improving, correction and learning remain unadopted; numerical HCE relationships require independent neural validation. Do not promote predictions into proof. |
| SR-010/012, R045 § I/J and OPEN frontier | Broader proven history equivalence, hash-move identity, TT domains/replacement/sizing/prefetch, partial-work non-score evidence and future selective/narrow-window interactions require scoped decisions. Mathematical equal-depth/current-generation proof is retained; old non-adoption does not authorize unsafe reuse. |
| SR-011, R045 mate-domain section | The explicitly REJECTED **HORIZON_DOMAIN** runtime candidate is admitted for reconsideration as an SR-011 subfeature. Its settled finite-horizon gap mathematics remains evidence; core MDP stays implemented. No additional top-level rejected SR is invented. |
| SR-013, R045 § K | Broader materials/outcomes/interior probes, repeated-history handling and other native platforms remain outside the validated root policy; resolve evidence/value-domain/ownership needs before adoption. |
| SR-014/015/016 | Bad-tactical demotion, LVA/capture history, killers, countermoves, continuation/relative/richer histories remain reconsiderable. History persistence across iterations/requests and aging/new-game reset remain OPEN; current per-invocation/private-worker reset stays the baseline. |
| SR-017/037, R045 § L | Full/hybrid sorting and generator/setup alternatives remain non-adopted evidence; future primitive mechanics need measured total value and exact baselines. |
| SR-018/019 | Pair-2 SR-018 extension is DEFERRED after five formulations, SHALLOW independent futility and weak expanded ADAPTIVE development; modest benefit remains unresolved, with scope, costs and fresh-resumption conditions above. HCE SR-018 stays BASELINE. Broader reductions/verification/margins and SR-019 neural calibration remain unadopted; no HCE transfer or SR-019 activation is implied. |
| SR-034/035 | Stability/forecast/root-effort stopping, resource redistribution, pondering and further lifecycle representation remain unresolved or non-adopted; preserve hard limits and completed-result semantics. |
| SR-036 | Root splitting, Jamboree, diversification, independent-depth Lazy SMP, voting, DTS and shared-history alternatives retain their historical limits and reconsideration scope; preserve owner/worker integrity. |
| R045 TENTATIVE/OPEN sections; SR-020–033/038 | Future selective lifecycles, reductions/extensions/re-search, technique-specific evidence and allocation of effort remain hypotheses. The active tables cover their subjects; no old tentative sequence becomes a required architecture or a feature queue. |

## Feature-level CGLHW and handback

One GPT conversation and normally one new Codex conversation own each selected
feature. GPT prepares the scope, baseline dependencies, owner decisions and launch.
Codex may autonomously execute the feature's discovery, primary literature and
reference-engine investigation, alternative hypotheses/prototypes, experiments,
failure analysis/remediation, performance and strength measurement, justified
implementation, correctness/integration validation, canon maintenance, temporary
cleanup and appropriate Git closeout. Apply the Contract's experimental-efficiency
policy autonomously; routine experimental decisions require no new prompt or approval.

1. Read both repository masters and relevant frozen Phase 1 scope/evidence.
   Verify actual code/configuration, dependencies and measurement feasibility.
   Record the selected scope and ACTIVE state. Apply the Contract's
   [experimental efficiency and resource allocation policy](CHESS_SEARCH_CONTRACT.md#experimental-efficiency-and-resource-allocation)
   throughout the feature, including BOTH execution requirements:
   [reuse or establish suitable independent-game tournament concurrency](CHESS_SEARCH_CONTRACT.md#tournament-concurrency),
   keeping per-engine Search workers appropriate to the measured composition
   (exactly one for established single-worker BRE-Pair 2), and
   [prefer completion-driven, blocking or event-driven monitoring](CHESS_SEARCH_CONTRACT.md#long-running-experiment-monitoring-and-reporting).
   Avoid repetitive short-interval polling and meaningless progress messages;
   use proportionate periodic checks only when required. Preserve stall/failure
   detection, cancellation safety, experimental integrity and comparability.
2. Select proportionate exploratory tests, controls and statistical designs.
   Investigate credible alternatives thoroughly, including failure mechanisms
   and cumulative-baseline interactions; progress promising formulations through
   increasingly informative experiments. Keep correctness, fixed-budget, whole-request and clock-strength
   evidence distinct.
3. Before expensive additional testing, estimate likely information value and
   computational cost under the Contract. Stop unpromising formulations when
   evidence and diagnostics justify it, while continuing useful investigation
   of credible, materially distinct alternatives within the selected feature.
4. Independently confirm suitable frozen finalists under defensible prospective
   protocols, preserving separation from exploration/tuning and accounting for
   selection, multiplicity and stopping. Record uncertainty and costs; a credible
   small Elo gain may suffice, while an inconclusive gain does not.
5. Reach a supported ACCEPTED, IMPLEMENTED, REJECTED or DEFERRED disposition
   under the Contract, distinguishing credible gains, negative evidence,
   unresolved uncertainty and resource-driven deferral. Implement only justified
   changes within authority and validate integration/compatibility. Record
   incomplete work and acceptance/resumption conditions explicitly; do not use
   DEFERRED or a canon edit to evade a blocker.
6. Update the Contract for material technical/accepted-baseline changes and
   this Frontier for disposition, scope, evidence links, limitations, dependencies,
   remaining work and human actions. Retain completed games/pairs, elapsed time,
   meaningful resource/execution evidence (actual game concurrency, per-engine
   workers, relevant conditions and material monitoring/resource limitations),
   statistical uncertainty, stopping rationale and other limitations with the
   Contract's required evidence in existing research locations.
   Distinguish evidence, accepted conclusions, implementation, strength validation,
   owner acceptance and deployment. Reconcile temporary artifacts and unrelated
   work, run applicable checks and complete attributable Git closeout.
7. Return control to GPT and the owner. Report revisions, validation actually
   run/skipped, risks, Git state and required human actions. If either canon
   changed, require owner replacement of **both** Project Source mirrors from
   the active repository files and confirmation before GPT plans from them.
   GPT reconciles closeout/acceptance and dynamically selects the next feature.
   Codex must not continue automatically to that feature.

Escalate genuine external authority, conflicting requirements, missing information,
unavailable required validation or unavoidable human-action blockers. Explain
their effect on dependent work and preserve the durable continuation state.
Ordinary research difficulty, one failed candidate or one completed internal
experiment is not an escalation condition. No extra orchestration ledger or
governance document is needed.

## Maintenance and initialization state

### SR-033 four-worker confirmation continues to frozen maximum - 2026-10-18 local

SR-033 remains ACTIVE. CAUTION/ROOT_REUSE4's fresh 128-pair first look is
+28.565 Elo, adjusted paired-normal [-21.727,+80.089], with a prospective
CONTINUE decision. The original executor is playing the frozen second stage
for 512 cumulative pairs; no intermediate operational outcomes are inspected.
The completed first-stage audit exactly reproduces the stored assessment.
MOVE/native1 remains CLOSED for computational futility. Production is unchanged.
Use the [research record](../docs/research/search/SR033_ADAPTIVE_DEPTH_PHASE2_2026-10-17.md)
and frozen protocol receipts to continue only SR-033; no successor is selected.

### SR-033 native confirmation closed; four-worker confirmation active - 2026-10-17 local

SR-033 remains ACTIVE with production unchanged. MOVE/native1's fresh 128-pair
confirmation closed at its prospective computational-futility boundary:
-1.357 Elo, adjusted normal [-49.700,+46.932]. The earlier positive development
estimate did not replicate; inferiority/equivalence and absence of small gains
are not established. Preserve the closed protocol; do not play its maximum stage.
CAUTION/ROOT_REUSE4 is executing its separate frozen confirmation. See the
[research record](../docs/research/search/SR033_ADAPTIVE_DEPTH_PHASE2_2026-10-17.md)
and immutable assessments for the current resume point. No successor is selected.

### SR-033 development complete; independent confirmation frozen - 2026-10-17 local

SR-033 remains ACTIVE with production unchanged. Four fixed 32-pair development
contrasts completed and passed full legal, paired, lifecycle and resource audits.
MOVE1 (58.6%) and CAUTION4 (60.2%) are selected only as exploratory finalists;
MOVE4/CAUTION1 were near equality. No gain or cross-worker conclusion is accepted.
The [feature research record](../docs/research/search/SR033_ADAPTIVE_DEPTH_PHASE2_2026-10-17.md)
records uncertainty, GC/CPU/throughput, alternative-mechanism implications and
fresh separate 128/512-pair confirmation protocols with allocated error, futility,
complete-pair operational blocks and bounded execution economics. Preserve all
closed development evidence and use the durable protocol resume point. Neither
SR-021 nor any successor is activated; version provenance remains separate.

### SR-033 validated first feedback mechanisms ? 2026-10-17 local

SR-033 remains ACTIVE; accepted production is unchanged. The
[feature research record](../docs/research/search/SR033_ADAPTIVE_DEPTH_PHASE2_2026-10-17.md)
records reduction-outcome censorship, sparse repeated-success opportunities,
explicit MOVE request-private and CAUTION node-local research controls, preserved
nominal verification/PVS/proof boundaries, independent tactical witnesses and
whole-request economics. Actual standalone production/frozen OFF parity, all
1..16 UCI worker settings and deeper real-model helper execution pass. Initial
coverage/harness failures remain classified and preserved. Four prospectively
fixed exploratory clock contrasts are frozen at one/four workers, concurrency
4/2 and 10+0.1; no strength result or adoption is established. Continue within
SR-033 according to the durable resume point; version finalization stays separate.

### SR-033 activation ? 2026-10-17 local

The owner confirms tournament-maintenance GPT reconciliation, owner acceptance
and BOTH P2-R038/P2-F042 Project Source replacements/synchronizations complete.
Historical pending statements below are superseded; no predecessor gate remains.
Clean HEAD `54de53e979bde0483bc5729ef5d41845aa2b4375` and the starting revisions
are verified. Only SR-033 Adaptive/Hindsight Depth Feedback is selected/ACTIVE;
SR-021 remains DEFERRED and its interrupted SHALLOW4 study is not resumed.

The [SR-033 research record](../docs/research/search/SR033_ADAPTIVE_DEPTH_PHASE2_2026-10-17.md)
records fixed generation-32 model identity, source inspection, available Phase 1
evidence, the censored-outcome challenge and current resume point. Accepted
SR-029 supplies the reduction-conditioned prerequisite. Research compares actual
cumulative QXRAY/EFFORT at one worker and ROOT_REUSE at four, preserving supported
workers 1..16, independent references and all binding proof/lifecycle requirements.
No feedback mechanism is adopted. Existing version DEFERRED_FINALIZATION is
preserved separately and does not block authorized independent research.

### Owner acceptance and tournament maintenance — 2026-10-17 local

The owner accepts SR-021 as **DEFERRED without adoption** after GPT closeout
reconciliation. Its six formulations, negative/inconclusive conclusions and
interrupted SHALLOW4 evidence remain unchanged; automatic continuation is closed.
No feature status or stable ID changes, no successor is selected, and no Mac
work is admitted. Earlier pending-acceptance wording below is historical.

The separately authorized [tournament execution maintenance](../docs/research/search/TOURNAMENT_EXECUTION_2026-10-17.md)
uses the unified repository masters before final mirror refresh. Its measured
valid-pair throughput, forecasts and validated bounded pair-block sessions inform
future feature planning under the Contract's existing efficiency policy. Preserve
the one-worker/four-worker distinction; freezes and execution windows are
prospective, pauses are not statistical looks, and partial/abnormal blocks may
not be automatically replayed. Detailed calibration and limitations live in that
evidence record rather than a duplicate programme methodology.

Maintenance is technically complete: 120 operational calibration games, a real
two-session complete-stage audit/export, and deliberate partial-pair interruption
with owned shutdown and rejected replay/export. The matched four-game pilot is
1.91× faster in match time (1.87× including session overhead) than two games,
but fixed-node controls show policy-dependent contention. Keep four-worker
concurrency 2 as the conservative confirmation domain and one-worker concurrency
4 unchanged. Four-game throughput is a prospectively justified fresh-domain
option, not evidence of strength neutrality or authority to alter frozen studies.
Partial/abnormal blocks remain a manual-investigation boundary, not a qualified
native Fastchess partial-pair resume capability. Future features can reuse the
calibration, forecasts and bounded-session method with those explicit limits.

Maintenance closeout still requires GPT/owner reconciliation. The owner then
replaces BOTH Project Source mirrors from final **P2-R038/P2-F042** and confirms
synchronization before subsequent planning/launch. Intermediate P2-R037/P2-F041
replacement is superseded; SR-024 acceptance and earlier synchronized mirrors
remain satisfied. The owner separately updates external Project Instructions to
the unified root. Codex neither performs nor claims these external actions.

### SR-021 technical closeout: DEFERRED - 2026-10-16 local

The owner resolves the pending resource choice: "Close technically as DEFERRED."
SR-021 is technically DEFERRED without futility adoption; no feature is ACTIVE
and no successor is selected or authorized. The six-session, approximately
15.3-hour new SHALLOW4 proposal remains unlaunched and unfrozen; contrast3 is
unspent. Mac access remains owner-deferred. This is the current state; earlier
ACTIVE/running/pending entries below are historical checkpoints, not instructions
to continue. Their original experimental conclusions are preserved.

Substantial investigation covered LEAF, SHALLOW, PREFIX, DEEP, D2 and CHILD.
SHALLOW has useful shallow work savings but no established strength gain.
Its independent native EFFORT confirmation CLOSED for the original computational
futility rule at 512 pairs: -0.679 Elo, adjusted interval [-25.162,+23.799].
The ROOT_REUSE four-worker confirmation is OWNER-INTERRUPTED, not statistically
completed: its 128/512-pair assessments retain +43.658 [-8.068,+97.400] and
+19.700 [-7.490,+47.134], both originally CONTINUE. No required 2,048-pair final
look occurred. All 165 surviving final-stage normal games (82 complete pairs
and one color) remain raw evidence, outside completed confirmatory inference.
The exact experiment process tree was stopped and shutdown verified. No
automatic resumption, pooling, narrower post-hoc adjustment or inferiority/
declining-trend/positive-criterion claim follows.

The bounded larger DEEP screens remain exploratory: native +9.503
[-28.387,+47.620] and four-worker -5.429 [-57.929,+46.823], both 95% intervals
including zero. LEAF/PREFIX/D2 have weak development support; CHILD has adverse
whole-request cost and move-choice evidence despite its local leaf predicate.
The report preserves tactical witnesses, capped replays, corrected prototype
cancellation handling, failed attempts and all-worker qualification. Research
policies remain explicit and default OFF; accepted QXRAY, quiet LMR, WINDOW,
EFFORT1/ROOT_REUSE2..16, HCE and independent references remain available.

DEFERRED reflects unresolved four-worker/small-gain hypotheses and the owner's
choice about primary-PC opportunity cost, not family rejection or a minimum
Elo threshold. The SR-021 constraint remains roughly 4-6 acceptable hours of
continuous disruptive primary-PC research, not another roughly 12 hours and
not a universal total-compute ceiling. Further investigation needs renewed
authorization and a worthwhile hypothesis with a suitable host or prospectively
scheduled complete-pair sessions, validated infrastructure and a new controlled
design with fresh inputs and correct family spending. Existing closed or
interrupted studies must not be extended automatically. Mac feasibility is
static only; no SSH, configuration, transfer or execution was performed.

The [final report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md)
and closeout-validation-v1.json identify evidence, source/binary/model identities,
validation and remaining uncertainty. This technical disposition is not GPT/owner
acceptance, Project Source synchronization or release/deployment. Human actions
required after this prompt: GPT/owner technical reconciliation, then owner
replacement and explicit confirmation of BOTH current Project Source mirrors
from P2-R037/P2-F041; these block subsequent programme planning/launch from the
mirrors. The already-completed SR-024 predecessor handback is not reopened.

### SR-021 bounded continuation and resource decision - 2026-10-16 local

After the owner-directed interruption, DEEP receives a fixed reduced-concurrency
screen:128 fresh native pairs, W/D/L[104,55,97],+9.503 Elo, exploratory95%
[-28.387,+47.620];64 disjoint four-worker pairs,[53,20,55],-5.429 Elo,
[-57.929,+46.823]. All384 games and identity/history/process audits pass.
Total wall3608.53s; CPU ratios.998071/1.002515. Neither establishes a gain.
These new resource domains are not pooled with earlier development or SHALLOW.
All experiment engines/runners have exited. Production remains unchanged.

Additional DEEP validation covers96 roots,1519 completed omission replays with
zero above-alpha results and one explicitly capped replay, plus actual1..16
worker execution and44 frozen-binary UCI lifecycle requests. Final verification
matches368 preserved v1 source entries and376 v3 source/build entries, preserves
Phase1 archives and all interrupted-study evidence, and confirms build35 with
the existing nonblocking initialization warning. No Mac runtime QA occurred.

The owner identifies a2020 M1 MacBook Air with8GB and BotLi idle, then explicitly
defers Mac access. Static feasibility is conditional; no SSH, installation,
transfer or tournament occurs. SHALLOW4 remains the strongest unresolved target,
with its original512-pair +19.700 adjusted[-7.490,+47.134] result unchanged.
The165 partial final-stage games remain raw evidence only; no automatic resume.

SR-021 stays ACTIVE pending a specific resource-priority decision: technical
DEFERRED without adoption, or preparation of a new independent SHALLOW4 study
in six separately scheduled Windows sessions (about1-3h each,15.3h total).
The latter needs a validated scoped session adapter and a prospective frozen
protocol; no contrast3 has been launched/spent. Scheduled statistical looks
would remain128/512/2048, with no strength looks at intervening operational
boundaries and no pooling or reuse of interrupted evidence. This is a proposal,
not authorization to run sessions back-to-back or restart the stopped study.
See the [SR-021 report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md)
and remaining-resource-decision-v1.json. No successor is authorized.

### SR-021 owner interruption and resource allocation - 2026-10-16 local

The owner stopped SHALLOW4 v2 during its final stage at 2026-10-15T16:28:37Z.
Verified shutdown covers the identified Fastchess, four engine JVMs and their
supervising process tree. The original 128/512-pair assessments and frozen inputs
are byte-preserved: the second remains +19.700 Elo, adjusted [-7.490,+47.134],
original-rule CONTINUE and inconclusive. No 2048-pair final look occurred.
The 165 surviving stage3 normal games (82 complete pairs and one extra color)
are retained solely as partial raw evidence. Native failure receipts reflect
forced shutdown; the research classification is OWNER-INTERRUPTED for resource
allocation, not statistical inferiority, a declining-effect claim or success.
Do not resume the interrupted study automatically or transfer it to another host.

The owner's constraint concerns productive access to the primary Windows PC:
approximately 4-6 continuous disruptive hours are acceptable; another 12 hours
are not. This is an SR-021 scheduling/resource constraint, not a programme-wide
compute ceiling, rejection rule or concern primarily about inconclusive results.
Prefer reduced disruption and prospectively fixed, safely checkpointed sessions
or another suitable host when additional evidence has value. Operational pauses
must preserve pairing, identities, resource comparability and statistical rules;
no inspection of incomplete results to alter candidates or hypotheses.

SR-021 remains ACTIVE. Continue bounded analysis, correctness and DEEP development
screening before a further investment decision. The prior contrast3 resource
proposal is unlaunched and does not authorize an uninterrupted eight-hour run.
Mac execution is a feasibility option only: no transfer, infrastructure change,
BotLi interruption or costly tournament is authorized by discussing it. Any Mac
confirmation needs a separate prospective design with controlled baseline and
candidate, not pooling into this interrupted Windows comparison. See the
[SR-021 report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md)
and owner-interruption evidence. Production and predecessor conclusions remain
unchanged; no successor is authorized.

### SR-021 four-worker second look and final frozen stage - 2026-10-16 local

SHALLOW4 v2 reaches512 independent fresh pairs/1,024 normal games,
W/D/L[454,174,396],+19.700 Elo, adjusted paired-normal[-7.490,+47.134],
required2.5-SE[-4.026,+43.611], bounded[-34.388,+74.767]. Original decision
CONTINUE, plug-in conditional power.93117. This is neither a positive boundary
nor a probability of benefit. All completed-stage audits pass; cumulative CPU
ratio.995568. The unchanged executor starts the final1,536-pair stage toward
the predeclared2,048-pair maximum. Do not retune, add looks or extend after closure.

Current throughput forecasts roughly11.5h more wall, within the original
resource envelope and24h stage watchdog. Heavy QA/build/book work remains idle.
Native SHALLOW1 stays closed for futility. Larger DEEP exploration/value review
is queued after this run; no contrast3 launch, production adoption or successor.
SR-021 remains ACTIVE. Evidence and runtime continuation are in the
[SR-021 report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md).

### SR-021 four-worker first independent look - 2026-10-16 local

The frozen SHALLOW4 v2 contrast2 completes128 fresh pairs/256 normal games,
W/D/L[124,40,92],+43.658 Elo with adjusted paired-normal[-8.068,+97.400],
required2.5-SE[-1.502,+90.344] and adjusted bounded[-65.236,+162.174].
Original decisionCONTINUE: the unchanged runner advances toward512 cumulative
pairs. This is not a positive boundary, adoption or demonstrated worker effect.
CPU ratio1.002848 and complete game/process/input audits pass. Preserve native
SHALLOW1's separate closed futility, all frozen protocols and current evidence.

After this contrast closes, reassess DEEP through proportionate larger clock
exploration before any separate fresh contrast3 confirmation. Existing resource
forecasts are per-look approximations, not overall sequential power or a minimum
acceptable gain. SR-021 alone remains ACTIVE, production unchanged, no successor.
See the [SR-021 report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md).

### SR-021 follow-up mechanisms and four-worker confirmation - 2026-10-16 local

SR-021 remains ACTIVE; production is unchanged cumulative QXRAY. Completed
D2one/four development is -21.74/-5.43 Elo and DEEPone +32.67, each only32 pairs
with broad intervals crossing zero. These do not establish variant strength.
A distinct CHILD D1 child-static formulation has a restricted stand-pat rationale
but retains selective ancestry and truthful omitted ordinals. Its corrected
prototype passed real-model evaluator/proof/replay/all-worker checks. Whole-D6
wall is1.155/1.074 ofOFF at1/4 and an independent opening10 replay finds an
adverse8 raw-unit choice. Higher admitted-node NPS counts cheap static probes,
not demonstrated useful search progress. CHILD is set aside without clock
confirmation; all attempts, including a corrected cancellation gap and one
capped root replay, are preserved in the [SR-021 report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md).

The original frozen SHALLOW4 v2 contrast2 remains unchanged and is the next
independent investment after checkpoint validation:128/512/2048 fresh paired
openings,10+0.1, two simultaneous four-worker games, original adjusted rules and
futility stopping. Its distinct worker information value survives SHALLOW1's
closed computational futility. Do not rerun/extend the closed native study,
retune the frozen finalist, pool exploration, or infer strength across workers.
DEEPone remains an unresolved, lower-opportunity development hypothesis for a
subsequent evidence/value decision. No successor, adoption or external acceptance.

### SR-021 first closed confirmation and distinct follow-up - 2026-10-16 local

SR-021 remains ACTIVE and production remains unchanged cumulative QXRAY.
The one-worker SHALLOW v2 independent protocol is CLOSED for its original
computational-futility rule at512 fresh pairs/1,024 normal games:400/222/402,
-0.679 Elo, adjusted paired-normal[-25.162,+23.799], required2.5-SE
[-23.875,+22.512], bounded sensitivity[-51.897,+50.510]. CPU ratio1.001139.
This establishes neither gain nor inferiority/equivalence. Do not extend the
protocol, reuse its unplayed reservation or pool exploratory results into it.
The separately frozen SHALLOW4 v2 protocol remains unlaunched at this checkpoint.

Independent nominal-D6 replay found an adverse opening04 choice: SHALLOW's
c2c3 scores0, while the baseline a2a3 scores2. These uncalibrated score units
are finite-horizon evidence, not demonstrated game loss or false TT proof.
At opening05, both choose e1g1 and SHALLOW16 matches the reference rather than
baseline14. Preserve both findings and conditional-sampling limitations.

Distinct D2 research retains the existing depth2 margin100 and protections,
removes depth1 omission, and leaves SHALLOW v1 binary/protocol unchanged. It
passes focused, evaluator/proof, independent omission, all-worker and UCI checks;
clean costs justify proportionate fresh exploratory clock screens at1/4. It
still exhibits the opening04 adverse choice. A separate DEEP1 screen tests
lower-opportunity depth3 work. No margin sweep, retuned frozen finalist or
production adoption is implied. All five formulations and raw positive,
negative, inconclusive and failed evidence remain in the
[SR-021 report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md).
The new D2 prototype is frozen as `app/build/sr021-p2-v2`, app.jar
`fdd4f7d65f86a12c92b132f99e7cede1128eb7bdd7a0254b9dafb4c61ab8275e`.
Future confirmation requires fresh evidence and the remaining family-error
allocation; SHALLOW4 retains its originally assigned contrast2. No successor
is selected, and no owner/GPT acceptance or Project Source synchronization is
claimed by this active research checkpoint.

### SR-021 frozen finalist and prospective confirmation - 2026-10-15 local

SR-021 remains ACTIVE; production is unchanged QXRAY/EFFORT1/ROOT_REUSE2..16.
The [SR-021 report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md)
and `sr021-phase2-2026-10-15/` evidence preserve the challenged static-headroom
study, LEAF/SHALLOW/PREFIX/DEEP formulations, independent omission replays,
clean whole-request costs and four completed development contrasts (256 normal
games). SHALLOW development is promising at1/4; LEAF1 and PREFIX4 are near
neutral with wide intervals. These screens establish neither gain nor variant
superiority. No production policy or binding proof requirement has changed.

Unchanged SHALLOW is frozen in `finalist-v1.json`: protected interior D1/D2 quiet
omission, empirical margins50/100, lazy parent evaluation and conservative
selective dependency. No omitted move supplies a child score, PV, searched
ordinal or history update. First/hash/PV moves, checked/checking/tactical/castling,
sparse-material, mate-band and rule-boundary protections remain. The frozen
candidate app.jar is `c2a5a379e0e5e8a3d0590a7322d449c1c0e6db7003b0fcc56744336fc46c3836`.

Independent SR-001A/QXRAY omission replays and rolling/fresh evaluator/ordinary
proof checks passed within their explicitly conditional samples. Broad regression
passes806 tests with11 existing disabled; frozen-model UCI/lifecycle coverage
passes44 requests across1..16, and deeper4/16 exercise futility,QXRAY,LMR,WINDOW
in every worker. Functional availability does not infer all-worker playing strength.

Separate fresh, root-disjoint1/4-worker confirmations are prospectively staged
at128/512/2,048 pairs and10+0.1, with family contrasts1/2, adjusted approximate
paired-normal inference, required2.5-SE and bounded sensitivities, and fixed
positive/negative/computational-futility/inconclusive rules. The legal full-history
Bjbraams source changes the development distribution; source identities,
exclusions, precision/resource forecasts and limits are durable. Unstarted v1
protocols had an inadequate inherited operational watchdog; preserve them and
all reserved inputs, and use only replacement `confirmation-w1-protocol-v2.json`
and `confirmation-w4-protocol-v2.json` with24hour stage watchdogs. No confirmation
outcome is claimed at this checkpoint. Do not retune or extend after a stopping
decision. No successor is selected, and owner acceptance/mirror handback remains
reserved for the eventual SR-021 technical disposition.

### SR-021 activation - 2026-10-15 local

The owner confirms SR-024 IMPLEMENTED acceptance, GPT reconciliation without
remediation and synchronization of BOTH P2-R028/P2-F032 Project Source mirrors.
Earlier pending statements are historical and satisfied; closed experimental
conclusions remain unchanged. Only SR-021 Futility-Pruning Family is ACTIVE.

The [SR-021 report](../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md)
records verified clean HEAD `bea10059c3c2384e146ecd99aa7d1019775d5d70`, all 377
source/build/resource and 15 frozen-installation hashes matching accepted SR-024,
fixed generation32/model identity and unchanged Phase1 archives. Comparator is
QXRAY cumulative production: EFFORT1 and ROOT_REUSE2..16 with WINDOW/quiet LMR;
SEE OFF remains an older control. Build35 retains its non-blocking initialization
warning, with idle finalizer state and no repair required.

The bootstrap challenges incremental opportunity beyond WINDOW/LMR, Pair-2
static headroom versus QXRAY continuation, early quiet/sparse resources, added
evaluation cost and worker-specific economics. Observe before selecting numeric
margins; validate omission/proof/ordinal/lifecycle behavior before costs and clock
screens. Adoption requires credible independent strength and supported-worker
integration. No pruning, evaluator change or neighboring programme is adopted.
Repository masters remain authoritative during this feature. Future technical
closeout, owner acceptance, BOTH mirror refreshes and deployment are distinct.


### SR-024 implemented SEE composition — 2026-10-15 local

**IMPLEMENTED technical closeout; external owner/GPT acceptance is pending.**
The [SR-024 report](../docs/research/search/SR024_SEE_PRUNING_PHASE2_2026-10-14.md)
records the challenged SEE premise, eight distinct selective/ordering formulations,
640 exploratory games, independent oracles and adverse tactical witnesses,
interrupted attempts, complete confirmations, resource limits and integration.
The original Overwatch-affected attempt and later Windows Update interruption
remain preserved and excluded from the successful confirmations; their former
human workload gates are satisfied. No successor is authorized.

QXRAY omits SEE-negative ordinary nonchecking qcaptures outside check, retaining
promotions, en passant and captures uncovering another friendly slider's geometric
attack on an enemy non-pawn/non-king piece. This narrow safeguard retains an
observed nonchecking-clearance resource; it is heuristic, not a full tactical
continuation or a position-score proof. Checked evasions, legal exhaustion,
terminal/draw handling and full history remain authoritative. Omitted moves
supply no evaluator/history transition, searched outcome, PV suffix or ordinary
nominal TT proof. Selective dependencies remain conservative. No nominal SEE
capture/quiet pruning, Pair-2 NMP/IID or evaluator change is adopted.

| Frozen independent comparison | Pairs / games | Candidate W/D/L | Elo | Adjusted primary interval | Required 2.5-SE interval | CPU candidate/baseline |
| --- | ---: | --- | ---: | --- | --- | ---: |
| QXRAY1 vs accepted native EFFORT1, concurrency 4 | 2,048 / 4,096 | 1,747/886/1,463 | +24.128 | +9.121 to +39.227 | +12.261 to +36.052 | 1.000747 |
| QXRAY4 vs accepted ROOT_REUSE4, concurrency 2 | 768 / 1,536 | 676/299/561 | +26.061 | +4.272 to +48.057 | +7.044 to +45.235 | 1.018770 |

Both met their prospective positive stopping criteria without retuning or
post-stop extension. The successful campaigns use disjoint fresh paired openings,
10+0.1, fixed generation32 quarter-residual mapping, Java21 and the Ryzen5500
host; they are not pooled across worker domains. Match wall totals are 8.106h
and 5.855h. Their adjusted bounded sensitivities still cross zero, so the positive
inference depends on the predeclared eligible paired-normal approximation.
Sampled background-load bursts/page reads in the one-worker run remain disclosed
limitations; no score-selected games were removed. Equal clocks do not imply
equal CPU cost, and no universal clock/weight/platform or equal-compute gain is
claimed. Functional availability at other worker counts is not strength evidence.

Normal Pair-2 production now applies QXRAY with native EFFORT1 and ROOT_REUSE2..16;
all retain quiet LMR, WINDOW LMP, private worker history, completed-root guidance,
correct accounting, legal PV and helper draining. Default workers remain one.
The explicit SEE-off control retains the entire pre-SR-024 cumulative composition;
older static/unpruned/research controls, independent exact/SR-001A references,
other evaluators and the inherited coordinator remain available. UCI warm-up,
service routing and normal/control/research identity distinguish absent SEE
configuration from explicit OFF, including after worker changes.

Integration passes 800 broad tests with 11 existing disabled, 33 focused tests
and 188 real-model UCI requests. Actual factory tests cover all 1..16 workers;
deeper 4/16-worker checks execute SEE, LMR and LMP in every worker. Eighteen
native deterministic comparisons match frozen QXRAY; eighteen SEE-off comparisons
match accepted production. Managed play, self-play, validation, budget/stop/reset,
worker transitions, HCE/other-evaluator and reference paths pass. All runtime
libraries and 709/714 application class files match the confirmed candidate,
including all hot Search/SEE/TT/evaluator code; differences are confined to the
three integration source classes and two nested lifecycle classes.

Frozen production source `35294146bb7e44975b516005b2cc196439666e09`, installation
`app/build/sr024-production-v1`, JAR SHA-256
`2341955302900ae1a15879d0cff6b14a3744498db0dd862b8cd2aabe7482ed0f`.
The report and identity manifest distinguish the assembly's dirty source tag
(build-log bookkeeping) from verified clean runtime source. Build35 retains the
non-blocking pre-reservation initialization warning; no bump or repair is claimed.
No release/deployment or external acceptance occurred. Owner/GPT reconciliation
and replacement of BOTH P2-R028/P2-F032 mirrors, followed by explicit confirmation,
block dependent programme planning and successor launch preparation.

### SR-024 activation — 2026-10-14 local (historical)

The owner explicitly accepts SR-036 as IMPLEMENTED, reconciled without remediation,
and confirms subsequent replacement and synchronization of BOTH P2-R026/P2-F030
Project Source mirrors. Their former pending handback statements are satisfied
history. The separately owner-accepted feature-availability investigation covers
all supported 1..16 workers; SR-024 must preserve that cumulative capability.

At activation, only **SR-024 SEE-Based Move Pruning was ACTIVE**, under the
complete authorized feature-level CGLHW. Clean HEAD `d96476053f8f9f7bf57fbe3c58c60bac4f4a00b7`, frozen
SR-036 source/binary, generation32 model payloads and Phase1 archive hashes match.
The [SR-024 report](../docs/research/search/SR024_SEE_PRUNING_PHASE2_2026-10-14.md)
records actual EFFORT1/ROOT_REUSE2..16 composition, historical witnesses, challenged
SEE assumptions, initial hypotheses, measurement conditions and continuation.
At that activation checkpoint no production change or strength conclusion was
claimed. The completed integration above preserves independent
references, ordinary proof restrictions, checked evasions, rule/terminal handling,
searched-only history and private worker state. No successor is authorized.

### Owner-accepted SR-036 handoff (historical evidence)

SR-036 Phase 2 Concurrency Extension has reached **IMPLEMENTED technical
closeout**. The inherited same-depth coordinator remains **BASELINE**, retained
through static controls and other-evaluator paths. At its closeout no successor
was selected; SR-024 subsequently reached the technical closeout above. SR-007 DEFERRED owner/GPT acceptance and BOTH
P2-R022/P2-F026 mirror synchronization are satisfied predecessor history.

The [SR-036 report](../docs/research/search/SR036_CONCURRENCY_PHASE2_2026-10-14.md)
records the verified starting composition, nine explicit formulations beyond OFF,
256 development games, serial/parallel and contention diagnostics, negative and
invalid evidence, model/proof/lifecycle validation, and production integration.
Private/copied ordering, reservation ablation, owner/offset/asymmetric helpers,
root scouts/full jobs and completed-root PV/private seeded history were compared.
The cumulative racing and initial split screens did not establish a clock gain;
ROOT_REUSE was independently confirmed without tuning after confirmation began.

Both disjoint, prospectively frozen confirmations are CLOSED POSITIVE at 10+0.1,
one simultaneous game, four candidate workers, fixed generation32/quarter-residual
mapping, 64MiB requested shared TT, 256..512MiB heaps and JDK21 on the measured
six-physical/twelve-logical-core Ryzen5500 host. JVM ActiveProcessorCount6 is not
affinity or exclusive CPU isolation. Primary against the actual accepted
one-worker binary stops at its prescribed 384-pair second look: **348/160/260,
+39.99 Elo**, family/repeated-look adjusted paired-normal **[+12.10,+68.40]**,
required 2.5-SE **[+13.55,+66.89]**, bounded sensitivity **[-18.91,+101.29]**.
The conclusion uses the predeclared approximate paired inference; the bounded
sensitivity does not independently establish gain. The final 1,024-pair look is
not run. The separately fixed 64-pair inherited-four-worker contrast gives
**115/6/7, +428.75 Elo**, primary adjusted bounded **[+166.45,+infinity]**.
All 896 confirmation games terminate normally and pass independent audits; inputs
are stable. Neither contrast is pooled with development or extended after closure.

Candidate lifetime CPU is **1.8665x** accepted one-worker and **.4832x** inherited
four-worker in the separate matches, with aggregate-admission ratios 1.4258/.4341.
Equal clocks do not mean equal computational cost. Fixed-depth/serial ablations
and 2/4/6-worker diagnostics support useful scheduling and diminishing returns,
not a universal optimum or unmeasured worker-count Elo. JIT/GC/allocation and
interrupted-work limits are preserved; no unique-duplication, energy, long-clock,
other-weight/platform or equal-total-CPU strength conclusion is claimed.

Production Pair-2 at 2..16 workers now selects the confirmed ROOT_REUSE coordinator,
with eldest-first root work, original-scout-window full retries, completed-root
PV/effort ordering and privately seeded quiet history. Whole cohorts complete
and drain before publication; restricted root jobs supply no ordinary root proof.
Interior proof/applicability, real-ply mates, rules/history, generation/owner and
private evaluator/board/PV boundaries remain unchanged. One-worker EFFORT and
default worker count 1 remain unchanged. Other evaluators and HCE's independently
calibrated path are preserved. `createStaticControl` and UCI
`qsearchResearch=static` preserve inherited static behavior. Research OFF means
no override of current production; earlier scoped LMR/LMP/reuse controls remain.
No TT storage/locking redesign, Pair-2 NMP, IID/IIR or successor feature is adopted.

Clean integrated source snapshot **b73a646d4232058f1b927beac52418839cd64520**,
installation `app/build/sr036-production-v1`, build35, app.jar
**bec9636831288f346f2a3590dcf6c3fa558ed65fb967e83c85e9daed79fb51b0**.
Integration code is dffa31700ab3f72649a7b84312f2020d32daf091. Of 712 application
classes, 710 are byte-identical to confirmed v4/d652078; only the production
factory and UCI identity reporter differ. Runtime dependencies are identical.
Validation passes **767 broad tests** with 11 existing opt-in skips and no failures,
**336 integrated UCI requests**, 30 production/legacy/research preflight requests,
and 64 deterministic accepted-one-worker comparisons. Earlier stratified frozen-
model/proof and complete-cohort audits apply to unchanged search class bytes.
All game/UCI processes close cleanly. No GUI/mobile, full training or foreign-
platform campaign is claimed. Both Phase1 archives and all model/checkpoint
identities remain unchanged. Build35 retains its non-blocking initialization
warning; final version status is idle, with no active/unfinished operation or bump.

Broader independent-depth Lazy SMP, voting, DTS, shared history and finer resource
attribution remain hypotheses, not rejected universals or prerequisites for this
scoped implementation. Future selected research must explicitly distinguish modern
production from legacy controls, preserve proof/lifecycle rules and calibrate
independent-game concurrency for actual workers/resources. SR-035's historical
ceiling concern remains unvalidated; no neighboring subject is activated.
SR-036 owner acceptance and mirror synchronization are now explicitly confirmed
by the SR-024 launch; no release or deployment is claimed.

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

Current research masters are **P2-R028/P2-F032**. SR-024 is technically IMPLEMENTED;
no feature is ACTIVE and no successor is selected. SR-036 and earlier records
retain their original evidence and scope; historical pending statements do not
reopen owner-confirmed predecessor gates.

Human actions required after this prompt: GPT/owner reconciliation and an explicit
acceptance or remediation decision, then replacement of BOTH Project Sources from
`source/CHESS_SEARCH_CONTRACT.md` (P2-R028) and
`source/CHESS_SEARCH_RESEARCH_FRONTIER.md` (P2-F032), followed by explicit
synchronization confirmation. These external actions are blocking for dependent
programme planning and successor launch preparation, not unfinished SR-024
implementation. Codex has not performed or claimed them. No cleanup, version
repair, push, release or deployment is required. `PROJECT_SOURCE_REFRESH_REQUIRED: YES`.

| Revision | Change |
| --- | --- |
| P2-F001 | Initialize all 38 historical subjects: admit 20 rejected subjects anew, retain 14 implemented baselines and two evidence conclusions, preserve two pending subjects and embedded unresolved/non-adopted scopes; establish baseline-dependent SR-001 intent, dynamic GPT selection and feature CGLHW handback. |
| P2-F002 | Close the BRE-Pair 2 measurement prerequisite as technically ready with documented limitations; record fixed Best identity, production/runner/statistical evidence, remaining limits and mirror/acceptance gates; preserve all 38 dispositions and SR-001 PENDING. |
| P2-F003 | Record confirmed predecessor acceptance/synchronization, verify the actual clean baseline and activate only SR-001; link challenged hypotheses, proof/mate integration boundaries, prospective strength design and durable continuation. |
| P2-F004 | Close SR-001 as IMPLEMENTED for single-worker Pair-2 with independently confirmed unpruned qsearch, retained alternatives/controls and proof boundaries, integration evidence, limits, cross-feature implications and owner/GPT synchronization handback; activate no successor. |
| P2-F005 | Confirm SR-001 owner/GPT acceptance and predecessor mirror synchronization; activate only authorized SR-029 on verified cumulative Pair-2 production and link challenged hypotheses, proof/accounting boundaries, prospective experiments and resume state. |
| P2-F006 | Close SR-029 as IMPLEMENTED for single-worker Pair-2 after independent quiet-LMR confirmation and production/control integration; preserve adverse alternatives, statistical/resource limits, proof/accounting and future-feature implications, exact evidence and owner/GPT mirror handback; select no successor. |
| P2-F007 | Confirm SR-029 owner/GPT acceptance and predecessor mirror synchronization; verify cumulative production/checkpoint/archive identities and activate only SR-006, linking challenged hypotheses, historical evidence, completion/proof boundaries and prospective research. |
| P2-F008 | Close SR-006 as REJECTED within investigated conditions after six formulations, 640 development and 2,048 independent confirmation games; retain inconclusive strength intervals, measured economics, production/controls, semantic evidence and future implications, with owner/GPT mirror handback and no successor activation. |
| P2-F009 | Confirm SR-006 owner/GPT acceptance and predecessor mirror synchronization; verify clean cumulative production, checkpoint and archive identities; activate only SR-022 with challenged ordinal/omission hypotheses, proof/history boundaries, prospective controls and durable continuation. |
| P2-F010 | Close SR-022 as IMPLEMENTED after six formulations, 448 development and 2,048 independent confirmation games; record protected WINDOW integration, runtime/control equivalence, proof/history/accounting findings, statistical/economic limits, future implications and owner/GPT both-mirror handback; select no successor. |
| P2-F011 | Confirm accepted SR-022 predecessor and both-mirror synchronization; verify cumulative production/checkpoint/archives and activate only SR-023, linking challenged history hypotheses, proof/learning boundaries, prospective experiments and durable continuation. |
| P2-F012 | Close SR-023 as DEFERRED after six formulations, 448 development and inconclusive 2,048-game independent REPLACE confirmation; preserve unchanged production, conditional-history/resource/proof evidence, validation, precision limits and explicit resumption conditions, with owner/GPT both-mirror handback and no successor. |
| P2-F013 | Confirm accepted SR-023 DEFERRED and both predecessor mirrors; verify clean cumulative production/checkpoint/archives and activate only SR-030, linking challenged check/qsearch horizons, proof/resource boundaries and prospective research. |
| P2-F014 | Close SR-030 as DEFERRED after six formulations, 704 development and two independent 2,048-game confirmations; retain unchanged production and controls, unresolved modest BOUNDARY benefit, proof/horizon/resource findings, full validation and fresh-evidence resumption conditions; hand back owner/GPT acceptance and BOTH mirrors with no successor. |
| P2-F015 | Apply Contract P2-R011 experimental efficiency throughout autonomous feature CGLHWs: suitable concurrency, progressive tests, information-value/cost decisions, justified formulation stopping, independent prospective confirmation, evidence and disposition distinctions; preserve feature-closeout handback, all dispositions and pending owner gates, with updated mirror targets and no successor activation. |
| P2-F016 | Confirm accepted SR-030 and synchronized predecessor mirrors; verify SR-005 eligibility and comparator, activate only its iteration-reuse CGLHW and link challenged hypotheses, scoped proof/lifecycle controls and durable continuation. |
| P2-F017 | Close SR-005 as IMPLEMENTED after six formulations, 192 development and positive first-look 256-game EFFORT confirmation; retain proof/lifecycle boundaries, costs and uncertainty, preserved OFF/research controls, verified integration, future implications and owner/GPT BOTH-mirror handback; activate no successor. |
| P2-F018 | Confirm owner-accepted SR-005 and synchronized predecessor mirrors; activate only the scoped Pair-2 SR-018 extension against verified EFFORT cumulative production, preserving HCE BASELINE and binding proof/history boundaries. |
| P2-F019 | Close Pair-2 SR-018 as DEFERRED after five pruning formulations, 256 development games and closed 256-game SHALLOW confirmation futility; preserve EFFORT production/HCE BASELINE, controls, semantic/resource evidence, unresolved benefit and proportionate fresh-resumption conditions; hand back owner/GPT reconciliation and BOTH mirrors, with no successor. |
| P2-F020 | Apply Contract P2-R016 concurrency and event-driven monitoring throughout future feature CGLHWs; reconcile owner-accepted SR-018 DEFERRED and synchronized predecessor mirrors, preserve all dispositions/production/evidence, and require new BOTH-mirror refresh plus starter revision correction without feature activation. |
| P2-F021 | Confirm owner-accepted SR-018 and synchronized starting mirrors; verify cumulative EFFORT/model/archive identities and activate only SR-007 IID/IIR with challenged mechanisms, binding proof/lifecycle boundaries and durable research continuation. |
| P2-F022 | Repair the stale activation header (P2-F021 was already recorded in history), reconcile SR-007 selection prose and record validated research controls/frozen candidate; preserve ACTIVE status, accepted EFFORT and independent clock-confirmation requirement. |
| P2-F023 | Record completed five-policy SR-007 development, refreshed all-policy integrity and selection of unchanged IID_WIDE for fresh prospective confirmation with explicit information-value, stopping and identity controls; retain ACTIVE status and unchanged EFFORT production. |
| P2-F024 | Record closed 128-pair IID_WIDE confirmation futility, complete integrity/results/process shutdown and owner-requested temporary pause; preserve SR007 ACTIVE/incomplete, unchanged EFFORT, frozen protocol, explicit RESUME gate and remaining research without successor activation. |
| P2-F025 | Record explicit owner RESUME, clean provenance and verified frozen/pause evidence; keep the completed IID_WIDE confirmation closed while continuing scoped IIR_ALL and distinct IID coverage/probe-quality research over unchanged EFFORT. |
| P2-F026 | Close SR-007 as DEFERRED after seven formulations, 416 development games and closed 256-game IID_WIDE confirmation futility; retain EFFORT production, explicit controls, verified evidence, quantified modest-gain uncertainty and fresh resumption conditions; hand back owner/GPT reconciliation and BOTH mirrors without successor activation. |
| P2-F027 | Confirm owner-accepted SR-007 DEFERRED and BOTH P2-R022/P2-F026 mirror synchronization; activate only the SR-036 Phase2 concurrency extension over the retained SR-036 BASELINE, recording verified comparators, composition confounding, lifecycle/proof boundaries and scoped research continuation. |
| P2-F028 | Record completed SR-036 development, validated ROOT_REUSE finalist, causal/resource diagnostics and two prospectively frozen independent confirmation contrasts; retain ACTIVE, unchanged production, inherited BASELINE and owner acceptance/mirror boundaries. |
| P2-F029 | Record positive 384-pair primary SR-036 confirmation, uncertainty and measured CPU cost; prepare unchanged ROOT_REUSE production integration while the independent inherited-option contrast runs, retaining ACTIVE pending executable/compatibility validation and preserving inherited BASELINE. |
| P2-F030 | Close SR-036 as IMPLEMENTED after positive independent one-worker and inherited-four-worker confirmations; record integrated ROOT_REUSE identity, unchanged search bytes, compatibility validation, CPU/resource and statistical limits, preserved inherited BASELINE and explicit owner/GPT both-mirror handback; select no successor. |
| P2-F031 | Confirm owner-accepted SR-036 IMPLEMENTED and BOTH predecessor mirrors; verify cumulative production/model/archive identities and activate only SR-024, recording challenged SEE hypotheses, all-worker compatibility, proof boundaries and durable research continuation. |
| P2-F032 | Close SR-024 as technically IMPLEMENTED: adopt frozen QXRAY across Pair-2 EFFORT1/ROOT_REUSE2..16 after independent positive confirmations; record tactical/proof boundaries, CPU/statistical/host limits, unchanged hot classes, all-worker and consumer validation, preserved controls and explicit owner/GPT both-mirror handback; select no successor. |
| P2-F033 | Confirm SR-024 owner acceptance and BOTH predecessor mirrors; activate only SR-021 over verified cumulative QXRAY production, preserving evaluator/reference/worker boundaries and recording bootstrap challenges and durable research continuation. |
| P2-F034 | Record SR-021 unchanged SHALLOW finalist, four-formulation development, conditional correctness/proof and all-worker validation, fresh source/exclusions and prospectively staged1/4 confirmation with preserved unstarted superseded protocols; retain ACTIVE and unchanged production without successor activation. |
| P2-F035 | Record closed512-pair SHALLOW1 confirmation futility without inferiority/adoption, independent adverse/favorable root witnesses, distinct validated D2 follow-up and prospective development while preserving frozen SHALLOW4 contrast2, original protocols and all-worker/proof boundaries; retain ACTIVE with no successor. |
| P2-F036 | Record completed D2/DEEP development, corrected child-static leaf evidence and adverse economics/decision findings, then justify the unchanged independent SHALLOW4 contrast while preserving closed native futility, all attempts and unchanged production; retain only SR-021 ACTIVE. |
| P2-F037 | Record four-worker SHALLOW first independent look and original-rule continuation without adoption, validated process/clock evidence, and subsequent DEEP exploration/value decision while preserving native closure, all frozen inputs and SR-021-only scope. |
| P2-F038 | Record the512-pair four-worker independent result and original-rule final-stage continuation, audited CPU/game evidence and prospective resource bounds; preserve native closure, queued DEEP investigation and no adoption or successor. |
| P2-F039 | Record owner-directed SHALLOW4 interruption, preserved completed and partial evidence, verified process shutdown and SR-021 primary-PC resource constraint; continue bounded research and limited Mac feasibility without resumption, pooling, adoption or successor. |
| P2-F040 | Record completed bounded DEEP development and additional correctness/worker evidence, preserved interruption and source identities, owner-deferred Mac access and the specific remaining resource choice; retain ACTIVE without adoption, automatic resumption or successor. |
| P2-F041 | Close SR-021 technically as DEFERRED by explicit owner resource choice after substantial six-formulation investigation; preserve native protocol futility, distinct owner-interrupted four-worker evidence, bounded DEEP uncertainty, unchanged production, no automatic resumption/Mac work/successor, and BOTH-mirror GPT/owner handback. |
| P2-F042 | Record owner-accepted SR-021 DEFERRED and unified repository authority; link tournament execution maintenance, measured concurrency/forecasting and bounded pair-block continuation with failure/statistical safeguards; preserve all feature IDs/dispositions and require BOTH final mirrors plus owner/GPT handback without selecting a successor. |
| P2-F043 | Activate only owner-selected SR-033 after verified clean cumulative baseline and confirmed maintenance acceptance/BOTH predecessor mirror synchronization; record fixed evaluator, censored-outcome challenge, worker scope, separate deferred version provenance and durable research resume point. |
| P2-F044 | Record SR-033 outcome observations, two explicit feedback controls, correctness/proof and all-worker validation, mixed diagnostic economics and frozen independent-worker development contrasts; retain ACTIVE and unchanged production. |
| P2-F045 | Record completed SR-033 development, exploratory worker-specific finalists, measured session economics and frozen fresh independent 128/512-pair confirmation; retain ACTIVE and unchanged production. |
| P2-F046 | Record native MOVE confirmation closed for prospective computational futility and separate frozen four-worker CAUTION confirmation underway; preserve uncertainty and unchanged production. |
| P2-F047 | Record the completed four-worker CAUTION first look and prospective continuation to 512 pairs; keep native MOVE closed, SR-033 ACTIVE and production unchanged. |
