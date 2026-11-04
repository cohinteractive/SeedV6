package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.search.driver.AspirationSearch;

import java.util.Objects;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.IterationMetrics;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** IC-C2 Upper-window carryover: preserve ADAPTIVE16's initial lower bound and
 * recovery width while retaining half the previous resolved initial UPPER width.
 * The wider upper endpoint permits volatility without lowering the starting scout
 * threshold. This tests an asymmetric cost hypothesis, not a score-direction forecast.
 * Research-only; baseline completion, mate, cancellation and lifecycle semantics. */
public final class P4UpperWindowSearch implements SingleDepthSearch {
    private final SingleDepthSearch owner;
    private final AspirationSearch.WindowSearch window;
    private final AspirationSearch.AttemptObserver observer;
    private final long[] root = new long[Board.MAX_BITBOARDS];
    private int previousDepth, previousScore, earlierScore, previousInitialWidth;
    private boolean hasEarlier, active, requestActive;

    public P4UpperWindowSearch(SingleDepthSearch owner, AspirationSearch.WindowSearch window,
            AspirationSearch.AttemptObserver observer) {
        this.owner = Objects.requireNonNull(owner);
        this.window = Objects.requireNonNull(window);
        this.observer = observer;
    }
    static int initialWidth(int movement, int previousWidth) {
        return Math.min(512, Math.max(Math.max(16, 2 * movement), previousWidth / 2));
    }
    @Override public SearchResult search(SearchRequest request) {
        if(active) throw new IllegalStateException("IC-C2 cannot be re-entered.");
        active = true;
        long start = System.nanoTime();
        try {
            var result = resolve(request);
            request.observer().onSearchFinished(result, System.nanoTime() - start);
            return result;
        } finally { active = false; }
    }
    private SearchResult resolve(SearchRequest request) {
        boolean narrow = requestActive && request.depth() > 1 && previousDepth == request.depth() - 1
                && !TranspositionScores.isMateScore(previousScore)
                && (!hasEarlier || !TranspositionScores.isMateScore(earlierScore));
        int movement = hasEarlier ? Math.abs(previousScore - earlierScore) : 0;
        int width = narrow ? Math.min(512, Math.max(16, 2 * movement)) : 16;
        int initialWidth = narrow ? initialWidth(movement, previousInitialWidth) : 16;
        int alpha = narrow ? lower(previousScore, width) : -ExactSearch.INFINITY;
        int beta = narrow ? upper(previousScore, initialWidth) : ExactSearch.INFINITY;
        int attempts = 0, lows = 0, highs = 0, fallbacks = 0;
        long nodes = 0;
        boolean selective = false;
        var diagnostics = request.diagnosticsEnabled()
                ? SearchDiagnosticsSnapshot.enabledEmpty() : SearchDiagnosticsSnapshot.disabled();
        // Suppress per-invocation observer publication. Only resolved completion escapes.
        SearchRequest invocation = request;
        if(request.observer() != SearchObserver.NONE) {
            request.copyBoardInto(root);
            invocation = new SearchRequest(root, request.gameHistory(), request.depth(),
                    SearchObserver.NONE, request.control(), request.diagnosticsEnabled());
        }
        SearchResult result;
        while(true) {
            long start = observer == null ? 0 : System.nanoTime();
            result = window.search(invocation, alpha, beta);
            nodes += result.nodes();
            selective |= result.selective();
            if(request.diagnosticsEnabled()) diagnostics = diagnostics.mergeWorkers(result.diagnostics());
            if(narrow) attempts++;
            if(observer != null) observer.completed(request.depth(), alpha, beta, result, System.nanoTime() - start);
            if(!result.completed() || !available(request, false)) break;
            boolean low = result.score() <= alpha, high = result.score() >= beta;
            // A one-sided window can contain a mate return. Preserve explicit
            // full-window verification before publishing any newly found mate.
            boolean mate = narrow && TranspositionScores.isMateScore(result.score());
            if(!narrow || (!low && !high && !mate)) {
                earlierScore = previousScore;
                hasEarlier = previousDepth > 0 && previousDepth == request.depth() - 1;
                previousScore = result.score(); previousDepth = request.depth();
                previousInitialWidth = initialWidth;
                return aggregate(request, result, nodes, diagnostics, attempts, lows, highs, fallbacks, selective, true);
            }
            if(!available(request, true)) break;
            // Re-search counters count another invocation actually admitted after a
            // completed failure. A stopped/exhausted failure is visible in the
            // attempt trace but must not claim an unperformed re-search.
            if(low) lows++; else if(high) highs++;
            boolean fallback = attempts >= 4 || mate;
            if(fallback) {
                alpha = -ExactSearch.INFINITY; beta = ExactSearch.INFINITY;
                narrow = false; fallbacks++;
            } else {
                width *= 2;
                // Preserve the unfailed endpoint; selective instability can reverse direction.
                if(low) alpha = lower(Math.min(alpha, result.score()), width);
                else beta = upper(Math.max(beta, result.score()), width);
            }
        }
        return aggregate(request, result, nodes, diagnostics, attempts, lows, highs, fallbacks, selective, false);
    }
    private static boolean available(SearchRequest request, boolean retry) {
        if(Thread.currentThread().isInterrupted()) request.control().request(SearchTermination.STOPPED);
        return !Thread.currentThread().isInterrupted() && request.control().checkpoint()
                && (!retry || request.control().checkpointNodeBudget());
    }
    private static SearchResult aggregate(SearchRequest request, SearchResult result, long nodes,
            SearchDiagnosticsSnapshot diagnostics, int attempts, int lows, int highs, int fallbacks,
            boolean selective, boolean completed) {
        if(diagnostics.enabled()) diagnostics = diagnostics.withIteration(new IterationMetrics(
                0, attempts, lows, highs, fallbacks, 0));
        return new SearchResult(completed ? result.bestMove() : 0, completed && result.hasMove(),
                completed ? result.score() : Value.INVALID, request.depth(), nodes, result.legalRootMoves(),
                completed, completed ? result.principalVariation() : new long[0], diagnostics, selective);
    }
    static int lower(int score, int width) {
        return (int)Math.max(-TranspositionScores.MATE_THRESHOLD, (long)score - width);
    }
    static int upper(int score, int width) {
        return (int)Math.min(TranspositionScores.MATE_THRESHOLD, (long)score + width);
    }
    @Override public void beginRequest() {
        if(requestActive || active) throw new IllegalStateException("Request already active");
        previousDepth = 0; previousInitialWidth = 0; hasEarlier = false; owner.beginRequest(); requestActive = true;
    }
    @Override public void endRequest() { previousDepth = 0; previousInitialWidth = 0; hasEarlier = false; requestActive = false; owner.endRequest(); }
    @Override public void newGame() { previousDepth = 0; previousInitialWidth = 0; hasEarlier = false; owner.newGame(); }
    @Override public void close() { owner.close(); }
    @Override public int maxSupportedDepth() { return owner.maxSupportedDepth(); }
    @Override public int activeSearchThreads() { return owner.activeSearchThreads(); }
}
