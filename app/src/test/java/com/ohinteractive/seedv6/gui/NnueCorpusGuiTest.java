package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

/** Non-visible components and production controller only. Never constructs a window or chooser. */
@Timeout(120)
class NnueCorpusGuiTest {
    @TempDir Path temp;
    TrainingController controller;
    TrainingPanel panel;
    TrainingController.ViewState state() {
        try { return edt(controller::state); } catch (Exception failure) { throw new AssertionError(failure); }
    }
    void awaitState(Callable<Boolean> condition) throws Exception {
        until(() -> edt(() -> { controller.poll(); return condition.call(); }));
    }
    @AfterEach void close() throws Exception { if (controller != null) edt(controller::beginShutdown).run(); }
    Path corpus() throws Exception {
        Path root = temp.resolve("corpus");
        try (var writer = new CorpusWriter(root, 8, new CorpusWriter.SourceInfo("test", "nnue-gui", "{}", "depth-then-work"))) {
            for (int i = 0; i < 32; i++) writer.put(new CorpusRecord(
                    CorpusPosition.fromFen("4k3/8/8/8/3pP3/8/8/4K3 w - - " + i + " 1"),
                    CorpusRecord.CP, 50 + i, CorpusRecord.WHITE, 20, -1, 0, 1, i + 1));
            writer.checkpoint(true);
        }
        return root;
    }
    TrainingSettings settings(Path root) { return new TrainingSettings(root, 1, 1, 4, 0, 0, 2, 4, 1, 2, 71, 8, 1); }
    void workspace() throws Exception {
        var entry = TrainingLineages.create(temp.resolve("networks"), NetworkArchitecture.NNUE, "NNUE corpus");
        TrainingLineages.save(TrainingLineages.read(entry), settings(entry.root()));
        var initial = settings(entry.root()); panel = edt(() -> new TrainingPanel(initial));
        controller = edt(() -> new TrainingController(initial, new TrainingController.Backend(), ignored -> {}, panel::showState));
        var loaded = new CompletableFuture<Exception>();
        edt(() -> { panel.bind(controller); controller.selectLineage(() -> TrainingLineages.read(entry), null, loaded::complete); });
        assertNull(loaded.get(10, TimeUnit.SECONDS));
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
    @Test void nnueChoicesShareManagementAndKeepValidationAndGeneratedValuesIndependent() throws Exception {
        workspace(); choose(corpus(), 12);
        edt(() -> {
            var source = named(panel, "brnTrainingSource", JComboBox.class);
            assertEquals(2, source.getItemCount());
            assertTrue(Arrays.asList(source.getItemAt(0), source.getItemAt(1)).contains(TrainingSource.Mode.SELF_PLAY));
            var methods = named(panel, "trainingValidationMethod", JComboBox.class);
            assertTrue(methods.isEnabled()); methods.setSelectedItem(ValidationMethod.GAME_PAIRS);
            assertTrue(named(panel, "trainingPairs", JSpinner.class).isEnabled());
            assertFalse(named(panel, "trainingGames", JSpinner.class).isEnabled());
            assertTrue(panel.applySettings()); assertEquals(NnueCorpusTargets.ID, state().settings().corpusTraining().targetAdapter());
            source.setSelectedItem(TrainingSource.Mode.SELF_PLAY);
            assertTrue(named(panel, "trainingGames", JSpinner.class).isEnabled());
            assertEquals(4, named(panel, "trainingGames", JSpinner.class).getValue());
            assertTrue(panel.applySettings()); assertNull(state().settings().config(TrainerConfig.DepthChange.REQUIRE_SAME).corpusTraining());
            var tabs = named(panel, "trainingViews", JTabbedPane.class); assertEquals("Corpus", tabs.getTitleAt(4));
            assertNotNull(named(panel, "corpusImportRoot", JTextField.class));
            return null;
        });
    }
    @Test void sharedRememberedRootMigratesAndSavedLineageRootAndAdapterTakePrecedence() throws Exception {
        Path corpus = corpus(); var prefs = Preferences.userRoot().node("seedv6-nnue-corpus-" + UUID.randomUUID());
        try {
            prefs.put("lastBrnCorpusRoot", corpus.toString()); var folders = new TrainingFolders(prefs);
            assertEquals(corpus.toString(), prefs.get("lastSeedCorpusRoot", ""));
            var source = edt(() -> new BrnTrainingSourcePanel(settings(temp.resolve("fresh")), folders, () -> {}));
            edt(() -> source.load(settings(temp.resolve("fresh")))); assertEquals(corpus.toString(), edt(source::corpusRoot));
            var own = settings(temp.resolve("saved")).withSource(TrainingSource.corpus(temp.resolve("own")))
                    .withCorpus(temp.resolve("own").toString(), new CorpusTrainingConfig(12, "a".repeat(64)));
            edt(() -> source.load(own)); assertEquals(own.corpusRoot(), edt(source::corpusRoot));
            assertEquals(own.corpusTraining(), edt(source::readCorpusConfig));
            own.save(prefs); assertEquals(own, TrainingSettings.load(prefs));
            assertEquals(own, LineageConfiguration.decode(LineageConfiguration.encode(own), own.root(), own.architecture()));
            folders.rememberCorpusRoot(temp.resolve("new-default").toString());
            assertEquals(folders.lastCorpusRoot(), new TrainingFolders(prefs).lastCorpusRoot());
            edt(() -> source.load(own)); assertEquals(own.corpusRoot(), edt(source::corpusRoot));
            byte[] legacy = Base64.getDecoder().decode(LineageConfiguration.encode(settings(temp.resolve("legacy"))));
            legacy[3] = 1; legacy = Arrays.copyOf(legacy, legacy.length - 3);
            assertEquals(settings(temp.resolve("legacy")), LineageConfiguration.decode(Base64.getEncoder().encodeToString(legacy), temp.resolve("legacy"), NetworkArchitecture.NNUE));
            byte[] v2 = Base64.getDecoder().decode(LineageConfiguration.encode(settings(temp.resolve("v2")))); v2[3] = 2;
            assertEquals(settings(temp.resolve("v2")), LineageConfiguration.decode(Base64.getEncoder().encodeToString(v2), temp.resolve("v2"), NetworkArchitecture.NNUE));
        } finally { prefs.removeNode(); }
    }
    @Test void productionControllerSettlesPersistsAndRestoresNnuePinWithoutVisibleGui() throws Exception {
        workspace(); choose(corpus(), 12);
        edt(() -> {
            named(panel, "trainingValidationMethod", JComboBox.class).setSelectedItem(ValidationMethod.HELD_OUT);
            assertTrue(panel.applySettings()); controller.start();
        });
        awaitState(() -> state().snapshot() != null && !state().active() && state().snapshot().totals().completedGenerations() == 1);
        var state = state(); assertFalse(state.snapshot().failed()); assertEquals(0, state.snapshot().totals().selfPlayGames());
        var pin = CorpusTraining.readPin(state.settings().root()).orElseThrow();
        assertEquals(pin.identity(), state.settings().corpusTraining().viewIdentity());
        var entry = new TrainingLineages.Entry(state.settings().root(), NetworkArchitecture.NNUE, "NNUE corpus");
        until(() -> TrainingLineages.read(entry).settings().equals(state.settings()));
        var saved = TrainingLineages.read(entry).settings(); assertEquals(state.settings(), saved);
        assertEquals(NnueCorpusTargets.ID, saved.corpusTraining().targetAdapter());
        until(() -> edt(() -> named(panel, "brnCorpusRoot", JTextField.class).isEnabled()));
        edt(() -> {
            assertTrue(named(panel, "brnTrainingSource", JComboBox.class).isEnabled());
            assertTrue(named(panel, "brnCorpusRoot", JTextField.class).isEnabled());
            assertTrue(named(panel, "brnCorpusPositions", JSpinner.class).isEnabled());
            assertTrue(named(panel, "trainingValidationMethod", JComboBox.class).isEnabled());
            named(panel, "brnCorpusPositions", JSpinner.class).setValue(9);
            assertTrue(panel.applySettings()); assertEquals("", state().settings().corpusTraining().viewIdentity());
            named(panel, "trainingSeed", JTextField.class).setText("72");
            assertTrue(panel.applySettings()); assertEquals("", state().settings().corpusTraining().viewIdentity());
            controller.start();
        });
        awaitState(() -> state().snapshot() != null && !state().active() && state().snapshot().generation() == 2);
        assertFalse(state().snapshot().failed(), state().message());
        assertEquals(71, CorpusTraining.evidence(saved.root(), 1).seed()); assertEquals(72, CorpusTraining.evidence(saved.root(), 2).seed());
        assertEquals(9, CorpusTraining.evidence(saved.root(), 2).requested());
    }
    @Test void stopRestoresCorpusEditorsAndChangedDraftRebindsOnRestart() throws Exception {
        workspace(); choose(corpus(), 12);
        edt(() -> {
            named(panel, "trainingValidationMethod", JComboBox.class).setSelectedItem(ValidationMethod.HELD_OUT);
            named(panel, "trainingEpochs", JSpinner.class).setValue(10000);
            assertTrue(panel.applySettings()); controller.start();
        });
        awaitState(() -> state().snapshot() != null && state().snapshot().generationOptimizerUpdates() > 0);
        edt(() -> { assertFalse(named(panel, "brnCorpusRoot", JTextField.class).isEnabled()); controller.stop(); });
        awaitState(() -> !state().active());
        var original = CorpusTraining.readPin(state().settings().root()).orElseThrow();
        until(() -> edt(() -> named(panel, "brnCorpusRoot", JTextField.class).isEnabled()));
        edt(() -> {
            assertTrue(named(panel, "brnCorpusRoot", JTextField.class).isEnabled());
            assertTrue(named(panel, "brnCorpusPositions", JSpinner.class).isEnabled());
            assertTrue(named(panel, "trainingValidationMethod", JComboBox.class).isEnabled());
            named(panel, "trainingEpochs", JSpinner.class).setValue(1);
            named(panel, "brnCorpusPositions", JSpinner.class).setValue(9);
            assertTrue(panel.applySettings()); assertEquals("", state().settings().corpusTraining().viewIdentity());
            controller.start();
        });
        awaitState(() -> state().snapshot() != null && !state().active() && state().snapshot().totals().completedGenerations() == 1);
        assertFalse(state().snapshot().failed(), state().message());
        var selected = CorpusTraining.readPin(state().settings().root()).orElseThrow();
        assertEquals(12, original.positions()); assertEquals(9, selected.positions());
        assertEquals(9, CorpusTraining.evidence(state().settings().root(), 1).requested());
        assertEquals("Restart Generation", state().snapshot().run().orElseThrow().action());
    }
}
