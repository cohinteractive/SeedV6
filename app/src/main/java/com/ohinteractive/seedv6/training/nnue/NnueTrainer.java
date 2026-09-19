package com.ohinteractive.seedv6.training.nnue;

import java.util.Objects;

/**
 * Offline, single-owner minibatch training. Caller retains board arrays/targets and must
 * not mutate them during a call. Targets are finite [-1,+1] values FROM SIDE TO MOVE:
 * +1 favorable eventual outcome, 0 draw, -1 unfavorable. No chess evaluator supplies labels.
 * No per-sample allocation. Scratch and gradients are retained; one statistics record per batch.
 */
public final class NnueTrainer {
    private final TrainableNnue model;
    private final AdamOptimizer optimizer;
    final TrainingScratch scratch = new TrainingScratch();
    final BatchGradients gradients = new BatchGradients();

    public NnueTrainer(TrainableNnue model) { this(model, AdamHyperparameters.DEFAULT); }

    public NnueTrainer(TrainableNnue model, AdamHyperparameters hyperparameters) {
        this(model, new AdamOptimizer(hyperparameters));
    }

    NnueTrainer(TrainableNnue model, AdamOptimizer optimizer) {
        this.model = Objects.requireNonNull(model, "model");
        this.optimizer = Objects.requireNonNull(optimizer, "optimizer");
    }

    public TrainableNnue model() { return model; }
    public AdamOptimizer optimizer() { return optimizer; }

    /** Full recomputation using exactly the immutable V1 kernels; returns tanh(raw). */
    public double predict(long[] board) { return scratch.forward(model.parameters, board); }

    /** Statistics describe the entire minibatch BEFORE its one averaged Adam update. */
    public record BatchStatistics(double meanLoss, double meanPrediction, double meanTarget, int samples) {}

    /**
     * Average accumulated gradients by count, then publish one Adam step. Invalid samples,
     * nonfinite gradients or nonfinite update candidates leave model/moments/step unchanged.
     * Failed scratch/gradient work is disposable and is reset on the next batch attempt.
     */
    public BatchStatistics trainBatch(long[][] positions, double[] targets, int count) {
        BatchStatistics statistics = accumulate(positions, targets, count);
        optimizer.update(model.parameters, gradients);
        return statistics;
    }

    // Package-private mathematical test seam; ordinary public API never exposes arrays/gradients.
    BatchStatistics accumulate(long[][] positions, double[] targets, int count) {
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(targets, "targets");
        if (count <= 0 || count > positions.length || count > targets.length) {
            throw new IllegalArgumentException("Invalid minibatch count.");
        }
        for (int i = 0; i < count; i++) requireTarget(targets[i]);
        gradients.reset();
        double loss = 0, prediction = 0, target = 0;
        for (int i = 0; i < count; i++) {
            double value = predict(positions[i]);
            double difference = value - targets[i];
            double sampleLoss = 0.5 * difference * difference;
            if (!Double.isFinite(sampleLoss)) throw new ArithmeticException("Nonfinite loss.");
            loss += sampleLoss;
            prediction += value;
            target += targets[i];
            scratch.backward(model.parameters, gradients, targets[i]);
        }
        gradients.average(count);
        return new BatchStatistics(loss / count, prediction / count, target / count, count);
    }

    private static void requireTarget(double target) {
        if (!Double.isFinite(target) || target < -1 || target > 1) {
            throw new IllegalArgumentException("Target must be finite and in [-1,+1].");
        }
    }
}
