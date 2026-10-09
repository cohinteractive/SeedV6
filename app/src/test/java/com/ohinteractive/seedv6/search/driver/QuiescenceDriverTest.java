package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static org.junit.jupiter.api.Assertions.*;

class QuiescenceDriverTest {
    @Test void wholeBudgetIncludesQsearchAndAbortedIterationAndRetainsCompletedResult() {
        for(int policy : new int[] {0, 2, 3, 4, 64, 65, 66, 67}) {
            var b = Board.startingPosition();
            var control = SearchControl.controlled(5000, System.nanoTime(), -1, TimeSource.SYSTEM);
            try(var driver = new SearchDriver(ExactSearchAdapter.quiescenceResearch(SearchEvaluation.handcraftedIsolation(), policy))) {
                var r = driver.search(new SearchRequest(b, GameHistory.initial(b), 20, SearchObserver.NONE, control, true));
                assertEquals(5000, r.nodes());
                assertEquals(5000, r.diagnostics().totalEnteredNodes());
                assertTrue(r.diagnostics().worker().nodes().qNodes() > 0);
                assertTrue(r.iterationIncomplete());
                assertNotNull(r.lastCompletedResult());
                assertTrue(r.lastCompletedResult().completed() && r.lastCompletedResult().selective());
                assertTrue(r.lastCompletedResult().depth() < r.attemptedDepth());
                assertEquals(SearchTermination.NODE_LIMIT, control.termination());
                assertEquals(0, driver.activeSearchThreads());
                driver.newGame();
                assertTrue(driver.search(new SearchRequest(b, 1)).targetDepthCompleted());
            }
        }
    }
}
