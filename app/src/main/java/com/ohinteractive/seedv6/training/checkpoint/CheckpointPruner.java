package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import static com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.*;

/** Single-owner lifecycle maintenance. All planning/deletion also excludes cross-process payload readers. */
final class CheckpointPruner {
    // Bound costly full-payload verification and reclamation during a backlog catch-up.
    static final int MAX_CHECKPOINTS = 8;
    record Result(int checkpoints, int files, long bytes, List<String> failures) {
        Result { failures = List.copyOf(failures); }
    }
    @FunctionalInterface interface Remover { void delete(Path path) throws IOException; }

    static Result prune(CheckpointStore store, String completedId) {
        return prune(store, completedId, Files::delete);
    }

    static Result prune(CheckpointStore store, String completedId, Remover remover) {
        int checkpoints = 0, files = 0, attempted = 0;
        long bytes = 0;
        List<String> failures = new ArrayList<>();
        Path root = store.root();
        try (var access = PayloadAccess.acquire(root)) {
            if (!Files.isDirectory(root.resolve("checkpoints"), LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Checkpoint directory is not a regular store directory.");
            var completed = CheckpointStore.historicalManifest(root, completedId);
            if (completed.generation() < 100) return new Result(0, 0, 0, failures);
            // Require explicit, settled references. Never infer a completion from mere publication.
            if (!CheckpointInspection.reference(root, "latest-training").equals(completedId))
                throw new IOException("Pruning deferred: completed checkpoint is not latest-training.");
            var recovered = store.recover();
            if (!recovered.diagnostics().isEmpty()) throw new IOException("Pruning deferred: " + recovered.diagnostics());
            var best = recovered.bestEvidence().orElseThrow(() -> new IOException("No accepted Best."));
            var validations = CheckpointInspection.validations(root);
            var decision = validations.get(completedId);
            if (decision == null || (decision.assessment().decision() == PromotionPolicy.Decision.PROMOTE
                    ? !best.checkpointId().equals(completedId) : !best.checkpointId().equals(decision.incumbentId())))
                throw new IOException("Pruning deferred: candidate decision is unresolved.");

            Set<String> protectedIds = new HashSet<>(Set.of(completedId));
            protectedIds.add(best.checkpointId());
            if (!best.previousCheckpointId().isEmpty()) protectedIds.add(best.previousCheckpointId());
            Set<String> accepted = new HashSet<>(), acceptedRecords = new HashSet<>();
            PromotionRecord cursor = best;
            while (true) {
                accepted.add(cursor.checkpointId()); acceptedRecords.add(cursor.id());
                if (cursor.kind() == PromotionRecord.Kind.BOOTSTRAP) break;
                cursor = CheckpointStore.readPromotion(root, cursor.previousRecordId());
            }
            // Evidence not yet reached by the explicit Best reference is pending, never historical garbage.
            try (var paths = Files.newDirectoryStream(root.resolve("promotions"))) {
                for (Path path : paths) {
                    var promotion = CheckpointStore.readPromotion(root, path.getFileName().toString());
                    if (!acceptedRecords.contains(promotion.id())) {
                        protectedIds.add(promotion.checkpointId());
                        protectedIds.add(promotion.previousCheckpointId());
                    }
                }
            }
            for (var validation : validations.values()) {
                if (validation.assessment().decision() == PromotionPolicy.Decision.PROMOTE
                        && !accepted.contains(validation.candidateId())) {
                    protectedIds.add(validation.candidateId()); protectedIds.add(validation.incumbentId());
                }
            }
            // Only proven members of this completed training lineage can be retired. Unresolved
            // candidates (no decision/acceptance), other branches, unknown files and stages are untouched.
            for (var manifest : CheckpointInspection.lineage(root, completedId)) {
                if (protectedIds.contains(manifest.id())
                        || GenerationRetention.retain(manifest.generation(), completed.generation())
                        || (!validations.containsKey(manifest.id()) && !accepted.contains(manifest.id()))) continue;
                Path directory = root.resolve("checkpoints").resolve(manifest.id());
                boolean pruned = CheckpointPayload.pruned(directory, manifest);
                if (pruned && !Files.exists(directory.resolve(NETWORK_FILE), LinkOption.NOFOLLOW_LINKS)
                        && !Files.exists(directory.resolve(TRAINING_FILE), LinkOption.NOFOLLOW_LINKS)) continue;
                if (attempted++ == MAX_CHECKPOINTS) break;
                try {
                    if (!pruned) {
                        // Independently validate codecs, both hashes, model equality and exact Adam state
                        // before retiring anything or extracting configuration from a legacy checkpoint.
                        var trainer = store.resume(manifest.id());
                        store.publishRecord("checkpoints/" + manifest.id(), CheckpointPayload.CONFIG,
                                CheckpointPayload.configuration(manifest, trainer.optimizer().hyperparameters()));
                        store.publishRecord("checkpoints/" + manifest.id(), CheckpointPayload.PRUNED,
                                CheckpointPayload.pruningRecord(directory, manifest, completed.generation()));
                    }
                    // The forced, atomic marker already makes all readers refuse this payload. A crash
                    // between deletes leaves a diagnosable PRUNED state, retried at the next boundary.
                    for (String name : List.of(NETWORK_FILE, TRAINING_FILE)) {
                        Path payload = directory.resolve(name);
                        if (!Files.exists(payload, LinkOption.NOFOLLOW_LINKS)) continue;
                        CheckpointPayload.regular(payload);
                        long size = Files.size(payload);
                        long expected = name.equals(NETWORK_FILE) ? manifest.networkBytes() : manifest.trainingBytes();
                        String hash = name.equals(NETWORK_FILE) ? manifest.networkSha256() : manifest.trainingSha256();
                        if (size != expected || !SmallRecord.hash(payload).equals(hash))
                            throw new IOException("Refusing to delete changed/unknown payload: " + payload);
                        remover.delete(payload); files++; bytes += size;
                    }
                    checkpoints++;
                } catch (IOException | RuntimeException failure) {
                    failures.add(manifest.id() + ": " + failure);
                }
            }
        } catch (IOException | RuntimeException failure) { failures.add(failure.toString()); }
        if (files > 0) System.err.println("NNUE retention: removed " + files + " payload files (" + bytes + " bytes).");
        for (String failure : failures) System.err.println("NNUE retention cleanup FAILED; retry at a later completed generation: " + failure);
        return new Result(checkpoints, files, bytes, failures);
    }

    private CheckpointPruner() {}
}
