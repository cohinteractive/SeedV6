package com.ohinteractive.seedv6.training.selfplay;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;

/** BRN-2 online updates: one deterministic shuffled pass over the existing terminal W/D/L samples. */
public final class Brn2SelfPlayTraining {
    public static Optional<SelfPlayTraining.Statistics> train(Brn2Trainer trainer, SelfPlayBatch batch,
            SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
        return trainSamples(trainer, batch.samples(), config, control, observer);
    }

    public static Optional<SelfPlayTraining.Statistics> trainSamples(Brn2Trainer trainer,
            List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config config, SelfPlayControl control,
            Consumer<SelfPlayTraining.Progress> observer) {
        return trainSamples(trainer, samples, config, control, observer, TrajectorySampler.Sample::target);
    }

    /** Configured targets; callers without a target selector retain terminal WDL. Targets are frozen before updates. */
    public static Optional<SelfPlayTraining.Statistics> trainSamples(Brn2Trainer trainer,
            List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config config, SelfPlayControl control,
            Consumer<SelfPlayTraining.Progress> observer, ToDoubleFunction<TrajectorySampler.Sample> target) {
        if (config.epochs() != 1 || config.minibatchSize() != 1)
            throw new IllegalArgumentException("BRN-2 uses one online pass (epochs=1, minibatch=1).");
        samples = SampleStore.copyOf(samples);
        if (samples.isEmpty() || control.cancelled()) return Optional.empty();
        samples = SampleStore.withTargets(samples, target, control);
        if (control.cancelled()) return Optional.empty();
        var cursor = control.takeTrainingStart();
        long initial = cursor.initialStep() < 0 ? trainer.optimizer().step() : cursor.initialStep();
        if (trainer.optimizer().step() != initial + cursor.updates() || cursor.samples() > samples.size())
            throw new IllegalArgumentException("Invalid training continuation position.");
        Metrics before = cursor.initialStep() < 0 ? metrics(trainer, samples, control) : new Metrics(cursor.initialLoss(), Double.NaN, Double.NaN);
        if (control.cancelled()) return Optional.empty();
        try (var order = new TrainingOrder(samples.size())) {
            if (!order.reset(config.shuffle(), new SplittableRandom(config.shuffleSeed()), control)) return Optional.empty();
            long[] board = new long[Board.MAX_BITBOARDS];
            long trained = cursor.samples(); double loss = cursor.lossSum();
            control.recordTraining(trained, trained, initial, before.loss(), loss);
            try (var prefetched = new PrefetchedSamples(samples, order, (int) trained)) {
                for (int position = (int) trained; position < order.length; position++) {
                    if (control.cancelled()) break;
                    var sample = prefetched.next();
                    sample.copyBoardInto(board);
                    // Bounded value from THIS position's side to move. Search scores are not targets.
                    loss += trainer.train(board, sample.trainingTarget());
                    trained++;
                    control.recordTraining(trained, trained, initial, before.loss(), loss);
                    observer.accept(new SelfPlayTraining.Progress(trained, trained, initial, trainer.optimizer().step(), loss / trained));
                }
            }
            if (trained == 0) return Optional.empty();
            Metrics after = metrics(trainer, samples, control);
            return Optional.of(new SelfPlayTraining.Statistics(trained, trained, initial, trainer.optimizer().step(),
                    before.loss(), after.loss(), loss / trained, after.prediction(), after.target(), control.cancelled()));
        }
    }

    private record Metrics(double loss, double prediction, double target) {}
    private static Metrics metrics(Brn2Trainer trainer, List<TrajectorySampler.Sample> samples,
                                  SelfPlayControl control) {
        long[] board = new long[Board.MAX_BITBOARDS];
        double loss = 0, prediction = 0, target = 0;
        for (int i = 0; i < samples.size(); i++) {
            var sample = samples.get(i);
            if (control.cancelled()) return new Metrics(Double.NaN, Double.NaN, Double.NaN);
            sample.copyBoardInto(board);
            double value = trainer.predict(board), difference = value - sample.trainingTarget();
            loss += 0.5 * difference * difference; prediction += value; target += sample.trainingTarget();
        }
        return new Metrics(loss / samples.size(), prediction / samples.size(), target / samples.size());
    }
    private Brn2SelfPlayTraining() {}
}
