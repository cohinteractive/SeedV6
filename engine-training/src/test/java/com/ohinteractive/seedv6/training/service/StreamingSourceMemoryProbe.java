package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.selfplay.SampleStore;
import com.ohinteractive.seedv6.training.validation.*;

/** Disposable real JSONL decoding/acquisition probe. Run with -Xmx64m; no network or search work. */
public final class StreamingSourceMemoryProbe {
    public static void main(String[] args) throws Exception {
        int count = Integer.parseInt(args[0]), held = Math.max(2, (count + 3) / 4);
        Path directory = Files.createTempDirectory("seedv6-streaming-source-probe-");
        try {
            Path input = directory.resolve("source.jsonl"), root = directory.resolve("lineage");
            try (var out = Files.newBufferedWriter(input)) {
                String line = "{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":100}]}]}\n";
                for (int i = 0; i < count + held; i++) out.write(line);
            }
            try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
                new DataSources(1, List.of(DataSource.register("Probe", input, 1)), false).save(DataSources.directory(root));
            }
            var config = new TrainerConfig(root, 71,
                    new TrainerConfig.SelfPlay(1, 1, 2, 0, 0, 2, 4, NnueScoreMapping.V1),
                    new TrainerConfig.Training(1, 1, true),
                    new TrainerConfig.Validation(2, 0, 0, 1, 1, 4, NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                    1, TrainerConfig.DepthChange.REQUIRE_SAME, TrainerConfig.STANDARD_START, TrainingArchitecture.BRN2, .001)
                    .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                    .withCorpusTraining(new CorpusTrainingConfig(count)).withValidationMethod(ValidationMethod.HELD_OUT);
            long started = System.nanoTime(); long[] first = {0}, peak = {0}; int[] seen = {0};
            try (var workspace = new SampleStore.Workspace(); var data = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) {
                var batch = data.batch(1, sample -> {
                    if (first[0] == 0) first[0] = System.nanoTime();
                    if ((++seen[0] & 1023) == 0) { var runtime = Runtime.getRuntime(); peak[0] = Math.max(peak[0], runtime.totalMemory() - runtime.freeMemory()); }
                });
                if (!(batch.training().samples() instanceof SampleStore) || !(batch.validation().samples() instanceof SampleStore)
                        || seen[0] != count || batch.validation().samples().size() != held || data.metrics().decodedRecords() != count + held)
                    throw new AssertionError("Incorrect bounded acquisition");
                System.out.printf(Locale.ROOT, "count=%d held=%d decoded=%d heapLimitMiB=%.1f sampledPeakMiB=%.2f firstSampleMs=%.2f acquireMs=%.2f%n",
                        count, held, data.metrics().decodedRecords(), Runtime.getRuntime().maxMemory()/1048576.0,
                        peak[0]/1048576.0, (first[0]-started)/1e6, (System.nanoTime()-started)/1e6);
            }
        } finally {
            try (var paths = Files.walk(directory)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); }
        }
    }
}
