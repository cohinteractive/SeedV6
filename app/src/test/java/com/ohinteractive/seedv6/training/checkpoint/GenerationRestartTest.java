package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnTrainer;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerationRestartTest {
    @TempDir static Path temporary;
    Path generator; CheckpointStore.Checkpoint actor;
    @BeforeAll void generator() throws Exception {
        generator = temporary.resolve("nnue");
        try (var store = new CheckpointStore(generator, TrainingArchitecture.NNUE)) {
            actor = store.initialize(new NnueTrainer(TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 1, ""));
        }
    }
    record Fixture(Path root, String parent, String candidate, GenerationAttempt old, GenerationAttempt replacement, BootstrapPlan plan, BootstrapData data) {}
    Fixture fixture(String name) throws Exception {
        Path root = temporary.resolve(name);
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            var trainer = new NetworkTrainingState.Brn(new BrnTrainer(.001));
            String parent = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            var source = TrainingSource.bootstrap(generator);
            var old = new GenerationAttempt(parent, parent, 1, source, "old settings", GenerationAttempt.Format.CURRENT, "");
            var replacement = new GenerationAttempt(parent, parent, 1, source, "new settings", GenerationAttempt.Format.CURRENT, "Restarted unfinished generation 1");
            store.writeGenerationAttempt(old); store.writeTrainingSource(source);
            var plan = new BootstrapPlan(parent, parent, 1, source, actor.manifest().id(), actor.manifest().networkSha256(), "old settings", 1);
            store.writeBootstrapPlan(plan);
            var sample = new TrajectorySampler.Sample(Board.startingPosition(), 1);
            var partition = new BootstrapPartition(List.of(sample, sample), List.of(sample, sample), List.of(0, 1), List.of(2, 3));
            var data = new BootstrapData(plan.hash(), partition, new SelfPlayBatch.Statistics(4, 4, 0, 4, 0, 0, 0, 4, 1, 1, 1, 4, 4), 1);
            store.writeBootstrapData(plan, data);
            String candidate = store.publish(trainer, new CheckpointManifest.Metadata(1, 1, parent)).manifest().id();
            Files.writeString(root.resolve("user-notes.txt"), "preserved");
            return new Fixture(root, parent, candidate, old, replacement, plan, data);
        }
    }
    enum Crash { PLAN, DATA, ATTEMPT, CANDIDATE, LATEST, SOURCE, REPLACEMENT, RECEIPT }
    static boolean target(Crash crash, Path from, Path to, Path root) {
        boolean archive = to.startsWith(root.resolve("restarted-generations"));
        return switch (crash) {
            case PLAN -> archive && to.toString().endsWith(".plan");
            case DATA -> archive && to.toString().endsWith(".data");
            case ATTEMPT -> archive && to.getFileName().toString().equals(GenerationAttempt.FILE);
            case CANDIDATE -> archive && from.getParent().equals(root.resolve("checkpoints"));
            case LATEST -> to.equals(root.resolve("refs/latest-training"));
            case SOURCE -> to.equals(root.resolve(CheckpointStore.TRAINING_SOURCE_FILE));
            case REPLACEMENT -> to.equals(root.resolve(GenerationAttempt.FILE));
            case RECEIPT -> archive && from.equals(root.resolve(GenerationRestart.PENDING));
        };
    }
    @ParameterizedTest @EnumSource(Crash.class)
    void reopenCompletesEachInterruptedAtomicArchiveBoundaryAndNeverReusesOldCandidate(Crash crash) throws Exception {
        for (boolean after : List.of(false, true)) {
            var f = fixture(crash + "-" + after); Path root = f.root();
            byte[] parent = Files.readAllBytes(root.resolve("checkpoints").resolve(f.parent()).resolve(CheckpointManifest.TRAINING_FILE));
            try (var store = new CheckpointStore(root, (from, to, replace) -> {
                boolean fail = target(crash, from, to, root);
                if (fail && !after) throw new IOException("injected before " + crash);
                if (replace) Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                else Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
                if (fail && after) throw new IOException("injected after " + crash);
            })) { assertThrows(IOException.class, () -> store.restartGeneration(f.old(), f.replacement(), f.candidate())); }
            try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
                assertEquals(f.replacement(), store.generationAttempt().orElseThrow());
                var recovered = store.recoverTrainingReferences(); assertTrue(recovered.diagnostics().isEmpty());
                assertEquals(f.parent(), recovered.latestTraining().orElseThrow().manifest().id());
                assertEquals(f.parent(), recovered.best().orElseThrow().manifest().id());
                assertTrue(store.bootstrapPlan(f.parent()).isEmpty()); assertFalse(Files.exists(root.resolve(GenerationRestart.PENDING)));
                assertFalse(Files.exists(root.resolve("checkpoints").resolve(f.candidate())));
                assertEquals(List.of(f.parent()), CheckpointStore.availableCheckpoints(root).checkpoints().stream().map(CheckpointManifest::id).toList());
                // Reusing generation 1 is legal; archive contents cannot establish a current highest generation.
                store.publish(new NetworkTrainingState.Brn(new BrnTrainer(.002)), new CheckpointManifest.Metadata(1, 2, f.parent()));
            }
            assertArrayEquals(parent, Files.readAllBytes(root.resolve("checkpoints").resolve(f.parent()).resolve(CheckpointManifest.TRAINING_FILE)));
            assertEquals("preserved", Files.readString(root.resolve("user-notes.txt")));
            try (var receipts = Files.walk(root.resolve("restarted-generations"))) {
                assertEquals(1, receipts.filter(p -> p.getFileName().toString().equals(GenerationRestart.PENDING)).count());
            }
        }
    }
    @Test void unknownCandidateFilesAndDurableDecisionsCannotBeArchived() throws Exception {
        var unknown = fixture("unknown"); Path notes = unknown.root().resolve("checkpoints").resolve(unknown.candidate()).resolve("notes.txt");
        Files.writeString(notes, "user data");
        try (var store = new CheckpointStore(unknown.root(), TrainingArchitecture.BRN)) {
            assertThrows(IOException.class, () -> store.restartGeneration(unknown.old(), unknown.replacement(), unknown.candidate()));
            assertEquals(unknown.candidate(), store.recover().latestTraining().orElseThrow().manifest().id());
        }
        assertEquals("user data", Files.readString(notes)); assertFalse(Files.exists(unknown.root().resolve(GenerationRestart.PENDING)));
        var decided = fixture("decided");
        try (var store = new CheckpointStore(decided.root(), TrainingArchitecture.BRN)) {
            store.recordBootstrapValidation(decided.candidate(), BootstrapEvidence.create(decided.plan(), decided.data(), new HeldOutLoss.Comparison(2, .1, .2)));
            assertThrows(IOException.class, () -> store.restartGeneration(decided.old(), decided.replacement(), decided.candidate()));
            assertTrue(store.validationFor(decided.candidate()).isPresent());
        }
        assertFalse(Files.exists(decided.root().resolve(GenerationRestart.PENDING)));
    }
}
