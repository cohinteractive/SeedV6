package com.ohinteractive.seedv6.training.service;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

/** Bounded real-search preflight; all writers are confined to temporary stores. */
@Timeout(180) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrnHandcraftedGenerationTest {
    @TempDir static Path temp;
    Path teacher;
    @BeforeAll void fixture() throws Exception {
        teacher = temp.resolve("teacher");
        try (var store = new CheckpointStore(teacher, TrainingArchitecture.NNUE)) {
            store.initialize(new NnueTrainer(TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 2, ""));
        }
    }
    static final String TRAJECTORY = "7k/8/8/5K2/8/8/2Q5/8 w - - 0 1";
    TrainerConfig config(Path root) { return config(root, BrnBootstrapTest.MATE); }
    TrainerConfig config(Path root, String fen) {
        return new TrainerConfig(root, 1, new TrainerConfig.SelfPlay(2, 1, 8, 0, 1, 8, 128, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(2, 0, 0, 2, 1, 128,
                NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)), 1, TrainerConfig.DepthChange.REQUIRE_SAME,
                fen, TrainingArchitecture.BRN2, .001, TrainingSource.HANDCRAFTED)
                .withRunSeeds(new BrnRunSeeds(1, 2));
    }
    TrainerConfig blend(Path root) { return config(root).withSupervision(BrnSupervision.blended(.75)).withTeacherStore(teacher.toString()); }
    BootstrapPlan plan(Path root) throws Exception {
        var line = CheckpointInspection.lineage(root, CheckpointInspection.reference(root, "latest-training"));
        return CheckpointInspection.bootstrapPlan(root, line.getFirst().id());
    }
    String samples(List<TrajectorySampler.Sample> samples, boolean targets) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(samples.size());
            for (var sample : samples) { for (long word : sample.board()) out.writeLong(word); if (targets) out.writeDouble(sample.target()); }
        }
        return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    }
    String content(Path root) throws Exception {
        var data = CheckpointInspection.bootstrapData(root, plan(root));
        return new BootstrapData("0".repeat(64), data.partition(), data.statistics(), 0).hash();
    }
    TrainerSnapshot complete(TrainerService service) throws Exception { return BrnBootstrapTest.finish(service); }
    void fail(TrainerService service, String text) throws Exception {
        service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(120)));
        assertTrue(service.snapshot().failed()); assertTrue(service.snapshot().failureSummary().contains(text), service.snapshot().failureSummary());
    }
    @Test void deterministicSmokeSeedAndGeneratorDifferencesAndExactWdlTargets() throws Exception {
        var rows = new ArrayList<String>();
        var roots = List.of(temp.resolve("same-a"), temp.resolve("same-b"), temp.resolve("different-seed"), temp.resolve("nnue"));
        for (int i = 0; i < roots.size(); i++) {
            Path root = roots.get(i); var cfg = config(root, TRAJECTORY).withRunSeeds(new BrnRunSeeds(1, i == 2 ? 3 : 2));
            if (i == 3) cfg = cfg.withSource(TrainingSource.bootstrap(teacher));
            var targetInspection = new TrainerService.Operations() {
                @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,
                        List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config c, SelfPlayControl control,
                        Consumer<SelfPlayTraining.Progress> observer, BrnSupervision supervision, ToDoubleFunction<TrajectorySampler.Sample> target) {
                    assertEquals(BrnSupervision.WDL, supervision);
                    for (var sample : samples) assertEquals(sample.target(), target.applyAsDouble(sample));
                    return super.trainBootstrap(state, samples, c, control, observer, supervision, target);
                }
            };
            TrainerSnapshot end;
            try (var service = TrainerService.fresh(cfg, new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), targetInspection, s -> {})) {
                end = complete(service); assertEquals(1, end.totals().completedGenerations()); assertTrue(end.optimizerStep() > 0);
            }
            var p = plan(root); var d = CheckpointInspection.bootstrapData(root, p);
            assertEquals(cfg.source(), p.source()); assertEquals(BrnSupervision.WDL, p.supervision());
            assertEquals("", p.teacherId()); assertEquals("", p.teacherStore());
            assertEquals(cfg.source().nnue(), !p.generatorId().isEmpty());
            var history = new HistoryRepository(root).refresh(); assertTrue(history.warnings().isEmpty());
            assertEquals(1, history.records().size()); var evidence = history.records().getFirst().bootstrap();
            assertEquals(cfg.source().mode(), evidence.generatorMode());
            assertEquals(HeldOutLoss.compare(CheckpointStore.readSnapshot(root, end.latestTrainingId()).model(),
                    CheckpointStore.readSnapshot(root, p.incumbentId()).model(), d.partition().heldOut()), evidence.comparison());
            assertEquals(evidence.comparison().decision(), history.records().getFirst().decision());
            rows.add(root.getFileName()+" plan="+p.hash()+" batch="+content(root)+" training="+samples(d.partition().training(),true)
                    +" heldout="+samples(d.partition().heldOut(),true)+" positions="+samples(d.partition().training(),false)
                    +" updates="+end.optimizerStep()+" completed="+d.statistics().completedGames()+" decision="+evidence.comparison().decision());
        }
        assertEquals(plan(roots.get(0)), plan(roots.get(1))); assertEquals(content(roots.get(0)), content(roots.get(1)));
        assertNotEquals(content(roots.get(0)), content(roots.get(2))); assertNotEquals(content(roots.get(0)), content(roots.get(3)));
        var a = CheckpointInspection.bootstrapData(roots.get(0), plan(roots.get(0))).partition().training();
        for (int i : new int[]{2,3}) assertNotEquals(samples(a,false), samples(CheckpointInspection.bootstrapData(roots.get(i),plan(roots.get(i))).partition().training(),false));
        assertEquals(CheckpointInspection.reference(roots.get(0),"latest-training"), CheckpointInspection.reference(roots.get(1),"latest-training"));
        // Independently replay trajectories and check each selected board against the actual terminal result.
        var cfg = config(temp.resolve("oracle"), TRAJECTORY);
        for (int game = 0; game < 8; game++) {
            var trajectory = SelfPlayRunner.play(SearchEvaluation.handcrafted(), cfg.selfPlay(1), game, cfg.startingBoard(), new SelfPlayControl());
            if (!trajectory.termination().completed()) continue;
            for (var sample : TrajectorySampler.sample(trajectory,8))
                assertEquals(trajectory.result().orElseThrow().target(Board.player((int)sample.board()[Board.STATUS])), sample.target());
        }
        Path output=Path.of("build/brn2-handcrafted/preflight.txt"); Files.createDirectories(output.getParent()); Files.write(output,rows);
    }
    @Test void handcraftedBlendUsesSeparateStaticTeacherAndResumePreservesExactOptimizer() throws Exception {
        Path whole=temp.resolve("blend-whole"), split=temp.resolve("blend-split");
        TrainerSnapshot expected;
        try (var service=TrainerService.fresh(blend(whole),new Brn2Trainer(.001))) { expected=complete(service); }
        var owner=new AtomicReference<TrainerService>();
        var stopping=new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg,SelfPlayControl control,Consumer<SelfPlayTraining.Progress> observer,
                    BrnSupervision objective,ToDoubleFunction<TrajectorySampler.Sample> target) {
                var nnue=new NnueEvaluator(com.ohinteractive.seedv6.core.nnue.NnueNetwork.initialized(17));
                for(var sample:samples) assertEquals(.25*sample.target()+.75*BrnSupervision.teacherValue(nnue,sample),target.applyAsDouble(sample));
                return super.trainBootstrap(state,samples,cfg,control,p->{observer.accept(p);owner.get().stop();},objective,target);
            }
        };
        try(var service=TrainerService.fresh(blend(split),new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),stopping,s->{})) {
            owner.set(service);assertEquals(1,complete(service).optimizerStep());
        }
        var pin=plan(split);assertEquals(TrainingSource.HANDCRAFTED,pin.source()); assertEquals("",pin.generatorId());
        assertEquals(CheckpointInspection.reference(teacher,"best"),pin.teacherId()); assertEquals(teacher.toString(),pin.teacherStore());
        byte[] attempt=Files.readAllBytes(split.resolve(GenerationAttempt.FILE));
        for(var incompatible:List.of(blend(split).withSource(TrainingSource.bootstrap(teacher)),blend(split).withRunSeeds(new BrnRunSeeds(1,3)),
                blend(split).withSupervision(BrnSupervision.WDL),blend(split).withSupervision(BrnSupervision.blended(.5)),
                blend(split).withTeacherStore(temp.resolve("another").toString()))) {
            try(var service=TrainerService.resume(incompatible)) { fail(service,"differ"); }
            assertArrayEquals(attempt,Files.readAllBytes(split.resolve(GenerationAttempt.FILE)));assertFalse(Files.exists(split.resolve("restarted-generations")));
        }
        var replay=new TrainerService.Operations() {
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig c,long[] board,SelfPlayControl control,Consumer<SelfPlayBatch.Progress> observer) {
                throw new AssertionError("Resume must reuse saved data");
            }
        };
        // A missing pinned teacher must fail even with saved data; restoring exactly it allows Resume.
        Path checkpoint=teacher.resolve("checkpoints").resolve(pin.teacherId()), away=teacher.resolve("temporarily-unavailable");
        Files.move(checkpoint,away);
        try { try(var service=TrainerService.resume(blend(split))) {fail(service,"Pinned NNUE teacher");} }
        finally {Files.move(away,checkpoint);}
        var reload=blend(split).withSource(null).withSupervision(null).withRunSeeds(null).withTeacherStore(null);
        try(var service=TrainerService.resume(reload,replay,s->{})) {assertEquals(expected.latestTrainingId(),complete(service).latestTrainingId());}
        assertEquals(pin,plan(split)); assertArrayEquals(BrnBootstrapTest.state(whole,expected.latestTrainingId()),BrnBootstrapTest.state(split,expected.latestTrainingId()));
        var data=CheckpointInspection.bootstrapData(split,pin); var row=new HistoryRepository(split).refresh().records().getFirst();
        var nnue=new NnueEvaluator(pin.loadTeacher(split).model().nnue());
        assertEquals(HeldOutLoss.compare(CheckpointStore.readSnapshot(split,row.candidate()).model(),CheckpointStore.readSnapshot(split,row.incumbent()).model(),
                data.partition().heldOut(),sample->.25*sample.target()+.75*BrnSupervision.teacherValue(nnue,sample)),row.bootstrap().comparison());
        assertEquals(pin.teacherId(),row.bootstrap().teacherId()); assertEquals(row.bootstrap().comparison().decision(),row.decision());
    }
    @Test void pinnedTeacherSurvivesBestChangeAndNextGenerationPinsNewBest() throws Exception {
        Path root=temp.resolve("changing-teacher-student"), external=temp.resolve("changing-teacher");
        String original, replacement;
        try(var store=new CheckpointStore(external,TrainingArchitecture.NNUE)) {
            original=store.initialize(new NnueTrainer(TrainableNnue.initialized(31)),new CheckpointManifest.Metadata(0,2,"")).manifest().id();
        }
        var c=blend(root).withTeacherStore(external.toString());var owner=new AtomicReference<TrainerService>();
        try(var service=TrainerService.fresh(c,new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),new TrainerService.Operations(),v->{
            if(v.state()==TrainerSnapshot.State.TRAINING) owner.get().stop();
        })) {owner.set(service);complete(service);}
        assertEquals(original,plan(root).teacherId());
        try(var store=new CheckpointStore(external,TrainingArchitecture.NNUE)) {
            replacement=store.publish(new NnueTrainer(TrainableNnue.initialized(32)),new CheckpointManifest.Metadata(1,2,original)).manifest().id();
            var result=TrainerServiceTest.outcome(c.validation(1),c.startingBoard(),com.ohinteractive.seedv6.rules.GameHistory.initial(c.startingBoard()),true);
            var record=store.recordValidation(replacement,original,result,new PromotionPolicy(1,.9,0));store.completePromotion(record.id());
        }
        try(var service=TrainerService.resume(c.withTeacherStore(null))) {assertEquals(original,complete(service).bootstrapValidation().orElseThrow().evidence().teacherId());}
        try(var service=TrainerService.resume(c.withTeacherStore(null))) {assertEquals(replacement,complete(service).bootstrapValidation().orElseThrow().evidence().teacherId());}
        var rows=new HistoryRepository(root).refresh();assertTrue(rows.warnings().isEmpty());assertEquals(2,rows.records().size());
        assertEquals(List.of(original,replacement),rows.records().stream().map(r->r.bootstrap().teacherId()).toList());
    }
    @Test void nnueGenerationCanUseAnIndependentTeacherStore() throws Exception {
        Path root=temp.resolve("two-nnue-roles"), external=temp.resolve("independent-teacher");
        try(var store=new CheckpointStore(external,TrainingArchitecture.NNUE)) {
            store.initialize(new NnueTrainer(TrainableNnue.initialized(41)),new CheckpointManifest.Metadata(0,2,""));
        }
        var c=blend(root).withSource(TrainingSource.bootstrap(teacher)).withTeacherStore(external.toString());
        try(var service=TrainerService.fresh(c,new Brn2Trainer(.001))) {complete(service);}
        var p=plan(root);assertEquals(CheckpointInspection.reference(teacher,"best"),p.generatorId());
        assertEquals(CheckpointInspection.reference(external,"best"),p.teacherId());assertNotEquals(p.generatorId(),p.teacherId());
        var d=CheckpointInspection.bootstrapData(root,p);var row=new HistoryRepository(root).refresh().records().getFirst();
        var nnue=new NnueEvaluator(p.loadTeacher(root).model().nnue());
        assertEquals(HeldOutLoss.compare(CheckpointStore.readSnapshot(root,row.candidate()).model(),CheckpointStore.readSnapshot(root,row.incumbent()).model(),
                d.partition().heldOut(),p.supervision().targets(nnue)),row.bootstrap().comparison());
        Path metadata=root.resolve(CheckpointStore.BRN_TEACHER_FILE);byte[] bytes=Files.readAllBytes(metadata);Files.delete(metadata);
        assertThrows(IOException.class,()->CheckpointStore.readBrnTeacherStore(root));Files.write(metadata,bytes);
        bytes[bytes.length-1]^=1;Files.write(metadata,bytes);assertThrows(IOException.class,()->CheckpointStore.readBrnTeacherStore(root));
    }
    @Test void defaultFreshSourceAndSeedsNeedNoNnueCheckpoint() throws Exception {
        Path root=temp.resolve("defaults");
        try(var service=TrainerService.fresh(config(root).withSource(null).withRunSeeds(null),new Brn2Trainer(.001))) {complete(service);}
        assertEquals(TrainingSource.HANDCRAFTED,CheckpointStore.readTrainingSource(root).orElseThrow());
        assertEquals(new BrnRunSeeds(1,1),CheckpointStore.readBrnRunSeeds(root).orElseThrow());
        assertEquals(BrnSupervision.WDL,plan(root).supervision());
        Files.delete(root.resolve(CheckpointStore.TRAINING_SOURCE_FILE));
        assertThrows(IOException.class,()->CheckpointStore.readTrainingSource(root));
    }
}
