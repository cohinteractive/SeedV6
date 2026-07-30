package com.ohinteractive.seedv6.tools;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerftPextTest {

    @Test
    void matchesKnownStandardPerftResults() {
        assertKnown(Board.FEN_STARTING_POSITION, 4, 197281L);
        assertKnown(
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            3,
            97862L
        );
        assertKnown("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 4, 43238L);
        assertKnown("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 3, 9467L);
        assertKnown("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 3, 62379L);
        assertKnown("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10", 3, 89890L);
    }

    @Test
    void matchesMagicRecursionForRuleSpecificPositions() {
        assertParity("8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1", 4);
        assertParity("8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1", 4);
        assertParity("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", 3);
        assertParity("2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1", 4);
        assertParity("4r2k/8/8/8/1b6/8/8/4K3 w - - 0 1", 3);
        assertParity("4r2k/8/8/8/8/8/4N3/4K3 w - - 0 1", 3);
    }

    private static void assertKnown(String fen, int depth, long expectedNodes) {
        long[] board = Board.fromFen(fen);
        long magicNodes = Perft.perftFlat(board, depth);
        long pextNodes = PerftPext.perftFlat(board, depth);
        assertEquals(expectedNodes, magicNodes, "Magic: " + fen);
        assertEquals(expectedNodes, pextNodes, "PEXT: " + fen);
    }

    private static void assertParity(String fen, int depth) {
        long[] board = Board.fromFen(fen);
        long magicNodes = Perft.perftFlat(board, depth);
        long pextNodes = PerftPext.perftFlat(board, depth);
        assertEquals(magicNodes, pextNodes, fen);
    }
}
