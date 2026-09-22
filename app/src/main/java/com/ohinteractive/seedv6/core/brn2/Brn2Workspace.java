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
    private double raw = Double.NaN;

    /** Value-head preactivation from the last successful evaluation, before tanh. */
    public double raw() {
        if (Double.isNaN(raw)) throw new IllegalStateException("No successful BRN-2 evaluation.");
        return raw;
    }

    double evaluate(long[] board, double[] weights) { return evaluate(board, weights, false, false); }
    double evaluate(long[] board, double[] weights, boolean bounded) { return evaluate(board, weights, false, bounded); }
    double evaluateReference(long[] board, double[] weights) { return evaluate(board, weights, true, false); }
    private double evaluate(long[] board, double[] weights, boolean reference, boolean bounded) {
        raw = Double.NaN;
        features.extract(board);
        int nodes = features.nodeCount(), statusStart = 1 + nodes + features.relationCount();
        Arrays.fill(context, 0);
        for (int n = statusStart; n < features.size(); n++) {
            int row = (STATUS_ROW + features.indexAt(n) - BrnFeatureSchema.STATUS_OFFSET) * HIDDEN_WIDTH;
            for (int h = 0; h < HIDDEN_WIDTH; h++) context[h] += weights[row + h];
        }
        if (!bounded) for (double value : context) requireFinite(value);
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
            if (reference) {
                for (int h = 0; h < HIDDEN_WIDTH; h++) {
                    localPre[a * HIDDEN_WIDTH + h] += weights[rowA + h];
                    localPre[b * HIDDEN_WIDTH + h] += weights[rowB + h];
                }
            } else {
                // Independent contiguous destinations avoid alias ambiguity for HotSpot.
                addRow(weights, rowA, a * HIDDEN_WIDTH);
                addRow(weights, rowB, b * HIDDEN_WIDTH);
            }
        }
        System.arraycopy(weights, BOARD_BIAS_OFFSET, boardPre, 0, HIDDEN_WIDTH);
        for (int i = 0; i < nodes; i++) for (int h = 0; h < HIDDEN_WIDTH; h++) {
            double pre = localPre[i * HIDDEN_WIDTH + h];
            if (!bounded) requireFinite(pre);
            boardPre[h] += Math.max(0, pre);
        }
        double z = weights[OUTPUT_BIAS];
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            if (!bounded) requireFinite(boardPre[h]);
            z += weights[OUTPUT_WEIGHT_OFFSET + h] * Math.max(0, boardPre[h]);
        }
        requireFinite(z);
        raw = z;
        return StrictMath.tanh(z);
    }

    private void addRow(double[] weights, int row, int local) {
        for (int h = 0; h < HIDDEN_WIDTH; h++) localPre[local + h] += weights[row + h];
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite BRN-2 preactivation.");
    }
}
