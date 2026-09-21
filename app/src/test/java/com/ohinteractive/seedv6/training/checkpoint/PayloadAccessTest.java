package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class PayloadAccessTest {
    @TempDir Path root;

    private Process probe(boolean hold) throws Exception {
        String classpath = Path.of(PayloadAccessProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator + Path.of(PayloadAccess.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        ProcessBuilder builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classpath, PayloadAccessProbe.class.getName(), root.toString(), hold ? "hold" : "");
        if (!hold) builder.command().removeLast();
        return builder.redirectError(ProcessBuilder.Redirect.INHERIT).start();
    }

    @Test void exclusiveOwnershipIsEnforcedAcrossSeparateJvmsAndReleasedOnProcessExit() throws Exception {
        Process child = null;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> acquired;
            try (var access = PayloadAccess.acquire(root)) {
                // Nested access in one JVM must not attempt an overlapping OS lock.
                try (var nested = PayloadAccess.acquire(root)) { assertNotNull(nested); }
                child = probe(false);
                var output = child.inputReader();
                assertEquals("waiting", executor.submit(output::readLine).get(10, TimeUnit.SECONDS));
                acquired = executor.submit(output::readLine);
                assertThrows(TimeoutException.class, () -> acquired.get(250, TimeUnit.MILLISECONDS));
            }
            assertEquals("acquired", acquired.get(10, TimeUnit.SECONDS));
            assertTrue(child.waitFor(10, TimeUnit.SECONDS)); assertEquals(0, child.exitValue());
            try (var again = PayloadAccess.acquire(root)) { assertNotNull(again); }
        } finally { if (child != null && child.isAlive()) child.destroyForcibly(); }
    }

    @Test void supportedInspectorsAndPrunerWaitForTheCrossProcessPayloadOwner() throws Exception {
        Path template = Files.createDirectories(root.resolve("template")); RetentionFixture.template(template);
        Process child = null;
        try (var store = new CheckpointStore(root); var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var fixture = new RetentionFixture(root, template);
            var best = fixture.add(0, ""); fixture.bootstrap(best);
            var old = fixture.add(1, best.id()); fixture.validation(old.id(), best.id(), false);
            var latest = fixture.add(120, old.id()); fixture.validation(latest.id(), best.id(), false);
            child = probe(true); var output = child.inputReader();
            assertEquals("waiting", executor.submit(output::readLine).get(10, TimeUnit.SECONDS));
            assertEquals("acquired", executor.submit(output::readLine).get(10, TimeUnit.SECONDS));
            var inspected = executor.submit(() -> CheckpointStore.inspect(root.resolve("checkpoints").resolve(old.id())));
            var snapshot = executor.submit(() -> CheckpointStore.readBestSnapshot(root));
            var raw = executor.submit(() -> CheckpointStore.readNetworkBytes(fixture.artifact(latest, CheckpointManifest.NETWORK_FILE)));
            var cleanup = executor.submit(() -> CheckpointPruner.prune(store, latest.id()));
            try {
                assertThrows(TimeoutException.class, () -> inspected.get(150, TimeUnit.MILLISECONDS));
                assertThrows(TimeoutException.class, () -> snapshot.get(150, TimeUnit.MILLISECONDS));
                assertThrows(TimeoutException.class, () -> raw.get(150, TimeUnit.MILLISECONDS));
                assertThrows(TimeoutException.class, () -> cleanup.get(150, TimeUnit.MILLISECONDS));
                assertTrue(Files.exists(fixture.artifact(old, CheckpointManifest.NETWORK_FILE)));
            } finally {
                // Release the external owner even if an assertion fails, before joining local workers.
                child.getOutputStream().write(1); child.getOutputStream().flush();
            }
            // Scheduling may legitimately select the reader or pruner first; partial/corrupt reads may not occur.
            try { assertEquals(old.id(), inspected.get(20, TimeUnit.SECONDS).manifest().id()); }
            catch (ExecutionException failure) { assertInstanceOf(CheckpointPrunedException.class, failure.getCause()); }
            assertEquals(best.id(), snapshot.get(20, TimeUnit.SECONDS).manifest().id());
            assertEquals(latest.networkBytes(), raw.get(20, TimeUnit.SECONDS).length);
            assertEquals(2, cleanup.get(20, TimeUnit.SECONDS).files());
            assertTrue(child.waitFor(10, TimeUnit.SECONDS)); assertEquals(0, child.exitValue());
        } finally { if (child != null && child.isAlive()) child.destroyForcibly(); }
    }
}
