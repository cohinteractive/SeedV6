package com.ohinteractive.seedv6.core.brn;

import java.util.Objects;

/**
 * Single-owner, online sparse Adam. Targets and predictions are from side to move.
 * Inactive parameters AND moments freeze; corrections use the global successful step.
 * This is masked sparse Adam, not dense Adam with zero gradients on absent features.
 */
public final class BrnTrainer {
    final double[] weights;
    private final BrnAdamConfig config;
    private final BrnAdamState optimizer;
    private final BrnFeatures features = new BrnFeatures();
    private final int[] multiplicities = new int[BrnFeatureSchema.PARAMETER_COUNT];
    private final int[] touched = new int[BrnFeatureSchema.MAX_ACTIVE_FEATURES];
    private int touchedCount;

    public BrnTrainer(double learningRate) { this(new BrnModel(), new BrnAdamConfig(learningRate)); }

    public BrnTrainer(BrnModel initial, BrnAdamConfig config) {
        this(initial.copyWeights(), config, new BrnAdamState());
    }

    BrnTrainer(double[] weights, BrnAdamConfig config, BrnAdamState optimizer) {
        BrnModel.validate(weights, false);
        this.weights = weights;
        this.config = Objects.requireNonNull(config, "config");
        this.optimizer = Objects.requireNonNull(optimizer, "optimizer");
    }

    public BrnAdamConfig config() { return config; }
    public BrnAdamState optimizer() { return optimizer; }
    public BrnModel snapshot() { return new BrnModel(weights); }

    public double predict(long[] board) {
        features.extract(board);
        return features.value(weights);
    }

    /**
     * Returns pre-update 0.5*(prediction-target)^2. Aggregates repeated feature gradients,
     * then updates each active parameter once. Invalid input or nonfinite candidates leave
     * all weights, moments and step untouched. Successful calls allocate nothing.
     */
    public double train(long[] board, double target) {
        if (!Double.isFinite(target) || target < -1 || target > 1) {
            throw new IllegalArgumentException("Target must be finite and in [-1,+1].");
        }
        if (optimizer.step == Long.MAX_VALUE) throw new ArithmeticException("Adam step exhausted.");
        double prediction = predict(board);
        double difference = prediction - target;
        double derivative = difference * (1 - prediction * prediction);
        for (int i = 0; i < touchedCount; i++) multiplicities[touched[i]] = 0;
        touchedCount = 0;
        for (int i = 0; i < features.size(); i++) {
            int index = features.indexAt(i);
            if (multiplicities[index]++ == 0) touched[touchedCount++] = index;
        }
        long next = optimizer.step + 1;
        double c1 = 1 - StrictMath.pow(config.beta1(), next);
        double c2 = 1 - StrictMath.pow(config.beta2(), next);
        update(derivative, c1, c2, false);
        update(derivative, c1, c2, true);
        optimizer.step = next;
        return 0.5 * difference * difference;
    }

    private void update(double derivative, double c1, double c2, boolean publish) {
        for (int n = 0; n < touchedCount; n++) {
            int i = touched[n];
            double gradient = derivative * multiplicities[i];
            double m = config.beta1() * optimizer.first[i] + (1 - config.beta1()) * gradient;
            double v = config.beta2() * optimizer.second[i] + (1 - config.beta2()) * gradient * gradient;
            double weight = weights[i] - config.learningRate() * (m / c1)
                    / (StrictMath.sqrt(v / c2) + config.epsilon());
            if (!Double.isFinite(m) || !Double.isFinite(v) || !Double.isFinite(weight)) {
                throw new ArithmeticException("Nonfinite BRN Adam candidate; update not published.");
            }
            if (publish) {
                weights[i] = weight;
                optimizer.first[i] = m;
                optimizer.second[i] = v;
            }
        }
    }
}
