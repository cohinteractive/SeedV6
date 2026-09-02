package com.ohinteractive.seedv6.search.iterative;

import com.ohinteractive.seedv6.search.common.SearchResult;

/** Internal controller outcome consumed by the managed lifecycle owner. */
public record IterativeSearchOutcome(
    SearchResult lastCompletedResult,
    boolean targetDepthCompleted,
    boolean terminalRoot
) {
    public IterativeSearchOutcome {
        if(lastCompletedResult != null && !lastCompletedResult.completed()) {
            throw new IllegalArgumentException("Last completed result must be exact and completed.");
        }
        if(terminalRoot && lastCompletedResult == null) {
            throw new IllegalArgumentException("A terminal outcome requires its completed result.");
        }
    }
}
