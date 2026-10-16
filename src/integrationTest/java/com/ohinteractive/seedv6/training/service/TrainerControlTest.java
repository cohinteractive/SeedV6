package com.ohinteractive.seedv6.training.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.training.service.TrainerServiceTest.*;
import static com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class TrainerControlTest {
    @TempDir Path root;
    static final class Pause implements AutoCloseable {
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        void block() {
            entered.countDown();
            try { if (!release.await(30, TimeUnit.SECONDS)) throw new AssertionError("Test boundary was not released."); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
        }
        void await() throws InterruptedException { assertTrue(entered.await(60, TimeUnit.SECONDS), "Phase was not reached."); }
        @Override public void close() { release.countDown(); }
    }

    enum StopPhase { SELF_PLAY, TRAINING, VALIDATION, PUBLICATION, DECISION, BETWEEN_GENERATIONS }

    @Tag("slow-nnue")
    @ParameterizedTest @EnumSource(StopPhase.class)
    void cooperativeStopPropagatesAndPreservesExactPhaseBoundary(StopPhase phase) throws Exception {
        Pause pause = new Pause();
        AtomicBoolean trainingPaused = new AtomicBoolean();
        AtomicReference<SelfPlayControl> gameControl = new AtomicReference<>();
        AtomicReference<ValidationControl> matchControl = new AtomicReference<>();
        Matches work = new Matches(phase == StopPhase.DECISION) {
            @Override SelfPlayBatch generate(NnueNetwork actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                gameControl.set(control);
                if (phase == StopPhase.SELF_PLAY) return CancelledBatch.generate(actor, cfg, control, pause::block);
                return super.generate(actor, cfg, board, control, observer);
            }
            @Override Optional<SelfPlayTraining.Statistics> train(NnueTrainer trainer, SelfPlayBatch batch,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.train(trainer, batch, cfg, control, progress -> {
                    observer.accept(progress);
                    if (phase == StopPhase.TRAINING && trainingPaused.compareAndSet(false, true)) pause.block();
                });
            }
            @Override CheckpointStore.Checkpoint publish(CheckpointStore store, NnueTrainer trainer,
                    CheckpointManifest.Metadata metadata) throws IOException {
                if (phase == StopPhase.PUBLICATION) pause.block();
                return super.publish(store, trainer, metadata);
            }
            @Override ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig cfg,
                    long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
                matchControl.set(control);
                if (phase == StopPhase.VALIDATION) return CancelledArena.validate(candidate, incumbent, cfg, board, history, control, pause::block, observer);
                return super.validate(candidate, incumbent, cfg, board, history, control, observer);
            }
            @Override CandidateLifecycle.Result recordDecision(CheckpointStore store, String candidate, String incumbent,
                    ValidationResult result, PromotionPolicy policy) throws IOException {
                if (phase != StopPhase.DECISION) return super.recordDecision(store, candidate, incumbent, result, policy);
                var validation = store.recordValidation(candidate, incumbent, result, policy);
                pause.block();
                return CandidateLifecycle.completeDecision(store, validation);
            }
        };
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config(root, 0), trainer(), work, snapshot -> {
            if (phase == StopPhase.BETWEEN_GENERATIONS && snapshot.totals().completedGenerations() == 1) pause.block();
        })) {
            try {
                service.start(); pause.await();
                TrainerSnapshot before = service.snapshot();
                service.stop();
                assertEquals(STOPPING, service.snapshot().state()); assertTrue(service.snapshot().stopping());
                assertFalse(service.awaitTermination(Duration.ofMillis(1)), "No terminal state before the active boundary drains.");
                assertNotEquals(STOPPING, before.state(), "Earlier snapshot remains immutable.");
                assertTrue(gameControl.get().cancelled());
                if (matchControl.get() != null) assertTrue(matchControl.get().cancelled());
            } finally { pause.close(); }
            assertTrue(service.awaitTermination(WAIT));
            end = service.snapshot();
            assertEquals(STOPPED, end.state(), end.failureSummary()); assertTrue(service.failure().isEmpty());
            assertEquals(1, end.generation(), "No second generation after stop.");
        }
        var rows = new HistoryRepository(root).refresh().records();
        boolean partial = phase == StopPhase.SELF_PLAY || phase == StopPhase.TRAINING
                || phase == StopPhase.PUBLICATION || phase == StopPhase.VALIDATION;
        if (partial) { assertTrue(rows.isEmpty()); assertTrue(PartialGeneration.inspect(root).isPresent()); }
        else assertEquals(1, rows.size());
        try (var store = new CheckpointStore(root)) {
            var refs = store.recover();
            if (phase == StopPhase.SELF_PLAY || phase == StopPhase.TRAINING) {
                assertEquals(1, count(root.resolve("checkpoints"))); assertEquals(0, count(root.resolve("validations")));
                assertEquals(0, refs.latestTraining().orElseThrow().manifest().optimizerStep());
                assertEquals(0, end.totals().completedGenerations());
                if (phase == StopPhase.SELF_PLAY) {
                    assertEquals(1, end.selfPlay().abortedGames()); assertEquals(0, end.selfPlay().sampledPositions());
                    assertEquals(0, end.selfPlay().draws()); assertEquals(0, end.totals().optimizerUpdates());
                } else {
                    assertEquals(1, end.optimizerStep()); assertEquals(1, end.totals().optimizerUpdates());
                    assertTrue(end.training().orElseThrow().cancelled());
                }
            } else if (phase == StopPhase.VALIDATION || phase == StopPhase.PUBLICATION) {
                assertEquals(2, count(root.resolve("checkpoints"))); assertEquals(0, count(root.resolve("validations")));
                assertEquals(0, end.totals().completedGenerations()); assertTrue(end.assessment().isEmpty());
                assertEquals(2, end.validation().orElseThrow().incompletePairs());
            } else {
                assertEquals(2, count(root.resolve("checkpoints"))); assertEquals(1, count(root.resolve("validations")));
                assertEquals(1, end.totals().completedGenerations());
                var evidence = store.validationFor(end.latestTrainingId()).orElseThrow();
                if (phase == StopPhase.DECISION) {
                    assertEquals(PromotionPolicy.Decision.PROMOTE, evidence.assessment().decision());
                    assertEquals(end.latestTrainingId(), end.bestId()); assertEquals(2, count(root.resolve("promotions")));
                }
            }
        }
        assertNoOwnedThreads();
    }

    @Test void stopBeforeStartAndDuringRecoveryDoesNotBootstrapOrLeakResources() throws Exception {
        Path idle = root.resolve("idle");
        try (var service = TrainerService.fresh(config(idle, 0), trainer())) {
            service.stop(); assertTrue(service.isTerminated()); assertEquals(STOPPED, service.snapshot().state());
            assertFalse(Files.exists(idle)); assertThrows(IllegalStateException.class, service::start);
        }
        Pause pause = new Pause();
        try (var service = TrainerService.fresh(config(root.resolve("recovery"), 0), trainer(), new Matches(), snapshot -> pause.block())) {
            try { service.start(); pause.await(); service.stop(); }
            finally { pause.close(); }
            assertTrue(service.awaitTermination(WAIT)); assertEquals(STOPPED, service.snapshot().state());
            assertFalse(Files.exists(root.resolve("recovery")));
        }
        assertNoOwnedThreads();
    }

    enum Fault { SELF_PLAY, TRAINING, CANDIDATE, LATEST_REFERENCE, VALIDATION, VALIDATION_RESULT,
                 VALIDATION_RECORD, PROMOTION_RECORD, BEST_REFERENCE }

    @Tag("slow-nnue")
    @ParameterizedTest @EnumSource(Fault.class)
    void infrastructureFailureStopsLoopAndReopenResolvesAnyPublishedCandidate(Fault fault) throws Exception {
        AtomicBoolean publishing = new AtomicBoolean();
        Matches work = new Matches(true) {
            @Override CheckpointStore open(TrainerConfig cfg) throws IOException {
                return StoreFaults.open(cfg.checkpointRoot(), (source, target) -> {
                    if (!publishing.get()) return false;
                    String directory = target.getParent().getFileName().toString();
                    return switch (fault) {
                        case CANDIDATE -> directory.equals("checkpoints");
                        case LATEST_REFERENCE -> target.endsWith("latest-training");
                        case VALIDATION_RECORD -> directory.equals("validations");
                        case PROMOTION_RECORD -> directory.equals("promotions");
                        case BEST_REFERENCE -> target.endsWith("best");
                        default -> false;
                    };
                });
            }
            @Override SelfPlayBatch generate(NnueNetwork actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                if (fault == Fault.SELF_PLAY) throw new IllegalStateException("Injected self-play failure");
                return super.generate(actor, cfg, board, control, observer);
            }
            @Override Optional<SelfPlayTraining.Statistics> train(NnueTrainer trainer, SelfPlayBatch batch,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.train(trainer, batch, cfg, control, progress -> {
                    observer.accept(progress);
                    if (fault == Fault.TRAINING) throw new IllegalStateException("Injected training failure after one atomic update");
                });
            }
            @Override CheckpointStore.Checkpoint publish(CheckpointStore store, NnueTrainer trainer,
                    CheckpointManifest.Metadata metadata) throws IOException {
                publishing.set(true);
                return super.publish(store, trainer, metadata);
            }
            @Override ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig cfg,
                    long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
                if (fault == Fault.VALIDATION) throw new IllegalStateException("Injected validation failure");
                if (fault == Fault.VALIDATION_RESULT) {
                    var bad = new ValidationResult.Game(GameTermination.INFRASTRUCTURE_FAILURE, 0);
                    return new ValidationResult(cfg, ValidationArena.stateHash(board, history),
                            Collections.nCopies(cfg.openingPairs(), new ValidationResult.Pair("", bad, bad)));
                }
                return super.validate(candidate, incumbent, cfg, board, history, control, observer);
            }
        };
        TrainerSnapshot failed;
        try (var service = TrainerService.fresh(config(root, 0), trainer(), work, ignore())) {
            service.start(); assertTrue(service.awaitTermination(WAIT)); failed = service.snapshot();
            assertEquals(FAILED, failed.state()); assertTrue(failed.failed()); assertFalse(failed.running());
            assertTrue(service.failure().isPresent()); assertFalse(failed.failureSummary().isBlank());
            assertEquals(1, failed.generation()); assertEquals(0, failed.totals().completedGenerations());
        }
        assertTrue(new HistoryRepository(root).refresh().records().isEmpty(), "Failed lifecycles are not completed history");
        boolean beforeCandidate = fault == Fault.SELF_PLAY || fault == Fault.TRAINING || fault == Fault.CANDIDATE;
        long priorGeneration = beforeCandidate ? 0 : 1;
        try (var store = new CheckpointStore(root)) {
            var old = store.recover();
            assertEquals(failed.bestId(), old.best().orElseThrow().manifest().id());
            assertEquals(beforeCandidate || fault == Fault.LATEST_REFERENCE ? 0 : 1,
                    old.latestTraining().orElseThrow().manifest().generation());
            assertEquals(beforeCandidate ? 1 : 2, count(root.resolve("checkpoints")));
        }
        Matches resumed = new Matches();
        try (var service = TrainerService.resume(config(root, 1), resumed, ignore())) {
            var end = finish(service); assertEquals(priorGeneration + 1, end.generation());
            assertEquals(beforeCandidate ? 0 : 1, end.totals().recoveredLifecycles());
            assertEquals(1, end.totals().completedGenerations());
            assertEquals(beforeCandidate ? 2 : 4, end.optimizerStep());
        }
        boolean storedPromote = fault == Fault.PROMOTION_RECORD || fault == Fault.BEST_REFERENCE;
        assertEquals(beforeCandidate || storedPromote ? 1 : 2, resumed.calls,
                "No match repeated when durable validation already authorizes completion.");
        assertEquals(storedPromote ? 2 : 1, count(root.resolve("promotions")));
        assertNoOwnedThreads();
    }

    @Tag("slow-nnue")
    @Test void duplicateStoreOwnerAndDuplicateStartFailWhileOriginalKeepsOwnership() throws Exception {
        Pause pause = new Pause();
        try (var first = TrainerService.fresh(config(root, 0), trainer(), new Matches(), snapshot -> {
            if (snapshot.state() == GENERATING_SELF_PLAY) pause.block();
        })) {
            try {
                first.start(); pause.await();
                assertThrows(IllegalStateException.class, first::start);
                try (var duplicate = TrainerService.resume(config(root, 1))) {
                    duplicate.start(); assertTrue(duplicate.awaitTermination(WAIT));
                    assertEquals(FAILED, duplicate.snapshot().state());
                    assertTrue(duplicate.snapshot().failureSummary().contains("ownership"));
                }
                assertEquals(GENERATING_SELF_PLAY, first.snapshot().state());
                assertThrows(IOException.class, () -> new CheckpointStore(root));
                first.stop();
            } finally { pause.close(); }
            assertTrue(first.awaitTermination(WAIT)); assertEquals(STOPPED, first.snapshot().state());
        }
        try (var next = TrainerService.resume(config(root, 1), new Matches(), ignore())) { finish(next); }
        assertNoOwnedThreads();
    }

    @Tag("slow-nnue")
    @Test void concurrentStartCreatesOnlyOneWorkerAndNewInstancesReleaseRealParallelSearchWorkers() throws Exception {
        Pause pause = new Pause();
        try (var service = TrainerService.fresh(config(root, 0), trainer(), new Matches(), snapshot -> {
            if (snapshot.state() == RECOVERING) pause.block();
        }); var callers = Executors.newFixedThreadPool(2)) {
            try {
                Callable<Boolean> start = () -> { try { service.start(); return true; } catch (IllegalStateException expected) { return false; } };
                var results = callers.invokeAll(List.of(start, start));
                assertNotEquals(results.get(0).get(), results.get(1).get()); pause.await();
                assertEquals(1, Thread.getAllStackTraces().keySet().stream().filter(t -> t.isAlive()
                        && t.getName().startsWith("seedv6-trainer-")).count());
                service.stop();
            } finally { pause.close(); }
            assertTrue(service.awaitTermination(WAIT));
        }
        for (int i = 0; i < 3; i++) {
            var cfg = config(root.resolve("parallel" + i), 1);
            var parallel = new TrainerConfig(cfg.checkpointRoot(), cfg.masterSeed(),
                    new TrainerConfig.SelfPlay(1, 2, 2, 0, 0, 2, 8, MAPPING), cfg.training(),
                    new TrainerConfig.Validation(2, 0, 0, 1, 2, 8, MAPPING, GATE),
                    1, cfg.depthChange(), START);
            try (var service = TrainerService.fresh(parallel, trainer())) { finish(service); }
            assertNoOwnedThreads();
        }
    }
}
