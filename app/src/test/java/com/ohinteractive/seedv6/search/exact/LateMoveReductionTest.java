package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class LateMoveReductionTest {
    static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);

    @Test void disabledPathPreservesAcceptedTreeAndReferenceRemainsIndependent() {
        for(String fen : new String[] {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "8/8/K2p4/8/2R2r2/5pk1/4P1P1/8 w - - 2 9"}) {
            var board = Board.fromFen(fen);
            var before = ExactSearch.quiescencePvsResearch(HCE, new TTable(1), 0).search(board, 4);
            var diag = new LmrDiagnostics();
            var after = ExactSearch.lateMoveReductionResearch(HCE, new TTable(1), 0, diag).search(board, 4);
            assertEquals(before.score(), after.score());
            assertEquals(before.nodes(), after.nodes());
            assertEquals(before.qnodes(), after.qnodes());
            assertArrayEquals(before.principalVariation(), after.principalVariation());
            assertEquals(0, diag.attempts);
        }
        var ordinary = new ExactSearch(HCE, new TTable(1)).search(Board.startingPosition(), 4);
        assertFalse(ordinary.selective()); assertEquals(0, ordinary.qnodes());
    }

    @Test void probesProtectTacticalsChecksRootAndRuleBoundaryAndVerifyEveryRaise() {
        var parents = new long[ExactSearch.MAX_DEPTH + 1][];
        var previous = new int[ExactSearch.MAX_DEPTH + 1][];
        long[] raised = {0}, verified = {0}, full = {0};
        var evaluator = new ExactEvaluator() {
            public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
            public void child(long[] parent, long[] child, int ply) { parents[ply] = parent.clone(); }
        };
        var diag = new LmrDiagnostics() {
            @Override public void probe(long[] child, int ply, int depth, int r, int ordinal,
                    int alpha, int beta, int score, boolean verify) {
                assertTrue(ply > 1 && ordinal >= 4 && depth - r >= 1);
                assertFalse(QuiescenceOracle.inCheck(parents[ply - 1]));
                assertFalse(QuiescenceOracle.inCheck(child));
                assertTrue(Board.halfMoveClock((int)child[Board.STATUS]) + depth < 100);
                assertTrue(alpha >= -ExactSearch.MAX_STATIC_SCORE && beta <= ExactSearch.MAX_STATIC_SCORE);
                // Eligible moves are quiet: total material and population are preserved.
                var parent = parents[ply - 1];
                assertEquals(Long.bitCount(parent[0] | parent[1] | parent[2]),
                        Long.bitCount(child[0] | child[1] | child[2]));
                if(score > alpha) { assertTrue(verify); raised[0]++; }
                if(verify) previous[ply] = new int[] {depth, alpha, beta};
            }
            @Override public void verified(int ply, int depth, int alpha, int beta, int score, boolean fullNext) {
                assertArrayEquals(new int[] {depth, alpha, beta}, previous[ply]);
                assertEquals(score > alpha && score < beta, fullNext);
                verified[0]++; if(fullNext) full[0]++;
            }
        };
        var board = Board.startingPosition(); var preserved = board.clone();
        var table = new TTable(1);
        var result = ExactSearch.lateMoveReductionResearch(evaluator, table, ExactSearch.LMR_SCALED, diag).search(board, 5);
        assertTrue(result.completed()); assertTrue(result.selective()); assertTrue(diag.attempts > 0);
        assertTrue(raised[0] > 0); assertEquals(verified[0], diag.verifications);
        assertEquals(full[0], diag.fullResearches);
        assertEquals(diag.attempts, diag.omissions + diag.verifications);
        assertEquals(diag.attempts, Arrays.stream(diag.byReduction).sum());
        Phase2QuiescenceTest.legalPv(board, result.principalVariation());
        assertArrayEquals(preserved, board);
        var entry = new TTable.TEntry();
        assertFalse(table.probe(SearchKey.key(board, SearchKey.rootHistory(board, GameHistory.initial(board))), entry),
                "Selective/q-dependent root cannot publish nominal proof");
    }

    @Test void alwaysVerifiedControlMatchesIndependentUnprunedOracle() {
        for(String fen : new String[] {"8/8/K2p4/8/2R2r2/5pk1/4P1P1/8 w - - 2 9",
                "8/8/8/8/8/4Q3/1k6/3K4 w - - 6 4"}) {
            var board = Board.fromFen(fen);
            int expected = ExactSearch.quiescenceResearch(HCE).search(board, 4).score();
            var diag = new LmrDiagnostics();
            var actual = ExactSearch.lateMoveReductionResearch(HCE, new TTable(1), ExactSearch.LMR_VERIFY_ALL, diag).search(board, 4);
            assertTrue(actual.completed()); assertEquals(expected, actual.score());
            assertEquals(0, diag.omissions); assertEquals(diag.attempts, diag.verifications);
            Phase2QuiescenceTest.legalPv(board, actual.principalVariation());
        }
    }

    @Test void cancellingAtProbeCannotPublishAndOwnerCanBeReused() {
        var cancel = new AtomicBoolean();
        var diag = new LmrDiagnostics() {
            @Override public void probe(long[] child, int ply, int depth, int r, int ordinal,
                    int alpha, int beta, int score, boolean verify) { cancel.set(true); }
        };
        var board = Board.startingPosition();
        var search = ExactSearch.lateMoveReductionResearch(HCE, new TTable(1), ExactSearch.LMR_FIXED, diag);
        var stopped = search.search(board, GameHistory.initial(board), 5, cancel::get);
        assertTrue(diag.attempts > 0); assertFalse(stopped.completed());
        assertFalse(stopped.hasMove()); assertEquals(0, stopped.principalVariation().length);
        cancel.set(false);
        var again = search.search(board, 2);
        var fresh = ExactSearch.quiescencePvsResearch(HCE, new TTable(1), 0).search(board, 2);
        assertTrue(again.completed()); assertEquals(fresh.score(), again.score());
        assertArrayEquals(fresh.principalVariation(), again.principalVariation());
    }

    @Test void mateDrawAndRealPlySemanticsSurviveEveryPolicy() {
        for(String fen : new String[] {"7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1",
                "7k/8/5KQ1/8/8/8/8/8 w - - 98 1",
                "4k3/8/8/8/8/8/8/4K3 w - - 100 1"}) {
            var board = Board.fromFen(fen);
            var reference = ExactSearch.quiescenceResearch(HCE).search(board, 3);
            for(int policy=1; policy<=3; policy++) {
                var result = ExactSearch.lateMoveReductionResearch(HCE, new TTable(1), policy, null).search(board, 3);
                assertTrue(result.completed()); assertEquals(reference.score(), result.score());
                Phase2QuiescenceTest.legalPv(board, result.principalVariation());
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
            var diag = new LmrDiagnostics();
            var result = ExactSearch.lateMoveReductionResearch(evaluator, table, ExactSearch.LMR_SCALED, diag).search(board, game, 5, ExactSearch.NEVER_CANCELLED);
            assertTrue(result.completed()); attempts[0] += diag.attempts;
        }
        assertTrue(attempts[0] > 0); assertTrue(writes[0] > 0);
    }
}
