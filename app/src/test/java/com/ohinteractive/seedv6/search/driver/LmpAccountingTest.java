package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static org.junit.jupiter.api.Assertions.*;

class LmpAccountingTest {
    @Test void omissionsDoNotSpendNodesAndAbortedWorkRemainsInRequestTotal() {
        for(int policy=1;policy<=6;policy++) try(var driver=new SearchDriver(
                ExactSearchAdapter.lateMovePruningResearch(SearchEvaluation.handcraftedIsolation(),policy,null))){
            var b=Board.startingPosition();
            var control=SearchControl.controlled(20_000,System.nanoTime(),-1,TimeSource.SYSTEM);
            var result=driver.search(new SearchRequest(b,GameHistory.initial(b),20,SearchObserver.NONE,control,true));
            assertFalse(result.targetDepthCompleted());assertTrue(result.iterationIncomplete());
            assertEquals(20_000,result.nodes());assertEquals(result.nodes(),result.diagnostics().totalEnteredNodes());
            assertTrue(result.lastCompletedResult().completed());
            assertTrue(result.lastCompletedResult().depth()<result.attemptedDepth());
            assertTrue(result.diagnostics().worker().nodes().qNodes()>0);
        }
    }
}
