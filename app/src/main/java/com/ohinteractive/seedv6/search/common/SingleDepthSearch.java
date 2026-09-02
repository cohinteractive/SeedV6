package com.ohinteractive.seedv6.search.common;

/**
 * One worker-confined exact-depth search owned by the lifecycle service.
 * Implementations may retain reusable primitive search state between calls.
 */
public interface SingleDepthSearch {

    SearchResult search(SearchRequest request);

    int maxSupportedDepth();

    /** Schedule implementation-owned new-game state to be reset safely. */
    default void newGame() {}
}
