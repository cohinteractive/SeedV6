package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkTrainingState;
import com.ohinteractive.seedv6.training.model.ModelLibrary;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import com.ohinteractive.seedv6.training.telemetry.OptimizationSnapshot;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static com.ohinteractive.seedv6.training.service.LearningArenaState.*;

/** Single-owner resumable campaign. No Best, validation decision, retention or promotion operation is called. */
public final class LearningArenaService implements AutoCloseable {
    public record Update(LearningArenaState state, String detail, ValidationProgress search, ActiveGameSnapshot liveGame, OptimizationSnapshot optimization) {
        public Update(LearningArenaState state, String detail, ValidationProgress search) { this(state, detail, search, null, null); }
        public Update withGame(ActiveGameSnapshot game) { return new Update(state, detail, search, game, optimization); }
    }
    private final Path root;
    private final FileChannel channel;
    private final FileLock lock;
    private final Consumer<Update> observer;
    private volatile LearningArenaState state;
    private volatile boolean paused;
    private volatile SelfPlayControl training;
    private volatile ValidationControl arena;
    private final ActiveGameFeed games = new ActiveGameFeed();
    private boolean running;
    private String trainingProgress = "";
    private OptimizationSnapshot optimization;
    // Tests can force every optimizer boundary to exercise byte-exact continuation cheaply.
    long snapshotNanos = 30_000_000_000L;
    Consumer<CheckpointStore.Checkpoint> checkpointPublished = checkpoint -> {};

    public static LearningArenaService create(Path root, LearningArenaConfig config, Consumer<Update> observer) throws IOException {
        root = root.toAbsolutePath().normalize();
        config.source().verify(); config.source().requireReady();
        Files.createDirectories(root.getParent()); Files.createDirectory(root);
        var initial = new LearningArenaState(1, UUID.randomUUID().toString(), config.identity(), config,
                List.of(Round.empty(0)), Status.READY, config.a().architecture().displayName() + " versus "
                        + config.b().architecture().displayName() + "; Round 0 precedes campaign training");
        return new LearningArenaService(root, initial, observer);
    }
    public static LearningArenaService resume(Path root, Consumer<Update> observer) throws IOException {
        if (!Files.isRegularFile(root.resolve("campaign.json"))) throw new IOException("Select a Learning Arena campaign folder containing campaign.json");
        return new LearningArenaService(root.toAbsolutePath().normalize(), null, observer);
    }
    private LearningArenaService(Path root, LearningArenaState initial, Consumer<Update> observer) throws IOException {
        this.root = root; this.observer = Objects.requireNonNull(observer);
        if (!Files.isDirectory(root)) throw new IOException("Select an existing Learning Arena campaign folder");
        channel = FileChannel.open(root.resolve("campaign.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = channel.tryLock();
            if (acquired == null) throw new IOException("Learning Arena campaign is already open");
        } catch (IOException | OverlappingFileLockException failure) { channel.close(); throw new IOException("Cannot own Learning Arena campaign", failure); }
        lock = acquired;
        try {
            if (initial == null) state = LearningArenaState.read(root);
            else { state = initial; save(initial); }
        } catch (IOException | RuntimeException failure) { close(); throw failure; }
    }
    public LearningArenaState state() { return state; }
    public Path root() { return root; }
    public ActiveGameSnapshot liveGame() { return games.latest(); }
    public void pause() {
        paused = true;
        games.close();
        var t = training; if (t != null) t.cancel();
        var a = arena; if (a != null) a.cancel();
    }
    /** Run on one background owner thread; callers join it before close. */
    public void run() throws IOException {
        synchronized (this) {
            if (running || !lock.isValid()) throw new IllegalStateException("Campaign owner unavailable");
            running = true;
        }
        try {
            if (state.status() == Status.COMPLETE) { report("Campaign complete", null); return; }
            save(state.status(Status.RUNNING, "Resume uses durable tranches, optimizer boundaries and game receipts; uncommitted work may be recomputed"));
            var config = state.config();
            try (var a = new CheckpointStore(root.resolve("A"), config.a().architecture());
                 var b = new CheckpointStore(root.resolve("B"), config.b().architecture())) {
                verifyEndpoints(a, b);
                while (!paused) {
                    var round = state.current();
                    switch (round.stage()) {
                        case INITIALIZE_A -> endpoint(true, a, null);
                        case INITIALIZE_B -> endpoint(false, b, null);
                        case SELECT_TRANCHE -> {
                            report("Selecting shared tranche once", null);
                            var tranche = tranche(); save(state.withRound(round.tranche(tranche.manifest)));
                        }
                        case TRAIN_A -> endpoint(true, a, tranche());
                        case TRAIN_B -> endpoint(false, b, tranche());
                        case ARENA -> match(a, b);
                        case ROUND_COMPLETE -> {
                            if (config.rounds() > 0 && round.number() >= config.rounds()) {
                                save(state.status(Status.COMPLETE, "Campaign complete")); return;
                            }
                            save(state.next());
                        }
                    }
                }
            }
            save(state.status(Status.PAUSED, "Paused safely; Resume continues durable progress. " + trainingProgress));
        } catch (IOException | RuntimeException failure) {
            boolean cancellation = paused && failure instanceof InterruptedIOException;
            try { save(state.status(cancellation ? Status.PAUSED : Status.FAILED, failure.toString())); }
            catch (IOException | RuntimeException saveFailure) { failure.addSuppressed(saveFailure); cancellation = false; }
            if (!cancellation) {
                if (failure instanceof UncheckedIOException io) throw io.getCause();
                if (failure instanceof IOException io) throw io;
                throw failure;
            }
        } finally { training = null; arena = null; games.clear(); synchronized (this) { running = false; } }
    }
    private void verifyEndpoints(CheckpointStore a, CheckpointStore b) throws IOException {
        // Historical manifest identities are checked without loading every old model on a long campaign.
        // The actual training/match endpoints receive CheckpointStore's full payload verification on load.
        for (var r : state.history()) {
            if (r.a() != null) verify(a, r.a());
            if (r.b() != null) verify(b, r.b());
        }
    }
    private static void verify(CheckpointStore store, Endpoint endpoint) throws IOException {
        var manifest = CheckpointInspection.manifest(store.root().resolve("checkpoints").resolve(endpoint.checkpoint()));
        if (manifest.generation() != endpoint.generation()) throw new IOException("Campaign checkpoint generation mismatch");
    }
    private Path roundDirectory() { return root.resolve("rounds").resolve(String.format(Locale.ROOT, "%06d", state.current().number())); }
    private LearningArenaTranche tranche() throws IOException {
        var r = state.current();
        long start = state.history().get(r.number() - 1).sourceEnd();
        LearningArenaTranche tranche;
        if (r.trancheHash().isEmpty()) tranche = LearningArenaTranche.obtain(roundDirectory(), root.resolve("seek"), state.config(), start, () -> paused);
        else tranche = LearningArenaTranche.read(roundDirectory().resolve("tranche"), state.config(), start);
        if (!r.trancheHash().isEmpty() && (!r.trancheHash().equals(tranche.manifest.hash()) || r.sourceEnd() != tranche.manifest.end()))
            throw new IOException("Campaign/shared-tranche receipt mismatch");
        return tranche;
    }
    private void endpoint(boolean first, CheckpointStore store, LearningArenaTranche tranche) throws IOException {
        var config = state.config(); var round = state.current();
        var competitor = first ? config.a() : config.b();
        Endpoint prior = round.number() == 0 ? null : first ? state.history().get(round.number() - 1).a() : state.history().get(round.number() - 1).b();
        Path progress = roundDirectory().resolve(first ? "A" : "B");
        trainingProgress = "";
        optimization = null;
        NetworkTrainingState model;
        report((prior == null ? "Initializing " : "Training ") + competitor.name(), null);
        if (prior == null) model = LearningArenaTraining.initial(competitor);
        else {
            var control = new SelfPlayControl(); training = control;
            if (paused) control.cancel();
            String binding = DataFiles.hash(state.binding() + ":" + state.id() + ":" + round.number() + ":" + first + ":" + prior.checkpoint() + ":" + round.trancheHash());
            model = LearningArenaTraining.train(progress, binding, store, prior.checkpoint(), competitor,
                    tranche.examples(competitor.architecture(), config.source().labelProfile()),
                    SelfPlayTraining.Config.fromBatchSeed(config.epochs(), competitor.minibatch(), SelfPlayRunner.gameSeed(config.seed(), round.number())),
                    control, (p, t) -> {
                        optimization = new OptimizationSnapshot(competitor.name() + " - " + competitor.architecture().displayName(),
                                "Round " + round.number() + " - training from Gen " + prior.generation(), config.epochs(), competitor.minibatch(), t.learningRate(),
                                p.samplesTrained(), (long) config.positionsPerRound() * config.epochs(), p.optimizerUpdates(), p.initialOptimizerStep(), p.optimizerStep(),
                                p.meanTrainingLoss(), t.elapsedNanos(), "This training segment", Math.addExact(prior.exposure(), p.samplesTrained()), t.invocationSamples());
                        trainingProgress = competitor.name() + " consumed " + p.samplesTrained() + " / "
                                + ((long) config.positionsPerRound() * config.epochs()) + " records this round; total exposure "
                                + Math.addExact(prior.exposure(), p.samplesTrained()) + " (including epochs)";
                        report(trainingProgress, null);
                    }, snapshotNanos);
            training = null;
            if (control.trainingCursor().samples() != (long) config.positionsPerRound() * config.epochs()) return;
        }
        long generation = prior == null ? 0 : Math.addExact(prior.generation(), 1);
        var checkpoint = store.publish(model, new CheckpointManifest.Metadata(generation, config.arena().matches(config.seed()).depth(), prior == null ? "" : prior.checkpoint()));
        if (prior == null) {
            var id = UUID.nameUUIDFromBytes((state.id() + (first ? ":A" : ":B")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (TrainingLineage.read(store.root()).isEmpty()) {
                // Legacy campaign names were unrestricted. Keep their full name in campaign.json.
                String displayName = competitor.name().replaceAll("\\p{Cntrl}", " ").strip();
                if (displayName.isEmpty()) displayName = first ? "Competitor A" : "Competitor B";
                if (displayName.length() > 120) displayName = displayName.substring(0, 117) + "...";
                store.writeLineage(new TrainingLineage(id, displayName, competitor.architecture(), java.time.Instant.now(), "",
                        "Arena campaign " + config.name() + "; recipe and source binding are preserved in campaign.json"));
            }
            var source = competitor.initialModel();
            if (LineageProvenance.read(store.root()).isEmpty())
                store.writeProvenance(new LineageProvenance(checkpoint.manifest().id(), competitor.architecture(),
                        source == null ? competitor.architecture().schemaId() + " seeded initializer" : "Exact optimizer/model copy; source binding in campaign.json",
                        source == null ? competitor.seed() : null, config.seed(), java.time.Instant.now()));
        }
        checkpointPublished.accept(checkpoint); // Failure-injection boundary: payload visible, campaign receipt not yet committed.
        long positions = Math.multiplyExact((long) round.number(), config.positionsPerRound());
        var receipt = new Endpoint(checkpoint.manifest().id(), checkpoint.manifest().generation(), positions, Math.multiplyExact(positions, config.epochs()));
        save(state.withRound(round.endpoint(first, receipt)));
        LearningArenaTraining.clear(progress);
    }
    private void match(CheckpointStore a, CheckpointStore b) throws IOException {
        var config = state.config(); var round = state.current();
        games.arena(round.number(), participant(true, round.a()), participant(false, round.b()));
        var control = new ValidationControl(games); arena = control;
        // Resume explicitly retries administrative failures/in-flight games, retaining every settled game.
        control.savedPairs(round.pairs().stream().map(p -> new ValidationResult.Pair(p.openingHash(), retry(p.candidateWhite()), retry(p.candidateBlack()))).toList());
        if (paused) control.cancel();
        long[] board = Board.fromFen(config.arena().startingFen());
        var result = new ValidationArena().match(a.load(round.a().checkpoint()).model(), b.load(round.b().checkpoint()).model(),
                config.arena().matches(config.seed()), board, GameHistory.initial(board), control, config.arena().timeLimit(),
                p -> report("Arena game " + p.gameOrdinal() + " / " + config.arena().games(), p),
                pairs -> {
                    try { save(state.withRound(state.current().games(pairs, false))); }
                    catch (IOException failure) { throw new UncheckedIOException(failure); }
                });
        arena = null;
        games.clear();
        boolean complete = LearningArenaState.settled(result.pairs());
        save(state.withRound(state.current().games(result.pairs(), complete)));
        if (!complete && !paused) throw new IOException("Arena search/infrastructure failure; completed games saved. Resume retries unfinished games. A time limit must allow one completed search iteration.");
    }
    private static ValidationResult.Game retry(ValidationResult.Game game) {
        return LearningArenaState.settled(game) ? game : new ValidationResult.Game(GameTermination.CANCELLED, 0);
    }
    private ModelLibrary.Binding participant(boolean first, Endpoint endpoint) throws IOException {
        Path store = root.resolve(first ? "A" : "B");
        var competitor = first ? state.config().a() : state.config().b();
        return new ModelLibrary.Binding(store, TrainingLineage.read(store).map(TrainingLineage::id),
                competitor.name(), competitor.architecture(), endpoint.checkpoint(), endpoint.generation());
    }
    private void save(LearningArenaState value) throws IOException {
        DataFiles.write(root.resolve("campaign.json"), value); state = value;
        report(value.current().stage().toString(), null);
    }
    private void report(String detail, ValidationProgress search) { observer.accept(new Update(state, detail, search, games.latest(), optimization)); }
    @Override public synchronized void close() throws IOException {
        if (running) throw new IllegalStateException("Join the campaign worker before closing");
        games.close();
        try { if (lock.isValid()) lock.release(); } finally { channel.close(); }
    }
}
