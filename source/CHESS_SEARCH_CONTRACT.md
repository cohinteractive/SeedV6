# SeedV6 Search Contract

Internal revision: **R015**

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

**LOCKED tactical-ordering direction (SR-015):** A legal applicable TT/hash
move has highest precedence and appears once only. Classify remaining
tacticals (captures, en passant and all promotions) using independently
oracle-validated threshold SEE: `SEE >= 0` is good and `SEE < 0` is bad.
Good tacticals precede bad tacticals; both precede quiet moves in the accepted
current tactical policy. Bad tacticals remain searchable. Ordering changes
visitation only, preserving exact Search values and move-set uniqueness;
this evidence does not authorize SEE pruning or other selectivity.

Within each tactical class, order by descending immediate material value:
captured-piece exchange value plus promotion gain (actual promoted-piece
value minus pawn value). En passant includes the captured pawn's value;
capture-promotions include both components, quiet promotions only the gain,
and underpromotions use the actual promoted piece. Use evaluator-independent
exchange/material values appropriate to Search ordering, not evaluator
output. Exact numeric SEE magnitude is not part of the accepted score.

Demoting all SEE-negative tacticals behind quiets is rejected for this
baseline because it caused severe Search-tree regressions. LVA/attacker-value
tie-breaking added no aggregate benefit beyond material-only ordering, and
the researched compact capture-history variant did not provide sufficiently
consistent wall-time benefit. Neither is retained in the preferred baseline;
these conclusions apply under the researched conditions, not universally.

**LOCKED quiet-ordering baseline (SR-016):** Main quiet history alone is the
preferred current quiet-ordering policy. After the legal applicable hash move
and both accepted SR-015 tactical classes, order quiets by higher history
evidence first; equal evidence retains deterministic relative order. Quiet
history must not elevate a quiet above those tactical classes. Preserve compact
primitive state distinguishing moving piece including side, from square and
to square. This is evaluator-independent ordering evidence, not authority for
pruning or reductions.

A completed searched quiet beta-cutoff winner receives a positive update;
earlier searched quiet moves at that node that failed before the cutoff receive
negative updates. Tactical and generated-but-unsearched moves do not update
main quiet history. Incomplete/cancelled work must not publish history effects
as though normal completed Search occurred. A searched quiet hash move may
receive the normal history reward/malus; hash precedence remains independent
of history.

The current accepted update baseline is signed gravity bounded to `+/-16384`,
with magnitude `min(depth, 64)^2`, positive for reward and negative for malus:
`h += bonus - h * abs(bonus) / 16384`, using safe arithmetic.

Immediate continuation history, two per-ply killer moves and compact
countermove ordering were researched and are not retained in the preferred
baseline under the tested conditions. This is not a universal rejection or
permanent prohibition. Relative history and richer contextual histories remain
unproven; they were not required for SR-016 closure.

Independent Search owners must not accidentally share mutable quiet-history
state. This does not settle future parallel/shared-history architecture, which
remains SR-036 work.

**OPEN quiet-history lifecycle integration:** The current implementation resets
quiet history per independent fixed-depth ExactSearch invocation, so each
SearchDriver iterative-deepening iteration begins with reset history. This does
not lock the final production lifecycle. Persistence across iterative-deepening
iterations within one SearchDriver request, persistence across top-level
requests, and aging/reset on new game or other lifecycle boundaries remain OPEN.
Future decisions must preserve independent owner isolation and
deterministic/reference requirements where applicable.

**LOCKED overall move-order architecture (SR-014):** The accepted precedence is
legal applicable hash move, then SEE-good tacticals by descending immediate
material, then SEE-bad tacticals by descending immediate material, then quiets
by main quiet history, using the SR-015/SR-016 policies above. Equal material or
history evidence retains original generated order within its class. Every
searched legal move appears at most once. These conceptual ordering classes
are distinct from their physical generation and consumption, governed by the
accepted SR-017 staged-generation/lazy-selection mechanics in section L.
Ordering remains visitation evidence, not pruning authority; this architecture
does not introduce a universal move-confidence scalar.

SR-014, SR-015, SR-016 and SR-017 are IMPLEMENTED in production/default
TT-enabled ExactSearch. CONTROL/reference and research modes remain available.
These ordering conclusions do not settle previous-PV ordering, qsearch,
selectivity or the OPEN history lifecycle and parallelism boundaries.

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
- **Window and return:** A full-window child receives the conventional negated,
  reversed negamax window `[-beta, -alpha]`; accepted PVS scouts use the narrow
  window specified below. Exact Search uses fail-soft returns: a
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

#### LOCKED exact traversal policy (SR-003)

TT-disabled ExactSearch retains ordinary ordered alpha-beta as the independent
exact/reference/oracle traversal. For normal TT-enabled ExactSearch, PVS is the
implemented production/default traversal under the accepted ordering architecture
(section A), staged/lazy mechanics (section L) and TT policy (section J).
Do not introduce position-specific, depth-specific or heuristic switching
between alpha-beta and PVS. This is exact Search, not selectivity.

At a positive-depth TT-enabled node requiring move search, including the root:

1. Search the first actually searched legal move in the accepted order with
   the normal full negamax window `[-beta, -alpha]`.
2. Search each later move initially with the null/scout window
   `[-alpha-1, -alpha]`, using the node's current alpha.
3. Negate the child return to obtain the node's move score. A scout score
   `<= alpha` fails low: continue. A scout score `>= beta` returns the fail-soft
   cutoff immediately, without a full re-search. A strict improvement
   `alpha < score < beta` requires a full-window re-search with
   `[-beta, -alpha]` before accepting the improved value/PV.

All child calls, including scouts and re-searches, use `depth - 1`; no depth
reduction is introduced. Returns remain fail-soft. A completed full re-search
supplies the improved PV. A fail-low scout does not replace the discovered PV;
a scout cutoff may expose only a legal valid searched-move prefix, not an
allegedly exact continuation. Section J's invocation-specific TT classification
applies to every scout and re-search.

Existing mate scoring and normalization, SearchKey/value-equivalence identity,
equal-depth/current-generation TT applicability, cutoff-only TT bounds,
hash-move ordering, static-leaf TT exclusion, cancellation/completion semantics
and evaluator independence remain unchanged.

SR-003 is IMPLEMENTED in production/default TT-enabled ExactSearch. The accepted
conclusion is principally a Search-tree result under the tested architecture,
not a claim that PVS universally outperforms alpha-beta. MTD(f), aspiration,
LMR/reduced-depth re-search, qsearch, other selective pruning/probe mechanisms,
previous-PV/iterative-deepening policies beyond existing decisions, and parallel
Search remain OPEN separate research.

### J. Transposition-table boundary, mechanics and exact evidence

The initial exact reference Search does not depend on a transposition table
(TT) and must remain independently runnable with Search TT use fully disabled.
R004 locks the mechanical substrate; R005 locks the initial exact-Search
interpretation and applicability of its evidence below. Neither revision
authorizes TT integration implementation or migration by itself, changes the
independent TT-off ExactSearch reference semantics, or accepts replacement
policy as optimal. Existing TT behaviour must not be silently imported merely
because it already exists elsewhere in SeedV6.

The current single-thread advanced-TT research is settled by the policies
below. Parallel/shared TT semantics remain OPEN and are deferred to SR-036
Parallel Search architecture; they do not block this single-thread outcome.

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

**LOCKED current single-thread replacement policy:** Retain the current
handcrafted `TTable` replacement mechanics. Do not add age-refresh,
evidence-strength, collision-depth or other replacement-policy changes.
This is the accepted current single-thread policy, not a claim of universal
optimality for future parallel Search. Hash-move accepted-write behaviour is
settled below.

**LOCKED fixed Search TT sizing:** The accepted fixed default is **64 MiB
requested**, now implemented in place of the former 192 MiB requested default.
With the current power-of-two capacity calculation and 24-byte logical entries,
a 64 MiB request maps to 2,097,152 slots and 48 MiB of logical entry storage,
excluding JVM/array/locking overhead.
Do not introduce adaptive sizing, evaluator-specific sizing, RAM detection
or configuration redesign.

**LOCKED TT statistics and prefetch boundary:** Production Search/`TTable`
remains statistics-free. TT diagnostics remain external/temporary unless a
future production consumer creates a new explicit requirement. Add no
production TT prefetch or early-touch behaviour for the current Java 21
architecture.

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
3. If `depth <= 0`, static evaluation directly, with no Search TT score/bound
   probe or store.
4. For positive depth, applicable Search TT evidence under the LOCKED rules.
5. Move generation/search as applicable.

A TT entry must not override current checkmate, stalemate, repetition,
rule-50 or any other applicable terminal condition. This is semantic
precedence, not incidental method ordering: legal-move generation needed to
establish terminal status may occur during adjudication.

#### LOCKED current static-depth TT participation

At the current ExactSearch nonterminal static-evaluation boundary (`depth <= 0`),
Search TT score/bound evidence is neither probed nor stored. After required
cancellation/interruption handling and current terminal/draw adjudication,
the node resolves directly through static evaluation. There is no move search
at this static leaf, so Search TT hash-move ordering has no role there; the
existing hash-move rules for positive-depth nodes remain unchanged.

This is the uniform evaluator-independent policy for HCE, NNUE and currently
supported neural evaluators. No evaluator-specific leaf-TT branch, threshold,
runtime evaluator-cost test or configuration switch is introduced. The empirical
result removes the need for evaluator-specific depth-zero TT behaviour at this
stage, preserving the LOCKED evaluator boundary and implementation economy.

The completed empirical programme strongly favoured disabling depth-zero
Search TT participation for HCE and favoured disabling it for trained NNUE.
Trained BRN-2 was non-regressing under NO_LEAF_TT in the final focused comparison,
with several materially better cases and no demonstrated material BASELINE
advantage. No tested accepted evaluator established a reproducible material
preference for full depth-zero Search TT participation. Under the current exact
Search architecture and measured evaluator set, uniform exclusion is therefore
the strongest-supported mechanically appropriate policy, removing recurring
depth-zero probe/store cost.

This decision changes only Search TT score/bound participation at the current
nonterminal static depth boundary. Positive-depth Search TT probing/storage,
terminal/draw precedence, equal-depth evidence semantics, generation semantics,
mate normalization, SearchKey/value-equivalence identity, cancellation/completion
rules and owner isolation remain unchanged. The separate evaluation cache and
`TTable` mechanics and locking are unaffected. The current single-thread
advanced-TT policies are settled separately in this section; TT questions tied
to future Search architecture retain their OPEN boundaries.

Quiescence Search and its TT participation policy remain **OPEN**. If qsearch is
introduced, its TT semantics must be designed separately; they must not silently
inherit either this static-leaf exclusion or conventional engine practice.

This is not a general judgment against shallow TT entries, depth-zero reuse,
TT-cached neural leaves or TT participation at leaves of future Search forms.
Future contrary evidence may justify an explicit later canon revision.

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

For current single-thread exact score/bound reuse, **stored remaining depth
must equal requested remaining depth** (`storedDepth == requestedDepth`).
`storedDepth >= requestedDepth` is not accepted as score/bound qualification:
different fixed depths have different evaluation horizons and need not have
the same minimax value. A depth mismatch makes the stored score/bound
inapplicable as proof; deeper-for-shallower score/bound reuse is rejected for
the current architecture. Hash-move ordering across depth mismatch remains
permitted under the rules below.

#### LOCKED bound meaning and conservative probes

Search classifies a completed node score `S` against its original caller
window `[originalAlpha, originalBeta]`, before local alpha changes caused by
searched moves:

| Completed score | Stored type | Meaning for the exact fixed-depth value |
| --- | --- | --- |
| `S <= originalAlpha` | UPPER | Value is at most the stored score. |
| `S >= originalBeta` | LOWER | Value is at least the stored score. |
| Otherwise | EXACT | The exact value was established. |

For accepted ordinary exact PVS, each scout or full re-search invocation
classifies its evidence against that invocation's own original alpha/beta
window. Narrow-window UPPER/LOWER evidence must not be misrepresented as EXACT
evidence for a wider window; a non-cutting scout bound cannot replace the
required full re-search. This settles ordinary exact PVS interaction only,
not future narrow-window drivers or selective Search evidence.

These meanings apply only after identity/state, generation, equal-depth,
mate-score interpretation and all other applicability requirements succeed.
Search owns these semantics; do not reinterpret the type constants inside
`TTable`.

A qualifying EXACT may return immediately.
A qualifying LOWER may return/cut off only when its decoded score is
`>= beta`; a qualifying UPPER may return/cut off only when its decoded score
is `<= alpha`. Otherwise the stored bound does not tighten alpha or beta.
This cutoff-only LOWER/UPPER policy is the accepted current single-thread
policy; non-cutting bound tightening is not adopted.

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
with its own generation. Score/bound reuse requires the stored
generation to equal the current Search request generation. Older-generation
scores/bounds are not proof for the current request; cross-generation
score/bound reuse is rejected for the current single-thread architecture.
R004's low-8-bit generation storage and wrap clearing remain unchanged.

Each independently operating Search owner has its own Search `TTable`
instance/state. In particular, independent Play and Training Search ownership
must not be coupled by TT reuse. This does not settle parallel-tree sharing,
parallel/shared TT architecture or Lazy SMP TT behaviour; those remain OPEN
under SR-036 Parallel Search architecture, independently of the settled
current single-thread TT programme.

#### LOCKED hash-move evidence and completion boundary

A matching TT hash move is ordering evidence, not mathematical proof of the
current Search value or proof that the move is currently best. Subject to
valid position identity and legal-move validation, it may be considered for
ordering despite a depth mismatch, a non-cutting bound, or an older stored
generation. The move must be legal in the current position before promotion
in move ordering. Score evidence and hash-move retrieval retain the same
conservative history-sensitive Search identity. Separate position-only move
keying is not adopted in the current baseline.

Retain current accepted-write hash-move behaviour: an accepted Search entry
write stores the supplied hash move with its score/metadata; a rejected write
leaves the existing move unchanged. Do not add DEEPEST_MOVE, EXACT_MOVE,
sticky move retention or other preservation rules.

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

### L. Implementation economy and hot-path mechanics

Building Search from first principles does not imply implementation layering
or one Java type per conceptual responsibility. Semantic responsibilities may
remain conceptually distinct without separate runtime objects, classes,
interfaces, enums, wrappers or policy objects. Use the mechanically smallest
and most direct implementation that correctly preserves the LOCKED semantics
and independently useful correctness/reference boundaries.

Codex may implement research/prototype variants, tests and evidence-producing
experiments, and production Search where appropriate. Experimental success or
useful evidence does not by itself accept the implementation's structure as
production architecture. Experimental classes, wrappers, objects, abstractions,
data or sorting structures and control-flow organization acquire no
architectural authority merely through research use.

Accepted semantic or algorithmic evidence and research conclusions can survive
replacement of the experimental implementation that produced them. Production
hot-path mechanics may be handcrafted or mechanically reimplemented in a more
direct SeedV6-specific style, subject to explicit LOCKED mechanical decisions.
Such replacements must preserve the established semantics, accepted conclusions
and required correctness/reference boundaries, with appropriate correctness
validation and measured performance evidence, including JVM/JIT scrutiny. The
implementation-economy principles below apply regardless of author or research
provenance.

Apply mechanical scrutiny according to execution frequency:

- Request/root-level abstractions may be reasonable where useful.
- Per-iteration work should remain lean.
- Per-node work should be primitive and direct by default.
- Per-move work receives the strongest scrutiny.

In recursive/hot paths prefer primitives, primitive arrays, packed state,
reusable caller-owned scratch, direct control flow and already-derived state.
Avoid hot-path allocation, boxing, collections, streams, temporary result or
wrapper objects, repeated conversions and unnecessary dynamic dispatch or
abstraction when they exist only to model concepts more conventionally.
Hot-path allocation or indirection requires a concrete correctness/ownership
need, a genuinely necessary boundary, or measured benefit. Conventional
software-engineering cleanliness alone does not justify recurring Search cost.

Frequently needed derived information should preferably be maintained
incrementally when this reduces total work and can be done safely, rather than
repeatedly reconstructing or traversing equivalent state. Incremental
maintenance is not unconditional: additional maintained state can cost update
work, footprint or cache locality and remains subject to correctness and
measurement.

Where implementations preserve equivalent semantics, prefer the one requiring
less computation, allocation, indirection, memory traffic and state
reconstruction unless evidence demonstrates a benefit from the more elaborate
alternative. Source-level class count is not itself a performance metric.
Abstractions and classes are not automatic virtues or design goals; their
induced runtime work must be justified, especially in hot paths.

Runtime layers may be collapsed while their semantic responsibilities remain
intact. Simplification must not weaken exact Search semantics,
cancellation/completion behaviour, evaluator independence, TT evidence or
applicability rules, ownership isolation, or the independently invocable
TT-off exact/reference path. Correctness remains non-negotiable: mechanical
simplicity does not authorize unsafe shortcuts, and performance claims require
evidence rather than assumption.

This Search-wide invariant complements section J's detailed TT mechanics and
the validation and performance principles below. It does not settle OPEN
Search techniques or TT policies, prescribe concrete Search class collapsing
or allocation-removal work, or authorize implementation changes. Those remain
later inspection, design and implementation work governed by this invariant.

**LOCKED current recursive Search mechanics (SR-002):** The production/default
ExactSearch mechanical baseline remains recursive. A serious primitive explicit
flat/iterative implementation was researched with identical exact Search semantics
and tree behaviour under OpenJDK 21 on Ryzen 5 5500 / Windows 11, using HCE and
cold fixed-depth 64 MiB TT conditions. The strongest refined local-leaves candidate
remained modestly slower, with no convincing repeatable aggregate performance
benefit or meaningful allocation/GC advantage, while adding explicit frame and
state-machine work and maintenance complexity. SR-002 is therefore REJECTED under
these researched conditions; flattened Search is not adopted for the current
baseline.

This is a researched current-baseline conclusion, not a universal prohibition.
Reconsider only after a concrete material JVM, platform, Search architecture or
other dependency change plausibly alters the cost/benefit; periodic retesting
alone is not grounds to reopen. Section G's independent recursive exact/reference
requirement remains intact. Experimental `FlatExactSearch` remains research
evidence unless separately adopted; its presence grants no production
architectural authority.

**LOCKED move-generation and consumption mechanics (SR-017):** Prefer staged
legal generation with lazy next-best selection: select the highest-ranked
remaining move only when Search needs another move, rather than fully sorting
an already-generated list. Use primitive, allocation-free recurring mechanics.
Once a generated phase's ordering evidence is sampled, keep it fixed while
that phase is consumed; do not reread mutable history between its siblings.

At non-check nodes, generate legal tacticals first and defer legal quiet
generation until required. A hash/tactical cutoff can avoid quiet generation
entirely; otherwise generate and lazily consume the quiet phase when needed.
At checked nodes, use complete legal evasions rather than an artificial
tactical/quiet split.

At the current ExactSearch static boundary, terminal/nonterminal status must
still be established before static evaluation. At a non-check node, a legal
tactical proves nonterminal status without quiet generation; if no legal
tactical exists, generate quiets to distinguish a legal quiet-only position
from stalemate. Complete evasions establish mate/nonterminal status in check.
Cancellation, terminal and draw precedence remain unchanged. This legal-move
existence handling searches no children and is not qsearch, which remains OPEN.

The legal applicable hash move remains globally first and appears exactly
once. Exact legal membership is required: a tactical hash may be resolved
against the tactical phase, while a quiet hash may force quiet generation
earlier than otherwise needed. Staging does not weaken section J's hash rules.

When quiet generation is genuinely deferred until after tactical children,
sample quiet-history evidence when the quiet phase is materialized. It may
reflect history updates from those earlier child searches; after sampling,
keep it fixed for the remaining quiet siblings. This differs from the earlier
full-generation baseline's node-entry snapshot. Exact fixed-depth Search value
is preserved, but the searched tree and node count need not be identical.
This within-node timing does not settle history persistence across iterations
or requests.

Current insertion ranking, primitive handcrafted full sorting and full-generation
lazy selection received adequate research. The Seed-specific handcrafted hybrid
full sorter was a first-class candidate, including bounded insertion/quicksort
crossover testing, but did not earn its added complexity over lazy selection as
the current baseline. This is a researched conclusion, not universal inferiority
or a prohibition on evidence-led reconsideration. Lazy selection was preferred
among full-generation alternatives; staged generation plus lazy selection then
materially outperformed that baseline and is the implemented production/default
baseline.

Use the existing tactical/quiet/evasion generation capabilities. Measured repeated
setup between tactical and quiet calls did not justify another prerequisite
experiment. No prepared-generation context or Board/Gen redesign is locked;
future measured mechanical optimization may revisit generator internals while
preserving accepted Search semantics.

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
LOCKED exact traversal in section I, precedence in section J and staged/lazy
move traversal in section L. Remaining evidence ownership and how selective
work and reduced-depth re-search fit within move traversal require further
design.

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
2. **Further bound/evidence semantics:** obligations beyond sections I and J's
   current exact PVS/TT rules, including interaction with future qsearch,
   selective Search, MTD(f), aspiration and other narrow-window drivers. The TT
   bound meanings, applicability and mate-score normalization, ordinary exact
   PVS scout/re-search rules, negamax window transformation and fail-soft return
   policy are LOCKED.
3. **Node evidence model:** what Search genuinely knows at node entry; what
   is derived locally; what may arrive from parent/path context; authoritative
   versus heuristic evidence.
4. **Further transposition-table evidence and policy:** Section J locks
   `TTable` mechanics and current single-thread policy. TT interaction with
   future qsearch, selective Search, MTD(f), aspiration and other narrow-window
   drivers; broader proven history/path equivalence classes for future Search
   architecture; partial-work non-score evidence; and parallel/shared TT
   architecture, including Lazy SMP, remain OPEN. Ordinary exact PVS interaction
   is settled by SR-003. Parallel/shared TT is deferred to SR-036 and does not
   block the settled current single-thread programme. The accepted 64 MiB requested
   fixed default is implemented.
5. **Static-evaluation evidence and reliability:** how Search uses an
   evaluator beyond the LOCKED evaluator-independent boundary; confidence in
   static evaluation; calibration or evaluator-specific Search heuristics;
   whether reliability/context signals are needed.
6. **Move-order evidence:** further implications of TT selection, tactical
   status, historical success, move rank and late position, beyond the LOCKED
   distinction between hash-move ordering evidence and score proof and
   section A's accepted SR-014/SR-015/SR-016 architecture and section L's
   SR-017 mechanics.
7. **Selective mechanisms:** reductions; pruning; narrow/probe searches;
   technique-specific eligibility and aggression.
8. **Extensions and re-search:** when earlier assumptions require additional
   proof; when reduced/narrow searches must be widened or deepened beyond
   section I's settled ordinary exact PVS re-search rules.

## Explicitly unresolved techniques and parallelism boundary

The following remain **OPEN**; their conventional implementations are
**not LOCKED** as accepted Search architecture:

- Null-move pruning.
- Late-move reductions (LMR).
- Futility pruning and reverse futility pruning.
- Razoring.
- ProbCut / MultiProbCut.
- Check extensions or other extensions.
- MTD(f) and other zero-window drivers beyond accepted ordinary exact PVS.
- Reduced-depth and other re-search rules beyond section I's ordinary exact PVS.
- Quiescence design.
- Aspiration-window policy.
- Sophisticated iterative-deepening heuristics beyond the LOCKED initial
  successive-depth progression and completed-result rule.
- Previous-PV ordering policy.
- TT policies beyond section J's current single-thread rules, as listed in
  the OPEN frontier, and TT interaction with future qsearch, selective Search
  and downstream narrow-window drivers. Ordinary exact PVS interaction is
  settled and implemented; future changes require separate authorization.
- Move-order policy beyond the accepted SR-014/SR-015/SR-016 architecture and
  SR-017 mechanics; broader quiet-history lifecycle integration.
- Evaluator calibration or evaluator-specific Search heuristics.
- Detailed time-management algorithms.
- Parallel Search architecture, including Lazy SMP or other concurrency
  strategies.
- Playing-strength optimization policy beyond the already LOCKED principles.

These may emerge from the contract, be rejected or take materially different
forms. Their presence in existing code or historical reports does not settle
their role in the new Search design.

Section J settles the current single-thread TT policy; conventional
practical-engine choices do not override it. Speculative/path-insensitive reuse
and TT-driven selectivity remain outside the exact design. TT questions tied
to future Search architecture remain OPEN. Reconsideration of settled policy
requires explicit reasoning, evidence and canon changes where material.

**LOCKED parallelism boundary:** Initial development and reference behaviour
are single-threaded. Establish logical Search semantics independently of
parallel execution. Avoid unnecessary architectural assumptions that would
make later concurrency impossible, but parallel Search does not drive the
initial implementation; detailed concurrency architecture remains OPEN under
SR-036 Parallel Search architecture.
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
disabled: TT-off ordinary ordered alpha-beta is the exact reference/oracle
path; TT-on is optimized exact execution using proven reusable evidence, with
PVS as the production/default traversal under section I's accepted scope. TT
support must not make TT mandatory for correctness or reference use.

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
| R006 | Locked Search-wide implementation economy and frequency-scaled hot-path mechanics while preserving semantic, correctness and reference boundaries; left concrete simplification and OPEN policies to later work. |
| R007 | Locked uniform exclusion of Search TT score/bound probing and storage at the current nonterminal ExactSearch static depth boundary; left future qsearch TT policy OPEN. |
| R008 | Settled current single-thread advanced TT policy: equal-depth/current-generation evidence, cutoff-only bounds, retained replacement/hash-move/shared identity, no production statistics/prefetch and accepted 64 MiB requested fixed default pending implementation; deferred parallel/shared TT to SR-036. |
| R009 | Locked the accepted SR-015 direction: legal hash first, SEE-good then SEE-bad tacticals ahead of quiets, immediate material ordering within each tactical class; production adoption pending, quiet ordering and mechanics unresolved. |
| R010 | Locked accepted SR-016 main quiet history alone: side/piece/from/to identity and bounded-gravity quiet cutoff reward/malus; continuation, killers and countermoves not retained in the current baseline; lifecycle integration and SR-017 mechanics remain OPEN; production adoption pending, CONTROL unchanged. |
| R011 | Separated accepted research evidence from experimental implementation structure; permitted validated production reimplementation while preserving LOCKED decisions, correctness/reference boundaries and hot-path implementation economy. |
| R012 | Locked accepted SR-014 overall ordering and SR-017 staged generation/lazy selection, including deferred quiet-history sampling; preserved exact value, unresolved lifecycle/selectivity boundaries and separate production adoption with CONTROL unchanged. |
| R013 | Accepted SR-003 PVS for current TT-enabled exact Search with fail-soft scout/full-window re-search and invocation-specific TT evidence; retained TT-off ordered alpha-beta oracle, separate production adoption and OPEN downstream narrow/selective research. |
| R014 | Recorded production adoption of SR-003 PVS for TT-enabled ExactSearch and SR-014/015/016/017 accepted ordering/staging mechanics; retained the TT-off ordered-alpha-beta oracle and recorded current per-fixed-depth quiet-history reset while broader lifecycle policy remains OPEN. |
| R015 | Closed SR-002 as REJECTED under researched conditions, retaining recursive production mechanics and the independent exact recursive/reference baseline; corrected stale 64 MiB TT-default implementation status. |
