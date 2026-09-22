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
    private final JComboBox<TrainingSource.Mode> mode = new JComboBox<>(TrainingSource.Mode.values());
    private final JTextField generator = new JTextField(24);
    private final JButton browse = new JButton("Browse...");
    private final JPanel generatorRow = panel(new BorderLayout(8, 0));
    private final JLabel note = label("", 11, SeedTheme.SECONDARY);
    private final Map<String, TrainingSource> drafts = new HashMap<>();
    private String key = "", error = "";
    private long request;
    private boolean ready, updating, editable = true;
    private final Runnable changed;

    BrnTrainingSourcePanel(TrainingSettings settings, Runnable changed) {
        super(new BorderLayout(0, 8)); setOpaque(false); this.changed = changed;
        mode.setName("brnTrainingSource"); generator.setName("nnueGeneratorStore"); browse.setName("browseNnueGenerator");
        generator.setText(settings.generatorStore());
        JPanel fields = panel(new GridBagLayout());
        TrainingPanel.row(fields, 0, "BRN Training Source", mode);
        generatorRow.add(generator); generatorRow.add(browse, BorderLayout.EAST);
        TrainingPanel.row(fields, 1, "NNUE Generator Store", generatorRow);
        add(fields); add(note, BorderLayout.SOUTH);
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
        setVisible(architecture != NetworkArchitecture.NNUE);
        if (architecture == NetworkArchitecture.NNUE) { ready = true; key = ""; changed.run(); return; }
        ready = false; error = ""; key = ""; refresh();
        if (path.isBlank()) { ready = true; apply(new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, generator.getText())); return; }
        final Path root;
        try { root = Path.of(path).toAbsolutePath().normalize(); }
        catch (RuntimeException invalid) { error = invalid.getMessage(); ready = true; refresh(); return; }
        String selectedKey = architecture + "|" + root;
        var draft = drafts.get(selectedKey);
        if (draft != null) { key = selectedKey; ready = true; apply(draft); return; }
        String fallback = generator.getText();
        new SwingWorker<TrainingSource, Void>() {
            protected TrainingSource doInBackground() throws Exception {
                var stored = CheckpointStore.readTrainingSource(root);
                if (stored.isPresent()) return stored.get();
                boolean fresh = CheckpointInspection.freshRoot(root, architecture.trainingArchitecture());
                return fresh ? new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, fallback) : TrainingSource.SELF_PLAY;
            }
            protected void done() {
                if (ticket != request) return;
                key = selectedKey; ready = true;
                try { apply(get()); }
                catch (Exception invalid) { error = TrainingController.concise(invalid); refresh(); }
            }
        }.execute();
    }
    private void apply(TrainingSource source) {
        updating = true;
        mode.setSelectedItem(source.mode());
        if (source.bootstrap()) generator.setText(source.generatorStore());
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
    boolean bootstrap() { return isVisible() && mode.getSelectedItem() == TrainingSource.Mode.NNUE_BOOTSTRAP; }
    void setEditable(boolean value) { editable = value; refresh(); }
    private void refresh() {
        boolean bootstrap = mode.getSelectedItem() == TrainingSource.Mode.NNUE_BOOTSTRAP;
        mode.setEnabled(editable && ready); generatorRow.setVisible(bootstrap);
        generator.setEnabled(editable && ready && bootstrap); browse.setEnabled(editable && ready && bootstrap);
        note.setText(!ready ? "Reading stored training source..." : !error.isEmpty() ? error : bootstrap
                ? "NNUE Best generates games. BRN learns terminal W/D/L; held-out loss selects Best."
                : "BRN generates games and uses Candidate-vs-Best game validation.");
        changed.run(); revalidate();
    }
}
