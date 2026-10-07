package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class HceArenaGuiTest {
    @TempDir Path temporary;
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2})
    void matchOnlySetupRunsWithoutTrainingDataAndRestoresReadOnlyHceParticipants(int orientation) throws Exception {
        var preferences = Preferences.userRoot().node("seedv6-hce-test-" + UUID.randomUUID());
        preferences.put("baseTrainingRoot", temporary.toString());
        var folders = new TrainingFolders(preferences);
        var panel = edt(() -> new LearningArenaPanel(folders));
        try {
            String checkpoint = orientation == 0 ? "" : PlayStoreWorkflowTest.brnStore(temporary.resolve("network"));
            edt(() -> {
                named(panel, "arenaMode", JComboBox.class).setSelectedItem(Mode.MATCH_ONLY);
                named(panel, "arenaFixedAEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
                named(panel, "arenaFixedBEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
                assertFalse(named(panel, "arenaFixedANetwork", JComboBox.class).isEnabled());
                assertFalse(named(panel, "arenaFixedBNetwork", JComboBox.class).isEnabled());
                if (orientation != 0) {
                    var network = named(panel, orientation == 1 ? "arenaFixedAEngineSetup" : "arenaFixedBEngineSetup", PlayEnginePanel.class);
                    network.evaluator.setSelectedItem(PlayEvaluator.Mode.BEST_NNUE);
                    network.selectStore(temporary.resolve("network"));
                }
            });
            until(() -> edt(() -> named(panel, "arenaCreate", JButton.class).isEnabled()));
            edt(() -> {
                assertTrue(named(panel, "arenaCreate", JButton.class).isEnabled());
                named(panel, "arenaGames", JSpinner.class).setValue(2);
                named(panel, "arenaDepth", JSpinner.class).setValue(1);
                named(panel, "arenaOpeningMax", JSpinner.class).setValue(0);
                named(panel, "arenaFen", JTextField.class).setText(SHORT_GAME);
                named(panel, "arenaCreate", JButton.class).doClick();
            });
            until(() -> edt(() -> { panel.poll(); return named(panel, "arenaStatus", JLabel.class).getText().contains("COMPLETE"); }));
            Path root = folders.arenaCampaign(); assertNotNull(root);
            var state = LearningArenaState.read(root); assertTrue(state.config().matchOnly());
            assertNull(state.config().source()); assertEquals(orientation != 1, state.config().a().isHandcrafted()); assertEquals(orientation != 2, state.config().b().isHandcrafted());
            if (orientation != 0) assertEquals(checkpoint, (orientation == 1 ? state.config().a() : state.config().b()).initialModel().checkpoint());
            edt(() -> {
                assertEquals(orientation == 0 ? "A · HCE A" : orientation == 1 ? "A · BRN-2" : "A · HCE", named(panel, "arenaFirstModel", JLabel.class).getText());
                assertEquals(2, named(panel, "arenaGameHistory", JTable.class).getRowCount());
                assertEquals(1, named(panel, "arenaHistory", JTable.class).getRowCount());
                assertTrue(named(panel, "arenaHistoryView", ArenaHistoryView.class).detailsText(0).contains("no checkpoint or training"));
                panel.openCampaign(root);
            });
            until(() -> edt(() -> { panel.poll(); return named(panel, "arenaStatus", JLabel.class).getText().contains("COMPLETE"); }));
            edt(() -> {
                assertEquals(Mode.MATCH_ONLY, named(panel, "arenaMode", JComboBox.class).getSelectedItem());
                assertFalse(named(panel, "arenaMode", JComboBox.class).isEnabled());
                assertFalse(named(panel, "arenaFixedAEvaluator", JComboBox.class).isEnabled());
                assertFalse(named(panel, "arenaResume", JButton.class).isEnabled());
                named(panel, "arenaStart", JButton.class).doClick();
                assertEquals(Mode.LEARNING, named(panel, "arenaMode", JComboBox.class).getSelectedItem());
                assertTrue(named(panel, "arenaMode", JComboBox.class).isEnabled());
            });
        } finally { edt(panel::beginShutdown).run(); preferences.removeNode(); }
    }
    @Test void liveHceScoreAndParticipantIdentityUseHandcraftedUnits() {
        var hce = new ActiveGameSnapshot.Participant(ActiveGameSnapshot.Role.HCE, "");
        var game = new ActiveGameSnapshot(ActiveGameSnapshot.Phase.ARENA, 0, 1, 2, 2, 2, 1,
                hce, hce, Board.startingPosition(), 0, new ActiveGameSnapshot.MoveEvaluation(125, Value.BLACK, 1, 0), 1, 2);
        assertEquals("+1.25", MatchPresentation.score(game, NetworkArchitecture.NNUE).text());
        assertTrue(MatchPresentation.evaluationCaption(game, NetworkArchitecture.NNUE).contains("HCE pawns"));
        assertEquals("HCE (Handcrafted evaluator)", MatchPresentation.participant(hce));
    }
    @Test void nativePlayAndArenaSelectionsRenderAndPinIndependentHceParticipants() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path network = temporary.resolve("network"); PlayStoreWorkflowTest.brnStore(network);
        var frame = edt(() -> new ChessFrame(settings(network, 1, 1), new TrainingController.Backend(), ignored -> {}));
        try {
            edt(() -> {
                frame.setVisible(true); frame.setSize(1440, 950);
                named(frame, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
                named(frame, "whiteEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
                named(frame, "playThreads", JSpinner.class).setValue(1); named(frame, "playDepth", JSpinner.class).setValue(1);
            });
            until(() -> edt(() -> named(frame, "startGame", JButton.class).isEnabled()));
            edt(() -> named(frame, "startGame", JButton.class).doClick());
            until(() -> edt(() -> named(frame, "blackPlayerNetwork", JLabel.class).getText().contains("BRN-2")));
            edt(() -> {
                named(frame, "stopGame", JButton.class).doClick();
                assertEquals("Handcrafted evaluator", named(frame, "whitePlayerNetwork", JLabel.class).getText());
                named(frame, "whiteEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.BEST_NNUE);
                named(frame, "blackEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
                assertEquals("Handcrafted evaluator", named(frame, "whitePlayerNetwork", JLabel.class).getText());
                frame.showSearch(new GameController.SearchInfo("Idle", 1, "cp 100", 1, 1, "e2e4", "DEPTH", 1, Value.WHITE));
                assertTrue(named(frame, "searchTermination", JLabel.class).getText().contains("White · HCE"));
            });
            until(() -> edt(() -> named(frame, "startGame", JButton.class).isEnabled()));
            edt(() -> named(frame, "startGame", JButton.class).doClick());
            until(() -> edt(() -> named(frame, "whitePlayerNetwork", JLabel.class).getText().contains("BRN-2")));
            edt(() -> {
                named(frame, "stopGame", JButton.class).doClick();
                assertEquals("Handcrafted evaluator", named(frame, "blackPlayerNetwork", JLabel.class).getText());
                for (var size : new Dimension[]{new Dimension(1440, 950), new Dimension(1100, 760)}) {
                    frame.setSize(size); frame.validate();
                    capture(frame, "play-" + size.width);
                }
                named(frame, "workspaces", JTabbedPane.class).setSelectedIndex(2);
            });
            edt(() -> {
                named(frame, "arenaMode", JComboBox.class).setSelectedItem(Mode.MATCH_ONLY);
                named(frame, "arenaFixedAEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
                named(frame, "arenaFixedBEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
            });
            edt(() -> {
                for (var size : new Dimension[]{new Dimension(1440, 950), new Dimension(1100, 760)}) {
                    frame.setSize(size); frame.validate();
                    named(frame, "learningArena", LearningArenaPanel.class).showSetupTop();
                    capture(frame, "arena-" + size.width);
                    var control = named(frame, "arenaFixedAEvaluator", JComboBox.class);
                    assertTrue(control.isShowing()); assertTrue(control.getWidth() >= 130);
                    assertTrue(control.getVisibleRect().contains(new Rectangle(0, 0, control.getWidth(), control.getHeight())));
                }
            });
        } finally {
            edt(() -> frame.dispatchEvent(new java.awt.event.WindowEvent(frame, java.awt.event.WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
    }
    private static void capture(JFrame frame, String name) {
        try {
            Path directory = Path.of("build/gui-smoke/hce"); Files.createDirectories(directory);
            var image = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics(); frame.paintAll(graphics); graphics.dispose();
            javax.imageio.ImageIO.write(image, "png", directory.resolve(name + ".png").toFile());
        } catch (Exception failure) { throw new AssertionError(failure); }
    }
}
