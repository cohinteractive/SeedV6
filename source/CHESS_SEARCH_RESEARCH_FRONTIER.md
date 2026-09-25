# SeedV6 Search Research Frontier

Internal frontier revision: **F001**

Status: **Active Search research-frontier canon; open-ended by design.**

Repository master: `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` in the current
`SeedV6` repository.

## Authority and purpose

The repository copy is the authoritative master. A ChatGPT Project Source copy,
when present, is a manually refreshed mirror, not an independent authority.

[CHESS_SEARCH_CONTRACT.md](CHESS_SEARCH_CONTRACT.md) remains authoritative for
actual Search purpose, semantics, architecture, principles, settled decisions
and **OPEN** architectural questions. This frontier is authoritative only for
which research subjects have been deliberately admitted to the programme and
their research disposition. Listing an item does not approve it for SeedV6
Search. The frontier must never override or silently modify the Search Contract.

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

A fresh GPT conversation selecting the next Search subject should read both
`CHESS_SEARCH_CONTRACT.md` for the current settled Search state and
`CHESS_SEARCH_RESEARCH_FRONTIER.md` for remaining admitted research coverage.
It should then recommend the logically strongest next research unit from the
current state, considering:

- Prerequisite maturity/correctness.
- Enabling value for later work.
- Expected playing-strength or useful-depth impact.
- Ability to measure the subject cleanly.
- Risk of designing later mechanisms against temporary assumptions.
- Architectural cohesion.

Next-work selection is dynamic, not a predetermined sequence. Do not choose the
lowest pending ID merely because it is first.

## Statuses

| Status | Meaning |
| --- | --- |
| PENDING | Research is admitted to the frontier but has not reached an accepted conclusion. |
| ACTIVE | Research is currently in progress. |
| ACCEPTED | Research has reached an accepted conclusion favouring incorporation into SeedV6 Search, but implementation/adoption is not yet fully complete. |
| IMPLEMENTED | The accepted result has been implemented and accepted. Any material durable Search-design consequence must also be represented in CHESS_SEARCH_CONTRACT.md under that file's own maintenance rules. |
| REJECTED | The subject was deliberately researched and the accepted conclusion is not to incorporate it under the researched conditions. |
| DEFERRED | Research has not reached a final include/exclude conclusion and has deliberately been postponed, normally because a prerequisite, evidence source or better timing is needed. |

## Maintenance rules

- Retain every admitted entry permanently, including IMPLEMENTED and REJECTED
  entries, so historical coverage is never lost. Normally do not delete items at
  all; terminal statuses preserve coverage.
- Every content-changing frontier update increments the internal `Fxxx` revision
  exactly once. Routine prose formatting alone should not create gratuitous
  revision churn.
- Codex may mechanically update the status, dependency/relationship field and
  concise disposition of an existing entry only when the accepted programme
  outcome is already established by the task/context. Codex must not independently
  infer ACCEPTED, IMPLEMENTED, REJECTED or DEFERRED merely because an implementation
  compiled, tests passed, one benchmark improved, a conventional engine uses the
  technique, or Codex personally recommends it.
- Adding a new research item; deleting an item; merging or splitting entries;
  renaming in a way that materially changes scope; or materially redefining an
  existing subject requires an explicit programme decision.
- If research exposes a previously unknown dependency or relationship, it may be
  mechanically recorded when the task has established it.
- A frontier change never by itself authorizes Search implementation or changes
  Search semantics. If an accepted research outcome materially changes durable
  Search architecture or policy, `CHESS_SEARCH_CONTRACT.md` must be maintained
  separately under its own authority and revision rules when that maintenance is
  authorized. If a task does not authorize required Search Contract maintenance,
  report it rather than silently editing that contract.
- Keep dispositions short. Do not embed detailed research results, benchmark
  histories, experiment logs, implementation notes, commit lists or conversation
  summaries here. Durable accepted architectural conclusions belong in
  `CHESS_SEARCH_CONTRACT.md`.
- After every repository frontier update, completion must report
  `FRONTIER_UPDATED: <old revision> -> <new revision>` and
  `PROJECT_SOURCE_REFRESH_REQUIRED: YES`. The human must add/refresh the ChatGPT
  Project Source mirror from the repository master before relying on it in
  subsequent frontier-selection conversations. A requested refresh is not proof
  that the human completed it.

## Foundational / Search architecture

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-001 | Quiescence Search | PENDING | qsearch semantics; stand-pat; captures, promotions, checks/evasions; in-check handling; capture ordering; SEE; delta pruning; qsearch futility; TT participation; repetition/draw handling; explosion/depth controls. | Foundational frontier semantics; interacts with tactical ordering and later selective pruning. | - |
| SR-002 | Recursive vs flattened Search mechanics | PENDING | Recursive negamax versus explicit flat/iterative Search frames or state machines; JVM/JIT effects; stack/call overhead; branches; locality; maintainability of exact semantics. | Exact-reference path must remain available; mechanical decision, not selective Search policy. | - |
| SR-003 | Principal Variation Search / zero-window Search | PENDING | PVS/NegaScout structure; first-move full windows; scout searches; fail-soft behaviour; re-search rules; TT and ordering interaction. | Benefits from strong move ordering; relevant to LMR and other probe searches. | - |
| SR-004 | MTD(f) and memory-enhanced zero-window drivers | PENDING | MTD(f) versus PVS/alpha-beta; first-guess quality; repeated zero-window passes; TT dependence; iterative-deepening interaction; PV handling; related MTD/NegaC\*/SSS\*/Dual\* ideas where useful. | Depends strongly on mature TT semantics and zero-window behaviour. | - |
| SR-005 | Iterative-deepening information reuse | PENDING | Previous PV, previous best move/value, iteration stability and information transfer beyond the already-settled simple successive-depth driver. | Supports ordering, aspiration and time management. | - |
| SR-006 | Aspiration windows | PENDING | Initial window; fail-low/high widening; asymmetric widening if justified; score volatility; mates; interaction with PVS/MTD(f). | Iterative deepening and stable previous-score evidence. | - |
| SR-007 | Internal iterative search: IID and IIR | PENDING | Internal iterative deepening; internal iterative reductions; missing-hash/PV situations; cost versus ordering/depth benefit. | TT, ordering and PVS. | - |
| SR-008 | Node/evidence model and path context | PENDING | PV/Cut/All or alternative node characterization; expected cutoff status; parent/path evidence; move rank; previous reductions; improving/worsening and authoritative versus heuristic evidence. | Foundational input to coherent selectivity. | - |
| SR-009 | Static-evaluation evidence and correction | PENDING | Raw versus Search-adjusted eval; TT eval; improving/worsening; correction-history-style mechanisms; reliability/context/uncertainty signals; evaluator independence versus justified evaluator-specific evidence. | Supports pruning, reductions and depth decisions. | - |
| SR-010 | Repetition, cycles and graph-history interaction | PENDING | Seed-style key-history scanning; twofold Search repetition versus formal game adjudication; upcoming repetition; rule-50; GHI; TT/SearchKey interaction and performance. | Correctness and value-equivalence boundary. | - |
| SR-011 | Mate-distance pruning | PENDING | Early alpha/beta tightening or return from already-proven superior mate distance; exactness; mate bands; ply-normalized TT interaction. | Relies on existing mate-score semantics. | - |
| SR-012 | Remaining advanced TT policy | PENDING | All TT questions deliberately left OPEN by the current contract, including deeper-for-shallower evidence, non-cutting bound tightening, cross-generation reuse, score versus move identity, replacement, sizing, statistics, prefetch and later parallel sharing. | Builds on the current initial TT programme. | - |
| SR-013 | Tablebase integration into Search | PENDING | Root/interior probing; WDL/DTZ; rule-50; score domains; TT interaction; probe depth and effects on selectivity. | Terminal/value semantics and TT. | - |

## Move ordering / Search information

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-014 | Overall move-order architecture | PENDING | Coherent ordering pipeline across hash/PV/tactical/refutation/history/bad-capture classes; staged versus monolithic ordering; ordering as Search evidence. | Major prerequisite/enabler for PVS, LMR and several pruning methods. | - |
| SR-015 | Capture/tactical ordering and SEE | PENDING | MVV-LVA versus SEE; good/bad captures; promotions; capture history; SEE thresholds; tactical ordering in normal Search and qsearch. | qsearch, move ordering and SEE pruning. | - |
| SR-016 | Quiet-move ordering memory | PENDING | Killer moves; history; relative history; countermove; continuation/follow-up histories; refutations; context/pawn/threat histories; update/decay/gravity policy. | Overall ordering; LMR and history-based pruning. | - |
| SR-017 | Move sorting and generation mechanics | PENDING | Seed's historical insertion-sort <=16 plus quicksort above that; full sort versus selection/partial/staged/lazy move picking; incremental generation; data/branch/JIT consequences. | Mechanical implementation of the ordering architecture. | - |

## Forward pruning / Selectivity

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-018 | Adaptive verified null-move pruning | PENDING | Adaptive NMP reduction; depth/material/static-eval conditions; zugzwang risk; verification search; recursive NMP restrictions; threat evidence. | Node/eval evidence; later selectivity. | - |
| SR-019 | Reverse futility / static-null pruning | PENDING | Depth-conditioned static-eval early returns; conventional RFP/static-null pruning; adaptive margins; improving/eval reliability; mate safeguards. Explicitly determine whether the user's historically successful depth-based static-eval return is identical to or distinct from conventional RFP. | Static-evaluation evidence. | - |
| SR-020 | Razoring | PENDING | Reduced-depth or qsearch-based razor forms; margins; PV restrictions; mate safeguards; relation to qsearch/futility. | qsearch and static-evaluation evidence. | - |
| SR-021 | Futility-pruning family | PENDING | Frontier, extended and deeper futility; node versus move forms; tactical/check/promotion exclusions; margins; qsearch and LMR interaction. | Static eval, qsearch and ordering. | - |
| SR-022 | Late-move / move-count pruning | PENDING | Pruning sufficiently late moves entirely; move-count formulas; history/improving/depth modifiers; tactical safeguards. | Ordering, quiet-history evidence and LMR. | - |
| SR-023 | History-based pruning | PENDING | Quiet/capture/noisy history as pruning evidence rather than merely ordering/reduction evidence. | Requires mature history systems. | - |
| SR-024 | SEE-based move pruning | PENDING | Depth-sensitive SEE thresholds for quiets and captures and their interaction with other pruning. | Requires SEE research. | - |
| SR-025 | ProbCut and Multi-ProbCut | PENDING | Probabilistic/reduced searches around bounds; tactical move filtering; depth/margins; historical Multi-ProbCut used in prior Seed engines; progressive/repeated variants where relevant. | Narrow-window Search, TT, qsearch/tactical ordering. | - |
| SR-026 | Multi-Cut pruning | PENDING | Reduced searches of several promising moves where enough fail-highs justify pruning. | Ordering and narrow/probe Search; distinct from Multi-ProbCut and singular extension. | - |
| SR-027 | Enhanced Transposition Cutoff and TT look-ahead | PENDING | Probing candidate children or related TT look-ahead before normal expansion; node savings versus additional TT traffic. | Mature TT and ordering. | - |
| SR-028 | Alternative/historical forward-pruning screen | PENDING | Deliberately screen credible techniques not otherwise represented, including AEL pruning, sibling-prediction ideas, uncertainty cutoffs, parity/enhanced-forward-pruning families and other serious historical alternatives discovered during research. | Coverage safeguard; promote a technique to its own frontier entry if later evidence warrants it, subject to an explicit programme decision. | - |

## Reductions / Extensions / Re-search

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-029 | Late-move reductions and re-search | PENDING | Base LMR curve; depth/move number; node type; tactical status; history; improving; previous reductions; reduced null-window Search; partial/full-depth re-search conditions. | Strongly depends on move ordering, node evidence and PVS/narrow-window semantics. | - |
| SR-030 | Check extensions | PENDING | Historical Seed check-extension success; extending giving check versus being in check/evasions; depth dependence; interaction with qsearch, LMR and runaway extension control. | Frontier/tactical semantics. | - |
| SR-031 | Singular-extension family | PENDING | TT-driven singular tests; exclusion Search; margins; ordinary/double/multiple singular extension; negative extensions/reductions; multi-cut interaction and cost control. | Mature TT, PVS/probe Search and ordering. | - |
| SR-032 | Other tactical/forced-line extensions | PENDING | Recapture, passed-pawn/promotion, one-reply, mate-threat/threat, history/hindsight and other credible extension families; fractional extensions and total-extension caps. | Coherent extension/re-search architecture. | - |
| SR-033 | Adaptive/hindsight depth feedback | PENDING | Depth adjustments from previous reductions, search outcomes, static-eval movement or other evidence that earlier assumptions were too optimistic/pessimistic. | LMR, static-eval evidence and history. | - |

## Lifecycle / Parallelism / Mechanics

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-034 | Time management | PENDING | Hard/soft limits; best-move and score stability; root effort; fail-high/low instability; remaining time/increment; easy-move behaviour; incomplete-iteration contract. | Mature iterative deepening and production driver. | - |
| SR-035 | Pondering and speculative opponent-turn Search | PENDING | Whether SeedV6 should ponder; predicted-move reuse; cancellation; TT/history reuse and protocol/lifecycle implications. | Time management, lifecycle and TT. | - |
| SR-036 | Parallel Search architecture | PENDING | Lazy SMP; root parallelism; YBWC; ABDADA; Jamboree/DTS-style alternatives; TT/history sharing versus isolation; thread diversification; voting and scaling. | Deliberately after mature single-thread Search semantics. | - |
| SR-037 | Production Search hot-path mechanical optimization | PENDING | Allocation removal; primitive/frame layout; branches; locality/cache behaviour; TT/history prefetch; repeated-state reconstruction; incremental information; move-list representation; JVM/JIT specialization; NPS versus tree-shape measurement. | Cross-cutting mechanical research; must preserve semantic/oracle baselines. | - |

## Experimental / Future guidance

| ID | Research subject | Status | Scope | Dependencies / relationships | Disposition |
| --- | --- | --- | --- | --- | --- |
| SR-038 | Learned or evaluator-assisted Search guidance | PENDING | Neural/policy move ordering; learned selectivity/uncertainty; evaluator confidence; whether evaluator-specific Search information can outperform generic histories without compromising the established evaluator-independent core boundary. | Experimental; best assessed against a mature conventional Search baseline unless earlier evidence justifies advancing it. | - |
