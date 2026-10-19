package com.ohinteractive.seedv6.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

final class BrnNextConfigurationPanel extends JPanel {
    BrnNextConfigurationPanel(NetworkArchitecture architecture) {
        super(new BorderLayout()); setOpaque(false);
        String description = switch (architecture) {
            case BRN_GATED -> "Four learned context gates combine shared local pair-pattern experts.";
            case BRN_CUBIC -> "Rank16 factors learn distinct two-piece and three-piece interactions.";
            case BRN_POOLED -> "A small pooled tanh network controls for generic nonlinear capacity.";
            default -> throw new IllegalArgumentException("Not a BRN successor");
        };
        JPanel body = padded(new BorderLayout(), 14);
        var information = text(description + "\n"
                + "Fresh models start from fixed material with a zero learned residual.\n"
                + "Defaults: Adam 0.003, batch 128, 8 epochs; compatible CP / BT4 Q Training Data.\n"
                + "Stop / Resume retains weights, moments, seed identity and source progress.\n"
                + "Play initially uses one quarter of the residual; strength is not yet established.\n"
                + "Training fit and held-out loss are diagnostics, not playing-strength evidence.", 12, SeedTheme.SECONDARY);
        information.setName("brnNextTrainingInformation-" + architecture.name()); information.setRows(7);
        body.add(information); add(card(architecture + " capabilities", null, body));
    }
}
