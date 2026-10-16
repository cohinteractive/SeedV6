package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;
import com.ohinteractive.seedv6.core.brn3.Brn3SearchCalibration;
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
class Brn3CorpusTrainingTest {
    @TempDir Path temporary;
    TrainerConfig config(Path root, Path data) throws Exception {
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN3)) {
            new DataSources(1,List.of(DataSource.register("test",data,1)),false).save(DataSources.directory(root));
        }
        var base=Brn2TrainerServiceTest.config(root,1);
        return new TrainerConfig(root,71,base.selfPlay(),new TrainerConfig.Training(2,4,true),base.validation(),1,
                base.depthChange(),TrainerConfig.STANDARD_START,TrainingArchitecture.BRN3,.003)
                .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                .withCorpusTraining(new CorpusTrainingConfig(12)).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    @Test void ordinarySequentialServiceTrainsAndResumesExactlyWithoutGeneratingPositions() throws Exception {
        Path data=temporary.resolve("source.jsonl");var lines=new StringBuilder();
        for(int i=0;i<80;i++)lines.append(SourceReadersTest.line(i));Files.writeString(data,lines);
        Path continuous=temporary.resolve("continuous"),split=temporary.resolve("split");
        var a=config(continuous,data);var b=config(split,data);TrainerSnapshot expected,actual;
        try(var service=TrainerService.fresh(a,new NetworkTrainingState.Brn3(new Brn3Trainer(71)),NnueCorpusTrainingTest.forbidden(),s->{})) {
            expected=NnueCorpusTrainingTest.finish(service);
            assertEquals(24,expected.training().orElseThrow().samplesTrained());
            assertEquals(6,expected.training().orElseThrow().optimizerUpdates());
            assertEquals(0,expected.totals().selfPlayGames());
            assertEquals(CorpusTraining.TargetPolicy.BRN3_CP_WDL_V1.identity,service.corpusReport().orElseThrow().adapterIdentity());
            assertEquals(0,service.corpusReport().orElseThrow().mateExamples());
            try(var store=new CheckpointStore(continuous,TrainingArchitecture.BRN3)) {
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
        try(var service=TrainerService.fresh(b,new NetworkTrainingState.Brn3(new Brn3Trainer(71)),stop,s->{})) {
            owner.set(service);assertEquals(1,NnueCorpusTrainingTest.finish(service).optimizerStep());
        }
        var reservation=new SourceLedger(split).active().orElseThrow();
        try(var service=TrainerService.resume(b,NnueCorpusTrainingTest.forbidden(),s->{})){actual=NnueCorpusTrainingTest.finish(service);}
        assertEquals(expected.latestTrainingId(),actual.latestTrainingId());
        try(var one=new CheckpointStore(continuous,TrainingArchitecture.BRN3);var two=new CheckpointStore(split,TrainingArchitecture.BRN3)) {
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
    @Test void oldGamePairAttemptsCannotMixCalibrationButHeldOutResumeKeepsItsIdentity() throws Exception {
        Path data=temporary.resolve("calibration-source.jsonl");
        Files.writeString(data,SourceReadersTest.line(0)+SourceReadersTest.line(1));
        var held=config(temporary.resolve("calibration"),data);
        var games=held.withValidationMethod(ValidationMethod.GAME_PAIRS);
        String suffix="|brn3-search="+Brn3SearchCalibration.ID;
        String parent="g000000-s000000000-"+"a".repeat(64);
        var current=GenerationAttempt.create(parent,parent,1,games,games.source());
        assertTrue(current.matches(games,games.source()));
        assertTrue(current.settings().endsWith(suffix));
        var old=new GenerationAttempt(current.parentId(),current.incumbentId(),current.generation(),current.source(),
                current.settings().replace(suffix,""),current.format(),"");
        assertFalse(old.matches(games,games.source()),"Old partial pairs must follow the existing restart/archive path");
        var rawOnly=GenerationAttempt.create(parent,parent,1,held,held.source());
        assertFalse(rawOnly.settings().contains("brn3-search="));
        assertTrue(rawOnly.matches(held,held.source()));
        assertTrue(games.historySettings(games.source()).contains(suffix));
        var nnue=Brn2TrainerServiceTest.config(temporary.resolve("nnue-history"),1);
        assertFalse(nnue.attemptSettings(1,nnue.source()).contains("brn3-search="));
    }
}
