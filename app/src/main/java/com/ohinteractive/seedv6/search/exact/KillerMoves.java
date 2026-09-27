package com.ohinteractive.seedv6.search.exact;

/** Two recent, distinct quiet cutoff moves per ply for the bounded SR-016 experiment. */
final class KillerMoves {
    static final int SLOTS = 2;

    private KillerMoves() {}

    /** Called after completion checking, with the actual searched cutoff move.
     * Zero is empty; every generated legal move encodes a nonzero moving piece. */
    static void recordCutoff(long[] killers, int ply, long move, int ep) {
        if(move == 0 || CaptureHistory.isTactical(move, ep)) return;
        int base = ply * SLOTS;
        long primary = killers[base];
        if(move == primary) return;
        // This also swaps an existing secondary into primary without duplicating it.
        killers[base + 1] = primary;
        killers[base] = move;
    }
}
