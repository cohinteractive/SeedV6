package com.ohinteractive.seedv6.training.nnue;

import org.junit.jupiter.api.Test;

import static com.ohinteractive.seedv6.training.nnue.TrainingFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class NnueGradientTest {
    @Test
    void centralDifferencesCoverEveryGroupBothOrdersAndBothTransformerBranches() {
        NnueTrainer trainer = new NnueTrainer(model());
        // Exact power of two fits float parameters; large enough to avoid float cancellation,
        // small enough to stay well clear of clip boundaries and limit O(epsilon^2) error.
        float epsilon = 1.0f / 1024;
        int[][] selected = {
                {1, WHITE_PAWN * 64}, {1, BLACK_PAWN * 64}, {1, SHARED_KING * 64},
                {0, 0}, {3, 0}, {3, 64}, {2, 0}, {5, 0}, {4, 0}, {1, 0}
        };
        double maximumDifference = 0;
        for (long[] board : new long[][] {WHITE, BLACK}) {
            trainer.accumulate(new long[][] {board}, new double[] {-0.4}, 1);
            for (float x : trainer.scratch.white) assertTrue(x > 0.01 && x < 0.99);
            for (float x : trainer.scratch.black) assertTrue(x > 0.01 && x < 0.99);
            for (float x : trainer.scratch.hiddenRaw) assertTrue(x > 0.01 && x < 0.99);
            for (int[] parameter : selected) {
                float[] weights = trainer.model().parameters.groups[parameter[0]];
                int index = parameter[1];
                float original = weights[index];
                double analytic = trainer.gradients.values.groups[parameter[0]][index];
                weights[index] = original + epsilon;
                double plus = loss(trainer, board, -0.4);
                weights[index] = original - epsilon;
                double minus = loss(trainer, board, -0.4);
                weights[index] = original;
                double numerical = (plus - minus) / (2 * epsilon);
                maximumDifference = Math.max(maximumDifference, Math.abs(analytic - numerical));
                assertEquals(numerical, analytic, 2e-6 + 2e-4 * Math.abs(numerical),
                        "group=" + parameter[0] + " index=" + index);
                if (parameter[0] == 1 && index == 0) assertEquals(0, analytic);
                else assertNotEquals(0, analytic);
            }
        }
        System.out.printf("GRADIENT epsilon=%.12f absTolerance=2e-6 relTolerance=2e-4 maxDifference=%.12g%n",
                epsilon, maximumDifference);
    }

    @Test
    void sharedRowsAndBiasSumBothPerspectivesAndSideToMoveSwapsTheBranches() {
        NnueTrainer trainer = new NnueTrainer(model());
        for (long[] board : new long[][] {WHITE, BLACK}) {
            trainer.accumulate(new long[][] {board}, new double[] {-0.4}, 1);
            double v = trainer.scratch.value;
            double d = (v + 0.4) * (1 - v * v) * 0.5;
            double white = d * (board == WHITE ? 0.25 : -0.125);
            double black = d * (board == WHITE ? -0.125 : 0.25);
            float[][] g = trainer.gradients.values.groups;
            assertEquals(white, g[1][WHITE_PAWN * 64], 1e-8);
            assertEquals(black, g[1][BLACK_PAWN * 64], 1e-8);
            assertEquals(white + black, g[1][SHARED_KING * 64], 1e-8);
            assertEquals(white + black, g[0][0], 1e-8);
            assertTrue(trainer.gradients.count < 2 * trainer.scratch.features,
                    "Overlapping shared rows must occupy one touch entry");
        }
    }

    @Test
    void clippingDerivativeIsZeroAtBothBoundariesAndOutsideForBothLayers() {
        NnueTrainer trainer = new NnueTrainer(model());
        Parameters p = trainer.model().parameters;
        p.groups[1][WHITE_PAWN * 64] = 0;
        p.groups[1][BLACK_PAWN * 64] = 0;
        for (float x : new float[] {-0.25f, 0, 0.5f, 1, 1.25f}) {
            int expected = x > 0 && x < 1 ? 1 : 0;
            assertEquals(expected, TrainingScratch.clipDerivative(x));
            p.groups[0][0] = x;
            trainer.accumulate(new long[][] {WHITE}, new double[] {-0.4}, 1);
            float gradient = trainer.gradients.values.groups[0][0];
            if (expected == 0) assertEquals(0, gradient);
            else assertNotEquals(0, gradient);
        }
        p.groups[0][0] = 0.25f;
        p.groups[3][0] = 0;
        p.groups[3][64] = 0;
        for (float x : new float[] {-0.25f, 0, 0.5f, 1, 1.25f}) {
            p.groups[2][0] = x;
            trainer.accumulate(new long[][] {WHITE}, new double[] {-0.4}, 1);
            float gradient = trainer.gradients.values.groups[2][0];
            if (x <= 0 || x >= 1) assertEquals(0, gradient);
            else assertNotEquals(0, gradient);
        }
    }

    @Test
    void distinctTargetsAndPositionsProduceTheMeanOfTheirIndependentGradients() {
        NnueTrainer trainer = new NnueTrainer(model());
        int[][] selected = {{0, 0}, {1, WHITE_PAWN * 64}, {1, BLACK_PAWN * 64},
                {1, SHARED_KING * 64}, {2, 0}, {3, 0}, {3, 64}, {4, 0}, {5, 0}};
        float[] first = new float[selected.length];
        trainer.accumulate(new long[][] {WHITE}, new double[] {0.8}, 1);
        for (int i = 0; i < selected.length; i++) {
            first[i] = trainer.gradients.values.groups[selected[i][0]][selected[i][1]];
        }
        trainer.accumulate(new long[][] {BLACK}, new double[] {-0.3}, 1);
        float[] expected = new float[selected.length];
        for (int i = 0; i < selected.length; i++) {
            expected[i] = (first[i] + trainer.gradients.values.groups[selected[i][0]][selected[i][1]]) / 2;
        }
        trainer.accumulate(new long[][] {WHITE, BLACK}, new double[] {0.8, -0.3}, 2);
        for (int i = 0; i < selected.length; i++) {
            // Shared row additions have a different rounding grouping within the batch.
            assertEquals(expected[i], trainer.gradients.values.groups[selected[i][0]][selected[i][1]], 2e-8);
        }
        assertEquals(0, trainer.optimizer().step());
    }

    @Test
    void duplicateSamplesAverageOnceAndOldSparseRowsAreClearedEvenAfterFailedBatch() {
        NnueTrainer single = new NnueTrainer(model());
        NnueTrainer duplicate = new NnueTrainer(model());
        NnueTrainer.BatchStatistics one = single.accumulate(new long[][] {WHITE}, new double[] {0.75}, 1);
        NnueTrainer.BatchStatistics two = duplicate.accumulate(
                new long[][] {WHITE, WHITE}, new double[] {0.75, 0.75}, 2);
        assertEquals(one.meanLoss(), two.meanLoss());
        assertEquals(one.meanPrediction(), two.meanPrediction());
        assertEquals(0.75, two.meanTarget());
        assertEquals(2, two.samples());
        assertBits(single.gradients.values, duplicate.gradients.values);
        single.trainBatch(new long[][] {WHITE}, new double[] {0.75}, 1);
        duplicate.trainBatch(new long[][] {WHITE, WHITE}, new double[] {0.75, 0.75}, 2);
        assertBits(single.model().parameters, duplicate.model().parameters);
        assertEquals(1, duplicate.optimizer().step());
        assertThrows(IllegalArgumentException.class, () -> single.accumulate(
                new long[][] {WHITE, new long[0]}, new double[] {0.5, 0.5}, 2));
        single.accumulate(new long[][] {OTHER}, new double[] {-0.5}, 1);
        assertFalse(single.gradients.touched[WHITE_PAWN]);
        for (int i = 0; i < 64; i++) assertEquals(0, single.gradients.values.groups[1][WHITE_PAWN * 64 + i]);
        NnueTrainer fresh = new NnueTrainer(TrainableNnue.fromNetwork(single.model().snapshot()));
        fresh.accumulate(new long[][] {OTHER}, new double[] {-0.5}, 1);
        assertBits(fresh.gradients.values, single.gradients.values);
    }
}
