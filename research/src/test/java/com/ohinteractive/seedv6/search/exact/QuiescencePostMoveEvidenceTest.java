package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.rules.*;
import static com.ohinteractive.seedv6.search.exact.QuiescenceDeltaEvidence.*;
import static com.ohinteractive.seedv6.search.exact.QuiescenceDeltaCorpus.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(45)
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="sr001i.instrumented",matches="true")
class QuiescencePostMoveEvidenceTest {
    static final class Counting implements ExactEvaluator {
        long calls,children,hash;
        public int evaluate(long[] b,int ply){calls++;hash=hash*31+b[Board.KEY]+ply;return Eval.evaluate(b);}
        public void child(long[] p,long[] c,int ply){children++;hash=hash*31+c[Board.KEY]+ply;}
    }
    @Test void corpusIdentityNaturalEvaluationBoundAndRepeatability() {
        var e=new QuiescencePostMoveEvidence(50_000);
        for(var f:QuiescencePostMoveCorpus.fixtures()) {
            long[] b=Board.fromFen(f.fen());var game=GameHistory.initial(b);
            var aEval=new Counting();var bEval=new Counting();
            var plain=ExactSearch.quiescenceResearch(aEval);var q=QuiescencePostMoveEvidence.search(bEval,e);
            var a=plain.search(b,game,0,()->plain.visitedNodes()>=BUDGET);
            var r=q.search(b,game,0,()->q.visitedNodes()>=BUDGET);same(a,r);
            assertEquals(aEval.calls,bEval.calls);assertEquals(aEval.children,bEval.children);assertEquals(aEval.hash,bEval.hash);
            validate(e);long[] base=Arrays.copyOf(e.base.data,e.base.size*WIDTH);
            int[] post=Arrays.copyOf(e.postStatic,e.base.size),kinds=Arrays.copyOf(e.childKind,e.base.size),nodes=Arrays.copyOf(e.childNodes,e.base.size);
            same(r,q.search(b,game,0,()->q.visitedNodes()>=BUDGET));
            assertArrayEquals(base,Arrays.copyOf(e.base.data,e.base.size*WIDTH));
            assertArrayEquals(post,Arrays.copyOf(e.postStatic,e.base.size));
            assertArrayEquals(kinds,Arrays.copyOf(e.childKind,e.base.size));assertArrayEquals(nodes,Arrays.copyOf(e.childNodes,e.base.size));
        }
    }
    static void validate(QuiescencePostMoveEvidence e) {
        for(int i=0;i<e.base.size;i++) {
            int flags=(int)e.base.get(i,FLAGS);
            if((flags&MOVE_DONE)==0){assertEquals(-1,e.childKind[i]);assertEquals(Integer.MIN_VALUE,e.postStatic[i]);continue;}
            assertTrue(e.childKind[i]>=0&&e.childKind[i]<=5);
            if(e.childKind[i]!=0){assertEquals(Integer.MIN_VALUE,e.postStatic[i]);assertEquals(0,e.base.get(i,SCORE));continue;}
            long score=e.base.get(i,SCORE),alpha=e.base.get(i,ALPHA),post=e.postStatic[i];
            assertTrue(score<=post);
            if(post<=alpha){assertEquals(post,score);assertEquals(1,e.childNodes[i]);assertTrue(e.childStandPatCutoff[i]);assertEquals(0,flags&(RAISED|CUTOFF));}
            if((flags&RAISED)!=0)assertTrue(post>alpha);
        }
    }
    @Test void narrowWindowsReuseExistingCutoffAndPreserveFailSoftWitnesses() {
        var e=new QuiescencePostMoveEvidence(5000);var q=QuiescencePostMoveEvidence.search(HCE,e);
        boolean equality=false,strict=false,finalBestBelowAlpha=false;
        for(String name:new String[]{"winning","losing-defended","ep-winning","nonchecking-clearance","three-capture-xray"}) {
            var f=QuiescencePostMoveCorpus.fixtures().stream().filter(x->x.name().equals(name)).findFirst().orElseThrow();
            long[] b=Board.fromFen(f.fen());int v=q.search(b,0).score();
            for(int[] w:new int[][]{{v-1,v},{v,v+1},{v+100,v+101},{-32769,32769}}){
                same(ExactSearch.quiescenceResearch(HCE).searchWindow(b,GameHistory.initial(b),0,w[0],w[1],ExactSearch.NEVER_CANCELLED),
                     q.searchWindow(b,GameHistory.initial(b),0,w[0],w[1],ExactSearch.NEVER_CANCELLED));validate(e);
                for(int i=0;i<e.base.size;i++)if(e.childKind[i]==0){
                    long gap=e.base.get(i,ALPHA)-e.postStatic[i];equality|=gap==0;strict|=gap>0;
                    finalBestBelowAlpha|=gap>0&&(e.base.get(i,FLAGS)&FINAL_BEST)!=0;
                }
            }
        }
        assertTrue(equality);assertTrue(strict);assertTrue(finalBestBelowAlpha,"Skipping rather than returning the proven fail-soft score would not be identical");
    }
    @Test void immediateAdjudicationExcludedInsteadOfInventingChildStatic() {
        var e=new QuiescencePostMoveEvidence(5000);var q=QuiescencePostMoveEvidence.search(HCE,e);
        for(String name:new String[]{"nonchecking-capture-dead-draw","capture-to-stalemate"}) {
            var f=QuiescencePostMoveCorpus.fixtures().stream().filter(x->x.name().equals(name)).findFirst().orElseThrow();
            long[] b=Board.fromFen(f.fen());same(ExactSearch.quiescenceResearch(HCE).search(b,0),q.search(b,0));
            assertTrue(e.base.size>0);assertEquals(name.equals("capture-to-stalemate")?STALEMATE:INSUFFICIENT,e.childKind[0]);
            assertEquals(Integer.MIN_VALUE,e.postStatic[0]);assertFalse(e.childStandPatCutoff[0]);validate(e);
        }
        for(String fen:new String[]{"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1","7k/5K2/6Q1/8/8/8/8/8 b - - 0 1","4r1k1/8/8/8/8/8/8/4K3 w - - 100 1"}) {
            long[] b=Board.fromFen(fen);same(ExactSearch.quiescenceResearch(HCE).search(b,0),q.search(b,0));assertEquals(0,e.base.size);
        }
    }
    @Test void cancellationLeavesUnresolvedChildWithoutStaticObservation() {
        long[] b=Board.fromFen("4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1");
        var e=new QuiescencePostMoveEvidence(5000);var q=QuiescencePostMoveEvidence.search(HCE,e);boolean pending=false;
        for(int budget=0;budget<35;budget++){
            final int limit=budget;var plain=ExactSearch.quiescenceResearch(HCE);
            same(plain.search(b,GameHistory.initial(b),0,()->plain.visitedNodes()>=limit),q.search(b,GameHistory.initial(b),0,()->q.visitedNodes()>=limit));
            validate(e);for(int i=0;i<e.base.size;i++)pending|=e.childKind[i]<0;
        }
        assertTrue(pending);
        Thread.currentThread().interrupt();try{assertFalse(q.search(b,0).completed());assertEquals(0,e.base.size);}finally{Thread.interrupted();}
        java.util.concurrent.atomic.AtomicBoolean stop=new java.util.concurrent.atomic.AtomicBoolean();
        var cancelled=QuiescencePostMoveEvidence.search((x,p)->{if(p>0)stop.set(true);return Eval.evaluate(x);},e);
        assertFalse(cancelled.search(b,GameHistory.initial(b),0,stop::get).completed());validate(e);
    }
    @Test void oracleStaticObserverPreservesEnumerationAndBoundsExactContinuation() {
        for(String name:new String[]{"winning","losing-defended","ep-winning","nonchecking-clearance","three-capture-xray","nonchecking-capture-dead-draw","capture-to-stalemate"}){
            var f=QuiescencePostMoveCorpus.fixtures().stream().filter(x->x.name().equals(name)).findFirst().orElseThrow();long[] b=Board.fromFen(f.fen());
            var evalA=new Counting();var evalB=new Counting();var a=new QuiescenceOracle(evalA);var o=new QuiescenceOracle(evalB);int[] count={0};
            o.staticEvidence=(board,p,qp,stand,best,moves,values,causes,post,kinds)->{
                for(int i=0;i<moves.length;i++)if(values[i]!=Integer.MIN_VALUE&&eligible(board,moves[i])){
                    count[0]++;if(kinds[i]==0)assertTrue(values[i]<=post[i]);else assertEquals(Integer.MIN_VALUE,post[i]);
                }
            };
            assertEquals(a.score(b,new SearchLineHistory(GameHistory.initial(b)),0,0),o.score(b,new SearchLineHistory(GameHistory.initial(b)),0,0));
            assertEquals(a.nodes,o.nodes);assertEquals(evalA.calls,evalB.calls);assertEquals(evalA.hash,evalB.hash);assertTrue(count[0]>0);
        }
    }
    @Test void historyAndActualPathMateDistanceRemainIdentical() throws Exception {
        var e=new QuiescencePostMoveEvidence(5000);var q=QuiescencePostMoveEvidence.search(HCE,e);
        long[] b=Board.fromFen("r6k/8/8/8/8/8/8/1K6 b - - 1 1");var game=GameHistory.builder(b);
        String[] cycle={"a8b8","b1a1","b8a8","a1b1"};
        for(int i=0;i<7;i++){b=ExhaustiveOracle.child(b,QuiescenceDepthCorpus.move(b,cycle[i%4]));game.appendPosition(b);}
        same(ExactSearch.quiescenceResearch(HCE).search(b,game.snapshot(),0,ExactSearch.NEVER_CANCELLED),q.search(b,game.snapshot(),0,ExactSearch.NEVER_CANCELLED));
        b=Board.fromFen("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1");
        for(int d:new int[]{0,1,2}){var r=q.search(b,d);assertEquals(32767,r.score());same(ExactSearch.quiescenceResearch(HCE).search(b,d),r);}
        assertEquals(-32768+256,node(q,Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"),256));
        var ex=assertThrows(java.lang.reflect.InvocationTargetException.class,
                ()->node(q,Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 0 1"),256));
        assertEquals("Aborted",ex.getCause().getClass().getSimpleName());
    }
    private static int node(ExactSearch q,long[] b,int ply)throws Exception {
        var boards=ExactSearch.class.getDeclaredField("boards");boards.setAccessible(true);
        System.arraycopy(b,0,((long[][])boards.get(q))[ply],0,Board.MAX_BITBOARDS);
        for(String name:new String[]{"history","cancelled"}){
            var f=ExactSearch.class.getDeclaredField(name);f.setAccessible(true);
            f.set(q,name.equals("history")?new SearchLineHistory(GameHistory.initial(b)):ExactSearch.NEVER_CANCELLED);
        }
        var m=ExactSearch.class.getDeclaredMethod("quiescence",int.class,int.class,int.class,int.class,int.class);
        m.setAccessible(true);return (int)m.invoke(q,ply,12,-32769,32769,0);
    }
}
