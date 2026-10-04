package com.ohinteractive.seedv6.training.data;

import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PreparedBinpackTest {
    @TempDir Path temporary;
    String prior;
    @BeforeEach void cache() { prior = System.getProperty("seedv6.preparedDataRoot"); System.setProperty("seedv6.preparedDataRoot", temporary.resolve("cache").toString()); }
    @AfterEach void restore() { if (prior == null) System.clearProperty("seedv6.preparedDataRoot"); else System.setProperty("seedv6.preparedDataRoot", prior); }
    @Test void detectionExplicitProfileMigrationAndMixedFolderRejection() throws Exception {
        var source = BinpackFixtures.source(temporary.resolve("source"));
        assertEquals(DataSource.Format.STOCKFISH_BINPACK_ZSTD, DataSource.detect(source.path()).format()); assertEquals(2, source.shards().size());
        assertThrows(IOException.class, () -> DataSource.register("missing profile", source.path(), 1));
        var mix = new DataSources(1, List.of(source), false); mix.save(temporary.resolve("lineage")); assertEquals(mix, DataSources.read(temporary.resolve("lineage")));
        assertThrows(RuntimeException.class, () -> DataFiles.JSON.fromJson(DataFiles.JSON.toJson(source).replace("BT4_Q_V1", "STOCKFISH_CP_MATE_V1"), DataSource.class));
        Path legacy = Files.createDirectory(temporary.resolve("legacy")); Files.writeString(legacy.resolve("index.sqlite"), "index"); Files.writeString(legacy.resolve("manifest.json"), "{}");
        assertEquals(DataSource.Format.SEED_LEGACY, DataSource.register("legacy", legacy, 1).format());
        Path plain = Files.writeString(temporary.resolve("old.jsonl"), SourceReadersTest.line(1)); var old = DataSource.register("old", plain, 1);
        String oldJson = "{\"name\":\"old\",\"format\":\"LICHESS_JSONL\",\"location\":" + DataFiles.JSON.toJson(plain.toRealPath().toString()) + ",\"identity\":\"" + old.identity() + "\",\"weight\":1}";
        DataSource migrated = DataFiles.JSON.fromJson(oldJson, DataSource.class); assertEquals(old, migrated); assertEquals(LabelProfile.STOCKFISH_CP_MATE_V1, migrated.labelProfile()); migrated.verify();
        Files.writeString(source.path().resolve("unexpected.jsonl"), "{}"); assertThrows(IOException.class, () -> DataSource.detect(source.path())); Files.delete(source.path().resolve("unexpected.jsonl"));
        Files.write(source.path().resolve("bad.zst"), com.github.luben.zstd.Zstd.compress("not binpack".getBytes())); assertThrows(IOException.class, () -> DataSource.detect(source.path()));
    }
    @Test void atomicPreparationInterleavesAndSeeksLaterUnitsWithoutReadingOriginalPrefix() throws Exception {
        var source = BinpackFixtures.source(temporary.resolve("source"));
        var bytes = new ArrayList<byte[]>(); for (var s : source.shards()) bytes.add(Files.readAllBytes(source.shardPath(s)));
        assertThrows(IOException.class, source::requireReady);
        var manifest = PreparedBinpack.prepare(source, CorpusPreparation.NONE); assertEquals(16, manifest.count()); assertEquals(4, manifest.units().size());
        assertEquals(List.of(0, 1, 0, 1), manifest.units().stream().map(PreparedBinpack.Unit::shard).toList());
        int[] expected = {32002,100,200,300,-100,-200,-300,-400,400,500,600,700,-500,-600,-700,-800};
        for (int start = 0; start <= expected.length; start++) try (var reader = SourceReaders.open(source, temporary.resolve("no-index-needed"), start)) {
            assertTrue(reader.seekRecords() < 4);
            for (int i = start; i < expected.length; i++) { var e = reader.next(); assertEquals(i, e.ordinal()); assertEquals(expected[i], e.position().target()); }
            assertNull(reader.next()); assertEquals(16, reader.nextPosition());
        }
        Path publication = PreparedBinpack.directory(source).resolve("manifest.json"); var modified = Files.getLastModifiedTime(publication);
        assertEquals(manifest, PreparedBinpack.prepare(source, CorpusPreparation.NONE)); assertEquals(modified, Files.getLastModifiedTime(publication));
        for (int i = 0; i < bytes.size(); i++) assertArrayEquals(bytes.get(i), Files.readAllBytes(source.shardPath(source.shards().get(i))));
        source.verify();
        // Losing an earlier unit cannot affect a direct later-unit seek; only the visited unit is decompressed.
        Path shard0 = PreparedBinpack.directory(source).resolve("shard-0.zst"); byte[] prepared = Files.readAllBytes(shard0); prepared[0] ^= 1; Files.write(shard0, prepared);
        try (var later = SourceReaders.open(source, temporary, 9)) { assertEquals(500, later.next().position().target()); assertEquals(1, later.seekRecords()); }
        try (var first = SourceReaders.open(source, temporary, 0)) { assertThrows(IOException.class, first::next); }
        assertEquals(manifest, PreparedBinpack.prepare(source, CorpusPreparation.NONE, true));
    }
    @Test void interruptionCorruptionInventoryChangesAndMalformedSuffixFailClosed() throws Exception {
        var source = BinpackFixtures.source(temporary.resolve("source")); var stop = new AtomicBoolean();
        assertThrows(CorpusPreparation.Cancelled.class, () -> PreparedBinpack.prepare(source, new CorpusPreparation(stop::get, p -> stop.set(true))));
        assertThrows(IOException.class, () -> PreparedBinpack.ready(source));
        Path pending = PreparedBinpack.root().resolve(source.identity() + ".pending"); Files.createDirectories(pending); Files.writeString(pending.resolve("partial"), "crashed worker");
        var good = PreparedBinpack.prepare(source, CorpusPreparation.NONE); assertFalse(Files.exists(pending));
        Path manifest = PreparedBinpack.directory(source).resolve("manifest.json"); Files.writeString(manifest, "{}"); assertThrows(IOException.class, () -> PreparedBinpack.ready(source));
        assertEquals(good, PreparedBinpack.prepare(source, CorpusPreparation.NONE));
        BinpackFixtures.archive(source.path().resolve("c.zst"), BinpackFixtures.chunk(5));
        assertThrows(IOException.class, source::verify); var changed = DataSource.register("new version", source.path(), 1, LabelProfile.BT4_Q_V1);
        assertNotEquals(source.identity(), changed.identity()); assertThrows(IOException.class, changed::requireReady);
        Path invalid = temporary.resolve("bad-suffix.zst"); BinpackFixtures.archive(invalid, BinpackFixtures.chunk(3), new byte[]{'B', 'I'});
        var suffix = DataSource.register("bad suffix", invalid, 1, LabelProfile.BT4_Q_V1);
        assertThrows(IOException.class, () -> PreparedBinpack.prepare(suffix, CorpusPreparation.NONE)); assertThrows(IOException.class, suffix::requireReady);
    }
    @Test void removedOrChangedShardsAndConcurrentPreparationCannotReuseAnOldIdentity() throws Exception {
        var source = BinpackFixtures.source(temporary.resolve("source"));
        Files.createDirectories(PreparedBinpack.root());
        try (var channel = java.nio.channels.FileChannel.open(PreparedBinpack.root().resolve(source.identity() + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            assertTrue(assertThrows(IOException.class, () -> PreparedBinpack.prepare(source, CorpusPreparation.NONE)).getMessage().contains("already preparing"));
        }
        Path second = source.shardPath(source.shards().get(1)); Files.delete(second);
        assertThrows(IOException.class, source::verify);
        var reduced = DataSource.register("reduced", source.path(), 1, LabelProfile.BT4_Q_V1); assertNotEquals(source.identity(), reduced.identity());
        BinpackFixtures.archive(source.path().resolve("a.binpack.zst"), BinpackFixtures.chunk(42));
        assertThrows(IOException.class, reduced::verify);
        var edited = DataSource.register("edited", source.path(), 1, LabelProfile.BT4_Q_V1); assertNotEquals(reduced.identity(), edited.identity());
        SourceReadersTest.framed(source.path().resolve("lichess.jsonl.zst"), SourceReadersTest.data(4), 100);
        assertThrows(IOException.class, () -> DataSource.detect(source.path()));
    }
    @Test void boundedRealShardInspection() throws Exception {
        String directory = System.getenv("SEED_BT4_SMOKE_SOURCE"); Assumptions.assumeTrue(directory != null);
        var detected = DataSource.detect(Path.of(directory)); assertEquals(2, detected.shards().size());
        for (var shard : detected.shards()) try (var input = new com.github.luben.zstd.ZstdInputStream(Files.newInputStream(Path.of(directory).resolve(shard.name())))) {
            byte[] first = BinpackDecoder.readChunk(input); var decoder = new BinpackDecoder(first); int count = 0, skip = 0;
            for (; count < 1000; count++) { var record = decoder.next(); assertNotNull(record); if (record.target() == 32002) skip++; else assertTrue(Math.abs(Bt4Targets.q(record.target())) <= 1); }
            System.out.printf("BT4_BOUNDED shard=%s archiveBytes=%d firstChunkBytes=%d records=%d sentinels=%d%n", shard.name(), shard.bytes(), first.length, count, skip);
        }
        assertFalse(Files.exists(PreparedBinpack.root()));
    }
}
