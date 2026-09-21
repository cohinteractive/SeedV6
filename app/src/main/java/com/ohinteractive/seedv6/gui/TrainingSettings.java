package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.prefs.Preferences;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.service.TrainerConfig;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Convenient UI choices only. Model, Adam and acceptance truth always comes from the store. */
record TrainingSettings(Path root, int depth, int threads, int games, int openingMin, int openingMax,
                        int samples, int minibatch, int epochs, int validationPairs, long seed,
                        int maximumPlies, long maximumGenerations) {
    static final NnueScoreMapping SCORE_MAPPING = NnueScoreMapping.V1;

    TrainingSettings {
        root = root.toAbsolutePath().normalize();
        // Use the authoritative service configuration validation, including cross-field bounds.
        config(root, depth, threads, games, openingMin, openingMax, samples, minibatch, epochs,
                validationPairs, seed, maximumPlies, maximumGenerations, TrainerConfig.DepthChange.REQUIRE_SAME);
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
                validationPairs, seed, maximumPlies, maximumGenerations, depthChange);
    }

    private static TrainerConfig config(Path root, int depth, int threads, int games, int openingMin,
            int openingMax, int samples, int minibatch, int epochs, int pairs, long seed, int maxPlies,
            long maxGenerations, TrainerConfig.DepthChange change) {
        return new TrainerConfig(root, seed,
                new TrainerConfig.SelfPlay(depth, threads, games, openingMin, openingMax, samples, maxPlies, SCORE_MAPPING),
                new TrainerConfig.Training(epochs, minibatch, true),
                new TrainerConfig.Validation(pairs, openingMin, openingMax, depth, threads, maxPlies,
                        SCORE_MAPPING, new PromotionPolicy(pairs, PromotionPolicy.DEFAULT.alpha(),
                                PromotionPolicy.DEFAULT.requiredMargin())), maxGenerations, change);
    }

    static Preferences preferences() { return Preferences.userNodeForPackage(TrainingSettings.class).node("nnue-training"); }

    static TrainingSettings load(Preferences prefs) {
        TrainingSettings d = defaults();
        try {
            return new TrainingSettings(Path.of(prefs.get("root", d.root.toString())),
                    prefs.getInt("depth", d.depth), prefs.getInt("threads", d.threads), prefs.getInt("games", d.games),
                    prefs.getInt("openingMin", d.openingMin), prefs.getInt("openingMax", d.openingMax),
                    prefs.getInt("samples", d.samples), prefs.getInt("minibatch", d.minibatch),
                    prefs.getInt("epochs", d.epochs), prefs.getInt("validationPairs", d.validationPairs),
                    prefs.getLong("seed", d.seed), prefs.getInt("maximumPlies", d.maximumPlies),
                    prefs.getLong("maximumGenerations", d.maximumGenerations));
        } catch (RuntimeException invalidPreference) { return d; }
    }

    void save(Preferences prefs) {
        prefs.put("root", root.toString()); prefs.putInt("depth", depth); prefs.putInt("threads", threads);
        prefs.putInt("games", games); prefs.putInt("openingMin", openingMin); prefs.putInt("openingMax", openingMax);
        prefs.putInt("samples", samples); prefs.putInt("minibatch", minibatch); prefs.putInt("epochs", epochs);
        prefs.putInt("validationPairs", validationPairs); prefs.putLong("seed", seed);
        prefs.putInt("maximumPlies", maximumPlies); prefs.putLong("maximumGenerations", maximumGenerations);
    }
}
