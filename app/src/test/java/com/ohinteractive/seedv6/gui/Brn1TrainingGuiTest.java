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
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class Brn1TrainingGuiTest {
    @TempDir Path root;
    TrainingSettings settings() {
        return new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 32, 1, 1, 73, 1, 1, NetworkArchitecture.BRN1, .001, .002);
    }

    @Test void legacyBrnPreferenceStaysBrn0AndBrn1DefaultsAndRateAreIndependent() throws Exception {
        var prefs = Preferences.userRoot().node("seedv6-brn1-test-" + UUID.randomUUID());
        try {
            prefs.put("architecture", "BRN"); prefs.putDouble("brnLearningRate", .007);
            var old = TrainingSettings.load(prefs);
            assertEquals(NetworkArchitecture.BRN, old.architecture());
            assertEquals("BRN-0", old.architecture().toString());
            assertEquals(.007, old.brnLearningRate()); assertEquals(.001, old.brn1LearningRate());
            assertEquals(.007, old.config(TrainerConfig.DepthChange.REQUIRE_SAME).brnLearningRate());
            old.save(prefs); assertEquals("BRN", prefs.get("architecture", ""));
            prefs.put("architecture", "BRN1");
            var fresh = TrainingSettings.load(prefs);
            assertEquals(.001, fresh.config(TrainerConfig.DepthChange.REQUIRE_SAME).brnLearningRate());
            assertEquals(.007, fresh.brnLearningRate());
            assertEquals(java.util.List.of("NNUE", "BRN-0", "BRN-1"),
                    java.util.Arrays.stream(NetworkArchitecture.values()).map(Object::toString).toList());
        } finally { prefs.removeNode(); }
    }

    @Test void preferencesRestoreBrnAndKeepNnueSettingsIndependent() throws Exception {
        var prefs = Preferences.userRoot().node("seedv6-brn-test-" + UUID.randomUUID());
        try {
            settings().save(prefs);
            assertEquals(settings(), TrainingSettings.load(prefs));
            var config = TrainingSettings.load(prefs).config(TrainerConfig.DepthChange.REQUIRE_SAME);
            assertEquals(TrainingArchitecture.BRN1, config.architecture()); assertEquals(.002, config.brnLearningRate());
            assertEquals(new TrainerConfig.Training(1, 1, true), config.training());
            assertEquals(32, prefs.getInt("minibatch", 0));
            var nnue = new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 32, 1, 1, 73, 1, 1);
            nnue.save(prefs); assertEquals(NetworkArchitecture.NNUE, TrainingSettings.load(prefs).architecture());
        } finally { prefs.removeNode(); }
    }

    @Test void cardsSwitchInExistingSlotAndBrnControlSharesAllLifecycleLocks() throws Exception {
        var panel = edt(() -> new TrainingPanel(settings()));
        var controller = edt(() -> new TrainingController(settings(), new TrainingController.Backend(), ignored -> {}, panel::showState));
        try {
            edt(() -> {
                panel.bind(controller);
                var selector = named(panel, "networkArchitecture", JComboBox.class);
                var nnue = named(panel, "nnueConfiguration", JPanel.class);
                var brn = named(panel, "brn1Configuration", JPanel.class);
                var rate = named(panel, "brn1LearningRate", JSpinner.class);
                assertTrue(brn.isVisible()); assertFalse(nnue.isVisible());
                selector.setSelectedItem(NetworkArchitecture.NNUE); assertTrue(nnue.isVisible()); assertFalse(brn.isVisible());
                selector.setSelectedItem(NetworkArchitecture.BRN1); assertTrue(brn.isVisible()); assertFalse(nnue.isVisible());
                assertNull(named(brn, "trainingMinibatch", JSpinner.class)); assertNull(named(brn, "trainingEpochs", JSpinner.class));
                for (var phase : TrainingController.Phase.values()) {
                    boolean active = switch (phase) { case STARTING, CONFIRM_DEPTH, RUNNING, STOPPING -> true; default -> false; };
                    boolean editable = !active && phase != TrainingController.Phase.CLOSING;
                    panel.showState(new TrainingController.ViewState(settings(), phase, null, "", active, editable, false, 1, ""));
                    assertEquals(editable, selector.isEnabled()); assertEquals(editable, rate.isEnabled());
                }
                panel.showState(controller.state()); rate.setValue(.003);
                assertTrue(panel.applySettings()); assertEquals(.003, controller.state().settings().brn1LearningRate());
                assertEquals(NetworkArchitecture.BRN1, controller.state().settings().architecture());
                assertTrue(named(panel, "trainingProgress", JTextArea.class).getText().contains("Network Architecture: BRN-1"));
            });
        } finally { edt(controller::beginShutdown).run(); }
    }

    @Test void productionControllerBootstrapsAndResumesBrnWithoutNnueFallback() throws Exception {
        var controller = edt(() -> new TrainingController(settings(), new TrainingController.Backend(), ignored -> {}, ignored -> {}));
        try {
            edt(controller::start); until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
            var first = edt(controller::state);
            assertEquals(TrainingController.Phase.STOPPED, first.phase(), first.message());
            assertEquals(1, first.snapshot().generation());
            byte[] pendingState;
            try (var store = new CheckpointStore(root, TrainingArchitecture.BRN1)) {
                var restored = (NetworkTrainingState.Brn1) store.resumeState(first.snapshot().latestTrainingId());
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
            try (var store = new CheckpointStore(root, TrainingArchitecture.BRN1)) {
                // The bounded, capped generation has no terminal samples, so every state bit must survive.
                assertArrayEquals(pendingState, store.resumeState(resumed.snapshot().latestTrainingId()).encode());
            }
            assertTrue(resumed.resume());
            assertThrows(java.io.IOException.class, () -> new TrainingController.Backend().inspect(
                    new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 32, 1, 1, 73, 1, 1)));
            assertThrows(java.io.IOException.class, () -> PlayEvaluator.loadBest(root));
        } finally { edt(controller::beginShutdown).run(); }
    }

    @Test void nativeButtonsRunRealBrn1TrainingStopRestartAndResume() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        edt(SeedTheme::initialize);
        var options = new TrainingSettings(root, 1, 1, 2, 0, 0, 2, 32, 1, 2, 73, 4, 0,
                NetworkArchitecture.BRN1, .001, .002);
        // Only the legal starting board is a fixture. Games, samples, Adam, publication,
        // validation, history and stop/restart all execute the production infrastructure.
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Handle create(TrainingSettings settings, boolean resume, TrainerConfig.DepthChange change)
                    throws java.io.IOException {
                var c = settings.config(change);
                assertEquals(TrainingArchitecture.BRN1, c.architecture());
                var fixture = new TrainerConfig(c.checkpointRoot(), c.masterSeed(), c.selfPlay(), c.training(), c.validation(),
                        c.maximumGenerations(), c.depthChange(), "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1", c.architecture(), c.brnLearningRate());
                return handle(resume ? com.ohinteractive.seedv6.training.service.TrainerService.resume(fixture)
                        : com.ohinteractive.seedv6.training.service.TrainerService.fresh(fixture,
                                new com.ohinteractive.seedv6.core.brn1.Brn1Trainer(c.brnLearningRate())));
            }
        };
        long stoppedStep = 0;
        for (int run = 0; run < 2; run++) {
            var panel = edt(() -> new TrainingPanel(options));
            var controller = edt(() -> new TrainingController(options, backend, ignored -> {}, panel::showState));
            var frame = edt(() -> {
                panel.bind(controller);
                var window = new JFrame("BRN-1 native lifecycle smoke"); window.setContentPane(panel);
                window.setSize(960, 820); window.setVisible(true); return window;
            });
            try {
                edt(() -> {
                    named(panel, "brn1LearningRate", JSpinner.class).setValue(.002);
                    named(panel, "startTraining", JButton.class).doClick();
                });
                long previousStep = stoppedStep;
                until(() -> edt(() -> {
                    controller.poll(); var view = controller.state();
                    assertNotEquals(TrainingController.Phase.FAILED, view.phase(), view.message());
                    return view.snapshot() != null && view.snapshot().totals().completedGenerations() >= 1
                            && view.snapshot().optimizerStep() > previousStep;
                }));
                edt(() -> {
                    assertFalse(named(panel, "networkArchitecture", JComboBox.class).isEnabled());
                    assertFalse(named(panel, "brn1LearningRate", JSpinner.class).isEnabled());
                    named(panel, "stopTraining", JButton.class).doClick();
                });
                until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
                var end = edt(controller::state);
                assertEquals(TrainingController.Phase.STOPPED, end.phase(), end.message());
                try (var store = new CheckpointStore(root, TrainingArchitecture.BRN1)) {
                    // Stop may discard work in the next generation; the durable model is authoritative.
                    stoppedStep = store.resumeState(end.snapshot().latestTrainingId()).step();
                    assertTrue(stoppedStep > previousStep);
                }
                var history = new com.ohinteractive.seedv6.training.history.HistoryRepository(root).refresh();
                assertTrue(history.records().size() >= run + 1); assertTrue(history.warnings().isEmpty());
                assertTrue(history.records().stream().anyMatch(r -> r.validPairs() > 0));
                var capture = edt(() -> {
                    var result = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    var graphics = result.createGraphics(); frame.paint(graphics); graphics.dispose(); return result;
                });
                Path output = Path.of("build/gui-smoke/brn1-lifecycle-" + run + ".png"); Files.createDirectories(output.getParent());
                ImageIO.write(capture, "png", output.toFile());
            } finally { edt(controller::beginShutdown).run(); edt(frame::dispose); }
        }
        System.out.println("BRN1_NATIVE_GUI real generations/training/validation/history, Stop, controller restart, Resume passed");
    }

    @Test void nativeConfigurationCardRendersAtSmallWindowSize() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame frame = edt(() -> {
            var window = new JFrame(SeedTheme.initialize() + " BRN configuration smoke");
            window.setContentPane(new TrainingPanel(settings()));
            window.setSize(SeedTheme.scale(760), SeedTheme.scale(740));
            named(window, "trainingViews", JTabbedPane.class).setSelectedIndex(2);
            window.setVisible(true); return window;
        });
        try {
            edt(() -> {
                var brn = named(frame, "brn1Configuration", JPanel.class);
                brn.scrollRectToVisible(new Rectangle(0, 0, brn.getWidth(), brn.getHeight())); frame.validate();
                assertTrue(named(frame, "brn1LearningRate", JSpinner.class).isShowing());
                var info = named(frame, "brn1TrainingInformation", JTextArea.class);
                try { assertTrue(info.modelToView2D(info.getDocument().getLength() - 1).getMaxY() <= info.getHeight()); }
                catch (javax.swing.text.BadLocationException e) { throw new AssertionError(e); }
            });
            var image = edt(() -> {
                var result = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = result.createGraphics(); frame.paint(graphics); graphics.dispose(); return result;
            });
            Path output = Path.of("build/gui-smoke/brn1-configuration.png"); Files.createDirectories(output.getParent());
            ImageIO.write(image, "png", output.toFile());
        } finally { edt(frame::dispose); }
    }
}
