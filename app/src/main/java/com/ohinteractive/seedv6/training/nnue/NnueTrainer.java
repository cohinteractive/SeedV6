package com.ohinteractive.seedv6.training.nnue;

import java.util.Objects;

/**
 * Offline, single-owner minibatch training. Caller retains board arrays/targets and must
 * not mutate them during a call. Targets are finite [-1,+1] values FROM SIDE TO MOVE:
 * +1 favorable eventual outcome, 0 draw, -1 unfavorable. Generated labels are terminal WDL;
 * explicit corpus supervision estimates the same outcome through STOCKFISH_WDL_V1.
 * No per-sample allocation. Scratch and gradients are retained; one statistics record per batch.
 */
public final class NnueTrainer {
    private final TrainableNnue model;
    private final boolean materialBootstrap;
    private final AdamOptimizer optimizer;
    final TrainingScratch scratch = new TrainingScratch();
    final BatchGradients gradients = new BatchGradients();

    public NnueTrainer(TrainableNnue model) { this(model, AdamHyperparameters.DEFAULT); }

    public NnueTrainer(TrainableNnue model, AdamHyperparameters hyperparameters) {
        this(model, new AdamOptimizer(hyperparameters));
    }

    NnueTrainer(TrainableNnue model, AdamOptimizer optimizer) {
        this(model, optimizer, false);
    }

    NnueTrainer(TrainableNnue model, AdamOptimizer optimizer, boolean materialBootstrap) {
        this.materialBootstrap = materialBootstrap;
        this.model = Objects.requireNonNull(model, "model");
        this.optimizer = Objects.requireNonNull(optimizer, "optimizer");
    }

    public static NnueTrainer materialParity(TrainableNnue model) {
        return new NnueTrainer(model, new AdamOptimizer(AdamHyperparameters.DEFAULT), true);
    }
    public boolean materialBootstrap() { return materialBootstrap; }
    public TrainableNnue model() { return model; }
    public AdamOptimizer optimizer() { return optimizer; }

    /** Full recomputation. Material parity trains the continuous combined score, not the neural term alone. */
    public double predict(long[] board) {
        double neural = scratch.forward(model.parameters, board);
        if (!materialBootstrap) return neural;
        int material = com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.forSideToMove(board,
                com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.whiteScore(board));
        return com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.combinedOutcome(neural, material);
    }

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
            if (materialBootstrap) {
                // L=.5*(clip(tanh(raw)+M/32511)-target)^2. M is fixed, never optimized.
                double derivative = Math.abs(value) < 1 ? difference * (1 - scratch.value * scratch.value) : 0;
                scratch.backwardRaw(model.parameters, gradients, derivative);
            } else scratch.backward(model.parameters, gradients, targets[i]);
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
