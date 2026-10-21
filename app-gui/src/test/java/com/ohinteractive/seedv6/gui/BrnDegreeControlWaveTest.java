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
class BrnDegreeControlWaveTest {
    @TempDir Path temporary;
    BrnContinuationWaveTest fixture;
    List<BrnDegreeControlWave.Arm> arms() throws Exception {
        fixture=new BrnContinuationWaveTest();fixture.temporary=temporary;
        var sources=fixture.sources();var cubic=sources.get(1);var bre=sources.get(2);
        // Tiny fixtures deliberately use their actual .003 and generation32 state.
        // Owner preparation remains separately bound to the selected Gen96 identities.
        return List.of(new BrnDegreeControlWave.Arm("W6-fixture-Q16",cubic,.003,"quadratic"),
                new BrnDegreeControlWave.Arm("W6-fixture-R16",cubic,.003,"exact"),
                new BrnDegreeControlWave.Arm("W6-fixture-BRE",bre,.003,"exact"));
    }
    @Test void projectedAndExactArmsContinueSameDataWithoutChangingSources() throws Exception {
        var arms=arms();var entries=BrnDegreeControlWave.prepare(fixture.library,arms);
        assertEquals(3,entries.size());SourceLedger.Reservation cubicRange=null;
        for(var entry:entries) {
            var arm=entry.origin().arm();Path original=Path.of(arm.source().root()),root=Path.of(entry.root());
            byte[] sourceBefore=CheckpointStore.readTrainingSnapshot(original,arm.source().checkpoint()).encode();
            byte[] ledgerBefore=Files.readAllBytes(DataSources.directory(original).resolve("cursors.json"));
            var expected=BrnDegreeControlWave.project(CheckpointStore.readTrainingSnapshot(original,arm.source().checkpoint()),arm);
            var actual=CheckpointStore.readTrainingSnapshot(root,entry.origin().bootstrap());
            assertArrayEquals(expected.encode(),actual.encode());
            assertArrayEquals(ledgerBefore,Files.readAllBytes(DataSources.directory(root).resolve("cursors.json")));
            BrnContinuationWaveTest.update(expected);BrnContinuationWaveTest.update(actual);assertArrayEquals(expected.encode(),actual.encode());
            var architecture=NetworkArchitecture.valueOf(entry.architecture());
            var selected=TrainingLineages.read(new TrainingLineages.Entry(root,architecture,entry.candidate()));
            assertEquals(32,selected.settings().maximumGenerations());
            assertEquals("Start Training",new TrainingController.Backend().preview(selected.settings()));
            assertTrue(LineageSummary.read(selected.entry()).contains("Gen 32 (Starting checkpoint)"));
            var tiny=new TrainingSettings(root,4,1,64,0,8,32,128,8,64,71,1024,1,architecture)
                    .withSource(selected.settings().source()).withCorpus("",selected.settings().corpusTraining())
                    .withValidationMethod(ValidationMethod.HELD_OUT).withLearningRate(arm.rate());
            TrainingLineages.save(selected,tiny);
            try(var service=TrainerService.resume(tiny.config(TrainerConfig.DepthChange.REQUIRE_SAME)).withIncrementalTraining().withSession()) {
                service.start();assertTrue(service.awaitTermination(Duration.ofSeconds(40)));
                assertFalse(service.snapshot().failed(),service.snapshot().failureSummary());assertEquals(11,service.snapshot().optimizerStep());
            }
            var session=TrainingSession.read(root).orElseThrow();assertEquals(32,session.baseline());
            assertEquals(1,session.completed());assertEquals(TrainingSession.State.COMPLETED,session.state());
            var range=new SourceLedger(root).generation(33).orElseThrow();assertEquals(5000,range.ranges().getFirst().start());
            if(architecture.trainingArchitecture().brnNext()) {
                if(cubicRange==null)cubicRange=range;
                else {assertEquals(cubicRange.ranges(),range.ranges());assertEquals(cubicRange.trainingHash(),range.trainingHash());assertEquals(cubicRange.validationHash(),range.validationHash());}
            }
            assertArrayEquals(sourceBefore,CheckpointStore.readTrainingSnapshot(original,arm.source().checkpoint()).encode());
            assertArrayEquals(ledgerBefore,Files.readAllBytes(DataSources.directory(original).resolve("cursors.json")));
        }
        for(var entry:BrnDegreeControlWave.prepare(fixture.library,arms)) {
            assertEquals(33,entry.latestGeneration());assertEquals(1,entry.additionalGenerations());
        }
    }
    @Test void sourceRateMissingSeekAndForeignDestinationFailWithoutReplacement() throws Exception {
        var arm=arms().getFirst();Path original=Path.of(arm.source().root());
        var wrongRate=new BrnDegreeControlWave.Arm(arm.name(),arm.source(),.001,arm.projection());
        assertThrows(java.io.IOException.class,()->BrnDegreeControlWave.prepare(fixture.library,List.of(wrongRate)));
        try(var channel=java.nio.channels.FileChannel.open(original.resolve("store.lock"),StandardOpenOption.WRITE);var lock=channel.lock()) {
            assertThrows(java.io.IOException.class,()->BrnDegreeControlWave.prepare(fixture.library,List.of(arm)));
        }
        var selection=DataSources.read(DataSources.directory(original));
        Path index=DataSources.directory(original).resolve("seek").resolve(selection.sources().getFirst().identity()+".json");
        byte[] bytes=Files.readAllBytes(index);Files.delete(index);
        assertThrows(java.io.IOException.class,()->BrnDegreeControlWave.prepare(fixture.library,List.of(arm)));
        Files.write(index,bytes);
        Path destination=fixture.library.resolve(NetworkArchitecture.BRN_GEN32_QUADRATIC.folderName()).resolve(BrnDegreeControlWave.id(arm).toString());
        assertFalse(Files.exists(destination));Files.createDirectories(destination);
        Path marker=destination.resolve("owner.txt");Files.writeString(marker,"preserve");
        assertThrows(java.io.IOException.class,()->BrnDegreeControlWave.prepare(fixture.library,List.of(arm)));
        try(var paths=Files.list(destination)){assertEquals(List.of(marker),paths.toList());}
    }
    @Test void activeSourcePreventsAnyDestinationCreation() throws Exception {
        var arm=arms().getFirst();Path original=Path.of(arm.source().root());
        var lineage=TrainingLineage.read(original).orElseThrow();
        var session=new TrainingSession(1,UUID.randomUUID(),lineage.id(),32,32,0,TrainingSession.State.ACTIVE);session.save(original);
        assertThrows(java.io.IOException.class,()->BrnDegreeControlWave.prepare(fixture.library,List.of(arm)));
        assertEquals(session,TrainingSession.read(original).orElseThrow());
        assertFalse(Files.exists(fixture.library.resolve(NetworkArchitecture.BRN_GEN32_QUADRATIC.folderName()).resolve(BrnDegreeControlWave.id(arm).toString())));
    }
}
