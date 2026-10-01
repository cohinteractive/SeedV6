package com.ohinteractive.seedv6.search.driver;

import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.tablebase.TablebaseWin;

/**
 * Lifecycle outcome. lastCompletedResult supplies the searched score/move/PV;
 * null means no iteration completed. A separate tablebaseWin can supply a root
 * winning move without a searched score or depth. attemptedDepth is not a completed depth.
 * Nodes include all admitted work, including an interrupted later iteration.
 */
public record SearchDriverOutcome(SearchResult lastCompletedResult, boolean targetDepthCompleted,
        boolean terminalRoot, int attemptedDepth, boolean iterationIncomplete, long nodes,
        SearchDiagnosticsSnapshot diagnostics, TablebaseWin tablebaseWin) {
    public SearchDriverOutcome(SearchResult lastCompletedResult, boolean targetDepthCompleted,
            boolean terminalRoot, int attemptedDepth, boolean iterationIncomplete, long nodes,
            SearchDiagnosticsSnapshot diagnostics) {
        this(lastCompletedResult, targetDepthCompleted, terminalRoot, attemptedDepth, iterationIncomplete,
                nodes, diagnostics, null);
    }
    public SearchDriverOutcome {
        if(attemptedDepth < 0 || nodes < 0 || diagnostics == null)
            throw new IllegalArgumentException("Invalid driver statistics.");
        if(lastCompletedResult != null && !lastCompletedResult.completed())
            throw new IllegalArgumentException("Cannot retain an incomplete iteration.");
        if(targetDepthCompleted && (lastCompletedResult == null || iterationIncomplete))
            throw new IllegalArgumentException("Completion requires a completed iteration.");
        if(iterationIncomplete && (attemptedDepth == 0
                || lastCompletedResult != null && lastCompletedResult.depth() >= attemptedDepth))
            throw new IllegalArgumentException("An incomplete attempt must follow any completed result.");
        if(terminalRoot && (!targetDepthCompleted || lastCompletedResult.hasMove()))
            throw new IllegalArgumentException("A terminal result must complete without a move.");
        if(tablebaseWin != null && (lastCompletedResult != null || targetDepthCompleted || terminalRoot
                || attemptedDepth != 0 || iterationIncomplete || nodes != 0))
            throw new IllegalArgumentException("A tablebase decision is separate from searched depth.");
    }
}
