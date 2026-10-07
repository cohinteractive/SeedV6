package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import com.ohinteractive.seedv6.training.validation.MaterialCount;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Arena-only layout over campaign receipts and existing move-boundary publications. */
final class ArenaLiveView extends JPanel {
    private final MatchView match = new MatchView("arenaMatch", true);
    private final TrainingProgressView optimization = new TrainingProgressView("arenaOptimization");
    private final JLabel first = label("Competitor A", 17), second = label("Competitor B", 17);
    private final JLabel firstScore = label("\u2014", 26), secondScore = label("\u2014", 26);
    private final JLabel firstWdl = label("0 W / 0 D / 0 L", 14), secondWdl = label("0 W / 0 D / 0 L", 14);
    private final JLabel progress = label("No campaign open", 14), position = label("", 14);
    private final JLabel scoring = label("Only completed valid pairs score", 13);
    private final JPanel summaryCard;
    private final JTextArea gameInfo = text(4), activity = text(2);
    private final JPanel context = new JPanel(new CardLayout()) {
        @Override public Dimension getPreferredSize() {
            for (Component child : getComponents()) if (child.isVisible()) return child.getPreferredSize();
            return super.getPreferredSize();
        }
    };
    private final DefaultTableModel rows = new DefaultTableModel(new String[]{"Game", "Pair", "White model", "Black model", "Result", "Winner", "Plies", "Final material W / B", "W-D-L (A)", "Score A / B", "Termination"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(rows) {
        @Override public String getToolTipText(java.awt.event.MouseEvent event) {
            int r = rowAtPoint(event.getPoint()), c = columnAtPoint(event.getPoint());
            if (r < 0 || c < 0) return null;
            if (c == 7) return "Final White / Black material in pawn units: P=1, N/B=3, R=5, Q=9; kings excluded. " + getValueAt(r, c);
            if ((c == 2 || c == 3) && shown != null) {
                boolean a = ((Number) getValueAt(r, 0)).intValue() % 2 == 1;
                if (c == 3) a = !a;
                return ArenaRoundSummary.model(a ? shown.config().a() : shown.config().b(), a ? shown.current().a() : shown.current().b());
            }
            return String.valueOf(getValueAt(r, c));
        }
    };
    private LearningArenaState shown;
    private ActiveGameSnapshot displayed;
    private MaterialCount displayedMaterial;
    private ArenaMatchSummary summary;

    ArenaLiveView() {
        super(new BorderLayout()); setOpaque(false); setName("arenaLive");
        first.setForeground(ArenaScoreChart.FIRST); firstScore.setForeground(ArenaScoreChart.FIRST);
        second.setForeground(ArenaScoreChart.SECOND); secondScore.setForeground(ArenaScoreChart.SECOND);
        first.setName("arenaFirstModel"); second.setName("arenaSecondModel");
        firstScore.setName("arenaFirstScore"); secondScore.setName("arenaSecondScore");
        firstWdl.setName("arenaFirstWdl"); secondWdl.setName("arenaSecondWdl");
        progress.setName("arenaMatchProgress"); position.setName("arenaCurrentOrdinal");
        var scores = SeedTheme.panel(new GridLayout(1, 2, SeedTheme.scale(20), 0));
        scores.add(competitor(first, firstScore, firstWdl)); scores.add(competitor(second, secondScore, secondWdl));
        var summaryBody = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(10)));
        SeedTheme.padding(summaryBody, 8, 16, 8, 16); summaryBody.add(scores, BorderLayout.NORTH);
        var counts = SeedTheme.panel(new GridLayout(3, 1, 0, 5)); counts.add(progress); counts.add(position); counts.add(scoring); summaryBody.add(counts);
        summaryCard = SeedTheme.card("Current match", null, summaryBody);
        summaryCard.setToolTipText("Chess scoring: win = 1, draw = 0.5. Only complete valid pairs enter W-D-L and score. A/B identities stay fixed.");
        gameInfo.setName("arenaCurrentGame"); SeedTheme.padding(gameInfo, 10, 16, 10, 16);
        context.setOpaque(false); context.setName("arenaContext");
        context.add(SeedTheme.card("Current game", null, gameInfo), "match");
        context.add(SeedTheme.card("Training / optimization", null, optimization), "training");
        preventAutoScroll(optimization);
        var contextual = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(8)));
        contextual.add(context, BorderLayout.NORTH);
        activity.setName("arenaActivity"); contextual.add(activity);
        var scroll = new JScrollPane(contextual); scroll.setName("arenaContextScroll"); scroll.setBorder(null); scroll.setMinimumSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(SeedTheme.scale(24));
        var details = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(10)));
        details.add(summaryCard, BorderLayout.NORTH); details.add(scroll);
        details.setMinimumSize(new Dimension(SeedTheme.scale(400), 0));
        var top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, match, details);
        configure(top, "arenaLiveColumns", .48, 500); top.setMinimumSize(new Dimension(0, SeedTheme.scale(330)));
        table.setName("arenaGameHistory"); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); table.setRowHeight(SeedTheme.scale(26));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                var cell = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (!selected) {
                    Color color = SeedTheme.TEXT;
                    if (col == 2 || col == 3) {
                        boolean a = ((Number)t.getValueAt(row, 0)).intValue() % 2 == 1;
                        if (col == 3) a = !a;
                        color = a ? ArenaScoreChart.FIRST : ArenaScoreChart.SECOND;
                    }
                    cell.setForeground(color);
                }
                return cell;
            }
        });
        int[] widths = {50, 45, 135, 135, 75, 135, 50, 145, 95, 145, 270};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(SeedTheme.scale(widths[i]));
        var history = SeedTheme.card("Completed games · active round", null, new JScrollPane(table));
        history.setMinimumSize(new Dimension(0, SeedTheme.scale(135)));
        var split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, history) {
            private boolean initialized;
            @Override public void doLayout() {
                if (getHeight() > 0) {
                    int available = getHeight() - getDividerSize();
                    int max = Math.max(0, available - history.getMinimumSize().height);
                    int min = Math.min(max, top.getMinimumSize().height);
                    int desired = initialized ? getDividerLocation() : (int)(available * .74);
                    int bounded = Math.max(min, Math.min(max, desired));
                    if (bounded != getDividerLocation()) setDividerLocation(bounded);
                    initialized = true;
                }
                super.doLayout();
            }
        };
        configure(split, "arenaLiveRows", .74, 400); add(split);
        showUpdate(null);
    }
    private static void configure(JSplitPane split, String name, double weight, int initial) {
        split.setName(name); split.setBorder(null); split.setOpaque(false); split.setContinuousLayout(true);
        split.setDividerSize(SeedTheme.scale(14)); split.setResizeWeight(weight); split.setDividerLocation(SeedTheme.scale(initial));
    }
    void showUpdate(LearningArenaService.Update update) {
        var state = update == null ? null : update.state();
        match.competitors(state == null ? null : state.config());
        match.showGame(update == null ? null : update.liveGame(), update == null ? null : update.search());
        if (state == null) {
            shown = null; summary = null; displayed = null; rows.setRowCount(0);
            first.setText("Competitor A"); second.setText("Competitor B"); firstScore.setText("\u2014"); secondScore.setText("\u2014");
            firstWdl.setText("0 W / 0 D / 0 L"); secondWdl.setText("0 W / 0 D / 0 L");
            progress.setText("No campaign open"); position.setText(""); gameInfo.setText("Games appear during Arena phases."); activity.setText("");
            scoring.setText("Only completed valid pairs score");
            summaryCard.setVisible(true);
            ((CardLayout)context.getLayout()).show(context, "match"); return;
        }
        if (shown != state) {
            shown = state; summary = ArenaMatchSummary.from(state.config(), state.current());
            first.setText("A · " + ArenaMatchSummary.label(state.config(), true)); second.setText("B · " + ArenaMatchSummary.label(state.config(), false));
            first.setToolTipText(ArenaRoundSummary.model(state.config().a(), state.current().a()));
            second.setToolTipText(ArenaRoundSummary.model(state.config().b(), state.current().b()));
            firstScore.setText(summary.score(true)); secondScore.setText(summary.score(false));
            firstWdl.setText(summary.wins() + " W / " + summary.draws() + " D / " + summary.losses() + " L");
            secondWdl.setText(summary.losses() + " W / " + summary.draws() + " D / " + summary.wins() + " L");
            progress.setText("Games: " + summary.games() + " / " + state.config().arena().games() + "    Pairs: " + summary.pairs() + " / " + state.config().arena().games() / 2);
            scoring.setText("Valid pairs only · " + (summary.wins() + summary.draws() + summary.losses()) + " scored games · " + summary.unscoredPairs() + " unscored pairs");
            int selected = table.getSelectedRow(); rows.setRowCount(0);
            for (var row : summary.history()) rows.addRow(new Object[]{row.game(), row.pair(),
                    ArenaMatchSummary.label(state.config(), row.firstIsWhite()), ArenaMatchSummary.label(state.config(), !row.firstIsWhite()),
                    row.result(), row.winner(), row.plies(), row.material(), row.cumulativeWdl(), row.cumulativeScore(), row.termination()});
            if (selected >= 0 && selected < rows.getRowCount()) table.setRowSelectionInterval(selected, selected);
        }
        var game = update.liveGame();
        position.setText("Round " + state.current().number() + (game == null ? " · " + state.current().stage()
                : " · Game " + game.gameOrdinal() + " · Pair " + (game.gameOrdinal() + 1) / 2));
        boolean training = state.current().stage() == LearningArenaState.Stage.TRAIN_A || state.current().stage() == LearningArenaState.Stage.TRAIN_B;
        summaryCard.setVisible(!training);
        ((CardLayout)context.getLayout()).show(context, training ? "training" : "match");
        if (training) optimization.showProgress(update.optimization());
        if (game == null) {
            displayed = null;
            gameInfo.setText("No active game · " + state.status() + "\nScored games: " + (summary.wins() + summary.draws() + summary.losses())
                    + "\nUnscored pairs: " + summary.unscoredPairs() + "\nOnly completed valid pairs enter the match score.\nMaterial: pawn units, P=1 N/B=3 R=5 Q=9.");
        } else {
            String search = "";
            if (update.search() != null && update.search().gameOrdinal() == game.gameOrdinal() && update.search().lastMoveSearch().isPresent()) {
                var move = update.search().lastMoveSearch().get(); search = "\nLast search: depth " + move.depth() + " · " + move.nodes() + " nodes";
            } else if (game.evaluation() != null) search = "\nLast search: depth " + game.evaluation().depth();
            if (displayed != game) { displayed = game; displayedMaterial = MaterialCount.from(game.board()); }
            var material = displayedMaterial;
            gameInfo.setText("White: " + ArenaMatchSummary.label(state.config(), game.gameInPair() == 1)
                    + " · Black: " + ArenaMatchSummary.label(state.config(), game.gameInPair() != 1)
                    + "\n" + (game.sideToMove() == Value.WHITE ? "White" : "Black") + " to move · " + game.playedPlies() + " plies"
                    + " · Last: " + (game.lastMove() == 0 ? "\u2014" : Move.coordinate(game.lastMove()))
                    + search + "\nMaterial W / B: " + material.white() + " / " + material.black() + " (pawn units)");
        }
        activity.setText(state.config().name() + " · " + state.status() + "\n" + update.detail());
    }
    private static JPanel competitor(JLabel name, JLabel score, JLabel wdl) {
        var panel = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(4))); panel.add(name, BorderLayout.NORTH); panel.add(score); panel.add(wdl, BorderLayout.SOUTH); return panel;
    }
    private static JLabel label(String text, int size) {
        var label = SeedTheme.label(text, size, SeedTheme.TEXT); if (size > 15) label.setFont(SeedTheme.font(size, Font.BOLD)); return label;
    }
    private static JTextArea text(int lines) {
        var area = new JTextArea(lines, 24); area.setEditable(false); area.setOpaque(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        ((javax.swing.text.DefaultCaret)area.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        area.setFont(SeedTheme.font(13, Font.PLAIN)); return area;
    }
    private static void preventAutoScroll(Container parent) {
        for (var child : parent.getComponents()) {
            if (child instanceof JTextArea area) ((javax.swing.text.DefaultCaret)area.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
            else if (child instanceof Container container) preventAutoScroll(container);
        }
    }
}
