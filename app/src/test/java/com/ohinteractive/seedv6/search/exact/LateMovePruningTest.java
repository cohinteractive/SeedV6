package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class LateMovePruningTest {
    static final ExactEvaluator HCE = (b,p) -> Eval.evaluate(b);

    @Test void offPreservesCumulativeTreeAndDiagnosticsDoNotChangeIt() {
        for(String fen : new String[]{"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "2bqk2r/1pp2ppp/1bn1p3/r2pPn2/3P4/P1NQ4/2P1NPPP/1RB1KB1R w Kk - 2 11"}) {
            var b=Board.fromFen(fen);
            var expected=ExactSearch.lateMoveReductionResearch(HCE,new TTable(1),ExactSearch.LMR_QUIET,null).search(b,5);
            for(var diag:new LmpDiagnostics[]{null,new LmpDiagnostics()}) {
                var actual=ExactSearch.lateMovePruningResearch(HCE,new TTable(1),0,diag).search(b,5);
                assertEquals(expected.score(),actual.score()); assertEquals(expected.nodes(),actual.nodes());
                assertEquals(expected.qnodes(),actual.qnodes());
                assertArrayEquals(expected.principalVariation(),actual.principalVariation());
            }
        }
        var exact=new ExactSearch(HCE).search(Board.startingPosition(),3);
        assertFalse(exact.selective()); assertEquals(0,exact.qnodes());
    }

    @Test void omissionsAreLegalProtectedAndAbsentFromHistoryPrefixAndEvaluatorAdmissions() {
        var omittedMoves=new ArrayList<Set<Long>>();
        for(int i=0;i<=ExactSearch.MAX_DEPTH;i++) omittedMoves.add(new HashSet<>());
        long[] omissions={0},checkedPrefixes={0},admissions={0};
        var diag=new LmpDiagnostics(){
            public void node(long[] b,int p,int d){omittedMoves.get(p).clear();}
            public void omitted(long[] b,long[] c,long move,int p,int d,int searched,int quiet,int a,int beta){
                assertTrue(p>0 && searched>=quiet && quiet>=6); assertEquals(a+1,beta);
                assertFalse(QuiescenceOracle.inCheck(b)); assertFalse(QuiescenceOracle.inCheck(c));
                assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(b)).anyMatch(m->m==move));
                assertArrayEquals(ExhaustiveOracle.child(b,move),c);
                assertFalse(CaptureHistory.isTactical(move,Board.enPassantSquare((int)b[Board.STATUS])));
                assertTrue(Board.halfMoveClock((int)c[Board.STATUS])+d-1<100);
                assertTrue(a>=-ExactSearch.MAX_STATIC_SCORE && beta<=ExactSearch.MAX_STATIC_SCORE);
                omittedMoves.get(p).add(move); omissions[0]++;
            }
            public void cutoff(long[] b,int p,int d,long[] prefix,int last){
                if(!omittedMoves.get(p).isEmpty()) checkedPrefixes[0]++;
                for(int i=0;i<=last;i++) assertFalse(omittedMoves.get(p).contains(prefix[i]),"Unsearched history malus");
            }
        };
        var prepared=new long[257][Board.MAX_BITBOARDS];
        var eval=new ExactEvaluator(){
            public void initialize(long[] b){System.arraycopy(b,0,prepared[0],0,b.length);}
            public void child(long[] b,long[] c,int p,int q){
                long move=Sr029Replay.moveBetween(b,c);
                if(q <= 0) assertFalse(omittedMoves.get(p).contains(move),"Omitted child admitted");
                System.arraycopy(c,0,prepared[p+1],0,c.length); admissions[0]++;
            }
            public int evaluate(long[] b,int p){assertArrayEquals(prepared[p],b);return ((int)(b[Board.KEY] ^ (b[Board.KEY] >>> 32)) & 511)-256;}
        };
        var b=Board.fromFen("2bqk2r/1pp2ppp/1bn1p3/r2pPn2/3P4/P1NQ4/2P1NPPP/1RB1KB1R w Kk - 2 11");var preserved=b.clone();
        var table=new TTable(1);
        var result=ExactSearch.lateMovePruningResearch(eval,table,3,diag).search(b,5);
        assertTrue(result.completed());assertTrue(omissions[0]>0);assertTrue(admissions[0]>0);
        assertEquals(omissions[0],Arrays.stream(diag.omitted).sum());
        assertTrue(checkedPrefixes[0]>0,"Exercise a cutoff after an omission");
        assertArrayEquals(preserved,b); Phase2QuiescenceTest.legalPv(b,result.principalVariation());
        assertFalse(table.probe(SearchKey.key(b,SearchKey.rootHistory(b,GameHistory.initial(b))),new TTable.TEntry()));
    }

    @Test void cancellationAtOmissionCannotPublishAndStateResets() {
        var cancel=new AtomicBoolean();
        var diag=new LmpDiagnostics(){
            public void omitted(long[] b,long[] c,long m,int p,int d,int n,int q,int a,int beta){cancel.set(true);}
        };
        var b=Board.startingPosition();var search=ExactSearch.lateMovePruningResearch(HCE,new TTable(1),3,diag);
        var stopped=search.search(b,GameHistory.initial(b),5,cancel::get);
        assertTrue(Arrays.stream(diag.omitted).sum()>0);assertFalse(stopped.completed());
        assertFalse(stopped.hasMove());assertEquals(0,stopped.principalVariation().length);
        cancel.set(false);var again=search.search(b,1);
        assertTrue(again.completed());assertEquals(0,Arrays.stream(diag.omitted).sum());
        assertEquals(ExactSearch.quiescenceResearch(HCE).search(b,1).score(),again.score());
    }

    @Test void legalExhaustionDrawAndMateDistancesSurviveEveryPolicy() {
        for(String fen:new String[]{"7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1","7k/8/5KQ1/8/8/8/8/8 w - - 98 1",
                "4k3/8/8/8/8/8/8/4K3 w - - 100 1",
                "8/8/8/8/8/4Q3/1k6/3K4 w - - 6 4"}){
            var b=Board.fromFen(fen);var ref=ExactSearch.quiescenceResearch(HCE).search(b,3);
            for(int p=0;p<=3;p++){
                var result=ExactSearch.lateMovePruningResearch(HCE,new TTable(1),p,null).search(b,3);
                assertTrue(result.completed());assertEquals(ref.score(),result.score());
                Phase2QuiescenceTest.legalPv(b,result.principalVariation());
            }
        }
    }
    @Test void everyObservedOrdinaryStoreIsSoundAtItsActualStoredDepth() {
        record Position(long[] board, GameHistory history) {}
        int[] writes = {0}; long[] attempts = {0};
        for(String fen : new String[] {"8/8/8/8/8/4Q3/1k6/3K4 w - - 6 4",
                "8/8/1Q6/8/7k/8/4K3/8 w - - 18 10"}) {
            var board = Board.fromFen(fen); var game = GameHistory.initial(board);
            var positions = new HashMap<Long, Position>();
            var path = new GameHistory[257]; var fingerprints = new long[257];
            var evaluator = new ExactEvaluator() {
                public int evaluate(long[] b, int p) { return Eval.evaluate(b); }
                public void initialize(long[] b) {
                    path[0] = game; fingerprints[0] = SearchKey.rootHistory(b, game);
                    positions.put(SearchKey.key(b, fingerprints[0]), new Position(b.clone(), game));
                }
                public void child(long[] parent, long[] child, int p) {
                    path[p+1] = GameHistory.builder(path[p]).appendPosition(child).snapshot();
                    fingerprints[p+1] = SearchKey.childHistory(fingerprints[p], path[p+1].currentKey(), (int)child[Board.STATUS]);
                    positions.put(SearchKey.key(child, fingerprints[p+1]), new Position(child.clone(), path[p+1]));
                }
            };
            var table = new TTable(1) {
                @Override public void save(long key, int depth, int type, int score, long move) {
                    var position = positions.get(key); assertNotNull(position);
                    var reference = new ExactSearch(HCE).search(position.board(), position.history(), depth, ExactSearch.NEVER_CANCELLED);
                    assertTrue(reference.completed());
                    assertTrue(type == TYPE_EXACT ? score == reference.score()
                            : type == TYPE_LOWER ? score <= reference.score() : score >= reference.score(),
                            "False ordinary proof at stored depth " + depth + ": " + score + "/" + reference.score());
                    writes[0]++; super.save(key, depth, type, score, move);
                }
            };
            var diag = new LmpDiagnostics();
            var result = ExactSearch.lateMovePruningResearch(evaluator, table, ExactSearch.LMP_COMBINED, diag).search(board, game, 5, ExactSearch.NEVER_CANCELLED);
            assertTrue(result.completed()); attempts[0] += Arrays.stream(diag.omitted).sum();
        }
        assertTrue(attempts[0] > 0); assertTrue(writes[0] > 0);
    }
}
