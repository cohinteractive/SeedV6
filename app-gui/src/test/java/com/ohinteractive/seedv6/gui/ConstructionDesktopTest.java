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

/** Native Swing QA through the existing frame and controls; no training or strength testing. */
@Timeout(90)
class ConstructionDesktopTest {
    private static final Path CAPTURES = Path.of("build/activation-gui/run-" + java.util.UUID.randomUUID());
    @TempDir Path temporary;
    @Test void nativePlayAndArenaControlsLaunchPinnedSearchAndShowIdentity() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Native desktop required");
        var frame = edt(() -> new ChessFrame(TrainingSettings.defaults(temporary, NetworkArchitecture.NNUE), new TrainingController.Backend(), ignored -> {}));
        try {
            edt(() -> {
                frame.setVisible(true); frame.setSize(1440, 950);
                named(frame, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
                named(frame, "whiteEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.CONVENTIONAL_CP64);
                named(frame, "blackEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.BREP2_32);
                named(frame, "playThreads", JSpinner.class).setValue(1);
                named(frame, "playDepth", JSpinner.class).setValue(3);
                assertTrue(named(frame, "startGame", JButton.class).isEnabled());
                named(frame, "startGame", JButton.class).doClick(); return null;
            });
            until(() -> edt(() -> named(frame, "whitePlayerNetwork", JTextArea.class).getText().contains("Conventional NNUE CP64")));
            until(() -> edt(() -> !named(frame, "principalVariation", JTextArea.class).getText().equals("—")
                    && named(frame, "engineDepth", JLabel.class).getText().equals("3")));
            edt(() -> { named(frame, "stopGame", JButton.class).doClick(); capture(frame, "play");
                assertTrue(named(frame, "blackPlayerNetwork", JTextArea.class).getText().contains("BREP2-32"));
                named(frame, "workspaces", JTabbedPane.class).setSelectedIndex(2);
                named(frame, "arenaMode", JComboBox.class).setSelectedItem(com.ohinteractive.seedv6.training.service.LearningArenaConfig.Mode.MATCH_ONLY);
                named(frame, "arenaFixedAEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.CONVENTIONAL_CP64);
                named(frame, "arenaFixedBEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.BREP2_32);
                assertTrue(named(frame, "arenaCreate", JButton.class).isEnabled());
                named(frame, "learningArena", LearningArenaPanel.class).showSetupTop(); return null;
            });
            edt(() -> { capture(frame, "arena"); return null; });
        } finally {
            edt(() -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); return null; });
            until(() -> !edt(frame::isDisplayable));
        }
    }
    private static void capture(JFrame frame, String name) throws Exception {
        frame.validate(); Path folder = CAPTURES; Files.createDirectories(folder);
        var image = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); frame.paintAll(graphics); graphics.dispose();
        javax.imageio.ImageIO.write(image, "png", folder.resolve(name + ".png").toFile());
    }
    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) { T found = find(container, name, type); if (found != null) return found; }
        }
        throw new AssertionError("Missing control " + name);
    }
    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        try { return named(root, name, type); } catch (AssertionError missing) { return null; }
    }
    private static void until(Callable<Boolean> check) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!check.call()) { if (System.nanoTime() > end) fail("Timed out waiting for desktop state"); Thread.sleep(50); }
    }
    private static <T> T edt(Callable<T> call) throws Exception {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> { try { result.set(call.call()); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get()); return result.get();
    }
}
