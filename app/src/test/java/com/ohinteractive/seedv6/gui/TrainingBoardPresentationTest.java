package com.ohinteractive.seedv6.gui;

import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed;
import com.ohinteractive.seedv6.training.service.TrainerSnapshot;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static com.ohinteractive.seedv6.training.telemetry.ActiveGameFeedTest.move;
import static org.junit.jupiter.api.Assertions.*;

class TrainingBoardPresentationTest {
    @Test void snapshotStateGuardsAndEdtRendererClearAllInactivePhasesAndKeepPlayIndependent() throws Exception {
        edt(() -> {
            SeedTheme.initialize();
            var feed = new ActiveGameFeed(); feed.validation(187, TrainingDashboardTest.CANDIDATE, TrainingDashboardTest.BEST);
            var game = new HeadlessGame(Board.startingPosition(), 20); feed.start(game, 34, 2);
            var pane = new TrainingBoard(() -> {}); var board = named(pane, "trainingBoard", BoardPanel.class);
            var play = new BoardPanel(); play.showTrainingPosition(feed.latest(), TrainingBoard.score(feed.latest()), "independent renderer");
            var playBoard = play.displayedBoard();
            var base = TrainingDashboardTest.snapshot(TrainerSnapshot.State.VALIDATING, false, false, false);
            pane.showState(TrainingDashboardTest.view(base.withActiveGame(feed.latest())));
            assertFalse(board.positionUnavailable()); assertArrayEquals(Board.startingPosition(), board.displayedBoard());
            assertEquals("White: Best Gen 181", named(pane, "trainingGameSides", JLabel.class).getText());
            long move = move(game, "e2e4"); game.play(move);
            feed.moved(game, move, new SearchResult(move, true, 200, 4, 10, 1, true));
            var live = base.withActiveGame(feed.latest()); pane.showState(TrainingDashboardTest.view(live));
            assertArrayEquals(game.boardSnapshot(), board.displayedBoard()); assertArrayEquals(playBoard, play.displayedBoard());
            assertTrue(named(pane, "trainingMoveEvaluation", JTextArea.class).getText().contains("White +200"));
            assertTrue(named(pane, "trainingMoveEvaluation", JTextArea.class).getText().contains("Best Gen 181 (White)"));
            assertTrue(named(pane, "trainingGameActivity", JLabel.class).getText().contains("Black to move"));
            for (var state : TrainerSnapshot.State.values()) {
                if (state == TrainerSnapshot.State.VALIDATING) continue;
                var inactive = TrainingDashboardTest.snapshot(state, false, false, false).withActiveGame(feed.latest());
                assertTrue(inactive.activeGame().isEmpty(), state.toString());
                pane.showState(TrainingDashboardTest.view(inactive)); assertTrue(board.positionUnavailable());
                assertTrue(named(pane, "trainingMoveEvaluation", JTextArea.class).getText().contains("unavailable"));
            }
            var next = TrainingDashboardTest.snapshot(TrainerSnapshot.State.VALIDATING, false, false, true).withActiveGame(feed.latest());
            assertTrue(next.activeGame().isEmpty(), "Previous generation must never appear current");
            pane.showState(TrainingDashboardTest.view(live));
            pane.showState(new TrainingController.ViewState(TrainingDashboardTest.view(live).settings(),
                    TrainingController.Phase.CLOSING, live, "Closing", true, false, true, 4, ""));
            assertTrue(board.positionUnavailable());
        });
    }
}
