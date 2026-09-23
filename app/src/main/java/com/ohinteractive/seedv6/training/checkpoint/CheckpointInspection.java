package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Read-only durable metadata. Does not create a lock file or repair references; use a stopped store. */
public final class CheckpointInspection {
    static final String BOOTSTRAP_IDENTITY = "bootstrap-identity.bin";
    /** Read-only root recognition. Incidental files do not erase a checksummed store identity.
     * No name-only inference from history/training directories or empty coordination files.
     */
    public static boolean freshRoot(Path root, com.ohinteractive.seedv6.training.model.TrainingArchitecture requested) throws IOException {
        if (Files.notExists(root)) return true;
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Checkpoint root must be a directory: " + root);
        try (var entries = Files.list(root)) { if (entries.findAny().isEmpty()) return true; }
        boolean identified = false;
        Path checkpoints = root.resolve("checkpoints");
        if (Files.isDirectory(checkpoints, LinkOption.NOFOLLOW_LINKS)) {
            try (var entries = Files.list(checkpoints)) {
                for (Path entry : entries.toList()) {
                    CheckpointManifest manifest;
                    try { manifest = manifest(entry); }
                    catch (com.ohinteractive.seedv6.training.model.TrainingArchitecture.IncompatibleEncodingException incompatible) { throw incompatible; }
                    catch (IOException invalid) { continue; } // Full recovery reports invalid siblings after recognition.
                    if (manifest.architecture() != requested) throw new IOException("Checkpoint architecture mismatch: selected "
                            + requested + ", store contains " + manifest.architecture() + ". Existing data was preserved.");
                    identified = true;
                }
            }
        }
        if (identified) return false;
        Path identity = root.resolve(BOOTSTRAP_IDENTITY);
        if (Files.isRegularFile(identity, LinkOption.NOFOLLOW_LINKS)) {
            var architecture = SmallRecord.read(identity, "bootstrap-identity-v1",
                    in -> com.ohinteractive.seedv6.training.model.TrainingArchitecture.valueOf(in.readUTF()));
            if (architecture != requested) throw new IOException("Checkpoint architecture mismatch: selected "
                    + requested + ", store contains " + architecture + ". Existing data was preserved.");
            // This release's interrupted initialization has an authenticated intent and only empty
            // scaffolding. History cannot exist before bootstrap. Unknown content is never adopted.
            try (var paths = Files.list(root)) {
                for (Path path : paths.toList()) {
                    String name = path.getFileName().toString();
                    if (name.equals(BOOTSTRAP_IDENTITY)) continue;
                    if (name.equals(CheckpointStore.BRN_TEACHER_FILE) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        if (requested != com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2)
                            throw new IOException("BRN teacher metadata requires BRN-2.");
                        CheckpointStore.readBrnTeacherStore(root).orElseThrow(); continue;
                    }
                    if (name.equals(FrozenReplay.FILE) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        if (requested != com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2)
                            throw new IOException("Frozen replay metadata requires BRN-2.");
                        FrozenReplay.read(root).orElseThrow(); continue;
                    }
                    if (name.equals(CheckpointStore.BRN_RUN_SEEDS_FILE) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        if (requested != com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2)
                            throw new IOException("BRN run seed metadata requires BRN-2.");
                        CheckpointStore.readBrnRunSeeds(root).orElseThrow();
                        continue;
                    }
                    if (name.equals(CheckpointStore.BRN_SUPERVISION_FILE) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        if (requested != com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2)
                            throw new IOException("BRN supervision metadata requires BRN-2.");
                        CheckpointStore.readBrnSupervision(root).orElseThrow();
                        continue;
                    }
                    if (name.equals(CheckpointStore.TRAINING_SOURCE_FILE) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                        var source = CheckpointStore.readTrainingSource(root).orElseThrow();
                        if (requested == com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE && source.bootstrap())
                            throw new IOException("NNUE cannot be a bootstrap student.");
                        continue;
                    }
                    if (Set.of("store.lock", "payload.lock").contains(name)
                            && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && Files.size(path) == 0) continue;
                    if (Set.of("checkpoints", "staging", "validations", "promotions", "refs").contains(name)
                            && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                        try (var children = Files.list(path)) { if (children.findAny().isEmpty()) continue; }
                    }
                    throw new IOException("Incomplete checkpoint bootstrap contains unrecognized artifacts: " + path
                            + ". Existing data was preserved; select an empty folder.");
                }
            }
            return true;
        }
        throw new IOException("This non-empty folder has no valid checkpoint store identity: " + root
                + ". Select an empty folder or an existing training store. History/training folders alone are not store identity; existing data was preserved.");
    }

    private record Reference(String checkpoint, String evidence) {}
    /** Read a persisted replay pin without acquiring a store writer or performing recovery. */
    public static BootstrapPlan bootstrapPlan(Path root, String parent) throws IOException {
        CheckpointManifest.requireId(parent);
        var plan = BootstrapPlan.read(root.resolve("bootstrap").resolve(parent + ".plan"));
        if (!plan.parentId().equals(parent)
                || plan.generation() != CheckpointStore.historicalManifest(root, parent).generation() + 1)
            throw new IOException("Bootstrap plan parent/generation mismatch.");
        return plan;
    }
    /** Read the exact persisted partition; never regenerate or repartition samples. */
    public static BootstrapData bootstrapData(Path root, BootstrapPlan plan) throws IOException {
        var data = BootstrapData.read(root.resolve("bootstrap").resolve(plan.parentId() + ".data"));
        if (!data.planHash().equals(plan.hash())) throw new IOException("Bootstrap data/plan mismatch.");
        return data;
    }
    public static CheckpointManifest manifest(Path checkpoint) throws IOException {
        if (!Files.isDirectory(checkpoint, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing checkpoint directory.");
        CheckpointPayload.regular(checkpoint.resolve(CheckpointManifest.MANIFEST_FILE));
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
        long childGeneration = -1;
        while (!id.isEmpty()) {
            if (!seen.add(id)) throw new IOException("Cyclic lineage.");
            var item = CheckpointStore.historicalManifest(root, id);
            if (childGeneration >= 0 && item.generation() >= childGeneration) throw new IOException("Unordered lineage.");
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
            if (CheckpointStore.historicalManifest(root, record.checkpointId()).generation() != record.generation())
                throw new IOException("Promotion generation mismatch.");
            if (!record.checkpointId().equals(expected) || (nextSequence >= 0 && record.sequence() != nextSequence)) {
                throw new IOException("Broken acceptance chain.");
            }
            if (record.kind() == PromotionRecord.Kind.PROMOTION) {
                var validation = validations.get(record.checkpointId());
                if (validation == null || !validation.id().equals(record.validationId())
                        || !validation.incumbentId().equals(record.previousCheckpointId())
                        || validation.decision() != com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.PROMOTE) {
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
