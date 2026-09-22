package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.Path;
import com.ohinteractive.seedv6.training.service.*;

/** Settings of work after a settled parent; never an acceptance record. */
public record GenerationAttempt(String parentId, String incumbentId, long generation,
                                TrainingSource source, String settings, Format format, String notice) {
    public enum Format { CURRENT, LEGACY_BOOTSTRAP, LEGACY_SELF_PLAY }
    public static final String FILE = "generation-attempt.bin";
    public GenerationAttempt {
        CheckpointManifest.requireId(parentId); CheckpointManifest.requireId(incumbentId);
        if (generation < 1 || source == null || settings.isEmpty() || format == null || notice == null)
            throw new IllegalArgumentException("Invalid generation attempt.");
    }
    public static GenerationAttempt create(String parent, String best, long generation, TrainerConfig config, TrainingSource source) {
        return new GenerationAttempt(parent, best, generation, source, config.attemptSettings(generation, source), Format.CURRENT, "");
    }
    public static GenerationAttempt legacy(BootstrapPlan plan) {
        return new GenerationAttempt(plan.parentId(), plan.incumbentId(), plan.generation(), plan.source(),
                plan.settings(), Format.LEGACY_BOOTSTRAP, "");
    }
    public boolean matches(TrainerConfig config, TrainingSource selected) {
        return source.equals(selected) && settings.equals(switch (format) {
            case CURRENT -> config.attemptSettings(generation, selected);
            case LEGACY_BOOTSTRAP -> config.generationSettings(generation);
            case LEGACY_SELF_PLAY -> Integer.toString(config.selfPlay().depth());
        });
    }
    public GenerationAttempt restarted(TrainerConfig config, TrainingSource selected) {
        return new GenerationAttempt(parentId, incumbentId, generation, selected, config.attemptSettings(generation, selected),
                Format.CURRENT, "Restarted unfinished generation " + generation + " from settled checkpoint " + parentId
                + " because generation settings changed. Previous unfinished work was archived; this is not an exact continuation.");
    }
    byte[] encode() throws IOException { return SmallRecord.encode("generation-attempt-v1", this::write); }
    void write(DataOutputStream out) throws IOException {
        out.writeUTF(parentId); out.writeUTF(incumbentId); out.writeLong(generation);
        out.writeUTF(source.mode().name()); out.writeUTF(source.generatorStore()); out.writeUTF(settings);
        out.writeUTF(format.name()); out.writeUTF(notice);
    }
    static GenerationAttempt read(DataInputStream in) throws IOException {
        return new GenerationAttempt(in.readUTF(), in.readUTF(), in.readLong(),
                new TrainingSource(TrainingSource.Mode.valueOf(in.readUTF()), in.readUTF()),
                in.readUTF(), Format.valueOf(in.readUTF()), in.readUTF());
    }
    static GenerationAttempt read(Path path) throws IOException {
        return SmallRecord.read(path, "generation-attempt-v1", GenerationAttempt::read);
    }
}
