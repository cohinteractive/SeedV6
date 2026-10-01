# SeedV6 Search Research Frontier

Internal frontier revision: **F048**

Status: **Active Search research-frontier canon; open-ended by design.**

Repository master: `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` in the current
`SeedV6` repository.

## Authority and purpose

The repository copy is the authoritative master. A ChatGPT Project Source copy,
when present, is a manually refreshed mirror, not an independent authority.

[CHESS_SEARCH_CONTRACT.md](CHESS_SEARCH_CONTRACT.md) remains authoritative for
actual Search purpose, semantics, architecture, principles, settled decisions
and **OPEN** architectural questions. This frontier is authoritative only for
which research subjects have been deliberately admitted to the programme, their
research disposition, and their selection and feature-closeout workflow within
granted authority. Listing an item does not approve it for SeedV6 Search. The
frontier must never override or silently modify the Search Contract.

This is a compact inventory of agreed research subjects and their eventual
dispositions, not a Search implementation specification, roadmap, worklog,
benchmark ledger or replacement for the Search Contract. Exact negamax/ordered
alpha-beta and the current initial TT programme are not retroactively listed:
their durable state is already represented by the Search Contract. This initial
frontier represents work still ahead. The inventory is intended to be exhaustive
for currently known worthwhile Search research, and deliberately extensible when
new credible ideas are discovered.

## Stable IDs and selection policy

IDs SR-001 through SR-038 are stable identifiers only. Numeric order is **not**
implementation priority or research order. Never renumber existing entries
merely because priorities change. Dependencies and relationships are advisory
planning information, not a fixed sequence.

When explicitly launched with programme-level autonomous authority, Codex may
coordinate the remaining admitted Search programme as a sequence of separate
feature work units. That launch authorizes next-feature selection without a
fresh GPT conversation or human choice at each boundary; this canon's presence
alone does not launch the programme or expand a feature-specific task.

Before the first selection and at every feature boundary after closeout, Codex
must re-read the current repository `CHESS_SEARCH_CONTRACT.md` for settled Search
state and `CHESS_SEARCH_RESEARCH_FRONTIER.md` for admitted research coverage and
dispositions. Select the logically strongest next eligible unresolved admitted
feature from that current durable state, considering:

- Prerequisite maturity/correctness.
- Enabling value for later work.
- Expected playing-strength or useful-depth impact.
- Ability to measure the subject cleanly.
- Risk of designing later mechanisms against temporary assumptions.
- Architectural cohesion.

Next-work selection is dynamic, not a predetermined sequence. Do not choose the
lowest pending ID merely because it is first, or blindly follow a queue chosen
earlier: the just-closed feature may change dependencies, evidence or ordering.
Eligibility follows the recorded scope, disposition, dependencies and authority,
not a status label alone. Existing accepted evidence without production adoption
does not by itself authorize a new mechanism. Preserve the reconsideration rules
below; proximity to other work or conventional engine practice does not reopen
REJECTED work.

## Autonomous feature cycle and programme boundary

Programme-long authority does not merge the frontier into one undifferentiated
task. Complete one coherent, independently reviewable feature work unit before
starting another:

1. Establish the selected feature's research question, scope, relevant existing
   contracts and evidence, dependencies, and applicable acceptance/rejection
   criteria.
2. Perform the necessary discovery, experiments and prototypes; challenge
   candidate conclusions with appropriate counterevidence and further research.
3. Reach an evidence-supported disposition from the complete researched evidence
   under the Search Contract and the status/maintenance rules below.
4. Implement/adopt an accepted result where justified within that feature's
   authority, and appropriately validate the research outcome and any adopted
   changes. Preserve the contract's correctness/reference and evidence standards.
5. Update durable Contract and frontier state as required, including the concise
   disposition, material dependencies and any remaining work or conditions. Keep
   ACCEPTED distinct from IMPLEMENTED; an ACCEPTED or DEFERRED closeout must
   account for incomplete adoption or research rather than conceal it.
6. Clean or reconcile temporary research changes/artifacts as applicable,
   preserving required evidence and unrelated work. Inspect repository/Git state
   and complete feature closeout with the outcome, validation, limitations and
   required actions recorded/reported as applicable.

Only after that closeout may Codex select and start another feature, first
re-reading both updated repository masters. Implications or newly established
dependencies for other features may be recorded while closing the current one;
those other features must not be started early. The Contract and frontier remain
the durable programme state; no separate orchestration document, queue or ledger
is required.

Continue through ordinary research difficulty, failed hypotheses, rejected
candidates, surprising results, additional experiments, debugging and reselection.
If a prerequisite requires another eligible admitted feature, record a legitimate
deferral and closeout before reselection. A failed candidate or one completed
feature is not programme completion while eligible frontier work remains.

Stop for external input when useful continuation cannot safely proceed under
current authority: a required owner/programme/design decision; conflicting
authoritative directions; a proposed change outside granted authority to a
LOCKED decision or programme scope; a necessary operation outside Codex's
capability/authority; unavoidable manual action blocking useful further work;
or scope ambiguity that would choose a materially new programme direction.
Do not use deferral or a canon edit to bypass such a blocker. Report the blocked
decision/action and preserve enough durable state for continuation.

The loop ends when no logically eligible unresolved admitted feature remains
under current authority, or a genuine blocker requires external input. At return,
distinguish completion of the applicable programme from exhaustion of currently
eligible work with unresolved/deferred subjects or external decisions remaining.
Feature closeout and programme return do not assert GPT/user acceptance or any
deployment completion beyond the granted authority.

## Statuses

| Status | Meaning |
| --- | --- |
| PENDING | Research is admitted to the frontier but has not reached an accepted conclusion. |
| ACTIVE | Research is currently in progress. |
| ACCEPTED | Research has reached an accepted conclusion favouring incorporation into SeedV6 Search, but implementation/adoption is not yet fully complete. |
| IMPLEMENTED | The accepted result has been implemented and accepted. Any material durable Search-design consequence must also be represented in CHESS_SEARCH_CONTRACT.md under that file's own maintenance rules. |
| REJECTED | The subject was deliberately researched and the accepted conclusion is not to incorporate it under the researched conditions. |
| DEFERRED | Research has not reached a final include/exclude conclusion and has deliberately been postponed, normally because a prerequisite, evidence source or better timing is needed. |

Interpret research conclusions against Search maturity where dependencies affect
value. Distinguish a feature justified for the current baseline, a feature not
justified under current tested conditions, and a feature deliberately REJECTED
for its stated researched scope. Current-baseline non-retention is evidence for
those conditions, not automatically a universal or permanent rejection.

Reconsider non-retained features only when a later accepted architectural or
mechanical change plausibly alters their cost/benefit or evidentiary role. For
ordering heuristics, relevant changes might include PVS, LMR, history-based or
move-count pruning, richer iterative-deepening information or final move-picking
mechanics; these are examples, not fixed prerequisites. Do not periodically
retest rejected or non-retained features without a concrete dependency change.
A REJECTED item remains rejected for its stated researched scope unless later
evidence or changed conditions justify reopening it through an explicit
programme decision. These rules do not change status meanings or reopen settled
items.

## Maintenance rules

- Retain every admitted entry permanently, including IMPLEMENTED and REJECTED
  entries, so historical coverage is never lost. Normally do not delete items at
  all; terminal statuses preserve coverage.
- Every content-changing frontier update increments the internal `Fxxx` revision
  exactly once and appends a concise revision-history entry. Routine prose
  formatting alone should not create gratuitous revision churn.
- Outside an explicitly authorized autonomous Search programme run, Codex may
  mechanically update the status, dependency/relationship field and concise
  disposition of an existing entry when the accepted programme outcome is already
  established by the task/context.
- During an explicitly authorized autonomous Search programme run, Codex may
  establish and record the selected feature's researched disposition itself:
  ACTIVE during research, and ACCEPTED, IMPLEMENTED, REJECTED or DEFERRED at
  legitimate feature closeout, consistently with the status definitions and
  Search Contract. No separate GPT/human acceptance step is required for a
  feature disposition within that grant. The conclusion must follow from
  sufficient, complete researched evidence and applicable acceptance/rejection
  criteria. Never infer a closeout disposition merely because code compiled,
  tests passed, one benchmark improved, node count fell, a conventional engine
  uses the technique, it is generally regarded as standard, or Codex personally
  prefers it. ACCEPTED does not establish completed implementation/adoption;
  IMPLEMENTED requires the accepted result to be implemented and validated.
- Adding a new research item; deleting an item; merging or splitting entries;
  renaming in a way that materially changes scope; or materially redefining an
  existing subject requires an explicit programme decision, unless existing
  canon already explicitly authorizes that particular change. Autonomous feature
  disposition authority is not authority to invent or materially redefine the
  programme, deliberately overturn LOCKED programme-level principles, or reopen
  REJECTED items outside the existing evidence and programme-decision rules.
- If research exposes a previously unknown dependency or relationship, it may be
  recorded when established by the task or evidence-supported autonomous feature
  research. Recording it does not start or reopen the related feature.
- A frontier change never by itself authorizes Search implementation or changes
  Search semantics. An explicit autonomous programme launch grants feature
  research, justified implementation/adoption and required canon maintenance
  within the boundaries above. If an evidence-supported accepted outcome
  materially changes durable Search semantics, architecture, policy or accepted
  direction, maintain `CHESS_SEARCH_CONTRACT.md` under its own authority and
  revision rules. Outside that programme grant, if a task does not authorize
  required Contract maintenance, report it rather than silently editing the
  contract. Neither grant permits overriding LOCKED decisions outside established
  authority; escalate required owner/programme decisions.
- Keep dispositions short. Do not embed detailed research results, benchmark
  histories, experiment logs, implementation notes, commit lists or conversation
  summaries here. Durable accepted architectural conclusions belong in
  `CHESS_SEARCH_CONTRACT.md`.
- After every repository frontier update, completion must report
  `FRONTIER_UPDATED: <old revision> -> <new revision>` and
  `PROJECT_SOURCE_REFRESH_REQUIRED: YES`.
  During an autonomous multi-feature run, Project Source mirrors may lag without
  blocking inter-feature continuation; use the current repository masters for
  every subsequent selection and research cycle. At an external handback,
  blocker or final programme return where GPT/user coordination resumes, require
  the human to refresh both Search Project Source mirrors from the current
  repository masters before GPT reasons from them again, including construction
  of a further programme launch prompt. Report that outstanding action and the
  current revisions. A requested refresh is not proof of completion; confirm
  synchronization before subsequent reliance on the mirrors.

## Foundational / Search architecture

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-001 | Quiescence Search | REJECTED | qsearch semantics; stand-pat; captures, promotions, checks/evasions; in-check handling; capture ordering; SEE; delta pruning; qsearch futility; TT participation; repetition/draw handling; explosion/depth controls. | Foundational frontier semantics; interacts with tactical ordering and later selective pruning. | A correct unpruned captures/en-passant/promotions plus checked-evasions reference was established but produced severe tree expansion. Researched SEE pruning, fixed qdepth, exact qTT, quiet-check participation/control, material-delta and post-static futility did not establish a sufficiently correct and efficient production policy. Retain static leaves for current production; preserve qsearch research/reference evidence without production authority. Reconsider only after a concrete material dependency or Search-architecture change plausibly alters the result. |
| SR-002 | Recursive vs flattened Search mechanics | REJECTED | Recursive negamax versus explicit flat/iterative Search frames or state machines; JVM/JIT effects; stack/call overhead; branches; locality; maintainability of exact semantics. | Exact-reference path must remain available; mechanical decision, not selective Search policy. | Semantically equivalent primitive flat Search was researched against recursive production Search. The strongest bounded candidate, refined local-leaves, remained modestly slower with no convincing performance benefit under the researched Java 21/Ryzen 5 5500 conditions, adding explicit frame/state-machine complexity without a meaningful allocation advantage. Retain recursive mechanics for the current production baseline; this is not universal inferiority of flattened Search. Reconsider only after a concrete material dependency, platform or Search architecture change plausibly alters the result. |
| SR-003 | Principal Variation Search / zero-window Search | IMPLEMENTED | PVS/NegaScout structure; first-move full windows; scout searches; fail-soft behaviour; re-search rules; TT and ordering interaction. | Benefits from strong move ordering; relevant to LMR and other probe searches. | PVS implemented in production/default TT-enabled ExactSearch under accepted ordering/TT architecture; ordered alpha-beta retained for TT-off reference/oracle execution. Exact fail-soft scout/full-window re-search semantics established. LMR and other downstream narrow/selective uses outside SR-004/SR-006's rejections remain separate research; SR-001 records qsearch's disposition. |
| SR-004 | MTD(f) and memory-enhanced zero-window drivers | REJECTED | MTD(f) versus PVS/alpha-beta; first-guess quality; repeated zero-window passes; TT dependence; iterative-deepening interaction; PV handling; related MTD/NegaC\*/SSS\*/Dual\* ideas where useful. | Depends strongly on mature TT semantics and zero-window behaviour. | Rejected for current production/default Search under researched single-thread HCE conditions. Unrestricted previous-score MTD regressed severely; exact/oracle guesses demonstrated theoretical economic headroom. Two-pass/full-window fallback and directional bracketing/bisection controlled convergence pathology but still materially regressed versus full-window PVS. Materialization was negligible; no practical MTD/zero-window root driver is retained. This is not a universal rejection: reconsider only after an explicit programme decision following a concrete dependency/architecture change plausibly improving practical first-guess quality or repeated-zero-window/TT economics, not periodic retesting or nearby parameter tuning. |
| SR-005 | Iterative-deepening information reuse | REJECTED | Previous PV, previous best move/value, iteration stability and information transfer beyond the already-settled simple successive-depth driver. | Same-request TT/hash-move reuse; completed-iteration data for separate SR-006 aspiration and SR-034 time-management research. | Explicit previous-PV/best-move ordering added no information beyond same-request TT/hash-move reuse in the researched architecture. All available candidates matched the legal TT move already searched first, including materially deeper normal depth-10 Search pressure; no fallback opportunity or TT/PV disagreement was observed. Previous completed score and best-move/score stability remain data for SR-006/SR-034, without active SR-005 Search authority. Reconsider only after a material dependency or architecture change plausibly alters TT retention or this information relationship. |
| SR-006 | Aspiration windows | REJECTED | Initial window; fail-low/high widening; asymmetric widening if justified; score volatility; mates; interaction with PVS/MTD(f). | Iterative deepening and stable previous-score evidence. | Previous-score predictiveness justified research under single-thread/HCE conditions, but fixed ASP-512 regressed and ASP-628 saved only about 0.25% nodes with inconclusive wall-time evidence. The researched stable/volatile adaptive policy also regressed. No aspiration policy is retained in current production; full-window successive-depth SearchDriver behaviour remains. Reconsider only after a concrete material dependency or Search-architecture change plausibly alters the result. |
| SR-007 | Internal iterative search: IID and IIR | REJECTED | Internal iterative deepening; internal iterative reductions; missing-hash/PV situations; cost versus ordering/depth benefit. | Mature TT, ordering and iterative PVS; static-horizon resource quality and actual-depth proof remain distinct from missing-hash evidence. | Exact D-2/D-1 IID probes, non-scout/deeper-floor and completed-history variants did not establish repeatable wall-time benefit. R=1 IIR retained false cutoffs and deeper costs; same-parity R=2 had bounded savings but lost rook/pawn resources across deeper horizons. Preserve nominal production and SR-018/SR-019; no internal mechanism or experimental state adopted. Reconsider only after a concrete dependency or mechanism-specific evidence change addressing information, economics or resource failures, not nearby depth tuning or node savings; no Elo claim. |
| SR-008 | Node/evidence model and path context | ACCEPTED | PV/Cut/All or alternative node characterization; expected cutoff status; parent/path evidence; move rank; previous reductions; improving/worsening and authoritative versus heuristic evidence. | Foundational input to coherent selectivity; static-eval reliability/correction follows SR-009's accepted disposition. | Orthogonal invocation/proof/provenance/predictive evidence model accepted; authoritative entry-time PV/Cut/All roles not adopted. PVS provenance and searched move ordinal demonstrated useful predictive evidence under single-thread HCE conditions. Limited path context can add conditional information, but no generic carried context or expected-cutoff predictor is justified. Static-eval reliability/correction follows SR-009's accepted disposition; no selective mechanism or production Search change is authorized. |
| SR-009 | Static-evaluation evidence and correction | ACCEPTED | Raw versus Search-adjusted eval; TT eval; improving/worsening; correction-history-style mechanisms; reliability/context/uncertainty signals; evaluator independence versus justified evaluator-specific evidence. | Evidence for later pruning, reductions and depth decisions; each mechanism must independently justify interpretation, eligibility, economics, safeguards and aggressiveness. | Accepted evidence-model/disposition conclusion, with no production mechanism adopted. Raw static eval is predictive heuristic evidence, not deeper proof; residual behaviour is horizon/context dependent. Coarse tactical presence earned current-HCE predictive standing; SEE sign added no stable reliability refinement. One-ply movement and conventional binary improving are not adopted as general evidence; continuous two-ply movement remains bounded research evidence. No universal confidence scalar or reliability boolean is justified. Held-out keyed signed residual predictability exists, but correction-history implementation is deferred/not adopted because evaluator cost, biased/limited natural supervision, unresearched learning mechanics and absence of a justified consumer at SR-009 closure prevented adoption. Reconsider only after a concrete material dependency changes economics or creates a justified consumer, not periodic retesting or tuning. Preserve evaluator independence and require neural validation before generalizing HCE numerical findings; later mechanisms must independently justify use and runtime state. |
| SR-010 | Repetition, cycles and graph-history interaction | IMPLEMENTED | Seed-style key-history scanning; twofold Search repetition versus formal game adjudication; upcoming repetition; rule-50; GHI; TT/SearchKey interaction and performance. | Fixed-depth value equivalence, evaluator status, TT/PVS/mate and SR-018 synthetic-domain composition. | Established uniform formal threefold across real prefix/root/legal path, with nonterminal twofold cycles and count-state GHI equivalence. Implemented exact primitive bucket-chain scanning after equivalent-tree measurement; retained full status and the conservative ordered SearchKey because broader-key benefit was insufficient at production table size. No upcoming-repetition shortcut, horizon clock masking or TT redesign; SR-018/SR-019 unchanged. |
| SR-011 | Mate-distance pruning | IMPLEMENTED | Early alpha/beta tightening or return from already-proven superior mate distance; exactness; mate bands; ply-normalized TT interaction. | Verified hard ordinary/mate separation, real-ply versus static horizon, original-window TT/PVS witnesses and SR-018/SR-019 composition. | Implemented exact post-terminal core extrema with the depth-one ordinary lower bound and return-only generic bounds. Preserved oracle values, ordinary TT precedence/no bound upgrade, searched endpoint PVs and selective provenance/eligibility. Finite-horizon gap rules are mathematically settled, but the separate runtime candidate is rejected because it added no work savings and did not earn its cost. Repeatable mate-workload wall gains and approximately neutral ordinary economics support core adoption; no strength claim or next feature. |
| SR-012 | Remaining advanced TT policy | IMPLEMENTED | Current single-thread advanced TT policy: deeper-for-shallower evidence, non-cutting bound tightening, cross-generation reuse, score versus move identity, replacement, sizing, statistics and prefetch. | Builds on the initial TT programme; parallel/shared TT is owned by and deferred to SR-036, not a blocker to this single-thread outcome. | Current single-thread advanced TT policy implemented: strict equal-depth/current-generation cutoff-only evidence, existing replacement/hash-move/shared-identity mechanics, NO_LEAF_TT, no prefetch or production statistics, and 64 MiB requested fixed default; parallel/shared TT deferred to SR-036. |
| SR-013 | Tablebase integration into Search | IMPLEMENTED | Root/interior probing; WDL/DTZ; rule-50; score domains; TT interaction; probe depth and effects on selectivity. | Mature terminal/history/value and TT semantics; separate root game-outcome provenance and process-serialized native ownership inform later lifecycle/parallel consumers. | Adopted optional verified three/four-piece root WIN decisions for managed UCI/Play, with conservative full-prefix/rule-clock guards and no nominal score/depth or TT substitution. Independent probes, complete defending-reply certificates and conversion tests support bounded value; default/exact/training paths remain independent. No interior probing, broader outcomes/materials or Elo claim; other native platforms remain unverified. Evidence: C:/Users/Central/Documents/SeedV6-SR013-2026-10-01/REPORT.txt. |

## Move ordering / Search information

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-014 | Overall move-order architecture | IMPLEMENTED | Coherent ordering pipeline across hash/PV/tactical/refutation/history/bad-capture classes; staged versus monolithic ordering; ordering as Search evidence. | Major prerequisite/enabler for PVS, LMR and several pruning methods. | Legal hash first, then SR-015 SEE-good followed by SEE-bad tacticals, each by descending immediate material, then SR-016 main quiet history; physical staged/lazy mechanics follow accepted SR-017. Ordering evidence only, not pruning. Implemented in production/default TT-enabled ExactSearch. |
| SR-015 | Capture/tactical ordering and SEE | IMPLEMENTED | MVV-LVA versus SEE; good/bad captures; promotions; capture history; SEE thresholds; tactical ordering in normal Search and qsearch. | qsearch, move ordering and SEE pruning. | Legal hash once, then SEE >= 0 good tacticals before SEE < 0 bad tacticals, all ahead of quiets; descending immediate capture value (including EP) plus promotion gain within each tactical class. LVA and the researched compact capture history are not retained in the current baseline. Ordering only, not pruning; mechanics remain SR-017. Implemented in production/default TT-enabled ExactSearch. |
| SR-016 | Quiet-move ordering memory | IMPLEMENTED | Killer moves; history; relative history; countermove; continuation/follow-up histories; refutations; context/pawn/threat histories; update/decay/gravity policy. | Overall ordering; LMR and history-based pruning. | Main quiet history alone accepted as the current quiet-ordering baseline: moving piece including side + from + to; completed searched quiet beta-cutoff winners rewarded and earlier searched quiets penalized by bounded gravity updates. Immediate continuation history, two per-ply killers and compact countermoves researched but not retained under tested conditions; relative/richer histories unproven, not required for closure. Ordering only, not pruning/reductions; broader quiet-history lifecycle remains OPEN and sorting mechanics remain SR-017. Implemented in production/default TT-enabled ExactSearch. |
| SR-017 | Move sorting and generation mechanics | IMPLEMENTED | Seed's historical insertion-sort <=16 plus quicksort above that; full sort versus selection/partial/staged/lazy move picking; incremental generation; data/branch/JIT consequences. | Mechanical implementation of the ordering architecture. | Lazy selection preferred over full insertion/handcrafted sorting; staged tactical/quiet generation over full generation, with complete checked-node evasions and exact static-leaf legal-existence detection, subsequently streamlined by SR-037. Deferred quiet history is sampled when quiets materialize, then fixed; exact value is preserved, not necessarily the tree. Primitive handcrafted hybrid Sort and bounded crossovers were researched but not retained as the current baseline. Implemented in production/default TT-enabled ExactSearch. |

## Forward pruning / Selectivity

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-018 | Adaptive verified null-move pruning | IMPLEMENTED | Adaptive NMP reduction; depth/material/static-eval conditions; zugzwang risk; verification search; recursive NMP restrictions; threat evidence. | Node/eval evidence; adds a positive-depth raw-E consumer relevant to possible later correction reconsideration; local synthetic-history isolation composes with SR-010 ordinary legal-history semantics. | Implemented the evidenced HCE-only actual-scout depth-4..6 fixed-R=2 policy with E-beta >=512, own non-pawn material, ordinary-window and rule-50 horizon guards. Synthetic probes exclude ordinary Search TT, repetition across the pass, nested NMP/SR-019 and quiet-history writes. Accepted predictions return beta with no synthetic PV and suppress affected legal TT stores; discarded probes do not taint legal proof. Reduced verification retained errors; nominal verification lost the economics; adaptive reduction did not stably outperform the fixed baseline. SR-019 remains unchanged outside composition. Broader depths, neural calibration and recursive/selective relaxation require new evidence; no Elo or universal safety claim. |
| SR-019 | Reverse futility / static-null pruning | IMPLEMENTED | Depth-conditioned static-eval early returns; conventional RFP/static-null pruning; adaptive margins; improving/eval reliability; mate safeguards. Explicitly determine whether the user's historically successful depth-based static-eval return is identical to or distinct from conventional RFP. | SR-008/SR-009 evidence; establishes a positive-depth raw-E consumer relevant to possible later correction reconsideration, without reopening correction research. | Historical SeedV3 used conventional shallow static-null pruning; its linear depth policy is not adopted. Implemented the evidenced HCE-only depth-two, non-check actual-scout variant with margin 960, ordinary-window/rule-50 guards and beta return. Predictive returns and affected ancestors cannot store mathematical TT evidence; completed results carry selective provenance. Exact constructors/oracle and neural defaults remain unpruned. Wider depths, neural calibration and other selective expansion require new evidence and explicit adoption; bounded gains do not establish playing strength or universal pruning safety. |
| SR-020 | Razoring | REJECTED | Reduced-depth or qsearch-based razor forms; margins; PV restrictions; mate safeguards; relation to qsearch/futility. | Static-leaf HCE, original-history nominal/probe evidence and SR-018/SR-019 composition; SR-001 default-qsearch rejection remains intact. | Reduced/qsearch forms retained quiet-mate and nominal-resource failures; narrower, wider-margin and quiet-check-guarded forms did not earn repeatable complete iterative cost. Preserved small positive timing evidence and unchanged historical best moves without inferring proof, root harm or Elo. Production unchanged; reconsider only with concrete evidence/dependency changes under Contract conditions. Evidence: C:/Users/Central/Documents/SeedV6-SR020-2026-10-01/. |
| SR-021 | Futility-pruning family | REJECTED | Frontier, extended and deeper futility; move-level alpha-side prediction; tactical/check/promotion exclusions; margins and static-leaf economics. | SR-008/SR-009 evidence, SR-017 mechanics, mathematical TT dependency and SR-018/SR-019 composition; no qsearch prerequisite or other frontier mechanism adopted. | Researched clean later quiets at parent depths one and two, with alpha >= 0 and independently frozen fixed margins. Smaller margins missed substantial passed-pawn/king resources; conservative D1-512 and D2-1024 separated sampled predictions but did not establish repeatable wall-time benefit across fresh suites or beyond concentrated winners. Preserved alpha-threshold return, local dependency/discharge and searched-only history findings as external research evidence; production remains unchanged nominal-depth PVS with SR-018/SR-019. Reconsider only after a concrete evidence or dependency change, not nearby margins, broader formulas or node savings alone; no Elo claim. |
| SR-022 | Late-move / move-count pruning | REJECTED | Pruning sufficiently late moves entirely; move-count formulas; history/improving/depth modifiers; tactical safeguards. | Mature staged ordering/main history; actual searched ordinals, nominal resource quality and mathematical TT proof. SR-029/SR-024 results supply evidence without reopening those features; SR-023 remains separate. | Shallow actual-scout quiet count policies exposed persistent local resources; D1/combined driver economics regressed, while modest D2 headroom failed an entirely fresh two-control holdout even after validated primitive quiet-check refinement. No count policy/helper adopted; retain nominal PVS and SR-018/SR-019. Reconsider only with concrete dependency or mechanism-specific evidence addressing resources and complete iterative cost, not nearby count sweeps or nodes alone; no Elo claim. Evidence: C:/Users/Central/Documents/SeedV6-SR022-2026-10-01/. |
| SR-023 | History-based pruning | REJECTED | Quiet/capture/noisy history as pruning evidence rather than merely ordering/reduction evidence. | Accepted main quiet ordering history and current lifecycle; nominal omission/proof obligations. Capture/noisy consumers require separate update and total-cost justification; other pruning/ordering rejections remain closed. | Broad shallow quiet omission gave modest concentrated iterative savings with mixed finite-horizon resources, while stronger/shallow safeguards and capture consumers failed fresh complete-driver economics after primitive-mechanics and two-control challenges. Retain ordering history and production semantics; no pruning table/helper adopted. Reconsider only with concrete joint resource/cost evidence or an accepted relevant dependency change, not nearby thresholds or ordering success; no Elo claim. Evidence: C:/Users/Central/Documents/SeedV6-SR023-2026-10-01/. |
| SR-024 | SEE-based move pruning | REJECTED | Depth-sensitive SEE thresholds for quiets and captures and their interaction with other pruning. | Validated SR-015 tactical SEE, SR-017/SR-037 mechanics, nominal static horizons and mathematical TT proof; separate from SR-021/SR-029 rejections. | Capture and broad quiet policies failed prediction/economics; refined D2..4 quiet pruning showed isolated wall savings and no demonstrated sustained resource harm, but fresh actual iterative-driver tests were flat or slower, including child-state reuse. No pruning or quiet-SEE API adopted; retain ordering, nominal PVS and SR-018/SR-019. Reconsider only with concrete dependency or mechanism-specific evidence addressing complete iterative value versus cost, not threshold sweeps or nodes alone; no Elo claim. Evidence: C:/Users/Central/Documents/SeedV6-SR024-2026-10-01/. |
| SR-025 | ProbCut and Multi-ProbCut | REJECTED | Probabilistic/reduced searches around bounds; tactical move filtering; depth/margins; historical Multi-ProbCut used in prior Seed engines; progressive/repeated variants where relevant. | Legal actual-depth TT/probe evidence; current static-leaf HCE calibration; distinct from SR-026. Reconsider only under Contract evidence/dependency conditions. | Reduced probes had real savings but retained resource/horizon failures; exact verification lost economics and wider-margin confirmation did not earn robust incremental value. Positive refined root evidence preserved; production unchanged. Evidence: C:/Users/Central/Documents/SeedV6-SR025-2026-10-01/REPORT.txt. |
| SR-026 | Multi-Cut pruning | REJECTED | Reduced searches of several promising moves where enough fail-highs justify pruning. | Mature ordering/PVS and legal actual-depth probe/oracle evidence; distinct from SR-025 and singular extension. Reconsider only under Contract evidence/dependency conditions. | Multiple successes and distinct moving pieces retained correlated resource/mate failures; nominal verification and stronger margins lost complete iterative economics. Positive evidence retained; production unchanged. Evidence: C:/Users/Central/Documents/SeedV6-SR026-2026-10-01/REPORT.txt. |
| SR-027 | Enhanced Transposition Cutoff and TT look-ahead | REJECTED | Probing candidate children or related TT look-ahead before normal expansion; node savings versus additional TT traffic. | Exact-depth/current-generation TT, SR-017 phase boundaries, PVS and SR-018/SR-019 composition. | Exact child UPPER/EXACT evidence supplied genuine later-candidate parent fail-high proofs, distinct from duplicate ordinary child-TT resolutions. Bounded scout tactical/evasion scans at parent-depth floors two/three/four reduced nodes, but preparation/traffic, fresh depth-seven costs and concentrated deeper gains did not establish sufficiently consistent wall-time benefit. Preserve ordinary child-entry TT resolution and unchanged production Search; no broader all-child mechanism adopted. Reconsider only after a concrete dependency or mechanism-specific evidence change, not convention, weaker proof or nodes alone. |
| SR-028 | Alternative/historical forward-pruning screen | PENDING | Deliberately screen credible techniques not otherwise represented, including AEL pruning, sibling-prediction ideas, uncertainty cutoffs, parity/enhanced-forward-pruning families and other serious historical alternatives discovered during research. | Coverage safeguard; promote a technique to its own frontier entry if later evidence warrants it, subject to an explicit programme decision. | - |

## Reductions / Extensions / Re-search

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-029 | Late-move reductions and re-search | REJECTED | Base LMR curve; depth/move number; node type; tactical status; history; improving; previous reductions; reduced null-window Search; partial/full-depth re-search conditions. | Ordering/PVS, static-leaf horizon and mathematical TT proof dependency; supplies no pruning authority to SR-022/SR-023 and does not reopen SR-001/SR-010. | Fixed quiet R=1/R=2 and a bounded two-step depth policy established real wall-time headroom but missed critical late quiet resources, including a 480-point oracle move-value loss and nominal mates at ordinals 14/21 after a promotion-horizon safeguard. Retain nominal-depth production PVS and unchanged SR-018/SR-019. Primitive dependency/discharge and actual-depth TT semantics were demonstrated in research only; no adaptive/history/root/tactical reduction is adopted. Reconsider only with a concrete dependency or evidence change addressing these failures, not threshold sweeps or speed alone; no Elo claim. |
| SR-030 | Check extensions | REJECTED | Historical Seed check-extension success; extending giving check versus being in check/evasions; depth dependence; interaction with qsearch, LMR and runaway extension control. | Static-leaf horizon, credit-dependent TT value equivalence, PVS/history and SR-018/SR-019 composition; distinct from SR-001 and SR-031/SR-032. | Boundary giving-check +1 with one credit, then an evidence-motivated tactical-only variant, exposed useful material/draw information but parity-sensitive root decisions and fresh/deeper costs prevented adoption. Credit-aware research TT removed artificial suppression overhead without establishing sufficient value; a verified adverse move lost mate two plies sooner. Retain nominal-depth production and unchanged SR-018/SR-019. Reconsider only after a concrete dependency or mechanism-specific evidence change addressing decision value versus cost; no broader in-check, cap, fractional or other extension policy adopted, and no Elo claim. |
| SR-031 | Singular-extension family | REJECTED | TT-driven singular tests; exclusion Search; margins; ordinary/double/multiple singular extension; negative extensions/reductions; multi-cut interaction and cost control. | Mathematical TT horizons, restricted-move proof, TT-dependent policy identity, PVS/ordering and SR-018/SR-019 composition. | Strict current-generation EXACT/LOWER anchors at D-1 with G=0 produced valid same-horizon exclusion proof, but +1/cap-one testing retained adverse promotion/material root decisions and high probe costs despite value-equivalent descendant/policy TT reuse. Preserve nominal-depth production and SR-018/SR-019. Snapshot/domain/provenance machinery remains external research only; positive margins, broader anchors, multiple/negative extensions and multi-cut were not earned. Reconsider only after a concrete dependency or mechanism-specific evidence change addressing root-decision value versus cost; no Elo claim. |
| SR-032 | Other tactical/forced-line extensions | REJECTED | Recapture, passed-pawn/promotion, one-reply, mate-threat/threat, history/hindsight and other credible extension families; fractional extensions and total-extension caps. | SR-017 static-leaf classification, factual credit-dependent TT/provenance, real rule/history state and SR-018/SR-019 composition; distinct from closed SR-030/SR-031 and SR-033's separate feedback disposition. | Cheap exactly-one-legal-reply boundary +1/cap-one research exposed real draw/material information and an isolated mate-delay resource, but fresh/adversarial root changes remained mixed or adverse across deeper parities. Economical classification and modest/concentrated costs did not establish adoption value. Preserve nominal-depth production and SR-018/SR-019; no cap increase, fractional rule or alternative family earned a pivot. Reconsider only after a concrete dependency or mechanism-specific evidence change addressing complete-root benefit versus cost; no Elo claim. |
| SR-033 | Adaptive/hindsight depth feedback | REJECTED | Depth adjustments from previous reductions, search outcomes, static-eval movement or other evidence that earlier assumptions were too optimistic/pessimistic. | Nominal PVS and available outcome/static/history evidence; reduction-conditioned hindsight lacks an accepted legal-reduction producer. Rejected families retain their reopening boundaries. | Independent confirmed-outcome +1/+2 feedback preserved ordinary proofs and exposed useful deeper resources, but mixed/adverse choices and repeated complete-request costs did not earn adoption. Retain nominal production and SR-018/SR-019. Reconsider only after a concrete accepted dependency or mechanism-specific evidence change addressing decision value versus cost; no implicit LMR/static-correction reopening or Elo claim. Evidence: C:/Users/Central/Documents/SeedV6-SR033-2026-10-01/. |

## Lifecycle / Parallelism / Mechanics

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-034 | Time management | IMPLEMENTED | Hard/soft limits; best-move and score stability; root effort; fail-high/low instability; remaining time/increment; easy-move behaviour; incomplete-iteration contract. | Mature iterative deepening/production driver and SR-037 mechanical baseline; informs SR-035/SR-036 lifecycle work. | Adopted clock-only completed forced-move/mate-in-one stopping with distinct allocation completion, truthful actual depth and hard-limit precedence; usable clock above reserve receives at least 1ms opportunity. Stability missed deeper resources; conservative cost forecasts showed modest headroom but did not establish net redistribution value and could miss mate proofs. No general predictive stop or new soft/hard multiplier adopted; reconsider with concrete stronger evidence/dependency changes. Preserved Search/evaluator/TT semantics; no Elo claim. Evidence: C:/Users/Central/Documents/SeedV6-SR034-2026-10-01/. |
| SR-035 | Pondering and speculative opponent-turn Search | PENDING | Whether SeedV6 should ponder; predicted-move reuse; cancellation; TT/history reuse and protocol/lifecycle implications. | Time management, lifecycle and TT. | - |
| SR-036 | Parallel Search architecture | PENDING | Lazy SMP; root parallelism; YBWC; ABDADA; Jamboree/DTS-style alternatives; TT/history sharing versus isolation; thread diversification; voting and scaling. | Deliberately after mature single-thread Search semantics; preserve SR-013's separate root decision and serialized provider ownership outside recursive workers. | - |
| SR-037 | Production Search hot-path mechanical optimization | IMPLEMENTED | Allocation removal; primitive/frame layout; branches; locality/cache behaviour; TT/history prefetch; repeated-state reconstruction; incremental information; move-list representation; JVM/JIT specialization; NPS versus tree-shape measurement. | Cross-cutting mechanical research; must preserve semantic/oracle baselines and settled TT/mechanical boundaries. | Profile-led research adopted direct primitive legal-existence querying at non-check static leaves, avoiding move-list materialization while retaining complete checked evasions. Preserved positive-depth ordering, exact/reference and evaluator boundaries, full tree/state/TT behavior and SR-018/SR-019. Repeated warmed production timing earned the smaller leaf-only implementation over broader generator-setup variants; no TT/layout/prefetch redesign or playing-strength claim. Evidence: C:/Users/Central/Documents/SeedV6-SR037-2026-10-01/. |

## Experimental / Future guidance

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-038 | Learned or evaluator-assisted Search guidance | PENDING | Neural/policy move ordering; learned selectivity/uncertainty; evaluator confidence; whether evaluator-specific Search information can outperform generic histories without compromising the established evaluator-independent core boundary. | Experimental; best assessed against a mature conventional Search baseline unless earlier evidence justifies advancing it. | - |

## Revision history

| Revision | Frontier change |
| --- | --- |
| F006 | Clarified maturity-dependent non-retention and dependency-triggered reconsideration; preserved status meanings and explicit programme authority to reopen REJECTED items. |
| F007 | Accepted SR-014 overall ordering and SR-017 staged-generation/lazy-selection mechanics, including deferred quiet-history sampling; production adoption remains separate. |
| F008 | Accepted SR-003 PVS for current TT-enabled exact Search, retaining TT-off ordered alpha-beta reference execution; production adoption and downstream narrow/selective research remain separate. |
| F009 | Moved SR-003 and SR-014/015/016/017 from ACCEPTED to IMPLEMENTED following production adoption. |
| F010 | Closed SR-002 as REJECTED under researched conditions; retained recursive production mechanics, with reconsideration requiring a concrete material dependency, platform or architecture change. |
| F011 | Closed SR-001 as REJECTED for current production/default Search; retained static leaves and qsearch research/reference evidence, with reconsideration requiring a concrete material dependency or Search-architecture change. |
| F012 | Closed SR-005 as REJECTED for additional explicit previous-PV/best-move ordering redundant with same-request TT/hash-move reuse; preserved separate SR-006/SR-034 research and required a material dependency or architecture change for reconsideration. |
| F013 | Closed SR-006 aspiration windows as REJECTED under researched single-thread/HCE conditions; retained full-window production Search and required a concrete material dependency or Search-architecture change for reconsideration. |
| F014 | Closed SR-004 MTD(f)/memory-enhanced zero-window root drivers as REJECTED for the researched current single-thread/HCE architecture; preserved oracle-headroom nuance and full-window production PVS, with reconsideration requiring a concrete material dependency or architecture change. |
| F015 | Closed SR-008 as ACCEPTED for the orthogonal evidence model and constrained path-context use; did not adopt authoritative PV/Cut/All roles or authorize selectivity, and preserved SR-009's static-evaluation boundary. |
| F016 | Closed SR-009 as ACCEPTED for static-evaluation evidence and research disposition; retained bounded HCE tactical/residual headroom, did not adopt movement/reliability mechanisms or correction history, and preserved dependency-triggered reconsideration and independent later consumers. |
| F017 | Closed SR-019 as IMPLEMENTED for the calibrated HCE-only depth-two static-null variant, preserving mathematical TT and exact-reference boundaries; recorded the raw-E consumer dependency without starting correction or another frontier feature. |
| F018 | Closed SR-018 as IMPLEMENTED for bounded HCE fixed-R=2 null move with measured eligibility and synthetic-domain isolation; did not adopt adaptive reduction or verification, preserved SR-019 and exact/neural boundaries, and recorded dependencies without starting another feature. |
| F019 | Closed SR-029 as REJECTED under researched quiet-LMR conditions despite material economic headroom; retained critical promotion/mate witnesses and proof-dependency findings, preserved production nominal-depth Search, and began no next feature. |
| F020 | Closed SR-021 as REJECTED under researched static-leaf HCE conditions: smaller fixed margins missed resources and conservative depth-one/depth-two policies lacked repeatable economic value. Preserved production Search and research proof/history findings; began no next feature. |
| F021 | Closed SR-030 as REJECTED for researched boundary giving-check and tactical-only +1/cap-one policies; preserved useful local evidence and credit-dependent TT/provenance findings without production adoption, retained existing Search and began no next feature. |
| F022 | Closed SR-031 as REJECTED for researched strict same-horizon singularity and +1/cap-one extension: preserved the valid exclusion/TT proof model and useful isolated resources, but adverse complete root decisions and measured probe/extension costs prevented adoption. Retained production Search and began no next feature. |
| F023 | Closed SR-032 as REJECTED for researched cheap forced-reply static-boundary +1/cap-one extensions: retained useful local resources and factual TT/chain findings, but insufficient consistent complete-root value prevented adoption. Preserved production Search; no alternative extension pivot or next frontier feature was begun. |
| F024 | Closed SR-027 as REJECTED for researched exact phase-local child-TT look-ahead: preserved mathematical proof and genuine avoided-prefix evidence, but the tested bounded populations did not earn consistent production economics. Retained ordinary TT resolution and existing Search; no next feature was begun. |
| F025 | Closed SR-010 as IMPLEMENTED for exact primitive repetition-scan acceleration and the formal prefix/path, count-state GHI, full-status and twofold/upcoming semantics. Retained the conservative ordered SearchKey, all TT/PVS/mate and SR-018/SR-019 boundaries, and began no other feature. |
| F026 | Closed SR-011 as IMPLEMENTED for exact core mate-domain restriction after numeric, oracle, TT/PVS and selective-composition validation. Recorded separately settled finite-horizon gap mathematics and rejection of its runtime candidate for unearned economics; retained independent controls and began no next feature. |
| F027 | Authorized explicitly launched Codex programme runs to dynamically select, research, disposition and close one admitted feature at a time, re-reading both repository masters at each boundary. Included justified adoption/canon maintenance, genuine escalation and programme-end conditions; deferred mirror synchronization to external handback and subsequent GPT use while preserving evidence, reopening, scope and LOCKED boundaries and all feature dispositions. |
| F028 | Closed SR-007 as REJECTED after exact missing-hash IID and selective one-/two-ply IIR research: IID variants lacked repeatable economics and IIR retained prediction/resource failures. Preserved nominal production Search, SR-018/SR-019 and oracle/TT boundaries, with concrete dependency/evidence conditions for reconsideration. |
| F029 | Selected SR-037 as ACTIVE after SR-007 closeout and fresh canon reads, investigating recurring mechanical costs with exact/reference and tree-equivalence requirements; no production mechanism adopted. |
| F030 | Closed SR-037 as IMPLEMENTED for exact non-check static-leaf legal-existence mechanics, supported by legality/oracle/state equivalence and repeated warmed actual-production timing. Reconciled SR-017's leaf description while preserving ordering, TT, evaluator and selective boundaries; no other feature reopened. |
| F031 | Selected SR-034 as ACTIVE after SR-037 closeout and fresh canon reads, examining clock allocation, completed-iteration evidence and hard/soft stopping semantics; no time policy adopted. |
| F032 | Closed SR-034 as IMPLEMENTED for bounded factual clock-decision stopping and positive usable-clock allocation; preserved completed-result/hard-limit/evaluator semantics. Recorded stability and forecast non-adoptions without overstating counterevidence or playing strength; retained further lifecycle dependencies. |
| F033 | Selected SR-024 as ACTIVE after SR-034 closeout and fresh canon reads, isolating SEE-based move prediction, quiet-exchange semantics and nominal-horizon/proof/economic obligations; no production mechanism adopted or rejected feature reopened. |
| F034 | Closed SR-024 as REJECTED for current production adoption after independent exchange/nominal/proof validation, resource challenges and repeated fixed-depth/iterative-driver timing. Preserved positive refined quiet evidence without inventing strength harm, recorded total-cost limitations and reopening conditions, and retained unchanged production semantics. |
| F035 | Selected SR-022 as ACTIVE after SR-024 closeout and fresh repository-master reads, isolating cheap searched-count evidence, shallow quiet omission, nominal resource/proof obligations and actual iterative-driver economics. No other feature reopened or policy adopted. |
| F036 | Closed SR-022 as REJECTED after fresh nominal/proof/resource audits, clean and primitive quiet-check mechanics, and repeated actual-driver tests including a two-control confirmation. Preserved modest positive D2 evidence without adopting unreliable complete-cost behaviour, kept SR-023 independent and retained production semantics. |
| F037 | Selected SR-025 as ACTIVE after SR-022 closeout and fresh repository-master reads, investigating legal reduced-probe information, historical Multi-Prob-Cut scope, actual-depth proof and complete iterative economics. No new pruning mechanism adopted or other feature reopened. |
| F038 | Closed SR-025 as REJECTED after original-history/proof/resource audits, full/progressive/tactical probes, nominal verification and primitive-mechanics/wider-margin cost challenges. Preserved positive evidence, unchanged production and SR-026 independence; recorded Contract reconsideration conditions. |
| F039 | Selected SR-026 as ACTIVE after SR-025 closeout and fresh repository-master reads, isolating multiple-success legal-probe evidence, correlated horizon errors, actual-depth proof and complete iterative cost. No other feature reopened or policy adopted. |
| F040 | Closed SR-026 as REJECTED after multiple-success/diversity/margin/proof/cost challenges. Preserved genuine savings and favourable resource results alongside correlated failures and conservative-policy regressions; production and other feature dispositions unchanged. |
| F041 | Selected SR-023 as ACTIVE after SR-026 closeout and fresh repository-master reads: isolate inexpensive accepted history evidence, rank/lifecycle conditioning, nominal omission proof and complete iterative economics. No other feature reopened or policy adopted. |
| F042 | Closed SR-023 as REJECTED after original-history observation/proof/resource audits and clean/compact/epoch iterative challenges. Preserved genuine concentrated quiet savings and mixed root effects without adopting weaker resource/economic combinations; production and other feature dispositions unchanged. |
| F043 | Selected SR-020 as ACTIVE after SR-023 closeout and fresh repository-master reads: isolate node-level alpha-side razor evidence, reduced/qsearch horizons, nominal proof and complete cost. No production leaf policy changed or other rejected feature reopened. |
| F044 | Closed SR-020 as REJECTED after reduced/qsearch original-path/proof/resource research and repeated clean actual-driver challenges. Preserved small positive results and bounded counterevidence, unchanged production and explicit reconsideration conditions; no other feature reopened. |
| F045 | Selected SR-033 as ACTIVE after SR-020 closeout and fresh full repository-master reads: trace legitimate depth-feedback evidence and prerequisites before later architecture/lifecycle work. No depth policy adopted or rejected feature reopened. |
| F046 | Closed SR-033 as REJECTED after actual-producer analysis and independent confirmed-outcome +1/+2 research with proof/state/resource and complete-driver challenges. Recorded missing legal-reduction prerequisite and bounded reconsideration; preserved production and positive evidence. |
| F047 | Selected SR-013 as ACTIVE after SR-033 closeout and fresh repository-master reads: settle tablebase outcome/value, rule/history and probe boundaries before later architecture/guidance consumers. No tablebase mechanism adopted or closed feature reopened. |
| F048 | Closed SR-013 as IMPLEMENTED for optional verified small-table root WIN decisions after independent probe/rule/history checks, complete policy certificates, conversion and implementation validation. Preserved separate outcome provenance, ordinary Search defaults and bounded platform/coverage limits; recorded native ownership for later parallel work without starting it. |
