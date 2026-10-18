package com.ohinteractive.seedv6.training.selfplay;

import java.util.SplittableRandom;
import com.ohinteractive.seedv6.training.service.CorpusPermutation;

/** Constant-space block permutation. Each disk block is read sequentially, then mixed in memory. */
final class ReplayOrder {
    static final int BLOCK = 2048;
    private final int count;
    private final long seed;
    private final int epoch;
    private final boolean shuffle;
    ReplayOrder(int count, long seed, int epoch, boolean shuffle) {
        this.count = count; this.seed = seed; this.epoch = epoch; this.shuffle = shuffle;
    }
    int blocks() { return (int) ((count + (long) BLOCK - 1) / BLOCK); }
    int block(int ordinal) { return shuffle ? (int) CorpusPermutation.index(blocks(), seed, epoch, ordinal) : ordinal; }
    int length(int block) { return Math.min(BLOCK, count - block * BLOCK); }
    void mix(TrajectorySampler.Sample[] samples, int length, int block) {
        if (!shuffle) return;
        var random = new SplittableRandom(seed ^ ((long) epoch << 32) ^ block ^ 0x9e3779b97f4a7c15L);
        for (int i = length - 1; i > 0; i--) {
            int other = random.nextInt(i + 1); var swap = samples[i]; samples[i] = samples[other]; samples[other] = swap;
        }
    }
}
