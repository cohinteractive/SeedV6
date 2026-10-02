package com.ohinteractive.seedv6.corpus;

import java.nio.file.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
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

    @Test void abandonedScratchIsRebuiltWithoutResumingPartialAddresses() throws Exception {
        Path root = corpus(), view = temp.resolve("view"); Files.createDirectories(view);
        Path pending = view.resolve("records.idx.pending"); Files.writeString(pending, "prior interrupted work");
        Files.writeString(view.resolve("view.json.pending"), "unfinished descriptor");
        try (var reader = new CorpusReader(root)) {
            CorpusView.create(reader, view, POLICY, record -> null);
        }
        assertFalse(Files.exists(pending)); assertFalse(Files.exists(view.resolve("view.json.pending")));
        try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
    }

    @Test void ordinaryFailureCleansScratchAndAllowsRetry() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        try (var reader = new CorpusReader(root)) {
            assertThrows(IllegalStateException.class, () -> CorpusView.create(reader, view, POLICY,
                    record -> { throw new IllegalStateException("Source rejection failure"); }));
        }
        assertFalse(Files.exists(view.resolve("records.idx.pending")));
        assertFalse(Files.exists(view.resolve("view.json")));
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
        try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
    }

    @Test void cancellationAfterWritingDescriptorRemovesBothScratchFiles() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        var control = new CorpusPreparation(() -> Files.exists(view.resolve("view.json.pending")), p -> {});
        try (var reader = new CorpusReader(root)) {
            assertThrows(CorpusPreparation.Cancelled.class, () -> CorpusView.create(reader, view, POLICY,
                    record -> null, Long.MAX_VALUE, control));
        }
        assertFalse(Files.exists(view.resolve("records.idx.pending")));
        assertFalse(Files.exists(view.resolve("view.json.pending")));
        assertFalse(Files.exists(view.resolve("records.idx")));
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
    }

    @Test void publishedViewAndItsFinalIndexAreNeverReplaced() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
        byte[] index = Files.readAllBytes(view.resolve("records.idx")), descriptor = Files.readAllBytes(view.resolve("view.json"));
        Files.writeString(view.resolve("records.idx.pending"), "unrelated scratch beside a pin");
        try (var reader = new CorpusReader(root)) {
            assertThrows(IOException.class, () -> CorpusView.create(reader, view, POLICY, record -> null));
        }
        assertArrayEquals(index, Files.readAllBytes(view.resolve("records.idx")));
        assertArrayEquals(descriptor, Files.readAllBytes(view.resolve("view.json")));
        assertEquals("unrelated scratch beside a pin", Files.readString(view.resolve("records.idx.pending")));
        try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void interruptedPairPublicationVerifiesCompletedIndexBeforeFinishing(boolean corrupt) throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
        // Exact on-disk state after moving records.idx and before moving view.json.
        Files.move(view.resolve("view.json"), view.resolve("view.json.pending"));
        if (corrupt) {
            byte[] bytes = Files.readAllBytes(view.resolve("records.idx")); bytes[0] ^= 1;
            Files.write(view.resolve("records.idx"), bytes);
        }
        byte[] index = Files.readAllBytes(view.resolve("records.idx")), descriptor = Files.readAllBytes(view.resolve("view.json.pending"));
        try (var reader = new CorpusReader(root)) {
            if (corrupt) assertThrows(IOException.class, () -> CorpusView.create(reader, view, POLICY, record -> null));
            else CorpusView.create(reader, view, POLICY, record -> { throw new AssertionError("Completed index was rebuilt"); });
        }
        assertArrayEquals(index, Files.readAllBytes(view.resolve("records.idx")));
        assertArrayEquals(descriptor, Files.readAllBytes(view.resolve(corrupt ? "view.json.pending" : "view.json")));
        assertEquals(corrupt, Files.exists(view.resolve("view.json.pending")));
        if (!corrupt) try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
    }

    @Test void cancellationWhileRecoveringCompletedPairPreservesItForRetry() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
        Files.move(view.resolve("view.json"), view.resolve("view.json.pending"));
        byte[] index = Files.readAllBytes(view.resolve("records.idx")), descriptor = Files.readAllBytes(view.resolve("view.json.pending"));
        var cancelled = new AtomicBoolean();
        try (var reader = new CorpusReader(root)) {
            assertThrows(CorpusPreparation.Cancelled.class, () -> CorpusView.create(reader, view, POLICY,
                    record -> null, Long.MAX_VALUE, new CorpusPreparation(cancelled::get, p -> cancelled.set(true))));
        }
        assertArrayEquals(index, Files.readAllBytes(view.resolve("records.idx")));
        assertArrayEquals(descriptor, Files.readAllBytes(view.resolve("view.json.pending")));
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
        try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
    }

    @Test void finalIndexWithoutDescriptorFailsClosedWithoutRemovingEvidence() throws Exception {
        Path root = corpus(), view = Files.createDirectories(temp.resolve("view"));
        Files.writeString(view.resolve("records.idx"), "unknown final index");
        Files.writeString(view.resolve("records.idx.pending"), "scratch");
        try (var reader = new CorpusReader(root)) {
            assertThrows(IOException.class, () -> CorpusView.create(reader, view, POLICY, record -> null));
        }
        assertEquals("unknown final index", Files.readString(view.resolve("records.idx")));
        assertEquals("scratch", Files.readString(view.resolve("records.idx.pending")));
    }

    @Test void overlappingJvmOwnerPreventsScratchReconciliation() throws Exception {
        Path root = corpus(), view = Files.createDirectories(temp.resolve("view"));
        Files.writeString(view.resolve("records.idx.pending"), "owned scratch");
        try (var channel = FileChannel.open(view.resolve("preparation.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                var lock = channel.lock(); var reader = new CorpusReader(root)) {
            var failure = assertThrows(IOException.class, () -> CorpusView.create(reader, view, POLICY, record -> null));
            assertTrue(failure.getMessage().contains("already has an owner"));
            assertEquals("owned scratch", Files.readString(view.resolve("records.idx.pending")));
        }
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
    }

    @Test void activePreparationInAnotherJvmKeepsItsScratchAndCanComplete() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        Process child = probe(root, view, "hold");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                var output = child.inputReader();
                assertEquals("owned", executor.submit(output::readLine).get(10, TimeUnit.SECONDS));
                byte[] scratch = Files.readAllBytes(view.resolve("records.idx.pending"));
                try (var reader = new CorpusReader(root)) {
                    var failure = assertThrows(IOException.class, () -> CorpusView.create(reader, view, POLICY, record -> null));
                    assertTrue(failure.getMessage().contains("already has an owner"));
                }
                assertArrayEquals(scratch, Files.readAllBytes(view.resolve("records.idx.pending")));
                child.getOutputStream().write(1); child.getOutputStream().flush();
                assertTrue(child.waitFor(10, TimeUnit.SECONDS)); assertEquals(0, child.exitValue());
            } finally {
                if (child.isAlive()) { child.destroyForcibly(); child.waitFor(10, TimeUnit.SECONDS); }
            }
        }
        assertFalse(Files.exists(view.resolve("records.idx.pending")));
        try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
    }

    @Test void abruptPreparationTerminationLeavesScratchThatTheNextOwnerRebuilds() throws Exception {
        Path root = corpus(), view = temp.resolve("view");
        Process child = probe(root, view, "crash");
        try {
            assertTrue(child.waitFor(10, TimeUnit.SECONDS));
            assertEquals(23, child.exitValue(), new String(child.getInputStream().readAllBytes()));
        } finally { if (child.isAlive()) child.destroyForcibly(); }
        assertTrue(Files.exists(view.resolve("records.idx.pending")));
        assertFalse(Files.exists(view.resolve("records.idx")));
        try (var reader = new CorpusReader(root)) { CorpusView.create(reader, view, POLICY, record -> null); }
        assertFalse(Files.exists(view.resolve("records.idx.pending")));
        try (var reopened = new CorpusView(root, view, POLICY)) { assertEquals(8, reopened.size()); }
    }

    private static Process probe(Path root, Path view, String mode) throws Exception {
        var entries = new LinkedHashSet<String>();
        for (Class<?> cls : List.of(PreparationProbe.class, CorpusView.class, org.sqlite.JDBC.class, com.google.gson.Gson.class))
            entries.add(Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp",
                String.join(File.pathSeparator, entries), PreparationProbe.class.getName(), root.toString(), view.toString(), mode)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
    }

    public static class PreparationProbe {
        public static void main(String[] args) throws Exception {
            var first = new AtomicBoolean(true);
            try (var reader = new CorpusReader(Path.of(args[0]))) {
                CorpusView.create(reader, Path.of(args[1]), POLICY, record -> {
                    if (first.getAndSet(false)) {
                        if (args[2].equals("crash")) Runtime.getRuntime().halt(23);
                        System.out.println("owned"); System.out.flush();
                        try { System.in.read(); } catch (IOException failure) { throw new UncheckedIOException(failure); }
                    }
                    return null;
                });
            }
        }
    }
}
