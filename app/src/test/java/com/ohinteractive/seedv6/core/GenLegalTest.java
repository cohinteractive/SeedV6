package com.ohinteractive.seedv6.core;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.util.Piece;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class GenLegalTest {

    private static final int MAX_MOVES = 256;

    @Test
    void matchesAuthoritativeMoveSetsForTargetedPositions() {
        String[] fens = {
            Board.FEN_STARTING_POSITION,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "4r2k/8/8/8/8/8/8/R3K3 w - - 0 1",
            "7k/8/8/8/1b6/8/8/4K3 w - - 0 1",
            "7k/8/8/8/8/8/4q3/R3K3 w - - 0 1",
            "7k/8/8/8/8/5n2/8/4K3 w - - 0 1",
            "7k/8/8/8/8/8/3p4/4K3 w - - 0 1",
            "4r2k/8/8/8/1b6/8/8/4K3 w - - 0 1",
            "4r2k/8/8/8/8/8/4N3/4K3 w - - 0 1",
            "4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1",
            "4r2k/8/8/8/8/8/4P3/4K3 w - - 0 1",
            "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1",
            "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1",
            "7k/8/8/r4pPK/8/8/8/8 w - f6 0 1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
            "r3k2r/8/8/8/2b5/8/8/R3K2R w KQkq - 0 1",
            "7k/P7/8/8/8/8/8/4K3 w - - 0 1",
            "1r5k/P7/8/8/8/8/8/4K3 w - - 0 1",
            "7k/8/8/8/8/6b1/5n2/4K3 w - - 0 1",
            "7k/8/8/8/8/8/4K2r/8 w - - 0 1"
        };
        for(String fen : fens) assertMatches(fen);
    }

    @Test
    void tacticalAndQuietPartitionEveryLegalMove() {
        String[] fens = {
            Board.FEN_STARTING_POSITION,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "4r2k/8/8/8/8/8/8/R3K3 w - - 0 1",
            "4r2k/8/8/8/1b6/8/8/4K3 w - - 0 1",
            "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1",
            "7k/P7/8/8/8/8/8/4K3 w - - 0 1"
        };
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            long[] all = new long[MAX_MOVES];
            long[] tactical = new long[MAX_MOVES];
            long[] quiet = new long[MAX_MOVES];
            int allCount = directAll(board, all);
            int tacticalCount = GenLegal.genTactical(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                tactical, new long[Board.MAX_BITBOARDS]
            );
            int quietCount = GenLegal.genQuiet(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                quiet, new long[Board.MAX_BITBOARDS]
            );
            long[] staged = new long[tacticalCount + quietCount];
            System.arraycopy(tactical, 0, staged, 0, tacticalCount);
            System.arraycopy(quiet, 0, staged, tacticalCount, quietCount);
            assertEquals(staged.length, Arrays.stream(staged).distinct().count(), fen);
            assertArrayEquals(sorted(all, allCount), sorted(staged, staged.length), fen);
        }
    }

    @Test
    void doubleCheckEmitsKingMovesOnly() {
        long[] board = Board.fromFen("4r2k/8/8/8/1b6/8/8/4K3 w - - 0 1");
        long[] moves = new long[MAX_MOVES];
        int count = directAll(board, moves);
        for(int i = 0; i < count; i ++) {
            assertEquals(Piece.KING, (int) moves[i] >>> Board.START_PIECE_SHIFT & Board.PIECE_BITS);
        }
    }

    @Test
    void handlesExplicitPinsEnPassantCastlingPromotionsAndKingSafetyCases() {
        assertNoMove("4r2k/8/8/8/8/8/4N3/4K3 w - - 0 1", "e2");

        assertNoMove("8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1", "c5d6");
        assertHasMove("8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1", "c4d3");
        assertNoMove("7k/8/8/r4pPK/8/8/8/8 w - f6 0 1", "g5f6");

        assertHasMove("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1");
        assertHasMove("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1c1");
        assertNoMove("r3k2r/8/8/8/2b5/8/8/R3K2R w KQkq - 0 1", "e1g1");
        assertNoMove("4k3/8/8/8/8/8/8/4K3 w KQ - 0 1", "e1g1");
        assertNoMove("4k3/8/8/8/8/8/8/4K3 w KQ - 0 1", "e1c1");

        assertHasMove("7k/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8q");
        assertHasMove("7k/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8r");
        assertHasMove("7k/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8b");
        assertHasMove("7k/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8n");
        assertHasMove("1r5k/P7/8/8/8/8/8/4K3 w - - 0 1", "a7b8q");

        assertNoMove("7k/8/8/8/8/6b1/5n2/4K3 w - - 0 1", "e1f2");
        assertNoMove("7k/8/8/8/8/8/4K2r/8 w - - 0 1", "e2d2");
    }

    @Test
    void deterministicPlayedPositionsMatchAuthoritativeGenerator() {
        Random random = new Random(0x5eed600dL);
        int comparedPositions = 0;
        for(int game = 0; game < 12; game ++) {
            long[] board = Board.startingPosition();
            long[] nextBoard = new long[Board.MAX_BITBOARDS];
            for(int ply = 0; ply < 160; ply ++) {
                long[] authoritative = new long[MAX_MOVES];
                long[] direct = new long[MAX_MOVES];
                long[] tactical = new long[MAX_MOVES];
                long[] quiet = new long[MAX_MOVES];
                int authoritativeCount = authoritative(board, authoritative);
                int directCount = directAll(board, direct);
                assertArrayEquals(
                    sorted(authoritative, authoritativeCount),
                    sorted(direct, directCount),
                    "game " + game + ", ply " + ply + "\n" + Board.boardString(board[0], board[1], board[2], board[3])
                );
                assertEquals(directCount, Arrays.stream(direct, 0, directCount).distinct().count());
                int tacticalCount = GenLegal.genTactical(
                    board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], true,
                    tactical, new long[Board.MAX_BITBOARDS]
                );
                int quietCount = GenLegal.genQuiet(
                    board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], true,
                    quiet, new long[Board.MAX_BITBOARDS]
                );
                long[] staged = new long[tacticalCount + quietCount];
                System.arraycopy(tactical, 0, staged, 0, tacticalCount);
                System.arraycopy(quiet, 0, staged, tacticalCount, quietCount);
                assertArrayEquals(sorted(direct, directCount), sorted(staged, staged.length));
                if(getCheckers(board) != 0L) {
                    long[] evasions = new long[MAX_MOVES];
                    int evasionCount = GenLegal.genEvasion(
                        board[0], board[1], board[2], board[3],
                        (int) board[Board.STATUS], board[Board.KEY], true,
                        getCheckers(board), evasions, new long[Board.MAX_BITBOARDS]
                    );
                    assertArrayEquals(sorted(direct, directCount), sorted(evasions, evasionCount));
                }
                comparedPositions ++;
                if(authoritativeCount == 0) break;
                long move = authoritative[random.nextInt(authoritativeCount)];
                Board.makeMoveInto(
                    board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], move, nextBoard
                );
                long[] swap = board;
                board = nextBoard;
                nextBoard = swap;
            }
        }
        assertTrue(comparedPositions >= 1000, "Only " + comparedPositions + " positions were compared");
    }

    @Test
    void directPerftMatchesStandardPositions() {
        assertEquals(4865609L, perft(Board.fromFen(Board.FEN_STARTING_POSITION), 5));
        assertEquals(4085603L, perft(Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"), 4));
        assertEquals(43238L, perft(Board.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"), 4));
        assertEquals(9467L, perft(Board.fromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"), 3));
        assertEquals(62379L, perft(Board.fromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"), 3));
        assertEquals(89890L, perft(Board.fromFen("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"), 3));
    }

    private static void assertMatches(String fen) {
        long[] board = Board.fromFen(fen);
        long[] authoritative = new long[MAX_MOVES];
        long[] direct = new long[MAX_MOVES];
        int authoritativeCount = authoritative(board, authoritative);
        int directCount = directAll(board, direct);
        assertArrayEquals(sorted(authoritative, authoritativeCount), sorted(direct, directCount), fen);
        assertFalse(hasDuplicates(direct, directCount), fen);

        long checkers = getCheckers(board);
        if(checkers != 0L) {
            long[] evasions = new long[MAX_MOVES];
            int evasionCount = GenLegal.genEvasion(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                checkers, evasions, new long[Board.MAX_BITBOARDS]
            );
            assertArrayEquals(sorted(direct, directCount), sorted(evasions, evasionCount), fen);
        }
    }

    private static int authoritative(long[] board, long[] moves) {
        return Gen.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
    }

    private static int directAll(long[] board, long[] moves) {
        return GenLegal.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
    }

    private static long getCheckers(long[] board) {
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        long allOccupancy = board[0] | board[1] | board[2];
        long colorMask = ~(-(player) ^ board[3]);
        long king = board[0] & ~board[1] & ~board[2] & colorMask;
        int kingSquare = Long.numberOfTrailingZeros(king);
        return Board.getCheckers(
            board[0], board[1], board[2], board[3],
            colorMask, player, kingSquare, allOccupancy
        );
    }

    private static long perft(long[] board, int depth) {
        long[][] boardStack = new long[depth + 1][Board.MAX_BITBOARDS];
        long[][] moveStack = new long[depth + 1][MAX_MOVES];
        long[] boardBuffer = new long[Board.MAX_BITBOARDS];
        return perft(board, depth, 0, boardStack, moveStack, boardBuffer);
    }

    private static long perft(
        long[] board, int depth, int ply,
        long[][] boardStack, long[][] moveStack, long[] boardBuffer
    ) {
        if(depth == 0) return 1L;
        long[] moves = moveStack[ply];
        int count = GenLegal.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, boardBuffer
        );
        if(depth == 1) return count;
        long nodes = 0L;
        long[] nextBoard = boardStack[ply + 1];
        for(int i = 0; i < count; i ++) {
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], moves[i], nextBoard
            );
            nodes += perft(nextBoard, depth - 1, ply + 1, boardStack, moveStack, boardBuffer);
        }
        return nodes;
    }

    private static long[] sorted(long[] moves, int count) {
        long[] sorted = Arrays.copyOf(moves, count);
        Arrays.sort(sorted);
        return sorted;
    }

    private static boolean hasDuplicates(long[] moves, int count) {
        long[] sorted = sorted(moves, count);
        for(int i = 1; i < sorted.length; i ++) {
            if(sorted[i - 1] == sorted[i]) return true;
        }
        return false;
    }

    private static void assertHasMove(String fen, String expectedMove) {
        long[] board = Board.fromFen(fen);
        long[] moves = new long[MAX_MOVES];
        int count = directAll(board, moves);
        for(int i = 0; i < count; i ++) {
            if(moveString(moves[i]).equals(expectedMove)) return;
        }
        fail(expectedMove + " missing from " + fen);
    }

    private static void assertNoMove(String fen, String rejectedMove) {
        long[] board = Board.fromFen(fen);
        long[] moves = new long[MAX_MOVES];
        int count = directAll(board, moves);
        for(int i = 0; i < count; i ++) {
            assertFalse(moveString(moves[i]).startsWith(rejectedMove), rejectedMove + " present in " + fen);
        }
    }

    private static String moveString(long move) {
        int startSquare = (int) move & Board.SQUARE_BITS;
        int targetSquare = (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS;
        int promotePiece = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Board.PIECE_BITS;
        String result = Board.squareToString(startSquare) + Board.squareToString(targetSquare);
        return result + switch(promotePiece & Piece.TYPE) {
            case Piece.QUEEN -> "q";
            case Piece.ROOK -> "r";
            case Piece.BISHOP -> "b";
            case Piece.KNIGHT -> "n";
            default -> "";
        };
    }

}
