package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.core.brn1.*;
import com.ohinteractive.seedv6.core.brn2.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static org.junit.jupiter.api.Assertions.*;

class BrnStoreRemediationTest {
    @TempDir Path temp;
    private final TrainingController.Backend backend = new TrainingController.Backend();

    @Test void interruptedNewBootstrapHasVerifiedIdentityAndOnlyExactEmptyScaffoldingCanRecover() throws Exception {
        Path root = temp.resolve("interrupted");
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) { store.requireEmptyForBootstrap(); }
        var before = inventory(root);
        assertFalse(backend.inspect(settings(root, NetworkArchitecture.BRN2)).resume());
        assertEquals(before, inventory(root));
        assertTrue(assertThrows(IOException.class, () -> backend.inspect(settings(root, NetworkArchitecture.BRN)))
                .getMessage().contains("architecture mismatch"));
        Path identity = root.resolve("bootstrap-identity.bin");
        byte[] original = Files.readAllBytes(identity), corrupt = original.clone(); corrupt[0] ^= 1;
        Files.write(identity, corrupt);
        assertThrows(IOException.class, () -> backend.inspect(settings(root, NetworkArchitecture.BRN2)));
        assertArrayEquals(corrupt, Files.readAllBytes(identity));
        Files.write(identity, original);
        Path stray = Files.createDirectories(root.resolve("history"));
        assertThrows(IOException.class, () -> backend.inspect(settings(root, NetworkArchitecture.BRN2)));
        Files.delete(stray); initialize(root, NetworkArchitecture.BRN2);
        assertTrue(backend.inspect(settings(root, NetworkArchitecture.BRN2)).resume());
    }

    @Test void freshBrn2InspectionAndHistoryPollingCreateNothingBeforeBootstrap() throws Exception {
        Path root = temp.resolve("fresh"); var settings = settings(root, NetworkArchitecture.BRN2);
        var controller = edt(() -> new TrainingController(settings, backend, s -> {}, s -> {}));
        try { edt(controller::poll); } finally { edt(controller::beginShutdown).run(); }
        assertFalse(Files.exists(root)); assertFalse(backend.inspect(settings).resume());
        Files.createDirectories(root); assertEquals(HistoryRepository.Snapshot.EMPTY, new HistoryRepository(root).refresh());
        assertFalse(backend.inspect(settings).resume()); assertEquals(List.of(), entries(root));
        initialize(root, NetworkArchitecture.BRN2);
        assertTrue(backend.inspect(settings).resume()); assertFalse(Files.exists(root.resolve("history")));
    }

    @Test void reportedSwitchFromBrn0ToBrn2WithExtraNewFolderRecognizesActualIdentity() throws Exception {
        Path a = temp.resolve("BRN-0/training"), b = temp.resolve("BRN-2/training");
        initialize(a, NetworkArchitecture.BRN); initialize(b, NetworkArchitecture.BRN2);
        assertTrue(backend.inspect(settings(a, NetworkArchitecture.BRN)).resume());
        Files.createDirectory(b.resolve("New Folder"));
        // Exact former reject predicate, applied before manifest inspection, reproduces the error.
        assertFalse(Set.of("store.lock", "payload.lock", "checkpoints", "staging", "validations", "promotions", "refs", "history")
                .containsAll(entries(b)));
        assertTrue(backend.inspect(settings(b, NetworkArchitecture.BRN2)).resume());
        assertTrue(Files.isDirectory(b.resolve("New Folder")));
    }

    @Test void everyArchitectureOpensAndMismatchPrecedesIncidentalNamesWithoutWrites() throws Exception {
        for (var architecture : NetworkArchitecture.values()) {
            Path root = temp.resolve(architecture.name()); initialize(root, architecture);
            Files.writeString(root.resolve("notes.txt"), "retain me");
            Files.delete(root.resolve("payload.lock")); // Legacy store before first coordinated load.
            var before = inventory(root);
            assertEquals(1, CheckpointStore.availableCheckpoints(root).checkpoints().size());
            assertEquals(before, inventory(root), "Catalogue browsing must not create a legacy lock");
            assertFalse(CheckpointInspection.freshRoot(root, architecture.trainingArchitecture()));
            assertEquals(before, inventory(root), "Selection recognition must be read only");
            assertTrue(backend.inspect(settings(root, architecture)).resume());
            var wrong = architecture == NetworkArchitecture.BRN2 ? NetworkArchitecture.NNUE : NetworkArchitecture.BRN2;
            before = inventory(root);
            var failure = assertThrows(IOException.class, () -> backend.inspect(settings(root, wrong)));
            assertTrue(failure.getMessage().contains("architecture mismatch"), failure.getMessage());
            assertEquals(before, inventory(root)); assertEquals("retain me", Files.readString(root.resolve("notes.txt")));
        }
    }

    @Test void arbitraryFoldersIncludingHistoryNamesAndEmptyLocksAreNotRecoveryEvidence() throws Exception {
        for (String name : List.of("training", "history", "notes", "scaffold")) {
            Path root = Files.createDirectory(temp.resolve(name));
            Files.createDirectories(root.resolve("training/history"));
            Files.createDirectories(root.resolve("history")); Files.createFile(root.resolve(HistoryRepository.FILE));
            if (name.equals("scaffold")) {
                for (String dir : List.of("checkpoints", "staging", "validations", "promotions", "refs")) Files.createDirectory(root.resolve(dir));
                Files.createFile(root.resolve("store.lock")); Files.createFile(root.resolve("payload.lock"));
            }
            var before = inventory(root);
            assertThrows(IOException.class, () -> backend.inspect(settings(root, NetworkArchitecture.BRN2)));
            assertEquals(before, inventory(root));
        }
    }

    private static List<String> entries(Path root) throws IOException {
        try (var paths = Files.list(root)) { return paths.map(p -> p.getFileName().toString()).sorted().toList(); }
    }
    private static Map<String, String> inventory(Path root) throws IOException {
        var result = new TreeMap<String, String>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.toList()) result.put(root.relativize(path).toString(), Files.isDirectory(path) ? "directory"
                    : Files.size(path) + ":" + Files.getLastModifiedTime(path));
        }
        return result;
    }
    private static void initialize(Path root, NetworkArchitecture architecture) throws Exception {
        NetworkTrainingState initial = switch (architecture) {
            case NNUE -> new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(1)));
            case BRN -> new NetworkTrainingState.Brn(new BrnTrainer(.001));
            case BRN1 -> new NetworkTrainingState.Brn1(new Brn1Trainer(.001));
            case BRN2 -> new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
        };
        try (var store = new CheckpointStore(root, architecture.trainingArchitecture())) {
            store.requireEmptyForBootstrap(); store.initialize(initial, new CheckpointManifest.Metadata(0, 1, ""));
        }
    }
    private static TrainingSettings settings(Path root, NetworkArchitecture architecture) {
        return new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 4, 1, architecture);
    }
}
