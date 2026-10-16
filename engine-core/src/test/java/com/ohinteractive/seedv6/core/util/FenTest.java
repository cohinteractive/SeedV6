package com.ohinteractive.seedv6.core.util;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FenTest {

    @Test
    void acceptsACompleteSixFieldFenAndPreservesPackedCounterPolicy() {
        final long[] board = Board.fromFen("4k3/8/8/8/3pP3/8/8/4K3 b - e3 200 7");

        assertEquals(1, Board.player((int) board[Board.STATUS]));
        assertEquals(20, Board.enPassantSquare((int) board[Board.STATUS]));
        assertEquals(Board.MAX_HALF_MOVE_CLOCK, Board.halfMoveClock((int) board[Board.STATUS]));
        assertEquals(7, Board.fullMoveNumber((int) board[Board.STATUS]));
    }

    @Test
    void rejectsMalformedOrIncompleteFenFields() {
        assertInvalid("8/8/8/8/8/8/8/8 w - - 0");
        assertInvalid("8/8/8/8/8/8/8/8 w - - 0 1 extra");
        assertInvalid("8/8/8/8/8/8/8 w - - 0 1");
        assertInvalid("9/8/8/8/8/8/8/8 w - - 0 1");
        assertInvalid("7x/8/8/8/8/8/8/8 w - - 0 1");
        assertInvalid("8/8/8/8/8/8/8/8 x - - 0 1");
        assertInvalid("8/8/8/8/8/8/8/8 w KK - 0 1");
        assertInvalid("8/8/8/8/8/8/8/8 w - e4 0 1");
        assertInvalid("8/8/8/8/8/8/8/8 b - i3 0 1");
        assertInvalid("8/8/8/8/8/8/8/8 w - - -1 1");
        assertInvalid("8/8/8/8/8/8/8/8 w - - 0 0");
    }

    private static void assertInvalid(String fen) {
        assertThrows(IllegalArgumentException.class, () -> Board.fromFen(fen), fen);
    }
}
