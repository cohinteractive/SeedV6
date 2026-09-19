package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Read-only durable metadata. Does not create a lock file or repair references; use a stopped store. */
public final class CheckpointInspection {
    private record Reference(String checkpoint, String evidence) {}
    public static CheckpointManifest manifest(Path checkpoint) throws IOException {
        var result = SmallRecord.read(checkpoint.resolve(CheckpointManifest.MANIFEST_FILE), "manifest", CheckpointManifest::read);
        if (!result.id().equals(checkpoint.getFileName().toString())) throw new IOException("Manifest directory mismatch.");
        return result;
    }

    public static String reference(Path root, String name) throws IOException {
        return readReference(root, name).checkpoint();
    }
    private static Reference readReference(Path root, String name) throws IOException {
        if (!Set.of("best", "latest-training").contains(name)) throw new IllegalArgumentException("Unknown reference.");
        return SmallRecord.read(root.resolve("refs").resolve(name), "reference-" + name, in -> {
            String id = CheckpointManifest.requireId(in.readUTF());
            String evidence = in.readUTF();
            if (!evidence.isEmpty()) PromotionRecord.requireId(evidence);
            return new Reference(id, evidence);
        });
    }

    public static List<CheckpointManifest> lineage(Path root, String latest) throws IOException {
        List<CheckpointManifest> reverse = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String id = latest;
        long childGeneration = Long.MAX_VALUE;
        while (!id.isEmpty()) {
            if (!seen.add(id)) throw new IOException("Cyclic lineage.");
            var item = manifest(root.resolve("checkpoints").resolve(CheckpointManifest.requireId(id)));
            if (item.generation() >= childGeneration) throw new IOException("Unordered lineage.");
            reverse.add(item);
            childGeneration = item.generation();
            id = item.parentId();
        }
        Collections.reverse(reverse);
        return List.copyOf(reverse);
    }

    public static Map<String, ValidationRecord> validations(Path root) throws IOException {
        Map<String, ValidationRecord> result = new TreeMap<>();
        try (var paths = Files.list(root.resolve("validations"))) {
            for (Path p : paths.toList()) {
                String id = ValidationRecord.requireId(p.getFileName().toString());
                if (!id.equals("v-" + SmallRecord.hash(p))) throw new IOException("Validation identity mismatch.");
                var record = SmallRecord.read(p, "validation", in -> ValidationRecord.read(id, in));
                if (result.put(record.candidateId(), record) != null) throw new IOException("Ambiguous validations.");
            }
        }
        return Map.copyOf(result);
    }

    public static Set<String> accepted(Path root) throws IOException {
        Set<String> result = new HashSet<>();
        // A published promotion record may still be pending best-reference publication after a crash.
        // Only the chain reached from the explicit best reference is accepted here.
        Reference best = readReference(root, "best");
        String id = best.evidence(), expected = best.checkpoint();
        long nextSequence = -1;
        Set<String> seen = new HashSet<>();
        var validations = validations(root);
        while (true) {
            PromotionRecord.requireId(id);
            if (!seen.add(id)) throw new IOException("Cyclic acceptance chain.");
            Path p = root.resolve("promotions").resolve(id);
            if (!id.equals("p-" + SmallRecord.hash(p))) throw new IOException("Promotion identity mismatch.");
            String recordId = id;
            var record = SmallRecord.read(p, "promotion", in -> PromotionRecord.read(recordId, in));
            if (!record.checkpointId().equals(expected) || (nextSequence >= 0 && record.sequence() != nextSequence)) {
                throw new IOException("Broken acceptance chain.");
            }
            if (record.kind() == PromotionRecord.Kind.PROMOTION) {
                var validation = validations.get(record.checkpointId());
                if (validation == null || !validation.id().equals(record.validationId())
                        || !validation.incumbentId().equals(record.previousCheckpointId())
                        || validation.assessment().decision() != com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.PROMOTE) {
                    throw new IOException("Unsupported promotion.");
                }
            }
            result.add(record.checkpointId());
            if (record.kind() == PromotionRecord.Kind.BOOTSTRAP) break;
            expected = record.previousCheckpointId(); id = record.previousRecordId(); nextSequence = record.sequence() - 1;
        }
        return Set.copyOf(result);
    }
    private CheckpointInspection() {}
}
