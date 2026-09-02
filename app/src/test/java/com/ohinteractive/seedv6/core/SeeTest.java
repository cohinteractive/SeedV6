package com.ohinteractive.seedv6.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

class SeeTest {

    @Test
    void basicExchangesCoverPositiveZeroNegativeAndMultiPieceChains() {
        assertSee(975, "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1", "e4d5");
        assertSee(-875, "3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1", "d1d5");
        assertSee(0, "rq2k3/8/8/8/8/8/8/R3K3 w - - 0 1", "a1a8");
        assertSee(320, "3qk3/8/8/3n4/4P3/8/8/3RK3 w - - 0 1", "e4d5");
        assertSee(-400,
                "7k/3r4/3q4/3r4/3p4/3R4/3Q4/K2R4 w - - 0 1", "d3d4");
    }

    @Test
    void rookBishopQueenAndSuccessiveXraysRefreshAfterEveryRemoval() {
        assertSee(-545,
                "4k3/6b1/8/8/3p4/8/3Q4/3RK3 w - - 0 1", "d2d4");
        assertSee(-145,
                "4k3/8/6b1/8/8/3r4/2Q5/1B2K3 w - - 0 1", "c2d3");
        assertSee(90,
                "4k3/8/8/5n2/3p4/4B3/5Q2/4K3 w - - 0 1", "e3d4");
        assertSee(-400,
                "7k/3r4/3q4/3r4/3p4/3R4/3Q4/K2R4 w - - 0 1", "d3d4");
    }

    @Test
    void absolutePinsAndDynamicKingExposureControlRecaptures() {
        assertSee(100,
                "4k3/3n4/8/1B2p3/5P2/8/8/4K3 w - - 0 1", "f4e5");
        assertSee(0,
                "4r1k1/6b1/8/8/3p4/2P5/4N3/4K3 w - - 0 1", "c3d4");
        assertSee(490,
                "k3r3/8/5n2/1B6/8/8/4R3/4K3 w - - 0 1", "b5e8");
    }

    @Test
    void kingCapturesRequireAnActuallySafeDestinationAfterCurrentOccupancy() {
        assertSee(500,
                "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1", "e1e2");
        assertSee(0,
                "8/8/4k3/3r4/8/8/8/3RK3 w - - 0 1", "d1d5");
        assertSee(500,
                "8/8/4k3/3r4/8/1B6/8/3RK3 w - - 0 1", "d1d5");
        assertSee(100,
                "8/8/8/4k3/3p4/4B3/5Q2/4K3 w - - 0 1", "e3d4");
    }

    @Test
    void enPassantValuesThePawnAndRemovesItsNonDestinationOccupancy() {
        assertSee(100,
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6");
        assertSee(0,
                "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1", "e5d6");

        long[] illegal = board("8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1");
        long pseudoEnPassant = encodedMove(illegal, "c5d6", Value.NONE);
        assertThrows(IllegalArgumentException.class,
                () -> See.evaluate(illegal, pseudoEnPassant));
    }

    @Test
    void promotionsCoverBothColoursEveryTypeCapturesAndRecaptureRisk() {
        int[] types = {Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT};
        String[] suffixes = {"q", "r", "b", "n"};
        for (int i = 0; i < types.length; i++) {
            int delta = Eval.exchangeValue(types[i]) - Eval.exchangeValue(Piece.PAWN);
            assertSee(delta, "7k/P7/8/8/8/8/8/K7 w - - 0 1", "a7a8" + suffixes[i]);
            assertSee(delta, "k7/8/8/8/8/8/p7/7K b - - 0 1", "a2a1" + suffixes[i]);
        }

        assertSee(1_375,
                "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8q");
        assertSee(720,
                "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8n");
        assertSee(1_375,
                "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1", "g2h1q");
        assertSee(730,
                "r6b/4k1P1/8/8/8/8/8/4K2R w - - 0 1", "g7h8q");
        assertSee(550,
                "r6b/4k1P1/8/8/8/8/8/4K2R w - - 0 1", "g7h8n");
        assertNotEquals(see("r6b/4k1P1/8/8/8/8/8/4K2R w - - 0 1", "g7h8q"),
                see("r6b/4k1P1/8/8/8/8/8/4K2R w - - 0 1", "g7h8n"));
    }

    @Test
    void laterPromotingRecaptureIncludesItsCreationGainRatherThanDonorQueenShortcut() {
        assertSee(-875,
                "R3k3/1P6/8/8/8/8/r7/4K3 b - - 0 1", "a2a8");
    }

    @Test
    void colourMirrorsHaveTheSameMoverRelativeResult() {
        assertEquals(
                see("4k3/8/6b1/8/8/3r4/2Q5/1B2K3 w - - 0 1", "c2d3"),
                see("1b2k3/2q5/3R4/8/8/6B1/8/4K3 b - - 0 1", "c7d6"));
        assertEquals(
                see("4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1", "e5d6"),
                see("4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1", "e4d3"));
    }

    @Test
    void evaluationIsPureDeterministicAndRejectsNonTacticalQuietMoves() {
        long[] position = board("4k3/8/8/3q4/4P3/8/8/4K3 w - - 7 19");
        long[] before = position.clone();
        long move = legalMove(position, "e4d5");
        int first = See.evaluate(position, move);
        int second = See.evaluate(position, move);
        assertEquals(first, second);
        assertArrayEquals(before, position);

        long[] quiet = board("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1");
        assertThrows(IllegalArgumentException.class,
                () -> See.evaluate(quiet, legalMove(quiet, "e4e5")));
    }

    @Test
    void adaptedDonorIntentionsMatchIndependentLegalOracle() {
        String[][] cases = {
            {"4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1", "e4d5"},
            {"3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1", "d1d5"},
            {"rq2k3/8/8/8/8/8/8/R3K3 w - - 0 1", "a1a8"},
            {"4k3/8/6b1/8/8/3r4/2Q5/1B2K3 w - - 0 1", "c2d3"},
            {"3qk3/8/8/3n4/4P3/8/8/3RK3 w - - 0 1", "e4d5"},
            {"4k3/3n4/8/1B2p3/5P2/8/8/4K3 w - - 0 1", "f4e5"},
            {"4k3/8/8/8/8/8/4r3/4K3 w - - 0 1", "e1e2"},
            {"4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8q"},
            {"4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", "g7h8n"},
            {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6"},
            {"8/8/4k3/3r4/8/1B6/8/3RK3 w - - 0 1", "d1d5"}
        };
        for (String[] testCase : cases) {
            assertOracle(testCase[0], testCase[1]);
        }
    }

    @Test
    void deterministicLegalCorpusMatchesOracleExactly() {
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
                        assertEquals(SeeLegalOracle.evaluate(position, move),
                                See.evaluate(position, move),
                                () -> "SEE mismatch at " + Move.coordinate(move));
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

    private static void assertSee(int expected, String fen, String uci) {
        long[] position = board(fen);
        long move = legalMove(position, uci);
        assertEquals(expected, See.evaluate(position, move), fen + " " + uci);
        assertEquals(expected, SeeLegalOracle.evaluate(position, move),
                "oracle " + fen + " " + uci);
    }

    private static void assertOracle(String fen, String uci) {
        long[] position = board(fen);
        long move = legalMove(position, uci);
        assertEquals(SeeLegalOracle.evaluate(position, move), See.evaluate(position, move),
                fen + " " + uci);
    }

    private static int see(String fen, String uci) {
        long[] position = board(fen);
        return See.evaluate(position, legalMove(position, uci));
    }

    private static long[] board(String fen) {
        return Board.fromFen(fen);
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

    private static long encodedMove(long[] board, String uci, int promoteType) {
        int from = square(uci.substring(0, 2));
        int target = square(uci.substring(2, 4));
        int startPiece = Board.getSquare(board[0], board[1], board[2], board[3], from);
        int targetPiece = Board.getSquare(board[0], board[1], board[2], board[3], target);
        int playerBit = (Math.toIntExact(board[Board.STATUS]) & Board.PLAYER_BIT)
                << Board.PLAYER_SHIFT;
        int promotePiece = promoteType == Value.NONE ? Value.NONE : promoteType | playerBit;
        return from | ((long) target << Board.TARGET_SQUARE_SHIFT)
                | ((long) promotePiece << Board.PROMOTE_PIECE_SHIFT)
                | ((long) startPiece << Board.START_PIECE_SHIFT)
                | ((long) targetPiece << Board.TARGET_PIECE_SHIFT);
    }

    private static int square(String coordinate) {
        int file = Value.FILE_STRING.indexOf(coordinate.charAt(0));
        int rank = coordinate.charAt(1) - '1';
        return file | rank << 3;
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
