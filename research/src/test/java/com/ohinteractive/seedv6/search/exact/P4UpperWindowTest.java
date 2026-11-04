package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

class P4UpperWindowTest {
    @Test void retainedUpperWidthDoesNotEnlargeFailLowRecoveryStep() {
        var w=new Windows(0,100,100,101,50,100);
        try(var s=new P4UpperWindowSearch(w,w,null)) {
            s.beginRequest();
            for(int d=1;d<=4;d++)assertTrue(s.search(request(d,SearchControl.unlimited())).completed());
            assertArrayEquals(new int[]{4,85,201},w.calls.get(w.calls.size()-2));
            // Baseline recovery width16 doubles to32, while the unfailed upper endpoint stays201.
            assertArrayEquals(new int[]{4,18,201},w.calls.getLast());
            s.endRequest();
        }
    }
    static final long[] ROOT=Board.startingPosition();
    static SearchRequest request(int depth,SearchControl control) {
        return new SearchRequest(ROOT,GameHistory.initial(ROOT),depth,SearchObserver.NONE,control,false);
    }
    static SearchResult result(int depth,int score,long nodes,boolean completed) {
        return new SearchResult(1,true,score,depth,nodes,20,completed,new long[]{1});
    }
    static final class Windows implements SingleDepthSearch, AspirationSearch.WindowSearch {
        final List<int[]> calls=new ArrayList<>();final Deque<Integer> scores=new ArrayDeque<>();
        int begins,ends;boolean incomplete,stop;P4UpperWindowSearch reenter;
        Windows(int... values) { for(int v:values)scores.add(v); }
        public SearchResult search(SearchRequest r) { throw new AssertionError(); }
        public SearchResult search(SearchRequest r,int a,int b) {
            calls.add(new int[]{r.depth(),a,b});
            if(stop)r.control().request(SearchTermination.STOPPED);
            if(reenter!=null)assertThrows(IllegalStateException.class,()->reenter.search(r));
            return result(r.depth(),scores.removeFirst(),7,!incomplete);
        }
        public int maxSupportedDepth(){return 64;}
        public void beginRequest(){begins++;}public void endRequest(){ends++;}
    }
    @Test void carriesOnlyUpperWidthAcrossQuietStepPreservingBaselineAlpha() {
        var w=new Windows(0,100,100,101,101);
        try(var s=new P4UpperWindowSearch(w,w,null)) {
            s.beginRequest();
            for(int d=1;d<=4;d++)assertTrue(s.search(request(d,SearchControl.unlimited())).completed());
            // d2 needs recovery, d3 width200, d4 lower width16 and remembered upper width100.
            assertArrayEquals(new int[]{4,85,201},w.calls.getLast());
            s.endRequest();
            w.scores.add(0);s.beginRequest();s.search(request(1,SearchControl.unlimited()));
            assertArrayEquals(new int[]{1,-ExactSearch.INFINITY,ExactSearch.INFINITY},w.calls.getLast());s.endRequest();
            assertEquals(2,w.begins);assertEquals(2,w.ends);
        }
    }
    @Test void boundThenAbortCannotReplaceLastCompletedIterationAndCountsEveryPass() {
        var w=new Windows(0,30,30);int[] callbacks={0};
        var s=new P4UpperWindowSearch(w,(r,a,b)->{
            var result=w.search(r,a,b);if(w.calls.size()==3)return result(r.depth(),30,7,false);return result;
        },(d,a,b,r,n)->callbacks[0]++);
        try(var driver=new SearchDriver(s)) {
            var out=driver.search(request(3,SearchControl.unlimited()));
            assertEquals(1,out.lastCompletedResult().depth());assertEquals(21,out.nodes());
            assertTrue(out.iterationIncomplete());assertEquals(3,callbacks[0]);assertEquals(1,w.ends);
        }
    }
    @Test void hardStopAfterBoundPreventsRetryAndPublication() {
        var w=new Windows(0,40);var c=SearchControl.controlled(100,System.nanoTime(),Long.MAX_VALUE,TimeSource.SYSTEM);
        try(var s=new P4UpperWindowSearch(w,w,null)) {
            s.beginRequest();s.search(request(1,c));w.stop=true;
            var r=s.search(request(2,c));assertFalse(r.completed());assertFalse(r.hasMove());
            assertEquals(7,r.nodes());assertEquals(0,r.principalVariation().length);assertEquals(2,w.calls.size());s.endRequest();
        }
    }
    @Test void newlyFoundMateRequiresFullWindowAndRetainsRequestedDepth() {
        int mate=TranspositionScores.MATE_SCORE-3;var w=new Windows(0,mate,mate);
        try(var s=new P4UpperWindowSearch(w,w,null)) {
            s.beginRequest();s.search(request(1,SearchControl.unlimited()));
            var r=s.search(request(2,SearchControl.unlimited()));
            assertTrue(r.completed());assertEquals(2,r.depth());assertEquals(14,r.nodes());
            assertArrayEquals(new int[]{2,-ExactSearch.INFINITY,ExactSearch.INFINITY},w.calls.getLast());s.endRequest();
        }
    }
    @Test void reversedFailuresKeepOppositeEndpointAndEventuallyResolve() {
        var w=new Windows(0,40,-60,5);
        try(var s=new P4UpperWindowSearch(w,w,null)) {
            s.beginRequest();s.search(request(1,SearchControl.unlimited()));
            var r=s.search(request(2,SearchControl.unlimited()));
            assertTrue(r.completed());assertEquals(21,r.nodes());
            assertArrayEquals(new int[]{2,-16,72},w.calls.get(2));
            assertArrayEquals(new int[]{2,-124,72},w.calls.get(3));s.endRequest();
        }
    }
    @Test void boundedWidthAndReentry() {
        assertEquals(512,P4UpperWindowSearch.initialWidth(10000,512));
        assertEquals(16,P4UpperWindowSearch.initialWidth(0,16));
        var w=new Windows(0);
        try(var s=new P4UpperWindowSearch(w,w,null)) {
            w.reenter=s;s.beginRequest();assertThrows(IllegalStateException.class,s::beginRequest);
            s.search(request(1,SearchControl.unlimited()));s.endRequest();
        }
    }
}
