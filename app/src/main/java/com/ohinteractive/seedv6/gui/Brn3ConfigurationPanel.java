package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** The investigated BRN-3 recipe; existing NNUE controls retain their own panel. */
final class Brn3ConfigurationPanel extends JPanel {
    Brn3ConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout());setOpaque(false);setName("brn3Configuration");
        JPanel body=padded(new BorderLayout(0,SeedTheme.scale(12)),14);
        var information=text("""
                Fresh BRN-3 starts from fixed material values and learns a relational correction from Training Data.
                Defaults are batch 128, 8 epochs and learning rate 0.003. Set the actual recipe above; Resume retains optimizer moments and step.
                Training loss fits corpus outcomes. Play uses fixed material plus 0.25 times the learned residual; game pairs measure playing strength.
                Search uses 100 units per pawn. Existing weights and optimizer state retain their format; fresh models start from material only.
                """,12,SeedTheme.SECONDARY);
        information.setName("brn3TrainingInformation");information.setRows(8);body.add(information);
        add(card("BRN-3 model capabilities",null,body));
    }
}
