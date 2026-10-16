package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class PairNullMoveTest {
    static final ExactEvaluator HCE = (b,p) -> Eval.evaluate(b);
    static Field field(String name) throws Exception {
        var f=ExactSearch.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    static int scout(ExactSearch e,long[] b,int depth,int beta,boolean scout) throws Exception {
        int ply=7; var g=GameHistory.initial(b);
        System.arraycopy(b,0,((long[][])field("boards").get(e))[ply],0,6);
        field("history").set(e,new SearchLineHistory(g,256));
        ((long[])field("historyKeys").get(e))[ply]=SearchKey.rootHistory(b,g);
        if(field("cancelled").get(e)==null)field("cancelled").set(e,ExactSearch.NEVER_CANCELLED);
        var m=ExactSearch.class.getDeclaredMethod("negamax",int.class,int.class,int.class,int.class,long.class,boolean.class);
        m.setAccessible(true);return (int)m.invoke(e,depth,ply,beta-1,beta,0L,scout);
    }
    @Test void offExactlyMatchesAcceptedEffortAcrossCompletedIterations() {
        var b=Board.startingPosition();var g=GameHistory.initial(b);
        var a=ExactSearch.iterationReuseResearch(HCE,new TTable(1),IterationReuse.EFFORT,true,null);
        var c=ExactSearch.nullMoveResearch(HCE,new TTable(1),NullMovePolicy.OFF,null);
        a.beginRequest();c.beginRequest();
        for(int d=1;d<=5;d++)IterationReuseTest.same(a.search(b,g,d,()->false),c.search(b,g,d,()->false));
        a.endRequest();c.endRequest();
    }
    @Test void observationDiscardsAllProbeEffectsExceptTruthfulWork() {
        var b=Board.startingPosition();var g=GameHistory.initial(b);var diag=new NullMovePolicy.Diagnostics();
        var a=ExactSearch.nullMoveResearch(HCE,new TTable(1),NullMovePolicy.OFF,null);
        var c=ExactSearch.nullMoveResearch(HCE,new TTable(1),NullMovePolicy.OBSERVE,diag);
        a.beginRequest();c.beginRequest();
        for(int d=1;d<=6;d++) {
            long n=diag.probeNodes,q=diag.probeQnodes;
            var x=a.search(b,g,d,()->false);var y=c.search(b,g,d,()->false);
            assertEquals(x.score(),y.score());assertArrayEquals(x.principalVariation(),y.principalVariation());
            assertEquals(x.nodes()+diag.probeNodes-n,y.nodes());assertEquals(x.qnodes()+diag.probeQnodes-q,y.qnodes());
            assertEquals(x.selective(),y.selective());
        }
        assertTrue(diag.attempts>0);assertEquals(0,diag.cutoffs);a.endRequest();c.endRequest();
    }
    @Test void pairPassPreservesRealClockAndStatusWithoutChangingHcePass() throws Exception {
        var m=ExactSearch.class.getDeclaredMethod("pairSyntheticPass",long[].class,long[].class);m.setAccessible(true);
        for(String fen:List.of(Board.FEN_STARTING_POSITION,"r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 99 31",
                "4k3/8/8/8/3Pp3/8/8/4K3 b - d3 99 32")) {
            var b=Board.fromFen(fen);var before=b.clone();var c=new long[6];m.invoke(null,b,c);
            assertArrayEquals(before,b);assertArrayEquals(Arrays.copyOf(b,4),Arrays.copyOf(c,4));
            assertEquals(Board.halfMoveClock((int)b[4]),Board.halfMoveClock((int)c[4]));
            assertNotEquals(Board.player((int)b[4]),Board.player((int)c[4]));
            assertEquals(Value.INVALID,Board.enPassantSquare((int)c[4]));assertEquals(b[4]&30,c[4]&30);
            var h=new SearchLineHistory(GameHistory.initial(b));h.enterSyntheticPosition(c);
            assertEquals(0,h.currentOccurrences(c));h.leaveSyntheticPosition();assertEquals(1,h.currentOccurrences(b));
        }
    }
    @Test void probesExcludeOrdinaryTtAndReturnOnlyThresholdWithNoPv() throws Exception {
        final ExactSearch[] owner={null};
        var tt=new TTable(1) {
            void legal() {try {assertFalse(field("nullProbeActive").getBoolean(owner[0]));}catch(Exception e){throw new AssertionError(e);}}
            @Override public boolean probe(long key,TEntry e){legal();return super.probe(key,e);}
            @Override public void save(long key,int depth,int type,int score,long move){legal();super.save(key,depth,type,score,move);}
        };
        var diag=new NullMovePolicy.Diagnostics();var e=ExactSearch.nullMoveResearch(HCE,tt,NullMovePolicy.FIXED,diag);owner[0]=e;
        var b=Board.startingPosition();assertEquals(-1000,scout(e,b,4,-1000,true));
        assertEquals(1,diag.attempts);assertEquals(1,diag.cutoffs);assertEquals(1,field("selectiveEpoch").getLong(e));
        assertEquals(0,((int[])field("pvLength").get(e))[7]);assertFalse(field("nullProbeActive").getBoolean(e));
        assertEquals(1,((SearchLineHistory)field("history").get(e)).currentOccurrences(b));
    }
    @Test void excludesRootCheckSparsePawnMateAndClockBoundaries() throws Exception {
        for(String fen:List.of("8/8/8/8/6q1/8/5k1P/7K w - - 99 1","8/8/8/8/2k5/2p5/2K5/8 w - - 0 1",
                "7k/8/8/8/8/8/Q7/K6r w - - 0 1",Board.FEN_STARTING_POSITION.replace("0 1","98 1"))) {
            var d=new NullMovePolicy.Diagnostics();scout(ExactSearch.nullMoveResearch(HCE,new TTable(1),NullMovePolicy.FIXED,d),Board.fromFen(fen),4,-2000,true);
            assertEquals(0,d.attempts,fen);
        }
        for(int beta:new int[]{-32700,32700}) {
            var d=new NullMovePolicy.Diagnostics();scout(ExactSearch.nullMoveResearch(HCE,new TTable(1),NullMovePolicy.FIXED,d),Board.startingPosition(),4,beta,true);assertEquals(0,d.attempts);
        }
        var d=new NullMovePolicy.Diagnostics();var e=ExactSearch.nullMoveResearch(HCE,new TTable(1),NullMovePolicy.FIXED,d);
        e.search(Board.startingPosition(),4);assertEquals(0,d.attempts); // Eligible root has only d3 descendants.
    }
    @Test void cancelledSyntheticWorkUnwindsHistoryAndCanBeReused() throws Exception {
        var stop=new AtomicBoolean();var e=ExactSearch.nullMoveResearch(new ExactEvaluator(){
            public int evaluate(long[] b,int p){return Eval.evaluate(b);}
            public void child(long[] b,long[] c,int p){stop.set(true);}
        },new TTable(1),NullMovePolicy.FIXED,null);
        field("cancelled").set(e,(java.util.function.BooleanSupplier)stop::get);var b=Board.startingPosition();
        assertThrows(InvocationTargetException.class,()->scout(e,b,4,-1000,true));
        assertFalse(field("nullProbeActive").getBoolean(e));assertEquals(0,field("selectiveEpoch").getLong(e));
        assertEquals(1,((SearchLineHistory)field("history").get(e)).currentOccurrences(b));
        stop.set(false);assertTrue(e.search(b,1).completed());
    }
    @Test void reducedVerificationIsSelectiveAndRestoresScratch() throws Exception {
        var d=new NullMovePolicy.Diagnostics();var e=ExactSearch.nullMoveResearch(HCE,new TTable(1),NullMovePolicy.VERIFIED,d);
        assertEquals(-1000,scout(e,Board.startingPosition(),4,-1000,true));
        assertEquals(1,d.verifications);assertEquals(1,d.cutoffs);assertTrue(field("selectiveEpoch").getLong(e)>0);
        assertFalse(field("nullVerification").getBoolean(e));assertEquals(0,((int[])field("pvLength").get(e))[7]);
        var r=e.search(Board.startingPosition(),5);assertTrue(r.completed());Phase2Oracle.legalPv(Board.startingPosition(),r.principalVariation());
    }
}
