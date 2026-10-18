package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.core.move.MoveTactics;
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
        samples = SampleStore.copyOf(samples);
        if (samples.isEmpty() || control.cancelled()) return Optional.empty();
        try (var relations = new CaptureRecords()) {
            var frozen = SampleStore.mutable();
            var captureRandom = new SplittableRandom(captureSeed);
            // Preserve teacher evaluation and capture RNG order before any optimizer update.
            for (var sample : samples) {
                if (control.cancelled()) return Optional.empty();
                long[] parent = sample.board();
                double nParent = teacherValue(teacher, parent);
                frozen.add(new TrajectorySampler.Sample(parent, sample.target(), CAMPAIGN.target(sample.target(), nParent)));
                relations.add(relation(parent, nParent, teacher, captureRandom));
            }
            relations.seal();
            samples = SampleStore.copyOf(frozen);
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
                        int index = order.get(position);
                        if (control.cancelled()) break;
                        var sample = prefetched.next();
                        sample.copyBoardInto(board);
                        // Bounded value from THIS position's side to move. Search scores are not targets.
                        var relation = relations.get(index);
                        // Existing progress/cursor telemetry continues to report ordinary BASE loss.
                        loss += relation == null ? trainer.train(board, sample.trainingTarget())
                                : trainer.trainCapture(board, sample.trainingTarget(), relation.child(), relation.targetDelta(), capture.lambda()).base();
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

    /** Fixed-width frozen capture relations; no dataset-sized target/child arrays. */
    private static final class CaptureRecords implements AutoCloseable {
        private final java.nio.file.Path path;
        private final java.io.RandomAccessFile file;
        private final java.io.DataOutputStream out;
        private boolean sealed;
        CaptureRecords() {
            try {
                path = java.nio.file.Files.createTempFile("seedv6-training-", ".captures");
                try {
                    var opened = new java.io.RandomAccessFile(path.toFile(), "rw");
                    try {
                        out = new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(opened.getFD()), 65536));
                        file = opened;
                    } catch (Throwable e) {
                        try { opened.close(); } catch (Throwable cleanup) { e.addSuppressed(cleanup); }
                        throw e;
                    }
                } catch (Throwable e) {
                    try { java.nio.file.Files.deleteIfExists(path); } catch (Throwable cleanup) { e.addSuppressed(cleanup); }
                    throw e;
                }
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        void add(Relation relation) {
            try {
                out.writeBoolean(relation != null);
                out.writeLong(relation == null ? 0 : relation.move());
                for (int i = 0; i < 6; i++) out.writeLong(relation == null ? 0 : relation.child()[i]);
                out.writeDouble(relation == null ? 0 : relation.targetDelta());
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        void seal() {
            try { out.flush(); sealed = true; } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        Relation get(int index) {
            if (!sealed) throw new IllegalStateException("Unfrozen capture records");
            try {
                file.seek((long) index * 65);
                if (!file.readBoolean()) return null;
                long move = file.readLong(); long[] child = new long[6];
                for (int i = 0; i < child.length; i++) child[i] = file.readLong();
                return new Relation(move, child, file.readDouble());
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        public void close() {
            try { try { out.close(); } finally { try { file.close(); } finally { java.nio.file.Files.deleteIfExists(path); } } }
            catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
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
                        && MoveTactics.isTactical(parent, move);
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
