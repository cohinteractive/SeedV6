package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.Objects;
import java.util.prefs.Preferences;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.service.TrainerConfig;
import com.ohinteractive.seedv6.training.service.TrainingSource;
import com.ohinteractive.seedv6.training.service.BrnSupervision;
import com.ohinteractive.seedv6.training.service.BrnRunSeeds;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Convenient UI choices only. Model, Adam and acceptance truth always comes from the store. */
record TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                        int samples, int minibatch, int epochs, int validationPairs, long seed,
                        int maximumPlies, long maximumGenerations, NetworkArchitecture architecture, double brnLearningRate, double brn1LearningRate, double brn2LearningRate, TrainingSource source, String generatorStore, BrnSupervision supervision, BrnRunSeeds runSeeds, String teacherStore, long maximumRunMinutes) {
    static final NnueScoreMapping SCORE_MAPPING = NnueScoreMapping.V1;

    TrainingSettings {
        Objects.requireNonNull(architecture, "architecture");
        if (maximumRunMinutes < 0 || maximumRunMinutes > 5256000) throw new IllegalArgumentException("Invalid run duration.");
        Objects.requireNonNull(generatorStore, "generatorStore");
        if (teacherStore != null) teacherStore = teacherStore.isBlank() ? "" : Path.of(teacherStore).toAbsolutePath().normalize().toString();
        if (runSeeds != null && (architecture != NetworkArchitecture.BRN2 || seed != runSeeds.masterSeed()))
            throw new IllegalArgumentException("Run seeds require BRN-2 and matching master seed.");
        if (supervision != null) supervision.requireSupported(architecture.trainingArchitecture(), source);
        new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(brnLearningRate);
        new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(brn1LearningRate);
        new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(brn2LearningRate);
        root = root.toAbsolutePath().normalize();
        // Use the authoritative service configuration validation, including cross-field bounds.
        config(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, TrainerConfig.DepthChange.REQUIRE_SAME, architecture, architecture == NetworkArchitecture.BRN2 ? brn2LearningRate : architecture == NetworkArchitecture.BRN1 ? brn1LearningRate : brnLearningRate);
    }

    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
            int samples, int minibatch, int epochs, int validationPairs, long seed, int maximumPlies,
            long maximumGenerations, NetworkArchitecture architecture, double rate, double rate1, double rate2,
            TrainingSource source, String generatorStore, BrnSupervision supervision, BrnRunSeeds runSeeds, String teacherStore) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs, validationPairs, seed,
                maximumPlies, maximumGenerations, architecture, rate, rate1, rate2, source, generatorStore,
                supervision, runSeeds, teacherStore, 0);
    }
    TrainingSettings withTimeLimit(long minutes) {
        return new TrainingSettings(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate,
                brn2LearningRate, source, generatorStore, supervision, runSeeds, teacherStore, minutes);
    }

    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
            int samples, int minibatch, int epochs, int validationPairs, long seed, int maximumPlies,
            long maximumGenerations, NetworkArchitecture architecture, double brnLearningRate, double brn1LearningRate,
            double brn2LearningRate, TrainingSource source, String generatorStore, BrnSupervision supervision, BrnRunSeeds runSeeds) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs, validationPairs,
                seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate,
                brn2LearningRate, source, generatorStore, supervision, runSeeds, null);
    }
    TrainingSettings withTeacherStore(String value) {
        return new TrainingSettings(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate,
                brn2LearningRate, source, generatorStore, supervision, runSeeds, value, maximumRunMinutes);
    }
    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                     int samples, int minibatch, int epochs, int validationPairs, long seed,
                     int maximumPlies, long maximumGenerations, NetworkArchitecture architecture,
                     double brnLearningRate, double brn1LearningRate, double brn2LearningRate,
                     TrainingSource source, String generatorStore) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs, validationPairs,
                seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate,
                brn2LearningRate, source, generatorStore, null);
    }
    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                     int samples, int minibatch, int epochs, int validationPairs, long seed,
                     int maximumPlies, long maximumGenerations, NetworkArchitecture architecture,
                     double brnLearningRate, double brn1LearningRate, double brn2LearningRate,
                     TrainingSource source, String generatorStore, BrnSupervision supervision) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs, validationPairs,
                seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate,
                brn2LearningRate, source, generatorStore, supervision, null);
    }
    TrainingSettings withRunSeeds(BrnRunSeeds value) {
        return new TrainingSettings(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, value == null ? seed : value.masterSeed(), maximumPlies, maximumGenerations, architecture,
                brnLearningRate, brn1LearningRate, brn2LearningRate, source, generatorStore, supervision, value, teacherStore, maximumRunMinutes);
    }
    TrainingSettings withSupervision(BrnSupervision value) {
        return new TrainingSettings(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate,
                brn2LearningRate, source, generatorStore, value, runSeeds, teacherStore, maximumRunMinutes);
    }

    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                     int samples, int minibatch, int epochs, int validationPairs, long seed,
                     int maximumPlies, long maximumGenerations, NetworkArchitecture architecture,
                     double brnLearningRate, double brn1LearningRate, double brn2LearningRate) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs, validationPairs,
                seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate, brn2LearningRate, null, "");
    }
    TrainingSettings withSource(TrainingSource value) {
        return new TrainingSettings(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate,
                brn2LearningRate, value, value != null && value.nnue() ? value.generatorStore() : generatorStore, supervision, runSeeds, teacherStore, maximumRunMinutes);
    }

    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                     int samples, int minibatch, int epochs, int validationPairs, long seed,
                     int maximumPlies, long maximumGenerations, NetworkArchitecture architecture, double brnLearningRate, double brn1LearningRate) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs, validationPairs,
                seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, brn1LearningRate, TrainerConfig.DEFAULT_BRN_LEARNING_RATE);
    }

    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                     int samples, int minibatch, int epochs, int validationPairs, long seed,
                     int maximumPlies, long maximumGenerations, NetworkArchitecture architecture, double brnLearningRate) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs, validationPairs,
                seed, maximumPlies, maximumGenerations, architecture, brnLearningRate, TrainerConfig.DEFAULT_BRN_LEARNING_RATE);
    }

    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                     int samples, int minibatch, int epochs, int validationPairs, long seed,
                     int maximumPlies, long maximumGenerations, NetworkArchitecture architecture) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs, validationPairs,
                seed, maximumPlies, maximumGenerations, architecture, TrainerConfig.DEFAULT_BRN_LEARNING_RATE);
    }

    // Existing callers continue to default to NNUE.
    TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                     int samples, int minibatch, int epochs, int validationPairs, long seed,
                     int maximumPlies, long maximumGenerations) {
        this(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, NetworkArchitecture.NNUE);
    }

    static Path defaultRoot() {
        return defaultRoot(System.getenv("LOCALAPPDATA"), System.getProperty("user.home"));
    }

    static Path defaultRoot(String localAppData, String userHome) {
        return localAppData == null || localAppData.isBlank()
                ? Path.of(userHome, ".seedv6-nnue", "training")
                : Path.of(localAppData, "SeedV6-NNUE", "training");
    }

    static TrainingSettings defaults() {
        return new TrainingSettings(defaultRoot(), 4, 1, 64, 0, 8, 32, 32, 1, 64, 1L, 1024, 0);
    }

    TrainerConfig config(TrainerConfig.DepthChange depthChange) {
        return config(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, depthChange, architecture, architecture == NetworkArchitecture.BRN2 ? brn2LearningRate : architecture == NetworkArchitecture.BRN1 ? brn1LearningRate : brnLearningRate).withSource(source).withSupervision(supervision).withRunSeeds(runSeeds).withTeacherStore(teacherStore).withTimeLimit(java.time.Duration.ofMinutes(maximumRunMinutes));
    }

    private static TrainerConfig config(Path root, int depth, int threads, int games, int openingMin,
            int openingMax, int samples, int minibatch, int epochs, int pairs, long seed, int maxPlies,
            long maxGenerations, TrainerConfig.DepthChange change, NetworkArchitecture architecture, double brnLearningRate) {
        return new TrainerConfig(root, seed,
                new TrainerConfig.SelfPlay(depth, threads, games, openingMin, openingMax, samples, maxPlies, SCORE_MAPPING),
                new TrainerConfig.Training(architecture == NetworkArchitecture.NNUE ? epochs : 1,
                        architecture == NetworkArchitecture.NNUE ? minibatch : 1, true),
                new TrainerConfig.Validation(pairs, openingMin, openingMax, depth, threads, maxPlies,
                        SCORE_MAPPING, new PromotionPolicy(pairs, PromotionPolicy.DEFAULT.alpha(),
                                PromotionPolicy.DEFAULT.requiredMargin())), maxGenerations, change, TrainerConfig.STANDARD_START,
                architecture.trainingArchitecture(), brnLearningRate);
    }

    static Preferences preferences() { return Preferences.userNodeForPackage(TrainingSettings.class).node("nnue-training"); }

    static TrainingSettings load(Preferences prefs) {
        TrainingSettings d = defaults();
        TrainingFolders.migrate(prefs);
        try {
            var architecture = NetworkArchitecture.valueOf(prefs.get("architecture", NetworkArchitecture.NNUE.name()));
            String selected = prefs.get(TrainingFolders.key(architecture), "");
            // Settings require a valid path; the GUI separately displays an unset selection and blocks Start.
            return new TrainingSettings(selected.isBlank() ? d.root : Path.of(selected),
                    prefs.getInt("depth", d.depth), prefs.getInt("threads", d.threads), prefs.getInt("games", d.games),
                    prefs.getInt("openingMin", d.openingMin), prefs.getInt("openingMax", d.openingMax),
                    prefs.getInt("samples", d.samples), prefs.getInt("minibatch", d.minibatch),
                    prefs.getInt("epochs", d.epochs), prefs.getInt("validationPairs", d.validationPairs),
                    prefs.getLong("seed", d.seed), prefs.getInt("maximumPlies", d.maximumPlies),
                    prefs.getLong("maximumGenerations", d.maximumGenerations),
                    architecture,
                    prefs.getDouble("brnLearningRate", d.brnLearningRate),
                    prefs.getDouble("brn1LearningRate", d.brn1LearningRate),
                    prefs.getDouble("brn2LearningRate", d.brn2LearningRate), sourcePreference(prefs, architecture, selected),
                    prefs.get("nnueGeneratorStore." + architecture.name(), "")).withSupervision(supervisionPreference(prefs, architecture, selected))
                    .withRunSeeds(runSeedsPreference(prefs, architecture, selected))
                    .withTeacherStore(architecture == NetworkArchitecture.BRN2 && selected.equals(prefs.get("brn2Teacher.root", ""))
                            ? prefs.get("brn2Teacher.store", null) : null).withTimeLimit(prefs.getLong("maximumRunMinutes", 0));
        } catch (RuntimeException invalidPreference) { return d; }
    }

    void save(Preferences prefs) {
        save(prefs, true);
    }

    private static TrainingSource sourcePreference(Preferences prefs, NetworkArchitecture architecture, String root) {
        String prefix = "trainingSource." + architecture.name() + ".";
        if (architecture == NetworkArchitecture.NNUE || root.isBlank() || !root.equals(prefs.get(prefix + "root", ""))) return null;
        String mode = prefs.get(prefix + "mode", "");
        return mode.isBlank() ? null : new TrainingSource(TrainingSource.Mode.valueOf(mode), prefs.get(prefix + "generator", ""));
    }

    private static BrnSupervision supervisionPreference(Preferences prefs, NetworkArchitecture architecture, String root) {
        if (architecture != NetworkArchitecture.BRN2 || root.isBlank()
                || !root.equals(prefs.get("brn2Supervision.root", ""))) return null;
        String mode = prefs.get("brn2Supervision.mode", "");
        return mode.isEmpty() ? null : new BrnSupervision(BrnSupervision.Mode.valueOf(mode), prefs.getDouble("brn2Supervision.weight", 0));
    }

    private static BrnRunSeeds runSeedsPreference(Preferences prefs, NetworkArchitecture architecture, String root) {
        if (architecture != NetworkArchitecture.BRN2 || root.isBlank()
                || !root.equals(prefs.get("brn2RunSeeds.root", "")) || prefs.get("brn2RunSeeds.data", "").isEmpty()) return null;
        return new BrnRunSeeds(prefs.getLong("brn2RunSeeds.master", 1), Long.parseLong(prefs.get("brn2RunSeeds.data", "")));
    }

    // The production panel persists selection immediately; queued configuration saves must not
    // overwrite a later architecture/folder selection made while the I/O executor was busy.
    void saveConfiguration(Preferences prefs) { save(prefs, false); }

    private void save(Preferences prefs, boolean selection) {
        TrainingFolders.migrate(prefs);
        if (architecture == NetworkArchitecture.BRN2) {
            prefs.put("brn2Teacher.root", root.toString());
            if (teacherStore == null) prefs.remove("brn2Teacher.store"); else prefs.put("brn2Teacher.store", teacherStore);
            prefs.put("brn2RunSeeds.root", root.toString());
            prefs.put("brn2RunSeeds.data", runSeeds == null ? "" : Long.toString(runSeeds.dataSeed()));
            if (runSeeds != null) prefs.putLong("brn2RunSeeds.master", runSeeds.masterSeed());
        }
        if (architecture == NetworkArchitecture.BRN2 && supervision != null) {
            prefs.put("brn2Supervision.root", root.toString()); prefs.put("brn2Supervision.mode", supervision.mode().name());
            prefs.putDouble("brn2Supervision.weight", supervision.teacherWeight());
        }
        if (architecture != NetworkArchitecture.NNUE) prefs.put("nnueGeneratorStore." + architecture.name(), generatorStore);
        if (architecture != NetworkArchitecture.NNUE && source != null) {
            String prefix = "trainingSource." + architecture.name() + ".";
            prefs.put(prefix + "root", root.toString()); prefs.put(prefix + "mode", source.mode().name());
            prefs.put(prefix + "generator", source.generatorStore());
        }
        // Absence remains the historical NNUE default; retain every existing NNUE key/value.
        if (selection) prefs.put("architecture", architecture.name());
        if (brnLearningRate != TrainerConfig.DEFAULT_BRN_LEARNING_RATE || architecture == NetworkArchitecture.BRN
                || prefs.get("brnLearningRate", null) != null) prefs.putDouble("brnLearningRate", brnLearningRate);
        if (brn1LearningRate != TrainerConfig.DEFAULT_BRN_LEARNING_RATE || architecture == NetworkArchitecture.BRN1
                || prefs.get("brn1LearningRate", null) != null) prefs.putDouble("brn1LearningRate", brn1LearningRate);
        if (brn2LearningRate != TrainerConfig.DEFAULT_BRN_LEARNING_RATE || architecture == NetworkArchitecture.BRN2
                || prefs.get("brn2LearningRate", null) != null) prefs.putDouble("brn2LearningRate", brn2LearningRate);
        if (selection) prefs.put(TrainingFolders.key(architecture), root.toString());
        prefs.putInt("depth", depth); prefs.putInt("threads", threads);
        prefs.putInt("games", games); prefs.putInt("openingMin", openingMin); prefs.putInt("openingMax", openingMax);
        prefs.putInt("samples", samples); prefs.putInt("minibatch", minibatch); prefs.putInt("epochs", epochs);
        prefs.putInt("validationPairs", validationPairs); prefs.putLong("seed", seed);
        prefs.putInt("maximumPlies", maximumPlies); prefs.putLong("maximumGenerations", maximumGenerations); prefs.putLong("maximumRunMinutes", maximumRunMinutes);
    }
}
