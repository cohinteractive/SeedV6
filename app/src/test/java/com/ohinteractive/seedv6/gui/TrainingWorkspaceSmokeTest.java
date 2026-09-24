package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.service.TrainerSnapshot;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Native window rendering through the existing injectable controller backend. No production fake data. */
class TrainingWorkspaceSmokeTest {
    @TempDir Path temp;
    private ChessFrame frame;
    private final PreviewHandle handle = new PreviewHandle();
    static final class PreviewHandle implements TrainingController.Handle {
        volatile TrainerSnapshot snapshot = activePositionFixture(false);
        volatile boolean terminated;
        public void start() {}
        public void stop() { terminated = true; snapshot = TrainingDashboardTest.snapshot(TrainerSnapshot.State.STOPPED, true, false, false); }
        public TrainerSnapshot snapshot() { return snapshot; }
        public boolean terminated() { return terminated; }
        public void close() { terminated = true; }
    }
    /** Deterministic layout fixture, deliberately separate from LiveTrainingBoardSmokeTest's real trainer. */
    private static TrainerSnapshot activePositionFixture(boolean selfPlay) {
        var feed = new com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed();
        if (selfPlay) feed.selfPlay(188, TrainingDashboardTest.CANDIDATE);
        else feed.validation(187, TrainingDashboardTest.CANDIDATE, TrainingDashboardTest.BEST);
        var game = new com.ohinteractive.seedv6.training.selfplay.HeadlessGame(com.ohinteractive.seedv6.core.Board.startingPosition(), 100);
        feed.start(game, selfPlay ? 1 : 33, selfPlay ? 0 : 1);
        for (String coordinate : new String[]{"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6"}) {
            long move = com.ohinteractive.seedv6.training.telemetry.ActiveGameFeedTest.move(game, coordinate);
            game.play(move); feed.moved(game, move, null);
        }
        return TrainingDashboardTest.snapshot(selfPlay ? TrainerSnapshot.State.GENERATING_SELF_PLAY : TrainerSnapshot.State.VALIDATING,
                selfPlay, selfPlay, selfPlay).withActiveGame(feed.latest());
    }
    @AfterEach void close() throws Exception {
        if (frame != null) {
            edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
    }

    @Test void dashboardStatesNavigationSquareBoardResizingAndDiagnostics() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Inspection inspect(TrainingSettings s) { return new TrainingController.Inspection(true, TrainingDashboardTest.CANDIDATE, s.depth(), ""); }
            @Override TrainingController.Handle create(TrainingSettings s, boolean resume, com.ohinteractive.seedv6.training.service.TrainerConfig.DepthChange change) { return handle; }
        };
        frame = edt(() -> new ChessFrame(TrainingDashboardTest.settings(temp), backend, ignored -> {}));
        edt(() -> {
            frame.setVisible(true); named(frame, "workspaces", JTabbedPane.class).setSelectedIndex(1);
            frame.setSize(SeedTheme.scale(1586), SeedTheme.scale(992)); frame.validate();
            assertEquals(0, named(frame, "trainingViews", JTabbedPane.class).getSelectedIndex());
            assertEquals("Network Training", named(frame, "workspaces", JTabbedPane.class).getTitleAt(1));
            assertEquals("Network Training", named(frame, "trainingBlackIdentity", JLabel.class).getText());
            var architecture = named(frame, "networkArchitecture", JComboBox.class);
            assertEquals(4, architecture.getItemCount()); assertEquals(NetworkArchitecture.NNUE, architecture.getSelectedItem());
            assertTrue(architecture.isEnabled());
            capture("training-idle.png"); named(frame, "startTraining", JButton.class).doClick();
        });
        until(() -> edt(() -> named(frame, "trainingState", JLabel.class).getText().equals("VALIDATING")));
        edt(() -> {
            assertEquals(0, named(frame, "trainingDashboardScroll", JScrollPane.class).getViewport().getViewPosition().y, "First dashboard display must keep the header visible");
            assertEquals("78.1%", named(frame, "candidateScore", JLabel.class).getText()); assertLayout(); capture("training-dashboard-reference.png");
            assertFalse(named(frame, "networkArchitecture", JComboBox.class).isEnabled());
            assertFalse(named(frame, "trainingMinibatch", JSpinner.class).isEnabled());
            assertFalse(named(frame, "trainingEpochs", JSpinner.class).isEnabled());
            for (Dimension size : new Dimension[] {new Dimension(1100, 760), new Dimension(1920, 1080)}) {
                frame.setSize(SeedTheme.scale(size.width), SeedTheme.scale(size.height)); frame.validate(); assertLayout();
                JLabel black = named(frame, "trainingBlackIdentity", JLabel.class);
                assertTrue(black.getWidth() >= black.getPreferredSize().width, "Active participant must fit");
                capture("training-live-fixture-" + size.width + "x" + size.height + ".png");
            }
            frame.setSize(SeedTheme.scale(1586), SeedTheme.scale(992)); frame.validate();
            assertEquals(0, named(frame, "recentTrainingHistory", JTable.class).getRowCount());
            for (int tab = 1; tab <= 3; tab++) {
                named(frame, "trainingViews", JTabbedPane.class).setSelectedIndex(tab); frame.validate(); capture("training-view-" + tab + ".png");
                if (tab == 2) {
                    assertTrue(named(frame, "validationInformation", JTextArea.class).getParent().getHeight() >= SeedTheme.scale(300), "Validation rules must have a readable scrolling viewport");
                    frame.setSize(SeedTheme.scale(1100), SeedTheme.scale(760)); frame.validate();
                    var nnue = named(frame, "nnueConfiguration", NnueConfigurationPanel.class);
                    nnue.scrollRectToVisible(new Rectangle(0, 0, nnue.getWidth(), nnue.getHeight()));
                    var information = named(nnue, "nnueSearchInformation", JTextArea.class);
                    try {
                        assertTrue(information.modelToView2D(information.getDocument().getLength() - 1).getMaxY() <= information.getHeight(),
                                "NNUE search explanation must not clip at the smallest window size");
                    } catch (javax.swing.text.BadLocationException failure) { throw new AssertionError(failure); }
                    capture("training-nnue-configuration-1100x760.png");
                    frame.setSize(SeedTheme.scale(1586), SeedTheme.scale(992)); frame.validate();
                }
            }
            assertTrue(named(frame, "validationProgress", JTextArea.class).isShowing());
            named(frame, "trainingViews", JTabbedPane.class).setSelectedIndex(0);
        });
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        edt(() -> frame.setExtendedState(Frame.MAXIMIZED_BOTH));
        until(() -> edt(() -> frame.getWidth() >= screen.width - 32 && frame.getHeight() >= screen.height - 32));
        edt(() -> {
            frame.validate();
            BoardPanel board = named(frame, "trainingBoard", BoardPanel.class);
            assertTrue(board.getVisibleRect().contains(board.boardBounds()));
            assertTrue(named(frame, "stopTraining", JButton.class).isShowing());
            System.out.println("PHASE4_NATIVE_MAX scale=" + System.getProperty("flatlaf.uiScale", "100%")
                    + " frame=" + frame.getWidth() + "x" + frame.getHeight() + " board=" + board.boardBounds()
                    + " usableScreen=" + screen);
            capture("training-live-fixture-native-maximized.png");
            frame.setExtendedState(Frame.NORMAL);
            frame.setSize(SeedTheme.scale(1586), SeedTheme.scale(992));
        });
        handle.snapshot = TrainingDashboardTest.snapshot(TrainerSnapshot.State.RECORDING_DECISION, true, true, false);
        until(() -> edt(() -> named(frame, "promotionDecision", JLabel.class).getText().equals("PROMOTED")));
        edt(() -> { capture("training-promoted.png"); assertEquals(0, named(frame, "recentTrainingHistory", JTable.class).getRowCount(), "A transient snapshot must not invent durable history"); });
        handle.snapshot = activePositionFixture(true);
        until(() -> edt(() -> named(frame, "trainingState", JLabel.class).getText().equals("SELF-PLAY")));
        edt(() -> {
            assertEquals("White: Latest Training Gen 187", named(frame, "trainingGameSides", JLabel.class).getText());
            assertEquals(0, named(frame, "validationPairProgress", JProgressBar.class).getValue()); capture("training-self-play.png");
            for (Dimension size : new Dimension[] {new Dimension(1100, 760), new Dimension(1920, 1080)}) {
                frame.setSize(SeedTheme.scale(size.width), SeedTheme.scale(size.height)); frame.validate(); assertLayout(); capture("training-" + size.width + "x" + size.height + ".png");
            }
            frame.setSize(SeedTheme.scale(1100), SeedTheme.scale(760)); frame.validate();
            named(frame, "trainingDashboardScroll", JScrollPane.class).getVerticalScrollBar().setValue(SeedTheme.scale(80));
        });
        handle.snapshot = TrainingDashboardTest.snapshot(TrainerSnapshot.State.TRAINING, true, true, true);
        until(() -> edt(() -> named(frame, "trainingState", JLabel.class).getText().equals("TRAINING")));
        edt(() -> {
            assertEquals(SeedTheme.scale(80), named(frame, "trainingDashboardScroll", JScrollPane.class).getVerticalScrollBar().getValue(), 2,
                    "A publication must not override the user's dashboard scroll position");
            assertFalse(named(frame, "networkTrainingProgress", JProgressBar.class).isVisible(), "Counts must not become a fictional total-work percentage");
            capture("training-optimizer.png");
        });
        handle.snapshot = TrainingDashboardTest.snapshot(TrainerSnapshot.State.FAILED, true, false, false); handle.terminated = true;
        until(() -> edt(() -> named(frame, "trainingState", JLabel.class).getText().equals("FAILED")));
        edt(() -> {
            assertEquals("PROMOTION BLOCKED", named(frame, "promotionDecision", JLabel.class).getText());
            assertTrue(named(frame, "networkArchitecture", JComboBox.class).isEnabled());
            assertTrue(named(frame, "trainingMinibatch", JSpinner.class).isEnabled());
            assertTrue(named(frame, "trainingEpochs", JSpinner.class).isEnabled());
            capture("training-failed.png");
        });
        assertFalse(Files.exists(temp.resolve("refs")), "Presentation fixture never opens a real checkpoint store");
    }

    private void assertLayout() {
        var architecture = named(frame, "networkArchitecture", JComboBox.class);
        assertTrue(architecture.isShowing());
        assertTrue(architecture.getVisibleRect().contains(new Rectangle(0, 0, architecture.getWidth(), architecture.getHeight())));
        BoardPanel board = named(frame, "trainingBoard", BoardPanel.class); Rectangle bounds = board.boardBounds();
        assertTrue(board.isShowing()); assertEquals(bounds.width, bounds.height); assertTrue(bounds.width >= SeedTheme.scale(290));
        assertTrue(board.getVisibleRect().contains(bounds));
        var activity = named(frame, "trainingActivityScroll", JScrollPane.class).getViewport();
        assertEquals(activity.getExtentSize().width, activity.getViewSize().width, "Activity text must wrap to its viewport");
        if (!board.positionUnavailable()) {
            JLabel black = named(frame, "trainingBlackIdentity", JLabel.class);
            assertTrue(black.getWidth() >= black.getPreferredSize().width, "Active participant clipped: " + black.getText());
        }
        for (String name : new String[] {"trainingHeadline", "trainingState", "candidateScore"}) {
            JLabel label = named(frame, name, JLabel.class); assertTrue(label.isShowing());
            assertTrue(label.getWidth() >= label.getPreferredSize().width, name + " clipped: " + label.getWidth());
        }
        assertFalse(named(frame, "chessBoard", BoardPanel.class).isShowing());
    }
    private void capture(String file) {
        try {
            frame.validate();
            Path directory = Path.of("build", "gui-smoke", System.getProperty("flatlaf.uiScale", "100%")); Files.createDirectories(directory);
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); frame.paintAll(g); g.dispose(); ImageIO.write(image, "png", directory.resolve(file).toFile());
        } catch (Exception e) { throw new AssertionError(e); }
    }
    static <T extends Component> T named(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container container) { T found = named(container, name, type); if (found != null) return found; }
        }
        return null;
    }
}
