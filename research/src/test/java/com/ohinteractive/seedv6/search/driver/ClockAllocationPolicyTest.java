package com.ohinteractive.seedv6.search.driver;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.exact.Cp64ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import static org.junit.jupiter.api.Assertions.*;

class ClockAllocationPolicyTest {
    static SearchResult result(int depth,long move,int score) {
        return new SearchResult(move,true,score,depth,1,20,true,new long[]{move},null,true);
    }
    static final class Fixture implements SingleDepthSearch {
        final AtomicLong clock; int searches,calls,ends; boolean stop=true, incomplete;
        Fixture(AtomicLong clock){this.clock=clock;}
        @Override public int maxSupportedDepth(){return 20;}
        @Override public SearchResult search(SearchRequest request){
            searches++;request.control().tryEnterNode();clock.addAndGet(10);
            return incomplete ? new SearchResult(0,false,0,request.depth(),1,20,false)
                    : result(request.depth(),42,12);
        }
        @Override public boolean clockAllocationComplete(SearchResult completed,long elapsed,long budget){
            calls++;assertTrue(completed.completed());assertTrue(completed.selective());
            assertEquals(0,activeSearchThreads());return stop;
        }
        @Override public void endRequest(){ends++;}
    }
    static SearchDriverOutcome run(Fixture f, SearchControl control,int depth,SearchObserver observer){
        var board=Board.startingPosition();
        try(var driver=new SearchDriver(f)) {
            return driver.search(new SearchRequest(board,GameHistory.initial(board),depth,observer,control,false));
        }
    }
    @Test void softCompletionKeepsOnlyActualCompletedDepthAndDoesNotAttemptAnotherIteration(){
        var time=new AtomicLong();var fixture=new Fixture(time);var publications=new ArrayList<Integer>();
        var control=SearchControl.controlled(-1,0,100,time::get,true);
        var r=run(fixture,control,10,new SearchObserver(){
            @Override public void onIterationCompleted(IterationSnapshot s){publications.add(s.depth());}
        });
        assertEquals(SearchTermination.TIME_ALLOCATION,control.termination());
        assertEquals(1,r.attemptedDepth());assertEquals(1,r.lastCompletedResult().depth());
        assertFalse(r.iterationIncomplete());assertFalse(r.targetDepthCompleted());
        assertEquals(java.util.List.of(1),publications);assertEquals(1,fixture.ends);
    }
    @Test void hardDeadlineNodesAndExternalStopDominateSoftCompletion(){
        for(int kind=0;kind<3;kind++) {
            var time=new AtomicLong();var fixture=new Fixture(time);
            var c=SearchControl.controlled(kind==1?1:-1,0,kind==0?10:100,time::get,true);
            final int mode=kind;
            run(fixture,c,10,new SearchObserver(){@Override public void onIterationCompleted(IterationSnapshot s){
                if(mode==2)c.request(SearchTermination.STOPPED);
            }});
            assertEquals(new SearchTermination[]{SearchTermination.TIME_LIMIT,SearchTermination.NODE_LIMIT,SearchTermination.STOPPED}[kind],c.termination());
        }
    }
    @Test void nonClockTargetCompletionAndIncompleteResultsNeverInvokePolicy(){
        for(int kind=0;kind<3;kind++) {
            var time=new AtomicLong();var f=new Fixture(time);f.incomplete=kind==2;
            var c=SearchControl.controlled(-1,0,1000,time::get,kind!=0);
            var r=run(f,c,kind==1?1:5,SearchObserver.NONE);
            assertEquals(0,f.calls);assertNotEquals(SearchTermination.TIME_ALLOCATION,c.termination());
            if(kind==2)assertNull(r.lastCompletedResult());else assertTrue(r.targetDepthCompleted());
        }
    }
    @Test void stablePolicyRequiresDepthEvidenceResetsAndDoesNotTreatMateAsOrdinary(){
        var f=new Fixture(new AtomicLong());var p=new Cp64ClockPolicy(f,Cp64ClockPolicy.Policy.STABLE3_HALF);
        p.beginRequest();
        for(int d=1;d<6;d++)assertFalse(p.clockAllocationComplete(result(d,42,12),99,100));
        assertFalse(p.clockAllocationComplete(result(6,42,12),49,100));
        assertTrue(p.clockAllocationComplete(result(7,42,12),50,100));
        assertFalse(p.clockAllocationComplete(result(8,43,12),50,100));
        assertFalse(p.clockAllocationComplete(result(9,43,12),50,100));
        assertTrue(p.clockAllocationComplete(result(10,43,12),50,100));
        assertFalse(p.clockAllocationComplete(result(11,43,TranspositionScores.MATE_SCORE-5),99,100));
        assertFalse(p.clockAllocationComplete(result(12,43,12),99,100));
        p.endRequest();p.beginRequest();assertFalse(p.clockAllocationComplete(result(6,42,12),99,100));p.endRequest();
    }
    @Test void unconditionalControlUsesSameDepthAndOverflowSafeThreshold(){
        var p=new Cp64ClockPolicy(new Fixture(new AtomicLong()),Cp64ClockPolicy.Policy.HALF);
        assertFalse(p.clockAllocationComplete(result(5,42,12),Long.MAX_VALUE,Long.MAX_VALUE));
        assertFalse(p.clockAllocationComplete(result(6,42,12),Long.MAX_VALUE/2,Long.MAX_VALUE));
        assertTrue(p.clockAllocationComplete(result(7,42,12),Long.MAX_VALUE/2+1,Long.MAX_VALUE));
    }
}
