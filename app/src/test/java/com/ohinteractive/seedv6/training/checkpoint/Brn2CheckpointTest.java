package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.nnue.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2CheckpointTest {
    @TempDir Path root;

    @Test void absoluteColorManifestBlocksOpeningResumeAndMixedStoresWithoutMutation() throws Exception {
        assertEquals(2, TrainingArchitecture.BRN2.schemaVersion());
        assertEquals(Brn2Features.LEGACY_MESSAGE, assertThrows(IOException.class,
                () -> TrainingArchitecture.fromSchema("seedv6.brn.2", 1)).getMessage());
        // Authentic schema-1 identity and checksum; rejection must precede payload access or writer setup.
        String networkHash = "1".repeat(64), trainingHash = "2".repeat(64);
        byte[] identity = SmallRecord.encode("checkpoint-identity", out -> {
            out.writeUTF("seedv6.brn.2"); out.writeInt(1);
            out.writeLong(315); out.writeLong(484034); out.writeInt(4); out.writeUTF("");
            out.writeUTF(networkHash); out.writeUTF(trainingHash);
        });
        String id = "g000315-s000484034-" + SmallRecord.hash(identity);
        byte[] manifest = SmallRecord.encode("manifest", out -> {
            out.writeUTF(id); out.writeUTF("seedv6.brn.2"); out.writeInt(1);
            out.writeLong(315); out.writeLong(484034); out.writeInt(4); out.writeUTF("");
            out.writeUTF("network.brn2"); out.writeLong(Brn2Codec.MODEL_BYTES); out.writeUTF(networkHash);
            out.writeUTF("training.state"); out.writeLong(Brn2Codec.TRAINING_BYTES); out.writeUTF(trainingHash);
        });
        Path oldRoot = root.resolve("old"), mixed = root.resolve("mixed");
        try (var store = new CheckpointStore(mixed, TrainingArchitecture.BRN2)) {
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new CheckpointManifest.Metadata(0, 1, ""));
        }
        for (Path location : List.of(oldRoot, mixed)) {
            Path directory = Files.createDirectories(location.resolve("checkpoints").resolve(id));
            Files.write(directory.resolve("manifest.bin"), manifest);
            // Existing published stores already contain this empty reader-coordination file.
            if (Files.notExists(location.resolve("payload.lock"))) Files.createFile(location.resolve("payload.lock"));
            Map<Path, String> before = hashes(location);
            assertEquals(Brn2Features.LEGACY_MESSAGE, assertThrows(IOException.class,
                    () -> new CheckpointStore(location, TrainingArchitecture.BRN2)).getMessage());
            var config = new TrainerConfig(location, 71,
                    new TrainerConfig.SelfPlay(1, 1, 8, 0, 0, 4, 8, NnueScoreMapping.V1),
                    new TrainerConfig.Training(1, 1, true),
                    new TrainerConfig.Validation(1, 0, 0, 1, 1, 8, NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                    1, TrainerConfig.DepthChange.REQUIRE_SAME, Board.FEN_STARTING_POSITION, TrainingArchitecture.BRN2, .001);
            try (var service = TrainerService.resume(config)) {
                service.start(); assertTrue(service.awaitTermination(java.time.Duration.ofSeconds(10)));
                assertTrue(service.snapshot().failed());
                assertEquals(Brn2Features.LEGACY_MESSAGE, service.failure().orElseThrow().getMessage());
            }
            assertEquals(Brn2Features.LEGACY_MESSAGE, assertThrows(IOException.class,
                    () -> CheckpointStore.inspectHistorical(directory)).getMessage());
            assertEquals(before, hashes(location));
        }
        assertFalse(Files.exists(oldRoot.resolve("store.lock")));
    }

    private static Map<Path, String> hashes(Path root) throws IOException {
        Map<Path, String> result = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) result.put(root.relativize(path), SmallRecord.hash(path));
        }
        return result;
    }

    @Test void brn1StoreAndPayloadRemainExactAndRejectBrn2InBothDirections() throws Exception {
        assertEquals(TrainingArchitecture.BRN1, TrainingArchitecture.fromSchema("seedv6.brn.1", 1));
        var old = new NetworkTrainingState.Brn1(new com.ohinteractive.seedv6.core.brn1.Brn1Trainer(.007));
        old.trainer().train(Board.startingPosition(), 1);
        byte[] before = old.encode(); String id;
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN1)) {
            id = store.initialize(old, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        byte[] best = Files.readAllBytes(root.resolve("refs/best"));
        byte[] latest = Files.readAllBytes(root.resolve("refs/latest-training"));
        assertThrows(IOException.class, () -> new CheckpointStore(root, TrainingArchitecture.BRN2));
        assertThrows(IOException.class, () -> Brn2Codec.decodeTraining(before));
        assertThrows(IOException.class, () -> Brn2Codec.decodeModel(com.ohinteractive.seedv6.core.brn1.Brn1Codec.encodeModel(old.trainer().snapshot())));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN1)) {
            assertArrayEquals(before, store.resumeState(id).encode());
            assertThrows(IOException.class, () -> store.publish(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),
                    new CheckpointManifest.Metadata(1, 1, id)));
        }
        byte[] newTraining = Brn2Codec.encodeTraining(new Brn2Trainer(.001));
        assertThrows(IOException.class, () -> com.ohinteractive.seedv6.core.brn1.Brn1Codec.decodeTraining(newTraining));
        byte[] newModel = Brn2Codec.encodeModel(new Brn2Model());
        assertThrows(IOException.class, () -> com.ohinteractive.seedv6.core.brn1.Brn1Codec.decodeModel(newModel));
        assertArrayEquals(before, Files.readAllBytes(root.resolve("checkpoints").resolve(id).resolve("training.state")));
        assertArrayEquals(best, Files.readAllBytes(root.resolve("refs/best")));
        assertArrayEquals(latest, Files.readAllBytes(root.resolve("refs/latest-training")));
        Path fresh = root.resolve("brn2");
        try (var store = new CheckpointStore(fresh, TrainingArchitecture.BRN2)) {
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new CheckpointManifest.Metadata(0, 1, ""));
        }
        assertThrows(IOException.class, () -> new CheckpointStore(fresh, TrainingArchitecture.BRN1));
    }

    @Test void brn0IdentityPayloadAndResumeSurviveRejectedBrn2AccessWithoutMigration() throws Exception {
        assertEquals(TrainingArchitecture.BRN, TrainingArchitecture.fromSchema("seedv6.brn.0", 1));
        assertEquals(TrainingArchitecture.BRN2, TrainingArchitecture.fromSchema("seedv6.brn.2", Brn2Features.VERSION));
        var old = new NetworkTrainingState.Brn(new com.ohinteractive.seedv6.core.brn.BrnTrainer(.007));
        old.trainer().train(Board.startingPosition(), 1);
        String id;
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            id = store.initialize(old, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        byte[] bytes = Files.readAllBytes(root.resolve("checkpoints").resolve(id).resolve("training.state"));
        assertThrows(IOException.class, () -> new CheckpointStore(root, TrainingArchitecture.BRN2));
        assertThrows(IOException.class, () -> Brn2Codec.decodeTraining(bytes));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN)) {
            assertArrayEquals(bytes, store.resumeState(id).encode());
            assertThrows(IOException.class, () -> store.publish(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),
                    new CheckpointManifest.Metadata(1, 1, id)));
        }
        Path newRoot = root.resolve("new-brn2");
        String newId;
        try (var store = new CheckpointStore(newRoot, TrainingArchitecture.BRN2)) {
            newId = store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),
                    new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        assertThrows(IOException.class, () -> new CheckpointStore(newRoot, TrainingArchitecture.BRN));
        byte[] newBytes = Files.readAllBytes(newRoot.resolve("checkpoints").resolve(newId).resolve("training.state"));
        assertThrows(IOException.class, () -> com.ohinteractive.seedv6.core.brn.BrnCodec.decodeTraining(newBytes));
        byte[] oldModel = com.ohinteractive.seedv6.core.brn.BrnCodec.encodeModel(old.trainer().snapshot());
        assertThrows(IOException.class, () -> Brn2Codec.decodeModel(oldModel));
        byte[] newModel = Brn2Codec.encodeModel(new Brn2Model());
        assertThrows(IOException.class, () -> com.ohinteractive.seedv6.core.brn.BrnCodec.decodeModel(newModel));
        assertThrows(IOException.class, () -> com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec.decode(newModel));
        assertArrayEquals(bytes, Files.readAllBytes(root.resolve("checkpoints").resolve(id).resolve("training.state")));
    }
    @Test void modelAndOptimizerRoundTripAndWrongArchitectureCannotRepairOrReplaceStore() throws Exception {
        String initial, candidate;
        var trainer = new NetworkTrainingState.Brn2(new Brn2Trainer(.004));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            initial = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            trainer.trainer().train(Board.startingPosition(), 1);
            candidate = store.publish(trainer, new CheckpointManifest.Metadata(1, 1, initial)).manifest().id();
            assertArrayEquals(trainer.encode(), store.resumeState(candidate).encode());
            assertThrows(IOException.class, () -> store.resume(candidate));
        }
        byte[] latest = Files.readAllBytes(root.resolve("refs/latest-training"));
        byte[] best = Files.readAllBytes(root.resolve("refs/best"));
        var error = assertThrows(IOException.class, () -> new CheckpointStore(root, TrainingArchitecture.NNUE));
        assertTrue(error.getMessage().contains("architecture mismatch"));
        assertArrayEquals(latest, Files.readAllBytes(root.resolve("refs/latest-training")));
        assertArrayEquals(best, Files.readAllBytes(root.resolve("refs/best")));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            var refs = store.recoverTrainingReferences();
            assertEquals(candidate, refs.latestTraining().orElseThrow().manifest().id());
            assertEquals(initial, refs.best().orElseThrow().manifest().id());
            var manifest = refs.latestTraining().orElseThrow().manifest();
            assertEquals(Brn2Codec.MODEL_BYTES, manifest.networkBytes());
            assertEquals(Brn2Codec.TRAINING_BYTES, manifest.trainingBytes());
            assertEquals("network.brn2", manifest.networkFile());
            var restored = (NetworkTrainingState.Brn2) store.resumeState(candidate);
            trainer.trainer().train(Board.startingPosition(), -1); restored.trainer().train(Board.startingPosition(), -1);
            assertArrayEquals(trainer.encode(), restored.encode());
        }
        var historical = CheckpointStore.inspectHistorical(root.resolve("checkpoints").resolve(candidate));
        assertEquals(.004, historical.optimizer().learningRate());
        assertEquals(2, CheckpointStore.availableCheckpoints(root).checkpoints().size());
    }

    @Tag("slow-nnue")
    @Test void legacyNnueStoreResumesUnchangedAndRejectsBrnInBothDirections() throws Exception {
        String id;
        byte[] original;
        try (var store = new CheckpointStore(root)) {
            var trainer = new NnueTrainer(TrainableNnue.initialized(71));
            trainer.trainBatch(new long[][] {Board.startingPosition()}, new double[] {1}, 1);
            original = TrainingStateCodec.encode(trainer);
            id = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        // Original stores predate the optional metadata sidecar; no migration should be needed.
        Files.delete(root.resolve("checkpoints").resolve(id).resolve(CheckpointPayload.CONFIG));
        var failure = assertThrows(IOException.class, () -> new CheckpointStore(root, TrainingArchitecture.BRN2));
        assertTrue(failure.getMessage().contains("architecture mismatch"));
        try (var store = new CheckpointStore(root, TrainingArchitecture.NNUE)) {
            assertEquals(id, store.recoverTrainingReferences().latestTraining().orElseThrow().manifest().id());
            assertArrayEquals(original, TrainingStateCodec.encode(store.resume(id)));
            assertThrows(IOException.class, () -> store.publish(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),
                    new CheckpointManifest.Metadata(1, 1, id)));
            assertEquals(id, store.recover().best().orElseThrow().manifest().id());
            assertArrayEquals(original, Files.readAllBytes(root.resolve("checkpoints").resolve(id).resolve("training.state")));
        }
        assertFalse(Files.exists(root.resolve("checkpoints").resolve(id).resolve(CheckpointPayload.CONFIG)));
    }

    @Test void legacyNnueManifestAndIdentityRemainByteForByteCompatible() throws Exception {
        var metadata = new CheckpointManifest.Metadata(12, 4, "");
        String networkHash = "1".repeat(64), trainingHash = "2".repeat(64);
        byte[] oldIdentity = SmallRecord.encode("checkpoint-identity", out -> {
            out.writeUTF(NnueFeatureSchema.ID); out.writeInt(NnueFeatureSchema.VERSION);
            out.writeLong(12); out.writeLong(31); out.writeInt(4); out.writeUTF("");
            out.writeUTF(networkHash); out.writeUTF(trainingHash);
        });
        var manifest = CheckpointManifest.create(metadata, 31, networkHash, trainingHash);
        assertEquals("g000012-s000000031-" + SmallRecord.hash(oldIdentity), manifest.id());
        byte[] legacy = SmallRecord.encode("manifest", out -> {
            out.writeUTF(manifest.id()); out.writeUTF(NnueFeatureSchema.ID); out.writeInt(NnueFeatureSchema.VERSION);
            out.writeLong(12); out.writeLong(31); out.writeInt(4); out.writeUTF("");
            out.writeUTF("network.nnue"); out.writeLong(manifest.networkBytes()); out.writeUTF(networkHash);
            out.writeUTF("training.state"); out.writeLong(manifest.trainingBytes()); out.writeUTF(trainingHash);
        });
        assertArrayEquals(legacy, manifest.encode());
        var loaded = SmallRecord.decode(legacy, "manifest", CheckpointManifest::read);
        assertEquals(TrainingArchitecture.NNUE, loaded.architecture()); assertEquals(manifest, loaded);
        var brn = CheckpointManifest.create(metadata, 31, networkHash, trainingHash, TrainingArchitecture.BRN2);
        assertNotEquals(manifest.id(), brn.id());
        assertEquals(brn, SmallRecord.decode(brn.encode(), "manifest", CheckpointManifest::read));
    }

    @Test void recoveryAdvancesPublishedBrnCandidateAfterLatestReferenceCrash() throws Exception {
        var trainer = new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
        String initial;
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            initial = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        try (var store = new CheckpointStore(root, (source, target, replace) -> {
            if (target.equals(root.resolve("refs/latest-training"))) throw new IOException("injected latest-reference failure");
            CheckpointStore.atomicMove(source, target, replace);
        })) {
            trainer.trainer().train(Board.startingPosition(), 1);
            assertThrows(IOException.class, () -> store.publish(trainer, new CheckpointManifest.Metadata(1, 1, initial)));
        }
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            var recovered = store.recoverTrainingReferences();
            var latest = recovered.latestTraining().orElseThrow().manifest();
            assertEquals(1, latest.generation()); assertEquals(TrainingArchitecture.BRN2, latest.architecture());
            assertArrayEquals(trainer.encode(), store.resumeState(latest.id()).encode());
            assertEquals(initial, recovered.best().orElseThrow().manifest().id());
        }
    }

    @Test void corruptedBrnPayloadIsRejectedAndPreserved() throws Exception {
        String id;
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            id = store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),
                    new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        Path file = root.resolve("checkpoints").resolve(id).resolve("network.brn2");
        byte[] corrupt = Files.readAllBytes(file); corrupt[0] ^= 1; Files.write(file, corrupt);
        assertThrows(IOException.class, () -> CheckpointStore.readBestSnapshot(root));
        assertArrayEquals(corrupt, Files.readAllBytes(file));
    }

    @Test void existingRetentionUsesBrnPayloadNamesAndPreservesExactLatestState() throws Exception {
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            var trainer = new NetworkTrainingState.Brn2(new Brn2Trainer(.004));
            String best = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            trainer.trainer().train(Board.startingPosition(), 1);
            String old = store.publish(trainer, new CheckpointManifest.Metadata(1, 1, best)).manifest().id();
            CandidateLifecycle.recordDecision(store, old, best, result(false), new PromotionPolicy(1, .9, 0));
            trainer.trainer().train(Board.startingPosition(), -1);
            String latest = store.publish(trainer, new CheckpointManifest.Metadata(101, 1, old)).manifest().id();
            CandidateLifecycle.recordDecision(store, latest, best, result(false), new PromotionPolicy(1, .9, 0));
            Path oldDirectory = root.resolve("checkpoints").resolve(old);
            assertFalse(Files.exists(oldDirectory.resolve("network.brn2")));
            assertFalse(Files.exists(oldDirectory.resolve("training.state")));
            assertThrows(CheckpointPrunedException.class, () -> store.resumeState(old));
            var inspected = CheckpointStore.inspectHistorical(oldDirectory);
            assertFalse(inspected.materialized()); assertEquals(TrainingArchitecture.BRN2, inspected.manifest().architecture());
            assertEquals(.004, inspected.optimizer().learningRate());
            assertArrayEquals(trainer.encode(), store.resumeState(latest).encode());
            assertEquals(latest, store.recoverTrainingReferences().latestTraining().orElseThrow().manifest().id());
        }
    }

    @Test void interruptedPromotionCompletesFromStoredBrnEvidenceWithoutRevalidation() throws Exception {
        String best, candidate, validation;
        var trainer = new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            best = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            trainer.trainer().train(Board.startingPosition(), 1);
            candidate = store.publish(trainer, new CheckpointManifest.Metadata(1, 1, best)).manifest().id();
            validation = store.recordValidation(candidate, best, result(true),
                    new PromotionPolicy(1, .9, 0)).id();
        }
        try (var store = new CheckpointStore(root, (source, target, replace) -> {
            if (target.equals(root.resolve("refs/best"))) throw new IOException("injected Best-reference failure");
            CheckpointStore.atomicMove(source, target, replace);
        })) { assertThrows(IOException.class, () -> store.completePromotion(validation)); }
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            assertEquals(best, store.recover().best().orElseThrow().manifest().id());
            var settled = CandidateLifecycle.completeDecision(store, store.readValidation(validation));
            assertEquals(candidate, settled.references().best().orElseThrow().manifest().id());
            assertArrayEquals(trainer.encode(), store.resumeState(candidate).encode());
            assertEquals(settled.promotion(), CandidateLifecycle.completeDecision(store, store.readValidation(validation)).promotion());
        }
    }

    private static ValidationResult result(boolean promote) {
        var cfg = new ValidationConfig(1, 1, 0, 0, 1, 1,
                NnueScoreMapping.V1, 8);
        var white = new ValidationResult.Game(
                GameTermination.WHITE_CHECKMATES_BLACK, 1);
        var black = new ValidationResult.Game(promote
                ? GameTermination.BLACK_CHECKMATES_WHITE
                : GameTermination.WHITE_CHECKMATES_BLACK, 1);
        return new ValidationResult(cfg, "a".repeat(64),
                List.of(new ValidationResult.Pair("", white, black)));
    }
}
