package com.ohinteractive.seedv6.training.service;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.nnue.TrainingStateCodec;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.*;

/**
 * Single-use, headless, single-owner autonomous trainer. Poll snapshot(); no UI or callback thread is
 * required. Fresh startup privately copies the caller's exact model/Adam state before start(). Resume
 * has no replacement model. A new instance is required after stop/failure.
 *
 * A durable generation is: freeze latest-training actor, generate, train, publish immutable candidate,
 * advance latest-training, validate persisted candidate against persisted best, record assessment,
 * finish any evidence-gated best update. Only then may the next generation load latest-training.
 * Startup completes a pending candidate's existing decision (or runs its missing validation) first.
 * RETAIN and INCONCLUSIVE leave best alone and keep the candidate as the learning parent.
 *
 * stop() is a nonblocking cooperative request. Search observes its existing control; training observes
 * optimizer boundaries. Once PUBLISHING_CANDIDATE begins, publication and a cancelled validation's
 * decision are allowed to finish. RECORDING_DECISION is likewise non-interruptible. No thread interrupt
 * is used to tear down file-channel writes. Filesystem stalls cannot have a universal deadline:
 * awaitTermination(timeout) reports this honestly, and close() waits at most 30 seconds then throws.
 * STOPPED/FAILED is published only after the store lock and synchronous search resources are released.
 */
public final class TrainerService implements AutoCloseable {
    private static final AtomicLong WORKER_IDS = new AtomicLong();
    private final TrainerConfig config;
    private final Operations operations;
    private final Consumer<TrainerSnapshot> observer;
    private final Object gate = new Object();
    private final SelfPlayControl selfPlayControl = new SelfPlayControl();
    private final ValidationControl validationControl = new ValidationControl();
    private volatile TrainerSnapshot published;
    private volatile Thread worker;
    private volatile boolean stopRequested;
    private volatile Throwable failure;
    private volatile long startedNanos, endedNanos;
    private byte[] initialState;

    // Worker-owned aggregates only. Readers receive one immutable, volatile publication.
    private long generation, optimizerStep, updates, samplesTrained;
    private String bestId = "", latestId = "", candidateId = "";
    private SelfPlayBatch.Statistics games;
    private Optional<SelfPlayTraining.Statistics> training = Optional.empty();
    private Optional<ValidationResult.Statistics> validation = Optional.empty();
    private Optional<ValidationProgress> validationProgress = Optional.empty();
    private Optional<TrainerSnapshot.ValidationDetails> validationDetails = Optional.empty();
    private Optional<PromotionPolicy.Assessment> assessment = Optional.empty();
    private double meanLoss = Double.NaN;
    private long completed, totalGames, totalCompleted, totalAborted, totalCapped, totalSamples, totalUpdates;
    private long promotions, retains, inconclusive, recoveredLifecycles;

    public static TrainerService fresh(TrainerConfig config, NnueTrainer initial) throws IOException {
        return fresh(config, initial, new Operations(), snapshot -> {});
    }
    public static TrainerService resume(TrainerConfig config) {
        return resume(config, new Operations(), snapshot -> {});
    }
    static TrainerService fresh(TrainerConfig config, NnueTrainer initial, Operations operations,
                                Consumer<TrainerSnapshot> observer) throws IOException {
        return new TrainerService(config, TrainingStateCodec.encode(Objects.requireNonNull(initial)), operations, observer);
    }
    static TrainerService resume(TrainerConfig config, Operations operations, Consumer<TrainerSnapshot> observer) {
        return new TrainerService(config, null, operations, observer);
    }
    private TrainerService(TrainerConfig config, byte[] initial, Operations operations, Consumer<TrainerSnapshot> observer) {
        this.config = Objects.requireNonNull(config);
        this.operations = Objects.requireNonNull(operations);
        this.observer = Objects.requireNonNull(observer);
        initialState = initial;
        games = emptyGames();
        published = view(IDLE);
    }

    /** Exactly one start; concurrent or repeated starts are rejected, including after close(). */
    public void start() {
        synchronized (gate) {
            if (published.state() != IDLE) throw new IllegalStateException("Trainer service is single-use and already started/stopped.");
            startedNanos = System.nanoTime();
            published = published.withState(RECOVERING, "", Duration.ZERO);
            worker = new Thread(this::run, "seedv6-trainer-" + WORKER_IDS.incrementAndGet());
            try { worker.start(); }
            catch (RuntimeException | Error unable) {
                failure = unable; initialState = null; endedNanos = System.nanoTime();
                published = published.withState(FAILED, unable.toString(), elapsed());
                throw unable;
            }
        }
    }

    public void stop() {
        synchronized (gate) {
            if (published.state() == STOPPED || published.state() == FAILED) return;
            stopRequested = true;
            selfPlayControl.cancel(); validationControl.cancel();
            if (published.state() == IDLE) {
                initialState = null;
                published = published.withState(STOPPED, "", Duration.ZERO);
            } else published = published.withState(STOPPING, "", elapsed());
        }
    }

    public TrainerConfig config() { return config; }
    public TrainerSnapshot snapshot() {
        TrainerSnapshot current = published;
        return current.withState(current.state(), current.failureSummary(), elapsed());
    }
    public Optional<Throwable> failure() { return Optional.ofNullable(failure); }
    public boolean isTerminated() {
        Thread owned = worker;
        return (published.state() == STOPPED || published.state() == FAILED) && (owned == null || !owned.isAlive());
    }
    public boolean awaitTermination(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout);
        if (timeout.isNegative()) throw new IllegalArgumentException("Negative timeout.");
        Thread owned = worker;
        if (owned == Thread.currentThread()) throw new IllegalStateException("Worker cannot join itself.");
        if (owned != null && !timeout.isZero()) owned.join(timeout);
        return isTerminated();
    }
    @Override public void close() {
        stop();
        try {
            if (!awaitTermination(Duration.ofSeconds(30))) throw new IllegalStateException("Trainer is still stopping; durable I/O has not drained.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while awaiting trainer shutdown.", interrupted);
        }
    }

    private void run() {
        try {
            phase(RECOVERING);
            if (!stopRequested) {
                try (CheckpointStore store = operations.open(config)) {
                    execute(store);
                }
            }
        } catch (Throwable unexpected) {
            failure = unexpected;
            selfPlayControl.cancel(); validationControl.cancel();
        } finally {
            initialState = null;
            synchronized (gate) {
                endedNanos = System.nanoTime();
                published = view(failure == null ? STOPPED : FAILED);
            }
        }
    }

    private void execute(CheckpointStore store) throws IOException {
        if (stopRequested) return;
        CheckpointStore.Recovery refs;
        if (initialState != null) {
            store.requireEmptyForBootstrap();
            if (stopRequested) return;
            NnueTrainer initial = TrainingStateCodec.decode(initialState);
            initialState = null;
            store.initialize(initial, new CheckpointManifest.Metadata(0, config.selfPlay().depth(), ""));
            refs = store.recover();
        } else refs = store.recoverTrainingReferences();
        updateReferences(refs);
        CheckpointManifest latest = refs.latestTraining().orElseThrow().manifest();
        generation = latest.generation(); optimizerStep = latest.optimizerStep();
        if (latest.trainingDepth() != config.selfPlay().depth()
                && config.depthChange() != TrainerConfig.DepthChange.EXPLICITLY_ALLOW) {
            throw new IOException("Resume training depth differs; explicitly allow a depth change in the run configuration.");
        }
        phase(RECOVERING);
        if (stopRequested) return;
        PromotionRecord accepted = refs.bestEvidence().orElseThrow(() -> new IOException("Missing best/bootstrap evidence."));
        if (!(accepted.kind() == PromotionRecord.Kind.BOOTSTRAP && accepted.checkpointId().equals(latestId))) {
            candidateId = latestId;
            Optional<ValidationRecord> existing = store.validationFor(latestId);
            boolean pending = existing.isEmpty() || (existing.get().assessment().decision() == PromotionPolicy.Decision.PROMOTE
                    && !bestId.equals(latestId));
            resolveCandidate(store, existing);
            if (pending) { recoveredLifecycles++; countDecision(); }
            publish(RECORDING_DECISION);
        }
        while (!stopRequested && (config.maximumGenerations() == 0 || completed < config.maximumGenerations())) {
            // Durable state is authoritative even without a process restart. RETAIN cannot select best here.
            NnueTrainer trainer = store.resume(latestId);
            CheckpointManifest parent = store.load(latestId).manifest();
            if (stopRequested) return;
            generation = Math.addExact(parent.generation(), 1);
            optimizerStep = trainer.optimizer().step();
            candidateId = ""; games = emptyGames(); training = Optional.empty();
            // Keep the previous decision readable during self-play/training; clear live game identity.
            validationProgress = validationProgress.map(ValidationProgress::withoutCurrentGame);
            updates = 0; samplesTrained = 0; meanLoss = Double.NaN;
            if (!generateTrainPublish(store, trainer, parent.id())) return;
            // Publication has begun/finished: even a stop resolves its cancellation evidence durably.
            resolveCandidate(store, Optional.empty());
            completed++;
            countDecision();
            phase(RECORDING_DECISION);
        }
    }

    /** Batch and frozen actor become unreachable before validation; no cross-generation replay buffer. */
    private boolean generateTrainPublish(CheckpointStore store, NnueTrainer trainer, String parent) throws IOException {
        phase(GENERATING_SELF_PLAY);
        if (stopRequested) return false;
        SelfPlayBatch batch = operations.generate(trainer.model().snapshot(), config.selfPlay(generation),
                config.startingBoard(), selfPlayControl, progress -> {
                    updateGames(progress.statistics());
                    publish(GENERATING_SELF_PLAY);
                    if (infrastructureFailure(progress.lastGame().termination())) {
                        throw new IllegalStateException("Self-play failed: " + progress.lastGame().failure());
                    }
                });
        updateGames(batch.statistics());
        publish(GENERATING_SELF_PLAY);
        for (var game : batch.games()) {
            if (infrastructureFailure(game.termination())) throw new IOException("Self-play failed: " + game.failure());
        }
        if (stopRequested || batch.cancelled()) return false;
        phase(TRAINING);
        if (stopRequested) return false;
        training = operations.train(trainer, batch, config.training(generation), selfPlayControl, progress -> {
            totalUpdates += progress.optimizerUpdates() - updates;
            updates = progress.optimizerUpdates(); samplesTrained = progress.samplesTrained();
            optimizerStep = progress.optimizerStep(); meanLoss = progress.meanTrainingLoss();
            publish(TRAINING);
        });
        optimizerStep = trainer.optimizer().step();
        publish(TRAINING);
        if (stopRequested || training.map(SelfPlayTraining.Statistics::cancelled).orElse(false)) return false;
        phase(PUBLISHING_CANDIDATE);
        // From this boundary onward, stop cannot interrupt the atomic filesystem protocol.
        var candidate = operations.publish(store, trainer,
                new CheckpointManifest.Metadata(generation, config.selfPlay().depth(), parent));
        candidateId = candidate.manifest().id(); latestId = candidateId;
        publish(PUBLISHING_CANDIDATE);
        return true;
    }

    private void resolveCandidate(CheckpointStore store, Optional<ValidationRecord> existing) throws IOException {
        CandidateLifecycle.Result resolved;
        if (existing.isPresent()) {
            validation = Optional.of(existing.get().statistics()); assessment = Optional.of(existing.get().assessment());
            var record = existing.get();
            validationDetails = Optional.of(new TrainerSnapshot.ValidationDetails(
                    record.candidateId(), record.incumbentId(), record.config(), record.policy()));
            phase(RECORDING_DECISION);
            resolved = operations.completeDecision(store, existing.get());
        } else {
            // Independently decoded, immutable persisted actors; neither mutable state nor a shared TT enters validation.
            var candidate = store.load(candidateId);
            var incumbent = store.load(bestId);
            var experiment = config.validation(generation);
            long[] board = config.startingBoard();
            GameHistory history = GameHistory.initial(board);
            validation = Optional.empty(); assessment = Optional.empty();
            validationDetails = Optional.of(new TrainerSnapshot.ValidationDetails(
                    candidateId, bestId, experiment, config.validation().policy(),
                    java.util.OptionalLong.of(candidate.manifest().generation()),
                    java.util.OptionalLong.of(incumbent.manifest().generation())));
            validationProgress = Optional.of(ValidationProgress.initial(experiment.openingPairs(), System.nanoTime()));
            phase(VALIDATING);
            ValidationResult result = operations.validate(candidate.network(), incumbent.network(), experiment,
                    board, history, validationControl, progress -> {
                        validationProgress = Optional.of(progress);
                        publish(VALIDATING);
                    });
            if (!result.config().equals(experiment) || !result.startingStateHash().equals(ValidationArena.stateHash(board, history))) {
                throw new IOException("Validation does not match the declared experiment.");
            }
            validation = Optional.of(result.statistics()); assessment = Optional.of(result.assess(config.validation().policy()));
            publish(VALIDATING);
            for (var reason : result.statistics().terminations().keySet()) {
                if (infrastructureFailure(reason) && result.statistics().terminations().get(reason) > 0) {
                    throw new IOException("Validation infrastructure failed: " + reason);
                }
            }
            phase(RECORDING_DECISION);
            resolved = operations.recordDecision(store, candidateId, bestId, result, config.validation().policy());
        }
        updateReferences(resolved.references());
        validation = Optional.of(resolved.validation().statistics());
        assessment = Optional.of(resolved.validation().assessment());
    }

    private static boolean infrastructureFailure(GameTermination reason) {
        return reason == GameTermination.SEARCH_FAILURE || reason == GameTermination.INFRASTRUCTURE_FAILURE;
    }
    private void countDecision() {
        switch (assessment.orElseThrow().decision()) {
            case PROMOTE -> promotions++;
            case RETAIN_INCUMBENT -> retains++;
            case INCONCLUSIVE -> { }
        }
        if (validation.orElseThrow().incompletePairs() > 0
                || assessment.orElseThrow().decision() == PromotionPolicy.Decision.INCONCLUSIVE) inconclusive++;
    }
    private void updateGames(SelfPlayBatch.Statistics next) {
        totalCompleted += next.completedGames() - games.completedGames();
        totalAborted += next.abortedGames() - games.abortedGames();
        totalGames += (long) next.completedGames() + next.abortedGames() - games.completedGames() - games.abortedGames();
        totalCapped += next.cappedGames() - games.cappedGames();
        totalSamples += next.sampledPositions() - games.sampledPositions();
        games = next;
    }
    private void updateReferences(CheckpointStore.Recovery refs) throws IOException {
        bestId = refs.best().orElseThrow(() -> new IOException("No valid best/bootstrap evidence.")).manifest().id();
        latestId = refs.latestTraining().orElseThrow(() -> new IOException("No durable latest-training state.")).manifest().id();
    }
    private SelfPlayBatch.Statistics emptyGames() {
        return new SelfPlayBatch.Statistics(config.selfPlay().games(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    private Duration elapsed() {
        long start = startedNanos;
        return start == 0 ? Duration.ZERO : Duration.ofNanos(Math.max(0, (endedNanos == 0 ? System.nanoTime() : endedNanos) - start));
    }
    private TrainerSnapshot view(TrainerSnapshot.State state) {
        return new TrainerSnapshot(state, failure == null ? "" : failure.toString(), elapsed(), generation,
                bestId, latestId, candidateId, optimizerStep, config.selfPlay().depth(), games, training, updates,
                samplesTrained, meanLoss, validation, assessment, new TrainerSnapshot.Totals(completed, totalGames,
                totalCompleted, totalAborted, totalCapped, totalSamples, totalUpdates, promotions, retains,
                inconclusive, recoveredLifecycles), validationProgress, validationDetails);
    }
    private void publish(TrainerSnapshot.State state) {
        synchronized (gate) { published = view(stopRequested ? STOPPING : state); }
    }
    private void phase(TrainerSnapshot.State state) {
        publish(state);
        observer.accept(published); // Package-private deterministic test hook; public callers only poll.
    }

    /** Bounded test seams. Public factories always compose the accepted E/F implementations. */
    static class Operations {
        CheckpointStore open(TrainerConfig config) throws IOException { return new CheckpointStore(config.checkpointRoot()); }
        SelfPlayBatch generate(NnueNetwork actor, SelfPlayConfig config, long[] board, SelfPlayControl control,
                Consumer<SelfPlayBatch.Progress> observer) {
            return SelfPlayBatch.generate(actor, config, board, control, observer);
        }
        Optional<SelfPlayTraining.Statistics> train(NnueTrainer trainer, SelfPlayBatch batch,
                SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
            return SelfPlayTraining.train(trainer, batch, config, control, observer);
        }
        CheckpointStore.Checkpoint publish(CheckpointStore store, NnueTrainer trainer, CheckpointManifest.Metadata metadata) throws IOException {
            return store.publish(trainer, metadata);
        }
        ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
                long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
            return new ValidationArena().validate(candidate, incumbent, config, board, history, control, observer);
        }
        CandidateLifecycle.Result recordDecision(CheckpointStore store, String candidate, String incumbent,
                ValidationResult result, PromotionPolicy policy) throws IOException {
            return CandidateLifecycle.recordDecision(store, candidate, incumbent, result, policy);
        }
        CandidateLifecycle.Result completeDecision(CheckpointStore store, ValidationRecord evidence) throws IOException {
            return CandidateLifecycle.completeDecision(store, evidence);
        }
    }
}
