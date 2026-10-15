package com.ohinteractive.seedv6.search.exact;

/** SR-021 research only; OFF preserves accepted cumulative QXRAY production.
 * Margins are exploratory Pair-2 headroom, never score bounds or calibrated cp. */
public enum FutilityPolicy {
    OFF, LEAF, SHALLOW, PREFIX, DEEP;

    boolean considers(int depth,int searchedQuiets) {
        return switch(this) {
            case LEAF -> depth == 1;
            case SHALLOW -> depth >= 1 && depth <= 2;
            case PREFIX -> depth >= 1 && depth <= 2 && searchedQuiets >= 3;
            case DEEP -> depth == 3 && searchedQuiets >= 3;
            default -> false;
        };
    }
    int margin(int depth) {return depth == 1 ? 50 : depth == 2 ? 100 : 200;}

    /** Worker-private optional counters/callbacks; no production observer allocation. */
    public static class Diagnostics {
        public long candidates,evaluations,omissions;
        public final long[] byDepth=new long[ExactSearch.MAX_DEPTH+1];
        public void omitted(long[] parent,long[] child,long move,int ply,int depth,
                int searched,int quiets,int alpha,int beta,int staticEval) {}
    }
}
