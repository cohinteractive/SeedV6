package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.service.TrainingSource;
import com.ohinteractive.seedv6.training.service.CorpusTrainingConfig;
import com.ohinteractive.seedv6.training.service.BrnCorpusTraining;
import com.ohinteractive.seedv6.corpus.CorpusReader;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** Read-only asynchronous store selection. User edits are local until the stopped service starts. */
final class BrnTrainingSourcePanel extends JPanel {
    private JLabel sourceLabel;
    private final JComboBox<TrainingSource.Mode> mode = new JComboBox<>();
    private final JTextField generator = new JTextField(24);
    private final JButton browse = new JButton("Browse...");
    private final JPanel generatorFields = panel(new GridBagLayout());
    private final JPanel generatorRow = panel(new BorderLayout(8, 0));
    private final JLabel note = label("", 11, SeedTheme.SECONDARY);
    private final JTextField corpusRoot = new JTextField(24);
    private final JButton corpusBrowse = new JButton("Browse...");
    private final JSpinner positions = new JSpinner(new SpinnerNumberModel(2, 2, Integer.MAX_VALUE, 1));
    private final JPanel corpusFields = panel(new GridBagLayout());
    private final JLabel corpusStatus = label("No corpus selected. Choose a Seed corpus root directory.", 11, SeedTheme.SECONDARY);
    private final Map<String, CorpusDraft> corpusDrafts = new HashMap<>();
    private record CorpusDraft(String root, CorpusTrainingConfig config) {}
    private String identity = "", corpusError = "";
    private long corpusRequest;
    private boolean corpusChecked, corpusConfigured;
    private final Timer corpusDelay = new Timer(250, e -> checkCorpus());
    private final Map<String, TrainingSource> drafts = new HashMap<>();
    private String key = "", error = "";
    private long request;
    private boolean ready, updating, editable = true, locked;
    private NetworkArchitecture architecture;
    private final TrainingFolders folders;
    private record Selection(TrainingSource source, boolean locked, BrnCorpusTraining.Pin pin) {}
    private final Runnable changed;

    BrnTrainingSourcePanel(TrainingSettings settings, Runnable changed) {
        this(settings, new TrainingFolders(settings), changed);
    }
    BrnTrainingSourcePanel(TrainingSettings settings, TrainingFolders folders, Runnable changed) {
        super(new BorderLayout(0, 8)); setOpaque(false); this.changed = changed; this.folders = folders;
        mode.setName("brnTrainingSource"); generator.setName("nnueGeneratorStore"); browse.setName("browseNnueGenerator");
        generator.setText(settings.generatorStore());
        corpusRoot.setName("brnCorpusRoot"); corpusBrowse.setName("browseBrnCorpus"); positions.setName("brnCorpusPositions");
        corpusStatus.setName("brnCorpusStatus"); positions.setEditor(new JSpinner.NumberEditor(positions, "0"));
        corpusRoot.setText(defaultCorpusRoot(settings.corpusRoot()));
        corpusConfigured = settings.corpusTraining() != null;
        if (settings.corpusTraining() != null) { positions.setValue(settings.corpusTraining().positionsPerGeneration()); identity = settings.corpusTraining().viewIdentity(); }
        corpusDelay.setRepeats(false);
        JPanel fields = panel(new GridBagLayout());
        TrainingPanel.row(fields, 0, "Position generation", mode); sourceLabel = (JLabel) fields.getComponent(0);
        generatorRow.add(generator); generatorRow.add(browse, BorderLayout.EAST);
        TrainingPanel.row(generatorFields, 0, "NNUE Generator Store", generatorRow);
        JPanel corpusEntry = panel(new BorderLayout(8, 0)); corpusEntry.add(corpusRoot); corpusEntry.add(corpusBrowse, BorderLayout.EAST);
        TrainingPanel.row(corpusFields, 0, "Seed corpus root", corpusEntry);
        TrainingPanel.row(corpusFields, 1, "Positions / generation", positions);
        TrainingPanel.row(corpusFields, 2, "Corpus status", corpusStatus);
        JPanel locations = panel(new BorderLayout()); locations.add(generatorFields, BorderLayout.NORTH); locations.add(corpusFields);
        JPanel selection = panel(new BorderLayout()); selection.add(fields, BorderLayout.NORTH); selection.add(locations);
        add(selection); add(note, BorderLayout.SOUTH);
        if (settings.source() != null) drafts.put(settings.architecture() + "|" + settings.root(), settings.source());
        corpusDrafts.put(settings.architecture() + "|" + settings.root(), new CorpusDraft(settings.corpusRoot(), settings.corpusTraining()));
        mode.addActionListener(e -> { if (!updating) { remember(); refresh(); validateCorpusLater(); } });
        corpusRoot.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            private void edited() { if (!updating) { identity = ""; remember(); validateCorpusLater(); } }
        });
        positions.addChangeListener(e -> { if (!updating) { identity = ""; remember(); validateCorpusLater(); } });
        corpusBrowse.addActionListener(e -> {
            var chooser = new JFileChooser(corpusRoot.getText()); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Seed corpus root (catalog and shards)");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) corpusRoot.setText(chooser.getSelectedFile().toString());
        });
        generator.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { remember(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { remember(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { remember(); }
        });
        browse.addActionListener(e -> {
            var chooser = new JFileChooser(generator.getText()); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) generator.setText(chooser.getSelectedFile().toString());
        });
    }
    private void remember() {
        if (updating || !ready || key.isEmpty()) return;
        try {
            drafts.put(key, selection());
            if (corpus()) corpusConfigured = true;
            corpusDrafts.put(key, new CorpusDraft(corpusRoot.getText(), corpusConfigured ? corpusConfig() : null));
        } catch (RuntimeException invalidDraft) { /* Read validates before Start. */ }
    }
    void selectRoot(String path, NetworkArchitecture architecture) {
        long ticket = ++request;
        this.architecture = architecture; locked = false;
        sourceLabel.setText("Position generation");
        updating = true; mode.removeAllItems();
        if (architecture == NetworkArchitecture.BRN2) mode.addItem(TrainingSource.Mode.HANDCRAFTED);
        if (architecture == NetworkArchitecture.BRN2) mode.addItem(TrainingSource.Mode.EXTERNAL_CORPUS);
        mode.addItem(TrainingSource.Mode.NNUE_BOOTSTRAP);
        mode.addItem(TrainingSource.Mode.SELF_PLAY);
        mode.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, BrnTrainingSourcePanel.this.architecture == NetworkArchitecture.NNUE ? "NNUE self-play"
                        : BrnTrainingSourcePanel.this.architecture == NetworkArchitecture.BRN2 && value == TrainingSource.Mode.NNUE_BOOTSTRAP ? "NNUE" : value, index, selected, focus);
            }
        });
        updating = false;
        setVisible(true);
        if (architecture == NetworkArchitecture.NNUE) {
            updating = true; mode.removeAllItems(); mode.addItem(TrainingSource.Mode.SELF_PLAY); updating = false;
            ready = true; key = ""; refresh(); return;
        }
        ready = false; error = ""; key = ""; refresh();
        if (path.isBlank()) { ready = true; apply(defaultSource(generator.getText())); return; }
        final Path root;
        try { root = Path.of(path).toAbsolutePath().normalize(); }
        catch (RuntimeException invalid) { error = invalid.getMessage(); ready = true; refresh(); return; }
        String selectedKey = architecture + "|" + root;
        var draft = drafts.get(selectedKey);
        String fallback = generator.getText();
        new SwingWorker<Selection, Void>() {
            protected Selection doInBackground() throws Exception {
                var stored = CheckpointStore.readTrainingSource(root);
                boolean fresh = CheckpointInspection.freshRoot(root, architecture.trainingArchitecture());
                boolean lock = stored.map(TrainingSource::frozen).orElse(false);
                var selected = draft != null ? draft : stored.orElse(fresh ? defaultSource(fallback) : TrainingSource.SELF_PLAY);
                var pin = selected.corpus() ? BrnCorpusTraining.readPin(root).orElse(null) : null;
                return new Selection(selected, lock, pin);
            }
            protected void done() {
                if (ticket != request) return;
                key = selectedKey; ready = true;
                try {
                    var selection = get(); locked = selection.locked();
                    var saved = corpusDrafts.get(selectedKey);
                    corpusConfigured = saved != null && saved.config() != null || selection.pin() != null;
                    updating = true;
                    corpusRoot.setText(defaultCorpusRoot(saved == null ? "" : saved.root()));
                    identity = saved == null || saved.config() == null ? "" : saved.config().viewIdentity();
                    positions.setValue(saved == null || saved.config() == null ? 2 : saved.config().positionsPerGeneration());
                    if (selection.pin() != null && selection.source().corpus()
                            && selection.source().generatorStore().equals(selection.pin().root())
                            && (saved == null || saved.config() == null || saved.config().positionsPerGeneration() == selection.pin().positions()
                            && (saved.config().viewIdentity().isEmpty() || saved.config().viewIdentity().equals(selection.pin().identity())))) {
                        corpusRoot.setText(selection.pin().root()); positions.setValue(selection.pin().positions()); identity = selection.pin().identity();
                    }
                    updating = false; apply(selection.source()); validateCorpusLater();
                }
                catch (Exception invalid) { error = TrainingController.concise(invalid); refresh(); }
            }
        }.execute();
    }
    private TrainingSource defaultSource(String fallback) {
        return architecture == NetworkArchitecture.BRN2 ? TrainingSource.HANDCRAFTED : new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, fallback);
    }
    /** All I/O was completed by the controller; no draft or asynchronous read may cross a lineage switch. */
    void load(TrainingSettings settings) {
        ++request; updating = true; key = ""; ready = false; error = "";
        architecture = settings.architecture(); mode.removeAllItems();
        if (architecture == NetworkArchitecture.BRN2) mode.addItem(TrainingSource.Mode.HANDCRAFTED);
        if (architecture == NetworkArchitecture.BRN2) mode.addItem(TrainingSource.Mode.EXTERNAL_CORPUS);
        if (architecture != NetworkArchitecture.NNUE) mode.addItem(TrainingSource.Mode.NNUE_BOOTSTRAP);
        mode.addItem(TrainingSource.Mode.SELF_PLAY);
        var source = settings.source() == null ? architecture == NetworkArchitecture.NNUE ? TrainingSource.SELF_PLAY
                : defaultSource(settings.generatorStore()) : settings.source();
        if (source.frozen()) mode.addItem(source.mode());
        identity = settings.corpusTraining() == null ? "" : settings.corpusTraining().viewIdentity();
        corpusConfigured = settings.corpusTraining() != null;
        locked = source.frozen(); generator.setText(settings.generatorStore());
        corpusRoot.setText(defaultCorpusRoot(settings.corpusRoot())); positions.setValue(settings.corpusTraining() == null ? 2 : settings.corpusTraining().positionsPerGeneration());
        mode.setSelectedItem(source.mode()); ready = true; updating = false;
        key = architecture + "|" + settings.root(); drafts.put(key, source);
        corpusDrafts.put(key, new CorpusDraft(settings.corpusRoot(), settings.corpusTraining())); refresh(); validateCorpusLater();
    }
    private void apply(TrainingSource source) {
        updating = true;
        if (source.frozen() && architecture == NetworkArchitecture.BRN2) mode.addItem(source.mode()); // Existing stores only.
        mode.setSelectedItem(source.mode());
        if (source.nnue()) generator.setText(source.generatorStore());
        if (source.corpus()) corpusRoot.setText(source.generatorStore());
        updating = false; refresh();
    }
    private String defaultCorpusRoot(String own) { return own.isBlank() ? folders.lastCorpusRoot() : own; }
    TrainingSource read() {
        // Apply may precede the asynchronous read; the backend resolves null under startup inspection.
        if (!ready) return null;
        if (!error.isEmpty()) throw new IllegalArgumentException(error);
        if (corpus() && !corpusError.isEmpty()) throw new IllegalArgumentException(corpusError);
        return selection();
    }
    private TrainingSource selection() { return new TrainingSource((TrainingSource.Mode) mode.getSelectedItem(), corpus() ? corpusRoot.getText() : generator.getText()); }
    boolean corpus() { return architecture == NetworkArchitecture.BRN2 && mode.getSelectedItem() == TrainingSource.Mode.EXTERNAL_CORPUS; }
    String corpusRoot() { return corpusRoot.getText().trim(); }
    private CorpusTrainingConfig corpusConfig() { return new CorpusTrainingConfig(((Number) positions.getValue()).intValue(), identity); }
    CorpusTrainingConfig readCorpusConfig() throws java.text.ParseException {
        if (!corpus() && !corpusConfigured) return null;
        if (corpus()) {
            // NumberFormatter can truncate a fractional input when its value class is Integer.
            // Validate the entered integer through the same service contract before committing.
            try { new CorpusTrainingConfig(Integer.parseInt(((JSpinner.DefaultEditor) positions.getEditor()).getTextField().getText().trim()), identity); }
            catch (RuntimeException invalid) { throw new IllegalArgumentException("Positions / generation must be an integer from 2 through " + Integer.MAX_VALUE + ".", invalid); }
            positions.commitEdit();
        }
        return corpusConfig();
    }
    String generatorStore() { return generator.getText().trim(); }
    boolean ready() { return ready && (!corpus() || corpusChecked && corpusError.isEmpty()); }
    boolean bootstrap() { return isVisible() && mode.getSelectedItem() != TrainingSource.Mode.SELF_PLAY; }
    void setEditable(boolean value) { editable = value; refresh(); }
    private void refresh() {
        boolean bootstrap = mode.getSelectedItem() == TrainingSource.Mode.NNUE_BOOTSTRAP;
        corpusFields.setVisible(corpus());
        corpusRoot.setEnabled(editable && ready && !locked && corpus()); corpusBrowse.setEnabled(corpusRoot.isEnabled());
        positions.setEnabled(editable && ready && !locked && corpus());
        sourceLabel.setText(architecture == NetworkArchitecture.BRN2 ? "Training source" : "Position generation");
        mode.setEnabled(editable && ready && !locked); generatorFields.setVisible(bootstrap);
        generator.setEnabled(editable && ready && !locked && bootstrap); browse.setEnabled(editable && ready && !locked && bootstrap);
        note.setText(!ready ? "Reading stored training source..." : !error.isEmpty() ? error : bootstrap
                ? "NNUE Best generates games. Candidate validation is selected independently."
                : corpus() ? "CP targets. Model / run seed controls deterministic corpus ordering. Validation is independent."
                : mode.getSelectedItem() == TrainingSource.Mode.FROZEN_REPLAY ? "Frozen data replay. Use the frozen-wdl command to Start or Resume."
                : mode.getSelectedItem() == TrainingSource.Mode.HANDCRAFTED ? "Handcrafted search generates positions. Supervision independently selects targets."
                : "The network generates games. Candidate validation is selected independently.");
        changed.run(); revalidate();
    }

    private void validateCorpusLater() {
        ++corpusRequest; corpusDelay.stop(); corpusChecked = false; corpusError = "";
        if (!corpus()) { changed.run(); return; }
        if (corpusRoot.getText().isBlank()) {
            corpusChecked = true; corpusError = "No corpus selected. Choose a Seed corpus root directory.";
            corpusStatus.setText(corpusError); changed.run(); return;
        }
        corpusStatus.setText("Checking corpus catalog..."); corpusDelay.restart(); changed.run();
    }
    private void checkCorpus() {
        long ticket = corpusRequest; String path = corpusRoot();
        new SwingWorker<Long, Void>() {
            protected Long doInBackground() throws Exception {
                try (var reader = new CorpusReader(Path.of(path))) { return reader.manifest().positions(); }
            }
            protected void done() {
                if (ticket != corpusRequest || !corpus()) return;
                corpusChecked = true;
                try {
                    long count = get(); corpusError = "";
                    folders.rememberCorpusRoot(path);
                    corpusStatus.setText("Valid corpus: " + count + " positions. " + (identity.isEmpty()
                            ? "View pins at Start." : "Pinned view " + identity.substring(0, 12) + "."));
                } catch (Exception invalid) {
                    corpusError = "Invalid/unreadable Seed corpus: " + TrainingController.concise(invalid);
                    corpusStatus.setText(corpusError);
                }
                corpusStatus.setToolTipText(identity.isEmpty() ? corpusStatus.getText() : identity);
                changed.run();
            }
        }.execute();
    }
}
