package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;

class BrnPair2GuiTest {
    @TempDir Path root;
    @Test void applicationIdentityChangesWithoutChangingLegacyPersistence() throws Exception {
        assertEquals("BRE-Pair 2", NetworkArchitecture.BRN_PAIR2.toString());
        assertEquals("BRE-Pair 2", TrainingArchitecture.BRN_PAIR2.displayName());
        assertEquals("BRN_PAIR2", TrainingArchitecture.BRN_PAIR2.name());
        assertEquals("BRN-Pair2", TrainingArchitecture.BRN_PAIR2.folderName());
        assertEquals("network.brn-pair2", TrainingArchitecture.BRN_PAIR2.networkFile());
        assertEquals(TrainingArchitecture.BRN_PAIR2, TrainingArchitecture.fromSchema("seedv6.brn.pair2", 1));
        assertEquals(TrainingArchitecture.BRN_PAIR2, com.ohinteractive.seedv6.training.data.DataFiles.JSON.fromJson("\"BRN_PAIR2\"", TrainingArchitecture.class));
    }
    @Test void independentManagedLineagesDefaultsPreferencesAndPlaySelection()throws Exception {
        var pair=TrainingLineages.create(root,NetworkArchitecture.BRN_PAIR2,"Pair scale run");
        var brn=TrainingLineages.create(root,NetworkArchitecture.BRN3,"Native BRN");
        var nnue=TrainingLineages.create(root,NetworkArchitecture.NNUE_MATERIAL,"NNUE");
        assertEquals(root.resolve("BRN-Pair2"),pair.root().getParent());
        assertNotEquals(pair.root(),brn.root());assertNotEquals(pair.root(),nnue.root());
        var settings=TrainingLineages.read(pair).settings();
        assertEquals(new TrainingRecipe(.01,128,8),settings.recipe());
        assertTrue(settings.source().corpus());assertEquals(ValidationMethod.HELD_OUT,settings.validationMethod());
        var prefs=Preferences.userRoot().node("seedv6-pair2-test-"+UUID.randomUUID());
        try {
            var nativeSettings=TrainingLineages.read(brn).settings();nativeSettings.save(prefs);
            String nativeRoot=prefs.get(TrainingFolders.key(NetworkArchitecture.BRN3),"");
            settings.save(prefs);assertEquals(settings,TrainingSettings.load(prefs));
            assertEquals(nativeRoot,prefs.get(TrainingFolders.key(NetworkArchitecture.BRN3),""));
        }finally{prefs.removeNode();}
        CheckpointStore.Checkpoint checkpoint;
        try(var store=new CheckpointStore(pair.root(),TrainingArchitecture.BRN_PAIR2)) {
            checkpoint=store.initialize(NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2,1,.01),new CheckpointManifest.Metadata(0,1,""));
        }
        assertThrows(java.io.IOException.class,()->new CheckpointStore(pair.root(),TrainingArchitecture.BRN3));
        var selected=PlayEvaluator.load(pair.root(),checkpoint.manifest().id());
        assertEquals(TrainingArchitecture.BRN_PAIR2,selected.architecture());
        assertTrue(selected.description().contains("BRE-Pair 2"));assertEquals(0,selected.evaluation().newState(2).evaluate(Board.startingPosition(),0));
        assertEquals(selected.checkpointId(),PlayEvaluator.loadBest(pair.root()).checkpointId());
        assertTrue(CheckpointInspection.freshRoot(brn.root(),TrainingArchitecture.BRN3));
        assertTrue(CheckpointInspection.freshRoot(nnue.root(),TrainingArchitecture.NNUE_MATERIAL));
    }
    @Test void capabilitiesAndRecipeEditorsAppearInExistingTrainingPanel()throws Exception {
        var settings=TrainingSettings.defaults(root,NetworkArchitecture.BRN_PAIR2);
        edt(()-> {
            var panel=new TrainingPanel(settings);
            var selector=named(panel,"networkArchitecture",JComboBox.class);
            assertEquals(NetworkArchitecture.BRN_PAIR2,selector.getSelectedItem());
            var information=named(panel,"brnPair2TrainingInformation",JTextArea.class);
            assertTrue(information.getText().contains("C02"));
            assertEquals(128,((Number)named(panel,"trainingMinibatch",JSpinner.class).getValue()).intValue());
            assertEquals(8,((Number)named(panel,"trainingEpochs",JSpinner.class).getValue()).intValue());
        });
    }
}
