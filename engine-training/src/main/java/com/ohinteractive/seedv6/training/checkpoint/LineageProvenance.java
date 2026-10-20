package com.ohinteractive.seedv6.training.checkpoint;

import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Immutable initializer evidence emitted only by a factory that actually created this bootstrap model. */
public record LineageProvenance(String initialCheckpoint, TrainingArchitecture architecture, String initializer,
                                Long initializationSeed, long firstRunSeed, Instant recorded) {
    public static final String FILE = "lineage-provenance.bin";
    public LineageProvenance {
        CheckpointManifest.requireId(initialCheckpoint); Objects.requireNonNull(architecture); Objects.requireNonNull(recorded);
        if (initializer == null || initializer.isBlank() || initializer.length() > 200) throw new IllegalArgumentException("Invalid initializer");
    }
    byte[] encode() throws IOException {
        return SmallRecord.encode("lineage-provenance-v1", out -> {
            out.writeUTF(initialCheckpoint); out.writeUTF(architecture.name()); out.writeUTF(initializer);
            out.writeBoolean(initializationSeed != null); if (initializationSeed != null) out.writeLong(initializationSeed);
            out.writeLong(firstRunSeed); out.writeUTF(recorded.toString());
        });
    }
    public static Optional<LineageProvenance> read(Path root) throws IOException {
        Path file = root.resolve(FILE); if (Files.notExists(file)) return Optional.empty();
        CheckpointPayload.regular(file);
        var value = SmallRecord.read(file, "lineage-provenance-v1", in -> new LineageProvenance(in.readUTF(),
                TrainingArchitecture.valueOf(in.readUTF()), in.readUTF(), in.readBoolean() ? in.readLong() : null,
                in.readLong(), Instant.parse(in.readUTF())));
        requireBootstrap(root, value.initialCheckpoint(), value.architecture());
        return Optional.of(value);
    }
    /** Continuation forks may retain a nonzero counter, but must name an accepted parentless bootstrap. */
    static void requireBootstrap(Path root, String checkpoint, TrainingArchitecture architecture) throws IOException {
        var manifest = CheckpointStore.historicalManifest(root, checkpoint);
        if (manifest.architecture() != architecture || !manifest.parentId().isEmpty()
                || !CheckpointInspection.accepted(root).contains(checkpoint))
            throw new IOException("Initializer evidence does not match this lineage bootstrap");
    }
}
