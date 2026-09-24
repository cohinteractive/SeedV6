package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

/** Small real pipeline fixtures, with every writer confined to @TempDir. */
@Timeout(120)
class TrainingRunControlTest {
    @TempDir Path temporary;
    TrainerConfig config(Path root, int generations) {
        return new TrainerConfig(root, 1, new TrainerConfig.SelfPlay(1, 1, 8, 0, 0, 4, 8, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(2, 0, 0, 1, 1, 8,
                NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)), generations,
                TrainerConfig.DepthChange.REQUIRE_SAME, BrnBootstrapTest.MATE, TrainingArchitecture.BRN2, .001,
                TrainingSource.HANDCRAFTED).withRunSeeds(new BrnRunSeeds(1, 2));
    }
    TrainerSnapshot finish(TrainerService s) throws Exception {
        s.start(); assertTrue(s.awaitTermination(Duration.ofSeconds(90)));
        assertFalse(s.snapshot().failed(), s.snapshot().failureSummary()); return s.snapshot();
    }
    TrainerSnapshot whole(Path root) throws Exception {
        try (var s = TrainerService.fresh(config(root, 1), new Brn2Trainer(.001))) { return finish(s); }
    }
    byte[] state(Path root, String id) throws Exception {
        return Files.readAllBytes(root.resolve("checkpoints").resolve(id).resolve("training.state"));
    }
    TrainerConfig gameConfig(Path root) {
        var base = config(root, 1);
        return new TrainerConfig(root, 1, base.selfPlay(), base.training(), base.validation(), 1,
                base.depthChange(), base.startingFen(), TrainingArchitecture.BRN2, .001, TrainingSource.SELF_PLAY)
                .withRunSeeds(new BrnRunSeeds(1, 2));
    }
    @Test void safeStopRetainsCompletedGamesAcrossNewServiceAndCountsResumedGenerationOnce() throws Exception {
        Path root = temporary.resolve("split"), baseline = temporary.resolve("whole"); var expected = whole(baseline);
        var owner = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig c, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> o) {
                return super.generateHandcrafted(c, board, control, p -> { o.accept(p); if (p.statistics().completedGames() == 2) owner.get().stop(); });
            }
        };
        TrainerSnapshot stopped;
        try (var s = TrainerService.fresh(config(root, 1), new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), work, v -> {})) {
            owner.set(s); stopped = finish(s);
        }
        assertEquals(0, stopped.totals().completedGenerations()); assertTrue(new HistoryRepository(root).refresh().records().isEmpty());
        var saved = PartialGeneration.inspect(root).orElseThrow(); assertEquals(2, saved.games().games().size()); assertTrue(saved.candidate().isEmpty());
        assertEquals(saved.activeNanos(), stopped.generationElapsed(System.nanoTime()).orElseThrow().toNanos());
        byte[] parent = state(root, stopped.latestTrainingId()), best = state(root, stopped.bestId());
        var remaining = new AtomicInteger();
        var resume = new TrainerService.Operations() {
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig c, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> o) {
                assertEquals(2, control.savedGames().games().size());
                return super.generateHandcrafted(c, board, control, p -> { o.accept(p); if (p.lastGame().gameIndex() >= 2) remaining.incrementAndGet(); });
            }
        };
        try (var s = TrainerService.resume(config(root, 1), resume, v -> {})) {
            var end = finish(s); assertEquals(1, end.generation()); assertEquals(1, end.totals().completedGenerations());
            long recorded = new HistoryRepository(root).refresh().records().getFirst().totalNanos();
            assertTrue(recorded >= saved.activeNanos());
            assertEquals(recorded, end.generationElapsed(System.nanoTime()).orElseThrow().toNanos());
            assertEquals(recorded, end.generationElapsed(Long.MAX_VALUE).orElseThrow().toNanos());
            assertEquals(1, end.run().orElseThrow().targetGeneration()); assertEquals(6, remaining.get());
            assertArrayEquals(state(baseline, expected.latestTrainingId()), state(root, end.latestTrainingId()));
        }
        assertArrayEquals(parent, state(root, stopped.latestTrainingId())); assertArrayEquals(best, state(root, stopped.bestId()));
        assertEquals(1, new HistoryRepository(root).refresh().records().size());
    }
    @Test void optimizerContinuationRestoresAppliedUpdatesWithoutReplay() throws Exception {
        Path root = temporary.resolve("split"), baseline = temporary.resolve("whole"); var expected = whole(baseline);
        var owner = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> o) {
                return super.trainBootstrap(state, samples, config, control, p -> { o.accept(p); if (p.optimizerUpdates() == 2) owner.get().stop(); });
            }
        };
        try (var s = TrainerService.fresh(config(root, 1), new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), work, v -> {})) {
            owner.set(s); assertEquals(2, finish(s).optimizerStep());
        }
        assertEquals(2, PartialGeneration.inspect(root).orElseThrow().training().updates());
        var seen = new ArrayList<Long>();
        var resume = new TrainerService.Operations() {
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig c, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> o) {
                throw new AssertionError("Completed self-play must not run again");
            }
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> o) {
                assertEquals(2, state.step());
                return super.trainBootstrap(state, samples, config, control, p -> { o.accept(p); seen.add(p.optimizerUpdates()); });
            }
        };
        try (var s = TrainerService.resume(config(root, 1), resume, v -> {})) {
            var end = finish(s); assertEquals(3, seen.getFirst()); assertEquals(6, seen.getLast());
            assertArrayEquals(state(baseline, expected.latestTrainingId()), state(root, end.latestTrainingId()));
        }
    }
    @Test void durationStopsThroughNormalCancellationAndResumeGetsFreshBudget() throws Exception {
        Path root = temporary.resolve("timed"); var entered = new AtomicInteger();
        var work = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> o) {
                entered.incrementAndGet();
                return super.trainBootstrap(state, samples, cfg, control, p -> {
                    o.accept(p);
                    if (p.optimizerUpdates() == 1) {
                        long end = System.nanoTime() + Duration.ofSeconds(25).toNanos();
                        while (!control.cancelled() && System.nanoTime() < end) java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
                        assertTrue(control.cancelled());
                    }
                });
            }
        };
        var timed = config(root, 1).withTimeLimit(Duration.ofSeconds(15));
        try (var s = TrainerService.fresh(timed, new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), work, v -> {})) {
            var end = finish(s); assertEquals(1, entered.get()); assertTrue(end.run().orElseThrow().timeLimitReached());
            assertEquals(0, end.totals().completedGenerations()); assertEquals(1, PartialGeneration.inspect(root).orElseThrow().training().updates());
        }
        try (var s = TrainerService.resume(timed)) {
            var end = finish(s); assertEquals(1, end.totals().completedGenerations()); assertFalse(end.run().orElseThrow().timeLimitReached());
            assertTrue(end.elapsed().compareTo(Duration.ofSeconds(15)) < 0);
        }
    }
    @Test void finiteTargetFinalizesTwentyWithoutInitializingTwentyOne() throws Exception {
        Path root = temporary.resolve("finite");
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.initializeBrnRunSeeds(new BrnRunSeeds(1, 2)); store.initializeBrnSupervision(BrnSupervision.WDL);
            store.writeTrainingSource(TrainingSource.HANDCRAFTED);
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new CheckpointManifest.Metadata(18, 1, ""));
        }
        var generations = new ArrayList<Long>();
        assertEquals(20, config(root, 10).finalGeneration(10));
        try (var s = TrainerService.resume(config(root, 2).withTimeLimit(Duration.ofMinutes(1)), new TrainerService.Operations(), v -> {
            if (v.state() == TrainerSnapshot.State.GENERATING_SELF_PLAY) generations.add(v.generation());
        })) {
            var end = finish(s); assertEquals(20, end.generation()); assertEquals(20, end.run().orElseThrow().targetGeneration());
            assertEquals(2, end.totals().completedGenerations()); assertFalse(end.run().orElseThrow().timeLimitReached());
        }
        assertEquals(java.util.List.of(19L, 20L), generations);
        assertEquals(20, GenerationAttempt.inspect(root).orElseThrow().generation());
        assertEquals(2, new HistoryRepository(root).refresh().records().size());
        assertEquals(2, CheckpointInspection.validations(root).size());
    }

    @Test void gameValidationPreservesCompletedColourGameAndFinalizesOnlyAfterResume() throws Exception {
        Path root = temporary.resolve("game-validation");
        var cfg = gameConfig(root);
        var owner = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override ValidationResult validate(NetworkModel candidate, NetworkModel best, ValidationConfig c,
                    long[] board, com.ohinteractive.seedv6.rules.GameHistory history, ValidationControl control, Consumer<ValidationProgress> o) {
                return super.validate(candidate, best, c, board, history, control, p -> {
                    o.accept(p); if (p.completedGames() == 1) owner.get().stop();
                });
            }
        };
        TrainerSnapshot stopped;
        try (var s = TrainerService.fresh(cfg, new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), work, v -> {})) {
            owner.set(s); stopped = finish(s);
        }
        assertEquals(0, stopped.totals().completedGenerations()); assertTrue(CheckpointInspection.validations(root).isEmpty());
        assertTrue(new HistoryRepository(root).refresh().records().isEmpty());
        var saved = PartialGeneration.inspect(root).orElseThrow();
        assertEquals(stopped.candidateId(), saved.candidate());
        assertNotEquals(GameTermination.CANCELLED, saved.pairs().getFirst().candidateWhite().termination());
        assertEquals(GameTermination.CANCELLED, saved.pairs().getFirst().candidateBlack().termination());
        byte[] candidate = state(root, stopped.candidateId()), best = state(root, stopped.bestId());
        var resume = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig c, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> o) {
                throw new AssertionError("Published candidate must not generate or train again");
            }
            @Override ValidationResult validate(NetworkModel candidate, NetworkModel best, ValidationConfig c,
                    long[] board, com.ohinteractive.seedv6.rules.GameHistory history, ValidationControl control, Consumer<ValidationProgress> o) {
                assertEquals(saved.pairs(), control.savedPairs());
                var reused = control.savedPairs().getFirst().candidateWhite();
                var result = super.validate(candidate, best, c, board, history, control, o);
                assertSame(reused, result.pairs().getFirst().candidateWhite());
                return result;
            }
        };
        try (var s = TrainerService.resume(cfg, resume, v -> {})) {
            var end = finish(s); assertEquals(1, end.generation()); assertEquals(1, end.totals().completedGenerations());
            assertEquals(stopped.candidateId(), end.candidateId()); assertEquals(1, end.run().orElseThrow().targetGeneration());
            var row = new HistoryRepository(root).refresh().records().getFirst();
            assertEquals(cfg.validation().policy().rawScoreThreshold(row.validPairs()).orElseThrow(), row.rawPromotionThreshold());
            assertEquals(row.totalNanos(), end.generationElapsed(System.nanoTime()).orElseThrow().toNanos());
        }
        assertArrayEquals(candidate, state(root, stopped.candidateId())); assertArrayEquals(best, state(root, stopped.bestId()));
        assertEquals(1, new HistoryRepository(root).refresh().records().size());
    }
    @Test void stopDoesNotTurnInfrastructureFailureIntoAReusablePartialMatch() throws Exception {
        Path root = temporary.resolve("failed-match"); var owner = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override ValidationResult validate(NetworkModel candidate, NetworkModel best, ValidationConfig c,
                    long[] board, com.ohinteractive.seedv6.rules.GameHistory history, ValidationControl control, Consumer<ValidationProgress> o) {
                owner.get().stop();
                var bad = new ValidationResult.Game(GameTermination.INFRASTRUCTURE_FAILURE, 0);
                var cancelled = new ValidationResult.Game(GameTermination.CANCELLED, 0);
                return new ValidationResult(c, ValidationArena.stateHash(board, history),
                        Collections.nCopies(c.openingPairs(), new ValidationResult.Pair("", bad, cancelled)));
            }
        };
        try (var s = TrainerService.fresh(gameConfig(root), new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), work, v -> {})) {
            owner.set(s); s.start(); assertTrue(s.awaitTermination(Duration.ofSeconds(45)));
            assertTrue(s.snapshot().failed()); assertTrue(s.snapshot().failureSummary().contains("infrastructure"));
        }
        assertTrue(PartialGeneration.inspect(root).isEmpty()); assertTrue(CheckpointInspection.validations(root).isEmpty());
        assertTrue(new HistoryRepository(root).refresh().records().isEmpty());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void interruptedCandidateRecoveryCanItselfPauseWithoutLosingFinishedValidationGames(boolean legacy) throws Exception {
        Path root = temporary.resolve("recovery"); var cfg = gameConfig(root);
        try (var s = TrainerService.fresh(cfg, new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new TrainerService.Operations(), v -> {
            if (v.state() == TrainerSnapshot.State.VALIDATING) throw new IllegalStateException("Injected process boundary");
        })) {
            s.start(); assertTrue(s.awaitTermination(Duration.ofSeconds(45))); assertTrue(s.snapshot().failed());
        }
        if (legacy) Files.delete(root.resolve(GenerationAttempt.FILE)); // Only this temporary legacy fixture lacks original settings.
        assertTrue(PartialGeneration.inspect(root).isEmpty());
        var owner = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override ValidationResult validate(NetworkModel candidate, NetworkModel best, ValidationConfig c,
                    long[] board, com.ohinteractive.seedv6.rules.GameHistory history, ValidationControl control, Consumer<ValidationProgress> o) {
                return super.validate(candidate, best, c, board, history, control, p -> {
                    o.accept(p); if (p.completedGames() == 1) owner.get().stop();
                });
            }
        };
        try (var s = TrainerService.resume(cfg, work, v -> {})) { owner.set(s); finish(s); }
        var partial = PartialGeneration.inspect(root).orElseThrow(); assertTrue(partial.recoveryOnly());
        assertEquals(!legacy, partial.generationSettingsKnown());
        assertNotEquals(GameTermination.CANCELLED, partial.pairs().getFirst().candidateWhite().termination());
        try (var s = TrainerService.resume(cfg)) {
            var end = finish(s); assertEquals(2, end.generation()); assertEquals(1, end.totals().completedGenerations());
            assertEquals(1, end.totals().recoveredLifecycles());
            assertTrue(end.run().orElseThrow().generationSettingsKnown());
        }
        assertEquals(List.of(2L), new HistoryRepository(root).refresh().records().stream().map(r -> r.generation()).toList(),
                "Recovery cannot invent the failed invocation's original timing and measurements");
    }
}
