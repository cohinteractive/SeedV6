package com.ohinteractive.seedv6.search.driver;

import java.util.Random;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.exact.Cp64SpendingPolicy;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import static org.junit.jupiter.api.Assertions.*;

class Boundary2EquivalenceTest {
    private static SingleDepthSearch unused() { return new SingleDepthSearch() {
        public SearchResult search(SearchRequest request) { throw new AssertionError(); }
        public int maxSupportedDepth() { return 100; }
    }; }
    @Test void exactSelectedResearchDecisionsAcrossDiscontinuitiesResetsAndThresholds() {
        var reference = new Cp64SpendingPolicy(unused(), false);
        var production = new Boundary2ClockPolicy(unused());
        var random = new Random(20261024);
        for (long hard : new long[]{0, 1, 2, 3, 5, 100, 201, Long.MAX_VALUE}) {
            for (int cycle = 0; cycle < 50; cycle++) {
                reference.beginRequest(); production.beginRequest();
                for (int depth = 1; depth <= 25; depth++) {
                    int d = random.nextInt(8) == 0 ? depth + 2 : depth;
                    long move = random.nextInt(4) == 0 ? 43 : 42;
                    int score = random.nextInt(9) == 0 ? TranspositionScores.MATE_SCORE - 5 : 12;
                    boolean hasMove = random.nextInt(11) != 0;
                    var result = new SearchResult(hasMove ? move : 0, hasMove, score, d, 1, 20, true);
                    long half = hard / 2 + hard % 2;
                    long[] times = {0, Math.max(0, half - 1), half, half / 2 + half % 2, hard};
                    long elapsed = times[random.nextInt(times.length)];
                    assertEquals(reference.clockAllocationComplete(result, elapsed, hard),
                            production.clockAllocationComplete(result, elapsed, hard), "hard=" + hard + " depth=" + d);
                    if (depth == 10) { reference.newGame(); production.newGame(); }
                }
                reference.endRequest(); production.endRequest();
            }
        }
        reference.close(); production.close();
    }
    @Test void allocationMatchesFrozenResearchForAllBoundaryValues() {
        for (long remaining : new long[]{0, 1, 49, 50, 51, 52, 150, 500, 2933, 7959, 9799, Long.MAX_VALUE})
            for (long increment : new long[]{0, 1, 3, 4, 100, Long.MAX_VALUE})
                for (int moves : new int[]{1, 2, 30, Integer.MAX_VALUE}) {
                    long actual = ClockPolicy.BOUNDARY2.allocateClockMillis(remaining, increment, moves);
                    assertEquals(Cp64SpendingPolicy.hardBudgetMillis(remaining, increment, moves), actual);
                    assertTrue(actual >= 0 && actual <= Math.max(0, remaining - 50));
                    assertEquals(TimeManager.allocateClockMillis(remaining, increment, moves),
                            ClockPolicy.FACTUAL.allocateClockMillis(remaining, increment, moves));
                }
        assertEquals(660, ClockPolicy.BOUNDARY2.allocateClockMillis(7959, 100, 30));
        assertEquals(330, ClockPolicy.FACTUAL.allocateClockMillis(7959, 100, 30));
        assertThrows(IllegalArgumentException.class, () -> ClockPolicy.BOUNDARY2.allocateClockMillis(-1, 0, 30));
        assertThrows(IllegalArgumentException.class, () -> ClockPolicy.BOUNDARY2.allocateClockMillis(100, -1, 30));
        assertThrows(IllegalArgumentException.class, () -> ClockPolicy.BOUNDARY2.allocateClockMillis(100, 0, 0));
    }
}
