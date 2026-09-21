package com.ohinteractive.seedv6.training.telemetry;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import org.junit.jupiter.api.Test;

/** Isolates copying/publication over prebuilt positions; never claims whole-training throughput. */
class ActiveGamePublicationBenchmarkTest {
    private static volatile ActiveGameSnapshot consumed;
    @Test void measureWarmedSnapshotPublicationAndAllocation() {
        var feed = new ActiveGameFeed(); feed.selfPlay(1, "actor");
        var game = new HeadlessGame(Board.startingPosition(), 100);
        long move = game.legalMoves()[0]; feed.start(game, 1, 0); game.play(move);
        var result = new SearchResult(move, true, 100, 4, 1000, 20, true);
        int iterations = 100_000;
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        boolean allocations = bean.isThreadAllocatedMemorySupported();
        if (allocations) bean.setThreadAllocatedMemoryEnabled(true);
        long id = Thread.currentThread().threadId();
        double[] on = new double[7], off = new double[7], bytes = new double[7];
        for (int pass = -3; pass < 7; pass++) {
            // Both paths consume the volatile latest value; only 'on' creates and publishes.
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) consumed = feed.latest();
            long baseline = System.nanoTime() - start;
            long allocated = allocations ? bean.getThreadAllocatedBytes(id) : 0;
            start = System.nanoTime();
            for (int i = 0; i < iterations; i++) { feed.moved(game, move, result); consumed = feed.latest(); }
            long duration = System.nanoTime() - start;
            if (pass >= 0) {
                off[pass] = (double) baseline / iterations; on[pass] = (double) duration / iterations;
                bytes[pass] = allocations ? (double) (bean.getThreadAllocatedBytes(id) - allocated) / iterations : -1;
            }
        }
        Arrays.sort(on); Arrays.sort(off); Arrays.sort(bytes);
        System.out.printf(java.util.Locale.ROOT,
                "PHASE4_PUBLICATION iterations=%d rounds=7 boardPayloadBytes=48 offMedianNs=%.1f onMedianNs=%.1f onRangeNs=%.1f..%.1f deltaNs=%.1f allocatedBytesPerMove=%.1f%n",
                iterations, off[3], on[3], on[0], on[6], on[3] - off[3], bytes[3]);
    }
}
