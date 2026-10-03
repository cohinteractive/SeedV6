package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/** Campaign source setup. Identity inspection runs off the EDT; no source is converted or deleted. */
final class TrainingDataSourcesPanel extends JPanel {
    private final ArrayList<DataSource> sources = new ArrayList<>();
    private final SourcesModel model = new SourcesModel();
    private final JTable table = new JTable(model);
    private final JButton add = new JButton("Add source..."), remove = new JButton("Remove registration"), relocate = new JButton("Change location..."), inspect = new JButton("Inspect sources");
    private final JTextArea details = new JTextArea(8, 30);
    private final JCheckBox acknowledge = new JCheckBox("Acknowledge a new sequential start at position zero");
    private Path lineage;
    private NetworkArchitecture architecture;
    private boolean editable = true, loading, dirty, legacy, failed;
    private long ticket;
    private final Runnable changed;
    TrainingDataSourcesPanel(Runnable changed) {
        super(new BorderLayout(8, 8)); this.changed = changed; setOpaque(false); setName("trainingDataSources");
        table.setName("trainingDataSourceTable"); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(540, 160));
        var rows = new JScrollPane(table); rows.setColumnHeaderView(table.getTableHeader()); add(rows);
        JPanel actions = new JPanel(new GridLayout(2, 2, 8, 8)); actions.setOpaque(false);
        add.setName("addTrainingDataSource"); remove.setName("removeTrainingDataSource"); inspect.setName("inspectTrainingDataSources");
        actions.add(add); actions.add(remove); actions.add(relocate); actions.add(inspect); add(actions, BorderLayout.NORTH);
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true); details.setName("trainingDataDetails");
        acknowledge.setOpaque(false); acknowledge.setName("acknowledgeTrainingDataMigration");
        JPanel lower = new JPanel(new BorderLayout()); lower.setOpaque(false); lower.add(acknowledge, BorderLayout.NORTH); lower.add(new JScrollPane(details)); add(lower, BorderLayout.SOUTH);
        add.addActionListener(e -> choose(false)); relocate.addActionListener(e -> choose(true));
        remove.addActionListener(e -> { int index = table.getSelectedRow(); if (index >= 0) { sources.remove(index); dirty = true; model.fireTableDataChanged(); changed.run(); } });
        inspect.addActionListener(e -> inspect()); acknowledge.addActionListener(e -> { dirty = true; changed.run(); });
    }
    void load(Path root, NetworkArchitecture architecture) {
        if (root.equals(lineage) && this.architecture == architecture) return;
        lineage = root; this.architecture = architecture; long expected = ++ticket; loading = true; failed = false; dirty = false; sources.clear(); model.fireTableDataChanged(); refresh();
        new SwingWorker<DataSources, Void>() {
            boolean old;
            protected DataSources doInBackground() throws Exception {
                old = Files.exists(root.resolve("corpus-training/campaign.json"));
                Path file = DataSources.directory(root).resolve("sources.json");
                return Files.exists(file) ? DataSources.read(file.getParent()) : null;
            }
            protected void done() {
                if (ticket != expected) return;
                try { var value = get(); if (value != null) sources.addAll(value.sources()); legacy = old; acknowledge.setSelected(value != null && value.legacyProgressAcknowledged());
                    details.setText(legacy ? "Earlier campaigns used a permutation, so consumed positions cannot be inferred as a sequential prefix. Existing data and history are preserved. Acknowledging a new sequential start permits overlap with earlier usage."
                            : "Sequential source order. Weights allocate each generation deterministically; exhausted sources stop the generation. Add Lichess JSONL/PZstandard files or legacy Seed data directories. Stockfish labels in Lichess exports are supported; other formats need a reader.");
                } catch (Exception failure) { failed = true; details.setText("Cannot load Training Data: " + TrainingController.concise(failure)); }
                loading = false; model.fireTableDataChanged(); refresh(); changed.run();
            }
        }.execute();
    }
    private void choose(boolean moving) {
        int selected = table.getSelectedRow(); if (moving && selected < 0) return;
        JFileChooser chooser = new JFileChooser(); chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setDialogTitle(moving ? "Locate the same Training Data source version" : "Add Training Data source (no conversion)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath(); DataSource old = moving ? sources.get(selected) : null;
        var identities = sources.stream().map(DataSource::identity).toList();
        long expected = ticket; loading = true; refresh();
        new SwingWorker<DataSource, Void>() {
            protected DataSource doInBackground() throws Exception {
                DataSource value = DataSource.register(old == null ? path.getFileName().toString() : old.name(), path, old == null ? 1 : old.weight());
                if (old != null && !old.identity().equals(value.identity())) throw new java.io.IOException("Source identity mismatch. Relocation must preserve bytes, length and modification timestamp. Existing cursor was preserved.");
                if (old == null && identities.contains(value.identity())) throw new java.io.IOException("This source version is already registered");
                return value;
            }
            protected void done() {
                if (ticket != expected) return;
                try { DataSource value = get(); if (moving) sources.set(selected, value); else sources.add(value); dirty = true; model.fireTableDataChanged(); details.setText("Ready: " + value.name() + "\nVersion fingerprint: " + value.identity() + "\n" + value.location() + "\nPosition count: unknown until reached. Seek metadata builds only as records are consumed."); }
                catch (Exception failure) { details.setText(TrainingController.concise(failure)); }
                loading = false; refresh(); changed.run();
            }
        }.execute();
    }
    private void inspect() {
        var snapshot = java.util.List.copyOf(sources); Path root = lineage; long expected = ticket; loading = true; refresh();
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                var ledger = new SourceLedger(root); var text = new StringBuilder();
                for (var source : snapshot) {
                    text.append(source.name()).append(" | ").append(source.format()).append("\n").append(source.location()).append("\nIdentity: ").append(source.identity());
                    try { source.verify(); text.append("\nReady"); } catch (Exception invalid) { text.append("\nBLOCKED: ").append(invalid.getMessage()); }
                    text.append(" | next unallocated position: ").append(ledger.next(source.identity()));
                    Path index = DataSources.directory(root).resolve("seek").resolve(source.identity() + ".json");
                    if (Files.exists(index)) { var nav = DataFiles.read(index, SourceReaders.Index.class); long known = nav.count() >= 0 ? nav.count() : source.knownPositions(); text.append(" | seek points: ").append(nav.points().size()).append(" | positions: ").append(known < 0 ? "unknown" : known); }
                    else { long known = source.knownPositions(); text.append(" | seek metadata: lazy | positions: ").append(known < 0 ? "unknown" : known); }
                    text.append("\n\n");
                }
                for (var reservation : ledger.state().reservations()) text.append("Generation ").append(reservation.generation()).append(" ").append(reservation.status()).append(" ").append(reservation.ranges()).append(" ").append(reservation.reason()).append("\n");
                return text.toString();
            }
            protected void done() { if (ticket != expected) return; try { details.setText(get()); } catch (Exception e) { details.setText(TrainingController.concise(e)); } loading = false; refresh(); changed.run(); }
        }.execute();
    }
    boolean ready() { return !loading && !failed && !sources.isEmpty() && (!legacy || acknowledge.isSelected()); }
    String summary() { return sources.stream().map(s -> s.name() + " (" + s.weight() + ")").collect(java.util.stream.Collectors.joining(", ")); }
    void save() throws java.io.IOException {
        if (!ready()) throw new java.io.IOException("Add Training Data sources and resolve migration acknowledgement first");
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) throw new java.io.IOException("Finish editing source weights");
        if (dirty || !Files.exists(DataSources.directory(lineage).resolve("sources.json"))) {
            try (var lock = new CheckpointStore(lineage, architecture.trainingArchitecture())) {
                new DataSources(1, sources, acknowledge.isSelected()).save(DataSources.directory(lineage));
            }
            dirty = false;
        }
    }
    void setEditable(boolean enabled) { editable = enabled; refresh(); }
    private void refresh() {
        for (var button : java.util.List.of(add, remove, relocate)) button.setEnabled(editable && !loading && !failed && lineage != null);
        inspect.setEnabled(!loading && lineage != null); table.setEnabled(editable && !loading); acknowledge.setEnabled(editable && !loading);
        acknowledge.setVisible(legacy);
    }
    private final class SourcesModel extends AbstractTableModel {
        private final String[] columns = {"Data Source", "Format", "Weight", "Version"};
        public int getRowCount() { return sources.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int column) { return columns[column]; }
        public Object getValueAt(int row, int column) { var s = sources.get(row); return switch (column) { case 0 -> s.name(); case 1 -> s.format(); case 2 -> s.weight(); default -> s.identity().substring(0, 12); }; }
        public boolean isCellEditable(int row, int column) { return editable && !loading && (column == 0 || column == 2); }
        public void setValueAt(Object value, int row, int column) {
            try { var s = sources.get(row); sources.set(row, new DataSource(column == 0 ? value.toString() : s.name(), s.format(), s.location(), s.identity(), column == 2 ? Integer.parseInt(value.toString()) : s.weight())); dirty = true; fireTableRowsUpdated(row, row); changed.run(); }
            catch (RuntimeException invalid) { details.setText("Use a nonempty name and an integer weight from 1 to 1000000."); }
        }
    }
}
