package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.service.TrainingSource;
import static org.junit.jupiter.api.Assertions.*;

class PartialGenerationStoreTest {
    @TempDir Path root;
    @Test void failedReferencePublicationKeepsPriorContinuationAndNeverMovesFinalizedReferences() throws Exception {
        String parent; GenerationAttempt attempt; PartialGeneration partial;
        var state = new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            parent = store.initialize(state, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            attempt = new GenerationAttempt(parent, parent, 1, TrainingSource.SELF_PLAY, "fixture", GenerationAttempt.Format.CURRENT, "");
            store.writeGenerationAttempt(attempt);
            state.trainer().train(Board.startingPosition(), 1);
            var games = new SelfPlayBatch.Saved(List.of(new SelfPlayBatch.GameSummary(0, 0, GameTermination.WHITE_CHECKMATES_BLACK, 1, 1, 1, null)),
                    List.of(new TrajectorySampler.Sample(Board.startingPosition(), 1)));
            partial = new PartialGeneration(attempt, "", games, SelfPlayBatch.statistics(games, 2),
                    new SelfPlayControl.TrainingCursor(1, 1, 0, .5, .25), List.of(), Instant.EPOCH, 1, 0, 1, 0, false);
            store.savePartial(partial, state);
        }
        byte[] reference = Files.readAllBytes(root.resolve(PartialGeneration.FILE));
        byte[] latest = Files.readAllBytes(root.resolve("refs/latest-training")), best = Files.readAllBytes(root.resolve("refs/best"));
        try (var store = new CheckpointStore(root, (from, to, replace) -> {
            if (to.getFileName().toString().equals(PartialGeneration.FILE)) throw new IOException("Injected pointer failure");
            CheckpointStore.atomicMove(from, to, replace);
        })) {
            store.requireArchitecture(TrainingArchitecture.BRN2);
            assertThrows(IOException.class, () -> store.savePartial(partial, state));
            assertArrayEquals(state.encode(), store.resumePartialState(PartialGeneration.inspect(root).orElseThrow()).encode());
        }
        assertArrayEquals(reference, Files.readAllBytes(root.resolve(PartialGeneration.FILE)));
        assertArrayEquals(latest, Files.readAllBytes(root.resolve("refs/latest-training")));
        assertArrayEquals(best, Files.readAllBytes(root.resolve("refs/best")));
        assertTrue(CheckpointInspection.validations(root).isEmpty());
        assertEquals(0, CheckpointInspection.manifest(root.resolve("checkpoints").resolve(parent)).generation());
        var saved = PartialGeneration.read(root).orElseThrow();
        byte[] bytes = Files.readAllBytes(saved.state()); bytes[bytes.length - 1] ^= 1; Files.write(saved.state(), bytes);
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            assertThrows(IOException.class, () -> store.resumePartialState(saved.progress()));
        }
        assertArrayEquals(best, Files.readAllBytes(root.resolve("refs/best")));
    }
}
