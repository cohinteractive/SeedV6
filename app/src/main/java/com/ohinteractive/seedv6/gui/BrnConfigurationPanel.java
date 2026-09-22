package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** BRN-0 has online sparse Adam, with no minibatch or epoch controls in this milestone. */
final class BrnConfigurationPanel extends JPanel {
    private final JSpinner learningRate;

    BrnConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout()); setOpaque(false); setName("brnConfiguration");
        learningRate = new JSpinner(new SpinnerNumberModel(settings.brnLearningRate(), Double.MIN_VALUE, Double.MAX_VALUE, 0.0001));
        learningRate.setEditor(new JSpinner.NumberEditor(learningRate, "0.##########"));
        learningRate.setName("brnLearningRate");
        learningRate.setToolTipText("Used only when bootstrapping a new BRN store. Resume restores the exact stored learning rate and Adam state.");
        JPanel body = padded(new BorderLayout(0, SeedTheme.scale(10)), 14);
        JPanel fields = panel(new GridBagLayout());
        TrainingPanel.row(fields, 0, "Initial learning rate", learningRate);
        body.add(fields, BorderLayout.NORTH);
        JTextArea explanation = text("""
                New BRN-0 stores start from zero weights and fresh Adam state. Use a separate empty checkpoint folder for a new lineage.
                Each generation makes one shuffled pass with online sparse Adam. Resume restores the stored learning rate, weights, moments and step.
                Targets are terminal win/draw/loss from side to move. Search uses bounded BRN units (uncalibrated), with full windows and mate-distance-only selectivity.
                """, 12, SeedTheme.SECONDARY);
        explanation.setName("brnTrainingInformation"); explanation.setRows(7); body.add(explanation);
        add(card("BRN-0 Configuration", null, body));
    }

    double read() throws ParseException {
        learningRate.commitEdit();
        double value = ((Number) learningRate.getValue()).doubleValue();
        new BrnAdamConfig(value);
        return value;
    }
    void setEditable(boolean editable) { learningRate.setEnabled(editable); }
}
