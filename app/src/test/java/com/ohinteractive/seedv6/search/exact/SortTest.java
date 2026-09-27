package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SortTest {
    @Test void everyCapacityAndCrossoverPreservesKeyMovePairsAndSubrangeBoundaries() {
        Random random = new Random(17017);
        for(int n = 0; n <= 512; n++) {
            int[] keys = new int[n + 4]; long[] moves = new long[n + 4];
            for(int i = 0; i < keys.length; i++) { keys[i] = random.nextInt(65) * 1024 + i; moves[i] = i; }
            int[] expected = keys.clone(); Arrays.sort(expected, 2, n + 2);
            for(int threshold : new int[] {8, 16, 24, 32, 512}) {
                int[] actual = keys.clone(); long[] ordered = moves.clone();
                Sort.full(ordered, actual, 2, n + 2, threshold);
                for(int i = 2; i < n + 2; i++) {
                    assertEquals(expected[n + 3 - i], actual[i]);
                    assertEquals(keys[(int) ordered[i]], actual[i]);
                }
                for(int i : new int[] {0, 1, n + 2, n + 3}) {
                    assertEquals(keys[i], actual[i]); assertEquals(moves[i], ordered[i]);
                }
            }
            // Offset simulates a non-root lazy ply. The prefix must survive each next().
            int[] lazy = new int[n + 15]; System.arraycopy(keys, 2, lazy, 11, n);
            long[] selected = Arrays.copyOfRange(moves, 2, n + 2);
            for(int i = 0; i < n; i++) {
                Sort.next(selected, lazy, 11, i, n);
                for(int j = 0; j <= i; j++) assertEquals(expected[n + 1 - j], keys[(int) selected[j]]);
            }
        }
    }

    @Test void sortedReverseAndEqualEvidenceKeysWorkAtMaximumCapacity() {
        for(int shape = 0; shape < 3; shape++) for(int threshold : new int[] {8, 16, 24, 32, 512}) {
            int[] keys = new int[512]; long[] moves = new long[512];
            for(int i = 0; i < 512; i++) { keys[i] = shape == 0 ? i : shape == 1 ? 511 - i : (i % 2) * 512 + i; moves[i] = keys[i]; }
            Sort.full(moves, keys, 0, 512, threshold);
            for(int i = 0; i < 512; i++) { assertEquals(keys[i], moves[i]); if(i > 0) assertTrue(keys[i - 1] > keys[i]); }
        }
    }
}
