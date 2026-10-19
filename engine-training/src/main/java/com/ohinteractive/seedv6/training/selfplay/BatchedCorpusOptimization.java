package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.core.Board;

import com.ohinteractive.seedv6.training.selfplay.SelfPlayControl;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayTraining.*;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler;
import java.util.*;
import java.util.function.*;

/** Shared bounded corpus loop; architecture owns update mathematics and optimizer state. */
public final class BatchedCorpusOptimization {
    @FunctionalInterface public interface BatchUpdate { double train(long[][] boards, double[] targets, int count); }
    /** Bounded corpus minibatches with exact continuation at optimizer boundaries. */
    public static Optional<Statistics> trainSamples(LongSupplier step, BatchUpdate update, ToDoubleFunction<long[]> predict, List<TrajectorySampler.Sample> samples,
            Config config, SelfPlayControl control, Consumer<Progress> observer,
            ToDoubleFunction<TrajectorySampler.Sample> target) {
        samples = SampleStore.copyOf(samples);
        Objects.requireNonNull(step); Objects.requireNonNull(update); Objects.requireNonNull(predict);
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(target, "target");
        if (samples.isEmpty() || control.cancelled()) return Optional.empty();
        var cursor = control.takeTrainingStart();
        long initialStep = cursor.initialStep() < 0 ? step.getAsLong() : cursor.initialStep();
        if (step.getAsLong() != initialStep + cursor.updates() || cursor.samples() > (long) samples.size() * config.epochs())
            throw new IllegalArgumentException("Invalid training continuation position.");
        Metrics before = cursor.initialStep() < 0 ? metrics(predict, samples, control, target) : new Metrics(cursor.initialLoss(), Double.NaN, Double.NaN);
        if (control.cancelled()) return Optional.empty();
        try (var order = new TrainingOrder(samples.size())) {
            int capacity = Math.min(config.minibatchSize(), samples.size());
            long[][] boards = new long[capacity][Board.MAX_BITBOARDS];
            double[] targets = new double[capacity];
            SplittableRandom random = new SplittableRandom(config.shuffleSeed());
            long trained = cursor.samples();
            double lossSum = cursor.lossSum();
            control.recordTraining(trained, cursor.updates(), initialStep, before.loss(), lossSum);
            for (int epoch = 0; epoch < config.epochs() && !control.cancelled(); epoch++) {
                if (!order.reset(config.shuffle(), random, control)) break;
                int resume = (int) Math.max(0, Math.min(order.length, cursor.samples() - (long) epoch * order.length));
                try (var prefetched = new PrefetchedSamples(samples, order, resume)) {
                    for (int start = 0; start < order.length && !control.cancelled();) {
                        int count = Math.min(capacity, order.length - start);
                        long position = (long) epoch * order.length + start;
                        if (position < cursor.samples()) {
                            if (position + count > cursor.samples()) throw new IllegalArgumentException("Continuation is inside an optimizer batch.");
                            start += count; continue;
                        }
                        for (int i = 0; i < count; i++) {
                            TrajectorySampler.Sample sample = prefetched.next();
                            sample.copyBoardInto(boards[i]);
                            targets[i] = target.applyAsDouble(sample);
                        }
                        double meanLoss = update.train(boards, targets, count);
                        trained += count;
                        lossSum += meanLoss * count;
                        start += count;
                        control.recordTraining(trained, step.getAsLong() - initialStep, initialStep, before.loss(), lossSum);
                        observer.accept(new Progress(trained, step.getAsLong() - initialStep, initialStep,
                                step.getAsLong(), lossSum / trained));
                    }
                }
            }
            if (trained == 0) return Optional.empty();
            Metrics after = metrics(predict, samples, control, target);
            long finalStep = step.getAsLong();
            return Optional.of(new Statistics(trained, finalStep - initialStep, initialStep, finalStep,
                    before.loss(), after.loss(), lossSum / trained, after.prediction(), after.target(), control.cancelled()));
        }
    }

    private record Metrics(double loss, double prediction, double target) {}

    private static Metrics metrics(ToDoubleFunction<long[]> predict, List<TrajectorySampler.Sample> samples, SelfPlayControl control,
            ToDoubleFunction<TrajectorySampler.Sample> targets) {
        long[] board = new long[Board.MAX_BITBOARDS];
        double loss = 0, prediction = 0, target = 0;
        for (TrajectorySampler.Sample sample : samples) {
            if (control.cancelled()) return new Metrics(Double.NaN, Double.NaN, Double.NaN);
            sample.copyBoardInto(board);
            double value = predict.applyAsDouble(board);
            double expected = targets.applyAsDouble(sample);
            double difference = value - expected;
            loss += 0.5 * difference * difference;
            prediction += value;
            target += expected;
        }
        return new Metrics(loss / samples.size(), prediction / samples.size(), target / samples.size());
    }

    private BatchedCorpusOptimization(){}
}
