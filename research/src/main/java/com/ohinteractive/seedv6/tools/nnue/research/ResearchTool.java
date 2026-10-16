package com.ohinteractive.seedv6.tools.nnue.research;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;

/** Explicit-path research CLI. No experiment is reachable from normal Main, UCI or GUI startup. */
public final class ResearchTool {
    static final NnueScoreMapping MAPPING = NnueScoreMapping.V1;
    private final Map<String, String> args;
    private ResearchTool(String[] argv) {
        args = new HashMap<>();
        for (int i = 1; i < argv.length; i += 2) {
            if (!argv[i].startsWith("--") || i + 1 == argv.length || args.put(argv[i].substring(2), argv[i + 1]) != null) {
                throw new IllegalArgumentException("Expected unique --name value arguments.");
            }
        }
    }
    public static void main(String[] argv) throws Exception {
        Locale.setDefault(Locale.ROOT);
        if (argv.length == 0 || argv[0].equals("help")) {
            System.out.println("ResearchTool <strength|health|progression|campaign|endurance|calibration|stage> --seed N [options]\n"
                    + "strength: --a handcrafted|CHECKPOINT_DIRECTORY --b handcrafted|CHECKPOINT_DIRECTORY --mode EVALUATION_ISOLATION|PRACTICAL_ENGINE\n"
                    + "  --depth 2 --threads 1 --pairs 8 --opening-min 0 --opening-max 8 --cap 1024 --minimum-pairs 8 --alpha 0.05\n"
                    + "  --seconds 0 (default: no clock limit; optional safety cancellation makes remaining pairs incomplete)\n"
                    + "strength/campaign/endurance: --scale N overrides canonical " + NnueScoreMapping.V1_ID + "\n"
                    + "health: --checkpoint CHECKPOINT_DIRECTORY --positions 128\n"
                    + "progression/stage: --root EXISTING_EXPERIMENT_ROOT --positions 128; stage optionally --strength-results MATCH_LOG\n"
                    + "campaign: --root NEW_ROOT --depth 4 --threads 1 --generations 4 --games 10 --validation-pairs 1 --seconds 3600\n"
                    + "  Whole games 4,9,... are holdout; one epoch, batch 32, default Adam, 0..8 exploration, samples 32.\n"
                    + "endurance: --root NEW_ROOT --depth 1 --threads 2 --generations 10 --seconds 1800\n"
                    + "calibration: --checkpoint CHECKPOINT_DIRECTORY --holdout HOLDOUT_BIN\n"
                    + "All experiments are bounded. Campaign/endurance reject existing roots. No default GUI store. Fixed-depth strength has no clock limit.");
            return;
        }
        var tool = new ResearchTool(argv);
        tool.seed(); // Every operation is explicitly identified, including corpus and match RNG.
        switch (argv[0]) {
            case "strength" -> tool.strength();
            case "health" -> {
                var health = NetworkHealth.analyze(tool.network(), NetworkHealth.corpus(tool.seed(), tool.integer("positions", 128)));
                out("HEALTH", tool.required("checkpoint"), "parameterRms", health.parameterRms(), health);
            }
            case "progression", "stage" -> tool.progression(argv[0].equals("stage"));
            case "campaign" -> tool.campaign();
            case "endurance" -> tool.endurance();
            case "calibration" -> out("CALIBRATION", tool.required("checkpoint"), ResearchData.calibrate(Path.of(tool.required("holdout")), tool.network()));
            default -> throw new IllegalArgumentException("Unknown command: " + argv[0]);
        }
        if (!tool.args.isEmpty()) throw new IllegalArgumentException("Unused options: " + tool.args.keySet());
    }
    // Consumption catches misspelled/irrelevant options before experiments through each command's check().
    private final Map<String, String> consumed = new HashMap<>();
    private String option(String name, String fallback) {
        if (consumed.containsKey(name)) return consumed.get(name);
        String value = args.remove(name);
        if (value == null) value = fallback;
        if (value != null) consumed.put(name, value);
        return value;
    }
    private String required(String name) { String value = option(name, null); if (value == null) throw new IllegalArgumentException("Required --" + name); return value; }
    private int integer(String name, int fallback) { return Integer.parseInt(option(name, Integer.toString(fallback))); }
    private long seed() { return Long.parseLong(required("seed")); }
    private NnueScoreMapping mapping() { return new NnueScoreMapping(Double.parseDouble(option("scale", Double.toString(MAPPING.scale())))); }
    private Path root() { return Path.of(required("root")).toAbsolutePath().normalize(); }
    private NnueNetwork network() throws IOException { return CheckpointStore.inspect(Path.of(required("checkpoint"))).network(); }
    private void check() { if (!args.isEmpty()) throw new IllegalArgumentException("Unknown/irrelevant options: " + args.keySet()); }
    static void out(Object... fields) {
        StringJoiner line = new StringJoiner("\t"); for (Object field : fields) line.add(String.valueOf(field)); System.out.println(line);
    }
    private ValidationConfig resources(int defaultDepth) {
        return new ValidationConfig(integer("pairs", 8), seed(), integer("opening-min", 0), integer("opening-max", 8),
                integer("depth", defaultDepth), integer("threads", 1), mapping(), integer("cap", 1024));
    }
    private static StrengthArena.Actor actor(String path) throws IOException {
        return path.equals("handcrafted") ? StrengthArena.Actor.handcrafted() : StrengthArena.Actor.checkpoint(Path.of(path));
    }
    private void strength() throws Exception {
        var a = actor(required("a")); var b = actor(required("b"));
        var config = new StrengthArena.Config(resources(2), StrengthArena.Mode.valueOf(required("mode")),
                integer("minimum-pairs", 8), Double.parseDouble(option("alpha", "0.05")));
        int limit = integer("seconds", 0);
        if (limit < 0) throw new IllegalArgumentException("Negative safety budget.");
        check(); out("MATCH_START", a.id(), b.id(), config, "safetySeconds", limit);
        long start = System.nanoTime();
        ValidationControl control = new ValidationControl();
        ScheduledExecutorService timer = limit == 0 ? null : Executors.newSingleThreadScheduledExecutor();
        if (timer != null) timer.schedule(control::cancel, limit, TimeUnit.SECONDS);
        StrengthArena.Result result;
        try {
            int[] index = {0};
            result = new StrengthArena().match(a, b, config, control, pair -> out("PAIR", index[0]++, pair));
        } finally { if (timer != null) { timer.shutdownNow(); timer.awaitTermination(10, TimeUnit.SECONDS); } }
        out("MATCH", a.id(), b.id(), config.mode(), result.games().statistics(), result.evidence(), "seconds", seconds(start));
        result.evidence().eloEquivalent().ifPresent(v -> out("Elo-equivalent from observed score", v));
    }
    private void progression(boolean stage) throws IOException {
        Path root = root(); int positions = integer("positions", 128);
        String strength = stage ? option("strength-results", null) : null; check();
        ProgressionAnalysis.analyze(root, seed(), positions, row -> out("CHECKPOINT", "parameterRms",
                row.health().map(NetworkHealth.Report::parameterRms), row));
        if (stage) {
            if (Files.exists(root.resolve("games.tsv"))) ResearchData.diversity(root);
            var lineage = CheckpointInspection.lineage(root, CheckpointInspection.reference(root, "latest-training"));
            int depth = lineage.getLast().trainingDepth();
            // Current contiguous depth stage, excluding the bootstrap publication from trained generations.
            Set<Long> generations = new TreeSet<>();
            for (int i = lineage.size() - 1; i >= 0 && lineage.get(i).trainingDepth() == depth; i--) {
                if (!lineage.get(i).parentId().isEmpty()) generations.add(lineage.get(i).generation());
            }
            long completed = 0, decisive = 0, attempts = 0;
            if (Files.exists(root.resolve("games.tsv"))) for (String line : Files.readAllLines(root.resolve("games.tsv"))) {
                String[] f = line.split("\t");
                if (!generations.contains(Long.parseLong(f[0]))) continue;
                attempts++; var termination = GameTermination.valueOf(f[3]);
                if (termination.completed()) { completed++; if (termination.result().orElseThrow() != GameResult.DRAW) decisive++; }
            }
            var validations = CheckpointInspection.validations(root);
            long promoted = 0, retained = 0, inconclusive = 0;
            for (var checkpoint : lineage) if (generations.contains(checkpoint.generation()) && validations.containsKey(checkpoint.id())) {
                switch (validations.get(checkpoint.id()).assessment().decision()) {
                    case PROMOTE -> promoted++; case RETAIN_INCUMBENT -> retained++; case INCONCLUSIVE -> inconclusive++;
                }
            }
            out("STAGE_METRICS", "depth", depth, "trainedGenerations", generations.size(), "attemptedGames", attempts,
                    "completedGames", completed, "decisiveRate", (double) decisive / completed, "promotions/retains/inconclusive",
                    promoted, retained, inconclusive, "promotionRate", (double) promoted / (promoted + retained + inconclusive));
            if (strength != null) for (String line : Files.readAllLines(Path.of(strength))) {
                if (line.startsWith("MATCH\t")) out("STAGE_STRENGTH", line);
            }
            out("STAGE", "CLASSIFICATION_DECLINED", "Inspect raw loss/update/health/validation and paired strength trends; no supported universal plateau threshold. No automatic depth change.");
        }
    }
    private TrainerConfig config(Path root, int depth, int threads, int games, int pairs, long generations) {
        return new TrainerConfig(root, seed(), new TrainerConfig.SelfPlay(depth, threads, games, 0, 8, 32, 1024, mapping()),
                new TrainerConfig.Training(1, 32, true), new TrainerConfig.Validation(pairs, 0, 8, 1, 1, 1024, mapping(),
                PromotionPolicy.DEFAULT), generations, TrainerConfig.DepthChange.REQUIRE_SAME);
    }
    static void newRoot(Path root) throws IOException {
        if (Files.exists(root)) throw new IOException("Experiment needs a fresh, nonexistent root: " + root);
        Files.createDirectories(root.getParent()); Files.createDirectory(root);
    }
    static boolean holdoutGame(int index) { return index % 5 == 4; }
    private void campaign() throws Exception {
        Path root = root(); int depth = integer("depth", 4), threads = integer("threads", 1), generations = integer("generations", 4);
        int games = integer("games", 10), pairs = integer("validation-pairs", 1), seconds = integer("seconds", 3600);
        if (generations < 1 || games < 5 || seconds < 1) throw new IllegalArgumentException("Invalid bounded campaign.");
        var config = config(root, depth, threads, games, pairs, generations); check(); newRoot(root);
        out("CAMPAIGN_START", config, "holdout", "index%5==4", "secondsBudget", seconds);
        ResearchData.append(root.resolve("experiment.tsv"), "campaign-v1", config, "holdout=index%5==4", "secondsBudget=" + seconds, AdamHyperparameters.DEFAULT);
        long start = System.nanoTime(); SelfPlayControl control = new SelfPlayControl(); ValidationControl validationControl = new ValidationControl();
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(() -> { control.cancel(); validationControl.cancel(); }, seconds, TimeUnit.SECONDS);
        try (CheckpointStore store = new CheckpointStore(root);
             var holdout = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(root.resolve("holdout.bin"), StandardOpenOption.CREATE_NEW)))) {
            holdout.writeInt(0x4A484F31);
            NnueTrainer initial = new NnueTrainer(TrainableNnue.initialized(seed()));
            String bootstrap = store.initialize(initial, new CheckpointManifest.Metadata(0, depth, "")).manifest().id();
            initial = null;
            out("BOOTSTRAP", bootstrap);
            String parent = bootstrap;
            for (int generation = 1; generation <= generations && !control.cancelled(); generation++) {
                long generationStart = System.nanoTime();
                NnueTrainer trainer = store.resume(parent);
                NnueNetwork frozen = trainer.model().snapshot();
                List<TrajectorySampler.Sample> training = new ArrayList<>();
                SelfPlayConfig selfPlay = config.selfPlay(generation);
                int completed = 0, held = 0;
                for (int index = 0; index < games && !control.cancelled(); index++) {
                    long gameStart = System.nanoTime();
                    GameTrajectory game = SelfPlayRunner.play(frozen, selfPlay, index, Board.startingPosition(), control);
                    boolean reserve = holdoutGame(index);
                    ResearchData.append(root.resolve("games.tsv"), generation, index, reserve ? "HOLDOUT" : "TRAIN", game.termination(),
                            game.playedPlies(), ResearchData.openingHash(game, selfPlay, index), ResearchData.gameHash(game), seconds(gameStart));
                    out("GAME", generation, index, reserve ? "HOLDOUT" : "TRAIN", game.termination(), game.playedPlies(), "seconds", seconds(gameStart));
                    if (game.termination() == GameTermination.SEARCH_FAILURE || game.termination() == GameTermination.INFRASTRUCTURE_FAILURE) {
                        throw new IOException("Campaign game failed: " + game.failure());
                    }
                    if (game.termination().completed()) {
                        completed++;
                        var samples = TrajectorySampler.sample(game, 32);
                        if (reserve && !samples.isEmpty()) { held++; ResearchData.holdout(holdout, generation, index, samples); }
                        else if (!reserve) training.addAll(samples);
                    }
                }
                if (control.cancelled()) { out("CAMPAIGN_PARTIAL", generation, "completedGames", completed, "No partial-generation update."); break; }
                var trained = SelfPlayTraining.trainSamples(trainer, training, config.training(generation), control, p -> {});
                if (control.cancelled()) { out("CAMPAIGN_PARTIAL", generation, "Unpublished training cancelled."); break; }
                var candidate = store.publish(trainer, new CheckpointManifest.Metadata(generation, depth, parent));
                var before = store.recover(); String best = before.best().orElseThrow().manifest().id();
                var match = new ValidationArena().validate(candidate.network(), store.load(best).network(), config.validation(generation), validationControl);
                for (var pair : match.pairs()) ResearchData.append(root.resolve("validation-games.tsv"), generation, pair);
                for (var reason : List.of(GameTermination.SEARCH_FAILURE, GameTermination.INFRASTRUCTURE_FAILURE)) {
                    if (match.statistics().terminations().getOrDefault(reason, 0) > 0) throw new IOException("Campaign validation failed: " + reason);
                }
                var decision = CandidateLifecycle.recordDecision(store, candidate.manifest().id(), best, match, PromotionPolicy.DEFAULT);
                var delta = NetworkHealth.distance(frozen, candidate.network());
                ResearchData.append(root.resolve("generations.tsv"), generation, candidate.manifest().id(), parent, candidate.manifest().optimizerStep(),
                        completed, held, trained, delta, decision.validation().assessment(), seconds(generationStart));
                out("GENERATION", generation, candidate.manifest().id(), "completed/held", completed, held, trained, delta,
                        decision.validation().assessment(), "best", decision.references().best().orElseThrow().manifest().id(), "seconds", seconds(generationStart));
                parent = candidate.manifest().id();
            }
            var recovered = store.recover();
            out("CAMPAIGN_END", "seconds", seconds(start), "cancelled", control.cancelled(),
                    "best", recovered.best().orElseThrow().manifest(), "latest", recovered.latestTraining().orElseThrow().manifest());
        } finally { timer.shutdownNow(); timer.awaitTermination(10, TimeUnit.SECONDS); }
        ResearchData.diversity(root);
        String bootstrap = CheckpointInspection.lineage(root, CheckpointInspection.reference(root, "latest-training")).getFirst().id();
        out("HOLDOUT_BOOTSTRAP", ResearchData.calibrate(root.resolve("holdout.bin"), CheckpointStore.inspect(root.resolve("checkpoints").resolve(bootstrap)).network()));
        out("HOLDOUT_LATEST", ResearchData.calibrate(root.resolve("holdout.bin"), CheckpointStore.inspect(root.resolve("checkpoints").resolve(CheckpointInspection.reference(root, "latest-training"))).network()));
    }
    private void endurance() throws Exception {
        Path root = root(); int depth = integer("depth", 1), threads = integer("threads", 2), target = integer("generations", 10), seconds = integer("seconds", 1800);
        if (target < 2 || seconds < 1) throw new IllegalArgumentException("Invalid soak bounds.");
        var first = config(root, depth, threads, 1, 1, 0); check(); newRoot(root);
        ResearchData.append(root.resolve("experiment.tsv"), "endurance-v1", first, "target=" + target, "secondsBudget=" + seconds);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds), start = System.nanoTime();
        resources(root, "BEFORE");
        try (var service = TrainerService.fresh(first, new NnueTrainer(TrainableNnue.initialized(seed())))) {
            runService(root, service, Math.max(1, target / 2), deadline);
        }
        var middle = recover(root); resources(root, "AFTER_STOP");
        long remaining = target - middle.generation();
        if (remaining > 0) try (var service = TrainerService.resume(config(root, depth, threads, 1, 1, remaining))) {
            runService(root, service, Long.MAX_VALUE, deadline);
        }
        var soaked = recover(root); resources(root, "AFTER_SOAK");
        if (soaked.generation() < target) throw new IOException("Soak target not reached before bound: " + soaked.generation());
        // Fresh service/store after both preceding services were closed. Exact Adam continuation is checked.
        try (var service = TrainerService.resume(config(root, depth, threads, 1, 1, 1))) {
            runService(root, service, Long.MAX_VALUE, deadline);
        }
        var finalState = recover(root); resources(root, "AFTER_REOPEN_GENERATION");
        if (finalState.generation() != soaked.generation() + 1 || finalState.optimizerStep() <= soaked.optimizerStep()
                || !finalState.parentId().equals(soaked.id())) throw new IOException("Post-soak continuation mismatch.");
        out("ENDURANCE_PASS", "soakGenerations", soaked.generation(), "finalGeneration", finalState.generation(), "seconds", seconds(start));
    }
    private static void runService(Path root, TrainerService service, long stopAt, long deadline) throws Exception {
        service.start(); long observed = -1; long start = System.nanoTime(); boolean stop = false;
        while (!service.isTerminated()) {
            TrainerSnapshot s = service.snapshot();
            if (s.totals().completedGenerations() != observed) {
                observed = s.totals().completedGenerations();
                out("SERVICE", s); ResearchData.append(root.resolve("service.tsv"), "observedSeconds", seconds(start), s);
                resources(root, "GENERATION_" + s.generation());
            }
            if (!stop && (observed >= stopAt || System.nanoTime() >= deadline)) { stop = true; service.stop(); out("STOP_REQUEST", s.generation(), observed); }
            if (System.nanoTime() > deadline + TimeUnit.SECONDS.toNanos(60)) throw new IOException("Trainer did not stop within grace period.");
            service.awaitTermination(Duration.ofMillis(100));
        }
        service.stop();
        out("SERVICE_STOPPED", service.snapshot()); ResearchData.append(root.resolve("service.tsv"), "STOPPED", service.snapshot());
        if (service.failure().isPresent()) throw new IOException("Unexpected trainer failure.", service.failure().get());
        if (System.nanoTime() >= deadline) throw new IOException("Endurance time budget exhausted.");
    }
    static CheckpointManifest recover(Path root) throws IOException {
        long start = System.nanoTime();
        try (var store = new CheckpointStore(root)) {
            var recovered = store.recover();
            if (!recovered.diagnostics().isEmpty()) throw new IOException("Recovery diagnostics: " + recovered.diagnostics());
            var latest = recovered.latestTraining().orElseThrow().manifest();
            long step = store.resume(latest.id()).optimizer().step();
            if (step != latest.optimizerStep()) throw new IOException("Adam recovery mismatch.");
            for (var manifest : CheckpointInspection.lineage(root, latest.id())) {
                CheckpointStore.inspectHistorical(root.resolve("checkpoints").resolve(manifest.id()));
                if (!manifest.parentId().isEmpty() && store.validationFor(manifest.id()).isEmpty()) throw new IOException("Missing durable generation validation.");
            }
            out("RECOVERED", "best", recovered.best().orElseThrow().manifest().id(), "latest", latest,
                    "adamStep", step, "seconds", seconds(start));
            return latest;
        }
    }
    static void resources(Path root, String label) throws IOException {
        long trainers = 0, search = 0;
        for (Thread thread : Thread.getAllStackTraces().keySet()) if (thread.isAlive()) {
            if (thread.getName().startsWith("seedv6-trainer-")) trainers++;
            if (thread.getName().startsWith("seedv6-root-worker-")) search++;
        }
        if (label.startsWith("AFTER") || label.equals("BEFORE")) {
            if (trainers != 0 || search != 0) throw new IOException("Worker leak after stop.");
            System.gc();
        }
        long disk = 0, checkpoints = 0, validations = 0, promotions = 0;
        try (var paths = Files.walk(root)) {
            for (Path p : paths.filter(Files::isRegularFile).toList()) {
                disk += Files.size(p);
                if (p.getFileName().toString().equals("manifest.bin")) checkpoints++;
                if (p.getParent().getFileName().toString().equals("validations")) validations++;
                if (p.getParent().getFileName().toString().equals("promotions")) promotions++;
            }
        }
        long gc = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0, b.getCollectionCount())).sum();
        long heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        out("RESOURCES", label, "trainerThreads", trainers, "searchThreads", search, "heapBytes", heap, "gc", gc,
                "checkpoints", checkpoints, "validations", validations, "promotions", promotions, "diskBytes", disk);
        ResearchData.append(root.resolve("resources.tsv"), label, trainers, search, heap, gc, checkpoints, validations, promotions, disk);
    }
    static double seconds(long start) { return (System.nanoTime() - start) / 1e9; }
}
