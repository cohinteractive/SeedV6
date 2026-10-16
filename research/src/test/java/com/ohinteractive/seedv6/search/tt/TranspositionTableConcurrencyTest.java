package com.ohinteractive.seedv6.search.tt;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.ProbeOutcome;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.StoreOutcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranspositionTableConcurrencyTest {

    @Test
    void sameKeyWritersCannotPublishCrossWriteMetadata() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            final int writers = 4;
            final int readers = 4;
            final int iterations = 25_000;
            final long key = 0L;
            final TranspositionTable table = new TranspositionTable(1);
            final ExecutorService executor = Executors.newFixedThreadPool(writers + readers);
            final CountDownLatch ready = new CountDownLatch(writers + readers);
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(writers + readers);
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            final AtomicLong hits = new AtomicLong();

            for(int writer = 0; writer < writers; writer ++) {
                final int writerId = writer;
                executor.execute(() -> {
                    ready.countDown();
                    await(start, failure);
                    try {
                        for(int i = 0; i < iterations && failure.get() == null; i ++) {
                            final long move = correlatedMove(writerId, i);
                            final StoreOutcome outcome = table.store(
                                key, 32, Bound.EXACT, scoreFromMove(move), 0, move,
                                Cacheability.POSITION_ONLY
                            );
                            if(outcome != StoreOutcome.STORED) {
                                throw new AssertionError("Same-key equal-depth write was not stored: " + outcome);
                            }
                        }
                    } catch(Throwable thrown) {
                        failure.compareAndSet(null, thrown);
                    } finally {
                        done.countDown();
                    }
                });
            }
            for(int reader = 0; reader < readers; reader ++) {
                executor.execute(() -> {
                    final Probe probe = new Probe();
                    ready.countDown();
                    await(start, failure);
                    try {
                        for(int i = 0; i < iterations * 4 && failure.get() == null; i ++) {
                            final ProbeOutcome outcome = table.probe(
                                key, 32, -TranspositionScores.MATE_SCORE,
                                TranspositionScores.MATE_SCORE, 0, probe
                            );
                            if(outcome == ProbeOutcome.EMPTY) continue;
                            if(outcome != ProbeOutcome.EXACT_HIT) {
                                throw new AssertionError("Unexpected probe outcome: " + outcome);
                            }
                            assertSameKeyCorrelation(probe);
                            hits.incrementAndGet();
                        }
                    } catch(Throwable thrown) {
                        failure.compareAndSet(null, thrown);
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(done.await(12, TimeUnit.SECONDS));
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
            assertNull(failure.get(), () -> "Concurrent failure: " + failure.get());
            assertTrue(hits.get() > 0, "Stress readers observed no published entries.");

            final Probe finalProbe = new Probe();
            assertEquals(
                ProbeOutcome.EXACT_HIT,
                table.probe(
                    key, 32, -TranspositionScores.MATE_SCORE,
                    TranspositionScores.MATE_SCORE, 0, finalProbe
                )
            );
            assertSameKeyCorrelation(finalProbe);
        });
    }

    @Test
    void collidingKeysAndGenerationChangesNeverExposeImpossibleSnapshots() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            final int writers = 4;
            final int readers = 4;
            final int iterations = 10_000;
            final TranspositionTable table = new TranspositionTable(1);
            final ExecutorService executor = Executors.newFixedThreadPool(writers + readers);
            final CountDownLatch ready = new CountDownLatch(writers + readers);
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(writers + readers);
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            final AtomicLongArray publishedKeys = new AtomicLongArray(writers);
            final AtomicLong hits = new AtomicLong();

            for(int writer = 0; writer < writers; writer ++) {
                final int writerId = writer;
                executor.execute(() -> {
                    ready.countDown();
                    await(start, failure);
                    try {
                        for(int i = 0; i < iterations && failure.get() == null; i ++) {
                            final long key = correlatedKey(writerId, i);
                            table.advanceGeneration();
                            final StoreOutcome outcome = table.store(
                                key, depthFromKey(key), Bound.EXACT, scoreFromKey(key), 0,
                                moveFromKey(key), Cacheability.POSITION_ONLY
                            );
                            if(outcome == StoreOutcome.STORED) publishedKeys.set(writerId, key);
                        }
                    } catch(Throwable thrown) {
                        failure.compareAndSet(null, thrown);
                    } finally {
                        done.countDown();
                    }
                });
            }
            for(int reader = 0; reader < readers; reader ++) {
                executor.execute(() -> {
                    final Probe probe = new Probe();
                    ready.countDown();
                    await(start, failure);
                    try {
                        for(int i = 0; i < iterations * 5 && failure.get() == null; i ++) {
                            final long key = publishedKeys.get(i & (writers - 1));
                            if(key == 0L) continue;
                            final ProbeOutcome outcome = table.probe(
                                key, 0, -TranspositionScores.MATE_SCORE,
                                TranspositionScores.MATE_SCORE, 0, probe
                            );
                            if(outcome == ProbeOutcome.EMPTY || outcome == ProbeOutcome.KEY_MISMATCH) {
                                continue;
                            }
                            if(outcome != ProbeOutcome.EXACT_HIT) {
                                throw new AssertionError("Unexpected matching outcome: " + outcome);
                            }
                            if(probe.key() != key
                                || probe.depth() != depthFromKey(key)
                                || probe.score() != scoreFromKey(key)
                                || probe.move() != moveFromKey(key)
                                || probe.bound() != Bound.EXACT
                                || probe.generation() < 0
                                || probe.generation() > 255) {
                                throw new AssertionError("Impossible colliding-entry field combination.");
                            }
                            hits.incrementAndGet();
                        }
                    } catch(Throwable thrown) {
                        failure.compareAndSet(null, thrown);
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(done.await(12, TimeUnit.SECONDS));
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
            assertNull(failure.get(), () -> "Concurrent failure: " + failure.get());
            assertTrue(hits.get() > 0, "Collision stress readers observed no matching entries.");
        });
    }

    private static long correlatedMove(int writer, int iteration) {
        return Long.rotateLeft(
            0x9e37_79b9_7f4a_7c15L ^ ((long) writer << 48) ^ iteration,
            (writer + iteration) & 63
        );
    }

    private static int scoreFromMove(long move) {
        return (int) Long.remainderUnsigned(move ^ (move >>> 32), 40_001L) - 20_000;
    }

    private static void assertSameKeyCorrelation(Probe probe) {
        if(probe.key() != 0L
            || probe.depth() != 32
            || probe.bound() != Bound.EXACT
            || probe.score() != scoreFromMove(probe.move())
            || probe.generation() < 0
            || probe.generation() > 255) {
            throw new AssertionError("Impossible same-key field combination.");
        }
    }

    private static long correlatedKey(int writer, int iteration) {
        final long sequence = ((long) (writer + 1) << 48) | (iteration + 1L);
        return Long.rotateLeft(sequence * 0x9e37_79b9_7f4a_7c15L, 23) | 1L;
    }

    private static int depthFromKey(long key) {
        return 1 + (int) ((key >>> 8) & 63L);
    }

    private static int scoreFromKey(long key) {
        return (int) Long.remainderUnsigned(key ^ (key >>> 32), 40_001L) - 20_000;
    }

    private static long moveFromKey(long key) {
        return Long.rotateLeft(key ^ 0xd1b5_4a32_d192_ed03L, 19);
    }

    private static void await(
        CountDownLatch latch, AtomicReference<Throwable> failure
    ) {
        try {
            latch.await();
        } catch(InterruptedException exception) {
            Thread.currentThread().interrupt();
            failure.compareAndSet(null, exception);
        }
    }
}
