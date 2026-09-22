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

    @Test void independentFoldersSurviveRestartForAllFourArchitectures() {
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
    @Test void switchingRetainsUnappliedEditsLoadsEmptyThenRestoresExactPathsAndStartSettings() throws Exception {
        settings(NetworkArchitecture.NNUE).save(prefs);
        var initial = TrainingSettings.load(prefs);
        var panel = edt(() -> new TrainingPanel(initial, new TrainingFolders(prefs)));
        var controller = edt(() -> new TrainingController(initial, new TrainingController.Backend(), s -> s.saveConfiguration(prefs), panel::showState));
        try {
            edt(() -> {
                panel.bind(controller);
                var selector = named(panel, "networkArchitecture", JComboBox.class);
                var field = named(panel, "trainingRoot", JTextField.class);
                for (var arch : new NetworkArchitecture[]{NetworkArchitecture.BRN, NetworkArchitecture.BRN2, NetworkArchitecture.BRN1}) {
                    selector.setSelectedItem(arch); assertEquals("", field.getText(), "Never selected must be unset");
                    field.setText(temp.resolve(arch.name()).toString());
                }
                for (var arch : new NetworkArchitecture[]{NetworkArchitecture.NNUE, NetworkArchitecture.BRN, NetworkArchitecture.BRN2, NetworkArchitecture.NNUE}) {
                    selector.setSelectedItem(arch); assertEquals(temp.resolve(arch.name()).toString(), field.getText());
                    assertTrue(panel.applySettings()); assertEquals(arch, controller.state().settings().architecture());
                    assertEquals(temp.resolve(arch.name()), controller.state().settings().config(com.ohinteractive.seedv6.training.service.TrainerConfig.DepthChange.REQUIRE_SAME).checkpointRoot());
                }
            });
        } finally { edt(controller::beginShutdown).run(); }
        var restarted = new TrainingFolders(prefs);
        for (var arch : NetworkArchitecture.values()) assertEquals(temp.resolve(arch.name()).toString(), restarted.root(arch));
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
