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
    MatchView(String prefix) {
        super(new BorderLayout(8, 8)); setOpaque(false);
        board.setName(prefix + "Board"); board.setFocusable(false);
        black.setName(prefix + "Black"); white.setName(prefix + "White"); evaluation.setName(prefix + "Evaluation");
        var bottom = new JPanel(new BorderLayout(0, 6)); bottom.setOpaque(false);
        bottom.add(white, BorderLayout.NORTH); bottom.add(status); bottom.add(evaluation, BorderLayout.SOUTH);
        add(black, BorderLayout.NORTH); add(board); add(bottom, BorderLayout.SOUTH);
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
        black.setToolTipText(game.black().checkpointId()); white.setToolTipText(game.white().checkpointId());
        String activity = "Game " + game.gameOrdinal() + " \u00b7 Pair " + ((game.gameOrdinal() + 1) / 2) + " \u00b7 "
                + (game.sideToMove() == Value.WHITE ? "White" : "Black") + " to move \u00b7 " + game.playedPlies() + " plies";
        if (search != null && search.gameOrdinal() == game.gameOrdinal() && search.lastMoveSearch().isPresent()) {
            var move = search.lastMoveSearch().get(); activity += " \u00b7 depth " + move.depth() + " \u00b7 " + move.nodes() + " nodes";
        }
        status.setText(activity); status.setToolTipText(activity);
        if (displayed != game) {
            displayed = game; cleared = false;
            String caption = MatchPresentation.evaluationCaption(game, NetworkArchitecture.NNUE);
            evaluation.setText(caption);
            board.showTrainingPosition(game, MatchPresentation.score(game, NetworkArchitecture.NNUE), caption + ". Read-only match, White at bottom.");
        }
    }
    private static JTextArea text() {
        var area = new JTextArea(2, 30); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setOpaque(false); area.setFont(SeedTheme.font(12, Font.PLAIN));
        area.setPreferredSize(new Dimension(1, SeedTheme.scale(36))); return area;
    }
}
