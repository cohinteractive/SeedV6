package com.ohinteractive.seedv6.training.service;

import java.nio.file.Path;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
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
 * Unfinished work records its effective settings. Changed settings restart from its settled parent;
 * unchanged settings preserve exact continuation. Legacy ordinary Candidates predate this record
 * and expose only their stored depth/source for comparison. startingFen defaults to normal chess; an explicit legal fixture is useful
 * for bounded experiments. Codecs enforce the accepted V1 model/optimizer schema on every load.
 */
public record TrainerConfig(Path checkpointRoot, long masterSeed, SelfPlay selfPlay, Training training,
                            Validation validation, long maximumGenerations, DepthChange depthChange,
                            String startingFen, TrainingArchitecture architecture, double brnLearningRate,
                            TrainingSource source, BrnSupervision supervision, BrnRunSeeds runSeeds) {
    public static final double DEFAULT_BRN_LEARNING_RATE = 0.001;
    public static final String STANDARD_START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public enum DepthChange { REQUIRE_SAME, EXPLICITLY_ALLOW }
    public enum SeedDomain {
        SELF_PLAY(0x6A09E667F3BCC909L), SHUFFLE(0xBB67AE8584CAA73BL), VALIDATION(0x3C6EF372FE94F82BL),
        HOLDOUT(0xA54FF53A5F1D36F1L);
        private final long salt;
        SeedDomain(long salt) { this.salt = salt; }
    }

    public TrainerConfig {
        checkpointRoot = Objects.requireNonNull(checkpointRoot, "checkpointRoot").toAbsolutePath().normalize();
        Objects.requireNonNull(selfPlay); Objects.requireNonNull(training);
        Objects.requireNonNull(validation); Objects.requireNonNull(depthChange);
        Objects.requireNonNull(startingFen); Objects.requireNonNull(architecture);
        new BrnAdamConfig(brnLearningRate);
        if (runSeeds != null && (architecture != TrainingArchitecture.BRN2 || masterSeed != runSeeds.masterSeed()))
            throw new IllegalArgumentException("Persisted run seeds require BRN-2 and matching master seed.");
        if (supervision != null) supervision.requireSupported(architecture, source);
        if (architecture == TrainingArchitecture.NNUE && source != null && source.bootstrap())
            throw new IllegalArgumentException("NNUE training does not support BRN bootstrap mode.");
        if (architecture != TrainingArchitecture.NNUE && (training.epochs() != 1 || training.minibatchSize() != 1))
            throw new IllegalArgumentException("BRN requires one online pass per generation.");
        if (maximumGenerations < 0) throw new IllegalArgumentException("Negative generation limit.");
        if (architecture != TrainingArchitecture.NNUE
                && (!NnueScoreMapping.V1.equals(selfPlay.scoreMapping()) || !NnueScoreMapping.V1.equals(validation.scoreMapping())))
            throw new IllegalArgumentException("BRN uses fixed full-range search units for self-play and validation.");
        Board.fromFen(startingFen);
    }

    public TrainerConfig(Path root, long seed, SelfPlay selfPlay, Training training, Validation validation,
                         long maximumGenerations, DepthChange depthChange, String startingFen,
                         TrainingArchitecture architecture, double brnLearningRate, TrainingSource source, BrnSupervision supervision) {
        this(root, seed, selfPlay, training, validation, maximumGenerations, depthChange, startingFen,
                architecture, brnLearningRate, source, supervision, null);
    }
    public TrainerConfig withRunSeeds(BrnRunSeeds value) {
        return new TrainerConfig(checkpointRoot, value == null ? masterSeed : value.masterSeed(), selfPlay, training,
                validation, maximumGenerations, depthChange, startingFen, architecture, brnLearningRate, source, supervision, value);
    }

    /** Null supervision restores durable lineage semantics; legacy/fresh absence means WDL. */
    public TrainerConfig(Path root, long seed, SelfPlay selfPlay, Training training, Validation validation,
                         long maximumGenerations, DepthChange depthChange, String startingFen,
                         TrainingArchitecture architecture, double brnLearningRate, TrainingSource source) {
        this(root, seed, selfPlay, training, validation, maximumGenerations, depthChange, startingFen,
                architecture, brnLearningRate, source, null);
    }
    public TrainerConfig withSupervision(BrnSupervision value) {
        return new TrainerConfig(checkpointRoot, masterSeed, selfPlay, training, validation, maximumGenerations,
                depthChange, startingFen, architecture, brnLearningRate, source, value, runSeeds);
    }

    /** Null source restores a stored selection; new BRN lineages require an explicit NNUE generator. */
    public TrainerConfig(Path root, long seed, SelfPlay selfPlay, Training training, Validation validation,
                         long maximumGenerations, DepthChange depthChange, String startingFen,
                         TrainingArchitecture architecture, double brnLearningRate) {
        this(root, seed, selfPlay, training, validation, maximumGenerations, depthChange, startingFen,
                architecture, brnLearningRate, null);
    }

    public TrainerConfig withSource(TrainingSource value) {
        return new TrainerConfig(checkpointRoot, masterSeed, selfPlay, training, validation, maximumGenerations,
                depthChange, startingFen, architecture, brnLearningRate, value, supervision, runSeeds);
    }

    /** Excludes run duration and fresh-only learning rate; resume restores the exact stored optimizer. */
    public String generationSettings(long generation) {
        return selfPlay(generation) + "|" + training(generation) + "|" + seed(generation, SeedDomain.HOLDOUT) + "|" + startingFen + (runSeeds == null ? "" : runSeeds.settingsSuffix());
    }

    /** Only effective generation settings: bootstrap has no game-pair validation; rates are fresh-only. */
    public String attemptSettings(long generation, TrainingSource selected) {
        return generationSettings(generation) + (selected.bootstrap() ? "" : "|" + validation(generation) + "|" + validation.policy());
    }

    public TrainerConfig(Path root, long seed, SelfPlay selfPlay, Training training, Validation validation,
                         long maximumGenerations, DepthChange depthChange, String startingFen) {
        this(root, seed, selfPlay, training, validation, maximumGenerations, depthChange, startingFen,
                TrainingArchitecture.NNUE, DEFAULT_BRN_LEARNING_RATE);
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
        long streamSeed = runSeeds != null && domain == SeedDomain.SELF_PLAY ? runSeeds.dataSeed() : masterSeed;
        return new SplittableRandom(streamSeed ^ Objects.requireNonNull(domain).salt
                ^ (generation * 0x9E3779B97F4A7C15L)).nextLong();
    }
    public SelfPlayConfig selfPlay(long generation) { return selfPlay.at(seed(generation, SeedDomain.SELF_PLAY)); }
    public SelfPlayTraining.Config training(long generation) { return training.at(seed(generation, SeedDomain.SHUFFLE)); }
    public ValidationConfig validation(long generation) { return validation.at(seed(generation, SeedDomain.VALIDATION)); }
    public long[] startingBoard() { return Board.fromFen(startingFen); }
}
