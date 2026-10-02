package com.ohinteractive.seedv6.training.service;

/** Six-round balanced Feistel permutation, cycle-walked into [0,size).
 * Domain is the next power of four (at most four times size); unsigned 64-bit
 * arithmetic also supports sizes above 2^62. No RNG implementation/state or array
 * enters replay. Each epoch has a seed-derived key; memory is constant.
 */
public final class CorpusPermutation {
    public static long index(long size, long seed, long epoch, long offset) {
        if (size < 1 || epoch < 0 || offset < 0 || offset >= size) throw new IllegalArgumentException("Invalid corpus permutation address");
        if (size == 1) return 0;
        int bits = 64 - Long.numberOfLeadingZeros(size - 1), half = (bits + 1) / 2;
        long mask = (1L << half) - 1, key = mix(seed ^ mix(epoch + 0x9e3779b97f4a7c15L));
        long value = offset;
        do {
            long left = value >>> half & mask, right = value & mask;
            for (int round = 0; round < 6; round++) {
                long next = left ^ (mix(right ^ key ^ (0x9e3779b97f4a7c15L * (round + 1))) & mask);
                left = right; right = next;
            }
            value = left << half | right;
        } while (Long.compareUnsigned(value, size) >= 0);
        return value;
    }
    public static long at(long size, long seed, long absolute) {
        if (absolute < 0) throw new IllegalArgumentException("Negative corpus cursor");
        return index(size, seed, absolute / size, absolute % size);
    }
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
    private CorpusPermutation() {}
}
