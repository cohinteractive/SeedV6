# SeedV6 Search Contract

Internal revision: **R005**

Status: **Active Search programme canon; architecture intentionally incomplete.**

Repository master: `source/CHESS_SEARCH_CONTRACT.md` in the current `SeedV6` repository.

**LOCKED** means a settled programme decision, subject to an explicitly accepted
contract change. **TENTATIVE** means a working model awaiting refinement.
**OPEN** means an unresolved question, not an implicit implementation choice.

## Authority and synchronization

The repository copy of `CHESS_SEARCH_CONTRACT.md` is the authoritative master.
The ChatGPT Project Source is a manually refreshed mirror of this file, never
an independently edited authority. There must be only one active repository
canon under this stable filename; revisions belong inside the file.

Verified repository code, tests and benchmarks govern observed implementation
reality. This canon governs accumulated Search intent, terminology,
architectural principles, settled decisions, boundaries, unresolved questions
and accepted programme direction. A design statement does not establish that
the implementation already satisfies it. Conflicts between the canon and
verified implementation must be surfaced for resolution, not silently
reconciled in either direction.

This canon is not a worklog. Routine implementation detail, experiment logs
and transient benchmark history belong elsewhere.

## Programme purpose and engine baseline

Build SeedV6 Search again from first principles. The objective is neither
feature parity with SeedV3 nor reproduction of the current SeedV6 Search.
SeedV3, current SeedV6 Search, Stockfish, other engines and literature may
supply evidence, algorithms, experimental ideas and comparison points; none
is the architecture to reproduce. Derive Search behaviour from explicit
semantics and evidence, rather than a checklist of conventional heuristics.
Correctness and measurable playing/search performance both matter.

The development baseline is the NNUE-capable SeedV6 engine, preserving normal
handcrafted evaluation while adding selectable NNUE evaluation and the Play
and NNUE Training surfaces.

**LOCKED evaluator boundary:** Handcrafted evaluation (HCE) is the default for
bringing up and measuring the new Search. The Search algorithm must remain
evaluator-independent: its evaluator boundary must suit both HCE and neural
evaluators SeedV6 supports or develops, including NNUE and later network
architectures. Do not introduce evaluator-specific branches into core exact
Search without a future explicit design decision. NNUE training,
effectiveness and performance improvement remain a separate, parallel
workstream and do not block rebuilding Search.

The SeedV3 to SeedV6 transplant programme is complete to the level now
required. Its [transplant/discovery report](../SEEDV3_TO_SEEDV6_TRANSPLANT_DISCOVERY.md)
is preserved as historical evidence, not the active Search programme canon.
Existing implementation choices and transplant-era roadmaps do not become
settled decisions of this programme merely by already existing.

## LOCKED foundational principles

### A. Ordered alpha-beta is the foundation

Start from correct ordered alpha-beta. Move ordering aims at best-first
visitation and is part of Search's information foundation, not merely a
cosmetic optimization. Move visitation must be deterministic; the first
implementation may use the simplest valid deterministic ordering available
through established engine infrastructure. Existing Search heuristics are
not automatically accepted. More sophisticated move ordering must follow
this contract's evidence and reasoning process.

### B. Exact and selective search are distinct

Exact alpha-beta may omit work because mathematical bounds prove it
irrelevant. Selective search deliberately spends less work based on evidence
and therefore has different correctness and risk semantics. Do not blur these
categories.

The first implementation is recursive negamax ordered alpha-beta, an exact
reference implementation. It begins single-threaded and includes no
selective pruning, reductions or extensions.

### C. Evidence precedes selectivity

First establish what the node and move are known to be, what evidence is
available and what that evidence justifies. Selective mechanisms consume
established evidence. Do not start by asking where LMR, null move or futility
should be inserted.

### D. Eligibility and aggression are separate

For each selective technique distinguish whether the node/move is eligible
from how aggressively the technique should apply once eligible. Do not
collapse both questions into one opaque conditional.

### E. No universal aggression/confidence scalar is assumed

Shared node evidence may support several techniques, but each may interpret
that evidence differently. Do not prematurely reduce the complete evidence
model to one universal confidence number.

### F. Parent and search-path context may be evidence

A node need not exist in informational isolation. How it was reached may
legitimately affect Search decisions. Beyond the LOCKED invocation,
mate-distance and TT value-equivalence requirements below, exactly which
parent/path information crosses the node boundary remains OPEN.

### G. Preserve independent correctness baselines

Do not optimize away independently useful exact/oracle search paths merely
because production Search becomes stronger or faster. The exact recursive
implementation must remain independently available as a correctness and
reference baseline even if later production Search becomes selective or uses
a different mechanical implementation such as flat search. Validate
performance improvements rather than assuming them.

Production adoption must preserve the independently invocable ExactSearch
fixed-depth path and its headless validation harness. They remain available
for semantic correctness comparison, implementation-only optimization
comparison, later selective-Search validation and deterministic node/tree
comparison where applicable.

### H. New implementation, integration and adoption boundaries

Build a genuinely new Search implementation, rather than refactoring the
internals of the existing SeedV6 Search. Existing non-Search infrastructure
may be reused where appropriate, including Board/state representation, legal
move generation, evaluator infrastructure, lifecycle integration and other
established engine services. Existing Search implementations remain evidence
and comparison material; only explicit canon decisions, including section J's
TT mechanical selection, define the new internal architecture.

Preserve the externally required Search integration boundary used by the
rest of SeedV6 where practical. An adapter or facade may preserve that
boundary so internal redesign does not force unrelated application rewrites.

Once the new Search has a functional and sufficiently validated baseline,
appropriate normal SeedV6 consumers should migrate through the production
adoption path `consumer -> Search driver/coordinator -> ExactSearch fixed-depth
primitive`, after compatibility has been verified, so ongoing Search development
can be exercised through normal application use. Consumers with legitimately
different semantics must be adapted carefully rather than forced through an
invalid abstraction. Migration is a later implementation unit, outside canon
maintenance. The existing production Search may remain temporarily for
comparison or consumers not yet migrated.

### I. Exact Search invocation and result semantics

ExactSearch's primary semantic responsibility is one logically complete
fixed-depth exact Search invocation under the negamax/alpha-beta semantics
below. It remains independently usable as the correctness/reference path.
Production lifecycle behaviour must not obscure or redefine the semantics of
that fixed-depth invocation; interrupted work remains incomplete.

- **Score perspective:** Every node returns a score from the perspective of
  the side to move at that node. Negamax negates the child result when
  propagating it to the parent.
- **Depth:** The depth argument is remaining nominal search depth in plies.
  A normal child receives `depth - 1`. Absolute/root ply is separate from
  remaining depth and must not be conflated with it.
- **Window and return:** A child receives the conventional negated, reversed
  negamax window `[-beta, -alpha]`. Exact Search uses fail-soft returns: a
  cutoff may return the actual discovered score outside the caller's window,
  rather than clamping it to the window edge.
- **Initial leaves:** At `depth <= 0`, non-terminal positions resolve through
  static evaluation. Quiescence Search is excluded from the first exact
  implementation; its design remains OPEN and must be specified separately,
  not inherited from existing Search.
- **Terminal and mate scores:** Drawn terminal positions return `0` in the
  Search score domain. Checkmate uses a mate score adjusted by search/root
  ply so Search prefers faster mates and delays unavoidable losses. Mate
  distance is based on path/root ply, not remaining depth. Terminal outcomes
  take precedence over static leaf evaluation. No numeric mate-score constant
  is locked by this decision.
- **Completion:** Cancellation or interruption must not represent an
  incomplete node/search as a valid completed Search result. The
  implementation must cleanly distinguish completed results from
  aborted/incomplete work while respecting the external lifecycle contract;
  this decision does not prescribe a concrete API.

### J. Transposition-table boundary, mechanics and exact evidence

The initial exact reference Search does not depend on a transposition table
(TT) and must remain independently runnable with Search TT use fully disabled.
R004 locks the mechanical substrate; R005 locks the initial exact-Search
interpretation and applicability of its evidence below. Neither revision
authorizes TT integration implementation or migration by itself, changes the
independent TT-off ExactSearch reference semantics, or accepts replacement
policy as optimal. Existing TT behaviour must not be silently imported merely
because it already exists elsewhere in SeedV6.

**LOCKED mechanical baseline:** The handcrafted
`com.ohinteractive.seedv6.search.tt.TTable` is the accepted mechanical
transposition-table substrate for future rebuilt SeedV6 Search TT work.
Build forward from `TTable`. The existing Codex-authored `TranspositionTable`
in the same package is not the architectural starting point for new-Search
TT development. When TT integration is explicitly designed, migrate
appropriate engine use toward `TTable`; existing historical, reference and
tool usages may remain until that implementation work determines their
disposition. This selection accepts storage and mechanics only, not existing
Search policy associated with either implementation, and does not itself
authorize code changes or migration.

The accepted mechanical architecture is:

- **Storage:** Direct-mapped, power-of-two capacity, indexed by the low key
  bits, with full 64-bit key comparison to verify hits. Entry storage uses
  three primitive `long` arrays: `key[]`, `data[]` and `hashMove[]`. The logical
  entry remains three `long` values (24 bytes). Preserve this primitive-array
  design; object-based entries, buckets, extra metadata arrays and abstraction
  layers are not requirements of the baseline.
- **Packed Search metadata:** `data[]` retains the following layout. Unused
  bits have no accepted future semantics.

  | Bits | Meaning |
  | --- | --- |
  | 0-7 | Depth |
  | 8-9 | Type |
  | 10-17 | Generation |
  | 18 | Validity |
  | 19-31 | Currently unused |
  | 32-63 | Score |

  Search types remain EXACT = 0 (`TYPE_EXACT`), LOWER = 1 (`TYPE_LOWER`) and
  UPPER = 2 (`TYPE_UPPER`); `TYPE_EVAL = 3` is reserved for the separate
  evaluation-cache use case. Normal Search entries use the validity bit /
  `VALID_MASK`, so zero-filled storage is unambiguously empty even when the
  position key itself is zero.
- **Probe and scratch:** `TEntry` is mutable caller-owned scratch.
  `probe(key, entry)` allocates nothing and returns only whether it updated
  that scratch from a matching valid Search entry. A failed probe leaves the
  scratch untouched. Callers must respect the boolean result; a successful
  mechanical probe does not establish Search applicability.
- **Locking:** Retain striped `synchronized` locking and the padded
  `StripeLock` design. This neither settles parallel-Search architecture nor
  proves optimal locking. A more conventional implementation style alone is
  not grounds to replace this selected mechanism.
- **Generation and clear:** Generation is an incrementing integer; its stored
  Search representation uses the low 8 bits. When those 8 bits wrap, clear
  `data[]` so ancient Search entries cannot become current solely because the
  stored generation byte repeats. For normal Search entries, `clear()`
  invalidates entries by zeroing `data[]`; stale `key[]` and `hashMove[]`
  values are irrelevant because validity resides in `data[]`. The Search
  generation lifecycle below governs when these mechanics are used.
- **Separate evaluation cache:** Evaluation caching deliberately uses a
  separate `TTable` instance, retaining the raw-score `TYPE_EVAL` / `probeEval`
  representation and caller-managed semantics. Its validity and clear
  behaviour differ from normal Search entries and remain caller-managed;
  do not generalize them into a single abstract TT policy or mix evaluation
  and Search entries in one physical table.

**OPEN replacement policy:** The current handcrafted `TTable` replacement
behaviour is used mechanically for the first integration. It is neither a
LOCKED first-principles Search policy nor accepted as optimal. R005 does not
redesign or evaluate it; replacement remains OPEN for later empirical and
design work.

**LOCKED defensive and performance boundary:** Defensive checking belongs
outside the hot `TTable` implementation. Callers and Search architecture must
enforce correct probe/store mode, valid scratch objects, legal metadata
ranges, score interpretation, lifecycle usage and evidence applicability.
This assigns responsibility for invariants; it does not permit violations.
Future correctness or Search-semantic changes should preserve the low-level
character of `TTable`: where practical, prefer its packed representation,
caller/lifecycle invariants, allocation-free probing and primitive storage
over per-probe allocations, extra arrays without demonstrated need,
unnecessary abstraction or hot-path defensive work. Correct Search semantics
constrain this preference; performance cannot override correctness.

#### LOCKED exact reuse and node resolution

TT use at this stage belongs on the exact side of the exact/selective
boundary: exact-work reuse and ordering, with no TT-driven selectivity.
A hit may avoid repeated work only when its evidence proves the result
required by the current fixed-depth invocation. TT-enabled ExactSearch must
preserve the same fixed-depth mathematical Search value as TT-disabled
ExactSearch, without horizon changes, selective assumptions or approximate
Search semantics.

The semantic precedence for initial exact TT use is:

1. Cancellation/interruption checking as required by lifecycle semantics.
2. Current terminal/draw adjudication.
3. Applicable TT evidence.
4. Depth-boundary static evaluation.
5. Move generation/search.

A TT entry must not override current checkmate, stalemate, repetition,
rule-50 or any other applicable terminal condition. This is semantic
precedence, not incidental method ordering: legal-move generation needed to
establish terminal status may occur during adjudication.

#### LOCKED score-evidence identity and depth

A matching ordinary board-position hash alone is insufficient if Search
value depends on state it omits. The Search TT key for score/bound evidence
must identify all state required for exact Search-value equivalence, including
relevant rule-50/halfmove-clock state, repetition-relevant game and search-path
history, and any other value-relevant Search state identified by implementation
inspection. This covers effects on both current terminal adjudication and
future Search value; a currently non-terminal position alone does not prove
history independence. Applicable evaluator/context state must also preserve
value equivalence.

`TTable` remains unaware of this distinction; the Search caller supplies the
correct 64-bit key. Correctness takes priority over hit rate. A conservative
value-equivalence key may miss transpositions that a future proof could safely
merge. No specific hashing algorithm or bit layout is locked here.

For initial exact score/bound reuse, **stored remaining depth must equal
requested remaining depth** (`storedDepth == requestedDepth`).
`storedDepth >= requestedDepth` is not accepted as score/bound qualification:
different fixed depths have different evaluation horizons and need not have
the same minimax value. A depth mismatch makes the stored score/bound
inapplicable as proof; hash-move evidence is separate.

#### LOCKED bound meaning and conservative probes

Search classifies a completed node score `S` against its original caller
window `[originalAlpha, originalBeta]`, before local alpha changes caused by
searched moves:

| Completed score | Stored type | Meaning for the exact fixed-depth value |
| --- | --- | --- |
| `S <= originalAlpha` | UPPER | Value is at most the stored score. |
| `S >= originalBeta` | LOWER | Value is at least the stored score. |
| Otherwise | EXACT | The exact value was established. |

These meanings apply only after identity/state, generation, equal-depth,
mate-score interpretation and all other applicability requirements succeed.
Search owns these semantics; do not reinterpret the type constants inside
`TTable`.

For the first exact integration, a qualifying EXACT may return immediately.
A qualifying LOWER may return/cut off only when its decoded score is
`>= beta`; a qualifying UPPER may return/cut off only when its decoded score
is `<= alpha`. Otherwise the stored bound does not tighten alpha or beta.
This cutoff-only LOWER/UPPER policy is deliberately conservative. Using a
qualifying non-cutting bound to tighten the window remains OPEN as a later
exact optimization subject to measurement and validation.

#### LOCKED mate-score interpretation

`TTable` does not interpret mate scores. Search callers must distinguish
mates from ordinary evaluator scores using the established Search mate-score
band and normalize on store/probe for the current root/path ply:

| Score class | Store | Probe |
| --- | --- | --- |
| Positive mate | Add current ply to the score. | Subtract current ply from the stored score. |
| Negative mate | Subtract current ply from the score. | Add current ply to the stored score. |
| Ordinary non-mate | Unchanged. | Unchanged. |

This stores a ply-independent mate representation and reconstructs the correct
root-relative mate distance when the same position is reached at another ply.
It agrees with the established Search convention; policy remains outside
`TTable`, and remaining depth must not be substituted for root/path ply.

#### LOCKED generation lifecycle and ownership

One top-level SearchDriver request owns one Search TT generation. All its
iterative-deepening iterations (depth 1, 2, 3, ...) reuse the same Search
`TTable` and the same generation; generation does not advance per iteration.
The table may remain allocated across subsequent top-level requests, each
with its own generation. Initial score/bound reuse requires the stored
generation to equal the current Search request generation. Older-generation
scores/bounds are not proof for the current request; cross-generation
score/bound reuse remains OPEN rather than silently crossing root-request or
evaluator/lifecycle contexts. R004's low-8-bit generation storage and wrap
clearing remain unchanged.

Each independently operating Search owner has its own Search `TTable`
instance/state. In particular, independent Play and Training Search ownership
must not be coupled by TT reuse. This does not settle parallel-tree sharing,
parallel/shared TT architecture or Lazy SMP TT behaviour; those remain OPEN.

#### LOCKED hash-move evidence and completion boundary

A matching TT hash move is ordering evidence, not mathematical proof of the
current Search value or proof that the move is currently best. Subject to
valid position identity and legal-move validation, it may be considered for
ordering despite a depth mismatch, a non-cutting bound, or an older stored
generation. The move must be legal in the current position before promotion
in move ordering. Conservative Search-key design may initially narrow this
reuse; separate position-only move keying from history-sensitive score keying
remains OPEN.

An incomplete/cancelled node must not store EXACT, LOWER or UPPER score/bound
evidence as though normal Search completion occurred. The existing completion
contract remains authoritative; R005 accepts no partial-score TT semantics.
Whether partial work can safely provide limited non-score evidence is a
separate future question, not an accepted capability here.

### K. Production Search driver and lifecycle

A Search driver/coordinator above ExactSearch owns production lifecycle
concerns: repeated fixed-depth invocations, iterative-deepening progression,
search limits, cancellation/stop propagation, observer/progress/result
adaptation and retention of the most recent completed Search result for
production consumers. This separation is semantic and architectural; it does
not prescribe a concrete Java class name, package or exact source-code shape.

For the first production adoption, iterative deepening is deliberately simple
and deterministic: begin at depth 1, then search successive complete depths
2, 3, 4, ... until the requested limit or an external stop condition prevents
further completion. Each iteration is an ordinary ExactSearch fixed-depth
invocation. This baseline adds no aspiration windows or other
iterative-deepening optimizations.

Only completed iterations may supply completed Search results. If depth 7
completes and depth 8 is then cancelled or otherwise stopped before completion,
the driver retains and may return/report the completed depth-7 result. The
depth-8 invocation remains incomplete and must never masquerade as a valid
depth-8 result. Retaining a previous completed result does not change the
completion semantics of the interrupted invocation.

Limits such as node budgets are driver/lifecycle concerns, not changes to
alpha-beta value semantics. A node budget may stop the active ExactSearch
invocation through the established cancellation/incomplete-result mechanism.
Reaching a node limit must not represent an incomplete node or iteration as a
mathematically completed Search result. Detailed time-management algorithms
remain OPEN.

GUI, Play, training and other consumer-specific observer, progress and result
requirements belong outside the ExactSearch recursive core wherever practical.
Thin adapters or driver-level observer translation are appropriate; satisfying
existing interfaces must not introduce GUI-specific or consumer-specific
lifecycle behaviour into the exact recursive algorithm.

## TENTATIVE working model

### Node lifecycle

This tentative model describes possible later evidence-driven Search. Exact
TT stages must respect section J's LOCKED evidence and precedence rules;
selective stages remain conditional on future accepted design. Neither is a
requirement of the independent TT-off exact reference path, and neither may
override its LOCKED semantics.

1. Enter the node and establish invocation context.
2. Resolve terminal state and usable transposition information.
3. Characterize the node and derive available evidence.
4. Determine permitted selectivity.
5. Generate and order moves.
6. Search moves.
7. Apply any justified reductions, pruning, extensions or re-search behaviour.
8. Establish cutoffs and the result.
9. Store only valid transposition information.
10. Return the result.

This is a working lifecycle, not a finalized execution ordering beyond the
LOCKED precedence in section J. Remaining evidence ownership and how selective
work and re-search fit within move traversal require further design.

### Allocation of search effort

Spend search effort in proportion to the importance and uncertainty of the
decision being resolved. Moves or nodes capable of materially changing the
result may require stronger proof. Multiple reliable signals of low relevance
may justify reduced work. This is not yet a numeric formula or universal
policy.

## OPEN design frontier

The current reasoning sequence is open work, not a set of settled answers:

1. **Invocation and lifecycle details:** concrete request/result and
   cancellation representation consistent with the LOCKED exact Search
   semantics, driver/core separation and the required external lifecycle
   boundary.
2. **Further bound/evidence semantics:** obligations beyond section J's
   initial exact TT rules, including interaction with future qsearch, selective
   Search and PVS. The initial TT bound meanings, applicability and mate-score
   normalization, negamax window transformation and fail-soft return policy
   are LOCKED.
3. **Node evidence model:** what Search genuinely knows at node entry; what
   is derived locally; what may arrive from parent/path context; authoritative
   versus heuristic evidence.
4. **Further transposition-table evidence and policy:** Section J locks
   `TTable` mechanics and initial exact evidence semantics. Non-cutting
   LOWER/UPPER window tightening; use of deeper entries for shallower requested
   depth; cross-generation score/bound reuse; broader safe history/path
   equivalence classes; separate position-only hash-move keying; replacement,
   sizing and statistics policies; parallel/shared TT architecture and Lazy
   SMP TT behaviour remain OPEN. Concrete integration and move-ordering
   implementation require separately authorized work within the locked rules.
5. **Static-evaluation evidence and reliability:** how Search uses an
   evaluator beyond the LOCKED evaluator-independent boundary; confidence in
   static evaluation; calibration or evaluator-specific Search heuristics;
   whether reliability/context signals are needed.
6. **Move-order evidence:** further implications of TT selection, tactical
   status, historical success, move rank and late position, beyond the LOCKED
   distinction between hash-move ordering evidence and score proof.
7. **Selective mechanisms:** reductions; pruning; narrow/probe searches;
   technique-specific eligibility and aggression.
8. **Extensions and re-search:** when earlier assumptions require additional
   proof; when reduced/narrow searches must be widened or deepened.

## Explicitly unresolved techniques and parallelism boundary

The following remain **OPEN**; their conventional implementations are
**not LOCKED** as accepted Search architecture:

- Null-move pruning.
- Late-move reductions (LMR).
- Futility pruning and reverse futility pruning.
- Razoring.
- ProbCut / MultiProbCut.
- Check extensions or other extensions.
- Detailed principal-variation search (PVS) / null-window policy.
- Detailed re-search rules.
- Quiescence design.
- Aspiration-window policy.
- Sophisticated iterative-deepening heuristics beyond the LOCKED initial
  successive-depth progression and completed-result rule.
- Previous-PV ordering policy.
- TT policies beyond section J's initial exact evidence rules, as listed in
  the OPEN frontier, and TT interaction with future qsearch, selective Search
  and PVS. Integration into ExactSearch requires separate authorization.
- Sophisticated move-order policy.
- Evaluator calibration or evaluator-specific Search heuristics.
- Detailed time-management algorithms.
- Parallel Search architecture, including Lazy SMP or other concurrency
  strategies.
- Playing-strength optimization policy beyond the already LOCKED principles.

These may emerge from the contract, be rejected or take materially different
forms. Their presence in existing code or historical reports does not settle
their role in the new Search design.

R005 does not accept conventional practical-engine policies merely because
they are common: deeper-for-shallower score reuse, cross-generation score
reuse, automatic non-cutting bound window tightening, replacement-policy
optimization, speculative/path-insensitive reuse and TT-driven selectivity
are outside the initial exact design. Reconsideration requires explicit
reasoning, evidence and canon changes where material.

**LOCKED parallelism boundary:** Initial development and reference behaviour
are single-threaded. Establish logical Search semantics independently of
parallel execution. Avoid unnecessary architectural assumptions that would
make later concurrency impossible, but parallel Search does not drive the
initial implementation; detailed concurrency architecture remains OPEN.
Existing or experimental root parallelism, Lazy SMP and proof-directed
splitting may provide evidence later; they are outside the immediate
first-principles contract.

## Validation and performance principles

Preserve appropriate exact/reference baselines wherever practical.
Correctness comparisons precede performance claims. Selective techniques
require explicit guard/semantic validation and measurement; being standard
chess-engine practice is not acceptance evidence.

### LOCKED TT-off oracle and exact TT acceptance

ExactSearch must remain independently runnable with Search TT use fully
disabled: TT-off is the exact reference/oracle path; TT-on is optimized exact
execution using proven reusable evidence. TT support must not make TT
mandatory for correctness or reference use.

Initial TT integration acceptance requires strong TT-off/TT-on fixed-depth
equivalence evidence covering:

- Identical fixed-depth score.
- Equivalent, legal best moves, allowing different moves with equal value.
- Legal and semantically consistent principal variations (PVs).
- Repetition/path-sensitive and rule-state fixtures.
- Mate-distance and exact-depth mismatch fixtures.
- EXACT/LOWER/UPPER window fixtures.
- Cancellation/incomplete-node fixtures.
- Actual transposition fixtures demonstrating reduced duplicated work where
  appropriate.

A score difference between TT-off and TT-on is not an accepted optimization
result. It indicates incorrect evidence applicability or a deliberate Search
semantics change requiring separate authorization.

### Performance considerations

Future Search design and implementation must explicitly consider:

- Allocation behaviour and branch behaviour.
- Primitive/data layout, memory locality and cache effects.
- Search-node count and useful work versus speculative/duplicated work.
- JVM/JIT behaviour.
- Synchronization costs when concurrency is eventually introduced.

Higher raw nodes per second (NPS) is not sufficient evidence of stronger
Search. Distinguish mechanical throughput improvements from changes that
alter tree shape or node count. Search performance evidence must distinguish
at least wall time, node count, throughput, changes in the searched tree,
warm versus cold state where relevant, and playing-strength evidence when
eventually available.

### LOCKED headless Search validation and benchmark surface

The programme requires a dedicated headless way to run fixed-depth searches
without the GUI. It may reuse useful mechanics or patterns from perft, such
as named positions, FEN input, warm-up/repetition and timing. Search testing
is a distinct semantic concern and must not be conflated with perft
correctness.

The intended output should support, as appropriate, requested/completed
depth, best move, score, principal variation (PV), searched nodes, elapsed
time and NPS. Add further Search statistics when they become meaningful.
Single-thread reference runs must be deterministic. Performance evaluation
must preserve the distinctions above between elapsed time, node count,
NPS/throughput and changes to the searched tree. A numerically different
score is not automatically an improvement. Use a suite of representative
positions; no single position, including Kiwipete, is the sole optimization
target.

## Canon maintenance rules

Future Codex tasks may update this canon only when the task explicitly
authorizes canon maintenance and its accepted result materially changes the
Search contract. Modify affected sections without rewriting unrelated canon
content. Preserve revision history and the stable filename.

For each material Search-contract change:

1. Update the repository master.
2. Increment the internal canon revision exactly once for the material
   update, continuing the `R001` format; never put it in the filename.
3. Report the resulting revision and that Project Source refresh is required.
4. Have the human manually refresh the ChatGPT Project Source mirror to the
   same revision before dependent Search-design work proceeds. A requested
   refresh is not evidence of completion; confirm synchronization before
   advancing that dependent work.

Ordinary implementation details, bug fixes, experimental runs or measurements
do not require a canon revision unless they materially alter durable Search
design or accepted direction. Canon maintenance does not authorize engine
changes, edits to the historical transplant report or automatic changes to
ChatGPT Project settings/sources.

### Revision history

| Revision | Contract change |
| --- | --- |
| R001 | Established the repository-master first-principles Search canon, evaluator-compatible baseline, LOCKED foundations, TENTATIVE model and OPEN design frontier. |
| R002 | Locked new-implementation boundaries, exact recursive negamax semantics, evaluator and parallelism boundaries, headless validation and later adoption; reconciled deferred techniques and corrected the SeedV6 repository master reference. |
| R003 | Locked the fixed-depth ExactSearch/production-driver separation, simple initial iterative deepening, completed-result retention, lifecycle limits and adaptation, and production adoption with reference/harness preservation; retained advanced policies as OPEN. |
| R004 | Selected handcrafted TTable as the LOCKED mechanical TT baseline, preserving packed primitive storage, scratch, locking, generation/clear and separate eval-cache mechanics; retained Search evidence, integration, replacement and lifecycle policy as OPEN. |
| R005 | Locked initial exact TT evidence applicability, equal-depth bounds, mate normalization, request generations, owner isolation and hash-move separation; preserved the TT-off oracle and OPEN replacement policy. |
