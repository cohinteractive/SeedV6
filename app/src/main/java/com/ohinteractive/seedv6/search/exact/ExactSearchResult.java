package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.util.Value;

/**
 * One fixed-depth attempt. An aborted attempt has no score, best move or PV:
 * score is Value.INVALID and completedDepth is -1 (even for requested depth 0).
 * A completed narrow-window attempt can be a fail-soft bound, not an exact
 * minimax value. Interpret it against the supplied window; its PV is a legal
 * discovered line/prefix, not a claim of an exact continuation. A fail-low PVS
 * scout does not replace that line; a scout cutoff supplies its move prefix.
 * An SR-011 domain-only narrow-window bound has no searched move or child PV.
 * Ordinary static-leaf full-window results without selective predictions have
 * exact scores and principal lines (TT may truncate). selective marks a completed invocation
 * affected by SR-018/SR-019 predictions; its score/PV is heuristic, not fixed-depth
 * mathematical proof, even when its score lies inside the caller's window.
 * A false flag is not proof for separate-domain/selective qsearch experiments.
 * SR-001A research PVs may extend beyond nominal depth. Boundary leaves count
 * only as qnodes; nodes is the total, normalNodes() excludes those leaves.
 * maximumQply is the maximum number of qsearch edges below a boundary (zero at entry).
 */
public record ExactSearchResult(
        int requestedDepth, boolean completed, long bestMove, int score,
        long[] principalVariation, long nodes, long elapsedNanos, long qnodes, int maximumQply,
        boolean selective) {
    /** Existing exact and separate qsearch-domain research producers. */
    public ExactSearchResult(int requestedDepth, boolean completed, long bestMove, int score,
                             long[] principalVariation, long nodes, long elapsedNanos, long qnodes, int maximumQply) {
        this(requestedDepth, completed, bestMove, score, principalVariation, nodes, elapsedNanos, qnodes, maximumQply, false);
    }
    /** Existing static-leaf producers retain their result contract. */
    public ExactSearchResult(int requestedDepth, boolean completed, long bestMove, int score,
                             long[] principalVariation, long nodes, long elapsedNanos) {
        this(requestedDepth, completed, bestMove, score, principalVariation, nodes, elapsedNanos, 0, 0);
    }

    public ExactSearchResult {
        if(requestedDepth < 0 || requestedDepth > ExactSearch.MAX_DEPTH
                || nodes < 0 || elapsedNanos < 0 || qnodes < 0 || qnodes > nodes
                || maximumQply < 0 || maximumQply > ExactSearch.MAX_DEPTH
                || (qnodes == 0 && maximumQply != 0)) throw new IllegalArgumentException("Invalid statistics.");
        principalVariation = principalVariation.clone();
        if(!completed && (bestMove != 0 || score != Value.INVALID || principalVariation.length != 0)) {
            throw new IllegalArgumentException("Aborted work cannot publish a search value or line.");
        }
        if(completed && (score < -ExactSearch.MATE_SCORE || score > ExactSearch.MATE_SCORE)) {
            throw new IllegalArgumentException("Invalid completed score.");
        }
        if(principalVariation.length > requestedDepth + maximumQply
                || principalVariation.length > ExactSearch.MAX_DEPTH
                || (principalVariation.length == 0 ? bestMove != 0 : principalVariation[0] != bestMove)) {
            throw new IllegalArgumentException("Best move and PV must agree.");
        }
    }

    public int completedDepth() { return completed ? requestedDepth : -1; }

    public long normalNodes() { return nodes - qnodes; }

    public boolean hasMove() { return bestMove != 0; }

    @Override public long[] principalVariation() { return principalVariation.clone(); }

    /** Includes fractional milliseconds; zero duration has no measurable throughput. */
    public long nps() { return elapsedNanos == 0 ? 0 : (long) (nodes * 1_000_000_000.0 / elapsedNanos); }
}
