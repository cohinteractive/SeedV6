package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.CapturedPieces;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.training.telemetry.ActiveGameFeedTest.move;
import static org.junit.jupiter.api.Assertions.*;

class UiObservabilityTest {
    @Test void captureHistoryAndCurrentMaterialAreIndependentOfStartingMaterialAndPromotions() {
        var session = GameSession.startingPosition();
        play(session, "e2e4"); play(session, "d7d5"); play(session, "e4d5");
        assertEquals(1, session.capturedPieces().count(Piece.PAWN | 8));
        assertEquals(1, BoardMaterial.balance(session.boardSnapshot(), Value.WHITE));
        assertEquals(-1, BoardMaterial.balance(session.boardSnapshot(), Value.BLACK));
        assertTrue(session.capturedPieces().fromStartingPosition());
        assertEquals(0, GameSession.startingPosition().capturedPieces().count(Piece.PAWN | 8));

        session = GameSession.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        assertFalse(session.capturedPieces().fromStartingPosition());
        play(session, "e5d6");
        assertEquals(1, session.capturedPieces().count(Piece.PAWN | 8));
        assertEquals(1, BoardMaterial.balance(session.boardSnapshot(), Value.WHITE));

        session = GameSession.fromFen("1rr4k/P7/8/8/8/8/8/7K w - - 0 1");
        play(session, "a7b8q");
        assertEquals(1, session.capturedPieces().count(Piece.ROOK | 8));
        assertEquals(0, session.capturedPieces().count(Piece.PAWN));
        assertEquals(4, BoardMaterial.balance(session.boardSnapshot(), Value.WHITE));
        play(session, "c8b8");
        assertEquals(1, session.capturedPieces().count(Piece.QUEEN));
        assertEquals(-5, BoardMaterial.balance(session.boardSnapshot(), Value.WHITE));

        session = GameSession.fromFen("7k/P7/8/8/8/8/8/7K w - - 0 1");
        play(session, "a7a8q");
        assertEquals(0, session.capturedPieces().count(Piece.PAWN));
        assertEquals(9, BoardMaterial.balance(session.boardSnapshot(), Value.WHITE));
    }

    @Test void loadedPositionsDoNotFabricateCapturesAndHistoryIsImmutable() {
        var custom = GameSession.fromFen("7k/8/8/8/8/8/8/KQ6 w - - 0 1");
        for (int type : BoardMaterial.ORDER) {
            assertEquals(0, custom.capturedPieces().count(type));
            assertEquals(0, custom.capturedPieces().count(type | 8));
        }
        assertEquals(9, BoardMaterial.balance(custom.boardSnapshot(), Value.WHITE));
        var session = GameSession.startingPosition();
        CapturedPieces before = session.capturedPieces();
        play(session, "e2e4"); play(session, "d7d5"); play(session, "e4d5");
        assertEquals(0, before.count(Piece.PAWN | 8));
        assertEquals("(+9)", BoardMaterial.signed(9));
        assertEquals("(-5)", BoardMaterial.signed(-5));
        assertEquals("(0)", BoardMaterial.signed(0));
    }

    @Test void feedRetainsAllCapturesAcrossUnsampledMovesAndResetsBetweenGames() {
        var feed = new ActiveGameFeed(); feed.validation(1, "candidate", "best");
        var game = new HeadlessGame(Board.startingPosition(), 30);
        feed.start(game, 1, 1);
        for (var coordinate : new String[]{"e2e4", "d7d5", "e4d5", "d8d5"}) {
            long move = move(game, coordinate); game.play(move); feed.moved(game, move, null);
        }
        assertEquals(1, feed.latest().captured().count(Piece.PAWN | 8));
        assertEquals(1, feed.latest().captured().count(Piece.PAWN));
        feed.start(new HeadlessGame(Board.startingPosition(), 30), 2, 2);
        assertEquals(0, feed.latest().captured().count(Piece.PAWN | 8));
        assertEquals(0, feed.latest().captured().count(Piece.PAWN));
    }

    @Test void candidateAccentFollowsRoleAcrossColourSwapsAndClearsOnOtherViews() throws Exception {
        edt(() -> {
            SeedTheme.initialize();
            var board = new BoardPanel();
            var feed = new ActiveGameFeed(); feed.validation(1, "candidate", "best");
            var game = new HeadlessGame(Board.startingPosition(), 30);
            feed.start(game, 1, 1);
            board.showTrainingPosition(feed.latest(), new PlayScore("—", .5, false), "validation");
            assertEquals(Value.WHITE, board.displayedCandidateSide());
            feed.start(game, 2, 2);
            board.showTrainingPosition(feed.latest(), new PlayScore("—", .5, false), "validation");
            assertEquals(Value.BLACK, board.displayedCandidateSide());
            feed.selfPlay(2, "latest"); feed.start(game, 1, 0);
            board.showTrainingPosition(feed.latest(), new PlayScore("—", .5, false), "self-play");
            assertEquals(-1, board.displayedCandidateSide());
            board.showUnavailablePosition("Waiting");
            assertEquals(-1, board.displayedCandidateSide());
            assertEquals(0, board.displayedCaptures().count(Piece.PAWN));
        });
    }

    @Test void tintPreservesAlphaAndLightDarkIdentity() {
        var source = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, new Color(245, 245, 245, 200).getRGB());
        source.setRGB(1, 0, new Color(30, 30, 30, 255).getRGB());
        source.setRGB(2, 0, 0);
        var tint = PieceRenderer.tint(source);
        var white = new Color(tint.getRGB(0, 0), true);
        var black = new Color(tint.getRGB(1, 0), true);
        assertEquals(200, white.getAlpha()); assertEquals(255, black.getAlpha());
        assertEquals(0, tint.getRGB(2, 0) >>> 24);
        assertTrue(white.getRed() - black.getRed() > 140);
        assertTrue(white.getBlue() > white.getGreen());
        assertTrue(black.getBlue() > black.getGreen());
        assertNotEquals(source.getRGB(0, 0), tint.getRGB(0, 0));
    }

    @Test void simultaneousActivityAndAnimationClearWithoutLosingIdleStatus() throws Exception {
        edt(() -> {
            SeedTheme.initialize();
            var tabs = new JTabbedPane();
            tabs.addTab("Play", new JPanel()); tabs.addTab("Network Training", new JPanel()); tabs.addTab("Arena", new JPanel());
            try (var activity = new WorkspaceActivityTabs(tabs)) {
                assertFalse(activity.animating());
                assertEquals("White to move", activity.status("White to move"));
                activity.update(0, new WorkspaceActivity(true, "Black searching · Depth 7"));
                activity.update(1, new WorkspaceActivity(true, "Gen 2 · VALIDATING"));
                activity.update(2, new WorkspaceActivity(true, "Round 3 · MATCH"));
                assertTrue(activity.animating());
                var text = activity.status("idle");
                assertTrue(text.contains("Play — Black searching")); assertTrue(text.contains("Network Training — Gen 2"));
                assertTrue(text.contains("Arena — Round 3"));
                activity.update(0, WorkspaceActivity.idle()); activity.update(1, WorkspaceActivity.idle());
                assertTrue(activity.animating()); activity.update(2, WorkspaceActivity.idle());
                assertFalse(activity.animating()); assertEquals("idle", activity.status("idle"));
            }
        });
    }

    @Test void threadsRenderActualParticipantsAlongsideCapacity() throws Exception {
        edt(() -> {
            SeedTheme.initialize(); var card = new EngineCard();
            card.showSearch(GameController.SearchInfo.thinking(Value.BLACK).withThreads(3), PlayEvaluator.handcrafted(), 8);
            assertEquals("3/8", TrainingWorkspaceSmokeTest.named(card, "engineThreads", JLabel.class).getText());
            card.showSearch(GameController.SearchInfo.idle(), PlayEvaluator.handcrafted(), 8);
            assertEquals("0/8", TrainingWorkspaceSmokeTest.named(card, "engineThreads", JLabel.class).getText());
        });
    }

    @Test void trainingActivityFollowsWorkRatherThanAnOpenSessionOrConfirmation() {
        var snapshot = TrainingDashboardTest.snapshot(com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.VALIDATING, false, false, false);
        var running = TrainingDashboardTest.view(snapshot);
        var feed = new ActiveGameFeed(); feed.validation(187, "candidate", "best");
        feed.start(new HeadlessGame(Board.startingPosition(), 30), 37, 1);
        var active = WorkspaceActivity.training(TrainingDashboardTest.view(snapshot.withActiveGame(feed.latest())));
        assertTrue(active.active()); assertTrue(active.detail().contains("Gen 187")); assertTrue(active.detail().contains("Game 37/64"));
        for (var phase : new TrainingController.Phase[]{TrainingController.Phase.IDLE, TrainingController.Phase.CONFIRM_DEPTH,
                TrainingController.Phase.STOPPED, TrainingController.Phase.FAILED}) {
            var idle = new TrainingController.ViewState(running.settings(), phase, snapshot, "",
                    phase == TrainingController.Phase.CONFIRM_DEPTH, true, true, 4, "");
            assertFalse(WorkspaceActivity.training(idle).active());
        }
    }

    private static void play(GameSession session, String coordinate) {
        var promotion = coordinate.length() == 5 ? Promotion.QUEEN : Promotion.NONE;
        session.applyIntent(square(coordinate.substring(0, 2)), square(coordinate.substring(2, 4)), promotion);
    }

    private static int square(String value) { return value.charAt(0) - 'a' + (value.charAt(1) - '1') * 8; }
}
