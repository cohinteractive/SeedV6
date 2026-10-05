package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** One explicit recipe-rate control for every architecture; legacy absence keeps checkpoint semantics. */
final class RecipeLearningRatePanel extends JPanel {
    private final JSpinner rate = new JSpinner(new SpinnerNumberModel(.001, Double.MIN_VALUE, Double.MAX_VALUE, .0001));
    private final JCheckBox inherit = new JCheckBox("Keep checkpoint learning rate on Resume");
    private final JTextArea evidence = text("", 11, SeedTheme.SECONDARY);
    private boolean editable = true;
    private long ticket;
    RecipeLearningRatePanel(TrainingSettings settings) {
        super(new BorderLayout(0, SeedTheme.scale(6))); setOpaque(false);
        SeedTheme.padding(this, 12, 14, 12, 14);
        rate.setName("recipeLearningRate"); inherit.setName("inheritLearningRate"); evidence.setName("recipeRateEvidence");
        rate.setEditor(new JSpinner.NumberEditor(rate, "0.############"));
        inherit.setOpaque(false); inherit.addActionListener(e -> refresh());
        var fields = panel(new GridBagLayout()); TrainingPanel.row(fields, 0, "Learning rate", rate);
        add(fields, BorderLayout.NORTH); add(inherit); add(evidence, BorderLayout.SOUTH);
        evidence.setRows(2); load(settings);
    }
    void load(TrainingSettings settings) {
        long expected = ++ticket;
        inherit.setSelected(settings.recipeLearningRate() == null);
        rate.setValue(settings.recipe().learningRate()); refresh();
        evidence.setText("Default " + settings.initialLearningRate() + ". Changed recipe rates preserve Adam moments and step; unfinished generations restart safely.");
        new SwingWorker<Double, Void>() {
            protected Double doInBackground() throws Exception {
                if (java.nio.file.Files.notExists(settings.root().resolve("refs/latest-training"))) return null;
                String id = CheckpointInspection.reference(settings.root(), "latest-training");
                return CheckpointStore.catalog(settings.root()).checkpoints().stream()
                        .filter(c -> c.manifest().id().equals(id) && c.hyperparameters() != null)
                        .map(c -> c.hyperparameters().learningRate()).findFirst().orElse(null);
            }
            protected void done() {
                if (expected != ticket) return;
                try {
                    Double stored = get();
                    if (stored != null) {
                        if (inherit.isSelected()) rate.setValue(stored);
                        evidence.setText("Latest checkpoint rate: " + stored + ". An explicit change preserves moments and step and restarts unfinished work safely.");
                    }
                } catch (Exception unavailable) {
                    evidence.setText("Stored learning rate unavailable: " + TrainingController.concise(unavailable)
                            + ". Resume still verifies the selected checkpoint.");
                }
            }
        }.execute();
    }
    Double read() throws ParseException {
        if (inherit.isSelected()) return null;
        rate.commitEdit(); double value = ((Number) rate.getValue()).doubleValue();
        new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(value); return value;
    }
    void setEditable(boolean value) { editable = value; refresh(); }
    private void refresh() { inherit.setEnabled(editable); rate.setEnabled(editable && !inherit.isSelected()); }
}
