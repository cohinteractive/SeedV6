package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Explicit isolated headless corpus campaign. Production service/optimizer/validation, no search. */
public final class BrnCorpusMain {
    public static void main(String[] args) throws Exception {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            int equals = arg.indexOf('=');
            if (!arg.startsWith("--") || equals < 3 || options.put(arg.substring(2, equals), arg.substring(equals + 1)) != null)
                throw new IllegalArgumentException("Use --corpus=PATH --output=PATH --seed=71 --positions=2000 [--generations=1 --resume=true --replay=1 --rate=0.001]");
        }
        if (!Set.of("corpus", "output", "seed", "positions", "generations", "resume", "replay", "rate").containsAll(options.keySet()))
            throw new IllegalArgumentException("Unknown corpus training option");
        Path corpus = Path.of(Objects.requireNonNull(options.get("corpus"), "--corpus"));
        Path output = Path.of(Objects.requireNonNull(options.get("output"), "--output"));
        int positions = Integer.parseInt(options.getOrDefault("positions", "2000"));
        long seed = Long.parseLong(options.getOrDefault("seed", "71"));
        double rate = Double.parseDouble(options.getOrDefault("rate", "0.001"));
        var config = new TrainerConfig(output, seed,
                new TrainerConfig.SelfPlay(1, 1, 4, 0, 0, 1, 1, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, false),
                new TrainerConfig.Validation(2, 0, 0, 1, 1, 1, NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                Long.parseLong(options.getOrDefault("generations", "1")), TrainerConfig.DepthChange.REQUIRE_SAME,
                TrainerConfig.STANDARD_START, TrainingArchitecture.BRN2, rate, TrainingSource.corpus(corpus))
                .withCorpusTraining(new CorpusTrainingConfig(positions)).withValidationMethod(ValidationMethod.HELD_OUT)
                .withTimeLimit(Duration.ofMinutes(2));
        if (options.containsKey("replay")) {
            BrnCorpusTraining.evidence(output, Long.parseLong(options.get("replay")));
            try (var source = new BrnCorpusTraining(config, config.source(), false)) {
                System.out.println("CORPUS_REPLAY " + source.batch(Long.parseLong(options.get("replay"))).evidence().json());
            }
            return;
        }
        boolean resume = Boolean.parseBoolean(options.getOrDefault("resume", "false"));
        if (!resume && !CheckpointInspection.freshRoot(output, TrainingArchitecture.BRN2))
            throw new IllegalArgumentException("Fresh validation requires a separate empty output folder");
        try (var service = resume ? TrainerService.resume(config) : TrainerService.fresh(config, new Brn2Trainer(rate))) {
            service.start();
            if (!service.awaitTermination(Duration.ofMinutes(3))) throw new IllegalStateException("Bounded corpus run did not terminate");
            if (service.failure().isPresent()) throw new IllegalStateException("Corpus training failed", service.failure().get());
            var result = service.snapshot();
            System.out.println("CORPUS_SELECTION " + service.corpusReport().orElseThrow().json());
            System.out.println("CORPUS_TRAINING " + result.training().orElseThrow());
            if (result.totals().completedGenerations() != config.maximumGenerations() || result.totals().completedGames() != 0
                    || !resume && result.totals().optimizerUpdates() != (long) positions * config.maximumGenerations())
                throw new IllegalStateException("Corpus lifecycle/update/generator accounting failed");
            try (var store = new CheckpointStore(output, TrainingArchitecture.BRN2)) {
                var latest = store.load(result.latestTrainingId());
                var validation = store.validationFor(result.latestTrainingId()).orElseThrow();
                var initial = CheckpointInspection.lineage(output, result.latestTrainingId()).stream().filter(m -> m.generation() == 0).findFirst().orElseThrow();
                boolean changed = !initial.networkSha256().equals(latest.manifest().networkSha256());
                if (!changed || !Double.isFinite(result.training().orElseThrow().finalLoss())) throw new IllegalStateException("No actual finite optimization result");
                System.out.println("CORPUS_RESULT generations=" + result.totals().completedGenerations() + " selfPlayGames=0 selectionSkipped=0 parametersChanged=" + changed
                        + " latestHash=" + latest.manifest().networkSha256() + " decision=" + validation.decision()
                        + " heldOut=" + validation.bootstrap().comparison() + " output=" + output.toAbsolutePath());
            }
            if (!service.historyWarning().isEmpty()) throw new IllegalStateException(service.historyWarning());
        }
    }
    private BrnCorpusMain() {}
}
