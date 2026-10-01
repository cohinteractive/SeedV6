package com.ohinteractive.seedv6.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.tools.perft.DefaultPerftPositionLibrary;

class LegalMoveExistenceTest {

    @Test
    void distinguishesStalemateAndExceptionalLegalResources() {
        String[] positions = {
            "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
            "8/8/b7/Pp6/8/8/1rk5/K7 w - b6 0 1", // En passant is the only move.
            "8/8/b7/Pp6/8/8/1rk5/K7 w - - 0 1", // Removing EP leaves stalemate.
            "8/8/8/r4pPK/8/8/8/k7 w - f6 0 1", // EP would expose the king.
            "7k/P7/8/8/8/8/8/K7 w - - 0 1",
            "k7/8/8/8/8/8/p7/7K b - - 0 1",
            "4r1k1/8/8/8/8/8/4N3/4K3 w - - 0 1",
            "4r1k1/8/8/8/8/8/4R3/4K3 w - - 0 1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        };
        for(String fen : positions) compare(Board.fromFen(fen));
        long[] ep = Board.fromFen(positions[1]);
        long[] moves = new long[512];
        assertEquals(1, generate(ep, moves));
        assertEquals("a5b6", Move.coordinate(moves[0]));
        assertFalse(exists(Board.fromFen(positions[2])));
    }

    @Test
    void matchesFullLegalGenerationOnPerftPositionsAndChildren() {
        for(var position : new DefaultPerftPositionLibrary().positions()) {
            long[] board = Board.fromFen(position.fen());
            compareUnlessChecked(board);
            long[] moves = new long[512];
            int count = generate(board, moves);
            for(int i = 0; i < count; i++) {
                long[] child = new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], moves[i], child);
                compareUnlessChecked(child);
            }
        }
    }

    @Test
    void matchesFullLegalGenerationThroughDeterministicLegalWalks() {
        Random random = new Random(3703);
        long[] moves = new long[512];
        for(int walk = 0; walk < 40; walk++) {
            long[] board = Board.startingPosition();
            for(int ply = 0; ply < 150; ply++) {
                compareUnlessChecked(board);
                int count = generate(board, moves);
                if(count == 0) break;
                long[] child = new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], moves[random.nextInt(count)], child);
                board = child;
            }
        }
    }

    private static boolean exists(long[] board) {
        return Gen.hasLegalMoveNotInCheck(board[0], board[1], board[2], board[3], (int) board[Board.STATUS]);
    }

    private static int generate(long[] board, long[] moves) {
        return Gen.genAll(board[0], board[1], board[2], board[3], (int) board[Board.STATUS],
            board[Board.KEY], true, moves, new long[Board.MAX_BITBOARDS]);
    }

    private static void compareUnlessChecked(long[] board) {
        if(!Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player((int) board[Board.STATUS]))) {
            compare(board);
        }
    }

    private static void compare(long[] board) {
        assertFalse(Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player((int) board[Board.STATUS])));
        assertEquals(generate(board, new long[512]) != 0, exists(board));
    }
}
