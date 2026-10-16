package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.exact.Phase2Oracle.*;

/** Independent full legal enumeration, plus production PVS/proof integration witnesses. */
@Timeout(90)
class Phase2QuiescenceTest {
    static final int[] POLICIES = {0, 2, 3, 4, 8, 12, 33, 64, 65, 66, 67};
    static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);

    @Test void preservedReferenceAndProductionTraversalAgreeOnCompactCorpus() {
        for(var f : QuiescenceSeeCorpus.FIXTURES) {
            var board = Board.fromFen(f.fen());
            for(int policy : new int[] {0, 2, 3, 4, 8, 12}) {
                var reference = ExactSearch.quiescenceResearch(HCE, policy).search(board, 0);
                var actual = ExactSearch.quiescencePvsResearch(HCE, new TTable(1), policy).search(board, 0);
                assertTrue(actual.completed(), f.name());
                assertEquals(reference.score(), actual.score(), f.name() + "/" + policy);
                assertTrue(actual.selective());
                legalPv(board, actual.principalVariation());
            }
        }
        for(String fen : new String[] {"4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1",
                "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2"}) {
            var board = Board.fromFen(fen);
            for(int d = 1; d <= 2; d++) for(int policy : new int[] {0, 2, 3, 4}) {
                int expected = ExactSearch.quiescenceResearch(HCE, policy).search(board, d).score();
                var actual = ExactSearch.quiescencePvsResearch(HCE, new TTable(1), policy).search(board, d);
                assertEquals(expected, actual.score(), fen + "/d" + d + "/" + policy);
                legalPv(board, actual.principalVariation());
            }
        }
    }

    @Test void distinctRestrictedTreesMatchIndependentEnumeration() {
        for(String fen : new String[] {
                "4k3/8/8/3q4/4P3/8/8/4K3 w - - 98 1",
                "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1",
                "8/k1P5/2K5/8/8/8/8/8 w - - 98 1",
                "7k/6p1/5KQ1/8/8/8/8/8 w - - 98 1",
                "7k/8/5KQ1/8/8/8/8/8 w - - 98 1",
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"}) {
            var b = Board.fromFen(fen);
            for(int policy : POLICIES) for(int d = 0; d <= 1; d++) {
                var evaluator = new ExactEvaluator() {
                    public int evaluate(long[] board, int ply) {
                        assertFalse(QuiescenceOracle.inCheck(board), "in-check static evaluation");
                        return Eval.evaluate(board);
                    }
                };
                var game = GameHistory.initial(b);
                var oracle = new RestrictedOracle(evaluator, policy);
                int expected = oracle.score(b, new SearchLineHistory(game), d, 0, 0, -1);
                for(TTable table : new TTable[] {null, new TTable(1)}) {
                    var q = ExactSearch.quiescencePvsResearch(evaluator, table, policy);
                    var r = q.search(b, game, d, () -> q.visitedNodes() > 1_000_000);
                    assertTrue(r.completed(), fen + "/" + policy);
                    assertEquals(expected, r.score(), fen + "/d" + d + "/" + policy);
                    legalPv(b, r.principalVariation());
                    if(r.hasMove()) {
                        var child = ExhaustiveOracle.child(b, r.bestMove());
                        var lineHistory = new SearchLineHistory(game);
                        lineHistory.pushRealPosition(child);
                        int chosen = -new RestrictedOracle(evaluator, policy).score(child, lineHistory,
                                d - 1, 1, d == 0 ? 1 : 0, Move.toSquare(r.bestMove()));
                        assertEquals(expected, chosen, "Reported best move must realize the independent value");
                    }
                }
            }
        }
    }

    @Test void extendedMateCannotUseStaticDepthOneLowerBound() {
        // Retained checking-sacrifice after Qe7xf8+: Bxf8 permits Nh6xf7#.
        // The loss lies beyond nominal depth one, inside its qsearch suffix.
        var sacrifice = Board.fromFen("5r1k/4Qpbp/4NN1N/8/8/8/8/4K3 w - - 0 1");
        long move = Arrays.stream(ExhaustiveOracle.legalMoves(sacrifice))
                .filter(m -> Move.coordinate(m).equals("e7f8")).findFirst().orElseThrow();
        var forced = ExhaustiveOracle.child(sacrifice, move);
        int expected = ExactSearch.quiescenceResearch(HCE).search(forced, 1).score();
        assertEquals(-32766, expected);
        var actual = ExactSearch.quiescencePvsResearch(HCE, new TTable(1), 0)
                .searchWindow(forced, GameHistory.initial(forced), 1, -32767, -32766, ExactSearch.NEVER_CANCELLED);
        assertEquals(expected, actual.score());
        // Broader full and mate-band windows against the independent qsearch path.
        for(var f : QuiescenceSeeCorpus.FIXTURES) {
            var b = Board.fromFen(f.fen());
            int value = ExactSearch.quiescenceResearch(HCE).search(b, 1).score();
            for(int[] window : new int[][] {{-32769, 32769}, {-32767, -32766}, {32765, 32766}}) {
                var r = ExactSearch.quiescencePvsResearch(HCE, new TTable(1), 0)
                        .searchWindow(b, GameHistory.initial(b), 1, window[0], window[1], ExactSearch.NEVER_CANCELLED);
                assertTrue(r.completed());
                if(r.score() <= window[0]) assertTrue(value <= r.score(), f.name());
                else if(r.score() >= window[1]) assertTrue(value >= r.score(), f.name());
                else assertEquals(value, r.score(), f.name());
            }
        }
    }

    @Test void noNominalProofFromQsearchAndStaticReferenceRemainsIndependent() throws Exception {
        var b = Board.startingPosition();
        var game = GameHistory.initial(b);
        var table = new TTable(1);
        var q = ExactSearch.quiescencePvsResearch(HCE, table, 2);
        q.beginRequest();
        try {
            var r = q.search(b, 2);
            assertTrue(r.completed() && r.selective());
            var entry = new TTable.TEntry();
            assertFalse(table.probe(SearchKey.key(b, SearchKey.rootHistory(b, game)), entry));
            var storage = TTable.class.getDeclaredField("data");
            storage.setAccessible(true);
            assertTrue(Arrays.stream((long[]) storage.get(table)).allMatch(value -> value == 0),
                    "All positive-depth start-position nodes depend on qsearch in this tree");
            assertEquals(r.score(), q.search(b, 2).score());
        } finally { q.endRequest(); }
        var plain = new ExactSearch(HCE, new TTable(1)).search(b, 2);
        assertFalse(plain.selective());
        assertEquals(new ExactSearch(HCE).search(b, 2).score(), plain.score());
    }

    @Test void explicitSuffixKeepsProvenanceAndRejectsMissingRecaptureIdentity() {
        var b = Board.fromFen("4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1");
        var history = GameHistory.initial(b);
        var q = ExactSearch.quiescencePvsResearch(HCE, null, 4);
        assertTrue(q.searchQuiescenceSuffix(b, history, 1, ExactSearch.NEVER_CANCELLED).selective());
        var recapture = ExactSearch.quiescencePvsResearch(HCE, null, 64);
        assertThrows(IllegalArgumentException.class,
                () -> recapture.searchQuiescenceSuffix(b, history, 1, ExactSearch.NEVER_CANCELLED));
        assertEquals(recapture.search(b, 0).score(),
                recapture.searchQuiescenceSuffix(b, history, 0, ExactSearch.NEVER_CANCELLED).score());
    }

    @Test void cancellationRestoresBoardHistoryAndOwner() {
        var b = Board.startingPosition(); var before = b.clone(); var game = GameHistory.initial(b);
        for(int policy : POLICIES) {
            var q = ExactSearch.quiescencePvsResearch(HCE, new TTable(1), policy);
            var r = q.search(b, game, 4, () -> q.visitedNodes() >= 80);
            assertFalse(r.completed()); assertFalse(r.hasMove());
            assertEquals(0, r.principalVariation().length);
            assertArrayEquals(before, b); game.requireCurrent(b);
            var fresh = ExactSearch.quiescencePvsResearch(HCE, null, policy).search(b, 1);
            assertEquals(fresh.score(), q.search(b, 1).score());
        }
        var stop = new AtomicBoolean();
        ExactEvaluator last = (board, ply) -> { stop.set(true); return 37; };
        assertFalse(ExactSearch.quiescencePvsResearch(last, new TTable(1), 0)
                .search(b, game, 0, stop::get).completed());
    }

}
