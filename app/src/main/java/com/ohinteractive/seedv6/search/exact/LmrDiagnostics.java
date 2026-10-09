package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;

/** Optional SR-029 research instrumentation; absent from timed production runs.
 * Work in a probe includes its nested probes, so inclusive costs are not additive. */
public class LmrDiagnostics {
    public long attempts, omissions, verifications, fullResearches;
    public long reducedNodes, verificationNodes, fullResearchNodes;
    public long reducedQnodes, verificationQnodes, fullResearchQnodes;
    public final long[] byReduction = new long[4];
    public final long[] byParentDepth = new long[ExactSearch.MAX_DEPTH + 1];

    void reset() {
        attempts = omissions = verifications = fullResearches = 0;
        reducedNodes = verificationNodes = fullResearchNodes = 0;
        reducedQnodes = verificationQnodes = fullResearchQnodes = 0;
        Arrays.fill(byReduction, 0);
        Arrays.fill(byParentDepth, 0);
    }

    /** Synchronous read-only observation. The reused child must not be retained or mutated.
     * Scores/thresholds are parent-relative; depth is nominal child depth. */
    public void probe(long[] child, int ply, int depth, int reduction, int ordinal,
            int alpha, int beta, int score, boolean verifiedNext) {}

    /** Nominal scout observation, before the ordinary PVS decision. */
    public void verified(int ply, int depth, int alpha, int beta, int score, boolean fullNext) {}
}
