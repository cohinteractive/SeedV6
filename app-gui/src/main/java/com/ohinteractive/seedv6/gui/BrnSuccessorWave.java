package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Explicit owner-run preparation command; ordinary GUI startup has no seeding side effect. */
public final class BrnSuccessorWave {
    static final String WAVE = "brn-successor-2026-10-20-wave1";
    static final List<NetworkArchitecture> ARCHITECTURES = List.of(NetworkArchitecture.BRN_GATED,
            NetworkArchitecture.BRN_CUBIC, NetworkArchitecture.BRN_POOLED);
    public record Entry(String candidate, String name, String architecture, String configuration,
                        String root, UUID lineageId, String initialCheckpoint, String initialNetworkSha256,
                        String currentCheckpoint, String currentNetworkSha256, long currentGeneration,
                        long seed, TrainingRecipe recipe, String sourceSelectionIdentity, String partition) {}
    public record Report(String wave, String recorded, String library, String operation,
                         String source, String sourceIdentity, List<Entry> entries) {}

    static UUID id(NetworkArchitecture architecture) {
        return UUID.nameUUIDFromBytes((WAVE + ":" + architecture.name()).getBytes(StandardCharsets.UTF_8));
    }
    static String candidate(NetworkArchitecture architecture) {
        return switch (architecture) { case BRN_GATED -> "W1-GATE"; case BRN_CUBIC -> "W1-CUBIC";
            case BRN_POOLED -> "W1-POOL"; default -> throw new IllegalArgumentException(); };
    }
    static List<Entry> prepare(Path base, DataSource source) throws IOException {
        base = base.toAbsolutePath().normalize();
        if (!Files.isDirectory(base, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Existing model library required: " + base);
        source.requireReady();
        var results = new ArrayList<Entry>();
        for (var architecture : ARCHITECTURES) {
            var implementation = architecture.trainingArchitecture();
            String incompatibility = TrainingDataLibrary.incompatibility(source, List.of(implementation));
            if (!incompatibility.isEmpty()) throw new IOException(incompatibility);
            UUID id = id(architecture);
            Path parent = base.resolve(architecture.folderName()), root = parent.resolve(id.toString());
            String name = candidate(architecture) + " " + architecture.toString();
            if (Files.isSymbolicLink(parent) || Files.isSymbolicLink(root)) throw new IOException("Linked wave lineage path refused: " + root);
            if (Files.notExists(root)) {
                Files.createDirectories(parent); Files.createDirectory(root);
                // Only this newly reserved directory can receive initialization writes.
                var settings = TrainingSettings.defaults(root, architecture).withLearningRate(.003);
                var lineage = new TrainingLineage(id, name, implementation, Instant.now(),
                        LineageConfiguration.encode(settings), WAVE + ": new independent material/zero-head initializer");
                try (var store = new CheckpointStore(root, implementation)) {
                    store.requireEmptyForBootstrap(); store.writeLineage(lineage);
                    new DataSources(1, List.of(source.withDisplay(source.name(), 1)), false).save(DataSources.directory(root));
                    store.writeTrainingSource(settings.source());
                    var checkpoint = store.initialize(NetworkTrainingState.initialized(implementation, settings.seed(), .003),
                            new CheckpointManifest.Metadata(0, settings.depth(), ""));
                    store.writeProvenance(new LineageProvenance(checkpoint.manifest().id(), implementation,
                            WAVE + ":" + implementation.nextKind().configurationId() + ": seeded zero head",
                            settings.seed(), settings.seed(), Instant.now()));
                }
            }
            // Idempotence never resets settings, optimizer, references, or source cursors after user training.
            var lineage = TrainingLineage.read(root).orElseThrow(() -> new IOException("Incomplete/foreign wave directory: " + root));
            if (!lineage.id().equals(id) || lineage.architecture() != implementation)
                throw new IOException("Wave identity collision: " + root);
            var selected = TrainingLineages.read(new TrainingLineages.Entry(root, architecture, lineage.name()));
            var provenance = LineageProvenance.read(root).orElseThrow(() -> new IOException("Wave initializer evidence missing: " + root));
            if (!provenance.initializer().startsWith(WAVE + ":") || provenance.initializationSeed() == null)
                throw new IOException("Wave initializer identity collision: " + root);
            var library = ModelLibrary.browse(ModelLibrary.entry(root, implementation));
            var initial = library.generations().stream().map(g -> g.checkpoint().manifest())
                    .filter(m -> m.id().equals(provenance.initialCheckpoint())).findFirst()
                    .orElseThrow(() -> new IOException("Wave initial manifest unavailable: " + root));
            var current = library.latest().orElseGet(() -> library.best().orElseThrow()).checkpoint().manifest();
            // Ordinary loader checks payload hashes and independently matches model with optimizer snapshot.
            CheckpointStore.readTrainingSnapshot(root, current.id());
            results.add(new Entry(candidate(architecture), lineage.name(), implementation.name(),
                    implementation.nextKind().configurationId(), root.toString(), id, initial.id(), initial.networkSha256(),
                    current.id(), current.networkSha256(), current.generation(), provenance.initializationSeed(),
                    selected.settings().recipe(), DataSources.read(DataSources.directory(root)).identity(), BrnNextPartition.ID));
        }
        return List.copyOf(results);
    }

    /** --inspect or --create REPORT; reads the existing GUI library setting, never changes preferences. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2 || !Set.of("--inspect", "--create").contains(args[0]))
            throw new IllegalArgumentException("Use --inspect or --create REPORT.json");
        boolean create = args[0].equals("--create");
        if (args.length != (create ? 2 : 1)) throw new IllegalArgumentException("Use --inspect or --create REPORT.json");
        Path report = create ? Path.of(args[1]).toAbsolutePath().normalize() : null;
        if (create && Files.exists(report)) throw new IOException("Preserve existing wave report: " + report);
        String configured = TrainingSettings.preferences().get("baseTrainingRoot", "");
        if (configured.isBlank()) throw new IOException("GUI model-library root is not configured");
        Path base = Path.of(configured).toAbsolutePath().normalize();
        var catalog = new TrainingDataLibrary(base).browse();
        if (!catalog.diagnostics().isEmpty()) throw new IOException("Training Data catalog diagnostics: " + catalog.diagnostics());
        // Reuse the registered CP source by stable identity; no implicit multi-source target mixture.
        var source = catalog.sources().stream().filter(s -> s.labelProfile() == LabelProfile.STOCKFISH_CP_MATE_V1)
                .filter(s -> TrainingDataLibrary.inspect(s).ready()).sorted(Comparator.comparing(DataSource::identity)).findFirst()
                .orElseThrow(() -> new IOException("No ready registered CP source; source selection needs review"));
        System.out.println("GUI library: " + base + "; source: " + source.name() + "; identity: " + source.identity());
        for (var a : ARCHITECTURES) System.out.println(candidate(a) + " -> " + base.resolve(a.folderName()).resolve(id(a).toString()));
        if (create) {
            var entries = prepare(base, source);
            DataFiles.write(report, new Report(WAVE, Instant.now().toString(), base.toString(), "prepare-existing-GUI-library",
                    source.location(), source.identity(), entries));
            System.out.println("Verified " + entries.size() + " GUI lineages; report: " + report);
        }
    }
    private BrnSuccessorWave() {}
}
