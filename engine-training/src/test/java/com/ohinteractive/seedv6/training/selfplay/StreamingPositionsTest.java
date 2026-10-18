package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class StreamingPositionsTest {
    private static TrajectorySampler.Sample sample(int id) {
        return new TrajectorySampler.Sample(new long[]{id, 0, 0, 0, 0, 0}, id % 3 - 1, .25);
    }
    @Test void diskOrderMatchesHistoricalShuffleAcrossEpochsAndLargeBoundary() {
        for (int count : new int[]{1, 17, 65536, 65537, 270001}) {
            int[] expected = new int[count]; var reference = new SplittableRandom(719); var random = new SplittableRandom(719);
            try (var order = new TrainingOrder(count)) {
                for (int epoch = 0; epoch < 3; epoch++) {
                    for (int i = 0; i < count; i++) expected[i] = i;
                    for (int i = count - 1; i > 0; i--) {
                        int other = reference.nextInt(i + 1), swap = expected[i]; expected[i] = expected[other]; expected[other] = swap;
                    }
                    assertTrue(order.reset(true, random, new SelfPlayControl()));
                    for (int i = 0; i < count; i++) assertEquals(expected[i], order.get(i));
                }
            }
        }
    }
    @Test void prefetchPreservesOrderTargetsAndResumeOffsetAndClosesBackpressuredWorker() throws Exception {
        try (var workspace = new SampleStore.Workspace(); var order = new TrainingOrder(3001)) {
            var mutable = SampleStore.mutable(); for (int i = 0; i < 3001; i++) mutable.add(sample(i));
            var samples = SampleStore.copyOf(mutable); assertInstanceOf(SampleStore.class, samples);
            assertSame(samples, SampleStore.copyOf(samples));
            assertThrows(UnsupportedOperationException.class, () -> samples.add(sample(0)));
            assertTrue(order.reset(true, new SplittableRandom(71), new SelfPlayControl()));
            try (var reader = new PrefetchedSamples(samples, order, 799)) {
                for (int i = 799; i < samples.size(); i++) {
                    var value = reader.next(); assertEquals(order.get(i), value.board()[0]); assertEquals(.25, value.trainingTarget());
                }
            }
            for (int i = 0; i < 20; i++) {
                try (var reader = new PrefetchedSamples(samples, order, 0)) { reader.next(); }
            }
            assertEquals(0, Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().equals("seedv6-position-prefetch") && t.isAlive()).count());
            workspace.clear();
            assertThrows(java.io.UncheckedIOException.class, () -> samples.get(2000));
        }
    }
    @Test void cancellationFinishesAnAdmittedReadBatchAndOrderPreparationCanCancel() throws Exception {
        try (var workspace = new SampleStore.Workspace(); var order = new TrainingOrder(3001)) {
            var mutable = SampleStore.mutable(); for (int i = 0; i < 3001; i++) mutable.add(sample(i));
            var samples = SampleStore.copyOf(mutable); var control = new SelfPlayControl();
            assertTrue(order.reset(false, new SplittableRandom(1), control));
            try (var reader = new PrefetchedSamples(samples, order, 0)) {
                control.cancel();
                for (int i = 0; i < 2049; i++) assertEquals(i, reader.next().board()[0]);
            }
            assertFalse(order.reset(true, new SplittableRandom(1), control));
        }
    }
    @Test void readerFailureReachesConsumerAndTerminates() throws Exception {
        try (var workspace = new SampleStore.Workspace(); var order = new TrainingOrder(1000)) {
            var mutable = SampleStore.mutable(); for (int i = 0; i < 1000; i++) mutable.add(sample(i));
            var samples = SampleStore.copyOf(mutable); workspace.clear();
            order.reset(false, new SplittableRandom(1), new SelfPlayControl());
            try (var reader = new PrefetchedSamples(samples, order, 0)) {
                assertThrows(IllegalStateException.class, reader::next);
            }
        }
    }
}
