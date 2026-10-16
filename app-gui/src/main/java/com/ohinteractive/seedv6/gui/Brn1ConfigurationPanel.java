package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** BRN-1 has online sparse Adam, with no minibatch or epoch controls in this milestone. */
final class Brn1ConfigurationPanel extends JPanel {

    Brn1ConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout()); setOpaque(false); setName("brn1Configuration");
        JPanel body = padded(new BorderLayout(0, SeedTheme.scale(10)), 14);
        JTextArea explanation = text("""
                New BRN-1 stores start from deterministic randomized weights and fresh Adam state. Use New Lineage to create another model.
                Each generation makes one shuffled pass with online sparse Adam. The common recipe controls the learning rate; Resume preserves weights, moments and step.
                Targets are terminal win/draw/loss from side to move. Search uses bounded BRN units (uncalibrated), with full windows and mate-distance-only selectivity.
                """, 12, SeedTheme.SECONDARY);
        explanation.setName("brn1TrainingInformation"); explanation.setRows(7); body.add(explanation);
        add(card("BRN-1 Configuration", null, body));
    }

}
