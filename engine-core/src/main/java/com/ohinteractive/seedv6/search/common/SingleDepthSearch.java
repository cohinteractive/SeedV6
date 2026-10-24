package com.ohinteractive.seedv6.search.common;

/**
 * One worker-confined exact-depth search owned by the lifecycle service.
 * Implementations may retain reusable primitive search state between calls.
 */
public interface SingleDepthSearch extends AutoCloseable {

    SearchResult search(SearchRequest request);

    int maxSupportedDepth();

    /** Presentation sampling only; implementations update at worker entry/exit, never per node. */
    default int activeSearchThreads() { return 0; }

    /** Bracket one production request; all its fixed-depth iterations share ownership. */
    default void beginRequest() {}
    default void endRequest() {}

    /**
     * Optional clock-only allocation decision at a completed, drained iteration.
     * The driver calls this after terminal/requested-depth handling. Results retain
     * their selective provenance; this decision supplies neither proof nor a new
     * hard budget. Implementations own/reset any state inside begin/endRequest.
     * Explicit non-clock modes never invoke it. This uncomposed seam is inert;
     * BaselineEngine supplies the selected production clock policy.
     */
    default boolean clockAllocationComplete(SearchResult completed, long elapsedNanos, long hardBudgetNanos) {
        return false;
    }

    /** Schedule implementation-owned new-game state to be reset safely. */
    default void newGame() {}

    /** Release implementation-owned workers; leaf searches have nothing to close. */
    @Override
    default void close() {}
}
