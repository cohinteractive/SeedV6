package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

class LmrAccountingTest {
    @Test void earlierQsearchIsCountedByActualDomainAndReSearchDoesNotDoubleAdmitChild() {
        long[] counts = {0, 0}; int[] minimumQroot = {256}, maxQply = {0};
        var observer = new ExactEvaluator() {
            public int evaluate(long[] b, int p) { return Eval.evaluate(b); }
            public void child(long[] parent, long[] child, int ply, int qply) {
                counts[qply < 0 ? 0 : 1]++;
                if(qply == 0) minimumQroot[0] = Math.min(minimumQroot[0], ply + 1);
                maxQply[0] = Math.max(maxQply[0], qply);
            }
        };
        var board = Board.startingPosition();
        var direct = ExactSearch.lateMoveReductionResearch(observer, new TTable(), ExactSearch.LMR_SCALED, null).search(board, 5);
        var adapter = ExactSearchAdapter.lateMoveReductionResearch(SearchEvaluation.handcraftedIsolation(), ExactSearch.LMR_SCALED, null);
        var result = adapter.search(new SearchRequest(board, GameHistory.initial(board), 5,
                SearchObserver.NONE, SearchControl.unlimited(), true));
        var metrics = result.diagnostics().worker().nodes();
        assertEquals(direct.score(), result.score());
        assertTrue(minimumQroot[0] < 5, "LMR must reach qsearch earlier than nominal root depth");
        assertEquals(counts[0], metrics.mainNodes()); assertEquals(counts[1], metrics.qNodes());
        assertEquals(counts[0] + counts[1], result.nodes()); assertEquals(maxQply[0], metrics.maximumQply());
    }

    @Test void abortedIterationsRemainInWholeRequestBudgetAndDiagnostics() {
        for(int policy=1; policy<=3; policy++) try(var driver = new SearchDriver(
                ExactSearchAdapter.lateMoveReductionResearch(SearchEvaluation.handcraftedIsolation(), policy, null))) {
            var board = Board.startingPosition();
            var control = SearchControl.controlled(20_000, System.nanoTime(), -1, TimeSource.SYSTEM);
            var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 20,
                    SearchObserver.NONE, control, true));
            assertFalse(result.targetDepthCompleted()); assertTrue(result.iterationIncomplete());
            assertEquals(20_000, result.nodes()); assertEquals(result.nodes(), result.diagnostics().totalEnteredNodes());
            assertTrue(result.lastCompletedResult().completed());
            assertTrue(result.lastCompletedResult().depth() < result.attemptedDepth());
            assertTrue(result.diagnostics().worker().nodes().qNodes() > 0);
        }
    }
}
