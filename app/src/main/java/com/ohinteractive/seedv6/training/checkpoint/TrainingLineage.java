package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;

/** Mutable lineage identity/configuration sidecar; generation evidence and checkpoint payloads stay immutable. */
public record TrainingLineage(UUID id, String name, TrainingArchitecture architecture, Instant created,
                              String configuration, String configurationOrigin) {
    public static final String FILE = "training-lineage.bin";
    public TrainingLineage {
        Objects.requireNonNull(id); Objects.requireNonNull(architecture); Objects.requireNonNull(created);
        Objects.requireNonNull(configuration); Objects.requireNonNull(configurationOrigin);
        name = name.strip();
        if (name.isEmpty() || name.length() > 120 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Enter a lineage name of 1 to 120 characters.");
    }
    public TrainingLineage withConfiguration(String value) {
        return new TrainingLineage(id, name, architecture, created, value, "Saved lineage configuration");
    }
    public static Optional<TrainingLineage> read(Path root) throws IOException {
        Path file = root.resolve(FILE);
        if (Files.notExists(file)) return Optional.empty();
        CheckpointPayload.regular(file);
        return Optional.of(SmallRecord.read(file, "training-lineage-v1", TrainingLineage::read));
    }
    static TrainingLineage read(DataInputStream in) throws IOException {
        return new TrainingLineage(UUID.fromString(in.readUTF()), in.readUTF(), TrainingArchitecture.valueOf(in.readUTF()),
                Instant.parse(in.readUTF()), in.readUTF(), in.readUTF());
    }
    void write(DataOutputStream out) throws IOException {
        out.writeUTF(id.toString()); out.writeUTF(name); out.writeUTF(architecture.name());
        out.writeUTF(created.toString()); out.writeUTF(configuration); out.writeUTF(configurationOrigin);
    }
    byte[] encode() throws IOException {
        return SmallRecord.encode("training-lineage-v1", this::write);
    }
}
