package com.ohinteractive.seedv6.search.tablebase;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.*;

@Timeout(15)
class TablebaseLifecycleTest {
    @Test void completedOutcomeHasNoFabricatedDepthScoreOrIteration() {
        long[] b=Board.fromFen(SyzygyRootTest.QUEEN);long m=SyzygyRootTest.move(b,"b1g1");
        SingleDepthSearch exact=new SingleDepthSearch(){
            public SearchResult search(SearchRequest r){throw new AssertionError("unexpected exact work");}
            public void beginRequest(){throw new AssertionError("unexpected TT generation");}
            public int maxSupportedDepth(){return 256;}
        };
        var iterations=new AtomicInteger();
        try(var driver=new SearchDriver(exact,(board,history,control)->new TablebaseWin(m,13))){
            var request=new SearchRequest(b,GameHistory.initial(b),12,new SearchObserver(){
                @Override public void onIterationCompleted(IterationSnapshot x){iterations.incrementAndGet();}
            },SearchControl.unlimited(),true);
            var out=driver.search(request);assertNotNull(out.tablebaseWin());assertNull(out.lastCompletedResult());assertNull(driver.lastCompletedResult());
            assertFalse(out.targetDepthCompleted());assertFalse(out.terminalRoot());assertFalse(out.iterationIncomplete());assertEquals(0,out.attemptedDepth());assertEquals(0,out.nodes());assertEquals(0,iterations.get());
        }
    }
    @Test void declinedProbePreservesOrdinarySearchAndLateDeadlineRejectsDecision() {
        long[] b=Board.fromFen(SyzygyRootTest.QUEEN);
        try(var baseline=new SearchDriver();var optional=new SearchDriver(new ExactSearchAdapter(),RootTablebase.NONE)){
            var request=new SearchRequest(b,3);assertEquals(baseline.search(request).lastCompletedResult(),optional.search(request).lastCompletedResult());
        }
        var time=new AtomicLong();var control=SearchControl.controlled(-1,0,1,time::get);long m=SyzygyRootTest.move(b,"b1g1");
        try(var driver=new SearchDriver(new ExactSearchAdapter(),(board,history,c)->{time.set(1);return new TablebaseWin(m,13);})){var out=driver.search(new SearchRequest(b,GameHistory.initial(b),5,control));assertNull(out.tablebaseWin());assertNull(out.lastCompletedResult());assertEquals(SearchTermination.TIME_LIMIT,control.termination());}
    }
    @Test void managedPublicationCarriesSeparateWinningMoveAndCanBeReused() throws Exception {
        long[] b=Board.fromFen(SyzygyRootTest.QUEEN);long m=SyzygyRootTest.move(b,"b1g1");
        var policy=new SyzygyRoot(x->SyzygyRootTest.packed(m,4,13));
        try(var service=new SearchLifecycleService(TimeSource.SYSTEM,ExactSearchAdapter::new,policy)){
            for(int i=0;i<2;i++){var result=new CompletableFuture<ManagedSearchResult>();
                service.start(b,GameHistory.initial(b),new SearchLimits(12,-1,-1,false),result::complete);
                var completed=result.get(10,TimeUnit.SECONDS);assertEquals(SearchTermination.TABLEBASE,completed.termination());assertNotNull(completed.tablebaseWin());assertNull(completed.lastCompletedResult());assertEquals(m,completed.bestMove());assertEquals(0,completed.nodes());assertNull(completed.failure());
                assertEquals(completed.tablebaseWin(),completed.withTermination(SearchTermination.STOPPED).tablebaseWin());
            }
        }
    }
    @Test void tablebaseCompletionCannotBeRequestedAsExternalCancellation() {
        assertThrows(IllegalArgumentException.class,()->SearchControl.controlled(-1,0,-1,()->0).request(SearchTermination.TABLEBASE));
    }

    @Test void infiniteAnalysisWaitsForStopBeforePublishingTheCompletedDecision() throws Exception {
        long[] b=Board.fromFen(SyzygyRootTest.QUEEN);long m=SyzygyRootTest.move(b,"b1g1");var probed=new CountDownLatch(1);
        RootTablebase policy=(board,history,control)->{probed.countDown();return new TablebaseWin(m,13);};
        try(var service=new SearchLifecycleService(TimeSource.SYSTEM,ExactSearchAdapter::new,policy)){
            var result=new CompletableFuture<ManagedSearchResult>();
            service.start(b,GameHistory.initial(b),new SearchLimits(0,-1,-1,true),result::complete);
            assertTrue(probed.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->result.get(50,TimeUnit.MILLISECONDS));
            service.stop();var complete=result.get(5,TimeUnit.SECONDS);assertEquals(SearchTermination.STOPPED,complete.termination());assertEquals(m,complete.bestMove());assertNotNull(complete.tablebaseWin());
        }
    }
}
