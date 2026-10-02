package com.ohinteractive.seedv6.corpus;

import java.nio.file.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CorpusPreparationTest {
    @TempDir Path temp;
    static final String POLICY = "test-all-records";

    Path corpus() throws Exception {
        Path root = temp.resolve("corpus");
        try (var writer = new CorpusWriter(root, 4, new CorpusWriter.SourceInfo("test", "preparation", "{}", "depth"))) {
            for (int i = 0; i < 8; i++) writer.put(new CorpusRecord(CorpusPosition.fromFen(
                    "4k3/8/8/8/3pP3/8/8/4K3 w - - " + i + " 1"), CorpusRecord.CP,
                    30, CorpusRecord.WHITE, 20, -1, 0, 1, i + 1));
            writer.checkpoint(true);
        }
        return root;
    }

    @Test void cancellationDuringScanRemovesOnlyOwnedUnpublishedFilesAndAllowsRetry() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        var cancelled = new AtomicBoolean(); var examined = new AtomicInteger();
        try (var reader = new CorpusReader(root)) {
            assertThrows(CorpusPreparation.Cancelled.class, () -> CorpusView.create(reader, view, POLICY, record -> {
                if (examined.incrementAndGet() == 3) cancelled.set(true);
                return null;
            }, Long.MAX_VALUE, new CorpusPreparation(cancelled::get, p -> {})));
        }
        assertFalse(Files.exists(view.resolve("records.idx.pending")));
        assertFalse(Files.exists(view.resolve("records.idx"))); assertFalse(Files.exists(view.resolve("view.json")));
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
        try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
    }

    @Test void cancellationDuringIndexHashingLeavesNoUnpublishedView() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        var hashing = new AtomicBoolean(); var checks = new AtomicInteger();
        var control = new CorpusPreparation(() -> hashing.get() && checks.incrementAndGet() == 2,
                p -> { if (p.stage().equals("Hashing corpus index bytes")) hashing.set(true); });
        try (var reader = new CorpusReader(root)) {
            assertThrows(CorpusPreparation.Cancelled.class, () -> CorpusView.create(reader, view, POLICY,
                    record -> null, Long.MAX_VALUE, control));
        }
        assertTrue(hashing.get()); assertFalse(Files.exists(view.resolve("records.idx.pending")));
        assertFalse(Files.exists(view.resolve("view.json"))); assertFalse(Files.exists(view.resolve("records.idx")));
    }

    @ParameterizedTest @ValueSource(strings = {"Verifying corpus index bytes", "Verifying corpus shard bytes"})
    void cancellingIntegrityPassPreservesPublishedViewAndRetryStillVerifies(String stage) throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
        byte[] index = Files.readAllBytes(view.resolve("records.idx")), metadata = Files.readAllBytes(view.resolve("view.json"));
        var verifying = new AtomicBoolean(); var checks = new AtomicInteger();
        var control = new CorpusPreparation(() -> verifying.get() && checks.incrementAndGet() == 2,
                p -> { if (p.stage().equals(stage)) verifying.set(true); });
        assertThrows(CorpusPreparation.Cancelled.class, () -> new CorpusView(root, view, POLICY, control));
        assertTrue(verifying.get());
        assertArrayEquals(index, Files.readAllBytes(view.resolve("records.idx")));
        assertArrayEquals(metadata, Files.readAllBytes(view.resolve("view.json")));
        try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
        Path shard;
        try (var reader = new CorpusReader(root)) { shard = root.resolve("shards").resolve(reader.shards().getFirst().file()); }
        byte[] bytes = Files.readAllBytes(shard); bytes[40] ^= 1; Files.write(shard, bytes);
        assertThrows(java.io.IOException.class, () -> new CorpusView(root, view, POLICY));
    }

    @Test void staleUnownedPreparationEvidenceIsPreserved() throws Exception {
        Path root = corpus(), view = temp.resolve("view"); Files.createDirectories(view);
        Path pending = view.resolve("records.idx.pending"); Files.writeString(pending, "prior interrupted work");
        try (var reader = new CorpusReader(root)) {
            assertThrows(FileAlreadyExistsException.class, () -> CorpusView.create(reader, view, POLICY, record -> null));
        }
        assertEquals("prior interrupted work", Files.readString(pending));
    }

    @Test void nonCancellationFailureKeepsExistingFailureEvidence() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        try (var reader = new CorpusReader(root)) {
            assertThrows(IllegalStateException.class, () -> CorpusView.create(reader, view, POLICY,
                    record -> { throw new IllegalStateException("Source rejection failure"); }));
        }
        assertTrue(Files.exists(view.resolve("records.idx.pending")));
        assertFalse(Files.exists(view.resolve("view.json")));
    }
}
