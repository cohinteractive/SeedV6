package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.util.Value;

/**
 * One fixed-depth attempt. An aborted attempt has no score, best move or PV:
 * score is Value.INVALID and completedDepth is -1 (even for requested depth 0).
 * A completed narrow-window attempt can be a fail-soft bound, not an exact
 * minimax value. Interpret it against the supplied window; its PV is the
 * discovered line. Full-window results have exact scores and principal lines.
 */
public record ExactSearchResult(
        int requestedDepth, boolean completed, long bestMove, int score,
        long[] principalVariation, long nodes, long elapsedNanos) {
    public ExactSearchResult {
        if(requestedDepth < 0 || requestedDepth > ExactSearch.MAX_DEPTH
                || nodes < 0 || elapsedNanos < 0) throw new IllegalArgumentException("Invalid statistics.");
        principalVariation = principalVariation.clone();
        if(!completed && (bestMove != 0 || score != Value.INVALID || principalVariation.length != 0)) {
            throw new IllegalArgumentException("Aborted work cannot publish a search value or line.");
        }
        if(completed && (score < -ExactSearch.MATE_SCORE || score > ExactSearch.MATE_SCORE)) {
            throw new IllegalArgumentException("Invalid completed score.");
        }
        if(principalVariation.length > requestedDepth
                || (principalVariation.length == 0 ? bestMove != 0 : principalVariation[0] != bestMove)) {
            throw new IllegalArgumentException("Best move and PV must agree.");
        }
    }

    public int completedDepth() { return completed ? requestedDepth : -1; }

    public boolean hasMove() { return bestMove != 0; }

    @Override public long[] principalVariation() { return principalVariation.clone(); }

    /** Includes fractional milliseconds; zero duration has no measurable throughput. */
    public long nps() { return elapsedNanos == 0 ? 0 : (long) (nodes * 1_000_000_000.0 / elapsedNanos); }
}
