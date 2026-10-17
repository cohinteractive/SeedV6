package com.ohinteractive.seedv6.search.exact;

/** Explicit SR-033 research. Outcome labels remain conditional selective evidence,
 * never scores, calibrated confidence, ordinary TT proof or ordering updates. */
public enum DepthFeedback {
    OFF,
    /** One-ply reduction relief when this worker's last nominal outcome for the
     * moving piece/from/to exceeded its captured alpha. Request-private memory. */
    MOVE,
    /** One-ply reduction relief for subsequent eligible siblings after an
     * optimistic reduced scout failed its mandatory nominal confirmation. */
    CAUTION;

    public static class Diagnostics {
        public long eligible, adjusted, bypassed, verified, confirmed, refuted;
        /** Synchronous completed nominal observation; original parent-relative threshold. */
        public void outcome(long move,int ply,int depth,int alpha,int score,boolean reduced) {}
    }
}
