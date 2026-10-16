package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap;

/** Orthogonal fixed-material ablation: no changed learned parameters or score mapping. */
public record MaterialBootstrapModel(IncrementalModel learned) implements IncrementalModel {
    @Override public int parameters() { return learned.parameters(); }
    @Override public Worker worker(int capacity) {
        var neural = learned.worker(capacity);
        return new Worker(1) {
            private final int[] whiteMaterial = new int[capacity];
            @Override public void refresh(long[] board, int slot) {
                neural.refresh(board, slot);
                whiteMaterial[slot] = NnueMaterialBootstrap.whiteScore(board);
            }
            @Override public void transition(long[] parent, long[] child, int source, int destination) {
                neural.transition(parent, child, source, destination);
                whiteMaterial[destination] = NnueMaterialBootstrap.update(parent, child, whiteMaterial[source]);
            }
            /** Deliberately reports the learned raw output only, in its original units. */
            @Override public double raw(long[] board, int slot) { return neural.raw(board, slot); }
            @Override public int evaluate(long[] board, int slot) {
                return NnueMaterialBootstrap.combine(NnueMaterialBootstrap.forSideToMove(board, whiteMaterial[slot]),
                        neural.evaluate(board, slot));
            }
        };
    }
}
