package com.ohinteractive.seedv6.training.nnue;

/** Conventional defaults, not tuned for Seed. Values are serialized as exact binary64 bits. */
public record AdamHyperparameters(double learningRate, double beta1, double beta2, double epsilon) {
    public static final AdamHyperparameters DEFAULT = new AdamHyperparameters(0.001, 0.9, 0.999, 1e-8);

    public AdamHyperparameters {
        if (!Double.isFinite(learningRate) || learningRate <= 0
                || !Double.isFinite(beta1) || beta1 < 0 || beta1 >= 1
                || !Double.isFinite(beta2) || beta2 < 0 || beta2 >= 1
                || !Double.isFinite(epsilon) || epsilon <= 0) {
            throw new IllegalArgumentException("Invalid Adam hyperparameters.");
        }
    }
}
