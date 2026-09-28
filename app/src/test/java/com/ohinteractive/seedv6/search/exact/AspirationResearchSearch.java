package com.ohinteractive.seedv6.search.exact;

import java.util.ArrayList;
import java.util.List;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * SR-006B/C test-only single-depth adapter, run by the unchanged production
 * SearchDriver. Width zero delegates to the production adapter (CONTROL).
 * Fixed widths and ADAPT-172/628 make one narrow attempt, then a full retry.
 * No failed bound escapes as a completed iteration. All attempts are inside
 * the driver's existing beginRequest/endRequest, including cancellation.
 * Consumer nodes retain ExactSearchAdapter's admitted-child convention.
 */
final class AspirationResearchSearch implements SingleDepthSearch {
    static final int ADAPT_172_628 = -1;
    record Attempt(int alpha, int beta, SearchResult result, long nanos) {}
    record Iteration(int depth, Integer earlier, Integer previous, Integer previousMovement,
                     String selectedClass, int halfWidth, String eligibility, String narrowResult,
                     Attempt initial, Attempt retry, SearchResult result, long nanos) {}

    final List<Iteration> iterations = new ArrayList<>();
    private final int width;
    private final ExactSearch exact;
    private final ExactSearchAdapter production;
    private final long[] root = new long[Board.MAX_BITBOARDS];
    private final long[] scratch = new long[Board.MAX_BITBOARDS];
    private final long[] moves = new long[512];
    private SearchControl control;
    private SearchResult previous;
    private SearchResult earlier;
    private long nodes;
    private long evaluations;
    private int maximumPly;

    AspirationResearchSearch(int width, ExactEvaluator evaluator, TTable table) {
        if(width != 0 && width != 512 && width != 628 && width != ADAPT_172_628)
            throw new IllegalArgumentException("SR-006B/C policy");
        this.width = width;
        production = width == 0 ? new ExactSearchAdapter(evaluator, table) : null;
        exact = width == 0 ? null : new ExactSearch(new ExactEvaluator() {
            @Override public void initialize(long[] b) { evaluator.initialize(b); }
            @Override public int evaluate(long[] b, int ply) { evaluations++; return evaluator.evaluate(b, ply); }
            @Override public void child(long[] parent, long[] child, int ply) {
                if(!control.tryEnterNode()) return;
                nodes++;
                maximumPly = Math.max(maximumPly, ply + 1);
                evaluator.child(parent, child, ply);
            }
        }, table);
    }

    static int alpha(int center, int width) {
        return (int) Math.max(-TranspositionScores.MATE_THRESHOLD, (long) center - width);
    }
    static int beta(int center, int width) {
        return (int) Math.min(TranspositionScores.MATE_THRESHOLD, (long) center + width);
    }

    // Zero denotes the conservative full-window mate bypass, never a mate window.
    static int adaptiveWidth(Integer earlier, int previous) {
        if(TranspositionScores.isMateScore(previous)
                || earlier != null && TranspositionScores.isMateScore(earlier)) return 0;
        if(earlier == null) return 628;
        return Math.abs(previous - earlier) <= 72 ? 172 : 628;
    }

    @Override public void beginRequest() {
        previous = null;
        earlier = null;
        iterations.clear();
        if(production != null) production.beginRequest(); else exact.beginRequest();
    }
    @Override public void endRequest() {
        if(production != null) production.endRequest(); else exact.endRequest();
    }
    @Override public void newGame() {
        if(production != null) production.newGame(); else exact.newGame();
    }
    @Override public int maxSupportedDepth() { return ExactSearch.MAX_DEPTH; }
    @Override public boolean usesAspiration() { return width != 0; }

    @Override public SearchResult search(SearchRequest request) {
        if(request.diagnosticsEnabled()) throw new IllegalArgumentException("Clean SR-006B/C timing only");
        long start = System.nanoTime();
        Integer center = previous == null ? null : previous.score();
        Integer older = earlier == null ? null : earlier.score();
        Integer movement = older != null && center != null && !TranspositionScores.isMateScore(older)
                && !TranspositionScores.isMateScore(center) ? Math.abs(center - older) : null;
        String eligibility = width == 0 ? "control" : request.depth() == 1 ? "depth-one"
                : previous == null ? "no-previous" : TranspositionScores.isMateScore(center) ? "previous-mate" : "eligible";
        int selectedWidth = eligibility.equals("eligible")
                ? width == ADAPT_172_628 ? adaptiveWidth(older, center) : width : 0;
        if(eligibility.equals("eligible") && selectedWidth == 0) eligibility = "earlier-mate";
        boolean narrow = eligibility.equals("eligible");
        String selectedClass = !narrow ? eligibility : width != ADAPT_172_628 ? "fixed"
                : older == null ? "insufficient-history" : selectedWidth == 172 ? "stable" : "volatile";
        int a = narrow ? alpha(center, selectedWidth) : -ExactSearch.INFINITY;
        int b = narrow ? beta(center, selectedWidth) : ExactSearch.INFINITY;
        Attempt initial = invoke(request, a, b);
        Attempt retry = null;
        SearchResult finalResult = initial.result();
        String classification = !finalResult.completed() ? "incomplete" : !narrow ? "full"
                : finalResult.score() <= a ? "fail-low" : finalResult.score() >= b ? "fail-high" : "exact";
        if(narrow && finalResult.completed() && !classification.equals("exact")) {
            // A completed bound is not a completed target depth. Do not continue
            // after stop/interruption or an exhausted cumulative node budget.
            if(Thread.currentThread().isInterrupted()) request.control().request(SearchTermination.STOPPED);
            if(!Thread.currentThread().isInterrupted() && request.control().checkpoint()
                    && request.control().checkpointNodeBudget()) {
                retry = invoke(request, -ExactSearch.INFINITY, ExactSearch.INFINITY);
                SearchResult r = retry.result();
                finalResult = new SearchResult(r.bestMove(), r.hasMove(), r.score(), request.depth(),
                        initial.result().nodes() + r.nodes(), r.legalRootMoves(), r.completed(), r.principalVariation());
            } else {
                finalResult = new SearchResult(0, false, com.ohinteractive.seedv6.core.util.Value.INVALID,
                        request.depth(), finalResult.nodes(), finalResult.legalRootMoves(), false, new long[0]);
            }
        }
        if(finalResult.completed()) {
            earlier = previous;
            previous = finalResult;
        }
        iterations.add(new Iteration(request.depth(), older, center, movement, selectedClass, selectedWidth, eligibility, classification,
                initial, retry, finalResult, System.nanoTime() - start));
        return finalResult;
    }

    private Attempt invoke(SearchRequest request, int alpha, int beta) {
        long start = System.nanoTime();
        if(production != null) return new Attempt(alpha, beta, production.search(request), System.nanoTime() - start);
        control = request.control();
        // Mirror the production adapter's counters even in clean timing runs.
        nodes = evaluations = 0;
        maximumPly = 0;
        try {
            request.copyBoardInto(root);
            int count = Gen.genAll(root[0], root[1], root[2], root[3], (int) root[Board.STATUS],
                    root[Board.KEY], true, moves, scratch);
            var result = exact.searchWindow(root, request.gameHistory(), request.depth(), alpha, beta,
                    () -> !control.checkpoint());
            if(!result.completed() && Thread.currentThread().isInterrupted()) control.request(SearchTermination.STOPPED);
            var converted = new SearchResult(result.bestMove(), result.hasMove(), result.score(), request.depth(),
                    nodes, count, result.completed(), result.principalVariation());
            if(result.completed() && (maximumPly > request.depth() || evaluations > result.nodes()))
                throw new AssertionError("Adapter accounting");
            // Only the owning production driver publishes completed iterations.
            return new Attempt(alpha, beta, converted, System.nanoTime() - start);
        } finally { control = null; }
    }
}
