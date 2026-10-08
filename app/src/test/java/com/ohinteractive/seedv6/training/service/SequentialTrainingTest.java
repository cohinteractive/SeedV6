package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class SequentialTrainingTest {
    @TempDir Path temporary;
    Path file(String name, int count) throws Exception {
        Path file = temporary.resolve(name + ".jsonl"); var text = new StringBuilder();
        for (int i = 0; i < count; i++) text.append(SourceReadersTest.line(i)); Files.writeString(file, text); return file;
    }
    TrainerConfig config(Path root, TrainingArchitecture architecture, int count, int generations, Path... inputs) throws Exception {
        var sources = new ArrayList<DataSource>(); for (Path file : inputs) sources.add(DataSource.register(file.getFileName().toString(), file, 1));
        try (var store = new CheckpointStore(root, architecture)) { new DataSources(1, sources, false).save(DataSources.directory(root)); }
        var base = Brn2TrainerServiceTest.config(root, generations);
        if (architecture == TrainingArchitecture.NNUE) base = new TrainerConfig(root, 71, base.selfPlay(), new TrainerConfig.Training(1, 4, true), base.validation(), generations, base.depthChange());
        return base.withSource(TrainingSource.dataSources(DataSources.directory(root))).withCorpusTraining(new CorpusTrainingConfig(count)).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    static TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(90))); assertTrue(service.failure().isEmpty(), () -> service.failure().toString()); return service.snapshot();
    }
    static NetworkTrainingState initial(TrainingArchitecture architecture) { return architecture == TrainingArchitecture.NNUE
            ? new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(71))) : new NetworkTrainingState.Brn2(new Brn2Trainer(.001)); }
    @Test void rangeReservationIsDeterministicAcrossRestartAndMixHasIndependentCursors() throws Exception {
        Path a = file("a", 100), b = file("b", 110), root = temporary.resolve("lineage");
        var config = config(root, TrainingArchitecture.BRN2, 8, 1, a, b); CorpusTraining.Evidence evidence;
        try (var training = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) {
            evidence = training.batch(1).evidence(); assertEquals(10, training.metrics().decodedRecords());
        }
        var ledger = new SourceLedger(root); var reservation = ledger.active().orElseThrow(); assertEquals(2, reservation.ranges().size());
        for (var range : reservation.ranges()) { assertEquals(0, range.start()); assertEquals(5, range.end()); }
        try (var training = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) { assertEquals(evidence, training.batch(1).evidence()); training.complete(1); }
        try (var training = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) { training.batch(2); }
        assertEquals(5, new SourceLedger(root).active().orElseThrow().ranges().getFirst().start());
        assertFalse(Files.exists(root.resolve("corpus-training/records.idx"))); assertFalse(Files.exists(root.resolve("corpus-training/view.json")));
    }
    @Test void relativeWeightsDriveActualAcceptedTrainingRangesAndDoNotChangeLibraryWeights() throws Exception {
        Path a = file("stockfish", 100), b = file("lichess", 100), root = temporary.resolve("weighted");
        var cfg = config(root, TrainingArchitecture.BRN2, 8, 1, a, b);
        var first = DataSource.register("Stockfish", a, 3); var second = DataSource.register("Lichess", b, 1);
        new DataSources(1, List.of(first, second), false).save(DataSources.directory(root));
        new TrainingDataLibrary(temporary).register(first);
        assertEquals(1, new TrainingDataLibrary(temporary).browse().sources().getFirst().weight());
        String hash;
        try (var training = new SequentialTraining(cfg, cfg.source(), CorpusPreparation.NONE)) {
            assertEquals(8, training.batch(1).training().samples().size());
            var reserved = new SourceLedger(root).active().orElseThrow();
            assertEquals(List.of(6, 2), reserved.ranges().stream().map(SourceLedger.Range::training).toList());
            hash = reserved.trainingHash();
        }
        try (var training = new SequentialTraining(cfg, cfg.source(), CorpusPreparation.NONE)) {
            training.batch(1); assertEquals(hash, new SourceLedger(root).active().orElseThrow().trainingHash());
        }
    }
    @Test void exhaustionDoesNotWrapOrPartiallyReserveOtherSource() throws Exception {
        Path a = file("short", 2), b = file("long", 50), root = temporary.resolve("lineage"); var config = config(root, TrainingArchitecture.BRN2, 8, 1, b, a);
        try (var training = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) { assertThrows(java.io.EOFException.class, () -> training.batch(1)); }
        assertTrue(new SourceLedger(root).state().reservations().isEmpty());
    }
    @Test void missingLedgerCannotResetAnEstablishedCampaign() throws Exception {
        Path root = temporary.resolve("missing-ledger"); var config = config(root, TrainingArchitecture.BRN2, 4, 1, file("data", 20));
        try (var training = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) { training.batch(1); training.complete(1); }
        Files.delete(root.resolve("training-data/cursors.json")); // only the test fixture: simulate lost durable metadata
        assertTrue(assertThrows(java.io.IOException.class, () -> new SequentialTraining(config, config.source(), CorpusPreparation.NONE)).getMessage().contains("no cursor was reset"));
    }
    @Test void bothArchitecturesUseCommonSourceWithoutSelfPlayAndSettleCursors() throws Exception {
        Path data = file("data", 60);
        for (var architecture : List.of(TrainingArchitecture.NNUE, TrainingArchitecture.BRN2)) {
            Path root = temporary.resolve(architecture.name()); var config = config(root, architecture, 4, 2, data);
            try (var service = TrainerService.fresh(config, initial(architecture), NnueCorpusTrainingTest.forbidden(), s -> {})) {
                var result = finish(service); assertEquals(2, result.totals().completedGenerations()); assertEquals(0, result.totals().selfPlayGames());
            }
            var ledger = new SourceLedger(root); assertEquals(12, ledger.state().next().values().iterator().next());
            assertTrue(ledger.state().reservations().stream().allMatch(r -> r.status() == SourceLedger.Status.COMPLETED));
        }
    }
    @Test void optimizerStopResumeAndExplicitRestartPreserveOrAbandonExactRanges() throws Exception {
        Path data = file("data", 80), root = temporary.resolve("lineage"); var config = config(root, TrainingArchitecture.BRN2, 8, 1, data);
        var owner = new AtomicReference<TrainerService>();
        var stopping = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainCorpus(NetworkTrainingState state, CorpusTraining.Examples examples, SelfPlayTraining.Config c, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainCorpus(state, examples, c, control, p -> { observer.accept(p); owner.get().stop(); });
            }
        };
        try (var service = TrainerService.fresh(config, initial(TrainingArchitecture.BRN2), stopping, s -> {})) { owner.set(service); finish(service); }
        var reserved = new SourceLedger(root).active().orElseThrow();
        try (var service = TrainerService.resume(config, NnueCorpusTrainingTest.forbidden(), s -> {})) { finish(service); }
        assertEquals(reserved.ranges(), new SourceLedger(root).generation(1).orElseThrow().ranges());
        try (var service = TrainerService.resume(config, stopping, s -> {})) { owner.set(service); finish(service); }
        var next = new SourceLedger(root).active().orElseThrow();
        try (var service = TrainerService.resume(config.withCorpusTraining(new CorpusTrainingConfig(4)), NnueCorpusTrainingTest.forbidden(), s -> {})) { finish(service); }
        var reservations = new SourceLedger(root).state().reservations(); assertEquals(SourceLedger.Status.ABANDONED, reservations.get(1).status());
        assertEquals(next.ranges().getFirst().end(), reservations.getLast().ranges().getFirst().start());
    }
    @Test void gamePairValidationStillUsesProductionArena() throws Exception {
        Path root = temporary.resolve("games"); var config = config(root, TrainingArchitecture.BRN2, 4, 1, file("data", 20)).withValidationMethod(ValidationMethod.GAME_PAIRS);
        try (var service = TrainerService.fresh(config, initial(TrainingArchitecture.BRN2), NnueCorpusTrainingTest.forbidden(), s -> {})) {
            var result = finish(service); assertTrue(result.validation().isPresent()); assertEquals(1, result.totals().completedGenerations());
            assertEquals(4, new SourceLedger(root).state().next().values().iterator().next());
        }
    }
    @Test void boundedLocalGeneration() throws Exception {
        String location = System.getenv("SEED_TRAINING_SMOKE_SOURCE"); Assumptions.assumeTrue(location != null);
        Path root = temporary.resolve("local-generation");
        var config = config(root, TrainingArchitecture.NNUE, 10000, 1, Path.of(location));
        long started = System.nanoTime();
        try (var training = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) {
            var batch = training.batch(1); assertEquals(10000, batch.training().samples().size()); assertEquals(2500, batch.validation().samples().size());
            assertTrue(training.metrics().decodedRecords() < 20000);
            System.out.printf(java.util.Locale.ROOT, "BOUNDED_GENERATION_SMOKE training=%d validation=%d decoded=%d seek_records=%d seconds=%.3f%n", batch.training().samples().size(), batch.validation().samples().size(), training.metrics().decodedRecords(), training.metrics().seekRecords(), (System.nanoTime() - started) / 1e9);
        }
        assertFalse(Files.exists(root.resolve("corpus-training/records.idx")));
    }
    @Test void nnueOptimizerContinuationIsBitExactAndCrashBeforeReceiptReplaysReservation() throws Exception {
        Path data = file("many", 80), one = temporary.resolve("one"), split = temporary.resolve("split");
        var a = config(one, TrainingArchitecture.NNUE, 8, 1, data); var b = config(split, TrainingArchitecture.NNUE, 8, 1, data);
        TrainerSnapshot expected, actual;
        try (var service = TrainerService.fresh(a, initial(TrainingArchitecture.NNUE), NnueCorpusTrainingTest.forbidden(), s -> {})) { expected = finish(service); }
        var owner = new AtomicReference<TrainerService>();
        var stop = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainCorpus(NetworkTrainingState state, CorpusTraining.Examples examples, SelfPlayTraining.Config c, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainCorpus(state, examples, c, control, p -> { observer.accept(p); owner.get().stop(); });
            }
        };
        try (var service = TrainerService.fresh(b, initial(TrainingArchitecture.NNUE), stop, s -> {})) { owner.set(service); finish(service); }
        var reservation = new SourceLedger(split).active().orElseThrow();
        Files.delete(split.resolve("corpus-training/generation-1.json")); // crash after ledger publication, before receipt publication
        try (var service = TrainerService.resume(b, NnueCorpusTrainingTest.forbidden(), s -> {})) { actual = finish(service); }
        assertEquals(expected.latestTrainingId(), actual.latestTrainingId());
        assertArrayEquals(NnueCorpusTrainingTest.state(one, expected.latestTrainingId()), NnueCorpusTrainingTest.state(split, actual.latestTrainingId()));
        assertEquals(reservation.ranges(), new SourceLedger(split).generation(1).orElseThrow().ranges());
    }
}
