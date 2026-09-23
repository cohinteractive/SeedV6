package com.ohinteractive.seedv6.training.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;

/** Generation actor selection, independent of the model/optimizer being trained. */
public record TrainingSource(Mode mode, String generatorStore) {
    public enum Mode {
        HANDCRAFTED("Handcrafted"), NNUE_BOOTSTRAP("Bootstrap with NNUE"), SELF_PLAY("Self-play with BRN");
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public static final TrainingSource HANDCRAFTED = new TrainingSource(Mode.HANDCRAFTED, "");
    public static final TrainingSource SELF_PLAY = new TrainingSource(Mode.SELF_PLAY, "");
    public TrainingSource {
        Objects.requireNonNull(mode); Objects.requireNonNull(generatorStore);
        generatorStore = generatorStore.isBlank() ? "" : Path.of(generatorStore).toAbsolutePath().normalize().toString();
        if (mode != Mode.NNUE_BOOTSTRAP) generatorStore = "";
    }
    public static TrainingSource bootstrap(Path root) { return new TrainingSource(Mode.NNUE_BOOTSTRAP, root.toString()); }
    public boolean bootstrap() { return mode != Mode.SELF_PLAY; }
    public boolean nnue() { return mode == Mode.NNUE_BOOTSTRAP; }

    public Path requireGenerator(Path student) throws IOException {
        if (!nnue() || generatorStore.isBlank()) throw new IOException("Select an NNUE Generator Store for BRN bootstrap training.");
        Path generator = Path.of(generatorStore);
        final Path actualGenerator;
        try {
            actualGenerator = generator.toRealPath();
            if (!Files.isDirectory(actualGenerator)) throw new IOException("Not a directory.");
        } catch (IOException unavailable) {
            throw new IOException("NNUE Generator Store is unavailable: " + generator + ". " + unavailable.getMessage(), unavailable);
        }
        // Resolve existing ancestors too, so aliases cannot nest the student inside the generator.
        Path actualStudent = realLocation(student.toAbsolutePath().normalize());
        if (actualStudent.startsWith(actualGenerator) || actualGenerator.startsWith(actualStudent))
            throw new IOException("BRN checkpoint store and NNUE generator store must be separate, non-nested folders.");
        return generator;
    }
    private static Path realLocation(Path path) throws IOException {
        if (Files.exists(path)) return path.toRealPath();
        return realLocation(path.getParent()).resolve(path.getFileName()).normalize();
    }
    public CheckpointStore.Checkpoint loadBest(Path student) throws IOException {
        Path root = requireGenerator(student);
        try {
            if (com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.freshRoot(root, TrainingArchitecture.NNUE))
                throw new IOException("The NNUE generator store has no accepted Best network.");
            return requireNnue(CheckpointStore.readBestSnapshot(root));
        } catch (IOException invalid) {
            throw new IOException("Invalid NNUE Generator Store " + root + ": " + invalid.getMessage(), invalid);
        }
    }
    public static CheckpointStore.Checkpoint requireNnue(CheckpointStore.Checkpoint checkpoint) throws IOException {
        if (checkpoint.manifest().architecture() != TrainingArchitecture.NNUE)
            throw new IOException("Generator architecture must be NNUE; found " + checkpoint.manifest().architecture() + ".");
        return checkpoint;
    }
}
