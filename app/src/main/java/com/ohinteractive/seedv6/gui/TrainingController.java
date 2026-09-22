package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.checkpoint.PromotionRecord;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.nnue.TrainableNnue;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;

/** EDT-owned UI state; the I/O executor prepares startup, and TrainerService owns all learning. */
final class TrainingController {
    enum Phase { IDLE, STARTING, CONFIRM_DEPTH, RUNNING, STOPPING, STOPPED, FAILED, CLOSING }
    record ViewState(TrainingSettings settings, Phase phase, TrainerSnapshot snapshot, String message,
                     boolean active, boolean canStart, boolean resume, int previousDepth, String bootstrapId,
                     HistoryRepository.Snapshot history, String historyWarning) {
        ViewState(TrainingSettings settings, Phase phase, TrainerSnapshot snapshot, String message,
                  boolean active, boolean canStart, boolean resume, int previousDepth, String bootstrapId) {
            this(settings,phase,snapshot,message,active,canStart,resume,previousDepth,bootstrapId,HistoryRepository.Snapshot.EMPTY,"");
        }
    }
    record Inspection(boolean resume, String latestId, int depth, String bootstrapId) {}

    interface Handle {
        void start();
        void stop();
        TrainerSnapshot snapshot();
        boolean terminated();
        void close();
        default String historyWarning() { return ""; }
    }

    /** Small lifecycle seam for controller tests; production delegates to F/G without duplicating it. */
    static class Backend {
        Inspection inspect(TrainingSettings settings) throws IOException {
            return inspectStore(settings);
        }

        private Inspection inspectStore(TrainingSettings settings) throws IOException {
            Path root = settings.root();
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
                    return new Inspection(true, latest.id(), latest.trainingDepth(), bootstrap ? best.id() : "");
                }
            }
        }

        Handle create(TrainingSettings settings, boolean resume, TrainerConfig.DepthChange change) throws IOException {
            TrainerConfig config = settings.config(change);
            return switch (settings.architecture()) {
                case NNUE -> handle(resume ? TrainerService.resume(config)
                        : TrainerService.fresh(config, new NnueTrainer(TrainableNnue.initialized(settings.seed()))));
                case BRN1 -> handle(resume ? TrainerService.resume(config)
                        : TrainerService.fresh(config, new com.ohinteractive.seedv6.core.brn1.Brn1Trainer(settings.brn1LearningRate())));
                case BRN2 -> handle(resume ? TrainerService.resume(config)
                        : TrainerService.fresh(config, new com.ohinteractive.seedv6.core.brn2.Brn2Trainer(settings.brn2LearningRate())));
                case BRN -> handle(resume ? TrainerService.resume(config)
                        : TrainerService.fresh(config, new com.ohinteractive.seedv6.core.brn.BrnTrainer(settings.brnLearningRate())));
            };
        }

        static Handle handle(TrainerService service) {
            return new Handle() {
                public void start() { service.start(); }
                public void stop() { service.stop(); }
                public TrainerSnapshot snapshot() { return service.snapshot(); }
                public boolean terminated() { return service.isTerminated(); }
                public void close() { service.close(); }
                public String historyWarning() { return service.historyWarning(); }
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
    private String message = "Start bootstraps an empty folder or resumes latest-training. Initial best is a bootstrap network.";
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

    ViewState state() {
        requireEdt();
        return new ViewState(settings, phase, snapshot, message, active,
                !active && !closing, resume, inspection == null ? settings.depth() : inspection.depth(), bootstrapId,
                history, String.join("\n", java.util.stream.Stream.of(historyReadWarning,
                        service == null ? "" : service.historyWarning()).filter(s -> !s.isBlank()).toList()));
    }

    void setSettings(TrainingSettings value) {
        requireEdt();
        if (active || closing) throw new IllegalStateException("Stop training before changing settings.");
        if (!settings.root().equals(value.root()) || settings.architecture() != value.architecture()) {
            history = HistoryRepository.Snapshot.EMPTY; historyReadWarning = ""; historyChecked = 0; historyCompleted = -1;
            snapshot = null; resume = false; inspection = null; bootstrapId = "";
            Handle previous = service;
            service = null;
            if (previous != null) io.execute(previous::close);
        }
        boolean changed = !settings.equals(value);
        settings = value;
        if (changed) io.execute(() -> {
            try { persist.accept(value); }
            catch (RuntimeException failure) {
                SwingUtilities.invokeLater(() -> {
                    if (!closing) { message = "Could not save GUI preferences: " + concise(failure); publish(); }
                });
            }
        });
        publish();
    }

    void start() {
        requireEdt();
        if (active || closing) return;
        active = true; stopRequested = false; phase = Phase.STARTING; snapshot = null;
        long ticket = ++operation;
        message = "Checking checkpoint store / recovering latest-training...";
        publish();
        TrainingSettings requested = settings;
        Handle previous = service;
        service = null;
        io.execute(() -> {
            try {
                if (previous != null) previous.close();
                persist.accept(requested);
                Inspection found = backend.inspect(requested);
                SwingUtilities.invokeLater(() -> prepared(found, ticket));
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
        message = resume ? "Resuming exact latest-training model / " + settings.architecture().optimizerName() + " state..."
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

    void poll() {
        requireEdt();
        if (closing) return;
        Handle owned = service;
        if (owned != null) {
            snapshot = owned.snapshot();
            refreshHistory();
            if (!resume && bootstrapId.isEmpty() && !snapshot.bestId().isEmpty()) bootstrapId = snapshot.bestId();
            if (active && owned.terminated()) {
                resume = !snapshot.latestTrainingId().isEmpty();
                finish(snapshot.failed() ? Phase.FAILED : Phase.STOPPED,
                        snapshot.failed() ? snapshot.failureSummary() : "Safely stopped. Resume continues latest-training; Best remains the accepted network.");
                return;
            }
        }
        refreshHistory();
        publish();
    }

    private void refreshHistory() {
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
        phase = next; message = detail; active = false; publish();
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
