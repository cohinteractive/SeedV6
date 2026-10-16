package com.ohinteractive.seedv6.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

class SeeThresholdTest {

    @Test
    void focusedTacticalEdgesMatchIndependentLegalOracleAtThresholdBoundaries() {
        // Reuse the established SEE fixtures, with the legal oracle as the authority.
        String[][] cases = {
            // Winning, equal, losing and multi-piece exchanges.
            {"4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1", "e4d5"},
            {"rq2k3/8/8/8/8/8/8/R3K3 w - - 0 1", "a1a8"},
            {"3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1", "d1d5"},
            // Rook, bishop, queen and successive occupancy-dependent x-rays.
            {"4k3/6b1/8/8/3p4/8/3Q4/3RK3 w - - 0 1", "d2d4"},
            {"4k3/8/6b1/8/8/3r4/2Q5/1B2K3 w - - 0 1", "c2d3"},
            {"4k3/8/8/5n2/3p4/4B3/5Q2/4K3 w - - 0 1", "e3d4"},
            {"7k/3r4/3q4/3r4/3p4/3R4/3Q4/K2R4 w - - 0 1", "d3d4"},
            // Absolute pins and dynamic exposure of the recapturing side's king.
            {"4k3/3n4/8/1B2p3/5P2/8/8/4K3 w - - 0 1", "f4e5"},
            {"4r1k1/6b1/8/8/3p4/2P5/4N3/4K3 w - - 0 1", "c3d4"},
            {"k3r3/8/5n2/1B6/8/8/4R3/4K3 w - - 0 1", "b5e8"},
            // Initial king capture, safe/unsafe king recaptures and king adjacency.
            {"4k3/8/8/8/8/8/4r3/4K3 w - - 0 1", "e1e2"},
            {"8/8/4k3/3r4/8/8/8/3RK3 w - - 0 1", "d1d5"},
            {"8/8/4k3/3r4/8/1B6/8/3RK3 w - - 0 1", "d1d5"},
            {"8/8/8/4k3/3p4/4B3/5Q2/4K3 w - - 0 1", "e3d4"},
            // EP removes a pawn away from the destination, exposing a rook ray.
            {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6"},
            {"4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1", "e5d6"},
            {"4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1", "e4d3"},
            // Promotion in a later recapture and already-promoted material.
            {"R3k3/1P6/8/8/8/8/r7/4K3 b - - 0 1", "a2a8"},
            {"4k3/8/4p3/3Q4/8/8/8/4K3 b - - 0 1", "e6d5"},
            // Colour mirror of a discovered bishop exchange.
            {"1b2k3/2q5/3R4/8/8/6B1/8/4K3 b - - 0 1", "c7d6"}
        };
        int comparisons = 0;
        for (String[] fixture : cases) comparisons += assertFixture(fixture[0], fixture[1]);
        System.out.println("threshold SEE focused: moves=" + cases.length + " comparisons=" + comparisons);
    }

    @Test
    void everyPromotionChoiceInBothColoursMatchesIndependentOracle() {
        String[][] positions = {
            {"7k/P7/8/8/8/8/8/K7 w - - 0 1", "a7a8"},
            {"k7/8/8/8/8/8/p7/7K b - - 0 1", "a2a1"},
            {"4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8"},
            {"4k3/8/8/8/8/8/6p1/4K2R b - - 0 1", "g2h1"},
            // Promotion type changes subsequent checking and recapture possibilities.
            {"r6b/4k1P1/8/8/8/8/8/4K2R w - - 0 1", "g7h8"},
            {"4k2r/8/8/8/8/8/4K1p1/R6B b - - 0 1", "g2h1"}
        };
        int comparisons = 0;
        for (String[] position : positions) {
            for (String suffix : new String[] {"q", "r", "b", "n"}) {
                comparisons += assertFixture(position[0], position[1] + suffix);
            }
        }
        System.out.println("threshold SEE promotions: moves=24 comparisons=" + comparisons);
    }

    @Test
    void decliningLosingRecapturesAndChoosingLaterBetterRecapturesAreNecessary() {
        int pawn = Eval.exchangeValue(Piece.PAWN);
        int knight = Eval.exchangeValue(Piece.KNIGHT);
        int rook = Eval.exchangeValue(Piece.ROOK);
        int queen = Eval.exchangeValue(Piece.QUEEN);

        long[] stopBoard = Board.fromFen("3qk3/8/8/3n4/4P3/8/8/3RK3 w - - 0 1");
        long stopMove = legalMove(stopBoard, "e4d5");
        long[] stopChild = child(stopBoard, stopMove);
        assertEquals(pawn - queen, SeeLegalOracle.evaluate(stopChild, legalMove(stopChild, "d8d5")));
        assertEquals(knight, SeeLegalOracle.evaluate(stopBoard, stopMove),
                "Black must be allowed to decline the losing queen recapture");
        int comparisons = assertThresholds(stopBoard, stopMove, knight, "optional stop");

        long[] choiceBoard = Board.fromFen("7k/8/3r1n2/3p4/4P3/8/8/K2Q4 w - - 0 1");
        long choiceMove = legalMove(choiceBoard, "d1d5");
        long[] choiceChild = child(choiceBoard, choiceMove);
        assertEquals(queen - rook + pawn,
                SeeLegalOracle.evaluate(choiceChild, legalMove(choiceChild, "d6d5")));
        assertEquals(queen - knight + pawn,
                SeeLegalOracle.evaluate(choiceChild, legalMove(choiceChild, "f6d5")));
        assertEquals(knight - queen, SeeLegalOracle.evaluate(choiceBoard, choiceMove),
                "The later knight recapture is better than the rook recapture");
        comparisons += assertThresholds(choiceBoard, choiceMove, knight - queen, "multiple recaptures");
        System.out.println("threshold SEE optional/branching: moves=2 comparisons=" + comparisons);
    }

    @Test
    void boundedDeterministicLegalGameCorpusMatchesOracle() {
        // Same legal-game construction as existing SEE tests, bounded to half as many games.
        Random random = new Random(0x5eed_0006L);
        long[] moves = new long[256];
        long[] scratch = new long[Board.MAX_BITBOARDS];
        int positions = 0;
        int evaluated = 0;
        int comparisons = 0;
        int positive = 0;
        int zero = 0;
        int negative = 0;
        for (int game = 0; game < 12; game++) {
            long[] board = Board.startingPosition();
            for (int ply = 0; ply < 80; ply++) {
                int count = Gen.genAll(board[0], board[1], board[2], board[3],
                        (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch);
                positions++;
                for (int i = 0; i < count; i++) {
                    long move = moves[i];
                    if (!isTactical(board, move)) continue;
                    int oracle = SeeLegalOracle.evaluate(board, move);
                    comparisons += assertThresholds(board, move, oracle,
                            "game=" + game + " ply=" + ply + " move=" + Move.coordinate(move));
                    evaluated++;
                    if (oracle > 0) positive++;
                    else if (oracle < 0) negative++;
                    else zero++;
                }
                if (count == 0) break;
                board = child(board, moves[random.nextInt(count)]);
            }
        }
        assertTrue(positions >= 500, "positions=" + positions);
        assertTrue(evaluated >= 1_000, "tactical moves=" + evaluated);
        assertTrue(positive > 0 && zero > 0 && negative > 0);
        System.out.println("threshold SEE corpus: positions=" + positions + " moves=" + evaluated
                + " comparisons=" + comparisons + " positive=" + positive
                + " zero=" + zero + " negative=" + negative);
    }

    private static int assertFixture(String fen, String coordinate) {
        long[] board = Board.fromFen(fen);
        long move = legalMove(board, coordinate);
        return assertThresholds(board, move, SeeLegalOracle.evaluate(board, move), fen + " " + coordinate);
    }

    private static int assertThresholds(long[] board, long move, int oracle, String context) {
        long[] before = board.clone();
        int pawn = Eval.exchangeValue(Piece.PAWN);
        int rook = Eval.exchangeValue(Piece.ROOK);
        int queen = Eval.exchangeValue(Piece.QUEEN);
        int[] thresholds = {Integer.MIN_VALUE, -2 * queen, -queen, -rook, -pawn,
                0, pawn, rook, queen, 2 * queen,
                oracle == Integer.MIN_VALUE ? oracle : oracle - 1, oracle,
                oracle == Integer.MAX_VALUE ? oracle : oracle + 1, Integer.MAX_VALUE};
        for (int threshold : thresholds) {
            assertEquals(oracle >= threshold, See.atLeastGeneratedLegal(board, move, threshold),
                    () -> context + " oracle=" + oracle + " threshold=" + threshold);
        }
        // Interleave early exits and recursive cases in reverse order to catch state leakage.
        for (int i = thresholds.length - 1; i >= 0; i--) {
            int threshold = thresholds[i];
            assertEquals(oracle >= threshold, See.atLeastGeneratedLegal(board, move, threshold),
                    () -> "repeat " + context + " oracle=" + oracle + " threshold=" + threshold);
        }
        assertArrayEquals(before, board, context);
        return 2 * thresholds.length;
    }

    private static boolean isTactical(long[] board, long move) {
        if (((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != Value.NONE
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != Value.NONE) return true;
        return ((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN
                && Move.toSquare(move) == Board.enPassantSquare((int) board[Board.STATUS]);
    }

    private static long legalMove(long[] board, String coordinate) {
        long[] moves = new long[256];
        int count = Gen.genAll(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                moves, new long[Board.MAX_BITBOARDS]);
        for (int i = 0; i < count; i++) {
            if (Move.coordinate(moves[i]).equals(coordinate)) return moves[i];
        }
        return fail("Expected legal move " + coordinate);
    }

    private static long[] child(long[] board, long move) {
        long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], move, child);
        return child;
    }
}
