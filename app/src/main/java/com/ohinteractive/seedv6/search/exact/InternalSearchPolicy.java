package com.ohinteractive.seedv6.search.exact;

/** SR-007 explicit research controls. OFF preserves accepted EFFORT production. */
public enum InternalSearchPolicy {
    OFF, OBSERVE, IID, IID_WIDE, IIR_SCOUT, IIR_ALL, HYBRID;

    boolean iid() { return this == IID || this == IID_WIDE || this == HYBRID; }
    boolean iir() { return this == IIR_SCOUT || this == IIR_ALL || this == HYBRID; }

    /** Optional owner-private counters; reset at each root invocation. */
    public static class Diagnostics {
        public long entries, ttEntries, ttApplicable, ttCutoffs, legalHash, legalEffort;
        public long eligible, attempts, probes, probeNodes, probeQnodes, hints, changedFirst, reductions;
        public final long[] missingScout = new long[ExactSearch.MAX_DEPTH + 1];
        public final long[] missingWide = new long[ExactSearch.MAX_DEPTH + 1];
        public final long[] reducedDepth = new long[ExactSearch.MAX_DEPTH + 1];
        public final long[] probeDepth = new long[ExactSearch.MAX_DEPTH + 1];
        void reset() {
            entries=ttEntries=ttApplicable=ttCutoffs=legalHash=legalEffort=0;
            eligible=attempts=probes=probeNodes=probeQnodes=hints=changedFirst=reductions=0;
            java.util.Arrays.fill(missingScout,0); java.util.Arrays.fill(missingWide,0);
            java.util.Arrays.fill(reducedDepth,0); java.util.Arrays.fill(probeDepth,0);
        }
        public void probe(long[] board, int ply, int depth, int alpha, int beta,
                int probeScore, long hint, long nodes) {}
        public void reduced(long[] board, int ply, int nominalDepth, int actualDepth,
                int alpha, int beta) {}
    }
}
