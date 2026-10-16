package com.ohinteractive.seedv6.training.telemetry;

/** Constant-size, presentation-only optimization facts; no samples or optimizer ownership. */
public record OptimizationSnapshot(String identity, String phase, int epochs, int minibatch,
        Double learningRate, long samples, long targetSamples, long updates, long initialStep, long step,
        double meanLoss, long elapsedNanos, String elapsedScope, Long totalExposure,
        long invocationSamples) {}
