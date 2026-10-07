package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.LearningArenaState;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** One completed round per row; Arena and per-model training facts retain separate ownership. */
final class ArenaHistoryView extends JPanel {
    private final ArenaScoreChart chart = new ArenaScoreChart();
    private final JLabel first = new JLabel(), second = new JLabel();
    private final DefaultTableModel rows = new DefaultTableModel(new String[]{"Round", "Winning model", "W-D-L", "Score", "Avg sample visits/s", "Training duration", "LR", "Minibatch", "Epochs", "Final mean loss", "Exposure", "Unscored pairs"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(rows) {
        @Override public String getToolTipText(java.awt.event.MouseEvent event) {
            int r = rowAtPoint(event.getPoint()), c = columnAtPoint(event.getPoint());
            if (r < 0 || c < 0) return null;
            if (c == 5 && state != null) return "Total active training time for both models. A: "
                    + ArenaTrainingSummary.value(state.config(), completed.get(r), true, 1) + " / B: "
                    + ArenaTrainingSummary.value(state.config(), completed.get(r), false, 1);
            if (c >= 4 && c <= 9 && state != null) return "Trained models A / B: " + state.config().a().name() + " / " + state.config().b().name() + ". " + getValueAt(r, c);
            if (c == 2 || c == 3) return "Winner first; ties use A / B. Valid pairs only. " + getValueAt(r, c);
            return String.valueOf(getValueAt(r, c));
        }
    };
    private LearningArenaState state;
    private List<LearningArenaState.Round> completed = List.of();
    private Path root;
    ArenaHistoryView() {
        super(new BorderLayout(0, SeedTheme.scale(10))); setOpaque(false); setName("arenaHistoryView");
        var trajectory = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(6)));
        var legend = SeedTheme.panel(new GridLayout(1, 2, SeedTheme.scale(14), 0));
        first.setForeground(ArenaScoreChart.FIRST); second.setForeground(ArenaScoreChart.SECOND);
        first.setName("arenaFirstLegend"); second.setName("arenaSecondLegend"); legend.add(first); legend.add(second); trajectory.add(legend, BorderLayout.NORTH);
        var plot = new JScrollPane(chart, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        plot.setBorder(null); plot.setPreferredSize(new Dimension(600, SeedTheme.scale(280))); trajectory.add(plot);
        var note = new JLabel("Completed rounds · valid pairs only. Training: A / B; duration totals both models. Round 0 has no training.");
        note.setFont(SeedTheme.font(12, Font.PLAIN)); trajectory.add(note, BorderLayout.SOUTH); add(trajectory, BorderLayout.NORTH);
        table.setName("arenaHistory"); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setRowHeight(SeedTheme.scale(28));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {55, 145, 85, 155, 160, 145, 130, 90, 80, 190, 105, 110};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(SeedTheme.scale(widths[i]));
        add(new JScrollPane(table));
        var details = new JButton("Selected snapshot details..."); details.setName("arenaHistoryDetails"); details.addActionListener(e -> details());
        var actions = SeedTheme.panel(new FlowLayout(FlowLayout.RIGHT)); actions.add(details); add(actions, BorderLayout.SOUTH);
    }
    void showState(LearningArenaState value, Path location) {
        root = location; if (state == value) return;
        var prior = state; state = value;
        if (value == null) { completed = List.of(); rows.setRowCount(0); chart.showRounds(List.of()); first.setText(""); second.setText(""); return; }
        var settled = value.history().stream().filter(LearningArenaState.Round::arenaComplete).toList();
        if (prior != null && prior.id().equals(value.id()) && completed.equals(settled)) return;
        first.setText("A / lower: " + ArenaMatchSummary.label(value.config(), true)); first.setToolTipText(value.config().a().name());
        second.setText("B / upper: " + ArenaMatchSummary.label(value.config(), false)); second.setToolTipText(value.config().b().name());
        int selected = table.getSelectedRow(); rows.setRowCount(0);
        completed = settled;
        chart.showRounds(completed.stream().map(r -> ArenaRoundSummary.from(value.config(), r)).toList());
        for (var r : completed) {
            var s = ArenaRoundSummary.from(value.config(), r);
            rows.addRow(new Object[]{s.round(), s.winner(), s.winningWdl(), s.orderedScores(),
                    ArenaTrainingSummary.cell(value.config(), r, 0), ArenaTrainingSummary.cell(value.config(), r, 1),
                    ArenaTrainingSummary.cell(value.config(), r, 2), ArenaTrainingSummary.cell(value.config(), r, 3),
                    ArenaTrainingSummary.cell(value.config(), r, 4), ArenaTrainingSummary.cell(value.config(), r, 5),
                    r.a().exposure(), s.unscoredPairs()});
        }
        if (selected >= 0 && selected < rows.getRowCount()) table.setRowSelectionInterval(selected, selected);
    }
    String detailsText(int selected) {
        var round = completed.get(selected); var config = state.config();
        var text = new StringBuilder(ArenaRoundSummary.from(config, round).description());
        text.append("\nExposure is cumulative sample visits per competitor; repeated epochs count.\nTraining metrics below belong to each trained model, regardless of the winner.\n");
        for (boolean a : new boolean[]{true, false}) {
            var endpoint = a ? round.a() : round.b(); var competitor = a ? config.a() : config.b();
            text.append("\n").append(a ? "A" : "B").append(round.number() == 0 ? " · Initial model: " : " · Trained model: ")
                    .append(ArenaRoundSummary.model(competitor, endpoint)).append("\nCheckpoint: ").append(endpoint.checkpoint())
                    .append("\nStore: ").append(root == null ? "unknown" : root.resolve(a ? "A" : "B"))
                    .append("\nPersisted architecture: ").append(competitor.architecture().name()).append(" / ").append(competitor.architecture().schemaId())
                    .append("\nCampaign records: ").append(endpoint.positions()).append(" · Sample visits: ").append(endpoint.exposure());
            if (competitor.initialModel() != null) text.append("\nInitial lineage: ").append(competitor.initialModel());
            text.append("\nTraining visits/s: ").append(ArenaTrainingSummary.value(config, round, a, 0))
                    .append(" · Active training time: ").append(ArenaTrainingSummary.value(config, round, a, 1))
                    .append("\nLR: ").append(ArenaTrainingSummary.value(config, round, a, 2))
                    .append(" · Minibatch: ").append(ArenaTrainingSummary.value(config, round, a, 3))
                    .append(" · Epochs: ").append(ArenaTrainingSummary.value(config, round, a, 4))
                    .append("\nTerminal mean training loss: ").append(ArenaTrainingSummary.value(config, round, a, 5)).append("\n");
        }
        return text.append("\nMean loss is the terminal optimizer mean over this round's sample visits.\nTraining time excludes Arena games and pauses; legacy missing timing/loss remains unavailable.\nShared tranche: ")
                .append(round.trancheHash().isEmpty() ? "none (initial round)" : round.trancheHash()).toString();
    }
    private void details() {
        if (state == null || table.getSelectedRow() < 0) return;
        var area = new JTextArea(detailsText(table.getSelectedRow()), 22, 70); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setCaretPosition(0); JOptionPane.showMessageDialog(this, new JScrollPane(area), "Round snapshot", JOptionPane.PLAIN_MESSAGE);
    }
}
