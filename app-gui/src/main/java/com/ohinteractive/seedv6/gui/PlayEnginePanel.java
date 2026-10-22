package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.*;
import com.ohinteractive.seedv6.training.model.ModelLibrary;
import com.ohinteractive.seedv6.training.service.LearningArenaConfig;

/** Participant selector; HCE has no architecture, lineage or checkpoint. */
final class PlayEnginePanel extends JPanel {
    final JComboBox<PlayEvaluator.Mode> evaluator = new JComboBox<>(PlayEvaluator.Mode.competitiveChoices());
    final JComboBox<NetworkArchitecture> architecture;
    final JComboBox<ModelLibrary.Entry> lineage;
    final JComboBox<ModelChoice> generation;
    private final ModelSelectionPanel model;
    private final Preferences preferences;
    private final Runnable changed;
    private final JLabel description = new JLabel("HCE does not use a checkpoint.");
    private LearningArenaConfig.Competitor fixedBinding;
    private boolean editing = true, disposed;
    private String legacySelection;
    PlayEnginePanel(String side, Path fallback, Preferences preferences, Runnable changed) {
        this(side, side.equals("white") ? "White Engine" : "Black Engine", fallback, preferences, changed);
    }
    PlayEnginePanel(String side, String title, Path fallback, Preferences preferences, Runnable changed) {
        this(side, title, fallback, new TrainingFolders(TrainingSettings.defaults(fallback, NetworkArchitecture.NNUE)), preferences, changed);
    }
    PlayEnginePanel(String side, String title, Path fallback, TrainingFolders folders, Preferences preferences, Runnable changed) {
        super(new BorderLayout(0, SeedTheme.scale(5))); setOpaque(false); setName(side + "EngineSetup");
        this.preferences = preferences; this.changed = changed;
        model = new ModelSelectionPanel(side, title, fallback, folders, preferences, changed, null, false,
                side.equals("white") || side.equals("black") || side.equals("opponent"));
        model.setName(side + "ModelSetup");
        architecture = model.architecture; lineage = model.lineage; generation = model.generation;
        var heading = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(5)));
        var titleLabel = ((BorderLayout) model.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        model.remove(titleLabel); if (!title.isBlank()) heading.add(titleLabel, BorderLayout.NORTH);
        evaluator.setName(side + "Evaluator");
        evaluator.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == PlayEvaluator.Mode.HANDCRAFTED
                        ? "HCE" : value == PlayEvaluator.Mode.BEST_NNUE ? "Network (production Search)" : value, index, selected, focus);
            }
        });
        var savedMode = PlayEvaluator.Mode.CONVENTIONAL_CP64;
        if (preferences != null && !side.equals("opponent")) {
            try { savedMode = PlayEvaluator.Mode.valueOf(preferences.get("evaluator", savedMode.name())); }
            catch (IllegalArgumentException obsolete) { legacySelection = preferences.get("evaluator", "unknown"); }
            if (!savedMode.competitive()) { legacySelection = savedMode.name(); savedMode = PlayEvaluator.Mode.CONVENTIONAL_CP64; }
        }
        evaluator.setSelectedItem(savedMode);
        if (!side.equals("opponent")) {
            var row = SeedTheme.panel(new GridBagLayout()); TrainingPanel.row(row, 0, "Engine / evaluator", evaluator); heading.add(row);
        }
        evaluator.addActionListener(e -> { legacySelection = null; remember(); setEditable(editing); changed.run(); });
        heading.add(description, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH); add(model); setEditable(true);
    }
    PlayEvaluator.Mode selectedMode() { return (PlayEvaluator.Mode) evaluator.getSelectedItem(); }
    boolean handcrafted() { return selectedMode() == PlayEvaluator.Mode.HANDCRAFTED; }
    boolean preset() { return selectedMode().bundled(); }
    boolean storedNetwork() { return !handcrafted() && !preset(); }
    boolean validSelection() { return !disposed && legacySelection == null && (!storedNetwork() || model.validSelection()); }
    Path selectedRoot() { return !storedNetwork() ? null : model.selectedRoot(); }
    UUID selectedLineageId() { return !storedNetwork() ? null : model.selectedLineageId(); }
    String selectedId() { return !storedNetwork() ? "" : model.selectedId(); }
    String error() { return legacySelection != null ? "Saved evaluator " + legacySelection + " is historical/unavailable. Choose an evaluator explicitly." : !storedNetwork() ? "" : model.error(); }
    String selectionDescription() {
        return storedNetwork() ? model.validSelection() ? model.binding().description() : "Network unavailable — select a checkpoint"
                : handcrafted() ? "Handcrafted evaluator" : selectedMode().toString();
    }
    void selectStore(Path root) { model.selectStore(root); }
    void refresh(boolean preserve) { model.refresh(preserve); }
    void setEditable(boolean value) {
        editing = value; evaluator.setEnabled(value && !disposed); model.setEditable(value && storedNetwork());
        model.setVisible(fixedBinding == null && storedNetwork());
        description.setVisible(fixedBinding != null || !storedNetwork());
        if (fixedBinding == null) description.setText(legacySelection != null ? error() : preset() ? selectedMode().toString() : "HCE does not use a checkpoint.");
        if (fixedBinding == null) description.setToolTipText(preset()
                ? selectedMode() == PlayEvaluator.Mode.CONVENTIONAL_CP64
                    ? "Bundled checkpoint 64, eight material heads; Construction V1; configured workers; Hash 64 MiB; WIDEN16 aspiration."
                    : "Bundled, SHA-256 verified Pair-2 generation 32; Construction V1; configured workers; Hash 64 MiB; WIDEN16 aspiration." : null);
        revalidate();
    }
    void dispose() { disposed = true; model.dispose(); }
    PlayParticipants.Selection versus(PlayEnginePanel other) {
        return new PlayParticipants.Selection(selectedId(), other.selectedId(), selectedRoot(), other.selectedRoot(),
                selectedLineageId(), other.selectedLineageId(), selectedMode(), other.selectedMode());
    }
    LearningArenaConfig.Competitor fixedCompetitor() {
        if (!validSelection()) throw new IllegalStateException("Select an evaluator and an available network generation");
        if (selectedMode().competitive()) return LearningArenaConfig.Competitor.competitive(
                selectedMode() == PlayEvaluator.Mode.HANDCRAFTED ? com.ohinteractive.seedv6.search.driver.BaselineEngine.Evaluator.HCE
                : selectedMode() == PlayEvaluator.Mode.BREP2_32 ? com.ohinteractive.seedv6.search.driver.BaselineEngine.Evaluator.BREP2_32
                : com.ohinteractive.seedv6.search.driver.BaselineEngine.Evaluator.CONVENTIONAL_CP64);
        if (preset()) return LearningArenaConfig.Competitor.pinned(selectedMode().policy());
        return handcrafted() ? LearningArenaConfig.Competitor.handcrafted()
                : LearningArenaConfig.Competitor.fixed(model.binding());
    }
    void loadFixed(LearningArenaConfig.Competitor competitor) {
        fixedBinding = competitor;
        evaluator.setSelectedItem(competitor.evaluator() != null && competitor.evaluator().competitive()
                ? switch (competitor.evaluator()) {
                    case HCE_CONSTRUCTION -> PlayEvaluator.Mode.HANDCRAFTED;
                    case BREP2_32 -> PlayEvaluator.Mode.BREP2_32;
                    case CONVENTIONAL_CP64 -> PlayEvaluator.Mode.CONVENTIONAL_CP64;
                    default -> throw new IllegalStateException();
                } : competitor.isPinned() ? competitor.evaluator() == LearningArenaConfig.Evaluator.CONSTRUCTION_V1
                ? PlayEvaluator.Mode.CONSTRUCTION_V1 : PlayEvaluator.Mode.POLICY4_PAIR2
                : competitor.isHandcrafted() ? PlayEvaluator.Mode.HANDCRAFTED : PlayEvaluator.Mode.BEST_NNUE);
        var initial = competitor.initialModel();
        description.setText(competitor.isPinned() ? competitor.displayName() : competitor.isHandcrafted() ? "HCE does not use a checkpoint."
                : competitor.displayName() + " · " + initial.name() + " · source Gen " + initial.generation());
        description.setToolTipText(initial == null ? null : initial.root() + " / " + initial.checkpoint());
        setEditable(false);
    }
    void clearFixed() { fixedBinding = null; description.setText("HCE does not use a checkpoint."); description.setToolTipText(null); setEditable(true); }
    void swapWith(PlayEnginePanel other) {
        if (!validSelection() || !other.validSelection()) throw new IllegalStateException("Choose both evaluators before swapping.");
        var mine = selectedMode(); var theirs = other.selectedMode();
        model.swapChoices(other.model);
        evaluator.setSelectedItem(theirs); other.evaluator.setSelectedItem(mine);
        remember(); other.remember(); changed.run(); other.changed.run();
    }
    private void remember() { if (preferences != null) preferences.put("evaluator", selectedMode().name()); }
}
