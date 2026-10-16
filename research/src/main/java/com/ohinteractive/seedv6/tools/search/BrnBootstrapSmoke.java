package com.ohinteractive.seedv6.tools.search;

import java.nio.file.*;
import java.time.Duration;
import java.util.Locale;
import com.ohinteractive.seedv6.core.brn.BrnTrainer;
import com.ohinteractive.seedv6.core.brn1.Brn1Trainer;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Explicit fresh-root, bounded real self-play/lifecycle measurement. No timing assertions or campaign. */
public final class BrnBootstrapSmoke {
    private static final String START = "7k/8/5K2/8/8/8/3Q4/8 w - - 0 1";
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("Usage: fresh-output-root [existing-NNUE-generator-store]");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(root)) throw new IllegalArgumentException("Output must be a new directory: " + root);
        Files.createDirectories(root);
        Path generator = args.length == 2 ? Path.of(args[1]).toAbsolutePath().normalize() : root.resolve("nnue-fixture");
        if (args.length == 1) try (var store = new CheckpointStore(generator, TrainingArchitecture.NNUE)) {
            store.initialize(new NnueTrainer(TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 4, ""));
        }
        String pinned = TrainingSource.bootstrap(generator).loadBest(root.resolve("probe")).manifest().id();
        System.out.println("BOOTSTRAP_SMOKE generator=" + generator + " checkpoint=" + pinned
                + " depth=4 threads=6 games=8 seed=71 fixture=" + START);
        for (var architecture : new TrainingArchitecture[] {TrainingArchitecture.BRN, TrainingArchitecture.BRN1, TrainingArchitecture.BRN2}) {
            Path student = root.resolve(architecture.name());
            var config = config(student, generator, architecture, architecture == TrainingArchitecture.BRN2 ? 1 : 2);
            try (var service = switch (architecture) {
                case BRN -> TrainerService.fresh(config, new BrnTrainer(.001));
                case BRN1 -> TrainerService.fresh(config, new Brn1Trainer(.001));
                case BRN2 -> TrainerService.fresh(config, new Brn2Trainer(.001));
                default -> throw new AssertionError();
            }) { finish(service); }
            if (architecture == TrainingArchitecture.BRN2) {
                try (var service = TrainerService.resume(config.withSource(null))) { finish(service); }
                System.out.println("BRN2_STOP_RESTART_RESUME=PASS");
            }
            var history = new HistoryRepository(student).refresh();
            if (!history.warnings().isEmpty() || history.records().size() != 2) throw new IllegalStateException("Incomplete smoke history: " + history);
            for (var r : history.records()) {
                var e = r.bootstrap();
                if (e == null || !e.generatorId().equals(pinned)) throw new IllegalStateException("Unexpected generator/evidence.");
                System.out.printf(Locale.ROOT, "%s generation=%d generation_ms=%.3f training_ms=%.3f validation_ms=%.3f total_ms=%.3f train=%d heldout=%d candidate_loss=%.17g best_loss=%.17g decision=%s%n",
                        architecture, r.generation(), r.selfPlayNanos()/1e6, r.trainingNanos()/1e6, r.validationNanos()/1e6,
                        r.totalNanos()/1e6, e.trainingSamples(), e.comparison().samples(), e.comparison().candidateLoss(), e.comparison().bestLoss(), r.decision());
            }
        }
    }
    private static TrainerConfig config(Path root, Path generator, TrainingArchitecture architecture, int generations) {
        return new TrainerConfig(root, 71, new TrainerConfig.SelfPlay(4, 6, 8, 0, 0, 32, 64, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(2, 0, 0, 4, 6, 64,
                NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)), generations, TrainerConfig.DepthChange.REQUIRE_SAME,
                START, architecture, .001, TrainingSource.bootstrap(generator));
    }
    private static void finish(TrainerService service) throws Exception {
        service.start();
        long deadline = System.nanoTime() + Duration.ofMinutes(3).toNanos();
        while (!service.awaitTermination(Duration.ofSeconds(10))) {
            System.out.println("SMOKE_PROGRESS " + service.config().architecture() + " " + service.snapshot().state()
                    + " games=" + service.snapshot().selfPlay().completedGames());
            if (System.nanoTime() >= deadline) { service.stop(); throw new IllegalStateException("Bounded smoke exceeded three minutes; stopping safely."); }
        }
        if (service.snapshot().failed() || !service.historyWarning().isEmpty())
            throw new IllegalStateException(service.snapshot().failureSummary() + " " + service.historyWarning());
    }
    private BrnBootstrapSmoke() {}
}
