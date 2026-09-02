package com.ohinteractive.seedv6.uci;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UciMoveParserTest {

    @Test
    void createsOnlyCoordinateAndColourNeutralPromotionIntent() {
        assertEquals(new MoveIntent(12, 28), UciMoveParser.parse("e2e4"));
        assertEquals(new MoveIntent(52, 60, Promotion.QUEEN), UciMoveParser.parse("e7e8q"));
        assertEquals(new MoveIntent(8, 0, Promotion.KNIGHT), UciMoveParser.parse("a2a1N"));
    }

    @Test
    void rejectsMalformedCoordinatesAndPromotionSuffixes() {
        assertInvalid(null);
        assertInvalid("");
        assertInvalid("e2e");
        assertInvalid("e2e4qq");
        assertInvalid("i2e4");
        assertInvalid("e0e4");
        assertInvalid("e2i4");
        assertInvalid("e2e9");
        assertInvalid("E2e4");
        assertInvalid("e7e8k");
    }

    private static void assertInvalid(String coordinate) {
        assertThrows(IllegalArgumentException.class, () -> UciMoveParser.parse(coordinate));
    }
}
