package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.manage.TimeManager;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** SR-034 bounded additional-spending experiment, with a matched extension-off control.
 * The hard cap is fixed before the request. No per-node consumer or deadline relaxation.
 * Nominal = ceil(hard/2), including where the remaining-time reserve clips the cap.
 * Stable ordinary moves may stop at ceil(nominal/2); other moves at nominal, always
 * at a completed boundary. A just-changed move may buy one more completed iteration.
 * This coarse contrast is a research hypothesis, not a calibrated production policy. */
public final class Cp64SpendingPolicy implements SingleDepthSearch {
    private final SingleDepthSearch owner;
    private final boolean extendChangedMove;
    private long previousMove;
    private int previousDepth, stable, extensionDepth;

    public Cp64SpendingPolicy(SingleDepthSearch owner, boolean extendChangedMove) {
        this.owner = Objects.requireNonNull(owner);
        this.extendChangedMove = extendChangedMove;
    }

    public static long hardBudgetMillis(long remaining, long increment, int movesToGo) {
        long base = TimeManager.allocateClockMillis(remaining, increment, movesToGo);
        return Math.min(Math.max(0L, remaining - TimeManager.CLOCK_RESERVE_MILLIS),
                TimeManager.saturatedAdd(base, base));
    }

    @Override public SearchResult search(SearchRequest request) { return owner.search(request); }
    @Override public int maxSupportedDepth() { return owner.maxSupportedDepth(); }
    @Override public int activeSearchThreads() { return owner.activeSearchThreads(); }
    @Override public void beginRequest() { reset(); owner.beginRequest(); }
    @Override public void endRequest() { try { owner.endRequest(); } finally { reset(); } }
    @Override public void newGame() { reset(); owner.newGame(); }
    @Override public void close() { reset(); owner.close(); }
    private void reset() { previousMove = 0; previousDepth = stable = extensionDepth = 0; }
    private static long halfCeiling(long value) { return value / 2 + value % 2; }

    @Override public boolean clockAllocationComplete(SearchResult completed, long elapsed, long hard) {
        if(!completed.completed()) throw new IllegalArgumentException("Completed evidence required");
        if(hard < 0 || elapsed < 0) throw new IllegalArgumentException("Finite clock required");
        long nominal = halfCeiling(hard);
        // The purchased iteration is complete even if its score enters the mate domain.
        // This is completion accounting, not a numerical confidence test on mate scores.
        if(extensionDepth > 0 && completed.depth() > extensionDepth && elapsed >= nominal) return true;
        if(!completed.hasMove() || TranspositionScores.isMateScore(completed.score())) {
            reset(); return false;
        }
        boolean adjacent = previousDepth + 1 == completed.depth();
        boolean changed = adjacent && previousDepth > 0 && previousMove != completed.bestMove();
        stable = adjacent && previousMove == completed.bestMove() ? stable + 1 : 1;
        previousMove = completed.bestMove(); previousDepth = completed.depth();
        if(completed.depth() < 6) return false;
        if(stable >= 3 && elapsed >= halfCeiling(nominal)) return true;
        if(elapsed < nominal) return false;
        if(extendChangedMove && changed) {
            extensionDepth = completed.depth(); return false;
        }
        return true;
    }

    public String identity() { return extendChangedMove ? "unstable2-d6-v1" : "boundary2-d6-v1"; }
}
