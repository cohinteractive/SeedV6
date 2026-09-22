package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.TrainerConfig;
import com.ohinteractive.seedv6.training.service.TrainingSource;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class BrnTrainingGuiTest {
    @TempDir Path root;
    TrainingSettings settings() {
        return new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 32, 1, 1, 73, 1, 1, NetworkArchitecture.BRN, .002);
    }

    @Test void preferencesRestoreBrnAndKeepNnueSettingsIndependent() throws Exception {
        var prefs = Preferences.userRoot().node("seedv6-brn-test-" + UUID.randomUUID());
        try {
            settings().save(prefs);
            assertEquals(settings(), TrainingSettings.load(prefs));
            var config = TrainingSettings.load(prefs).config(TrainerConfig.DepthChange.REQUIRE_SAME);
            assertEquals(TrainingArchitecture.BRN, config.architecture()); assertEquals(.002, config.brnLearningRate());
            assertEquals(new TrainerConfig.Training(1, 1, true), config.training());
            assertEquals(32, prefs.getInt("minibatch", 0));
            var nnue = new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 32, 1, 1, 73, 1, 1);
            nnue.save(prefs); assertEquals(NetworkArchitecture.NNUE, TrainingSettings.load(prefs).architecture());
        } finally { prefs.removeNode(); }
    }

    @Test void cardsSwitchInExistingSlotAndBrnControlSharesAllLifecycleLocks() throws Exception {
        var panel = edt(() -> new TrainingPanel(settings().withSource(TrainingSource.SELF_PLAY)));
        var controller = edt(() -> new TrainingController(settings().withSource(TrainingSource.SELF_PLAY), new TrainingController.Backend(), ignored -> {}, panel::showState));
        try {
            edt(() -> {
                panel.bind(controller);
                var selector = named(panel, "networkArchitecture", JComboBox.class);
                var nnue = named(panel, "nnueConfiguration", JPanel.class);
                var brn = named(panel, "brnConfiguration", JPanel.class);
                var rate = named(panel, "brnLearningRate", JSpinner.class);
                assertTrue(brn.isVisible()); assertFalse(nnue.isVisible());
                selector.setSelectedItem(NetworkArchitecture.NNUE); assertTrue(nnue.isVisible()); assertFalse(brn.isVisible());
                selector.setSelectedItem(NetworkArchitecture.BRN); assertTrue(brn.isVisible()); assertFalse(nnue.isVisible());
                assertNull(named(brn, "trainingMinibatch", JSpinner.class)); assertNull(named(brn, "trainingEpochs", JSpinner.class));
                for (var phase : TrainingController.Phase.values()) {
                    boolean active = switch (phase) { case STARTING, CONFIRM_DEPTH, RUNNING, STOPPING -> true; default -> false; };
                    boolean editable = !active && phase != TrainingController.Phase.CLOSING;
                    panel.showState(new TrainingController.ViewState(settings(), phase, null, "", active, editable, false, 1, ""));
                    assertEquals(editable, selector.isEnabled()); assertEquals(editable, rate.isEnabled());
                }
                panel.showState(controller.state()); rate.setValue(.003);
                assertTrue(panel.applySettings()); assertEquals(.003, controller.state().settings().brnLearningRate());
                assertEquals(NetworkArchitecture.BRN, controller.state().settings().architecture());
                assertTrue(named(panel, "trainingProgress", JTextArea.class).getText().contains("Network Architecture: BRN"));
            });
        } finally { edt(controller::beginShutdown).run(); }
    }

    @Test void productionControllerBootstrapsAndResumesBrnWithoutNnueFallback() throws Exception {
        var controller = edt(() -> new TrainingController(settings().withSource(TrainingSource.SELF_PLAY), new TrainingController.Backend(), ignored -> {}, ignored -> {}));
        try {
            edt(controller::start); until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
            var first = edt(controller::state);
            assertEquals(TrainingController.Phase.STOPPED, first.phase(), first.message());
            assertEquals(1, first.snapshot().generation());
            byte[] pendingState;
            try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
                var restored = (NetworkTrainingState.Brn) store.resumeState(first.snapshot().latestTrainingId());
                assertEquals(.002, restored.hyperparameters().learningRate());
                restored.trainer().train(Board.startingPosition(), 1);
                pendingState = restored.encode();
                // Exercise the actual GUI/controller serialization boundary with nonzero moments
                // and a durable Candidate whose validation has not yet been recorded.
                store.publish(restored, new CheckpointManifest.Metadata(2, 1, first.snapshot().latestTrainingId()));
            }
            edt(controller::start); until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
            var resumed = edt(controller::state);
            assertEquals(TrainingController.Phase.STOPPED, resumed.phase(), resumed.message()); assertEquals(3, resumed.snapshot().generation());
            assertEquals(1, resumed.snapshot().optimizerStep());
            try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
                // The bounded, capped generation has no terminal samples, so every state bit must survive.
                assertArrayEquals(pendingState, store.resumeState(resumed.snapshot().latestTrainingId()).encode());
            }
            assertTrue(resumed.resume());
            assertThrows(java.io.IOException.class, () -> new TrainingController.Backend().inspect(
                    new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 32, 1, 1, 73, 1, 1)));
            assertThrows(java.io.IOException.class, () -> PlayEvaluator.loadBest(root));
        } finally { edt(controller::beginShutdown).run(); }
    }

    @Test void nativeConfigurationCardRendersAtSmallWindowSize() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame frame = edt(() -> {
            var window = new JFrame(SeedTheme.initialize() + " BRN configuration smoke");
            window.setContentPane(new TrainingPanel(settings().withSource(TrainingSource.SELF_PLAY)));
            window.setSize(SeedTheme.scale(760), SeedTheme.scale(740));
            named(window, "trainingViews", JTabbedPane.class).setSelectedIndex(2);
            window.setVisible(true); return window;
        });
        try {
            edt(() -> {
                var brn = named(frame, "brnConfiguration", JPanel.class);
                brn.scrollRectToVisible(new Rectangle(0, 0, brn.getWidth(), brn.getHeight())); frame.validate();
                assertTrue(named(frame, "brnLearningRate", JSpinner.class).isShowing());
                var info = named(frame, "brnTrainingInformation", JTextArea.class);
                try { assertTrue(info.modelToView2D(info.getDocument().getLength() - 1).getMaxY() <= info.getHeight()); }
                catch (javax.swing.text.BadLocationException e) { throw new AssertionError(e); }
            });
            var image = edt(() -> {
                var result = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = result.createGraphics(); frame.paint(graphics); graphics.dispose(); return result;
            });
            Path output = Path.of("build/gui-smoke/brn-configuration.png"); Files.createDirectories(output.getParent());
            ImageIO.write(image, "png", output.toFile());
        } finally { edt(frame::dispose); }
    }
}
