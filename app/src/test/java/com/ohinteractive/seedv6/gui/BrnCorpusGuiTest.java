package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class BrnCorpusGuiTest {
    @TempDir Path temp;
    TrainingController controller;
    TrainingPanel panel;
    JFrame frame;

    @BeforeEach void theme() throws Exception {
        if (!GraphicsEnvironment.isHeadless()) edt(SeedTheme::initialize);
    }

    @AfterEach void close() throws Exception {
        if (controller != null) edt(controller::beginShutdown).run();
        if (frame != null) edt(frame::dispose);
    }
    Path corpus(boolean usable) throws Exception {
        Path root = temp.resolve("corpus");
        try (var writer = new CorpusWriter(root, 4, new CorpusWriter.SourceInfo("test", "gui", "{}", "depth-then-work"))) {
            for (int i = 0; i < 12; i++) writer.put(new CorpusRecord(
                    CorpusPosition.fromFen("4k3/8/8/8/3pP3/8/8/4K3 w - - " + i + " 1"),
                    usable ? CorpusRecord.CP : CorpusRecord.MATE, 30 + i, CorpusRecord.WHITE, 20, -1,
                    CorpusRecord.UNKNOWN_WORK, 1, i + 1));
            writer.checkpoint(true);
        }
        return root;
    }
    TrainingSettings settings(Path root) {
        return new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 32, 1, 1, 73, 1, 1,
                NetworkArchitecture.BRN2, .001, .001, .002).withSource(TrainingSource.HANDCRAFTED);
    }
    void workspace() throws Exception {
        var entry = TrainingLineages.create(temp.resolve("training"), NetworkArchitecture.BRN2, "Corpus GUI test");
        TrainingLineages.save(TrainingLineages.read(entry), settings(entry.root()));
        var initial = settings(entry.root());
        panel = edt(() -> new TrainingPanel(initial));
        controller = edt(() -> new TrainingController(initial, new TrainingController.Backend(), ignored -> {}, panel::showState));
        var selected = new CompletableFuture<Exception>();
        edt(() -> { panel.bind(controller); controller.selectLineage(() -> TrainingLineages.read(entry), null, selected::complete); });
        assertNull(selected.get(10, TimeUnit.SECONDS));
        until(() -> edt(() -> named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
    }
    void choose(Path corpus, int count) throws Exception {
        edt(() -> {
            named(panel, "brnTrainingSource", JComboBox.class).setSelectedItem(TrainingSource.Mode.EXTERNAL_CORPUS);
            named(panel, "brnCorpusRoot", JTextField.class).setText(corpus.toString());
            named(panel, "brnCorpusPositions", JSpinner.class).setValue(count);
        });
        until(() -> edt(() -> named(panel, "brnCorpusStatus", JLabel.class).getText().startsWith("Valid corpus:")));
    }
    void capture(String name, JComponent focus) throws Exception {
        frame.validate();
        var viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, focus);
        var content = (JComponent) viewport.getView();
        var bounds = SwingUtilities.convertRectangle(focus, new Rectangle(0, 0, focus.getWidth(), focus.getHeight()), content);
        content.scrollRectToVisible(bounds); frame.validate();
        assertTrue(content.getVisibleRect().contains(bounds)); assertTrue(focus.isShowing());
        var image = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
        Path output = Path.of("build/gui-smoke", name); Files.createDirectories(output.getParent());
        javax.imageio.ImageIO.write(image, "png", output.toFile());
    }
    TrainingSettings train(Path corpus, boolean nativeWindow) throws Exception {
        workspace(); choose(corpus, 8);
        if (nativeWindow) edt(() -> {
            SeedTheme.initialize(); frame = new JFrame("SeedV6 corpus Network Training smoke");
            frame.setContentPane(panel); frame.setSize(980, 900);
            named(panel, "trainingViews", JTabbedPane.class).setSelectedIndex(2); frame.setVisible(true);
            return null;
        });
        if (nativeWindow) edt(() -> {
            frame.validate();
            var controls = named(panel, "brnCorpusRoot", JTextField.class);
            var viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, controls);
            var content = (JComponent) viewport.getView();
            var source = (JComponent) SwingUtilities.getAncestorOfClass(BrnTrainingSourcePanel.class, controls);
            content.scrollRectToVisible(SwingUtilities.convertRectangle(source, new Rectangle(0, 0, source.getWidth(), source.getHeight()), content));
            frame.validate();
            assertTrue(content.getVisibleRect().contains(SwingUtilities.convertRectangle(controls,
                    new Rectangle(0, 0, controls.getWidth(), controls.getHeight()), content)));
            var count = named(panel, "brnCorpusPositions", JSpinner.class);
            assertTrue(content.getVisibleRect().contains(SwingUtilities.convertRectangle(count,
                    new Rectangle(0, 0, count.getWidth(), count.getHeight()), content)));
            var image = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
            Path output = Path.of("build/gui-smoke/brn2-corpus.png"); Files.createDirectories(output.getParent());
            javax.imageio.ImageIO.write(image, "png", output.toFile());
            return null;
        });
        edt(() -> {
            assertTrue(panel.applySettings());
            var c = controller.state().settings().config(TrainerConfig.DepthChange.REQUIRE_SAME);
            assertTrue(c.source().corpus()); assertEquals(8, c.corpusTraining().positionsPerGeneration());
            assertEquals(73, c.masterSeed()); assertEquals(ValidationMethod.HELD_OUT, c.validationMethod());
            named(panel, "startTraining", JButton.class).doClick();
        });
        until(() -> edt(() -> { controller.poll(); return !controller.state().active() && controller.state().snapshot() != null; }));
        var state = edt(controller::state);
        assertEquals(TrainingController.Phase.STOPPED, state.phase(), state.message());
        assertEquals(1, state.snapshot().totals().completedGenerations());
        assertEquals(0, state.snapshot().totals().completedGames());
        assertEquals(8, state.snapshot().totals().optimizerUpdates()); assertEquals(8, state.snapshot().optimizerStep());
        var receipt = BrnCorpusTraining.evidence(state.settings().root(), 1);
        assertEquals(8, receipt.usable()); assertEquals(2, receipt.heldOut()); assertEquals(73, receipt.seed());
        var restored = TrainingLineages.read(state.lineage().entry()).settings();
        assertTrue(restored.source().corpus()); assertEquals(corpus.toAbsolutePath().normalize().toString(), restored.corpusRoot());
        assertEquals(new CorpusTrainingConfig(8, receipt.viewIdentity()), restored.corpusTraining());
        assertEquals(73, restored.seed());
        assertTrue(TrainingProgress.format(state).contains("Seed corpus root:"));
        assertFalse(TrainingProgress.format(state).contains("NNUE generator store:"));
        assertEquals("Corpus CP loss", TrainingComparison.regime(new com.ohinteractive.seedv6.training.history.HistoryRepository(restored.root()).refresh().records().getLast()));
        assertEquals("Corpus CP loss", TrainingComparison.method(state.snapshot().run().orElseThrow()));
        assertTrue(TrainingProgress.validation(state, System.nanoTime()).contains("Corpus CP"));
        System.out.println("GUI_CORPUS_SMOKE corpus=" + corpus + " output=" + restored.root()
                + " source=" + restored.source().mode().name() + " positions=" + receipt.usable()
                + " heldOut=" + receipt.heldOut() + " games=0 optimizerUpdates=" + state.snapshot().totals().optimizerUpdates()
                + " loss=" + state.snapshot().training().orElseThrow().finalLoss() + " view=" + receipt.viewIdentity());
        return restored;
    }

    @Test void enumSelectionConditionalControlsAndInactiveDraftsSurviveSwitches() throws Exception {
        workspace(); Path corpus = corpus(true);
        edt(() -> {
            var source = named(panel, "brnTrainingSource", JComboBox.class);
            assertTrue(java.util.stream.IntStream.range(0, source.getItemCount()).anyMatch(i -> source.getItemAt(i) == TrainingSource.Mode.EXTERNAL_CORPUS));
            named(panel, "brn2Supervision", JComboBox.class).setSelectedItem(BrnSupervision.Mode.NNUE_BLENDED);
            named(panel, "brn2CaptureConsistencyLambda", JSpinner.class).setValue(1.0);
            named(panel, "trainingValidationMethod", JComboBox.class).setSelectedItem(ValidationMethod.GAME_PAIRS);
        });
        choose(corpus, 9);
        edt(() -> {
            assertFalse(named(panel, "trainingGames", JSpinner.class).isEnabled());
            assertFalse(named(panel, "trainingSamples", JSpinner.class).isEnabled());
            assertFalse(named(panel, "brn2Supervision", JComboBox.class).isEnabled());
            assertFalse(named(panel, "brn2DataSeed", JTextField.class).isEnabled());
            assertTrue(named(panel, "trainingSeed", JTextField.class).isEnabled());
            assertTrue(panel.applySettings());
            var s = controller.state().settings(); var c = s.config(TrainerConfig.DepthChange.REQUIRE_SAME);
            assertEquals(TrainingSource.corpus(corpus), c.source()); assertEquals(new CorpusTrainingConfig(9), c.corpusTraining());
            assertEquals(BrnSupervision.WDL, c.supervision()); assertEquals(BrnCaptureConsistency.OFF, c.captureConsistency());
            assertEquals(ValidationMethod.GAME_PAIRS, s.validationMethod()); assertEquals(ValidationMethod.GAME_PAIRS, s.selectedValidation());
            assertEquals(ValidationMethod.GAME_PAIRS, c.validationMethod());
            assertTrue(named(panel, "trainingValidationMethod", JComboBox.class).isEnabled());
            assertTrue(named(panel, "trainingValidationMethod", JComboBox.class).isVisible());
            assertTrue(named(panel, "trainingPairs", JSpinner.class).isEnabled());
            assertTrue(named(panel, "trainingDepth", JSpinner.class).isEnabled());
            assertTrue(s.supervision().blended()); assertTrue(s.captureConsistency().enabled());
            named(panel, "brnTrainingSource", JComboBox.class).setSelectedItem(TrainingSource.Mode.HANDCRAFTED);
            assertTrue(named(panel, "trainingGames", JSpinner.class).isEnabled());
            assertTrue(named(panel, "brn2Supervision", JComboBox.class).isEnabled());
            assertTrue(panel.applySettings());
            var generated = controller.state().settings(); assertNull(generated.config(TrainerConfig.DepthChange.REQUIRE_SAME).corpusTraining());
            assertEquals(s.games(), generated.games()); assertEquals(s.samples(), generated.samples());
            assertEquals(s.supervision(), generated.supervision()); assertEquals(s.captureConsistency(), generated.captureConsistency());
            assertEquals(ValidationMethod.GAME_PAIRS, generated.selectedValidation());
            named(panel, "brnTrainingSource", JComboBox.class).setSelectedItem(TrainingSource.Mode.EXTERNAL_CORPUS);
            assertEquals(corpus.toString(), named(panel, "brnCorpusRoot", JTextField.class).getText());
            assertEquals(9, named(panel, "brnCorpusPositions", JSpinner.class).getValue());
        });
    }
    @Test void preferenceAndLineageRoundTripsAndLegacyVersionLoads() throws Exception {
        var old = settings(temp.resolve("output"));
        var s = old.withSource(TrainingSource.corpus(temp.resolve("corpus")))
                .withCorpus(temp.resolve("corpus").toString(), new CorpusTrainingConfig(17, "a".repeat(64)))
                .withRunSeeds(new BrnRunSeeds(73, 89));
        var prefs = Preferences.userRoot().node("seedv6-corpus-gui-" + UUID.randomUUID());
        try { s.save(prefs); assertEquals(s, TrainingSettings.load(prefs)); }
        finally { prefs.removeNode(); }
        assertEquals(s, LineageConfiguration.decode(LineageConfiguration.encode(s), s.root(), s.architecture()));
        // Version 1 is the exact old layout: version 2 only appends an empty path and absent config.
        byte[] legacy = Base64.getDecoder().decode(LineageConfiguration.encode(old));
        legacy[3] = 1; legacy = Arrays.copyOf(legacy, legacy.length - 3);
        assertEquals(old, LineageConfiguration.decode(Base64.getEncoder().encodeToString(legacy), old.root(), old.architecture()));
        for (var architecture : NetworkArchitecture.values()) {
            var defaults = TrainingSettings.defaults(temp.resolve(architecture.name()), architecture);
            byte[] bytes = Base64.getDecoder().decode(LineageConfiguration.encode(defaults));
            bytes[3] = 1; bytes = Arrays.copyOf(bytes, bytes.length - 3);
            assertEquals(defaults, LineageConfiguration.decode(Base64.getEncoder().encodeToString(bytes), defaults.root(), architecture));
        }
        assertThrows(IllegalArgumentException.class, () -> new CorpusTrainingConfig(1));
    }
    @Test void missingInvalidAndMalformedConfigurationAreClear() throws Exception {
        workspace();
        edt(() -> named(panel, "brnTrainingSource", JComboBox.class).setSelectedItem(TrainingSource.Mode.EXTERNAL_CORPUS));
        edt(() -> assertTrue(named(panel, "brnCorpusStatus", JLabel.class).getText().contains("No corpus selected")));
        edt(() -> named(panel, "brnCorpusRoot", JTextField.class).setText(temp.resolve("missing").toString()));
        until(() -> edt(() -> named(panel, "brnCorpusStatus", JLabel.class).getText().startsWith("Invalid/unreadable")));
        edt(() -> assertFalse(named(panel, "startTraining", JButton.class).isEnabled()));
        edt(() -> named(panel, "brnCorpusRoot", JTextField.class).setText(temp.toString()));
        until(() -> edt(() -> named(panel, "brnCorpusStatus", JLabel.class).getText().startsWith("Invalid/unreadable")));
        choose(corpus(true), 8);
        edt(() -> {
            var spinner = named(panel, "brnCorpusPositions", JSpinner.class);
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText("oops");
            assertThrows(java.text.ParseException.class, spinner::commitEdit);
        });
        var source = edt(() -> new BrnTrainingSourcePanel(controller.state().settings(), () -> {}));
        edt(() -> source.load(controller.state().settings().withSource(TrainingSource.corpus(temp.resolve("corpus")))));
        edt(() -> {
            var spinner = named(source, "brnCorpusPositions", JSpinner.class);
            for (String invalid : java.util.List.of("oops", "1", "2.5", "2147483648")) {
                ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText(invalid);
                var failure = assertThrows(IllegalArgumentException.class, source::readCorpusConfig);
                assertTrue(failure.getMessage().contains("Positions / generation must be an integer"));
            }
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText("12");
            assertEquals(new CorpusTrainingConfig(12), source.readCorpusConfig());
            return null;
        });
    }
    @Test void corpusOptionIsAbsentForNnueAndOtherBrnArchitectures() throws Exception {
        for (var architecture : java.util.List.of(NetworkArchitecture.NNUE, NetworkArchitecture.BRN, NetworkArchitecture.BRN1)) {
            var p = edt(() -> new TrainingPanel(TrainingSettings.defaults(temp.resolve(architecture.name()), architecture)));
            edt(() -> {
                var source = named(p, "brnTrainingSource", JComboBox.class);
                for (int i = 0; i < source.getItemCount(); i++) assertNotEquals(TrainingSource.Mode.EXTERNAL_CORPUS, source.getItemAt(i));
            });
        }
        var sourcePanel = edt(() -> new BrnTrainingSourcePanel(TrainingSettings.defaults(temp, NetworkArchitecture.NNUE), () -> {}));
        edt(() -> {
            sourcePanel.selectRoot(temp.toString(), NetworkArchitecture.NNUE);
            sourcePanel.load(settings(temp));
            @SuppressWarnings("unchecked") var source = (JComboBox<TrainingSource.Mode>) named(sourcePanel, "brnTrainingSource", JComboBox.class);
            var label = (JLabel) source.getRenderer().getListCellRendererComponent(new JList<>(), TrainingSource.Mode.EXTERNAL_CORPUS, 0, false, false);
            assertEquals(TrainingSource.Mode.EXTERNAL_CORPUS.toString(), label.getText());
        });
    }
    @Test void guiSavedConfigurationRunsProductionServiceAndRestoresPin() throws Exception {
        var restored = train(corpus(true), false);
        var entry = edt(() -> controller.state().lineage().entry());
        var selected = new CompletableFuture<Exception>();
        edt(() -> controller.selectLineage(() -> TrainingLineages.read(entry), null, selected::complete));
        assertNull(selected.get(10, TimeUnit.SECONDS));
        until(() -> edt(() -> named(panel, "brnCorpusStatus", JLabel.class).getText().contains("Pinned view")));
        edt(() -> {
            assertTrue(named(panel, "brnTrainingSource", JComboBox.class).isEnabled());
            assertTrue(named(panel, "brnCorpusRoot", JTextField.class).isEnabled());
            assertTrue(named(panel, "brnCorpusPositions", JSpinner.class).isEnabled());
            assertFalse(named(panel, "trainingSeed", JTextField.class).isEnabled());
            assertEquals(restored.corpusTraining(), controller.state().settings().corpusTraining());
            controller.start();
        });
        until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
        var resumed = edt(controller::state);
        assertEquals(TrainingController.Phase.STOPPED, resumed.phase(), resumed.message());
        assertEquals(2, resumed.snapshot().generation()); assertEquals(16, resumed.snapshot().optimizerStep());
        var pin = BrnCorpusTraining.readPin(restored.root()).orElseThrow();
        assertEquals(restored.corpusTraining().viewIdentity(), pin.identity());
        byte[] original = Files.readAllBytes(restored.root().resolve("corpus-training/campaign.json"));
        until(() -> edt(() -> named(panel, "brnCorpusPositions", JSpinner.class).isEnabled()));
        edt(() -> named(panel, "brnCorpusPositions", JSpinner.class).setValue(9));
        until(() -> edt(() -> named(panel, "startTraining", JButton.class).isEnabled()));
        edt(() -> { assertTrue(panel.applySettings()); assertEquals("", controller.state().settings().corpusTraining().viewIdentity()); controller.start(); });
        until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
        var changed = edt(controller::state);
        assertEquals(TrainingController.Phase.STOPPED, changed.phase(), changed.message());
        assertEquals(3, changed.snapshot().generation()); assertEquals(25, changed.snapshot().optimizerStep());
        assertEquals(8, BrnCorpusTraining.evidence(restored.root(), 1).requested());
        assertEquals(9, BrnCorpusTraining.evidence(restored.root(), 3).requested());
        var history = new com.ohinteractive.seedv6.training.history.HistoryRepository(restored.root()).refresh();
        assertTrue(history.warnings().isEmpty());
        assertTrue(history.records().getLast().regime().effectiveSettings().contains(new CorpusTrainingConfig(9, pin.identity()).settings()));
        assertArrayEquals(original, Files.readAllBytes(restored.root().resolve("corpus-training/campaign.json")));
    }
    @Test void noUsableExamplesReportsProductionFailureWithoutFallback() throws Exception {
        workspace(); choose(corpus(false), 8);
        edt(() -> { assertTrue(panel.applySettings()); controller.start(); });
        until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
        var state = edt(controller::state);
        assertEquals(TrainingController.Phase.FAILED, state.phase()); assertTrue(state.message().contains("at least four eligible CP"), state.message());
        assertEquals(TrainingSource.Mode.EXTERNAL_CORPUS, state.settings().source().mode());
    }
    @Test void realBoundedCorpusGuiSaveReloadAndProductionTrainingSmoke() throws Exception {
        String path = System.getenv("SEEDV6_CORPUS_SMOKE_ROOT");
        Assumptions.assumeTrue(path != null && !path.isBlank(), "Explicit real-corpus smoke path was not supplied");
        train(Path.of(path), false);
    }
    @Test void nativeNetworkTrainingCorpusScreenConstructsAndStarts() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        train(corpus(true), true);
    }

    @Test void validPathBecomesPersistentDefaultButNeverReplacesLineageRootOrPin() throws Exception {
        Path corpus = corpus(true);
        var prefs = Preferences.userRoot().node("seedv6-corpus-default-" + UUID.randomUUID());
        try {
            var folders = new TrainingFolders(prefs);
            var source = edt(() -> new BrnTrainingSourcePanel(settings(temp.resolve("first")), folders, () -> {}));
            edt(() -> source.load(settings(temp.resolve("first")).withSource(TrainingSource.corpus(corpus))));
            until(() -> edt(source::ready));
            assertEquals(corpus.toString(), folders.lastCorpusRoot());
            prefs.flush();
            var reloaded = new TrainingFolders(Preferences.userRoot().node(prefs.absolutePath()));
            var fresh = edt(() -> new BrnTrainingSourcePanel(settings(temp.resolve("fresh")), reloaded, () -> {}));
            edt(() -> {
                fresh.load(settings(temp.resolve("fresh")));
                named(fresh, "brnTrainingSource", JComboBox.class).setSelectedItem(TrainingSource.Mode.EXTERNAL_CORPUS);
                assertEquals(corpus.toString(), fresh.corpusRoot());
                return null;
            });
            until(() -> edt(fresh::ready));
            var own = settings(temp.resolve("own")).withSource(TrainingSource.corpus(temp.resolve("own-corpus")))
                    .withCorpus(temp.resolve("own-corpus").toString(), new CorpusTrainingConfig(17, "a".repeat(64)));
            edt(() -> {
                fresh.load(own);
                assertEquals(own.corpusRoot(), fresh.corpusRoot()); assertEquals(own.corpusTraining(), fresh.readCorpusConfig());
                named(fresh, "brnCorpusRoot", JTextField.class).setText(corpus.toString());
                assertEquals("", fresh.readCorpusConfig().viewIdentity());
                return null;
            });
            until(() -> edt(fresh::ready));
        } finally { prefs.removeNode(); }
    }

    @Test void nativeRealCorpusStopEditRestartAndNewLineageWorkflow() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        String path = System.getenv("SEEDV6_CORPUS_SMOKE_ROOT");
        Assumptions.assumeTrue(path != null && !path.isBlank(), "Explicit real-corpus smoke path was not supplied");
        var prefs = Preferences.userRoot().node("seedv6-corpus-workflow-" + UUID.randomUUID());
        try {
            var entry = TrainingLineages.create(temp.resolve("training"), NetworkArchitecture.BRN2, "Corpus remediation smoke");
            var initial = settings(entry.root()); TrainingLineages.save(TrainingLineages.read(entry), initial);
            var folders = new TrainingFolders(prefs);
            panel = edt(() -> new TrainingPanel(initial, folders));
            controller = edt(() -> new TrainingController(initial, new TrainingController.Backend(), ignored -> {}, panel::showState));
            var selected = new CompletableFuture<Exception>();
            edt(() -> { panel.bind(controller); controller.selectLineage(() -> TrainingLineages.read(entry), null, selected::complete); });
            assertNull(selected.get(10, TimeUnit.SECONDS));
            until(() -> edt(() -> named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
            choose(Path.of(path), 8);
            edt(() -> {
                frame = new JFrame("SeedV6 Network Training: corpus remediation workflow");
                frame.setContentPane(panel); frame.setSize(980, 900);
                named(panel, "trainingViews", JTabbedPane.class).setSelectedIndex(2); frame.setVisible(true);
                var validation = named(panel, "trainingValidationMethod", JComboBox.class);
                assertTrue(validation.isEnabled()); validation.setSelectedItem(ValidationMethod.GAME_PAIRS);
                // Leave enough bounded match work to exercise Stop Now after optimization completes.
                named(panel, "trainingPairs", JSpinner.class).setValue(1000);
                capture("corpus-game-validation.png", validation);
                named(panel, "startTraining", JButton.class).doClick();
                assertFalse(named(panel, "brnCorpusRoot", JTextField.class).isEnabled());
                assertFalse(named(panel, "brnTrainingSource", JComboBox.class).isEnabled());
                assertFalse(validation.isEnabled());
                return null;
            });
            until(() -> edt(() -> { controller.poll(); var s = controller.state(); return s.snapshot() != null && s.snapshot().optimizerStep() >= 8; }));
            edt(() -> {
                assertTrue(controller.state().active());
                named(panel, "stopTraining", JButton.class).doClick();
            });
            until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
            var stopped = edt(controller::state);
            assertEquals(TrainingController.Phase.STOPPED, stopped.phase(), stopped.message());
            assertEquals(0, stopped.snapshot().totals().completedGames());
            var firstReceipt = BrnCorpusTraining.evidence(entry.root(), 1);
            assertEquals(8, firstReceipt.usable());
            until(() -> edt(() -> named(panel, "brnCorpusPositions", JSpinner.class).isEnabled()));
            edt(() -> {
                assertTrue(named(panel, "brnTrainingSource", JComboBox.class).isEnabled());
                assertTrue(named(panel, "brnCorpusRoot", JTextField.class).isEnabled());
                assertTrue(named(panel, "trainingPairs", JSpinner.class).isEnabled());
                assertFalse(named(panel, "trainingSeed", JTextField.class).isEnabled()); // Existing shared seed lock.
                named(panel, "trainingPairs", JSpinner.class).setValue(1);
                named(panel, "brnCorpusPositions", JSpinner.class).setValue(9);
                capture("corpus-stopped-editable.png", named(panel, "brnCorpusRoot", JTextField.class));
                return null;
            });
            until(() -> edt(() -> named(panel, "startTraining", JButton.class).isEnabled()));
            edt(() -> named(panel, "startTraining", JButton.class).doClick());
            until(() -> edt(() -> { controller.poll(); return !controller.state().active(); }));
            var restarted = edt(controller::state);
            assertEquals(TrainingController.Phase.STOPPED, restarted.phase(), restarted.message());
            assertEquals(9, restarted.snapshot().totals().optimizerUpdates());
            assertEquals(0, restarted.snapshot().totals().completedGames());
            assertEquals(1, restarted.snapshot().validationDetails().orElseThrow().config().openingPairs());
            assertEquals("Game Pair Validation", TrainingComparison.method(restarted.snapshot().run().orElseThrow()));
            var receipt = BrnCorpusTraining.evidence(entry.root(), restarted.snapshot().generation());
            assertEquals(9, receipt.usable());
            prefs.flush();
            var freshEntry = TrainingLineages.create(temp.resolve("training"), NetworkArchitecture.BRN2, "Fresh remembered corpus");
            assertEquals(path, new TrainingFolders(Preferences.userRoot().node(prefs.absolutePath())).lastCorpusRoot());
            edt(() -> panel.selectCatalog(temp.resolve("training"), NetworkArchitecture.BRN2, () -> freshEntry));
            until(() -> edt(() -> !controller.state().loading() && controller.state().settings().root().equals(freshEntry.root())));
            edt(() -> {
                named(panel, "trainingViews", JTabbedPane.class).setSelectedIndex(2);
                named(panel, "brnTrainingSource", JComboBox.class).setSelectedItem(TrainingSource.Mode.EXTERNAL_CORPUS);
                assertEquals(path, named(panel, "brnCorpusRoot", JTextField.class).getText());
                assertEquals(2, named(panel, "brnCorpusPositions", JSpinner.class).getValue());
            });
            until(() -> edt(() -> named(panel, "brnCorpusStatus", JLabel.class).getText().startsWith("Valid corpus:")));
            edt(() -> { capture("corpus-remembered-root.png", named(panel, "brnCorpusRoot", JTextField.class)); return null; });
            System.out.println("GUI_CORPUS_REMEDIATION_SMOKE root=" + path + " output=" + entry.root()
                    + " source=EXTERNAL_CORPUS firstPositions=8 restartedPositions=9 trainingGames=0"
                    + " validationPairs=1 validPairs=" + restarted.snapshot().validation().orElseThrow().validPairs()
                    + " incompletePairs=" + restarted.snapshot().validation().orElseThrow().incompletePairs()
                    + " optimizerUpdates=9 stoppedEditable=true newLineageRemembered=true view=" + receipt.viewIdentity());
        } finally { prefs.removeNode(); }
    }
}
