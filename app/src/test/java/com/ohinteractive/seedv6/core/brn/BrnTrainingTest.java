package com.ohinteractive.seedv6.core.brn;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static com.ohinteractive.seedv6.core.brn.BrnFeatureSchema.*;
import static org.junit.jupiter.api.Assertions.*;

class BrnTrainingTest {
    private static final long[] PAWNS = Board.fromFen("7k/8/8/8/8/8/PPP5/7K w - - 0 1");

    @Test
    void tanhGradientMatchesFiniteDifferencesIncludingRepeatedRelations() {
        double[] weights = new double[PARAMETER_COUNT];
        weights[0] = 0.375;
        BrnAdamConfig config = new BrnAdamConfig(0.01, 0.5, 0.75, 0.125);
        BrnTrainer trainer = new BrnTrainer(new BrnModel(weights), config);
        double target = -0.4, before = trainer.predict(PAWNS);
        assertEquals(0.5 * (before - target) * (before - target), trainer.train(PAWNS, target));
        int repeated = relationIndex(6, 8, 6, 9);
        for (int index : new int[] {BIAS, nodeIndex(6, 8), repeated, statusIndex(18), nodeIndex(6, 11)}) {
            double h = 1e-6;
            double[] plus = weights.clone(), minus = weights.clone();
            plus[index] += h;
            minus[index] -= h;
            double numerical = (loss(new BrnModel(plus).evaluate(PAWNS, new BrnFeatures()), target)
                    - loss(new BrnModel(minus).evaluate(PAWNS, new BrnFeatures()), target)) / (2 * h);
            double actual = trainer.optimizer().firstMoment(index) / (1 - config.beta1());
            assertEquals(numerical, actual, 1e-9, "index=" + index);
        }
        assertEquals(2 * trainer.optimizer().firstMoment(nodeIndex(6, 8)), trainer.optimizer().firstMoment(repeated));
        assertEquals(4 * trainer.optimizer().secondMoment(nodeIndex(6, 8)), trainer.optimizer().secondMoment(repeated));
    }

    @Test
    void sparseAdamMatchesIndependentReferenceThroughInactivityAndReactivation() {
        double[] expectedWeights = new double[PARAMETER_COUNT];
        expectedWeights[0] = 0.125;
        double[] m = new double[PARAMETER_COUNT], v = new double[PARAMETER_COUNT];
        BrnAdamConfig hp = new BrnAdamConfig(0.07, 0.4, 0.6, 0.13);
        BrnTrainer trainer = new BrnTrainer(new BrnModel(expectedWeights), hp);
        long[][] boards = {PAWNS, new long[5], Board.startingPosition(), new long[] {0, 0, 0, 1L << 63, Long.MIN_VALUE}, PAWNS};
        double[] targets = {0.7, -0.3, 0.1, -0.8, -0.5};
        BrnFeatures scratch = new BrnFeatures();
        for (int step = 1; step <= boards.length; step++) {
            long[] board = boards[step - 1];
            double prediction = new BrnModel(expectedWeights).evaluate(board, scratch);
            double g = (prediction - targets[step - 1]) * (1 - prediction * prediction);
            int[] counts = oracleCounts(board);
            for (int i = 0; i < PARAMETER_COUNT; i++) {
                if (counts[i] == 0) continue;
                double gradient = g * counts[i];
                m[i] = 0.4 * m[i] + 0.6 * gradient;
                v[i] = 0.6 * v[i] + 0.4 * gradient * gradient;
                double correctedM = m[i] / (1 - StrictMath.pow(0.4, step));
                double correctedV = v[i] / (1 - StrictMath.pow(0.6, step));
                expectedWeights[i] -= 0.07 * correctedM / (StrictMath.sqrt(correctedV) + 0.13);
            }
            trainer.train(board, targets[step - 1]);
            assertArrayEquals(expectedWeights, trainer.snapshot().copyWeights(), 1e-15);
            for (int i = 0; i < PARAMETER_COUNT; i++) {
                assertEquals(m[i], trainer.optimizer().firstMoment(i), 1e-15);
                assertEquals(v[i], trainer.optimizer().secondMoment(i), 1e-15);
            }
            assertEquals(step, trainer.optimizer().step());
        }
    }

    @Test
    void invalidSamplesAndLateNumericalFailureAreAtomicAndRecoverable() throws Exception {
        BrnTrainer trainer = new BrnTrainer(0.01);
        trainer.train(PAWNS, 0.5);
        byte[] baseline = BrnCodec.encodeTraining(trainer);
        for (double bad : new double[] {-1.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> trainer.train(PAWNS, bad));
        }
        assertThrows(NullPointerException.class, () -> trainer.train(null, 0));
        assertThrows(IllegalArgumentException.class, () -> trainer.train(new long[4], 0));
        assertArrayEquals(baseline, BrnCodec.encodeTraining(trainer));

        double[] weights = new double[PARAMETER_COUNT];
        int relation = relationIndex(6, 8, 6, 9);
        weights[0] = Double.MAX_VALUE;
        weights[relation] = -Double.MAX_VALUE / 2; // Finite z=0, but relation gradient has multiplicity 2.
        BrnTrainer extreme = new BrnTrainer(new BrnModel(weights), new BrnAdamConfig(Double.MAX_VALUE, 0, 0, 1));
        byte[] extremeBefore = BrnCodec.encodeTraining(extreme);
        assertEquals(0, extreme.predict(PAWNS));
        assertThrows(ArithmeticException.class, () -> extreme.train(PAWNS, -1));
        assertArrayEquals(extremeBefore, BrnCodec.encodeTraining(extreme));
        assertDoesNotThrow(() -> extreme.train(PAWNS, 0)); // Failed scratch does not poison retry.

        weights[0] = Double.MAX_VALUE;
        weights[nodeIndex(6, 8)] = Double.MAX_VALUE;
        weights[relation] = 0;
        BrnTrainer overflowing = new BrnTrainer(new BrnModel(weights), new BrnAdamConfig(0.01));
        byte[] overflowBefore = BrnCodec.encodeTraining(overflowing);
        assertThrows(ArithmeticException.class, () -> overflowing.train(PAWNS, 0));
        assertArrayEquals(overflowBefore, BrnCodec.encodeTraining(overflowing));
    }

    @Test
    void configurationAndOptimizerBoundaryValidation() {
        for (double bad : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new BrnAdamConfig(bad));
            assertThrows(IllegalArgumentException.class, () -> new BrnAdamConfig(0.1, 0.9, 0.999, bad));
        }
        for (double bad : new double[] {-0.1, 1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new BrnAdamConfig(0.1, bad, 0.999, 1e-8));
            assertThrows(IllegalArgumentException.class, () -> new BrnAdamConfig(0.1, 0.9, bad, 1e-8));
        }
        BrnAdamConfig defaults = new BrnAdamConfig(0.003);
        assertEquals(0.003, defaults.learningRate());
        assertEquals(0.9, defaults.beta1());
        assertEquals(0.999, defaults.beta2());
        assertEquals(1e-8, defaults.epsilon());
        BrnAdamState exhausted = new BrnAdamState(Long.MAX_VALUE, new double[PARAMETER_COUNT], new double[PARAMETER_COUNT]);
        BrnTrainer trainer = new BrnTrainer(new double[PARAMETER_COUNT], defaults, exhausted);
        assertThrows(ArithmeticException.class, () -> trainer.train(PAWNS, 1));
        assertEquals(Long.MAX_VALUE, exhausted.step());
        assertArrayEquals(new double[PARAMETER_COUNT], trainer.snapshot().copyWeights());
    }

    @Test
    void learnsSpecificNodePatternAndRetainsFrozenInferenceSnapshot() {
        long[][] boards = {Board.fromFen("7k/8/8/8/8/8/N7/K7 w - - 0 1"),
                Board.fromFen("7k/8/8/8/8/8/1N6/K7 w - - 0 1")};
        BrnTrainer trainer = new BrnTrainer(0.01);
        BrnModel frozen = trainer.snapshot();
        double[] targets = {0.7, -0.7};
        double before = meanLoss(trainer, boards, targets);
        for (int epoch = 0; epoch < 250; epoch++) for (int i = 0; i < boards.length; i++) trainer.train(boards[i], targets[i]);
        double after = meanLoss(trainer, boards, targets);
        assertTrue(after < before / 100);
        assertTrue(trainer.predict(boards[0]) > 0.65);
        assertTrue(trainer.predict(boards[1]) < -0.65);
        for (long[] board : boards) {
            assertEquals(0, frozen.evaluate(board, new BrnFeatures()));
            assertEquals(trainer.predict(board), trainer.snapshot().evaluate(board, new BrnFeatures()));
        }
        System.out.printf("BRN NODE steps=%d loss=%.12g->%.12g predictions=[%.9f,%.9f]%n",
                trainer.optimizer().step(), before, after, trainer.predict(boards[0]), trainer.predict(boards[1]));
    }

    @Test
    void learnsBalancedRelationalPatternThatNodeOnlyAdditionsCannotFit() throws Exception {
        long[][] boards = {
                Board.fromFen("7k/8/5n2/8/8/8/1N6/K7 w - - 0 1"),
                Board.fromFen("7k/8/6n1/8/8/8/1N6/K7 w - - 0 1"),
                Board.fromFen("7k/8/5n2/8/8/8/2N5/K7 w - - 0 1"),
                Board.fromFen("7k/8/6n1/8/8/8/2N5/K7 w - - 0 1")};
        // Target +.7 iff the N->n offset is (4,4); both individual square choices are balanced.
        double[] targets = {0.7, -0.7, -0.7, 0.7};
        BrnTrainer trainer = new BrnTrainer(0.01), identical = new BrnTrainer(0.01);
        double before = meanLoss(trainer, boards, targets);
        for (int epoch = 0; epoch < 500; epoch++) {
            for (int i = 0; i < boards.length; i++) {
                assertEquals(trainer.train(boards[i], targets[i]), identical.train(boards[i], targets[i]));
            }
        }
        double after = meanLoss(trainer, boards, targets);
        assertTrue(after < before / 100);
        for (int i = 0; i < boards.length; i++) assertEquals(targets[i], trainer.predict(boards[i]), 0.05);
        assertArrayEquals(BrnCodec.encodeTraining(trainer), BrnCodec.encodeTraining(identical));
        double[] ablated = trainer.snapshot().copyWeights();
        Arrays.fill(ablated, RELATION_OFFSET, STATUS_OFFSET, 0);
        BrnTrainer nodeOnly = new BrnTrainer(new BrnModel(ablated), new BrnAdamConfig(0.01));
        assertTrue(meanLoss(nodeOnly, boards, targets) > after * 100);
        System.out.printf("BRN RELATION steps=%d loss=%.12g->%.12g predictions=[%.9f,%.9f,%.9f,%.9f]%n",
                trainer.optimizer().step(), before, after, trainer.predict(boards[0]), trainer.predict(boards[1]),
                trainer.predict(boards[2]), trainer.predict(boards[3]));
    }

    private static int[] oracleCounts(long[] board) {
        int[] counts = new int[PARAMETER_COUNT];
        counts[0] = 1;
        for (int a = 0; a < 64; a++) {
            int codeA = Board.getSquare(board[0], board[1], board[2], board[3], a);
            if (codeA == 0) continue;
            counts[nodeIndex(codeA, a)]++;
            for (int b = a + 1; b < 64; b++) {
                int codeB = Board.getSquare(board[0], board[1], board[2], board[3], b);
                if (codeB != 0) counts[relationIndex(codeA, a, codeB, b)]++;
            }
        }
        for (int bit = 0; bit < 64; bit++) if ((board[4] & (1L << bit)) != 0) counts[statusIndex(bit)]++;
        return counts;
    }
    private static double loss(double prediction, double target) { return 0.5 * (prediction - target) * (prediction - target); }
    private static double meanLoss(BrnTrainer trainer, long[][] boards, double[] targets) {
        double sum = 0;
        for (int i = 0; i < boards.length; i++) sum += loss(trainer.predict(boards[i]), targets[i]);
        return sum / boards.length;
    }
}
