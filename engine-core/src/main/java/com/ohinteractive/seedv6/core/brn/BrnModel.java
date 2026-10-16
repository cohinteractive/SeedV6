package com.ohinteractive.seedv6.core.brn;

import java.util.Objects;

/** Immutable, shareable binary64 BRN-0 weights. No optimizer or engine score conversion. */
public final class BrnModel {
    private final double[] weights;

    public BrnModel() { weights = new double[BrnFeatureSchema.PARAMETER_COUNT]; }

    /** Copies caller-owned parameters in schema order. */
    public BrnModel(double[] weights) {
        validate(weights, false);
        this.weights = weights.clone();
    }

    /** Normalized side-to-move value. Caller owns scratch; successful calls allocate nothing. */
    public double evaluate(long[] board, BrnFeatures scratch) {
        scratch.extract(board);
        return scratch.value(weights);
    }

    public double weight(int index) { return weights[index]; }
    public double[] copyWeights() { return weights.clone(); }

    static void validate(double[] values, boolean nonnegative) {
        Objects.requireNonNull(values, "values");
        if (values.length != BrnFeatureSchema.PARAMETER_COUNT) throw new IllegalArgumentException("Wrong BRN parameter count.");
        for (double value : values) {
            if (!Double.isFinite(value) || (nonnegative && value < 0)) {
                throw new IllegalArgumentException("Invalid BRN parameter/moment.");
            }
        }
    }
}
