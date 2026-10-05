package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class TrainingFoldersTest {
    @TempDir Path temp;
    Preferences prefs;
    @BeforeEach void setup() { prefs = Preferences.userRoot().node("seedv6-folders-" + UUID.randomUUID()); }
    @AfterEach void cleanup() throws Exception { prefs.removeNode(); }

    @Test void freshSelectionUsesMaterialButHistoricalRootsRetainLegacyIdentityAndFolder() {
        assertEquals(NetworkArchitecture.NNUE_MATERIAL, TrainingSettings.selectionDefaults(prefs).architecture());
        prefs.put(TrainingFolders.key(NetworkArchitecture.NNUE),temp.toString());
        assertEquals(NetworkArchitecture.NNUE, TrainingSettings.selectionDefaults(prefs).architecture());
        assertEquals("NNUE",NetworkArchitecture.NNUE.folderName());
        assertEquals("NNUE-Material",NetworkArchitecture.NNUE_MATERIAL.folderName());
        assertTrue(NetworkArchitecture.NNUE.toString().contains("legacy"));
    }

    @Test void independentFoldersSurviveRestartForAllArchitectures() {
        for (var arch : NetworkArchitecture.values()) settings(arch).save(prefs);
        for (var arch : NetworkArchitecture.values()) {
            prefs.put("architecture", arch.name());
            assertEquals(temp.resolve(arch.name()), TrainingSettings.load(prefs).root());
            assertEquals(temp.resolve(arch.name()).toString(), new TrainingFolders(prefs).root(arch));
        }
    }
    @Test void legacyMigratesOnlyToPreviouslySelectedArchitectureAndIsIdempotent() {
        prefs.put("root", temp.toString()); prefs.put("architecture", "BRN1");
        var folders = new TrainingFolders(prefs);
        assertEquals(temp.toString(), folders.root(NetworkArchitecture.BRN1));
        for (var arch : NetworkArchitecture.values()) if (arch != NetworkArchitecture.BRN1) assertEquals("", folders.root(arch));
        folders.select(NetworkArchitecture.BRN2); folders.remember(NetworkArchitecture.BRN2, temp.resolve("second").toString());
        var restarted = new TrainingFolders(prefs);
        assertEquals(temp.toString(), restarted.root(NetworkArchitecture.BRN1));
        assertEquals(temp.resolve("second").toString(), restarted.root(NetworkArchitecture.BRN2));
        assertEquals("", restarted.root(NetworkArchitecture.NNUE)); assertEquals(temp.toString(), prefs.get("root", null));
    }
    @Test void absentLegacyArchitectureMeansNnueAndExistingSpecificPreferenceWins() {
        prefs.put("root", temp.toString()); prefs.put(TrainingFolders.key(NetworkArchitecture.NNUE), "already-selected");
        var folders = new TrainingFolders(prefs);
        assertEquals("already-selected", folders.root(NetworkArchitecture.NNUE));
        assertEquals("", folders.root(NetworkArchitecture.BRN));
    }
    @Test void legacyWithoutArchitectureMigratesToNnue() {
        prefs.put("root", temp.toString()); var folders = new TrainingFolders(prefs);
        assertEquals(temp.toString(), folders.root(NetworkArchitecture.NNUE));
        assertEquals("", folders.root(NetworkArchitecture.BRN2));
    }
    @Test void unknownArchitectureDoesNotAssignLegacyToAnUnrelatedModel() {
        prefs.put("root", temp.toString()); prefs.put("architecture", "future-architecture");
        var folders = new TrainingFolders(prefs);
        for (var arch : NetworkArchitecture.values()) assertEquals("", folders.root(arch));
    }
    @Test void switchingArchitectureLoadsItsNamedLineageAndRemembersSelection() throws Exception {
        prefs.put("baseTrainingRoot", temp.toString());
        var entries = new java.util.EnumMap<NetworkArchitecture, TrainingLineages.Entry>(NetworkArchitecture.class);
        for (var arch : NetworkArchitecture.values()) entries.put(arch, TrainingLineages.create(temp, arch, "Lineage " + arch));
        // BRN-3 deliberately has no generator fallback: its default provider needs a registered source.
        Path source = temp.resolve("brn3-source.jsonl");
        java.nio.file.Files.writeString(source, com.ohinteractive.seedv6.training.data.SourceReadersTest.line(1));
        new com.ohinteractive.seedv6.training.data.DataSources(1, java.util.List.of(
                com.ohinteractive.seedv6.training.data.DataSource.register("Test source", source, 1)), false)
                .save(com.ohinteractive.seedv6.training.data.DataSources.directory(entries.get(NetworkArchitecture.BRN3).root()));
        var initial = TrainingLineages.read(entries.get(NetworkArchitecture.NNUE)).settings(); initial.save(prefs);
        var panel = edt(() -> new TrainingPanel(initial, new TrainingFolders(prefs)));
        var controller = edt(() -> new TrainingController(initial, new TrainingController.Backend(), ignored -> {}, panel::showState));
        try {
            edt(() -> { panel.bind(controller); panel.loadInitialLineage(); });
            NnueGuiFixtures.until(() -> edt(() -> !controller.state().loading()));
            for (var arch : new NetworkArchitecture[]{NetworkArchitecture.BRN, NetworkArchitecture.BRN2, NetworkArchitecture.BRN1, NetworkArchitecture.BRN3, NetworkArchitecture.NNUE_MATERIAL, NetworkArchitecture.NNUE}) {
                edt(() -> named(panel, "networkArchitecture", JComboBox.class).setSelectedItem(arch));
                NnueGuiFixtures.until(() -> edt(() -> !controller.state().loading()));
                if (arch == NetworkArchitecture.BRN3) NnueGuiFixtures.until(() -> edt(() ->
                        named(panel, "trainingDataSourceTable", JTable.class).getRowCount() == 1));
                edt(() -> {
                    assertEquals(entries.get(arch).root(), controller.state().settings().root());
                    assertEquals(arch, controller.state().settings().architecture());
                    assertEquals("Lineage " + arch, named(panel, "trainingLineage", JComboBox.class).getSelectedItem().toString());
                    assertEquals(4, named(panel, "trainingDepth", JSpinner.class).getValue());
                    assertTrue(panel.applySettings());
                });
            }
        } finally { edt(controller::beginShutdown).run(); }
        var restarted = new TrainingFolders(prefs);
        for (var arch : NetworkArchitecture.values()) assertEquals(entries.get(arch).root().toString(), restarted.root(arch));
        assertEquals(NetworkArchitecture.NNUE, TrainingSettings.load(prefs).architecture());
    }
    @Test void queuedOldConfigurationSaveCannotOverwriteNewSelection() {
        var old = settings(NetworkArchitecture.NNUE); old.save(prefs);
        var folders = new TrainingFolders(prefs); folders.select(NetworkArchitecture.BRN2);
        folders.remember(NetworkArchitecture.NNUE, temp.resolve("newer").toString());
        old.saveConfiguration(prefs);
        assertEquals("BRN2", prefs.get("architecture", null));
        assertEquals(temp.resolve("newer").toString(), new TrainingFolders(prefs).root(NetworkArchitecture.NNUE));
    }
    TrainingSettings settings(NetworkArchitecture architecture) {
        return new TrainingSettings(temp.resolve(architecture.name()), 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 4, 1, architecture);
    }
}
