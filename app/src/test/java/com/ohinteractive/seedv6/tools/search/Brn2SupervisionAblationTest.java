package com.ohinteractive.seedv6.tools.search;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2SupervisionAblationTest {
    @TempDir Path temp;
    private List<Sample> samples() {
        return Brn2DiagnosticCorpus.POSITIONS.subList(0, 6).stream()
                .map(p -> new Sample(Board.fromFen(p.fen()), 1)).toList();
    }
    @Test void shuffledDefaultAndExplicitWdlMatchIndependentOnlineOrderAndOptimizerBytes() throws Exception {
        var samples = samples();
        var config = new SelfPlayTraining.Config(1, 1, true, -6540313355536843707L);
        var expected = new Brn2Trainer(.001);
        int[] order = {0, 1, 2, 3, 4, 5};
        var random = new SplittableRandom(config.shuffleSeed());
        for (int i = order.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), swap = order[i]; order[i] = order[j]; order[j] = swap;
        }
        for (int index : order) expected.train(samples.get(index).board(), samples.get(index).target());
        var normal = new Brn2Trainer(.001); var explicit = new Brn2Trainer(.001);
        var a = Brn2SelfPlayTraining.trainSamples(normal, samples, config, new SelfPlayControl(), p -> {}).orElseThrow();
        var b = Brn2SelfPlayTraining.trainSamples(explicit, samples, config, new SelfPlayControl(), p -> {}, Sample::target).orElseThrow();
        assertEquals(a, b);
        assertArrayEquals(Brn2Codec.encodeTraining(expected), Brn2Codec.encodeTraining(normal));
        assertArrayEquals(Brn2Codec.encodeTraining(normal), Brn2Codec.encodeTraining(explicit));
    }
    @Test void denseTargetsUseExistingGradientAndAreFrozenBeforeUpdates() throws Exception {
        var samples = samples(); var expected = new Brn2Trainer(.001); var actual = new Brn2Trainer(.001);
        double[] targets = {-.7, -.3, .1, .2, .8, .9}; int[] calls = {0};
        for (int i = 0; i < samples.size(); i++) expected.train(samples.get(i).board(), targets[i]);
        var stats = Brn2SelfPlayTraining.trainSamples(actual, samples, new SelfPlayTraining.Config(1, 1, false, 0),
                new SelfPlayControl(), p -> assertEquals(samples.size(), calls[0]), s -> targets[calls[0]++]).orElseThrow();
        assertEquals(6, calls[0]); assertEquals(6, stats.optimizerUpdates());
        assertArrayEquals(Brn2Codec.encodeTraining(expected), Brn2Codec.encodeTraining(actual));
        assertTrue(samples.stream().allMatch(s -> s.target() == 1));
    }
    @Test void invalidTargetsFailBeforeAnyUpdate() {
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1.01, 1.01}) {
            var trainer = new Brn2Trainer(.001); int[] calls = {0};
            assertThrows(IllegalArgumentException.class, () -> Brn2SelfPlayTraining.trainSamples(trainer, samples(),
                    new SelfPlayTraining.Config(1, 1, true, 0), new SelfPlayControl(), p -> {},
                    s -> ++calls[0] == 3 ? bad : .25));
            assertEquals(0, trainer.optimizer().step());
        }
    }
    @Test void boundedTeacherUsesExactBoardSideAndMatchesScalarOracleWithoutChangingSamples() {
        var network = NnueNetwork.initialized(17); var teacher = new NnueEvaluator(network);
        var oracle = NnueEvaluator.scalarOracle(network);
        for (var position : Brn2DiagnosticCorpus.POSITIONS) for (String side : List.of("w", "b")) {
            String[] fen = position.fen().split(" ");
            if (!fen[1].equals(side)) fen[3] = "-"; // Opposite-side fixture cannot retain an incompatible EP rank.
            fen[1] = side;
            var sample = new Sample(Board.fromFen(String.join(" ", fen)), -1);
            long[] before = sample.board(); oracle.evaluate(before);
            double value = Brn2SupervisionAblation.teacherValue(teacher, sample);
            assertEquals(oracle.boundedValue(), value);
            assertEquals(StrictMath.tanh(oracle.raw()), value);
            assertEquals(.5 * -1 + .5 * value, Brn2SupervisionAblation.Arm.BLENDED.target(-1, value));
            assertEquals(value, Brn2SupervisionAblation.Arm.TEACHER.target(-1, value));
            assertArrayEquals(before, sample.board()); assertEquals(-1, sample.target());
        }
    }
    @Test void heldOutObjectiveIsSharedAndCanChangePromotionWithoutChangingWdlDefault() {
        var samples = samples();
        var wdl = HeldOutLoss.compare(b -> .5, b -> 0, samples);
        var teacher = HeldOutLoss.compare(b -> .5, b -> 0, samples, s -> -.5);
        assertEquals(.125, wdl.candidateLoss()); assertEquals(.5, wdl.bestLoss());
        assertEquals(.5, teacher.candidateLoss()); assertEquals(.125, teacher.bestLoss());
        assertEquals(PromotionPolicy.Decision.PROMOTE, wdl.decision());
        assertEquals(PromotionPolicy.Decision.RETAIN_INCUMBENT, teacher.decision());
        assertThrows(IllegalArgumentException.class, () -> HeldOutLoss.compare(b -> 0, b -> 0, samples, s -> 2));
    }
    @Test void savedTrainingSettingsAreExactAndUnknownRegimesFailClosed() throws Exception {
        var config = Brn2SupervisionAblation.trainingConfig("generation|Config[epochs=1, minibatchSize=1, shuffle=true, shuffleSeed=-123]|4|fen");
        assertEquals(new SelfPlayTraining.Config(1, 1, true, -123), config);
        for (String bad : List.of("", "generation|Config[epochs=2, minibatchSize=1, shuffle=true, shuffleSeed=-123]|4|fen",
                "generation|Config[epochs=1, minibatchSize=1, shuffle=false, shuffleSeed=-123]|4|fen"))
            assertThrows(IOException.class, () -> Brn2SupervisionAblation.trainingConfig(bad));
    }
    @Test void sourceOverlapAndExistingOutputAreRejectedBeforeMutation() throws Exception {
        Path source = Files.createDirectory(temp.resolve("source")), teacher = Files.createDirectory(temp.resolve("teacher"));
        Path valid = temp.resolve("experimental-supervision-ablation-test");
        assertEquals(valid.toAbsolutePath(), Brn2SupervisionAblation.newOutput(valid, source, teacher));
        for (Path invalid : List.of(source.resolve(valid.getFileName()), teacher.resolve(valid.getFileName()), source, temp.resolve("training")))
            assertThrows(IOException.class, () -> Brn2SupervisionAblation.newOutput(invalid, source, teacher));
        Files.createDirectory(valid);
        assertThrows(IOException.class, () -> Brn2SupervisionAblation.newOutput(valid, source, teacher));
    }
    @Test void controlHashMismatchFailsClosedBeforeOtherEvidenceIsUsed() {
        String hash = "0".repeat(64), id = "g000001-s000000006-" + hash;
        var manifest = new CheckpointManifest(id, 1, 6, 2, "", Brn2Codec.MODEL_BYTES, hash,
                Brn2Codec.TRAINING_BYTES, hash, TrainingArchitecture.BRN2);
        var batch = new Brn2SupervisionAblation.Batch(manifest, null, null, null);
        assertThrows(IOException.class, () -> Brn2SupervisionAblation.verifyControl(batch, "1".repeat(64), hash, 6, 0,
                new HeldOutLoss.Comparison(2, 0, 1), 0));
    }
}
