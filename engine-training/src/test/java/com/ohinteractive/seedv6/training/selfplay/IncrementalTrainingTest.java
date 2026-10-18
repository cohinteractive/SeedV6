package com.ohinteractive.seedv6.training.selfplay;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.BrnCaptureConsistency;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class IncrementalTrainingTest {
    static TrajectorySampler.Sample sample(int id) {
        long[] board = Board.startingPosition(); board[Board.KEY] = id;
        return new TrajectorySampler.Sample(board, id % 3 - 1, (id % 11 - 5) / 5.0);
    }
    @Test void firstOptimizerUpdateReleasesProducerBeforeCompletionAndEightWholeEpochsFinish() throws Exception {
        var firstUpdate = new CountDownLatch(1); var produced = new AtomicInteger();
        int count = 4101;
        var state = NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2, 71, .001);
        try (var workspace = new SampleStore.Workspace()) {
            var result = IncrementalTraining.train(state, state.snapshot(), new SelfPlayTraining.Config(8, 128, true, 19),
                    new SelfPlayControl(), sink -> {
                        for (int i = 0; i < count; i++) {
                            sink.accept(sample(i)); produced.incrementAndGet();
                            if (i == 1023) assertTrue(firstUpdate.await(5, TimeUnit.SECONDS), "Producer cannot finish until a real optimizer update occurs");
                        }
                        return count;
                    }, s -> new IncrementalTraining.Prepared(s, null), BrnCaptureConsistency.OFF, p -> {
                        assertEquals(p.optimizerStep(), state.step());
                        if (p.optimizerUpdates() == 1) { assertTrue(produced.get() < count); firstUpdate.countDown(); }
                    }, s -> {});
            assertTrue(result.verified()); assertEquals(count, result.source());
            assertEquals(8L * count, result.training().orElseThrow().samplesTrained());
            assertEquals(8L * ((count + 127) / 128), state.step());
            assertEquals(count, result.samples().size());
            for (int i = 0; i < count; i++) {
                var actual = result.samples().get(i);
                assertEquals(i, actual.board()[Board.KEY]); assertEquals(sample(i).trainingTarget(), actual.trainingTarget());
            }
        }
        noReaders();
    }
    @ParameterizedTest @EnumSource(TrainingArchitecture.class)
    void everyArchitectureKeepsItsOriginalBatchMathematicsAndPartialFinalBatch(TrainingArchitecture architecture) throws Exception {
        boolean batches = architecture.nnueFamily() || architecture.corpusOnly();
        int count = 13, batch = batches ? 4 : 1, epochs = batches ? 3 : 1;
        var reference = NetworkTrainingState.initialized(architecture, 71, .001);
        for (int epoch = 0; epoch < epochs; epoch++) for (int at = 0; at < count; at += batch) {
            int n = Math.min(batch, count - at); long[][] boards = new long[n][]; double[] targets = new double[n];
            for (int i = 0; i < n; i++) { var s = sample(at + i); boards[i] = s.board(); targets[i] = s.trainingTarget(); }
            update(reference, boards, targets, n);
        }
        byte[] expected = reference.encode(); reference = null;
        var actual = NetworkTrainingState.initialized(architecture, 71, .001);
        double before = 0;
        for (int i = 0; i < count; i++) before += IncrementalTraining.loss(actual, sample(i));
        var pinned = actual.snapshot();
        var pinnedBytes = new ByteArrayOutputStream(); pinned.write(pinnedBytes);
        try (var workspace = new SampleStore.Workspace()) {
            var result = IncrementalTraining.train(actual, pinned, new SelfPlayTraining.Config(epochs, batch, false, 1),
                    new SelfPlayControl(), sink -> { for (int i = 0; i < count; i++) sink.accept(sample(i)); return count; },
                    s -> new IncrementalTraining.Prepared(s, null), BrnCaptureConsistency.OFF, p -> {}, s -> {});
            assertEquals((long) count * epochs, result.training().orElseThrow().samplesTrained());
            assertEquals((long) ((count + batch - 1) / batch) * epochs, actual.step());
            assertArrayEquals(expected, actual.encode());
            assertEquals(before / count, result.training().orElseThrow().initialLoss(), 1e-12);
            var after = new ByteArrayOutputStream(); pinned.write(after);
            assertArrayEquals(pinnedBytes.toByteArray(), after.toByteArray(), "Generation actor must remain immutable");
        }
    }
    @Test void captureSupervisionUsesTheSameFrozenTeacherTargetsAndCaptureRng() throws Exception {
        var samples = new ArrayList<TrajectorySampler.Sample>();
        for (int i = 0; i < 17; i++) samples.add(new TrajectorySampler.Sample(Board.fromFen("7k/8/8/3pP3/8/8/8/K7 w - d6 0 1"), i % 3 - 1));
        var config = new SelfPlayTraining.Config(1, 1, false, 71);
        var capture = new BrnCaptureConsistency(2);
        var model = com.ohinteractive.seedv6.core.nnue.NnueNetwork.initialized(17);
        var teacher = new com.ohinteractive.seedv6.core.nnue.NnueEvaluator(model);
        java.util.function.ToDoubleFunction<long[]> value = board -> { teacher.evaluate(board); return teacher.boundedValue(); };
        var reference = new com.ohinteractive.seedv6.core.brn2.Brn2Trainer(.001);
        BrnCaptureTraining.trainSamples(reference, samples, config, new SelfPlayControl(), p -> {}, TrajectorySampler.Sample::target, capture, value, 55);
        var actual = new NetworkTrainingState.Brn2(new com.ohinteractive.seedv6.core.brn2.Brn2Trainer(.001));
        var supervision = com.ohinteractive.seedv6.training.service.BrnSupervision.blended(.5);
        try (var workspace = new SampleStore.Workspace()) {
            IncrementalTraining.train(actual, actual.snapshot(), config, new SelfPlayControl(), sink -> { samples.forEach(sink); return null; },
                    IncrementalTraining.supervision(s -> supervision.target(s.target(), value.applyAsDouble(s.board())), capture, value, 55), capture, p -> {}, s -> {});
            assertArrayEquals(new NetworkTrainingState.Brn2(reference).encode(), actual.encode());
        }
    }
    @Test void replayOrderCoversHundredsOfMillionsOfLogicalSamplesWithoutAPositionIndex() {
        int count = 200_000_003;
        for (int epoch = 1; epoch < 4; epoch++) {
            var order = new ReplayOrder(count, 71, epoch, true);
            var seen = new BitSet(order.blocks()); long positions = 0;
            for (int at = 0; at < order.blocks(); at++) {
                int block = order.block(at); assertFalse(seen.get(block)); seen.set(block); positions += order.length(block);
            }
            assertEquals(order.blocks(), seen.cardinality()); assertEquals(count, positions);
        }
    }
    @Test void cancellationAndProducerFailureReleaseBackpressureAndWorkers() throws Exception {
        for (boolean fail : new boolean[]{false, true}) {
            var state = NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2, 71, .001);
            var control = new SelfPlayControl();
            try (var workspace = new SampleStore.Workspace()) {
                IncrementalTraining.Producer<Void> producer = sink -> {
                    for (int i = 0; i < 100000; i++) {
                        if (fail && i == 2048) throw new IOException("controlled reader failure");
                        sink.accept(sample(i));
                    }
                    return null;
                };
                if (fail) assertTrue(assertThrows(IOException.class, () -> IncrementalTraining.train(state, state.snapshot(),
                        new SelfPlayTraining.Config(2, 128, true, 71), control, producer, s -> new IncrementalTraining.Prepared(s, null),
                        BrnCaptureConsistency.OFF, p -> {}, s -> {})).getMessage().contains("controlled reader failure"));
                else {
                    var result = IncrementalTraining.train(state, state.snapshot(), new SelfPlayTraining.Config(2, 128, true, 71),
                            control, producer, s -> new IncrementalTraining.Prepared(s, null), BrnCaptureConsistency.OFF,
                            p -> { if (p.optimizerUpdates() == 3) control.cancel(); }, s -> {});
                    assertFalse(result.verified()); assertEquals(3, state.step());
                }
            }
            noReaders();
        }
    }
    static void noReaders() { assertEquals(0, Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.isAlive() && t.getName().equals("seedv6-training-acquisition")).count()); }
    static void update(NetworkTrainingState state, long[][] boards, double[] targets, int n) {
        switch (state) {
            case NetworkTrainingState.Nnue s -> s.trainer().trainBatch(boards, targets, n);
            case NetworkTrainingState.NnueMaterial s -> s.trainer().trainBatch(boards, targets, n);
            case NetworkTrainingState.Brn3 s -> s.trainer().trainBatch(boards, targets, n);
            case NetworkTrainingState.BrnPair2 s -> s.trainer().trainBatch(boards, targets, n);
            case NetworkTrainingState.Brn s -> s.trainer().train(boards[0], targets[0]);
            case NetworkTrainingState.Brn1 s -> s.trainer().train(boards[0], targets[0]);
            case NetworkTrainingState.Brn2 s -> s.trainer().train(boards[0], targets[0]);
        }
    }
}
