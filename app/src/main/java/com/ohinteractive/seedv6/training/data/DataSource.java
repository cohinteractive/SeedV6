package com.ohinteractive.seedv6.training.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Path-independent version fingerprint. Samples are identity evidence, not a full integrity checksum. */
public record DataSource(String name, Format format, String location, String identity, int weight) {
    public enum Format { LICHESS_JSONL, LICHESS_PZSTD, SEED_LEGACY }
    public DataSource {
        if (name == null || name.isBlank() || format == null || location == null || location.isBlank()
                || identity == null || !identity.matches("[0-9a-f]{64}") || weight < 1 || weight > 1000000)
            throw new IllegalArgumentException("Invalid Training Data source");
    }
    public Path path() { return Path.of(location); }
    public long knownPositions() throws IOException {
        return format == Format.SEED_LEGACY ? DataFiles.read(path().resolve("manifest.json"), com.ohinteractive.seedv6.corpus.CorpusManifest.class).positions() : -1;
    }
    public static DataSource register(String name, Path path, int weight) throws IOException {
        path = path.toRealPath();
        Format format = Files.isDirectory(path) ? Format.SEED_LEGACY
                : path.toString().toLowerCase(Locale.ROOT).endsWith(".zst") ? Format.LICHESS_PZSTD : Format.LICHESS_JSONL;
        if (format == Format.LICHESS_JSONL && !path.toString().toLowerCase(Locale.ROOT).endsWith(".jsonl"))
            throw new IOException("Unsupported Training Data format. Select a Lichess .jsonl / framed .jsonl.zst source or a legacy Seed data directory; native Stockfish binpack needs a source reader.");
        if (format == Format.LICHESS_PZSTD) SourceReaders.requirePzstd(path);
        if (format == Format.SEED_LEGACY && !Files.isRegularFile(path.resolve("index.sqlite")))
            throw new IOException("Select a legacy Seed Training Data directory containing index.sqlite");
        return new DataSource(name, format, path.toString(), fingerprint(path, format), weight);
    }
    public void verify() throws IOException {
        if (!identity.equals(fingerprint(path(), format)))
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
