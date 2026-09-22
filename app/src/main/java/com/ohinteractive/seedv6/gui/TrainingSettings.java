package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.Objects;
import java.util.prefs.Preferences;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.service.TrainerConfig;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Convenient UI choices only. Model, Adam and acceptance truth always comes from the store. */
record TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                        int samples, int minibatch, int epochs, int validationPairs, long seed,
                        int maximumPlies, long maximumGenerations, NetworkArchitecture architecture, double brnLearningRate, double brn1LearningRate, double brn2LearningRate) {
    static final NnueScoreMapping SCORE_MAPPING = NnueScoreMapping.V1;

    TrainingSettings {
        Objects.requireNonNull(architecture, "architecture");
        new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(brnLearningRate);
        new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(brn1LearningRate);
        new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(brn2LearningRate);
        root = root.toAbsolutePath().normalize();
        // Use the authoritative service configuration validation, including cross-field bounds.
        config(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, TrainerConfig.DepthChange.REQUIRE_SAME, architecture, architecture == NetworkArchitecture.BRN2 ? brn2LearningRate : architecture == NetworkArchitecture.BRN1 ? brn1LearningRate : brnLearningRate);
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
                validationPairs, seed, maximumPlies, maximumGenerations, depthChange, architecture, architecture == NetworkArchitecture.BRN2 ? brn2LearningRate : architecture == NetworkArchitecture.BRN1 ? brn1LearningRate : brnLearningRate);
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
                    prefs.getDouble("brn2LearningRate", d.brn2LearningRate));
        } catch (RuntimeException invalidPreference) { return d; }
    }

    void save(Preferences prefs) {
        save(prefs, true);
    }

    // The production panel persists selection immediately; queued configuration saves must not
    // overwrite a later architecture/folder selection made while the I/O executor was busy.
    void saveConfiguration(Preferences prefs) { save(prefs, false); }

    private void save(Preferences prefs, boolean selection) {
        TrainingFolders.migrate(prefs);
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
        prefs.putInt("maximumPlies", maximumPlies); prefs.putLong("maximumGenerations", maximumGenerations);
    }
}
