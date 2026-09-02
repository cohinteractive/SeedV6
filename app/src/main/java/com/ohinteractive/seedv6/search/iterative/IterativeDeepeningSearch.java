package com.ohinteractive.seedv6.search.iterative;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.common.WindowedSearch;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.IterationMetrics;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * Single-threaded iterative-deepening controller over the accepted exact WS10
 * search. Only exact depths 1, 2, 3, ... are ordinary iterations.
 *
 * <p>Depth one and mate-adjacent previous scores use the full window. Other
 * depths try symmetric 50, 100, 200 and 400 centipawn windows around the last
 * completed score, then unconditionally fall back to the full mate-safe
 * window. A score on either window edge is a fail-soft bound and is never
 * published as the exact iteration.</p>
 *
 * <p>Every depth and retry shares the request's cumulative {@link SearchControl}
 * and one {@link WindowedSearch} top-level sequence. The WS10 worker therefore
 * reuses its TT generation, history and killers throughout one iterative
 * search. This controller does not maintain a root-order sidecar; existing
 * position-keyed TT hints and legally validated WS10 ordering remain the sole
 * root-order owners. {@link #newGame()} forwards the established worker reset.</p>
 */
public final class IterativeDeepeningSearch implements AutoCloseable {

    /** V6 evaluation is centipawn-scaled; a pawn is approximately 80-150 cp. */
    public static final int INITIAL_ASPIRATION_WIDTH = 50;
    public static final int MAX_NARROW_ATTEMPTS = 4;
    public static final int MATE_GUARD = 512;

    public IterativeDeepeningSearch(SingleDepthSearch exactSearch) {
        this(exactSearch, INITIAL_ASPIRATION_WIDTH, MAX_NARROW_ATTEMPTS, MATE_GUARD);
    }

    IterativeDeepeningSearch(
        SingleDepthSearch exactSearch, int initialWidth,
        int maximumNarrowAttempts, int mateGuard
    ) {
        this.exactSearch = Objects.requireNonNull(exactSearch, "exactSearch");
        if(initialWidth < 1) throw new IllegalArgumentException("Aspiration width must be positive.");
        if(maximumNarrowAttempts < 1) {
            throw new IllegalArgumentException("At least one bounded aspiration attempt is required.");
        }
        if(mateGuard < 0 || mateGuard >= TranspositionScores.MATE_THRESHOLD) {
            throw new IllegalArgumentException("Mate aspiration guard is invalid.");
        }
        this.initialWidth = initialWidth;
        this.maximumNarrowAttempts = maximumNarrowAttempts;
        this.mateGuard = mateGuard;
        windowedSearch = exactSearch instanceof WindowedSearch candidate ? candidate : null;
    }

    public IterativeSearchOutcome search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        if(request.depth() < 1) {
            throw new IllegalArgumentException("Iterative deepening requires a target depth of at least 1.");
        }
        if(request.depth() > maxSupportedDepth()) {
            throw new IllegalArgumentException("Unsupported search depth: " + request.depth());
        }

        final long[] root = new long[Board.MAX_BITBOARDS];
        request.copyBoardInto(root);
        final SearchControl control = request.control();
        final SearchObserver observer = request.observer();
        final long localStartNanos = control.isUnlimited() ? System.nanoTime() : 0L;
        long uncontrolledNodes = 0L;
        lastCompletedResult = null;
        final IterationCounters diagnostics = new IterationCounters(request.diagnosticsEnabled());
        lastDiagnostics = diagnostics.snapshot();

        if(windowedSearch != null) windowedSearch.beginTopLevelSearch();
        try {
            for(int depth = 1; depth <= request.depth(); depth ++) {
                if(!control.checkpoint() || !control.checkpointNodeBudget()) break;
                final DepthOutcome depthOutcome = searchDepth(
                    root, request, depth,
                    lastCompletedResult == null ? null : lastCompletedResult.score(),
                    uncontrolledNodes, diagnostics
                );
                uncontrolledNodes = depthOutcome.uncontrolledNodes();
                final SearchResult attempt = depthOutcome.result();
                lastDiagnostics = diagnostics.snapshot();
                if(attempt == null || !attempt.completed()) break;

                final long cumulativeNodes = control.isUnlimited()
                    ? uncontrolledNodes : control.nodes();
                diagnostics.recordCompletedIteration(depth);
                lastDiagnostics = diagnostics.snapshot();
                lastCompletedResult = cumulativeResult(attempt, cumulativeNodes, lastDiagnostics);
                final long elapsedNanos = control.isUnlimited()
                    ? SearchControl.elapsedNanos(System.nanoTime(), localStartNanos)
                    : control.elapsedNanos();
                observer.onIterationCompleted(IterationSnapshot.from(lastCompletedResult, elapsedNanos));

                final boolean terminal = lastCompletedResult.legalRootMoves() == 0;
                if(terminal || depth == request.depth()) {
                    return new IterativeSearchOutcome(
                        lastCompletedResult, true, terminal, lastDiagnostics
                    );
                }
            }
            return new IterativeSearchOutcome(lastCompletedResult, false, false, lastDiagnostics);
        } finally {
            lastDiagnostics = diagnostics.snapshot();
            if(windowedSearch != null) windowedSearch.endTopLevelSearch();
        }
    }

    public int maxSupportedDepth() {
        return exactSearch.maxSupportedDepth();
    }

    public void newGame() {
        exactSearch.newGame();
    }

    @Override
    public void close() {
        exactSearch.close();
    }

    /** Deepest completed iteration retained even when a later attempt fails. */
    public SearchResult lastCompletedResult() {
        return lastCompletedResult;
    }

    public SearchDiagnosticsSnapshot lastDiagnostics() {
        return lastDiagnostics;
    }

    private static final int NEGATIVE_INFINITY = -TranspositionScores.MATE_SCORE - 1;
    private static final int POSITIVE_INFINITY = TranspositionScores.MATE_SCORE + 1;

    private final SingleDepthSearch exactSearch;
    private final WindowedSearch windowedSearch;
    private final int initialWidth;
    private final int maximumNarrowAttempts;
    private final int mateGuard;
    private SearchResult lastCompletedResult;
    private SearchDiagnosticsSnapshot lastDiagnostics = SearchDiagnosticsSnapshot.disabled();

    private DepthOutcome searchDepth(
        long[] root, SearchRequest topLevel, int depth, Integer previousScore,
        long uncontrolledNodes, IterationCounters diagnostics
    ) {
        if(windowedSearch == null || depth == 1 || previousScore == null
            || aspirationUnsafe(previousScore)) {
            return attempt(
                root, topLevel, depth, NEGATIVE_INFINITY, POSITIVE_INFINITY,
                uncontrolledNodes, diagnostics
            );
        }

        long width = initialWidth;
        for(int attemptIndex = 0; attemptIndex < maximumNarrowAttempts; attemptIndex ++) {
            if(!topLevel.control().checkpoint()
                || !topLevel.control().checkpointNodeBudget()) {
                return new DepthOutcome(null, uncontrolledNodes);
            }
            final int alpha = (int) Math.max(
                NEGATIVE_INFINITY, (long) previousScore - width
            );
            final int beta = (int) Math.min(
                POSITIVE_INFINITY, (long) previousScore + width
            );
            if(alpha == NEGATIVE_INFINITY && beta == POSITIVE_INFINITY) break;

            diagnostics.recordAspirationAttempt();
            final DepthOutcome outcome = attempt(
                root, topLevel, depth, alpha, beta, uncontrolledNodes, diagnostics
            );
            uncontrolledNodes = outcome.uncontrolledNodes();
            final SearchResult result = outcome.result();
            if(result == null || !result.completed()) return outcome;
            if(result.score() > alpha && result.score() < beta) return outcome;
            if(result.score() <= alpha) diagnostics.recordFailLow();
            else diagnostics.recordFailHigh();
            width = Math.min((long) POSITIVE_INFINITY, width * 2L);
        }

        if(!topLevel.control().checkpoint()
            || !topLevel.control().checkpointNodeBudget()) {
            return new DepthOutcome(null, uncontrolledNodes);
        }
        diagnostics.recordFullWindowFallback();
        return attempt(
            root, topLevel, depth, NEGATIVE_INFINITY, POSITIVE_INFINITY,
            uncontrolledNodes, diagnostics
        );
    }

    private DepthOutcome attempt(
        long[] root, SearchRequest topLevel, int depth, int alpha, int beta,
        long uncontrolledNodes, IterationCounters diagnostics
    ) {
        final SearchRequest attemptRequest = new SearchRequest(
            root, topLevel.gameHistory(), depth, SearchObserver.NONE, topLevel.control(),
            topLevel.diagnosticsEnabled()
        );
        final SearchResult result = windowedSearch == null
            ? exactSearch.search(attemptRequest)
            : windowedSearch.searchWindow(attemptRequest, alpha, beta);
        diagnostics.observe(result.diagnostics());
        final long updatedNodes = topLevel.control().isUnlimited()
            ? saturatedAdd(uncontrolledNodes, result.nodes()) : uncontrolledNodes;
        return new DepthOutcome(result, updatedNodes);
    }

    private boolean aspirationUnsafe(int score) {
        return Math.abs((long) score)
            >= (long) TranspositionScores.MATE_THRESHOLD - mateGuard;
    }

    private static SearchResult cumulativeResult(
        SearchResult result, long nodes, SearchDiagnosticsSnapshot diagnostics
    ) {
        return new SearchResult(
            result.bestMove(), result.hasMove(), result.score(), result.depth(), nodes,
            result.legalRootMoves(), true, result.principalVariation(), diagnostics
        );
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private record DepthOutcome(SearchResult result, long uncontrolledNodes) {}

    private static final class IterationCounters {
        private final boolean enabled;
        private SearchDiagnosticsSnapshot workerSnapshot;
        private long completedIterations;
        private long aspirationAttempts;
        private long failLowResearches;
        private long failHighResearches;
        private long fullWindowFallbacks;
        private int deepestCompletedDepth;

        IterationCounters(boolean enabled) {
            this.enabled = enabled;
            workerSnapshot = enabled
                ? SearchDiagnosticsSnapshot.enabledEmpty()
                : SearchDiagnosticsSnapshot.disabled();
        }

        void observe(SearchDiagnosticsSnapshot snapshot) {
            if(!enabled) return;
            workerSnapshot = snapshot.enabled()
                ? snapshot : SearchDiagnosticsSnapshot.enabledEmpty();
        }

        void recordCompletedIteration(int depth) {
            if(!enabled) return;
            completedIterations ++;
            deepestCompletedDepth = Math.max(deepestCompletedDepth, depth);
        }

        void recordAspirationAttempt() {
            if(enabled) aspirationAttempts ++;
        }

        void recordFailLow() {
            if(enabled) failLowResearches ++;
        }

        void recordFailHigh() {
            if(enabled) failHighResearches ++;
        }

        void recordFullWindowFallback() {
            if(enabled) fullWindowFallbacks ++;
        }

        SearchDiagnosticsSnapshot snapshot() {
            if(!enabled) return SearchDiagnosticsSnapshot.disabled();
            return workerSnapshot.withIteration(new IterationMetrics(
                completedIterations, aspirationAttempts, failLowResearches,
                failHighResearches, fullWindowFallbacks, deepestCompletedDepth
            ));
        }
    }
}
