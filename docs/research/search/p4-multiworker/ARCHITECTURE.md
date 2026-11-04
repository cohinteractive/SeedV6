# MW-R001 bootstrap challenge and external transfer map

4 November 2026. Source inspection at clean starting acc997b2, then the attributed
research-only probe. Static findings are not runtime qualification or evidence of
playing strength. [external-manifest.json](external-manifest.json) identifies locally
available pinned source bytes and retrieved history; raw files are ignored evidence.

## SeedV6 facts and material constraints

| Finding | Source and consequence |
| --- | --- |
| Construction production routing | BaselineEngine.configuration explicitly selects TC_P1/1177812991. ConstructionSearch chooses ExactSearchAdapter at W1 and ParallelSearch ROOT_REUSE at W2-W16, with ADAPTIVE16. No Search source difference between accepted build91 implementation and starting HEAD. Later committed training/tooling work is unrelated. |
| Root distribution | ParallelSearch.searchWindow searches the preferred/first root job on worker0 before executor submission. Remaining jobs share nextRoot; current completed splitBest supplies alpha. Every original-window scout fail-high below beta receives a full retry, even when another worker raised alpha. No assumption that an old bound proves the newer window. |
| Completion and publication | Root cutoff or completed coverage plus no incomplete job may produce winner after drain. Bound-only passes stay inside aspiration; only resolved iterations leave it. Private boards/evaluator/history stacks are reset only when workers are quiescent. Helpers do not independently advance depth in ROOT_REUSE. |
| Learning | ExactSearch.beginSharedRequest clears request learning; TC-P1 retains worker-private Construction learning across invocations/jobs during that request. seedRootHistoryFrom skips cross-worker quiet-history copying with REQUEST_LEARNING. Root PV/effort reuse is a separate completed-pass channel. No shared-history benefit has been measured. |
| TT channel | TTable uses stripe locks for coherent slot reads/writes. Current extensions suppress ordinary score stores/reuse; completed legal hints remain available and affect ordering/protection/reduction eligibility. Requested Hash64 has 2097152 slots, 50331648 bytes of three-long payload, plus array/lock/JVM overhead. That calculation is not a live heap measurement. No production qTT/raw-eval sharing. |
| Accounting | Shared SearchControl.tryEnterNode is an atomic global admission; successful child, qsearch/synthetic child and explicit same-position reenter callbacks increment private counts and delegate exactly once. Refused admissions do neither. Kernel-root counts differ from consumer admission counts intentionally. Hot global atomic contention is a hypothesis, not measured cost. |
| Stop/drain | Local abort and shared control checkpoints stop siblings; Throwable records failure. drain waits on every submitted Future, preserves owner interruption and requests STOPPED. endRequest/reset/close require idle. Executor threads are daemon threads but lifecycle qualification requires actual drain, not reliance on JVM exit. |
| Independent-worker integration constraint | SearchDriver sums per-invocation SearchResult nodes and constructs SearchDriverOutcome before its finally calls endRequest. It also overwrites completion depth with its own loop depth. A helper persisting across invocations could add unreported work, or a selected deeper result could be relabelled at the wrong depth. A naive asynchronous wrapper is inadmissible. Design request-level finalization/accounting and honest selection first, or use a fully drained per-invocation diagnostic with its narrower identity. |
| Historical alternatives are not exact TC-P1 | OFF/PRIVATE/COOPERATIVE/PLAIN/OWNER/OFFSET/MIXED predate current Construction. Existing construction factory chooses only ROOT_REUSE/ROOT_SPLIT; narrow-window access is restricted accordingly. Merely choosing an older enum would change kernel/dependencies and does not isolate a concurrency formulation. |

Representative files: `engine-core/src/main/java/com/ohinteractive/seedv6/search/`
`exact/{ParallelSearch,ExactSearch,ConstructionSearch,FlatSearchConfig}.java`,
`driver/{BaselineEngine,SearchDriver,AspirationSearch,ExactSearchAdapter}.java`,
`common/SearchControl.java`, `tt/TTable.java`. These observations justify diagnosis
and alternatives; they establish neither a defect in current ROOT_REUSE nor its
optimality. Prior technical acceptance and adverse W4 competitive results remain.

## Pinned external source observations

**Stockfish19** at edb0d9db6731067ec50ce619ff372b463bc4dd5d:
[search.cpp](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/search.cpp)
starts helper iterative deepening alongside the main worker. Workers have their own
root-depth/aspiration state; aspiration width includes worker identity. Main search
controls stopping, then joins helpers before selecting/reporting a result. This is
not SeedV6's root-job cohort. Its surrounding TT/selectivity/learning must travel
with any transfer hypothesis, not necessarily with its code or constants.

[thread.cpp](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/thread.cpp)
selects a worker after drain using move votes weighted by relative root score, plus
decisive-score and PV-length handling. The pinned selector does not weight votes
by completed depth. Therefore a generic description of Stockfish as selecting the
deepest worker is incorrect for this revision. Inexact decisive scores are treated
explicitly. No SeedV6 arbitration design is yet selected.

[history.h](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/history.h)
and search.h distinguish private main/low-ply/capture histories from NUMA-group
shared continuation, pawn and correction structures. Shared entries use
RelaxedAtomic-based statistics; bounded read/update/write can lose concurrent
updates. This does not mean all Search learning is shared or that Java plain arrays
would provide the intended visibility. Seed's current histories, labels, scale and
request lifetime differ. Any sharing experiment needs an explicit Java memory model
and signal-quality/cost test. [tt.cpp](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/tt.cpp)
stores move, score, evaluation, depth and bound in compact clustered records with
relaxed atomic fields; a record update is not one coherent atomic transaction.
Do not import that race tolerance as permission to weaken Seed's coherent contract.

**Ethereal14.00** at 96cde8d2f82a3ab0536bf33572405fda96f6e236:
[search.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/search.c)
starts helper iterative deepening, runs main Search, signals abort and joins before
selection. Its selector ranks retained worker PVs using completed depth, score and
mate preference, unlike the pinned Stockfish rule. Aspiration has explicit retained
line/update/revert behavior; its reporting semantics must be inspected before any
adaptation. [thread.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/thread.c)
allocates private boards/NNUE stacks and ordering histories; ucinewgame clears them,
while new-search setup primarily resets per-search statistics/state. Its shared
[transposition.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/transposition.c)
has clustered move/value/eval/depth/bound records, mate/TB normalization and age/depth
replacement. These are materially richer than hints-only TC-P1 sharing.

**Bagatur5.5c** at 56d1b3651f62bc770c7cde5f4c1b0360890b9bd5:
[MTDParallelSearch_ThreadsImpl.java](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/rootsearch/parallel/MTDParallelSearch_ThreadsImpl.java)
creates SequentialSearch_MTD workers with shared resource allocation, sets root-first
indices and wraps mediators. The active threads configuration uses a one-worker
depth-report threshold and disables restart-on-new-depth. Comments discussing other
thresholds are alternatives/claims, not active settings.
[SearchersInfo.java](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/rootsearch/parallel/SearchersInfo.java)
retains per-searcher/depth reports, rejects upper bounds or reports without PV/move,
and combines best-move/evaluation information with aggregate work counts. A lower
bound with PV is not thereby an exact completed result under Seed's contract.
MemoryConsumers allocates configurable TT groups; SearchEnv constructs private
history/correction objects. This is not evidence that all Bagatur state is shared.
The base coordinator's polling/start-stop machinery is a source observation, not a
recommended supervision design for Seed.

## Development evidence and its limits

- Stockfish [shared continuation history](https://github.com/official-stockfish/Stockfish/commit/f9b002cf7db4a6ec53dd5f06d72601638e326b82)
  records passing author-run tests at 8/16/64 threads, including 23472 games at
  8-thread 5+0.05 and 44692 at 20+0.2. It also reports roughly4-6% NPS loss on two
  large machines. This supports investigating strength despite speed loss, not
  expecting an identical Seed gain. Linked raw test data were not independently
  audited; the Fishtest page was unavailable through the web tool.
- Stockfish [thread-selection simplification](https://github.com/official-stockfish/Stockfish/commit/9449162d588b2a185471501ad8ebea1ec8864f87)
  records removing depth dependence after author-reported SMP non-regression tests
  (437904/484778 games). It is not evidence that depth can never help arbitration.
- Ethereal [worker PV selection](https://github.com/AndyGrant/Ethereal/commit/cc12382180405247d5afacfeafedea78019e0a6d)
  reports gains at 8-thread 5+0.05 (33040 games, +2.31 +/-1.86 Elo) and 16-thread
  short/long tests. These are author reports in an older surrounding engine, not
  independently reproduced evidence or W4 Seed measurements.
- Ethereal [early qsearch static storage](https://github.com/AndyGrant/Ethereal/commit/df3fa74c9dd33e715bb4a8d4f674268fcd195d4a)
  reports replacing its evaluation cache and W1/W8 tests; the W8 reported estimate
  is +2.28 +/-2.16 Elo. This illustrates information-consumer coupling, not a mandate
  to add qTT to this campaign.
- Bagatur's pinned [SMP scaling note](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/SMP.scaling.issue.txt)
  reports high-thread shared-TT scaling difficulties and an NPS/strength tradeoff
  when reducing shallow TT access. Its cache/Java diagnosis is an author hypothesis;
  its large-VM traces are not independent causal proof or a Ryzen W4 prediction.

No external benchmark was executed here. Retrieved history is a targeted sample,
not an exhaustive evolution audit. Broken Ethereal tt.c/tt.h URLs were retained as
retrieval failures; correct transposition.c/.h bytes were obtained. The Bagatur web
page fetch failed, but pinned GitHub raw/API retrieval succeeded and is hashed.

## Licensing boundary

Inspected Stockfish and Ethereal source headers/license files specify GPLv3-or-later.
Code incorporation would require preserving applicable notices/license and satisfying
corresponding-source and other distribution obligations for the resulting work;
do not assume repository/source availability alone resolves compatibility.
Bagatur root LICENSE is EPL2.0 while the inspected Search module LICENSE and parallel
source headers specify EPL1.0. Preserve that per-file distinction and resolve the
applicable terms/compatibility before incorporating its code. No top-level Seed
license was found in the scoped tracked-file check. No external code was incorporated:
the new probe uses Seed's existing APIs and the supervisor reuses Seed-owned tools.
The next algorithm experiments can be independently implemented from hypotheses;
they are not authorized to strip notices or silently transplant external expression.

## Evidence-ranked next selection

1. Run the prepared bounded TC-P1 W4/W2 scheduling/admission probe after PC pressure
   clears. It can quantify serialized first-job work and per-worker imbalance; it
   cannot establish exact idle/tail duration, contention cost, unique useful coverage
   or information consumption. Initialized job counts include scout retries.
2. Compare an exact-TC-P1 fully drained whole-root formulation against root splitting
   as an economical diagnostic, if the measured question warrants it. Label a
   same-depth race honestly; it is not independent iterative Lazy SMP. Retain the
   stronger competing request-level Lazy SMP design even if this cheap test loses.
3. Design true independent-worker coordination with completed-result provenance,
   final accounting after drain, single TT generation, private learning and shared
   control. Choose owner-result versus completed-worker arbitration as explicit
   alternatives. Do not let interface convenience prescribe the final architecture.
4. Follow observed information limitations into bounded TT or history integration
   and reverse ablations. F3/H1/H2 are optional dependencies with prior qualifications
   and adverse results, not automatically selected. Measure actual consumption and
   avoid simultaneously changing every subsystem before a causal comparison.

These are revisable research decisions within MW-R001, not an owner approval gate
or a claim that any mechanism will improve strength. Before competitive development,
protect a fresh opening partition, qualify the fixed-node runner and account both
engines' admitted work. No competitive runner has yet been admitted for MW-R001.
Confirmation sample/clock remains unfrozen: for illustration, at480 pairs a pair
SD of0.286 gives a normal half-width about2.56 percentage points, while worst-case
SD0.5 gives4.47. Actual variance and throughput must drive planning; the reserve
does not guarantee adequate power at a true3-point advantage.
