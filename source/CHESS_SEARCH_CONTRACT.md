# SeedV6 Search Contract

Internal revision: **R004**

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
legitimately affect Search decisions. Beyond the locked invocation and
mate-distance semantics below, exactly which parent/path information crosses
the node boundary remains OPEN.

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

### J. Transposition-table boundary and LOCKED mechanical substrate

The initial exact reference Search does not depend on a transposition table
(TT). TT score/bound use is deferred until applicability, depth qualification,
bound semantics, mate-score handling and related evidence rules are explicitly
designed. Existing TT behaviour must not be silently imported merely because
it already exists elsewhere in SeedV6.

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
  values are irrelevant because validity resides in `data[]`. These mechanics
  do not decide when production Search advances generations.
- **Separate evaluation cache:** Evaluation caching deliberately uses a
  separate `TTable` instance, retaining the raw-score `TYPE_EVAL` / `probeEval`
  representation and caller-managed semantics. Its validity and clear
  behaviour differ from normal Search entries and remain caller-managed;
  do not generalize them into a single abstract TT policy or mix evaluation
  and Search entries in one physical table.

**OPEN Search evidence and policy:** `TTable` retrieves and stores mechanical
evidence only. Search callers and later TT evidence/lifecycle design own
depth sufficiency, EXACT / LOWER / UPPER applicability to the current window,
cutoff validity, hash-move usability, mate-score normalization and
interpretation, repetition and other path dependence, node completion
requirements, Search lifecycle policy, replacement-policy correctness and
evaluator/Search score semantics. The current replacement behaviour is
inherited mechanically for now; it is not a LOCKED first-principles Search
decision or accepted optimal policy. Replacement remains OPEN for later
empirical and design work. The OPEN frontier below retains the unresolved
qualification and use of stored evidence; selecting mechanics settles none
of those questions or authorizes TT integration into ExactSearch.

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

This tentative model describes possible later evidence-driven Search. TT and
selective stages are conditional on future accepted design; they are not
requirements of the initial exact reference implementation and cannot
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

This is a working lifecycle, not a finalized execution ordering. Refine its
remaining details as TT semantics and evidence ownership are locked,
including how selective work and re-search fit within move traversal.

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
2. **Further bound/evidence semantics:** EXACT / LOWER / UPPER qualification
   and caller/callee evidence obligations, including TT use and mate-score
   handling. The negamax window transformation and fail-soft return policy
   are already LOCKED.
3. **Node evidence model:** what Search genuinely knows at node entry; what
   is derived locally; what may arrive from parent/path context; authoritative
   versus heuristic evidence.
4. **Transposition-table evidence and policy:** Section J locks the `TTable`
   mechanical substrate only. Position-evidence applicability; path-dependent
   exclusions, including repetition-derived results; depth qualification for
   score/bound reuse; EXACT / LOWER / UPPER caller/callee semantics beyond
   their names/constants; alpha/beta cutoff applicability; mate-score
   store/probe normalization; whether and when hash moves are usable
   independently of score evidence and how their legality is validated;
   incomplete-node storage policy beyond the LOCKED general completion
   principles; TT ownership across iterative-deepening iterations;
   Search-driver generation advancement; replacement policy; sizing policy;
   statistics policy; integration into ExactSearch; and TT-based move
   ordering all remain OPEN.
5. **Static-evaluation evidence and reliability:** how Search uses an
   evaluator beyond the LOCKED evaluator-independent boundary; confidence in
   static evaluation; calibration or evaluator-specific Search heuristics;
   whether reliability/context signals are needed.
6. **Move-order evidence:** what TT selection, tactical status, historical
   success, move rank and late position actually imply; ordering metadata
   versus proof.
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
- TT integration into ExactSearch, TT-based move ordering, evidence/applicability,
  replacement and lifecycle policy, sizing and statistics; section J locks
  only the mechanical substrate and its defensive/performance boundary.
- Sophisticated move-order policy.
- Evaluator calibration or evaluator-specific Search heuristics.
- Detailed time-management algorithms.
- Parallel Search architecture, including Lazy SMP or other concurrency
  strategies.
- Playing-strength optimization policy beyond the already LOCKED principles.

These may emerge from the contract, be rejected or take materially different
forms. Their presence in existing code or historical reports does not settle
their role in the new Search design.

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
