package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.nnue.*;
import static com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class CheckpointRetentionTest {
    @TempDir static Path template;
    @TempDir Path root;
    @BeforeAll static void payloadTemplate() throws Exception { RetentionFixture.template(template); }

    @Test void automaticCompletionPrunesBothFilesPreservesMetadataAndRotatesOnlyOnePreviousBest() throws Exception {
        try (var store = new CheckpointStore(root)) {
            var f = new RetentionFixture(root, template);
            var bootstrap = f.add(0, ""); var accepted = f.bootstrap(bootstrap);
            var old = f.add(1, bootstrap.id()); accepted = f.accept(old, accepted);
            var previous = f.add(2, old.id()); accepted = f.accept(previous, accepted);
            var best = f.add(3, previous.id()); accepted = f.accept(best, accepted);
            var sparse = f.add(10, best.id()); f.validation(sparse.id(), best.id(), false);
            var routine = f.add(11, sparse.id()); f.validation(routine.id(), best.id(), false);
            var unresolved = f.add(12, routine.id());
            var latest = f.add(120, unresolved.id()); var decision = f.validation(latest.id(), best.id(), false);
            Path unknown = f.artifact(routine, "manual-notes.txt"); Files.writeString(unknown, "keep");
            Files.createDirectories(root.resolve("history")); Files.writeString(root.resolve("history/generations-v1.tsv"), "existing-history\n");
            Path partial = Files.createDirectories(root.resolve("staging/checkpoint-partial")); Files.writeString(partial.resolve(NETWORK_FILE), "partial");
            Files.writeString(root.resolve("refs/.best-interrupted.tmp"), "partial");
            Files.createDirectories(root.resolve("checkpoints/unknown")); Files.writeString(root.resolve("checkpoints/unknown/readme"), "keep");
            byte[] identity = Files.readAllBytes(f.artifact(old, MANIFEST_FILE));
            var result = CandidateLifecycle.completeDecision(store, decision);
            assertEquals(latest.id(), result.candidate().manifest().id());
            for (var retired : List.of(old, routine)) {
                assertFalse(Files.exists(f.artifact(retired, NETWORK_FILE))); assertFalse(Files.exists(f.artifact(retired, TRAINING_FILE)));
                var history = CheckpointStore.inspectHistorical(f.artifact(retired, MANIFEST_FILE).getParent());
                assertFalse(history.materialized()); assertEquals(RetentionFixture.HP, history.optimizer());
                assertEquals(retired, history.manifest());
                assertThrows(CheckpointPrunedException.class, () -> store.resume(retired.id()));
                assertThrows(CheckpointPrunedException.class, () -> CheckpointStore.inspect(f.artifact(retired, MANIFEST_FILE).getParent()));
                assertThrows(CheckpointPrunedException.class, () -> CheckpointStore.readNetworkBytes(f.artifact(retired, NETWORK_FILE)));
            }
            for (var kept : List.of(bootstrap, previous, best, sparse, unresolved, latest)) {
                assertTrue(Files.exists(f.artifact(kept, NETWORK_FILE))); assertTrue(Files.exists(f.artifact(kept, TRAINING_FILE)));
            }
            assertEquals(RetentionFixture.HP, store.resume(sparse.id()).optimizer().hyperparameters());
            assertEquals(best.id(), CheckpointStore.readBestSnapshot(root).manifest().id());
            assertArrayEquals(identity, Files.readAllBytes(f.artifact(old, MANIFEST_FILE)));
            assertEquals("existing-history\n", Files.readString(root.resolve("history/generations-v1.tsv")));
            assertEquals("keep", Files.readString(unknown)); assertTrue(Files.exists(partial.resolve(NETWORK_FILE)));
            assertTrue(Files.exists(root.resolve("refs/.best-interrupted.tmp"))); assertTrue(Files.exists(root.resolve("checkpoints/unknown/readme")));
            assertEquals(8, CheckpointInspection.lineage(root, latest.id()).size());
            assertTrue(assertThrows(IllegalArgumentException.class, () -> com.ohinteractive.seedv6.tools.nnue.NnuePerformanceBenchmark.main(
                    new String[] {"--create-network=" + f.artifact(old, NETWORK_FILE)})).getMessage().contains("Standalone fixtures"));
            assertEquals(0, CheckpointPruner.prune(store, latest.id()).files());
            f.latest(bootstrap.id()); // A valid stale reference can traverse intentionally pruned ancestors.
            assertEquals(latest.id(), store.recoverTrainingReferences().latestTraining().orElseThrow().manifest().id());
            var nextBest = f.add(121, latest.id()); accepted = f.accept(nextBest, accepted);
            var cleanup = CheckpointPruner.prune(store, nextBest.id());
            assertTrue(cleanup.failures().isEmpty()); assertEquals(2, cleanup.files());
            assertEquals(previous.networkBytes() + previous.trainingBytes(), cleanup.bytes());
            assertFalse(Files.exists(f.artifact(previous, TRAINING_FILE))); // Older previous loses protection.
            assertTrue(Files.exists(f.artifact(best, TRAINING_FILE)));
            assertEquals(nextBest.id(), CheckpointStore.readBestSnapshot(root).manifest().id());
            Files.delete(f.artifact(nextBest, NETWORK_FILE)); Files.write(f.artifact(nextBest, NETWORK_FILE), new byte[] {1});
            assertThrows(IOException.class, () -> CheckpointStore.readBestSnapshot(root), "Current Best must still pass payload integrity.");
        }
    }

    @Test void pendingValidationPromotionAndUnresolvedCandidatesAreProtected() throws Exception {
        try (var store = new CheckpointStore(root)) {
            var f = new RetentionFixture(root, template);
            var best = f.add(0, ""); var accepted = f.bootstrap(best);
            var pendingValidation = f.add(1, best.id()); f.validation(pendingValidation.id(), best.id(), true);
            var pendingPromotion = f.add(2, pendingValidation.id()); var validation = f.validation(pendingPromotion.id(), best.id(), true);
            f.promotion(PromotionRecord.create(PromotionRecord.Kind.PROMOTION, 1, 2, pendingPromotion.id(), best.id(), accepted.id(), validation.id()));
            var unresolved = f.add(3, pendingPromotion.id());
            var routine = f.add(4, unresolved.id()); f.validation(routine.id(), best.id(), false);
            var latest = f.add(120, routine.id()); f.validation(latest.id(), best.id(), false);
            var cleanup = CheckpointPruner.prune(store, latest.id());
            assertTrue(cleanup.failures().isEmpty()); assertEquals(2, cleanup.files());
            for (var kept : List.of(best, pendingValidation, pendingPromotion, unresolved, latest))
                assertTrue(Files.exists(f.artifact(kept, TRAINING_FILE)));
            var active = f.add(121, latest.id());
            assertFalse(CheckpointPruner.prune(store, latest.id()).failures().isEmpty(), "Stale completion must not prune during active publication.");
            assertTrue(Files.exists(f.artifact(active, NETWORK_FILE)));
            assertFalse(CheckpointPruner.prune(store, active.id()).failures().isEmpty(), "Publication alone is not a decision.");
        }
    }

    @Test void unmarkedMissingPayloadAndDamagedMarkerRemainCorruption() throws Exception {
        try (var store = new CheckpointStore(root)) {
            var f = new RetentionFixture(root, template);
            var best = f.add(0, ""); f.bootstrap(best);
            var old = f.add(1, best.id()); f.validation(old.id(), best.id(), false);
            var latest = f.add(120, old.id()); f.validation(latest.id(), best.id(), false);
            Files.delete(f.artifact(old, NETWORK_FILE));
            assertFalse(assertThrows(IOException.class, () -> store.load(old.id())) instanceof CheckpointPrunedException);
            assertThrows(IOException.class, () -> CheckpointInspection.lineage(root, latest.id()));
            assertFalse(CheckpointPruner.prune(store, latest.id()).failures().isEmpty());
            assertFalse(Files.exists(f.artifact(old, CheckpointPayload.PRUNED)));
            Files.createLink(f.artifact(old, NETWORK_FILE), template.resolve(NETWORK_FILE));
            assertEquals(2, CheckpointPruner.prune(store, latest.id()).files());
            Files.write(f.artifact(old, CheckpointPayload.PRUNED), new byte[] {7});
            assertFalse(assertThrows(IOException.class, () -> store.load(old.id())) instanceof CheckpointPrunedException);
            assertThrows(IOException.class, () -> CheckpointStore.inspectHistorical(f.artifact(old, MANIFEST_FILE).getParent()));
        }
    }

    @Test void failedMarkerPublicationIsNonFatalVisibleAndRetryable() throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        try (var store = new CheckpointStore(root, (source, target, replace) -> {
            if (fail.get() && target.getFileName().toString().equals(CheckpointPayload.PRUNED)) throw new IOException("injected marker publication failure");
            CheckpointStore.atomicMove(source, target, replace);
        })) {
            var f = new RetentionFixture(root, template);
            var best = f.add(0, ""); f.bootstrap(best);
            var old = f.add(1, best.id()); f.validation(old.id(), best.id(), false);
            var latest = f.add(120, old.id()); var decision = f.validation(latest.id(), best.id(), false);
            ByteArrayOutputStream log = new ByteArrayOutputStream(); PrintStream prior = System.err;
            try (var capture = new PrintStream(log)) {
                System.setErr(capture); assertEquals(latest.id(), CandidateLifecycle.completeDecision(store, decision).candidate().manifest().id());
            } finally { System.setErr(prior); }
            assertTrue(log.toString().contains("NNUE retention cleanup FAILED"));
            assertTrue(Files.exists(f.artifact(old, TRAINING_FILE))); assertTrue(Files.exists(f.artifact(old, NETWORK_FILE)));
            assertFalse(Files.exists(f.artifact(old, CheckpointPayload.PRUNED)));
            fail.set(false); assertEquals(2, CheckpointPruner.prune(store, latest.id()).files());
        }
    }

    @Test void interruptionAfterMarkerMoveLeavesDiagnosableIntentAndRetriesWithoutReopeningPayload() throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        try (var store = new CheckpointStore(root, (source, target, replace) -> {
            CheckpointStore.atomicMove(source, target, replace);
            if (fail.get() && target.getFileName().toString().equals(CheckpointPayload.PRUNED))
                throw new IOException("injected interruption after marker move");
        })) {
            var f = new RetentionFixture(root, template);
            var best = f.add(0, ""); f.bootstrap(best);
            var old = f.add(1, best.id()); f.validation(old.id(), best.id(), false);
            var latest = f.add(120, old.id()); f.validation(latest.id(), best.id(), false);
            var interrupted = CheckpointPruner.prune(store, latest.id());
            assertEquals(0, interrupted.files()); assertEquals(1, interrupted.failures().size());
            assertTrue(Files.exists(f.artifact(old, NETWORK_FILE))); assertTrue(Files.exists(f.artifact(old, TRAINING_FILE)));
            assertThrows(CheckpointPrunedException.class, () -> store.load(old.id()));
            assertFalse(CheckpointStore.inspectHistorical(f.artifact(old, MANIFEST_FILE).getParent()).materialized());
            fail.set(false);
            assertEquals(2, CheckpointPruner.prune(store, latest.id()).files());
            assertEquals(latest.id(), store.recoverTrainingReferences().latestTraining().orElseThrow().manifest().id());
        }
    }

    @Test void interruptedDeletionLeavesExplicitPrunedStateAndReclaimsRemainderOnRetry() throws Exception {
        try (var store = new CheckpointStore(root)) {
            var f = new RetentionFixture(root, template);
            var best = f.add(0, ""); f.bootstrap(best);
            var old = f.add(1, best.id()); f.validation(old.id(), best.id(), false);
            var latest = f.add(120, old.id()); f.validation(latest.id(), best.id(), false);
            var partial = CheckpointPruner.prune(store, latest.id(), path -> {
                if (path.getFileName().toString().equals(TRAINING_FILE)) throw new IOException("injected interrupted deletion");
                Files.delete(path);
            });
            assertEquals(1, partial.files()); assertEquals(old.networkBytes(), partial.bytes()); assertEquals(1, partial.failures().size());
            assertTrue(Files.exists(f.artifact(old, TRAINING_FILE))); assertThrows(CheckpointPrunedException.class, () -> store.resume(old.id()));
            assertFalse(CheckpointStore.inspectHistorical(f.artifact(old, MANIFEST_FILE).getParent()).materialized());
            var retry = CheckpointPruner.prune(store, latest.id());
            assertEquals(1, retry.files()); assertEquals(old.trainingBytes(), retry.bytes()); assertTrue(retry.failures().isEmpty());
            assertEquals(0, CheckpointPruner.prune(store, latest.id()).files());
        }
    }

    @Test void futurePublicationWritesConfigurationWithoutChangingManifestIdentity() throws Exception {
        try (var store = new CheckpointStore(root)) {
            var trainer = new NnueTrainer(TrainableNnue.initialized(71), RetentionFixture.HP);
            var checkpoint = store.initialize(trainer, new Metadata(0, 1, ""));
            Path path = root.resolve("checkpoints").resolve(checkpoint.manifest().id());
            assertEquals(RetentionFixture.HP, CheckpointPayload.readConfiguration(path, checkpoint.manifest()));
            assertEquals(checkpoint.manifest(), store.publish(trainer, new Metadata(0, 1, "")).manifest());
            assertEquals(RetentionFixture.HP, store.resume(checkpoint.manifest().id()).optimizer().hyperparameters());
        }
    }

    @Test void backlogPayloadWorkIsBoundedAndEventuallyDrains() throws Exception {
        try (var store = new CheckpointStore(root)) {
            var f = new RetentionFixture(root, template);
            var best = f.add(0, ""); f.bootstrap(best); var parent = best;
            for (int generation = 1; generation <= 17; generation++) {
                parent = f.add(generation, parent.id()); f.validation(parent.id(), best.id(), false);
            }
            var latest = f.add(120, parent.id()); f.validation(latest.id(), best.id(), false);
            assertEquals(8, CheckpointPruner.prune(store, latest.id()).checkpoints());
            assertEquals(8, CheckpointPruner.prune(store, latest.id()).checkpoints());
            assertEquals(0, CheckpointPruner.prune(store, latest.id()).checkpoints());
        }
    }
}
