package com.ohinteractive.seedv6.search.exact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.*;
import static com.ohinteractive.seedv6.search.exact.QuiescenceDeltaEvidence.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="sr001h.instrumented",matches="true")
class QuiescenceDeltaEvidenceTest {
    @Test void completeCorpusKeepsScorePvTreeAndRecordsOnlyEligibleMoves() {
        var e=new QuiescenceDeltaEvidence(50_000);
        for(var f:QuiescenceDeltaCorpus.fixtures()) {
            long[] b=Board.fromFen(f.fen());var game=GameHistory.initial(b);
            var plain=ExactSearch.quiescenceResearch(QuiescenceDeltaCorpus.HCE);
            var traced=QuiescenceDeltaEvidence.search(QuiescenceDeltaCorpus.HCE,e);
            var a=plain.search(b,game,0,()->plain.visitedNodes()>=1_000_000);
            var r=traced.search(b,game,0,()->traced.visitedNodes()>=1_000_000);
            QuiescenceDeltaCorpus.same(a,r);
            assertTrue(r.completed(),f.name());
            for(int i=0;i<e.size;i++) {
                long[] node=Arrays.copyOfRange(e.data,i*WIDTH+BOARD,(i+1)*WIDTH);
                long m=e.get(i,MOVE);assertTrue(QuiescenceDeltaCorpus.eligible(node,m),f.name());
                assertEquals(See.atLeastGeneratedLegal(node,m,0)?1:-1,e.get(i,SEE));
                assertEquals(ExactSearch.tacticalMaterialValue(m,Board.enPassantSquare((int)node[Board.STATUS])),e.get(i,GAIN));
                assertEquals(MOVE_DONE|NODE_DONE,e.get(i,FLAGS)&(MOVE_DONE|NODE_DONE));
                assertEquals(e.get(i,SCORE)>e.get(i,ALPHA),(e.get(i,FLAGS)&RAISED)!=0);
                assertEquals(e.get(i,SCORE)>=e.get(i,BETA),(e.get(i,FLAGS)&CUTOFF)!=0);
                if((e.get(i,FLAGS)&FINAL_BEST)!=0) {
                    assertEquals(e.get(i,SCORE),e.get(i,FINAL));
                    assertTrue((e.get(i,FLAGS)&BEST_CHANGED)!=0);
                }
            }
            long[] snapshot=Arrays.copyOf(e.data,e.size*WIDTH);
            QuiescenceDeltaCorpus.same(r,traced.search(b,game,0,()->traced.visitedNodes()>=1_000_000));
            assertArrayEquals(snapshot,Arrays.copyOf(e.data,e.size*WIDTH));
        }
    }
    @Test void standPatMaterialEpNegativeSeeAndBoundsAreObservedWithoutPruning() {
        var e=new QuiescenceDeltaEvidence(5000);var q=QuiescenceDeltaEvidence.search(QuiescenceDeltaCorpus.HCE,e);
        for(String name:new String[]{"winning","losing-defended","ep-winning","nonchecking-clearance","nonchecking-capture-dead-draw"}) {
            var f=QuiescenceDeltaCorpus.fixtures().stream().filter(x->x.name().equals(name)).findFirst().orElseThrow();
            long[] b=Board.fromFen(f.fen());int staticScore=Eval.evaluate(b);
            var full=q.search(b,0);assertTrue(e.size>0);
            for(int i=0;i<e.size;i++) if(e.get(i,PLY)==0) assertEquals(staticScore,e.get(i,STAND));
            if(name.equals("losing-defended"))assertEquals(-1,e.get(0,SEE));
            if(name.equals("ep-winning"))assertEquals(100,e.get(0,GAIN));
            if(name.equals("nonchecking-capture-dead-draw")) {
                assertEquals(0,full.score());assertEquals(INSUFFICIENT,e.get(0,CAUSE));
                assertTrue((e.get(0,FLAGS)&RAISED)!=0);
            }
            for(int[] window:new int[][]{{full.score()-1,full.score()},{full.score(),full.score()+1},{staticScore-1,staticScore}}) {
                var plain=ExactSearch.quiescenceResearch(QuiescenceDeltaCorpus.HCE);
                var game=GameHistory.initial(b);
                QuiescenceDeltaCorpus.same(plain.searchWindow(b,game,0,window[0],window[1],ExactSearch.NEVER_CANCELLED),
                        q.searchWindow(b,game,0,window[0],window[1],ExactSearch.NEVER_CANCELLED));
            }
        }
    }
    @Test void terminalHistoryAndMatePlyRemainAuthoritative() throws Exception {
        var e=new QuiescenceDeltaEvidence(5000);var q=QuiescenceDeltaEvidence.search(QuiescenceDeltaCorpus.HCE,e);
        long[] b=Board.fromFen("r6k/8/8/8/8/8/8/1K6 b - - 1 1");var game=GameHistory.builder(b);
        String[] cycle={"a8b8","b1a1","b8a8","a1b1"};
        for(int i=0;i<7;i++){b=ExhaustiveOracle.child(b,QuiescenceDepthCorpus.move(b,cycle[i%4]));game.appendPosition(b);}
        var a=ExactSearch.quiescenceResearch(QuiescenceDeltaCorpus.HCE).search(b,game.snapshot(),0,ExactSearch.NEVER_CANCELLED);
        QuiescenceDeltaCorpus.same(a,q.search(b,game.snapshot(),0,ExactSearch.NEVER_CANCELLED));
        for(String fen:new String[]{"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1","7k/5K2/6Q1/8/8/8/8/8 b - - 0 1","4r1k1/8/8/8/8/8/8/4K3 w - - 100 1"}) {
            b=Board.fromFen(fen);QuiescenceDeltaCorpus.same(ExactSearch.quiescenceResearch(QuiescenceDeltaCorpus.HCE).search(b,0),q.search(b,0));assertEquals(0,e.size);
        }
        b=Board.fromFen("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1");
        for(int depth:new int[]{0,1,2})assertEquals(32767,q.search(b,depth).score());
        assertEquals(-32768+256,node(q,Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"),256));
        var ex=assertThrows(InvocationTargetException.class,()->node(q,Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 0 1"),256));
        assertEquals("Aborted",ex.getCause().getClass().getSimpleName());
        for(String field:new String[]{"table","qtable","quietHistory","killers","countermoves"})assertNull(field(q,field));
    }
    @Test void cancellationDoesNotPublishIncompleteMoveOrNodeEvidence() {
        long[] b=Board.fromFen("4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1");var game=GameHistory.initial(b);
        var e=new QuiescenceDeltaEvidence(5000);var q=QuiescenceDeltaEvidence.search(QuiescenceDeltaCorpus.HCE,e);
        boolean sawPending=false;
        for(int limit=0;limit<35;limit++) {
            final int budget=limit;var plain=ExactSearch.quiescenceResearch(QuiescenceDeltaCorpus.HCE);
            var a=plain.search(b,game,0,()->plain.visitedNodes()>=budget);
            var r=q.search(b,game,0,()->q.visitedNodes()>=budget);QuiescenceDeltaCorpus.same(a,r);
            if(!r.completed()) {assertEquals(Value.INVALID,r.score());assertFalse(r.hasMove());}
            for(int i=0;i<e.size;i++)if((e.get(i,FLAGS)&MOVE_DONE)==0){sawPending=true;assertEquals(0,e.get(i,FLAGS)&NODE_DONE);}
        }
        assertTrue(sawPending);
        Thread.currentThread().interrupt();try{assertFalse(q.search(b,0).completed());assertEquals(0,e.size);}finally{Thread.interrupted();}
        AtomicBoolean stop=new AtomicBoolean();var childCancel=QuiescenceDeltaEvidence.search((x,p)->{if(p>0)stop.set(true);return Eval.evaluate(x);},e);
        assertFalse(childCancel.search(b,game,0,stop::get).completed());
    }
    @Test void oracleEvidenceIsCompleteAndDoesNotChangeEnumeration() {
        for(String name:new String[]{"winning","nonchecking-clearance","ep-winning","nonchecking-capture-dead-draw","three-capture-xray"}) {
            var f=QuiescenceDeltaCorpus.fixtures().stream().filter(x->x.name().equals(name)).findFirst().orElseThrow();long[]b=Board.fromFen(f.fen());
            var plain=new QuiescenceOracle(QuiescenceDeltaCorpus.HCE);var diag=new QuiescenceOracle(QuiescenceDeltaCorpus.HCE);List<String>rows=new ArrayList<>();
            diag.evidence=(board,ply,qply,stand,best,moves,values,causes)->QuiescenceDeltaCorpus.oracleRows(rows,name,board,ply,qply,stand,best,moves,values,causes);
            assertEquals(plain.score(b,new SearchLineHistory(GameHistory.initial(b)),0,0),diag.score(b,new SearchLineHistory(GameHistory.initial(b)),0,0));
            assertEquals(plain.nodes,diag.nodes);assertFalse(rows.isEmpty());
        }
    }
    private static Object field(ExactSearch q,String n)throws Exception{Field f=ExactSearch.class.getDeclaredField(n);f.setAccessible(true);return f.get(q);}
    private static int node(ExactSearch q,long[]b,int ply)throws Exception{
        System.arraycopy(b,0,((long[][])field(q,"boards"))[ply],0,6);
        for(String n:new String[]{"history","cancelled"}){Field f=ExactSearch.class.getDeclaredField(n);f.setAccessible(true);f.set(q,n.equals("history")?new SearchLineHistory(GameHistory.initial(b)):ExactSearch.NEVER_CANCELLED);}
        Method m=ExactSearch.class.getDeclaredMethod("quiescence",int.class,int.class,int.class,int.class,int.class);m.setAccessible(true);return(int)m.invoke(q,ply,12,-32769,32769,0);
    }
}
