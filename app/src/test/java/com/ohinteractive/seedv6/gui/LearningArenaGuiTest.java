package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;

@Timeout(90)
class LearningArenaGuiTest {
    @TempDir Path temporary;
    @Test void configurationContainsTwoIndependentArchitecturesSharedExposureAndBothSearchModes() throws Exception {
        edt(() -> {
            SeedTheme.initialize();
            var panel = new LearningArenaPanel(new TrainingFolders(TrainingDashboardTest.settings(temporary)));
            var a = named(panel, "arenaArchitectureA", JComboBox.class); var b = named(panel, "arenaArchitectureB", JComboBox.class);
            assertEquals(TrainingArchitecture.NNUE, a.getSelectedItem()); assertEquals(TrainingArchitecture.BRN3, b.getSelectedItem());
            a.setSelectedItem(TrainingArchitecture.BRN3); b.setSelectedItem(TrainingArchitecture.NNUE);
            assertEquals(2, a.getItemCount()); assertEquals(8, named(panel, "arenaEpochs", JSpinner.class).getValue());
            assertTrue(named(panel, "arenaDepth", JSpinner.class).isEnabled()); assertFalse(named(panel, "arenaMillis", JSpinner.class).isEnabled());
            named(panel, "arenaLimit", JComboBox.class).setSelectedItem(Limit.TIME);
            assertFalse(named(panel, "arenaDepth", JSpinner.class).isEnabled()); assertTrue(named(panel, "arenaMillis", JSpinner.class).isEnabled());
            assertFalse(named(panel, "arenaResume", JButton.class).isEnabled()); assertFalse(named(panel, "arenaPause", JButton.class).isEnabled());
            panel.beginShutdown().run();
        });
    }
    @Test void controllerRunsOffEdtAndReopensDurableResults() throws Exception {
        Path source = temporary.resolve("source.jsonl"); Files.writeString(source, SourceReadersTest.line(100) + SourceReadersTest.line(200));
        var config = new LearningArenaConfig("GUI controller", new Competitor("A", TrainingArchitecture.BRN3, 71, 2),
                new Competitor("B", TrainingArchitecture.BRN3, 97, 2), DataSource.register("shared", source, 1), 2, 1, 1, 71,
                new Arena(2, Limit.DEPTH, 1, 100, 1, 0, 0, 4, "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1"));
        var ref = new AtomicReference<LearningArenaController>(); Path root = temporary.resolve("campaign");
        edt(() -> {
            var c = new LearningArenaController(v -> assertTrue(SwingUtilities.isEventDispatchThread())); ref.set(c);
            c.startDraft(root, () -> { assertFalse(SwingUtilities.isEventDispatchThread()); return config; }); assertTrue(c.busy());
        });
        until(() -> !edt(() -> ref.get().busy()));
        assertEquals("", ref.get().error()); assertEquals(LearningArenaState.Status.COMPLETE, ref.get().update().state().status());
        edt(() -> ref.get().open(root)); until(() -> !edt(() -> ref.get().busy()));
        assertEquals(2, ref.get().update().state().history().size());
        Runnable close = edt(() -> ref.get().beginShutdown()); close.run();
    }
    @Test void mainTabShowsAndRendersAtSupportedDesktopSizes() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        ChessFrame frame = edt(() -> new ChessFrame(TrainingDashboardTest.settings(temporary), new TrainingController.Backend(), ignored -> {}));
        try {
            edt(() -> {
                frame.setVisible(true); var tabs = named(frame, "workspaces", JTabbedPane.class);
                assertEquals("Play", tabs.getTitleAt(0)); assertEquals("Network Training", tabs.getTitleAt(1)); assertEquals("Learning Arena", tabs.getTitleAt(2));
                tabs.setSelectedIndex(2);
                for (var size : new Dimension[]{new Dimension(1440, 950), new Dimension(1100, 760)}) {
                    frame.setSize(size); frame.validate();
                    named(frame, "learningArena", LearningArenaPanel.class).showSetupTop();
                    var scroll = named(frame, "arenaSetupScroll", JScrollPane.class);
                    assertEquals(0, scroll.getViewport().getViewPosition().y);
                    assertFalse(scroll.getHorizontalScrollBar().isVisible(), "Configuration must fit the window width");
                    assertTrue(named(frame, "arenaStart", JButton.class).isShowing()); assertTrue(named(frame, "arenaSource", JTextField.class).isShowing());
                    var image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
                    try { Path file = Path.of("build/learning-arena/gui-" + size.width + ".png"); Files.createDirectories(file.getParent()); ImageIO.write(image, "png", file.toFile()); }
                    catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                }
            });
        } finally { edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING))); until(() -> !edt(frame::isDisplayable)); }
    }
}
