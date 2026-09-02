package com.ohinteractive.seedv6.search.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchControlTest {

    @Test
    void fakeClockHonorsNotReachedExactAndExceededDeadline() {
        final FakeClock clock = new FakeClock(1_000L);
        final SearchControl control = SearchControl.controlled(-1L, clock.nanoTime(), 500L, clock);

        clock.now = 1_499L;
        assertTrue(control.checkpoint());
        clock.now = 1_500L;
        assertFalse(control.checkpoint());
        assertEquals(SearchTermination.TIME_LIMIT, control.termination());

        final SearchControl exceeded = SearchControl.controlled(-1L, 1_000L, 500L, clock);
        clock.now = 1_501L;
        assertFalse(exceeded.checkpoint());
        assertEquals(SearchTermination.TIME_LIMIT, exceeded.termination());
    }

    @Test
    void zeroTimeBudgetExpiresAtItsFirstCheckpoint() {
        final FakeClock clock = new FakeClock(77L);
        final SearchControl control = SearchControl.controlled(-1L, 77L, 0L, clock);

        assertFalse(control.checkpoint());
        assertEquals(SearchTermination.TIME_LIMIT, control.termination());
    }

    @Test
    void elapsedCalculationHandlesBackwardClockAndNanoTimeWrap() {
        assertEquals(25L, SearchControl.elapsedNanos(125L, 100L));
        assertEquals(0L, SearchControl.elapsedNanos(99L, 100L));
        assertEquals(10L, SearchControl.elapsedNanos(
            Long.MIN_VALUE + 4L, Long.MAX_VALUE - 5L
        ));
    }

    @Test
    void nodeBudgetAllowsAtMostNEnteredNodes() {
        final SearchControl zero = SearchControl.controlled(0L, 0L, -1L, new FakeClock(0L));
        assertFalse(zero.tryEnterNode());
        assertEquals(0L, zero.nodes());
        assertEquals(SearchTermination.NODE_LIMIT, zero.termination());

        final SearchControl one = SearchControl.controlled(1L, 0L, -1L, new FakeClock(0L));
        assertTrue(one.tryEnterNode());
        assertEquals(1L, one.nodes());
        assertFalse(one.tryEnterNode());
        assertEquals(1L, one.nodes());
        assertEquals(SearchTermination.NODE_LIMIT, one.termination());
    }

    @Test
    void cancellationIsVisibleAndIdempotent() {
        final SearchControl control = SearchControl.controlled(-1L, 0L, -1L, new FakeClock(0L));

        assertTrue(control.request(SearchTermination.STOPPED));
        assertFalse(control.request(SearchTermination.REPLACED));
        assertFalse(control.checkpoint());
        assertEquals(SearchTermination.STOPPED, control.termination());
    }

    private static final class FakeClock implements TimeSource {
        private long now;

        FakeClock(long now) {
            this.now = now;
        }

        @Override
        public long nanoTime() {
            return now;
        }
    }
}
