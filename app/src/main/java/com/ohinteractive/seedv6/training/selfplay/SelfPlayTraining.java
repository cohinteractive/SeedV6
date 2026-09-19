package com.ohinteractive.seedv6.training.selfplay;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;

/** One finite generate-then-train operation. Caller exclusively owns the mutable trainer throughout. */
public final class SelfPlayTraining {
    public record Config(int epochs, int minibatchSize, boolean shuffle, long shuffleSeed) {
        public Config {
            if (epochs < 1 || minibatchSize < 1) throw new IllegalArgumentException("Invalid training bounds.");
        }

        public static Config fromBatchSeed(int epochs, int minibatchSize, long batchSeed) {
            return new Config(epochs, minibatchSize, true, batchSeed ^ 0xD1B54A32D192ED03L);
        }
    }

    /** Losses use terminal targets only. Initial/final metrics cover the fixed full sampled dataset. */
    public record Statistics(long samplesTrained, long optimizerUpdates, long initialOptimizerStep,
                             long finalOptimizerStep, double initialLoss, double finalLoss,
                             double meanTrainingLoss, double predictionMean, double targetMean,
                             boolean cancelled) {}

    public record Result(SelfPlayBatch generation, Optional<Statistics> training, NnueNetwork snapshot) {}

    /** One fully applied optimizer boundary; an observer must return promptly and must not mutate the trainer. */
    public record Progress(long samplesTrained, long optimizerUpdates, long initialOptimizerStep,
                           long optimizerStep, double meanTrainingLoss) {}

    public static Result run(NnueTrainer trainer, SelfPlayConfig generationConfig,
                              Config trainingConfig, SelfPlayControl control) {
        return run(trainer, generationConfig, trainingConfig, Board.startingPosition(), control);
    }

    public static Result run(NnueTrainer trainer, SelfPlayConfig generationConfig,
                              Config trainingConfig, long[] initialBoard, SelfPlayControl control) {
        Objects.requireNonNull(trainingConfig, "trainingConfig");
        NnueNetwork actor = trainer.model().snapshot();
        SelfPlayBatch batch = SelfPlayBatch.generate(actor, generationConfig, initialBoard, control);
        // Cancellation never implicitly trains a partial generation result. Explicit train() is available.
        Optional<Statistics> training = control.cancelled() || batch.cancelled()
                ? Optional.empty() : train(trainer, batch, trainingConfig, control);
        return new Result(batch, training, trainer.model().snapshot());
    }

    /**
     * Explicitly trains completed-game samples, including a caller-approved partial batch.
     * For a cancelled generation use a fresh control. Cancellation is observed between Adam updates;
     * a started minibatch finishes atomically according to Workstream D semantics.
     * Empty data or cancellation before training performs no optimizer work and returns empty.
     */
    public static Optional<Statistics> train(NnueTrainer trainer, SelfPlayBatch batch,
                                             Config config, SelfPlayControl control) {
        return train(trainer, batch, config, control, progress -> {});
    }

    public static Optional<Statistics> train(NnueTrainer trainer, SelfPlayBatch batch,
                                             Config config, SelfPlayControl control, Consumer<Progress> observer) {
        return trainSamples(trainer, batch.samples(), config, control, observer);
    }

    /** Explicit completed-game dataset, for research with whole games reserved outside training. */
    public static Optional<Statistics> trainSamples(NnueTrainer trainer, List<TrajectorySampler.Sample> samples,
            Config config, SelfPlayControl control, Consumer<Progress> observer) {
        samples = List.copyOf(samples);
        Objects.requireNonNull(trainer, "trainer");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(observer, "observer");
        if (samples.isEmpty() || control.cancelled()) return Optional.empty();
        long initialStep = trainer.optimizer().step();
        Metrics before = metrics(trainer, samples, control);
        if (control.cancelled()) return Optional.empty();
        int[] order = new int[samples.size()];
        int capacity = Math.min(config.minibatchSize(), samples.size());
        long[][] boards = new long[capacity][Board.MAX_BITBOARDS];
        double[] targets = new double[capacity];
        SplittableRandom random = new SplittableRandom(config.shuffleSeed());
        long trained = 0;
        double lossSum = 0;
        for (int epoch = 0; epoch < config.epochs() && !control.cancelled(); epoch++) {
            for (int i = 0; i < order.length; i++) order[i] = i;
            if (config.shuffle()) {
                for (int i = order.length - 1; i > 0; i--) {
                    int other = random.nextInt(i + 1);
                    int swap = order[i]; order[i] = order[other]; order[other] = swap;
                }
            }
            for (int start = 0; start < order.length && !control.cancelled();) {
                int count = Math.min(capacity, order.length - start);
                for (int i = 0; i < count; i++) {
                    TrajectorySampler.Sample sample = samples.get(order[start + i]);
                    sample.copyBoardInto(boards[i]);
                    targets[i] = sample.target();
                }
                NnueTrainer.BatchStatistics statistics = trainer.trainBatch(boards, targets, count);
                trained += count;
                lossSum += statistics.meanLoss() * count;
                start += count;
                observer.accept(new Progress(trained, trainer.optimizer().step() - initialStep, initialStep,
                        trainer.optimizer().step(), lossSum / trained));
            }
        }
        if (trained == 0) return Optional.empty();
        Metrics after = metrics(trainer, samples, control);
        long finalStep = trainer.optimizer().step();
        return Optional.of(new Statistics(trained, finalStep - initialStep, initialStep, finalStep,
                before.loss(), after.loss(), lossSum / trained, after.prediction(), after.target(), control.cancelled()));
    }

    private record Metrics(double loss, double prediction, double target) {}

    private static Metrics metrics(NnueTrainer trainer, List<TrajectorySampler.Sample> samples, SelfPlayControl control) {
        long[] board = new long[Board.MAX_BITBOARDS];
        double loss = 0, prediction = 0, target = 0;
        for (TrajectorySampler.Sample sample : samples) {
            if (control.cancelled()) return new Metrics(Double.NaN, Double.NaN, Double.NaN);
            sample.copyBoardInto(board);
            double value = trainer.predict(board);
            double difference = value - sample.target();
            loss += 0.5 * difference * difference;
            prediction += value;
            target += sample.target();
        }
        return new Metrics(loss / samples.size(), prediction / samples.size(), target / samples.size());
    }

    private SelfPlayTraining() {}
}
