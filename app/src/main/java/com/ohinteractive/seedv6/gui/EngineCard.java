package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.util.Locale;
import javax.swing.*;

/** Completed-iteration telemetry, rendered only on the EDT at the existing callback cadence. */
final class EngineCard extends JPanel {
    private final JLabel state = SeedTheme.label("●  Ready", 13, SeedTheme.GREEN);
    private final JLabel score = metric("engineScore"), depth = metric("engineDepth"), nodes = metric("engineNodes"),
            nps = metric("engineNps"), time = metric("engineTime");
    private final JLabel scoreTitle = SeedTheme.label("Score · White", 12, SeedTheme.SECONDARY);
    private final JTextArea pv = new JTextArea(1, 10);
    private final JLabel termination = SeedTheme.label("Ready", 11, SeedTheme.MUTED);

    EngineCard() {
        super(new BorderLayout()); setOpaque(false);
        state.setName("engineState"); termination.setName("searchTermination");
        JPanel body = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(12)));
        SeedTheme.padding(body, 14, 16, 12, 16);
        JPanel metrics = SeedTheme.panel(new GridLayout(1, 5, SeedTheme.scale(10), 0));
        metrics.add(metricColumn(scoreTitle, score));
        metrics.add(metricColumn(SeedTheme.label("Depth", 12, SeedTheme.SECONDARY), depth));
        metrics.add(metricColumn(SeedTheme.label("Nodes", 12, SeedTheme.SECONDARY), nodes));
        metrics.add(metricColumn(SeedTheme.label("NPS", 12, SeedTheme.SECONDARY), nps));
        metrics.add(metricColumn(SeedTheme.label("Time", 12, SeedTheme.SECONDARY), time));
        body.add(metrics, BorderLayout.NORTH);
        JPanel variation = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(6)));
        variation.add(SeedTheme.label("Principal variation (PV)", 12, SeedTheme.SECONDARY), BorderLayout.NORTH);
        pv.setName("principalVariation"); pv.setEditable(false); pv.setLineWrap(true); pv.setWrapStyleWord(true);
        pv.setFont(new Font(Font.MONOSPACED, Font.PLAIN, SeedTheme.scale(13)));
        pv.setBackground(SeedTheme.INSET); pv.setMargin(new Insets(SeedTheme.scale(9), SeedTheme.scale(10), SeedTheme.scale(9), SeedTheme.scale(10)));
        JScrollPane scroll = new JScrollPane(pv); scroll.setPreferredSize(new Dimension(0, SeedTheme.scale(44)));
        variation.add(scroll, BorderLayout.CENTER);
        variation.add(termination, BorderLayout.SOUTH); body.add(variation, BorderLayout.CENTER);
        add(SeedTheme.card("Engine", state, body));
        time.setToolTipText("Elapsed time at the last completed depth; no per-node polling.");
        depth.setToolTipText("Last fully completed search depth.");
    }

    PlayScore showSearch(GameController.SearchInfo search, PlayEvaluator evaluator) {
        boolean nnue = evaluator.mode() == PlayEvaluator.Mode.BEST_NNUE;
        PlayScore value = PlayScore.from(search, nnue);
        score.setText(value.text()); score.setForeground(value.available() ? SeedTheme.GREEN : SeedTheme.TEXT);
        scoreTitle.setText(nnue ? "NNUE · White" : "Score · White");
        score.setToolTipText(nnue ? "Uncalibrated NNUE V1 units, White perspective; not centipawns." : "Pawns, White perspective.");
        depth.setText(search.depth() == 0 ? "—" : Integer.toString(search.depth()));
        nodes.setText(search.depth() == 0 && search.nodes() == 0 ? "—" : compact(search.nodes()));
        nps.setText(search.nps() < 0 ? "—" : compact(search.nps()) + "/s");
        time.setText(search.elapsedMillis() < 0 ? "—" : String.format(Locale.ROOT, "%.2f s", search.elapsedMillis() / 1000.0));
        state.setText("●  " + (search.state().equals("Idle") ? "Ready" : search.state()));
        state.setForeground(search.state().equals("Failed") ? SeedTheme.ERROR : SeedTheme.GREEN);
        pv.setText(search.pv().isEmpty() ? "—" : search.pv()); pv.setCaretPosition(0);
        termination.setText(nnue ? "NNUE units · uncalibrated  |  " + search.termination() : "Centipawns / 100  |  " + search.termination());
        if (nnue && search.depth() > 0) termination.setText(termination.getText() + "  |  "
                + (search.scoreSide() == com.ohinteractive.seedv6.core.util.Value.WHITE ? "White" : "Black")
                + " · " + PlayEvaluator.shortId(evaluator.checkpointId()));
        termination.setToolTipText(evaluator.identity());
        return value;
    }
    private static JPanel metricColumn(JLabel title, JLabel value) {
        JPanel column = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(7)));
        column.add(title, BorderLayout.NORTH); column.add(value, BorderLayout.CENTER); return column;
    }
    private static JLabel metric(String name) {
        JLabel value = SeedTheme.label("—", 21, SeedTheme.TEXT); value.setFont(SeedTheme.font(21, Font.BOLD));
        value.setName(name); return value;
    }
    private static String compact(long value) {
        if (value >= 1_000_000_000) return String.format(Locale.ROOT, "%.1f B", value / 1e9);
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.1f M", value / 1e6);
        if (value >= 10_000) return String.format(Locale.ROOT, "%.1f k", value / 1e3);
        return Long.toString(value);
    }
}
