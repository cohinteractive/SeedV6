package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.PlayStoreWorkflowTest.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class ModelSelectionPanelTest {
    @TempDir Path temp;
    final List<ModelSelectionPanel> panels = new ArrayList<>();
    @AfterEach void close() throws Exception { edt(() -> panels.forEach(ModelSelectionPanel::dispose)); }
    ModelSelectionPanel panel(Path root, TrainingFolders folders, Preferences preferences) throws Exception {
        var panel = edt(() -> new ModelSelectionPanel("test", "Test model", root, folders, preferences, () -> {}));
        panels.add(panel); until(() -> edt(() -> panel.validSelection() || !panel.error().isEmpty())); return panel;
    }
    @Test void swapsCompleteBindingsAndFreezesBestAgainstSubsequentPromotion() throws Exception {
        var a = LineageRedesignTest.fixture(temp, NetworkArchitecture.BRN2, "Same display name");
        var b = LineageRedesignTest.fixture(temp, NetworkArchitecture.BRN2, "Same display name");
        String bestA = brnStore(a.root()); brnStore(b.root());
        String explicitB = publish(b.root(), 3, false, true).candidateId();
        var folders = new TrainingFolders(TrainingSettings.defaults(a.root(), NetworkArchitecture.BRN2));
        var white = panel(a.root(), folders, null); var black = panel(b.root(), folders, null);
        UUID aId = TrainingLineage.read(a.root()).orElseThrow().id(), bId = TrainingLineage.read(b.root()).orElseThrow().id();
        edt(() -> {
            black.generation.setSelectedItem(new ModelChoice(explicitB));
            white.swapWith(black);
            assertEquals(b.root(), white.selectedRoot()); assertEquals(a.root(), black.selectedRoot());
            assertEquals(bId, white.selectedLineageId()); assertEquals(aId, black.selectedLineageId());
            assertEquals(explicitB, white.selectedId()); assertEquals(bestA, black.selectedId());
            assertEquals(2, white.lineage.getItemCount()); assertEquals(2, black.lineage.getItemCount());
        });
        publish(a.root(), 7, true, true);
        var selection = edt(() -> new PlayParticipants.Selection(white.selectedId(), black.selectedId(), white.selectedRoot(), black.selectedRoot(), white.selectedLineageId(), black.selectedLineageId()));
        var loaded = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, temp, selection);
        assertEquals(bestA, loaded.black().checkpointId()); assertEquals(explicitB, loaded.white().checkpointId());
        assertTrue(loaded.white().description().contains("Same display name · Gen 3"));
        assertEquals(selection, selection.swapped().swapped());
    }
    @Test void publishOnlyArenaLineageUsesLatestAndDoesNotCreateBest() throws Exception {
        Path root = temp.resolve("Arena competitor"); String id;
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            id = store.publish(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        var folders = new TrainingFolders(TrainingSettings.defaults(root, NetworkArchitecture.BRN2));
        var panel = panel(root, folders, null);
        assertTrue(edt(panel::validSelection), panel.error()); assertEquals(id, edt(panel::selectedId));
        var loaded = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, root, PlayParticipants.Selection.singleEngine(root, id));
        assertEquals(id, loaded.white().checkpointId()); assertFalse(Files.exists(root.resolve("refs/best")));
    }
    @Test void legacyStorePreferenceIsRegisteredAndExactSelectionSurvivesRestart() throws Exception {
        var entry = LineageRedesignTest.fixture(temp, NetworkArchitecture.BRN2, "Persistent choice");
        brnStore(entry.root()); String id = publish(entry.root(), 3, false, true).candidateId();
        var preferences = Preferences.userRoot().node("seedv6-model-selection-test/" + UUID.randomUUID());
        try {
            preferences.put("store", entry.root().toString());
            var folders = new TrainingFolders(TrainingSettings.defaults(entry.root(), NetworkArchitecture.BRN2));
            var first = panel(temp.resolve("unused"), folders, preferences);
            assertTrue(edt(first::validSelection), first.error());
            edt(() -> first.generation.setSelectedItem(new ModelChoice(id)));
            var second = panel(temp.resolve("unused"), folders, preferences);
            assertEquals(id, edt(second::selectedId));
            assertEquals(first.selectedLineageId(), second.selectedLineageId());
            assertTrue(folders.adopted(folders.base(), NetworkArchitecture.BRN2).contains(entry.root()));
            var wrong = new PlayParticipants.Selection(id, id, entry.root(), entry.root(), UUID.randomUUID(), null);
            assertThrows(java.io.IOException.class, () -> PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, entry.root(), wrong));
        } finally { preferences.removeNode(); }
    }
    @Test void absentBestWithPromotionEvidenceDoesNotSilentlySelectLatest() throws Exception {
        Path root = temp.resolve("missing-best"); brnStore(root); publish(root, 3, false, true);
        Files.delete(root.resolve("refs/best"));
        var snapshot = ModelLibrary.browse(ModelLibrary.identify(root));
        assertFalse(snapshot.publishOnly()); assertTrue(snapshot.best().isEmpty());
        var folders = new TrainingFolders(TrainingSettings.defaults(root, NetworkArchitecture.BRN2));
        var panel = edt(() -> new ModelSelectionPanel("test", "Test", root, folders, null, () -> {})); panels.add(panel);
        until(() -> edt(() -> panel.generation.getItemCount() == 2));
        assertFalse(edt(panel::validSelection)); assertEquals("", edt(panel::selectedId));
        edt(() -> panel.generation.setSelectedItem(new ModelChoice(snapshot.latest().orElseThrow().id())));
        assertTrue(edt(panel::validSelection));
    }
    @Test void detailsUseUnknownHistoricalEvidenceAndEditableAnnotationsWithoutChangingModels() throws Exception {
        Path root = temp.resolve("historical"); brnStore(root);
        var snapshot = ModelLibrary.browse(ModelLibrary.identify(root));
        var g = snapshot.generations().getFirst();
        var text = GenerationDetailsPanel.describe(snapshot, g);
        assertTrue(text.contains("Generation completed: not recorded"));
        assertTrue(text.contains("Lineage ID: not recorded"));
        assertTrue(text.contains("Learning rate: 0.001"));
        assertTrue(text.contains("Training loss (final): Unavailable; validation loss: Unavailable"));
        var annotation = edt(() -> {
            var panel = new GenerationDetailsPanel(snapshot, g);
            TrainingWorkspaceSmokeTest.named(panel, "generationNotes", javax.swing.JTextArea.class).setText("Strong against Gen 12");
            TrainingWorkspaceSmokeTest.named(panel, "generationTags", javax.swing.JTextField.class).setText("interesting, comparison");
            return panel.annotation();
        });
        assertEquals(g.id(), annotation.checkpointId()); assertEquals("Strong against Gen 12", annotation.notes());
        assertEquals(List.of("interesting", "comparison"), annotation.tags());
    }
    @Test void generatorAndTeacherRolesUseOnlyCompatibleBestWithoutChangingConcretePlaySelection() throws Exception {
        Path nnue = temp.resolve("teacher"); NnueGuiFixtures.bootstrap(nnue);
        var folders = new TrainingFolders(TrainingSettings.defaults(nnue, NetworkArchitecture.NNUE));
        var role = edt(() -> new ModelSelectionPanel("role", "Best per generation", nnue, folders, null, () -> {}, NetworkArchitecture.NNUE, true));
        panels.add(role); until(() -> edt(role::validSelection));
        edt(() -> {
            assertEquals(1, role.architecture.getItemCount()); assertFalse(role.architecture.isEnabled());
            assertEquals(1, role.generation.getItemCount()); assertEquals(ModelChoice.BEST, role.generation.getSelectedItem());
            assertFalse(role.generation.isEnabled()); assertTrue(role.lineage.isEnabled());
        });
        Path wrong = temp.resolve("wrong-role"); brnStore(wrong);
        edt(() -> role.selectStore(wrong)); until(() -> edt(() -> !role.error().isEmpty()));
        assertFalse(edt(role::validSelection)); assertTrue(role.error().contains("requires"));
    }
}
