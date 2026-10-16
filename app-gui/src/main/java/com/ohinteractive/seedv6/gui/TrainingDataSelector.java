package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import javax.swing.*;

/** Shared source registration/selection, with all discovery and preparation outside the EDT. */
final class TrainingDataSelector extends JPanel {
    private final Supplier<Path> root;
    private final Supplier<java.util.List<TrainingArchitecture>> architectures;
    private final Runnable changed;
    private final JComboBox<DataSource> sources = new JComboBox<>();
    private final JTextArea detail = new JTextArea(4, 24);
    private final JButton refresh = new JButton("Refresh"), addFile = new JButton("Add dataset file..."), addFolder = new JButton("Add dataset folder...");
    private final JButton prepare = new JButton("Prepare / retry"), location = new JButton("Change location...");
    private final Map<String, TrainingDataLibrary.Readiness> readiness = new HashMap<>();
    private boolean busy, editable = true, updating;
    private long ticket;
    private boolean closed;
    private final java.util.List<LibraryWorker<?, ?>> workers = new java.util.ArrayList<>();
    private String diagnostics = "";
    private String busyDetail = "Checking Training Data...";
    TrainingDataSelector(String prefix, Supplier<Path> root, Supplier<java.util.List<TrainingArchitecture>> architectures, Runnable changed) {
        super(new BorderLayout(4, 6)); this.root = root; this.architectures = architectures; this.changed = changed; setOpaque(false);
        setName(prefix + "Library"); sources.setName(prefix); sources.setPrototypeDisplayValue(null);
        sources.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value instanceof DataSource source
                        ? source.name() + " · " + source.format() + " · " + source.identity().substring(0, 8) : "Select Training Data", index, selected, focus);
            }
        });
        sources.setPreferredSize(new Dimension(280, sources.getPreferredSize().height));
        add(sources, BorderLayout.NORTH);
        detail.setEditable(false); detail.setLineWrap(true); detail.setWrapStyleWord(true); detail.setOpaque(false); detail.setName(prefix + "Details");
        add(detail);
        var buttons = new JPanel(new GridLayout(0, 2, 6, 6)); buttons.setOpaque(false);
        for (var button : new JButton[]{refresh, addFile, addFolder, prepare, location}) buttons.add(button);
        add(buttons, BorderLayout.SOUTH); refresh.setName(prefix + "Refresh"); prepare.setName(prefix + "Prepare");
        sources.addActionListener(e -> { if (!updating) { presentation(); changed.run(); } });
        refresh.addActionListener(e -> reload(null)); addFile.addActionListener(e -> choose(false, FilePickers.Kind.FILE));
        addFolder.addActionListener(e -> choose(false, FilePickers.Kind.DIRECTORY));
        location.addActionListener(e -> choose(true, FilePickers.Kind.FILE_OR_DIRECTORY)); prepare.addActionListener(e -> prepare());
        reload(null);
    }
    void refreshLibrary() { reload(null); }
    void registerImported(String path) {
        if (closed || path.isBlank()) return;
        var library = new TrainingDataLibrary(root.get()); long expected = ++ticket;
        busy = true; busyDetail = "Registering imported dataset..."; presentation();
        new LibraryWorker<DataSource, Void>() {
            protected DataSource doInBackground() throws Exception {
                Path imported = Path.of(path);
                return library.register(DataSource.register(imported.getFileName().toString(), imported, 1, null));
            }
            protected void done() {
                if (ticket != expected) return; busy = false;
                try { reload(get()); }
                catch (Exception failure) { diagnostics = TrainingController.concise(failure); presentation(); changed.run(); }
            }
        }.start();
    }
    DataSource selected() { return (DataSource) sources.getSelectedItem(); }
    boolean ready() {
        var value = selected(); return !closed && !busy && value != null && readiness.getOrDefault(value.identity(), new TrainingDataLibrary.Readiness(false, -1, "Checking")).ready()
                && TrainingDataLibrary.incompatibility(value, architectures.get()).isEmpty();
    }
    void setEditable(boolean enabled) { editable = enabled && !closed; presentation(); }
    void compatibilityChanged() { presentation(); changed.run(); }
    /** Display the exact frozen campaign descriptor, even if its catalog entry was relocated. */
    void select(DataSource source) {
        if (closed) return;
        long expected = ++ticket; busy = true; readiness.clear(); diagnostics = "";
        updating = true;
        try { sources.setModel(new DefaultComboBoxModel<>(new DataSource[]{source})); }
        finally { updating = false; }
        busyDetail = "Checking Training Data..."; presentation(); changed.run();
        new LibraryWorker<TrainingDataLibrary.Readiness, Void>() {
            protected TrainingDataLibrary.Readiness doInBackground() throws Exception {
                new TrainingDataLibrary(root.get()).remember(source);
                return TrainingDataLibrary.inspect(source);
            }
            protected void done() {
                if (ticket != expected) return;
                try { readiness.put(source.identity(), get()); }
                catch (Exception failure) { diagnostics = TrainingController.concise(failure); }
                finally { busy = false; presentation(); changed.run(); }
            }
        }.start();
    }
    /** Use the same first catalog entry as a fresh selector, without retaining a campaign selection. */
    void resetSelection() {
        sources.setSelectedItem(null);
        reload(null);
    }
    private void reload(DataSource remembered) {
        if (closed) return;
        DataSource previous = remembered == null ? selected() : remembered;
        var library = new TrainingDataLibrary(root.get()); long expected = ++ticket; busy = true; busyDetail = "Checking Training Data..."; presentation(); changed.run();
        new LibraryWorker<TrainingDataLibrary.Catalog, Void>() {
            final Map<String, TrainingDataLibrary.Readiness> checked = new HashMap<>();
            protected TrainingDataLibrary.Catalog doInBackground() throws Exception {
                if (remembered != null) library.remember(remembered);
                var catalog = library.browse();
                for (var source : catalog.sources()) checked.put(source.identity(), TrainingDataLibrary.inspect(source));
                return catalog;
            }
            protected void done() {
                if (ticket != expected) return;
                updating = true;
                try {
                    var catalog = get(); readiness.clear(); readiness.putAll(checked); diagnostics = String.join("\n", catalog.diagnostics());
                    sources.setModel(new DefaultComboBoxModel<>(catalog.sources().toArray(DataSource[]::new)));
                    if (previous != null) sources.setSelectedItem(catalog.sources().stream().filter(s -> s.identity().equals(previous.identity())).findFirst().orElse(null));
                } catch (Exception failure) { readiness.clear(); diagnostics = TrainingController.concise(failure); }
                finally { busy = false; updating = false; presentation(); changed.run(); }
            }
        }.start();
    }
    private void choose(boolean moving, FilePickers.Kind kind) {
        DataSource previous = moving ? selected() : null; if (moving && previous == null) return;
        var choice = FilePickers.choose(this, moving ? FilePickers.Purpose.RELOCATE_TRAINING_DATA : FilePickers.Purpose.ADD_TRAINING_DATA,
                moving ? "Locate the same Training Data version" : "Register reusable Training Data", previous == null ? "" : previous.location(), kind);
        if (choice.isEmpty()) return;
        Path path = choice.get(); long expected = ++ticket; busy = true; busyDetail = "Detecting Training Data..."; presentation(); changed.run();
        new LibraryWorker<DataSource, Void>() {
            protected DataSource doInBackground() throws Exception {
                var format = DataSource.detect(path).format();
                var value = DataSource.register(previous == null ? path.getFileName().toString() : previous.name(), path, 1,
                        previous != null ? previous.labelProfile() : format == DataSource.Format.STOCKFISH_BINPACK_ZSTD ? LabelProfile.BT4_Q_V1 : null);
                if (previous != null && !previous.identity().equals(value.identity())) throw new java.io.IOException("Source version changed. Preserve bytes, size and modification timestamps to relocate; register changed data as a new version.");
                return value;
            }
            protected void done() {
                if (ticket != expected) return;
                busy = false;
                try {
                    var value = get();
                    if (previous == null && value.labelProfile() == LabelProfile.BT4_Q_V1 && JOptionPane.showConfirmDialog(TrainingDataSelector.this,
                            "Confirm BT4_Q_V1 labels: LC0 BT4 side-to-move Q, not centipawns.\nBINP bytes cannot identify the teacher. Use only data published with this label scheme.\nPreparation is reusable across Training and Arena.",
                            "Confirm source label profile", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) { presentation(); changed.run(); return; }
                    register(value);
                } catch (Exception failure) { diagnostics = TrainingController.concise(failure); presentation(); changed.run(); }
            }
        }.start();
    }
    /** Caller has explicitly chosen the descriptor and its label profile. */
    void register(DataSource value) {
        var library = new TrainingDataLibrary(root.get()); long expected = ++ticket; busy = true; busyDetail = "Registering Training Data..."; presentation(); changed.run();
        new LibraryWorker<DataSource, Void>() {
            protected DataSource doInBackground() throws Exception { return library.register(value); }
            protected void done() {
                if (ticket != expected) return;
                busy = false;
                try { reload(get()); }
                catch (Exception failure) { diagnostics = TrainingController.concise(failure); presentation(); changed.run(); }
            }
        }.start();
    }
    private void prepare() {
        DataSource value = selected(); if (value == null || value.format() != DataSource.Format.STOCKFISH_BINPACK_ZSTD) return;
        long expected = ++ticket; busy = true; busyDetail = "Preparing " + value.name(); presentation(); changed.run();
        new LibraryWorker<Void, CorpusPreparation.Progress>() {
            protected Void doInBackground() throws Exception { PreparedBinpack.prepare(value, new CorpusPreparation(this::isCancelled, p -> publish(p)), true); return null; }
            protected void process(java.util.List<CorpusPreparation.Progress> values) {
                if (ticket == expected && !values.isEmpty()) { busyDetail = "Preparing " + value.name() + "\n" + values.getLast(); presentation(); }
            }
            protected void done() {
                if (ticket != expected) return; busy = false;
                try { get(); reload(value); }
                catch (Exception failure) { diagnostics = TrainingController.concise(failure); presentation(); changed.run(); }
            }
        }.start();
    }
    /** Cancel preparation and drain owned workers off the EDT before application shutdown. */
    Runnable beginShutdown() {
        closed = true; editable = false; ++ticket; presentation();
        var owned = java.util.List.copyOf(workers);
        for (var worker : owned) worker.cancel(true);
        return () -> {
            for (var worker : owned) try { worker.thread.join(); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException("Training Data shutdown interrupted", failure); }
        };
    }
    private abstract class LibraryWorker<T, V> extends SwingWorker<T, V> {
        final Thread thread = new Thread(() -> {
            try { run(); } finally { SwingUtilities.invokeLater(() -> workers.remove(this)); }
        }, "seedv6-data-library");
        void start() { if (closed) return; workers.add(this); thread.start(); }
    }
    private void presentation() {
        sources.setEnabled(editable && !busy);
        for (var button : new JButton[]{refresh, addFile, addFolder}) button.setEnabled(editable && !busy);
        var value = selected(); location.setEnabled(editable && !busy && value != null);
        prepare.setEnabled(editable && !busy && value != null && value.format() == DataSource.Format.STOCKFISH_BINPACK_ZSTD);
        String text = busy ? busyDetail : value == null ? "Register a source once, then select it in Training or Arena." : readiness.getOrDefault(value.identity(), new TrainingDataLibrary.Readiness(false, -1, "Not checked")).detail()
                + "\nDataset ID: " + value.identity() + "\nFormat: " + value.format()
                + "\nLabel profile: " + value.labelProfile() + "\nSource: " + value.location() + "\n" + capabilities(value);
        if (!busy && !diagnostics.isEmpty()) text += "\n" + diagnostics;
        detail.setText(text); detail.setToolTipText(value == null ? new TrainingDataLibrary(root.get()).directory().toString() : value.location());
    }
    private String capabilities(DataSource value) {
        if (architectures.get().isEmpty()) return "Select this dataset under a lineage's Training Sources to check compatibility.";
        String reason = TrainingDataLibrary.incompatibility(value, architectures.get());
        return reason.isEmpty() ? "Compatible with selected trainer(s)" : "Unsupported: " + reason;
    }
}
