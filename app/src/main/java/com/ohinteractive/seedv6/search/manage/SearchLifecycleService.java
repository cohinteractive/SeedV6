package com.ohinteractive.seedv6.search.manage;

import java.util.Objects;
import java.util.function.Supplier;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.search.iterative.IterativeSearchOutcome;

/**
 * Single owner of managed search generations, cancellation, the reusable
 * worker-confined search instance, publication, stale suppression and shutdown.
 */
public final class SearchLifecycleService implements AutoCloseable {

    @FunctionalInterface
    public interface Listener {
        void onSearchComplete(ManagedSearchResult result);
    }

    public SearchLifecycleService() {
        this(TimeSource.SYSTEM, AlphaBetaPvsSearch::new);
    }

    public SearchLifecycleService(
        TimeSource timeSource, Supplier<? extends SingleDepthSearch> searchFactory
    ) {
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        search = new IterativeDeepeningSearch(
            Objects.requireNonNull(searchFactory, "searchFactory").get()
        );
        worker = new Thread(this::workerLoop, "seedv6-search-worker");
        worker.start();
    }

    public long start(
        long[] board, GameHistory history, SearchLimits limits, Listener listener
    ) {
        return start(board, history, limits, SearchObserver.NONE, listener);
    }

    public long start(
        long[] board, GameHistory history, SearchLimits limits,
        SearchObserver observer, Listener listener
    ) {
        return start(board, history, limits, observer, false, listener);
    }

    /** Starts one managed generation with optional worker-local diagnostics. */
    public long start(
        long[] board, GameHistory history, SearchLimits limits,
        SearchObserver observer, boolean diagnosticsEnabled, Listener listener
    ) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(listener, "listener");
        if(limits.depth() > search.maxSupportedDepth()) {
            throw new IllegalArgumentException("Unsupported search depth: " + limits.depth());
        }

        final long[] boardSnapshot = new long[Board.MAX_BITBOARDS];
        System.arraycopy(board, 0, boardSnapshot, 0, Board.MAX_BITBOARDS);
        history.requireCurrent(boardSnapshot);
        final GameHistory historySnapshot = history.snapshot();
        final long startNanos = timeSource.nanoTime();
        final long timeBudgetNanos = limits.timeMillis() == SearchLimits.NO_LIMIT
            ? SearchLimits.NO_LIMIT
            : TimeManager.millisToNanos(limits.timeMillis());
        final SearchControl control = SearchControl.controlled(
            limits.nodes(), startNanos, timeBudgetNanos, timeSource
        );

        synchronized(lock) {
            ensureOpen();
            generation ++;
            if(current != null) current.control.request(SearchTermination.REPLACED);
            if(pending != null) pending.control.request(SearchTermination.REPLACED);
            final SearchJob job = new SearchJob(
                generation, boardSnapshot, historySnapshot, limits, control,
                startNanos, observer, diagnosticsEnabled, listener
            );
            current = job;
            pending = job;
            lock.notifyAll();
            return generation;
        }
    }

    public void stop() {
        synchronized(lock) {
            if(current != null) current.control.request(SearchTermination.STOPPED);
        }
    }

    public void invalidate(SearchTermination reason) {
        if(reason != SearchTermination.POSITION_CHANGED && reason != SearchTermination.NEW_GAME) {
            throw new IllegalArgumentException("Unsupported invalidation reason: " + reason);
        }
        synchronized(lock) {
            generation ++;
            if(current != null) current.control.request(reason);
            if(pending != null) pending.control.request(reason);
            if(reason == SearchTermination.NEW_GAME) search.newGame();
            current = null;
            pending = null;
            lock.notifyAll();
        }
    }

    public boolean isSearching() {
        synchronized(lock) {
            return current != null;
        }
    }

    public long generation() {
        synchronized(lock) {
            return generation;
        }
    }

    public Throwable lastFailure() {
        return lastFailure;
    }

    public boolean isTerminated() {
        return !worker.isAlive();
    }

    @Override
    public void close() {
        synchronized(lock) {
            if(!shutdown) {
                shutdown = true;
                generation ++;
                if(current != null) current.control.request(SearchTermination.SHUTDOWN);
                if(pending != null) pending.control.request(SearchTermination.SHUTDOWN);
                current = null;
                pending = null;
                lock.notifyAll();
            }
        }
        joinWorker();
        if(worker.isAlive()) {
            worker.interrupt();
            joinWorker();
        }
    }

    private static final int MAX_MOVES = 256;
    private static final long JOIN_MILLIS = 2_000L;

    private final Object lock = new Object();
    private final TimeSource timeSource;
    private final IterativeDeepeningSearch search;
    private final Thread worker;
    private long generation;
    private SearchJob current;
    private SearchJob pending;
    private boolean shutdown;
    private volatile Throwable lastFailure;

    private void workerLoop() {
        while(true) {
            final SearchJob job;
            synchronized(lock) {
                while(pending == null && !shutdown) {
                    try {
                        lock.wait();
                    } catch(InterruptedException exception) {
                        if(shutdown) return;
                    }
                }
                if(shutdown) return;
                job = pending;
                pending = null;
            }

            final ManagedSearchResult result = execute(job);
            synchronized(lock) {
                if(current != job || shutdown) continue;
                ManagedSearchResult publication = result;
                final SearchTermination controlReason = job.control.termination();
                if(controlReason == SearchTermination.STOPPED
                    && publication.termination() == SearchTermination.COMPLETED) {
                    publication = publication.withTermination(SearchTermination.STOPPED);
                }
                current = null;
                try {
                    job.listener.onSearchComplete(publication);
                } catch(Throwable failure) {
                    lastFailure = failure;
                }
            }
        }
    }

    private ManagedSearchResult execute(SearchJob job) {
        final long[] rootMoves = new long[MAX_MOVES];
        final long[] scratch = new long[Board.MAX_BITBOARDS];
        final int rootMoveCount;
        try {
            rootMoveCount = Gen.genAll(
                job.board[0], job.board[1], job.board[2], job.board[3],
                (int) job.board[Board.STATUS], job.board[Board.KEY], true, rootMoves, scratch
            );
        } catch(Throwable failure) {
            return failure(
                job, 0L, false, null, failure,
                job.diagnosticsEnabled
                    ? SearchDiagnosticsSnapshot.enabledEmpty()
                    : SearchDiagnosticsSnapshot.disabled()
            );
        }

        final boolean hasFallback = rootMoveCount > 0;
        final long fallback = hasFallback ? rootMoves[0] : 0L;
        SearchResult lastCompleted = null;
        SearchDiagnosticsSnapshot diagnostics = job.diagnosticsEnabled
            ? SearchDiagnosticsSnapshot.enabledEmpty()
            : SearchDiagnosticsSnapshot.disabled();
        try {
            final int maximumDepth = job.limits.depth() == SearchLimits.NO_DEPTH
                ? search.maxSupportedDepth()
                : job.limits.depth();
            // A terminal root is exact independently of an already-expired go
            // budget. Its separate unlimited traversal has no child nodes but
            // retains the managed monotonic start for reporting.
            final SearchControl iterationControl = hasFallback ? job.control
                : SearchControl.controlled(
                    SearchLimits.NO_LIMIT, job.startNanos, SearchLimits.NO_LIMIT,
                    timeSource
                );
            final IterativeSearchOutcome outcome = search.search(
                new SearchRequest(
                    job.board, job.history, hasFallback ? maximumDepth : 1,
                    iterationObserver(job), iterationControl, job.diagnosticsEnabled
                )
            );
            lastCompleted = outcome.lastCompletedResult();
            diagnostics = outcome.diagnostics();
            if(outcome.targetDepthCompleted()) {
                return managed(
                    job, lastCompleted, SearchTermination.COMPLETED,
                    lastCompleted.bestMove(), lastCompleted.hasMove(), null, diagnostics
                );
            }

            if(job.control.termination() == SearchTermination.NONE) {
                try {
                    job.control.awaitTermination();
                } catch(InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    job.control.request(SearchTermination.SHUTDOWN);
                }
            }
            return interrupted(job, lastCompleted, fallback, true, null, diagnostics);
        } catch(Throwable failure) {
            if(lastCompleted == null) lastCompleted = search.lastCompletedResult();
            diagnostics = search.lastDiagnostics();
            return failure(job, fallback, hasFallback, lastCompleted, failure, diagnostics);
        }
    }

    private ManagedSearchResult interrupted(
        SearchJob job, SearchResult lastCompleted, long fallback, boolean hasFallback,
        Throwable failure, SearchDiagnosticsSnapshot diagnostics
    ) {
        final SearchTermination reason = job.control.termination() == SearchTermination.NONE
            ? SearchTermination.FAILURE
            : job.control.termination();
        final boolean useCompleted = lastCompleted != null && lastCompleted.hasMove();
        return managed(
            job, lastCompleted, reason,
            useCompleted ? lastCompleted.bestMove() : fallback,
            useCompleted || hasFallback,
            failure, diagnostics
        );
    }

    private ManagedSearchResult failure(
        SearchJob job, long fallback, boolean hasFallback, SearchResult lastCompleted,
        Throwable failure, SearchDiagnosticsSnapshot diagnostics
    ) {
        lastFailure = failure;
        final boolean useCompleted = lastCompleted != null && lastCompleted.hasMove();
        return managed(
            job, lastCompleted, SearchTermination.FAILURE,
            useCompleted ? lastCompleted.bestMove() : fallback,
            useCompleted || hasFallback,
            failure, diagnostics
        );
    }

    private ManagedSearchResult managed(
        SearchJob job, SearchResult completed, SearchTermination reason,
        long bestMove, boolean hasMove, Throwable failure,
        SearchDiagnosticsSnapshot diagnostics
    ) {
        return new ManagedSearchResult(
            job.generation, hasMove ? bestMove : 0L, hasMove, completed,
            reason, job.control.nodes(), failure, diagnostics
        );
    }

    /**
     * Iteration callbacks run synchronously on the owned worker while holding
     * the generation gate. Replacement/invalidation therefore cannot race an
     * old callback past the installation of a newer generation. Observer
     * failures are recorded and contained; exact search ownership continues.
     */
    private SearchObserver iterationObserver(SearchJob job) {
        return new SearchObserver() {
            @Override
            public void onIterationCompleted(IterationSnapshot snapshot) {
                synchronized(lock) {
                    if(current != job || shutdown) return;
                    try {
                        job.observer.onIterationCompleted(snapshot);
                    } catch(Throwable failure) {
                        lastFailure = failure;
                    }
                }
            }
        };
    }

    private void ensureOpen() {
        if(shutdown) throw new IllegalStateException("Search lifecycle is shut down.");
    }

    private void joinWorker() {
        boolean interrupted = false;
        try {
            worker.join(JOIN_MILLIS);
        } catch(InterruptedException exception) {
            interrupted = true;
        }
        if(interrupted) Thread.currentThread().interrupt();
    }

    private static final class SearchJob {
        final long generation;
        final long[] board;
        final GameHistory history;
        final SearchLimits limits;
        final SearchControl control;
        final long startNanos;
        final SearchObserver observer;
        final boolean diagnosticsEnabled;
        final Listener listener;

        SearchJob(
            long generation, long[] board, GameHistory history, SearchLimits limits,
            SearchControl control, long startNanos, SearchObserver observer,
            boolean diagnosticsEnabled, Listener listener
        ) {
            this.generation = generation;
            this.board = board;
            this.history = history;
            this.limits = limits;
            this.control = control;
            this.startNanos = startNanos;
            this.observer = observer;
            this.diagnosticsEnabled = diagnosticsEnabled;
            this.listener = listener;
        }
    }
}
