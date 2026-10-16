package com.ohinteractive.seedv6.tools.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.SearchResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NnuePerformanceBenchmarkTest {
    @Test
    void rejectsInvalidAndAmbiguousBenchmarkConfiguration() {
        for (String option : new String[] {"--depth=0", "--threads=0", "--warmups=-1", "--repetitions=0",
                "--iterations=0", "--scale=NaN", "--diagnostics=maybe", "--mode=unknown", "--unknown=1"})
            assertThrows(IllegalArgumentException.class, () -> NnuePerformanceBenchmark.Options.parse(new String[] {option}));
        assertThrows(IllegalArgumentException.class, () -> NnuePerformanceBenchmark.Options.parse(
                new String[] {"--network=a", "--create-network=b"}));
        var options = NnuePerformanceBenchmark.Options.parse(new String[] {"--threads=1,2,4", "--mode=micro"});
        assertArrayEquals(new int[] {1, 2, 4}, options.threads());
    }

    @Test
    void benchmarkValidationRejectsAnIllegalPv() {
        assertThrows(AssertionError.class, () -> NnuePerformanceBenchmark.requireLegalPv(Board.startingPosition(),
                new SearchResult(0, true, 0, 1, 1, 20, true, new long[] {0})));
    }
}
