package com.ohinteractive.seedv6.search.manage;

import java.util.Objects;
import java.util.function.Supplier;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

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
        this(TimeSource.SYSTEM, FlatNegamax::new);
    }

    public SearchLifecycleService(TimeSource timeSource, Supplier<FlatNegamax> searchFactory) {
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        search = Objects.requireNonNull(searchFactory, "searchFactory").get();
        worker = new Thread(this::workerLoop, "seedv6-search-worker");
        worker.start();
    }

    public long start(
        long[] board, GameHistory history, SearchLimits limits, Listener listener
    ) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(listener, "listener");
        if(limits.depth() > FlatNegamax.MAX_SUPPORTED_DEPTH) {
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
                generation, boardSnapshot, historySnapshot, limits, control, listener
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
    private final FlatNegamax search;
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
            return failure(job, 0L, false, null, failure);
        }

        final boolean hasFallback = rootMoveCount > 0;
        final long fallback = hasFallback ? rootMoves[0] : 0L;
        SearchResult lastCompleted = null;
        try {
            if(!hasFallback) {
                final SearchResult terminal = search.search(
                    new SearchRequest(job.board, job.history, 1)
                );
                return managed(job, terminal, SearchTermination.COMPLETED, terminal.bestMove(), terminal.hasMove(), null);
            }

            if(job.limits.pureDepth()) {
                final SearchResult exact = search.search(
                    new SearchRequest(job.board, job.history, job.limits.depth(), job.control)
                );
                if(exact.completed()) {
                    return managed(job, exact, SearchTermination.COMPLETED, exact.bestMove(), exact.hasMove(), null);
                }
                return interrupted(job, null, fallback, true, null);
            }

            final int maximumDepth = job.limits.depth() == SearchLimits.NO_DEPTH
                ? FlatNegamax.MAX_SUPPORTED_DEPTH
                : job.limits.depth();
            for(int depth = 1; depth <= maximumDepth; depth ++) {
                if(!job.control.checkpoint() || !job.control.checkpointNodeBudget()) break;
                final SearchResult iteration = search.search(
                    new SearchRequest(job.board, job.history, depth, job.control)
                );
                if(!iteration.completed()) break;
                lastCompleted = iteration;
                if(job.limits.depth() != SearchLimits.NO_DEPTH && depth == maximumDepth) {
                    return managed(
                        job, lastCompleted, SearchTermination.COMPLETED,
                        lastCompleted.bestMove(), lastCompleted.hasMove(), null
                    );
                }
            }

            if(job.control.termination() == SearchTermination.NONE) {
                try {
                    job.control.awaitTermination();
                } catch(InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    job.control.request(SearchTermination.SHUTDOWN);
                }
            }
            return interrupted(job, lastCompleted, fallback, true, null);
        } catch(Throwable failure) {
            return failure(job, fallback, hasFallback, lastCompleted, failure);
        }
    }

    private ManagedSearchResult interrupted(
        SearchJob job, SearchResult lastCompleted, long fallback, boolean hasFallback, Throwable failure
    ) {
        final SearchTermination reason = job.control.termination() == SearchTermination.NONE
            ? SearchTermination.FAILURE
            : job.control.termination();
        final boolean useCompleted = lastCompleted != null && lastCompleted.hasMove();
        return managed(
            job, lastCompleted, reason,
            useCompleted ? lastCompleted.bestMove() : fallback,
            useCompleted || hasFallback,
            failure
        );
    }

    private ManagedSearchResult failure(
        SearchJob job, long fallback, boolean hasFallback, SearchResult lastCompleted, Throwable failure
    ) {
        lastFailure = failure;
        final boolean useCompleted = lastCompleted != null && lastCompleted.hasMove();
        return managed(
            job, lastCompleted, SearchTermination.FAILURE,
            useCompleted ? lastCompleted.bestMove() : fallback,
            useCompleted || hasFallback,
            failure
        );
    }

    private ManagedSearchResult managed(
        SearchJob job, SearchResult completed, SearchTermination reason,
        long bestMove, boolean hasMove, Throwable failure
    ) {
        return new ManagedSearchResult(
            job.generation, hasMove ? bestMove : 0L, hasMove, completed,
            reason, job.control.nodes(), failure
        );
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
        final Listener listener;

        SearchJob(
            long generation, long[] board, GameHistory history, SearchLimits limits,
            SearchControl control, Listener listener
        ) {
            this.generation = generation;
            this.board = board;
            this.history = history;
            this.limits = limits;
            this.control = control;
            this.listener = listener;
        }
    }
}
