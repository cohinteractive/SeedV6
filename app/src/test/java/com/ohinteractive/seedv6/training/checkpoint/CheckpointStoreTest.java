package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class CheckpointStoreTest {
    @TempDir Path root;
    static final NnueNetwork NETWORK = NnueNetwork.initialized(71);
    static final PromotionPolicy EASY = new PromotionPolicy(1, 0.9, 0);
    static final long[] BOARD = Board.startingPosition();

    @Tag("slow-nnue")
    @Test void concurrentBestReaderSeesOnlyCompletedAcceptedSnapshotsAtEveryPublicationBoundary() throws Exception {
        var reached = new CountDownLatch[] {new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)};
        var release = new CountDownLatch[] {new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)};
        var publishing = new AtomicBoolean();
        var writer = Executors.newSingleThreadExecutor();
        try (var store = new CheckpointStore(root, (source, destination, replace) -> {
            int gate = destination.getParent().equals(root.resolve("checkpoints")) ? 0
                    : destination.getParent().equals(root.resolve("promotions")) ? 1
                    : destination.equals(root.resolve("refs/best")) ? 2 : -1;
            if (publishing.get() && gate >= 0) {
                reached[gate].countDown();
                try { if (!release[gate].await(30, TimeUnit.SECONDS)) throw new IOException("Publication gate timed out"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
            }
            CheckpointStore.atomicMove(source, destination, replace);
        })) {
            var model = trainer();
            var first = store.initialize(model, metadata(0, ""));
            var pinned = CheckpointStore.readBestSnapshot(root);
            byte[] pinnedBytes = NnueNetworkCodec.encode(pinned.network());
            assertThrows(IOException.class, () -> new CheckpointStore(root), "Readers must not weaken exclusive writer ownership");
            publishing.set(true);
            var promotion = writer.submit(() -> {
                train(model);
                var next = store.publish(model, metadata(1, first.manifest().id()));
                var validation = store.recordValidation(next.manifest().id(), first.manifest().id(), result(true, config(1)), EASY);
                store.completePromotion(validation.id());
                return next;
            });
            try {
                for (int gate = 0; gate < 3; gate++) {
                    assertTrue(reached[gate].await(30, TimeUnit.SECONDS));
                    var during = CheckpointStore.readBestSnapshot(root);
                    assertEquals(first.manifest(), during.manifest());
                    assertArrayEquals(pinnedBytes, NnueNetworkCodec.encode(during.network()));
                    release[gate].countDown();
                }
                var promoted = promotion.get(30, TimeUnit.SECONDS);
                var nextGame = CheckpointStore.readBestSnapshot(root);
                assertEquals(promoted.manifest(), nextGame.manifest());
                assertNotEquals(pinned.manifest().networkSha256(), nextGame.manifest().networkSha256());
                assertArrayEquals(pinnedBytes, NnueNetworkCodec.encode(pinned.network()));
            } finally {
                for (var gate : release) gate.countDown();
                writer.shutdown(); assertTrue(writer.awaitTermination(30, TimeUnit.SECONDS));
            }
        } finally { writer.shutdown(); }
    }

    @Tag("slow-nnue")
    @Test void bestSnapshotReaderPreservesRecoveryAndCorruptionRulesWithoutCreatingOrRepairingFiles() throws Exception {
        Path absent = root.resolve("absent");
        assertThrows(IOException.class, () -> CheckpointStore.readBestSnapshot(absent));
        assertFalse(Files.exists(absent));
        try (var store = new CheckpointStore(root)) {
            var initial = store.publish(trainer(), metadata(0, ""));
            assertThrows(IOException.class, () -> CheckpointStore.readBestSnapshot(root), "A completed candidate is not an accepted Best");
            store.initialize(trainer(), metadata(0, ""));
            Files.delete(root.resolve("refs/best"));
            assertEquals(initial.manifest(), CheckpointStore.readBestSnapshot(root).manifest());
            assertFalse(Files.exists(root.resolve("refs/best")), "Read-only recovery never repairs the store");
            Files.write(root.resolve("refs/best"), new byte[] {1});
            assertThrows(IOException.class, () -> CheckpointStore.readBestSnapshot(root));
            assertArrayEquals(new byte[] {1}, Files.readAllBytes(root.resolve("refs/best")));
        }
    }

    @Tag("slow-nnue")
    @Test void bootstrapAndExactResumePreserveAllArtifactBitsAndOptimizerContinuation() throws Exception {
        NnueTrainer trainer = trainer();
        train(trainer);
        byte[] boundary = TrainingStateCodec.encode(trainer);
        String id;
        try (var store = new CheckpointStore(root)) {
            var checkpoint = store.initialize(trainer, metadata(0, ""));
            id = checkpoint.manifest().id();
            var recovered = store.recover();
            assertEquals(id, recovered.best().orElseThrow().manifest().id());
            assertEquals(id, recovered.latestTraining().orElseThrow().manifest().id());
            assertEquals(PromotionRecord.Kind.BOOTSTRAP, recovered.bestEvidence().orElseThrow().kind());
            assertEquals("", recovered.bestEvidence().orElseThrow().validationId());
            assertEquals(1, count(root.resolve("promotions")));
            assertEquals(0, count(root.resolve("validations")));
            assertArrayEquals(boundary, Files.readAllBytes(artifact(id, TRAINING_FILE)));
            assertEquals(NnueNetworkCodec.ENCODED_BYTES, Files.size(artifact(id, NETWORK_FILE)));
            assertEquals(TrainingStateCodec.ENCODED_BYTES, Files.size(artifact(id, TRAINING_FILE)));
            assertEquals(checkpoint.manifest().networkSha256(), SmallRecord.hash(artifact(id, NETWORK_FILE)));
            assertEquals(checkpoint.manifest().trainingSha256(), SmallRecord.hash(artifact(id, TRAINING_FILE)));
            assertArrayEquals(NnueNetworkCodec.encode(trainer.model().snapshot()),
                    NnueNetworkCodec.encode(store.load(id).network()));
            NnueTrainer resumed = store.resume(id);
            assertArrayEquals(boundary, TrainingStateCodec.encode(resumed));
            long[][] otherAnchor = {Board.fromFen("8/4k3/8/8/8/8/4K3/R7 w - - 0 1")};
            for (int i = 0; i < 2; i++) {
                assertEquals(trainer.trainBatch(otherAnchor, new double[] {0.25}, 1),
                        resumed.trainBatch(otherAnchor, new double[] {0.25}, 1));
            }
            assertArrayEquals(TrainingStateCodec.encode(trainer), TrainingStateCodec.encode(resumed));
            assertArrayEquals(boundary, Files.readAllBytes(artifact(id, TRAINING_FILE)), "Published state stays immutable.");
            assertEquals(1, store.load(id).manifest().optimizerStep());
            System.out.printf("CHECKPOINT_V1 networkBytes=%d trainingBytes=%d manifestBytes=%d directoryForce=%s exactResume=true%n",
                    Files.size(artifact(id, NETWORK_FILE)), Files.size(artifact(id, TRAINING_FILE)),
                    Files.size(artifact(id, MANIFEST_FILE)), store.directoryForceSupported());
        }
        try (var reopened = new CheckpointStore(root)) {
            assertEquals(id, reopened.recover().best().orElseThrow().manifest().id());
            assertThrows(IOException.class, () -> reopened.initialize(trainer(), metadata(1, id)));
        }
    }

    @Tag("slow-nnue")
    @Test void publicationAloneDoesNotInventAnIncumbent() throws Exception {
        try (var store = new CheckpointStore(root)) {
            var checkpoint = store.publish(trainer(), metadata(0, ""));
            assertTrue(store.recover().best().isEmpty());
            assertEquals(checkpoint.manifest().id(), store.recover().latestTraining().orElseThrow().manifest().id());
            Files.delete(root.resolve("refs/latest-training"));
            assertTrue(store.recover().best().isEmpty());
            assertEquals(checkpoint.manifest().id(), store.recover().latestTraining().orElseThrow().manifest().id());
        }
    }

    @Tag("slow-nnue")
    @Test void latestMovesIndependentlyAndDuplicatePublicationIsIdempotentWithoutRewinding() throws Exception {
        NnueTrainer trainer = trainer();
        try (var store = new CheckpointStore(root)) {
            var a = store.initialize(trainer, metadata(0, ""));
            var bytes = Files.readAllBytes(artifact(a.manifest().id(), MANIFEST_FILE));
            var b = store.publish(trainer, metadata(1, a.manifest().id()));
            assertNotEquals(a.manifest().id(), b.manifest().id());
            assertEquals(a.manifest(), store.recover().best().orElseThrow().manifest());
            assertEquals(b.manifest(), store.recover().latestTraining().orElseThrow().manifest());
            assertEquals(a.manifest(), store.publish(trainer, metadata(0, "")).manifest());
            assertEquals(b.manifest(), store.recover().latestTraining().orElseThrow().manifest());
            assertEquals(b.manifest(), store.publish(trainer, metadata(1, a.manifest().id())).manifest());
            assertEquals(2, count(root.resolve("checkpoints")));
            assertEquals(0, count(root.resolve("staging")));
            assertArrayEquals(bytes, Files.readAllBytes(artifact(a.manifest().id(), MANIFEST_FILE)));
            assertThrows(IOException.class, () -> store.publish(trainer, metadata(0, a.manifest().id())));
            train(trainer);
            assertThrows(IOException.class, () -> store.publish(trainer, metadata(1, a.manifest().id())));
        }
    }

    @Test void mismatchedInferenceSnapshotRejectsPublicationBeforeAnyReference() throws Exception {
        try (var store = new CheckpointStore(root)) {
            assertThrows(IOException.class, () -> store.publish(NnueNetwork.initialized(99), trainer(), metadata(0, "")));
            assertEquals(0, count(root.resolve("checkpoints")));
            assertFalse(Files.exists(root.resolve("refs/latest-training")));
        }
        assertThrows(IllegalArgumentException.class, () -> metadata(-1, ""));
        assertThrows(IllegalArgumentException.class, () -> metadata(0, "../escape"));
        assertThrows(IllegalArgumentException.class, () -> new Metadata(0, 0, ""));
    }

    @Tag("slow-nnue")
    @Test void distinctOptimizerStatesWithSameModelCannotCollide() throws Exception {
        try (var first = new CheckpointStore(root.resolve("one"));
             var second = new CheckpointStore(root.resolve("two"))) {
            var one = first.publish(trainer(), metadata(0, ""));
            var otherHp = new NnueTrainer(TrainableNnue.fromNetwork(NETWORK), new AdamHyperparameters(0.02, 0.8, 0.95, 1e-7));
            var two = second.publish(otherHp, metadata(0, ""));
            assertEquals(one.manifest().networkSha256(), two.manifest().networkSha256());
            assertNotEquals(one.manifest().trainingSha256(), two.manifest().trainingSha256());
            assertNotEquals(one.manifest().id(), two.manifest().id());
        }
    }

    @Tag("slow-nnue")
    @Test void retainedCandidateSurvivesReopenAndResumesItsOwnLineage() throws Exception {
        String a, b, validation;
        byte[] candidateState;
        try (var store = new CheckpointStore(root)) {
            a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            NnueTrainer candidate = store.resume(a);
            train(candidate);
            candidateState = TrainingStateCodec.encode(candidate);
            var result = lifecycle(store, candidate, metadata(1, a), false, EASY);
            b = result.candidate().manifest().id();
            validation = result.validation().id();
            assertEquals(PromotionPolicy.Decision.RETAIN_INCUMBENT, result.validation().assessment().decision());
            assertTrue(result.promotion().isEmpty());
            assertEquals(a, result.references().best().orElseThrow().manifest().id());
            assertEquals(b, result.references().latestTraining().orElseThrow().manifest().id());
            assertEquals(1, count(root.resolve("promotions")));
            assertEquals(1, count(root.resolve("validations")));
            assertThrows(IOException.class, () -> store.promote(validation));
        }
        try (var store = new CheckpointStore(root)) {
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            assertEquals(b, store.recover().latestTraining().orElseThrow().manifest().id());
            assertArrayEquals(candidateState, TrainingStateCodec.encode(store.resume(b)));
            assertEquals(validation, store.readValidation(validation).id());
            NnueTrainer resumed = store.resume(b);
            NnueTrainer expected = TrainingStateCodec.decode(candidateState);
            train(resumed); train(expected);
            assertArrayEquals(TrainingStateCodec.encode(expected), TrainingStateCodec.encode(resumed));
        }
    }

    @Tag("slow-nnue")
    @Test void promotionPublishesEvidenceBeforeBestAndRecoversOnlyThroughItsChain() throws Exception {
        String a, b, recordId;
        try (var store = new CheckpointStore(root)) {
            a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            NnueTrainer candidate = store.resume(a); train(candidate);
            var result = lifecycle(store, candidate, metadata(1, a), true, EASY);
            b = result.candidate().manifest().id();
            recordId = result.promotion().orElseThrow().id();
            assertEquals(PromotionPolicy.Decision.PROMOTE, result.validation().assessment().decision());
            assertEquals(2, count(root.resolve("promotions")));
            assertEquals(1, count(root.resolve("validations")));
            assertEquals(a, result.promotion().orElseThrow().previousCheckpointId());
            assertEquals(result.validation().id(), result.promotion().orElseThrow().validationId());
            assertEquals(b, result.references().best().orElseThrow().manifest().id());
            assertEquals(b, result.references().latestTraining().orElseThrow().manifest().id());
            assertThrows(IOException.class, () -> store.promote(result.validation().id()), "Old incumbent evidence is stale now.");
        }
        try (var store = new CheckpointStore(root)) {
            Files.write(root.resolve("refs/best"), new byte[] {1, 2, 3});
            assertEquals(b, store.recover().best().orElseThrow().manifest().id());
            assertEquals(recordId, store.recover().bestEvidence().orElseThrow().id());
            store.repairReferences();
            Files.delete(root.resolve("refs/best"));
            assertEquals(b, store.recover().best().orElseThrow().manifest().id());
        }
    }

    @Tag("slow-nnue")
    @Test void cancelledLifecycleStillPreservesCandidateAndInconclusiveRecord() throws Exception {
        try (var store = new CheckpointStore(root)) {
            String a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            var control = new ValidationControl(); control.cancel();
            var result = CandidateLifecycle.run(store, trainer(), metadata(1, a), config(2),
                    PromotionPolicy.DEFAULT, control);
            assertEquals(PromotionPolicy.Decision.INCONCLUSIVE, result.validation().assessment().decision());
            assertEquals(2, result.validation().statistics().incompletePairs());
            assertEquals(0, result.validation().statistics().draws());
            assertEquals(a, result.references().best().orElseThrow().manifest().id());
            assertEquals(result.candidate().manifest().id(), result.references().latestTraining().orElseThrow().manifest().id());
            assertEquals(result.validation(), store.readValidation(result.validation().id()));
            assertEquals(1, count(root.resolve("promotions")));
        }
    }

    @Tag("slow-nnue")
    @Test void actualPersistedCandidateIncumbentMatchProducesDurableDeterministicSmokeEvidence() throws Exception {
        String a, b;
        ValidationRecord evidence;
        long[] board = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 98 1");
        var cfg = new ValidationConfig(2, 12091, 0, 0, 1, 1, new NnueScoreMapping(1_000_000), 8);
        try (var store = new CheckpointStore(root)) {
            a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            NnueTrainer candidate = store.resume(a); train(candidate);
            var result = CandidateLifecycle.run(store, candidate, metadata(1, a), cfg, PromotionPolicy.DEFAULT,
                    board, GameHistory.initial(board), new ValidationControl());
            b = result.candidate().manifest().id();
            evidence = result.validation();
            var stats = evidence.statistics();
            assertEquals(2, stats.validPairs());
            assertEquals(0, stats.incompletePairs());
            assertEquals(0, stats.wins());
            assertEquals(4, stats.draws());
            assertEquals(0, stats.losses());
            assertEquals(new ValidationResult.ColourRecord(0, 2, 0), stats.white());
            assertEquals(stats.white(), stats.black());
            assertEquals(0.5, evidence.assessment().mean());
            assertEquals(PromotionPolicy.Decision.INCONCLUSIVE, evidence.assessment().decision());
            var again = new ValidationArena().validate(store.load(b).network(), store.load(a).network(), cfg,
                    board, GameHistory.initial(board), new ValidationControl());
            assertEquals(stats, again.statistics());
            assertEquals(evidence.assessment(), again.assess(PromotionPolicy.DEFAULT));
            System.out.println("REAL_PERSISTED_NNUE_SMOKE depth=1 threads=1 noTimeLimit=true " + stats + " " + evidence.assessment());
        }
        try (var store = new CheckpointStore(root)) {
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            assertEquals(b, store.recover().latestTraining().orElseThrow().manifest().id());
            assertEquals(evidence, store.readValidation(evidence.id()));
        }
    }

    @Tag("slow-nnue")
    @Test void corruptNetworkRejectsItsReferenceAndFallsBackWithoutDeletingEvidence() throws Exception {
        corruptArtifact(NETWORK_FILE, false);
    }
    @Tag("slow-nnue")
    @Test void corruptTrainingStateRejectsItsReferenceAndFallsBackWithoutDeletingEvidence() throws Exception {
        corruptArtifact(TRAINING_FILE, false);
    }
    @Tag("slow-nnue")
    @Test void corruptManifestRejectsItsReferenceAndFallsBackWithoutDeletingEvidence() throws Exception {
        corruptArtifact(MANIFEST_FILE, false);
    }
    @Tag("slow-nnue")
    @Test void truncatedNetworkRejectsItsReference() throws Exception { corruptArtifact(NETWORK_FILE, true); }
    @Tag("slow-nnue")
    @Test void truncatedTrainingRejectsItsReference() throws Exception { corruptArtifact(TRAINING_FILE, true); }
    @Tag("slow-nnue")
    @Test void truncatedManifestRejectsItsReference() throws Exception { corruptArtifact(MANIFEST_FILE, true); }

    @Tag("slow-nnue")
    @Test void partialStagingAndReferenceTempsAreNeverRecoveryCandidates() throws Exception {
        try (var store = new CheckpointStore(root)) {
            String id = store.initialize(trainer(), metadata(0, "")).manifest().id();
            Path partial = Files.createDirectory(root.resolve("staging/g999999-partial"));
            Files.copy(artifact(id, NETWORK_FILE), partial.resolve(NETWORK_FILE));
            Files.writeString(root.resolve("refs/.best-interrupted.tmp"), "partial");
            Files.delete(root.resolve("refs/best"));
            Files.delete(root.resolve("refs/latest-training"));
            var recovered = store.recover();
            assertEquals(id, recovered.best().orElseThrow().manifest().id());
            assertEquals(id, recovered.latestTraining().orElseThrow().manifest().id());
            assertTrue(Files.exists(partial.resolve(NETWORK_FILE)));
        }
    }

    @Tag("slow-nnue")
    @Test void missingAndCorruptLatestRecoverHighestValidGenerationWhileBestUsesAcceptanceOnly() throws Exception {
        try (var store = new CheckpointStore(root)) {
            String a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            String b = store.publish(trainer(), metadata(8, a)).manifest().id();
            String c = store.publish(trainer(), metadata(12, b)).manifest().id();
            Files.delete(root.resolve("refs/latest-training"));
            Files.delete(root.resolve("refs/best"));
            assertEquals(c, store.recover().latestTraining().orElseThrow().manifest().id());
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            Files.writeString(root.resolve("refs/latest-training"), "broken");
            assertEquals(c, store.recover().latestTraining().orElseThrow().manifest().id());
            var repaired = store.repairReferences();
            assertEquals(c, repaired.latestTraining().orElseThrow().manifest().id());
            assertEquals(a, repaired.best().orElseThrow().manifest().id());
            assertTrue(repaired.diagnostics().isEmpty());
        }
    }

    @Tag("slow-nnue")
    @Test void bestReferenceCannotLegitimizeAnUnpromotedCandidate() throws Exception {
        try (var store = new CheckpointStore(root)) {
            String a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            String initialRecord = store.recover().bestEvidence().orElseThrow().id();
            String b = store.publish(trainer(), metadata(1, a)).manifest().id();
            Files.write(root.resolve("refs/best"), SmallRecord.encode("reference-best", out -> {
                out.writeUTF(b); out.writeUTF(initialRecord);
            }));
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
        }
    }

    @Tag("slow-nnue")
    @Test void invalidPromotionCheckpointOrValidationCannotBecomeRecoveredBest() throws Exception {
        try (var store = new CheckpointStore(root)) {
            String a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            var result = lifecycle(store, trainer(), metadata(1, a), true, EASY);
            String b = result.candidate().manifest().id();
            byte[] validation = Files.readAllBytes(root.resolve("validations").resolve(result.validation().id()));
            Files.write(root.resolve("validations").resolve(result.validation().id()), new byte[] {7});
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            Files.write(root.resolve("validations").resolve(result.validation().id()), validation);
            Files.write(artifact(b, NETWORK_FILE), new byte[] {7});
            Files.delete(root.resolve("refs/best"));
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            assertEquals(2, count(root.resolve("promotions")));
        }
    }

    @Tag("slow-nnue")
    @Test void invalidPromotionRecordAndMissingBootstrapReportNoIncumbent() throws Exception {
        try (var store = new CheckpointStore(root)) {
            store.initialize(trainer(), metadata(0, ""));
            String bootstrap = store.recover().bestEvidence().orElseThrow().id();
            Files.write(root.resolve("promotions").resolve(bootstrap), new byte[] {1});
            assertTrue(store.recover().best().isEmpty());
            assertTrue(store.recover().latestTraining().isPresent());
            assertThrows(IOException.class, () -> store.initialize(trainer(), metadata(1, "")));
        }
    }

    @Test void trailingAndUnknownSmallRecordFormatsAreStrictlyRejectedEvenWithNewChecksum() throws Exception {
        byte[] good = SmallRecord.encode("fixture", out -> out.writeInt(42));
        assertEquals(42, SmallRecord.decode(good, "fixture", DataInputStream::readInt));
        assertThrows(IOException.class, () -> SmallRecord.decode(Arrays.copyOf(good, good.length + 1), "fixture", DataInputStream::readInt));
        byte[] unknown = good.clone();
        ByteBuffer.wrap(unknown).putInt(4, 999);
        checksumRecord(unknown);
        assertThrows(IOException.class, () -> SmallRecord.decode(unknown, "fixture", DataInputStream::readInt));
        byte[] trailingPayload = SmallRecord.encode("fixture", out -> { out.writeInt(42); out.writeByte(1); });
        assertThrows(IOException.class, () -> SmallRecord.decode(trailingPayload, "fixture", DataInputStream::readInt));
        assertThrows(IOException.class, () -> SmallRecord.decode(good, "other-kind", DataInputStream::readInt));
        assertThrows(IOException.class, () -> SmallRecord.decode(Arrays.copyOf(good, 20), "fixture", DataInputStream::readInt));
    }

    @Tag("slow-nnue")
    @Test void trailingArtifactsAndManifestAreRejected() throws Exception {
        try (var store = new CheckpointStore(root)) {
            String id = store.publish(trainer(), metadata(0, "")).manifest().id();
            for (String name : List.of(NETWORK_FILE, TRAINING_FILE, MANIFEST_FILE)) {
                Path file = artifact(id, name);
                long length = Files.size(file);
                Files.write(file, new byte[] {1}, StandardOpenOption.APPEND);
                assertThrows(IOException.class, () -> store.load(id));
                try (var channel = FileChannel.open(file, StandardOpenOption.WRITE)) { channel.truncate(length); }
            }
            assertEquals(id, store.load(id).manifest().id());
        }
    }

    @Tag("slow-nnue")
    @Test void validHashesDoNotBypassNetworkModelConsistencySchemaOrOptimizerCodecValidation() throws Exception {
        try (var store = new CheckpointStore(root)) {
            var original = store.publish(trainer(), metadata(0, "")).manifest();
            String mismatch = syntheticCheckpoint(original, NnueNetworkCodec.encode(NnueNetwork.initialized(98)), null);
            assertThrows(IOException.class, () -> store.load(mismatch), "Individually valid network and trainer differ.");
            byte[] network = Files.readAllBytes(artifact(original.id(), NETWORK_FILE));
            ByteBuffer.wrap(network).putInt(12, 99); // schema version
            checksumCodec(network);
            String schema = syntheticCheckpoint(original, network, null);
            assertThrows(IOException.class, () -> store.load(schema));
            byte[] training = Files.readAllBytes(artifact(original.id(), TRAINING_FILE));
            ByteBuffer.wrap(training).putLong(NnueBinaryFormat.HEADER_BYTES, -1);
            checksumCodec(training);
            String optimizer = syntheticCheckpoint(original, null, training);
            assertThrows(IOException.class, () -> store.load(optimizer));
            training = Files.readAllBytes(artifact(original.id(), TRAINING_FILE));
            ByteBuffer.wrap(training).putInt(NnueBinaryFormat.HEADER_BYTES + 40 + 2 * NnueBinaryFormat.PARAMETER_BYTES,
                    Float.floatToRawIntBits(-1));
            checksumCodec(training);
            String moments = syntheticCheckpoint(original, null, training);
            assertThrows(IOException.class, () -> store.load(moments));
        }
    }

    @Test void unsupportedAtomicCheckpointMoveFailsClosedAndLeavesOnlyStage() throws Exception {
        try (var store = new CheckpointStore(root, (source, destination, replace) -> {
            throw new AtomicMoveNotSupportedException(source.toString(), destination.toString(), "fixture");
        })) {
            assertThrows(IOException.class, () -> store.publish(trainer(), metadata(0, "")));
            assertEquals(0, count(root.resolve("checkpoints")));
            assertEquals(1, count(root.resolve("staging")));
            assertFalse(Files.exists(root.resolve("refs/latest-training")));
            assertTrue(store.recover().latestTraining().isEmpty());
        }
    }

    @Tag("slow-nnue")
    @Test void interruptedLatestReplacementLeavesOldValidReferenceAndNewCheckpointRecoverable() throws Exception {
        AtomicBoolean fail = new AtomicBoolean();
        try (var store = new CheckpointStore(root, (source, destination, replace) -> {
            if (fail.get() && destination.equals(root.resolve("refs/latest-training"))) throw new IOException("Injected before reference move");
            CheckpointStore.atomicMove(source, destination, replace);
        })) {
            String a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            fail.set(true);
            assertThrows(IOException.class, () -> store.publish(trainer(), metadata(1, a)));
            assertEquals(2, count(root.resolve("checkpoints")));
            assertEquals(a, store.recover().latestTraining().orElseThrow().manifest().id(), "Old complete reference wins.");
            Files.delete(root.resolve("refs/latest-training"));
            assertEquals(1, store.recover().latestTraining().orElseThrow().manifest().generation());
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            fail.set(false);
            store.repairReferences();
            assertEquals(1, store.recover().latestTraining().orElseThrow().manifest().generation());
        }
    }

    @Tag("slow-nnue")
    @Test void interruptedPromotionRecordCannotAdvanceBest() throws Exception {
        interruptedPromotion(false);
    }
    @Tag("slow-nnue")
    @Test void promotionRecordBeforeFailedBestReplaceKeepsOldExplicitIncumbentAuthoritative() throws Exception {
        interruptedPromotion(true);
    }

    @Tag("slow-nnue")
    @Test void newBestAfterAtomicMoveHasProvableEvidenceEvenIfCallerSeesLaterFailure() throws Exception {
        AtomicBoolean fail = new AtomicBoolean();
        String b;
        try (var store = new CheckpointStore(root, (source, destination, replace) -> {
            CheckpointStore.atomicMove(source, destination, replace);
            if (fail.get() && destination.equals(root.resolve("refs/best"))) throw new IOException("Injected after atomic reference move");
        })) {
            String a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            b = store.publish(trainer(), metadata(1, a)).manifest().id();
            var validation = store.recordValidation(b, a, result(true, config(1)), EASY);
            fail.set(true);
            assertThrows(IOException.class, () -> store.promote(validation.id()));
            assertEquals(b, store.recover().best().orElseThrow().manifest().id());
            assertEquals(validation.id(), store.recover().bestEvidence().orElseThrow().validationId());
        }
        try (var store = new CheckpointStore(root)) {
            assertEquals(b, store.recover().best().orElseThrow().manifest().id());
        }
    }

    @Test void simultaneousStoreOwnersAreRejectedAndClosedStoreCannotMutate() throws Exception {
        var store = new CheckpointStore(root);
        assertThrows(IOException.class, () -> new CheckpointStore(root));
        store.close();
        assertThrows(IOException.class, store::recover);
        try (var reopened = new CheckpointStore(root)) { assertTrue(reopened.recover().best().isEmpty()); }
    }

    @Tag("slow-nnue")
    @Test void duplicateConflictingOrCorruptCheckpointIsNeverOverwritten() throws Exception {
        try (var store = new CheckpointStore(root)) {
            String id = store.publish(trainer(), metadata(0, "")).manifest().id();
            byte[] corrupt = {1, 2, 3};
            Files.write(artifact(id, MANIFEST_FILE), corrupt);
            assertThrows(IOException.class, () -> store.publish(trainer(), metadata(0, "")));
            assertArrayEquals(corrupt, Files.readAllBytes(artifact(id, MANIFEST_FILE)));
            assertEquals(1, count(root.resolve("checkpoints")));
        }
    }

    private void corruptArtifact(String name, boolean truncate) throws Exception {
        try (var store = new CheckpointStore(root)) {
            String a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            String b = store.publish(trainer(), metadata(1, a)).manifest().id();
            Path file = artifact(b, name);
            if (truncate) {
                try (var channel = FileChannel.open(file, StandardOpenOption.WRITE)) { channel.truncate(Files.size(file) / 2); }
            } else {
                try (var random = new RandomAccessFile(file.toFile(), "rw")) {
                    random.seek(20); int value = random.read(); random.seek(20); random.write(value ^ 1);
                }
            }
            assertThrows(IOException.class, () -> store.load(b));
            assertEquals(a, store.recover().latestTraining().orElseThrow().manifest().id());
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            assertTrue(Files.exists(file));
            assertFalse(store.recover().diagnostics().isEmpty());
        }
    }

    private void interruptedPromotion(boolean atBestReference) throws Exception {
        AtomicBoolean fail = new AtomicBoolean();
        String a, b;
        try (var store = new CheckpointStore(root, (source, destination, replace) -> {
            if (fail.get() && (atBestReference ? destination.equals(root.resolve("refs/best"))
                    : destination.getParent().equals(root.resolve("promotions")))) throw new IOException("Injected interruption");
            CheckpointStore.atomicMove(source, destination, replace);
        })) {
            a = store.initialize(trainer(), metadata(0, "")).manifest().id();
            b = store.publish(trainer(), metadata(1, a)).manifest().id();
            var validation = store.recordValidation(b, a, result(true, config(1)), EASY);
            fail.set(true);
            assertThrows(IOException.class, () -> store.promote(validation.id()));
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            assertEquals(b, store.recover().latestTraining().orElseThrow().manifest().id());
            assertEquals(atBestReference ? 2 : 1, count(root.resolve("promotions")));
        }
        try (var store = new CheckpointStore(root)) {
            assertEquals(a, store.recover().best().orElseThrow().manifest().id());
            if (atBestReference) {
                Files.delete(root.resolve("refs/best"));
                assertEquals(b, store.recover().best().orElseThrow().manifest().id(), "Missing reference uses valid durable promotion history.");
            }
        }
    }

    private CandidateLifecycle.Result lifecycle(CheckpointStore store, NnueTrainer candidate, Metadata metadata,
                                                boolean promote, PromotionPolicy policy) throws Exception {
        var cfg = config(1);
        byte[] expectedCandidate = NnueNetworkCodec.encode(candidate.model().snapshot());
        String incumbentId = store.recover().best().orElseThrow().manifest().id();
        byte[] expectedIncumbent = Files.readAllBytes(artifact(incumbentId, NETWORK_FILE));
        return CandidateLifecycle.run(store, candidate, metadata, cfg, policy, BOARD, GameHistory.initial(BOARD),
                new ValidationControl(), (persistedCandidate, persistedIncumbent) -> {
                    try {
                        assertArrayEquals(expectedCandidate, NnueNetworkCodec.encode(persistedCandidate));
                        assertArrayEquals(expectedIncumbent, NnueNetworkCodec.encode(persistedIncumbent));
                    } catch (IOException failure) { throw new UncheckedIOException(failure); }
                    return result(promote, cfg);
                });
    }

    private static ValidationResult result(boolean promote, ValidationConfig config) {
        var a = new ValidationResult.Game(GameTermination.WHITE_CHECKMATES_BLACK, 7);
        var b = new ValidationResult.Game(promote ? GameTermination.BLACK_CHECKMATES_WHITE : GameTermination.WHITE_CHECKMATES_BLACK, 4);
        return new ValidationResult(config, ValidationArena.stateHash(BOARD, GameHistory.initial(BOARD)),
                Collections.nCopies(config.openingPairs(), new ValidationResult.Pair("fixture", a, b)));
    }
    private static ValidationConfig config(int pairs) {
        return new ValidationConfig(pairs, 512, 0, 0, 1, 1, new NnueScoreMapping(1_000_000), 16);
    }
    private static NnueTrainer trainer() {
        return new NnueTrainer(TrainableNnue.fromNetwork(NETWORK), new AdamHyperparameters(0.007, 0.8, 0.95, 1e-7));
    }
    private static void train(NnueTrainer trainer) { trainer.trainBatch(new long[][] {BOARD}, new double[] {0.8}, 1); }
    private static Metadata metadata(long generation, String parent) { return new Metadata(generation, 1, parent); }
    private Path artifact(String id, String name) { return root.resolve("checkpoints").resolve(id).resolve(name); }
    private static long count(Path directory) throws IOException {
        try (var entries = Files.list(directory)) { return entries.count(); }
    }
    private String syntheticCheckpoint(CheckpointManifest original, byte[] network, byte[] training) throws Exception {
        String networkHash = network == null ? original.networkSha256() : SmallRecord.hash(network);
        String trainingHash = training == null ? original.trainingSha256() : SmallRecord.hash(training);
        var manifest = CheckpointManifest.create(metadata(0, ""), original.optimizerStep(), networkHash, trainingHash);
        Path directory = Files.createDirectory(root.resolve("checkpoints").resolve(manifest.id()));
        if (network == null) Files.copy(artifact(original.id(), NETWORK_FILE), directory.resolve(NETWORK_FILE));
        else Files.write(directory.resolve(NETWORK_FILE), network);
        if (training == null) Files.copy(artifact(original.id(), TRAINING_FILE), directory.resolve(TRAINING_FILE));
        else Files.write(directory.resolve(TRAINING_FILE), training);
        Files.write(directory.resolve(MANIFEST_FILE), manifest.encode());
        return manifest.id();
    }
    private static void checksumRecord(byte[] bytes) {
        byte[] hash = SmallRecord.digest().digest(Arrays.copyOf(bytes, bytes.length - 32));
        System.arraycopy(hash, 0, bytes, bytes.length - 32, 32);
    }
    private static void checksumCodec(byte[] bytes) {
        CRC32 crc = new CRC32(); crc.update(bytes, 0, bytes.length - 4);
        ByteBuffer.wrap(bytes).putInt(bytes.length - 4, (int) crc.getValue());
    }
}
