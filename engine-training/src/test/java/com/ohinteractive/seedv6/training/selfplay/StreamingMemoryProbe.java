package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;

/** Standalone bounded synthetic I/O probe; run with -Xmx32m. No network updates or self-play. */
public final class StreamingMemoryProbe {
    public static void main(String[] args) throws Exception {
        int count = Integer.parseInt(args[0]);
        boolean shuffle = args.length > 1 && Boolean.parseBoolean(args[1]);
        long started = System.nanoTime();
        long peak = 0;
        try (var workspace = new SampleStore.Workspace(); var order = new TrainingOrder(count)) {
            var samples = SampleStore.mutable();
            var sample = new TrajectorySampler.Sample(new long[]{1, 2, 3, 4, 5, 6}, 1, .25);
            for (int i = 0; i < count; i++) {
                samples.add(sample);
                if ((i & 1023) == 0) peak = Math.max(peak, used());
            }
            samples = SampleStore.copyOf(samples);
            long acquired = System.nanoTime();
            var control = new SelfPlayControl(); order.reset(shuffle, new SplittableRandom(71), control);
            peak = Math.max(peak, used());
            long first, checksum = 0;
            try (var reader = new PrefetchedSamples(samples, order, 0)) {
                checksum += reader.next().board()[0]; first = System.nanoTime();
                for (int i = 1; i < count; i++) {
                    checksum += reader.next().board()[0];
                    if ((i & 1023) == 0) peak = Math.max(peak, used());
                }
            }
            long end = System.nanoTime();
            if (checksum != count) throw new AssertionError("Missing positions");
            System.out.printf(Locale.ROOT, "count=%d shuffle=%s heapLimitMiB=%.1f sampledPeakMiB=%.2f acquireMs=%.2f firstReadMs=%.2f readPositionsPerSecond=%.0f totalMs=%.2f%n",
                    count, shuffle, Runtime.getRuntime().maxMemory()/1048576.0, peak/1048576.0, (acquired-started)/1e6,
                    (first-started)/1e6, (count-1)*1e9/(end-first), (end-started)/1e6);
        }
    }
    private static long used() { var runtime = Runtime.getRuntime(); return runtime.totalMemory() - runtime.freeMemory(); }
}
