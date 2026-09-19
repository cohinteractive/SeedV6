package com.ohinteractive.seedv6.core.nnue;

/** Shared V1 float kernels for immutable inference and offline training. No FMA or allocation. */
public final class NnueMath {
    private NnueMath() {}

    /** Caller supplies correctly sized arrays. Row additions retain ascending unit order. */
    public static void addFeature(float[] weights, int feature, float[] accumulator) {
        int offset = feature * NnueNetwork.ACCUMULATOR_SIZE;
        for (int unit = 0; unit < NnueNetwork.ACCUMULATOR_SIZE; unit++) {
            accumulator[unit] += weights[offset + unit];
        }
    }

    /**
     * Caller supplies correctly sized arrays. Optional preActivations retains hidden raw
     * sums for training; null requests inference only. Input is already clipped and ordered
     * side-to-move first. Arithmetic/order is the accepted V1 inference calculation.
     */
    public static float forward(float[] hiddenBias, float[] hiddenWeights,
                                float outputBias, float[] outputWeights, float[] input,
                                float[] preActivations, float[] hidden) {
        for (int unit = 0; unit < NnueNetwork.HIDDEN_SIZE; unit++) {
            float sum = hiddenBias[unit];
            int offset = unit * NnueNetwork.CONCATENATED_SIZE;
            for (int i = 0; i < NnueNetwork.CONCATENATED_SIZE; i++) {
                sum += hiddenWeights[offset + i] * input[i];
            }
            if (preActivations != null) preActivations[unit] = sum;
            hidden[unit] = NnueNetwork.clip01(sum);
        }
        float raw = outputBias;
        for (int unit = 0; unit < NnueNetwork.HIDDEN_SIZE; unit++) {
            raw += outputWeights[unit] * hidden[unit];
        }
        return raw;
    }
}
