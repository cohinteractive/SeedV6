package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class Brn2SupervisionGuiTest {
    @TempDir Path temp;
    TrainingSettings settings(Path root) {
        return new TrainingSettings(root, 2, 6, 64, 0, 8, 32, 32, 1, 64, 71, 1024, 128,
                NetworkArchitecture.BRN2, .001, .001, .001).withSource(TrainingSource.bootstrap(temp.resolve("nnue")));
    }
    @Test void freshControlsDefaultWdlExposePercentComplementAndFollowEveryLifecycleLock() throws Exception {
        var settings = settings(temp.resolve("fresh"));
        var panel = edt(() -> new TrainingPanel(settings));
        until(() -> edt(() -> named(panel, "brn2Supervision", JComboBox.class).isEnabled()
                && named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
        var controller = edt(() -> new TrainingController(settings, new TrainingController.Backend(), s -> {}, panel::showState));
        try {
            edt(() -> {
                panel.bind(controller);
                var mode = named(panel, "brn2Supervision", JComboBox.class);
                var weight = named(panel, "brn2TeacherWeight", JSpinner.class);
                assertEquals(BrnSupervision.Mode.WDL, mode.getSelectedItem());
                assertFalse(weight.getParent().getParent().isVisible());
                mode.setSelectedItem(BrnSupervision.Mode.NNUE_BLENDED); weight.setValue(75.0);
                assertTrue(weight.getParent().getParent().isVisible());
                assertEquals("WDL: 25.00%", named(panel, "brn2WdlContribution", JLabel.class).getText());
                assertTrue(panel.applySettings());
                assertEquals(BrnSupervision.blended(.75), controller.state().settings().config(TrainerConfig.DepthChange.REQUIRE_SAME).supervision());
                for (var phase : TrainingController.Phase.values()) {
                    boolean active = List.of(TrainingController.Phase.STARTING, TrainingController.Phase.CONFIRM_DEPTH,
                            TrainingController.Phase.RUNNING, TrainingController.Phase.STOPPING).contains(phase);
                    panel.showState(new TrainingController.ViewState(controller.state().settings(), phase, null, "", active, !active, false, 2, ""));
                    if (active || phase == TrainingController.Phase.CLOSING) {
                        assertFalse(mode.isEnabled(), phase.name()); assertFalse(weight.isEnabled(), phase.name());
                    }
                }
                named(panel, "networkArchitecture", JComboBox.class).setSelectedItem(NetworkArchitecture.NNUE);
                assertFalse(named(panel, "brn2Configuration", JPanel.class).isVisible());
            });
        } finally { edt(controller::beginShutdown).run(); }
    }
    @Test void storedObjectiveIsDefaultAndExplicitIdleDraftCanChangeIt() throws Exception {
        Path root = temp.resolve("existing"); var exact = BrnSupervision.blended(.12345678901234567);
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.initializeBrnSupervision(exact); store.writeTrainingSource(TrainingSource.bootstrap(temp.resolve("nnue")));
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new CheckpointManifest.Metadata(0, 2, ""));
        }
        var settings = settings(root).withSupervision(BrnSupervision.blended(.75));
        var panel = edt(() -> new Brn2ConfigurationPanel(settings));
        edt(() -> panel.selectRoot(root.toString(), NetworkArchitecture.BRN2)); until(() -> edt(panel::ready));
        edt(() -> {
            panel.setEditable(true);
            assertTrue(named(panel, "brn2Supervision", JComboBox.class).isEnabled());
            assertTrue(named(panel, "brn2TeacherWeight", JSpinner.class).isEnabled());
            assertEquals(BrnSupervision.blended(.75), panel.readSupervision());
            return null;
        });
        var backend = new TrainingController.Backend();
        assertEquals(BrnSupervision.blended(.75), backend.resolveSource(settings).supervision());
        assertEquals(exact, backend.resolveSource(settings.withSupervision(null)).supervision());
        edt(() -> panel.selectRoot(temp.resolve("different-fresh").toString(), NetworkArchitecture.BRN2));
        until(() -> edt(panel::ready));
        assertEquals(BrnSupervision.WDL, edt(panel::readSupervision));
        assertTrue(edt(() -> named(panel, "brn2Supervision", JComboBox.class).isEnabled()));
    }
    @Test void legacyStoreDefaultsToWdlAndPreferencesRemainBoundToTheirFolder() throws Exception {
        Path root = temp.resolve("legacy");
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new CheckpointManifest.Metadata(0, 2, ""));
        }
        var panel = edt(() -> new Brn2ConfigurationPanel(settings(root)));
        edt(() -> panel.selectRoot(root.toString(), NetworkArchitecture.BRN2)); until(() -> edt(panel::ready));
        assertEquals(BrnSupervision.WDL, edt(panel::readSupervision));
        assertTrue(edt(() -> named(panel, "brn2Supervision", JComboBox.class).isEnabled()));
        assertFalse(Files.exists(root.resolve(CheckpointStore.BRN_SUPERVISION_FILE)));
        var prefs = Preferences.userRoot().node("seedv6-supervision-" + UUID.randomUUID());
        try {
            var chosen = settings(temp.resolve("fresh")).withSupervision(BrnSupervision.blended(.75)); chosen.save(prefs);
            assertEquals(chosen, TrainingSettings.load(prefs));
            prefs.put(TrainingFolders.key(NetworkArchitecture.BRN2), temp.resolve("other").toString());
            assertNull(TrainingSettings.load(prefs).supervision());
            assertNull(TrainingSettings.defaults().supervision());
        } finally { prefs.removeNode(); }
    }
    @Test void nnueRejectsBlendedButBrn2SourcesAreIndependent() {
        var nnue = TrainingSettings.defaults(); var cfg = nnue.config(TrainerConfig.DepthChange.REQUIRE_SAME);
        assertEquals(TrainingArchitecture.NNUE, cfg.architecture()); assertNull(cfg.supervision());
        assertEquals(new TrainerConfig.Training(nnue.epochs(), nnue.minibatch(), true), cfg.training());
        assertThrows(IllegalArgumentException.class, () -> cfg.withSupervision(BrnSupervision.blended(.75)));
        assertEquals(BrnSupervision.blended(.75), settings(temp.resolve("self-play")).withSource(TrainingSource.SELF_PLAY)
                .withSupervision(BrnSupervision.blended(.75)).supervision());
    }
    @Test void nativeStartButtonCompletesBoundedBlendAndDisplaysEditableDurableObjective() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless()); edt(SeedTheme::initialize);
        Path root = temp.resolve("native-student"), generator = temp.resolve("native-generator");
        try (var store = new CheckpointStore(generator, TrainingArchitecture.NNUE)) {
            store.initialize(new com.ohinteractive.seedv6.training.nnue.NnueTrainer(
                    com.ohinteractive.seedv6.training.nnue.TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 2, ""));
        }
        var options = new TrainingSettings(root, 2, 1, 4, 0, 0, 2, 32, 1, 2, 71, 8, 1,
                NetworkArchitecture.BRN2, .001, .001, .001).withSource(TrainingSource.bootstrap(generator));
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Handle create(TrainingSettings settings, boolean resume, TrainerConfig.DepthChange change) throws java.io.IOException {
                var c = settings.config(change);
                assertEquals(BrnSupervision.blended(.75), c.supervision());
                var fixture = new TrainerConfig(c.checkpointRoot(), c.masterSeed(), c.selfPlay(), c.training(), c.validation(),
                        c.maximumGenerations(), c.depthChange(), "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1",
                        c.architecture(), c.brnLearningRate(), c.source(), c.supervision()).withRunSeeds(c.runSeeds());
                return handle(resume ? TrainerService.resume(fixture) : TrainerService.fresh(fixture, new Brn2Trainer(.001)));
            }
        };
        var panel = edt(() -> new TrainingPanel(options));
        var controller = edt(() -> new TrainingController(options, backend, s -> {}, panel::showState));
        var frame = edt(() -> {
            var window = new JFrame("BRN-2 blended supervision smoke"); window.setContentPane(panel);
            window.setSize(1100, 1000); panel.bind(controller); window.setVisible(true); return window;
        });
        try {
            until(() -> edt(() -> named(panel, "brn2Supervision", JComboBox.class).isEnabled()
                    && named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
            edt(() -> {
                named(panel, "trainingViews", JTabbedPane.class).setSelectedIndex(2);
                named(panel, "brn2Supervision", JComboBox.class).setSelectedItem(BrnSupervision.Mode.NNUE_BLENDED);
                named(panel, "brn2TeacherWeight", JSpinner.class).setValue(75.0);
                named(panel, "brn2DataSeed", JTextField.class).setText("72");
                var configuration = named(panel, "brn2Configuration", JPanel.class);
                configuration.scrollRectToVisible(new java.awt.Rectangle(0, 0, configuration.getWidth(), configuration.getHeight()));
            });
            edt(() -> {
                Path image = Path.of("build/reports/gui/brn2-blended-configuration.png"); Files.createDirectories(image.getParent());
                var bitmap = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics = bitmap.createGraphics(); frame.printAll(graphics); graphics.dispose();
                javax.imageio.ImageIO.write(bitmap, "png", image.toFile()); return null;
            });
            edt(() -> named(panel, "startTraining", JButton.class).doClick());
            until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
            var state = edt(controller::state); assertEquals(TrainingController.Phase.STOPPED, state.phase(), state.message());
            assertEquals(1, state.snapshot().totals().completedGenerations());
            assertEquals(new BrnRunSeeds(71,72), CheckpointStore.readBrnRunSeeds(root).orElseThrow());
            assertEquals(BrnSupervision.blended(.75), state.snapshot().bootstrapValidation().orElseThrow().evidence().supervision());
            until(() -> edt(() -> named(panel, "brn2SupervisionStatus", JLabel.class).getText().startsWith("Next campaign")));
            assertTrue(edt(() -> named(panel, "brn2TeacherWeight", JSpinner.class).isEnabled()));
            assertTrue(edt(() -> named(panel, "validationProgress", JTextArea.class).getText()).contains("teacher 0.75, WDL 0.25"));
            assertEquals(BrnSupervision.blended(.75), new TrainingController.Backend().resolveSource(options).supervision());
        } finally { edt(controller::beginShutdown).run(); edt(frame::dispose); }
    }
}
