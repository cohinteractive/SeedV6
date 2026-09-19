package com.ohinteractive.seedv6.tools.nnue.research;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class NetworkHealthTest {
    @TempDir Path root;
    static NnueNetwork constant(float transformerBias, float hiddenBias, float outputBias) {
        float[] f = new float[64], h = new float[32]; Arrays.fill(f, transformerBias); Arrays.fill(h, hiddenBias);
        return NnueNetwork.of(1, f, new float[NnueNetwork.FEATURE_WEIGHT_COUNT], h,
                new float[NnueNetwork.HIDDEN_WEIGHT_COUNT], outputBias, new float[32]);
    }
    static List<long[]> positions() { return List.of(Board.startingPosition(), Board.fromFen(StrengthArenaTest.MATE)); }
    @Test void initializedNetworkIsFiniteWithSixSeparateTensorsAndImmutableAnalysis() throws Exception {
        NnueNetwork n = NnueNetwork.initialized(27);
        var before = NetworkHealth.distance(n, NnueNetwork.initialized(27));
        var health = NetworkHealth.analyze(n, NetworkHealth.corpus(1, 8));
        assertEquals(6, health.parameters().size());
        assertEquals(NnueNetwork.PARAMETER_COUNT, health.parameters().values().stream().mapToLong(NetworkHealth.Distribution::finite).sum());
        assertTrue(health.parameters().values().stream().allMatch(d -> d.nonfinite() == 0));
        assertEquals(8 * 128, health.transformer().count()); assertEquals(8 * 32, health.hidden().count());
        assertEquals(0, health.saturationPercent()); assertEquals(before, NetworkHealth.distance(n, NnueNetwork.initialized(27)));
    }
    @Test void deliberatelySaturatedActivationsAndOutputAreMeasuredExactly() {
        var report = NetworkHealth.analyze(constant(2, 2, 4), positions());
        assertEquals(256, report.transformer().one()); assertEquals(64, report.hidden().one());
        assertEquals(100, report.transformer().onePercent()); assertEquals(100, report.hidden().onePercent());
        assertEquals(4, report.raw().min()); assertEquals(4, report.raw().max()); assertEquals(0, report.raw().stddev());
        assertEquals(StrictMath.tanh(4), report.prediction().mean()); assertEquals(100, report.saturationPercent());
        assertEquals(100, report.positivePercent());
    }
    @Test void zeroAndInteriorNetworksHaveExactCountsAndOutputDistribution() {
        var zero = NetworkHealth.analyze(constant(0, 0, 0), positions());
        assertEquals(256, zero.transformer().zero()); assertEquals(64, zero.hidden().zero());
        assertEquals(100, zero.nearZeroPercent()); assertEquals(0, zero.raw().mean()); assertEquals(0, zero.saturationPercent());
        var interior = NetworkHealth.analyze(constant(.5f, .5f, -.1f), positions());
        assertEquals(256, interior.transformer().interior()); assertEquals(64, interior.hidden().interior());
        assertEquals(100, interior.negativePercent()); assertEquals(0, interior.prediction().stddev());
    }
    @Test void distanceHasExactL2RmsAndMaximumAcrossEveryTensor() {
        var distance = NetworkHealth.distance(constant(0, 0, 0), constant(0, 0, 3));
        assertEquals(NnueNetwork.PARAMETER_COUNT, distance.parameters()); assertEquals(3, distance.l2());
        assertEquals(3 / Math.sqrt(NnueNetwork.PARAMETER_COUNT), distance.rms(), 1e-15); assertEquals(3, distance.maximumAbsolute());
    }
    @Test void momentsQuantilesAndNonfiniteCountsAreIndependent() {
        double[] values = {-2, -1, 0, 1, 2, Double.NaN, Double.POSITIVE_INFINITY};
        var d = NetworkHealth.distribution(values.length, i -> values[i]);
        assertEquals(5, d.finite()); assertEquals(2, d.nonfinite()); assertEquals(-2, d.min()); assertEquals(2, d.max());
        assertEquals(0, d.mean()); assertEquals(Math.sqrt(2), d.rms()); assertEquals(Math.sqrt(2), d.stddev());
        assertEquals(1, d.absMedian()); assertEquals(2, d.absP95()); assertEquals(2, d.absP99());
        assertThrows(IllegalArgumentException.class, () -> NnueNetwork.of(1, new float[64], new float[NnueNetwork.FEATURE_WEIGHT_COUNT],
                new float[32], new float[NnueNetwork.HIDDEN_WEIGHT_COUNT], Float.NaN, new float[32]));
    }
    @Test void corpusIsDeterministicAndDifferentSeedChangesIt() {
        var a = NetworkHealth.corpus(3, 4); var b = NetworkHealth.corpus(3, 4); var c = NetworkHealth.corpus(4, 4);
        for (int i = 0; i < a.size(); i++) assertArrayEquals(a.get(i), b.get(i));
        assertFalse(Arrays.equals(a.getFirst(), c.getFirst()));
    }
    @Tag("slow-nnue")
    @Test void progressionStreamsOneCheckpointAtATimeAndPreservesAncestorIdentity() throws Exception {
        List<String> ids = new ArrayList<>();
        try (var store = new CheckpointStore(root)) {
            var trainer = new NnueTrainer(TrainableNnue.fromNetwork(constant(0, 0, 0)));
            ids.add(store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, "")).manifest().id());
            for (int g = 1; g <= 3; g++) {
                trainer.trainBatch(new long[][]{Board.startingPosition()}, new double[]{1}, 1);
                ids.add(store.publish(trainer, new CheckpointManifest.Metadata(g, 1, ids.getLast())).manifest().id());
            }
        }
        ResearchData.append(root.resolve("generations.tsv"), 1, ids.get(1), ids.get(0), "loss=0.1");
        ResearchData.append(root.resolve("generations.tsv"), 2, ids.get(2), ids.get(1), "loss=0.2");
        var before = StrengthArenaTest.hashes(root);
        List<ProgressionAnalysis.Row> rows = new ArrayList<>(); int[] loads = {0};
        ProgressionAnalysis.analyze(root, positions(), row -> {
            assertEquals(rows.size() + 1, loads[0], "Next checkpoint is not loaded before previous metrics are consumed.");
            rows.add(row);
        }, path -> { loads[0]++; return CheckpointStore.inspect(path).network(); });
        assertEquals(4, loads[0]); assertEquals(ids, rows.stream().map(r -> r.checkpoint().id()).toList());
        assertTrue(rows.getFirst().best()); assertTrue(rows.getFirst().accepted()); assertTrue(rows.getLast().latest());
        assertTrue(rows.getFirst().researchMeasurements().isEmpty());
        assertEquals(1, rows.get(1).researchMeasurements().size());
        assertTrue(rows.get(1).researchMeasurements().getFirst().endsWith("loss=0.1"));
        assertEquals(0, rows.getFirst().fromParent().l2()); assertTrue(rows.getLast().fromBootstrap().l2() > 0);
        for (int i = 0; i < rows.size(); i++) {
            assertEquals(ids.getFirst(), rows.get(i).bootstrapId());
            assertEquals(i == 0 ? "" : ids.get(i - 1), rows.get(i).checkpoint().parentId());
        }
        assertEquals(before, StrengthArenaTest.hashes(root));
    }
    @Test void wholeGameHoldoutSelectionCalibrationAndTruncationAreExplicit() throws Exception {
        assertEquals(List.of(4, 9, 14, 19), java.util.stream.IntStream.range(0, 20).filter(ResearchTool::holdoutGame).boxed().toList());
        Path data = root.resolve("holdout.bin");
        try (var out = new DataOutputStream(Files.newOutputStream(data))) {
            out.writeInt(0x4A484F31);
            ResearchData.holdout(out, 1, 4, List.of(new TrajectorySampler.Sample(Board.startingPosition(), 1),
                    new TrajectorySampler.Sample(Board.startingPosition(), -1)));
        }
        var calibration = ResearchData.calibrate(data, constant(0, 0, 0));
        assertEquals(1, calibration.games()); assertEquals(2, calibration.samples()); assertEquals(1, calibration.mse());
        assertEquals(0, calibration.targets().mean()); assertEquals(0, calibration.predictions().mean());
        assertEquals(1, calibration.negativeTargets()); assertEquals(1, calibration.positiveTargets());
        var bytes = Files.readAllBytes(data); Files.write(data, Arrays.copyOf(bytes, bytes.length - 1));
        assertThrows(IOException.class, () -> ResearchData.calibrate(data, constant(0, 0, 0)));
    }
    @Tag("slow-nnue")
    @Test void readOnlyAcceptanceDoesNotMistakePendingPromotionForAuthoritativeBest() throws Exception {
        String bootstrap, child;
        byte[] oldBest;
        try (var store = new CheckpointStore(root)) {
            var trainer = new NnueTrainer(TrainableNnue.initialized(44));
            bootstrap = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            oldBest = Files.readAllBytes(root.resolve("refs/best"));
            child = store.publish(trainer, new CheckpointManifest.Metadata(1, 1, bootstrap)).manifest().id();
            var record = store.recordValidation(child, bootstrap, StrengthArenaTest.result(List.of(StrengthArenaTest.win())),
                    new PromotionPolicy(1, .99, 0));
            store.promote(record.id());
            assertEquals(Set.of(bootstrap, child), CheckpointInspection.accepted(root));
        }
        Files.write(root.resolve("refs/best"), oldBest); // Test fixture: crash before replacing the best reference.
        assertEquals(Set.of(bootstrap), CheckpointInspection.accepted(root));
        assertEquals(bootstrap, CheckpointInspection.reference(root, "best"));
    }
    @Test void explicitSampleTrainingMatchesAcceptedBatchPathBitForBit() {
        var config = new SelfPlayConfig(1, 1, 1, 7, 0, 0, 32, 8, ResearchTool.MAPPING, -1, -1);
        var batch = SelfPlayBatch.generate(NnueNetwork.initialized(7), config, Board.fromFen(StrengthArenaTest.MATE), new SelfPlayControl());
        assertFalse(batch.samples().isEmpty());
        var a = new NnueTrainer(TrainableNnue.initialized(7)); var b = new NnueTrainer(TrainableNnue.initialized(7));
        var training = new SelfPlayTraining.Config(1, 32, true, 99);
        assertEquals(SelfPlayTraining.train(a, batch, training, new SelfPlayControl()),
                SelfPlayTraining.trainSamples(b, batch.samples(), training, new SelfPlayControl(), p -> {}));
        assertEquals(0, NetworkHealth.distance(a.model().snapshot(), b.model().snapshot()).l2());
        assertEquals(a.optimizer().step(), b.optimizer().step());
    }
    @Test void measuredOpeningUsesOnlyActuallyPlayedExplorationMoves() {
        var config = new SelfPlayConfig(1, 1, 1, 21, 8, 8, 32, 16, ResearchTool.MAPPING, -1, -1);
        var game = SelfPlayRunner.play(NnueNetwork.initialized(21), config, 0, Board.startingPosition(), new SelfPlayControl());
        var openingConfig = new ValidationConfig(1, 21, 8, 8, 1, 1, ResearchTool.MAPPING, 16);
        var board = Board.startingPosition();
        assertEquals(ValidationArena.opening(board, com.ohinteractive.seedv6.rules.GameHistory.initial(board), openingConfig, 0).identity(),
                ResearchData.openingHash(game, config, 0));
        SelfPlayControl cancelled = new SelfPlayControl(); cancelled.cancel();
        var aborted = SelfPlayRunner.play(NnueNetwork.initialized(21), config, 0, board, cancelled);
        assertEquals(ValidationArena.stateHash(board, com.ohinteractive.seedv6.rules.GameHistory.initial(board)), ResearchData.openingHash(aborted, config, 0));
    }
    @Tag("slow-nnue")
    @Test void fastRealLifecycleReopensAdamStateAndRetainsBestAuthority() throws Exception {
        var config = new TrainerConfig(root, 7, new TrainerConfig.SelfPlay(1, 2, 1, 0, 0, 32, 8, ResearchTool.MAPPING),
                new TrainerConfig.Training(1, 32, true), new TrainerConfig.Validation(1, 0, 0, 1, 1, 8, ResearchTool.MAPPING,
                PromotionPolicy.DEFAULT), 1, TrainerConfig.DepthChange.REQUIRE_SAME, StrengthArenaTest.MATE);
        try (var service = TrainerService.fresh(config, new NnueTrainer(TrainableNnue.initialized(7)))) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(60))); assertTrue(service.failure().isEmpty());
        }
        var before = ResearchTool.recover(root);
        String best = CheckpointInspection.reference(root, "best");
        try (var service = TrainerService.resume(config)) {
            service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(60))); assertTrue(service.failure().isEmpty());
        }
        var after = ResearchTool.recover(root);
        assertEquals(before.generation() + 1, after.generation()); assertEquals(before.optimizerStep() + 1, after.optimizerStep());
        assertEquals(before.id(), after.parentId()); assertEquals(best, CheckpointInspection.reference(root, "best"));
        ResearchTool.resources(root, "AFTER_TEST");
        assertThrows(IOException.class, () -> ResearchTool.newRoot(root));
    }
}
