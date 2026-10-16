package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** These settings belong to the current NNUE trainer, not to every future architecture. */
final class NnueConfigurationPanel extends JPanel {
    private final JLabel semantics = new JLabel();

    NnueConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout()); setOpaque(false); setName("nnueConfiguration");
        JPanel body = padded(new BorderLayout(0, SeedTheme.scale(10)), 14);
        JTextArea explanation = text("""
                Fixed-depth iterative deepening with PVS and static leaves. Games run sequentially; each search uses the configured threads. No node/time limit.
                Incremental NNUE uses V1 units (uncalibrated), scale %s. Full-window exact search with mate-distance bounds.
                Maximum search depth: %d plies.
                Each game has a private TTable and evaluator state, reused across moves.
                """.formatted(TrainingSettings.SCORE_MAPPING.scale(), ExactSearch.MAX_DEPTH), 12, SeedTheme.SECONDARY);
        explanation.setName("nnueSearchInformation"); explanation.setRows(7); body.add(explanation);
        body.add(semantics, BorderLayout.SOUTH);
        load(settings);
        add(card("NNUE model capabilities", null, body));
    }

    void load(TrainingSettings settings) {
        semantics.setText(settings.architecture() == NetworkArchitecture.NNUE_MATERIAL
                ? "Fixed BRN-3 material + learned residual in training and search."
                : "Legacy knowledge-free NNUE: no fixed material contribution.");
    }

}
