package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.Path;
import com.ohinteractive.seedv6.training.service.*;

/** Immutable generation and supervision identities. Legacy encodings retain their original hashes. */
public record BootstrapPlan(String parentId, String incumbentId, long generation, TrainingSource source,
        String generatorId, String generatorHash, String settings, long splitSeed, BrnSupervision supervision,
        String teacherStore, String teacherId, String teacherHash, int version) {
    public BootstrapPlan {
        java.util.Objects.requireNonNull(supervision);
        CheckpointManifest.requireId(parentId); CheckpointManifest.requireId(incumbentId);
        requirePin(source.nnue(), source.generatorStore(), generatorId, generatorHash);
        requirePin(supervision.blended(), teacherStore, teacherId, teacherHash);
        if (generation < 1 || !source.bootstrap() || settings.isEmpty() || version < 1 || version > 3
                || version < 3 && (!source.nnue() || version != (supervision.blended() ? 2 : 1)
                || supervision.blended() && (!teacherStore.equals(source.generatorStore())
                || !teacherId.equals(generatorId) || !teacherHash.equals(generatorHash))))
            throw new IllegalArgumentException("Invalid bootstrap plan.");
    }
    static void requirePin(boolean required, String store, String id, String hash) {
        java.util.Objects.requireNonNull(store); java.util.Objects.requireNonNull(id); java.util.Objects.requireNonNull(hash);
        if (required) {
            if (store.isBlank()) throw new IllegalArgumentException("Missing NNUE store.");
            CheckpointManifest.requireId(id); SmallRecord.requireHash(hash);
        } else if (!store.isEmpty() || !id.isEmpty() || !hash.isEmpty())
            throw new IllegalArgumentException("Unexpected neural checkpoint identity.");
    }
    /** Historical constructor: the single NNUE pin served both roles for blends. */
    public BootstrapPlan(String parentId, String incumbentId, long generation, TrainingSource source,
            String generatorId, String generatorHash, String settings, long splitSeed, BrnSupervision supervision) {
        this(parentId, incumbentId, generation, source, generatorId, generatorHash, settings, splitSeed, supervision,
                supervision.blended() ? source.generatorStore() : "", supervision.blended() ? generatorId : "",
                supervision.blended() ? generatorHash : "", supervision.blended() ? 2 : 1);
    }
    public BootstrapPlan(String parentId, String incumbentId, long generation, TrainingSource source,
            String generatorId, String generatorHash, String settings, long splitSeed) {
        this(parentId, incumbentId, generation, source, generatorId, generatorHash, settings, splitSeed, BrnSupervision.WDL);
    }
    public static BootstrapPlan create(String parent, String best, long generation, TrainerConfig config,
            TrainingSource source, CheckpointStore.Checkpoint generator, String teacherStore, CheckpointStore.Checkpoint teacher) {
        return new BootstrapPlan(parent, best, generation, source,
                generator == null ? "" : generator.manifest().id(), generator == null ? "" : generator.manifest().networkSha256(),
                config.generationSettings(generation), config.seed(generation, TrainerConfig.SeedDomain.HOLDOUT),
                config.supervision() == null ? BrnSupervision.WDL : config.supervision(), teacherStore,
                teacher == null ? "" : teacher.manifest().id(), teacher == null ? "" : teacher.manifest().networkSha256(), 3);
    }
    public void requireSettings(TrainerConfig config, TrainingSource selected) throws IOException {
        if (!source.equals(selected) || !settings.equals(config.generationSettings(generation)))
            throw new IOException("Bootstrap pin differs from the reconciled generation settings; existing artifacts were preserved.");
    }
    public CheckpointStore.Checkpoint loadGenerator(Path student) throws IOException {
        if (!source.nnue()) throw new IOException("Handcrafted generation has no neural checkpoint.");
        return loadPin(student, source.generatorStore(), generatorId, generatorHash, "generator");
    }
    public CheckpointStore.Checkpoint loadTeacher(Path student) throws IOException {
        if (!supervision.blended()) throw new IOException("WDL has no teacher checkpoint.");
        return loadPin(student, teacherStore, teacherId, teacherHash, "teacher");
    }
    private static CheckpointStore.Checkpoint loadPin(Path student, String location, String id, String hash, String role) throws IOException {
        try {
            Path root = TrainingSource.bootstrap(Path.of(location)).requireGenerator(student);
            var pinned = TrainingSource.requireNnue(CheckpointStore.readSnapshot(root, id));
            if (!pinned.manifest().networkSha256().equals(hash)) throw new IOException("Pinned NNUE checksum changed.");
            return pinned;
        } catch (IOException unavailable) {
            throw new IOException("Pinned NNUE " + role + " " + id + " is unavailable at " + location
                    + "; restore that checkpoint to resume. No fallback was selected.", unavailable);
        }
    }
    byte[] encode() throws IOException {
        return SmallRecord.encode("brn-bootstrap-plan-v" + version, out -> {
            out.writeUTF(parentId); out.writeUTF(incumbentId); out.writeLong(generation);
            if (version == 3) out.writeUTF(source.mode().name());
            out.writeUTF(source.generatorStore()); out.writeUTF(generatorId); out.writeUTF(generatorHash);
            out.writeUTF(settings); out.writeLong(splitSeed);
            if (version >= 2) supervision.write(out);
            if (version == 3) { out.writeUTF(teacherStore); out.writeUTF(teacherId); out.writeUTF(teacherHash); }
        });
    }
    static BootstrapPlan read(Path path) throws IOException {
        return SmallRecord.read(path, java.util.Map.of("brn-bootstrap-plan-v1", in -> read(in, 1),
                "brn-bootstrap-plan-v2", in -> read(in, 2), "brn-bootstrap-plan-v3", in -> read(in, 3)));
    }
    private static BootstrapPlan read(DataInputStream in, int version) throws IOException {
        String parent = in.readUTF(), incumbent = in.readUTF(); long generation = in.readLong();
        var mode = version == 3 ? TrainingSource.Mode.valueOf(in.readUTF()) : TrainingSource.Mode.NNUE_BOOTSTRAP;
        var source = new TrainingSource(mode, in.readUTF());
        String generator = in.readUTF(), hash = in.readUTF(), settings = in.readUTF(); long seed = in.readLong();
        var supervision = version >= 2 ? BrnSupervision.read(in) : BrnSupervision.WDL;
        if (version == 2 && !supervision.blended()) throw new IOException("Invalid extended bootstrap objective.");
        if (version < 3) return new BootstrapPlan(parent, incumbent, generation, source, generator, hash, settings, seed, supervision);
        return new BootstrapPlan(parent, incumbent, generation, source, generator, hash, settings, seed,
                supervision, in.readUTF(), in.readUTF(), in.readUTF(), version);
    }
    public String hash() throws IOException { return SmallRecord.hash(encode()); }
}
