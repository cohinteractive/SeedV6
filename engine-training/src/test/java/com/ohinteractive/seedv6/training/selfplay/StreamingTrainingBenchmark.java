package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;

/** Bounded, warmed BRN-3 comparison. Reuses legal synthetic inputs; no strength claim. */
public final class StreamingTrainingBenchmark {
    private record Measurement(double acquireMs, double firstUpdateMs, double totalMs, double loss) {}
    private static Measurement run(List<TrajectorySampler.Sample> input, boolean streaming) throws Exception {
        var trainer = new Brn3Trainer(71);
        var config = new SelfPlayTraining.Config(1, 128, true, 871);
        long started = System.nanoTime(); long[] first = {0};
        try (var workspace = streaming ? new SampleStore.Workspace() : null) {
            var samples = SampleStore.copyOf(input); long acquired = System.nanoTime();
            var result = Brn3CorpusOptimization.trainSamples(trainer, samples, config, new SelfPlayControl(),
                    p -> { if (first[0] == 0) first[0] = System.nanoTime(); }, TrajectorySampler.Sample::target).orElseThrow();
            return new Measurement((acquired-started)/1e6, (first[0]-started)/1e6, (System.nanoTime()-started)/1e6, result.finalLoss());
        }
    }
    public static void main(String[] args) throws Exception {
        int count = Integer.parseInt(args[0]);
        var input = new ArrayList<TrajectorySampler.Sample>();
        for (int i = 0; i < count; i++) input.add(new TrajectorySampler.Sample(Board.startingPosition(), i % 3 - 1));
        run(input.subList(0, Math.min(2048, count)), false); run(input.subList(0, Math.min(2048, count)), true);
        for (int trial = 1; trial <= 3; trial++) {
            Measurement memory, disk;
            if (trial % 2 == 1) { memory = run(input, false); disk = run(input, true); }
            else { disk = run(input, true); memory = run(input, false); }
            if (Double.doubleToRawLongBits(memory.loss) != Double.doubleToRawLongBits(disk.loss)) throw new AssertionError("Loss differs");
            System.out.printf(Locale.ROOT, "count=%d trial=%d memoryAcquireMs=%.2f diskAcquireMs=%.2f memoryFirstUpdateMs=%.2f diskFirstUpdateMs=%.2f memoryTotalMs=%.2f diskTotalMs=%.2f memoryPositionsPerSecond=%.0f diskPositionsPerSecond=%.0f%n",
                    count, trial, memory.acquireMs, disk.acquireMs, memory.firstUpdateMs, disk.firstUpdateMs,
                    memory.totalMs, disk.totalMs, count*1000/memory.totalMs, count*1000/disk.totalMs);
        }
    }
}
