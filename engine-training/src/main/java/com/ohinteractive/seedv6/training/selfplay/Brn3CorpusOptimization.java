package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayControl;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayTraining.*;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler;
import java.util.*;
import java.util.function.*;

/** Architecture-specific corpus optimizer; acquisition and lifecycle remain in TrainerService. */
public final class Brn3CorpusOptimization {
    /** Bounded corpus minibatches with exact continuation at optimizer boundaries. */
    public static Optional<Statistics> trainSamples(Brn3Trainer trainer, List<TrajectorySampler.Sample> samples,
            Config config, SelfPlayControl control, Consumer<Progress> observer,
            ToDoubleFunction<TrajectorySampler.Sample> target) {
        samples = List.copyOf(samples);
        Objects.requireNonNull(trainer, "trainer");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(target, "target");
        if (samples.isEmpty() || control.cancelled()) return Optional.empty();
        var cursor = control.takeTrainingStart();
        long initialStep = cursor.initialStep() < 0 ? trainer.step() : cursor.initialStep();
        if (trainer.step() != initialStep + cursor.updates() || cursor.samples() > (long) samples.size() * config.epochs())
            throw new IllegalArgumentException("Invalid training continuation position.");
        Metrics before = cursor.initialStep() < 0 ? metrics(trainer, samples, control, target) : new Metrics(cursor.initialLoss(), Double.NaN, Double.NaN);
        if (control.cancelled()) return Optional.empty();
        int[] order = new int[samples.size()];
        int capacity = Math.min(config.minibatchSize(), samples.size());
        long[][] boards = new long[capacity][Board.MAX_BITBOARDS];
        double[] targets = new double[capacity];
        SplittableRandom random = new SplittableRandom(config.shuffleSeed());
        long trained = cursor.samples();
        double lossSum = cursor.lossSum();
        control.recordTraining(trained, cursor.updates(), initialStep, before.loss(), lossSum);
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
                long position = (long) epoch * order.length + start;
                if (position < cursor.samples()) {
                    if (position + count > cursor.samples()) throw new IllegalArgumentException("Continuation is inside an optimizer batch.");
                    start += count; continue;
                }
                for (int i = 0; i < count; i++) {
                    TrajectorySampler.Sample sample = samples.get(order[start + i]);
                    sample.copyBoardInto(boards[i]);
                    targets[i] = target.applyAsDouble(sample);
                }
                double meanLoss = trainer.trainBatch(boards, targets, count);
                trained += count;
                lossSum += meanLoss * count;
                start += count;
                control.recordTraining(trained, trainer.step() - initialStep, initialStep, before.loss(), lossSum);
                observer.accept(new Progress(trained, trainer.step() - initialStep, initialStep,
                        trainer.step(), lossSum / trained));
            }
        }
        if (trained == 0) return Optional.empty();
        Metrics after = metrics(trainer, samples, control, target);
        long finalStep = trainer.step();
        return Optional.of(new Statistics(trained, finalStep - initialStep, initialStep, finalStep,
                before.loss(), after.loss(), lossSum / trained, after.prediction(), after.target(), control.cancelled()));
    }

    private record Metrics(double loss, double prediction, double target) {}

    private static Metrics metrics(Brn3Trainer trainer, List<TrajectorySampler.Sample> samples, SelfPlayControl control,
            ToDoubleFunction<TrajectorySampler.Sample> targets) {
        long[] board = new long[Board.MAX_BITBOARDS];
        double loss = 0, prediction = 0, target = 0;
        for (TrajectorySampler.Sample sample : samples) {
            if (control.cancelled()) return new Metrics(Double.NaN, Double.NaN, Double.NaN);
            sample.copyBoardInto(board);
            double value = trainer.predictOutcome(board);
            double expected = targets.applyAsDouble(sample);
            double difference = value - expected;
            loss += 0.5 * difference * difference;
            prediction += value;
            target += expected;
        }
        return new Metrics(loss / samples.size(), prediction / samples.size(), target / samples.size());
    }

    private Brn3CorpusOptimization(){}
}
