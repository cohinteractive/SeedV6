package com.ohinteractive.seedv6.core.brn;

/** Exact binary64 configuration, retained in every training payload. */
public record BrnAdamConfig(double learningRate, double beta1, double beta2, double epsilon) {
    public BrnAdamConfig(double learningRate) { this(learningRate, 0.9, 0.999, 1e-8); }

    public BrnAdamConfig {
        if (!Double.isFinite(learningRate) || learningRate <= 0
                || !Double.isFinite(beta1) || beta1 < 0 || beta1 >= 1
                || !Double.isFinite(beta2) || beta2 < 0 || beta2 >= 1
                || !Double.isFinite(epsilon) || epsilon <= 0) {
            throw new IllegalArgumentException("Invalid BRN Adam configuration.");
        }
    }
}
