package com.ohinteractive.seedv6.gui;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import javax.swing.*;
import com.ohinteractive.seedv6.corpus.CorpusReader;
import com.ohinteractive.seedv6.corpus.lichess.LichessImporter;

/** EDT-owned task state; all source/catalog I/O runs in SwingWorker's background executor. */
final class CorpusImportController {
    record Request(String archive, String root, boolean all, long limit) {
        LichessImporter.Options options() {
            if (archive.isBlank()) throw new IllegalArgumentException("Select a source .jsonl.zst archive.");
            if (root.isBlank()) throw new IllegalArgumentException("Select a Seed corpus root directory.");
            if (!all && limit < 1) throw new IllegalArgumentException("Bounded import count must be a positive integer.");
            return new LichessImporter.Options(Path.of(archive).toAbsolutePath().normalize(),
                    Path.of(root).toAbsolutePath().normalize(), all ? 0 : limit,
                    LichessImporter.DEFAULT_SHARD_SIZE, LichessImporter.DEFAULT_PROGRESS_EVERY);
        }
    }
    enum Phase { IDLE, IMPORTING, STOPPING, VALIDATING, COMPLETE, STOPPED, VALID, FAILED }
    record State(Phase phase, String message, LichessImporter.Progress progress,
                 LichessImporter.Summary summary, CorpusReader.Integrity integrity, String root) {
        boolean active() { return phase == Phase.IMPORTING || phase == Phase.STOPPING || phase == Phase.VALIDATING; }
    }
    interface Backend {
        LichessImporter.Summary run(LichessImporter.Options options, Consumer<LichessImporter.Progress> progress,
                                   BooleanSupplier stop) throws Exception;
        CorpusReader.Integrity validate(Path root) throws Exception;
    }
    static final class ProductionBackend implements Backend {
        public LichessImporter.Summary run(LichessImporter.Options options, Consumer<LichessImporter.Progress> progress,
                                          BooleanSupplier stop) throws Exception {
            try (var out = new PrintStream(OutputStream.nullOutputStream())) {
                return LichessImporter.run(options, out, progress, stop);
            }
        }
        public CorpusReader.Integrity validate(Path root) throws Exception {
            try (var reader = new CorpusReader(root)) { return reader.validate(); }
        }
    }
    private final Backend backend;
    private final Consumer<State> changed;
    private final Consumer<String> corpusChanged;
    private final AtomicBoolean stop = new AtomicBoolean();
    private volatile LichessImporter.Progress latest;
    private final Timer timer = new Timer(250, event -> poll());
    private SwingWorker<?, ?> worker;
    private boolean closing;
    private State state = new State(Phase.IDLE, "Select an archive and corpus root.", null, null, null, "");

    CorpusImportController(Backend backend, Consumer<State> changed, Consumer<String> corpusChanged) {
        this.backend = backend; this.changed = changed; this.corpusChanged = corpusChanged;
    }
    State state() { return state; }
    private void publish(State value) { state = value; changed.accept(value); }
    private void available() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Corpus controls require EDT.");
        if (closing || state.active()) throw new IllegalStateException("A corpus task is already active or closing.");
    }
    void start(Request request) {
        available();
        latest = null;
        final LichessImporter.Options options;
        try { options = request.options(); }
        catch (RuntimeException invalid) { fail(invalid, request.root()); return; }
        stop.set(false);
        publish(new State(Phase.IMPORTING, "Opening source and corpus...", null, null, null, options.corpus().toString()));
        worker = new SwingWorker<LichessImporter.Summary, Void>() {
            protected LichessImporter.Summary doInBackground() throws Exception {
                if (!Files.isRegularFile(options.input()) || !Files.isReadable(options.input())
                        || !options.input().getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jsonl.zst"))
                    throw new IllegalArgumentException("Source must be an existing readable .jsonl.zst file.");
                checkRoot(options.corpus(), true);
                return backend.run(options, progress -> latest = progress, stop::get);
            }
            protected void done() {
                timer.stop();
                try {
                    var result = get();
                    CorpusImportController.this.publish(new CorpusImportController.State(result.stopped() ? Phase.STOPPED : Phase.COMPLETE,
                            result.stopped() ? "Import stopped. Committed data retained; rerunning is safe."
                                    : "Import complete. Corpus available for training.", latest, result, null, options.corpus().toString()));
                } catch (Exception invalid) { fail(invalid, options.corpus().toString()); }
                // Even failed imports can retain completed checkpoints; reread the selected catalog.
                corpusChanged.accept(options.corpus().toString());
            }
        };
        timer.start(); worker.execute();
    }
    void stop() {
        if (state.phase() != Phase.IMPORTING) return;
        stop.set(true);
        publish(new State(Phase.STOPPING, "Stopping after the current record and committing the batch...",
                latest, null, null, state.root()));
    }
    void poll() {
        if (state.active() && latest != null && latest != state.progress())
            publish(new State(state.phase(), state.message(), latest, null, null, state.root()));
    }
    void validate(String root) {
        available(); latest = null;
        publish(new State(Phase.VALIDATING, "Validating catalog, checksums and records; large corpora may take time...",
                null, null, null, root));
        worker = new SwingWorker<CorpusReader.Integrity, Void>() {
            protected CorpusReader.Integrity doInBackground() throws Exception {
                if (root.isBlank()) throw new IllegalArgumentException("Select a Seed corpus root directory.");
                Path path = Path.of(root); checkRoot(path, false);
                return backend.validate(path);
            }
            protected void done() {
                try { CorpusImportController.this.publish(new CorpusImportController.State(Phase.VALID, "Corpus integrity valid.", null, null, get(), root)); }
                catch (Exception invalid) { fail(invalid, root); }
                corpusChanged.accept(root);
            }
        };
        worker.execute();
    }
    private static void checkRoot(Path root, boolean write) {
        if (!Files.isDirectory(root) || !Files.isReadable(root) || write && !Files.isWritable(root))
            throw new IllegalArgumentException("Corpus root must be an existing " + (write ? "writable " : "readable ")
                    + "directory. Choose an existing Seed corpus or an empty directory for import.");
    }
    private void fail(Exception invalid, String root) {
        boolean validation = state.phase() == Phase.VALIDATING;
        publish(new State(Phase.FAILED, TrainingController.concise(invalid)
                + (validation ? " Corpus validation failed." : " Completed import checkpoints, if any, remain retained."), latest, null, null, root));
    }
    /** Uses the window's existing background shutdown join, with no forced interruption. */
    Runnable beginShutdown() {
        closing = true; stop(); timer.stop();
        var task = worker;
        return () -> {
            if (task == null) return;
            try { task.get(35, TimeUnit.SECONDS); }
            catch (java.util.concurrent.ExecutionException completedFailure) { /* Task already closed its resources. */ }
            catch (Exception incomplete) {
                throw new IllegalStateException("Corpus task has not completed. Wait, then close again. "
                        + TrainingController.concise(incomplete), incomplete);
            }
        };
    }
}
