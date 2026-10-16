package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

import static org.junit.jupiter.api.Assertions.*;

class ExactSearchTest {
    private static final ExactEvaluator HCE = (board, ply) -> Eval.evaluate(board);
    private static final String MATE = "7k/8/5KQ1/8/8/8/8/8 w - - 0 1";

    @Test void depthZeroUsesSideToMoveStaticEvaluation() {
        for(String side : new String[] {"w", "b"}) {
            long[] board = Board.fromFen("4k3/8/8/8/8/8/P7/4K3 " + side + " - - 0 1");
            ExactSearchResult result = new ExactSearch().search(board, 0);
            assertTrue(result.completed());
            assertEquals(0, result.completedDepth());
            assertEquals(Eval.evaluate(board), result.score());
            assertEquals(side.equals("w"), result.score() > 0);
            assertEquals(1, result.nodes());
            assertFalse(result.hasMove());
        }
    }

    @Test void negamaxNegatesEveryEdgeAndCountsRootAndLeaves() {
        ExactSearch search = new ExactSearch((board, ply) -> 37);
        assertEquals(37, search.search(Board.startingPosition(), 0).score());
        ExactSearchResult one = search.search(Board.startingPosition(), 1);
        assertEquals(-37, one.score());
        assertEquals(21, one.nodes());
        assertEquals(37, search.search(Board.startingPosition(), 2).score());
    }

    @Test void shallowScoresAndSelectedMovesAgreeWithIndependentExhaustiveOracle() {
        int comparisons = 0;
        for(ExactSearchHarness.Position p : ExactSearchHarness.positions()) {
            int maxDepth = p.name().equals("kiwipete") ? 2 : 3;
            for(int depth = 0; depth <= maxDepth; depth++) {
                compareOracle(Board.fromFen(p.fen()), depth);
                comparisons++;
            }
        }
        for(String fen : new String[] {
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
                "1r5k/P7/8/8/8/8/8/7K w - - 0 1",
                "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1"}) {
            for(int depth = 0; depth <= 2; depth++) {
                compareOracle(Board.fromFen(fen), depth);
                comparisons++;
            }
        }
        assertEquals(35, comparisons);
    }

    private void compareOracle(long[] board, int depth) {
        long[] original = board.clone();
        GameHistory game = GameHistory.initial(board);
        ExhaustiveOracle oracle = new ExhaustiveOracle(HCE);
        int expected = oracle.score(board, new SearchLineHistory(game), depth, 0);
        ExactSearchResult result = new ExactSearch().search(board, game, depth, ExactSearch.NEVER_CANCELLED);
        assertTrue(result.completed());
        assertEquals(expected, result.score(), "depth=" + depth + " board=" + Arrays.toString(board));
        assertTrue(result.nodes() <= oracle.nodes);
        assertArrayEquals(original, board);
        if(result.hasMove()) {
            assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(board)).anyMatch(m -> m == result.bestMove()));
            long[] child = ExhaustiveOracle.child(board, result.bestMove());
            SearchLineHistory line = new SearchLineHistory(game);
            line.pushRealPosition(child);
            assertEquals(expected, -oracle.score(child, line, depth - 1, 1), "Selected move must be optimal, including ties.");
        }
        assertPv(board, game, depth, result);
    }

    private void assertPv(long[] board, GameHistory game, int depth, ExactSearchResult result) {
        SearchLineHistory history = new SearchLineHistory(game);
        long[] pv = result.principalVariation();
        for(long move : pv) {
            assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(board)).anyMatch(m -> m == move));
            board = ExhaustiveOracle.child(board, move);
            history.pushRealPosition(board);
        }
        int leaf = new ExhaustiveOracle(HCE).score(board, history, depth - pv.length, pv.length);
        assertEquals(result.score(), pv.length % 2 == 0 ? leaf : -leaf, "PV must realize the root score.");
        if(result.hasMove()) assertEquals(result.bestMove(), pv[0]);
    }

    @Test void narrowWindowsAreFailSoftAtLeavesAndInternalNodes() {
        long[] board = Board.startingPosition();
        GameHistory game = GameHistory.initial(board);
        ExactSearch search = new ExactSearch((b, p) -> 37);
        assertEquals(37, search.searchWindow(board, game, 0, -10, 10, ExactSearch.NEVER_CANCELLED).score());
        assertEquals(-37, search.searchWindow(board, game, 1, -60, -50, ExactSearch.NEVER_CANCELLED).score());
        assertEquals(-37, search.searchWindow(board, game, 1, 0, 10, ExactSearch.NEVER_CANCELLED).score());
        ExactSearch hce = new ExactSearch();
        for(int depth = 1; depth <= 3; depth++) {
            int exact = new ExhaustiveOracle(HCE).score(board, new SearchLineHistory(game), depth, 0);
            int high = hce.searchWindow(board, game, depth, exact - 21, exact - 1, ExactSearch.NEVER_CANCELLED).score();
            int low = hce.searchWindow(board, game, depth, exact + 1, exact + 21, ExactSearch.NEVER_CANCELLED).score();
            assertTrue(high >= exact - 1 && high <= exact);
            assertTrue(low <= exact + 1 && low >= exact);
            assertEquals(exact, hce.searchWindow(board, game, depth, exact - 1, exact + 1, ExactSearch.NEVER_CANCELLED).score());
        }
    }

    @Test void terminalAndRuleDrawsPrecedeStaticEvaluationEvenAtDepthZero() {
        ExactSearch search = new ExactSearch((b, p) -> { throw new AssertionError("Terminal evaluation"); });
        for(int depth : new int[] {0, 1, 5, ExactSearch.MAX_DEPTH}) {
            ExactSearchResult mate = search.search(Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"), depth);
            assertEquals(-32768, mate.score()); // Mate precedes the 50-move draw.
            assertEquals(1, mate.nodes());
            assertFalse(mate.hasMove());
            for(String fen : new String[] {
                    "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1", // stalemate
                    "4k3/8/8/8/8/8/8/R3K3 w - - 100 1", // fifty move
                    "4k3/8/8/8/8/8/8/4K3 w - - 0 1"}) { // insufficient material
                ExactSearchResult draw = search.search(Board.fromFen(fen), depth);
                assertEquals(0, draw.score());
                assertEquals(1, draw.nodes());
                assertFalse(draw.hasMove());
            }
        }
    }

    @Test void repetitionUsesGameHistoryAndSearchPathRatherThanTwofoldHeuristics() {
        long[] board = Board.startingPosition();
        GameHistory.Builder history = GameHistory.builder(board);
        String[] cycle = {"g1f3", "g8f6", "f3g1", "f6g8"};
        ExactSearch search = new ExactSearch((b, p) -> 37);
        for(int i = 0; i < 8; i++) {
            if(i == 4) assertEquals(37, search.search(board, history.snapshot(), 0, ExactSearch.NEVER_CANCELLED).score());
            if(i == 7) {
                // Black can choose the repetition at the horizon over a -37 leaf.
                ExactSearchResult result = search.search(board, history.snapshot(), 1, ExactSearch.NEVER_CANCELLED);
                assertEquals(0, result.score());
                assertEquals("f6g8", Move.coordinate(result.bestMove()));
            }
            board = play(board, cycle[i % 4]);
            history.appendPosition(board);
        }
        assertEquals(0, search.search(board, history.snapshot(), 0, ExactSearch.NEVER_CANCELLED).score());
    }

    @Test void mateDistanceIsRootPlyAndPrefersTheFasterAvailableMate() {
        long[] board = Board.fromFen(MATE);
        ExactSearch search = new ExactSearch((b, p) -> 0);
        for(int depth : new int[] {1, 2, 3, 4}) {
            ExactSearchResult result = search.search(board, depth);
            assertEquals(32767, result.score());
            assertEquals(1, result.principalVariation().length);
        }
        // Exhaustively prove this root also offers slower mates, rather than
        // asserting an unverified analysis claim about a middlegame position.
        assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(board)).anyMatch(move -> {
            long[] child = ExhaustiveOracle.child(board, move);
            SearchLineHistory history = new SearchLineHistory(GameHistory.initial(board));
            history.pushRealPosition(child);
            return -new ExhaustiveOracle((b, p) -> 0).score(child, history, 2, 1) == 32765;
        }));
    }

    @Test void repeatedRunsPreserveInputAndResultPvOwnership() {
        long[] board = Board.startingPosition();
        long[] original = board.clone();
        ExactSearch search = new ExactSearch();
        ExactSearchResult first = search.search(board, 3);
        long[] pv = first.principalVariation();
        for(int i = 0; i < 3; i++) assertSameSearch(first, search.search(board, 3));
        first.principalVariation()[0] = 0;
        search.search(Board.fromFen(MATE), 1);
        assertArrayEquals(pv, first.principalVariation());
        assertArrayEquals(original, board);
        assertTrue(first.elapsedNanos() > 0);
        assertTrue(first.nps() > 0);
    }

    @Test void unavoidableLossChoosesFourPlyMateOverTwoPlyMate() {
        long[] board = Board.fromFen("6k1/8/5K2/8/8/8/8/Q7 b - - 0 1");
        GameHistory game = GameHistory.initial(board);
        for(long move : ExhaustiveOracle.legalMoves(board)) {
            long[] child = ExhaustiveOracle.child(board, move);
            SearchLineHistory history = new SearchLineHistory(game);
            history.pushRealPosition(child);
            int score = -new ExhaustiveOracle((b, p) -> 0).score(child, history, 3, 1);
            assertEquals(Move.coordinate(move).equals("g8f8") ? -32766 : -32764, score);
        }
        ExactSearch search = new ExactSearch((b, p) -> 0);
        for(int depth : new int[] {4, 5}) {
            ExactSearchResult result = search.search(board, depth);
            assertEquals(-32764, result.score());
            assertNotEquals("g8f8", Move.coordinate(result.bestMove()));
            assertEquals(4, result.principalVariation().length);
        }
    }

    @Test void stableTacticalOrderingIncludesEnPassantAndPromotions() {
        ExactSearch search = new ExactSearch((b, p) -> 0);
        String[][] fixtures = {
                {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "e5d6"},
                {"7k/P7/8/8/8/8/8/7K w - - 0 1", "a7a8q"},
                {"4k3/8/8/8/8/8/r7/R3K3 w - - 0 1", "a1a2"}
        };
        for(String[] fixture : fixtures) {
            assertEquals(fixture[1], Move.coordinate(search.search(Board.fromFen(fixture[0]), 1).bestMove()));
        }
        // All start-position moves are quiet and tied: retain the generator's first move.
        long[] board = Board.startingPosition();
        assertEquals(ExhaustiveOracle.legalMoves(board)[0], search.search(board, 1).bestMove());
    }

    @Test void cancellationBeforeAndDuringTraversalNeverPublishesPartialResultsAndCanBeReused() {
        long[] board = Board.startingPosition();
        long[] original = board.clone();
        GameHistory history = GameHistory.initial(board);
        ExactSearch search = new ExactSearch();
        ExactSearchResult pre = search.search(board, history, 3, () -> true);
        assertAborted(pre);
        assertEquals(0, pre.nodes());
        AtomicInteger checks = new AtomicInteger();
        ExactSearchResult during = search.search(board, history, 3, () -> checks.incrementAndGet() > 40);
        assertAborted(during);
        assertTrue(during.nodes() > 1);
        assertSameSearch(new ExactSearch().search(board, 3), search.search(board, 3));
        assertArrayEquals(original, board);
        SearchControl control = SearchControl.controlled(-1, 0, -1, TimeSource.SYSTEM);
        control.request(SearchTermination.STOPPED);
        assertAborted(search.search(board, history, 0, () -> !control.checkpoint()));
    }

    @Test void cancellationDuringTheLastLeafAndThreadInterruptionAreObserved() {
        AtomicBoolean cancelled = new AtomicBoolean();
        ExactSearch search = new ExactSearch((board, ply) -> { cancelled.set(true); return 42; });
        long[] board = Board.startingPosition();
        assertAborted(search.search(board, GameHistory.initial(board), 0, cancelled::get));
        Thread.currentThread().interrupt();
        try {
            assertAborted(new ExactSearch().search(board, 1));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }

    @Test void invalidEvaluatorScoresCannotCollideWithMateAndExceptionsAllowReuse() {
        assertEquals(32768, ExactSearch.MATE_SCORE);
        assertEquals(32511, ExactSearch.MAX_STATIC_SCORE);
        assertTrue(ExactSearch.MATE_SCORE - ExactSearch.MAX_DEPTH > ExactSearch.MAX_STATIC_SCORE);
        assertTrue(Eval.MAX_STATIC_SCORE <= ExactSearch.MAX_STATIC_SCORE);
        for(int value : new int[] {Integer.MIN_VALUE, -32512, 32512, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new ExactSearch((b, p) -> value).search(Board.startingPosition(), 0));
        }
        AtomicBoolean fail = new AtomicBoolean(true);
        ExactSearch search = new ExactSearch((b, p) -> {
            if(fail.getAndSet(false)) throw new IllegalStateException("test evaluator failure");
            return 37;
        });
        assertThrows(IllegalStateException.class, () -> search.search(Board.startingPosition(), 2));
        assertEquals(37, search.search(Board.startingPosition(), 2).score());
    }

    @Test void smallNeuralAdapterCheckMatchesIndependentFullRebuild() {
        NnueNetwork network = NnueNetwork.initialized(73);
        ExactSearch incremental = new ExactSearch(SearchEvaluation.incremental(network));
        ExactSearch recomputed = new ExactSearch(SearchEvaluation.fullRecompute(network, NnueScoreMapping.V1));
        for(String fen : new String[] {
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
                "1r5k/P7/8/8/8/8/8/7K w - - 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"}) {
            assertSameSearch(recomputed.search(Board.fromFen(fen), 2), incremental.search(Board.fromFen(fen), 2));
        }
    }

    private static long[] play(long[] board, String coordinate) {
        for(long move : ExhaustiveOracle.legalMoves(board)) {
            if(Move.coordinate(move).equals(coordinate)) return ExhaustiveOracle.child(board, move);
        }
        throw new AssertionError("Illegal fixture move " + coordinate);
    }

    private static void assertAborted(ExactSearchResult result) {
        assertFalse(result.completed());
        assertEquals(-1, result.completedDepth());
        assertEquals(Value.INVALID, result.score());
        assertFalse(result.hasMove());
        assertEquals(0, result.principalVariation().length);
    }

    private static void assertSameSearch(ExactSearchResult expected, ExactSearchResult actual) {
        assertTrue(actual.completed());
        assertEquals(expected.score(), actual.score());
        assertEquals(expected.bestMove(), actual.bestMove());
        assertEquals(expected.nodes(), actual.nodes());
        assertEquals(expected.completedDepth(), actual.completedDepth());
        assertArrayEquals(expected.principalVariation(), actual.principalVariation());
    }
}
