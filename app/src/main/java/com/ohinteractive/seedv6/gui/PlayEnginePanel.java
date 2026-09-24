package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.prefs.Preferences;
import javax.swing.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** One side's next-game selection. All store I/O runs away from the EDT. */
final class PlayEnginePanel extends JPanel {
    private final JTextField store = new JTextField(18);
    final JComboBox<PlayEvaluator.Choice> generation = new JComboBox<>();
    private final JButton browse = new JButton("Browse..."), refresh = new JButton("Refresh");
    private final JLabel detail = SeedTheme.label("Select a checkpoint store", 12, SeedTheme.SECONDARY);
    private final Runnable changed;
    private final Preferences preferences;
    private final Timer debounce;
    private long request, bestGeneration;
    private boolean ready, editing = true, updating;
    private Path resolvedRoot;
    private String error = "";

    PlayEnginePanel(String side, Path fallback, Preferences preferences, Runnable changed) {
        super(new BorderLayout(0, SeedTheme.scale(5))); setOpaque(false);
        this.changed = changed; this.preferences = preferences;
        store.setName(side + "CheckpointStore"); generation.setName(side + "Network");
        detail.setName(side + "StoreIdentity"); refresh.setName(side + "RefreshNetworks");
        var pathRow = SeedTheme.panel(new BorderLayout(SeedTheme.scale(5), 0));
        pathRow.add(store); pathRow.add(browse, BorderLayout.EAST);
        var choices = SeedTheme.panel(new BorderLayout(SeedTheme.scale(5), 0));
        choices.add(generation); choices.add(refresh, BorderLayout.EAST);
        var fields = SeedTheme.panel(new GridLayout(0, 1, 0, SeedTheme.scale(5)));
        fields.add(pathRow); fields.add(choices); fields.add(detail);
        add(SeedTheme.label(side.equals("white") ? "White Engine" : "Black Engine", 14, SeedTheme.TEXT), BorderLayout.NORTH);
        add(fields);
        generation.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof PlayEvaluator.Choice choice) {
                    setText(choice.checkpointId().isEmpty() ? "Best (Gen " + bestGeneration + ")"
                            : "Gen " + TrainingProgress.generation(java.util.OptionalLong.empty(), choice.checkpointId()));
                    if (index == -1 && ((DefaultComboBoxModel<?>) generation.getModel()).getIndexOf(value) < 0)
                        setText(getText() + " (unavailable)");
                }
                return this;
            }
        });
        generation.addActionListener(e -> { if (!updating) changed.run(); });
        debounce = new Timer(250, e -> refresh(false)); debounce.setRepeats(false);
        store.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            private void edited() {
                request++; ready = false; resolvedRoot = null; error = "";
                generation.removeAllItems(); detail.setText("Reading store...");
                store.setToolTipText(store.getText()); debounce.restart(); changed.run();
            }
        });
        browse.addActionListener(e -> {
            var chooser = new JFileChooser(store.getText()); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) selectStore(chooser.getSelectedFile().toPath());
        });
        refresh.addActionListener(e -> refresh(true));
        // Persist only the store. Best is always the default, so stale generations cannot break startup.
        store.setText(preferences == null ? fallback.toString() : preferences.get("store", fallback.toString()));
    }
    void selectStore(Path root) { store.setText(root.toString()); debounce.stop(); refresh(false); }
    boolean validSelection() {
        Object choice = generation.getSelectedItem();
        return ready && error.isEmpty() && resolvedRoot != null && choice != null
                && ((DefaultComboBoxModel<?>) generation.getModel()).getIndexOf(choice) >= 0;
    }
    Path selectedRoot() { return resolvedRoot; }
    String selectedId() {
        var choice = (PlayEvaluator.Choice) generation.getSelectedItem();
        return choice == null ? "" : choice.checkpointId();
    }
    String error() { return error; }
    void setEditable(boolean enabled) {
        editing = enabled; store.setEnabled(enabled); browse.setEnabled(enabled); refresh.setEnabled(enabled);
        generation.setEnabled(enabled && ready && error.isEmpty());
    }
    void dispose() { request++; debounce.stop(); }
    void refresh(boolean preserveSelection) {
        debounce.stop(); long ticket = ++request;
        String requested = store.getText().trim();
        String selected = preserveSelection ? selectedId() : "";
        ready = false; resolvedRoot = null; setEditable(editing); changed.run();
        new SwingWorker<Choices, Void>() {
            protected Choices doInBackground() throws Exception {
                if (requested.isEmpty()) throw new java.io.IOException("Select a checkpoint store.");
                Path root = Path.of(requested).toAbsolutePath().normalize();
                var best = CheckpointStore.readBestSnapshot(root);
                PlayEvaluator.recognize(root, best);
                return new Choices(root, best.manifest(), CheckpointStore.availableCheckpoints(root));
            }
            protected void done() {
                if (ticket != request) return;
                updating = true;
                try {
                    var choices = get(); resolvedRoot = choices.root(); bestGeneration = choices.best().generation();
                    var model = new DefaultComboBoxModel<PlayEvaluator.Choice>(); model.addElement(PlayEvaluator.Choice.BEST);
                    for (var checkpoint : choices.available().checkpoints()) model.addElement(new PlayEvaluator.Choice(checkpoint.id()));
                    // A live explicit choice must not silently fall back after pruning.
                    model.setSelectedItem(new PlayEvaluator.Choice(selected)); generation.setModel(model);
                    error = "";
                    detail.setText(NetworkArchitecture.valueOf(choices.best().architecture().name()) + " \u00b7 Best Gen " + bestGeneration);
                    detail.setToolTipText(choices.root() + (choices.available().diagnostics().isEmpty() ? ""
                            : " \u00b7 " + String.join("; ", choices.available().diagnostics())));
                    if (preferences != null) preferences.put("store", choices.root().toString());
                } catch (Exception failure) {
                    error = TrainingController.concise(failure); generation.removeAllItems();
                    detail.setText("Store unavailable"); detail.setToolTipText(error);
                } finally { ready = true; updating = false; setEditable(editing); changed.run(); }
            }
        }.execute();
    }
    private record Choices(Path root, com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest best,
                           CheckpointStore.AvailableCheckpoints available) {}
}
