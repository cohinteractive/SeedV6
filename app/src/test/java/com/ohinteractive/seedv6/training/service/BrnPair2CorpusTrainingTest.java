package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Trainer;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class BrnPair2CorpusTrainingTest {
    @TempDir Path temporary;
    TrainerConfig config(Path root, Path data) throws Exception {
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN_PAIR2)) {
            new DataSources(1,List.of(DataSource.register("test",data,1)),false).save(DataSources.directory(root));
        }
        var base=Brn2TrainerServiceTest.config(root,1);
        return new TrainerConfig(root,71,base.selfPlay(),new TrainerConfig.Training(2,4,true),base.validation(),1,
                base.depthChange(),TrainerConfig.STANDARD_START,TrainingArchitecture.BRN_PAIR2,.01)
                .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                .withCorpusTraining(new CorpusTrainingConfig(12)).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    @Test void ordinarySequentialServiceTrainsAndResumesExactlyWithoutGeneratingPositions() throws Exception {
        Path data=temporary.resolve("source.jsonl");var lines=new StringBuilder();
        for(int i=0;i<80;i++)lines.append(SourceReadersTest.line(i));Files.writeString(data,lines);
        Path continuous=temporary.resolve("continuous"),split=temporary.resolve("split");
        var a=config(continuous,data);var b=config(split,data);TrainerSnapshot expected,actual;
        try(var service=TrainerService.fresh(a,new NetworkTrainingState.BrnPair2(new BrnPair2Trainer(.01)),NnueCorpusTrainingTest.forbidden(),s->{})) {
            expected=NnueCorpusTrainingTest.finish(service);
            assertEquals(24,expected.training().orElseThrow().samplesTrained());
            assertEquals(6,expected.training().orElseThrow().optimizerUpdates());
            assertEquals(0,expected.totals().selfPlayGames());
            assertEquals(CorpusTraining.TargetPolicy.BRN3_CP_WDL_V1.identity,service.corpusReport().orElseThrow().adapterIdentity());
            assertEquals(0,service.corpusReport().orElseThrow().mateExamples());
            try(var store=new CheckpointStore(continuous,TrainingArchitecture.BRN_PAIR2)) {
                var latest=store.load(expected.latestTrainingId());var parent=store.load(latest.manifest().parentId());
                assertNotEquals(parent.manifest().networkSha256(),latest.manifest().networkSha256());
                assertTrue(store.validationFor(latest.manifest().id()).orElseThrow().bootstrap().comparison()!=null);
            }
        }
        var owner=new AtomicReference<TrainerService>();
        var stop=new TrainerService.Operations(){
            @Override Optional<SelfPlayTraining.Statistics> trainCorpus(NetworkTrainingState state,CorpusTraining.Examples examples,
                    SelfPlayTraining.Config c,SelfPlayControl control,Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainCorpus(state,examples,c,control,p->{observer.accept(p);owner.get().stop();});
            }
        };
        try(var service=TrainerService.fresh(b,new NetworkTrainingState.BrnPair2(new BrnPair2Trainer(.01)),stop,s->{})) {
            owner.set(service);assertEquals(1,NnueCorpusTrainingTest.finish(service).optimizerStep());
        }
        var reservation=new SourceLedger(split).active().orElseThrow();
        try(var service=TrainerService.resume(b,NnueCorpusTrainingTest.forbidden(),s->{})){actual=NnueCorpusTrainingTest.finish(service);}
        assertEquals(expected.latestTrainingId(),actual.latestTrainingId());
        try(var one=new CheckpointStore(continuous,TrainingArchitecture.BRN_PAIR2);var two=new CheckpointStore(split,TrainingArchitecture.BRN_PAIR2)) {
            assertArrayEquals(one.resumeState(expected.latestTrainingId()).encode(),two.resumeState(actual.latestTrainingId()).encode());
        }
        assertEquals(reservation.ranges(),new SourceLedger(split).generation(1).orElseThrow().ranges());
        assertThrows(IllegalArgumentException.class,()->b.withSource(TrainingSource.SELF_PLAY));
    }
    @Test void targetPolicyRejectsMateAndNormalizesPerspectiveOnce() {
        var record=NnueCorpusTrainingTest.record(1,com.ohinteractive.seedv6.corpus.CorpusRecord.CP,150,
                com.ohinteractive.seedv6.corpus.CorpusRecord.WHITE,20);
        long[] board=com.ohinteractive.seedv6.core.Board.fromFen(record.position().fen()+" 1");
        var policy=CorpusTraining.TargetPolicy.BRN3_CP_WDL_V1;
        assertNull(policy.rejection(record));
        assertEquals(com.ohinteractive.seedv6.core.brn3.Brn3Objective.outcome(-1.5,board),policy.target(record,board));
        assertNotNull(policy.rejection(NnueCorpusTrainingTest.record(1,com.ohinteractive.seedv6.corpus.CorpusRecord.MATE,3,
                com.ohinteractive.seedv6.corpus.CorpusRecord.WHITE,20)));
    }
    @Test void normalInitializerAndRestartAdvanceOwnGenerationsAndSourceCursor()throws Exception {
        Path data=temporary.resolve("fresh-source.jsonl");var text=new StringBuilder();
        for(int i=0;i<100;i++)text.append(SourceReadersTest.line(i));Files.writeString(data,text);
        Path root=temporary.resolve("Pair-2");var config=config(root,data);TrainerSnapshot first,second;
        try(var service=TrainerService.freshInitialized(config,NnueCorpusTrainingTest.forbidden(),s->{})) {
            first=NnueCorpusTrainingTest.finish(service);
        }
        var provenance=LineageProvenance.read(root).orElseThrow();
        assertNull(provenance.initializationSeed());assertEquals(71,provenance.firstRunSeed());
        var previous=new SourceLedger(root).generation(1).orElseThrow();
        try(var service=TrainerService.resume(config,NnueCorpusTrainingTest.forbidden(),s->{})) {
            second=NnueCorpusTrainingTest.finish(service);
        }
        assertEquals(12,second.optimizerStep());
        var next=new SourceLedger(root).generation(2).orElseThrow();
        assertEquals(previous.ranges().getFirst().end(),next.ranges().getFirst().start());
        assertNotEquals(first.latestTrainingId(),second.latestTrainingId());
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN_PAIR2)) {
            var latest=store.load(second.latestTrainingId());
            assertEquals(2,latest.manifest().generation());assertEquals(first.latestTrainingId(),latest.manifest().parentId());
            assertEquals(TrainingArchitecture.BRN_PAIR2,store.load(second.bestId()).model().architecture());
            assertNotNull(store.validationFor(latest.manifest().id()).orElseThrow());
        }
    }
}
