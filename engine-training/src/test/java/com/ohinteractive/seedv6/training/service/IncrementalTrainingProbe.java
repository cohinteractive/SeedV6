package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;

/** Standalone real decoding + optimizer + replay probe; excludes fixture creation and validation matches. */
public final class IncrementalTrainingProbe {
    public static void main(String[] args) throws Exception {
        int count = Integer.parseInt(args[0]), held = Math.max(2, (count + 3) / 4);
        Path directory = Files.createTempDirectory("seedv6-incremental-probe-");
        try {
            Path input = directory.resolve("source.jsonl"), root = directory.resolve("lineage");
            try (var out = Files.newBufferedWriter(input)) {
                String line = "{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":100}]}]}\n";
                for (int i = 0; i < count + held; i++) out.write(line);
            }
            try (var store = new CheckpointStore(root, TrainingArchitecture.BRN_PAIR2)) {
                new DataSources(1, List.of(DataSource.register("Probe", input, 1)), false).save(DataSources.directory(root));
            }
            var config = new TrainerConfig(root, 71,
                    new TrainerConfig.SelfPlay(1, 1, 2, 0, 0, 2, 4, NnueScoreMapping.V1), new TrainerConfig.Training(2, 128, true),
                    new TrainerConfig.Validation(2, 0, 0, 1, 1, 4, NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                    1, TrainerConfig.DepthChange.REQUIRE_SAME, TrainerConfig.STANDARD_START, TrainingArchitecture.BRN_PAIR2, .001)
                    .withSource(TrainingSource.dataSources(DataSources.directory(root))).withCorpusTraining(new CorpusTrainingConfig(count))
                    .withValidationMethod(ValidationMethod.HELD_OUT);
            var state = NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2, 71, .001);
            var acquiredEnd = new AtomicLong(); var produced = new AtomicLong(); long[] first = {0}, peak = {0}, atFirst = {0};
            long started = System.nanoTime();
            try (var workspace = new SampleStore.Workspace(); var data = new SequentialTraining(config, config.source(), CorpusPreparation.NONE)) {
                var result = IncrementalTraining.train(state, state.snapshot(), config.training(1), new SelfPlayControl(), sink -> {
                    var batch = data.stream(1, sample -> { sink.accept(sample); produced.incrementAndGet(); });
                    acquiredEnd.set(System.nanoTime()); return batch;
                }, sample -> new IncrementalTraining.Prepared(sample, null), BrnCaptureConsistency.OFF, progress -> {
                    if (first[0] == 0) { first[0] = System.nanoTime(); atFirst[0] = produced.get(); }
                    var runtime = Runtime.getRuntime(); peak[0] = Math.max(peak[0], runtime.totalMemory() - runtime.freeMemory());
                }, status -> {});
                long ended = System.nanoTime();
                if (!result.verified() || result.training().orElseThrow().samplesTrained() != 2L * count
                        || state.step() != 2L * ((count + 127) / 128) || atFirst[0] >= count
                        || result.source().validation().samples().size() != held) throw new AssertionError("Incorrect incremental epochs");
                System.out.printf(Locale.ROOT,
                        "count=%d epochs=2 batch=128 acquiredAtFirstUpdate=%d firstUpdateMs=%.2f acquireMs=%.2f totalMs=%.2f acquiredRecordsPerSec=%.0f trainingVisitsPerSec=%.0f heapLimitMiB=%.1f sampledPeakMiB=%.2f sampleDiskBytes=%d%n",
                        count, atFirst[0], (first[0]-started)/1e6, (acquiredEnd.get()-started)/1e6, (ended-started)/1e6,
                        (count+held)*1e9/(acquiredEnd.get()-started), 2.0*count*1e9/(ended-started), Runtime.getRuntime().maxMemory()/1048576.0,
                        peak[0]/1048576.0, 64L*(count+held));
            }
        } finally {
            try (var paths = Files.walk(directory)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); }
        }
    }
}
