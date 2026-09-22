package com.ohinteractive.seedv6.core.brn2;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.alphabeta.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2AccumulatorTest {
    private static final Brn2Model MODEL = new Brn2Model();
    private static final double TOLERANCE = 2e-13;
    private double maxError;
    private int comparisons;

    @Test void quietCaptureCastlingEnPassantAndEveryPromotionBothColours() {
        String[][] fixtures = {
            {Board.FEN_STARTING_POSITION, "e2e4", "g1f3"},
            {"4k3/8/8/3n4/4P3/8/8/4K3 w - - 0 1", "e4d5"},
            {"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1", "e1c1"},
            {"r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1", "e8g8", "e8c8"},
            {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "e5d6"},
            {"4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 2", "d4e3"},
            {"1r5k/P7/8/8/8/8/8/7K w - - 0 1", "a7a8q", "a7a8r", "a7a8b", "a7a8n", "a7b8q", "a7b8r", "a7b8b", "a7b8n"},
            {"7k/8/8/8/8/8/p7/1R5K b - - 0 1", "a2a1q", "a2a1r", "a2a1b", "a2a1n", "a2b1q", "a2b1r", "a2b1b", "a2b1n"}
        };
        for (var f : fixtures) {
            long[] board = Board.fromFen(f[0]); var parent = new Brn2Accumulator(MODEL); parent.rebuild(board);
            for (int m = 1; m < f.length; m++) {
                String coordinate = f[m];
                long move = Arrays.stream(moves(board)).filter(x -> Move.coordinate(x).equals(coordinate)).findFirst().orElseThrow();
                long[] child = play(board, move); var next = new Brn2Accumulator(MODEL);
                next.update(board, child, parent); check(MODEL, child, next); check(MODEL, board, parent);
            }
        }
    }

    @Test void longDeterministicLegalMakeUnmakeSequencesAndDifferentWeights() {
        var random = new SplittableRandom(20260922);
        double[] weights = MODEL.copyWeights();
        for (int i = 0; i < weights.length; i++) weights[i] += random.nextDouble(-.02, .02);
        for (var model : new Brn2Model[]{MODEL, new Brn2Model(weights)}) {
            for (int game = 0; game < 24; game++) {
                long[][] boards = new long[201][]; var states = new Brn2Accumulator[201];
                boards[0] = Board.startingPosition(); states[0] = new Brn2Accumulator(model); states[0].rebuild(boards[0]);
                int ply = 0;
                for (; ply < 200; ply++) {
                    var legal = moves(boards[ply]); if (legal.length == 0) break;
                    boards[ply + 1] = play(boards[ply], legal[random.nextInt(legal.length)]);
                    states[ply + 1] = new Brn2Accumulator(model);
                    states[ply + 1].update(boards[ply], boards[ply + 1], states[ply]);
                    check(model, boards[ply + 1], states[ply + 1]);
                }
                // Every unmake restores the untouched parent. Reuse child slots for a sibling too.
                for (; ply >= 0; ply--) {
                    check(model, boards[ply], states[ply]);
                    if (ply < 200) {
                        var legal = moves(boards[ply]);
                        if (legal.length > 0) {
                            var sibling = play(boards[ply], legal[0]);
                            var child = states[ply + 1] == null ? new Brn2Accumulator(model) : states[ply + 1];
                            child.update(boards[ply], sibling, states[ply]); check(model, sibling, child);
                        }
                    }
                }
            }
        }
        assertTrue(comparisons > 20_000);
        System.out.printf(Locale.ROOT, "BRN-2 equivalence comparisons=%d maxOutputError=%.16g tolerance=%.1g mappedScoreDifferences=0%n", comparisons, maxError, TOLERANCE);
    }

    @Test void allRawStatusBitsKeyIndependenceAndModelPositionIsolation() {
        long[] board = Board.startingPosition(); var parent = new Brn2Accumulator(MODEL); parent.rebuild(board);
        var child = new Brn2Accumulator(MODEL);
        for (int bit = 0; bit < 64; bit++) {
            var next = board.clone(); next[Board.STATUS] ^= 1L << bit;
            child.update(board, next, parent); check(MODEL, next, child);
            assertThrows(IllegalArgumentException.class, () -> child.evaluate(board));
        }
        var keyOnly = board.clone(); keyOnly[Board.KEY] ^= -1;
        assertEquals(parent.evaluate(board), parent.evaluate(keyOnly));
        assertThrows(IllegalArgumentException.class, () -> parent.update(board, board, parent));
        assertThrows(IllegalArgumentException.class, () -> new Brn2Accumulator(new Brn2Model()).update(board, board, parent));
        assertThrows(IllegalStateException.class, () -> new Brn2Accumulator(MODEL).evaluate(board));
    }

    @Test void boundedSingleAndSixThreadSearchMatchesFullReference() {
        for (int threads : new int[]{1, 6}) {
            var board = Board.startingPosition();
            SearchResult reference = search(board, SearchEvaluation.brn2FullRecompute(MODEL), threads);
            SearchResult actual = search(board, SearchEvaluation.brn2(MODEL), threads);
            assertEquals(reference.score(), actual.score()); assertEquals(reference.bestMove(), actual.bestMove());
            if (threads == 1) { assertEquals(reference.nodes(), actual.nodes()); assertArrayEquals(reference.principalVariation(), actual.principalVariation()); }
        }
    }

    @Test void extremeFiniteModelsKeepCheckedFullRecomputationAndOverflowRejection() {
        double[] weights = MODEL.copyWeights(); weights[Brn2Model.OUTPUT_BIAS] = 1e101;
        var model = new Brn2Model(weights); assertFalse(model.boundedIntermediates());
        var board = Board.startingPosition(); var state = SearchEvaluation.brn2(model).newState(2);
        state.initialize(board, 0);
        assertEquals(BrnScoreMapping.map(model.evaluateReference(board, new Brn2Workspace())), state.evaluate(board, 0));
        Arrays.fill(weights, Double.MAX_VALUE); model = new Brn2Model(weights);
        var huge = model;
        assertThrows(ArithmeticException.class, () -> huge.evaluate(board, new Brn2Workspace()));
        assertThrows(ArithmeticException.class, () -> huge.evaluateReference(board, new Brn2Workspace()));
    }

    private static SearchResult search(long[] board, SearchEvaluation eval, int threads) {
        try (var search = threads == 1 ? new AlphaBetaPvsSearch(eval, 1 << 16) : new RootParallelSearch(threads, eval, 1 << 16)) {
            var result = search.search(new SearchRequest(board, GameHistory.initial(board), 2, SearchObserver.NONE,
                    SearchControl.controlled(20_000, 0, -1, () -> 0)));
            assertTrue(result.completed()); return result;
        }
    }
    private void check(Brn2Model model, long[] board, Brn2Accumulator accumulator) {
        var scratch = new Brn2Workspace();
        double expected = model.evaluateReference(board, scratch), actual = accumulator.evaluate(board);
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(model.evaluate(board, scratch)), "Full hot path must remain bit exact");
        maxError = Math.max(maxError, Math.abs(actual - expected)); comparisons++;
        assertEquals(expected, actual, TOLERANCE); assertEquals(BrnScoreMapping.map(expected), BrnScoreMapping.map(actual));
    }
    private static long[] moves(long[] board) {
        long[] moves = new long[256], scratch = new long[256];
        return Arrays.copyOf(moves, Gen.genAll(board[0], board[1], board[2], board[3], (int)board[4], board[5], true, moves, scratch));
    }
    private static long[] play(long[] board, long move) {
        long[] child = new long[6]; Board.makeMoveInto(board[0], board[1], board[2], board[3], (int)board[4], board[5], move, child); return child;
    }
}
