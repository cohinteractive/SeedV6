package com.ohinteractive.seedv6.training.checkpoint;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenerationRetentionTest {
    @Test void tierBoundariesUseGenerationModuloIncludingVeryOldHistory() {
        long[][] cases = {{0, 1}, {99, 1}, {100, 10}, {999, 10}, {1000, 100}, {9999, 100},
                {10000, 1000}, {99999, 1000}, {100000, 10000}, {999999, 10000},
                {1000000, 100000}, {1000000000000000000L, 100000000000000000L}};
        for (long[] pair : cases) {
            long age = pair[0], interval = pair[1], generation = 2 * interval;
            assertTrue(GenerationRetention.retain(generation, generation + age), "sample age " + age);
            assertEquals(interval == 1, GenerationRetention.retain(generation + 1, generation + 1 + age), "adjacent age " + age);
            assertEquals(interval == 1, GenerationRetention.retain(generation - 1, generation - 1 + age), "prior age " + age);
        }
    }

    @Test void newestHundredAlwaysSurviveAtAnyOffsetAndLongLimitsDoNotOverflow() {
        for (long latest : new long[] {99, 100, 101, 999, 10001, Long.MAX_VALUE}) {
            for (int age = 0; age < 100; age++) assertTrue(GenerationRetention.retain(latest - age, latest));
        }
        assertTrue(GenerationRetention.retain(0, Long.MAX_VALUE));
        assertTrue(GenerationRetention.retain(100000000000000000L, Long.MAX_VALUE));
        assertFalse(GenerationRetention.retain(100000000000000001L, Long.MAX_VALUE));
        assertTrue(GenerationRetention.retain(10, 9), "Future/active generations are conservative.");
        assertThrows(IllegalArgumentException.class, () -> GenerationRetention.retain(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> GenerationRetention.retain(0, -1));
    }
}
