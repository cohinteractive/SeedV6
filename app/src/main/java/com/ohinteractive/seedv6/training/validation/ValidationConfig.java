package com.ohinteractive.seedv6.training.validation;

import java.util.Objects;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;

/** Fixed depth, no node or wall-clock limits. Ply cap applies after the common opening. */
public record ValidationConfig(int openingPairs, long seed, int minimumOpeningPlies,
                               int maximumOpeningPlies, int depth, int threads,
                               NnueScoreMapping scoreMapping, int maximumPlies) {
    public static final String SEARCH_POLICY = "seedv6.nnue.full-window.incremental.v1";
    public ValidationConfig {
        Objects.requireNonNull(scoreMapping, "scoreMapping");
        if (openingPairs < 1 || openingPairs > Integer.MAX_VALUE / 2
                || minimumOpeningPlies < 0 || maximumOpeningPlies < minimumOpeningPlies
                || maximumOpeningPlies > maximumPlies || maximumPlies < 1
                || depth < 1 || depth > AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH
                || threads < RootParallelSearch.MIN_WORKERS || threads > RootParallelSearch.MAX_WORKERS) {
            throw new IllegalArgumentException("Invalid bounded validation configuration.");
        }
    }

    public static ValidationConfig defaults(int pairs, long seed) {
        return defaults(pairs, seed, NnueScoreMapping.V1);
    }

    public static ValidationConfig defaults(int pairs, long seed, NnueScoreMapping mapping) {
        return new ValidationConfig(pairs, seed, 0, 8, 4, 1, mapping, 1024);
    }
}
