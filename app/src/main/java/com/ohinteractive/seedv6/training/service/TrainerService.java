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
 * optimizer boundaries. Safe stop saves completed games, optimizer state/cursor, and validation games.
 * Once PUBLISHING_CANDIDATE begins, publication finishes; a cancelled game match remains partial.
 * Bounded held-out passes and RECORDING_DECISION are non-interruptible. No thread interrupt
 * is used to tear down file-channel writes. Filesystem stalls cannot have a universal deadline:
 * awaitTermination(timeout) reports this honestly, and close() waits at most 30 seconds then throws.
 * STOPPED/FAILED is published only after the store lock and synchronous search resources are released.
 */
public final class TrainerService implements AutoCloseable {
    private static final AtomicLong WORKER_IDS = new AtomicLong();
    private volatile TrainerConfig config;
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
    private java.util.concurrent.ScheduledExecutorService deadline;
    private volatile boolean timeLimitReached;
    private long firstRunGeneration, targetGeneration, priorActiveNanos, trainingSampleTarget;
    private String runAction = "Start";
    private PartialGeneration continuation;
    private NetworkTrainingState activeTrainer;
    private boolean generationFinalized = true;
    private boolean recoveryOnly;
    private boolean generationSettingsKnown = true;
    private Optional<TrainerSnapshot.LossProgress> lossProgress = Optional.empty();
    private Optional<TrainerSnapshot.RunDetails> runDetails = Optional.empty();
    private long lastTrainingPublication;
    private byte[] initialState;
    private final HistoryRepository history;
    private volatile String historyWarning = "";
    private volatile String lifecycleNotice = "";
    private Instant generationStarted;
    private long generationNanos, selfPlayNanos, trainingNanos, validationNanos;
    private Long settledGenerationNanos;
    private TrainingSource source, storedSource;
    private BrnSupervision supervision = BrnSupervision.WDL;
    private FrozenReplay frozenReplay;
    private long frozenThroughGeneration;
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
    /** Explicit frozen workflow: restores its pinned controls and uses the ordinary checkpoint lifecycle. */
    public static TrainerService frozenReplay(java.nio.file.Path root, FrozenReplay replay, long throughGeneration) throws IOException {
        return frozenReplay(root, replay, throughGeneration, new Operations(), snapshot -> {});
    }
    static TrainerService frozenReplay(java.nio.file.Path root, FrozenReplay replay, long throughGeneration,
                                      Operations operations, Consumer<TrainerSnapshot> observer) throws IOException {
        if (throughGeneration < 1 || throughGeneration > replay.entries().size())
            throw new IOException("Requested endpoint is outside the frozen corpus.");
        replay.verify(root); // Read-only source and destination separation checks before ANY destination writer.
        boolean fresh = CheckpointInspection.freshRoot(root, TrainingArchitecture.BRN2);
        var stored = FrozenReplay.read(root);
        if ((!fresh && stored.isEmpty()) || stored.isPresent() && !stored.get().equals(replay))
            throw new IOException("Select a fresh replay store or its exact existing frozen lineage.");
        var service = new TrainerService(replay.config(root), fresh ? replay.initialState() : null, operations, observer);
        service.frozenReplay = replay; service.frozenThroughGeneration = throughGeneration;
        return service;
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
            try {
                if (config.maximumRunMillis() > 0) {
                    deadline = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        var thread = new Thread(r, "seedv6-training-time-limit"); thread.setDaemon(true); return thread;
                    });
                    deadline.schedule(() -> {
                        synchronized (gate) {
                            if (published.running() && !stopRequested) { timeLimitReached = true; stop(); }
                        }
                    }, config.maximumRunMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                }
                worker.start();
            }
            catch (RuntimeException | Error unable) {
                if (deadline != null) deadline.shutdownNow();
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
                    source = newBrn ? config.architecture() == TrainingArchitecture.BRN2 ? TrainingSource.HANDCRAFTED
                            : new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, "") : storedSource;
                }
                if (source.bootstrap() && config.architecture() == TrainingArchitecture.NNUE)
                    throw new IOException("NNUE cannot be a bootstrap student.");
                if (config.validationMethod() == null) {
                    var previous = GenerationAttempt.inspect(config.checkpointRoot());
                    config = config.withValidationMethod(previous.map(GenerationAttempt::validationMethod)
                            .orElse(ValidationMethod.legacy(source)));
                }
                resolveSourceIdentity();
                resolveRunSeeds();
                resolveFrozenIdentity();
                resolveSupervision();
                // An invalid external source must not create a new student lineage.
                if (initialState != null && source.nnue()) source.loadBest(config.checkpointRoot());
                try (CheckpointStore store = operations.open(config)) {
                    // Opening may finish an interrupted, already-authorized restart transaction.
                    storedSource = CheckpointStore.readTrainingSource(config.checkpointRoot()).orElse(TrainingSource.SELF_PLAY);
                    if (initialState == null && config.source() == null) source = storedSource;
                    resolveSourceIdentity();
                    resolveRunSeeds();
                    resolveFrozenIdentity();
                    resolveSupervision(); // Recheck under exclusive ownership before reconciliation.
                    execute(store);
                    saveStoppedGeneration(store);
                }
            }
        } catch (Throwable unexpected) {
            failure = unexpected;
            selfPlayControl.cancel(); validationControl.cancel();
        } finally {
            if (deadline != null) deadline.shutdownNow();
            activeGame.close();
            initialState = null;
            synchronized (gate) {
                endedNanos = System.nanoTime();
                published = view(failure == null ? STOPPED : FAILED);
            }
        }
    }

    private void resolveSourceIdentity() throws IOException {
        if ((source.mode() == TrainingSource.Mode.HANDCRAFTED || source.frozen()) && config.architecture() != TrainingArchitecture.BRN2)
            throw new IOException("Handcrafted generation or frozen replay requires BRN-2.");
    }

    private void resolveFrozenIdentity() throws IOException {
        var stored = FrozenReplay.read(config.checkpointRoot());
        if (source.frozen() || frozenReplay != null || stored.isPresent() || !config.frozenReplayHash().isEmpty()) {
            if (!source.frozen() || frozenReplay == null)
                throw new IOException("Use the explicit frozen-wdl replay workflow; live generation is forbidden for this lineage.");
            frozenReplay.requireConfig(config);
            if (stored.isPresent() ? !stored.get().equals(frozenReplay) : initialState == null)
                throw new IOException("Missing or changed frozen corpus identity; existing work was preserved.");
            frozenReplay.verify(config.checkpointRoot());
            if (java.nio.file.Files.exists(config.checkpointRoot().resolve(CheckpointStore.BRN_TEACHER_FILE)))
                throw new IOException("Frozen WDL replay cannot have a teacher dependency.");
        }
    }

    private void resolveRunSeeds() throws IOException {
        if (config.architecture() != TrainingArchitecture.BRN2) return;
        var stored = CheckpointStore.readBrnRunSeeds(config.checkpointRoot());
        if (config.runSeeds() != null && (initialState == null || stored.isPresent()))
            CheckpointStore.requireSameRunSeeds(stored.orElse(null), config.runSeeds());
        if (stored.isPresent()) config = config.withRunSeeds(stored.get());
        else if (initialState != null && config.runSeeds() == null)
            config = config.withRunSeeds(new BrnRunSeeds(config.masterSeed(), config.masterSeed()));
    }

    private void resolveSupervision() throws IOException {
        if (config.architecture() != TrainingArchitecture.BRN2) return;
        var stored = CheckpointStore.readBrnSupervision(config.checkpointRoot());
        supervision = stored.orElse(BrnSupervision.WDL);
        if (config.supervision() != null) {
            supervision = config.supervision();
        }
        supervision.requireSupported(config.architecture(), source);
        config = config.withSupervision(supervision);
        if (config.captureConsistency() == null)
            config = config.withCaptureConsistency(CheckpointStore.readBrnCaptureConsistency(config.checkpointRoot()));
        config.effectiveCaptureConsistency().requireSupported(config.architecture(), supervision, source);
        if (supervision.blended()) {
            var storedTeacher = CheckpointStore.readBrnTeacherStore(config.checkpointRoot());
            String requested = config.teacherStore();
            String selected = requested != null ? requested : storedTeacher.orElse(source.nnue() ? source.generatorStore() : "");
            if (selected == null || selected.isBlank()) throw new IOException("Select an NNUE Teacher Store for blended supervision.");
            config = config.withTeacherStore(selected);
            if (initialState != null) TrainingSource.bootstrap(java.nio.file.Path.of(selected)).loadBest(config.checkpointRoot());
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
            if (frozenReplay != null) store.initializeFrozenReplay(frozenReplay);
            if (config.runSeeds() != null) store.initializeBrnRunSeeds(config.runSeeds());
            if (config.architecture() == TrainingArchitecture.BRN2) store.initializeBrnSupervision(supervision);
            if (supervision.blended()) store.initializeBrnTeacherStore(config.teacherStore());
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
            var priorPlan = store.bootstrapPlan(attempt.parentId());
            boolean sameObjective = priorPlan.map(p -> p.supervision().equals(supervision)
                    && (!supervision.blended() || p.teacherStore().equals(config.teacherStore()))).orElse(attempt.format() == GenerationAttempt.Format.INDEPENDENT || !supervision.blended());
            if (!attempt.matches(config, source) || !sameObjective) {
                // Validate a replacement external source before superseding any resumable work.
                if (source.nnue()) source.loadBest(config.checkpointRoot());
                if (supervision.blended()) TrainingSource.bootstrap(java.nio.file.Path.of(config.teacherStore())).loadBest(config.checkpointRoot());
                var replacement = attempt.restarted(config, source);
                store.restartGeneration(attempt, replacement, unfinished.candidate());
                lifecycleNotice = replacement.notice(); runAction = "Restart Generation";
                refs = store.recoverTrainingReferences(); updateReferences(refs);
                latest = refs.latestTraining().orElseThrow().manifest();
                generation = latest.generation(); optimizerStep = latest.optimizerStep();
            } else {
                runAction = "Resume";
                lifecycleNotice = "Resuming unfinished generation " + attempt.generation() + " from durable progress.";
                continuation = PartialGeneration.inspect(store.root()).filter(p -> p.attempt().equals(attempt)).orElse(null);
                if (continuation != null && (!bestId.equals(attempt.incumbentId()) || !(continuation.candidate().isEmpty()
                        ? latestId.equals(attempt.parentId()) : latestId.equals(continuation.candidate()))))
                    throw new IOException("Partial continuation is disconnected from the durable lineage.");
            }
        }
        firstRunGeneration = continuation != null && !continuation.recoveryOnly() && !continuation.candidate().isEmpty()
                ? continuation.attempt().generation() : Math.addExact(latest.generation(), 1);
        targetGeneration = frozenReplay == null ? config.finalGeneration(firstRunGeneration - 1) : frozenThroughGeneration;
        if (frozenReplay != null && latest.generation() > frozenThroughGeneration)
            throw new IOException("Frozen replay endpoint precedes the existing lineage.");
        PromotionRecord accepted = refs.bestEvidence().orElseThrow(() -> new IOException("Missing best/bootstrap evidence."));
        if (!(accepted.kind() == PromotionRecord.Kind.BOOTSTRAP && accepted.checkpointId().equals(latestId))) {
            candidateId = latestId;
            Optional<ValidationRecord> existing = store.validationFor(latestId);
            boolean pending = existing.isEmpty() || (existing.get().decision() == PromotionPolicy.Decision.PROMOTE
                    && !bestId.equals(latestId));
            boolean resumedCandidate = continuation != null && continuation.candidate().equals(candidateId);
            if (resumedCandidate) restoreContinuation(continuation);
            else if (existing.isEmpty()) {
                // Legacy/crash reconciliation keeps its existing separate run-count semantics, but a
                // subsequent safe stop must still preserve games newly completed during recovery.
                generationStarted = Instant.now(); generationNanos = System.nanoTime(); priorActiveNanos = 0;
                recoveryOnly = true; generationFinalized = false;
                generationSettingsKnown = unfinished == null || unfinished.attempt().format() != GenerationAttempt.Format.LEGACY_SELF_PLAY;
                if (unfinished != null && (store.generationAttempt().isEmpty()
                        || unfinished.attempt().format() == GenerationAttempt.Format.LEGACY_BOOTSTRAP
                        || unfinished.attempt().format() == GenerationAttempt.Format.LEGACY_SELF_PLAY))
                    store.writeGenerationAttempt(GenerationAttempt.create(unfinished.attempt().parentId(), bestId, generation, config, source));
            }
            if (!resolveCandidate(store, existing)) return;
            if (resumedCandidate && !recoveryOnly) {
                recordHistory(); completed++; countDecision(); generationFinalized = true; continuation = null;
            } else if (pending) { recoveredLifecycles++; countDecision(); generationFinalized = true; continuation = null; }
            publish(RECORDING_DECISION);
        }
        store.writeCampaignObjective(supervision, config.teacherStore());
        store.writeBrnCaptureConsistency(config.effectiveCaptureConsistency());
        if (config.architecture() != TrainingArchitecture.NNUE) store.writeTrainingSource(source);
        storedSource = source;
        while (!stopRequested && (config.maximumGenerations() == 0 || completed < config.maximumGenerations())) {
            if (frozenReplay != null && store.load(latestId).manifest().generation() >= frozenThroughGeneration) break;
            Instant invocationStarted = Instant.now(); long invocationNanos = System.nanoTime();
            // Durable state is authoritative even without a process restart. RETAIN cannot select best here.
            NetworkTrainingState trainer = continuation == null ? store.resumeState(latestId) : store.resumePartialState(continuation);
            activeTrainer = trainer;
            CheckpointManifest parent = store.load(latestId).manifest();
            if (stopRequested) return;
            generationStarted = invocationStarted; generationNanos = invocationNanos; priorActiveNanos = 0;
            settledGenerationNanos = null;
            recoveryOnly = false;
            generationSettingsKnown = true;
            generation = Math.addExact(parent.generation(), 1);
            optimizerStep = trainer.step();
            candidateId = ""; games = emptyGames(); training = Optional.empty();
            // Keep the previous decision readable during self-play/training; clear live game identity.
            validationProgress = validationProgress.map(ValidationProgress::withoutCurrentGame);
            updates = 0; samplesTrained = 0; meanLoss = Double.NaN;
            selfPlayNanos = 0; trainingNanos = 0; validationNanos = 0;
            selfPlayControl.savedGames(SelfPlayBatch.Saved.EMPTY);
            selfPlayControl.trainingCursor(SelfPlayControl.TrainingCursor.EMPTY);
            validationControl.savedPairs(java.util.List.of()); lossProgress = Optional.empty(); trainingSampleTarget = 0;
            if (continuation != null) { restoreContinuation(continuation); generationNanos = invocationNanos; continuation = null; }
            var attempt = store.generationAttempt();
            if (attempt.isEmpty() || !attempt.get().parentId().equals(parent.id()))
                store.writeGenerationAttempt(GenerationAttempt.create(parent.id(), bestId, generation, config, source));
            generationFinalized = false;
            if (!generateTrainPublish(store, trainer, parent.id())) return;
            activeTrainer = null;
            selfPlayControl.savedGames(SelfPlayBatch.Saved.EMPTY); // Published Candidate owns the model; release training data before validation.
            // Publication is complete; validation either settles or persists a resumable game match.
            if (!resolveCandidate(store, Optional.empty())) return;
            recordHistory();
            generationFinalized = true; activeTrainer = null;
            completed++;
            countDecision();
            phase(RECORDING_DECISION);
        }
    }

    private void restoreContinuation(PartialGeneration p) {
        generationStarted = p.started(); generationNanos = System.nanoTime(); priorActiveNanos = p.activeNanos();
        settledGenerationNanos = null;
        recoveryOnly = p.recoveryOnly();
        generationSettingsKnown = p.generationSettingsKnown();
        generationFinalized = false;
        selfPlayNanos = p.selfPlayNanos(); trainingNanos = p.trainingNanos(); validationNanos = p.validationNanos();
        selfPlayControl.savedGames(p.games()); selfPlayControl.trainingCursor(p.training()); validationControl.savedPairs(p.pairs());
        games = p.candidate().isEmpty() ? SelfPlayBatch.statistics(p.games(), config.selfPlay().games()) : p.statistics();
        updates = p.training().updates(); samplesTrained = p.training().samples();
        if (!p.candidate().isEmpty()) trainingSampleTarget = samplesTrained;
        meanLoss = samplesTrained == 0 ? Double.NaN : p.training().lossSum() / samplesTrained;
        if (p.training().initialStep() >= 0) optimizerStep = p.training().initialStep() + updates;
    }

    private void saveStoppedGeneration(CheckpointStore store) throws IOException {
        if (!stopRequested || generationFinalized || generationStarted == null) return;
        var attempt = store.generationAttempt().orElseThrow();
        long duration = generationDuration(System.nanoTime());
        store.savePartial(new PartialGeneration(attempt, candidateId, selfPlayControl.savedGames(), games,
                selfPlayControl.trainingCursor(), validationControl.savedPairs(), generationStarted,
                duration, selfPlayNanos, trainingNanos, validationNanos, recoveryOnly,
                generationSettingsKnown), activeTrainer);
        settledGenerationNanos = duration;
        lifecycleNotice = (runAction.equals("Restart Generation") ? lifecycleNotice + " " : "")
                + "Saved partial generation " + generation + "; Resume continues completed games and optimizer batches."
                + (timeLimitReached ? " Time limit reached." : "");
    }

    /** Batch and frozen actor become unreachable before validation; no cross-generation replay buffer. */
    private boolean generateTrainPublish(CheckpointStore store, NetworkTrainingState trainer, String parent) throws IOException {
        if (config.heldOut(source) || supervision.blended() || source.frozen()) return generateBootstrap(store, trainer, parent);
        BootstrapPlan plan = source.bootstrap() ? preparePlan(store, parent) : null;
        NetworkModel actor = source.nnue() ? plan.loadGenerator(config.checkpointRoot()).model() : trainer.snapshot();
        bootstrapValidation = Optional.empty();
        activeGame.selfPlay(generation, source.nnue() ? plan.generatorId() : source.bootstrap() ? "HANDCRAFTED" : parent);
        phase(GENERATING_SELF_PLAY);
        if (stopRequested) return false;
        long phaseStart = System.nanoTime();
        Consumer<SelfPlayBatch.Progress> generated = progress -> {
            updateGames(progress.statistics()); publish(GENERATING_SELF_PLAY);
            if (infrastructureFailure(progress.lastGame().termination()))
                throw new IllegalStateException("Self-play failed: " + progress.lastGame().failure());
        };
        SelfPlayBatch batch = source.mode() == TrainingSource.Mode.HANDCRAFTED
                ? operations.generateHandcrafted(config.selfPlay(generation), config.startingBoard(), selfPlayControl, generated)
                : operations.generate(actor, config.selfPlay(generation), config.startingBoard(), selfPlayControl, generated);
        selfPlayNanos += System.nanoTime() - phaseStart;
        updateGames(batch.statistics());
        publish(GENERATING_SELF_PLAY);
        for (var game : batch.games()) {
            if (infrastructureFailure(game.termination())) throw new IOException("Self-play failed: " + game.failure());
        }
        if (stopRequested || batch.cancelled()) return false;
        trainingSampleTarget = (long) batch.samples().size() * config.training().epochs();
        phase(TRAINING);
        if (stopRequested) return false;
        phaseStart = System.nanoTime();
        training = operations.train(trainer, batch, config.training(generation), selfPlayControl, progress -> {
            totalUpdates += progress.optimizerUpdates() - updates;
            updates = progress.optimizerUpdates(); samplesTrained = progress.samplesTrained();
            optimizerStep = progress.optimizerStep(); meanLoss = progress.meanTrainingLoss();
            publishTrainingProgress();
        });
        trainingNanos += System.nanoTime() - phaseStart;
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

    private BootstrapPlan preparePlan(CheckpointStore store, String parent) throws IOException {
        var plan = store.bootstrapPlan(parent).orElse(null);
        if (plan == null) {
            var generator = source.nnue() ? source.loadBest(config.checkpointRoot()) : null;
            var teacher = supervision.blended() ? (generator != null && config.teacherStore().equals(source.generatorStore())
                    ? generator : TrainingSource.bootstrap(java.nio.file.Path.of(config.teacherStore())).loadBest(config.checkpointRoot())) : null;
            plan = BootstrapPlan.create(parent, bestId, generation, config.withSupervision(supervision), source, generator,
                    supervision.blended() ? config.teacherStore() : "", teacher);
            store.writeBootstrapPlan(plan);
        }
        CheckpointStore.requireSameSupervision(supervision, plan.supervision()); plan.requireSettings(config, source);
        return plan;
    }

    private boolean generateBootstrap(CheckpointStore store, NetworkTrainingState trainer, String parent) throws IOException {
        BootstrapPlan plan = preparePlan(store, parent);
        NetworkModel generator = null;
        BootstrapData data = store.bootstrapData(plan).orElse(null);
        if (!source.frozen()) activeGame.selfPlay(generation, source.nnue() ? plan.generatorId() : source.bootstrap() ? "HANDCRAFTED" : parent);
        phase(GENERATING_SELF_PLAY);
        if (stopRequested) return false;
        if (source.frozen()) {
            // No generator, sampling, repartitioning, outcome reconstruction or teacher call is reachable here.
            var origin = frozenReplay.data(generation);
            if (data == null) {
                data = new BootstrapData(plan.hash(), origin.partition(), origin.statistics(), 0);
                store.writeBootstrapData(plan, data);
            }
            frozenReplay.requireData(generation, data);
            updateGames(data.statistics());
        } else if (data == null) {
            if (source.nnue() && generator == null) generator = plan.loadGenerator(config.checkpointRoot()).model();
            long start = System.nanoTime();
            Consumer<SelfPlayBatch.Progress> progress = value -> { updateGames(value.statistics()); publish(GENERATING_SELF_PLAY); };
            SelfPlayBatch batch = source.nnue()
                    ? operations.generate(generator, config.selfPlay(generation), config.startingBoard(), selfPlayControl, progress)
                    : source.bootstrap() ? operations.generateHandcrafted(config.selfPlay(generation), config.startingBoard(), selfPlayControl, progress)
                    : operations.generate(trainer.snapshot(), config.selfPlay(generation), config.startingBoard(), selfPlayControl, progress);
            selfPlayNanos += System.nanoTime() - start;
            updateGames(batch.statistics());
            for (var game : batch.games()) if (infrastructureFailure(game.termination())) throw new IOException("Self-play failed: " + game.failure());
            if (stopRequested || batch.cancelled()) return false;
            if (batch.games().stream().filter(game -> game.sampledPositions() > 0).count() < 4) {
                store.archiveInsufficientBootstrap(plan);
                throw new IOException("Held-out or blended training needs at least four completed games with samples. The insufficient attempt's pin was archived before any updates; increase games or maximum game plies, then Resume to start a new attempt.");
            }
            var partition = BootstrapPartition.split(batch, plan.splitSeed());
            data = new BootstrapData(plan.hash(), partition, batch.statistics(), selfPlayNanos);
            store.writeBootstrapData(plan, data);
        } else updateGames(data.statistics());
        bootstrapValidation = Optional.empty(); validation = Optional.empty(); assessment = Optional.empty();
        validationDetails = Optional.empty(); validationProgress = Optional.empty();
        trainingSampleTarget = (long) data.partition().training().size() * config.training().epochs();
        phase(TRAINING);
        if (stopRequested) return false;
        var teacher = supervision.blended()
                ? new com.ohinteractive.seedv6.core.nnue.NnueEvaluator(
                        plan.loadTeacher(config.checkpointRoot()).model().nnue()) : null;
        var target = supervision.targets(teacher);
        long start = System.nanoTime();
        training = operations.trainBootstrap(trainer, data.partition().training(), config.training(generation), selfPlayControl, progress -> {
            totalUpdates += progress.optimizerUpdates() - updates;
            updates = progress.optimizerUpdates(); samplesTrained = progress.samplesTrained();
            optimizerStep = progress.optimizerStep(); meanLoss = progress.meanTrainingLoss(); publishTrainingProgress();
        }, supervision, target, config.effectiveCaptureConsistency(), teacher,
                config.effectiveCaptureConsistency().enabled() ? config.seed(generation, TrainerConfig.SeedDomain.CAPTURE) : 0);
        trainingNanos += System.nanoTime() - start; optimizerStep = trainer.step();
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

    private boolean resolveCandidate(CheckpointStore store, Optional<ValidationRecord> existing) throws IOException {
        var candidateManifest = CheckpointInspection.manifest(store.root().resolve("checkpoints").resolve(candidateId));
        var bootstrapPlan = store.bootstrapPlan(candidateManifest.parentId());
        if (existing.map(record -> record.bootstrap() != null).orElseGet(() -> bootstrapPlan
                .map(plan -> plan.validationMethod() == ValidationMethod.HELD_OUT).orElse(false))) {
            resolveBootstrap(store, existing, bootstrapPlan.orElse(null));
            return true;
        }
        if (existing.isEmpty() && (source.bootstrap() || config.heldOut(source)) && bootstrapPlan.isEmpty())
            throw new IOException("Missing durable generation/holdout plan; fallback is forbidden.");
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
            // Saved valid pairs are replayed through telemetry by the arena. They are not new live wins.
            int restoredWins = 0, restoredLosses = 0;
            for (var pair : validationControl.savedPairs()) if (pair.valid()) {
                double white = pair.candidateWhite().score(com.ohinteractive.seedv6.core.util.Value.WHITE);
                double black = pair.candidateBlack().score(com.ohinteractive.seedv6.core.util.Value.BLACK);
                restoredWins += (white == 1 ? 1 : 0) + (black == 1 ? 1 : 0);
                restoredLosses += (white == 0 ? 1 : 0) + (black == 0 ? 1 : 0);
            }
            validationDetails = Optional.of(new TrainerSnapshot.ValidationDetails(
                    candidateId, bestId, experiment, config.validation().policy(),
                    java.util.OptionalLong.of(candidate.manifest().generation()),
                    java.util.OptionalLong.of(incumbent.manifest().generation()), restoredWins, restoredLosses));
            validationProgress = Optional.of(ValidationProgress.initial(experiment.openingPairs(), System.nanoTime()));
            activeGame.validation(generation, candidateId, bestId);
            phase(VALIDATING);
            long validationStart = System.nanoTime();
            ValidationResult result = operations.validate(candidate.model(), incumbent.model(), experiment,
                    board, history, validationControl, progress -> {
                        validationProgress = Optional.of(progress);
                        publish(VALIDATING);
                    });
            validationNanos += System.nanoTime() - validationStart;
            if (!result.config().equals(experiment) || !result.startingStateHash().equals(ValidationArena.stateHash(board, history))) {
                throw new IOException("Validation does not match the declared experiment.");
            }
            validation = Optional.of(result.statistics());
            for (var reason : result.statistics().terminations().keySet()) {
                if (infrastructureFailure(reason) && result.statistics().terminations().get(reason) > 0) {
                    throw new IOException("Validation infrastructure failed: " + reason);
                }
            }
            if (stopRequested && result.statistics().terminations().getOrDefault(GameTermination.CANCELLED, 0) > 0) {
                validationControl.savedPairs(result.pairs()); assessment = Optional.empty(); publish(VALIDATING); return false;
            }
            assessment = Optional.of(result.assess(config.validation().policy()));
            publish(VALIDATING);
            phase(RECORDING_DECISION);
            resolved = operations.recordDecision(store, candidateId, bestId, result, config.validation().policy());
        }
        updateReferences(resolved.references());
        validation = Optional.of(resolved.validation().statistics());
        assessment = Optional.of(resolved.validation().assessment());
        return true;
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
            if (frozenReplay != null) frozenReplay.requireData(plan.generation(), data);
            var candidate = store.load(candidateId); var best = store.load(plan.incumbentId());
            phase(VALIDATING);
            // This bounded prediction pass completes even after Stop at the publication boundary.
            long start = System.nanoTime();
            CheckpointStore.requireSameSupervision(supervision, plan.supervision());
            var teacher = plan.supervision().blended() ? new com.ohinteractive.seedv6.core.nnue.NnueEvaluator(
                    plan.loadTeacher(config.checkpointRoot()).model().nnue()) : null;
            var samples = data.partition().heldOut();
            int sampleCount = samples.size(); long work = (teacher == null ? 1L : 3L) * sampleCount;
            lossProgress = Optional.of(new TrainerSnapshot.LossProgress(0, work, sampleCount)); publish(VALIDATING);
            operations.lossObserver = done -> lossProgress(done, work, sampleCount);
            var comparison = operations.validateBootstrap(candidate.model(), best.model(), samples,
                    plan.supervision(), plan.supervision().targets(teacher));
            // Descriptive components never enter the decision. Both actors use the same samples.
            var wdl = teacher == null ? null : HeldOutLoss.compare(candidate.model(), best.model(), samples, TrajectorySampler.Sample::target,
                    done -> lossProgress(sampleCount + (long) done, work, sampleCount));
            var teacherLoss = teacher == null ? null : HeldOutLoss.compare(candidate.model(), best.model(), samples,
                    sample -> BrnSupervision.teacherValue(teacher, sample),
                    done -> lossProgress(2L * sampleCount + done, work, sampleCount));
            validationNanos += System.nanoTime() - start;
            lossProgress(work, work, sampleCount);
            var evidence = BootstrapEvidence.create(plan, data, comparison, wdl, teacherLoss);
            record = store.recordBootstrapValidation(candidateId, evidence);
        }
        bootstrapValidation = Optional.of(new TrainerSnapshot.BootstrapValidation(record.candidateId(), record.incumbentId(), record.bootstrap()));
        phase(RECORDING_DECISION);
        var resolved = operations.completeDecision(store, record);
        updateReferences(resolved.references());
    }

    private void lossProgress(long done, long total, int samples) {
        lossProgress = Optional.of(new TrainerSnapshot.LossProgress(done, total, samples)); publish(VALIDATING);
    }

    /** Only newly measured, settled generations enter analytics. Recovery never invents prior run settings/times. */
    private void recordHistory() {
        Instant ended = Instant.now(); long duration = generationDuration(System.nanoTime());
        settledGenerationNanos = duration;
        try {
            if (bootstrapValidation.isPresent()) {
                var detail = bootstrapValidation.get(); var evidence = detail.evidence();
                var record = GenerationRecord.bootstrap(generation, candidateId, detail.incumbentId(), bestId,
                        historyRegime(0),
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
                    historyRegime(config.validation().openingPairs()),
                    games.completedGames(), games.abortedGames(), (long) games.sampledPositions(),
                    training.map(SelfPlayTraining.Statistics::finalLoss).filter(Double::isFinite).orElse(null),
                    generationStarted, ended, selfPlayNanos, trainingNanos, validationNanos, duration, null,
                    validationDetails.orElseThrow().policy().rawScoreThreshold(v.validPairs()).stream().boxed().findFirst().orElse(null));
            operations.appendHistory(history, record);
            if (!history.refresh().warnings().isEmpty()) historyWarning = String.join("\n", history.refresh().warnings());
        } catch (IOException | RuntimeException problem) {
            historyWarning = "History NOT confirmed persisted for generation " + generation + " / " + candidateId
                    + " at " + history.file() + ": " + problem;
            System.err.println(historyWarning);
        }
    }

    private GenerationRecord.Regime historyRegime(int pairs) {
        return new GenerationRecord.Regime(config.selfPlay().depth(), config.selfPlay().games(), pairs,
                config.selfPlay().threads(), source.mode().name(), config.validationMethod(source).name(),
                config.historySettings(source));
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
    private long generationDuration(long now) { return priorActiveNanos + Math.max(0, now - generationNanos); }
    private TrainerSnapshot view(TrainerSnapshot.State state) {
        TrainerSnapshot.GenerationTiming timing = generationStarted == null || recoveryOnly ? null
                : settledGenerationNanos != null || endedNanos != 0
                ? new TrainerSnapshot.GenerationTiming(generation, settledGenerationNanos != null ? settledGenerationNanos
                        : generationDuration(endedNanos), 0, false)
                : new TrainerSnapshot.GenerationTiming(generation, priorActiveNanos, generationNanos, true);
        var previous = runDetails.orElse(null);
        if (firstRunGeneration != 0 && (previous == null || previous.effective() != config
                || previous.trainingSampleTarget() != trainingSampleTarget || previous.timeLimitReached() != timeLimitReached
                || previous.generationSettingsKnown() != generationSettingsKnown
                || !Objects.equals(previous.generationTiming(), timing)))
            runDetails = Optional.of(new TrainerSnapshot.RunDetails(config, source, supervision, firstRunGeneration,
                    targetGeneration, runAction, timeLimitReached, trainingSampleTarget, generationSettingsKnown, timing));
        return new TrainerSnapshot(state, failure == null ? "" : failure.toString(), elapsed(), generation,
                bestId, latestId, candidateId, optimizerStep, config.selfPlay().depth(), games, training, updates,
                samplesTrained, meanLoss, validation, assessment, new TrainerSnapshot.Totals(completed, totalGames,
                totalCompleted, totalAborted, totalCapped, totalSamples, totalUpdates, promotions, retains,
                inconclusive, recoveredLifecycles), validationProgress, validationDetails, Optional.empty(), bootstrapValidation, runDetails, lossProgress);
    }
    private void publishTrainingProgress() {
        long now = System.nanoTime();
        if (now - lastTrainingPublication >= 50_000_000L) { lastTrainingPublication = now; publish(TRAINING); }
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
        java.util.function.IntConsumer lossObserver = done -> {};
        SelfPlayBatch generateHandcrafted(SelfPlayConfig config, long[] board, SelfPlayControl control,
                Consumer<SelfPlayBatch.Progress> observer) {
            return SelfPlayBatch.generateHandcrafted(config, board, control, observer);
        }
        Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, java.util.List<TrajectorySampler.Sample> samples,
                SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer,
                BrnSupervision supervision, java.util.function.ToDoubleFunction<TrajectorySampler.Sample> target,
                BrnCaptureConsistency capture, com.ohinteractive.seedv6.core.nnue.NnueEvaluator teacher, long captureSeed) {
            if (!capture.enabled()) return trainBootstrap(state, samples, config, control, observer, supervision, target);
            if (!(state instanceof NetworkTrainingState.Brn2)) throw new IllegalArgumentException("Capture consistency requires BRN-2.");
            capture.requireSupported(TrainingArchitecture.BRN2, supervision, null);
            var brn = (NetworkTrainingState.Brn2) state;
            java.util.Objects.requireNonNull(teacher, "Pinned NNUE teacher");
            return com.ohinteractive.seedv6.training.selfplay.BrnCaptureTraining.trainSamples(brn.trainer(), samples,
                    config, control, observer, target, capture, board -> {
                        teacher.evaluate(board); return teacher.boundedValue();
                    }, captureSeed);
        }
        Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, java.util.List<TrajectorySampler.Sample> samples,
                SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer,
                BrnSupervision supervision, java.util.function.ToDoubleFunction<TrajectorySampler.Sample> target) {
            if (!supervision.blended()) return trainBootstrap(state, samples, config, control, observer);
            if (!(state instanceof NetworkTrainingState.Brn2 b)) throw new IllegalArgumentException("Blending requires BRN-2.");
            return Brn2SelfPlayTraining.trainSamples(b.trainer(), samples, config, control, observer, target);
        }
        HeldOutLoss.Comparison validateBootstrap(NetworkModel candidate, NetworkModel incumbent,
                java.util.List<TrajectorySampler.Sample> samples, BrnSupervision supervision,
                java.util.function.ToDoubleFunction<TrajectorySampler.Sample> target) {
            return supervision.blended() ? HeldOutLoss.compare(candidate, incumbent, samples, target, lossObserver)
                    : validateBootstrap(candidate, incumbent, samples);
        }
        Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, java.util.List<TrajectorySampler.Sample> samples,
                SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
            return switch (state) {
                case NetworkTrainingState.Brn b -> BrnSelfPlayTraining.trainSamples(b.trainer(), samples, config, control, observer);
                case NetworkTrainingState.Brn1 b -> Brn1SelfPlayTraining.trainSamples(b.trainer(), samples, config, control, observer);
                case NetworkTrainingState.Brn2 b -> Brn2SelfPlayTraining.trainSamples(b.trainer(), samples, config, control, observer);
                case NetworkTrainingState.Nnue n -> SelfPlayTraining.trainSamples(n.trainer(), samples, config, control, observer);
            };
        }
        HeldOutLoss.Comparison validateBootstrap(NetworkModel candidate, NetworkModel incumbent, java.util.List<TrajectorySampler.Sample> samples) {
            return HeldOutLoss.compare(candidate, incumbent, samples, TrajectorySampler.Sample::target, lossObserver);
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
