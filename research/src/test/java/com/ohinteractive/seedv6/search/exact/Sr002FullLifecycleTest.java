package com.ohinteractive.seedv6.search.exact;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

/** Separate late integrity gates; no changes to the frozen runtime candidate. */
@Timeout(120)
class Sr002FullLifecycleTest {
    @Test void multiworkerSparseExactValuesMatchIndependentOracle() throws Exception {
        var e=P3InformationBaselineTest.evaluation();
        for(String fen:List.of("4k3/8/8/3q4/4P3/8/8/4K3 w - - 98 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1",
                "8/k1P5/2K5/8/8/8/8/8 w - - 98 1")) {
            var board=Board.fromFen(fen);var game=GameHistory.initial(board);
            // Sparse positions exclude LMP, and D3 children cannot qualify for LMR.
            // QXRAY is disabled explicitly in both candidates for exact enumeration.
            var state=e.newState(1);
            ExactEvaluator refreshed=(b,p)->{state.initialize(b,0);return state.evaluate(b,0);};
            int expected=new Phase2Oracle.RestrictedOracle(refreshed,0)
                    .score(board,new SearchLineHistory(game),3,0,0,-1);
            for(int count:new int[]{2,4}) for(boolean flat:new boolean[]{false,true}) {
                var owner=new ParallelSearch(count,e,new TTable(1),ConcurrencyPolicy.ROOT_REUSE);
                if(flat) owner.withFlatSearchResearch(true);
                try(var driver=new SearchDriver(owner)) {
                    var result=driver.search(new SearchRequest(board,game,3));
                    assertTrue(result.targetDepthCompleted());assertEquals(expected,result.lastCompletedResult().score(),fen);
                    Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                    assertEquals(0,driver.activeSearchThreads());
                }
            }
        }
    }

    @Test void workerFailureHardStopAndRecoveryCannotPublishStaleWork() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var fail=new AtomicBoolean(true);
        var owner=new ParallelSearch(4,e,new TTable(1),ConcurrencyPolicy.ROOT_REUSE,index->{
            var delegate=ExactEvaluator.from(e);
            return new ExactEvaluator() {
                public void initialize(long[] b){delegate.initialize(b);}
                public void child(long[] p,long[] c,int ply){delegate.child(p,c,ply);}
                public int evaluate(long[] b,int ply) {
                    if(ply>=4 && fail.get()) throw new IllegalStateException("SR002 injected worker failure");
                    return delegate.evaluate(b,ply);
                }
            };
        }).withSeeResearch(SeePolicy.QXRAY,null).withCompletedHints(true,null).withFlatSearchResearch(true);
        var board=Board.fromFen(SeePruningTest.MIDDLE);var history=GameHistory.initial(board);
        try(var driver=new SearchDriver(owner)) {
            assertThrows(IllegalStateException.class,()->driver.search(new SearchRequest(board,history,6)));
            assertEquals(0,driver.activeSearchThreads());fail.set(false);driver.newGame();
            var expired=SearchControl.controlled(100000,System.nanoTime()-1_000_000_000L,1,TimeSource.SYSTEM);
            assertNull(driver.search(new SearchRequest(board,history,6,expired)).lastCompletedResult());
            assertEquals(SearchTermination.TIME_LIMIT,expired.termination());
            var result=driver.search(new SearchRequest(board,history,4));
            assertTrue(result.targetDepthCompleted());Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
            assertEquals(0,driver.activeSearchThreads());
        }
    }
}
