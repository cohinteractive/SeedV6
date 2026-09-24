package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.training.service.BrnCaptureConsistency;
import com.ohinteractive.seedv6.training.service.BrnSupervision;

/** Enabled-only campaign-consistent capture training. No synthetic child is a Sample or base example. */
public final class BrnCaptureTraining {
    private static final BrnSupervision CAMPAIGN = BrnSupervision.blended(.5);

    /** ordinaryTarget is the untouched OFF path; enabled training requires the validated 50:50 campaign. */
    public static Optional<SelfPlayTraining.Statistics> trainSamples(Brn2Trainer trainer,
            List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config config, SelfPlayControl control,
            Consumer<SelfPlayTraining.Progress> observer, ToDoubleFunction<TrajectorySampler.Sample> ordinaryTarget,
            BrnCaptureConsistency capture, ToDoubleFunction<long[]> teacher, long captureSeed) {
        if (!capture.enabled())
            return Brn2SelfPlayTraining.trainSamples(trainer, samples, config, control, observer, ordinaryTarget);
        if (config.epochs() != 1 || config.minibatchSize() != 1)
            throw new IllegalArgumentException("BRN-2 uses one online pass (epochs=1, minibatch=1).");
        samples = List.copyOf(samples);
        if (samples.isEmpty() || control.cancelled()) return Optional.empty();
        double[] targets = new double[samples.size()];
        Relation[] relations = new Relation[samples.size()];
        var captureRandom = new SplittableRandom(captureSeed);
        // Freeze in original sample order, including on resume, before the independent shuffle stream.
        for (int i = 0; i < targets.length; i++) {
            if (control.cancelled()) return Optional.empty();
            var sample = samples.get(i);
            long[] parent = sample.board();
            double nParent = teacherValue(teacher, parent);
            targets[i] = CAMPAIGN.target(sample.target(), nParent);
            relations[i] = relation(parent, nParent, teacher, captureRandom);
        }
        var cursor = control.takeTrainingStart();
        long initial = cursor.initialStep() < 0 ? trainer.optimizer().step() : cursor.initialStep();
        if (trainer.optimizer().step() != initial + cursor.updates() || cursor.samples() > samples.size())
            throw new IllegalArgumentException("Invalid training continuation position.");
        Metrics before = cursor.initialStep() < 0 ? metrics(trainer, samples, targets, control) : new Metrics(cursor.initialLoss(), Double.NaN, Double.NaN);
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
        long trained = cursor.samples(); double loss = cursor.lossSum();
        control.recordTraining(trained, trained, initial, before.loss(), loss);
        for (int position = (int) trained; position < order.length; position++) {
            int index = order[position];
            if (control.cancelled()) break;
            var sample = samples.get(index);
            sample.copyBoardInto(board);
            // Bounded value from THIS position's side to move. Search scores are not targets.
            var relation = relations[index];
            // Existing progress/cursor telemetry continues to report ordinary BASE loss.
            loss += relation == null ? trainer.train(board, targets[index])
                    : trainer.trainCapture(board, targets[index], relation.child(), relation.targetDelta(), capture.lambda()).base();
            trained++;
            control.recordTraining(trained, trained, initial, before.loss(), loss);
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

    record Relation(long move, long[] child, double targetDelta) {}

    static Relation relation(long[] parent, double nParent, ToDoubleFunction<long[]> teacher, SplittableRandom random) {
        var game = new HeadlessGame(parent, 2);
        if (!game.active()) return null;
        var candidates = new ArrayList<Long>();
        long[] scratch = new long[Board.MAX_BITBOARDS];
        for (long move : game.legalMoves()) {
            // Includes en passant and capturing promotions, excludes quiet promotions.
            boolean capture = ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                    || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) == 0
                        && MoveOrdering.isTactical(parent, move);
            if (!capture) continue;
            play(parent, move, scratch);
            if (new HeadlessGame(scratch, 2).active()) candidates.add(move);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparing(Move::coordinate));
        long move = candidates.get(random.nextInt(candidates.size()));
        long[] child = new long[Board.MAX_BITBOARDS];
        play(parent, move, child);
        double nChild = teacherValue(teacher, child);
        return new Relation(move, child, .5 * (-nChild - nParent));
    }
    private static void play(long[] board, long move, long[] into) {
        Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY], move, into);
    }
    private static double teacherValue(ToDoubleFunction<long[]> teacher, long[] board) {
        double value = teacher.applyAsDouble(board);
        if (!Double.isFinite(value) || Math.abs(value) > 1) throw new IllegalArgumentException("Invalid bounded NNUE teacher value.");
        return value;
    }
    private BrnCaptureTraining() {}
}
