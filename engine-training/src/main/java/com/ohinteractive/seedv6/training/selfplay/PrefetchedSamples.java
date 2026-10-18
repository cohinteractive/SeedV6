package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;
import java.util.concurrent.*;

/** Two bounded blocks ahead of the consumer. The owner closes at optimizer boundaries before
 * resetting the order; an admitted minibatch can finish reading after cancellation. */
final class PrefetchedSamples implements AutoCloseable {
    private static final int BLOCK = 256;
    private record Block(TrajectorySampler.Sample[] samples, Throwable failure) {}
    private final ArrayBlockingQueue<Block> ready = new ArrayBlockingQueue<>(2);
    private final Thread worker;
    private final List<TrajectorySampler.Sample> direct;
    private final TrainingOrder order;
    private int directIndex;
    private volatile boolean closed;
    private TrajectorySampler.Sample[] current = new TrajectorySampler.Sample[0];
    private int cursor;

    PrefetchedSamples(List<TrajectorySampler.Sample> samples, TrainingOrder order, int start) {
        this.order = order; directIndex = start;
        direct = start < order.length && samples instanceof SampleStore && samples.size() > BLOCK ? null : samples;
        if (direct != null) { worker = null; return; }
        worker = new Thread(() -> {
            try (var reader = samples instanceof SampleStore store ? store.reader() : null) {
                for (int at = start; at < order.length && !closed;) {
                    var block = new TrajectorySampler.Sample[Math.min(BLOCK, order.length - at)];
                    for (int i = 0; i < block.length; i++) {
                        if (closed) return;
                        int index = order.get(at++);
                        block[i] = reader == null ? samples.get(index) : reader.get(index);
                    }
                    if (!publish(new Block(block, null))) return;
                }
            } catch (Throwable failure) {
                if (!closed) try { publish(new Block(null, failure)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        }, "seedv6-position-prefetch");
        worker.start();
    }
    private boolean publish(Block block) throws InterruptedException {
        while (!closed) if (ready.offer(block, 50, TimeUnit.MILLISECONDS)) return true;
        return false;
    }
    TrajectorySampler.Sample next() {
        if (direct != null) return direct.get(order.get(directIndex++));
        if (cursor < current.length) return current[cursor++];
        try {
            for (;;) {
                Block block = ready.poll(50, TimeUnit.MILLISECONDS);
                if (block != null) {
                    if (block.failure != null) {
                        if (block.failure instanceof RuntimeException e) throw e;
                        if (block.failure instanceof Error e) throw e;
                        throw new IllegalStateException("Training position reader failed: " + block.failure, block.failure);
                    }
                    current = block.samples; cursor = 0; return current[cursor++];
                }
                if (closed) throw new CancellationException("Training position reading cancelled");
                if (!worker.isAlive()) throw new IllegalStateException("Training position reader ended early");
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted reading training positions", e); }
    }
    public void close() {
        closed = true; if (worker == null) return; worker.interrupt();
        boolean interrupted = false;
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (worker.isAlive()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new IllegalStateException("Training position reader did not terminate");
                try { worker.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining))); }
                catch (InterruptedException e) { interrupted = true; }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); ready.clear(); current = new TrajectorySampler.Sample[0]; }
    }
}
