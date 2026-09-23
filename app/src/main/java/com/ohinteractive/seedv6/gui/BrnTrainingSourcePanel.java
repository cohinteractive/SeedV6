package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.service.TrainingSource;
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
    private final Map<String, TrainingSource> drafts = new HashMap<>();
    private String key = "", error = "";
    private long request;
    private boolean ready, updating, editable = true, locked;
    private NetworkArchitecture architecture;
    private record Selection(TrainingSource source, boolean locked) {}
    private final Runnable changed;

    BrnTrainingSourcePanel(TrainingSettings settings, Runnable changed) {
        super(new BorderLayout(0, 8)); setOpaque(false); this.changed = changed;
        mode.setName("brnTrainingSource"); generator.setName("nnueGeneratorStore"); browse.setName("browseNnueGenerator");
        generator.setText(settings.generatorStore());
        JPanel fields = panel(new GridBagLayout());
        TrainingPanel.row(fields, 0, "Position generation", mode); sourceLabel = (JLabel) fields.getComponent(0);
        generatorRow.add(generator); generatorRow.add(browse, BorderLayout.EAST);
        TrainingPanel.row(generatorFields, 0, "NNUE Generator Store", generatorRow);
        JPanel selection = panel(new BorderLayout()); selection.add(fields, BorderLayout.NORTH); selection.add(generatorFields);
        add(selection); add(note, BorderLayout.SOUTH);
        if (settings.source() != null) drafts.put(settings.architecture() + "|" + settings.root(), settings.source());
        mode.addActionListener(e -> { if (!updating) { remember(); refresh(); } });
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
        try { drafts.put(key, selection()); } catch (RuntimeException invalidDraft) { /* Read validates before Start. */ }
    }
    void selectRoot(String path, NetworkArchitecture architecture) {
        long ticket = ++request;
        this.architecture = architecture; locked = false;
        sourceLabel.setText(architecture == NetworkArchitecture.BRN2 ? "Position generation" : "BRN Training Source");
        updating = true; mode.removeAllItems();
        if (architecture == NetworkArchitecture.BRN2) mode.addItem(TrainingSource.Mode.HANDCRAFTED);
        mode.addItem(TrainingSource.Mode.NNUE_BOOTSTRAP);
        if (architecture != NetworkArchitecture.BRN2) mode.addItem(TrainingSource.Mode.SELF_PLAY);
        mode.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, architecture == NetworkArchitecture.BRN2 && value == TrainingSource.Mode.NNUE_BOOTSTRAP ? "NNUE" : value, index, selected, focus);
            }
        });
        updating = false;
        setVisible(architecture != NetworkArchitecture.NNUE);
        if (architecture == NetworkArchitecture.NNUE) { ready = true; key = ""; changed.run(); return; }
        ready = false; error = ""; key = ""; refresh();
        if (path.isBlank()) { ready = true; apply(defaultSource(generator.getText())); return; }
        final Path root;
        try { root = Path.of(path).toAbsolutePath().normalize(); }
        catch (RuntimeException invalid) { error = invalid.getMessage(); ready = true; refresh(); return; }
        String selectedKey = architecture + "|" + root;
        var draft = drafts.get(selectedKey);
        if (architecture != NetworkArchitecture.BRN2 && draft != null) { key = selectedKey; ready = true; apply(draft); return; }
        String fallback = generator.getText();
        new SwingWorker<Selection, Void>() {
            protected Selection doInBackground() throws Exception {
                var stored = CheckpointStore.readTrainingSource(root);
                boolean fresh = CheckpointInspection.freshRoot(root, architecture.trainingArchitecture());
                boolean lock = architecture == NetworkArchitecture.BRN2 && (!fresh || stored.isPresent());
                return new Selection(stored.orElse(fresh ? draft == null ? defaultSource(fallback) : draft : TrainingSource.SELF_PLAY), lock);
            }
            protected void done() {
                if (ticket != request) return;
                key = selectedKey; ready = true;
                try { var selection = get(); locked = selection.locked(); apply(selection.source()); }
                catch (Exception invalid) { error = TrainingController.concise(invalid); refresh(); }
            }
        }.execute();
    }
    private TrainingSource defaultSource(String fallback) {
        return architecture == NetworkArchitecture.BRN2 ? TrainingSource.HANDCRAFTED : new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, fallback);
    }
    private void apply(TrainingSource source) {
        if (!locked && architecture == NetworkArchitecture.BRN2 && source.mode() == TrainingSource.Mode.SELF_PLAY) source = TrainingSource.HANDCRAFTED;
        updating = true;
        if ((source.mode() == TrainingSource.Mode.SELF_PLAY || source.frozen()) && architecture == NetworkArchitecture.BRN2) mode.addItem(source.mode()); // Existing stores only.
        mode.setSelectedItem(source.mode());
        if (source.nnue()) generator.setText(source.generatorStore());
        updating = false; refresh();
    }
    TrainingSource read() {
        // Apply may precede the asynchronous read; the backend resolves null under startup inspection.
        if (!ready) return null;
        if (!error.isEmpty()) throw new IllegalArgumentException(error);
        return selection();
    }
    private TrainingSource selection() { return new TrainingSource((TrainingSource.Mode) mode.getSelectedItem(), generator.getText()); }
    String generatorStore() { return generator.getText().trim(); }
    boolean ready() { return ready; }
    boolean bootstrap() { return isVisible() && mode.getSelectedItem() != TrainingSource.Mode.SELF_PLAY; }
    void setEditable(boolean value) { editable = value; refresh(); }
    private void refresh() {
        boolean bootstrap = mode.getSelectedItem() == TrainingSource.Mode.NNUE_BOOTSTRAP;
        mode.setEnabled(editable && ready && !locked); generatorFields.setVisible(bootstrap);
        generator.setEnabled(editable && ready && !locked && bootstrap); browse.setEnabled(editable && ready && !locked && bootstrap);
        note.setText(!ready ? "Reading stored training source..." : !error.isEmpty() ? error : bootstrap
                ? "NNUE Best generates games. Configured BRN supervision and held-out loss select Best."
                : mode.getSelectedItem() == TrainingSource.Mode.FROZEN_REPLAY ? "Frozen data replay. Use the frozen-wdl command to Start or Resume."
                : mode.getSelectedItem() == TrainingSource.Mode.HANDCRAFTED ? "Handcrafted search generates positions. Supervision independently selects targets."
                : "BRN generates games and uses Candidate-vs-Best game validation.");
        changed.run(); revalidate();
    }
}
