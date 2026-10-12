package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.tt.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class CheckExtensionTest {
    static final ExactEvaluator HCE = (b,p) -> Eval.evaluate(b);
    static final String[] FENS = {
        "4r1k1/8/8/8/8/8/8/4K3 w - - 98 1",
        "8/k1P5/8/1K6/8/8/8/8 w - - 98 1",
        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1",
        "7k/6p1/5KQ1/8/8/8/8/8 w - - 98 1",
        "5Q1k/5pbp/4NN1N/8/8/8/8/4K3 b - - 0 1"
    };
    static ExactSearch search(int policy, TTable table, boolean selective, CheckExtensionDiagnostics diag) {
        return ExactSearch.checkExtensionResearch(HCE, table, policy,
                selective ? ExactSearch.LMR_QUIET : 0, selective ? ExactSearch.LMP_WINDOW : 0, diag);
    }

    @Test void offPreservesCumulativeScoreLineAndAllWork() {
        var b=Board.startingPosition();
        var expected=ExactSearch.lateMovePruningResearch(HCE,new TTable(1),ExactSearch.LMP_WINDOW,null).search(b,5);
        var actual=search(0,new TTable(1),true,new CheckExtensionDiagnostics()).search(b,5);
        equal(expected,actual);
        assertEquals(0,actual.maximumExtensionPlies());
        assertFalse(new ExactSearch(HCE).search(b,2).selective());
    }

    @Test void independentlyEnumeratedExtendedHorizonsAndOriginalWindowBounds() {
        for(String fen:FENS) for(int policy=1;policy<=6;policy++) for(int depth=1;depth<=2;depth++) {
            var b=Board.fromFen(fen);var game=GameHistory.initial(b);
            int expected=new Oracle(policy).score(b,new SearchLineHistory(game),depth,0,0);
            for(TTable tt:new TTable[]{null,new TTable(1)}) {
                var engine=search(policy,tt,false,null);
                for(int[] w:new int[][]{{-32769,32769},{expected-1,expected},{expected,expected+1},{-32767,-32766},{32765,32766}}) {
                    if(w[0]<-32769||w[1]>32769)continue;
                    var r=engine.searchWindow(b,game,depth,w[0],w[1],ExactSearch.NEVER_CANCELLED);
                    assertTrue(r.completed(),fen+"/"+policy);
                    if(r.score()<=w[0])assertTrue(expected<=r.score(),fen+" upper "+policy);
                    else if(r.score()>=w[1])assertTrue(expected>=r.score(),fen+" lower "+policy);
                    else assertEquals(expected,r.score(),fen+"/"+policy);
                    Phase2QuiescenceTest.legalPv(b,r.principalVariation());
                    assertTrue(r.maximumExtensionPlies()<=(policy==1||policy==5?1:2));
                }
            }
        }
    }

    @Test void lastPlyGivingCheckOverlapsExistingQsearchValue() {
        long events=0;
        for(String fen:FENS) {
            var b=Board.fromFen(fen);var diag=new CheckExtensionDiagnostics();
            var r=search(1,null,false,diag).search(b,1);
            assertEquals(ExactSearch.quiescenceResearch(HCE).search(b,1).score(),r.score());
            events+=diag.extensions;
        }
        assertTrue(events>0);
    }

    @Test void capturedAndPlainTreesAgreeAndRetriesKeepTheirCredit() {
        var b=Board.fromFen("2bqk2r/1pp2ppp/1bn1p3/r2pPn2/3P4/P1NQ4/2P1NPPP/1RB1KB1R w Kk - 2 11");
        long retries=0;
        for(int policy=1;policy<=6;policy++) {
            final int p=policy;
            var diag=new CheckExtensionDiagnostics(){
                public void extended(long[] parent,long[] child,long move,int ply,int depth,int used,int ordinal,boolean checked,int count) {
                    assertArrayEquals(ExhaustiveOracle.child(parent,move),child);
                    assertTrue(used>0&&used<=(p==1||p==5?1:2));assertTrue(ordinal>0);
                    if(p<=2)assertTrue(QuiescenceOracle.inCheck(child));
                    else assertTrue(checked&&QuiescenceOracle.inCheck(parent));
                    if(p==4)assertEquals(1,ExhaustiveOracle.legalMoves(parent).length);
                    if(p==5)assertEquals(1,depth);
                }
            };
            var actual=search(policy,new TTable(1),true,diag).search(b,5);
            var plain=search(policy,new TTable(1),true,null).search(b,5);
            equal(plain,actual);assertTrue(actual.selective());assertTrue(diag.extensions>0);
            assertEquals(diag.maximumUsed,actual.maximumExtensionPlies());
            if(policy==2||policy==3)assertEquals(2,diag.maximumUsed,"Exercise exhausted credit policy "+policy);
            retries+=diag.fullResearches;
            Phase2QuiescenceTest.legalPv(b,actual.principalVariation());
        }
        assertTrue(retries>0,"Exercise full PVS retry of an extended edge");
    }

    @Test void repeatedForcedCheckingSequenceExhaustsTwoCredits() {
        // Rh-file check has exactly one evasion; a later Rg-file check repeats it.
        var b=Board.fromFen("7k/8/5KB1/2B5/8/8/8/7R b - - 0 1");
        var diag=new CheckExtensionDiagnostics();var engine=search(4,null,false,diag);
        var r=engine.search(b,GameHistory.initial(b),4,()->engine.visitedNodes()>2_000_000);
        assertTrue(r.completed());assertEquals(2,diag.maximumUsed);
        assertEquals(2,r.maximumExtensionPlies());Phase2QuiescenceTest.legalPv(b,r.principalVariation());
    }

    @Test void repeatedChecksAccumulatePathCreditWithoutExtendingTheFirstOccurrence() {
        var b=Board.fromFen("7k/8/5KB1/2B5/8/8/8/7R b - - 0 1");
        var diag=new CheckExtensionDiagnostics();var engine=search(6,null,false,diag);
        assertTrue(engine.search(b,1).completed());assertEquals(0,diag.extensions);
        var r=engine.search(b,3);
        assertTrue(r.completed());assertTrue(diag.extensions>0);assertEquals(1,r.maximumExtensionPlies());
        assertEquals(new Oracle(6).score(b,new SearchLineHistory(GameHistory.initial(b)),3,0,0),r.score());
        // At depth eight this legal forcing tree reaches four checked main nodes.
        r=engine.search(b,GameHistory.initial(b),8,()->engine.visitedNodes()>2_000_000);
        assertEquals(2,r.maximumExtensionPlies(),"Four checked main positions reach the two-ply cap");
    }

    @Test void ordinaryCreditlessProofCannotAnswerUnusedCreditAndAffectedRootCannotStore() {
        var b=Board.fromFen(FENS[0]);var game=GameHistory.initial(b);
        for(int policy=1;policy<=6;policy++) {
            var table=new TTable(1);var engine=search(policy,table,false,null);
            engine.beginRequest();
            try {
                long key=SearchKey.key(b,SearchKey.rootHistory(b,game));
                var staticResult=new ExactSearch(HCE).search(b,1);
                table.save(key,1,TTable.TYPE_EXACT,staticResult.score(),staticResult.bestMove());
                var r=engine.search(b,1);
                assertEquals(search(policy,null,false,null).search(b,1).score(),r.score());
                assertTrue(r.nodes()>1,"Unused credit must reject the ordinary root cutoff");
            } finally {engine.endRequest();}
            table=new TTable(1);var diag=new CheckExtensionDiagnostics();
            b=Board.fromFen(FENS[3]);game=GameHistory.initial(b);
            var r=search(policy,table,false,diag).search(b,2);
            if(diag.extensions>0) {
                assertTrue(r.selective());
                assertFalse(table.probe(SearchKey.key(b,SearchKey.rootHistory(b,game)),new TTable.TEntry()));
            }
            b=Board.fromFen(FENS[0]);game=GameHistory.initial(b);
        }
    }

    @Test void cancellationInsideExtensionRestoresOwnedStateAndHistory() {
        for(int policy=1;policy<=6;policy++) {
            var b=Board.fromFen(policy==6 ? "7k/8/5KB1/2B5/8/8/8/7R b - - 0 1"
                    : "2bqk2r/1pp2ppp/1bn1p3/r2pPn2/3P4/P1NQ4/2P1NPPP/1RB1KB1R w Kk - 2 11");
            var preserved=b.clone();var game=GameHistory.initial(b);
            var cancel=new AtomicBoolean();
            var diag=new CheckExtensionDiagnostics(){
                public void extended(long[] a,long[] c,long m,int p,int d,int u,int o,boolean ck,int n){cancel.set(true);}
            };
            var engine=search(policy,new TTable(1),policy!=6,diag);
            var r=engine.search(b,game,policy==6?3:4,cancel::get);
            assertTrue(cancel.get(),"Exercise admission cancellation for policy "+policy);assertFalse(r.completed());assertFalse(r.hasMove());
            assertEquals(0,r.maximumExtensionPlies(),"Cancelled first award never entered its child");
            assertEquals(0,r.principalVariation().length);assertArrayEquals(preserved,b);
            // Diagnostics no longer request cancellation from this invocation's supplier.
            var again=engine.search(b,game,1,ExactSearch.NEVER_CANCELLED);
            equal(search(policy,new TTable(1),policy!=6,null).search(b,1),again);
        }
    }

    @Test void finalRealPlyResolvesTerminalsButAbortsUnresolvedMainContinuation() throws Exception {
        var engine=search(3,null,false,null);
        var boards=ExactSearch.class.getDeclaredField("boards");boards.setAccessible(true);
        var hist=ExactSearch.class.getDeclaredField("history");hist.setAccessible(true);
        var cancel=ExactSearch.class.getDeclaredField("cancelled");cancel.setAccessible(true);
        cancel.set(engine,ExactSearch.NEVER_CANCELLED);
        var method=ExactSearch.class.getDeclaredMethod("negamax",int.class,int.class,int.class,int.class,long.class,boolean.class);
        method.setAccessible(true);
        for(String fen:new String[]{"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1", FENS[0],"4r1k1/8/8/8/8/8/8/4K3 w - - 100 1"}) {
            var b=Board.fromFen(fen);System.arraycopy(b,0,((long[][])boards.get(engine))[256],0,b.length);
            hist.set(engine,new SearchLineHistory(GameHistory.initial(b)));
            if(fen.equals(FENS[0])) {
                var e=assertThrows(java.lang.reflect.InvocationTargetException.class,()->method.invoke(engine,1,256,-32769,32769,0L,false));
                assertEquals("Aborted",e.getCause().getClass().getSimpleName());
            } else assertEquals(fen.startsWith("7k")?-32512:0,method.invoke(engine,1,256,-32769,32769,0L,false));
        }
    }

    @Test void repeatedRealPrefixDistinguishesTwofoldAndThreefoldForAllPolicies() {
        for(int policy=1;policy<=6;policy++) {
            var b=Board.startingPosition();var h=GameHistory.builder(b);
            for(int i=0;i<8;i++) {
                if(i==4)assertNotEquals(0,ExactSearch.checkExtensionResearch((x,p)->37,null,policy,0,0,null)
                        .search(b,h.snapshot(),0,ExactSearch.NEVER_CANCELLED).score());
                String uci=new String[]{"g1f3","g8f6","f3g1","f6g8"}[i%4];
                long m=Arrays.stream(ExhaustiveOracle.legalMoves(b)).filter(x->Move.coordinate(x).equals(uci)).findFirst().orElseThrow();
                b=ExhaustiveOracle.child(b,m);h.appendPosition(b);
            }
            assertEquals(0,search(policy,null,false,null).search(b,h.snapshot(),4,ExactSearch.NEVER_CANCELLED).score());
        }
    }

    static void equal(ExactSearchResult a,ExactSearchResult b) {
        assertTrue(a.completed()&&b.completed());assertEquals(a.score(),b.score());
        assertArrayEquals(a.principalVariation(),b.principalVariation());
        assertEquals(a.nodes(),b.nodes());assertEquals(a.qnodes(),b.qnodes());
        assertEquals(a.maximumQply(),b.maximumQply());assertEquals(a.maximumExtensionPlies(),b.maximumExtensionPlies());
    }

    /** Independently enumerated legal minimax; no PVS, ordering, TT, reductions or omissions. */
    static class Oracle {
        final int policy;long nodes;
        Oracle(int policy){this.policy=policy;}
        int score(long[] b,SearchLineHistory h,int d,int ply,int used) {
            if(++nodes>500_000)throw new AssertionError("Unsuitable oracle fixture");
            if(d<=0)return new QuiescenceOracle(HCE).score(b,h,0,ply);
            var moves=ExhaustiveOracle.legalMoves(b);boolean checked=QuiescenceOracle.inCheck(b);
            if(moves.length==0)return checked?-32768+ply:0;
            if(DrawAdjudicator.adjudicateNonTerminal(b,h)!=DrawAdjudicator.RuleDraw.NONE)return 0;
            int best=-32769;
            for(long m:moves) {
                var c=ExhaustiveOracle.child(b,m);
                boolean eligible=switch(policy) {
                    case 1 -> d==1&&QuiescenceOracle.inCheck(c);
                    case 2 -> d>1&&QuiescenceOracle.inCheck(c);
                    case 3,6 -> checked;
                    case 4 -> checked&&moves.length==1;
                    case 5 -> checked&&d==1;
                    default -> false;
                };
                int next=used+(eligible&&used<(policy==6?4:policy==1||policy==5?1:2)?1:0);
                int extra=policy==6?next/2-used/2:next-used;
                h.pushRealPosition(c);
                try{best=Math.max(best,-score(c,h,d-1+extra,ply+1,next));}
                finally{h.popRealPosition();}
            }
            return best;
        }
    }
}
