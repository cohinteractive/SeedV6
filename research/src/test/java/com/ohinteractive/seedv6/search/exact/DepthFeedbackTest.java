package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

@Timeout(120)
class DepthFeedbackTest {
    static ExactSearch engine(DepthFeedback p,DepthFeedback.Diagnostics d,TTable table) {
        return ExactSearch.iterationReuseResearch(ExactEvaluator.from(SearchEvaluation.handcrafted()),table,IterationReuse.EFFORT,true,null)
                .withSeeResearch(SeePolicy.QXRAY,null).withDepthFeedback(p,d);
    }
    static Object get(Object s,String n)throws Exception{Field f=s.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(s);}
    static boolean empty(byte[] table){for(byte v:table)if(v!=0)return false;return true;}
    @Test void offTreeAndReferenceRemainIndependent() {
        var board=Board.fromFen(SeePruningTest.MIDDLE);
        var plain=ExactSearch.iterationReuseResearch(ExactEvaluator.from(SearchEvaluation.handcrafted()),new TTable(1),IterationReuse.EFFORT,true,null).withSeeResearch(SeePolicy.QXRAY,null).search(board,5);
        var off=engine(DepthFeedback.OFF,null,new TTable(1)).search(board,5);
        assertEquals(plain.score(),off.score());assertEquals(plain.nodes(),off.nodes());assertEquals(plain.qnodes(),off.qnodes());assertArrayEquals(plain.principalVariation(),off.principalVariation());
        assertThrows(IllegalStateException.class,()->new ExactSearch().withDepthFeedback(DepthFeedback.MOVE,null));
        assertThrows(IllegalArgumentException.class,()->ExactSearchAdapter.depthFeedbackResearch(SearchEvaluation.handcrafted(),DepthFeedback.MOVE,null));
        assertThrows(IllegalStateException.class,()->engine(DepthFeedback.MOVE,null,null).withSeeResearch(SeePolicy.OFF,null));
        assertThrows(IllegalStateException.class,()->engine(DepthFeedback.CAUTION,null,null).withFutilityResearch(FutilityPolicy.SHALLOW,null));
    }
    @Test void learningRequiresNominalObservationAndClearsOnRequestEnd() throws Exception {
        var board=Board.fromFen(SeePruningTest.MIDDLE);var history=GameHistory.initial(board);
        for(var policy:List.of(DepthFeedback.MOVE,DepthFeedback.CAUTION)) {
            var d=new DepthFeedback.Diagnostics();var table=new TTable(1);var s=engine(policy,d,table);
            var lmr=new LmrDiagnostics();Field f=ExactSearch.class.getDeclaredField("lmrDiagnostics");f.setAccessible(true);f.set(s,lmr);
            s.beginRequest();
            try {
                for(int depth=1;depth<=8;depth++) {
                    long before=d.verified,bypass=d.bypassed;
                    var r=s.search(board,history,depth,()->false);assertTrue(r.completed());Phase2Oracle.legalPv(board,r.principalVariation());
                    assertEquals(lmr.verifications+d.bypassed-bypass,d.verified-before);
                    if(r.selective())assertFalse(table.probe(SearchKey.key(board,SearchKey.rootHistory(board,history)),new TTable.TEntry()));
                }
                assertTrue(d.verified>0);assertTrue(d.adjusted>0,policy.toString());assertEquals(d.verified,d.confirmed+d.refuted);
                if(policy==DepthFeedback.MOVE)assertFalse(empty((byte[])get(s,"feedbackMoves")));
                assertThrows(IllegalStateException.class,()->s.withDepthFeedback(DepthFeedback.OFF,null));
            } finally {s.endRequest();}
            if(policy==DepthFeedback.MOVE)assertTrue(empty((byte[])get(s,"feedbackMoves")));
            s.newGame();var repeat=s.search(board,4);var fresh=engine(policy,null,new TTable(1)).search(board,4);
            assertEquals(fresh.score(),repeat.score());assertEquals(fresh.nodes(),repeat.nodes());assertArrayEquals(fresh.principalVariation(),repeat.principalVariation());
        }
    }
    @Test void abortAfterReachedFeedbackAndReuseAreHonest() throws Exception {
        var board=Board.fromFen(SeePruningTest.MIDDLE);var original=board.clone();
        for(var policy:List.of(DepthFeedback.MOVE,DepthFeedback.CAUTION)) {
            var d=new DepthFeedback.Diagnostics();var s=engine(policy,d,new TTable(1));
            var r=s.search(board,GameHistory.initial(board),8,()->d.adjusted>0);
            assertTrue(d.adjusted>0);assertFalse(r.completed());assertEquals(0,r.principalVariation().length);assertArrayEquals(original,board);
            if(policy==DepthFeedback.MOVE)assertTrue(empty((byte[])get(s,"feedbackMoves")));
            s.newGame();var after=s.search(board,4);var fresh=engine(policy,null,new TTable(1)).search(board,4);
            assertEquals(fresh.nodes(),after.nodes());assertEquals(fresh.score(),after.score());assertArrayEquals(fresh.principalVariation(),after.principalVariation());
        }
    }
    @Test void terminalRulesRetainScoresAndNoFeedback() {
        for(String fen:List.of("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1","7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",SeePruningTest.MIDDLE.replace("0 1","100 1"))) {
            var b=Board.fromFen(fen);var expected=engine(DepthFeedback.OFF,null,null).search(b,4);
            for(var p:DepthFeedback.values()){var d=new DepthFeedback.Diagnostics();var r=engine(p,d,null).search(b,4);assertEquals(expected.score(),r.score());assertEquals(0,d.eligible);}
        }
    }
    @Test void workersOwnDistinctMemoryAndDrainBeforeReuse() throws Exception {
        var b=Board.fromFen(SeePruningTest.MIDDLE);var h=GameHistory.initial(b);
        for(var policy:List.of(DepthFeedback.MOVE,DepthFeedback.CAUTION))for(int count:new int[]{2,4,16}) {
            var sinks=new DepthFeedback.Diagnostics[count];
            try(var owner=new ParallelSearch(count,SearchEvaluation.handcrafted(),new TTable(1),ConcurrencyPolicy.ROOT_REUSE).withSeeResearch(SeePolicy.QXRAY,null).withDepthFeedback(policy,i->sinks[i]=new DepthFeedback.Diagnostics());var driver=new SearchDriver(owner)) {
                var seen=Collections.newSetFromMap(new IdentityHashMap<>());
                if(policy==DepthFeedback.MOVE)for(Object w:(Object[])get(owner,"workers"))assertTrue(seen.add(get(get(w,"search"),"feedbackMoves")));
                var c=SearchControl.controlled(3000000,0,-1,TimeSource.SYSTEM);var r=driver.search(new SearchRequest(b,h,8,c));
                assertNotNull(r.lastCompletedResult());assertEquals(r.nodes(),c.nodes());assertEquals(0,owner.activeSearchThreads());Phase2Oracle.legalPv(b,r.lastCompletedResult().principalVariation());
                assertTrue(Arrays.stream(sinks).mapToLong(d->d.adjusted).sum()>0,policy+"/"+count);
                assertTrue(Arrays.stream(sinks).skip(1).mapToLong(d->d.eligible).sum()>0,policy+"/"+count+" helper eligibility");
                assertTrue(Arrays.stream(sinks).skip(1).mapToLong(d->d.verified).sum()>0,policy+"/"+count+" helper learning");
                // Scheduling does not guarantee a repeat/refutation on this particular helper path.
                // Real-model corpus diagnostics measure reached helper adjustments separately.
                if(policy==DepthFeedback.MOVE)for(Object w:(Object[])get(owner,"workers"))assertTrue(empty((byte[])get(get(w,"search"),"feedbackMoves")));
                driver.newGame();assertTrue(driver.search(new SearchRequest(b,h,2)).targetDepthCompleted());
            }
        }
    }
}
