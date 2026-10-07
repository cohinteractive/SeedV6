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
    final JComboBox<PlayEvaluator.Mode> evaluator = new JComboBox<>(PlayEvaluator.Mode.values());
    final JComboBox<NetworkArchitecture> architecture;
    final JComboBox<ModelLibrary.Entry> lineage;
    final JComboBox<ModelChoice> generation;
    private final ModelSelectionPanel model;
    private final Preferences preferences;
    private final Runnable changed;
    private final JLabel description = new JLabel("HCE does not use a checkpoint.");
    private LearningArenaConfig.Competitor fixedBinding;
    private boolean editing = true, disposed;
    PlayEnginePanel(String side, Path fallback, Preferences preferences, Runnable changed) {
        this(side, side.equals("white") ? "White Engine" : "Black Engine", fallback, preferences, changed);
    }
    PlayEnginePanel(String side, String title, Path fallback, Preferences preferences, Runnable changed) {
        this(side, title, fallback, new TrainingFolders(TrainingSettings.defaults(fallback, NetworkArchitecture.NNUE)), preferences, changed);
    }
    PlayEnginePanel(String side, String title, Path fallback, TrainingFolders folders, Preferences preferences, Runnable changed) {
        super(new BorderLayout(0, SeedTheme.scale(5))); setOpaque(false); setName(side + "EngineSetup");
        this.preferences = preferences; this.changed = changed;
        model = new ModelSelectionPanel(side, title, fallback, folders, preferences, changed);
        model.setName(side + "ModelSetup");
        architecture = model.architecture; lineage = model.lineage; generation = model.generation;
        var heading = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(5)));
        var titleLabel = ((BorderLayout) model.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        model.remove(titleLabel); if (!title.isBlank()) heading.add(titleLabel, BorderLayout.NORTH);
        evaluator.setName(side + "Evaluator");
        evaluator.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == PlayEvaluator.Mode.HANDCRAFTED
                        ? "HCE (Handcrafted)" : "Network", index, selected, focus);
            }
        });
        evaluator.setSelectedItem(preferences != null && preferences.get("evaluator", "").equals("HANDCRAFTED")
                && !side.equals("opponent") ? PlayEvaluator.Mode.HANDCRAFTED : PlayEvaluator.Mode.BEST_NNUE);
        if (!side.equals("opponent")) {
            var row = SeedTheme.panel(new GridBagLayout()); TrainingPanel.row(row, 0, "Evaluator", evaluator); heading.add(row);
        }
        evaluator.addActionListener(e -> { remember(); setEditable(editing); changed.run(); });
        heading.add(description, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH); add(model); setEditable(true);
    }
    PlayEvaluator.Mode selectedMode() { return (PlayEvaluator.Mode) evaluator.getSelectedItem(); }
    boolean handcrafted() { return selectedMode() == PlayEvaluator.Mode.HANDCRAFTED; }
    boolean validSelection() { return !disposed && (handcrafted() || model.validSelection()); }
    Path selectedRoot() { return handcrafted() ? null : model.selectedRoot(); }
    UUID selectedLineageId() { return handcrafted() ? null : model.selectedLineageId(); }
    String selectedId() { return handcrafted() ? "" : model.selectedId(); }
    String error() { return handcrafted() ? "" : model.error(); }
    void selectStore(Path root) { model.selectStore(root); }
    void refresh(boolean preserve) { model.refresh(preserve); }
    void setEditable(boolean value) {
        editing = value; evaluator.setEnabled(value && !disposed); model.setEditable(value && !handcrafted());
        model.setVisible(fixedBinding == null && !handcrafted());
        description.setVisible(fixedBinding != null || handcrafted());
        revalidate();
    }
    void dispose() { disposed = true; model.dispose(); }
    PlayParticipants.Selection versus(PlayEnginePanel other) {
        return new PlayParticipants.Selection(selectedId(), other.selectedId(), selectedRoot(), other.selectedRoot(),
                selectedLineageId(), other.selectedLineageId(), selectedMode(), other.selectedMode());
    }
    LearningArenaConfig.Competitor fixedCompetitor() {
        if (!validSelection()) throw new IllegalStateException("Select an evaluator and an available network generation");
        return handcrafted() ? LearningArenaConfig.Competitor.handcrafted()
                : LearningArenaConfig.Competitor.fixed(model.binding());
    }
    void loadFixed(LearningArenaConfig.Competitor competitor) {
        fixedBinding = competitor;
        evaluator.setSelectedItem(competitor.isHandcrafted() ? PlayEvaluator.Mode.HANDCRAFTED : PlayEvaluator.Mode.BEST_NNUE);
        var initial = competitor.initialModel();
        description.setText(competitor.isHandcrafted() ? "HCE does not use a checkpoint."
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
