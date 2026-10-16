package com.ohinteractive.seedv6.search.iterative;

import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;

/** Internal controller outcome consumed by the managed lifecycle owner. */
public record IterativeSearchOutcome(
    SearchResult lastCompletedResult,
    boolean targetDepthCompleted,
    boolean terminalRoot,
    SearchDiagnosticsSnapshot diagnostics
) {
    public IterativeSearchOutcome {
        if(lastCompletedResult != null && !lastCompletedResult.completed()) {
            throw new IllegalArgumentException("Last completed result must be exact and completed.");
        }
        if(terminalRoot && lastCompletedResult == null) {
            throw new IllegalArgumentException("A terminal outcome requires its completed result.");
        }
        if(diagnostics == null) diagnostics = SearchDiagnosticsSnapshot.disabled();
    }

    public IterativeSearchOutcome(
        SearchResult lastCompletedResult, boolean targetDepthCompleted, boolean terminalRoot
    ) {
        this(
            lastCompletedResult, targetDepthCompleted, terminalRoot,
            lastCompletedResult == null
                ? SearchDiagnosticsSnapshot.disabled() : lastCompletedResult.diagnostics()
        );
    }
}
