package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.ohinteractive.seedv6.training.checkpoint.*;

/** Managed directories plus explicit in-place adoptions. No checkpoint payload is copied or relocated. */
final class TrainingLineages {
    record Entry(Path root, NetworkArchitecture architecture, String name) {
        Entry { root = root.toAbsolutePath().normalize(); }
        @Override public String toString() { return name + " | " + architecture; }
    }
    record Selection(Entry entry, TrainingLineage lineage, TrainingSettings settings, boolean fresh, boolean seedLocked) {}

    static List<Entry> discover(Path base, NetworkArchitecture architecture, Collection<Path> adopted) throws IOException {
        return com.ohinteractive.seedv6.training.model.ModelLibrary.discover(base,
                architecture.trainingArchitecture(), adopted).stream()
                .map(e -> new Entry(e.root(), architecture, e.toString())).toList();
    }

    static Selection read(Entry entry) throws IOException {
        if (!Files.isDirectory(entry.root())) throw new IOException("Training lineage folder is unavailable: " + entry.root());
        boolean fresh = CheckpointInspection.freshRoot(entry.root(), entry.architecture().trainingArchitecture());
        var metadata = TrainingLineage.read(entry.root());
        TrainingSettings settings;
        TrainingLineage lineage;
        if (metadata.isPresent() && !metadata.get().configuration().isEmpty()) {
            lineage = metadata.get();
            settings = LineageConfiguration.decode(lineage.configuration(), entry.root(), entry.architecture());
        } else {
            // GenerationAttempt stores derived RNG streams/fingerprints; history omits run limits and
            // fresh-only controls. Neither can reconstruct the complete editable configuration exactly.
            settings = TrainingSettings.defaults(entry.root(), entry.architecture());
            settings = new TrainingController.Backend().resolveSource(settings);
            String origin = "Architecture defaults loaded; complete historical configuration was not recorded. Review before Start/Resume.";
            var old = metadata.orElse(null);
            lineage = new TrainingLineage(old == null ? UUID.randomUUID() : old.id(), old == null ? entry.name() : old.name(),
                    entry.architecture().trainingArchitecture(), old == null ? Instant.now() : old.created(),
                    LineageConfiguration.encode(settings), origin);
        }
        settings = new TrainingController.Backend().resolveSource(settings);
        boolean seedLocked = !fresh || CheckpointStore.readBrnRunSeeds(entry.root()).isPresent();
        return new Selection(new Entry(entry.root(), entry.architecture(), lineage.name()), lineage, settings, fresh, seedLocked);
    }

    static Entry create(Path base, NetworkArchitecture architecture, String name) throws IOException {
        if (!com.ohinteractive.seedv6.training.model.ArchitectureLibrary.describe(architecture.trainingArchitecture()).canCreate())
            throw new IOException("This architecture is retained for existing lineages only.");
        UUID id = UUID.randomUUID();
        Path root = base.toAbsolutePath().normalize().resolve(architecture.folderName()).resolve(id.toString());
        var settings = new TrainingController.Backend().resolveSource(TrainingSettings.defaults(root, architecture));
        settings = settings.withLearningRate(com.ohinteractive.seedv6.training.model.ArchitectureLibrary.describe(architecture.trainingArchitecture()).defaults().learningRate());
        var lineage = new TrainingLineage(id, name, architecture.trainingArchitecture(), Instant.now(),
                LineageConfiguration.encode(settings), "New lineage: architecture defaults");
        try (var store = new CheckpointStore(root, architecture.trainingArchitecture())) { store.writeLineage(lineage); }
        return new Entry(root, architecture, lineage.name());
    }

    static Entry rename(Selection selection, String name) throws IOException {
        try (var store = new CheckpointStore(selection.entry().root(), selection.lineage().architecture())) {
            var previous = TrainingLineage.read(store.root()).orElseThrow(() -> new IOException("Missing lineage metadata."));
            if (!previous.id().equals(selection.lineage().id())) throw new IOException("Training lineage identity changed.");
            store.writeLineage(previous.withName(name));
            return new Entry(store.root(), selection.entry().architecture(), name.strip());
        }
    }

    static void adopt(Selection selection) throws IOException {
        if (TrainingLineage.read(selection.entry().root()).filter(l -> !l.configuration().isEmpty()).isPresent()) return;
        try (var store = new CheckpointStore(selection.entry().root(), selection.lineage().architecture())) {
            var existing = TrainingLineage.read(store.root());
            if (existing.isPresent() && (!existing.get().id().equals(selection.lineage().id()) || !existing.get().configuration().isEmpty()))
                throw new IOException("Lineage was adopted concurrently. Select it again.");
            store.writeLineage(selection.lineage());
        }
    }

    static void save(Selection selected, TrainingSettings settings) throws IOException { save(selected, settings, null); }
    static void save(Selection selected, TrainingSettings settings, com.ohinteractive.seedv6.training.data.DataSources sources) throws IOException {
        if (!selected.entry().root().equals(settings.root()) || selected.entry().architecture() != settings.architecture())
            throw new IOException("Settings do not belong to the selected training lineage.");
        try (var store = new CheckpointStore(settings.root(), settings.architecture().trainingArchitecture())) {
            var previous = TrainingLineage.read(store.root()).orElseThrow(() -> new IOException("Missing lineage metadata."));
            if (!previous.id().equals(selected.lineage().id())) throw new IOException("Training lineage identity changed.");
            if (sources != null) sources.save(com.ohinteractive.seedv6.training.data.DataSources.directory(store.root()));
            store.writeLineage(previous.withConfiguration(LineageConfiguration.encode(settings)));
        }
    }
    private TrainingLineages() {}
}
