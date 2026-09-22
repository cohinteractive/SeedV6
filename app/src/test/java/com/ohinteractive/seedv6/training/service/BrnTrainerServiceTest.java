package com.ohinteractive.seedv6.training.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class BrnTrainerServiceTest {
    @TempDir Path root;
    static final String MATE = "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1";
    static TrainerConfig config(Path root, long generations) {
        return new TrainerConfig(root, 71,
                new TrainerConfig.SelfPlay(1, 1, 2, 0, 0, 2, 4, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true),
                new TrainerConfig.Validation(2, 0, 0, 1, 1, 4, NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                generations, TrainerConfig.DepthChange.REQUIRE_SAME, MATE, TrainingArchitecture.BRN, .001);
    }
    static TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(45)));
        assertEquals(STOPPED, service.snapshot().state(), service.failure().map(Throwable::toString).orElse(""));
        return service.snapshot();
    }
    static byte[] state(Path root, String id) throws IOException {
        return Files.readAllBytes(root.resolve("checkpoints").resolve(id).resolve(CheckpointManifest.TRAINING_FILE));
    }
    static CheckpointManifest.Metadata metadata(long generation, String parent) {
        return new CheckpointManifest.Metadata(generation, 1, parent);
    }

    @Test void realTwoGenerationSmokeBootstrapsZeroTrainsPublishesValidatesAndRecordsHistory() throws Exception {
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config(root, 2), new BrnTrainer(.001))) { end = finish(service); }
        assertEquals(2, end.totals().completedGenerations()); assertEquals(4, end.totals().completedGames());
        assertEquals(0, end.totals().abortedGames()); assertEquals(4, end.totals().optimizerUpdates());
        assertEquals(2, end.validation().orElseThrow().validPairs());
        assertEquals(2, end.totals().retainedCandidates());
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            var refs = store.recover(); var best = refs.best().orElseThrow(); var latest = refs.latestTraining().orElseThrow();
            assertEquals(0, best.manifest().generation()); assertEquals(0, best.manifest().optimizerStep());
            assertEquals(TrainingArchitecture.BRN, latest.manifest().architecture());
            assertArrayEquals(BrnCodec.encodeTraining(new BrnTrainer(.001)), state(root, best.manifest().id()));
            assertEquals(4, store.resumeState(latest.manifest().id()).step());
            assertNotEquals(best.manifest().networkSha256(), latest.manifest().networkSha256());
            assertInstanceOf(NetworkModel.Brn.class, latest.model());
            assertFalse(Files.exists(root.resolve("checkpoints").resolve(latest.manifest().id()).resolve("network.nnue")));
        }
        var history = new HistoryRepository(root).refresh();
        assertTrue(history.warnings().isEmpty()); assertEquals(2, history.records().size());
        for (var record : history.records()) {
            assertEquals(GenerationRecord.Outcome.RETAINED, record.outcome()); assertEquals(.5, record.score());
            assertEquals(2, record.samples()); assertNotNull(record.loss());
        }
        System.out.println("BRN_REAL_SMOKE generations=2 selfPlayCompleted=4 aborted=0 samples=4 updates=4 validPairs=4 retained=2");
    }

    @Test void splitResumeIsBitExactAcrossServiceSerializationAndUsesStoredLearningRate() throws Exception {
        Path continuous = root.resolve("continuous"), split = root.resolve("split");
        TrainerSnapshot a, b, c;
        try (var service = TrainerService.fresh(config(continuous, 2), new BrnTrainer(.001))) { a = finish(service); }
        var initial = new BrnTrainer(.001);
        try (var service = TrainerService.fresh(config(split, 1), initial)) {
            initial.train(config(split, 1).startingBoard(), -1); // fresh() already owns exact private codec copy
            b = finish(service);
        }
        byte[] before = state(split, b.latestTrainingId());
        var work = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> train(NetworkTrainingState trainer, SelfPlayBatch batch,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                try { assertArrayEquals(before, trainer.encode()); } catch (IOException e) { throw new AssertionError(e); }
                assertEquals(.001, trainer.hyperparameters().learningRate());
                return super.train(trainer, batch, cfg, control, observer);
            }
        };
        var old = config(split, 1);
        var changedInitialRate = new TrainerConfig(split, old.masterSeed(), old.selfPlay(), old.training(), old.validation(),
                1, old.depthChange(), MATE, TrainingArchitecture.BRN, .9);
        try (var service = TrainerService.resume(changedInitialRate, work, ignored -> {})) { c = finish(service); }
        assertEquals(a.latestTrainingId(), c.latestTrainingId()); assertEquals(a.bestId(), c.bestId());
        assertArrayEquals(state(continuous, a.latestTrainingId()), state(split, c.latestTrainingId()));
        assertArrayEquals(before, state(split, b.latestTrainingId()), "Resume never mutates its parent");
    }

    @Test void pendingPersistedCandidateIsValidatedBeforeTrainingContinues() throws Exception {
        String candidate;
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            var trainer = new NetworkTrainingState.Brn(new BrnTrainer(.002));
            String initial = store.initialize(trainer, metadata(0, "")).manifest().id();
            trainer.trainer().train(config(root, 1).startingBoard(), 1);
            candidate = store.publish(trainer, metadata(1, initial)).manifest().id();
        }
        byte[] prior = state(root, candidate);
        var work = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control,
                    Consumer<SelfPlayBatch.Progress> observer) {
                try (var entries = Files.list(root.resolve("validations"))) { assertEquals(1, entries.count()); }
                catch (IOException e) { throw new AssertionError(e); }
                assertInstanceOf(NetworkModel.Brn.class, actor);
                return super.generate(actor, cfg, board, control, observer);
            }
        };
        TrainerSnapshot end;
        try (var service = TrainerService.resume(config(root, 1), work, ignored -> {})) { end = finish(service); }
        assertEquals(1, end.totals().recoveredLifecycles()); assertEquals(2, end.generation());
        assertEquals(3, end.optimizerStep()); assertArrayEquals(prior, state(root, candidate));
    }

    @Test void brnPromotionUsesPersistedPinnedActorsAndNextGenerationUsesLatestTraining() throws Exception {
        var work = new TrainerService.Operations() {
            int validations;
            @Override ValidationResult validate(NetworkModel candidate, NetworkModel incumbent, ValidationConfig cfg,
                    long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
                assertInstanceOf(NetworkModel.Brn.class, candidate); assertInstanceOf(NetworkModel.Brn.class, incumbent);
                try {
                    var c = CheckpointStore.readSnapshot(root, CheckpointInspection.reference(root, "latest-training"));
                    var b = CheckpointStore.readBestSnapshot(root);
                    assertArrayEquals(BrnCodec.encodeModel(((NetworkModel.Brn) c.model()).model()),
                            BrnCodec.encodeModel(((NetworkModel.Brn) candidate).model()));
                    assertArrayEquals(BrnCodec.encodeModel(((NetworkModel.Brn) b.model()).model()),
                            BrnCodec.encodeModel(((NetworkModel.Brn) incumbent).model()));
                } catch (IOException e) { throw new AssertionError(e); }
                return TrainerServiceTest.outcome(cfg, board, history, validations++ == 0);
            }
        };
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config(root, 2), new NetworkTrainingState.Brn(new BrnTrainer(.001)), work, ignored -> {})) {
            end = finish(service);
        }
        assertEquals(1, end.totals().promotions()); assertEquals(1, end.totals().retainedCandidates());
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            assertEquals(end.bestId(), store.load(end.latestTrainingId()).manifest().parentId());
            assertEquals(end.bestId(), store.validationFor(end.latestTrainingId()).orElseThrow().incumbentId());
        }
    }

    @ParameterizedTest @EnumSource(value = TrainerSnapshot.State.class, names = {
            "GENERATING_SELF_PLAY", "TRAINING", "PUBLISHING_CANDIDATE", "VALIDATING", "RECORDING_DECISION"})
    void cleanStopAndResumePreserveTheLastDurableBrnState(TrainerSnapshot.State stopAt) throws Exception {
        var owned = new AtomicReference<TrainerService>();
        Consumer<TrainerSnapshot> stop = snapshot -> { if (snapshot.state() == stopAt) owned.get().stop(); };
        TrainerSnapshot stopped;
        try (var service = TrainerService.fresh(config(root, 1), new NetworkTrainingState.Brn(new BrnTrainer(.001)),
                new TrainerService.Operations(), stop)) {
            owned.set(service); stopped = finish(service);
        }
        byte[] durable = state(root, stopped.latestTrainingId());
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            assertArrayEquals(durable, store.resumeState(stopped.latestTrainingId()).encode());
        }
        TrainerSnapshot resumed;
        try (var service = TrainerService.resume(config(root, 1))) { resumed = finish(service); }
        assertArrayEquals(durable, state(root, stopped.latestTrainingId()));
        assertEquals(stopped.generation() == 1 && stopped.candidateId().isEmpty() ? 1 : stopped.generation() + 1, resumed.generation());
        assertEquals(TrainingArchitecture.BRN, CheckpointStore.readSnapshot(root, resumed.latestTrainingId()).manifest().architecture());
    }
}
