package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.ohinteractive.seedv6.training.nnue.TrainingFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AdamOptimizerTest {
    @Test
    void handComputedAdamUpdatesEveryGroupAndDecaysUntouchedHistoricalRows() {
        Parameters p = new Parameters();
        BatchGradients gradients = new BatchGradients();
        AdamOptimizer adam = new AdamOptimizer(new AdamHyperparameters(0.25, 0.5, 0.75, 0.125));
        for (float[] group : p.groups) group[0] = 0.4f;
        for (float[] group : gradients.values.groups) group[0] = 0.5f;
        gradients.touched[0] = true;
        gradients.rows[0] = 0;
        gradients.count = 1;
        adam.update(p, gradients);
        // m=.25, s=.0625, corrected m=.5, corrected s=.25, update=.25*.5/(.5+.125)=.2.
        for (int group = 0; group < p.groups.length; group++) {
            assertEquals((float) (0.4f - 0.2), p.groups[group][0]);
            assertEquals(0.25f, adam.firstMoment.groups[group][0]);
            assertEquals(0.0625f, adam.secondMoment.groups[group][0]);
        }
        gradients.reset();
        float before = p.groups[1][0];
        adam.update(p, gradients);
        // No new features touched: ordinary Adam must still decay the prior feature moments.
        double correctedM = 0.125 / 0.75;
        double correctedS = 0.046875 / 0.4375;
        float expected = (float) (before - 0.25 * correctedM / (StrictMath.sqrt(correctedS) + 0.125));
        for (int group = 0; group < p.groups.length; group++) {
            assertEquals(expected, p.groups[group][0]);
            assertEquals(0.125f, adam.firstMoment.groups[group][0]);
            assertEquals(0.046875f, adam.secondMoment.groups[group][0]);
        }
        assertNotEquals(before, p.groups[1][0]);
        assertEquals(0, p.groups[1][64]);
        assertEquals(2, adam.step());
    }

    @Test
    void nonfiniteLateCandidateCannotPublishEarlierValidCandidatesOrMoments() {
        Parameters p = new Parameters();
        p.groups[5][31] = -Float.MAX_VALUE;
        BatchGradients g = new BatchGradients();
        for (float[] group : g.values.groups) group[0] = 0.01f;
        g.values.groups[5][31] = 1;
        g.touched[0] = true;
        g.rows[0] = 0;
        g.count = 1;
        AdamOptimizer adam = new AdamOptimizer(new AdamHyperparameters(Float.MAX_VALUE, 0, 0, 1));
        Parameters before = Parameters.from(p.snapshot());
        assertThrows(ArithmeticException.class, () -> adam.update(p, g));
        assertBits(before, p);
        assertZero(adam.firstMoment);
        assertZero(adam.secondMoment);
        assertEquals(0, adam.step());
        // A rejected update also must not pollute the derived row cache or the next attempt.
        g.reset();
        adam.update(p, g);
        assertBits(before, p);
        assertEquals(1, adam.step());
    }

    @Test
    void numericalGradientCorruptionIsRejectedWithoutModelChanges() {
        NnueTrainer trainer = new NnueTrainer(model());
        Parameters before = Parameters.from(trainer.model().snapshot());
        trainer.accumulate(new long[][] {WHITE}, new double[] {1}, 1);
        trainer.gradients.values.groups[5][31] = Float.NaN;
        assertThrows(ArithmeticException.class, () -> trainer.optimizer().update(trainer.model().parameters, trainer.gradients));
        assertBits(before, trainer.model().parameters);
        assertZero(trainer.optimizer().firstMoment);
        assertZero(trainer.optimizer().secondMoment);
        trainer.gradients.values.groups[1][WHITE_PAWN * 64] = Float.POSITIVE_INFINITY;
        assertThrows(ArithmeticException.class, () -> trainer.gradients.average(1));
    }

    @Test
    void invalidSamplesAndForwardOverflowDoNotChangeOptimizerBoundary() {
        NnueTrainer trainer = new NnueTrainer(model());
        Parameters before = Parameters.from(trainer.model().snapshot());
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.01, 1.01}) {
            assertThrows(IllegalArgumentException.class, () -> trainer.trainBatch(new long[][] {WHITE, BLACK}, new double[] {0, bad}, 2));
        }
        for (int count : new int[] {-1, 0, 2}) {
            assertThrows(IllegalArgumentException.class, () -> trainer.trainBatch(new long[][] {WHITE}, new double[] {0}, count));
        }
        assertThrows(NullPointerException.class, () -> trainer.trainBatch(new long[][] {null}, new double[] {0}, 1));
        long[] invalid = new long[Board.MAX_BITBOARDS];
        assertThrows(IllegalArgumentException.class, () -> trainer.trainBatch(new long[][] {WHITE, invalid}, new double[] {0, 0}, 2));
        long[] encoding = WHITE.clone();
        encoding[0] |= 1L << 20;
        encoding[1] |= 1L << 20;
        encoding[2] |= 1L << 20;
        assertThrows(IllegalArgumentException.class, () -> trainer.predict(encoding));
        assertBits(before, trainer.model().parameters);
        assertEquals(0, trainer.optimizer().step());
        assertZero(trainer.optimizer().firstMoment);
        assertZero(trainer.optimizer().secondMoment);
        // Parameters are finite, but forward accumulation overflows: clip01 cannot hide it.
        trainer.model().parameters.groups[0][0] = Float.MAX_VALUE;
        trainer.model().parameters.groups[1][WHITE_PAWN * 64] = Float.MAX_VALUE;
        assertThrows(ArithmeticException.class, () -> trainer.trainBatch(new long[][] {WHITE}, new double[] {0}, 1));
        assertEquals(0, trainer.optimizer().step());
        assertEquals(Float.MAX_VALUE, trainer.model().parameters.groups[0][0]);
    }

    @Test
    void hyperparametersAndDecodedStateAreStrictAndStepCannotWrap() {
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> new AdamHyperparameters(bad, 0.9, 0.999, 1e-8));
            assertThrows(IllegalArgumentException.class, () -> new AdamHyperparameters(0.001, 0.9, 0.999, bad));
        }
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.1, 1, 1.1}) {
            assertThrows(IllegalArgumentException.class, () -> new AdamHyperparameters(0.001, bad, 0.999, 1e-8));
            assertThrows(IllegalArgumentException.class, () -> new AdamHyperparameters(0.001, 0.9, bad, 1e-8));
        }
        assertDoesNotThrow(() -> new AdamHyperparameters(0.001, 0, 0, 1e-8));
        Parameters first = new Parameters(), second = new Parameters();
        second.groups[5][31] = -1;
        assertThrows(IllegalArgumentException.class, () -> new AdamOptimizer(AdamHyperparameters.DEFAULT, 1, first, second));
        second.groups[5][31] = 0;
        first.groups[5][31] = 1;
        assertThrows(IllegalArgumentException.class, () -> new AdamOptimizer(AdamHyperparameters.DEFAULT, 0, first, second));
        first.groups[5][31] = 0;
        AdamOptimizer exhausted = new AdamOptimizer(AdamHyperparameters.DEFAULT, Long.MAX_VALUE, first, second);
        assertThrows(ArithmeticException.class, () -> exhausted.update(new Parameters(), new BatchGradients()));
        assertEquals(Long.MAX_VALUE, exhausted.step());
        Parameters corrupt = new Parameters();
        corrupt.groups[3][0] = Float.NaN;
        assertThrows(IllegalArgumentException.class, () -> new TrainableNnue(corrupt));
    }

    private static void assertZero(Parameters parameters) {
        for (float[] group : parameters.groups) {
            assertTrue(Arrays.equals(group, new float[group.length]));
        }
    }
}
