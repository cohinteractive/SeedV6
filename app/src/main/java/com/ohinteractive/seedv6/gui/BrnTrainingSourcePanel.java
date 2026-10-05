package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import javax.swing.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.data.DataSources;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** Shared provider and campaign setup, independent of architecture configuration cards. */
final class BrnTrainingSourcePanel extends JPanel implements Scrollable {
    private final JComboBox<TrainingSource.Mode> mode = new JComboBox<>();
    private final JTextField generator = new JTextField(24);
    private final JPanel generatorFields = panel(new BorderLayout(8, 0));
    private final JSpinner positions = new JSpinner(new SpinnerNumberModel(10000, 2, Integer.MAX_VALUE, 1));
    private final JTextArea note = text("", 11, SeedTheme.SECONDARY);
    private final TrainingDataSourcesPanel sources;
    private final Runnable changed;
    private Path root;
    private NetworkArchitecture architecture;
    private TrainingSource legacy;
    private CorpusTrainingConfig legacyConfig;
    private boolean editable = true, updating;
    BrnTrainingSourcePanel(TrainingSettings settings, Runnable changed) { this(settings, new TrainingFolders(settings), changed); }
    BrnTrainingSourcePanel(TrainingSettings settings, TrainingFolders folders, Runnable changed) { this(settings, folders, changed, () -> {}); }
    BrnTrainingSourcePanel(TrainingSettings settings, TrainingFolders folders, Runnable changed, Runnable ignored) {
        super(new BorderLayout(8, 8)); setOpaque(false); this.changed = changed;
        mode.setName("brnTrainingSource"); generator.setName("nnueGeneratorStore"); positions.setName("brnCorpusPositions");
        mode.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == TrainingSource.Mode.NNUE_BOOTSTRAP ? "Bootstrap with legacy NNUE" : value, index, selected, focus);
            }
        });
        positions.setEditor(new JSpinner.NumberEditor(positions, "0"));
        sources = new TrainingDataSourcesPanel(changed);
        JPanel selection = panel(new GridBagLayout()); TrainingPanel.row(selection, 0, "Position provider", mode);
        JButton browse = new JButton("Browse..."); browse.setName("browseNnueGenerator"); generatorFields.add(generator); generatorFields.add(browse, BorderLayout.EAST);
        TrainingPanel.row(selection, 1, "Legacy NNUE generator store", generatorFields);
        add(selection, BorderLayout.NORTH); add(sources); add(note, BorderLayout.SOUTH);
        browse.addActionListener(e -> FilePickers.choose(this, FilePickers.Purpose.GENERATOR_STORE, "Open", generator.getText())
                .ifPresent(path -> generator.setText(path.toString())));
        mode.addActionListener(e -> { if (!updating) refresh(); });
        load(settings);
    }
    JSpinner positionsControl() { return positions; }
    void load(TrainingSettings settings) {
        updating = true;
        root = settings.root(); architecture = settings.architecture(); generator.setText(settings.generatorStore());
        mode.removeAllItems();
        if (architecture.supportsTrainingData()) mode.addItem(TrainingSource.Mode.TRAINING_DATA);
        if (architecture == NetworkArchitecture.BRN2) mode.addItem(TrainingSource.Mode.HANDCRAFTED);
        if (!architecture.nnueFamily() && architecture != NetworkArchitecture.BRN3) mode.addItem(TrainingSource.Mode.NNUE_BOOTSTRAP);
        if (architecture != NetworkArchitecture.BRN3) mode.addItem(TrainingSource.Mode.SELF_PLAY);
        TrainingSource selected = settings.source() == null ? architecture == NetworkArchitecture.BRN3 ? TrainingSource.dataSources(DataSources.directory(root)) : architecture.nnueFamily() ? TrainingSource.SELF_PLAY
                : architecture == NetworkArchitecture.BRN2 ? TrainingSource.HANDCRAFTED : new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, settings.generatorStore()) : settings.source();
        legacy = selected.mode() == TrainingSource.Mode.EXTERNAL_CORPUS || selected.frozen() ? selected : null;
        if (legacy != null) mode.addItem(legacy.mode());
        legacyConfig = settings.corpusTraining();
        positions.setValue(legacyConfig == null ? 10000 : legacyConfig.positionsPerGeneration());
        mode.setSelectedItem(selected.mode()); sources.load(root, architecture); updating = false; refresh();
    }
    void selectRoot(String path, NetworkArchitecture value) {
        if (!path.isBlank()) { root = Path.of(path); architecture = value; sources.load(root, value); }
    }
    TrainingSource read() {
        var value = (TrainingSource.Mode) mode.getSelectedItem();
        if (value == TrainingSource.Mode.TRAINING_DATA) return TrainingSource.dataSources(DataSources.directory(root));
        if (legacy != null && value == legacy.mode()) return legacy;
        return new TrainingSource(value, generator.getText());
    }
    void saveSources() throws java.io.IOException { if (mode.getSelectedItem() == TrainingSource.Mode.TRAINING_DATA) sources.save(); }
    boolean corpus() { return mode.getSelectedItem() == TrainingSource.Mode.TRAINING_DATA || mode.getSelectedItem() == TrainingSource.Mode.EXTERNAL_CORPUS; }
    String corpusRoot() { return mode.getSelectedItem() == TrainingSource.Mode.EXTERNAL_CORPUS && legacy != null ? legacy.generatorStore() : DataSources.directory(root).toString(); }
    CorpusTrainingConfig readCorpusConfig() throws java.text.ParseException {
        if (!corpus()) return null;
        int value;
        try { value = Integer.parseInt(((JSpinner.DefaultEditor) positions.getEditor()).getTextField().getText().trim()); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Positions / generation must be an integer", invalid); }
        positions.commitEdit();
        return new CorpusTrainingConfig(value, mode.getSelectedItem() == TrainingSource.Mode.EXTERNAL_CORPUS && legacyConfig != null
                && legacyConfig.positionsPerGeneration() == value ? legacyConfig.viewIdentity() : "",
                mode.getSelectedItem() == TrainingSource.Mode.TRAINING_DATA && sources.sourceSpecificTargets()
                        ? CorpusTraining.sourceOutcomeAdapter(architecture.trainingArchitecture()) : "").forArchitecture(architecture.trainingArchitecture());
    }
    String generatorStore() { return generator.getText().trim(); }
    boolean ready() { return mode.getSelectedItem() != TrainingSource.Mode.TRAINING_DATA || sources.ready(); }
    boolean bootstrap() { return mode.getSelectedItem() != TrainingSource.Mode.SELF_PLAY; }
    void setEditable(boolean enabled) { editable = enabled; refresh(); }
    void selectionSeedChanged(long seed) { }
    void corpusChanged(String path) { }
    private void refresh() {
        mode.setEnabled(editable && (legacy == null || !legacy.frozen()));
        boolean nnue = mode.getSelectedItem() == TrainingSource.Mode.NNUE_BOOTSTRAP;
        generatorFields.setVisible(nnue);
        for (Component child : generatorFields.getParent().getComponents()) if (child instanceof JLabel label && label.getLabelFor() == generatorFields) label.setVisible(nnue);
        for (Component child : generatorFields.getComponents()) child.setEnabled(editable && nnue);
        sources.setVisible(corpus()); sources.setEditable(editable);
        positions.setEnabled(editable && corpus());
        note.setText(mode.getSelectedItem() == TrainingSource.Mode.EXTERNAL_CORPUS
                ? "Legacy Training Data: unchanged partial work can resume with its original records. For sequential generations, choose Training Data sources, register the existing source, and acknowledge unknown previous usage."
                : corpus() ? "Sequential Training Data. Source locations and allocation weights are campaign setup; Positions / generation is in Configuration."
                : "Self-generated positions. Candidate validation is configured independently.");
        changed.run(); revalidate();
    }
    String summary() { return corpus() ? "Training Data: " + sources.summary() + " | sequential" : "Positions: " + mode.getSelectedItem(); }
    public Dimension getPreferredScrollableViewportSize() { return new Dimension(560, 500); }
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 20; }
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(20, visible.height - 20); }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
}
