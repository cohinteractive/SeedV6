package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class InternalSearchTest {
    static final ExactEvaluator HCE = (b,p) -> Eval.evaluate(b);
    static ExactSearch engine(InternalSearchPolicy p, InternalSearchPolicy.Diagnostics d) {
        return ExactSearch.internalSearchResearch(HCE,new TTable(1),p,d);
    }
    @Test void offAndObservationPreserveAcceptedEffortExactly() {
        for(var policy:List.of(InternalSearchPolicy.OFF,InternalSearchPolicy.OBSERVE)) {
            var b=Board.startingPosition(); var g=GameHistory.initial(b);
            var d=new InternalSearchPolicy.Diagnostics(); var c=engine(policy,d);
            var a=ExactSearch.iterationReuseResearch(HCE,new TTable(1),IterationReuse.EFFORT,true,null);
            a.beginRequest();c.beginRequest();
            for(int depth=1;depth<=6;depth++) IterationReuseTest.same(a.search(b,g,depth,()->false),c.search(b,g,depth,()->false));
            assertTrue(d.legalEffort>0);assertTrue(Arrays.stream(d.missingScout).sum()>0);
            assertEquals(0,d.probes);assertEquals(0,d.reductions);a.endRequest();c.endRequest();
        }
    }
    @Test void iidStaticValuesAgreeWithIndependentOrderedAlphaBeta() {
        for(String fen:List.of(Board.FEN_STARTING_POSITION,
                "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1")) {
            var b=Board.fromFen(fen);var g=GameHistory.initial(b);var copy=b.clone();
            int reference=new ExactSearch(HCE).search(b,g,5,()->false).score();
            for(var policy:List.of(InternalSearchPolicy.IID,InternalSearchPolicy.IID_WIDE,
                    InternalSearchPolicy.IID_SHALLOW,InternalSearchPolicy.IID_NEAR)) {
                var d=new InternalSearchPolicy.Diagnostics();
                var e=ExactSearch.internalSearchResearch(HCE,new TTable(1),policy,d,false,0);
                var r=e.search(b,g,5,()->false);
                assertTrue(r.completed());assertEquals(reference,r.score(),fen+policy);
                assertFalse(r.selective());if(fen.equals(Board.FEN_STARTING_POSITION)) {assertTrue(d.probes>0);assertTrue(d.hints>0);}
                Phase2QuiescenceTest.legalPv(b,r.principalVariation());assertArrayEquals(copy,b);
            }
        }
    }
    @Test void shallowerWideAcquisitionActivatesWithoutChangingTheExactHorizon() {
        var b=Board.startingPosition();var g=GameHistory.initial(b);
        int expected=new ExactSearch(HCE).search(b,g,4,()->false).score();
        var shallow=new InternalSearchPolicy.Diagnostics();var original=new InternalSearchPolicy.Diagnostics();
        for(var policy:List.of(InternalSearchPolicy.IID_SHALLOW,InternalSearchPolicy.IID_WIDE)) {
            var d=policy==InternalSearchPolicy.IID_SHALLOW?shallow:original;
            var e=ExactSearch.internalSearchResearch(HCE,new TTable(1),policy,d,false,0);
            var r=e.search(b,g,4,()->false);
            assertTrue(r.completed());assertFalse(r.selective());assertEquals(expected,r.score());
            Phase2QuiescenceTest.legalPv(b,r.principalVariation());
        }
        assertTrue(shallow.probeDepth[3]>0);assertEquals(0,original.probes);
    }
    @Test void iirSuppressesReducedNodeAndAncestorNominalProof() throws Exception {
        var b=Board.startingPosition();var g=GameHistory.initial(b);long key=SearchKey.key(b,SearchKey.rootHistory(b,g));
        for(var policy:List.of(InternalSearchPolicy.IIR_SCOUT,InternalSearchPolicy.IIR_ALL)) {
            var d=new InternalSearchPolicy.Diagnostics();
            var tt=new TTable(1){
                @Override public void save(long k,int depth,int type,int score,long move) {
                    assertNotEquals(key,k,"Reduced node must not save nominal or relabeled proof");
                    super.save(k,depth,type,score,move);
                }
            };
            var e=ExactSearch.internalSearchResearch(HCE,tt,policy,d,false,0);
            PairNullMoveTest.scout(e,b,4,0,true);
            assertTrue(d.reductions>0);assertTrue(e.anySelectiveWork());
            assertEquals(1,((SearchLineHistory)PairNullMoveTest.field("history").get(e)).currentOccurrences(b));
        }
    }
    @Test void legalInapplicableHashStillPreventsInternalWorkAndIllegalHintDoesNot() throws Exception {
        var b=Board.startingPosition();var g=GameHistory.initial(b);
        long key=SearchKey.key(b,SearchKey.rootHistory(b,g));
        long legal=ExhaustiveOracle.legalMoves(b)[0];
        for(long hint:new long[]{legal,Long.MAX_VALUE,0}) {
            var tt=new TTable(1);tt.save(key,2,TTable.TYPE_EXACT,12345,hint);
            var d=new InternalSearchPolicy.Diagnostics();
            var e=ExactSearch.internalSearchResearch(HCE,tt,InternalSearchPolicy.IID,d,false,0);
            PairNullMoveTest.scout(e,b,4,0,true);
            assertTrue(d.ttEntries>0);assertEquals(hint==legal?0:1,d.probes);
            if(hint==legal)assertTrue(d.legalHash>0);
        }
    }
    @Test void checkedSparseClockAndMateWindowsProtectIir() throws Exception {
        for(String fen:List.of("7k/8/8/8/8/8/Q7/K6r w - - 0 1",
                "8/8/8/8/2k5/2p5/2K5/8 w - - 0 1",
                Board.FEN_STARTING_POSITION.replace("0 1","98 1"))) {
            var d=new InternalSearchPolicy.Diagnostics();PairNullMoveTest.scout(engine(InternalSearchPolicy.IIR_ALL,d),Board.fromFen(fen),4,0,true);
            assertEquals(0,d.reductions,fen);
        }
        for(int beta:new int[]{-32700,32700}) {
            var d=new InternalSearchPolicy.Diagnostics();PairNullMoveTest.scout(engine(InternalSearchPolicy.IIR_ALL,d),Board.startingPosition(),4,beta,true);
            assertEquals(0,d.reductions);
        }
    }
    @Test void cancellationInsideSamePlyProbeRestoresRowsAndOwnerReuse() throws Exception {
        var stop=new AtomicBoolean();var e=ExactSearch.internalSearchResearch(new ExactEvaluator(){
            public int evaluate(long[] b,int p){return Eval.evaluate(b);}
            public void reenter(int ply){stop.set(true);}
        },new TTable(1),InternalSearchPolicy.IID,null);
        var rows=(long[][])PairNullMoveTest.field("moves").get(e);var original=rows.clone();var b=Board.startingPosition();
        var r=e.search(b,GameHistory.initial(b),5,stop::get);assertFalse(r.completed());assertEquals(0,r.principalVariation().length);
        assertFalse(PairNullMoveTest.field("internalProbe").getBoolean(e));
        for(int p=0;p<rows.length;p++)assertSame(original[p],rows[p]);
        stop.set(false);assertTrue(e.search(b,2).completed());
    }
    @Test void ruleTerminalsAndLegalPvSurviveEveryPolicy() {
        for(var policy:InternalSearchPolicy.values()) {
            for(String fen:List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                    "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1","4k3/8/8/8/8/8/8/R3K3 w - - 100 1")) {
                var b=Board.fromFen(fen);IterationReuseTest.same(engine(InternalSearchPolicy.OFF,null).search(b,6),engine(policy,null).search(b,6));
            }
            var b=Board.startingPosition();var e=engine(policy,null);var r=e.search(b,6);
            assertTrue(r.completed());Phase2QuiescenceTest.legalPv(b,r.principalVariation());
        }
    }
}
