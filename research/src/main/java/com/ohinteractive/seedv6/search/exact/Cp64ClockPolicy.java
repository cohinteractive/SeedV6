package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** SR-034 research-only clock consumers; no extra nodes, diagnostics or score calibration.
 * Saved time stays on the game clock. Unstable decisions retain the original hard
 * allocation. This first experiment never expands that independently enforced cap. */
public final class Cp64ClockPolicy implements SingleDepthSearch {
    public enum Policy { HALF, STABLE3_HALF }
    private final SingleDepthSearch owner;
    private final Policy policy;
    private long previousMove;
    private int stable, previousDepth;

    public Cp64ClockPolicy(SingleDepthSearch owner, Policy policy) {
        this.owner=Objects.requireNonNull(owner);this.policy=Objects.requireNonNull(policy);
    }
    @Override public SearchResult search(SearchRequest request) { return owner.search(request); }
    @Override public int maxSupportedDepth() { return owner.maxSupportedDepth(); }
    @Override public int activeSearchThreads() { return owner.activeSearchThreads(); }
    @Override public void beginRequest() { reset();owner.beginRequest(); }
    @Override public void endRequest() { try {owner.endRequest();} finally {reset();} }
    @Override public void newGame() { reset();owner.newGame(); }
    @Override public void close() { reset();owner.close(); }
    private void reset() {previousMove=0;stable=previousDepth=0;}

    @Override public boolean clockAllocationComplete(SearchResult completed, long elapsedNanos, long hardBudgetNanos) {
        if(!completed.completed()) throw new IllegalArgumentException("Completed evidence required");
        if(hardBudgetNanos<0 || elapsedNanos<0) throw new IllegalArgumentException("Finite clock required");
        if(!completed.hasMove() || TranspositionScores.isMateScore(completed.score())) {reset();return false;}
        stable=previousDepth+1==completed.depth() && previousMove==completed.bestMove() ? stable+1 : 1;
        previousDepth=completed.depth();previousMove=completed.bestMove();
        return completed.depth()>=6 && (policy==Policy.HALF || stable>=3)
                && elapsedNanos>=hardBudgetNanos/2+hardBudgetNanos%2;
    }
    public String identity() { return policy==Policy.HALF ? "half-d6-v1" : "stable3-half-d6-v1"; }
}
