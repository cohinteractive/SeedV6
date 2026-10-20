package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.RunTermination;
import com.ohinteractive.seedv6.training.service.TrainerConfig;
import com.ohinteractive.seedv6.training.data.*;
import java.awt.*;
import java.nio.file.*;
import java.text.ParseException;
import java.util.concurrent.Callable;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static com.ohinteractive.seedv6.gui.BrnNextGuiTest.named;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class TrainingGenerationLimitTest {
    @TempDir Path temporary;
    TrainingPanel panel;
    TrainingController controller;
    JFrame frame;

    void open(NetworkArchitecture architecture) throws Exception {
        edt(() -> {
            if (!GraphicsEnvironment.isHeadless()) SeedTheme.initialize();
            var settings = TrainingSettings.defaults(temporary.resolve(architecture.name()), architecture);
            panel = new TrainingPanel(settings);
            controller = new TrainingController(settings, new TrainingController.Backend(), ignored -> {}, panel::showState);
            panel.bind(controller);
            if (!GraphicsEnvironment.isHeadless()) {
                frame = new JFrame("Generation limit regression fixture");
                frame.setContentPane(panel); frame.setSize(1100, 850); frame.setVisible(true);
            } else panel.setSize(1100, 850);
            return null;
        });
    }

    @AfterEach void close() throws Exception {
        if (controller != null) edt(controller::beginShutdown).run();
        if (panel != null) edt(panel::beginCorpusShutdown).run();
        if (frame != null) edt(() -> { frame.dispose(); return null; });
    }

    @ParameterizedTest @EnumSource(value = NetworkArchitecture.class, names = {"BRN_GATED", "BRN_CUBIC", "BRN_POOLED", "BRN_GEN32_GATED", "BRN_GEN32_CUBIC"})
    void runControlsRemainVisibleAcrossTabsAndContinuousMode(NetworkArchitecture architecture) throws Exception {
        open(architecture);
        edt(() -> {
            var tabs = named(panel, "trainingViews", JTabbedPane.class);
            var mode = named(panel, "trainingTermination", JComboBox.class);
            var count = named(panel, "trainingGenerations", JSpinner.class);
            assertEquals(RunTermination.Kind.UNLIMITED, mode.getSelectedItem());
            assertTrue(count.isVisible(), "The generation count must remain discoverable in continuous mode");
            assertFalse(count.isEnabled());
            mode.setSelectedItem(RunTermination.Kind.GENERATIONS);
            var field = ((JSpinner.DefaultEditor) count.getEditor()).getTextField();
            field.setText("100"); count.commitEdit();
            assertEquals(100L, count.getValue());
            for (int width : new int[]{650, 1100}) {
                if (frame != null) { frame.setSize(width, 850); frame.validate(); }
                else panel.setSize(width, 850);
                for (int tab = 0; tab < tabs.getTabCount(); tab++) {
                    tabs.setSelectedIndex(tab); layout(panel); layout(panel);
                    for (var control : new JComponent[]{mode, count}) {
                        assertTrue(control.isVisible()); assertTrue(control.isEnabled());
                        assertFalse(SwingUtilities.isDescendingFrom(control, tabs), "Run controls must not disappear on another tab");
                        Rectangle bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), panel);
                        assertTrue(bounds.width >= 75 && bounds.height >= 25, bounds.toString());
                        assertTrue(panel.getVisibleRect().contains(bounds), bounds.toString());
                        if (frame != null) assertTrue(control.isShowing());
                    }
                }
            }
            tabs.setSelectedIndex(tabs.indexOfTab("Training Settings"));
            if (frame != null) {
                frame.validate();
                Path image = Path.of("build/generation-limit", architecture + ".png");
                Files.createDirectories(image.getParent());
                var bitmap = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics = bitmap.createGraphics(); frame.paintAll(graphics); graphics.dispose();
                javax.imageio.ImageIO.write(bitmap, "png", image.toFile());
            }
            mode.setSelectedItem(RunTermination.Kind.UNLIMITED);
            assertTrue(count.isVisible()); assertFalse(count.isEnabled());
            mode.setSelectedItem(RunTermination.Kind.GENERATIONS);
            assertEquals(100L, count.getValue());
            panel.showState(new TrainingController.ViewState(controller.state().settings(), TrainingController.Phase.RUNNING,
                    null, "", true, false, true, 4, ""));
            assertFalse(mode.isEnabled()); assertFalse(count.isEnabled());
            panel.showState(controller.state()); assertTrue(mode.isEnabled()); assertTrue(count.isEnabled());
            return null;
        });
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1.5", "100x", "1e2", "9223372036854775808", ""})
    void generationCountRejectsInvalidTextWithoutRoundingOrTruncation(String invalid) throws Exception {
        open(NetworkArchitecture.BRN_GATED);
        edt(() -> {
            named(panel, "trainingTermination", JComboBox.class).setSelectedItem(RunTermination.Kind.GENERATIONS);
            var count = named(panel, "trainingGenerations", JSpinner.class);
            var field = ((JSpinner.DefaultEditor) count.getEditor()).getTextField();
            field.setText(invalid);
            assertThrows(ParseException.class, count::commitEdit);
            return null;
        });
    }

    @ParameterizedTest @EnumSource(value = NetworkArchitecture.class, names = {"BRN_GATED", "BRN_CUBIC", "BRN_POOLED", "BRN_GEN32_GATED", "BRN_GEN32_CUBIC"})
    void startUsesTypedBudgetAndLineageReloadPreservesItWithoutChangingTrainingParameters(NetworkArchitecture architecture) throws Exception {
        var entry = TrainingLineages.create(temporary, architecture, "Bounded GUI fixture");
        Path source = temporary.resolve("tiny.jsonl");
        Files.writeString(source, "{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":50}]}]}\n");
        new DataSources(1, java.util.List.of(DataSource.register("tiny", source, 1)), false).save(DataSources.directory(entry.root()));
        var selection = TrainingLineages.read(entry);
        var original = selection.settings();
        var started = new java.util.concurrent.CompletableFuture<TrainingSettings>();
        var selected = new java.util.concurrent.CompletableFuture<Exception>();
        edt(() -> {
            panel = new TrainingPanel(original);
            controller = new TrainingController(original, new TrainingController.Backend() {
                @Override TrainingController.Handle create(TrainingSettings settings, boolean resume, TrainerConfig.DepthChange change) {
                    started.complete(settings);
                    throw new IllegalStateException("Fixture stops before executing training");
                }
            }, ignored -> {}, panel::showState);
            panel.bind(controller);
            controller.selectLineage(() -> selection, original, selected::complete);
            return null;
        });
        assertNull(selected.get(10, java.util.concurrent.TimeUnit.SECONDS));
        long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (!edt(() -> named(panel, "startTraining", JButton.class).isEnabled())) {
            if (System.nanoTime() > until) fail("Fixture did not become ready");
            Thread.sleep(20);
        }
        edt(() -> {
            named(panel, "trainingTermination", JComboBox.class).setSelectedItem(RunTermination.Kind.GENERATIONS);
            var count = named(panel, "trainingGenerations", JSpinner.class);
            ((JSpinner.DefaultEditor) count.getEditor()).getTextField().setText("100");
            named(panel, "startTraining", JButton.class).doClick();
            return null;
        });
        var requested = started.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(100, requested.maximumGenerations()); assertEquals(0, requested.maximumRunMinutes());
        assertEquals(original.recipe(), requested.recipe()); assertEquals(original.seed(), requested.seed());
        assertEquals(original.source(), requested.source()); assertEquals(original.corpusTraining(), requested.corpusTraining());
        var before = original.config(TrainerConfig.DepthChange.REQUIRE_SAME);
        var after = requested.config(TrainerConfig.DepthChange.REQUIRE_SAME);
        assertEquals(before.selfPlay(), after.selfPlay()); assertEquals(before.training(), after.training());
        assertEquals(before.validation(), after.validation()); assertEquals(before.validationMethod(), after.validationMethod());
        assertEquals(100, after.maximumGenerations());
        edt(controller::beginShutdown).run(); controller = null;
        var restored = TrainingLineages.read(entry);
        assertEquals(selection.lineage().id(), restored.lineage().id());
        assertEquals(requested, restored.settings());
        assertFalse(Files.exists(entry.root().resolve("refs/latest-training")), "GUI settings fixture must never train");
    }

    static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container nested) layout(nested);
    }
    static <T> T edt(Callable<T> call) throws Exception {
        var task = new java.util.concurrent.FutureTask<>(call);
        SwingUtilities.invokeAndWait(task); return task.get();
    }
}
