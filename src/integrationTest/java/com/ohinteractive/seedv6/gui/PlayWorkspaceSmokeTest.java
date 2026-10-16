package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual Swing window/actions/rendering; screenshots are comparison evidence, not pixel assertions. */
class PlayWorkspaceSmokeTest {
    @TempDir Path temp;
    private ChessFrame frame;

    @Test void terminationControlsAndCandidateColourSwapsRenderThroughTheDesktopShell() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        frame = edt(() -> new ChessFrame(settings(temp.resolve("unused"), 1, 1), new TrainingController.Backend(), ignored -> {}));
        var field = ChessFrame.class.getDeclaredField("controller"); field.setAccessible(true);
        var trainingPublication = ChessFrame.class.getDeclaredMethod("showTraining", TrainingController.ViewState.class);
        trainingPublication.setAccessible(true);
        GameController controller = edt(() -> (GameController) field.get(frame));
        edt(() -> {
            frame.setVisible(true); frame.validate();
            named(frame, "playThreads", JSpinner.class).setValue(4);
            combo("searchLimit").setSelectedItem(GameController.LimitKind.MOVETIME);
            named(frame, "playMovetime", JSpinner.class).setValue(2000L);
            combo("humanSide").setSelectedItem(GameController.HumanSide.BLACK);
            controller.loadFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
            assertTrue(named(frame, "stopGame", JButton.class).isEnabled());
            assertTrue(named(frame, "resign", JButton.class).isEnabled());
        });
        until(() -> edt(() -> {
            controller.pollSearchTelemetry();
            return !named(frame, "engineThreads", JLabel.class).getText().equals("0/4");
        }));
        edt(() -> {
            var board = controller.boardSnapshot();
            JTabbedPane tabs = named(frame, "workspaces", JTabbedPane.class);
            tabs.setSelectedIndex(1);
            assertTrue(named(frame, "workspaceStatus", JLabel.class).getText().contains("Play — White searching"));
            tabs.setSelectedIndex(0);
            named(frame, "stopGame", JButton.class).doClick();
            assertArrayEquals(board, controller.boardSnapshot());
            assertEquals(PositionStatus.Outcome.STOPPED, controller.positionStatus().outcome());
            assertEquals("0/4", named(frame, "engineThreads", JLabel.class).getText());
            assertFalse(named(frame, "stopGame", JButton.class).isEnabled());
            assertFalse(named(frame, "resign", JButton.class).isEnabled());
            capture("play-stopped.png");
            named(frame, "newGame", JButton.class).doClick();
            named(frame, "resign", JButton.class).doClick();
            assertEquals(PositionStatus.Outcome.BLACK_RESIGNED, controller.positionStatus().outcome());
            assertTrue(named(frame, "workspaceStatus", JLabel.class).getText().contains("White wins by resignation"));

            var feed = new com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed();
            feed.validation(187, TrainingDashboardTest.CANDIDATE, TrainingDashboardTest.BEST);
            var game = new com.ohinteractive.seedv6.training.selfplay.HeadlessGame(com.ohinteractive.seedv6.core.Board.startingPosition(), 30);
            feed.start(game, 33, 1);
            for (var coordinate : new String[]{"e2e4", "d7d5", "e4d5", "d8d5"}) {
                long move = com.ohinteractive.seedv6.training.telemetry.ActiveGameFeedTest.move(game, coordinate);
                game.play(move); feed.moved(game, move, null);
            }
            var pane = named(frame, "trainingBoard", BoardPanel.class);
            var snapshot = TrainingDashboardTest.snapshot(com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.VALIDATING, false, false, false);
            tabs.setSelectedIndex(1); frame.validate();
            try { trainingPublication.invoke(frame, TrainingDashboardTest.view(snapshot.withActiveGame(feed.latest()))); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            assertEquals(com.ohinteractive.seedv6.core.util.Value.WHITE, pane.displayedCandidateSide());
            assertEquals(PieceRenderer.CANDIDATE_ACCENT, named(frame, "trainingGameSides", JLabel.class).getForeground());
            capture("candidate-white.png");
            tabs.setSelectedIndex(0);
            assertTrue(named(frame, "workspaceStatus", JLabel.class).getText().contains("Network Training — Gen 187"));
            capture("background-validation.png");
            tabs.setSelectedIndex(1);
            feed.start(game, 34, 2);
            try { trainingPublication.invoke(frame, TrainingDashboardTest.view(snapshot.withActiveGame(feed.latest()))); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            assertEquals(com.ohinteractive.seedv6.core.util.Value.BLACK, pane.displayedCandidateSide());
            assertEquals(PieceRenderer.CANDIDATE_ACCENT, named(frame, "trainingBlackIdentity", JLabel.class).getForeground());
            capture("candidate-black.png");
        });
    }

    @AfterEach void close() throws Exception {
        if (frame != null) {
            edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
    }

    @Test void playBoardInputScoresheetLimitsAndResizingRemainUsable() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        frame = edt(() -> new ChessFrame(settings(temp.resolve("unused"), 1, 1), new TrainingController.Backend(), ignored -> {}));
        edt(() -> {
            frame.setVisible(true); frame.setSize(SeedTheme.scale(1440), SeedTheme.scale(950)); frame.validate();
            combo("gameMode").setSelectedItem(GameController.GameMode.HUMAN_VS_HUMAN);
            move("e2e4"); move("e7e5"); move("g1f3");
            JTable table = named(frame, "moveHistory", JTable.class);
            assertEquals(2, table.getRowCount()); assertEquals("e2e4", table.getValueAt(0, 1));
            assertEquals("e7e5", table.getValueAt(0, 2)); assertEquals("g1f3", table.getValueAt(1, 1));
            assertFalse(table.isCellEditable(0, 1));
            assertLayout();
            named(frame, "playDepth", JSpinner.class).setValue(3);
            combo("gameMode").setSelectedItem(GameController.GameMode.HUMAN_VS_ENGINE);
        });
        until(() -> edt(() -> !named(frame, "moveHistory", JTable.class).getValueAt(1, 2).toString().isEmpty()));
        edt(() -> {
            // Book decisions have a legal move/PV but deliberately have no searched evaluation.
            if (!named(frame, "engineState", JLabel.class).getText().contains("Opening book"))
                assertFalse(named(frame, "engineScore", JLabel.class).getText().equals("—"));
            assertFalse(named(frame, "principalVariation", JTextArea.class).getText().equals("—"));
            frame.setSize(SeedTheme.scale(1586), SeedTheme.scale(992)); frame.validate(); assertLayout();
            capture("play-reference.png");
            JTabbedPane tabs = named(frame, "workspaces", JTabbedPane.class);
            tabs.setSelectedIndex(1); frame.validate();
            assertTrue(named(frame, "trainingBoard", BoardPanel.class).isShowing());
            assertFalse(named(frame, "chessBoard", BoardPanel.class).isShowing());
            tabs.setSelectedIndex(0); frame.validate(); assertLayout();
            assertEquals(2, named(frame, "moveHistory", JTable.class).getRowCount());
            combo("searchLimit").setSelectedItem(GameController.LimitKind.MOVETIME);
            assertTrue(named(frame, "playMovetime", JSpinner.class).isShowing());
            assertFalse(named(frame, "playDepth", JSpinner.class).isShowing());
            combo("searchLimit").setSelectedItem(GameController.LimitKind.DEPTH);
            for (Dimension size : new Dimension[] {new Dimension(1100, 760), new Dimension(1920, 1080)}) {
                frame.setSize(SeedTheme.scale(size.width), SeedTheme.scale(size.height)); frame.validate(); assertLayout();
                capture("play-" + size.width + "x" + size.height + ".png");
            }
            named(frame, "newGame", JButton.class).doClick(); assertEquals(0, named(frame, "moveHistory", JTable.class).getRowCount());
            assertFalse(Files.exists(temp.resolve("unused")), "Play startup must not create a trainer store");
        });
    }

    @Test void threadsSpinnerAndEngineStatusFollowTheEffectiveProductionSetting() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        frame = edt(() -> new ChessFrame(settings(temp.resolve("unused"), 1, 1), new TrainingController.Backend(), ignored -> {}));
        edt(() -> {
            frame.setVisible(true); frame.validate();
            JSpinner threads = named(frame, "playThreads", JSpinner.class);
            JLabel state = named(frame, "engineState", JLabel.class);
            assertTrue(threads.getToolTipText().contains("including the main worker"));
            for (int workers : new int[] {1, 2, 4, 1}) {
                threads.setValue(workers);
                assertEquals("●  Idle — configured for up to " + workers
                        + (workers == 1 ? " thread" : " threads"), state.getText());
            }
            threads.setValue(2);
            named(frame, "newGame", JButton.class).doClick();
            assertEquals(2, threads.getValue());
            assertEquals("●  Idle — configured for up to 2 threads", state.getText());
            frame.validate();
            assertTrue(state.getPreferredSize().width <= state.getWidth(), "Capacity text must fit");
            capture("play-thread-capacity.png");
            named(frame, "playDepth", JSpinner.class).setValue(3);
            combo("humanSide").setSelectedItem(GameController.HumanSide.BLACK);
            assertEquals("●  Thinking — configured for up to 2 threads", state.getText());
            assertFalse(threads.isEnabled());
        });
        until(() -> edt(() -> named(frame, "moveHistory", JTable.class).getRowCount() == 1
                && !named(frame, "stopSearch", JButton.class).isEnabled()));
        edt(() -> {
            JSpinner threads = named(frame, "playThreads", JSpinner.class);
            JLabel state = named(frame, "engineState", JLabel.class);
            assertTrue(threads.isEnabled());
            assertTrue(state.getText().equals("●  Opening book") || state.getText().equals("●  Idle — configured for up to 2 threads"));
            assertEquals("0/2", named(frame, "engineThreads", JLabel.class).getText());
            threads.setValue(4);
            assertEquals("●  Idle — configured for up to 4 threads", state.getText());
        });
    }

    private void assertLayout() {
        BoardPanel board = PlayPresentationTest.descendants(frame, BoardPanel.class);
        Rectangle b = board.boardBounds(); assertEquals(b.width, b.height); assertTrue(b.width >= 360);
        assertTrue(board.getVisibleRect().contains(b));
        for (String name : new String[] {"gameMode", "humanSide", "playEvaluator", "searchLimit"}) {
            JComboBox<?> box = combo(name);
            assertTrue(box.getWidth() >= box.getMinimumSize().width, name);
            assertTrue(box.getHeight() >= box.getPreferredSize().height, name);
        }
        JTextArea pv = named(frame, "principalVariation", JTextArea.class);
        assertTrue(pv.getVisibleRect().height >= 35, "PV must not collapse to a clipped line");
        assertTrue(named(frame, "engineScore", JLabel.class).getWidth() >= 75);
        assertTrue(UIManager.getColor("TextArea.background").getRed() < 70);
        assertTrue(UIManager.getColor("PopupMenu.background").getRed() < 70);
    }

    private void move(String coordinate) {
        BoardPanel board = PlayPresentationTest.descendants(frame, BoardPanel.class);
        Rectangle bounds = board.boardBounds();
        for (int i = 0; i < 2; i++) {
            int file = coordinate.charAt(i * 2) - 'a', rank = coordinate.charAt(i * 2 + 1) - '1';
            int x = bounds.x + (file * 2 + 1) * bounds.width / 16;
            int y = bounds.y + ((7 - rank) * 2 + 1) * bounds.height / 16;
            board.dispatchEvent(new MouseEvent(board, i == 0 ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
        }
    }
    private JComboBox<?> combo(String name) { return named(frame, name, JComboBox.class); }
    private void capture(String filename) {
        try {
            Path directory = Path.of("build", "gui-smoke"); Files.createDirectories(directory);
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); frame.paintAll(g); g.dispose();
            ImageIO.write(image, "png", directory.resolve(filename).toFile());
        } catch (Exception error) { throw new AssertionError(error); }
    }
    private static <T extends Component> T named(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container container) {
                T found = named(container, name, type); if (found != null) return found;
            }
        }
        return null;
    }
}
