package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;

/** Optional SR-030 observation; absent in clean timings and clock games. */
public class CheckExtensionDiagnostics {
    public long extensions, scouts, fullResearches;
    /** Maximum awarded plies, possibly before cancellation prevents child entry. */
    public int maximumUsed;
    public final long[] byDepth = new long[ExactSearch.MAX_DEPTH + 1];
    public void reset() {
        extensions = scouts = fullResearches = 0;
        maximumUsed = 0;
        Arrays.fill(byDepth, 0);
    }
    /** Board arrays are borrowed only for the duration of this call. */
    public void extended(long[] parent, long[] child, long move, int ply, int depth,
            int used, int ordinal, boolean checked, int legalCount) {}
}
