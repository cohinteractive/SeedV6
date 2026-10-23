package com.ohinteractive.seedv6.search.exact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class Cp64SelectivityStudyTest {
    @Test void observedAttemptsIncludeAbortedWorkAndDoNotChangeTraversal() throws Exception {
        var evaluation=BaselineEngine.load().evaluation();var board=Board.startingPosition();
        var root=new P3HistoryStudy.Root("start",board,GameHistory.initial(board));
        assertEquals(BaselineEngine.configuration(1,64),Cp64SelectivityStudy.configuration("baseline",1));
        for(String profile:Cp64SelectivityStudy.PROFILES) {
            var observed=Cp64SelectivityStudy.observed(evaluation,profile);
            try(var a=new SearchDriver(Cp64SelectivityStudy.configuration(profile,1).create(evaluation));
                var b=new SearchDriver(observed.owner())) {
                var plain=ConstructionTtAdaptiveStudy.request(a,root,30,12000);
                var measured=ConstructionTtAdaptiveStudy.request(b,root,30,12000);
                ConstructionTtStudy.same((SearchDriverOutcome)plain.get("outcome"),(SearchDriverOutcome)measured.get("outcome"));
                assertEquals(12000,observed.attempts().stream().mapToLong(x->((SearchResult)x.get("outcome")).nodes()).sum());
                assertFalse(((SearchResult)observed.attempts().getLast().get("outcome")).completed());
            }
        }
    }
    @Test void existingControlCompositionsDrainAndReuseAcrossWorkers() throws Exception {
        var evaluation=BaselineEngine.load().evaluation();var board=Board.startingPosition();var saved=board.clone();
        for(String profile:Cp64SelectivityStudy.PROFILES) for(int workers:new int[]{1,4,16})
            try(var driver=new SearchDriver(Cp64SelectivityStudy.configuration(profile,workers).create(evaluation))) {
                var control=SearchControl.controlled(2000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var result=driver.search(new SearchRequest(board,GameHistory.initial(board),40,control));
                assertEquals(2000,result.nodes());assertEquals(0,driver.activeSearchThreads());assertArrayEquals(saved,board);
                if(result.lastCompletedResult()!=null) Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                driver.newGame();assertTrue(driver.search(new SearchRequest(board,2)).targetDepthCompleted());
                var expired=SearchControl.controlled(2000,System.nanoTime()-10000,1,TimeSource.SYSTEM);
                assertNull(driver.search(new SearchRequest(board,GameHistory.initial(board),40,expired)).lastCompletedResult());
                assertEquals(SearchTermination.TIME_LIMIT,expired.termination());assertEquals(0,driver.activeSearchThreads());
            }
    }
}
