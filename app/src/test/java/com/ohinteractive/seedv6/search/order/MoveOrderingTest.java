package com.ohinteractive.seedv6.search.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;

class MoveOrderingTest {

    @Test
    void killersAreEmptyDistinctRecentFirstPerPlyAndResettable() {
        final MoveOrdering ordering = new MoveOrdering(4);
        final long[] board = Board.startingPosition();
        final long e2e4 = move(board, "e2e4");
        final long d2d4 = move(board, "d2d4");
        final long g1f3 = move(board, "g1f3");

        assertEquals(StagedMovePicker.NO_MOVE, ordering.killer(0, 0));
        assertEquals(StagedMovePicker.NO_MOVE, ordering.killer(0, 1));

        assertTrue(ordering.recordQuietCutoff(board, 0, e2e4, 1));
        assertEquals(e2e4, ordering.killer(0, 0));
        assertEquals(StagedMovePicker.NO_MOVE, ordering.killer(0, 1));

        assertTrue(ordering.recordQuietCutoff(board, 0, e2e4, 1));
        assertEquals(e2e4, ordering.killer(0, 0));
        assertEquals(StagedMovePicker.NO_MOVE, ordering.killer(0, 1));

        assertTrue(ordering.recordQuietCutoff(board, 0, d2d4, 1));
        assertEquals(d2d4, ordering.killer(0, 0));
        assertEquals(e2e4, ordering.killer(0, 1));

        assertTrue(ordering.recordQuietCutoff(board, 0, e2e4, 1));
        assertEquals(e2e4, ordering.killer(0, 0));
        assertEquals(d2d4, ordering.killer(0, 1));

        assertTrue(ordering.recordQuietCutoff(board, 0, g1f3, 1));
        assertEquals(g1f3, ordering.killer(0, 0));
        assertEquals(e2e4, ordering.killer(0, 1));

        assertTrue(ordering.recordQuietCutoff(board, 1, d2d4, 1));
        assertEquals(d2d4, ordering.killer(1, 0));
        assertEquals(g1f3, ordering.killer(0, 0));

        ordering.reset();
        for(int ply = 0; ply < ordering.maxPly(); ply ++) {
            assertEquals(StagedMovePicker.NO_MOVE, ordering.killer(ply, 0));
            assertEquals(StagedMovePicker.NO_MOVE, ordering.killer(ply, 1));
        }
        assertEquals(0, ordering.historyScore(e2e4));
        assertEquals(0, ordering.historyScore(d2d4));
        assertEquals(0, ordering.historyScore(g1f3));
    }

    @Test
    void plyAndSlotBoundsAreSafeAndExplicit() {
        assertThrows(IllegalArgumentException.class, () -> new MoveOrdering(0));
        final MoveOrdering ordering = new MoveOrdering(2);
        final long[] board = Board.startingPosition();
        final long quiet = move(board, "e2e4");

        assertTrue(ordering.recordQuietCutoff(board, 0, quiet, 1));
        assertTrue(ordering.recordQuietCutoff(board, 1, quiet, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ordering.killer(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ordering.killer(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ordering.killer(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> ordering.killer(0, 2));
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> ordering.recordQuietCutoff(board, -1, quiet, 1)
        );
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> ordering.recordQuietCutoff(board, 2, quiet, 1)
        );
    }

    @Test
    void historyUsesBoundedOverflowProofPositiveUpdatesAndReset() {
        final MoveOrdering ordering = new MoveOrdering(2);
        final long[] board = Board.startingPosition();
        final long quiet = move(board, "e2e4");

        assertEquals(0, ordering.historyScore(quiet));
        assertTrue(ordering.recordQuietCutoff(board, 1, quiet, 3));
        assertEquals(9, ordering.historyScore(quiet));

        ordering.reset();
        for(int update = 1; update <= 4; update ++) {
            assertTrue(ordering.recordQuietCutoff(board, 1, quiet, 64));
            assertEquals(update * 4_096, ordering.historyScore(quiet));
        }
        assertEquals(MoveOrdering.HISTORY_LIMIT, ordering.historyScore(quiet));

        for(int update = 0; update < 10_000; update ++) {
            assertTrue(ordering.recordQuietCutoff(board, 1, quiet, Integer.MAX_VALUE));
        }
        assertEquals(MoveOrdering.HISTORY_LIMIT, ordering.historyScore(quiet));

        ordering.reset();
        assertTrue(ordering.recordQuietCutoff(board, 1, quiet, Integer.MAX_VALUE));
        assertEquals(4_096, ordering.historyScore(quiet));
        assertThrows(
            IllegalArgumentException.class,
            () -> ordering.recordQuietCutoff(board, 1, quiet, 0)
        );
    }

    @Test
    void historySeparatesSidePieceFromAndToDimensions() {
        final long[] whiteRookBoard = Board.fromFen(
            "6k1/8/8/8/8/8/8/R6K w - - 0 1"
        );
        final long[] whiteQueenBoard = Board.fromFen(
            "6k1/8/8/8/8/8/8/Q6K w - - 0 1"
        );
        final long[] blackRookBoard = Board.fromFen(
            "7K/8/8/8/8/8/8/r6k b - - 0 1"
        );
        final long rookA1A2 = move(whiteRookBoard, "a1a2");
        final long rookA1B1 = move(whiteRookBoard, "a1b1");
        final long queenA1A2 = move(whiteQueenBoard, "a1a2");
        final long blackRookA1A2 = move(blackRookBoard, "a1a2");
        final MoveOrdering ordering = new MoveOrdering(2);

        assertTrue(ordering.recordQuietCutoff(whiteRookBoard, 1, rookA1A2, 5));
        assertEquals(25, ordering.historyScore(rookA1A2));
        assertEquals(0, ordering.historyScore(rookA1B1), "to-square alias");
        assertEquals(0, ordering.historyScore(queenA1A2), "piece alias");
        assertEquals(0, ordering.historyScore(blackRookA1A2), "side alias");
    }

    @Test
    void capturesPromotionsAndEnPassantNeverEnterQuietHeuristics() {
        assertTacticalRejected(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1", "e4d5"
        );
        assertTacticalRejected(
            "7k/P7/8/8/8/8/8/K7 w - - 0 1", "a7a8q"
        );
        assertTacticalRejected(
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6"
        );
    }

    @Test
    void resetInvalidatesPreparedPickerFrames() {
        final MoveOrdering ordering = new MoveOrdering(2);
        ordering.picker().prepare(Board.startingPosition(), 0, StagedMovePicker.NO_MOVE);
        assertTrue(ordering.picker().moveCount(0) > 0);

        ordering.reset();

        assertThrows(IllegalStateException.class, () -> ordering.picker().moveCount(0));
        assertThrows(IllegalStateException.class, () -> ordering.picker().next(0));
    }

    private static void assertTacticalRejected(String fen, String coordinate) {
        final long[] board = Board.fromFen(fen);
        final long tactical = move(board, coordinate);
        final MoveOrdering ordering = new MoveOrdering(2);

        assertTrue(MoveOrdering.isTactical(board, tactical));
        assertFalse(ordering.recordQuietCutoff(board, 0, tactical, 8));
        assertEquals(0, ordering.historyScore(tactical));
        assertEquals(StagedMovePicker.NO_MOVE, ordering.killer(0, 0));
        assertEquals(StagedMovePicker.NO_MOVE, ordering.killer(0, 1));
    }

    private static long move(long[] board, String coordinate) {
        final Promotion promotion = coordinate.length() == 4 ? Promotion.NONE
            : switch(coordinate.charAt(4)) {
                case 'q' -> Promotion.QUEEN;
                case 'r' -> Promotion.ROOK;
                case 'b' -> Promotion.BISHOP;
                case 'n' -> Promotion.KNIGHT;
                default -> throw new IllegalArgumentException("Unknown promotion: " + coordinate);
            };
        return new LegalMoveResolver().resolve(
            board,
            new MoveIntent(square(coordinate.substring(0, 2)),
                square(coordinate.substring(2, 4)), promotion)
        );
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }
}
