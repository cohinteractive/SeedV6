package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(45)
class CorpusRecoveryLifecycleTest {
    @TempDir Path temp;

    Path corpus() throws Exception {
        Path root = temp.resolve("corpus");
        try (var writer = new CorpusWriter(root, 4, new CorpusWriter.SourceInfo("test", "recovery", "{}", "depth"))) {
            for (int i = 0; i < 8; i++) writer.put(new CorpusRecord(CorpusPosition.fromFen(
                    "4k3/8/8/8/3pP3/8/8/4K3 w - - " + i + " 1"), CorpusRecord.CP,
                    i * 30, CorpusRecord.WHITE, 20, -1, 0, 1, i + 1));
            writer.checkpoint(true);
        }
        return root;
    }

    TrainerConfig config(TrainingArchitecture architecture, Path corpus) {
        return new TrainerConfig(temp.resolve("lineage"), 71,
                new TrainerConfig.SelfPlay(1, 1, 1, 0, 0, 1, 8, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, false),
                new TrainerConfig.Validation(1, 0, 0, 1, 1, 8, NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                1, TrainerConfig.DepthChange.REQUIRE_SAME, TrainerConfig.STANDARD_START, architecture, .001,
                TrainingSource.corpus(corpus)).withCorpusTraining(new CorpusTrainingConfig(2))
                .withValidationMethod(ValidationMethod.HELD_OUT);
    }

    NetworkTrainingState initial(TrainingArchitecture architecture) {
        return architecture == TrainingArchitecture.NNUE
                ? new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(71)))
                : new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
    }

    String initialize(TrainerConfig config) throws Exception {
        try (var store = new CheckpointStore(config.checkpointRoot(), config.architecture())) {
            return store.initialize(initial(config.architecture()), new CheckpointManifest.Metadata(143, 1, ""))
                    .manifest().id();
        }
    }

    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "BRN2"})
    void failedCorpusPreparationRetainsRecoveredBestAndGeneration(TrainingArchitecture architecture) throws Exception {
        var config = config(architecture, corpus());
        String best = initialize(config);
        try (var prepared = new CorpusTraining(config, config.source(), true)) { assertEquals(2, prepared.pin().positions()); }
        Files.writeString(config.checkpointRoot().resolve("corpus-training/view.json"), "invalid");
        try (var service = TrainerService.resume(config)) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertEquals(TrainerSnapshot.State.FAILED, service.snapshot().state());
            assertEquals(best, service.snapshot().bestId());
            assertEquals(best, service.snapshot().latestTrainingId());
            assertEquals(143, service.snapshot().generation(), "Corpus preparation must not hide the recovered generation");
        }
        assertEquals(best, CheckpointInspection.reference(config.checkpointRoot(), "best"));
        assertEquals(best, CheckpointInspection.reference(config.checkpointRoot(), "latest-training"));
    }

    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "BRN2"})
    void preparationPublishesRecoveredIdentityAndCanStopBeforeCorpusIO(TrainingArchitecture architecture) throws Exception {
        var config = config(architecture, corpus());
        String best = initialize(config);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var work = new TrainerService.Operations() {
            @Override CorpusTraining openCorpus(TrainerConfig c, TrainingSource source, boolean mayCreate,
                    CorpusPreparation preparation) throws IOException {
                entered.countDown();
                try { if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("Test did not release preparation"); }
                catch (InterruptedException e) { throw new IOException(e); }
                return super.openCorpus(c, source, mayCreate, preparation);
            }
        };
        try (var service = TrainerService.resume(config, work, s -> {})) {
            try {
                service.start(); assertTrue(entered.await(10, TimeUnit.SECONDS));
                var snapshot = service.snapshot();
                assertEquals(TrainerSnapshot.State.PREPARING_CORPUS, snapshot.state());
                assertEquals(143, snapshot.generation()); assertEquals(best, snapshot.bestId());
                assertEquals(best, snapshot.latestTrainingId());
                assertEquals(config.source(), snapshot.run().orElseThrow().source());
                assertEquals(144, snapshot.run().orElseThrow().firstGeneration());
                assertFalse(snapshot.run().orElseThrow().generationSettingsKnown());
                assertTrue(GenerationAttempt.inspect(config.checkpointRoot()).isEmpty());
                service.stop();
            } finally { release.countDown(); }
            assertTrue(service.awaitTermination(Duration.ofSeconds(10)));
            assertEquals(TrainerSnapshot.State.STOPPED, service.snapshot().state());
            assertTrue(service.failure().isEmpty());
        }
        assertFalse(Files.exists(config.checkpointRoot().resolve("corpus-training")));
        assertEquals(best, CheckpointInspection.reference(config.checkpointRoot(), "best"));
    }

    static TrainerService.Operations forbiddenGenerators() {
        return new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig config, long[] board,
                    SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) { throw new AssertionError("Corpus invoked generator"); }
            @Override SelfPlayBatch generate(NnueNetwork actor, SelfPlayConfig config, long[] board,
                    SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) { throw new AssertionError("Corpus invoked NNUE generator"); }
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig config, long[] board,
                    SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) { throw new AssertionError("Corpus invoked handcrafted generator"); }
        };
    }

    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "BRN2"})
    void existingLineageResumesAfterAbandonedPreparationWithoutGeneratedArtifacts(TrainingArchitecture architecture) throws Exception {
        var config = config(architecture, corpus());
        String best = initialize(config);
        Path binding = config.checkpointRoot().resolve("corpus-training");
        Files.createDirectories(binding);
        Files.writeString(binding.resolve("records.idx.pending"), "interrupted admission");
        var phases = new ArrayList<TrainerSnapshot>();
        try (var service = TrainerService.resume(config, forbiddenGenerators(), phases::add)) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
            assertEquals(144, service.snapshot().generation());
            assertEquals(1, service.snapshot().totals().completedGenerations());
            assertEquals(0, service.snapshot().totals().selfPlayGames());
            assertFalse(service.config().corpusTraining().viewIdentity().isEmpty());
            var prepared = phases.stream().filter(s -> s.state() == TrainerSnapshot.State.PREPARING_CORPUS).findFirst().orElseThrow();
            assertEquals(143, prepared.generation()); assertEquals(best, prepared.bestId());
            assertTrue(phases.stream().noneMatch(s -> s.state() == TrainerSnapshot.State.GENERATING_SELF_PLAY));
        }
        var history = new com.ohinteractive.seedv6.training.history.HistoryRepository(config.checkpointRoot()).refresh();
        assertEquals(144, history.records().getFirst().generation()); assertTrue(history.warnings().isEmpty());
        assertFalse(Files.exists(binding.resolve("records.idx.pending")));
        assertFalse(Files.exists(binding.resolve("view.json.pending")));
    }

    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "BRN2"})
    void stopDuringCorpusScanCleansScratchAndNextServiceResumes(TrainingArchitecture architecture) throws Exception {
        var config = config(architecture, corpus());
        String best = initialize(config);
        var owned = new AtomicReference<TrainerService>(); var cancelled = new AtomicBoolean();
        var work = new TrainerService.Operations() {
            @Override CorpusTraining openCorpus(TrainerConfig c, TrainingSource source, boolean mayCreate,
                    CorpusPreparation preparation) throws IOException {
                return super.openCorpus(c, source, mayCreate, new CorpusPreparation(cancelled::get, progress -> {
                    if (progress.stage().equals("Examining corpus positions") && progress.completed() > 0) {
                        cancelled.set(true); owned.get().stop();
                    }
                }));
            }
        };
        try (var service = TrainerService.resume(config, work, s -> {})) {
            owned.set(service); service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertTrue(cancelled.get()); assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
            assertEquals(TrainerSnapshot.State.STOPPED, service.snapshot().state());
            assertEquals(143, service.snapshot().generation()); assertEquals(best, service.snapshot().bestId());
        }
        Path binding = config.checkpointRoot().resolve("corpus-training");
        assertFalse(Files.exists(binding.resolve("records.idx.pending")));
        assertFalse(Files.exists(binding.resolve("view.json.pending")));
        assertFalse(Files.exists(binding.resolve("records.idx")));
        assertTrue(GenerationAttempt.inspect(config.checkpointRoot()).isEmpty());
        assertEquals(best, CheckpointInspection.reference(config.checkpointRoot(), "latest-training"));
        try (var service = TrainerService.resume(config, forbiddenGenerators(), s -> {})) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
            assertEquals(144, service.snapshot().generation()); assertEquals(1, service.snapshot().totals().completedGenerations());
        }
    }

    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "BRN2"})
    void freshCorpusLineageStillInitializesAndGeneratedModeSkipsPreparation(TrainingArchitecture architecture) throws Exception {
        var corpusConfig = config(architecture, corpus());
        try (var service = TrainerService.fresh(corpusConfig, initial(architecture), forbiddenGenerators(), s -> {})) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
            assertEquals(1, service.snapshot().generation()); assertFalse(service.snapshot().bestId().isEmpty());
        }
        var generated = corpusConfig.withCorpusTraining(null).withSource(TrainingSource.SELF_PLAY);
        var owned = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override CorpusTraining openCorpus(TrainerConfig c, TrainingSource source, boolean mayCreate,
                    CorpusPreparation preparation) { throw new AssertionError("Generated mode opened corpus"); }
        };
        try (var service = TrainerService.resume(generated, work, snapshot -> {
            assertNotEquals(TrainerSnapshot.State.PREPARING_CORPUS, snapshot.state());
            if (snapshot.state() == TrainerSnapshot.State.GENERATING_SELF_PLAY) owned.get().stop();
        })) {
            owned.set(service); service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
            assertEquals(2, service.snapshot().generation());
        }
    }

    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "BRN2"})
    void stoppingPreparationPreservesPartialGenerationAndResumeRestoresItsPin(TrainingArchitecture architecture) throws Exception {
        var config = config(architecture, corpus());
        String best = initialize(config);
        var owned = new AtomicReference<TrainerService>();
        try (var service = TrainerService.resume(config, forbiddenGenerators(), snapshot -> {
            if (snapshot.state() == TrainerSnapshot.State.TRAINING) owned.get().stop();
        })) {
            owned.set(service); service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
            assertEquals(144, service.snapshot().generation()); assertEquals(0, service.snapshot().optimizerStep());
        }
        Path root = config.checkpointRoot();
        var partial = PartialGeneration.inspect(root).orElseThrow();
        byte[] attempt = Files.readAllBytes(root.resolve(GenerationAttempt.FILE));
        byte[] receipt = Files.readAllBytes(root.resolve("corpus-training/generation-144.json"));
        var pin = CorpusTraining.readPin(root).orElseThrow();
        var restored = config.withSource(null).withCorpusTraining(null);
        try (var service = TrainerService.resume(restored, forbiddenGenerators(), snapshot -> {
            if (snapshot.state() == TrainerSnapshot.State.PREPARING_CORPUS) {
                assertEquals(144, snapshot.generation());
                owned.get().stop();
            }
        })) {
            owned.set(service); service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
        }
        assertEquals(partial, PartialGeneration.inspect(root).orElseThrow());
        assertArrayEquals(attempt, Files.readAllBytes(root.resolve(GenerationAttempt.FILE)));
        assertArrayEquals(receipt, Files.readAllBytes(root.resolve("corpus-training/generation-144.json")));
        assertEquals(pin, CorpusTraining.readPin(root).orElseThrow());
        assertEquals(best, CheckpointInspection.reference(root, "best"));
        try (var service = TrainerService.resume(restored, forbiddenGenerators(), snapshot -> {})) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(20)));
            assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
            assertEquals(144, service.snapshot().generation());
            assertEquals("Resume", service.snapshot().run().orElseThrow().action());
            assertEquals(pin.identity(), service.config().corpusTraining().viewIdentity());
            assertEquals(pin.positions(), service.config().corpusTraining().positionsPerGeneration());
            assertEquals(1, service.snapshot().totals().completedGenerations());
        }
    }
}
