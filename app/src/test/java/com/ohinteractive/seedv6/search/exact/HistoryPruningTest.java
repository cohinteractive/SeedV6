package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class HistoryPruningTest {
    static final String FEN="2bqk2r/1pp2ppp/1bn1p3/r2pPn2/3P4/P1NQ4/2P1NPPP/1RB1KB1R w Kk - 2 11";

    @ParameterizedTest @ValueSource(ints={7,8,9,10,11,12})
    void policyDecisionsKeepGuardsAndAnActuallySearchedHistoryPrefix(int policy) {
        var omittedMoves = new ArrayList<Set<Long>>(); for(int i=0;i<257;i++) omittedMoves.add(new HashSet<>());
        int[] oa=new int[257],ob=new int[257]; boolean[] scouts=new boolean[257];
        long[] counts=new long[3];
        var diag=new LmpDiagnostics() {
            public void node(long[] b,int p,int d,int a,int z,boolean scout) {
                omittedMoves.get(p).clear();oa[p]=a;ob[p]=z;scouts[p]=scout;
            }
            public void historyCandidate(long[] b,long[] c,long m,int p,int d,int n,int q,int a,int z,
                    int frozen,int live,boolean skip) {
                assertTrue(Math.abs(frozen)<=QuietHistory.LIMIT);assertTrue(Math.abs(live)<=QuietHistory.LIMIT);
                boolean material=Long.bitCount(b[0]|b[1]|b[2])>6;
                boolean window=d<=2 && q>=(d==1?6:10);
                boolean negative=d<=2 && q>=3 && frozen<0;
                boolean expected=material && switch(policy) {
                    case 7 -> window && frozen<=0;
                    case 8 -> window || negative;
                    case 9 -> negative;
                    case 10 -> window || (d==3 && q>=6 && frozen<=-9);
                    case 11 -> window && frozen<0;
                    case 12 -> window || (d==1 && negative);
                    default -> throw new AssertionError();
                };
                assertEquals(expected,skip);counts[0]++;
            }
            public void omitted(long[] b,long[] c,long m,int p,int d,int n,int q,int a,int z) {
                assertTrue(p>0 && n>=q && q>=3);
                assertTrue(scouts[p] || ob[p]==oa[p]+1);
                assertTrue(Long.bitCount(b[0]|b[1]|b[2])>6);
                assertFalse(QuiescenceOracle.inCheck(b));assertFalse(QuiescenceOracle.inCheck(c));
                assertFalse(CaptureHistory.isTactical(m,Board.enPassantSquare((int)b[Board.STATUS])));
                assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(b)).anyMatch(x->x==m));
                assertArrayEquals(ExhaustiveOracle.child(b,m),c);
                assertTrue(Board.halfMoveClock((int)c[Board.STATUS])+d-1<100);
                assertTrue(a>=-ExactSearch.MAX_STATIC_SCORE && z<=ExactSearch.MAX_STATIC_SCORE);
                omittedMoves.get(p).add(m);counts[1]++;
            }
            public void cutoff(long[] b,int p,int d,long[] moves,int last) {
                if(!omittedMoves.get(p).isEmpty()) counts[2]++;
                for(int i=0;i<=last;i++) assertFalse(omittedMoves.get(p).contains(moves[i]),"Unsearched malus");
            }
        };
        var evaluator=new ExactEvaluator() {
            public void child(long[] b,long[] c,int p,int q) {
                if(q<=0) assertFalse(omittedMoves.get(p).contains(Sr029Replay.moveBetween(b,c)),"Omitted child admitted");
            }
            public int evaluate(long[] b,int p) { return (int)Math.floorMod(b[Board.KEY],201)-100; }
        };
        var b=Board.fromFen(FEN);
        var result=ExactSearch.lateMovePruningResearch(evaluator,new TTable(1),policy,diag).search(b,5);
        assertTrue(result.completed());assertTrue(result.selective());assertTrue(counts[0]>0);assertTrue(counts[1]>0);
        assertTrue(counts[2]>0,"No cutoff after omission exercised");Phase2QuiescenceTest.legalPv(b,result.principalVariation());
        // A fresh callback-free evaluator prevents diagnostics state from affecting the comparison.
        var silent=ExactSearch.lateMovePruningResearch((x,p)->(int)Math.floorMod(x[Board.KEY],201)-100,new TTable(1),policy,null).search(b,5);
        assertEquals(result.score(),silent.score());assertEquals(result.nodes(),silent.nodes());
        assertEquals(result.qnodes(),silent.qnodes());assertArrayEquals(result.principalVariation(),silent.principalVariation());
    }

    @ParameterizedTest @ValueSource(ints={7,8,9,10,11,12})
    void cancellationAndNewGameDoNotPublishOrRetainOmittedLearning(int policy) {
        var stop=new AtomicBoolean();
        var diag=new LmpDiagnostics() {
            public void omitted(long[] b,long[] c,long m,int p,int d,int n,int q,int a,int z) { stop.set(true); }
        };
        var b=Board.fromFen(FEN);
        var search=ExactSearch.lateMovePruningResearch((x,p)->Eval.evaluate(x),new TTable(1),policy,diag);
        var result=search.search(b,GameHistory.initial(b),6,stop::get);
        assertTrue(stop.get());assertFalse(result.completed());assertFalse(result.hasMove());
        assertEquals(0,result.principalVariation().length);
        search.newGame();stop.set(false);
        var again=search.search(b,2);
        var fresh=ExactSearch.lateMovePruningResearch((x,p)->Eval.evaluate(x),new TTable(1),policy,null).search(b,2);
        assertTrue(again.completed());assertEquals(fresh.score(),again.score());assertEquals(fresh.nodes(),again.nodes());
        assertArrayEquals(fresh.principalVariation(),again.principalVariation());
    }

    @ParameterizedTest @ValueSource(ints={7,8,9,10,11,12})
    void sparseAndClockResourcesKeepCumulativeTree(int policy) {
        for(String fen:new String[]{"8/8/3N4/2PPk3/7K/8/8/8 w - - 1 10",
                "8/8/2Kp1r2/8/8/8/6k1/5b2 b - - 1 16",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 99 1"}) {
            var b=Board.fromFen(fen);
            var control=ExactSearch.lateMovePruningResearch((x,p)->Eval.evaluate(x),new TTable(1),6,null).search(b,4);
            var result=ExactSearch.lateMovePruningResearch((x,p)->Eval.evaluate(x),new TTable(1),policy,null).search(b,4);
            assertTrue(result.completed());assertEquals(control.score(),result.score());
            // Pawn moves reset the clock; only sparse roots guarantee no later eligible domain.
            if(Long.bitCount(b[0]|b[1]|b[2])<=6) {
                assertEquals(control.nodes(),result.nodes());assertArrayEquals(control.principalVariation(),result.principalVariation());
            }
            Phase2QuiescenceTest.legalPv(b,result.principalVariation());
        }
    }
}
