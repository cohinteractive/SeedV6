package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.validation.ValidationConfig;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import java.util.Objects;

/** Immutable experimental contract. Rounds count shared exposure, never architecture generations. */
public record LearningArenaConfig(String name, Competitor a, Competitor b, DataSource source,
        int positionsPerRound, int epochs, int rounds, long seed, Arena arena) {
    public record Competitor(String name, TrainingArchitecture architecture, long seed, int minibatch) {
        public Competitor {
            if (name == null || name.isBlank() || minibatch < 1 || minibatch > 100000)
                throw new IllegalArgumentException("Competitor needs a name and positive minibatch size");
            LearningArenaTraining.requireSupported(architecture);
        }
    }
    public enum Limit { DEPTH, TIME }
    public record Arena(int games, Limit limit, int depth, long millis, int threads,
                        int openingMin, int openingMax, int maximumPlies, String startingFen) {
        public Arena {
            Objects.requireNonNull(limit); Objects.requireNonNull(startingFen);
            if (games < 2 || games % 2 != 0 || millis < 1 || millis > 3600000)
                throw new IllegalArgumentException("Use an even game count and 1..3600000 ms per move");
            new ValidationConfig(games / 2, 0, openingMin, openingMax, depth, threads, NnueScoreMapping.V1, maximumPlies);
            Board.fromFen(startingFen);
        }
        public ValidationConfig matches(long seed) {
            return new ValidationConfig(games / 2, seed, openingMin, openingMax,
                    limit == Limit.DEPTH ? depth : ExactSearch.MAX_DEPTH, threads, NnueScoreMapping.V1, maximumPlies);
        }
        public long timeLimit() { return limit == Limit.TIME ? millis : -1; }
    }
    public LearningArenaConfig {
        if (name == null || name.isBlank() || positionsPerRound < 1 || positionsPerRound > 10000000
                || epochs < 1 || epochs > 100000 || rounds < 0)
            throw new IllegalArgumentException("Invalid Learning Arena campaign bounds (0 rounds means until paused)");
        Objects.requireNonNull(a); Objects.requireNonNull(b); Objects.requireNonNull(source); Objects.requireNonNull(arena);
        CorpusTraining.targetPolicy(a.architecture(), source.labelProfile());
        CorpusTraining.targetPolicy(b.architecture(), source.labelProfile());
        Math.multiplyExact((long) positionsPerRound, epochs);
    }
    public String identity() {
        return DataFiles.hash("learning-arena-v1:" + LearningArenaTraining.recipe(a.architecture()) + ":"
                + LearningArenaTraining.recipe(b.architecture()) + ":"
                + CorpusTraining.targetPolicy(a.architecture(), source.labelProfile()).policy + ":"
                + CorpusTraining.targetPolicy(b.architecture(), source.labelProfile()).policy + ":" + DataFiles.JSON.toJson(this));
    }
}
