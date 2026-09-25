package com.ohinteractive.seedv6.search.common;

/**
 * One worker-confined exact-depth search owned by the lifecycle service.
 * Implementations may retain reusable primitive search state between calls.
 */
public interface SingleDepthSearch extends AutoCloseable {

    SearchResult search(SearchRequest request);

    int maxSupportedDepth();

    /** Bracket one production request; all its fixed-depth iterations share ownership. */
    default void beginRequest() {}
    default void endRequest() {}

    /** Fixed score-scale policy: uncalibrated evaluators require full-window iterations. */
    default boolean usesAspiration() { return true; }

    /** Schedule implementation-owned new-game state to be reset safely. */
    default void newGame() {}

    /** Release implementation-owned workers; leaf searches have nothing to close. */
    @Override
    default void close() {}
}
