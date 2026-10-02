package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** Corpus management is independent of model/run configuration and pinned training views. */
final class CorpusImportPanel extends JPanel implements Scrollable {
    private final JTextField archive = new JTextField(32), root = new JTextField(32);
    private final JButton sourceBrowse = new JButton("Browse..."), rootBrowse = new JButton("Browse...");
    private final JRadioButton all = new JRadioButton("All available source records"), bounded = new JRadioButton("Up to");
    private final JSpinner limit;
    private final JButton start = new JButton("Start Import"), stop = new JButton("Stop Import"), validate = new JButton("Validate Corpus");
    private final JTextArea status = text("Select an archive and corpus root.", 12, SeedTheme.TEXT);
    private final JProgressBar activity = new JProgressBar();
    private final TrainingFolders folders;
    private final CorpusImportController controller;
    private boolean closing;

    CorpusImportPanel(TrainingFolders folders, Consumer<String> corpusChanged) {
        this(folders, corpusChanged, new CorpusImportController.ProductionBackend());
    }
    CorpusImportPanel(TrainingFolders folders, Consumer<String> corpusChanged, CorpusImportController.Backend backend) {
        super(new BorderLayout(0, 12)); setOpaque(false); this.folders = folders;
        controller = new CorpusImportController(backend, this::showState, path -> {
            if (controllerStateValid()) folders.rememberCorpusRoot(path);
            corpusChanged.accept(path);
        });
        archive.setText(folders.lastCorpusArchive()); root.setText(folders.lastCorpusRoot());
        all.setSelected(folders.corpusImportAll()); bounded.setSelected(!all.isSelected());
        var group = new ButtonGroup(); group.add(all); group.add(bounded);
        all.setOpaque(false); bounded.setOpaque(false);
        limit = new JSpinner(new SpinnerNumberModel(Long.valueOf(folders.corpusImportLimit()),
                Long.valueOf(1), Long.valueOf(Long.MAX_VALUE), Long.valueOf(1)));
        limit.setEditor(new JSpinner.NumberEditor(limit, "0")); limit.setEnabled(!all.isSelected());
        archive.setName("corpusImportArchive"); root.setName("corpusImportRoot"); all.setName("corpusImportAll");
        bounded.setName("corpusImportBounded"); limit.setName("corpusImportLimit"); start.setName("startCorpusImport");
        stop.setName("stopCorpusImport"); validate.setName("validateCorpus"); status.setName("corpusImportStatus");
        JPanel fields = panel(new GridBagLayout());
        TrainingPanel.row(fields, 0, "Source archive", entry(archive, sourceBrowse));
        TrainingPanel.row(fields, 1, "Seed corpus root", entry(root, rootBrowse));
        // Keep the full long-count editor usable without adding all three controls' widths.
        JPanel amount = panel(new GridBagLayout());
        GridBagConstraints choice = new GridBagConstraints(); choice.anchor = GridBagConstraints.WEST;
        choice.gridx = 0; choice.gridy = 0; choice.gridwidth = 2; choice.weightx = 1; amount.add(all, choice);
        choice.gridy = 1; choice.gridwidth = 1; choice.weightx = 0;
        choice.insets = new Insets(SeedTheme.scale(6), 0, 0, SeedTheme.scale(8)); amount.add(bounded, choice);
        choice.gridx = 1; choice.weightx = 1; choice.insets = new Insets(SeedTheme.scale(6), 0, 0, 0); amount.add(limit, choice);
        TrainingPanel.row(fields, 2, "Import amount", amount);
        JPanel top = panel(new BorderLayout(0, 12)); top.add(label("Corpus Import / Management", 18, SeedTheme.TEXT), BorderLayout.NORTH);
        top.add(fields); top.add(text("Expand an existing Seed corpus or choose an empty directory. Previously imported positions are deduplicated automatically.\n"
                + "All rescans the archive from the start and may take a long time. Existing training views stay pinned.", 12, SeedTheme.SECONDARY), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        JPanel results = panel(new BorderLayout(0, 8)); results.add(activity, BorderLayout.NORTH); results.add(status, BorderLayout.CENTER); add(results);
        JPanel actions = panel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.add(start); actions.add(stop); actions.add(validate); add(actions, BorderLayout.SOUTH);
        stop.setEnabled(false); activity.setVisible(false);
        sourceBrowse.addActionListener(event -> {
            var chooser = new JFileChooser(archive.getText()); chooser.setDialogTitle("Select Lichess evaluated positions (.jsonl.zst)");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY); chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(new FileNameExtensionFilter("Lichess JSONL Zstandard archive (*.jsonl.zst)", "zst"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                archive.setText(chooser.getSelectedFile().toString()); remember();
            }
        });
        rootBrowse.addActionListener(event -> {
            var chooser = new JFileChooser(root.getText()); chooser.setDialogTitle("Select Seed corpus root or empty directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) chooseRoot(chooser.getSelectedFile().toString());
        });
        all.addActionListener(event -> { limit.setEnabled(!all.isSelected()); remember(); });
        bounded.addActionListener(event -> { limit.setEnabled(!all.isSelected()); remember(); });
        limit.addChangeListener(event -> remember());
        archive.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { remember(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { remember(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { remember(); }
        });
        start.addActionListener(event -> {
            try {
                if (!all.isSelected()) {
                    String entered = ((JSpinner.DefaultEditor) limit.getEditor()).getTextField().getText().trim();
                    if (Long.parseLong(entered) < 1) throw new IllegalArgumentException();
                    limit.commitEdit();
                }
            } catch (Exception invalid) {
                status.setText("Bounded import count must be a positive integer up to " + Long.MAX_VALUE + "."); return;
            }
            try {
                remember();
                controller.start(new CorpusImportController.Request(archive.getText().trim(), root.getText().trim(),
                        all.isSelected(), ((Number) limit.getValue()).longValue()));
            } catch (RuntimeException invalid) { status.setText(TrainingController.concise(invalid)); }
        });
        stop.addActionListener(event -> controller.stop()); validate.addActionListener(event -> controller.validate(root.getText().trim()));
    }
    private boolean controllerStateValid() {
        var phase = controller.state().phase();
        return phase == CorpusImportController.Phase.COMPLETE || phase == CorpusImportController.Phase.STOPPED || phase == CorpusImportController.Phase.VALID;
    }
    private static JPanel entry(JTextField field, JButton browse) {
        JPanel row = panel(new BorderLayout(8, 0)); row.add(field); row.add(browse, BorderLayout.EAST); return row;
    }
    private void remember() { folders.rememberCorpusImport(archive.getText().trim(), all.isSelected(), ((Number) limit.getValue()).longValue()); }
    void selectRoot(String path) {
        if (!controller.state().active()) root.setText(path.isBlank() ? folders.lastCorpusRoot() : path);
    }
    void chooseRoot(String path) {
        if (controller.state().active()) return;
        root.setText(path); folders.rememberCorpusRoot(path);
    }
    private void showState(CorpusImportController.State state) {
        boolean active = state.active(), editable = !active && !closing;
        for (JComponent control : List.of(archive, root, sourceBrowse, rootBrowse, all, bounded, start, validate)) control.setEnabled(editable);
        limit.setEnabled(editable && !all.isSelected()); stop.setEnabled(!closing && state.phase() == CorpusImportController.Phase.IMPORTING);
        activity.setVisible(active); activity.setIndeterminate(active);
        String details = state.message();
        var progress = state.summary() == null ? state.progress() : new com.ohinteractive.seedv6.corpus.lichess.LichessImporter.Progress(
                state.summary().stats(), state.summary().corpusTotal(), state.summary().elapsedSeconds());
        if (progress != null) {
            var stats = progress.stats();
            details += String.format(java.util.Locale.ROOT,
                    "\nSource records examined: %,d\nAccepted: %,d   Rejected: %,d\nPositions added: %,d   Duplicates: %,d   Labels upgraded: %,d"
                    + "\nPersisted records: %,d   Completed shards: %,d\nCommitted corpus total: %,d\nElapsed: %.1f s   Throughput: %,.0f records/s",
                    stats.read(), stats.accepted(), stats.rejected(), stats.added(), stats.duplicates(), stats.upgraded(),
                    stats.persisted(), stats.completedShards(), progress.corpusTotal(), progress.elapsedSeconds(),
                    stats.read() / Math.max(.001, progress.elapsedSeconds()));
        }
        if (state.integrity() != null) details += "\nPositions: " + state.integrity().positions() + "   Stored records: "
                + state.integrity().storedRecords() + "   Shards: " + state.integrity().shards();
        status.setText(details); revalidate();
    }
    Runnable beginShutdown() {
        closing = true; var cleanup = controller.beginShutdown(); showState(controller.state()); return cleanup;
    }
    // Match Configuration/Dashboard: the viewport supplies width, while content can scroll vertically.
    @Override public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        // Wrapped text's previous layout width must not become the viewport's minimum width.
        size.width = 0; return size;
    }
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return SeedTheme.scale(24); }
    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(1, r.height - SeedTheme.scale(24)); }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
}
