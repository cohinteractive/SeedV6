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

class BrnHandcraftedGuiTest {
    @TempDir Path temp;
    TrainingSettings settings(Path root) {
        return new TrainingSettings(root,2,1,4,0,0,2,32,1,2,1,8,1,NetworkArchitecture.BRN2,.001,.001,.001);
    }
    @Test void freshAxesDefaultsTeacherAvailabilityPreferencesAndLocks() throws Exception {
        var settings=settings(temp.resolve("fresh"));var panel=edt(()->new TrainingPanel(settings));
        until(()->edt(()->named(panel,"brnTrainingSource",JComboBox.class).isEnabled()&&named(panel,"brn2Supervision",JComboBox.class).isEnabled()));
        var controller=edt(()->new TrainingController(settings,new TrainingController.Backend(),s->{},panel::showState));
        try {
            edt(()->{
                panel.bind(controller);
                var source=named(panel,"brnTrainingSource",JComboBox.class);var supervision=named(panel,"brn2Supervision",JComboBox.class);
                var teacher=named(panel,"nnueTeacherStore",JTextField.class);var generator=named(panel,"nnueGeneratorStore",JTextField.class);
                assertEquals(TrainingSource.Mode.HANDCRAFTED,source.getSelectedItem());assertEquals(2,source.getItemCount());
                assertTrue(SwingUtilities.isDescendingFrom(source,named(panel,"brn2Configuration",JPanel.class)));
                assertEquals(BrnSupervision.Mode.WDL,supervision.getSelectedItem());assertFalse(generator.isEnabled());
                assertTrue(panel.applySettings());assertEquals(TrainingSource.HANDCRAFTED,controller.state().settings().source());
                supervision.setSelectedItem(BrnSupervision.Mode.NNUE_BLENDED);assertTrue(teacher.isEnabled());assertFalse(generator.isEnabled());
                teacher.setText(temp.resolve("nnue").toString());named(panel,"brn2TeacherWeight",JSpinner.class).setValue(75.0);
                assertTrue(panel.applySettings());var selected=controller.state().settings();
                assertEquals(TrainingSource.HANDCRAFTED,selected.source());assertEquals(teacher.getText(),selected.teacherStore());
                assertEquals(BrnSupervision.blended(.75),selected.supervision());
                var prefs=Preferences.userRoot().node("seedv6-handcrafted-"+UUID.randomUUID());
                try {selected.save(prefs);assertEquals(selected,TrainingSettings.load(prefs));}
                finally {prefs.removeNode();}
                source.setSelectedItem(TrainingSource.Mode.NNUE_BOOTSTRAP);assertTrue(generator.isEnabled());assertTrue(teacher.isEnabled());
                for(var phase:List.of(TrainingController.Phase.STARTING,TrainingController.Phase.RUNNING,TrainingController.Phase.STOPPING,TrainingController.Phase.CLOSING)) {
                    panel.showState(new TrainingController.ViewState(selected,phase,null,"",true,false,false,2,""));
                    assertFalse(source.isEnabled());assertFalse(teacher.isEnabled());assertFalse(supervision.isEnabled());
                }
                return null;
            });
        } finally {edt(controller::beginShutdown).run();}
    }
    @Test void frozenStoreIsDisplayedAndGuiStartFailsBeforeOpeningAWriter() throws Exception {
        Path root=temp.resolve("frozen"),origin=Files.createDirectory(temp.resolve("control"));
        String id="g000000-s000000000-"+"a".repeat(64),hash="0".repeat(64);
        var replay=new FrozenReplay(origin.toRealPath().toString(),id,hash,hash,new BrnRunSeeds(1,1),
                settings(root).config(TrainerConfig.DepthChange.REQUIRE_SAME).selfPlay(),new TrainerConfig.Training(1,1,true),
                TrainerConfig.STANDARD_START,.001,List.of(new FrozenReplay.Entry(1,id,hash,hash,hash)));
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN2)) {
            store.initializeFrozenReplay(replay);store.writeTrainingSource(TrainingSource.FROZEN_REPLAY);
        }
        var panel=edt(()->new BrnTrainingSourcePanel(settings(root),()->{}));
        edt(()->{panel.selectRoot(root.toString(),NetworkArchitecture.BRN2);return null;});
        until(()->edt(panel::ready));
        assertEquals(TrainingSource.FROZEN_REPLAY,edt(panel::read));
        assertFalse(edt(()->named(panel,"brnTrainingSource",JComboBox.class).isEnabled()));
        byte[] metadata=Files.readAllBytes(root.resolve(FrozenReplay.FILE));
        var lockTime=Files.getLastModifiedTime(root.resolve("store.lock"));
        var failure=assertThrows(java.io.IOException.class,()->new TrainingController.Backend().inspect(settings(root)));
        assertTrue(failure.getMessage().contains("frozen-wdl"));
        assertArrayEquals(metadata,Files.readAllBytes(root.resolve(FrozenReplay.FILE)));
        assertEquals(lockTime,Files.getLastModifiedTime(root.resolve("store.lock")));
        try(var checkpoints=Files.list(root.resolve("checkpoints"))){assertEquals(0,checkpoints.count());}
    }
    @Test void historicalNnueSourceOverridesStaleHandcraftedDraftAndTeacherComesFromLegacyPin() throws Exception {
        Path root=temp.resolve("old"),nnue=temp.resolve("nnue");
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN2)) {
            store.initializeBrnSupervision(BrnSupervision.blended(.75));store.writeTrainingSource(TrainingSource.bootstrap(nnue));
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),new CheckpointManifest.Metadata(0,2,""));
        }
        var stale=settings(root).withSource(TrainingSource.HANDCRAFTED).withTeacherStore(temp.resolve("wrong").toString());
        var panel=edt(()->new TrainingPanel(stale));
        until(()->edt(()->!named(panel,"brn2SupervisionStatus",JLabel.class).getText().startsWith("Reading")));
        until(()->edt(()->named(panel,"brnTrainingSource",JComboBox.class).getSelectedItem()==TrainingSource.Mode.NNUE_BOOTSTRAP));
        edt(()->{
            assertFalse(named(panel,"brnTrainingSource",JComboBox.class).isEnabled());
            assertFalse(named(panel,"nnueGeneratorStore",JTextField.class).isEnabled());
            assertFalse(named(panel,"nnueTeacherStore",JTextField.class).isEnabled());
            assertEquals(nnue.toString(),named(panel,"nnueTeacherStore",JTextField.class).getText());
        });
        assertThrows(java.io.IOException.class,()->new TrainingController.Backend().resolveSource(stale));
        var restored=new TrainingController.Backend().resolveSource(settings(root));
        assertEquals(TrainingSource.bootstrap(nnue),restored.source());assertEquals(nnue.toString(),restored.teacherStore());
        assertFalse(Files.exists(root.resolve(CheckpointStore.BRN_TEACHER_FILE)));
    }
    @Test @Timeout(120) void nativeHandcraftedBlendStartSettlesAndLocksSourceAndTeacher() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());edt(SeedTheme::initialize);
        Path root=temp.resolve("native"),nnue=temp.resolve("native-nnue");
        try(var store=new CheckpointStore(nnue,TrainingArchitecture.NNUE)) {
            store.initialize(new com.ohinteractive.seedv6.training.nnue.NnueTrainer(com.ohinteractive.seedv6.training.nnue.TrainableNnue.initialized(17)),new CheckpointManifest.Metadata(0,2,""));
        }
        var options=settings(root).withTeacherStore(nnue.toString());
        var backend=new TrainingController.Backend(){
            @Override TrainingController.Handle create(TrainingSettings settings,boolean resume,TrainerConfig.DepthChange change) throws java.io.IOException {
                var c=settings.config(change);assertEquals(TrainingSource.HANDCRAFTED,c.source());
                var fixture=new TrainerConfig(c.checkpointRoot(),c.masterSeed(),c.selfPlay(),c.training(),c.validation(),1,c.depthChange(),
                        "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1",c.architecture(),c.brnLearningRate(),c.source(),c.supervision(),c.runSeeds(),c.teacherStore());
                return handle(TrainerService.fresh(fixture,new Brn2Trainer(.001)));
            }
        };
        var panel=edt(()->new TrainingPanel(options));var controller=edt(()->new TrainingController(options,backend,s->{},panel::showState));
        var frame=edt(()->{var w=new JFrame("Handcrafted BRN-2 configuration");w.setContentPane(panel);w.setSize(1100,1000);panel.bind(controller);w.setVisible(true);return w;});
        try {
            until(()->edt(()->named(panel,"brnTrainingSource",JComboBox.class).isEnabled()&&named(panel,"brn2Supervision",JComboBox.class).isEnabled()));
            edt(()->{
                named(panel,"trainingViews",JTabbedPane.class).setSelectedIndex(2);
                named(panel,"brn2Supervision",JComboBox.class).setSelectedItem(BrnSupervision.Mode.NNUE_BLENDED);
                named(panel,"brn2TeacherWeight",JSpinner.class).setValue(75.0);
                var card=named(panel,"brn2Configuration",JPanel.class);card.scrollRectToVisible(new java.awt.Rectangle(0,0,card.getWidth(),card.getHeight()));
            });
            edt(()->{
                Path image=Path.of("build/brn2-handcrafted/configuration.png");Files.createDirectories(image.getParent());
                var bitmap=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics=bitmap.createGraphics();frame.printAll(graphics);graphics.dispose();javax.imageio.ImageIO.write(bitmap,"png",image.toFile());return null;
            });
            edt(()->named(panel,"startTraining",JButton.class).doClick());
            until(()->edt(()->{controller.poll();return !controller.state().active();}));
            var result=edt(controller::state);assertEquals(TrainingController.Phase.STOPPED,result.phase(),result.message());
            assertEquals(1,result.snapshot().totals().completedGenerations());var evidence=result.snapshot().bootstrapValidation().orElseThrow().evidence();
            assertEquals(TrainingSource.Mode.HANDCRAFTED,evidence.generatorMode());assertEquals("",evidence.generatorId());assertFalse(evidence.teacherId().isBlank());
            until(()->edt(()->named(panel,"brn2SupervisionStatus",JLabel.class).getText().startsWith("Supervision locked")));
            assertFalse(edt(()->named(panel,"nnueTeacherStore",JTextField.class).isEnabled()));
            until(()->edt(()->!named(panel,"brnTrainingSource",JComboBox.class).isEnabled()));
        } finally {edt(controller::beginShutdown).run();edt(frame::dispose);}
    }
}
