package com.ohinteractive.seedv6.search.common;

/**
 * Exact single-depth search that can execute multiple fail-soft root windows
 * inside one top-level search generation.
 *
 * <p>The lifecycle is deliberately explicit: an iterative owner begins one
 * sequence, performs one or more exact-depth/window attempts with the same
 * worker-owned state, and ends the sequence in a {@code finally} block. Direct
 * {@link #search(SearchRequest)} calls remain independent top-level searches.</p>
 */
public interface WindowedSearch extends SingleDepthSearch {

    /** Prepare TT/new-game ownership once for a top-level iterative search. */
    void beginTopLevelSearch();

    /** Execute one fail-soft exact-depth attempt inside the active sequence. */
    SearchResult searchWindow(SearchRequest request, int alpha, int beta);

    /** End the active top-level sequence after all depths and retries unwind. */
    void endTopLevelSearch();
}
