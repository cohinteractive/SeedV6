package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class ConstructionSearchTest {
    @Test void resolvedNarrowIterationsRetainGuidanceButFailedBoundsDoNot() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();
        var raw=ExactSearch.constructionResearch(ExactEvaluator.from(e),new com.ohinteractive.seedv6.search.tt.TTable(1),FlatSearchConfig.policy4());
        raw.beginRequest();
        try {
            assertTrue(raw.search(board,1).completed());
            var reuse=Policy4ProductionTest.field(raw,"iterationReuse");
            var r=raw.searchWindow(board,GameHistory.initial(board),2,-1000,1000,()->false);
            assertTrue(r.completed());assertTrue(r.score()>-1000 && r.score()<1000);
            assertEquals(2,Policy4ProductionTest.field(reuse,"completedDepth"));
            assertTrue(raw.searchWindow(board,GameHistory.initial(board),3,1000,1001,()->false).completed());
            assertEquals(2,Policy4ProductionTest.field(reuse,"completedDepth"));
        } finally {raw.endRequest();}
    }
    @Test void configurationRejectsUnknownBitsAndInvalidResources() {
        assertThrows(IllegalArgumentException.class,()->new FlatSearchConfig(-1));
        assertThrows(IllegalArgumentException.class,()->new ConstructionSearch(FlatSearchConfig.policy4(),17,64,null));
        assertThrows(IllegalArgumentException.class,()->new ConstructionSearch(FlatSearchConfig.policy4(),1,0,null));
        var config=FlatSearchConfig.policy4();
        assertTrue(config.has(QXRAY));assertFalse(config.with(QXRAY,false).has(QXRAY));
        assertTrue(config.has(QXRAY));
        assertNotEquals(config.identity(),config.with(QXRAY,false).identity());
    }

    @Test void policy4CompositionIsTraversalIdenticalToAcceptedFlattenedFactory() throws Exception {
        var e=P3InformationBaselineTest.evaluation();
        for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")) {
            var board=Board.fromFen(fen);
            try(var expected=new SearchDriver(Sr002ExecutionStudy.engine(1,e,2));
                var actual=new SearchDriver(new ConstructionSearch(FlatSearchConfig.policy4(),1,64,null).create(e))) {
                var a=expected.search(new SearchRequest(board,4));var b=actual.search(new SearchRequest(board,4));
                assertTrue(a.targetDepthCompleted());assertTrue(b.targetDepthCompleted());
                assertEquals(a.nodes(),b.nodes());assertEquals(a.lastCompletedResult().score(),b.lastCompletedResult().score());
                assertArrayEquals(a.lastCompletedResult().principalVariation(),b.lastCompletedResult().principalVariation());
            }
        }
    }

    @Test void exactControlsAndParallelAspirationMatchIndependentFiniteTrees() {
        var e=SearchEvaluation.handcrafted();ExactEvaluator hce=(b,p)->Eval.evaluate(b);
        for(String fen:List.of("4k3/8/8/3q4/4P3/8/8/4K3 w - - 98 1",
                "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1",
                "8/k1P5/2K5/8/8/8/8/8 w - - 98 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5Q2/6K1/8/8/8/8/8 b - - 100 1")) {
            var board=Board.fromFen(fen);var game=GameHistory.initial(board);
            int expected=new Phase2Oracle.RestrictedOracle(hce,0).score(board,new SearchLineHistory(game),3,0,0,-1);
            for(int workers:new int[]{1,2,4}) for(var aspiration:new AspirationSearch.Policy[]{null,AspirationSearch.Policy.WIDEN16}) {
                try(var driver=new SearchDriver(new ConstructionSearch(FlatSearchConfig.exact(),workers,1,aspiration).create(e))) {
                    var r=driver.search(new SearchRequest(board,game,3));assertTrue(r.targetDepthCompleted());
                    assertEquals(expected,r.lastCompletedResult().score(),fen+workers+aspiration);
                    Phase2Oracle.legalPv(board,r.lastCompletedResult().principalVariation());
                    assertEquals(0,driver.activeSearchThreads());
                }
            }
        }
        var b=Board.startingPosition();
        var ttOff=ExactSearch.constructionResearch(hce,null,FlatSearchConfig.exact()).search(b,2);
        assertEquals(ExactSearch.quiescenceResearch(hce).search(b,2).score(),ttOff.score());
    }

    @Test void allWorkerDomainsPreserveBudgetCancellationAndReuseWithAspiration() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var b=Board.startingPosition();var original=b.clone();
        for(int workers:new int[]{1,2,4,16}) {
            var config=new ConstructionSearch(FlatSearchConfig.policy4(),workers,1,AspirationSearch.Policy.WIDEN16);
            try(var driver=new SearchDriver(config.create(e))) {
                for(int budget:new int[]{1,100,1500}) {
                    var c=SearchControl.controlled(budget,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                    var r=driver.search(new SearchRequest(b,GameHistory.initial(b),20,SearchObserver.NONE,c,true));
                    assertEquals(budget,r.nodes());assertEquals(r.nodes(),r.diagnostics().totalEnteredNodes());
                    assertFalse(r.targetDepthCompleted());assertEquals(0,driver.activeSearchThreads());
                    if(r.lastCompletedResult()!=null) Phase2Oracle.legalPv(b,r.lastCompletedResult().principalVariation());
                    assertArrayEquals(original,b);driver.newGame();
                }
                assertTrue(driver.search(new SearchRequest(b,3)).targetDepthCompleted());
            }
        }
    }
}
