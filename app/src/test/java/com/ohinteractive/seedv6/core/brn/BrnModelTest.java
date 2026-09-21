package com.ohinteractive.seedv6.core.brn;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static com.ohinteractive.seedv6.core.brn.BrnFeatureSchema.*;
import static org.junit.jupiter.api.Assertions.*;

class BrnModelTest {
    @Test
    void all64RawStatusBitsContributeOnlyWhenSet() {
        BrnFeatures scratch = new BrnFeatures();
        long[] board = new long[5];
        for (int bit = 0; bit < 64; bit++) {
            double[] weights = new double[PARAMETER_COUNT];
            weights[statusIndex(bit)] = (bit + 1) / 128.0;
            BrnModel model = new BrnModel(weights);
            board[4] = 1L << bit;
            assertEquals(StrictMath.tanh(weights[statusIndex(bit)]), model.evaluate(board, scratch));
            assertEquals(1, scratch.statusCount());
            assertEquals(statusIndex(bit), scratch.indexAt(1));
            board[4] = ~(1L << bit);
            assertEquals(0, model.evaluate(board, scratch));
        }
    }

    @Test
    void handComputedForwardIncludesMultiplicityAndNoImplicitPerspectiveTransform() {
        long[] board = Board.fromFen("7k/8/8/8/8/8/PPP5/7K w - - 0 1");
        double[] weights = new double[PARAMETER_COUNT];
        weights[BIAS] = 0.125;
        weights[nodeIndex(6, 8)] = 0.25;
        weights[relationIndex(6, 8, 6, 9)] = -0.0625;
        weights[statusIndex(18)] = 0.03125;
        weights[statusIndex(0)] = 0.5;
        BrnModel model = new BrnModel(weights);
        BrnFeatures scratch = new BrnFeatures();
        assertEquals(StrictMath.tanh(0.28125), model.evaluate(board, scratch));
        board[Board.STATUS] ^= 1;
        assertEquals(StrictMath.tanh(0.78125), model.evaluate(board, scratch));
        assertEquals(0, new BrnModel().evaluate(board, scratch));
    }

    @Test
    void deterministicForwardMatchesIndependentDenseSquareOracleAndSnapshotsAreImmutable() {
        Random random = new Random(739);
        double[] weights = new double[PARAMETER_COUNT];
        for (int i = 0; i < weights.length; i++) weights[i] = (random.nextDouble() - 0.5) / 64;
        BrnModel model = new BrnModel(weights);
        BrnFeatures scratch = new BrnFeatures();
        for (int trial = 0; trial < 100; trial++) {
            long[] board = {random.nextLong(), random.nextLong(), random.nextLong(), random.nextLong(), random.nextLong()};
            double z = weights[0];
            for (int a = 0; a < 64; a++) {
                int code = Board.getSquare(board[0], board[1], board[2], board[3], a);
                if (code != 0) z += weights[nodeIndex(code, a)];
            }
            for (int a = 0; a < 64; a++) {
                int codeA = Board.getSquare(board[0], board[1], board[2], board[3], a);
                if (codeA == 0) continue;
                for (int b = a + 1; b < 64; b++) {
                    int codeB = Board.getSquare(board[0], board[1], board[2], board[3], b);
                    if (codeB != 0) z += weights[relationIndex(codeA, a, codeB, b)];
                }
            }
            for (int bit = 0; bit < 64; bit++) if ((board[4] & (1L << bit)) != 0) z += weights[statusIndex(bit)];
            double expected = StrictMath.tanh(z);
            for (int repeat = 0; repeat < 3; repeat++) assertEquals(expected, model.evaluate(board, scratch));
        }
        double original = model.weight(0);
        weights[0] = 42;
        model.copyWeights()[0] = 99;
        assertEquals(original, model.weight(0));
    }

    @Test
    void invalidWeightsAndOverflowAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BrnModel(new double[1]));
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            double[] weights = new double[PARAMETER_COUNT];
            weights[5] = bad;
            assertThrows(IllegalArgumentException.class, () -> new BrnModel(weights));
        }
        double[] weights = new double[PARAMETER_COUNT];
        weights[0] = Double.MAX_VALUE;
        weights[nodeIndex(3, 0)] = Double.MAX_VALUE;
        BrnModel model = new BrnModel(weights);
        assertThrows(ArithmeticException.class, () -> model.evaluate(Board.startingPosition(), new BrnFeatures()));
    }
}
