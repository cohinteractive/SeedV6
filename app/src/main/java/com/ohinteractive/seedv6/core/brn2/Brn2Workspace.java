package com.ohinteractive.seedv6.core.brn2;

import java.util.Arrays;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;
import com.ohinteractive.seedv6.core.brn.BrnFeatures;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;

/** Thread-confined primitive buffers; no graph objects or evaluation allocations. */
public final class Brn2Workspace {
    final BrnFeatures features = new BrnFeatures();
    final double[] context = new double[HIDDEN_WIDTH];
    final double[] localPre = new double[64 * HIDDEN_WIDTH];
    final double[] boardPre = new double[HIDDEN_WIDTH];

    double evaluate(long[] board, double[] weights) {
        features.extract(board);
        int nodes = features.nodeCount(), statusStart = 1 + nodes + features.relationCount();
        Arrays.fill(context, 0);
        for (int n = statusStart; n < features.size(); n++) {
            int row = (STATUS_ROW + features.indexAt(n) - BrnFeatureSchema.STATUS_OFFSET) * HIDDEN_WIDTH;
            for (int h = 0; h < HIDDEN_WIDTH; h++) context[h] += weights[row + h];
        }
        for (double value : context) requireFinite(value);
        for (int i = 0; i < nodes; i++) {
            int row = (features.indexAt(1 + i) - BrnFeatureSchema.NODE_OFFSET) * HIDDEN_WIDTH;
            for (int h = 0; h < HIDDEN_WIDTH; h++)
                localPre[i * HIDDEN_WIDTH + h] = weights[row + h] + context[h] + weights[LOCAL_BIAS_OFFSET + h];
        }
        // BrnFeatures emits ascending square identities followed by lexicographic (a,b), a<b.
        // Ascending square is exactly canonical endpoint A, independent of input construction order.
        int pair = 1 + nodes;
        for (int a = 0; a < nodes; a++) for (int b = a + 1; b < nodes; b++) {
            int relation = features.indexAt(pair++) - BrnFeatureSchema.RELATION_OFFSET;
            int rowA = (RELATION_A_ROW + relation) * HIDDEN_WIDTH;
            int rowB = (RELATION_B_ROW + relation) * HIDDEN_WIDTH;
            for (int h = 0; h < HIDDEN_WIDTH; h++) {
                localPre[a * HIDDEN_WIDTH + h] += weights[rowA + h];
                localPre[b * HIDDEN_WIDTH + h] += weights[rowB + h];
            }
        }
        System.arraycopy(weights, BOARD_BIAS_OFFSET, boardPre, 0, HIDDEN_WIDTH);
        for (int i = 0; i < nodes; i++) for (int h = 0; h < HIDDEN_WIDTH; h++) {
            double pre = localPre[i * HIDDEN_WIDTH + h];
            requireFinite(pre);
            boardPre[h] += Math.max(0, pre);
        }
        double z = weights[OUTPUT_BIAS];
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            requireFinite(boardPre[h]);
            z += weights[OUTPUT_WEIGHT_OFFSET + h] * Math.max(0, boardPre[h]);
        }
        requireFinite(z);
        return StrictMath.tanh(z);
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite BRN-2 preactivation.");
    }
}
