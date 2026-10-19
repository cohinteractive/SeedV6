package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

/**
 * Owner-confined SMP coordinator. The inherited default races same-depth roots
 * with private board, evaluator and history stacks, coherent shared TT evidence
 * and advisory move reservations. Explicit research policies add cumulative
 * workers, owner publication, offset helpers or complete root-job cohorts.
 * All helpers drain before publication or reuse; there is no cross-depth voting.
 *
 * The owner is one of the requested workers. Depths below three and roots with
 * at most one legal move use only the owner. Ordinary Threads=1 construction
 * continues to use ExactSearchAdapter, the independent single-worker baseline.
 */
public final class ParallelSearch implements SingleDepthSearch {
    public static final int MIN_WORKERS = 1;
    public static final int MAX_WORKERS = 16;
    public static final int DEFAULT_WORKERS = 1;

    private final Worker[] workers;
    private final FlatSearchConfig construction;
    private final ConcurrencyPolicy policy;
    private final java.util.function.IntFunction<ExactEvaluator> evaluatorFactory;
    private final TTable table;
    private final MoveReservations reservations;
    private final ExecutorService pool;
    private final Future<?>[] futures;
    private final AtomicBoolean abort = new AtomicBoolean();
    private final AtomicInteger participating = new AtomicInteger();
    private final AtomicReference<ExactSearchResult> winner = new AtomicReference<>();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final long[] root = new long[Board.MAX_BITBOARDS];
    private final long[] scratch = new long[Board.MAX_BITBOARDS];
    private final long[] rootMoves = new long[512];
    private SearchRequest request;
    private SearchControl control;
    private int generation;
    private boolean requestActive;
    private boolean active;
    private boolean closed;
    private boolean splitActive;
    private int rootAlpha, rootBeta;
    private long[] splitMoves = new long[0], previousMoves = new long[0], previousEffort = new long[0];
    private long[] splitEffort = new long[0];
    private long preferred;
    private final AtomicInteger nextRoot = new AtomicInteger();
    private final AtomicReference<ExactSearchResult> splitBest = new AtomicReference<>();
    private final AtomicBoolean splitIncomplete = new AtomicBoolean();
    private final IterationReuse rootOrdering;

    /** Configure all idle ROOT_REUSE workers with separate optional diagnostic sinks. */
    public ParallelSearch withSeeResearch(SeePolicy seePolicy,
            java.util.function.IntFunction<SeePolicy.Diagnostics> diagnostics) {
        ensureIdle();
        if(requestActive || policy != ConcurrencyPolicy.ROOT_REUSE)
            throw new IllegalStateException("SEE research requires idle ROOT_REUSE");
        for(var worker : workers) worker.search.withSeeResearch(seePolicy,
                diagnostics == null ? null : diagnostics.apply(worker.index));
        return this;
    }

    public ParallelSearch withFutilityResearch(FutilityPolicy futilityPolicy,
            java.util.function.IntFunction<FutilityPolicy.Diagnostics> diagnostics) {
        ensureIdle();
        if(requestActive || policy != ConcurrencyPolicy.ROOT_REUSE)
            throw new IllegalStateException("Futility research requires idle ROOT_REUSE");
        for(var worker : workers) worker.search.withFutilityResearch(futilityPolicy,
                diagnostics == null ? null : diagnostics.apply(worker.index));
        return this;
    }

    /** Same coherent TT, private counters, and unchanged ROOT_REUSE scheduling. */
    public ParallelSearch withFlatQuiescenceResearch(boolean enabled) {
        ensureIdle();
        if(requestActive || policy!=ConcurrencyPolicy.ROOT_REUSE)
            throw new IllegalStateException("Flat qsearch requires idle ROOT_REUSE");
        for(var worker:workers) worker.search.withFlatQuiescenceResearch(enabled);
        return this;
    }

    public ParallelSearch withFlatSearchResearch(boolean enabled) {
        ensureIdle();
        if(requestActive || policy!=ConcurrencyPolicy.ROOT_REUSE)
            throw new IllegalStateException("Flat Search requires idle ROOT_REUSE");
        for(var worker:workers) worker.search.withFlatSearchResearch(enabled);
        return this;
    }

    public ParallelSearch withRepeatedRootPassResearch() {
        ensureIdle();
        if(requestActive || policy!=ConcurrencyPolicy.ROOT_REUSE)
            throw new IllegalStateException("Repeated probes require idle ROOT_REUSE");
        for(var worker:workers) worker.search.withRepeatedRootPassResearch();
        return this;
    }

    /** Same coherent TT, private counters, and unchanged ROOT_REUSE scheduling. */
    public ParallelSearch withCompletedHints(boolean enabled,
            java.util.function.IntFunction<MoveHintDiagnostics> diagnostics) {
        ensureIdle();
        if(requestActive || policy != ConcurrencyPolicy.ROOT_REUSE)
            throw new IllegalStateException("Completed hints require idle ROOT_REUSE");
        for(var worker:workers) worker.search.withCompletedHints(enabled,
                diagnostics==null?null:diagnostics.apply(worker.index));
        return this;
    }

    /** Each worker learns privately over its own request; no shared table or copy. */
    public ParallelSearch withDepthFeedback(DepthFeedback feedback,
            java.util.function.IntFunction<DepthFeedback.Diagnostics> diagnostics) {
        ensureIdle();
        if(requestActive || policy != ConcurrencyPolicy.ROOT_REUSE)
            throw new IllegalStateException("Depth feedback requires idle ROOT_REUSE");
        for(var worker : workers) worker.search.withDepthFeedback(feedback,
                diagnostics == null ? null : diagnostics.apply(worker.index));
        return this;
    }

    public ParallelSearch withTacticalExtension(TacticalExtension extension,
            java.util.function.IntFunction<TacticalExtensionDiagnostics> diagnostics) {
        ensureIdle();
        if(requestActive || policy != ConcurrencyPolicy.ROOT_REUSE)
            throw new IllegalStateException("Tactical extensions require idle ROOT_REUSE");
        for(var worker:workers)worker.search.withTacticalExtension(extension,
                diagnostics==null?null:diagnostics.apply(worker.index));
        return this;
    }

    public ParallelSearch(int threads) { this(threads, SearchEvaluation.handcrafted()); }

    public ParallelSearch(int threads, SearchEvaluation definition) {
        this(threads, definition, new TTable());
    }

    /** The supplied table is exclusively owned by this coordinator. */
    ParallelSearch(int threads, SearchEvaluation definition, TTable table) {
        this(threads, definition, table, ConcurrencyPolicy.OFF);
    }

    /** Explicit research; production constructors keep inherited static-leaf behavior. */
    public ParallelSearch(int threads, SearchEvaluation definition, ConcurrencyPolicy policy) {
        this(threads, definition, new TTable(), policy);
    }

    /** The supplied table is exclusively owned by this coordinator and shared by its private workers. */
    public ParallelSearch(int threads, SearchEvaluation definition, TTable table, ConcurrencyPolicy policy) {
        this(threads, definition, table, policy, null);
    }

    /** Package-local validation seam; each index must receive private mutable state. */
    ParallelSearch(int threads, SearchEvaluation definition, TTable table, ConcurrencyPolicy policy,
            java.util.function.IntFunction<ExactEvaluator> evaluatorFactory) {
        this(threads,definition,table,policy,evaluatorFactory,null);
    }

    public static ParallelSearch constructionResearch(int threads,SearchEvaluation evaluation,
            TTable table,FlatSearchConfig config) {
        Objects.requireNonNull(config,"config");
        return new ParallelSearch(threads,evaluation,table,
                config.has(FlatSearchConfig.Feature.EFFORT)?ConcurrencyPolicy.ROOT_REUSE:ConcurrencyPolicy.ROOT_SPLIT,
                null,config);
    }

    private ParallelSearch(int threads,SearchEvaluation definition,TTable table,ConcurrencyPolicy policy,
            java.util.function.IntFunction<ExactEvaluator> evaluatorFactory,FlatSearchConfig construction) {
        this.construction=construction;
        if(threads < MIN_WORKERS || threads > MAX_WORKERS)
            throw new IllegalArgumentException("Threads must be in 1..16: " + threads);
        Objects.requireNonNull(definition, "evaluation");
        this.policy = Objects.requireNonNull(policy, "policy");
        rootOrdering = policy == ConcurrencyPolicy.ROOT_REUSE ? new IterationReuse(IterationReuse.EFFORT,null) : null;
        this.evaluatorFactory = evaluatorFactory;
        this.table = Objects.requireNonNull(table, "table");
        reservations = threads == 1 || !policy.reservations() ? null : new MoveReservations();
        workers = new Worker[threads];
        for(int i = 0; i < threads; i++) workers[i] = new Worker(definition, i);
        futures = new Future<?>[threads - 1];
        pool = threads == 1 ? null : Executors.newFixedThreadPool(threads - 1, action -> {
            var thread = new Thread(action, "seedv6-search-helper");
            thread.setDaemon(true);
            return thread;
        });
    }

    private final class Worker implements Runnable {
        final ExactSearch search;
        final int index;
        long nodes;
        long evaluations;
        long qnodes;
        int maximumQply;
        ExactSearchResult completed;
        long abortedNodes;
        boolean tainted;
        int maximumPly;
        boolean used;

        Worker(SearchEvaluation definition, int index) {
            this.index = index;
            // A definition is immutable; its mutable evaluator State is never shared.
            var evaluator = evaluatorFactory == null ? ExactEvaluator.from(definition) : evaluatorFactory.apply(index);
            var observed = new ExactEvaluator() {
                @Override public void initialize(long[] board) { evaluator.initialize(board); }
                @Override public int evaluate(long[] board, int ply) {
                    evaluations++;
                    return evaluator.evaluate(board, ply);
                }
                @Override public void child(long[] parent, long[] child, int parentPly) {
                    child(parent, child, parentPly, -1);
                }
                @Override public void child(long[] parent, long[] child, int parentPly, int qply) {
                    // Exactly the ordinary adapter's prepared-transition accounting,
                    // including synthetic probes, against one global request budget.
                    if(!control.tryEnterNode()) return;
                    nodes++;
                    if(qply >= 0) { qnodes++; maximumQply = Math.max(maximumQply, qply); }
                    maximumPly = Math.max(maximumPly, parentPly + 1);
                    evaluator.child(parent, child, parentPly, qply);
                }
            };
            search = construction!=null ? ExactSearch.constructionResearch(observed,table,construction)
                    : policy == ConcurrencyPolicy.OFF
                    ? ExactSearch.sharedWorker(observed, table, reservations, definition == SearchEvaluation.handcrafted())
                    : ExactSearch.sharedCumulativeWorker(observed, table, reservations,
                            policy != ConcurrencyPolicy.MIXED || index == 0);
        }

        @Override public void run() {
            used = true;
            participating.incrementAndGet();
            try {
                if(splitActive) { rootJobs(); return; }
                int depth = policy == ConcurrencyPolicy.OFFSET && index > 0
                        ? Math.min(maxSupportedDepth(), request.depth() + 1) : request.depth();
                var result = search.searchWindow(root, request.gameHistory(), depth, rootAlpha, rootBeta,
                        ParallelSearch.this::cancelled);
                tainted |= search.anySelectiveWork();
                if(!result.completed()) abortedNodes += nodes;
                if(result.completed()) {
                    completed = result;
                    if((!policy.ownerOnly() || index == 0) && winner.compareAndSet(null, result)) abort.set(true);
                }
                if(Thread.currentThread().isInterrupted()) {
                    abort.set(true);
                    control.request(SearchTermination.STOPPED);
                }
            } catch(Throwable problem) {
                fail(problem);
            } finally {
                participating.decrementAndGet();
            }
        }

        private ExactSearchResult rootJob(int job, int alpha, int beta, boolean scout) {
            long before=nodes;
            var result=search.searchRootMove(root,request.gameHistory(),request.depth(),splitMoves[job],
                    alpha,beta,scout,ParallelSearch.this::cancelled);
            splitEffort[job]+=nodes-before;
            tainted |= search.anySelectiveWork();
            if(!result.completed()) { abortedNodes+=nodes-before; splitIncomplete.set(true); }
            return result;
        }

        private void rootJobs() {
            for(int job; !cancelled() && (job=nextRoot.getAndIncrement())<splitMoves.length;) {
                if(splitBest.get().score()>=rootBeta) return;
                int originalAlpha=policy==ConcurrencyPolicy.ROOT_FULL ? rootAlpha : Math.max(rootAlpha,splitBest.get().score());
                var result=rootJob(job,originalAlpha,policy==ConcurrencyPolicy.ROOT_FULL ? rootBeta : originalAlpha+1,
                        policy!=ConcurrencyPolicy.ROOT_FULL);
                if(!result.completed()) return;
                if(policy!=ConcurrencyPolicy.ROOT_FULL && result.score()>originalAlpha && result.score()<rootBeta) {
                    // The old scout lower bound cannot be compared with a newer alpha
                    // to skip its full retry. Re-search every original-window fail-high.
                    int retryAlpha=Math.max(rootAlpha,splitBest.get().score());
                    if(retryAlpha>=rootBeta) return; // Another completed job already proves this root cutoff.
                    result=rootJob(job,retryAlpha,rootBeta,false);
                    if(!result.completed()) return;
                }
                completed=result;
                for(;;) {
                    var before=splitBest.get();
                    if(result.score()<=before.score() || splitBest.compareAndSet(before,result)) break;
                }
            }
        }
    }

    @Override public int maxSupportedDepth() { return ExactSearch.MAX_DEPTH; }

    /** Owner and helpers currently inside Worker.run; queued helpers and a draining owner are excluded. */
    @Override public int activeSearchThreads() { return participating.get(); }

    @Override public void beginRequest() {
        ensureIdle();
        if(requestActive) throw new IllegalStateException("Parallel Search request already active.");
        // Workers never advance or clear the shared table. Executor submission
        // publishes this generation; joins precede its next mutation, including wrap.
        table.advanceGeneration();
        generation = (generation + 1) & 255;
        for(var worker : workers) worker.search.beginSharedRequest(generation);
        preferred=0; previousMoves=previousEffort=new long[0];
        if(rootOrdering != null) rootOrdering.clear();
        requestActive = true;
    }

    @Override public void endRequest() {
        ensureIdle();
        if(!requestActive) throw new IllegalStateException("No Parallel Search request.");
        for(var worker : workers) worker.search.endRequest();
        requestActive = false;
        assert reservations == null || reservations.isEmpty() : "Undrained move reservation";
    }

    @Override public void newGame() {
        ensureIdle();
        if(requestActive) throw new IllegalStateException("Parallel Search request active.");
        table.clear();
    }

    private void ensureIdle() {
        if(closed) throw new IllegalStateException("Parallel Search is closed.");
        if(active) throw new IllegalStateException("Parallel Search cannot be re-entered.");
    }

    private boolean cancelled() { return abort.get() || !control.checkpoint(); }

    private void fail(Throwable problem) {
        failure.compareAndSet(null, problem);
        abort.set(true);
        control.fail();
    }

    @Override public SearchResult search(SearchRequest next) {
        return searchWindow(next,-ExactSearch.INFINITY,ExactSearch.INFINITY);
    }

    /** Invocation-only research seam. Narrow completion is a bound, never a completed iteration.
     * ROOT_REUSE retains private seeding and drains every pass; restricted jobs never store root proof. */
    public SearchResult searchWindow(SearchRequest next,int alpha,int beta) {
        Objects.requireNonNull(next, "request");
        ensureIdle();
        if(alpha < -ExactSearch.INFINITY || beta > ExactSearch.INFINITY || alpha>=beta)
            throw new IllegalArgumentException("Invalid root window");
        if((alpha!=-ExactSearch.INFINITY || beta!=ExactSearch.INFINITY) && policy!=ConcurrencyPolicy.ROOT_REUSE
                && !(construction!=null && policy==ConcurrencyPolicy.ROOT_SPLIT))
            throw new IllegalStateException("Narrow parallel research requires ROOT_REUSE");
        if(next.depth() > maxSupportedDepth()) throw new IllegalArgumentException("Unsupported depth.");
        boolean standalone = !requestActive;
        if(standalone) beginRequest();
        active = true;
        rootAlpha=alpha;rootBeta=beta;
        request = next;
        control = next.control();
        abort.set(false);
        winner.set(null);
        failure.set(null);
        for(var worker : workers) {
            worker.nodes = worker.evaluations = worker.qnodes = worker.abortedNodes = 0;
            worker.maximumPly = worker.maximumQply = 0;
            worker.completed = null;
            worker.tainted = false;
            worker.used = false;
        }
        long started = System.nanoTime();
        try {
            next.copyBoardInto(root);
            int count = Gen.genAll(root[0], root[1], root[2], root[3], (int) root[Board.STATUS],
                    root[Board.KEY], true, rootMoves, scratch);
            splitActive=policy.rootSplit() && next.depth()>=3 && count>1
                    && DrawAdjudicator.adjudicateNonTerminal(root,new SearchLineHistory(next.gameHistory()))==DrawAdjudicator.RuleDraw.NONE;
            try {
                if(splitActive) {
                    splitMoves=workers[0].search.orderedRootMoves(root,preferred);
                    // Completed previous root effort orders alternatives; no pending job
                    // or scout score supplies ordering proof or changes the preferred PV.
                    for(int i=1;i<splitMoves.length;i++) {
                        long move=splitMoves[i],cost=priorEffort(move);int j=i;
                        while(j>0 && splitMoves[j-1]!=preferred && priorEffort(splitMoves[j-1])<cost) {
                            splitMoves[j]=splitMoves[j-1];j--;
                        }
                        splitMoves[j]=move;
                    }
                    splitEffort=new long[splitMoves.length];nextRoot.set(1);splitIncomplete.set(false);
                    if(rootOrdering != null) for(var worker:workers) worker.search.prepareRootIteration(rootOrdering);
                    workers[0].used=true;participating.incrementAndGet();
                    ExactSearchResult first;
                    try { first=workers[0].rootJob(0,rootAlpha,rootBeta,false); }
                    finally { participating.decrementAndGet(); }
                    splitBest.set(first);
                    if(!first.completed()) abort.set(true);
                    else if(rootOrdering != null) for(var worker:workers) worker.search.seedRootHistoryFrom(workers[0].search);
                }
                if(pool != null && next.depth() >= 3 && count > 1) {
                    for(int i = 0; i < futures.length; i++) futures[i] = pool.submit(workers[i + 1]);
                }
                workers[0].run();
            } catch(Throwable problem) {
                fail(problem);
            }
            drain();
            Throwable problem = failure.get();
            if(problem instanceof Error error) throw error;
            if(problem instanceof RuntimeException runtime) throw runtime;
            if(problem != null) throw new IllegalStateException("Parallel Search failed.", problem);
            if(splitActive && !cancelled() && (splitBest.get().score()>=rootBeta
                    || (!splitIncomplete.get() && nextRoot.get()>=splitMoves.length))) {
                winner.set(splitBest.get());
                if((alpha==-ExactSearch.INFINITY && beta==ExactSearch.INFINITY
                        || (construction!=null && splitBest.get().score()>alpha && splitBest.get().score()<beta
                            && !com.ohinteractive.seedv6.search.tt.TranspositionScores.isMateScore(splitBest.get().score())))
                        && (construction==null || construction.has(FlatSearchConfig.Feature.EFFORT))) {
                    previousMoves=splitMoves.clone();previousEffort=splitEffort.clone();
                }
            }

            long nodes = 0, evaluations = 0, qnodes = 0;
            int maximumPly = 0, maximumQply = 0;
            boolean selective = false;
            for(var worker : workers) {
                nodes += worker.nodes;
                evaluations += worker.evaluations;
                qnodes += worker.qnodes;
                maximumQply = Math.max(maximumQply, worker.maximumQply);
                maximumPly = Math.max(maximumPly, worker.maximumPly);
                // Conservatively include predictions in aborted helpers as work
                // of this invocation; they are never promoted to TT proofs.
                if(worker.used) selective |= worker.tainted;
            }
            var diagnostics = diagnostics(next.diagnosticsEnabled(), nodes, evaluations, maximumPly, qnodes, maximumQply);
            var completed = winner.get();
            boolean fullWindow=alpha==-ExactSearch.INFINITY && beta==ExactSearch.INFINITY;
            boolean resolved=fullWindow || (construction!=null && completed!=null && completed.score()>alpha
                    && completed.score()<beta && !com.ohinteractive.seedv6.search.tt.TranspositionScores.isMateScore(completed.score()));
            if(completed!=null && completed.hasMove() && resolved
                    && (construction==null || construction.has(FlatSearchConfig.Feature.EFFORT))) preferred=completed.bestMove();
            if(completed != null && rootOrdering != null && resolved) {
                rootOrdering.prepare(root,next.gameHistory(),next.depth(),true,-ExactSearch.INFINITY,ExactSearch.INFINITY);
                if(splitActive) for(int i=0;i<splitMoves.length;i++) rootOrdering.rootResult(splitMoves[i],0,splitEffort[i]);
                long[] line=completed.principalVariation();
                rootOrdering.complete(next.depth(),line,line.length);
            }
            if(completed != null && policy.transfer()) {
                Worker source = null;
                for(var worker : workers) if(worker.completed == completed) source = worker;
                if(source == null) throw new IllegalStateException("No completed winner owner");
                for(var worker : workers) worker.search.copyCompletedOrderingFrom(source.search);
            }
            var result = completed == null
                    ? new SearchResult(0, false, Value.INVALID, next.depth(), nodes, count, false,
                            new long[0], diagnostics, selective)
                    : new SearchResult(completed.bestMove(), completed.hasMove(), completed.score(),
                            next.depth(), nodes, count, true, completed.principalVariation(), diagnostics, selective);
            next.observer().onSearchFinished(result, System.nanoTime() - started);
            return result;
        } finally {
            request = null;
            control = null;
            active = false;
            if(standalone) endRequest();
        }
    }

    private long priorEffort(long move) {
        for(int i=0;i<previousMoves.length;i++) if(previousMoves[i]==move) return previousEffort[i];
        return 0;
    }

    private void drain() {
        boolean interrupted = false;
        for(int i = 0; i < futures.length; i++) {
            var future = futures[i];
            if(future == null) continue;
            for(;;) {
                try {
                    future.get();
                    break;
                } catch(InterruptedException problem) {
                    interrupted = true;
                    abort.set(true);
                    control.request(SearchTermination.STOPPED);
                } catch(ExecutionException problem) {
                    fail(problem.getCause());
                    break;
                }
            }
            futures[i] = null;
        }
        if(interrupted) Thread.currentThread().interrupt();
    }

    private static SearchDiagnosticsSnapshot diagnostics(boolean enabled, long nodes,
            long evaluations, int maximumPly, long qnodes, int maximumQply) {
        if(!enabled) return SearchDiagnosticsSnapshot.disabled();
        var empty = SearchDiagnosticsSnapshot.enabledEmpty();
        var worker = empty.worker();
        return new SearchDiagnosticsSnapshot(true, new WorkerMetrics(
                new NodeMetrics(nodes - qnodes, qnodes, maximumPly, maximumQply, evaluations), worker.transpositionTable(),
                worker.moveOrder(), worker.qsearch(), worker.selective()), empty.iteration());
    }

    /** Quiescent diagnostic snapshot. Losing work is included in global admissions. */
    public WorkerWork[] workerWork() {
        ensureIdle();
        var result = new WorkerWork[workers.length];
        for(int i=0;i<workers.length;i++) {
            var w=workers[i];
            result[i]=new WorkerWork(i,w.used,w.nodes,w.qnodes,w.evaluations,w.completed != null,
                    w.completed != null && w.completed == winner.get(), w.abortedNodes);
        }
        return result;
    }
    public record WorkerWork(int worker, boolean used, long nodes, long qnodes, long evaluations,
            boolean completed, boolean winner, long abortedNodes) {}

    @Override public void close() {
        if(closed) return;
        ensureIdle();
        if(requestActive) throw new IllegalStateException("Parallel Search request active.");
        closed = true;
        if(pool == null) return;
        pool.shutdown();
        boolean interrupted = false;
        for(;;) {
            try {
                if(pool.awaitTermination(1, TimeUnit.SECONDS)) break;
            } catch(InterruptedException problem) {
                interrupted = true;
            }
        }
        if(interrupted) Thread.currentThread().interrupt();
    }
}
