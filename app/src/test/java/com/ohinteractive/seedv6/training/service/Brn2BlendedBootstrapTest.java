package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

/** Small legal mate fixtures exercise real search/data/Adam/publication, never an external store. */
@Timeout(120) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Brn2BlendedBootstrapTest {
    @TempDir static Path temporary;
    Path generator;
    String generatorId;
    @BeforeAll void fixture() throws Exception {
        generator = temporary.resolve("generator");
        try (var store = new CheckpointStore(generator, TrainingArchitecture.NNUE)) {
            generatorId = store.initialize(new NnueTrainer(TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 2, "")).manifest().id();
        }
    }
    TrainerConfig config(Path root) {
        return new TrainerConfig(root, 71, new TrainerConfig.SelfPlay(2, 1, 4, 0, 0, 2, 8, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(2, 0, 0, 2, 1, 8,
                NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)), 1, TrainerConfig.DepthChange.REQUIRE_SAME,
                BrnBootstrapTest.MATE, TrainingArchitecture.BRN2, .001, TrainingSource.bootstrap(generator))
                .withSupervision(BrnSupervision.blended(.75));
    }
    TrainerSnapshot finish(TrainerService service) throws Exception {
        return BrnBootstrapTest.finish(service);
    }
    TrainerSnapshot fail(TrainerService service, String message) throws Exception {
        service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(90)));
        assertEquals(TrainerSnapshot.State.FAILED, service.snapshot().state());
        assertTrue(service.snapshot().failureSummary().contains(message), service.snapshot().failureSummary());
        return service.snapshot();
    }
    NetworkTrainingState student() { return new NetworkTrainingState.Brn2(new Brn2Trainer(.001)); }
    @Test void realGenerationUsesConfiguredTargetsAndPersistsOwnAndComponentLosses() throws Exception {
        Path root = temporary.resolve("complete"); var config = config(root);
        var operations = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,
                    List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config cfg, SelfPlayControl control,
                    Consumer<SelfPlayTraining.Progress> observer, BrnSupervision supervision,
                    ToDoubleFunction<TrajectorySampler.Sample> target) {
                assertEquals(BrnSupervision.blended(.75), supervision);
                var teacher = new NnueEvaluator(com.ohinteractive.seedv6.core.nnue.NnueNetwork.initialized(17));
                for (var sample : samples) {
                    double expected = .25 * sample.target() + .75 * BrnSupervision.teacherValue(teacher, sample);
                    assertEquals(expected, target.applyAsDouble(sample)); assertNotEquals(sample.target(), expected);
                }
                return super.trainBootstrap(state, samples, cfg, control, observer, supervision, target);
            }
        };
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config, student(), operations, s -> {})) { end = finish(service); }
        assertEquals(1, end.totals().completedGenerations()); assertEquals(2, end.optimizerStep());
        var history = new HistoryRepository(root).refresh(); assertTrue(history.warnings().isEmpty());
        assertEquals(1, history.records().size()); var row = history.records().getFirst();
        assertEquals(BrnSupervision.blended(.75), row.bootstrap().supervision());
        assertTrue(Files.readString(root.resolve(HistoryRepository.FILE)).contains("schema=3"));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            var candidate = store.load(end.latestTrainingId());
            var plan = store.bootstrapPlan(candidate.manifest().parentId()).orElseThrow();
            var data = store.bootstrapData(plan).orElseThrow();
            assertEquals(generatorId, plan.generatorId()); assertEquals(BrnSupervision.blended(.75), plan.supervision());
            var teacher = new NnueEvaluator(plan.loadGenerator(root).model().nnue());
            var incumbent = store.load(plan.incumbentId()); var samples = data.partition().heldOut();
            var evidence = store.validationFor(candidate.manifest().id()).orElseThrow().bootstrap();
            assertEquals(row.bootstrap(), evidence);
            assertEquals(HeldOutLoss.compare(candidate.model(), incumbent.model(), samples,
                    sample -> .25 * sample.target() + .75 * BrnSupervision.teacherValue(teacher, sample)), evidence.comparison());
            assertEquals(HeldOutLoss.compare(candidate.model(), incumbent.model(), samples), evidence.wdlLoss());
            assertEquals(HeldOutLoss.compare(candidate.model(), incumbent.model(), samples,
                    sample -> BrnSupervision.teacherValue(teacher, sample)), evidence.teacherLoss());
            assertEquals(evidence.comparison().decision(), row.decision());
            assertEquals(row.promoted() ? row.candidate() : row.incumbent(), row.resultingBest());
            assertEquals(2, candidate.manifest().architecture().schemaVersion());
        }
    }
    @Test void partialUpdateReloadReusesDataAndExactOptimizerAndRejectsChangedWeightBeforeMutation() throws Exception {
        Path continuous = temporary.resolve("continuous"), split = temporary.resolve("split");
        TrainerSnapshot expected;
        try (var service = TrainerService.fresh(config(continuous), new Brn2Trainer(.001))) { expected = finish(service); }
        var owner = new AtomicReference<TrainerService>();
        var operations = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,
                    List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config cfg, SelfPlayControl control,
                    Consumer<SelfPlayTraining.Progress> observer, BrnSupervision supervision,
                    ToDoubleFunction<TrajectorySampler.Sample> target) {
                return super.trainBootstrap(state, samples, cfg, control, p -> { observer.accept(p); owner.get().stop(); }, supervision, target);
            }
        };
        TrainerSnapshot stopped;
        try (var service = TrainerService.fresh(config(split), student(), operations, s -> {})) {
            owner.set(service); stopped = finish(service);
        }
        assertEquals(1, stopped.optimizerStep()); assertEquals("", stopped.candidateId());
        byte[] attempt = Files.readAllBytes(split.resolve(GenerationAttempt.FILE));
        for (var incompatible : List.of(BrnSupervision.WDL, BrnSupervision.blended(.5), BrnSupervision.blended(1))) {
            try (var service = TrainerService.resume(config(split).withSupervision(incompatible))) { fail(service, "Supervision differs"); }
            assertArrayEquals(attempt, Files.readAllBytes(split.resolve(GenerationAttempt.FILE)));
            assertFalse(Files.exists(split.resolve("restarted-generations")));
        }
        var replay = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl c, Consumer<SelfPlayBatch.Progress> o) {
                throw new AssertionError("Durable data must be reused");
            }
        };
        TrainerSnapshot resumed;
        try (var service = TrainerService.resume(config(split).withSource(null).withSupervision(null), replay, s -> {})) { resumed = finish(service); }
        assertEquals(expected.latestTrainingId(), resumed.latestTrainingId());
        assertArrayEquals(BrnBootstrapTest.state(continuous, expected.latestTrainingId()), BrnBootstrapTest.state(split, resumed.latestTrainingId()));
        assertEquals(BrnSupervision.blended(.75), new HistoryRepository(split).refresh().records().getFirst().bootstrap().supervision());
    }
    @Test void generationPinSurvivesChangedBestAndNextGenerationSelectsNewBest() throws Exception {
        Path root = temporary.resolve("pin"), external = temporary.resolve("changing-generator");
        String original, replacement;
        try (var store = new CheckpointStore(external, TrainingArchitecture.NNUE)) {
            original = store.initialize(new NnueTrainer(TrainableNnue.initialized(31)), new CheckpointManifest.Metadata(0, 2, "")).manifest().id();
        }
        var cfg = config(root).withSource(TrainingSource.bootstrap(external)); var owner = new AtomicReference<TrainerService>();
        TrainerSnapshot stopped;
        try (var service = TrainerService.fresh(cfg, student(), new TrainerService.Operations(), s -> {
            if (s.state() == TrainerSnapshot.State.GENERATING_SELF_PLAY) owner.get().stop();
        })) { owner.set(service); stopped = finish(service); }
        var pinned = CheckpointInspection.bootstrapPlan(root, stopped.latestTrainingId()); assertEquals(original, pinned.generatorId());
        // Real acceptance evidence advances external Best, retaining the old checkpoint payload.
        try (var store = new CheckpointStore(external, TrainingArchitecture.NNUE)) {
            var candidate = store.publish(new NnueTrainer(TrainableNnue.initialized(32)), new CheckpointManifest.Metadata(1, 2, original));
            replacement = candidate.manifest().id();

        }
        // The existing acceptance fixture builds valid colour/termination aggregates for the store API.
        promoteFixture(external, original, replacement, cfg);
        TrainerSnapshot resumed;
        try (var service = TrainerService.resume(cfg.withSource(null).withSupervision(null))) { resumed = finish(service); }
        assertEquals(original, resumed.bootstrapValidation().orElseThrow().evidence().generatorId());
        try (var service = TrainerService.resume(cfg.withSource(null).withSupervision(null))) { finish(service); }
        var rows = new HistoryRepository(root).refresh().records(); assertEquals(2, rows.size());
        assertEquals(List.of(original, replacement), rows.stream().map(r -> r.bootstrap().generatorId()).toList());
    }
    private void promoteFixture(Path external, String original, String replacement, TrainerConfig cfg) throws Exception {
        var result = TrainerServiceTest.outcome(cfg.validation(1), cfg.startingBoard(),
                com.ohinteractive.seedv6.rules.GameHistory.initial(cfg.startingBoard()), true);
        try (var store = new CheckpointStore(external, TrainingArchitecture.NNUE)) {
            var record = store.recordValidation(replacement, original, result, new PromotionPolicy(1, .9, 0));
            store.completePromotion(record.id());
        }
    }
    @Test void missingPinnedTeacherFailsClosedEvenWhenSamplesExist() throws Exception {
        Path root = temporary.resolve("missing"); var owner = new AtomicReference<TrainerService>();
        Path privateGenerator = temporary.resolve("missing-generator");
        try (var store = new CheckpointStore(privateGenerator, TrainingArchitecture.NNUE)) {
            store.initialize(new NnueTrainer(TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 2, ""));
        }
        var cfg = config(root).withSource(TrainingSource.bootstrap(privateGenerator));
        TrainerSnapshot stopped;
        try (var service = TrainerService.fresh(cfg, student(), new TrainerService.Operations(), s -> {
            if (s.state() == TrainerSnapshot.State.TRAINING) owner.get().stop();
        })) { owner.set(service); stopped = finish(service); }
        var plan = CheckpointInspection.bootstrapPlan(root, stopped.latestTrainingId());
        assertNotNull(CheckpointInspection.bootstrapData(root, plan));
        Path checkpoint = privateGenerator.resolve("checkpoints").resolve(plan.generatorId());
        Path away = privateGenerator.resolve("temporarily-unavailable"); Files.move(checkpoint, away);
        try {
            try (var service = TrainerService.resume(cfg.withSupervision(null))) { fail(service, "Pinned NNUE generator"); }
            assertEquals(stopped.latestTrainingId(), CheckpointInspection.reference(root, "latest-training"));
        } finally { Files.move(away, checkpoint); }
        try (var service = TrainerService.resume(cfg.withSupervision(null))) { finish(service); }
    }
    @Test void legacyWdlLineageCannotBeConvertedAndMissingBlendedMetadataCannotBecomeWdl() throws Exception {
        Path legacy = temporary.resolve("legacy"), blended = temporary.resolve("metadata");
        try (var store = new CheckpointStore(legacy, TrainingArchitecture.BRN2)) {
            store.writeTrainingSource(TrainingSource.bootstrap(generator));
            store.initialize(student(), new CheckpointManifest.Metadata(0, 2, ""));
        }
        assertTrue(CheckpointStore.readBrnSupervision(legacy).isEmpty());
        try (var service = TrainerService.resume(config(legacy))) { fail(service, "Supervision differs"); }
        try (var service = TrainerService.resume(config(legacy).withSupervision(null))) { finish(service); }
        assertTrue(CheckpointStore.readBrnSupervision(legacy).isEmpty());
        assertEquals(BrnSupervision.WDL, new HistoryRepository(legacy).refresh().records().getFirst().bootstrap().supervision());
        try (var service = TrainerService.fresh(config(blended), new Brn2Trainer(.001))) { finish(service); }
        Files.move(blended.resolve(CheckpointStore.BRN_SUPERVISION_FILE), blended.resolve("saved-supervision"));
        try (var service = TrainerService.resume(config(blended).withSupervision(null))) { fail(service, "Supervision differs"); }
    }
    @Test void publishedCandidateRecoveryUsesPinnedObjectiveAndDrainsDecisionOnStop() throws Exception {
        Path root = temporary.resolve("pending-candidate");
        var interrupted = new TrainerService.Operations() {
            @Override CheckpointStore.Checkpoint publish(CheckpointStore store, NetworkTrainingState state, CheckpointManifest.Metadata metadata)
                    throws java.io.IOException {
                super.publish(store, state, metadata);
                throw new java.io.IOException("Injected interruption after Candidate publication");
            }
        };
        try (var service = TrainerService.fresh(config(root), student(), interrupted, s -> {})) { fail(service, "Injected interruption"); }
        String candidate = CheckpointInspection.reference(root, "latest-training");
        var owner = new AtomicReference<TrainerService>();
        var replay = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl c, Consumer<SelfPlayBatch.Progress> o) {
                throw new AssertionError("Pending decision must settle without new games");
            }
        };
        TrainerSnapshot resumed;
        try (var service = TrainerService.resume(config(root).withSource(null).withSupervision(null), replay, s -> {
            if (s.state() == TrainerSnapshot.State.RECORDING_DECISION) owner.get().stop();
        })) { owner.set(service); resumed = finish(service); }
        assertEquals(candidate, resumed.latestTrainingId()); assertEquals(1, resumed.totals().recoveredLifecycles());
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            var evidence = store.validationFor(candidate).orElseThrow().bootstrap();
            var model = store.load(candidate); var plan = store.bootstrapPlan(model.manifest().parentId()).orElseThrow();
            var data = store.bootstrapData(plan).orElseThrow();
            var teacher = new NnueEvaluator(plan.loadGenerator(root).model().nnue());
            assertEquals(HeldOutLoss.compare(model.model(), store.load(plan.incumbentId()).model(), data.partition().heldOut(),
                    sample -> .25 * sample.target() + .75 * BrnSupervision.teacherValue(teacher, sample)), evidence.comparison());
            assertEquals(BrnSupervision.blended(.75), evidence.supervision());
        }
    }
}
