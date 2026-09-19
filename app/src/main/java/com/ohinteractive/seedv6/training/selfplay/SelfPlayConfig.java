package com.ohinteractive.seedv6.training.selfplay;

import java.util.Objects;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;

/** Finite generation bounds. Search node/time limits apply per move; -1 means absent. */
public record SelfPlayConfig(int games, int depth, int threads, long seed,
                             int minimumOpeningPlies, int maximumOpeningPlies,
                             int maximumSamplesPerGame, int maximumPlies,
                             NnueScoreMapping scoreMapping, long nodesPerMove, long millisPerMove) {
    public SelfPlayConfig {
        Objects.requireNonNull(scoreMapping, "scoreMapping");
        if (games < 1 || depth < 1 || depth > AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH
                || threads < RootParallelSearch.MIN_WORKERS || threads > RootParallelSearch.MAX_WORKERS
                || minimumOpeningPlies < 0 || maximumOpeningPlies < minimumOpeningPlies
                || maximumOpeningPlies > maximumPlies || maximumPlies < 1
                || maximumSamplesPerGame < 1 || (long) games * maximumSamplesPerGame > Integer.MAX_VALUE
                || nodesPerMove < -1 || millisPerMove < -1 || millisPerMove > Long.MAX_VALUE / 1_000_000) {
            throw new IllegalArgumentException("Invalid bounded self-play configuration.");
        }
    }

    public static SelfPlayConfig defaults(int games, long seed) {
        return defaults(games, seed, NnueScoreMapping.V1);
    }

    public static SelfPlayConfig defaults(int games, long seed, NnueScoreMapping mapping) {
        return new SelfPlayConfig(games, 4, 1, seed, 0, 8,
                TrajectorySampler.DEFAULT_MAXIMUM_SAMPLES, 1024, mapping, -1, -1);
    }
}
