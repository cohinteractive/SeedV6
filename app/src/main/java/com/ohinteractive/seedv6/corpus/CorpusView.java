package com.ohinteractive.seedv6.corpus;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import com.google.gson.Gson;

/** Immutable, unshuffled disk index of logical records at a reader snapshot.
 * Only two longs per retained record are copied, never positions, labels or features.
 * Shard hashes bind the original labels even after catalog pointers are upgraded.
 */
public final class CorpusView implements AutoCloseable {
    private static final Gson JSON = new Gson();
    public record Descriptor(int version, String policy, CorpusManifest manifest, long retained,
            Map<String, Long> excluded, List<CorpusReader.Shard> shards, String indexHash) {}
    private final Descriptor descriptor;
    private final String identity;
    private final Path root;
    private final FileChannel index;
    private final Map<Long, CorpusReader.Shard> shards = new HashMap<>();
    private final LinkedHashMap<Long, FileChannel> channels = new LinkedHashMap<>(16, .75f, true);
    private final ByteBuffer pointer = ByteBuffer.allocate(16), bytes = ByteBuffer.allocate(CorpusRecord.BYTES);

    public static void create(CorpusReader reader, Path directory, String policy,
            Function<CorpusRecord, String> rejection) throws IOException, SQLException {
        create(reader, directory, policy, rejection, Long.MAX_VALUE);
    }

    /** Diagnostic-only bounded prefix of the real snapshot. Unexamined identities are explicit
     * exclusions, and only visited immutable shards are bound. Default campaign views remain full.
     * Uses the same version-1 descriptor, disk index, integrity checks and selection substrate.
     */
    public static void create(CorpusReader reader, Path directory, String policy,
            Function<CorpusRecord, String> rejection, long maximumRecords) throws IOException, SQLException {
        create(reader, directory, policy, rejection, maximumRecords, CorpusPreparation.NONE);
    }

    public static void create(CorpusReader reader, Path directory, String policy,
            Function<CorpusRecord, String> rejection, long maximumRecords, CorpusPreparation preparation) throws IOException, SQLException {
        if (maximumRecords < 1) throw new IllegalArgumentException("Invalid view record limit");
        preparation.checkCancelled();
        Files.createDirectories(directory);
        // Trainers also hold store.lock. This lease protects direct view creators across JVMs.
        // Keep the coordination file: deleting it could let callers lock different file identities.
        try (var channel = FileChannel.open(directory.resolve("preparation.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            FileLock acquired;
            try { acquired = channel.tryLock(); }
            catch (OverlappingFileLockException conflict) { throw new IOException("Corpus view preparation already has an owner: " + directory, conflict); }
            if (acquired == null) throw new IOException("Corpus view preparation already has an owner: " + directory);
            try (acquired) { createOwned(reader, directory, policy, rejection, maximumRecords, preparation); }
        }
    }

    private static void createOwned(CorpusReader reader, Path directory, String policy,
            Function<CorpusRecord, String> rejection, long maximumRecords, CorpusPreparation preparation) throws IOException, SQLException {
        preparation.checkCancelled();
        Path published = directory.resolve("view.json"), index = directory.resolve("records.idx");
        Path pending = directory.resolve("records.idx.pending"), metadata = directory.resolve("view.json.pending");
        if (Files.exists(published))
            throw new IOException("Refusing to replace a pinned corpus view: " + directory);
        if (Files.exists(index)) {
            // A crash between the two atomic moves leaves a complete index and its descriptor.
            // Finish publication only after the usual descriptor/index checks, without rebuilding
            // or replacing the final index. Unknown or corrupt final state remains fail-closed.
            if (!Files.exists(metadata)) throw new IOException("Missing descriptor for existing corpus index: " + directory);
            verifyIndex(directory, readDescriptor(metadata, policy), preparation);
            preparation.checkCancelled();
            Files.move(metadata, published, StandardCopyOption.ATOMIC_MOVE);
            Files.deleteIfExists(pending);
            return;
        }
        // Neither file is durable or resumable without a published view. The exclusive lease
        // proves these fixed-name scratch files cannot belong to another participating creator.
        Files.deleteIfExists(pending);
        Files.deleteIfExists(metadata);
        var manifest = reader.manifest();
        long total = Math.min(manifest.positions(), maximumRecords);
        preparation.report("Examining corpus positions", 0, total);
        long[] retained = {0}, examined = {0}; var excluded = new TreeMap<String, Long>();
        var visitedShards = new HashSet<Long>();
        try {
            var stream = Files.newOutputStream(pending, StandardOpenOption.CREATE_NEW);
            try (var out = new DataOutputStream(new BufferedOutputStream(stream))) {
                reader.forEachLocated((shard, ordinal, record) -> {
                    preparation.checkCancelled();
                    examined[0]++; visitedShards.add(shard);
                    String reason = rejection.apply(record);
                    if (reason == null) { out.writeLong(shard); out.writeLong(ordinal); retained[0]++; }
                    else excluded.merge(reason, 1L, Long::sum);
                    if (examined[0] == 1 || (examined[0] & 1023) == 0)
                        preparation.report("Examining corpus positions", examined[0], total);
                }, maximumRecords);
            }
            preparation.report("Examining corpus positions", examined[0], total);
            try (FileChannel f = FileChannel.open(pending, StandardOpenOption.WRITE)) { f.force(true); }
            if (examined[0] < manifest.positions()) excluded.put("diagnostic-unexamined", manifest.positions() - examined[0]);
            var boundShards = maximumRecords == Long.MAX_VALUE ? reader.shards()
                    : reader.shards().stream().filter(s -> visitedShards.contains(s.id())).toList();
            long indexBytes = Files.size(pending);
            preparation.report("Hashing corpus index bytes", 0, indexBytes);
            var descriptor = new Descriptor(1, policy, manifest, retained[0], excluded, boundShards,
                    CorpusCatalog.sha256(pending, preparation, "Hashing corpus index bytes", 0, indexBytes));
            Files.writeString(metadata, JSON.toJson(descriptor), StandardOpenOption.CREATE_NEW);
            try (FileChannel f = FileChannel.open(metadata, StandardOpenOption.WRITE)) { f.force(true); }
            preparation.checkCancelled();
            // Publish the complete pair without a cancellation boundary between the atomic moves.
            Files.move(pending, index, StandardCopyOption.ATOMIC_MOVE);
            Files.move(metadata, published, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | SQLException | RuntimeException | Error failure) {
            removeScratch(pending, failure);
            // Preserve the completed pair if index publication succeeded but metadata publication
            // failed. The next owner can verify and finish it through the branch above.
            if (Files.notExists(index)) removeScratch(metadata, failure);
            throw failure;
        }
    }

    private static void removeScratch(Path path, Throwable failure) {
        try { Files.deleteIfExists(path); }
        catch (IOException cleanup) { failure.addSuppressed(cleanup); }
    }

    private static Descriptor readDescriptor(Path metadata, String policy) throws IOException {
        Descriptor descriptor;
        try { descriptor = JSON.fromJson(Files.readString(metadata), Descriptor.class); }
        catch (RuntimeException invalid) { throw new IOException("Invalid pinned corpus metadata", invalid); }
        if (descriptor == null || descriptor.version() != 1 || !policy.equals(descriptor.policy())
                || descriptor.retained() < 0 || descriptor.retained() > descriptor.manifest().positions()
                || descriptor.excluded().values().stream().anyMatch(n -> n < 0)
                || descriptor.retained() + descriptor.excluded().values().stream().mapToLong(Long::longValue).sum() != descriptor.manifest().positions())
            throw new IOException("Invalid pinned corpus view/policy");
        return descriptor;
    }

    private static Path verifyIndex(Path directory, Descriptor descriptor, CorpusPreparation preparation) throws IOException {
        Path indexPath = directory.resolve("records.idx");
        long indexBytes = Math.multiplyExact(16L, descriptor.retained());
        preparation.report("Verifying corpus index bytes", 0, indexBytes);
        if (Files.size(indexPath) != indexBytes
                || !CorpusCatalog.sha256(indexPath, preparation, "Verifying corpus index bytes", 0, indexBytes).equals(descriptor.indexHash())) throw new IOException("Pinned corpus index changed");
        return indexPath;
    }

    public CorpusView(Path root, Path directory, String policy) throws IOException, SQLException {
        this(root, directory, policy, CorpusPreparation.NONE);
    }
    public CorpusView(Path root, Path directory, String policy, CorpusPreparation preparation) throws IOException, SQLException {
        preparation.checkCancelled();
        this.root = root.toAbsolutePath().normalize();
        descriptor = readDescriptor(directory.resolve("view.json"), policy);
        Path indexPath = verifyIndex(directory, descriptor, preparation);
        // One verification per campaign open, never per generation. Compatible appends/upgrades
        // may change the catalog, but every originally pinned immutable shard must still exist.
        try (CorpusReader reader = new CorpusReader(this.root)) {
            Map<Long, CorpusReader.Shard> current = new HashMap<>();
            for (var shard : reader.shards()) current.put(shard.id(), shard);
            long totalBytes = 0, verifiedBytes = 0;
            for (var shard : descriptor.shards()) totalBytes = Math.addExact(totalBytes,
                    Math.addExact(CorpusCatalog.HEADER_BYTES, Math.multiplyExact(shard.records(), CorpusRecord.BYTES)));
            preparation.report("Verifying corpus shard bytes", 0, totalBytes);
            for (var shard : descriptor.shards()) {
                if (!shard.equals(current.get(shard.id())) || shards.put(shard.id(), shard) != null
                        || !CorpusCatalog.sha256(CorpusCatalog.shardPath(this.root, shard.file()), preparation,
                                "Verifying corpus shard bytes", verifiedBytes, totalBytes).equals(shard.sha256()))
                    throw new IOException("Pinned corpus shard missing or changed: " + shard.file());
                verifiedBytes += CorpusCatalog.HEADER_BYTES + shard.records() * CorpusRecord.BYTES;
            }
        }
        preparation.checkCancelled();
        identity = java.util.HexFormat.of().formatHex(CorpusCatalog.digest().digest(JSON.toJson(descriptor).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        index = FileChannel.open(indexPath);
    }
    public Descriptor descriptor() { return descriptor; }
    public String identity() { return identity; }
    public long size() { return descriptor.retained(); }

    /** O(1) address lookup; at most eight shard channels and two small byte buffers. */
    public CorpusRecord record(long logicalIndex) throws IOException {
        if (logicalIndex < 0 || logicalIndex >= size()) throw new IndexOutOfBoundsException();
        pointer.clear(); CorpusCatalog.readFully(index, pointer, Math.multiplyExact(logicalIndex, 16)); pointer.flip();
        long id = pointer.getLong(), ordinal = pointer.getLong();
        var shard = shards.get(id);
        if (shard == null || ordinal < 0 || ordinal >= shard.records()) throw new IOException("Invalid pinned corpus address");
        FileChannel channel = channels.get(id);
        if (channel == null) {
            if (channels.size() == 8) { var oldest = channels.entrySet().iterator(); var entry = oldest.next(); entry.getValue().close(); oldest.remove(); }
            channel = FileChannel.open(CorpusCatalog.shardPath(root, shard.file()));
            CorpusCatalog.checkHeader(channel, id, shard.records()); channels.put(id, channel);
        }
        bytes.clear(); CorpusCatalog.readFully(channel, bytes, CorpusCatalog.HEADER_BYTES + ordinal * CorpusRecord.BYTES);
        try { return CorpusRecord.decode(bytes.array()); }
        catch (IllegalArgumentException invalid) { throw new IOException("Invalid pinned corpus record", invalid); }
    }
    @Override public void close() throws IOException {
        try { for (var channel : channels.values()) channel.close(); }
        finally { index.close(); }
    }
}
