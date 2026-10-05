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
            assertEquals(TrainingArchitecture.NNUE_MATERIAL, a.getSelectedItem()); assertEquals(TrainingArchitecture.BRN3, b.getSelectedItem());
            assertEquals(.001, named(panel, "arenaLearningRateA", JSpinner.class).getValue());
            assertEquals(.003, named(panel, "arenaLearningRateB", JSpinner.class).getValue());
            named(panel, "arenaInheritRateA", JCheckBox.class).doClick(); assertFalse(named(panel, "arenaLearningRateA", JSpinner.class).isEnabled());
            assertTrue(((JLabel)a.getRenderer().getListCellRendererComponent(new JList<>(), TrainingArchitecture.NNUE_MATERIAL, 0, false, false)).getText().contains("material parity"));
            assertTrue(((JLabel)a.getRenderer().getListCellRendererComponent(new JList<>(), TrainingArchitecture.NNUE, 1, false, false)).getText().contains("legacy, no material"));
            a.setSelectedItem(TrainingArchitecture.BRN3); b.setSelectedItem(TrainingArchitecture.NNUE);
            assertEquals(3, a.getItemCount()); assertEquals(8, named(panel, "arenaEpochs", JSpinner.class).getValue());
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
        assertNotNull(ref.get().update().optimization());
        assertEquals(2, ref.get().update().optimization().samples());
        assertEquals(.003, ref.get().update().optimization().learningRate());
        var resolving = new java.util.concurrent.CountDownLatch(1);
        edt(() -> ref.get().startDraft(temporary.resolve("other-campaign"), () -> {
            resolving.await(); throw new java.io.IOException("Fixture rejects the next draft");
        }));
        try { assertNull(edt(() -> ref.get().update()), "Opening another campaign must clear the old endpoints before any new root can be published"); }
        finally { resolving.countDown(); }
        until(() -> !edt(() -> ref.get().busy()));
        assertTrue(ref.get().error().contains("Fixture rejects the next draft"));
        edt(() -> ref.get().open(root)); until(() -> !edt(() -> ref.get().busy()));
        assertEquals("", ref.get().error()); assertEquals(root, ref.get().root());
        assertEquals(2, ref.get().update().state().history().size());
        Runnable close = edt(() -> ref.get().beginShutdown()); close.run();
    }
    @Test void nativeCommonBrowserPinsAnExistingGenerationAndPublishesDiscoverableIndependentLineages() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path original = temporary.resolve("original"); String checkpoint;
        var architecture = TrainingArchitecture.BRN3;
        try (var store = new com.ohinteractive.seedv6.training.checkpoint.CheckpointStore(original, architecture)) {
            store.writeLineage(new com.ohinteractive.seedv6.training.checkpoint.TrainingLineage(java.util.UUID.randomUUID(), "Selected research model", architecture, java.time.Instant.now(), "", "fixture"));
            checkpoint = store.publish(com.ohinteractive.seedv6.training.model.NetworkTrainingState.initialized(architecture, 71, .005),
                    new com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.Metadata(3, 1, "")).manifest().id();
        }
        var folders = new TrainingFolders(TrainingSettings.defaults(temporary.resolve("settings"), NetworkArchitecture.BRN3));
        folders.register(folders.base(), NetworkArchitecture.BRN3, original);
        var data = DataSource.register("Shared fixture", Files.writeString(temporary.resolve("data.jsonl"), SourceReadersTest.line(100) + SourceReadersTest.line(200)), 1);
        new TrainingDataLibrary(folders.base()).register(data);
        var panel = edt(() -> new LearningArenaPanel(folders));
        JFrame window = edt(() -> { var f = new JFrame("Arena model selection"); f.setContentPane(panel); f.setSize(1100, 760); f.setVisible(true); return f; });
        try {
            until(() -> edt(() -> named(panel, "arenaStart", JButton.class).isEnabled()));
            var error = new AtomicReference<Throwable>();
            edt(() -> {
                named(panel, "arenaArchitectureA", JComboBox.class).setSelectedItem(architecture);
                var timer = new javax.swing.Timer(50, null);
                timer.addActionListener(e -> {
                    for (Window candidate : Window.getWindows()) if (candidate instanceof JDialog dialog && dialog.isShowing() && dialog.getTitle().equals("Starting model")) {
                        var browser = named(dialog, "arenaInitialAEngineSetup", ModelSelectionPanel.class);
                        if (browser == null || !browser.validSelection()) return;
                        timer.stop();
                        try {
                            assertEquals(checkpoint, browser.concreteId());
                            ((JOptionPane) dialog.getContentPane().getComponent(0)).setValue(JOptionPane.OK_OPTION);
                        } catch (Throwable failure) { error.set(failure); dialog.dispose(); }
                    }
                });
                timer.start(); try { named(panel, "arenaSelectModelA", JButton.class).doClick(); } finally { timer.stop(); }
                assertNull(error.get(), () -> String.valueOf(error.get()));
                assertTrue(named(panel, "arenaInitialModelA", JLabel.class).getText().contains("Gen 3"));
                assertFalse(named(panel, "arenaSeedA", JTextField.class).isEnabled());
                named(panel, "arenaInheritRateA", JCheckBox.class).doClick();
                named(panel, "arenaPositions", JSpinner.class).setValue(2); named(panel, "arenaEpochs", JSpinner.class).setValue(1);
                named(panel, "arenaRounds", JSpinner.class).setValue(1); named(panel, "arenaGames", JSpinner.class).setValue(2);
                named(panel, "arenaDepth", JSpinner.class).setValue(1); named(panel, "arenaOpeningMax", JSpinner.class).setValue(0);
                named(panel, "arenaPlyCap", JSpinner.class).setValue(4); named(panel, "arenaFen", JTextField.class).setText("7k/5K2/6Q1/8/8/8/8/8 w - - 0 1");
                named(panel, "arenaStart", JButton.class).doClick();
            });
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(60);
            while (System.nanoTime() < deadline && !edt(() -> { panel.poll(); return named(panel, "arenaStatus", JLabel.class).getText().contains("COMPLETE"); })) Thread.sleep(50);
            assertTrue(edt(() -> named(panel, "arenaStatus", JLabel.class).getText()).contains("COMPLETE"),
                    edt(() -> named(panel, "arenaStatus", JLabel.class).getText()));
            Path campaign; try (var paths = Files.list(folders.base().resolve("learning-arena"))) { campaign = paths.findFirst().orElseThrow(); }
            var state = LearningArenaState.read(campaign); assertEquals(checkpoint, state.config().a().initialModel().checkpoint());
            assertEquals(.005, com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.readTrainingSnapshot(campaign.resolve("A"), state.history().getFirst().a().checkpoint()).hyperparameters().learningRate());
            assertTrue(folders.adopted(folders.base(), NetworkArchitecture.BRN3).contains(campaign.resolve("A")));
            assertTrue(folders.adopted(folders.base(), NetworkArchitecture.BRN3).contains(campaign.resolve("B")));
            assertFalse(Files.exists(original.resolve("refs/best")));
        } finally { Runnable close = edt(panel::beginShutdown); close.run(); edt(window::dispose); }
    }
    @Test void mainTabShowsAndRendersAtSupportedDesktopSizes() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        ChessFrame frame = edt(() -> new ChessFrame(TrainingDashboardTest.settings(temporary), new TrainingController.Backend(), ignored -> {}));
        try {
            edt(() -> {
                frame.setVisible(true); var tabs = named(frame, "workspaces", JTabbedPane.class);
                assertEquals("Play", tabs.getTitleAt(0)); assertEquals("Network Training", tabs.getTitleAt(1)); assertEquals("Arena", tabs.getTitleAt(2));
                tabs.setSelectedIndex(2);
                for (var size : new Dimension[]{new Dimension(1440, 950), new Dimension(1100, 760)}) {
                    frame.setSize(size); frame.validate();
                    named(frame, "learningArena", LearningArenaPanel.class).showSetupTop();
                    var scroll = named(frame, "arenaSetupScroll", JScrollPane.class);
                    assertEquals(0, scroll.getViewport().getViewPosition().y);
                    assertFalse(scroll.getHorizontalScrollBar().isVisible(), "Configuration must fit the window width");
                    assertTrue(named(frame, "arenaStart", JButton.class).isShowing()); assertTrue(named(frame, "arenaSource", JComboBox.class).isShowing());
                    var image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
                    try { Path file = Path.of("build/learning-arena/gui-" + size.width + ".png"); Files.createDirectories(file.getParent()); ImageIO.write(image, "png", file.toFile()); }
                    catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                    var picker = named(frame, "arenaSourceLibrary", TrainingDataSelector.class);
                    int sourceTop = SwingUtilities.convertPoint(picker, 0, 0, scroll.getViewport().getView()).y;
                    scroll.getViewport().setViewPosition(new Point(0, sourceTop));
                    assertTrue(picker.getVisibleRect().contains(new Rectangle(0, 0, picker.getWidth(), picker.getHeight())), "Source picker clipped");
                    var prepare = named(picker, "arenaSourcePrepare", JButton.class);
                    assertTrue(prepare.getVisibleRect().contains(new Rectangle(0, 0, prepare.getWidth(), prepare.getHeight())), "Preparation action clipped");
                    assertTrue(picker.getHeight() >= 180, "Source details/actions must not collapse to a form row");
                    graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
                    try { ImageIO.write(image, "png", Path.of("build/learning-arena/data-library-" + size.width + ".png").toFile()); }
                    catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                }
            });
        } finally { edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING))); until(() -> !edt(frame::isDisplayable)); }
    }
}
