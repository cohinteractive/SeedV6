package com.ohinteractive.seedv6.search.exact;

/** Optional SR-032 observations. Each search worker owns its sink; callbacks must
 * not mutate borrowed boards or influence Search. Disabled in production. */
public class TacticalExtensionDiagnostics {
    public long extensions, scouts, fullResearches;
    public void extended(long[] parent,long[] child,long move,int ply,int depth,int used) {}
    public void edge(long[] parent, long[] child, long move, long previousMove,
            int ply, int depth, boolean checked, boolean completeMoves, int legalCount) {}
    public void quietBoundary(long[] board, int ply, int qply) {}
}
