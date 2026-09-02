package com.ohinteractive.seedv6.search.manage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeManagerTest {

    @Test
    void movetimeReservesFixedEngineOverhead() {
        assertEquals(0L, TimeManager.movetimeBudgetMillis(0L));
        assertEquals(0L, TimeManager.movetimeBudgetMillis(10L));
        assertEquals(1L, TimeManager.movetimeBudgetMillis(11L));
        assertEquals(90L, TimeManager.movetimeBudgetMillis(100L));
        assertThrows(IllegalArgumentException.class, () -> TimeManager.movetimeBudgetMillis(-1L));
    }

    @Test
    void clockFormulaUsesMovesIncrementReserveAndOverheadExactly() {
        // remaining / mtg + 75% increment - overhead, capped below reserve
        assertEquals(165L, TimeManager.allocateClockMillis(3_000L, 100L, 30));
        assertEquals(365L, TimeManager.allocateClockMillis(3_000L, 100L, 10));
        assertEquals(950L, TimeManager.allocateClockMillis(1_000L, Long.MAX_VALUE, 30));
    }

    @Test
    void veryLowClockNeverConsumesTheReserve() {
        assertEquals(0L, TimeManager.allocateClockMillis(0L, 0L, 30));
        assertEquals(0L, TimeManager.allocateClockMillis(49L, 1_000L, 30));
        assertEquals(0L, TimeManager.allocateClockMillis(50L, 1_000L, 30));
        assertEquals(1L, TimeManager.allocateClockMillis(51L, 1_000L, 30));
    }

    @Test
    void hugeInputsAndConversionsSaturateWithoutOverflow() {
        assertEquals(Long.MAX_VALUE, TimeManager.saturatedAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, TimeManager.millisToNanos(Long.MAX_VALUE));
        assertEquals(1_000_000L, TimeManager.millisToNanos(1L));
        final long allocated = TimeManager.allocateClockMillis(
            Long.MAX_VALUE, Long.MAX_VALUE, 1
        );
        assertTrue(allocated >= 0L);
        assertTrue(allocated <= Long.MAX_VALUE - TimeManager.CLOCK_RESERVE_MILLIS);
    }

    @Test
    void invalidClockInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> TimeManager.allocateClockMillis(-1L, 0L, 30));
        assertThrows(IllegalArgumentException.class, () -> TimeManager.allocateClockMillis(1L, -1L, 30));
        assertThrows(IllegalArgumentException.class, () -> TimeManager.allocateClockMillis(1L, 0L, 0));
        assertThrows(IllegalArgumentException.class, () -> TimeManager.millisToNanos(-1L));
    }
}
