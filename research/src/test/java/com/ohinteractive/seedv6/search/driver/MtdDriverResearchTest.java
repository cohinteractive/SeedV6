package com.ohinteractive.seedv6.search.driver;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import static org.junit.jupiter.api.Assertions.*;

class MtdDriverResearchTest {
    static final class Owner implements SingleDepthSearch {
        public SearchResult search(SearchRequest r){throw new AssertionError();}
        public int maxSupportedDepth(){return 10;}
    }
    private static SearchResult result(SearchRequest r,int score,boolean selective) {
        return new SearchResult(1,true,score,r.depth(),0,2,true,new long[]{1},null,selective);
    }
    @Test void allScoreDomainEndpointsAndBadGuessesConvergeWithoutOverflow() {
        for(var policy:MtdDriverResearch.Policy.values()) for(int target:new int[]{-32768,-32512,-32511,-1,0,1,32511,32512,32768})
            for(int guess:new int[]{-32768,0,32768}) {
                var mtd=new MtdDriverResearch(new Owner(),(r,a,b)->result(r,r.depth()==1?guess:target,false),policy,null);
                try(var d=new SearchDriver(mtd)) {
                    var r=d.search(new SearchRequest(Board.startingPosition(),2));
                    assertTrue(r.targetDepthCompleted());assertEquals(target,r.lastCompletedResult().score());
                }
            }
    }
    @Test void selectiveContradictionAndFailedMaterializationUseFullFallback() {
        for(boolean contradiction:new boolean[]{true,false}) {
            var phases=new ArrayList<String>();int[] calls={0};
            var mtd=new MtdDriverResearch(new Owner(),(r,a,b)-> {
                if(r.depth()==1)return result(r,0,false);
                int call=++calls[0];
                return result(r,call==1?10:call==2?(contradiction?9:10):call==3?12:15,true);
            },MtdDriverResearch.Policy.PREV,(d,p,a,b,l,u,r)->{if(d==2)phases.add(p);});
            try(var d=new SearchDriver(mtd)) {
                var r=d.search(new SearchRequest(Board.startingPosition(),2));
                assertTrue(r.targetDepthCompleted());assertTrue(r.lastCompletedResult().selective());
                assertEquals("fallback",phases.getLast());
                assertEquals(contradiction?12:15,r.lastCompletedResult().score());
            }
        }
    }
    @Test void stopBetweenEachPassCannotPublishConvergenceOrMaterialization() {
        for(int stopAfter=1;stopAfter<=3;stopAfter++) {
            final int stop=stopAfter;int[] calls={0};var b=Board.startingPosition();
            var control=SearchControl.controlled(1000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
            var mtd=new MtdDriverResearch(new Owner(),(r,a,z)->result(r,r.depth()==1?0:10,true),
                    MtdDriverResearch.Policy.PREV,(d,p,a,z,l,u,r)->{if(d==2 && ++calls[0]==stop)control.request(SearchTermination.STOPPED);});
            try(var driver=new SearchDriver(mtd)) {
                var r=driver.search(new SearchRequest(b,GameHistory.initial(b),2,control));
                assertFalse(r.targetDepthCompleted());assertEquals(1,r.lastCompletedResult().depth());
                assertEquals(0,r.lastCompletedResult().score());
            }
        }
    }
}
