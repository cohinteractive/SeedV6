package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

/** Configuration only: no games, searches or optimizer updates. */
class TrainingValidationGamesTest {
    @TempDir Path temp;

    private TrainingSettings settings(NetworkArchitecture architecture) {
        return new TrainingSettings(temp.resolve(architecture.name()), 1, 1, 4, 0, 0, 2,
                32, 1, 2, 71, 8, 1, architecture, .001, .001, .001);
    }

    private static void ready(TrainingPanel panel, NetworkArchitecture architecture) throws Exception {
        if (architecture == NetworkArchitecture.NNUE) return;
        until(() -> edt(() -> named(panel, "brnTrainingSource", JComboBox.class).isEnabled()
                && (architecture != NetworkArchitecture.BRN2
                    || named(panel, "brn2Supervision", JComboBox.class).isEnabled())));
    }

    @ParameterizedTest @EnumSource(NetworkArchitecture.class)
    void gameSelectionPersistsAndReachesFreshBackendWithNormalEditingLocks(NetworkArchitecture architecture) throws Exception {
        var initial = settings(architecture).withValidationMethod(ValidationMethod.GAME_PAIRS);
        var panel = edt(() -> new TrainingPanel(initial));
        ready(panel, architecture);
        var backend = new TrainingController.Backend();
        var controller = edt(() -> new TrainingController(initial, backend, s -> {}, panel::showState));
        try {
            var selected = edt(() -> {
                panel.bind(controller);
                named(panel, "trainingViews", JTabbedPane.class).setSelectedIndex(2);
                var source = named(panel, "brnTrainingSource", JComboBox.class);
                if (architecture != NetworkArchitecture.NNUE) {
                    assertEquals(architecture == NetworkArchitecture.BRN2 ? TrainingSource.Mode.HANDCRAFTED
                            : TrainingSource.Mode.NNUE_BOOTSTRAP, source.getSelectedItem());
                    assertTrue(source.isEnabled());
                    assertTrue(SwingUtilities.getAncestorOfClass(BrnTrainingSourcePanel.class, source).isVisible());
                    assertTrue(java.util.stream.IntStream.range(0, source.getItemCount())
                            .anyMatch(i -> source.getItemAt(i) == TrainingSource.Mode.SELF_PLAY));
                    source.setSelectedItem(TrainingSource.Mode.SELF_PLAY);
                }
                var pairs = named(panel, "trainingPairs", JSpinner.class);
                assertTrue(pairs.isEnabled()); pairs.setValue(7);
                assertTrue(panel.applySettings());
                var result = controller.state().settings();
                assertEquals(architecture == NetworkArchitecture.NNUE ? null : TrainingSource.SELF_PLAY, result.source());
                assertEquals(7, result.validationPairs());
                for (var phase : java.util.List.of(TrainingController.Phase.STARTING, TrainingController.Phase.RUNNING,
                        TrainingController.Phase.STOPPING, TrainingController.Phase.CLOSING)) {
                    panel.showState(new TrainingController.ViewState(result, phase, null, "", true, false, false, 1, ""));
                    assertFalse(pairs.isEnabled()); assertFalse(source.isEnabled());
                    assertEquals(7, pairs.getValue());
                }
                panel.showState(controller.state());
                return result;
            });
            // Stopping triggers an asynchronous reread of BRN-2 lineage controls.
            ready(panel, architecture);
            edt(() -> assertTrue(named(panel, "trainingPairs", JSpinner.class).isEnabled()));
            var resolved = backend.resolveSource(selected);
            var config = resolved.config(TrainerConfig.DepthChange.REQUIRE_SAME);
            assertEquals(architecture.trainingArchitecture(), config.architecture());
            assertEquals(selected.source(), config.source());
            assertEquals(7, config.validation().openingPairs());
            assertEquals("Start Training", backend.preview(resolved));
            assertFalse(backend.inspect(resolved).resume());
            var handle = backend.create(resolved, false, TrainerConfig.DepthChange.REQUIRE_SAME);
            handle.close(); // Exercise production construction without starting training.
            assertFalse(Files.exists(selected.root()));

            var prefs = Preferences.userRoot().node("seedv6-validation-games-" + UUID.randomUUID());
            try {
                selected.save(prefs);
                var restored = TrainingSettings.load(prefs);
                assertEquals(selected, restored);
                var reopened = edt(() -> new TrainingPanel(restored));
                ready(reopened, architecture);
                edt(() -> {
                    assertTrue(named(reopened, "trainingPairs", JSpinner.class).isEnabled());
                    assertEquals(7, named(reopened, "trainingPairs", JSpinner.class).getValue());
                    if (architecture != NetworkArchitecture.NNUE)
                        assertEquals(TrainingSource.Mode.SELF_PLAY, named(reopened, "brnTrainingSource", JComboBox.class).getSelectedItem());
                });
            } finally { prefs.removeNode(); }
        } finally { edt(controller::beginShutdown).run(); }
    }

    @ParameterizedTest @EnumSource(value = TrainingSource.Mode.class, names = {"SELF_PLAY", "HANDCRAFTED", "NNUE_BOOTSTRAP"})
    void brn2ModeSurvivesArchitectureFolderAndConfigurationChanges(TrainingSource.Mode mode) throws Exception {
        var expected = new TrainingSource(mode, temp.resolve("nnue-generator").toString());
        var initial = settings(NetworkArchitecture.BRN2).withSource(expected).withValidationMethod(ValidationMethod.GAME_PAIRS);
        var panel = edt(() -> new TrainingPanel(initial));
        ready(panel, NetworkArchitecture.BRN2);
        var controller = edt(() -> new TrainingController(initial, new TrainingController.Backend(), s -> {}, panel::showState));
        try {
            edt(() -> {
                panel.bind(controller);
                assertEquals(mode, named(panel, "brnTrainingSource", JComboBox.class).getSelectedItem());
                named(panel, "networkArchitecture", JComboBox.class).setSelectedItem(NetworkArchitecture.NNUE);
                assertTrue(named(panel, "trainingPairs", JSpinner.class).isEnabled());
                named(panel, "networkArchitecture", JComboBox.class).setSelectedItem(NetworkArchitecture.BRN2);
            });
            ready(panel, NetworkArchitecture.BRN2);
            edt(() -> {
                assertEquals(mode, named(panel, "brnTrainingSource", JComboBox.class).getSelectedItem());
                named(panel, "trainingRoot", JTextField.class).setText(temp.resolve("another-fresh-store").toString());
            });
            ready(panel, NetworkArchitecture.BRN2);
            edt(() -> {
                assertEquals(TrainingSource.Mode.HANDCRAFTED, named(panel, "brnTrainingSource", JComboBox.class).getSelectedItem());
                named(panel, "trainingRoot", JTextField.class).setText(initial.root().toString());
            });
            ready(panel, NetworkArchitecture.BRN2);
            var selected = edt(() -> {
                assertEquals(mode, named(panel, "brnTrainingSource", JComboBox.class).getSelectedItem());
                assertTrue(named(panel, "trainingPairs", JSpinner.class).isEnabled());
                assertEquals(ValidationMethod.GAME_PAIRS, named(panel, "trainingValidationMethod", JComboBox.class).getSelectedItem());
                named(panel, "trainingDepth", JSpinner.class).setValue(2);
                assertTrue(panel.applySettings());
                return controller.state().settings();
            });
            assertEquals(expected, selected.source());
            assertEquals(expected, new TrainingController.Backend().resolveSource(selected).config(TrainerConfig.DepthChange.REQUIRE_SAME).source());
            assertEquals(mode != TrainingSource.Mode.SELF_PLAY, selected.source().bootstrap());
        } finally { edt(controller::beginShutdown).run(); }
    }

    @ParameterizedTest @EnumSource(value = TrainingSource.Mode.class, names = {"SELF_PLAY", "HANDCRAFTED", "NNUE_BOOTSTRAP"})
    void storedBrn2SourceIsAnEditableDefaultAndExplicitDraftIsPreserved(TrainingSource.Mode mode) throws Exception {
        var initial = settings(NetworkArchitecture.BRN2);
        var stored = new TrainingSource(mode, temp.resolve("nnue-generator").toString());
        try (var store = new CheckpointStore(initial.root(), TrainingArchitecture.BRN2)) {
            store.writeTrainingSource(stored);
            store.initialize(new com.ohinteractive.seedv6.training.model.NetworkTrainingState.Brn2(new com.ohinteractive.seedv6.core.brn2.Brn2Trainer(.001)),
                    new CheckpointManifest.Metadata(0, 1, ""));
        }
        byte[] before = Files.readAllBytes(initial.root().resolve(CheckpointStore.TRAINING_SOURCE_FILE));
        var draft = initial.withSource(mode == TrainingSource.Mode.SELF_PLAY ? TrainingSource.HANDCRAFTED : TrainingSource.SELF_PLAY);
        var panel = edt(() -> new BrnTrainingSourcePanel(draft, () -> {}));
        edt(() -> panel.selectRoot(initial.root().toString(), NetworkArchitecture.BRN2));
        until(() -> edt(panel::ready));
        edt(() -> {
            assertEquals(draft.source(), panel.read());
            assertTrue(named(panel, "brnTrainingSource", JComboBox.class).isEnabled());
            assertEquals(3, named(panel, "brnTrainingSource", JComboBox.class).getItemCount());
            panel.setEditable(false); panel.setEditable(true);
            assertTrue(named(panel, "brnTrainingSource", JComboBox.class).isEnabled());
        });
        var backend = new TrainingController.Backend();
        assertEquals(draft.source(), backend.resolveSource(draft).source());
        assertEquals(stored, backend.resolveSource(initial).source());
        assertArrayEquals(before, Files.readAllBytes(initial.root().resolve(CheckpointStore.TRAINING_SOURCE_FILE)));
    }
}
