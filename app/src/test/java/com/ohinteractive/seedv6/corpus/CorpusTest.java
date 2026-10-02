package com.ohinteractive.seedv6.corpus;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;

class CorpusTest {
    @TempDir Path temp;
    static final String FEN = "4k3/8/8/8/3pP3/8/8/4K3 b - e3";
    static final CorpusWriter.SourceInfo SOURCE = new CorpusWriter.SourceInfo("test", "test-file", "{}", "depth-then-work");
    static CorpusRecord record(String fen, int source, int depth, long work, int target) {
        return new CorpusRecord(CorpusPosition.fromFen(fen), CorpusRecord.CP, target, CorpusRecord.WHITE,
                depth, work, CorpusRecord.KILONODES, source, 1);
    }

    @Test void primitiveRoundTripAndOptionalStateDoNotInventCounters() {
        CorpusPosition unknown = CorpusPosition.fromFen(FEN);
        CorpusPosition zero = CorpusPosition.fromFen(FEN + " 0 1");
        assertFalse(unknown.halfmoveKnown());
        assertTrue(zero.halfmoveKnown());
        assertEquals(FEN, unknown.fen());
        assertNotEquals(unknown, zero);
        assertEquals(zero, CorpusPosition.fromFen(FEN + " 0 998877"));
        assertThrows(IllegalArgumentException.class, () -> unknown.toBoard(-1));
        assertArrayEquals(Board.fromFen(FEN + " 7 1"), unknown.toBoard(7));
        CorpusPosition exactClock = CorpusPosition.fromFen(FEN + " 200 9");
        assertEquals(200, exactClock.halfmove());
        assertEquals(127, Board.halfMoveClock((int) exactClock.toBoard(0)[Board.STATUS]));
        assertEquals(FEN + " 200", exactClock.fen());
        CorpusRecord r = record(FEN, 1, 42, 123456789012L, -40000);
        assertEquals(80, r.encode().length);
        assertEquals(r, CorpusRecord.decode(r.encode()));
        assertThrows(IllegalArgumentException.class, () -> CorpusRecord.decode(new byte[79]));
        assertThrows(IllegalArgumentException.class, () -> CorpusPosition.fromFen("8/8/8/8/8/8/8/8 w - -"));
    }

    @Test void exactIdentityIncludesRulesAndClockButNotLabelOrFullmove() {
        CorpusPosition base = CorpusPosition.fromFen(FEN);
        assertNotEquals(base, CorpusPosition.fromFen(FEN.replace(" b ", " w ").replace("e3", "e6")));
        assertNotEquals(base, CorpusPosition.fromFen(FEN.replace("e3", "-")));
        assertNotEquals(base, CorpusPosition.fromFen(FEN.replace(" - e3", " K e3")));
        assertNotEquals(base, CorpusPosition.fromFen(FEN + " 0"));
        assertArrayEquals(record(FEN, 1, 10, 1, 5).position().identity(), record(FEN, 1, 20, 2, -7).position().identity());
    }

    @Test void shardsManifestReopenAndDuplicateQualityConsolidation() throws Exception {
        Path root = temp.resolve("corpus");
        try (CorpusWriter w = new CorpusWriter(root, 2, SOURCE)) {
            assertEquals(CorpusWriter.Result.ADDED, w.put(record(FEN, w.sourceId(), 20, 100, 12)));
            assertEquals(CorpusWriter.Result.UPGRADED, w.put(record(FEN, w.sourceId(), 21, 50, 30)));
            w.put(record(FEN + " 0 1", w.sourceId(), 20, 1, 8));
            w.checkpoint(true);
        }
        Map<String, String> before = shardHashes(root);
        try (CorpusWriter w = new CorpusWriter(root, 2, SOURCE)) {
            assertEquals(CorpusWriter.Result.DUPLICATE, w.put(record(FEN, w.sourceId(), 20, 999, -9)));
            assertEquals(CorpusWriter.Result.DUPLICATE, w.put(record(FEN, w.sourceId(), 21, 50, 99)));
            assertEquals(CorpusWriter.Result.UPGRADED, w.put(record(FEN, w.sourceId(), 21, 51, 31)));
            w.checkpoint(true);
        }
        for (var entry : before.entrySet()) assertEquals(entry.getValue(), shardHashes(root).get(entry.getKey()));
        try (CorpusReader r = new CorpusReader(root)) {
            assertEquals(2, r.manifest().positions());
            assertEquals(4, r.manifest().storedRecords());
            assertEquals(3, r.manifest().completedShards());
            assertEquals(6, r.manifest().sources().getFirst().recordsRead());
            assertEquals(4, r.manifest().sources().getFirst().duplicates());
            assertEquals(2, r.manifest().sources().getFirst().passes());
            assertEquals(2, r.validate().positions());
            List<CorpusRecord> records = new ArrayList<>();
            r.forEach(records::add);
            assertEquals(31, records.stream().filter(x -> !x.position().halfmoveKnown()).findFirst().orElseThrow().target());
            assertEquals(FEN + " 0 1", Fen.fromBoard(records.getFirst().position().toBoard(0)));
        }
        assertTrue(Files.readString(root.resolve("manifest.json")).contains("\"schemaVersion\": 1"));
    }

    @Test void unfinishedBatchRollsBackAndOrphansDoNotEnterManifest() throws Exception {
        Path root = temp.resolve("corpus");
        try (CorpusWriter w = new CorpusWriter(root, 1, SOURCE)) { w.put(record(FEN, w.sourceId(), 10, 1, 5)); }
        try (CorpusWriter w = new CorpusWriter(root, 100, SOURCE)) { w.put(record(FEN, w.sourceId(), 20, 1, 7)); }
        Files.write(root.resolve("shards/shard-deadbeef.scs"), new byte[4]);
        Files.write(root.resolve("shards/ignored.scs.pending"), new byte[4]);
        Files.writeString(root.resolve("manifest.json"), "stale snapshot");
        try (CorpusWriter w = new CorpusWriter(root, 2, SOURCE)) {
            assertEquals(CorpusWriter.Result.DUPLICATE, w.put(record(FEN, w.sourceId(), 10, 1, 99)));
            w.checkpoint(true);
        }
        try (CorpusReader r = new CorpusReader(root)) {
            assertEquals(1, r.validate().positions());
            r.forEach(x -> assertEquals(5, x.target()));
        }
        assertTrue(Files.readString(root.resolve("manifest.json")).contains("schemaVersion"));
    }

    @Test void incompatibleAdapterLabelsAreNotSilentlyComparedAndWriterIsExclusive() throws Exception {
        Path root = temp.resolve("corpus");
        try (CorpusWriter w = new CorpusWriter(root, 1, SOURCE)) {
            w.put(record(FEN, w.sourceId(), 10, 1, 5));
            assertThrows(IOException.class, () -> new CorpusWriter(root, 1, SOURCE));
        }
        var other = new CorpusWriter.SourceInfo("other", "another", "{}", "other-quality");
        try (CorpusWriter w = new CorpusWriter(root, 1, other)) {
            assertEquals(CorpusWriter.Result.DUPLICATE, w.put(record(FEN, w.sourceId(), 100, 999, 99)));
            w.checkpoint(true);
        }
        try (CorpusReader r = new CorpusReader(root)) { r.forEach(x -> assertEquals(5, x.target())); }
    }

    @Test void knownWorkUpgradesUnknownWorkAndDifferentPerspectiveIsIncomparable() throws Exception {
        Path root = temp.resolve("corpus");
        try (CorpusWriter w = new CorpusWriter(root, 10, SOURCE)) {
            w.put(new CorpusRecord(CorpusPosition.fromFen(FEN), CorpusRecord.CP, 1, CorpusRecord.WHITE,
                    10, -1, CorpusRecord.UNKNOWN_WORK, w.sourceId(), 1));
            assertEquals(CorpusWriter.Result.UPGRADED, w.put(record(FEN, w.sourceId(), 10, 3, 5)));
            assertEquals(CorpusWriter.Result.DUPLICATE, w.put(new CorpusRecord(CorpusPosition.fromFen(FEN),
                    CorpusRecord.CP, 99, CorpusRecord.SIDE_TO_MOVE, 100, 999, CorpusRecord.KILONODES, w.sourceId(), 2)));
            w.checkpoint(true);
        }
        try (CorpusReader r = new CorpusReader(root)) { r.forEach(x -> assertEquals(5, x.target())); }
    }

    @Test void truncatedHeaderChecksumSchemaAndCatalogMismatchFailClosed() throws Exception {
        Path root = temp.resolve("corpus");
        try (CorpusWriter w = new CorpusWriter(root, 1, SOURCE)) { w.put(record(FEN, w.sourceId(), 10, 1, 5)); }
        Path shard;
        try (var files = Files.list(root.resolve("shards"))) { shard = files.findFirst().orElseThrow(); }
        byte[] original = Files.readAllBytes(shard);
        Files.write(shard, Arrays.copyOf(original, original.length - 1));
        assertThrows(IOException.class, () -> new CorpusReader(root));
        Files.write(shard, original);
        byte[] badHeader = original.clone(); badHeader[11] = 2;
        Files.write(shard, badHeader);
        assertThrows(IOException.class, () -> new CorpusReader(root));
        Files.write(shard, original);
        byte[] corrupt = original.clone(); corrupt[79] ^= 1;
        Files.write(shard, corrupt);
        try (CorpusReader r = new CorpusReader(root)) { assertThrows(IOException.class, r::validate); }
        Files.write(shard, original);
        try (Connection c = CorpusCatalog.connect(root, false); Statement s = c.createStatement()) { s.execute("UPDATE positions SET depth=999"); }
        try (CorpusReader r = new CorpusReader(root)) { assertThrows(IOException.class, () -> r.forEach(x -> {})); }
        try (Connection c = CorpusCatalog.connect(root, false); Statement s = c.createStatement()) { s.execute("PRAGMA user_version=2"); }
        assertThrows(IOException.class, () -> new CorpusReader(root));
    }

    @Test void abruptJvmTerminationLeavesOnlyCompletedCheckpoint() throws Exception {
        Path root = temp.resolve("crashed");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", crashClasspath(), CrashWriter.class.getName(), root.toString()).redirectErrorStream(true).start();
        assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(23, process.exitValue(), new String(process.getInputStream().readAllBytes()));
        try (CorpusReader r = new CorpusReader(root)) { assertEquals(1, r.validate().positions()); }
        try (CorpusWriter w = new CorpusWriter(root, 1, SOURCE)) {
            assertEquals(CorpusWriter.Result.DUPLICATE, w.put(record(FEN, w.sourceId(), 10, 1, 5)));
            assertEquals(CorpusWriter.Result.ADDED, w.put(record(FEN + " 0 1", w.sourceId(), 10, 1, 5)));
            w.checkpoint(true);
        }
        try (CorpusReader r = new CorpusReader(root)) { assertEquals(2, r.validate().positions()); }
    }
    public static class CrashWriter {
        public static void main(String[] args) throws Exception {
            CorpusWriter w = new CorpusWriter(Path.of(args[0]), 100, SOURCE);
            w.put(record(FEN, w.sourceId(), 10, 1, 5)); w.checkpoint(false);
            w.put(record(FEN + " 0 1", w.sourceId(), 10, 1, 5));
            Runtime.getRuntime().halt(23);
        }
    }

    @Test void sqliteHotJournalRecoversSpilledUncommittedChangesOnReaderReopen() throws Exception {
        Path root = temp.resolve("hot-journal");
        try (CorpusWriter w = new CorpusWriter(root, 1, SOURCE)) { w.put(record(FEN, w.sourceId(), 10, 1, 5)); }
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", crashClasspath(), CrashSpill.class.getName(), root.toString()).redirectErrorStream(true).start();
        assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(24, process.exitValue(), new String(process.getInputStream().readAllBytes()));
        assertTrue(Files.size(root.resolve("index.sqlite-journal")) > 512, "A real spilled hot journal must exist");
        try (CorpusReader r = new CorpusReader(root)) {
            assertEquals(1, r.validate().positions());
            assertEquals(1, r.manifest().sources().getFirst().recordsRead());
        }
    }
    public static class CrashSpill {
        public static void main(String[] args) throws Exception {
            Connection c = CorpusCatalog.connect(Path.of(args[0]), false);
            try (Statement s = c.createStatement()) { s.execute("PRAGMA cache_size=1"); }
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.execute("UPDATE manifest SET positions=999");
                s.execute("UPDATE sources SET read=999");
                s.execute("UPDATE positions SET depth=999");
                // Enough different pages to force dirty cache spill into the main catalog.
                s.execute("UPDATE shards SET sha256='bad'");
            }
            Runtime.getRuntime().halt(24);
        }
    }

    @Test void committedShardSurvivesManifestSnapshotFailure() throws Exception {
        Path root = temp.resolve("snapshot-failure");
        try (CorpusWriter w = new CorpusWriter(root, 1, SOURCE)) {
            Files.createDirectory(root.resolve("manifest.json.pending"));
            assertThrows(IOException.class, () -> w.put(record(FEN, w.sourceId(), 10, 1, 5)));
            assertThrows(IOException.class, () -> w.put(record(FEN + " 1 1", w.sourceId(), 10, 1, 5)));
        }
        try (CorpusReader r = new CorpusReader(root)) { assertEquals(1, r.validate().positions()); }
        Files.delete(root.resolve("manifest.json.pending"));
        try (CorpusWriter w = new CorpusWriter(root, 1, SOURCE)) { w.checkpoint(true); }
        assertTrue(Files.readString(root.resolve("manifest.json")).contains("\"positions\": 1"));
    }
    private static String crashClasspath() throws Exception {
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        for (Class<?> cls : List.of(CrashWriter.class, CorpusWriter.class, org.sqlite.JDBC.class,
                com.google.gson.Gson.class)) entries.add(Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        return String.join(File.pathSeparator, entries);
    }
    private static Map<String, String> shardHashes(Path root) throws IOException {
        Map<String, String> hashes = new HashMap<>();
        try (var files = Files.list(root.resolve("shards"))) {
            for (Path p : files.toList()) hashes.put(p.getFileName().toString(), CorpusCatalog.sha256(p));
        }
        return hashes;
    }
}
