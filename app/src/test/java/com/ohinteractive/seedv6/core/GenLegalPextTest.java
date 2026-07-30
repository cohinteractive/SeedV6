package com.ohinteractive.seedv6.core;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GenLegalPextTest {

    private static final int MAX_MOVES = 256;

    private static final String[] RULE_POSITIONS = {
        Board.FEN_STARTING_POSITION,
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        "4r2k/8/8/8/8/8/8/R3K3 w - - 0 1",
        "4r2k/8/8/8/1b6/8/8/4K3 w - - 0 1",
        "4r2k/8/8/8/8/8/4N3/4K3 w - - 0 1",
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

    @Test
    void matchesMagicLegalMoveCountsAndEncodedSequence() {
        for(String fen : RULE_POSITIONS) {
            long[] board = Board.fromFen(fen);
            long[] magicMoves = new long[MAX_MOVES];
            long[] pextMoves = new long[MAX_MOVES];
            int magicCount = genAllMagic(board, magicMoves);
            int pextCount = genAllPext(board, pextMoves);

            assertEquals(magicCount, pextCount, fen);
            assertArrayEquals(
                Arrays.copyOf(magicMoves, magicCount),
                Arrays.copyOf(pextMoves, pextCount),
                fen
            );
        }
    }

    @Test
    void matchesMagicTacticalQuietAndEvasionSequences() {
        for(String fen : RULE_POSITIONS) {
            long[] board = Board.fromFen(fen);
            assertStageMatches(board, fen, true);
            assertStageMatches(board, fen, false);

            long magicCheckers = getMagicCheckers(board);
            long pextCheckers = getPextCheckers(board);
            assertEquals(magicCheckers, pextCheckers, fen);
            if(magicCheckers != 0L) {
                long[] magicMoves = new long[MAX_MOVES];
                long[] pextMoves = new long[MAX_MOVES];
                int magicCount = GenLegal.genEvasion(
                    board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], true,
                    magicCheckers, magicMoves, new long[Board.MAX_BITBOARDS]
                );
                int pextCount = GenLegalPext.genEvasion(
                    board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], true,
                    pextCheckers, pextMoves, new long[Board.MAX_BITBOARDS]
                );
                assertEquals(magicCount, pextCount, fen);
                assertArrayEquals(
                    Arrays.copyOf(magicMoves, magicCount),
                    Arrays.copyOf(pextMoves, pextCount),
                    fen
                );
            }
        }
    }

    private static void assertStageMatches(long[] board, String fen, boolean tactical) {
        long[] magicMoves = new long[MAX_MOVES];
        long[] pextMoves = new long[MAX_MOVES];
        int magicCount = tactical
            ? GenLegal.genTactical(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                magicMoves, new long[Board.MAX_BITBOARDS]
            )
            : GenLegal.genQuiet(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                magicMoves, new long[Board.MAX_BITBOARDS]
            );
        int pextCount = tactical
            ? GenLegalPext.genTactical(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                pextMoves, new long[Board.MAX_BITBOARDS]
            )
            : GenLegalPext.genQuiet(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                pextMoves, new long[Board.MAX_BITBOARDS]
            );
        String context = (tactical ? "tactical: " : "quiet: ") + fen;
        assertEquals(magicCount, pextCount, context);
        assertArrayEquals(
            Arrays.copyOf(magicMoves, magicCount),
            Arrays.copyOf(pextMoves, pextCount),
            context
        );
    }

    private static int genAllMagic(long[] board, long[] moves) {
        return GenLegal.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
    }

    private static int genAllPext(long[] board, long[] moves) {
        return GenLegalPext.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
    }

    private static long getMagicCheckers(long[] board) {
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

    private static long getPextCheckers(long[] board) {
        int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        long allOccupancy = board[0] | board[1] | board[2];
        long colorMask = ~(-(player) ^ board[3]);
        long king = board[0] & ~board[1] & ~board[2] & colorMask;
        int kingSquare = Long.numberOfTrailingZeros(king);
        return Board.getCheckersPext(
            board[0], board[1], board[2], board[3],
            colorMask, player, kingSquare, allOccupancy
        );
    }
}
