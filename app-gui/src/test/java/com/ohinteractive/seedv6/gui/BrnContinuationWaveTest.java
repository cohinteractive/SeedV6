package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class BrnContinuationWaveTest {
    @TempDir Path temporary;
    Path library;
    List<BrnContinuationWave.Source> sources() throws Exception {
        library=temporary.resolve("library");Files.createDirectories(library);
        Path file=temporary.resolve("source.jsonl");var text=new StringBuilder();
        for(int i=0;i<1600;i++) {
            int white=8+i%40,black=8+(i/40)%40;if(white==black)continue;
            char[] squares=new char[64];Arrays.fill(squares,' ');squares[4]='K';squares[60]='k';
            squares[white]='N';squares[black]='p';var fen=new StringBuilder();
            for(int rank=7;rank>=0;rank--) {
                int empty=0;
                for(int f=0;f<8;f++) {
                    char piece=squares[rank*8+f];
                    if(piece==' ')empty++;else {if(empty>0)fen.append(empty);empty=0;fen.append(piece);}
                }
                if(empty>0)fen.append(empty);if(rank>0)fen.append('/');
            }
            text.append("{\"fen\":\"").append(fen).append(" w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":50}]}]}\n");
        }
        Files.writeString(file,text);
        var selection=new DataSources(1,List.of(DataSource.register("fixture",file,1)),false);
        var result=new ArrayList<BrnContinuationWave.Source>();
        for(var architecture:BrnResidualWave.ARCHITECTURES) {
            Path root=library.resolve(architecture.folderName()).resolve(UUID.randomUUID().toString());
            var corpus=new CorpusTrainingConfig(12,selection.identity()).forArchitecture(architecture.trainingArchitecture());
            var configured=BrnContinuationWave.settings(root,architecture,.003,corpus,71);
            var state=NetworkTrainingState.initialized(architecture.trainingArchitecture(),71,.003);
            update(state);update(state);update(state);
            try(var store=new CheckpointStore(root,architecture.trainingArchitecture())) {
                store.writeLineage(new TrainingLineage(UUID.randomUUID(),"Original fixture",architecture.trainingArchitecture(),
                        Instant.now(),LineageConfiguration.encode(configured),"Synthetic counter/ledger fixture; not research exposure"));
                selection.save(DataSources.directory(root));store.writeTrainingSource(configured.source());
                var checkpoint=store.initialize(state,new CheckpointManifest.Metadata(32,4,""));
                var ledger=new SourceLedger(root);ledger.initialize();
                ledger.reserve(32,"fixture",selection.identity(),List.of(new SourceLedger.Range(selection.sources().getFirst().identity(),0,10,8,2,0)),
                        "a".repeat(64),"b".repeat(64));ledger.complete(32);
                DataFiles.write(DataSources.directory(root).resolve("configuration.json"),corpus);
                result.add(new BrnContinuationWave.Source(architecture.name(),root.toString(),checkpoint.manifest().id(),
                        checkpoint.manifest().networkSha256(),checkpoint.manifest().trainingSha256(),32,71,12,
                        BrnContinuationWave.hash(Files.readAllBytes(DataSources.directory(root).resolve("configuration.json")))));
            }
        }
        return result;
    }
    static void update(NetworkTrainingState state) {
        long[][] boards={Board.startingPosition()};double[] targets={.25};
        if(state instanceof NetworkTrainingState.BrnPair2 p)p.trainer().trainBatch(boards,targets,1);
        else ((NetworkTrainingState.BrnNext)state).trainer().trainBatch(boards,targets,1);
    }
    @Test void exactStateCursorsGuiMetadataAndAdditionalSessionSurviveForkAndReuse() throws Exception {
        var input=sources();var entries=BrnContinuationWave.prepare(library,input);assertEquals(6,entries.size());
        var firstRanges=new HashMap<String,SourceLedger.Reservation>();
        for(var entry:entries) {
            Path root=Path.of(entry.root()),original=Path.of(entry.origin().source().root());
            var architecture=NetworkArchitecture.valueOf(entry.architecture());
            byte[] sourceBefore=CheckpointStore.readTrainingSnapshot(original,entry.origin().source().checkpoint()).encode();
            byte[] sourceLedgerBefore=Files.readAllBytes(DataSources.directory(original).resolve("cursors.json"));
            assertArrayEquals(sourceBefore,CheckpointStore.readTrainingSnapshot(root,entry.origin().bootstrap()).encode());
            assertArrayEquals(Files.readAllBytes(DataSources.directory(original).resolve("cursors.json")),
                    Files.readAllBytes(DataSources.directory(root).resolve("cursors.json")));
            var selected=TrainingLineages.read(new TrainingLineages.Entry(root,architecture,entry.candidate()));
            assertEquals(entry.origin().requestedRate(),selected.settings().recipe().learningRate());
            assertEquals(32,selected.settings().maximumGenerations());
            assertEquals("Start Training",new TrainingController.Backend().preview(selected.settings()));
            assertTrue(LineageSummary.read(selected.entry()).contains("Gen 32 (Starting checkpoint)"));
            assertFalse(LineageSummary.read(selected.entry()).contains("unfinished"));
            var expected=CheckpointStore.readTrainingSnapshot(original,entry.origin().source().checkpoint());
            var clone=CheckpointStore.readTrainingSnapshot(root,entry.origin().bootstrap());
            expected.setLearningRate(entry.recipe().learningRate());clone.setLearningRate(entry.recipe().learningRate());
            update(expected);update(clone);assertArrayEquals(expected.encode(),clone.encode());
            // One tiny GUI-service generation:12 positions,8 epochs; no original-store training.
            var tiny=new TrainingSettings(root,4,1,64,0,8,32,128,8,64,71,1024,1,architecture)
                    .withSource(selected.settings().source()).withCorpus("",selected.settings().corpusTraining())
                    .withValidationMethod(ValidationMethod.HELD_OUT).withLearningRate(entry.recipe().learningRate());
            TrainingLineages.save(selected,tiny);
            try(var service=TrainerService.resume(tiny.config(TrainerConfig.DepthChange.REQUIRE_SAME))
                    .withIncrementalTraining().withSession()) {
                service.start();assertTrue(service.awaitTermination(Duration.ofSeconds(40)));
                assertFalse(service.snapshot().failed(),service.snapshot().failureSummary());
                assertEquals(11,service.snapshot().optimizerStep());
            }
            var session=TrainingSession.read(root).orElseThrow();
            assertEquals(32,session.baseline());assertEquals(1,session.completed());assertEquals(TrainingSession.State.COMPLETED,session.state());
            var current=new SourceLedger(root);var next=current.generation(33).orElseThrow();
            assertEquals(10,next.ranges().getFirst().start());assertEquals(SourceLedger.Status.COMPLETED,next.status());
            var prior=firstRanges.putIfAbsent(entry.architecture(),next);
            if(prior!=null) { assertEquals(prior.ranges(),next.ranges());assertEquals(prior.trainingHash(),next.trainingHash());assertEquals(prior.validationHash(),next.validationHash()); }
            assertArrayEquals(sourceBefore,CheckpointStore.readTrainingSnapshot(original,entry.origin().source().checkpoint()).encode());
            assertArrayEquals(sourceLedgerBefore,Files.readAllBytes(DataSources.directory(original).resolve("cursors.json")));
        }
        var replay=BrnContinuationWave.prepare(library,input);
        for(var entry:replay) { assertEquals(33,entry.latestGeneration());assertEquals(1,entry.additionalGenerations()); }
    }
    @Test void foreignDestinationAndActiveSourceArePreserved() throws Exception {
        var input=sources();var source=input.getFirst();
        var architecture=NetworkArchitecture.BRN_GEN32_GATED;
        Path root=library.resolve(architecture.folderName()).resolve(BrnContinuationWave.id(source,.001).toString());
        Files.createDirectories(root);Path owner=root.resolve("owner.txt");Files.writeString(owner,"preserve");
        assertThrows(java.io.IOException.class,()->BrnContinuationWave.prepare(library,List.of(source)));
        assertEquals("preserve",Files.readString(owner));
        try(var paths=Files.list(root)){assertEquals(List.of(owner),paths.toList());}
        var other=input.get(1);Path original=Path.of(other.root());
        var lineage=TrainingLineage.read(original).orElseThrow();
        new TrainingSession(1,UUID.randomUUID(),lineage.id(),32,32,0,TrainingSession.State.ACTIVE).save(original);
        assertThrows(java.io.IOException.class,()->BrnContinuationWave.prepare(library,List.of(other)));
        assertFalse(Files.exists(library.resolve(NetworkArchitecture.BRN_GEN32_CUBIC.folderName()).resolve(BrnContinuationWave.id(other,.001).toString())));
    }
    @Test void mismatchedAcquisitionSettingsAndConcurrentWriterPreventCreation() throws Exception {
        var source=sources().getFirst();
        var changed=new BrnContinuationWave.Source(source.candidate(),source.root(),source.checkpoint(),source.networkSha256(),
                source.trainingSha256(),source.generation(),source.seed()+1,source.positionsPerGeneration(),source.corpusConfigurationSha256());
        assertThrows(java.io.IOException.class,()->BrnContinuationWave.prepare(library,List.of(changed)));
        try(var channel=java.nio.channels.FileChannel.open(Path.of(source.root()).resolve("store.lock"),StandardOpenOption.WRITE);
            var lock=channel.lock()) {
            assertThrows(java.io.IOException.class,()->BrnContinuationWave.prepare(library,List.of(source)));
        }
        assertFalse(Files.exists(library.resolve(NetworkArchitecture.BRN_GEN32_GATED.folderName()).resolve(BrnContinuationWave.id(source,.001).toString())));
    }
    @Test void nativeWindowLoadsEachContinuationWithoutStartingTraining() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),"Native Swing required");
        String ownerReport=System.getProperty("seedv6.continuationWaveGuiReport");
        List<BrnContinuationWave.Entry> entries;
        if(ownerReport==null) {var input=sources();entries=BrnContinuationWave.prepare(library,input);}
        else entries=DataFiles.read(Path.of(ownerReport),BrnContinuationWave.Report.class).entries();
        for(var entry:entries) {
            var architecture=NetworkArchitecture.valueOf(entry.architecture());
            var selected=TrainingLineages.read(new TrainingLineages.Entry(Path.of(entry.root()),architecture,entry.candidate()));
            var frame=edt(()->new ChessFrame(selected.settings(),new TrainingController.Backend(),ignored->{}));
            try {
                edt(()->{frame.setSize(1440,950);frame.setVisible(true);
                    BrnResidualWaveTest.named(frame,null,TrainingPanel.class).loadInitialLineage();return null;});
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
                while(!edt(()->BrnResidualWaveTest.named(frame,"trainingLineage",javax.swing.JComboBox.class).getSelectedItem()
                        instanceof TrainingLineages.Entry active&&active.root().equals(Path.of(entry.root()))
                        &&BrnResidualWaveTest.named(frame,"startTraining",javax.swing.JButton.class).isEnabled())) {
                    if(System.nanoTime()>deadline)fail("Continuation GUI did not become ready: "+entry.candidate());
                    Thread.sleep(50);
                }
                edt(()->{
                    BrnResidualWaveTest.named(frame,"workspaces",javax.swing.JTabbedPane.class).setSelectedIndex(1);
                    BrnResidualWaveTest.named(frame,"trainingViews",javax.swing.JTabbedPane.class).setSelectedIndex(1);
                    assertEquals(entry.recipe().learningRate(),((Number)BrnResidualWaveTest.named(frame,"recipeLearningRate",javax.swing.JSpinner.class).getValue()).doubleValue());
                    assertFalse(BrnResidualWaveTest.named(frame,"inheritLearningRate",javax.swing.JCheckBox.class).isSelected());
                    assertEquals(32,((Number)BrnResidualWaveTest.named(frame,"trainingGenerations",javax.swing.JSpinner.class).getValue()).longValue());
                    assertEquals(128,((Number)BrnResidualWaveTest.named(frame,"trainingMinibatch",javax.swing.JSpinner.class).getValue()).intValue());
                    assertEquals(8,((Number)BrnResidualWaveTest.named(frame,"trainingEpochs",javax.swing.JSpinner.class).getValue()).intValue());
                    assertEquals(ValidationMethod.HELD_OUT,BrnResidualWaveTest.named(frame,"trainingValidationMethod",javax.swing.JComboBox.class).getSelectedItem());
                    String summary=BrnResidualWaveTest.named(frame,"trainingLineageSummary",javax.swing.JTextArea.class).getText();
                    assertTrue(summary.contains(entry.candidate()));assertTrue(summary.contains("Gen 32 (Starting checkpoint)"));assertFalse(summary.contains("unfinished"));
                    frame.validate();Path output=Path.of("build/brn-successor-native");Files.createDirectories(output);
                    var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics();frame.paintAll(graphics);graphics.dispose();
                    javax.imageio.ImageIO.write(image,"png",output.resolve(entry.candidate()+(ownerReport==null?"-fixture.png":"-owner.png")).toFile());
                    return null;
                });
            } finally {
                edt(()->{frame.dispatchEvent(new java.awt.event.WindowEvent(frame,java.awt.event.WindowEvent.WINDOW_CLOSING));return null;});
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
                while(edt(frame::isDisplayable)){if(System.nanoTime()>deadline)fail("Window did not close");Thread.sleep(50);}
            }
            assertEquals(entry.latest(),CheckpointInspection.reference(Path.of(entry.root()),"latest-training"));
            assertTrue(TrainingSession.read(Path.of(entry.root())).isEmpty(),"Visual inspection must not start training");
        }
    }
    private static <T> T edt(java.util.concurrent.Callable<T> call) throws Exception {
        var result=new java.util.concurrent.atomic.AtomicReference<T>();
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        javax.swing.SwingUtilities.invokeAndWait(()->{try{result.set(call.call());}catch(Throwable t){failure.set(t);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());return result.get();
    }
}
