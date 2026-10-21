package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class BrnNextCorpusTrainingTest {
    @TempDir Path temporary;
    TrainerConfig config(Path root, Path data, TrainingArchitecture architecture) throws Exception {
        return config(root, data, architecture, 1);
    }
    TrainerConfig config(Path root, Path data, TrainingArchitecture architecture, int generations) throws Exception {
        try (var store = new CheckpointStore(root, architecture)) {
            new DataSources(1, List.of(DataSource.register("tiny-source", data, 1)), false).save(DataSources.directory(root));
        }
        var base = Brn2TrainerServiceTest.config(root, 1);
        return new TrainerConfig(root, 71, base.selfPlay(), new TrainerConfig.Training(2, 4, true), base.validation(), generations,
                base.depthChange(), TrainerConfig.STANDARD_START, architecture, .003)
                .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                .withCorpusTraining(new CorpusTrainingConfig(12)).withValidationMethod(ValidationMethod.HELD_OUT);
    }

    Path source() throws Exception {
        Path source = temporary.resolve("source.jsonl"); var lines = new StringBuilder();
        for (int i = 0; i < 1200; i++) {
            // Varied placements: a repeated single-position fixture cannot supply disjoint partitions.
            int white = 8 + i % 40, black = 8 + (i / 40) % 40;
            if (white == black) continue;
            char[] squares = new char[64]; Arrays.fill(squares, ' '); squares[4] = 'K'; squares[60] = 'k';
            squares[white] = 'N'; squares[black] = 'p'; var fen = new StringBuilder();
            for (int rank = 7; rank >= 0; rank--) {
                int empty = 0;
                for (int file = 0; file < 8; file++) {
                    char piece = squares[rank * 8 + file];
                    if (piece == ' ') empty++; else { if (empty > 0) fen.append(empty); empty = 0; fen.append(piece); }
                }
                if (empty > 0) fen.append(empty); if (rank > 0) fen.append('/');
            }
            lines.append(SourceReadersTest.line(i % 100 - 50).replace("4k3/8/8/8/3pP3/8/8/4K3", fen));
        }
        Files.writeString(source, lines);
        return source;
    }

    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"BRN_GATED", "BRN_CUBIC", "BRN_POOLED", "BRN_GEN32_GATED", "BRN_GEN32_CUBIC", "BRN_GEN32_CUBIC_R4", "BRN_GEN32_QUADRATIC", "BRN_GEN32_QUADRATIC_R4"})
    void guiServicePublicationPartialResumeAndNextGenerationKeepExactStateAndOwnCursor(TrainingArchitecture architecture) throws Exception {
        Path source = source();
        var a = config(temporary.resolve("continuous"), source, architecture);
        var b = config(temporary.resolve("split"), source, architecture);
        var probe = config(temporary.resolve("partition-probe"), source, architecture);
        var trainGroups = new HashSet<String>(); var validGroups = new HashSet<String>();
        try (var acquisition = new SequentialTraining(probe, probe.source(), com.ohinteractive.seedv6.corpus.CorpusPreparation.NONE)) {
            for (int generation = 1; generation <= 2; generation++) {
                var batch = acquisition.batch(generation);
                for (var sample : batch.training().samples()) {
                    var position = com.ohinteractive.seedv6.corpus.CorpusPosition.fromFen(com.ohinteractive.seedv6.core.util.Fen.fromBoard(sample.board()));
                    assertEquals(BrnNextPartition.Part.TRAIN, BrnNextPartition.part(position)); trainGroups.add(BrnNextPartition.groupKey(position));
                }
                for (var sample : batch.validation().samples()) {
                    var position = com.ohinteractive.seedv6.corpus.CorpusPosition.fromFen(com.ohinteractive.seedv6.core.util.Fen.fromBoard(sample.board()));
                    assertEquals(BrnNextPartition.Part.VALIDATION, BrnNextPartition.part(position)); validGroups.add(BrnNextPartition.groupKey(position));
                }
                acquisition.complete(generation);
            }
        }
        assertTrue(Collections.disjoint(trainGroups, validGroups));
        TrainerSnapshot expected, actual;
        try (var service = TrainerService.freshInitialized(a, NnueCorpusTrainingTest.forbidden(), s -> {})) {
            expected = NnueCorpusTrainingTest.finish(service);
            assertEquals(24, expected.training().orElseThrow().samplesTrained());
            assertEquals(6, expected.optimizerStep()); assertEquals(0, expected.totals().selfPlayGames());
            assertEquals(CorpusTraining.TargetPolicy.BRN3_CP_WDL_V1.identity, service.corpusReport().orElseThrow().adapterIdentity());
        }
        var provenance = LineageProvenance.read(a.checkpointRoot()).orElseThrow();
        assertEquals(71L, provenance.initializationSeed());
        var owner = new AtomicReference<TrainerService>();
        var stop = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainCorpus(NetworkTrainingState state, CorpusTraining.Examples examples,
                    SelfPlayTraining.Config c, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainCorpus(state, examples, c, control, p -> { observer.accept(p); owner.get().stop(); });
            }
        };
        try (var service = TrainerService.freshInitialized(b, stop, s -> {})) {
            owner.set(service); assertEquals(1, NnueCorpusTrainingTest.finish(service).optimizerStep());
        }
        var reservation = new SourceLedger(b.checkpointRoot()).active().orElseThrow();
        try (var service = TrainerService.resume(b, NnueCorpusTrainingTest.forbidden(), s -> {})) { actual = NnueCorpusTrainingTest.finish(service); }
        assertEquals(expected.latestTrainingId(), actual.latestTrainingId());
        try (var left = new CheckpointStore(a.checkpointRoot(), architecture); var right = new CheckpointStore(b.checkpointRoot(), architecture)) {
            assertArrayEquals(left.resumeState(expected.latestTrainingId()).encode(), right.resumeState(actual.latestTrainingId()).encode());
            var checkpoint = right.load(actual.latestTrainingId());
            assertNotEquals(right.load(checkpoint.manifest().parentId()).manifest().networkSha256(), checkpoint.manifest().networkSha256());
            assertTrue(right.validationFor(checkpoint.manifest().id()).isPresent());
            assertEquals(architecture, checkpoint.model().architecture());
        }
        assertEquals(reservation.ranges(), new SourceLedger(b.checkpointRoot()).generation(1).orElseThrow().ranges());
        try (var service = TrainerService.resume(b, NnueCorpusTrainingTest.forbidden(), s -> {})) {
            assertEquals(12, NnueCorpusTrainingTest.finish(service).optimizerStep());
        }
        var next = new SourceLedger(b.checkpointRoot()).generation(2).orElseThrow();
        assertEquals(reservation.ranges().getFirst().end(), next.ranges().getFirst().start());
        assertThrows(IllegalArgumentException.class, () -> b.withSource(TrainingSource.SELF_PLAY));
        assertEquals(CorpusTraining.TargetPolicy.BT4_Q_V1, CorpusTraining.targetPolicy(architecture, LabelProfile.BT4_Q_V1));
    }

    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"BRN_GATED", "BRN_CUBIC", "BRN_POOLED", "BRN_GEN32_GATED", "BRN_GEN32_CUBIC", "BRN_GEN32_CUBIC_R4", "BRN_GEN32_QUADRATIC", "BRN_GEN32_QUADRATIC_R4"})
    void boundedGuiSessionsPersistFinalDecisionOptimizerAndCursorBeforeResuming(TrainingArchitecture architecture) throws Exception {
        Path source = source(), root = temporary.resolve("bounded"), uninterrupted = temporary.resolve("uninterrupted");
        var first = config(root, source, architecture, 1);
        try (var store = new CheckpointStore(root, architecture)) {
            store.writeLineage(new TrainingLineage(UUID.randomUUID(), "bounded fixture", architecture, java.time.Instant.now(), "", "test"));
        }
        TrainerSnapshot initial;
        try (var service = TrainerService.freshInitialized(first, NnueCorpusTrainingTest.forbidden(), s -> {}).withSession().withIncrementalTraining()) {
            initial = NnueCorpusTrainingTest.finish(service);
        }
        byte[] initialOptimizer = CheckpointStore.readTrainingSnapshot(root, initial.latestTrainingId()).encode();
        var firstSession = TrainingSession.read(root).orElseThrow();
        assertEquals(TrainingSession.State.COMPLETED, firstSession.state());
        var finalValidationSeen = new java.util.concurrent.atomic.AtomicBoolean();
        TrainerSnapshot resumed;
        try (var service = TrainerService.resume(config(root, source, architecture, 2), NnueCorpusTrainingTest.forbidden(), s -> {
            if (s.generation() == 3 && s.state() == TrainerSnapshot.State.VALIDATING) {
                try {
                    var session = TrainingSession.read(root).orElseThrow();
                    assertEquals(1, session.baseline()); assertEquals(2, session.target());
                    assertEquals(1, session.completed()); assertEquals(TrainingSession.State.ACTIVE, session.state());
                    finalValidationSeen.set(true);
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            }
        }).withSession().withIncrementalTraining()) {
            resumed = NnueCorpusTrainingTest.finish(service);
        }
        assertTrue(finalValidationSeen.get()); assertEquals(3, resumed.generation());
        assertEquals(2, resumed.totals().completedGenerations());
        var complete = TrainingSession.read(root).orElseThrow();
        assertNotEquals(firstSession.id(), complete.id());
        assertEquals(TrainingSession.State.COMPLETED, complete.state()); assertEquals(2, complete.completed());
        assertEquals(complete, resumed.run().orElseThrow().session());
        assertEquals(3, GenerationAttempt.inspect(root).orElseThrow().generation());
        assertEquals(3, CheckpointInspection.validations(root).size());
        assertEquals(3, new com.ohinteractive.seedv6.training.history.HistoryRepository(root).refresh().records().size());
        assertTrue(PartialGeneration.inspect(root).isEmpty());
        var ledger = new SourceLedger(root); assertTrue(ledger.active().isEmpty()); assertTrue(ledger.generation(4).isEmpty());
        assertEquals(SourceLedger.Status.COMPLETED, ledger.generation(3).orElseThrow().status());
        try (var store = new CheckpointStore(root, architecture)) {
            assertEquals(3, TrainingSession.settledGeneration(store));
            var decision = store.validationFor(resumed.latestTrainingId()).orElseThrow();
            assertEquals(decision.decision() == com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.PROMOTE,
                    resumed.latestTrainingId().equals(CheckpointInspection.reference(root, "best")));
            assertArrayEquals(initialOptimizer, store.resumeState(initial.latestTrainingId()).encode());
        }
        try (var service = TrainerService.freshInitialized(config(uninterrupted, source, architecture, 3),
                NnueCorpusTrainingTest.forbidden(), s -> {}).withIncrementalTraining()) {
            var expected = NnueCorpusTrainingTest.finish(service);
            assertEquals(expected.latestTrainingId(), resumed.latestTrainingId());
            assertArrayEquals(CheckpointStore.readTrainingSnapshot(uninterrupted, expected.latestTrainingId()).encode(),
                    CheckpointStore.readTrainingSnapshot(root, resumed.latestTrainingId()).encode());
        }
        assertEquals(new SourceLedger(uninterrupted).state().next(), ledger.state().next());
    }
}
