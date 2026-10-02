package com.ohinteractive.seedv6.training.service;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class BrnCorpusTrainingTest {
    @TempDir Path temp;
    static final CorpusWriter.SourceInfo SOURCE = new CorpusWriter.SourceInfo("test", "corpus-training", "{}", "depth-then-work");
    static CorpusRecord record(int clock, int kind, int target, int perspective, int depth) {
        return new CorpusRecord(CorpusPosition.fromFen("4k3/8/8/8/3pP3/8/8/4K3 " + (clock % 2 == 0 ? "w" : "b") + " - - " + clock + " 1"),
                kind, target, perspective, depth, -1, CorpusRecord.UNKNOWN_WORK, 1, clock + 1);
    }
    Path fixture(int cp) throws Exception {
        Path corpus = temp.resolve("corpus");
        try (var writer = new CorpusWriter(corpus, 7, SOURCE)) {
            for (int i = 0; i < cp; i++) writer.put(record(i, CorpusRecord.CP, i * 30 - 200, CorpusRecord.WHITE, 20));
            writer.put(record(cp, CorpusRecord.MATE, 3, CorpusRecord.WHITE, 20));
            writer.put(record(cp + 1, CorpusRecord.CP, 100, CorpusRecord.RAW_SOURCE, 20));
            writer.put(record(cp + 2, CorpusRecord.CP, 40000, CorpusRecord.WHITE, 20));
            writer.checkpoint(true);
        }
        return corpus;
    }
    TrainerConfig config(Path root, Path corpus, int positions, long generations) {
        return Brn2TrainerServiceTest.config(root, generations).withSource(TrainingSource.corpus(corpus))
                .withCorpusTraining(new CorpusTrainingConfig(positions)).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    @Test void cpPerspectiveCanonicalFeaturesAndSafeLabelPolicy() throws Exception {
        for (int side = 0; side < 2; side++) {
            var white = record(side, CorpusRecord.CP, 650, CorpusRecord.WHITE, 20);
            var stm = record(side, CorpusRecord.CP, 650, CorpusRecord.SIDE_TO_MOVE, 20);
            assertEquals((side == 0 ? 650 : -650) / 32511.0, BrnCorpusTraining.target(white));
            assertEquals(650 / 32511.0, BrnCorpusTraining.target(stm));
            var fromCorpus = white.position().toBoard(0);
            var fromSeed = Board.fromFen(white.position().fen() + " 1");
            assertArrayEquals(fromSeed, fromCorpus);
            var a = new Brn2Features(); var b = new Brn2Features(); a.extract(fromSeed); b.extract(fromCorpus);
            assertEquals(a.size(), b.size());
            for (int i = 0; i < a.size(); i++) assertEquals(a.indexAt(i), b.indexAt(i));
        }
        assertEquals("mate", BrnCorpusTraining.rejection(record(0, CorpusRecord.MATE, 3, CorpusRecord.WHITE, 20)));
        assertEquals("cp-out-of-range", BrnCorpusTraining.rejection(record(0, CorpusRecord.CP, Integer.MIN_VALUE, CorpusRecord.WHITE, 20)));
        assertEquals("unsupported-perspective", BrnCorpusTraining.rejection(record(0, CorpusRecord.CP, 1, CorpusRecord.RAW_SOURCE, 20)));
        assertEquals(1, BrnCorpusTraining.target(record(0, CorpusRecord.CP, 32511, CorpusRecord.SIDE_TO_MOVE, 20)));
    }
    @Test void fixedViewSurvivesAppendAndStrongerRevisionAndRejectsChangedShard() throws Exception {
        Path corpus = fixture(20), root = temp.resolve("campaign");
        var config = config(root, corpus, 8, 1);
        BrnCorpusTraining.Evidence before;
        String viewIdentity;
        try (var source = new BrnCorpusTraining(config, config.source(), true)) {
            before = source.batch(1).evidence();
            viewIdentity = source.config().viewIdentity();
            assertEquals(23, before.viewExamined()); assertEquals(Map.of("mate", 1L, "unsupported-perspective", 1L, "cp-out-of-range", 1L), before.viewSkipped());
            var batch = source.batch(1);
            Set<String> trainingBoards = new HashSet<>();
            for (var sample : batch.training().samples()) trainingBoards.add(Arrays.toString(sample.board()));
            assertEquals(8, trainingBoards.size());
            for (var sample : batch.validation().samples()) assertFalse(trainingBoards.contains(Arrays.toString(sample.board())));
            for (var sample : source.batch(2).training().samples()) assertFalse(trainingBoards.contains(Arrays.toString(sample.board())));
            assertNotEquals(before.trainingHash(), source.batch(2).evidence().trainingHash());
        }
        var other = config(temp.resolve("other-seed"), corpus, 8, 1).withRunSeeds(new BrnRunSeeds(72,72));
        try (var source = new BrnCorpusTraining(other, other.source(), true)) {
            assertEquals(viewIdentity, source.config().viewIdentity());
            assertNotEquals(before.trainingHash(), source.batch(1).evidence().trainingHash());
        }
        try (var writer = new CorpusWriter(corpus, 7, SOURCE)) {
            writer.put(record(0, CorpusRecord.CP, -900, CorpusRecord.WHITE, 40));
            writer.put(record(100, CorpusRecord.CP, 700, CorpusRecord.WHITE, 20)); writer.checkpoint(true);
        }
        try (var source = new BrnCorpusTraining(config, config.source(), false)) { assertEquals(before, source.batch(1).evidence()); }
        assertThrows(IOException.class, () -> new BrnCorpusTraining(config.withCorpusTraining(new CorpusTrainingConfig(9)), config.source(), false));
        Path shard;
        try (var reader = new CorpusReader(corpus)) { shard = corpus.resolve("shards").resolve(reader.shards().getFirst().file()); }
        try (var file = new RandomAccessFile(shard.toFile(), "rw")) { file.seek(70); file.writeByte(file.readByte() ^ 1); }
        assertThrows(IOException.class, () -> new BrnCorpusTraining(config, config.source(), false));
    }
    @Test void corpusLargerGenerationRollsOverAndReplayTargetsRemainExact() throws Exception {
        Path corpus = fixture(4); var cfg = config(temp.resolve("campaign"), corpus, 9, 1);
        try (var source = new BrnCorpusTraining(cfg, cfg.source(), true)) {
            var batch = source.batch(1);
            assertEquals(9, batch.training().samples().size()); assertEquals(3, batch.validation().samples().size());
            assertEquals(batch.evidence(), source.batch(1).evidence());
            for (var sample : batch.training().samples()) {
                int clock = Board.halfMoveClock((int) sample.board()[Board.STATUS]);
                assertEquals(BrnCorpusTraining.target(record(clock, CorpusRecord.CP, clock * 30 - 200, CorpusRecord.WHITE, 20)), batch.training().targets().applyAsDouble(sample));
            }
        }
    }
    static TrainerService.Operations forbiddenGenerator() {
        return new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                throw new AssertionError("Corpus mode invoked network position generation");
            }
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                throw new AssertionError("Corpus mode invoked handcrafted position generation");
            }
        };
    }
    @Test void realOptimizerLifecycleAndBitExactStoppedResumeBypassBothGenerators() throws Exception {
        Path corpus = fixture(24), continuous = temp.resolve("continuous"), split = temp.resolve("split");
        TrainerSnapshot a, b;
        try (var service = TrainerService.fresh(config(continuous, corpus, 8, 2), new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), forbiddenGenerator(), s -> {})) {
            a = Brn2TrainerServiceTest.finish(service);
            assertEquals(16, a.totals().optimizerUpdates()); assertEquals(0, a.totals().completedGames()); assertEquals(2, a.totals().completedGenerations());
            assertNotEquals(a.training().orElseThrow().initialLoss(), a.training().orElseThrow().finalLoss());
            assertTrue(service.historyWarning().isEmpty());
        }
        var owned = new AtomicReference<TrainerService>();
        var work = forbiddenGenerator();
        work = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainCorpus(NetworkTrainingState state, BrnCorpusTraining.Examples examples, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainCorpus(state, examples, control, progress -> { observer.accept(progress); owned.get().stop(); });
            }
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) { throw new AssertionError(); }
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) { throw new AssertionError(); }
        };
        try (var service = TrainerService.fresh(config(split, corpus, 8, 2), new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), work, s -> {})) {
            owned.set(service); var stopped = Brn2TrainerServiceTest.finish(service); assertEquals(1, stopped.optimizerStep());
        }
        try (var service = TrainerService.resume(config(split, corpus, 8, 2), forbiddenGenerator(), s -> {})) { b = Brn2TrainerServiceTest.finish(service); }
        assertEquals(a.latestTrainingId(), b.latestTrainingId());
        assertArrayEquals(Brn2TrainerServiceTest.state(continuous, a.latestTrainingId()), Brn2TrainerServiceTest.state(split, b.latestTrainingId()));
        var history = new HistoryRepository(split).refresh(); assertTrue(history.warnings().isEmpty()); assertEquals(2, history.records().size());
        assertNotNull(history.records().getFirst().bootstrap().corpus());
        assertEquals(0, history.records().getFirst().bootstrap().trainingGames());
    }
    @Test void publishedCandidateRecoveryUsesPinnedHoldoutWithoutGeneratingPositions() throws Exception {
        Path corpus = fixture(12), output = temp.resolve("candidate-recovery");
        var cfg = config(output, corpus, 8, 1);
        var work = new TrainerService.Operations() {
            @Override CheckpointStore.Checkpoint publish(CheckpointStore store, NetworkTrainingState state, CheckpointManifest.Metadata metadata) throws IOException {
                super.publish(store, state, metadata);
                throw new IOException("Simulated interruption after durable corpus Candidate publication");
            }
        };
        try (var service = TrainerService.fresh(cfg, new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), work, s -> {})) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(30))); assertTrue(service.failure().isPresent());
        }
        // Same corpus source is restored; null count recovers the pinned campaign count.
        try (var service = TrainerService.resume(cfg.withCorpusTraining(null), forbiddenGenerator(), s -> {})) {
            var end = Brn2TrainerServiceTest.finish(service);
            assertEquals(1, end.totals().completedGenerations());
            assertEquals(1, end.totals().recoveredLifecycles());
            assertEquals(8, service.config().corpusTraining().positionsPerGeneration());
            assertEquals(0, end.totals().completedGames());
        }
        byte[] source = Files.readAllBytes(output.resolve(CheckpointStore.TRAINING_SOURCE_FILE));
        try (var service = TrainerService.resume(cfg.withSource(null).withSupervision(BrnSupervision.blended(.5)))) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(30)));
            assertTrue(service.failure().orElseThrow().getMessage().contains("Corpus CP supervision"));
        }
        assertArrayEquals(source, Files.readAllBytes(output.resolve(CheckpointStore.TRAINING_SOURCE_FILE)));
    }
    @Test void existingSelfPlayStillUsesItsOriginalGeneratorAndLegacyPriorFailsClosed() throws Exception {
        var called = new boolean[1];
        var owned = new AtomicReference<TrainerService>();
        var work = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                called[0] = true; owned.get().stop(); return super.generate(actor, cfg, board, control, observer);
            }
        };
        try (var service = TrainerService.fresh(Brn2TrainerServiceTest.config(temp.resolve("existing"), 1), new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), work, s -> {})) {
            owned.set(service); Brn2TrainerServiceTest.finish(service); assertTrue(called[0]);
        }
        Path corpus = fixture(8);
        var legacy = new Brn2Trainer(new Brn2Model(71, Brn2MaterialPrior.NONE), new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(.001));
        try (var service = TrainerService.fresh(config(temp.resolve("legacy"), corpus, 8, 1), legacy)) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(30)));
            assertTrue(service.failure().orElseThrow().getMessage().contains("BASIC_V1"));
        }
    }
    @Test void invalidCorpusCannotCreateStudentAndPinCorruptionCannotRebindCampaign() throws Exception {
        Path missing = temp.resolve("absent"), output = temp.resolve("student");
        try (var service = TrainerService.fresh(config(output, missing, 8, 1), new Brn2Trainer(.001))) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(30))); assertTrue(service.failure().isPresent());
            assertFalse(Files.exists(output));
        }
        Path corpus = fixture(8), campaign = temp.resolve("pin"); var cfg = config(campaign, corpus, 8, 1);
        try (var source = new BrnCorpusTraining(cfg, cfg.source(), true)) { assertEquals(8, source.pin().positions()); }
        Path pin = campaign.resolve("corpus-training/campaign.json");
        Files.writeString(pin, Files.readString(pin).replace("\"positions\":8", "\"positions\":9"));
        assertThrows(IOException.class, () -> new BrnCorpusTraining(cfg, cfg.source(), false));
    }
}
