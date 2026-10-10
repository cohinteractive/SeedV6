package com.ohinteractive.seedv6.search.driver;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.tt.*;
import static org.junit.jupiter.api.Assertions.*;

class AspirationSearchTest {
    @Test void directObserverReceivesOnlyTheResolvedOrIncompleteAggregate() {
        for(boolean cancel : new boolean[]{false,true}) {
            var published = new ArrayList<SearchResult>();
            int[] attempts = {0};
            var control = SearchControl.controlled(-1,System.nanoTime(),-1,TimeSource.SYSTEM);
            var search = new AspirationSearch(new ExactSearchAdapter((b,p)->40,new TTable(1)),
                    AspirationSearch.Policy.ONE16,(d,a,b,r,n)->{
                        if(d==2) {
                            attempts[0]++;
                            if(cancel) control.request(SearchTermination.STOPPED);
                        }
                    });
            search.beginRequest();
            try {
                search.search(request(1,control));
                var board = Board.startingPosition();
                var result = search.search(new SearchRequest(board,GameHistory.initial(board),2,
                        new SearchObserver() {
                            public void onSearchFinished(SearchResult r,long elapsed) { published.add(r); }
                        },control,true));
                assertEquals(cancel?1:2,attempts[0]);
                assertEquals(1,published.size());
                assertSame(result,published.getFirst());
                assertEquals(!cancel,result.completed());
                if(!cancel) assertEquals(40,result.score());
                else assertEquals(0,result.principalVariation().length);
            } finally { search.endRequest(); search.close(); }
        }
    }
    @Test void unbracketedSearchesCannotConsumeScoresFromAnotherRequest() {
        var windows=new ArrayList<int[]>();
        var search=new AspirationSearch(new ExactSearchAdapter((b,p)->0,new TTable(1)),
                AspirationSearch.Policy.ONE16,(d,a,b,r,n)->windows.add(new int[]{a,b}));
        search.search(new SearchRequest(Board.startingPosition(),1));
        search.search(new SearchRequest(Board.startingPosition(),2));
        assertEquals(2,windows.size());
        for(var w:windows) assertArrayEquals(new int[]{-ExactSearch.INFINITY,ExactSearch.INFINITY},w);
    }
    static SearchRequest request(int depth, SearchControl control) {
        var b = Board.startingPosition();
        return new SearchRequest(b, GameHistory.initial(b), depth, SearchObserver.NONE, control, true);
    }
    @Test void successfulIterationMayFinishOnLastAdmittedNode() {
        long budget;
        try(var driver=new SearchDriver(new AspirationSearch(
                new ExactSearchAdapter((b,p)->0,new TTable(1)),AspirationSearch.Policy.ONE16))) {
            budget=driver.search(request(2,SearchControl.unlimited())).nodes();
        }
        try(var driver=new SearchDriver(new AspirationSearch(
                new ExactSearchAdapter((b,p)->0,new TTable(1)),AspirationSearch.Policy.ONE16))) {
            var r=driver.search(request(2,SearchControl.controlled(budget,System.nanoTime(),-1,TimeSource.SYSTEM)));
            assertTrue(r.targetDepthCompleted()); assertEquals(2,r.lastCompletedResult().depth());
            assertEquals(budget,r.nodes()); assertEquals(budget,r.diagnostics().totalEnteredNodes());
        }
    }
    @Test void deadlineAndInterruptAfterInvocationCannotPublishResolvedOrFailedDepth() {
        for(boolean interruption : new boolean[]{false,true}) for(int value : new int[]{0,40}) {
            long[] now={0};
            var control=SearchControl.controlled(-1,0,100,()->now[0]);
            var search=new AspirationSearch(new ExactSearchAdapter((b,p)->value,new TTable(1)),
                    AspirationSearch.Policy.WIDEN16,(d,a,b,r,n)->{
                        if(d==2) { if(interruption) Thread.currentThread().interrupt(); else now[0]=100; }
                    });
            try(var driver=new SearchDriver(search)) {
                var r=driver.search(request(3,control));
                assertEquals(1,r.lastCompletedResult().depth()); assertTrue(r.iterationIncomplete());
                assertEquals(interruption?SearchTermination.STOPPED:SearchTermination.TIME_LIMIT,control.termination());
            } finally { Thread.interrupted(); }
        }
    }
    @Test void exactControlsMatchIndependentTtOffAtEveryCompletedDepth() {
        for(var policy : AspirationSearch.Policy.values()) for(String fen : List.of(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")) {
            var b = Board.fromFen(fen);
            var adapter = new ExactSearchAdapter((board, ply) -> Eval.evaluate(board), new TTable(1));
            try(var driver = new SearchDriver(new AspirationSearch(adapter, policy))) {
                var outcome = driver.search(new SearchRequest(b, 4, new SearchObserver() {
                    public void onIterationCompleted(IterationSnapshot r) {
                        assertEquals(new ExactSearch().search(b, r.depth()).score(), r.score());
                    }
                }));
                assertTrue(outcome.targetDepthCompleted());
            }
        }
    }
    @Test void equalityFailsAndEachInvocationStoresItsOwnOriginalBound() {
        for(int value : new int[]{8,-8}) {
            var types = new ArrayList<Integer>(); int[] generations={0};
            var b=Board.startingPosition();
            var table = new TTable(1) {
                public void advanceGeneration() { generations[0]++; super.advanceGeneration(); }
                public void save(long k,int d,int type,int score,long move) {
                    if(d==2) types.add(type);
                    super.save(k,d,type,score,move);
                }
            };
            var adapter=new ExactSearchAdapter((board, ply)->value, table);
            try(var driver=new SearchDriver(new AspirationSearch(adapter, AspirationSearch.Policy.ONE16))) {
                var r=driver.search(request(2,SearchControl.unlimited()));
                assertEquals(value,r.lastCompletedResult().score());
                assertEquals(1,r.diagnostics().iteration().aspirationAttempts());
                assertEquals(1,r.diagnostics().iteration().fullWindowFallbacks());
                assertEquals(r.nodes(),r.diagnostics().totalEnteredNodes());
                assertEquals(1,generations[0]);
                assertTrue(types.contains(value>0?TTable.TYPE_LOWER:TTable.TYPE_UPPER));
                assertEquals(TTable.TYPE_EXACT,types.getLast());
            }
        }
    }
    @Test void cancelledRetryOrNarrowCannotPublishAndOwnerCanBeReused() {
        for(int abort : new int[]{2,3}) {
            int[] calls={0}; var control=SearchControl.controlled(-1,System.nanoTime(),-1,TimeSource.SYSTEM);
            var adapter=new ExactSearchAdapter(new ExactEvaluator() {
                public void initialize(long[] b) { calls[0]++; }
                public int evaluate(long[] b,int ply) {
                    if(calls[0]==abort) control.request(SearchTermination.STOPPED);
                    return 40;
                }
            },new TTable(1));
            try(var driver=new SearchDriver(new AspirationSearch(adapter,AspirationSearch.Policy.ONE16))) {
                var r=driver.search(request(3,control));
                assertTrue(r.iterationIncomplete()); assertEquals(1,r.lastCompletedResult().depth());
                assertEquals(r.nodes(),r.diagnostics().totalEnteredNodes());
                calls[0]=10;
                assertTrue(driver.search(request(3,SearchControl.unlimited())).targetDepthCompleted());
            }
        }
    }
    @Test void exactNodeExhaustionAfterFailedAttemptRetainsPreviousAndCountsEverything() {
        var costs=new ArrayList<Long>();
        var first=new AspirationSearch(new ExactSearchAdapter((b,p)->40,new TTable(1)),
                AspirationSearch.Policy.ONE16,(d,a,b,r,n)->costs.add(r.nodes()));
        new SearchDriver(first).search(request(2,SearchControl.unlimited()));
        long budget=costs.get(0)+costs.get(1);
        try(var driver=new SearchDriver(new AspirationSearch(
                new ExactSearchAdapter((b,p)->40,new TTable(1)),AspirationSearch.Policy.ONE16))) {
            var r=driver.search(request(2,SearchControl.controlled(budget,System.nanoTime(),-1,TimeSource.SYSTEM)));
            assertEquals(budget,r.nodes()); assertEquals(1,r.lastCompletedResult().depth());
            assertTrue(r.iterationIncomplete()); assertEquals(0,r.diagnostics().iteration().fullWindowFallbacks());
            assertEquals(0,r.diagnostics().iteration().failHighResearches());
            assertEquals(0,r.diagnostics().iteration().failLowResearches());
        }
    }
    @Test void mateEndpointsAndNewMateForceFullWindowWithoutOverflow() {
        int max=TranspositionScores.MAX_NORMAL_SCORE;
        assertEquals(max+1,AspirationSearch.upper(max,512));
        assertEquals(-max-1,AspirationSearch.lower(-max,512));
        var seen=new ArrayList<int[]>();
        SingleDepthSearch owner=new ExactSearchAdapter((b,p)->0,null);
        var search=new AspirationSearch(owner,(r,a,b)->{
            seen.add(new int[]{a,b});
            return new SearchResult(0,false,r.depth()==1?0:TranspositionScores.MATE_SCORE-3,
                    r.depth(),0,0,true);
        },AspirationSearch.Policy.WIDEN16,null);
        search.beginRequest();
        search.search(new SearchRequest(Board.startingPosition(),1));
        assertTrue(search.search(new SearchRequest(Board.startingPosition(),2)).completed());
        search.search(new SearchRequest(Board.startingPosition(),3)); search.endRequest();
        assertEquals(4,seen.size());
        assertArrayEquals(new int[]{-16,16},seen.get(1));
        assertArrayEquals(new int[]{-ExactSearch.INFINITY,ExactSearch.INFINITY},seen.get(2));
        assertArrayEquals(seen.get(2),seen.get(3));
    }
    @Test void alternatingFailuresAreBoundedAndSameDepthAndNeverShrinkUnfailedSide() {
        var seen=new ArrayList<int[]>();
        var owner=new ExactSearchAdapter((b,p)->0,null);
        var search=new AspirationSearch(owner,(r,a,b)->{
            seen.add(new int[]{r.depth(),a,b});
            int score=r.depth()==1||a==-ExactSearch.INFINITY?0:seen.size()%2==0?b:a;
            return new SearchResult(0,false,score,r.depth(),0,0,true);
        },AspirationSearch.Policy.WIDEN16,null);
        search.beginRequest();
        search.search(new SearchRequest(Board.startingPosition(),1));
        assertTrue(search.search(new SearchRequest(Board.startingPosition(),2)).completed()); search.endRequest();
        assertEquals(6,seen.size());
        for(int i=1;i<seen.size();i++) assertEquals(2,seen.get(i)[0]);
        for(int i=2;i<5;i++) {
            assertTrue(seen.get(i)[1]<=seen.get(i-1)[1]);
            assertTrue(seen.get(i)[2]>=seen.get(i-1)[2]);
        }
        assertEquals(-ExactSearch.INFINITY,seen.getLast()[1]);
    }
}
