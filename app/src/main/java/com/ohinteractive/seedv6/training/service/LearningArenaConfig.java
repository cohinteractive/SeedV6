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
        int positionsPerRound, int epochs, int rounds, long seed, Arena arena, Mode mode) {
    public enum Mode {
        LEARNING("Learning campaign"), MATCH_ONLY("Match only");
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public LearningArenaConfig(String name, Competitor a, Competitor b, DataSource source,
            int positionsPerRound, int epochs, int rounds, long seed, Arena arena) {
        this(name, a, b, source, positionsPerRound, epochs, rounds, seed, arena, null);
    }
    public boolean matchOnly() { return mode == Mode.MATCH_ONLY; }
    public static LearningArenaConfig match(String name, Competitor a, Competitor b, long seed, Arena arena) {
        return new LearningArenaConfig(name, a, b, null, 0, 0, 0, seed, arena, Mode.MATCH_ONLY);
    }
    public enum Evaluator { HCE }
    /** A concrete source snapshot. Source stores are read only; each competitor gets its own campaign lineage. */
    public record InitialModel(String root, String lineageId, String name, String checkpoint, long generation) {
        public InitialModel {
            if (root == null || root.isBlank() || name == null || name.isBlank() || generation < 0 || checkpoint == null
                    || !checkpoint.matches("g[0-9]{6,19}-s[0-9]{9,19}-[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid initial model binding");
            java.nio.file.Path.of(root); if (lineageId != null) java.util.UUID.fromString(lineageId);
        }
        public static InitialModel from(com.ohinteractive.seedv6.training.model.ModelLibrary.Binding binding) {
            return new InitialModel(binding.root().toString(), binding.lineageId().map(Object::toString).orElse(null), binding.lineageName(), binding.checkpointId(), binding.generation());
        }
    }
    public record Competitor(String name, TrainingArchitecture architecture, long seed, int minibatch,
                             Double learningRate, InitialModel initialModel, String nnueObjective, Evaluator evaluator) {
        public Competitor(String name, TrainingArchitecture architecture, long seed, int minibatch,
                          Double learningRate, InitialModel initialModel, String nnueObjective) {
            this(name, architecture, seed, minibatch, learningRate, initialModel, nnueObjective, null);
        }
        public boolean isHandcrafted() { return evaluator == Evaluator.HCE; }
        public String displayName() { return isHandcrafted() ? "HCE" : architecture.displayName(); }
        public static Competitor handcrafted() { return new Competitor("HCE", null, 0, 1, null, null, null, Evaluator.HCE); }
        public static Competitor fixed(com.ohinteractive.seedv6.training.model.ModelLibrary.Binding binding) {
            return new Competitor(binding.lineageName(), binding.architecture(), 0, 1, null, InitialModel.from(binding), null);
        }
        /** Null additive fields serialize exactly like historical campaign JSON. */
        public Competitor(String name, TrainingArchitecture architecture, long seed, int minibatch) { this(name, architecture, seed, minibatch, null, null); }
        public Competitor(String name, TrainingArchitecture architecture, long seed, int minibatch, Double learningRate, InitialModel initialModel) {
            this(name, architecture, seed, minibatch, learningRate, initialModel,
                    architecture == TrainingArchitecture.NNUE_MATERIAL && initialModel == null
                            ? com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.CALIBRATED_TRAINING_ID : null);
        }
        public Competitor {
            if (name == null || name.isBlank() || minibatch < 1 || minibatch > 100000)
                throw new IllegalArgumentException("Competitor needs a name and positive minibatch size");
            if (evaluator == Evaluator.HCE) {
                if (architecture != null || initialModel != null || learningRate != null || nnueObjective != null)
                    throw new IllegalArgumentException("HCE has no network or training recipe");
            } else {
                Objects.requireNonNull(architecture);
                if (initialModel == null) LearningArenaTraining.requireSupported(architecture);
            }
            if (nnueObjective != null && (architecture != TrainingArchitecture.NNUE_MATERIAL
                    || !nnueObjective.equals(com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.CALIBRATED_TRAINING_ID)))
                throw new IllegalArgumentException("Unsupported NNUE objective binding");
            if (learningRate != null) new com.ohinteractive.seedv6.training.model.TrainingRecipe(learningRate, minibatch, 1);
        }
    }
    public enum Limit { DEPTH, TIME }
    public record Arena(int games, Limit limit, int depth, long millis, int threads,
                        int openingMin, int openingMax, int maximumPlies, String startingFen) {
        public Arena {
            Objects.requireNonNull(limit); Objects.requireNonNull(startingFen);
            if (games < 2 || games % 2 != 0 || millis < 1 || millis > 3600000)
                throw new IllegalArgumentException("Use an even game count and 1..3600000 ms per move");
            new ValidationConfig(games / 2, 0, openingMin, openingMax, depth,
                    new com.ohinteractive.seedv6.search.exact.SearchThreads(threads).resolve(), NnueScoreMapping.V1, maximumPlies);
            Board.fromFen(startingFen);
        }
        public ValidationConfig matches(long seed) {
            return new ValidationConfig(games / 2, seed, openingMin, openingMax,
                    limit == Limit.DEPTH ? depth : ExactSearch.MAX_DEPTH,
                    new com.ohinteractive.seedv6.search.exact.SearchThreads(threads).resolve(), NnueScoreMapping.V1, maximumPlies);
        }
        public long timeLimit() { return limit == Limit.TIME ? millis : -1; }
    }
    public LearningArenaConfig {
        boolean fixed = mode == Mode.MATCH_ONLY;
        if (name == null || name.isBlank() || !fixed && (positionsPerRound < 1 || positionsPerRound > 10000000
                || epochs < 1 || epochs > 100000 || rounds < 0))
            throw new IllegalArgumentException("Invalid Learning Arena campaign bounds (0 rounds means until paused)");
        Objects.requireNonNull(a); Objects.requireNonNull(b); Objects.requireNonNull(arena);
        if (fixed) {
            if (source != null || positionsPerRound != 0 || epochs != 0 || rounds != 0)
                throw new IllegalArgumentException("Match-only Arena has no training exposure");
            for (var c : new Competitor[]{a, b})
                if (!c.isHandcrafted() && (c.initialModel() == null || c.learningRate() != null))
                    throw new IllegalArgumentException("Match-only networks require a fixed checkpoint and unchanged recipe");
        } else {
            Objects.requireNonNull(source);
            for (var c : new Competitor[]{a, b}) {
                if (c.isHandcrafted()) throw new IllegalArgumentException("HCE requires match-only Arena");
                LearningArenaTraining.requireSupported(c.architecture());
                CorpusTraining.targetPolicy(c.architecture(), source.labelProfile());
            }
        }
        Math.multiplyExact((long) positionsPerRound, epochs);
    }
    public String identity() {
        if (matchOnly()) return DataFiles.hash("arena-match-v1:" + DataFiles.JSON.toJson(this));
        return DataFiles.hash("learning-arena-v1:" + LearningArenaTraining.recipe(a) + ":"
                + LearningArenaTraining.recipe(b) + ":"
                + CorpusTraining.targetPolicy(a.architecture(), source.labelProfile()).policy + ":"
                + CorpusTraining.targetPolicy(b.architecture(), source.labelProfile()).policy + ":" + DataFiles.JSON.toJson(this));
    }
}
