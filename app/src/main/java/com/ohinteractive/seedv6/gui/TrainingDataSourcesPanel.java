package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.training.service.CorpusTraining;
import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/** Campaign source setup. Detection and application-managed BINP preparation run off the EDT. */
final class TrainingDataSourcesPanel extends JPanel {
    private final ArrayList<DataSource> sources = new ArrayList<>();
    private final SourcesModel model = new SourcesModel();
    private final JTable table = new JTable(model);
    private final JButton add = new JButton("Add from library..."), remove = new JButton("Remove from mix"), relocate = new JButton("Change location..."), inspect = new JButton("Inspect sources");
    private final JButton prepare = new JButton("Prepare / retry");
    private final java.util.Map<String, String> statuses = new java.util.HashMap<>();
    private final java.util.Set<String> preparing = new java.util.HashSet<>();
    private final JTextArea details = new JTextArea(8, 30);
    private final JCheckBox acknowledge = new JCheckBox("Acknowledge a new sequential start at position zero");
    private Path lineage;
    private NetworkArchitecture architecture;
    private boolean editable = true, loading, persisting, dirty, legacy, failed;
    private long ticket;
    private final Runnable changed;
    private final TrainingFolders folders;
    TrainingDataSourcesPanel(Runnable changed) { this(null, changed); }
    TrainingDataSourcesPanel(TrainingFolders folders, Runnable changed) {
        super(new BorderLayout(8, 8)); this.changed = changed; this.folders = folders; setOpaque(false); setName("trainingDataSources");
        table.setName("trainingDataSourceTable"); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {140, 230, 60, 110, 195, 190};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setPreferredScrollableViewportSize(new Dimension(540, 160));
        var rows = new JScrollPane(table); rows.setColumnHeaderView(table.getTableHeader()); add(rows);
        JPanel actions = new JPanel(new GridLayout(0, 2, 8, 8)); actions.setOpaque(false);
        add.setName("addTrainingDataSource"); remove.setName("removeTrainingDataSource"); inspect.setName("inspectTrainingDataSources");
        actions.add(add); actions.add(remove); actions.add(relocate); actions.add(inspect); add(actions, BorderLayout.NORTH);
        prepare.setName("prepareTrainingDataSource"); actions.add(prepare);
        prepare.addActionListener(e -> { int index = table.getSelectedRow(); if (index >= 0) startPreparation(sources.get(index), true); });
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true); details.setName("trainingDataDetails");
        acknowledge.setOpaque(false); acknowledge.setName("acknowledgeTrainingDataMigration");
        JPanel lower = new JPanel(new BorderLayout()); lower.setOpaque(false); lower.add(acknowledge, BorderLayout.NORTH); lower.add(new JScrollPane(details)); add(lower, BorderLayout.SOUTH);
        add.addActionListener(e -> chooseLibrary());
        relocate.addActionListener(e -> relocate());
        remove.addActionListener(e -> { int index = table.getSelectedRow(); if (index >= 0) { sources.remove(index); dirty = true; model.fireTableDataChanged(); changed.run(); } });
        inspect.addActionListener(e -> inspect()); acknowledge.addActionListener(e -> { dirty = true; changed.run(); });
    }
    void load(Path root, NetworkArchitecture architecture) {
        if (root.equals(lineage) && this.architecture == architecture) return;
        lineage = root; this.architecture = architecture; long expected = ++ticket; loading = true; persisting = false; failed = false; dirty = false; sources.clear(); statuses.clear(); model.fireTableDataChanged(); refresh();
        var library = library();
        new SwingWorker<DataSources, Void>() {
            boolean old;
            String catalogWarning = "";
            final java.util.Map<String, String> checked = new java.util.HashMap<>();
            protected DataSources doInBackground() throws Exception {
                old = Files.exists(root.resolve("corpus-training/campaign.json"));
                Path file = DataSources.directory(root).resolve("sources.json");
                DataSources value = Files.exists(file) ? DataSources.read(file.getParent()) : null;
                if (value != null) for (var source : value.sources()) {
                    checked.put(source.identity(), status(source));
                    try { library.remember(source); }
                    catch (java.io.IOException unavailable) { catalogWarning = "\nLibrary registration unavailable: " + unavailable.getMessage(); }
                }
                return value;
            }
            protected void done() {
                if (ticket != expected) return;
                try { var value = get(); if (value != null) sources.addAll(value.sources()); statuses.putAll(checked); legacy = old; acknowledge.setSelected(value != null && value.legacyProgressAcknowledged());
                    details.setText(legacy ? "Earlier campaigns used a permutation, so consumed positions cannot be inferred as a sequential prefix. Existing data and history are preserved. Acknowledging a new sequential start permits overlap with earlier usage."
                            : "Select registered Training Data with Add from library. Weights allocate accepted examples deterministically. Removing a source from this mix preserves its library registration and previous consumption history.");
                    details.append(catalogWarning);
                } catch (Exception failure) { failed = true; details.setText("Cannot load Training Data: " + TrainingController.concise(failure)); }
                loading = false; model.fireTableDataChanged(); refresh(); changed.run();
            }
        }.execute();
    }
    private TrainingDataLibrary library() { return new TrainingDataLibrary(folders == null ? lineage.resolveSibling("networks") : folders.base()); }
    private void chooseLibrary() {
        var picker = new TrainingDataSelector("trainingLibrarySource", () -> folders == null ? lineage.resolveSibling("networks") : folders.base(),
                () -> java.util.List.of(architecture.trainingArchitecture()), () -> {});
        while (JOptionPane.showConfirmDialog(this, picker, "Add Training Data to this lineage's mix", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            if (!picker.ready()) { JOptionPane.showMessageDialog(this, "Select a ready, compatible source. Register and prepare it here if needed."); continue; }
            var value = picker.selected();
            if (sources.stream().anyMatch(s -> s.identity().equals(value.identity()))) { details.setText("This source version is already in the mix."); return; }
            acceptSource(value, -1); changed.run(); return;
        }
    }
    private void relocate() {
        int selected = table.getSelectedRow(); if (selected < 0) return;
        DataSource old = sources.get(selected);
        var selection = FilePickers.choose(this, FilePickers.Purpose.RELOCATE_TRAINING_DATA,
                "Locate the same Training Data source version", old.location(), FilePickers.Kind.FILE_OR_DIRECTORY);
        if (selection.isEmpty()) return;
        Path path = selection.get(); var library = library();
        long expected = ticket; loading = true; refresh();
        new SwingWorker<DataSource, Void>() {
            protected DataSource doInBackground() throws Exception {
                var value = DataSource.register(old.name(), path, old.weight(), old.labelProfile());
                if (!old.identity().equals(value.identity())) throw new java.io.IOException("Source identity mismatch. Relocation must preserve bytes, length and modification timestamp. Existing cursor was preserved.");
                library.register(value); return value;
            }
            protected void done() {
                if (ticket != expected) return;
                try { acceptSource(get(), selected); }
                catch (Exception failure) { details.setText(TrainingController.concise(failure)); }
                finally { loading = false; refresh(); changed.run(); }
            }
        }.execute();
    }
    /** EDT seam shared by the chooser and component tests; callers have explicitly selected the profile. */
    void acceptSource(DataSource value, int replacing) {
        if (replacing >= 0) sources.set(replacing, value); else sources.add(value);
        dirty = true;
        statuses.put(value.identity(), value.format() == DataSource.Format.STOCKFISH_BINPACK_ZSTD ? "NOT READY" : "READY");
        model.fireTableDataChanged();
        if (value.format() == DataSource.Format.STOCKFISH_BINPACK_ZSTD) startPreparation(value);
        else details.setText("READY: " + value.name() + "\nVersion fingerprint: " + value.identity() + "\n" + value.location());
    }
    private static String status(DataSource source) {
        try { source.requireReady(); return "READY"; }
        catch (Exception invalid) { return "NOT READY: " + invalid.getMessage(); }
    }
    void startPreparation(DataSource source) {
        startPreparation(source, false);
    }
    private void startPreparation(DataSource source, boolean retry) {
        if (source.format() != DataSource.Format.STOCKFISH_BINPACK_ZSTD || !preparing.add(source.identity())) return;
        long expected = ticket; Path root = lineage; var trainingArchitecture = architecture.trainingArchitecture();
        var selection = new DataSources(1, sources, acknowledge.isSelected());
        persisting = true; statuses.put(source.identity(), "PREPARING"); model.fireTableDataChanged(); refresh(); changed.run();
        new SwingWorker<String, CorpusPreparation.Progress>() {
            protected String doInBackground() throws Exception {
                // Registration survives process interruption during the potentially long preparation.
                try (var lock = new CheckpointStore(root, trainingArchitecture)) { selection.save(DataSources.directory(root)); }
                finally { SwingUtilities.invokeLater(() -> { if (ticket == expected) { persisting = false; refresh(); changed.run(); } }); }
                var manifest = PreparedBinpack.prepare(source, new CorpusPreparation(this::isCancelled, p -> publish(p)), retry);
                return "READY: " + source.name() + " | " + manifest.count() + " raw positions, " + manifest.units().size()
                        + " restartable chunks\n" + PreparedBinpack.directory(source);
            }
            protected void process(java.util.List<CorpusPreparation.Progress> updates) {
                if (ticket != expected) return;
                var p = updates.getLast();
                statuses.put(source.identity(), "PREPARING " + (p.total() > 0 ? String.format(java.util.Locale.ROOT, "%.1f%%", 100.0 * p.completed() / p.total()) : ""));
                details.setText(p.stage() + "\n" + p.completed() + " / " + p.total() + " (compressed source bytes, or units when checking a cache)\nOriginals remain unchanged. Large preparation may take considerable time; after interruption use Prepare / retry.");
                model.fireTableDataChanged();
            }
            protected void done() {
                preparing.remove(source.identity());
                if (sources.stream().noneMatch(s -> s.identity().equals(source.identity()))) return;
                try { details.setText(get()); statuses.put(source.identity(), "READY"); }
                catch (Exception failure) { String error = TrainingController.concise(failure); statuses.put(source.identity(), "FAILED"); details.setText("Preparation failed: " + error + "\nUse Prepare / retry. If original shards changed, remove this registration and Add source again to create a new version."); }
                model.fireTableDataChanged(); refresh(); changed.run();
            }
        }.execute();
    }
    private void inspect() {
        var snapshot = java.util.List.copyOf(sources); Path root = lineage; long expected = ticket; loading = true; refresh();
        new SwingWorker<String, Void>() {
            final java.util.Map<String, String> checked = new java.util.HashMap<>();
            protected String doInBackground() throws Exception {
                var ledger = new SourceLedger(root); var text = new StringBuilder();
                for (var source : snapshot) {
                    text.append(source.name()).append(" | ").append(source.format()).append(" | ").append(source.labelProfile()).append("\n").append(source.location()).append("\nIdentity: ").append(source.identity());
                    String state = status(source); checked.put(source.identity(), state); text.append("\n").append(state);
                    if (source.format() == DataSource.Format.STOCKFISH_BINPACK_ZSTD) {
                        text.append("\nShards: ").append(source.shards()).append("\nPrepared: ").append(PreparedBinpack.directory(source));
                        if (state.equals("READY")) {
                            var manifest = PreparedBinpack.ready(source);
                            text.append("\nRaw positions: ").append(manifest.count()).append(" | restartable chunks: ").append(manifest.units().size());
                        }
                    }
                    text.append(" | next unallocated position: ").append(ledger.next(source.identity()));
                    Path index = DataSources.directory(root).resolve("seek").resolve(source.identity() + ".json");
                    if (Files.exists(index)) { var nav = DataFiles.read(index, SourceReaders.Index.class); long known = nav.count() >= 0 ? nav.count() : source.knownPositions(); text.append(" | seek points: ").append(nav.points().size()).append(" | positions: ").append(known < 0 ? "unknown" : known); }
                    else if (source.format() != DataSource.Format.STOCKFISH_BINPACK_ZSTD) { long known = source.knownPositions(); text.append(" | seek metadata: lazy | positions: ").append(known < 0 ? "unknown" : known); }
                    text.append("\n\n");
                }
                for (var reservation : ledger.state().reservations()) text.append("Generation ").append(reservation.generation()).append(" ").append(reservation.status()).append(" ").append(reservation.ranges()).append(" ").append(reservation.reason()).append("\n");
                return text.toString();
            }
            protected void done() { if (ticket != expected) return; try { details.setText(get()); checked.forEach((id, s) -> { if (!preparing.contains(id)) statuses.put(id, s); }); model.fireTableDataChanged(); } catch (Exception e) { details.setText(TrainingController.concise(e)); } loading = false; refresh(); changed.run(); }
        }.execute();
    }
    boolean ready() { return !loading && !persisting && !failed && !sources.isEmpty() && (!legacy || acknowledge.isSelected())
            && sources.stream().allMatch(s -> !preparing.contains(s.identity()) && "READY".equals(statuses.get(s.identity())) && compatible(s)); }
    private boolean compatible(DataSource source) {
        return incompatibility(source) == null;
    }
    private String incompatibility(DataSource source) {
        try { CorpusTraining.targetPolicy(architecture.trainingArchitecture(), source.labelProfile()); return null; }
        catch (IllegalArgumentException unsupported) { return unsupported.getMessage(); }
    }
    boolean sourceSpecificTargets() { return sources.stream().anyMatch(s -> s.labelProfile() == LabelProfile.BT4_Q_V1); }
    String summary() { return sources.stream().map(s -> s.name() + " (" + s.weight() + ")").collect(java.util.stream.Collectors.joining(", ")); }
    void save() throws java.io.IOException {
        if (!ready()) throw new java.io.IOException("Training Data is not ready. Inspect source status, use Prepare / retry for BINP, and resolve compatibility or migration acknowledgement.");
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
        for (var button : java.util.List.of(add, remove, relocate)) button.setEnabled(editable && !loading && !persisting && !failed && lineage != null);
        inspect.setEnabled(!loading && lineage != null); table.setEnabled(editable && !loading && !persisting); acknowledge.setEnabled(editable && !loading && !persisting);
        prepare.setEnabled(editable && !loading && !persisting && lineage != null);
        acknowledge.setVisible(legacy);
    }
    private final class SourcesModel extends AbstractTableModel {
        private final String[] columns = {"Data Source", "Format", "Weight", "Version", "Label profile", "Status"};
        public int getRowCount() { return sources.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int column) { return columns[column]; }
        public Object getValueAt(int row, int column) { var s = sources.get(row); return switch (column) { case 0 -> s.name(); case 1 -> s.format(); case 2 -> s.weight(); case 3 -> s.identity().substring(0, 12); case 4 -> s.labelProfile(); default -> compatible(s) ? preparing.contains(s.identity()) && !statuses.getOrDefault(s.identity(), "").startsWith("PREPARING")
                ? "PREPARING" : statuses.getOrDefault(s.identity(), "NOT READY") : "UNSUPPORTED: " + incompatibility(s); }; }
        public boolean isCellEditable(int row, int column) { return editable && !loading && !persisting && (column == 0 || column == 2); }
        public void setValueAt(Object value, int row, int column) {
            try { var s = sources.get(row); sources.set(row, s.withDisplay(column == 0 ? value.toString() : s.name(), column == 2 ? Integer.parseInt(value.toString()) : s.weight())); dirty = true; fireTableRowsUpdated(row, row); changed.run(); }
            catch (RuntimeException invalid) { details.setText("Use a nonempty name and an integer weight from 1 to 1000000."); }
        }
    }
}
