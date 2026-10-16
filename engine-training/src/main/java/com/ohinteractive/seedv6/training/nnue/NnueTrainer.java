package com.ohinteractive.seedv6.training.nnue;

import java.util.Objects;

/**
 * Offline, single-owner minibatch training. Caller retains board arrays/targets and must
 * not mutate them during a call. Targets are finite [-1,+1] values FROM SIDE TO MOVE:
 * +1 favorable eventual outcome, 0 draw, -1 unfavorable. Generated labels are terminal WDL;
 * explicit corpus supervision estimates the same outcome through STOCKFISH_WDL_V1.
 * Forward/backprop scratch and gradients are retained; one statistics record per batch.
 * The calibrated supervision adapter returns small loss/link arrays outside runtime search.
 */
public final class NnueTrainer {
    private final TrainableNnue model;
    private final boolean materialBootstrap;
    private final boolean calibratedOutcome;
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
        this(model, optimizer, materialBootstrap, false);
    }

    NnueTrainer(TrainableNnue model, AdamOptimizer optimizer, boolean materialBootstrap, boolean calibratedOutcome) {
        if (calibratedOutcome && !materialBootstrap) throw new IllegalArgumentException("Calibration requires fixed material");
        this.materialBootstrap = materialBootstrap;
        this.calibratedOutcome = calibratedOutcome;
        this.model = Objects.requireNonNull(model, "model");
        this.optimizer = Objects.requireNonNull(optimizer, "optimizer");
    }

    public static NnueTrainer materialParity(TrainableNnue model) {
        return new NnueTrainer(model, new AdamOptimizer(AdamHyperparameters.DEFAULT), true);
    }
    /** Corrected objective; E008 initializer and search scores are unchanged. */
    public static NnueTrainer calibratedMaterialParity(TrainableNnue model) {
        return new NnueTrainer(model, new AdamOptimizer(AdamHyperparameters.DEFAULT), true, true);
    }
    public boolean calibratedOutcome() { return calibratedOutcome; }
    public boolean materialBootstrap() { return materialBootstrap; }
    public TrainableNnue model() { return model; }
    public AdamOptimizer optimizer() { return optimizer; }

    /** Full recomputation of the persisted outcome semantics, including fixed material. */
    public double predict(long[] board) {
        double score = normalizedScore(board);
        return calibratedOutcome ? com.ohinteractive.seedv6.core.brn3.Brn3Objective.smoothOutcome(
                score * 325.11, NnueCorpusTargets.material(board))[0] : score;
    }

    /** Continuous search score divided by 32511, before the supervision-only WDL link. */
    private double normalizedScore(long[] board) {
        double neural = scratch.forward(model.parameters, board);
        if (!materialBootstrap) return neural;
        int material = com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.forSideToMove(board,
                com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.whiteScore(board));
        return com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.combinedOutcome(neural, material);
    }

    /** Objective loss, including the calibrated score-to-outcome link where persisted. */
    public double loss(long[] board, double target) {
        requireTarget(target);
        if (calibratedOutcome) return com.ohinteractive.seedv6.core.brn3.Brn3Objective.crossEntropy(
                normalizedScore(board) * 325.11, NnueCorpusTargets.material(board), target)[0];
        double difference = predict(board) - target;
        return .5 * difference * difference;
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
            if (calibratedOutcome) {
                // Search uses 100 units/pawn for its fixed prior. Apply the same existing WDL
                // supervision link to the WHOLE score in pawns, not a linear score/MAX outcome.
                double score = normalizedScore(positions[i]);
                int material = NnueCorpusTargets.material(positions[i]);
                var objective = com.ohinteractive.seedv6.core.brn3.Brn3Objective.crossEntropy(score * 325.11, material, targets[i]);
                if (!Double.isFinite(objective[0])) throw new ArithmeticException("Nonfinite loss.");
                loss += objective[0];
                prediction += com.ohinteractive.seedv6.core.brn3.Brn3Objective.smoothOutcome(score * 325.11, material)[0];
                target += targets[i];
                scratch.backwardRaw(model.parameters, gradients, Math.abs(score) < 1
                        ? objective[1] * 325.11 * (1 - scratch.value * scratch.value) : 0);
                continue;
            }
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
