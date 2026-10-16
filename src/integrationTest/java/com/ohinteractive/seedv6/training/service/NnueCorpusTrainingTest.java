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
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class NnueCorpusTrainingTest {
    @TempDir Path temp;
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false,true})
    void materialIdentityTrainsValidatesPublishesAndResumesThroughOrdinaryService(boolean calibrated) throws Exception {
        Path source = corpus("material-source", 24, false), root = temp.resolve("material-output");
        var legacyConfig = config(root, source, 71, 6, 1, 2);
        var config = new TrainerConfig(root,71,legacyConfig.selfPlay(),legacyConfig.training(),legacyConfig.validation(),
                1,TrainerConfig.DepthChange.REQUIRE_SAME,TrainerConfig.STANDARD_START,TrainingArchitecture.NNUE_MATERIAL,.001)
                .withSource(legacyConfig.source()).withCorpusTraining(legacyConfig.corpusTraining())
                .withValidationMethod(ValidationMethod.HELD_OUT);
        var trainer = calibrated ? NnueTrainer.calibratedMaterialParity(TrainableNnue.initialized(71)) : NnueTrainer.materialParity(TrainableNnue.initialized(71));
        try (var service = TrainerService.fresh(config, trainer, forbidden(), s -> {})) {
            var result = finish(service);
            assertEquals(12,result.training().orElseThrow().samplesTrained());
            assertEquals(4,result.training().orElseThrow().optimizerUpdates());
            assertEquals(0,result.totals().selfPlayGames());
        }
        try (var store = new CheckpointStore(root,TrainingArchitecture.NNUE_MATERIAL)) {
            var checkpoint = store.recover().latestTraining().orElseThrow();
            assertInstanceOf(NetworkModel.NnueMaterial.class,checkpoint.model());
            var resumed = (NetworkTrainingState.NnueMaterial)store.resumeState(checkpoint.manifest().id());
            assertTrue(resumed.trainer().materialBootstrap());
            assertEquals(calibrated,resumed.trainer().calibratedOutcome());
            assertEquals(4,resumed.step());
            assertTrue(store.validationFor(checkpoint.manifest().id()).isPresent());
        }
        try (var service = TrainerService.resume(config, forbidden(), s -> {})) { finish(service); }
    }
    static final CorpusWriter.SourceInfo SOURCE = new CorpusWriter.SourceInfo("test", "nnue-corpus", "{}", "depth-then-work");
    static CorpusRecord record(int clock, int kind, int value, int perspective, int depth) {
        return new CorpusRecord(CorpusPosition.fromFen("4k3/8/8/8/3pP3/8/8/4K3 "
                + (clock % 2 == 0 ? "w" : "b") + " - - " + clock + " 1"), kind, value, perspective, depth, -1, 0, 1, clock + 1);
    }
    Path corpus(String name, int count, boolean mixed) throws Exception {
        Path root = temp.resolve(name);
        try (var writer = new CorpusWriter(root, 7, SOURCE)) {
            for (int i = 0; i < count; i++) writer.put(record(i, mixed && i % 4 == 0 ? CorpusRecord.MATE : CorpusRecord.CP,
                    i * 30 - 400, CorpusRecord.WHITE, 20));
            writer.put(record(count, CorpusRecord.MATE, 0, CorpusRecord.WHITE, 20));
            writer.put(record(count + 1, CorpusRecord.CP, 100, CorpusRecord.RAW_SOURCE, 20));
            writer.checkpoint(true);
        }
        return root;
    }
    TrainerConfig config(Path root, Path corpus, long seed, int count, long generations, int epochs) {
        return new TrainerConfig(root, seed,
                new TrainerConfig.SelfPlay(1, 1, 4, 0, 0, 1, 8, com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1),
                new TrainerConfig.Training(epochs, 4, true),
                new TrainerConfig.Validation(2, 0, 0, 1, 1, 8, com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                generations, TrainerConfig.DepthChange.REQUIRE_SAME)
                .withSource(TrainingSource.corpus(corpus)).withCorpusTraining(new CorpusTrainingConfig(count))
                .withValidationMethod(ValidationMethod.HELD_OUT);
    }
    static TrainerService.Operations forbidden() {
        return new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig c, long[] b, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) { throw new AssertionError("Corpus called network generator"); }
            @Override SelfPlayBatch generate(NnueNetwork actor, SelfPlayConfig c, long[] b, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) { throw new AssertionError("Corpus called NNUE generator"); }
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig c, long[] b, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) { throw new AssertionError("Corpus called handcrafted generator"); }
        };
    }
    static TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(90)));
        assertTrue(service.failure().isEmpty(), () -> service.failure().toString());
        assertEquals(TrainerSnapshot.State.STOPPED, service.snapshot().state());
        assertTrue(service.historyWarning().isEmpty()); return service.snapshot();
    }
    static byte[] state(Path root, String id) throws Exception {
        try (var store = new CheckpointStore(root)) {
            var out = new ByteArrayOutputStream(); store.resumeState(id).write(out); return out.toByteArray();
        }
    }
    @Test void sharedSelectionHasDeterministicDisjointEpochsAndPinnedAppendResume() throws Exception {
        Path corpus = corpus("source", 50, true); var config = config(temp.resolve("one"), corpus, 71, 8, 1, 1);
        CorpusTraining.Evidence first; String identity, rollover;
        try (var source = new CorpusTraining(config, config.source(), true)) {
            first = source.batch(1).evidence(); identity = source.pin().identity();
            assertEquals(NnueCorpusTargets.ID, first.adapterIdentity());
            assertEquals(Map.of("mate-zero", 1L, "unsupported-perspective", 1L), first.viewSkipped());
            var all = new HashSet<String>(); var held = new HashSet<String>();
            // 50 eligible identities: 10 reserved, one training epoch spans exactly five generations.
            for (int g = 1; g <= 5; g++) {
                var batch = source.batch(g);
                for (var sample : batch.training().samples()) assertTrue(all.add(Arrays.toString(sample.board())));
                for (var sample : batch.validation().samples()) held.add(Arrays.toString(sample.board()));
            }
            assertTrue(Collections.disjoint(all, held)); assertEquals(40, all.size());
            rollover = source.batch(6).evidence().trainingHash(); assertNotEquals(first.trainingHash(), rollover);
            assertEquals(first, source.batch(1).evidence());
        }
        try (var source = new CorpusTraining(config(temp.resolve("same"), corpus, 71, 8, 1, 1), config.source(), true)) {
            assertEquals(identity, source.pin().identity()); assertEquals(first.trainingHash(), source.batch(1).evidence().trainingHash());
            assertEquals(rollover, source.batch(6).evidence().trainingHash());
        }
        try (var source = new CorpusTraining(config(temp.resolve("different"), corpus, 72, 8, 1, 1), config.source(), true)) {
            assertEquals(identity, source.pin().identity()); assertNotEquals(first.trainingHash(), source.batch(1).evidence().trainingHash());
        }
        try (var writer = new CorpusWriter(corpus, 7, SOURCE)) {
            writer.put(record(0, CorpusRecord.CP, 800, CorpusRecord.WHITE, 40));
            writer.put(record(100, CorpusRecord.CP, 900, CorpusRecord.WHITE, 20)); writer.checkpoint(true);
        }
        try (var source = new CorpusTraining(config, config.source(), false)) { assertEquals(first, source.batch(1).evidence()); }
        try (var source = new CorpusTraining(config.withCorpusTraining(new CorpusTrainingConfig(9)), config.source(), true)) {
            assertNotEquals(identity, source.pin().identity());
        }
        Path shard; try (var reader = new CorpusReader(corpus)) { shard = corpus.resolve("shards").resolve(reader.shards().getFirst().file()); }
        try (var f = new RandomAccessFile(shard.toFile(), "rw")) { f.seek(70); f.writeByte(f.readByte() ^ 1); }
        assertThrows(IOException.class, () -> new CorpusTraining(config, config.source(), false));
    }
    @Test void corpusUsesProductionFeatureKernelsTargetsAndRealMinibatchOptimizer() throws Exception {
        Path corpus = corpus("source", 48, true), output = temp.resolve("training");
        var config = config(output, corpus, 71, 12, 1, 2);
        var initial = new NnueTrainer(TrainableNnue.initialized(71));
        var isolated = config(temp.resolve("feature-check"), corpus, 71, 12, 1, 2);
        try (var source = new CorpusTraining(isolated, isolated.source(), true)) {
            var examples = source.batch(1).training();
            var evaluator = new NnueEvaluator(initial.model().snapshot());
            for (var sample : examples.samples()) {
                long[] board = sample.board();
                int clock = Board.halfMoveClock((int) board[Board.STATUS]);
                var record = record(clock, clock % 4 == 0 ? CorpusRecord.MATE : CorpusRecord.CP, clock * 30 - 400, CorpusRecord.WHITE, 20);
                assertArrayEquals(Board.fromFen(record.position().fen() + " 1"), board);
                evaluator.evaluate(board); assertEquals(evaluator.boundedValue(), initial.predict(board));
                assertEquals(NnueCorpusTargets.target(record, board), examples.targets().applyAsDouble(sample));
            }
        }
        // The view prepared above is recognized only after production initialization.
        try (var store = new CheckpointStore(output)) { store.initialize(initial, new CheckpointManifest.Metadata(0, 1, "")); store.writeTrainingSource(config.source()); }
        try (var service = TrainerService.resume(config, forbidden(), s -> {})) {
            var result = finish(service); var training = result.training().orElseThrow();
            assertEquals(24, training.samplesTrained()); assertEquals(6, training.optimizerUpdates());
            assertEquals(0, result.totals().selfPlayGames()); assertEquals(0, result.selfPlay().requestedGames());
            assertTrue(Double.isFinite(training.finalLoss())); assertNotEquals(training.initialLoss(), training.finalLoss());
            assertEquals(NnueCorpusTargets.ID, service.config().corpusTraining().targetAdapter());
            assertEquals(NnueCorpusTargets.ID, service.corpusReport().orElseThrow().adapterIdentity());
            assertTrue(service.corpusReport().orElseThrow().mateExamples() > 0);
            assertEquals(service.corpusReport().orElseThrow(), result.run().orElseThrow().corpus());
            var records = new HistoryRepository(output).refresh(); assertTrue(records.warnings().isEmpty());
            assertEquals(NnueCorpusTargets.ID, records.records().getFirst().bootstrap().corpus().adapterIdentity());
            assertTrue(records.records().getFirst().regime().effectiveSettings().contains(NnueCorpusTargets.ID));
            try (var store = new CheckpointStore(output)) {
                var latest = store.load(result.latestTrainingId());
                var parent = store.load(latest.manifest().parentId());
                assertNotEquals(parent.manifest().networkSha256(), latest.manifest().networkSha256());
                assertEquals(NnueCorpusTargets.ID, store.validationFor(latest.manifest().id()).orElseThrow().bootstrap().corpus().adapterIdentity());
            }
        }
    }
    @Test void optimizerBoundaryStopAndResumeAreBitExactWithShuffleAndMultipleEpochs() throws Exception {
        Path corpus = corpus("source", 48, true), continuous = temp.resolve("continuous"), split = temp.resolve("split");
        TrainerSnapshot a, b;
        try (var service = TrainerService.fresh(config(continuous, corpus, 71, 12, 2, 2),
                new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(71))), forbidden(), s -> {})) { a = finish(service); }
        var owner = new AtomicReference<TrainerService>();
        var stopping = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainCorpus(NetworkTrainingState state, CorpusTraining.Examples examples,
                    SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainCorpus(state, examples, config, control, p -> { observer.accept(p); owner.get().stop(); });
            }
            @Override SelfPlayBatch generate(NetworkModel a, SelfPlayConfig c, long[] b, SelfPlayControl k, Consumer<SelfPlayBatch.Progress> o) { throw new AssertionError(); }
        };
        try (var service = TrainerService.fresh(config(split, corpus, 71, 12, 2, 2),
                new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(71))), stopping, s -> {})) {
            owner.set(service); assertEquals(1, finish(service).optimizerStep());
        }
        try (var service = TrainerService.resume(config(split, corpus, 71, 12, 2, 2), forbidden(), s -> {})) { b = finish(service); }
        assertEquals(a.latestTrainingId(), b.latestTrainingId());
        assertArrayEquals(state(continuous, a.latestTrainingId()), state(split, b.latestTrainingId()));
    }
    @Test void publishedCandidateRecoveryAndChangedBindingKeepGenerationEvidenceTruthful() throws Exception {
        Path corpus = corpus("source", 48, true), output = temp.resolve("training"); var config = config(output, corpus, 71, 12, 1, 1);
        var interrupt = new TrainerService.Operations() {
            @Override CheckpointStore.Checkpoint publish(CheckpointStore store, NetworkTrainingState state, CheckpointManifest.Metadata metadata) throws IOException {
                super.publish(store, state, metadata); throw new IOException("Interrupted after corpus Candidate");
            }
        };
        try (var service = TrainerService.fresh(config, new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(71))), interrupt, s -> {})) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(90))); assertTrue(service.failure().isPresent());
        }
        var first = CorpusTraining.evidence(output, 1);
        var recovering = new AtomicReference<TrainerService>();
        try (var service = TrainerService.resume(config.withSource(null).withCorpusTraining(null), forbidden(), s -> {
            if (s.state() == TrainerSnapshot.State.RECORDING_DECISION) recovering.get().stop();
        })) {
            recovering.set(service);
            var result = finish(service); assertEquals(1, result.totals().recoveredLifecycles()); assertEquals(0, result.totals().optimizerUpdates());
        }
        var changed = config(output, corpus, 72, 9, 1, 1);
        try (var service = TrainerService.resume(changed, forbidden(), s -> {})) { finish(service); }
        assertEquals(first, CorpusTraining.evidence(output, 1));
        assertEquals(72, CorpusTraining.evidence(output, 2).seed()); assertEquals(9, CorpusTraining.evidence(output, 2).requested());
        var records = new HistoryRepository(output).refresh(); assertTrue(records.warnings().isEmpty()); assertEquals(1, records.records().size());
        assertEquals(2, records.records().getFirst().generation());
        // Existing recovery policy does not invent missing analytics measurements. The recovered
        // generation retains its immutable selection and self-contained validation provenance.
        try (var store = new CheckpointStore(output)) {
            var parent = store.load(store.recover().latestTraining().orElseThrow().manifest().parentId());
            assertEquals(first, store.validationFor(parent.manifest().id()).orElseThrow().bootstrap().corpus());
        }
        // Recovery through the changed configuration's nested binding reads the shared receipt location.
        try (var source = new SequentialTraining(changed, changed.source(), CorpusPreparation.NONE)) { assertEquals(3, source.validation(2).samples().size()); }
    }
    @Test void gamePairValidationRemainsIndependentAndGeneratesNoTrainingGames() throws Exception {
        Path corpus = corpus("source", 24, false), output = temp.resolve("games");
        var validationCalls = new java.util.concurrent.atomic.AtomicInteger();
        var operations = new TrainerServiceTest.Matches(false) {
            @Override ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig c,
                    long[] b, com.ohinteractive.seedv6.rules.GameHistory h, ValidationControl control, Consumer<ValidationProgress> observer) {
                validationCalls.incrementAndGet(); return TrainerServiceTest.outcome(c, b, h, false);
            }
            @Override SelfPlayBatch generate(NetworkModel a, SelfPlayConfig c, long[] b, SelfPlayControl k, Consumer<SelfPlayBatch.Progress> o) { throw new AssertionError(); }
            @Override SelfPlayBatch generate(NnueNetwork a, SelfPlayConfig c, long[] b, SelfPlayControl k, Consumer<SelfPlayBatch.Progress> o) { throw new AssertionError(); }
        };
        var config = config(output, corpus, 71, 12, 1, 1).withValidationMethod(ValidationMethod.GAME_PAIRS);
        try (var service = TrainerService.fresh(config, new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(71))), operations, s -> {})) {
            var result = finish(service); assertEquals(1, validationCalls.get()); assertTrue(result.validation().isPresent());
            assertEquals(0, result.totals().selfPlayGames()); assertEquals(3, result.totals().optimizerUpdates());
            assertEquals(ValidationMethod.GAME_PAIRS, service.config().validationMethod(config.source()));
        }
        var record = new HistoryRepository(output).refresh().records().getFirst();
        assertEquals("EXTERNAL_CORPUS", record.regime().positionSource());
        assertTrue(record.regime().effectiveSettings().contains(NnueCorpusTargets.ID));
    }
    @Test void diagnosticPrefixUsesExistingPinnedViewWithoutScanningOrBindingOtherShards() throws Exception {
        Path corpus = corpus("source", 50, false), output = temp.resolve("prefix");
        try (var reader = new CorpusReader(corpus)) {
            CorpusView.create(reader, output.resolve("corpus-training"), CorpusTraining.NNUE_POLICY, NnueCorpusTargets::rejection, 8);
        }
        var config = config(output, corpus, 71, 4, 1, 1);
        try (var source = new CorpusTraining(config, config.source(), true)) {
            var evidence = source.batch(1).evidence(); assertEquals(8, evidence.viewExamined());
            assertEquals(44L, evidence.viewSkipped().get("diagnostic-unexamined"));
            assertEquals(evidence, source.batch(1).evidence());
        }
    }
}
