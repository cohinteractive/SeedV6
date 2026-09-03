package com.ohinteractive.seedv6.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

class SeeGeneratedLegalTest {

    @Test
    void focusedGeneratedLegalCasesMatchPublicReferenceAndIndependentOracle() {
        String[][] cases = {
            // Winning, equal, losing, and successive x-ray exchanges.
            {"4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1", "e4d5"},
            {"rq2k3/8/8/8/8/8/8/R3K3 w - - 0 1", "a1a8"},
            {"3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1", "d1d5"},
            {"7k/3r4/3q4/3r4/3p4/3R4/3Q4/K2R4 w - - 0 1", "d3d4"},
            // Absolute pins and dynamic king exposure.
            {"4k3/3n4/8/1B2p3/5P2/8/8/4K3 w - - 0 1", "f4e5"},
            {"4r1k1/6b1/8/8/3p4/2P5/4N3/4K3 w - - 0 1", "c3d4"},
            {"k3r3/8/5n2/1B6/8/8/4R3/4K3 w - - 0 1", "b5e8"},
            // Initial king capture and legal/illegal king recapture continuations.
            {"4k3/8/8/8/8/8/4r3/4K3 w - - 0 1", "e1e2"},
            {"8/8/4k3/3r4/8/8/8/3RK3 w - - 0 1", "d1d5"},
            {"8/8/4k3/3r4/8/1B6/8/3RK3 w - - 0 1", "d1d5"},
            // En passant in both colours.
            {"4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1", "e5d6"},
            {"4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1", "e4d3"},
            // Later promoting recapture and capture of queen-valued promoted material.
            {"R3k3/1P6/8/8/8/8/r7/4K3 b - - 0 1", "a2a8"},
            {"4k3/8/4p3/3Q4/8/8/8/4K3 b - - 0 1", "e6d5"}
        };

        long[] scratch = new long[Board.MAX_BITBOARDS];
        for (String[] testCase : cases) {
            assertFastReferenceOracle(testCase[0], testCase[1], scratch);
        }
    }

    @Test
    void everyPromotionTypeForBothColoursMatchesReferencesForQuietAndCapturePromotions() {
        String[] suffixes = {"q", "r", "b", "n"};
        long[] scratch = new long[Board.MAX_BITBOARDS];
        for (String suffix : suffixes) {
            assertFastReferenceOracle(
                    "7k/P7/8/8/8/8/8/K7 w - - 0 1", "a7a8" + suffix, scratch);
            assertFastReferenceOracle(
                    "k7/8/8/8/8/8/p7/7K b - - 0 1", "a2a1" + suffix, scratch);
            assertFastReferenceOracle(
                    "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8" + suffix, scratch);
            assertFastReferenceOracle(
                    "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1", "g2h1" + suffix, scratch);
        }
    }

    @Test
    void generatedLegalPiecePlanesExactlyMatchFullBoardTransition() {
        String[][] cases = {
            {"4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1", "e4d5"},
            {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6"},
            {"4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8q"},
            {"4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8r"},
            {"4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8b"},
            {"4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8n"},
            {"k7/8/8/8/8/8/p7/7K b - - 0 1", "a2a1q"},
            {"k7/8/8/8/8/8/p7/7K b - - 0 1", "a2a1r"},
            {"k7/8/8/8/8/8/p7/7K b - - 0 1", "a2a1b"},
            {"k7/8/8/8/8/8/p7/7K b - - 0 1", "a2a1n"},
            {"4k3/8/8/8/8/8/4r3/4K3 w - - 0 1", "e1e2"}
        };

        long[] fast = new long[Board.MAX_BITBOARDS];
        long[] full = new long[Board.MAX_BITBOARDS];
        for (String[] testCase : cases) {
            long[] board = Board.fromFen(testCase[0]);
            long move = legalMove(board, testCase[1]);
            Arrays.fill(fast, 0x5eed_0006_dead_beefL);
            See.evaluateGeneratedLegal(board, move, fast);
            Board.makeMoveInto(board[0], board[1], board[2], board[3],
                    Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, full);
            for (int plane = 0; plane < 4; plane++) {
                assertEquals(full[plane], fast[plane],
                        testCase[0] + " " + testCase[1] + " plane=" + plane);
            }
            assertEquals(0x5eed_0006_dead_beefL, fast[Board.STATUS]);
            assertEquals(0x5eed_0006_dead_beefL, fast[Board.KEY]);
        }
    }

    @Test
    void boardPurityDeterminismAndScratchReuseDoNotLeakState() {
        String[][] sequence = {
            // En passant, ordinary capture, promotion, ordinary capture, alternating colours.
            {"4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1", "e5d6"},
            {"4k3/8/8/3q4/4P3/8/8/4K3 w - - 7 19", "e4d5"},
            {"7k/P7/8/8/8/8/8/K7 w - - 0 1", "a7a8n"},
            {"4k3/8/4p3/3Q4/8/8/8/4K3 b - - 0 1", "e6d5"},
            {"k7/8/8/8/8/8/p7/7K b - - 0 1", "a2a1r"},
            {"4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1", "e5d6"}
        };

        long[] scratch = new long[Board.MAX_BITBOARDS];
        for (String[] testCase : sequence) {
            long[] board = Board.fromFen(testCase[0]);
            long[] before = board.clone();
            long move = legalMove(board, testCase[1]);
            int publicFirst = See.evaluate(board, move);
            int publicSecond = See.evaluate(board, move);
            int fastFirst = See.evaluateGeneratedLegal(board, move, scratch);
            int fastSecond = See.evaluateGeneratedLegal(board, move, scratch);
            assertEquals(publicFirst, publicSecond, testCase[1]);
            assertEquals(publicFirst, fastFirst, testCase[1]);
            assertEquals(fastFirst, fastSecond, testCase[1]);
            assertEquals(SeeLegalOracle.evaluate(board, move), fastFirst, testCase[1]);
            assertArrayEquals(before, board, testCase[1]);
        }
    }

    @Test
    void deterministicGeneratedLegalCorpusMatchesPublicReferenceAndOracleExactly() {
        Random random = new Random(0x5eed_0006L);
        long[] moves = new long[256];
        long[] scratch = new long[Board.MAX_BITBOARDS];
        int positions = 0;
        int evaluated = 0;

        for (int game = 0; game < 24; game++) {
            long[] position = Board.startingPosition();
            for (int ply = 0; ply < 80; ply++) {
                int count = Gen.genAll(position[0], position[1], position[2], position[3],
                        Math.toIntExact(position[Board.STATUS]), position[Board.KEY], true,
                        moves, scratch);
                positions++;
                for (int i = 0; i < count; i++) {
                    long move = moves[i];
                    if (isCaptureOrPromotion(position, move)) {
                        int oracle = SeeLegalOracle.evaluate(position, move);
                        int reference = See.evaluate(position, move);
                        int fast = See.evaluateGeneratedLegal(position, move, scratch);
                        assertEquals(oracle, reference,
                                () -> "Reference mismatch at " + Move.coordinate(move));
                        assertEquals(oracle, fast,
                                () -> "Fast mismatch at " + Move.coordinate(move));
                        evaluated++;
                    }
                }
                if (count == 0) {
                    break;
                }
                long selected = moves[random.nextInt(count)];
                long[] next = new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(position[0], position[1], position[2], position[3],
                        Math.toIntExact(position[Board.STATUS]), position[Board.KEY],
                        selected, next);
                position = next;
            }
        }
        assertTrue(positions >= 1_000, "positions=" + positions);
        assertTrue(evaluated >= 1_000, "evaluated=" + evaluated);
    }

    private static void assertFastReferenceOracle(String fen, String uci, long[] scratch) {
        long[] board = Board.fromFen(fen);
        long move = legalMove(board, uci);
        int oracle = SeeLegalOracle.evaluate(board, move);
        assertEquals(oracle, See.evaluate(board, move), "reference " + fen + " " + uci);
        assertEquals(oracle, See.evaluateGeneratedLegal(board, move, scratch),
                "fast " + fen + " " + uci);
    }

    private static long legalMove(long[] board, String uci) {
        String expected = uci.toLowerCase(Locale.ROOT);
        long[] moves = new long[256];
        long[] scratch = new long[Board.MAX_BITBOARDS];
        int count = Gen.genAll(board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true, moves, scratch);
        for (int i = 0; i < count; i++) {
            if (Move.coordinate(moves[i]).equals(expected)) {
                return moves[i];
            }
        }
        fail("Legal move not generated: " + uci + "\n"
                + Board.boardString(board[0], board[1], board[2], board[3]));
        return 0L;
    }

    private static boolean isCaptureOrPromotion(long[] board, long move) {
        int targetPiece = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
        int promotePiece = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
        if (targetPiece != Value.NONE || promotePiece != Value.NONE) {
            return true;
        }
        int from = (int) move & Board.SQUARE_BITS;
        int target = (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
        int movingType = Board.getSquare(board[0], board[1], board[2], board[3], from)
                & Piece.TYPE;
        return movingType == Piece.PAWN
                && target == Board.enPassantSquare(Math.toIntExact(board[Board.STATUS]));
    }
}
