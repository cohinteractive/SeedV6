package com.ohinteractive.seedv6.training.service;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic frozen fixtures require neither generation nor an available teacher. */
@Timeout(120) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrozenReplayTest {
    @TempDir static Path temp;
    Path source; FrozenReplay replay;
    TrainerConfig controls() {
        return new TrainerConfig(temp.resolve("unused"), 1,
                new TrainerConfig.SelfPlay(4,6,4,0,8,32,1024,NnueScoreMapping.V1),
                new TrainerConfig.Training(1,1,true),
                new TrainerConfig.Validation(2,0,8,4,6,1024,NnueScoreMapping.V1,PromotionPolicy.DEFAULT),
                2,TrainerConfig.DepthChange.REQUIRE_SAME,TrainerConfig.STANDARD_START,TrainingArchitecture.BRN2,.001,
                TrainingSource.HANDCRAFTED).withRunSeeds(new BrnRunSeeds(1,1));
    }
    @BeforeAll void fixture() throws Exception {
        source=temp.resolve("source"); var c=controls();
        var a=new TrajectorySampler.Sample(Board.startingPosition(),1);
        var b=new TrajectorySampler.Sample(Board.fromFen("7k/8/8/5K2/8/8/2Q5/8 w - - 0 1"),-1);
        var draw=new TrajectorySampler.Sample(a.board(),0);
        // Duplicates and intentionally mixed labels exercise multiplicity/order, not chess-strength truth.
        var partition=new BootstrapPartition(List.of(a,b,a,draw,b),List.of(b,draw,a),List.of(0,2),List.of(1,3));
        var stats=new SelfPlayBatch.Statistics(4,4,0,2,1,1,0,16,4,4,4,16,8);
        try(var store=new CheckpointStore(source,TrainingArchitecture.BRN2)) {
            store.initializeBrnRunSeeds(c.runSeeds());store.initializeBrnSupervision(BrnSupervision.blended(.75));
            store.initializeBrnTeacherStore(temp.resolve("deliberately-absent-teacher").toString());
            store.writeTrainingSource(TrainingSource.HANDCRAFTED);
            var state=new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
            var initial=store.initialize(state,new CheckpointManifest.Metadata(0,4,""));String parent=initial.manifest().id();
            for(int g=1;g<=2;g++) {
                String incumbent=store.recover().best().orElseThrow().manifest().id();
                var plan=new BootstrapPlan(parent,incumbent,g,TrainingSource.HANDCRAFTED,"","",c.generationSettings(g),
                        c.seed(g,TrainerConfig.SeedDomain.HOLDOUT),BrnSupervision.blended(.75),
                        temp.resolve("deliberately-absent-teacher").toString(),initial.manifest().id(),initial.manifest().networkSha256(),3);
                var data=new BootstrapData(plan.hash(),partition,stats,123);store.writeBootstrapPlan(plan);store.writeBootstrapData(plan,data);
                Brn2SelfPlayTraining.trainSamples(state.trainer(),partition.training(),c.training(g),new SelfPlayControl(),p->{},s->.25*s.target());
                var candidate=store.publish(state,new CheckpointManifest.Metadata(g,4,parent));
                var loss=HeldOutLoss.compare(candidate.model(),store.load(incumbent).model(),partition.heldOut());
                var evidence=BootstrapEvidence.create(plan,data,loss,loss,loss);
                var decision=CandidateLifecycle.completeDecision(store,store.recordBootstrapValidation(candidate.manifest().id(),evidence));
                new HistoryRepository(source).append(GenerationRecord.bootstrap(g,candidate.manifest().id(),incumbent,
                        decision.references().best().orElseThrow().manifest().id(),new GenerationRecord.Regime(4,4,0,6),
                        4,0,8L,.1,Instant.EPOCH,Instant.EPOCH,0L,0L,0L,0L,evidence));
                parent=candidate.manifest().id();
            }
        }
        replay=FrozenReplay.capture(source,c);
        assertFalse(Files.exists(temp.resolve("deliberately-absent-teacher")));
    }
    TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start();assertTrue(service.awaitTermination(Duration.ofSeconds(90)));
        assertFalse(service.snapshot().failed(),()->service.failure().toString());assertEquals("",service.historyWarning());return service.snapshot();
    }
    static class Probe extends FrozenWdlReplay.GuardedOperations {
        int targets,heldout,canonicalTrain,canonicalValidation;
        Probe(long stopAfter){super(stopAfter);}
        @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,List<TrajectorySampler.Sample> samples,
                SelfPlayTraining.Config cfg,SelfPlayControl control,Consumer<SelfPlayTraining.Progress> observer,
                BrnSupervision objective,ToDoubleFunction<TrajectorySampler.Sample> target) {
            assertEquals(BrnSupervision.WDL,objective);
            for(var sample:samples) assertEquals(sample.target(),target.applyAsDouble(sample));targets+=samples.size();
            return super.trainBootstrap(state,samples,cfg,control,observer,objective,target);
        }
        @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,List<TrajectorySampler.Sample> samples,
                SelfPlayTraining.Config cfg,SelfPlayControl control,Consumer<SelfPlayTraining.Progress> observer) {
            canonicalTrain++;return super.trainBootstrap(state,samples,cfg,control,observer);
        }
        @Override HeldOutLoss.Comparison validateBootstrap(NetworkModel candidate,NetworkModel incumbent,List<TrajectorySampler.Sample> samples,
                BrnSupervision objective,ToDoubleFunction<TrajectorySampler.Sample> target) {
            assertEquals(BrnSupervision.WDL,objective);
            for(var sample:samples)assertEquals(sample.target(),target.applyAsDouble(sample));heldout+=samples.size();
            return super.validateBootstrap(candidate,incumbent,samples,objective,target);
        }
        @Override HeldOutLoss.Comparison validateBootstrap(NetworkModel candidate,NetworkModel incumbent,List<TrajectorySampler.Sample> samples) {
            canonicalValidation++;return super.validateBootstrap(candidate,incumbent,samples);
        }
    }
    @Test void exactFrozenWdlContinuationUsesNoGeneratorOrTeacherAndStopsAtAbsoluteEndpoint() throws Exception {
        Path root=temp.resolve("continued");var stop=new Probe(1);
        try(var service=TrainerService.frozenReplay(root,replay,2,stop,s->{})) {
            stop.service=service;var end=finish(service);assertEquals(1,end.optimizerStep());assertEquals(0,end.totals().completedGenerations());
        }
        assertEquals(1,PartialGeneration.inspect(root).orElseThrow().training().samples());
        assertEquals(1,PartialGeneration.inspect(root).orElseThrow().training().updates());
        assertEquals(replay,FrozenReplay.read(root).orElseThrow());
        assertArrayEquals(replay.initialState(),Files.readAllBytes(root.resolve("checkpoints").resolve(replay.initialId()).resolve("training.state")));
        var resume=new Probe(0);TrainerSnapshot end;
        try(var service=TrainerService.frozenReplay(root,FrozenReplay.read(root).orElseThrow(),2,resume,s->{})){resume.service=service;end=finish(service);}
        assertEquals(2,end.generation());assertEquals(10,end.optimizerStep());assertEquals(2,resume.canonicalTrain);assertEquals(2,resume.canonicalValidation);
        assertEquals(10,resume.targets);assertEquals(6,resume.heldout);assertFalse(Files.exists(root.resolve(CheckpointStore.BRN_TEACHER_FILE)));
        var oracle=new Brn2Trainer(.001);var rows=new HistoryRepository(root).refresh().records();assertEquals(2,rows.size());
        for(int g=1;g<=2;g++) {
            var row=rows.get(g-1);var cp=CheckpointStore.readSnapshot(root,row.candidate());
            var plan=CheckpointInspection.bootstrapPlan(root,cp.manifest().parentId());var data=CheckpointInspection.bootstrapData(root,plan);
            assertEquals(replay.entry(g).contentHash(),FrozenReplay.contentHash(data));assertEquals(0,data.generationNanos());assertEquals(0,row.selfPlayNanos());
            assertEquals(TrainingSource.FROZEN_REPLAY,plan.source());assertEquals("",plan.teacherId());assertEquals("",plan.teacherStore());
            assertEquals(BrnSupervision.WDL,row.bootstrap().supervision());assertNull(row.bootstrap().teacherLoss());
            Brn2SelfPlayTraining.trainSamples(oracle,replay.data(g).partition().training(),controls().training(g),new SelfPlayControl(),p->{});
            assertArrayEquals(Brn2Codec.encodeTraining(oracle),Files.readAllBytes(root.resolve("checkpoints").resolve(row.candidate()).resolve("training.state")));
            assertEquals(HeldOutLoss.compare(cp.model(),CheckpointStore.readSnapshot(root,row.incumbent()).model(),data.partition().heldOut()),row.bootstrap().comparison());
        }
        try(var service=TrainerService.frozenReplay(root,replay,2)){assertEquals(0,finish(service).totals().completedGenerations());}
        assertEquals(2,new HistoryRepository(root).refresh().records().size());
        try(var service=TrainerService.frozenReplay(root,replay,1)) {
            service.start();assertTrue(service.awaitTermination(Duration.ofSeconds(30)));assertTrue(service.snapshot().failed());
        }
    }
    @Test void missingOrChangedSourceFailsBeforeCreatingAStudent() throws Exception {
        Path data=source.resolve("bootstrap").resolve(replay.entries().getFirst().parent()+".data");byte[] bytes=Files.readAllBytes(data);
        try {
            byte[] corrupt=bytes.clone();corrupt[corrupt.length-1]^=1;Files.write(data,corrupt);
            assertThrows(IOException.class,()->TrainerService.frozenReplay(temp.resolve("bad-source"),replay,1));
            assertFalse(Files.exists(temp.resolve("bad-source")));
            Files.delete(data);
            assertThrows(IOException.class,()->TrainerService.frozenReplay(temp.resolve("missing-source"),replay,1));
            assertFalse(Files.exists(temp.resolve("missing-source")));
        } finally {Files.write(data,bytes);}
        assertThrows(IOException.class,()->TrainerService.frozenReplay(source.resolve("nested"),replay,1));
        assertThrows(IOException.class,()->TrainerService.frozenReplay(source,replay,1));
        assertFalse(Files.exists(source.resolve("nested")));
    }
    @Test void metadataLossLiveResumeAndReconfigurationCannotFallBackOrArchive() throws Exception {
        Path root=temp.resolve("guarded");var stop=new Probe(1);
        try(var service=TrainerService.frozenReplay(root,replay,1,stop,s->{})){stop.service=service;finish(service);}
        byte[] attempt=Files.readAllBytes(root.resolve(GenerationAttempt.FILE));
        try(var service=TrainerService.resume(replay.config(root))){service.start();assertTrue(service.awaitTermination(Duration.ofSeconds(30)));assertTrue(service.snapshot().failed());}
        assertArrayEquals(attempt,Files.readAllBytes(root.resolve(GenerationAttempt.FILE)));
        assertFalse(Files.exists(root.resolve("restarted-generations")));
        assertThrows(IllegalArgumentException.class,()->replay.config(root).withSupervision(BrnSupervision.blended(0)));
        Path metadata=root.resolve(FrozenReplay.FILE);byte[] bytes=Files.readAllBytes(metadata);
        try {
            Files.delete(metadata);
            assertThrows(IOException.class,()->TrainerService.frozenReplay(root,replay,1));
        } finally {Files.write(metadata,bytes);}
        var wrong=new FrozenReplay(replay.sourceStore(),replay.initialId(),replay.modelHash(),replay.optimizerHash(),
                new BrnRunSeeds(2,2),replay.selfPlay(),replay.training(),replay.startingFen(),replay.learningRate(),replay.entries());
        assertThrows(IOException.class,()->TrainerService.frozenReplay(root,wrong,1));
        assertArrayEquals(attempt,Files.readAllBytes(root.resolve(GenerationAttempt.FILE)));
    }
    @Test void corruptCopiedDataAndChangedInitializationAreRejected() throws Exception {
        Path root=temp.resolve("corrupt-local");var stop=new Probe(1);
        try(var service=TrainerService.frozenReplay(root,replay,1,stop,s->{})){stop.service=service;finish(service);}
        Path data=root.resolve("bootstrap").resolve(replay.initialId()+".data");byte[] bytes=Files.readAllBytes(data);bytes[bytes.length-1]^=1;Files.write(data,bytes);
        assertThrows(IOException.class,()->TrainerService.frozenReplay(root,replay,1));
        var wrong=new FrozenReplay(replay.sourceStore(),replay.initialId(),replay.modelHash(),replay.optimizerHash(),
                replay.seeds(),replay.selfPlay(),replay.training(),replay.startingFen(),.002,replay.entries());
        assertThrows(IOException.class,wrong::initialState);
        assertFalse(Files.exists(root.resolve("restarted-generations")));
    }
    @Test void normalReferenceRecoveryStillResumesTheSavedOptimizerCursor() throws Exception {
        Path root=temp.resolve("reference-recovery");var stop=new Probe(1);
        try(var service=TrainerService.frozenReplay(root,replay,1,stop,s->{})){stop.service=service;finish(service);}
        Files.delete(root.resolve("refs/latest-training"));
        var resume=new Probe(0);
        try(var service=TrainerService.frozenReplay(root,replay,1,resume,s->{})) {
            resume.service=service;var end=finish(service);assertEquals(5,end.optimizerStep());assertEquals(1,end.totals().completedGenerations());
        }
        assertEquals(1,new HistoryRepository(root).refresh().records().size());
    }
}
