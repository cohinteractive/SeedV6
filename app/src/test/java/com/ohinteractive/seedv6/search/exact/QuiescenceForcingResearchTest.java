package com.ohinteractive.seedv6.search.exact;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class QuiescenceForcingResearchTest {
    private static final int[] MODES = {36,37,38};

    @Test void corpusOraclesLegalPvAndCompletedSuffixesAgree() {
        try(var out = new PrintStream(OutputStream.nullOutputStream())) { QuiescenceForcingCorpus.run(out, false); }
    }

    @Test void independentFullLegalEnumerationProvesThresholdMembershipAndSiblingIndependence() {
        Set<Integer> bins = new HashSet<>();
        for(var f : QuiescenceQuietCheckCorpus.TARGETED) if(f.oracle()) {
            long[] b = Board.fromFen(f.fen());
            for(int qply : new int[] {0,7}) for(int mode : MODES) {
                List<Long> observed = new ArrayList<>();
                var q = owner(mode, new ExactEvaluator() {
                    public int evaluate(long[] x,int p) { assertFalse(QuiescenceOracle.inCheck(x)); return Eval.evaluate(x); }
                    public void child(long[] a,long[] c,int p) { if(p==0) observed.add(move(a,c)); }
                }, false);
                var r = q.searchQuiescenceSuffix(b,GameHistory.initial(b),qply,ExactSearch.NEVER_CANCELLED);
                assertTrue(r.completed());
                List<Long> expected = new ArrayList<>();
                for(long m : ExhaustiveOracle.legalMoves(b)) {
                    long[] c=ExhaustiveOracle.child(b,m);
                    if(QuiescenceOracle.tactical(b,m)) expected.add(m);
                    else if(QuiescenceOracle.inCheck(c)) {
                        int n=ExhaustiveOracle.legalMoves(c).length; bins.add(Math.min(n,3));
                        if(mode!=36 && qply==0 || n<=(mode==38?2:1)) expected.add(m);
                    }
                }
                assertEquals(new HashSet<>(expected),new HashSet<>(observed),f.name()+"/"+mode+"/"+qply);
                assertEquals(observed.size(),new HashSet<>(observed).size());
            }
        }
        assertEquals(Set.of(0,1,2,3),bins);
    }

    @Test void meaningfulDeeperTwoReplyForkDistinguishesThresholdsAndOneReplyForkSurvivesBoth() {
        for(int i=0;i<2;i++) {
            long[] b=Board.fromFen(QuiescenceForcingCorpus.THRESHOLDS.get(i).fen());var h=GameHistory.builder(b);
            b=child(b,"a2b3");h.appendPosition(b);
            assertEquals(i==0?2:1,ExhaustiveOracle.legalMoves(child(b,"f4d3")).length);
            for(int mode:MODES) {
                var q=owner(mode,QuiescenceSeeCorpus.HCE,false);
                var r=q.searchQuiescenceSuffix(b,h.snapshot(),1,()->q.visitedNodes()>=1000000);
                assertTrue(r.completed());
                assertEquals(i==1?1763:mode==38?1246:-41,r.score());
                if(i==1||mode==38) {
                    assertEquals("f4d3",Move.coordinate(r.bestMove()));
                    assertTrue(QuiescenceSeeCorpus.pv(r).contains("d3f2"),"Fork wins queen");
                }
            }
        }
    }

    @Test void fourReplyDeeperForkRemainsLostButBroadInitialChecksRecoverDrawAndDisplacement() {
        long[] b=Board.fromFen(QuiescenceCheckLimitCorpus.DEEPER.get(2).fen());
        assertEquals(4,ExhaustiveOracle.legalMoves(child(child(b,"a2b3"),"f4d3")).length);
        for(int mode:MODES) { var r=search(owner(mode,QuiescenceSeeCorpus.HCE,false),b);assertEquals(944,r.score());assertEquals("a2b3",Move.coordinate(r.bestMove())); }
        for(int idx:new int[]{5,7,10}) {
            var f=QuiescenceQuietCheckCorpus.TARGETED.get(idx); b=Board.fromFen(f.fen());
            assertEquals(4,ExhaustiveOracle.legalMoves(child(b,f.resource())).length);
            for(int mode:new int[]{37,38}) assertEquals(idx==10?241:0,search(owner(mode,QuiescenceSeeCorpus.HCE,false),b).score());
            assertNotEquals(idx==10?241:0,search(owner(36,QuiescenceSeeCorpus.HCE,false),b).score());
        }
    }

    @Test void repeatedOneReplyChecksRecoverSecondAndThirdMatesWithoutAnyCountLimit() {
        for(int mode:MODES) for(int idx:new int[]{0,1}) {
            long[] b=Board.fromFen(idx==0?QuiescenceQuietCheckCorpus.TARGETED.get(6).fen():QuiescenceCheckLimitCorpus.DEEPER.get(3).fen());
            var q=owner(mode,QuiescenceSeeCorpus.HCE,true);var r=search(q,b);
            assertTrue(r.completed());assertEquals(idx==0?32765:32763,r.score());assertTrue(q.forcingDiagnostics()[9]>=idx+2);
            int quiet=0;
            for(long m:r.principalVariation()) {
                boolean ordinary=QuiescenceCheckLimitCorpus.ordinary(b,m);b=ExhaustiveOracle.child(b,m);
                if(ordinary){quiet++;assertTrue(ExhaustiveOracle.legalMoves(b).length<=1);}
            }
            assertEquals(idx+2,quiet);
        }
    }

    @Test void checkedEvasionsIncludingCounterchecksAreUnfilteredAndTacticalsAreUnaffected() throws Exception {
        long[] b=Board.fromFen("4r3/8/8/8/8/8/8/2B1K1k1 w - - 98 1");
        for(int mode:MODES) {
            List<Long> root=new ArrayList<>();var q=owner(mode,new ExactEvaluator(){
                public int evaluate(long[] a,int p){assertFalse(QuiescenceOracle.inCheck(a));return 37;}
                public void child(long[] a,long[] c,int p){if(p==17)root.add(move(a,c));}
            },false);
            node(q,b,17,10,-32769,32769);
            assertEquals(Arrays.stream(ExhaustiveOracle.legalMoves(b)).boxed().toList(),root);
        }
        for(String fen:new String[]{"7k/P7/8/8/8/8/8/7K w - - 0 1","k6r/6P1/8/8/8/8/8/K7 w - - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"}) {
            b=Board.fromFen(fen);
            for(int mode:MODES) {
                List<Long> root=new ArrayList<>();var q=owner(mode,new ExactEvaluator(){public int evaluate(long[]a,int p){return 37;}public void child(long[]a,long[]c,int p){if(p==0)root.add(move(a,c));}},false);
                assertTrue(search(q,b).completed());
                for(long m:ExhaustiveOracle.legalMoves(b))if(QuiescenceOracle.tactical(b,m))assertTrue(root.contains(m));
            }
        }
    }

    @Test void terminalDrawPrecedenceActualPlyFailSoftAndNormalLeafEntryRemainIntact() throws Exception {
        long[] mate=Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(0).fen());
        for(int mode:MODES) {
            assertEquals(32750,node(owner(mode,QuiescenceSeeCorpus.HCE,false),mate,17,91,-32769,32769));
            var q=owner(mode,(b,p)->{throw new AssertionError("Terminal eval");},false);
            assertEquals(-32751,node(q,Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"),17,91,-32769,32769));
            for(String fen:new String[]{"7k/5K2/6Q1/8/8/8/8/8 b - - 0 1","4k3/8/8/8/8/8/8/R3K3 w - - 100 1","4k3/8/8/8/8/8/8/4K3 w - - 0 1"})assertEquals(0,node(q,Board.fromFen(fen),17,91,-32769,32769));
            q=owner(mode,QuiescenceSeeCorpus.HCE,false);
            var r=q.searchWindow(mate,GameHistory.initial(mate),0,-32769,2000,ExactSearch.NEVER_CANCELLED);assertEquals(32767,r.score());assertEquals(1,r.principalVariation().length);
            r=q.searchWindow(mate,GameHistory.initial(mate),0,-32769,1000,ExactSearch.NEVER_CANCELLED);assertEquals(Eval.evaluate(mate),r.score());assertFalse(r.hasMove());
            var o=new QuiescenceOracle(QuiescenceSeeCorpus.HCE,mode);
            assertEquals(o.score(mate,new SearchLineHistory(GameHistory.initial(mate)),1,0),q.search(mate,1).score());
        }
    }

    @Test void historySensitiveRepetitionStillAdjudicatesBeforeEvaluation() {
        long[] b=Board.fromFen("7k/8/8/8/8/8/8/rK6 w - - 2 2");var h=GameHistory.builder(b);String[] cycle={"b1b2","a1a2","b2b1","a2a1"};
        for(int i=0;i<8;i++){b=child(b,cycle[i%4]);h.appendPosition(b);}assertTrue(h.snapshot().isFormalThreefold(b));
        for(int mode:MODES)assertEquals(0,owner(mode,(x,p)->{throw new AssertionError("Draw eval");},false).search(b,h.snapshot(),0,ExactSearch.NEVER_CANCELLED).score());
    }

    @Test void safetyDiagnosticsNoHistoryOrOtherPolicyAndRetainedTrees() throws Exception {
        long[] b=Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(6).fen());var g=GameHistory.initial(b);
        for(int mode:MODES) {
            var q=owner(mode,QuiescenceSeeCorpus.HCE,false);var d=owner(mode,QuiescenceSeeCorpus.HCE,true);var r=search(q,b);
            same(r,search(q,b));same(r,search(d,b));long[] diag=d.forcingDiagnostics();same(r,search(d,b));assertArrayEquals(diag,d.forcingDiagnostics());
            assertEquals(diag[0],diag[1]+diag[2]+diag[3]+diag[4]);assertEquals(diag[0],diag[5]+diag[7]);
            aborted(q.search(b,g,0,()->q.visitedNodes()>=3));
            AtomicBoolean stop=new AtomicBoolean();var cancel=owner(mode,(a,p)->{stop.set(true);return Eval.evaluate(a);},false);aborted(cancel.search(b,g,0,stop::get));
            Thread.currentThread().interrupt();try{aborted(q.search(b,0));}finally{Thread.interrupted();}
            for(String fen:new String[]{"4r1k1/8/8/8/8/8/8/4K3 w - - 0 1",QuiescenceQuietCheckCorpus.TARGETED.get(0).fen()}) {
                var ex=assertThrows(InvocationTargetException.class,()->node(q,Board.fromFen(fen),256,12,-32769,32769));assertEquals("Aborted",ex.getCause().getClass().getSimpleName());
            }
            long[] excluded=Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 98 1");assertEquals(Eval.evaluate(excluded),node(q,excluded,256,12,-32769,32769));
            for(String f:new String[]{"table","qtable","quietHistory","killers","countermoves","continuationHistory"})assertNull(field(q,f));
            assertEquals(0,field(q,"qsearchPruning"));assertEquals(Integer.MAX_VALUE,field(q,"qdepthLimit"));assertEquals(Integer.MAX_VALUE,field(q,"quietCheckLimit"));
        }
        int[] modes={0,32,33,34,35};int[] scores={82,32765,152,152,32765};long[] nodes={1,571,8,8,20};
        for(int i=0;i<modes.length;i++){var r=search(owner(modes[i],QuiescenceSeeCorpus.HCE,false),b);assertEquals(scores[i],r.score());assertEquals(nodes[i],r.nodes());}
        var control=new ExactSearch(QuiescenceSeeCorpus.HCE).search(Board.startingPosition(),2);assertEquals(0,control.score());assertEquals(128,control.nodes());assertEquals("e2e4",Move.coordinate(control.bestMove()));
    }

    private static ExactSearch owner(int m,ExactEvaluator e,boolean d){return ExactSearch.quiescenceResearch(e,m,d);}
    private static ExactSearchResult search(ExactSearch q,long[]b){return q.search(b,GameHistory.initial(b),0,()->q.visitedNodes()>=1000000);}
    private static long[] child(long[]b,String m){return ExhaustiveOracle.child(b,QuiescenceDepthCorpus.move(b,m));}
    private static long move(long[]a,long[]b){for(long m:ExhaustiveOracle.legalMoves(a))if(Arrays.equals(ExhaustiveOracle.child(a,m),b))return m;throw new AssertionError("Illegal child");}
    private static void same(ExactSearchResult a,ExactSearchResult b){assertEquals(a.completed(),b.completed());assertEquals(a.score(),b.score());assertEquals(a.nodes(),b.nodes());assertEquals(a.maximumQply(),b.maximumQply());assertArrayEquals(a.principalVariation(),b.principalVariation());}
    private static void aborted(ExactSearchResult r){assertFalse(r.completed());assertEquals(Value.INVALID,r.score());assertEquals(-1,r.completedDepth());assertFalse(r.hasMove());}
    private static Object field(ExactSearch q,String n)throws Exception{Field f=ExactSearch.class.getDeclaredField(n);f.setAccessible(true);return f.get(q);}
    private static int node(ExactSearch q,long[]b,int ply,int qply,int alpha,int beta)throws Exception{
        System.arraycopy(b,0,((long[][])field(q,"boards"))[ply],0,6);
        for(String n:new String[]{"history","cancelled"}){Field f=ExactSearch.class.getDeclaredField(n);f.setAccessible(true);f.set(q,n.equals("history")?new SearchLineHistory(GameHistory.initial(b)):ExactSearch.NEVER_CANCELLED);}
        Method m=ExactSearch.class.getDeclaredMethod("quiescence",int.class,int.class,int.class,int.class,int.class);m.setAccessible(true);return(int)m.invoke(q,ply,qply,alpha,beta,0);
    }
}
