package com.ohinteractive.seedv6.search.common;

import java.math.BigInteger;
import java.util.Objects;

import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;

/**
 * Immutable publication for one fully completed iterative-search depth.
 * Result nodes are cumulative from the managed top-level start and include
 * earlier depths plus aspiration retries. Elapsed time uses that same monotonic
 * start. NPS is unavailable until at least one whole millisecond and one node
 * have elapsed; it is then derived from the unfloored nanosecond duration.
 */
public record IterationSnapshot(
    SearchResult result,
    long elapsedMillis,
    long nps
) {

    public static final long NPS_UNAVAILABLE = -1L;

    public IterationSnapshot {
        Objects.requireNonNull(result, "result");
        if(!result.completed()) {
            throw new IllegalArgumentException("An iteration snapshot must contain a completed exact result.");
        }
        if(elapsedMillis < 0L) {
            throw new IllegalArgumentException("Elapsed time must not be negative.");
        }
        if(result.nodes() < 0L) {
            throw new IllegalArgumentException("Iteration nodes must not be negative.");
        }
        if(nps < NPS_UNAVAILABLE) {
            throw new IllegalArgumentException("NPS must be non-negative or unavailable.");
        }
    }

    public static IterationSnapshot from(SearchResult result, long elapsedNanos) {
        Objects.requireNonNull(result, "result");
        final long nonNegativeNanos = Math.max(0L, elapsedNanos);
        final long elapsedMillis = nonNegativeNanos / 1_000_000L;
        final long nps = elapsedMillis == 0L || result.nodes() == 0L
            ? NPS_UNAVAILABLE
            : multiplyDivideSaturated(result.nodes(), 1_000_000_000L, nonNegativeNanos);
        return new IterationSnapshot(result, elapsedMillis, nps);
    }

    public int depth() {
        return result.depth();
    }

    public int score() {
        return result.score();
    }

    public long nodes() {
        return result.nodes();
    }

    public boolean hasMove() {
        return result.hasMove();
    }

    public long bestMove() {
        return result.bestMove();
    }

    /** Cumulative diagnostics at this completed-depth publication point. */
    public SearchDiagnosticsSnapshot diagnostics() {
        return result.diagnostics();
    }

    /** Returns an independently owned PV copy. */
    public long[] principalVariation() {
        return result.principalVariation();
    }

    public boolean hasNps() {
        return nps != NPS_UNAVAILABLE;
    }

    private static long multiplyDivideSaturated(long value, long multiplier, long divisor) {
        final BigInteger quotient = BigInteger.valueOf(value)
            .multiply(BigInteger.valueOf(multiplier))
            .divide(BigInteger.valueOf(divisor));
        return quotient.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0
            ? Long.MAX_VALUE : quotient.longValue();
    }
}
