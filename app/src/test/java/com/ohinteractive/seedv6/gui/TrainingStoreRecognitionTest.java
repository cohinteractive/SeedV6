package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.checkpoint.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.settings;
import static org.junit.jupiter.api.Assertions.*;

/** Real backend/store integration, without starting training or using the user's configured folder. */
@Timeout(60)
class TrainingStoreRecognitionTest {
    @TempDir static Path template;
    @TempDir Path temporary;
    private final TrainingController.Backend backend = new TrainingController.Backend();

    @BeforeAll static void payloads() throws Exception { RetentionFixture.template(template); }

    @Test void missingAndEmptyDirectoriesRemainFreshWithoutCreatingAStore() throws Exception {
        Path missing = temporary.resolve("missing");
        assertFalse(backend.inspect(settings(missing, 1, 1)).resume());
        assertFalse(Files.exists(missing));
        assertFalse(backend.inspect(settings(temporary, 1, 1)).resume());
        try (var entries = Files.list(temporary)) { assertEquals(0, entries.count()); }
    }

    @Test void unidentifiedScaffoldingIsPreservedAndRequiresAnExplicitEmptyFolder() throws Exception {
        Path root = temporary.resolve("store");
        try (var store = new CheckpointStore(root)) { store.requireEmptyForBootstrap(); }
        assertTrue(Files.isRegularFile(root.resolve("payload.lock")));
        assertThrows(IOException.class, () -> backend.inspect(settings(root, 1, 1)));
    }

    @Test void legacyStoreRemainsRecognizedAfterFirstInspectionCreatesTheCoordinationLock() throws Exception {
        Path root = temporary.resolve("legacy");
        CheckpointManifest latest;
        try (var store = new CheckpointStore(root)) {
            var fixture = new RetentionFixture(root, template);
            var best = fixture.add(0, ""); fixture.bootstrap(best);
            latest = fixture.add(1, best.id()); fixture.validation(latest.id(), best.id(), false);
        }
        // Only this disposable fixture is changed to the pre-retention layout.
        Files.delete(root.resolve("payload.lock"));
        assertFalse(Files.exists(root.resolve("checkpoints").resolve(latest.id()).resolve("training-metadata.bin")));
        var first = backend.inspect(settings(root, 1, 1));
        assertTrue(first.resume()); assertEquals(latest.id(), first.latestId());
        assertTrue(Files.isRegularFile(root.resolve("payload.lock")));
        assertEquals(first, backend.inspect(settings(root, 1, 1)), "An inspection must not make its store unrecognizable.");
    }

    @Test void existingLegacyPayloadsPlusHistoryAndNewLockMatchTheReportedRegression() throws Exception {
        Path root = temporary.resolve("training");
        CheckpointManifest latest;
        try (var store = new CheckpointStore(root)) {
            var fixture = new RetentionFixture(root, template);
            var best = fixture.add(0, ""); fixture.bootstrap(best);
            latest = fixture.add(132, best.id()); fixture.validation(latest.id(), best.id(), false);
        }
        Files.createDirectories(root.resolve("history"));
        Path history = Files.createFile(root.resolve("history/generations-v1.tsv"));
        var chosen = settings(root.resolve("unused/.."), 10, 1);
        assertEquals(root.toAbsolutePath(), chosen.root());
        assertEquals(history, chosen.root().resolve("history/generations-v1.tsv"));
        var inspected = backend.inspect(chosen);
        assertTrue(inspected.resume()); assertEquals(latest.id(), inspected.latestId());
        assertEquals(1, inspected.depth(), "Persisted depth still drives the existing confirmation flow.");
        assertEquals(0, Files.size(history));
    }

    @Test void prunedAncestorsAndOnlyPolicyGuaranteedPayloadsRemainRecognized() throws Exception {
        Path root = temporary.resolve("retained");
        CheckpointManifest bootstrap, previous, best, sample, old, latest;
        try (var store = new CheckpointStore(root)) {
            var fixture = new RetentionFixture(root, template);
            bootstrap = fixture.add(1, ""); var accepted = fixture.bootstrap(bootstrap);
            previous = fixture.add(2, bootstrap.id()); accepted = fixture.accept(previous, accepted);
            best = fixture.add(3, previous.id()); fixture.accept(best, accepted);
            sample = fixture.add(10, best.id()); fixture.validation(sample.id(), best.id(), false);
            old = fixture.add(11, sample.id()); fixture.validation(old.id(), best.id(), false);
            latest = fixture.add(120, old.id());
            CandidateLifecycle.completeDecision(store, fixture.validation(latest.id(), best.id(), false));
        }
        Files.createDirectories(root.resolve("history")); Files.createFile(root.resolve("history/generations-v1.tsv"));
        for (var pruned : List.of(bootstrap, old)) {
            Path directory = root.resolve("checkpoints").resolve(pruned.id());
            assertTrue(Files.exists(directory.resolve("manifest.bin"))); assertTrue(Files.exists(directory.resolve("payload-pruned.bin")));
            assertFalse(Files.exists(directory.resolve("network.nnue"))); assertFalse(Files.exists(directory.resolve("training.state")));
        }
        var inspected = backend.inspect(settings(root, 1, 1));
        assertTrue(inspected.resume()); assertEquals(latest.id(), inspected.latestId());
        try (var store = new CheckpointStore(root)) {
            assertEquals(best.id(), store.recover().best().orElseThrow().manifest().id());
            for (var kept : List.of(previous, best, sample, latest)) assertEquals(kept.optimizerStep(), store.resume(kept.id()).optimizer().step());
            assertThrows(CheckpointPrunedException.class, () -> store.resume(bootstrap.id()));
        }
    }

    @Test void unrelatedNonEmptyFolderIsStillRejectedWithoutCreatingStoreArtifacts() throws Exception {
        Files.writeString(temporary.resolve("notes.txt"), "unrelated valuable file");
        Files.createFile(temporary.resolve("payload.lock"));
        var failure = assertThrows(IOException.class, () -> backend.inspect(settings(temporary, 1, 1)));
        assertTrue(failure.getMessage().contains("This non-empty folder has no valid checkpoint store identity"));
        assertEquals("unrelated valuable file", Files.readString(temporary.resolve("notes.txt")));
        try (var entries = Files.list(temporary)) { assertEquals(2, entries.count()); }
    }

    @Test void recognizedNamesDoNotBypassReferenceOrPayloadIntegrityChecks() throws Exception {
        Path fake = Files.createDirectories(temporary.resolve("fake"));
        Files.createDirectories(fake.resolve("refs")); Files.writeString(fake.resolve("refs/best"), "invalid reference");
        Files.createFile(fake.resolve("payload.lock"));
        assertTrue(assertThrows(IOException.class, () -> backend.inspect(settings(fake, 1, 1)))
                .getMessage().contains("no valid checkpoint store identity"));
        assertEquals("invalid reference", Files.readString(fake.resolve("refs/best")));

        Path root = temporary.resolve("incomplete");
        CheckpointManifest latest;
        try (var store = new CheckpointStore(root)) {
            var fixture = new RetentionFixture(root, template);
            var best = fixture.add(0, ""); fixture.bootstrap(best);
            latest = fixture.add(1, best.id()); fixture.validation(latest.id(), best.id(), false);
        }
        Path payload = root.resolve("checkpoints").resolve(latest.id()).resolve("network.nnue");
        Files.delete(payload); // Unexpected missing payload in a temporary fixture, without a prune marker.
        assertTrue(assertThrows(IOException.class, () -> backend.inspect(settings(root, 1, 1)))
                .getMessage().contains("corruption/incompatibility"));
        assertFalse(Files.exists(payload));
    }
}
