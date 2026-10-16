package com.ohinteractive.seedv6.training.checkpoint;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class AvailableCheckpointsTest {
    @TempDir static Path template;
    @TempDir Path root;
    @BeforeAll static void payload() throws Exception { RetentionFixture.template(template); }

    @Test void emptySingleAndMultipleNetworksUseFullInspectionAndNewestFirstOrder() throws Exception {
        Path absent = root.resolve("absent");
        assertTrue(CheckpointStore.availableCheckpoints(absent).checkpoints().isEmpty());
        assertFalse(Files.exists(absent));
        try (var writer = new CheckpointStore(root)) {
            assertTrue(CheckpointStore.availableCheckpoints(root).checkpoints().isEmpty());
            var fixture = new RetentionFixture(root, template);
            var first = fixture.add(0, ""); fixture.bootstrap(first);
            assertEquals(List.of(first), CheckpointStore.availableCheckpoints(root).checkpoints());
            var expected = new ArrayList<CheckpointManifest>(); expected.add(first);
            var parent = first;
            for (int i = 1; i <= 12; i++) {
                parent = fixture.add(i, parent.id()); expected.addFirst(parent);
            }
            // Non-accepted and unresolved checkpoints are still valid concrete playing networks.
            assertEquals(expected, CheckpointStore.availableCheckpoints(root).checkpoints());
            Files.delete(fixture.artifact(parent, TRAINING_FILE));
            expected.removeFirst();
            var available = CheckpointStore.availableCheckpoints(root);
            assertEquals(expected, available.checkpoints());
            assertTrue(available.diagnostics().getFirst().contains(parent.id()));
            assertThrows(java.io.IOException.class, () -> CheckpointStore.readSnapshot(root, "../best"));
        }
    }

    @Test void retentionBetweenDiscoveryAndLoadFailsButAlreadyLoadedNetworkSurvives() throws Exception {
        try (var writer = new CheckpointStore(root)) {
            var fixture = new RetentionFixture(root, template);
            var best = fixture.add(0, ""); fixture.bootstrap(best);
            var old = fixture.add(1, best.id()); fixture.validation(old.id(), best.id(), false);
            var latest = fixture.add(120, old.id()); var decision = fixture.validation(latest.id(), best.id(), false);
            assertTrue(CheckpointStore.availableCheckpoints(root).checkpoints().contains(old));
            var pinned = CheckpointStore.readSnapshot(root, old.id());
            float bias = pinned.network().outputBias();
            CandidateLifecycle.completeDecision(writer, decision);
            assertTrue(Files.exists(fixture.artifact(old, MANIFEST_FILE)));
            assertFalse(CheckpointStore.availableCheckpoints(root).checkpoints().contains(old));
            assertThrows(CheckpointPrunedException.class, () -> CheckpointStore.readSnapshot(root, old.id()));
            assertEquals(old.id(), pinned.manifest().id());
            assertEquals(bias, pinned.network().outputBias());
            assertEquals(List.of(latest, best), CheckpointStore.availableCheckpoints(root).checkpoints());
        }
    }

    @Test void listingAndExplicitLoadingWaitForExistingPayloadOwnershipWhileWriterRemainsOpen() throws Exception {
        try (var writer = new CheckpointStore(root); var executor = Executors.newSingleThreadExecutor()) {
            var fixture = new RetentionFixture(root, template);
            var checkpoint = fixture.add(0, ""); fixture.bootstrap(checkpoint);
            for (boolean list : List.of(true, false)) {
                CountDownLatch entered = new CountDownLatch(1);
                Future<?> reader;
                try (var access = PayloadAccess.acquire(root)) {
                    reader = executor.submit(() -> {
                        entered.countDown();
                        return list ? CheckpointStore.availableCheckpoints(root) : CheckpointStore.readSnapshot(root, checkpoint.id());
                    });
                    assertTrue(entered.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> reader.get(100, TimeUnit.MILLISECONDS));
                }
                assertNotNull(reader.get(20, TimeUnit.SECONDS));
            }
        }
    }
}
