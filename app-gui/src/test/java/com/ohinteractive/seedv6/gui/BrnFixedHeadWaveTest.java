package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class BrnFixedHeadWaveTest {
    @TempDir Path temporary;
    @Test void explicitNewHeadOptimizersAndExactBreControlContinueInheritedData() throws Exception {
        var fixture=new BrnContinuationWaveTest();fixture.temporary=temporary;
        var sources=fixture.sources(List.of(NetworkArchitecture.BRN_GEN32_QUADRATIC_R4,NetworkArchitecture.BRN_PAIR2));
        var arms=List.of(new BrnDegreeControlWave.Arm("W8-fixture-Q",sources.get(0),.003,"fixed-quadratic-heads"),
                new BrnDegreeControlWave.Arm("W8-fixture-C",sources.get(0),.003,"fixed-cubic-heads"),
                new BrnDegreeControlWave.Arm("W8-fixture-BRE",sources.get(1),.003,"exact"));
        var entries=BrnDegreeControlWave.prepare(fixture.library,arms);SourceLedger.Reservation firstRange=null;
        for(var entry:entries) {
            var arm=entry.origin().arm();Path root=Path.of(entry.root()),original=Path.of(arm.source().root());
            var source=CheckpointStore.readTrainingSnapshot(original,arm.source().checkpoint());byte[] preserved=source.encode();
            byte[] ledger=Files.readAllBytes(DataSources.directory(original).resolve("cursors.json"));
            var initial=CheckpointStore.readTrainingSnapshot(root,entry.origin().bootstrap());
            assertEquals(source.step(),entry.origin().inheritedStep());
            assertEquals(arm.projection().equals("exact")?source.step():0,initial.step());
            assertArrayEquals(BrnDegreeControlWave.project(source,arm).encode(),initial.encode());
            assertArrayEquals(ledger,Files.readAllBytes(DataSources.directory(root).resolve("cursors.json")));
            var architecture=NetworkArchitecture.valueOf(entry.architecture());
            var selected=TrainingLineages.read(new TrainingLineages.Entry(root,architecture,entry.candidate()));
            assertEquals(8,selected.settings().maximumGenerations());
            assertEquals("Start Training",new TrainingController.Backend().preview(selected.settings()));
            assertTrue(LineageSummary.read(selected.entry()).contains("Gen 32 (Starting checkpoint)"));
            var tiny=new TrainingSettings(root,4,1,64,0,8,32,128,8,64,71,1024,1,architecture)
                    .withSource(selected.settings().source()).withCorpus("",selected.settings().corpusTraining())
                    .withValidationMethod(ValidationMethod.HELD_OUT).withLearningRate(.003);
            TrainingLineages.save(selected,tiny);
            try(var service=TrainerService.resume(tiny.config(TrainerConfig.DepthChange.REQUIRE_SAME)).withIncrementalTraining().withSession()) {
                service.start();assertTrue(service.awaitTermination(Duration.ofSeconds(40)));
                assertFalse(service.snapshot().failed(),service.snapshot().failureSummary());
                assertEquals(initial.step()+8,service.snapshot().optimizerStep());
            }
            var latest=CheckpointInspection.reference(root,"latest-training");
            var trained=CheckpointStore.readTrainingSnapshot(root,latest);
            if(initial instanceof NetworkTrainingState.BrnNext before) {
                var after=((NetworkTrainingState.BrnNext)trained).trainer().snapshot();
                for(int i=0;i<before.trainer().architecture().trainableStart();i++)
                    assertEquals(before.trainer().snapshot().weight(i),after.weight(i),"frozen coefficient "+i);
                var range=new SourceLedger(root).generation(33).orElseThrow();
                if(firstRange==null)firstRange=range;
                else {assertEquals(firstRange.ranges(),range.ranges());assertEquals(firstRange.trainingHash(),range.trainingHash());assertEquals(firstRange.validationHash(),range.validationHash());}
            }
            assertEquals(TrainingSession.State.COMPLETED,TrainingSession.read(root).orElseThrow().state());
            assertArrayEquals(preserved,CheckpointStore.readTrainingSnapshot(original,arm.source().checkpoint()).encode());
            assertArrayEquals(ledger,Files.readAllBytes(DataSources.directory(original).resolve("cursors.json")));
        }
        for(var entry:BrnDegreeControlWave.prepare(fixture.library,arms))assertEquals(33,entry.latestGeneration());
    }
    @Test void projectionRequiresExplicitWaveAndTrainedQuadraticRankFour() throws Exception {
        var fixture=new BrnContinuationWaveTest();fixture.temporary=temporary;
        var source=fixture.sources(List.of(NetworkArchitecture.BRN_GEN32_CUBIC_R4)).getFirst();
        assertThrows(IllegalArgumentException.class,()->new BrnDegreeControlWave.Arm("W7-wrong",source,.003,"fixed-cubic-heads"));
        assertThrows(IllegalArgumentException.class,()->new BrnDegreeControlWave.Arm("W8-wrong",source,.003,"quadratic"));
        var wrong=new BrnDegreeControlWave.Arm("W8-wrong",source,.003,"fixed-cubic-heads");
        assertThrows(java.io.IOException.class,()->BrnDegreeControlWave.prepare(fixture.library,List.of(wrong)));
        assertFalse(Files.exists(fixture.library.resolve(NetworkArchitecture.BRN_GEN32_FIXED_CUBIC_R4.folderName())));
    }
}
