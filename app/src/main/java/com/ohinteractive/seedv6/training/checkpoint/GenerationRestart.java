package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.*;

/** Forced intent, atomic archival, reference reconciliation, replacement attempt. No payload deletion. */
final class GenerationRestart {
    static final String PENDING = "generation-restart.bin";
    private record Intent(String archive, String candidate, GenerationAttempt replacement, Map<String,String> artifacts) {
        Intent {
            UUID.fromString(archive);
            if (!candidate.isEmpty()) requireId(candidate);
            String parent = replacement.parentId();
            Set<String> allowed = new HashSet<>(List.of(GenerationAttempt.FILE,
                    "bootstrap/" + parent + ".plan", "bootstrap/" + parent + ".data"));
            if (!candidate.isEmpty()) allowed.add("checkpoints/" + candidate);
            if (!allowed.containsAll(artifacts.keySet())) throw new IllegalArgumentException("Invalid restart artifact path.");
            artifacts.values().forEach(SmallRecord::requireHash);
            artifacts = Collections.unmodifiableMap(new TreeMap<>(artifacts));
        }
        byte[] encode() throws IOException {
            return SmallRecord.encode("generation-restart-v1", out -> {
                out.writeUTF(archive); out.writeUTF(candidate); replacement.write(out); out.writeInt(artifacts.size());
                for (var item : artifacts.entrySet()) { out.writeUTF(item.getKey()); out.writeUTF(item.getValue()); }
            });
        }
        static Intent read(Path file) throws IOException {
            return SmallRecord.read(file, "generation-restart-v1", in -> {
                String archive = in.readUTF(), candidate = in.readUTF(); var replacement = GenerationAttempt.read(in);
                int count = in.readInt(); if (count < 0 || count > 4) throw new IOException("Invalid restart artifact count.");
                Map<String,String> artifacts = new TreeMap<>();
                for (int i = 0; i < count; i++) if (artifacts.put(in.readUTF(), in.readUTF()) != null)
                    throw new IOException("Duplicate restart artifact.");
                return new Intent(archive, candidate, replacement, artifacts);
            });
        }
    }
    static void start(CheckpointStore store, GenerationAttempt previous, GenerationAttempt replacement, String candidate) throws IOException {
        if (!previous.parentId().equals(replacement.parentId()) || !previous.incumbentId().equals(replacement.incumbentId())
                || previous.generation() != replacement.generation()) throw new IOException("Restart changed the settled boundary.");
        verifyBoundary(store, replacement, candidate);
        if (!candidate.isEmpty()) {
            var manifest = store.load(candidate).manifest();
            if (!manifest.parentId().equals(previous.parentId()) || manifest.generation() != previous.generation())
                throw new IOException("Candidate is not exclusively part of the unfinished generation.");
        }
        Map<String,String> artifacts = new TreeMap<>();
        var active = store.generationAttempt();
        if (active.isPresent() && !active.get().equals(previous)) throw new IOException("Generation attempt changed.");
        add(store.root(), artifacts, GenerationAttempt.FILE);
        String prefix = "bootstrap/" + previous.parentId();
        var plan = store.bootstrapPlan(previous.parentId());
        if (plan.isPresent()) {
            if (!plan.get().incumbentId().equals(previous.incumbentId())) throw new IOException("Bootstrap incumbent changed.");
            store.bootstrapData(plan.get()); // Verify existing sample envelope before archival.
            add(store.root(), artifacts, prefix + ".plan"); add(store.root(), artifacts, prefix + ".data");
        } else if (Files.exists(store.root().resolve(prefix + ".data"))) throw new IOException("Orphan bootstrap data; restart refused.");
        if (!candidate.isEmpty()) add(store.root(), artifacts, "checkpoints/" + candidate);
        var intent = new Intent(UUID.randomUUID().toString(), candidate, replacement, artifacts);
        store.publishRecord("", PENDING, intent.encode());
        recover(store);
    }
    static void recover(CheckpointStore store) throws IOException {
        Path root = store.root(), pending = root.resolve(PENDING);
        if (Files.notExists(pending)) return;
        var intent = Intent.read(pending);
        verifyBoundary(store, intent.replacement(), intent.candidate());
        Path archive = root.resolve("restarted-generations").resolve(intent.archive());
        try (var access = PayloadAccess.acquire(root)) {
            for (var item : intent.artifacts().entrySet()) {
                Path from = root.resolve(item.getKey()), to = archive.resolve(item.getKey());
                if (Files.exists(to, LinkOption.NOFOLLOW_LINKS)) {
                    requireHash(to, item.getValue());
                    // A retry after replacement publication sees the new active attempt here.
                    if (Files.exists(from, LinkOption.NOFOLLOW_LINKS)
                            && !(item.getKey().equals(GenerationAttempt.FILE)
                            && GenerationAttempt.read(from).equals(intent.replacement())))
                        throw new IOException("Conflicting source after generation archival: " + from);
                } else {
                    requireHash(from, item.getValue());
                    archiveDirectory(root, to.getParent());
                    store.moveGenerationArtifact(from, to);
                }
            }
            store.restoreGenerationParent(intent.replacement().parentId());
            store.writeTrainingSource(intent.replacement().source());
            store.writeGenerationAttempt(intent.replacement());
            archiveDirectory(root, archive);
            store.moveGenerationArtifact(pending, archive.resolve(PENDING));
        }
    }
    private static void verifyBoundary(CheckpointStore store, GenerationAttempt attempt, String candidate) throws IOException {
        String latest = CheckpointInspection.reference(store.root(), "latest-training");
        if (!latest.equals(attempt.parentId()) && !latest.equals(candidate)) throw new IOException("Latest training changed; restart refused.");
        var parent = store.load(attempt.parentId()).manifest();
        if (parent.generation() + 1 != attempt.generation()) throw new IOException("Restart generation/parent mismatch.");
        var best = CheckpointStore.readBestSnapshot(store.root());
        if (!best.manifest().id().equals(attempt.incumbentId())) throw new IOException("Accepted Best changed; restart refused.");
        if (!candidate.isEmpty() && store.validationFor(candidate).isPresent())
            throw new IOException("A durable Candidate decision must be completed, never abandoned.");
        if (!parent.id().equals(best.manifest().id()) && store.validationFor(parent.id()).isEmpty())
            throw new IOException("Restart parent has no settled decision.");
    }
    private static void archiveDirectory(Path root, Path directory) throws IOException {
        for (Path path = directory; !path.equals(root); path = path.getParent()) {
            if (path == null || !path.startsWith(root)) throw new IOException("Archive outside store.");
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Archive directory is not a regular store directory: " + path);
        }
        Files.createDirectories(directory);
    }
    private static void add(Path root, Map<String,String> artifacts, String relative) throws IOException {
        Path path = root.resolve(relative);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) artifacts.put(relative, hash(path));
    }
    private static void requireHash(Path path, String expected) throws IOException {
        if (!hash(path).equals(expected)) throw new IOException("Restart artifact changed: " + path);
    }
    private static String hash(Path path) throws IOException {
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            CheckpointPayload.regular(path); return SmallRecord.hash(path);
        }
        var manifest = CheckpointInspection.manifest(path);
        Set<String> required = Set.of(MANIFEST_FILE, TRAINING_FILE, manifest.networkFile());
        Set<String> allowed = new HashSet<>(required); allowed.add(CheckpointPayload.CONFIG);
        Map<String,String> hashes = new TreeMap<>();
        try (var entries = Files.list(path)) {
            for (Path entry : entries.toList()) {
                String name = entry.getFileName().toString();
                if (!allowed.contains(name)) throw new IOException("Unrecognized Candidate artifact; preserved: " + entry);
                CheckpointPayload.regular(entry); hashes.put(name, SmallRecord.hash(entry));
            }
        }
        if (!hashes.keySet().containsAll(required)) throw new IOException("Incomplete Candidate; restart refused.");
        return SmallRecord.hash(hashes.toString().getBytes(StandardCharsets.UTF_8));
    }
    private GenerationRestart() {}
}
