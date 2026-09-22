package com.ohinteractive.seedv6.training.service;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.core.brn.BrnTrainer;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoppedReconfigurationTest {
    @TempDir static Path temporary;
    Path generator, alternate;
    @BeforeAll void generators() throws Exception {
        generator = temporary.resolve("generator"); alternate = temporary.resolve("alternate");
        for (Path path : List.of(generator, alternate)) try (var store = new CheckpointStore(path, TrainingArchitecture.NNUE)) {
            store.initialize(new NnueTrainer(TrainableNnue.initialized(path.equals(generator) ? 17 : 18)), new CheckpointManifest.Metadata(0, 1, ""));
        }
    }
    TrainerConfig config(Path root, TrainingArchitecture architecture, TrainingSource source) {
        var mapping = com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1;
        return new TrainerConfig(root, 71, new TrainerConfig.SelfPlay(1, 1, 8, 0, 0, 4, 8, mapping),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(2, 0, 0, 1, 1, 8, mapping,
                new PromotionPolicy(1, .9, 0)), 1, TrainerConfig.DepthChange.EXPLICITLY_ALLOW,
                BrnBootstrapTest.MATE, architecture, .001, source);
    }
    NetworkTrainingState initial(TrainingArchitecture architecture) {
        return architecture == TrainingArchitecture.NNUE ? new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(17)))
                : BrnBootstrapTest.student(architecture);
    }
    TrainerSnapshot run(TrainerService service) throws Exception { return BrnBootstrapTest.finish(service); }
    TrainerSnapshot stop(TrainerConfig config, TrainerSnapshot.State phase, boolean fresh) throws Exception {
        var owned = new AtomicReference<TrainerService>();
        Consumer<TrainerSnapshot> observer = s -> { if (s.state() == phase) owned.get().stop(); };
        try (var service = fresh ? TrainerService.fresh(config, initial(config.architecture()), new TrainerService.Operations(), observer)
                : TrainerService.resume(config, new TrainerService.Operations(), observer)) { owned.set(service); return run(service); }
    }
    enum Change { DEPTH, THREADS, GAMES, SAMPLES, PLIES, SEED, SHUFFLE, GENERATOR, TO_SELF_PLAY, VALIDATION_PAIRS }
    TrainerConfig changed(TrainerConfig c, Change change) {
        var s = c.selfPlay(); var v = c.validation();
        return new TrainerConfig(c.checkpointRoot(), c.masterSeed() + (change == Change.SEED ? 1 : 0),
                new TrainerConfig.SelfPlay(change == Change.DEPTH ? 2 : s.depth(), change == Change.THREADS ? 2 : s.threads(),
                        change == Change.GAMES ? 5 : s.games(), s.minimumOpeningPlies(), s.maximumOpeningPlies(),
                        change == Change.SAMPLES ? 2 : s.maximumSamplesPerGame(), change == Change.PLIES ? 9 : s.maximumPlies(), s.scoreMapping()),
                new TrainerConfig.Training(1, 1, change != Change.SHUFFLE),
                change == Change.VALIDATION_PAIRS ? new TrainerConfig.Validation(3, 0, 0, 1, 1, 8, v.scoreMapping(), v.policy()) : v,
                c.maximumGenerations(), c.depthChange(), c.startingFen(), c.architecture(), c.brnLearningRate(),
                change == Change.GENERATOR ? TrainingSource.bootstrap(alternate) : change == Change.TO_SELF_PLAY ? TrainingSource.SELF_PLAY : c.source());
    }
    @ParameterizedTest @EnumSource(value = Change.class, names = {"VALIDATION_PAIRS"}, mode = EnumSource.Mode.EXCLUDE)
    void changedBootstrapSettingsArchiveOnlyUnfinishedWorkAndRestartSameGeneration(Change change) throws Exception {
        Path root = temporary.resolve("change-" + change);
        var original = config(root, TrainingArchitecture.BRN, TrainingSource.bootstrap(generator));
        var stopped = stop(original, TrainerSnapshot.State.TRAINING, true);
        byte[] parent = BrnBootstrapTest.state(root, stopped.latestTrainingId());
        var next = changed(original, change); var owned = new AtomicReference<TrainerService>(); var calls = new AtomicInteger();
        var work = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                calls.incrementAndGet(); assertEquals(next.selfPlay(1), cfg);
                assertEquals(next.source().bootstrap() ? TrainingArchitecture.NNUE : TrainingArchitecture.BRN, actor.architecture());
                return super.generate(actor, cfg, board, control, observer);
            }
        };
        try (var service = TrainerService.resume(next, work, s -> { if (s.state() == TrainerSnapshot.State.TRAINING) owned.get().stop(); })) {
            owned.set(service); var end = run(service);
            assertEquals(1, end.generation()); assertEquals(0, end.optimizerStep());
            assertEquals(stopped.latestTrainingId(), end.latestTrainingId()); assertEquals(stopped.bestId(), end.bestId());
            assertTrue(service.lifecycleNotice().contains("Restarted unfinished generation 1"));
        }
        assertEquals(1, calls.get()); assertArrayEquals(parent, BrnBootstrapTest.state(root, stopped.latestTrainingId()));
        assertTrue(new HistoryRepository(root).refresh().records().isEmpty());
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            assertTrue(store.generationAttempt().orElseThrow().matches(next, next.source()));
            if (next.source().bootstrap()) assertEquals(next.source(), store.bootstrapPlan(stopped.latestTrainingId()).orElseThrow().source());
            else assertTrue(store.bootstrapPlan(stopped.latestTrainingId()).isEmpty());
        }
        try (var paths = Files.walk(root.resolve("restarted-generations"))) {
            assertTrue(paths.anyMatch(path -> path.toString().endsWith(".data")));
        }
    }
    @Test void unchangedSearchStopKeepsTheExactPinAndGeneration() throws Exception {
        Path root = temporary.resolve("unchanged"); var c = config(root, TrainingArchitecture.BRN, TrainingSource.bootstrap(generator));
        var first = stop(c, TrainerSnapshot.State.GENERATING_SELF_PLAY, true);
        Path pin = root.resolve("bootstrap").resolve(first.latestTrainingId() + ".plan"); byte[] bytes = Files.readAllBytes(pin);
        var next = stop(c.withSource(null), TrainerSnapshot.State.TRAINING, false);
        assertEquals(first.generation(), next.generation()); assertArrayEquals(bytes, Files.readAllBytes(pin));
        assertFalse(Files.exists(root.resolve("restarted-generations")));
    }
    @Test void selfPlayToBootstrapRestartsWithoutInvokingTheBrnGenerator() throws Exception {
        Path root = temporary.resolve("to-bootstrap"); var c = config(root, TrainingArchitecture.BRN, TrainingSource.SELF_PLAY);
        stop(c, TrainerSnapshot.State.TRAINING, true);
        var next = c.withSource(TrainingSource.bootstrap(generator));
        var work = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                assertInstanceOf(NetworkModel.Nnue.class, actor); return super.generate(actor, cfg, board, control, observer);
            }
        };
        try (var service = TrainerService.resume(next, work, s -> {})) {
            var end = run(service); assertEquals(1, end.generation()); assertTrue(end.bootstrapValidation().isPresent());
        }
    }
    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"BRN", "BRN1", "BRN2"})
    void partialUpdatesAreDiscardedAndNewCandidateStartsFromSettledOptimizer(TrainingArchitecture architecture) throws Exception {
        Path root = temporary.resolve("updates-" + architecture); var c = config(root, architecture, TrainingSource.bootstrap(generator));
        var owned = new AtomicReference<TrainerService>();
        var partial = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainBootstrap(state, samples, cfg, control, p -> { observer.accept(p); owned.get().stop(); });
            }
        };
        TrainerSnapshot first;
        try (var service = TrainerService.fresh(c, initial(architecture), partial, s -> {})) { owned.set(service); first = run(service); }
        assertEquals(1, first.optimizerStep()); byte[] settled = BrnBootstrapTest.state(root, first.latestTrainingId());
        var verify = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                try { assertArrayEquals(settled, state.encode()); } catch (IOException e) { throw new UncheckedIOException(e); }
                return super.trainBootstrap(state, samples, cfg, control, observer);
            }
        };
        try (var service = TrainerService.resume(changed(c, Change.GAMES), verify, s -> {})) {
            var end = run(service); assertEquals(1, end.generation()); assertEquals(3, end.optimizerStep());
        }
        assertArrayEquals(settled, BrnBootstrapTest.state(root, first.latestTrainingId()));
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void undecidedCandidateIsArchivedWhileEarlierSettledHistoryAndBestStayAuthoritative(boolean retain) throws Exception {
        Path root = temporary.resolve("candidate-" + retain); var c = config(root, TrainingArchitecture.BRN, TrainingSource.bootstrap(generator));
        TrainerSnapshot settled;
        var decision = new TrainerService.Operations() {
            @Override HeldOutLoss.Comparison validateBootstrap(NetworkModel c, NetworkModel b, List<TrajectorySampler.Sample> samples) {
                return retain ? new HeldOutLoss.Comparison(samples.size(), .2, .1) : super.validateBootstrap(c, b, samples);
            }
        };
        try (var service = TrainerService.fresh(c, initial(c.architecture()), decision, s -> {})) { settled = run(service); }
        Path history = root.resolve(HistoryRepository.FILE); byte[] previousHistory = Files.readAllBytes(history);
        byte[] previousState = BrnBootstrapTest.state(root, settled.latestTrainingId());
        var crash = new TrainerService.Operations() {
            @Override HeldOutLoss.Comparison validateBootstrap(NetworkModel c, NetworkModel b, List<TrajectorySampler.Sample> samples) {
                throw new IllegalStateException("interrupted before durable decision");
            }
        };
        TrainerSnapshot pending;
        try (var service = TrainerService.resume(c, crash, s -> {})) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(100))); pending = service.snapshot(); assertTrue(pending.failed());
        }
        String abandoned = pending.candidateId(); assertEquals(2, pending.generation());
        var owned = new AtomicReference<TrainerService>();
        try (var service = TrainerService.resume(changed(c, Change.GAMES), new TrainerService.Operations(), s -> {
            if (s.state() == TrainerSnapshot.State.GENERATING_SELF_PLAY) owned.get().stop();
        })) {
            owned.set(service); var end = run(service);
            assertEquals(2, end.generation()); assertEquals(settled.latestTrainingId(), end.latestTrainingId()); assertEquals(settled.bestId(), end.bestId());
        }
        assertFalse(Files.exists(root.resolve("checkpoints").resolve(abandoned)));
        assertArrayEquals(previousHistory, Files.readAllBytes(history)); assertArrayEquals(previousState, BrnBootstrapTest.state(root, settled.latestTrainingId()));
        try (var service = TrainerService.resume(changed(c, Change.GAMES))) { assertEquals(2, run(service).generation()); }
        var records = new HistoryRepository(root).refresh().records(); assertEquals(List.of(1L, 2L), records.stream().map(r -> r.generation()).toList());
        assertTrue(Files.readString(history).startsWith(new String(previousHistory, java.nio.charset.StandardCharsets.UTF_8)));
    }
    @Test void durableDecisionIsFinishedAndNeverRolledBackOnChangedSettings() throws Exception {
        Path root = temporary.resolve("decision"); var c = config(root, TrainingArchitecture.BRN, TrainingSource.bootstrap(generator));
        var crash = new TrainerService.Operations() {
            @Override CandidateLifecycle.Result completeDecision(CheckpointStore store, ValidationRecord evidence) throws IOException {
                throw new IOException("interrupted after decision commit");
            }
        };
        String candidate;
        try (var service = TrainerService.fresh(c, initial(c.architecture()), crash, s -> {})) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(100))); assertTrue(service.snapshot().failed()); candidate = service.snapshot().candidateId();
        }
        var end = stop(changed(c, Change.GAMES), TrainerSnapshot.State.GENERATING_SELF_PLAY, false);
        assertEquals(2, end.generation()); assertEquals(candidate, end.latestTrainingId()); assertEquals(candidate, end.bestId());
        assertFalse(Files.exists(root.resolve("restarted-generations")));
    }
    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"BRN", "BRN1", "BRN2"})
    void initialRateRunLimitAndInapplicableValidationPairsDoNotInvalidateExactBootstrapResume(TrainingArchitecture architecture) throws Exception {
        Path root = temporary.resolve("safe-settings-" + architecture); var c = config(root, architecture, TrainingSource.bootstrap(generator));
        stop(c, TrainerSnapshot.State.TRAINING, true);
        var changed = changed(c, Change.VALIDATION_PAIRS);
        var next = new TrainerConfig(root, changed.masterSeed(), changed.selfPlay(), changed.training(), changed.validation(), 2,
                changed.depthChange(), changed.startingFen(), changed.architecture(), .004, null);
        var owned = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] b, SelfPlayControl ctl, Consumer<SelfPlayBatch.Progress> observer) {
                throw new AssertionError("Unchanged effective settings must reuse durable data");
            }
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg, SelfPlayControl ctl, Consumer<SelfPlayTraining.Progress> observer) {
                assertEquals(.001, state.hyperparameters().learningRate()); return super.trainBootstrap(state, samples, cfg, ctl, observer);
            }
        };
        try (var service = TrainerService.resume(next, work, s -> { if (s.state() == TrainerSnapshot.State.RECORDING_DECISION) owned.get().stop(); })) {
            owned.set(service); run(service); assertEquals("", service.lifecycleNotice());
        }
        assertFalse(Files.exists(root.resolve("restarted-generations")));
    }
    @ParameterizedTest @EnumSource(value = Change.class, names = {"DEPTH", "THREADS", "GAMES", "VALIDATION_PAIRS"})
    void ordinaryNnueRestartsForChangedSettingsAndKeepsSettledState(Change change) throws Exception {
        Path root = temporary.resolve("nnue-" + change); var c = config(root, TrainingArchitecture.NNUE, TrainingSource.SELF_PLAY);
        var first = stop(c, TrainerSnapshot.State.TRAINING, true);
        byte[] state = BrnBootstrapTest.state(root, first.latestTrainingId());
        var next = changed(c, change); var owned = new AtomicReference<TrainerService>();
        try (var service = TrainerService.resume(next, new TrainerService.Operations(), s -> {
            if (s.state() == TrainerSnapshot.State.TRAINING) owned.get().stop();
        })) {
            owned.set(service); var end = run(service); assertEquals(1, end.generation()); assertEquals(first.latestTrainingId(), end.latestTrainingId());
            assertTrue(service.lifecycleNotice().contains("Restarted unfinished"));
        }
        assertArrayEquals(state, BrnBootstrapTest.state(root, first.latestTrainingId()));
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void legacyBootstrapPinWithoutAttemptRecordStillResumesOrRestartsCorrectly(boolean change) throws Exception {
        Path root = temporary.resolve("legacy-" + change); var c = config(root, TrainingArchitecture.BRN, TrainingSource.bootstrap(generator));
        var first = stop(c, TrainerSnapshot.State.GENERATING_SELF_PLAY, true);
        Files.delete(root.resolve(GenerationAttempt.FILE)); // Test fixture representing the previous release's store.
        Path pin = root.resolve("bootstrap").resolve(first.latestTrainingId() + ".plan"); byte[] old = Files.readAllBytes(pin);
        var next = stop(change ? changed(c, Change.DEPTH) : c, TrainerSnapshot.State.TRAINING, false);
        assertEquals(1, next.generation());
        if (change) { assertFalse(Arrays.equals(old, Files.readAllBytes(pin))); assertTrue(Files.exists(root.resolve("restarted-generations"))); }
        else { assertArrayEquals(old, Files.readAllBytes(pin)); assertFalse(Files.exists(root.resolve("restarted-generations"))); }
    }
    @Test void invalidNewGeneratorPreservesTheOriginalResumableAttempt() throws Exception {
        Path root = temporary.resolve("invalid-source"); var c = config(root, TrainingArchitecture.BRN, TrainingSource.bootstrap(generator));
        var first = stop(c, TrainerSnapshot.State.TRAINING, true);
        byte[] attempt = Files.readAllBytes(root.resolve(GenerationAttempt.FILE));
        try (var service = TrainerService.resume(c.withSource(TrainingSource.bootstrap(temporary.resolve("missing"))))) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(100))); assertTrue(service.snapshot().failed());
        }
        assertArrayEquals(attempt, Files.readAllBytes(root.resolve(GenerationAttempt.FILE)));
        assertFalse(Files.exists(root.resolve("restarted-generations")));
        assertTrue(Files.exists(root.resolve("bootstrap").resolve(first.latestTrainingId() + ".data")));
    }
}
