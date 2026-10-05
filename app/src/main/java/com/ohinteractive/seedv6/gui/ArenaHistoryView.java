package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.LearningArenaState;
import java.awt.*;
import java.nio.file.Path;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Named round history, exact endpoint details and a symmetric score trajectory. */
final class ArenaHistoryView extends JPanel {
    private final ArenaScoreChart chart = new ArenaScoreChart();
    private final JTextArea first = text(), second = text();
    private final DefaultTableModel rows = new DefaultTableModel(new String[]{"Round", "Model", "Gen", "W", "D", "L", "Score", "Round winner", "Exposure", "Unscored pairs"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(rows) {
        @Override public String getToolTipText(java.awt.event.MouseEvent event) {
            int r = rowAtPoint(event.getPoint()), c = columnAtPoint(event.getPoint());
            return r < 0 || c < 0 ? null : String.valueOf(getValueAt(r, c));
        }
    };
    private LearningArenaState state;
    private Path root;
    ArenaHistoryView() {
        super(new BorderLayout(0, 10)); setOpaque(false);
        var trajectory = new JPanel(new BorderLayout(0, 6)); trajectory.setOpaque(false);
        var legend = new JPanel(new GridLayout(1, 2, 14, 0)); legend.setOpaque(false);
        first.setForeground(ArenaScoreChart.FIRST); second.setForeground(ArenaScoreChart.SECOND);
        first.setName("arenaFirstLegend"); second.setName("arenaSecondLegend"); legend.add(first); legend.add(second); trajectory.add(legend, BorderLayout.NORTH);
        var plot = new JScrollPane(chart, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        plot.setBorder(null); plot.setPreferredSize(new Dimension(600, SeedTheme.scale(250))); trajectory.add(plot);
        var note = new JLabel("Scores count valid pairs only; draws split points. Exposure = campaign sample visits. Empty bars = pending / unscored.");
        note.setFont(SeedTheme.font(11, Font.PLAIN)); trajectory.add(note, BorderLayout.SOUTH); add(trajectory, BorderLayout.NORTH);
        table.setName("arenaHistory"); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setRowHeight(SeedTheme.scale(25));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {50, 280, 45, 30, 30, 30, 70, 170, 90, 100};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        add(new JScrollPane(table));
        var details = new JButton("Selected snapshot details..."); details.setName("arenaHistoryDetails"); details.addActionListener(e -> details());
        var actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setOpaque(false); actions.add(details); add(actions, BorderLayout.SOUTH);
    }
    void showState(LearningArenaState value, Path location) {
        root = location; if (state == value) return; state = value;
        var a = value.config().a(); var b = value.config().b();
        first.setText("Lower / green: " + a.name() + " - " + a.architecture().displayName());
        second.setText("Upper / blue: " + b.name() + " - " + b.architecture().displayName());
        int selected = table.getSelectedRow(); rows.setRowCount(0);
        var values = value.history().stream().map(r -> ArenaRoundSummary.from(value.config(), r)).toList(); chart.showRounds(values);
        for (int i = 0; i < values.size(); i++) {
            var r = value.history().get(i); var s = values.get(i);
            rows.addRow(new Object[]{s.round(), a.name() + " / " + a.architecture().displayName(), r.a() == null ? "pending" : r.a().generation(), s.wins(), s.draws(), s.losses(), s.score(true), s.winner(), r.a() == null ? "pending" : r.a().exposure(), s.unscoredPairs()});
            rows.addRow(new Object[]{s.round(), b.name() + " / " + b.architecture().displayName(), r.b() == null ? "pending" : r.b().generation(), s.losses(), s.draws(), s.wins(), s.score(false), s.winner(), r.b() == null ? "pending" : r.b().exposure(), s.unscoredPairs()});
        }
        if (selected >= 0 && selected < rows.getRowCount()) table.setRowSelectionInterval(selected, selected);
    }
    private void details() {
        if (state == null || table.getSelectedRow() < 0) return;
        int selected = table.getSelectedRow(); var round = state.history().get(selected / 2); boolean a = selected % 2 == 0;
        var endpoint = a ? round.a() : round.b(); var competitor = a ? state.config().a() : state.config().b();
        var area = text(); area.setRows(10); area.setColumns(60); area.setPreferredSize(new Dimension(650, 240));
        area.setText(ArenaRoundSummary.model(competitor, endpoint) + "\nRound " + round.number() + " - " + round.stage()
                + "\n" + (endpoint == null ? "Snapshot pending" : "Checkpoint: " + endpoint.checkpoint() + "\nCampaign records: " + endpoint.positions() + "\nCampaign sample visits (including epochs): " + endpoint.exposure())
                + "\nStore: " + (root == null ? "unknown" : root.resolve(a ? "A" : "B"))
                + "\nShared tranche: " + (round.trancheHash().isEmpty() ? "none (initial round or pending)" : round.trancheHash())
                + "\n" + ArenaRoundSummary.from(state.config(), round).description());
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Round snapshot", JOptionPane.PLAIN_MESSAGE);
    }
    private static JTextArea text() {
        var area = new JTextArea(2, 25); area.setEditable(false); area.setOpaque(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setFont(SeedTheme.font(12, Font.PLAIN)); area.setPreferredSize(new Dimension(1, SeedTheme.scale(40))); return area;
    }
}
