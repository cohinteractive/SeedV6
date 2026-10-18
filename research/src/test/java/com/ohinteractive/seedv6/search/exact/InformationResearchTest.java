package com.ohinteractive.seedv6.search.exact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class InformationResearchTest {
    @ParameterizedTest @ValueSource(ints={2,4,16})
    void completedHintsRespectRequestAccountingAndDrainAtSupportedWorkerCounts(int workers) throws Exception {
        var board=Board.startingPosition();var e=P3InformationBaselineTest.evaluation();
        var engine=(ParallelSearch)InformationResearch.create(e,workers,64,4);
        try(var driver=new SearchDriver(engine)) {
            var complete=driver.search(new SearchRequest(board,3));
            assertTrue(complete.targetDepthCompleted());assertEquals(0,engine.activeSearchThreads());
            Phase2Oracle.legalPv(board,complete.lastCompletedResult().principalVariation());
            driver.newGame();
            var control=SearchControl.controlled(200,System.nanoTime(),10_000_000_000L,TimeSource.SYSTEM);
            var stopped=driver.search(new SearchRequest(board,GameHistory.initial(board),20,SearchObserver.NONE,control,true));
            assertEquals(SearchTermination.NODE_LIMIT,control.termination());assertEquals(200,stopped.nodes());
            assertEquals(stopped.nodes(),stopped.diagnostics().totalEnteredNodes());assertEquals(0,engine.activeSearchThreads());
            Phase2Oracle.legalPv(board,stopped.lastCompletedResult().principalVariation());
        }
        assertEquals(0,engine.activeSearchThreads());
    }

    @Test void policyAndWorkerDomainsAreExplicit() throws Exception {
        for(int p=0;p<8;p++) assertTrue(InformationResearch.accepts(1,p));
        for(int workers=2;workers<=16;workers++) for(int p=0;p<8;p++)
            assertEquals(p==0 || p==4,InformationResearch.accepts(workers,p));
        assertFalse(InformationResearch.accepts(0,0));assertFalse(InformationResearch.accepts(17,4));
        assertFalse(InformationResearch.accepts(1,8));
        assertThrows(IllegalArgumentException.class,()->InformationResearch.create(SearchEvaluation.handcrafted(),1,64,4));
        var e=P3InformationBaselineTest.evaluation();
        assertThrows(IllegalArgumentException.class,()->InformationResearch.create(e,4,64,7));
        assertThrows(IllegalArgumentException.class,()->InformationResearch.create(e,1,72,4));
    }
}
