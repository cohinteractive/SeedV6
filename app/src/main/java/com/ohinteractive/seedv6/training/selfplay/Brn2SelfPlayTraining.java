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

    /** Explicit experimental targets; the normal path remains terminal WDL. Targets are frozen before updates. */
    public static Optional<SelfPlayTraining.Statistics> trainSamples(Brn2Trainer trainer,
            List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config config, SelfPlayControl control,
            Consumer<SelfPlayTraining.Progress> observer, ToDoubleFunction<TrajectorySampler.Sample> target) {
        if (config.epochs() != 1 || config.minibatchSize() != 1)
            throw new IllegalArgumentException("BRN-2 uses one online pass (epochs=1, minibatch=1).");
        samples = List.copyOf(samples);
        if (samples.isEmpty() || control.cancelled()) return Optional.empty();
        double[] targets = new double[samples.size()];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = target.applyAsDouble(samples.get(i));
            if (!Double.isFinite(targets[i]) || Math.abs(targets[i]) > 1)
                throw new IllegalArgumentException("Target must be finite and in [-1,+1].");
        }
        long initial = trainer.optimizer().step();
        Metrics before = metrics(trainer, samples, targets, control);
        if (control.cancelled()) return Optional.empty();
        int[] order = new int[samples.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        if (config.shuffle()) {
            var random = new SplittableRandom(config.shuffleSeed());
            for (int i = order.length - 1; i > 0; i--) {
                int other = random.nextInt(i + 1);
                int swap = order[i]; order[i] = order[other]; order[other] = swap;
            }
        }
        long[] board = new long[Board.MAX_BITBOARDS];
        long trained = 0; double loss = 0;
        for (int index : order) {
            if (control.cancelled()) break;
            var sample = samples.get(index);
            sample.copyBoardInto(board);
            // Bounded value from THIS position's side to move. Search scores are not targets.
            loss += trainer.train(board, targets[index]);
            trained++;
            observer.accept(new SelfPlayTraining.Progress(trained, trained, initial, trainer.optimizer().step(), loss / trained));
        }
        if (trained == 0) return Optional.empty();
        Metrics after = metrics(trainer, samples, targets, control);
        return Optional.of(new SelfPlayTraining.Statistics(trained, trained, initial, trainer.optimizer().step(),
                before.loss(), after.loss(), loss / trained, after.prediction(), after.target(), control.cancelled()));
    }

    private record Metrics(double loss, double prediction, double target) {}
    private static Metrics metrics(Brn2Trainer trainer, List<TrajectorySampler.Sample> samples, double[] targets,
                                  SelfPlayControl control) {
        long[] board = new long[Board.MAX_BITBOARDS];
        double loss = 0, prediction = 0, target = 0;
        for (int i = 0; i < samples.size(); i++) {
            var sample = samples.get(i);
            if (control.cancelled()) return new Metrics(Double.NaN, Double.NaN, Double.NaN);
            sample.copyBoardInto(board);
            double value = trainer.predict(board), difference = value - targets[i];
            loss += 0.5 * difference * difference; prediction += value; target += targets[i];
        }
        return new Metrics(loss / samples.size(), prediction / samples.size(), target / samples.size());
    }
    private Brn2SelfPlayTraining() {}
}
