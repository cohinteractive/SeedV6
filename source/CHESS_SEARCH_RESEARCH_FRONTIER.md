# SeedV6 Search Research Frontier — Phase 2

Internal frontier revision: **P2-F013**

Status: **Accepted SR-001/SR-029/SR-022 and scoped SR-006 retained; predecessor gates satisfied. SR-023 owner-accepted DEFERRED with both P2-R008/P2-F012 mirrors synchronized. Only SR-030 check extensions is ACTIVE.**

Repository master: `source/CHESS_SEARCH_RESEARCH_FRONTIER.md`.

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
only SR-030 check extensions.
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

Supported next use: frozen-evaluator single-worker feature comparisons with the
documented paired fixed-sample protocol and independent opening confirmation.
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

### SR-030 active feature - 2026-10-12 local

The owner confirms SR-023 technical reconciliation, DEFERRED acceptance and
replacement/synchronization of BOTH P2-R008/P2-F012 mirrors. Earlier pending
handback statements are historical; do not extend its completed confirmation.
Only SR-030 is selected. The clean starting HEAD is f4927c05d2d5;
19 frozen comparator/checkpoint/archive identities match. The mandatory comparator
is SR-022 production v5, source 2f08516d0f24, generation-32 Pair-2 with unpruned
qsearch, protected quiet LMR and WINDOW LMP, full-window iterative PVS.

[SR-030 report](../docs/research/search/SR030_CHECK_EXTENSIONS_PHASE2_2026-10-12.md)
and [evidence](../docs/research/search/sr030-phase2-2026-10-12/) retain the
challenge, hypotheses, prospective controls, validation and continuation.
Existing checked qsearch already searches evasions; distinguish genuinely added
quiet continuations from redundant boundary work. Nominal and real ply, credit,
PVS retries, TT proof, supported-ply exhaustion and cancellation stay explicit.
No singular/other-extension family, evaluator change or TT redesign is selected.

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
| SR-005 | Iterative-deepening information reuse | PENDING | TT/PV retention, completed-iteration scores/stability and ordering information; interacts with SR-006/034. |
| SR-006 | Aspiration windows | REJECTED | Scoped six-formulation investigation found no credible strength-supported adoption over cumulative SR-001+SR-029. Independent ALPHA16: 809/453/786 over 1,024 pairs, +3.90 Elo, adjusted interval [-12.30,+20.12]; both required/wider sensitivities include zero. Retain full-window production, research controls, negative/cost evidence, uncertainty and owner/GPT handback above; no universal inferiority claim. |
| SR-007 | Internal iterative search: IID and IIR | PENDING | Missing-hash/ordering information, actual-depth proof, reduction quality and total probe/re-search cost. |
| SR-020 | Razoring | PENDING | Reduced/qsearch horizons, evaluator evidence, nominal resources and SR-018/019 composition. |
| SR-021 | Futility-pruning family | PENDING | Current leaf/evaluator behavior, move exclusions, resource losses and omission/proof dependencies. |
| SR-022 | Late-move / move-count pruning | IMPLEMENTED | Confirmed protected D1/D2 WINDOW omission over single-worker Pair-2 SR-001+SR-029: 876/485/687 in 1,024 fresh pairs, +32.15 Elo, adjusted normal lower +16.09 and required 2.5-SE lower +15.38; Hoeffding includes zero. Production/default and explicit-off bridges pass. Preserve omission/proof/history protections, adverse alternatives, complete cost limits and scoped controls; owner/GPT acceptance and predecessor both-mirror synchronization confirmed by SR-023 launch. |
| SR-023 | History-based pruning | DEFERRED | Six formulations and 448 development games; unchanged REPLACE independent 2,048-game confirmation 810/460/778, +5.43 Elo with adjusted interval [-10.56,+21.44], wider and Hoeffding intervals also include zero. No production adoption; possible small benefit unresolved. Preserve frozen evidence, WINDOW baseline and history/proof protections; fresh justified independent design and GPT/owner selection required for resumption, with no post-hoc extension. |
| SR-024 | SEE-based move pruning | PENDING | Validated tactical SEE, quiet-exchange semantics, current leaf horizon and cost of evidence. |
| SR-025 | ProbCut and Multi-ProbCut | PENDING | Legal reduced probes, calibration, actual-depth proof and complete economics; distinct from SR-026. |
| SR-026 | Multi-Cut pruning | PENDING | Multiple-success/diversity evidence, correlated horizon errors and probe cost; distinct from SR-025. |
| SR-027 | Enhanced Transposition Cutoff and TT look-ahead | PENDING | Applicable child proofs, phase/TT traffic, PVS and evaluator-dependent saved work. |
| SR-028 | Alternative/historical forward-pruning screen | PENDING | Credible distinct historical/novel candidates, overlap with named families; RankCut relates to SR-038. |
| SR-029 | Late-move reductions and re-search | IMPLEMENTED | Confirmed quiet-class-protected scaled LMR over accepted single-worker Pair-2 SR-001. Independent 231/119/162 over 256 pairs, +47.11 Elo, primary approximate 95% lower bound +21.06 and required 2.0-SE lower bound +20.53; Hoeffding sensitivity includes zero. Integrated/bridged with conservative proof suppression, preserved off/static controls and actual-domain accounting. Full alternatives, limits, future implications and owner/GPT mirror handback above. |
| SR-030 | Check extensions | ACTIVE | Owner-selected over verified single-worker Pair-2 SR-001+SR-029+WINDOW. Compare checking-edge versus checked-node/evasion horizons, qsearch overlap, bounded forcing continuation, LMR/LMP interaction, real-ply/credit/proof integrity and fixed-clock strength; see active report. |
| SR-031 | Singular-extension family | PENDING | TT anchors, exclusion-domain proof, policy identity, extension alternatives and probe cost. |
| SR-032 | Other tactical/forced-line extensions | PENDING | Forced replies, recapture/promotion/threat/history families, caps/fractional depth and complete root decisions. |
| SR-033 | Adaptive/hindsight depth feedback | PENDING | Confirmed outcomes and static/context evidence; reduction-conditioned variants need a justified reduction producer. |

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
| SR-018/019 | Adaptive reduction/verification, broader depths/margins and neural calibration remain unadopted. BRE-Pair 2 requires its own evidence; no HCE transfer is implied. |
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
cleanup and appropriate Git closeout. Do not require a new prompt per experiment.

1. Read both repository masters and relevant frozen Phase 1 scope/evidence.
   Verify actual code/configuration, dependencies and measurement feasibility.
   Record the selected scope and ACTIVE state; establish proportionate controls,
   statistical design, acceptance criteria and resource constraints.
2. Investigate credible alternatives thoroughly, including reasons for failure
   and interactions with the cumulative baseline. Continue useful internal work
   after failed hypotheses or surprising results. Use appropriate correctness,
   fixed-budget, whole-request and playing-strength measurements separately.
3. Independently confirm promising finalists, avoiding exploratory-data reuse,
   suite overfitting and parameter-selection bias. Record uncertainty and costs.
   A credible small Elo gain may suffice; an inconclusive gain does not.
4. Reach a supported ACCEPTED, IMPLEMENTED, REJECTED or DEFERRED disposition
   under the Contract. Implement only justified changes within authority and
   validate integration/compatibility. Record incomplete work and acceptance
   conditions explicitly; do not use DEFERRED or a canon edit to evade a blocker.
5. Update the Contract for material technical/accepted-baseline changes and
   this Frontier for disposition, scope, evidence links, limitations, dependencies,
   remaining work and human actions. Distinguish evidence, accepted conclusions,
   implementation, strength validation, owner acceptance and deployment.
   Preserve required evidence, reconcile temporary artifacts and unrelated work,
   run applicable checks and complete attributable Git closeout.
6. Return control to GPT and the owner. Report revisions, validation actually
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

Keep entries and evidence cross-references. Increment this document's
`P2-Fxxx` revision exactly once per content-changing update and append one concise
history entry; the Contract independently uses `P2-Rxxx`. Report
`FRONTIER_UPDATED`, the current Contract revision and
`PROJECT_SOURCE_REFRESH_REQUIRED: YES` at handback whenever canon changes.
Never edit the frozen R045/F052 histories or assume a requested mirror refresh
has happened. Only the two active documents require routine mirroring.

Initialization and P2-F002 prerequisite synchronization/acceptance are confirmed
by the owner. P2-F003 activated the separately authorized SR-001 CGLHW after actual
baseline verification. P2-F004 recorded its IMPLEMENTED handback; the owner
confirmed acceptance and P2-R003/P2-F004 mirror synchronization in the SR-029
launch. P2-F005 activated only SR-029 after baseline verification; P2-F006 records
its IMPLEMENTED technical closeout. The SR-006 launch confirms owner/GPT
acceptance and both P2-R004/P2-F006 mirror replacements/synchronization.
P2-F007 activated only SR-006 after baseline verification. P2-F008 records its
scoped REJECTED technical closeout and retains cumulative full-window production.
The SR-022 launch confirms SR-006 acceptance and both P2-R005/P2-F008 mirror
replacements/synchronization. P2-F009 activated only SR-022 after actual baseline
verification. P2-F010 records its independently confirmed, integrated and validated
IMPLEMENTED technical closeout. The SR-023 launch confirms owner/GPT acceptance
and both P2-R006/P2-F010 mirror replacements with explicit synchronization.
P2-F011 activated only SR-023 after actual baseline verification. P2-F012 records
its DEFERRED technical handback, retained WINDOW production and unresolved
strength evidence. The SR-030 launch confirms SR-023 GPT/owner acceptance and
BOTH P2-R008/P2-F012 mirror replacements/synchronization. P2-F013 activates only
SR-030; its final handback remains separate from predecessor acceptance.
No evaluator superiority, deployment or successor selection is implied.

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
