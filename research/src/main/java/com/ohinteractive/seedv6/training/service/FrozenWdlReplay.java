package com.ohinteractive.seedv6.training.service;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Explicit, bounded CLI over TrainerService; 'stop' on stdin or Ctrl+C requests a durable safe stop. */
public final class FrozenWdlReplay {
    private FrozenWdlReplay() {}
    public static TrainerConfig controlSettings(Path student) {
        return new TrainerConfig(student, 1, new TrainerConfig.SelfPlay(4, 6, 64, 0, 8, 32, 1024, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(64, 0, 8, 4, 6, 1024,
                NnueScoreMapping.V1, PromotionPolicy.DEFAULT), 128, TrainerConfig.DepthChange.REQUIRE_SAME,
                TrainerConfig.STANDARD_START, TrainingArchitecture.BRN2, .001, TrainingSource.HANDCRAFTED)
                .withRunSeeds(new BrnRunSeeds(1, 1));
    }
    public static void main(String[] args) throws Exception {
        var options = new HashMap<String, String>();
        for (String arg : args) {
            int separator = arg.indexOf('=');
            if (!arg.startsWith("--") || separator < 3 || options.put(arg.substring(2, separator), arg.substring(separator + 1)) != null)
                throw new IllegalArgumentException("Expected unique --store=PATH [--source=PATH] --until=GEN [--stop-after-updates=N].");
        }
        if (!Set.of("store", "source", "until", "stop-after-updates").containsAll(options.keySet())
                || !options.containsKey("store") || !options.containsKey("until"))
            throw new IllegalArgumentException("Explicit --store and absolute --until generation are required.");
        Path root = Path.of(options.get("store")).toAbsolutePath().normalize();
        long until = Long.parseLong(options.get("until"));
        long stopAfter = Long.parseLong(options.getOrDefault("stop-after-updates", "0"));
        if (stopAfter < 0) throw new IllegalArgumentException("Negative update stop bound.");
        var saved = FrozenReplay.read(root);
        FrozenReplay replay;
        if (saved.isPresent()) {
            replay = saved.get();
            if (options.containsKey("source") && !Path.of(options.get("source")).toRealPath().toString().equals(replay.sourceStore()))
                throw new IOException("Resume source differs from the pinned frozen lineage.");
        } else {
            if (!options.containsKey("source")) throw new IllegalArgumentException("A fresh replay requires --source.");
            replay = FrozenReplay.capture(Path.of(options.get("source")), controlSettings(root));
        }
        if (replay.entries().size() != 128) throw new IOException("This workflow requires the complete 128-generation frozen control.");
        System.out.println("Frozen WDL source=" + replay.sourceStore() + " corpusSha256=" + replay.hash()
                + " endpoint=g" + until + "; no generation or teacher evaluation. Type stop and Enter for a durable stop.");
        var operations = new GuardedOperations(stopAfter);
        try (var service = TrainerService.frozenReplay(root, replay, until, operations, snapshot -> {})) {
            operations.service = service;
            Thread shutdown = new Thread(service::close, "frozen-replay-safe-stop");
            Runtime.getRuntime().addShutdownHook(shutdown);
            Thread input = new Thread(() -> {
                try {
                    var reader = new BufferedReader(new InputStreamReader(System.in));
                    for (String line; (line = reader.readLine()) != null;) if (line.trim().equalsIgnoreCase("stop")) { service.stop(); return; }
                } catch (IOException ignored) { /* EOF/closed console is not a force-stop request. */ }
            }, "frozen-replay-console");
            input.setDaemon(true); input.start();
            try {
                service.start();
                while (!service.awaitTermination(Duration.ofSeconds(10))) {
                    var s = service.snapshot();
                    System.out.println("g" + s.generation() + " " + s.state() + " step=" + s.optimizerStep());
                }
                if (service.snapshot().failed()) throw new IOException("Replay failed; durable state preserved.", service.failure().orElse(null));
                var end = service.snapshot();
                System.out.println("STOPPED generation=" + end.generation() + " step=" + end.optimizerStep()
                        + " settledThisRun=" + end.totals().completedGenerations() + " latest=" + end.latestTrainingId());
                System.out.println("Replay generationCalls=0; canonical WDL training/heldout; " + service.lifecycleNotice());
                if (!service.historyWarning().isEmpty()) throw new IOException(service.historyWarning());
            } finally {
                try { Runtime.getRuntime().removeShutdownHook(shutdown); }
                catch (IllegalStateException exiting) { /* Ctrl+C already owns the safe-stop hook. */ }
            }
        }
    }
    static class GuardedOperations extends TrainerService.Operations {
        final long stopAfter; long updates; TrainerService service;
        GuardedOperations(long stopAfter) { this.stopAfter = stopAfter; }
        @Override SelfPlayBatch generate(NetworkModel model, SelfPlayConfig config, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
            throw new AssertionError("Frozen replay attempted neural position generation.");
        }
        @Override SelfPlayBatch generate(NnueNetwork model, SelfPlayConfig config, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
            throw new AssertionError("Frozen replay attempted NNUE position generation.");
        }
        @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig config, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
            throw new AssertionError("Frozen replay attempted Handcrafted position generation.");
        }
        @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,
                List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config config, SelfPlayControl control,
                Consumer<SelfPlayTraining.Progress> observer) {
            return super.trainBootstrap(state, samples, config, control, progress -> {
                observer.accept(progress);
                if (stopAfter > 0 && ++updates >= stopAfter) service.stop();
            });
        }
    }
}
