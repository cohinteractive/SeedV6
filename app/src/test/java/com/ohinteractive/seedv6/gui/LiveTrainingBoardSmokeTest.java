package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.service.TrainerConfig;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

/** Genuine production trainer/search from the standard starting position, no scripted moves or gates. */
@Tag("slow-nnue")
@Timeout(90)
class LiveTrainingBoardSmokeTest {
    @TempDir Path temp;
    private ChessFrame frame;
    private TrainingController.Handle trainer;
    @AfterEach void close() throws Exception {
        if (frame != null && edt(frame::isDisplayable)) {
            edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
        if (trainer != null) { assertTrue(trainer.terminated()); assertTrue(trainer.snapshot().activeGame().isEmpty()); }
    }
    @Test void realChangingPositionsUseTheNormalPollWhilePlayRemainsIndependentAndShutdownDrains() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path playStore = temp.resolve("play-store"); bootstrap(playStore);
        var settings = new TrainingSettings(temp.resolve("real-store"), 3, 1, 2, 0, 0, 2, 2, 1, 1, 71, 64, 1);
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Handle create(TrainingSettings s, boolean resume, TrainerConfig.DepthChange change) throws java.io.IOException {
                trainer = super.create(s, resume, change); return trainer;
            }
        };
        frame = edt(() -> new ChessFrame(settings, backend, ignored -> {}));
        edt(() -> {
            frame.setVisible(true); frame.setSize(SeedTheme.scale(1586), SeedTheme.scale(992));
            named(frame, "playDepth", JSpinner.class).setValue(2);
            named(frame, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
            named(frame, "whiteCheckpointStore", JTextField.class).setText(playStore.toString());
            named(frame, "blackCheckpointStore", JTextField.class).setText(playStore.toString());
        });
        until(() -> edt(() -> named(frame, "startGame", JButton.class).isEnabled()));
        edt(() -> {
            named(frame, "startGame", JButton.class).doClick();
            named(frame, "workspaces", JTabbedPane.class).setSelectedIndex(1);
            named(frame, "startTraining", JButton.class).doClick();
        });
        until(() -> edt(() -> !named(frame, "trainingBoard", BoardPanel.class).positionUnavailable()
                && named(frame, "trainingGameActivity", JLabel.class).getText().contains("Last")));
        long[] first = edt(() -> {
            var board = named(frame, "trainingBoard", BoardPanel.class);
            capture("real-selfplay-first.png");
            System.out.println("PHASE4_REAL_FIRST board=" + Arrays.toString(board.displayedBoard())
                    + " activity=" + named(frame, "trainingGameActivity", JLabel.class).getText()
                    + " score=" + named(frame, "trainingMoveEvaluation", JTextArea.class).getText());
            return board.displayedBoard();
        });
        until(() -> edt(() -> !named(frame, "trainingBoard", BoardPanel.class).positionUnavailable()
                && !Arrays.equals(first, named(frame, "trainingBoard", BoardPanel.class).displayedBoard())));
        edt(() -> {
            var board = named(frame, "trainingBoard", BoardPanel.class);
            capture("real-selfplay-next.png");
            assertTrue(named(frame, "moveHistory", JTable.class).getRowCount() > 0, "Concurrent Play also progressed");
            assertFalse(Arrays.equals(board.displayedBoard(), named(frame, "chessBoard", BoardPanel.class).displayedBoard()));
            System.out.println("PHASE4_REAL_NEXT board=" + Arrays.toString(board.displayedBoard())
                    + " activity=" + named(frame, "trainingGameActivity", JLabel.class).getText()
                    + " playRows=" + named(frame, "moveHistory", JTable.class).getRowCount());
            named(frame, "stopSearch", JButton.class).doClick();
            assertTrue(trainer.snapshot().running(), "Stopping Play must not stop Training");
            named(frame, "stopTraining", JButton.class).doClick();
            assertTrue(board.positionUnavailable(), "Stop clears the rendered board immediately");
            assertTrue(trainer.snapshot().activeGame().isEmpty());
            capture("real-stopping-cleared.png");
        });
    }
    private void capture(String filename) {
        try {
            frame.validate();
            Path directory = Path.of("build", "gui-smoke", "phase4", System.getProperty("flatlaf.uiScale", "100%"));
            Files.createDirectories(directory);
            var image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics(); frame.paintAll(graphics); graphics.dispose();
            ImageIO.write(image, "png", directory.resolve(filename).toFile());
        } catch (Exception problem) { throw new AssertionError(problem); }
    }
}
