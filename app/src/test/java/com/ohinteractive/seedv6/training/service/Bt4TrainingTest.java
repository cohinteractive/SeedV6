package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;
import com.ohinteractive.seedv6.training.selfplay.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class Bt4TrainingTest {
    @TempDir Path temporary;
    String prior;
    @BeforeEach void cache() { prior = System.getProperty("seedv6.preparedDataRoot"); System.setProperty("seedv6.preparedDataRoot", temporary.resolve("prepared").toString()); }
    @AfterEach void restore() { if (prior == null) System.clearProperty("seedv6.preparedDataRoot"); else System.setProperty("seedv6.preparedDataRoot", prior); }
    TrainerConfig config(Path root, DataSource bt4) throws Exception {
        Path lichess = temporary.resolve("lichess.jsonl");
        if (!Files.exists(lichess)) Files.writeString(lichess, SourceReadersTest.line(100).repeat(30));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN3)) {
            new DataSources(1, List.of(DataSource.register("Lichess", lichess, 1), bt4), false).save(DataSources.directory(root));
        }
        var base = Brn2TrainerServiceTest.config(root, 1);
        return new TrainerConfig(root, 71, base.selfPlay(), new TrainerConfig.Training(2, 2, true), base.validation(), 1,
                base.depthChange(), TrainerConfig.STANDARD_START, TrainingArchitecture.BRN3, .003)
                .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                .withCorpusTraining(new CorpusTrainingConfig(8)).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    @Test void mixedTargetsSkipQuotaReservationReplayAndProfileMismatch() throws Exception {
        var bt4 = BinpackFixtures.source(temporary.resolve("bt4")); Path root = temporary.resolve("lineage"); var config = config(root, bt4);
        assertTrue(assertThrows(IOException.class, () -> new SequentialTraining(config, config.source(), CorpusPreparation.NONE)).getMessage().contains("not READY"));
        PreparedBinpack.prepare(bt4, CorpusPreparation.NONE);
        CorpusTraining.Evidence receipt;
        try (var training = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) {
            var batch = training.batch(1); receipt = batch.evidence(); assertEquals(8, batch.training().samples().size()); assertEquals(2, batch.validation().samples().size());
            var targets = batch.training().targets(); var samples = batch.training().samples();
            var old = com.ohinteractive.seedv6.corpus.lichess.LichessDecoder.decode(SourceReadersTest.line(100), 1, 1);
            double cpOutcome = CorpusTraining.TargetPolicy.BRN3_CP_WDL_V1.target(old, old.position().toBoard(0));
            for (int i = 0; i < 4; i++) assertEquals(cpOutcome, targets.applyAsDouble(samples.get(i)));
            for (int i = 0; i < 4; i++) assertEquals(Bt4Targets.q(new int[]{100, 200, 300, -100}[i]), targets.applyAsDouble(samples.get(4 + i)));
            assertNotEquals(cpOutcome, Bt4Targets.q(100));
            assertEquals(CorpusTraining.SOURCE_OUTCOME, receipt.adapterIdentity()); receipt.verifySourceLabels(root);
            assertEquals(2, receipt.sourceLabels().size()); assertEquals(1, training.metrics().skippedRecords());
        }
        var ledger = new SourceLedger(root); var reserved = ledger.active().orElseThrow();
        assertEquals(new SourceLedger.Range(bt4.identity(), 0, 6, 4, 1, 1), reserved.ranges().get(1));
        try (var replay = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) { assertEquals(receipt, replay.batch(1).evidence()); }
        Files.delete(root.resolve("corpus-training/generation-1.json"));
        try (var replay = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) { assertEquals(receipt, replay.batch(1).evidence()); }
        Path evidence = root.resolve("corpus-training/generation-1.json");
        var altered = com.google.gson.JsonParser.parseString(receipt.json()).getAsJsonObject();
        altered.getAsJsonObject("sourceLabels").addProperty(bt4.identity(), "STOCKFISH_CP_MATE_V1");
        Files.writeString(evidence, altered.toString());
        assertThrows(IOException.class, () -> CorpusTraining.evidence(root, 1).verifySourceLabels(root));
        try (var replay = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) { assertThrows(IOException.class, () -> replay.batch(1)); }
        assertEquals(reserved, new SourceLedger(root).active().orElseThrow());
    }
    @Test void sourceWeightsAndArchitectureCompatibilityRemainExplicit() throws Exception {
        var bt4 = BinpackFixtures.source(temporary.resolve("bt4"));
        var old = DataSource.register("old", Files.writeString(temporary.resolve("old.jsonl"), SourceReadersTest.line(100)), 1);
        for (int weight : new int[]{9, 4, 1}) {
            var sources = new DataSources(1, List.of(old.withDisplay("old", weight), bt4), false);
            assertArrayEquals(new int[]{10000 * weight / (weight + 1), 10000 / (weight + 1)}, sources.allocate(10000));
        }
        assertEquals(CorpusTraining.TargetPolicy.BT4_Q_V1, CorpusTraining.targetPolicy(TrainingArchitecture.BRN3, LabelProfile.BT4_Q_V1));
        for (var architecture : List.of(TrainingArchitecture.BRN2, TrainingArchitecture.NNUE)) {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> CorpusTraining.targetPolicy(architecture, LabelProfile.BT4_Q_V1)).getMessage().contains("requires BRN-3"));
            assertEquals(CorpusTraining.targetPolicy(architecture), CorpusTraining.targetPolicy(architecture, LabelProfile.STOCKFISH_CP_MATE_V1));
        }
        var sentinel = new BinpackDecoder(BinpackFixtures.record(BinpackFixtures.FEN, 32002)).next();
        assertEquals("BT4-skip-sentinel", CorpusTraining.TargetPolicy.BT4_Q_V1.rejection(sentinel));
        assertNotNull(CorpusTraining.TargetPolicy.STOCKFISH_WDL_V1.rejection(sentinel));
    }
    @Test void oldReceiptsKeepTheirJsonAndArchitectureIdentityWithoutSourceProfiles() {
        for (String adapter : new String[]{null, "STOCKFISH_WDL_V1", "BRN3_CP_WDL_V1"}) {
            var old = new CorpusTraining.Evidence("root", "a".repeat(64), 71, 1, 8, 10, 8, 2,
                    "b".repeat(64), "c".repeat(64), 10, Map.of(), adapter, 0, 0);
            String json = old.json(); assertFalse(json.contains("sourceLabels"));
            assertEquals(json, CorpusTraining.Evidence.read(json).json());
            assertTrue(old.supports(adapter == null ? TrainingArchitecture.BRN2 : adapter.equals("STOCKFISH_WDL_V1") ? TrainingArchitecture.NNUE : TrainingArchitecture.BRN3));
        }
    }
    @Test void mixedServiceCheckpointAndOptimizerResumeRemainExact() throws Exception {
        var source = BinpackFixtures.source(temporary.resolve("bt4")); PreparedBinpack.prepare(source, CorpusPreparation.NONE);
        var a = config(temporary.resolve("continuous"), source); var b = config(temporary.resolve("resumed"), source);
        TrainerSnapshot continuous, resumed;
        try (var service = TrainerService.fresh(a, new NetworkTrainingState.Brn3(new Brn3Trainer(71)), NnueCorpusTrainingTest.forbidden(), s -> {})) {
            continuous = NnueCorpusTrainingTest.finish(service); assertEquals(0, continuous.totals().selfPlayGames());
        }
        var owner = new AtomicReference<TrainerService>();
        var stop = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainCorpus(NetworkTrainingState state, CorpusTraining.Examples examples,
                    SelfPlayTraining.Config c, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainCorpus(state, examples, c, control, p -> { observer.accept(p); owner.get().stop(); });
            }
        };
        try (var service = TrainerService.fresh(b, new NetworkTrainingState.Brn3(new Brn3Trainer(71)), stop, s -> {})) { owner.set(service); NnueCorpusTrainingTest.finish(service); }
        var reservation = new SourceLedger(b.checkpointRoot()).active().orElseThrow();
        try (var service = TrainerService.resume(b, NnueCorpusTrainingTest.forbidden(), s -> {})) { resumed = NnueCorpusTrainingTest.finish(service); }
        assertEquals(continuous.latestTrainingId(), resumed.latestTrainingId());
        try (var one = new CheckpointStore(a.checkpointRoot(), TrainingArchitecture.BRN3); var two = new CheckpointStore(b.checkpointRoot(), TrainingArchitecture.BRN3)) {
            assertArrayEquals(one.resumeState(continuous.latestTrainingId()).encode(), two.resumeState(resumed.latestTrainingId()).encode());
            var validation = two.validationFor(resumed.latestTrainingId()).orElseThrow();
            assertEquals(CorpusTraining.SOURCE_OUTCOME, validation.bootstrap().corpus().adapterIdentity());
            validation.bootstrap().corpus().verifySourceLabels(b.checkpointRoot());
        }
        assertEquals(reservation.ranges(), new SourceLedger(b.checkpointRoot()).generation(1).orElseThrow().ranges());
    }
}
