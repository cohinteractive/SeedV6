package com.ohinteractive.seedv6.search.manage;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** Selected SR-034 BOUNDARY2, equivalent to Cp64SpendingPolicy(owner, false).
 * One instance per search owner. SearchDriver invokes this only for a completed
 * iteration of a managed clock request and rechecks hard/node/external limits.
 * Nominal is ceil(clipped hard / 2), not necessarily the original base allocation. */
public final class Boundary2ClockPolicy implements SingleDepthSearch {
    private final SingleDepthSearch owner;
    private long previousMove;
    private int previousDepth, stable;

    public Boundary2ClockPolicy(SingleDepthSearch owner) { this.owner = Objects.requireNonNull(owner); }
    @Override public SearchResult search(SearchRequest request) { return owner.search(request); }
    @Override public int maxSupportedDepth() { return owner.maxSupportedDepth(); }
    @Override public int activeSearchThreads() { return owner.activeSearchThreads(); }
    @Override public void beginRequest() { reset(); owner.beginRequest(); }
    @Override public void endRequest() { try { owner.endRequest(); } finally { reset(); } }
    @Override public void newGame() { reset(); owner.newGame(); }
    @Override public void close() { reset(); owner.close(); }
    private void reset() { previousMove = 0; previousDepth = stable = 0; }
    private static long halfCeiling(long value) { return value / 2 + value % 2; }

    @Override public boolean clockAllocationComplete(SearchResult completed, long elapsed, long hard) {
        if (!completed.completed()) throw new IllegalArgumentException("Completed evidence required");
        if (hard < 0 || elapsed < 0) throw new IllegalArgumentException("Finite clock required");
        long nominal = halfCeiling(hard);
        if (!completed.hasMove() || TranspositionScores.isMateScore(completed.score())) {
            reset(); return false;
        }
        boolean adjacent = previousDepth + 1 == completed.depth();
        stable = adjacent && previousMove == completed.bestMove() ? stable + 1 : 1;
        previousMove = completed.bestMove(); previousDepth = completed.depth();
        if (completed.depth() < 6) return false;
        if (stable >= 3 && elapsed >= halfCeiling(nominal)) return true;
        return elapsed >= nominal;
    }
}
