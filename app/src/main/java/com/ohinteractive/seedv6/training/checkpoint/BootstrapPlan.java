package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.Path;
import com.ohinteractive.seedv6.training.service.*;

/** Durable generation pin. It references the external NNUE checkpoint; it contains no NNUE payload. */
public record BootstrapPlan(String parentId, String incumbentId, long generation, TrainingSource source,
                            String generatorId, String generatorHash, String settings, long splitSeed, BrnSupervision supervision) {
    public BootstrapPlan {
        java.util.Objects.requireNonNull(supervision);
        CheckpointManifest.requireId(parentId); CheckpointManifest.requireId(incumbentId);
        CheckpointManifest.requireId(generatorId); SmallRecord.requireHash(generatorHash);
        if (generation < 1 || !source.bootstrap() || source.generatorStore().isEmpty() || settings.isEmpty())
            throw new IllegalArgumentException("Invalid bootstrap plan.");
    }
    public BootstrapPlan(String parentId, String incumbentId, long generation, TrainingSource source,
                         String generatorId, String generatorHash, String settings, long splitSeed) {
        this(parentId, incumbentId, generation, source, generatorId, generatorHash, settings, splitSeed, BrnSupervision.WDL);
    }
    public static BootstrapPlan create(String parent, String best, long generation, TrainerConfig config,
                                       TrainingSource source, CheckpointStore.Checkpoint generator) {
        return new BootstrapPlan(parent, best, generation, source, generator.manifest().id(),
                generator.manifest().networkSha256(), config.generationSettings(generation),
                config.seed(generation, TrainerConfig.SeedDomain.HOLDOUT),
                config.supervision() == null ? BrnSupervision.WDL : config.supervision());
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
        return SmallRecord.encode(supervision.blended() ? "brn-bootstrap-plan-v2" : "brn-bootstrap-plan-v1", out -> {
            out.writeUTF(parentId); out.writeUTF(incumbentId); out.writeLong(generation);
            out.writeUTF(source.generatorStore()); out.writeUTF(generatorId); out.writeUTF(generatorHash);
            out.writeUTF(settings); out.writeLong(splitSeed);
            if (supervision.blended()) supervision.write(out);
        });
    }
    static BootstrapPlan read(Path path) throws IOException {
        return SmallRecord.read(path, java.util.Map.of(
                "brn-bootstrap-plan-v1", in -> read(in, false), "brn-bootstrap-plan-v2", in -> read(in, true)));
    }
    private static BootstrapPlan read(DataInputStream in, boolean extended) throws IOException {
        String parent = in.readUTF(), incumbent = in.readUTF(); long generation = in.readLong();
        var source = TrainingSource.bootstrap(Path.of(in.readUTF()));
        String generator = in.readUTF(), hash = in.readUTF(), settings = in.readUTF(); long seed = in.readLong();
        var supervision = extended ? BrnSupervision.read(in) : BrnSupervision.WDL;
        if (extended && !supervision.blended()) throw new IOException("Invalid extended bootstrap objective.");
        return new BootstrapPlan(parent, incumbent, generation, source, generator, hash, settings, seed, supervision);
    }

    public String hash() throws IOException { return SmallRecord.hash(encode()); }
}
