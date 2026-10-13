package com.ohinteractive.seedv6.search.exact;

/** SR-018 Pair-2 research only. Constants are hypotheses, not HCE/WDL calibration.
 * Every policy retains cumulative EFFORT, qsearch, quiet LMR and WINDOW outside probes. */
public enum NullMovePolicy {
    OFF, OBSERVE, FIXED, SHALLOW, ADAPTIVE, VERIFIED, GUARDED;

    int minimumDepth() { return this == SHALLOW ? 3 : 4; }
    int reduction(int depth) { return this == ADAPTIVE ? Math.min(depth - 2, 2 + depth / 4) : 2; }
    int surplus() { return this == GUARDED ? 100 : 0; }

    /** Optional diagnostics; never allocated in ordinary or clean cost searches. */
    public static class Diagnostics {
        public long eligible, evaluations, attempts, probeNodes, probeQnodes, failHighs, cutoffs;
        public long verifications, verificationNodes, rejected, sparseExcluded;
        public final long[] attemptsByDepth = new long[257], cutoffsByDepth = new long[257];
        public void probe(long[] board, int ply, int depth, int beta, int evaluation, int reduction,
                int prediction, boolean accepted) {}
    }
}
