package com.ohinteractive.seedv6.search.manage;

import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;

/** Final publication selected by the lifecycle service for one generation. */
public record ManagedSearchResult(
    long generation,
    long bestMove,
    boolean hasMove,
    SearchResult lastCompletedResult,
    SearchTermination termination,
    long nodes,
    Throwable failure,
    SearchDiagnosticsSnapshot diagnostics
) {
    public ManagedSearchResult {
        if(!hasMove && bestMove != 0L) {
            throw new IllegalArgumentException("A managed result without a move must use zero.");
        }
        if(lastCompletedResult != null && !lastCompletedResult.completed()) {
            throw new IllegalArgumentException("The retained result must be a completed exact depth.");
        }
        if(diagnostics == null) diagnostics = SearchDiagnosticsSnapshot.disabled();
    }

    public ManagedSearchResult(
        long generation, long bestMove, boolean hasMove, SearchResult lastCompletedResult,
        SearchTermination termination, long nodes, Throwable failure
    ) {
        this(
            generation, bestMove, hasMove, lastCompletedResult, termination, nodes, failure,
            lastCompletedResult == null
                ? SearchDiagnosticsSnapshot.disabled() : lastCompletedResult.diagnostics()
        );
    }

    public ManagedSearchResult withTermination(SearchTermination reason) {
        return new ManagedSearchResult(
            generation, bestMove, hasMove, lastCompletedResult, reason, nodes, failure,
            diagnostics
        );
    }
}
