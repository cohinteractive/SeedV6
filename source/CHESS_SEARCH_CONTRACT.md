# SeedV6 Search Contract

Internal revision: **R001**

Status: **Active Search programme canon; architecture intentionally incomplete.**

Repository master: `source/CHESS_SEARCH_CONTRACT.md` in the `seedv6-nnue` repository.

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
and NNUE Training surfaces. Search must accommodate both evaluators and must
not accidentally assume that only one can exist, unless a later explicit
contract decision establishes such a dependency. NNUE training,
effectiveness and performance improvement remain a separate, parallel
workstream and do not block rebuilding Search.

The SeedV3 to SeedV6 transplant programme is complete to the level now
required. Its [transplant/discovery report](../SEEDV3_TO_SEEDV6_TRANSPLANT_DISCOVERY.md)
is preserved as historical evidence, not the active Search programme canon.
Existing implementation choices and transplant-era roadmaps do not become
settled decisions of this programme merely by already existing.

## LOCKED foundational principles

### A. Ordered alpha-beta is the foundation

Start from correct ordered alpha-beta with best-first move visitation. Move
ordering is part of Search's information foundation, not merely a cosmetic
optimization.

### B. Exact and selective search are distinct

Exact alpha-beta may omit work because mathematical bounds prove it
irrelevant. Selective search deliberately spends less work based on evidence
and therefore has different correctness and risk semantics. Do not blur these
categories.

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
legitimately affect Search decisions. Exactly which parent/path information
crosses the node boundary remains OPEN.

### G. Preserve independent correctness baselines

Do not optimize away independently useful exact/oracle search paths merely
because production Search becomes stronger or faster. Validate performance
improvements rather than assuming them.

## TENTATIVE working model

### Node lifecycle

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

This is a working lifecycle, not a finalized execution ordering. Refine it as
invocation semantics, transposition-table (TT) semantics and evidence
ownership are locked, including how selective work and re-search fit within
move traversal.

### Allocation of search effort

Spend search effort in proportion to the importance and uncertainty of the
decision being resolved. Moves or nodes capable of materially changing the
result may require stronger proof. Multiple reliable signals of low relevance
may justify reduced work. This is not yet a numeric formula or universal
policy.

## OPEN design frontier

The current reasoning sequence is open work, not a set of settled answers:

1. **Exact Search invocation semantics:** meaning of a Search call; remaining
   depth versus absolute ply; score perspective; alpha/beta window;
   completion/cancellation semantics; terminal and mate conventions.
2. **Alpha-beta window and bound semantics:** fail-soft versus fail-hard;
   EXACT / LOWER / UPPER meaning; caller/callee obligations; interaction with
   mate distance.
3. **Node evidence model:** what Search genuinely knows at node entry; what
   is derived locally; what may arrive from parent/path context; authoritative
   versus heuristic evidence.
4. **Transposition-table evidence:** applicability of stored information;
   depth and bound qualification; path-dependent exclusions; hash-move
   evidence versus score/bound evidence.
5. **Static-evaluation evidence and reliability:** how Search uses an
   evaluator; confidence in static evaluation; how handcrafted and NNUE
   evaluation fit the same Search contract; whether reliability/context
   signals are needed.
6. **Move-order evidence:** what TT selection, tactical status, historical
   success, move rank and late position actually imply; ordering metadata
   versus proof.
7. **Selective mechanisms:** reductions; pruning; narrow/probe searches;
   technique-specific eligibility and aggression.
8. **Extensions and re-search:** when earlier assumptions require additional
   proof; when reduced/narrow searches must be widened or deepened.

## Explicitly unresolved techniques and parallelism boundary

Conventional implementations of the following are **not LOCKED** as accepted
Search architecture:

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
- Iterative-deepening details.
- Parallel Search architecture.

These may emerge from the contract, be rejected or take materially different
forms. Their presence in existing code or historical reports does not settle
their role in the new Search design.

Define logical single-search semantics before allowing parallel execution
architecture to complicate the contract. Existing or experimental root
parallelism, Lazy SMP and proof-directed splitting may provide evidence
later; they are outside the immediate first-principles contract.

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
