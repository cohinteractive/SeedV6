package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.driver.MtdDriverResearch.Policy;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(180)
class Sr004IntegratedTest {
    @Test void tacticalUnprunedScoresAndPvWitnessesMatchIndependentTtOffQsearch() {
        var e=SearchEvaluation.handcrafted();ExactEvaluator hce=(b,p)->Eval.evaluate(b);
        for(var fixture:QuiescenceSeeCorpus.FIXTURES) {
            var b=Board.fromFen(fixture.fen());
            var expected=ExactSearch.quiescenceResearch(hce).search(b,2);
            assertTrue(expected.completed());
            for(var policy:List.of(Policy.PREV,Policy.BRACKET,Policy.TWO_PASS)) {
                var owner=ExactSearchAdapter.unprunedQuiescence(e).withFlatSearchResearch(true);
                try(var driver=new SearchDriver(new MtdDriverResearch(owner,policy,null))) {
                    var r=driver.search(new SearchRequest(b,2)).lastCompletedResult();
                    assertEquals(expected.score(),r.score(),fixture.name()+policy);
                    Phase2Oracle.legalPv(b,r.principalVariation());
                    // Independent TT-off searched first move must attain the returned value.
                    if(r.hasMove()) {
                        var child=ExhaustiveOracle.child(b,r.bestMove());
                        int childScore=ExactSearch.quiescenceResearch(hce).search(child,1).score();
                        if(Math.abs(childScore)>=32512)childScore+=childScore>0?-1:1;
                        assertEquals(r.score(),-childScore,fixture.name()+policy+" PV witness");
                    }
                }
            }
        }
    }

    @Test void unprunedFlattenedDriversMatchIndependentEnumerationAndRecursiveReferences() {
        var e=SearchEvaluation.handcrafted();
        ExactEvaluator hce=(b,p)->Eval.evaluate(b);
        for(String fen:List.of("4k3/8/8/3q4/4P3/8/8/4K3 w - - 98 1",
                "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1",
                "8/k1P5/2K5/8/8/8/8/8 w - - 98 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5Q2/6K1/8/8/8/8/8 b - - 100 1")) {
            var board=Board.fromFen(fen);var history=GameHistory.initial(board);
            int expected=new Phase2Oracle.RestrictedOracle(hce,0).score(board,new SearchLineHistory(history),3,0,0,-1);
            assertEquals(expected,ExactSearch.quiescenceResearch(hce).search(board,history,3,()->false).score());
            for(var policy:Policy.values()) {
                var owner=ExactSearchAdapter.unprunedQuiescence(e).withFlatSearchResearch(true);
                try(var driver=new SearchDriver(new MtdDriverResearch(owner,policy,null))) {
                    var r=driver.search(new SearchRequest(board,history,3));
                    assertTrue(r.targetDepthCompleted());assertEquals(expected,r.lastCompletedResult().score(),fen+policy);
                    Phase2Oracle.legalPv(board,r.lastCompletedResult().principalVariation());
                }
            }
        }
    }

    @Test void policy4AccountingCancellationAndReuseAtSupportedWorkerExtremes() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var b=Board.startingPosition();var original=b.clone();
        for(int workers:new int[]{1,2,4,16}) for(var policy:List.of(Policy.PREV,Policy.BRACKET,Policy.TWO_PASS)) {
            var raw=Sr002ExecutionStudy.engine(workers,e,2);
            var mtd=raw instanceof ExactSearchAdapter a?new MtdDriverResearch(a,policy,null):new MtdDriverResearch((ParallelSearch)raw,policy,null);
            try(var driver=new SearchDriver(mtd)) {
                for(int budget:new int[]{1,100,3000}) {
                    var c=SearchControl.controlled(budget,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                    var r=driver.search(new SearchRequest(b,GameHistory.initial(b),20,SearchObserver.NONE,c,true));
                    assertEquals(budget,r.nodes());assertEquals(r.nodes(),r.diagnostics().totalEnteredNodes());
                    assertFalse(r.targetDepthCompleted());assertEquals(0,driver.activeSearchThreads());
                    if(r.lastCompletedResult()!=null) Phase2Oracle.legalPv(b,r.lastCompletedResult().principalVariation());
                    assertArrayEquals(original,b);driver.newGame();
                }
                var expired=SearchControl.controlled(10000,System.nanoTime()-1_000_000_000L,1,TimeSource.SYSTEM);
                assertNull(driver.search(new SearchRequest(b,GameHistory.initial(b),4,expired)).lastCompletedResult());
                assertEquals(SearchTermination.TIME_LIMIT,expired.termination());
                assertTrue(driver.search(new SearchRequest(b,3)).targetDepthCompleted());
                assertEquals(0,driver.activeSearchThreads());
            }
        }
    }

    @Test void parallelNarrowBoundsAndMaterializationAgreeWithSparseIndependentOracle() throws Exception {
        var e=P3InformationBaselineTest.evaluation();
        for(String fen:List.of("4k3/8/8/3q4/4P3/8/8/4K3 w - - 98 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1","8/k1P5/2K5/8/8/8/8/8 w - - 98 1")) {
            var b=Board.fromFen(fen);var history=GameHistory.initial(b);var state=e.newState(1);
            ExactEvaluator refreshed=(board,p)->{state.initialize(board,0);return state.evaluate(board,0);};
            int expected=new Phase2Oracle.RestrictedOracle(refreshed,0).score(b,new SearchLineHistory(history),3,0,0,-1);
            for(int count:new int[]{2,4}) try(var owner=new ParallelSearch(count,e,new TTable(1),ConcurrencyPolicy.ROOT_REUSE).withFlatSearchResearch(true)) {
                owner.beginRequest();
                try {
                    for(int beta:new int[]{-32768,-32512,-1,0,1,32512,32768}) {
                        var r=owner.searchWindow(new SearchRequest(b,history,3),beta-1,beta);
                        assertTrue(r.completed());assertEquals(expected>=beta,r.score()>=beta,fen+beta);
                        if(r.score()>=beta) assertTrue(r.score()<=expected);else assertTrue(r.score()>=expected);
                        Phase2Oracle.legalPv(b,r.principalVariation());assertEquals(0,owner.activeSearchThreads());
                    }
                    var r=owner.searchWindow(new SearchRequest(b,history,3),expected-1,expected+1);
                    assertTrue(r.completed());assertEquals(expected,r.score());
                } finally {owner.endRequest();}
                try(var driver=new SearchDriver(new MtdDriverResearch(owner,Policy.BRACKET,null))) {
                    var r=driver.search(new SearchRequest(b,history,3));
                    assertTrue(r.targetDepthCompleted());assertEquals(expected,r.lastCompletedResult().score());
                }
            }
        }
    }

    @Test void actualNarrowWorkerFailureDrainsAndAllowsReuse() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var fail=new AtomicBoolean(true);
        var owner=new ParallelSearch(4,e,new TTable(1),ConcurrencyPolicy.ROOT_REUSE,index->{
            var delegate=ExactEvaluator.from(e);
            return new ExactEvaluator() {
                public void initialize(long[] b){delegate.initialize(b);}
                public void child(long[] p,long[] c,int ply){delegate.child(p,c,ply);}
                public int evaluate(long[] b,int ply){if(ply>=4 && fail.get())throw new IllegalStateException("injected narrow failure");return delegate.evaluate(b,ply);}
            };
        }).withSeeResearch(SeePolicy.QXRAY,null).withCompletedHints(true,null).withFlatSearchResearch(true);
        try(owner) {
            owner.beginRequest();
            try {assertThrows(IllegalStateException.class,()->owner.searchWindow(new SearchRequest(Board.startingPosition(),6),-1,0));}
            finally {owner.endRequest();}
            assertEquals(0,owner.activeSearchThreads());fail.set(false);owner.newGame();
            assertTrue(owner.searchWindow(new SearchRequest(Board.startingPosition(),3),-1,0).completed());
        }
    }
}
