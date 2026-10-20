package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class IncrementalTrainerServiceTest {
    @TempDir Path temporary;
    Path data(int count) throws Exception {
        Path file = temporary.resolve("positions-" + count + ".jsonl");
        try (var out = Files.newBufferedWriter(file)) { for (int i = 0; i < count; i++) out.write(SourceReadersTest.line(i % 500)); }
        return file;
    }
    TrainerConfig config(Path root, Path data, int count) throws Exception {
        var base = Brn2TrainerServiceTest.config(root, 1);
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN_PAIR2)) {
            new DataSources(1, List.of(DataSource.register("test", data, 1)), false).save(DataSources.directory(root));
        }
        return new TrainerConfig(root, 71, base.selfPlay(), new TrainerConfig.Training(3, 128, true), base.validation(),
                1, base.depthChange(), base.startingFen(), TrainingArchitecture.BRN_PAIR2, .001,
                TrainingSource.dataSources(DataSources.directory(root))).withCorpusTraining(new CorpusTrainingConfig(count))
                .withValidationMethod(ValidationMethod.HELD_OUT);
    }
    static NetworkTrainingState initial() { return NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2, 71, .001); }
    static TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(90)));
        assertTrue(service.failure().isEmpty(), () -> service.failure().toString()); return service.snapshot();
    }
    static byte[] state(Path root, TrainerSnapshot end) throws Exception {
        return Files.readAllBytes(root.resolve("checkpoints").resolve(end.latestTrainingId()).resolve(CheckpointManifest.TRAINING_FILE));
    }
    @Test void actualSourceUpdatesBeforeCompletionAndStopResumePreservesParentOrVerifiedCursor() throws Exception {
        int count = 4101; Path data = data(6000);
        var referenceConfig = config(temporary.resolve("reference"), data, count);
        TrainerSnapshot reference;
        try (var service = TrainerService.fresh(referenceConfig, initial(), NnueCorpusTrainingTest.forbidden(), s -> {}).withIncrementalTraining()) {
            reference = finish(service);
        }
        assertEquals(3L * count, reference.generationSamplesTrained());
        assertEquals(3 * 33, reference.generationOptimizerUpdates());
        for (boolean verified : new boolean[] {false, true}) {
            var cfg = config(temporary.resolve("split-" + verified), data, count);
            var owner = new AtomicReference<TrainerService>(); var stopped = new AtomicBoolean();
            try (var service = TrainerService.fresh(cfg, initial(), NnueCorpusTrainingTest.forbidden(), snapshot -> {
                var acquisition = snapshot.run().map(TrainerSnapshot.RunDetails::acquisition).orElse(null);
                if (acquisition != null && snapshot.generationOptimizerUpdates() > 0
                        && (verified ? acquisition.epoch() == 2 : !acquisition.verified()) && stopped.compareAndSet(false, true)) {
                    if (!verified) assertTrue(acquisition.uniquePositions() < count, "Updates must precede full acquisition");
                    owner.get().stop();
                }
            }).withIncrementalTraining()) { owner.set(service); finish(service); }
            assertTrue(stopped.get());
            var partial = PartialGeneration.inspect(cfg.checkpointRoot()).orElseThrow();
            assertEquals(GenerationAttempt.Format.INCREMENTAL, partial.attempt().format());
            assertEquals(verified ? 33 : 0, partial.training().updates());
            assertTrue(partial.candidate().isEmpty());
            TrainerSnapshot resumed;
            try (var service = TrainerService.resume(cfg, NnueCorpusTrainingTest.forbidden(), s -> {}).withIncrementalTraining()) { resumed = finish(service); }
            assertArrayEquals(state(referenceConfig.checkpointRoot(), reference), state(cfg.checkpointRoot(), resumed));
            var reservation = new SourceLedger(cfg.checkpointRoot()).generation(1).orElseThrow();
            assertEquals(SourceLedger.Status.COMPLETED, reservation.status());
            assertEquals(count, reservation.ranges().getFirst().training());
            assertEquals((count + 3) / 4, reservation.ranges().getFirst().validation());
            assertNotEquals(reservation.trainingHash(), reservation.validationHash());
        }
    }
    @Test void sourceFailureAfterUpdatesCannotPublishOrReserveAnIncompleteDataset() throws Exception {
        var cfg = config(temporary.resolve("failure"), data(2048), 4101);
        var updated = new AtomicBoolean();
        try (var service = TrainerService.fresh(cfg, initial(), NnueCorpusTrainingTest.forbidden(), s -> {
            if (s.generationOptimizerUpdates() > 0) updated.set(true);
        }).withIncrementalTraining()) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(90))); assertTrue(service.failure().isPresent());
            assertEquals(TrainerSnapshot.State.FAILED, service.snapshot().state()); assertTrue(service.snapshot().candidateId().isEmpty());
        }
        assertTrue(updated.get()); assertTrue(new SourceLedger(cfg.checkpointRoot()).state().reservations().isEmpty());
        try (var store = new CheckpointStore(cfg.checkpointRoot(), TrainingArchitecture.BRN_PAIR2)) {
            assertEquals(0, store.recover().latestTraining().orElseThrow().manifest().optimizerStep());
        }
    }
    @Test void inheritedSeekFailureAfterProvisionalUpdatesResumesExactParentAndSession() throws Exception {
        Path first=data(9000), second=temporary.resolve("second.jsonl");
        Files.copy(first,second);Files.writeString(second,SourceReadersTest.line(77),StandardOpenOption.APPEND);
        var selection=new DataSources(1,List.of(DataSource.register("first",first,1),DataSource.register("second",second,1)),false);
        Path donor=temporary.resolve("donor-seek");
        for(var item:selection.sources())try(var reader=SourceReaders.open(item,donor,0)) {
            for(int i=0;i<5000;i++)assertNotNull(reader.next());
        }
        byte[][] finalStates=new byte[2][];
        for(int run=0;run<2;run++) {
            var cfg=config(temporary.resolve("seek-resume-"+run),first,2048);
            Path root=cfg.checkpointRoot();selection.save(DataSources.directory(root));
            try(var store=new CheckpointStore(root,TrainingArchitecture.BRN_PAIR2)) {
                store.writeLineage(new TrainingLineage(UUID.randomUUID(),"Seek fixture",TrainingArchitecture.BRN_PAIR2,
                        java.time.Instant.now(),"","Synthetic generation counter"));
                store.initialize(initial(),new CheckpointManifest.Metadata(32,1,""));store.writeTrainingSource(cfg.source());
                var ledger=new SourceLedger(root);ledger.initialize();
                ledger.reserve(32,"fixture",selection.identity(),selection.sources().stream()
                        .map(s->new SourceLedger.Range(s.identity(),0,5000,4000,1000,0)).toList(),"a".repeat(64),"b".repeat(64));
                ledger.complete(32);
            }
            Path seek=DataSources.directory(root).resolve("seek");
            if(run==0) {
                Files.createDirectories(seek);
                Files.copy(donor.resolve(selection.sources().getFirst().identity()+".json"),seek.resolve(selection.sources().getFirst().identity()+".json"));
                String parent=CheckpointInspection.reference(root,"latest-training");
                try(var service=TrainerService.resume(cfg,NnueCorpusTrainingTest.forbidden(),s->{}).withIncrementalTraining().withSession()) {
                    service.start();assertTrue(service.awaitTermination(Duration.ofSeconds(30)));
                    assertTrue(service.snapshot().failed());assertTrue(service.snapshot().generationOptimizerUpdates()>0);
                    assertTrue(service.snapshot().failureSummary().contains("does not cover position 5000"));
                }
                assertEquals(parent,CheckpointInspection.reference(root,"latest-training"));
                assertTrue(PartialGeneration.inspect(root).isEmpty());assertTrue(new SourceLedger(root).generation(33).isEmpty());
                var session=TrainingSession.read(root).orElseThrow();assertEquals(TrainingSession.State.ACTIVE,session.state());
                assertEquals(32,session.baseline());assertEquals(33,session.finalGeneration());
                var attempt=GenerationAttempt.inspect(root).orElseThrow();assertEquals(33,attempt.generation());
                assertEquals(GenerationAttempt.Format.INCREMENTAL,attempt.format());
            }
            ContinuationSeek.inherit(selection,new SourceLedger(root).state().next(),donor,seek);
            UUID sessionId=TrainingSession.read(root).map(TrainingSession::id).orElse(null);
            try(var service=TrainerService.resume(cfg,NnueCorpusTrainingTest.forbidden(),s->{}).withIncrementalTraining().withSession()) {
                var end=finish(service);assertEquals(48,end.generationOptimizerUpdates());
                finalStates[run]=state(root,end);
            }
            var end=TrainingSession.read(root).orElseThrow();assertEquals(TrainingSession.State.COMPLETED,end.state());
            assertEquals(1,end.completed());assertEquals(33,end.finalGeneration());if(sessionId!=null)assertEquals(sessionId,end.id());
        }
        assertArrayEquals(finalStates[1],finalStates[0],"Repair/resume must equal uninterrupted Adam/weights byte for byte");
    }
    @Test void zeroUpdateHistoricalReservationAdoptsIncrementalOrderingWithoutAbandoningRanges() throws Exception {
        var cfg = config(temporary.resolve("upgrade"), data(500), 257);
        try (var store = new CheckpointStore(cfg.checkpointRoot(), TrainingArchitecture.BRN_PAIR2)) {
            var parent = store.initialize(initial(), new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            store.writeTrainingSource(cfg.source());
            try (var corpus = new SequentialTraining(cfg, cfg.source(), CorpusPreparation.NONE)) { cfg = cfg.withCorpusTraining(corpus.config()); }
            store.writeGenerationAttempt(GenerationAttempt.create(parent, parent, 1, cfg, cfg.source()));
        }
        try (var corpus = new SequentialTraining(cfg, cfg.source(), CorpusPreparation.NONE)) { corpus.batch(1); }
        var ranges = new SourceLedger(cfg.checkpointRoot()).generation(1).orElseThrow().ranges();
        try (var service = TrainerService.resume(cfg, NnueCorpusTrainingTest.forbidden(), s -> {}).withIncrementalTraining()) {
            assertEquals(9, finish(service).generationOptimizerUpdates());
        }
        assertEquals(ranges, new SourceLedger(cfg.checkpointRoot()).generation(1).orElseThrow().ranges());
    }
    @Test void generatedGuiPathPersistsAssignedHoldoutAndCompletesValidation() throws Exception {
        var base = Brn2TrainerServiceTest.config(temporary.resolve("generated"), 1);
        var s = base.selfPlay();
        var cfg = base.withGenerationProtocol(new TrainerConfig.SelfPlay(s.depth(), s.threads(), 8, s.minimumOpeningPlies(),
                s.maximumOpeningPlies(), s.maximumSamplesPerGame(), s.maximumPlies(), s.scoreMapping()))
                .withValidationMethod(ValidationMethod.HELD_OUT);
        try (var service = TrainerService.fresh(cfg, NetworkTrainingState.initialized(TrainingArchitecture.BRN2, 71, .001),
                new TrainerService.Operations(), snapshot -> {}).withIncrementalTraining()) {
            var end = finish(service);
            assertEquals(1, end.totals().completedGenerations()); assertTrue(end.generationOptimizerUpdates() > 0);
            assertTrue(end.bootstrapValidation().isPresent());
        }
    }
    @Test void stopAfterGenerationAllowsAllEpochsAndValidationButAdmitsNoNextDataset() throws Exception {
        var single = config(temporary.resolve("boundary"), data(5000), 257);
        var cfg = new TrainerConfig(single.checkpointRoot(), single.masterSeed(), single.selfPlay(), single.training(), single.validation(),
                2, single.depthChange(), single.startingFen(), single.architecture(), single.brnLearningRate(), single.source())
                .withCorpusTraining(single.corpusTraining()).withValidationMethod(ValidationMethod.HELD_OUT);
        var owner = new AtomicReference<TrainerService>(); var requested = new AtomicBoolean();
        try (var service = TrainerService.fresh(cfg, initial(), NnueCorpusTrainingTest.forbidden(), s -> {
            if (s.generationOptimizerUpdates() > 0 && requested.compareAndSet(false, true)) assertEquals(1, owner.get().stopAfterGeneration());
        }).withIncrementalTraining()) {
            owner.set(service); var end = finish(service);
            assertTrue(requested.get()); assertEquals(1, end.totals().completedGenerations()); assertEquals(771, end.generationSamplesTrained());
            assertEquals(9, end.generationOptimizerUpdates());
        }
        assertEquals(1, new SourceLedger(cfg.checkpointRoot()).state().reservations().size());
    }
}
