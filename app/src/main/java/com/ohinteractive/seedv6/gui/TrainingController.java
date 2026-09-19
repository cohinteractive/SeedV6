package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.checkpoint.PromotionRecord;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.nnue.TrainableNnue;
import com.ohinteractive.seedv6.training.service.*;

/** EDT-owned UI state; the I/O executor prepares startup, and TrainerService owns all learning. */
final class TrainingController {
    enum Phase { IDLE, STARTING, CONFIRM_DEPTH, RUNNING, STOPPING, STOPPED, FAILED, CLOSING }
    record ViewState(TrainingSettings settings, Phase phase, TrainerSnapshot snapshot, String message,
                     boolean active, boolean canStart, boolean resume, int previousDepth, String bootstrapId) {}
    record Inspection(boolean resume, String latestId, int depth, String bootstrapId) {}

    interface Handle {
        void start();
        void stop();
        TrainerSnapshot snapshot();
        boolean terminated();
        void close();
    }

    /** Small lifecycle seam for controller tests; production delegates to F/G without duplicating it. */
    static class Backend {
        Inspection inspect(TrainingSettings settings) throws IOException {
            Path root = settings.root();
            if (!Files.exists(root)) return new Inspection(false, "", settings.depth(), "");
            if (!Files.isDirectory(root)) throw new IOException("Checkpoint root must be a directory: " + root);
            try (var entries = Files.list(root)) {
                var names = entries.map(p -> p.getFileName().toString()).toList();
                if (names.isEmpty()) return new Inspection(false, "", settings.depth(), "");
                if (!Set.of("store.lock", "checkpoints", "staging", "validations", "promotions", "refs").containsAll(names)) {
                    throw new IOException("This non-empty folder is not a checkpoint store. Select an empty folder or an existing training store.");
                }
            }
            try (var store = new CheckpointStore(root)) {
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
            return handle(resume ? TrainerService.resume(config)
                    : TrainerService.fresh(config, new NnueTrainer(TrainableNnue.initialized(settings.seed()))));
        }

        static Handle handle(TrainerService service) {
            return new Handle() {
                public void start() { service.start(); }
                public void stop() { service.stop(); }
                public TrainerSnapshot snapshot() { return service.snapshot(); }
                public boolean terminated() { return service.isTerminated(); }
                public void close() { service.close(); }
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

    ViewState state() {
        requireEdt();
        return new ViewState(settings, phase, snapshot, message, active,
                !active && !closing, resume, inspection == null ? settings.depth() : inspection.depth(), bootstrapId);
    }

    void setSettings(TrainingSettings value) {
        requireEdt();
        if (active || closing) throw new IllegalStateException("Stop training before changing settings.");
        if (!settings.root().equals(value.root())) {
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
        message = resume ? "Resuming exact latest-training model / Adam state..." : "Bootstrapping deterministic network (seed " + settings.seed() + ")...";
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
            if (!resume && bootstrapId.isEmpty() && !snapshot.bestId().isEmpty()) bootstrapId = snapshot.bestId();
            if (active && owned.terminated()) {
                resume = !snapshot.latestTrainingId().isEmpty();
                finish(snapshot.failed() ? Phase.FAILED : Phase.STOPPED,
                        snapshot.failed() ? snapshot.failureSummary() : "Safely stopped. Resume continues latest-training; play uses best.");
                return;
            }
        }
        publish();
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
