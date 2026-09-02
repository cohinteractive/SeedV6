package com.ohinteractive.seedv6.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Point;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class SquareMapperTest {

    private final SquareMapper mapper = new SquareMapper();

    @Test
    void everySquareCenterRoundTripsOnAResizedNonSquareComponent() {
        for(int square = 0; square < 64; square ++) {
            final Point center = mapper.squareCenter(square, 913, 641);
            assertEquals(square, mapper.squareAt(center.x, center.y, 913, 641).orElseThrow());
        }
    }

    @Test
    void cornersUseExplicitWhiteAtBottomMapping() {
        assertEquals(0, mapper.squareAt(50, 750, 800, 800).orElseThrow());
        assertEquals(7, mapper.squareAt(750, 750, 800, 800).orElseThrow());
        assertEquals(56, mapper.squareAt(50, 50, 800, 800).orElseThrow());
        assertEquals(63, mapper.squareAt(750, 50, 800, 800).orElseThrow());
        assertEquals(27, mapper.squareAt(350, 450, 800, 800).orElseThrow());
    }

    @Test
    void exactEdgesAreIncludedAndJustOutsidePointsAreRejected() {
        final Rectangle board = mapper.boardBounds(1_000, 800);
        assertEquals(new Rectangle(100, 0, 800, 800), board);
        assertEquals(56, mapper.squareAt(100, 0, 1_000, 800).orElseThrow());
        assertEquals(7, mapper.squareAt(899, 799, 1_000, 800).orElseThrow());
        assertFalse(mapper.squareAt(99, 400, 1_000, 800).isPresent());
        assertFalse(mapper.squareAt(900, 400, 1_000, 800).isPresent());
        assertFalse(mapper.squareAt(500, -1, 1_000, 800).isPresent());
        assertFalse(mapper.squareAt(500, 800, 1_000, 800).isPresent());
    }

    @Test
    void zeroSizedComponentsAndInvalidSquaresAreControlled() {
        assertFalse(mapper.squareAt(0, 0, 0, 100).isPresent());
        assertFalse(mapper.squareAt(0, 0, 100, 0).isPresent());
        assertThrows(IllegalArgumentException.class, () -> mapper.squareBounds(-1, 100, 100));
        assertThrows(IllegalArgumentException.class, () -> mapper.squareBounds(64, 100, 100));
    }
}
