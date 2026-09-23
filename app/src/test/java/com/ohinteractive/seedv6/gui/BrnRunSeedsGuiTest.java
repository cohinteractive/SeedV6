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

class BrnRunSeedsGuiTest {
    @TempDir Path temp;
    TrainingSettings settings(Path root) {
        return new TrainingSettings(root, 2, 6, 64, 0, 8, 32, 32, 1, 64, 1, 1024, 127,
                NetworkArchitecture.BRN2, .001, .001, .001).withSource(TrainingSource.bootstrap(temp.resolve("nnue")));
    }
    @Test void newSeedIsFolderBoundAndFollowsLifecycleLocks() throws Exception {
        var options = settings(temp.resolve("new"));
        var panel = edt(() -> new TrainingPanel(options));
        var controller = edt(() -> new TrainingController(options, new TrainingController.Backend(), s -> {}, panel::showState));
        try {
            until(() -> edt(() -> named(panel,"brn2DataSeed",JTextField.class).isEnabled()));
            edt(() -> {
                panel.bind(controller); var field = named(panel,"brn2DataSeed",JTextField.class);
                assertEquals("",field.getText()); field.setText("2"); assertTrue(panel.applySettings());
                assertEquals(new BrnRunSeeds(1,2),controller.state().settings().config(TrainerConfig.DepthChange.REQUIRE_SAME).runSeeds());
                for(var phase: List.of(TrainingController.Phase.STARTING,TrainingController.Phase.CONFIRM_DEPTH,
                        TrainingController.Phase.RUNNING,TrainingController.Phase.STOPPING,TrainingController.Phase.CLOSING)) {
                    panel.showState(new TrainingController.ViewState(options,phase,null,"",phase != TrainingController.Phase.CLOSING,false,false,2,""));
                    assertFalse(field.isEnabled(),phase.name()); assertFalse(named(panel,"trainingSeed",JTextField.class).isEnabled(),phase.name());
                }
            });
            var prefs = Preferences.userRoot().node("seedv6-run-seeds-"+UUID.randomUUID());
            try {
                var selected = options.withRunSeeds(new BrnRunSeeds(1,2)); selected.save(prefs);
                assertEquals(selected,TrainingSettings.load(prefs));
                prefs.put(TrainingFolders.key(NetworkArchitecture.BRN2),temp.resolve("other").toString());
                assertNull(TrainingSettings.load(prefs).runSeeds());
            } finally { prefs.removeNode(); }
        } finally { edt(controller::beginShutdown).run(); }
    }
    @Test void storedSeedsRestoreVisibleMasterAndDataAndLockStoppedLineage() throws Exception {
        Path root=temp.resolve("stored");
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN2)) {
            store.initializeBrnRunSeeds(new BrnRunSeeds(1,2));
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),new CheckpointManifest.Metadata(0,2,""));
        }
        var options=settings(root); var panel=edt(() -> new TrainingPanel(options));
        var controller=edt(() -> new TrainingController(options,new TrainingController.Backend(),s -> {},panel::showState));
        try {
            edt(() -> panel.bind(controller));
            until(() -> edt(() -> named(panel,"brn2DataSeed",JTextField.class).getText().equals("2")));
            edt(() -> {
                assertEquals("1",named(panel,"trainingSeed",JTextField.class).getText());
                assertFalse(named(panel,"trainingSeed",JTextField.class).isEnabled());
                assertFalse(named(panel,"brn2DataSeed",JTextField.class).isEnabled());
                assertTrue(panel.applySettings());
                assertEquals(new BrnRunSeeds(1,2),controller.state().settings().runSeeds());
            });
            assertEquals(new BrnRunSeeds(1,2),new TrainingController.Backend().resolveSource(options).runSeeds());
            assertThrows(java.io.IOException.class,() -> new TrainingController.Backend().resolveSource(options.withRunSeeds(new BrnRunSeeds(1,3))));
        } finally { edt(controller::beginShutdown).run(); }
    }
    @Test void seedOnlyInterruptedInitializationDoesNotInventLockedWdlObjective() throws Exception {
        Path root = temp.resolve("seed-only");
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.initializeBrnRunSeeds(new BrnRunSeeds(1, 2));
        }
        var panel = edt(() -> new Brn2ConfigurationPanel(settings(root).withSupervision(BrnSupervision.blended(.75))));
        edt(() -> panel.selectRoot(root.toString(), NetworkArchitecture.BRN2)); until(() -> edt(panel::ready));
        edt(() -> {
            assertFalse(named(panel, "brn2DataSeed", JTextField.class).isEnabled());
            assertTrue(named(panel, "brn2Supervision", JComboBox.class).isEnabled());
            assertEquals(new BrnRunSeeds(1, 2), panel.readRunSeeds(999));
            assertEquals(BrnSupervision.blended(.75), panel.readSupervision());
            return null;
        });
    }
}
