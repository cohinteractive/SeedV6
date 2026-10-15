package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

class FutilityPruningTest {
    static final SearchEvaluation EVAL=SearchEvaluation.handcrafted();
    static final String MIDDLE=SeePruningTest.MIDDLE;
    static ExactSearch engine(FutilityPolicy p,FutilityPolicy.Diagnostics d,TTable t) {
        return ExactSearch.iterationReuseResearch(ExactEvaluator.from(EVAL),t,IterationReuse.EFFORT,true,null)
                .withSeeResearch(SeePolicy.QXRAY,null).withFutilityResearch(p,d);
    }
    @Test void everyOmissionHasLegalProtectedDomainAndConservativeProof() {
        for(var policy:FutilityPolicy.values())if(policy!=FutilityPolicy.OFF){
            var d=new FutilityPolicy.Diagnostics(){
                public void omitted(long[] b,long[] c,long m,int ply,int depth,int n,int q,int a,int z,int e){
                    assertTrue(ply>0&&n>0&&q<=n);assertTrue(policy.considers(depth,q));
                    assertFalse(QuiescenceOracle.inCheck(b));assertFalse(QuiescenceOracle.inCheck(c));
                    assertFalse(QuiescenceOracle.tactical(b,m));
                    assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(b)).anyMatch(x->x==m));
                    assertTrue(Long.bitCount(b[0]|b[1]|b[2])>6);assertTrue(a>=0&&z<=ExactSearch.MAX_STATIC_SCORE);
                    assertTrue(e+policy.margin(depth)<=a);
                    if(policy==FutilityPolicy.CHILD && omissions<=64) {
                        var fresh=ExactEvaluator.from(EVAL);fresh.initialize(c);
                        assertEquals(-fresh.evaluate(c,0),e);
                        var leaf=ExactSearch.quiescenceResearch(ExactEvaluator.from(EVAL))
                                .search(c,GameHistory.initial(c),0,()->false);
                        assertTrue(leaf.completed());assertTrue(-leaf.score()<=a);
                    }
                    assertTrue(Board.halfMoveClock((int)c[Board.STATUS])+depth-1<100);
                }
            };
            var table=new TTable(1);var s=engine(policy,d,table);var b=Board.fromFen(MIDDLE);var h=GameHistory.initial(b);
            var result=s.search(b,h,6,()->s.visitedNodes()>3_000_000);
            assertTrue(result.completed(),policy.toString());assertTrue(result.selective());
            assertTrue(d.omissions>0,policy.toString());assertTrue(d.evaluations<=d.candidates);
            Phase2QuiescenceTest.legalPv(b,result.principalVariation());
            assertFalse(table.probe(SearchKey.key(b,SearchKey.rootHistory(b,h)),new TTable.TEntry()));
        }
    }
    @Test void offMatchesCumulativeTreeAndIndependentReferenceIsNotConfigured() {
        var b=Board.fromFen(MIDDLE);var h=GameHistory.initial(b);
        var off=engine(FutilityPolicy.OFF,null,new TTable(1)).search(b,h,5,()->false);
        var plain=ExactSearch.iterationReuseResearch(ExactEvaluator.from(EVAL),new TTable(1),IterationReuse.EFFORT,true,null)
                .withSeeResearch(SeePolicy.QXRAY,null).search(b,h,5,()->false);
        assertEquals(plain.score(),off.score());assertEquals(plain.nodes(),off.nodes());assertEquals(plain.qnodes(),off.qnodes());
        assertArrayEquals(plain.principalVariation(),off.principalVariation());
        assertThrows(IllegalStateException.class,()->new ExactSearch().withFutilityResearch(FutilityPolicy.LEAF,null));
        assertThrows(IllegalArgumentException.class,()->ExactSearchAdapter.futilityResearch(EVAL,FutilityPolicy.LEAF,null));
        assertThrows(IllegalStateException.class,()->engine(FutilityPolicy.LEAF,null,null).withSeeResearch(SeePolicy.OFF,null));
    }
    @Test void terminalDrawMateAndSparseDomainsRemainProtected() {
        for(String fen:new String[]{"7k/6Q1/6K1/8/8/8/8/8 b - - 0 1","7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                "8/8/2P5/8/4k2K/8/8/8 w - - 99 12",MIDDLE.replace("0 1","100 1")}){
            var b=Board.fromFen(fen);var h=GameHistory.initial(b);var expected=engine(FutilityPolicy.OFF,null,null).search(b,h,3,()->false);
            for(var p:FutilityPolicy.values()){
                var d=new FutilityPolicy.Diagnostics();var actual=engine(p,d,null).search(b,h,3,()->false);
                assertTrue(actual.completed());assertEquals(expected.score(),actual.score());assertEquals(0,d.omissions);
                Phase2QuiescenceTest.legalPv(b,actual.principalVariation());
            }
        }
        var b=Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 8 5");
        var h=GameHistory.builder(b).appendPosition(b).appendPosition(b).snapshot();
        for(var p:FutilityPolicy.values())assertEquals(0,engine(p,null,null).search(b,h,5,()->false).score());
    }
    @Test void childStaticNeverEvaluatesAfterCancelledPreparation() {
        var sink=new FutilityPolicy.Diagnostics();var cancelled=new boolean[1];
        var delegate=ExactEvaluator.from(EVAL);
        var guarded=new ExactEvaluator(){
            public void initialize(long[] b){delegate.initialize(b);}
            public void child(long[] b,long[] c,int p){
                if(sink.candidates>0){cancelled[0]=true;return;}
                delegate.child(b,c,p);
            }
            public int evaluate(long[] b,int p){assertFalse(cancelled[0]);return delegate.evaluate(b,p);}
        };
        var search=ExactSearch.iterationReuseResearch(guarded,new TTable(1),IterationReuse.EFFORT,true,null)
                .withSeeResearch(SeePolicy.QXRAY,null).withFutilityResearch(FutilityPolicy.CHILD,sink);
        var board=Board.fromFen(MIDDLE);
        var result=search.search(board,GameHistory.initial(board),6,()->cancelled[0]);
        assertTrue(cancelled[0]);assertTrue(sink.candidates>0);assertFalse(result.completed());
    }
    @Test void interruptionAndRequestReuseDoNotLeakFutilityState() {
        var b=Board.fromFen(MIDDLE);var h=GameHistory.initial(b);
        for(var p:FutilityPolicy.values()){
            var s=engine(p,new FutilityPolicy.Diagnostics(),new TTable(1));
            assertFalse(s.search(b,h,12,()->s.visitedNodes()>=1000).completed());
            var after=s.search(b,h,4,()->false);var fresh=engine(p,null,new TTable(1)).search(b,h,4,()->false);
            assertEquals(fresh.score(),after.score());assertArrayEquals(fresh.principalVariation(),after.principalVariation());
        }
    }
    @Test void allWorkerCountsHavePrivatePolicyAndDrainedReuse() {
        var b=Board.fromFen(MIDDLE);var h=GameHistory.initial(b);
        for(int workers=1;workers<=16;workers++){
            var sinks=new FutilityPolicy.Diagnostics[workers];
            try(var owner=new ParallelSearch(workers,EVAL,new TTable(1),ConcurrencyPolicy.ROOT_REUSE)
                    .withSeeResearch(SeePolicy.QXRAY,null).withFutilityResearch(FutilityPolicy.SHALLOW,i->sinks[i]=new FutilityPolicy.Diagnostics());
                var driver=new SearchDriver(owner)){
                var r=driver.search(new SearchRequest(b,h,7,SearchControl.controlled(500_000,0,-1,TimeSource.SYSTEM)));
                assertNotNull(r.lastCompletedResult());assertEquals(0,owner.activeSearchThreads());
                assertEquals(workers,new HashSet<>(Arrays.asList(sinks)).size());
                assertTrue(Arrays.stream(sinks).mapToLong(x->x.omissions).sum()>0);
                if(workers>1)assertTrue(Arrays.stream(sinks).skip(1).mapToLong(x->x.omissions).sum()>0);
                Phase2QuiescenceTest.legalPv(b,r.lastCompletedResult().principalVariation());
                driver.newGame();assertTrue(driver.search(new SearchRequest(b,h,2)).targetDepthCompleted());
            }
        }
    }
}
