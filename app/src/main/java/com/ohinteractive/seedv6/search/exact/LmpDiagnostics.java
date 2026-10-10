package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;

/** Optional SR-022 observations, disabled in clean timed runs. Arrays count
 * candidates/omissions, never pretend omitted children were visited/evaluated. */
public class LmpDiagnostics {
    public final long[] eligible = new long[5];
    public final long[] omitted = new long[5];
    void reset() { Arrays.fill(eligible, 0); Arrays.fill(omitted, 0); }
    public void node(long[] board, int ply, int depth) {}
    /** Exactly the compact searched prefix used by existing history updates. */
    public void cutoff(long[] board, int ply, int depth, long[] searchedMoves, int cutoff) {}
    /** Read-only reused boards, before evaluator/history preparation; counts are prior searched children. */
    public void omitted(long[] parent, long[] child, long move, int ply, int depth,
            int searched, int searchedQuiets, int alpha, int beta) {}
    /** Read-only reused boards after a completed child; ordinals include this child once. */
    public void searched(long[] parent, long[] child, long move, int ply, int depth,
            int ordinal, int quietOrdinal, int alpha, int beta, int score) {}
}
