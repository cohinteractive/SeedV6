package com.ohinteractive.seedv6.training.selfplay;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.BrnCaptureConsistency;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;

/** GUI generation worker: bounded acquisition/mixing, one complete pass per epoch, unchanged updates.
 * The caller owns a SampleStore.Workspace until this operation and checkpoint publication finish.
 * Updates are provisional until the producer has verified the complete selected generation. */
public final class IncrementalTraining {
    public static final int MIXING = 2048;
    @FunctionalInterface public interface Producer<R> { R acquire(Consumer<Sample> samples) throws Exception; }
    public record Status(long acquired, int epoch, boolean verified, boolean measuring) {
        Status(long acquired, int epoch, boolean verified) { this(acquired, epoch, verified, false); }
    }
    public record Result<R>(R source, List<Sample> samples, Optional<SelfPlayTraining.Statistics> training, boolean verified) {}
    public record Prepared(Sample sample, BrnCaptureTraining.Relation capture) {}

    /** Called in acquisition order, before mixing. Teacher and capture RNG never see mutable student weights. */
    public static Function<Sample, Prepared> supervision(ToDoubleFunction<Sample> target,
            BrnCaptureConsistency capture, ToDoubleFunction<long[]> teacher, long seed) {
        var random = new SplittableRandom(seed);
        return sample -> {
            double value = target.applyAsDouble(sample);
            var relation = capture.enabled() ? BrnCaptureTraining.relation(sample.board(),
                    teacher.applyAsDouble(sample.board()), teacher, random) : null;
            return new Prepared(new Sample(sample.board(), sample.target(), value), relation);
        };
    }

    public static <R> Result<R> train(NetworkTrainingState state, NetworkModel initial,
            SelfPlayTraining.Config config, SelfPlayControl control, Producer<R> producer,
            Function<Sample, Prepared> preparation, BrnCaptureConsistency capture,
            Consumer<SelfPlayTraining.Progress> progress, Consumer<Status> status) throws IOException {
        if (!SampleStore.streaming()) throw new IllegalStateException("Incremental training requires an owned disk workspace");
        boolean batched = state.architecture().nnueFamily() || state.architecture().corpusOnly();
        if (!batched && (config.epochs() != 1 || config.minibatchSize() != 1))
            throw new IllegalArgumentException("This architecture requires one online pass");
        if (capture.enabled() && !(state instanceof NetworkTrainingState.Brn2)) throw new IllegalArgumentException("Capture requires BRN-2");
        var cursor = control.takeTrainingStart();
        long firstStep = cursor.initialStep() < 0 ? state.step() : cursor.initialStep();
        if (state.step() != firstStep + cursor.updates()) throw new IllegalArgumentException("Optimizer continuation mismatch");
        var samples = SampleStore.mutable();
        var update = new Updates(state, config.minibatchSize(), cursor, firstStep, capture, progress, control);
        ToDoubleFunction<Sample> initialLoss = frozenLoss(initial);
        double[] initialSum = {0};
        boolean verified = false;
        R source = null;
        status.accept(new Status(0, 1, false));
        final var acquiring = samples;
        try (var input = new Acquisition<R>(producer, preparation, control,
                () -> status.accept(new Status(acquiring.size(), 1, false)))) {
            var mixing = new Prepared[config.shuffle() ? (int) Math.min(MIXING, Math.max(128L, 4L * config.minibatchSize())) : 1];
            var random = new SplittableRandom(config.shuffleSeed());
            int size = 0;
            while (size < mixing.length && !control.cancelled()) {
                Prepared sample = input.next(); if (sample == null) break;
                samples.add(sample.sample()); initialSum[0] += initialLoss.applyAsDouble(sample.sample());
                mixing[size++] = sample;
                if ((size & 127) == 0) status.accept(new Status(samples.size(), 1, false));
            }
            while (size > 0 && !control.cancelled()) {
                int chosen = config.shuffle() ? random.nextInt(size) : 0;
                Prepared sample = mixing[chosen];
                // Consume available samples before waiting for their replacement at the next iteration.
                update.add(sample);
                if (((update.visited + update.count) & 127) == 0) status.accept(new Status(samples.size(), 1, false));
                Prepared next = input.next();
                if (next == null) { mixing[chosen] = mixing[--size]; mixing[size] = null; }
                else {
                    samples.add(next.sample()); initialSum[0] += initialLoss.applyAsDouble(next.sample());
                    mixing[chosen] = next;
                }
            }
            if (!control.cancelled()) {
                source = input.result();
                update.flush();
                verified = true;
                samples = SampleStore.copyOf(samples);
                update.initialLoss = initialSum[0] / samples.size();
                update.record();
                status.accept(new Status(samples.size(), 1, true));
            }
        } catch (AcquisitionCancelled cancelled) {
            if (!control.cancelled()) throw cancelled;
        }
        if (!verified) return new Result<>(source, SampleStore.copyOf(samples), Optional.empty(), false);
        if (cursor.samples() > (long) samples.size() * config.epochs()) throw new IOException("Continuation exceeds the selected dataset");
        for (int epoch = 1; epoch < config.epochs() && !control.cancelled(); epoch++) {
            status.accept(new Status(samples.size(), epoch + 1, true));
            final var retained = samples;
            var order = new ReplayOrder(samples.size(), config.shuffleSeed(), epoch, config.shuffle());
            try (var replay = new Acquisition<Void>(sink -> {
                var block = new Sample[ReplayOrder.BLOCK];
                for (int at = 0; at < order.blocks(); at++) {
                    int index = order.block(at), length = order.length(index);
                    for (int i = 0; i < length; i++) block[i] = retained.get(index * ReplayOrder.BLOCK + i);
                    order.mix(block, length, index);
                    for (int i = 0; i < length; i++) { sink.accept(block[i]); block[i] = null; }
                }
                return null;
            }, sample -> new Prepared(sample, null), control)) {
                for (Prepared sample; !control.cancelled() && (sample = replay.next()) != null;) {
                    update.add(sample);
                    if (((update.visited + update.count) & 127) == 0) status.accept(new Status(samples.size(), epoch + 1, true));
                }
                if (!control.cancelled()) update.flush();
            } catch (AcquisitionCancelled cancelled) { if (!control.cancelled()) throw cancelled; }
        }
        if (update.trained == 0) return new Result<>(source, samples, Optional.empty(), true);
        if (!control.cancelled()) status.accept(new Status(samples.size(), config.epochs(), true, true));
        double loss = 0, prediction = 0, target = 0;
        for (var sample : samples) {
            if (control.cancelled()) { loss = prediction = target = Double.NaN; break; }
            double value = predict(state, sample.board());
            loss += loss(state, sample); prediction += value; target += sample.trainingTarget();
        }
        var statistics = new SelfPlayTraining.Statistics(update.trained, state.step() - firstStep, firstStep,
                state.step(), update.initialLoss, loss / samples.size(), update.lossSum / update.trained,
                prediction / samples.size(), target / samples.size(), control.cancelled());
        return new Result<>(source, samples, Optional.of(statistics), true);
    }

    static ToDoubleFunction<Sample> frozenLoss(NetworkModel initial) {
        if (initial instanceof NetworkModel.NnueMaterial n && n.calibratedOutcome()) {
            var score = HeldOutLoss.predictor(new NetworkModel.NnueMaterial(n.network(), false));
            return sample -> com.ohinteractive.seedv6.core.brn3.Brn3Objective.crossEntropy(score.applyAsDouble(sample.board()) * 325.11,
                    com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.material(sample.board()), sample.trainingTarget())[0];
        }
        var predictor = HeldOutLoss.predictor(initial);
        return sample -> { double difference = predictor.applyAsDouble(sample.board()) - sample.trainingTarget(); return .5 * difference * difference; };
    }
    static double predict(NetworkTrainingState state, long[] board) {
        return switch (state) {
            case NetworkTrainingState.Nnue n -> n.trainer().predict(board);
            case NetworkTrainingState.NnueMaterial n -> n.trainer().predict(board);
            case NetworkTrainingState.Brn n -> n.trainer().predict(board);
            case NetworkTrainingState.Brn1 n -> n.trainer().predict(board);
            case NetworkTrainingState.Brn2 n -> n.trainer().predict(board);
            case NetworkTrainingState.Brn3 n -> n.trainer().predictOutcome(board);
            case NetworkTrainingState.BrnPair2 n -> n.trainer().predictOutcome(board);
            case NetworkTrainingState.BrnNext n -> n.trainer().predictOutcome(board);
        };
    }
    static double loss(NetworkTrainingState state, Sample sample) {
        if (state instanceof NetworkTrainingState.Nnue n) return n.trainer().loss(sample.board(), sample.trainingTarget());
        if (state instanceof NetworkTrainingState.NnueMaterial n) return n.trainer().loss(sample.board(), sample.trainingTarget());
        double difference = predict(state, sample.board()) - sample.trainingTarget(); return .5 * difference * difference;
    }
    private static final class Updates {
        final NetworkTrainingState state;
        final long[][] boards;
        final double[] targets;
        final SelfPlayControl.TrainingCursor resume;
        final SelfPlayControl control;
        final Consumer<SelfPlayTraining.Progress> progress;
        final long firstStep;
        final BrnCaptureConsistency capture;
        BrnCaptureTraining.Relation relation;
        int count;
        long visited, trained;
        double lossSum, initialLoss;
        Updates(NetworkTrainingState state, int size, SelfPlayControl.TrainingCursor cursor, long step,
                BrnCaptureConsistency capture, Consumer<SelfPlayTraining.Progress> progress, SelfPlayControl control) {
            this.state = state; boards = new long[size][Board.MAX_BITBOARDS]; targets = new double[size];
            resume = cursor; firstStep = step; this.capture = capture; this.progress = progress; this.control = control;
            trained = cursor.samples(); lossSum = cursor.lossSum(); initialLoss = cursor.initialLoss();
        }
        void add(Prepared prepared) {
            prepared.sample().copyBoardInto(boards[count]); targets[count++] = prepared.sample().trainingTarget(); relation = prepared.capture();
            if (count == boards.length) flush();
        }
        void flush() {
            if (count == 0) return;
            if (visited < resume.samples()) {
                if (visited + count > resume.samples()) throw new IllegalArgumentException("Continuation is inside an optimizer batch");
            } else {
                double mean = switch (state) {
                    case NetworkTrainingState.Nnue n -> n.trainer().trainBatch(boards, targets, count).meanLoss();
                    case NetworkTrainingState.NnueMaterial n -> n.trainer().trainBatch(boards, targets, count).meanLoss();
                    case NetworkTrainingState.Brn3 n -> n.trainer().trainBatch(boards, targets, count);
                    case NetworkTrainingState.BrnPair2 n -> n.trainer().trainBatch(boards, targets, count);
                    case NetworkTrainingState.BrnNext n -> n.trainer().trainBatch(boards, targets, count);
                    case NetworkTrainingState.Brn n -> n.trainer().train(boards[0], targets[0]);
                    case NetworkTrainingState.Brn1 n -> n.trainer().train(boards[0], targets[0]);
                    case NetworkTrainingState.Brn2 n -> relation == null ? n.trainer().train(boards[0], targets[0])
                            : n.trainer().trainCapture(boards[0], targets[0], relation.child(), relation.targetDelta(), capture.lambda()).base();
                };
                trained += count; lossSum += mean * count; record();
                progress.accept(new SelfPlayTraining.Progress(trained, state.step() - firstStep, firstStep, state.step(), lossSum / trained));
            }
            visited += count; count = 0;
        }
        void record() { control.recordTraining(trained, state.step() - firstStep, firstStep, initialLoss, lossSum); }
    }

    private static final class AcquisitionCancelled extends RuntimeException {}
    /** Two 128-record blocks, plus producer/consumer blocks; no unbounded future or work queue. */
    private static final class Acquisition<R> implements AutoCloseable {
        private record Block(Prepared[] samples, Throwable failure, boolean end) {}
        private final ArrayBlockingQueue<Block> queue = new ArrayBlockingQueue<>(2);
        private final Thread worker;
        private final SelfPlayControl control;
        private final Runnable waiting;
        private long lastWaitNotice;
        private volatile boolean closed;
        private R result;
        private Prepared[] current = new Prepared[0];
        private int index;
        private boolean ended;
        Acquisition(Producer<R> producer, Function<Sample, Prepared> prepare, SelfPlayControl control) {
            this(producer, prepare, control, () -> {});
        }
        Acquisition(Producer<R> producer, Function<Sample, Prepared> prepare, SelfPlayControl control, Runnable waiting) {
            this.control = control;
            this.waiting = waiting;
            worker = new Thread(() -> {
                try {
                    Prepared[][] block = {new Prepared[128]}; int[] used = {0};
                    result = producer.acquire(sample -> {
                        if (closed || control.cancelled()) throw new AcquisitionCancelled();
                        block[0][used[0]++] = prepare.apply(sample);
                        if (used[0] == block[0].length) { publish(new Block(block[0], null, false)); block[0] = new Prepared[128]; used[0] = 0; }
                    });
                    if (used[0] != 0) publish(new Block(Arrays.copyOf(block[0], used[0]), null, false));
                    publish(new Block(null, null, true));
                } catch (Throwable failure) {
                    if (!closed) try { publish(new Block(null, failure, true)); } catch (AcquisitionCancelled cancelled) { /* owner closed */ }
                }
            }, "seedv6-training-acquisition");
            worker.start();
        }
        void publish(Block value) {
            try { while (!closed) { if (queue.offer(value, 50, TimeUnit.MILLISECONDS)) return; } }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            throw new AcquisitionCancelled();
        }
        Prepared next() throws IOException {
            if (closed || control.cancelled()) throw new AcquisitionCancelled();
            if (index < current.length) { var result = current[index]; current[index++] = null; return result; }
            if (ended) return null;
            try {
                for (;;) {
                    if (closed || control.cancelled()) throw new AcquisitionCancelled();
                    var block = queue.poll(50, TimeUnit.MILLISECONDS);
                    if (block == null) {
                        if (!worker.isAlive() && queue.isEmpty()) throw new IOException("Acquisition worker ended without a result");
                        long now = System.nanoTime();
                        if (now - lastWaitNotice >= TimeUnit.MILLISECONDS.toNanos(250)) { lastWaitNotice = now; waiting.run(); }
                        continue;
                    }
                    if (block.failure != null) {
                        if (block.failure instanceof IOException e) throw e;
                        if (block.failure instanceof RuntimeException e) throw e;
                        if (block.failure instanceof Error e) throw e;
                        throw new IOException("Training acquisition failed", block.failure);
                    }
                    if (block.end) { ended = true; return null; }
                    current = block.samples; index = 0; return next();
                }
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("Interrupted acquiring training positions", interrupted); }
        }
        R result() { if (!ended) throw new IllegalStateException("Unverified acquisition"); return result; }
        public void close() {
            if (!ended && worker.isAlive()) control.cancel();
            closed = true; worker.interrupt(); boolean interrupted = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            try {
                while (worker.isAlive()) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) throw new IllegalStateException("Acquisition worker did not terminate");
                    try { worker.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining))); }
                    catch (InterruptedException e) { interrupted = true; }
                }
            } finally { queue.clear(); current = new Prepared[0]; if (interrupted) Thread.currentThread().interrupt(); }
        }
    }
    private IncrementalTraining() {}
}
