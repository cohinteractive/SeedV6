package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Bounded real-record optimization through the production service. Verification-only; no GUI. */
public final class NnueCorpusMain {
    public static void main(String[] args) throws Exception {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            int equals = arg.indexOf('=');
            if (!arg.startsWith("--") || equals < 3 || options.put(arg.substring(2, equals), arg.substring(equals + 1)) != null)
                throw new IllegalArgumentException("Use --corpus=PATH --output=NEW_PATH [--positions=2048 --batch=128 --epochs=1 --seed=71 --view-record-limit=4096 --variant=material|legacy]");
        }
        if (!Set.of("corpus", "output", "positions", "batch", "epochs", "seed", "view-record-limit", "variant").containsAll(options.keySet()))
            throw new IllegalArgumentException("Unknown NNUE corpus diagnostic option");
        Path corpus = Path.of(Objects.requireNonNull(options.get("corpus"), "--corpus"));
        Path output = Path.of(Objects.requireNonNull(options.get("output"), "--output")).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalArgumentException("Diagnostic output must be a new isolated directory");
        int positions = Integer.parseInt(options.getOrDefault("positions", "2048"));
        int batch = Integer.parseInt(options.getOrDefault("batch", "128"));
        int epochs = Integer.parseInt(options.getOrDefault("epochs", "1"));
        long seed = Long.parseLong(options.getOrDefault("seed", "71"));
        long limit = Long.parseLong(options.getOrDefault("view-record-limit", "4096"));
        if (positions < 2 || positions > 10000 || epochs < 1 || epochs > 3 || batch < 1 || batch > 1024 || limit < 4 || limit > 100000)
            throw new IllegalArgumentException("Diagnostic bounds exceeded");
        // Preserve historical invocations; always report the resolved identity before any work.
        TrainingArchitecture architecture = switch (options.getOrDefault("variant", "legacy")) {
            case "legacy" -> TrainingArchitecture.NNUE;
            case "material" -> TrainingArchitecture.NNUE_MATERIAL;
            default -> throw new IllegalArgumentException("Use --variant=material or --variant=legacy");
        };
        System.out.println("NNUE_IDENTITY " + architecture.displayName() + " schema=" + architecture.schemaId());
        var config = new TrainerConfig(output, seed,
                new TrainerConfig.SelfPlay(1, 1, 4, 0, 0, 1, 1, NnueScoreMapping.V1),
                new TrainerConfig.Training(epochs, batch, true),
                new TrainerConfig.Validation(2, 0, 0, 1, 1, 1, NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                1, TrainerConfig.DepthChange.REQUIRE_SAME, TrainerConfig.STANDARD_START, architecture, TrainerConfig.DEFAULT_BRN_LEARNING_RATE)
                .withSource(TrainingSource.dataSources(com.ohinteractive.seedv6.training.data.DataSources.directory(output))).withCorpusTraining(new CorpusTrainingConfig(positions))
                .withValidationMethod(ValidationMethod.HELD_OUT).withTimeLimit(Duration.ofMinutes(3));
        var registered = com.ohinteractive.seedv6.training.data.DataSource.register("Diagnostic source", corpus, 1);
        try (var store = new CheckpointStore(output, architecture)) {
            new com.ohinteractive.seedv6.training.data.DataSources(1, List.of(registered), false)
                    .save(com.ohinteractive.seedv6.training.data.DataSources.directory(output));
            store.initialize(architecture == TrainingArchitecture.NNUE_MATERIAL
                    ? new NetworkTrainingState.NnueMaterial(NnueTrainer.materialParity(TrainableNnue.initialized(seed)))
                    : new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(seed))), new CheckpointManifest.Metadata(0, 1, ""));
            store.writeTrainingSource(config.source());
        }
        var bounded = new TrainerService.Operations() {
            @Override CorpusTraining openCorpus(TrainerConfig c, TrainingSource source, boolean mayCreate, CorpusPreparation preparation) throws java.io.IOException {
                return new SequentialTraining(c, source, preparation, limit);
            }
        };
        try (var service = TrainerService.resume(config, bounded, snapshot -> {})) {
            service.start();
            if (!service.awaitTermination(Duration.ofMinutes(4))) { service.stop(); throw new IllegalStateException("Bounded NNUE diagnostic did not terminate"); }
            if (service.failure().isPresent()) throw new IllegalStateException("NNUE corpus training failed", service.failure().get());
            var result = service.snapshot(); var trained = result.training().orElseThrow(); var selection = service.corpusReport().orElseThrow();
            System.out.println("CORPUS_SELECTION " + selection.json());
            System.out.println("CORPUS_TRAINING " + trained);
            if (result.totals().completedGenerations() != 1 || result.totals().selfPlayGames() != 0
                    || trained.samplesTrained() != (long) positions * epochs
                    || trained.optimizerUpdates() != ((positions + (long) batch - 1) / batch) * epochs)
                throw new IllegalStateException("NNUE corpus lifecycle/update/generator accounting failed");
            try (var replay = new SequentialTraining(service.config(), config.source(), CorpusPreparation.NONE, limit)) {
                if (!selection.equals(replay.batch(1).evidence())) throw new IllegalStateException("Deterministic replay differs");
                System.out.println("CORPUS_REPLAY identical=true trainingHash=" + selection.trainingHash() + " heldOutHash=" + selection.heldOutHash());
            }
            try (var store = new CheckpointStore(output, architecture)) {
                var latest = store.load(result.latestTrainingId()); var initial = store.load(latest.manifest().parentId());
                boolean changed = !initial.manifest().networkSha256().equals(latest.manifest().networkSha256());
                var validation = store.validationFor(latest.manifest().id()).orElseThrow();
                if (!changed || trained.optimizerUpdates() < 1 || !Double.isFinite(trained.finalLoss()))
                    throw new IllegalStateException("No actual finite NNUE optimization result");
                System.out.println("CORPUS_RESULT adapter=" + selection.adapterIdentity() + " parametersChanged=" + changed
                        + " trainingPositionGamesGenerated=0 decision=" + validation.decision() + " heldOut=" + validation.bootstrap().comparison()
                        + " initialHash=" + initial.manifest().networkSha256() + " latestHash=" + latest.manifest().networkSha256() + " output=" + output);
            }
            if (!service.historyWarning().isEmpty()) throw new IllegalStateException(service.historyWarning());
        }
    }
    private NnueCorpusMain() {}
}
