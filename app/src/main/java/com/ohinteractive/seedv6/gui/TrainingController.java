package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import com.ohinteractive.seedv6.training.checkpoint.*;
import java.util.Optional;
import java.time.Duration;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayBatch;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.nnue.TrainableNnue;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;

/** EDT-owned UI state; the I/O executor prepares startup, and TrainerService owns all learning. */
final class TrainingController {
    enum Phase { IDLE, STARTING, CONFIRM_DEPTH, RUNNING, STOPPING, STOPPED, FAILED, CLOSING }
    record ViewState(TrainingSettings settings, Phase phase, TrainerSnapshot snapshot, String message,
                     boolean active, boolean canStart, boolean resume, int previousDepth, String bootstrapId,
                     HistoryRepository.Snapshot history, String historyWarning, String startAction,
                     TrainingLineages.Selection lineage, boolean loading, long scheduledStopGeneration) {
        ViewState(TrainingSettings settings, Phase phase, TrainerSnapshot snapshot, String message,
                  boolean active, boolean canStart, boolean resume, int previousDepth, String bootstrapId,
                  HistoryRepository.Snapshot history, String historyWarning, String startAction) {
            this(settings, phase, snapshot, message, active, canStart, resume, previousDepth, bootstrapId,
                    history, historyWarning, startAction, null, false, 0);
        }
        ViewState(TrainingSettings settings, Phase phase, TrainerSnapshot snapshot, String message,
                  boolean active, boolean canStart, boolean resume, int previousDepth, String bootstrapId,
                  HistoryRepository.Snapshot history, String historyWarning) {
            this(settings, phase, snapshot, message, active, canStart, resume, previousDepth, bootstrapId,
                    history, historyWarning, resume ? "Resume Training" : "Start Training");
        }
        ViewState(TrainingSettings settings, Phase phase, TrainerSnapshot snapshot, String message,
                  boolean active, boolean canStart, boolean resume, int previousDepth, String bootstrapId) {
            this(settings,phase,snapshot,message,active,canStart,resume,previousDepth,bootstrapId,HistoryRepository.Snapshot.EMPTY,"");
        }
    }
    record Inspection(boolean resume, String latestId, int depth, String bootstrapId, String action) {
        Inspection(boolean resume, String latestId, int depth, String bootstrapId) {
            this(resume, latestId, depth, bootstrapId, resume ? "Resume Training" : "Start Training");
        }
    }

    interface Handle {
        void start();
        void stop();
        TrainerSnapshot snapshot();
        boolean terminated();
        void close();
        default String historyWarning() { return ""; }
        default String lifecycleNotice() { return ""; }
        default long stopAfterGeneration() { return 0; }
        default boolean cancelScheduledStop() { return false; }
        default long scheduledStopGeneration() { return 0; }
    }

    /** Small lifecycle seam for controller tests; production delegates to F/G without duplicating it. */
    static class Backend {
        String preview(TrainingSettings requested) throws IOException {
            var settings = resolveSource(requested);
            var root = settings.root();
            if (com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.freshRoot(root,
                    settings.architecture().trainingArchitecture())) return "Start Training";
            var attempt = com.ohinteractive.seedv6.training.checkpoint.GenerationAttempt.inspect(root);
            String latest = com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.reference(root, "latest-training");
            if (attempt.isPresent()) {
                var a = attempt.get();
                var manifest = com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.manifest(root.resolve("checkpoints").resolve(latest));
                boolean partial = latest.equals(a.parentId()) || manifest.generation() == a.generation()
                        && !com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.validations(root).containsKey(latest);
                if (partial) return (a.matches(settings.config(TrainerConfig.DepthChange.EXPLICITLY_ALLOW),
                        settings.source() == null ? TrainingSource.SELF_PLAY : settings.source())
                        ? "Resume Generation " : "Restart Generation ") + a.generation();
            }
            var latestManifest = CheckpointInspection.manifest(root.resolve("checkpoints").resolve(latest));
            if (!latestManifest.parentId().isEmpty()) {
                var validation = CheckpointInspection.validations(root).get(latest);
                if (validation == null || validation.decision() == com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.PROMOTE
                        && !CheckpointInspection.reference(root, "best").equals(latest))
                    return "Resume Generation " + latestManifest.generation();
            }
            return "Start Training";
        }
        record Stopped(TrainerSnapshot snapshot, HistoryRepository.Snapshot history, String action, String bootstrapId) {}
        Stopped stopped(TrainingSettings settings) throws IOException {
            if (!CheckpointInspection.freshRoot(settings.root(), settings.architecture().trainingArchitecture())
                    && (Files.notExists(settings.root().resolve("refs/latest-training")) || Files.notExists(settings.root().resolve("refs/best"))))
                inspect(settings); // Preserve the existing evidence-gated recovery of interrupted reference publication.
            String action = preview(settings);
            var history = new HistoryRepository(settings.root()).refresh();
            if (CheckpointInspection.freshRoot(settings.root(), settings.architecture().trainingArchitecture()))
                return new Stopped(null, history, action, "");
            String latest = CheckpointInspection.reference(settings.root(), "latest-training");
            String best = CheckpointInspection.reference(settings.root(), "best");
            var manifest = CheckpointInspection.manifest(settings.root().resolve("checkpoints").resolve(latest));
            var bestManifest = CheckpointInspection.manifest(settings.root().resolve("checkpoints").resolve(best));
            CheckpointInspection.lineage(settings.root(), latest);
            CheckpointInspection.accepted(settings.root());
            boolean unfinished = action.startsWith("Resume Generation") || action.startsWith("Restart Generation");
            long generation = unfinished ? Long.parseLong(action.substring(action.lastIndexOf(' ') + 1)) : manifest.generation() + 1;
            var partial = PartialGeneration.inspect(settings.root()).filter(p -> unfinished && p.attempt().generation() == generation);
            var stats = partial.map(PartialGeneration::statistics).orElseGet(() -> new SelfPlayBatch.Statistics(
                    settings.games(), 0, 0, 0, 0, 0, 0, 0, 0, 0, Double.NaN, 0, 0));
            var decision = Optional.ofNullable(CheckpointInspection.validations(settings.root()).get(latest));
            var gamesDecision = decision.filter(v -> v.bootstrap() == null);
            var snapshot = new TrainerSnapshot(TrainerSnapshot.State.STOPPED, "", Duration.ZERO, generation,
                    best, latest, manifest.parentId().isEmpty() ? "" : latest, manifest.optimizerStep(), manifest.trainingDepth(),
                    stats, Optional.empty(), partial.map(p -> p.training().updates()).orElse(0L), 0, Double.NaN,
                    gamesDecision.map(ValidationRecord::statistics), gamesDecision.map(ValidationRecord::assessment),
                    new TrainerSnapshot.Totals(0,0,0,0,0,0,0,0,0,0,0), Optional.empty(),
                    gamesDecision.map(v -> new TrainerSnapshot.ValidationDetails(v.candidateId(), v.incumbentId(), v.config(), v.policy())))
                    .withBootstrapValidation(decision.filter(v -> v.bootstrap() != null).map(v ->
                            new TrainerSnapshot.BootstrapValidation(v.candidateId(), v.incumbentId(), v.bootstrap())));
            return new Stopped(snapshot, history, action, bestManifest.parentId().isEmpty() ? best : "");
        }
        TrainingSettings resolveSource(TrainingSettings settings) throws IOException {
            if (settings.validationMethod() == null) {
                var attempt = com.ohinteractive.seedv6.training.checkpoint.GenerationAttempt.inspect(settings.root());
                if (attempt.isPresent()) settings = settings.withValidationMethod(attempt.get().validationMethod());
            }
            if (settings.architecture() == NetworkArchitecture.BRN2) {
                var seeds = CheckpointStore.readBrnRunSeeds(settings.root());
                boolean seedFresh = com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.freshRoot(settings.root(), settings.architecture().trainingArchitecture());
                if (settings.runSeeds() != null && (!seedFresh || seeds.isPresent()))
                    CheckpointStore.requireSameRunSeeds(seeds.orElse(null), settings.runSeeds());
                if (seeds.isPresent()) settings = settings.withRunSeeds(seeds.get());
                var stored = CheckpointStore.readBrnSupervision(settings.root());
                boolean fresh = com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.freshRoot(settings.root(), settings.architecture().trainingArchitecture());
                var requested = settings.supervision();
                if (requested == null) settings = settings.withSupervision(stored.orElse(BrnSupervision.WDL));
                if (settings.captureConsistency() == null) settings = settings.withCaptureConsistency(CheckpointStore.readBrnCaptureConsistency(settings.root()));
            }
            var stored = CheckpointStore.readTrainingSource(settings.root());
            if (settings.architecture().nnueFamily() && settings.source() == null && stored.isEmpty()) return settings;
            boolean fresh = com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.freshRoot(settings.root(), settings.architecture().trainingArchitecture());
            if (settings.source() == null) settings = settings.withSource(stored.orElse(fresh
                    ? settings.architecture().corpusOnly() ? TrainingSource.dataSources(com.ohinteractive.seedv6.training.data.DataSources.directory(settings.root())) : settings.architecture().nnueFamily() ? TrainingSource.SELF_PLAY : settings.architecture() == NetworkArchitecture.BRN2 ? TrainingSource.HANDCRAFTED
                    : new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, settings.generatorStore()) : TrainingSource.SELF_PLAY));
            if (settings.architecture().supportsTrainingData()) {
                var pin = CorpusTraining.readPin(settings.root(), settings.source(), settings.corpusTraining(), settings.seed(), settings.architecture().trainingArchitecture());
                if (pin.isPresent() && settings.corpusSelected()) {
                    var corpus = settings.corpusTraining();
                    if (corpus == null || corpus.viewIdentity().isEmpty()) settings = settings.withCorpus(settings.source().generatorStore(),
                            new CorpusTrainingConfig(corpus == null ? pin.get().positions() : corpus.positionsPerGeneration(), pin.get().identity()));
                }
                if (settings.architecture() == NetworkArchitecture.BRN2 && settings.supervision().blended()) {
                    var teacher = CheckpointStore.readBrnTeacherStore(settings.root());
                    if (settings.teacherStore() == null) settings = settings.withTeacherStore(teacher.orElse(
                            settings.source().nnue() ? settings.source().generatorStore() : ""));
                }
            }
            return settings;
        }
        Inspection inspect(TrainingSettings settings) throws IOException {
            return inspectStore(settings);
        }

        private Inspection inspectStore(TrainingSettings settings) throws IOException {
            Path root = settings.root();
            if (com.ohinteractive.seedv6.training.checkpoint.FrozenReplay.read(root).isPresent()
                    || CheckpointStore.readTrainingSource(root).map(TrainingSource::frozen).orElse(false))
                throw new IOException("Frozen data replay: use the frozen-wdl command to Start or Resume this store.");
            if (com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.freshRoot(root,
                    settings.architecture().trainingArchitecture())) return new Inspection(false, "", settings.depth(), "");
            try (var store = new CheckpointStore(root, settings.architecture().trainingArchitecture())) {
                try {
                    store.requireEmptyForBootstrap();
                    return new Inspection(false, "", settings.depth(), "");
                } catch (IOException nonempty) {
                    var preview = store.recover();
                    for (String diagnostic : preview.diagnostics()) {
                        boolean missingReference = (diagnostic.startsWith("latest-training:") && !Files.exists(root.resolve("refs/latest-training")))
                                || (diagnostic.startsWith("best:") && !Files.exists(root.resolve("refs/best")));
                        if (!missingReference) throw new IOException("Checkpoint recovery reported corruption/incompatibility: " + diagnostic
                                + ". Store was preserved; select a valid store or repair it before resuming.");
                    }
                    var recovered = store.recoverTrainingReferences();
                    var latest = recovered.latestTraining().orElseThrow().manifest();
                    var best = recovered.best().orElseThrow().manifest();
                    boolean bootstrap = recovered.bestEvidence().orElseThrow().kind() == PromotionRecord.Kind.BOOTSTRAP;
                    return new Inspection(true, latest.id(), latest.trainingDepth(), bootstrap ? best.id() : "", preview(settings));
                }
            }
        }

        Handle create(TrainingSettings settings, boolean resume, TrainerConfig.DepthChange change) throws IOException {
            settings = resolveSource(settings);
            TrainerConfig config = settings.config(change);
            return handle(resume ? TrainerService.resume(config) : TrainerService.freshInitialized(config));
        }

        static Handle handle(TrainerService service) {
            return new Handle() {
                public void start() { service.start(); }
                public void stop() { service.stop(); }
                public TrainerSnapshot snapshot() { return service.snapshot(); }
                public boolean terminated() { return service.isTerminated(); }
                public void close() { service.close(); }
                public String historyWarning() { return service.historyWarning(); }
                public String lifecycleNotice() { return service.lifecycleNotice(); }
                public long stopAfterGeneration() { return service.stopAfterGeneration(); }
                public boolean cancelScheduledStop() { return service.cancelScheduledStop(); }
                public long scheduledStopGeneration() { return service.scheduledStopGeneration(); }
            };
        }
    }

    TrainingController(TrainingSettings settings, Backend backend, Consumer<TrainingSettings> persist,
                       Consumer<ViewState> view) {
        requireEdt();
        this.settings = settings; this.backend = backend; this.persist = persist;
        this.view = view;
    }

    private TrainingSettings settings;
    private final Backend backend;
    private final Consumer<TrainingSettings> persist;
    private final Consumer<ViewState> view;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "seedv6-ui-training-io"));
    private volatile Handle service;
    // Only the nonblocking start/stop requests share this gate; filesystem work and close never hold it.
    private final Object startStopGate = new Object();
    private volatile boolean stopRequested, closing;
    private Phase phase = Phase.IDLE;
    private TrainerSnapshot snapshot;
    private Inspection inspection;
    private String message = "Select a lineage to start or resume its training. Initial Best is a bootstrap network.";
    private String bootstrapId = "";
    private boolean active, resume;
    private long operation;
    private HistoryRepository.Snapshot history = HistoryRepository.Snapshot.EMPTY;
    private String historyReadWarning = "";
    private boolean historyLoading;
    private long historyChecked, historyCompleted = -1;
    // Accessed only on the serial I/O executor, never by the trainer writer.
    private HistoryRepository historyReader;
    private Path historyRoot;
    private String nextAction = "Start / Resume Training";
    private long previewTicket;
    private boolean previewed, previewPending;
    private TrainingLineages.Selection lineage;
    private boolean loading, lineageSelectionRequired;
    void requireLineageSelection() {
        requireEdt(); lineageSelectionRequired = true; previewed = true; nextAction = "Start Training"; publish();
    }

    /** Prepare privately on the serial I/O executor, then replace every lineage-owned field in one EDT publication. */
    void selectLineage(Callable<TrainingLineages.Selection> loader, TrainingSettings empty,
                       Consumer<Exception> completed) {
        requireEdt();
        if (active || closing || loading) throw new IllegalStateException("Stop training before changing lineage.");
        loading = true; ++previewTicket; publish();
        io.execute(() -> {
            try {
                var selected = loader.call();
                var nextSettings = selected == null ? empty : selected.settings();
                var stopped = selected == null ? new Backend.Stopped(null, HistoryRepository.Snapshot.EMPTY, "Start Training", "")
                        : backend.stopped(nextSettings);
                if (selected != null) TrainingLineages.adopt(selected);
                var previous = service;
                if (previous != null) previous.close();
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    service = null; lineage = selected; settings = nextSettings; snapshot = stopped.snapshot();
                    history = stopped.history(); historyReadWarning = ""; historyChecked = 0; historyCompleted = -1;
                    inspection = null; resume = snapshot != null; bootstrapId = stopped.bootstrapId();
                    nextAction = stopped.action(); phase = Phase.IDLE; previewed = true; previewPending = false; loading = false;
                    lineageSelectionRequired = selected == null;
                    message = selected == null ? "Create or import a training lineage." : selected.lineage().configurationOrigin();
                    publish(); completed.accept(null);
                });
            } catch (Exception failure) {
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    loading = false;
                    if (previewPending) { previewPending = false; previewed = false; }
                    publish(); completed.accept(failure);
                });
            }
        });
    }

    private void persist(TrainingSettings value, TrainingLineages.Selection owner) throws IOException {
        if (owner != null) TrainingLineages.save(owner, value);
        persist.accept(value);
    }

    ViewState state() {
        requireEdt();
        return new ViewState(settings, phase, snapshot, message, active,
                !active && !closing && !loading && !previewPending && !lineageSelectionRequired && !nextAction.equals("New Store Required"), resume, inspection == null ? settings.depth() : inspection.depth(), bootstrapId,
                history, String.join("\n", java.util.stream.Stream.of(historyReadWarning,
                        service == null ? "" : service.historyWarning()).filter(s -> !s.isBlank()).toList()), nextAction,
                lineage, loading, active && service != null ? service.scheduledStopGeneration() : 0);
    }

    void setSettings(TrainingSettings value) {
        requireEdt();
        if (active || closing || loading) throw new IllegalStateException("Stop training before changing settings.");
        if (lineage != null && (!lineage.entry().root().equals(value.root()) || lineage.entry().architecture() != value.architecture()))
            throw new IllegalArgumentException("Select a training lineage before applying its settings.");
        if (!settings.root().equals(value.root()) || settings.architecture() != value.architecture()) {
            history = HistoryRepository.Snapshot.EMPTY; historyReadWarning = ""; historyChecked = 0; historyCompleted = -1;
            snapshot = null; resume = false; inspection = null; bootstrapId = "";
            nextAction = "Start Training"; ++previewTicket;
            Handle previous = service;
            service = null;
            if (previous != null) io.execute(previous::close);
        }
        boolean changed = !settings.equals(value);
        settings = value;
        var owner = lineage;
        if (changed) previewed = false;
        if (changed) persistConfiguration(value, owner);
        publish();
    }

    private void persistConfiguration(TrainingSettings value, TrainingLineages.Selection owner) {
        io.execute(() -> {
            try { persist(value, owner); }
            catch (Exception failure) {
                SwingUtilities.invokeLater(() -> {
                    if (!closing) { message = "Could not save lineage configuration: " + concise(failure); publish(); }
                });
            }
        });
    }

    void start() {
        requireEdt();
        if (active || closing || loading || lineageSelectionRequired || nextAction.equals("New Store Required")) return;
        ++previewTicket; previewPending = false;
        active = true; stopRequested = false; phase = Phase.STARTING; snapshot = null;
        long ticket = ++operation;
        message = "Checking checkpoint store / recovering latest-training...";
        publish();
        TrainingSettings requested = settings;
        var owner = lineage;
        Handle previous = service;
        service = null;
        io.execute(() -> {
            try {
                if (previous != null) previous.close();
                TrainingSettings resolved = backend.resolveSource(requested);
                if (resolved.source() != null && resolved.source().nnue()) resolved.source().requireGenerator(resolved.root());
                if (!resolved.corpusSelected() && resolved.supervision() != null && resolved.supervision().blended()) {
                    if (resolved.teacherStore() == null || resolved.teacherStore().isBlank()) throw new IOException("Select an NNUE Teacher Store for blended supervision.");
                    TrainingSource.bootstrap(Path.of(resolved.teacherStore())).requireGenerator(resolved.root());
                }
                persist(resolved, owner);
                Inspection found = backend.inspect(resolved);
                SwingUtilities.invokeLater(() -> {
                    if (ticket == operation && !closing) { settings = resolved; nextAction = found.action(); }
                    prepared(found, ticket);
                });
            } catch (Exception failure) { reportFailure(failure, ticket); }
        });
    }

    private void prepared(Inspection found, long ticket) {
        if (closing || ticket != operation || !active) return;
        if (stopRequested) { finish(Phase.STOPPED, "Startup cancelled safely."); return; }
        inspection = found; resume = found.resume(); bootstrapId = found.bootstrapId();
        if (found.resume() && found.depth() != settings.depth()) {
            phase = Phase.CONFIRM_DEPTH;
            message = "Resume from depth " + found.depth() + " at depth " + settings.depth() + "? Existing checkpoints will be preserved.";
            publish();
        } else launch(TrainerConfig.DepthChange.REQUIRE_SAME, false);
    }

    void confirmDepth(boolean approved) {
        requireEdt();
        if (phase != Phase.CONFIRM_DEPTH || closing) return;
        if (!approved) { finish(Phase.STOPPED, "Depth change declined. Training remains stopped."); return; }
        launch(TrainerConfig.DepthChange.EXPLICITLY_ALLOW, true);
    }

    private void launch(TrainerConfig.DepthChange change, boolean recheck) {
        phase = Phase.STARTING;
        message = resume ? nextAction + " from durable training state..."
                : settings.architecture() == NetworkArchitecture.BRN_PAIR2 ? "Initializing experimental Pair-2 material prior / zero pair residual / Adam state..."
                : settings.architecture() == NetworkArchitecture.BRN3 ? "Bootstrapping BRN-3 material prior / fresh relational residual / Adam state..."
                : settings.architecture() == NetworkArchitecture.BRN1 ? "Bootstrapping deterministic BRN-1 network / Adam state..."
                : settings.architecture() == NetworkArchitecture.BRN2 ? "Bootstrapping deterministic BRN-2 network / Adam state..."
                : settings.architecture() == NetworkArchitecture.BRN ? "Bootstrapping zero-initialized BRN network / Adam state..."
                : "Bootstrapping deterministic network (seed " + settings.seed() + ")...";
        publish();
        TrainingSettings requested = settings;
        Inspection approved = inspection;
        long ticket = operation;
        io.execute(() -> {
            try {
                if (recheck) {
                    Inspection now = backend.inspect(requested);
                    if (!now.equals(approved)) throw new IOException("Store changed during depth confirmation. Start again to review its current lineage.");
                }
                if (!stopRequested) {
                    service = backend.create(requested, approved.resume(), change);
                    synchronized (startStopGate) {
                        if (stopRequested) service.stop(); else service.start();
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    if (closing || ticket != operation || !active) return;
                    if (service == null) finish(Phase.STOPPED, "Startup cancelled safely.");
                    else {
                        phase = stopRequested ? Phase.STOPPING : Phase.RUNNING;
                        message = stopRequested ? "Stopping at a safe durable boundary..."
                                : approved.resume() ? "Resumed latest-training; best remains the accepted playing network."
                                : "Initial best is a deterministic bootstrap network, not a validation promotion.";
                        poll();
                    }
                });
            } catch (Exception failure) { reportFailure(failure, ticket); }
        });
    }

    void stop() {
        requireEdt();
        if (!active || closing) return;
        stopRequested = true;
        if (phase == Phase.CONFIRM_DEPTH) { finish(Phase.STOPPED, "Startup cancelled safely."); return; }
        phase = Phase.STOPPING; message = "Stopping at a safe durable boundary...";
        synchronized (startStopGate) { if (service != null) service.stop(); }
        publish();
    }

    void toggleScheduledStop() {
        requireEdt();
        if (!active || closing || phase != Phase.RUNNING || service == null) return;
        if (service.scheduledStopGeneration() != 0) service.cancelScheduledStop();
        else service.stopAfterGeneration();
        publish();
    }

    void poll() {
        requireEdt();
        if (closing) return;
        if (!active && !loading && !lineageSelectionRequired && !previewed) previewAction();
        Handle owned = service;
        if (owned != null) {
            snapshot = owned.snapshot();
            if (active && !owned.lifecycleNotice().isBlank()) message = owned.lifecycleNotice();
            refreshHistory();
            if (!resume && bootstrapId.isEmpty() && !snapshot.bestId().isEmpty()) bootstrapId = snapshot.bestId();
            if (active && owned.terminated()) {
                // The service has released its store lock. Preserve the binding established at
                // Start before stopped edits or a later invocation can replace the draft.
                snapshot.run().filter(r -> r.source().corpus() && settings.corpusSelected()).ifPresent(r -> {
                    var bound = settings.withCorpus(r.source().generatorStore(), r.effective().corpusTraining());
                    if (!settings.equals(bound)) {
                        settings = bound;
                        persistConfiguration(bound, lineage);
                    }
                });
                resume = !snapshot.latestTrainingId().isEmpty();
                finish(snapshot.failed() ? Phase.FAILED : Phase.STOPPED,
                        snapshot.failed() ? snapshot.failureSummary() : owned.lifecycleNotice().isBlank()
                                ? "Safely stopped. Resume continues latest-training; Best remains the accepted network."
                                : "Safely stopped. " + owned.lifecycleNotice());
                return;
            }
        }
        refreshHistory();
        publish();
    }

    private void previewAction() {
        previewed = true; previewPending = true;
        TrainingSettings requested = settings; long ticket = ++previewTicket;
        io.execute(() -> {
            String action, detail = "";
            try { action = backend.preview(requested); }
            catch (Exception problem) {
                detail = concise(problem);
                action = detail.contains("lineage") && detail.contains("fresh") ? "New Store Required" : "Review Training Settings";
            }
            String result = action, notice = detail;
            SwingUtilities.invokeLater(() -> {
                if (closing || active || loading || ticket != previewTicket || !requested.equals(settings)) return;
                nextAction = result;
                previewPending = false;
                if (phase != Phase.FAILED) {
                    if (!notice.isBlank()) message = notice;
                    else if (result.startsWith("Restart Generation")) message = result
                            + ": changed generation settings restart only unfinished work; completed history and Best are preserved.";
                    else if (result.startsWith("Resume Generation")) message = result + ": continue from persisted partial work.";
                }
                publish();
            });
        });
    }

    private void refreshHistory() {
        if (loading || lineageSelectionRequired) return;
        long completed = snapshot == null ? 0 : snapshot.totals().completedGenerations();
        long now = System.nanoTime();
        if (historyLoading || historyChecked != 0 && completed == historyCompleted && now-historyChecked < 5_000_000_000L) return;
        historyLoading = true; historyChecked = now; historyCompleted = completed;
        Path requested = settings.root();
        io.execute(() -> {
            HistoryRepository.Snapshot loaded = null; String warning = "";
            try {
                if (!requested.equals(historyRoot)) { historyReader = new HistoryRepository(requested); historyRoot = requested; }
                loaded = historyReader.refresh();
            } catch (IOException | RuntimeException failure) { warning = "History read failed at " + requested.resolve(HistoryRepository.FILE) + ": " + failure; }
            var result = loaded; var error = warning;
            SwingUtilities.invokeLater(() -> {
                historyLoading = false;
                if (closing || !requested.equals(settings.root())) { historyChecked = 0; return; }
                if (result != null) history = result;
                historyReadWarning = error; publish();
            });
        });
    }

    /** Called on EDT, then joined by the window's existing background shutdown path. Never interrupts durable I/O. */
    Runnable beginShutdown() {
        requireEdt();
        if (closing) return () -> awaitShutdown();
        closing = true; stopRequested = true; phase = Phase.CLOSING;
        synchronized (startStopGate) { if (service != null) service.stop(); }
        publish();
        shutdown = io.submit(() -> { if (service != null) service.close(); });
        io.shutdown();
        return this::awaitShutdown;
    }

    private Future<?> shutdown;
    private void awaitShutdown() {
        try {
            shutdown.get(35, TimeUnit.SECONDS);
            if (!io.awaitTermination(1, TimeUnit.SECONDS)) throw new IllegalStateException("Training startup is still draining. Retry closing shortly.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted while closing training.", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            // Preserve ownership on timeout. A subsequent close can wait again without interrupting checkpoint writes.
            Handle owned = service;
            if (owned != null && owned.terminated() && io.isTerminated()) return;
            throw new IllegalStateException("Training shutdown has not completed. Wait, then close again. " + concise(failure), failure);
        }
    }

    private void reportFailure(Exception failure, long ticket) {
        SwingUtilities.invokeLater(() -> {
            if (!closing && ticket == operation && active) finish(Phase.FAILED, concise(failure));
        });
    }

    private void finish(Phase next, String detail) {
        operation++; // A queued startup callback cannot alter a stopped or subsequently restarted run.
        phase = next; message = detail; active = false; previewed = false;
        nextAction = "Checking lineage..."; previewPending = true;
        previewAction(); publish();
    }

    private void publish() { view.accept(state()); }
    static String concise(Throwable failure) {
        Throwable cause = failure;
        if (cause instanceof ExecutionException && cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Training UI access must occur on the EDT.");
    }
}
