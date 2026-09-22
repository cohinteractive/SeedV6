package com.ohinteractive.seedv6.training.selfplay;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn1.Brn1Trainer;

/** BRN-1 online updates: one deterministic shuffled pass over the existing terminal W/D/L samples. */
public final class Brn1SelfPlayTraining {
    public static Optional<SelfPlayTraining.Statistics> train(Brn1Trainer trainer, SelfPlayBatch batch,
            SelfPlayTraining.Config config, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
        return trainSamples(trainer, batch.samples(), config, control, observer);
    }

    public static Optional<SelfPlayTraining.Statistics> trainSamples(Brn1Trainer trainer,
            List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config config, SelfPlayControl control,
            Consumer<SelfPlayTraining.Progress> observer) {
        if (config.epochs() != 1 || config.minibatchSize() != 1)
            throw new IllegalArgumentException("BRN-1 uses one online pass (epochs=1, minibatch=1).");
        samples = List.copyOf(samples);
        if (samples.isEmpty() || control.cancelled()) return Optional.empty();
        long initial = trainer.optimizer().step();
        Metrics before = metrics(trainer, samples, control);
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
            // Already exact [-1,+1], from THIS position's side to move. Search scores are not targets.
            loss += trainer.train(board, sample.target());
            trained++;
            observer.accept(new SelfPlayTraining.Progress(trained, trained, initial, trainer.optimizer().step(), loss / trained));
        }
        if (trained == 0) return Optional.empty();
        Metrics after = metrics(trainer, samples, control);
        return Optional.of(new SelfPlayTraining.Statistics(trained, trained, initial, trainer.optimizer().step(),
                before.loss(), after.loss(), loss / trained, after.prediction(), after.target(), control.cancelled()));
    }

    private record Metrics(double loss, double prediction, double target) {}
    private static Metrics metrics(Brn1Trainer trainer, List<TrajectorySampler.Sample> samples, SelfPlayControl control) {
        long[] board = new long[Board.MAX_BITBOARDS];
        double loss = 0, prediction = 0, target = 0;
        for (var sample : samples) {
            if (control.cancelled()) return new Metrics(Double.NaN, Double.NaN, Double.NaN);
            sample.copyBoardInto(board);
            double value = trainer.predict(board), difference = value - sample.target();
            loss += 0.5 * difference * difference; prediction += value; target += sample.target();
        }
        return new Metrics(loss / samples.size(), prediction / samples.size(), target / samples.size());
    }
    private Brn1SelfPlayTraining() {}
}
