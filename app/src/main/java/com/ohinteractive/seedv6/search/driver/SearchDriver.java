package com.ohinteractive.seedv6.search.driver;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.IterationMetrics;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;

/**
 * R003 production coordinator: depth 1, 2, ...; one full-window ExactSearch
 * invocation at each depth. No previous-iteration information enters the tree.
 * Worker-confined; separate consumers own separate drivers/evaluator stacks.
 */
public final class SearchDriver implements AutoCloseable {
    private final SingleDepthSearch exact;
    private SearchResult lastCompletedResult;
    private SearchDiagnosticsSnapshot lastDiagnostics = SearchDiagnosticsSnapshot.disabled();
    private boolean active;

    public SearchDriver() { this(SearchEvaluation.handcrafted()); }
    public SearchDriver(SearchEvaluation evaluation) { this(new ExactSearchAdapter(evaluation)); }

    /** Compatibility seam for independently supplied single-depth facilities/tests. */
    public SearchDriver(SingleDepthSearch exact) { this.exact = Objects.requireNonNull(exact, "exact"); }

    public SearchDriverOutcome search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        if(request.depth() < 1 || request.depth() > maxSupportedDepth())
            throw new IllegalArgumentException("Unsupported iterative depth: " + request.depth());
        if(active) throw new IllegalStateException("SearchDriver cannot be re-entered.");
        active = true;
        lastCompletedResult = null;
        lastDiagnostics = request.diagnosticsEnabled()
                ? SearchDiagnosticsSnapshot.enabledEmpty() : SearchDiagnosticsSnapshot.disabled();
        var workerDiagnostics = lastDiagnostics;
        long nodes = 0;
        int attemptedDepth = 0;
        int completedDepth = 0;
        boolean incomplete = false;
        long start = System.nanoTime();
        var control = request.control();
        long[] root = new long[Board.MAX_BITBOARDS];
        request.copyBoardInto(root);
        try {
            for(int depth = 1; depth <= request.depth(); depth++) {
                if(Thread.currentThread().isInterrupted()) control.request(SearchTermination.STOPPED);
                if(Thread.currentThread().isInterrupted() || !control.checkpoint() || !control.checkpointNodeBudget()) break;
                attemptedDepth = depth;
                SearchResult attempt = exact.search(new SearchRequest(root, request.gameHistory(), depth,
                        SearchObserver.NONE, control, request.diagnosticsEnabled()));
                nodes += attempt.nodes();
                if(request.diagnosticsEnabled() && attempt.diagnostics().enabled())
                    workerDiagnostics = workerDiagnostics.mergeWorkers(attempt.diagnostics());
                incomplete = !attempt.completed();
                if(!incomplete) completedDepth = depth;
                if(request.diagnosticsEnabled()) {
                    lastDiagnostics = workerDiagnostics.withIteration(new IterationMetrics(
                            completedDepth, 0, 0, 0, 0, completedDepth));
                }
                if(incomplete) break;
                lastCompletedResult = new SearchResult(attempt.bestMove(), attempt.hasMove(), attempt.score(),
                        depth, nodes, attempt.legalRootMoves(), true, attempt.principalVariation(), lastDiagnostics);
                long elapsed = control.isUnlimited() ? SearchControl.elapsedNanos(System.nanoTime(), start) : control.elapsedNanos();
                request.observer().onIterationCompleted(IterationSnapshot.from(lastCompletedResult, elapsed));
                // At positive depth a completed no-move ExactSearch result is
                // terminal (including rule draws with otherwise legal moves).
                boolean terminal = !attempt.hasMove();
                if(terminal || depth == request.depth())
                    return new SearchDriverOutcome(lastCompletedResult, true, terminal, attemptedDepth, false, nodes, lastDiagnostics);
            }
            return new SearchDriverOutcome(lastCompletedResult, false, false, attemptedDepth, incomplete, nodes, lastDiagnostics);
        } finally {
            active = false;
        }
    }

    public int maxSupportedDepth() { return exact.maxSupportedDepth(); }
    public SearchResult lastCompletedResult() { return lastCompletedResult; }
    public SearchDiagnosticsSnapshot lastDiagnostics() { return lastDiagnostics; }
    public void newGame() { exact.newGame(); }
    @Override public void close() { exact.close(); }
}
