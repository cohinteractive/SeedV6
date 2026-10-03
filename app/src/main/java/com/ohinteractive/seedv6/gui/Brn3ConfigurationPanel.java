package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** The investigated BRN-3 recipe; existing NNUE controls retain their own panel. */
final class Brn3ConfigurationPanel extends JPanel {
    private final JSpinner minibatch,epochs;
    Brn3ConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout());setOpaque(false);setName("brn3Configuration");
        minibatch=new JSpinner(new SpinnerNumberModel(settings.minibatch(),1,100000,1));
        epochs=new JSpinner(new SpinnerNumberModel(settings.epochs(),1,100000,1));
        minibatch.setName("brn3Minibatch");epochs.setName("brn3Epochs");
        JPanel body=padded(new BorderLayout(0,SeedTheme.scale(12)),14),fields=panel(new GridBagLayout());
        TrainingPanel.row(fields,0,"Minibatch size",minibatch);TrainingPanel.row(fields,1,"Training epochs",epochs);
        body.add(fields,BorderLayout.NORTH);
        var information=text("""
                Fresh BRN-3 starts from fixed material values and learns a relational correction from Training Data.
                The V1 recipe uses minibatches of 128, 8 epochs and an initial learning rate of 0.003. Resume restores the stored model and optimizer exactly.
                Training uses CP labels mapped to outcome targets. Held-out loss measures prediction quality; game pairs measure playing strength.
                Search scores use 100 units per pawn. No trained network is needed to start a new lineage.
                """,12,SeedTheme.SECONDARY);
        information.setName("brn3TrainingInformation");information.setRows(8);body.add(information);
        add(card("BRN-3 Configuration",null,body));
    }
    NnueConfigurationPanel.Values read()throws ParseException {
        minibatch.commitEdit();epochs.commitEdit();
        return new NnueConfigurationPanel.Values(((Number)minibatch.getValue()).intValue(),((Number)epochs.getValue()).intValue());
    }
    void load(TrainingSettings settings){minibatch.setValue(settings.minibatch());epochs.setValue(settings.epochs());}
    void setEditable(boolean editable){minibatch.setEnabled(editable);epochs.setEnabled(editable);}
}
