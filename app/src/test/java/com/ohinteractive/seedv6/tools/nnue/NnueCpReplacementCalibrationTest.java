package com.ohinteractive.seedv6.tools.nnue;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NnueCpReplacementCalibrationTest {
    @Test void allAnchorsBothDirectionsColoursAndPerspectives() {
        String[] names = {"", "K", "Q", "R", "B", "N", "P"};
        int[] values = {0, 0, 900, 500, 330, 320, 100};
        for (int[] anchor : NnueCpReplacementCalibration.CLASSES)
            for (int direction = 0; direction < 2; direction++)
                for (int side = 0; side < 2; side++)
                    for (String stm : new String[]{"w", "b"}) {
                        int from = anchor[direction], to = anchor[1 - direction];
                        String piece = side == 0 ? names[from] : names[from].toLowerCase();
                        long[] b = Board.fromFen("7k/8/8/8/8/8/" + piece + "7/7K " + stm + " - - 37 29");
                        long[] original = b.clone();
                        long[] r = NnueCpReplacementCalibration.replace(b, 8, to);
                        int expected = (side == (stm.equals("w") ? 0 : 1) ? 1 : -1) * (values[to] - values[from]);
                        assertArrayEquals(original, b);
                        assertEquals(to | (side << 3), NnueCpReplacementCalibration.code(r, 8));
                        assertEquals(expected, NnueCpReplacementCalibration.delta(b, 8, to));
                        assertEquals(expected, NnueCpCalibration.material(r) - NnueCpCalibration.material(b));
                        assertEquals(b[4], r[4]);
                        assertEquals(b[0] | b[1] | b[2], r[0] | r[1] | r[2]);
                        assertArrayEquals(b, NnueCpReplacementCalibration.replace(r, 8, from));
                        assertEquals(-expected, NnueCpReplacementCalibration.delta(NnueCpCalibration.flip(b), 8, to));
                        assertEquals(expected, NnueCpReplacementCalibration.delta(NnueCpCalibration.reflect(b), 48, to));
                    }
    }

    @Test void rejectsKingsEmptySquareAndSameIdentity() {
        long[] b = Board.startingPosition();
        assertThrows(IllegalArgumentException.class, () -> NnueCpReplacementCalibration.replace(b, 4, 3));
        assertThrows(IllegalArgumentException.class, () -> NnueCpReplacementCalibration.replace(b, 16, 3));
        assertThrows(IllegalArgumentException.class, () -> NnueCpReplacementCalibration.replace(b, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> NnueCpReplacementCalibration.replace(b, 1, 5));
    }

    @Test void preservesRightsForScreeningAndRejectsDamagedState() {
        long[] b = Board.startingPosition();
        assertEquals("castling", NnueCpCalibration.invalid(NnueCpReplacementCalibration.replace(b, 0, 5)));
        assertEquals("pawn_rank", NnueCpCalibration.invalid(NnueCpReplacementCalibration.replace(b, 0, 6)));
        assertNull(NnueCpCalibration.invalid(NnueCpReplacementCalibration.replace(b, 1, 4)));
        long[] ep = Board.fromFen("7k/8/8/3pP3/8/8/N7/7K w - d6 0 2");
        assertEquals("en_passant", NnueCpCalibration.invalid(NnueCpReplacementCalibration.replace(ep, 8, 4)));
    }

    @Test void rejectsNewCheckAndStalemate() {
        long[] b = Board.fromFen("N6k/8/8/8/8/8/P7/7K w - - 0 1");
        assertNull(NnueCpCalibration.invalid(b));
        assertEquals("check", NnueCpCalibration.invalid(NnueCpReplacementCalibration.replace(b, 56, 3)));
        assertEquals("no_legal_move", NnueCpCalibration.invalid(Board.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")));
    }
}
