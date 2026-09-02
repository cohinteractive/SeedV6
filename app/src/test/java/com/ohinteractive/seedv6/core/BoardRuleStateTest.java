package com.ohinteractive.seedv6.core;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardRuleStateTest {

    @Test
    void fenSeedsHalfmoveValuesNinetyNineAndOneHundred() {
        assertEquals(99, halfmove(Board.fromFen(Board.FEN_STARTING_POSITION.replace(" 0 1", " 99 1"))));
        assertEquals(100, halfmove(Board.fromFen(Board.FEN_STARTING_POSITION.replace(" 0 1", " 100 1"))));
    }

    @Test
    void reversibleMoveIncrementsAndBlackMoveAdvancesFullmoveNumber() {
        final long[] white = Board.fromFen(Board.FEN_STARTING_POSITION.replace(" 0 1", " 99 1"));
        final long[] afterWhite = play(white, "g1f3");
        assertEquals(100, halfmove(afterWhite));
        assertEquals(1, fullmove(afterWhite));

        final long[] afterBlack = play(afterWhite, "g8f6");
        assertEquals(101, halfmove(afterBlack));
        assertEquals(2, fullmove(afterBlack));
    }

    @Test
    void pawnMoveAndCaptureResetHalfmoveClock() {
        final long[] pawnRoot = Board.fromFen(Board.FEN_STARTING_POSITION.replace(" 0 1", " 99 1"));
        assertEquals(0, halfmove(play(pawnRoot, "e2e4")));

        final long[] captureRoot = Board.fromFen("r3k3/8/8/8/8/8/8/R3K3 w - - 99 1");
        assertEquals(0, halfmove(play(captureRoot, "a1a8")));
    }

    @Test
    void packedHalfmoveClockSaturatesInsteadOfWrappingOrSpilling() {
        final long[] atLimit = Board.fromFen(
            Board.FEN_STARTING_POSITION.replace(" 0 1", " 127 1")
        );
        final long[] after = play(atLimit, "g1f3");
        assertEquals(Board.MAX_HALF_MOVE_CLOCK, halfmove(after));
        assertEquals(1, fullmove(after));

        final long[] oversizedFen = Board.fromFen(
            Board.FEN_STARTING_POSITION.replace(" 0 1", " 200 1")
        );
        assertEquals(Board.MAX_HALF_MOVE_CLOCK, halfmove(oversizedFen));
        assertEquals(1, fullmove(oversizedFen));
    }

    @Test
    void nullTransitionPreservesClockAndIsNotAnActualPly() {
        final long[] board = Board.fromFen("4k3/8/8/8/3pP3/8/8/4K3 b - e3 99 7");
        final long[] nullBoard = new long[Board.MAX_BITBOARDS];
        Board.nullMoveInto(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], nullBoard
        );

        assertEquals(99, halfmove(nullBoard));
        assertEquals(7, fullmove(nullBoard));
    }

    private static int halfmove(long[] board) {
        return Board.halfMoveClock((int) board[Board.STATUS]);
    }

    private static int fullmove(long[] board) {
        return Board.fullMoveNumber((int) board[Board.STATUS]);
    }

    private static long[] play(long[] board, String coordinate) {
        final long move = new LegalMoveResolver().resolve(
            board,
            new MoveIntent(
                square(coordinate.substring(0, 2)),
                square(coordinate.substring(2, 4)),
                MoveIntent.Promotion.NONE
            )
        );
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], move, child
        );
        return child;
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }
}
