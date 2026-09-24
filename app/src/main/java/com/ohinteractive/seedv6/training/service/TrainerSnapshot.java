package com.ohinteractive.seedv6.training.service;

import java.time.Duration;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import java.util.Optional;
import java.util.OptionalLong;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayBatch;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayTraining;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import com.ohinteractive.seedv6.training.validation.ValidationResult;
import com.ohinteractive.seedv6.training.validation.ValidationProgress;
import com.ohinteractive.seedv6.training.validation.ValidationConfig;

/**
 * Immutable, constant-size aggregate view. Optional phase statistics are absent until available;
 * they never contain samples, trajectories, models or game lists. Totals cover this service instance,
 * not reconstructed historical runs. Recovered lifecycles are counted separately from new generations.
 * Validation holds the latest match/decision until the next validation begins; self-play generation
 * may already have advanced. Its completed progress has no live game identity after that advance.
 */
public record TrainerSnapshot(State state, String failureSummary, Duration elapsed, long generation,
                              String bestId, String latestTrainingId, String candidateId,
                              long optimizerStep, int trainingDepth, SelfPlayBatch.Statistics selfPlay,
                              Optional<SelfPlayTraining.Statistics> training,
                              long generationOptimizerUpdates, long generationSamplesTrained,
                              double meanTrainingLoss, Optional<ValidationResult.Statistics> validation,
                              Optional<PromotionPolicy.Assessment> assessment, Totals totals,
                              Optional<ValidationProgress> validationProgress,
                              Optional<ValidationDetails> validationDetails,
                              Optional<ActiveGameSnapshot> activeGame,
                              Optional<BootstrapValidation> bootstrapValidation,
                              Optional<RunDetails> run, Optional<LossProgress> lossProgress) {
    public record RunDetails(TrainerConfig effective, TrainingSource source, BrnSupervision supervision,
                             long firstGeneration, long targetGeneration, String action, boolean timeLimitReached,
                             long trainingSampleTarget, boolean generationSettingsKnown, GenerationTiming generationTiming) {
        public RunDetails(TrainerConfig effective, TrainingSource source, BrnSupervision supervision,
                          long firstGeneration, long targetGeneration, String action, boolean timeLimitReached,
                          long trainingSampleTarget, boolean generationSettingsKnown) {
            this(effective, source, supervision, firstGeneration, targetGeneration, action, timeLimitReached,
                    trainingSampleTarget, generationSettingsKnown, null);
        }
        public RunDetails(TrainerConfig effective, TrainingSource source, BrnSupervision supervision,
                          long firstGeneration, long targetGeneration, String action, boolean timeLimitReached, long trainingSampleTarget) {
            this(effective, source, supervision, firstGeneration, targetGeneration, action, timeLimitReached, trainingSampleTarget, true);
        }
        public long ordinal(long generation) { return Math.max(0, generation - firstGeneration + 1); }
    }
    /** Immutable publication of the history clock. Accumulated active time survives a safe resume. */
    public record GenerationTiming(long generation, long accumulatedNanos, long startedNanos, boolean ticking) {
        public Duration elapsed(long now) {
            return Duration.ofNanos(accumulatedNanos + (ticking ? Math.max(0, now - startedNanos) : 0));
        }
    }
    public Optional<Duration> generationElapsed(long now) {
        return run.map(RunDetails::generationTiming).filter(t -> t.generation() == generation).map(t -> t.elapsed(now));
    }
    public record LossProgress(long completed, long total, int heldOutSamples) {
        public LossProgress {
            if (completed < 0 || total < completed || heldOutSamples < 0) throw new IllegalArgumentException("Invalid held-out progress.");
        }
    }
    public TrainerSnapshot withRun(Optional<RunDetails> value, Optional<LossProgress> progress) {
        return new TrainerSnapshot(state, failureSummary, elapsed, generation, bestId, latestTrainingId, candidateId,
                optimizerStep, trainingDepth, selfPlay, training, generationOptimizerUpdates, generationSamplesTrained,
                meanTrainingLoss, validation, assessment, totals, validationProgress, validationDetails, activeGame,
                bootstrapValidation, value, progress);
    }
    public TrainerSnapshot(State state, String failureSummary, Duration elapsed, long generation,
            String bestId, String latestTrainingId, String candidateId, long optimizerStep, int trainingDepth,
            SelfPlayBatch.Statistics selfPlay, Optional<SelfPlayTraining.Statistics> training,
            long generationOptimizerUpdates, long generationSamplesTrained, double meanTrainingLoss,
            Optional<ValidationResult.Statistics> validation, Optional<PromotionPolicy.Assessment> assessment, Totals totals,
            Optional<ValidationProgress> validationProgress, Optional<ValidationDetails> validationDetails,
            Optional<ActiveGameSnapshot> activeGame, Optional<BootstrapValidation> bootstrapValidation) {
        this(state, failureSummary, elapsed, generation, bestId, latestTrainingId, candidateId, optimizerStep, trainingDepth,
                selfPlay, training, generationOptimizerUpdates, generationSamplesTrained, meanTrainingLoss, validation,
                assessment, totals, validationProgress, validationDetails, activeGame, bootstrapValidation, Optional.empty(), Optional.empty());
    }
    public record BootstrapValidation(String candidateId, String incumbentId,
                                      com.ohinteractive.seedv6.training.checkpoint.BootstrapEvidence evidence) {}
    public TrainerSnapshot {
        activeGame = activeGame.filter(game -> game.generation() == generation
                && (state == State.GENERATING_SELF_PLAY && game.phase() == ActiveGameSnapshot.Phase.SELF_PLAY
                || state == State.VALIDATING && game.phase() == ActiveGameSnapshot.Phase.VALIDATION));
    }
    public TrainerSnapshot(State state, String failureSummary, Duration elapsed, long generation,
                           String bestId, String latestTrainingId, String candidateId,
                           long optimizerStep, int trainingDepth, SelfPlayBatch.Statistics selfPlay,
                           Optional<SelfPlayTraining.Statistics> training, long generationOptimizerUpdates,
                           long generationSamplesTrained, double meanTrainingLoss,
                           Optional<ValidationResult.Statistics> validation,
                           Optional<PromotionPolicy.Assessment> assessment, Totals totals,
                           Optional<ValidationProgress> validationProgress, Optional<ValidationDetails> validationDetails,
                           Optional<ActiveGameSnapshot> activeGame) {
        this(state, failureSummary, elapsed, generation, bestId, latestTrainingId, candidateId,
                optimizerStep, trainingDepth, selfPlay, training, generationOptimizerUpdates,
                generationSamplesTrained, meanTrainingLoss, validation, assessment, totals,
                validationProgress, validationDetails, activeGame, Optional.empty());
    }
    public TrainerSnapshot withBootstrapValidation(Optional<BootstrapValidation> value) {
        return new TrainerSnapshot(state, failureSummary, elapsed, generation, bestId, latestTrainingId, candidateId,
                optimizerStep, trainingDepth, selfPlay, training, generationOptimizerUpdates, generationSamplesTrained,
                meanTrainingLoss, validation, assessment, totals, validationProgress, validationDetails, activeGame, value, run, lossProgress);
    }
    /** Compatibility for aggregate-only publications and existing fixtures. */
    public TrainerSnapshot(State state, String failureSummary, Duration elapsed, long generation,
                           String bestId, String latestTrainingId, String candidateId,
                           long optimizerStep, int trainingDepth, SelfPlayBatch.Statistics selfPlay,
                           Optional<SelfPlayTraining.Statistics> training, long generationOptimizerUpdates,
                           long generationSamplesTrained, double meanTrainingLoss,
                           Optional<ValidationResult.Statistics> validation,
                           Optional<PromotionPolicy.Assessment> assessment, Totals totals,
                           Optional<ValidationProgress> validationProgress, Optional<ValidationDetails> validationDetails) {
        this(state, failureSummary, elapsed, generation, bestId, latestTrainingId, candidateId,
                optimizerStep, trainingDepth, selfPlay, training, generationOptimizerUpdates,
                generationSamplesTrained, meanTrainingLoss, validation, assessment, totals,
                validationProgress, validationDetails, Optional.empty());
    }

    public TrainerSnapshot withActiveGame(ActiveGameSnapshot game) {
        return new TrainerSnapshot(state, failureSummary, elapsed, generation, bestId, latestTrainingId, candidateId,
                optimizerStep, trainingDepth, selfPlay, training, generationOptimizerUpdates,
                generationSamplesTrained, meanTrainingLoss, validation, assessment, totals,
                validationProgress, validationDetails, Optional.ofNullable(game), bootstrapValidation, run, lossProgress);
    }
    /** The actual match inputs, retained with its results even across recovery or settings changes. */
    public record ValidationDetails(String candidateId, String bestId, ValidationConfig config, PromotionPolicy policy,
                                    OptionalLong candidateGeneration, OptionalLong bestGeneration,
                                    int restoredWins, int restoredLosses) {
        public ValidationDetails(String candidateId, String bestId, ValidationConfig config, PromotionPolicy policy,
                                 OptionalLong candidateGeneration, OptionalLong bestGeneration) {
            this(candidateId, bestId, config, policy, candidateGeneration, bestGeneration, 0, 0);
        }
        /** Recovered validation records retain IDs, but do not store separate generation metadata. */
        public ValidationDetails(String candidateId, String bestId, ValidationConfig config, PromotionPolicy policy) {
            this(candidateId, bestId, config, policy, OptionalLong.empty(), OptionalLong.empty());
        }
    }

    public TrainerSnapshot(State state, String failureSummary, Duration elapsed, long generation,
                           String bestId, String latestTrainingId, String candidateId,
                           long optimizerStep, int trainingDepth, SelfPlayBatch.Statistics selfPlay,
                           Optional<SelfPlayTraining.Statistics> training, long generationOptimizerUpdates,
                           long generationSamplesTrained, double meanTrainingLoss,
                           Optional<ValidationResult.Statistics> validation,
                           Optional<PromotionPolicy.Assessment> assessment, Totals totals,
                           Optional<ValidationProgress> validationProgress) {
        this(state, failureSummary, elapsed, generation, bestId, latestTrainingId, candidateId,
                optimizerStep, trainingDepth, selfPlay, training, generationOptimizerUpdates,
                generationSamplesTrained, meanTrainingLoss, validation, assessment, totals, validationProgress, Optional.empty());
    }
    /** Compatibility constructor for snapshots without live validation telemetry. */
    public TrainerSnapshot(State state, String failureSummary, Duration elapsed, long generation,
                           String bestId, String latestTrainingId, String candidateId,
                           long optimizerStep, int trainingDepth, SelfPlayBatch.Statistics selfPlay,
                           Optional<SelfPlayTraining.Statistics> training, long generationOptimizerUpdates,
                           long generationSamplesTrained, double meanTrainingLoss,
                           Optional<ValidationResult.Statistics> validation,
                           Optional<PromotionPolicy.Assessment> assessment, Totals totals) {
        this(state, failureSummary, elapsed, generation, bestId, latestTrainingId, candidateId,
                optimizerStep, trainingDepth, selfPlay, training, generationOptimizerUpdates,
                generationSamplesTrained, meanTrainingLoss, validation, assessment, totals, Optional.empty());
    }

    public enum State {
        IDLE, RECOVERING, GENERATING_SELF_PLAY, TRAINING, PUBLISHING_CANDIDATE,
        VALIDATING, RECORDING_DECISION, STOPPING, STOPPED, FAILED
    }
    public record Totals(long completedGenerations, long selfPlayGames, long completedGames,
                         long abortedGames, long cappedGames, long sampledPositions, long optimizerUpdates,
                         long promotions, long retainedCandidates, long incompleteValidations,
                         long recoveredLifecycles) {}

    public boolean running() { return state != State.IDLE && state != State.STOPPED && state != State.FAILED; }
    public boolean stopping() { return state == State.STOPPING; }
    public boolean failed() { return state == State.FAILED; }

    TrainerSnapshot withState(State next, String failure, Duration runtime) {
        return new TrainerSnapshot(next, failure, runtime, generation, bestId, latestTrainingId, candidateId,
                optimizerStep, trainingDepth, selfPlay, training, generationOptimizerUpdates,
                generationSamplesTrained, meanTrainingLoss, validation, assessment, totals, validationProgress, validationDetails, activeGame, bootstrapValidation, run, lossProgress);
    }
}
