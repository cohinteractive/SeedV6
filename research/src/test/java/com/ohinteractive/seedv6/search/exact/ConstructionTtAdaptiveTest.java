package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class ConstructionTtAdaptiveTest {
    @Test void cp64RebuildAndDiagnosticsNeutralityWithLegalPv() throws Exception {
        var model=ConventionalNnueEngine.load();
        for(String fen:List.of(Board.FEN_STARTING_POSITION,
                "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","7k/P7/8/8/8/8/8/7K w - - 0 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1","7k/5Q2/6K1/8/8/8/8/8 b - - 100 1")) {
            var board=Board.fromFen(fen);var config=ConstructionSearch.v1(1,8,AspirationSearch.Policy.ADAPTIVE16);
            try(var actual=new SearchDriver(config.create(model.evaluation()));
                var reference=new SearchDriver(config.create(SearchEvaluation.conventionalFullRebuild(model.model())))) {
                var a=actual.search(new SearchRequest(board,5));var b=reference.search(new SearchRequest(board,5));
                assertEquals(b.nodes(),a.nodes());assertEquals(b.lastCompletedResult().score(),a.lastCompletedResult().score());
                assertArrayEquals(b.lastCompletedResult().principalVariation(),a.lastCompletedResult().principalVariation());
                actual.newGame();var c=actual.search(new SearchRequest(board,GameHistory.initial(board),5,
                    SearchObserver.NONE,SearchControl.unlimited(),true));
                assertEquals(a.nodes(),c.nodes());assertEquals(a.lastCompletedResult().score(),c.lastCompletedResult().score());
                assertArrayEquals(a.lastCompletedResult().principalVariation(),c.lastCompletedResult().principalVariation());
                assertEquals(c.nodes(),c.diagnostics().totalEnteredNodes());Phase2Oracle.legalPv(board,c.lastCompletedResult().principalVariation());
            }
        }
    }
    @Test void supportedWorkersAccountCancelDrainAndReuse() throws Exception {
        var evaluation=BaselineEngine.load().evaluation();var board=Board.startingPosition();var initial=board.clone();
        for(int workers=1;workers<=16;workers++) try(var driver=new SearchDriver(
                ConstructionSearch.v1(workers,1,AspirationSearch.Policy.ADAPTIVE16).create(evaluation))) {
            for(long budget:new long[]{1,100,1500}) {
                var control=SearchControl.controlled(budget,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var result=driver.search(new SearchRequest(board,GameHistory.initial(board),40,control));
                assertEquals(budget,result.nodes());assertEquals(control.nodes(),result.nodes());
                assertEquals(0,driver.activeSearchThreads());assertArrayEquals(initial,board);
                if(result.lastCompletedResult()!=null) Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                driver.newGame();
            }
            var expired=SearchControl.controlled(10000,System.nanoTime()-10000,1,TimeSource.SYSTEM);
            assertNull(driver.search(new SearchRequest(board,GameHistory.initial(board),10,expired)).lastCompletedResult());
            assertEquals(SearchTermination.TIME_LIMIT,expired.termination());assertEquals(0,driver.activeSearchThreads());
            assertTrue(driver.search(new SearchRequest(board,3)).targetDepthCompleted());
        }
    }
}
