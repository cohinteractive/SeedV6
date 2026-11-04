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

1. Baseline scheduling probe is qualified (see STATE/results). It quantifies
   first-job admissions and per-worker imbalance; it
   cannot establish exact idle/tail duration, contention cost, unique useful coverage
   or information consumption. Initialized job counts include scout retries.
2. MW-C1/build92 implements the controlled fully drained whole-root comparison.
   Its diagnostic completed about one ply less at equal admissions. Label a
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
engines' admitted work. The research-only runner is now qualified by fixed/timed pilots (see
competitive-qualification-v1-summary.json); the frozen development screens follow.
Confirmation sample/clock remains unfrozen: for illustration, at480 pairs a pair
SD of0.286 gives a normal half-width about2.56 percentage points, while worst-case
SD0.5 gives4.47. Actual variance and throughput must drive planning; the reserve
does not guarantee adequate power at a true3-point advantage.

## Overnight follow-up: cancellation and learning

Source inspection at bfd11801 identifies a supporting-condition hypothesis for C1.
ExactSearch.searchWindow catches Aborted by clearing iterationReuse, quietHistory
and Construction learned tables through constructionState.reset(false). C1 cancels
peers at every winning window, including failed aspiration windows. Therefore a
losing worker cannot carry its completed-adjacent iteration guidance or request
learning through that cancellation. ROOT_REUSE normally cancels on actual request
stop, while its completed root jobs retain private request learning. This is a
verified source behavior, not a measured causal fraction of C1's depth deficit.
Shared completed TT hints remain available; aborted work is not wholly useless.

Two distinct next hypotheses remain: (a) quiescent copying of the completed winner's
ordering/learning into private worker copies can restore continuity at the cost of
diversity; (b) request-level independent iterative workers avoid the per-window
barrier and keep their own completed progress. Neither is presumed stronger. The
first can be isolated economically; the second needs explicit final drain/accounting,
honest completed-result selection and qualification of owner BOUNDARY2 coordination.
The existing policy thresholds and hard budgets must remain unchanged. Ordinary
SearchDriver cannot safely host asynchronous work merely via begin/endRequest.
MW-C2 draft now selects (a): copy the resolved winner's completed ordering and
learned quiet/capture/continuation/killer/counter tables into private peers. It never
copies path state, proof, correction history or bound-only/aborted root results.
Test and runtime qualification remain pending until the timed screen has drained.
This is a supporting-condition experiment; independent iterative coordination
remains a competing architecture, not rejected by the C1 result.

Further pinned-source inspection sharpens the transfer boundary: Ethereal's
update_best_line may retain a fail-high PV at its nominal completed depth and
revert_best_line rolls it back after a fail-low. Stockfish's selection explicitly
handles inexact decisive root scores and restores previous mate evidence after
aborted loss searches. Those surrounding safeguards cannot be replaced by a bare
highest-score vote under Seed's completed-exact-result contract. Any request-level
candidate will first retain only resolved per-worker completed snapshots and use
owner-result selection as a separately identified initial control; arbitration is
an additional testable mechanism, not an assumed part of Lazy SMP.

C2 independently uses Seed-owned code and semantics; it does not copy external
source. Its quiescent full-array copies differ from Stockfish's concurrent NUMA
shared histories and Ethereal's private retained histories. At W4 the learned
arrays total4,454,448 copied bytes per resolved window across3 recipients (quiet,
capture, continuation, killer and counter tables; excludes small ordering copy).
This is a source-size calculation, not measured bandwidth/latency. New diagnostic
board-key coverage counts admitted callback Zobrist-key overlap, including qsearch
and synthetic transitions, without equating keys to history/depth/window states
or labelling overlap wasted work. It deliberately perturbs scheduling and will
not be used as a competitive-speed measurement.

The frozen prior diagnostic's post-hoc aspiration analysis is retained in
architecture-aspiration-v1-summary.json. Completed bound-pass work is8.68%/13.60%
(ROOT_REUSE/C1) on opening roots and16.79%/17.61% on mixed roots. This does not
isolate causation and does not by itself explain the roughly one-ply deficit.

Request-level implementation sketch (not yet implemented/qualified): preserve the
existing SingleDepthSearch path and add an explicit whole-request facility to
SearchDriver/SearchLifecycleService only for an opt-in factory. The coordinator
owns one shared TT generation and admission control, private ExactSearch stacks,
private aspiration/iteration loops, immutable completed snapshots, and final join
before aggregate counts/outcome construction. Begin with owner-result selection
to distinguish independent work from worker arbitration. Owner BOUNDARY2 thresholds
and allocation remain the existing implementation; qualify its completed-owner
iteration decisions with asynchronous helpers and final drain explicitly. Do not
relabel a helper depth or put a bound into the completed-result slot. Live progress
may sample the atomic admission counter; full per-worker diagnostics require
quiescent snapshots. Failure/replacement/new-game and no-completed-iteration paths
need managed-service qualification, beyond direct driver tests. This design is an
alternative to C2, not further unvalidated implementation during the timed screen.

A second near-term structural hypothesis follows the measured first-root barrier:
within a finite aspiration window, admit preferred-root full search and alternative
root scouts concurrently, using the already-declared alpha until a completed root
job improves it. Preserve every original-scout fail-high's full retry and require
completed coverage or a completed beta-cutoff witness before publication. Full-window
fallback could retain owner-first scheduling, avoiding the especially expensive
uninformed -infinity scouts. Existing per-root private state and drain discipline
can support this without a request-level interface change. It trades earlier helper
work against poorer early alpha and needs independent bound/rule/accounting tests.
This is an unimplemented competing hypothesis, not a claim that eager splitting
outperforms either whole-root or independent iterative Search.

MW-C3 now implements the finite-window hypothesis with an explicit opt-in factory.
The empty completed-result slot is null scheduling state, never an invented score
or PV. Only completed root results can raise alpha; an original scout fail-high
still receives its full retry even if another worker has meanwhile raised alpha.
All workers drain before publication; require complete root coverage or a completed
beta-cutoff witness. Quiescent preparation preserves TC-P1 worker-private learning.
The39-test qualification includes actual concurrent first-root entry; runtime and
playing strength remain unmeasured at this implementation boundary.

## Selected score-sharing dependency investigation

BaselineEngine.legacyConfiguration plus TC-P1 enables bits25/26/30 but leaves
HEURISTIC_TT_MEMORY22, AGED_HEURISTIC_TT_MEMORY23 and TT_INFORMATION24 disabled.
The existing H1 current-generation mechanism is therefore available as a new
integrated supporting-condition test, not already present in TC-P1. H1/H2/F3
historical results and spent acceptance attempts remain intact. The hypothesis is
that separate/eager root scheduling may benefit from richer reusable estimates
beyond completed hints, reducing work needed while helpers start with poorer alpha.
The same mechanism may also benefit ROOT_REUSE; use both to distinguish integration
from a general Search effect rather than presuming an interaction.

Selected diagnostic: ROOT_REUSE and EAGER_ROOT crossed with HINTS_ONLY,
SHARED_SCORES and PRIVATE_SCORES delivery. Both score modes add only existing
HEURISTIC_TT_MEMORY to exact TC-P1. No aged retention, raw caching, changed depth
threshold, score domain, synthetic-null permission or clock policy. PRIVATE_SCORES
retains shared storage/replacement and hints but withholds other-worker score
probes; it is not a worker-private table and may change subsequent ordering as
well as cutoff eligibility. Actual heuristicCutoff callbacks distinguish guarded
consumption from availability. Score producer snapshots survive later replacement;
independently retained move hints are not falsely attributed to that score writer.
A research-only global provenance monitor perturbs scheduling, so no NPS or timed
strength inference comes from this probe. New source/tests are pending validation
until the active frozen C3 competitive screen drains.

Include WHOLE_ROOT_RACE as the third schedule in this bounded diagnostic matrix: its
negative unchanged-C1 screens do not test whether missing score sharing is a material
supporting dependency. This adds only an existing factory to the probe, not another
large implementation. Complete all nine preselected schedule/delivery cells before
choosing any competitive integration. No unchanged-C1 sample extension or historic
confirmation is selected.

Further pinned-source detail: Stockfish search.cpp333-394 lets every worker advance
its own rootDepth; only main observes the requested-depth stop. Its aspiration width
also includes thread index and each root move has local score history; main history
is decayed at request startup instead of rebuilt from zero. Ethereal search.c205-235
likewise advances each helper independently and skips main time/report decisions.
Neither observation proves a Seed gain, and C1 same-depth synchronized races do not
reproduce those conditions. Stockfish also adjusts effective depth during aspiration
recovery, requiring care when comparing nominal completed depths across engines.
No external constants or code are imported. These source findings reinforce keeping
independent iteration/learning continuity distinct from first-finished cohort racing.

A narrower independent-iteration interface option is now identified (design only):
SearchLifecycleService uses SearchDriver through just search, maxSupportedDepth,
activeSearchThreads, lastCompletedResult, lastDiagnostics, newGame and close. A
small iterative-owner interface implemented by existing SearchDriver plus a separate
opt-in coordinator could preserve SingleDepthSearch semantics and all ordinary
constructors. A research service factory can inject that owner without teaching the
fixed-depth driver to return asynchronously or overloading endRequest accounting.
The coordinator would share one request generation/admission control, run private
aspiration loops, let only owner make unchanged BOUNDARY2 allocation decisions,
then locally stop/join helpers before final aggregate outcome. Normal owner depth
completion must not falsely mark the shared control STOPPED; helpers reaching their
own depth ceiling must not stop owner. Start with owner PV selection and retain
actual helper completed depths for later arbitration research. New-game, replacement,
failure and stale-publication tests must qualify the managed service seam if selected.
No interface or coordinator change is currently implemented.

SearchKey is another transfer constraint: it folds ordered reversible history and
complete Board.STATUS into the position key; pawn/capture edges reset that history
fingerprint. Consequently equal board Zobrist keys need not be reusable TT states,
including across separately assigned root paths. This supports the existing caution
that the board-key overlap metric is not useful transposition coverage. The selected
score probe keeps SearchKey unchanged and will measure actual eligible cross-worker
cutoffs. A future separately identified legal-move-only position hint domain could
be investigated if evidence warrants it, while retaining history-rich score keys
and exact controls; it is not implemented or presumed beneficial. No position-only
score fallback or rule-history weakening is authorized by this observation.

The complete nine-cell result supports selecting eager/shared-score integration
(MW-C4) with root/shared-score comparison (MW-C5), retaining exact TC-P1 control.
Whole-root score sharing changes actual consumption strongly but recovers only
part of its depth deficit; no competitive expansion selected. Both new bindings
use ordinary production TTable locking, with no diagnostic provenance monitor.
Flags1182007295 add only existing current-generation HEURISTIC_TT_MEMORY. Runtime
identity reports actual EAGER_ROOT or ROOT_REUSE scheduling separately from the
MW-C4/MW-C5 formulation and explicit TT policy; W1 uses the same OWNER algorithm.

## Independent iterative owner seam (MW-C6 implementation)

IndependentSearch now provides separate adjacent iterative/ADAPTIVE16 loops with
worker-private mutable boards, evaluators and request learning, one shared TT
generation and global admission control. Helpers never cancel peers after a window
or claim owner clock allocation. OWNER alone selects completed score/PV/depth and
applies unchanged BOUNDARY2; its end locally stops helpers and joins every future
before final callback totals, diagnostics, return/failure/reset. No external code
or constants were copied. There is no cross-depth vote or speculative result reuse.

The small IterativeSearch interface lets the existing lifecycle service inject this
explicit research owner; ordinary constructors still use unchanged SearchDriver.
The shared aspiration window seam preserves full verification/recovery. Live owner
iteration reports carry a global admission snapshot and explicitly disabled worker
diagnostics; final diagnostics include every drained worker. Helper completed
results and actual depths remain separately inspectable for arbitration research.

69 Java tests passed: independent-depth and owner-only clock witnesses, helper-tail
join, exact oracle/rules/PV, W2-W16 callback caps/failure/stop/reuse, interruption,
managed replacement/newGame/stale publication, plus existing lifecycle/driver/
aspiration/default identity regressions. See independent-tests-v2-summary.json.
The initial research probe parenthesis compile error is retained. Shared runtime
impact warrants build94->95; normal TC-P1 selection is unchanged. No new UCI
selector, diagnostic runtime or competitive C6 qualification yet. C4's frozen
build94 runtime remains the separate selected strength candidate.
