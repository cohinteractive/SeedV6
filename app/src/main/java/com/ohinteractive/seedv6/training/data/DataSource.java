package com.ohinteractive.seedv6.training.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Path-independent version fingerprint. Samples are identity evidence, not a full integrity checksum. */
public record DataSource(String name, Format format, String location, String identity, int weight,
                         LabelProfile labelProfile, List<Shard> shards) {
    public enum Format { LICHESS_JSONL, LICHESS_PZSTD, SEED_LEGACY, STOCKFISH_BINPACK_ZSTD }
    public record Shard(String name, String fingerprint, long bytes) {
        public Shard {
            if (name == null || name.isBlank() || !Path.of(name).getFileName().toString().equals(name)
                    || name.equals(".") || name.equals("..") || fingerprint == null || !fingerprint.matches("[0-9a-f]{64}") || bytes < 1)
                throw new IllegalArgumentException("Invalid shard inventory");
        }
    }
    public record Detection(Format format, List<Shard> shards) {}
    public DataSource {
        if (name == null || name.isBlank() || format == null || location == null || location.isBlank()
                || identity == null || !identity.matches("[0-9a-f]{64}") || weight < 1 || weight > 1000000)
            throw new IllegalArgumentException("Invalid Training Data source");
        if (labelProfile == null) labelProfile = LabelProfile.established(format);
        shards = shards == null ? List.of() : List.copyOf(shards);
        if (format == Format.STOCKFISH_BINPACK_ZSTD) {
            if (labelProfile != LabelProfile.BT4_Q_V1 || shards.isEmpty()
                    || !shards.stream().map(Shard::name).toList().equals(shards.stream().map(Shard::name).distinct().sorted().toList())
                    || !identity.equals(binpackIdentity(shards, labelProfile)))
                throw new IllegalArgumentException("Invalid BINP inventory/label identity");
        } else if (labelProfile != LabelProfile.STOCKFISH_CP_MATE_V1 || !shards.isEmpty())
            throw new IllegalArgumentException("Label profile does not match established source semantics");
    }
    /** Old descriptors and callers retain exactly their established identity and interpretation. */
    public DataSource(String name, Format format, String location, String identity, int weight) {
        this(name, format, location, identity, weight, null, null);
    }
    public DataSource withDisplay(String name, int weight) { return new DataSource(name, format, location, identity, weight, labelProfile, shards); }
    public Path path() { return Path.of(location); }
    public long knownPositions() throws IOException {
        return format == Format.SEED_LEGACY ? DataFiles.read(path().resolve("manifest.json"), com.ohinteractive.seedv6.corpus.CorpusManifest.class).positions() : -1;
    }
    public static DataSource register(String name, Path path, int weight) throws IOException {
        return register(name, path, weight, null);
    }
    public static DataSource register(String name, Path path, int weight, LabelProfile profile) throws IOException {
        path = path.toRealPath();
        Detection detected = detect(path);
        if (detected.format() == Format.STOCKFISH_BINPACK_ZSTD && profile == null)
            throw new IOException("Confirm an explicit label profile: BT4_Q_V1. BINP bytes do not identify their teacher.");
        try { return new DataSource(name, detected.format(), path.toString(), detected.format() == Format.STOCKFISH_BINPACK_ZSTD
                ? binpackIdentity(detected.shards(), profile) : fingerprint(path, detected.format()), weight, profile, detected.shards()); }
        catch (IllegalArgumentException invalid) { throw new IOException(invalid.getMessage(), invalid); }
    }
    public static Detection detect(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            if (Files.isRegularFile(path.resolve("index.sqlite"))) return new Detection(Format.SEED_LEGACY, List.of());
            var inventory = inventory(path);
            for (var shard : inventory) probe(path.resolve(shard.name()));
            return new Detection(Format.STOCKFISH_BINPACK_ZSTD, inventory);
        }
        String lower = path.toString().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jsonl")) return new Detection(Format.LICHESS_JSONL, List.of());
        if (lower.endsWith(".zst")) {
            try { SourceReaders.requirePzstd(path); return new Detection(Format.LICHESS_PZSTD, List.of()); }
            catch (IOException notPzstd) {
                try { probe(path); return new Detection(Format.STOCKFISH_BINPACK_ZSTD, inventory(path)); }
                catch (IOException notBinp) { throw new IOException("Unsupported Zstandard source: expected Lichess PZstandard or Stockfish BINP/Zstd. No conversion was started. " + notBinp.getMessage(), notBinp); }
            }
        }
        throw new IOException("Select Lichess JSONL/PZstandard, a legacy Seed directory, or Stockfish BINP/Zstd shards.");
    }
    static void probe(Path file) throws IOException {
        try (var bounded = new java.io.FilterInputStream(Files.newInputStream(file)) {
            long remaining = 128L * 1024 * 1024;
            public int read() throws IOException {
                if (remaining == 0) throw new IOException("BINP detection exceeded its 128 MiB compressed-input bound");
                int value = in.read(); if (value >= 0) remaining--; return value;
            }
            public int read(byte[] b, int off, int len) throws IOException {
                if (remaining == 0) throw new IOException("BINP detection exceeded its 128 MiB compressed-input bound");
                int n = in.read(b, off, (int) Math.min(len, remaining)); if (n > 0) remaining -= n; return n;
            }
        }; var input = new com.github.luben.zstd.ZstdInputStream(bounded)) {
            byte[] chunk = BinpackDecoder.readChunk(input);
            if (chunk == null) throw new IOException("Empty BINP source: " + file);
            var decoder = new BinpackDecoder(chunk);
            for (int n = 0; n < 64 && decoder.next() != null; n++) { /* bounded physical verification */ }
        }
    }
    private static List<Shard> inventory(Path path) throws IOException {
        List<Path> paths;
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) { paths = entries.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList(); }
        } else paths = List.of(path);
        if (paths.isEmpty()) throw new IOException("Empty source directory; expected legacy index.sqlite or BINP/Zstd shards");
        var result = new ArrayList<Shard>();
        for (Path file : paths) {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !file.toString().toLowerCase(Locale.ROOT).endsWith(".zst"))
                throw new IOException("Mixed/ambiguous BINP folder: unsupported entry " + file.getFileName() + ". Select a directory containing only compatible .zst shards.");
            result.add(new Shard(file.getFileName().toString(), fingerprint(file, Format.STOCKFISH_BINPACK_ZSTD), Files.size(file)));
        }
        return List.copyOf(result);
    }
    private static String binpackIdentity(List<Shard> shards, LabelProfile profile) {
        return DataFiles.hash(Format.STOCKFISH_BINPACK_ZSTD + ";" + profile + ";" + PreparedBinpack.RECIPE + ";" + shards);
    }
    public Path shardPath(Shard shard) { return Files.isDirectory(path()) ? path().resolve(shard.name()) : path(); }
    public void requireReady() throws IOException {
        verify();
        if (format == Format.STOCKFISH_BINPACK_ZSTD) PreparedBinpack.ready(this);
    }
    public void verify() throws IOException {
        if (!identity.equals(format == Format.STOCKFISH_BINPACK_ZSTD ? binpackIdentity(inventory(path()), labelProfile) : fingerprint(path(), format)))
            throw new IOException("Training Data source changed: " + name + ". Its saved cursor cannot be used with this version.");
    }
    static String fingerprint(Path path, Format format) throws IOException {
        var hash = DataFiles.digest();
        hash.update(format.name().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (format == Format.SEED_LEGACY) {
            sample(path.resolve("index.sqlite"), hash);
            sample(path.resolve("manifest.json"), hash);
        } else sample(path, hash);
        return HexFormat.of().formatHex(hash.digest());
    }
    private static void sample(Path file, java.security.MessageDigest digest) throws IOException {
        var before = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class);
        if (!before.isRegularFile()) throw new IOException("Not a source file: " + file);
        digest.update(ByteBuffer.allocate(16).putLong(before.size()).putLong(before.lastModifiedTime().toMillis()).array());
        // At most 256 KiB, independent of source size. Preserve timestamps when relocating sources.
        try (var channel = FileChannel.open(file)) {
            ByteBuffer buffer = ByteBuffer.allocate(65536);
            for (long offset : new long[]{0, before.size() / 3, before.size() * 2 / 3, Math.max(0, before.size() - 65536)}) {
                buffer.clear(); buffer.limit((int) Math.min(buffer.capacity(), before.size() - offset));
                channel.position(offset);
                while (buffer.hasRemaining() && channel.read(buffer) >= 0) {}
                digest.update(buffer.array(), 0, buffer.position());
            }
        }
        var after = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class);
        if (before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime()))
            throw new IOException("Source changed while checking its identity: " + file);
    }
}
