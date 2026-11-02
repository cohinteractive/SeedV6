package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class P4LearningTest {
    static FlatSearchConfig config() {return P4LearningStudy.flags("retain",1);}
    static ExactSearch kernel() {return ExactSearch.constructionResearch(
            ExactEvaluator.from(SearchEvaluation.handcrafted()),new TTable(1),config());}
    static int[] quiet(ExactSearch k) throws Exception {return (int[])P4TtFoundationStudy.field(k,"quietHistory");}
    static FlatSearchState state(ExactSearch k) throws Exception {return (FlatSearchState)P4TtFoundationStudy.field(k,"constructionState");}

    @Test void learnedLabelsSurviveRepeatedWindowsButNotRequestBoundaries() throws Exception {
        var k=kernel();var b=Board.startingPosition();var saved=b.clone();
        k.beginRequest();
        assertTrue(k.search(b,4).completed());
        assertTrue(Arrays.stream(quiet(k)).anyMatch(x->x!=0));
        // Unused moving-piece code0 witnesses reset independently of Search updates.
        quiet(k)[0]=317;
        assertTrue(k.searchWindow(b,GameHistory.initial(b),4,1000,1001,()->false).completed());
        assertEquals(317,quiet(k)[0]);
        assertTrue(k.searchWindow(b,GameHistory.initial(b),4,-1000,1000,()->false).completed());
        assertEquals(317,quiet(k)[0]);assertArrayEquals(saved,b);
        k.endRequest();assertTrue(Arrays.stream(quiet(k)).allMatch(x->x==0));
        assertTrue(Arrays.stream(state(k).captures).allMatch(x->x==0));
        for(short x:state(k).continuation)assertEquals(0,x);
        k.beginRequest();assertEquals(0,quiet(k)[0]);k.endRequest();k.newGame();
    }
    @Test void abortedInvocationDiscardsLearningAndAllowsReuse() throws Exception {
        var k=kernel();var b=Board.startingPosition();k.beginRequest();
        assertTrue(k.search(b,3).completed());quiet(k)[0]=100;
        var aborted=k.search(b,GameHistory.initial(b),8,()->true);
        assertFalse(aborted.completed());assertEquals(0,quiet(k)[0]);
        assertTrue(Arrays.stream(state(k).captures).allMatch(x->x==0));
        assertTrue(k.search(b,3).completed());k.endRequest();
    }
    @Test void retainedTablesDoNotRetainPathEvaluationOrExtensionScratch() {
        var s=new FlatSearchState(config());s.captures[0]=19;s.continuation[0]=7;
        s.killers[2]=111;s.counters[0]=222;s.pathMoves[2]=333;s.rawEval[2]=42;s.credits[2]=1;
        s.reset(true);
        assertEquals(19,s.captures[0]);assertEquals(7,s.continuation[0]);
        assertEquals(111,s.killers[2]);assertEquals(222,s.counters[0]);
        assertEquals(0,s.pathMoves[2]);assertEquals(Integer.MIN_VALUE,s.rawEval[2]);assertEquals(0,s.credits[2]);
        s.reset();assertEquals(0,s.captures[0]);assertEquals(0,s.continuation[0]);
    }
    @Test void rootReuseKeepsWorkersPrivateAndRootOrderingDoesNotEraseLearning() throws Exception {
        var a=kernel();var b=kernel();a.beginSharedRequest(1);b.beginSharedRequest(1);
        a.prepareRootIteration(new IterationReuse(IterationReuse.EFFORT,null));
        b.prepareRootIteration(new IterationReuse(IterationReuse.EFFORT,null));
        quiet(a)[0]=12;quiet(b)[0]=34;state(a).captures[0]=56;state(b).captures[0]=78;
        a.orderedRootMoves(Board.startingPosition(),0);assertEquals(12,quiet(a)[0]);assertEquals(56,state(a).captures[0]);
        b.seedRootHistoryFrom(a);assertEquals(34,quiet(b)[0]);assertEquals(78,state(b).captures[0]);
        assertNotSame(quiet(a),quiet(b));assertNotSame(state(a).continuation,state(b).continuation);
        a.endRequest();b.endRequest();
    }
    @Test void productionAndIndependentReferenceConfigurationsStayUnchanged() {
        assertEquals(3407871L,BaselineEngine.configuration(1,64).mechanisms().flags());
        assertEquals(3407871L,P4LearningStudy.flags("control",4).flags());
        assertFalse(FlatSearchConfig.exact().has(REQUEST_LEARNING));
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(REQUEST_LEARNING,true));
    }
    @Test void requestsPreserveFullAccountingBoardPvCancellationAndDrain() {
        var b=Board.startingPosition();var saved=b.clone();
        for(String profile:List.of("retain","guided"))for(int workers:new int[]{1,4})try(var driver=new SearchDriver(
                new ConstructionSearch(P4LearningStudy.flags(profile,workers),workers,1,AspirationSearch.Policy.ADAPTIVE16).create(SearchEvaluation.handcrafted()))) {
            for(int budget:new int[]{1,100,5000,20000}) {
                var c=SearchControl.controlled(budget,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var r=driver.search(new SearchRequest(b,GameHistory.initial(b),20,SearchObserver.NONE,c,true));
                assertEquals(budget,r.nodes());assertEquals(r.nodes(),r.diagnostics().totalEnteredNodes());
                assertFalse(r.targetDepthCompleted());assertEquals(0,driver.activeSearchThreads());assertArrayEquals(saved,b);
                if(r.lastCompletedResult()!=null)Phase2Oracle.legalPv(b,r.lastCompletedResult().principalVariation());
                driver.newGame();
            }
            assertTrue(driver.search(new SearchRequest(b,3)).targetDepthCompleted());
        }
    }
    @Test void effortIsBoundedMonotoneAndNeverCreatesAnExtension() {
        var s=new FlatSearchState(P4LearningStudy.flags("guided",1));
        var b=Board.startingPosition();long move=ExhaustiveOracle.legalMoves(b)[0];
        var h=new int[QuietHistory.SIZE];int index=QuietHistory.index(move);
        for(int depth=3;depth<=16;depth++) {
            int prior=depth;
            for(int evidence=-QuietHistory.LIMIT;evidence<=QuietHistory.LIMIT;evidence++) {
                h[index]=evidence;int r=s.historyReduction(1,depth,1,move,h);
                assertTrue(r>=0 && r<=depth-2);assertTrue(r<=prior);assertTrue(Math.abs(r-1)<=2);prior=r;
            }
        }
        h[index]=0;assertEquals(1,s.historyReduction(1,8,1,move,h));
        h[index]=32;assertEquals(0,s.historyReduction(1,8,1,move,h));
        h[index]=-32;assertEquals(2,s.historyReduction(1,8,1,move,h));
        assertThrows(IllegalArgumentException.class,()->BaselineEngine.configuration(1,64).mechanisms().with(HISTORY_EFFORT,true));
    }
}
