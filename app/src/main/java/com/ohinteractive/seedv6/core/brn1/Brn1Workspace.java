package com.ohinteractive.seedv6.core.brn1;

import com.ohinteractive.seedv6.core.brn.BrnFeatures;
import static com.ohinteractive.seedv6.core.brn1.Brn1Model.*;

/** Thread-confined primitive feature and hidden buffers. No dense input vector. */
public final class Brn1Workspace {
    final BrnFeatures features = new BrnFeatures();
    final double[] hidden = new double[HIDDEN_WIDTH];

    double evaluate(long[] board, double[] weights) {
        features.extract(board);
        System.arraycopy(weights, HIDDEN_BIAS_OFFSET, hidden, 0, HIDDEN_WIDTH);
        // Occurrence zero is BRN-0's scalar bias. BRN-1 instead has 32 hidden biases.
        for (int n = 1; n < features.size(); n++) {
            int row = (features.indexAt(n) - 1) * HIDDEN_WIDTH;
            for (int c = 0; c < HIDDEN_WIDTH; c++) hidden[c] += weights[row + c];
        }
        double z = weights[OUTPUT_BIAS];
        for (int c = 0; c < HIDDEN_WIDTH; c++) {
            if (!Double.isFinite(hidden[c])) throw new ArithmeticException("Nonfinite BRN-1 hidden preactivation.");
            z += weights[OUTPUT_WEIGHT_OFFSET + c] * Math.max(0, hidden[c]);
        }
        if (!Double.isFinite(z)) throw new ArithmeticException("Nonfinite BRN-1 output preactivation.");
        return StrictMath.tanh(z);
    }
}
