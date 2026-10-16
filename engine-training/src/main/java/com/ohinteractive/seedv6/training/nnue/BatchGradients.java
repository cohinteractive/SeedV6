package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;

import java.util.Arrays;

/** Persistent storage, with clearing and normalization restricted to touched sparse rows. */
final class BatchGradients {
    final Parameters values = new Parameters();
    final boolean[] touched = new boolean[NnueFeatureSchema.FEATURE_COUNT];
    final int[] rows = new int[NnueFeatureSchema.FEATURE_COUNT];
    int count;

    void reset() {
        for (int i = 0; i < count; i++) {
            int row = rows[i];
            int offset = row * NnueNetwork.ACCUMULATOR_SIZE;
            Arrays.fill(values.groups[Parameters.FEATURES], offset,
                    offset + NnueNetwork.ACCUMULATOR_SIZE, 0);
            touched[row] = false;
        }
        count = 0;
        for (int group = 0; group < values.groups.length; group++) {
            if (group != Parameters.FEATURES) Arrays.fill(values.groups[group], 0);
        }
    }

    void addRow(int feature, double[] delta, int start) {
        if (!touched[feature]) {
            touched[feature] = true;
            rows[count++] = feature;
        }
        int offset = feature * NnueNetwork.ACCUMULATOR_SIZE;
        float[] weights = values.groups[Parameters.FEATURES];
        for (int i = 0; i < NnueNetwork.ACCUMULATOR_SIZE; i++) {
            weights[offset + i] += delta[start + i];
        }
    }

    void average(int samples) {
        for (int group = 0; group < values.groups.length; group++) {
            float[] gradient = values.groups[group];
            if (group == Parameters.FEATURES) {
                for (int row = 0; row < count; row++) {
                    int offset = rows[row] * NnueNetwork.ACCUMULATOR_SIZE;
                    normalize(gradient, offset, offset + NnueNetwork.ACCUMULATOR_SIZE, samples);
                }
            } else normalize(gradient, 0, gradient.length, samples);
        }
    }

    private static void normalize(float[] values, int start, int end, int samples) {
        for (int i = start; i < end; i++) {
            if (!Float.isFinite(values[i])) throw new ArithmeticException("Nonfinite batch gradient.");
            values[i] = (float) ((double) values[i] / samples);
        }
    }
}
