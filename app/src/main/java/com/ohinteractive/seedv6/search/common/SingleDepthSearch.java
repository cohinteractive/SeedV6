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

    /** Schedule implementation-owned new-game state to be reset safely. */
    default void newGame() {}

    /** Release implementation-owned workers; leaf searches have nothing to close. */
    @Override
    default void close() {}
}
