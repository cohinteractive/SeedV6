package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;

/** Reusable library selector. EDT owns selection; workers own filesystem access. */
class ModelSelectionPanel extends JPanel {
    final JComboBox<NetworkArchitecture> architecture = new JComboBox<>(NetworkArchitecture.values());
    final JComboBox<ModelLibrary.Entry> lineage = new JComboBox<>();
    final JComboBox<ModelChoice> generation = new JComboBox<>();
    private final JLabel detail = SeedTheme.label("Choose a model lineage", 12, SeedTheme.SECONDARY);
    private final JButton refresh = new JButton("Refresh"), register = new JButton("Register...");
    private final JButton details = new JButton("Details / notes...");
    private final TrainingFolders folders;
    private final Preferences preferences;
    private final Runnable changed;
    private ModelLibrary.Snapshot snapshot;
    private long request;
    private boolean editing = true, updating, loading, disposed;
    private String error = "";

    ModelSelectionPanel(String key, String title, Path fallback, TrainingFolders folders, Preferences preferences, Runnable changed) {
        super(new BorderLayout(0, SeedTheme.scale(5))); setOpaque(false);
        this.folders = folders; this.preferences = preferences; this.changed = changed;
        setName(key + "EngineSetup"); architecture.setName(key + "Architecture"); lineage.setName(key + "Lineage");
        generation.setName(key + "Network"); detail.setName(key + "StoreIdentity"); refresh.setName(key + "RefreshNetworks");
        for (var box : List.of(architecture, lineage, generation)) box.setMinimumSize(new Dimension(80, box.getPreferredSize().height));
        register.setName(key + "RegisterModel");
        register.setToolTipText("Register an existing external lineage once, without moving its files");
        details.setName(key + "GenerationDetails");
        var fields = SeedTheme.panel(new GridBagLayout());
        TrainingPanel.row(fields, 0, "Architecture", architecture);
        TrainingPanel.row(fields, 1, "Lineage", lineage);
        TrainingPanel.row(fields, 2, "Generation", generation);
        var actions = SeedTheme.panel(new GridLayout(0, 2, SeedTheme.scale(5), SeedTheme.scale(5)));
        actions.add(refresh); actions.add(details); actions.add(register);
        var footer = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(5)));
        footer.add(detail, BorderLayout.NORTH); footer.add(actions);
        add(SeedTheme.label(title, 14, SeedTheme.TEXT), BorderLayout.NORTH); add(fields); add(footer, BorderLayout.SOUTH);
        generation.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof ModelChoice choice) {
                    var g = find(choice.checkpointId());
                    setText(g.map(item -> choice.checkpointId().isEmpty() ? "Best (Gen " + item.number() + ")" : item.label()).orElse(choice + " (unavailable)"));
                    setToolTipText(g.flatMap(ModelLibrary.Generation::annotation).map(GenerationAnnotation::notes).orElse(null));
                }
                return this;
            }
        });
        architecture.addActionListener(e -> { if (!updating) load(null, "", false, ""); });
        lineage.addActionListener(e -> {
            if (!updating && lineage.getSelectedItem() instanceof ModelLibrary.Entry selected)
                load(selected.root(), "", false, selected.lineage().map(l -> l.id().toString()).orElse(""));
        });
        generation.addActionListener(e -> { if (!updating) { remember(); setEditable(editing); changed.run(); } });
        details.addActionListener(e -> showDetails());
        refresh.addActionListener(e -> refresh(true));
        register.addActionListener(e -> FilePickers.choose(this, FilePickers.Purpose.ADOPT_CHECKPOINT,
                "Register existing model lineage in place", folders.base().toString()).ifPresent(this::selectStore));
        try {
            Path initial = preferences == null ? fallback : Path.of(preferences.get("store", fallback.toString()));
            load(initial, preferences == null ? "" : preferences.get("generation", ""), true,
                    preferences == null ? "" : preferences.get("lineageId", ""));
        } catch (java.nio.file.InvalidPathException invalid) {
            error = "Saved model location is invalid. Select or register the intended lineage.";
            detail.setText("Model unavailable"); detail.setToolTipText(error); setEditable(editing);
        }
    }

    private Optional<ModelLibrary.Generation> find(String id) {
        if (snapshot == null) return Optional.empty();
        return id.isEmpty() ? snapshot.best() : snapshot.generations().stream().filter(g -> g.id().equals(id)).findFirst();
    }
    boolean validSelection() {
        return !disposed && !loading && error.isEmpty() && snapshot != null
                && generation.getSelectedItem() instanceof ModelChoice c
                && ((DefaultComboBoxModel<?>) generation.getModel()).getIndexOf(c) >= 0
                && find(c.checkpointId()).filter(g -> g.checkpoint().materialized()).isPresent();
    }
    Path selectedRoot() { return snapshot == null ? null : snapshot.lineage().root(); }
    UUID selectedLineageId() { return snapshot == null ? null : snapshot.lineage().lineage().map(TrainingLineage::id).orElse(null); }
    String selectedId() { return generation.getSelectedItem() instanceof ModelChoice c ? c.checkpointId() : ""; }
    String concreteId() { return validSelection() ? find(selectedId()).orElseThrow().id() : ""; }
    String error() { return error; }
    void setEditable(boolean value) {
        editing = value; boolean enabled = value && !disposed && !loading;
        architecture.setEnabled(enabled); lineage.setEnabled(enabled); generation.setEnabled(enabled && snapshot != null);
        refresh.setEnabled(enabled); register.setEnabled(enabled);
        details.setEnabled(enabled && find(selectedId()).isPresent());
    }
    void dispose() { disposed = true; request++; }
    void selectStore(Path root) { load(root, "", true, ""); }
    void refresh(boolean preserveSelection) {
        load(selectedRoot(), preserveSelection ? selectedId() : "", false,
                selectedLineageId() == null ? "" : selectedLineageId().toString());
    }

    private record Loaded(List<ModelLibrary.Entry> entries, ModelLibrary.Snapshot snapshot, String selection) {}
    private void load(Path root, String selected, boolean registerExisting, String expectedId) {
        if (disposed) return;
        long ticket = ++request; loading = true; error = ""; setEditable(editing); changed.run();
        NetworkArchitecture requested = (NetworkArchitecture) architecture.getSelectedItem();
        Path base = folders.base(); detail.setText("Reading model library...");
        new SwingWorker<Loaded, Void>() {
            protected Loaded doInBackground() throws Exception {
                ModelLibrary.Entry entry = root == null ? null : ModelLibrary.identify(root);
                var actual = entry == null ? requested : NetworkArchitecture.valueOf(entry.architecture().name());
                var entries = new ArrayList<>(ModelLibrary.discover(base, actual.trainingArchitecture(), folders.adopted(base, actual)));
                if (entry != null) {
                    var chosen = entry;
                    entries.removeIf(e -> e.root().equals(chosen.root())); entries.add(entry);
                } else if (!entries.isEmpty()) entry = entries.getFirst();
                if (entry == null) return new Loaded(entries, null, selected);
                var found = ModelLibrary.browse(entry);
                if (!expectedId.isEmpty() && !found.lineage().lineage().map(l -> l.id().toString()).orElse("").equals(expectedId))
                    throw new IOException("Selected lineage identity changed. Register the intended lineage explicitly.");
                if (registerExisting) folders.register(base, actual, entry.root());
                return new Loaded(List.copyOf(entries), found, selected);
            }
            protected void done() {
                if (disposed || ticket != request) return;
                updating = true;
                try { var value = get(); install(value.entries(), value.snapshot(), value.selection()); }
                catch (Exception failure) {
                    snapshot = null; generation.removeAllItems(); error = TrainingController.concise(failure);
                    detail.setText("Model unavailable"); detail.setToolTipText(error);
                } finally { updating = false; loading = false; setEditable(editing); remember(); changed.run(); }
            }
        }.execute();
    }

    private void install(List<ModelLibrary.Entry> entries, ModelLibrary.Snapshot value, String id) {
        snapshot = value; error = "";
        lineage.setModel(new DefaultComboBoxModel<>(entries.toArray(ModelLibrary.Entry[]::new)));
        var choices = new DefaultComboBoxModel<ModelChoice>();
        if (value != null) {
            architecture.setSelectedItem(NetworkArchitecture.valueOf(value.lineage().architecture().name()));
            lineage.setSelectedItem(value.lineage());
            if (value.best().filter(g -> g.checkpoint().materialized()).isPresent()) choices.addElement(ModelChoice.BEST);
            for (var item : value.generations()) if (item.checkpoint().materialized()) choices.addElement(new ModelChoice(item.id()));
            // Publish-only stores have no Best alias; initial selection uses their recorded Latest.
            if (id.isEmpty() && value.publishOnly()) id = value.latest().map(ModelLibrary.Generation::id).orElse("");
        }
        choices.setSelectedItem(new ModelChoice(id)); generation.setModel(choices);
        if (value == null) { detail.setText("No lineages here. Create one in Network Training."); detail.setToolTipText(null); }
        else {
            detail.setText(value.generations().size() + " generations · Best " + value.best().map(g -> "Gen " + g.number()).orElse("not recorded")
                    + " · Latest " + value.latest().map(g -> "Gen " + g.number()).orElse("not recorded"));
            detail.setToolTipText(value.lineage().root() + (value.diagnostics().isEmpty() ? "" : " · " + String.join("; ", value.diagnostics())));
        }
    }
    private void remember() {
        if (preferences == null || !validSelection()) return;
        preferences.put("store", selectedRoot().toString()); preferences.put("generation", selectedId());
        preferences.put("lineageId", selectedLineageId() == null ? "" : selectedLineageId().toString());
    }

    /** Complete synchronous transfer: freeze aliases before swapping; no model re-resolution. */
    void swapWith(ModelSelectionPanel other) {
        if (!validSelection() || !other.validSelection()) throw new IllegalStateException("Choose both model generations before swapping.");
        var mine = snapshot; var theirs = other.snapshot;
        var myEntries = entries(); var theirEntries = other.entries();
        String myId = concreteId(), theirId = other.concreteId();
        request++; other.request++; updating = other.updating = true;
        try {
            install(theirEntries, theirs, theirId);
            other.install(myEntries, mine, myId);
        } finally { updating = other.updating = false; }
        remember(); other.remember(); changed.run(); other.changed.run();
    }
    private List<ModelLibrary.Entry> entries() {
        var result = new ArrayList<ModelLibrary.Entry>();
        for (int i = 0; i < lineage.getItemCount(); i++) result.add(lineage.getItemAt(i));
        return List.copyOf(result);
    }
    private void showDetails() {
        var found = find(selectedId()); if (found.isEmpty()) return;
        var selected = found.get(); var current = snapshot;
        var panel = new GenerationDetailsPanel(current, selected);
        if (JOptionPane.showConfirmDialog(this, panel, "Generation details / notes", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        final GenerationAnnotation annotation;
        try { annotation = panel.annotation(); }
        catch (IllegalArgumentException invalid) { JOptionPane.showMessageDialog(this, invalid.getMessage()); return; }
        long ticket = ++request; loading = true; setEditable(editing); changed.run();
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                try (var store = new CheckpointStore(current.lineage().root(), current.lineage().architecture())) {
                    var actual = TrainingLineage.read(store.root());
                    if (!actual.map(TrainingLineage::id).equals(current.lineage().lineage().map(TrainingLineage::id)))
                        throw new IOException("Lineage identity changed. Refresh before saving notes.");
                    store.writeAnnotation(annotation, selected.annotation());
                }
                return null;
            }
            protected void done() {
                if (disposed || ticket != request) return;
                loading = false;
                try { get(); refresh(true); }
                catch (Exception failure) {
                    setEditable(editing); changed.run();
                    JOptionPane.showMessageDialog(ModelSelectionPanel.this, TrainingController.concise(failure), "Notes were not saved", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
