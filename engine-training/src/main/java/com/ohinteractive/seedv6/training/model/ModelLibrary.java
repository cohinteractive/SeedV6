package com.ohinteractive.seedv6.training.model;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Common application model browser over existing managed folders and adopted stores.
 * Registration remains owned by the existing application catalog; no parallel registry or cursor.
 * Call from an I/O worker. Metadata never participates in search or evaluator hot paths.
 */
public final class ModelLibrary {
    public record Entry(Path root, TrainingArchitecture architecture, String name,
                        Optional<TrainingLineage> lineage, String diagnostic) {
        public Entry {
            root = root.toAbsolutePath().normalize();
            Objects.requireNonNull(architecture); Objects.requireNonNull(name);
            Objects.requireNonNull(lineage); Objects.requireNonNull(diagnostic);
        }
        @Override public String toString() { return name + (diagnostic.isEmpty() ? "" : " (unavailable)"); }
    }
    public record Generation(CheckpointStore.CatalogCheckpoint checkpoint, boolean best, boolean latest,
                             Optional<GenerationRecord> history, Optional<GenerationAnnotation> annotation) {
        public String id() { return checkpoint.manifest().id(); }
        public long number() { return checkpoint.manifest().generation(); }
        public String label() {
            String tags = annotation.map(a -> String.join(", ", a.tags())).orElse("");
            return "Gen " + number() + (best ? " · Best" : "") + (latest ? " · Latest" : "")
                    + (!checkpoint.materialized() ? " · payload unavailable" : "")
                    + (tags.isEmpty() ? "" : " · " + tags);
        }
    }
    public record Snapshot(Entry lineage, List<Generation> generations, List<String> diagnostics, boolean publishOnly,
                           Optional<LineageProvenance> provenance, List<LineageRevision> revisions, List<GenerationRecord> records) {
        public Snapshot {
            generations = List.copyOf(generations); diagnostics = List.copyOf(diagnostics);
            revisions = List.copyOf(revisions); records = List.copyOf(records); Objects.requireNonNull(provenance);
        }
        public Optional<Generation> best() { return generations.stream().filter(Generation::best).findFirst(); }
        public Optional<Generation> latest() { return generations.stream().filter(Generation::latest).findFirst(); }
        public String exposureDescription() {
            var measured = records.stream().filter(r -> r.samples() != null).toList();
            if (measured.isEmpty()) return "Cumulative sampled positions: not recorded";
            java.math.BigInteger total = measured.stream().map(r -> java.math.BigInteger.valueOf(r.samples()))
                    .reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add);
            return "Recorded sampled positions: " + total + " across " + measured.size() + " completed generations"
                    + ". Includes held-out samples where recorded; repeated exposure is not unique positions."
                    + (generations.stream().anyMatch(g -> g.number() > 0 && g.history().map(h -> h.samples() == null).orElse(true))
                    ? " Additional historical exposure is unknown." : "");
        }
    }
    /** Concrete immutable binding; a historical unadopted lineage may have no recorded UUID. */
    public record Binding(Path root, Optional<UUID> lineageId, String lineageName,
                          TrainingArchitecture architecture, String checkpointId, long generation) {
        public Binding {
            root = root.toAbsolutePath().normalize();
            Objects.requireNonNull(lineageId); Objects.requireNonNull(lineageName);
            Objects.requireNonNull(architecture); Objects.requireNonNull(checkpointId);
            if (generation < 0) throw new IllegalArgumentException("Negative generation.");
        }
        public String description() { return architecture.displayName() + " · " + lineageName + " · Gen " + generation; }
    }
    public record Resolved(Binding binding, CheckpointStore.Checkpoint checkpoint) {}

    public static List<Entry> discover(Path base, TrainingArchitecture architecture, Collection<Path> adopted) throws IOException {
        base = base.toAbsolutePath().normalize();
        if (Files.exists(base) && !Files.isDirectory(base)) throw new IOException("Model library root must be a directory: " + base);
        var roots = new LinkedHashSet<Path>();
        Path directory = base.resolve(architecture.folderName());
        if (Files.exists(directory)) {
            if (!Files.isDirectory(directory)) throw new IOException("Architecture storage is not a directory: " + directory);
            try (var children = Files.list(directory)) {
                children.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).sorted().forEach(roots::add);
            }
        }
        for (Path root : adopted) roots.add(root.toAbsolutePath().normalize());
        var entries = new ArrayList<Entry>();
        for (Path root : roots) {
            try { entries.add(entry(root, architecture)); }
            catch (IOException invalid) {
                entries.add(new Entry(root, architecture, fallbackName(root), Optional.empty(), invalid.getMessage()));
            }
        }
        entries.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER).thenComparing(e -> e.root().toString()));
        return List.copyOf(entries);
    }

    public static Entry entry(Path root, TrainingArchitecture architecture) throws IOException {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Model lineage is unavailable: " + root);
        var lineage = TrainingLineage.read(root);
        if (lineage.isPresent() && lineage.get().architecture() != architecture)
            throw new IOException("Lineage architecture differs from the selected architecture.");
        return new Entry(root, architecture, lineage.map(TrainingLineage::name).orElseGet(() -> fallbackName(root)), lineage, "");
    }

    /** Resolve an existing preference/import without guessing architecture from a folder name. */
    public static Entry identify(Path root) throws IOException {
        var metadata = TrainingLineage.read(root);
        if (metadata.isPresent()) return entry(root, metadata.get().architecture());
        var catalog = CheckpointStore.catalog(root);
        var architectures = catalog.checkpoints().stream().map(c -> c.manifest().architecture()).distinct().toList();
        if (architectures.size() != 1) throw new IOException("No unambiguous model lineage at " + root);
        return entry(root, architectures.getFirst());
    }

    public static Snapshot browse(Entry requested) throws IOException {
        var entry = entry(requested.root(), requested.architecture());
        CheckpointInspection.freshRoot(entry.root(), entry.architecture());
        var catalog = CheckpointStore.catalog(entry.root());
        var diagnostics = new ArrayList<>(catalog.diagnostics());
        var history = new HistoryRepository(entry.root()).refresh();
        diagnostics.addAll(history.warnings());
        String best = reference(entry.root(), "best", diagnostics);
        String latest = reference(entry.root(), "latest-training", diagnostics);
        var records = new HashMap<String, GenerationRecord>();
        for (var record : history.records()) records.put(record.candidate(), record);
        var generations = new ArrayList<Generation>();
        for (var checkpoint : catalog.checkpoints()) {
            var manifest = checkpoint.manifest();
            if (manifest.architecture() != entry.architecture()) throw new IOException("Mixed architectures in model lineage.");
            Optional<GenerationAnnotation> annotation = Optional.empty();
            try { annotation = GenerationAnnotation.read(entry.root(), manifest.id()); }
            catch (IOException invalid) { diagnostics.add("Gen " + manifest.generation() + " annotation: " + invalid.getMessage()); }
            generations.add(new Generation(checkpoint, manifest.id().equals(best), manifest.id().equals(latest),
                    Optional.ofNullable(records.get(manifest.id())), annotation));
        }
        for (String id : List.of(best, latest))
            if (!id.isEmpty() && generations.stream().noneMatch(g -> g.id().equals(id)))
                diagnostics.add("Referenced generation metadata unavailable: " + id);
        boolean publishOnly = !Files.exists(entry.root().resolve("refs/best"), LinkOption.NOFOLLOW_LINKS);
        Path promotions = entry.root().resolve("promotions");
        if (Files.exists(promotions)) try (var files = Files.list(promotions)) { publishOnly &= files.findAny().isEmpty(); }
        Optional<LineageProvenance> provenance = Optional.empty(); List<LineageRevision> revisions = List.of();
        try { provenance = LineageProvenance.read(entry.root()); }
        catch (IOException invalid) { diagnostics.add("Initialization provenance: " + invalid.getMessage()); }
        try {
            revisions = LineageRevision.read(entry.root());
            if (entry.lineage().isPresent() && revisions.stream().anyMatch(r -> !r.lineage().id().equals(entry.lineage().get().id())))
                throw new IOException("Configuration history belongs to another lineage");
        } catch (IOException invalid) { revisions = List.of(); diagnostics.add("Configuration history: " + invalid.getMessage()); }
        return new Snapshot(entry, generations, diagnostics, publishOnly, provenance, revisions, history.records());
    }

    /** Best is an explicit alias; a concrete generation never depends on a Best pointer. */
    public static Resolved resolve(Entry requested, String checkpointId) throws IOException {
        var entry = entry(requested.root(), requested.architecture());
        if (requested.lineage().isPresent() && (entry.lineage().isEmpty()
                || !requested.lineage().get().id().equals(entry.lineage().get().id())))
            throw new IOException("Selected model lineage identity changed. Refresh the library.");
        if (CheckpointInspection.freshRoot(entry.root(), entry.architecture())) throw new IOException("Lineage has no trained generations.");
        var checkpoint = checkpointId.isEmpty() ? CheckpointStore.readBestSnapshot(entry.root())
                : CheckpointStore.readSnapshot(entry.root(), checkpointId);
        var manifest = checkpoint.manifest();
        if (manifest.architecture() != entry.architecture()) throw new IOException("Selected generation architecture differs from the lineage.");
        return new Resolved(new Binding(entry.root(), entry.lineage().map(TrainingLineage::id), entry.name(),
                entry.architecture(), manifest.id(), manifest.generation()), checkpoint);
    }

    private static String reference(Path root, String name, List<String> diagnostics) {
        try { return CheckpointInspection.reference(root, name); }
        catch (NoSuchFileException missing) { return ""; }
        catch (IOException invalid) { diagnostics.add(name + ": " + invalid.getMessage()); return ""; }
    }
    private static String fallbackName(Path root) {
        Path name = root.toAbsolutePath().normalize().getFileName();
        return name == null ? root.toString() : name.toString();
    }
    private ModelLibrary() {}
}
