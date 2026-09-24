package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class BrnCaptureIntegrationTest {
    @TempDir Path temp;
    static final String FEN = "7k/6rp/5KQ1/8/8/8/8/8 w - - 0 1";
    TrainerConfig config(Path root) {
        return new TrainerConfig(root,71,new TrainerConfig.SelfPlay(1,1,4,0,0,1,8,NnueScoreMapping.V1),
                new TrainerConfig.Training(1,1,true),new TrainerConfig.Validation(2,0,0,1,1,8,NnueScoreMapping.V1,
                new PromotionPolicy(1,.9,0)),1,TrainerConfig.DepthChange.REQUIRE_SAME,FEN,TrainingArchitecture.BRN2,.001,
                TrainingSource.HANDCRAFTED).withSupervision(BrnSupervision.blended(.5))
                .withTeacherStore(temp.resolve("teacher").toString()).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    @Test void numericValidationLegacyDefaultsAndNonzeroIdentityDoNotChangeOrdinaryOrNnueSettings() throws Exception {
        for(double good:new double[]{0,2,.125,Double.MIN_VALUE}) assertEquals(good,new BrnCaptureConsistency(good).lambda());
        for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new BrnCaptureConsistency(bad));
        assertEquals(BrnCaptureConsistency.OFF,new BrnCaptureConsistency(-0.0));
        var old=config(temp.resolve("legacy")); var zero=old.withCaptureConsistency(BrnCaptureConsistency.OFF);
        var two=old.withCaptureConsistency(new BrnCaptureConsistency(2));
        assertEquals(BrnCaptureConsistency.OFF,CheckpointStore.readBrnCaptureConsistency(old.checkpointRoot()));
        assertEquals(old.generationSettings(1),zero.generationSettings(1));
        assertEquals(old.attemptSettings(1,old.source()),zero.attemptSettings(1,old.source()));
        assertEquals(old.historySettings(old.source()),zero.historySettings(old.source()));
        assertTrue(two.generationSettings(1).contains("brn-capture-v1:lambda=2.0:seed="));
        assertNotEquals(two.generationSettings(1),two.generationSettings(2));
        String id="g000000-s000000000-"+"a".repeat(64);
        assertFalse(GenerationAttempt.create(id,id,1,zero,zero.source()).matches(two,two.source()));
        assertTrue(GenerationAttempt.create(id,id,1,two,two.source()).matches(two,two.source()));
        assertThrows(IllegalArgumentException.class,()->two.withSupervision(BrnSupervision.WDL));
        assertThrows(IllegalArgumentException.class,()->two.withSupervision(BrnSupervision.blended(.75)));
        var nnue=new TrainerConfig(temp.resolve("nnue"),71,old.selfPlay(),old.training(),old.validation(),1,old.depthChange());
        assertEquals(nnue.generationSettings(1),nnue.withCaptureConsistency(BrnCaptureConsistency.OFF).generationSettings(1));
        assertThrows(IllegalArgumentException.class,()->nnue.withCaptureConsistency(new BrnCaptureConsistency(2)));
        for(var domain:TrainerConfig.SeedDomain.values()) if(domain!=TrainerConfig.SeedDomain.CAPTURE)
            assertEquals(old.seed(1,domain),two.seed(1,domain));
        try(var store=new CheckpointStore(old.checkpointRoot(),TrainingArchitecture.BRN2)) {
            store.writeBrnCaptureConsistency(BrnCaptureConsistency.OFF);
            assertFalse(Files.exists(old.checkpointRoot().resolve(CheckpointStore.BRN_CAPTURE_CONSISTENCY_FILE)));
            store.writeBrnCaptureConsistency(new BrnCaptureConsistency(2));
        }
        assertEquals(new BrnCaptureConsistency(2),CheckpointStore.readBrnCaptureConsistency(old.checkpointRoot()));
        try(var store=new CheckpointStore(old.checkpointRoot(),TrainingArchitecture.BRN2)) { store.writeBrnCaptureConsistency(BrnCaptureConsistency.OFF); }
        assertEquals(BrnCaptureConsistency.OFF,CheckpointStore.readBrnCaptureConsistency(old.checkpointRoot()));
    }
    /** Supplies four copies of a real completed one-ply mate trajectory, with no self-play search. */
    static class Fixture extends TrainerService.Operations {
        @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig cfg,long[] board,SelfPlayControl control,Consumer<SelfPlayBatch.Progress> observer) {
            var game=new HeadlessGame(board,8);
            game.play(Arrays.stream(game.legalMoves()).filter(m->Move.coordinate(m).equals("g6g7")).findFirst().orElseThrow());
            assertEquals(GameTermination.WHITE_CHECKMATES_BLACK,game.termination());
            var samples=new ArrayList<TrajectorySampler.Sample>();var summaries=new ArrayList<SelfPlayBatch.GameSummary>();
            for(int i=0;i<cfg.games();i++) {
                samples.addAll(TrajectorySampler.sample(game.trajectory(),1));
                summaries.add(new SelfPlayBatch.GameSummary(i,SelfPlayRunner.gameSeed(cfg.seed(),i),game.termination(),1,1,1,null));
            }
            control.savedGames(new SelfPlayBatch.Saved(summaries,samples));
            return super.generateHandcrafted(cfg,board,control,observer);
        }
    }
    TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start();assertTrue(service.awaitTermination(Duration.ofSeconds(90)));
        assertEquals(TrainerSnapshot.State.STOPPED,service.snapshot().state(),service.failure().map(Throwable::toString).orElse(""));
        return service.snapshot();
    }
    @Test void tinyPairedTrainingCheckpointPinnedTeacherExactResumeAndLambdaChangeArchivesUnfinishedWork() throws Exception {
        String teacherId;
        try(var store=new CheckpointStore(temp.resolve("teacher"),TrainingArchitecture.NNUE)) {
            teacherId=store.initialize(new NnueTrainer(TrainableNnue.initialized(17)),new CheckpointManifest.Metadata(0,1,"")).manifest().id();
        }
        Path teacherFile=temp.resolve("teacher/checkpoints").resolve(teacherId).resolve("training.state");
        byte[] teacherBefore=Files.readAllBytes(teacherFile);
        var lambda=new BrnCaptureConsistency(2);
        var continuous=config(temp.resolve("continuous")).withCaptureConsistency(lambda); TrainerSnapshot expected;
        try(var service=TrainerService.fresh(continuous,new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),new Fixture(),s->{})) { expected=finish(service); }
        assertEquals(2,expected.optimizerStep()); // 2 ordinary training rows; other 2 are held out.
        var split=config(temp.resolve("split")).withCaptureConsistency(lambda);var owner=new AtomicReference<TrainerService>();
        var stop=new Fixture() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg,SelfPlayControl control,Consumer<SelfPlayTraining.Progress> observer,
                    BrnSupervision supervision,ToDoubleFunction<TrajectorySampler.Sample> target,BrnCaptureConsistency capture,NnueEvaluator teacher,long seed) {
                assertEquals(lambda,capture);
                return super.trainBootstrap(state,samples,cfg,control,p->{observer.accept(p);owner.get().stop();},supervision,target,capture,teacher,seed);
            }
        };
        try(var service=TrainerService.fresh(split,new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),stop,s->{})) {
            owner.set(service);assertEquals(1,finish(service).optimizerStep());
        }
        assertEquals(lambda,CheckpointStore.readBrnCaptureConsistency(split.checkpointRoot()));
        var before=PartialGeneration.inspect(split.checkpointRoot()).orElseThrow();
        assertTrue(before.attempt().settings().contains(lambda.settingsSuffix()));
        Path changed=temp.resolve("changed");
        try(var paths=Files.walk(split.checkpointRoot())) { for(Path from:paths.toList()) {
            Path to=changed.resolve(split.checkpointRoot().relativize(from));
            if(Files.isDirectory(from)) Files.createDirectories(to); else Files.copy(from,to);
        }}
        // Omitted setting restores lambda=2 and the exact pinned teacher/data/order after restart.
        try(var service=TrainerService.resume(config(split.checkpointRoot()).withSupervision(null),new Fixture(){
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig c,long[] b,SelfPlayControl s,Consumer<SelfPlayBatch.Progress> o) {
                throw new AssertionError("Same settings must reuse durable data");
            }
        },s->{})) {
            var end=finish(service);assertEquals(expected.latestTrainingId(),end.latestTrainingId());
            assertEquals(lambda,end.run().orElseThrow().effective().effectiveCaptureConsistency());
        }
        assertArrayEquals(BrnBootstrapTest.state(continuous.checkpointRoot(),expected.latestTrainingId()),
                BrnBootstrapTest.state(split.checkpointRoot(),expected.latestTrainingId()));
        try(var service=TrainerService.resume(config(changed).withCaptureConsistency(BrnCaptureConsistency.OFF),new Fixture(),s->{})) {
            finish(service);assertTrue(service.lifecycleNotice().contains("Restarted unfinished generation"));
        }
        assertTrue(Files.isDirectory(changed.resolve("restarted-generations")));
        assertEquals(BrnCaptureConsistency.OFF,CheckpointStore.readBrnCaptureConsistency(changed));
        assertArrayEquals(teacherBefore,Files.readAllBytes(teacherFile));
        try(var store=new CheckpointStore(continuous.checkpointRoot(),TrainingArchitecture.BRN2)) {
            var saved=store.load(expected.latestTrainingId());var restored=store.resumeState(saved.manifest().id());
            assertEquals(2,restored.step());assertEquals(2,saved.manifest().architecture().schemaVersion());
            var plan=store.bootstrapPlan(saved.manifest().parentId()).orElseThrow();assertEquals(teacherId,plan.teacherId());
            assertTrue(plan.settings().contains(lambda.settingsSuffix()));
            var data=store.bootstrapData(plan).orElseThrow();
            var ordinary=(NetworkTrainingState.Brn2)store.resumeState(plan.parentId());
            var pinnedTeacher=new NnueEvaluator(plan.loadTeacher(continuous.checkpointRoot()).model().nnue());
            Brn2SelfPlayTraining.trainSamples(ordinary.trainer(),data.partition().training(),continuous.training(1),new SelfPlayControl(),p->{},BrnSupervision.blended(.5).targets(pinnedTeacher));
            assertFalse(Arrays.equals(Brn2Codec.encodeTraining(ordinary.trainer()),
                    Brn2Codec.encodeTraining(((NetworkTrainingState.Brn2)restored).trainer())),"Fixture must execute an auxiliary update");

            var evaluator=saved.model().evaluation(NnueScoreMapping.V1);
            long[] board=Board.fromFen(FEN);var state=evaluator.newState(4);state.initialize(board,0);state.evaluate(board,0);
            try(var search=new AlphaBetaPvsSearch(evaluator,1<<14)) {
                var result=search.search(new SearchRequest(board,GameHistory.initial(board),1,SearchObserver.NONE,
                        SearchControl.controlled(10000,0,-1,()->0)));
                assertTrue(result.completed());
            }
        }
    }
}
