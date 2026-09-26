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
        @Override public String toString() { return name; }
    }
    record Selection(Entry entry, TrainingLineage lineage, TrainingSettings settings, boolean fresh, boolean seedLocked) {}

    static List<Entry> discover(Path base, NetworkArchitecture architecture, Collection<Path> adopted) throws IOException {
        if (Files.exists(base) && !Files.isDirectory(base)) throw new IOException("Base Training Root must be a directory: " + base);
        var roots = new LinkedHashSet<Path>();
        Path directory = base.resolve(architecture.toString());
        if (Files.exists(directory)) {
            if (!Files.isDirectory(directory)) throw new IOException("Architecture storage is not a directory: " + directory);
            try (var children = Files.list(directory)) {
                children.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).sorted().forEach(roots::add);
            }
        }
        roots.addAll(adopted);
        var entries = new ArrayList<Entry>();
        for (Path root : roots) {
            // Keep invalid/unavailable registered stores selectable so a load reports its error, never a silent substitute.
            String name = root.getFileName().toString();
            try { name = TrainingLineage.read(root).map(TrainingLineage::name).orElse(name); }
            catch (IOException invalid) { name += " (unavailable)"; }
            entries.add(new Entry(root, architecture, name));
        }
        entries.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER).thenComparing(e -> e.root().toString()));
        return List.copyOf(entries);
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
        UUID id = UUID.randomUUID();
        Path root = base.toAbsolutePath().normalize().resolve(architecture.toString()).resolve(id.toString());
        var settings = new TrainingController.Backend().resolveSource(TrainingSettings.defaults(root, architecture));
        var lineage = new TrainingLineage(id, name, architecture.trainingArchitecture(), Instant.now(),
                LineageConfiguration.encode(settings), "New lineage: architecture defaults");
        try (var store = new CheckpointStore(root, architecture.trainingArchitecture())) { store.writeLineage(lineage); }
        return new Entry(root, architecture, lineage.name());
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

    static void save(Selection selected, TrainingSettings settings) throws IOException {
        if (!selected.entry().root().equals(settings.root()) || selected.entry().architecture() != settings.architecture())
            throw new IOException("Settings do not belong to the selected training lineage.");
        try (var store = new CheckpointStore(settings.root(), settings.architecture().trainingArchitecture())) {
            var previous = TrainingLineage.read(store.root()).orElseThrow(() -> new IOException("Missing lineage metadata."));
            if (!previous.id().equals(selected.lineage().id())) throw new IOException("Training lineage identity changed.");
            store.writeLineage(previous.withConfiguration(LineageConfiguration.encode(settings)));
        }
    }
    private TrainingLineages() {}
}
