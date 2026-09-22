package com.ohinteractive.seedv6.training.service;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import com.ohinteractive.seedv6.core.brn.BrnTrainer;
import com.ohinteractive.seedv6.training.model.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.training.history.*;
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
    private final com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed activeGame =
            new com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed();
    private final SelfPlayControl selfPlayControl = new SelfPlayControl(activeGame);
    private final ValidationControl validationControl = new ValidationControl(activeGame);
    private volatile TrainerSnapshot published;
    private volatile Thread worker;
    private volatile boolean stopRequested;
    private volatile Throwable failure;
    private volatile long startedNanos, endedNanos;
    private byte[] initialState;
    private final HistoryRepository history;
    private volatile String historyWarning = "";
    private volatile String lifecycleNotice = "";
    private Instant generationStarted;
    private long generationNanos, selfPlayNanos, trainingNanos, validationNanos;
    private TrainingSource source, storedSource;
    private Optional<TrainerSnapshot.BootstrapValidation> bootstrapValidation = Optional.empty();

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
        return fresh(config, new NetworkTrainingState.Nnue(initial), operations, observer);
    }
    public static TrainerService fresh(TrainerConfig config, com.ohinteractive.seedv6.core.brn1.Brn1Trainer initial) throws IOException {
        return fresh(config, new NetworkTrainingState.Brn1(initial), new Operations(), snapshot -> {});
    }

    public static TrainerService fresh(TrainerConfig config, com.ohinteractive.seedv6.core.brn2.Brn2Trainer initial) throws IOException {
        return fresh(config, new NetworkTrainingState.Brn2(initial), new Operations(), snapshot -> {});
    }

    public static TrainerService fresh(TrainerConfig config, BrnTrainer initial) throws IOException {
        return fresh(config, new NetworkTrainingState.Brn(initial), new Operations(), snapshot -> {});
    }
    static TrainerService fresh(TrainerConfig config, NetworkTrainingState initial, Operations operations,
                                Consumer<TrainerSnapshot> observer) throws IOException {
        if (config.architecture() != initial.architecture()) throw new IOException("Initial model/config architecture mismatch.");
        return new TrainerService(config, initial.encode(), operations, observer);
    }
    static TrainerService resume(TrainerConfig config, Operations operations, Consumer<TrainerSnapshot> observer) {
        return new TrainerService(config, null, operations, observer);
    }
    private TrainerService(TrainerConfig config, byte[] initial, Operations operations, Consumer<TrainerSnapshot> observer) {
        this.config = Objects.requireNonNull(config);
        this.operations = Objects.requireNonNull(operations);
        this.observer = Objects.requireNonNull(observer);
        history = new HistoryRepository(config.checkpointRoot());
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
        return current.withState(current.state(), current.failureSummary(), elapsed()).withActiveGame(activeGame.latest());
    }
    public Optional<Throwable> failure() { return Optional.ofNullable(failure); }
    public String historyWarning() { return historyWarning; }
    public String lifecycleNotice() { return lifecycleNotice; }
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
                var storedSelection = CheckpointStore.readTrainingSource(config.checkpointRoot());
                storedSource = storedSelection.orElse(TrainingSource.SELF_PLAY);
                source = config.source();
                if (source == null) {
                    boolean newBrn = initialState != null && config.architecture() != TrainingArchitecture.NNUE && storedSelection.isEmpty();
                    source = newBrn ? new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, "") : storedSource;
                }
                if (source.bootstrap() && config.architecture() == TrainingArchitecture.NNUE)
                    throw new IOException("NNUE cannot be a bootstrap student.");
                // An invalid external source must not create a new student lineage.
                if (initialState != null && source.bootstrap()) source.loadBest(config.checkpointRoot());
                try (CheckpointStore store = operations.open(config)) {
                    // Opening may finish an interrupted, already-authorized restart transaction.
                    storedSource = CheckpointStore.readTrainingSource(config.checkpointRoot()).orElse(TrainingSource.SELF_PLAY);
                    if (initialState == null && config.source() == null) source = storedSource;
                    execute(store);
                }
            }
        } catch (Throwable unexpected) {
            failure = unexpected;
            selfPlayControl.cancel(); validationControl.cancel();
        } finally {
            activeGame.close();
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
            NetworkTrainingState initial = NetworkTrainingState.read(config.architecture(), new ByteArrayInputStream(initialState));
            initialState = null;
            if (config.architecture() != TrainingArchitecture.NNUE) store.writeTrainingSource(source);
            store.initialize(initial, new CheckpointManifest.Metadata(0, config.selfPlay().depth(), ""));
            refs = store.recover();
        } else refs = store.recoverTrainingReferences();
        updateReferences(refs);
        CheckpointManifest latest = refs.latestTraining().orElseThrow().manifest();
        generation = latest.generation(); optimizerStep = latest.optimizerStep();
        var unfinished = unfinished(store, refs);
        if (latest.trainingDepth() != config.selfPlay().depth()
                && config.depthChange() != TrainerConfig.DepthChange.EXPLICITLY_ALLOW && unfinished == null) {
            throw new IOException("Resume training depth differs; explicitly allow a depth change in the run configuration.");
        }
        phase(RECOVERING);
        if (stopRequested) return;
        if (unfinished != null) {
            var attempt = unfinished.attempt();
            if (!attempt.matches(config, source)) {
                // Validate a replacement external source before superseding any resumable work.
                if (source.bootstrap()) source.loadBest(config.checkpointRoot());
                var replacement = attempt.restarted(config, source);
                store.restartGeneration(attempt, replacement, unfinished.candidate());
                lifecycleNotice = replacement.notice();
                refs = store.recoverTrainingReferences(); updateReferences(refs);
                latest = refs.latestTraining().orElseThrow().manifest();
                generation = latest.generation(); optimizerStep = latest.optimizerStep();
            } else lifecycleNotice = attempt.notice();
        }
        PromotionRecord accepted = refs.bestEvidence().orElseThrow(() -> new IOException("Missing best/bootstrap evidence."));
        if (!(accepted.kind() == PromotionRecord.Kind.BOOTSTRAP && accepted.checkpointId().equals(latestId))) {
            candidateId = latestId;
            Optional<ValidationRecord> existing = store.validationFor(latestId);
            boolean pending = existing.isEmpty() || (existing.get().decision() == PromotionPolicy.Decision.PROMOTE
                    && !bestId.equals(latestId));
            resolveCandidate(store, existing);
            if (pending) { recoveredLifecycles++; countDecision(); }
            publish(RECORDING_DECISION);
        }
        if (config.architecture() != TrainingArchitecture.NNUE) store.writeTrainingSource(source);
        storedSource = source;
        while (!stopRequested && (config.maximumGenerations() == 0 || completed < config.maximumGenerations())) {
            generationStarted = Instant.now(); generationNanos = System.nanoTime();
            // Durable state is authoritative even without a process restart. RETAIN cannot select best here.
            NetworkTrainingState trainer = store.resumeState(latestId);
            CheckpointManifest parent = store.load(latestId).manifest();
            if (stopRequested) return;
            generation = Math.addExact(parent.generation(), 1);
            optimizerStep = trainer.step();
            candidateId = ""; games = emptyGames(); training = Optional.empty();
            // Keep the previous decision readable during self-play/training; clear live game identity.
            validationProgress = validationProgress.map(ValidationProgress::withoutCurrentGame);
            updates = 0; samplesTrained = 0; meanLoss = Double.NaN;
            selfPlayNanos = 0; trainingNanos = 0; validationNanos = 0;
            var attempt = store.generationAttempt();
            if (attempt.isEmpty() || !attempt.get().parentId().equals(parent.id()))
                store.writeGenerationAttempt(GenerationAttempt.create(parent.id(), bestId, generation, config, source));
            if (!generateTrainPublish(store, trainer, parent.id())) return;
            // Publication has begun/finished: even a stop resolves its cancellation evidence durably.
            resolveCandidate(store, Optional.empty());
            recordHistory();
            completed++;
            countDecision();
            phase(RECORDING_DECISION);
        }
    }

    /** Batch and frozen actor become unreachable before validation; no cross-generation replay buffer. */
    private boolean generateTrainPublish(CheckpointStore store, NetworkTrainingState trainer, String parent) throws IOException {
        if (source.bootstrap()) return generateBootstrap(store, trainer, parent);
        bootstrapValidation = Optional.empty();
        activeGame.selfPlay(generation, parent);
        phase(GENERATING_SELF_PLAY);
        if (stopRequested) return false;
        long phaseStart = System.nanoTime();
        SelfPlayBatch batch = operations.generate(trainer.snapshot(), config.selfPlay(generation),
                config.startingBoard(), selfPlayControl, progress -> {
                    updateGames(progress.statistics());
                    publish(GENERATING_SELF_PLAY);
                    if (infrastructureFailure(progress.lastGame().termination())) {
                        throw new IllegalStateException("Self-play failed: " + progress.lastGame().failure());
                    }
                });
        selfPlayNanos = System.nanoTime() - phaseStart;
        updateGames(batch.statistics());
        publish(GENERATING_SELF_PLAY);
        for (var game : batch.games()) {
            if (infrastructureFailure(game.termination())) throw new IOException("Self-play failed: " + game.failure());
        }
        if (stopRequested || batch.cancelled()) return false;
        phase(TRAINING);
        if (stopRequested) return false;
        phaseStart = System.nanoTime();
        training = operations.train(trainer, batch, config.training(generation), selfPlayControl, progress -> {
            totalUpdates += progress.optimizerUpdates() - updates;
            updates = progress.optimizerUpdates(); samplesTrained = progress.samplesTrained();
            optimizerStep = progress.optimizerStep(); meanLoss = progress.meanTrainingLoss();
            publish(TRAINING);
        });
        trainingNanos = System.nanoTime() - phaseStart;
        optimizerStep = trainer.step();
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

    private boolean generateBootstrap(CheckpointStore store, NetworkTrainingState trainer, String parent) throws IOException {
        BootstrapPlan plan = store.bootstrapPlan(parent).orElse(null);
        NetworkModel generator = null;
        if (plan == null) {
            var pinned = source.loadBest(config.checkpointRoot());
            plan = BootstrapPlan.create(parent, bestId, generation, config, source, pinned);
            store.writeBootstrapPlan(plan);
            generator = pinned.model();
        }
        plan.requireSettings(config, source);
        BootstrapData data = store.bootstrapData(plan).orElse(null);
        activeGame.selfPlay(generation, plan.generatorId());
        phase(GENERATING_SELF_PLAY);
        if (stopRequested) return false;
        if (data == null) {
            if (generator == null) generator = plan.loadGenerator(config.checkpointRoot()).model();
            long start = System.nanoTime();
            // Only this pinned NNUE actor is passed into the existing search/self-play stack.
            SelfPlayBatch batch = operations.generate(generator, config.selfPlay(generation), config.startingBoard(),
                    selfPlayControl, progress -> { updateGames(progress.statistics()); publish(GENERATING_SELF_PLAY); });
            selfPlayNanos = System.nanoTime() - start;
            updateGames(batch.statistics());
            for (var game : batch.games()) if (infrastructureFailure(game.termination())) throw new IOException("Self-play failed: " + game.failure());
            if (stopRequested || batch.cancelled()) return false;
            if (batch.games().stream().filter(game -> game.sampledPositions() > 0).count() < 4) {
                store.archiveInsufficientBootstrap(plan);
                throw new IOException("Bootstrap needs at least four completed games with samples. The insufficient attempt's pin was archived before any updates; increase games or maximum game plies, then Resume to start a new attempt.");
            }
            var partition = BootstrapPartition.split(batch, plan.splitSeed());
            data = new BootstrapData(plan.hash(), partition, batch.statistics(), selfPlayNanos);
            store.writeBootstrapData(plan, data);
        } else updateGames(data.statistics());
        bootstrapValidation = Optional.empty(); validation = Optional.empty(); assessment = Optional.empty();
        validationDetails = Optional.empty(); validationProgress = Optional.empty();
        phase(TRAINING);
        if (stopRequested) return false;
        long start = System.nanoTime();
        training = operations.trainBootstrap(trainer, data.partition().training(), config.training(generation), selfPlayControl, progress -> {
            totalUpdates += progress.optimizerUpdates() - updates;
            updates = progress.optimizerUpdates(); samplesTrained = progress.samplesTrained();
            optimizerStep = progress.optimizerStep(); meanLoss = progress.meanTrainingLoss(); publish(TRAINING);
        });
        trainingNanos = System.nanoTime() - start; optimizerStep = trainer.step();
        if (stopRequested || training.map(SelfPlayTraining.Statistics::cancelled).orElse(false)) return false;
        phase(PUBLISHING_CANDIDATE);
        var candidate = operations.publish(store, trainer, new CheckpointManifest.Metadata(generation, config.selfPlay().depth(), parent));
        candidateId = candidate.manifest().id(); latestId = candidateId; publish(PUBLISHING_CANDIDATE);
        return true;
    }

    private record Unfinished(GenerationAttempt attempt, String candidate) {}
    private Unfinished unfinished(CheckpointStore store, CheckpointStore.Recovery refs) throws IOException {
        var latest = refs.latestTraining().orElseThrow().manifest();
        var accepted = refs.bestEvidence().orElseThrow();
        boolean initial = accepted.kind() == PromotionRecord.Kind.BOOTSTRAP && accepted.checkpointId().equals(latest.id());
        boolean pending = !initial && store.validationFor(latest.id()).isEmpty();
        var active = store.generationAttempt();
        if (active.isPresent()) {
            var attempt = active.get();
            if (latest.id().equals(attempt.parentId())) return new Unfinished(attempt, "");
            if (latest.parentId().equals(attempt.parentId()) && latest.generation() == attempt.generation())
                return pending ? new Unfinished(attempt, latest.id()) : null;
            // A public checkpoint writer may have published a later Candidate without an attempt
            // record. Ignore the old record only after proving its own generation was settled.
            var completedAttempt = CheckpointInspection.lineage(store.root(), latest.id()).stream()
                    .filter(m -> m.parentId().equals(attempt.parentId()) && m.generation() == attempt.generation()).findFirst();
            if (completedAttempt.isEmpty() || store.validationFor(completedAttempt.get().id()).isEmpty())
                throw new IOException("Generation attempt is disconnected from latest-training; existing work was preserved.");
        }
        // Compatibility: old bootstrap stores already carry the complete generation pin.
        var plan = store.bootstrapPlan(pending ? latest.parentId() : latest.id());
        GenerationAttempt legacy = plan.map(GenerationAttempt::legacy).orElse(null);
        // Older ordinary stores did not persist generation settings. Compare only what is known.
        if (legacy == null && pending) legacy = new GenerationAttempt(latest.parentId(), bestId, latest.generation(),
                storedSource, Integer.toString(latest.trainingDepth()), GenerationAttempt.Format.LEGACY_SELF_PLAY, "");
        if (legacy != null) {
            if (active.isPresent()) store.writeGenerationAttempt(legacy); // Supersede proven settled bookkeeping only.
            return new Unfinished(legacy, pending ? latest.id() : "");
        }
        return null;
    }

    private void resolveCandidate(CheckpointStore store, Optional<ValidationRecord> existing) throws IOException {
        var candidateManifest = CheckpointInspection.manifest(store.root().resolve("checkpoints").resolve(candidateId));
        var bootstrapPlan = store.bootstrapPlan(candidateManifest.parentId());
        if (existing.map(record -> record.bootstrap() != null).orElse(false) || bootstrapPlan.isPresent()) {
            resolveBootstrap(store, existing, bootstrapPlan.orElse(null));
            return;
        }
        if (existing.isEmpty() && storedSource.bootstrap()) throw new IOException("Missing bootstrap plan; BRN search fallback is forbidden.");
        bootstrapValidation = Optional.empty();
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
            activeGame.validation(generation, candidateId, bestId);
            phase(VALIDATING);
            long validationStart = System.nanoTime();
            ValidationResult result = operations.validate(candidate.model(), incumbent.model(), experiment,
                    board, history, validationControl, progress -> {
                        validationProgress = Optional.of(progress);
                        publish(VALIDATING);
                    });
            validationNanos = System.nanoTime() - validationStart;
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

    private void resolveBootstrap(CheckpointStore store, Optional<ValidationRecord> existing, BootstrapPlan plan) throws IOException {
        validation = Optional.empty(); assessment = Optional.empty(); validationProgress = Optional.empty(); validationDetails = Optional.empty();
        ValidationRecord record;
        if (existing.isPresent()) {
            record = existing.get();
            if (record.bootstrap() == null) throw new IOException("Conflicting validation mode for bootstrap Candidate.");
        } else {
            if (plan == null) throw new IOException("Missing bootstrap plan.");
            var data = store.bootstrapData(plan).orElseThrow(() -> new IOException("Missing durable bootstrap holdout."));
            var candidate = store.load(candidateId); var best = store.load(plan.incumbentId());
            phase(VALIDATING);
            // This bounded prediction pass completes even after Stop at the publication boundary.
            long start = System.nanoTime();
            var comparison = operations.validateBootstrap(candidate.model(), best.model(), data.partition().heldOut());
            validationNanos = System.nanoTime() - start;
            var evidence = BootstrapEvidence.create(plan, data, comparison);
            record = store.recordBootstrapValidation(candidateId, evidence);
        }
        bootstrapValidation = Optional.of(new TrainerSnapshot.BootstrapValidation(record.candidateId(), record.incumbentId(), record.bootstrap()));
        phase(RECORDING_DECISION);
        var resolved = operations.completeDecision(store, record);
        updateReferences(resolved.references());
    }

    /** Only newly measured, settled generations enter analytics. Recovery never invents prior run settings/times. */
    private void recordHistory() {
        Instant ended = Instant.now(); long duration = System.nanoTime() - generationNanos;
        try {
            if (bootstrapValidation.isPresent()) {
                var detail = bootstrapValidation.get(); var evidence = detail.evidence();
                var record = GenerationRecord.bootstrap(generation, candidateId, detail.incumbentId(), bestId,
                        new GenerationRecord.Regime(config.selfPlay().depth(), config.selfPlay().games(), 0, config.selfPlay().threads()),
                        games.completedGames(), games.abortedGames(), (long) games.sampledPositions(),
                        training.map(SelfPlayTraining.Statistics::finalLoss).filter(Double::isFinite).orElse(null),
                        generationStarted, ended, selfPlayNanos, trainingNanos, validationNanos, duration, evidence);
                operations.appendHistory(history, record);
                return;
            }
            var v = validation.orElseThrow(); var a = assessment.orElseThrow();
            boolean promoted = candidateId.equals(bestId);
            var outcome = promoted ? GenerationRecord.Outcome.PROMOTED
                    : v.terminations().getOrDefault(GameTermination.CANCELLED, 0) > 0 ? GenerationRecord.Outcome.CANCELLED_VALIDATION
                    : a.decision() == PromotionPolicy.Decision.INCONCLUSIVE ? GenerationRecord.Outcome.INCONCLUSIVE
                    : GenerationRecord.Outcome.RETAINED;
            var record = new GenerationRecord(generation, candidateId, validationDetails.orElseThrow().bestId(), bestId,
                    outcome, a.decision(), v.wins(), v.draws(), v.losses(), v.validPairs(), v.incompletePairs(),
                    v.validPairs() == 0 ? null : a.mean(), v.validPairs() == 0 ? null : a.lowerBound(), a.threshold(),
                    new GenerationRecord.Regime(config.selfPlay().depth(), config.selfPlay().games(), config.validation().openingPairs(), config.selfPlay().threads()),
                    games.completedGames(), games.abortedGames(), (long) games.sampledPositions(),
                    training.map(SelfPlayTraining.Statistics::finalLoss).filter(Double::isFinite).orElse(null),
                    generationStarted, ended, selfPlayNanos, trainingNanos, validationNanos, duration);
            operations.appendHistory(history, record);
            if (!history.refresh().warnings().isEmpty()) historyWarning = String.join("\n", history.refresh().warnings());
        } catch (IOException | RuntimeException problem) {
            historyWarning = "History NOT confirmed persisted for generation " + generation + " / " + candidateId
                    + " at " + history.file() + ": " + problem;
            System.err.println(historyWarning);
        }
    }

    private static boolean infrastructureFailure(GameTermination reason) {
        return reason == GameTermination.SEARCH_FAILURE || reason == GameTermination.INFRASTRUCTURE_FAILURE;
    }
    private void countDecision() {
        if (bootstrapValidation.isPresent()) {
            if (bootstrapValidation.get().evidence().comparison().decision() == PromotionPolicy.Decision.PROMOTE) promotions++;
            else retains++;
            return;
        }
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
                inconclusive, recoveredLifecycles), validationProgress, validationDetails).withBootstrapValidation(bootstrapValidation);
    }
    private void publish(TrainerSnapshot.State state) {
        synchronized (gate) { published = view(stopRequested ? STOPPING : state); }
    }
    private void phase(TrainerSnapshot.State state) {
        activeGame.clear();
        publish(state);
        observer.accept(published); // Package-private deterministic test hook; public callers only poll.
    }

    /** Bounded test seams. Public factories always compose the accepted E/F implementations. */
    static class Operations {
        Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, java.util.List<TrajectorySampler.Sample> samples,
                SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
            return switch (state) {
                case NetworkTrainingState.Brn b -> BrnSelfPlayTraining.trainSamples(b.trainer(), samples, config, control, observer);
                case NetworkTrainingState.Brn1 b -> Brn1SelfPlayTraining.trainSamples(b.trainer(), samples, config, control, observer);
                case NetworkTrainingState.Brn2 b -> Brn2SelfPlayTraining.trainSamples(b.trainer(), samples, config, control, observer);
                default -> throw new IllegalArgumentException("Bootstrap requires a BRN student.");
            };
        }
        HeldOutLoss.Comparison validateBootstrap(NetworkModel candidate, NetworkModel incumbent, java.util.List<TrajectorySampler.Sample> samples) {
            return HeldOutLoss.compare(candidate, incumbent, samples);
        }
        void appendHistory(HistoryRepository repository, GenerationRecord record) throws IOException { repository.append(record); }
        CheckpointStore open(TrainerConfig config) throws IOException {
            CheckpointInspection.freshRoot(config.checkpointRoot(), config.architecture());
            return new CheckpointStore(config.checkpointRoot(), config.architecture());
        }
        SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig config, long[] board, SelfPlayControl control,
                Consumer<SelfPlayBatch.Progress> observer) {
            if (actor instanceof NetworkModel.Nnue nnue) return generate(nnue.network(), config, board, control, observer);
            return SelfPlayBatch.generate(actor, config, board, control, observer);
        }
        Optional<SelfPlayTraining.Statistics> train(NetworkTrainingState state, SelfPlayBatch batch,
                SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
            if (state instanceof NetworkTrainingState.Nnue nnue) return train(nnue.trainer(), batch, config, control, observer);
            if (state instanceof NetworkTrainingState.Brn1 b)
                return Brn1SelfPlayTraining.train(b.trainer(), batch, config, control, observer);
            if (state instanceof NetworkTrainingState.Brn2 b)
                return Brn2SelfPlayTraining.train(b.trainer(), batch, config, control, observer);
            return BrnSelfPlayTraining.train(((NetworkTrainingState.Brn) state).trainer(), batch, config, control, observer);
        }
        CheckpointStore.Checkpoint publish(CheckpointStore store, NetworkTrainingState state, CheckpointManifest.Metadata metadata) throws IOException {
            if (state instanceof NetworkTrainingState.Nnue nnue) return publish(store, nnue.trainer(), metadata);
            return store.publish(state, metadata);
        }
        ValidationResult validate(NetworkModel candidate, NetworkModel incumbent, ValidationConfig config,
                long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
            if (candidate.architecture() != incumbent.architecture()) throw new IllegalArgumentException("Architecture mismatch.");
            if (candidate instanceof NetworkModel.Nnue nnue) return validate(nnue.network(), incumbent.nnue(), config, board, history, control, observer);
            return new ValidationArena().validate(candidate, incumbent, config, board, history, control, observer);
        }
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
