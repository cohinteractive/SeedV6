# SeedV6 Search Contract

Internal revision: **R034**

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

When explicitly launched with autonomous Search programme authority, Codex uses
this repository master and [CHESS_SEARCH_RESEARCH_FRONTIER.md](CHESS_SEARCH_RESEARCH_FRONTIER.md)
to select and complete successive admitted features under the frontier's
selection, evidence, disposition and feature-closeout rules. Each feature remains
a separate work unit. At every feature boundary, Codex must re-read both current
repository masters before selecting the next feature; no fresh GPT selection or
per-feature Project Source refresh is required. Mirror lag does not limit that
repository-grounded continuation. External handback and subsequent GPT use follow
the synchronization requirements under Canon maintenance rules below.

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

SR-018 and SR-019 explicitly calibrate selective policies for HCE only. The production
adapter chooses their composition at construction; the recursive evaluator interface
remains independent of evaluator type. Neural and uncalibrated evaluators do
not inherit the HCE margin or pruning authority.

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
Section K records SR-005's rejection of additional explicit previous-PV/best-move
ordering under the current architecture. Selectivity and the history lifecycle
and parallelism boundaries remain OPEN. SR-001's qsearch disposition is recorded
in section I.

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

SR-008 accepts orthogonal invocation facts, mathematical proof/bound evidence,
Search provenance and relevant path context, and heuristic/predictive evidence.
The LOCKED node/evidence model below keeps these dimensions distinct; their
predictive usefulness alone does not authorize selective mechanisms.

### D. Eligibility and aggression are separate

For each selective technique distinguish whether the node/move is eligible
from how aggressively the technique should apply once eligible. Do not
collapse both questions into one opaque conditional.

### E. No universal aggression/confidence scalar is assumed

Shared node evidence may support several techniques, but each may interpret
that evidence differently. Do not prematurely reduce the complete evidence
model to one universal confidence number.

### F. Parent and search-path context may be evidence

A node need not exist in informational isolation. Beyond the LOCKED invocation,
mate-distance and TT value-equivalence requirements below, future mechanisms
may carry parent/path information only for a concrete consumer with evidence
of useful information beyond already available local facts. Prefer information
already present locally or cheaply derivable from current primitive Search state.

Under SR-008B's single-thread HCE conditions, parent move ordinal supplied the
clearest additional conditional information, with substantial position dependence.
Parent check state showed limited conditional association; incoming tactical/quiet
state became weak after conditioning. Incoming first/later status was redundant
when actual PVS provenance was known. No general expected-cutoff/node-role
predictor was justified. These findings do not justify speculative carried
context; any later parent-to-child state must also satisfy section L's economics.

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

Ordinary constructors preserve this exact contract with or without TT. The
explicitly named selective factories are separate modes of the same mechanics;
the HCE production adapter composes SR-018 with SR-019. Their completed results
carry selective provenance and must not be interpreted as exact proof.

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
- **Current leaves (SR-001):** At `depth <= 0`, the production/default and
  static-reference paths resolve nonterminal positions through static evaluation
  after required terminal/draw adjudication. Researched Quiescence Search is
  not adopted into this production boundary; its research/reference path
  remains independently available under the disposition below.
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

#### LOCKED repetition, cycles and history value semantics (SR-010)

Ordinary exact Search preserves SeedV6's existing game adjudication: after
legal exhaustion resolves checkmate/stalemate, a current formal threefold or
halfmove clock at least 100 is terminal score zero. The current product has no
optional claim action or pre-move claim choice. This records the existing
SeedV6 rule, not a redesign of external chess rules. Insufficient-material
adjudication remains separately owned and retains its precedence/composition.

The ordinary repetition domain is the supplied real played-game prefix,
including the current root exactly once, followed by newly searched legal
positions. Prefix and Search-path occurrences have identical authority.
Unknown history before a supplied initial/FEN position is not manufactured.
Twofold alone is nonterminal; fixed nominal depth already terminates cycles.
No Search-only twofold/cycle-to-zero convention is adopted as exact proof.

Board/Zobrist identity, formal repetition identity and Search-value identity
are distinct. Repetition compares piece placement, side to move, castling
rights and an EP opportunity only when a legal EP capture exists, including
king-safety constraints. The raw board key also distinguishes an unusable EP
field. Search-value identity must additionally preserve evaluator-relevant
board/status state and future draw behaviour.

For an identical complete board/status and fixed evaluator definition at equal
remaining depth, the occurrence counts of repetition identities in the active
legal-history domain suffice for ordinary exact value equivalence after mate
normalization. Chronological order is not required by the current recurrence:
terminal tests inspect the current count/board, static leaves inspect the
board, a nonzeroing legal child increments one count, and a zeroing child
starts a singleton domain. Induction over remaining depth preserves all child
values and the negamax choice. Counts capped at three also suffice. This is a
sufficient equivalence, not necessarily the coarsest finite-horizon partition.
Neither board identity nor the current position's count alone suffices: prior
counts of another reachable position can change a future terminal outcome.
New history-sensitive consumers must establish their own compatibility.

The current conservative SearchKey is retained: an incrementally maintained
ordered fingerprint of canonical repetition keys in the halfmove window,
combined with the raw board key and full board status. A researched multiset
fingerprint proved broader legal-history reuse but did not establish a material
production-size economic benefit. This does not reject the count-equivalence
proof. Hash fingerprints remain probabilistic; full-key comparison and all
section J applicability rules remain mandatory. Hash moves retain this same
identity; no separate position-only move table is introduced.

Full rule-clock/status identity is retained. Although `clock + depth < 100`
excludes reaching rule50 when leaves ignore the clock, supported evaluators
can read the complete status, including clock, fullmove and raw EP state.
That rule-only observation does not authorize general horizon clock masking.
SR-018/SR-019 continue to use their real-clock guards. Remaining depth stays
in the exact-equality TT applicability test; it is not duplicated in the key.
Real ply stays separate and is removed from stored mate values by section J's
normalization, not added to history identity.

The implemented detection mechanism retains primitive history keys and adds
primitive predecessor links through fixed hash buckets. Each push links the
new index; pop restores the previous head. Count queries follow decreasing
indexes, compare full repetition keys, and stop at the existing halfmove or
synthetic boundary. Bucket collisions add work, never occurrences. This is an
exact scan acceleration, with no per-node allocation or semantic count cap.
Pawn/capture clock-zero boundaries remain sufficient. Castling-right loss can
justify a tighter boundary because rights cannot return, but the current
conservative longer history is retained without additional root metadata.

PVS scouts and full re-search share one real-history push; finally-pop restores
it on every unwind, including cancellation. Same-request iterative deepening
reinitializes the same root prefix and preserves its established TT generation.
Section J's cancellation/terminal/static/TT precedence, NO_LEAF_TT, fail-soft
returns, equal-depth proof, original-window classification and mate rules are
unchanged. A known terminal zero still stores UPPER/LOWER/EXACT according to
its own original caller window when a completed invocation is stored.

Upcoming repetition has no independent Search authority. A legal move known
to produce a formal-draw child can at most supply that child's ordinary
terminal value with correct precedence; a nonterminal repeat supplies no
draw-score, pruning, cutoff or ordering authority. Availability of a draw
continuation does not make its parent drawn: minimax can choose another move.
No separate upcoming-repetition mechanism is adopted.

SR-018 is unchanged: the synthetic pass is not a repetition occurrence,
pre-barrier legal history is isolated, later legal synthetic-domain positions
can establish local threefold, ordinary TT is excluded throughout that subtree,
and unwind restores the barrier/prefix exactly. Selective provenance and
affected-ancestor write suppression remain unchanged. TT-off exact Search and
the independent oracle remain available. No other frontier feature is begun.
Reproducible proof, adversarial fixtures, observations, timings and external
comparison are at `C:/Users/Central/Documents/SeedV6-SR010-2026-10-01/REPORT.txt`.

#### LOCKED mate-domain restriction (SR-011)

SR-011 is **IMPLEMENTED** for the simple post-terminal score extrema in normal
TT-enabled ExactSearch and the separately selective HCE factories. This is exact
fixed-depth mathematics, not a selective prediction. TT-off ordered alpha-beta
and explicitly configured research mechanics/traversals retain the unmodified
independent control. The separate qsearch research recurrence is excluded.

Let `M` be base mate magnitude, `S` the hard ordinary-score maximum, `P` real
root/path ply and `D` remaining nominal depth. Current checkmate at ply `T`
returns `-M + T`; a winning ancestor sees `M - T`. Once current terminal/draw
outcomes have been excluded, future winning mates have odd positive distances
`d <= D`, and future losing mates have even positive distances `d <= D`.
The complete envelope includes the ordinary range and those reachable mates.

The inspected implementation has `M = 32768`, `S = 32511`, inclusive mate bands
starting at `+/-32512`, maximum root/path ply 256 and infinity `32769`. All
supported evaluator mappings and the ExactEvaluator boundary enforce the ordinary
band; HCE additionally rejects magnitudes above 30000. Invalid evaluator output
fails rather than becoming a mate. Ordinary root depth is at most 256, with
`P + D <= 256`; there is no additional ordinary absolute-ply static leaf. The
SR-018 pass consumes one real/synthetic path ply while reducing depth by three,
so preserves that limit. Mate and ordinary bands remain strictly separated over
the entire supported horizon. These inspected numbers instantiate the proof;
future changes to scoring, evaluator bounds, horizon or path limits must establish
the corresponding complete domain again. Remaining depth never substitutes for P.

After static leaves have resolved, the adopted positive-depth envelope is:

- `D == 1`: `L = -S`, `U = M - P - 1`; a future losing mate is unreachable.
- `D >= 2`: `L = -M + P + 2`, `U = M - P - 1`.

For original caller window `[A,B]`, `U <= A` proves an UPPER return **U**;
`L >= B` proves a LOWER return **L**. Otherwise use local
`alpha = max(A,L)` and `beta = min(B,U)`. A crossed-window convention alone is
not proof. Return the established domain endpoint, never an invented mate
distance or an arbitrary caller threshold. A domain-only invocation has no
searched move/PV and writes no TT entry, including at a narrow research root.
The bound contains no position-specific witness and cannot justify displacing
ordinary TT evidence. Full-window production roots still search a legal witness.

Precedence is cancellation, current checkmate/stalemate and rule draws, static
leaf, applicable ordinary positive-depth TT resolution, domain restriction,
then remaining Search/selectivity. Current checkmate retains its actual current
ply score. Current formal repetition, rule-50 and insufficient-material draws
retain their existing adjudication. A possible future draw is only a minimax
continuation. NO_LEAF_TT, SearchKey, equal depth/current generation, legal hash
ordering and real-ply normalization remain unchanged. In particular, an original-
window non-cutting TT LOWER/UPPER is not reconsidered after local tightening:
combining a bound with a domain endpoint does not establish its hash move as the
witness for a newly exact result/PV. Ordinary EXACT and existing caller-window
TT cutoffs still precede this mechanism.

Normal searched returns and stores classify against **original A/B**. A searched
fail-high at mathematical U, combined with `V <= U`, establishes `V = U`; a
searched fail-low at L, combined with `V >= L`, establishes `V = L`. Fail-soft
mechanics must not produce an outward contradiction of a proven endpoint.
The move actually searched supplies the witness. Positive U normalizes to
`M - 1`, and the D>=2 negative L to `-M + 2`, independently of P. Ordinary
`-S` is unchanged. A mate-band LOWER/UPPER threshold still does not assert an
exact mating distance.

PVS uses the tightened local window for the first full child and subsequent
scouts. A strict scout improvement below local beta still requires the ordinary
full re-search. Bound-only child resolution does not create a child PV or waive
that rule. A searched move reaching U may stop, including at the root, because
no legal value can improve it. Exact full-window values and required PV witnesses
remain equivalent to the independent oracle; narrow fail-soft numbers need not
be identical when both satisfy the original invocation's mathematical bound.

SR-018/SR-019 guards, calibration thresholds, probe windows and prediction returns
use the **original invocation window** and actual scout provenance. This mechanism
creates no selective event, removes no inherited provenance, and never upgrades
a selective result to proof. Synthetic recurrence may use the same exact domain
without turning its pass assumption into legal-position proof. Ordinary TT,
nested selectivity and quiet-history-write exclusions, history isolation and
path-ply advancement remain intact. In the current adopted NMP policy, the
ordinary zero-window probe and its descendants do not cross these extrema;
measured synthetic work was unchanged. Cancellation remains incomplete work.

The finite-horizon gap mathematics is also settled. With `dWinSlow` the largest
odd integer <=D and, for D>=2, `dLossSlow` the largest even integer <=D, define
`slowWin = M - P - dWinSlow` and `slowLoss = -M + P + dLossSlow`. Strict band
separation excludes exact values in `(S,slowWin)` and `(slowLoss,-S)`.
For any such integer gap `(g,h)`, alpha in `[g,h)` may become `h-1`, and beta
in `(g,h]` may become `g+1`, preserving their inclusive failure classifications.
If both occupy the same gap, do not return from the resulting crossing: an
equivalent `[g,g+1]` search must distinguish `V <= g` from `V >= h`. A searched
weak LOWER number inside the gap rounds upward to h; a weak UPPER rounds downward
to g before original-window classification. Parity gaps between mates are not
implemented. These are domain/bound rules, not point-value or move evidence.

The separate **HORIZON_DOMAIN candidate is REJECTED for runtime incorporation**
under the tested conditions. Its exact fixtures and TT audits passed, but it
searched the same trees as CORE_MDP and supplied no additional node/evaluation
savings; extra arithmetic/branches did not earn their cost. Core adoption rests
on large, repeatable mate-workload wall-time savings with equivalent exact values,
and approximately neutral ordinary/composed-workload economics. Fewer nodes alone
are not the adoption criterion; neither these results nor improved node-budget
completion establish playing strength. Reconsider gap machinery only after a
concrete domain/Search dependency or mechanism-specific evidence changes its
economics, not conventional usage or periodic retesting. Reproducible fixtures,
candidate sources, oracle/TT/PV audits and warmed measurements are preserved at
`C:/Users/Central/Documents/SeedV6-SR011-2026-10-01/REPORT.txt`.

#### LOCKED current leaf policy (SR-001)

SR-001 Quiescence Search is **REJECTED** for incorporation into current
production/default SeedV6 Search under the researched architecture and
conditions. Retain static leaves. The researched policy families did not
establish a sufficiently correct and efficient production qsearch configuration.

Preserve the independently invocable TT-off SR-001A unpruned
`QSEARCH_BASELINE` as research/reference and oracle evidence. It uses recursive
fail-soft negamax alpha-beta, current terminal/draw adjudication before
stand-pat, and stand-pat only at non-check nodes. Non-check moves are all legal
captures, en passant and promotions; checked nodes search every legal evasion
without stand-pat. Actual root/path ply controls mate distance. Accepted SR-015
SEE/material evidence orders tacticals only: no SEE pruning, other selectivity,
ordinary quiet checks or qsearch Search-TT participation. Existing
cancellation/incomplete semantics remain authoritative. This reference and the
retained experimental selectors/artifacts gain no production architectural
authority from their research use.

The durable evidence is:

- Unpruned qsearch produced severe tree expansion and deep tactical tails.
  SEE-only pruning reduced work but lost demonstrated tactical resources;
  promotion/check safeguards did not establish a general tactical-error bound.
  Fixed qdepth limits truncated mate, draw and other tactical resources; no
  tested horizon supplied a satisfactory standalone semantic boundary.
- The tested separate cold exact qsearch `TTable` preserved tested values and
  removed a meaningful minority of nodes, but increased aggregate wall time,
  left maximum qply unchanged and did not solve the primarily unique-tree
  expansion. This qTT execution policy is not adopted.
- Ordinary quiet checks exposed real mate, draw and material resources.
  Unrestricted inclusion caused severe expansion and incomplete searches;
  fixed-count and legal-evasion-count/forcingness policies either lost deeper
  resources or recreated severe growth and pathological checking chains.
  Evasion classification also added substantial generation cost. No acceptable
  current participation policy was established; quiet checks are not universally
  worthless and may be reconsidered under materially different selective evidence.
- Conventional `standPat + immediateMaterialGain + fixedMargin` delta evidence
  is not accepted as qsearch pruning authority. Material gain was not an upper
  bound on Search value, including en passant, clearance and draw witnesses;
  fixed material/Search-unit margins embed unsupported evaluator-calibration
  assumptions.
- For the analyzed non-check-parent, non-promotion, non-checking captures with
  nonterminal children, `moveValue <= -Eval(child) = postMoveStatic` follows
  from child stand-pat. `postMoveStatic <= alpha` already resolves through a
  one-node child stand-pat cutoff, not new futility savings. Skipping such calls
  outright may alter fail-soft scores/PVs and is not an authorized equivalence
  optimization.

This is a maturity/architecture-dependent conclusion, not universal qsearch
inferiority. No strength/self-play or measured Elo conclusion was established.
Reconsider production qsearch only after a concrete material dependency or
Search-architecture change plausibly alters this evidence, through an explicit
programme decision; convention or periodic retesting alone is insufficient.

#### LOCKED exact traversal policy (SR-003)

TT-disabled ExactSearch retains ordinary ordered alpha-beta as the independent
exact/reference/oracle traversal. For normal TT-enabled ExactSearch, PVS is the
implemented production/default traversal under the accepted ordering architecture
(section A), staged/lazy mechanics (section L) and TT policy (section J).
Do not introduce position-specific, depth-specific or heuristic switching
between alpha-beta and PVS. These traversal rules remain exact in ordinary
constructors; SR-018/SR-019 add separately identified predictions to eligible scouts
in the explicitly selected HCE production mode.

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
not a claim that PVS universally outperforms alpha-beta.
SR-029 rejects the researched quiet-LMR policies for this baseline, as recorded
below; production retains nominal-depth scouts. Other selective pruning/probe
mechanisms, iterative-deepening consumers beyond section K's LOCKED decisions,
and parallel Search remain OPEN separate research. Section K records SR-004's rejection of
MTD(f)/memory-enhanced zero-window root drivers and SR-006's rejection of
aspiration windows for the current production/default Search.

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
5. SR-011 mathematical domain handling in its adopted modes.
6. Remaining move generation/search and separately authorized selectivity as applicable.

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

Production qsearch and the tested separate cold exact qTT policy are not adopted
under SR-001 (section I). The static-leaf TT exclusion remains unchanged.
A materially different future qsearch architecture requires explicit
reconsideration under SR-001's dependency-change boundary; it must not silently
inherit static-leaf TT semantics or conventional engine practice.

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

Proof strength and completed-window classification are distinct (SR-008).
Terminal resolution, a current static leaf or an applicable TT EXACT source may
establish the exact value for the invocation internally even when its returned
score is classified UPPER or LOWER against its own original window. Conversely,
full-width/full-search provenance alone does not guarantee an EXACT result.
This distinction changes neither the classification/store rules above nor exact
PVS re-search obligations, TT applicability or static-leaf TT exclusion.

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

#### LOCKED ETC / TT look-ahead disposition (SR-027)

SR-027 is **REJECTED** for incorporation into current production/default Search
under the researched single-thread HCE, static-leaf, nominal-depth PVS and exact-TT
conditions. Genuine later-candidate mathematical cutoffs exist, but the tested
phase-local policies did not establish sufficiently consistent wall-time benefit
relative to preparation, probe traffic and changed reuse. Production retains
ordinary child-entry TT resolution, unchanged SR-018/SR-019 composition, and the
independent exact constructors and TT-off oracle.

For a legal parent move at remaining depth D, its value is the negation of the
child value at D - 1. An applicable child UPPER or EXACT score s <= -parentBeta
therefore proves a parent LOWER result of at least -s. Child LOWER evidence instead
supplies an upper bound on that move; it does not prove parent fail-high. One child
ordinarily establishes neither parent fail-low nor an exact parent value. These
mathematical distinctions grant no new production probing policy.

Every researched proof retained the value-equivalent history-sensitive SearchKey,
current generation, exactly matching positive child depth, terminal/draw/rule-state
precedence, real child-ply mate decoding, and completion checks. A speculative read
was not proof: the economical variant deferred child adjudication until a possible
cutoff entry was found, but always adjudicated before accepting it. NO_LEAF_TT,
PVS scout/full-window re-search obligations and synthetic-null TT isolation remained
intact. No eval-cache evidence, stale score, depth relaxation or position-only key
was used.

A proof-only legal move can expose a one-move cutoff prefix without inventing a
searched continuation. Inspection does not increment searched ordinal or earn a
quiet-history reward/malus batch. Such mathematical proof is not selective by
itself; ordinary parent LOWER storage and parent-ply normalization remain valid
unless existing SR-018/SR-019 provenance independently suppresses the write.
Avoided predictions and history effects must not be synthesized from CONTROL.

Observation separated ordinary immediate child-TT resolutions from pre-existing
later-candidate proofs and measured the ordinary prefix they could avoid. Most
proofs were already the next/hash candidate; genuine scan-ahead savings concentrated
in tactical/evasion phases and actual scouts. The active family inspected only the
next two later candidates in the naturally materialized initial phase, admitting
tacticals or complete evasions, with parent-depth floors two, three and then four.
It never forced parent quiet generation or repeatedly rescanned siblings.

The bounded policies reduced nodes, but extra probes and changed history/table
reuse consumed much of that headroom. Fresh depth-seven costs, concentrated deeper
winners and the inconsistent narrower depth-four result did not establish a
reliable production policy. Useful deep-workload gains remain counterevidence to
universal ETC inferiority. Complete all-child fail-low/exact-dominance observations
were sparse and did not earn a broader active mechanism. No playing-strength/Elo
or neural-evaluator generalization is claimed.

No production look-ahead, research counters or experimental Search state is
retained. Reconsider only through an explicit programme decision after a concrete
Search/TT/evaluator dependency change or mechanism-specific evidence plausibly
retains worthwhile avoided work without taxing the unproductive population.
Conventional ETC use elsewhere, nearby tuning, weaker proof qualification and
node reduction alone are insufficient. No other frontier feature was begun.
Reproducible sources, captures, oracle audits and clean timing evidence are at
`C:/Users/Central/Documents/SeedV6-SR027-2026-10-01/REPORT.txt`.

### K. Production Search driver and lifecycle

A Search driver/coordinator above ExactSearch owns production lifecycle
concerns: repeated fixed-depth invocations, iterative-deepening progression,
search limits, cancellation/stop propagation, observer/progress/result
adaptation and retention of the most recent completed Search result for
production consumers. This separation is semantic and architectural; it does
not prescribe a concrete Java class name, package or exact source-code shape.

The current production iterative-deepening baseline remains simple and
deterministic: begin at depth 1, then search successive complete depths
2, 3, 4, ... until the requested limit or an external stop condition prevents
further completion. Each iteration is a full-window fixed-depth invocation: the
HCE default composes SR-018 and SR-019, while neural and explicit unpruned
controls retain ordinary ExactSearch. All iterations within one top-level
SearchDriver request reuse the same Search TT and generation under section J.
This baseline adds no aspiration windows or other iterative-deepening optimizations.

Only completed iterations may supply completed Search results. If depth 7
completes and depth 8 is then cancelled or otherwise stopped before completion,
the driver retains and may return/report the completed depth-7 result. The
depth-8 invocation remains incomplete and must never masquerade as a valid
depth-8 result. Retaining a previous completed result does not change the
completion semantics of the interrupted invocation. Only completed iterations
publish reusable iterative information; cancelled/incomplete iterations do not
publish provisional move, PV or score state as completed evidence.

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

#### LOCKED memory-enhanced zero-window driver disposition (SR-004)

SR-004 (MTD(f) and memory-enhanced zero-window root drivers) is **REJECTED** for
incorporation into current production/default SeedV6 Search under the researched
single-thread HCE architecture and conditions. This conclusion depends on Search
maturity and architecture; it is not a universal rejection of MTD(f). Exact/oracle initial
guesses demonstrated meaningful economic headroom from repeated zero-window
PVS with same-generation TT reuse, but the previous completed depth's exact
score did not make that headroom practically accessible.

Unrestricted previous-score convergence regressed severely. A bounded two-pass
probe stage with full-window fallback prevented runaway convergence and made
the fallback itself cheaper through TT warming, but total cost still regressed
materially. Directional exponential bracketing followed by bisection eliminated
the pathological pass counts, yet also regressed materially against full-window
PVS. Exact-result/PV materialization was negligible; measured throughput remained
similar to or above the control. Repeated Search work dominated the measured
cost, with no score, best-move, PV, mate-distance, cancellation or completion
defect found. Same-type TT replacement and invocation-local quiet-history
resets remain characteristics of the researched architecture, not demonstrated
causal dependencies whose modification would make practical MTD profitable.

No researched MTD(f) or memory-enhanced zero-window root driver is adopted.
Production SearchDriver retains ordinary full-window successive fixed-depth PVS.
Retained research implementations and harnesses have research authority only.
ExactSearch/PVS, TT applicability, replacement, bounds, generation, SearchKey,
hash-move and history semantics remain unchanged. SR-006's independent rejection
of aspiration windows remains unchanged and is not reopened by this conclusion.

Reconsideration requires an explicit programme decision after a concrete material
dependency or architecture change plausibly alters practical first-guess quality
or repeated-zero-window/TT economics. Examples include a materially different
relevant Search/TT architecture or a newly established source of substantially
stronger exact-depth score prediction. Conventional practice, periodic retesting,
nearby pass limits, score-step sizes, threshold sweeps or parameter tuning alone
are insufficient grounds to reopen SR-004.

#### LOCKED iterative-deepening information reuse (SR-005)

Explicit previous-iteration PV or best-move ordering is **REJECTED** as an
additional production/default Search mechanism under the current single-thread
architecture and researched conditions. Focused research found it completely
redundant with existing same-request TT/hash-move reuse: all 90 candidate-bearing
observations across six representative positions through depth 6, and all 60 new
observations into depths 7-10 for Kiwipete and middlegame, matched the legal TT move
already searched first. No TT/PV disagreement, missing-TT fallback opportunity,
illegal previous-PV candidate or SearchKey mismatch was observed.

The deeper continuation used the normal 64 MiB requested TT, one generation
across iterations, single-thread HCE, PVS, accepted staged/lazy ordering and
static leaves, under materially larger depth-10 Search trees. Its matching
entries were current-generation and depth-mismatched by one remaining ply:
valid move-order evidence, not score evidence under section J. Observation-only
instrumentation preserved Search semantics and tree counts; deterministic and
non-interference validation passed.

The legal applicable TT/hash move retains its existing highest ordering
precedence. Do not introduce a second explicit previous-PV/best-move ordering
source merely to duplicate it. The independently invocable fixed-depth
ExactSearch path and TT-off reference semantics remain unchanged.

Previous completed root score, best move and their iteration-to-iteration
stability may remain completed driver/result information. SR-005 gives them no
authority to alter alpha/beta windows, stop Search, change nominal depth,
reduce, prune or extend work, modify TT evidence, or otherwise alter ExactSearch
semantics. Active previous-score use for aspiration windows is **REJECTED** for
current production under SR-006 below. Active score/best-move-stability use for
time allocation, stopping or easy-move behaviour remains **OPEN** under SR-034
Time management.

This is a maturity/architecture-dependent conclusion, not a universal claim
that explicit PV reuse can never help. Reconsider only after a concrete material
dependency or Search-architecture change plausibly alters TT retention or how
previous-depth move evidence survives, such as a materially different TT
retention/replacement design or parallel/shared TT architecture. Such changes
remain separate research; convention or periodic deeper retesting alone is
insufficient to reopen SR-005.

#### LOCKED aspiration-window disposition (SR-006)

SR-006 Aspiration windows is **REJECTED** for incorporation into current
production/default SeedV6 Search under the researched single-thread architecture
and HCE measurement conditions. Previous completed root scores were moderately
predictive, enough to justify research, but not stable enough to make the tested
policies economically worthwhile. Substantial tactical and odd/even score
movement persisted through deeper Search; neither depth nor recent volatility
reliably bounded the next movement.

The durable evidence is:

- Fixed one-shot aspiration with immediate full-window retry preserved completed
  score, best-move and PV equivalence under the accepted exact semantics.
  ASP-512 regressed by 0.641% aggregate nodes versus CONTROL. ASP-628 reduced
  aggregate nodes by only 0.250%; measured wall-time change was within timing
  noise, so no meaningful wall-time benefit was established.
- The evidence-backed stable/volatile ADAPT-172/628 policy also passed exact-result
  correctness but regressed by 0.564% nodes versus CONTROL and 0.816% versus
  ASP-628, with no demonstrated wall-time benefit. Its small stable-subset savings
  were outweighed by later volatile-iteration costs from changed retained TT
  state. Results were concentrated in a few positions; excluding the principal
  starting-position regression left only about 0.060% aggregate improvement.
- Retaining the same request-owned TT generation across failed attempts and
  retries materially reduced fallback work. ASP-628 failure plus retry work was
  already comparatively inexpensive, leaving little plausible aggregate upside
  for staged or directional widening. No credible remaining evidence-backed
  aspiration mechanism was identified; nearby threshold/width tuning had reached
  diminishing returns.

No aspiration policy is adopted. Production SearchDriver retains its normal
full-window successive fixed-depth behaviour. ExactSearch, PVS, TT evidence and
applicability, mate handling, cancellation/completion and evaluator independence
remain unchanged. Retained aspiration research implementation and harness support
have research evidence value only and gain no production architectural authority.

This is a maturity/architecture-dependent rejection, not universal inferiority
of aspiration windows. HCE measurements do not establish evaluator-wide numerical
score distributions. Reconsider only after a concrete material dependency or
Search-architecture change plausibly alters the result, such as materially
different relevant score volatility or Search/TT economics, through an explicit
programme decision. Conventional engine practice, periodic retuning, nearby
constants or parameter sweeps alone are insufficient grounds to reopen SR-006.

#### LOCKED internal iterative search disposition (SR-007)

SR-007 is **REJECTED** for incorporation under the researched single-thread HCE,
static-leaf, iterative PVS and mathematical-TT baseline. Production retains
nominal-depth search and unchanged SR-018/SR-019. Missing legal hash evidence
does not prove low value, depth unreliability or an expected node role.

Internal iterative deepening (IID) researches a shallower legal invocation for
move-order evidence before performing the nominal invocation. Its score is not
nominal-depth proof. The researched probes preserved real ply and complete
history, actual-depth TT applicability, terminal/mate/cancellation semantics and
the independent oracle. They excluded synthetic domains and nested internal or
selective work. A single reusable move row preserved the caller's generated
phase across a same-ply probe; this is research mechanics, not adopted runtime
state. Quiet-hint validation and staged generation remained mandatory.

Missing hashes concentrated at shallower interior nodes in the observed
iterative workloads. Depth-minus-two probes, non-scout-only and deeper-floor
variants, normal completed-probe history updates, and a nearer depth-minus-one
probe did not establish repeatable useful wall-time benefit on fresh and deeper
suites. Exact values/PVs and incremental evaluator state passed their audits;
correctness alone did not earn the additional work, branches and state.

Internal iterative reduction (IIR) instead substitutes a reduced horizon and
therefore predicts a nominal result. Actual-depth descendant TT proof can remain
valid, but the substituted result and affected ancestors cannot store ordinary
nominal-depth proof. Conservative selective provenance and write suppression
were demonstrated in research; no new production proof-recovery mechanism was
adopted. Missing-hash R=1 retained false cutoffs and reversed its shallow savings
on a deeper workload. A directly motivated same-parity R=2 had genuine bounded
economic headroom but retained prediction errors and substantial rook/pawn
resource losses persisting across deeper parities. Independent original-history,
original-ply TT-off searches confirmed those adverse move values. Deeper horizon
values remain evidence, not game-theoretic truth or measured playing strength.

No internal probe, reduction, expected-node classifier or experimental state is
retained in production. Further eligibility formulas or witness-specific guards
were not earned. Reconsider only through an explicit programme decision after a
concrete Search/ordering/TT/evaluator dependency or mechanism-specific evidence
change plausibly addresses the observed information, economics or resource
failures; convention, nearby depth tuning and nominal node savings alone are
insufficient. No neural or Elo generalization is claimed. Reproducible sources,
corpus qualification, oracle audits, witnesses and warmed timings are at
`C:/Users/Central/Documents/SeedV6-SR007-2026-10-01/REPORT.txt`.

### L. Implementation economy and hot-path mechanics

Building Search from first principles does not imply implementation layering
or one Java type per conceptual responsibility. Semantic responsibilities may
remain conceptually distinct without separate runtime objects, classes,
interfaces, enums, wrappers or policy objects. Use the mechanically smallest
and most direct implementation that correctly preserves the LOCKED semantics
and independently useful correctness/reference boundaries.

SR-008's conceptual evidence dimensions do not require separate stored fields
or runtime representations. Do not create a generic Search-context, Node or
Evidence object, node-role enum or recurring hot-path state merely to represent
the model. Prefer primitive/direct local state and economical recomputation or
derivation. A later mechanism requiring parent-to-child state must justify its
hot-path argument cost, footprint, branches, locality and JVM/JIT behaviour
against the information and benefit its concrete consumer needs.

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

At positive-depth non-check nodes, generate legal tacticals first and defer legal quiet
generation until required. A hash/tactical cutoff can avoid quiet generation
entirely; otherwise generate and lazily consume the quiet phase when needed.
At checked nodes, use complete legal evasions rather than an artificial
tactical/quiet split.

At the current ExactSearch static boundary, terminal/nonterminal status must
still be established before static evaluation. At a non-check node, use exact
legal-move existence without materializing a move list (SR-037). An empty tactical
subset alone never establishes stalemate; quiet and exceptional legal resources
must also be accounted for. Complete evasions establish mate/nonterminal status in check.
Cancellation, terminal and draw precedence remain unchanged. This legal-move
existence handling searches no children and is not qsearch; SR-001 retains this
static production boundary.

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

Use the existing tactical/quiet/evasion generation capabilities at positive depth.
No prepared-generation context or Board/Gen redesign is locked; future measured
mechanical optimization may revisit generator internals while preserving accepted
Search semantics.

**Adopted static-leaf mechanical optimization (SR-037):** Production/default
staged ExactSearch uses a primitive, allocation-free boolean legal-existence
query after establishing that a static leaf is not in check. It does not emit
moves or maintain additional frame state. King destination safety, absolute pins,
pawn movement and en-passant king exposure remain exact. Promotion choices need
no separate emission for existence; at a non-check node, a legal double pawn push
implies a legal single push and a legal standard castle implies a legal ordinary
king step to its transit square. Checked nodes retain complete legal evasions.

This changes physical leaf work only: terminal-before-draw-before-static-evaluation
precedence, static-leaf TT exclusion, positive-depth generation/ordering, history
sampling, evaluator lifecycle and SR-018/SR-019 are unchanged. Preserve the
independent TT-off/full-generation reference and separate qsearch research path.
Compared with the preceding production baseline, this optimization must preserve
the searched tree, values/PVs, evaluator calls, TT traffic and history semantics;
it grants no new prediction, horizon or pruning authority.

Profile-led alternatives that reused checker setup or stopped existing piece
generators early were investigated, but the direct leaf-only query earned better
economics with smaller production integration. Repeated warmed HCE measurements,
fresh deeper workloads, legality/oracle comparisons and state/trace checks
supported adoption. These are throughput results, not playing-strength evidence
or universal evaluator/platform speed guarantees. Reproducible research sources
and evidence are preserved at `C:/Users/Central/Documents/SeedV6-SR037-2026-10-01/`.

## Node/evidence model and working lifecycle

### LOCKED node/evidence model (SR-008)

SR-008 research is **ACCEPTED** and logically closed. SeedV6 Search uses an
orthogonal evidence model, with conceptually distinct dimensions:

- **Invocation facts:** the caller's original alpha/beta window, remaining
  nominal depth, root/path ply and other facts defining the actual invocation.
- **Mathematical proof/bound evidence:** what value or bound has been established
  and why it applies; section J distinguishes proof strength from the completed
  invocation's classification against its original window.
- **Search provenance and relevant path context:** how the invocation arose,
  including actual PVS call provenance and relevant facts about its path,
  subject to section F's consumer and incremental-information requirements.
- **Heuristic/predictive evidence:** signals about likely outcomes or useful
  work, distinct from mathematical proof and subject to separate acceptance
  before a Search mechanism may act on them.

Caller window geometry, how that window arose, actual PVS call provenance,
completed-window classification and predicted fail-high/fail-low tendency are
different concepts. Do not conflate them in an authoritative entry-time PV/Cut/All
category. PV/Cut/All terminology may later be useful only as explicitly defined
diagnostic or predictive shorthand; it is not current mathematical node identity.

SR-008B demonstrated strong predictive distinctions from actual PVS provenance
and searched move ordinal in current exact Search under the researched
single-thread HCE workload. Ordering/history evidence also had useful but
context-sensitive predictive relationships. Late moves still produced real
cutoffs and re-searches. These signals establish neither a universal confidence
scalar nor authority for pruning, reductions, extensions, stopping, changed
windows, changed depth or other selectivity. Measured percentages must not
become Search constants or architectural probabilities.

The numerical distributions and conditional rates are empirical evidence under
the current HCE baseline, not evaluator-independent probabilities. Evaluator
independence remains LOCKED; current HCE score behaviour is not universal Search
evidence. Static-evaluation evidence fits within this model; its accepted
relationships, non-adoptions and dependency-triggered reconsideration boundaries
are settled by SR-009 below, not by SR-008 alone.

No production Search implementation change is required for SR-008 closure.
Current Search already supplies or cheaply derives much of the relevant factual
and provenance information. This conceptual model does not authorize new
recursive context state or changed exact Search behaviour; section L governs
its physical representation.

### LOCKED static-evaluation evidence and correction disposition (SR-009)

SR-009 research is **ACCEPTED** and logically closed as an evidence-model and
research-disposition conclusion. It adopts no production Search mechanism.

#### Raw evaluation and empirical scope

Raw static evaluation `E(P)` is an ordinary Search-domain score from the
side-to-move perspective, distinct from terminal/mate scores and Search TT
EXACT/LOWER/UPPER evidence. Its availability alone makes it neither mathematical
proof nor a bound on a deeper fixed-depth Search value. For a fixed evaluator
and mapping, with exact fixed-depth value `Vd(P)`, the research quantity
`residual(P,d) = Vd(P) - E(P)` measures fixed-horizon disagreement, not
game-theoretic evaluator error.

Under current HCE/current Search conditions, residual behaviour is strongly
position- and horizon-dependent, with repeatedly observed odd/even depth
structure; a simple monotonic uncertainty-versus-depth rule is inadequate.
Raw-versus-searched value, proof-versus-prediction, exact-versus-bound and the
residual definition are general Search concepts. Measured distributions,
tactical effect magnitudes, movement relationships and correction predictions
are current-HCE empirical evidence, not evaluator-independent probabilities,
constants or universal calibration. NNUE/BRN validation is required before
treating those numerical findings as evaluator-wide. Preserve the LOCKED
evaluator-independent core; SR-009 introduces no evaluator-specific exact
Search branch.

#### Tactical and movement evidence

Immediate legal tactical-resource presence (capture, en passant or promotion)
earned durable predictive standing for larger static-evaluation disagreement
under the researched single-thread HCE conditions. It replicated at roots,
survived material/source/phase/composition checks, transferred to ordinary
interior nodes and replicated on a disjoint common-support interior population.
Its magnitude remains context-dependent. This is heuristic evidence, not proof,
a reliability boolean, a universal confidence scalar or authority for pruning,
reductions or extensions; later consumers need their own evidence, eligibility
and economics.

SEE sign did not establish stable incremental static-evaluation-reliability
information beyond coarse tactical presence in the researched scope. Do not
infer a general SEE-good/SEE-bad reliability ordering. This leaves accepted
SR-015 tactical ordering and independent future SR-024 SEE-pruning research
unchanged.

One-ply current-perspective movement `E(C) + E(P)`, for child/current node `C`
and parent `P`, is not retained as useful incremental predictive evidence in
the bounded HCE interior scope: valid buffered replication found no useful
positive movement-versus-residual relationship. Do not introduce one-ply
movement state from SR-009. Reconsider only after a material architecture or
evaluator dependency change, not periodic resampling.

Same-side two-ply movement `delta2 = E(C) - E(G)`, with grandparent `G`, is
semantically distinct. Continuous delta2 showed bounded, context-dependent
structure in the narrow researched HCE scope, not a general Search-wide
predictive relation. Preserve that bounded research evidence without
architectural authority. Conventional binary positive/negative
improving/worsening did not earn adoption; do not add a conventional improving
flag or generic improving/worsening runtime state.

#### Residual headroom and correction implementation

Reusable signed HCE residual predictability exists under the researched depth-1
conditions. A frozen generalized non-pawn-material-count key predicted signed
residual on held-out roots/exact states materially better than zero correction
(raw static evaluation) and the preregistered context-only comparator. This is
accepted empirical predictive headroom, not justification for production
correction history. Improvement was strongly concentrated and context-dependent,
tactical observations supplied most improvement, held-out residual sign coverage
was limited, and numerical behaviour remains evaluator/workload dependent.

Legitimate production point-value labels are broader than completed-window
EXACT classification alone: exhaustive depth-1 move search can establish an
exact point value despite a non-EXACT window classification; applicable TT EXACT
and other independently exact completed values may also supply point evidence.
This preserves section J's distinction between proof strength and classification
and changes no TT or PVS rules.

Correction-history-style implementation is **deferred / not adopted** for current
SR-009. At that closure, positive-depth production Search did not compute raw
`E` merely because evaluator/accumulator state existed; it was naturally
available at none of the identified production point-label opportunities.
Obtaining it requires extra evaluator work. Natural point labels are compositionally biased relative to the
difficult held-out residual population, with weak or absent supervision in
several predictive context cells. No production consumer had then been accepted
for corrected evaluation. Online learning, lifecycle, update policy, table sizing,
aging and correction bounds remain unresearched, and HCE economics establish
neither NNUE nor BRN economics. A frozen offline correction table could isolate
consumption headroom but provides no Search benefit without a justified consumer.

Preserve the held-out headroom and reconsider correction implementation only
after a concrete material dependency changes its economics or creates a
justified consumer: an accepted mechanism already needs positive-depth raw `E`;
better, less-biased production labels become naturally available; a changed
evaluator/Search architecture alters evaluation/key economics; or a concrete
mechanism-specific experiment requires corrected-eval evidence. Convention,
parameter tuning and periodic retesting alone do not reopen correction history.
Any reopening must establish production learning/table/update mechanics before
adoption.

SR-019 establishes a bounded positive-depth raw-`E` consumer; SR-018 adds a
second, at remaining depths 4 through 6. These are concrete dependencies relevant
to possible later correction reconsideration. They do not adopt corrected evaluation,
establish unbiased supervision, resolve
learning mechanics or reopen correction research automatically. Selective
scores are not legitimate mathematical point-value labels.

#### Consumer and representation boundaries

SR-009 justifies no universal confidence scalar, reliable/unreliable boolean or
combined aggression score. Keep horizon/depth, tactical environment,
position/tree context and Search provenance/path context conceptually separate;
later mechanisms must interpret them technique-specifically.

Conceptual evidence requires no new runtime `StaticEvalEvidence`, reliability
object, `SearchContext`, improving flag, correction table, parent/grandparent
eval argument or universal confidence state. Under section L, concrete consumers
must justify evaluator calls, recursive arguments, per-ply storage, table
footprint, branches, memory traffic, locality and JVM/JIT effects. Prefer
already-local/cheap information and primitive/direct representation.

Later selective work remains independent: SR-018 and SR-019's separately calibrated
adopted policies follow below;
SR-020 may use static-evaluation relationships; SR-021
records its bounded rejection and reconsideration conditions below; SR-029 may consider tactical
context but cannot assume conventional improving is accepted; SR-033 remains
separate adaptive/hindsight depth-feedback research. Each must justify its own
eligibility, interpretation, economics, safeguards and aggressiveness. SR-009
designs none of these mechanisms and supplies no universal selectivity policy.

### LOCKED reverse-futility / static-null disposition (SR-019)

SR-019 is **IMPLEMENTED** as a deliberately narrow HCE selective policy. The
historical SeedV3 mechanism is conventional shallow reverse futility/static-null:
at non-PV, non-check nodes below depth 3, `E - {0,120,240}[depth] >= beta`
returns `beta` before searching a child, with an upper mate-window guard and
no tactical/material restriction. Its historical presence supplies terminology,
not adoption authority. SeedV6 does not adopt that linear depth policy.

#### Eligibility, aggression and lifecycle

The adopted mode requires the calibrated standard HCE definition and TT-enabled
PVS. Consider only an actual later-move scout invocation, remaining depth
**exactly 2**, not in check, with halfmove clock **below 98**, and an ordinary
score window (`alpha >= -MAX_STATIC_SCORE`). Before paying for evaluation,
also require `beta <= MAX_STATIC_SCORE - 960`: a larger beta cannot pass the
margin within the evaluator's permitted ordinary range. Root/first-move/full
re-search invocation provenance does not qualify merely because its window
happens to be narrow. This introduces no authoritative PV/Cut/All node role.

Cancellation, legal-existence/terminal adjudication, rule draws, static leaves
and applicable equal-depth/current-generation mathematical TT resolution all
precede the prediction. Under staged generation, legal tacticals (or complete
evasions) and, where needed, a quiet-existence check already establish terminal
status. That generation cost is paid; pruning saves subsequent ordering, quiet
materialization where still deferred, and child search. No child of the pruned
invocation has yet been searched. Positive-depth raw `E` is an additional call,
not a free consequence of evaluator/accumulator state.

For an eligible node, evaluate raw ordinary side-to-move `E` and predict a
cutoff only if **`E - 960 >= beta`**. This is an empirical depth-two threshold,
not a per-ply law or universal confidence estimate. Return **beta**, retaining
an empty PV at the omitted node. Raw-`E` and `E-margin` returns added no useful
benefit in the researched candidate; beta conveys only the predicted threshold.
Searched nodes retain ordinary fail-soft mechanics, but a selective result is
not a mathematical bound or exact value.

Tactical presence was available cheaply but did not justify a blanket ban or
an additional margin. Its residual-magnitude association in SR-009 does not
imply the same relation to the lower tail relevant to false cutoffs. Restricting
the accepted candidate to tactical-present nodes added no material benefit.
No general material/endgame ban is adopted. Check, mate-window and rule-50
horizon exclusions are explicit; the latter rejects demonstrated false draw
predictions at clocks 98/99. Terminal and current repetition adjudication retain
precedence. These safeguards do not prove absence of unseen tactical, future
repetition, zugzwang or missed-mate errors.

#### Mathematical TT, result and PV boundary

Section J's ordinary EXACT/LOWER/UPPER meaning, keys, depth/generation
applicability, replacement and mate normalization remain unchanged. A predicted
return is never stored as ordinary mathematical TT evidence. A primitive
invocation-local counter advances on each actual prediction; every active
ancestor suppresses its TT write if that counter changed during its subtree.
The root likewise suppresses its write after any prediction. Unaffected
subtrees may still store/reuse ordinary evidence. Suppression is conservative,
including predictions later made irrelevant by re-search; no proof recovery
or second selective TT domain is introduced.

Completed fixed-depth and driver results preserve a conservative `selective`
flag when any prediction occurred in that iteration. A true flag forbids exact
proof interpretation even for an inside-window or full-window score. A false
flag alone is not proof for legacy, custom, narrow-window or separate-domain
research producers. Completion, legal PV prefixes and iterative publication
remain distinct from proof. No continuation is invented for omitted work.
Quiet history remains ordering evidence even when an update follows selective
descendants; it is not promoted into pruning authority or mathematical proof.

#### Evidence, implementation economy and scope

Outcome-blind capture preserved production score/move/PV/node identity and
replayed full history and original ply against the TT-off oracle. Actual
selective audits then measured false predictions (`Vd < beta`), severity,
position concentration and sampled TT proofs. The historical shallow rule
made false cutoffs including missed mates. Conservative depth-one-plus-two
pruning lost its timing benefit on the deeper suite; depth-one-only and
tactical odd-depth alternatives did not establish robust useful economics.
Depth-two-only pruning reduced both total evaluator calls and wall time despite
paying for new positive-depth calls and suppressing affected ancestor TT stores.
The final frozen production policy improved both a fresh legal-walk holdout and
the deeper suite, retaining benefit beyond the largest winning positions.
The accepted HCE sampled actual cutoffs had no observed false predictions;
this is bounded empirical evidence, not a mathematical guarantee or Elo result.

Neural screens produced substantial false predictions using the HCE threshold.
NNUE, BRN, isolated rebuilt HCE controls and unknown/custom evaluator definitions
therefore remain unpruned by default. Ordinary ExactSearch constructors and
TT-off ordered-alpha-beta remain independently exact; the named selective
factory requires explicitly calibrated use. SR-019 itself adds one mode flag, one
counter, a local counter snapshot and actual-scout provenance, with no generic
context object, retained static-eval stack, confidence scalar, improving state,
correction table or research counters. Unpruned production-source benchmarks
retained the baseline tree and showed no material timing regression.

Broader depths, different numerical calibration, neural adoption or other
selective mechanisms require new mechanism-specific evidence and an explicit
programme decision. SR-019 creates the raw-`E` consumer dependency noted under
SR-009; it begins none of the other frontier subjects. Bounded reproducible
evidence is preserved at
`C:/Users/Central/Documents/SeedV6-SR019-2026-09-30/REPORT.txt`.

### LOCKED null-move pruning disposition (SR-018)

SR-018 is **IMPLEMENTED** as a bounded HCE-only null-move policy, composed with
the unchanged SR-019 policy. Fixed reduction earned adoption; adaptive reduction
and verification did not earn production inclusion under the researched conditions.
The feature's historical title does not require either mechanism.

#### Eligibility and prediction

After cancellation, legal-existence/terminal and draw adjudication, static leaves
and applicable ordinary mathematical TT resolution, consider only an actual
later-move PVS scout, not in check, at remaining depth **4, 5 or 6**. Root,
first-move and full re-search calls do not qualify from window shape alone.
Require `halfmoveClock + depth < 100`, `alpha >= -MAX_STATIC_SCORE`, and
`beta <= MAX_STATIC_SCORE - 512`. The side to move must own at least one queen,
rook, bishop or knight. This measured king-and-pawns-only exclusion is not a
blanket endgame prohibition or an assertion that remaining positions cannot be
zugzwang. Terminal/draw handling precedes every guard.

Pay for one additional raw side-to-move evaluation `E` at an eligible node;
attempt the probe only when **`E - beta >= 512`**. This is SR-018-specific HCE
calibration, not an inherited SR-019 margin or evaluator-independent probability.
The fixed deliberate reduction is **R=2**:
`nullScore = -Search(N(P), depth - 1 - 2, -beta, -beta + 1)`.
The pass consumes one ply separately from R. Thus probe depths are 1, 2 and 3.
No verification search, adaptive reduction, tactical blanket exclusion,
improving state or universal aggression scalar is retained.

A completed fail-high predicts only the threshold: return **beta**, with an
empty PV at the pruned node and one committed selective event. The synthetic
fail-soft score is never a legal-position value or mathematical proof. A fail-low
probe is discarded and ordinary legal Search continues without committing an
event. Neural, isolated HCE and custom evaluator defaults remain unpruned;
ordinary ExactSearch constructors and the TT-off ordered-alpha-beta oracle remain
independently exact. Factory selection preserves the evaluator-independent
recursive interface.

#### Synthetic state and proof boundary

The synthetic child toggles side to move, preserves pieces and castling rights,
clears en passant, increments the local reversible halfmove clock, and advances
the fullmove counter after Black in the same way as a normal transition. Root/path
ply advances by one for evaluator slots and mate distance. Parent board storage is
untouched; ordinary child preparation maintains status-dependent evaluator state.
Probe transitions consume the normal lifecycle node budget. Cancellation unwinds
the probe and history barrier without publishing a prediction or completed result.

A primitive SearchLineHistory barrier leaves the real prefix intact. The pass
itself is not pushed or counted as a repetition occurrence. Only legal positions
reached after the barrier participate in local repetition adjudication; those
positions can still establish local threefold repetition. Unwind removes the
barrier and restores the real history exactly. This settles only SR-018's local
synthetic domain, composing with SR-010's ordinary legal-history semantics.

Throughout an active null subtree, prohibit further NMP and SR-019 predictions,
all ordinary Search-TT score/bound and hash-move participation, and quiet-history
writes. Existing quiet ordering evidence may be read. No synthetic SearchKey is
published, no second persistent TT domain exists, and no null move enters a legal
PV. Discarded probes therefore have no proof/provenance effect on subsequent legal
results. An accepted cutoff uses the existing committed-event counter: its node
and every affected active legal ancestor suppress ordinary TT writes; unaffected
legal subtrees retain normal TT proof. Completed fixed-depth/driver results retain
conservative selective provenance. All ordinary key, depth, generation, bound,
mate-normalization, completion and terminal precedence requirements remain intact.

Reduced or selective legal verification remains heuristic for nominal depth.
A deeper synthetic search addresses reduction uncertainty, not the pass assumption.
Only an independent legal nominal-depth search can supply ordinary mathematical
proof; merely attempting a discarded probe does not invalidate that legal proof.
No generic context/evidence object or recurring allocation is introduced: production
adds one policy flag, one active-probe flag, and primitive history-barrier state.

#### Evidence and limits

Outcome-blind capture covered 76,055 actual scout observations with 7,422 retained
states, preserving production tree identity and complete board/path/game history.
Replay used original ply and the TT-off nominal-depth oracle, with independent
public reroot checks. Fixed R=0/1/2/3 exposed both pass-assumption failures and
reduction/horizon failures, with strong depth-parity effects under static leaves.
Unrestricted policies and insufficient margins made false cutoffs, including missed
mates. King-and-pawns-only errors earned the material guard; later middlegame errors
rejected the 256 margin. Rule-50 fixtures demonstrated a synthetic draw concealing
a legal forced mate near clock 100, earning the explicit horizon exclusion.

Reduced legal verification retained important errors. Even nominal verification
with ordinary legal TT reuse cost about 4.2% wall time over unpruned control on its
tested suite. An adaptive R=3 at surplus 1024 improved the fresh holdout but was
about 1.35% slower than fixed R=2 on the deeper suite, despite 1.38% fewer nodes;
it did not establish stable incremental economics over the simpler policy.

The frozen fixed policy, composed with SR-019, reduced nodes/evaluator calls/wall
time by approximately **16.8%/18.1%/12.6%** on a fresh 36-position legal-walk
depth-7 suite and **18.3%/18.6%/18.3%** on the 12-position depth-8 suite, including
probe work and ancestor TT suppression. Timing used warmed Java 21 execution with
fresh request-owned TT state, successive-depth iterations and rotated policy order.
Final composition audits found no false cutoffs in 11,932 sampled cold fixed-depth
NMP predictions and a further 6,884 with iterative TT reuse. All 6,144 sampled
legal TT writes passed oracle checks. These are bounded
empirical results, not mathematical pruning safety or playing-strength/Elo evidence.

The depth, evaluator and synthetic-recursion limits are deliberate. Broader depth
bands, neural calibration, nested selectivity or different verification/reduction
policies require new evidence and explicit adoption. SR-018 adds the positive-depth
raw-E dependency noted under SR-009 without reopening correction history or another
frontier feature. Reproducible sources, state captures, witnesses, audits, tests and
measurements are retained at
`C:/Users/Central/Documents/SeedV6-SR018-2026-09-30/REPORT.txt`.

### LOCKED late-move reduction disposition (SR-029)

SR-029 is **REJECTED** for incorporation into current production/default Search
under the researched single-thread HCE, static-leaf and exact-TT architecture.
Real economic headroom was established, but the tested quiet-reduction policies
did not adequately preserve critical late quiet resources. Production remains
SR-018/SR-019-composed nominal-depth PVS. Ordinary exact constructors, the TT-off
ordered-alpha-beta oracle and neural/custom defaults remain unchanged.

#### Horizon evidence, ordinal and re-search

An exact reduced-depth scout establishes evidence only for its actual requested
horizon. Accepting its fail-low to omit a nominal scout is a heuristic prediction,
not nominal-depth mathematical evidence. The researched flow confirmed every
reduced improvement, including a fail-soft score beyond beta, with the ordinary
nominal-depth scout; ordinary PVS then decided fail-low, cutoff or full-window
re-search. No reduced score directly established a nominal improvement/cutoff.

The measured late-move ordinal is one-based over distinct legal moves whose child
search actually began, across all staged phases. Generated-but-unsearched moves
do not count; neither reduced confirmation nor full PVS re-search increments it.
The legal applicable hash/first move is excluded. This factual ordinal definition
supplies no independent authority for SR-022 move-count pruning.

Outcome-blind observation preserved score, best move, PV, nodes, evaluator calls,
TT operation streams and quiet-history fingerprints. Replay retained complete
board/path/game history, original ply, rule state and captured thresholds, using
the TT-off oracle and independent public reroot checks. Errors and costs had
strong depth-parity and position dependence. R=1 at nominal child depth 2 already
missed mate at ordinal 15. At odd child depths 3/5, few sampled false dismissals
did not imply useful economics: reduced work and nominal confirmation could
exceed saved work. Moving later in the order reduced observed errors without
establishing safety; main history supplied no adopted reduction authority.

Active candidates excluded root reductions, checked parents/evasions, captures,
en passant, promotions, checking moves, hash/first moves, mate windows and a legal
child whose clock plus nominal horizon could reach rule 50. Fixed R=1 and R=2
were tested at nominal child depths 4..5 and ordinal >=8, along with depth-4-only
R=1. A simple two-step R=1 at child depth 4 / R=2 at child depth 5 (both reduced
to depth 3) materially improved on fixed R=1. No logarithmic/history/improving
formula or added positive-depth evaluation was imported.

#### Why the policies were not adopted

The two-step candidate reduced wall time by about 34% on one fresh depth-7 suite
and 45% on the depth-8 suite; benefits remained after removing the largest three
timing winners. A further fresh suite retained about 23% wall-time benefit but
exposed an actual 480-point oracle move-value loss. A quiet rook move at ordinal
16 had reduced score -300 at alpha -201 but nominal value 699: the extra horizon
revealed the rook's later capture of a promoted queen. All tested fixed/depth-only
alternatives retained the resource failure.

A directly motivated cheap correction excluding parents with a white pawn on
rank 7 or black pawn on rank 2 did not solve the broader problem. A fresh sparse
mate/promotion/endgame audit still found 31 false dismissals in 2,285 sampled
accepted omissions, including two nominal mates at ordinals 14 and 21 under
ordinary windows. Those quiet, nonchecking moves had ordinary reduced values;
window and reduced-score mate guards could not recognize the omitted mate.
Public reroot checks confirmed the original-ply oracle results. These are
counterexample witnesses, not universal error probabilities.

Retuning nearby depth/ordinal thresholds, banning broader material classes or
adding speculative modifiers was not justified by this evidence. The two-step
schedule earned economic standing only, not production inclusion. Neither history
conditioning, root LMR nor tactical reductions earned inclusion or further active
expansion after these quiet-policy failures. No self-play/Elo or universal
inferiority/safety claim is made.

#### Proof dependency, TT and existing selective composition

The rejected prototype demonstrated a direct primitive distinction between a
discarded reduced attempt, a pending nominal omission and dependency surviving
in the returned proof. A later independent nominal beta cutoff can discharge
earlier local omissions; an upper/exact result depending on omitted nominal moves,
or a cutoff depending on a selective child, cannot publish ordinary TT proof.
Nominal replacement alone need not taint a result. A per-return boolean and local
pending state sufficed without recurring objects or a generic evidence context;
this experimental representation is not installed production architecture.

Reduced legal invocations used ordinary TT evidence/storage only at actual depth,
with unchanged key/generation/bound/mate rules. Nominal confirmation could use
depth-mismatched hash ordering but not reduced-depth scores as proof. Research
measured useful reuse as well as replacement of deeper nominal entries and later
hits on reduced entries. Disabling reduced TT participation increased work;
history-write suppression did not establish broad independent benefit. Neither
general TT nor SR-016 policy was changed. All 47,040 sampled published legal TT
writes passed matching-requested-depth oracle checks across the active audits.

LMR was excluded from synthetic null subtrees. Its initial reduced legal subtree
disabled nested LMR, NMP and static-null predictions while retaining real legal
history/repetition. Nominal confirmation resumed normal SR-018/SR-019 composition,
whose conservative monotonic provenance was preserved. The experiments did not
reopen SR-010, qsearch or another frontier feature.

Reconsideration requires an explicit programme decision after a concrete changed
architecture, evaluator or mechanism-specific evidence plausibly addresses the
demonstrated horizon/resource failures; speed alone, convention and repeated
threshold sweeps are insufficient. This disposition does not establish that
qsearch or any other rejected/pending feature is a necessary remedy.
Sources, state captures, witnesses, benchmarks and validation are preserved at
`C:/Users/Central/Documents/SeedV6-SR029-2026-09-30/REPORT.txt`.

### LOCKED futility-pruning disposition (SR-021)

SR-021 is **REJECTED** for incorporation into current production/default Search
under the researched single-thread HCE, static-leaf and mathematical-TT conditions.
Parent raw-evaluation headroom supplied useful predictive separation, but the
researched fixed depth-one and independently calibrated depth-two policies did
not establish sufficiently repeatable wall-time value. Production retains
unchanged SR-018/SR-019 composition and nominal-depth PVS. Ordinary exact
constructors, TT-off oracle and neural/custom defaults remain unchanged.

#### Move prediction and researched population

Futility predicts that one legal candidate's nominal value
`S_D(P,m) = -V_(D-1)(child(P,m))` will not exceed current alpha. Omitting it is
a selective assumption, distinct from a searched mathematical fail-low and from
SR-019's node-level beta prediction. Raw `E(P)` is not an upper bound;
`E(P) + M <= alpha` uses empirical headroom, not a proven margin. A nonfiring
test creates no selective event or proof dependency.

Research initially isolated interior, non-check parents; actual later PVS
candidates after a legal child had begun Search; ordinary current windows; and
nonchecking ordinary quiets. First/hash moves, root, captures, en passant,
promotions, castling and evasions were excluded. `alpha >= 0` prevented a move
worth only a draw from improving alpha. This is the researched isolation rule,
not universal futility eligibility canon. Ordinal, history, tactical presence and
pawn tags supplied strata, not adopted pruning authority or witness exclusions.
Depth one and depth two were calibrated separately; no per-ply margin law,
improving, SEE/history pruning, LMR, qsearch or preliminary verification was added.

Outcome-blind capture preserved score, best move, PV, nodes, evaluator/child calls,
TT operation streams and quiet-history fingerprints. Replay retained full real
game/path history, board/rule state and original ply, using the TT-off nominal
oracle plus independent public reroot checks. At depth one, ordinary nonterminal
move values matched `-E(child)`; parent E was obtained offline, never injected
into the observed production tree. Runtime prototypes paid for lazy parent E at
most once per node; depth two reused an actually computed local SR-019 E without
changing that mechanism. No retained evaluation stack was justified.

#### Prediction, resources and economics

Depth-one margin 256 failed fresh and sparse validation. A quiet king move gained
356 HCE points, including 348 from passed-pawn evaluation, and exceeded alpha by
76 despite 280 points of headroom. Further sparse cases missed cutoffs; excluding
pawn moves would not protect these non-pawn resources. The conservative fixed
512 policy had no false omissions in 144,703 sampled corrected active prunes,
but did not earn robust economics. On a fresh depth-seven suite it saved 5.3%
nodes and 11.2% evaluator calls yet cost 3.5% wall time in the repeated clean run.
A deeper suite saved 8.7% wall time, but removing its three largest winners left
a 2.8% regression. Quiet generation and make/check classification were still paid;
omissions saved child preparation and Search, not the whole move-generation cost.
Changed history/order and lost affected TT evidence substantially changed later
work: fewer total nodes did not imply fewer positive-depth TT operations.

Depth-two margin 512 failed independently: one quiet king move had headroom 838
yet exceeded alpha by 84; a quiet pawn move exceeded alpha by 328. These original-
path nominal witnesses involved passed-pawn/king transformations, not current
captures or checking moves. They establish local false omissions, not measured
root-score loss or Elo. A final fixed 1024 margin was frozen before new general
and resource holdouts, including both root-depth parities. It had no sampled
false omissions there or in 19,146 active fresh prunes, but gains remained
concentrated: 2.9% and 7.4% aggregate wall savings became neutral or regressing
after removing the three largest winners. A further fresh suite saved only 0.6%
nodes and cost 0.9% wall time. Same-parent SR-019 eval reuse did not establish
stable incremental value. No further thresholds, depth-conditioned formulas,
tactical/deeper expansion or witness-specific exclusions earned adoption.

#### Proof, returned thresholds and history

An actual omission creates a local dependency. While it survives, the returned
result cannot be published as ordinary mathematical EXACT/UPPER/LOWER evidence.
The prediction is only `S <= alpha`: if searched best is lower, omission must
not silently assert that stronger fail-soft upper bound. The corrected prototype
raised its predictive return floor to the alpha used for omission without
inventing an omitted-move PV. Independently searched siblings retained normal
fail-soft/PVS semantics. A later independent nominal beta cutoff could discharge
earlier local omissions for its LOWER proof. Primitive returned dependency and
local pending state sufficed; conservative top-level selective-work provenance
remained distinct. SR-018/SR-019's monotonic epoch suppression was unchanged.

Omitted moves were generated but unsearched: no searched-ordinal increment,
history reward or later cutoff malus. Compacting only the consumed searched
prefix preserved the unconsumed staged/lazy ordering and ordinary history updates.
Targeted threshold, TT precedence/discharge, history/ordinal and evaluation-reuse
checks passed; all 27,264 sampled candidate TT writes in the corrected active
audits passed their matching-depth original-path oracle checks. These are
research findings, not installed production representation or proof recovery.

No policy or research hot-path state is retained in production. Reconsider only
after an explicit programme decision and a concrete evaluator, Search/TT economics
or mechanism-specific evidence change plausibly addresses the observed resource
and economic limitations. Convention, nearby margin sweeps and node savings
alone are insufficient. No self-play/Elo or universal futility-inferiority/safety
claim is made. Reproducible sources, captures, witnesses, timing and validation are
preserved at `C:/Users/Central/Documents/SeedV6-SR021-2026-09-30/REPORT.txt`.

### LOCKED check-extension disposition (SR-030)

SR-030 is **REJECTED** for incorporation into current production/default Search
under the researched single-thread HCE, static-leaf and value-equivalent TT
conditions. Giving-check replies contained useful tactical/draw information,
but the tested boundary policies did not establish sufficiently repeatable
root-decision value for their practical cost. Production retains nominal-depth
PVS composed with unchanged SR-018/SR-019. Ordinary exact constructors, the
TT-off oracle, neural/custom defaults and SR-001's qsearch rejection are unchanged.

#### Researched horizon and path semantics

The initial policy extended a legal giving-check **edge at parent remaining
depth exactly 1**, substituting child depth 1 for depth 0: integer +1, with
one path-local credit. It included quiet/tactical checks, root/interior edges,
first/later PVS moves and mate/rule-50 windows. It was not an in-check-node rule:
positive-depth checked nodes already search evasions; a checked depth-zero root
retained ordinary terminal/draw/static resolution. After the credit was used,
no descendant could extend. Measured maximum added real horizon was one ply;
counterchecking evasions exercised the exhausted-credit boundary.

Eligibility and child depth were determined once for each legal edge, before
its initial child search. Scout and required full re-search used identical
depth/credit, with no second searched-ordinal increment. Existing fail-soft,
ordering, tactical classification and completed quiet-cutoff history rules were
preserved; giving check earned no special history update. Real root/path ply
continued to control mate distance, evaluator slots and legal history. The
research result allowed an extra legal PV ply without calling it qsearch or
increasing reported nominal depth; production's result boundary was not changed.

Extra evasions legitimately exposed repetition and rule-50 draws. Clock-99
checked-child fixtures distinguished nonzeroing evasions reaching 100 from a
zeroing capture that retained a non-draw value. A history-preserving repeated
evasion likewise exposed a draw absent at the static child. No rule-50 exclusion
was justified. Synthetic SR-018 null subtrees never used SR-030 credit or
ordinary TT evidence; legal SR-018/SR-019 composition was otherwise unchanged.

#### Value-equivalent TT and provenance evidence

Unused and consumed credit are different value semantics at the same board and
remaining depth. Ordinary nominal TT proof cannot resolve an unused-credit
invocation. A consumed depth-one child is an ordinary depth-one invocation;
that fact does not make its selectively extended parent ordinary nominal proof.
An actual extension marks the affected completed result selective; enabling the
mode without extension work does not. Policy-TT reuse must preserve evidence of
actual extended work, including when a hit avoids executing the original edge.

The first conservative prototype suppressed incompatible ordinary score reads
and affected ancestor writes, retaining legal hash ordering. It cost about
2.15 times baseline wall time. A single research table with separate
unused-credit keys and explicit stored extension provenance restored valid
policy reuse and removed most of that artificial cost. Consumed-credit entries
retained ordinary keys and actual-depth proof; SR-018/SR-019 still suppressed
their affected stores. All 17,411 sampled ordinary/policy writes passed their
matching-depth, original-ply, TT-off value/bound audits. These are research
findings, not an adopted TT namespace, payload encoding, proof-recovery design
or second persistent production table. Ordinary section J semantics remain intact.

#### Information, economics and limits

Outcome-blind observation preserved production result/PV/tree, evaluator calls,
TT operation streams and quiet-history fingerprints. The 19,334 retained states
preserved complete game/path history, original ply, windows and factual strata.
TT-off replay compared child horizons 0..3, with sampled horizons 4/5 and public
reroot checks. One reply often improved deeper-horizon decision agreement and
exposed material/draw resources, but disagreements remained parity-dependent.
Deeper exact values are horizon evidence, not general game-theoretic truth.

An active promotion witness changed a nominally attractive king move whose
promotion could be captured into a move retaining a later forced mate. Conversely,
an adverse queen-versus-king witness changed the defender's move to a line losing
two real plies sooner; independent searches through depth 8 confirmed the mate
distances. Fresh general root choices also improved at one deeper parity while
worsening at the next. Local checking-child information did not reliably transfer
to a better root decision.

A directly motivated tactical-only variant retained captures, en passant and
promotions that gave check, with the same depth/credit policy. Its fresh changed
general move was worse at both deeper horizons; a resource move gained 101 HCE
points at one horizon but lost 321 at the next. It did not establish sufficient
root benefit. No arbitrary ordinal, root, window, history or evaluator formula
was introduced to fit those witnesses.

Final warmed, rotated, three-round measurements used simplified research code
against unmodified production, with same-request iterative TT reuse. All-check
wall costs were approximately **+14.9%** on fresh depth-7 walks and **+12.6%** on
the depth-8 suite; tactical-only costs were **+8.8%** and **+14.4%**. Node growth
was about 12.3%/12.3% and 6.4%/13.1%, respectively; throughput changes were small.
Regressions remained after removing each policy's three largest costly positions.
At 300,000 nodes, all-check/tactical-only completed one less nominal depth on
5/4 of 36 fresh roots, with no deeper completions. This is practical search
economics, not playing-strength/Elo evidence.

Historical SeedV3 used a +1 **in-check-node** extension before its leaf boundary,
without a depth/check-class/move-ordinal/mate-window filter. Its propagated flag
limited ordinary paths, but null and IID calls reset it; checked qsearch also
continued independently. Different pruning, TT and ordering semantics and the
absence of isolated controlled check-extension performance evidence prevent
transferring a historical success claim or formula to this architecture.

No production extension or research hot-path state is retained. A separate
in-check mechanism, another depth band, larger cap or fractional accounting did
not earn incremental investigation from these results. Reconsider only through
an explicit programme decision after a concrete evaluator/Search/TT dependency
change or mechanism-specific evidence plausibly improves root-decision value
relative to cost. Conventional check-extension practice, nearby eligibility
tuning, local oracle agreement or nodes alone are insufficient. SR-001/SR-021/
SR-029 remain closed; no singular, other tactical or adaptive-depth feature was
started. Reproducible sources, states, witnesses, audits and measurements are at
`C:/Users/Central/Documents/SeedV6-SR030-2026-10-01/REPORT.txt`.

### LOCKED singular-extension disposition (SR-031)

SR-031 is **REJECTED** for incorporation into current production/default Search
under the researched single-thread HCE, static-leaf, PVS and mathematical-TT
conditions. Strict same-horizon singularity was correctly established, but its
use as a reason to buy one extra candidate ply did not establish sufficient
root-decision value for its cost. Production retains nominal-depth PVS and the
unchanged SR-018/SR-019 composition, ordinary exact constructors, the TT-off
oracle and neural/custom defaults. This is a bounded current-baseline conclusion,
not a claim that all singular-extension mechanisms are universally inferior.

#### Same-horizon evidence and restricted proof

The initial classifier used a legal ordinary TT/hash candidate, current request
generation, stored remaining depth **exactly h = D - 1**, EXACT or LOWER type,
an ordinary non-mate anchor A, and at least one other legal move. Root/interior,
checked/non-check, quiet/tactical and scout/full invocations were included.
NO_LEAF_TT naturally made D >= 2; no conventional minimum-depth formula was
introduced. The hash move supplied candidate/order evidence separately from the
score. The TT score was mathematical evidence at h, never proof of the current
depth-D value. Depth/generation mismatches and other types did not qualify.

With **G = 0, T = A**, a completed fail-soft restricted search of the same
position and real history at h, excluding exactly that candidate, used the
integer window **[A - 1, A]**. Fail-low established every alternative < A;
fail-high did not establish singularity; cancellation established nothing.
An exact alternative value was unnecessary for this binary proof. Ordinary
anchor endpoints remained safe in the established score domain; mate anchors
were recorded separately and excluded from active classification.

For LOWER evidence, the stored hash move need not have caused the original
bound. If the unrestricted value is >= A and every other legal move is < A at
the same horizon, the candidate necessarily supplies the separating value.
Oracle and unrelated-hash fixtures verified this reasoning. It establishes
separation at h, not persistence, a nominal-depth proof or extension authority.

The restricted root has different value semantics: ordinary TT scores cannot
resolve it and its result must never be stored as ordinary-position proof.
The excluded move is unsearched, with no searched-ordinal increment or history
reward/malus. Research suppressed all exclusion quiet-history writes, NMP and
static-null predictions. Once a real alternative is made, ordinary descendants
can safely reuse/publish actual-depth proof under the complete SearchKey, real
history, generation and ply rules. All 259 dedicated reuse comparisons preserved
the restricted bound; prefilled ordinary descendant evidence reduced their
nodes from 13,199 to 4,285. Active measurements retained that reuse.

#### Extension, value identity and provenance

The active hypothesis assigned the qualifying candidate child depth **D**
instead of D - 1, with integer +1 and one credit per real path. Consumption
prevented further extension on that path; subsequent edges declined normally.
The legal hash candidate was first at its own node; that node could itself be
a PVS scout. Assigned depth/credit stayed fixed across initial and required
full searches, without a second ordinal increment or extension. Real path ply
continued to control mate distance, evaluator slots and history. Extra real
plies legitimately exposed rule-50/repetition draws, promotions and mates;
no new rule-50 guard was justified. Synthetic SR-018 subtrees remained isolated,
and real extended subtrees restored accepted SR-018/SR-019 composition.

Actual horizon substitution and affected ancestors carried selective provenance
and could not publish ordinary nominal-depth proof. Enabling the policy or
discarding a nonsingular probe alone did not taint a result. Unaffected siblings
and consumed-credit subtrees retained ordinary actual-depth TT participation.
Separate primitive extension and existing prediction epochs sufficed for the
conservative research suppression; no general evidence/context object or
independent proof-discharge rule was adopted.

Credit state alone is insufficient TT identity for a mechanism whose future
eligibility depends on mutable TT evidence. The research path froze ordinary
anchor entries at each iteration's start. Available-credit policy scores used
a separate key domain valid only for that snapshot; each new snapshot changed
the domain. Cached extended work retained extension provenance, and cached PV
prefixes came from the cached result's own move, not its anchor candidate.
SR-018/SR-019-affected results stored in neither domain. Consumed-credit exact
work used ordinary keys. All 20,160 sampled ordinary writes, 9,578 active
exclusion bounds and 4,927 policy-domain bounds passed their original-ply,
matching-horizon audits; policy audits disabled policy-score reuse in replay.

This was a research-only value-equivalence treatment, not an accepted snapshot,
TT namespace, payload, allocation or production-cache architecture. Its frozen
population excludes newly created within-iteration anchors. Its measurements
must not be presented as the economics of every dynamic singular policy.

#### Decision evidence, economics and limits

Outcome-blind observation preserved production score, move, PV, node/evaluator
counts, TT operation streams and quiet-history fingerprints over 445 iterations
of 89 roots. It saw 39,014 eligible anchors, including 82 mate anchors, and
retained 2,869 full board/game/path states. TT-off replay independently enumerated
candidate and best-alternative values at h, h + 1 and h + 2. Of 2,845 ordinary
samples, 1,717 were strict; all passed their same-horizon proof checks. Only
764 stayed strictly separated at both deeper horizons. Alternatives caught up
or exceeded the candidate in 658 cases at h + 1 and 842 at h + 2. Checked nodes,
LOWER anchors, small gaps and parity reversals were represented. These are
stratified workload samples, not universal probabilities or mate-score averages.

Tactical persistence supplied enough useful concentration to test the bounded
all-legal active hypothesis, but complete root results did not support adoption.
On 35 fresh depth-four roots, ten choices changed: four were better at both
deeper exact horizons and two worse at both, with parity-sensitive outcomes.
A 140-root resource workload, including repeated starting positions, changed
50 choices: ten were better at both and 25 worse at both. A further 36 fresh
depth-five roots changed five choices: one better and four worse at both
depths six and seven. Deeper fixed-depth evidence is not game-theoretic truth.

A fresh rook/pawn witness chose Rd2 instead of gxf3, losing 412/560 HCE points
at depths six/seven and remaining 295 worse at depth eight. Another witness
delayed a queen promotion for Nc3 and remained worse through depths four to
eight, including losses of 829/841 at the odd deeper horizons. Counterevidence
includes a king move that delayed forced mate by two real plies, independently
confirmed at depth eight. Local useful information did not establish sufficient
overall decision value. TT-off/ordinary TT-on witness comparisons agreed.

Final clean, warmed, rotated three-round timings used unmodified production
CONTROL and value-equivalent research reuse. Excluding snapshot-copy cost in
the candidate's favour, wall time was **3.12 times** baseline on 34 fresh
depth-seven roots and **3.60 times** on 12 depth-eight roots; node ratios were
2.97 and 3.48. Removing each suite's three most costly positions still left
2.91/2.73 times wall cost. These are optimistic timings for this prototype,
not a lower bound on all possible implementations. A classifier-only ablation
under the same snapshot/domain mechanics still cost about 2.99/2.91 times;
it is not a minimal ordinary-Search-plus-probe implementation.

The active runs performed 68,410/79,177 extensions and spent 7.83/16.79 million
nodes in exclusion probes, with 51,149/56,349 probes not establishing singularity.
Extended-candidate work, evaluator calls, TT traffic/replacement and PVS
re-searches were counted separately. At a 300,000-node request budget, 26 of
34 fresh roots completed fewer nominal depths, eight the same and none more.
This does not equate nominal depth or NPS with strength; no Elo claim is made.

Historical SeedV3 briefly enabled a hash-driven +1 extension before disabling
it. Its last active trigger used depth >= 5, a shallower TT entry and an unused
shared extension flag, without current-generation or EXACT/LOWER qualification.
Its margin-50 alternative searches called selective Search at child depth D - 2
with full windows, qsearch and other historical mechanisms, without requiring
the anchor's exact horizon to match. The current helper is unused. Historical
presence, introduction/disable commits and comparison prose supply no isolated
strength evidence and authorize no formula transfer.

No production extension or research hot-path state is retained. Positive-gap
or conventional depth formulas, broader anchors, double/multiple/fractional or
negative extensions, reductions, multi-cut and history modifiers did not earn
additional investigation from this strict-policy evidence. Reconsider only
through an explicit programme decision after a concrete evaluator/Search/TT
dependency change or mechanism-specific evidence addresses useful root decisions
relative to probe cost; nearby tuning, convention or local separation alone is
insufficient. HCE numerical findings do not generalize to neural evaluation.
No other frontier feature was begun. Reproducible sources, captures, witnesses,
audits and measurements are preserved at
`C:/Users/Central/Documents/SeedV6-SR031-2026-10-01/REPORT.txt`.

### LOCKED other tactical/forced-line extension disposition (SR-032)

SR-032 is **REJECTED** for incorporation into current production/default Search
under the researched single-thread HCE, static-leaf and PVS conditions. Exactly
one legal reply supplied useful local information, but the bounded boundary
extension did not establish sufficiently consistent complete-root decision value.
Production retains nominal-depth PVS with unchanged SR-018/SR-019, static leaves,
ordinary exact constructors, the TT-off oracle and neural/custom defaults.

#### Factual uniqueness and the researched horizon

A nonterminal position with exactly one legal move proves that its transition
contains no move choice. It does not prove static-evaluation error, extra-depth
necessity, deeper truth or root benefit. The tested predictive proposition was
that the horizon after that compulsory transition is more decision-informative
than the horizon immediately before it.

SR-017 already establishes the complete count at checked leaves through legal
evasions, and at non-check leaves with no legal tactical through the quiet
generation needed to distinguish stalemate. These were the initial cheap
population. A non-check tactical-present leaf does not establish a complete
count; offline full generation studied that excluded population without adding
production classification. Promotion choices remain distinct legal moves.

The active policy searched the unique move at the nominal static boundary,
with integer +1 and one credit per real path. Its child reached ordinary depth
zero at real ply +1; no second credit or nested extension was permitted. All
caller windows, checked/quiet-only cases and root leaves were included. Real
history, evaluator slots, rule state and mate distance advanced normally. A
forced edge had no sibling ordering competition and earned no quiet-history
reward/malus. Ancestor scouts and full re-searches retained identical credit
and eligibility, without another searched-ordinal increment. Synthetic SR-018
subtrees could not extend; normal legal SR-018/SR-019 composition was preserved.

Real additional plies legitimately exposed rule-50, repetition and insufficient-
material draws; no artificial clock exclusion was justified. A constructed legal
four-transition forced chain confirmed the cap. Its exact values at horizons
0..4 were -59, 704, -461, 766, 758: another forced ply can reverse information
again. No nested forced leaf occurred among 1,368 sampled cheap unique leaves;
no baseline-known nested forced leaf occurred in the final counted timing
workloads. This is bounded frequency evidence,
not proof chains cannot occur. Repeatedly offsetting every forced transition
would preserve remaining depth along the chain; extra credits were not earned.

#### TT and provenance findings

Available-credit policy value differs from consumed-credit ordinary value.
Research used one TTable with a factual credit-dependent SearchKey domain and
cached actual-extension provenance; no mutable anchor snapshot was required.
Equal depth/current generation, ordinary bound meaning, legal cached move,
mate normalization and terminal/draw precedence remained mandatory. Ordinary
static leaves retained NO_LEAF_TT. In this boundary-only policy, consuming the
credit reaches depth zero immediately, so no positive-depth consumed subtree
needs an ordinary TT write. A policy hit bypassing the extended edge still
propagated actual selective-work provenance. Enabling the policy without an
extension did not itself taint the result. SR-018/SR-019's separate prediction
epoch continued to suppress their affected stores in either domain.

All 24,910 sampled policy writes passed original-ply TT-off policy-value/bound
audits, including 1,149 carrying extension provenance and 150 mate-band values.
These are exact proofs for the explicitly researched policy, not ordinary
nominal-depth proofs. A domain-only ablation preserved baseline values, choices
and trees on 84 tested roots. No production TT namespace, score payload, credit
stack, extra table or generic evidence/context object is adopted.

#### Decision evidence, economics and limits

Outcome-blind observation preserved score, move, PV, nodes, evaluator/child calls,
TT operation streams and quiet history over 445 iterations of 89 roots. The
9,127 retained full board/game/path states were replayed TT-off at horizons
0..3, with systematic sampled 4/5 and public reroot checks. Among 421 fresh
checked-one leaves, +1 was closer to both +2/+3 in 267 cases and farther in 60;
87 reversed by parity. Quiet-only uniqueness was much weaker (16/100 closer,
73 farther). Local resource discovery and that checked concentration justified
testing the all-cheap-unique hypothesis, not a tuned eligibility formula.

Across 392 complete quality workload roots at nominal depths 4..6, 13 choices
changed: four were better at both deeper exact horizons, six worse at both and
three reversed by parity. These correlated general/resource workloads are not
independent strength trials. The fresh depth-six changes were both worse at
exact depths seven/eight. One chose Bb2 instead of Re3 and lost 129/29 HCE
points there, remaining worse at every tested horizon four through eight.
A separate depth-eight choice of exd5 instead of gxh3 gained 60 at depth nine
but lost 107 at depth ten. Deeper values remain horizon evidence, not truth.

Useful counterevidence includes a queen-versus-king defender choosing Kf3 over
Kg4: it improved at horizons five through ten, where Kg4 lost by a demonstrated
mate and Kf3 still had an ordinary value. This does not prove Kf3 draws. Draw
and material witnesses confirm genuine local benefit without establishing
sufficient general decision value. Most workload roots retained their choice;
extension-event counts do not identify each event's causal root contribution.

Clean, warmed, rotated three-round measurements against unmodified production
used same-request iterative TT reuse and no new classification generation.
On 34 fresh depth-seven roots, wall time was +0.8% with round variation spanning
zero and nodes -0.45%: no material timing benefit was established. On 12 depth-
eight roots, wall time was +7.3% and nodes +7.5%, with essentially unchanged
throughput. Removing the three largest cost increases left approximately neutral
timing in both suites. At 300,000 request nodes all 34 fresh roots completed the
same nominal depth. These modest/concentrated economics differ from SR-031's
probe expense; rejection rests on insufficient reliable root value, not a claim
that a forced edge intrinsically causes severe expansion or lower strength.

The additional child replaces the parent static evaluation; it need not add an
evaluator call. Changed values, cutoffs, ordering and TT reuse can change later
tree work. Historical engine formulas and strength claims under other leaf/TT/
pruning architectures supply context, not authority for this policy.

No production change is retained. Checked-only/tactical-only tuning, larger caps,
fractional accounting or another conventional family was not earned. The results
did not isolate a stronger predictive recapture, promotion/passed-pawn or threat
signal justifying a pivot. Those families were not exhaustively implemented;
this is a current-baseline non-incorporation conclusion, not universal rejection
of every conceivable extension. Reconsider only through an explicit programme
decision after a concrete dependency or mechanism-specific evidence change
addresses persistent complete-root benefit relative to cost. Do not reopen
SR-030/SR-031 with nearby variants or begin SR-033 from this result. No Elo or
neural-evaluator generalization is claimed. Reproducible sources, states,
witnesses, audits and measurements are retained at
`C:/Users/Central/Documents/SeedV6-SR032-2026-10-01/REPORT.txt`.

### TENTATIVE node lifecycle

This tentative model describes possible later evidence-driven Search. Exact
TT stages must respect section J's LOCKED evidence and precedence rules;
selective stages beyond the accepted SR-018/SR-019 policies remain conditional on future
accepted design. Neither is a requirement of the independent TT-off exact
reference path, and neither may
override its LOCKED semantics.

1. Enter the node and establish invocation context.
2. Resolve terminal state and usable transposition information.
3. Derive available proof, provenance and predictive evidence under the LOCKED
   orthogonal model, without assigning an authoritative PV/Cut/All node role.
4. Determine permitted selectivity.
5. Generate and order moves.
6. Search moves.
7. Apply any justified reductions, pruning, extensions or re-search behaviour.
8. Establish cutoffs and the result.
9. Store only valid transposition information.
10. Return the result.

This is a working lifecycle, not a finalized execution ordering beyond the
LOCKED exact traversal in section I, precedence in section J and staged/lazy
move traversal in section L and the concrete SR-018/SR-019 lifecycles above. Other
evidence consumers and how selective work and reduced-depth re-search fit within
move traversal require further design;
the general SR-008 evidence model and SR-009 static-evaluation dispositions are
settled.

### TENTATIVE allocation of search effort

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
   current exact PVS/TT rules, including interaction with future selective
   Search and future narrow-window uses outside section K's SR-004/SR-006
   rejections. The TT bound meanings, applicability and mate-score
   normalization, ordinary exact PVS scout/re-search rules, negamax window
   transformation and fail-soft return policy are LOCKED.
3. **Mechanism-specific evidence use:** SR-008 settles the orthogonal evidence
   model and rejects authoritative entry-time PV/Cut/All roles. Future consumers
   must justify their use of predictive signals and any additional path state
   under sections C, F and L; no selective mechanism is authorized by SR-008.
4. **Further transposition-table evidence and policy:** Section J locks
   `TTable` mechanics and current single-thread policy. TT interaction with
   future selective Search and future narrow-window uses outside section K's
   SR-004/SR-006 rejections; broader proven history/path equivalence
   classes beyond SR-010's settled ordinary count equivalence; partial-work
   non-score evidence; and
   parallel/shared TT architecture, including Lazy SMP, remain OPEN. Ordinary
   exact PVS interaction is settled by SR-003. Parallel/shared TT is deferred to
   SR-036 and does not block the settled current single-thread programme. The
   accepted 64 MiB requested fixed default is implemented.
5. **Further static-evaluation use and dependencies:** SR-009's evidence model,
   movement non-adoptions and current correction-implementation disposition are
   settled above, with SR-018/SR-019's HCE-only consumers and calibration implemented.
   Further mechanism-specific use, calibration and neural adoption remain future
   work under the LOCKED evaluator-independent boundary. Correction reconsideration requires
   the stated material dependency change; production learning/table/update
   mechanics must then be researched, not inherited by convention.
6. **Move-order evidence:** further implications of TT selection, tactical
   status, historical success, move rank and late position, beyond the LOCKED
   distinction between hash-move ordering evidence and score proof and
   section A's accepted SR-014/SR-015/SR-016 architecture and section L's
   SR-017 mechanics and SR-008's accepted predictive distinctions, subject to
   section K's SR-005 non-adoption boundary.
7. **Further selective mechanisms:** unresearched reductions beyond SR-029's
   current-baseline rejection; pruning beyond SR-018/SR-019 and SR-021's
   researched rejection;
   narrow/probe searches; technique-specific eligibility and aggression.
8. **Extensions and re-search:** beyond SR-030/SR-031/SR-032's researched
   extension rejections and their evidence/dependency reconsideration boundaries,
   when earlier assumptions require
   additional proof; when reduced/narrow searches must be widened or deepened beyond
   section I's settled ordinary exact PVS re-search rules.

## Explicitly unresolved techniques and parallelism boundary

The following remain **OPEN**; their conventional implementations are
**not LOCKED** as accepted Search architecture:

- Null-move extensions beyond the accepted SR-018 scope.
- LMR variants beyond SR-029's researched rejection, subject to its
  evidence/dependency-based reconsideration boundary.
- Futility variants beyond SR-021's researched rejection, subject to its
  evidence/dependency-based reconsideration boundary; reverse-futility extensions
  beyond the accepted SR-019 scope.
- Razoring.
- ProbCut / MultiProbCut.
- Extension variants beyond SR-030/SR-031/SR-032's researched rejections,
  subject to their evidence/dependency-based reconsideration boundaries; no
  conventional untested family is implicitly authorized.
- Reduced-depth and other re-search rules beyond section I's ordinary exact PVS.
- Further iterative-deepening heuristics beyond section K's LOCKED decisions.
- TT policies beyond section J's current single-thread rules, as listed in
  the OPEN frontier, and TT interaction with future selective Search and
  downstream narrow-window uses outside section K's SR-004/SR-006 rejections.
  Ordinary exact PVS interaction is settled and implemented; future changes
  require separate authorization.
- Move-order policy beyond the accepted SR-014/SR-015/SR-016 architecture and
  SR-017 mechanics; broader quiet-history lifecycle integration.
- Explicitly justified future evaluator calibration or evaluator-specific Search
  evidence, subject to SR-009's settled dispositions and the LOCKED
  evaluator-independent core boundary.
- Detailed time-management algorithms (SR-034), including active score/best-move
  stability use for time allocation, stopping or easy-move behaviour.
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
path; ordinary TT-on constructors provide optimized exact execution using proven
reusable evidence, with PVS as the production/default traversal under section I's
accepted scope. TT
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
result for those exact modes. It indicates incorrect evidence applicability or
a deliberate Search semantics change requiring separate authorization. SR-018/SR-019's
separately selected production mode is such an explicitly reconciled selective
change: compare it to the oracle as predictive research, preserving the exact
constructors and all ordinary TT proof semantics.

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

Future Codex tasks may update this canon when the task explicitly authorizes
canon maintenance and its evidence-supported accepted result materially changes
durable Search semantics, architecture, policy or accepted direction. An explicit
launch of an autonomous Search programme run also grants this maintenance
authority for conclusions reached within the selected admitted feature's scope;
no separate per-feature canon-maintenance permission is required.

That programme grant includes researching, establishing evidence-supported
feature dispositions, and implementing/adopting justified results under this
contract and the frontier's feature-level cycle. It does not authorize silently
violating or overriding LOCKED decisions, materially redefining the programme,
or bypassing existing reopening conditions. Deliberately overturning a LOCKED
programme-level principle or requiring an owner/programme decision outside the
established grant is an escalation condition, not authority Codex can create by
editing this canon. Any particular change already explicitly authorized by the
current canon remains governed by that authority and its conditions.

Modify only affected sections without rewriting unrelated canon content.
Preserve revision history and the stable filename; do not turn this contract
into a worklog.

For each material Search-contract change:

1. Update the repository master.
2. Increment the internal canon revision exactly once for the material
   update, continuing the `R001` format; never put it in the filename.
3. Append a concise revision-history entry and report
   `CANON_UPDATED: <old revision> -> <new revision>` and
   `PROJECT_SOURCE_REFRESH_REQUIRED: YES`.
4. During an authorized autonomous programme run, continue from the current
   repository masters even while Project Source mirrors lag. At an external
   handback, blocker or final programme return where GPT/user coordination
   resumes, require the human to refresh both Search Project Source mirrors to
   the current repository revisions before subsequent GPT reasoning from them,
   including construction of a further programme launch prompt. This refresh
   is a gate on that GPT use, not on inter-feature Codex continuation from the
   repository. Report the outstanding refresh; a request is not evidence that
   the human completed it, and synchronization must be confirmed before relying
   on those mirrors again.

Ordinary implementation details, bug fixes, experimental runs or measurements
do not require a canon revision unless they materially alter durable Search
design or accepted direction. Canon maintenance alone does not authorize engine
changes; those require feature/task authority, including the explicit autonomous
programme grant above. It does not authorize edits to the historical transplant
report or automatic changes to ChatGPT Project settings/sources.

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
| R016 | Closed SR-001 as REJECTED for current production/default Search under researched conditions; retained static leaves and independent qsearch research/reference evidence, reconciled qsearch/TT OPEN statements, and required a material dependency or architecture change for reconsideration. |
| R017 | Closed SR-005 as REJECTED for additional explicit previous-PV/best-move ordering, redundant with same-request TT/hash-move reuse under researched conditions; preserved completed-iteration semantics and OPEN SR-006/SR-034 consumers, with reconsideration requiring a material dependency or architecture change. |
| R018 | Closed SR-006 aspiration windows as REJECTED for current production/default Search under researched single-thread/HCE conditions; retained full-window successive-depth SearchDriver behaviour and research-only support, reconciled OPEN references, and required a concrete material dependency or architecture change for reconsideration. |
| R019 | Closed SR-004 MTD(f)/memory-enhanced zero-window root drivers as REJECTED under the researched current single-thread/HCE architecture; retained full-window successive-depth PVS, preserved oracle-headroom nuance, reconciled OPEN references, and required a concrete material dependency or architecture change for reconsideration. |
| R020 | Closed SR-008 as ACCEPTED: locked the orthogonal invocation/proof/provenance/predictive evidence model without authoritative entry-time PV/Cut/All roles; distinguished proof strength from completed-window classification, constrained path state to justified consumers and implementation economy, preserved the SR-009/evaluator boundary, and authorized no selectivity or production Search change. |
| R021 | Closed SR-009 as ACCEPTED evidence/disposition: locked static-evaluation semantics, bounded HCE tactical/residual evidence and movement non-adoptions; deferred correction implementation to material dependency changes, reconciled OPEN references, and preserved evaluator independence, implementation economy and separate selective-mechanism research. |
| R022 | Implemented SR-019's calibrated HCE-only depth-two scout static-null policy, with beta predictions, conservative ancestor TT suppression and explicit result provenance; preserved independent exact paths and neural defaults, reconciled the raw-eval consumer dependency without adopting correction history, and bounded further selective expansion. |
| R023 | Implemented SR-018's HCE-only depth-4..6 scout null move with fixed R=2, measured eval/material/rule-50 guards, isolated synthetic history and TT-free probes; preserved discarded-probe proof and committed ancestor suppression, did not adopt adaptive reduction or verification, and recorded the additional raw-E dependency without reopening other features. |
| R024 | Closed SR-029 as REJECTED for researched quiet LMR: substantial fixed/two-step economic headroom did not adequately preserve late quiet promotion and mate resources, including after a bounded promotion guard. Recorded actual searched ordinal and reduction-specific proof/TT research boundaries; retained unchanged nominal-depth production PVS and SR-018/SR-019 without starting another feature. |
| R025 | Closed SR-021 as REJECTED for researched static-leaf quiet futility: small margins missed substantial passed-pawn/king resources; conservative independently calibrated depth-one/depth-two policies did not establish repeatable wall-time value. Recorded alpha-threshold prediction, omission dependency/discharge and searched-history semantics as research evidence; preserved production SR-018/SR-019 and began no next feature. |
| R026 | Closed SR-030 as REJECTED for boundary giving-check +1/cap-one and tactical-only policies: useful local tactical/draw evidence did not establish repeatable root-decision value for fresh/deeper costs. Recorded credit-dependent TT/provenance, horizon/PVS/history and rule-state research findings without production adoption; preserved SR-018/SR-019, exact/oracle paths and closed-feature boundaries. |
| R027 | Closed SR-031 as REJECTED for strict same-horizon ordinary-TT singularity with +1/cap-one extension: valid exclusion proof did not establish sufficient persistent separation, root-decision value or economics. Recorded restricted-domain proof, TT-dependent policy identity, real-ply/provenance and descendant-reuse findings as research evidence; retained production SR-018/SR-019, exact/oracle paths and all other feature boundaries. |
| R028 | Closed SR-032 as REJECTED after cheap exactly-one-legal-reply boundary +1/cap-one research: useful local draw/material information and an isolated mate-delay resource did not establish persistent complete-root value. Recorded factual uniqueness, chain/parity, credit-domain TT/provenance and modest/concentrated economics; retained production Search and began no other feature. |
| R029 | Closed SR-027 as REJECTED for researched exact phase-local child-TT look-ahead: retained the fail-high proof and proof-only result/history findings, but additional traffic, fresh shallow costs and concentrated deeper gains did not establish a sufficiently consistent active policy. Preserved production Search and began no next feature. |
| R030 | Closed SR-010 with formal threefold/prefix-path and count-state GHI semantics, preserving full evaluator status, twofold nonterminal behaviour, TT/PVS/mate and SR-018 boundaries. Implemented exact primitive bucket-chain repetition scanning after equivalent-tree measurement; retained the conservative ordered SearchKey and no upcoming-repetition or clock-normalization mechanism. |
| R031 | Implemented SR-011 exact post-terminal mate-domain extrema, including the depth-one ordinary lower bound, original-window TT/PVS classification and return-only domain bounds. Verified numeric/path separation, endpoint witnesses and unchanged selective provenance/eligibility. Settled finite-horizon gap mathematics but rejected its runtime candidate for no additional work savings and unearned cost; preserved independent controls and began no other feature. |
| R032 | Authorized explicitly launched Codex programme runs to select, research, disposition and close successive admitted features, including justified adoption and canon maintenance. Required fresh repository-canon reads between features, preserved LOCKED/evidence/programme boundaries, and moved Project Source synchronization to external handback and subsequent GPT use; changed no Search semantics or feature dispositions. |
| R033 | Closed SR-007 as REJECTED: exact missing-hash IID variants did not earn repeatable economics, while one-/two-ply IIR retained prediction/resource failures despite bounded headroom. Preserved nominal production PVS, SR-018/SR-019, mathematical TT and independent oracle; recorded research-only same-ply mechanics and reopening boundaries. |
| R034 | Adopted SR-037 exact non-check static-leaf legal-existence querying without move materialization after profile-led alternatives, tree/state/oracle equivalence and repeated warmed production timing. Retained complete checked evasions, positive-depth ordering, terminal/draw/static/TT precedence, evaluator lifecycle, selective policies and independent references; made no playing-strength claim. |
