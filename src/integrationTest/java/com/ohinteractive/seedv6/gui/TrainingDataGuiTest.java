package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;

/** Replaces the former import/pinned-view GUI scenarios with the source-based workflow. */
@Timeout(120)
class TrainingDataGuiTest {
    @TempDir Path temporary;
    TrainingController controller;
    @AfterEach void close() throws Exception { if (controller != null) edt(controller::beginShutdown).run(); }
    TrainingSettings settings(NetworkArchitecture architecture, Path root) {
        return new TrainingSettings(root, 1, 1, 4, 0, 0, 2, 4, 1, 2, 71, 8, 1,
                architecture, .001, .001, .001, TrainingSource.dataSources(DataSources.directory(root)), "", null)
                .withCorpus(DataSources.directory(root).toString(), new CorpusTrainingConfig(4)).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    @ParameterizedTest @EnumSource(value = NetworkArchitecture.class, names = {"NNUE", "BRN2"})
    void productionControllerPersistsConfigurationAndSettlesSourceCursor(NetworkArchitecture architecture) throws Exception {
        var entry = LineageRedesignTest.fixture(temporary.resolve("networks"), architecture, "Sequential");
        var settings = settings(architecture, entry.root());
        Path source = temporary.resolve("positions.jsonl"); var text = new StringBuilder();
        for (int i = 0; i < 40; i++) text.append(com.ohinteractive.seedv6.training.data.SourceReadersTest.line(i)); Files.writeString(source, text);
        try (var store = new CheckpointStore(entry.root(), architecture.trainingArchitecture())) { new DataSources(1, List.of(DataSource.register("Source", source, 1)), false).save(DataSources.directory(entry.root())); }
        TrainingLineages.save(TrainingLineages.read(entry), settings);
        var panel = edt(() -> new TrainingPanel(settings));
        controller = edt(() -> new TrainingController(settings, new TrainingController.Backend(), ignored -> {}, panel::showState));
        var loaded = new CompletableFuture<Exception>();
        edt(() -> { panel.bind(controller); controller.selectLineage(() -> TrainingLineages.read(entry), null, loaded::complete); });
        assertNull(loaded.get(10, TimeUnit.SECONDS));
        until(() -> edt(() -> named(panel, "startTraining", JButton.class).isEnabled()));
        edt(() -> { assertTrue(panel.applySettings()); controller.start(); });
        until(() -> edt(() -> { controller.poll(); return !controller.state().active() && controller.state().snapshot() != null; }));
        var state = edt(controller::state); assertFalse(state.snapshot().failed(), state.message()); assertEquals(1, state.snapshot().totals().completedGenerations());
        assertEquals(0, state.snapshot().totals().selfPlayGames());
        var ledger = new SourceLedger(entry.root()); assertEquals(6, ledger.state().next().values().iterator().next()); assertTrue(ledger.active().isEmpty());
        assertFalse(Files.exists(entry.root().resolve("corpus-training/view.json")));
        until(() -> { var saved = TrainingLineages.read(entry);
            return LineageConfiguration.decode(saved.lineage().configuration(), entry.root(), architecture).equals(edt(controller::state).settings());
        });
        assertTrue(TrainingLineages.read(entry).settings().source().dataSources());
        edt(() -> { named(panel, "brnTrainingSource", JComboBox.class).setSelectedItem(TrainingSource.Mode.SELF_PLAY); assertTrue(named(panel, "trainingGames", JSpinner.class).isVisible()); });
    }
    @ParameterizedTest @EnumSource(value = NetworkArchitecture.class, names = {"NNUE", "BRN2"})
    void newAndLegacyConfigurationRoundTripsPreserveStoredMetadata(NetworkArchitecture architecture) throws Exception {
        Path root = temporary.resolve("lineage"); var selected = settings(architecture, root);
        Preferences prefs = Preferences.userRoot().node("seed-training-data-test-" + UUID.randomUUID());
        try {
            selected.save(prefs); assertEquals(selected, TrainingSettings.load(prefs));
            assertEquals(selected, LineageConfiguration.decode(LineageConfiguration.encode(selected), root, architecture));
            var legacy = selected.withSource(TrainingSource.corpus(temporary.resolve("legacy")))
                    .withCorpus(temporary.resolve("legacy").toString(), new CorpusTrainingConfig(7, "a".repeat(64)));
            assertEquals(legacy, LineageConfiguration.decode(LineageConfiguration.encode(legacy), root, architecture));
            var panel = edt(() -> new BrnTrainingSourcePanel(legacy, () -> {}));
            assertEquals(legacy.source(), edt(panel::read)); assertEquals(legacy.corpusTraining(), edt(panel::readCorpusConfig));
        } finally { prefs.removeNode(); }
    }
}
