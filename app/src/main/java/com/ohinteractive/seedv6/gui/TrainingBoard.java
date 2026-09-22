package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;
import com.formdev.flatlaf.util.ScaledImageIcon;
import com.ohinteractive.seedv6.training.service.TrainerSnapshot;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.core.move.Move;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboardModel.*;

/** Independent, read-only Training board over immutable move-boundary publications. */
final class TrainingBoard extends JPanel {
    private final JLabel identity = label("Network Training", 17, SeedTheme.TEXT);
    private final JLabel detail = label("No active training game", 12, SeedTheme.SECONDARY);
    private final JLabel sides = label("No active game", 14, SeedTheme.TEXT);
    private final JLabel game = label("Waiting to start", 12, SeedTheme.SECONDARY);
    private final JLabel timer = label("—", 17, SeedTheme.TEXT);
    private final JLabel phase = label("IDLE", 12, SeedTheme.GREEN);
    private final BoardPanel board = new BoardPanel();
    private ActiveGameSnapshot displayed;
    private boolean cleared;
    private final JTextArea evaluation = text("", 11, SeedTheme.SECONDARY);
    private final JTextArea message = text("", 12, SeedTheme.SECONDARY);

    TrainingBoard(Runnable diagnostics) {
        super(new BorderLayout(0, SeedTheme.scale(10))); setOpaque(false);
        JPanel boardCard = new SeedTheme.Card(); boardCard.setLayout(new BorderLayout());
        JPanel top = padded(new BorderLayout(SeedTheme.scale(12), 0), 12);
        top.add(new JLabel(new ScaledImageIcon(new ImageIcon(ApplicationIcons.windowImages().getLast()), 40, 40)), BorderLayout.WEST);
        JPanel names = panel(new GridLayout(2, 1, 0, SeedTheme.scale(5))); identity.setFont(SeedTheme.font(15, Font.BOLD)); identity.setName("trainingBlackIdentity"); names.add(identity); names.add(detail); top.add(names);
        top.add(phase, BorderLayout.EAST); boardCard.add(top, BorderLayout.NORTH);
        board.setName("trainingBoard"); board.setFocusable(false);
        board.showUnavailablePosition("No active training game. Live positions appear during self-play and validation.");
        boardCard.add(board);
        JPanel bottom = padded(new BorderLayout(SeedTheme.scale(10), 0), 10);
        JPanel labels = panel(new GridLayout(2, 1, 0, SeedTheme.scale(5))); labels.add(sides); labels.add(game); bottom.add(labels); bottom.add(timer, BorderLayout.EAST);
        evaluation.setName("trainingMoveEvaluation"); evaluation.setRows(2);
        sides.setName("trainingGameSides"); game.setName("trainingGameActivity"); boardCard.add(bottom, BorderLayout.SOUTH); add(boardCard);
        JPanel activity = padded(new BorderLayout(SeedTheme.scale(8), 0), 8);
        message.setRows(2); message.setName("trainingActivity");
        JPanel activityText = new ActivityText();
        activityText.add(evaluation, BorderLayout.NORTH); activityText.add(message);
        JScrollPane messagePane = scroll(activityText); messagePane.setPreferredSize(new Dimension(1, SeedTheme.scale(40)));
        messagePane.setName("trainingActivityScroll");
        messagePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        activity.add(messagePane);
        JButton open = new JButton("Diagnostics"); open.addActionListener(e -> diagnostics.run()); activity.add(open, BorderLayout.EAST);
        add(card("Training activity", null, activity), BorderLayout.SOUTH);
        setMinimumSize(new Dimension(SeedTheme.scale(360), 0));
    }

    void showState(TrainingController.ViewState view) {
        var s = view.snapshot();
        var live = view.phase() == TrainingController.Phase.RUNNING && s != null ? s.activeGame().orElse(null) : null;
        phase.setText(phase(view));
        if (live != null) {
            identity.setText("Black: " + participant(live.black()));
            sides.setText("White: " + participant(live.white()));
            identity.setToolTipText(live.black().checkpointId()); sides.setToolTipText(live.white().checkpointId());
            detail.setText(live.phase() == ActiveGameSnapshot.Phase.SELF_PLAY
                    ? "Self-play · Generation " + live.generation() + " · Game " + live.gameOrdinal() + " / " + view.settings().games()
                    : "Validation · Pair " + ((live.gameOrdinal() + 1) / 2) + " · Game " + live.gameInPair() + " / 2");
            game.setText((live.sideToMove() == Value.WHITE ? "White" : "Black") + " to move · " + live.playedPlies()
                    + " plies" + (live.lastMove() == 0 ? "" : " · Last " + Move.coordinate(live.lastMove())));
            timer.setText(timer(Math.max(0, System.nanoTime() - live.startedNanos()) / 1_000_000_000));
            timer.setToolTipText("Active game elapsed time; not a chess clock");
            if (displayed != live) {
                displayed = live; cleared = false;
                String caption = evaluationCaption(live, view.settings().architecture());
                evaluation.setText(caption);
                board.showTrainingPosition(live, score(live, view.settings().architecture()), caption + ". Read-only training position, White at bottom.");
            }
        } else {
            identity.setText(s == null ? "Network Training" : "Training · Generation " + s.generation());
            detail.setText(s == null ? "No network loaded" : "Latest training " + network(s.latestTrainingId()));
            identity.setToolTipText(null); sides.setToolTipText(null);
            sides.setText("No active game");
            game.setText("Live positions during self-play and validation");
            timer.setText("—"); timer.setToolTipText(null);
            evaluation.setText("Evaluation unavailable · no active game");
            if (!cleared) {
                board.showUnavailablePosition("No active training game. Live positions appear during self-play and validation.");
                displayed = null; cleared = true;
            }
        }
        message.setForeground(view.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.SECONDARY);
        if (!message.getText().equals(view.message())) { message.setText(view.message()); message.setCaretPosition(0); }
    }

    static String participant(ActiveGameSnapshot.Participant p) {
        return switch (p.role()) {
            case LATEST_TRAINING -> "Latest Training ";
            case CANDIDATE -> "Candidate ";
            case BEST -> "Best ";
        } + network(p.checkpointId());
    }
    static PlayScore score(ActiveGameSnapshot game, NetworkArchitecture architecture) {
        var e = game.evaluation();
        if (e == null) return new PlayScore("—", 0.5, false);
        int raw = e.whiteScore();
        if (com.ohinteractive.seedv6.search.tt.TranspositionScores.isMateScore(raw)) {
            int moves = (com.ohinteractive.seedv6.search.tt.TranspositionScores.MATE_SCORE - Math.abs(raw) + 1) / 2;
            return new PlayScore((raw > 0 ? "+M" : "−M") + moves, raw > 0 ? 1 : 0, true);
        }
        return new PlayScore(String.format(java.util.Locale.ROOT, "%+d", raw),
                architecture.evaluationBar(raw), true);
    }
    private static String evaluationCaption(ActiveGameSnapshot game, NetworkArchitecture architecture) {
        var e = game.evaluation();
        if (e == null) return "Evaluation unavailable · no completed search for this move";
        return "Last move search · White " + score(game, architecture).text() + " · " + architecture.evaluationUnits() + "\n"
                + participant(game.searchingParticipant()) + " (" + (e.searchingSide() == Value.WHITE ? "White" : "Black")
                + ") · depth " + e.depth() + " · before " + Move.coordinate(game.lastMove());
    }

    /** Track narrow viewports so both passive text areas wrap instead of being clipped horizontally. */
    private static final class ActivityText extends JPanel implements Scrollable {
        ActivityText() { super(new BorderLayout(0, SeedTheme.scale(4))); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return SeedTheme.scale(16); }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(1, r.height - SeedTheme.scale(16)); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
