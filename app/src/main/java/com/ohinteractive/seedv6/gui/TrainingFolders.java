package com.ohinteractive.seedv6.gui;

import java.util.EnumMap;
import java.util.prefs.Preferences;

/** GUI selection only; a folder never supplies model identity or lifecycle authority. */
final class TrainingFolders {
    private final EnumMap<NetworkArchitecture, String> roots = new EnumMap<>(NetworkArchitecture.class);
    private final Preferences preferences;

    TrainingFolders(TrainingSettings initial) {
        preferences = null;
        roots.put(initial.architecture(), initial.root().toString());
    }

    TrainingFolders(Preferences preferences) {
        this.preferences = preferences;
        migrate(preferences);
        for (var architecture : NetworkArchitecture.values()) roots.put(architecture, preferences.get(key(architecture), ""));
    }

    static String key(NetworkArchitecture architecture) {
        return switch (architecture) {
            case NNUE -> "checkpointRoot.nnue";
            case BRN -> "checkpointRoot.brn0";
            case BRN1 -> "checkpointRoot.brn1";
            case BRN2 -> "checkpointRoot.brn2";
        };
    }

    static void migrate(Preferences preferences) {
        if (preferences.getBoolean("checkpointRootsMigrated", false)) return;
        String legacy = preferences.get("root", null);
        if (legacy != null) {
            // Absence was NNUE. An unknown stored architecture is not evidence of NNUE ownership.
            try {
                var owner = NetworkArchitecture.valueOf(preferences.get("architecture", "NNUE"));
                if (preferences.get(key(owner), null) == null) preferences.put(key(owner), legacy);
            } catch (IllegalArgumentException invalidArchitecture) { /* Preserve legacy bytes without guessing. */ }
        }
        preferences.putBoolean("checkpointRootsMigrated", true);
    }

    String root(NetworkArchitecture architecture) { return roots.getOrDefault(architecture, ""); }

    void remember(NetworkArchitecture architecture, String root) {
        roots.put(architecture, root);
        if (preferences != null) preferences.put(key(architecture), root);
    }

    void select(NetworkArchitecture architecture) {
        if (preferences != null) preferences.put("architecture", architecture.name());
    }
}
