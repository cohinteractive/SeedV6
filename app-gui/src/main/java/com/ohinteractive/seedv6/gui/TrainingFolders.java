package com.ohinteractive.seedv6.gui;

import java.util.prefs.Preferences;
import java.nio.file.Path;
import java.util.*;

/** GUI selection only; a folder never supplies model identity or lifecycle authority. */
final class TrainingFolders {
    private final EnumMap<NetworkArchitecture, String> roots = new EnumMap<>(NetworkArchitecture.class);
    private final Preferences preferences;
    private Path base;
    private Path arenaCampaign;
    private String lastCorpusRoot = "";
    private String lastCorpusArchive = "";
    private boolean corpusImportAll = true;
    private long corpusImportLimit = 1_000_000;
    private final Map<Path, EnumMap<NetworkArchitecture, LinkedHashSet<Path>>> catalogs = new HashMap<>();

    TrainingFolders(TrainingSettings initial) {
        preferences = null;
        roots.put(initial.architecture(), initial.root().toString());
        base = inferBase(initial.root(), initial.architecture());
        register(base, initial.architecture(), initial.root());
    }

    TrainingFolders(Preferences preferences) {
        this.preferences = preferences;
        String savedArena = preferences.get("lastArenaCampaign", "");
        if (!savedArena.isBlank()) {
            try { arenaCampaign = Path.of(savedArena).toAbsolutePath().normalize(); }
            catch (java.nio.file.InvalidPathException invalid) { preferences.remove("lastArenaCampaign"); }
        }
        lastCorpusRoot = preferences.get("lastSeedCorpusRoot", preferences.get("lastBrnCorpusRoot", ""));
        if (!lastCorpusRoot.isBlank() && preferences.get("lastSeedCorpusRoot", null) == null)
            preferences.put("lastSeedCorpusRoot", lastCorpusRoot);
        lastCorpusArchive = preferences.get("lastCorpusArchive", "");
        corpusImportAll = preferences.getBoolean("corpusImportAll", true);
        corpusImportLimit = Math.max(1, preferences.getLong("corpusImportLimit", 1_000_000));
        migrate(preferences);
        for (var architecture : NetworkArchitecture.values()) roots.put(architecture, preferences.get(key(architecture), ""));
        base = Path.of(preferences.get("baseTrainingRoot", suggestedBase().toString())).toAbsolutePath().normalize();
        if (!preferences.getBoolean("lineageCatalogMigrated", false)) {
            for (var architecture : NetworkArchitecture.values()) if (!root(architecture).isBlank())
                register(base, architecture, Path.of(root(architecture)));
            preferences.putBoolean("lineageCatalogMigrated", true);
        }
    }

    private Path suggestedBase() {
        for (var a : NetworkArchitecture.values()) if (!root(a).isBlank()) {
            Path p = Path.of(root(a)).toAbsolutePath().normalize();
            if (p.getParent() != null && p.getParent().getFileName() != null
                    && p.getParent().getFileName().toString().equals(a.folderName())) return p.getParent().getParent();
        }
        return TrainingSettings.defaultRoot().resolveSibling("networks");
    }
    private static Path inferBase(Path root, NetworkArchitecture architecture) {
        Path p = root.toAbsolutePath().normalize().getParent();
        return p != null && p.getFileName() != null && p.getFileName().toString().equals(architecture.folderName())
                ? p.getParent() : root.toAbsolutePath().normalize().resolveSibling("networks");
    }
    Path base() { return base; }
    /** Only the selection lives in preferences; campaign.json owns all campaign settings. */
    Path arenaCampaign() { return arenaCampaign; }
    void rememberArenaCampaign(Path path) {
        Path selected = path == null ? null : path.toAbsolutePath().normalize();
        if (Objects.equals(arenaCampaign, selected)) return;
        arenaCampaign = selected;
        if (preferences != null) {
            if (selected == null) preferences.remove("lastArenaCampaign");
            else preferences.put("lastArenaCampaign", selected.toString());
        }
    }
    void base(Path value) {
        base = value.toAbsolutePath().normalize();
        if (preferences != null) preferences.put("baseTrainingRoot", base.toString());
    }
    private Preferences catalog(Path base, NetworkArchitecture architecture) {
        String id = UUID.nameUUIDFromBytes(base.toAbsolutePath().normalize().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        return preferences.node("lineages").node(id).node(architecture.name());
    }
    synchronized void register(Path base, NetworkArchitecture architecture, Path root) {
        root = root.toAbsolutePath().normalize();
        catalogs.computeIfAbsent(base, b -> new EnumMap<>(NetworkArchitecture.class))
                .computeIfAbsent(architecture, a -> new LinkedHashSet<>()).add(root);
        if (preferences != null) catalog(base, architecture).put(UUID.nameUUIDFromBytes(root.toString()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(), root.toString());
    }
    synchronized List<Path> adopted(Path base, NetworkArchitecture architecture) throws java.io.IOException {
        var result = new LinkedHashSet<Path>(catalogs.getOrDefault(base, new EnumMap<>(NetworkArchitecture.class))
                .getOrDefault(architecture, new LinkedHashSet<>()));
        if (preferences != null) try {
            var node = catalog(base, architecture);
            for (String key : node.keys()) result.add(Path.of(node.get(key, "")));
        } catch (java.util.prefs.BackingStoreException invalid) { throw new java.io.IOException("Cannot read lineage catalog.", invalid); }
        return List.copyOf(result);
    }

    static String key(NetworkArchitecture architecture) {
        return switch (architecture) {
            case NNUE -> "checkpointRoot.nnue";
            case NNUE_MATERIAL -> "checkpointRoot.nnueMaterial";
            case BRN -> "checkpointRoot.brn0";
            case BRN1 -> "checkpointRoot.brn1";
            case BRN2 -> "checkpointRoot.brn2";
            case BRN3 -> "checkpointRoot.brn3";
            case BRN_PAIR2 -> "checkpointRoot.brnPair2";
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

    String lastCorpusRoot() { return lastCorpusRoot; }
    String lastCorpusArchive() { return lastCorpusArchive; }
    boolean corpusImportAll() { return corpusImportAll; }
    long corpusImportLimit() { return corpusImportLimit; }

    void rememberCorpusImport(String archive, boolean all, long limit) {
        if (limit < 1) throw new IllegalArgumentException("Bounded import count must be positive.");
        lastCorpusArchive = archive; corpusImportAll = all; corpusImportLimit = limit;
        if (preferences != null) {
            preferences.put("lastCorpusArchive", archive);
            preferences.putBoolean("corpusImportAll", all);
            preferences.putLong("corpusImportLimit", limit);
        }
    }

    /** Shared GUI default, after a catalog check or explicit import-directory selection. */
    void rememberCorpusRoot(String root) {
        lastCorpusRoot = root;
        if (preferences != null) preferences.put("lastSeedCorpusRoot", root);
    }

    void remember(NetworkArchitecture architecture, String root) {
        roots.put(architecture, root);
        if (preferences != null) preferences.put(key(architecture), root);
    }

    void select(NetworkArchitecture architecture) {
        if (preferences != null) preferences.put("architecture", architecture.name());
    }
}
