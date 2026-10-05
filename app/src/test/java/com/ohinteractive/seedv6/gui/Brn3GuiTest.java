package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;

class Brn3GuiTest {
    @TempDir Path temporary;
    @Test @Timeout(90) void normalGuiBackendCreatesTrainsAndReopensBrn3Lineage() throws Exception {
        var entry=TrainingLineages.create(temporary.resolve("lineages"),NetworkArchitecture.BRN3,"BRN-3 smoke");
        Path root=entry.root(),data=temporary.resolve("source.jsonl");var text=new StringBuilder();
        for(int i=0;i<40;i++)text.append(com.ohinteractive.seedv6.training.data.SourceReadersTest.line(i));Files.writeString(data,text);
        try(var store=new com.ohinteractive.seedv6.training.checkpoint.CheckpointStore(root,entry.architecture().trainingArchitecture())) {
            new com.ohinteractive.seedv6.training.data.DataSources(1,java.util.List.of(
                    com.ohinteractive.seedv6.training.data.DataSource.register("GUI test",data,1)),false)
                    .save(com.ohinteractive.seedv6.training.data.DataSources.directory(root));
        }
        var settings=new TrainingSettings(root,1,1,4,0,0,1,4,2,2,71,8,1,NetworkArchitecture.BRN3)
                .withSource(TrainingSource.dataSources(com.ohinteractive.seedv6.training.data.DataSources.directory(root)))
                .withCorpus("",new CorpusTrainingConfig(8)).withValidationMethod(ValidationMethod.HELD_OUT);
        var backend=new TrainingController.Backend();
        var handle=backend.create(settings,false,TrainerConfig.DepthChange.REQUIRE_SAME);
        try {
            handle.start();long deadline=System.nanoTime()+60_000_000_000L;
            while(!handle.terminated()&&System.nanoTime()<deadline)Thread.sleep(20);
            assertTrue(handle.terminated());assertEquals(TrainerSnapshot.State.STOPPED,handle.snapshot().state());
            assertEquals(4,handle.snapshot().optimizerStep());assertEquals(1,handle.snapshot().totals().completedGenerations());
            assertEquals(0,handle.snapshot().totals().selfPlayGames());
        } finally {handle.close();}
        assertTrue(backend.inspect(settings).resume());
        var resumed=backend.create(settings,true,TrainerConfig.DepthChange.REQUIRE_SAME);
        try {
            resumed.start();long deadline=System.nanoTime()+60_000_000_000L;
            while(!resumed.terminated()&&System.nanoTime()<deadline)Thread.sleep(20);
            assertTrue(resumed.terminated());assertEquals(TrainerSnapshot.State.STOPPED,resumed.snapshot().state());
            assertEquals(8,resumed.snapshot().optimizerStep());
        } finally {resumed.close();}
    }
    @Test void defaultsSourceControlsLifecycleAndRenderedNetworkTab() throws Exception {
        var settings=TrainingSettings.defaults(temporary,NetworkArchitecture.BRN3);
        var config=settings.config(TrainerConfig.DepthChange.REQUIRE_SAME);
        assertEquals(128,config.training().minibatchSize());assertEquals(8,config.training().epochs());
        assertEquals(.003,config.brnLearningRate());assertTrue(config.source().corpus());
        assertEquals(131072,config.corpusTraining().positionsPerGeneration());
        assertEquals(ValidationMethod.HELD_OUT,settings.selectedValidation());
        assertEquals(settings,LineageConfiguration.decode(LineageConfiguration.encode(settings),temporary,NetworkArchitecture.BRN3));
        var panel=edt(()->{SeedTheme.initialize();return new TrainingPanel(settings);});
        edt(()->{
            var modes=named(panel,"brnTrainingSource",JComboBox.class);
            assertEquals(1,modes.getItemCount());assertEquals(TrainingSource.Mode.TRAINING_DATA,modes.getSelectedItem());
            var batch=named(panel,"trainingMinibatch",JSpinner.class);var epochs=named(panel,"trainingEpochs",JSpinner.class);
            for(var phase:TrainingController.Phase.values()) {
                boolean active=phase==TrainingController.Phase.STARTING||phase==TrainingController.Phase.CONFIRM_DEPTH
                        ||phase==TrainingController.Phase.RUNNING||phase==TrainingController.Phase.STOPPING;
                boolean editable=!active&&phase!=TrainingController.Phase.CLOSING;
                panel.showState(new TrainingController.ViewState(settings,phase,null,"",active,editable,false,4,""));
                assertEquals(editable,batch.isEnabled());assertEquals(editable,epochs.isEnabled());
            }
            panel.showState(new TrainingController.ViewState(settings,TrainingController.Phase.STOPPED,null,"",false,true,false,4,""));
            var tabs=named(panel,"trainingViews",JTabbedPane.class);tabs.setSelectedIndex(tabs.indexOfTab("Recipe & lineage"));
            panel.setSize(1000,850);layout(panel);layout(panel);
            var viewport=((JScrollPane)tabs.getSelectedComponent()).getViewport();
            var bounds=SwingUtilities.convertRectangle(batch.getParent(),batch.getBounds(),viewport);
            assertTrue(bounds.width>75&&bounds.x>=0&&bounds.x+bounds.width<=viewport.getWidth(),bounds.toString());
            var image=new BufferedImage(1000,850,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);g.dispose();
            try {Path out=Path.of("build/research/brn-programme/brn3-gui.png");Files.createDirectories(out.getParent());ImageIO.write(image,"png",out.toFile());}
            catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        });
    }
    static void layout(Container parent){parent.doLayout();for(var child:parent.getComponents())if(child instanceof Container c)layout(c);}
}
