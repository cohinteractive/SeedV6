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
        for(int i=0;i<12000;i++) {
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
                ledger.reserve(32,"fixture",selection.identity(),List.of(new SourceLedger.Range(selection.sources().getFirst().identity(),0,5000,4000,1000,0)),
                        "a".repeat(64),"b".repeat(64));ledger.complete(32);
                try(var reader=SourceReaders.open(selection.sources().getFirst(),DataSources.directory(root).resolve("seek"),0)) {
                    for(int i=0;i<5000;i++) assertNotNull(reader.next());
                }
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
            assertEquals(5000,next.ranges().getFirst().start());assertEquals(SourceLedger.Status.COMPLETED,next.status());
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
    @Test void missingSourceSeekPreventsCreationAndExplicitRepairPreservesFailedSession() throws Exception {
        var input=sources();var source=input.getFirst();Path original=Path.of(source.root());
        var selection=DataSources.read(DataSources.directory(original));
        Path sourceIndex=DataSources.directory(original).resolve("seek").resolve(selection.sources().getFirst().identity()+".json");
        byte[] index=Files.readAllBytes(sourceIndex);Files.delete(sourceIndex);
        assertThrows(java.io.IOException.class,()->BrnContinuationWave.prepare(library,List.of(source)));
        Path root=library.resolve(NetworkArchitecture.BRN_GEN32_GATED.folderName()).resolve(BrnContinuationWave.id(source,.001).toString());
        assertFalse(Files.exists(root));Files.write(sourceIndex,index);
        var entry=BrnContinuationWave.prepare(library,List.of(source)).getFirst();
        Path destinationIndex=DataSources.directory(root).resolve("seek").resolve(sourceIndex.getFileName());Files.delete(destinationIndex);
        var session=new TrainingSession(1,UUID.randomUUID(),entry.lineageId(),32,32,0,TrainingSession.State.ACTIVE);session.save(root);
        var selected=TrainingLineages.read(new TrainingLineages.Entry(root,NetworkArchitecture.BRN_GEN32_GATED,entry.candidate()));
        var attempt=GenerationAttempt.create(entry.latest(),entry.latest(),33,selected.settings().config(TrainerConfig.DepthChange.REQUIRE_SAME),selected.settings().source()).incremental();
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN_GEN32_GATED)){store.writeGenerationAttempt(attempt);}
        byte[] ledger=Files.readAllBytes(DataSources.directory(root).resolve("cursors.json"));
        try(var channel=java.nio.channels.FileChannel.open(root.resolve("store.lock"),StandardOpenOption.WRITE);var lock=channel.lock()) {
            assertThrows(java.io.IOException.class,()->BrnContinuationWave.repairSeek(entry));assertFalse(Files.exists(destinationIndex));
        }
        assertEquals(1,BrnContinuationWave.repairSeek(entry).size());assertTrue(BrnContinuationWave.repairSeek(entry).isEmpty());
        assertArrayEquals(index,Files.readAllBytes(destinationIndex));assertEquals(session,TrainingSession.read(root).orElseThrow());
        assertEquals(attempt,GenerationAttempt.inspect(root).orElseThrow());assertEquals(entry.latest(),CheckpointInspection.reference(root,"latest-training"));
        assertArrayEquals(ledger,Files.readAllBytes(DataSources.directory(root).resolve("cursors.json")));
    }
    @Test void nativeWindowLoadsEachContinuationWithoutStartingTraining() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),"Native Swing required");
        String ownerReport=System.getProperty("seedv6.continuationWaveGuiReport");
        String compactReport=System.getProperty("seedv6.compactWaveGuiReport");
        String degreeReport=System.getProperty("seedv6.degreeControlWaveGuiReport");
        List<BrnContinuationWave.Entry> entries;
        if(degreeReport!=null) {
            assertNull(ownerReport,"Choose one owner wave for native verification");
            assertNull(compactReport,"Choose one owner wave for native verification");
            entries=DataFiles.read(Path.of(degreeReport),BrnDegreeControlWave.Report.class).entries().stream()
                    .map(e->new BrnContinuationWave.Entry(e.candidate(),e.architecture(),e.root(),e.lineageId(),null,
                            e.latest(),e.latestGeneration(),e.recipe(),e.additionalGenerations(),e.sourceOffsets(),e.partition())).toList();
        } else if(compactReport!=null) {
            assertNull(ownerReport,"Choose one owner wave for native verification");
            // Shared visual assertions need only the prepared GUI entry fields,
            // not either wave's distinct immutable origin record.
            entries=DataFiles.read(Path.of(compactReport),BrnCompactContinuationWave.Report.class).entries().stream()
                    .map(e->new BrnContinuationWave.Entry(e.candidate(),e.architecture(),e.root(),e.lineageId(),null,
                            e.latest(),e.latestGeneration(),e.recipe(),e.additionalGenerations(),e.sourceOffsets(),e.partition())).toList();
        }
        else if(ownerReport==null) {var input=sources();entries=BrnContinuationWave.prepare(library,input);}
        else entries=DataFiles.read(Path.of(ownerReport),BrnContinuationWave.Report.class).entries();
        for(var entry:entries) {
            var architecture=NetworkArchitecture.valueOf(entry.architecture());
            var selected=TrainingLineages.read(new TrainingLineages.Entry(Path.of(entry.root()),architecture,entry.candidate()));
            var savedSession=TrainingSession.read(Path.of(entry.root()));
            var savedAttempt=GenerationAttempt.inspect(Path.of(entry.root()));
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
                    assertEquals(selected.settings().corpusTraining().positionsPerGeneration(),
                            ((Number)BrnResidualWaveTest.named(frame,"brnCorpusPositions",javax.swing.JSpinner.class).getValue()).intValue());
                    assertEquals(selected.settings().seed(),
                            Long.parseLong(BrnResidualWaveTest.named(frame,"trainingSeed",javax.swing.JTextField.class).getText()));
                    assertEquals(1,((Number)BrnResidualWaveTest.named(frame,"trainingThreads",javax.swing.JSpinner.class).getValue()).intValue());
                    assertEquals(ValidationMethod.HELD_OUT,BrnResidualWaveTest.named(frame,"trainingValidationMethod",javax.swing.JComboBox.class).getSelectedItem());
                    String summary=BrnResidualWaveTest.named(frame,"trainingLineageSummary",javax.swing.JTextArea.class).getText();
                    assertTrue(summary.contains(entry.candidate()));assertTrue(summary.contains("Gen "+entry.latestGeneration()+" (Starting checkpoint)"));
                    assertEquals(savedAttempt.isPresent()?"Resume Generation "+(entry.latestGeneration()+1):"Start Training",
                            BrnResidualWaveTest.named(frame,"startTraining",javax.swing.JButton.class).getText());
                    frame.validate();Path output=Path.of("build/brn-successor-native");Files.createDirectories(output);
                    var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics();frame.paintAll(graphics);graphics.dispose();
                    javax.imageio.ImageIO.write(image,"png",output.resolve(entry.candidate()+(ownerReport==null&&compactReport==null&&degreeReport==null?"-fixture.png":"-owner.png")).toFile());
                    if(ownerReport!=null||compactReport!=null||degreeReport!=null) {
                        BrnResidualWaveTest.named(frame,"trainingViews",javax.swing.JTabbedPane.class).setSelectedIndex(2);
                        frame.validate();graphics=image.createGraphics();frame.paintAll(graphics);graphics.dispose();
                        javax.imageio.ImageIO.write(image,"png",output.resolve(entry.candidate()+"-owner-recipe.png").toFile());
                    }
                    return null;
                });
            } finally {
                edt(()->{frame.dispatchEvent(new java.awt.event.WindowEvent(frame,java.awt.event.WindowEvent.WINDOW_CLOSING));return null;});
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
                while(edt(frame::isDisplayable)){if(System.nanoTime()>deadline)fail("Window did not close");Thread.sleep(50);}
            }
            assertEquals(entry.latest(),CheckpointInspection.reference(Path.of(entry.root()),"latest-training"));
            assertEquals(savedSession,TrainingSession.read(Path.of(entry.root())),"Visual inspection must not change session");
            assertEquals(savedAttempt,GenerationAttempt.inspect(Path.of(entry.root())),"Visual inspection must not change attempt");
        }
    }
    private static <T> T edt(java.util.concurrent.Callable<T> call) throws Exception {
        var result=new java.util.concurrent.atomic.AtomicReference<T>();
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        javax.swing.SwingUtilities.invokeAndWait(()->{try{result.set(call.call());}catch(Throwable t){failure.set(t);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());return result.get();
    }
}
