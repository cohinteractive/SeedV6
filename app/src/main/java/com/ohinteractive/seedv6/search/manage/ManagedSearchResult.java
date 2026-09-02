package com.ohinteractive.seedv6.search.manage;

import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;

/** Final publication selected by the lifecycle service for one generation. */
public record ManagedSearchResult(
    long generation,
    long bestMove,
    boolean hasMove,
    SearchResult lastCompletedResult,
    SearchTermination termination,
    long nodes,
    Throwable failure
) {
    public ManagedSearchResult {
        if(!hasMove && bestMove != 0L) {
            throw new IllegalArgumentException("A managed result without a move must use zero.");
        }
        if(lastCompletedResult != null && !lastCompletedResult.completed()) {
            throw new IllegalArgumentException("The retained result must be a completed exact depth.");
        }
    }

    public ManagedSearchResult withTermination(SearchTermination reason) {
        return new ManagedSearchResult(
            generation, bestMove, hasMove, lastCompletedResult, reason, nodes, failure
        );
    }
}
