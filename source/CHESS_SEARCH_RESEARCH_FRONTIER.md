# SeedV6 Search Research Frontier - Phase 3

Internal frontier revision: **P3-F014**

Status: **The owner has selected full Construction V1 + Conventional NNUE CP64 as the production and Search Phase 3 baseline. Implementation, focused correctness tests, Windows image/ZIP checks and the informational E008 comparison are complete. The final packaged Windows GUI interaction was stopped with Escape; native macOS remains untested. These acceptance gates remain open; strength does not veto the selection.**

Repository master: `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` in `C:/projects/seed/java/seedv6-unified`.

## Current baseline decision - 23 October 2026

The owner's explicit consolidation launch supersedes the earlier non-promotion,
Pair-2-principal-evaluator and pending Construction acceptance boundaries for this
work. Full Construction V1 (all 18 controls, flags 262143, WIDEN16 aspiration) with
Conventional NNUE S6CNU002 checkpoint 64, eight material heads, is the selected
production and Phase 3 research baseline. Technical correctness and packaging
remain qualification gates; strength relative to E008 is informational, not a
promotion veto. No new CGLHW, training, network replacement, feature addition,
external publication, live bot change or subsequent optimization is authorized.

`engine-core/search/driver/BaselineEngine` owns the composition shared by GUI
Play, ordinary Arena and standard UCI. HCE and frozen BREP2-32 are the two other
normal competitive choices, with the same Construction controls and resource
settings. Conventional CP64 is frozen and absent from training architectures.
`ConventionalNnueEngine` / `NnueCandidateUciMain` preserve recursive E008; historical
Policy 4 and original Construction/Pair-2 entry points remain independently usable.
Historical results below retain their original evaluator, kernel and evidence.

See the [existing construction record](../docs/research/search/P3_CONSTRUCTION_2026-10-20.md#construction-v1--cp64-consolidation-23-october-2026)
for implementation, score semantics, qualification and comparison evidence.
The network file SHA-256 is
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
Mutable accumulators, histories and search state are private to each worker;
only immutable network tables and the coherently synchronized request TT are shared.
Explicit worker settings 1-16 are honored; Max is bounded by available processors.
The default requested hash remains 64 MiB. No score rescaling or frozen-weight
mutation is part of this consolidation.

Build69 runtime source is `18f3c4f12a01417a48fb4c044ff3bde2c5b44b2d`.
CP64 uses BT4_Q_LOGIT400_V1 teacher-link units, displayed as NNUE score / 100;
no numeric rescaling occurred. Play and Arena share explicit units/perspective.
At one worker/Hash32, 24 color-reversed opening pairs per budget give Construction
minus E008 Elo 0.0 (paired bootstrap 95% -65.9 to +65.9) at 200 ms and -51.0
(-137.0 to +36.3) at 1000 ms. These results establish neither an advantage nor
equivalence within +/-50 Elo. All 96 games are legally audited and uncensored.
The comparison used clean build68; a class/source audit verifies unchanged
search/inference in the final package. Exact protocols/runtime remain preserved.
No future search-strengthening work is implicitly launched by this handback.

The 18-20 October authority and champion accounts below are historical scopes,
not contradictory current defaults. The explicit current owner launch authorizes
this work without waiting for manual Project Source refresh. Repository updates
do not update those external mirrors; refresh both before future mirror-dependent
GPT planning. Future independent search optimization retains the existing contract.

## Authority and current direction

**Current disposition, 20 October 2026:** Workstream 2 is closed and owner-accepted:
**retain PVS** on the accepted flattened architecture. The owner confirms the
P3-R012/P3-F010 mirrors and explicitly launches Workstream 3 construction.
The [driver record](../docs/research/search/P3_SR003_SR004_DRIVERS_2026-10-19.md)
retains its historical evidence and limits. The [construction record](../docs/research/search/P3_CONSTRUCTION_2026-10-20.md)
records the completed dependency-aware implementation, all 31 dispositions,
frozen candidate, independent validation and measured costs. Construction is
technically complete; GPT/owner reconciliation and acceptance remain outstanding.
Production Policy 4 is unchanged; no successor or promotion is authorized.

The [Contract](CHESS_SEARCH_CONTRACT.md) governs objectives, technical guarantees,
methodology, architecture and acceptance. This Frontier governs subjects, current
dispositions, dependencies, opportunities and progression within that authority.
Repository masters are authoritative; Project Sources are owner-maintained mirrors.
Listing a subject or opportunity neither activates it nor authorizes implementation.

All **38 stable SR identifiers and research subjects** are retained below, including
embedded alternatives. Identifiers are references, not an execution order or a set
of independent runtime switches. Historical Phase 1/2 dispositions are evidence,
not automatic Phase 3 rejection/adoption decisions. Existing accepted production
remains the champion until a separately validated and explicitly accepted promotion.
A selected coherent objective may span several SR identifiers and internal
workstreams; it need not stop after each feature or experiment.

The owner's 19 October 2026 decision selects two bounded preliminary architectural
investigations, rapid feature-rich construction, then whole-configuration strength
evaluation and reverse ablation. The partition and progression below record approved
future scope. The owner subsequently launched and Codex technically completed
SR-002 as the first bounded investigation. The owner has explicitly accepted
flattened Search execution for subsequent Phase 3 development under the Contract's
near-equal-performance preference, with the recorded limits. The separately launched
driver investigation is owner-accepted; the subsequently launched construction
now has its technical handback, without automatic successor authorization.

Historical production champion before 23 October: **Policy 4**, cumulative Pair-2 **QXRAY + quiet LMR +
WINDOW + completed legal TT hints**, with **EFFORT at one worker / ROOT_REUSE at 2-16**,
requested default 64 MiB TT and the
fixed generation-32 evaluator identity in the Contract. Other evaluators, independent
references, historical anchors and explicit controls retain their supported scope.
The owner confirms final Phase 2 mirror synchronization and GPT acceptance of
SR-032 as DEFERRED without adoption. No successor was authorized by that closeout.
The initialization began no research; the owner's 18 October 2026 launch
authorized the now-completed first objective below.

The first direction was the **production, retention and reuse of useful
Search information**, especially TT move guidance and relevant history interactions.
The owner selected O1 + O2 as that first coherent objective. Principal
opportunities from the [pinned Bagatur analysis][Bagatur] are hypotheses, not proven
bottlenecks or accepted architecture:

| Opportunity | Question and dependencies |
| --- | --- |
| O1: Separate heuristic move guidance from mathematical TT proof admission | Can better completed-node producers, move retention or reuse improve ordering/retries without relaxing proof? Examine current storage before assuming a new table. SR-003/004/005/006/007/012/014/029/031/036; legal hints alone cannot supply singular score proof. |
| O2: History lifetime and learning-signal quality | Can request/iteration/retry retention and controlled private worker seeding improve ordering or selectivity? Distinguish unseen, sparse/censored, reduced-unverified and nominally verified labels; retention can preserve bias. SR-005/014/015/016/021/022/023/024/029/033/036. |
| O3: Pair-2 evaluation evidence and consumer cost | Do calibrated raw movement, residual/correction or uncertainty signals add predictive value beyond rank/history at affordable marginal evaluation cost? Preserve raw/heuristic/proof separation and fixed evaluator identity. SR-009/018/019/020/021/022/029/033, with driver/clock interactions. |

Related opportunities are explicitly compatible selective/restricted-result domains,
precise call/retry/reduction evidence, and targeted Search mechanics. They need a
concrete consumer and scope; a new generic evidence framework is not presumed
necessary. Bagatur examined 38 subjects with 31 direct behavioural matches; its
permissive proof/draw/outcome practices and suggested sequencing do not override
the Contract or establish isolated Elo contributions.

## Completed first objective

**Search Information Retention and Reuse — O1 + O2**, owner-authorized 18 October
2026, is technically complete. It investigated TT information production,
availability and reuse, and history quality/lifetime jointly, with ordering, iteration/retry,
reduction, pruning and private-worker interactions. It began with measured current
structures; neither a TT redesign nor independent gains for each component were
prerequisites. The cumulative Phase 2 champion and fixed Pair-2 evaluator above
supplied the comparison identity; production stayed unchanged at that research handback.

The [focused research record](../docs/research/search/P3_INFORMATION_REUSE_2026-10-18.md)
governs exact experimental identities, protocols, measurements and limitations.
Its pending-acceptance wording records the earlier handback and is superseded by
the owner's subsequent acceptance below; the closed research record is preserved.
It is evidence under this canon, not another governing contract. The completed
work covers current-structure observation, controlled history retention/learning,
completed legal hints, combinations and reverse ablations, lifetime/pressure and
cost measurements, and separate fresh one-/four-worker confirmations.

The owner has explicitly accepted **Policy 4**, the cumulative Phase 2 configuration
plus completed legal TT hint production and reuse. Separate fresh confirmations
used **10+0.1 clocks**, each with **128 paired openings / 256 games**:

| Search workers | Policy 4 W / D / L | Estimated Elo gain | Adjusted paired-normal interval |
| --- | --- | --- | --- |
| One | 113 / 70 / 73 | +54.7 | +4.8 to +107.0 |
| Four | 137 / 46 / 73 | +88.7 | +34.3 to +147.9 |

Both closed positive at their first scheduled look and passed the required 2.5-SE
sensitivity checks. Conservative bounded intervals still include zero; these are
qualified approximate paired-normal results, not distribution-free guarantees.
Four-worker candidate CPU was approximately **0.98% higher under equal clocks**;
equal clocks do not establish equal compute. The recorded evaluator, host/JVM and
resource limits apply; no gain is established at other worker counts, clocks,
memory pressures, platforms or resource configurations. Closed protocols stay closed.

The owner's subsequent production-integration launch authorized implementation
and promotion without reopening research acceptance. The
[integration record](../docs/research/search/P3_POLICY4_PRODUCTION_2026-10-19.md)
records passed configuration-equivalence, correctness, architecture, lifecycle and
compatibility gates. **Policy 4 is formally promoted as the normal Pair-2 production
Search champion/default at 1-16 workers and the future research baseline.**
The previous cumulative Phase 2 engine remains an explicit hints-off comparison
and regression control (`ProductionSearch.createInformationControl`; research
information policy 0). Normal standalone UCI, legacy Pair-2 and GUI Pair-2 share
the production factory. Main-history lifetime and ROOT_REUSE private seeding are
unchanged; no retained-history/filter combination is promoted.

Compatibility checks cover all supported worker counts and representative Hash
settings. Other evaluator families retain prior behavior with completed hints off;
other Pair-2 checkpoints have no new strength claim. Production applicability is
broader than the one-/four-worker generation-32 strength evidence above. The TT
budget, layout, locking, replacement, SearchKey and proof policy are unchanged.
A pre-existing headless Training-panel widget-test failure reproduced with the
frozen pre-integration core; it is separate from the passed Search/GUI-adapter
gates. Native GUI/client and macOS acceptance were not newly established.
Promotion is not external release or deployment. The owner's instruction and revised
local `AGENTS.md` removed the finalizer execution gate for that earlier
production-integration canon update. That historical instruction does not resolve
the inherited reservation or establish version finalization for SR-002; see its
[current closeout state](../docs/research/search/P3_SR002_EXECUTION_2026-10-19.md#owner-acceptance-and-repository-closeout).

Retained history and nominal-only malus filtering have inconclusive conditional
strength evidence and remain unadopted; combined **Policy 7 remains unconfirmed**.
Richer histories, consumer attribution and further TT/driver refinements remain
open hypotheses, not accepted results. Their inclusion in the programme below
does not reopen the completed objective or its closed studies. Limited Pair-2
implementation calibration is within construction scope; independent evaluator
architecture/training research remains outside it. That earlier objective did not
activate a successor; the separate SR-002 launch and disposition follow below.

## Completed SR-002 execution-architecture investigation

The owner-authorized 19 October 2026 CGLHW is **technically complete and
owner-accepted**. The owner explicitly selected **flattened Search execution for
subsequent Phase 3 development** under the Contract's near-equal-performance
preference. This is architectural acceptance, not a claim of superior speed or
playing strength, production adoption or deployment. Recursive Policy 4 remains
the unchanged production champion/default and fallback; independent recursive
references remain required.

A primitive explicit main/q candidate preserves the integrated Policy 4 mechanisms
and existing shared-core ownership. Independent references, exact single-worker
events/outcomes/TT/history, selective retries, PVs, cancellation and representative
one-/two-/four-worker lifecycle checks passed. Frozen one-million-node complete
requests gave flat/recursive elapsed ratios **1.019 [1.014,1.025]** at one worker
and **1.022 [1.013,1.030]** at four; process CPU ratios were 1.020/1.031. These are
approximate host/corpus-specific mechanical comparisons, not Elo or universal
equivalence. Short-request warmup was adverse (four-worker depth-six ratio 1.085);
a separate warmed diagnostic was 1.037 [1.023,1.053]. Four-worker CPU varied across
JVMs, with one fork about 5.2% higher. No material allocation/GC or reserved-stack
advantage was established. The fixed frame payload is 25,600 bytes per worker.

The [focused record](../docs/research/search/P3_SR002_EXECUTION_2026-10-19.md) binds
implementations, frozen protocols, all results, JVM/memory evidence, failures and
limits. Further generic rewriting/layout tuning has insufficient decision value
for this prerequisite. Future feature composition must validate its selected
kernel while retaining recursive independent references; unrelated historical
research policies are outside the new candidate's guarded domain. Recursive
production remains a fallback if later correctness or economics require it.

## Completed SR-003/SR-004 root-driver investigation

The owner accepted the bounded Workstream 2 decision to **retain PVS** under the
current integrated configuration and binding TT model. Seven experimental
PVS/MTD configurations share the accepted flattened kernel. Independent exact/unpruned
references, PV/history/mate/proof checks and cancellation/worker lifecycle tests
passed; final groups total 365 passes with no failures/skips. Recursive Policy 4
remains production champion/default/fallback, and independent recursive references
remain unchanged. That closeout did not promote production or launch construction;
the subsequent owner instruction explicitly launches Workstream 3.

Conventional repeated-pass MTD had scarce ordinary TT proof reuse despite many legal
hint hits, and 35/60 development materializations needed full-window recovery from
selective estimated convergence. Parity, bracketing and bounded/preserved-guidance
hybrids did not remove the complete-request penalty. A frozen fresh-root comparison
completed all 480 requests: TWO_PASS elapsed ratios **1.619 [1.486,1.765] at W1**
and **1.428 [1.339,1.517] at W4**, with aggregate process CPU **1.601/1.758**.
Intervals describe root-cluster uncertainty on this host, not universal performance.

The fixed **32-pair/64-game W4, 10+0.1** development screen completed normally with
TWO_PASS **25 W / 9 D / 30 L**, estimated **-27.2 Elo**, approximate paired-normal
interval **[-108.7,+51.3]** and wider bounded interval crossing zero. This is
inconclusive strength evidence, not rejection by proven inferiority. Lifetime CPU
was **16.5% higher**. There is insufficient replacement benefit to justify more
preliminary work or independent confirmation. Desktop load, short requests, JVM,
platform, clock and worker-domain limits remain explicit. Two zero-game startup
failures and their corrections are preserved; no valid games were excluded.

The [focused record][SR004] binds frozen protocols, exact executable/configuration
identities, costs, failures, uncertainties and all evidence. Retain MTD comparison
capabilities over the shared kernel. This disposition concerns tested formulations,
not every MTD algorithm; broader TT/selective-cache hypotheses require their own
authority and evidence. No binding policy was relaxed and no new prerequisite is
created for the separately authorized construction programme.

## Completed Workstream 3 construction

The [focused record](../docs/research/search/P3_CONSTRUCTION_2026-10-20.md) freezes
`construction-v1` on shared flattened PVS: all 18 immutable controls (flags 262143),
Pair-2 shallow margins 100*d/200*d, shared extension cap one, WIDEN16 and requested
64 MiB TT. Aspiration OFF and individual removals/variants remain available at
1-16 workers. Ordering/history, raw evaluation movement, reduction caution,
bounded check/recapture extension and guarded shallow pruning are active alongside
accepted QXRAY/LMR/WINDOW/EFFORT/completed hints. Production selection is unchanged.

All 31 selected subjects have explicit dispositions. SR-007/018 alternatives
remain available but inactive; SR-020/025/026/027/028/031 formulations are inactive
or excluded for the recorded cost, calibration, redundancy or proof-anchor reasons.
No binding TT change is needed or authorized. The four programme deferrals remain.

Final integrated Search testing passed 303 tests plus one subsequent focused
history-protection control; core/UCI testing passed 78.
Real UCI sessions cover 1/4/16 workers and aspiration ON/OFF. New selective
diagnostics completed 96 sampled independent checks with no sampled false
threshold; a shallow disagreement is explained by recapture/QXRAY horizons.
This is neither a population error guarantee nor playing-strength confirmation.
Warmer depth-six requests cost the aspiration candidate about 11.2%/4.7% more
elapsed and 12.1%/17.3% more process CPU at one/four workers than flat Policy 4;
equal-node costs expose additional per-node overhead. Preserve all failed starts,
earlier JIT-heavy runs and uncertainty. Technical completion awaits GPT/owner
acceptance; whole-configuration strength research and reverse ablation require
their own launch and have not begun.

## Compact research inventory

**Production** means a retained accepted implementation, with scope stated.
**Baseline** identifies inherited functionality, not a new strength claim.
**Evidence only** is a conclusion without an adopted runtime mechanism.
**Unadopted** includes retained research implementations and historical formulations;
P1/P2 REJECTED or DEFERRED labels below describe those studies only. Opportunities
describe each subject's broader scope, not a requirement to implement every variant.
The programme partition below supplies the new dispositions alongside these retained
baseline/historical facts. Dependency references to deferred subjects do not select
them for the first milestone. This inventory activates no work or acceptance.

[P1] and [P2] point to complete frozen Frontiers: consult the same SR row, embedded
scope and linked technical reports. [C1] and [C2] supply detailed technical rules
only within the Contract's explicit inheritance boundary. R-numbers below link
focused Phase 2 reports. This inventory does not duplicate their experimental results.

| ID | Stable research subject | Existing production / historical disposition | Phase 3 opportunity, including embedded scope | Important dependencies | Evidence |
| --- | --- | --- | --- | --- | --- |
| SR-001 | Quiescence Search | Production Pair-2 qsearch, now QXRAY through SR-024; independent unpruned SR-001A retained. Other leaves/controls preserved. | Stand-pat, tactical ordering, SEE, qdepth/explosion limits, qTT, quiet checks/forcing limits, delta and post-static futility; combine credible variants without weakening checked evasions/rules. | SR-009/012/015/020/024/029/030; explicit q-domain identity. | [P1], [R001], [R024] |
| SR-002 | Recursive vs flattened Search mechanics | P3 technically complete and owner-accepted: flattened execution selected for subsequent Phase 3 development under the near-equal preference. Recursive Policy 4 remains production default/fallback. P1 scoped negative evidence preserved. | Guarded integrated main/q candidate; approximately equal warmed costs with short-request/JVM/worker limits. Future composition validation remains necessary; no speed or strength superiority claim. | Independent recursive oracle; accepted foundation used by completed Workstream 2. | [SR002], [P1], [C1], [Bagatur] |
| SR-003 | Principal Variation Search / zero-window Search | Workstream 2 owner-accepted: retain PVS on flattened execution; independent TT-off references retained. | First-move quality, retry economics, exact/scout provenance and interaction with selective consumers. | SR-004/006/008/012/014/029; original-window and nominal verification rules. | [SR004], [P1], [C1], [R029] |
| SR-004 | MTD(f) and memory-enhanced zero-window drivers | Bounded P3 investigation technically complete: tested integrated MTD/hybrids do not justify replacing PVS. Strength inconclusive; adverse request economics. Research comparisons retained; no universal rejection. | Preserved first-guess, convergence, TT, PV-recovery and worker evidence; broader formulations remain hypotheses outside this closed unit. | SR-003/005/006/012; unchanged TT/proof policy; any new material policy requires owner authority. | [SR004], [P1], [C1], [Bagatur] |
| SR-005 | Iterative-deepening information reuse | Production EFFORT/PV and ROOT_REUSE completed-root guidance; broader HISTORY/PVHISTORY unadopted. | Previous value/move/PV, root effort/stability, history retention across iterations/retries and request/new-game aging. | O1/O2, SR-006/012/014/016/034/036. | [R005], [R036] |
| SR-006 | Aspiration windows | Full windows retained; P2 six-formulation study scoped REJECTED, no adoption. | Initial windows, asymmetric/directional widening, volatility/parity and mate handling with better retained guidance. | O1, SR-003/004/005/012; full-request retries. | [R006], [P1] |
| SR-007 | Internal iterative search: IID and IIR | No production IID/IIR; P2 DEFERRED, controls retained. | Missing-hash/PV probes, shallow/near/wide eligibility, reduction versus acquisition cost under improved information reuse. | O1/O2, SR-003/005/012/014/029/031; same-ply restoration and actual-depth proof. | [R007], [P1] |
| SR-008 | Node/evidence model and path context | Evidence only; orthogonal facts/proof/provenance/prediction model binding. No generic role predictor adopted. | Actual scout/full/retry facts, assigned versus nominal depth, rank, previous reductions, parent/path and expected PV/Cut/All evidence with concrete consumers. | All selective consumers; SR-009/029/033/037. | [P1], [C1], [Bagatur] |
| SR-009 | Static-evaluation evidence and correction | Evidence only; bounded HCE findings. No correction-history, generic improving or confidence mechanism adopted. | Raw/cached/heuristic/proof separation, one/two-ply movement, residual calibration, sparse/biased labels, correction learning/aging/bounds and marginal cost. | O3, SR-008/018/019/020/021/029/033/038; evaluator identity. | [P1], [C1], [R018], [R021] |
| SR-010 | Repetition, cycles and graph-history interaction | Baseline formal real-history adjudication and primitive bucket-chain detection; conservative ordered SearchKey. | Broader proven count-state equivalence, history mechanics, upcoming repetition and clock/status relevance without twofold-as-draw or invented history. | SR-011/012/013/018/036; evaluator-relevant status. | [P1], [C1] |
| SR-011 | Mate-distance pruning | Baseline core mate-domain restriction. Finite-horizon gap proof retained; HORIZON_DOMAIN runtime candidate historically REJECTED. | Reassess domain cost/usefulness under new horizons; retain ordinary/mate separation, real-ply normalization and searched witnesses. | SR-001/003/012/029/030/031/032. | [P1], [C1], [R001] |
| SR-012 | Remaining advanced TT policy | Production Pair-2 completed legal hints in the existing TTable; 64 MiB requested, equal-depth/current-generation/cutoff-only proof, NO_LEAF_TT, unchanged SearchKey identity; no production prefetch/statistics. | Information content, producer eligibility, hint retention/identity, replacement, sizing/layout, bound retention, q/selective domains, partial-work non-score evidence and measured traffic. | O1; SR-004/005/007/010/027/031/036/037. Binding changes need owner authority. | [P1], [C1], [Bagatur] |
| SR-013 | Tablebase integration into Search | Baseline optional guarded three/four-piece root WIN outcomes; no ordinary score/depth/TT substitution. | Broader materials/outcomes, root/interior WDL/DTZ, repeated-history/rule guards, probe economics and native-platform coverage. | SR-010/011/012/034/035/036; separate outcome/native ownership. | [P1], [C1] |
| SR-014 | Overall move-order architecture | Baseline legal hash, good/bad tacticals, main quiet history, with accepted completed-PV/root-effort guidance and Pair-2 completed legal TT hints. | Coherent hash/PV/refutation/history/tactical precedence, bad-tactical demotion and consumer interactions; ordering remains distinct from pruning. | O1/O2, SR-003/005/015/016/017/022/029. | [P1], [R005], [R036] |
| SR-015 | Capture/tactical ordering and SEE | Baseline SEE-sign partition and descending immediate material including EP/promotion; LVA/capture-history alternatives unadopted. | MVV-LVA, capture/noisy history, tactical tiers/thresholds, main/q ordering and reusable SEE evidence. | O2, SR-001/014/017/024/037; exchange units are not Pair-2 score units. | [P1], [R024] |
| SR-016 | Quiet-move ordering memory | Baseline signed bounded main history; per-invocation reset plus ROOT_REUSE private seeding. Richer histories unadopted. | Killers, countermoves, continuation/follow-up/relative/context/pawn/threat histories; gravity/update/decay, lifetime and label quality. | O2, SR-005/014/021/022/023/029/033/036; no labels for omitted moves. | [P1], [R005], [R023], [R036] |
| SR-017 | Move sorting and generation mechanics | Baseline staged generation/lazy selection, complete checked evasions and exact legal existence; full/hybrid sorts unadopted. | Full/partial/selection sorting, historical insertion/quick-sort crossover, generated-but-unused work, stage/setup and repeated classification costs. | SR-002/014/015/016/037; distinguish value parity from tree-shape identity. | [P1], [C1], [Bagatur] |
| SR-018 | Adaptive verified null-move pruning | Baseline bounded HCE fixed-R NMP; Pair-2 formulations P2 DEFERRED, not adopted. | Reduction/material/depth/surplus calibration, legal verification, zugzwang/threat evidence and recursive-probe economics. | O3, SR-009/010/019/029; synthetic isolation and ordinary-proof exclusion. | [R018], [P1], [C1] |
| SR-019 | Reverse futility / static-null pruning | Baseline HCE depth-two/margin-960 policy; Pair-2 disabled. Historical SeedV3 depth rule identified as conventional static-null. | Evaluator-specific margins/depths, improving/reliability and paid-eval reuse; no automatic HCE-to-neural threshold transfer. | O3, SR-009/018/020/021; ordinary/rule/mate guards and threshold predictions. | [P1], [C1], [Bagatur] |
| SR-020 | Razoring | Unadopted; P1 reduced/qsearch formulations scoped REJECTED. | Reduced and qsearch razors, margins, PV restrictions and quiet-resource safeguards on current leaves. | O1/O3, SR-001/009/018/019/021; actual-depth versus nominal evidence. | [P1], [C1], [Bagatur] |
| SR-021 | Futility-pruning family | Unadopted, P2 DEFERRED by resource choice; frozen/partial SHALLOW and DEEP studies retained. | Frontier/extended/deeper omission, static/reduction-depth margins, calibrated resources and composition with WINDOW/LMR. | O2/O3, SR-009/017/019/022/023/029; searched-prefix and proof dependencies. | [R021], [P1] |
| SR-022 | Late-move / move-count pruning | Production protected D1/D2 WINDOW; 6/10 actually searched quiets, original scout/unit-window guards. | Count/depth/history/improving modifiers, protection versus replacement and coherent later-move combinations. | O1/O2/O3, SR-014/016/021/023/024/029; omission/lifecycle guarantees. | [R022], [R036], [R024] |
| SR-023 | History-based pruning | Unadopted, P2 DEFERRED; REPLACE independent result inconclusive. | Quiet/capture/noisy history gates, confidence/count/age, sparse/censored labels and retained-history interactions. | O2, SR-005/015/016/021/022/029/033; history is not a bound. | [R023], [R005], [P1] |
| SR-024 | SEE-based move pruning | Production QXRAY for Pair-2 EFFORT1/ROOT_REUSE2..16; nominal quiet/capture pruning unadopted. | Depth-sensitive quiet/capture/q thresholds, safeguards, history/context composition and cheap shared SEE; retain adverse clearance resources. | SR-001/009/014/015/021/022/029/037. | [R024], [P1] |
| SR-025 | ProbCut and Multi-ProbCut | Unadopted; P1 scoped REJECTED. | Legal reduced/probabilistic probes, tactical filtering, calibrated margins, historical progressive/repeated Multi-ProbCut. | SR-001/008/009/012/018/029; complete probe cost; distinct from SR-026. | [P1], [C1] |
| SR-026 | Multi-Cut pruning | Unadopted; P1 scoped REJECTED. | Multiple reduced fail-highs, move diversity, correlated horizon error and sufficient-success/cost criteria. | SR-003/008/009/014/025/031; not a single-probe ProbCut or singular test. | [P1], [C1] |
| SR-027 | Enhanced Transposition Cutoff and TT look-ahead | Unadopted; P1 scoped REJECTED. | Applicable child proofs, look-ahead placement, retained information and saved work versus extra TT/cache traffic. | O1, SR-003/012/017/037; heuristic hits cannot supply exact cutoff authority. | [P1], [C1] |
| SR-028 | Alternative/historical forward-pruning screen | Evidence/unadopted P1 formulations; no generic alternative adopted. | Credible AEL, sibling prediction, uncertainty, parity/enhanced-forward families and new distinct hypotheses; RankCut conditional support relates to SR-038. | SR-008/009/020-027/029/038; avoid duplicating named families. | [P1], [C1] |
| SR-029 | Late-move reductions and re-search | Production protected scaled quiet LMR throughout accepted Pair-2 composition. | Curves, rank/history/context, prior reductions, improving and interaction-aware protection/removal; mandatory nominal verification retained. | O1/O2/O3, SR-003/007/014/016/021/022/023/024/033. | [R029], [R036], [R024] |
| SR-030 | Check extensions | Unadopted, P2 DEFERRED; FORCED/BOUNDARY confirmations closed. | Giving check versus checked/evasion extension, boundary/credit/depth controls, qsearch/LMR interaction and runaway cost. | SR-001/029/031/032; credit/path identity and real-ply rules. | [R030], [P1] |
| SR-031 | Singular-extension family | Unadopted; P1 scoped REJECTED, policy-cache/anchor evidence retained. | TT anchors/exclusion, ordinary/double/multiple/negative extensions, restricted-result identity and cost-controlled probes. | O1/O3, SR-007/008/012/026/029/033; hint availability is not score-anchor proof. | [P1], [C1], [Bagatur] |
| SR-032 | Other tactical/forced-line extensions | Unadopted, GPT-accepted P2 DEFERRED. RECAPTURE confirmation closed for futility; PROMOTION four-worker run interrupted before assessment. | Recapture, passed-pawn/promotion, one-reply, mate-threat/threat, history/hindsight, fractional extensions and total caps. | SR-001/010/011/017/029/030/031/033; retained default-OFF and partial evidence. | [R032], [P1] |
| SR-033 | Adaptive/hindsight depth feedback | Unadopted, P2 DEFERRED; MOVE1 closed for futility, CAUTION4 inconclusive. | Previous-reduction/outcome feedback, static movement and confidence relief/adjustment; distinguish feedback from predictive shortening and censored failures. | O2/O3, SR-008/009/016/023/029; legitimate labels and unchanged nominal verification. | [R033], [P1], [Bagatur] |
| SR-034 | Time management | Baseline bounded clocks with hard-stop/completed-iteration rules; forecast/stability policies unadopted. | Soft allocation, best-move/score stability, root effort, fail-high/low instability, easy moves, increment and resource redistribution. | SR-004/005/006/035/036; full trajectory/whole-game cost and hard-stop precedence. | [P1], [C1], [Bagatur] |
| SR-035 | Pondering and speculative opponent-turn Search | Pending opportunity; no pondering adoption. Historical nonterminal infinite-depth-ceiling concern remains unvalidated. | Predicted-move hit/miss reuse, protocol/cancellation, history/TT retention and resource scheduling; inspect old ceiling lead against current lifecycle before asserting a bug. | SR-005/010/012/034/036; immutable state transfer and drained ownership. | [P1], [P2], [R036] |
| SR-036 | Parallel Search architecture | Baseline inherited same-depth coordinator plus accepted Pair-2 ROOT_REUSE2..16; default one worker. | Root splitting/YBWC, Jamboree/ABDADA, independent-depth Lazy SMP, DTS, diversification/voting, hint sharing and private/shared-history alternatives. | O1/O2, SR-005/012/014/016/034/035; coherent publication/budget/drain semantics. | [R036], [R024], [P1] |
| SR-037 | Production Search hot-path mechanical optimization | Baseline primitive/reusable state and static-leaf legal-existence optimization. | Frame/move layout, branch/cache/JIT costs, incremental/reconstructed state, repeated SEE/eval/setup, justified prefetch alternatives and allocation removal. | SR-002/012/014/015/017; settled TT boundaries and independent semantic controls. | [P1], [C1], [Bagatur] |
| SR-038 | Learned or evaluator-assisted Search guidance | Pending opportunity; no generic learned policy/selectivity adopted. | Neural/policy ordering, learned uncertainty, evaluator-assisted confidence and RankCut conditional-support ideas when economical. | SR-008/009/014/016/028/029; core evaluator independence, labeling and held-out validation. | [P1], [P2] |

## Selection, combinations and progression

The accepted first feature-rich programme accounts for **all 38 subjects: 31
selected construction subjects, 3 preliminary/foundational subjects and 4 explicit
deferrals**. These dispositions do not replace historical research outcomes:

| Programme disposition | Subjects | Scope and dependency |
| --- | --- | --- |
| Workstream 1: bounded execution-architecture investigation | SR-002 | Technically complete and owner-accepted: flattened execution selected for subsequent Phase 3 development under the near-equal preference, with recorded limits. Recursive Policy 4 remains production default/fallback; independent references remain retained. |
| Workstream 2: bounded root-driver investigation | SR-003, SR-004 | Closed and owner-accepted: retain PVS on accepted flattened execution. Tested MTD variants have adverse costs and no confirmed strength benefit; comparison capability retained, TT policy unchanged. |
| Workstream 3: selected feature-rich construction | SR-001; SR-005; SR-006 through SR-012; SR-014 through SR-034; SR-036 | Technically complete after the explicit 20 October launch; all 31 dispositions and the active construction-v1 candidate are recorded above and in the focused record. Pending GPT/owner reconciliation and acceptance; no production promotion. |
| Deferred from first construction milestone | SR-013 | Broader tablebase integration; retain existing guarded functionality. |
| Deferred from first construction milestone | SR-035 | Pondering and speculative opponent-turn Search. |
| Deferred from first construction milestone | SR-037 | Dedicated production Search hot-path mechanical optimization research; normal implementation-level performance discipline still applies. |
| Deferred from first construction milestone | SR-038 | Experimental learned/evaluator-assisted Search guidance. |

SR-003 remains a foundation of the driver comparison; SR-002/SR-004 are resolved
by the preliminary investigations. They are not additional independent construction
features. The four deferrals remain available for later Phase 3 work or explicitly
authorized subsequent phases; none is a rejection. There are no other dedicated
preliminary research workstreams, including for composition architecture.

Construction retains appropriate accepted mechanisms and selects credible concrete
formulations for the remaining subjects without mandatory standalone Elo research
or owner approval for each routine implementation/parameter choice. The initial
configuration must meaningfully activate a broad compatible combination. SR-006
aspiration must support configuration-level enabled/disabled comparison where
compatible with the chosen driver. Validate interacting mechanisms in dependency-aware
groups, not only at final assembly; expose individual controls and meaningful
variants with immutable pre-request configuration, validated dependencies/exclusions
and reproducible identities under the Contract. Record every material selected-subject
exclusion, supersession, incompatibility or technical blocker explicitly. The 31
subjects are not 31 switches, new implementations or separate authorization gates.

Construction closes with a technically validated, reproducible experimental
configuration and its explicit composition/dispositions; superiority over Policy 4
is a separate question and must not hold construction open indefinitely. Freeze and
identify the whole candidate, then compare it against the Policy 4 champion under
controlled strength evidence. Follow with reverse ablations, interaction studies,
substitutions and configuration improvements, distinguishing harmful, redundant,
interaction-beneficial and inconclusive components without inferring universal value
from one configuration. Preserve the cumulative Phase 2 control and reproducible
feature controls. Independent confirmation and explicit acceptance remain necessary
before production promotion; these follow-on investigations need not await promotion.
Apply the Contract's resource, worker-domain, selection-bias and confirmation rules;
do not extend closed studies post hoc or enumerate configurations exhaustively.

**Current work selection:** reconcile the technically completed construction
handback with GPT/owner, refresh both Project Source mirrors and obtain explicit
acceptance/next-objective authority before dependent research. No in-scope technical
blocker remains. Preserve the frozen candidate, controls and negative/resource
evidence for that separately launched whole-configuration investigation. Workstream
4, production promotion and binding TT changes are not authorized. No SR is renumbered.

At objective closeout retain concise dispositions and dependencies here, with detail
in focused existing research locations. Distinguish technical completion, adoption,
owner acceptance and deployment. Unresolved small gains or resource-limited studies
may be DEFERRED; a failed formulation does not reject an entire subject. Return to
GPT/owner for reconciliation and selection, with both master revisions and mirror
refresh requirements. Do not automatically activate a successor.

## Historical references and initialization handback

The Contract records verified byte identities of both complete Phase 1 and final
Phase 2 archives. Historical ACTIVE/pending/mirror statements are preserved as
history, not current instructions; Phase 2 relative links retain their original
`source/` context. Detailed scopes and results remain in these archives, linked
reports, retained local evidence and source/Git history. Check local evidence
availability before reuse; no historical experiment is recertified here.

The initialization-era refresh wording is historical. The owner confirmed
P3-F004 / P3-R006 synchronization before production integration and has also
confirmed the P3-R007 / P3-F005 refresh. The SR-002 launch explicitly confirmed the
**P3-R008 / P3-F006** refresh after `fab5461`. The owner has now explicitly accepted
the SR-002 architectural recommendation and confirmed **P3-R009 / P3-F007**
synchronization. Those gates are satisfied; earlier pending wording is superseded.
The owner subsequently confirmed P3-R010/P3-F008 synchronization at the Workstream 2
launch. The owner confirms P3-R012/P3-F010 synchronization, accepts Workstream 2
and explicitly launches construction on 20 October. BOTH mirrors require refresh
to the current master revisions before dependent GPT planning or a subsequent
launch. Workstream 3 now awaits GPT/owner reconciliation and acceptance after
technical completion. Codex uses repository masters for authorized work;
repository edits do not synchronize external mirrors. Architectural acceptance
does not establish production adoption, playing-strength confirmation or deployment.

[P1]: ../docs/search-phase1/CHESS_SEARCH_RESEARCH_FRONTIER.md
[P2]: ../docs/search-phase2/CHESS_SEARCH_RESEARCH_FRONTIER.md
[C1]: ../docs/search-phase1/CHESS_SEARCH_CONTRACT.md
[C2]: ../docs/search-phase2/CHESS_SEARCH_CONTRACT.md
[Bagatur]: ../../BAGATUR_SEARCH_FRONTIER_ARCHITECTURE_ANALYSIS.md
[SR002]: ../docs/research/search/P3_SR002_EXECUTION_2026-10-19.md
[SR004]: ../docs/research/search/P3_SR003_SR004_DRIVERS_2026-10-19.md
[R001]: ../docs/research/search/SR001_QUIESCENCE_PHASE2_2026-10-09.md
[R005]: ../docs/research/search/SR005_ITERATION_REUSE_PHASE2_2026-10-13.md
[R006]: ../docs/research/search/SR006_ASPIRATION_PHASE2_2026-10-10.md
[R007]: ../docs/research/search/SR007_INTERNAL_ITERATIVE_PHASE2_2026-10-13.md
[R018]: ../docs/research/search/SR018_NULL_MOVE_PHASE2_2026-10-13.md
[R021]: ../docs/research/search/SR021_FUTILITY_PHASE2_2026-10-15.md
[R022]: ../docs/research/search/SR022_LMP_PHASE2_2026-10-11.md
[R023]: ../docs/research/search/SR023_HISTORY_PHASE2_2026-10-12.md
[R024]: ../docs/research/search/SR024_SEE_PRUNING_PHASE2_2026-10-14.md
[R029]: ../docs/research/search/SR029_LMR_PHASE2_2026-10-10.md
[R030]: ../docs/research/search/SR030_CHECK_EXTENSIONS_PHASE2_2026-10-12.md
[R032]: ../docs/research/search/SR032_TACTICAL_EXTENSIONS_PHASE2_2026-10-18.md
[R033]: ../docs/research/search/SR033_ADAPTIVE_DEPTH_PHASE2_2026-10-17.md
[R036]: ../docs/research/search/SR036_CONCURRENCY_PHASE2_2026-10-14.md
