package com.ohinteractive.seedv6.training.data;

import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.service.CorpusTraining;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Application catalog of existing descriptors. Mix weights and consumption ledgers remain per lineage. */
public final class TrainingDataLibrary {
    public record Catalog(List<DataSource> sources, List<String> diagnostics) {
        public Catalog { sources = List.copyOf(sources); diagnostics = List.copyOf(diagnostics); }
    }
    public record Readiness(boolean ready, long positions, String detail) {}
    private final Path directory;
    public TrainingDataLibrary(Path modelLibrary) { directory = modelLibrary.toAbsolutePath().normalize().resolve("training-data-library"); }
    public Path directory() { return directory; }
    public Catalog browse() throws IOException {
        if (Files.notExists(directory)) return new Catalog(List.of(), List.of());
        var sources = new ArrayList<DataSource>(); var diagnostics = new ArrayList<String>();
        try (var paths = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : paths) {
                try {
                    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Not a regular source registration");
                    DataSource source = DataFiles.read(file, DataSource.class);
                    if (!file.getFileName().toString().equals(source.identity() + ".json")) throw new IOException("Source registration identity mismatch");
                    sources.add(source);
                } catch (IOException invalid) { diagnostics.add(file.getFileName() + ": " + invalid.getMessage()); }
            }
        }
        sources.sort(Comparator.comparing(DataSource::name, String.CASE_INSENSITIVE_ORDER).thenComparing(DataSource::identity));
        diagnostics.sort(String::compareTo); return new Catalog(sources, diagnostics);
    }
    /** Additive adoption of saved selections. Never rewrites a campaign descriptor or reinterprets its identity. */
    public DataSource remember(DataSource source) throws IOException { return write(source, false); }
    /** Explicit registration/relocation verifies the backing version before replacing catalog metadata. */
    public DataSource register(DataSource source) throws IOException { source.verify(); return write(source, true); }
    private DataSource write(DataSource source, boolean replace) throws IOException {
        Files.createDirectories(directory);
        try (var channel = FileChannel.open(directory.resolve("catalog.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            java.nio.channels.FileLock lock;
            try { lock = channel.tryLock(); }
            catch (java.nio.channels.OverlappingFileLockException busy) { throw new IOException("Training Data library is being updated; retry.", busy); }
            if (lock == null) throw new IOException("Training Data library is being updated; retry.");
            try (lock) {
                Path file = directory.resolve(source.identity() + ".json");
                if (Files.exists(file)) {
                    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid source registration");
                    var previous = DataFiles.read(file, DataSource.class);
                    if (!previous.identity().equals(source.identity())) throw new IOException("Source registration identity mismatch");
                    if (!replace) return previous;
                }
                var value = source.withDisplay(source.name(), 1);
                DataFiles.write(file, value); return value;
            }
        }
    }
    public static Readiness inspect(DataSource source) {
        try {
            source.requireReady();
            long count = source.format() == DataSource.Format.STOCKFISH_BINPACK_ZSTD
                    ? PreparedBinpack.ready(source).count() : source.knownPositions();
            return new Readiness(true, count, "Ready" + (count < 0 ? " · position count not indexed" : " · " + count + " raw positions"));
        } catch (Exception invalid) { return new Readiness(false, -1, "Not ready: " + invalid.getMessage()); }
    }
    public static String incompatibility(DataSource source, Collection<TrainingArchitecture> architectures) {
        var reasons = new ArrayList<String>();
        for (var architecture : new LinkedHashSet<>(architectures)) {
            try { CorpusTraining.targetPolicy(architecture, source.labelProfile()); }
            catch (IllegalArgumentException invalid) { reasons.add(architecture.displayName() + ": " + invalid.getMessage()); }
        }
        return String.join("; ", reasons);
    }
}
