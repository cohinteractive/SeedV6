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

/**
 * Owner-confined fixed-depth SMP coordinator. Workers search the same nominal
 * depth with private board, evaluator and history stacks, sharing only coherent
 * TT evidence and ordering-only move reservations. The first complete root
 * wins; all helpers drain before publication or reuse. No cross-depth voting.
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

    public ParallelSearch(int threads) { this(threads, SearchEvaluation.handcrafted()); }

    public ParallelSearch(int threads, SearchEvaluation definition) {
        this(threads, definition, new TTable());
    }

    /** The supplied table is exclusively owned by this coordinator. */
    ParallelSearch(int threads, SearchEvaluation definition, TTable table) {
        if(threads < MIN_WORKERS || threads > MAX_WORKERS)
            throw new IllegalArgumentException("Threads must be in 1..16: " + threads);
        Objects.requireNonNull(definition, "evaluation");
        this.table = Objects.requireNonNull(table, "table");
        reservations = threads == 1 ? null : new MoveReservations();
        workers = new Worker[threads];
        for(int i = 0; i < threads; i++) workers[i] = new Worker(definition);
        futures = new Future<?>[threads - 1];
        pool = threads == 1 ? null : Executors.newFixedThreadPool(threads - 1, action -> {
            var thread = new Thread(action, "seedv6-search-helper");
            thread.setDaemon(true);
            return thread;
        });
    }

    private final class Worker implements Runnable {
        final ExactSearch search;
        long nodes;
        long evaluations;
        int maximumPly;
        boolean used;

        Worker(SearchEvaluation definition) {
            // A definition is immutable; its mutable evaluator State is never shared.
            var evaluator = ExactEvaluator.from(definition);
            var observed = new ExactEvaluator() {
                @Override public void initialize(long[] board) { evaluator.initialize(board); }
                @Override public int evaluate(long[] board, int ply) {
                    evaluations++;
                    return evaluator.evaluate(board, ply);
                }
                @Override public void child(long[] parent, long[] child, int parentPly) {
                    // Exactly the ordinary adapter's prepared-transition accounting,
                    // including synthetic probes, against one global request budget.
                    if(!control.tryEnterNode()) return;
                    nodes++;
                    maximumPly = Math.max(maximumPly, parentPly + 1);
                    evaluator.child(parent, child, parentPly);
                }
            };
            search = ExactSearch.sharedWorker(observed, table, reservations,
                    definition == SearchEvaluation.handcrafted());
        }

        @Override public void run() {
            used = true;
            participating.incrementAndGet();
            try {
                var result = search.search(root, request.gameHistory(), request.depth(),
                        ParallelSearch.this::cancelled);
                if(result.completed() && winner.compareAndSet(null, result)) abort.set(true);
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
        Objects.requireNonNull(next, "request");
        ensureIdle();
        if(next.depth() > maxSupportedDepth()) throw new IllegalArgumentException("Unsupported depth.");
        boolean standalone = !requestActive;
        if(standalone) beginRequest();
        active = true;
        request = next;
        control = next.control();
        abort.set(false);
        winner.set(null);
        failure.set(null);
        for(var worker : workers) {
            worker.nodes = worker.evaluations = 0;
            worker.maximumPly = 0;
            worker.used = false;
        }
        long started = System.nanoTime();
        try {
            next.copyBoardInto(root);
            int count = Gen.genAll(root[0], root[1], root[2], root[3], (int) root[Board.STATUS],
                    root[Board.KEY], true, rootMoves, scratch);
            try {
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

            long nodes = 0, evaluations = 0;
            int maximumPly = 0;
            boolean selective = false;
            for(var worker : workers) {
                nodes += worker.nodes;
                evaluations += worker.evaluations;
                maximumPly = Math.max(maximumPly, worker.maximumPly);
                // Conservatively include predictions in aborted helpers as work
                // of this invocation; they are never promoted to TT proofs.
                if(worker.used) selective |= worker.search.anySelectiveWork();
            }
            var diagnostics = diagnostics(next.diagnosticsEnabled(), nodes, evaluations, maximumPly);
            var completed = winner.get();
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
            long evaluations, int maximumPly) {
        if(!enabled) return SearchDiagnosticsSnapshot.disabled();
        var empty = SearchDiagnosticsSnapshot.enabledEmpty();
        var worker = empty.worker();
        return new SearchDiagnosticsSnapshot(true, new WorkerMetrics(
                new NodeMetrics(nodes, 0, maximumPly, 0, evaluations), worker.transpositionTable(),
                worker.moveOrder(), worker.qsearch(), worker.selective()), empty.iteration());
    }

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
