package com.ohinteractive.seedv6.training.data;

import com.github.luben.zstd.Zstd;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class SourceReadersTest {
    @TempDir Path temporary;
    public static String line(int ordinal) {
        return "{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":" + ordinal + "}]}]}\n";
    }
    static byte[] data(int count) {
        var result = new StringBuilder(); for (int i = 0; i < count; i++) result.append(line(i));
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }
    static void framed(Path file, byte[] raw, int frameSize) throws Exception {
        try (var out = Files.newOutputStream(file)) {
            for (int offset = 0; offset < raw.length; offset += frameSize) {
                byte[] compressed = Zstd.compress(java.util.Arrays.copyOfRange(raw, offset, Math.min(raw.length, offset + frameSize)));
                out.write(ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(0x184d2a50).putInt(4).putInt(compressed.length).array());
                out.write(compressed);
            }
        }
    }
    @Test void exactRangesAcrossFramesAndRestartNeverDecodeUnusedSuffix() throws Exception {
        Path file = temporary.resolve("positions.jsonl.zst"); framed(file, data(16000), 31007);
        DataSource source = DataSource.register("Lichess", file, 1); Path indexes = temporary.resolve("seek");
        try (var reader = SourceReaders.open(source, indexes, 0)) {
            for (int i = 0; i < 10000; i++) { var entry = reader.next(); assertEquals(i, entry.ordinal()); assertEquals(i, entry.position().target()); }
            assertEquals(10000, reader.nextPosition()); assertEquals(10000, reader.decodedRecords());
        }
        var metadata = DataFiles.read(indexes.resolve(source.identity() + ".json"), SourceReaders.Index.class);
        assertEquals(-1, metadata.count()); assertTrue(metadata.points().size() < 100);
        try (var reader = SourceReaders.open(source, indexes, 9998)) {
            for (int i = 9998; i < 10010; i++) assertEquals(i, reader.next().position().target());
            assertEquals(12, reader.decodedRecords()); assertTrue(reader.seekRecords() < 400);
        }
    }
    @Test void plainTextSparseSeekExhaustionAndIdentityAreStrict() throws Exception {
        Path file = temporary.resolve("positions.jsonl"); Files.write(file, data(10000));
        var source = DataSource.register("Plain", file, 1); Path index = temporary.resolve("seek");
        try (var reader = SourceReaders.open(source, index, 0)) { for (int i = 0; i < 9998; i++) assertNotNull(reader.next()); }
        try (var reader = SourceReaders.open(source, index, 9998)) {
            assertTrue(reader.seekRecords() < 4096); assertEquals(9998, reader.next().position().target()); assertEquals(9999, reader.next().position().target());
            assertNull(reader.next()); assertNull(reader.next()); assertEquals(10000, reader.nextPosition());
        }
        assertTrue(assertThrows(IOException.class, () -> SourceReaders.open(source, temporary.resolve("missing-index"), 9998)).getMessage().contains("unbounded prefix"));
        Files.writeString(file, line(42), StandardOpenOption.APPEND);
        assertThrows(IOException.class, () -> SourceReaders.open(source, index, 9998));
    }
    @Test void relocatedSourceHasSameIdentityAndOrdinaryZstdFailsClearly() throws Exception {
        Path file = temporary.resolve("one.jsonl"); Files.write(file, data(10)); var source = DataSource.register("One", file, 1);
        Path copied = temporary.resolve("another.jsonl"); Files.copy(file, copied, StandardCopyOption.COPY_ATTRIBUTES);
        assertEquals(source.identity(), DataSource.register("Other path", copied, 1).identity());
        Path ordinary = temporary.resolve("ordinary.zst"); Files.write(ordinary, Zstd.compress(data(10)));
        assertTrue(assertThrows(IOException.class, () -> DataSource.register("Unsupported", ordinary, 1)).getMessage().contains("No conversion"));
    }
    @Test void lineSlicesPreserveBoundaryRecordsAndFinalUnterminatedLine() throws Exception {
        String base = line(19).stripTrailing();
        // Whitespace is legal JSON padding; exercise LF at/beyond the input-buffer
        // boundary and a UTF-8 string spanning multiple compressed frames.
        String raw = base + " ".repeat(65535 - base.length()) + "\n"
                + base.substring(0, base.length() - 1) + ",\"unused\":\"" + "\u03bb".repeat(40000) + "\"}\r\n"
                + base + " ".repeat(140000);
        for (boolean compressed : new boolean[]{false, true}) {
            Path file = temporary.resolve(compressed ? "slices.jsonl.zst" : "slices.jsonl");
            if (compressed) framed(file, raw.getBytes(StandardCharsets.UTF_8), 31007);
            else Files.writeString(file, raw);
            var source = DataSource.register("Slices", file, 1);
            Path index = temporary.resolve(compressed ? "compressed-index" : "plain-index");
            try (var reader = SourceReaders.open(source, index, 0)) {
                for (int i = 0; i < 3; i++) {
                    var entry = reader.next(); assertEquals(i, entry.ordinal());
                    assertNotNull(entry.position()); assertEquals(19, entry.position().target());
                }
                assertNull(reader.next()); assertEquals(3, reader.nextPosition());
            }
            try (var reader = SourceReaders.open(source, index, 1)) {
                assertEquals(1, reader.next().ordinal()); assertEquals(2, reader.next().ordinal());
                assertNull(reader.next());
            }
        }
    }
    @Test void lineLimitAllowsExactlyOneMebibyteAndRejectsMore() throws Exception {
        String base = line(23).stripTrailing();
        Path file = temporary.resolve("limit.jsonl");
        Files.writeString(file, base + " ".repeat(1048576 - base.length()) + "\n"
                + base + " ".repeat(1048577 - base.length()));
        var source = DataSource.register("Limit", file, 1);
        try (var reader = SourceReaders.open(source, temporary.resolve("index"), 0)) {
            assertEquals(23, reader.next().position().target());
            assertTrue(assertThrows(IOException.class, reader::next).getMessage().contains("exceeds 1 MiB"));
        }
    }
    @Test void boundedLocalArchiveSmoke() throws Exception {
        String location = System.getenv("SEED_TRAINING_SMOKE_SOURCE"); Assumptions.assumeTrue(location != null);
        Path file = Path.of(location); long started = System.nanoTime(); var source = DataSource.register("Local archive", file, 1);
        Path index = temporary.resolve("seek");
        try (var reader = SourceReaders.open(source, index, 0)) {
            for (int i = 0; i < 10000; i++) assertNotNull(reader.next());
            assertEquals(10000, reader.decodedRecords());
        }
        double first = (System.nanoTime() - started) / 1e9; started = System.nanoTime(); long sought;
        try (var reader = SourceReaders.open(source, index, 10000)) {
            sought = reader.seekRecords(); for (int i = 0; i < 10000; i++) assertNotNull(reader.next());
            assertEquals(10000, reader.decodedRecords()); assertTrue(sought < 4096);
        }
        var metadata = DataFiles.read(index.resolve(source.identity() + ".json"), SourceReaders.Index.class);
        assertEquals(-1, metadata.count());
        System.out.printf(java.util.Locale.ROOT, "BOUNDED_SOURCE_SMOKE bytes=%d first_10000_seconds=%.3f next_10000_seconds=%.3f prefix_lines_skipped=%d index_points=%d index_bytes=%d%n",
                Files.size(file), first, (System.nanoTime() - started) / 1e9, sought, metadata.points().size(), Files.size(index.resolve(source.identity() + ".json")));
    }
}
