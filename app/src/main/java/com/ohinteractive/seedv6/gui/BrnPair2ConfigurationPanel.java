package com.ohinteractive.seedv6.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** Existing capabilities card for the explicitly selected experimental family. */
final class BrnPair2ConfigurationPanel extends JPanel {
    BrnPair2ConfigurationPanel() {
        super(new BorderLayout()); setOpaque(false); setName("brnPair2Configuration");
        JPanel body=padded(new BorderLayout(),14);
        var information=text("""
                Experimental BRN Pair-2 is the compiled order-2 relational architecture from research C02.
                Fresh training starts with fixed material and zero learned pair weights.
                Defaults: learning rate 0.01, batch 128 and 8 epochs over selected Training Data.
                Stop / Resume preserves the complete training weights, Adam moments and source progress.
                Play uses compiled pair tables and material plus one quarter of the learned residual.
                Pair-2 keeps its own generations, latest training and Best network.
                """,12,SeedTheme.SECONDARY);
        information.setName("brnPair2TrainingInformation"); information.setRows(7); body.add(information);
        add(card("BRN Pair-2 model capabilities (experimental)",null,body));
    }
}
