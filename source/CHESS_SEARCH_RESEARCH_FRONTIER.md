# SeedV6 Search Research Frontier - Phase 3

Internal frontier revision: **P3-F002**

Status: **ACTIVE: first owner-selected CGLHW, Search Information Retention and Reuse (O1 + O2). No candidate promoted.**

Repository master: `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` in `C:/projects/seed/java/seedv6-unified`.

## Authority and current direction

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

Initial champion: accepted cumulative Pair-2 **QXRAY + quiet LMR + WINDOW**, with
**EFFORT at one worker / ROOT_REUSE at 2-16**, requested default 64 MiB TT and the
fixed generation-32 evaluator identity in the Contract. Other evaluators, independent
references, historical anchors and explicit controls retain their supported scope.
The owner confirms final Phase 2 mirror synchronization and GPT acceptance of
SR-032 as DEFERRED without adoption. No successor was authorized by that closeout.
The initialization began no research; the owner's 18 October 2026 launch now
authorizes the selected objective below.

The intended first direction is the **production, retention and reuse of useful
Search information**, especially TT move guidance and relevant history interactions.
The owner has selected O1 + O2 as that first coherent objective. Principal
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

## Active objective

**Search Information Retention and Reuse — O1 + O2**, owner-authorized 18 October
2026. Investigate current TT information production/availability/reuse and history
quality/lifetime jointly, with relevant ordering, iteration/retry, reduction,
pruning and private-worker interactions. Begin with measured current structures;
neither a TT redesign nor independent gains for each component are prerequisites.
The champion and fixed Pair-2 evaluator above remain the comparator and production
authority. Research-controlled alternatives do not change accepted policies.

The [focused research record](../docs/research/search/P3_INFORMATION_REUSE_2026-10-18.md)
is the detailed work-selection/resume entrypoint: bootstrap challenge, verified
implementation, prospective comparisons, evidence and unresolved dependencies.
It is evidence under this canon, not another governing contract. Work begins with
observation and controlled retention/learning contrasts; further consumers and
combinations follow decision value. O3 as an independent programme and unrelated
Frontier objectives remain unselected. Promotion requires Contract confirmation
and explicit GPT/owner acceptance; no successor activates automatically.

## Compact research inventory

**Production** means a retained accepted implementation, with scope stated.
**Baseline** identifies inherited functionality, not a new strength claim.
**Evidence only** is a conclusion without an adopted runtime mechanism.
**Unadopted** includes retained research implementations and historical formulations;
P1/P2 REJECTED or DEFERRED labels below describe those studies only. Every row has
an available Phase 3 investigation opportunity. The O1/O2 objective above selects
its relevant dependencies; it does not activate all 38 rows or pre-accept changes.

[P1] and [P2] point to complete frozen Frontiers: consult the same SR row, embedded
scope and linked technical reports. [C1] and [C2] supply detailed technical rules
only within the Contract's explicit inheritance boundary. R-numbers below link
focused Phase 2 reports. This inventory does not duplicate their experimental results.

| ID | Stable research subject | Existing production / historical disposition | Phase 3 opportunity, including embedded scope | Important dependencies | Evidence |
| --- | --- | --- | --- | --- | --- |
| SR-001 | Quiescence Search | Production Pair-2 qsearch, now QXRAY through SR-024; independent unpruned SR-001A retained. Other leaves/controls preserved. | Stand-pat, tactical ordering, SEE, qdepth/explosion limits, qTT, quiet checks/forcing limits, delta and post-static futility; combine credible variants without weakening checked evasions/rules. | SR-009/012/015/020/024/029/030; explicit q-domain identity. | [P1], [R001], [R024] |
| SR-002 | Recursive vs flattened Search mechanics | Recursive baseline; P1 flat/local-leaf candidate unadopted, scoped REJECTED. | Explicit frames/state machines, same-ply scratch and setup, stack/JIT/branch/locality costs under current composition. | Independent recursive oracle; SR-007/017/031/037. | [P1], [C1], [Bagatur] |
| SR-003 | Principal Variation Search / zero-window Search | Baseline exact PVS scouts/full re-search; TT-off ordered alpha-beta reference. | First-move quality, retry economics, exact/scout provenance and interaction with selective consumers. | SR-004/006/008/012/014/029; original-window and nominal verification rules. | [P1], [C1], [R029] |
| SR-004 | MTD(f) and memory-enhanced zero-window drivers | Full-window driver retained; P1 MTD formulations REJECTED, research controls retained. | First guesses, repeated-pass reuse, convergence/PV costs; related `MTD`/`NegaC*`/`SSS*`/`Dual*` drivers where useful. | O1, SR-003/005/006/012; explicit driver/outcome ownership. | [P1], [C1], [Bagatur] |
| SR-005 | Iterative-deepening information reuse | Production EFFORT/PV and ROOT_REUSE completed-root guidance; broader HISTORY/PVHISTORY unadopted. | Previous value/move/PV, root effort/stability, history retention across iterations/retries and request/new-game aging. | O1/O2, SR-006/012/014/016/034/036. | [R005], [R036] |
| SR-006 | Aspiration windows | Full windows retained; P2 six-formulation study scoped REJECTED, no adoption. | Initial windows, asymmetric/directional widening, volatility/parity and mate handling with better retained guidance. | O1, SR-003/004/005/012; full-request retries. | [R006], [P1] |
| SR-007 | Internal iterative search: IID and IIR | No production IID/IIR; P2 DEFERRED, controls retained. | Missing-hash/PV probes, shallow/near/wide eligibility, reduction versus acquisition cost under improved information reuse. | O1/O2, SR-003/005/012/014/029/031; same-ply restoration and actual-depth proof. | [R007], [P1] |
| SR-008 | Node/evidence model and path context | Evidence only; orthogonal facts/proof/provenance/prediction model binding. No generic role predictor adopted. | Actual scout/full/retry facts, assigned versus nominal depth, rank, previous reductions, parent/path and expected PV/Cut/All evidence with concrete consumers. | All selective consumers; SR-009/029/033/037. | [P1], [C1], [Bagatur] |
| SR-009 | Static-evaluation evidence and correction | Evidence only; bounded HCE findings. No correction-history, generic improving or confidence mechanism adopted. | Raw/cached/heuristic/proof separation, one/two-ply movement, residual calibration, sparse/biased labels, correction learning/aging/bounds and marginal cost. | O3, SR-008/018/019/020/021/029/033/038; evaluator identity. | [P1], [C1], [R018], [R021] |
| SR-010 | Repetition, cycles and graph-history interaction | Baseline formal real-history adjudication and primitive bucket-chain detection; conservative ordered SearchKey. | Broader proven count-state equivalence, history mechanics, upcoming repetition and clock/status relevance without twofold-as-draw or invented history. | SR-011/012/013/018/036; evaluator-relevant status. | [P1], [C1] |
| SR-011 | Mate-distance pruning | Baseline core mate-domain restriction. Finite-horizon gap proof retained; HORIZON_DOMAIN runtime candidate historically REJECTED. | Reassess domain cost/usefulness under new horizons; retain ordinary/mate separation, real-ply normalization and searched witnesses. | SR-001/003/012/029/030/031/032. | [P1], [C1], [R001] |
| SR-012 | Remaining advanced TT policy | Baseline TTable, 64 MiB requested, equal-depth/current-generation/cutoff-only proof, NO_LEAF_TT, shared score/move identity; no production prefetch/statistics. | Information content, producer eligibility, hint retention/identity, replacement, sizing/layout, bound retention, q/selective domains, partial-work non-score evidence and measured traffic. | O1; SR-004/005/007/010/027/031/036/037. Binding changes need owner authority. | [P1], [C1], [Bagatur] |
| SR-013 | Tablebase integration into Search | Baseline optional guarded three/four-piece root WIN outcomes; no ordinary score/depth/TT substitution. | Broader materials/outcomes, root/interior WDL/DTZ, repeated-history/rule guards, probe economics and native-platform coverage. | SR-010/011/012/034/035/036; separate outcome/native ownership. | [P1], [C1] |
| SR-014 | Overall move-order architecture | Baseline legal hash, good/bad tacticals, main quiet history, with accepted completed-PV/root-effort guidance. | Coherent hash/PV/refutation/history/tactical precedence, bad-tactical demotion and consumer interactions; ordering remains distinct from pruning. | O1/O2, SR-003/005/015/016/017/022/029. | [P1], [R005], [R036] |
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

Select a coherent objective by plausible whole-engine strength, architectural
leverage, informative evidence, dependencies, implementation maturity and resource
value. Record its scope, champion/configuration identity, owner decisions and
acceptance boundaries here when separately authorized. Newly discovered dependencies
may be investigated within that objective; unrelated objectives need a new launch.
New top-level identifiers or material subject-scope changes require a GPT/owner
programme decision; do not renumber existing subjects.

The Contract supports both evidence-guided foundations/combinations and integrated
candidate optimization. Useful prospective contrasts include guidance + aspiration,
retained history + pruning, evaluation evidence + depth adjustment, and TT retention
+ repeated zero-window drivers. Baseline/foundation/consumer/combination controls
can separate interactions when informative; they are not a mandatory tournament
matrix or a requirement for positive standalone Elo before composition.

Build capabilities and validate semantics incrementally, then select coherent
configurations. Pair/triplet/larger combinations, removals, substitutions and
economical stochastic/evolutionary exploration are permitted within scope. A
feature-rich candidate may include individually neutral or apparently negative
components. It still needs whole-candidate confirmation and explicit acceptance
before replacing the champion; mere construction changes no production status.

After promotion, normally compare against the new champion. Use reverse ablation
and mutations to distinguish harmful, redundant, interaction-beneficial and uncertain
components without claiming universal value. Preserve older champions and feature
controls with reproducible identities. Account for selection bias/multiplicity,
independent confirmation, worker domains and resource cost under the Contract;
avoid exhaustive configuration enumeration and post-hoc extension of closed studies.

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

The initialization-era P3-F001 / P3-R001 refresh wording is historical. The owner
confirms both current predecessor mirrors (P3-F001 / P3-R002) synchronized and
selects O1 + O2 in this launch. **P3-F002 / P3-R003 now require replacement of BOTH
Project Source mirrors and synchronization confirmation before dependent GPT
planning or a subsequent CGLHW launch.** Codex continues this authorized objective
from repository masters and does not claim to perform external synchronization.

[P1]: ../docs/search-phase1/CHESS_SEARCH_RESEARCH_FRONTIER.md
[P2]: ../docs/search-phase2/CHESS_SEARCH_RESEARCH_FRONTIER.md
[C1]: ../docs/search-phase1/CHESS_SEARCH_CONTRACT.md
[C2]: ../docs/search-phase2/CHESS_SEARCH_CONTRACT.md
[Bagatur]: ../../BAGATUR_SEARCH_FRONTIER_ARCHITECTURE_ANALYSIS.md
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
