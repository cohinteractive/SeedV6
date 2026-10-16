package com.ohinteractive.seedv6.corpus.lichess;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import com.github.luben.zstd.ZstdInputStream;
import com.google.gson.Gson;
import com.ohinteractive.seedv6.corpus.*;

/** Streams from byte zero on each pass; no arbitrary compressed seeking/resume claim. */
public final class LichessImporter {
    public static final int DEFAULT_SHARD_SIZE = 100_000;
    public static final long DEFAULT_PROGRESS_EVERY = 100_000;
    public record Options(Path input, Path corpus, long maxRecords, int shardSize, long progressEvery) {
        public Options {
            if (maxRecords < 0 || progressEvery < 1 || shardSize < 1 || shardSize > 1_000_000)
                throw new IllegalArgumentException("Invalid import bounds");
        }
    }
    public record Summary(CorpusWriter.Stats stats, long corpusTotal, double elapsedSeconds, boolean stopped) {
        public Summary(CorpusWriter.Stats stats, long corpusTotal, double elapsedSeconds) {
            this(stats, corpusTotal, elapsedSeconds, false);
        }
    }
    /** Counters include the current batch; corpusTotal counts only committed logical positions. */
    public record Progress(CorpusWriter.Stats stats, long corpusTotal, double elapsedSeconds) {}
    private record Provenance(String path, long compressedBytes, long modifiedMillis,
                              String firstMiBCompressedSha256, String sourceUrl, String engine) {}
    private LichessImporter() {}

    public static Summary run(Options options, PrintStream out) throws IOException, SQLException {
        return run(options, out, progress -> {}, () -> false);
    }

    /** Same streaming implementation for CLI and GUI. Stop cooperates between source records,
     * then commits the current batch through the normal checkpoint protocol; never interrupts I/O. */
    public static Summary run(Options options, PrintStream out, Consumer<Progress> progress,
                              BooleanSupplier stopRequested) throws IOException, SQLException {
        long start = System.nanoTime();
        Path input = options.input().toRealPath();
        BasicFileAttributes before = Files.readAttributes(input, BasicFileAttributes.class);
        String prefix = prefixHash(input);
        // This is an observed file fingerprint, not a claimed full-archive checksum.
        String metadata = new Gson().toJson(new Provenance(input.toString(), before.size(), before.lastModifiedTime().toMillis(),
                prefix, "https://database.lichess.org/#evals", "Stockfish variants; exact engine version unavailable in export"));
        CorpusWriter.SourceInfo source = new CorpusWriter.SourceInfo(LichessDecoder.ADAPTER,
                LichessDecoder.ADAPTER + ":" + input + ":" + before.size() + ":" + before.lastModifiedTime().toMillis() + ":" + prefix,
                metadata, LichessDecoder.POLICY);
        out.println("Streaming from source start; restart rescans, exact corpus identity prevents inflation. max-records=0 means unbounded.");
        CorpusWriter.Stats stats;
        boolean stopped = false;
        try (CorpusWriter writer = new CorpusWriter(options.corpus(), options.shardSize(), source);
             InputStream raw = new BufferedInputStream(Files.newInputStream(input), 131072);
             ZstdInputStream zstd = new ZstdInputStream(raw);
             Reader decoded = new InputStreamReader(zstd, StandardCharsets.UTF_8.newDecoder()
                     .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                     .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT))) {
            BoundedLines lines = new BoundedLines(decoded);
            long initialTotal;
            try (CorpusReader reader = new CorpusReader(options.corpus())) { initialTotal = reader.manifest().positions(); }
            progress.accept(new Progress(writer.stats(), initialTotal, (System.nanoTime() - start) / 1e9));
            long lastProgress = System.nanoTime();
            long line = 0;
            while (options.maxRecords() == 0 || line < options.maxRecords()) {
                if (stopRequested.getAsBoolean()) { stopped = true; break; }
                String json = lines.next();
                if (json == null) break;
                line++;
                CorpusRecord record = null;
                try {
                    if (lines.tooLong) throw new IllegalArgumentException("JSONL line exceeds 1 MiB characters");
                    record = LichessDecoder.decode(json, writer.sourceId(), line);
                } catch (IllegalArgumentException ex) {
                    if (writer.stats().rejected() < 3) out.println("Rejected source line " + line + ": " + ex.getMessage());
                }
                // Storage errors must abort, never masquerade as rejected source records.
                if (record == null) writer.reject(); else writer.put(record);
                if (line % options.progressEvery() == 0) report(out, writer.stats(), start);
                long now = System.nanoTime();
                if (line % options.progressEvery() == 0 || (line % 1024 == 0 && now - lastProgress >= 1_000_000_000L)) {
                    progress.accept(new Progress(writer.stats(), initialTotal + writer.committedAdded(), (now - start) / 1e9));
                    lastProgress = now;
                }
            }
            BasicFileAttributes after = Files.readAttributes(input, BasicFileAttributes.class);
            if (before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime()))
                throw new IOException("Source changed during import; completed checkpoints remain durable");
            writer.checkpoint(!stopped);
            stats = writer.stats();
        }
        long total;
        try (CorpusReader reader = new CorpusReader(options.corpus())) { total = reader.manifest().positions(); }
        report(out, stats, start);
        double seconds = (System.nanoTime() - start) / 1e9;
        progress.accept(new Progress(stats, total, seconds));
        out.println((stopped ? "Import stopped: corpus-total=" : "Import complete: corpus-total=") + total);
        return new Summary(stats, total, seconds, stopped);
    }

    private static void report(PrintStream out, CorpusWriter.Stats s, long start) {
        double seconds = (System.nanoTime() - start) / 1e9;
        out.printf(java.util.Locale.ROOT, "read=%d accepted=%d rejected=%d duplicates=%d added=%d upgraded=%d persisted=%d completed-shards=%d elapsed=%.2fs records/s=%.0f%n",
                s.read(), s.accepted(), s.rejected(), s.duplicates(), s.added(), s.upgraded(), s.persisted(), s.completedShards(), seconds, s.read() / seconds);
    }
    private static String prefixHash(Path input) throws IOException {
        try (InputStream in = Files.newInputStream(input)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int remaining = 1048576;
            for (int n; remaining > 0 && (n = in.read(buffer, 0, Math.min(buffer.length, remaining))) >= 0;) {
                digest.update(buffer, 0, n); remaining -= n;
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }

    /** Fixed input buffer and capped per-record retention, even for malformed giant lines. */
    private static final class BoundedLines {
        private static final int MAX = 1048576;
        private final Reader reader;
        private final char[] buffer = new char[65536];
        private int cursor, end;
        private boolean tooLong;
        BoundedLines(Reader reader) { this.reader = reader; }
        String next() throws IOException {
            if (end < 0) return null;
            StringBuilder line = new StringBuilder(1024);
            boolean any = false;
            tooLong = false;
            for (;;) {
                if (cursor == end) {
                    end = reader.read(buffer); cursor = 0;
                    if (end < 0) return any ? line.toString() : null;
                    if (end == 0) continue;
                }
                char ch = buffer[cursor++]; any = true;
                if (ch == '\n') {
                    if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') line.setLength(line.length() - 1);
                    return line.toString();
                }
                if (line.length() < MAX) line.append(ch); else tooLong = true;
            }
        }
    }
}
