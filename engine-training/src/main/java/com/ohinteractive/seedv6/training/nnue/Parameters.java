package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.nnue.NnueNetwork;

/** Package-owned arrays. Group order is also the codec order; no arrays escape publicly. */
final class Parameters {
    static final int FEATURE_BIAS = 0, FEATURES = 1, HIDDEN_BIAS = 2, HIDDEN = 3,
            OUTPUT_BIAS = 4, OUTPUT = 5;
    final float[][] groups = {
            new float[NnueNetwork.ACCUMULATOR_SIZE],
            new float[NnueNetwork.FEATURE_WEIGHT_COUNT],
            new float[NnueNetwork.HIDDEN_SIZE],
            new float[NnueNetwork.HIDDEN_WEIGHT_COUNT],
            new float[1], new float[NnueNetwork.HIDDEN_SIZE]
    };

    static Parameters from(NnueNetwork network) {
        Parameters p = new Parameters();
        for (int i = 0; i < p.groups[0].length; i++) p.groups[0][i] = network.featureBias(i);
        for (int i = 0; i < p.groups[1].length; i++) {
            p.groups[1][i] = network.featureWeight(i / 64, i % 64);
        }
        for (int i = 0; i < p.groups[2].length; i++) p.groups[2][i] = network.hiddenBias(i);
        for (int i = 0; i < p.groups[3].length; i++) {
            p.groups[3][i] = network.hiddenWeight(i / 128, i % 128);
        }
        p.groups[4][0] = network.outputBias();
        for (int i = 0; i < p.groups[5].length; i++) p.groups[5][i] = network.outputWeight(i);
        return p;
    }

    NnueNetwork snapshot() {
        return NnueNetwork.of(1, groups[0], groups[1], groups[2], groups[3], groups[4][0], groups[5]);
    }

    void validate(boolean secondMoment) {
        for (float[] group : groups) {
            for (float value : group) {
                if (!Float.isFinite(value) || (secondMoment && value < 0)) {
                    throw new IllegalArgumentException("Invalid parameter/moment state.");
                }
            }
        }
    }
}
