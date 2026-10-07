package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/** Independent GUI worker lifecycle; filesystem/model work never runs on the EDT. */
final class LearningArenaController {
    private final Consumer<LearningArenaController> view;
    private Thread worker;
    private volatile LearningArenaService service;
    private volatile LearningArenaService.Update update;
    private volatile Path root;
    private volatile String error = "";
    private volatile boolean pauseRequested;
    private boolean closing;
    private long attachment;
    LearningArenaController(Consumer<LearningArenaController> view) { this.view = view; }
    boolean busy() { return worker != null; }
    LearningArenaService.Update update() {
        var value = update; var owner = service;
        return value == null ? null : value.withGame(owner == null ? null : owner.liveGame());
    }
    String error() { return error; }
    Path root() { return root; }
    long attachment() { return attachment; }
    void newCampaign() {
        requireEdt();
        if (busy() || closing) throw new IllegalStateException("Learning Arena worker is busy");
        root = null; update = null; error = ""; ++attachment;
        view.accept(this);
    }
    interface Configuration { LearningArenaConfig resolve() throws Exception; }
    void start(Path path, LearningArenaConfig config) { launch(path, () -> config, false); }
    void startDraft(Path path, Configuration config) { launch(path, config, false); }
    void resume() { if (root == null) throw new IllegalStateException("Open a campaign first"); launch(root, null, false); }
    void open(Path path) { launch(path, null, true); }
    private void launch(Path path, Configuration config, boolean inspect) {
        requireEdt();
        if (busy() || closing) throw new IllegalStateException("Learning Arena worker is busy");
        error = ""; pauseRequested = false;
        // A previous campaign's endpoints must never be registered under a newly opened root.
        root = null; update = null; ++attachment;
        worker = new Thread(() -> {
            try {
                if (inspect) {
                    var loaded = LearningArenaState.read(path);
                    root = path; update = new LearningArenaService.Update(loaded, "Opened saved campaign", null);
                } else {
                    Consumer<LearningArenaService.Update> publish = u -> { root = path; update = u; };
                    try (var owner = config == null ? LearningArenaService.resume(path, publish)
                            : LearningArenaService.create(path, config.resolve(), publish)) {
                        root = path; service = owner;
                        if (pauseRequested) owner.pause();
                        owner.run();
                        var last = update;
                        update = new LearningArenaService.Update(owner.state(), owner.state().message(), null, null, last == null ? null : last.optimization());
                    } finally { service = null; }
                }
            } catch (Exception failure) { error = failure.toString(); }
            finally { SwingUtilities.invokeLater(() -> { worker = null; view.accept(this); }); }
        }, "seedv6-learning-arena");
        worker.start(); view.accept(this);
    }
    void pause() { requireEdt(); pauseRequested = true; var owner = service; if (owner != null) owner.pause(); }
    void poll() { requireEdt(); view.accept(this); }
    Runnable beginShutdown() {
        requireEdt(); closing = true; pause(); Thread owned = worker;
        return () -> {
            if (owned == null) return;
            try { owned.join(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Learning Arena shutdown interrupted", e); }
        };
    }
    private static void requireEdt() { if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Expected Swing EDT"); }
}
