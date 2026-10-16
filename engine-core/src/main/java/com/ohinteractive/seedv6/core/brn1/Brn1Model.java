package com.ohinteractive.seedv6.core.brn1;

import java.util.Objects;
import java.util.Random;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;

/** Immutable binary64 BRN-1: pooled primitive embeddings, ReLU, linear output, tanh. */
public final class Brn1Model {
    public static final int HIDDEN_WIDTH = 32;
    public static final int FEATURE_COUNT = BrnFeatureSchema.PARAMETER_COUNT - 1;
    public static final int HIDDEN_BIAS_OFFSET = FEATURE_COUNT * HIDDEN_WIDTH;
    public static final int OUTPUT_WEIGHT_OFFSET = HIDDEN_BIAS_OFFSET + HIDDEN_WIDTH;
    public static final int OUTPUT_BIAS = OUTPUT_WEIGHT_OFFSET + HIDDEN_WIDTH;
    public static final int PARAMETER_COUNT = OUTPUT_BIAS + 1;
    /** Fixed bootstrap seed; run/shuffle seeds do not change fresh BRN-1 weights. */
    public static final long INITIALIZATION_SEED = 0x533642524e310001L;
    private final double[] weights;

    public Brn1Model() {
        weights = new double[PARAMETER_COUNT];
        // java.util.Random specifies its reproducible algorithm, including nextDouble().
        Random random = new Random(INITIALIZATION_SEED);
        for (int i = 0; i < HIDDEN_BIAS_OFFSET; i++) weights[i] = (2 * random.nextDouble() - 1) * .01;
        double bound = StrictMath.sqrt(6.0 / (HIDDEN_WIDTH + 1));
        for (int c = 0; c < HIDDEN_WIDTH; c++) weights[OUTPUT_WEIGHT_OFFSET + c] = (2 * random.nextDouble() - 1) * bound;
    }

    public Brn1Model(double[] weights) {
        validate(weights, false);
        this.weights = weights.clone();
    }

    /** Shared BRN schema IDs remain unchanged (1..26224); ID 0 is the old scalar bias. */
    public static int embeddingIndex(int featureId, int channel) {
        if (featureId < BrnFeatureSchema.NODE_OFFSET || featureId >= BrnFeatureSchema.PARAMETER_COUNT)
            throw new IllegalArgumentException("Expected a non-bias BRN feature ID.");
        Objects.checkIndex(channel, HIDDEN_WIDTH);
        return (featureId - BrnFeatureSchema.NODE_OFFSET) * HIDDEN_WIDTH + channel;
    }

    /** Side-to-move value; scratch is caller-owned and reusable, with no evaluation allocation. */
    public double evaluate(long[] board, Brn1Workspace scratch) { return scratch.evaluate(board, weights); }
    public double weight(int index) { return weights[index]; }
    public double[] copyWeights() { return weights.clone(); }

    static void validate(double[] values, boolean nonnegative) {
        Objects.requireNonNull(values, "values");
        if (values.length != PARAMETER_COUNT) throw new IllegalArgumentException("Wrong BRN-1 parameter count.");
        for (double value : values) {
            if (!Double.isFinite(value) || (nonnegative && value < 0))
                throw new IllegalArgumentException("Invalid BRN-1 parameter/moment.");
        }
    }
}
