package com.ohinteractive.seedv6.training.service;

import java.io.*;
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
import com.ohinteractive.seedv6.core.brn1.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrnBootstrapTest {
    @TempDir static Path temporary;
    Path generator;
    String generatorId;
    static final String MATE = "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1";
    @BeforeAll void nnueFixture() throws Exception {
        generator = temporary.resolve("generator");
        try (var store = new CheckpointStore(generator, TrainingArchitecture.NNUE)) {
            generatorId = store.initialize(new NnueTrainer(TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
    }
    static NetworkTrainingState student(TrainingArchitecture architecture) {
        return switch (architecture) {
            case BRN -> new NetworkTrainingState.Brn(new BrnTrainer(.001));
            case BRN1 -> new NetworkTrainingState.Brn1(new Brn1Trainer(.001));
            case BRN2 -> new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
            default -> throw new IllegalArgumentException();
        };
    }
    TrainerConfig config(Path root, TrainingArchitecture architecture, int generations) {
        return new TrainerConfig(root, 71, new TrainerConfig.SelfPlay(1, 1, 8, 0, 0, 4, 8, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(2, 0, 0, 1, 1, 8,
                NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)), generations, TrainerConfig.DepthChange.REQUIRE_SAME,
                MATE, architecture, .001, TrainingSource.bootstrap(generator));
    }
    static TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(100)));
        assertEquals(TrainerSnapshot.State.STOPPED, service.snapshot().state(), service.failure().map(Throwable::toString).orElse(""));
        assertEquals("", service.historyWarning()); return service.snapshot();
    }
    static byte[] state(Path root, String id) throws IOException {
        return Files.readAllBytes(root.resolve("checkpoints").resolve(id).resolve("training.state"));
    }
    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"BRN", "BRN1", "BRN2"})
    void realTwoGenerationsOnlySearchWithPinnedNnueTrainOnlyTrainingPartitionAndCompareSameHoldout(TrainingArchitecture architecture) throws Exception {
        Path root = temporary.resolve("lifecycle-" + architecture);
        var work = new TrainerService.Operations() {
            List<TrajectorySampler.Sample> trained;
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                assertInstanceOf(NetworkModel.Nnue.class, actor);
                var batch = super.generate(actor, cfg, board, control, observer);
                assertInstanceOf(NetworkModel.Nnue.class, batch.generationModel());
                assertEquals(8, batch.statistics().completedGames());
                batch.samples().forEach(sample -> assertEquals(1, sample.target(), "White mate: each sampled white-to-move target is +1"));
                return batch;
            }
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState trainer, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                assertEquals(architecture, trainer.architecture()); trained = samples;
                long before = trainer.step();
                var result = super.trainBootstrap(trainer, samples, cfg, control, observer);
                assertEquals(samples.size(), trainer.step() - before); return result;
            }
            @Override HeldOutLoss.Comparison validateBootstrap(NetworkModel candidate, NetworkModel best, List<TrajectorySampler.Sample> samples) {
                assertEquals(architecture, candidate.architecture()); assertEquals(architecture, best.architecture());
                for (var sample : samples) assertFalse(trained.contains(sample), "Held-out examples never enter updates");
                return super.validateBootstrap(candidate, best, samples);
            }
            @Override ValidationResult validate(NetworkModel candidate, NetworkModel best, ValidationConfig cfg, long[] board,
                    GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
                throw new AssertionError("BRN game search is forbidden during bootstrap validation");
            }
        };
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config(root, architecture, 2), student(architecture), work, s -> {})) { end = finish(service); }
        assertEquals(2, end.totals().completedGenerations()); assertEquals(12, end.optimizerStep());
        assertTrue(end.validation().isEmpty()); assertTrue(end.assessment().isEmpty());
        assertEquals(TrainingSource.bootstrap(generator), CheckpointStore.readTrainingSource(root).orElseThrow());
        var history = new HistoryRepository(root).refresh(); assertTrue(history.warnings().isEmpty()); assertEquals(2, history.records().size());
        try (var store = new CheckpointStore(root, architecture)) {
            for (var record : history.records()) {
                assertEquals("Bootstrap WDL loss", record.validationKind()); assertNull(record.score()); assertEquals(0, record.validPairs());
                var evidence = record.bootstrap(); assertNotNull(evidence); assertEquals(generatorId, evidence.generatorId());
                assertEquals(6, evidence.trainingSamples()); assertEquals(2, evidence.comparison().samples());
                assertEquals(evidence.comparison().decision(), record.decision());
                var candidate = store.load(record.candidate()); assertEquals(architecture, candidate.manifest().architecture());
                assertFalse(Files.exists(root.resolve("checkpoints").resolve(record.candidate()).resolve("network.nnue")));
                assertEquals(evidence, store.validationFor(record.candidate()).orElseThrow().bootstrap());
                var plan = store.bootstrapPlan(candidate.manifest().parentId()).orElseThrow();
                var data = store.bootstrapData(plan).orElseThrow();
                assertEquals(evidence.dataHash(), data.hash());
                assertEquals(6, data.partition().training().size()); assertEquals(2, data.partition().heldOutGames().size());
            }
            assertEquals(architecture, store.recover().best().orElseThrow().manifest().architecture());
        }
        assertEquals(generatorId, CheckpointStore.readBestSnapshot(generator).manifest().id());
    }
    @Test void stopAfterUpdateRestartUsesDurableDataAndExactOptimizerWithoutRegenerating() throws Exception {
        Path continuous = temporary.resolve("continuous"), split = temporary.resolve("split");
        TrainerSnapshot expected;
        try (var service = TrainerService.fresh(config(continuous, TrainingArchitecture.BRN2, 1), new Brn2Trainer(.001))) { expected = finish(service); }
        var owned = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState trainer, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainBootstrap(trainer, samples, cfg, control, progress -> { observer.accept(progress); owned.get().stop(); });
            }
        };
        TrainerSnapshot stopped;
        try (var service = TrainerService.fresh(config(split, TrainingArchitecture.BRN2, 1), student(TrainingArchitecture.BRN2), work, s -> {})) {
            owned.set(service); stopped = finish(service);
        }
        assertEquals(1, stopped.optimizerStep()); assertEquals("", stopped.candidateId());
        assertArrayEquals(student(TrainingArchitecture.BRN2).encode(), state(split, stopped.latestTrainingId()));
        var replay = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl c, Consumer<SelfPlayBatch.Progress> o) {
                throw new AssertionError("Durable data must be reused");
            }
        };
        TrainerSnapshot resumed;
        try (var service = TrainerService.resume(config(split, TrainingArchitecture.BRN2, 1).withSource(null), replay, s -> {})) { resumed = finish(service); }
        assertEquals(expected.latestTrainingId(), resumed.latestTrainingId());
        assertArrayEquals(state(continuous, expected.latestTrainingId()), state(split, resumed.latestTrainingId()));
    }
    @ParameterizedTest @EnumSource(value = TrainerSnapshot.State.class, names = {"GENERATING_SELF_PLAY", "TRAINING", "PUBLISHING_CANDIDATE", "VALIDATING", "RECORDING_DECISION"})
    void stopAtEveryBoundaryKeepsModeAndResumes(TrainerSnapshot.State phase) throws Exception {
        Path root = temporary.resolve("stop-" + phase); var owned = new AtomicReference<TrainerService>();
        TrainerSnapshot stopped;
        try (var service = TrainerService.fresh(config(root, TrainingArchitecture.BRN, 1), student(TrainingArchitecture.BRN),
                new TrainerService.Operations(), s -> { if (s.state() == phase) owned.get().stop(); })) {
            owned.set(service); stopped = finish(service);
        }
        byte[] prior = state(root, stopped.latestTrainingId());
        try (var service = TrainerService.resume(config(root, TrainingArchitecture.BRN, 1).withSource(null))) {
            var resumed = finish(service); assertTrue(resumed.bootstrapValidation().isPresent());
        }
        assertArrayEquals(prior, state(root, stopped.latestTrainingId()));
        assertEquals(TrainingSource.bootstrap(generator), CheckpointStore.readTrainingSource(root).orElseThrow());
    }
    @Test void undecidedCandidateRestartsFromSettledParentOnDeliberateSelfPlayTransition() throws Exception {
        Path root = temporary.resolve("recover-decision");
        var crash = new TrainerService.Operations() {
            @Override HeldOutLoss.Comparison validateBootstrap(NetworkModel c, NetworkModel b, List<TrajectorySampler.Sample> samples) {
                throw new IllegalStateException("simulated process failure after publication");
            }
        };
        TrainerSnapshot stopped;
        try (var service = TrainerService.fresh(config(root, TrainingArchitecture.BRN, 1), student(TrainingArchitecture.BRN), crash, s -> {})) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(100))); stopped = service.snapshot(); assertTrue(stopped.failed());
        }
        assertFalse(stopped.candidateId().isEmpty());
        try (var service = TrainerService.resume(config(root, TrainingArchitecture.BRN, 1).withSource(TrainingSource.SELF_PLAY))) {
            var end = finish(service); assertTrue(end.bootstrapValidation().isEmpty()); assertEquals(2, end.validation().orElseThrow().validPairs());
            assertEquals(0, end.totals().recoveredLifecycles()); assertEquals(1, end.generation());
            assertTrue(service.lifecycleNotice().contains("Restarted unfinished generation"));
        }
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            assertTrue(store.validationFor(stopped.candidateId()).isEmpty());
            assertFalse(Files.exists(root.resolve("checkpoints").resolve(stopped.candidateId())));
            assertEquals(TrainingSource.SELF_PLAY, CheckpointStore.readTrainingSource(root).orElseThrow());
            assertNull(store.validationFor(store.recover().latestTraining().orElseThrow().manifest().id()).orElseThrow().bootstrap());
        }
    }
    @Test void unfinishedGenerationRestartsForChangedModeAndSource() throws Exception {
        Path root = temporary.resolve("locked"); var owned = new AtomicReference<TrainerService>();
        try (var service = TrainerService.fresh(config(root, TrainingArchitecture.BRN, 1), student(TrainingArchitecture.BRN),
                new TrainerService.Operations(), s -> { if (s.state() == TrainerSnapshot.State.GENERATING_SELF_PLAY) owned.get().stop(); })) {
            owned.set(service); finish(service);
        }
        try (var service = TrainerService.resume(config(root, TrainingArchitecture.BRN, 1).withSource(TrainingSource.SELF_PLAY))) {
            var end = finish(service); assertEquals(1, end.generation());
            assertTrue(service.lifecycleNotice().contains("Restarted unfinished generation"));
        }
        assertEquals(TrainingSource.SELF_PLAY, CheckpointStore.readTrainingSource(root).orElseThrow());
    }
    @Test void invalidSourcesRejectBeforeStudentMutationAndPathsStayIndependent() throws Exception {
        Path student = temporary.resolve("invalid-student");
        for (TrainingSource source : List.of(new TrainingSource(TrainingSource.Mode.NNUE_BOOTSTRAP, ""),
                TrainingSource.bootstrap(temporary.resolve("missing")), TrainingSource.bootstrap(generator))) {
            Path root = source.generatorStore().equals(generator.toString()) ? generator.resolve("nested-student") : student;
            try (var service = TrainerService.fresh(config(root, TrainingArchitecture.BRN, 1).withSource(source), new BrnTrainer(.001))) {
                service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(100))); assertTrue(service.snapshot().failed());
            }
            assertFalse(Files.exists(root));
        }
        Path brn = temporary.resolve("not-nnue");
        try (var store = new CheckpointStore(brn, TrainingArchitecture.BRN)) { store.initialize(student(TrainingArchitecture.BRN), new CheckpointManifest.Metadata(0, 1, "")); }
        var error = assertThrows(IOException.class, () -> TrainingSource.bootstrap(brn).loadBest(student));
        assertTrue(error.getMessage().contains("architecture mismatch"));
        Path empty = Files.createDirectory(temporary.resolve("empty-source"));
        assertTrue(assertThrows(IOException.class, () -> TrainingSource.bootstrap(empty).loadBest(student))
                .getMessage().contains("no accepted Best"));
        try (var paths = Files.list(empty)) { assertEquals(0, paths.count(), "Invalid selection must not create payload.lock"); }
        Path unrelated = Files.createDirectory(temporary.resolve("unrelated-source"));
        Files.writeString(unrelated.resolve("notes.txt"), "preserved");
        assertThrows(IOException.class, () -> TrainingSource.bootstrap(unrelated).loadBest(student));
        try (var paths = Files.list(unrelated)) { assertEquals(1, paths.count()); }
        assertEquals("preserved", Files.readString(unrelated.resolve("notes.txt")));
        assertThrows(IllegalArgumentException.class, () -> config(student, TrainingArchitecture.NNUE, 1));
    }

    @Test void generationPinsBestEvenWhenGeneratorStorePromotesAndNextGenerationSelectsNewBest() throws Exception {
        Path ownGenerator = temporary.resolve("changing-generator"), root = temporary.resolve("pin-student");
        String original;
        try (var store = new CheckpointStore(ownGenerator, TrainingArchitecture.NNUE)) {
            original = store.initialize(new NnueTrainer(TrainableNnue.initialized(81)), new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        var replacement = new AtomicReference<String>();
        var work = new TrainerService.Operations() {
            int calls;
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                try {
                    var pinned = CheckpointStore.readSnapshot(ownGenerator, calls == 0 ? original : replacement.get());
                    assertEquals(digest(pinned.model()), digest(actor));
                    if (calls++ == 0) {
                        try (var store = new CheckpointStore(ownGenerator, TrainingArchitecture.NNUE)) {
                            var next = store.publish(new NnueTrainer(TrainableNnue.initialized(82)), new CheckpointManifest.Metadata(1, 1, original));
                            var validation = config(root, TrainingArchitecture.BRN, 1).validation(1);
                            var policy = new PromotionPolicy(1, .9, 0);
                            var evidence = store.recordValidation(next.manifest().id(), original,
                                    TrainerServiceTest.outcome(validation, board, GameHistory.initial(board), true), policy);
                            store.completePromotion(evidence.id()); replacement.set(next.manifest().id());
                        }
                        assertNotEquals(original, CheckpointStore.readBestSnapshot(ownGenerator).manifest().id());
                        assertEquals(digest(pinned.model()), digest(actor), "Already loaded immutable actor remains pinned");
                    }
                } catch (IOException e) { throw new UncheckedIOException(e); }
                return super.generate(actor, cfg, board, control, observer);
            }
        };
        try (var service = TrainerService.fresh(config(root, TrainingArchitecture.BRN, 2).withSource(TrainingSource.bootstrap(ownGenerator)),
                student(TrainingArchitecture.BRN), work, s -> {})) { finish(service); }
        var records = new HistoryRepository(root).refresh().records();
        assertEquals(original, records.get(0).bootstrap().generatorId());
        assertEquals(replacement.get(), records.get(1).bootstrap().generatorId());
    }
    static String digest(NetworkModel model) throws IOException {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            model.write(new java.security.DigestOutputStream(OutputStream.nullOutputStream(), digest));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    @Test void insufficientCompletedDataPreservesFailedAttemptAndAllowsCorrectedBoundsBeforeAnyUpdates() throws Exception {
        Path root = temporary.resolve("insufficient");
        var c = config(root, TrainingArchitecture.BRN, 1);
        var small = new TrainerConfig(root, c.masterSeed(), new TrainerConfig.SelfPlay(1, 1, 3, 0, 0, 4, 8, NnueScoreMapping.V1),
                c.training(), c.validation(), 1, c.depthChange(), MATE, c.architecture(), c.brnLearningRate(), c.source());
        String parent;
        try (var service = TrainerService.fresh(small, new BrnTrainer(.001))) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(100)));
            var end = service.snapshot(); assertTrue(end.failed()); assertTrue(end.failureSummary().contains("pin was archived"));
            assertEquals(0, end.optimizerStep()); assertEquals("", end.candidateId()); parent = end.latestTrainingId();
        }
        try (var paths = Files.list(root.resolve("bootstrap"))) {
            assertEquals(1, paths.filter(path -> path.toString().endsWith(".insufficient-plan")).count());
        }
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) { assertTrue(store.bootstrapPlan(parent).isEmpty()); }
        try (var service = TrainerService.resume(c.withSource(null))) { assertEquals(6, finish(service).optimizerStep()); }
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.CsvSource({"0.1,0.2,true", "0.2,0.1,false", "0.1,0.1,false"})
    void durablePromotionUsesStrictLowerLossIncludingDeterministicTies(double candidateLoss, double bestLoss, boolean promote) throws Exception {
        Path root = temporary.resolve("decision-" + candidateLoss + "-" + bestLoss);
        var work = new TrainerService.Operations() {
            @Override HeldOutLoss.Comparison validateBootstrap(NetworkModel c, NetworkModel b, List<TrajectorySampler.Sample> samples) {
                return new HeldOutLoss.Comparison(samples.size(), candidateLoss, bestLoss);
            }
        };
        try (var service = TrainerService.fresh(config(root, TrainingArchitecture.BRN, 1), student(TrainingArchitecture.BRN), work, s -> {})) {
            var end = finish(service); assertEquals(promote, end.bestId().equals(end.candidateId()));
            assertEquals(promote ? 1 : 0, end.totals().promotions());
        }
        var record = new HistoryRepository(root).refresh().records().getFirst();
        assertEquals(promote, record.promoted()); assertEquals(candidateLoss, record.bootstrap().comparison().candidateLoss());
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            var decision = store.validationFor(record.candidate()).orElseThrow();
            assertEquals(record.resultingBest(), CandidateLifecycle.completeDecision(store, decision).references().best().orElseThrow().manifest().id());
        }
    }
}
