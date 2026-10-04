package com.ohinteractive.seedv6.training.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Bt4TargetsTest {
    @Test void inverseIsDeterministicMonotonicOddBoundedAndQuantizationConsistent() {
        assertEquals(0, Bt4Targets.q(0)); assertEquals(100 / 660.6, Bt4Targets.q(100), 2e-9);
        double previous = 0;
        for (int score = 0; score <= 32000; score++) {
            double q = Bt4Targets.q(score);
            assertTrue(q >= previous && q <= 1); assertEquals(-q, Bt4Targets.q(-score), 0);
            assertEquals(q, Bt4Targets.q(score), 0); previous = q;
        }
        for (int i = -10000; i <= 10000; i++) {
            double q = i / 10000.0; int score = (int) Math.round(660.6 * q / (1 - .9751875 * StrictMath.pow(q, 10)));
            assertEquals(q, Bt4Targets.q(score), .500001 / 660.6);
        }
        assertEquals(1, Bt4Targets.q(32000), 1e-15);
        for (int invalid : new int[]{32002, 32001, -32001, Integer.MIN_VALUE, Integer.MAX_VALUE}) assertThrows(IllegalArgumentException.class, () -> Bt4Targets.q(invalid));
    }
}
