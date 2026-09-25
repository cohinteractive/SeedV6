package com.ohinteractive.seedv6.search.driver;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.tt.TTable;

/**
 * Worker-confined production boundary for one ordinary ExactSearch invocation.
 * Owns evaluator and Search TTable state, reusing both across iterations and
 * requests. Production uses TTable's existing default size; injected null is TT-off.
 * Consumer nodes count admitted children (not roots), as SearchControl requires;
 * the independent ExactSearch result continues to count roots as well.
 */
public final class ExactSearchAdapter implements SingleDepthSearch {
    private final ExactSearch exact;
    private final long[] root = new long[Board.MAX_BITBOARDS];
    private final long[] scratch = new long[Board.MAX_BITBOARDS];
    private final long[] rootMoves = new long[512];
    private SearchControl control;
    private long nodes;
    private long evaluations;
    private int maximumPly;
    private boolean active;

    public ExactSearchAdapter() { this(SearchEvaluation.handcrafted()); }

    public ExactSearchAdapter(SearchEvaluation evaluation) { this(ExactEvaluator.from(evaluation)); }

    public ExactSearchAdapter(ExactEvaluator evaluator) { this(evaluator, new TTable()); }

    /** Explicit small table or null is useful for bounded/headless comparisons. */
    public ExactSearchAdapter(SearchEvaluation evaluation, TTable table) { this(ExactEvaluator.from(evaluation), table); }

    public ExactSearchAdapter(ExactEvaluator evaluator, TTable table) {
        Objects.requireNonNull(evaluator, "evaluator");
        exact = new ExactSearch(new ExactEvaluator() {
            @Override public void initialize(long[] board) { evaluator.initialize(board); }
            @Override public int evaluate(long[] board, int ply) {
                evaluations++;
                return evaluator.evaluate(board, ply);
            }
            @Override public void child(long[] parent, long[] child, int parentPly) {
                // ExactSearch prepares each real child exactly once, before its
                // next cancellation checkpoint. Refusing admission sets the
                // control reason; that checkpoint aborts without using child state.
                // Exhausting the budget at the FINAL admitted child does not
                // cancel: a complete iteration is still allowed to unwind.
                if(!control.tryEnterNode()) return;
                nodes++;
                maximumPly = Math.max(maximumPly, parentPly + 1);
                evaluator.child(parent, child, parentPly);
            }
        }, table);
    }

    @Override public SearchResult search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        if(active) throw new IllegalStateException("ExactSearchAdapter cannot be re-entered.");
        if(request.depth() > maxSupportedDepth()) throw new IllegalArgumentException("Unsupported depth.");
        active = true;
        control = request.control();
        nodes = evaluations = 0;
        maximumPly = 0;
        try {
            request.copyBoardInto(root);
            int count = Gen.genAll(root[0], root[1], root[2], root[3], (int) root[Board.STATUS],
                    root[Board.KEY], true, rootMoves, scratch);
            var result = exact.search(root, request.gameHistory(), request.depth(), () -> !control.checkpoint());
            // Thread interruption is also an ExactSearch cancellation source.
            if(!result.completed() && Thread.currentThread().isInterrupted()) control.request(SearchTermination.STOPPED);
            var converted = new SearchResult(result.bestMove(), result.hasMove(), result.score(),
                    request.depth(), nodes, count, result.completed(), result.principalVariation(),
                    diagnostics(request.diagnosticsEnabled()));
            request.observer().onSearchFinished(converted, result.elapsedNanos());
            return converted;
        } finally {
            control = null;
            active = false;
        }
    }

    private SearchDiagnosticsSnapshot diagnostics(boolean enabled) {
        if(!enabled) return SearchDiagnosticsSnapshot.disabled();
        var empty = SearchDiagnosticsSnapshot.enabledEmpty();
        var worker = empty.worker();
        return new SearchDiagnosticsSnapshot(true, new WorkerMetrics(
                new NodeMetrics(nodes, 0, maximumPly, 0, evaluations), worker.transpositionTable(),
                worker.moveOrder(), worker.qsearch(), worker.selective()), empty.iteration());
    }

    @Override public int maxSupportedDepth() { return ExactSearch.MAX_DEPTH; }
    @Override public void beginRequest() { exact.beginRequest(); }
    @Override public void endRequest() { exact.endRequest(); }
    @Override public void newGame() { exact.newGame(); }
    @Override public boolean usesAspiration() { return false; }
}
