package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import com.ohinteractive.seedv6.training.validation.ValidationProgress;
import java.awt.*;
import javax.swing.*;

/** Read-only match view using the same board and move publications as Training validation. */
final class MatchView extends JPanel {
    private final BoardPanel board = new BoardPanel();
    private final JTextArea black = text(), white = text(), evaluation = text();
    private final JLabel status = new JLabel("No active game");
    private ActiveGameSnapshot displayed;
    private boolean cleared;
    private com.ohinteractive.seedv6.training.service.LearningArenaConfig competitors;
    void competitors(com.ohinteractive.seedv6.training.service.LearningArenaConfig config) { competitors = config; }
    MatchView(String prefix) {
        this(prefix, false);
    }
    MatchView(String prefix, boolean compact) {
        super(new BorderLayout(8, 8)); setName(prefix); setOpaque(false);
        board.setName(prefix + "Board"); board.setFocusable(false);
        black.setName(prefix + "Black"); white.setName(prefix + "White"); evaluation.setName(prefix + "Evaluation");
        var bottom = new JPanel(new BorderLayout(0, 6)); bottom.setOpaque(false);
        SeedTheme.padding(bottom, 10, 12, 10, 12); SeedTheme.padding(black, 10, 12, 4, 12);
        bottom.add(white, BorderLayout.NORTH);
        if (compact) {
            black.setRows(1); white.setRows(1);
            black.setFont(SeedTheme.font(14, Font.PLAIN)); white.setFont(SeedTheme.font(14, Font.PLAIN));
            black.setPreferredSize(new Dimension(1, SeedTheme.scale(32)));
            white.setPreferredSize(new Dimension(1, SeedTheme.scale(20)));
            SeedTheme.padding(bottom, 4, 12, 6, 12);
        } else { bottom.add(status); bottom.add(evaluation, BorderLayout.SOUTH); }
        var card = new SeedTheme.Card(); card.setLayout(new BorderLayout());
        card.add(black, BorderLayout.NORTH); card.add(board); card.add(bottom, BorderLayout.SOUTH); add(card);
        setMinimumSize(new Dimension(360, 300)); showGame(null, null);
    }
    void showGame(ActiveGameSnapshot game, ValidationProgress search) {
        if (game == null) {
            black.setText("Black: waiting for a match"); white.setText("White: waiting for a match");
            status.setText("No active game"); evaluation.setText("Evaluation unavailable");
            black.setToolTipText(null); white.setToolTipText(null);
            if (!cleared)
                board.showUnavailablePosition("No active Arena game. Positions appear during match phases.");
            displayed = null; cleared = true; return;
        }
        black.setText("Black: " + MatchPresentation.participant(game.black()));
        white.setText("White: " + MatchPresentation.participant(game.white()));
        if (competitors != null) {
            boolean aWhite = game.gameInPair() == 1;
            black.setText("Black: " + ArenaMatchSummary.label(competitors, !aWhite)); white.setText("White: " + ArenaMatchSummary.label(competitors, aWhite));
            black.setForeground(aWhite ? ArenaScoreChart.SECOND : ArenaScoreChart.FIRST);
            white.setForeground(aWhite ? ArenaScoreChart.FIRST : ArenaScoreChart.SECOND);
        }
        black.setToolTipText(MatchPresentation.participant(game.black()) + " / " + game.black().checkpointId());
        white.setToolTipText(MatchPresentation.participant(game.white()) + " / " + game.white().checkpointId());
        String activity = "Game " + game.gameOrdinal() + " \u00b7 Pair " + ((game.gameOrdinal() + 1) / 2) + " \u00b7 "
                + (game.sideToMove() == Value.WHITE ? "White" : "Black") + " to move \u00b7 " + game.playedPlies() + " plies";
        if (competitors == null && search != null && search.gameOrdinal() == game.gameOrdinal() && search.lastMoveSearch().isPresent()) {
            var move = search.lastMoveSearch().get(); activity += " \u00b7 depth " + move.depth() + " \u00b7 " + move.nodes() + " nodes";
        }
        status.setText(activity); status.setToolTipText(activity);
        if (displayed != game) {
            displayed = game; cleared = false;
            String caption = MatchPresentation.evaluationCaption(game, NetworkArchitecture.NNUE);
            evaluation.setText(competitors == null ? caption : caption.split("\n")[0]); evaluation.setToolTipText(caption);
            board.showTrainingPosition(game, MatchPresentation.score(game, NetworkArchitecture.NNUE), caption + ". Read-only match, White at bottom.");
        }
    }
    private static JTextArea text() {
        var area = new JTextArea(2, 30); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setOpaque(false); area.setFont(SeedTheme.font(12, Font.PLAIN));
        area.setPreferredSize(new Dimension(1, SeedTheme.scale(36))); return area;
    }
}
