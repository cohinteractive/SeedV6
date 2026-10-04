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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class Bt4TrainingTest {
    @TempDir Path temporary;
    String prior;
    @BeforeEach void cache() { prior = System.getProperty("seedv6.preparedDataRoot"); System.setProperty("seedv6.preparedDataRoot", temporary.resolve("prepared").toString()); }
    @AfterEach void restore() { if (prior == null) System.clearProperty("seedv6.preparedDataRoot"); else System.setProperty("seedv6.preparedDataRoot", prior); }
    TrainerConfig config(Path root, DataSource bt4) throws Exception {
        return config(root, bt4, TrainingArchitecture.BRN3);
    }
    TrainerConfig config(Path root, DataSource bt4, TrainingArchitecture architecture) throws Exception {
        Path lichess = temporary.resolve("lichess.jsonl");
        if (!Files.exists(lichess)) Files.writeString(lichess, SourceReadersTest.line(100).repeat(30));
        try (var store = new CheckpointStore(root, architecture)) {
            new DataSources(1, List.of(DataSource.register("Lichess", lichess, 1), bt4), false).save(DataSources.directory(root));
        }
        var base = Brn2TrainerServiceTest.config(root, 1);
        return new TrainerConfig(root, 71, base.selfPlay(), new TrainerConfig.Training(2, 2, true), base.validation(), 1,
                base.depthChange(), TrainerConfig.STANDARD_START, architecture, .003)
                .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                .withCorpusTraining(new CorpusTrainingConfig(8)).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "BRN3"})
    void mixedTargetsSkipQuotaReservationReplayAndProfileMismatch(TrainingArchitecture architecture) throws Exception {
        var bt4 = BinpackFixtures.source(temporary.resolve("bt4")); Path root = temporary.resolve("lineage"); var config = config(root, bt4, architecture);
        assertTrue(assertThrows(IOException.class, () -> new SequentialTraining(config, config.source(), CorpusPreparation.NONE)).getMessage().contains("not READY"));
        PreparedBinpack.prepare(bt4, CorpusPreparation.NONE);
        CorpusTraining.Evidence receipt;
        try (var training = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) {
            var batch = training.batch(1); receipt = batch.evidence(); assertEquals(8, batch.training().samples().size()); assertEquals(2, batch.validation().samples().size());
            var targets = batch.training().targets(); var samples = batch.training().samples();
            var old = com.ohinteractive.seedv6.corpus.lichess.LichessDecoder.decode(SourceReadersTest.line(100), 1, 1);
            double cpOutcome = CorpusTraining.targetPolicy(architecture).target(old, old.position().toBoard(0));
            for (int i = 0; i < 4; i++) assertEquals(cpOutcome, targets.applyAsDouble(samples.get(i)));
            for (int i = 0; i < 4; i++) assertEquals(Bt4Targets.q(new int[]{100, 200, 300, -100}[i]), targets.applyAsDouble(samples.get(4 + i)));
            assertNotEquals(cpOutcome, Bt4Targets.q(100));
            assertEquals(CorpusTraining.sourceOutcomeAdapter(architecture), receipt.adapterIdentity()); receipt.verifySourceLabels(root);
            assertTrue(receipt.supports(architecture));
            assertFalse(receipt.supports(TrainingArchitecture.BRN2));
            assertFalse(receipt.supports(architecture == TrainingArchitecture.NNUE ? TrainingArchitecture.BRN3 : TrainingArchitecture.NNUE));
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
        for (var architecture : List.of(TrainingArchitecture.BRN3, TrainingArchitecture.NNUE)) {
            assertEquals(CorpusTraining.TargetPolicy.BT4_Q_V1, CorpusTraining.targetPolicy(architecture, LabelProfile.BT4_Q_V1));
            assertEquals(CorpusTraining.targetPolicy(architecture), CorpusTraining.targetPolicy(architecture, LabelProfile.STOCKFISH_CP_MATE_V1));
        }
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CorpusTraining.targetPolicy(TrainingArchitecture.BRN2, LabelProfile.BT4_Q_V1)).getMessage().contains("No Q-to-centipawn conversion"));
        assertThrows(IllegalArgumentException.class, () -> CorpusTraining.targetPolicy(TrainingArchitecture.BRN1, LabelProfile.BT4_Q_V1));
        assertEquals(CorpusTraining.TargetPolicy.BASIC_V1, CorpusTraining.targetPolicy(TrainingArchitecture.BRN2, LabelProfile.STOCKFISH_CP_MATE_V1));
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
    @Test void sourceOutcomeRecipesPreserveHistoricalMeaningAndEnforceConsumerCompatibility() {
        for (var architecture : List.of(TrainingArchitecture.NNUE, TrainingArchitecture.BRN3)) {
            String adapter = CorpusTraining.sourceOutcomeAdapter(architecture);
            var receipt = new CorpusTraining.Evidence("root", "a".repeat(64), 71, 1, 8, 10, 8, 2,
                    "b".repeat(64), "c".repeat(64), 10, Map.of(), adapter, 0, 0,
                    Map.of("d".repeat(64), "BT4_Q_V1;prepared=" + "e".repeat(64)));
            assertEquals(receipt.json(), CorpusTraining.Evidence.read(receipt.json()).json());
            assertEquals(adapter, new CorpusTrainingConfig(8, "", adapter).forArchitecture(architecture).targetAdapter());
            var other = architecture == TrainingArchitecture.NNUE ? TrainingArchitecture.BRN3 : TrainingArchitecture.NNUE;
            assertThrows(IllegalArgumentException.class, () -> new CorpusTrainingConfig(8, "", adapter).forArchitecture(other));
            assertThrows(IllegalArgumentException.class, () -> new CorpusTrainingConfig(8, "", adapter).forArchitecture(TrainingArchitecture.BRN2));
            var tampered = com.google.gson.JsonParser.parseString(receipt.json()).getAsJsonObject();
            tampered.getAsJsonObject("sourceLabels").addProperty("d".repeat(64), "UNKNOWN;prepared=" + "e".repeat(64));
            assertThrows(RuntimeException.class, () -> CorpusTraining.Evidence.read(tampered.toString()));
        }
        assertEquals("BRN3_SOURCE_OUTCOME_V1", CorpusTraining.sourceOutcomeAdapter(TrainingArchitecture.BRN3));
    }
    @Test void qTargetsRetainSideToMoveMeaningForIndividualPreparedArchives() throws Exception {
        var raw = new java.io.ByteArrayOutputStream();
        for (String turn : List.of("w", "b")) for (int score : new int[]{100, -100, 0, 32002, 32001})
            raw.write(BinpackFixtures.record(BinpackFixtures.FEN.replace(" w ", " " + turn + " "), score));
        Path archive = BinpackFixtures.archive(temporary.resolve("single.zst"), BinpackFixtures.frame(raw.toByteArray()));
        var source = DataSource.register("individual", archive, 1, LabelProfile.BT4_Q_V1);
        PreparedBinpack.prepare(source, CorpusPreparation.NONE);
        var adapter = CorpusTraining.targetPolicy(TrainingArchitecture.NNUE, source.labelProfile());
        try (var reader = SourceReaders.open(source, temporary, 0)) {
            for (int turn = 0; turn < 2; turn++) {
                for (int score : new int[]{100, -100, 0}) {
                    var record = reader.next().position();
                    assertNull(adapter.rejection(record));
                    var examples = new CorpusTraining.Examples(adapter); examples.add(record);
                    assertEquals(Bt4Targets.q(score), examples.targets().applyAsDouble(examples.samples().getFirst()));
                }
                assertEquals("BT4-skip-sentinel", adapter.rejection(reader.next().position()));
                assertEquals("BT4-score-out-of-range", adapter.rejection(reader.next().position()));
            }
            assertNull(reader.next());
        }
        var cp = com.ohinteractive.seedv6.corpus.lichess.LichessDecoder.decode(SourceReadersTest.line(100), 1, 1);
        assertEquals("unsupported-BT4-label", adapter.rejection(cp));
    }
    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "BRN3"})
    void mixedServiceCheckpointAndOptimizerResumeRemainExact(TrainingArchitecture architecture) throws Exception {
        var source = BinpackFixtures.source(temporary.resolve("bt4")); PreparedBinpack.prepare(source, CorpusPreparation.NONE);
        // NNUE keeps its established mate-sign semantics even in a mixed BT4 campaign.
        if (architecture == TrainingArchitecture.NNUE) Files.writeString(temporary.resolve("lichess.jsonl"),
                (SourceReadersTest.line(100).replace("\"cp\":100", "\"mate\":3") + SourceReadersTest.line(100)).repeat(15));
        var a = config(temporary.resolve("continuous"), source, architecture); var b = config(temporary.resolve("resumed"), source, architecture);
        TrainerSnapshot continuous, resumed;
        try (var service = TrainerService.fresh(a, initial(architecture), NnueCorpusTrainingTest.forbidden(), s -> {})) {
            continuous = NnueCorpusTrainingTest.finish(service); assertEquals(0, continuous.totals().selfPlayGames());
            assertEquals(16, continuous.training().orElseThrow().samplesTrained());
            assertEquals(8, continuous.training().orElseThrow().optimizerUpdates());
        }
        var owner = new AtomicReference<TrainerService>();
        var stop = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainCorpus(NetworkTrainingState state, CorpusTraining.Examples examples,
                    SelfPlayTraining.Config c, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                return super.trainCorpus(state, examples, c, control, p -> { observer.accept(p); owner.get().stop(); });
            }
        };
        try (var service = TrainerService.fresh(b, initial(architecture), stop, s -> {})) { owner.set(service); NnueCorpusTrainingTest.finish(service); }
        var reservation = new SourceLedger(b.checkpointRoot()).active().orElseThrow();
        try (var service = TrainerService.resume(b, NnueCorpusTrainingTest.forbidden(), s -> {})) { resumed = NnueCorpusTrainingTest.finish(service); }
        assertEquals(continuous.latestTrainingId(), resumed.latestTrainingId());
        try (var one = new CheckpointStore(a.checkpointRoot(), architecture); var two = new CheckpointStore(b.checkpointRoot(), architecture)) {
            assertArrayEquals(one.resumeState(continuous.latestTrainingId()).encode(), two.resumeState(resumed.latestTrainingId()).encode());
            var validation = two.validationFor(resumed.latestTrainingId()).orElseThrow();
            assertEquals(CorpusTraining.sourceOutcomeAdapter(architecture), validation.bootstrap().corpus().adapterIdentity());
            assertEquals(architecture == TrainingArchitecture.NNUE ? 2 : 0, validation.bootstrap().corpus().mateExamples());
            assertEquals(architecture == TrainingArchitecture.NNUE ? 1 : 0, validation.bootstrap().corpus().heldOutMateExamples());
            validation.bootstrap().corpus().verifySourceLabels(b.checkpointRoot());
        }
        assertEquals(reservation.ranges(), new SourceLedger(b.checkpointRoot()).generation(1).orElseThrow().ranges());
    }
    private static NetworkTrainingState initial(TrainingArchitecture architecture) {
        return architecture == TrainingArchitecture.BRN3 ? new NetworkTrainingState.Brn3(new Brn3Trainer(71))
                : new NetworkTrainingState.Nnue(new com.ohinteractive.seedv6.training.nnue.NnueTrainer(
                        com.ohinteractive.seedv6.training.nnue.TrainableNnue.initialized(71)));
    }
}
