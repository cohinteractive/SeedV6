package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.core.brn.BrnTrainer;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class BrnBootstrapGuiTest {
    @TempDir Path root;
    TrainingSettings settings(NetworkArchitecture architecture) {
        return new TrainingSettings(root.resolve(architecture.name()), 1, 1, 8, 0, 0, 4, 32, 1, 2, 71, 8, 1,
                architecture, .002, .003, .004);
    }
    @ParameterizedTest @EnumSource(value = NetworkArchitecture.class, names = {"BRN", "BRN1"})
    void freshBrnDefaultsToNnueBootstrapWithSeparateFieldsAndActiveLocks(NetworkArchitecture architecture) throws Exception {
        var settings = settings(architecture);
        var panel = edt(() -> new TrainingPanel(settings));
        until(() -> edt(() -> named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
        var controller = edt(() -> new TrainingController(settings, new TrainingController.Backend(), s -> {}, panel::showState));
        try {
            edt(() -> {
                panel.bind(controller);
                var source = named(panel, "brnTrainingSource", JComboBox.class);
                var generator = named(panel, "nnueGeneratorStore", JTextField.class);
                assertEquals(TrainingSource.Mode.NNUE_BOOTSTRAP, source.getSelectedItem());
                generator.setText(root.resolve("common-nnue").toString());
                assertTrue(panel.applySettings());
                var configured = controller.state().settings();
                assertEquals(settings.root(), configured.root());
                assertEquals(TrainingSource.bootstrap(root.resolve("common-nnue")), configured.source());
                assertNotEquals(configured.root().toString(), configured.source().generatorStore());
                assertFalse(named(panel, "trainingPairs", JSpinner.class).isEnabled());
                for (var phase : java.util.List.of(TrainingController.Phase.STARTING, TrainingController.Phase.RUNNING, TrainingController.Phase.STOPPING)) {
                    panel.showState(new TrainingController.ViewState(configured, phase, null, "", true, false, false, 1, ""));
                    assertFalse(source.isEnabled()); assertFalse(generator.isEnabled());
                }
                panel.showState(controller.state());
                source.setSelectedItem(TrainingSource.Mode.SELF_PLAY); assertTrue(panel.applySettings());
                assertEquals(TrainingSource.SELF_PLAY, controller.state().settings().source());
                assertTrue(named(panel, "trainingPairs", JSpinner.class).isEnabled());
                named(panel, "networkArchitecture", JComboBox.class).setSelectedItem(NetworkArchitecture.NNUE);
                assertFalse(source.isShowing());
            });
        } finally { edt(controller::beginShutdown).run(); }
    }
    @Test void preferencesKeepGeneratorFolderIndependentAndStoreModeOverridesDefaultOnRestart() throws Exception {
        var prefs = Preferences.userRoot().node("seedv6-bootstrap-" + UUID.randomUUID());
        try {
            var selected = settings(NetworkArchitecture.BRN).withSource(TrainingSource.bootstrap(root.resolve("nnue")));
            selected.save(prefs);
            var loaded = TrainingSettings.load(prefs);
            assertEquals(selected.root(), loaded.root()); assertEquals(selected.generatorStore(), loaded.generatorStore());
            assertEquals(selected.brnLearningRate(), loaded.brnLearningRate());
            try (var store = new CheckpointStore(selected.root(), TrainingArchitecture.BRN)) {
                store.writeTrainingSource(selected.source());
                store.initialize(new NetworkTrainingState.Brn(new BrnTrainer(.002)), new CheckpointManifest.Metadata(0, 1, ""));
            }
            assertEquals(selected.source(), new TrainingController.Backend().resolveSource(loaded).source());
            var panel = edt(() -> new TrainingPanel(loaded));
            until(() -> edt(() -> named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
            assertEquals(selected.generatorStore(), edt(() -> named(panel, "nnueGeneratorStore", JTextField.class).getText()));
        } finally { prefs.removeNode(); }
    }
    @Test void legacyBrnStoreLoadsAsSelfPlayWithoutWritingConfigurationOrChangingPayloads() throws Exception {
        var selected = settings(NetworkArchitecture.BRN);
        String id;
        try (var store = new CheckpointStore(selected.root(), TrainingArchitecture.BRN)) {
            id = store.initialize(new NetworkTrainingState.Brn(new BrnTrainer(.002)), new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        byte[] before = Files.readAllBytes(selected.root().resolve("checkpoints").resolve(id).resolve("training.state"));
        assertEquals(TrainingSource.SELF_PLAY, new TrainingController.Backend().resolveSource(selected).source());
        var panel = edt(() -> new TrainingPanel(selected));
        until(() -> edt(() -> named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
        assertEquals(TrainingSource.Mode.SELF_PLAY, edt(() -> named(panel, "brnTrainingSource", JComboBox.class).getSelectedItem()));
        assertFalse(Files.exists(selected.root().resolve(CheckpointStore.TRAINING_SOURCE_FILE)));
        assertArrayEquals(before, Files.readAllBytes(selected.root().resolve("checkpoints").resolve(id).resolve("training.state")));
    }
    @Test @Timeout(120) void nativeBootstrapControlsRunRealTrainingAndDisplayLossDecision() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        Path generator = root.resolve("nnue");
        try (var store = new CheckpointStore(generator, TrainingArchitecture.NNUE)) {
            store.initialize(new com.ohinteractive.seedv6.training.nnue.NnueTrainer(
                    com.ohinteractive.seedv6.training.nnue.TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 1, ""));
        }
        var options = settings(NetworkArchitecture.BRN2).withSource(TrainingSource.bootstrap(generator));
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Handle create(TrainingSettings settings, boolean resume, TrainerConfig.DepthChange change) throws java.io.IOException {
                var c = settings.config(change);
                var fixture = new TrainerConfig(c.checkpointRoot(), c.masterSeed(), c.selfPlay(), c.training(), c.validation(),
                        c.maximumGenerations(), c.depthChange(), "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1", c.architecture(), c.brnLearningRate(), c.source());
                return handle(resume ? TrainerService.resume(fixture) : TrainerService.fresh(fixture, new com.ohinteractive.seedv6.core.brn2.Brn2Trainer(.004)));
            }
        };
        edt(SeedTheme::initialize);
        var panel = edt(() -> new TrainingPanel(options));
        var controller = edt(() -> new TrainingController(options, backend, s -> {}, panel::showState));
        var frame = edt(() -> {
            var window = new JFrame("BRN NNUE bootstrap smoke"); window.setContentPane(panel); window.setSize(1100, 1000);
            panel.bind(controller); window.setVisible(true); return window;
        });
        try {
            edt(() -> { named(panel, "trainingViews", JTabbedPane.class).setSelectedIndex(2); });
            screenshot(frame, "brn-bootstrap-configuration.png");
            edt(() -> { named(panel, "trainingViews", JTabbedPane.class).setSelectedIndex(0); named(panel, "startTraining", JButton.class).doClick(); });
            until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
            var result = edt(controller::state);
            assertEquals(TrainingController.Phase.STOPPED, result.phase(), result.message());
            assertTrue(result.snapshot().bootstrapValidation().isPresent());
            String text = edt(() -> named(panel, "bootstrapValidationDecision", JTextArea.class).getText());
            assertTrue(text.contains("Candidate loss")); assertTrue(text.contains("not game strength"));
            screenshot(frame, "brn-bootstrap-dashboard.png");
        } finally { edt(controller::beginShutdown).run(); edt(frame::dispose); }
    }
    private static void screenshot(JFrame frame, String name) throws Exception {
        var image = edt(() -> {
            var result = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = result.createGraphics(); frame.paint(graphics); graphics.dispose(); return result;
        });
        Path path = Path.of("build/gui-smoke").resolve(name); Files.createDirectories(path.getParent());
        javax.imageio.ImageIO.write(image, "png", path.toFile());
    }
}
