package com.ohinteractive.seedv6.training.validation;

import java.util.Objects;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;

/** Depth or per-move time limits, identical for both colours. Ply cap follows the common opening. */
public record ValidationConfig(int openingPairs, long seed, int minimumOpeningPlies,
                               int maximumOpeningPlies, int depth, int threads,
                               NnueScoreMapping scoreMapping, int maximumPlies, long moveMillis) {
    public static final String SEARCH_POLICY = "seedv6.nnue.full-window.incremental.v1";
    public static final String TIMED_SEARCH_POLICY = "seedv6.network.per-move-time.v1";
    public ValidationConfig(int pairs, long seed, int min, int max, int depth, int threads, NnueScoreMapping mapping, int cap) {
        this(pairs, seed, min, max, depth, threads, mapping, cap, 0);
    }
    public ValidationConfig {
        Objects.requireNonNull(scoreMapping, "scoreMapping");
        if (openingPairs < 1 || openingPairs > Integer.MAX_VALUE / 2
                || minimumOpeningPlies < 0 || maximumOpeningPlies < minimumOpeningPlies
                || maximumOpeningPlies > maximumPlies || maximumPlies < 1
                || depth < 1 || depth > ExactSearch.MAX_DEPTH
                || threads < ParallelSearch.MIN_WORKERS || threads > ParallelSearch.MAX_WORKERS
                || moveMillis < 0 || moveMillis > 86_400_000) {
            throw new IllegalArgumentException("Invalid bounded validation configuration.");
        }
    }

    public boolean timed() { return moveMillis > 0; }
    public String searchDescription() { return timed() ? moveMillis + " ms / move" : "Depth " + depth; }
    /** Preserve historical depth-only generation fingerprints byte for byte. */
    @Override public String toString() {
        return "ValidationConfig[openingPairs=" + openingPairs + ", seed=" + seed + ", minimumOpeningPlies=" + minimumOpeningPlies
                + ", maximumOpeningPlies=" + maximumOpeningPlies + ", depth=" + depth + ", threads=" + threads
                + ", scoreMapping=" + scoreMapping + ", maximumPlies=" + maximumPlies
                + (timed() ? ", moveMillis=" + moveMillis : "") + "]";
    }

    public static ValidationConfig defaults(int pairs, long seed) {
        return defaults(pairs, seed, NnueScoreMapping.V1);
    }

    public static ValidationConfig defaults(int pairs, long seed, NnueScoreMapping mapping) {
        return new ValidationConfig(pairs, seed, 0, 8, 4, 1, mapping, 1024);
    }
}
