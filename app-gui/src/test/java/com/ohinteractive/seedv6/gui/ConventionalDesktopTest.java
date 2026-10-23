package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real desktop controls and searches, with isolated settings and no training. */
@Timeout(90)
class ConventionalDesktopTest {
    private static final Path CAPTURES = Path.of("build/nnue-gui/run-" + java.util.UUID.randomUUID());
    @TempDir Path temporary;
    @Test void playSwitchResetIndependentColoursAndClose() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Native desktop required");
        var frame = edt(() -> new ChessFrame(TrainingSettings.defaults(temporary, NetworkArchitecture.NNUE),
                new TrainingController.Backend(), ignored -> {}));
        try {
            var field = ChessFrame.class.getDeclaredField("controller"); field.setAccessible(true);
            var controller = (GameController) field.get(frame);
            edt(() -> {
                frame.setVisible(true); frame.setSize(1500, 1000);
                var choices = named(frame, "playEvaluator", JComboBox.class);
                assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, choices.getSelectedItem());
                assertEquals(3, choices.getItemCount());
                choices.setSelectedItem(PlayEvaluator.Mode.CONVENTIONAL_CP64);
                named(frame, "gameSettings", JButton.class).doClick();
                capture(frame, "selection");
                named(frame, "closeGameSettings", JButton.class).doClick();
                named(frame, "playThreads", JSpinner.class).setValue(4);
                named(frame, "playDepth", JSpinner.class).setValue(3);
                assertTrue(controller.loadFen("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1"));
                named(frame, "humanSide", JComboBox.class).setSelectedItem(GameController.HumanSide.BLACK);
                named(frame, "startGame", JButton.class).doClick(); return null;
            });
            until(() -> edt(() -> controller.displayedMoves().size() == 1));
            edt(() -> {
                assertTrue(controller.evaluator().evaluation().isConventional());
                assertEquals("3", named(frame, "engineDepth", JLabel.class).getText());
                assertEquals(4, controller.workerCount());
                assertTrue(named(frame, "engineThreads", JLabel.class).getText().endsWith("/4"));
                assertTrue(named(frame, "searchTermination", JLabel.class).getToolTipText().contains("heads=8"));
                // Human black king e8-d8 through the board's ordinary interaction port.
                controller.squarePressed(60); controller.squareReleased(59); return null;
            });
            until(() -> edt(() -> controller.displayedMoves().size() == 3));
            edt(() -> { capture(frame, "human"); named(frame, "newGame", JButton.class).doClick(); return null; });
            until(() -> edt(controller::settingsEditable));
            edt(() -> {
                assertTrue(controller.displayedMoves().isEmpty());
                named(frame, "playEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.BREP2_32);
                named(frame, "startGame", JButton.class).doClick(); return null;
            });
            until(() -> edt(controller::gameStarted));
            edt(() -> { named(frame, "newGame", JButton.class).doClick(); return null; });
            until(() -> edt(controller::settingsEditable));
            edt(() -> {
                named(frame, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
                named(frame, "whiteEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.BREP2_32);
                named(frame, "blackEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.CONVENTIONAL_CP64);
                assertTrue(controller.loadFen("4k3/8/8/8/4P3/8/8/4K3 b - - 0 1"));
                named(frame, "startGame", JButton.class).doClick(); return null;
            });
            until(() -> edt(() -> controller.displayedMoves().size() >= 2));
            edt(() -> {
                named(frame, "stopGame", JButton.class).doClick();
                assertTrue(named(frame, "whitePlayerNetwork", JTextArea.class).getText().contains("BREP2-32"));
                assertTrue(named(frame, "blackPlayerNetwork", JTextArea.class).getText().contains("CP64"));
                capture(frame, "independent"); return null;
            });
            until(() -> edt(controller::settingsEditable));
            // Close during a fresh long NNUE search; the window owns cleanup.
            edt(() -> {
                named(frame, "newGame", JButton.class).doClick();
                named(frame, "whiteEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.CONVENTIONAL_CP64);
                named(frame, "playDepth", JSpinner.class).setValue(20);
                assertTrue(controller.loadFen("r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1"));
                named(frame, "startGame", JButton.class).doClick(); return null;
            });
            until(() -> edt(controller::gameStarted));
        } finally {
            edt(() -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); return null; });
            until(() -> !edt(frame::isDisplayable));
        }
    }
    private static void capture(JFrame frame, String name) throws Exception {
        frame.validate(); var directory = CAPTURES; Files.createDirectories(directory);
        var image = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); frame.paintAll(graphics); graphics.dispose();
        javax.imageio.ImageIO.write(image, "png", directory.resolve(name + ".png").toFile());
    }
    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                try { return named(container, name, type); } catch (IllegalArgumentException absent) { }
            }
        }
        throw new IllegalArgumentException("Missing control " + name);
    }
    private static void until(Callable<Boolean> check) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!check.call()) { if (System.nanoTime() > end) fail("Timed out waiting for Play"); Thread.sleep(25); }
    }
    private static <T> T edt(Callable<T> call) throws Exception {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> { try { result.set(call.call()); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
        return result.get();
    }
}
