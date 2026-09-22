package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.Path;
import com.ohinteractive.seedv6.training.service.*;

/** Durable generation pin. It references the external NNUE checkpoint; it contains no NNUE payload. */
public record BootstrapPlan(String parentId, String incumbentId, long generation, TrainingSource source,
                            String generatorId, String generatorHash, String settings, long splitSeed) {
    public BootstrapPlan {
        CheckpointManifest.requireId(parentId); CheckpointManifest.requireId(incumbentId);
        CheckpointManifest.requireId(generatorId); SmallRecord.requireHash(generatorHash);
        if (generation < 1 || !source.bootstrap() || source.generatorStore().isEmpty() || settings.isEmpty())
            throw new IllegalArgumentException("Invalid bootstrap plan.");
    }
    public static BootstrapPlan create(String parent, String best, long generation, TrainerConfig config,
                                       TrainingSource source, CheckpointStore.Checkpoint generator) {
        return new BootstrapPlan(parent, best, generation, source, generator.manifest().id(),
                generator.manifest().networkSha256(), config.generationSettings(generation),
                config.seed(generation, TrainerConfig.SeedDomain.HOLDOUT));
    }
    public void requireSettings(TrainerConfig config, TrainingSource selected) throws IOException {
        if (!source.equals(selected) || !settings.equals(config.generationSettings(generation)))
            throw new IOException("Bootstrap pin differs from the reconciled generation settings; existing artifacts were preserved.");
    }
    public CheckpointStore.Checkpoint loadGenerator(Path student) throws IOException {
        Path root = source.requireGenerator(student);
        try {
            var pinned = TrainingSource.requireNnue(CheckpointStore.readSnapshot(root, generatorId));
            if (!pinned.manifest().networkSha256().equals(generatorHash)) throw new IOException("Pinned NNUE checksum changed.");
            return pinned;
        } catch (IOException unavailable) {
            throw new IOException("Pinned NNUE generator " + generatorId + " is unavailable at " + root
                    + "; restore that checkpoint to resume. No fallback was selected.", unavailable);
        }
    }
    byte[] encode() throws IOException {
        return SmallRecord.encode("brn-bootstrap-plan-v1", out -> {
            out.writeUTF(parentId); out.writeUTF(incumbentId); out.writeLong(generation);
            out.writeUTF(source.generatorStore()); out.writeUTF(generatorId); out.writeUTF(generatorHash);
            out.writeUTF(settings); out.writeLong(splitSeed);
        });
    }
    static BootstrapPlan read(Path path) throws IOException {
        return SmallRecord.read(path, "brn-bootstrap-plan-v1", in -> new BootstrapPlan(in.readUTF(), in.readUTF(),
                in.readLong(), TrainingSource.bootstrap(Path.of(in.readUTF())), in.readUTF(), in.readUTF(), in.readUTF(), in.readLong()));
    }
    public String hash() throws IOException { return SmallRecord.hash(encode()); }
}
