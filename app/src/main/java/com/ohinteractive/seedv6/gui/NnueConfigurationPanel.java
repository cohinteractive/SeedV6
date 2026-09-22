package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import com.ohinteractive.seedv6.search.quiescence.QuiescenceSearch;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** These settings belong to the current NNUE trainer, not to every future architecture. */
final class NnueConfigurationPanel extends JPanel {
    private final JSpinner minibatch, epochs;
    record Values(int minibatch, int epochs) {}

    NnueConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout()); setOpaque(false); setName("nnueConfiguration");
        minibatch = new JSpinner(new SpinnerNumberModel(settings.minibatch(), 1, 100_000, 1));
        epochs = new JSpinner(new SpinnerNumberModel(settings.epochs(), 1, 100_000, 1));
        minibatch.setName("trainingMinibatch"); epochs.setName("trainingEpochs");
        JPanel body = padded(new BorderLayout(0, SeedTheme.scale(10)), 14);
        JPanel fields = panel(new GridLayout(1, 2, SeedTheme.scale(20), 0));
        JPanel left = panel(new GridBagLayout()), right = panel(new GridBagLayout());
        TrainingPanel.row(left, 0, "Minibatch size", minibatch);
        TrainingPanel.row(right, 0, "Training epochs", epochs);
        fields.add(left); fields.add(right); body.add(fields, BorderLayout.NORTH);
        JTextArea explanation = text("""
                Fixed-depth iterative deepening with alpha-beta/PVS and quiescence; root parallelism uses the configured threads. No node/time limit.
                Incremental NNUE uses V1 units (uncalibrated), scale %s. Full-window search, no aspiration; selective policy is mate-distance only.
                Quiescence soft limit: %d plies (checks continue); absolute search ply limit: %d.
                Each network has a private TT per game, reused across moves; its root workers share that TT.
                """.formatted(TrainingSettings.SCORE_MAPPING.scale(), QuiescenceSearch.SOFT_QPLY_LIMIT,
                        QuiescenceSearch.MAX_ABSOLUTE_PLY), 12, SeedTheme.SECONDARY);
        explanation.setName("nnueSearchInformation"); explanation.setRows(7); body.add(explanation);
        add(card("NNUE Configuration", null, body));
    }

    Values read() throws ParseException {
        minibatch.commitEdit(); epochs.commitEdit();
        return new Values(((Number) minibatch.getValue()).intValue(), ((Number) epochs.getValue()).intValue());
    }

    void setEditable(boolean editable) {
        minibatch.setEnabled(editable); epochs.setEnabled(editable);
    }
}
