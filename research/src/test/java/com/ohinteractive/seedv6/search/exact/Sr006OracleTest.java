package com.ohinteractive.seedv6.search.exact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static org.junit.jupiter.api.Assertions.*;

/** Aspiration must preserve the independent unpruned qsearch tree's score. */
@Timeout(90)
class Sr006OracleTest {
    @Test void everyHistoricalRootPreservesUnprunedQsearchScoreAndLegalPrefix() {
        for(var position : AspirationResearch.positions()) {
            var b=Board.fromFen(position.fen());
            for(var policy : AspirationSearch.Policy.values()) try(var driver=new SearchDriver(
                    new AspirationSearch(ExactSearchAdapter.unprunedQuiescence(
                            SearchEvaluation.handcraftedIsolation()),policy))) {
                var outcome=driver.search(new SearchRequest(b,2,new SearchObserver() {
                    public void onIterationCompleted(IterationSnapshot r) {
                        var expected=ExactSearch.quiescenceResearch((board,ply)->Eval.evaluate(board),0)
                                .search(b,r.depth());
                        assertTrue(expected.completed()); assertEquals(expected.score(),r.score(),position.name()+policy);
                        Phase2Oracle.legalPv(b,r.result().principalVariation());
                    }
                }));
                assertTrue(outcome.targetDepthCompleted());
            }
        }
    }
}
