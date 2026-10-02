package com.ohinteractive.seedv6.training.service;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CorpusPermutationTest {
    @Test void versionedGoldenVectorsAreIndependentOfJvmRngImplementation() {
        assertEquals(List.of(15L,16L,3L,12L,8L,0L,7L,1L,2L,9L,13L,10L,4L,6L,11L,5L,14L), range(17, 71, 0, 17));
        assertEquals(List.of(3L,13L,12L,6L,15L,10L,14L,8L,11L,5L,4L,2L,0L,9L,16L,1L,7L), range(17, 71, 17, 17));
    }
    static List<Long> range(long size, long seed, long start, int count) {
        var result = new ArrayList<Long>();
        for (int i = 0; i < count; i++) result.add(CorpusPermutation.at(size, seed, start + i));
        return result;
    }
    @Test void deterministicSeedsAndGenerationRanges() {
        assertEquals(range(101, 71, 0, 101), range(101, 71, 0, 101));
        assertNotEquals(range(101, 71, 0, 101), range(101, 72, 0, 101));
        assertTrue(Collections.disjoint(range(101, 71, 0, 20), range(101, 71, 20, 20)));
    }
    @Test void bijectionForArbitrarySmallSizesAndEpochs() {
        for (int size = 1; size <= 257; size++) for (long seed : new long[] {0, 71, Long.MIN_VALUE}) for (int epoch = 0; epoch < 2; epoch++) {
            var selection = range(size, seed, (long) epoch * size, size);
            assertEquals(size, new HashSet<>(selection).size(), "size=" + size);
            for (long index : selection) assertTrue(index >= 0 && index < size);
        }
    }
    @Test void smallCorpusCrossesEpochsInsteadOfRandomReplacementOrFailure() {
        var batch = range(3, 71, 0, 11);
        assertEquals(3, new HashSet<>(batch.subList(0, 3)).size());
        assertEquals(3, new HashSet<>(batch.subList(3, 6)).size());
        assertEquals(batch, range(3, 71, 0, 11));
        assertNotEquals(range(101, 71, 0, 101), range(101, 71, 101, 101));
        assertEquals(List.of(0L, 0L, 0L), range(1, 9, 0, 3));
    }
    @Test void longAddressSpaceNeedsNoCorpusSizedArray() {
        for (long size : new long[] {(1L << 32) + 17, (1L << 62) + 1, Long.MAX_VALUE})
            for (long offset : new long[] {0, 1, size / 2, size - 1}) {
                long index = CorpusPermutation.index(size, 71, 9, offset);
                assertTrue(index >= 0 && index < size);
                assertEquals(index, CorpusPermutation.index(size, 71, 9, offset));
            }
        assertThrows(IllegalArgumentException.class, () -> CorpusPermutation.index(0, 0, 0, 0));
    }
}
