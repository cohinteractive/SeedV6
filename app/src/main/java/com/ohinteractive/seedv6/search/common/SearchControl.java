package com.ohinteractive.seedv6.search.common;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Search-private cooperative control shared only by the lifecycle owner and
 * its worker. Cancellation is cross-thread visible and idempotent. The worker
 * alone updates the primitive cumulative node count.
 */
public final class SearchControl {

    public static SearchControl unlimited() {
        return UNLIMITED;
    }

    public static SearchControl controlled(
        long nodeLimit, long startNanos, long timeBudgetNanos, TimeSource timeSource
    ) {
        if(nodeLimit < NO_LIMIT) {
            throw new IllegalArgumentException("Node limit must be non-negative or absent.");
        }
        if(timeBudgetNanos < NO_LIMIT) {
            throw new IllegalArgumentException("Time budget must be non-negative or absent.");
        }
        return new SearchControl(
            nodeLimit, startNanos, timeBudgetNanos,
            Objects.requireNonNull(timeSource, "timeSource"), false
        );
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public boolean hasNodeLimit() {
        return nodeLimit != NO_LIMIT;
    }

    public boolean hasTimeLimit() {
        return timeBudgetNanos != NO_LIMIT;
    }

    /**
     * Called immediately before entering one counted child node. A budget N
     * permits exactly N successful calls over the whole go command.
     */
    public boolean tryEnterNode() {
        if(unlimited) return true;
        if(nodeLimit != NO_LIMIT) {
            if(termination != SearchTermination.NONE) return false;
            if(nodes >= nodeLimit) {
                terminate(SearchTermination.NODE_LIMIT);
                return false;
            }
        }
        nodes ++;
        return true;
    }

    /** Cheap periodic worker checkpoint for cancellation and elapsed time. */
    public boolean checkpoint() {
        if(unlimited) return true;
        if(termination != SearchTermination.NONE) return false;
        if(timeBudgetNanos != NO_LIMIT
            && elapsedNanos(timeSource.nanoTime(), startNanos) >= timeBudgetNanos) {
            terminate(SearchTermination.TIME_LIMIT);
            return false;
        }
        return termination == SearchTermination.NONE;
    }

    /**
     * Controller boundary used before starting another exact iteration. Node
     * exhaustion is deliberately not part of periodic traversal checkpoints:
     * the Nth and final node of a complete tree must still be allowed to unwind.
     */
    public boolean checkpointNodeBudget() {
        if(unlimited) return true;
        if(termination != SearchTermination.NONE) return false;
        if(nodeLimit != NO_LIMIT && nodes >= nodeLimit) {
            terminate(SearchTermination.NODE_LIMIT);
            return false;
        }
        return true;
    }

    public boolean request(SearchTermination reason) {
        Objects.requireNonNull(reason, "reason");
        if(reason == SearchTermination.NONE || reason == SearchTermination.COMPLETED
            || reason == SearchTermination.NODE_LIMIT || reason == SearchTermination.TIME_LIMIT
            || reason == SearchTermination.FAILURE) {
            throw new IllegalArgumentException("Not an external cancellation reason: " + reason);
        }
        return terminate(reason);
    }

    public SearchTermination termination() {
        return termination;
    }

    public long nodes() {
        return nodes;
    }

    /** Elapsed monotonic time from the managed top-level search start. */
    public long elapsedNanos() {
        return unlimited ? 0L : elapsedNanos(timeSource.nanoTime(), startNanos);
    }

    /** Waits without spinning after the current exact-search depth ceiling. */
    public void awaitTermination() throws InterruptedException {
        while(checkpoint() && checkpointNodeBudget()) {
            if(hasTimeLimit()) terminationSignal.await(1L, java.util.concurrent.TimeUnit.MILLISECONDS);
            else terminationSignal.await();
        }
    }

    public static long elapsedNanos(long nowNanos, long startNanos) {
        final long elapsed = nowNanos - startNanos;
        return elapsed < 0L ? 0L : elapsed;
    }

    private static final long NO_LIMIT = -1L;
    private static final SearchControl UNLIMITED = new SearchControl(
        NO_LIMIT, 0L, NO_LIMIT, TimeSource.SYSTEM, true
    );

    private final long nodeLimit;
    private final long startNanos;
    private final long timeBudgetNanos;
    private final TimeSource timeSource;
    private final boolean unlimited;
    private final CountDownLatch terminationSignal = new CountDownLatch(1);
    private volatile SearchTermination termination = SearchTermination.NONE;
    private long nodes;

    private SearchControl(
        long nodeLimit, long startNanos, long timeBudgetNanos,
        TimeSource timeSource, boolean unlimited
    ) {
        this.nodeLimit = nodeLimit;
        this.startNanos = startNanos;
        this.timeBudgetNanos = timeBudgetNanos;
        this.timeSource = timeSource;
        this.unlimited = unlimited;
    }

    private synchronized boolean terminate(SearchTermination reason) {
        if(termination != SearchTermination.NONE) return false;
        termination = reason;
        terminationSignal.countDown();
        return true;
    }
}
