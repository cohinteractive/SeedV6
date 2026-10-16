package com.ohinteractive.seedv6.tools.nnue;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NnueCpCalibrationTest {
    @Test void allFiveAnchorsAndCanonicalTransforms() {
        String[] pieces = {"P", "N", "B", "R", "Q"};
        int[] values = {100, 320, 330, 500, 900};
        for (int i = 0; i < pieces.length; i++) {
            long[] board = Board.fromFen("7k/8/8/8/8/8/" + pieces[i] + "7/7K w - - 0 1");
            assertEquals(values[i], NnueCpCalibration.material(board));
            assertEquals(-values[i], NnueCpCalibration.material(NnueCpCalibration.flip(board)));
            assertEquals(values[i], NnueCpCalibration.material(NnueCpCalibration.reflect(board)));
            assertArrayEquals(board, NnueCpCalibration.reflect(NnueCpCalibration.reflect(board)));
        }
        assertEquals(0, NnueCpCalibration.phase(Board.startingPosition()));
        assertEquals(24, NnueCpCalibration.phase(Board.fromFen("7k/8/8/8/8/8/P7/7K w - - 0 1")));
    }

    @Test void rejectsStateDamageAndTerminalControls() {
        assertEquals("castling", NnueCpCalibration.invalid(Board.fromFen("4k3/8/8/8/8/8/P7/4K3 w K - 0 1")));
        assertEquals("en_passant", NnueCpCalibration.invalid(Board.fromFen("7k/8/8/3pP3/8/8/8/7K w - d6 0 2")));
        assertEquals("check", NnueCpCalibration.invalid(Board.fromFen("7k/8/8/8/8/8/7r/7K w - - 0 1")));
        assertEquals("kings", NnueCpCalibration.invalid(Board.fromFen("8/8/8/8/8/8/P7/7K w - - 0 1")));
        assertEquals("pawn_rank", NnueCpCalibration.invalid(Board.fromFen("7k/8/8/8/8/8/8/P6K w - - 0 1")));
        assertEquals("fifty_move", NnueCpCalibration.invalid(Board.fromFen("7k/8/8/8/8/8/P7/7K w - - 100 1")));
        assertEquals("dead_material", NnueCpCalibration.invalid(Board.fromFen("7k/8/8/8/8/8/8/7K w - - 0 1")));
    }

    @Test void quietClassifierDetectsImmediateQueenLossForEitherSide() {
        long[] board = Board.fromFen("7k/8/8/8/q7/8/R7/7K w - - 0 1");
        assertNull(NnueCpCalibration.invalid(board));
        assertTrue(NnueCpCalibration.tactical(board) >= 900);
        assertTrue(NnueCpCalibration.tactical(NnueCpCalibration.flip(board)) >= 900);
        assertEquals(0, NnueCpCalibration.tactical(Board.startingPosition()));
        assertFalse(NnueCpCalibration.mateInOne(Board.startingPosition()));
        long[] mating = Board.fromFen("7k/5Q2/5K2/8/8/8/8/8 w - - 0 1");
        assertNull(NnueCpCalibration.invalid(mating));
        assertTrue(NnueCpCalibration.mateInOne(mating));
        assertTrue(NnueCpCalibration.mateInOne(NnueCpCalibration.flip(mating)));
    }
}
