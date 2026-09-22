package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** BRN-2 has online sparse Adam, with no minibatch or epoch controls in this milestone. */
final class Brn2ConfigurationPanel extends JPanel {
    private final JSpinner learningRate;

    Brn2ConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout()); setOpaque(false); setName("brn2Configuration");
        learningRate = new JSpinner(new SpinnerNumberModel(settings.brn2LearningRate(), Double.MIN_VALUE, Double.MAX_VALUE, 0.0001));
        learningRate.setEditor(new JSpinner.NumberEditor(learningRate, "0.##########"));
        learningRate.setName("brn2LearningRate");
        learningRate.setToolTipText("Used only when bootstrapping a new BRN-2 store. Resume restores the exact stored learning rate and Adam state.");
        JPanel body = padded(new BorderLayout(0, SeedTheme.scale(10)), 14);
        JPanel fields = panel(new GridBagLayout());
        TrainingPanel.row(fields, 0, "Initial learning rate", learningRate);
        body.add(fields, BorderLayout.NORTH);
        JTextArea explanation = text("""
                New BRN-2 stores start from deterministic randomized weights and fresh Adam state. Use a separate empty checkpoint folder for a new lineage.
                Each generation makes one shuffled pass with online sparse Adam. Resume restores the stored learning rate, weights, moments and step.
                Targets are terminal win/draw/loss from side to move. Search uses bounded BRN units (uncalibrated), with full windows and mate-distance-only selectivity.
                """, 12, SeedTheme.SECONDARY);
        explanation.setName("brn2TrainingInformation"); explanation.setRows(7); body.add(explanation);
        add(card("BRN-2 Configuration", null, body));
    }

    double read() throws ParseException {
        learningRate.commitEdit();
        double value = ((Number) learningRate.getValue()).doubleValue();
        new BrnAdamConfig(value);
        return value;
    }
    void setEditable(boolean editable) { learningRate.setEnabled(editable); }
}
