package com.ohinteractive.seedv6.training.service;

import java.nio.file.Path;
import java.util.Objects;
import java.util.SplittableRandom;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayConfig;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayTraining;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import com.ohinteractive.seedv6.training.validation.ValidationConfig;

/**
 * Fixed for one service instance. Zero maximumGenerations means indefinite; a positive bound counts
 * newly trained generations in this run, excluding startup reconciliation. Resume may explicitly
 * select another depth, but never rewrites old manifests. All other settings are explicit caller
 * choices too; an already recorded validation is always completed under its stored policy.
 * A pending candidate with no durable validation uses the supplied run's validation settings and
 * generation-derived seed. startingFen defaults to normal chess; an explicit legal fixture is useful
 * for bounded experiments. Codecs enforce the accepted V1 model/optimizer schema on every load.
 */
public record TrainerConfig(Path checkpointRoot, long masterSeed, SelfPlay selfPlay, Training training,
                            Validation validation, long maximumGenerations, DepthChange depthChange,
                            String startingFen) {
    public static final String STANDARD_START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public enum DepthChange { REQUIRE_SAME, EXPLICITLY_ALLOW }
    public enum SeedDomain {
        SELF_PLAY(0x6A09E667F3BCC909L), SHUFFLE(0xBB67AE8584CAA73BL), VALIDATION(0x3C6EF372FE94F82BL);
        private final long salt;
        SeedDomain(long salt) { this.salt = salt; }
    }

    public TrainerConfig {
        checkpointRoot = Objects.requireNonNull(checkpointRoot, "checkpointRoot").toAbsolutePath().normalize();
        Objects.requireNonNull(selfPlay); Objects.requireNonNull(training);
        Objects.requireNonNull(validation); Objects.requireNonNull(depthChange);
        Objects.requireNonNull(startingFen);
        if (maximumGenerations < 0) throw new IllegalArgumentException("Negative generation limit.");
        Board.fromFen(startingFen);
    }

    public TrainerConfig(Path root, long seed, SelfPlay selfPlay, Training training, Validation validation,
                         long maximumGenerations, DepthChange depthChange) {
        this(root, seed, selfPlay, training, validation, maximumGenerations, depthChange, STANDARD_START);
    }

    public record SelfPlay(int depth, int threads, int games, int minimumOpeningPlies, int maximumOpeningPlies,
                           int maximumSamplesPerGame, int maximumPlies, NnueScoreMapping scoreMapping) {
        public SelfPlay {
            new SelfPlayConfig(games, depth, threads, 0, minimumOpeningPlies, maximumOpeningPlies,
                    maximumSamplesPerGame, maximumPlies, scoreMapping, -1, -1);
        }
        SelfPlayConfig at(long seed) {
            return new SelfPlayConfig(games, depth, threads, seed, minimumOpeningPlies, maximumOpeningPlies,
                    maximumSamplesPerGame, maximumPlies, scoreMapping, -1, -1);
        }
    }

    public record Training(int epochs, int minibatchSize, boolean shuffle) {
        public Training { new SelfPlayTraining.Config(epochs, minibatchSize, shuffle, 0); }
        SelfPlayTraining.Config at(long seed) { return new SelfPlayTraining.Config(epochs, minibatchSize, shuffle, seed); }
    }

    public record Validation(int openingPairs, int minimumOpeningPlies, int maximumOpeningPlies, int depth,
                             int threads, int maximumPlies, NnueScoreMapping scoreMapping, PromotionPolicy policy) {
        public Validation {
            Objects.requireNonNull(policy);
            new ValidationConfig(openingPairs, 0, minimumOpeningPlies, maximumOpeningPlies,
                    depth, threads, scoreMapping, maximumPlies);
        }
        ValidationConfig at(long seed) {
            return new ValidationConfig(openingPairs, seed, minimumOpeningPlies, maximumOpeningPlies,
                    depth, threads, scoreMapping, maximumPlies);
        }
    }

    /** Indexed, separate RNG streams; no time, scheduling or previous game length enters identity. */
    public long seed(long generation, SeedDomain domain) {
        if (generation < 0) throw new IllegalArgumentException("Negative generation.");
        return new SplittableRandom(masterSeed ^ Objects.requireNonNull(domain).salt
                ^ (generation * 0x9E3779B97F4A7C15L)).nextLong();
    }
    public SelfPlayConfig selfPlay(long generation) { return selfPlay.at(seed(generation, SeedDomain.SELF_PLAY)); }
    public SelfPlayTraining.Config training(long generation) { return training.at(seed(generation, SeedDomain.SHUFFLE)); }
    public ValidationConfig validation(long generation) { return validation.at(seed(generation, SeedDomain.VALIDATION)); }
    public long[] startingBoard() { return Board.fromFen(startingFen); }
}
