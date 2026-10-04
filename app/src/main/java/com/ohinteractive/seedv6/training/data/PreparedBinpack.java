package com.ohinteractive.seedv6.training.data;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/** Shared, immutable derived BINP chunks. Only an atomically published manifest is READY. */
public final class PreparedBinpack {
    public static final String RECIPE = "binp-chunks-zstd3-roundrobin-v1";
    public record Unit(int shard, int chunk, long start, long count, long offset, int compressed, int decoded, String hash) {
        public Unit {
            if (shard < 0 || chunk < 0 || start < 0 || count < 1 || offset < 0 || compressed < 1
                    || decoded < 42 || decoded > BinpackDecoder.MAX_CHUNK + 8 || hash == null || !hash.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("Invalid prepared BINP unit");
        }
    }
    public record Manifest(String source, String recipe, LabelProfile profile, List<DataSource.Shard> shards,
                           List<Unit> units, long count, String identity) {
        public Manifest {
            if (!RECIPE.equals(recipe) || profile == null || shards == null || shards.isEmpty() || units == null || units.isEmpty()
                    || !Objects.equals(identity, manifestHash(source, profile, shards, units, count)))
                throw new IllegalArgumentException("Invalid prepared BINP manifest/checksum");
            shards = List.copyOf(shards); units = List.copyOf(units);
            long next = 0; long[] offsets = new long[shards.size()]; int[] chunks = new int[shards.size()];
            int lastChunk = -1, lastShard = -1;
            for (Unit unit : units) {
                if (unit.shard() >= shards.size() || unit.start() != next || unit.offset() != offsets[unit.shard()]
                        || unit.chunk() != chunks[unit.shard()]++ || unit.chunk() < lastChunk
                        || unit.chunk() == lastChunk && unit.shard() <= lastShard)
                    throw new IllegalArgumentException("Invalid prepared BINP unit ordering");
                next = Math.addExact(next, unit.count()); offsets[unit.shard()] = Math.addExact(unit.offset(), unit.compressed());
                lastChunk = unit.chunk(); lastShard = unit.shard();
            }
            if (count != next || Arrays.stream(chunks).anyMatch(n -> n == 0)) throw new IllegalArgumentException("Invalid prepared BINP count");
        }
    }
    private static String manifestHash(String source, LabelProfile profile, List<DataSource.Shard> shards, List<Unit> units, long count) {
        return DataFiles.hash(source + ";" + RECIPE + ";" + profile + ";" + shards + ";" + units + ";" + count);
    }
    public static Path root() {
        String override = System.getProperty("seedv6.preparedDataRoot");
        if (override != null && !override.isBlank()) return Path.of(override).toAbsolutePath().normalize();
        String local = System.getenv("LOCALAPPDATA");
        return (local == null || local.isBlank() ? Path.of(System.getProperty("user.home"), ".seedv6-nnue")
                : Path.of(local, "SeedV6-NNUE")).resolve("prepared-data");
    }
    public static Path directory(DataSource source) { return root().resolve(source.identity()); }
    private static Path payload(Path directory, int shard) { return directory.resolve("shard-" + shard + ".zst"); }
    public static Manifest ready(DataSource source) throws IOException {
        if (source.format() != DataSource.Format.STOCKFISH_BINPACK_ZSTD) throw new IOException("Not a BINP source");
        try {
            Manifest manifest = DataFiles.read(directory(source).resolve("manifest.json"), Manifest.class);
            if (!manifest.source().equals(source.identity()) || manifest.profile() != source.labelProfile() || !manifest.shards().equals(source.shards()))
                throw new IOException("Prepared source binding mismatch");
            long[] sizes = new long[source.shards().size()];
            for (Unit unit : manifest.units()) sizes[unit.shard()] += unit.compressed();
            for (int i = 0; i < sizes.length; i++) if (Files.size(payload(directory(source), i)) != sizes[i]) throw new IOException("Prepared shard size mismatch");
            return manifest;
        } catch (IOException | RuntimeException invalid) {
            throw new IOException("Source " + source.name() + " is not READY: preparation is missing, incomplete or invalid. Use Training Data > Prepare / retry. " + invalid.getMessage(), invalid);
        }
    }
    public static Manifest prepare(DataSource source, CorpusPreparation control) throws IOException {
        return prepare(source, control, false);
    }
    /** Explicit retry verifies existing units; ordinary registration reuses a published unchanged cache immediately. */
    public static Manifest prepare(DataSource source, CorpusPreparation control, boolean verifyExisting) throws IOException {
        source.verify(); Files.createDirectories(root());
        Path lockPath = root().resolve(source.identity() + ".lock");
        try (var channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            FileLock lock;
            try { lock = channel.tryLock(); } catch (OverlappingFileLockException busy) { throw new IOException("This source is already preparing; inspect it when the other worker finishes", busy); }
            if (lock == null) throw new IOException("This source is preparing in another SeedV6 process");
            try (lock) {
                try { Manifest existing = ready(source); if (verifyExisting) verifyUnits(source, existing, control); return existing; }
                catch (CorpusPreparation.Cancelled cancelled) { throw cancelled; }
                catch (IOException invalid) { /* incomplete/corrupt derived data is rebuilt from unchanged originals */ }
                Path target = directory(source), pending = root().resolve(source.identity() + ".pending");
                // Both directories are strictly application-owned. Never touch source archives.
                removeDerived(pending);
                Files.createDirectory(pending);
                try {
                    var units = new ArrayList<Unit>(); long totalBytes = source.shards().stream().mapToLong(DataSource.Shard::bytes).sum(), completedBytes = 0;
                    for (int shard = 0; shard < source.shards().size(); shard++) {
                        var original = source.shards().get(shard);
                        try (var raw = new CountingInput(Files.newInputStream(source.shardPath(original)));
                             var input = new ZstdInputStream(raw);
                             var output = FileChannel.open(payload(pending, shard), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                            int chunkIndex = 0;
                            for (byte[] chunk; (chunk = BinpackDecoder.readChunk(input)) != null;) {
                                control.checkCancelled(); var decoder = new BinpackDecoder(chunk); long count = 0;
                                while (decoder.next() != null) { if ((++count & 4095) == 0) control.checkCancelled(); }
                                byte[] nativeChunk = ByteBuffer.allocate(chunk.length + 8).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                                        .putInt(0x504e4942).putInt(chunk.length).put(chunk).array();
                                byte[] compressed = Zstd.compress(nativeChunk, 3); long offset = output.position();
                                ByteBuffer bytes = ByteBuffer.wrap(compressed); while (bytes.hasRemaining()) output.write(bytes);
                                units.add(new Unit(shard, chunkIndex++, 0, count, offset, compressed.length, nativeChunk.length, digest(nativeChunk)));
                                control.report("Preparing " + source.name() + ": shard " + (shard + 1) + "/" + source.shards().size() + ", " + chunkIndex + " chunks", completedBytes + raw.count, totalBytes);
                            }
                            if (chunkIndex == 0) throw new IOException("Empty BINP shard: " + original.name());
                            output.force(true);
                        }
                        completedBytes += original.bytes();
                    }
                    units.sort(Comparator.comparingInt(Unit::chunk).thenComparingInt(Unit::shard));
                    long count = 0;
                    for (int i = 0; i < units.size(); i++) {
                        Unit u = units.get(i); units.set(i, new Unit(u.shard(), u.chunk(), count, u.count(), u.offset(), u.compressed(), u.decoded(), u.hash()));
                        count = Math.addExact(count, u.count());
                    }
                    source.verify(); control.checkCancelled();
                    Manifest manifest = new Manifest(source.identity(), RECIPE, source.labelProfile(), source.shards(), units, count,
                            manifestHash(source.identity(), source.labelProfile(), source.shards(), units, count));
                    DataFiles.write(pending.resolve("manifest.json"), manifest);
                    // Retire the old publication before replacing it. A crash here leaves NOT READY, never a partial READY.
                    removeDerived(target);
                    Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE);
                    control.report("READY: " + source.name() + " (" + count + " raw positions)", totalBytes, totalBytes);
                    return ready(source);
                } catch (IOException | RuntimeException failure) { removeDerived(pending); throw failure; }
            }
        }
    }
    private static void removeDerived(Path directory) throws IOException {
        Path normalized = directory.toAbsolutePath().normalize();
        if (!normalized.getParent().equals(root().toAbsolutePath().normalize()) || !normalized.getFileName().toString().matches("[0-9a-f]{64}(\\.pending)?"))
            throw new IOException("Unsafe prepared-data path");
        if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(normalized)) throw new IOException("Prepared directory must not be a symbolic link");
        try (var entries = Files.walk(normalized)) {
            for (Path path : entries.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
    private static final class CountingInput extends FilterInputStream {
        long count;
        CountingInput(InputStream input) { super(input); }
        public int read() throws IOException { int b = in.read(); if (b >= 0) count++; return b; }
        public int read(byte[] b, int off, int length) throws IOException { int n = in.read(b, off, length); if (n > 0) count += n; return n; }
    }
    private static String digest(byte[] bytes) { return HexFormat.of().formatHex(DataFiles.digest().digest(bytes)); }
    private static byte[] readUnit(Path directory, Unit unit) throws IOException {
        if (unit.compressed() > Zstd.compressBound(unit.decoded())) throw new IOException("Invalid prepared unit length");
        byte[] compressed = new byte[unit.compressed()];
        try (var file = FileChannel.open(payload(directory, unit.shard()))) {
            file.position(unit.offset()); ByteBuffer bytes = ByteBuffer.wrap(compressed);
            while (bytes.hasRemaining()) if (file.read(bytes) < 0) throw new EOFException("Truncated prepared BINP unit");
        }
        byte[] decoded = new byte[unit.decoded()]; long result;
        try { result = Zstd.decompress(decoded, compressed); }
        catch (RuntimeException corrupt) { throw new IOException("Prepared BINP decompression failed; use Training Data > Prepare / retry", corrupt); }
        if (Zstd.isError(result) || result != decoded.length || !digest(decoded).equals(unit.hash()))
            throw new IOException("Prepared BINP checksum failed; use Training Data > Prepare / retry");
        var input = new ByteArrayInputStream(decoded); byte[] chunk = BinpackDecoder.readChunk(input);
        if (input.available() != 0) throw new IOException("Invalid prepared chunk boundary");
        return chunk;
    }
    private static void verifyUnits(DataSource source, Manifest manifest, CorpusPreparation control) throws IOException {
        long done = 0;
        for (Unit unit : manifest.units()) { control.checkCancelled(); readUnit(directory(source), unit); control.report("Checking prepared " + source.name(), ++done, manifest.units().size()); }
    }
    public static PositionReader open(DataSource source, long start) throws IOException {
        return new Reader(source, ready(source), start);
    }
    private static final class Reader implements PositionReader {
        private final DataSource source;
        private final Manifest manifest;
        private int unitIndex;
        private long position, decoded, seek;
        private BinpackDecoder decoder;
        Reader(DataSource source, Manifest manifest, long start) throws IOException {
            this.source = source; this.manifest = manifest;
            if (start < 0 || start > manifest.count()) throw new EOFException("BINP source exhausted before " + start);
            int low = 0, high = manifest.units().size();
            while (low < high) { int mid = (low + high) >>> 1; Unit u = manifest.units().get(mid); if (u.start() + u.count() <= start) low = mid + 1; else high = mid; }
            unitIndex = low;
            position = unitIndex == manifest.units().size() ? manifest.count() : manifest.units().get(unitIndex).start();
            while (position < start) { if (next() == null) throw new EOFException("Missing prepared ordinal"); seek++; }
            decoded = 0;
        }
        public Entry next() throws IOException {
            if (position == manifest.count()) return null;
            Unit unit = manifest.units().get(unitIndex);
            if (decoder == null) decoder = new BinpackDecoder(readUnit(directory(source), unit));
            var record = decoder.next();
            if (record == null) throw new IOException("Prepared BINP record count mismatch");
            Entry entry = new Entry(position++, record, ""); decoded++;
            if (position == unit.start() + unit.count()) {
                if (decoder.next() != null) throw new IOException("Prepared BINP record count mismatch");
                decoder = null; unitIndex++;
            }
            return entry;
        }
        public long nextPosition() { return position; }
        public long decodedRecords() { return decoded; }
        public long seekRecords() { return seek; }
        public void close() throws IOException { source.verify(); }
    }
    private PreparedBinpack() {}
}
